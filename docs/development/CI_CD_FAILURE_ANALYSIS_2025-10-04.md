# CI/CD Failure Analysis - October 4, 2025

## 🔴 Problem Summary

The GitHub Actions CI/CD pipeline is failing due to **database configuration issues** in the test environment. The application's `DatabaseConfig` class requires environment variables to be set, but the way it initializes causes conflicts in CI/CD environments.

---

## 🔍 Root Causes Identified

### Issue 1: Static Initialization Order Problem

**File:** `app/src/main/java/fin/config/DatabaseConfig.java`

```java
static {
    loadEnvironmentVariables();
    initializeConfiguration();
}
```

**Problem:**
- `DatabaseConfig` has a static initializer that runs when the class is first loaded
- It attempts to load from `.env` file which **doesn't exist in CI/CD**
- It **requires** `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` to be set
- If these aren't set, it throws `RuntimeException` immediately
- This happens **before** tests can set test-specific environment variables

**CI/CD Impact:**
```
❌ Database configuration missing. Please set DATABASE_URL, 
   DATABASE_USER, and DATABASE_PASSWORD environment variables 
   in .env file or system environment.
```

---

### Issue 2: Environment Variable Timing

**File:** `.github/workflows/ci-cd.yml`

```yaml
env:
  TEST_DATABASE_URL: jdbc:postgresql://localhost:5432/drimacc_test
  TEST_DATABASE_USER: postgres
  TEST_DATABASE_PASSWORD: postgres
```

**Problem:**
- CI/CD sets `TEST_DATABASE_*` variables (correct naming)
- But `DatabaseConfig` static block expects `DATABASE_*` variables (production naming)
- The test environment variables are set too late - **after** `DatabaseConfig` class loads
- When JUnit tries to run tests, `DatabaseConfig` static initializer fails first

**Timeline:**
1. GitHub Actions starts → Sets `TEST_DATABASE_*` env vars
2. Gradle test task starts
3. JUnit loads test classes
4. Test class references `DatabaseConfig` → Static block runs
5. Static block looks for `DATABASE_*` vars (not `TEST_DATABASE_*`)
6. **FAILS** with missing configuration exception
7. Tests never run

---

### Issue 3: Test Configuration Mismatch

**File:** `app/src/test/java/fin/TestConfiguration.java`

```java
static {
    loadEnvironmentVariables(); // Looks for test.env file
    
    TEST_DB_URL = getConfigValue("TEST_DATABASE_URL");  // ✅ Correct
    TEST_DB_USER = getConfigValue("TEST_DATABASE_USER"); // ✅ Correct
    TEST_DB_PASSWORD = getConfigValue("TEST_DATABASE_PASSWORD"); // ✅ Correct
}
```

**Problem:**
- `TestConfiguration` correctly reads `TEST_DATABASE_*` variables
- But many services/tests directly use `DatabaseConfig.getDatabaseUrl()`
- This bypasses `TestConfiguration` entirely
- Result: Tests use production config instead of test config

---

## 📋 Affected Test Files

Tests that will fail in CI/CD:

1. **AccountClassificationServiceTest** (NEW - just added)
   ```java
   service = new AccountClassificationService("jdbc:postgresql://dummy:5432/test");
   // ✅ This one is OK - uses explicit URL, doesn't trigger DatabaseConfig
   ```

2. **Any test using services that reference `DatabaseConfig`:**
   - `CompanyServiceTest`
   - `AccountServiceTest`
   - `TransactionMappingServiceTest`
   - `BankStatementProcessingServiceTest`
   - `PayrollServiceTest`
   - All integration tests

---

## 💡 Solutions

### Solution 1: Fix DatabaseConfig to Support Test Mode (RECOMMENDED)

**Modify:** `app/src/main/java/fin/config/DatabaseConfig.java`

