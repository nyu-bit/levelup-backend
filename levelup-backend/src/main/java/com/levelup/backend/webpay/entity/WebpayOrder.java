package com.levelup.backend.webpay.entity;

import com.levelup.backend.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una orden de pago procesada con Webpay.
 * Almacena toda la información relacionada con la transacción.
 */
@Entity
@Table(name = "webpay_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebpayOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario que realizó la orden.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Monto total de la orden.
     */
    @Column(nullable = false)
    private BigDecimal totalAmount;

    /**
     * Estado de la orden.
     * PENDING, AUTHORIZED, FAILED, ABORTED, TIMEOUT, ERROR
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /**
     * Buy Order enviado a Transbank (ej: "ORDER-123").
     */
    @Column(name = "buy_order", length = 50)
    private String buyOrder;

    /**
     * Session ID enviado a Transbank.
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * Token de la transacción Webpay.
     */
    @Column(name = "webpay_token", length = 100)
    private String webpayToken;

    /**
     * Código de autorización (si fue aprobada).
     */
    @Column(name = "authorization_code", length = 20)
    private String authorizationCode;

    /**
     * Tipo de pago: VD, VN, VC, SI, S2, NC, etc.
     */
    @Column(name = "payment_type_code", length = 10)
    private String paymentTypeCode;

    /**
     * Número de cuotas.
     */
    @Column(name = "installments_number")
    private Integer installmentsNumber;

    /**
     * Código de respuesta de Transbank.
     */
    @Column(name = "response_code")
    private Integer responseCode;

    /**
     * Últimos 4 dígitos de la tarjeta.
     */
    @Column(name = "card_number", length = 20)
    private String cardNumber;

    /**
     * Fecha de la transacción en Transbank.
     */
    @Column(name = "transaction_date")
    private String transactionDate;

    /**
     * Dirección de envío (opcional).
     */
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    /**
     * Notas adicionales.
     */
    @Column(length = 1000)
    private String notes;

    /**
     * Fecha de creación de la orden.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Fecha de última actualización.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Items de la orden.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WebpayOrderItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Helper para agregar items.
     */
    public void addItem(WebpayOrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /**
     * Constantes de estado.
     */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_AUTHORIZED = "AUTHORIZED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_ABORTED = "ABORTED";
    public static final String STATUS_TIMEOUT = "TIMEOUT";
    public static final String STATUS_ERROR = "ERROR";
}
