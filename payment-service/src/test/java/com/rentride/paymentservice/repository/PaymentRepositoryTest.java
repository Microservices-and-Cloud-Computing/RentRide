package com.rentride.paymentservice.repository;

import com.rentride.paymentservice.entity.Currency;
import com.rentride.paymentservice.entity.Payment;
import com.rentride.paymentservice.entity.PaymentMethod;
import com.rentride.paymentservice.entity.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DataJpaTest slice verifying the Payment entity mapping, Hibernate DDL schema,
 * lifecycle callbacks, unique constraints, and Spring Data query derivations.
 */
@DataJpaTest
@ActiveProfiles("test")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("testSavePayment_Success: Persists record, generates ID and auditing timestamps, and verifies default enums")
    void testSavePayment_Success() {
        Payment payment = Payment.builder()
                .transactionReference("TXN-RR-20261026-SAVE01")
                .bookingId(1L)
                .customerId(10L)
                .amount(new BigDecimal("7500.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.saveAndFlush(payment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isPositive();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getCurrency()).isEqualTo(Currency.LKR);
        assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("7500.00"));
        assertThat(saved.getTransactionReference()).isEqualTo("TXN-RR-20261026-SAVE01");
    }

    @Test
    @DisplayName("testFindByTransactionReference_Success: Query returns exact entity matching TXN-RR-TEST-001")
    void testFindByTransactionReference_Success() {
        Payment payment = Payment.builder()
                .transactionReference("TXN-RR-TEST-001")
                .bookingId(25L)
                .customerId(5L)
                .amount(new BigDecimal("12000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .status(PaymentStatus.SUCCESS)
                .gatewayTransactionId("GW-MOCK-999")
                .receiptUrl("https://rentride.com/receipts/TXN-RR-TEST-001.pdf")
                .build();

        paymentRepository.saveAndFlush(payment);

        Optional<Payment> result = paymentRepository.findByTransactionReference("TXN-RR-TEST-001");

        assertThat(result).isPresent();
        Payment found = result.get();
        assertThat(found.getTransactionReference()).isEqualTo("TXN-RR-TEST-001");
        assertThat(found.getBookingId()).isEqualTo(25L);
        assertThat(found.getCustomerId()).isEqualTo(5L);
        assertThat(found.getAmount()).isEqualByComparingTo(new BigDecimal("12000.00"));
        assertThat(found.getCurrency()).isEqualTo(Currency.LKR);
        assertThat(found.getPaymentMethod()).isEqualTo(PaymentMethod.DEBIT_CARD);
        assertThat(found.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(found.getGatewayTransactionId()).isEqualTo("GW-MOCK-999");
        assertThat(found.getReceiptUrl()).isEqualTo("https://rentride.com/receipts/TXN-RR-TEST-001.pdf");
    }

    @Test
    @DisplayName("testDuplicateTransactionReference_ThrowsException: Enforces unique constraint on transactionReference")
    void testDuplicateTransactionReference_ThrowsException() {
        Payment first = Payment.builder()
                .transactionReference("TXN-RR-DUP-001")
                .bookingId(10L)
                .customerId(2L)
                .amount(new BigDecimal("5000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.saveAndFlush(first);

        Payment duplicate = Payment.builder()
                .transactionReference("TXN-RR-DUP-001")
                .bookingId(11L)
                .customerId(3L)
                .amount(new BigDecimal("9000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.DIGITAL_WALLET)
                .status(PaymentStatus.PENDING)
                .build();

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("testFindByBookingId_ReturnsPaymentList: Returns all payment attempts for a given bookingId")
    void testFindByBookingId_ReturnsPaymentList() {
        Payment attemptFailed = Payment.builder()
                .transactionReference("TXN-RR-BK50-01")
                .bookingId(50L)
                .customerId(8L)
                .amount(new BigDecimal("10000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.FAILED)
                .failureReason("Declined by bank")
                .build();

        Payment attemptSuccess = Payment.builder()
                .transactionReference("TXN-RR-BK50-02")
                .bookingId(50L)
                .customerId(8L)
                .amount(new BigDecimal("10000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.DIGITAL_WALLET)
                .status(PaymentStatus.SUCCESS)
                .build();

        paymentRepository.saveAndFlush(attemptFailed);
        paymentRepository.saveAndFlush(attemptSuccess);

        List<Payment> bookingPayments = paymentRepository.findByBookingId(50L);

        assertThat(bookingPayments).hasSize(2);
        assertThat(bookingPayments)
                .extracting(Payment::getTransactionReference)
                .containsExactlyInAnyOrder("TXN-RR-BK50-01", "TXN-RR-BK50-02");
        assertThat(bookingPayments)
                .extracting(Payment::getStatus)
                .containsExactlyInAnyOrder(PaymentStatus.FAILED, PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("testExistsByBookingIdAndStatus_DetectsSuccessfulPayment: Confirms boolean existence query")
    void testExistsByBookingIdAndStatus_DetectsSuccessfulPayment() {
        Payment paid = Payment.builder()
                .transactionReference("TXN-RR-BK100-SUCCESS")
                .bookingId(100L)
                .customerId(15L)
                .amount(new BigDecimal("25000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.SUCCESS)
                .build();

        paymentRepository.saveAndFlush(paid);

        boolean hasSuccess = paymentRepository.existsByBookingIdAndStatus(100L, PaymentStatus.SUCCESS);
        boolean hasFailed = paymentRepository.existsByBookingIdAndStatus(100L, PaymentStatus.FAILED);
        boolean hasPending = paymentRepository.existsByBookingIdAndStatus(100L, PaymentStatus.PENDING);

        assertThat(hasSuccess).isTrue();
        assertThat(hasFailed).isFalse();
        assertThat(hasPending).isFalse();
    }

    @Test
    @DisplayName("testFindByGatewayTransactionId: Retrieves matching payment by gateway transaction ID")
    void testFindByGatewayTransactionId() {
        Payment payment = Payment.builder()
                .transactionReference("TXN-RR-GW-001")
                .bookingId(77L)
                .customerId(12L)
                .amount(new BigDecimal("4500.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.SUCCESS)
                .gatewayTransactionId("GW-TXN-EXT-777")
                .build();

        paymentRepository.saveAndFlush(payment);

        Optional<Payment> found = paymentRepository.findByGatewayTransactionId("GW-TXN-EXT-777");

        assertThat(found).isPresent();
        assertThat(found.get().getTransactionReference()).isEqualTo("TXN-RR-GW-001");
    }

    @Test
    @DisplayName("testFindByCustomerIdOrderByCreatedAtDesc: Returns all customer payments in descending timestamp order")
    void testFindByCustomerIdOrderByCreatedAtDesc() {
        Payment first = Payment.builder()
                .transactionReference("TXN-RR-CUST-01")
                .bookingId(81L)
                .customerId(99L)
                .amount(new BigDecimal("3000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.DEBIT_CARD)
                .status(PaymentStatus.SUCCESS)
                .build();

        Payment second = Payment.builder()
                .transactionReference("TXN-RR-CUST-02")
                .bookingId(82L)
                .customerId(99L)
                .amount(new BigDecimal("6000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.SUCCESS)
                .build();

        paymentRepository.saveAndFlush(first);
        paymentRepository.saveAndFlush(second);

        List<Payment> customerPayments = paymentRepository.findByCustomerIdOrderByCreatedAtDesc(99L);

        assertThat(customerPayments).hasSize(2);
        assertThat(customerPayments)
                .extracting(Payment::getTransactionReference)
                .containsExactly("TXN-RR-CUST-02", "TXN-RR-CUST-01");
    }

    @Test
    @DisplayName("testFindByStatus: Returns list of payments matching given status")
    void testFindByStatus() {
        Payment refund = Payment.builder()
                .transactionReference("TXN-RR-REFUND-01")
                .bookingId(90L)
                .customerId(30L)
                .amount(new BigDecimal("12000.00"))
                .currency(Currency.LKR)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.REFUNDED)
                .build();

        paymentRepository.saveAndFlush(refund);

        List<Payment> refundedList = paymentRepository.findByStatus(PaymentStatus.REFUNDED);

        assertThat(refundedList).hasSize(1);
        assertThat(refundedList.get(0).getTransactionReference()).isEqualTo("TXN-RR-REFUND-01");
    }
}
