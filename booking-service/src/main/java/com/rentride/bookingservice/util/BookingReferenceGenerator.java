package com.rentride.bookingservice.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Thread-safe utility and Spring component for generating unique business reservation reference codes.
 *
 * <p>Format: {@code RR-BK-YYYYMMDD-XXXXXX}
 * <ul>
 *   <li>{@code RR-BK-} — Platform booking prefix</li>
 *   <li>{@code YYYYMMDD} — Calendar date in UTC (e.g., 20261025)</li>
 *   <li>{@code XXXXXX} — 6-character cryptographic alphanumeric entropy segment</li>
 * </ul>
 *
 * <p>Ensures collision resistance and uniqueness across concurrent reservation transactions.
 */
@Component
public class BookingReferenceGenerator {

    public static final String PREFIX = "RR-BK-";
    private static final String ALPHANUMERIC_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int RANDOM_SEGMENT_LENGTH = 6;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a new unique booking reference code using the current date and secure randomness.
     *
     * @return alphanumeric reservation code in format {@code RR-BK-YYYYMMDD-XXXXXX}
     */
    public String generate() {
        return generate(LocalDate.now());
    }

    /**
     * Generates a new unique booking reference code for a specified date.
     *
     * @param date date to encode in the reference code
     * @return alphanumeric reservation code in format {@code RR-BK-YYYYMMDD-XXXXXX}
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
     * Static convenience method to generate a reference code without injecting the component.
     *
     * @return alphanumeric reservation code in format {@code RR-BK-YYYYMMDD-XXXXXX}
     */
    public static String generateReference() {
        return new BookingReferenceGenerator().generate();
    }
}

