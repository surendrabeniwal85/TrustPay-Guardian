# ==============================================================================
# TrustPay Guardian — Backend Multi-Stage Dockerfile (Java 25 LTS)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build JAR using Maven Wrapper and Temurin JDK 25
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-noble AS builder

WORKDIR /build

# Copy Maven wrapper and POM for dependency pre-caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Resolve dependencies offline for layer caching
RUN ./mvnw dependency:go-offline -B || true

# Copy source tree and compile application package
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-noble AS runtime

# Metadata labels
LABEL maintainer="TrustPay Guardian Engineering Team"
LABEL description="TrustPay Guardian Fraud-Aware Digital Banking Platform - Backend API Service"
LABEL version="1.0.0"

# Install curl for healthcheck ping
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Run as non-privileged user for security compliance (OWASP / CIS Docker Benchmark)
RUN groupadd -g 10001 appgroup && \
    useradd -u 10001 -g appgroup -s /bin/sh -d /home/appuser -m appuser

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar /app/app.jar

USER appuser:appgroup

EXPOSE 8080

# Health check against Spring Boot Actuator
HEALTHCHECK --interval=20s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Duser.timezone=Asia/Kolkata", \
  "-jar", "/app/app.jar"]
