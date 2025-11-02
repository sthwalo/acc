# TASK 6.4: AccountClassificationService Architectural Refactoring

**Status:** 🔴 NOT STARTED  
**Priority:** MEDIUM (Technical Debt)  
**Category:** Architecture & Code Quality  
**Created:** 2025-11-02  
**Estimated Effort:** 3-5 days  

## 🎯 Objective

Refactor `AccountClassificationService.java` (2,230 lines) to follow modular architecture principles by:
1. Extracting display/formatting logic to OutputFormatter
2. Implementing proper dependency injection for CompanyService
3. Splitting into focused, single-responsibility services
4. Eliminating direct System.out.println usage (50+ occurrences)

---

## 🔍 Current State Analysis

### File Statistics:
- **Total Lines:** 2,230 lines (MASSIVE service class)
- **System.out.println calls:** 50+ instances
- **Manual instantiation:** CompanyService (line 103) - violates DI pattern
- **Mixed concerns:** Business logic + presentation logic + data access

### Architectural Violations:

#### 1. **No OutputFormatter Usage**
- **Lines:** 118, 129, 151, 160, 206, 226, 244, 281, 301, 302, 496, 542-545, 564, 568-569, 611, 614, 619, 662+
- **Issue:** Manual `System.out.println()` with emoji formatting
- **Should Use:** `OutputFormatter.printSuccess()`, `printInfo()`, `printHeader()`, `printSeparator()`

Example violations:
```java
// ❌ WRONG - Manual formatting
System.out.println("🏗️ Initializing Chart of Accounts for: " + company.getName());
System.out.println("✅ Chart of Accounts initialization complete!");
System.out.println("=".repeat(DISPLAY_WIDTH_STANDARD));

// ✅ CORRECT - Use OutputFormatter
outputFormatter.printInfo("Initializing Chart of Accounts for: " + company.getName());
outputFormatter.printSuccess("Chart of Accounts initialization complete!");
outputFormatter.printSeparator();
```

#### 2. **Manual Service Instantiation (Line 103)**
```java
// ❌ WRONG - Manual instantiation in constructor
public AccountClassificationService(String initialDbUrl) {
    this.dbUrl = initialDbUrl;
    try {
        this.companyService = new CompanyService(dbUrl);  // VIOLATES DI
    } catch (Exception e) {
        throw new RuntimeException("Failed to initialize CompanyService", e);
    }
}

// ✅ CORRECT - Dependency injection via constructor
public AccountClassificationService(String dbUrl, CompanyService companyService, 
                                   OutputFormatter outputFormatter) {
    this.dbUrl = dbUrl;
    this.companyService = companyService;  // Injected
    this.outputFormatter = outputFormatter;  // Injected
}
```

#### 3. **Single Responsibility Violation**
The service handles multiple concerns:
- Chart of accounts initialization (accounts, categories)
- Transaction pattern analysis
- Mapping rule generation (1,500+ lines of hardcoded rules)
- Display/formatting logic
- Report generation

**Recommended Split:**
- `ChartOfAccountsService` - Account/category management
- `TransactionPatternAnalyzer` - Pattern analysis
- `MappingRuleGenerator` - Rule creation (or use database-driven rules)
- Controllers/UI layer - Display logic

---

## 📋 Implementation Plan

### Phase 1: Inject OutputFormatter (2 hours)
**Goal:** Replace all System.out.println with OutputFormatter methods

**Steps:**
1. Add `OutputFormatter` field to constructor
2. Replace 50+ println calls with:
   - `printInfo()` - Info messages (🔗, 🏗️, 📋)
   - `printSuccess()` - Success messages (✅)
   - `printError()` - Error messages (❌)
   - `printHeader()` - Report headers (📈, 📊)
   - `printSeparator()` - Dividers (=====, -----)

**Files Modified:**
- `AccountClassificationService.java` (50+ line changes)
- `ApplicationContext.java` (pass OutputFormatter to constructor)

