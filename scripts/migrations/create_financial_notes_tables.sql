-- Migration: Create Financial Notes and Report Metadata Tables
-- Description: Add database support for complete IFRS-compliant financial reports
-- Date: 2025-11-04
-- Author: FIN Financial Management System

-- =====================================================
-- 1. FINANCIAL NOTES TABLE
-- Stores notes to financial statements (IFRS/IAS/GAAP compliant)
-- =====================================================
CREATE TABLE IF NOT EXISTS financial_notes (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    fiscal_period_id BIGINT NOT NULL REFERENCES fiscal_periods(id) ON DELETE CASCADE,
    note_number VARCHAR(10) NOT NULL, -- e.g., "1", "2.1", "7.3"
    note_title VARCHAR(255) NOT NULL, -- e.g., "Accounting Policies", "Revenue", "Property, Plant & Equipment"
    note_category VARCHAR(50) NOT NULL, -- 'accounting_policies', 'revenue', 'expenses', 'assets', 'liabilities', 'equity', 'cash_flow', 'other'
    note_content TEXT NOT NULL, -- Full text content of the note
    display_order INTEGER NOT NULL DEFAULT 0, -- Order for display in reports
    is_mandatory BOOLEAN DEFAULT FALSE, -- Required by IFRS/IAS
    standard_reference VARCHAR(100), -- e.g., "IAS 1", "IFRS 15", "GAAP ASC 606"
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, fiscal_period_id, note_number)
);

CREATE INDEX idx_financial_notes_company_period ON financial_notes(company_id, fiscal_period_id);
CREATE INDEX idx_financial_notes_category ON financial_notes(note_category);
CREATE INDEX idx_financial_notes_order ON financial_notes(display_order);

-- =====================================================
-- 2. DIRECTORS RESPONSIBILITY STATEMENT TABLE
-- Stores statement of financial responsibility by directors
-- =====================================================
CREATE TABLE IF NOT EXISTS directors_responsibility_statements (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    fiscal_period_id BIGINT NOT NULL REFERENCES fiscal_periods(id) ON DELETE CASCADE,
    statement_text TEXT NOT NULL, -- Full responsibility statement
    statement_date DATE NOT NULL, -- Date statement was made
    approved_by VARCHAR(255), -- Name of approving director
    approved_by_title VARCHAR(100), -- e.g., "Chief Executive Officer", "Chairperson"
    witness_name VARCHAR(255), -- Name of witness (if applicable)
    witness_title VARCHAR(100), -- Title of witness
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, fiscal_period_id)
);

CREATE INDEX idx_directors_responsibility_company_period ON directors_responsibility_statements(company_id, fiscal_period_id);

-- =====================================================
-- 3. AUDIT REPORTS TABLE
-- Stores independent auditor's reports
-- =====================================================
CREATE TABLE IF NOT EXISTS audit_reports (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    fiscal_period_id BIGINT NOT NULL REFERENCES fiscal_periods(id) ON DELETE CASCADE,
    audit_opinion VARCHAR(50) NOT NULL, -- 'unqualified', 'qualified', 'adverse', 'disclaimer'
    opinion_text TEXT NOT NULL, -- Full opinion paragraph
    basis_for_opinion TEXT, -- Basis for opinion section
    key_audit_matters TEXT, -- Key audit matters (if applicable)
    other_information TEXT, -- Other information section
    responsibilities_management TEXT, -- Management's responsibilities
    responsibilities_auditor TEXT, -- Auditor's responsibilities
    auditor_firm_name VARCHAR(255) NOT NULL,
    auditor_firm_registration VARCHAR(100), -- IRBA registration number
    auditor_partner_name VARCHAR(255),
    audit_date DATE NOT NULL,
    report_date DATE NOT NULL,
    audit_standard VARCHAR(100) DEFAULT 'ISA', -- 'ISA', 'GAAS', etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, fiscal_period_id)
);

CREATE INDEX idx_audit_reports_company_period ON audit_reports(company_id, fiscal_period_id);
CREATE INDEX idx_audit_reports_opinion ON audit_reports(audit_opinion);

-- =====================================================
-- 4. DIRECTORS REPORT TABLE
-- Stores directors' report on company activities
-- =====================================================
CREATE TABLE IF NOT EXISTS directors_reports (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    fiscal_period_id BIGINT NOT NULL REFERENCES fiscal_periods(id) ON DELETE CASCADE,
    nature_of_business TEXT, -- Description of business activities
    review_of_operations TEXT, -- Review of financial results and operations
    directors_names TEXT, -- List of directors and changes during year
    directors_interests TEXT, -- Directors' interests in contracts
    share_capital_info TEXT, -- Information about share capital
    dividends_info TEXT, -- Dividend declarations
    property_plant_equipment TEXT, -- PPE movements
    events_after_reporting_date TEXT, -- Post-balance sheet events
    going_concern_assessment TEXT, -- Going concern statement
    secretary_info VARCHAR(255), -- Company secretary details
    auditors_info VARCHAR(255), -- Auditors appointment/resignation
    special_resolutions TEXT, -- Special resolutions passed
    report_date DATE NOT NULL,
    approved_by VARCHAR(255), -- Approving director name
    approved_by_title VARCHAR(100), -- Approving director title
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, fiscal_period_id)
);

