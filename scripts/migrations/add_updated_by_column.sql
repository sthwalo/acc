-- Add missing updated_by column to bank_transactions table
ALTER TABLE bank_transactions ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);