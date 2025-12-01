package com.levelup.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitTransactionResponse {
    
    private String token;
    private String url;
    private Long saleId;
    private Integer subtotal;
    private Integer iva;
    private Integer shipping;
    private Integer total;
}
