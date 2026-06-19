### Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy pom first for dependency caching
COPY pom.xml ./

RUN mvn -B -ntp dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN mvn -B -ntp clean package -DskipTests


### Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy jar from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Create non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]