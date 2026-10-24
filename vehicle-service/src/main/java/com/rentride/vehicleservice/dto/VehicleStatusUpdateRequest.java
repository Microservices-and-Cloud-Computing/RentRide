package com.rentride.vehicleservice.dto;

import com.rentride.vehicleservice.entity.VehicleStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for requesting a vehicle operational status update.
 *
 * <p>Used primarily by internal services (such as Booking Service) to atomically
 * transition vehicle status during reservation lifecycle events (e.g. locking for rental,
 * releasing upon return, or placing into maintenance).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleStatusUpdateRequest {

    @NotNull(message = "Vehicle status is required")
    private VehicleStatus status;

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;
}

