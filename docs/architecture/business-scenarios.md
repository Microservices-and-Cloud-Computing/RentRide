# RentRide — Core Business Scenarios & Workflow Sequence Diagrams

This document details the five core end-to-end distributed business workflows implemented across the RentRide microservices platform.

---

## Scenario 01: Customer Vehicle Discovery, Filtering & Reservation

A customer registers, logs in, browses the fleet catalog using multi-attribute criteria (location, pricing, transmission, fuel type), checks availability, and reserves a vehicle for specified dates.

```mermaid
sequenceDiagram
    autonumber
    actor Customer as RentRide Customer
    participant Gateway as API Gateway (:8080)
    participant UserSvc as User-Service (:8081)
    participant VehicleSvc as Vehicle-Service (:8082)
    participant BookingSvc as Booking-Service (:8083)

    %% 1. Registration & Login
    Customer->>Gateway: POST /api/v1/auth/register/customer
    Gateway->>UserSvc: Forward registration request
    UserSvc->>UserSvc: Hash password with BCrypt & persist User entity
    UserSvc-->>Gateway: 201 Created (User details)
    Gateway-->>Customer: 201 Created

    Customer->>Gateway: POST /api/v1/auth/login
    Gateway->>UserSvc: Verify credentials
    UserSvc->>UserSvc: Authenticate & generate HMAC-SHA256 JWT
    UserSvc-->>Gateway: 200 OK (accessToken, role: CUSTOMER)
    Gateway-->>Customer: 200 OK with Bearer Token

    %% 2. Catalog Search & Discovery
    Customer->>Gateway: GET /api/v1/vehicles/search?location=Colombo&maxPrice=15000
    Gateway->>VehicleSvc: Route catalog search
    VehicleSvc->>VehicleSvc: Query active inventory (status == AVAILABLE)
    VehicleSvc-->>Gateway: 200 OK (PaginatedResponse of vehicles)
    Gateway-->>Customer: 200 OK with matching vehicles

    %% 3. Availability Check & Reservation Initiation
    Customer->>Gateway: GET /api/v1/vehicles/{id}/availability
    Gateway->>VehicleSvc: Check availability status
    VehicleSvc-->>Gateway: 200 OK (available: true, dailyRate: 10000.00)
    Gateway-->>Customer: 200 OK

    Customer->>Gateway: POST /api/v1/bookings (Authorization: Bearer <token>)
    Gateway->>Gateway: Validate JWT & inject X-User-Id, X-User-Role headers
    Gateway->>BookingSvc: Route reservation creation
    BookingSvc->>VehicleSvc: Feign: GET /api/v1/vehicles/{id}/availability
    VehicleSvc-->>BookingSvc: 200 OK (available: true)
    BookingSvc->>BookingSvc: Calculate totalDays (3) and totalCost (30,000 LKR)
    BookingSvc->>BookingSvc: Persist Booking entity (status: PENDING_PAYMENT)
    BookingSvc-->>Gateway: 201 Created (BookingResponse with bookingReference)
    Gateway-->>Customer: 201 Created (Reservation awaiting payment)
```

---

## Scenario 02: Online Payment Execution & Feign Booking Confirmation

The customer settles the reservation cost using a payment card. `payment-service` verifies the booking, invokes the card gateway simulator, updates transaction status, and invokes an OpenFeign callback to `booking-service` to confirm the reservation.

