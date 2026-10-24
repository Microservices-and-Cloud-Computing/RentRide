package com.rentride.vehicleservice.config;

import com.rentride.vehicleservice.entity.Category;
import com.rentride.vehicleservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Baseline data seeder for development and local runtime environments.
 * Seeds standard vehicle categories (Economy Sedan, Compact SUV, Luxury Van)
 * if the {@code categories} table is empty on application startup.
 * Disabled under the 'test' profile to preserve test isolation.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class CategoryDataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        log.info("Checking baseline category data initialization for RentRide vehicle-service...");
        seedBaselineCategories();
    }

    private void seedBaselineCategories() {
        if (categoryRepository.count() > 0) {
            log.info("Vehicle categories already present in database (count={}). Skipping seed.", categoryRepository.count());
            return;
        }

        log.info("No vehicle categories found. Seeding baseline categories: SEDAN, SUV, VAN...");

        List<Category> baselineCategories = List.of(
                Category.builder()
                        .name("SEDAN")
                        .description("Economy Sedan — Fuel-efficient 4-door vehicles ideal for city transit and everyday business commuting")
                        .baseDailyRate(new BigDecimal("45.00"))
                        .build(),
                Category.builder()
                        .name("SUV")
                        .description("Compact SUV — Spacious multi-terrain sports utility vehicles suitable for family trips and weekend getaways")
                        .baseDailyRate(new BigDecimal("75.00"))
                        .build(),
                Category.builder()
                        .name("VAN")
                        .description("Luxury Van — High passenger and cargo capacity vans designed for group travel and touring comfort")
                        .baseDailyRate(new BigDecimal("110.00"))
                        .build()
        );

        categoryRepository.saveAll(baselineCategories);
        log.info("Successfully seeded {} baseline vehicle categories into the database.", baselineCategories.size());
    }
}
