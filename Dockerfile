# ============================================================================
# STAGE 1: MAVEN COMPILATION AND JAR BUILDING
# ============================================================================
FROM maven:3.8.5-openjdk-17-slim AS builder
WORKDIR /app

# 1. Copy POM and compile dependencies to cache them (improves subsequent build times)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copy source files and compile/package the executable JAR binary
COPY src ./src
RUN mvn clean package -DskipTests

# ============================================================================
# STAGE 2: LIGHTWEIGHT ECLIPSE TEMURIN RUNTIME RUNTIME CONTAINER
# ============================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 1. Copy the packaged JAR file from the builder stage
COPY --from=builder /app/target/eduverse-0.0.1-SNAPSHOT.jar /app/eduverse.jar

# 2. Expose port 8080 to host system traffic
EXPOSE 8080

# 3. Secure environment setups (default to production profile inside containers)
ENV SPRING_PROFILES_ACTIVE=prod

# 4. Boot up the Spring Boot JAR application
ENTRYPOINT ["java", "-jar", "/app/eduverse.jar"]
