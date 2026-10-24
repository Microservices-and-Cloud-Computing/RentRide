package com.rentride.vehicleservice.repository;

import com.rentride.vehicleservice.entity.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Data JPA Slice tests for {@link CategoryRepository}.
 *
 * <p>Verifies entity persistence, unique name constraints, and custom lookup methods.
 */
@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("testSaveCategory_Success: Persist category and assert ID and audit timestamps")
    void testSaveCategory_Success() {
        Category category = Category.builder()
                .name("HATCHBACK")
                .description("Compact 5-door hatchback suitable for urban driving")
                .baseDailyRate(new BigDecimal("35.00"))
                .build();

        Category savedCategory = categoryRepository.saveAndFlush(category);

        assertThat(savedCategory.getId()).isNotNull();
        assertThat(savedCategory.getCreatedAt()).isNotNull();
        assertThat(savedCategory.getUpdatedAt()).isNotNull();
        assertThat(savedCategory.getName()).isEqualTo("HATCHBACK");
        assertThat(savedCategory.getDescription()).isEqualTo("Compact 5-door hatchback suitable for urban driving");
        assertThat(savedCategory.getBaseDailyRate()).isEqualByComparingTo(new BigDecimal("35.00"));
    }

    @Test
    @DisplayName("testFindByName_Success: Persist category and find by unique name")
    void testFindByName_Success() {
        Category category = Category.builder()
                .name("LUXURY_SEDAN")
                .description("Premium executive sedan")
                .baseDailyRate(new BigDecimal("95.00"))
                .build();

        categoryRepository.saveAndFlush(category);

        Optional<Category> found = categoryRepository.findByName("LUXURY_SEDAN");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("LUXURY_SEDAN");
        assertThat(found.get().getBaseDailyRate()).isEqualByComparingTo(new BigDecimal("95.00"));

        Optional<Category> notFound = categoryRepository.findByName("NON_EXISTENT");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("testExistsByName: Returns true if category name exists, false otherwise")
    void testExistsByName() {
        Category category = Category.builder()
                .name("CONVERTIBLE")
                .description("Open-top sports vehicle")
                .baseDailyRate(new BigDecimal("120.00"))
                .build();

        categoryRepository.saveAndFlush(category);

        assertThat(categoryRepository.existsByName("CONVERTIBLE")).isTrue();
        assertThat(categoryRepository.existsByName("COUPE")).isFalse();
    }

    @Test
    @DisplayName("testDuplicateCategoryName_ThrowsDataIntegrityException: Unique constraint enforcement on category name")
    void testDuplicateCategoryName_ThrowsDataIntegrityException() {
        Category cat1 = Category.builder()
                .name("CROSSOVER")
                .description("Compact crossover")
                .baseDailyRate(new BigDecimal("50.00"))
                .build();
        categoryRepository.saveAndFlush(cat1);

        Category cat2 = Category.builder()
                .name("CROSSOVER")
                .description("Duplicate crossover name")
                .baseDailyRate(new BigDecimal("60.00"))
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            categoryRepository.saveAndFlush(cat2);
        });
    }
}
