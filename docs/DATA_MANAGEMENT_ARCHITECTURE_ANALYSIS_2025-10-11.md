# Data Management Architecture Analysis
**Date:** October 11, 2025  
**Purpose:** Complete mapping of all components involved in data management operations  
**Related:** TRANSACTION_CLASSIFICATION_CONSOLIDATION_2025-10-11.md

## 🏗️ Architecture Overview

This document maps all models, repositories, services, and controllers involved in data management operations, showing how they interact and where redundancies exist.

## 📊 Models (Domain Entities)

### Core Transaction Models
```java
fin.model.BankTransaction
├── Purpose: Represents raw bank statement data
├── Key Fields: id, companyId, fiscalPeriodId, transactionDate, details, debitAmount, creditAmount, balance, accountCode, accountName
├── Classification Fields: accountCode, accountName (populated during classification)
├── Usage: Primary entity for all transaction processing
└── Database Table: bank_transactions

fin.model.JournalEntry
├── Purpose: Double-entry accounting representation
├── Key Fields: id, companyId, fiscalPeriodId, reference, description, entryDate
├── Relationship: One-to-Many with JournalEntryLine
├── Validation: Must balance (total debits = total credits)
└── Database Table: journal_entries

fin.model.JournalEntryLine
├── Purpose: Individual debit/credit lines in journal entries
├── Key Fields: id, journalEntryId, accountId, debitAmount, creditAmount, sourceTransactionId
├── Relationship: Many-to-One with JournalEntry, links to BankTransaction via sourceTransactionId
├── Usage: Creates audit trail from bank transactions to journal entries
└── Database Table: journal_entry_lines
```

### Account Management Models
```java
fin.model.Account
├── Purpose: Chart of accounts structure
├── Key Fields: id, companyId, accountCode, accountName, category, isActive
├── Account Code Ranges: 1000-9999 (Assets, Liabilities, Equity, Revenue, Expenses)
├── Usage: Referenced by classification rules and journal entries
└── Database Table: accounts

fin.model.TransactionMappingRule
├── Purpose: Pattern-based classification rules
├── Key Fields: id, companyId, patternText, accountCode, priority, isActive
├── Schema Issue: Some code uses 'match_value', newer code uses 'pattern_text'
├── Usage: Automatic transaction classification
└── Database Table: transaction_mapping_rules
```

### Company & Period Models
```java
fin.model.Company
├── Purpose: Multi-tenant company management
├── Key Fields: id, name, registrationNumber, taxNumber, address, contact
├── Usage: Scopes all financial data by company
└── Database Table: companies

fin.model.FiscalPeriod
├── Purpose: Financial reporting periods
├── Key Fields: id, companyId, periodName, startDate, endDate, status
├── Usage: Scopes transactions and reports by time period
└── Database Table: fiscal_periods
```

## 🗃️ Repositories (Data Access Layer)

### Primary Data Repositories
```java
fin.repository.CompanyRepository
├── Purpose: Company CRUD operations
├── Key Methods: getCompany(), getAllCompanies(), createCompany()
├── Database: Direct JDBC to companies table
└── Usage: Company selection and management

fin.repository.AccountRepository
├── Purpose: Chart of accounts management
├── Key Methods: getAccountById(), getAccountByCode(), createAccount()
├── Database: Direct JDBC to accounts table
├── Relationships: Links to journal_entry_lines via account_id
└── Usage: Account lookup during classification and reporting

fin.repository.FinancialDataRepository (Interface)
├── Purpose: Centralized financial data access interface
├── Implementations: JdbcFinancialDataRepository
├── Key Methods: getBankTransactions(), getAccountBalancesByType(), getTrialBalance()
├── Usage: Financial reporting services
└── Database: Multi-table queries for reporting

fin.repository.JdbcFinancialDataRepository (Implementation)
├── Purpose: JDBC implementation of FinancialDataRepository
├── Key Methods: 
│   ├── getBankTransactions() - Retrieves classified transactions
│   ├── getJournalEntries() - Journal entry queries
│   ├── getAccountBalancesByType() - Summarized balances
│   └── getTrialBalance() - Trial balance calculations
├── Database: Complex joins across bank_transactions, journal_entries, accounts
└── Usage: All financial reporting services depend on this
```

