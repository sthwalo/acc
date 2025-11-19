-- Standardize all table IDs to BIGINT for consistency across Spark and Spring applications
-- This migration converts remaining INTEGER ID columns to BIGINT

-- ===========================================
-- DROP EXISTING SEQUENCES (if any)
-- ===========================================

DROP SEQUENCE IF EXISTS public.account_categories_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.account_types_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.accounts_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.bank_accounts_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.bank_transactions_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.budget_categories_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.budget_items_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.budget_monthly_allocations_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.budget_projections_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.budgets_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.companies_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.data_corrections_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.fiscal_periods_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.journal_entries_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.journal_entry_lines_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.manual_invoices_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.operational_activities_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.strategic_initiatives_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.strategic_milestones_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.strategic_plans_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.strategic_priorities_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.transaction_mapping_rules_id_seq CASCADE;
DROP SEQUENCE IF EXISTS public.transaction_types_id_seq CASCADE;

-- ===========================================
-- ALTER ID COLUMNS TO BIGINT
-- ===========================================

-- Convert ID columns from INTEGER to BIGINT
ALTER TABLE public.account_categories ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.account_types ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.accounts ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.bank_accounts ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.bank_transactions ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.budget_categories ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.budget_items ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.budget_monthly_allocations ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.budget_projections ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.budgets ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.companies ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.data_corrections ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.fiscal_periods ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.journal_entries ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.journal_entry_lines ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.manual_invoices ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.operational_activities ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.strategic_initiatives ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.strategic_milestones ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.strategic_plans ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.strategic_priorities ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.transaction_mapping_rules ALTER COLUMN id TYPE BIGINT;
ALTER TABLE public.transaction_types ALTER COLUMN id TYPE BIGINT;

-- ===========================================
-- UPDATE FOREIGN KEY REFERENCES
-- ===========================================

-- Update foreign key columns that reference the converted tables
ALTER TABLE public.account_categories ALTER COLUMN category_id TYPE BIGINT;
ALTER TABLE public.accounts ALTER COLUMN category_id TYPE BIGINT;
ALTER TABLE public.accounts ALTER COLUMN parent_account_id TYPE BIGINT;
ALTER TABLE public.bank_transactions ALTER COLUMN bank_account_id TYPE BIGINT;
ALTER TABLE public.budget_categories ALTER COLUMN budget_id TYPE BIGINT;
ALTER TABLE public.budget_items ALTER COLUMN budget_category_id TYPE BIGINT;
ALTER TABLE public.budget_monthly_allocations ALTER COLUMN budget_item_id TYPE BIGINT;
ALTER TABLE public.budget_projections ALTER COLUMN budget_id TYPE BIGINT;
ALTER TABLE public.budgets ALTER COLUMN fiscal_period_id TYPE BIGINT;
ALTER TABLE public.data_corrections ALTER COLUMN transaction_id TYPE BIGINT;
ALTER TABLE public.data_corrections ALTER COLUMN original_account_id TYPE BIGINT;
ALTER TABLE public.data_corrections ALTER COLUMN new_account_id TYPE BIGINT;
ALTER TABLE public.journal_entry_lines ALTER COLUMN journal_entry_id TYPE BIGINT;
ALTER TABLE public.journal_entry_lines ALTER COLUMN account_id TYPE BIGINT;
ALTER TABLE public.journal_entry_lines ALTER COLUMN source_transaction_id TYPE BIGINT;
ALTER TABLE public.manual_invoices ALTER COLUMN debit_account_id TYPE BIGINT;
ALTER TABLE public.manual_invoices ALTER COLUMN credit_account_id TYPE BIGINT;
ALTER TABLE public.operational_activities ALTER COLUMN strategic_plan_id TYPE BIGINT;
ALTER TABLE public.payroll_periods ALTER COLUMN fiscal_period_id TYPE BIGINT;
ALTER TABLE public.strategic_milestones ALTER COLUMN strategic_plan_id TYPE BIGINT;
ALTER TABLE public.strategic_milestones ALTER COLUMN strategic_initiative_id TYPE BIGINT;
ALTER TABLE public.strategic_priorities ALTER COLUMN strategic_plan_id TYPE BIGINT;
ALTER TABLE public.transaction_mapping_rules ALTER COLUMN transaction_type_id TYPE BIGINT;

-- ===========================================
-- RECREATE SEQUENCES WITH BIGINT
-- ===========================================

