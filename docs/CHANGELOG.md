# Changelog

All notable changes to the FIN application will be documented in this file.

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
