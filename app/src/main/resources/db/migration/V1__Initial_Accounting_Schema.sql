-- Create companies table first as it's referenced by many other tables
CREATE TABLE IF NOT EXISTS companies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    registration_number TEXT,
    tax_number TEXT,
    address TEXT,
    contact_email TEXT,
    contact_phone TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Create fiscal_periods table next as it's referenced by journal_entries
CREATE TABLE IF NOT EXISTS fiscal_periods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    company_id INTEGER NOT NULL,
    period_name TEXT NOT NULL,
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    is_closed INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id)
);

-- Create account_types table
CREATE TABLE IF NOT EXISTS account_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    normal_balance TEXT NOT NULL CHECK (normal_balance IN ('D', 'C')),
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Insert default account types
INSERT OR IGNORE INTO account_types (code, name, normal_balance, description) VALUES
    ('A', 'Asset', 'D', 'Resources owned by the company'),
    ('L', 'Liability', 'C', 'Obligations of the company'),
    ('E', 'Equity', 'C', 'Owner''s claim on assets'),
    ('R', 'Revenue', 'C', 'Income from operations'),
    ('X', 'Expense', 'D', 'Costs incurred to generate revenue');

-- Create account_categories table
CREATE TABLE IF NOT EXISTS account_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    account_type_id INTEGER NOT NULL,
    company_id INTEGER NOT NULL,
    is_active INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_type_id) REFERENCES account_types(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create trigger for account_categories updated_at
CREATE TRIGGER IF NOT EXISTS trig_account_categories_updated_at 
AFTER UPDATE ON account_categories
BEGIN
    UPDATE account_categories SET updated_at = DATETIME('now')
    WHERE id = NEW.id;
END;

-- Create accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_code TEXT NOT NULL,
    account_name TEXT NOT NULL,
    description TEXT,
    category_id INTEGER NOT NULL,
    parent_account_id INTEGER,
    company_id INTEGER NOT NULL,
    is_active INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES account_categories(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (parent_account_id) REFERENCES accounts(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE ON UPDATE CASCADE,
    UNIQUE(company_id, account_code)
);

-- Create trigger for accounts updated_at
CREATE TRIGGER IF NOT EXISTS trig_accounts_updated_at 
AFTER UPDATE ON accounts
BEGIN
    UPDATE accounts SET updated_at = DATETIME('now')
    WHERE id = NEW.id;
END;

-- Create index for account lookups
CREATE INDEX IF NOT EXISTS idx_accounts_company ON accounts(company_id, is_active);
CREATE INDEX IF NOT EXISTS idx_accounts_category ON accounts(category_id);
CREATE INDEX IF NOT EXISTS idx_accounts_parent ON accounts(parent_account_id) WHERE parent_account_id IS NOT NULL;

-- Create transaction_types table
CREATE TABLE IF NOT EXISTS transaction_types (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    company_id INTEGER NOT NULL,
    is_active INTEGER DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create index for transaction_types lookups
CREATE INDEX IF NOT EXISTS idx_transaction_types_company ON transaction_types(company_id, is_active);

-- Create journal_entries table
CREATE TABLE IF NOT EXISTS journal_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    reference TEXT NOT NULL,
    entry_date TEXT NOT NULL,
    description TEXT,
    transaction_type_id INTEGER,
    fiscal_period_id INTEGER NOT NULL,
    company_id INTEGER NOT NULL,
    created_by TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_type_id) REFERENCES transaction_types(id) ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create trigger for journal_entries updated_at
CREATE TRIGGER IF NOT EXISTS trig_journal_entries_updated_at 
AFTER UPDATE ON journal_entries
BEGIN
    UPDATE journal_entries SET updated_at = DATETIME('now')
    WHERE id = NEW.id;
END;

-- Create journal_entry_lines table
CREATE TABLE IF NOT EXISTS journal_entry_lines (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    journal_entry_id INTEGER NOT NULL,
    account_id INTEGER NOT NULL,
    debit_amount REAL DEFAULT 0,
    credit_amount REAL DEFAULT 0,
    description TEXT,
    reference TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CHECK ((debit_amount > 0 AND credit_amount = 0) OR (credit_amount > 0 AND debit_amount = 0))
);

-- Create transaction_mapping_rules table for automated transaction categorization
CREATE TABLE IF NOT EXISTS transaction_mapping_rules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    company_id INTEGER NOT NULL,
    rule_name TEXT NOT NULL,
    description TEXT,
    match_type TEXT NOT NULL CHECK (match_type IN ('CONTAINS', 'STARTS_WITH', 'ENDS_WITH', 'EQUALS', 'REGEX')),
    match_value TEXT NOT NULL,
    account_id INTEGER NOT NULL,
    is_active INTEGER DEFAULT 1 CHECK (is_active IN (0, 1)),
    priority INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Create trigger for transaction_mapping_rules updated_at
CREATE TRIGGER IF NOT EXISTS trig_transaction_mapping_rules_updated_at 
AFTER UPDATE ON transaction_mapping_rules
BEGIN
    UPDATE transaction_mapping_rules SET updated_at = DATETIME('now')
    WHERE id = NEW.id;
END;

-- Create bank_transactions table
CREATE TABLE IF NOT EXISTS bank_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    company_id INTEGER NOT NULL,
    fiscal_period_id INTEGER NOT NULL,
    transaction_date TEXT NOT NULL,
    description TEXT,
    reference TEXT,
    amount REAL NOT NULL,
    running_balance REAL,
    transaction_type TEXT,
    bank_account TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Create index for better performance on transaction lookups
CREATE INDEX IF NOT EXISTS idx_journal_entries_company_date ON journal_entries(company_id, entry_date);
CREATE INDEX IF NOT EXISTS idx_journal_entry_lines_account ON journal_entry_lines(account_id);
CREATE INDEX IF NOT EXISTS idx_transaction_mapping_rules_company ON transaction_mapping_rules(company_id, is_active, priority);
CREATE INDEX IF NOT EXISTS idx_bank_transactions_company_date ON bank_transactions(company_id, transaction_date);
