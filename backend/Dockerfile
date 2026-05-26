FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY automation automation
COPY backend backend
RUN mvn -pl backend -am -DskipTests package

FROM mcr.microsoft.com/playwright/java:v1.49.0-jammy
WORKDIR /app
COPY --from=build /workspace/backend/target/backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
