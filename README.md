# FIN Financial Management System

A comprehensive Java-based financial management application with PostgreSQL database, PDF processing, and Excel reporting capabilities.

## 🏢 System Overview

FIN is a production-ready financial management system that handles:
- **Bank Statement Processing** - Automated PDF text extraction and transaction parsing
- **Financial Reporting** - Complete financial statements with real database values
- **API Server** - RESTful API for frontend integration
- **Excel Generation** - Professional financial reports in Excel format
- **Data Management** - Manual journal entries, invoices, and data corrections

## 🏗️ Architecture

### **Database:** PostgreSQL (Production-ready)
- Companies, fiscal periods, accounts, journal entries
- 7,156+ transactions with real financial data
- Full ACID compliance and foreign key constraints

### **Modular Service Architecture**
The system is built using a modular service architecture with 5 core services:

#### **✅ Implemented Services (2/5)**
- **TransactionMappingService** - Rule-based transaction classification with database persistence
- **AccountManager** - Account creation, lookup, and category mapping with database persistence

#### **🔄 Planned Services (3/5)**
- **JournalEntryGenerator** - Automated journal entry creation for classified transactions
- **RuleMappingService** - Database-driven rule management for transaction classification
- **TransactionBatchProcessor** - Batch processing orchestration for large transaction volumes

### **Processing Pipeline:**
```
Bank PDFs → Text Extraction → Transaction Parsing → Modular Services → PostgreSQL → Financial Reports
```

### **Application Modes:**
1. **API Server** (Recommended) - RESTful API with CORS support
2. **Excel Generator** - Professional financial statements 
3. **Console App** - Interactive terminal interface

## 🚀 Quick Start

### **Prerequisites**
- Java 17+
- PostgreSQL 12+ (configured and running)
- Environment variables set (see `.env`)

### **1. API Server Mode (Recommended)**
```bash
cd /Users/sthwalonyoni/FIN
source .env
./gradlew run --args="api"
```
**Endpoints:**
- Health: `http://localhost:8080/api/v1/health`
- Companies: `http://localhost:8080/api/v1/companies`
- Documentation: `http://localhost:8080/api/v1/docs`

### **2. Excel Financial Reports**
```bash
cd /Users/sthwalonyoni/FIN
source .env
# Set mainClass to ComprehensiveExcelGenerator in build.gradle.kts
./gradlew run -x test
# Output: xinghizana_financial_report_YYYY-MM-DD.xls
```

### **3. Console Application**
```bash
cd /Users/sthwalonyoni/FIN
source .env
# Set mainClass to fin.App in build.gradle.kts  
./gradlew run
```

## 📊 Current Data Status

### **Live Company Data: Xinghizana Group (FY2024-2025)**
- **45 accounts** across all categories (Assets, Liabilities, Equity, Revenue, Expenses)
- **7,156 journal entry lines** with real transaction data
- **Major balances:**
  - Bank Current Account: R9,792,128.08 (credits)
  - Employee Costs: R3,817,301.12
  - Computer Expenses: R573,653.01
  - Insurance: R224,324.17

## 📈 Financial Reports Available

### **✅ Console Reports (Full Implementation)**
- Cashbook Report (detailed transactions)
- General Ledger (account-grouped transactions)
- Trial Balance (debit/credit verification)
- Income Statement (revenue vs expenses)

### **✅ Excel Reports (Professional Format)**
- **Balance Sheet** - Assets, Liabilities, Equity with real amounts
- **Income Statement** - Revenue and Expenses with calculations
- **Trial Balance** - All accounts with debit/credit totals
- **15 professional sheets** including Cover, Index, Company Details

### **🔄 In Development**
- General Ledger details in Excel
- Journal Entries listing in Excel
- Bank Reconciliation module

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
- PostgreSQL database with real financial data
- PDF processing pipeline working
- API server fully functional  
- Excel reporting with actual amounts
- Comprehensive database schema
- TransactionMappingService (fully implemented)
- AccountManager service (fully implemented)

### **🔄 In Development - Modular Services**
- JournalEntryGenerator service (test-driven development in progress)
- RuleMappingService (test-driven development in progress)
- TransactionBatchProcessor (test-driven development in progress)

### **📊 Live Financial Data**
The system currently contains **real financial data** for Xinghizana Group with proper account structures, journal entries, and calculated balances ready for reporting.

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
| GET | `/health` | System health check |
| GET | `/companies` | List all companies |
| POST | `/companies` | Create new company |
| GET | `/companies/{id}/fiscal-periods` | Get fiscal periods |
| POST | `/companies/{id}/upload` | Process bank statement |

## ⚖️ License

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

**Status:** Active development | **Database:** PostgreSQL | **Data:** Production-ready | **Reports:** Excel + Console

## Documentation

Detailed documentation is available in the `docs` directory:

- [Project Overview](docs/README.md)
- [Usage Guide](docs/USAGE.md)
- [Development Guide](docs/DEVELOPMENT.md)
- [Changelog](docs/CHANGELOG.md)
- [System Architecture](docs/system_architecture/README.md)
  - [System Design](docs/system_architecture/SYSTEM_ARCHITECTURE.md)
  - [Implementation Strategy](docs/system_architecture/IMPLEMENTATION_STRATEGY.md)
  - [Technical Specifications](docs/system_architecture/TECHNICAL_SPECIFICATIONS.md)
  - [Integration Points](docs/system_architecture/INTEGRATION_POINTS.md)

## Building

```bash
# Build the project
./gradlew build

# Create a fat JAR with all dependencies
./gradlew fatJar

# Create a distributable package
./gradlew distZip
```

## Requirements

- Java 17 or later
- SQLite JDBC driver (automatically included in build)

## License

**FIN Financial Management System uses a dual licensing model:**

### 🆓 **Personal Use - Apache License 2.0**
- ✅ **FREE** for personal finance management
- ✅ **FREE** for educational use and research  
- ✅ **FREE** for open source development
- ✅ View source code and contribute improvements

### 💼 **Commercial Use - Commercial License Required**
- 💰 **PAID** for business financial management
- 💰 **PAID** for revenue-generating activities
- 💰 **PAID** for hosting services to others
- 💰 **PAID** for integration into commercial products

**📋 See [COMMERCIAL_LICENSE.md](COMMERCIAL_LICENSE.md) for pricing and terms.**

---

### **License Details:**

**Source Code License:** Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

**Application Use License:** Commercial license required for business use.

**Copyright:** 2024-2025 Sthwalo Holdings (Pty) Ltd.
**Owner:** Immaculate Nyoni
**Contact:** sthwaloe@gmail.com | +27 61 514 6185

```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

**⚠️ Important:** The Apache 2.0 license covers source code access and modification rights only. Commercial use of the application requires a separate commercial license.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For support, please open an issue in the GitHub repository.