### Specialized Repositories
```java
fin.repository.BaseRepository (Abstract)
├── Purpose: Common database operations
├── Key Methods: getConnection(), executeQuery(), executeUpdate()
├── Database: Connection management and common JDBC patterns
└── Extended By: Most repository classes

// Note: Some services act as their own repositories (anti-pattern)
TransactionMappingService
├── Contains: Direct database access mixed with business logic
├── Database: Direct JDBC in service layer (should be in repository)
├── Issue: Violates separation of concerns
└── Target: Extract to proper repository class
```

## 🔧 Services (Business Logic Layer)

### Classification Services (REDUNDANT - TARGET FOR CONSOLIDATION)

#### Primary Classification Services
```java
fin.service.ClassificationIntegrationService
├── Purpose: Main orchestrator for transaction classification
├── Key Methods: classifyTransaction(), classifyBatch()
├── Dependencies: TransactionMappingService, RuleMappingService
├── Usage: Entry point for most classification operations
├── Status: KEEP - but consolidate logic
└── Integration: Calls multiple other classification services

fin.service.TransactionMappingService (2000+ lines!)
├── Purpose: Core classification logic with hardcoded rules
├── Key Methods: 
│   ├── mapTransactionToAccount() - Main classification logic
│   ├── classifyAllUnclassifiedTransactions() - Batch processing
│   ├── reclassifyAllTransactions() - Re-classification
│   ├── createJournalEntryForTransaction() - Journal generation
│   └── initializeStandardAccounts() - Account setup
├── Database: Direct JDBC mixed with business logic (anti-pattern)
├── Issues: 
│   ├── Too large (2000+ lines)
│   ├── Mixed concerns (classification + journal generation + account setup)
│   ├── Hardcoded classification rules
│   └── Direct database access in service layer
├── Status: CONSOLIDATE - extract and simplify
└── Target: Split into focused services
```

#### Rule Management Services
```java
fin.service.RuleMappingService (OLD - SCHEMA CONFLICTS)
├── Purpose: Manages classification rules (old implementation)
├── Key Methods: createRule(), getMatchingRule(), updateRule()
├── Database: Uses 'match_value' column (old schema)
├── Issues: Schema conflicts with newer implementation
├── Status: DEPRECATED - migrate to TransactionMappingRuleService
└── Conflicts: With TransactionMappingRuleService

fin.service.TransactionMappingRuleService (NEW - BETTER DESIGN)
├── Purpose: Manages classification rules (new implementation)
├── Key Methods: createRule(), findMatchingRule(), persistStandardRules()
├── Database: Uses 'pattern_text' column (new schema)
├── Design: Proper repository pattern, better structure
├── Status: KEEP - use as foundation for consolidation
└── Usage: Preferred for new rule management
```

#### Account & Chart Management
```java
fin.service.ChartOfAccountsService
├── Purpose: Chart of accounts initialization and management
├── Key Methods: initializeChartOfAccounts(), getStandardAccounts()
├── Integration: Works with AccountClassificationService
├── Status: KEEP - single source for account setup
└── Usage: Account structure initialization

fin.service.AccountClassificationService (SINGLE SOURCE OF TRUTH)
├── Purpose: Centralized account definitions and classification rules
├── Key Methods: 
│   ├── getStandardAccountDefinitions() - Account structure
│   ├── getStandardMappingRules() - Classification rules
│   └── getAccountCategories() - Account categorization
├── Design: Single source of truth for all account-related definitions
├── Status: KEEP - foundation for consolidation
├── Usage: Referenced by multiple services
└── Integration: Core component for all classification logic
```

