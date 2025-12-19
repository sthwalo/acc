# Payroll System Integration Guide

## Overview

The FIN Financial Management System now includes a fully functional payroll module that seamlessly integrates with the existing accounting system. This payroll system is designed specifically for South African businesses and includes PAYE tax calculations, UIF contributions, and full integration with the double-entry bookkeeping system.

## Features

### ✅ Employee Management
- **Employee Registration**: Full employee profiles with personal, employment, and banking details
- **Employment Types**: Support for permanent, contract, and temporary employees
- **Salary Types**: Monthly, weekly, daily, and hourly salary structures
- **Tax Information**: Tax numbers, rebate codes, UIF numbers, medical aid details

### ✅ Payroll Processing
- **Automated Calculations**: PAYE tax, UIF contributions, and deductions
- **South African Compliance**: 2024 tax tables and UIF rates
- **Payroll Periods**: Monthly, weekly, and quarterly processing
- **Approval Workflow**: Draft → Processed → Approved → Paid status flow

### ✅ Financial Integration
- **Journal Entries**: Automatic creation of double-entry bookkeeping records
- **Chart of Accounts**: Integration with existing account structure
- **Financial Reports**: Payroll costs appear in income statements and balance sheets
- **Bank Integration**: Net pay transactions and liability tracking

### ✅ Compliance Features
- **PAYE Calculations**: Accurate South African income tax calculations
- **UIF Contributions**: Employee and employer UIF calculations (1% each)
- **Tax Certificates**: Annual tax information tracking
- **Audit Trail**: Complete tracking of payroll changes and approvals

## Database Schema

The payroll system adds the following tables to your PostgreSQL database:

### Core Tables
- `employees` - Employee master data
- `payroll_periods` - Payroll processing periods
- `payslips` - Individual employee payslips
- `deductions` - Employee deductions (loans, medical aid, etc.)
- `benefits` - Employee benefits and allowances
- `tax_configurations` - Tax rates and brackets
- `payroll_journal_entries` - Links to accounting system

### Supporting Tables
- `tax_brackets` - Progressive tax rate structure
- `employee_leave` - Leave management (future enhancement)

## Installation

### 1. Database Setup
Execute the payroll database schema:
```sql
-- Run the contents of docs/payroll_database_schema.sql
-- This creates all necessary tables, indexes, and views
```

### 2. Verify Integration
The payroll system is already integrated into the main application. Look for:
- Menu option "9. Payroll Management" in the main menu
- New Java classes in `fin.model`, `fin.service`, and `fin.controller`

## Usage Guide

### Getting Started

1. **Start the Application**
   ```bash
   java -jar app/build/libs/fin-spring.jar
   ```

2. **Setup Prerequisites**
   - Create or select a company (Option 1)
   - Create or select a fiscal period (Option 2)

3. **Access Payroll**
   - Select option 9: Payroll Management

### Employee Management

#### Adding an Employee
1. Navigate to Employee Management → Add New Employee
2. Enter employee details:
   - Employee Number (unique identifier)
   - Personal information (name, contact details)
   - Position and department
   - Employment details (hire date, salary, employment type)
   - Tax information (tax number, rebate code)

#### Example Employee Data
```
Employee Number: EMP001
First Name: John
Last Name: Doe
Position: Software Developer
Department: IT
Hire Date: 2025-01-01
Basic Salary: R25,000.00
Employment Type: PERMANENT
Tax Number: 9876543210123
```

### Payroll Processing

#### Creating a Payroll Period
1. Navigate to Payroll Period Management → Create New Payroll Period
2. Define the period:
   - Period Name: "January 2025"
   - Start Date: 2025-01-01
   - End Date: 2025-01-31
   - Pay Date: 2025-02-01

#### Processing Payroll
1. Navigate to Process Payroll
2. Select an open payroll period
3. Confirm processing - this will:
   - Calculate all employee payslips
   - Generate journal entries
   - Update payroll period status

