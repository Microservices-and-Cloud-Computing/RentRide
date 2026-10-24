package com.rentride.vehicleservice.service.impl;

import com.rentride.vehicleservice.dto.VehicleCreateRequest;
import com.rentride.vehicleservice.dto.VehicleResponse;
import com.rentride.vehicleservice.dto.VehicleUpdateRequest;
import com.rentride.vehicleservice.entity.Category;
import com.rentride.vehicleservice.entity.FuelType;
import com.rentride.vehicleservice.entity.TransmissionType;
import com.rentride.vehicleservice.entity.Vehicle;
import com.rentride.vehicleservice.entity.VehicleStatus;
import com.rentride.vehicleservice.exception.DuplicateResourceException;
import com.rentride.vehicleservice.exception.ResourceNotFoundException;
import com.rentride.vehicleservice.exception.UnauthorizedAccessException;
import com.rentride.vehicleservice.repository.CategoryRepository;
import com.rentride.vehicleservice.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VehicleServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private Category testCategory;
    private Vehicle testVehicle;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("SEDAN")
                .description("Economy Sedan")
                .baseDailyRate(new BigDecimal("45.00"))
                .build();

        testVehicle = Vehicle.builder()
                .id(100L)
                .providerId(10L)
                .make("Toyota")
                .model("Aqua")
                .manufacturingYear(2021)
                .licensePlateNumber("WP-CBA-9090")
                .seatingCapacity(5)
                .fuelType(FuelType.HYBRID)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("8500.00"))
                .status(VehicleStatus.AVAILABLE)
                .location("Colombo")
                .category(testCategory)
                .build();
    }

    @Test
    @DisplayName("testCreateVehicle_Success: Valid request saves vehicle with AVAILABLE status and returns response")
    void testCreateVehicle_Success() {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .make("Toyota")
                .model("Aqua")
                .manufacturingYear(2021)
                .licensePlateNumber("wp-cba-9090 ") // tests trim & uppercase normalization
                .categoryId(1L)
                .seatingCapacity(5)
                .fuelType(FuelType.HYBRID)
                .transmission(TransmissionType.AUTOMATIC)
                .dailyRentalRate(new BigDecimal("8500.00"))
                .location("Colombo")
                .imageUrl("https://images.rentride.com/aqua.jpg")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(vehicleRepository.existsByLicensePlateNumber("WP-CBA-9090")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle v = invocation.getArgument(0);
            v.setId(100L);
            return v;
        });

        VehicleResponse response = vehicleService.createVehicle(10L, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getProviderId()).isEqualTo(10L);
        assertThat(response.getLicensePlateNumber()).isEqualTo("WP-CBA-9090");
        assertThat(response.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(response.getCategoryName()).isEqualTo("SEDAN");
        assertThat(response.getCategoryId()).isEqualTo(1L);

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        Vehicle persisted = captor.getValue();
        assertThat(persisted.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(persisted.getLicensePlateNumber()).isEqualTo("WP-CBA-9090");
    }

    @Test
    @DisplayName("testCreateVehicle_CategoryNotFound_ThrowsResourceNotFoundException")
    void testCreateVehicle_CategoryNotFound_ThrowsResourceNotFoundException() {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .categoryId(999L)
                .licensePlateNumber("WP-CBA-9090")
                .build();

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehicleService.createVehicle(10L, request));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("testCreateVehicle_DuplicatePlate_ThrowsDuplicateResourceException")
    void testCreateVehicle_DuplicatePlate_ThrowsDuplicateResourceException() {
        VehicleCreateRequest request = VehicleCreateRequest.builder()
                .make("Toyota")
                .model("Aqua")
                .categoryId(1L)
                .licensePlateNumber("WP-CBA-9090")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(vehicleRepository.existsByLicensePlateNumber("WP-CBA-9090")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> vehicleService.createVehicle(10L, request));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("testUpdateVehicle_Success: Authorized provider updates daily rate, location, and status")
    void testUpdateVehicle_Success() {
        VehicleUpdateRequest request = VehicleUpdateRequest.builder()
                .dailyRentalRate(new BigDecimal("9500.00"))
                .location("Kandy")
                .imageUrl("https://new-image.jpg")
                .seatingCapacity(5)
                .status(VehicleStatus.MAINTENANCE)
                .build();

        when(vehicleRepository.findById(100L)).thenReturn(Optional.of(testVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.updateVehicle(100L, 10L, request);

        assertThat(response.getDailyRentalRate()).isEqualByComparingTo(new BigDecimal("9500.00"));
        assertThat(response.getLocation()).isEqualTo("Kandy");
        assertThat(response.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
        assertThat(response.getImageUrl()).isEqualTo("https://new-image.jpg");
    }

    @Test
    @DisplayName("testUpdateVehicle_UnauthorizedProvider_ThrowsUnauthorizedAccessException")
    void testUpdateVehicle_UnauthorizedProvider_ThrowsUnauthorizedAccessException() {
        VehicleUpdateRequest request = VehicleUpdateRequest.builder()
                .dailyRentalRate(new BigDecimal("9500.00"))
                .location("Kandy")
                .seatingCapacity(5)
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.findById(100L)).thenReturn(Optional.of(testVehicle));

        // Provider 99 attempting to update vehicle owned by provider 10
        assertThrows(UnauthorizedAccessException.class, () -> vehicleService.updateVehicle(100L, 99L, request));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("testUpdateVehicle_NotFound_ThrowsResourceNotFoundException")
    void testUpdateVehicle_NotFound_ThrowsResourceNotFoundException() {
        VehicleUpdateRequest request = VehicleUpdateRequest.builder()
                .dailyRentalRate(new BigDecimal("9500.00"))
                .location("Kandy")
                .seatingCapacity(5)
                .status(VehicleStatus.AVAILABLE)
                .build();

        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehicleService.updateVehicle(999L, 10L, request));
    }

    @Test
    @DisplayName("testGetVehicleById_Success: Returns vehicle details")
    void testGetVehicleById_Success() {
        when(vehicleRepository.findById(100L)).thenReturn(Optional.of(testVehicle));

        VehicleResponse response = vehicleService.getVehicleById(100L);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getMake()).isEqualTo("Toyota");
        assertThat(response.getCategoryName()).isEqualTo("SEDAN");
    }

    @Test
    @DisplayName("testGetVehicleById_NotFound_ThrowsResourceNotFoundException")
    void testGetVehicleById_NotFound_ThrowsResourceNotFoundException() {
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehicleService.getVehicleById(999L));
    }

    @Test
    @DisplayName("testGetVehiclesByProvider_ReturnsList: Returns all vehicles owned by provider")
    void testGetVehiclesByProvider_ReturnsList() {
        when(vehicleRepository.findByProviderId(10L)).thenReturn(List.of(testVehicle));

        List<VehicleResponse> fleet = vehicleService.getVehiclesByProvider(10L);

        assertThat(fleet).hasSize(1);
        assertThat(fleet.get(0).getProviderId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("testDeleteVehicle_Success: Soft-deletes vehicle by updating status to DECOMMISSIONED")
    void testDeleteVehicle_Success() {
        when(vehicleRepository.findById(100L)).thenReturn(Optional.of(testVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        vehicleService.deleteVehicle(100L, 10L);

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(VehicleStatus.DECOMMISSIONED);
    }

    @Test
    @DisplayName("testDeleteVehicle_UnauthorizedProvider_ThrowsUnauthorizedAccessException")
    void testDeleteVehicle_UnauthorizedProvider_ThrowsUnauthorizedAccessException() {
        when(vehicleRepository.findById(100L)).thenReturn(Optional.of(testVehicle));

        assertThrows(UnauthorizedAccessException.class, () -> vehicleService.deleteVehicle(100L, 99L));
        verify(vehicleRepository, never()).save(any());
    }
}