**Testing:**
```bash
./gradlew test --tests "*AccountClassification*"
```

---

### Phase 2: Fix CompanyService Dependency Injection (1 hour)
**Goal:** Inject CompanyService via constructor instead of manual instantiation

**Changes:**
```java
// Before (line 91-107):
public AccountClassificationService(String initialDbUrl) {
    this.dbUrl = initialDbUrl;
    this.companyService = new CompanyService(dbUrl);  // Manual
}

// After:
public AccountClassificationService(String dbUrl, CompanyService companyService) {
    this.dbUrl = dbUrl;
    this.companyService = companyService;  // Injected
}
```

**Files Modified:**
- `AccountClassificationService.java` (constructor signature)
- `ApplicationContext.java` (pass CompanyService)
- All callers (controllers, tests)

**Testing:**
```bash
./gradlew test --tests "*AccountClassification*"
./gradlew compileJava --no-daemon
```

---

### Phase 3: Extract ChartOfAccountsService (4-6 hours)
**Goal:** Create separate service for chart of accounts management

**New Service:**
```java
public class ChartOfAccountsService {
    private final String dbUrl;
    private final OutputFormatter outputFormatter;
    
    public void initializeChartOfAccounts(Long companyId) { ... }
    private Map<String, Long> createAccountCategories(Long companyId) { ... }
    private void createStandardAccounts(Long companyId, Map<String, Long> categoryIds) { ... }
    private List<AccountDefinition> getStandardAccountDefinitions(...) { ... }
    // + 20 helper methods for account creation
}
```

**Methods to Extract (from AccountClassificationService):**
- `initializeChartOfAccounts()`
- `createAccountCategories()`
- `createCategory()`
- `createStandardAccounts()`
- `getStandardAccountDefinitions()`
- `addCurrentAssetAccounts()` + 13 similar methods
- `AccountDefinition` inner class

**Files Created:**
- `fin/service/ChartOfAccountsService.java` (new)

**Files Modified:**
- `AccountClassificationService.java` (remove extracted methods)
- `ApplicationContext.java` (register new service)
- Controllers using chart of accounts initialization

**Testing:**
```bash
./gradlew test --tests "*ChartOfAccounts*"
./gradlew test --tests "*AccountClassification*"
```

---

### Phase 4: Extract MappingRuleGenerator (6-8 hours)
**Goal:** Separate mapping rule generation logic (1,500+ lines of hardcoded rules)

**New Service:**
```java
public class MappingRuleGenerator {
    public List<TransactionMappingRule> getStandardMappingRules() { ... }
    
    // 40+ private methods for rule creation
    private void addCriticalPatternsRules(List<TransactionMappingRule> rules) { ... }
    private void addHighConfidencePatternsRules(List<TransactionMappingRule> rules) { ... }
    // ... etc
}
```

**Methods to Extract (Lines 772-2100+):**
- `getStandardMappingRules()`
- `addCriticalPatternsRules()` + 40+ similar methods
- All employee payment rules
- All supplier payment rules
- All bank charge rules
- All revenue/expense rules

**Files Created:**
- `fin/service/MappingRuleGenerator.java` (new, ~1,500 lines)

**Files Modified:**
- `AccountClassificationService.java` (remove extracted methods)
- `ApplicationContext.java` (register new service)

**Testing:**
```bash
./gradlew test --tests "*MappingRule*"
./run.sh  # Test full application flow
```

---

### Phase 5: Extract TransactionPatternAnalyzer (3-4 hours)
**Goal:** Separate transaction analysis and reporting logic

