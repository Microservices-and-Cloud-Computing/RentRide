# RentRide — API Contracts & Swagger UI Documentation

This document catalogs the REST API interfaces exposed across the RentRide microservices platform, including interactive Swagger UI dashboards, endpoint specifications, and sample cURL requests.

---

## 1. Interactive Swagger UI & OpenAPI Specification Links

When running the RentRide cluster locally or via Docker Compose, OpenAPI 3 (Swagger UI) documentation is hosted on each respective microservice:

| Microservice | Swagger UI URL | Raw OpenAPI 3 JSON | Primary Purpose |
|---|---|---|---|
| **API Gateway** | [http://localhost:8080](http://localhost:8080) | N/A (Edge Proxy) | Edge entry point routing all downstream APIs |
| **User Service** | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs) | Authentication, JWT, and user profiles |
| **Vehicle Service** | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) | [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs) | Fleet inventory, search, and category management |
| **Booking Service** | [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) | [http://localhost:8083/v3/api-docs](http://localhost:8083/v3/api-docs) | Reservation lifecycles, handovers, and returns |
| **Payment Service** | [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html) | [http://localhost:8084/v3/api-docs](http://localhost:8084/v3/api-docs) | Checkout execution and transaction verification |
| **Notification Service** | [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html) | [http://localhost:8085/v3/api-docs](http://localhost:8085/v3/api-docs) | Notification health and internal dispatch APIs |
| **Eureka Registry** | [http://localhost:8761](http://localhost:8761) | N/A | Service discovery registry dashboard |
| **RabbitMQ Console** | [http://localhost:15672](http://localhost:15672) | N/A | Queue monitoring (User: `guest` / Pass: `guest`) |
| **MailHog UI** | [http://localhost:8025](http://localhost:8025) | N/A | Web UI for inspecting transactional emails |

---

## 2. API Endpoint Catalog & Sample cURL Requests

All requests below target the unified **API Gateway** at `http://localhost:8080`.

### A. Authentication & User Management (`user-service`)

#### 1. Register Customer
```bash
curl -X POST http://localhost:8080/api/v1/auth/register/customer \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Sarah Connor",
    "email": "sarah.connor@rentride.com",
    "password": "Password123!",
    "phoneNumber": "+94771234567",
    "driverLicenseNumber": "B1234567"
  }'
```

#### 2. Register Provider
```bash
curl -X POST http://localhost:8080/api/v1/auth/register/provider \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Fleet Manager",
    "email": "provider@rentride.com",
    "password": "Password123!",
    "phoneNumber": "+94779876543",
    "businessRegistrationNumber": "PV-123456",
    "companyName": "RentRide Fleet Services Ltd"
  }'
```

#### 3. Login & Obtain JWT Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "sarah.connor@rentride.com",
    "password": "Password123!"
  }'
```

---

### B. Vehicle & Fleet Management (`vehicle-service`)

#### 1. Public Inventory Search with Filters
```bash
curl -X GET "http://localhost:8080/api/v1/vehicles/search?location=Colombo&minPrice=5000&maxPrice=15000&page=0&size=10"
```

#### 2. Get Vehicle Details by ID
```bash
curl -X GET http://localhost:8080/api/v1/vehicles/1
```

#### 3. Check Vehicle Availability
```bash
curl -X GET http://localhost:8080/api/v1/vehicles/1/availability
```

#### 4. Provider Onboards New Vehicle (Requires `PROVIDER` role)
```bash
curl -X POST http://localhost:8080/api/v1/vehicles \
  -H "Authorization: Bearer <provider_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "make": "Toyota",
    "model": "Premio",
    "manufacturingYear": 2022,
    "licensePlateNumber": "WP-CBA-2026",
    "seatingCapacity": 5,
    "dailyRentalRate": 10000.00,
    "fuelType": "PETROL",
    "transmission": "AUTOMATIC",
    "location": "Colombo",
    "mileage": 25000.0
  }'
```

#### 5. Get Authenticated Provider Fleet
```bash
curl -X GET http://localhost:8080/api/v1/vehicles/provider/my-fleet \
  -H "Authorization: Bearer <provider_jwt_token>"
```

#### 6. Update Vehicle Operational Status (Internal / Provider)
```bash
curl -X PATCH http://localhost:8080/api/v1/vehicles/1/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "RENTED",
    "reason": "Handover completed for reservation RR-BK-20261104-001"
  }'
```

---

### C. Booking & Reservation Management (`booking-service`)

#### 1. Create Vehicle Reservation
```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Authorization: Bearer <customer_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "vehicleId": 1,
    "startDate": "2026-11-05",
    "endDate": "2026-11-08",
    "pickupLocation": "Colombo Central"
  }'
```

#### 2. Get Booking Details
```bash
curl -X GET http://localhost:8080/api/v1/bookings/1 \
  -H "Authorization: Bearer <customer_jwt_token>"
```

#### 3. Record Vehicle Handover (Pickup)
```bash
curl -X POST http://localhost:8080/api/v1/bookings/1/pickup \
  -H "Authorization: Bearer <provider_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "odometerReading": 25000.0,
    "fuelLevelPercent": 100,
    "pickupNotes": "Vehicle handed over clean, no prior scratch"
  }'
```

#### 4. Record Vehicle Return Inspection
```bash
curl -X POST http://localhost:8080/api/v1/bookings/1/return \
  -H "Authorization: Bearer <provider_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "odometerReading": 25450.0,
    "fuelLevelPercent": 95,
    "vehicleCondition": "GOOD",
    "additionalCharges": 0.00,
    "returnNotes": "Returned on time in pristine condition"
  }'
```

---

### D. Payment & Checkout Settlement (`payment-service`)

#### 1. Process Reservation Checkout (Success: card ending in `4242`)
```bash
curl -X POST http://localhost:8080/api/v1/payments/checkout \
  -H "Authorization: Bearer <customer_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": 1,
    "amount": 30000.00,
    "currency": "LKR",
    "paymentMethod": "CREDIT_CARD",
    "cardNumber": "4111111111114242",
    "expiryMonth": 12,
    "expiryYear": 2028,
    "cvv": "123",
    "cardHolderName": "Sarah Connor"
  }'
```

#### 2. Process Reservation Checkout (Declined: card ending in `0000`)
```bash
curl -X POST http://localhost:8080/api/v1/payments/checkout \
  -H "Authorization: Bearer <customer_jwt_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": 1,
    "amount": 30000.00,
    "currency": "LKR",
    "paymentMethod": "CREDIT_CARD",
    "cardNumber": "4111111111110000",
    "expiryMonth": 10,
    "expiryYear": 2027,
    "cvv": "456",
    "cardHolderName": "Sarah Connor"
  }'
```

#### 3. Verify Payment Transaction by Reference
```bash
curl -X GET http://localhost:8080/api/v1/payments/verify/TXN-20261104-A1B2C3
```

---

### E. Health Actuators & Metrics Across Cluster

Check readiness and liveness status:
```bash
# Gateway Health
curl -s http://localhost:8080/actuator/health

# User Service Health
curl -s http://localhost:8081/actuator/health

# Vehicle Service Health
curl -s http://localhost:8082/actuator/health

# Booking Service Health
curl -s http://localhost:8083/actuator/health

# Payment Service Health
curl -s http://localhost:8084/actuator/health

# Notification Service Health
curl -s http://localhost:8085/actuator/health
```
All return `{"status":"UP"}`.
