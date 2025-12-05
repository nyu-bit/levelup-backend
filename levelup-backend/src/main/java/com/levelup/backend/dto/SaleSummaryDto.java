package com.levelup.backend.dto;

import com.levelup.backend.sale.SaleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO resumen de una venta procesada con Transbank mock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleSummaryDto {

    /**
     * Número de orden único (UUID)
     */
    private String orderNumber;

    /**
     * ID de la venta en la base de datos
     */
    private Long saleId;

    /**
     * Estado de la venta: PENDING, APPROVED (PAGADO), REJECTED (RECHAZADO)
     */
    private SaleStatus status;

    /**
     * Estado en texto legible
     */
    private String statusDescription;

    /**
     * Subtotal sin IVA ni envío
     */
    private Integer subtotal;

    /**
     * IVA (19%)
     */
    private Integer iva;

    /**
     * Costo de envío
     */
    private Integer shipping;

    /**
     * Total de la venta
     */
    private Integer total;

    /**
     * Token de Transbank (para consultas posteriores)
     */
    private String transbankToken;

    /**
     * Código de autorización (si fue aprobado)
     */
    private String authorizationCode;

    /**
     * Mensaje del resultado del pago
     */
    private String paymentMessage;

    /**
     * Fecha de creación
     */
    private LocalDateTime createdAt;

    /**
     * Cantidad de items en la venta
     */
    private Integer itemsCount;

    /**
     * Lista de items (opcional, para respuesta detallada)
     */
    private List<SaleItemSummary> items;

    /**
     * Resumen de un item de venta
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemSummary {
        private Long productId;
        private String productName;
        private Integer quantity;
        private Integer unitPrice;
        private Integer totalPrice;
    }

    /**
     * Helper para obtener descripción del estado
     */
    public static String getStatusDescription(SaleStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente de pago";
            case APPROVED -> "Pagado";
            case REJECTED -> "Rechazado";
        };
    }
}
