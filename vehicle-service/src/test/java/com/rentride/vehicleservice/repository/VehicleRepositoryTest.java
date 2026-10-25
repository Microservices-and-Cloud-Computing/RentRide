package com.rentride.vehicleservice.repository;

import com.rentride.vehicleservice.entity.Category;
import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
import com.rentride.vehicleservice.entity.Vehicle;
import com.rentride.vehicleservice.entity.VehicleStatus;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Data JPA Slice tests for {@link VehicleRepository}.
 *
 * <p>Verifies entity persistence, foreign key relationships to {@link Category},
 * unique constraints on license plates, auditing timestamps, and custom repository finder methods.
 */
@DataJpaTest
@ActiveProfiles("test")
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Category sedanCategory;
    private Category suvCategory;

    @BeforeEach
    void setUp() {
        sedanCategory = categoryRepository.saveAndFlush(Category.builder()
                .name("SEDAN")
                .description("Standard sedan category")
                .baseDailyRate(new BigDecimal("45.00"))
                .build());

        suvCategory = categoryRepository.saveAndFlush(Category.builder()
                .name("SUV")
                .description("Sports utility vehicle category")
                .baseDailyRate(new BigDecimal("75.00"))
                .build());
    }

    @Test
    @DisplayName("testSaveCategoryAndVehicle_Success: Persist category and vehicle, assert auto-generated IDs, foreign key, and audit timestamps")
    void testSaveCategoryAndVehicle_Success() {
        Vehicle vehicle = Vehicle.builder()
                .providerId(100L)
                .make("Toyota")
                .model("Prius")
                .manufacturingYear(2023)
                .licensePlateNumber("WP-CAR-1001")
                .seatingCapacity(5)
                .fuelType(FuelType.HYBRID)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("55.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Colombo")
                .imageUrl("https://images.rentride.com/vehicles/prius-2023.jpg")
                .category(sedanCategory)
                .build();

        Vehicle savedVehicle = vehicleRepository.saveAndFlush(vehicle);

        assertThat(savedVehicle.getId()).isNotNull();
        assertThat(savedVehicle.getCreatedAt()).isNotNull();
        assertThat(savedVehicle.getUpdatedAt()).isNotNull();
        assertThat(savedVehicle.getCategory()).isNotNull();
        assertThat(savedVehicle.getCategory().getId()).isEqualTo(sedanCategory.getId());
        assertThat(savedVehicle.getCategory().getName()).isEqualTo("SEDAN");
        assertThat(savedVehicle.getMake()).isEqualTo("Toyota");
        assertThat(savedVehicle.getModel()).isEqualTo("Prius");
        assertThat(savedVehicle.getManufacturingYear()).isEqualTo(2023);
        assertThat(savedVehicle.getLicensePlateNumber()).isEqualTo("WP-CAR-1001");
        assertThat(savedVehicle.getSeatingCapacity()).isEqualTo(5);
        assertThat(savedVehicle.getFuelType()).isEqualTo(FuelType.HYBRID);
        assertThat(savedVehicle.getTransmission()).isEqualTo(TransmissionType.AUTOMATIC);
        assertThat(savedVehicle.getDailyRentalRate()).isEqualByComparingTo(new BigDecimal("55.00"));
        assertThat(savedVehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(savedVehicle.getLocation()).isEqualTo("Colombo");
    }

    @Test
    @DisplayName("testFindByLicensePlateNumber_Success: Save vehicle with license plate CAB-1234, query via findByLicensePlateNumber and assert fields")
    void testFindByLicensePlateNumber_Success() {
        Vehicle vehicle = Vehicle.builder()
                .providerId(101L)
                .make("Honda")
                .model("Civic")
                .manufacturingYear(2022)
                .licensePlateNumber("CAB-1234")
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("50.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Kandy")
                .category(sedanCategory)
                .build();

        vehicleRepository.saveAndFlush(vehicle);

        Optional<Vehicle> foundVehicle = vehicleRepository.findByLicensePlateNumber("CAB-1234");

        assertThat(foundVehicle).isPresent();
        Vehicle found = foundVehicle.get();
        assertThat(found.getLicensePlateNumber()).isEqualTo("CAB-1234");
        assertThat(found.getMake()).isEqualTo("Honda");
        assertThat(found.getModel()).isEqualTo("Civic");
        assertThat(found.getProviderId()).isEqualTo(101L);
        assertThat(found.getLocation()).isEqualTo("Kandy");
        assertThat(found.getCategory().getId()).isEqualTo(sedanCategory.getId());
    }

    @Test
    @DisplayName("testDuplicateLicensePlate_ThrowsDataIntegrityException: Persisting duplicate license plate throws DataIntegrityViolationException")
    void testDuplicateLicensePlate_ThrowsDataIntegrityException() {
        Vehicle vehicle1 = Vehicle.builder()
                .providerId(100L)
                .make("Nissan")
                .model("Leaf")
                .manufacturingYear(2022)
                .licensePlateNumber("DUPLICATE-PLATE")
                .seatingCapacity(5)
                .fuelType(FuelType.ELECTRIC)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("60.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Colombo")
                .category(sedanCategory)
                .build();

        vehicleRepository.saveAndFlush(vehicle1);

        Vehicle vehicle2 = Vehicle.builder()
                .providerId(200L)
                .make("Hyundai")
                .model("Ioniq")
                .manufacturingYear(2023)
                .licensePlateNumber("DUPLICATE-PLATE")
                .seatingCapacity(5)
                .fuelType(FuelType.ELECTRIC)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("65.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Galle")
                .category(sedanCategory)
                .build();

        assertThrows(DataIntegrityViolationException.class, () -> {
            vehicleRepository.saveAndFlush(vehicle2);
        });
    }

    @Test
    @DisplayName("testFindByLocationAndStatus_FiltersAccurately: Filter by location (case-insensitive) and status")
    void testFindByLocationAndStatus_FiltersAccurately() {
        Vehicle colomboAvailable = Vehicle.builder()
                .providerId(100L)
                .make("Toyota")
                .model("Corolla")
                .manufacturingYear(2021)
                .licensePlateNumber("COL-AVAIL-1")
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("48.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Colombo")
                .category(sedanCategory)
                .build();

        Vehicle colomboRented = Vehicle.builder()
                .providerId(100L)
                .make("Toyota")
                .model("Axio")
                .manufacturingYear(2020)
                .licensePlateNumber("COL-RENT-2")
                .seatingCapacity(5)
                .fuelType(FuelType.HYBRID)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("45.00"))
                .status(VehicleStatus.RENTED)
                .location("Colombo")
                .category(sedanCategory)
                .build();

        Vehicle galleAvailable = Vehicle.builder()
                .providerId(101L)
                .make("Toyota")
                .model("Raize")
                .manufacturingYear(2022)
                .licensePlateNumber("GAL-AVAIL-3")
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("65.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Galle")
                .category(suvCategory)
                .build();

        Vehicle galleRented = Vehicle.builder()
                .providerId(101L)
                .make("Kia")
                .model("Sportage")
                .manufacturingYear(2021)
                .licensePlateNumber("GAL-RENT-4")
                .seatingCapacity(5)
                .fuelType(FuelType.DIESEL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("80.00"))
                .status(VehicleStatus.RENTED)
                .location("Galle")
                .category(suvCategory)
                .build();

        vehicleRepository.saveAllAndFlush(List.of(colomboAvailable, colomboRented, galleAvailable, galleRented));

        // Exact case match
        List<Vehicle> colomboAvailableResults = vehicleRepository.findByLocationIgnoreCaseAndStatus("Colombo", VehicleStatus.AVAILABLE);
        assertThat(colomboAvailableResults).hasSize(1);
        assertThat(colomboAvailableResults.get(0).getLicensePlateNumber()).isEqualTo("COL-AVAIL-1");

        // Lowercase location match (verifies IgnoreCase behavior)
        List<Vehicle> lowercaseResults = vehicleRepository.findByLocationIgnoreCaseAndStatus("colombo", VehicleStatus.AVAILABLE);
        assertThat(lowercaseResults).hasSize(1);
        assertThat(lowercaseResults.get(0).getLicensePlateNumber()).isEqualTo("COL-AVAIL-1");

        // Galle available query
        List<Vehicle> galleAvailableResults = vehicleRepository.findByLocationIgnoreCaseAndStatus("GALLE", VehicleStatus.AVAILABLE);
        assertThat(galleAvailableResults).hasSize(1);
        assertThat(galleAvailableResults.get(0).getLicensePlateNumber()).isEqualTo("GAL-AVAIL-3");
    }

    @Test
    @DisplayName("testFindByProviderId_ReturnsProviderFleet: Persist 3 vehicles for provider 100 and 1 for provider 200, assert provider 100 has 3")
    void testFindByProviderId_ReturnsProviderFleet() {
        Vehicle v1 = Vehicle.builder()
                .providerId(100L)
                .make("Toyota")
                .model("Yaris")
                .manufacturingYear(2021)
                .licensePlateNumber("PROV-100-A")
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.MANUAL)
                .dailyRentalRate(new BigDecimal("40.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Colombo")
                .category(sedanCategory)
                .build();

        Vehicle v2 = Vehicle.builder()
                .providerId(100L)
                .make("Toyota")
                .model("Camry")
                .manufacturingYear(2022)
                .licensePlateNumber("PROV-100-B")
                .seatingCapacity(5)
                .fuelType(FuelType.HYBRID)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("70.00"))
                .status(VehicleStatus.RENTED)
                .location("Colombo")
                .category(sedanCategory)
                .build();

        Vehicle v3 = Vehicle.builder()
                .providerId(100L)
                .make("Toyota")
                .model("RAV4")
                .manufacturingYear(2023)
                .licensePlateNumber("PROV-100-C")
                .seatingCapacity(5)
                .fuelType(FuelType.HYBRID)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("85.00"))
                .status(VehicleStatus.MAINTENANCE)
                .location("Colombo")
                .category(suvCategory)
                .build();

        Vehicle v4 = Vehicle.builder()
                .providerId(200L)
                .make("Mitsubishi")
                .model("Outlander")
                .manufacturingYear(2022)
                .licensePlateNumber("PROV-200-A")
                .seatingCapacity(7)
                .fuelType(FuelType.DIESEL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("90.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Kandy")
                .category(suvCategory)
                .build();

        vehicleRepository.saveAllAndFlush(List.of(v1, v2, v3, v4));

        List<Vehicle> provider100Fleet = vehicleRepository.findByProviderId(100L);
        List<Vehicle> provider200Fleet = vehicleRepository.findByProviderId(200L);

        assertThat(provider100Fleet).hasSize(3);
        assertThat(provider100Fleet).extracting(Vehicle::getLicensePlateNumber)
                .containsExactlyInAnyOrder("PROV-100-A", "PROV-100-B", "PROV-100-C");

        assertThat(provider200Fleet).hasSize(1);
        assertThat(provider200Fleet.get(0).getLicensePlateNumber()).isEqualTo("PROV-200-A");
    }

    @Test
    @DisplayName("testFindByCategoryId_ReturnsCategoryVehicles: Query vehicles associated with category ID")
    void testFindByCategoryId_ReturnsCategoryVehicles() {
        Vehicle sedanVehicle = Vehicle.builder()
                .providerId(300L)
                .make("Mazda")
                .model("3")
                .manufacturingYear(2022)
                .licensePlateNumber("CAT-SEDAN-1")
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("45.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Colombo")
                .category(sedanCategory)
                .build();

        Vehicle suvVehicle = Vehicle.builder()
                .providerId(300L)
                .make("Mazda")
                .model("CX-5")
                .manufacturingYear(2023)
                .licensePlateNumber("CAT-SUV-1")
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("80.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Colombo")
                .category(suvCategory)
                .build();

        vehicleRepository.saveAllAndFlush(List.of(sedanVehicle, suvVehicle));

        List<Vehicle> sedans = vehicleRepository.findByCategoryId(sedanCategory.getId());
        List<Vehicle> suvs = vehicleRepository.findByCategoryId(suvCategory.getId());

        assertThat(sedans).hasSize(1);
        assertThat(sedans.get(0).getLicensePlateNumber()).isEqualTo("CAT-SEDAN-1");

        assertThat(suvs).hasSize(1);
        assertThat(suvs.get(0).getLicensePlateNumber()).isEqualTo("CAT-SUV-1");
    }

    @Test
    @DisplayName("testFindByStatus_ReturnsFilteredVehicles: Query vehicles by status")
    void testFindByStatus_ReturnsFilteredVehicles() {
        Vehicle maintenanceVehicle = Vehicle.builder()
                .providerId(400L)
                .make("Ford")
                .model("Focus")
                .manufacturingYear(2019)
                .licensePlateNumber("STATUS-MAINT-1")
                .seatingCapacity(5)
                .fuelType(FuelType.DIESEL)
                .transmission(TransmissionType.MANUAL)
                .dailyRentalRate(new BigDecimal("35.00"))
                .status(VehicleStatus.MAINTENANCE)
                .location("Jaffna")
                .category(sedanCategory)
                .build();

        vehicleRepository.saveAndFlush(maintenanceVehicle);

        List<Vehicle> maintenanceVehicles = vehicleRepository.findByStatus(VehicleStatus.MAINTENANCE);
        List<Vehicle> decommissionedVehicles = vehicleRepository.findByStatus(VehicleStatus.DECOMMISSIONED);

        assertThat(maintenanceVehicles).hasSize(1);
        assertThat(maintenanceVehicles.get(0).getLicensePlateNumber()).isEqualTo("STATUS-MAINT-1");
        assertThat(decommissionedVehicles).isEmpty();
    }

    @Test
    @DisplayName("testExistsByLicensePlateNumber: Returns true when plate exists, false otherwise")
    void testExistsByLicensePlateNumber() {
        Vehicle vehicle = Vehicle.builder()
                .providerId(500L)
                .make("Suzuki")
                .model("Swift")
                .manufacturingYear(2022)
                .licensePlateNumber("EXISTS-PLATE-1")
                .seatingCapacity(5)
                .fuelType(FuelType.PETROL)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("35.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Negombo")
                .category(sedanCategory)
                .build();

        vehicleRepository.saveAndFlush(vehicle);

        assertThat(vehicleRepository.existsByLicensePlateNumber("EXISTS-PLATE-1")).isTrue();
        assertThat(vehicleRepository.existsByLicensePlateNumber("NON-EXISTENT-PLATE")).isFalse();
    }

    @Test
    @DisplayName("testFindByIdWithPessimisticLock: Retrieves vehicle with pessimistic write lock")
    void testFindByIdWithPessimisticLock() {
        Vehicle vehicle = Vehicle.builder()
                .providerId(600L)
                .make("Toyota")
                .model("Camry")
                .manufacturingYear(2023)
                .licensePlateNumber("LOCK-PLATE-1")
                .seatingCapacity(5)
                .fuelType(FuelType.HYBRID)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("95.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Colombo")
                .category(sedanCategory)
                .build();

        Vehicle saved = vehicleRepository.saveAndFlush(vehicle);
        entityManager.clear();

        Optional<Vehicle> lockedVehicle = vehicleRepository.findByIdWithPessimisticLock(saved.getId());

        assertThat(lockedVehicle).isPresent();
        assertThat(lockedVehicle.get().getLicensePlateNumber()).isEqualTo("LOCK-PLATE-1");
        assertThat(lockedVehicle.get().getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }
}