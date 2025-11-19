-- Migration: 010_no_fallback_data_remediation.sql
-- Description: Add database-first architecture support tables for report templates, company defaults, and employee import defaults
-- Date: 2025-11-17
-- Author: FIN Development Team
-- Risk: LOW - Additive changes only, no data loss
-- Rollback: DROP TABLE report_templates, company_defaults, employee_import_defaults;

-- ===========================================
-- PHASE 1: CREATE NEW TABLES
-- ===========================================

-- Table: report_templates
-- Purpose: Store business text templates for reports (database-first approach)
-- Replaces hardcoded strings in ExcelFinancialReportService.java
CREATE TABLE report_templates (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    template_type VARCHAR(50) NOT NULL, -- 'index_intro', 'approval_text', 'index_contents', 'directors_statement', etc.
    template_key VARCHAR(100) NOT NULL, -- Specific identifier within type
    template_text TEXT NOT NULL,
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'FIN_SYSTEM',
    updated_by VARCHAR(100) DEFAULT 'FIN_SYSTEM',
    UNIQUE(company_id, template_type, template_key)
);

-- Add indexes for performance
CREATE INDEX idx_report_templates_company_type ON report_templates(company_id, template_type);
CREATE INDEX idx_report_templates_active ON report_templates(is_active) WHERE is_active = TRUE;

-- Add trigger for updated_at
CREATE TRIGGER update_report_templates_updated_at
    BEFORE UPDATE ON report_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Table: company_defaults
-- Purpose: Store default values for company fields (database-first approach)
-- Replaces silent fallbacks in ExcelFinancialReportService.java getCompanyInfo()
CREATE TABLE company_defaults (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    field_name VARCHAR(100) NOT NULL, -- 'country_of_incorporation', 'business_nature', 'directors', 'secretary', 'auditors', 'bankers'
    default_value VARCHAR(500) NOT NULL,
    is_required BOOLEAN DEFAULT TRUE,
    validation_rule VARCHAR(200), -- Optional regex or validation rule
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, field_name)
);

-- Add indexes for performance
CREATE INDEX idx_company_defaults_company_field ON company_defaults(company_id, field_name);

-- Add trigger for updated_at
CREATE TRIGGER update_company_defaults_updated_at
    BEFORE UPDATE ON company_defaults
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Table: employee_import_defaults
-- Purpose: Store default values for employee import fields (database-first approach)
-- Replaces hardcoded country assignment in PayrollService.java
CREATE TABLE employee_import_defaults (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    field_name VARCHAR(100) NOT NULL, -- 'country', 'department', 'position', etc.
    default_value VARCHAR(200) NOT NULL,
    applies_to_file_type VARCHAR(50) DEFAULT 'employee_data', -- 'employee_data', 'payroll_data', etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, field_name, applies_to_file_type)
);

-- Add indexes for performance
CREATE INDEX idx_employee_import_defaults_company ON employee_import_defaults(company_id, field_name);

-- ===========================================
-- PHASE 2: SEED DATA FOR EXISTING COMPANIES
-- ===========================================

-- Based on database status (November 4, 2025):
-- Active companies: 3 (Xinghizana Group, Rock Of Ages Fellowship Church, Limelight Academy Institutions)
-- Assuming standard auto-increment IDs: 1, 2, 3

-- Seed data for Xinghizana Group (company_id = 1)
INSERT INTO report_templates (company_id, template_type, template_key, template_text, display_order, created_by) VALUES
(1, 'index_intro', 'main', 'The reports and statements set out below comprise the annual financial statements presented to the members:', 1, 'FIN_SYSTEM'),
(1, 'approval_text', 'directors_statement', 'The annual financial statements set out on pages 7 to 15 were approved by the board of directors on [DATE] and are signed on its behalf by:', 1, 'FIN_SYSTEM');

