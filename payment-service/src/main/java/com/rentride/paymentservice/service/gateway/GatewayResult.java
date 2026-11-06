package com.rentride.paymentservice.service.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsulates the response from the payment gateway acquisition engine.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayResult {

    private boolean successful;
    private String gatewayTransactionId;
    private String failureReason;
    private String receiptUrl;

    public static GatewayResult success(String gatewayTransactionId, String receiptUrl) {
        return GatewayResult.builder()
                .successful(true)
                .gatewayTransactionId(gatewayTransactionId)
                .receiptUrl(receiptUrl)
                .build();
    }

    public static GatewayResult failed(String failureReason) {
        return GatewayResult.builder()
                .successful(false)
                .failureReason(failureReason)
                .build();
    }
}

