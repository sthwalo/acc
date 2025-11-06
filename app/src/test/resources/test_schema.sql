--
-- PostgreSQL database dump
--

\restrict mil14gCPvzaZMzvc70nLdU5TGfW77UbJef7FxCv0JM6DjPZrNAJ3bwk8vzZKui4

-- Dumped from database version 17.6 (Homebrew)
-- Dumped by pg_dump version 17.6 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

DROP DATABASE IF EXISTS drimacc_db;
--
-- Name: drimacc_db; Type: DATABASE; Schema: -; Owner: sthwalonyoni
--

CREATE DATABASE drimacc_db WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE_PROVIDER = libc LOCALE = 'en_US.UTF-8';


ALTER DATABASE drimacc_db OWNER TO sthwalonyoni;

\unrestrict mil14gCPvzaZMzvc70nLdU5TGfW77UbJef7FxCv0JM6DjPZrNAJ3bwk8vzZKui4
\connect drimacc_db
\restrict mil14gCPvzaZMzvc70nLdU5TGfW77UbJef7FxCv0JM6DjPZrNAJ3bwk8vzZKui4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: get_closing_balance(bigint, bigint); Type: FUNCTION; Schema: public; Owner: sthwalonyoni
--

CREATE FUNCTION public.get_closing_balance(p_company_id bigint, p_fiscal_period_id bigint) RETURNS numeric
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


ALTER FUNCTION public.get_closing_balance(p_company_id bigint, p_fiscal_period_id bigint) OWNER TO sthwalonyoni;

--
-- Name: get_net_movement(bigint, bigint); Type: FUNCTION; Schema: public; Owner: sthwalonyoni
--

CREATE FUNCTION public.get_net_movement(p_company_id bigint, p_fiscal_period_id bigint) RETURNS TABLE(total_credits numeric, total_debits numeric, net_movement numeric, calculated_closing numeric, actual_closing numeric, difference numeric)
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
        (v_opening + SUM(COALESCE(bt.credit_amount, 0)) - SUM(COALESCE(bt.debit_amount, 0)) - get_closing_balance(p_company_id, p_fiscal_period_id))::NUMERIC as difference
    FROM bank_transactions bt
    WHERE bt.company_id = p_company_id
      AND bt.transaction_date >= v_start_date
      AND bt.transaction_date <= v_end_date
      AND bt.details NOT ILIKE '%balance%brought%forward%';  -- Exclude opening balance transaction
END;
$$;


ALTER FUNCTION public.get_net_movement(p_company_id bigint, p_fiscal_period_id bigint) OWNER TO sthwalonyoni;

--
-- Name: get_opening_balance(bigint, bigint); Type: FUNCTION; Schema: public; Owner: sthwalonyoni
--

CREATE FUNCTION public.get_opening_balance(p_company_id bigint, p_fiscal_period_id bigint) RETURNS numeric
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
    
    -- Get the opening balance from the first transaction on the start date
    SELECT balance INTO v_opening_balance
    FROM bank_transactions
    WHERE company_id = p_company_id
      AND transaction_date = v_start_date
      AND details ILIKE '%balance%brought%forward%'
    LIMIT 1;
    
    -- If no explicit opening balance transaction, calculate from previous period
    IF v_opening_balance IS NULL THEN
        SELECT COALESCE(
            (SELECT balance 
             FROM bank_transactions 
             WHERE company_id = p_company_id 
               AND transaction_date < v_start_date
             ORDER BY transaction_date DESC, id DESC 
             LIMIT 1),
            0
        ) INTO v_opening_balance;
    END IF;
    
    RETURN COALESCE(v_opening_balance, 0);
END;
$$;


ALTER FUNCTION public.get_opening_balance(p_company_id bigint, p_fiscal_period_id bigint) OWNER TO sthwalonyoni;

--
-- Name: update_updated_at_column(); Type: FUNCTION; Schema: public; Owner: sthwalonyoni
--

CREATE FUNCTION public.update_updated_at_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_updated_at_column() OWNER TO sthwalonyoni;

--
-- Name: verify_balance_continuity(bigint, bigint); Type: FUNCTION; Schema: public; Owner: sthwalonyoni
--

CREATE FUNCTION public.verify_balance_continuity(p_company_id bigint, p_fiscal_period_id bigint) RETURNS TABLE(current_period_opening numeric, previous_period_closing numeric, difference numeric, is_continuous boolean)
    LANGUAGE plpgsql STABLE
    AS $$
DECLARE
    v_current_opening NUMERIC;
    v_previous_closing NUMERIC;
    v_previous_period_id BIGINT;
    v_start_date DATE;
BEGIN
    -- Get current period opening balance
    v_current_opening := get_opening_balance(p_company_id, p_fiscal_period_id);
    
    -- Get the previous fiscal period
    SELECT start_date INTO v_start_date
    FROM fiscal_periods
    WHERE id = p_fiscal_period_id;
    
    SELECT id INTO v_previous_period_id
    FROM fiscal_periods
    WHERE company_id = p_company_id
      AND end_date < v_start_date
    ORDER BY end_date DESC
    LIMIT 1;
    
    -- Get previous period closing balance
    IF v_previous_period_id IS NOT NULL THEN
        v_previous_closing := get_closing_balance(p_company_id, v_previous_period_id);
    ELSE
        v_previous_closing := 0;
    END IF;
    
    RETURN QUERY SELECT 
        v_current_opening,
        v_previous_closing,
        v_current_opening - v_previous_closing,
        ABS(v_current_opening - v_previous_closing) < 0.01;
END;
$$;


ALTER FUNCTION public.verify_balance_continuity(p_company_id bigint, p_fiscal_period_id bigint) OWNER TO sthwalonyoni;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: account_categories; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.account_categories OWNER TO sthwalonyoni;

--
-- Name: account_categories_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.account_categories_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.account_categories_id_seq OWNER TO sthwalonyoni;

--
-- Name: account_categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.account_categories_id_seq OWNED BY public.account_categories.id;


--
-- Name: account_types; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.account_types OWNER TO sthwalonyoni;

--
-- Name: account_types_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.account_types_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.account_types_id_seq OWNER TO sthwalonyoni;

--
-- Name: account_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.account_types_id_seq OWNED BY public.account_types.id;


--
-- Name: accounts; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.accounts OWNER TO sthwalonyoni;

--
-- Name: accounts_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.accounts_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.accounts_id_seq OWNER TO sthwalonyoni;

--
-- Name: accounts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.accounts_id_seq OWNED BY public.accounts.id;


--
-- Name: asset_disposals; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.asset_disposals (
    id bigint NOT NULL,
    asset_id bigint NOT NULL,
    company_id bigint NOT NULL,
    disposal_date date NOT NULL,
    disposal_type character varying(50) NOT NULL,
    proceeds_received numeric(15,2) DEFAULT 0.00,
    tax_value numeric(15,2) NOT NULL,
    loss_on_disposal numeric(15,2) DEFAULT 0.00,
    gain_on_disposal numeric(15,2) DEFAULT 0.00,
    reference character varying(100) NOT NULL,
    description text,
    journal_entry_id bigint,
    created_by character varying(100) DEFAULT 'SYSTEM'::character varying,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT asset_disposals_disposal_type_check CHECK (((disposal_type)::text = ANY ((ARRAY['SALE'::character varying, 'SCRAP'::character varying, 'THEFT'::character varying, 'DONATION'::character varying, 'DESTRUCTION'::character varying])::text[]))),
    CONSTRAINT loss_or_gain_only CHECK ((((loss_on_disposal > (0)::numeric) AND (gain_on_disposal = (0)::numeric)) OR ((gain_on_disposal > (0)::numeric) AND (loss_on_disposal = (0)::numeric)) OR ((loss_on_disposal = (0)::numeric) AND (gain_on_disposal = (0)::numeric)))),
    CONSTRAINT non_negative_gain CHECK ((gain_on_disposal >= (0)::numeric)),
    CONSTRAINT non_negative_loss CHECK ((loss_on_disposal >= (0)::numeric)),
    CONSTRAINT positive_proceeds CHECK ((proceeds_received >= (0)::numeric)),
    CONSTRAINT positive_tax_value CHECK ((tax_value >= (0)::numeric))
);


ALTER TABLE public.asset_disposals OWNER TO sthwalonyoni;

--
-- Name: TABLE asset_disposals; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON TABLE public.asset_disposals IS 'Tracks asset disposals with section 11(o) loss/gain calculations per SARS guidelines';


--
-- Name: COLUMN asset_disposals.tax_value; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON COLUMN public.asset_disposals.tax_value IS 'Asset cost minus accumulated depreciation (tax value for disposal)';


--
-- Name: COLUMN asset_disposals.loss_on_disposal; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON COLUMN public.asset_disposals.loss_on_disposal IS 'Section 11(o) allowance when tax value > proceeds';


--
-- Name: COLUMN asset_disposals.gain_on_disposal; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON COLUMN public.asset_disposals.gain_on_disposal IS 'Recoupment under section 8(4)(a) when proceeds > tax value';


--
-- Name: asset_disposals_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.asset_disposals_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.asset_disposals_id_seq OWNER TO sthwalonyoni;

--
-- Name: asset_disposals_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.asset_disposals_id_seq OWNED BY public.asset_disposals.id;


--
-- Name: assets; Type: TABLE; Schema: public; Owner: sthwalonyoni
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
    updated_by character varying(100),
    CONSTRAINT chk_asset_status CHECK (((status)::text = ANY (ARRAY[('ACTIVE'::character varying)::text, ('DISPOSED'::character varying)::text, ('SOLD'::character varying)::text, ('WRITTEN_OFF'::character varying)::text])))
);


ALTER TABLE public.assets OWNER TO sthwalonyoni;

--
-- Name: assets_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.assets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.assets_id_seq OWNER TO sthwalonyoni;

--
-- Name: assets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.assets_id_seq OWNED BY public.assets.id;


