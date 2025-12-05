package com.levelup.backend.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para procesar pagos a través del mock de Transbank.
 * Las llamadas se hacen desde el backend para evitar problemas de CORS.
 * 
 * Configuración en application.properties:
 * - payment.transbank.mock-base-url: URL base del mock
 * - payment.transbank.return-url: URL de retorno al frontend
 * - payment.transbank.callback-url: URL de callback del backend
 */
@Service
@Slf4j
public class PaymentService {

    private final RestTemplate restTemplate;

    // ============================================
    // Propiedades inyectadas desde application.properties
    // Cambiar según ambiente (dev, ec2, prod)
    // ============================================
    
    @Value("${payment.transbank.mock-base-url:https://webpay.mock/api}")
    private String transbankMockBaseUrl;

    @Value("${payment.transbank.mock-init-endpoint:${payment.transbank.mock-base-url}/transaction}")
    private String transbankInitEndpoint;

    @Value("${payment.transbank.mock-status-endpoint:${payment.transbank.mock-base-url}/status}")
    private String transbankStatusEndpoint;

    @Value("${payment.transbank.mock-confirm-endpoint:${payment.transbank.mock-base-url}/confirm}")
    private String transbankConfirmEndpoint;

    @Value("${payment.transbank.return-url:https://levelupgamer.lol/pago/retorno}")
    private String returnUrl;

    @Value("${payment.transbank.callback-url:}")
    private String callbackUrl;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public PaymentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Procesa un pago llamando al mock de Transbank.
     *
     * @param orderId ID de la orden/venta
     * @param total   Monto total a pagar
     * @return PaymentResponse con el resultado del pago
     */
    public PaymentResponse procesarPagoConMock(String orderId, BigDecimal total) {
        log.info("Iniciando pago con mock de Transbank. OrderId: {}, Total: {}", orderId, total);
        log.debug("URL del mock: {}", transbankInitEndpoint);

        // Construir el body de la petición
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("buy_order", orderId);
        requestBody.put("amount", total.intValue());
        requestBody.put("session_id", "LVLUP-" + orderId);
        requestBody.put("return_url", returnUrl);
        
        // Agregar callback URL si está configurada
        if (callbackUrl != null && !callbackUrl.isEmpty()) {
            requestBody.put("callback_url", callbackUrl);
        }

        // Configurar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Llamando al mock de Transbank en: {}", transbankInitEndpoint);
            
            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                    transbankInitEndpoint,
                    HttpMethod.POST,
                    request,
                    PaymentResponse.class
            );

            PaymentResponse paymentResponse = response.getBody();
            
            if (paymentResponse != null) {
                log.info("Respuesta del mock - Token: {}, Approved: {}", 
                        paymentResponse.getToken(), paymentResponse.isApproved());
            }

