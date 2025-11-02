# TASK 6.6: InteractiveClassificationService Architectural Refactoring
**Status:** 🔴 NOT STARTED  
**Priority:** MEDIUM  
**Risk Level:** MEDIUM - Technical Debt & Architectural Consistency  
**Created:** 2025-11-02  
**Estimated Effort:** 15-22 hours (2-3 days)  
**Files Affected:** 3 files (1 major refactoring, 2 updates)

---

## 🎯 Objective

Refactor `InteractiveClassificationService` (2,066 lines) to align with FIN's modular UI architecture by:
1. **Injecting UI components** (OutputFormatter, InputHandler, ConsoleMenu) via constructor
2. **Replacing 194 System.out.println calls** with OutputFormatter methods
3. **Replacing manual Scanner creation** with InputHandler dependency injection
4. **Replacing inline menu logic** with ConsoleMenu pattern
5. **Extracting business logic** into separate ClassificationEngine service

**Goal:** Reduce service size from 2,066 lines to <500 lines per service through proper separation of concerns.

---

## 🔍 Root Cause Analysis

### Current Architectural Violations

#### **Violation #1: Zero-Argument Constructor (No Dependency Injection)**
```java
// ApplicationContext.java - Line 175 (CURRENT - WRONG)
InteractiveClassificationService interactiveClassificationService = new InteractiveClassificationService();
register(InteractiveClassificationService.class, interactiveClassificationService);

// InteractiveClassificationService.java - Line 259 (CURRENT - WRONG)
public InteractiveClassificationService() {
    this.dbUrl = DatabaseConfig.getDatabaseUrl();
    this.scanner = new Scanner(System.in, StandardCharsets.UTF_8);  // ❌ Manual Scanner creation
    this.accountClassificationService = new AccountClassificationService(dbUrl);
    // ... NO OutputFormatter, InputHandler, or ConsoleMenu injection
}
```

**Should Be:**
```java
// ApplicationContext.java (CORRECT)
InteractiveClassificationService interactiveClassificationService = new InteractiveClassificationService(
    initialDbUrl,
    get(OutputFormatter.class),
    get(InputHandler.class),
    get(ConsoleMenu.class),
    get(AccountClassificationService.class)
);
register(InteractiveClassificationService.class, interactiveClassificationService);

// InteractiveClassificationService.java (CORRECT)
public InteractiveClassificationService(
    String dbUrl,
    OutputFormatter outputFormatter,
    InputHandler inputHandler,
    ConsoleMenu consoleMenu,
    AccountClassificationService accountClassificationService
) {
    this.dbUrl = dbUrl;
    this.outputFormatter = outputFormatter;
    this.inputHandler = inputHandler;
    this.consoleMenu = consoleMenu;
    this.accountClassificationService = accountClassificationService;
    // NO manual Scanner creation
}
```

#### **Violation #2: 194 System.out.println Calls (Should Use OutputFormatter)**
**Examples Throughout Service:**
- Line 453: `System.out.println("\n" + "=".repeat(DISPLAY_LINE_WIDTH));`
- Line 454: `System.out.println("🔍 TRANSACTION CLASSIFICATION - " + company.getName());`
- Line 740-780: 40+ calls in `showAccountSuggestions()` method
- Line 1001-1009: Inline menu display
- Line 1190-1210: User input prompts

**Should Use OutputFormatter Methods:**
- `outputFormatter.printHeader("TRANSACTION CLASSIFICATION - " + company.getName())`
- `outputFormatter.printInfo(message)`
- `outputFormatter.printSuccess("✅ " + successMessage)`
- `outputFormatter.printError("❌ " + errorMessage)`
- `outputFormatter.printWarning("⚠️ " + warningMessage)`
- `outputFormatter.printSeparator()`

#### **Violation #3: Manual Scanner Usage (Should Use InputHandler)**
**Current Pattern (20+ occurrences):**
```java
String choice = scanner.nextLine().trim();  // ❌ Direct Scanner usage
```