--
-- Name: audit_reports; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.audit_reports (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    audit_opinion character varying(50) NOT NULL,
    opinion_text text NOT NULL,
    basis_for_opinion text,
    key_audit_matters text,
    other_information text,
    responsibilities_management text,
    responsibilities_auditor text,
    auditor_firm_name character varying(255) NOT NULL,
    auditor_firm_registration character varying(100),
    auditor_partner_name character varying(255),
    audit_date date NOT NULL,
    report_date date NOT NULL,
    audit_standard character varying(100) DEFAULT 'ISA'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.audit_reports OWNER TO sthwalonyoni;

--
-- Name: audit_reports_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.audit_reports_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.audit_reports_id_seq OWNER TO sthwalonyoni;

--
-- Name: audit_reports_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.audit_reports_id_seq OWNED BY public.audit_reports.id;


--
-- Name: bank_accounts; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.bank_accounts OWNER TO sthwalonyoni;

--
-- Name: bank_accounts_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.bank_accounts_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.bank_accounts_id_seq OWNER TO sthwalonyoni;

--
-- Name: bank_accounts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.bank_accounts_id_seq OWNED BY public.bank_accounts.id;


--
-- Name: bank_transactions; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.bank_transactions OWNER TO sthwalonyoni;

--
-- Name: bank_transactions_backup_20251003; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.bank_transactions_backup_20251003 (
    id integer,
    company_id integer,
    bank_account_id integer,
    fiscal_period_id integer,
    transaction_date date,
    details text,
    debit_amount numeric(15,2),
    credit_amount numeric(15,2),
    balance numeric(15,2),
    service_fee boolean,
    account_number character varying(50),
    statement_period character varying(50),
    source_file character varying(500),
    created_at timestamp without time zone,
    account_code character varying(10),
    account_name character varying(255),
    classification_date timestamp without time zone,
    classified_by character varying(100)
);


ALTER TABLE public.bank_transactions_backup_20251003 OWNER TO sthwalonyoni;

--
-- Name: bank_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.bank_transactions_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.bank_transactions_id_seq OWNER TO sthwalonyoni;

--
-- Name: bank_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.bank_transactions_id_seq OWNED BY public.bank_transactions.id;


--
-- Name: benefits; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.benefits OWNER TO sthwalonyoni;

--
-- Name: benefits_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.benefits_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.benefits_id_seq OWNER TO sthwalonyoni;

--
-- Name: benefits_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.benefits_id_seq OWNED BY public.benefits.id;


--
-- Name: budget_categories; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.budget_categories (
    id integer NOT NULL,
    budget_id integer NOT NULL,
    name character varying(255) NOT NULL,
    category_type character varying(50) NOT NULL,
    description text,
    allocated_percentage numeric(5,2),
    total_allocated numeric(15,2) DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.budget_categories OWNER TO sthwalonyoni;

--
-- Name: TABLE budget_categories; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON TABLE public.budget_categories IS 'Budget categories (Revenue/Expense) with percentage allocations';


--
-- Name: budget_categories_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.budget_categories_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.budget_categories_id_seq OWNER TO sthwalonyoni;

--
-- Name: budget_categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.budget_categories_id_seq OWNED BY public.budget_categories.id;


--
-- Name: budget_items; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.budget_items (
    id integer NOT NULL,
    budget_category_id integer NOT NULL,
    account_id integer,
    description character varying(255) NOT NULL,
    amount numeric(15,2) DEFAULT 0 NOT NULL,
    notes text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    annual_amount numeric(15,2)
);


ALTER TABLE public.budget_items OWNER TO sthwalonyoni;

--
-- Name: TABLE budget_items; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON TABLE public.budget_items IS 'Individual budget line items linked to chart of accounts with annual totals';


--
-- Name: budget_items_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.budget_items_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.budget_items_id_seq OWNER TO sthwalonyoni;

--
-- Name: budget_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.budget_items_id_seq OWNED BY public.budget_items.id;


--
-- Name: budget_monthly_allocations; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.budget_monthly_allocations (
    id integer NOT NULL,
    budget_item_id integer NOT NULL,
    month_number integer NOT NULL,
    allocated_amount numeric(15,2) DEFAULT 0 NOT NULL,
    actual_amount numeric(15,2) DEFAULT 0,
    variance_amount numeric(15,2) GENERATED ALWAYS AS ((actual_amount - allocated_amount)) STORED,
    notes text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT budget_monthly_allocations_month_number_check CHECK (((month_number >= 1) AND (month_number <= 12)))
);


ALTER TABLE public.budget_monthly_allocations OWNER TO sthwalonyoni;

--
-- Name: TABLE budget_monthly_allocations; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON TABLE public.budget_monthly_allocations IS 'Monthly budget allocations and actual spending with variance tracking';


--
-- Name: budget_monthly_allocations_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.budget_monthly_allocations_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.budget_monthly_allocations_id_seq OWNER TO sthwalonyoni;

--
-- Name: budget_monthly_allocations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.budget_monthly_allocations_id_seq OWNED BY public.budget_monthly_allocations.id;


--
-- Name: budget_projections; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.budget_projections (
    id integer NOT NULL,
    budget_id integer NOT NULL,
    projection_year integer NOT NULL,
    growth_rate numeric(5,2),
    projected_revenue numeric(15,2),
    projected_expenses numeric(15,2),
    assumptions text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.budget_projections OWNER TO sthwalonyoni;

--
-- Name: TABLE budget_projections; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON TABLE public.budget_projections IS 'Multi-year financial projections and growth modeling';


--
-- Name: budget_projections_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.budget_projections_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.budget_projections_id_seq OWNER TO sthwalonyoni;

--
-- Name: budget_projections_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.budget_projections_id_seq OWNED BY public.budget_projections.id;


--
-- Name: budgets; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.budgets (
    id integer NOT NULL,
    company_id integer NOT NULL,
    fiscal_period_id integer,
    title character varying(255) NOT NULL,
    description text,
    budget_year integer NOT NULL,
    status character varying(50) DEFAULT 'DRAFT'::character varying,
    total_revenue numeric(15,2) DEFAULT 0,
    total_expenses numeric(15,2) DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    approved_at timestamp without time zone,
    approved_by character varying(255)
);


ALTER TABLE public.budgets OWNER TO sthwalonyoni;

--
-- Name: TABLE budgets; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON TABLE public.budgets IS 'Annual budgets linked to fiscal periods and companies';


--
-- Name: budgets_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.budgets_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.budgets_id_seq OWNER TO sthwalonyoni;

--
-- Name: budgets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.budgets_id_seq OWNED BY public.budgets.id;


--
-- Name: companies; Type: TABLE; Schema: public; Owner: sthwalonyoni
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
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    logo_path character varying(500),
    bank_name character varying(255),
    account_number character varying(50),
    account_type character varying(50),
    branch_code character varying(20),
    vat_registered boolean DEFAULT false
);


ALTER TABLE public.companies OWNER TO sthwalonyoni;

--
-- Name: companies_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.companies_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.companies_id_seq OWNER TO sthwalonyoni;

--
-- Name: companies_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.companies_id_seq OWNED BY public.companies.id;


--
-- Name: company_classification_rules; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.company_classification_rules (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    pattern character varying(1000) NOT NULL,
    keywords character varying(2000) NOT NULL,
    account_code character varying(20) NOT NULL,
    account_name character varying(255) NOT NULL,
    usage_count integer DEFAULT 1,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    last_used timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.company_classification_rules OWNER TO sthwalonyoni;

--
-- Name: company_classification_rules_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE public.company_classification_rules ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.company_classification_rules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: company_extended_info; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.company_extended_info (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    nature_of_business text,
    directors_names text,
    company_secretary character varying(255),
    business_address text,
    postal_address text,
    auditors_name character varying(255),
    auditors_address text,
    auditors_registration character varying(100),
    bankers_name character varying(255),
    bankers_branch character varying(255),
    legal_advisors character varying(255),
    year_of_incorporation integer,
    country_of_incorporation character varying(100) DEFAULT 'South Africa'::character varying,
    accounting_framework character varying(50) DEFAULT 'IFRS'::character varying,
    functional_currency character varying(10) DEFAULT 'ZAR'::character varying,
    presentation_currency character varying(10) DEFAULT 'ZAR'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.company_extended_info OWNER TO sthwalonyoni;

--
-- Name: company_extended_info_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.company_extended_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.company_extended_info_id_seq OWNER TO sthwalonyoni;

--
-- Name: company_extended_info_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.company_extended_info_id_seq OWNED BY public.company_extended_info.id;


--
-- Name: data_corrections; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.data_corrections OWNER TO sthwalonyoni;

--
-- Name: data_corrections_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.data_corrections_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.data_corrections_id_seq OWNER TO sthwalonyoni;

--
-- Name: data_corrections_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.data_corrections_id_seq OWNED BY public.data_corrections.id;


--
-- Name: deductions; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.deductions OWNER TO sthwalonyoni;

--
-- Name: deductions_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.deductions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.deductions_id_seq OWNER TO sthwalonyoni;

--
-- Name: deductions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.deductions_id_seq OWNED BY public.deductions.id;


--
-- Name: depreciation_adjustments; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.depreciation_adjustments (
    id bigint NOT NULL,
    depreciation_schedule_id bigint NOT NULL,
    depreciation_entry_id bigint,
    adjustment_type character varying(50) NOT NULL,
    adjustment_date date NOT NULL,
    description text,
    previous_amount numeric(15,2),
    adjusted_amount numeric(15,2) NOT NULL,
    adjustment_amount numeric(15,2) NOT NULL,
    approved_by character varying(100),
    approved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100)
);


ALTER TABLE public.depreciation_adjustments OWNER TO sthwalonyoni;

--
-- Name: depreciation_adjustments_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.depreciation_adjustments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.depreciation_adjustments_id_seq OWNER TO sthwalonyoni;

--
-- Name: depreciation_adjustments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.depreciation_adjustments_id_seq OWNED BY public.depreciation_adjustments.id;


--
-- Name: depreciation_entries; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.depreciation_entries (
    id bigint NOT NULL,
    depreciation_schedule_id bigint NOT NULL,
    year_number integer NOT NULL,
    fiscal_year integer,
    period_start date,
    period_end date,
    depreciation_amount numeric(15,2) NOT NULL,
    cumulative_depreciation numeric(15,2) NOT NULL,
    book_value numeric(15,2) NOT NULL,
    status character varying(20) DEFAULT 'CALCULATED'::character varying,
    journal_entry_line_id bigint,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_entry_status CHECK (((status)::text = ANY (ARRAY[('CALCULATED'::character varying)::text, ('POSTED'::character varying)::text, ('ADJUSTED'::character varying)::text])))
);


ALTER TABLE public.depreciation_entries OWNER TO sthwalonyoni;

--
-- Name: depreciation_entries_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.depreciation_entries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.depreciation_entries_id_seq OWNER TO sthwalonyoni;

--
-- Name: depreciation_entries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.depreciation_entries_id_seq OWNED BY public.depreciation_entries.id;


--
-- Name: depreciation_journal_entries_backup; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.depreciation_journal_entries_backup (
    id integer,
    reference character varying(100),
    entry_date date,
    description text,
    transaction_type_id integer,
    fiscal_period_id integer,
    company_id integer,
    created_by character varying(100),
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    period_name character varying(100),
    company_name character varying(255)
);


ALTER TABLE public.depreciation_journal_entries_backup OWNER TO sthwalonyoni;

--
-- Name: depreciation_policies; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.depreciation_policies (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    policy_name character varying(255) NOT NULL,
    policy_code character varying(50) NOT NULL,
    description text,
    default_method character varying(30) DEFAULT 'STRAIGHT_LINE'::character varying,
    default_convention character varying(50),
    default_db_factor numeric(5,2) DEFAULT 2.0,
    asset_category character varying(100),
    min_useful_life integer,
    max_useful_life integer,
    default_useful_life integer,
    is_active boolean DEFAULT true,
    is_default boolean DEFAULT false,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100),
    updated_by character varying(100)
);


ALTER TABLE public.depreciation_policies OWNER TO sthwalonyoni;

--
-- Name: depreciation_policies_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.depreciation_policies_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.depreciation_policies_id_seq OWNER TO sthwalonyoni;

--
-- Name: depreciation_policies_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.depreciation_policies_id_seq OWNED BY public.depreciation_policies.id;


--
-- Name: depreciation_schedules; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.depreciation_schedules (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    asset_id bigint,
    schedule_number character varying(50) NOT NULL,
    schedule_name character varying(255),
    description text,
    cost numeric(15,2) NOT NULL,
    salvage_value numeric(15,2) DEFAULT 0,
    useful_life_years integer NOT NULL,
    depreciation_method character varying(30) NOT NULL,
    db_factor numeric(5,2) DEFAULT 2.0,
    convention character varying(50),
    total_depreciation numeric(15,2) NOT NULL,
    final_book_value numeric(15,2) NOT NULL,
    status character varying(20) DEFAULT 'DRAFT'::character varying,
    calculation_date timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    approved_at timestamp with time zone,
    approved_by character varying(100),
    posted_at timestamp with time zone,
    posted_by character varying(100),
    journal_entry_id bigint,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    created_by character varying(100),
    updated_by character varying(100),
    CONSTRAINT chk_depreciation_method CHECK (((depreciation_method)::text = ANY (ARRAY[('STRAIGHT_LINE'::character varying)::text, ('DECLINING_BALANCE'::character varying)::text, ('FIN'::character varying)::text]))),
    CONSTRAINT chk_schedule_status CHECK (((status)::text = ANY (ARRAY[('DRAFT'::character varying)::text, ('CALCULATED'::character varying)::text, ('APPROVED'::character varying)::text, ('POSTED'::character varying)::text])))
);


ALTER TABLE public.depreciation_schedules OWNER TO sthwalonyoni;

--
-- Name: depreciation_schedules_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.depreciation_schedules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.depreciation_schedules_id_seq OWNER TO sthwalonyoni;

--
-- Name: depreciation_schedules_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.depreciation_schedules_id_seq OWNED BY public.depreciation_schedules.id;


--
-- Name: directors_reports; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.directors_reports (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    nature_of_business text,
    review_of_operations text,
    directors_names text,
    directors_interests text,
    share_capital_info text,
    dividends_info text,
    property_plant_equipment text,
    events_after_reporting_date text,
    going_concern_assessment text,
    secretary_info character varying(255),
    auditors_info character varying(255),
    special_resolutions text,
    report_date date NOT NULL,
    approved_by character varying(255),
    approved_by_title character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.directors_reports OWNER TO sthwalonyoni;

--
-- Name: directors_reports_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.directors_reports_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.directors_reports_id_seq OWNER TO sthwalonyoni;

--
-- Name: directors_reports_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.directors_reports_id_seq OWNED BY public.directors_reports.id;


--
-- Name: directors_responsibility_statements; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.directors_responsibility_statements (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    statement_text text NOT NULL,
    statement_date date NOT NULL,
    approved_by character varying(255),
    approved_by_title character varying(100),
    witness_name character varying(255),
    witness_title character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.directors_responsibility_statements OWNER TO sthwalonyoni;

--
-- Name: directors_responsibility_statements_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.directors_responsibility_statements_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.directors_responsibility_statements_id_seq OWNER TO sthwalonyoni;

--
-- Name: directors_responsibility_statements_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.directors_responsibility_statements_id_seq OWNED BY public.directors_responsibility_statements.id;


--
-- Name: employee_leave; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.employee_leave OWNER TO sthwalonyoni;

--
-- Name: employee_leave_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.employee_leave_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.employee_leave_id_seq OWNER TO sthwalonyoni;

--
-- Name: employee_leave_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.employee_leave_id_seq OWNED BY public.employee_leave.id;


--
-- Name: employees; Type: TABLE; Schema: public; Owner: sthwalonyoni
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
    updated_by character varying(100)
);


ALTER TABLE public.employees OWNER TO sthwalonyoni;

--
-- Name: employees_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.employees_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.employees_id_seq OWNER TO sthwalonyoni;

--
-- Name: employees_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.employees_id_seq OWNED BY public.employees.id;


--
-- Name: equity_movements; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.equity_movements (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    equity_component character varying(100) NOT NULL,
    opening_balance numeric(15,2) DEFAULT 0 NOT NULL,
    profit_loss numeric(15,2) DEFAULT 0,
    dividends numeric(15,2) DEFAULT 0,
    share_issues numeric(15,2) DEFAULT 0,
    share_buybacks numeric(15,2) DEFAULT 0,
    transfers_to_reserves numeric(15,2) DEFAULT 0,
    transfers_from_reserves numeric(15,2) DEFAULT 0,
    other_movements numeric(15,2) DEFAULT 0,
    other_movements_description text,
    closing_balance numeric(15,2) DEFAULT 0 NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.equity_movements OWNER TO sthwalonyoni;

--
-- Name: equity_movements_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.equity_movements_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.equity_movements_id_seq OWNER TO sthwalonyoni;

--
-- Name: equity_movements_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.equity_movements_id_seq OWNED BY public.equity_movements.id;


--
-- Name: financial_notes; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.financial_notes (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    fiscal_period_id bigint NOT NULL,
    note_number character varying(10) NOT NULL,
    note_title character varying(255) NOT NULL,
    note_category character varying(50) NOT NULL,
    note_content text NOT NULL,
    display_order integer DEFAULT 0 NOT NULL,
    is_mandatory boolean DEFAULT false,
    standard_reference character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.financial_notes OWNER TO sthwalonyoni;

--
-- Name: financial_notes_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.financial_notes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.financial_notes_id_seq OWNER TO sthwalonyoni;

--
-- Name: financial_notes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.financial_notes_id_seq OWNED BY public.financial_notes.id;


--
-- Name: fiscal_periods; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.fiscal_periods OWNER TO sthwalonyoni;

--
-- Name: fiscal_periods_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.fiscal_periods_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.fiscal_periods_id_seq OWNER TO sthwalonyoni;

--
-- Name: fiscal_periods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.fiscal_periods_id_seq OWNED BY public.fiscal_periods.id;


--
-- Name: journal_entries; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.journal_entries OWNER TO sthwalonyoni;

--
-- Name: journal_entries_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.journal_entries_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.journal_entries_id_seq OWNER TO sthwalonyoni;

--
-- Name: journal_entries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.journal_entries_id_seq OWNED BY public.journal_entries.id;


--
-- Name: journal_entry_lines; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.journal_entry_lines OWNER TO sthwalonyoni;

--
-- Name: COLUMN journal_entry_lines.source_transaction_id; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON COLUMN public.journal_entry_lines.source_transaction_id IS 'Links journal entry line to the bank transaction that generated it. Used for audit trail and preventing duplicates.';


--
-- Name: journal_entry_lines_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.journal_entry_lines_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.journal_entry_lines_id_seq OWNER TO sthwalonyoni;

--
-- Name: journal_entry_lines_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.journal_entry_lines_id_seq OWNED BY public.journal_entry_lines.id;


--
-- Name: manual_invoices; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.manual_invoices OWNER TO sthwalonyoni;

--
-- Name: manual_invoices_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.manual_invoices_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.manual_invoices_id_seq OWNER TO sthwalonyoni;

--
-- Name: manual_invoices_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.manual_invoices_id_seq OWNED BY public.manual_invoices.id;


--
-- Name: operational_activities; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.operational_activities (
    id integer NOT NULL,
    strategic_plan_id integer NOT NULL,
    month_number integer NOT NULL,
    title character varying(255) NOT NULL,
    activities text NOT NULL,
    responsible_parties character varying(500),
    status character varying(50) DEFAULT 'PLANNED'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT operational_activities_month_number_check CHECK (((month_number >= 1) AND (month_number <= 12)))
);


ALTER TABLE public.operational_activities OWNER TO sthwalonyoni;

--
-- Name: TABLE operational_activities; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON TABLE public.operational_activities IS 'Monthly operational activities and milestones aligned with strategic plan';


--
-- Name: operational_activities_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.operational_activities_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.operational_activities_id_seq OWNER TO sthwalonyoni;

--
-- Name: operational_activities_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.operational_activities_id_seq OWNED BY public.operational_activities.id;


--
-- Name: payroll_journal_entries; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.payroll_journal_entries OWNER TO sthwalonyoni;

--
-- Name: payroll_journal_entries_backup; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.payroll_journal_entries_backup (
    id bigint,
    company_id bigint,
    payroll_period_id bigint,
    journal_entry_id bigint,
    entry_type character varying(20),
    description text,
    total_amount numeric(15,2),
    created_at timestamp with time zone,
    created_by character varying(100),
    reference character varying(100),
    entry_date date,
    company_name character varying(255)
);


ALTER TABLE public.payroll_journal_entries_backup OWNER TO sthwalonyoni;

--
-- Name: payroll_journal_entries_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.payroll_journal_entries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.payroll_journal_entries_id_seq OWNER TO sthwalonyoni;

--
-- Name: payroll_journal_entries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.payroll_journal_entries_id_seq OWNED BY public.payroll_journal_entries.id;


--
-- Name: payroll_periods; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.payroll_periods OWNER TO sthwalonyoni;

--
-- Name: payroll_periods_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.payroll_periods_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.payroll_periods_id_seq OWNER TO sthwalonyoni;

--
-- Name: payroll_periods_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.payroll_periods_id_seq OWNED BY public.payroll_periods.id;


--
-- Name: payslips; Type: TABLE; Schema: public; Owner: sthwalonyoni
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
    created_by character varying(100),
    sdl_levy numeric(15,2) DEFAULT 0
);


ALTER TABLE public.payslips OWNER TO sthwalonyoni;

--
-- Name: payslips_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.payslips_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.payslips_id_seq OWNER TO sthwalonyoni;

--
-- Name: payslips_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.payslips_id_seq OWNED BY public.payslips.id;


--
-- Name: strategic_initiatives; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.strategic_initiatives (
    id integer NOT NULL,
    strategic_priority_id integer NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    start_date date,
    end_date date,
    budget_allocated numeric(15,2) DEFAULT 0,
    status character varying(50) DEFAULT 'PLANNED'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.strategic_initiatives OWNER TO sthwalonyoni;

--
-- Name: TABLE strategic_initiatives; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON TABLE public.strategic_initiatives IS 'Specific initiatives with timelines and budget allocations';


--
-- Name: strategic_initiatives_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.strategic_initiatives_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.strategic_initiatives_id_seq OWNER TO sthwalonyoni;

--
-- Name: strategic_initiatives_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.strategic_initiatives_id_seq OWNED BY public.strategic_initiatives.id;


--
-- Name: strategic_milestones; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.strategic_milestones (
    id integer NOT NULL,
    strategic_initiative_id integer NOT NULL,
    title character varying(255) NOT NULL,
    description text,
    target_date date,
    completion_date date,
    status character varying(50) DEFAULT 'PENDING'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.strategic_milestones OWNER TO sthwalonyoni;

--
-- Name: TABLE strategic_milestones; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON TABLE public.strategic_milestones IS 'Measurable outcomes and KPIs for initiatives';


--
-- Name: strategic_milestones_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.strategic_milestones_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.strategic_milestones_id_seq OWNER TO sthwalonyoni;

--
-- Name: strategic_milestones_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.strategic_milestones_id_seq OWNED BY public.strategic_milestones.id;


--
-- Name: strategic_plans; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.strategic_plans (
    id integer NOT NULL,
    company_id integer NOT NULL,
    title character varying(255) NOT NULL,
    vision_statement text,
    mission_statement text,
    goals text,
    status character varying(50) DEFAULT 'DRAFT'::character varying,
    start_date date,
    end_date date,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.strategic_plans OWNER TO sthwalonyoni;

--
-- Name: TABLE strategic_plans; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON TABLE public.strategic_plans IS 'Strategic plans with vision, mission, and goals for organizations';


--
-- Name: strategic_plans_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.strategic_plans_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.strategic_plans_id_seq OWNER TO sthwalonyoni;

--
-- Name: strategic_plans_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.strategic_plans_id_seq OWNED BY public.strategic_plans.id;


--
-- Name: strategic_priorities; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.strategic_priorities (
    id integer NOT NULL,
    strategic_plan_id integer NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    priority_order integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.strategic_priorities OWNER TO sthwalonyoni;

--
-- Name: TABLE strategic_priorities; Type: COMMENT; Schema: public; Owner: sthwalonyoni
--

COMMENT ON TABLE public.strategic_priorities IS 'Key priority areas within a strategic plan (e.g., Academic Excellence, Infrastructure)';


--
-- Name: strategic_priorities_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.strategic_priorities_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.strategic_priorities_id_seq OWNER TO sthwalonyoni;

--
-- Name: strategic_priorities_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.strategic_priorities_id_seq OWNED BY public.strategic_priorities.id;


--
-- Name: tax_brackets; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.tax_brackets OWNER TO sthwalonyoni;

--
-- Name: tax_brackets_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.tax_brackets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tax_brackets_id_seq OWNER TO sthwalonyoni;

--
-- Name: tax_brackets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.tax_brackets_id_seq OWNED BY public.tax_brackets.id;


--
-- Name: tax_configurations; Type: TABLE; Schema: public; Owner: sthwalonyoni
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


ALTER TABLE public.tax_configurations OWNER TO sthwalonyoni;

--
-- Name: tax_configurations_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.tax_configurations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tax_configurations_id_seq OWNER TO sthwalonyoni;

--
-- Name: tax_configurations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.tax_configurations_id_seq OWNED BY public.tax_configurations.id;


--
-- Name: transaction_mapping_rules; Type: TABLE; Schema: public; Owner: sthwalonyoni
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
    CONSTRAINT transaction_mapping_rules_match_type_check CHECK (((match_type)::text = ANY (ARRAY[('CONTAINS'::character varying)::text, ('STARTS_WITH'::character varying)::text, ('ENDS_WITH'::character varying)::text, ('EQUALS'::character varying)::text, ('REGEX'::character varying)::text])))
);


ALTER TABLE public.transaction_mapping_rules OWNER TO sthwalonyoni;

--
-- Name: transaction_mapping_rules_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.transaction_mapping_rules_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.transaction_mapping_rules_id_seq OWNER TO sthwalonyoni;

--
-- Name: transaction_mapping_rules_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.transaction_mapping_rules_id_seq OWNED BY public.transaction_mapping_rules.id;


--
-- Name: transaction_types; Type: TABLE; Schema: public; Owner: sthwalonyoni
--

CREATE TABLE public.transaction_types (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    company_id integer NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.transaction_types OWNER TO sthwalonyoni;

--
-- Name: transaction_types_id_seq; Type: SEQUENCE; Schema: public; Owner: sthwalonyoni
--

CREATE SEQUENCE public.transaction_types_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.transaction_types_id_seq OWNER TO sthwalonyoni;

--
-- Name: transaction_types_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: sthwalonyoni
--

ALTER SEQUENCE public.transaction_types_id_seq OWNED BY public.transaction_types.id;


--
-- Name: v_asset_depreciation_summary; Type: VIEW; Schema: public; Owner: sthwalonyoni
--

CREATE VIEW public.v_asset_depreciation_summary AS
 SELECT a.id AS asset_id,
    a.asset_code,
    a.asset_name,
    a.asset_category,
    a.cost AS original_cost,
    a.accumulated_depreciation,
    a.current_book_value,
    a.status AS asset_status,
    ds.id AS latest_schedule_id,
    ds.schedule_number,
    ds.total_depreciation,
    ds.final_book_value AS calculated_book_value,
    ds.depreciation_method,
    ds.calculation_date,
    c.name AS company_name
   FROM (((public.assets a
     LEFT JOIN ( SELECT depreciation_schedules.asset_id,
            max(depreciation_schedules.calculation_date) AS latest_calc
           FROM public.depreciation_schedules
          WHERE (((depreciation_schedules.status)::text = 'CALCULATED'::text) OR ((depreciation_schedules.status)::text = 'APPROVED'::text) OR ((depreciation_schedules.status)::text = 'POSTED'::text))
          GROUP BY depreciation_schedules.asset_id) latest ON ((a.id = latest.asset_id)))
     LEFT JOIN public.depreciation_schedules ds ON (((ds.asset_id = a.id) AND (ds.calculation_date = latest.latest_calc))))
     LEFT JOIN public.companies c ON ((a.company_id = c.id)))
  WHERE ((a.status)::text = 'ACTIVE'::text);


ALTER VIEW public.v_asset_depreciation_summary OWNER TO sthwalonyoni;

--
-- Name: v_depreciation_schedule_detail; Type: VIEW; Schema: public; Owner: sthwalonyoni
--

CREATE VIEW public.v_depreciation_schedule_detail AS
 SELECT ds.id AS schedule_id,
    ds.schedule_number,
    ds.schedule_name,
    ds.cost,
    ds.salvage_value,
    ds.useful_life_years,
    ds.depreciation_method,
    ds.total_depreciation,
    ds.final_book_value,
    ds.status AS schedule_status,
    de.year_number,
    de.fiscal_year,
    de.period_start,
    de.period_end,
    de.depreciation_amount,
    de.cumulative_depreciation,
    de.book_value,
    a.asset_code,
    a.asset_name,
    c.name AS company_name
   FROM (((public.depreciation_schedules ds
     LEFT JOIN public.depreciation_entries de ON ((ds.id = de.depreciation_schedule_id)))
     LEFT JOIN public.assets a ON ((ds.asset_id = a.id)))
     LEFT JOIN public.companies c ON ((ds.company_id = c.id)))
  ORDER BY ds.id, de.year_number;


ALTER VIEW public.v_depreciation_schedule_detail OWNER TO sthwalonyoni;

--
-- Name: v_employee_summary; Type: VIEW; Schema: public; Owner: sthwalonyoni
--

CREATE VIEW public.v_employee_summary AS
 SELECT e.id,
    e.company_id,
    e.employee_number,
    (((e.first_name)::text || ' '::text) || (e.last_name)::text) AS full_name,
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


ALTER VIEW public.v_employee_summary OWNER TO sthwalonyoni;

--
-- Name: v_monthly_depreciation; Type: VIEW; Schema: public; Owner: sthwalonyoni
--

CREATE VIEW public.v_monthly_depreciation AS
 SELECT c.name AS company_name,
    date_trunc('month'::text, (de.period_start)::timestamp with time zone) AS depreciation_month,
    count(DISTINCT ds.id) AS schedules_count,
    sum(de.depreciation_amount) AS monthly_depreciation,
    count(DISTINCT a.id) AS assets_count
   FROM (((public.depreciation_entries de
     JOIN public.depreciation_schedules ds ON ((de.depreciation_schedule_id = ds.id)))
     LEFT JOIN public.assets a ON ((ds.asset_id = a.id)))
     JOIN public.companies c ON ((ds.company_id = c.id)))
  WHERE ((ds.status)::text = ANY (ARRAY[('APPROVED'::character varying)::text, ('POSTED'::character varying)::text]))
  GROUP BY c.name, (date_trunc('month'::text, (de.period_start)::timestamp with time zone))
  ORDER BY (date_trunc('month'::text, (de.period_start)::timestamp with time zone)), c.name;


ALTER VIEW public.v_monthly_depreciation OWNER TO sthwalonyoni;

--
-- Name: v_payslip_summary; Type: VIEW; Schema: public; Owner: sthwalonyoni
--

CREATE VIEW public.v_payslip_summary AS
 SELECT p.id,
    p.payslip_number,
    p.company_id,
    pp.period_name,
    pp.pay_date,
    e.employee_number,
    (((e.first_name)::text || ' '::text) || (e.last_name)::text) AS employee_name,
    p.total_earnings,
    p.total_deductions,
    p.net_pay,
    p.status
   FROM ((public.payslips p
     JOIN public.employees e ON ((p.employee_id = e.id)))
     JOIN public.payroll_periods pp ON ((p.payroll_period_id = pp.id)));


ALTER VIEW public.v_payslip_summary OWNER TO sthwalonyoni;

--
-- Name: account_categories id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.account_categories ALTER COLUMN id SET DEFAULT nextval('public.account_categories_id_seq'::regclass);


--
-- Name: account_types id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.account_types ALTER COLUMN id SET DEFAULT nextval('public.account_types_id_seq'::regclass);


--
-- Name: accounts id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.accounts ALTER COLUMN id SET DEFAULT nextval('public.accounts_id_seq'::regclass);


--
-- Name: asset_disposals id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.asset_disposals ALTER COLUMN id SET DEFAULT nextval('public.asset_disposals_id_seq'::regclass);


--
-- Name: assets id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.assets ALTER COLUMN id SET DEFAULT nextval('public.assets_id_seq'::regclass);


--
-- Name: audit_reports id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.audit_reports ALTER COLUMN id SET DEFAULT nextval('public.audit_reports_id_seq'::regclass);


--
-- Name: bank_accounts id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.bank_accounts ALTER COLUMN id SET DEFAULT nextval('public.bank_accounts_id_seq'::regclass);


--
-- Name: bank_transactions id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.bank_transactions ALTER COLUMN id SET DEFAULT nextval('public.bank_transactions_id_seq'::regclass);


--
-- Name: benefits id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.benefits ALTER COLUMN id SET DEFAULT nextval('public.benefits_id_seq'::regclass);


--
-- Name: budget_categories id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_categories ALTER COLUMN id SET DEFAULT nextval('public.budget_categories_id_seq'::regclass);


--
-- Name: budget_items id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_items ALTER COLUMN id SET DEFAULT nextval('public.budget_items_id_seq'::regclass);


--
-- Name: budget_monthly_allocations id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_monthly_allocations ALTER COLUMN id SET DEFAULT nextval('public.budget_monthly_allocations_id_seq'::regclass);


--
-- Name: budget_projections id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_projections ALTER COLUMN id SET DEFAULT nextval('public.budget_projections_id_seq'::regclass);


--
-- Name: budgets id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budgets ALTER COLUMN id SET DEFAULT nextval('public.budgets_id_seq'::regclass);


--
-- Name: companies id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.companies ALTER COLUMN id SET DEFAULT nextval('public.companies_id_seq'::regclass);


--
-- Name: company_extended_info id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.company_extended_info ALTER COLUMN id SET DEFAULT nextval('public.company_extended_info_id_seq'::regclass);


--
-- Name: data_corrections id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.data_corrections ALTER COLUMN id SET DEFAULT nextval('public.data_corrections_id_seq'::regclass);


--
-- Name: deductions id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.deductions ALTER COLUMN id SET DEFAULT nextval('public.deductions_id_seq'::regclass);


--
-- Name: depreciation_adjustments id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_adjustments ALTER COLUMN id SET DEFAULT nextval('public.depreciation_adjustments_id_seq'::regclass);


--
-- Name: depreciation_entries id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_entries ALTER COLUMN id SET DEFAULT nextval('public.depreciation_entries_id_seq'::regclass);


--
-- Name: depreciation_policies id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_policies ALTER COLUMN id SET DEFAULT nextval('public.depreciation_policies_id_seq'::regclass);


--
-- Name: depreciation_schedules id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_schedules ALTER COLUMN id SET DEFAULT nextval('public.depreciation_schedules_id_seq'::regclass);


--
-- Name: directors_reports id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.directors_reports ALTER COLUMN id SET DEFAULT nextval('public.directors_reports_id_seq'::regclass);


--
-- Name: directors_responsibility_statements id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.directors_responsibility_statements ALTER COLUMN id SET DEFAULT nextval('public.directors_responsibility_statements_id_seq'::regclass);


--
-- Name: employee_leave id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.employee_leave ALTER COLUMN id SET DEFAULT nextval('public.employee_leave_id_seq'::regclass);


--
-- Name: employees id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.employees ALTER COLUMN id SET DEFAULT nextval('public.employees_id_seq'::regclass);


--
-- Name: equity_movements id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.equity_movements ALTER COLUMN id SET DEFAULT nextval('public.equity_movements_id_seq'::regclass);


--
-- Name: financial_notes id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.financial_notes ALTER COLUMN id SET DEFAULT nextval('public.financial_notes_id_seq'::regclass);


--
-- Name: fiscal_periods id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.fiscal_periods ALTER COLUMN id SET DEFAULT nextval('public.fiscal_periods_id_seq'::regclass);


--
-- Name: journal_entries id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.journal_entries ALTER COLUMN id SET DEFAULT nextval('public.journal_entries_id_seq'::regclass);


--
-- Name: journal_entry_lines id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.journal_entry_lines ALTER COLUMN id SET DEFAULT nextval('public.journal_entry_lines_id_seq'::regclass);


--
-- Name: manual_invoices id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.manual_invoices ALTER COLUMN id SET DEFAULT nextval('public.manual_invoices_id_seq'::regclass);


--
-- Name: operational_activities id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.operational_activities ALTER COLUMN id SET DEFAULT nextval('public.operational_activities_id_seq'::regclass);


--
-- Name: payroll_journal_entries id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payroll_journal_entries ALTER COLUMN id SET DEFAULT nextval('public.payroll_journal_entries_id_seq'::regclass);


--
-- Name: payroll_periods id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payroll_periods ALTER COLUMN id SET DEFAULT nextval('public.payroll_periods_id_seq'::regclass);


--
-- Name: payslips id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payslips ALTER COLUMN id SET DEFAULT nextval('public.payslips_id_seq'::regclass);


--
-- Name: strategic_initiatives id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_initiatives ALTER COLUMN id SET DEFAULT nextval('public.strategic_initiatives_id_seq'::regclass);


--
-- Name: strategic_milestones id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_milestones ALTER COLUMN id SET DEFAULT nextval('public.strategic_milestones_id_seq'::regclass);


--
-- Name: strategic_plans id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_plans ALTER COLUMN id SET DEFAULT nextval('public.strategic_plans_id_seq'::regclass);


--
-- Name: strategic_priorities id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_priorities ALTER COLUMN id SET DEFAULT nextval('public.strategic_priorities_id_seq'::regclass);


--
-- Name: tax_brackets id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.tax_brackets ALTER COLUMN id SET DEFAULT nextval('public.tax_brackets_id_seq'::regclass);


--
-- Name: tax_configurations id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.tax_configurations ALTER COLUMN id SET DEFAULT nextval('public.tax_configurations_id_seq'::regclass);


--
-- Name: transaction_mapping_rules id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.transaction_mapping_rules ALTER COLUMN id SET DEFAULT nextval('public.transaction_mapping_rules_id_seq'::regclass);


--
-- Name: transaction_types id; Type: DEFAULT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.transaction_types ALTER COLUMN id SET DEFAULT nextval('public.transaction_types_id_seq'::regclass);


--
-- Name: account_categories account_categories_company_id_name_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.account_categories
    ADD CONSTRAINT account_categories_company_id_name_key UNIQUE (company_id, name);


--
-- Name: account_categories account_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.account_categories
    ADD CONSTRAINT account_categories_pkey PRIMARY KEY (id);


--
-- Name: account_types account_types_code_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.account_types
    ADD CONSTRAINT account_types_code_key UNIQUE (code);


--
-- Name: account_types account_types_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.account_types
    ADD CONSTRAINT account_types_pkey PRIMARY KEY (id);


--
-- Name: accounts accounts_company_id_account_code_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_company_id_account_code_key UNIQUE (company_id, account_code);


--
-- Name: accounts accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);


--
-- Name: asset_disposals asset_disposals_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.asset_disposals
    ADD CONSTRAINT asset_disposals_pkey PRIMARY KEY (id);


--
-- Name: asset_disposals asset_disposals_reference_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.asset_disposals
    ADD CONSTRAINT asset_disposals_reference_key UNIQUE (reference);


--
-- Name: assets assets_asset_code_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.assets
    ADD CONSTRAINT assets_asset_code_key UNIQUE (asset_code);


--
-- Name: assets assets_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.assets
    ADD CONSTRAINT assets_pkey PRIMARY KEY (id);


--
-- Name: audit_reports audit_reports_company_id_fiscal_period_id_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.audit_reports
    ADD CONSTRAINT audit_reports_company_id_fiscal_period_id_key UNIQUE (company_id, fiscal_period_id);


--
-- Name: audit_reports audit_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.audit_reports
    ADD CONSTRAINT audit_reports_pkey PRIMARY KEY (id);


--
-- Name: bank_accounts bank_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT bank_accounts_pkey PRIMARY KEY (id);


--
-- Name: bank_transactions bank_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT bank_transactions_pkey PRIMARY KEY (id);


--
-- Name: benefits benefits_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.benefits
    ADD CONSTRAINT benefits_pkey PRIMARY KEY (id);


--
-- Name: budget_categories budget_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_categories
    ADD CONSTRAINT budget_categories_pkey PRIMARY KEY (id);


--
-- Name: budget_items budget_items_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_items
    ADD CONSTRAINT budget_items_pkey PRIMARY KEY (id);


--
-- Name: budget_monthly_allocations budget_monthly_allocations_budget_item_id_month_number_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_monthly_allocations
    ADD CONSTRAINT budget_monthly_allocations_budget_item_id_month_number_key UNIQUE (budget_item_id, month_number);


--
-- Name: budget_monthly_allocations budget_monthly_allocations_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_monthly_allocations
    ADD CONSTRAINT budget_monthly_allocations_pkey PRIMARY KEY (id);


--
-- Name: budget_projections budget_projections_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_projections
    ADD CONSTRAINT budget_projections_pkey PRIMARY KEY (id);


--
-- Name: budgets budgets_company_id_budget_year_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budgets
    ADD CONSTRAINT budgets_company_id_budget_year_key UNIQUE (company_id, budget_year);


--
-- Name: budgets budgets_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budgets
    ADD CONSTRAINT budgets_pkey PRIMARY KEY (id);


--
-- Name: companies companies_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_pkey PRIMARY KEY (id);


--
-- Name: company_classification_rules company_classification_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.company_classification_rules
    ADD CONSTRAINT company_classification_rules_pkey PRIMARY KEY (id);


--
-- Name: company_extended_info company_extended_info_company_id_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.company_extended_info
    ADD CONSTRAINT company_extended_info_company_id_key UNIQUE (company_id);


--
-- Name: company_extended_info company_extended_info_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.company_extended_info
    ADD CONSTRAINT company_extended_info_pkey PRIMARY KEY (id);


--
-- Name: data_corrections data_corrections_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.data_corrections
    ADD CONSTRAINT data_corrections_pkey PRIMARY KEY (id);


--
-- Name: deductions deductions_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.deductions
    ADD CONSTRAINT deductions_pkey PRIMARY KEY (id);


--
-- Name: depreciation_adjustments depreciation_adjustments_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_adjustments
    ADD CONSTRAINT depreciation_adjustments_pkey PRIMARY KEY (id);


--
-- Name: depreciation_entries depreciation_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_entries
    ADD CONSTRAINT depreciation_entries_pkey PRIMARY KEY (id);


--
-- Name: depreciation_policies depreciation_policies_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_policies
    ADD CONSTRAINT depreciation_policies_pkey PRIMARY KEY (id);


--
-- Name: depreciation_policies depreciation_policies_policy_code_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_policies
    ADD CONSTRAINT depreciation_policies_policy_code_key UNIQUE (policy_code);


--
-- Name: depreciation_schedules depreciation_schedules_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_schedules
    ADD CONSTRAINT depreciation_schedules_pkey PRIMARY KEY (id);


--
-- Name: depreciation_schedules depreciation_schedules_schedule_number_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_schedules
    ADD CONSTRAINT depreciation_schedules_schedule_number_key UNIQUE (schedule_number);


--
-- Name: directors_reports directors_reports_company_id_fiscal_period_id_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.directors_reports
    ADD CONSTRAINT directors_reports_company_id_fiscal_period_id_key UNIQUE (company_id, fiscal_period_id);


--
-- Name: directors_reports directors_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.directors_reports
    ADD CONSTRAINT directors_reports_pkey PRIMARY KEY (id);


--
-- Name: directors_responsibility_statements directors_responsibility_statem_company_id_fiscal_period_id_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.directors_responsibility_statements
    ADD CONSTRAINT directors_responsibility_statem_company_id_fiscal_period_id_key UNIQUE (company_id, fiscal_period_id);


--
-- Name: directors_responsibility_statements directors_responsibility_statements_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.directors_responsibility_statements
    ADD CONSTRAINT directors_responsibility_statements_pkey PRIMARY KEY (id);


--
-- Name: employee_leave employee_leave_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.employee_leave
    ADD CONSTRAINT employee_leave_pkey PRIMARY KEY (id);


--
-- Name: employees employees_email_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_email_key UNIQUE (email);


--
-- Name: employees employees_employee_number_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_employee_number_key UNIQUE (employee_number);


--
-- Name: employees employees_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_pkey PRIMARY KEY (id);


--
-- Name: equity_movements equity_movements_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.equity_movements
    ADD CONSTRAINT equity_movements_pkey PRIMARY KEY (id);


--
-- Name: financial_notes financial_notes_company_id_fiscal_period_id_note_number_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.financial_notes
    ADD CONSTRAINT financial_notes_company_id_fiscal_period_id_note_number_key UNIQUE (company_id, fiscal_period_id, note_number);


--
-- Name: financial_notes financial_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.financial_notes
    ADD CONSTRAINT financial_notes_pkey PRIMARY KEY (id);


--
-- Name: fiscal_periods fiscal_periods_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.fiscal_periods
    ADD CONSTRAINT fiscal_periods_pkey PRIMARY KEY (id);


--
-- Name: journal_entries journal_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT journal_entries_pkey PRIMARY KEY (id);


--
-- Name: journal_entry_lines journal_entry_lines_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.journal_entry_lines
    ADD CONSTRAINT journal_entry_lines_pkey PRIMARY KEY (id);


--
-- Name: manual_invoices manual_invoices_company_id_invoice_number_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.manual_invoices
    ADD CONSTRAINT manual_invoices_company_id_invoice_number_key UNIQUE (company_id, invoice_number);


--
-- Name: manual_invoices manual_invoices_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.manual_invoices
    ADD CONSTRAINT manual_invoices_pkey PRIMARY KEY (id);


--
-- Name: operational_activities operational_activities_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.operational_activities
    ADD CONSTRAINT operational_activities_pkey PRIMARY KEY (id);


--
-- Name: operational_activities operational_activities_strategic_plan_id_month_number_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.operational_activities
    ADD CONSTRAINT operational_activities_strategic_plan_id_month_number_key UNIQUE (strategic_plan_id, month_number);


--
-- Name: payroll_journal_entries payroll_journal_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payroll_journal_entries
    ADD CONSTRAINT payroll_journal_entries_pkey PRIMARY KEY (id);


--
-- Name: payroll_periods payroll_periods_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payroll_periods
    ADD CONSTRAINT payroll_periods_pkey PRIMARY KEY (id);


--
-- Name: payslips payslips_payslip_number_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_payslip_number_key UNIQUE (payslip_number);


--
-- Name: payslips payslips_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_pkey PRIMARY KEY (id);


--
-- Name: strategic_initiatives strategic_initiatives_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_initiatives
    ADD CONSTRAINT strategic_initiatives_pkey PRIMARY KEY (id);


--
-- Name: strategic_milestones strategic_milestones_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_milestones
    ADD CONSTRAINT strategic_milestones_pkey PRIMARY KEY (id);


--
-- Name: strategic_plans strategic_plans_company_id_title_key; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_plans
    ADD CONSTRAINT strategic_plans_company_id_title_key UNIQUE (company_id, title);


--
-- Name: strategic_plans strategic_plans_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_plans
    ADD CONSTRAINT strategic_plans_pkey PRIMARY KEY (id);


--
-- Name: strategic_priorities strategic_priorities_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_priorities
    ADD CONSTRAINT strategic_priorities_pkey PRIMARY KEY (id);


--
-- Name: tax_brackets tax_brackets_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.tax_brackets
    ADD CONSTRAINT tax_brackets_pkey PRIMARY KEY (id);


--
-- Name: tax_configurations tax_configurations_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.tax_configurations
    ADD CONSTRAINT tax_configurations_pkey PRIMARY KEY (id);


--
-- Name: transaction_mapping_rules transaction_mapping_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.transaction_mapping_rules
    ADD CONSTRAINT transaction_mapping_rules_pkey PRIMARY KEY (id);


--
-- Name: transaction_types transaction_types_pkey; Type: CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.transaction_types
    ADD CONSTRAINT transaction_types_pkey PRIMARY KEY (id);


--
-- Name: idx_accounts_category; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_accounts_category ON public.accounts USING btree (category_id);


--
-- Name: idx_accounts_company; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_accounts_company ON public.accounts USING btree (company_id, is_active);


--
-- Name: idx_accounts_parent; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_accounts_parent ON public.accounts USING btree (parent_account_id) WHERE (parent_account_id IS NOT NULL);


--
-- Name: idx_asset_disposals_asset_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_asset_disposals_asset_id ON public.asset_disposals USING btree (asset_id);


--
-- Name: idx_asset_disposals_company_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_asset_disposals_company_id ON public.asset_disposals USING btree (company_id);


--
-- Name: idx_asset_disposals_disposal_date; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_asset_disposals_disposal_date ON public.asset_disposals USING btree (disposal_date);


--
-- Name: idx_asset_disposals_reference; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_asset_disposals_reference ON public.asset_disposals USING btree (reference);


--
-- Name: idx_assets_asset_code; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_assets_asset_code ON public.assets USING btree (asset_code);


--
-- Name: idx_assets_category; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_assets_category ON public.assets USING btree (asset_category);


--
-- Name: idx_assets_company_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_assets_company_id ON public.assets USING btree (company_id);


--
-- Name: idx_assets_status; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_assets_status ON public.assets USING btree (status);


--
-- Name: idx_audit_reports_company_period; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_audit_reports_company_period ON public.audit_reports USING btree (company_id, fiscal_period_id);


--
-- Name: idx_audit_reports_opinion; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_audit_reports_opinion ON public.audit_reports USING btree (audit_opinion);


--
-- Name: idx_bank_transactions_company_date; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_bank_transactions_company_date ON public.bank_transactions USING btree (company_id, transaction_date);


--
-- Name: idx_bank_transactions_fiscal_period; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_bank_transactions_fiscal_period ON public.bank_transactions USING btree (fiscal_period_id);


--
-- Name: idx_benefits_company_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_benefits_company_id ON public.benefits USING btree (company_id);


--
-- Name: idx_benefits_employee_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_benefits_employee_id ON public.benefits USING btree (employee_id);


--
-- Name: idx_benefits_is_active; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_benefits_is_active ON public.benefits USING btree (is_active);


--
-- Name: idx_budget_items_account; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_budget_items_account ON public.budget_items USING btree (account_id);


--
-- Name: idx_budget_monthly_allocations_item; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_budget_monthly_allocations_item ON public.budget_monthly_allocations USING btree (budget_item_id);


--
-- Name: idx_budget_monthly_allocations_month; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_budget_monthly_allocations_month ON public.budget_monthly_allocations USING btree (month_number);


--
-- Name: idx_budgets_company; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_budgets_company ON public.budgets USING btree (company_id);


--
-- Name: idx_budgets_status; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_budgets_status ON public.budgets USING btree (status);


--
-- Name: idx_budgets_year; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_budgets_year ON public.budgets USING btree (budget_year);


--
-- Name: idx_classification_rules_company; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_classification_rules_company ON public.company_classification_rules USING btree (company_id, account_code);


--
-- Name: idx_company_extended_info_company; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_company_extended_info_company ON public.company_extended_info USING btree (company_id);


--
-- Name: idx_deductions_company_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_deductions_company_id ON public.deductions USING btree (company_id);


--
-- Name: idx_deductions_employee_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_deductions_employee_id ON public.deductions USING btree (employee_id);


--
-- Name: idx_deductions_is_active; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_deductions_is_active ON public.deductions USING btree (is_active);


--
-- Name: idx_depreciation_adjustments_entry_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_depreciation_adjustments_entry_id ON public.depreciation_adjustments USING btree (depreciation_entry_id);


--
-- Name: idx_depreciation_adjustments_schedule_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_depreciation_adjustments_schedule_id ON public.depreciation_adjustments USING btree (depreciation_schedule_id);


--
-- Name: idx_depreciation_entries_fiscal_year; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_depreciation_entries_fiscal_year ON public.depreciation_entries USING btree (fiscal_year);


--
-- Name: idx_depreciation_entries_schedule_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_depreciation_entries_schedule_id ON public.depreciation_entries USING btree (depreciation_schedule_id);


--
-- Name: idx_depreciation_entries_year_number; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_depreciation_entries_year_number ON public.depreciation_entries USING btree (year_number);


--
-- Name: idx_depreciation_policies_company_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_depreciation_policies_company_id ON public.depreciation_policies USING btree (company_id);


--
-- Name: idx_depreciation_policies_is_active; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_depreciation_policies_is_active ON public.depreciation_policies USING btree (is_active);


--
-- Name: idx_depreciation_policies_is_default; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_depreciation_policies_is_default ON public.depreciation_policies USING btree (is_default);


--
-- Name: idx_depreciation_schedules_asset_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_depreciation_schedules_asset_id ON public.depreciation_schedules USING btree (asset_id);


--
-- Name: idx_depreciation_schedules_company_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_depreciation_schedules_company_id ON public.depreciation_schedules USING btree (company_id);


--
-- Name: idx_depreciation_schedules_schedule_number; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_depreciation_schedules_schedule_number ON public.depreciation_schedules USING btree (schedule_number);


--
-- Name: idx_depreciation_schedules_status; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_depreciation_schedules_status ON public.depreciation_schedules USING btree (status);


--
-- Name: idx_directors_reports_company_period; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_directors_reports_company_period ON public.directors_reports USING btree (company_id, fiscal_period_id);


--
-- Name: idx_directors_responsibility_company_period; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_directors_responsibility_company_period ON public.directors_responsibility_statements USING btree (company_id, fiscal_period_id);


--
-- Name: idx_employees_company_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_employees_company_id ON public.employees USING btree (company_id);


--
-- Name: idx_employees_employee_number; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_employees_employee_number ON public.employees USING btree (employee_number);


--
-- Name: idx_employees_is_active; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_employees_is_active ON public.employees USING btree (is_active);


--
-- Name: idx_equity_movements_company_period; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_equity_movements_company_period ON public.equity_movements USING btree (company_id, fiscal_period_id);


--
-- Name: idx_equity_movements_component; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_equity_movements_component ON public.equity_movements USING btree (equity_component);


--
-- Name: idx_financial_notes_category; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_financial_notes_category ON public.financial_notes USING btree (note_category);


--
-- Name: idx_financial_notes_company_period; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_financial_notes_company_period ON public.financial_notes USING btree (company_id, fiscal_period_id);


--
-- Name: idx_financial_notes_order; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_financial_notes_order ON public.financial_notes USING btree (display_order);


--
-- Name: idx_journal_entries_company_date; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_journal_entries_company_date ON public.journal_entries USING btree (company_id, entry_date);


--
-- Name: idx_journal_entry_lines_account; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_journal_entry_lines_account ON public.journal_entry_lines USING btree (account_id);


--
-- Name: idx_journal_entry_lines_source_transaction; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_journal_entry_lines_source_transaction ON public.journal_entry_lines USING btree (source_transaction_id);


--
-- Name: idx_operational_activities_month; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_operational_activities_month ON public.operational_activities USING btree (month_number);


--
-- Name: idx_operational_activities_plan; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_operational_activities_plan ON public.operational_activities USING btree (strategic_plan_id);


--
-- Name: idx_payroll_periods_company_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_payroll_periods_company_id ON public.payroll_periods USING btree (company_id);


--
-- Name: idx_payroll_periods_pay_date; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_payroll_periods_pay_date ON public.payroll_periods USING btree (pay_date);


--
-- Name: idx_payroll_periods_status; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_payroll_periods_status ON public.payroll_periods USING btree (status);


--
-- Name: idx_payslips_company_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_payslips_company_id ON public.payslips USING btree (company_id);


--
-- Name: idx_payslips_employee_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_payslips_employee_id ON public.payslips USING btree (employee_id);


--
-- Name: idx_payslips_payroll_period_id; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_payslips_payroll_period_id ON public.payslips USING btree (payroll_period_id);


--
-- Name: idx_payslips_status; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_payslips_status ON public.payslips USING btree (status);


--
-- Name: idx_strategic_plans_company; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_strategic_plans_company ON public.strategic_plans USING btree (company_id);


--
-- Name: idx_strategic_plans_status; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_strategic_plans_status ON public.strategic_plans USING btree (status);


--
-- Name: idx_transaction_mapping_rules_company; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_transaction_mapping_rules_company ON public.transaction_mapping_rules USING btree (company_id, is_active, priority);


--
-- Name: idx_transaction_types_company; Type: INDEX; Schema: public; Owner: sthwalonyoni
--

CREATE INDEX idx_transaction_types_company ON public.transaction_types USING btree (company_id, is_active);


--
-- Name: account_categories account_categories_account_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.account_categories
    ADD CONSTRAINT account_categories_account_type_id_fkey FOREIGN KEY (account_type_id) REFERENCES public.account_types(id) ON DELETE RESTRICT;


--
-- Name: account_categories account_categories_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.account_categories
    ADD CONSTRAINT account_categories_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: accounts accounts_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.account_categories(id) ON DELETE RESTRICT;


--
-- Name: accounts accounts_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: accounts accounts_parent_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_parent_account_id_fkey FOREIGN KEY (parent_account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- Name: asset_disposals asset_disposals_asset_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.asset_disposals
    ADD CONSTRAINT asset_disposals_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES public.assets(id) ON DELETE CASCADE;


--
-- Name: asset_disposals asset_disposals_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.asset_disposals
    ADD CONSTRAINT asset_disposals_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: asset_disposals asset_disposals_journal_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.asset_disposals
    ADD CONSTRAINT asset_disposals_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id);


--
-- Name: assets assets_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.assets
    ADD CONSTRAINT assets_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: audit_reports audit_reports_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.audit_reports
    ADD CONSTRAINT audit_reports_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: audit_reports audit_reports_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.audit_reports
    ADD CONSTRAINT audit_reports_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id) ON DELETE CASCADE;


--
-- Name: bank_accounts bank_accounts_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.bank_accounts
    ADD CONSTRAINT bank_accounts_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: bank_transactions bank_transactions_bank_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT bank_transactions_bank_account_id_fkey FOREIGN KEY (bank_account_id) REFERENCES public.bank_accounts(id) ON DELETE SET NULL;


--
-- Name: bank_transactions bank_transactions_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT bank_transactions_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: bank_transactions bank_transactions_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.bank_transactions
    ADD CONSTRAINT bank_transactions_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id) ON DELETE RESTRICT;


--
-- Name: benefits benefits_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.benefits
    ADD CONSTRAINT benefits_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: benefits benefits_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.benefits
    ADD CONSTRAINT benefits_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- Name: budget_categories budget_categories_budget_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_categories
    ADD CONSTRAINT budget_categories_budget_id_fkey FOREIGN KEY (budget_id) REFERENCES public.budgets(id) ON DELETE CASCADE;


--
-- Name: budget_items budget_items_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_items
    ADD CONSTRAINT budget_items_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id);


--
-- Name: budget_items budget_items_budget_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_items
    ADD CONSTRAINT budget_items_budget_category_id_fkey FOREIGN KEY (budget_category_id) REFERENCES public.budget_categories(id) ON DELETE CASCADE;


--
-- Name: budget_monthly_allocations budget_monthly_allocations_budget_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_monthly_allocations
    ADD CONSTRAINT budget_monthly_allocations_budget_item_id_fkey FOREIGN KEY (budget_item_id) REFERENCES public.budget_items(id) ON DELETE CASCADE;


--
-- Name: budget_projections budget_projections_budget_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budget_projections
    ADD CONSTRAINT budget_projections_budget_id_fkey FOREIGN KEY (budget_id) REFERENCES public.budgets(id) ON DELETE CASCADE;


--
-- Name: budgets budgets_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budgets
    ADD CONSTRAINT budgets_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: budgets budgets_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.budgets
    ADD CONSTRAINT budgets_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id);


--
-- Name: company_classification_rules company_classification_rules_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.company_classification_rules
    ADD CONSTRAINT company_classification_rules_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: company_extended_info company_extended_info_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.company_extended_info
    ADD CONSTRAINT company_extended_info_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: data_corrections data_corrections_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.data_corrections
    ADD CONSTRAINT data_corrections_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: data_corrections data_corrections_new_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.data_corrections
    ADD CONSTRAINT data_corrections_new_account_id_fkey FOREIGN KEY (new_account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- Name: data_corrections data_corrections_original_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.data_corrections
    ADD CONSTRAINT data_corrections_original_account_id_fkey FOREIGN KEY (original_account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- Name: data_corrections data_corrections_transaction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.data_corrections
    ADD CONSTRAINT data_corrections_transaction_id_fkey FOREIGN KEY (transaction_id) REFERENCES public.bank_transactions(id) ON DELETE CASCADE;


--
-- Name: deductions deductions_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.deductions
    ADD CONSTRAINT deductions_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: deductions deductions_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.deductions
    ADD CONSTRAINT deductions_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- Name: depreciation_adjustments depreciation_adjustments_depreciation_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_adjustments
    ADD CONSTRAINT depreciation_adjustments_depreciation_entry_id_fkey FOREIGN KEY (depreciation_entry_id) REFERENCES public.depreciation_entries(id);


--
-- Name: depreciation_adjustments depreciation_adjustments_depreciation_schedule_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_adjustments
    ADD CONSTRAINT depreciation_adjustments_depreciation_schedule_id_fkey FOREIGN KEY (depreciation_schedule_id) REFERENCES public.depreciation_schedules(id);


--
-- Name: depreciation_entries depreciation_entries_depreciation_schedule_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_entries
    ADD CONSTRAINT depreciation_entries_depreciation_schedule_id_fkey FOREIGN KEY (depreciation_schedule_id) REFERENCES public.depreciation_schedules(id);


--
-- Name: depreciation_entries depreciation_entries_journal_entry_line_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_entries
    ADD CONSTRAINT depreciation_entries_journal_entry_line_id_fkey FOREIGN KEY (journal_entry_line_id) REFERENCES public.journal_entry_lines(id);


--
-- Name: depreciation_policies depreciation_policies_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_policies
    ADD CONSTRAINT depreciation_policies_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: depreciation_schedules depreciation_schedules_asset_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_schedules
    ADD CONSTRAINT depreciation_schedules_asset_id_fkey FOREIGN KEY (asset_id) REFERENCES public.assets(id);


--
-- Name: depreciation_schedules depreciation_schedules_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_schedules
    ADD CONSTRAINT depreciation_schedules_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: depreciation_schedules depreciation_schedules_journal_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.depreciation_schedules
    ADD CONSTRAINT depreciation_schedules_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id);


--
-- Name: directors_reports directors_reports_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.directors_reports
    ADD CONSTRAINT directors_reports_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: directors_reports directors_reports_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.directors_reports
    ADD CONSTRAINT directors_reports_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id) ON DELETE CASCADE;


--
-- Name: directors_responsibility_statements directors_responsibility_statements_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.directors_responsibility_statements
    ADD CONSTRAINT directors_responsibility_statements_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: directors_responsibility_statements directors_responsibility_statements_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.directors_responsibility_statements
    ADD CONSTRAINT directors_responsibility_statements_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id) ON DELETE CASCADE;


--
-- Name: employee_leave employee_leave_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.employee_leave
    ADD CONSTRAINT employee_leave_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: employee_leave employee_leave_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.employee_leave
    ADD CONSTRAINT employee_leave_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- Name: employees employees_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: equity_movements equity_movements_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.equity_movements
    ADD CONSTRAINT equity_movements_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: equity_movements equity_movements_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.equity_movements
    ADD CONSTRAINT equity_movements_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id) ON DELETE CASCADE;


--
-- Name: financial_notes financial_notes_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.financial_notes
    ADD CONSTRAINT financial_notes_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: financial_notes financial_notes_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.financial_notes
    ADD CONSTRAINT financial_notes_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id) ON DELETE CASCADE;


