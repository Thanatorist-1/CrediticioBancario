# Etapa 1: compilar el JAR
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: imagen final ligera solo con el JAR
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/CrediticioBancario-1.19.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