CREATE SEQUENCE public.account_categories_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.account_categories.id;
CREATE SEQUENCE public.account_types_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.account_types.id;
CREATE SEQUENCE public.accounts_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.accounts.id;
CREATE SEQUENCE public.bank_accounts_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.bank_accounts.id;
CREATE SEQUENCE public.bank_transactions_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.bank_transactions.id;
CREATE SEQUENCE public.budget_categories_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.budget_categories.id;
CREATE SEQUENCE public.budget_items_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.budget_items.id;
CREATE SEQUENCE public.budget_monthly_allocations_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.budget_monthly_allocations.id;
CREATE SEQUENCE public.budget_projections_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.budget_projections.id;
CREATE SEQUENCE public.budgets_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.budgets.id;
CREATE SEQUENCE public.companies_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.companies.id;
CREATE SEQUENCE public.data_corrections_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.data_corrections.id;
CREATE SEQUENCE public.fiscal_periods_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.fiscal_periods.id;
CREATE SEQUENCE public.journal_entries_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.journal_entries.id;
CREATE SEQUENCE public.journal_entry_lines_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.journal_entry_lines.id;
CREATE SEQUENCE public.manual_invoices_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.manual_invoices.id;
CREATE SEQUENCE public.operational_activities_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.operational_activities.id;
CREATE SEQUENCE public.strategic_initiatives_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.strategic_initiatives.id;
CREATE SEQUENCE public.strategic_milestones_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.strategic_milestones.id;
CREATE SEQUENCE public.strategic_plans_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.strategic_plans.id;
CREATE SEQUENCE public.strategic_priorities_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.strategic_priorities.id;
CREATE SEQUENCE public.transaction_mapping_rules_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.transaction_mapping_rules.id;
CREATE SEQUENCE public.transaction_types_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1 OWNED BY public.transaction_types.id;

-- ===========================================
-- SET DEFAULT VALUES FOR ID COLUMNS
-- ===========================================

ALTER TABLE public.account_categories ALTER COLUMN id SET DEFAULT nextval('public.account_categories_id_seq');
ALTER TABLE public.account_types ALTER COLUMN id SET DEFAULT nextval('public.account_types_id_seq');
ALTER TABLE public.accounts ALTER COLUMN id SET DEFAULT nextval('public.accounts_id_seq');
ALTER TABLE public.bank_accounts ALTER COLUMN id SET DEFAULT nextval('public.bank_accounts_id_seq');
ALTER TABLE public.bank_transactions ALTER COLUMN id SET DEFAULT nextval('public.bank_transactions_id_seq');
ALTER TABLE public.budget_categories ALTER COLUMN id SET DEFAULT nextval('public.budget_categories_id_seq');
ALTER TABLE public.budget_items ALTER COLUMN id SET DEFAULT nextval('public.budget_items_id_seq');
ALTER TABLE public.budget_monthly_allocations ALTER COLUMN id SET DEFAULT nextval('public.budget_monthly_allocations_id_seq');
ALTER TABLE public.budget_projections ALTER COLUMN id SET DEFAULT nextval('public.budget_projections_id_seq');
ALTER TABLE public.budgets ALTER COLUMN id SET DEFAULT nextval('public.budgets_id_seq');
ALTER TABLE public.companies ALTER COLUMN id SET DEFAULT nextval('public.companies_id_seq');
ALTER TABLE public.data_corrections ALTER COLUMN id SET DEFAULT nextval('public.data_corrections_id_seq');
ALTER TABLE public.fiscal_periods ALTER COLUMN id SET DEFAULT nextval('public.fiscal_periods_id_seq');
ALTER TABLE public.journal_entries ALTER COLUMN id SET DEFAULT nextval('public.journal_entries_id_seq');
ALTER TABLE public.journal_entry_lines ALTER COLUMN id SET DEFAULT nextval('public.journal_entry_lines_id_seq');
ALTER TABLE public.manual_invoices ALTER COLUMN id SET DEFAULT nextval('public.manual_invoices_id_seq');
ALTER TABLE public.operational_activities ALTER COLUMN id SET DEFAULT nextval('public.operational_activities_id_seq');
ALTER TABLE public.strategic_initiatives ALTER COLUMN id SET DEFAULT nextval('public.strategic_initiatives_id_seq');
ALTER TABLE public.strategic_milestones ALTER COLUMN id SET DEFAULT nextval('public.strategic_milestones_id_seq');
ALTER TABLE public.strategic_plans ALTER COLUMN id SET DEFAULT nextval('public.strategic_plans_id_seq');
ALTER TABLE public.strategic_priorities ALTER COLUMN id SET DEFAULT nextval('public.strategic_priorities_id_seq');
ALTER TABLE public.transaction_mapping_rules ALTER COLUMN id SET DEFAULT nextval('public.transaction_mapping_rules_id_seq');
ALTER TABLE public.transaction_types ALTER COLUMN id SET DEFAULT nextval('public.transaction_types_id_seq');