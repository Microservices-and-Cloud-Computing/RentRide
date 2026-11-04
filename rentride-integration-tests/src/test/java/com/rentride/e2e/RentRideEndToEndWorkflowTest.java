package com.rentride.e2e;

import com.rentride.e2e.dto.E2EDtos.*;
import com.rentride.e2e.event.BookingConfirmedEvent;
import com.rentride.e2e.event.PaymentProcessedEvent;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;

/**
 * Multi-service End-to-End Integration Test Suite for RentRide Platform (Commit 34).
 *
 * <p>Validates all 5 core distributed business scenarios:
 * <ol>
 *   <li>Scenario 01: Customer registration, catalog search with dynamic filters, reservation creation</li>
 *   <li>Scenario 02: Online payment execution, inter-service OpenFeign callback to confirm reservation</li>
 *   <li>Scenario 03: Provider fleet onboarding with role-based checks</li>
 *   <li>Scenario 04: Provider handover (pickup) and return inspection (AVAILABLE vs MAINTENANCE state)</li>
 *   <li>Scenario 05: Asynchronous event propagation to RabbitMQ queues verified via Awaitility</li>
 * </ol>
 */
@Slf4j
class RentRideEndToEndWorkflowTest extends BaseE2EIntegrationTest {

    @Test
    @DisplayName("Scenario 01-05: Complete Rental Lifecycle from Registration to Return")
    void testCompleteRentalLifecycle_FromRegistrationToReturn() {
        log.info("═══ START: testCompleteRentalLifecycle_FromRegistrationToReturn ═══");

        // ──────────────────────────────────────────────────────────────────────
        // Step A (User Service): Register customer and provider users; log in
        // ──────────────────────────────────────────────────────────────────────
        log.info("Step A: Registering customer and provider users...");

        CustomerRegisterRequest customerReq = CustomerRegisterRequest.builder()
                .name("John Doe")
                .email("john.doe@rentride.com")
                .password("SecurePass123!")
                .phoneNumber("+94771234567")
                .licenseNumber("B9876543")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(customerReq)
        .when()
                .post("/api/v1/auth/register/customer")
        .then()
                .statusCode(201)
                .body("email", equalTo("john.doe@rentride.com"))
                .body("role", equalTo("CUSTOMER"));

        ProviderRegisterRequest providerReq = ProviderRegisterRequest.builder()
                .name("RentRide Fleet Corp")
                .email("fleet@rentride.com")
                .password("SecurePass123!")
                .phoneNumber("+94777654321")
                .businessRegistrationNumber("PV-998877")
                .companyName("RentRide Fleet Services")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(providerReq)
        .when()
                .post("/api/v1/auth/register/provider")
        .then()
                .statusCode(201)
                .body("email", equalTo("fleet@rentride.com"))
                .body("role", equalTo("PROVIDER"));

        // Login Customer
        LoginRequest customerLogin = LoginRequest.builder()
                .email("john.doe@rentride.com")
                .password("SecurePass123!")
                .build();

        String customerToken = given()
                .contentType(ContentType.JSON)
                .body(customerLogin)
        .when()
                .post("/api/v1/auth/login")
        .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("role", equalTo("CUSTOMER"))
                .extract().path("accessToken");

        // Login Provider
        LoginRequest providerLogin = LoginRequest.builder()
                .email("fleet@rentride.com")
                .password("SecurePass123!")
                .build();

        String providerToken = given()
                .contentType(ContentType.JSON)
                .body(providerLogin)
        .when()
                .post("/api/v1/auth/login")
        .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("role", equalTo("PROVIDER"))
                .extract().path("accessToken");

        assertThat(customerToken).isNotBlank();
        assertThat(providerToken).isNotBlank();
        log.info("Step A Completed: Customer and Provider authenticated with valid JWTs");

        // ──────────────────────────────────────────────────────────────────────
        // Step B (Vehicle Service): Provider adds a vehicle (WP-CBA-2026, 10,000 LKR)
        // ──────────────────────────────────────────────────────────────────────
        log.info("Step B: Provider registering vehicle WP-CBA-2026...");

        VehicleCreateRequest vehicleReq = VehicleCreateRequest.builder()
                .make("Toyota")
                .model("Premio")
                .manufacturingYear(2022)
                .licensePlateNumber("WP-CBA-2026")
                .seatingCapacity(5)
                .dailyRentalRate(new BigDecimal("10000.00"))
                .fuelType("PETROL")
                .transmission("AUTOMATIC")
                .location("Colombo")
                .mileage(25000.0)
                .build();

        Number vehicleIdNum = given()
                .header("Authorization", "Bearer " + providerToken)
                .contentType(ContentType.JSON)
                .body(vehicleReq)
        .when()
                .post("/api/v1/vehicles")
        .then()
                .statusCode(201)
                .body("licensePlateNumber", equalTo("WP-CBA-2026"))
                .body("status", equalTo("AVAILABLE"))
                .body("dailyRentalRate", equalTo(10000.0f))
                .extract().path("id");

        Long vehicleId = vehicleIdNum.longValue();
        log.info("Step B: Vehicle registered with id={}", vehicleId);

        // Verify listing appears in provider fleet
        given()
                .header("Authorization", "Bearer " + providerToken)
        .when()
                .get("/api/v1/vehicles/provider/my-fleet")
        .then()
                .statusCode(200)
                .body("licensePlateNumber", hasItem("WP-CBA-2026"));

        // Verify listing appears in public search
        given()
        .when()
                .get("/api/v1/vehicles/search")
        .then()
                .statusCode(200)
                .body("content.licensePlateNumber", hasItem("WP-CBA-2026"));

        log.info("Step B Completed: Vehicle active in provider fleet and public catalog");

        // ──────────────────────────────────────────────────────────────────────
        // Step C (Vehicle Search): Customer searches by location and max price
        // ──────────────────────────────────────────────────────────────────────
        log.info("Step C: Customer searching catalog by location and max price...");

        given()
                .queryParam("location", "Colombo")
                .queryParam("maxPrice", 15000)
        .when()
                .get("/api/v1/vehicles/search")
        .then()
                .statusCode(200)
                .body("content.licensePlateNumber", hasItem("WP-CBA-2026"));

        // Verify vehicle availability
        given()
        .when()
                .get("/api/v1/vehicles/" + vehicleId + "/availability")
        .then()
                .statusCode(200)
                .body("available", equalTo(true))
                .body("dailyRate", equalTo(10000.0f));

        log.info("Step C Completed: Customer located available vehicle");

        // ──────────────────────────────────────────────────────────────────────
        // Step D (Booking Service): Customer creates a reservation for 3 days
        // ──────────────────────────────────────────────────────────────────────
        log.info("Step D: Customer booking vehicle for 3 days...");

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(4);

        BookingCreateRequest bookingReq = BookingCreateRequest.builder()
                .vehicleId(vehicleId)
                .startDate(start)
                .endDate(end)
                .pickupLocation("Colombo Central")
                .build();

        Number bookingIdNum = given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType(ContentType.JSON)
                .body(bookingReq)
        .when()
                .post("/api/v1/bookings")
        .then()
                .statusCode(201)
                .body("status", equalTo("PENDING_PAYMENT"))
                .body("totalDays", equalTo(3))
                .body("totalCost", equalTo(30000.0f))
                .body("bookingReference", notNullValue())
                .extract().path("id");

        Long bookingId = bookingIdNum.longValue();

        String bookingReference = given()
                .header("Authorization", "Bearer " + customerToken)
        .when()
                .get("/api/v1/bookings/" + bookingId)
        .then()
                .statusCode(200)
                .extract().path("bookingReference");

        log.info("Step D Completed: Booking reservation created id={}, ref={}", bookingId, bookingReference);

        // ──────────────────────────────────────────────────────────────────────
        // Step E (Payment Service): Customer pays with valid card (ends with 4242)
        // ──────────────────────────────────────────────────────────────────────
        log.info("Step E: Customer executing payment checkout with valid card...");

        PaymentProcessRequest paymentReq = PaymentProcessRequest.builder()
                .bookingId(bookingId)
                .amount(new BigDecimal("30000.00"))
                .currency("LKR")
                .paymentMethod("CREDIT_CARD")
                .cardNumber("4111111111114242")
                .expiryMonth(12)
                .expiryYear(2028)
                .cvv("123")
                .cardHolderName("John Doe")
                .build();

        given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType(ContentType.JSON)
                .body(paymentReq)
        .when()
                .post("/api/v1/payments/checkout")
        .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"))
                .body("transactionReference", notNullValue());

        // Verify OpenFeign callback automatically updated booking status to CONFIRMED
        given()
                .header("Authorization", "Bearer " + customerToken)
        .when()
                .get("/api/v1/bookings/" + bookingId)
        .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .body("paymentStatus", equalTo("PAID"));

        log.info("Step E Completed: Payment succeeded and Booking confirmed via callback");

        // ──────────────────────────────────────────────────────────────────────
        // Step F (Event Notification): Assert events arrive in RabbitMQ queues
        // ──────────────────────────────────────────────────────────────────────
        log.info("Step F: Asserting async domain event publication via Awaitility...");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(eventCollector.getBookingConfirmedEvents())
                    .isNotEmpty()
                    .anyMatch(e -> bookingReference.equals(e.getBookingReference()) &&
                                   e.getCustomerEmail().contains("rentride.com") &&
                                   e.getTotalAmount().compareTo(new BigDecimal("30000.00")) == 0);

            assertThat(eventCollector.getPaymentProcessedEvents())
                    .isNotEmpty()
                    .anyMatch(e -> bookingReference.equals(e.getBookingReference()) &&
                                   "SUCCESS".equalsIgnoreCase(e.getStatus()) &&
                                   e.getAmount().compareTo(new BigDecimal("30000.00")) == 0);
        });

        log.info("Step F Completed: BookingConfirmedEvent & PaymentProcessedEvent successfully delivered");

        // ──────────────────────────────────────────────────────────────────────
        // Step G (Handover / Pickup): Provider records vehicle pickup
        // ──────────────────────────────────────────────────────────────────────
        log.info("Step G: Provider completing vehicle pickup handover...");

        VehiclePickupRequest pickupReq = VehiclePickupRequest.builder()
                .odometerReading(25000.0)
                .fuelLevelPercent(100)
                .pickupNotes("Vehicle handed over in clean condition")
                .build();

        given()
                .header("Authorization", "Bearer " + providerToken)
                .contentType(ContentType.JSON)
                .body(pickupReq)
        .when()
                .post("/api/v1/bookings/" + bookingId + "/pickup")
        .then()
                .statusCode(200)
                .body("bookingStatus", equalTo("ACTIVE_RENTAL"))
                .body("vehicleStatus", equalTo("RENTED"));

        // Verify booking status is ACTIVE_RENTAL
        given()
                .header("Authorization", "Bearer " + customerToken)
        .when()
                .get("/api/v1/bookings/" + bookingId)
        .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE_RENTAL"));

        log.info("Step G Completed: Handover recorded, status is ACTIVE_RENTAL");

        // ──────────────────────────────────────────────────────────────────────
        // Step H (Vehicle Return): Provider completes return with condition GOOD
        // ──────────────────────────────────────────────────────────────────────
        log.info("Step H: Provider processing return in GOOD condition...");

        VehicleReturnRequest returnReq = VehicleReturnRequest.builder()
                .odometerReading(25450.0)
                .fuelLevelPercent(95)
                .vehicleCondition("GOOD")
                .additionalCharges(BigDecimal.ZERO)
                .returnNotes("Clean return with zero damages")
                .build();

        given()
                .header("Authorization", "Bearer " + providerToken)
                .contentType(ContentType.JSON)
                .body(returnReq)
        .when()
                .post("/api/v1/bookings/" + bookingId + "/return")
        .then()
                .statusCode(200)
                .body("bookingStatus", equalTo("COMPLETED"))
                .body("vehicleStatus", equalTo("AVAILABLE"));

        // Verify booking status is COMPLETED
        given()
                .header("Authorization", "Bearer " + customerToken)
        .when()
                .get("/api/v1/bookings/" + bookingId)
        .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));

        // Verify vehicle status is reset to AVAILABLE
        given()
        .when()
                .get("/api/v1/vehicles/" + vehicleId)
        .then()
                .statusCode(200)
                .body("status", equalTo("AVAILABLE"));

        log.info("═══ SUCCESS: testCompleteRentalLifecycle_FromRegistrationToReturn passed ═══");
    }

    @Test
    @DisplayName("Scenario 02: Payment failure with declined card does not confirm booking")
    void testPaymentFailureAndDeclinedCard_DoesNotConfirmBooking() {
        log.info("═══ START: testPaymentFailureAndDeclinedCard_DoesNotConfirmBooking ═══");

        // 1. Setup Customer, Provider, Vehicle, and Booking
        String customerToken = registerAndLogin("alice@rentride.com", "CUSTOMER");
        String providerToken = registerAndLogin("fleet-b@rentride.com", "PROVIDER");

        Long vehicleId = createVehicle(providerToken, "WP-CAD-5544", new BigDecimal("8000.00"));
        Long bookingId = createBooking(customerToken, vehicleId, 2);

        // 2. Customer checks out with declined card (ending in 0000)
        PaymentProcessRequest paymentReq = PaymentProcessRequest.builder()
                .bookingId(bookingId)
                .amount(new BigDecimal("16000.00"))
                .currency("LKR")
                .paymentMethod("CREDIT_CARD")
                .cardNumber("4111111111110000")
                .expiryMonth(10)
                .expiryYear(2027)
                .cvv("456")
                .cardHolderName("Alice Smith")
                .build();

        given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType(ContentType.JSON)
                .body(paymentReq)
        .when()
                .post("/api/v1/payments/checkout")
        .then()
                .statusCode(200)
                .body("status", equalTo("FAILED"))
                .body("failureReason", containsString("Declined"));

        // 3. Verify booking remains in PENDING_PAYMENT
        given()
                .header("Authorization", "Bearer " + customerToken)
        .when()
                .get("/api/v1/bookings/" + bookingId)
        .then()
                .statusCode(200)
                .body("status", equalTo("PENDING_PAYMENT"));

        // 4. Verify failure alert event is published
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(eventCollector.getPaymentProcessedEvents())
                    .isNotEmpty()
                    .anyMatch(e -> "FAILED".equalsIgnoreCase(e.getStatus()) &&
                                   e.getFailureReason() != null &&
                                   e.getFailureReason().contains("Declined"));
        });

        log.info("═══ SUCCESS: testPaymentFailureAndDeclinedCard_DoesNotConfirmBooking passed ═══");
    }

    @Test
    @DisplayName("Scenario 04: Vehicle damage on return transitions vehicle to MAINTENANCE")
    void testVehicleDamageOnReturn_TransitionsVehicleToMaintenance() {
        log.info("═══ START: testVehicleDamageOnReturn_TransitionsVehicleToMaintenance ═══");

        // 1. Setup customer, provider, vehicle
        String customerToken = registerAndLogin("charlie@rentride.com", "CUSTOMER");
        String providerToken = registerAndLogin("fleet-c@rentride.com", "PROVIDER");

        Long vehicleId = createVehicle(providerToken, "WP-DAM-9911", new BigDecimal("12000.00"));
        Long bookingId = createBooking(customerToken, vehicleId, 2);

        // 2. Pay and confirm
        PaymentProcessRequest paymentReq = PaymentProcessRequest.builder()
                .bookingId(bookingId)
                .amount(new BigDecimal("24000.00"))
                .currency("LKR")
                .paymentMethod("CREDIT_CARD")
                .cardNumber("4111111111114242")
                .expiryMonth(12)
                .expiryYear(2029)
                .cvv("789")
                .cardHolderName("Charlie Brown")
                .build();

        given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType(ContentType.JSON)
                .body(paymentReq)
        .when()
                .post("/api/v1/payments/checkout")
        .then()
                .statusCode(200);

        // 3. Record pickup
        VehiclePickupRequest pickupReq = VehiclePickupRequest.builder()
                .odometerReading(10000.0)
                .fuelLevelPercent(100)
                .pickupNotes("Good condition")
                .build();

        given()
                .header("Authorization", "Bearer " + providerToken)
                .contentType(ContentType.JSON)
                .body(pickupReq)
        .when()
                .post("/api/v1/bookings/" + bookingId + "/pickup")
        .then()
                .statusCode(200)
                .body("bookingStatus", equalTo("ACTIVE_RENTAL"));

        // 4. Execute return with REQUIRES_MAINTENANCE
        VehicleReturnRequest returnReq = VehicleReturnRequest.builder()
                .odometerReading(10500.0)
                .fuelLevelPercent(80)
                .vehicleCondition("REQUIRES_MAINTENANCE")
                .additionalCharges(new BigDecimal("15000.00"))
                .returnNotes("Front bumper cracked, sent to workshop")
                .build();

        given()
                .header("Authorization", "Bearer " + providerToken)
                .contentType(ContentType.JSON)
                .body(returnReq)
        .when()
                .post("/api/v1/bookings/" + bookingId + "/return")
        .then()
                .statusCode(200)
                .body("bookingStatus", equalTo("COMPLETED"))
                .body("vehicleStatus", equalTo("MAINTENANCE"));

        // 5. Verify booking completed and vehicle in MAINTENANCE
        given()
                .header("Authorization", "Bearer " + customerToken)
        .when()
                .get("/api/v1/bookings/" + bookingId)
        .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));

        given()
        .when()
                .get("/api/v1/vehicles/" + vehicleId)
        .then()
                .statusCode(200)
                .body("status", equalTo("MAINTENANCE"));

        // 6. Verify vehicle is blocked from new customer search
        given()
                .queryParam("location", "Colombo")
        .when()
                .get("/api/v1/vehicles/search")
        .then()
                .statusCode(200)
                .body("content.licensePlateNumber", not(hasItem("WP-DAM-9911")));

        log.info("═══ SUCCESS: testVehicleDamageOnReturn_TransitionsVehicleToMaintenance passed ═══");
    }

    @Test
    @DisplayName("Scenario 03 & Security: Security and Role Enforcement at Gateway")
    void testSecurityAndRoleEnforcementAtGateway() {
        log.info("═══ START: testSecurityAndRoleEnforcementAtGateway ═══");

        String customerToken = registerAndLogin("regular.user@rentride.com", "CUSTOMER");

        // 1. Customer token attempting provider fleet endpoints -> 403 Forbidden
        given()
                .header("Authorization", "Bearer " + customerToken)
        .when()
                .get("/api/v1/vehicles/provider/my-fleet")
        .then()
                .statusCode(403)
                .body("message", containsString("insufficient privileges"));

        // 2. Customer token attempting vehicle creation -> 403 Forbidden
        VehicleCreateRequest vehicleReq = VehicleCreateRequest.builder()
                .make("Nissan")
                .model("Leaf")
                .manufacturingYear(2021)
                .licensePlateNumber("WP-FORBIDDEN-01")
                .seatingCapacity(5)
                .dailyRentalRate(new BigDecimal("9000.00"))
                .location("Kandy")
                .build();

        given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType(ContentType.JSON)
                .body(vehicleReq)
        .when()
                .post("/api/v1/vehicles")
        .then()
                .statusCode(403);

        // 3. Unauthenticated request to secured booking creation -> 401 Unauthorized
        BookingCreateRequest bookingReq = BookingCreateRequest.builder()
                .vehicleId(999L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(bookingReq)
        .when()
                .post("/api/v1/bookings")
        .then()
                .statusCode(401)
                .body("message", containsString("Missing or invalid authorization token"));

        log.info("═══ SUCCESS: testSecurityAndRoleEnforcementAtGateway passed ═══");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ──────────────────────────────────────────────────────────────────────────

    private String registerAndLogin(String email, String role) {
        if ("PROVIDER".equalsIgnoreCase(role)) {
            ProviderRegisterRequest req = ProviderRegisterRequest.builder()
                    .name("Fleet " + email)
                    .email(email)
                    .password("Password123!")
                    .phoneNumber("+94770000000")
                    .businessRegistrationNumber("BR-" + System.currentTimeMillis())
                    .companyName("Fleet Ltd")
                    .build();

            given()
                    .contentType(ContentType.JSON)
                    .body(req)
            .when()
                    .post("/api/v1/auth/register/provider")
            .then()
                    .statusCode(201);
        } else {
            CustomerRegisterRequest req = CustomerRegisterRequest.builder()
                    .name("User " + email)
                    .email(email)
                    .password("Password123!")
                    .phoneNumber("+94771111111")
                    .licenseNumber("DL-12345")
                    .build();

            given()
                    .contentType(ContentType.JSON)
                    .body(req)
            .when()
                    .post("/api/v1/auth/register/customer")
            .then()
                    .statusCode(201);
        }

        LoginRequest login = LoginRequest.builder()
                .email(email)
                .password("Password123!")
                .build();

        return given()
                .contentType(ContentType.JSON)
                .body(login)
        .when()
                .post("/api/v1/auth/login")
        .then()
                .statusCode(200)
                .extract().path("accessToken");
    }

    private Long createVehicle(String providerToken, String plate, BigDecimal rate) {
        VehicleCreateRequest req = VehicleCreateRequest.builder()
                .make("Toyota")
                .model("Axio")
                .manufacturingYear(2020)
                .licensePlateNumber(plate)
                .seatingCapacity(5)
                .dailyRentalRate(rate)
                .fuelType("HYBRID")
                .transmission("AUTOMATIC")
                .location("Colombo")
                .mileage(30000.0)
                .build();

        Number id = given()
                .header("Authorization", "Bearer " + providerToken)
                .contentType(ContentType.JSON)
                .body(req)
        .when()
                .post("/api/v1/vehicles")
        .then()
                .statusCode(201)
                .extract().path("id");

        return id.longValue();
    }

    private Long createBooking(String customerToken, Long vehicleId, int days) {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(days);

        BookingCreateRequest req = BookingCreateRequest.builder()
                .vehicleId(vehicleId)
                .startDate(start)
                .endDate(end)
                .pickupLocation("Colombo")
                .build();

        Number id = given()
                .header("Authorization", "Bearer " + customerToken)
                .contentType(ContentType.JSON)
                .body(req)
        .when()
                .post("/api/v1/bookings")
        .then()
                .statusCode(201)
                .extract().path("id");

        return id.longValue();
    }
}

