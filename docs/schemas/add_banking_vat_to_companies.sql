-- Migration: Add banking details and VAT registration to companies table
-- Date: 2025-11-02
-- Description: Adds bank account information and VAT registration flag to companies

-- Add banking details columns
ALTER TABLE companies ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS account_number VARCHAR(50);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS account_type VARCHAR(50);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS branch_code VARCHAR(20);

-- Add VAT registration flag
ALTER TABLE companies ADD COLUMN IF NOT EXISTS vat_registered BOOLEAN DEFAULT FALSE;

-- Update existing companies to have default VAT registration as false
UPDATE companies SET vat_registered = FALSE WHERE vat_registered IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN companies.bank_name IS 'Name of the bank where company account is held';
COMMENT ON COLUMN companies.account_number IS 'Company bank account number';
COMMENT ON COLUMN companies.account_type IS 'Type of account (e.g., Business Cheque, Savings)';
COMMENT ON COLUMN companies.branch_code IS 'Bank branch code';
COMMENT ON COLUMN companies.vat_registered IS 'Whether company is registered for VAT';