#### Deprecated/Redundant Services
```java
fin.service.TransactionClassifier
├── Purpose: Thin wrapper around other classification services
├── Key Methods: classify() - delegates to other services
├── Issues: Adds no value, just another layer
├── Status: DEPRECATED - remove entirely
└── Usage: Should be replaced by direct service calls

fin.app.TransactionClassifier (Different from service!)
├── Purpose: Another thin wrapper in app package
├── Issues: Same name as service class, adds confusion
├── Status: DEPRECATED - remove entirely
└── Conflicts: Naming conflict with service class
```

### Journal & Financial Services
```java
fin.service.JournalEntryGenerator
├── Purpose: Creates journal entries from bank transactions
├── Key Methods: createJournalEntryForTransaction(), validateBalance()
├── Dependencies: AccountRepository
├── Usage: Called after transaction classification
├── Status: KEEP - core functionality
└── Integration: Used by TransactionMappingService

fin.service.GeneralLedgerService
├── Purpose: General ledger calculations and reporting
├── Key Methods: getAccountClosingBalances(), generateGL()
├── Dependencies: FinancialDataRepository
├── Usage: Financial reporting (Trial Balance, Balance Sheet, Income Statement)
├── Status: KEEP - core reporting
└── Database: Uses JdbcFinancialDataRepository

fin.service.FinancialReportingService
├── Purpose: Coordinates all financial report generation
├── Key Methods: generateTrialBalance(), generateBalanceSheet(), generateIncomeStatement()
├── Dependencies: Multiple specialized services
├── Usage: Main entry point for financial reports
├── Status: KEEP - orchestration layer
└── Integration: Calls specialized reporting services
```

### Data Management Services
```java
fin.service.BankStatementProcessingService
├── Purpose: Processes bank statement text into BankTransaction objects
├── Key Methods: parseBankStatement(), extractTransactions()
├── Dependencies: Document processing utilities
├── Usage: First step in transaction import workflow
├── Status: KEEP - core functionality
└── Flow: PDF → Text → BankTransaction objects

fin.service.DocumentTextExtractor
├── Purpose: Extracts text from PDF bank statements
├── Key Methods: extractText(), processPDF()
├── Dependencies: Apache PDFBox
├── Usage: PDF processing for bank statements
├── Status: KEEP - core functionality
└── Technology: PDFBox 3.0.0
```

## 🎮 Controllers (Presentation Layer)

### Data Management Controllers
```java
fin.controller.DataManagementController
├── Purpose: Main data management menu and operations
├── Key Methods:
│   ├── showDataManagementMenu() - Main menu
│   ├── showTransactionClassificationMenu() - Classification submenu
│   ├── handleTransactionClassificationOption() - Menu handlers
│   ├── interactiveClassification() - Manual classification
│   ├── autoClassifyUnclassified() - Batch classification
│   └── reclassifyAllTransactions() - Re-classification
├── Dependencies: Multiple classification services
├── Issues: Menu options 4 & 6 are redundant (as identified)
├── Status: MODIFY - Phase 1 target for menu consolidation
└── Usage: Primary user interface for data management

fin.controller.ReportController
├── Purpose: Financial report generation interface
├── Key Methods: generateTrialBalance(), generateBalanceSheet()
├── Dependencies: FinancialReportingService
├── Usage: Report generation from main menu
├── Status: KEEP - separate concern
└── Integration: Uses data processed by DataManagementController
```

### Supporting Controllers
```java
fin.controller.CompanyController
├── Purpose: Company selection and management
├── Key Methods: selectCompany(), createCompany()
├── Dependencies: CompanyService
├── Usage: Company context for all operations
├── Status: KEEP - core functionality
└── Scope: Sets company context for data management

fin.controller.FiscalPeriodController
├── Purpose: Fiscal period management
├── Key Methods: selectFiscalPeriod(), createPeriod()
├── Dependencies: FiscalPeriodService
├── Usage: Period context for all operations
├── Status: KEEP - core functionality
└── Scope: Sets period context for data management
```

## 🔄 Data Flow Analysis