**Should Be:**
```java
String choice = inputHandler.readLine().trim();  // ✅ InputHandler injection
```

#### **Violation #4: Inline Menu Logic (Should Use ConsoleMenu)**
**Current Pattern (Lines 1001-1010):**
```java
System.out.println("\n📝 Classification Options:");
System.out.println("1. Enter account code and name manually");
System.out.println("2. Skip this transaction");
System.out.println("3. Quit categorization");
System.out.print("Choose option (1-3): ");
```

**Should Use ConsoleMenu Pattern (Like PayrollController):**
```java
List<String> options = Arrays.asList(
    "Enter account code and name manually",
    "Skip this transaction",
    "Quit categorization"
);
String choice = consoleMenu.displayMenu("Classification Options", options);
```

---

## 📋 Implementation Plan

### **Phase 1: Constructor Refactoring & Dependency Injection** (2-3 hours)

#### Step 1.1: Update InteractiveClassificationService Constructor
```java
// Before (Line 259)
public InteractiveClassificationService() {
    this.dbUrl = DatabaseConfig.getDatabaseUrl();
    this.scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    this.accountClassificationService = new AccountClassificationService(dbUrl);
    // ...
}

// After
private final OutputFormatter outputFormatter;
private final InputHandler inputHandler;
private final ConsoleMenu consoleMenu;

public InteractiveClassificationService(
    String dbUrl,
    OutputFormatter outputFormatter,
    InputHandler inputHandler,
    ConsoleMenu consoleMenu,
    AccountClassificationService accountClassificationService
) {
    this.dbUrl = dbUrl;
    this.outputFormatter = outputFormatter;
    this.inputHandler = inputHandler;
    this.consoleMenu = consoleMenu;
    this.accountClassificationService = accountClassificationService;
    this.companyRules = new HashMap<>();
    this.changesMade = new ArrayList<>();
    this.accountCategories = new LinkedHashMap<>();
    
    initializeService();
}
```

#### Step 1.2: Remove Scanner Field
```java
// DELETE this field (Line 68)
private final Scanner scanner;
```

#### Step 1.3: Update ApplicationContext Registration (Line 175)
```java
// Before
InteractiveClassificationService interactiveClassificationService = new InteractiveClassificationService();

// After
InteractiveClassificationService interactiveClassificationService = new InteractiveClassificationService(
    initialDbUrl,
    get(OutputFormatter.class),
    get(InputHandler.class),
    get(ConsoleMenu.class),
    get(AccountClassificationService.class)
);
```

#### Step 1.4: Update TransactionClassificationService Constructor Calls
```java
// In TransactionClassificationService.java (Line 113)
// Before
this.interactiveService = new InteractiveClassificationService();

// After
this.interactiveService = new InteractiveClassificationService(
    initialDbUrl,
    outputFormatter,
    inputHandler,
    consoleMenu,
    accountClassificationService
);
```

**Testing:** Verify compilation, no runtime errors, services load correctly

---

### **Phase 2: Replace System.out.println with OutputFormatter** (4-6 hours)

**Strategy:** Work section-by-section, test incrementally

#### Section 1: Header and Separator Patterns (40 occurrences)
```java
// Before
System.out.println("\n" + "=".repeat(DISPLAY_LINE_WIDTH));
System.out.println("🔍 TRANSACTION CLASSIFICATION - " + company.getName());
System.out.println("=".repeat(DISPLAY_LINE_WIDTH));

// After
outputFormatter.printSeparator();
outputFormatter.printHeader("TRANSACTION CLASSIFICATION - " + company.getName());
outputFormatter.printSeparator();
```

#### Section 2: Success Messages (30 occurrences)
```java
// Before
System.out.println("✅ Transaction classified successfully");

// After
outputFormatter.printSuccess("Transaction classified successfully");
```

