package com.levelup.backend.dto;

import com.levelup.backend.sale.SaleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {
    
    private Long saleId;
    private String token;
    private SaleStatus status;
    private Integer subtotal;
    private Integer iva;
    private Integer shipping;
    private Integer total;
    private LocalDateTime createdAt;
    private Integer itemsCount;
}