--
-- Name: fiscal_periods fiscal_periods_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.fiscal_periods
    ADD CONSTRAINT fiscal_periods_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: journal_entries journal_entries_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT journal_entries_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: journal_entries journal_entries_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT journal_entries_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id) ON DELETE RESTRICT;


--
-- Name: journal_entries journal_entries_transaction_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT journal_entries_transaction_type_id_fkey FOREIGN KEY (transaction_type_id) REFERENCES public.transaction_types(id) ON DELETE SET NULL;


--
-- Name: journal_entry_lines journal_entry_lines_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.journal_entry_lines
    ADD CONSTRAINT journal_entry_lines_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- Name: journal_entry_lines journal_entry_lines_journal_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.journal_entry_lines
    ADD CONSTRAINT journal_entry_lines_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id) ON DELETE CASCADE;


--
-- Name: journal_entry_lines journal_entry_lines_source_transaction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.journal_entry_lines
    ADD CONSTRAINT journal_entry_lines_source_transaction_id_fkey FOREIGN KEY (source_transaction_id) REFERENCES public.bank_transactions(id) ON DELETE SET NULL;


--
-- Name: manual_invoices manual_invoices_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.manual_invoices
    ADD CONSTRAINT manual_invoices_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: manual_invoices manual_invoices_credit_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.manual_invoices
    ADD CONSTRAINT manual_invoices_credit_account_id_fkey FOREIGN KEY (credit_account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- Name: manual_invoices manual_invoices_debit_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.manual_invoices
    ADD CONSTRAINT manual_invoices_debit_account_id_fkey FOREIGN KEY (debit_account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;


--
-- Name: manual_invoices manual_invoices_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.manual_invoices
    ADD CONSTRAINT manual_invoices_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id) ON DELETE RESTRICT;


--
-- Name: operational_activities operational_activities_strategic_plan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.operational_activities
    ADD CONSTRAINT operational_activities_strategic_plan_id_fkey FOREIGN KEY (strategic_plan_id) REFERENCES public.strategic_plans(id) ON DELETE CASCADE;


--
-- Name: payroll_journal_entries payroll_journal_entries_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payroll_journal_entries
    ADD CONSTRAINT payroll_journal_entries_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: payroll_journal_entries payroll_journal_entries_journal_entry_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payroll_journal_entries
    ADD CONSTRAINT payroll_journal_entries_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id);


