package com.rentride.bookingservice.service;

import com.rentride.bookingservice.client.VehicleServiceClient;
import com.rentride.bookingservice.dto.external.VehicleAvailabilityDto;
import com.rentride.bookingservice.dto.external.VehicleResponseDto;
import com.rentride.bookingservice.dto.external.VehicleStatusUpdateDto;
import com.rentride.bookingservice.exception.VehicleNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VehicleIntegrationService}.
 */
@ExtendWith(MockitoExtension.class)
class VehicleIntegrationServiceTest {

    @Mock
    private VehicleServiceClient vehicleServiceClient;

    @InjectMocks
    private VehicleIntegrationService vehicleIntegrationService;

    @Test
    @DisplayName("Should retrieve vehicle details via Feign client")
    void shouldRetrieveVehicleById() {
        VehicleResponseDto mockResponse = VehicleResponseDto.builder()
                .id(1L)
                .providerId(10L)
                .make("Toyota")
                .model("Aqua")
                .dailyRentalRate(new BigDecimal("7500.00"))
                .status("AVAILABLE")
                .location("Colombo")
                .build();

        when(vehicleServiceClient.getVehicleById(1L)).thenReturn(mockResponse);

        VehicleResponseDto result = vehicleIntegrationService.getVehicleById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMake()).isEqualTo("Toyota");
        assertThat(result.getModel()).isEqualTo("Aqua");
        assertThat(result.getDailyRentalRate()).isEqualByComparingTo("7500.00");
        verify(vehicleServiceClient).getVehicleById(1L);
    }

    @Test
    @DisplayName("Should verify vehicle availability and return availability DTO")
    void shouldCheckAvailability() {
        VehicleAvailabilityDto mockAvailability = VehicleAvailabilityDto.builder()
                .vehicleId(2L)
                .status("AVAILABLE")
                .isAvailable(true)
                .dailyRentalRate(new BigDecimal("8000.00"))
                .build();

        when(vehicleServiceClient.checkAvailability(2L)).thenReturn(mockAvailability);

        VehicleAvailabilityDto result = vehicleIntegrationService.checkAvailability(2L);

        assertThat(result).isNotNull();
        assertThat(result.isAvailable()).isTrue();
        assertThat(result.getStatus()).isEqualTo("AVAILABLE");
        assertThat(result.getDailyRentalRate()).isEqualByComparingTo("8000.00");
        verify(vehicleServiceClient).checkAvailability(2L);
    }

    @Test
    @DisplayName("Should return boolean true when vehicle is available")
    void shouldReturnTrueWhenVehicleIsAvailable() {
        VehicleAvailabilityDto mockAvailability = VehicleAvailabilityDto.builder()
                .vehicleId(3L)
                .isAvailable(true)
                .build();

        when(vehicleServiceClient.checkAvailability(3L)).thenReturn(mockAvailability);

        boolean available = vehicleIntegrationService.isVehicleAvailable(3L);

        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Should return boolean false when vehicle is not available")
    void shouldReturnFalseWhenVehicleIsNotAvailable() {
        VehicleAvailabilityDto mockAvailability = VehicleAvailabilityDto.builder()
                .vehicleId(4L)
                .isAvailable(false)
                .build();

        when(vehicleServiceClient.checkAvailability(4L)).thenReturn(mockAvailability);

        boolean available = vehicleIntegrationService.isVehicleAvailable(4L);

        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("Should retrieve daily rental rate for vehicle")
    void shouldGetDailyRentalRate() {
        VehicleAvailabilityDto mockAvailability = VehicleAvailabilityDto.builder()
                .vehicleId(5L)
                .dailyRentalRate(new BigDecimal("9500.00"))
                .build();

        when(vehicleServiceClient.checkAvailability(5L)).thenReturn(mockAvailability);

        BigDecimal rate = vehicleIntegrationService.getDailyRentalRate(5L);

        assertThat(rate).isEqualByComparingTo("9500.00");
    }

    @Test
    @DisplayName("Should request vehicle status transition and return updated vehicle DTO")
    void shouldUpdateVehicleStatus() {
        VehicleResponseDto updatedVehicle = VehicleResponseDto.builder()
                .id(6L)
                .status("RENTED")
                .build();

        when(vehicleServiceClient.updateVehicleStatus(eq(6L), any(VehicleStatusUpdateDto.class)))
                .thenReturn(updatedVehicle);

        VehicleResponseDto result = vehicleIntegrationService.updateVehicleStatus(6L, "RENTED", "Reservation confirmed");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("RENTED");
        verify(vehicleServiceClient).updateVehicleStatus(eq(6L), any(VehicleStatusUpdateDto.class));
    }

    @Test
    @DisplayName("Should propagate VehicleNotFoundException when vehicle does not exist")
    void shouldPropagateVehicleNotFoundException() {
        when(vehicleServiceClient.getVehicleById(999L))
                .thenThrow(new VehicleNotFoundException("Vehicle not found with ID: 999"));

        assertThatThrownBy(() -> vehicleIntegrationService.getVehicleById(999L))
                .isInstanceOf(VehicleNotFoundException.class)
                .hasMessage("Vehicle not found with ID: 999");
    }
}

