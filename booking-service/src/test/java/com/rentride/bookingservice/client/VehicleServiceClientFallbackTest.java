package com.rentride.bookingservice.client;

import com.rentride.bookingservice.dto.external.VehicleStatusUpdateDto;
import com.rentride.bookingservice.exception.ServiceUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VehicleServiceClientFallback}.
 */
class VehicleServiceClientFallbackTest {

    private final VehicleServiceClientFallback fallback = new VehicleServiceClientFallback();

    @Test
    @DisplayName("Should throw ServiceUnavailableException when getVehicleById fallback triggers")
    void shouldThrowWhenGetVehicleByIdFallbackTriggers() {
        assertThatThrownBy(() -> fallback.getVehicleById(55L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Vehicle service is temporarily unreachable. Unable to fetch vehicle: 55");
    }

    @Test
    @DisplayName("Should throw ServiceUnavailableException when checkAvailability fallback triggers")
    void shouldThrowWhenCheckAvailabilityFallbackTriggers() {
        assertThatThrownBy(() -> fallback.checkAvailability(77L))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Vehicle service is temporarily unreachable. Unable to check availability for vehicle: 77");
    }

    @Test
    @DisplayName("Should throw ServiceUnavailableException when updateVehicleStatus fallback triggers")
    void shouldThrowWhenUpdateVehicleStatusFallbackTriggers() {
        VehicleStatusUpdateDto dto = new VehicleStatusUpdateDto("RENTED", "Booking in progress");
        assertThatThrownBy(() -> fallback.updateVehicleStatus(88L, dto))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("Vehicle service is temporarily unreachable. Unable to update status for vehicle: 88");
    }
}

