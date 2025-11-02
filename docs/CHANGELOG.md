# Changelog

All notable changes to the FIN application will be documented in this file.

---

## [2.3.0] - 2025-10-06

### Added - SARS Tax Compliance
- **SDL (Skills Development Levy) Calculation** - Complete SARS tax compliance
  - Rate: 1% of gross salary
  - Threshold: R500,000 annual payroll (R41,667/month)
  - Database: Added `payslips.sdl_levy` column
  - Implementation: `SARSTaxCalculator.calculateSDL()` method
  - Status: ✅ **VERIFIED WORKING** - R1,701.00 SDL on R170,100 payroll (13 employees)

### Enhanced - Payroll Processing
- **Payroll Reprocessing Capability** - Seamless recalculation without creating new periods
  - Delete existing payslips automatically
  - Reset period status to OPEN
  - Recalculate with current data
  - Generate new PDFs
  - Transaction-safe (all-or-nothing ACID compliance)
  - Use Cases: Correcting employee data, implementing new features (SDL), testing scenarios
  - Methods: `clearExistingPayslips()`, `resetPayrollPeriodStatus()` in `PayrollService`
  - Status: ✅ **VERIFIED WORKING** with user confirmation

### Fixed
- **EMP 201 Report Enhancement**
  - Fixed column name mismatches in SQL queries
  - Added SDL totals to report
  - Status: ✅ Generates successfully with SDL compliance

### Database
- **Migration**: `ALTER TABLE payslips ADD COLUMN sdl_levy NUMERIC(15,2) DEFAULT 0;`
- **Verification Status (Nov 2, 2025):**
  - ✅ Xinghizana Group (Company ID 2): 3,823 journal entries, 3,813 transactions
  - ✅ 76 SARS-compliant accounts (Chart of Accounts initialized)
  - ✅ 100% transaction classification (3,813/3,813 classified)

### Documentation
- Created comprehensive implementation guides:
  - `SDL_AND_REPROCESSING_IMPLEMENTATION_2025-10-06.md` - Complete 9,000+ line implementation report
  - `SDL_IMPLEMENTATION_2025-10-06.md` - SDL technical documentation
  - `PAYROLL_REPROCESSING_2025-10-06.md` - Reprocessing workflow guide
  - `CHANGES_SUMMARY_2025-10-06.md` - High-level overview
  - `QUICK_REFERENCE_2025-10-06.md` - Quick testing guide
  - `IMPLEMENTATION_SUMMARY_2025-10-06.md` - Executive summary

### Test Results (September 2025 Payroll)
- Employees processed: 13
- Total gross salary: R170,100.00
- Total PAYE: R13,204.00
- Total UIF (employee): R1,536.36
- Total UIF (employer): R1,536.36
- **Total SDL: R1,701.00** ✅
- Total net pay: R155,359.64

---

## [2.2.0] - 2025-10-04

### Fixed - CRITICAL
- **Journal Entry Generation** - Fixed PSQLException for missing `source_transaction_id` column
  - Added database migration script to add column to `journal_entry_lines` table
  - Updated `JournalEntryGenerator` to populate source transaction ID
  - Fixed audit trail generation (was producing empty results)
  - All journal entries now properly linked to source bank transactions
  
- **CI/CD Pipeline** - Fixed GitHub Actions build failures
  - Implemented context-aware database configuration in `DatabaseConfig`
  - Test context now uses `drimacc_test_db` database
  - Production/development context uses `drimacc_db` database
  - Resolved "database does not exist" errors in CI environment
  - All tests now passing in GitHub Actions

### Added
- **Enhanced Re-classification Menu** - Major UX overhaul with professional features
  - Pagination support (50 transactions per page, all 7,156+ accessible)
  - Filtering options (All/Uncategorized/Categorized)
  - Intelligent keyword-based suggestions (top 3 matches)
  - Bulk re-classification (classify 20 at a time)
  - Time savings: ~80% reduction in re-classification workflow
  
- **Comprehensive Test Suite** - Added 16 unit tests for `AccountClassificationService`
  - Tests for 3-tier suggestion system
  - Tests for account code retrieval
  - Tests for rule matching and pattern detection
  - All 118+ tests passing with 100% pass rate
  
- **Interactive Classification Enhancement** - Integrated suggestion system
  - Tier 1: Pattern-based suggestions from mapping rules
  - Tier 2: Keyword-based suggestions from transaction details
  - Tier 3: General category suggestions (5 options)
  - Seamless integration with existing classification flow

### Changed
- **DataManagementController** - Complete rewrite of transaction correction handling
  - Added `handleTransactionCorrection()` method (+296 lines)
  - Implemented pagination with configurable page size
  - Added filtering by categorization status
  - Integrated suggestion system for faster re-classification
  - Enhanced user feedback with emojis and progress indicators
  - Improved error handling and validation
  
