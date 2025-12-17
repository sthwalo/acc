-- Reconstructed V2 migration: add bank transaction columns
-- Idempotent: adds columns only if they do not exist
ALTER TABLE IF EXISTS public.bank_transactions
    ADD COLUMN IF NOT EXISTS debit_amount numeric(38,2),
    ADD COLUMN IF NOT EXISTS credit_amount numeric(38,2),
    ADD COLUMN IF NOT EXISTS balance numeric(38,2);

DO $$ BEGIN
    RAISE NOTICE 'Reconstructed migration V2 executed (idempotent ALTER TABLE).';
END$$;
