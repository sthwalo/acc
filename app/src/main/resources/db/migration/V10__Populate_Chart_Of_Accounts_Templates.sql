-- Reconstructed V10 migration: Populate Chart Of Accounts Templates
-- Generated from live database dump. These INSERTs are idempotent and
-- will only insert rows when an industry+account_code does not already exist.
-- Please review before applying to other environments.

-- Included reconstructed inserts (generated from drimacc_db)
-- The inserts are stored in `backups/V10_reconstructed_inserts.sql` for review.
-- Applying them here so Flyway can apply this migration deterministically.

-- Begin inserts

-- NOTE: Each INSERT uses a WHERE NOT EXISTS check to avoid duplicates.

-- (The following lines were generated from the running database and will be applied if missing)

-- Inserts for industries 5..14

-- Begin reconstructed inserts from backups/V10_reconstructed_inserts.sql (inlined for Flyway compatibility)
-- Reconstructed inserts for chart_of_accounts_templates (industries 5..14)
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1000', 'ASSETS', 'ASSET', NULL, 1, true, 0.00, 'Asset accounts' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1000');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1100', 'NON-CURRENT ASSETS', 'ASSET', NULL, 2, true, 0.00, 'Long-term assets' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1100');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1110', 'Property, Plant and Equipment', 'ASSET', NULL, 3, true, 0.00, 'Infrastructure assets' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1110');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1111', 'Water Treatment Plants', 'ASSET', NULL, 4, true, 0.00, 'Treatment facilities' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1111');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1112', 'Pumping Stations', 'ASSET', NULL, 4, false, 0.00, 'Water pumps and stations' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1112');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1113', 'Pipe Networks', 'ASSET', NULL, 4, false, 0.00, 'Water distribution pipes' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1113');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1114', 'Vehicles', 'ASSET', NULL, 4, false, 0.00, 'Service vehicles' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1114');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1200', 'CURRENT ASSETS', 'ASSET', NULL, 2, true, 0.00, 'Short-term assets' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1200');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1210', 'Inventories', 'ASSET', NULL, 3, true, 0.00, 'Chemicals, spare parts' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1210');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1220', 'Trade Receivables', 'ASSET', NULL, 3, true, 0.00, 'Customer receivables' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1220');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1230', 'Cash and Cash Equivalents', 'ASSET', NULL, 3, true, 0.00, 'Bank accounts' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1230');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '2000', 'EQUITY AND LIABILITIES', 'LIABILITY', NULL, 1, true, 0.00, 'Equity and liability accounts' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='2000');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '2100', 'EQUITY', 'EQUITY', NULL, 2, true, 0.00, 'Owner''s equity' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='2100');
-- Reconstructed inserts for chart_of_accounts_templates (industries 5..14)
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1000', 'ASSETS', 'ASSET', NULL, 1, true, 0.00, 'Asset accounts' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1000');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1100', 'NON-CURRENT ASSETS', 'ASSET', NULL, 2, true, 0.00, 'Long-term assets' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1100');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1110', 'Property, Plant and Equipment', 'ASSET', NULL, 3, true, 0.00, 'Infrastructure assets' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1110');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1111', 'Water Treatment Plants', 'ASSET', NULL, 4, true, 0.00, 'Treatment facilities' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1111');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1112', 'Pumping Stations', 'ASSET', NULL, 4, false, 0.00, 'Water pumps and stations' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1112');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1113', 'Pipe Networks', 'ASSET', NULL, 4, false, 0.00, 'Water distribution pipes' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1113');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1114', 'Vehicles', 'ASSET', NULL, 4, false, 0.00, 'Service vehicles' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1114');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1200', 'CURRENT ASSETS', 'ASSET', NULL, 2, true, 0.00, 'Short-term assets' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1200');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1210', 'Inventories', 'ASSET', NULL, 3, true, 0.00, 'Chemicals, spare parts' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1210');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1220', 'Trade Receivables', 'ASSET', NULL, 3, true, 0.00, 'Customer receivables' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1220');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '1230', 'Cash and Cash Equivalents', 'ASSET', NULL, 3, true, 0.00, 'Bank accounts' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='1230');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '2000', 'EQUITY AND LIABILITIES', 'LIABILITY', NULL, 1, true, 0.00, 'Equity and liability accounts' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='2000');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '2100', 'EQUITY', 'EQUITY', NULL, 2, true, 0.00, 'Owner''s equity' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='2100');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '2110', 'Share Capital', 'EQUITY', NULL, 3, true, 0.00, 'Initial capital' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='2110');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '2120', 'Retained Earnings', 'EQUITY', NULL, 3, true, 0.00, 'Accumulated profits' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='2120');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '2200', 'NON-CURRENT LIABILITIES', 'LIABILITY', NULL, 2, true, 0.00, 'Long-term liabilities' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='2200');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '2210', 'Loans and Borrowings', 'LIABILITY', NULL, 3, true, 0.00, 'Infrastructure financing' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='2210');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '2300', 'CURRENT LIABILITIES', 'LIABILITY', NULL, 2, true, 0.00, 'Short-term liabilities' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='2300');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '2310', 'Trade Payables', 'LIABILITY', NULL, 3, true, 0.00, 'Chemical and service suppliers' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='2310');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '2320', 'Tax Liabilities', 'LIABILITY', NULL, 3, true, 0.00, 'VAT, PAYE, UIF' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='2320');
INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type, parent_template_id, level, is_required, default_balance, description, created_at, updated_at) SELECT 5, '3000', 'INCOME', 'REVENUE', NULL, 1, true, 0.00, 'Revenue accounts' WHERE NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id=5 AND account_code='3000');