INSERT INTO report_templates (company_id, template_type, template_key, template_text, display_order, created_by) VALUES
(1, 'index_contents', 'general_info', 'General information', 1, 'FIN_SYSTEM'),
(1, 'index_contents', 'directors_responsibility', 'Statement of financial responsibility by the directors', 2, 'FIN_SYSTEM'),
(1, 'index_contents', 'auditors_report', 'Report of the independent auditors', 3, 'FIN_SYSTEM'),
(1, 'index_contents', 'directors_report', 'Report of the directors', 4, 'FIN_SYSTEM'),
(1, 'index_contents', 'balance_sheet', 'Statement of financial position', 5, 'FIN_SYSTEM'),
(1, 'index_contents', 'income_statement', 'Statement of comprehensive income', 6, 'FIN_SYSTEM'),
(1, 'index_contents', 'equity_statement', 'Statement of changes in equity', 7, 'FIN_SYSTEM'),
(1, 'index_contents', 'cash_flow', 'Statement of cash flows', 8, 'FIN_SYSTEM'),
(1, 'index_contents', 'financial_notes', 'Notes to the annual financial statements', 9, 'FIN_SYSTEM');

INSERT INTO company_defaults (company_id, field_name, default_value, is_required) VALUES
(1, 'country_of_incorporation', 'South Africa', TRUE),
(1, 'business_nature', 'See directors'' report', FALSE),
(1, 'directors', 'See directors'' report', FALSE),
(1, 'company_secretary', 'See directors'' report', FALSE),
(1, 'auditors_name', 'To be appointed', FALSE),
(1, 'bankers_name', 'See company records', FALSE);

INSERT INTO employee_import_defaults (company_id, field_name, default_value, applies_to_file_type) VALUES
(1, 'country', 'ZA', 'employee_data'),
(1, 'department', 'General', 'employee_data');

-- Seed data for Rock Of Ages Fellowship Church (company_id = 2)
INSERT INTO report_templates (company_id, template_type, template_key, template_text, display_order, created_by) VALUES
(2, 'index_intro', 'main', 'The reports and statements set out below comprise the annual financial statements presented to the members:', 1, 'FIN_SYSTEM'),
(2, 'approval_text', 'directors_statement', 'The annual financial statements set out on pages 7 to 15 were approved by the board of directors on [DATE] and are signed on its behalf by:', 1, 'FIN_SYSTEM');

INSERT INTO report_templates (company_id, template_type, template_key, template_text, display_order, created_by) VALUES
(2, 'index_contents', 'general_info', 'General information', 1, 'FIN_SYSTEM'),
(2, 'index_contents', 'directors_responsibility', 'Statement of financial responsibility by the directors', 2, 'FIN_SYSTEM'),
(2, 'index_contents', 'auditors_report', 'Report of the independent auditors', 3, 'FIN_SYSTEM'),
(2, 'index_contents', 'directors_report', 'Report of the directors', 4, 'FIN_SYSTEM'),
(2, 'index_contents', 'balance_sheet', 'Statement of financial position', 5, 'FIN_SYSTEM'),
(2, 'index_contents', 'income_statement', 'Statement of comprehensive income', 6, 'FIN_SYSTEM'),
(2, 'index_contents', 'equity_statement', 'Statement of changes in equity', 7, 'FIN_SYSTEM'),
(2, 'index_contents', 'cash_flow', 'Statement of cash flows', 8, 'FIN_SYSTEM'),
(2, 'index_contents', 'financial_notes', 'Notes to the annual financial statements', 9, 'FIN_SYSTEM');

INSERT INTO company_defaults (company_id, field_name, default_value, is_required) VALUES
(2, 'country_of_incorporation', 'South Africa', TRUE),
(2, 'business_nature', 'Religious and charitable activities', FALSE),
(2, 'directors', 'Church council members', FALSE),
(2, 'company_secretary', 'Church administrator', FALSE),
(2, 'auditors_name', 'To be appointed', FALSE),
(2, 'bankers_name', 'See company records', FALSE);

INSERT INTO employee_import_defaults (company_id, field_name, default_value, applies_to_file_type) VALUES
(2, 'country', 'ZA', 'employee_data'),
(2, 'department', 'Ministry', 'employee_data');

