package com.rentride.paymentservice.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Thread-safe utility and Spring component for generating unique, collision-resistant
 * financial transaction identifiers across payment operations.
 *
 * <p>Format: {@code TXN-RR-YYYYMMDD-XXXXXX}
 * <ul>
 *   <li>{@code TXN-RR-} — Platform payment transaction prefix</li>
 *   <li>{@code YYYYMMDD} — Calendar date in UTC (e.g., 20261026)</li>
 *   <li>{@code XXXXXX} — Cryptographically secure alphanumeric entropy segment</li>
 * </ul>
 *
 * <p>Guarantees idempotency and traceability across payment gateway authorization attempts.
 */
@Component
public class TransactionReferenceGenerator {

    public static final String PREFIX = "TXN-RR-";
    private static final String ALPHANUMERIC_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int RANDOM_SEGMENT_LENGTH = 6;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a new unique transaction reference code using the current date and secure randomness.
     *
     * @return alphanumeric transaction reference in format {@code TXN-RR-YYYYMMDD-XXXXXX}
     */
    public String generate() {
        return generate(LocalDate.now());
    }

    /**
     * Generates a new unique transaction reference code for a specified date.
     *
     * @param date calendar date to encode in the reference
     * @return alphanumeric transaction reference in format {@code TXN-RR-YYYYMMDD-XXXXXX}
     */
    public String generate(LocalDate date) {
        LocalDate effectiveDate = Objects.requireNonNullElseGet(date, LocalDate::now);
        String dateSegment = effectiveDate.format(DATE_FORMATTER);

        StringBuilder entropy = new StringBuilder(RANDOM_SEGMENT_LENGTH);
        for (int i = 0; i < RANDOM_SEGMENT_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(ALPHANUMERIC_CHARS.length());
            entropy.append(ALPHANUMERIC_CHARS.charAt(index));
        }

        return PREFIX + dateSegment + "-" + entropy;
    }

    /**
     * Static convenience method to generate a reference code without dependency injection.
     *
     * @return alphanumeric transaction reference in format {@code TXN-RR-YYYYMMDD-XXXXXX}
     */
    public static String generateReference() {
        return new TransactionReferenceGenerator().generate();
    }
}

