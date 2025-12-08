package com.levelup.backend.webpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para solicitar la creación de una orden Webpay.
 * 
 * POST /api/payments/webpay/create
 * {
 *   "buyOrder": "ORDEN-123",
 *   "sessionId": "SESSION-123",
 *   "amount": 9990
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWebpayOrderRequest {
    
    /**
     * Número de orden de compra (máximo 26 caracteres).
     * Si no se proporciona, se genera automáticamente.
     */
    private String buyOrder;
    
    /**
     * ID de sesión (máximo 61 caracteres).
     * Si no se proporciona, se genera automáticamente.
     */
    private String sessionId;
    
    /**
     * Monto total de la orden.
     */
    private BigDecimal amount;
    
    /**
     * ID del usuario que realiza la compra (opcional).
     */
    private Long userId;
    
    /**
     * Lista de items de la orden (opcional).
     */
    private List<OrderItemRequest> items;
    
    /**
     * Dirección de envío (opcional).
     */
    private String shippingAddress;

    /**
     * DTO para un item de la orden.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