```mermaid
sequenceDiagram
    autonumber
    actor Customer as RentRide Customer
    participant Gateway as API Gateway (:8080)
    participant PaymentSvc as Payment-Service (:8084)
    participant BookingSvc as Booking-Service (:8083)
    participant Rabbit as RabbitMQ (:5672)

    Customer->>Gateway: POST /api/v1/payments/checkout
    Gateway->>Gateway: Validate JWT & extract customerId
    Gateway->>PaymentSvc: Route checkout payload (X-User-Id: 1)
    
    %% Verify Booking Pre-check via Feign
    PaymentSvc->>BookingSvc: Feign: GET /api/v1/bookings/{bookingId}
    BookingSvc-->>PaymentSvc: 200 OK (status: PENDING_PAYMENT, totalCost: 30000.00)
    
    %% Payment Settlement
    alt Card Approved (Card ends in 4242)
        PaymentSvc->>PaymentSvc: Persist Payment record (status: SUCCESS, txnRef)
        PaymentSvc->>BookingSvc: Feign Callback: PATCH /api/v1/bookings/{id}/payment-status (SUCCESS)
        BookingSvc->>BookingSvc: Advance status from PENDING_PAYMENT to CONFIRMED
        BookingSvc->>Rabbit: Asynchronously publish BookingConfirmedEvent
        BookingSvc-->>PaymentSvc: 200 OK (Booking updated)
        PaymentSvc->>Rabbit: Asynchronously publish PaymentProcessedEvent (SUCCESS)
        PaymentSvc-->>Gateway: 200 OK (PaymentResponse: COMPLETED, receiptUrl)
        Gateway-->>Customer: 200 OK (Payment Approved & Booking Confirmed)
    else Card Declined (Card ends in 0000)
        PaymentSvc->>PaymentSvc: Persist Payment record (status: FAILED, reason: "Declined")
        PaymentSvc->>BookingSvc: Feign Callback: PATCH /api/v1/bookings/{id}/payment-status (FAILED)
        BookingSvc->>BookingSvc: Retain status in PENDING_PAYMENT
        BookingSvc-->>PaymentSvc: 200 OK (Booking acknowledged)
        PaymentSvc->>Rabbit: Asynchronously publish PaymentProcessedEvent (FAILED)
        PaymentSvc-->>Gateway: 200 OK (PaymentResponse: FAILED)
        Gateway-->>Customer: 200 OK (Payment Declined)
    end
```

---

## Scenario 03: Rental Provider Fleet Onboarding & Rate Management

A registered rental fleet provider logs in, onboards a new vehicle with specifications (make, model, license plate, daily rate, location), and manages their private fleet with role-based access control.

```mermaid
sequenceDiagram
    autonumber
    actor Provider as Fleet Provider
    actor Customer as Regular Customer
    participant Gateway as API Gateway (:8080)
    participant VehicleSvc as Vehicle-Service (:8082)

    %% Provider Onboarding
    Provider->>Gateway: POST /api/v1/vehicles (Authorization: Bearer <providerToken>)
    Gateway->>Gateway: Verify JWT (role: PROVIDER) & inject X-User-Role: PROVIDER
    Gateway->>VehicleSvc: Forward vehicle registration
    VehicleSvc->>VehicleSvc: Validate license plate uniqueness
    VehicleSvc->>VehicleSvc: Persist Vehicle entity (status: AVAILABLE)
    VehicleSvc-->>Gateway: 201 Created (VehicleResponse)
    Gateway-->>Provider: 201 Created with Vehicle Details

    %% Query Provider's Fleet
    Provider->>Gateway: GET /api/v1/vehicles/provider/my-fleet
    Gateway->>Gateway: Verify role is PROVIDER or ADMIN
    Gateway->>VehicleSvc: Forward request with X-User-Id: <providerId>
    VehicleSvc->>VehicleSvc: Query all vehicles matching providerId
    VehicleSvc-->>Gateway: 200 OK (List of VehicleResponse)
    Gateway-->>Provider: 200 OK (Fleet inventory list)

    %% Security Enforcement: Unauthorized Customer Attempt
    Customer->>Gateway: GET /api/v1/vehicles/provider/my-fleet (Authorization: Bearer <customerToken>)
    Gateway->>Gateway: Validate JWT claims (role: CUSTOMER)
    Note over Gateway: Role check failed: CUSTOMER cannot access provider fleet
    Gateway-->>Customer: 403 Forbidden ("Access denied: insufficient privileges")
```

---

## Scenario 04: Vehicle Handover (Pickup) & Return Inspection Cycle

The customer arrives at the provider branch to collect the vehicle. The provider logs the handover (pickup metrics). Later, upon return, the provider inspects condition and logs odometer, fuel, and damages, synchronizing both booking and vehicle states.

