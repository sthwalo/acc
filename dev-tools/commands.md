# FIN Application Commands Reference

## 🚀 Application Execution Commands

### **Modular Architecture Entry Points**

#### **Console Application (Default)**
```bash
# Run console application (default mode)
./gradlew run

# Run console application explicitly
./gradlew runConsole

# Run console with specific args
./gradlew run --args="console"

# Run console app directly
cd /Users/sthwalonyoni/FIN && ./gradlew run
```

#### **API Server**
```bash
# Start API server (modular architecture)
./gradlew runApi

# Start API server with args
./gradlew run --args="api"

# Start API server with logs
./gradlew run --args="api" 2>&1 | tee api_server.log

# Start API server (background)
./gradlew run --args="api" &

# Test API server is running
curl http://localhost:8080/api/health

# Stop API server
pkill -f "java.*ApiApplication"
```

#### **Application Context Commands**
```bash
# View application context services
./gradlew run --args="--context-info"

# Test dependency injection
./gradlew run --args="--di-test"

# Validate service initialization
./gradlew run --args="--validate-services"
```

## 🏗️ Modular Architecture Commands

### **Dependency Injection & Context**
```bash
# Test application context initialization
./gradlew test --tests "fin.context.ApplicationContextTest"

# Validate service registration
./gradlew run --args="--services-validate"

# Check dependency injection health
./gradlew run --args="--di-health"

# View registered services
./gradlew run --args="--services-list"
```

### **Entry Point Management**
```bash
# Switch between application modes
./gradlew runApi      # API server mode
./gradlew runConsole  # Console application mode

# Run both applications simultaneously
./gradlew runApi & ./gradlew runConsole &

# Graceful shutdown of both
pkill -f "java.*ApiApplication" && pkill -f "java.*ConsoleApplication"

# Check which mode is running
ps aux | grep -E "(ApiApplication|ConsoleApplication)" | grep -v grep
```

### **Modular Testing**
```bash
# Test architectural layers separately
./gradlew test --tests "fin.context.*"     # Context/Injection layer
./gradlew test --tests "fin.api.*"         # API layer
./gradlew test --tests "fin.controller.*"  # Controller layer
./gradlew test --tests "fin.service.*"     # Service layer
./gradlew test --tests "fin.repository.*"  # Repository layer
./gradlew test --tests "fin.ui.*"          # UI layer

# Integration tests for modular components
./gradlew test --tests "*IntegrationTest"
./gradlew test --tests "*ModularTest"
```

### **Excel Report Generation**
```bash
# Generate Excel reports directly
./gradlew run --args="excel"

# Generate with custom output
./gradlew run --args="excel reports/custom_report.xls"
```

### **CI/CD and Deployment**
```bash
# Full CI pipeline simulation (local)
./gradlew clean build test

# Build JAR for deployment
./gradlew build -x test
ls -la app/build/libs/

# Build fat JAR with all dependencies
./gradlew fatJar
ls -la app/build/libs/*-fat.jar

# Run smoke tests after deployment
curl http://localhost:8080/api/health
./gradlew runConsole &
sleep 5 && pkill -f "gradle"

# Test both application modes
./gradlew runApi &
sleep 3 && curl http://localhost:8080/api/health && pkill -f "ApiApplication"
./gradlew runConsole &
sleep 3 && pkill -f "ConsoleApplication"

# Database migration for deployment
PGPASSWORD='your_password' psql -h localhost -U drimacc_user -d drimacc_db -f scripts/migrate.sql

# Backup before deployment
pg_dump -h localhost -U drimacc_user -d drimacc_db > backup_$(date +%Y%m%d_%H%M%S).sql

# Health check script for both modes
watch -n 30 'curl -s http://localhost:8080/api/health && echo " ✓ API OK" || echo " ✗ API DOWN"'
watch -n 30 'ps aux | grep -q ConsoleApplication && echo " ✓ Console OK" || echo " ✗ Console DOWN"'
```

## 🗄️ Database Commands

### **PostgreSQL Connection**
```bash
# Connect to database
psql -h localhost -p 5432 -U drimacc_user -d drimacc_db

# Connect with password prompt
PGPASSWORD='your_password' psql -h localhost -p 5432 -U drimacc_user -d drimacc_db

# Test database connection
pg_isready -h localhost -p 5432 -d drimacc_db
```

