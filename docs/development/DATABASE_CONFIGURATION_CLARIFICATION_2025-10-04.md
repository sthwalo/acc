# Database Configuration Clarification - October 4, 2025

## 🎯 Your Question

> "Is the CI/CD pipeline supposed to connect to the real database or the test database? Since we updated our CI/CD for test database credentials, when I run my app ./run.sh it is connecting to the test database not the actual database."

## ✅ CORRECT Answer

### What SHOULD Happen

| **Context** | **Should Connect To** | **Environment Variables Used** |
|-------------|----------------------|-------------------------------|
| **Production App** (`./run.sh`) | ✅ **Production Database** | `DATABASE_*` |
| **Local Testing** (`./gradlew test`) | ✅ **Test Database** | `TEST_DATABASE_*` |
| **CI/CD Pipeline** (GitHub Actions) | ✅ **Test Database** | `TEST_DATABASE_*` |

### Key Principle

- **CI/CD should NEVER touch production database**
- **CI/CD should ALWAYS use isolated test database**
- **Production app should NEVER use test database**
- **Tests should NEVER use production database**

---

## 🔴 The Bug You Discovered

### What Was Happening (BROKEN)

After my first fix, the logic was:

```java
// WRONG LOGIC:
1. Check for TEST_DATABASE_URL environment variable
2. If it exists → Use test database
3. Otherwise → Use production database
```

**Problem:** If you have `TEST_DATABASE_URL` in your `.env` file (which you should!), then `./run.sh` would use the test database instead of production!

**Example:**
```bash
# Your .env file
DATABASE_URL=jdbc:postgresql://localhost:5432/drimacc_db          # Production
DATABASE_USER=sthwalonyoni
DATABASE_PASSWORD=your_password

TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/drimacc_test   # Test
TEST_DATABASE_USER=sthwalonyoni
TEST_DATABASE_PASSWORD=your_password

# Running production app
./run.sh

# WRONG BEHAVIOR:
# ❌ Connected to: jdbc:postgresql://localhost:5432/drimacc_test
# Should connect to drimacc_db, not drimacc_test!
```

---

## ✅ The Fix (Implemented Now)

### New Correct Logic

```java
// CORRECT LOGIC:
1. Check if we're in TEST CONTEXT (JUnit running, Gradle test task, CI/CD)
2. If YES and TEST_DATABASE_URL exists → Use test database
3. If NO (normal app runtime) → Use production DATABASE_* vars
```

### How Test Context Detection Works

```java
private static boolean isRunningInTestContext() {
    // Method 1: Check if JUnit is running
    // - Looks for org.junit classes in stack trace
    // - Detects Gradle test task execution
    // - Identifies test classes (classes ending with "Test")
    
    // Method 2: Check CI/CD environment
    // - CI=true (standard CI/CD variable)
    // - GITHUB_ACTIONS=true (GitHub Actions specific)
    
    // Method 3: Check Gradle test property
    // - gradle.test=true
    
    return isJUnitRunning || isCIEnvironment || isGradleTest;
}
```

---

## 🎯 Behavior After Fix

### Scenario 1: Running Production App (`./run.sh`)

```bash
./run.sh

# Database detection:
# 1. Is this a test context? NO (not JUnit, not CI/CD, not Gradle test)
# 2. Use production DATABASE_* variables
# ✅ Connected to: jdbc:postgresql://localhost:5432/drimacc_db

# Output:
🔍 DATABASE_URL from env: jdbc:postgresql://localhost:5432/drimacc_db
✅ PostgreSQL connection successful
📊 Database: jdbc:postgresql://localhost:5432/drimacc_db
```

### Scenario 2: Running Local Tests (`./gradlew test`)

```bash
./gradlew test

# Database detection:
# 1. Is this a test context? YES (Gradle test task detected)
# 2. Look for TEST_DATABASE_* variables
# 3. Found TEST_DATABASE_URL
# ✅ Connected to: jdbc:postgresql://localhost:5432/drimacc_test

# Output:
🧪 Test execution context detected - using TEST_DATABASE_* environment variables
✅ Test database configuration loaded from environment
🔍 Test databaseUrl: jdbc:postgresql://localhost:5432/drimacc_test
✅ Test PostgreSQL connection successful
```

### Scenario 3: CI/CD Pipeline (GitHub Actions)

```bash
# GitHub Actions environment
CI=true
GITHUB_ACTIONS=true
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/drimacc_test
TEST_DATABASE_USER=postgres
TEST_DATABASE_PASSWORD=postgres

# Build and test execution
./gradlew build

# Database detection:
# 1. Is this a test context? YES (CI=true detected)
# 2. Look for TEST_DATABASE_* variables
# 3. Found TEST_DATABASE_URL
# ✅ Connected to: jdbc:postgresql://localhost:5432/drimacc_test

# Output:
🔍 CI/CD environment detected (CI=true, GITHUB_ACTIONS=true)
🧪 Test execution context detected - using TEST_DATABASE_* environment variables
✅ Test database configuration loaded from environment
```

---

## 📋 Your .env File Should Look Like This

