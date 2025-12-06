package com.levelup.backend.webpay.service;

import cl.transbank.webpay.webpayplus.WebpayPlus;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCommitResponse;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import com.levelup.backend.user.User;
import com.levelup.backend.user.UserRepository;
import com.levelup.backend.webpay.dto.CreateWebpayOrderRequest;
import com.levelup.backend.webpay.dto.WebpayCommitResult;
import com.levelup.backend.webpay.dto.WebpayCreateResponse;
import com.levelup.backend.webpay.entity.WebpayOrder;
import com.levelup.backend.webpay.entity.WebpayOrderItem;
import com.levelup.backend.webpay.repository.WebpayOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Servicio para integración con Webpay Plus usando el SDK oficial de Transbank.
 * Solo ambiente de integración (nunca producción).
 */
@Service
@Slf4j
public class WebpayService {

    private final WebpayPlus.Transaction webpayTransaction;
    private final WebpayOrderRepository orderRepository;
    private final UserRepository userRepository;

    @Value("${app.webpay.return-url}")
    private String returnUrl;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public WebpayService(
            WebpayPlus.Transaction webpayTransaction,
            WebpayOrderRepository orderRepository,
            UserRepository userRepository) {
        this.webpayTransaction = webpayTransaction;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    /**
     * Inicia una transacción Webpay Plus.
     * 
     * @param request datos de la orden
     * @return respuesta con URL de redirección y token
     */
    @Transactional
    public WebpayCreateResponse initTransaction(CreateWebpayOrderRequest request) {
        log.info("Iniciando transacción Webpay para usuario: {}", request.getUserId());

        // Buscar usuario
        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId()).orElse(null);
        }

        // Generar buy order único
        String buyOrder = "ORDER-" + System.currentTimeMillis();
        String sessionId = UUID.randomUUID().toString();
        
        // Convertir monto a entero (Transbank requiere enteros)
        int amount = request.getAmount().intValue();

        // Crear la orden en estado PENDING
        WebpayOrder order = WebpayOrder.builder()
                .user(user)
                .totalAmount(request.getAmount())
                .status(WebpayOrder.STATUS_PENDING)
                .buyOrder(buyOrder)
                .sessionId(sessionId)
                .shippingAddress(request.getShippingAddress())
                .build();

        // Agregar items si existen
        if (request.getItems() != null) {
            for (CreateWebpayOrderRequest.OrderItemRequest item : request.getItems()) {
                WebpayOrderItem orderItem = WebpayOrderItem.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build();
                order.addItem(orderItem);
            }
        }

        order = orderRepository.save(order);
        log.info("Orden creada con ID: {} y buyOrder: {}", order.getId(), buyOrder);

