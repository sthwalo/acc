-- Reconstructed V9 migration: Add Industry Templates and Company Setup
-- Ensure industry_templates table exists
CREATE TABLE IF NOT EXISTS public.industry_templates (
    id bigint PRIMARY KEY,
    industry_id bigint NOT NULL,
    template jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Ensure company setup table exists
CREATE TABLE IF NOT EXISTS public.company_setups (
    id bigint PRIMARY KEY,
    company_id bigint NOT NULL,
    settings jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

DO $$ BEGIN
    RAISE NOTICE 'Reconstructed migration V9 (industry templates & company setup) applied.';
END$$;
