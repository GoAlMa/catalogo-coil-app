# 1. Fase de compilación con Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Fase de ejecución
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copiamos todos los .jar generados en target al contenedor
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]