-- V3__Standardize_IDs_to_BIGSERIAL.sql
-- Standardize all ID columns to BIGINT and update foreign key references

-- First, drop views that depend on these tables to avoid conflicts
DROP VIEW IF EXISTS company_fiscal_periods_view CASCADE;
DROP VIEW IF EXISTS transaction_summary_view CASCADE;
DROP VIEW IF EXISTS payroll_summary_view CASCADE;

-- Convert all ID columns to BIGINT
ALTER TABLE companies ALTER COLUMN id TYPE BIGINT;
ALTER TABLE fiscal_periods ALTER COLUMN id TYPE BIGINT;
ALTER TABLE fiscal_periods ALTER COLUMN company_id TYPE BIGINT;
ALTER TABLE bank_transactions ALTER COLUMN id TYPE BIGINT;
ALTER TABLE bank_transactions ALTER COLUMN company_id TYPE BIGINT;
ALTER TABLE bank_transactions ALTER COLUMN fiscal_period_id TYPE BIGINT;
ALTER TABLE employees ALTER COLUMN id TYPE BIGINT;
ALTER TABLE employees ALTER COLUMN company_id TYPE BIGINT;
ALTER TABLE employees ALTER COLUMN fiscal_period_id TYPE BIGINT;
ALTER TABLE payroll_periods ALTER COLUMN id TYPE BIGINT;
ALTER TABLE payroll_periods ALTER COLUMN company_id TYPE BIGINT;
ALTER TABLE payroll_periods ALTER COLUMN fiscal_period_id TYPE BIGINT;
ALTER TABLE plans ALTER COLUMN id TYPE BIGINT;
ALTER TABLE users ALTER COLUMN id TYPE BIGINT;
ALTER TABLE user_companies ALTER COLUMN id TYPE BIGINT;
ALTER TABLE user_companies ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE user_companies ALTER COLUMN company_id TYPE BIGINT;

-- Update sequences to BIGINT
ALTER SEQUENCE companies_id_seq AS BIGINT;
ALTER SEQUENCE fiscal_periods_id_seq AS BIGINT;
ALTER SEQUENCE bank_transactions_id_seq AS BIGINT;
ALTER SEQUENCE employees_id_seq AS BIGINT;
ALTER SEQUENCE payroll_periods_id_seq AS BIGINT;
ALTER SEQUENCE plans_id_seq AS BIGINT;
ALTER SEQUENCE users_id_seq AS BIGINT;
ALTER SEQUENCE user_companies_id_seq AS BIGINT;

-- Recreate the views (you may need to adjust these based on your actual view definitions)
-- Note: These are placeholder recreations - you should verify the actual view definitions
-- CREATE VIEW company_fiscal_periods_view AS SELECT * FROM fiscal_periods;
-- CREATE VIEW transaction_summary_view AS SELECT * FROM bank_transactions;
-- CREATE VIEW payroll_summary_view AS SELECT * FROM payroll_periods;