-- Reconstructed V6 migration: Add configuration_options table
CREATE TABLE IF NOT EXISTS public.configuration_options (
    id bigint PRIMARY KEY,
    name character varying(255) NOT NULL UNIQUE,
    value text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

DO $$ BEGIN
    RAISE NOTICE 'Reconstructed migration V6 (configuration_options) applied (idempotent CREATE TABLE).';
END$$;
