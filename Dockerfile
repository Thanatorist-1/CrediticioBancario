FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 spring
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 10000
USER spring
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