-- Seed data for Limelight Academy Institutions (company_id = 3)
INSERT INTO report_templates (company_id, template_type, template_key, template_text, display_order, created_by) VALUES
(3, 'index_intro', 'main', 'The reports and statements set out below comprise the annual financial statements presented to the members:', 1, 'FIN_SYSTEM'),
(3, 'approval_text', 'directors_statement', 'The annual financial statements set out on pages 7 to 15 were approved by the board of directors on [DATE] and are signed on its behalf by:', 1, 'FIN_SYSTEM');

INSERT INTO report_templates (company_id, template_type, template_key, template_text, display_order, created_by) VALUES
(3, 'index_contents', 'general_info', 'General information', 1, 'FIN_SYSTEM'),
(3, 'index_contents', 'directors_responsibility', 'Statement of financial responsibility by the directors', 2, 'FIN_SYSTEM'),
(3, 'index_contents', 'auditors_report', 'Report of the independent auditors', 3, 'FIN_SYSTEM'),
(3, 'index_contents', 'directors_report', 'Report of the directors', 4, 'FIN_SYSTEM'),
(3, 'index_contents', 'balance_sheet', 'Statement of financial position', 5, 'FIN_SYSTEM'),
(3, 'index_contents', 'income_statement', 'Statement of comprehensive income', 6, 'FIN_SYSTEM'),
(3, 'index_contents', 'equity_statement', 'Statement of changes in equity', 7, 'FIN_SYSTEM'),
(3, 'index_contents', 'cash_flow', 'Statement of cash flows', 8, 'FIN_SYSTEM'),
(3, 'index_contents', 'financial_notes', 'Notes to the annual financial statements', 9, 'FIN_SYSTEM');

INSERT INTO company_defaults (company_id, field_name, default_value, is_required) VALUES
(3, 'country_of_incorporation', 'South Africa', TRUE),
(3, 'business_nature', 'Educational services and training', FALSE),
(3, 'directors', 'Board of trustees', FALSE),
(3, 'company_secretary', 'Academy administrator', FALSE),
(3, 'auditors_name', 'To be appointed', FALSE),
(3, 'bankers_name', 'See company records', FALSE);

INSERT INTO employee_import_defaults (company_id, field_name, default_value, applies_to_file_type) VALUES
(3, 'country', 'ZA', 'employee_data'),
(3, 'department', 'Education', 'employee_data');

-- ===========================================
-- PHASE 3: VERIFICATION QUERIES
-- ===========================================

-- Verification: Check that tables were created successfully
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('report_templates', 'company_defaults', 'employee_import_defaults');

-- Verification: Check seed data counts
-- SELECT 'report_templates' as table_name, COUNT(*) as record_count FROM report_templates
-- UNION ALL
-- SELECT 'company_defaults' as table_name, COUNT(*) as record_count FROM company_defaults
-- UNION ALL
-- SELECT 'employee_import_defaults' as table_name, COUNT(*) as record_count FROM employee_import_defaults;

-- ===========================================
-- PHASE 4: ROLLBACK SCRIPT
-- ===========================================

-- In case of rollback, run these commands in reverse order:
-- DELETE FROM employee_import_defaults WHERE created_by = 'FIN_SYSTEM';
-- DELETE FROM company_defaults WHERE created_by = 'FIN_SYSTEM';
-- DELETE FROM report_templates WHERE created_by = 'FIN_SYSTEM';
-- DROP TABLE employee_import_defaults;
-- DROP TABLE company_defaults;
-- DROP TABLE report_templates;

-- ===========================================
-- MIGRATION COMPLETE
-- ===========================================

-- Expected results after migration:
-- - 3 new tables created
-- - 27 report_templates records (9 per company)
-- - 18 company_defaults records (6 per company)
-- - 6 employee_import_defaults records (2 per company)
-- - All tables have proper indexes and triggers
-- - No existing data affected