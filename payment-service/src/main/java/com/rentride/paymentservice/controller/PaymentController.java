package com.rentride.paymentservice.controller;

import com.rentride.paymentservice.dto.PaymentProcessRequest;
import com.rentride.paymentservice.dto.PaymentResponse;
import com.rentride.paymentservice.dto.TransactionVerifyResponse;
import com.rentride.paymentservice.entity.PaymentStatus;
import com.rentride.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@Tag(
        name = "Payment Management",
        description = "Endpoints for processing rental reservation payments, " +
                      "verifying transactions, and checking payment history"
)
public class PaymentController {

    private final PaymentService paymentService;

    // ──────────────────────────────────────────────────────────────────────────
    // POST /checkout
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Executes payment checkout for a vehicle reservation.
     *
     * @param customerId ID of the authenticated user injected by API Gateway header
     * @param request    payment parameters and card/channel details
     * @return 200 OK on approved transaction, or 402 Payment Required if declined by gateway
     */
    @Operation(
            summary = "Process rental payment checkout",
            description = """
                    Initiates a simulated card-gateway payment for the specified booking reservation.

                    **Test card numbers:**
                    - `4111111111114242` → payment **APPROVED**
                    - `4111111111110000` → payment **DECLINED** (insufficient funds)

                    The `X-User-Id` header is normally injected by the API Gateway; supply it manually
                    when calling this endpoint directly (e.g. `1`).
                    """,
            security = @SecurityRequirement(name = "X-User-Id")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment processed — response includes transaction reference, status, and receipt URL",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentResponse.class),
                            examples = @ExampleObject(
                                    name = "Approved payment",
                                    value = """
                                            {
                                              "id": 1,
                                              "transactionReference": "TXN-20241025-A1B2C3",
                                              "bookingId": 101,
                                              "customerId": 1,
                                              "amount": 17000.00,
                                              "currency": "LKR",
                                              "paymentMethod": "CREDIT_CARD",
                                              "status": "COMPLETED",
                                              "gatewayTransactionId": "GW-9988776655",
                                              "receiptUrl": "http://localhost:8084/receipts/TXN-20241025-A1B2C3",
                                              "failureReason": null,
                                              "createdAt": "2024-10-25T14:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request — invalid card details, amount below minimum, or validation failure",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found — booking ID does not exist in the booking-service",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict — a COMPLETED payment already exists for this booking",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PostMapping("/checkout")
    public ResponseEntity<PaymentResponse> processPayment(
            @Parameter(
                    description = "Authenticated customer ID (injected by API Gateway). " +
                                  "Pass manually when testing directly — e.g. `1`.",
                    example = "1",
                    required = false
            )
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

    // ──────────────────────────────────────────────────────────────────────────
    // GET /verify/{transactionReference}
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Public transaction verification endpoint.
     *
     * @param transactionReference unique business transaction reference
     * @return 200 OK with TransactionVerifyResponse
     */
    @Operation(
            summary = "Verify a transaction by reference",
            description = "Returns verification status and summary details for the given transaction reference. " +
                          "Use the `transactionReference` value returned from the checkout response " +
                          "(e.g. `TXN-20241025-A1B2C3`)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction verification report returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransactionVerifyResponse.class),
                            examples = @ExampleObject(
                                    name = "Verified transaction",
                                    value = """
                                            {
                                              "transactionReference": "TXN-20241025-A1B2C3",
                                              "bookingId": 101,
                                              "status": "COMPLETED",
                                              "amount": 17000.00,
                                              "isVerified": true,
                                              "timestamp": "2024-10-25T14:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found — no payment record matches the provided transaction reference",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/verify/{transactionReference}")
    public ResponseEntity<TransactionVerifyResponse> verifyTransaction(
            @Parameter(
                    description = "Unique business transaction reference returned from the checkout response",
                    example = "TXN-20241025-A1B2C3",
                    required = true
            )
            @PathVariable("transactionReference") String transactionReference) {

        log.info("REST GET /api/v1/payments/verify/{}", transactionReference);
        TransactionVerifyResponse response = paymentService.verifyTransaction(transactionReference);
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /booking/{bookingId}
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves the latest payment attempt details for a given booking reservation.
     *
     * @param bookingId reservation identifier
     * @return 200 OK with PaymentResponse
     */
    @Operation(
            summary = "Get payment details by booking ID",
            description = "Retrieves the most recent payment attempt record associated with the given " +
                          "booking reservation ID. Useful for confirming payment completion before " +
                          "releasing a vehicle."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payment details retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaymentResponse.class),
                            examples = @ExampleObject(
                                    name = "Payment for booking 101",
                                    value = """
                                            {
                                              "id": 1,
                                              "transactionReference": "TXN-20241025-A1B2C3",
                                              "bookingId": 101,
                                              "customerId": 1,
                                              "amount": 17000.00,
                                              "currency": "LKR",
                                              "paymentMethod": "CREDIT_CARD",
                                              "status": "COMPLETED",
                                              "gatewayTransactionId": "GW-9988776655",
                                              "receiptUrl": "http://localhost:8084/receipts/TXN-20241025-A1B2C3",
                                              "failureReason": null,
                                              "createdAt": "2024-10-25T14:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found — no payment record found for the specified booking ID",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(
            @Parameter(
                    description = "Unique reservation / booking identifier",
                    example = "101",
                    required = true
            )
            @PathVariable("bookingId") Long bookingId) {

        log.info("REST GET /api/v1/payments/booking/{}", bookingId);
        PaymentResponse response = paymentService.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(response);
    }
}
