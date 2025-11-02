# TASK 6.9: TransactionClassificationService Dependency Injection & Repository Pattern

**Status:** 📋 PLANNED  
**Priority:** HIGH  
**Category:** Architectural Improvement & Technical Debt  
**Estimated Effort:** 3-4 hours  
**Created:** 2025-11-02  

---

## 🎯 Objective

Refactor `TransactionClassificationService` to follow FIN's architectural patterns by:
1. Implementing proper dependency injection for all services
2. Removing unused constructor parameter (`ClassificationRuleManager`)
3. Extracting direct database access into repository pattern
4. Improving testability and consistency with other FIN services

---

## 📋 Current State Analysis

### Existing Implementation Issues:

#### 1. **❌ Incomplete Dependency Injection (Tight Coupling)**
```java
// CURRENT: Line 87-91
public TransactionClassificationService(String initialDbUrl,
                                       ClassificationRuleManager ruleManager,  // ❌ NEVER USED!
                                       InteractiveClassificationService initialInteractiveService) {
    this.dbUrl = initialDbUrl;
    this.accountClassificationService = new AccountClassificationService(initialDbUrl);  // ❌ Direct instantiation
    this.interactiveService = initialInteractiveService;
```

**Problems:**
- `AccountClassificationService` created directly instead of injected
- ApplicationContext already creates `AccountClassificationService` → wasteful duplication
- `ClassificationRuleManager ruleManager` parameter accepted but **NEVER used anywhere**
- Can't inject mocks for testing
- Violates FIN's ApplicationContext pattern

#### 2. **❌ Direct Database Access (Repository Pattern Violation)**

**5 helper methods with direct JDBC:**

```java
// Line 421: deleteJournalEntriesExceptOpeningBalances()
try (Connection conn = DriverManager.getConnection(dbUrl);  // ❌ Direct JDBC
     PreparedStatement stmt = conn.prepareStatement(sql)) {

// Line 448: countClassifiedTransactions()
// Line 482: getCompanyById()
// Line 519: countUnclassifiedTransactions()
// Line 552: countClassifiedWithoutJournalEntries()
```

