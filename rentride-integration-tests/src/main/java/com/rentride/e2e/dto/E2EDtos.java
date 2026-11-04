package com.rentride.e2e.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class E2EDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerRegisterRequest {
        private String name;
        private String email;
        private String password;
        private String phoneNumber;
        private String licenseNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderRegisterRequest {
        private String name;
        private String email;
        private String password;
        private String phoneNumber;
        private String businessRegistrationNumber;
        private String companyName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        private String tokenType;
        private Long userId;
        private String email;
        private String role;
        private Long expiresIn;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleCreateRequest {
        private String make;
        private String model;
        private Integer manufacturingYear;
        private String licensePlateNumber;
        private Integer seatingCapacity;
        private BigDecimal dailyRentalRate;
        private String fuelType;
        private String transmission;
        private String location;
        private Double mileage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VehicleResponse {
        private Long id;
        private Long providerId;
        private String make;
        private String model;
        private Integer manufacturingYear;
        private String licensePlateNumber;
        private Integer seatingCapacity;
        private BigDecimal dailyRentalRate;
        private String fuelType;
        private String transmission;
        private String location;
        private Double mileage;
        private String status;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginatedResponse<T> {
        private List<T> content;
        private int pageNumber;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleAvailabilityResponse {
        private Long vehicleId;
        private boolean available;
        private BigDecimal dailyRate;
        private String currentStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleStatusUpdateRequest {
        private String status;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingCreateRequest {
        private Long vehicleId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String pickupLocation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BookingResponse {
        private Long id;
        private String bookingReference;
        private Long customerId;
        private Long providerId;
        private Long vehicleId;
        private String vehicleName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer totalDays;
        private BigDecimal dailyRate;
        private BigDecimal totalCost;
        private String status;
        private String pickupLocation;
        private String paymentStatus;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BookingLifecycleResponse {
        private Long id;
        private String bookingReference;
        private String bookingStatus;
        private String vehicleStatus;
        private LocalDateTime actualPickupDate;
        private LocalDateTime actualReturnDate;
        private Double endOdometer;
        private BigDecimal additionalCharges;
        private String returnNotes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingPaymentCallbackRequest {
        private String transactionReference;
        private String paymentStatus;
        private BigDecimal paidAmount;
        private LocalDateTime paymentDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehiclePickupRequest {
        private Double odometerReading;
        private Integer fuelLevelPercent;
        private String pickupNotes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleReturnRequest {
        private Double odometerReading;
        private Integer fuelLevelPercent;
        private String vehicleCondition;
        private BigDecimal additionalCharges;
        private String returnNotes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentProcessRequest {
        private Long bookingId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String cardNumber;
        private Integer expiryMonth;
        private Integer expiryYear;
        private String cvv;
        private String cardHolderName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentResponse {
        private Long id;
        private String transactionReference;
        private Long bookingId;
        private Long customerId;
        private BigDecimal amount;
        private String currency;
        private String paymentMethod;
        private String status;
        private String gatewayTransactionId;
        private String receiptUrl;
        private String failureReason;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionVerifyResponse {
        private String transactionReference;
        private Long bookingId;
        private String status;
        private BigDecimal amount;
        private boolean isVerified;
        private LocalDateTime timestamp;
    }
}

