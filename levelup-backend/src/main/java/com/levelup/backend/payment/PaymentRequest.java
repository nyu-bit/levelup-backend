package com.levelup.backend.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request para iniciar un pago con el mock de Transbank.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank(message = "El orderId es obligatorio")
    private String orderId;

    @NotNull(message = "El total es obligatorio")
    @DecimalMin(value = "1", message = "El total debe ser mayor a 0")
    private BigDecimal total;

    /**
     * Usar mock local (sin llamada HTTP externa)
     */
    private boolean useMockLocal;
}
