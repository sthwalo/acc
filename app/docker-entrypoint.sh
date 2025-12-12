#!/bin/sh
# Docker entrypoint script for FIN application

set -e

echo "🚀 Starting FIN Application Container"
echo "====================================="

# Wait for PostgreSQL to be ready
/app/wait-for-postgres.sh

# Start the application
echo "📊 Starting FIN Spring Boot application..."
exec java ${JAVA_OPTS:--Xmx512m} -jar /app/app.jar