### **Database Queries**
```sql
-- View all companies
SELECT * FROM companies;

-- View fiscal periods
SELECT * FROM fiscal_periods;

-- View accounts summary
SELECT account_code, account_name, account_type, balance 
FROM accounts 
ORDER BY account_code;

-- View journal entries (recent first)
SELECT je.*, a.account_name 
FROM journal_entries je 
JOIN accounts a ON je.account_id = a.id 
ORDER BY je.transaction_date DESC 
LIMIT 50;

-- Financial summary by account type
SELECT account_type, SUM(balance) as total_balance 
FROM accounts 
GROUP BY account_type;

-- Monthly transaction summary
SELECT 
    DATE_TRUNC('month', transaction_date) as month,
    COUNT(*) as transaction_count,
    SUM(debit_amount) as total_debits,
    SUM(credit_amount) as total_credits
FROM journal_entries 
GROUP BY DATE_TRUNC('month', transaction_date)
ORDER BY month;

-- Account balances
SELECT 
    a.account_code,
    a.account_name,
    a.account_type,
    COALESCE(SUM(je.debit_amount), 0) - COALESCE(SUM(je.credit_amount), 0) as calculated_balance,
    a.balance as stored_balance
FROM accounts a
LEFT JOIN journal_entries je ON a.id = je.account_id
GROUP BY a.id, a.account_code, a.account_name, a.account_type, a.balance
ORDER BY a.account_code;

-- Search transactions by description
SELECT * FROM journal_entries 
WHERE description ILIKE '%keyword%' 
ORDER BY transaction_date DESC;
```

### **Database Schema Information**
```sql
-- List all tables
\dt

-- Describe table structure
\d accounts
\d journal_entries
\d companies
\d fiscal_periods

-- View table sizes
SELECT 
    schemaname,
    tablename,
    attname,
    n_distinct,
    correlation
FROM pg_stats
WHERE schemaname = 'public';

-- Get row counts
SELECT 
    'accounts' as table_name, COUNT(*) as row_count FROM accounts
UNION ALL
SELECT 
    'journal_entries' as table_name, COUNT(*) as row_count FROM journal_entries
UNION ALL
SELECT 
    'companies' as table_name, COUNT(*) as row_count FROM companies
UNION ALL
SELECT 
    'fiscal_periods' as table_name, COUNT(*) as row_count FROM fiscal_periods;
```

## 🔍 File Search Commands

### **Find Files by Type**
```bash
# Find all Java files
find . -name "*.java" -type f

# Find all test files
find . -path "*/test/*" -name "*.java"

# Find configuration files
find . -name "*.properties" -o -name "*.yml" -o -name "*.yaml"

# Find documentation files
find . -name "*.md" -type f

# Find Excel files
find . -name "*.xls" -o -name "*.xlsx"

# Find SQL files
find . -name "*.sql" -type f

# Find large files (>1MB)
find . -size +1M -type f

# Find recently modified files (last 7 days)
find . -mtime -7 -type f
```

### **Find Files by Content**
```bash
# Find files containing specific text
grep -r "PostgreSQL" --include="*.java" .
grep -r "DatabaseConfig" --include="*.java" .
grep -r "drimacc_db" .

# Find files with database connections
grep -r "jdbc:" --include="*.java" .

# Find Excel-related code
grep -r "ExcelGenerator" --include="*.java" .

# Find API endpoints
grep -r "@GetMapping\|@PostMapping" --include="*.java" .

# Find configuration references
grep -r "application\.properties\|application\.yml" .

# Find test classes and methods
grep -r "class.*Test" --include="*.java" .
grep -r "@Test" --include="*.java" .

# Find main methods
grep -r "public static void main" --include="*.java" .

# Find service classes
grep -r "Service" --include="*.java" app/src/main/java/fin/service/

# Find controller classes
grep -r "Controller" --include="*.java" app/src/main/java/fin/controller/

# Find UI classes
grep -r "ConsoleMenu\|InputHandler\|OutputFormatter" --include="*.java" .

# Find error handling
grep -r "try\|catch\|Exception" --include="*.java" .

# Find TODO comments
grep -r "TODO\|FIXME\|XXX" --include="*.java" .
```

### **Grep Commands for Code Analysis**
```bash
# Find all main methods
grep -r "public static void main" --include="*.java" .

# Find all class definitions
grep -r "^public class\|^class" --include="*.java" .

# Find all service classes
grep -r "Service" --include="*.java" app/src/main/java/fin/service/

# Find database-related imports
grep -r "import.*sql\|import.*database" --include="*.java" .

# Find error handling
grep -r "try\|catch\|throw" --include="*.java" .

# Find logging statements
grep -r "System\.out\.println\|log\." --include="*.java" .

# Find TODO comments
grep -r "TODO\|FIXME\|XXX" --include="*.java" .
```

## 📊 Project Analysis Commands

### **Code Statistics**
```bash
# Count lines of code
find . -name "*.java" -exec wc -l {} + | tail -1

# Count files by type
echo "Java files: $(find . -name "*.java" | wc -l)"
echo "Test files: $(find . -path "*/test/*" -name "*.java" | wc -l)"
echo "SQL files: $(find . -name "*.sql" | wc -l)"

# List all packages
find app/src/main/java -type d | sed 's|app/src/main/java/||' | grep -v "^$"
```