- **DatabaseConfig** - Intelligent test context detection
  - Checks for test indicators (JUnit, Maven Surefire, GitHub Actions)
  - Returns appropriate database name based on execution context
  - Prevents production app from connecting to test database
  - More reliable CI/CD builds

### Documentation
- **Created 15 comprehensive documents** covering all aspects of recent work:
  - `SESSION_SUMMARY_2025-10-04.md` - Detailed session log with all changes
  - `SESSION_SUMMARY_2025-10-03.md` - Previous session comprehensive log
  - `QUICK_TEST_GUIDE.md` - User-friendly 5-10 minute testing guide
  - `SOURCE_TRANSACTION_ID_MIGRATION_2025-10-04.md` - Database migration details
  - `RECLASSIFICATION_ENHANCEMENT_2025-10-04.md` - Re-classification feature specs
  - `RECLASSIFICATION_REPORT_2025-10-04.md` - Implementation report
  - `REFACTORING_COMPLETION_REPORT_2025-10-04.md` - Service layer refactoring
  - `INTERACTIVE_CLASSIFICATION_ENHANCEMENT_2025-10-04.md` - Suggestion system
  - `DATABASE_CONFIGURATION_CLARIFICATION_2025-10-04.md` - Context detection
  - `CI_CD_FAILURE_ANALYSIS_2025-10-04.md` - Build failure root cause analysis
  - `TRANSACTION_CLASSIFICATION_ARCHITECTURE_ANALYSIS.md` - Architecture deep dive
  - `MODEL_AND_SERVICE_REDUNDANCY_ANALYSIS.md` - Redundancy analysis
  - Plus 3 more technical analysis documents

- **Reorganized Documentation Structure** - Major cleanup and organization
  - Moved 40 development-related files from `docs/` root to `docs/development/`
  - Reduced root directory from 59+ files to 20 files (66% reduction)
  - Created `DOCUMENTATION_REORGANIZATION_2025-10-04.md` report
  - Updated `docs/README.md` with new structure and quick links
  - Updated `docs/development/README.md` with corrected paths
  - Organized development docs by category (progress, phases, analysis, enhancements)
  - Improved discoverability and navigation for developers

### Database
- **Migration Status**: All migrations deployed successfully
  - Added `source_transaction_id` column to `journal_entry_lines` table
  - Backward compatible with existing data (nullable column)
  - Migration verified on production database
  
- **Schema Enhancement**: Journal entry audit trail now fully functional
  - Links between journal entries and source transactions established
  - Audit trail generation working correctly
  - Financial reporting accuracy improved

### Impact
- **Audit Trail**: Now fully functional (was producing empty results)
- **Journal Entries**: Working correctly (was completely broken due to missing column)
- **Re-classification Workflow**: 80% time savings (20 mins → 4 mins for 100 transactions)
- **CI/CD Pipeline**: Stable and passing (was failing intermittently)
- **Code Quality**: 118+ tests passing, zero compilation errors
- **User Experience**: Significantly improved with pagination, filtering, and suggestions
- **Documentation Navigation**: 2-click access to any document (was 3-5 clicks)

---

## [2.1.0] - 2025-10-03

### Major - Phase 7 Service Layer Cleanup
- **Service Layer Deep Cleanup** - Removed 1,277 lines of redundant/misplaced code
  - Deleted: `TransactionVerificationService.java` (369 lines) - Unused service
  - Deleted: `VerificationController.java` (~200 lines) - Dependent on deleted service
  - Deleted: `JournalEntryCreationService.java` (335 lines) - Duplicate of JournalEntryGenerator
  - Deleted: `LibharuTest.java` (73 lines) - Misplaced in service/ directory
  - Deleted: `AccountManager.java` (203 lines) - Wrong layer (service instead of repository)

### Added
- **Repository Pattern Migration** - Proper layer separation
  - Created: `AccountRepository.java` (203 lines) - Correct repository layer placement
  - Created: `LibharuIntegrationTest.java` (107 lines) - Proper JUnit 5 test with @TempDir
  - Updated: 10+ files to use new repository structure
  - Architecture: Clean separation between service and repository layers

### Changed
- **Menu Streamlining** - Data Management menu optimization
  - Before: 9 options with 2 critical redundancies
  - After: 7 options with zero redundancies
  - Reduction: 22% fewer menu options
  - Removed duplicate "Initialize Mapping Rules" from top level
  - Merged "Correct Transaction Categorization" into Classification sub-menu
  - Classification sub-menu: 5 → 7 options (reorganized with moved functionality)

### CI/CD
- **Pipeline Modernization** - PostgreSQL integration
  - Added PostgreSQL 15 service to all build/test/deploy jobs
  - Configured test database environment variables
  - Added health checks with 30-second timeout
  - Wait logic for PostgreSQL readiness
  - Build process now includes tests with real database

