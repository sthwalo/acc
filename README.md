# FIN Application

An interactive Java financial reporting system built with Gradle.

## Overview

FIN is a comprehensive financial reporting application that helps businesses manage their financial data. The system provides tools for company setup, fiscal period management, CSV transaction data import, and financial report generation including cashbook, general ledger, trial balance, income statement, balance sheet, and cash flow statement.

## Features

- Company management (create, select, and view companies)
- Fiscal period management (create, select, and view fiscal periods)
- CSV transaction data import with automatic fiscal period mapping
- Financial report generation:
  - Cashbook Report
  - General Ledger Report
  - Trial Balance Report
  - Income Statement
  - Balance Sheet
  - Cash Flow Statement
- Interactive console menu interface
- SQLite database for data persistence

## Quick Start

Run the application:

```bash
# From source
./gradlew run

# With arguments
./gradlew run --args="Your Name"

# Using the fat JAR (includes all dependencies)
./gradlew fatJar
java -jar app/build/libs/app-fat.jar

# Important: When using the application interactively, enter your choices directly in the terminal
# Do not type commands in a new terminal prompt
```

## Using the Application

1. **Company Setup**: First, create a company or select an existing one
2. **Fiscal Period Management**: Create fiscal periods for your company
3. **Import CSV Data**: Import bank transaction data from CSV files
4. **Generate Reports**: Create financial reports filtered by fiscal period

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

[Add your license information here]
