-- ===============================================
-- FINANCIAL DATA ANALYSIS QUERIES
-- Database: (PostgreSQL)
-- Total Transactions: 3,829
-- Period: 2024-03-01 to 2025-02-28
-- ===============================================

-- 1. BASIC CONNECTION TEST
SELECT current_database(), current_user, version();

-- 2. TOTAL TRANSACTION COUNT
SELECT COUNT(*) as total_transactions FROM bank_transactions;

-- 3. TRANSACTIONS BY MONTH (Summary)
SELECT 
    TO_CHAR(transaction_date, 'YYYY-MM') as month,
    COUNT(*) as transaction_count,
    SUM(debit_amount) as total_debits,
    SUM(credit_amount) as total_credits,
    SUM(credit_amount) - SUM(debit_amount) as net_flow
FROM bank_transactions 
GROUP BY TO_CHAR(transaction_date, 'YYYY-MM')
ORDER BY month;

-- 4. RECENT TRANSACTIONS (Last 20)
SELECT 
    transaction_date,
    details,
    debit_amount,
    credit_amount,
    balance,
    service_fee
FROM bank_transactions 
ORDER BY transaction_date DESC, id DESC
LIMIT 20;

-- 5. LARGEST TRANSACTIONS (Top 10 Debits and Credits)
-- Top 10 Debits
SELECT 
    transaction_date,
    details,
    debit_amount,
    'DEBIT' as type
FROM bank_transactions 
WHERE debit_amount > 0
ORDER BY debit_amount DESC
LIMIT 10;

-- Top 10 Credits
SELECT 
    transaction_date,
    details,
    credit_amount,
    'CREDIT' as type
FROM bank_transactions 
WHERE credit_amount > 0
ORDER BY credit_amount DESC
LIMIT 10;

-- 6. SERVICE FEES ANALYSIS
SELECT 
    COUNT(*) as service_fee_count,
    SUM(CASE WHEN service_fee THEN debit_amount ELSE 0 END) as total_service_fees
FROM bank_transactions 
WHERE service_fee = true;

-- 7. DAILY TRANSACTION VOLUME (Top 10 busiest days)
SELECT 
    transaction_date,
    COUNT(*) as daily_transactions,
    SUM(debit_amount) as daily_debits,
    SUM(credit_amount) as daily_credits
FROM bank_transactions 
GROUP BY transaction_date
ORDER BY daily_transactions DESC
LIMIT 10;

-- 8. SEARCH FOR SPECIFIC TRANSACTIONS
-- Example: Search for salary payments
SELECT 
    transaction_date,
    details,
    debit_amount,
    credit_amount
FROM bank_transactions 
WHERE LOWER(details) LIKE '%salary%' OR LOWER(details) LIKE '%salaries%'
ORDER BY transaction_date DESC;

-- 9. MONTHLY BALANCE TRENDS
SELECT 
    TO_CHAR(transaction_date, 'YYYY-MM') as month,
    AVG(balance) as avg_balance,
    MIN(balance) as min_balance,
    MAX(balance) as max_balance
FROM bank_transactions 
WHERE balance IS NOT NULL
GROUP BY TO_CHAR(transaction_date, 'YYYY-MM')
ORDER BY month;

-- 10. FISCAL PERIOD VERIFICATION
SELECT 
    fp.period_name,
    fp.start_date,
    fp.end_date,
    COUNT(bt.*) as transactions_in_period,
    MIN(bt.transaction_date) as earliest_transaction,
    MAX(bt.transaction_date) as latest_transaction
FROM fiscal_periods fp
LEFT JOIN bank_transactions bt ON bt.fiscal_period_id = fp.id
GROUP BY fp.id, fp.period_name, fp.start_date, fp.end_date;

-- 11. DATABASE SCHEMA OVERVIEW
\d bank_transactions
\d companies
\d fiscal_periods

-- 12. CUSTOM DATE RANGE QUERY TEMPLATE
-- Replace YYYY-MM-DD with your desired dates
/*
SELECT 
    transaction_date,
    details,
    debit_amount,
    credit_amount,
    balance
FROM bank_transactions 
WHERE transaction_date BETWEEN 'YYYY-MM-DD' AND 'YYYY-MM-DD'
ORDER BY transaction_date;
*/

-- 13. TRANSACTION PATTERN ANALYSIS
-- Most common transaction types
SELECT 
    CASE 
        WHEN LOWER(details) LIKE '%salary%' OR LOWER(details) LIKE '%salaries%' THEN 'Salary'
        WHEN LOWER(details) LIKE '%insurance%' THEN 'Insurance'
        WHEN LOWER(details) LIKE '%fee%' THEN 'Banking Fee'
        WHEN LOWER(details) LIKE '%payment%' THEN 'Payment'
        WHEN LOWER(details) LIKE '%transfer%' THEN 'Transfer'
        WHEN LOWER(details) LIKE '%cash%' THEN 'Cash Transaction'
        ELSE 'Other'
    END as transaction_category,
    COUNT(*) as count,
    SUM(debit_amount) as total_debits,
    SUM(credit_amount) as total_credits
FROM bank_transactions
GROUP BY transaction_category
ORDER BY count DESC;

-- 14. QUICK STATS SUMMARY
SELECT 
    'Total Transactions' as metric, COUNT(*)::text as value
FROM bank_transactions
UNION ALL
SELECT 
    'Date Range', 
    MIN(transaction_date)::text || ' to ' || MAX(transaction_date)::text
FROM bank_transactions
UNION ALL
SELECT 
    'Total Debits', 
    'R' || TO_CHAR(SUM(debit_amount), 'FM999,999,999.00')
FROM bank_transactions
UNION ALL
SELECT 
    'Total Credits', 
    'R' || TO_CHAR(SUM(credit_amount), 'FM999,999,999.00')
FROM bank_transactions
UNION ALL
SELECT 
    'Net Flow', 
    'R' || TO_CHAR(SUM(credit_amount) - SUM(debit_amount), 'FM999,999,999.00')
FROM bank_transactions;
