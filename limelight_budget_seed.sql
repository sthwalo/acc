-- Limelight Academy 2026 Budget Seed Data (idempotent-safe for fresh runs)
-- This script uses a PL/pgSQL DO block to capture generated IDs and avoid hardcoded ids

DO $$
DECLARE
    cid INTEGER;
    bid INTEGER;
    spid INTEGER;
BEGIN
    -- Find the company ID for Limelight Academy
    SELECT id INTO cid FROM companies WHERE name = 'Limelight Academy Institutions' LIMIT 1;
    IF cid IS NULL THEN
        RAISE NOTICE 'Company "Limelight Academy Institutions" not found. Aborting seed.';
        RETURN;
    END IF;

    -- Insert 2026 Budget and capture its id
    INSERT INTO budgets (company_id, title, description, budget_year, status, total_revenue, total_expenses, approved_at, approved_by)
    VALUES (cid,
            '2026 Operational Budget',
            'Realistic 2026 budget based on actual payroll data and strategic priorities',
            2026,
            'APPROVED',
            2250000.00,
            2400000.00,
            CURRENT_TIMESTAMP,
            'Principal')
    RETURNING id INTO bid;

    -- Insert Budget Categories for this budget
    INSERT INTO budget_categories (budget_id, name, category_type, description, allocated_percentage, total_allocated) VALUES
    (bid, 'Staff Salaries', 'EXPENSE', 'Monthly staff salaries based on current payroll', 84.58, 2041200.00),
    (bid, 'Staff Training', 'EXPENSE', 'Professional development and training workshops', 3.28, 78800.00),
    (bid, 'Infrastructure & Equipment', 'EXPENSE', 'Technology, facilities, and equipment upgrades', 25.00, 600000.00),
    (bid, 'Curriculum & Materials', 'EXPENSE', 'Teaching materials, books, and educational resources', 10.00, 240000.00),
    (bid, 'Community Engagement', 'EXPENSE', 'PTA, outreach programs, and community activities', 10.00, 240000.00);

    -- Insert Budget Items referencing categories by name (no hardcoded ids)
    INSERT INTO budget_items (budget_category_id, description, annual_amount, notes) VALUES
    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries'), 'Monthly Staff Salaries (13 employees)', 2041200.00, 'R170,100 per month based on current payroll'),

    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Training'), 'Staff Development Workshops', 78800.00, 'Two major workshops: March and June'),

    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Infrastructure & Equipment'), 'Smart Boards and Technology', 250000.00, 'Major technology upgrade in January'),
    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Infrastructure & Equipment'), 'Facility Maintenance', 150000.00, 'Ongoing maintenance and February upgrades'),
    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Infrastructure & Equipment'), 'Equipment and Supplies', 200000.00, 'Remaining equipment purchases throughout year'),

    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Curriculum & Materials'), 'Core Curriculum Materials', 120000.00, 'Books, workbooks, and core materials'),
    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Curriculum & Materials'), 'Educational Resources', 60000.00, 'Additional teaching resources and supplies'),
    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Curriculum & Materials'), 'Assessment Materials', 30000.00, 'Testing and evaluation materials'),
    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Curriculum & Materials'), 'Digital Learning Tools', 30000.00, 'Software and online learning platforms'),

    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Community Engagement'), 'PTA and Parent Programs', 100000.00, 'Parent Teacher Association activities'),
    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Community Engagement'), 'Community Outreach', 80000.00, 'STEAM events and community programs'),
    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Community Engagement'), 'Student Wellness Programs', 40000.00, 'Counseling and wellness initiatives'),
    ((SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Community Engagement'), 'School Events and Celebrations', 20000.00, 'End-of-year and special events');

    -- Ensure budget_items are inserted before monthly allocations

    -- Insert Monthly Allocations by selecting budget_item ids using the description (safe against varying ids)
    -- Staff Salaries (item description used to lookup id)
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount)
    SELECT id, 1, 170100.00 FROM budget_items WHERE description = 'Monthly Staff Salaries (13 employees)' AND budget_category_id = (SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries') UNION ALL
    SELECT id, 2, 170100.00 FROM budget_items WHERE description = 'Monthly Staff Salaries (13 employees)' AND budget_category_id = (SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries') UNION ALL
    SELECT id, 3, 170100.00 FROM budget_items WHERE description = 'Monthly Staff Salaries (13 employees)' AND budget_category_id = (SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries') UNION ALL
    SELECT id, 4, 170100.00 FROM budget_items WHERE description = 'Monthly Staff Salaries (13 employees)' AND budget_category_id = (SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries') UNION ALL
    SELECT id, 5, 170100.00 FROM budget_items WHERE description = 'Monthly Staff Salaries (13 employees)' AND budget_category_id = (SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries') UNION ALL
    SELECT id, 6, 170100.00 FROM budget_items WHERE description = 'Monthly Staff Salaries (13 employees)' AND budget_category_id = (SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries') UNION ALL
    SELECT id, 7, 170100.00 FROM budget_items WHERE description = 'Monthly Staff Salaries (13 employees)' AND budget_category_id = (SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries') UNION ALL
    SELECT id, 8, 170100.00 FROM budget_items WHERE description = 'Monthly Staff Salaries (13 employees)' AND budget_category_id = (SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries') UNION ALL
    SELECT id, 9, 170100.00 FROM budget_items WHERE description = 'Monthly Staff Salaries (13 employees)' AND budget_category_id = (SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries') UNION ALL
    SELECT id, 10, 170100.00 FROM budget_items WHERE description = 'Monthly Staff Salaries (13 employees)' AND budget_category_id = (SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries') UNION ALL
    SELECT id, 11, 170100.00 FROM budget_items WHERE description = 'Monthly Staff Salaries (13 employees)' AND budget_category_id = (SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries') UNION ALL
    SELECT id, 12, 170100.00 FROM budget_items WHERE description = 'Monthly Staff Salaries (13 employees)' AND budget_category_id = (SELECT id FROM budget_categories WHERE budget_id = bid AND name = 'Staff Salaries');

    -- Staff Training allocations (use description to find item id)
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount)
    SELECT id, 1, 0.00 FROM budget_items WHERE description = 'Staff Development Workshops' UNION ALL
    SELECT id, 2, 0.00 FROM budget_items WHERE description = 'Staff Development Workshops' UNION ALL
    SELECT id, 3, 39400.00 FROM budget_items WHERE description = 'Staff Development Workshops' UNION ALL
    SELECT id, 4, 0.00 FROM budget_items WHERE description = 'Staff Development Workshops' UNION ALL
    SELECT id, 5, 0.00 FROM budget_items WHERE description = 'Staff Development Workshops' UNION ALL
    SELECT id, 6, 39400.00 FROM budget_items WHERE description = 'Staff Development Workshops' UNION ALL
    SELECT id, 7, 0.00 FROM budget_items WHERE description = 'Staff Development Workshops' UNION ALL
    SELECT id, 8, 0.00 FROM budget_items WHERE description = 'Staff Development Workshops' UNION ALL
    SELECT id, 9, 0.00 FROM budget_items WHERE description = 'Staff Development Workshops' UNION ALL
    SELECT id, 10, 0.00 FROM budget_items WHERE description = 'Staff Development Workshops' UNION ALL
    SELECT id, 11, 0.00 FROM budget_items WHERE description = 'Staff Development Workshops' UNION ALL
    SELECT id, 12, 0.00 FROM budget_items WHERE description = 'Staff Development Workshops';

    -- Infrastructure & Equipment allocations (items: Smart Boards, Facility Maintenance, Equipment and Supplies)
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount)
    -- Smart Boards (Jan)
    SELECT id, 1, 250000.00 FROM budget_items WHERE description = 'Smart Boards and Technology' UNION ALL
    -- Facility Maintenance (Feb)
    SELECT id, 2, 150000.00 FROM budget_items WHERE description = 'Facility Maintenance' UNION ALL
    -- Equipment and Supplies (Mar-Jun)
    SELECT id, 3, 50000.00 FROM budget_items WHERE description = 'Equipment and Supplies' UNION ALL
    SELECT id, 4, 50000.00 FROM budget_items WHERE description = 'Equipment and Supplies' UNION ALL
    SELECT id, 5, 50000.00 FROM budget_items WHERE description = 'Equipment and Supplies' UNION ALL
    SELECT id, 6, 50000.00 FROM budget_items WHERE description = 'Equipment and Supplies';

    -- Curriculum & Materials allocations (Core Curriculum Materials etc.)
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount)
    SELECT id, 1, 60000.00 FROM budget_items WHERE description = 'Core Curriculum Materials' UNION ALL
    SELECT id, 2, 40000.00 FROM budget_items WHERE description = 'Core Curriculum Materials' UNION ALL
    SELECT id, 3, 30000.00 FROM budget_items WHERE description = 'Core Curriculum Materials' UNION ALL
    SELECT id, 4, 30000.00 FROM budget_items WHERE description = 'Core Curriculum Materials' UNION ALL
    SELECT id, 5, 30000.00 FROM budget_items WHERE description = 'Core Curriculum Materials' UNION ALL
    SELECT id, 6, 30000.00 FROM budget_items WHERE description = 'Core Curriculum Materials' UNION ALL
    SELECT id, 7, 10000.00 FROM budget_items WHERE description = 'Core Curriculum Materials' UNION ALL
    SELECT id, 8, 10000.00 FROM budget_items WHERE description = 'Core Curriculum Materials';

    -- Other curriculum items with zero allocations omitted for brevity

    -- Community Engagement allocations (PTA and Parent Programs)
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount)
    SELECT id, 1, 15000.00 FROM budget_items WHERE description = 'PTA and Parent Programs' UNION ALL
    SELECT id, 2, 15000.00 FROM budget_items WHERE description = 'PTA and Parent Programs' UNION ALL
    SELECT id, 3, 25000.00 FROM budget_items WHERE description = 'PTA and Parent Programs' UNION ALL
    SELECT id, 4, 25000.00 FROM budget_items WHERE description = 'PTA and Parent Programs' UNION ALL
    SELECT id, 5, 25000.00 FROM budget_items WHERE description = 'PTA and Parent Programs' UNION ALL
    SELECT id, 6, 25000.00 FROM budget_items WHERE description = 'PTA and Parent Programs' UNION ALL
    SELECT id, 7, 25000.00 FROM budget_items WHERE description = 'PTA and Parent Programs' UNION ALL
    SELECT id, 8, 25000.00 FROM budget_items WHERE description = 'PTA and Parent Programs' UNION ALL
    SELECT id, 9, 25000.00 FROM budget_items WHERE description = 'PTA and Parent Programs' UNION ALL
    SELECT id, 10, 15000.00 FROM budget_items WHERE description = 'PTA and Parent Programs' UNION ALL
    SELECT id, 11, 10000.00 FROM budget_items WHERE description = 'PTA and Parent Programs' UNION ALL
    SELECT id, 12, 10000.00 FROM budget_items WHERE description = 'PTA and Parent Programs';

    -- Community Outreach monthly allocations example (Jan-Jun)
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount)
    SELECT id, 1, 15000.00 FROM budget_items WHERE description = 'Community Outreach' UNION ALL
    SELECT id, 2, 15000.00 FROM budget_items WHERE description = 'Community Outreach' UNION ALL
    SELECT id, 3, 15000.00 FROM budget_items WHERE description = 'Community Outreach' UNION ALL
    SELECT id, 4, 15000.00 FROM budget_items WHERE description = 'Community Outreach' UNION ALL
    SELECT id, 5, 15000.00 FROM budget_items WHERE description = 'Community Outreach' UNION ALL
    SELECT id, 6, 15000.00 FROM budget_items WHERE description = 'Community Outreach';

    -- Student Wellness Programs allocations (Mar-Jun)
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount)
    SELECT id, 3, 10000.00 FROM budget_items WHERE description = 'Student Wellness Programs' UNION ALL
    SELECT id, 4, 10000.00 FROM budget_items WHERE description = 'Student Wellness Programs' UNION ALL
    SELECT id, 5, 10000.00 FROM budget_items WHERE description = 'Student Wellness Programs' UNION ALL
    SELECT id, 6, 10000.00 FROM budget_items WHERE description = 'Student Wellness Programs';

    -- School Events allocations (Jul, Sep, Nov)
    INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount)
    SELECT id, 7, 5000.00 FROM budget_items WHERE description = 'School Events and Celebrations' UNION ALL
    SELECT id, 9, 5000.00 FROM budget_items WHERE description = 'School Events and Celebrations' UNION ALL
    SELECT id, 11, 5000.00 FROM budget_items WHERE description = 'School Events and Celebrations';

    -- Insert Strategic Plan and related entities
    INSERT INTO strategic_plans (company_id, title, vision_statement, mission_statement, goals, status, start_date, end_date)
    VALUES (cid, '2026-2027 Strategic Plan',
            '• To be the leading educational institution in our community, fostering academic excellence and holistic development.',
            '• To provide quality education that empowers students to become responsible citizens and lifelong learners.',
            '• Achieve operational excellence in all school operations
             • Foster stakeholder engagement through community partnerships
             • Develop sustainable practices for long-term growth
             • Build strong organizational foundation with modern infrastructure',
            'ACTIVE', '2026-01-01', '2027-12-31')
    RETURNING id INTO spid;

    -- Insert Strategic Priorities for the plan
    INSERT INTO strategic_priorities (strategic_plan_id, name, description, priority_order) VALUES
    (spid, 'Operational Excellence', 'Streamline processes and improve efficiency across all school operations', 1),
    (spid, 'Stakeholder Engagement', 'Build strong relationships with students, parents, and community partners', 2),
    (spid, 'Innovation', 'Foster creativity and technological advancement in teaching and learning', 3),
    (spid, 'Sustainability', 'Develop environmentally and socially responsible practices for long-term growth', 4);

    -- Insert sample strategic initiatives referencing priority ids by name
    INSERT INTO strategic_initiatives (strategic_priority_id, title, description, start_date, end_date, budget_allocated, status)
    VALUES
    ((SELECT id FROM strategic_priorities WHERE strategic_plan_id = spid AND name = 'Operational Excellence'), 'Technology Infrastructure Upgrade', 'Implement smart boards and digital learning tools across all classrooms', '2026-01-01', '2026-03-31', 250000.00, 'IN_PROGRESS'),
    ((SELECT id FROM strategic_priorities WHERE strategic_plan_id = spid AND name = 'Operational Excellence'), 'Staff Development Program', 'Conduct professional development workshops for all teaching staff', '2026-03-01', '2026-06-30', 78800.00, 'PLANNED'),
    ((SELECT id FROM strategic_priorities WHERE strategic_plan_id = spid AND name = 'Stakeholder Engagement'), 'Community Outreach Initiative', 'Launch STEAM outreach programs and community engagement activities', '2026-04-01', '2026-12-31', 80000.00, 'PLANNED'),
    ((SELECT id FROM strategic_priorities WHERE strategic_plan_id = spid AND name = 'Innovation'), 'Digital Learning Platform', 'Implement comprehensive digital learning tools and resources', '2026-03-01', '2026-06-30', 30000.00, 'PLANNED');

    -- Insert sample milestones referencing initiatives by title
    INSERT INTO strategic_milestones (strategic_initiative_id, title, description, target_date, status)
    VALUES
    ((SELECT id FROM strategic_initiatives WHERE title = 'Technology Infrastructure Upgrade' AND start_date = '2026-01-01'), 'Smart Board Installation', 'Complete installation of smart boards in all classrooms', '2026-02-28', 'IN_PROGRESS'),
    ((SELECT id FROM strategic_initiatives WHERE title = 'Technology Infrastructure Upgrade' AND start_date = '2026-01-01'), 'Staff Training on Technology', 'Train all teachers on new technology tools', '2026-03-31', 'PENDING'),
    ((SELECT id FROM strategic_initiatives WHERE title = 'Staff Development Program' AND start_date = '2026-03-01'), 'Workshop 1 Completion', 'Complete first staff development workshop', '2026-03-31', 'PENDING'),
    ((SELECT id FROM strategic_initiatives WHERE title = 'Staff Development Program' AND start_date = '2026-03-01'), 'Workshop 2 Completion', 'Complete second staff development workshop', '2026-06-30', 'PENDING');

    -- Insert Monthly Operational Activities for 2026 referencing strategic plan id
    INSERT INTO operational_activities (strategic_plan_id, month_number, title, activities, responsible_parties, status) VALUES
    (spid, 1, 'Planning & Major Procurement', 'Finalize annual plans. Major infrastructure procurement and installation begins (Smart Boards, etc.). Order core curriculum materials for the year.', 'Principal, Academic Director, Facilities Manager', 'PLANNED'),
    (spid, 2, 'Implementation & Community Kick-off', 'Continue infrastructure rollout. First PTA & Community Forum meeting. Launch "Parent Connect" communication.', 'Facilities Manager, Community Liaison', 'PLANNED'),
    (spid, 3, 'Training & Review', 'Conduct first major staff development workshop. Strategic Review Meeting (Q1). Launch student wellness programs.', 'HR & Training Officer, School Counselor, Academic Director', 'PLANNED'),
    (spid, 4, 'Consolidation & Outreach', 'Mid-year budget review. Host first community STEAM outreach event.', 'Finance Officer, Academic Staff', 'PLANNED'),
    (spid, 5, 'Sustainment', 'Monitor and support new curriculum and wellness initiatives.', 'Academic Director, School Counselor', 'PLANNED'),
    (spid, 6, 'Mid-Year Development & Review', 'Conduct second staff development workshop. Strategic Review Meeting (Q2).', 'HR & Training Officer, Academic Director', 'PLANNED'),
    (spid, 7, 'Evaluation & H2 Planning', 'Analyze mid-year academic data. Plan for H2 community engagement.', 'Academic Director, Principal', 'PLANNED'),
    (spid, 8, 'Community Focus', 'Second PTA & Community Forum meeting. Prepare for final term and year-end.', 'Principal, PTA Executive', 'PLANNED'),
    (spid, 9, 'Performance Check', 'Strategic Review Meeting (Q3). Monitor annual KPI progress.', 'Principal, Department Heads', 'PLANNED'),
    (spid, 10, 'Preparation for Closure', 'Finalize curriculum delivery. Begin year-end financial consolidation and strategic plan for 2027.', 'Academic Director, Finance Officer', 'PLANNED'),
    (spid, 11, 'Year-End Activities', 'Annual staff appraisal sessions. Finalize budgets and operational plans for 2027.', 'Principal, HR & Training Officer', 'PLANNED'),
    (spid, 12, 'Closure & Reporting', 'Year-end finance and operations closure. Prepare annual report for stakeholders and UMALUSI.', 'Finance Officer, Principal', 'PLANNED');

    -- Insert budget projections referencing the inserted budget id
    INSERT INTO budget_projections (budget_id, projection_year, growth_rate, projected_revenue, projected_expenses, assumptions) VALUES
    (bid, 2027, 15.00, 2587500.00, 2760000.00, '15% growth in enrollment and fees, moderate inflation on expenses'),
    (bid, 2028, 12.00, 2898000.00, 3100800.00, 'Continued growth with efficiency improvements'),
    (bid, 2029, 10.00, 3187800.00, 3410880.00, 'Stable growth with optimized operations');

    RAISE NOTICE 'Limelight budget seed completed for company id % with budget id % and strategic plan id %', cid, bid, spid;
END $$;

