package com.levelup.backend.webpay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entidad que representa un item dentro de una orden Webpay.
 */
@Entity
@Table(name = "webpay_order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebpayOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Orden a la que pertenece este item.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private WebpayOrder order;

    /**
     * ID del producto.
     */
    @Column(name = "product_id")
    private Long productId;

    /**
     * Nombre del producto.
     */
    @Column(name = "product_name", length = 200)
    private String productName;

    /**
     * Cantidad.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Precio unitario.
     */
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    /**
     * Precio total del item (quantity * unitPrice).
     */
    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    /**
     * Calcula el precio total.
     */
    @PrePersist
    @PreUpdate
    protected void calculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