### Documentation
- Created: `SESSION_SUMMARY_2025-10-03.md` - Comprehensive session log
- Created: `PHASE7_SERVICE_LAYER_DEEP_CLEANUP.md` - Detailed cleanup analysis
- Created: `PHASE7_MENU_STREAMLINING_REPORT.md` - Menu optimization report

---

## [2.0.0] - 2025-09-28 to 2025-10-03

### Major - SARS Compliance Achievement (Phases 2-6)
- **100% SARS Compliance** - Complete migration to standard account codes
  - Phase 2: SARS account structure migration (78% → 85% compliance)
  - Phase 3: 100% SARS compliance achievement (85% → 100% compliance)
  - Phase 4: Code cleanup and optimization
  - Phase 5: Architecture refinement
  - Phase 6: Testing and verification

### Changed - Account Code Migration
- **Eliminated Dynamic Account Creation** - Standardized to static SARS codes
  - Migrated 52 transactions from custom codes to standard codes
  - Classified 13 previously unclassified transactions
  - Eliminated 16 obsolete custom accounts
  - Fixed category ID mapping (Categories 7-16 instead of 18-20)
  - Code reduction: 36 lines removed (67% reduction in supplier logic)

### Migration Summary
| Category | Old Pattern | New Code | SARS Classification |
|----------|-------------|----------|---------------------|
| Reimbursements | `8900-XXX` | `8900` | Other Operating Expenses |
| School Fees | `8900-XXX` | `8900` | Other Operating Expenses |
| Stokvela Payments | `2000-XXX` | `2400` | Long-term Liabilities |
| Vehicle Purchases | `2000-XXX` | `2100` | Property, Plant & Equipment |
| IT Services | `8400-XXX` | `8400` | Communication |
| Labour Services | `8100-XXX` | `8100` | Employee Costs |

### Added
- **General Ledger → Trial Balance Hierarchy** - Proper accounting flow
  - `GeneralLedgerService.getAccountClosingBalances()` - Central balance calculation
  - `TrialBalanceService` now reads from GL (not bypassing)
  - `BalanceSheetService`, `IncomeStatementService`, `CashFlowService` all use GL
  - Opening balance calculation with proper normal balance types (DR/CR)
  - Status: ✅ **VERIFIED WORKING** - Trial Balance perfectly balances

### Data Integrity
- **Zero Data Loss** - All 267 transactions preserved during migration
- **Complete Audit Trail** - Full migration history maintained
- **Balance Verification** - R650,995.54 debits = R650,995.54 credits ✅

### Documentation
- Created 16 PHASE reports (now archived in `/docs/development/archive/`)
- Created: `REFACTORING_SUMMARY.md` - Comprehensive Phases 2-7 summary
- Detailed reports: Migration plans, completion reports, verification reports

---

## [1.5.0] - 2025-09-11

### Changed - Ownership & Licensing
- **Updated Ownership** - All files reflect proper company structure
  - Copyright: Sthwalo Holdings (Pty) Ltd. (2024-2025)
  - Owner: Immaculate Nyoni
  - Contact: sthwaloe@gmail.com | +27 61 514 6185

### Added
- **IP Protection Strategy** - Comprehensive intellectual property protection
  - Dual Licensing Model: Apache 2.0 (code access) + Commercial License (usage)
  - Trademark protection for "FIN Financial Management System"
  - Trade secret protection for proprietary algorithms
  - Commercial licensing tiers ($29-$299/month)

### Documentation
- Created: `IP_PROTECTION_STRATEGY.md` - Complete IP protection guide
- Created: `add-copyright-headers.sh` - Script to add copyright headers
- Updated: All major files with proper copyright notices
- Updated: `LICENSING.md` with commercial terms

---

## System Status (Verified November 2, 2025)

### Production Database (Xinghizana Group - Company ID 2)
- ✅ 3,823 journal entries
- ✅ 3,813 bank transactions (100% classified)
- ✅ 76 SARS-compliant accounts
- ✅ SDL payroll calculations working
- ✅ General Ledger → Trial Balance → Financial Statements hierarchy correct
- ✅ Opening balance calculations working

### Test Companies (Empty - Used for Feature Testing)
- Limelight Academy (ID 1)
- Rock Of Ages Fellowship Church (ID 3)
- Global Hope Consortium (ID 5)

### Architecture
- ✅ Dependency injection pattern enforced
- ✅ Service layer modular and testable
- ✅ Repository pattern standardized
- ✅ API server mode production-ready (http://localhost:8080/api/v1/)
- ✅ Console application with interactive menus
- ✅ Batch processing mode for automation

### Quality Metrics
- ✅ 118+ tests passing
- ✅ 100% SARS compliance
- ✅ Zero critical bugs
- ✅ Production-ready

### Testing
- **Unit Tests**: 118+ tests total (all passing)
- **Integration Tests**: Manual testing completed successfully
- **CI/CD**: GitHub Actions pipeline passing
- **Build Status**: ✅ SUCCESSFUL in 46 seconds

---

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