```java
static {
    // BEFORE: Always required DATABASE_* vars
    loadEnvironmentVariables();
    initializeConfiguration();
    
    // AFTER: Check for test mode first
    if (isTestMode()) {
        loadTestConfiguration();
    } else {
        loadEnvironmentVariables();
        initializeConfiguration();
    }
}

private static boolean isTestMode() {
    // Check if we're running in test context
    String testDb = System.getenv("TEST_DATABASE_URL");
    String testProperty = System.getProperty("fin.test.mode");
    return testDb != null || testProperty != null;
}

private static void loadTestConfiguration() {
    databaseUrl = getConfigValue("TEST_DATABASE_URL");
    databaseUser = getConfigValue("TEST_DATABASE_USER");
    databasePassword = getConfigValue("TEST_DATABASE_PASSWORD");
    
    if (databaseUrl != null && databaseUser != null && databasePassword != null) {
        System.out.println("🧪 DatabaseConfig initialized in TEST MODE");
        return;
    }
    
    // Fall back to production config if test vars not set
    loadEnvironmentVariables();
    initializeConfiguration();
}
```

**Benefits:**
- ✅ Automatically detects test environment
- ✅ Uses `TEST_DATABASE_*` vars when available
- ✅ Falls back to production config gracefully
- ✅ No changes needed to tests
- ✅ Works in CI/CD without `.env` file

---

### Solution 2: Make Environment Variables Optional (ALTERNATIVE)

**Modify:** `app/src/main/java/fin/config/DatabaseConfig.java`

```java
private static void initializeConfiguration() {
    loadConfiguration();
}

public static void loadConfiguration() {
    // Check test mode first
    String testDbUrl = System.getProperty("fin.database.test.url");
    if (testDbUrl != null && !testDbUrl.isEmpty()) {
        // Already handles test mode correctly
        return;
    }
    
    // Read production database configuration
    String dbUrl = getConfigValue("DATABASE_URL");
    databaseUser = getConfigValue("DATABASE_USER");
    databasePassword = getConfigValue("DATABASE_PASSWORD");
    
    // CHANGE: Make optional instead of required
    if (dbUrl == null || databaseUser == null || databasePassword == null) {
        // Check if TEST_DATABASE_* vars are available
        dbUrl = getConfigValue("TEST_DATABASE_URL");
        databaseUser = getConfigValue("TEST_DATABASE_USER");
        databasePassword = getConfigValue("TEST_DATABASE_PASSWORD");
        
        if (dbUrl == null || databaseUser == null || databasePassword == null) {
            System.err.println("⚠️ No database configuration found. Set DATABASE_* or TEST_DATABASE_* env vars.");
            // DON'T throw exception - let tests configure later
            return;
        } else {
            System.out.println("🧪 Using TEST_DATABASE_* configuration");
        }
    }
    
    // Rest of initialization...
}
```

---

### Solution 3: Add GitHub Actions Environment File

**Create:** `.github/workflows/.env` (NOT RECOMMENDED - security risk)

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/drimacc_test
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres
```

**Problems:**
- ❌ Exposes credentials in repository
- ❌ Doesn't work - Actions won't automatically load this
- ❌ Requires secrets configuration
- ❌ Still has timing issues

---

### Solution 4: Update CI/CD Workflow to Set Correct Variables

**Modify:** `.github/workflows/ci-cd.yml`

```yaml
env:
  JAVA_VERSION: '17'
  GRADLE_OPTS: -Xmx4g -XX:MaxMetaspaceSize=1g
  
  # CHANGE: Use DATABASE_* instead of TEST_DATABASE_*
  DATABASE_URL: jdbc:postgresql://localhost:5432/drimacc_test
  DATABASE_USER: postgres
  DATABASE_PASSWORD: postgres
  
  # Keep these for TestConfiguration
  TEST_DATABASE_URL: jdbc:postgresql://localhost:5432/drimacc_test
  TEST_DATABASE_USER: postgres
  TEST_DATABASE_PASSWORD: postgres