#### Section 3: Error Messages (35 occurrences)
```java
// Before
System.out.println("❌ Failed to update transaction classification");

// After
outputFormatter.printError("Failed to update transaction classification");
```

#### Section 4: Info Messages (50 occurrences)
```java
// Before
System.out.println("💡 ACCOUNT SUGGESTIONS:");

// After
outputFormatter.printInfo("💡 ACCOUNT SUGGESTIONS:");
```

#### Section 5: Warning Messages (20 occurrences)
```java
// Before
System.out.println("⚠️ No matching rules found");

// After
outputFormatter.printWarning("No matching rules found");
```

#### Section 6: Prompts and Interactive Messages (19 occurrences)
```java
// Before
System.out.print("Choose option (1-3): ");

// After
outputFormatter.print("Choose option (1-3): ");  // No newline
```

**Testing:** After each section, run application and verify output formatting

---

### **Phase 3: Replace Scanner with InputHandler** (2-3 hours)

**Pattern:** Replace all `scanner.nextLine()` calls with `inputHandler.readLine()`

#### Common Patterns to Replace (20+ occurrences)
```java
// Pattern 1: Simple input
// Before
String choice = scanner.nextLine().trim();
// After
String choice = inputHandler.readLine().trim();

// Pattern 2: Account code input
// Before
System.out.print("Account Code (e.g., 8100): ");
String accountCode = scanner.nextLine().trim();
// After
outputFormatter.print("Account Code (e.g., 8100): ");
String accountCode = inputHandler.readLine().trim();

// Pattern 3: Account name input
// Before
System.out.print("Account Name (e.g., Employee Costs): ");
String accountName = scanner.nextLine().trim();
// After
outputFormatter.print("Account Name (e.g., Employee Costs): ");
String accountName = inputHandler.readLine().trim();
```

**Files to Search:**
- `getUserClassificationInput()` method (Lines 1176-1214)
- `showMainMenu()` method (Lines 1000-1030)
- All interactive prompts throughout service

**Testing:** Verify all user input still works correctly

---

### **Phase 4: Replace Inline Menus with ConsoleMenu** (1-2 hours)

#### Example 1: Main Classification Menu (Lines 1001-1010)
```java
// Before
System.out.println("\n📝 Classification Options:");
System.out.println("1. Enter account code and name manually");
System.out.println("2. Skip this transaction");
System.out.println("3. Quit categorization");
System.out.print("Choose option (1-3): ");
String choice = scanner.nextLine().trim();

// After
List<String> options = Arrays.asList(
    "Enter account code and name manually",
    "Skip this transaction",
    "Quit categorization"
);
int choice = consoleMenu.displayMenu("Classification Options", options);
```

#### Example 2: Account Category Menu
```java
// Before (inline category display)
int categoryIndex = 1;
for (String category : accountCategories.keySet()) {
    System.out.println(categoryIndex + ". " + category);
    categoryIndex++;
}

// After
List<String> categories = new ArrayList<>(accountCategories.keySet());
int selectedCategory = consoleMenu.displayMenu("Account Categories", categories);
```

**Testing:** Verify menu navigation, selection handling

---

### **Phase 5: Extract Business Logic into ClassificationEngine Service** (6-8 hours)

**Goal:** Separate UI orchestration from classification algorithm logic

#### Step 5.1: Create ClassificationEngine Service
```java
package fin.service;

/**
 * Pure business logic for transaction classification
 * NO UI dependencies (OutputFormatter, InputHandler, ConsoleMenu)
 * 
 * Responsibilities:
 * - Pattern matching algorithms
 * - Rule evaluation and scoring
 * - Confidence calculation
 * - Suggestion generation
 */
public class ClassificationEngine {
    private final String dbUrl;
    private final AccountClassificationService accountClassificationService;
    private final Map<Long, Map<String, ClassificationRule>> companyRules;
    
    public ClassificationEngine(String dbUrl, AccountClassificationService accountClassificationService) {
        this.dbUrl = dbUrl;
        this.accountClassificationService = accountClassificationService;
        this.companyRules = new HashMap<>();
    }
    
    // Extract these methods from InteractiveClassificationService:
    // - findMatchingRules()
    // - calculateConfidence()
    // - generateSuggestions()
    // - evaluatePattern()
    // - scoreMatch()
    // - updateRuleUsage()
}
```

