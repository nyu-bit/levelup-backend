package com.levelup.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequest {
    
    @NotEmpty(message = "La venta debe tener al menos un item")
    @Valid
    private List<SaleItemRequest> items;
}
