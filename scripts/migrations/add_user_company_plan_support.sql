-- FIN Financial Management System - Database Migration
-- Migration: Add pricing plans and user-company relationships
-- Date: November 2025
-- Description: Implements proper user authentication with company access control and pricing plans

-- Create plans table for pricing plans and feature access
CREATE TABLE IF NOT EXISTS plans (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Feature flags
    can_access_dashboard BOOLEAN NOT NULL DEFAULT true,
    can_manage_companies BOOLEAN NOT NULL DEFAULT false,
    can_process_bank_statements BOOLEAN NOT NULL DEFAULT true,
    can_generate_reports BOOLEAN NOT NULL DEFAULT true,
    can_manage_payroll BOOLEAN NOT NULL DEFAULT false,
    can_manage_budgets BOOLEAN NOT NULL DEFAULT false,
    can_access_multiple_companies BOOLEAN NOT NULL DEFAULT false,
    max_companies INTEGER NOT NULL DEFAULT 1,
    max_users INTEGER NOT NULL DEFAULT 1,
    max_transactions_per_month INTEGER NOT NULL DEFAULT 1000,
    has_api_access BOOLEAN NOT NULL DEFAULT false,
    has_priority_support BOOLEAN NOT NULL DEFAULT false,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL DEFAULT 'FIN',
    updated_by VARCHAR(255) NOT NULL DEFAULT 'FIN'
);

-- Create index on plan name for fast lookups
CREATE INDEX IF NOT EXISTS idx_plans_name ON plans(name);
CREATE INDEX IF NOT EXISTS idx_plans_active ON plans(is_active);

-- Create user_companies table for many-to-many user-company relationships
CREATE TABLE IF NOT EXISTS user_companies (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'USER' CHECK (role IN ('ADMIN', 'MANAGER', 'USER', 'VIEWER')),
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NOT NULL DEFAULT 'FIN',
    updated_by VARCHAR(255) NOT NULL DEFAULT 'FIN',

    -- Ensure unique active relationship per user-company pair
    UNIQUE(user_id, company_id)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_user_companies_user_id ON user_companies(user_id);
CREATE INDEX IF NOT EXISTS idx_user_companies_company_id ON user_companies(company_id);
CREATE INDEX IF NOT EXISTS idx_user_companies_active ON user_companies(is_active);
CREATE INDEX IF NOT EXISTS idx_user_companies_user_company ON user_companies(user_id, company_id, is_active);

-- Add plan_id column to users table (replacing company_id)
ALTER TABLE users ADD COLUMN IF NOT EXISTS plan_id BIGINT REFERENCES plans(id);

-- Create index on plan_id
CREATE INDEX IF NOT EXISTS idx_users_plan_id ON users(plan_id);

-- Insert default plans with correct licensing structure
INSERT INTO plans (
    name, description, price, currency, billing_cycle, is_active,
    can_access_dashboard, can_manage_companies, can_process_bank_statements,
    can_generate_reports, can_manage_payroll, can_manage_budgets,
    can_access_multiple_companies, max_companies, max_users,
    max_transactions_per_month, has_api_access, has_priority_support,
    created_by, updated_by
) VALUES
-- Free Plan: 1 company, 100 transactions, $0/month
('Trial', '1-month trial with basic financial management features', 0.00, 'ZAR', 'MONTHLY', true,
 true, false, true, true, false, false, false, 1, 1, 100, false, false, 'FIN', 'FIN'),
-- Starter Plan: 3 companies, 1,000 transactions, $29/month
('Starter', 'Perfect for small businesses getting started', 29.00, 'ZAR', 'MONTHLY', true,
 true, true, true, true, false, false, true, 3, 5, 1000, false, false, 'FIN', 'FIN'),
-- Professional Plan: 10 companies, 10,000 transactions, $99/month
('Professional', 'Advanced features for growing businesses', 99.00, 'ZAR', 'MONTHLY', true,
 true, true, true, true, true, true, true, 10, 15, 10000, true, false, 'FIN', 'FIN'),
-- Enterprise Plan: unlimited usage, $299/month
('Enterprise', 'Complete solution for large enterprises', 299.00, 'ZAR', 'MONTHLY', true,
 true, true, true, true, true, true, true, -1, -1, -1, true, true, 'FIN', 'FIN')
ON CONFLICT (name) DO NOTHING;

-- Migrate existing users to use plans
-- For existing users, assign them to the Free plan and create user-company relationships
DO $$
DECLARE
    free_plan_id BIGINT;
    user_record RECORD;
BEGIN
    -- Get the Free plan ID
    SELECT id INTO free_plan_id FROM plans WHERE name = 'Trial' LIMIT 1;

    -- Update existing users to use plan_id instead of company_id
    FOR user_record IN SELECT id, company_id FROM users WHERE company_id IS NOT NULL AND plan_id IS NULL
    LOOP
        -- Set plan_id to Free plan
        UPDATE users SET plan_id = free_plan_id WHERE id = user_record.id;

        -- Create user-company relationship
        INSERT INTO user_companies (user_id, company_id, role, created_by, updated_by)
        VALUES (user_record.id, user_record.company_id, 'ADMIN', 'FIN', 'FIN')
        ON CONFLICT (user_id, company_id) DO NOTHING;
    END LOOP;

    -- For users without company_id, assign Free plan
    UPDATE users SET plan_id = free_plan_id WHERE plan_id IS NULL;
END $$;

-- Remove the old company_id column from users table (after migration)
-- ALTER TABLE users DROP COLUMN IF EXISTS company_id;

-- Add comments for documentation
COMMENT ON TABLE plans IS 'Pricing plans and feature access control for users';
COMMENT ON TABLE user_companies IS 'Many-to-many relationships between users and companies with role-based access';
COMMENT ON COLUMN users.plan_id IS 'Reference to the user''s pricing plan';

-- Create a view for user permissions (optional, for easier querying)
CREATE OR REPLACE VIEW user_permissions AS
SELECT
    u.id as user_id,
    u.email,
    u.first_name,
    u.last_name,
    p.name as plan_name,
    p.price as plan_price,
    p.can_access_dashboard,
    p.can_manage_companies,
    p.can_process_bank_statements,
    p.can_generate_reports,
    p.can_manage_payroll,
    p.can_manage_budgets,
    p.can_access_multiple_companies,
    p.max_companies,
    p.max_users,
    p.max_transactions_per_month,
    p.has_api_access,
    p.has_priority_support,
    uc.company_id,
    uc.role as company_role,
    c.name as company_name
FROM users u
JOIN plans p ON u.plan_id = p.id
LEFT JOIN user_companies uc ON u.id = uc.user_id AND uc.is_active = true
LEFT JOIN companies c ON uc.company_id = c.id
WHERE u.is_active = true;

-- Grant permissions (adjust as needed for your setup)
-- GRANT SELECT ON plans TO fin_user;
-- GRANT SELECT, INSERT, UPDATE ON user_companies TO fin_user;
-- GRANT SELECT, UPDATE ON users TO fin_user;

-- Migration completed successfully
-- Next steps:
-- 1. Run this migration script against your database
-- 2. Update your application code to use the new UserService methods
-- 3. Test user registration, login, and company access validation
-- 4. Update frontend to handle company selection for multi-company users