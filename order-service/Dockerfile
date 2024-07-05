# Build stage
FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the JAR file and pom.xml
COPY sagaevent-dto-1.0-SNAPSHOT.jar /app/
COPY pom.xml /app/
COPY settings.xml /usr/share/maven/ref/

# Install the JAR into the local Maven repository
RUN mvn install:install-file -Dfile=sagaevent-dto-1.0-SNAPSHOT.jar -DgroupId=org.example -DartifactId=common -Dversion=1.0 -Dpackaging=jar

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests

# Run stage
FROM openjdk:21-jdk

COPY --from=build /app/target/*.jar /app/app.jar
COPY src/main/resources/scheme.sql /app/scheme.sql

EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]