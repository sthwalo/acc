#!/bin/sh
# Wait for PostgreSQL to be ready
# This script runs in Debian/Ubuntu-based containers

set -e

# Parse DATABASE_URL to extract host and port
# Expected format: jdbc:postgresql://host:port/database
if [ -n "$DATABASE_URL" ]; then
    # Extract host and port from JDBC URL
    DB_HOST_PORT=$(echo "$DATABASE_URL" | sed -n 's|jdbc:postgresql://\([^/]*\)/.*|\1|p')
    DB_HOST=$(echo "$DB_HOST_PORT" | cut -d: -f1)
    DB_PORT=$(echo "$DB_HOST_PORT" | cut -d: -f2)
    DB_NAME=$(echo "$DATABASE_URL" | sed -n 's|jdbc:postgresql://[^/]*/\([^?]*\).*|\1|p')
else
    # Fallback to defaults
    DB_HOST="postgres"
    DB_PORT="5432"
    DB_NAME="drimacc_db"
fi

echo "Waiting for PostgreSQL to be ready..."
echo "Database host: $DB_HOST"
echo "Database port: $DB_PORT"
echo "Database user: ${DATABASE_USER:-postgres}"

# Wait for PostgreSQL with timeout
timeout=120
elapsed=0

while [ $elapsed -lt $timeout ]; do
    if pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "${DATABASE_USER:-postgres}" -d "$DB_NAME" -q 2>/dev/null; then
        echo "PostgreSQL is ready!"
        # Give it a moment for any remaining initialization
        sleep 2
        echo "Database connection test successful!"
        exit 0
    fi

    echo "PostgreSQL not ready, waiting... ($elapsed/$timeout seconds)"
    sleep 2
    elapsed=$((elapsed + 2))
done

echo "Timeout waiting for PostgreSQL after $timeout seconds"
exit 1