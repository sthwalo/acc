# Database Connection & Query Reference

## 🔗 Connection Details
```
Host: localhost
Port: 5432
Database: drimacc_db
User: sthwalonyoni
Password: Mapaya400151
```

## 🚀 Quick Connect Methods

### Method 1: Using the script
```bash
./connect_db.sh
```

### Method 2: Direct psql command
```bash
psql -U sthwalonyoni -d drimacc_db -h localhost
```

### Method 3: Using environment variables
```bash
source .env
psql
```

## 📊 Essential Queries

### 1. Total Transaction Count
```sql
SELECT COUNT(*) FROM bank_transactions;
```

### 2. Monthly Summary
```sql
SELECT 
    TO_CHAR(transaction_date, 'YYYY-MM') as month,
    COUNT(*) as transactions,
    SUM(debit_amount) as debits,
    SUM(credit_amount) as credits,
    SUM(credit_amount) - SUM(debit_amount) as net_flow
FROM bank_transactions 
GROUP BY month 
ORDER BY month;
```

### 3. Recent Transactions (Last 10)
```sql
SELECT 
    transaction_date,
    details,
    debit_amount,
    credit_amount,
    balance
FROM bank_transactions 
ORDER BY transaction_date DESC 
LIMIT 10;
```

### 4. Search Transactions
```sql
-- Search for specific terms
SELECT * FROM bank_transactions 
WHERE LOWER(details) LIKE '%salary%' 
ORDER BY transaction_date DESC;

-- Date range search
SELECT * FROM bank_transactions 
WHERE transaction_date BETWEEN '2024-12-01' AND '2024-12-31'
ORDER BY transaction_date;
```

### 5. Largest Transactions
```sql
-- Biggest debits
SELECT transaction_date, details, debit_amount 
FROM bank_transactions 
WHERE debit_amount > 0 
ORDER BY debit_amount DESC 
LIMIT 5;

-- Biggest credits
SELECT transaction_date, details, credit_amount 
FROM bank_transactions 
WHERE credit_amount > 0 
ORDER BY credit_amount DESC 
LIMIT 5;
```

### 6. Database Schema
```sql
\d bank_transactions
\d companies  
\d fiscal_periods
```

### 7. Exit psql
```
\q
```

## 📈 Current Data Status
- **Total Transactions**: 3,829
- **Date Range**: 2024-03-01 to 2025-02-28  
- **Months Covered**: 12 months
- **Files Processed**: 13 PDF statements

## 🎯 Key Tables
- `bank_transactions` - All extracted transactions
- `companies` - Company information
- `fiscal_periods` - Fiscal year definitions

## 🔍 Useful psql Commands
- `\l` - List databases
- `\dt` - List tables
- `\d table_name` - Describe table structure
- `\q` - Quit
- `\h` - Help
- `\timing` - Show query execution time
