# FIN Development Documentation Hub

## Overview
This directory contains all development-related documentation for the FIN Financial Management System. Use this guide to navigate development resources, setup instructions, and ongoing project documentation.

## 🚀 Quick Start (New Developers)

### 1. Environment Setup
1. **[Java Version Management](JAVA_VERSION_MANAGEMENT.md)** - Java 17 setup and troubleshooting
2. **[Quick Start Guide](QUICK_START.md)** - Commands and daily workflow
3. **[Fullstack Development](FULLSTACK_DEVELOPMENT.md)** - Complete development setup

### 2. Development Workflow
```bash
# Verify Java 17 is active
java --version
# Expected: java 17.0.12 2024-07-16 LTS

# Build and test
./gradlew clean build

# Run application modes
java -jar app/build/libs/fin-spring.jar        # Console application
java -jar app/build/libs/fin-spring.jar api    # REST API server
java -jar app/build/libs/fin-spring.jar --batch [cmd]  # Batch processing
```

### 3. Essential Resources
- **[Refactoring Summary](REFACTORING_SUMMARY.md)** - Phases 2-7 refactoring journey
- **[Task Documentation](tasks/README.md)** - Active development tasks
- **[Code Quality Guidelines](../CODE_QUALITY_GUIDE.md)** - Standards and remediation

## 📚 Development Guides

### Environment & Setup
| Guide | Purpose | Read Time |
|-------|---------|-----------|
| **[Java Version Management](JAVA_VERSION_MANAGEMENT.md)** | Java 17 migration and troubleshooting | 10 min |
| **[Quick Start](QUICK_START.md)** | Essential commands and workflow | 5 min |
| **[Fullstack Development](FULLSTACK_DEVELOPMENT.md)** | Complete development environment | 15 min |

### Architecture & Patterns
| Guide | Purpose | Read Time |
|-------|---------|-----------|
| **[Refactoring Summary](REFACTORING_SUMMARY.md)** | Phases 2-7 refactoring journey (Sept-Oct 2025) | 20 min |
| **[Clean Code Implementation](ROBERT_C_MARTIN_CLEAN_CODE_IMPLEMENTATION.md)** | Robert C. Martin principles application | 15 min |
| **[Frontend Component Placement](FRONTEND_COMPONENT_PLACEMENT.md)** | UI component organization | 10 min |

### Active Development
| Resource | Purpose | Update Frequency |
|----------|---------|------------------|
| **[Task Documentation](tasks/)** | Active development tracking (30 TASK files) | Daily |
| **[Task README](tasks/README.md)** | Task organization and protocols | Weekly |

## 🔧 Current Development Status

### Environment Configuration
- **Java Runtime**: Java 17.0.12 LTS (Oracle Corporation)
- **Build System**: Gradle 8.4
- **Database**: PostgreSQL 12+
- **IDE**: VS Code with Java extensions

### Recent Achievements (November 2024)
- ✅ **Java Version Migration**: Successfully downgraded from Java 21 to Java 17
- ✅ **Build Verification**: `./gradlew clean build` passes with Java 17
- ✅ **Environment Persistence**: Java 17 configuration saved to `~/.zshrc`
- ✅ **Gradle Integration**: Confirmed Gradle 8.4 compatibility with Java 17

### Production Metrics
- **Transactions Processed**: 7,156+ bank transactions
- **Journal Entries**: 3,823 balanced entries
- **Account Structure**: 76 SARS-compliant accounts
- **Test Coverage**: 118 automated tests
- **Quality Status**: 32% SpotBugs remediation complete (7/22 critical issues fixed)

## 🛠️ Development Workflow

### Daily Development Routine
```bash
# 1. Start development session
cd /Users/sthwalonyoni/FIN
source .env  # Load database credentials

# 2. Verify environment
java --version  # Confirm Java 17
./gradlew --version  # Confirm Gradle integration

# 3. Pull latest changes
git pull origin main

# 4. Run tests
./gradlew test

# 5. Start development server (choose mode)
java -jar app/build/libs/fin-spring.jar        # Console mode for testing
java -jar app/build/libs/fin-spring.jar api    # API mode for frontend dev
```

### Code Quality Standards
- **Checkstyle**: Follow holistic cleanup approach (see [CODE_QUALITY_GUIDE.md](../CODE_QUALITY_GUIDE.md))
- **SpotBugs**: Address critical issues systematically
- **Testing**: All new features require tests
- **Documentation**: Update task files for significant changes

