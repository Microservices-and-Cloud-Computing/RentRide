package com.rentride.paymentservice.repository;

import com.rentride.paymentservice.entity.Payment;
import com.rentride.paymentservice.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for managing {@link Payment} persistence and queries.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Finds a payment record by its unique platform transaction reference.
     *
     * @param transactionReference the business transaction reference (e.g. TXN-RR-YYYYMMDD-XXXXXX)
     * @return an {@link Optional} containing the payment if found
     */
    Optional<Payment> findByTransactionReference(String transactionReference);

    /**
     * Finds a payment record by external payment gateway transaction identifier.
     *
     * @param gatewayTransactionId external provider transaction reference
     * @return an {@link Optional} containing the payment if found
     */
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);

    /**
     * Retrieves all payment attempts associated with a specific vehicle reservation.
     *
     * @param bookingId the reservation identifier
     * @return list of payment records matching the bookingId
     */
    List<Payment> findByBookingId(Long bookingId);

    /**
     * Retrieves all payments for a specific customer, ordered newest first.
     *
     * @param customerId the customer identifier
     * @return list of payments sorted by creation timestamp descending
     */
    List<Payment> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /**
     * Retrieves all payments in a specific lifecycle status.
     *
     * @param status the payment status filter
     * @return list of matching payment records
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Checks if a booking already has a payment record in a given status (e.g., SUCCESS).
     *
     * @param bookingId the reservation identifier
     * @param status    the target payment status
     * @return true if a payment exists matching both bookingId and status
     */
    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);
}

