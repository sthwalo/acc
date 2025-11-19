-- FIN Financial Management System - Plan Migration
-- Migration: Fix plan names and pricing to match LicenseManager structure
-- Date: November 2025
-- Description: Updates plans to match the correct licensing structure from LicenseManager.java

-- Update the incorrectly named "Premium" plan to "Enterprise" with correct pricing
UPDATE plans
SET name = 'Enterprise',
    description = 'Complete solution for large enterprises',
    price = 299.00,
    max_companies = -1,  -- unlimited
    max_users = -1,      -- unlimited
    max_transactions_per_month = -1,  -- unlimited
    has_priority_support = true,
    updated_by = 'FIN',
    updated_at = NOW()
WHERE name = 'Premium';

-- Insert missing plans (Starter and Professional) if they don't exist
INSERT INTO plans (
    name, description, price, currency, billing_cycle, is_active,
    can_access_dashboard, can_manage_companies, can_process_bank_statements,
    can_generate_reports, can_manage_payroll, can_manage_budgets,
    can_access_multiple_companies, max_companies, max_users,
    max_transactions_per_month, has_api_access, has_priority_support,
    created_by, updated_by
) VALUES
-- Starter Plan: 3 companies, 1,000 transactions, $29/month
('Starter', 'Perfect for small businesses getting started', 29.00, 'ZAR', 'MONTHLY', true,
 true, true, true, true, false, false, true, 3, 5, 1000, false, false, 'FIN', 'FIN'),
-- Professional Plan: 10 companies, 10,000 transactions, $99/month
('Professional', 'Advanced features for growing businesses', 99.00, 'ZAR', 'MONTHLY', true,
 true, true, true, true, true, true, true, 10, 15, 10000, true, false, 'FIN', 'FIN')
ON CONFLICT (name) DO NOTHING;

-- Migration completed successfully