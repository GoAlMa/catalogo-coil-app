# 1. Fase de compilación con Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Fase de ejecución
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copiamos ÚNICAMENTE el JAR generado que NO empieza por "original-"
COPY --from=build /app/target/coildefectcatalog-1.0-SNAPSHOT-shaded.jar /app/app.jarEXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]

