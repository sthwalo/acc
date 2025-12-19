### Recent Updates 

### ✅ SDL (Skills Development Levy) Implementation - COMPLETED
- Automatic SDL calculation (1% of payroll for companies > R500k/year)
- Integrated into payroll processing and database schema
- SARS compliant tax reporting (included in EMP 201)
- Tested with 13 employees: R1,701.00 SDL on R170,100 payroll
- See: `/docs/SDL_IMPLEMENTATION_2025-10-06.md`

### ✅ Payroll Reprocessing Enhancement - COMPLETED
- Can now reprocess payroll periods without errors or duplicates
- Automatic clearing of existing payslips with user confirmation
- Transaction-safe recalculation (ACID compliant)
- In-memory status synchronization with database
- Tested successfully with September 2025 payroll (13 employees)
- See: `/docs/PAYROLL_REPROCESSING_2025-10-06.md`

### ✅ EMP 201 Report Enhancement - COMPLETED
- Fixed database column name mismatches
- Added SDL totals to SARS tax submission report
- Debug output shows all statutory amounts
- Report generates successfully as PDF
- See: `/docs/SDL_AND_REPROCESSING_IMPLEMENTATION_2025-10-06.md` (Complete Report)

**Quick Reference:** See `/docs/QUICK_REFERENCE_2025-10-06.md` for testing instructions  
**Complete Implementation Report:** `/docs/SDL_AND_REPROCESSING_IMPLEMENTATION_2025-10-06.md`gement System

A comprehensive Java-based financial management system with PostgreSQL database, PDF processing, Excel reporting, and REST API capabilities.

## � Recent Updates (October 6, 2025)

### ✅ SDL (Skills Development Levy) Implementation
- Automatic SDL calculation (1% of payroll for companies > R500k/year)
- Integrated into payroll processing and database
- SARS compliant tax reporting
- See: `/docs/SDL_IMPLEMENTATION_2025-10-06.md`

### ✅ Payroll Reprocessing Enhancement
- Can now reprocess payroll periods without errors
- Automatic clearing of existing payslips
- No need to create duplicate periods
- Transaction-safe recalculation
- See: `/docs/PAYROLL_REPROCESSING_2025-10-06.md`

**Quick Reference:** See `/docs/QUICK_REFERENCE_2025-10-06.md` for testing instructions

---

## 🚀 Quick Start - Container Mode

**One-command full-stack startup with automatic browser launch:**

```bash
# Start everything (backend, database, frontend) and open browser
./start.sh

# Or from frontend directory:
npm run start:containers
```

This will:
- ✅ Start PostgreSQL database
- ✅ Start FIN backend API on port 8080
- ✅ Start React frontend on port 3000
- ✅ Automatically open http://localhost:3000 in your browser
- ✅ Configure CORS for seamless frontend-backend communication

**Stop containers:**
```bash
docker compose down
```

**View logs:**
```bash
docker compose logs -f
```

---

## 🏢 System Overview

FIN is a production-ready financial management system that handles:
- **Bank Statement Processing** - Automated PDF text extraction and transaction parsing
- **Financial Reporting** - Complete financial statements with real database values
- **REST API Server** - RESTful API with CORS support for frontend integration
- **Excel Generation** - Professional financial reports in Excel format
- **Payroll Management** - Employee management, tax calculations, and payslip generation
- **Transaction Classification** - Intelligent categorization using business rules
- **Data Management** - Manual journal entries, data corrections, and verification

## 🏗️ Architecture

### **Database:** PostgreSQL (Production-ready)
- Companies, employees, fiscal periods, accounts, journal entries
- Transaction processing with 7,156+ real financial transactions
- Full ACID compliance and foreign key constraints

### **Modular Service Architecture**
The system uses a modern modular architecture with 5 core services:

#### **✅ Fully Implemented Services (5/5)**
- **TransactionClassifier** - Intelligent transaction classification using pattern matching
- **AccountManager** - Account creation, lookup, and category management
- **JournalEntryGenerator** - Automated double-entry journal entry creation
- **RuleMappingService** - Database-driven rule management for classification
- **TransactionBatchProcessor** - Batch processing orchestration for large volumes

### **Application Modes**
1. **REST API Server** (Recommended) - Full REST API with CORS for frontend integration
2. **Console Application** - Interactive terminal interface with menu system
3. **Batch Processing** - Automated processing for large transaction volumes

