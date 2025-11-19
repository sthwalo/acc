#!/bin/bash
# Production database restoration script for Docker
# This script runs after the database is initialized and restores production data

set -e

echo "🔄 Starting production database restoration..."

# Export password for psql/pg_restore
export PGPASSWORD="$POSTGRES_PASSWORD"

# Wait for database to be ready by trying to connect with psql
echo "Waiting for database to accept connections..."
until psql -h localhost -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT 1;" >/dev/null 2>&1; do
  echo "Database not ready yet, waiting..."
  sleep 2
done

echo "✅ Database is ready and accepting connections!"

# Check if backup file exists
if [ -f "/tmp/production-backup.dump" ]; then
    echo "📦 Found production backup file. Restoring..."

    # The database and user are already created by docker-init.sql
    # Just restore the backup data
    pg_restore -h localhost -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-privileges --clean /tmp/production-backup.dump

    echo "✅ Production database restored successfully!"
    echo "📊 Database contains:"
    USER_COUNT=$(psql -h localhost -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT COUNT(*) FROM users;" -t 2>/dev/null || echo "0")
    COMPANY_COUNT=$(psql -h localhost -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT COUNT(*) FROM companies;" -t 2>/dev/null || echo "0")
    echo "  Users: $USER_COUNT"
    echo "  Companies: $COMPANY_COUNT"

else
    echo "⚠️  No production backup file found at /tmp/production-backup.dump. Using minimal schema."
fi

echo "🎉 Database initialization complete!"