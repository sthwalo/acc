-- V1__baseline_schema.sql
-- Baseline migration for FIN Financial Management System
-- This migration creates the complete database schema as it exists in production

-- ===========================================
-- FUNCTIONS
-- ===========================================

CREATE OR REPLACE FUNCTION public.get_closing_balance(p_company_id bigint, p_fiscal_period_id bigint) RETURNS numeric
    LANGUAGE plpgsql STABLE
    AS $$
DECLARE
    v_closing_balance NUMERIC := 0;
    v_end_date DATE;
BEGIN
    -- Get the fiscal period end date
    SELECT end_date INTO v_end_date
    FROM fiscal_periods
    WHERE id = p_fiscal_period_id;

    -- Get the closing balance from the last transaction on or before the end date
    SELECT balance INTO v_closing_balance
    FROM bank_transactions
    WHERE company_id = p_company_id
      AND transaction_date <= v_end_date
    ORDER BY transaction_date DESC, id DESC
    LIMIT 1;

    RETURN COALESCE(v_closing_balance, 0);
END;
$$;

CREATE OR REPLACE FUNCTION public.get_net_movement(p_company_id bigint, p_fiscal_period_id bigint) RETURNS TABLE(total_credits numeric, total_debits numeric, net_movement numeric, calculated_closing numeric, actual_closing numeric, difference numeric)
    LANGUAGE plpgsql STABLE
    AS $$
DECLARE
    v_start_date DATE;
    v_end_date DATE;
    v_opening NUMERIC;
BEGIN
    -- Get fiscal period dates
    SELECT start_date, end_date INTO v_start_date, v_end_date
    FROM fiscal_periods
    WHERE id = p_fiscal_period_id;

    -- Get opening balance
    v_opening := get_opening_balance(p_company_id, p_fiscal_period_id);

    RETURN QUERY
    SELECT
        SUM(COALESCE(bt.credit_amount, 0))::NUMERIC as total_credits,
        SUM(COALESCE(bt.debit_amount, 0))::NUMERIC as total_debits,
        (SUM(COALESCE(bt.credit_amount, 0)) - SUM(COALESCE(bt.debit_amount, 0)))::NUMERIC as net_movement,
        (v_opening + SUM(COALESCE(bt.credit_amount, 0)) - SUM(COALESCE(bt.debit_amount, 0)))::NUMERIC as calculated_closing,
        get_closing_balance(p_company_id, p_fiscal_period_id) as actual_closing,
        (get_closing_balance(p_company_id, p_fiscal_period_id) - (v_opening + SUM(COALESCE(bt.credit_amount, 0)) - SUM(COALESCE(bt.debit_amount, 0))))::NUMERIC as difference
    FROM bank_transactions bt
    WHERE bt.company_id = p_company_id
      AND bt.transaction_date BETWEEN v_start_date AND v_end_date;
END;
$$;

CREATE OR REPLACE FUNCTION public.get_opening_balance(p_company_id bigint, p_fiscal_period_id bigint) RETURNS numeric
    LANGUAGE plpgsql STABLE
    AS $$
DECLARE
    v_opening_balance NUMERIC := 0;
    v_start_date DATE;
BEGIN
    -- Get the fiscal period start date
    SELECT start_date INTO v_start_date
    FROM fiscal_periods
    WHERE id = p_fiscal_period_id;

    -- Get the balance from the last transaction before the start date
    SELECT balance INTO v_opening_balance
    FROM bank_transactions
    WHERE company_id = p_company_id
      AND transaction_date < v_start_date
    ORDER BY transaction_date DESC, id DESC
    LIMIT 1;

    RETURN COALESCE(v_opening_balance, 0);
END;
$$;

CREATE OR REPLACE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.verify_balance_continuity(p_company_id bigint, p_fiscal_period_id bigint) RETURNS TABLE(transaction_date date, description text, debit_amount numeric, credit_amount numeric, balance numeric, expected_balance numeric, difference numeric)
    LANGUAGE plpgsql STABLE
    AS $$
DECLARE
    v_previous_balance NUMERIC := 0;
    v_start_date DATE;
BEGIN
    -- Get fiscal period start date
    SELECT start_date INTO v_start_date
    FROM fiscal_periods
    WHERE id = p_fiscal_period_id;

    -- Get opening balance
    v_previous_balance := get_opening_balance(p_company_id, p_fiscal_period_id);

    RETURN QUERY
    SELECT
        bt.transaction_date,
        bt.description,
        bt.debit_amount,
        bt.credit_amount,
        bt.balance,
        (LAG(bt.balance, 1, v_previous_balance) OVER (ORDER BY bt.transaction_date, bt.id) +
         COALESCE(LAG(bt.credit_amount, 1, 0) OVER (ORDER BY bt.transaction_date, bt.id), 0) -
         COALESCE(LAG(bt.debit_amount, 1, 0) OVER (ORDER BY bt.transaction_date, bt.id), 0))::NUMERIC as expected_balance,
        (bt.balance - (LAG(bt.balance, 1, v_previous_balance) OVER (ORDER BY bt.transaction_date, bt.id) +
         COALESCE(LAG(bt.credit_amount, 1, 0) OVER (ORDER BY bt.transaction_date, bt.id), 0) -
         COALESCE(LAG(bt.debit_amount, 1, 0) OVER (ORDER BY bt.transaction_date, bt.id), 0)))::NUMERIC as difference
    FROM bank_transactions bt
    WHERE bt.company_id = p_company_id
      AND bt.transaction_date BETWEEN v_start_date AND (SELECT end_date FROM fiscal_periods WHERE id = p_fiscal_period_id)
    ORDER BY bt.transaction_date, bt.id;
