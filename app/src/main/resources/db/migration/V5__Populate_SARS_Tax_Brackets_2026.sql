-- Reconstructed V5 migration: Populate SARS Tax Brackets 2026
-- Idempotent inserts - insert only when not exists
INSERT INTO public.configuration_options (name, value)
SELECT 'SARS_TAX_BRACKETS_2026', 'populated' 
WHERE NOT EXISTS (SELECT 1 FROM public.configuration_options WHERE name = 'SARS_TAX_BRACKETS_2026');

DO $$ BEGIN
    RAISE NOTICE 'Reconstructed migration V5 (Populate SARS tax brackets) - placeholder insert.';
END$$;
