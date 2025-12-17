-- Reconstructed V13 migration: Fix Chart Of Accounts Templates Account Type
-- Ensure account template account_type values conform to expected set
UPDATE public.account_templates
SET account_type = 'ASSET'
WHERE account_type IS NULL;

DO $$ BEGIN
    RAISE NOTICE 'Reconstructed migration V13 applied (simple cleanup update). Please review.';
END$$;
