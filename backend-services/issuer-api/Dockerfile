# -------- Build Stage --------
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
# Copy entire project
COPY . .
# Build the application
RUN ./gradlew :issuer-api:bootJar --no-daemon

# -------- Runtime Stage --------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Correct physical path
COPY --from=build /app/backend-services/issuer-api/build/libs/*.jar app.jar

# Expose port
EXPOSE 8083

# Health check(requires actuator dependency)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
