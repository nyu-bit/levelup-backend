package com.levelup.backend.webpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para confirmar una transacción Webpay.
 * 
 * POST /api/payments/webpay/commit
 * {
 *   "tokenWs": "TOKEN_WS_AQUI"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebpayCommitRequest {
    
    /**
     * Token de Webpay recibido en el retorno.
     */
    private String tokenWs;
}
