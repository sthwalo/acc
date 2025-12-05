#!/bin/bash

# FIN Database Migration: Add transaction_type_id column to journal_entries table
# This migration fixes the schema mismatch causing 500 errors in data management reset

set -e

echo "🔄 Starting database migration: Add transaction_type_id to journal_entries"

# Load environment variables
if [ -f .env ]; then
    source .env
else
    echo "❌ Error: .env file not found"
    exit 1
fi

# Database connection details
DB_HOST=${DATABASE_HOST:-localhost}
DB_PORT=${DATABASE_PORT:-5432}
# Extract database name from DATABASE_URL
DB_NAME=$(echo "$DATABASE_URL" | sed -n 's|.*://.*/\([^?]*\).*|\1|p')
DB_USER=${DATABASE_USER}
DB_PASSWORD=${DATABASE_PASSWORD}

# Check if column already exists
echo "🔍 Checking if transaction_type_id column exists..."
COLUMN_EXISTS=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "
SELECT 1 FROM information_schema.columns
WHERE table_name = 'journal_entries' AND column_name = 'transaction_type_id';
" 2>/dev/null || echo "0")

if [ "$COLUMN_EXISTS" = "1" ]; then
    echo "✅ Column transaction_type_id already exists in journal_entries table"
    exit 0
fi

echo "📝 Adding transaction_type_id column to journal_entries table..."

# Add the column
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" << 'EOF'
ALTER TABLE journal_entries
ADD COLUMN IF NOT EXISTS transaction_type_id BIGINT;

-- Add comment for documentation
COMMENT ON COLUMN journal_entries.transaction_type_id IS 'Reference to transaction type for classification';

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_journal_entries_transaction_type_id
ON journal_entries(transaction_type_id);

EOF

if [ $? -eq 0 ]; then
    echo "✅ Migration completed successfully!"
    echo "📋 Summary:"
    echo "   - Added transaction_type_id column to journal_entries table"
    echo "   - Added index for performance"
    echo "   - Column is nullable (existing records will have NULL values)"
else
    echo "❌ Migration failed!"
    exit 1
fi

echo "🎉 Database migration complete. The data management reset endpoint should now work."