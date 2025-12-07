package com.levelup.backend.webpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para la creación de una transacción Webpay.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebpayCreateResponse {
    
    /**
     * URL a la que se debe redirigir al usuario para completar el pago.
     */
    private String url;
    
    /**
     * Token de la transacción Webpay.
     */
    private String token;
    
    /**
     * ID de la orden creada en el sistema.
     */
    private Long orderId;
}