```mermaid
sequenceDiagram
    autonumber
    actor Provider as Fleet Provider
    participant Gateway as API Gateway (:8080)
    participant BookingSvc as Booking-Service (:8083)
    participant VehicleSvc as Vehicle-Service (:8082)
    participant Rabbit as RabbitMQ (:5672)

    %% 1. Handover (Pickup)
    Provider->>Gateway: POST /api/v1/bookings/{id}/pickup (odometer: 25000, fuel: 100%)
    Gateway->>BookingSvc: Forward handover request (X-User-Id: <providerId>)
    BookingSvc->>BookingSvc: Verify provider authorization & state == CONFIRMED
    BookingSvc->>BookingSvc: Transition status to ACTIVE_RENTAL
    BookingSvc->>VehicleSvc: Feign: PATCH /api/v1/vehicles/{id}/status (RENTED)
    VehicleSvc-->>BookingSvc: 200 OK (Vehicle status = RENTED)
    BookingSvc->>Rabbit: Publish RentalLifecycleEvent (PICKUP_COMPLETED)
    BookingSvc-->>Gateway: 200 OK (BookingLifecycleResponse)
    Gateway-->>Provider: 200 OK (Pickup Completed)

    %% 2. Return Inspection
    alt Clean Return (Condition: GOOD)
        Provider->>Gateway: POST /api/v1/bookings/{id}/return (odometer: 25450, condition: GOOD)
        Gateway->>BookingSvc: Forward return inspection
        BookingSvc->>BookingSvc: Verify state == ACTIVE_RENTAL
        BookingSvc->>BookingSvc: Transition status to COMPLETED
        BookingSvc->>VehicleSvc: Feign: PATCH /api/v1/vehicles/{id}/status (AVAILABLE)
        VehicleSvc-->>BookingSvc: 200 OK (Vehicle status = AVAILABLE)
        BookingSvc->>Rabbit: Publish RentalLifecycleEvent (RETURN_COMPLETED)
        BookingSvc-->>Gateway: 200 OK (Booking COMPLETED, Vehicle AVAILABLE)
        Gateway-->>Provider: 200 OK
    else Damaged Return (Condition: REQUIRES_MAINTENANCE)
        Provider->>Gateway: POST /api/v1/bookings/{id}/return (condition: REQUIRES_MAINTENANCE, charges: 15000)
        Gateway->>BookingSvc: Forward return inspection
        BookingSvc->>BookingSvc: Record additional charges and damage notes
        BookingSvc->>BookingSvc: Transition status to COMPLETED
        BookingSvc->>VehicleSvc: Feign: PATCH /api/v1/vehicles/{id}/status (MAINTENANCE)
        VehicleSvc-->>BookingSvc: 200 OK (Vehicle status = MAINTENANCE)
        BookingSvc->>Rabbit: Publish RentalLifecycleEvent (RETURN_COMPLETED)
        BookingSvc-->>Gateway: 200 OK (Booking COMPLETED, Vehicle MAINTENANCE)
        Gateway-->>Provider: 200 OK (Vehicle automatically locked from new bookings)
    end
```

---

## Scenario 05: Asynchronous Event Messaging & Customer Notifications

Transactional domain events raised by `booking-service` and `payment-service` are queued in RabbitMQ and processed asynchronously by `notification-service`, which renders responsive Thymeleaf HTML templates and delivers emails via MailHog.

```mermaid
sequenceDiagram
    autonumber
    participant BookingSvc as Booking-Service
    participant PaymentSvc as Payment-Service
    participant Exchange as RabbitMQ (rentride.exchange)
    participant BookingQueue as notification.booking.confirmed.queue
    participant PaymentQueue as notification.payment.processed.queue
    participant RentalQueue as notification.rental.lifecycle.queue
    participant NotifSvc as Notification-Service
    participant MailHog as MailHog SMTP (:1025)

    %% Event Publishing
    BookingSvc->>Exchange: Publish BookingConfirmedEvent (key: booking.event.confirmed)
    PaymentSvc->>Exchange: Publish PaymentProcessedEvent (key: payment.event.processed)
    BookingSvc->>Exchange: Publish RentalLifecycleEvent (key: rental.event.pickup)
    BookingSvc->>Exchange: Publish RentalLifecycleEvent (key: rental.event.return)

    %% Queue Routing
    Exchange->>BookingQueue: Route booking.event.confirmed
    Exchange->>PaymentQueue: Route payment.event.processed
    Exchange->>RentalQueue: Route rental.event.*

    %% Event Consumption & Email Rendering
    par Booking Confirmed Email
        BookingQueue->>NotifSvc: Consume BookingConfirmedEvent
        NotifSvc->>NotifSvc: Populate Thymeleaf template: booking-confirmation.html
        NotifSvc->>MailHog: Send MIME Email to customer & provider
    and Payment Receipt Email
        PaymentQueue->>NotifSvc: Consume PaymentProcessedEvent
        NotifSvc->>NotifSvc: Populate Thymeleaf template: payment-receipt.html
        NotifSvc->>MailHog: Send Payment Transaction Receipt
    and Rental Status Email
        RentalQueue->>NotifSvc: Consume RentalLifecycleEvent
        NotifSvc->>NotifSvc: Populate Thymeleaf template: rental-status.html
        NotifSvc->>MailHog: Send Handover / Return Inspection Report
    end
```
