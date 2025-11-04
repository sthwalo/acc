#!/bin/sh
# Wait for PostgreSQL to be ready
# This script runs in Chainguard distroless environment

set -e

echo "Waiting for PostgreSQL to be ready..."

# Wait for PostgreSQL with timeout
timeout=60
elapsed=0

while [ $elapsed -lt $timeout ]; do
    if pg_isready -h postgres -p 5432 -U "${DATABASE_USER:-postgres}" -q 2>/dev/null; then
        echo "PostgreSQL is ready!"
        exit 0
    fi

    echo "PostgreSQL not ready, waiting... ($elapsed/$timeout seconds)"
    sleep 2
    elapsed=$((elapsed + 2))
done

echo "Timeout waiting for PostgreSQL"
exit 1