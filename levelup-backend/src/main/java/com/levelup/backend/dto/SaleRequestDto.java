package com.levelup.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para crear una venta con integración al mock de Transbank.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequestDto {

    @NotEmpty(message = "La venta debe tener al menos un item")
    @Valid
    private List<SaleItemRequest> items;

    /**
     * Si es true, usa el mock local (sin llamada HTTP externa).
     * Si es false o null, llama al mock externo de Transbank.
     */
    private Boolean useMockLocal;

    /**
     * Incluir costo de envío (por defecto true)
     */
    private Boolean includeShipping;
}