```

**Benefits:**
- ✅ Quick fix - minimal code changes
- ✅ Works immediately
- ✅ Both production and test vars set

**Drawbacks:**
- ⚠️ Confusing - why set both?
- ⚠️ Doesn't solve root cause
- ⚠️ Future maintenance confusion

---

## 🎯 Recommended Fix (Combined Approach)

### Step 1: Update DatabaseConfig (Best Fix)

```java
private static void loadConfiguration() {
    // Check test mode FIRST (before checking .env)
    String testDbUrl = getConfigValue("TEST_DATABASE_URL");
    if (testDbUrl != null) {
        System.out.println("🧪 Test mode detected - using TEST_DATABASE_* variables");
        databaseUrl = testDbUrl;
        databaseUser = getConfigValue("TEST_DATABASE_USER");
        databasePassword = getConfigValue("TEST_DATABASE_PASSWORD");
        
        if (databaseUser != null && databasePassword != null) {
            System.out.println("✅ Test database configuration loaded");
            return; // Success - use test config
        }
    }
    
    // Continue with existing production config logic...
    String dbUrl = getConfigValue("DATABASE_URL");
    // ... rest of existing code
}
```

### Step 2: Verify CI/CD Variables Are Set

Ensure `.github/workflows/ci-cd.yml` has:
```yaml
env:
  TEST_DATABASE_URL: jdbc:postgresql://localhost:5432/drimacc_test
  TEST_DATABASE_USER: postgres
  TEST_DATABASE_PASSWORD: postgres
```

### Step 3: Test Locally

```bash
# Test with environment variables (simulating CI/CD)
export TEST_DATABASE_URL="jdbc:postgresql://localhost:5432/drimacc_test"
export TEST_DATABASE_USER="postgres"
export TEST_DATABASE_PASSWORD="postgres"

./gradlew clean build --no-daemon
```

---

## 📊 Verification Checklist

After implementing fix:

- [ ] Run `./gradlew clean build --no-daemon` locally (should pass)
- [ ] Run `./gradlew test --no-daemon` locally (should pass)
- [ ] Verify `AccountClassificationServiceTest` passes (16/16 tests)
- [ ] Push to GitHub and check Actions tab
- [ ] Verify Build job passes (green checkmark)
- [ ] Verify Test job passes (both unit and integration)
- [ ] Verify Quality job passes (checkstyle warnings OK)
- [ ] Verify Deploy job passes (if on main branch)

---

## 🚨 Current CI/CD Status

**Expected Failure Points:**

1. **Build Job** - Will likely FAIL at test execution
   ```
   > Task :app:test FAILED
   Error: Database configuration missing
   ```

2. **Test Job** - Will FAIL before any tests run
   ```
   Could not initialize class fin.config.DatabaseConfig
   ```

3. **Quality Job** - Might pass (doesn't run tests)

4. **Deploy Job** - Will FAIL (depends on test job)

---

## 📝 Action Items

### Immediate (Priority 1)
1. ✅ Update `DatabaseConfig.java` to check for `TEST_DATABASE_*` vars first
2. ✅ Test locally with environment variables
3. ✅ Commit and push fix
4. ✅ Monitor GitHub Actions for green build

### Short Term (Priority 2)
1. Add integration tests that verify database config in test mode
2. Update documentation with CI/CD setup instructions
3. Create `.env.example` and `test.env.example` templates

### Long Term (Priority 3)
1. Consider dependency injection for database configuration
2. Migrate to connection pool (HikariCP already in dependencies)
3. Add database migration framework (Flyway or Liquibase)

---

## 🔗 Related Files

- `/app/src/main/java/fin/config/DatabaseConfig.java` - Main config class
- `/app/src/test/java/fin/TestConfiguration.java` - Test config class
- `/.github/workflows/ci-cd.yml` - GitHub Actions workflow
- `/app/build.gradle.kts` - Gradle build configuration
- `/test_schema.sql` - Test database schema

---

## 📚 References

- [GitHub Actions Environment Variables](https://docs.github.com/en/actions/learn-github-actions/environment-variables)
- [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/documentation/)
- [JUnit 5 Test Configuration](https://junit.org/junit5/docs/current/user-guide/)
- [Gradle Test Configuration](https://docs.gradle.org/current/userguide/java_testing.html)

---

**Analysis Date:** October 4, 2025  
**Status:** ⚠️ IDENTIFIED - Fix Ready to Implement  
**Impact:** 🔴 HIGH - All CI/CD builds failing  
**Effort:** 🟢 LOW - 10-20 lines of code change
