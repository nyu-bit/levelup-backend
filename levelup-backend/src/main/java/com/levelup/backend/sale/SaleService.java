package com.levelup.backend.sale;

import com.levelup.backend.dto.SaleItemRequest;
import com.levelup.backend.dto.SaleRequest;
import com.levelup.backend.product.Product;
import com.levelup.backend.product.ProductRepository;
import com.levelup.backend.user.RoleName;
import com.levelup.backend.user.User;
import com.levelup.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {
    
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    
    // Porcentaje de IVA
    private static final double IVA_PERCENTAGE = 0.19;
    
    // Costo de envío fijo (puede modificarse a una lógica más compleja)
    private static final int SHIPPING_COST = 3990;
    
    @Transactional
    public Sale createSale(SaleRequest request, String userEmail) {
        // Buscar usuario
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        // Crear la venta
        Sale sale = Sale.builder()
                .user(user)
                .status(SaleStatus.PENDING)
                .transbankToken(UUID.randomUUID().toString()) // Token simulado para Transbank
                .build();
        
        int subtotal = 0;
        
        // Procesar cada item
        for (SaleItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Producto no encontrado con ID: " + itemRequest.getProductId()));
            
            // Verificar stock suficiente
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new IllegalArgumentException(
                        "Stock insuficiente para el producto '" + product.getName() + 
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
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return saleRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    @Transactional(readOnly = true)
    public Sale getById(Long id, String userEmail, Set<String> roles) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada con ID: " + id));
        
        // Verificar permisos
        boolean isAdminOrVendedor = roles.contains("ROLE_ADMIN") || roles.contains("ROLE_VENDEDOR");
        
        if (!isAdminOrVendedor) {
            // CLIENTE solo puede ver sus propias ventas
            if (!sale.getUser().getEmail().equals(userEmail)) {
                throw new IllegalArgumentException("No tiene permisos para ver esta venta");
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
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada con token: " + token));
        
        if ("OK".equalsIgnoreCase(status)) {
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