### **Build and Test Commands**
```bash
# Clean build
./gradlew clean build

# Build without tests
./gradlew build -x test

# Build fat JAR
./gradlew fatJar

# Run all tests
./gradlew test

# Run modular architecture tests
./gradlew test --tests "fin.context.*Test"       # Application context tests
./gradlew test --tests "fin.api.*Test"          # API server tests

# Run specific test class (updated for modular structure)
./gradlew test --tests "*BankStatementProcessingServiceTest*"
./gradlew test --tests "fin.ui.ConsoleMenuTest"
./gradlew test --tests "fin.controller.ApplicationControllerTest"
./gradlew test --tests "fin.service.*ServiceTest"

# Run specific test method
./gradlew test --tests "fin.ui.ConsoleMenuTest.displayMainMenu_PrintsAllOptions"
./gradlew test --tests "fin.controller.ApplicationControllerTest.start_WithExitChoice_ExitsCleanly"

# Run tests by architectural layer
./gradlew test --tests "*UI*" --quiet           # UI layer tests
./gradlew test --tests "fin.controller.*Test"   # Controller layer tests
./gradlew test --tests "fin.service.*Test"      # Service layer tests
./gradlew test --tests "fin.repository.*Test"   # Repository layer tests
./gradlew test --tests "fin.context.*Test"      # Context/Injection tests

# Run tests with detailed output
./gradlew test --tests "*UI*" --info | head -50

# Generate test report
./gradlew test && open app/build/reports/tests/test/index.html

# Check dependencies
./gradlew dependencies

# View project structure
./gradlew projects

# Run separate application tasks
./gradlew runApi     # Run API server only
./gradlew runConsole # Run console application only
```

## 🛠️ Development Commands

### **Git Operations**
```bash
# Quick status
git status --porcelain

# View recent commits
git log --oneline -10

# Show file changes
git diff --name-only

# Stage specific file types
git add "*.java"
git add "*.md"

# Commit with message
git commit -m "Your commit message"

# Push to origin
git push origin main
```

### **File Operations**
```bash
# View file with line numbers
cat -n filename.java

# View first/last lines
head -20 filename.java
tail -20 filename.java

# Search in specific file
grep -n "search_term" filename.java

# Count lines in file
wc -l filename.java

# Check file size
ls -lh filename.java

# Compare files
diff file1.java file2.java
```

### **Directory Operations**
```bash
# Show directory structure
tree -I 'build|.git|.gradle'

# List files by size
ls -lah | sort -k5 -hr

# Show disk usage
du -sh */

# Find empty directories
find . -type d -empty

# Create directory structure
mkdir -p path/to/new/directory
```

## 🔧 System Monitoring

### **Application Monitoring**
```bash
# Check Java processes (modular architecture)
jps -l | grep -E "(ApiApplication|ConsoleApplication)"

# Monitor Java heap usage for both applications
jstat -gc [API_PID]     # API server memory
jstat -gc [CONSOLE_PID] # Console app memory

# View application logs
tail -f api_server.log
tail -f console_app.log

# Monitor file changes
watch -n 2 'ls -lat reports/'

# Check port usage
lsof -i :8080
netstat -an | grep 8080

# Monitor both applications
watch -n 10 'ps aux | grep -E "(ApiApplication|ConsoleApplication)" | grep -v grep || echo "No FIN apps running"'
```

### **Modular Architecture Monitoring**
```bash
# Check application context status
curl http://localhost:8080/api/health

# Monitor dependency injection
./gradlew run --args="--di-status"

# Check service initialization
./gradlew run --args="--services-status"

# View active application contexts
jps -l | grep Application | xargs -I {} jstack {} | grep -A 5 -B 5 "ApplicationContext"
```

### **Database Monitoring**
```bash
# Check PostgreSQL status
pg_ctl status

# View active connections
psql -c "SELECT * FROM pg_stat_activity WHERE datname = 'drimacc_db';"

# Database size
psql -c "SELECT pg_size_pretty(pg_database_size('drimacc_db'));"

# Table sizes
psql -c "SELECT schemaname,tablename,pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size FROM pg_tables WHERE schemaname = 'public' ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;"
```

### **Debugging and Troubleshooting**
```bash
# View test failure details
./gradlew test --tests "*TestName*" --info

# Run tests with stack traces
./gradlew test --tests "*TestName*" --stacktrace

# Debug test execution
./gradlew test --tests "*TestName*" --debug-jvm

# View build logs
./gradlew build --info | tee build.log

# Check for compilation errors
./gradlew compileJava --info

# View dependency tree
./gradlew dependencies --configuration runtimeClasspath

# Find hanging processes
ps aux | grep java
pkill -f "gradle"

# Clear Gradle cache
./gradlew clean && rm -rf ~/.gradle/caches

# Check disk space
df -h
du -sh app/build/
```

