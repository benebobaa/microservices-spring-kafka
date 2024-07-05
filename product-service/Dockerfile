# Build stage
FROM maven:3.9.6-eclipse-temurin-22-jammy AS build
WORKDIR /app
COPY pom.xml .
COPY sagaevent-dto-1.0-SNAPSHOT.jar /app/sagaevent-dto-1.0-SNAPSHOT.jar
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Run stage
FROM openjdk:22-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY src/main/resources/application.properties /app/application.properties

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]