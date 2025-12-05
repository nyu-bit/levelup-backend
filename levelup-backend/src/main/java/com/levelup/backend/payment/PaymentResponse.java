package com.levelup.backend.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta del servicio de pago Transbank (mock).
 * Mapea los campos típicos de una respuesta de Webpay.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResponse {

    /**
     * Token único de la transacción
     */
    private String token;

    /**
     * Indica si el pago fue aprobado
     */
    private boolean approved;

    /**
     * Estado de la transacción: AUTHORIZED, FAILED, PENDING, ERROR
     */
    private String status;

    /**
     * Mensaje descriptivo del resultado
     */
    private String message;

    /**
     * Número de orden de compra
     */
    @JsonProperty("buy_order")
    private String buyOrder;

    /**
     * ID de sesión
     */
    @JsonProperty("session_id")
    private String sessionId;

    /**
     * Monto de la transacción
     */
    private Integer amount;

    /**
     * Código de autorización (si fue aprobado)
     */
    @JsonProperty("authorization_code")
    private String authorizationCode;

    /**
     * Código de respuesta (0 = aprobado)
     */
    @JsonProperty("response_code")
    private Integer responseCode;

    /**
     * Tipo de pago: VD (débito), VN (crédito normal), etc.
     */
    @JsonProperty("payment_type_code")
    private String paymentTypeCode;

    /**
     * Número de cuotas (0 para débito o pago contado)
     */
    @JsonProperty("installments_number")
    private Integer installmentsNumber;

    /**
     * Últimos 4 dígitos de la tarjeta
     */
    @JsonProperty("card_number")
    private String cardNumber;

    /**
     * Fecha de la transacción
     */
    @JsonProperty("transaction_date")
    private String transactionDate;

    /**
     * URL de redirección para completar el pago
     */
    @JsonProperty("redirect_url")
    private String redirectUrl;

    /**
     * URL del formulario de Webpay (para init)
     */
    @JsonProperty("url")
    private String url;

    /**
     * Verifica si la transacción fue exitosa basándose en múltiples campos
     */
    public boolean isSuccessful() {
        return approved || 
               "AUTHORIZED".equalsIgnoreCase(status) || 
               (responseCode != null && responseCode == 0);
    }
}
