# app/Dockerfile

# Stage 1: Build the application using Gradle
FROM gradle:8.10-jdk21 AS builder

# Set the working directory inside the container
WORKDIR /home/gradle/project

# Copy the Gradle wrapper and related files
COPY gradlew .
COPY gradle gradle

# Grant execute permission to the Gradle wrapper
RUN chmod +x gradlew

# Copy the rest of the project source
COPY src src
COPY build.gradle.kts settings.gradle.kts ./

# (Optional) Cache dependencies by downloading them before building
#RUN ./gradlew dependencies

# Build the application
RUN ./gradlew clean shadowJar

# Stage 2: Create the runtime image
FROM openjdk:21-jdk-slim

# Set environment variables (can be overridden at runtime)
#ENV TELEGRAM_BOT_TOKEN=YOUR_TELEGRAM_BOT_TOKEN
#ENV TELEGRAM_BOT_USERNAME=YOUR_TELEGRAM_BOT_USERNAME
#ENV MONGO_URI=mongodb://mongodb:27017

# Set the working directory inside the container
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /home/gradle/project/build/libs/buddyBot-1.0.0-all.jar app.jar

# Expose necessary ports (if your app provides an HTTP server)
# EXPOSE 8080

# Run the application
#ENTRYPOINT ["java", "-jar", "app.jar"]
