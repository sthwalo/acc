-- Payroll System Database Schema for FIN Financial Management System
-- Compatible with PostgreSQL and designed to integrate with existing schema

-- ===== EMPLOYEES TABLE =====
CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    employee_number VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20),
    position VARCHAR(100),
    department VARCHAR(100),
    hire_date DATE NOT NULL,
    termination_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    
    -- Address Information
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    province VARCHAR(100),
    postal_code VARCHAR(10),
    country VARCHAR(2) DEFAULT 'ZA',
    
    -- Banking Information
    bank_name VARCHAR(100),
    account_holder_name VARCHAR(255),
    account_number VARCHAR(50),
    branch_code VARCHAR(20),
    account_type VARCHAR(20) DEFAULT 'SAVINGS',
    
    -- Employment Details
    employment_type VARCHAR(20) DEFAULT 'PERMANENT', -- PERMANENT, CONTRACT, TEMPORARY
    salary_type VARCHAR(20) DEFAULT 'MONTHLY', -- MONTHLY, WEEKLY, HOURLY, DAILY
    basic_salary DECIMAL(15,2) NOT NULL,
    overtime_rate DECIMAL(5,2) DEFAULT 1.5,
    
    -- Tax Information
    tax_number VARCHAR(50),
    tax_rebate_code VARCHAR(10) DEFAULT 'A',
    uif_number VARCHAR(50),
    medical_aid_number VARCHAR(50),
    pension_fund_number VARCHAR(50),
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- ===== PAYROLL PERIODS TABLE =====
CREATE TABLE payroll_periods (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    fiscal_period_id BIGINT REFERENCES fiscal_periods(id),
    period_name VARCHAR(100) NOT NULL,
    pay_date DATE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    period_type VARCHAR(20) DEFAULT 'MONTHLY', -- WEEKLY, MONTHLY, QUARTERLY
    status VARCHAR(20) DEFAULT 'OPEN', -- OPEN, PROCESSED, APPROVED, PAID, CLOSED
    total_gross_pay DECIMAL(15,2) DEFAULT 0,
    total_deductions DECIMAL(15,2) DEFAULT 0,
    total_net_pay DECIMAL(15,2) DEFAULT 0,
    employee_count INTEGER DEFAULT 0,
    
    -- Approval Workflow
    processed_at TIMESTAMP WITH TIME ZONE,
    processed_by VARCHAR(100),
    approved_at TIMESTAMP WITH TIME ZONE,
    approved_by VARCHAR(100),
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- ===== PAYSLIPS TABLE =====
CREATE TABLE payslips (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    payroll_period_id BIGINT NOT NULL REFERENCES payroll_periods(id),
    payslip_number VARCHAR(50) UNIQUE NOT NULL,
    
    -- Salary Information
    basic_salary DECIMAL(15,2) NOT NULL,
    overtime_hours DECIMAL(8,2) DEFAULT 0,
    overtime_amount DECIMAL(15,2) DEFAULT 0,
    
    -- Earnings
    gross_salary DECIMAL(15,2) NOT NULL,
    housing_allowance DECIMAL(15,2) DEFAULT 0,
    transport_allowance DECIMAL(15,2) DEFAULT 0,
    medical_allowance DECIMAL(15,2) DEFAULT 0,
    other_allowances DECIMAL(15,2) DEFAULT 0,
    commission DECIMAL(15,2) DEFAULT 0,
    bonus DECIMAL(15,2) DEFAULT 0,
    total_earnings DECIMAL(15,2) NOT NULL,
    
    -- Statutory Deductions
    paye_tax DECIMAL(15,2) DEFAULT 0,
    uif_employee DECIMAL(15,2) DEFAULT 0,
    uif_employer DECIMAL(15,2) DEFAULT 0,
    
    -- Other Deductions
    medical_aid DECIMAL(15,2) DEFAULT 0,
    pension_fund DECIMAL(15,2) DEFAULT 0,
    loan_deduction DECIMAL(15,2) DEFAULT 0,
    other_deductions DECIMAL(15,2) DEFAULT 0,
    total_deductions DECIMAL(15,2) NOT NULL,
    
    -- Net Pay
    net_pay DECIMAL(15,2) NOT NULL,
    
    -- Tax Certificate Info
    annual_gross DECIMAL(15,2),
    annual_paye DECIMAL(15,2),
    annual_uif DECIMAL(15,2),
    
    -- Status
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, APPROVED, PAID, EXPORTED
    payment_method VARCHAR(20) DEFAULT 'EFT', -- EFT, CASH, CHEQUE
    payment_date DATE,
    payment_reference VARCHAR(100),
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- ===== DEDUCTIONS TABLE =====
CREATE TABLE deductions (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    employee_id BIGINT REFERENCES employees(id), -- NULL for company-wide deductions
    deduction_code VARCHAR(20) NOT NULL,
    deduction_name VARCHAR(100) NOT NULL,
    deduction_type VARCHAR(20) NOT NULL, -- FIXED, PERCENTAGE, STATUTORY
    amount DECIMAL(15,2),
    percentage DECIMAL(5,2),
    is_pre_tax BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    effective_from DATE,
    effective_to DATE,
    
    -- For loan deductions
    total_loan_amount DECIMAL(15,2),
    remaining_balance DECIMAL(15,2),
    installment_amount DECIMAL(15,2),
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- ===== BENEFITS TABLE =====
CREATE TABLE benefits (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    employee_id BIGINT REFERENCES employees(id), -- NULL for company-wide benefits
    benefit_code VARCHAR(20) NOT NULL,
    benefit_name VARCHAR(100) NOT NULL,
    benefit_type VARCHAR(20) NOT NULL, -- FIXED, PERCENTAGE, ALLOWANCE
    amount DECIMAL(15,2),
    percentage DECIMAL(5,2),
    is_taxable BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    effective_from DATE,
    effective_to DATE,
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- ===== TAX CONFIGURATION TABLE =====
CREATE TABLE tax_configurations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    tax_year INTEGER NOT NULL,
    tax_rebate_primary DECIMAL(15,2) NOT NULL DEFAULT 17235, -- 2024 primary rebate
    tax_rebate_secondary DECIMAL(15,2) NOT NULL DEFAULT 9444, -- 2024 secondary rebate (65+)
    tax_rebate_tertiary DECIMAL(15,2) NOT NULL DEFAULT 3145, -- 2024 tertiary rebate (75+)
    
    -- UIF Configuration
    uif_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0100, -- 1%
    uif_max_earnings DECIMAL(15,2) NOT NULL DEFAULT 17712, -- Monthly max
    
    -- Skills Development Levy
    sdl_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0100, -- 1%
    sdl_threshold DECIMAL(15,2) NOT NULL DEFAULT 500000, -- Annual threshold
    
    is_active BOOLEAN DEFAULT TRUE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- ===== TAX BRACKETS TABLE =====
CREATE TABLE tax_brackets (
    id BIGSERIAL PRIMARY KEY,
    tax_configuration_id BIGINT NOT NULL REFERENCES tax_configurations(id),
    bracket_order INTEGER NOT NULL,
    min_amount DECIMAL(15,2) NOT NULL,
    max_amount DECIMAL(15,2), -- NULL for top bracket
    rate DECIMAL(5,4) NOT NULL,
    cumulative_tax DECIMAL(15,2) NOT NULL DEFAULT 0,
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ===== PAYROLL JOURNAL ENTRIES TABLE =====
-- This connects payroll to the existing financial system
CREATE TABLE payroll_journal_entries (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    payroll_period_id BIGINT NOT NULL REFERENCES payroll_periods(id),
    journal_entry_id BIGINT REFERENCES journal_entries(id), -- Link to main accounting system
    entry_type VARCHAR(20) NOT NULL, -- GROSS_PAY, PAYE, UIF, DEDUCTIONS, NET_PAY
    description TEXT,
    total_amount DECIMAL(15,2) NOT NULL,
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- ===== EMPLOYEE LEAVE TABLE =====
CREATE TABLE employee_leave (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    leave_type VARCHAR(20) NOT NULL, -- ANNUAL, SICK, MATERNITY, PATERNITY, UNPAID
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_taken DECIMAL(5,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, CANCELLED
    reason TEXT,
    affects_pay BOOLEAN DEFAULT FALSE,
    
    -- Approval
    approved_by VARCHAR(100),
    approved_at TIMESTAMP WITH TIME ZONE,
    
    -- Audit Fields
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- ===== INDEXES FOR PERFORMANCE =====
CREATE INDEX idx_employees_company_id ON employees(company_id);
CREATE INDEX idx_employees_employee_number ON employees(employee_number);
CREATE INDEX idx_employees_is_active ON employees(is_active);

CREATE INDEX idx_payroll_periods_company_id ON payroll_periods(company_id);
CREATE INDEX idx_payroll_periods_status ON payroll_periods(status);
CREATE INDEX idx_payroll_periods_pay_date ON payroll_periods(pay_date);

CREATE INDEX idx_payslips_company_id ON payslips(company_id);
CREATE INDEX idx_payslips_employee_id ON payslips(employee_id);
CREATE INDEX idx_payslips_payroll_period_id ON payslips(payroll_period_id);
CREATE INDEX idx_payslips_status ON payslips(status);

CREATE INDEX idx_deductions_company_id ON deductions(company_id);
CREATE INDEX idx_deductions_employee_id ON deductions(employee_id);
CREATE INDEX idx_deductions_is_active ON deductions(is_active);

CREATE INDEX idx_benefits_company_id ON benefits(company_id);
CREATE INDEX idx_benefits_employee_id ON benefits(employee_id);
CREATE INDEX idx_benefits_is_active ON benefits(is_active);

-- ===== SAMPLE TAX BRACKETS FOR 2024 TAX YEAR (South Africa) =====
-- These will be inserted via the PayrollService during initialization

-- VIEWS FOR REPORTING
CREATE VIEW v_employee_summary AS
SELECT 
    e.id,
    e.company_id,
    e.employee_number,
    e.first_name || ' ' || e.last_name as full_name,
    e.position,
    e.department,
    e.basic_salary,
    e.employment_type,
    e.hire_date,
    e.is_active,
    c.name as company_name
FROM employees e
JOIN companies c ON e.company_id = c.id
WHERE e.is_active = TRUE;

CREATE VIEW v_payslip_summary AS
SELECT 
    p.id,
    p.payslip_number,
    p.company_id,
    pp.period_name,
    pp.pay_date,
    e.employee_number,
    e.first_name || ' ' || e.last_name as employee_name,
    p.total_earnings,
    p.total_deductions,
    p.net_pay,
    p.status
FROM payslips p
JOIN employees e ON p.employee_id = e.id
JOIN payroll_periods pp ON p.payroll_period_id = pp.id;
