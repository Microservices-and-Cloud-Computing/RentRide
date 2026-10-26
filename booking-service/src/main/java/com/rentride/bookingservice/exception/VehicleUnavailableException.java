package com.rentride.bookingservice.exception;

/**
 * Thrown when an operation cannot proceed because the vehicle is not currently available
 * for rental (e.g., status is RENTED, MAINTENANCE, or UNAVAILABLE).
 */
public class VehicleUnavailableException extends RuntimeException {

    public VehicleUnavailableException(String message) {
        super(message);
    }

    public VehicleUnavailableException(Long vehicleId, String status) {
        super(String.format("Vehicle ID %d is not available for reservation (current status: %s)", vehicleId, status));
    }
}

