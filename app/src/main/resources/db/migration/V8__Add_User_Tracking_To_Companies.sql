-- Reconstructed V8 migration: Add User Tracking to Companies (columns)
ALTER TABLE IF EXISTS public.companies
    ADD COLUMN IF NOT EXISTS created_by bigint,
    ADD COLUMN IF NOT EXISTS updated_by bigint;

DO $$ BEGIN
    RAISE NOTICE 'Reconstructed migration V8 (user tracking on companies) applied (idempotent ALTER TABLE).';
END$$;
