-- FIN Financial Management System - Data Migration
-- Migration: Update created_by and updated_by from 'SYSTEM' to 'FIN'
-- Date: November 2025
-- Description: Updates all existing audit fields to use 'FIN' instead of 'SYSTEM'

-- Update users table
UPDATE users
SET created_by = 'FIN', updated_by = 'FIN'
WHERE created_by = 'SYSTEM' OR updated_by = 'SYSTEM';

-- Update plans table
UPDATE plans
SET created_by = 'FIN', updated_by = 'FIN'
WHERE created_by = 'SYSTEM' OR updated_by = 'SYSTEM';

-- Update user_companies table
UPDATE user_companies
SET created_by = 'FIN', updated_by = 'FIN'
WHERE created_by = 'SYSTEM' OR updated_by = 'SYSTEM';

-- Update companies table (if it exists and has these fields)
-- Note: This assumes companies table has created_by and updated_by fields
-- Uncomment the following if companies table has these audit fields:
-- UPDATE companies
-- SET created_by = 'FIN', updated_by = 'FIN'
-- WHERE created_by = 'SYSTEM' OR updated_by = 'SYSTEM';

-- Migration completed successfully