        try {
            // Llamar al SDK de Transbank
            WebpayPlusTransactionCreateResponse response = webpayTransaction.create(
                    buyOrder,
                    sessionId,
                    amount,
                    returnUrl
            );

            // Guardar el token de Webpay
            order.setWebpayToken(response.getToken());
            orderRepository.save(order);

            log.info("Transacción Webpay creada exitosamente. Token: {}", response.getToken());

            return WebpayCreateResponse.builder()
                    .url(response.getUrl())
                    .token(response.getToken())
                    .orderId(order.getId())
                    .build();

        } catch (Exception e) {
            log.error("Error al crear transacción Webpay: {}", e.getMessage(), e);
            order.setStatus(WebpayOrder.STATUS_ERROR);
            order.setNotes("Error al iniciar: " + e.getMessage());
            orderRepository.save(order);
            throw new RuntimeException("Error al iniciar transacción Webpay: " + e.getMessage(), e);
        }
    }

    /**
     * Maneja el retorno de Webpay.
     * 
     * @param tokenWs token de transacción exitosa/rechazada
     * @param tbkToken token de transacción abortada
     * @param tbkOrdenCompra buy order (en caso de abort/timeout)
     * @param tbkIdSesion session id (en caso de abort/timeout)
     * @return resultado del commit
     */
    @Transactional
    public WebpayCommitResult handleReturn(String tokenWs, String tbkToken, String tbkOrdenCompra, String tbkIdSesion) {
        log.info("Procesando retorno Webpay - tokenWs: {}, tbkToken: {}, tbkOrdenCompra: {}, tbkIdSesion: {}", 
                tokenWs, tbkToken, tbkOrdenCompra, tbkIdSesion);

        // Caso 1: Usuario abortó el pago
        if (tbkToken != null && tbkOrdenCompra != null && tokenWs == null) {
            log.info("Usuario abortó el pago");
            return handleAbortedPayment(tbkOrdenCompra, tbkToken);
        }

        // Caso 2: Timeout - formulario enviado pero sin completar
        if (tbkOrdenCompra != null && tbkIdSesion != null && tokenWs == null && tbkToken == null) {
            log.info("Timeout en el pago");
            return handleTimeoutPayment(tbkOrdenCompra);
        }

        // Caso 3: Flujo normal (exitoso o rechazado)
        if (tokenWs != null) {
            return commitTransaction(tokenWs);
        }

        // Caso 4: No se recibió ningún parámetro válido
        log.error("Retorno Webpay sin parámetros válidos");
        return WebpayCommitResult.builder()
                .success(false)
                .status("ERROR")
                .message("No se recibieron parámetros válidos de Webpay")
                .build();
    }

    /**
     * Confirma una transacción con Transbank.
     */
    private WebpayCommitResult commitTransaction(String token) {
        log.info("Confirmando transacción con token: {}", token);

        // Buscar la orden por token
        WebpayOrder order = orderRepository.findByWebpayToken(token).orElse(null);

        try {
            // Llamar al SDK para confirmar
            WebpayPlusTransactionCommitResponse response = webpayTransaction.commit(token);

            log.info("Respuesta de commit - responseCode: {}, status: {}", 
                    response.getResponseCode(), response.getStatus());

            // Verificar si fue autorizada (responseCode == 0)
            boolean isAuthorized = response.getResponseCode() != null && response.getResponseCode() == 0;

            if (order != null) {
                if (isAuthorized) {
                    order.setStatus(WebpayOrder.STATUS_AUTHORIZED);
                } else {
                    order.setStatus(WebpayOrder.STATUS_FAILED);
                }
                order.setAuthorizationCode(response.getAuthorizationCode());
                order.setPaymentTypeCode(response.getPaymentTypeCode());
                order.setInstallmentsNumber(response.getInstallmentsNumber() != null ? 
                        response.getInstallmentsNumber().intValue() : null);
                order.setResponseCode(response.getResponseCode());
                order.setCardNumber(response.getCardDetail() != null ? 
                        response.getCardDetail().getCardNumber() : null);
                order.setTransactionDate(response.getTransactionDate() != null ? 
                        response.getTransactionDate().toString() : null);
                orderRepository.save(order);
            }

            return WebpayCommitResult.builder()
                    .success(isAuthorized)
                    .status(isAuthorized ? "AUTHORIZED" : "FAILED")
                    .orderId(order != null ? order.getId() : null)
                    .amount(response.getAmount() != null ? BigDecimal.valueOf(response.getAmount()) : null)
                    .authorizationCode(response.getAuthorizationCode())
                    .paymentTypeCode(response.getPaymentTypeCode())
                    .installmentsNumber(response.getInstallmentsNumber() != null ? 
                            response.getInstallmentsNumber().intValue() : null)
                    .buyOrder(response.getBuyOrder())
                    .sessionId(response.getSessionId())
                    .transactionDate(response.getTransactionDate() != null ? 
                            response.getTransactionDate().toString() : null)
                    .cardNumber(response.getCardDetail() != null ? 
                            response.getCardDetail().getCardNumber() : null)
                    .message(isAuthorized ? "Pago autorizado exitosamente" : 
                            "Pago rechazado. Código: " + response.getResponseCode())
                    .build();

        } catch (Exception e) {
            log.error("Error al confirmar transacción: {}", e.getMessage(), e);
            
            if (order != null) {
                order.setStatus(WebpayOrder.STATUS_ERROR);
                order.setNotes("Error en commit: " + e.getMessage());
                orderRepository.save(order);
            }

            return WebpayCommitResult.builder()
                    .success(false)
                    .status("ERROR")
                    .orderId(order != null ? order.getId() : null)
                    .message("Error al confirmar transacción: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Maneja un pago abortado por el usuario.
     */
    private WebpayCommitResult handleAbortedPayment(String buyOrder, String tbkToken) {
        WebpayOrder order = orderRepository.findByBuyOrder(buyOrder).orElse(null);
        
        if (order != null) {
            order.setStatus(WebpayOrder.STATUS_ABORTED);
            order.setNotes("Pago abortado por el usuario");
            orderRepository.save(order);
        }

        return WebpayCommitResult.builder()
                .success(false)
                .status("ABORTED")
                .orderId(order != null ? order.getId() : null)
                .buyOrder(buyOrder)
                .message("El usuario canceló el pago")
                .build();
    }

    /**
     * Maneja un timeout en el pago.
     */
    private WebpayCommitResult handleTimeoutPayment(String buyOrder) {
        WebpayOrder order = orderRepository.findByBuyOrder(buyOrder).orElse(null);
        
        if (order != null) {
            order.setStatus(WebpayOrder.STATUS_TIMEOUT);
            order.setNotes("Timeout en el formulario de pago");
            orderRepository.save(order);
        }

        return WebpayCommitResult.builder()
                .success(false)
                .status("TIMEOUT")
                .orderId(order != null ? order.getId() : null)
                .buyOrder(buyOrder)
                .message("Tiempo de espera agotado en el formulario de pago")
                .build();
    }

    /**
     * Obtiene la URL de redirección al frontend según el resultado.
     */
    public String getRedirectUrl(WebpayCommitResult result) {
        String baseUrl = frontendBaseUrl;
        
        if (result.isSuccess()) {
            return baseUrl + "/checkout/success?orderId=" + result.getOrderId() + 
                    "&status=" + result.getStatus();
        } else {
            return baseUrl + "/checkout/failure?orderId=" + result.getOrderId() + 
                    "&status=" + result.getStatus() + 
                    "&message=" + URLEncoder.encode(
                            result.getMessage() != null ? result.getMessage() : "Error desconocido", 
                            StandardCharsets.UTF_8);
        }
    }

    /**
     * Obtiene una orden por su ID.
     */
    @Transactional(readOnly = true)
    public WebpayOrder getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    /**
     * Obtiene una orden por su token.
     */
    @Transactional(readOnly = true)
    public WebpayOrder getOrderByToken(String token) {
        return orderRepository.findByWebpayToken(token).orElse(null);
    }
}