### **Processing Pipeline**
```
Bank PDFs → Text Extraction → Transaction Parsing → Classification → Journal Entries → PostgreSQL → Financial Reports
```

## 🚀 Quick Start

### **Prerequisites**
- Java 17+ (see [Java Version Management Guide](docs/development/JAVA_VERSION_MANAGEMENT.md) for setup)
- PostgreSQL 17+ (configured and running)
- Environment variables set (see `.env`)

### **1. REST API Server Mode (Recommended)**
```bash
cd /path/to/fin-project
source .env
java -jar app/build/libs/fin-spring.jar api
```

> Note: when building Docker images we copy `fin-spring.jar` to `/app/app.jar` inside the container; examples inside container images may therefore reference `/app/app.jar`.

**API Endpoints:**
- Health: `http://localhost:8080/api/v1/health`
- Companies: `http://localhost:8080/api/v1/companies`
- Documentation: `http://localhost:8080/api/v1/docs`
- File Upload: `POST /api/v1/companies/{id}/upload`

### **2. Console Application Mode**
```bash
cd /path/to/fin-project
source .env
java -jar app/build/libs/fin-spring.jar
```
**Features:**
- Interactive menu system
- Company and fiscal period management
- Bank statement processing
- Financial reporting (Cashbook, Ledger, Trial Balance, Income Statement)
- Data management and verification
- Payroll management

### **3. Batch Processing Mode**
```bash
cd /path/to/fin-project
source .env
java -jar app/build/libs/fin-spring.jar --batch [command]
```**Batch Commands:**
- Automated transaction processing
- Bulk data imports
- Report generation

## 📊 Current Capabilities

### **Live Data Processing**
- **Transaction Processing:** 7,156+ real financial transactions processed
- **Bank Statement Parsing:** Automated PDF text extraction from multiple bank formats
- **Intelligent Classification:** Pattern-based transaction categorization
- **Double-Entry Accounting:** Automated journal entry generation with balance validation

### **Financial Reports Available**

#### **✅ Console Reports (Fully Implemented)**
- **Cashbook Report** - Detailed transaction listings with running balances
- **General Ledger** - Account-grouped transactions with subtotals
- **Trial Balance** - Debit/credit verification across all accounts
- **Income Statement** - Revenue vs expenses with profit/loss calculation
- **Balance Sheet** - Assets, liabilities, and equity positions

#### **✅ Excel Reports (Professional Format)**
- **Complete Balance Sheet** - Assets, Liabilities, Equity with real amounts
- **Income Statement** - Revenue and Expenses with calculations
- **Trial Balance** - All accounts with debit/credit totals
- **15+ professional sheets** including Cover, Index, Company Details

#### **✅ Payroll System**
- **Employee Management** - Complete employee records with banking details
- **Tax Calculations** - PAYE, UIF, and medical aid deductions
- **Payslip Generation** - PDF payslips with detailed breakdowns
- **Email Distribution** - Automated payslip delivery via SMTP

### **🔧 Advanced Features**
- **Batch Processing** - Large volume transaction processing
- **Data Verification** - Transaction validation and reconciliation
- **Interactive Classification** - Manual transaction categorization
- **Multi-Company Support** - Separate data isolation per company
- **REST API Integration** - Full CRUD operations via HTTP endpoints

## 🔧 Development

### **Building**
```bash
./gradlew clean build
./gradlew test
```

### **Database Setup**
```bash
# PostgreSQL must be running with credentials in .env
source .env
psql -h localhost -U [YOUR_DB_USER] -d [YOUR_DB_NAME] -c "SELECT COUNT(*) FROM accounts;"
```

