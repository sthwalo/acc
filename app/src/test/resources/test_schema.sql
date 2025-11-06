--
-- PostgreSQL database dump
--

-- Dumped from database version 13.21
-- Dumped by pg_dump version 13.21

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE ONLY public.transaction_types DROP CONSTRAINT transaction_types_company_id_fkey;
ALTER TABLE ONLY public.transaction_mapping_rules DROP CONSTRAINT transaction_mapping_rules_company_id_fkey;
ALTER TABLE ONLY public.transaction_mapping_rules DROP CONSTRAINT transaction_mapping_rules_account_id_fkey;
ALTER TABLE ONLY public.tax_configurations DROP CONSTRAINT tax_configurations_company_id_fkey;
ALTER TABLE ONLY public.tax_brackets DROP CONSTRAINT tax_brackets_tax_configuration_id_fkey;
ALTER TABLE ONLY public.payslips DROP CONSTRAINT payslips_payroll_period_id_fkey;
ALTER TABLE ONLY public.payslips DROP CONSTRAINT payslips_employee_id_fkey;
ALTER TABLE ONLY public.payslips DROP CONSTRAINT payslips_company_id_fkey;
ALTER TABLE ONLY public.payroll_periods DROP CONSTRAINT payroll_periods_fiscal_period_id_fkey;
ALTER TABLE ONLY public.payroll_periods DROP CONSTRAINT payroll_periods_company_id_fkey;
ALTER TABLE ONLY public.payroll_journal_entries DROP CONSTRAINT payroll_journal_entries_payroll_period_id_fkey;
ALTER TABLE ONLY public.payroll_journal_entries DROP CONSTRAINT payroll_journal_entries_journal_entry_id_fkey;
ALTER TABLE ONLY public.payroll_journal_entries DROP CONSTRAINT payroll_journal_entries_company_id_fkey;
ALTER TABLE ONLY public.manual_invoices DROP CONSTRAINT manual_invoices_fiscal_period_id_fkey;
ALTER TABLE ONLY public.manual_invoices DROP CONSTRAINT manual_invoices_debit_account_id_fkey;
ALTER TABLE ONLY public.manual_invoices DROP CONSTRAINT manual_invoices_credit_account_id_fkey;
ALTER TABLE ONLY public.manual_invoices DROP CONSTRAINT manual_invoices_company_id_fkey;
ALTER TABLE ONLY public.journal_entry_lines DROP CONSTRAINT journal_entry_lines_source_transaction_id_fkey;
ALTER TABLE ONLY public.journal_entry_lines DROP CONSTRAINT journal_entry_lines_journal_entry_id_fkey;
ALTER TABLE ONLY public.journal_entry_lines DROP CONSTRAINT journal_entry_lines_account_id_fkey;
ALTER TABLE ONLY public.journal_entries DROP CONSTRAINT journal_entries_transaction_type_id_fkey;
ALTER TABLE ONLY public.journal_entries DROP CONSTRAINT journal_entries_fiscal_period_id_fkey;
ALTER TABLE ONLY public.journal_entries DROP CONSTRAINT journal_entries_company_id_fkey;
ALTER TABLE ONLY public.company_classification_rules DROP CONSTRAINT fk_classification_company;
ALTER TABLE ONLY public.fiscal_periods DROP CONSTRAINT fiscal_periods_company_id_fkey;
ALTER TABLE ONLY public.employees DROP CONSTRAINT employees_company_id_fkey;
ALTER TABLE ONLY public.employee_leave DROP CONSTRAINT employee_leave_employee_id_fkey;
ALTER TABLE ONLY public.employee_leave DROP CONSTRAINT employee_leave_company_id_fkey;
ALTER TABLE ONLY public.deductions DROP CONSTRAINT deductions_employee_id_fkey;
ALTER TABLE ONLY public.deductions DROP CONSTRAINT deductions_company_id_fkey;
ALTER TABLE ONLY public.data_corrections DROP CONSTRAINT data_corrections_transaction_id_fkey;
ALTER TABLE ONLY public.data_corrections DROP CONSTRAINT data_corrections_original_account_id_fkey;
ALTER TABLE ONLY public.data_corrections DROP CONSTRAINT data_corrections_new_account_id_fkey;
ALTER TABLE ONLY public.data_corrections DROP CONSTRAINT data_corrections_company_id_fkey;
ALTER TABLE ONLY public.benefits DROP CONSTRAINT benefits_employee_id_fkey;
ALTER TABLE ONLY public.benefits DROP CONSTRAINT benefits_company_id_fkey;
ALTER TABLE ONLY public.bank_transactions DROP CONSTRAINT bank_transactions_fiscal_period_id_fkey;
ALTER TABLE ONLY public.bank_transactions DROP CONSTRAINT bank_transactions_company_id_fkey;
ALTER TABLE ONLY public.bank_transactions DROP CONSTRAINT bank_transactions_bank_account_id_fkey;
ALTER TABLE ONLY public.bank_accounts DROP CONSTRAINT bank_accounts_company_id_fkey;
ALTER TABLE ONLY public.accounts DROP CONSTRAINT accounts_parent_account_id_fkey;
ALTER TABLE ONLY public.accounts DROP CONSTRAINT accounts_company_id_fkey;
ALTER TABLE ONLY public.accounts DROP CONSTRAINT accounts_category_id_fkey;
ALTER TABLE ONLY public.account_categories DROP CONSTRAINT account_categories_company_id_fkey;
ALTER TABLE ONLY public.account_categories DROP CONSTRAINT account_categories_account_type_id_fkey;
DROP TRIGGER update_transaction_mapping_rules_updated_at ON public.transaction_mapping_rules;
DROP TRIGGER update_manual_invoices_updated_at ON public.manual_invoices;
DROP TRIGGER update_journal_entries_updated_at ON public.journal_entries;
DROP TRIGGER update_accounts_updated_at ON public.accounts;
DROP TRIGGER update_account_categories_updated_at ON public.account_categories;
DROP INDEX public.idx_transaction_types_company;
DROP INDEX public.idx_transaction_mapping_rules_company;
DROP INDEX public.idx_tax_configurations_company;
DROP INDEX public.idx_payslips_status;
DROP INDEX public.idx_payslips_payroll_period_id;
DROP INDEX public.idx_payslips_employee_id;
DROP INDEX public.idx_payslips_company_id;
DROP INDEX public.idx_payroll_periods_status;
DROP INDEX public.idx_payroll_periods_pay_date;
DROP INDEX public.idx_payroll_periods_company_id;
DROP INDEX public.idx_mapping_rules_company;
DROP INDEX public.idx_journal_entry_lines_account;
DROP INDEX public.idx_journal_entries_company_date;
DROP INDEX public.idx_employees_is_active;
DROP INDEX public.idx_employees_employee_number;
DROP INDEX public.idx_employees_company_id;
DROP INDEX public.idx_deductions_is_active;
DROP INDEX public.idx_deductions_employee_id;
DROP INDEX public.idx_deductions_company_id;
DROP INDEX public.idx_deductions_company;
DROP INDEX public.idx_classification_rules_usage;
DROP INDEX public.idx_classification_rules_pattern;
DROP INDEX public.idx_classification_rules_company;
DROP INDEX public.idx_benefits_is_active;
DROP INDEX public.idx_benefits_employee_id;
DROP INDEX public.idx_benefits_company_id;
DROP INDEX public.idx_benefits_company;
DROP INDEX public.idx_bank_transactions_fiscal_period;
DROP INDEX public.idx_bank_transactions_company_date;
DROP INDEX public.idx_bank_transactions_account;
DROP INDEX public.idx_accounts_parent;
DROP INDEX public.idx_accounts_company;
DROP INDEX public.idx_accounts_category;
ALTER TABLE ONLY public.transaction_types DROP CONSTRAINT transaction_types_pkey;
ALTER TABLE ONLY public.transaction_mapping_rules DROP CONSTRAINT transaction_mapping_rules_pkey;
ALTER TABLE ONLY public.tax_configurations DROP CONSTRAINT tax_configurations_pkey;
ALTER TABLE ONLY public.tax_brackets DROP CONSTRAINT tax_brackets_pkey;
ALTER TABLE ONLY public.payslips DROP CONSTRAINT payslips_pkey;
ALTER TABLE ONLY public.payslips DROP CONSTRAINT payslips_payslip_number_key;
ALTER TABLE ONLY public.payroll_periods DROP CONSTRAINT payroll_periods_pkey;
ALTER TABLE ONLY public.payroll_journal_entries DROP CONSTRAINT payroll_journal_entries_pkey;
ALTER TABLE ONLY public.manual_invoices DROP CONSTRAINT manual_invoices_pkey;
ALTER TABLE ONLY public.manual_invoices DROP CONSTRAINT manual_invoices_company_id_invoice_number_key;
ALTER TABLE ONLY public.journal_entry_lines DROP CONSTRAINT journal_entry_lines_pkey;
ALTER TABLE ONLY public.journal_entries DROP CONSTRAINT journal_entries_pkey;
ALTER TABLE ONLY public.fiscal_periods DROP CONSTRAINT fiscal_periods_pkey;
ALTER TABLE ONLY public.employees DROP CONSTRAINT employees_pkey;
ALTER TABLE ONLY public.employees DROP CONSTRAINT employees_employee_number_key;
ALTER TABLE ONLY public.employees DROP CONSTRAINT employees_email_key;
ALTER TABLE ONLY public.employee_leave DROP CONSTRAINT employee_leave_pkey;
ALTER TABLE ONLY public.deductions DROP CONSTRAINT deductions_pkey;
ALTER TABLE ONLY public.data_corrections DROP CONSTRAINT data_corrections_pkey;
ALTER TABLE ONLY public.company_classification_rules DROP CONSTRAINT company_classification_rules_pkey;
ALTER TABLE ONLY public.companies DROP CONSTRAINT companies_pkey;
ALTER TABLE ONLY public.benefits DROP CONSTRAINT benefits_pkey;
ALTER TABLE ONLY public.bank_transactions DROP CONSTRAINT bank_transactions_pkey;
ALTER TABLE ONLY public.bank_accounts DROP CONSTRAINT bank_accounts_pkey;
ALTER TABLE ONLY public.accounts DROP CONSTRAINT accounts_pkey;
ALTER TABLE ONLY public.accounts DROP CONSTRAINT accounts_company_id_account_code_key;
ALTER TABLE ONLY public.account_types DROP CONSTRAINT account_types_pkey;
ALTER TABLE ONLY public.account_types DROP CONSTRAINT account_types_code_key;
ALTER TABLE ONLY public.account_categories DROP CONSTRAINT account_categories_pkey;
ALTER TABLE ONLY public.account_categories DROP CONSTRAINT account_categories_company_id_name_key;
ALTER TABLE public.transaction_types ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.transaction_mapping_rules ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.tax_configurations ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.tax_brackets ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.payslips ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.payroll_periods ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.payroll_journal_entries ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.manual_invoices ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.journal_entry_lines ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.journal_entries ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.fiscal_periods ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.employees ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.employee_leave ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.deductions ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.data_corrections ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.company_classification_rules ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.companies ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.benefits ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.bank_transactions ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.bank_accounts ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.accounts ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.account_types ALTER COLUMN id DROP DEFAULT;
ALTER TABLE public.account_categories ALTER COLUMN id DROP DEFAULT;
DROP VIEW public.v_payslip_summary;
DROP VIEW public.v_employee_summary;
DROP SEQUENCE public.transaction_types_id_seq;
DROP TABLE public.transaction_types;
DROP SEQUENCE public.transaction_mapping_rules_id_seq;
DROP TABLE public.transaction_mapping_rules;
DROP SEQUENCE public.tax_configurations_id_seq;
DROP TABLE public.tax_configurations;
DROP SEQUENCE public.tax_brackets_id_seq;
DROP TABLE public.tax_brackets;
DROP SEQUENCE public.payslips_id_seq;
DROP TABLE public.payslips;
DROP SEQUENCE public.payroll_periods_id_seq;
DROP TABLE public.payroll_periods;
DROP SEQUENCE public.payroll_journal_entries_id_seq;
DROP TABLE public.payroll_journal_entries;
DROP SEQUENCE public.manual_invoices_id_seq;
DROP TABLE public.manual_invoices;
DROP SEQUENCE public.journal_entry_lines_id_seq;
DROP TABLE public.journal_entry_lines;
DROP SEQUENCE public.journal_entries_id_seq;
DROP TABLE public.journal_entries;
DROP SEQUENCE public.fiscal_periods_id_seq;
DROP TABLE public.fiscal_periods;
DROP SEQUENCE public.employees_id_seq;
DROP TABLE public.employees;
DROP SEQUENCE public.employee_leave_id_seq;
DROP TABLE public.employee_leave;
DROP SEQUENCE public.deductions_id_seq;
DROP TABLE public.deductions;
DROP SEQUENCE public.data_corrections_id_seq;
DROP TABLE public.data_corrections;
DROP SEQUENCE public.company_classification_rules_id_seq;
DROP TABLE public.company_classification_rules;
DROP SEQUENCE public.companies_id_seq;
DROP TABLE public.companies;
DROP SEQUENCE public.benefits_id_seq;
DROP TABLE public.benefits;
DROP SEQUENCE public.bank_transactions_id_seq;
DROP TABLE public.bank_transactions;
DROP SEQUENCE public.bank_accounts_id_seq;
DROP TABLE public.bank_accounts;
DROP SEQUENCE public.accounts_id_seq;
DROP TABLE public.accounts;
DROP SEQUENCE public.account_types_id_seq;
DROP TABLE public.account_types;
DROP SEQUENCE public.account_categories_id_seq;
DROP TABLE public.account_categories;
DROP FUNCTION public.update_updated_at_column();
--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: account_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.account_categories (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    account_type_id integer NOT NULL,
    company_id integer NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: account_categories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.account_categories_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: account_categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.account_categories_id_seq OWNED BY public.account_categories.id;


--
-- Name: account_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.account_types (
    id integer NOT NULL,
    code character varying(10) NOT NULL,
    name character varying(100) NOT NULL,
    normal_balance character(1) NOT NULL,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT account_types_normal_balance_check CHECK ((normal_balance = ANY (ARRAY['D'::bpchar, 'C'::bpchar])))
);


--
-- Name: account_types_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.account_types_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: account_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.account_types_id_seq OWNED BY public.account_types.id;


--
-- Name: accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.accounts (
    id integer NOT NULL,
    account_code character varying(50) NOT NULL,
    account_name character varying(255) NOT NULL,
    description text,
    category_id integer NOT NULL,
    parent_account_id integer,
    company_id integer NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: accounts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.accounts_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: accounts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.accounts_id_seq OWNED BY public.accounts.id;


--
-- Name: bank_accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bank_accounts (
    id integer NOT NULL,
    company_id integer NOT NULL,
    account_number character varying(50) NOT NULL,
    account_name character varying(255) NOT NULL,
    account_type character varying(50),
    bank_name character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: bank_accounts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bank_accounts_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bank_accounts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bank_accounts_id_seq OWNED BY public.bank_accounts.id;


--
-- Name: bank_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bank_transactions (
    id integer NOT NULL,
    company_id integer NOT NULL,
    bank_account_id integer,
    fiscal_period_id integer NOT NULL,
    transaction_date date NOT NULL,
    details text,
    debit_amount numeric(15,2),
    credit_amount numeric(15,2),
    balance numeric(15,2),
    service_fee boolean DEFAULT false,
    account_number character varying(50),
    statement_period character varying(50),
    source_file character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    account_code character varying(10),
    account_name character varying(255),
    classification_date timestamp without time zone,
    classified_by character varying(100)
);


--
-- Name: bank_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.bank_transactions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: bank_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.bank_transactions_id_seq OWNED BY public.bank_transactions.id;


--
-- Name: benefits; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.benefits (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    employee_id bigint,
    benefit_code character varying(20) NOT NULL,
    benefit_name character varying(100) NOT NULL,
    benefit_type character varying(20) NOT NULL,
    amount numeric(15,2),
    percentage numeric(5,2),
    is_taxable boolean DEFAULT true,
    is_active boolean DEFAULT true,
    effective_from date,
    effective_to date,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100)
);


--
-- Name: benefits_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.benefits_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: benefits_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.benefits_id_seq OWNED BY public.benefits.id;


--
-- Name: companies; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.companies (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    registration_number character varying(50),
    tax_number character varying(50),
    address text,
    contact_email character varying(255),
    contact_phone character varying(50),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    logo_path character varying(500),
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: companies_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.companies_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: companies_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.companies_id_seq OWNED BY public.companies.id;


--
-- Name: company_classification_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.company_classification_rules (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    pattern text NOT NULL,
    keywords character varying(255)[] NOT NULL,
    account_code character varying(10) NOT NULL,
    account_name character varying(255) NOT NULL,
    usage_count integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_used timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: company_classification_rules_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.company_classification_rules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: company_classification_rules_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.company_classification_rules_id_seq OWNED BY public.company_classification_rules.id;


--
-- Name: data_corrections; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.data_corrections (
    id integer NOT NULL,
    company_id integer NOT NULL,
    transaction_id integer NOT NULL,
    original_account_id integer NOT NULL,
    new_account_id integer NOT NULL,
    correction_reason text NOT NULL,
    corrected_by character varying(100) NOT NULL,
    corrected_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: data_corrections_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.data_corrections_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: data_corrections_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.data_corrections_id_seq OWNED BY public.data_corrections.id;


--
-- Name: deductions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.deductions (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    employee_id bigint,
    deduction_code character varying(20) NOT NULL,
    deduction_name character varying(100) NOT NULL,
    deduction_type character varying(20) NOT NULL,
    amount numeric(15,2),
    percentage numeric(5,2),
    is_pre_tax boolean DEFAULT false,
    is_active boolean DEFAULT true,
    effective_from date,
    effective_to date,
    total_loan_amount numeric(15,2),
    remaining_balance numeric(15,2),
    installment_amount numeric(15,2),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100)
);


--
-- Name: deductions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.deductions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: deductions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.deductions_id_seq OWNED BY public.deductions.id;


--
-- Name: employee_leave; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.employee_leave (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    leave_type character varying(20) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    days_taken numeric(5,2) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying,
    reason text,
    affects_pay boolean DEFAULT false,
    approved_by character varying(100),
    approved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100)
);


--
-- Name: employee_leave_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.employee_leave_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: employee_leave_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.employee_leave_id_seq OWNED BY public.employee_leave.id;


--
-- Name: employees; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.employees (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    employee_number character varying(50) NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    email character varying(255),
    phone character varying(20),
    "position" character varying(100),
    department character varying(100),
    hire_date date NOT NULL,
    termination_date date,
    is_active boolean DEFAULT true,
    address_line1 character varying(255),
    address_line2 character varying(255),
    city character varying(100),
    province character varying(100),
    postal_code character varying(10),
    country character varying(2) DEFAULT 'ZA'::character varying,
    bank_name character varying(100),
    account_holder_name character varying(255),
    account_number character varying(50),
    branch_code character varying(20),
    account_type character varying(20) DEFAULT 'SAVINGS'::character varying,
    employment_type character varying(20) DEFAULT 'PERMANENT'::character varying,
    salary_type character varying(20) DEFAULT 'MONTHLY'::character varying,
    basic_salary numeric(15,2) NOT NULL,
    overtime_rate numeric(5,2) DEFAULT 1.5,
    tax_number character varying(50),
    tax_rebate_code character varying(10) DEFAULT 'A'::character varying,
    uif_number character varying(50),
    medical_aid_number character varying(50),
    pension_fund_number character varying(50),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100),
    updated_by character varying(100),
    title character varying(20),
    second_name character varying(100),
    known_as_name character varying(100),
    birth_date date
);


--
-- Name: employees_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.employees_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: employees_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.employees_id_seq OWNED BY public.employees.id;


--
-- Name: fiscal_periods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.fiscal_periods (
    id integer NOT NULL,
    company_id integer NOT NULL,
    period_name character varying(100) NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    is_closed boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: fiscal_periods_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.fiscal_periods_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: fiscal_periods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.fiscal_periods_id_seq OWNED BY public.fiscal_periods.id;


--
-- Name: journal_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.journal_entries (
    id integer NOT NULL,
    reference character varying(100) NOT NULL,
    entry_date date NOT NULL,
    description text,
    transaction_type_id integer,
    fiscal_period_id integer NOT NULL,
    company_id integer NOT NULL,
    created_by character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: journal_entries_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.journal_entries_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: journal_entries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.journal_entries_id_seq OWNED BY public.journal_entries.id;


--
-- Name: journal_entry_lines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.journal_entry_lines (
    id integer NOT NULL,
    journal_entry_id integer NOT NULL,
    account_id integer NOT NULL,
    debit_amount numeric(15,2) DEFAULT 0.00,
    credit_amount numeric(15,2) DEFAULT 0.00,
    description text,
    reference character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    source_transaction_id integer,
    CONSTRAINT journal_entry_lines_check CHECK ((((debit_amount > (0)::numeric) AND (credit_amount = (0)::numeric)) OR ((credit_amount > (0)::numeric) AND (debit_amount = (0)::numeric))))
);


--
-- Name: journal_entry_lines_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.journal_entry_lines_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: journal_entry_lines_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.journal_entry_lines_id_seq OWNED BY public.journal_entry_lines.id;


--
-- Name: manual_invoices; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.manual_invoices (
    id integer NOT NULL,
    company_id integer NOT NULL,
    invoice_number character varying(100) NOT NULL,
    invoice_date date NOT NULL,
    description text,
    amount numeric(15,2) NOT NULL,
    debit_account_id integer NOT NULL,
    credit_account_id integer NOT NULL,
    fiscal_period_id integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: manual_invoices_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.manual_invoices_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: manual_invoices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.manual_invoices_id_seq OWNED BY public.manual_invoices.id;


--
-- Name: payroll_journal_entries; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payroll_journal_entries (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    payroll_period_id bigint NOT NULL,
    journal_entry_id bigint,
    entry_type character varying(20) NOT NULL,
    description text,
    total_amount numeric(15,2) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100)
);


--
-- Name: payroll_journal_entries_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payroll_journal_entries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payroll_journal_entries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payroll_journal_entries_id_seq OWNED BY public.payroll_journal_entries.id;


--
-- Name: payroll_periods; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payroll_periods (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    fiscal_period_id bigint,
    period_name character varying(100) NOT NULL,
    pay_date date NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    period_type character varying(20) DEFAULT 'MONTHLY'::character varying,
    status character varying(20) DEFAULT 'OPEN'::character varying,
    total_gross_pay numeric(15,2) DEFAULT 0,
    total_deductions numeric(15,2) DEFAULT 0,
    total_net_pay numeric(15,2) DEFAULT 0,
    employee_count integer DEFAULT 0,
    processed_at timestamp with time zone,
    processed_by character varying(100),
    approved_at timestamp with time zone,
    approved_by character varying(100),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100)
);


--
-- Name: payroll_periods_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payroll_periods_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payroll_periods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payroll_periods_id_seq OWNED BY public.payroll_periods.id;


--
-- Name: payslips; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payslips (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    payroll_period_id bigint NOT NULL,
    payslip_number character varying(50) NOT NULL,
    basic_salary numeric(15,2) NOT NULL,
    overtime_hours numeric(8,2) DEFAULT 0,
    overtime_amount numeric(15,2) DEFAULT 0,
    gross_salary numeric(15,2) NOT NULL,
    housing_allowance numeric(15,2) DEFAULT 0,
    transport_allowance numeric(15,2) DEFAULT 0,
    medical_allowance numeric(15,2) DEFAULT 0,
    other_allowances numeric(15,2) DEFAULT 0,
    commission numeric(15,2) DEFAULT 0,
    bonus numeric(15,2) DEFAULT 0,
    total_earnings numeric(15,2) NOT NULL,
    paye_tax numeric(15,2) DEFAULT 0,
    uif_employee numeric(15,2) DEFAULT 0,
    uif_employer numeric(15,2) DEFAULT 0,
    medical_aid numeric(15,2) DEFAULT 0,
    pension_fund numeric(15,2) DEFAULT 0,
    loan_deduction numeric(15,2) DEFAULT 0,
    other_deductions numeric(15,2) DEFAULT 0,
    total_deductions numeric(15,2) NOT NULL,
    net_pay numeric(15,2) NOT NULL,
    annual_gross numeric(15,2),
    annual_paye numeric(15,2),
    annual_uif numeric(15,2),
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    payment_method character varying(20) DEFAULT 'EFT'::character varying,
    payment_date date,
    payment_reference character varying(100),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100)
);


--
-- Name: payslips_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payslips_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payslips_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payslips_id_seq OWNED BY public.payslips.id;


--
-- Name: tax_brackets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tax_brackets (
    id bigint NOT NULL,
    tax_configuration_id bigint NOT NULL,
    bracket_order integer NOT NULL,
    min_amount numeric(15,2) NOT NULL,
    max_amount numeric(15,2),
    rate numeric(5,4) NOT NULL,
    cumulative_tax numeric(15,2) DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: tax_brackets_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tax_brackets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tax_brackets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tax_brackets_id_seq OWNED BY public.tax_brackets.id;


--
-- Name: tax_configurations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tax_configurations (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    tax_year integer NOT NULL,
    tax_rebate_primary numeric(15,2) DEFAULT 17235 NOT NULL,
    tax_rebate_secondary numeric(15,2) DEFAULT 9444 NOT NULL,
    tax_rebate_tertiary numeric(15,2) DEFAULT 3145 NOT NULL,
    uif_rate numeric(5,4) DEFAULT 0.0100 NOT NULL,
    uif_max_earnings numeric(15,2) DEFAULT 17712 NOT NULL,
    sdl_rate numeric(5,4) DEFAULT 0.0100 NOT NULL,
    sdl_threshold numeric(15,2) DEFAULT 500000 NOT NULL,
    is_active boolean DEFAULT true,
    effective_from date NOT NULL,
    effective_to date,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100)
);


--
-- Name: tax_configurations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tax_configurations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tax_configurations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tax_configurations_id_seq OWNED BY public.tax_configurations.id;


--
-- Name: transaction_mapping_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transaction_mapping_rules (
    id integer NOT NULL,
    company_id integer NOT NULL,
    rule_name character varying(255) NOT NULL,
    description text,
    match_type character varying(20) NOT NULL,
    match_value text NOT NULL,
    account_id integer NOT NULL,
    is_active boolean DEFAULT true,
    priority integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    pattern_text text,
    CONSTRAINT transaction_mapping_rules_match_type_check CHECK (((match_type)::text = ANY ((ARRAY['CONTAINS'::character varying, 'STARTS_WITH'::character varying, 'ENDS_WITH'::character varying, 'EQUALS'::character varying, 'REGEX'::character varying])::text[])))
);


--
-- Name: transaction_mapping_rules_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.transaction_mapping_rules_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: transaction_mapping_rules_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.transaction_mapping_rules_id_seq OWNED BY public.transaction_mapping_rules.id;


--
-- Name: transaction_types; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transaction_types (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    company_id integer NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


--
-- Name: transaction_types_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.transaction_types_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: transaction_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.transaction_types_id_seq OWNED BY public.transaction_types.id;


--
-- Name: v_employee_summary; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_employee_summary AS
 SELECT e.id,
    e.company_id,
    e.employee_number,
    (((COALESCE(((e.title)::text || ' '::text), ''::text) || (COALESCE(COALESCE(e.known_as_name, e.first_name), ''::character varying))::text) || COALESCE((' '::text || (e.second_name)::text), ''::text)) || COALESCE((' '::text || (e.last_name)::text), ''::text)) AS full_name,
    e."position",
    e.department,
    e.basic_salary,
    e.employment_type,
    e.hire_date,
    e.is_active,
    c.name AS company_name
   FROM (public.employees e
     JOIN public.companies c ON ((e.company_id = c.id)))
  WHERE (e.is_active = true);


--
-- Name: v_payslip_summary; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_payslip_summary AS
 SELECT p.id,
    p.payslip_number,
    p.company_id,
    pp.period_name,
    pp.pay_date,
    e.employee_number,
    (((COALESCE(((e.title)::text || ' '::text), ''::text) || (COALESCE(COALESCE(e.known_as_name, e.first_name), ''::character varying))::text) || COALESCE((' '::text || (e.second_name)::text), ''::text)) || COALESCE((' '::text || (e.last_name)::text), ''::text)) AS employee_name,
    p.total_earnings,
    p.total_deductions,
    p.net_pay,
    p.status
   FROM ((public.payslips p
     JOIN public.employees e ON ((p.employee_id = e.id)))
     JOIN public.payroll_periods pp ON ((p.payroll_period_id = pp.id)));


--
-- Name: account_categories id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_categories ALTER COLUMN id SET DEFAULT nextval('public.account_categories_id_seq'::regclass);


--
-- Name: account_types id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_types ALTER COLUMN id SET DEFAULT nextval('public.account_types_id_seq'::regclass);


--
-- Name: accounts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts ALTER COLUMN id SET DEFAULT nextval('public.accounts_id_seq'::regclass);


--
-- Name: bank_accounts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bank_accounts ALTER COLUMN id SET DEFAULT nextval('public.bank_accounts_id_seq'::regclass);


--
-- Name: bank_transactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bank_transactions ALTER COLUMN id SET DEFAULT nextval('public.bank_transactions_id_seq'::regclass);


--
-- Name: benefits id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.benefits ALTER COLUMN id SET DEFAULT nextval('public.benefits_id_seq'::regclass);


--
-- Name: companies id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies ALTER COLUMN id SET DEFAULT nextval('public.companies_id_seq'::regclass);


--
-- Name: company_classification_rules id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_classification_rules ALTER COLUMN id SET DEFAULT nextval('public.company_classification_rules_id_seq'::regclass);


--
-- Name: data_corrections id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_corrections ALTER COLUMN id SET DEFAULT nextval('public.data_corrections_id_seq'::regclass);


--
-- Name: deductions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deductions ALTER COLUMN id SET DEFAULT nextval('public.deductions_id_seq'::regclass);


--
-- Name: employee_leave id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employee_leave ALTER COLUMN id SET DEFAULT nextval('public.employee_leave_id_seq'::regclass);


--
-- Name: employees id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employees ALTER COLUMN id SET DEFAULT nextval('public.employees_id_seq'::regclass);


--
-- Name: fiscal_periods id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fiscal_periods ALTER COLUMN id SET DEFAULT nextval('public.fiscal_periods_id_seq'::regclass);


--
-- Name: journal_entries id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journal_entries ALTER COLUMN id SET DEFAULT nextval('public.journal_entries_id_seq'::regclass);


--
-- Name: journal_entry_lines id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journal_entry_lines ALTER COLUMN id SET DEFAULT nextval('public.journal_entry_lines_id_seq'::regclass);


--
-- Name: manual_invoices id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manual_invoices ALTER COLUMN id SET DEFAULT nextval('public.manual_invoices_id_seq'::regclass);


--
-- Name: payroll_journal_entries id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payroll_journal_entries ALTER COLUMN id SET DEFAULT nextval('public.payroll_journal_entries_id_seq'::regclass);


--
-- Name: payroll_periods id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payroll_periods ALTER COLUMN id SET DEFAULT nextval('public.payroll_periods_id_seq'::regclass);


--
-- Name: payslips id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payslips ALTER COLUMN id SET DEFAULT nextval('public.payslips_id_seq'::regclass);


--
-- Name: tax_brackets id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_brackets ALTER COLUMN id SET DEFAULT nextval('public.tax_brackets_id_seq'::regclass);


--
-- Name: tax_configurations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_configurations ALTER COLUMN id SET DEFAULT nextval('public.tax_configurations_id_seq'::regclass);


--
-- Name: transaction_mapping_rules id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction_mapping_rules ALTER COLUMN id SET DEFAULT nextval('public.transaction_mapping_rules_id_seq'::regclass);


--
-- Name: transaction_types id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction_types ALTER COLUMN id SET DEFAULT nextval('public.transaction_types_id_seq'::regclass);


--
-- Name: account_categories account_categories_company_id_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_categories
    ADD CONSTRAINT account_categories_company_id_name_key UNIQUE (company_id, name);


--
-- Name: account_categories account_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_categories
    ADD CONSTRAINT account_categories_pkey PRIMARY KEY (id);


--
-- Name: account_types account_types_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_types
    ADD CONSTRAINT account_types_code_key UNIQUE (code);


--
-- Name: account_types account_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_types
    ADD CONSTRAINT account_types_pkey PRIMARY KEY (id);


--
-- Name: accounts accounts_company_id_account_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_company_id_account_code_key UNIQUE (company_id, account_code);


--
-- Name: accounts accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);


--
-- Name: bank_accounts bank_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT bank_accounts_pkey PRIMARY KEY (id);


--
-- Name: bank_transactions bank_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT bank_transactions_pkey PRIMARY KEY (id);


--
-- Name: benefits benefits_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.benefits
    ADD CONSTRAINT benefits_pkey PRIMARY KEY (id);


--
-- Name: companies companies_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_pkey PRIMARY KEY (id);


--
-- Name: company_classification_rules company_classification_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_classification_rules
    ADD CONSTRAINT company_classification_rules_pkey PRIMARY KEY (id);


--
-- Name: data_corrections data_corrections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_corrections
    ADD CONSTRAINT data_corrections_pkey PRIMARY KEY (id);


--
-- Name: deductions deductions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deductions
    ADD CONSTRAINT deductions_pkey PRIMARY KEY (id);


--
-- Name: employee_leave employee_leave_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employee_leave
    ADD CONSTRAINT employee_leave_pkey PRIMARY KEY (id);


--
-- Name: employees employees_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_email_key UNIQUE (email);


--
-- Name: employees employees_employee_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_employee_number_key UNIQUE (employee_number);


--
-- Name: employees employees_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_pkey PRIMARY KEY (id);


--
-- Name: fiscal_periods fiscal_periods_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fiscal_periods
    ADD CONSTRAINT fiscal_periods_pkey PRIMARY KEY (id);


--
-- Name: journal_entries journal_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT journal_entries_pkey PRIMARY KEY (id);


--
-- Name: journal_entry_lines journal_entry_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journal_entry_lines
    ADD CONSTRAINT journal_entry_lines_pkey PRIMARY KEY (id);


--
-- Name: manual_invoices manual_invoices_company_id_invoice_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manual_invoices
    ADD CONSTRAINT manual_invoices_company_id_invoice_number_key UNIQUE (company_id, invoice_number);


--
-- Name: manual_invoices manual_invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manual_invoices
    ADD CONSTRAINT manual_invoices_pkey PRIMARY KEY (id);


--
-- Name: payroll_journal_entries payroll_journal_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payroll_journal_entries
    ADD CONSTRAINT payroll_journal_entries_pkey PRIMARY KEY (id);


--
-- Name: payroll_periods payroll_periods_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payroll_periods
    ADD CONSTRAINT payroll_periods_pkey PRIMARY KEY (id);


--
-- Name: payslips payslips_payslip_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_payslip_number_key UNIQUE (payslip_number);


--
-- Name: payslips payslips_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_pkey PRIMARY KEY (id);


--
-- Name: tax_brackets tax_brackets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_brackets
    ADD CONSTRAINT tax_brackets_pkey PRIMARY KEY (id);


--
-- Name: tax_configurations tax_configurations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_configurations
    ADD CONSTRAINT tax_configurations_pkey PRIMARY KEY (id);


--
-- Name: transaction_mapping_rules transaction_mapping_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction_mapping_rules
    ADD CONSTRAINT transaction_mapping_rules_pkey PRIMARY KEY (id);


--
-- Name: transaction_types transaction_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction_types
    ADD CONSTRAINT transaction_types_pkey PRIMARY KEY (id);


--
-- Name: idx_accounts_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_accounts_category ON public.accounts USING btree (category_id);


--
-- Name: idx_accounts_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_accounts_company ON public.accounts USING btree (company_id, is_active);


--
-- Name: idx_accounts_parent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_accounts_parent ON public.accounts USING btree (parent_account_id) WHERE (parent_account_id IS NOT NULL);


--
-- Name: idx_bank_transactions_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bank_transactions_account ON public.bank_transactions USING btree (account_code, company_id);


--
-- Name: idx_bank_transactions_company_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bank_transactions_company_date ON public.bank_transactions USING btree (company_id, transaction_date);


--
-- Name: idx_bank_transactions_fiscal_period; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_bank_transactions_fiscal_period ON public.bank_transactions USING btree (fiscal_period_id);


--
-- Name: idx_benefits_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_benefits_company ON public.benefits USING btree (company_id);


--
-- Name: idx_benefits_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_benefits_company_id ON public.benefits USING btree (company_id);


--
-- Name: idx_benefits_employee_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_benefits_employee_id ON public.benefits USING btree (employee_id);


--
-- Name: idx_benefits_is_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_benefits_is_active ON public.benefits USING btree (is_active);


--
-- Name: idx_classification_rules_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_classification_rules_company ON public.company_classification_rules USING btree (company_id, account_code);


--
-- Name: idx_classification_rules_pattern; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_classification_rules_pattern ON public.company_classification_rules USING btree (company_id, pattern);


--
-- Name: idx_classification_rules_usage; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_classification_rules_usage ON public.company_classification_rules USING btree (company_id, usage_count DESC);


--
-- Name: idx_deductions_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_deductions_company ON public.deductions USING btree (company_id);


--
-- Name: idx_deductions_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_deductions_company_id ON public.deductions USING btree (company_id);


--
-- Name: idx_deductions_employee_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_deductions_employee_id ON public.deductions USING btree (employee_id);


--
-- Name: idx_deductions_is_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_deductions_is_active ON public.deductions USING btree (is_active);


--
-- Name: idx_employees_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_employees_company_id ON public.employees USING btree (company_id);


--
-- Name: idx_employees_employee_number; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_employees_employee_number ON public.employees USING btree (employee_number);


--
-- Name: idx_employees_is_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_employees_is_active ON public.employees USING btree (is_active);


--
-- Name: idx_journal_entries_company_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_journal_entries_company_date ON public.journal_entries USING btree (company_id, entry_date);


--
-- Name: idx_journal_entry_lines_account; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_journal_entry_lines_account ON public.journal_entry_lines USING btree (account_id);


--
-- Name: idx_mapping_rules_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_mapping_rules_company ON public.transaction_mapping_rules USING btree (company_id);


--
-- Name: idx_payroll_periods_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payroll_periods_company_id ON public.payroll_periods USING btree (company_id);


--
-- Name: idx_payroll_periods_pay_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payroll_periods_pay_date ON public.payroll_periods USING btree (pay_date);


--
-- Name: idx_payroll_periods_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payroll_periods_status ON public.payroll_periods USING btree (status);


--
-- Name: idx_payslips_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payslips_company_id ON public.payslips USING btree (company_id);


--
-- Name: idx_payslips_employee_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payslips_employee_id ON public.payslips USING btree (employee_id);


--
-- Name: idx_payslips_payroll_period_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payslips_payroll_period_id ON public.payslips USING btree (payroll_period_id);


--
-- Name: idx_payslips_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payslips_status ON public.payslips USING btree (status);


--
-- Name: idx_tax_configurations_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tax_configurations_company ON public.tax_configurations USING btree (company_id);


--
-- Name: idx_transaction_mapping_rules_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transaction_mapping_rules_company ON public.transaction_mapping_rules USING btree (company_id, is_active, priority);


--
-- Name: idx_transaction_types_company; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transaction_types_company ON public.transaction_types USING btree (company_id, is_active);


--
-- Name: account_categories update_account_categories_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_account_categories_updated_at BEFORE UPDATE ON public.account_categories FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: accounts update_accounts_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON public.accounts FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: journal_entries update_journal_entries_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_journal_entries_updated_at BEFORE UPDATE ON public.journal_entries FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: manual_invoices update_manual_invoices_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_manual_invoices_updated_at BEFORE UPDATE ON public.manual_invoices FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: transaction_mapping_rules update_transaction_mapping_rules_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER update_transaction_mapping_rules_updated_at BEFORE UPDATE ON public.transaction_mapping_rules FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();


--
-- Name: account_categories account_categories_account_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_categories
    ADD CONSTRAINT account_categories_account_type_id_fkey FOREIGN KEY (account_type_id) REFERENCES public.account_types(id) ON DELETE RESTRICT;


--
-- Name: account_categories account_categories_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.account_categories
    ADD CONSTRAINT account_categories_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: accounts accounts_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.account_categories(id) ON DELETE RESTRICT;


--
-- Name: accounts accounts_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: accounts accounts_parent_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_parent_account_id_fkey FOREIGN KEY (parent_account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- Name: bank_accounts bank_accounts_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT bank_accounts_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: bank_transactions bank_transactions_bank_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT bank_transactions_bank_account_id_fkey FOREIGN KEY (bank_account_id) REFERENCES public.bank_accounts(id) ON DELETE SET NULL;


--
-- Name: bank_transactions bank_transactions_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT bank_transactions_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: bank_transactions bank_transactions_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT bank_transactions_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id) ON DELETE RESTRICT;


--
-- Name: benefits benefits_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.benefits
    ADD CONSTRAINT benefits_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: benefits benefits_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.benefits
    ADD CONSTRAINT benefits_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- Name: data_corrections data_corrections_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_corrections
    ADD CONSTRAINT data_corrections_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: data_corrections data_corrections_new_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_corrections
    ADD CONSTRAINT data_corrections_new_account_id_fkey FOREIGN KEY (new_account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- Name: data_corrections data_corrections_original_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_corrections
    ADD CONSTRAINT data_corrections_original_account_id_fkey FOREIGN KEY (original_account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- Name: data_corrections data_corrections_transaction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_corrections
    ADD CONSTRAINT data_corrections_transaction_id_fkey FOREIGN KEY (transaction_id) REFERENCES public.bank_transactions(id) ON DELETE CASCADE;


--
-- Name: deductions deductions_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deductions
    ADD CONSTRAINT deductions_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: deductions deductions_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.deductions
    ADD CONSTRAINT deductions_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- Name: employee_leave employee_leave_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employee_leave
    ADD CONSTRAINT employee_leave_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: employee_leave employee_leave_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employee_leave
    ADD CONSTRAINT employee_leave_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- Name: employees employees_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: fiscal_periods fiscal_periods_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.fiscal_periods
    ADD CONSTRAINT fiscal_periods_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: company_classification_rules fk_classification_company; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.company_classification_rules
    ADD CONSTRAINT fk_classification_company FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: journal_entries journal_entries_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT journal_entries_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: journal_entries journal_entries_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT journal_entries_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id) ON DELETE RESTRICT;


--
-- Name: journal_entries journal_entries_transaction_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT journal_entries_transaction_type_id_fkey FOREIGN KEY (transaction_type_id) REFERENCES public.transaction_types(id) ON DELETE SET NULL;


--
-- Name: journal_entry_lines journal_entry_lines_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journal_entry_lines
    ADD CONSTRAINT journal_entry_lines_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- Name: journal_entry_lines journal_entry_lines_journal_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journal_entry_lines
    ADD CONSTRAINT journal_entry_lines_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id) ON DELETE CASCADE;


--
-- Name: journal_entry_lines journal_entry_lines_source_transaction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journal_entry_lines
    ADD CONSTRAINT journal_entry_lines_source_transaction_id_fkey FOREIGN KEY (source_transaction_id) REFERENCES public.bank_transactions(id);


--
-- Name: manual_invoices manual_invoices_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manual_invoices
    ADD CONSTRAINT manual_invoices_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: manual_invoices manual_invoices_credit_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manual_invoices
    ADD CONSTRAINT manual_invoices_credit_account_id_fkey FOREIGN KEY (credit_account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- Name: manual_invoices manual_invoices_debit_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manual_invoices
    ADD CONSTRAINT manual_invoices_debit_account_id_fkey FOREIGN KEY (debit_account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- Name: manual_invoices manual_invoices_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.manual_invoices
    ADD CONSTRAINT manual_invoices_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id) ON DELETE RESTRICT;


--
-- Name: payroll_journal_entries payroll_journal_entries_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payroll_journal_entries
    ADD CONSTRAINT payroll_journal_entries_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: payroll_journal_entries payroll_journal_entries_journal_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payroll_journal_entries
    ADD CONSTRAINT payroll_journal_entries_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id);


--
-- Name: payroll_journal_entries payroll_journal_entries_payroll_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payroll_journal_entries
    ADD CONSTRAINT payroll_journal_entries_payroll_period_id_fkey FOREIGN KEY (payroll_period_id) REFERENCES public.payroll_periods(id);


--
-- Name: payroll_periods payroll_periods_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payroll_periods
    ADD CONSTRAINT payroll_periods_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: payroll_periods payroll_periods_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payroll_periods
    ADD CONSTRAINT payroll_periods_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id);


--
-- Name: payslips payslips_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: payslips payslips_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- Name: payslips payslips_payroll_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_payroll_period_id_fkey FOREIGN KEY (payroll_period_id) REFERENCES public.payroll_periods(id);


--
-- Name: tax_brackets tax_brackets_tax_configuration_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_brackets
    ADD CONSTRAINT tax_brackets_tax_configuration_id_fkey FOREIGN KEY (tax_configuration_id) REFERENCES public.tax_configurations(id);


--
-- Name: tax_configurations tax_configurations_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tax_configurations
    ADD CONSTRAINT tax_configurations_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: transaction_mapping_rules transaction_mapping_rules_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction_mapping_rules
    ADD CONSTRAINT transaction_mapping_rules_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id) ON DELETE CASCADE;


