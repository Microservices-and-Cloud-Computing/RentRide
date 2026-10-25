package com.rentride.bookingservice.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit and concurrency test suite for {@link BookingReferenceGenerator}.
 */
class BookingReferenceGeneratorTest {

    private final BookingReferenceGenerator generator = new BookingReferenceGenerator();
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^RR-BK-\\d{8}-[A-Z0-9]{6}$");

    @Test
    @DisplayName("Should generate reference code matching expected regex pattern")
    void shouldMatchExpectedReferencePattern() {
        String reference = generator.generate();

        assertThat(reference)
                .isNotNull()
                .matches(REFERENCE_PATTERN);
    }

    @Test
    @DisplayName("Should encode specific date into booking reference")
    void shouldEncodeSpecificDateIntoReference() {
        LocalDate specificDate = LocalDate.of(2026, 12, 25);
        String reference = generator.generate(specificDate);

        assertThat(reference)
                .isNotNull()
                .startsWith("RR-BK-20261225-")
                .matches(REFERENCE_PATTERN);
    }

    @Test
    @DisplayName("Should generate valid reference via static utility method")
    void shouldGenerateReferenceViaStaticMethod() {
        String reference = BookingReferenceGenerator.generateReference();

        assertThat(reference)
                .isNotNull()
                .matches(REFERENCE_PATTERN);
    }

    @Test
    @DisplayName("Should ensure zero collisions during high-concurrency multi-threaded generation")
    void shouldEnsureZeroCollisionsConcurrently() throws InterruptedException {
        int threadCount = 20;
        int operationsPerThread = 250;
        int totalExpectedReferences = threadCount * operationsPerThread; // 5000 references

        Set<String> generatedReferences = Collections.newSetFromMap(new ConcurrentHashMap<>());
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String ref = generator.generate();
                        generatedReferences.add(ref);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertThat(generatedReferences).hasSize(totalExpectedReferences);
    }
}

