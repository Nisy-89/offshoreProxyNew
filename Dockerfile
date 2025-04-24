# Use OpenJDK 11 as base image
FROM openjdk:11-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the compiled JAR file into the container
COPY target/offshoreProxy-1.0.0-jar-with-dependencies.jar /app/offshoreProxy.jar

# Expose the port for offshore proxy communication
EXPOSE 9090

# Command to run the JAR file
CMD ["java", "-jar", "offshoreProxy.jar"]
