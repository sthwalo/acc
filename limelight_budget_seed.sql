-- Limelight Academy 2026 Budget Seed Data
-- Realistic monthly budget based on actual payroll data (R170,100/month)
-- Total Annual Budget: R2,400,000

-- First, get the Limelight Academy company ID
-- Assuming Limelight Academy exists in the companies table

-- Insert 2026 Budget for Limelight Academy
INSERT INTO budgets (company_id, title, description, budget_year, status, total_revenue, total_expenses, approved_at, approved_by)
SELECT
    c.id,
    '2026 Operational Budget',
    'Realistic 2026 budget based on actual payroll data and strategic priorities',
    2026,
    'APPROVED',
    2250000.00, -- Projected revenue
    2400000.00, -- Total expenses
    CURRENT_TIMESTAMP,
    'Principal'
FROM companies c
WHERE c.name = 'Limelight Academy Institutions';

-- Get the budget ID we just created
-- Note: In a real scenario, you'd use the returned ID, but for this seed data we'll assume ID 1

-- Insert Budget Categories
INSERT INTO budget_categories (budget_id, name, category_type, description, allocated_percentage, total_allocated) VALUES
(8, 'Staff Salaries', 'EXPENSE', 'Monthly staff salaries based on current payroll', 84.58, 2041200.00),
(8, 'Staff Training', 'EXPENSE', 'Professional development and training workshops', 3.28, 78800.00),
(8, 'Infrastructure & Equipment', 'EXPENSE', 'Technology, facilities, and equipment upgrades', 25.00, 600000.00),
(8, 'Curriculum & Materials', 'EXPENSE', 'Teaching materials, books, and educational resources', 10.00, 240000.00),
(8, 'Community Engagement', 'EXPENSE', 'PTA, outreach programs, and community activities', 10.00, 240000.00);

-- Insert Budget Items with Annual Amounts
INSERT INTO budget_items (budget_category_id, description, annual_amount, notes) VALUES
-- Staff Salaries (Category 1)
(1, 'Monthly Staff Salaries (13 employees)', 2041200.00, 'R170,100 per month based on current payroll'),

-- Staff Training (Category 2)
(2, 'Staff Development Workshops', 78800.00, 'Two major workshops: March and June'),

-- Infrastructure & Equipment (Category 3)
(3, 'Smart Boards and Technology', 250000.00, 'Major technology upgrade in January'),
(3, 'Facility Maintenance', 150000.00, 'Ongoing maintenance and February upgrades'),
(3, 'Equipment and Supplies', 200000.00, 'Remaining equipment purchases throughout year'),

-- Curriculum & Materials (Category 4)
(4, 'Core Curriculum Materials', 120000.00, 'Books, workbooks, and core materials'),
(4, 'Educational Resources', 60000.00, 'Additional teaching resources and supplies'),
(4, 'Assessment Materials', 30000.00, 'Testing and evaluation materials'),
(4, 'Digital Learning Tools', 30000.00, 'Software and online learning platforms'),

-- Community Engagement (Category 5)
(5, 'PTA and Parent Programs', 100000.00, 'Parent Teacher Association activities'),
(5, 'Community Outreach', 80000.00, 'STEAM events and community programs'),
(5, 'Student Wellness Programs', 40000.00, 'Counseling and wellness initiatives'),
(5, 'School Events and Celebrations', 20000.00, 'End-of-year and special events');

-- Insert Monthly Allocations for Staff Salaries (R170,100 every month)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(1, 1, 170100.00), (1, 2, 170100.00), (1, 3, 170100.00), (1, 4, 170100.00),
(1, 5, 170100.00), (1, 6, 170100.00), (1, 7, 170100.00), (1, 8, 170100.00),
(1, 9, 170100.00), (1, 10, 170100.00), (1, 11, 170100.00), (1, 12, 170100.00);

-- Insert Monthly Allocations for Staff Training (R39,400 in March and June)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(2, 1, 0.00), (2, 2, 0.00), (2, 3, 39400.00), (2, 4, 0.00),
(2, 5, 0.00), (2, 6, 39400.00), (2, 7, 0.00), (2, 8, 0.00),
(2, 9, 0.00), (2, 10, 0.00), (2, 11, 0.00), (2, 12, 0.00);

