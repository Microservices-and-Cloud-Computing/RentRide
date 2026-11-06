# ── Stage 1: Build (Maven packaging using Eclipse Temurin 21 Alpine) ──────────
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy application source and build JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ── Stage 2: Runtime (Minimal JRE runtime) ────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for container health check
RUN apk add --no-cache curl

# Create non-root system user and group
RUN addgroup -S rentride && adduser -S rentride -G rentride
USER rentride

# Copy generated executable JAR from builder stage
COPY --from=builder --chown=rentride:rentride /build/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
