-- Reconstructed V4 migration: Add user_companies table
-- Creates table if not exists (idempotent)
CREATE TABLE IF NOT EXISTS public.user_companies (
    id bigint PRIMARY KEY,
    user_id bigint NOT NULL,
    company_id bigint NOT NULL,
    role character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

DO $$ BEGIN
    RAISE NOTICE 'Reconstructed migration V4 (user_companies) applied (idempotent CREATE TABLE).';
END$$;