CREATE INDEX idx_directors_reports_company_period ON directors_reports(company_id, fiscal_period_id);

-- =====================================================
-- 5. STATEMENT OF CHANGES IN EQUITY TABLE
-- Stores equity movements for the period
-- =====================================================
CREATE TABLE IF NOT EXISTS equity_movements (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    fiscal_period_id BIGINT NOT NULL REFERENCES fiscal_periods(id) ON DELETE CASCADE,
    equity_component VARCHAR(100) NOT NULL, -- 'share_capital', 'retained_earnings', 'reserves', etc.
    opening_balance NUMERIC(15, 2) NOT NULL DEFAULT 0,
    profit_loss NUMERIC(15, 2) DEFAULT 0,
    dividends NUMERIC(15, 2) DEFAULT 0,
    share_issues NUMERIC(15, 2) DEFAULT 0,
    share_buybacks NUMERIC(15, 2) DEFAULT 0,
    transfers_to_reserves NUMERIC(15, 2) DEFAULT 0,
    transfers_from_reserves NUMERIC(15, 2) DEFAULT 0,
    other_movements NUMERIC(15, 2) DEFAULT 0,
    other_movements_description TEXT,
    closing_balance NUMERIC(15, 2) NOT NULL DEFAULT 0,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_equity_movements_company_period ON equity_movements(company_id, fiscal_period_id);
CREATE INDEX idx_equity_movements_component ON equity_movements(equity_component);

-- =====================================================
-- 6. COMPANY EXTENDED INFO TABLE
-- Additional company information for financial reports
-- =====================================================
CREATE TABLE IF NOT EXISTS company_extended_info (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE UNIQUE,
    nature_of_business TEXT, -- Detailed business description
    directors_names TEXT, -- Full list of directors with qualifications
    company_secretary VARCHAR(255), -- Company secretary name
    business_address TEXT, -- Physical business address (if different from registered)
    postal_address TEXT, -- Postal address
    auditors_name VARCHAR(255), -- Auditing firm name
    auditors_address TEXT, -- Auditors' address
    auditors_registration VARCHAR(100), -- IRBA registration number
    bankers_name VARCHAR(255), -- Banking institution
    bankers_branch VARCHAR(255), -- Bank branch
    legal_advisors VARCHAR(255), -- Legal advisors (if applicable)
    year_of_incorporation INTEGER, -- Year company was incorporated
    country_of_incorporation VARCHAR(100) DEFAULT 'South Africa',
    accounting_framework VARCHAR(50) DEFAULT 'IFRS', -- 'IFRS', 'IFRS for SMEs', 'GAAP'
    functional_currency VARCHAR(10) DEFAULT 'ZAR', -- Functional currency code
    presentation_currency VARCHAR(10) DEFAULT 'ZAR', -- Presentation currency code
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_company_extended_info_company ON company_extended_info(company_id);

-- =====================================================
-- 7. INSERT DEFAULT FINANCIAL NOTES TEMPLATES
-- Standard IFRS-compliant note templates
-- =====================================================

-- Note: These will be inserted per company/period as needed
-- Template structure for common notes:
-- Note 1: Accounting Policies
-- Note 2: Revenue
-- Note 3: Operating Expenses
-- Note 4: Investment Income
-- Note 5: Finance Costs
-- Note 6: Taxation
-- Note 7: Property, Plant and Equipment
-- Note 8: Intangible Assets
-- Note 9: Investments
-- Note 10: Inventory
-- Note 11: Trade and Other Receivables
-- Note 12: Cash and Cash Equivalents
-- Note 13: Share Capital
-- Note 14: Reserves
-- Note 15: Borrowings
-- Note 16: Trade and Other Payables
-- Note 17: Provisions
-- Note 18: Commitments
-- Note 19: Contingencies
-- Note 20: Related Party Transactions
-- Note 21: Events After Reporting Date
-- Note 22: Going Concern

-- =====================================================
-- 8. GRANT PERMISSIONS
-- =====================================================

-- Grant permissions to application user (adjust username as needed)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO fin_app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO fin_app_user;

-- =====================================================
-- ROLLBACK SCRIPT (if needed)
-- =====================================================
-- DROP TABLE IF EXISTS equity_movements;
-- DROP TABLE IF EXISTS directors_reports;
-- DROP TABLE IF EXISTS audit_reports;
-- DROP TABLE IF EXISTS directors_responsibility_statements;
-- DROP TABLE IF EXISTS financial_notes;
-- DROP TABLE IF EXISTS company_extended_info;
