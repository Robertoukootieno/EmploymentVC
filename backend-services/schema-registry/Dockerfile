# Multi-stage build for Schema Registry
FROM gradle:8.5-jdk21 AS build
WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy source code
COPY backend-services/schema-registry ./backend-services/schema-registry
COPY backend-libraries ./backend-libraries

# Build the application
RUN gradle :backend-services:schema-registry:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/backend-services/schema-registry/build/libs/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8086/actuator/health || exit 1

# Expose port
EXPOSE 8086

# Run application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
