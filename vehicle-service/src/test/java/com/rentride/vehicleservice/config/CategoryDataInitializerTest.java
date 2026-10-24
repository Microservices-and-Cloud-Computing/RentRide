package com.rentride.vehicleservice.config;

import com.rentride.vehicleservice.entity.Category;
import com.rentride.vehicleservice.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CategoryDataInitializer}.
 */
@ExtendWith(MockitoExtension.class)
class CategoryDataInitializerTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryDataInitializer categoryDataInitializer;

    @Test
    @DisplayName("testRun_WhenNoCategoriesExist_SeedsBaselineCategories: Table is empty, seeds SEDAN, SUV, and VAN")
    void testRun_WhenNoCategoriesExist_SeedsBaselineCategories() {
        when(categoryRepository.count()).thenReturn(0L);

        categoryDataInitializer.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Category>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(captor.capture());

        List<Category> seededCategories = captor.getValue();
        assertThat(seededCategories).hasSize(3);
        assertThat(seededCategories).extracting(Category::getName)
                .containsExactly("SEDAN", "SUV", "VAN");
    }

    @Test
    @DisplayName("testRun_WhenCategoriesAlreadyExist_SkipsSeeding: Table has existing categories, skips seeding")
    void testRun_WhenCategoriesAlreadyExist_SkipsSeeding() {
        when(categoryRepository.count()).thenReturn(3L);

        categoryDataInitializer.run();

        verify(categoryRepository, never()).saveAll(anyList());
    }
}