#### Expected Calculations (Example for R25,000 monthly salary)
- **Gross Salary**: R25,000.00
- **PAYE Tax**: ~R3,200.00 (varies by rebates)
- **UIF Employee**: R250.00 (1%)
- **UIF Employer**: R250.00 (1%)
- **Net Pay**: ~R21,550.00

## Integration Details

### Journal Entry Structure

When payroll is processed, the system creates journal entries like:

```
Reference: PAY-1-202501
Description: Payroll for January 2025

Dr. Employee Costs (8100)           R25,000.00
    Cr. Payroll Liabilities (2500)              R3,450.00
    Cr. Bank Account (1100)                      R21,550.00
```

### Account Codes Used
- **8100** - Employee Costs (Expense)
- **2500** - Payroll Liabilities (Current Liability)
- **1100** - Bank - Current Account (Asset)

### Financial Statement Impact
- **Income Statement**: Employee costs appear as operating expenses
- **Balance Sheet**: Payroll liabilities appear as current liabilities
- **Cash Flow**: Net pay appears as operating cash outflows

## Tax Compliance

### South African PAYE (2024 Tax Year)
The system uses the current South African tax brackets:

| Annual Income Range | Tax Rate | Cumulative Tax |
|-------------------|----------|----------------|
| R0 - R237,100 | 18% | R0 + 18% of amount |
| R237,101 - R370,500 | 26% | R42,678 + 26% of excess |
| R370,501 - R512,800 | 31% | R77,362 + 31% of excess |
| R512,801 - R673,000 | 36% | R121,475 + 36% of excess |
| R673,001 - R857,900 | 39% | R179,147 + 39% of excess |
| R857,901 - R1,817,000 | 41% | R251,258 + 41% of excess |
| R1,817,001+ | 45% | R644,489 + 45% of excess |

**Primary Rebate**: R17,235 annually
**Secondary Rebate** (65+): R9,444 annually  
**Tertiary Rebate** (75+): R3,145 annually

### UIF Contributions (2024)
- **Rate**: 1% employee + 1% employer = 2% total
- **Maximum Monthly Earnings**: R17,712
- **Maximum Monthly Contribution**: R177.12 each (employee & employer)

## Reports

### Available Reports
1. **Payroll Summary** - Overview of all processed payroll periods
2. **Employee Payslips** - Individual payslip details (coming soon)
3. **Tax Summary** - PAYE and UIF summaries (planned)

### Integration with Existing Reports
Payroll costs automatically appear in:
- **Income Statement** - Under Employee Costs
- **Balance Sheet** - Payroll liabilities
- **Trial Balance** - All payroll-related accounts
- **General Ledger** - Detailed payroll transactions

## Customization

### Tax Rates
Tax rates are configurable through the `tax_configurations` and `tax_brackets` tables. The system currently uses 2024 South African rates.

### Chart of Accounts
The system integrates with your existing chart of accounts. Default mappings:
- Employee costs → Account 8100
- Payroll liabilities → Account 2500
- Bank payments → Account 1100

### Payroll Frequency
Supports multiple payroll frequencies:
- Monthly (default)
- Weekly
- Quarterly

## Security Features

- **Approval Workflow**: Multi-stage payroll approval process
- **Audit Trail**: Complete tracking of all payroll activities
- **Access Control**: Integration with existing company security model
- **Data Validation**: Comprehensive validation of all payroll data

## Future Enhancements

Planned features include:
- **Advanced Reporting**: Detailed payroll analytics and dashboards
- **Leave Management**: Integration with payroll processing
- **Benefit Administration**: Enhanced benefit and allowance management
- **Tax Certificates**: Automated IRP5/IT3(a) generation
- **Multi-Currency**: Support for international operations

## Support

The payroll system is fully integrated with the existing FIN Financial Management System. It maintains the same:
- Database connectivity
- Error handling
- User interface consistency
- Data validation standards
- Security protocols

## Testing

Use the provided test script to verify the integration:
```bash
./test-payroll-integration.sh
```

This script provides a comprehensive checklist and test scenarios for validating the payroll system integration.

---

**The payroll system is now ready for production use in your FIN Financial Management System!**
