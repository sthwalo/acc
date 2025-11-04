-- ============================================================================
-- Import Limelight Academy Budget Data from November 2, 2025 Report
-- Strategic Plan Report: strategic_plan_report_1_20251102_103420.pdf
-- ============================================================================
-- This script populates the budget_categories, budget_items, and 
-- budget_monthly_allocations tables with the actual budget data
-- ============================================================================

-- Get the budget ID for Limelight Academy (Budget Year 2026)
DO $$
DECLARE
    v_budget_id INTEGER;
    v_company_id INTEGER;
    
    -- Category IDs
    v_cat_community INTEGER;
    v_cat_curriculum INTEGER;
    v_cat_infrastructure INTEGER;
    v_cat_salaries INTEGER;
    v_cat_training INTEGER;
    
    -- Item IDs
    v_item_community INTEGER;
    v_item_curriculum INTEGER;
    v_item_infrastructure INTEGER;
    v_item_salaries INTEGER;
    v_item_training INTEGER;
    
BEGIN
    -- Get Limelight Academy company ID
    SELECT id INTO v_company_id FROM companies WHERE name = 'Limelight Academy Institutions';
    
    IF v_company_id IS NULL THEN
        RAISE EXCEPTION 'Limelight Academy Institutions not found in companies table';
    END IF;
    
    -- Get the budget ID for year 2026
    SELECT id INTO v_budget_id FROM budgets 
    WHERE company_id = v_company_id AND budget_year = 2026;
    
    IF v_budget_id IS NULL THEN
        RAISE EXCEPTION 'Budget for year 2026 not found for Limelight Academy';
    END IF;
    
    RAISE NOTICE 'Found Budget ID: %, Company ID: %', v_budget_id, v_company_id;
    
    -- ========================================================================
    -- STEP 1: Create Budget Categories
    -- ========================================================================
    
    -- Delete existing categories and items to avoid duplicates
    DELETE FROM budget_monthly_allocations WHERE budget_item_id IN 
        (SELECT id FROM budget_items WHERE budget_category_id IN 
            (SELECT id FROM budget_categories WHERE budget_id = v_budget_id));
    DELETE FROM budget_items WHERE budget_category_id IN 
        (SELECT id FROM budget_categories WHERE budget_id = v_budget_id);
    DELETE FROM budget_categories WHERE budget_id = v_budget_id;
    
    -- Category 1: Community Engagement (R240,000)
    INSERT INTO budget_categories (budget_id, name, category_type, description, allocated_percentage, total_allocated)
    VALUES (v_budget_id, 'Community Engagement', 'EXPENSE', 'Community engagement and outreach programs', 7.50, 240000.00)
    RETURNING id INTO v_cat_community;
    
    -- Category 2: Curriculum & Materials (R240,000)
    INSERT INTO budget_categories (budget_id, name, category_type, description, allocated_percentage, total_allocated)
    VALUES (v_budget_id, 'Curriculum & Materials', 'EXPENSE', 'Curriculum development and learning materials', 7.50, 240000.00)
    RETURNING id INTO v_cat_curriculum;
    
    -- Category 3: Infrastructure & Equipment (R600,000)
    INSERT INTO budget_categories (budget_id, name, category_type, description, allocated_percentage, total_allocated)
    VALUES (v_budget_id, 'Infrastructure & Equipment', 'EXPENSE', 'Infrastructure development and equipment purchases', 18.75, 600000.00)
    RETURNING id INTO v_cat_infrastructure;
    
    -- Category 4: Staff Salaries (R2,041,200)
    INSERT INTO budget_categories (budget_id, name, category_type, description, allocated_percentage, total_allocated)
    VALUES (v_budget_id, 'Staff Salaries', 'EXPENSE', 'Employee salaries and benefits', 63.79, 2041200.00)
    RETURNING id INTO v_cat_salaries;
    
    -- Category 5: Staff Training (R78,800)
    INSERT INTO budget_categories (budget_id, name, category_type, description, allocated_percentage, total_allocated)
    VALUES (v_budget_id, 'Staff Training', 'EXPENSE', 'Staff professional development and training', 2.46, 78800.00)
    RETURNING id INTO v_cat_training;
    
    RAISE NOTICE 'Created 5 budget categories';
    
    -- ========================================================================
    -- STEP 2: Create Budget Items (one per category for simplicity)
    -- ========================================================================
    
    -- Item 1: Community Engagement
    INSERT INTO budget_items (budget_category_id, description, annual_amount, notes)
    VALUES (v_cat_community, 'Community Engagement Programs', 240000.00, 'Community outreach, events, and partnerships')
    RETURNING id INTO v_item_community;
    
    -- Item 2: Curriculum & Materials
    INSERT INTO budget_items (budget_category_id, description, annual_amount, notes)
    VALUES (v_cat_curriculum, 'Curriculum & Learning Materials', 240000.00, 'Educational materials, textbooks, and curriculum development')
    RETURNING id INTO v_item_curriculum;
    
    -- Item 3: Infrastructure & Equipment
    INSERT INTO budget_items (budget_category_id, description, annual_amount, notes)
    VALUES (v_cat_infrastructure, 'Infrastructure & Equipment', 600000.00, 'Facilities, technology, and equipment')
    RETURNING id INTO v_item_infrastructure;
    
    -- Item 4: Staff Salaries
    INSERT INTO budget_items (budget_category_id, description, annual_amount, notes)
    VALUES (v_cat_salaries, 'Staff Salaries', 2041200.00, 'Monthly employee salaries')
    RETURNING id INTO v_item_salaries;
    
    -- Item 5: Staff Training
    INSERT INTO budget_items (budget_category_id, description, annual_amount, notes)
    VALUES (v_cat_training, 'Staff Training', 78800.00, 'Professional development workshops and courses')
    RETURNING id INTO v_item_training;
    
    RAISE NOTICE 'Created 5 budget items';
    
    -- ========================================================================
    -- STEP 3: Create Monthly Allocations
    -- Data from ANNUAL OPERATIONAL BUDGET table in PDF report
    -- ========================================================================
    
    -- Community Engagement monthly allocations
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
    (v_item_community, 1, 30000.00),   -- Jan
    (v_item_community, 2, 30000.00),   -- Feb
    (v_item_community, 3, 50000.00),   -- Mar
    (v_item_community, 4, 50000.00),   -- Apr
    (v_item_community, 5, 50000.00),   -- May
    (v_item_community, 6, 50000.00),   -- Jun
    (v_item_community, 7, 30000.00),   -- Jul
    (v_item_community, 8, 25000.00),   -- Aug
    (v_item_community, 9, 30000.00),   -- Sep
    (v_item_community, 10, 15000.00),  -- Oct
    (v_item_community, 11, 15000.00),  -- Nov
    (v_item_community, 12, 10000.00);  -- Dec
    
    -- Curriculum & Materials monthly allocations
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
    (v_item_curriculum, 1, 60000.00),  -- Jan
    (v_item_curriculum, 2, 40000.00),  -- Feb
    (v_item_curriculum, 3, 30000.00),  -- Mar
    (v_item_curriculum, 4, 30000.00),  -- Apr
    (v_item_curriculum, 5, 30000.00),  -- May
    (v_item_curriculum, 6, 30000.00),  -- Jun
    (v_item_curriculum, 7, 10000.00),  -- Jul
    (v_item_curriculum, 8, 10000.00),  -- Aug
    (v_item_curriculum, 9, 0.00),      -- Sep
    (v_item_curriculum, 10, 0.00),     -- Oct
    (v_item_curriculum, 11, 0.00),     -- Nov
    (v_item_curriculum, 12, 0.00);     -- Dec
    
    -- Infrastructure & Equipment monthly allocations
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
    (v_item_infrastructure, 1, 250000.00),  -- Jan
    (v_item_infrastructure, 2, 150000.00),  -- Feb
    (v_item_infrastructure, 3, 50000.00),   -- Mar
    (v_item_infrastructure, 4, 50000.00),   -- Apr
    (v_item_infrastructure, 5, 50000.00),   -- May
    (v_item_infrastructure, 6, 50000.00),   -- Jun
    (v_item_infrastructure, 7, 0.00),       -- Jul
    (v_item_infrastructure, 8, 0.00),       -- Aug
    (v_item_infrastructure, 9, 0.00),       -- Sep
    (v_item_infrastructure, 10, 0.00),      -- Oct
    (v_item_infrastructure, 11, 0.00),      -- Nov
    (v_item_infrastructure, 12, 0.00);      -- Dec
    
    -- Staff Salaries monthly allocations (equal distribution)
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
    (v_item_salaries, 1, 170100.00),   -- Jan
    (v_item_salaries, 2, 170100.00),   -- Feb
    (v_item_salaries, 3, 170100.00),   -- Mar
    (v_item_salaries, 4, 170100.00),   -- Apr
    (v_item_salaries, 5, 170100.00),   -- May
    (v_item_salaries, 6, 170100.00),   -- Jun
    (v_item_salaries, 7, 170100.00),   -- Jul
    (v_item_salaries, 8, 170100.00),   -- Aug
    (v_item_salaries, 9, 170100.00),   -- Sep
    (v_item_salaries, 10, 170100.00),  -- Oct
    (v_item_salaries, 11, 170100.00),  -- Nov
    (v_item_salaries, 12, 170100.00);  -- Dec
    
    -- Staff Training monthly allocations (bi-annual)
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
    (v_item_training, 1, 0.00),        -- Jan
    (v_item_training, 2, 0.00),        -- Feb
    (v_item_training, 3, 39400.00),    -- Mar
    (v_item_training, 4, 0.00),        -- Apr
    (v_item_training, 5, 0.00),        -- May
    (v_item_training, 6, 39400.00),    -- Jun
    (v_item_training, 7, 0.00),        -- Jul
    (v_item_training, 8, 0.00),        -- Aug
    (v_item_training, 9, 0.00),        -- Sep
    (v_item_training, 10, 0.00),       -- Oct
    (v_item_training, 11, 0.00),       -- Nov
    (v_item_training, 12, 0.00);       -- Dec
    
    RAISE NOTICE 'Created 60 monthly allocations (5 items x 12 months)';
    
    -- ========================================================================
    -- STEP 4: Verify totals match the budget
    -- ========================================================================
    
    DECLARE
        v_total_allocated NUMERIC(15,2);
        v_expected_expenses NUMERIC(15,2);
    BEGIN
        SELECT SUM(total_allocated) INTO v_total_allocated FROM budget_categories WHERE budget_id = v_budget_id;
        SELECT total_expenses INTO v_expected_expenses FROM budgets WHERE id = v_budget_id;
        
        RAISE NOTICE 'Total Categories Allocated: R%, Expected Expenses: R%', v_total_allocated, v_expected_expenses;
        
        IF ABS(v_total_allocated - v_expected_expenses) > 0.01 THEN
            RAISE WARNING 'Budget totals do not match! Allocated: R%, Expected: R%', v_total_allocated, v_expected_expenses;
        ELSE
            RAISE NOTICE '✓ Budget totals verified successfully!';
        END IF;
    END;
    
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Budget data import completed successfully!';
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Company: Limelight Academy Institutions';
    RAISE NOTICE 'Budget Year: 2026';
    RAISE NOTICE 'Categories: 5';
    RAISE NOTICE 'Items: 5';
    RAISE NOTICE 'Monthly Allocations: 60';
    RAISE NOTICE '============================================';
    
