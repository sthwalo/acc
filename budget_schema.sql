-- Budget Generation and Strategic Planning Database Schema
-- Implementation for TASK 6.1: Budget Generation and Strategic Planning

-- Strategic Plans Table
CREATE TABLE strategic_plans (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    vision_statement TEXT,
    mission_statement TEXT,
    goals TEXT,
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, ACTIVE, ARCHIVED
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, title)
);

-- Strategic Priorities Table
CREATE TABLE strategic_priorities (
    id SERIAL PRIMARY KEY,
    strategic_plan_id INTEGER NOT NULL REFERENCES strategic_plans(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    priority_order INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Strategic Initiatives Table
CREATE TABLE strategic_initiatives (
    id SERIAL PRIMARY KEY,
    strategic_priority_id INTEGER NOT NULL REFERENCES strategic_priorities(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATE,
    end_date DATE,
    budget_allocated DECIMAL(15,2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PLANNED', -- PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Strategic Milestones Table
CREATE TABLE strategic_milestones (
    id SERIAL PRIMARY KEY,
    strategic_initiative_id INTEGER NOT NULL REFERENCES strategic_initiatives(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_date DATE,
    completion_date DATE,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, ACHIEVED, DELAYED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Budgets Table
CREATE TABLE budgets (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    fiscal_period_id INTEGER REFERENCES fiscal_periods(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    budget_year INTEGER NOT NULL,
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, APPROVED, ACTIVE, ARCHIVED
    total_revenue DECIMAL(15,2) DEFAULT 0,
    total_expenses DECIMAL(15,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    approved_by VARCHAR(255),
    UNIQUE(company_id, budget_year)
);

-- Budget Categories Table
CREATE TABLE budget_categories (
    id SERIAL PRIMARY KEY,
    budget_id INTEGER NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    category_type VARCHAR(50) NOT NULL, -- REVENUE, EXPENSE
    description TEXT,
    allocated_percentage DECIMAL(5,2), -- e.g., 60.00 for 60%
    total_allocated DECIMAL(15,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Budget Items Table
CREATE TABLE budget_items (
    id SERIAL PRIMARY KEY,
    budget_category_id INTEGER NOT NULL REFERENCES budget_categories(id) ON DELETE CASCADE,
    account_id INTEGER REFERENCES accounts(id), -- Link to chart of accounts
    description VARCHAR(255) NOT NULL,
    annual_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Budget Monthly Allocations Table (for month-to-month reporting)
CREATE TABLE budget_monthly_allocations (
    id SERIAL PRIMARY KEY,
    budget_item_id INTEGER NOT NULL REFERENCES budget_items(id) ON DELETE CASCADE,
    month_number INTEGER NOT NULL CHECK (month_number BETWEEN 1 AND 12), -- 1=Jan, 2=Feb, etc.
    allocated_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    actual_amount DECIMAL(15,2) DEFAULT 0,
    variance_amount DECIMAL(15,2) GENERATED ALWAYS AS (actual_amount - allocated_amount) STORED,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(budget_item_id, month_number)
);

-- Budget Projections Table (Multi-year planning)
CREATE TABLE budget_projections (
    id SERIAL PRIMARY KEY,
    budget_id INTEGER NOT NULL REFERENCES budgets(id) ON DELETE CASCADE,
    projection_year INTEGER NOT NULL,
    growth_rate DECIMAL(5,2), -- e.g., 50.00 for 50% growth
    projected_revenue DECIMAL(15,2),
    projected_expenses DECIMAL(15,2),
    assumptions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_strategic_plans_company ON strategic_plans(company_id);
CREATE INDEX idx_strategic_plans_status ON strategic_plans(status);
CREATE INDEX idx_budgets_company ON budgets(company_id);
CREATE INDEX idx_budgets_year ON budgets(budget_year);
CREATE INDEX idx_budgets_status ON budgets(status);
CREATE INDEX idx_budget_items_account ON budget_items(account_id);
CREATE INDEX idx_budget_monthly_allocations_item ON budget_monthly_allocations(budget_item_id);
CREATE INDEX idx_budget_monthly_allocations_month ON budget_monthly_allocations(month_number);

-- Comments for documentation
COMMENT ON TABLE strategic_plans IS 'Strategic plans with vision, mission, and goals for organizations';
COMMENT ON TABLE strategic_priorities IS 'Key priority areas within a strategic plan (e.g., Academic Excellence, Infrastructure)';
COMMENT ON TABLE strategic_initiatives IS 'Specific initiatives with timelines and budget allocations';
COMMENT ON TABLE strategic_milestones IS 'Measurable outcomes and KPIs for initiatives';
COMMENT ON TABLE budgets IS 'Annual budgets linked to fiscal periods and companies';
COMMENT ON TABLE budget_categories IS 'Budget categories (Revenue/Expense) with percentage allocations';
COMMENT ON TABLE budget_items IS 'Individual budget line items linked to chart of accounts with annual totals';
COMMENT ON TABLE budget_monthly_allocations IS 'Monthly budget allocations and actual spending with variance tracking';
COMMENT ON TABLE budget_projections IS 'Multi-year financial projections and growth modeling';