-- V1 (reconstructed baseline): core tables, sequences and helper functions
-- This migration is idempotent and aims to reconstruct the original baseline schema
-- derived from `backups/drimacc_schema_2025-12-17_151310.sql` for review.
-- DO NOT run repair until you have reviewed & approved these changes.

/* === Helper function: update_updated_at_column === */
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

/* === companies === */
CREATE SEQUENCE IF NOT EXISTS public.companies_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS public.companies (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    registration_number character varying(255),
    tax_number character varying(255),
    address character varying(255),
    contact_email character varying(255),
    contact_phone character varying(255),
    bank_name character varying(255),
    account_number character varying(255),
    account_type character varying(255),
    branch_code character varying(255),
    vat_registered boolean DEFAULT false,
    logo_path character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(255),
    updated_by character varying(255),
    industry_id bigint
);

ALTER SEQUENCE IF EXISTS public.companies_id_seq OWNED BY public.companies.id;
ALTER TABLE IF EXISTS public.companies ALTER COLUMN id SET DEFAULT nextval('public.companies_id_seq'::regclass);

/* === industries === */
CREATE SEQUENCE IF NOT EXISTS public.industries_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS public.industries (
    id bigint NOT NULL,
    division_code character varying(10) NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    category character varying(100),
    is_active boolean DEFAULT true NOT NULL,
    is_sars_compliant boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(255),
    updated_by character varying(255)
);

ALTER SEQUENCE IF EXISTS public.industries_id_seq OWNED BY public.industries.id;
ALTER TABLE IF EXISTS public.industries ALTER COLUMN id SET DEFAULT nextval('public.industries_id_seq'::regclass);

/* === chart_of_accounts_templates === */
CREATE SEQUENCE IF NOT EXISTS public.chart_of_accounts_templates_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS public.chart_of_accounts_templates (
    id bigint NOT NULL,
    industry_id bigint NOT NULL,
    account_code character varying(50) NOT NULL,
    account_name character varying(255) NOT NULL,
    parent_template_id bigint,
    level integer NOT NULL,
    is_required boolean DEFAULT false NOT NULL,
    default_balance numeric(15,2) DEFAULT 0,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(255),
    updated_by character varying(255),
    account_type character varying(50) NOT NULL,
    CONSTRAINT chart_of_accounts_templates_level_check CHECK ((level >= 1) AND (level <= 4))
);

ALTER SEQUENCE IF EXISTS public.chart_of_accounts_templates_id_seq OWNED BY public.chart_of_accounts_templates.id;
ALTER TABLE IF EXISTS public.chart_of_accounts_templates ALTER COLUMN id SET DEFAULT nextval('public.chart_of_accounts_templates_id_seq'::regclass);

-- Create helpful indexes if missing (non-destructive)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relname = 'idx_coa_templates_industry_id') THEN
        CREATE INDEX idx_coa_templates_industry_id ON public.chart_of_accounts_templates USING btree (industry_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relname = 'idx_coa_templates_account_type') THEN
        CREATE INDEX idx_coa_templates_account_type ON public.chart_of_accounts_templates USING btree (account_type);
    END IF;
END$$;

-- Create trigger to maintain updated_at
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger t JOIN pg_class c ON t.tgrelid = c.oid WHERE t.tgname = 'update_coa_templates_updated_at') THEN
        CREATE TRIGGER update_coa_templates_updated_at BEFORE UPDATE ON public.chart_of_accounts_templates FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
    END IF;
END$$;

/* === Basic verification at end (raises if core tables missing) === */
DO $$
DECLARE
    missing text := '';
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'companies') THEN
        missing := missing || 'companies ';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'industries') THEN
        missing := missing || 'industries ';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'chart_of_accounts_templates') THEN
        missing := missing || 'chart_of_accounts_templates ';
    END IF;
    IF missing <> '' THEN
        RAISE NOTICE 'V1 baseline checks: missing objects: %', missing;
    ELSE
        RAISE NOTICE 'V1 baseline checks: all core objects present';
    END IF;
END$$;
