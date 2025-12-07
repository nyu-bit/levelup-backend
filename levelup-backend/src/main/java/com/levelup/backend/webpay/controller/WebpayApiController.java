package com.levelup.backend.webpay.controller;

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
 * Controlador REST para Webpay Plus.
 * Expone los endpoints en /api/webpay (ruta solicitada por el frontend).
 */
@RestController
@RequestMapping("/api/webpay")
@RequiredArgsConstructor
@Slf4j
public class WebpayApiController {

    private final WebpayService webpayService;

    /**
     * POST /api/webpay/create
     * 
     * Crea una nueva transacción en Webpay Plus.
     * 
     * Request body:
     * {
     *   "buyOrder": "ORDER123",
     *   "sessionId": "session-abc",
     *   "amount": 15000
     * }
     * 
     * Response:
     * {
     *   "url": "https://webpay3gint.transbank.cl/...",
     *   "token": "abc123..."
     * }
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createTransaction(@RequestBody Map<String, Object> request) {
        log.info("POST /api/webpay/create - Recibida solicitud: {}", request);
        
        try {
            // Extraer parámetros del request
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            Long userId = request.containsKey("userId") ? Long.valueOf(request.get("userId").toString()) : null;
            
            // Crear request interno
            com.levelup.backend.webpay.dto.CreateWebpayOrderRequest orderRequest = 
                    com.levelup.backend.webpay.dto.CreateWebpayOrderRequest.builder()
                            .userId(userId)
                            .amount(amount)
                            .build();
            
            WebpayCreateResponse response = webpayService.initTransaction(orderRequest);
            
            // Respuesta en formato simple
            Map<String, Object> result = new HashMap<>();
            result.put("url", response.getUrl());
            result.put("token", response.getToken());
            result.put("orderId", response.getOrderId());
            
            log.info("Transacción creada: url={}, token={}", response.getUrl(), response.getToken());
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
     * POST /api/webpay/commit
     * 
     * Confirma una transacción después del retorno de Webpay.
     * 
     * Request body:
     * {
     *   "token_ws": "abc123..."
     * }
     */
    @PostMapping("/commit")
    public ResponseEntity<Map<String, Object>> commitTransaction(@RequestBody Map<String, String> request) {
        String tokenWs = request.get("token_ws");
        log.info("POST /api/webpay/commit - token_ws: {}", tokenWs);
        
        try {
            WebpayCommitResult result = webpayService.handleReturn(tokenWs, null, null, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("status", result.getStatus());
            response.put("orderId", result.getOrderId());
            response.put("amount", result.getAmount());
            response.put("authorizationCode", result.getAuthorizationCode());
            response.put("paymentTypeCode", result.getPaymentTypeCode());
            response.put("buyOrder", result.getBuyOrder());
            response.put("transactionDate", result.getTransactionDate());
            response.put("cardNumber", result.getCardNumber());
            response.put("message", result.getMessage());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error al confirmar transacción: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", true);
            error.put("message", "Error al confirmar transacción: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * GET/POST /api/webpay/return
     * 
     * Callback de retorno desde Webpay.
     * Webpay redirige aquí después del pago.
     */
    @PostMapping("/return")
    public void handleReturnPost(
            @RequestParam(value = "token_ws", required = false) String tokenWs,
            @RequestParam(value = "TBK_TOKEN", required = false) String tbkToken,
            @RequestParam(value = "TBK_ORDEN_COMPRA", required = false) String tbkOrdenCompra,
            @RequestParam(value = "TBK_ID_SESION", required = false) String tbkIdSesion,
            HttpServletResponse response) throws IOException {
        
        log.info("POST /api/webpay/return - token_ws: {}, TBK_TOKEN: {}", tokenWs, tbkToken);
        handleReturn(tokenWs, tbkToken, tbkOrdenCompra, tbkIdSesion, response);
    }

    @GetMapping("/return")
    public void handleReturnGet(
            @RequestParam(value = "token_ws", required = false) String tokenWs,
            @RequestParam(value = "TBK_TOKEN", required = false) String tbkToken,
            @RequestParam(value = "TBK_ORDEN_COMPRA", required = false) String tbkOrdenCompra,
            @RequestParam(value = "TBK_ID_SESION", required = false) String tbkIdSesion,
            HttpServletResponse response) throws IOException {
        
        log.info("GET /api/webpay/return - token_ws: {}, TBK_TOKEN: {}", tokenWs, tbkToken);
        handleReturn(tokenWs, tbkToken, tbkOrdenCompra, tbkIdSesion, response);
    }

    private void handleReturn(String tokenWs, String tbkToken, String tbkOrdenCompra, 
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
     * GET /api/webpay/status/{orderId}
     * 
     * Obtiene el estado de una orden.
     */
    @GetMapping("/status/{orderId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long orderId) {
        log.info("GET /api/webpay/status/{}", orderId);
        
        WebpayOrder order = webpayService.getOrderById(orderId);
        
        if (order == null) {
            Map<String, Object> notFound = new HashMap<>();
            notFound.put("error", true);
            notFound.put("message", "Orden no encontrada");
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getId());
        response.put("status", order.getStatus());
        response.put("amount", order.getTotalAmount());
        response.put("buyOrder", order.getBuyOrder());
        response.put("authorizationCode", order.getAuthorizationCode());
        response.put("paymentTypeCode", order.getPaymentTypeCode());
        response.put("cardNumber", order.getCardNumber());
        response.put("transactionDate", order.getTransactionDate());
        response.put("createdAt", order.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/webpay/health
     * 
     * Health check del servicio Webpay.
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
