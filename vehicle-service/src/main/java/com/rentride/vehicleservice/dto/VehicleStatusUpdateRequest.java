package com.rentride.vehicleservice.dto;

import com.rentride.vehicleservice.entity.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request payload for updating a vehicle's operational status")
public class VehicleStatusUpdateRequest {

    @NotNull(message = "Vehicle status is required")
    @Schema(description = "Target operational status of the vehicle", example = "RENTED", requiredMode = Schema.RequiredMode.REQUIRED)
    private VehicleStatus status;

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    @Schema(description = "Operational reason or booking reference triggering status transition", example = "Booking #501 active")
    private String reason;
}
