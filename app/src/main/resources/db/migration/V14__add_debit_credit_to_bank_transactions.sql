-- Reconstructed V14 migration: add debit credit to bank transactions
-- Idempotent: ensure debit_account_id and credit_account_id columns exist
ALTER TABLE IF EXISTS public.bank_transactions
    ADD COLUMN IF NOT EXISTS debit_account_id bigint,
    ADD COLUMN IF NOT EXISTS credit_account_id bigint;

DO $$ BEGIN
    RAISE NOTICE 'Reconstructed migration V14 (add debit/credit account ids) applied (idempotent ALTER TABLE).';
END$$;
