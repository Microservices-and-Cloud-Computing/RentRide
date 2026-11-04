# RentRide — Distributed System Architecture & Design Documentation

## 1. System Context & Component Architecture

RentRide is structured as a decentralized, cloud-native microservices architecture designed to handle high-throughput vehicle rentals, dynamic pricing, concurrent inventory booking, and real-time customer event notifications.

### High-Level Architecture Diagram

```mermaid
flowchart TD
    subgraph External["Client Layer"]
        UserBrowser["Web Application (React / Angular)"]
        MobileApp["Mobile App (Flutter / Android)"]
    end

    subgraph Perimeter["Security & Edge Routing"]
        Gateway["Spring Cloud Gateway :8080\n- Stateless JWT Validation\n- Claim Extraction & Forwarding\n- Global CORS Handler\n- Path-Based Routing"]
    end

    subgraph ServiceMesh["Microservices Mesh (Spring Boot 3.4.5 / Java 21)"]
        Eureka["Netflix Eureka Server :8761\n- Service Registration & Discovery\n- Heartbeat Monitoring\n- Dynamic Client Load Balancing"]
        
        UserService["User Service :8081\n- Authentication & RBAC\n- BCrypt Passwords\n- JWT Token Provider"]
        
        VehicleService["Vehicle Service :8082\n- Vehicle Inventory & Search\n- Category Management\n- Status State Transitions"]
        
        BookingService["Booking Service :8083\n- Reservation State Machine\n- Rental Handover & Return\n- Availability Pre-checks"]
        
        PaymentService["Payment Service :8084\n- Payment Processing\n- Card Gateway Simulator\n- Transaction Verification"]
        
        NotificationService["Notification Service :8085\n- AMQP Event Consumers\n- Thymeleaf HTML Renderer\n- SMTP Mail Dispatcher"]
    end

    subgraph MessagingBroker["Event Messaging Backbone"]
        RabbitMQ["RabbitMQ Broker :5672\n- Exchange: rentride.exchange (Topic)\n- Exchange: rentride.dlx (Dead Letter)\n- Queues: booking, payment, rental, dlq"]
    end

    subgraph StorageLayer["Data Tier (PostgreSQL 15 Container)"]
        UserDB[("rentride_user_db")]
        VehicleDB[("rentride_vehicle_db")]
        BookingDB[("rentride_booking_db")]
        PaymentDB[("rentride_payment_db")]
        NotificationDB[("rentride_notification_db")]
    end

    subgraph NotificationSink["Mail Infrastructure"]
        MailHog["MailHog SMTP Server :1025\n- Web UI :8025\n- Mail Inspection Sink"]
    end

    %% Client Routing
    UserBrowser -->|HTTP REST| Gateway
    MobileApp -->|HTTP REST| Gateway

    %% Gateway to Services
    Gateway -->|Discovery Lookup| Eureka
    Gateway -->|/api/v1/auth/**, /api/v1/users/**| UserService
    Gateway -->|/api/v1/vehicles/**| VehicleService
    Gateway -->|/api/v1/bookings/**| BookingService
    Gateway -->|/api/v1/payments/**| PaymentService
    Gateway -->|/api/v1/notifications/**| NotificationService

    %% Service to Eureka
    UserService -.->|Register & Heartbeat| Eureka
    VehicleService -.->|Register & Heartbeat| Eureka
    BookingService -.->|Register & Heartbeat| Eureka
    PaymentService -.->|Register & Heartbeat| Eureka
    NotificationService -.->|Register & Heartbeat| Eureka

    %% Synchronous Inter-Service Feign Calls
    BookingService -->|OpenFeign: Check Availability| VehicleService
    BookingService -->|OpenFeign: Update Vehicle Status| VehicleService
    PaymentService -->|OpenFeign: Verify Booking Pre-check| BookingService
    PaymentService -->|OpenFeign: Update Payment Status Callback| BookingService

    %% Asynchronous Event Publishing
    BookingService -->|Publish: booking.event.confirmed| RabbitMQ
    BookingService -->|Publish: rental.event.*| RabbitMQ
    PaymentService -->|Publish: payment.event.processed| RabbitMQ

    %% Event Consumption
    RabbitMQ -->|Consume Events| NotificationService
    NotificationService -->|SMTP Email Dispatch| MailHog

    %% Persistence Links
    UserService --- UserDB
    VehicleService --- VehicleDB
    BookingService --- BookingDB
    PaymentService --- PaymentDB
    NotificationService --- NotificationDB
```

---

## 2. Distributed Transaction Strategy

In a microservices architecture with a database-per-service pattern, distributed ACID transactions (2PC) are avoided due to latency and availability concerns (CAP theorem). RentRide combines **Synchronous Orchestration** with **Choreographed Eventual Consistency**:

