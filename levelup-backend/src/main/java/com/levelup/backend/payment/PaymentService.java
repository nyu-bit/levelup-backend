package com.levelup.backend.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para procesar pagos a través del mock de Transbank.
 * Las llamadas se hacen desde el backend para evitar problemas de CORS.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RestTemplate restTemplate;

    @Value("${transbank.mock.url:https://webpay.mock/api/transaction}")
    private String transbankMockUrl;

    @Value("${transbank.return.url:https://levelupgamer.lol/pago/retorno}")
    private String returnUrl;

    /**
     * Procesa un pago llamando al mock de Transbank.
     *
     * @param orderId ID de la orden/venta
     * @param total   Monto total a pagar
     * @return PaymentResponse con el resultado del pago
     */
    public PaymentResponse procesarPagoConMock(String orderId, BigDecimal total) {
        log.info("Iniciando pago con mock de Transbank. OrderId: {}, Total: {}", orderId, total);

        // Construir el body de la petición
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("buy_order", orderId);
        requestBody.put("amount", total.intValue());
        requestBody.put("session_id", "LVLUP-" + orderId);
        requestBody.put("return_url", returnUrl);

        // Configurar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Llamando al mock de Transbank en: {}", transbankMockUrl);
            
            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                    transbankMockUrl,
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

        } catch (RestClientException e) {
            log.error("Error al llamar al mock de Transbank: {}", e.getMessage());
            
            // Retornar respuesta de error
            return PaymentResponse.builder()
                    .token(null)
                    .approved(false)
                    .status("ERROR")
                    .message("Error de conexión con el servicio de pago: " + e.getMessage())
                    .buyOrder(orderId)
                    .amount(total.intValue())
                    .build();
        }
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

        String statusUrl = transbankMockUrl + "/status/" + token;

        try {
            ResponseEntity<PaymentResponse> response = restTemplate.getForEntity(
                    statusUrl,
                    PaymentResponse.class
            );

            return response.getBody();

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

        String confirmUrl = transbankMockUrl + "/confirm";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("token", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                    confirmUrl,
                    HttpMethod.POST,
                    request,
                    PaymentResponse.class
            );

            return response.getBody();

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
