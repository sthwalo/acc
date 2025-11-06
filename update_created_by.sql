-- Update all created_by fields from 'SYSTEM' to 'FIN'
UPDATE asset_disposals SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE assets SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE benefits SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE deductions SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE depreciation_adjustments SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE depreciation_journal_entries_backup SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE depreciation_policies SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE depreciation_schedules SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE employee_leave SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE employees SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE journal_entries SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE payroll_journal_entries SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE payroll_journal_entries_backup SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE payroll_periods SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE payslips SET created_by = 'FIN' WHERE created_by = 'SYSTEM';
UPDATE tax_configurations SET created_by = 'FIN' WHERE created_by = 'SYSTEM';

-- Also update any 'system' (lowercase) to 'FIN'
UPDATE asset_disposals SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE assets SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE benefits SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE deductions SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE depreciation_adjustments SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE depreciation_journal_entries_backup SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE depreciation_policies SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE depreciation_schedules SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE employee_leave SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE employees SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE journal_entries SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE payroll_journal_entries SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE payroll_journal_entries_backup SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE payroll_periods SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE payslips SET created_by = 'FIN' WHERE created_by = 'system';
UPDATE tax_configurations SET created_by = 'FIN' WHERE created_by = 'system';
