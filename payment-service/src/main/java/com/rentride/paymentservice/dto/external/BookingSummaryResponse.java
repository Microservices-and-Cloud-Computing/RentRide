package com.rentride.paymentservice.dto.external;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Summary DTO representing an upstream vehicle reservation fetched from booking-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSummaryResponse {

    private Long id;
    private String bookingReference;
    private String status;
    private BigDecimal totalCost;
    private Long customerId;
}