### Task Management
- All development work tracked in `tasks/TASK_*.md` files
- Use task documentation protocol for new initiatives
- Regular progress updates in task files

## 🏗️ Architecture Overview

### Core Components
- **Application Modes**: Console, API Server, Batch Processing
- **Dependency Injection**: `ApplicationContext` pattern
- **Database Access**: Repository pattern with connection pooling
- **Transaction Processing**: PDF parsing → Classification → Persistence pipeline

### Key Services
- `BankStatementProcessingService` - PDF processing orchestration
- `TransactionClassificationService` - Business rule application
- `PayrollService` - SDL compliance and calculations
- `FinancialReportingService` - Report generation

### Development Patterns
```java
// Service Registration (ApplicationContext)
SomeService someService = new SomeService(dbUrl, dependency);
register(SomeService.class, someService);

// Database Access (Repository Pattern)
public class SomeRepository {
    public SomeRepository(String dbUrl) { 
        // Use DatabaseConfig.getConnection() 
    }
}

// UI Patterns
// Console: Inject OutputFormatter/InputHandler
// API: Use ApiServer.setup* methods, return Gson-wrapped responses
```

## 📊 Quality Metrics & Progress

### Build Status
- **Java Compatibility**: ✅ Java 17.0.12 LTS
- **Build Success**: ✅ `./gradlew clean build` passing
- **Test Suite**: ✅ 118 tests passing
- **Checkstyle**: 🔄 2,113 violations (systematic cleanup in progress)
- **SpotBugs**: 🔄 32% remediation complete (7/22 critical issues)

### Code Quality Trends
- **File Structure**: Organized by domain (service, model, controller)
- **Documentation**: 68 active docs + 43 archived
- **Task Tracking**: 30 active TASK files
- **Refactoring**: Phases 2-7 completed (Sept-Oct 2025)

## 🔗 Related Documentation

### Core System Documentation
- **[Main Documentation Hub](../README.md)** - Complete documentation index
- **[System Architecture](../SYSTEM_ARCHITECTURE_STATUS.md)** - High-level architecture
- **[Database Reference](../DATABASE_REFERENCE.md)** - Schema and operations

### User & Business Documentation
- **[Usage Guide](../USAGE.md)** - Application user guide
- **[Business Documentation](../business/)** - Licensing and commercial info
- **[Technical Documentation](../technical/)** - Production deployment guides

### Testing & Quality
- **[Quick Test Guide](../QUICK_TEST_GUIDE.md)** - 5-10 minute verification
- **[Code Quality Guide](../CODE_QUALITY_GUIDE.md)** - Standards and remediation
- **[Code Quality Status](../CODE_QUALITY_STATUS.md)** - Current metrics

## 🤝 Development Protocols

### Contributing Guidelines
1. **Environment Setup**: Follow [Java Version Management](JAVA_VERSION_MANAGEMENT.md)
2. **Code Standards**: Adhere to [Code Quality Guide](../CODE_QUALITY_GUIDE.md)
3. **Task Documentation**: Create TASK files for new initiatives
4. **Testing**: Ensure all tests pass before committing
5. **Documentation**: Update relevant documentation for changes

### Code Review Process
- All changes must pass `./gradlew clean build`
- Follow checkstyle and SpotBugs remediation patterns
- Update task documentation for significant changes
- Maintain backward compatibility with Java 17

### Issue Resolution
- **Environment Issues**: Check [Java Version Management](JAVA_VERSION_MANAGEMENT.md)
- **Build Issues**: Verify Java 17 and Gradle compatibility
- **Database Issues**: Reference [Database Documentation](../DATABASE_REFERENCE.md)
- **Quality Issues**: Follow [Code Quality Guide](../CODE_QUALITY_GUIDE.md)

## 📞 Support & Resources

### Internal Resources
- **Documentation**: 68 organized documentation files
- **Task System**: 30 active task tracking files
- **Quality Tools**: Checkstyle, SpotBugs, JUnit configurations

### External Resources
- **Java 17 Documentation**: [Oracle Java 17 Docs](https://docs.oracle.com/en/java/javase/17/)
- **Gradle Documentation**: [Gradle 8.4 Docs](https://docs.gradle.org/8.4/)
- **PostgreSQL Documentation**: [PostgreSQL Docs](https://www.postgresql.org/docs/)

---

**Development Hub Version**: 1.0  
**Last Updated**: November 4, 2024  
**Next Review**: December 1, 2024  
**Status**: ✅ Complete and Active