**New Service:**
```java
public class TransactionPatternAnalyzer {
    private final String dbUrl;
    private final OutputFormatter outputFormatter;
    
    public void analyzeTransactionPatterns(Long companyId) { ... }
    public Map<String, String> getAccountMappingSuggestions(Long companyId) { ... }
    public void generateClassificationReport(Long companyId) { ... }
    
    // Display methods
    private void displayTransactionPatternAnalysis(Long companyId) { ... }
    private void displaySampleTransactions(Long companyId) { ... }
    private void displayAccountCategories(Long companyId) { ... }
}
```

**Methods to Extract:**
- `analyzeTransactionPatterns()`
- `displayTransactionPatternAnalysis()`
- `displaySampleTransactions()`
- `getAccountMappingSuggestions()`
- `generateClassificationReport()`
- All print/display helper methods

**Files Created:**
- `fin/service/TransactionPatternAnalyzer.java` (new)

**Files Modified:**
- `AccountClassificationService.java` (remove extracted methods)
- `ApplicationContext.java` (register new service)

**Testing:**
```bash
./gradlew test --tests "*TransactionPattern*"
```

---

## ✅ Success Criteria

### Code Quality Metrics:
- ✅ **No System.out.println in services** (use OutputFormatter)
- ✅ **All services use dependency injection** (no manual `new Service()`)
- ✅ **AccountClassificationService < 500 lines** (down from 2,230)
- ✅ **Single responsibility per service**
- ✅ **All existing tests pass**
- ✅ **New unit tests for extracted services**

### Build Verification:
```bash
# Must pass after each phase:
./gradlew clean build
./gradlew test
./run.sh  # Interactive testing

# Final verification:
./gradlew spotbugsMain
./gradlew checkstyleMain
```

### Integration Testing:
1. **Chart of Accounts Initialization**
   - Run interactive menu: Data Management → Initialize Chart of Accounts
   - Verify all accounts created correctly
   - Check database: `SELECT COUNT(*) FROM accounts;`

2. **Mapping Rules Generation**
   - Run interactive menu: Data Management → Initialize Mapping Rules
   - Verify rules created: `SELECT COUNT(*) FROM transaction_mapping_rules;`

3. **Transaction Classification**
   - Process bank statement
   - Verify transactions auto-classified correctly
   - Check manual fallback for unmatched patterns

4. **Pattern Analysis Reports**
   - Run classification report
   - Verify formatting uses OutputFormatter
   - Check report accuracy

---

## 🧪 Testing Strategy

### Unit Tests (New):
```java
// ChartOfAccountsServiceTest.java
@Test
void testInitializeChartOfAccounts() {
    service.initializeChartOfAccounts(companyId);
    // Verify accounts created
}

// MappingRuleGeneratorTest.java
@Test
void testGetStandardMappingRules() {
    List<TransactionMappingRule> rules = generator.getStandardMappingRules();
    assertTrue(rules.size() > 100);
}

// TransactionPatternAnalyzerTest.java
@Test
void testAnalyzeTransactionPatterns() {
    analyzer.analyzeTransactionPatterns(companyId);
    // Verify analysis completes
}
```

### Integration Tests:
```bash
# Test full workflow
./run.sh
# 1. Select company
# 2. Data Management → Initialize Chart of Accounts
# 3. Data Management → Initialize Mapping Rules
# 4. Upload bank statement
# 5. Verify classification
# 6. Generate reports
```

---

## 📚 References

### Related Documentation:
- `/docs/TRANSACTION_CLASSIFICATION_CONSOLIDATION_2025-10-11.md` - Classification system overview
- `/docs/DATA_MANAGEMENT_ARCHITECTURE_ANALYSIS_2025-10-11.md` - Data management analysis
- `/.github/copilot-instructions.md` - Modular architecture pattern

### Related Code Files:
- `fin/service/AccountClassificationService.java` (2,230 lines - TO REFACTOR)
- `fin/service/CompanyService.java` (proper DI example)
- `fin/ui/OutputFormatter.java` (formatting methods to use)
- `fin/context/ApplicationContext.java` (DI container)

