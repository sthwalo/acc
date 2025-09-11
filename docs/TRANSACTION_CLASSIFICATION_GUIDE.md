# Xinghizana Group Transaction Classification Guide

This guide explains how to use the transaction classification system to automatically classify bank transactions based on custom rules specific to Xinghizana Group's business patterns.

## Overview

The system provides two main utilities:

1. **Chart of Accounts Initializer** - Sets up the standard chart of accounts and creates transaction mapping rules specific to Xinghizana Group.
2. **Transaction Classifier** - Applies the defined mapping rules to unclassified bank transactions.

## Getting Started

### 1. Initialize Chart of Accounts

Before classifying transactions, you need to initialize the chart of accounts and set up transaction mapping rules:

```bash
./initialize-accounts.sh
```

This script will:
- Create standard account categories (Assets, Liabilities, Equity, Revenue, Expenses)
- Create standard accounts with appropriate codes
- Set up transaction mapping rules specific to Xinghizana Group, including:
  - Bank fees → Account 9600 (Bank Charges)
  - XG Salaries → Account 8100 (Employee Costs)
  - Insurance payments → Account 8800 (Insurance)
  - Corobrik income → Account 6000 (Sales Revenue)
  - Two Way Technologies → Account 8400 (Communication)
  - Office rent (Ellispark) → Account 8200 (Rent Expense)
  - Director funding (COMPANY ASSIST) → Account 4000 (Long-term Loans)
  - And more...

### 2. Classify Transactions

After initializing the chart of accounts and setting up mapping rules, you can classify all unclassified transactions:

```bash
./classify-transactions.sh
```

This script will:
- Load all transaction mapping rules for your company
- Find all unclassified bank transactions (those with NULL account_code)
- Apply the mapping rules to each transaction
- Update the database with the appropriate account code and name for each transaction

## Custom Transaction Mapping Rules

The system has been configured with custom mapping rules specific to Xinghizana Group's transaction patterns:

| Pattern Text | Account Code | Account Name |
|--------------|--------------|--------------|
| FEE | 9600 | Bank Charges |
| CHARGE | 9600 | Bank Charges |
| ADMIN FEE | 9600 | Bank Charges |
| XG SALARIES | 8100 | Employee Costs |
| SALARIES | 8100 | Employee Costs |
| INSURANCE | 8800 | Insurance |
| SANTAM | 8800 | Insurance |
| COMPANY ASSIST | 4000 | Long-term Loans |
| COROBRIK | 6000 | Sales Revenue |
| ELLISPARK | 8200 | Rent Expense |
| TWO WAY TECHNOLOGIES | 8400 | Communication |
| FUEL | 8500 | Motor Vehicle Expenses |
| PETROL | 8500 | Motor Vehicle Expenses |
| ACCOUNTING | 8700 | Professional Services |
| LEGAL | 8700 | Professional Services |
| MUNICIPAL | 8300 | Utilities |
| ELECTRICITY | 8300 | Utilities |
| STATIONERY | 9000 | Office Supplies |

## How It Works

1. The system analyzes transaction details for specific patterns
2. When a pattern is found, it assigns the corresponding account code and name
3. For example, any transaction containing "XG SALARIES" will be classified as account 8100 (Employee Costs)
4. The classification is stored in the database for reporting and financial analysis

## Extending the Rules

To add more custom rules:

1. Edit the `createXGTransactionMappingRules` method in `ChartOfAccountsInitializer.java`
2. Add a new rule using the `createMappingRule` method:
   ```java
   createMappingRule(companyId, "PATTERN_TEXT", "ACCOUNT_CODE", "ACCOUNT_NAME");
   ```
3. Run the `./initialize-accounts.sh` script again to update the rules

## Analyzing Transactions

To run a transaction analysis without making any changes:

```bash
./initialize-accounts.sh --analyze-only
```

This will:
- Analyze all unique transaction descriptions in your database
- Show statistics and patterns found in the transactions
- Suggest account mappings based on the analysis
- Not make any changes to your database

## Account Structure

The chart of accounts follows a standard South African business structure:

- **1000-1999**: Current Assets
- **2000-2999**: Non-Current Assets
- **3000-3999**: Current Liabilities
- **4000-4999**: Non-Current Liabilities
- **5000-5999**: Equity
- **6000-6999**: Operating Revenue
- **7000-7999**: Other Income
- **8000-8999**: Operating Expenses
- **9000-9499**: Administrative Expenses
- **9500-9999**: Finance Costs

## Troubleshooting

### No Rules Applied

If transactions aren't being classified, check:
1. That you've run the `initialize-accounts.sh` script first
2. That the transaction details don't match any defined patterns
3. The database connection is working correctly

### Missing Accounts

If account codes referenced in rules don't exist:
1. Make sure you've run the chart of accounts initialization
2. Check that the account code exists in the `accounts` table
3. Run the initialization script again if needed

### Manual Classification

For transactions that don't match any rules, you can manually update them in the database:

```sql
UPDATE bank_transactions
SET account_code = '8100', account_name = 'Employee Costs'
WHERE id = 12345;
```

## Support

For assistance with the transaction classification system, contact:
- IT Support Team
- Financial Department