--
-- Name: payroll_journal_entries payroll_journal_entries_payroll_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payroll_journal_entries
    ADD CONSTRAINT payroll_journal_entries_payroll_period_id_fkey FOREIGN KEY (payroll_period_id) REFERENCES public.payroll_periods(id);


--
-- Name: payroll_periods payroll_periods_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payroll_periods
    ADD CONSTRAINT payroll_periods_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: payroll_periods payroll_periods_fiscal_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payroll_periods
    ADD CONSTRAINT payroll_periods_fiscal_period_id_fkey FOREIGN KEY (fiscal_period_id) REFERENCES public.fiscal_periods(id);


--
-- Name: payslips payslips_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: payslips payslips_employee_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.employees(id);


--
-- Name: payslips payslips_payroll_period_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.payslips
    ADD CONSTRAINT payslips_payroll_period_id_fkey FOREIGN KEY (payroll_period_id) REFERENCES public.payroll_periods(id);


--
-- Name: strategic_initiatives strategic_initiatives_strategic_priority_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_initiatives
    ADD CONSTRAINT strategic_initiatives_strategic_priority_id_fkey FOREIGN KEY (strategic_priority_id) REFERENCES public.strategic_priorities(id) ON DELETE CASCADE;


--
-- Name: strategic_milestones strategic_milestones_strategic_initiative_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_milestones
    ADD CONSTRAINT strategic_milestones_strategic_initiative_id_fkey FOREIGN KEY (strategic_initiative_id) REFERENCES public.strategic_initiatives(id) ON DELETE CASCADE;


