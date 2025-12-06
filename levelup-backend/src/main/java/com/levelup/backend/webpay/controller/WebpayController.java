package com.levelup.backend.webpay.controller;

import com.levelup.backend.webpay.dto.CreateWebpayOrderRequest;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para integración con Webpay Plus.
 * Maneja la creación de transacciones y el callback de retorno.
 */
@RestController
@RequestMapping("/api/payments/webpay")
@RequiredArgsConstructor
@Slf4j
public class WebpayController {

    private final WebpayService webpayService;

    /**
     * Crea una nueva transacción Webpay.
     * 
     * POST /api/payments/webpay/create
     * 
     * Request body:
     * {
     *   "userId": 1,
     *   "amount": 15000,
     *   "items": [
     *     { "productId": 1, "productName": "Game A", "quantity": 2, "unitPrice": 5000 }
     *   ],
     *   "shippingAddress": "Calle 123, Santiago"
     * }
     * 
     * Response:
     * {
     *   "url": "https://webpay3gint.transbank.cl/...",
     *   "token": "abc123...",
     *   "orderId": 1
     * }
     */
    @PostMapping("/create")
    public ResponseEntity<WebpayCreateResponse> createTransaction(@RequestBody CreateWebpayOrderRequest request) {
        log.info("Recibida solicitud de creación de transacción: userId={}, amount={}", 
                request.getUserId(), request.getAmount());
        
        try {
            WebpayCreateResponse response = webpayService.initTransaction(request);
            log.info("Transacción creada exitosamente: orderId={}, token={}", 
                    response.getOrderId(), response.getToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al crear transacción: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear transacción: " + e.getMessage(), e);
        }
    }

    /**
     * Callback de retorno desde Webpay.
     * Webpay hace POST a esta URL después del pago.
     * 
     * Parámetros posibles:
     * - token_ws: Token de transacción exitosa/rechazada
     * - TBK_TOKEN: Token cuando usuario aborta
     * - TBK_ORDEN_COMPRA: Buy order (abort/timeout)
     * - TBK_ID_SESION: Session ID (timeout)
     */
    @PostMapping("/return")
    public void handleReturn(
            @RequestParam(value = "token_ws", required = false) String tokenWs,
            @RequestParam(value = "TBK_TOKEN", required = false) String tbkToken,
            @RequestParam(value = "TBK_ORDEN_COMPRA", required = false) String tbkOrdenCompra,
            @RequestParam(value = "TBK_ID_SESION", required = false) String tbkIdSesion,
            HttpServletResponse response) throws IOException {
        
        log.info("Recibido callback de Webpay - token_ws: {}, TBK_TOKEN: {}, TBK_ORDEN_COMPRA: {}, TBK_ID_SESION: {}", 
                tokenWs, tbkToken, tbkOrdenCompra, tbkIdSesion);

        try {
            WebpayCommitResult result = webpayService.handleReturn(tokenWs, tbkToken, tbkOrdenCompra, tbkIdSesion);
            String redirectUrl = webpayService.getRedirectUrl(result);
            
            log.info("Redirigiendo a: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
            
        } catch (Exception e) {
            log.error("Error procesando retorno de Webpay: {}", e.getMessage(), e);
            // Redirigir a página de error
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
     * Endpoint alternativo GET para el retorno (algunos navegadores pueden hacer GET).
     */
    @GetMapping("/return")
    public void handleReturnGet(
            @RequestParam(value = "token_ws", required = false) String tokenWs,
            @RequestParam(value = "TBK_TOKEN", required = false) String tbkToken,
            @RequestParam(value = "TBK_ORDEN_COMPRA", required = false) String tbkOrdenCompra,
            @RequestParam(value = "TBK_ID_SESION", required = false) String tbkIdSesion,
            HttpServletResponse response) throws IOException {
        handleReturn(tokenWs, tbkToken, tbkOrdenCompra, tbkIdSesion, response);
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