-- Insert Monthly Allocations for Infrastructure & Equipment
-- Smart Boards (R250,000 in January)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(3, 1, 250000.00), (3, 2, 0.00), (3, 3, 0.00), (3, 4, 0.00),
(3, 5, 0.00), (3, 6, 0.00), (3, 7, 0.00), (3, 8, 0.00),
(3, 9, 0.00), (3, 10, 0.00), (3, 11, 0.00), (3, 12, 0.00);

-- Facility Maintenance (R50,000 in Jan, R100,000 in Feb)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(4, 1, 50000.00), (4, 2, 100000.00), (4, 3, 0.00), (4, 4, 0.00),
(4, 5, 0.00), (4, 6, 0.00), (4, 7, 0.00), (4, 8, 0.00),
(4, 9, 0.00), (4, 10, 0.00), (4, 11, 0.00), (4, 12, 0.00);

-- Equipment and Supplies (R50,000 each in Mar-Jun)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(5, 1, 0.00), (5, 2, 0.00), (5, 3, 50000.00), (5, 4, 50000.00),
(5, 5, 50000.00), (5, 6, 50000.00), (5, 7, 0.00), (5, 8, 0.00),
(5, 9, 0.00), (5, 10, 0.00), (5, 11, 0.00), (5, 12, 0.00);

-- Insert Monthly Allocations for Curriculum & Materials
-- Core Curriculum (R60,000 each in Jan-Jun, R10,000 in Jul-Aug)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(6, 1, 60000.00), (6, 2, 40000.00), (6, 3, 30000.00), (6, 4, 30000.00),
(6, 5, 30000.00), (6, 6, 30000.00), (6, 7, 10000.00), (6, 8, 10000.00),
(6, 9, 0.00), (6, 10, 0.00), (6, 11, 0.00), (6, 12, 0.00);

-- Educational Resources (R15,000 each in Jan-Apr)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(7, 1, 15000.00), (7, 2, 15000.00), (7, 3, 15000.00), (7, 4, 15000.00),
(7, 5, 0.00), (7, 6, 0.00), (7, 7, 0.00), (7, 8, 0.00),
(7, 9, 0.00), (7, 10, 0.00), (7, 11, 0.00), (7, 12, 0.00);

-- Assessment Materials (R10,000 each in Feb-Apr)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(8, 1, 0.00), (8, 2, 10000.00), (8, 3, 10000.00), (8, 4, 10000.00),
(8, 5, 0.00), (8, 6, 0.00), (8, 7, 0.00), (8, 8, 0.00),
(8, 9, 0.00), (8, 10, 0.00), (8, 11, 0.00), (8, 12, 0.00);

-- Digital Learning Tools (R10,000 each in Mar-May)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(9, 1, 0.00), (9, 2, 0.00), (9, 3, 10000.00), (9, 4, 10000.00),
(9, 5, 10000.00), (9, 6, 0.00), (9, 7, 0.00), (9, 8, 0.00),
(9, 9, 0.00), (9, 10, 0.00), (9, 11, 0.00), (9, 12, 0.00);

-- Insert Monthly Allocations for Community Engagement
-- PTA and Parent Programs (R15,000 each in Jan-Feb, R25,000 each in Mar-Jun, R15,000 in Oct, R10,000 each in Nov-Dec)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(10, 1, 15000.00), (10, 2, 15000.00), (10, 3, 25000.00), (10, 4, 25000.00),
(10, 5, 25000.00), (10, 6, 25000.00), (10, 7, 25000.00), (10, 8, 25000.00),
(10, 9, 25000.00), (10, 10, 15000.00), (10, 11, 10000.00), (10, 12, 10000.00);

-- Community Outreach (R15,000 each in Jan-Jun)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(11, 1, 15000.00), (11, 2, 15000.00), (11, 3, 15000.00), (11, 4, 15000.00),
(11, 5, 15000.00), (11, 6, 15000.00), (11, 7, 0.00), (11, 8, 0.00),
(11, 9, 0.00), (11, 10, 0.00), (11, 11, 0.00), (11, 12, 0.00);