### Related Tasks:
- TASK 6.1 - Budget Generation (similar service architecture)
- TASK 5.x - Checkstyle cleanup tasks
- TASK 4.x - SpotBugs remediation

---

## 🚨 Risks & Mitigation

### Risk 1: Breaking Existing Functionality
**Mitigation:**
- Extract methods incrementally (one phase at a time)
- Run full test suite after each phase
- Keep original methods commented out initially
- Test with real data (7,156+ transactions)

### Risk 2: Tight Coupling with Controllers
**Mitigation:**
- Identify all callers before refactoring
- Update ApplicationContext registration
- Update controller dependencies
- Test each controller after changes

### Risk 3: Database Query Changes
**Mitigation:**
- Do NOT change SQL queries during refactoring
- Only move methods, don't rewrite logic
- Verify query results match before/after
- Test with production database backup

---

## 📝 Progress Tracking

### Phase 1: OutputFormatter Injection
- [ ] Add OutputFormatter field to constructor
- [ ] Replace println in `initializeChartOfAccounts()`
- [ ] Replace println in `initializeTransactionMappingRules()`
- [ ] Replace println in analysis methods
- [ ] Replace println in report generation methods
- [ ] Update ApplicationContext
- [ ] Run tests
- [ ] Verify with `./run.sh`

### Phase 2: CompanyService DI
- [ ] Update constructor signature
- [ ] Remove manual instantiation
- [ ] Update ApplicationContext
- [ ] Update all callers
- [ ] Run tests

### Phase 3: ChartOfAccountsService Extraction
- [ ] Create new service class
- [ ] Extract account initialization methods
- [ ] Extract category creation methods
- [ ] Extract account definition methods
- [ ] Update ApplicationContext
- [ ] Create unit tests
- [ ] Run integration tests

### Phase 4: MappingRuleGenerator Extraction
- [ ] Create new service class
- [ ] Extract getStandardMappingRules()
- [ ] Extract 40+ rule creation methods
- [ ] Update ApplicationContext
- [ ] Create unit tests
- [ ] Test mapping rule generation

### Phase 5: TransactionPatternAnalyzer Extraction
- [ ] Create new service class
- [ ] Extract analysis methods
- [ ] Extract display methods
- [ ] Update ApplicationContext
- [ ] Create unit tests
- [ ] Test full classification workflow

### Final Verification:
- [ ] All tests pass: `./gradlew clean build`
- [ ] No checkstyle warnings: `./gradlew checkstyleMain`
- [ ] No spotbugs warnings: `./gradlew spotbugsMain`
- [ ] Interactive testing: `./run.sh`
- [ ] Production data testing (7,156+ transactions)
- [ ] Update documentation

---

## 🔄 Rollback Plan

If refactoring causes issues:

1. **Git Revert:**
   ```bash
   git log --oneline  # Find commit before refactoring
   git revert <commit-hash>
   ```

2. **Phase Rollback:**
   - Each phase should be a separate commit
   - Can revert individual phases if needed
   - Keep original code commented out during first phase

3. **Emergency Fallback:**
   - Restore from backup: `git stash`
   - Use previous working commit
   - File issue for investigation

---

## 📊 Completion Checklist

Before marking this task as COMPLETED:

- [ ] AccountClassificationService < 500 lines
- [ ] ChartOfAccountsService created and tested
- [ ] MappingRuleGenerator created and tested
- [ ] TransactionPatternAnalyzer created and tested
- [ ] All services use dependency injection
- [ ] All services use OutputFormatter (no direct println)
- [ ] ApplicationContext registers all new services
- [ ] All existing tests pass
- [ ] New unit tests created (3 new test classes)
- [ ] Integration testing completed
- [ ] Code review completed
- [ ] Documentation updated
- [ ] README.md updated with new services

---

**Last Updated:** 2025-11-02  
**Next Review:** After Phase 1 completion  
**Assigned To:** [Team Lead / Developer]
