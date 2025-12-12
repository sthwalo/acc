# Multi-stage Dockerfile for FIN Spring Boot Application
# Optimized for development: uses pre-built JAR instead of building inside container

# Runtime image only (no build stage)
FROM eclipse-temurin:17-jre

# Install PostgreSQL client, libharu for PDF generation, and Tesseract OCR
RUN apt-get update && \
    apt-get install -y --no-install-recommends postgresql-client wget unzip libhpdf-dev fonts-dejavu-core fonts-liberation tesseract-ocr tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

# Copy pre-built JAR from local build
COPY app/build/libs/fin-spring.jar /app/app.jar

# Copy scripts
COPY app/wait-for-postgres.sh /app/wait-for-postgres.sh
COPY app/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/wait-for-postgres.sh /app/docker-entrypoint.sh

# Set working directory
WORKDIR /app

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health || exit 1

# Entrypoint script handles database wait and application start
ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["api"]