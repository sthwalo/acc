# FIN Copilot Instructions
Of course. Here is the bulleted summary with all commands included.

### **Strict Development & Documentation Protocol **

*   **Execution Protocol:**
    *   **Inventory First:** Before touching a file, run a full violation scan:
        ```bash
        ./gradlew clean checkstyleMain --no-daemon 2>&1 | grep -E "(MethodLength|MagicNumber|HiddenField|NeedBraces|AvoidStarImport|OperatorWrap|NewlineAtEndOfFile|DesignForExtension)" | sort > violations_inventory.txt
        ```
    *   **File Focus:** Work on **ONE file at a time**. Fix **ALL violations** in it before moving on.
    *   **Build Verification:** After every change, run a full build to verify stability:
    
        ```bash
        ./gradlew clean build

        ```

*   **Priority Order:** Start with core business logic (services, repositories), then controllers/utilities, and finally main classes/CLI tools.

*   **Documentation Mandate:** All work **MUST** be documented in the `/docs/development/tasks/` directory using the `TASK_[NUMBER]_[Name].md` format.

*   **Critical Pre-Creation Check:** **ABSOLUTELY FORBIDDEN** to create a new task file without first checking for existing work. You **MUST** run these checks:
    ```bash
    # 1. Directory Scan
    ls -la /docs/development/tasks/
    # 2. Content Search (replace [keywords] with your topic)
    grep -r "TASK.*" /docs/development/tasks/ | grep -i "[relevant keywords]"
    # 3. README Review
    cat /docs/development/tasks/README.md
    ```

*   **Correct Approach:** Update an existing task file if the work fits; only create a new one for genuinely new work.

*   **Template:** All tasks must use the mandated markdown template with status, priority, and implementation details.

*   **Rationale:** This prevents violation cascades, duplicate effort, and inconsistent quality, which is critical for a system processing real financial data (7,156+ transactions).

- **Architecture**: Single Gradle module under `app` with layered packages (`controller`, `service`, `repository`, `ui`). `ApplicationContext` wires everything; always register new services/controllers there and use the "secure constructor" pattern (finish dependency validation before assigning fields).
- **Runtime modes**: `ConsoleApplication`, `ApiApplication`, and batch via `BatchProcessor` share the same context. Console controllers drive menus in `ui/*`; SparkJava routes live in `api/ApiServer.java` and must be added through its `setup*` helpers to keep logging and error handling consistent.
- **Dependency graph**: Services pull JDBC via `DatabaseConfig` or prebuilt repositories (e.g. `JdbcFinancialDataRepository`). When introducing new data access code, prefer repositories and reuse the Hikari-backed instances created in `ApplicationContext.initializeFinancialServices`.
- **Database configuration**: `.env` (or system vars) must define `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`. Tests pick up `TEST_DATABASE_*`; you can point to a disposable Postgres by exporting them before `./gradlew test`. Never commit credentials—`DatabaseConfig` logs placeholders, so keep output clean in tests.
- **Licensing guard**: `LicenseManager.checkLicenseCompliance()` runs on every entry point. For automated flows/tasks set `-Dfin.license.autoconfirm=true` (already done in Gradle tasks); if you add a new launcher remember to propagate the flag.
- **Key commands**: `./gradlew clean build`, `./gradlew test`, `./gradlew run --args="api"`, and scripts like `./start-backend.sh`, `./test-api.sh`, `./start-fullstack.sh`. CLI smoke tests live in `scripts/test_single_pdf.sh` etc.; keep them in sync when changing transaction pipelines.
- **Transaction pipeline**: Parsing lives under `service/parser`, classification under `TransactionClassificationService` (2k+ lines). Follow existing helpers instead of duplicating regex/keyword logic, and update `ClassificationRuleManager` when adding rule sources.
- **Reporting stack**: General ledger, trial balance, income statement, and balance sheet services depend on `FinancialDataRepository`. If you extend reporting, fetch data through that repository to keep caching and pooling behaviour aligned.
- **Payroll & SDL**: Payroll flows hinge on `PayrollService`, `PayrollReportService`, and PDF helpers in `service`. SDL logic ties back to `docs/SDL_IMPLEMENTATION_2025-10-06.md`; reuse existing deduction calculators and keep percentages configurable in one place.
- **API patterns**: `ApiServer` centralises JSON responses with `Gson`. Use its status constants, wrap results in `{success,data}` envelopes, and add validation before touching services to avoid leaking SQL errors.
- **Console UX**: `ApplicationController` orchestrates menus. Inject `OutputFormatter` and `InputHandler` rather than calling `System.out`. New flows should live in dedicated controllers so they can be registered and toggled from the menu definitions.
- **Batch automation**: Commands like `./scripts/process_all_statements.sh` expect PDFs in `input/`. When altering file formats, update both the parser and these scripts so operational docs remain accurate.
- **Testing**: Unit tests in `app/src/test/java` use JUnit 5 and Mockito; integration tests rely on real PDFs and Postgres. Use `@Disabled` sparingly—prefer environment guards that check for required fixtures.
- **Quality gates**: Checkstyle (10.12.4) and SpotBugs (ignoreFailures=true pending remediation). Follow the documentation in `docs/development/tasks/` when touching files tagged with open tasks, and retain defensive copying patterns that resolved EI_EXPOSE_REP warnings.
- **Documentation**: Major runbooks live in `docs/` (`QUICK_TEST_GUIDE.md`, `DEVELOPMENT/QUICK_START.md`, `SYSTEM_ARCHITECTURE_STATUS.md`). Update the relevant guide whenever behaviour or command surfaces change; archives are frozen—create new dated docs instead of editing old ones.
- **Large refactors**: `AccountClassificationService`, `InteractiveClassificationService`, and `BudgetReportService` are flagged for decomposition (see TASK 6.x files). Keep changes scoped and note follow-up work inside matching task docs.
