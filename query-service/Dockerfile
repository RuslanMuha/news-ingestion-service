# Multi-stage build for query-service
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent pom and all module poms first for dependency resolution
# Maven validates all modules from parent pom.xml, so we need all pom.xml files
COPY pom.xml .
COPY common-module/pom.xml ./common-module/
COPY data-ingestion-service/pom.xml ./data-ingestion-service/
COPY query-service/pom.xml ./query-service/

# Download dependencies (cached layer if pom.xml doesn't change)
# Use -pl to specify only the modules we need (common-module is a dependency)
RUN mvn dependency:go-offline -B -pl common-module,query-service -am

# Copy source code
COPY common-module ./common-module
COPY query-service ./query-service

# Build the application
RUN mvn clean package -DskipTests -B -pl common-module,query-service -am

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy the built JAR from build stage
COPY --from=build /app/query-service/target/query-service-*.jar app.jar

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health/readiness || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

