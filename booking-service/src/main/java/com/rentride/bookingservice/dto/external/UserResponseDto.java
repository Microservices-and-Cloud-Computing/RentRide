package com.rentride.bookingservice.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing user profile details fetched from user-service via Feign RPC.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponseDto {

    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String role;
    private String status;
    private String businessName;
    private String driverLicenseNumber;
    private LocalDateTime createdAt;
}