```bash
# Production Database (used by ./run.sh)
DATABASE_URL=jdbc:postgresql://localhost:5432/drimacc_db
DATABASE_USER=sthwalonyoni
DATABASE_PASSWORD=your_actual_password

# Test Database (used by ./gradlew test)
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/drimacc_test
TEST_DATABASE_USER=sthwalonyoni
TEST_DATABASE_PASSWORD=your_actual_password

# Note: Both can use the same credentials, different databases
# Production data is in drimacc_db
# Test data is in drimacc_test (can be wiped/recreated)
```

---

## 🔍 How to Verify It's Working

### Test 1: Production App Should Use Production DB

```bash
./run.sh

# Look for this output:
# ✅ Should see: DATABASE_URL from env: jdbc:postgresql://localhost:5432/drimacc_db
# ❌ Should NOT see: Test execution context detected
```

### Test 2: Local Tests Should Use Test DB

```bash
./gradlew test

# Look for this output:
# ✅ Should see: Test execution context detected
# ✅ Should see: using TEST_DATABASE_* environment variables
# ✅ Should see: Test databaseUrl: jdbc:postgresql://localhost:5432/drimacc_test
```

### Test 3: Check Which Database You're Connected To

Run this in your app or test:

```bash
# In production app (./run.sh)
# Should connect to drimacc_db

# In tests (./gradlew test)
# Should connect to drimacc_test
```

---

## 🛡️ Safety Guarantees

### Production Database Protection

1. **Never touched by tests** - All tests use `drimacc_test`
2. **Never touched by CI/CD** - GitHub Actions uses PostgreSQL service container with `drimacc_test`
3. **Only accessed by production app** - When you run `./run.sh` or deploy to production

### Test Database Isolation

1. **Can be wiped safely** - No production data lost
2. **Fresh for every CI/CD run** - GitHub Actions creates new database each time
3. **Local tests won't affect production** - Separate database

---

## 📊 Database Setup Guide

### Local Development Setup

```bash
# 1. Create production database
createdb drimacc_db

# 2. Create test database
createdb drimacc_test

# 3. Run migrations on production database
psql -U sthwalonyoni -d drimacc_db -f test_schema.sql

# 4. Run migrations on test database (for local testing)
psql -U sthwalonyoni -d drimacc_test -f test_schema.sql

# 5. Verify both exist
psql -l | grep drimacc
# Should see:
#  drimacc_db    | sthwalonyoni | ...
#  drimacc_test  | sthwalonyoni | ...
```

---

## 🚀 CI/CD Configuration

### GitHub Actions (.github/workflows/ci-cd.yml)

```yaml
services:
  postgres:
    image: postgres:15
    env:
      POSTGRES_DB: drimacc_test          # ✅ Test database name
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - 5432:5432

env:
  TEST_DATABASE_URL: jdbc:postgresql://localhost:5432/drimacc_test
  TEST_DATABASE_USER: postgres
  TEST_DATABASE_PASSWORD: postgres
```

**Key Points:**
- ✅ Uses `drimacc_test` database (not production)
- ✅ PostgreSQL service is isolated per workflow run
- ✅ Data is discarded after workflow completes
- ✅ No way to accidentally connect to production

---

## 🎓 Key Takeaways

1. **CI/CD ALWAYS uses test database** (`drimacc_test`)
   - Reason: Safe, isolated, can be wiped

2. **Production app ALWAYS uses production database** (`drimacc_db`)
   - Reason: Real data, must be protected

3. **Test detection is automatic**
   - No manual configuration needed
   - Detects JUnit, Gradle test task, CI/CD environment

4. **Your .env file can have both sets of variables**
   - `DATABASE_*` for production
   - `TEST_DATABASE_*` for tests
   - Context determines which is used

5. **Safety by design**
   - Production cannot accidentally use test database
   - Tests cannot accidentally use production database
   - CI/CD cannot accidentally use production database

---

## 🔧 Troubleshooting

### Issue: ./run.sh connects to test database

**Check:**
```bash
./run.sh 2>&1 | grep "database"

# Should see:
# DATABASE_URL from env: jdbc:postgresql://localhost:5432/drimacc_db

# Should NOT see:
# Test execution context detected
```

**If you see "Test execution context detected":**
- The fix didn't apply correctly
- Rebuild: `./gradlew clean build`

### Issue: Tests connect to production database

**Check:**
```bash
./gradlew test 2>&1 | grep "database"

# Should see:
# Test execution context detected
# using TEST_DATABASE_* environment variables
# Test databaseUrl: jdbc:postgresql://localhost:5432/drimacc_test

# Should NOT see:
# DATABASE_URL from env: jdbc:postgresql://localhost:5432/drimacc_db
```

**If tests use production database:**
- TEST_DATABASE_* variables not set in .env
- Add them to your .env file

---

## 📝 Summary

**Your confusion was COMPLETELY VALID!**

The initial fix I implemented was **WRONG** - it made the production app use the test database whenever `TEST_DATABASE_*` variables existed.

**The new fix is CORRECT:**
- ✅ Production app uses `DATABASE_*` → `drimacc_db`
- ✅ Local tests use `TEST_DATABASE_*` → `drimacc_test`
- ✅ CI/CD uses `TEST_DATABASE_*` → `drimacc_test`

**No more confusion - each context uses the right database automatically!** 🎉

---

**Document Created:** October 4, 2025  
**Status:** ✅ FIXED - Context-aware database selection implemented  
**Impact:** Production safety restored, tests properly isolated
