package com.levelup.backend.dto;

import com.levelup.backend.sale.SaleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransbankCallbackResponse {
    
    private String message;
    private Long saleId;
    private SaleStatus status;
    private Integer total;
}