            return paymentResponse;

        } catch (HttpClientErrorException e) {
            // Errores 4xx (Bad Request, Not Found, etc.)
            log.error("Error del cliente al llamar al mock ({}): {}", e.getStatusCode(), e.getMessage());
            return buildErrorResponse(orderId, total, 
                    "Error en la solicitud de pago: " + e.getStatusCode().value());
            
        } catch (HttpServerErrorException e) {
            // Errores 5xx (Internal Server Error, etc.)
            log.error("Error del servidor mock ({}): {}", e.getStatusCode(), e.getMessage());
            return buildErrorResponse(orderId, total, 
                    "El servicio de pago no está disponible. Intente nuevamente.");
            
        } catch (ResourceAccessException e) {
            // Timeout o conexión rechazada
            log.error("Error de conexión con el mock: {}", e.getMessage());
            return buildErrorResponse(orderId, total, 
                    "No se pudo conectar con el servicio de pago. Verifique su conexión e intente nuevamente.");
            
        } catch (RestClientException e) {
            // Otros errores de RestTemplate
            log.error("Error al llamar al mock de Transbank: {}", e.getMessage());
            return buildErrorResponse(orderId, total, 
                    "Error al procesar el pago. Intente nuevamente.");
        }
    }

    /**
     * Construye una respuesta de error estandarizada.
     */
    private PaymentResponse buildErrorResponse(String orderId, BigDecimal total, String message) {
        return PaymentResponse.builder()
                .token(null)
                .approved(false)
                .status("ERROR")
                .message(message)
                .buyOrder(orderId)
                .amount(total.intValue())
                .build();
    }

    /**
     * Procesa un pago con mock simulado localmente (sin llamada HTTP externa).
     * Útil para testing o cuando no hay mock externo disponible.
     *
     * @param orderId ID de la orden/venta
     * @param total   Monto total a pagar
     * @return PaymentResponse simulado
     */
    public PaymentResponse procesarPagoMockLocal(String orderId, BigDecimal total) {
        log.info("Procesando pago con mock LOCAL. OrderId: {}, Total: {}", orderId, total);

        // Generar token simulado
        String token = "tbk_" + UUID.randomUUID().toString().substring(0, 8);
        String sessionId = "LVLUP-" + orderId;

        // Simular aprobación (siempre aprobado en mock local)
        return PaymentResponse.builder()
                .token(token)
                .approved(true)
                .status("AUTHORIZED")
                .message("Pago aprobado exitosamente")
                .buyOrder(orderId)
                .sessionId(sessionId)
                .amount(total.intValue())
                .authorizationCode("AUTH-" + System.currentTimeMillis())
                .responseCode(0)
                .paymentTypeCode("VD")
                .installmentsNumber(0)
                .redirectUrl(returnUrl + "?token=" + token)
                .build();
    }

    /**
     * Consulta el estado de una transacción por token.
     *
     * @param token Token de la transacción
     * @return PaymentResponse con el estado actual
     */
    public PaymentResponse consultarEstado(String token) {
        log.info("Consultando estado de transacción. Token: {}", token);

        String statusUrl = transbankStatusEndpoint + "/" + token;

        try {
            log.debug("Consultando estado en: {}", statusUrl);
            ResponseEntity<PaymentResponse> response = restTemplate.getForEntity(
                    statusUrl,
                    PaymentResponse.class
            );

            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error HTTP al consultar estado ({}): {}", e.getStatusCode(), e.getMessage());
            return PaymentResponse.builder()
                    .token(token)
                    .approved(false)
                    .status("ERROR")
                    .message("Error al consultar estado del pago: " + e.getStatusCode().value())
                    .build();
                    
        } catch (ResourceAccessException e) {
            log.error("Error de conexión al consultar estado: {}", e.getMessage());
            return PaymentResponse.builder()
                    .token(token)
                    .approved(false)
                    .status("ERROR")
                    .message("No se pudo conectar con el servicio de pago.")
                    .build();
                    
        } catch (RestClientException e) {
            log.error("Error al consultar estado: {}", e.getMessage());
            return PaymentResponse.builder()
                    .token(token)
                    .approved(false)
                    .status("ERROR")
                    .message("Error al consultar estado: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Confirma una transacción después del retorno de Webpay.
     *
     * @param token Token de la transacción a confirmar
     * @return PaymentResponse con el resultado de la confirmación
     */
    public PaymentResponse confirmarTransaccion(String token) {
        log.info("Confirmando transacción. Token: {}", token);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("token", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("Confirmando transacción en: {}", transbankConfirmEndpoint);
            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                    transbankConfirmEndpoint,
                    HttpMethod.POST,
                    request,
                    PaymentResponse.class
            );

            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error HTTP al confirmar transacción ({}): {}", e.getStatusCode(), e.getMessage());
            return PaymentResponse.builder()
                    .token(token)
                    .approved(false)
                    .status("ERROR")
                    .message("Error al confirmar el pago: " + e.getStatusCode().value())
                    .build();
                    
        } catch (ResourceAccessException e) {
            log.error("Error de conexión al confirmar: {}", e.getMessage());
            return PaymentResponse.builder()
                    .token(token)
                    .approved(false)
                    .status("ERROR")
                    .message("No se pudo conectar con el servicio de pago.")
                    .build();
                    
        } catch (RestClientException e) {
            log.error("Error al confirmar transacción: {}", e.getMessage());
            return PaymentResponse.builder()
                    .token(token)
                    .approved(false)
                    .status("ERROR")
                    .message("Error al confirmar transacción: " + e.getMessage())
                    .build();
        }
    }
}