## 🎯 Quick Commands

### **One-Liners for Common Tasks**
```bash
# Full system test (modular architecture)
./gradlew clean build -x test && ./gradlew runConsole

# Test both application modes
./gradlew runApi &
sleep 3 && curl http://localhost:8080/api/health && pkill -f "ApiApplication"
./gradlew runConsole &
sleep 3 && pkill -f "ConsoleApplication"

# Run console app with auto-confirm license
echo "yes" | timeout 10s ./gradlew runConsole || echo "Process completed or timed out"

# Run API server with auto-confirm license
echo "yes" | ./gradlew runApi

# Run with specific inputs for testing
echo -e "yes\n1\n" | ./gradlew runConsole

# Direct JAR execution (modular)
java -jar app/build/libs/app.jar  # Runs ConsoleApplication by default

# Run specific application modes
./gradlew runConsole    # Console mode
./gradlew runApi        # API mode

# Run API server and monitor logs
./gradlew runApi 2>&1 | tee api_server.log &

# Run console and monitor logs
./gradlew runConsole 2>&1 | tee console_app.log &

# Run database migration
./gradlew run --args="migrate" 2>&1 | tee db_migration.log &

# Run database seeder
./gradlew run --args="seed" 2>&1 | tee db_seeder.log &

# Generate report and open
./gradlew run --args="excel" && open reports/*.xls

# Search for modular architecture components
grep -r "ApplicationContext\|ApiApplication\|ConsoleApplication" --include="*.java" app/src/

# Find configuration files
find . -name "*.properties" -o -name "*.yml" -o -name "application.*"

# Quick database query
PGPASSWORD='your_password' psql -h localhost -U drimacc_user -d drimacc_db -c "SELECT COUNT(*) FROM journal_entries;"

# Monitor both application modes
watch -n 5 'curl -s http://localhost:8080/api/health || echo "API Down"'
watch -n 5 'ps aux | grep -E "(ApiApplication|ConsoleApplication)" | grep -v grep || echo "Apps Down"'

# View recent git changes
git log --oneline --since="1 day ago"

# Clean and rebuild everything
./gradlew clean && rm -rf build && ./gradlew build

# Quick test execution patterns (modular)
./gradlew test --tests "*UI*" --quiet
./gradlew test --tests "fin.controller.*Test"
./gradlew test --tests "fin.service.*Test"
./gradlew test --tests "fin.context.*Test"  # New: Context tests

# Debug failing tests
./gradlew test --tests "*TestName*" --info | head -50
./gradlew test --tests "*TestName*" --stacktrace
```

## 📝 Notes

- Replace `your_password` with actual database password
- Adjust file paths as needed for your system
- Some commands may require additional setup or permissions
- Use `Ctrl+C` to stop background processes
- Always backup database before running destructive queries

## 🔗 Useful Aliases

Add these to your `.bashrc` or `.zshrc`:

```bash
alias finrun='cd /Users/sthwalonyoni/FIN && ./gradlew run'
alias finapi='cd /Users/sthwalonyoni/FIN && ./gradlew runApi'
alias finconsole='cd /Users/sthwalonyoni/FIN && ./gradlew runConsole'
alias finbuild='cd /Users/sthwalonyoni/FIN && ./gradlew clean build'
alias finstatus='cd /Users/sthwalonyoni/FIN && git status'
alias fintest='cd /Users/sthwalonyoni/FIN && ./gradlew test'
alias finuitest='cd /Users/sthwalonyoni/FIN && ./gradlew test --tests "*UI*" --quiet'
alias fincontexttest='cd /Users/sthwalonyoni/FIN && ./gradlew test --tests "fin.context.*Test"'
alias fincleantest='cd /Users/sthwalonyoni/FIN && ./gradlew clean test'
alias finhealth='curl -s http://localhost:8080/api/health || echo "API Down"'
alias finlogs='tail -f /Users/sthwalonyoni/FIN/api_server.log'
alias finconsolelogs='tail -f /Users/sthwalonyoni/FIN/console_app.log'
alias finstop='pkill -f "java.*ApiApplication"'
alias finstopconsole='pkill -f "java.*ConsoleApplication"'
alias finstopall='pkill -f "java.*Application"'
alias findb='PGPASSWORD="your_password" psql -h localhost -U drimacc_user -d drimacc_db'
alias finfat='cd /Users/sthwalonyoni/FIN && ./gradlew fatJar'
alias finboth='cd /Users/sthwalonyoni/FIN && ./gradlew runApi & ./gradlew runConsole'
```