END;
$$;

-- ===========================================
-- TABLES
-- ===========================================

-- Core Accounting Tables
CREATE TABLE public.account_categories (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.account_types (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    category_id bigint,
    normal_balance character varying(10) CHECK (normal_balance::text = ANY (ARRAY['debit'::character varying, 'credit'::character varying]::text[])),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.accounts (
    id bigint NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    type_id bigint,
    company_id bigint,
    parent_account_id bigint,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Company and Banking Tables
CREATE TABLE public.companies (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    registration_number character varying(100),
    tax_number character varying(100),
    address text,
    contact_email character varying(255),
    contact_phone character varying(50),
    bank_name character varying(255),
    account_number character varying(50),
    account_type character varying(50),
    branch_code character varying(20),
    vat_registered boolean DEFAULT false,
    logo_path character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.company_extended_info (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    business_description text,
    industry_sector character varying(255),
    fiscal_year_end date,
    auditor_name character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.fiscal_periods (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    is_closed boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Banking Tables
CREATE TABLE public.bank_accounts (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    account_name character varying(255) NOT NULL,
    account_number character varying(50) NOT NULL,
    bank_name character varying(255) NOT NULL,
    branch_code character varying(20),
    account_type character varying(50),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.bank_transactions (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    bank_account_id bigint,
    transaction_date date NOT NULL,
    description text,
    reference character varying(255),
    debit_amount numeric(15,2),
    credit_amount numeric(15,2),
    balance numeric(15,2),
    transaction_type_id bigint,
    category character varying(100),
    subcategory character varying(100),
    is_reconciled boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.bank_transactions_backup_20251003 (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    bank_account_id bigint,
    transaction_date date NOT NULL,
    description text,
    reference character varying(255),
    debit_amount numeric(15,2),
    credit_amount numeric(15,2),
    balance numeric(15,2),
    transaction_type_id bigint,
    category character varying(100),
    subcategory character varying(100),
    is_reconciled boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Journal Entries
CREATE TABLE public.journal_entries (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    entry_date date NOT NULL,
    description text,
    reference character varying(255),
    total_debit numeric(15,2) DEFAULT 0,
    total_credit numeric(15,2) DEFAULT 0,
    is_posted boolean DEFAULT false,
    created_by character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.journal_entry_lines (
    id bigint NOT NULL,
    journal_entry_id bigint NOT NULL,
    account_id bigint NOT NULL,
    description text,
    debit_amount numeric(15,2) DEFAULT 0,
    credit_amount numeric(15,2) DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Transaction Processing
CREATE TABLE public.transaction_types (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    category character varying(100),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.transaction_mapping_rules (
    id bigint NOT NULL,
    company_id bigint,
    pattern text NOT NULL,
    transaction_type_id bigint,
    account_id bigint,
    category character varying(100),
    subcategory character varying(100),
    priority integer DEFAULT 0,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.company_classification_rules (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    pattern text NOT NULL,
    category character varying(100),
    subcategory character varying(100),
    priority integer DEFAULT 0,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Asset Management
CREATE TABLE public.assets (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    asset_code character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    category character varying(100),
    purchase_date date,
    purchase_cost numeric(15,2),
    accumulated_depreciation numeric(15,2) DEFAULT 0,
    book_value numeric(15,2),
    depreciation_policy_id bigint,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.asset_disposals (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    disposal_date date NOT NULL,
    disposal_value numeric(15,2),
    gain_loss numeric(15,2),
    reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Depreciation
CREATE TABLE public.depreciation_policies (
    id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    method character varying(50) NOT NULL,
    useful_life_years integer,
    depreciation_rate numeric(5,2),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.depreciation_schedules (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    depreciation_amount numeric(15,2),
    accumulated_depreciation numeric(15,2),
    book_value numeric(15,2),
    is_calculated boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.depreciation_entries (
    id bigint NOT NULL,
    depreciation_schedule_id bigint NOT NULL,
    journal_entry_id bigint,
    amount numeric(15,2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.depreciation_adjustments (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    adjustment_date date NOT NULL,
    adjustment_amount numeric(15,2),
    reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.depreciation_journal_entries_backup (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    journal_entry_id bigint,
    depreciation_amount numeric(15,2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Payroll
CREATE TABLE public.employees (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    employee_number character varying(50) NOT NULL,
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    id_number character varying(20),
    tax_number character varying(20),
    email character varying(255),
    phone character varying(50),
    hire_date date,
    termination_date date,
    basic_salary numeric(15,2),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.payroll_periods (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    payment_date date,
    is_processed boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.payslips (
    id bigint NOT NULL,
    employee_id bigint NOT NULL,
    payroll_period_id bigint NOT NULL,
    basic_salary numeric(15,2),
    gross_pay numeric(15,2),
    net_pay numeric(15,2),
    paye_tax numeric(15,2),
    uif numeric(15,2),
    sdl numeric(15,2),
    total_deductions numeric(15,2),
    is_finalized boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.benefits (
    id bigint NOT NULL,
    payslip_id bigint NOT NULL,
    benefit_type character varying(100) NOT NULL,
    amount numeric(15,2),
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.deductions (
    id bigint NOT NULL,
    payslip_id bigint NOT NULL,
    deduction_type character varying(100) NOT NULL,
    amount numeric(15,2),
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.payroll_journal_entries (
    id bigint NOT NULL,
    payslip_id bigint NOT NULL,
    journal_entry_id bigint,
    account_id bigint,
    amount numeric(15,2),
    entry_type character varying(20),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.payroll_journal_entries_backup (
    id bigint NOT NULL,
    payslip_id bigint NOT NULL,
    journal_entry_id bigint,
    account_id bigint,
    amount numeric(15,2),
    entry_type character varying(20),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.employee_leave (
    id bigint NOT NULL,
    employee_id bigint NOT NULL,
    leave_type character varying(100) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    days_taken numeric(5,2),
    is_paid boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Tax Configuration
CREATE TABLE public.tax_brackets (
    id bigint NOT NULL,
    tax_year integer NOT NULL,
    min_income numeric(15,2) NOT NULL,
    max_income numeric(15,2),
    tax_rate numeric(5,2) NOT NULL,
    tax_amount numeric(15,2) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.tax_configurations (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    tax_type character varying(50) NOT NULL,
    rate numeric(5,2),
    threshold numeric(15,2),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Budget Management
CREATE TABLE public.budgets (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    fiscal_year integer NOT NULL,
    total_budget numeric(15,2),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.budget_categories (
    id bigint NOT NULL,
    budget_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    allocated_amount numeric(15,2),
    spent_amount numeric(15,2) DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.budget_items (
    id bigint NOT NULL,
    budget_category_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    budgeted_amount numeric(15,2),
    actual_amount numeric(15,2) DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.budget_monthly_allocations (
    id bigint NOT NULL,
    budget_item_id bigint NOT NULL,
    month integer NOT NULL,
    year integer NOT NULL,
    allocated_amount numeric(15,2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.budget_projections (
    id bigint NOT NULL,
    budget_id bigint NOT NULL,
    month integer NOT NULL,
    year integer NOT NULL,
    projected_revenue numeric(15,2),
    projected_expenses numeric(15,2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Strategic Planning
CREATE TABLE public.strategic_plans (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    start_year integer NOT NULL,
    end_year integer NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.strategic_priorities (
    id bigint NOT NULL,
    strategic_plan_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    priority_level integer,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.strategic_initiatives (
    id bigint NOT NULL,
    strategic_priority_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    start_date date,
    end_date date,
    budget_allocated numeric(15,2),
    status character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.strategic_milestones (
    id bigint NOT NULL,
    strategic_initiative_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    target_date date,
    is_completed boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Financial Reporting
CREATE TABLE public.audit_reports (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    report_type character varying(100) NOT NULL,
    title character varying(500),
    content text,
    auditor_name character varying(255),
    audit_date date,
    is_finalized boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.directors_reports (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    title character varying(500),
    content text,
    chairman_name character varying(255),
    approval_date date,
    is_finalized boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.directors_responsibility_statements (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    content text,
    is_finalized boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.financial_notes (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    note_number integer,
    title character varying(500),
    content text,
    is_finalized boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.equity_movements (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    movement_type character varying(100),
    description text,
    amount numeric(15,2),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Operational Activities
CREATE TABLE public.operational_activities (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    activity_type character varying(100),
    description text,
    amount numeric(15,2),
    activity_date date,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- Data Management
CREATE TABLE public.data_corrections (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    table_name character varying(100) NOT NULL,
    record_id bigint NOT NULL,
    field_name character varying(100) NOT NULL,
    old_value text,
    new_value text,
    correction_reason text,
    corrected_by character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.manual_invoices (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    invoice_number character varying(50) NOT NULL,
    invoice_date date NOT NULL,
    due_date date,
    customer_name character varying(255),
    customer_address text,
    total_amount numeric(15,2),
    tax_amount numeric(15,2),
    is_paid boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- ===========================================
-- PRIMARY KEYS
-- ===========================================

ALTER TABLE ONLY public.account_categories ADD CONSTRAINT account_categories_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.account_types ADD CONSTRAINT account_types_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.accounts ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.assets ADD CONSTRAINT assets_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.asset_disposals ADD CONSTRAINT asset_disposals_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.audit_reports ADD CONSTRAINT audit_reports_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.bank_accounts ADD CONSTRAINT bank_accounts_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.bank_transactions ADD CONSTRAINT bank_transactions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.bank_transactions_backup_20251003 ADD CONSTRAINT bank_transactions_backup_20251003_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.benefits ADD CONSTRAINT benefits_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.budget_categories ADD CONSTRAINT budget_categories_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.budget_items ADD CONSTRAINT budget_items_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.budget_monthly_allocations ADD CONSTRAINT budget_monthly_allocations_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.budget_projections ADD CONSTRAINT budget_projections_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.budgets ADD CONSTRAINT budgets_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.companies ADD CONSTRAINT companies_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.company_classification_rules ADD CONSTRAINT company_classification_rules_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.company_extended_info ADD CONSTRAINT company_extended_info_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.data_corrections ADD CONSTRAINT data_corrections_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.deductions ADD CONSTRAINT deductions_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.depreciation_adjustments ADD CONSTRAINT depreciation_adjustments_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.depreciation_entries ADD CONSTRAINT depreciation_entries_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.depreciation_journal_entries_backup ADD CONSTRAINT depreciation_journal_entries_backup_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.depreciation_policies ADD CONSTRAINT depreciation_policies_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.depreciation_schedules ADD CONSTRAINT depreciation_schedules_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.directors_reports ADD CONSTRAINT directors_reports_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.directors_responsibility_statements ADD CONSTRAINT directors_responsibility_statements_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.employee_leave ADD CONSTRAINT employee_leave_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.employees ADD CONSTRAINT employees_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.equity_movements ADD CONSTRAINT equity_movements_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.financial_notes ADD CONSTRAINT financial_notes_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.fiscal_periods ADD CONSTRAINT fiscal_periods_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.journal_entries ADD CONSTRAINT journal_entries_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.journal_entry_lines ADD CONSTRAINT journal_entry_lines_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.manual_invoices ADD CONSTRAINT manual_invoices_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.operational_activities ADD CONSTRAINT operational_activities_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.payroll_journal_entries ADD CONSTRAINT payroll_journal_entries_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.payroll_journal_entries_backup ADD CONSTRAINT payroll_journal_entries_backup_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.payroll_periods ADD CONSTRAINT payroll_periods_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.payslips ADD CONSTRAINT payslips_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.strategic_initiatives ADD CONSTRAINT strategic_initiatives_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.strategic_milestones ADD CONSTRAINT strategic_milestones_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.strategic_plans ADD CONSTRAINT strategic_plans_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.strategic_priorities ADD CONSTRAINT strategic_priorities_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.tax_brackets ADD CONSTRAINT tax_brackets_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.tax_configurations ADD CONSTRAINT tax_configurations_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.transaction_mapping_rules ADD CONSTRAINT transaction_mapping_rules_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.transaction_types ADD CONSTRAINT transaction_types_pkey PRIMARY KEY (id);

-- ===========================================
-- FOREIGN KEYS
-- ===========================================

ALTER TABLE ONLY public.account_types ADD CONSTRAINT account_types_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.account_categories(id);
ALTER TABLE ONLY public.accounts ADD CONSTRAINT accounts_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.accounts ADD CONSTRAINT accounts_parent_account_id_fkey FOREIGN KEY (parent_account_id) REFERENCES public.accounts(id);
ALTER TABLE ONLY public.accounts ADD CONSTRAINT accounts_type_id_fkey FOREIGN KEY (type_id) REFERENCES public.account_types(id);
ALTER TABLE ONLY public.asset_disposals ADD CONSTRAINT asset_disposals_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES public.assets(id);
ALTER TABLE ONLY public.assets ADD CONSTRAINT assets_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.assets ADD CONSTRAINT assets_depreciation_policy_id_fkey FOREIGN KEY (depreciation_policy_id) REFERENCES public.depreciation_policies(id);
ALTER TABLE ONLY public.audit_reports ADD CONSTRAINT audit_reports_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.audit_reports ADD CONSTRAINT audit_reports_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id);
ALTER TABLE ONLY public.bank_accounts ADD CONSTRAINT bank_accounts_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.bank_transactions ADD CONSTRAINT bank_transactions_bank_account_id_fkey FOREIGN KEY (bank_account_id) REFERENCES public.bank_accounts(id);
ALTER TABLE ONLY public.bank_transactions ADD CONSTRAINT bank_transactions_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.bank_transactions ADD CONSTRAINT bank_transactions_transaction_type_id_fkey FOREIGN KEY (transaction_type_id) REFERENCES public.transaction_types(id);
ALTER TABLE ONLY public.bank_transactions_backup_20251003 ADD CONSTRAINT bank_transactions_backup_20251003_bank_account_id_fkey FOREIGN KEY (bank_account_id) REFERENCES public.bank_accounts(id);
ALTER TABLE ONLY public.bank_transactions_backup_20251003 ADD CONSTRAINT bank_transactions_backup_20251003_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.bank_transactions_backup_20251003 ADD CONSTRAINT bank_transactions_backup_20251003_transaction_type_id_fkey FOREIGN KEY (transaction_type_id) REFERENCES public.transaction_types(id);
ALTER TABLE ONLY public.benefits ADD CONSTRAINT benefits_payslip_id_fkey FOREIGN KEY (payslip_id) REFERENCES public.payslips(id);
ALTER TABLE ONLY public.budget_categories ADD CONSTRAINT budget_categories_budget_id_fkey FOREIGN KEY (budget_id) REFERENCES public.budgets(id);
ALTER TABLE ONLY public.budget_items ADD CONSTRAINT budget_items_budget_category_id_fkey FOREIGN KEY (budget_category_id) REFERENCES public.budget_categories(id);
ALTER TABLE ONLY public.budget_monthly_allocations ADD CONSTRAINT budget_monthly_allocations_budget_item_id_fkey FOREIGN KEY (budget_item_id) REFERENCES public.budget_items(id);
ALTER TABLE ONLY public.budget_projections ADD CONSTRAINT budget_projections_budget_id_fkey FOREIGN KEY (budget_id) REFERENCES public.budgets(id);
ALTER TABLE ONLY public.budgets ADD CONSTRAINT budgets_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.company_classification_rules ADD CONSTRAINT company_classification_rules_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.company_extended_info ADD CONSTRAINT company_extended_info_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.data_corrections ADD CONSTRAINT data_corrections_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.deductions ADD CONSTRAINT deductions_payslip_id_fkey FOREIGN KEY (payslip_id) REFERENCES public.payslips(id);
ALTER TABLE ONLY public.depreciation_adjustments ADD CONSTRAINT depreciation_adjustments_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES public.assets(id);
ALTER TABLE ONLY public.depreciation_entries ADD CONSTRAINT depreciation_entries_depreciation_schedule_id_fkey FOREIGN KEY (depreciation_schedule_id) REFERENCES public.depreciation_schedules(id);
ALTER TABLE ONLY public.depreciation_entries ADD CONSTRAINT depreciation_entries_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id);
ALTER TABLE ONLY public.depreciation_journal_entries_backup ADD CONSTRAINT depreciation_journal_entries_backup_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES public.assets(id);
ALTER TABLE ONLY public.depreciation_journal_entries_backup ADD CONSTRAINT depreciation_journal_entries_backup_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id);
ALTER TABLE ONLY public.depreciation_journal_entries_backup ADD CONSTRAINT depreciation_journal_entries_backup_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id);
ALTER TABLE ONLY public.depreciation_schedules ADD CONSTRAINT depreciation_schedules_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES public.assets(id);
ALTER TABLE ONLY public.depreciation_schedules ADD CONSTRAINT depreciation_schedules_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id);
ALTER TABLE ONLY public.directors_reports ADD CONSTRAINT directors_reports_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.directors_reports ADD CONSTRAINT directors_reports_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id);
ALTER TABLE ONLY public.directors_responsibility_statements ADD CONSTRAINT directors_responsibility_statements_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.directors_responsibility_statements ADD CONSTRAINT directors_responsibility_statements_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id);
ALTER TABLE ONLY public.employee_leave ADD CONSTRAINT employee_leave_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employees(id);
ALTER TABLE ONLY public.employees ADD CONSTRAINT employees_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.equity_movements ADD CONSTRAINT equity_movements_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.equity_movements ADD CONSTRAINT equity_movements_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id);
ALTER TABLE ONLY public.financial_notes ADD CONSTRAINT financial_notes_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.financial_notes ADD CONSTRAINT financial_notes_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id);
ALTER TABLE ONLY public.fiscal_periods ADD CONSTRAINT fiscal_periods_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.journal_entries ADD CONSTRAINT journal_entries_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.journal_entries ADD CONSTRAINT journal_entries_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id);
ALTER TABLE ONLY public.journal_entry_lines ADD CONSTRAINT journal_entry_lines_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id);
ALTER TABLE ONLY public.journal_entry_lines ADD CONSTRAINT journal_entry_lines_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id);
ALTER TABLE ONLY public.manual_invoices ADD CONSTRAINT manual_invoices_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.operational_activities ADD CONSTRAINT operational_activities_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.payroll_journal_entries ADD CONSTRAINT payroll_journal_entries_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id);
ALTER TABLE ONLY public.payroll_journal_entries ADD CONSTRAINT payroll_journal_entries_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id);
ALTER TABLE ONLY public.payroll_journal_entries ADD CONSTRAINT payroll_journal_entries_payslip_id_fkey FOREIGN KEY (payslip_id) REFERENCES public.payslips(id);
ALTER TABLE ONLY public.payroll_journal_entries_backup ADD CONSTRAINT payroll_journal_entries_backup_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id);
ALTER TABLE ONLY public.payroll_journal_entries_backup ADD CONSTRAINT payroll_journal_entries_backup_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id);
ALTER TABLE ONLY public.payroll_journal_entries_backup ADD CONSTRAINT payroll_journal_entries_backup_payslip_id_fkey FOREIGN KEY (payslip_id) REFERENCES public.payslips(id);
ALTER TABLE ONLY public.payroll_periods ADD CONSTRAINT payroll_periods_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.payslips ADD CONSTRAINT payslips_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employees(id);
ALTER TABLE ONLY public.payslips ADD CONSTRAINT payslips_payroll_period_id_fkey FOREIGN KEY (payroll_period_id) REFERENCES public.payroll_periods(id);
ALTER TABLE ONLY public.strategic_initiatives ADD CONSTRAINT strategic_initiatives_strategic_priority_id_fkey FOREIGN KEY (strategic_priority_id) REFERENCES public.strategic_priorities(id);
ALTER TABLE ONLY public.strategic_milestones ADD CONSTRAINT strategic_milestones_strategic_initiative_id_fkey FOREIGN KEY (strategic_initiative_id) REFERENCES public.strategic_initiatives(id);
ALTER TABLE ONLY public.strategic_plans ADD CONSTRAINT strategic_plans_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.strategic_priorities ADD CONSTRAINT strategic_priorities_strategic_plan_id_fkey FOREIGN KEY (strategic_plan_id) REFERENCES public.strategic_plans(id);
ALTER TABLE ONLY public.tax_configurations ADD CONSTRAINT tax_configurations_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.transaction_mapping_rules ADD CONSTRAINT transaction_mapping_rules_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id);
ALTER TABLE ONLY public.transaction_mapping_rules ADD CONSTRAINT transaction_mapping_rules_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);
ALTER TABLE ONLY public.transaction_mapping_rules ADD CONSTRAINT transaction_mapping_rules_transaction_type_id_fkey FOREIGN KEY (transaction_type_id) REFERENCES public.transaction_types(id);

-- ===========================================
-- INDEXES
-- ===========================================

CREATE INDEX idx_account_types_category_id ON public.account_types USING btree (category_id);
CREATE INDEX idx_accounts_code ON public.accounts USING btree (code);
CREATE INDEX idx_accounts_company_id ON public.accounts USING btree (company_id);
CREATE INDEX idx_accounts_parent_account_id ON public.accounts USING btree (parent_account_id);
CREATE INDEX idx_accounts_type_id ON public.accounts USING btree (type_id);
CREATE INDEX idx_asset_disposals_asset_id ON public.asset_disposals USING btree (asset_id);
CREATE INDEX idx_assets_company_id ON public.assets USING btree (company_id);
CREATE INDEX idx_assets_depreciation_policy_id ON public.assets USING btree (depreciation_policy_id);
CREATE INDEX idx_audit_reports_company_id ON public.audit_reports USING btree (company_id);
CREATE INDEX idx_audit_reports_fiscal_period_id ON public.audit_reports USING btree (fiscal_period_id);
CREATE INDEX idx_bank_accounts_company_id ON public.bank_accounts USING btree (company_id);
CREATE INDEX idx_bank_transactions_bank_account_id ON public.bank_transactions USING btree (bank_account_id);
CREATE INDEX idx_bank_transactions_company_id ON public.bank_transactions USING btree (company_id);
CREATE INDEX idx_bank_transactions_date ON public.bank_transactions USING btree (transaction_date);
CREATE INDEX idx_bank_transactions_transaction_type_id ON public.bank_transactions USING btree (transaction_type_id);
CREATE INDEX idx_bank_transactions_backup_20251003_bank_account_id ON public.bank_transactions_backup_20251003 USING btree (bank_account_id);
CREATE INDEX idx_bank_transactions_backup_20251003_company_id ON public.bank_transactions_backup_20251003 USING btree (company_id);
CREATE INDEX idx_bank_transactions_backup_20251003_date ON public.bank_transactions_backup_20251003 USING btree (transaction_date);
CREATE INDEX idx_bank_transactions_backup_20251003_transaction_type_id ON public.bank_transactions_backup_20251003 USING btree (transaction_type_id);
CREATE INDEX idx_benefits_payslip_id ON public.benefits USING btree (payslip_id);
CREATE INDEX idx_budget_categories_budget_id ON public.budget_categories USING btree (budget_id);
CREATE INDEX idx_budget_items_budget_category_id ON public.budget_items USING btree (budget_category_id);
CREATE INDEX idx_budget_monthly_allocations_budget_item_id ON public.budget_monthly_allocations USING btree (budget_item_id);
CREATE INDEX idx_budget_projections_budget_id ON public.budget_projections USING btree (budget_id);
CREATE INDEX idx_budgets_company_id ON public.budgets USING btree (company_id);
CREATE INDEX idx_company_classification_rules_company_id ON public.company_classification_rules USING btree (company_id);
CREATE INDEX idx_company_extended_info_company_id ON public.company_extended_info USING btree (company_id);
CREATE INDEX idx_data_corrections_company_id ON public.data_corrections USING btree (company_id);
CREATE INDEX idx_deductions_payslip_id ON public.deductions USING btree (payslip_id);
CREATE INDEX idx_depreciation_adjustments_asset_id ON public.depreciation_adjustments USING btree (asset_id);
CREATE INDEX idx_depreciation_entries_depreciation_schedule_id ON public.depreciation_entries USING btree (depreciation_schedule_id);
CREATE INDEX idx_depreciation_entries_journal_entry_id ON public.depreciation_entries USING btree (journal_entry_id);
CREATE INDEX idx_depreciation_journal_entries_backup_asset_id ON public.depreciation_journal_entries_backup USING btree (asset_id);
CREATE INDEX idx_depreciation_journal_entries_backup_fiscal_period_id ON public.depreciation_journal_entries_backup USING btree (fiscal_period_id);
CREATE INDEX idx_depreciation_journal_entries_backup_journal_entry_id ON public.depreciation_journal_entries_backup USING btree (journal_entry_id);
CREATE INDEX idx_depreciation_schedules_asset_id ON public.depreciation_schedules USING btree (asset_id);
CREATE INDEX idx_depreciation_schedules_fiscal_period_id ON public.depreciation_schedules USING btree (fiscal_period_id);
CREATE INDEX idx_directors_reports_company_id ON public.directors_reports USING btree (company_id);
CREATE INDEX idx_directors_reports_fiscal_period_id ON public.directors_reports USING btree (fiscal_period_id);
CREATE INDEX idx_directors_responsibility_statements_company_id ON public.directors_responsibility_statements USING btree (company_id);
CREATE INDEX idx_directors_responsibility_statements_fiscal_period_id ON public.directors_responsibility_statements USING btree (fiscal_period_id);
CREATE INDEX idx_employee_leave_employee_id ON public.employee_leave USING btree (employee_id);
CREATE INDEX idx_employees_company_id ON public.employees USING btree (company_id);
CREATE INDEX idx_equity_movements_company_id ON public.equity_movements USING btree (company_id);
CREATE INDEX idx_equity_movements_fiscal_period_id ON public.equity_movements USING btree (fiscal_period_id);
CREATE INDEX idx_financial_notes_company_id ON public.financial_notes USING btree (company_id);
CREATE INDEX idx_financial_notes_fiscal_period_id ON public.financial_notes USING btree (fiscal_period_id);
CREATE INDEX idx_fiscal_periods_company_id ON public.fiscal_periods USING btree (company_id);
CREATE INDEX idx_journal_entries_company_id ON public.journal_entries USING btree (company_id);
CREATE INDEX idx_journal_entries_fiscal_period_id ON public.journal_entries USING btree (fiscal_period_id);
CREATE INDEX idx_journal_entry_lines_account_id ON public.journal_entry_lines USING btree (account_id);
CREATE INDEX idx_journal_entry_lines_journal_entry_id ON public.journal_entry_lines USING btree (journal_entry_id);
CREATE INDEX idx_manual_invoices_company_id ON public.manual_invoices USING btree (company_id);
CREATE INDEX idx_operational_activities_company_id ON public.operational_activities USING btree (company_id);
CREATE INDEX idx_payroll_journal_entries_account_id ON public.payroll_journal_entries USING btree (account_id);
CREATE INDEX idx_payroll_journal_entries_journal_entry_id ON public.payroll_journal_entries USING btree (journal_entry_id);
CREATE INDEX idx_payroll_journal_entries_payslip_id ON public.payroll_journal_entries USING btree (payslip_id);
CREATE INDEX idx_payroll_journal_entries_backup_account_id ON public.payroll_journal_entries_backup USING btree (account_id);
CREATE INDEX idx_payroll_journal_entries_backup_journal_entry_id ON public.payroll_journal_entries_backup USING btree (journal_entry_id);
CREATE INDEX idx_payroll_journal_entries_backup_payslip_id ON public.payroll_journal_entries_backup USING btree (payslip_id);
CREATE INDEX idx_payroll_periods_company_id ON public.payroll_periods USING btree (company_id);
CREATE INDEX idx_payslips_employee_id ON public.payslips USING btree (employee_id);
CREATE INDEX idx_payslips_payroll_period_id ON public.payslips USING btree (payroll_period_id);
CREATE INDEX idx_strategic_initiatives_strategic_priority_id ON public.strategic_initiatives USING btree (strategic_priority_id);
CREATE INDEX idx_strategic_milestones_strategic_initiative_id ON public.strategic_milestones USING btree (strategic_initiative_id);
CREATE INDEX idx_strategic_plans_company_id ON public.strategic_plans USING btree (company_id);
CREATE INDEX idx_strategic_priorities_strategic_plan_id ON public.strategic_priorities USING btree (strategic_plan_id);
CREATE INDEX idx_tax_configurations_company_id ON public.tax_configurations USING btree (company_id);
CREATE INDEX idx_transaction_mapping_rules_account_id ON public.transaction_mapping_rules USING btree (account_id);
CREATE INDEX idx_transaction_mapping_rules_company_id ON public.transaction_mapping_rules USING btree (company_id);
CREATE INDEX idx_transaction_mapping_rules_transaction_type_id ON public.transaction_mapping_rules USING btree (transaction_type_id);

-- ===========================================
-- TRIGGERS
-- ===========================================

CREATE TRIGGER update_account_categories_updated_at BEFORE UPDATE ON public.account_categories FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_account_types_updated_at BEFORE UPDATE ON public.account_types FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON public.accounts FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_assets_updated_at BEFORE UPDATE ON public.assets FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_asset_disposals_updated_at BEFORE UPDATE ON public.asset_disposals FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_audit_reports_updated_at BEFORE UPDATE ON public.audit_reports FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_bank_accounts_updated_at BEFORE UPDATE ON public.bank_accounts FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_bank_transactions_updated_at BEFORE UPDATE ON public.bank_transactions FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_bank_transactions_backup_20251003_updated_at BEFORE UPDATE ON public.bank_transactions_backup_20251003 FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_benefits_updated_at BEFORE UPDATE ON public.benefits FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_budget_categories_updated_at BEFORE UPDATE ON public.budget_categories FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_budget_items_updated_at BEFORE UPDATE ON public.budget_items FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_budget_monthly_allocations_updated_at BEFORE UPDATE ON public.budget_monthly_allocations FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_budget_projections_updated_at BEFORE UPDATE ON public.budget_projections FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_budgets_updated_at BEFORE UPDATE ON public.budgets FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_companies_updated_at BEFORE UPDATE ON public.companies FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_company_classification_rules_updated_at BEFORE UPDATE ON public.company_classification_rules FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_company_extended_info_updated_at BEFORE UPDATE ON public.company_extended_info FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_data_corrections_updated_at BEFORE UPDATE ON public.data_corrections FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_deductions_updated_at BEFORE UPDATE ON public.deductions FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_depreciation_adjustments_updated_at BEFORE UPDATE ON public.depreciation_adjustments FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_depreciation_entries_updated_at BEFORE UPDATE ON public.depreciation_entries FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_depreciation_journal_entries_backup_updated_at BEFORE UPDATE ON public.depreciation_journal_entries_backup FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_depreciation_policies_updated_at BEFORE UPDATE ON public.depreciation_policies FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_depreciation_schedules_updated_at BEFORE UPDATE ON public.depreciation_schedules FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_directors_reports_updated_at BEFORE UPDATE ON public.directors_reports FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_directors_responsibility_statements_updated_at BEFORE UPDATE ON public.directors_responsibility_statements FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_employee_leave_updated_at BEFORE UPDATE ON public.employee_leave FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_employees_updated_at BEFORE UPDATE ON public.employees FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_equity_movements_updated_at BEFORE UPDATE ON public.equity_movements FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_financial_notes_updated_at BEFORE UPDATE ON public.financial_notes FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_fiscal_periods_updated_at BEFORE UPDATE ON public.fiscal_periods FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_journal_entries_updated_at BEFORE UPDATE ON public.journal_entries FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_journal_entry_lines_updated_at BEFORE UPDATE ON public.journal_entry_lines FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_manual_invoices_updated_at BEFORE UPDATE ON public.manual_invoices FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_operational_activities_updated_at BEFORE UPDATE ON public.operational_activities FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_payroll_journal_entries_updated_at BEFORE UPDATE ON public.payroll_journal_entries FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_payroll_journal_entries_backup_updated_at BEFORE UPDATE ON public.payroll_journal_entries_backup FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_payroll_periods_updated_at BEFORE UPDATE ON public.payroll_periods FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_payslips_updated_at BEFORE UPDATE ON public.payslips FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_strategic_initiatives_updated_at BEFORE UPDATE ON public.strategic_initiatives FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_strategic_milestones_updated_at BEFORE UPDATE ON public.strategic_milestones FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_strategic_plans_updated_at BEFORE UPDATE ON public.strategic_plans FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_strategic_priorities_updated_at BEFORE UPDATE ON public.strategic_priorities FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_tax_brackets_updated_at BEFORE UPDATE ON public.tax_brackets FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_tax_configurations_updated_at BEFORE UPDATE ON public.tax_configurations FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_transaction_mapping_rules_updated_at BEFORE UPDATE ON public.transaction_mapping_rules FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_transaction_types_updated_at BEFORE UPDATE ON public.transaction_types FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();