#### Step 5.2: Refactor InteractiveClassificationService as UI Orchestrator
```java
/**
 * UI Orchestrator for interactive transaction classification
 * Delegates business logic to ClassificationEngine
 * Handles user interaction via OutputFormatter, InputHandler, ConsoleMenu
 * 
 * Target Size: <500 lines (down from 2,066 lines)
 */
public class InteractiveClassificationService {
    private final ClassificationEngine classificationEngine;
    private final OutputFormatter outputFormatter;
    private final InputHandler inputHandler;
    private final ConsoleMenu consoleMenu;
    
    // Keep only UI orchestration methods:
    // - classifyTransactionsInteractively()
    // - getUserClassificationInput()
    // - displaySuggestions()
    // - confirmClassification()
    // - showSessionSummary()
}
```

#### Methods to Extract to ClassificationEngine (30+ methods)
1. `findMatchingRules()` - Pattern matching logic
2. `calculateConfidenceScore()` - Confidence calculation
3. `generateDatabaseSuggestions()` - Suggestion generation
4. `evaluatePattern()` - Pattern evaluation
5. `scorePatternMatch()` - Scoring algorithm
6. `updateRuleUsageCount()` - Rule statistics
7. `loadCompanyRules()` - Rule loading
8. `loadAccountCategories()` - Category loading
9. `createMappingRule()` - Rule creation logic
10. `insertNewRule()` - Database operations
11. `updateExistingRule()` - Database operations
12. ... (20+ more business logic methods)

**Testing:** Unit tests for ClassificationEngine, integration tests for UI flow

---

## ✅ Success Criteria

### Functional Requirements
- [ ] All 194 System.out.println calls replaced with OutputFormatter methods
- [ ] Zero direct Scanner usage (all via InputHandler)
- [ ] Zero inline menu logic (all via ConsoleMenu)
- [ ] Service constructor accepts 5 parameters (dbUrl, OutputFormatter, InputHandler, ConsoleMenu, AccountClassificationService)
- [ ] ApplicationContext properly injects all dependencies
- [ ] TransactionClassificationService properly instantiates with dependencies

### Architectural Requirements
- [ ] InteractiveClassificationService size reduced from 2,066 lines to <500 lines
- [ ] ClassificationEngine service created (<800 lines of pure business logic)
- [ ] Clear separation: UI orchestration vs business logic
- [ ] No UI components in ClassificationEngine
- [ ] All UI operations delegated to OutputFormatter/InputHandler/ConsoleMenu

### Testing Requirements
- [ ] All existing tests pass without modification
- [ ] New unit tests for ClassificationEngine (pure business logic)
- [ ] Integration tests verify UI flow with injected dependencies
- [ ] Manual testing confirms user experience unchanged

### Build & Quality Requirements
- [ ] `./gradlew compileJava --no-daemon` passes
- [ ] `./gradlew test` passes
- [ ] No new SpotBugs warnings
- [ ] No new Checkstyle violations
- [ ] Code coverage maintained or improved

---

## 🧪 Testing Strategy

### Unit Testing (ClassificationEngine)
```java
@Test
void testPatternMatching() {
    ClassificationEngine engine = new ClassificationEngine(dbUrl, mockAccountService);
    String pattern = "XG SALARIES";
    List<ClassificationRule> rules = engine.findMatchingRules(companyId, pattern);
    
    assertFalse(rules.isEmpty());
    assertEquals("8100", rules.get(0).getAccountCode());
}

@Test
void testConfidenceCalculation() {
    ClassificationEngine engine = new ClassificationEngine(dbUrl, mockAccountService);
    double confidence = engine.calculateConfidence("EXACT MATCH", "EXACT MATCH");
    
    assertEquals(1.0, confidence, 0.01);  // 100% confidence for exact match
}
```