-- End of reconstructed inserts

-- End of reconstructed inserts

-- End of reconstructed inserts


-- V10__Populate_Chart_Of_Accounts_Templates.sql
-- Populate chart of accounts templates with IFRS-compliant structures
-- Only populate industries that don't already have templates

DO $$
BEGIN
    -- Water Supply; Sewerage, Waste Management (36-39)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 5) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (5, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (5, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (5, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Infrastructure assets'),
        (5, '1111', 'Water Treatment Plants', NULL, 4, true, 'Treatment facilities'),
        (5, '1112', 'Pumping Stations', NULL, 4, false, 'Water pumps and stations'),
        (5, '1113', 'Pipe Networks', NULL, 4, false, 'Water distribution pipes'),
        (5, '1114', 'Vehicles', NULL, 4, false, 'Service vehicles'),
        (5, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (5, '1210', 'Inventories', NULL, 3, true, 'Chemicals, spare parts'),
        (5, '1220', 'Trade Receivables', NULL, 3, true, 'Customer receivables'),
        (5, '1230', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (5, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (5, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (5, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (5, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (5, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (5, '2210', 'Loans and Borrowings', NULL, 3, true, 'Infrastructure financing'),
        (5, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (5, '2310', 'Trade Payables', NULL, 3, true, 'Chemical and service suppliers'),
        (5, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (5, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (5, '3100', 'REVENUE', NULL, 2, true, 'Water and waste services'),
        (5, '3110', 'Water Sales', NULL, 3, true, 'Water supply revenue'),
        (5, '3120', 'Waste Management', NULL, 3, true, 'Sewerage and waste services'),
        (5, '3130', 'Other Services', NULL, 3, false, 'Consulting, maintenance'),
        (5, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (5, '4100', 'COST OF SALES', NULL, 2, true, 'Service delivery costs'),
        (5, '4110', 'Chemicals and Treatment', NULL, 3, true, 'Water treatment chemicals'),
        (5, '4120', 'Energy Costs', NULL, 3, true, 'Electricity for pumping'),
        (5, '4130', 'Maintenance', NULL, 3, true, 'Infrastructure maintenance'),
        (5, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (5, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (5, '4220', 'Depreciation', NULL, 3, true, 'Asset depreciation'),
        (5, '4230', 'Insurance', NULL, 3, true, 'Infrastructure insurance'),
        (5, '4240', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Construction (41-43)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 6) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (6, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (6, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (6, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Construction equipment'),
        (6, '1111', 'Heavy Machinery', NULL, 4, true, 'Excavators, bulldozers'),
        (6, '1112', 'Vehicles', NULL, 4, false, 'Transport vehicles'),
        (6, '1113', 'Tools and Equipment', NULL, 4, false, 'Construction tools'),
        (6, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (6, '1210', 'Construction in Progress', NULL, 3, true, 'Work in progress'),
        (6, '1220', 'Trade Receivables', NULL, 3, true, 'Customer receivables'),
        (6, '1230', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (6, '1240', 'Retentions Receivable', NULL, 3, false, 'Contract retentions'),
        (6, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (6, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (6, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (6, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (6, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (6, '2210', 'Loans and Borrowings', NULL, 3, true, 'Construction financing'),
        (6, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (6, '2310', 'Trade Payables', NULL, 3, true, 'Material and subcontractor payables'),
        (6, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (6, '2330', 'Progress Payments', NULL, 3, false, 'Customer progress payments'),
        (6, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (6, '3100', 'REVENUE', NULL, 2, true, 'Construction revenue'),
        (6, '3110', 'Contract Revenue', NULL, 3, true, 'Construction contract income'),
        (6, '3120', 'Variation Orders', NULL, 3, false, 'Contract variations'),
        (6, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (6, '4100', 'COST OF SALES', NULL, 2, true, 'Direct construction costs'),
        (6, '4110', 'Materials', NULL, 3, true, 'Building materials'),
        (6, '4120', 'Subcontractors', NULL, 3, true, 'Subcontractor costs'),
        (6, '4130', 'Labor', NULL, 3, true, 'Construction labor'),
        (6, '4140', 'Equipment Rental', NULL, 3, true, 'Heavy equipment rental'),
        (6, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (6, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (6, '4220', 'Depreciation', NULL, 3, true, 'Equipment depreciation'),
        (6, '4230', 'Insurance', NULL, 3, true, 'Construction insurance'),
        (6, '4240', 'Marketing', NULL, 3, true, 'Business development'),
        (6, '4250', 'Office Expenses', NULL, 3, true, 'Administrative costs'),
        (6, '4260', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Wholesale and Retail Trade (45-47)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 7) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (7, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (7, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (7, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Store fixtures, vehicles'),
        (7, '1111', 'Store Fixtures', NULL, 4, true, 'Shelving, displays'),
        (7, '1112', 'Vehicles', NULL, 4, false, 'Delivery vehicles'),
        (7, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (7, '1210', 'Inventories', NULL, 3, true, 'Goods for sale'),
        (7, '1220', 'Trade Receivables', NULL, 3, true, 'Customer receivables'),
        (7, '1230', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (7, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (7, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (7, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (7, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (7, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (7, '2210', 'Loans and Borrowings', NULL, 3, true, 'Business financing'),
        (7, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (7, '2310', 'Trade Payables', NULL, 3, true, 'Supplier accounts'),
        (7, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (7, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (7, '3100', 'REVENUE', NULL, 2, true, 'Sales revenue'),
        (7, '3110', 'Product Sales', NULL, 3, true, 'Goods sales'),
        (7, '3120', 'Service Revenue', NULL, 3, false, 'Additional services'),
        (7, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (7, '4100', 'COST OF SALES', NULL, 2, true, 'Cost of goods sold'),
        (7, '4110', 'Purchases', NULL, 3, true, 'Goods purchased'),
        (7, '4120', 'Freight In', NULL, 3, true, 'Shipping costs'),
        (7, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (7, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (7, '4220', 'Rent', NULL, 3, true, 'Store rent'),
        (7, '4230', 'Utilities', NULL, 3, true, 'Electricity, water'),
        (7, '4240', 'Marketing', NULL, 3, true, 'Advertising, promotions'),
        (7, '4250', 'Depreciation', NULL, 3, true, 'Asset depreciation'),
        (7, '4260', 'Insurance', NULL, 3, true, 'Business insurance'),
        (7, '4270', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Transportation and Storage (49-53)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 8) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (8, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (8, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (8, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Vehicles, equipment'),
        (8, '1111', 'Vehicles', NULL, 4, true, 'Trucks, cars'),
        (8, '1112', 'Equipment', NULL, 4, false, 'Loading equipment'),
        (8, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (8, '1210', 'Inventories', NULL, 3, true, 'Fuel, supplies'),
        (8, '1220', 'Trade Receivables', NULL, 3, true, 'Customer receivables'),
        (8, '1230', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (8, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (8, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (8, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (8, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (8, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (8, '2210', 'Loans and Borrowings', NULL, 3, true, 'Vehicle financing'),
        (8, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (8, '2310', 'Trade Payables', NULL, 3, true, 'Fuel and service suppliers'),
        (8, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (8, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (8, '3100', 'REVENUE', NULL, 2, true, 'Transportation revenue'),
        (8, '3110', 'Freight Revenue', NULL, 3, true, 'Goods transportation'),
        (8, '3120', 'Storage Revenue', NULL, 3, true, 'Warehousing services'),
        (8, '3130', 'Other Services', NULL, 3, false, 'Additional services'),
        (8, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (8, '4100', 'COST OF SALES', NULL, 2, true, 'Direct transportation costs'),
        (8, '4110', 'Fuel Costs', NULL, 3, true, 'Vehicle fuel'),
        (8, '4120', 'Vehicle Maintenance', NULL, 3, true, 'Repairs and servicing'),
        (8, '4130', 'Driver Costs', NULL, 3, true, 'Driver salaries'),
        (8, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (8, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (8, '4220', 'Depreciation', NULL, 3, true, 'Vehicle depreciation'),
        (8, '4230', 'Insurance', NULL, 3, true, 'Vehicle and cargo insurance'),
        (8, '4240', 'Tolls and Permits', NULL, 3, true, 'Road tolls, licenses'),
        (8, '4250', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Accommodation and Food Service Activities (55-56)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 9) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (9, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (9, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (9, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Buildings, furniture'),
        (9, '1111', 'Buildings', NULL, 4, true, 'Hotel/restaurant premises'),
        (9, '1112', 'Furniture and Fixtures', NULL, 4, true, 'Tables, chairs'),
        (9, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (9, '1210', 'Inventories', NULL, 3, true, 'Food, beverages'),
        (9, '1220', 'Trade Receivables', NULL, 3, true, 'Customer receivables'),
        (9, '1230', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (9, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (9, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (9, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (9, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (9, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (9, '2210', 'Loans and Borrowings', NULL, 3, true, 'Property financing'),
        (9, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (9, '2310', 'Trade Payables', NULL, 3, true, 'Supplier accounts'),
        (9, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (9, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (9, '3100', 'REVENUE', NULL, 2, true, 'Hospitality revenue'),
        (9, '3110', 'Room Revenue', NULL, 3, true, 'Accommodation income'),
        (9, '3120', 'Food and Beverage', NULL, 3, true, 'Restaurant sales'),
        (9, '3130', 'Other Services', NULL, 3, false, 'Conferences, events'),
        (9, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (9, '4100', 'COST OF SALES', NULL, 2, true, 'Cost of goods sold'),
        (9, '4110', 'Food and Beverage Costs', NULL, 3, true, 'Ingredients, beverages'),
        (9, '4120', 'Room Costs', NULL, 3, true, 'Room maintenance'),
        (9, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (9, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (9, '4220', 'Utilities', NULL, 3, true, 'Electricity, water, gas'),
        (9, '4230', 'Marketing', NULL, 3, true, 'Advertising, promotions'),
        (9, '4240', 'Depreciation', NULL, 3, true, 'Asset depreciation'),
        (9, '4250', 'Insurance', NULL, 3, true, 'Property and liability insurance'),
        (9, '4260', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Information and Communication (58-63)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 10) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (10, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (10, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (10, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Computers, servers'),
        (10, '1111', 'Computer Equipment', NULL, 4, true, 'Hardware assets'),
        (10, '1112', 'Software', NULL, 4, true, 'Software licenses'),
        (10, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (10, '1210', 'Trade Receivables', NULL, 3, true, 'Customer receivables'),
        (10, '1220', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (10, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (10, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (10, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (10, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (10, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (10, '2210', 'Loans and Borrowings', NULL, 3, true, 'Equipment financing'),
        (10, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (10, '2310', 'Trade Payables', NULL, 3, true, 'Service providers'),
        (10, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (10, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (10, '3100', 'REVENUE', NULL, 2, true, 'Service revenue'),
        (10, '3110', 'Consulting Services', NULL, 3, true, 'IT consulting'),
        (10, '3120', 'Software Development', NULL, 3, true, 'Custom development'),
        (10, '3130', 'Hosting Services', NULL, 3, true, 'Server hosting'),
        (10, '3140', 'Other Services', NULL, 3, false, 'Additional services'),
        (10, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (10, '4100', 'COST OF SALES', NULL, 2, true, 'Direct service costs'),
        (10, '4110', 'Subcontractors', NULL, 3, true, 'Third-party services'),
        (10, '4120', 'Software Licenses', NULL, 3, true, 'Third-party software'),
        (10, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (10, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (10, '4220', 'Office Rent', NULL, 3, true, 'Office space'),
        (10, '4230', 'Utilities', NULL, 3, true, 'Electricity, internet'),
        (10, '4240', 'Marketing', NULL, 3, true, 'Business development'),
        (10, '4250', 'Depreciation', NULL, 3, true, 'Equipment depreciation'),
        (10, '4260', 'Insurance', NULL, 3, true, 'Professional liability'),
        (10, '4270', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Financial and Insurance Activities (64-66)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 11) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (11, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (11, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (11, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Office equipment'),
        (11, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (11, '1210', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (11, '1220', 'Loans Receivable', NULL, 3, true, 'Outstanding loans'),
        (11, '1230', 'Investment Securities', NULL, 3, true, 'Investment portfolio'),
        (11, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (11, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (11, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (11, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (11, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (11, '2210', 'Deposits', NULL, 3, true, 'Customer deposits'),
        (11, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (11, '2310', 'Trade Payables', NULL, 3, true, 'Supplier accounts'),
        (11, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (11, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (11, '3100', 'REVENUE', NULL, 2, true, 'Financial services revenue'),
        (11, '3110', 'Interest Income', NULL, 3, true, 'Loan interest'),
        (11, '3120', 'Fee Income', NULL, 3, true, 'Service fees'),
        (11, '3130', 'Investment Income', NULL, 3, true, 'Portfolio returns'),
        (11, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (11, '4100', 'COST OF SALES', NULL, 2, true, 'Direct costs'),
        (11, '4110', 'Interest Expense', NULL, 3, true, 'Deposit interest'),
        (11, '4120', 'Bad Debts', NULL, 3, true, 'Loan losses'),
        (11, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (11, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (11, '4220', 'Office Expenses', NULL, 3, true, 'Administrative costs'),
        (11, '4230', 'Marketing', NULL, 3, true, 'Business development'),
        (11, '4240', 'Depreciation', NULL, 3, true, 'Equipment depreciation'),
        (11, '4250', 'Insurance', NULL, 3, true, 'Professional liability'),
        (11, '4260', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Real Estate Activities (68-68)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 12) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (12, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (12, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (12, '1110', 'Investment Property', NULL, 3, true, 'Rental properties'),
        (12, '1120', 'Property, Plant and Equipment', NULL, 3, true, 'Office equipment'),
        (12, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (12, '1210', 'Trade Receivables', NULL, 3, true, 'Tenant receivables'),
        (12, '1220', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (12, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (12, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (12, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (12, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (12, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (12, '2210', 'Mortgages', NULL, 3, true, 'Property financing'),
        (12, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (12, '2310', 'Trade Payables', NULL, 3, true, 'Property management suppliers'),
        (12, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (12, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (12, '3100', 'REVENUE', NULL, 2, true, 'Property income'),
        (12, '3110', 'Rental Income', NULL, 3, true, 'Property rentals'),
        (12, '3120', 'Property Sales', NULL, 3, true, 'Property disposals'),
        (12, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (12, '4100', 'COST OF SALES', NULL, 2, true, 'Property costs'),
        (12, '4110', 'Property Maintenance', NULL, 3, true, 'Repairs and maintenance'),
        (12, '4120', 'Property Taxes', NULL, 3, true, 'Municipal rates'),
        (12, '4130', 'Insurance', NULL, 3, true, 'Property insurance'),
        (12, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (12, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (12, '4220', 'Property Management Fees', NULL, 3, true, 'Management costs'),
        (12, '4230', 'Marketing', NULL, 3, true, 'Property marketing'),
        (12, '4240', 'Depreciation', NULL, 3, true, 'Asset depreciation'),
        (12, '4250', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Professional, Scientific and Technical Activities (69-75)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 14) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (14, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (14, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (14, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Office equipment, computers'),
        (14, '1111', 'Computer Equipment', NULL, 4, true, 'Hardware assets'),
        (14, '1112', 'Software', NULL, 4, true, 'Software licenses'),
        (14, '1113', 'Office Furniture', NULL, 4, false, 'Desks, chairs'),
        (14, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (14, '1210', 'Trade Receivables', NULL, 3, true, 'Client receivables'),
        (14, '1220', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (14, '1230', 'Work in Progress', NULL, 3, false, 'Unbilled services'),
        (14, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (14, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (14, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (14, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (14, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (14, '2210', 'Loans and Borrowings', NULL, 3, true, 'Equipment financing'),
        (14, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (14, '2310', 'Trade Payables', NULL, 3, true, 'Supplier accounts'),
        (14, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (14, '2330', 'Accrued Expenses', NULL, 3, false, 'Outstanding expenses'),
        (14, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (14, '3100', 'REVENUE', NULL, 2, true, 'Professional services revenue'),
        (14, '3110', 'Consulting Fees', NULL, 3, true, 'Professional consulting'),
        (14, '3120', 'Project Fees', NULL, 3, true, 'Project-based services'),
        (14, '3130', 'Training Revenue', NULL, 3, false, 'Training and education'),
        (14, '3140', 'Other Services', NULL, 3, false, 'Additional services'),
        (14, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (14, '4100', 'COST OF SALES', NULL, 2, true, 'Direct service costs'),
        (14, '4110', 'Subcontractors', NULL, 3, true, 'Third-party consultants'),
        (14, '4120', 'Travel Expenses', NULL, 3, true, 'Business travel'),
        (14, '4130', 'Training Materials', NULL, 3, false, 'Course materials'),
        (14, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (14, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (14, '4220', 'Office Rent', NULL, 3, true, 'Office space'),
        (14, '4230', 'Utilities', NULL, 3, true, 'Electricity, internet'),
        (14, '4240', 'Marketing', NULL, 3, true, 'Business development'),
        (14, '4250', 'Professional Development', NULL, 3, true, 'Certifications, courses'),
        (14, '4260', 'Depreciation', NULL, 3, true, 'Equipment depreciation'),
        (14, '4270', 'Insurance', NULL, 3, true, 'Professional liability'),
        (14, '4280', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Administrative and Support Service Activities (77-82)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 15) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (15, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (15, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (15, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Office equipment'),
        (15, '1111', 'Computer Equipment', NULL, 4, true, 'Hardware assets'),
        (15, '1112', 'Office Furniture', NULL, 4, false, 'Desks, chairs'),
        (15, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (15, '1210', 'Trade Receivables', NULL, 3, true, 'Client receivables'),
        (15, '1220', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (15, '1230', 'Prepaid Expenses', NULL, 3, false, 'Advance payments'),
        (15, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (15, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (15, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (15, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (15, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (15, '2210', 'Loans and Borrowings', NULL, 3, true, 'Equipment financing'),
        (15, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (15, '2310', 'Trade Payables', NULL, 3, true, 'Supplier accounts'),
        (15, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (15, '2330', 'Accrued Expenses', NULL, 3, false, 'Outstanding expenses'),
        (15, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (15, '3100', 'REVENUE', NULL, 2, true, 'Service revenue'),
        (15, '3110', 'Management Fees', NULL, 3, true, 'Administrative services'),
        (15, '3120', 'Consulting Fees', NULL, 3, true, 'Consulting services'),
        (15, '3130', 'Support Services', NULL, 3, true, 'Technical support'),
        (15, '3140', 'Other Services', NULL, 3, false, 'Additional services'),
        (15, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (15, '4100', 'COST OF SALES', NULL, 2, true, 'Direct service costs'),
        (15, '4110', 'Subcontractors', NULL, 3, true, 'Third-party services'),
        (15, '4120', 'Travel Expenses', NULL, 3, true, 'Business travel'),
        (15, '4130', 'Communication', NULL, 3, true, 'Phone, internet'),
        (15, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (15, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (15, '4220', 'Office Rent', NULL, 3, true, 'Office space'),
        (15, '4230', 'Utilities', NULL, 3, true, 'Electricity, water'),
        (15, '4240', 'Marketing', NULL, 3, true, 'Business development'),
        (15, '4250', 'Depreciation', NULL, 3, true, 'Equipment depreciation'),
        (15, '4260', 'Insurance', NULL, 3, true, 'Professional liability'),
        (15, '4270', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Manufacturing (10-33)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 17) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (17, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (17, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (17, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Manufacturing equipment'),
        (17, '1111', 'Machinery', NULL, 4, true, 'Production machinery'),
        (17, '1112', 'Buildings', NULL, 4, true, 'Factory premises'),
        (17, '1113', 'Vehicles', NULL, 4, false, 'Delivery vehicles'),
        (17, '1120', 'Intangible Assets', NULL, 3, false, 'Patents, trademarks'),
        (17, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (17, '1210', 'Inventories', NULL, 3, true, 'Raw materials, work in progress, finished goods'),
        (17, '1211', 'Raw Materials', NULL, 4, true, 'Production inputs'),
        (17, '1212', 'Work in Progress', NULL, 4, true, 'Partially completed goods'),
        (17, '1213', 'Finished Goods', NULL, 4, true, 'Completed products'),
        (17, '1220', 'Trade Receivables', NULL, 3, true, 'Customer receivables'),
        (17, '1230', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (17, '1240', 'Prepaid Expenses', NULL, 3, false, 'Advance payments'),
        (17, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (17, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (17, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (17, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (17, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (17, '2210', 'Loans and Borrowings', NULL, 3, true, 'Equipment financing'),
        (17, '2220', 'Debentures', NULL, 3, false, 'Long-term debt'),
        (17, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (17, '2310', 'Trade Payables', NULL, 3, true, 'Supplier accounts'),
        (17, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (17, '2330', 'Accrued Expenses', NULL, 3, true, 'Outstanding wages, utilities'),
        (17, '2340', 'Short-term Loans', NULL, 3, false, 'Working capital loans'),
        (17, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (17, '3100', 'REVENUE', NULL, 2, true, 'Sales revenue'),
        (17, '3110', 'Product Sales', NULL, 3, true, 'Finished goods sales'),
        (17, '3120', 'Service Revenue', NULL, 3, false, 'Installation, maintenance'),
        (17, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (17, '4100', 'COST OF SALES', NULL, 2, true, 'Cost of goods manufactured'),
        (17, '4110', 'Raw Materials', NULL, 3, true, 'Direct material costs'),
        (17, '4120', 'Direct Labor', NULL, 3, true, 'Production wages'),
        (17, '4130', 'Manufacturing Overhead', NULL, 3, true, 'Factory utilities, depreciation'),
        (17, '4140', 'Freight Out', NULL, 3, true, 'Shipping costs'),
        (17, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (17, '4210', 'Employee Costs', NULL, 3, true, 'Administrative salaries'),
        (17, '4220', 'Office Rent', NULL, 3, true, 'Administrative space'),
        (17, '4230', 'Utilities', NULL, 3, true, 'Office electricity, water'),
        (17, '4240', 'Marketing', NULL, 3, true, 'Sales and advertising'),
        (17, '4250', 'Depreciation', NULL, 3, true, 'Equipment depreciation'),
        (17, '4260', 'Insurance', NULL, 3, true, 'Property and liability'),
        (17, '4270', 'Research and Development', NULL, 3, false, 'Product development'),
        (17, '4280', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Electricity, Gas, Steam and Air Conditioning Supply (35-35)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 18) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (18, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (18, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (18, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Generation and distribution equipment'),
        (18, '1111', 'Generation Equipment', NULL, 4, true, 'Turbines, generators'),
        (18, '1112', 'Transmission Lines', NULL, 4, true, 'Power lines, substations'),
        (18, '1113', 'Distribution Equipment', NULL, 4, true, 'Transformers, meters'),
        (18, '1120', 'Intangible Assets', NULL, 3, false, 'Licenses, permits'),
        (18, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (18, '1210', 'Trade Receivables', NULL, 3, true, 'Customer receivables'),
        (18, '1220', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (18, '1230', 'Fuel Inventory', NULL, 3, true, 'Coal, gas reserves'),
        (18, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (18, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (18, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (18, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (18, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (18, '2210', 'Loans and Borrowings', NULL, 3, true, 'Infrastructure financing'),
        (18, '2220', 'Bonds', NULL, 3, false, 'Corporate bonds'),
        (18, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (18, '2310', 'Trade Payables', NULL, 3, true, 'Fuel and service suppliers'),
        (18, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (18, '2330', 'Customer Deposits', NULL, 3, false, 'Security deposits'),
        (18, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (18, '3100', 'REVENUE', NULL, 2, true, 'Utility revenue'),
        (18, '3110', 'Electricity Sales', NULL, 3, true, 'Power sales'),
        (18, '3120', 'Gas Sales', NULL, 3, true, 'Gas distribution'),
        (18, '3130', 'Steam Sales', NULL, 3, false, 'Steam supply'),
        (18, '3140', 'Connection Fees', NULL, 3, false, 'New connections'),
        (18, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (18, '4100', 'COST OF SALES', NULL, 2, true, 'Fuel and distribution costs'),
        (18, '4110', 'Fuel Costs', NULL, 3, true, 'Coal, gas, nuclear fuel'),
        (18, '4120', 'Transmission Costs', NULL, 3, true, 'Power transmission'),
        (18, '4130', 'Distribution Costs', NULL, 3, true, 'Local distribution'),
        (18, '4140', 'Maintenance', NULL, 3, true, 'Equipment maintenance'),
        (18, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (18, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (18, '4220', 'Depreciation', NULL, 3, true, 'Infrastructure depreciation'),
        (18, '4230', 'Insurance', NULL, 3, true, 'Property and liability'),
        (18, '4240', 'Regulatory Fees', NULL, 3, true, 'NERSA fees'),
        (18, '4250', 'Marketing', NULL, 3, true, 'Customer acquisition'),
        (18, '4260', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Water Supply; Sewerage, Waste Management and Remediation Activities (36-39)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 19) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (19, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (19, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (19, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Infrastructure assets'),
        (19, '1111', 'Water Treatment Plants', NULL, 4, true, 'Treatment facilities'),
        (19, '1112', 'Pumping Stations', NULL, 4, true, 'Water pumps, equipment'),
        (19, '1113', 'Pipelines', NULL, 4, true, 'Distribution network'),
        (19, '1114', 'Waste Facilities', NULL, 4, false, 'Landfill, recycling centers'),
        (19, '1120', 'Intangible Assets', NULL, 3, false, 'Water rights, permits'),
        (19, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (19, '1210', 'Trade Receivables', NULL, 3, true, 'Customer receivables'),
        (19, '1220', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (19, '1230', 'Chemicals Inventory', NULL, 3, true, 'Treatment chemicals'),
        (19, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (19, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (19, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (19, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (19, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (19, '2210', 'Loans and Borrowings', NULL, 3, true, 'Infrastructure financing'),
        (19, '2220', 'Bonds', NULL, 3, false, 'Municipal bonds'),
        (19, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (19, '2310', 'Trade Payables', NULL, 3, true, 'Chemical and service suppliers'),
        (19, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (19, '2330', 'Customer Deposits', NULL, 3, false, 'Connection deposits'),
        (19, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (19, '3100', 'REVENUE', NULL, 2, true, 'Utility revenue'),
        (19, '3110', 'Water Sales', NULL, 3, true, 'Water supply revenue'),
        (19, '3120', 'Sewerage Fees', NULL, 3, true, 'Wastewater treatment'),
        (19, '3130', 'Waste Collection', NULL, 3, true, 'Refuse collection'),
        (19, '3140', 'Connection Fees', NULL, 3, false, 'New service connections'),
        (19, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (19, '4100', 'COST OF SALES', NULL, 2, true, 'Treatment and distribution costs'),
        (19, '4110', 'Chemicals', NULL, 3, true, 'Water treatment chemicals'),
        (19, '4120', 'Electricity', NULL, 3, true, 'Pumping and treatment power'),
        (19, '4130', 'Maintenance', NULL, 3, true, 'Infrastructure maintenance'),
        (19, '4140', 'Waste Disposal', NULL, 3, true, 'Landfill and recycling costs'),
        (19, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (19, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (19, '4220', 'Depreciation', NULL, 3, true, 'Infrastructure depreciation'),
        (19, '4230', 'Insurance', NULL, 3, true, 'Property and liability'),
        (19, '4240', 'Regulatory Fees', NULL, 3, true, 'Environmental compliance'),
        (19, '4250', 'Marketing', NULL, 3, true, 'Public awareness campaigns'),
        (19, '4260', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Construction (41-43)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 20) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (20, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (20, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (20, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Construction equipment'),
        (20, '1111', 'Heavy Machinery', NULL, 4, true, 'Excavators, bulldozers'),
        (20, '1112', 'Vehicles', NULL, 4, false, 'Transport vehicles'),
        (20, '1113', 'Tools and Equipment', NULL, 4, false, 'Construction tools'),
        (20, '1120', 'Intangible Assets', NULL, 3, false, 'Contract rights'),
        (20, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (20, '1210', 'Construction in Progress', NULL, 3, true, 'Work in progress'),
        (20, '1220', 'Trade Receivables', NULL, 3, true, 'Customer receivables'),
        (20, '1230', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (20, '1240', 'Retentions Receivable', NULL, 3, false, 'Contract retentions'),
        (20, '1250', 'Materials Inventory', NULL, 3, true, 'Construction materials'),
        (20, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (20, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (20, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (20, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (20, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (20, '2210', 'Loans and Borrowings', NULL, 3, true, 'Construction financing'),
        (20, '2220', 'Retentions Payable', NULL, 3, false, 'Contractor retentions'),
        (20, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (20, '2310', 'Trade Payables', NULL, 3, true, 'Material and subcontractor payables'),
        (20, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (20, '2330', 'Progress Payments', NULL, 3, false, 'Customer progress payments'),
        (20, '2340', 'Accrued Expenses', NULL, 3, true, 'Outstanding wages, expenses'),
        (20, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (20, '3100', 'REVENUE', NULL, 2, true, 'Construction revenue'),
        (20, '3110', 'Contract Revenue', NULL, 3, true, 'Construction contract income'),
        (20, '3120', 'Variation Orders', NULL, 3, false, 'Contract variations'),
        (20, '3130', 'Material Sales', NULL, 3, false, 'Direct material sales'),
        (20, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (20, '4100', 'COST OF SALES', NULL, 2, true, 'Direct construction costs'),
        (20, '4110', 'Materials', NULL, 3, true, 'Building materials'),
        (20, '4120', 'Subcontractors', NULL, 3, true, 'Subcontractor costs'),
        (20, '4130', 'Labor', NULL, 3, true, 'Construction labor'),
        (20, '4140', 'Equipment Rental', NULL, 3, true, 'Heavy equipment rental'),
        (20, '4150', 'Fuel and Transport', NULL, 3, true, 'Site transport costs'),
        (20, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (20, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (20, '4220', 'Depreciation', NULL, 3, true, 'Equipment depreciation'),
        (20, '4230', 'Insurance', NULL, 3, true, 'Construction insurance'),
        (20, '4240', 'Marketing', NULL, 3, true, 'Business development'),
        (20, '4250', 'Office Expenses', NULL, 3, true, 'Administrative costs'),
        (20, '4260', 'Professional Fees', NULL, 3, false, 'Architect, engineer fees'),
        (20, '4270', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

    -- Other Service Activities (84-99)
    IF NOT EXISTS (SELECT 1 FROM chart_of_accounts_templates WHERE industry_id = 21) THEN
        INSERT INTO chart_of_accounts_templates (industry_id, account_code, account_name, account_type_id, level, is_required, description)
        VALUES
        (21, '1000', 'ASSETS', NULL, 1, true, 'Asset accounts'),
        (21, '1100', 'NON-CURRENT ASSETS', NULL, 2, true, 'Long-term assets'),
        (21, '1110', 'Property, Plant and Equipment', NULL, 3, true, 'Business equipment'),
        (21, '1111', 'Vehicles', NULL, 4, false, 'Service vehicles'),
        (21, '1112', 'Equipment', NULL, 4, true, 'Specialized tools'),
        (21, '1113', 'Furniture and Fixtures', NULL, 4, false, 'Office furniture'),
        (21, '1200', 'CURRENT ASSETS', NULL, 2, true, 'Short-term assets'),
        (21, '1210', 'Trade Receivables', NULL, 3, true, 'Customer receivables'),
        (21, '1220', 'Cash and Cash Equivalents', NULL, 3, true, 'Bank accounts'),
        (21, '1230', 'Inventories', NULL, 3, false, 'Supplies, parts'),
        (21, '1240', 'Prepaid Expenses', NULL, 3, false, 'Advance payments'),
        (21, '2000', 'EQUITY AND LIABILITIES', NULL, 1, true, 'Equity and liability accounts'),
        (21, '2100', 'EQUITY', NULL, 2, true, 'Owner''s equity'),
        (21, '2110', 'Share Capital', NULL, 3, true, 'Initial capital'),
        (21, '2120', 'Retained Earnings', NULL, 3, true, 'Accumulated profits'),
        (21, '2200', 'NON-CURRENT LIABILITIES', NULL, 2, true, 'Long-term liabilities'),
        (21, '2210', 'Loans and Borrowings', NULL, 3, true, 'Equipment financing'),
        (21, '2300', 'CURRENT LIABILITIES', NULL, 2, true, 'Short-term liabilities'),
        (21, '2310', 'Trade Payables', NULL, 3, true, 'Supplier accounts'),
        (21, '2320', 'Tax Liabilities', NULL, 3, true, 'VAT, PAYE, UIF'),
        (21, '2330', 'Accrued Expenses', NULL, 3, false, 'Outstanding expenses'),
        (21, '3000', 'INCOME', NULL, 1, true, 'Revenue accounts'),
        (21, '3100', 'REVENUE', NULL, 2, true, 'Service revenue'),
        (21, '3110', 'Service Fees', NULL, 3, true, 'Core service income'),
        (21, '3120', 'Product Sales', NULL, 3, false, 'Related product sales'),
        (21, '3130', 'Commissions', NULL, 3, false, 'Commission income'),
        (21, '3140', 'Other Income', NULL, 3, false, 'Miscellaneous revenue'),
        (21, '4000', 'EXPENSES', NULL, 1, true, 'Operating expenses'),
        (21, '4100', 'COST OF SALES', NULL, 2, true, 'Direct service costs'),
        (21, '4110', 'Materials and Supplies', NULL, 3, true, 'Service materials'),
        (21, '4120', 'Subcontractors', NULL, 3, false, 'Third-party services'),
        (21, '4130', 'Travel Expenses', NULL, 3, true, 'Business travel'),
        (21, '4140', 'Communication', NULL, 3, true, 'Phone, internet'),
        (21, '4200', 'OPERATING EXPENSES', NULL, 2, true, 'Administrative costs'),
        (21, '4210', 'Employee Costs', NULL, 3, true, 'Salaries and benefits'),
        (21, '4220', 'Office Rent', NULL, 3, true, 'Office space'),
        (21, '4230', 'Utilities', NULL, 3, true, 'Electricity, water'),
        (21, '4240', 'Marketing', NULL, 3, true, 'Business development'),
        (21, '4250', 'Depreciation', NULL, 3, true, 'Equipment depreciation'),
        (21, '4260', 'Insurance', NULL, 3, true, 'Business liability'),
        (21, '4270', 'Professional Fees', NULL, 3, false, 'Legal, accounting'),
        (21, '4280', 'Other Expenses', NULL, 3, false, 'Miscellaneous costs');
    END IF;

END $$;
