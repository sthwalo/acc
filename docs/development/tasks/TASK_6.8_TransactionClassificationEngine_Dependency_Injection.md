# TASK 6.8: TransactionClassificationEngine Dependency Injection Refactoring

**Status:** 📋 PLANNED  
**Priority:** MEDIUM  
**Category:** Architectural Improvement  
**Estimated Effort:** 2-3 hours  
**Created:** 2025-11-02  

---

## 🎯 Objective

Refactor `TransactionClassificationEngine` to follow FIN's established architectural patterns by implementing proper dependency injection, registering in ApplicationContext, and improving testability while maintaining backward compatibility.

---

## 📋 Current State Analysis

### Existing Implementation Issues:

1. **❌ Direct Service Instantiation (Tight Coupling)**
   ```java
   public TransactionClassificationEngine() {
       this.dbUrl = DatabaseConfig.getDatabaseUrl();
       this.accountClassificationService = new AccountClassificationService(dbUrl);  // Tight coupling
   }
   ```
   - Creates `AccountClassificationService` directly in constructor
   - Violates FIN's ApplicationContext pattern
   - Hard to test (can't mock dependencies)

2. **❌ Not Registered in ApplicationContext**
   - Service not managed by centralized ApplicationContext
   - Used directly with manual instantiation in `ClassificationUIHandler`
   - Inconsistent with other FIN services (CompanyService, PayrollService, etc.)

3. **⚠️ Direct Database Access**
   ```java
   public List<BankTransaction> findSimilarUnclassifiedTransactions(...) {
       try (Connection conn = DriverManager.getConnection(dbUrl);  // Direct JDBC
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
   ```
   - Mixing classification logic with data access
   - Should delegate to BankTransactionRepository
   - Violates Single Responsibility Principle

4. **⚠️ ClassificationUIHandler Creates Engine Manually**
   ```java
   // Current usage in ClassificationUIHandler
   TransactionClassificationEngine engine = new TransactionClassificationEngine();
   ```

---

## 🔧 Implementation Plan

### Phase 1: Dependency Injection Refactoring (Priority: HIGH)

#### Step 1.1: Update TransactionClassificationEngine Constructor
**File:** `app/src/main/java/fin/service/TransactionClassificationEngine.java`

**Before:**
```java
public TransactionClassificationEngine() {
    this.dbUrl = DatabaseConfig.getDatabaseUrl();
    this.accountClassificationService = new AccountClassificationService(dbUrl);
}
```

**After:**
```java
// Constructor with dependency injection
public TransactionClassificationEngine(AccountClassificationService accountClassificationService) {
    if (accountClassificationService == null) {
        throw new IllegalArgumentException("AccountClassificationService is required");
    }
    this.accountClassificationService = accountClassificationService;
}

// Legacy constructor for backward compatibility (deprecated)
@Deprecated
public TransactionClassificationEngine() {
    this(new AccountClassificationService(DatabaseConfig.getDatabaseUrl()));
}
```

**Changes:**
- Add primary constructor accepting `AccountClassificationService`
- Keep no-arg constructor for backward compatibility (deprecated)
- Remove `dbUrl` field (no longer needed for service instantiation)
- Add null validation for dependency

#### Step 1.2: Update Field Declarations
**Remove:**
```java
private final String dbUrl;
```

**Reasoning:** No longer needed since AccountClassificationService is injected

---

### Phase 2: ApplicationContext Registration (Priority: HIGH)

#### Step 2.1: Register in ApplicationContext
**File:** `app/src/main/java/fin/context/ApplicationContext.java`

**Add to initialization:**
```java
public void initialize() {
    // ... existing service registrations
    
    // Register AccountClassificationService first (dependency)
    AccountClassificationService accountClassificationService = 
        new AccountClassificationService(dbUrl);
    services.put(AccountClassificationService.class, accountClassificationService);
    
    // Register TransactionClassificationEngine with injected dependency
    TransactionClassificationEngine classificationEngine = 
        new TransactionClassificationEngine(accountClassificationService);
    services.put(TransactionClassificationEngine.class, classificationEngine);
    
    // ... other services
}
```

**Note:** Ensure `AccountClassificationService` is registered before `TransactionClassificationEngine`

---

### Phase 3: Update Consumers (Priority: HIGH)

#### Step 3.1: Update ClassificationUIHandler
**File:** `app/src/main/java/fin/service/ClassificationUIHandler.java`

**Before:**
```java
// Direct instantiation
TransactionClassificationEngine engine = new TransactionClassificationEngine();
ClassificationUIHandler handler = new ClassificationUIHandler(engine, ruleManager);
```

**After:**
```java
// Get from ApplicationContext
ApplicationContext context = new ApplicationContext();
TransactionClassificationEngine engine = context.get(TransactionClassificationEngine.class);
ClassificationUIHandler handler = new ClassificationUIHandler(engine, ruleManager);
```

**Search for all instantiations:**
```bash
grep -r "new TransactionClassificationEngine" app/src/
```

#### Step 3.2: Update Any Other Consumers
- Search codebase for all usages
- Update to use ApplicationContext pattern
- Add comments explaining architectural pattern

---

### Phase 4: Extract Repository (Priority: LOW - Future Enhancement)

#### Step 4.1: Create BankTransactionRepository Method
**File:** `app/src/main/java/fin/repository/BankTransactionRepository.java` (if exists) or create new

**Add method:**
```java
public List<BankTransaction> findSimilarUnclassified(
        BankTransaction referenceTransaction, 
        Long companyId, 
        int maxResults) {
    
    String sql = """
        SELECT bt.*
        FROM bank_transactions bt
        WHERE bt.company_id = ? 
        AND bt.account_code IS NULL
        AND bt.id != ?
        AND (
            LOWER(bt.details) LIKE ? OR
            LOWER(bt.details) LIKE ? OR
            LOWER(bt.details) LIKE ?
        )
        ORDER BY bt.transaction_date DESC
        LIMIT ?
        """;
    
    // Implementation with keyword extraction
    // Move from TransactionClassificationEngine.findSimilarUnclassifiedTransactions()
}
```

#### Step 4.2: Update TransactionClassificationEngine
**Inject BankTransactionRepository:**
```java
private final BankTransactionRepository transactionRepository;

public TransactionClassificationEngine(
        AccountClassificationService accountClassificationService,
        BankTransactionRepository transactionRepository) {
    this.accountClassificationService = accountClassificationService;
    this.transactionRepository = transactionRepository;
}

public List<BankTransaction> findSimilarUnclassifiedTransactions(...) {
    return transactionRepository.findSimilarUnclassified(transaction, companyId, maxResults);
}
```

**Note:** This is OPTIONAL for now, can be deferred to future cleanup

---

## 🧪 Testing Strategy

### Unit Tests to Add/Update

#### Test 1: Constructor Dependency Injection
```java
@Test
void testConstructorWithDependencyInjection() {
    AccountClassificationService mockService = mock(AccountClassificationService.class);
    TransactionClassificationEngine engine = 
        new TransactionClassificationEngine(mockService);
    
    assertNotNull(engine);
}

@Test
void testConstructorRejectsNullDependency() {
    assertThrows(IllegalArgumentException.class, () -> {
        new TransactionClassificationEngine(null);
    });
}
```

#### Test 2: ApplicationContext Registration
```java
@Test
void testEngineRegisteredInApplicationContext() {
    ApplicationContext context = new ApplicationContext();
    TransactionClassificationEngine engine = 
        context.get(TransactionClassificationEngine.class);
    
    assertNotNull(engine);
}
```

#### Test 3: Backward Compatibility
```java
@Test
@SuppressWarnings("deprecation")
void testLegacyNoArgConstructor() {
    // Ensure deprecated constructor still works for backward compatibility
    TransactionClassificationEngine engine = new TransactionClassificationEngine();
    assertNotNull(engine);
}
```

### Integration Tests

#### Test 4: End-to-End Classification with ApplicationContext
```java
@Test
void testClassificationWithApplicationContext() {
    ApplicationContext context = new ApplicationContext();
    TransactionClassificationEngine engine = 
        context.get(TransactionClassificationEngine.class);
    
    BankTransaction transaction = createTestTransaction();
    ClassificationResult result = engine.classifyTransaction(transaction, 1L);
    
    assertNotNull(result);
}
```

---

## ✅ Success Criteria

### Phase 1 (Dependency Injection):
- ✅ Constructor accepts `AccountClassificationService` parameter
- ✅ Null validation added for dependency
- ✅ Deprecated no-arg constructor for backward compatibility
- ✅ `dbUrl` field removed
- ✅ All unit tests pass

### Phase 2 (ApplicationContext):
- ✅ `TransactionClassificationEngine` registered in ApplicationContext
- ✅ `AccountClassificationService` registered before engine
- ✅ Can retrieve engine via `context.get(TransactionClassificationEngine.class)`
- ✅ Integration tests pass

### Phase 3 (Consumer Updates):
- ✅ All direct instantiations updated to use ApplicationContext
- ✅ `ClassificationUIHandler` uses ApplicationContext pattern
- ✅ No regressions in classification functionality
- ✅ Build successful with no errors

### Phase 4 (Repository - Optional):
- ⏸️ Deferred to future task (low priority)
- Would require BankTransactionRepository enhancement

---

## 📊 Impact Assessment

### Files to Modify:

**High Priority (Must Do):**
1. `app/src/main/java/fin/service/TransactionClassificationEngine.java` (constructor refactoring)
2. `app/src/main/java/fin/context/ApplicationContext.java` (service registration)
3. `app/src/main/java/fin/service/ClassificationUIHandler.java` (update instantiation)
4. Unit test files (add new tests for DI)

**Low Priority (Optional):**
5. `app/src/main/java/fin/repository/BankTransactionRepository.java` (extract data access)

### Estimated Lines Changed:
- **TransactionClassificationEngine.java:** ~15 lines modified, 5 added
- **ApplicationContext.java:** ~10 lines added
- **ClassificationUIHandler.java:** ~5 lines modified
- **Tests:** ~50 lines added (new test cases)

**Total:** ~85 lines changed

---

## 🚧 Risks & Mitigation

### Risk 1: Breaking Existing Consumers
**Mitigation:** Keep deprecated no-arg constructor for backward compatibility

### Risk 2: ApplicationContext Initialization Order
**Mitigation:** Register `AccountClassificationService` before `TransactionClassificationEngine`

### Risk 3: Test Coverage Gaps
**Mitigation:** Add comprehensive unit and integration tests before refactoring

### Risk 4: Circular Dependencies
**Mitigation:** Ensure clean dependency graph (Engine depends on AccountService, not vice versa)

---

## 📚 References

### FIN Architectural Patterns:
- `.github/copilot-instructions.md` - Service Instantiation Pattern section
- `fin/context/ApplicationContext.java` - Existing DI implementation
- `fin/service/PayrollService.java` - Example of proper DI usage

### Related Documentation:
- `docs/SYSTEM_ARCHITECTURE_STATUS.md` - Dependency Injection pattern
- `docs/development/MODEL_AND_SERVICE_REDUNDANCY_ANALYSIS.md` - Service coupling analysis

### Related Tasks:
- TASK 6.4: AccountClassificationService Architectural Refactoring (related service)
- TASK 5.x: Checkstyle cleanup tasks (code quality improvements)

---

## 🔄 Migration Path

### Step-by-Step Execution:

1. **Commit Point 1:** Add new constructor with DI (keep deprecated constructor)
2. **Commit Point 2:** Register in ApplicationContext
3. **Commit Point 3:** Update ClassificationUIHandler to use ApplicationContext
4. **Commit Point 4:** Run full test suite, verify no regressions
5. **Commit Point 5:** Update documentation

### Rollback Strategy:
- Each commit point is independent
- Can revert individual commits without breaking build
- Deprecated constructor ensures backward compatibility during transition

---

## 📝 Implementation Checklist

### Phase 1: Dependency Injection
- [ ] Update `TransactionClassificationEngine` constructor
- [ ] Add null validation for injected dependency
- [ ] Add deprecated no-arg constructor
- [ ] Remove `dbUrl` field
- [ ] Add unit tests for new constructor
- [ ] Verify build successful

### Phase 2: ApplicationContext Registration
- [ ] Register `AccountClassificationService` in ApplicationContext
- [ ] Register `TransactionClassificationEngine` after AccountService
- [ ] Add integration test for ApplicationContext retrieval
- [ ] Verify service initialization order
- [ ] Verify build successful

### Phase 3: Consumer Updates
- [ ] Search for all `new TransactionClassificationEngine()` usages
- [ ] Update `ClassificationUIHandler` to use ApplicationContext
- [ ] Update any other consumers found
- [ ] Add comments explaining architectural pattern
- [ ] Run full test suite
- [ ] Verify classification functionality works end-to-end
- [ ] Verify build successful

### Phase 4: Documentation
- [ ] Update `.github/copilot-instructions.md` if needed
- [ ] Update SYSTEM_ARCHITECTURE_STATUS.md
- [ ] Update this task document with completion notes
- [ ] Mark task as COMPLETED

### Phase 5: Repository Extraction (OPTIONAL - Future Task)
- [ ] Create separate TASK for repository refactoring
- [ ] Move to backlog for future sprint

---

## 🎓 Learning Outcomes

This refactoring demonstrates:
- **Dependency Injection:** Reducing tight coupling between services
- **ApplicationContext Pattern:** Centralized service lifecycle management
- **Backward Compatibility:** Using deprecation to support gradual migration
- **Single Responsibility:** Separating concerns (classification vs data access)
- **Testability:** Making services easier to mock and test

---

## 📅 Timeline

- **Created:** 2025-11-02
- **Estimated Start:** TBD (after current checkstyle cleanup)
- **Estimated Completion:** +2-3 hours from start
- **Actual Start:** _Not started_
- **Actual Completion:** _Pending_

---

## ✅ Completion Notes

_To be filled upon completion_

---

**Next Steps After Completion:**
1. Consider extracting repository pattern (TASK 6.9 or later)
2. Apply same DI pattern to other services if needed
3. Remove deprecated constructor in future major version (v2.0+)