-- Student Wellness Programs (R10,000 each in Mar-Jun)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(12, 1, 0.00), (12, 2, 0.00), (12, 3, 10000.00), (12, 4, 10000.00),
(12, 5, 10000.00), (12, 6, 10000.00), (12, 7, 0.00), (12, 8, 0.00),
(12, 9, 0.00), (12, 10, 0.00), (12, 11, 0.00), (12, 12, 0.00);

-- School Events (R5,000 each in Jul, Sep, Nov)
INSERT INTO budget_monthly_allocations (budget_item_id, month_number, allocated_amount) VALUES
(13, 1, 0.00), (13, 2, 0.00), (13, 3, 0.00), (13, 4, 0.00),
(13, 5, 0.00), (13, 6, 0.00), (13, 7, 5000.00), (13, 8, 0.00),
(13, 9, 5000.00), (13, 10, 0.00), (13, 11, 5000.00), (13, 12, 0.00);

-- Insert Strategic Plan for Limelight Academy
INSERT INTO strategic_plans (company_id, title, vision_statement, mission_statement, goals, status, start_date, end_date)
SELECT
    c.id,
    '2026-2027 Strategic Plan',
    'To be the leading educational institution in our community, fostering academic excellence and holistic development.',
    'To provide quality education that empowers students to become responsible citizens and lifelong learners.',
    '• Achieve operational excellence in all school operations\n• Foster stakeholder engagement through community partnerships\n• Develop sustainable practices for long-term growth\n• Build strong organizational foundation with modern infrastructure',
    'ACTIVE',
    '2026-01-01',
    '2027-12-31'
FROM companies c
WHERE c.name = 'Limelight Academy Institutions';

-- Insert Strategic Priorities
INSERT INTO strategic_priorities (strategic_plan_id, name, description, priority_order) VALUES
(1, 'Operational Excellence', 'Streamline processes and improve efficiency across all school operations', 1),
(1, 'Stakeholder Engagement', 'Build strong relationships with students, parents, and community partners', 2),
(1, 'Innovation', 'Foster creativity and technological advancement in teaching and learning', 3),
(1, 'Sustainability', 'Develop environmentally and socially responsible practices for long-term growth', 4);

-- Insert sample strategic initiatives
INSERT INTO strategic_initiatives (strategic_priority_id, title, description, start_date, end_date, budget_allocated, status) VALUES
(1, 'Technology Infrastructure Upgrade', 'Implement smart boards and digital learning tools across all classrooms', '2026-01-01', '2026-03-31', 250000.00, 'IN_PROGRESS'),
(1, 'Staff Development Program', 'Conduct professional development workshops for all teaching staff', '2026-03-01', '2026-06-30', 78800.00, 'PLANNED'),
(2, 'Community Outreach Initiative', 'Launch STEAM outreach programs and community engagement activities', '2026-04-01', '2026-12-31', 80000.00, 'PLANNED'),
(3, 'Digital Learning Platform', 'Implement comprehensive digital learning tools and resources', '2026-03-01', '2026-06-30', 30000.00, 'PLANNED');

-- Insert sample milestones
INSERT INTO strategic_milestones (strategic_initiative_id, title, description, target_date, status) VALUES
(1, 'Smart Board Installation', 'Complete installation of smart boards in all classrooms', '2026-02-28', 'IN_PROGRESS'),
(1, 'Staff Training on Technology', 'Train all teachers on new technology tools', '2026-03-31', 'PENDING'),
(2, 'Workshop 1 Completion', 'Complete first staff development workshop', '2026-03-31', 'PENDING'),
(2, 'Workshop 2 Completion', 'Complete second staff development workshop', '2026-06-30', 'PENDING');

-- Insert budget projections for multi-year planning
INSERT INTO budget_projections (budget_id, projection_year, growth_rate, projected_revenue, projected_expenses, assumptions) VALUES
(8, 2027, 15.00, 2587500.00, 2760000.00, '15% growth in enrollment and fees, moderate inflation on expenses'),
(8, 2028, 12.00, 2898000.00, 3100800.00, 'Continued growth with efficiency improvements'),
(8, 2029, 10.00, 3187800.00, 3410880.00, 'Stable growth with optimized operations');