END $$;

-- Display summary of imported data
SELECT 
    'SUMMARY' as report_section,
    (SELECT COUNT(*) FROM budget_categories WHERE budget_id = (SELECT id FROM budgets WHERE company_id = (SELECT id FROM companies WHERE name = 'Limelight Academy Institutions') AND budget_year = 2026)) as categories_count,
    (SELECT COUNT(*) FROM budget_items WHERE budget_category_id IN (SELECT id FROM budget_categories WHERE budget_id = (SELECT id FROM budgets WHERE company_id = (SELECT id FROM companies WHERE name = 'Limelight Academy Institutions') AND budget_year = 2026))) as items_count,
    (SELECT COUNT(*) FROM budget_monthly_allocations WHERE budget_item_id IN (SELECT id FROM budget_items WHERE budget_category_id IN (SELECT id FROM budget_categories WHERE budget_id = (SELECT id FROM budgets WHERE company_id = (SELECT id FROM companies WHERE name = 'Limelight Academy Institutions') AND budget_year = 2026)))) as allocations_count;

-- Display imported budget categories
SELECT 
    bc.name as category_name,
    bc.category_type,
    bc.total_allocated,
    bc.allocated_percentage,
    (SELECT COUNT(*) FROM budget_items WHERE budget_category_id = bc.id) as items_count
FROM budget_categories bc
JOIN budgets b ON bc.budget_id = b.id
JOIN companies c ON b.company_id = c.id
WHERE c.name = 'Limelight Academy Institutions' AND b.budget_year = 2026
ORDER BY bc.total_allocated DESC;
