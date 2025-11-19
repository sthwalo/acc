-- Add missing reference column to bank_transactions table
ALTER TABLE bank_transactions ADD COLUMN IF NOT EXISTS reference VARCHAR(255);