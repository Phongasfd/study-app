### Build stage: use Maven with JDK to build the application
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /workspace

# copy pom and download dependencies first for better caching
COPY pom.xml ./
COPY mvnw mvnw
COPY .mvn .mvn
RUN mvn -B -ntp dependency:go-offline

# copy sources and build
COPY src ./src
RUN mvn -B -ntp package


### Run stage: use lightweight JRE
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the fat jar built by Maven
COPY --from=builder /workspace/target/*.jar app.jar

RUN addgroup --system spring && adduser --system spring --ingroup spring

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
