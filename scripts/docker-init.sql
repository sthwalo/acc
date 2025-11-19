-- PostgreSQL Docker initialization script for FIN application
-- This script sets up the database schema and initial data

-- Create the database if it doesn't exist
SELECT 'CREATE DATABASE drimacc_db WITH OWNER = ''' || 'sthwalonyoni' || ''''
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'drimacc_db')\gexec

-- Connect to the database
\c drimacc_db

-- Set up permissions for the user
GRANT ALL PRIVILEGES ON DATABASE drimacc_db TO "sthwalonyoni";
GRANT ALL ON SCHEMA public TO "sthwalonyoni";
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "sthwalonyoni";
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO "sthwalonyoni";

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO "sthwalonyoni";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO "sthwalonyoni";

-- Check if we have a production backup to restore
-- This allows the Docker container to start with production data
DO $$
BEGIN
    -- Only restore if the database is empty (no users table exists)
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'users') THEN
        -- Look for the most recent backup file
        -- Note: In Docker, backup files would need to be mounted as volumes
        RAISE NOTICE 'Database appears empty. For production deployment, mount a backup file at /docker-entrypoint-initdb.d/ and restore it manually, or modify this script to include production data.';
    END IF;
END $$;

-- Create minimal schema structure for development/testing
-- The full schema will be restored from backup if needed, but for Docker we start minimal

-- Create companies table if it doesn't exist (minimal structure for startup)
CREATE TABLE IF NOT EXISTS public.companies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(50),
    tax_number VARCHAR(50),
    address TEXT,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    logo_path VARCHAR(500),
    bank_name VARCHAR(255),
    account_number VARCHAR(50),
    account_type VARCHAR(50),
    branch_code VARCHAR(20),
    vat_registered BOOLEAN DEFAULT FALSE
);

-- Create company_classification_rules table if it doesn't exist
CREATE TABLE IF NOT EXISTS public.company_classification_rules (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    pattern VARCHAR(1000) NOT NULL,
    keywords VARCHAR(2000) NOT NULL,
    account_code VARCHAR(20) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    usage_count INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create fiscal_periods table if it doesn't exist
CREATE TABLE IF NOT EXISTS public.fiscal_periods (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL,
    period_name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_closed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create manual_invoices table if it doesn't exist
CREATE TABLE IF NOT EXISTS public.manual_invoices (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL,
    invoice_number VARCHAR(100) NOT NULL,
    invoice_date DATE NOT NULL,
    description TEXT,
    amount NUMERIC(15,2) NOT NULL,
    debit_account_id INTEGER NOT NULL,
    credit_account_id INTEGER NOT NULL,
    fiscal_period_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Set ownership
ALTER TABLE public.companies OWNER TO "sthwalonyoni";
ALTER TABLE public.company_classification_rules OWNER TO "sthwalonyoni";
ALTER TABLE public.fiscal_periods OWNER TO "sthwalonyoni";
ALTER TABLE public.manual_invoices OWNER TO "sthwalonyoni";

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_companies_name ON public.companies(name);
CREATE INDEX IF NOT EXISTS idx_company_classification_rules_company_id ON public.company_classification_rules(company_id);
CREATE INDEX IF NOT EXISTS idx_fiscal_periods_company_id ON public.fiscal_periods(company_id);
CREATE INDEX IF NOT EXISTS idx_manual_invoices_company_id ON public.manual_invoices(company_id);
CREATE INDEX IF NOT EXISTS idx_manual_invoices_invoice_number ON public.manual_invoices(invoice_number);

-- Insert a default company if none exists (for testing)
INSERT INTO public.companies (name, registration_number, tax_number, contact_email, vat_registered)
SELECT 'Default Company', 'DEFAULT001', '1234567890', 'admin@default.com', false
WHERE NOT EXISTS (SELECT 1 FROM public.companies LIMIT 1);

-- Insert a default fiscal period if none exists
INSERT INTO public.fiscal_periods (company_id, period_name, start_date, end_date)
SELECT 1, 'FY2024-2025', '2024-03-01', '2025-02-28'
WHERE NOT EXISTS (SELECT 1 FROM public.fiscal_periods LIMIT 1);

-- Insert some basic classification rules if none exist
INSERT INTO public.company_classification_rules (company_id, pattern, keywords, account_code, account_name)
SELECT 1, 'salary|wage|payroll', 'salary,wage,payroll,employee', '5000', 'Salaries and Wages'
WHERE NOT EXISTS (SELECT 1 FROM public.company_classification_rules LIMIT 1);

INSERT INTO public.company_classification_rules (company_id, pattern, keywords, account_code, account_name)
SELECT 1, 'rent|lease', 'rent,lease,office,premises', '6000', 'Rent Expense'
WHERE NOT EXISTS (SELECT 1 FROM public.company_classification_rules WHERE pattern LIKE '%rent%');

INSERT INTO public.company_classification_rules (company_id, pattern, keywords, account_code, account_name)
SELECT 1, 'bank|interest|fee', 'bank,interest,fee,charge', '7000', 'Bank Charges'
WHERE NOT EXISTS (SELECT 1 FROM public.company_classification_rules WHERE pattern LIKE '%bank%');

-- Grant permissions on all tables to the application user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "sthwalonyoni";
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO "sthwalonyoni";

-- Set up update trigger function
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create update triggers for tables that have updated_at columns
DROP TRIGGER IF EXISTS update_companies_updated_at ON public.companies;
CREATE TRIGGER update_companies_updated_at
    BEFORE UPDATE ON public.companies
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

DROP TRIGGER IF EXISTS update_manual_invoices_updated_at ON public.manual_invoices;
CREATE TRIGGER update_manual_invoices_updated_at
    BEFORE UPDATE ON public.manual_invoices
    FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();