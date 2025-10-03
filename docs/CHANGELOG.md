# Changelog

All notable changes to the FIN application will be documented in this file.

## [2.1.0] - 2025-10-03

### Changed - BREAKING
- **Chart of Accounts Architecture** - Unified three conflicting chart of accounts structures into single SARS-compliant source of truth (AccountClassificationService)
  - `TransactionClassificationService` constructor signature changed: removed `ChartOfAccountsService` parameter (4 params instead of 5)
  - All chart of accounts operations now use `AccountClassificationService` exclusively
  - Standard South African account code range (1000-9999) enforced throughout system

### Deprecated
- **ChartOfAccountsService** - Deprecated in favor of `AccountClassificationService` (scheduled for removal after 30 days)
  - Custom account range (4000-6999) conflicts with SARS standards
  - IDE will show deprecation warnings with migration path
  - Runtime warnings display when deprecated methods are called
  - See `/docs/CHART_OF_ACCOUNTS_REFACTORING_SUMMARY.md` for migration guide

### Fixed
- **Account Code Conflicts** - Resolved conflicts between three competing chart of accounts structures:
  - Account 5000: Fixed conflict between "Share Capital" (Equity) and "Other Income" - now exclusively "Share Capital"
  - Account 6000: Fixed conflict between "Sales Revenue" and "Reversals & Adjustments" - now exclusively "Sales Revenue"
  - Account 8310-001: Removed non-existent sub-account, replaced with 8400 "Communication"
- **Transaction Mapping Rules** - Fixed 10 hardcoded mapping rules to reference correct account codes:
  - Interest income: 5000 → 7000 (Interest Income)
  - Reversals/adjustments: 6000 → 7200 (Gain on Asset Disposal - temporary)
  - Telephone/mobile: 8310-001 → 8400 (Communication) - 6 rules fixed
- **Account Not Found Errors** - Eliminated all "Account with code X not found" errors by ensuring mapping rules reference existing accounts

### Added
- **Database Migration Script** - Created `scripts/migrate_chart_of_accounts_fixed.sql` to update existing `bank_transactions` records
  - Transaction-wrapped with automatic backup table creation
  - Updated 1 transaction: account 5000 → 7000 (Interest Income)
  - Includes verification queries and rollback instructions
  - Backup table: `bank_transactions_backup_20251003`
- **Comprehensive Documentation** - Added detailed refactoring documentation:
  - `/docs/CHART_OF_ACCOUNTS_REFACTORING_SUMMARY.md` - Complete 500+ line technical summary
  - `/docs/CHART_OF_ACCOUNTS_REFACTORING_QUICKREF.md` - Quick reference guide for developers
  - Updated test file comments to reflect deprecated services

### Technical Debt
- **Phase 1 Complete** - Successfully completed chart of accounts refactoring (Phase 1 of larger classification system overhaul)
- **Future Cleanup** - ChartOfAccountsService scheduled for deletion after 30 days of successful operation (target: 2025-11-03)
- **Account 7300** - Need to add proper "Reversals & Adjustments" account (currently using 7200 "Gain on Asset Disposal" as temporary placeholder)

### Database
- **Migration Status**: ✅ COMPLETED (1 transaction updated, backup created)
- **Backup Table**: `bank_transactions_backup_20251003` (1 row, keep for 30 days)
- **Verification**: All old account codes (8310-001, 5000, 6000) successfully migrated
- **Rollback**: Script provided in migration file

### Testing
- **Build Status**: ✅ SUCCESS - Zero compilation errors
- **Unit Tests**: 12 tests (10 existing + 2 new) - All passing
- **Integration Tests**: Pending - Manual verification required via `./run.sh`
- **Test Coverage**: Added tests for single source of truth architecture and deprecation handling

---

## [2.0.1] - 2025-09-28

### Fixed
- **Tax Calculation Bug** - Fixed SARSTaxCalculator to properly handle salaries below R5,586 threshold, resolving payslip generation failure for low-income employees (e.g., R5,500 salary)
- **Menu Navigation Issue** - Fixed PayrollController menu validation to accept option 7 "Back to Main Menu", allowing proper navigation flow
- **Employee Management Enhancement** - Enhanced employee update functionality to support all payslip-relevant fields including tax numbers, UIF numbers, medical aid numbers, pension fund numbers, and complete banking information

### Added
- **Document Management System** - Added comprehensive document management functionality to payroll system allowing users to list and delete payslip PDF documents from both `exports/` and `payslips/` directories with confirmation prompts
- **Payroll Period Deletion** - Added safe and force deletion methods for payroll periods with proper status validation and cascade operations
- **Enhanced Payroll Menu** - Updated payroll menu to include "Document Management" option and improved employee update form with current value display

### Changed
- **Code Quality Enforcement** - Added mandatory build verification requirement after any code changes to prevent regressions
- **Documentation Updates** - Updated copilot instructions with patch documentation and build enforcement policies

## [2.0.0] - 2025-09-26

### Added
- **Complete Modular Service Architecture** - All 5 core services fully implemented and tested
- **REST API Server** - Full REST API with CORS support for frontend integration
- **Payroll Management System** - Complete employee management, tax calculations, and payslip generation
- **Email Service** - SMTP email functionality for payslip distribution
- **Batch Processing** - Automated processing for large transaction volumes
- **Interactive Console Application** - Menu-driven interface for all operations
- **PostgreSQL Database** - Production-ready database with comprehensive schema
- **Transaction Classification** - Intelligent pattern-based transaction categorization
- **Professional Excel Reports** - Complete financial statements with real data
- **PDF Processing Pipeline** - Automated bank statement text extraction
- **Multi-Company Support** - Data isolation and management for multiple companies

### Changed
- **Architecture Refactor** - Migrated from monolithic App.java to modular service architecture
- **Database Migration** - Complete migration from SQLite to PostgreSQL
- **Dependency Injection** - Implemented ApplicationContext with service registration
- **Security Enhancement** - Removed hardcoded company information from source code
- **Documentation Update** - Comprehensive README and system documentation

### Fixed
- **Compilation Issues** - Added missing JNA and JavaMail dependencies
- **Model Classes** - Added missing fields and methods to Employee and Company classes
- **Security Vulnerabilities** - Removed company-specific data from source code

## [1.0.0] - 2025-09-02

### Added
- Initial modular architecture with service separation
- PostgreSQL database integration
- Basic PDF processing capabilities
- Financial reporting framework
- Unit testing infrastructure

### Changed
- Java version upgrade to Java 17
- Build system migration to Gradle
- Database backend migration to PostgreSQL

### Fixed
- Dependency management and compilation issues
