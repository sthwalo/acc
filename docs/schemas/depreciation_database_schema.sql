-- Depreciation System Database Schema for FIN Financial Management System
-- Compatible with PostgreSQL and designed to integrate with existing schema

-- ===== ASSETS TABLE =====
-- This table stores fixed assets that can be depreciated
CREATE TABLE assets (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    asset_code VARCHAR(50) UNIQUE NOT NULL,
    asset_name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Asset Classification
    asset_category VARCHAR(100), -- VEHICLE, BUILDING, EQUIPMENT, FURNITURE, etc.
    asset_subcategory VARCHAR(100),

    -- Acquisition Details
    acquisition_date DATE NOT NULL,
    cost DECIMAL(15,2) NOT NULL,
    salvage_value DECIMAL(15,2) DEFAULT 0,
    useful_life_years INTEGER NOT NULL,

    -- Location and Assignment
    location VARCHAR(255),
    department VARCHAR(100),
    assigned_to VARCHAR(255), -- Employee name or department

    -- Asset Status
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, DISPOSED, SOLD, WRITTEN_OFF
    disposal_date DATE,
    disposal_value DECIMAL(15,2),

    -- Accounting Integration
    account_code VARCHAR(20), -- Link to chart of accounts
    accumulated_depreciation DECIMAL(15,2) DEFAULT 0,
    current_book_value DECIMAL(15,2),

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- ===== DEPRECIATION_SCHEDULES TABLE =====
-- Header table for depreciation calculations
CREATE TABLE depreciation_schedules (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    asset_id BIGINT REFERENCES assets(id), -- NULL for standalone calculations

    -- Schedule Identification
    schedule_number VARCHAR(50) UNIQUE NOT NULL,
    schedule_name VARCHAR(255),
    description TEXT,

    -- Calculation Parameters
    cost DECIMAL(15,2) NOT NULL,
    salvage_value DECIMAL(15,2) DEFAULT 0,
    useful_life_years INTEGER NOT NULL,
    depreciation_method VARCHAR(30) NOT NULL, -- STRAIGHT_LINE, DECLINING_BALANCE, FIN
    db_factor DECIMAL(5,2) DEFAULT 2.0, -- Declining balance factor
    convention VARCHAR(50), -- Half-Year, Mid-Quarter, etc.

    -- Results
    total_depreciation DECIMAL(15,2) NOT NULL,
    final_book_value DECIMAL(15,2) NOT NULL,

    -- Status and Workflow
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, CALCULATED, APPROVED, POSTED
    calculation_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP WITH TIME ZONE,
    approved_by VARCHAR(100),
    posted_at TIMESTAMP WITH TIME ZONE,
    posted_by VARCHAR(100),

    -- Journal Entry Integration
    journal_entry_id BIGINT REFERENCES journal_entries(id),

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- ===== DEPRECIATION_ENTRIES TABLE =====
-- Detail table for year-by-year depreciation entries
CREATE TABLE depreciation_entries (
    id BIGSERIAL PRIMARY KEY,
    depreciation_schedule_id BIGINT NOT NULL REFERENCES depreciation_schedules(id),

    -- Period Information
    year_number INTEGER NOT NULL,
    fiscal_year INTEGER, -- Calendar year for reporting
    period_start DATE,
    period_end DATE,

    -- Depreciation Amounts
    depreciation_amount DECIMAL(15,2) NOT NULL,
    cumulative_depreciation DECIMAL(15,2) NOT NULL,
    book_value DECIMAL(15,2) NOT NULL,

    -- Status
    status VARCHAR(20) DEFAULT 'CALCULATED', -- CALCULATED, POSTED, ADJUSTED

    -- Journal Entry Line Integration
    journal_entry_line_id BIGINT REFERENCES journal_entry_lines(id),

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ===== DEPRECIATION_ADJUSTMENTS TABLE =====
-- For tracking changes to depreciation calculations
CREATE TABLE depreciation_adjustments (
    id BIGSERIAL PRIMARY KEY,
    depreciation_schedule_id BIGINT NOT NULL REFERENCES depreciation_schedules(id),
    depreciation_entry_id BIGINT REFERENCES depreciation_entries(id),

    -- Adjustment Details
    adjustment_type VARCHAR(50) NOT NULL, -- REVISION, IMPAIRMENT, DISPOSAL, etc.
    adjustment_date DATE NOT NULL,
    description TEXT,

    -- Amount Changes
    previous_amount DECIMAL(15,2),
    adjusted_amount DECIMAL(15,2) NOT NULL,
    adjustment_amount DECIMAL(15,2) NOT NULL,

    -- Approval
    approved_by VARCHAR(100),
    approved_at TIMESTAMP WITH TIME ZONE,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- ===== DEPRECIATION_POLICIES TABLE =====
-- Company-wide depreciation policies and rules
CREATE TABLE depreciation_policies (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),

    -- Policy Details
    policy_name VARCHAR(255) NOT NULL,
    policy_code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,

    -- Default Settings
    default_method VARCHAR(30) DEFAULT 'STRAIGHT_LINE',
    default_convention VARCHAR(50),
    default_db_factor DECIMAL(5,2) DEFAULT 2.0,

    -- Category-specific Rules
    asset_category VARCHAR(100),
    min_useful_life INTEGER,
    max_useful_life INTEGER,
    default_useful_life INTEGER,

    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    is_default BOOLEAN DEFAULT FALSE,

    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- ===== INDEXES FOR PERFORMANCE =====
CREATE INDEX idx_assets_company_id ON assets(company_id);
CREATE INDEX idx_assets_asset_code ON assets(asset_code);
CREATE INDEX idx_assets_status ON assets(status);
CREATE INDEX idx_assets_category ON assets(asset_category);

CREATE INDEX idx_depreciation_schedules_company_id ON depreciation_schedules(company_id);
CREATE INDEX idx_depreciation_schedules_asset_id ON depreciation_schedules(asset_id);
CREATE INDEX idx_depreciation_schedules_status ON depreciation_schedules(status);
CREATE INDEX idx_depreciation_schedules_schedule_number ON depreciation_schedules(schedule_number);

CREATE INDEX idx_depreciation_entries_schedule_id ON depreciation_entries(depreciation_schedule_id);
CREATE INDEX idx_depreciation_entries_year_number ON depreciation_entries(year_number);
CREATE INDEX idx_depreciation_entries_fiscal_year ON depreciation_entries(fiscal_year);

CREATE INDEX idx_depreciation_adjustments_schedule_id ON depreciation_adjustments(depreciation_schedule_id);
CREATE INDEX idx_depreciation_adjustments_entry_id ON depreciation_adjustments(depreciation_entry_id);

CREATE INDEX idx_depreciation_policies_company_id ON depreciation_policies(company_id);
CREATE INDEX idx_depreciation_policies_is_active ON depreciation_policies(is_active);
CREATE INDEX idx_depreciation_policies_is_default ON depreciation_policies(is_default);

-- ===== CONSTRAINTS =====
ALTER TABLE depreciation_schedules
ADD CONSTRAINT chk_depreciation_method
CHECK (depreciation_method IN ('STRAIGHT_LINE', 'DECLINING_BALANCE', 'FIN'));

ALTER TABLE depreciation_schedules
ADD CONSTRAINT chk_schedule_status
CHECK (status IN ('DRAFT', 'CALCULATED', 'APPROVED', 'POSTED'));

ALTER TABLE assets
ADD CONSTRAINT chk_asset_status
CHECK (status IN ('ACTIVE', 'DISPOSED', 'SOLD', 'WRITTEN_OFF'));

ALTER TABLE depreciation_entries
ADD CONSTRAINT chk_entry_status
CHECK (status IN ('CALCULATED', 'POSTED', 'ADJUSTED'));

-- ===== VIEWS FOR REPORTING =====

-- Asset Depreciation Summary View
CREATE VIEW v_asset_depreciation_summary AS
SELECT
    a.id as asset_id,
    a.asset_code,
    a.asset_name,
    a.asset_category,
    a.cost as original_cost,
    a.accumulated_depreciation,
    a.current_book_value,
    a.status as asset_status,
    ds.id as latest_schedule_id,
    ds.schedule_number,
    ds.total_depreciation,
    ds.final_book_value as calculated_book_value,
    ds.depreciation_method,
    ds.calculation_date,
    c.name as company_name
FROM assets a
LEFT JOIN (
    SELECT asset_id, MAX(calculation_date) as latest_calc
    FROM depreciation_schedules
    WHERE status = 'CALCULATED' OR status = 'APPROVED' OR status = 'POSTED'
    GROUP BY asset_id
) latest ON a.id = latest.asset_id
LEFT JOIN depreciation_schedules ds ON ds.asset_id = a.id
    AND ds.calculation_date = latest.latest_calc
LEFT JOIN companies c ON a.company_id = c.id
WHERE a.status = 'ACTIVE';

-- Depreciation Schedule Detail View
CREATE VIEW v_depreciation_schedule_detail AS
SELECT
    ds.id as schedule_id,
    ds.schedule_number,
    ds.schedule_name,
    ds.cost,
    ds.salvage_value,
    ds.useful_life_years,
    ds.depreciation_method,
    ds.total_depreciation,
    ds.final_book_value,
    ds.status as schedule_status,
    de.year_number,
    de.fiscal_year,
    de.period_start,
    de.period_end,
    de.depreciation_amount,
    de.cumulative_depreciation,
    de.book_value,
    a.asset_code,
    a.asset_name,
    c.name as company_name
FROM depreciation_schedules ds
LEFT JOIN depreciation_entries de ON ds.id = de.depreciation_schedule_id
LEFT JOIN assets a ON ds.asset_id = a.id
LEFT JOIN companies c ON ds.company_id = c.id
ORDER BY ds.id, de.year_number;

-- Monthly Depreciation Report View
CREATE VIEW v_monthly_depreciation AS
SELECT
    c.name as company_name,
    DATE_TRUNC('month', de.period_start) as depreciation_month,
    COUNT(DISTINCT ds.id) as schedules_count,
    SUM(de.depreciation_amount) as monthly_depreciation,
    COUNT(DISTINCT a.id) as assets_count
FROM depreciation_entries de
JOIN depreciation_schedules ds ON de.depreciation_schedule_id = ds.id
LEFT JOIN assets a ON ds.asset_id = a.id
JOIN companies c ON ds.company_id = c.id
WHERE ds.status IN ('APPROVED', 'POSTED')
GROUP BY c.name, DATE_TRUNC('month', de.period_start)
ORDER BY depreciation_month, company_name;

-- ===== SAMPLE DATA INSERTS =====
-- These would be inserted by the DepreciationService during initialization

-- Sample Depreciation Policy
INSERT INTO depreciation_policies (
    company_id, policy_name, policy_code, description,
    default_method, default_useful_life, is_default
) VALUES (
    1, 'Standard Depreciation Policy', 'STD_DEP_001',
    'Standard depreciation policy for general assets',
    'STRAIGHT_LINE', 5, TRUE
);

-- Sample Asset Categories and Policies
INSERT INTO depreciation_policies (
    company_id, policy_name, policy_code, asset_category,
    default_method, default_useful_life, min_useful_life, max_useful_life
) VALUES
(1, 'Vehicle Depreciation', 'VEH_DEP_001', 'VEHICLE', 'DECLINING_BALANCE', 5, 3, 7),
(1, 'Building Depreciation', 'BLD_DEP_001', 'BUILDING', 'STRAIGHT_LINE', 20, 10, 50),
(1, 'Equipment Depreciation', 'EQP_DEP_001', 'EQUIPMENT', 'DECLINING_BALANCE', 7, 3, 15),
(1, 'Furniture Depreciation', 'FUR_DEP_001', 'FURNITURE', 'STRAIGHT_LINE', 10, 5, 15);