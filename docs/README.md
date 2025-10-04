# FIN Documentation

This directory contains comprehensive documentation for the FIN Financial Management System.

## � Quick Links

- **[Development README](development/README.md)** - Start here for development setup and recent progress
- **[QUICK_TEST_GUIDE.md](QUICK_TEST_GUIDE.md)** - 5-10 minute testing guide (START HERE!)
- **[CHANGELOG.md](CHANGELOG.md)** - Version history and recent changes
- **[SYSTEM_ARCHITECTURE_STATUS.md](SYSTEM_ARCHITECTURE_STATUS.md)** - Current system architecture

## 📂 Documentation Structure

### 🔧 Development Documentation ([development/](development/))
All development-related documents, progress reports, and analysis:
- **[README.md](development/README.md)** - Main development index (START HERE!)
- **[QUICK_START.md](development/QUICK_START.md)** - Quick commands and setup
- **[DEVELOPMENT_STATUS.md](development/DEVELOPMENT_STATUS.md)** - Current system state
- **[PROGRESS_REPORT_OCT_2_4_2025.md](development/PROGRESS_REPORT_OCT_2_4_2025.md)** - Latest 3-day sprint
- **Session Summaries** - Detailed daily development logs
- **Phase Reports** - Refactoring phase completion reports (Phase 2-7)
- **Analysis Documents** - Deep technical analysis and architecture reviews
- **Refactoring Reports** - Code cleanup and improvement reports

### 📚 Reference Guides (Root Level)
Core reference documentation:
- **[DATABASE_REFERENCE.md](DATABASE_REFERENCE.md)** - Database schema and operations
- **[SYSTEM_ARCHITECTURE_STATUS.md](SYSTEM_ARCHITECTURE_STATUS.md)** - System architecture overview
- **[INTERACTIVE_CLASSIFICATION_GUIDE.md](INTERACTIVE_CLASSIFICATION_GUIDE.md)** - Transaction classification
- **[TRANSACTION_CLASSIFICATION_GUIDE.md](TRANSACTION_CLASSIFICATION_GUIDE.md)** - Classification rules
- **[PAYROLL_INTEGRATION_GUIDE.md](PAYROLL_INTEGRATION_GUIDE.md)** - Payroll system integration
- **[USAGE.md](USAGE.md)** - Application usage instructions

### 🏗️ Strategy & Planning Documents (Root Level)
Strategic documents and deployment guides:
- **[PRODUCTION_DEPLOYMENT_STRATEGY.md](PRODUCTION_DEPLOYMENT_STRATEGY.md)** - Production deployment
- **[IP_PROTECTION_STRATEGY.md](IP_PROTECTION_STRATEGY.md)** - Intellectual property protection
- **[TYPESCRIPT_INTEGRATION_STRATEGY.md](TYPESCRIPT_INTEGRATION_STRATEGY.md)** - TypeScript integration
- **[POSTGRESQL_MIGRATION_GUIDE.md](POSTGRESQL_MIGRATION_GUIDE.md)** - PostgreSQL migration
- **[MIGRATION_STRATEGY.md](MIGRATION_STRATEGY.md)** - Data migration strategies
- **[SIMULTANEOUS_DEVELOPMENT_GUIDE.md](SIMULTANEOUS_DEVELOPMENT_GUIDE.md)** - Multi-developer workflow

### 📊 Reports & Incidents (Root Level)
Verification reports and incident analysis:
- **[EXPORT_VERIFICATION_REPORT.md](EXPORT_VERIFICATION_REPORT.md)** - Export functionality verification
- **[INCIDENT_REPORT_2025-09-28.md](INCIDENT_REPORT_2025-09-28.md)** - Critical incident analysis

## 🎯 Getting Started

### For New Developers
1. Read **[development/README.md](development/README.md)** - Main development index
2. Follow **[development/QUICK_START.md](development/QUICK_START.md)** - Setup instructions
3. Run **[QUICK_TEST_GUIDE.md](QUICK_TEST_GUIDE.md)** - Verify your setup (5-10 mins)
4. Review **[development/DEVELOPMENT_STATUS.md](development/DEVELOPMENT_STATUS.md)** - Current system state

### For Understanding Recent Changes
1. Check **[CHANGELOG.md](CHANGELOG.md)** - Version history
2. Read **[development/PROGRESS_REPORT_OCT_2_4_2025.md](development/PROGRESS_REPORT_OCT_2_4_2025.md)** - Latest sprint
3. Review **[development/SESSION_SUMMARY_2025-10-04.md](development/SESSION_SUMMARY_2025-10-04.md)** - Latest session

### For System Architecture
1. See **[SYSTEM_ARCHITECTURE_STATUS.md](SYSTEM_ARCHITECTURE_STATUS.md)** - High-level overview
2. See **[DATABASE_REFERENCE.md](DATABASE_REFERENCE.md)** - Database schema
3. See **[development/FULLSTACK_DEVELOPMENT.md](development/FULLSTACK_DEVELOPMENT.md)** - Full-stack guide

## Project Structure

```
FIN/
├── app/                    # Main application module
│   ├── src/
│   │   ├── main/          # Application source code
│   │   └── test/          # Test source code
│   └── build.gradle.kts   # Module build configuration
├── docs/                  # Documentation (organized by category)
│   ├── business/          # Business and legal documents
│   ├── development/       # Development guides and reports
│   ├── technical/         # Technical reference documentation
│   └── system_architecture/ # System design and architecture
├── scripts/               # Shell scripts and utilities
├── gradle/                # Gradle wrapper files
├── gradlew                # Gradle wrapper script (Unix)
├── gradlew.bat            # Gradle wrapper script (Windows)
└── settings.gradle.kts    # Project settings
```

## Technology Stack

- **Language**: Java 17
- **Build Tool**: Gradle 8.8
- **Database**: PostgreSQL
- **Testing Framework**: JUnit Jupiter
- **PDF Processing**: Apache PDFBox
- **Export**: iText PDF, CSV

## Change History

See [CHANGELOG.md](CHANGELOG.md) for a history of changes to the application.
