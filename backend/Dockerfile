# ---- Etapa 1: build con Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copiamos primero el pom para cachear dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Copiamos el codigo y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Etapa 2: imagen final, solo el JAR ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copiamos el JAR generado en la etapa de build
COPY --from=build /app/target/consultores-backend-0.0.1-SNAPSHOT.jar app.jar
# Render asigna el puerto por la variable PORT; exponemos 8080 por defecto
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
