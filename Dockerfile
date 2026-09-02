# 1. Fase de compilación
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Fase de ejecución
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/coildefectcatalog-1.0-SNAPSHOT-shaded.jar app.jar
EXPOSE 8085
CMD ["java", "-jar", "app.jar"]

