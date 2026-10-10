# syntax=docker/dockerfile:1

# Build stage: compile and package the Spring Boot jar with the Maven wrapper
FROM eclipse-temurin:25.0.4.1_1-jdk-noble AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

# Runtime stage: JRE only, non-root user
FROM eclipse-temurin:25.0.4.1_1-jre-noble
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/target/*.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