--
-- Name: strategic_plans strategic_plans_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_plans
    ADD CONSTRAINT strategic_plans_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: strategic_priorities strategic_priorities_strategic_plan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.strategic_priorities
    ADD CONSTRAINT strategic_priorities_strategic_plan_id_fkey FOREIGN KEY (strategic_plan_id) REFERENCES public.strategic_plans(id) ON DELETE CASCADE;


--
-- Name: tax_brackets tax_brackets_tax_configuration_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.tax_brackets
    ADD CONSTRAINT tax_brackets_tax_configuration_id_fkey FOREIGN KEY (tax_configuration_id) REFERENCES public.tax_configurations(id);


--
-- Name: tax_configurations tax_configurations_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.tax_configurations
    ADD CONSTRAINT tax_configurations_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);


--
-- Name: transaction_mapping_rules transaction_mapping_rules_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.transaction_mapping_rules
    ADD CONSTRAINT transaction_mapping_rules_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id) ON DELETE CASCADE;


--
-- Name: transaction_mapping_rules transaction_mapping_rules_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.transaction_mapping_rules
    ADD CONSTRAINT transaction_mapping_rules_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: transaction_types transaction_types_company_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: sthwalonyoni
--

ALTER TABLE ONLY public.transaction_types
    ADD CONSTRAINT transaction_types_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: pg_database_owner
--

GRANT ALL ON SCHEMA public TO sthwalonyoni;


--
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: sthwalonyoni
--

ALTER DEFAULT PRIVILEGES FOR ROLE sthwalonyoni IN SCHEMA public GRANT ALL ON SEQUENCES TO sthwalonyoni;


--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: sthwalonyoni
--

ALTER DEFAULT PRIVILEGES FOR ROLE sthwalonyoni IN SCHEMA public GRANT ALL ON TABLES TO sthwalonyoni;


--
-- PostgreSQL database dump complete
--

\unrestrict mil14gCPvzaZMzvc70nLdU5TGfW77UbJef7FxCv0JM6DjPZrNAJ3bwk8vzZKui4

