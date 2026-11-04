# ==============================================================================
# RentRide Platform - Vehicle & Fleet Management Microservice
# Multi-Stage Production Dockerfile
# ==============================================================================

# ── Stage 1: Build & Dependency Caching ──────────────────────────────────────
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21-alpine
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre-alpine

FROM ${MAVEN_IMAGE} AS builder
WORKDIR /workspace/app

# Pre-cache Maven dependencies via POM layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy application source and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Minimal Production Runtime ──────────────────────────────────────
FROM ${RUNTIME_IMAGE}
WORKDIR /app

# Install diagnostic utilities & create dedicated non-root application user
RUN apk add --no-cache curl wget \
    && addgroup -S spring && adduser -S spring -G spring

# Copy compiled JAR from build stage
COPY --from=builder /workspace/app/target/*.jar app.jar
RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8082

# Actuator Health Check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8082/actuator/health | grep UP || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

