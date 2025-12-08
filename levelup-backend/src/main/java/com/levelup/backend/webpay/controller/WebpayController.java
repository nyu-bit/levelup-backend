package com.levelup.backend.webpay.controller;

import com.levelup.backend.webpay.dto.CreateWebpayOrderRequest;
import com.levelup.backend.webpay.dto.WebpayCommitRequest;
import com.levelup.backend.webpay.dto.WebpayCommitResult;
import com.levelup.backend.webpay.dto.WebpayCreateResponse;
import com.levelup.backend.webpay.entity.WebpayOrder;
import com.levelup.backend.webpay.service.WebpayService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para integración con Webpay Plus.
 * 
 * Rutas:
 * - POST /api/payments/webpay/create
 * - POST /api/payments/webpay/commit
 * - POST|GET /api/payments/webpay/return (callback de Transbank)
 * - GET /api/payments/webpay/orders/{orderId}
 * - GET /api/payments/webpay/health
 */
@RestController
@RequestMapping("/api/payments/webpay")
@RequiredArgsConstructor
@Slf4j
public class WebpayController {

    private final WebpayService webpayService;

    /**
     * POST /api/payments/webpay/create
     * 
     * Crea una nueva transacción Webpay Plus.
     * 
     * Request body:
     * {
     *   "buyOrder": "ORDEN-123",
     *   "sessionId": "SESSION-123",
     *   "amount": 9990
     * }
     * 
     * Response:
     * {
     *   "url": "https://webpay3gint.transbank.cl/webpayserver/initTransaction",
     *   "token": "TOKEN_WS_AQUI",
     *   "orderId": 1
     * }
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createTransaction(@RequestBody Map<String, Object> request) {
        log.info("POST /api/payments/webpay/create - Request: {}", request);
        
        try {
            // Extraer parámetros del request
            String buyOrder = request.get("buyOrder") != null ? request.get("buyOrder").toString() : null;
            String sessionId = request.get("sessionId") != null ? request.get("sessionId").toString() : null;
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            Long userId = request.containsKey("userId") ? Long.valueOf(request.get("userId").toString()) : null;
            
            // Crear request interno
            CreateWebpayOrderRequest orderRequest = CreateWebpayOrderRequest.builder()
                    .userId(userId)
                    .amount(amount)
                    .buyOrder(buyOrder)
                    .sessionId(sessionId)
                    .build();
            
            WebpayCreateResponse response = webpayService.initTransaction(orderRequest);
            
            // Respuesta en formato solicitado
            Map<String, Object> result = new HashMap<>();
            result.put("url", response.getUrl());
            result.put("token", response.getToken());
            result.put("orderId", response.getOrderId());
            
            log.info("Transacción creada: url={}, token={}, orderId={}", 
                    response.getUrl(), response.getToken(), response.getOrderId());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error al crear transacción Webpay: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", "Error al crear transacción: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * POST /api/payments/webpay/commit
     * 
     * Confirma una transacción después del retorno de Webpay.
     * 
     * Request body:
     * {
     *   "tokenWs": "TOKEN_WS_AQUI"
     * }
     * 
     * Response (autorizado):
     * {
     *   "status": "AUTHORIZED",
     *   "amount": 9990,
     *   "buyOrder": "ORDEN-123",
     *   "authorizationCode": "123456",
     *   "paymentTypeCode": "VD",
     *   "transactionDate": "2025-12-07T...",
     *   "installmentsNumber": 0
     * }
     */
    @PostMapping("/commit")
    public ResponseEntity<Map<String, Object>> commitTransaction(@RequestBody WebpayCommitRequest request) {
        String tokenWs = request.getTokenWs();
        log.info("POST /api/payments/webpay/commit - tokenWs: {}", tokenWs);
        
        try {
            WebpayCommitResult result = webpayService.handleReturn(tokenWs, null, null, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", result.getStatus());
            response.put("amount", result.getAmount());
            response.put("buyOrder", result.getBuyOrder());
            response.put("authorizationCode", result.getAuthorizationCode());
            response.put("paymentTypeCode", result.getPaymentTypeCode());
            response.put("transactionDate", result.getTransactionDate());
            response.put("installmentsNumber", result.getInstallmentsNumber());
            response.put("cardNumber", result.getCardNumber());
            response.put("orderId", result.getOrderId());
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                response.put("message", result.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("Error al confirmar transacción: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", "Error al confirmar transacción: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * POST /api/payments/webpay/return
     * 
     * Callback de retorno desde Webpay (Transbank redirige aquí).
     */
    @PostMapping("/return")
    public void handleReturnPost(
            @RequestParam(value = "token_ws", required = false) String tokenWs,
            @RequestParam(value = "TBK_TOKEN", required = false) String tbkToken,
            @RequestParam(value = "TBK_ORDEN_COMPRA", required = false) String tbkOrdenCompra,
            @RequestParam(value = "TBK_ID_SESION", required = false) String tbkIdSesion,
            HttpServletResponse response) throws IOException {
        
        log.info("POST /api/payments/webpay/return - token_ws: {}, TBK_TOKEN: {}", tokenWs, tbkToken);
        handleReturnInternal(tokenWs, tbkToken, tbkOrdenCompra, tbkIdSesion, response);
    }

    /**
     * Endpoint alternativo GET para el retorno (algunos navegadores pueden hacer GET).
     */
    @GetMapping("/return")
    public void handleReturnGet(
            @RequestParam(value = "token_ws", required = false) String tokenWs,
            @RequestParam(value = "TBK_TOKEN", required = false) String tbkToken,
            @RequestParam(value = "TBK_ORDEN_COMPRA", required = false) String tbkOrdenCompra,
            @RequestParam(value = "TBK_ID_SESION", required = false) String tbkIdSesion,
            HttpServletResponse response) throws IOException {
        handleReturnInternal(tokenWs, tbkToken, tbkOrdenCompra, tbkIdSesion, response);
    }

    private void handleReturnInternal(String tokenWs, String tbkToken, String tbkOrdenCompra,
                                       String tbkIdSesion, HttpServletResponse response) throws IOException {
        try {
            WebpayCommitResult result = webpayService.handleReturn(tokenWs, tbkToken, tbkOrdenCompra, tbkIdSesion);
            String redirectUrl = webpayService.getRedirectUrl(result);
            
            log.info("Redirigiendo a: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
            
        } catch (Exception e) {
            log.error("Error procesando retorno de Webpay: {}", e.getMessage(), e);
            response.sendRedirect(webpayService.getRedirectUrl(
                    WebpayCommitResult.builder()
                            .success(false)
                            .status("ERROR")
                            .message("Error procesando el pago: " + e.getMessage())
                            .build()
            ));
        }
    }

    /**
     * Obtiene el estado de una orden.
     * 
     * GET /api/payments/webpay/orders/{orderId}
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable Long orderId) {
        log.info("Consultando estado de orden: {}", orderId);
        
        WebpayOrder order = webpayService.getOrderById(orderId);
        
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getId());
        response.put("status", order.getStatus());
        response.put("amount", order.getTotalAmount());
        response.put("buyOrder", order.getBuyOrder());
        response.put("authorizationCode", order.getAuthorizationCode());
        response.put("paymentTypeCode", order.getPaymentTypeCode());
        response.put("installmentsNumber", order.getInstallmentsNumber());
        response.put("cardNumber", order.getCardNumber());
        response.put("transactionDate", order.getTransactionDate());
        response.put("createdAt", order.getCreatedAt());
        response.put("updatedAt", order.getUpdatedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * Health check del endpoint de Webpay.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Webpay Plus Integration");
        response.put("environment", "INTEGRATION");
        return ResponseEntity.ok(response);
    }
}
