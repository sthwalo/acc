-- Reconstructed V7 migration: add payroll_documents table
CREATE TABLE IF NOT EXISTS public.payroll_documents (
    id bigint PRIMARY KEY,
    company_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    document_type character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

DO $$ BEGIN
    RAISE NOTICE 'Reconstructed migration V7 (payroll_documents) applied (idempotent CREATE TABLE).';
END$$;
