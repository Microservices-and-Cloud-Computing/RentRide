package com.rentride.paymentservice.controller;

import com.rentride.paymentservice.dto.PaymentProcessRequest;
import com.rentride.paymentservice.dto.PaymentResponse;
import com.rentride.paymentservice.dto.TransactionVerifyResponse;
import com.rentride.paymentservice.entity.PaymentStatus;
import com.rentride.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing checkout execution, transaction verification,
 * and reservation payment tracking endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Executes payment checkout for a vehicle reservation.
     *
     * @param customerId ID of the authenticated user injected by API Gateway header
     * @param request    payment parameters and card/channel details
     * @return 200 OK on approved transaction, or 402 Payment Required if declined by gateway
     */
    @PostMapping("/checkout")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader(value = "X-User-Id", required = false) Long customerId,
            @Valid @RequestBody PaymentProcessRequest request) {

        log.info("REST POST /api/v1/payments/checkout: customerId={}, bookingId={}, amount={}",
                customerId, request.getBookingId(), request.getAmount());

        PaymentResponse response = paymentService.processPayment(customerId, request);

        if (response.getStatus() == PaymentStatus.FAILED) {
            log.warn("REST /checkout completed with FAILED payment: txnRef={}, reason={}",
                    response.getTransactionReference(), response.getFailureReason());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Public transaction verification endpoint.
     *
     * @param transactionReference unique business transaction reference
     * @return 200 OK with TransactionVerifyResponse
     */
    @GetMapping("/verify/{transactionReference}")
    public ResponseEntity<TransactionVerifyResponse> verifyTransaction(
            @PathVariable("transactionReference") String transactionReference) {

        log.info("REST GET /api/v1/payments/verify/{}", transactionReference);
        TransactionVerifyResponse response = paymentService.verifyTransaction(transactionReference);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the latest payment attempt details for a given booking reservation.
     *
     * @param bookingId reservation identifier
     * @return 200 OK with PaymentResponse
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(
            @PathVariable("bookingId") Long bookingId) {

        log.info("REST GET /api/v1/payments/booking/{}", bookingId);
        PaymentResponse response = paymentService.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(response);
    }
}

