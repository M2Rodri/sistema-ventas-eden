# Imagen de build: compila el jar con Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Imagen final: solo el jar y un JRE, mucho mas liviana
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/ecommerce-backend-1.0.0.jar app.jar

# Render asigna el puerto real por la variable de entorno PORT.
ENTRYPOINT ["java", "-jar", "app.jar"]
