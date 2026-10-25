package com.rentride.vehicleservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response structure returned across all REST endpoints in the Vehicle Service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response returned when an API request fails")
public class ErrorResponse {

    @Schema(description = "Timestamp when the error occurred", example = "2026-10-25T14:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error classification or HTTP reason phrase", example = "Validation Failed")
    private String error;

    @Schema(description = "Descriptive error message", example = "Validation failed for one or more fields")
    private String message;

    @Schema(description = "Request URI path that triggered the error", example = "/api/v1/vehicles")
    private String path;

    @Schema(description = "Detailed field-specific validation failure messages")
    private Map<String, String> errors;

    @Schema(description = "Field-specific validation errors for backward compatibility")
    private Map<String, String> validationErrors;
}