### Integration Testing (InteractiveClassificationService)
```java
@Test
void testInteractiveClassification() {
    // Mock UI components
    OutputFormatter mockOutput = mock(OutputFormatter.class);
    InputHandler mockInput = mock(InputHandler.class);
    ConsoleMenu mockMenu = mock(ConsoleMenu.class);
    
    // Setup input sequence
    when(mockInput.readLine()).thenReturn("8100", "Employee Costs");
    when(mockMenu.displayMenu(anyString(), anyList())).thenReturn(1);
    
    InteractiveClassificationService service = new InteractiveClassificationService(
        dbUrl, mockOutput, mockInput, mockMenu, accountClassificationService
    );
    
    // Test classification flow
    ClassificationResult result = service.classifyTransaction(transaction, companyId);
    
    assertNotNull(result);
    assertEquals("8100", result.getAccountCode());
    verify(mockOutput).printHeader(anyString());
    verify(mockInput, atLeastOnce()).readLine();
}
```

### Manual Testing Checklist
- [ ] Launch application via `./run.sh`
- [ ] Select "Data Management" → "Transaction Classification"
- [ ] Verify header displays correctly (OutputFormatter)
- [ ] Verify suggestions display correctly (OutputFormatter)
- [ ] Verify menu displays correctly (ConsoleMenu)
- [ ] Verify input prompts work (InputHandler)
- [ ] Classify 5-10 transactions successfully
- [ ] Verify session summary displays correctly
- [ ] Compare output with previous version (should be identical)

---

## 📚 Reference Materials

### Similar Refactoring Examples
1. **PayrollController.java** - Uses OutputFormatter, InputHandler, ConsoleMenu correctly
2. **CompanyService.java** - Consistent named constants pattern (refactored Oct 2025)
3. **ExcelFinancialReportService.java** - Proper Logger usage instead of System.out/err

### Architecture Documentation
- `/docs/development/CLEAN_APPLICATION_GUIDE.md` - Modular UI architecture
- `/.github/copilot-instructions.md` - Dependency injection patterns
- `/docs/development/tasks/TASK_6.4_AccountClassificationService_Architectural_Refactoring.md` - Similar refactoring pattern

### Code Patterns to Follow
```java
// Constructor pattern
public ServiceClass(
    String dbUrl,
    OutputFormatter outputFormatter,
    InputHandler inputHandler,
    ConsoleMenu consoleMenu
) {
    this.dbUrl = dbUrl;
    this.outputFormatter = outputFormatter;
    this.inputHandler = inputHandler;
    this.consoleMenu = consoleMenu;
}

// Output pattern
outputFormatter.printHeader("SECTION TITLE");
outputFormatter.printInfo("Information message");
outputFormatter.printSuccess("Success message");
outputFormatter.printError("Error message");
outputFormatter.printWarning("Warning message");
outputFormatter.printSeparator();

// Input pattern
String input = inputHandler.readLine().trim();
int number = inputHandler.readInt();
BigDecimal amount = inputHandler.readBigDecimal();

// Menu pattern
List<String> options = Arrays.asList("Option 1", "Option 2", "Option 3");
int choice = consoleMenu.displayMenu("Menu Title", options);
```

---

## ⚠️ Risks & Mitigation

### Risk 1: Breaking Existing Functionality
**Probability:** MEDIUM  
**Impact:** HIGH  
**Mitigation:**
- Incremental refactoring (phase by phase)
- Test after each phase
- Keep backup of original file
- Manual testing before commit