--
-- Name: transaction_mapping_rules transaction_mapping_rules_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction_mapping_rules
    ADD CONSTRAINT transaction_mapping_rules_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: transaction_types transaction_types_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction_types
    ADD CONSTRAINT transaction_types_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: assets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.assets (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    asset_code character varying(50) NOT NULL,
    asset_name character varying(255) NOT NULL,
    description text,
    asset_category character varying(100),
    asset_subcategory character varying(100),
    acquisition_date date NOT NULL,
    cost numeric(15,2) NOT NULL,
    salvage_value numeric(15,2) DEFAULT 0,
    useful_life_years integer NOT NULL,
    location character varying(255),
    department character varying(100),
    assigned_to character varying(255),
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    disposal_date date,
    disposal_value numeric(15,2),
    account_code character varying(20),
    accumulated_depreciation numeric(15,2) DEFAULT 0,
    current_book_value numeric(15,2),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100),
    updated_by character varying(100)
);


--
-- Name: assets_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.assets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: assets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.assets_id_seq OWNED BY public.assets.id;


--
-- Name: assets id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assets ALTER COLUMN id SET DEFAULT nextval('public.assets_id_seq'::regclass);


--
-- Name: assets assets_asset_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assets
    ADD CONSTRAINT assets_asset_code_key UNIQUE (asset_code);


--
-- Name: assets assets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assets
    ADD CONSTRAINT assets_pkey PRIMARY KEY (id);


--
-- Name: idx_assets_asset_code; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assets_asset_code ON public.assets USING btree (asset_code);


--
-- Name: idx_assets_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assets_category ON public.assets USING btree (asset_category);


--
-- Name: idx_assets_company_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assets_company_id ON public.assets USING btree (company_id);


--
-- Name: idx_assets_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_assets_status ON public.assets USING btree (status);


--
-- Name: assets assets_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.assets
    ADD CONSTRAINT assets_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- PostgreSQL database dump complete
--