### Transaction Processing Flow
```
1. PDF Import Flow:
   DocumentTextExtractor → BankStatementProcessingService → BankTransaction objects
   
2. Classification Flow:
   DataManagementController → ClassificationIntegrationService → TransactionMappingService
   ↓
   AccountClassificationService (rules) → Account lookup → Classified transactions
   
3. Journal Entry Flow:
   Classified transactions → JournalEntryGenerator → JournalEntry + JournalEntryLine objects
   
4. Reporting Flow:
   Journal entries → FinancialDataRepository → ReportController → Financial statements
```

### Menu Navigation Flow
```
Main Menu → Data Management → Transaction Classification
├── 1. Interactive Classification → ClassificationIntegrationService
├── 2. Auto-Classify → TransactionMappingService.classifyAllUnclassified()
├── 3. Reclassify ALL → TransactionMappingService.reclassifyAllTransactions()
├── 4. Re-classify Manual → REDUNDANT (same as #1) ← DELETE
├── 5. Initialize Accounts → ChartOfAccountsService
├── 6. Initialize Rules → REDUNDANT (included in #5) ← DELETE
├── 7. Sync Journal Entries → JournalEntryGenerator
└── 8. Regenerate ALL Journal → JournalEntryGenerator (full rebuild)
```

## 🚨 Identified Issues & Redundancies

### Service Layer Issues
1. **Multiple Classification Entry Points**
   - `ClassificationIntegrationService` (orchestrator)
   - `TransactionMappingService` (main logic)
   - `TransactionClassifier` (deprecated wrapper)

2. **Schema Conflicts**
   - `RuleMappingService` uses `match_value` column
   - `TransactionMappingRuleService` uses `pattern_text` column
   - Same table, different column names

3. **Mixed Concerns**
   - `TransactionMappingService` does classification + journal generation + account setup
   - Services contain direct database access (should be in repositories)

4. **Initialization Scattered**
   - Chart of accounts setup in multiple places
   - Mapping rules initialization separate from account setup

### Controller Layer Issues
1. **Menu Redundancy**
   - Option 4 "Re-classify Manual" = same as Option 1 "Interactive"
   - Option 6 "Initialize Rules" = subset of Option 5 "Initialize Accounts"

2. **Service Dependencies**
   - Controller directly calls multiple overlapping services
   - No clear single entry point for classification operations

## 🎯 Consolidation Targets

### Phase 1: Menu Simplification (IMMEDIATE)
- **File:** `DataManagementController.java`
- **Remove:** Options 4 & 6
- **Consolidate:** Chart of accounts + mapping rules initialization
- **Renumber:** Remaining options

### Phase 2: Service Consolidation (NEXT)
- **Create:** `TransactionClassificationService` (unified)
- **Consolidate:** `ClassificationIntegrationService` + core `TransactionMappingService` logic
- **Keep:** `AccountClassificationService` (single source of truth)
- **Keep:** `TransactionMappingRuleService` (better design)
- **Remove:** `TransactionClassifier`, `RuleMappingService`

### Phase 3: Repository Extraction (LATER)
- **Extract:** Database access from `TransactionMappingService`
- **Create:** Proper repository classes
- **Maintain:** Clean separation of concerns

---

## 📋 Component Summary

### KEEP (Core Components)
- ✅ **Models:** All domain entities (BankTransaction, JournalEntry, Account, etc.)
- ✅ **Repositories:** FinancialDataRepository, AccountRepository, CompanyRepository
- ✅ **Services:** AccountClassificationService, TransactionMappingRuleService, ChartOfAccountsService
- ✅ **Controllers:** Core functionality in DataManagementController (after cleanup)

### CONSOLIDATE (Merge Redundant)
- 🔄 **Services:** ClassificationIntegrationService + TransactionMappingService → TransactionClassificationService
- 🔄 **Menu:** Options 5 & 6 → Single "Initialize Chart & Rules" option

### REMOVE (Deprecated/Redundant)
- ❌ **Services:** TransactionClassifier, RuleMappingService
- ❌ **Menu Options:** #4 Re-classify Manual, #6 Initialize Rules

This comprehensive mapping provides the foundation for systematic consolidation of the data management architecture.