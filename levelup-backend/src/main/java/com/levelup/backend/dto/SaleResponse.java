package com.levelup.backend.dto;

import com.levelup.backend.sale.SaleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponse {
    
    private Long saleId;
    private SaleStatus status;
    private Integer subtotal;
    private Integer iva;
    private Integer shipping;
    private Integer total;
    private String transbankToken;
    private LocalDateTime createdAt;
    private List<SaleItemResponse> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleItemResponse {
        private Long productId;
        private String productName;
        private Integer quantity;
        private Integer unitPrice;
        private Integer totalPrice;
    }
}