### Risk 2: Test Failures
**Probability:** MEDIUM  
**Impact:** MEDIUM  
**Mitigation:**
- Update test mocks for new constructor
- Add tests for ClassificationEngine
- Run full test suite after each phase

### Risk 3: User Experience Changes
**Probability:** LOW  
**Impact:** HIGH  
**Mitigation:**
- OutputFormatter should maintain identical output format
- InputHandler should behave identically to Scanner
- ConsoleMenu should provide same options
- Manual testing by user before approval

### Risk 4: Performance Degradation
**Probability:** LOW  
**Impact:** MEDIUM  
**Mitigation:**
- OutputFormatter/InputHandler have minimal overhead
- ClassificationEngine logic unchanged (just extracted)
- Performance testing if concerns arise

---

## 📝 Implementation Notes

### Code Quality Improvements
- **Before:** 2,066 lines mixing UI and business logic
- **After:** <500 lines UI orchestration + <800 lines business logic (ClassificationEngine)
- **Maintainability:** Clear separation of concerns, testable business logic
- **Consistency:** Aligns with PayrollController, CompanyController patterns

### Architectural Benefits
1. **Testability:** ClassificationEngine can be unit tested without UI dependencies
2. **Reusability:** ClassificationEngine can be used by API endpoints (future)
3. **Maintainability:** UI changes don't affect business logic
4. **Consistency:** All services use OutputFormatter/InputHandler/ConsoleMenu
5. **Extensibility:** Easy to add new classification strategies

### Dependencies Updated
- `ApplicationContext.java` (Line 175) - Constructor call
- `TransactionClassificationService.java` (Line 113) - Constructor call
- `InteractiveClassificationService.java` (All 2,066 lines refactored)
- New file: `ClassificationEngine.java` (800 lines extracted)

---

## 🚀 Deployment Checklist

### Pre-Deployment
- [ ] All 5 phases completed
- [ ] Unit tests pass (ClassificationEngine)
- [ ] Integration tests pass (InteractiveClassificationService)
- [ ] Manual testing confirms identical user experience
- [ ] Build successful (`./gradlew clean build`)
- [ ] No SpotBugs warnings
- [ ] No Checkstyle violations

### Deployment
- [ ] Create feature branch: `feature/task-6.6-interactive-classification-refactoring`
- [ ] Commit Phase 1 changes (constructor refactoring)
- [ ] Commit Phase 2 changes (OutputFormatter replacement)
- [ ] Commit Phase 3 changes (InputHandler replacement)
- [ ] Commit Phase 4 changes (ConsoleMenu replacement)
- [ ] Commit Phase 5 changes (ClassificationEngine extraction)
- [ ] User testing and approval
- [ ] Merge to main branch

### Post-Deployment Verification
- [ ] Application runs successfully
- [ ] Transaction classification works correctly
- [ ] Session summary displays correctly
- [ ] No runtime errors
- [ ] Performance acceptable

---

## 📊 Progress Tracking

**Created:** 2025-11-02  
**Status:** 🔴 NOT STARTED  
**Estimated Completion:** TBD (2-3 days focused work)

### Phase Completion Status
- [ ] **Phase 1:** Constructor Refactoring & DI (0/4 steps)
- [ ] **Phase 2:** Replace System.out.println (0/6 sections)
- [ ] **Phase 3:** Replace Scanner with InputHandler (0/3 patterns)
- [ ] **Phase 4:** Replace Inline Menus (0/2 examples)
- [ ] **Phase 5:** Extract ClassificationEngine (0/2 steps)

**Overall Progress:** 0% (0/17 total steps)

---

## 🔗 Related Tasks
- **TASK 6.4:** AccountClassificationService Architectural Refactoring (similar pattern)
- **TASK 6.5:** BudgetReportService OutputFormatter Refactoring (OutputFormatter usage)
- **TASK 5.4:** Holistic Checkstyle Cleanup (code quality improvements)

---

**Last Updated:** 2025-11-02  
**Next Review:** After Phase 1 completion
