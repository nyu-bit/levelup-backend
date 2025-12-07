package com.levelup.backend.webpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO con el resultado del commit de una transacción Webpay.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebpayCommitResult {
    
    /**
     * Indica si el pago fue exitoso.
     */
    private boolean success;
    
    /**
     * Estado de la transacción: AUTHORIZED, FAILED, ABORTED, TIMEOUT, ERROR.
     */
    private String status;
    
    /**
     * ID de la orden en el sistema.
     */
    private Long orderId;
    
    /**
     * Monto de la transacción.
     */
    private BigDecimal amount;
    
    /**
     * Código de autorización (si fue aprobada).
     */
    private String authorizationCode;
    
    /**
     * Tipo de pago: VD (débito), VN (crédito normal), VC (cuotas), etc.
     */
    private String paymentTypeCode;
    
    /**
     * Número de cuotas.
     */
    private Integer installmentsNumber;
    
    /**
     * Número de orden de compra enviado a Transbank.
     */
    private String buyOrder;
    
    /**
     * ID de sesión enviado a Transbank.
     */
    private String sessionId;
    
    /**
     * Fecha de la transacción.
     */
    private String transactionDate;
    
    /**
     * Últimos 4 dígitos de la tarjeta.
     */
    private String cardNumber;
    
    /**
     * Mensaje descriptivo del resultado.
     */
    private String message;
}
