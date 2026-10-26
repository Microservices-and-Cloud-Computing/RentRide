package com.rentride.paymentservice.service.gateway;

import com.rentride.paymentservice.dto.PaymentProcessRequest;
import com.rentride.paymentservice.entity.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulator component modeling external acquiring bank / payment gateway behavior (e.g. Stripe, PayHere).
 *
 * <p>Deterministic evaluation rules:
 * <ul>
 *   <li>Cards ending in {@code 0000} &rarr; PAYMENT_DECLINED ("Insufficient funds")</li>
 *   <li>Cards ending in {@code 9999} &rarr; GATEWAY_TIMEOUT ("Acquiring network unresponsive")</li>
 *   <li>All other card numbers / alternative payment channels &rarr; APPROVED with gateway reference</li>
 * </ul>
 */
@Slf4j
@Component
public class PaymentGatewaySimulator {

    public static final String DECLINED_REASON_INSUFFICIENT_FUNDS = "Insufficient funds";
    public static final String DECLINED_REASON_GATEWAY_TIMEOUT = "Acquiring network unresponsive";
    public static final String RECEIPT_BASE_URL = "https://receipts.rentride.com/receipts/";

    /**
     * Simulates payment gateway authorization and settlement.
     *
     * @param transactionReference unique RentRide business transaction reference
     * @param request              payment parameters including method and masked details
     * @return {@link GatewayResult} containing outcome, gateway reference or failure reason
     */
    public GatewayResult process(String transactionReference, PaymentProcessRequest request) {
        log.info("Processing gateway authorization: txnRef={}, method={}, amount={} {}, card={}",
                transactionReference,
                request.getPaymentMethod(),
                request.getAmount(),
                request.getCurrency(),
                request.getMaskedCardNumber());

        if (request.getPaymentMethod() == PaymentMethod.CREDIT_CARD || request.getPaymentMethod() == PaymentMethod.DEBIT_CARD) {
            String cardNumber = request.getCardNumber();
            if (cardNumber != null) {
                if (cardNumber.endsWith("0000")) {
                    log.warn("Gateway declined transaction {}: Insufficient funds", transactionReference);
                    return GatewayResult.failed(DECLINED_REASON_INSUFFICIENT_FUNDS);
                }
                if (cardNumber.endsWith("9999")) {
                    log.warn("Gateway timeout for transaction {}: Acquiring network unresponsive", transactionReference);
                    return GatewayResult.failed(DECLINED_REASON_GATEWAY_TIMEOUT);
                }
            }
        }

        String gatewayTxnId = "GW-TXN-" + UUID.randomUUID();
        String receiptUrl = RECEIPT_BASE_URL + transactionReference + ".pdf";

        log.info("Gateway approved transaction {}: gatewayTxnId={}", transactionReference, gatewayTxnId);
        return GatewayResult.success(gatewayTxnId, receiptUrl);
    }
}

