# FIN Documentation

This directory contains comprehensive documentation for the FIN Financial Management System.

## Documentation Structure

### 📋 Business Documentation ([business/](business/))
Business strategy, licensing, and legal documents:
- [Business Model Options](business/BUSINESS_MODEL_OPTIONS.md) - Revenue and licensing strategies
- [Commercial License](business/COMMERCIAL_LICENSE.md) - Commercial licensing terms
- [Licensing Guide](business/LICENSING.md) - License usage and compliance
- [IP Protection Strategy](business/IP_PROTECTION_STRATEGY.md) - Intellectual property protection

### 🔧 Development Documentation ([development/](development/))
Development guides, progress reports, and setup instructions:
- [Clean Application Guide](development/CLEAN_APPLICATION_GUIDE.md) - Application cleanup procedures
- [Enhanced Parser Completion Report](development/ENHANCED_PARSER_COMPLETION_REPORT.md) - Parser development progress
- [Frontend Component Placement](development/FRONTEND_COMPONENT_PLACEMENT.md) - UI component organization
- [Fullstack Development Guide](development/FULLSTACK_DEVELOPMENT.md) - Full-stack development setup
- [Quick Start Guide](development/QUICK_START.md) - Getting started quickly

### ⚙️ Technical Documentation ([technical/](technical/))
Technical reference and implementation details:
- [Database Reference](technical/DATABASE_REFERENCE.md) - Database schema and operations

### 🏗️ System Architecture ([system_architecture/](system_architecture/))
Architectural design and system specifications:
- [System Architecture](system_architecture/SYSTEM_ARCHITECTURE.md) - Overall system design
- [Implementation Strategy](system_architecture/IMPLEMENTATION_STRATEGY.md) - Development approach
- [Integration Points](system_architecture/INTEGRATION_POINTS.md) - System integration details
- [Technical Specifications](system_architecture/TECHNICAL_SPECIFICATIONS.md) - Detailed technical specs


## Getting Started

For quick setup and usage instructions, see the [Quick Start Guide](development/QUICK_START.md).

For detailed development information, see [DEVELOPMENT.md](DEVELOPMENT.md).

For application usage instructions, see [USAGE.md](USAGE.md).

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
