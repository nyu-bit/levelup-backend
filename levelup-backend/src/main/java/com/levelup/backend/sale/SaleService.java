package com.levelup.backend.sale;

import com.levelup.backend.dto.*;
import com.levelup.backend.payment.PaymentResponse;
import com.levelup.backend.payment.PaymentService;
import com.levelup.backend.product.Product;
import com.levelup.backend.product.ProductRepository;
import com.levelup.backend.user.User;
import com.levelup.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {
    
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    
    // Porcentaje de IVA
    private static final double IVA_PERCENTAGE = 0.19;
    
    // Costo de envío fijo (puede modificarse a una lógica más compleja)
    private static final int SHIPPING_COST = 3990;

    // URL de redirección a Webpay (configurada en application.properties)
    @Value("${payment.transbank.mock-base-url:https://webpay.mock/redirect}")
    private String webpayMockUrl;

    /**
     * Crea una venta y procesa el pago a través del mock de Transbank.
     * - Genera orderId único (UUID)
     * - Llama al PaymentService.procesarPagoConMock
     * - Marca la venta como APPROVED o REJECTED según respuesta
     * - Retorna SaleSummaryDto con el resumen
     */
    @Transactional
    public SaleSummaryDto createSaleWithPayment(SaleRequestDto request, String userEmail) {
        log.info("Creando venta con pago para usuario: {}", userEmail);
        
        // Buscar usuario
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        
        // Generar orderId único
        String orderId = UUID.randomUUID().toString();
        log.info("OrderId generado: {}", orderId);
        
        // Crear la venta con estado PENDING
        Sale sale = Sale.builder()
                .user(user)
                .status(SaleStatus.PENDING)
                .transbankToken(orderId) // Usamos orderId como token inicial
                .build();
        
        int subtotal = 0;
        
        // Procesar cada item (validar productos y stock, pero NO descontar aún)
        for (SaleItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Producto no encontrado con ID: " + itemRequest.getProductId()));
            
            // Verificar stock suficiente
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para '" + product.getName() + 
                        "'. Disponible: " + product.getStock() + ", solicitado: " + itemRequest.getQuantity());
            }
            
            // Crear item de venta (sin descontar stock todavía)
            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            
            sale.addItem(saleItem);
            subtotal += product.getPrice() * itemRequest.getQuantity();
        }
        
        // Calcular IVA (19%)
        int iva = (int) Math.round(subtotal * IVA_PERCENTAGE);
        
        // Calcular shipping
        boolean includeShipping = request.getIncludeShipping() == null || request.getIncludeShipping();
        int shipping = includeShipping ? SHIPPING_COST : 0;
        
        // Calcular total
        int total = subtotal + iva + shipping;
        
        sale.setSubtotal(subtotal);
        sale.setIva(iva);
        sale.setShipping(shipping);
        sale.setTotal(total);
        
        // Guardar venta en estado PENDING
        Sale savedSale = saleRepository.save(sale);
        log.info("Venta guardada con ID: {}, estado: PENDING", savedSale.getId());
        
        // ========================================
        // LLAMAR AL MOCK DE TRANSBANK
        // ========================================
        PaymentResponse paymentResponse;
        String paymentMessage;
        String authorizationCode = null;
        
        try {
            boolean useMockLocal = request.getUseMockLocal() != null && request.getUseMockLocal();
            
            if (useMockLocal) {
                log.info("Usando mock LOCAL para pago");
                paymentResponse = paymentService.procesarPagoMockLocal(orderId, BigDecimal.valueOf(total));
            } else {
                log.info("Llamando al mock EXTERNO de Transbank");
                paymentResponse = paymentService.procesarPagoConMock(orderId, BigDecimal.valueOf(total));
            }
            
            // Actualizar token con el que devuelve el mock
            if (paymentResponse.getToken() != null) {
                savedSale.setTransbankToken(paymentResponse.getToken());
            }
            
            // Procesar respuesta del mock
            if (paymentResponse.isSuccessful()) {
                // PAGO APROBADO
                savedSale.setStatus(SaleStatus.APPROVED);
                paymentMessage = paymentResponse.getMessage() != null ? 
                        paymentResponse.getMessage() : "Pago aprobado exitosamente";
                authorizationCode = paymentResponse.getAuthorizationCode();
                
                // Descontar stock
                for (SaleItem item : savedSale.getItems()) {
                    Product product = item.getProduct();
                    product.setStock(product.getStock() - item.getQuantity());
                    productRepository.save(product);
                }
                log.info("Pago APROBADO - Stock descontado");
                
            } else {
                // PAGO RECHAZADO
                savedSale.setStatus(SaleStatus.REJECTED);
                paymentMessage = paymentResponse.getMessage() != null ? 
                        paymentResponse.getMessage() : "Pago rechazado";
                log.warn("Pago RECHAZADO: {}", paymentMessage);
            }
            
        } catch (Exception e) {
            log.error("Error al procesar pago con mock: {}", e.getMessage());
            savedSale.setStatus(SaleStatus.REJECTED);
            paymentMessage = "Error al procesar el pago: " + e.getMessage();
        }
        
        // Guardar venta con estado final
        Sale finalSale = saleRepository.save(savedSale);
        
        // Construir items para el resumen
        List<SaleSummaryDto.SaleItemSummary> itemSummaries = finalSale.getItems().stream()
                .map(item -> SaleSummaryDto.SaleItemSummary.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getUnitPrice() * item.getQuantity())
                        .build())
                .toList();
        
        // Construir y retornar SaleSummaryDto
        return SaleSummaryDto.builder()
                .orderNumber(orderId)
                .saleId(finalSale.getId())
                .status(finalSale.getStatus())
                .statusDescription(SaleSummaryDto.getStatusDescription(finalSale.getStatus()))
                .subtotal(finalSale.getSubtotal())
                .iva(finalSale.getIva())
                .shipping(finalSale.getShipping())
                .total(finalSale.getTotal())
                .transbankToken(finalSale.getTransbankToken())
                .authorizationCode(authorizationCode)
                .paymentMessage(paymentMessage)
                .createdAt(finalSale.getCreatedAt())
                .itemsCount(finalSale.getItems().size())
                .items(itemSummaries)
                .build();
    }
    
    /**
     * Crea una venta y SIMULA Transbank internamente (auto-approve)
     * - Valida stock
     * - Crea Sale con estado APPROVED
     * - Descuenta stock inmediatamente
     * - Genera token Transbank simulado
     */
    @Transactional
    public SaleResponse createSaleWithAutoApprove(CreateSaleRequest request, String userEmail) {
        // Buscar usuario
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        
        // Generar token Transbank simulado
        String transbankToken = "tbk_" + UUID.randomUUID().toString().substring(0, 8);
        
        // Crear la venta
        Sale sale = Sale.builder()
                .user(user)
                .status(SaleStatus.PENDING) // Inicia PENDING
                .transbankToken(transbankToken)
                .build();
        
        int subtotal = 0;
        
        // Procesar cada item
        for (SaleItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Producto no encontrado con ID: " + itemRequest.getProductId()));
            
            // Verificar stock suficiente
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para '" + product.getName() + 
                        "'. Disponible: " + product.getStock() + ", solicitado: " + itemRequest.getQuantity());
            }
            
            // Crear item de venta
            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            
            sale.addItem(saleItem);
            
            // Sumar al subtotal
            subtotal += product.getPrice() * itemRequest.getQuantity();
        }
        
        // Calcular IVA (19%)
        int iva = (int) Math.round(subtotal * IVA_PERCENTAGE);
        
        // Calcular total (sin shipping para este flujo simplificado)
        int total = subtotal + iva;
        
        sale.setSubtotal(subtotal);
        sale.setIva(iva);
        sale.setShipping(0); // Sin shipping en este flujo
        sale.setTotal(total);
        
        // ========================================
        // SIMULACIÓN TRANSBANK: AUTO-APPROVE
        // ========================================
        sale.setStatus(SaleStatus.APPROVED);
        
        // Descontar stock de cada producto
        for (SaleItem item : sale.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }
        
        Sale savedSale = saleRepository.save(sale);
        
        // Construir respuesta
        List<SaleResponse.SaleItemResponse> itemResponses = savedSale.getItems().stream()
                .map(item -> SaleResponse.SaleItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getUnitPrice() * item.getQuantity())
                        .build())
                .toList();
        
        return SaleResponse.builder()
                .saleId(savedSale.getId())
                .status(savedSale.getStatus())
                .subtotal(savedSale.getSubtotal())
                .iva(savedSale.getIva())
                .shipping(savedSale.getShipping())
                .total(savedSale.getTotal())
                .transbankToken(savedSale.getTransbankToken())
                .createdAt(savedSale.getCreatedAt())
                .items(itemResponses)
                .build();
    }
    
    /**
     * Inicializa una transacción tipo Transbank (simulado)
     * Crea la venta con estado PENDING, NO descuenta stock aún
     */
    @Transactional
    public InitTransactionResponse initTransaction(InitTransactionRequest request, String userEmail) {
        // Buscar usuario
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        
        // Generar token Transbank simulado
        String transbankToken = "tbk_" + UUID.randomUUID().toString().substring(0, 8);
        
        // Crear la venta con estado PENDING
        Sale sale = Sale.builder()
                .user(user)
                .status(SaleStatus.PENDING)
                .transbankToken(transbankToken)
                .build();
        
        int subtotal = 0;
        
        // Procesar cada item (validar productos y stock, pero NO descontar aún)
        for (SaleItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Producto no encontrado con ID: " + itemRequest.getProductId()));
            
            // Verificar stock suficiente
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para '" + product.getName() + 
                        "'. Disponible: " + product.getStock() + ", solicitado: " + itemRequest.getQuantity());
            }
            
            // Crear item de venta (sin descontar stock todavía)
            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            
            sale.addItem(saleItem);
            
            // Sumar al subtotal
            subtotal += product.getPrice() * itemRequest.getQuantity();
        }
        
        // Calcular IVA (19%)
        int iva = (int) Math.round(subtotal * IVA_PERCENTAGE);
        
        // Calcular total
        int total = subtotal + iva + SHIPPING_COST;
        
        sale.setSubtotal(subtotal);
        sale.setIva(iva);
        sale.setShipping(SHIPPING_COST);
        sale.setTotal(total);
        
        Sale savedSale = saleRepository.save(sale);
        
        // Retornar respuesta con token y URL de redirección
        return InitTransactionResponse.builder()
                .token(transbankToken)
                .url(webpayMockUrl)
                .saleId(savedSale.getId())
                .subtotal(subtotal)
                .iva(iva)
                .shipping(SHIPPING_COST)
                .total(total)
                .build();
    }
    
    /**
     * Procesa el callback de Transbank (simulado)
     * Si AUTHORIZED: aprueba venta y descuenta stock
     * Si FAILED: rechaza venta
     */
    @Transactional
    public TransbankCallbackResponse processCallback(TransbankCallbackRequest request) {
        String token = request.getToken();
        String status = request.getStatus();
        
        Sale sale = saleRepository.findByTransbankToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Venta no encontrada con token: " + token));
        
        // Verificar que la venta esté pendiente
        if (sale.getStatus() != SaleStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La venta ya fue procesada. Estado actual: " + sale.getStatus());
        }
        
        String message;
        
        if ("AUTHORIZED".equalsIgnoreCase(status)) {
            // Aprobar venta
            sale.setStatus(SaleStatus.APPROVED);
            
            // Descontar stock de los productos
            for (SaleItem item : sale.getItems()) {
                Product product = item.getProduct();
                
                // Verificar stock nuevamente (por si cambió)
                if (product.getStock() < item.getQuantity()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Stock insuficiente para '" + product.getName() + 
                            "'. Disponible: " + product.getStock());
                }
                
                product.setStock(product.getStock() - item.getQuantity());
                productRepository.save(product);
            }
            
            message = "Payment approved";
            
        } else if ("FAILED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
            // Rechazar venta (no descontamos stock porque nunca se descontó)
            sale.setStatus(SaleStatus.REJECTED);
            message = "Payment rejected";
            
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status inválido: " + status + ". Use AUTHORIZED o FAILED");
        }
        
        Sale updatedSale = saleRepository.save(sale);
        
        return TransbankCallbackResponse.builder()
                .message(message)
                .saleId(updatedSale.getId())
                .status(updatedSale.getStatus())
                .total(updatedSale.getTotal())
                .build();
    }
    
    /**
     * Consulta el estado de un pago por token
     */
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String token) {
        Sale sale = saleRepository.findByTransbankToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Venta no encontrada con token: " + token));
        
        return PaymentStatusResponse.builder()
                .saleId(sale.getId())
                .token(sale.getTransbankToken())
                .status(sale.getStatus())
                .subtotal(sale.getSubtotal())
                .iva(sale.getIva())
                .shipping(sale.getShipping())
                .total(sale.getTotal())
                .createdAt(sale.getCreatedAt())
                .itemsCount(sale.getItems().size())
                .build();
    }
    
    @Transactional
    public Sale createSale(SaleRequest request, String userEmail) {
        // Buscar usuario
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        
        // Crear la venta
        Sale sale = Sale.builder()
                .user(user)
                .status(SaleStatus.PENDING)
                .transbankToken(UUID.randomUUID().toString())
                .build();
        
        int subtotal = 0;
        
        // Procesar cada item
        for (SaleItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Producto no encontrado con ID: " + itemRequest.getProductId()));
            
            // Verificar stock suficiente
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para '" + product.getName() + 
                        "'. Disponible: " + product.getStock() + ", solicitado: " + itemRequest.getQuantity());
            }
            
            // Descontar stock
            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);
            
            // Crear item de venta
            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            
            sale.addItem(saleItem);
            
            // Sumar al subtotal
            subtotal += product.getPrice() * itemRequest.getQuantity();
        }
        
        // Calcular IVA (19%)
        int iva = (int) Math.round(subtotal * IVA_PERCENTAGE);
        
        // Calcular total
        int total = subtotal + iva + SHIPPING_COST;
        
        sale.setSubtotal(subtotal);
        sale.setIva(iva);
        sale.setShipping(SHIPPING_COST);
        sale.setTotal(total);
        
        return saleRepository.save(sale);
    }
    
    @Transactional(readOnly = true)
    public List<Sale> getMySales(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return saleRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    @Transactional(readOnly = true)
    public Sale getById(Long id, String userEmail, Set<String> roles) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada con ID: " + id));
        
        // Verificar permisos
        boolean isAdminOrVendedor = roles.contains("ROLE_ADMIN") || roles.contains("ROLE_VENDEDOR");
        
        if (!isAdminOrVendedor) {
            // CLIENTE solo puede ver sus propias ventas
            if (!sale.getUser().getEmail().equals(userEmail)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos para ver esta venta");
            }
        }
        
        return sale;
    }
    
    @Transactional(readOnly = true)
    public Page<Sale> getAllSales(Pageable pageable) {
        return saleRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    @Transactional
    public Sale handleTransbankCallback(String token, String status) {
        Sale sale = saleRepository.findByTransbankToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada con token: " + token));
        
        if ("OK".equalsIgnoreCase(status) || "AUTHORIZED".equalsIgnoreCase(status)) {
            sale.setStatus(SaleStatus.APPROVED);
        } else if ("FAILED".equalsIgnoreCase(status)) {
            sale.setStatus(SaleStatus.REJECTED);
            
            // Si la venta es rechazada, devolver el stock
            for (SaleItem item : sale.getItems()) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }
        
        return saleRepository.save(sale);
    }
}
