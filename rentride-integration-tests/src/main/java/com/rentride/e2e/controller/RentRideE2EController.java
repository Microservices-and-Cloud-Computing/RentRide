package com.rentride.e2e.controller;

import com.rentride.e2e.config.RabbitMQTestConfig;
import com.rentride.e2e.dto.E2EDtos.*;
import com.rentride.e2e.event.BookingConfirmedEvent;
import com.rentride.e2e.event.PaymentProcessedEvent;
import com.rentride.e2e.event.RentalLifecycleEvent;
import com.rentride.e2e.security.JwtTestUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RentRideE2EController {

    private final JwtTestUtils jwtTestUtils;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQTestConfig.TestEventCollector eventCollector;

    // In-memory repositories representing the 4 isolated microservice datastores
    private final Map<Long, UserRecord> userStore = new ConcurrentHashMap<>();
    private final Map<String, UserRecord> userEmailIndex = new ConcurrentHashMap<>();
    private final Map<Long, VehicleRecord> vehicleStore = new ConcurrentHashMap<>();
    private final Map<Long, BookingRecord> bookingStore = new ConcurrentHashMap<>();
    private final Map<Long, PaymentRecord> paymentStore = new ConcurrentHashMap<>();

    private final AtomicLong userIdGen = new AtomicLong(1);
    private final AtomicLong vehicleIdGen = new AtomicLong(100);
    private final AtomicLong bookingIdGen = new AtomicLong(1000);
    private final AtomicLong paymentIdGen = new AtomicLong(5000);

    // ──────────────────────────────────────────────────────────────────────────
    // 1. User & Authentication Microservice Endpoints
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/auth/register/customer")
    public ResponseEntity<Map<String, Object>> registerCustomer(@RequestBody CustomerRegisterRequest request) {
        log.info("User-Service: Registering customer {}", request.getEmail());
        long id = userIdGen.getAndIncrement();
        UserRecord user = new UserRecord(id, request.getName(), request.getEmail(), request.getPassword(), "CUSTOMER", request.getPhoneNumber());
        userStore.put(id, user);
        userEmailIndex.put(request.getEmail().toLowerCase(), user);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", id);
        resp.put("name", user.name);
        resp.put("email", user.email);
        resp.put("role", user.role);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/api/v1/auth/register/provider")
    public ResponseEntity<Map<String, Object>> registerProvider(@RequestBody ProviderRegisterRequest request) {
        log.info("User-Service: Registering provider {}", request.getEmail());
        long id = userIdGen.getAndIncrement();
        UserRecord user = new UserRecord(id, request.getName(), request.getEmail(), request.getPassword(), "PROVIDER", request.getPhoneNumber());
        userStore.put(id, user);
        userEmailIndex.put(request.getEmail().toLowerCase(), user);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", id);
        resp.put("name", user.name);
        resp.put("email", user.email);
        resp.put("role", user.role);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("User-Service: Authenticating {}", request.getEmail());
        UserRecord user = userEmailIndex.get(request.getEmail().toLowerCase());
        if (user == null || !user.password.equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtTestUtils.generateToken(user.id, user.email, user.role);
        AuthResponse response = AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.id)
                .email(user.email)
                .role(user.role)
                .expiresIn(86400000L)
                .build();
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Vehicle Microservice Endpoints
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/vehicles")
    public ResponseEntity<VehicleResponse> createVehicle(
            @RequestHeader(value = "X-User-Id", required = false) Long providerId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody VehicleCreateRequest request) {

        long id = vehicleIdGen.getAndIncrement();
        long effectiveProviderId = providerId != null ? providerId : 2L;

        VehicleRecord record = VehicleRecord.builder()
                .id(id)
                .providerId(effectiveProviderId)
                .make(request.getMake())
                .model(request.getModel())
                .manufacturingYear(request.getManufacturingYear())
                .licensePlateNumber(request.getLicensePlateNumber())
                .seatingCapacity(request.getSeatingCapacity())
                .dailyRentalRate(request.getDailyRentalRate())
                .fuelType(request.getFuelType())
                .transmission(request.getTransmission())
                .location(request.getLocation())
                .mileage(request.getMileage())
                .status("AVAILABLE")
                .createdAt(LocalDateTime.now())
                .build();

        vehicleStore.put(id, record);
        log.info("Vehicle-Service: Created vehicle {} ({}) by provider {}", id, record.licensePlateNumber, effectiveProviderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(record.toResponse());
    }

    @GetMapping("/api/v1/vehicles/provider/my-fleet")
    public ResponseEntity<List<VehicleResponse>> getMyFleet(
            @RequestHeader("X-User-Id") Long providerId) {

        List<VehicleResponse> fleet = vehicleStore.values().stream()
                .filter(v -> v.providerId.equals(providerId))
                .map(VehicleRecord::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(fleet);
    }

    @GetMapping("/api/v1/vehicles/search")
    public ResponseEntity<PaginatedResponse<VehicleResponse>> searchVehicles(
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        List<VehicleResponse> matches = vehicleStore.values().stream()
                .filter(v -> "AVAILABLE".equalsIgnoreCase(v.status))
                .filter(v -> location == null || (v.location != null && v.location.toLowerCase().contains(location.toLowerCase())))
                .filter(v -> minPrice == null || v.dailyRentalRate.compareTo(minPrice) >= 0)
                .filter(v -> maxPrice == null || v.dailyRentalRate.compareTo(maxPrice) <= 0)
                .map(VehicleRecord::toResponse)
                .collect(Collectors.toList());

        PaginatedResponse<VehicleResponse> resp = PaginatedResponse.<VehicleResponse>builder()
                .content(matches)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(matches.size())
                .totalPages(matches.isEmpty() ? 0 : 1)
                .last(true)
                .build();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/api/v1/vehicles/{id:[0-9]+}")
    public ResponseEntity<VehicleResponse> getVehicleById(@PathVariable("id") Long id) {
        VehicleRecord v = vehicleStore.get(id);
        if (v == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(v.toResponse());
    }

    @GetMapping("/api/v1/vehicles/{id:[0-9]+}/availability")
    public ResponseEntity<VehicleAvailabilityResponse> checkVehicleAvailability(@PathVariable("id") Long id) {
        VehicleRecord v = vehicleStore.get(id);
        if (v == null) {
            return ResponseEntity.notFound().build();
        }
        boolean available = "AVAILABLE".equalsIgnoreCase(v.status);
        VehicleAvailabilityResponse resp = VehicleAvailabilityResponse.builder()
                .vehicleId(id)
                .available(available)
                .dailyRate(v.dailyRentalRate)
                .currentStatus(v.status)
                .build();
        return ResponseEntity.ok(resp);
    }

    @PatchMapping("/api/v1/vehicles/{id:[0-9]+}/status")
    public ResponseEntity<VehicleResponse> updateVehicleStatus(
            @PathVariable("id") Long id,
            @RequestBody VehicleStatusUpdateRequest request) {
        VehicleRecord v = vehicleStore.get(id);
        if (v == null) {
            return ResponseEntity.notFound().build();
        }
        v.status = request.getStatus();
        log.info("Vehicle-Service: Updated vehicle {} status to {}", id, v.status);
        return ResponseEntity.ok(v.toResponse());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Booking Microservice Endpoints
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/bookings")
    public ResponseEntity<BookingResponse> createBooking(
            @RequestHeader(value = "X-User-Id", required = false) Long customerId,
            @Valid @RequestBody BookingCreateRequest request) {

        VehicleRecord vehicle = vehicleStore.get(request.getVehicleId());
        if (vehicle == null) {
            return ResponseEntity.badRequest().build();
        }

        long bookingId = bookingIdGen.getAndIncrement();
        String bookingRef = "RR-BK-" + System.currentTimeMillis();
        long effectiveCustomerId = customerId != null ? customerId : 1L;

        int totalDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        if (totalDays <= 0) totalDays = 1;
        BigDecimal totalCost = vehicle.dailyRentalRate.multiply(BigDecimal.valueOf(totalDays));

        BookingRecord booking = BookingRecord.builder()
                .id(bookingId)
                .bookingReference(bookingRef)
                .customerId(effectiveCustomerId)
                .providerId(vehicle.providerId)
                .vehicleId(vehicle.id)
                .vehicleName(vehicle.make + " " + vehicle.model)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(totalDays)
                .dailyRate(vehicle.dailyRentalRate)
                .totalCost(totalCost)
                .status("PENDING_PAYMENT")
                .pickupLocation(request.getPickupLocation() != null ? request.getPickupLocation() : vehicle.location)
                .paymentStatus("UNPAID")
                .createdAt(LocalDateTime.now())
                .build();

        bookingStore.put(bookingId, booking);
        log.info("Booking-Service: Reservation created: id={}, ref={}, status={}", bookingId, bookingRef, booking.status);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking.toResponse());
    }

    @GetMapping("/api/v1/bookings/{id:[0-9]+}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable("id") Long id) {
        BookingRecord booking = bookingStore.get(id);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(booking.toResponse());
    }

    @PatchMapping("/api/v1/bookings/{id:[0-9]+}/payment-status")
    public ResponseEntity<BookingResponse> updateBookingPaymentStatus(
            @PathVariable("id") Long id,
            @RequestBody BookingPaymentCallbackRequest request) {

        BookingRecord booking = bookingStore.get(id);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        if ("SUCCESS".equalsIgnoreCase(request.getPaymentStatus())) {
            booking.status = "CONFIRMED";
            booking.paymentStatus = "PAID";
            log.info("Booking-Service: Reservation {} confirmed via payment callback", booking.bookingReference);

            // Asynchronously publish BookingConfirmedEvent to RabbitMQ
            BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                    .bookingReference(booking.bookingReference)
                    .customerEmail(getCustomerEmail(booking.customerId))
                    .customerName("John Doe")
                    .providerEmail("provider@rentride.com")
                    .vehicleName(booking.vehicleName)
                    .startDate(booking.startDate)
                    .endDate(booking.endDate)
                    .totalAmount(booking.totalCost)
                    .pickupLocation(booking.pickupLocation)
                    .occurredOn(LocalDateTime.now())
                    .build();

            publishEvent(RabbitMQTestConfig.ROUTING_KEY_BOOKING_CONFIRMED, event);
            eventCollector.recordBookingConfirmed(event);
        } else {
            log.warn("Booking-Service: Payment callback indicates payment failure for booking {}", booking.bookingReference);
        }

        return ResponseEntity.ok(booking.toResponse());
    }

    @PostMapping("/api/v1/bookings/{id:[0-9]+}/pickup")
    public ResponseEntity<BookingLifecycleResponse> recordPickup(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long providerId,
            @RequestBody VehiclePickupRequest request) {

        BookingRecord booking = bookingStore.get(id);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        booking.status = "ACTIVE_RENTAL";
        booking.actualPickupDate = LocalDateTime.now();

        // Downstream vehicle status sync
        VehicleRecord v = vehicleStore.get(booking.vehicleId);
        if (v != null) {
            v.status = "RENTED";
        }

        RentalLifecycleEvent event = RentalLifecycleEvent.builder()
                .bookingReference(booking.bookingReference)
                .customerEmail(getCustomerEmail(booking.customerId))
                .customerName("John Doe")
                .vehicleName(booking.vehicleName)
                .eventType("PICKUP_COMPLETED")
                .odometerReading(request.getOdometerReading())
                .fuelLevelPercent(request.getFuelLevelPercent())
                .vehicleCondition("EXCELLENT")
                .additionalCharges(BigDecimal.ZERO)
                .occurredOn(LocalDateTime.now())
                .build();

        publishEvent("rental.event.pickup", event);
        eventCollector.recordRentalLifecycle(event);

        BookingLifecycleResponse resp = BookingLifecycleResponse.builder()
                .id(booking.id)
                .bookingReference(booking.bookingReference)
                .bookingStatus("ACTIVE_RENTAL")
                .vehicleStatus("RENTED")
                .actualPickupDate(booking.actualPickupDate)
                .build();

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/api/v1/bookings/{id:[0-9]+}/return")
    public ResponseEntity<BookingLifecycleResponse> recordReturn(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long providerId,
            @RequestBody VehicleReturnRequest request) {

        BookingRecord booking = bookingStore.get(id);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        booking.status = "COMPLETED";
        booking.actualReturnDate = LocalDateTime.now();
        booking.endOdometer = request.getOdometerReading();
        booking.additionalCharges = request.getAdditionalCharges() != null ? request.getAdditionalCharges() : BigDecimal.ZERO;
        booking.returnNotes = request.getReturnNotes();

        // Downstream vehicle status sync
        String downstreamStatus = "GOOD".equalsIgnoreCase(request.getVehicleCondition()) ? "AVAILABLE" : "MAINTENANCE";
        VehicleRecord v = vehicleStore.get(booking.vehicleId);
        if (v != null) {
            v.status = downstreamStatus;
        }

        RentalLifecycleEvent event = RentalLifecycleEvent.builder()
                .bookingReference(booking.bookingReference)
                .customerEmail(getCustomerEmail(booking.customerId))
                .customerName("John Doe")
                .vehicleName(booking.vehicleName)
                .eventType("RETURN_COMPLETED")
                .odometerReading(request.getOdometerReading())
                .fuelLevelPercent(request.getFuelLevelPercent())
                .vehicleCondition(request.getVehicleCondition())
                .additionalCharges(booking.additionalCharges)
                .occurredOn(LocalDateTime.now())
                .build();

        publishEvent("rental.event.return", event);
        eventCollector.recordRentalLifecycle(event);

        BookingLifecycleResponse resp = BookingLifecycleResponse.builder()
                .id(booking.id)
                .bookingReference(booking.bookingReference)
                .bookingStatus("COMPLETED")
                .vehicleStatus(downstreamStatus)
                .actualReturnDate(booking.actualReturnDate)
                .endOdometer(booking.endOdometer)
                .additionalCharges(booking.additionalCharges)
                .returnNotes(booking.returnNotes)
                .build();

        return ResponseEntity.ok(resp);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Payment Microservice Endpoints
    // ──────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/payments/checkout")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader(value = "X-User-Id", required = false) Long customerId,
            @Valid @RequestBody PaymentProcessRequest request) {

        BookingRecord booking = bookingStore.get(request.getBookingId());
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        long paymentId = paymentIdGen.getAndIncrement();
        String txnRef = "TXN-" + System.currentTimeMillis() + "-E2E";
        long effectiveCustomerId = customerId != null ? customerId : booking.customerId;

        boolean isDeclined = request.getCardNumber() != null && request.getCardNumber().endsWith("0000");

        PaymentRecord payment = PaymentRecord.builder()
                .id(paymentId)
                .transactionReference(txnRef)
                .bookingId(request.getBookingId())
                .customerId(effectiveCustomerId)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "LKR")
                .paymentMethod(request.getPaymentMethod())
                .status(isDeclined ? "FAILED" : "COMPLETED")
                .gatewayTransactionId(isDeclined ? null : "GW-" + UUID.randomUUID().toString().substring(0, 8))
                .receiptUrl(isDeclined ? null : "http://localhost:8084/receipts/" + txnRef)
                .failureReason(isDeclined ? "Declined: Insufficient funds" : null)
                .createdAt(LocalDateTime.now())
                .build();

        paymentStore.put(paymentId, payment);

        // OpenFeign callback to Booking-Service
        BookingPaymentCallbackRequest callback = BookingPaymentCallbackRequest.builder()
                .transactionReference(txnRef)
                .paymentStatus(isDeclined ? "FAILED" : "SUCCESS")
                .paidAmount(request.getAmount())
                .paymentDate(LocalDateTime.now())
                .build();
        updateBookingPaymentStatus(request.getBookingId(), callback);

        // Publish PaymentProcessedEvent to RabbitMQ
        PaymentProcessedEvent event = PaymentProcessedEvent.builder()
                .transactionReference(txnRef)
                .bookingReference(booking.bookingReference)
                .customerEmail(getCustomerEmail(effectiveCustomerId))
                .customerName("John Doe")
                .amount(request.getAmount())
                .currency(payment.currency)
                .paymentMethod(payment.paymentMethod)
                .status(isDeclined ? "FAILED" : "SUCCESS")
                .failureReason(payment.failureReason)
                .receiptUrl(payment.receiptUrl)
                .occurredOn(LocalDateTime.now())
                .build();

        publishEvent(RabbitMQTestConfig.ROUTING_KEY_PAYMENT_PROCESSED, event);
        eventCollector.recordPaymentProcessed(event);

        return ResponseEntity.ok(payment.toResponse());
    }

    private void publishEvent(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQTestConfig.RENTRIDE_EXCHANGE, routingKey, event);
            log.info("Event successfully sent to RabbitMQ: routingKey={}, event={}", routingKey, event.getClass().getSimpleName());
        } catch (Exception ex) {
            log.debug("RabbitMQ broker not reachable; captured in memory for test assertions: {}", ex.getMessage());
        }
    }

    private String getCustomerEmail(Long customerId) {
        UserRecord u = userStore.get(customerId);
        return u != null ? u.email : "customer@rentride.com";
    }

    public void clearData() {
        userStore.clear();
        userEmailIndex.clear();
        vehicleStore.clear();
        bookingStore.clear();
        paymentStore.clear();
        eventCollector.clear();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal Data Classes
    // ──────────────────────────────────────────────────────────────────────────

    static class UserRecord {
        Long id;
        String name;
        String email;
        String password;
        String role;
        String phone;

        UserRecord(Long id, String name, String email, String password, String role, String phone) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.password = password;
            this.role = role;
            this.phone = phone;
        }
    }

    @lombok.Builder
    static class VehicleRecord {
        Long id;
        Long providerId;
        String make;
        String model;
        Integer manufacturingYear;
        String licensePlateNumber;
        Integer seatingCapacity;
        BigDecimal dailyRentalRate;
        String fuelType;
        String transmission;
        String location;
        Double mileage;
        String status;
        LocalDateTime createdAt;

        VehicleResponse toResponse() {
            return VehicleResponse.builder()
                    .id(id)
                    .providerId(providerId)
                    .make(make)
                    .model(model)
                    .manufacturingYear(manufacturingYear)
                    .licensePlateNumber(licensePlateNumber)
                    .seatingCapacity(seatingCapacity)
                    .dailyRentalRate(dailyRentalRate)
                    .fuelType(fuelType)
                    .transmission(transmission)
                    .location(location)
                    .mileage(mileage)
                    .status(status)
                    .createdAt(createdAt)
                    .build();
        }
    }

    @lombok.Builder
    static class BookingRecord {
        Long id;
        String bookingReference;
        Long customerId;
        Long providerId;
        Long vehicleId;
        String vehicleName;
        LocalDate startDate;
        LocalDate endDate;
        Integer totalDays;
        BigDecimal dailyRate;
        BigDecimal totalCost;
        String status;
        String pickupLocation;
        String paymentStatus;
        LocalDateTime createdAt;
        LocalDateTime actualPickupDate;
        LocalDateTime actualReturnDate;
        Double endOdometer;
        BigDecimal additionalCharges;
        String returnNotes;

        BookingResponse toResponse() {
            return BookingResponse.builder()
                    .id(id)
                    .bookingReference(bookingReference)
                    .customerId(customerId)
                    .providerId(providerId)
                    .vehicleId(vehicleId)
                    .vehicleName(vehicleName)
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalDays(totalDays)
                    .dailyRate(dailyRate)
                    .totalCost(totalCost)
                    .status(status)
                    .pickupLocation(pickupLocation)
                    .paymentStatus(paymentStatus)
                    .createdAt(createdAt)
                    .build();
        }
    }

    @lombok.Builder
    static class PaymentRecord {
        Long id;
        String transactionReference;
        Long bookingId;
        Long customerId;
        BigDecimal amount;
        String currency;
        String paymentMethod;
        String status;
        String gatewayTransactionId;
        String receiptUrl;
        String failureReason;
        LocalDateTime createdAt;

        PaymentResponse toResponse() {
            return PaymentResponse.builder()
                    .id(id)
                    .transactionReference(transactionReference)
                    .bookingId(bookingId)
                    .customerId(customerId)
                    .amount(amount)
                    .currency(currency)
                    .paymentMethod(paymentMethod)
                    .status(status)
                    .gatewayTransactionId(gatewayTransactionId)
                    .receiptUrl(receiptUrl)
                    .failureReason(failureReason)
                    .createdAt(createdAt)
                    .build();
        }
    }
}

