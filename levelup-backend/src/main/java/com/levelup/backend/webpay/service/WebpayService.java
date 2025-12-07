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
 * Servicio para integración con Webpay Plus usando el SDK oficial de Transbank v3.x.
 * Configurado para ambiente de integración (testing).
 */
@Service
@Slf4j
public class WebpayService {

    private final WebpayOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WebpayPlus.Transaction transaction;

    @Value("${app.webpay.return-url}")
    private String returnUrl;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public WebpayService(
            WebpayOrderRepository orderRepository,
            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        // Crear instancia configurada para ambiente de integración (testing)
        this.transaction = new WebpayPlus.Transaction();
        log.info("Webpay Plus configurado para ambiente de INTEGRACIÓN");
    }

    /**
     * Inicia una transacción Webpay Plus.
     */
    @Transactional
    public WebpayCreateResponse initTransaction(CreateWebpayOrderRequest request) {
        log.info("Iniciando transacción Webpay para usuario: {}", request.getUserId());

        User user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId()).orElse(null);
        }

        // Generar buy order único (máximo 26 caracteres)
        String buyOrder = "ORD" + System.currentTimeMillis();
        String sessionId = UUID.randomUUID().toString().substring(0, 20);
        double amount = request.getAmount().doubleValue();

        WebpayOrder order = WebpayOrder.builder()
                .user(user)
                .totalAmount(request.getAmount())
                .status(WebpayOrder.STATUS_PENDING)
                .buyOrder(buyOrder)
                .sessionId(sessionId)
                .shippingAddress(request.getShippingAddress())
                .build();

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
            WebpayPlusTransactionCreateResponse response = transaction.create(
                    buyOrder, sessionId, amount, returnUrl);

            order.setWebpayToken(response.getToken());
            orderRepository.save(order);

            log.info("Transacción Webpay creada. URL: {}, Token: {}", response.getUrl(), response.getToken());

            return WebpayCreateResponse.builder()
                    .url(response.getUrl())
                    .token(response.getToken())
                    .orderId(order.getId())
                    .build();
        } catch (Exception e) {
            log.error("Error al crear transacción Webpay: {}", e.getMessage(), e);
            order.setStatus(WebpayOrder.STATUS_ERROR);
            order.setNotes("Error: " + e.getMessage());
            orderRepository.save(order);
            throw new RuntimeException("Error al crear transacción: " + e.getMessage(), e);
        }
    }

    /**
     * Maneja el retorno de Webpay.
     */
    @Transactional
    public WebpayCommitResult handleReturn(String tokenWs, String tbkToken, String tbkOrdenCompra, String tbkIdSesion) {
        log.info("Procesando retorno Webpay - tokenWs: {}, tbkToken: {}, tbkOrdenCompra: {}, tbkIdSesion: {}", 
                tokenWs, tbkToken, tbkOrdenCompra, tbkIdSesion);

        if (tbkToken != null && tbkOrdenCompra != null && tokenWs == null) {
            log.info("Usuario abortó el pago");
            return handleAbortedPayment(tbkOrdenCompra);
        }

        if (tbkOrdenCompra != null && tbkIdSesion != null && tokenWs == null && tbkToken == null) {
            log.info("Timeout en el pago");
            return handleTimeoutPayment(tbkOrdenCompra);
        }

        if (tokenWs != null) {
            return commitTransaction(tokenWs);
        }

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

        WebpayOrder order = orderRepository.findByWebpayToken(token).orElse(null);

        try {
            // Llamar al SDK para confirmar
            WebpayPlusTransactionCommitResponse response = transaction.commit(token);

            log.info("Respuesta commit - responseCode: {}, status: {}", 
                    response.getResponseCode(), response.getStatus());

            boolean isAuthorized = response.getResponseCode() == 0;

            if (order != null) {
                order.setStatus(isAuthorized ? WebpayOrder.STATUS_AUTHORIZED : WebpayOrder.STATUS_FAILED);
                order.setAuthorizationCode(response.getAuthorizationCode());
                order.setPaymentTypeCode(response.getPaymentTypeCode());
                order.setInstallmentsNumber((int) response.getInstallmentsNumber());
                order.setResponseCode((int) response.getResponseCode());
                order.setCardNumber(response.getCardDetail() != null ? response.getCardDetail().getCardNumber() : null);
                order.setTransactionDate(response.getTransactionDate() != null ? response.getTransactionDate().toString() : null);
                orderRepository.save(order);
            }

            return WebpayCommitResult.builder()
                    .success(isAuthorized)
                    .status(isAuthorized ? "AUTHORIZED" : "FAILED")
                    .orderId(order != null ? order.getId() : null)
                    .amount(BigDecimal.valueOf(response.getAmount()))
                    .authorizationCode(response.getAuthorizationCode())
                    .paymentTypeCode(response.getPaymentTypeCode())
                    .installmentsNumber((int) response.getInstallmentsNumber())
                    .buyOrder(response.getBuyOrder())
                    .sessionId(response.getSessionId())
                    .transactionDate(response.getTransactionDate() != null ? response.getTransactionDate().toString() : null)
                    .cardNumber(response.getCardDetail() != null ? response.getCardDetail().getCardNumber() : null)
                    .message(isAuthorized ? "Pago autorizado exitosamente" : "Pago rechazado. Código: " + response.getResponseCode())
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
                    .message("Error al confirmar: " + e.getMessage())
                    .build();
        }
    }

    private WebpayCommitResult handleAbortedPayment(String buyOrder) {
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
                .message("Tiempo de espera agotado")
                .build();
    }

    public String getRedirectUrl(WebpayCommitResult result) {
        if (result.isSuccess()) {
            return frontendBaseUrl + "/checkout/success?orderId=" + result.getOrderId() + "&status=" + result.getStatus();
        } else {
            String message = result.getMessage() != null ? result.getMessage() : "Error desconocido";
            return frontendBaseUrl + "/checkout/failure?orderId=" + result.getOrderId() + 
                    "&status=" + result.getStatus() + 
                    "&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
        }
    }

    @Transactional(readOnly = true)
    public WebpayOrder getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }
}