### 1. Synchronous Feign Invocations (Critical Invariants)
- **Vehicle Availability Check:** Before creating a reservation in `booking-service`, it invokes `vehicle-service` via OpenFeign (`GET /api/v1/vehicles/{id}/availability`) to verify the vehicle exists and is in `AVAILABLE` state.
- **Payment Verification:** Before charging a card, `payment-service` calls `booking-service` (`GET /api/v1/bookings/{id}`) to confirm the reservation is in `PENDING_PAYMENT` state and the checkout amount matches the booking's calculated total cost.
- **Settlement Callback:** Upon card approval, `payment-service` invokes `PATCH /api/v1/bookings/{id}/payment-status` to transition the booking state atomically from `PENDING_PAYMENT` to `CONFIRMED`.

### 2. Asynchronous Event-Driven Choreography (Decoupled Notification Delivery)
- Once the database transaction commits in `booking-service` or `payment-service`, domain events are dispatched asynchronously over RabbitMQ:
  - `BookingConfirmedEvent` on routing key `booking.event.confirmed`
  - `PaymentProcessedEvent` on routing key `payment.event.processed`
  - `RentalLifecycleEvent` on routing key `rental.event.pickup` and `rental.event.return`
- `notification-service` independently consumes these messages, generates responsive HTML templates, and sends emails without blocking caller response latency or tying transaction boundaries together.

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant Gateway as API Gateway
    participant Payment as Payment-Service
    participant Booking as Booking-Service
    participant Rabbit as RabbitMQ (rentride.exchange)
    participant Notification as Notification-Service
    participant Mail as MailHog (SMTP)

    Customer->>Gateway: POST /api/v1/payments/checkout
    Gateway->>Payment: Route checkout request
    Note over Payment: Validate card details & amount
    Payment->>Booking: GET /api/v1/bookings/{id} (Verify state == PENDING_PAYMENT)
    Booking-->>Payment: 200 OK (Booking details verified)
    Note over Payment: Process payment via Gateway Simulator
    Payment->>Payment: Save Payment entity (Status = SUCCESS)
    Payment->>Booking: PATCH /api/v1/bookings/{id}/payment-status (Status = SUCCESS)
    Booking->>Booking: Update status to CONFIRMED
    Booking-->>Payment: 200 OK (Booking confirmed)
    
    par Async Event Publication
        Payment->>Rabbit: Publish PaymentProcessedEvent (payment.event.processed)
    and
        Booking->>Rabbit: Publish BookingConfirmedEvent (booking.event.confirmed)
    end

    Payment-->>Gateway: 200 OK (PaymentResponse: COMPLETED)
    Gateway-->>Customer: 200 OK (Checkout Success)

    par Async Consumption
        Rabbit->>Notification: Deliver PaymentProcessedEvent
        Notification->>Mail: Send Payment Receipt Email
    and
        Rabbit->>Notification: Deliver BookingConfirmedEvent
        Notification->>Mail: Send Booking Confirmation Email
    end
```

---

## 3. Resilience, Fault Tolerance & Recovery Patterns

### 1. Declarative OpenFeign Client Fallbacks
Every inter-service Feign client defines a resilient `FallbackFactory`:
- **`VehicleServiceClientFallbackFactory`:** When `vehicle-service` is unreachable or times out, the fallback returns safe default availability responses (`available: false`) with meaningful failure logging, preventing cascading failures in `booking-service`.
- **`BookingServiceClientFallbackFactory`:** If `booking-service` fails during payment callback, the payment record is marked with `REQUIRES_RECONCILIATION` and a high-priority compensation alert is logged for automated operational remediation.

### 2. Custom Feign Error Decoders
- Downstream HTTP 4xx and 5xx errors are caught by `CustomFeignErrorDecoder`.
- Specific domain exceptions (`VehicleNotFoundException`, `BookingNotFoundException`, `InvalidBookingStateException`) are re-instantiated in the caller context preserving original error codes and diagnostic messages.

### 3. Edge Security & Claim Propagation
- Edge-level token verification occurs within `AuthenticationFilter` in `api-gateway`.
- Decoded JWT claims (`userId`, `email`, `role`) are injected as downstream request headers (`X-User-Id`, `X-User-Email`, `X-User-Role`).
- Internal microservices read these trusted headers without re-verifying the JWT cryptographic signature on every hop.

### 4. RabbitMQ Dead Letter Exchange (DLQ) Strategy
```text
Topic Exchange: rentride.exchange
  ├── routing key: booking.event.confirmed ──> notification.booking.confirmed.queue
  ├── routing key: payment.event.processed ──> notification.payment.processed.queue
  └── routing key: rental.event.*          ──> notification.rental.lifecycle.queue

Dead Letter Exchange: rentride.dlx
  └── routing key: #                       ──> notification.dead-letter.queue
```
- Exponential backoff retry is configured on consumers (3 attempts, initial interval 2000ms, multiplier 2.0).
- If consumer execution fails repeatedly (e.g., SMTP outage or malformed payload), the message is routed to `notification.dead-letter.queue` without re-queuing, avoiding poison-pill crashes.
