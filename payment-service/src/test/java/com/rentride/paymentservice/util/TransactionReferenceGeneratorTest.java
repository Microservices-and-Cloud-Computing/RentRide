package com.rentride.paymentservice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionReferenceGeneratorTest {

    private TransactionReferenceGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new TransactionReferenceGenerator();
    }

    @Test
    @DisplayName("generate: Should format transaction reference with TXN-RR- prefix, date segment, and 6-char entropy")
    void testGenerate_FormatMatchesExpectedPattern() {
        String reference = generator.generate();

        assertThat(reference).isNotNull();
        assertThat(reference).startsWith("TXN-RR-");
        // Format: TXN-RR-YYYYMMDD-XXXXXX (total length = 7 + 8 + 1 + 6 = 22)
        assertThat(reference).matches("^TXN-RR-\\d{8}-[A-Z0-9]{6}$");
        assertThat(reference).hasSize(22);
    }

    @Test
    @DisplayName("generate with custom date: Should encode specified date accurately in reference string")
    void testGenerate_WithCustomDate_EncodesCorrectDate() {
        LocalDate testDate = LocalDate.of(2026, 10, 18);
        String reference = generator.generate(testDate);

        assertThat(reference).startsWith("TXN-RR-20261018-");
        assertThat(reference).matches("^TXN-RR-20261018-[A-Z0-9]{6}$");
    }

    @Test
    @DisplayName("generate: Multiple invocations must produce distinct unique references")
    void testGenerate_CollisionResistance() {
        int count = 500;
        Set<String> references = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String ref = generator.generate();
            references.add(ref);
        }

        assertThat(references).hasSize(count);
    }

    @Test
    @DisplayName("generateReference: Static convenience method functions identically")
    void testStaticGenerateReference() {
        String reference = TransactionReferenceGenerator.generateReference();

        assertThat(reference).isNotNull();
        assertThat(reference).startsWith("TXN-RR-");
        assertThat(reference).matches("^TXN-RR-\\d{8}-[A-Z0-9]{6}$");
    }
}