**Problems:**
- Violates Single Responsibility Principle (business logic + data access)
- Doesn't follow FIN's Repository Pattern
- Code duplication (queries likely exist in repositories)
- Hard to test (can't mock database)
- Inconsistent with other FIN services (CompanyService, PayrollService use repositories)

#### 3. **⚠️ Misleading API (Unused Parameter)**

ApplicationContext passes `ClassificationRuleManager` but service completely ignores it:

```java
// In ApplicationContext.java (line 204-208):
TransactionClassificationService transactionClassificationService = new TransactionClassificationService(
    initialDbUrl,
    classificationRuleManager,  // ❌ Passed but never used
    get(InteractiveClassificationService.class)
);
```

---

## 🔧 Implementation Plan

### Phase 1: Fix Constructor Dependency Injection (Priority: HIGH)

#### Step 1.1: Update Constructor Signature
**File:** `app/src/main/java/fin/service/TransactionClassificationService.java`

**Before (Lines 87-95):**
```java
public TransactionClassificationService(String initialDbUrl,
                                       ClassificationRuleManager ruleManager,  // ❌ Unused
                                       InteractiveClassificationService initialInteractiveService) {
    this.dbUrl = initialDbUrl;
    this.accountClassificationService = new AccountClassificationService(initialDbUrl);  // ❌ Direct
    this.interactiveService = initialInteractiveService;
    
    LOGGER.info("TransactionClassificationService initialized with AccountClassificationService as single source of truth and ClassificationRuleManager for learned rules");
}
```

**After:**
```java
/**
 * Constructor with dependency injection
 * 
 * @param accountClassificationService The account classification service (injected)
 * @param interactiveService The interactive classification service (injected)
 */
public TransactionClassificationService(
        AccountClassificationService accountClassificationService,
        InteractiveClassificationService interactiveService) {
    if (accountClassificationService == null || interactiveService == null) {
        throw new IllegalArgumentException("Services cannot be null");
    }
    this.accountClassificationService = accountClassificationService;
    this.interactiveService = interactiveService;
    
    LOGGER.info("TransactionClassificationService initialized with injected dependencies");
}
```

**Changes:**
- Remove unused `ClassificationRuleManager ruleManager` parameter
- Remove `String initialDbUrl` parameter (no longer needed for service instantiation)
- Inject `AccountClassificationService` instead of creating it
- Add null validation for dependencies
- Update Javadoc to reflect new signature

#### Step 1.2: Add Deprecated Constructor for Backward Compatibility
**File:** `app/src/main/java/fin/service/TransactionClassificationService.java`

**Add after new constructor:**
```java
/**
 * Simplified constructor for backward compatibility
 * Creates its own service dependencies
 * 
 * @deprecated Use constructor with dependency injection instead
 */
@Deprecated
public TransactionClassificationService(String dbUrl) {
    this(new AccountClassificationService(dbUrl), 
         new InteractiveClassificationService());
    
    LOGGER.warning("Using deprecated constructor - consider using dependency injection");
}
```

**Note:** The existing simplified constructor (line 102) can be marked as deprecated

#### Step 1.3: Update Field Declarations
**File:** `app/src/main/java/fin/service/TransactionClassificationService.java`

**Before (Lines 63-65):**
```java
private final String dbUrl;
private final AccountClassificationService accountClassificationService;
private final InteractiveClassificationService interactiveService;
```

**After (Phase 1 - Keep dbUrl temporarily):**
```java
private final String dbUrl;  // ⏸️ Keep temporarily for Phase 2 (repository methods)
private final AccountClassificationService accountClassificationService;
private final InteractiveClassificationService interactiveService;
```

**Note:** `dbUrl` will be removed in Phase 2 after repository extraction

---

### Phase 2: Update ApplicationContext Registration (Priority: HIGH)

#### Step 2.1: Update Service Registration
**File:** `app/src/main/java/fin/context/ApplicationContext.java`

**Before (Lines 198-208):**
```java
AccountClassificationService accountClassificationService = new AccountClassificationService(initialDbUrl);
register(AccountClassificationService.class, accountClassificationService);

// REMOVED: TransactionMappingService - consolidated into AccountClassificationService

// NOTE: TransactionClassificationService uses AccountClassificationService as single source of truth
TransactionClassificationService transactionClassificationService = new TransactionClassificationService(
    initialDbUrl,
    classificationRuleManager,  // ❌ Passed but unused
    get(InteractiveClassificationService.class)
);
register(TransactionClassificationService.class, transactionClassificationService);
```

**After:**
```java
AccountClassificationService accountClassificationService = new AccountClassificationService(initialDbUrl);
register(AccountClassificationService.class, accountClassificationService);

// NOTE: TransactionClassificationService uses AccountClassificationService as single source of truth
// Dependencies are injected (no direct instantiation)
TransactionClassificationService transactionClassificationService = new TransactionClassificationService(
    accountClassificationService,  // ✅ Inject (already created above)
    get(InteractiveClassificationService.class)  // ✅ Inject (from ApplicationContext)
);
register(TransactionClassificationService.class, transactionClassificationService);
```

**Changes:**
- Remove `initialDbUrl` parameter
- Remove `classificationRuleManager` parameter (unused)
- Pass already-instantiated `accountClassificationService`
- Update comments to reflect new pattern

---

### Phase 3: Extract Repository Pattern (Priority: MEDIUM)

#### Step 3.1: Add Repository Methods (If Not Already Present)

**File:** `app/src/main/java/fin/repository/CompanyRepository.java`

**Check if exists, add if missing:**
```java
/**
 * Find company by ID
 * 
 * @param companyId The company ID
 * @return Company or null if not found
 */
public Company findById(Long companyId) {
    String sql = "SELECT * FROM companies WHERE id = ?";
    
    try (Connection conn = DriverManager.getConnection(dbUrl);
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setLong(1, companyId);
        
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return mapResultSetToCompany(rs);
            }
        }
        
    } catch (SQLException e) {
        throw new RuntimeException("Error finding company by ID: " + companyId, e);
    }
    
    return null;
}
```

**File:** `app/src/main/java/fin/repository/BankTransactionRepository.java` (or create if missing)

**Add methods:**
```java
/**
 * Count unclassified transactions for a company and fiscal period
 */
public int countUnclassified(Long companyId, Long fiscalPeriodId) {
    String sql = """
        SELECT COUNT(*) 
        FROM bank_transactions 
        WHERE company_id = ? 
        AND fiscal_period_id = ? 
        AND account_code IS NULL
        """;
        
    try (Connection conn = DriverManager.getConnection(dbUrl);
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setLong(1, companyId);
        stmt.setLong(2, fiscalPeriodId);
        
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
    } catch (SQLException e) {
        throw new RuntimeException("Error counting unclassified transactions", e);
    }
    
    return 0;
}

/**
 * Count classified transactions (account_code IS NOT NULL)
 */
public int countClassified(Long companyId, Long fiscalPeriodId) {
    String sql = """
        SELECT COUNT(*) 
        FROM bank_transactions 
        WHERE company_id = ? 
        AND fiscal_period_id = ? 
        AND account_code IS NOT NULL
        """;
        
    // Implementation similar to countUnclassified
}

/**
 * Count classified transactions without journal entries
 */
public int countClassifiedWithoutJournalEntries(Long companyId, Long fiscalPeriodId) {
    String sql = """
        SELECT COUNT(*) 
        FROM bank_transactions bt
        WHERE bt.company_id = ? 
        AND bt.fiscal_period_id = ?
        AND bt.account_code IS NOT NULL
        AND bt.id NOT IN (
            SELECT DISTINCT source_transaction_id 
            FROM journal_entry_lines 
            WHERE source_transaction_id IS NOT NULL
        )
        """;
        
    // Implementation similar to countUnclassified
}
```

**File:** `app/src/main/java/fin/repository/JournalEntryRepository.java` (or create if missing)

**Add method:**
```java
/**
 * Delete all journal entries except opening balances
 * 
 * @param companyId The company ID
 * @param fiscalPeriodId The fiscal period ID
 * @return Number of journal entries deleted
 */
public int deleteExceptOpeningBalances(Long companyId, Long fiscalPeriodId) {
    String sql = """
        DELETE FROM journal_entries 
        WHERE company_id = ? 
        AND fiscal_period_id = ?
        AND NOT (LOWER(description) LIKE '%opening%balance%' OR reference LIKE 'OB-%')
        """;
        
    try (Connection conn = DriverManager.getConnection(dbUrl);
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setLong(1, companyId);
        stmt.setLong(2, fiscalPeriodId);
        
        return stmt.executeUpdate();
        
    } catch (SQLException e) {
        throw new RuntimeException("Failed to delete journal entries", e);
    }
}
```

#### Step 3.2: Update TransactionClassificationService Constructor (Phase 3)
**File:** `app/src/main/java/fin/service/TransactionClassificationService.java`

**After Phase 2, update to:**
```java
/**
 * Constructor with full dependency injection (including repositories)
 * 
 * @param accountClassificationService The account classification service (injected)
 * @param interactiveService The interactive classification service (injected)
 * @param companyRepository The company repository (injected)
 * @param transactionRepository The transaction repository (injected)
 * @param journalEntryRepository The journal entry repository (injected)
 */
public TransactionClassificationService(
        AccountClassificationService accountClassificationService,
        InteractiveClassificationService interactiveService,
        CompanyRepository companyRepository,
        BankTransactionRepository transactionRepository,
        JournalEntryRepository journalEntryRepository) {
    if (accountClassificationService == null || interactiveService == null 
        || companyRepository == null || transactionRepository == null 
        || journalEntryRepository == null) {
        throw new IllegalArgumentException("All dependencies are required");
    }
    
    this.accountClassificationService = accountClassificationService;
    this.interactiveService = interactiveService;
    this.companyRepository = companyRepository;
    this.transactionRepository = transactionRepository;
    this.journalEntryRepository = journalEntryRepository;
    
    LOGGER.info("TransactionClassificationService initialized with full dependency injection");
}
```

**Update fields:**
```java
private final AccountClassificationService accountClassificationService;
private final InteractiveClassificationService interactiveService;
private final CompanyRepository companyRepository;
private final BankTransactionRepository transactionRepository;
private final JournalEntryRepository journalEntryRepository;
// dbUrl field removed - no longer needed
```

#### Step 3.3: Replace Helper Methods with Repository Calls
**File:** `app/src/main/java/fin/service/TransactionClassificationService.java`

**Before (Lines 421-442):**
```java
private int deleteJournalEntriesExceptOpeningBalances(Long companyId, Long fiscalPeriodId) {
    String sql = """
        DELETE FROM journal_entries 
        WHERE company_id = ? 
        AND fiscal_period_id = ?
        AND NOT (LOWER(description) LIKE '%opening%balance%' OR reference LIKE 'OB-%')
        """;
        
    try (Connection conn = DriverManager.getConnection(dbUrl);
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setLong(1, companyId);
        stmt.setLong(2, fiscalPeriodId);
        
        return stmt.executeUpdate();
        
    } catch (SQLException e) {
        LOGGER.log(Level.SEVERE, "Error deleting journal entries", e);
        throw new RuntimeException("Failed to delete journal entries", e);
    }
}
```

**After:**
```java
private int deleteJournalEntriesExceptOpeningBalances(Long companyId, Long fiscalPeriodId) {
    return journalEntryRepository.deleteExceptOpeningBalances(companyId, fiscalPeriodId);
}
```

**Similar replacements for:**
- `countClassifiedTransactions()` → `transactionRepository.countClassified()`
- `getCompanyById()` → `companyRepository.findById()`
- `countUnclassifiedTransactions()` → `transactionRepository.countUnclassified()`
- `countClassifiedWithoutJournalEntries()` → `transactionRepository.countClassifiedWithoutJournalEntries()`

#### Step 3.4: Update ApplicationContext (Phase 3)
**File:** `app/src/main/java/fin/context/ApplicationContext.java`

**After Phase 2, update to:**
```java
// Register repositories first
CompanyRepository companyRepository = new CompanyRepository(initialDbUrl);
register(CompanyRepository.class, companyRepository);

BankTransactionRepository transactionRepository = new BankTransactionRepository(initialDbUrl);
register(BankTransactionRepository.class, transactionRepository);

JournalEntryRepository journalEntryRepository = new JournalEntryRepository(initialDbUrl);
register(JournalEntryRepository.class, journalEntryRepository);

// Register services (with repository dependencies)
AccountClassificationService accountClassificationService = new AccountClassificationService(initialDbUrl);
register(AccountClassificationService.class, accountClassificationService);

TransactionClassificationService transactionClassificationService = new TransactionClassificationService(
    accountClassificationService,
    get(InteractiveClassificationService.class),
    companyRepository,
    transactionRepository,
    journalEntryRepository
);
register(TransactionClassificationService.class, transactionClassificationService);
```

---

## 🧪 Testing Strategy

### Unit Tests to Add/Update

#### Test 1: Constructor Dependency Injection (Phase 1)
```java
@Test
void testConstructorWithDependencyInjection() {
    AccountClassificationService mockAccountService = mock(AccountClassificationService.class);
    InteractiveClassificationService mockInteractiveService = mock(InteractiveClassificationService.class);
    
    TransactionClassificationService service = 
        new TransactionClassificationService(mockAccountService, mockInteractiveService);
    
    assertNotNull(service);
}

@Test
void testConstructorRejectsNullDependencies() {
    assertThrows(IllegalArgumentException.class, () -> {
        new TransactionClassificationService(null, null);
    });
}
```

#### Test 2: ApplicationContext Registration
```java
@Test
void testServiceRegisteredInApplicationContext() {
    ApplicationContext context = new ApplicationContext();
    TransactionClassificationService service = 
        context.get(TransactionClassificationService.class);
    
    assertNotNull(service);
}
```

#### Test 3: Repository Integration (Phase 3)
```java
@Test
void testRepositoryIntegration() {
    CompanyRepository mockCompanyRepo = mock(CompanyRepository.class);
    BankTransactionRepository mockTransactionRepo = mock(BankTransactionRepository.class);
    JournalEntryRepository mockJournalRepo = mock(JournalEntryRepository.class);
    
    when(mockTransactionRepo.countUnclassified(1L, 1L)).thenReturn(10);
    
    TransactionClassificationService service = new TransactionClassificationService(
        mock(AccountClassificationService.class),
        mock(InteractiveClassificationService.class),
        mockCompanyRepo,
        mockTransactionRepo,
        mockJournalRepo
    );
    
    // Service should delegate to repository
    // Test via public methods that use repositories
}
```

#### Test 4: Backward Compatibility
```java
@Test
@SuppressWarnings("deprecation")
void testDeprecatedConstructor() {
    // Ensure deprecated constructor still works
    TransactionClassificationService service = 
        new TransactionClassificationService("jdbc:postgresql://localhost/test");
    
    assertNotNull(service);
}
```

---

## ✅ Success Criteria

### Phase 1 (Constructor Refactoring):
- ✅ Constructor accepts `AccountClassificationService` and `InteractiveClassificationService` parameters
- ✅ Null validation added for dependencies
- ✅ Unused `ClassificationRuleManager` parameter removed
- ✅ `dbUrl` parameter removed from primary constructor
- ✅ Deprecated backward-compatible constructor added
- ✅ All unit tests pass

### Phase 2 (ApplicationContext Update):
- ✅ ApplicationContext passes injected dependencies
- ✅ No direct service instantiation in service constructor
- ✅ Service retrieval via `context.get()` works correctly
- ✅ Integration tests pass

### Phase 3 (Repository Pattern):
- ✅ All 5 helper methods replaced with repository calls
- ✅ No direct JDBC in service class
- ✅ `dbUrl` field removed
- ✅ Repositories properly injected
- ✅ ApplicationContext registers repositories before services
- ✅ All tests pass with mocked repositories

---

## 📊 Impact Assessment

### Files to Modify:

**Phase 1 (HIGH Priority):**
1. `app/src/main/java/fin/service/TransactionClassificationService.java` (constructor refactoring)
2. `app/src/main/java/fin/context/ApplicationContext.java` (service registration)
3. Unit test files (add new tests)

**Phase 2 (MEDIUM Priority - Can be deferred):**
4. `app/src/main/java/fin/repository/CompanyRepository.java` (add `findById()` if missing)
5. `app/src/main/java/fin/repository/BankTransactionRepository.java` (add count methods)
6. `app/src/main/java/fin/repository/JournalEntryRepository.java` (add delete method)
7. `app/src/main/java/fin/service/TransactionClassificationService.java` (replace helper methods)
8. `app/src/main/java/fin/context/ApplicationContext.java` (register repositories)

### Estimated Lines Changed:

**Phase 1:**
- TransactionClassificationService.java: ~30 lines modified, ~10 added
- ApplicationContext.java: ~10 lines modified
- Tests: ~60 lines added

**Phase 3:**
- Repository files: ~150 lines added (3 repositories × 50 lines each)
- TransactionClassificationService.java: ~120 lines removed (helper methods), ~15 added (repository calls)
- ApplicationContext.java: ~15 lines added

**Total:** ~410 lines changed

---

## 🚧 Risks & Mitigation

### Risk 1: Breaking Existing Consumers
**Mitigation:** Keep deprecated constructor for backward compatibility during Phase 1

### Risk 2: Repository Methods Don't Exist
**Mitigation:** Check existing repositories first, create methods only if missing

### Risk 3: ApplicationContext Initialization Order
**Mitigation:** Register repositories before services, ensure proper dependency graph

### Risk 4: Test Coverage Gaps
**Mitigation:** Add comprehensive unit and integration tests before refactoring

### Risk 5: Performance Impact
**Mitigation:** Repository methods use same SQL queries as before, no performance degradation expected

---

## 📚 References

### FIN Architectural Patterns:
- `.github/copilot-instructions.md` - Service Instantiation Pattern section
- `fin/context/ApplicationContext.java` - Existing DI implementation
- `fin/service/PayrollService.java` - Example of proper DI + repository usage
- `fin/repository/CompanyRepository.java` - Repository pattern example

### Related Documentation:
- `docs/SYSTEM_ARCHITECTURE_STATUS.md` - Dependency Injection pattern
- `docs/development/DATA_MANAGEMENT_FLOW_ANALYSIS.md` - Service architecture analysis

### Related Tasks:
- TASK 6.8: TransactionClassificationEngine Dependency Injection (similar pattern)
- TASK 6.4: AccountClassificationService Architectural Refactoring (related service)

---

## 🔄 Migration Path

### Step-by-Step Execution:

**Phase 1 (HIGH Priority - Do First):**
1. **Commit Point 1:** Update constructor signature (remove unused param, inject AccountClassificationService)
2. **Commit Point 2:** Add deprecated constructor for backward compatibility
3. **Commit Point 3:** Update ApplicationContext registration
4. **Commit Point 4:** Run full test suite, verify no regressions
5. **Commit Point 5:** Update documentation

**Phase 3 (MEDIUM Priority - Can be deferred to future sprint):**
6. **Commit Point 6:** Add repository methods (or verify they exist)
7. **Commit Point 7:** Replace first helper method with repository call
8. **Commit Point 8:** Replace remaining helper methods, remove `dbUrl` field
9. **Commit Point 9:** Update ApplicationContext to register repositories
10. **Commit Point 10:** Run full test suite, verify all tests pass

### Rollback Strategy:
- Each commit point is independent
- Can revert individual commits without breaking build
- Deprecated constructor ensures backward compatibility during transition
- Phase 3 (repository extraction) can be deferred without blocking Phase 1

---

## 📝 Implementation Checklist

### Phase 1: Dependency Injection (HIGH Priority)
- [ ] Update `TransactionClassificationService` constructor
  - [ ] Remove unused `ClassificationRuleManager` parameter
  - [ ] Inject `AccountClassificationService` instead of creating it
  - [ ] Add null validation
  - [ ] Update Javadoc
- [ ] Add deprecated backward-compatible constructor
- [ ] Update `ApplicationContext` registration
  - [ ] Pass injected `AccountClassificationService`
  - [ ] Remove `classificationRuleManager` parameter
  - [ ] Update comments
- [ ] Add unit tests for new constructor
- [ ] Verify build successful
- [ ] Update documentation

### Phase 3: Repository Pattern (MEDIUM Priority)
- [ ] Check if repository methods exist
  - [ ] `CompanyRepository.findById()`
  - [ ] `BankTransactionRepository.countUnclassified()`
  - [ ] `BankTransactionRepository.countClassified()`
  - [ ] `BankTransactionRepository.countClassifiedWithoutJournalEntries()`
  - [ ] `JournalEntryRepository.deleteExceptOpeningBalances()`
- [ ] Add missing repository methods
- [ ] Update `TransactionClassificationService` constructor to inject repositories
- [ ] Replace helper methods with repository calls
  - [ ] `deleteJournalEntriesExceptOpeningBalances()` → repository
  - [ ] `countClassifiedTransactions()` → repository
  - [ ] `getCompanyById()` → repository
  - [ ] `countUnclassifiedTransactions()` → repository
  - [ ] `countClassifiedWithoutJournalEntries()` → repository
- [ ] Remove `dbUrl` field
- [ ] Update `ApplicationContext` to register repositories
- [ ] Run full test suite
- [ ] Verify no direct JDBC in service
- [ ] Update documentation
- [ ] Mark task as COMPLETED

---

## 🎓 Learning Outcomes

This refactoring demonstrates:
- **Proper Dependency Injection:** All dependencies injected, no direct instantiation
- **Repository Pattern:** Separation of data access from business logic
- **Single Responsibility Principle:** Service orchestrates, repositories access data
- **Testability:** Easy to mock all dependencies
- **Backward Compatibility:** Using deprecation for gradual migration
- **ApplicationContext Pattern:** Centralized service and repository lifecycle management

---

## 📅 Timeline

- **Created:** 2025-11-02
- **Estimated Start (Phase 1):** TBD (after TASK 6.8 or current sprint)
- **Estimated Completion (Phase 1):** +3-4 hours from start
- **Estimated Start (Phase 3):** TBD (future sprint)
- **Estimated Completion (Phase 3):** +2-3 hours from Phase 3 start
- **Actual Start:** _Not started_
- **Actual Completion:** _Pending_

---

## ✅ Completion Notes

_To be filled upon completion_

---

**Next Steps After Completion:**
1. Apply same DI + repository pattern to other services if needed
2. Remove deprecated constructor in future major version (v2.0+)
3. Consider extracting more helper methods to repositories for consistency
4. Review other services for similar architectural improvements
