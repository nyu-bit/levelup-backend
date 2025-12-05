package com.levelup.backend.payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para el servicio de pagos Transbank mock.
 * Todas las llamadas al mock se hacen desde el backend para evitar CORS.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Inicia un pago llamando al mock de Transbank.
     * POST /api/v1/payments/init
     */
    @PostMapping("/init")
    public ResponseEntity<PaymentResponse> initPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Iniciando pago para orden: {}", request.getOrderId());

        PaymentResponse response;
        
        if (request.isUseMockLocal()) {
            // Usar mock local (sin llamada HTTP externa)
            response = paymentService.procesarPagoMockLocal(
                    request.getOrderId(),
                    request.getTotal()
            );
        } else {
            // Llamar al mock externo
            response = paymentService.procesarPagoConMock(
                    request.getOrderId(),
                    request.getTotal()
            );
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Consulta el estado de una transacción.
     * GET /api/v1/payments/status/{token}
     */
    @GetMapping("/status/{token}")
    public ResponseEntity<PaymentResponse> getStatus(@PathVariable String token) {
        log.info("Consultando estado de pago: {}", token);
        PaymentResponse response = paymentService.consultarEstado(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirma una transacción después del retorno de Webpay.
     * POST /api/v1/payments/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@RequestParam String token) {
        log.info("Confirmando transacción: {}", token);
        PaymentResponse response = paymentService.confirmarTransaccion(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de prueba con mock local (siempre funciona).
     * POST /api/v1/payments/test
     */
    @PostMapping("/test")
    public ResponseEntity<PaymentResponse> testPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Pago de prueba para orden: {}", request.getOrderId());
        
        PaymentResponse response = paymentService.procesarPagoMockLocal(
                request.getOrderId(),
                request.getTotal()
        );

        return ResponseEntity.ok(response);
    }
}
