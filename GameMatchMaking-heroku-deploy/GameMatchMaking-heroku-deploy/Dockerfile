# Use Eclipse Temurin (replacement for OpenJDK)
FROM maven:3.8.4-eclipse-temurin-11 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Second stage - runtime
FROM eclipse-temurin:11-jre

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/steam-module-1.0-SNAPSHOT.jar app.jar

# Create exports directory
RUN mkdir -p /app/exports

EXPOSE 8080

# Use environment variables
ENTRYPOINT ["java", "-jar", "app.jar"]