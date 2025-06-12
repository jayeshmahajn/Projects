# Dockerfile
FROM openjdk:19-jdk-slim

# Set maintainer label
LABEL maintainer="jayeshmahajan340@gmail.com"
LABEL description="Document to PDF Converter Service"



# Install LibreOffice with recommended packages
RUN apt-get update && \
    apt-get install -y \
    libreoffice \
    libreoffice-writer \
    libreoffice-calc \
    libreoffice-draw \
    libreoffice-impress \
    fonts-liberation \
    fonts-dejavu \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Create app directory and set ownership
WORKDIR /app
RUN chown -R appuser:appuser /app

# Create temp directory for file processing
RUN mkdir -p /tmp/libreoffice && chown -R appuser:appuser /tmp/libreoffice

# Copy the built jar file
COPY target/Doc2Pdf-0.0.1-SNAPSHOT.jar app.jar

# Change ownership of the jar file
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose the port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# Set JVM options for container environment
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]