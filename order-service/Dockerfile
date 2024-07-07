# Build stage
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app
COPY pom-docker.xml pom.xml
COPY settings.xml /usr/share/maven/ref/
COPY src ./src
COPY sagaevent-dto-1.0-SNAPSHOT.jar .
COPY .env .env

RUN mvn clean

RUN mvn install:install-file -Dfile=sagaevent-dto-1.0-SNAPSHOT.jar -DgroupId=org.example -DartifactId=common -Dversion=1.0 -Dpackaging=jar

RUN mvn dependency:go-offline
RUN mvn package -DskipTests

# Run stage
FROM openjdk:21-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
#COPY src/main/resources/db/migration/*.sql /app/db/migration/
COPY src/main/resources/application.properties /app/application.properties
COPY .env /app/.env

EXPOSE 8082
#CMD ["sh", "-c", "cat /app/application.properties"]
CMD ["java", "-jar", "app.jar"]
#CMD ["sh", "-c", "cat /app/application.properties && java -jar app.jar"]