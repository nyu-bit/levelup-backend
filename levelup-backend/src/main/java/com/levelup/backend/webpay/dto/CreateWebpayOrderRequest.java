package com.levelup.backend.webpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para solicitar la creación de una orden Webpay.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWebpayOrderRequest {
    
    /**
     * ID del usuario que realiza la compra (opcional).
     */
    private Long userId;
    
    /**
     * Monto total de la orden.
     */
    private BigDecimal amount;
    
    /**
     * Lista de items de la orden.
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