### **Environment Configuration**
See `.env` file for database connection settings:
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/[YOUR_DATABASE]
DATABASE_USER=[YOUR_USERNAME]  
DATABASE_PASSWORD=[YOUR_PASSWORD]
```

## 📋 System Status

### **✅ Production Ready Components**
- **PostgreSQL Database** - Full production schema with real financial data
- **Modular Service Architecture** - All 5 core services fully implemented and tested
- **REST API Server** - Complete CRUD operations with CORS support
- **PDF Processing Pipeline** - Automated bank statement text extraction
- **Excel Reporting** - Professional financial statements with real data
- **Payroll System** - Complete employee management and payslip generation
- **Transaction Processing** - Intelligent classification and journal entry creation

### **� Development & Quality**
- **Test Coverage** - 118+ unit tests passing across all services
- **Code Quality** - Checkstyle and SpotBugs integration
- **Build System** - Gradle with fat JAR creation
- **Dependency Injection** - Clean architecture with service separation
- **Error Handling** - Comprehensive exception management

### **📊 Live Financial Data**
The system contains **real production financial data** with:
- Multi-company support with proper data isolation
- Complete chart of accounts with 45+ accounts
- 7,156+ processed transactions with accurate balances
- Automated transaction classification and journal entries

## 🗂️ Documentation

Detailed documentation available in `/docs`:
- [Complete System Architecture](SYSTEM_ARCHITECTURE_STATUS.md)
- [Usage Guide](docs/USAGE.md)
- [Development Guide](docs/DEVELOPMENT.md)
- [PostgreSQL Migration Guide](docs/POSTGRESQL_MIGRATION_GUIDE.md)
- [Technical Specifications](docs/system_architecture/TECHNICAL_SPECIFICATIONS.md)

## 📱 API Endpoints

**Base URL:** `http://localhost:8080/api/v1/`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | System health check with database status |
| GET | `/docs` | API documentation and endpoint listing |
| GET | `/companies` | List all companies |
| POST | `/companies` | Create new company |
| GET | `/companies/{id}/fiscal-periods` | Get fiscal periods for company |
| GET | `/companies/{id}/transactions` | Get transactions for company (placeholder) |
| POST | `/companies/{id}/upload` | Upload and process bank statement PDFs |
| POST | `/companies/{id}/process-local` | Process local PDF files for testing |

**CORS Support:** Enabled for `http://localhost:3000` (frontend development)

**Dual License Model:**

### 🆓 **Personal Use - Apache License 2.0**
- FREE for personal finance management
- FREE for educational use and research
- FREE for open source development

### 💼 **Commercial Use - Commercial License Required**
- PAID for business financial management
- PAID for revenue-generating activities  
- PAID for hosting services to others

See [COMMERCIAL_LICENSE.md](COMMERCIAL_LICENSE.md) for pricing and [LICENSE](LICENSE) for source code terms.

## 🤝 Contributing

Contributions welcome! The system is actively developed with modern architecture:
- Spring-style dependency injection
- Repository pattern implementation
- Comprehensive error handling
- Production-ready database design

## 📞 Support & Contact

- **Owner:** Immaculate Nyoni | Sthwalo Holdings (Pty) Ltd.
- **Email:** sthwaloe@gmail.com
- **Phone:** +27 61 514 6185
- **Issues:** Open a GitHub issue
- **Documentation:** See `/docs` directory
- **Architecture:** Review `SYSTEM_ARCHITECTURE_STATUS.md`

---

**Status:** ✅ **PRODUCTION READY** | **Database:** PostgreSQL | **Architecture:** Modular Services | **API:** REST with CORS | **Reports:** Excel + Console | **Payroll:** Complete

## Documentation

Detailed documentation is available in the `docs` directory:

- [System Architecture](docs/SYSTEM_ARCHITECTURE_STATUS.md)
- [Usage Guide](docs/USAGE.md)
- [Development Guide](docs/DEVELOPMENT.md)
- [PostgreSQL Migration Guide](docs/POSTGRESQL_MIGRATION_GUIDE.md)
- [Technical Specifications](docs/system_architecture/TECHNICAL_SPECIFICATIONS.md)

## Building & Development

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run API server
java -jar app/build/libs/fin-spring.jar api

# Run console application
java -jar app/build/libs/fin-spring.jar
```

## Requirements

- **Java 17+** (OpenJDK or Oracle JDK)
- **PostgreSQL 17+** (configured and running)
- **Environment variables** (see `.env` template)

## ⚖️ License

**FIN Financial Management System uses a dual licensing model:**

### 🆓 **Personal Use - Apache License 2.0**
- ✅ **FREE** for personal finance management
- ✅ **FREE** for educational use and research
- ✅ **FREE** for open source development

### 💼 **Commercial Use - Commercial License Required**
- 💰 **PAID** for business financial management
- 💰 **PAID** for revenue-generating activities
- 💰 **PAID** for hosting services to others

**📋 See [COMMERCIAL_LICENSE.md](COMMERCIAL_LICENSE.md) for pricing and terms.**

---

**Copyright:** 2024-2025 Sthwalo Holdings (Pty) Ltd.  
**Owner:** Immaculate Nyoni  
**Contact:** sthwaloe@gmail.com | +27 61 514 6185
