package fin.controller;

import fin.model.Budget;
import fin.model.StrategicPlan;
import fin.model.StrategicPriority;
import fin.service.BudgetService;
import fin.service.StrategicPlanningService;
import fin.service.BudgetReportService;
import fin.state.ApplicationState;
import fin.ui.ConsoleMenu;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

/**
 * Budget and Strategic Planning management controller
 * Handles budget creation, strategic planning, and financial projections for companies
 */
public class BudgetController {
    private final BudgetService budgetService;
    private final StrategicPlanningService strategicPlanningService;
    private final BudgetReportService budgetReportService;
    private final ApplicationState applicationState;
    private final ConsoleMenu menu;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;

    // Menu option constants
    private static final int MENU_OPTION_STRATEGIC_PLANNING = 1;
    private static final int MENU_OPTION_BUDGET_MANAGEMENT = 2;
    private static final int MENU_OPTION_BUDGET_REPORTS = 3;
    private static final int MENU_OPTION_BACK_TO_MAIN = 4;
    private static final int BUDGET_MENU_MAX_OPTION = 4;

    // Strategic planning menu options
    private static final int STRATEGIC_OPTION_CREATE_PLAN = 1;
    private static final int STRATEGIC_OPTION_VIEW_PLANS = 2;
    private static final int STRATEGIC_OPTION_EDIT_PLAN = 3;
    private static final int STRATEGIC_OPTION_DELETE_PLAN = 4;
    private static final int STRATEGIC_OPTION_BACK = 5;
    private static final int STRATEGIC_MENU_MAX_OPTION = 5;

    // Budget management menu options
    private static final int BUDGET_MGMT_OPTION_CREATE_BUDGET = 1;
    private static final int BUDGET_MGMT_OPTION_VIEW_BUDGETS = 2;
    private static final int BUDGET_MGMT_OPTION_EDIT_BUDGET = 3;
    private static final int BUDGET_MGMT_OPTION_DELETE_BUDGET = 4;
    private static final int BUDGET_MGMT_OPTION_BACK = 5;
    private static final int BUDGET_MGMT_MENU_MAX_OPTION = 5;

    // Budget reports menu options
    private static final int BUDGET_REPORT_OPTION_BUDGET_SUMMARY = 1;
    private static final int BUDGET_REPORT_OPTION_BUDGET_VS_ACTUAL = 2;
    private static final int BUDGET_REPORT_OPTION_STRATEGIC_PLAN_REPORT = 3;
    private static final int BUDGET_REPORT_OPTION_PRINT_PDF_REPORTS = 4;
    private static final int BUDGET_REPORT_OPTION_BACK = 5;
    private static final int BUDGET_REPORT_MENU_MAX_OPTION = 5;

    // PDF reports menu options
    private static final int PDF_REPORT_OPTION_BUDGET_SUMMARY_PDF = 1;
    private static final int PDF_REPORT_OPTION_BUDGET_VS_ACTUAL_PDF = 2;
    private static final int PDF_REPORT_OPTION_STRATEGIC_PLAN_PDF = 3;
    private static final int PDF_REPORT_OPTION_BACK = 4;
    private static final int PDF_REPORT_MENU_MAX_OPTION = 4;

    /**
     * Constructor with dependency injection.
     *
     * @param budgetService the service for budget management operations
     * @param strategicPlanningService the service for strategic planning operations
     * @param budgetReportService the service for PDF report generation
     * @param applicationState the application state manager
     * @param menu the console menu for user interaction
     * @param inputHandler the input handler for user input
     * @param outputFormatter the output formatter for display formatting
     */
    public BudgetController(BudgetService initialBudgetService,
                          StrategicPlanningService initialStrategicPlanningService,
                          BudgetReportService initialBudgetReportService,
                          ApplicationState initialApplicationState,
                          ConsoleMenu initialMenu,
                          InputHandler initialInputHandler,
                          OutputFormatter initialOutputFormatter) {
        this.budgetService = initialBudgetService;
        this.strategicPlanningService = initialStrategicPlanningService;
        this.budgetReportService = initialBudgetReportService;
        this.applicationState = initialApplicationState;
        this.menu = initialMenu;
        this.inputHandler = initialInputHandler;
        this.outputFormatter = initialOutputFormatter;
    }

    /**
     * Main budget management handler
     */
    public void handleBudgetManagement() {
        if (!applicationState.hasCurrentCompany()) {
            outputFormatter.printError("No company selected. Please select a company first.");
            return;
        }

        boolean back = false;
        while (!back) {
            displayBudgetMenu();
            int choice = inputHandler.getInteger("Enter your choice", 1, BUDGET_MENU_MAX_OPTION);

            switch (choice) {
                case MENU_OPTION_STRATEGIC_PLANNING:
                    handleStrategicPlanning();
                    break;
                case MENU_OPTION_BUDGET_MANAGEMENT:
                    handleBudgetManagementMenu();
                    break;
                case MENU_OPTION_BUDGET_REPORTS:
                    handleBudgetReports();
                    break;
                case MENU_OPTION_BACK_TO_MAIN:
                    back = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Display the main budget menu
     */
    private void displayBudgetMenu() {
        outputFormatter.printHeader("Budget & Strategic Planning");
        System.out.println("Company: " + applicationState.getCurrentCompany().getName());
        System.out.println();
        System.out.println("1. Strategic Planning");
        System.out.println("2. Budget Management");
        System.out.println("3. Budget Reports");
        System.out.println("4. Back to Main Menu");
        System.out.print("Enter your choice (1-4): ");
    }

    /**
     * Handle strategic planning operations
     */
    private void handleStrategicPlanning() {
        boolean back = false;
        while (!back) {
            displayStrategicPlanningMenu();
            int choice = inputHandler.getInteger("Enter your choice", 1, STRATEGIC_MENU_MAX_OPTION);

            switch (choice) {
                case STRATEGIC_OPTION_CREATE_PLAN:
                    createStrategicPlan();
                    break;
                case STRATEGIC_OPTION_VIEW_PLANS:
                    viewStrategicPlans();
                    break;
                case STRATEGIC_OPTION_EDIT_PLAN:
                    editStrategicPlan();
                    break;
                case STRATEGIC_OPTION_DELETE_PLAN:
                    deleteStrategicPlan();
                    break;
                case STRATEGIC_OPTION_BACK:
                    back = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Display strategic planning menu
     */
    private void displayStrategicPlanningMenu() {
        outputFormatter.printSubHeader("Strategic Planning");
        System.out.println("1. Create Strategic Plan");
        System.out.println("2. View Strategic Plans");
        System.out.println("3. Edit Strategic Plan");
        System.out.println("4. Delete Strategic Plan");
        System.out.println("5. Back");
        System.out.print("Enter your choice (1-5): ");
    }

    /**
     * Create a new strategic plan for the current company
     */
    private void createStrategicPlan() {
        outputFormatter.printHeader("Create Strategic Plan");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();

            // Check if company already has a strategic plan
            List<StrategicPlan> existingPlans = strategicPlanningService.getStrategicPlansForCompany(companyId);
            if (!existingPlans.isEmpty()) {
                outputFormatter.printWarning("This company already has strategic plans. Consider editing existing plans instead.");
                if (!inputHandler.getBoolean("Continue creating a new strategic plan?")) {
                    return;
                }
            }

            // Get plan details
            String title = inputHandler.getString("Enter plan title");
            String vision = inputHandler.getString("Enter vision statement");
            String mission = inputHandler.getString("Enter mission statement");

            // Use sample creation method
            StrategicPlan plan = strategicPlanningService.createSampleStrategicPlan(companyId, title, vision, mission);

            outputFormatter.printSuccess("Strategic plan created successfully!");
            outputFormatter.printInfo("Plan ID: " + plan.getId());
            outputFormatter.printInfo("Title: " + plan.getTitle());
            outputFormatter.printInfo("Vision: " + plan.getVisionStatement());
            outputFormatter.printInfo("Mission: " + plan.getMissionStatement());

        } catch (Exception e) {
            outputFormatter.printError("Error creating strategic plan: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

        /**
     * View all strategic plans for the current company
     */
    private void viewStrategicPlans() {
        outputFormatter.printHeader("Strategic Plans");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();
            List<StrategicPlan> plans = strategicPlanningService.getStrategicPlansForCompany(companyId);

            if (plans.isEmpty()) {
                outputFormatter.printInfo("No strategic plans found for this company.");
                outputFormatter.printInfo("Use option 1 to create your first strategic plan.");
                inputHandler.waitForEnter();
                return;
            }

            outputFormatter.printInfo("Found " + plans.size() + " strategic plan(s):");
            System.out.println();

            for (StrategicPlan plan : plans) {
                System.out.println("📋 Plan ID: " + plan.getId());
                System.out.println("📝 Title: " + plan.getTitle());
                System.out.println("👁️  Vision: " + plan.getVisionStatement());
                System.out.println("🎯 Mission: " + plan.getMissionStatement());
                System.out.println("📅 Created: " + plan.getCreatedAt());
                System.out.println("📅 Updated: " + plan.getUpdatedAt());

                // Get priorities for this plan
                List<StrategicPriority> priorities = strategicPlanningService.getStrategicPriorities(plan.getId());
                if (priorities != null && !priorities.isEmpty()) {
                    System.out.println("🎯 Strategic Priorities:");
                    priorities.forEach(priority ->
                        System.out.println("   • " + priority.getName() + " (" + priority.getPriorityOrder() + ")")
                    );
                } else {
                    System.out.println("🎯 No strategic priorities defined yet.");
                }

                System.out.println("─".repeat(50));
            }

        } catch (Exception e) {
            outputFormatter.printError("Error viewing strategic plans: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    /**
     * Display strategic plan details
     */
    private void displayStrategicPlanDetails(StrategicPlan plan) {
        System.out.println("📋 Strategic Plan ID: " + plan.getId());
        System.out.println("🏢 Company ID: " + plan.getCompanyId());
        System.out.println("👁️  Vision: " + plan.getVisionStatement());
        System.out.println("🎯 Mission: " + plan.getMissionStatement());
        System.out.println("🎯 Goals: " + plan.getGoals());
        System.out.println("📅 Created: " + plan.getCreatedAt());

        List<StrategicPriority> priorities;
        try {
            priorities = strategicPlanningService.getStrategicPriorities(plan.getId());
        } catch (SQLException e) {
            System.out.println("❌ Error loading priorities: " + e.getMessage());
            priorities = null;
        }

        if (priorities != null && !priorities.isEmpty()) {
            System.out.println("🎯 Strategic Priorities:");
            priorities.forEach(priority ->
                System.out.println("  • " + priority.getName() + " (" + priority.getPriorityOrder() + ") - " + priority.getDescription())
            );
        }
    }

    /**
     * Edit an existing strategic plan
     */
    private void editStrategicPlan() {
        outputFormatter.printHeader("Edit Strategic Plan");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();
            List<StrategicPlan> plans = strategicPlanningService.getStrategicPlansForCompany(companyId);

            if (plans.isEmpty()) {
                outputFormatter.printWarning("No strategic plans found to edit.");
                inputHandler.waitForEnter();
                return;
            }

            // For now, just edit the first plan (could be enhanced to select)
            StrategicPlan plan = plans.get(0);

            outputFormatter.printInfo("Current Vision: " + plan.getVisionStatement());
            inputHandler.getString("New vision (press Enter to keep current)", plan.getVisionStatement());

            outputFormatter.printInfo("Current Mission: " + plan.getMissionStatement());
            inputHandler.getString("New mission (press Enter to keep current)", plan.getMissionStatement());

            outputFormatter.printInfo("Current Goals: " + plan.getGoals());
            inputHandler.getString("New goals (press Enter to keep current)", plan.getGoals());

            // Note: Strategic plan editing not yet implemented in service layer
            outputFormatter.printWarning("Strategic plan editing is not yet implemented.");
            outputFormatter.printInfo("Current values shown for reference only.");
            outputFormatter.printInfo("Please contact development team to modify strategic plans.");

        } catch (Exception e) {
            outputFormatter.printError("Error editing strategic plan: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    /**
     * Delete a strategic plan
     */
    private void deleteStrategicPlan() {
        outputFormatter.printHeader("Delete Strategic Plan");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();
            List<StrategicPlan> plans = strategicPlanningService.getStrategicPlansForCompany(companyId);

            if (plans.isEmpty()) {
                outputFormatter.printWarning("No strategic plans found to delete.");
                inputHandler.waitForEnter();
                return;
            }

            // For now, just delete the first plan (could be enhanced to select)
            StrategicPlan plan = plans.get(0);

            outputFormatter.printWarning("This will permanently delete the strategic plan.");
            outputFormatter.printInfo("Plan to delete: " + plan.getVisionStatement());

            outputFormatter.printWarning("Strategic plan deletion is not yet implemented.");
            outputFormatter.printInfo("Please contact development team to delete strategic plans.");

        } catch (Exception e) {
            outputFormatter.printError("Error deleting strategic plan: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    /**
     * Handle budget management operations
     */
    private void handleBudgetManagementMenu() {
        boolean back = false;
        while (!back) {
            displayBudgetManagementMenu();
            int choice = inputHandler.getInteger("Enter your choice", 1, BUDGET_MGMT_MENU_MAX_OPTION);

            switch (choice) {
                case BUDGET_MGMT_OPTION_CREATE_BUDGET:
                    createBudget();
                    break;
                case BUDGET_MGMT_OPTION_VIEW_BUDGETS:
                    viewBudgets();
                    break;
                case BUDGET_MGMT_OPTION_EDIT_BUDGET:
                    editBudget();
                    break;
                case BUDGET_MGMT_OPTION_DELETE_BUDGET:
                    deleteBudget();
                    break;
                case BUDGET_MGMT_OPTION_BACK:
                    back = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Display budget management menu
     */
    private void displayBudgetManagementMenu() {
        outputFormatter.printSubHeader("Budget Management");
        System.out.println("1. Create Budget");
        System.out.println("2. View Budgets");
        System.out.println("3. Edit Budget");
        System.out.println("4. Delete Budget");
        System.out.println("5. Back");
        System.out.print("Enter your choice (1-5): ");
    }

    /**
     * Create a new budget for the current company
     */
    private void createBudget() {
        outputFormatter.printHeader("Create Budget");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();

            // Check if company already has budgets
            List<Budget> existingBudgets = budgetService.getBudgetsForCompany(companyId);
            if (!existingBudgets.isEmpty()) {
                outputFormatter.printWarning("This company already has budgets. Consider editing existing budgets instead.");
                if (!inputHandler.getBoolean("Continue creating a new budget?")) {
                    return;
                }
            }

            // Get budget details
            String title = inputHandler.getString("Enter budget title");
            Integer year = inputHandler.getInteger("Enter budget year", 2024, 2030);

            // Use sample creation method
            Budget budget = budgetService.createSampleBudget(companyId, title, year);

            outputFormatter.printSuccess("Budget created successfully!");
            outputFormatter.printInfo("Budget ID: " + budget.getId());
            outputFormatter.printInfo("Year: " + budget.getBudgetYear());
            outputFormatter.printInfo("Total Revenue: R" + budget.getTotalRevenue());
            outputFormatter.printInfo("Total Expenses: R" + budget.getTotalExpenses());

        } catch (Exception e) {
            outputFormatter.printError("Error creating budget: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    /**
     * View all budgets for the current company
     */
    private void viewBudgets() {
        outputFormatter.printHeader("Budgets");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();
            List<Budget> budgets = budgetService.getBudgetsForCompany(companyId);

            if (budgets.isEmpty()) {
                outputFormatter.printWarning("No budgets found for this company.");
                outputFormatter.printInfo("Create a budget first.");
                inputHandler.waitForEnter();
                return;
            }

            for (Budget budget : budgets) {
                displayBudgetDetails(budget);
                System.out.println();
            }

        } catch (Exception e) {
            outputFormatter.printError("Error viewing budgets: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    /**
     * Display budget details
     */
    private void displayBudgetDetails(Budget budget) {
        System.out.println("💰 Budget ID: " + budget.getId());
        System.out.println("🏢 Company ID: " + budget.getCompanyId());
        System.out.println("📅 Year: " + budget.getBudgetYear());
        System.out.println("💵 Total Revenue: R" + budget.getTotalRevenue());
        System.out.println("💸 Total Expenses: R" + budget.getTotalExpenses());
        System.out.println("📊 Net Budget: R" + budget.getNetBudget());
        System.out.println("📅 Created: " + budget.getCreatedAt());

        if (budget.getCategories() != null && !budget.getCategories().isEmpty()) {
            System.out.println("📊 Budget Categories:");
            budget.getCategories().forEach(category -> {
                System.out.println("  📁 " + category.getName() + " (R" + category.getTotalAllocated() + ")");
                if (category.getItems() != null && !category.getItems().isEmpty()) {
                    category.getItems().forEach(item ->
                        System.out.println("    • " + item.getDescription() + ": R" + item.getAnnualAmount())
                    );
                }
            });
        }
    }

    /**
     * Edit an existing budget
     */
    private void editBudget() {
        outputFormatter.printHeader("Edit Budget");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();
            List<Budget> budgets = budgetService.getBudgetsForCompany(companyId);

            if (budgets.isEmpty()) {
                outputFormatter.printWarning("No budgets found to edit.");
                inputHandler.waitForEnter();
                return;
            }

            // For now, just edit the first budget (could be enhanced to select)
            Budget budget = budgets.get(0);

            outputFormatter.printInfo("Current Year: " + budget.getBudgetYear());
            outputFormatter.printInfo("Current Total Revenue: R" + budget.getTotalRevenue());
            outputFormatter.printInfo("Current Total Expenses: R" + budget.getTotalExpenses());

            // Note: Budget editing not yet implemented in service layer
            outputFormatter.printWarning("Budget editing is not yet implemented.");
            outputFormatter.printInfo("Please contact development team to modify budgets.");

        } catch (Exception e) {
            outputFormatter.printError("Error editing budget: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    /**
     * Delete a budget
     */
    private void deleteBudget() {
        outputFormatter.printHeader("Delete Budget");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();
            List<Budget> budgets = budgetService.getBudgetsForCompany(companyId);

            if (budgets.isEmpty()) {
                outputFormatter.printWarning("No budgets found to delete.");
                inputHandler.waitForEnter();
                return;
            }

            // For now, just delete the first budget (could be enhanced to select)
            Budget budget = budgets.get(0);

            outputFormatter.printWarning("This will permanently delete the budget.");
            outputFormatter.printInfo("Budget to delete: " + budget.getBudgetYear() + " (R" + budget.getTotalRevenue() + " revenue, R" + budget.getTotalExpenses() + " expenses)");

            outputFormatter.printWarning("Budget deletion is not yet implemented.");
            outputFormatter.printInfo("Please contact development team to delete budgets.");

        } catch (Exception e) {
            outputFormatter.printError("Error deleting budget: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    /**
     * Handle budget reports
     */
    private void handleBudgetReports() {
        boolean back = false;
        while (!back) {
            displayBudgetReportsMenu();
            int choice = inputHandler.getInteger("Enter your choice", 1, BUDGET_REPORT_MENU_MAX_OPTION);

            switch (choice) {
                case BUDGET_REPORT_OPTION_BUDGET_SUMMARY:
                    generateBudgetSummaryReport();
                    break;
                case BUDGET_REPORT_OPTION_BUDGET_VS_ACTUAL:
                    generateBudgetVsActualReport();
                    break;
                case BUDGET_REPORT_OPTION_STRATEGIC_PLAN_REPORT:
                    generateStrategicPlanReport();
                    break;
                case BUDGET_REPORT_OPTION_PRINT_PDF_REPORTS:
                    handlePdfReports();
                    break;
                case BUDGET_REPORT_OPTION_BACK:
                    back = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Display budget reports menu
     */
    private void displayBudgetReportsMenu() {
        outputFormatter.printSubHeader("Budget Reports");
        System.out.println("1. Budget Summary Report");
        System.out.println("2. Budget vs Actual Report");
        System.out.println("3. Strategic Plan Report");
        System.out.println("4. Print PDF Reports");
        System.out.println("5. Back");
        System.out.print("Enter your choice (1-5): ");
    }

    /**
     * Generate budget summary report
     */
    private void generateBudgetSummaryReport() {
        outputFormatter.printHeader("Budget Summary Report");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();
            List<Budget> budgets = budgetService.getBudgetsForCompany(companyId);

            if (budgets.isEmpty()) {
                outputFormatter.printWarning("No budgets found for this company.");
                inputHandler.waitForEnter();
                return;
            }

            // For now, show summary of first budget
            Budget budget = budgets.get(0);

            System.out.println("🏢 Company: " + applicationState.getCurrentCompany().getName());
            System.out.println("📅 Budget Year: " + budget.getBudgetYear());
            System.out.println("💰 Total Revenue: R" + budget.getTotalRevenue());
            System.out.println("💸 Total Expenses: R" + budget.getTotalExpenses());
            System.out.println("📊 Net Budget: R" + budget.getNetBudget());
            System.out.println();

            if (budget.getCategories() != null && !budget.getCategories().isEmpty()) {
                System.out.println("📊 Budget Breakdown by Category:");
                budget.getCategories().forEach(category -> {
                    BigDecimal totalBudget = budget.getTotalRevenue().add(budget.getTotalExpenses());
                    BigDecimal percentage = category.getTotalAllocated()
                        .divide(totalBudget, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                    System.out.println("  • " + category.getName() + ": R" + category.getTotalAllocated() +
                                     " (" + percentage.setScale(1, RoundingMode.HALF_UP) + "%)");
                });
            }

        } catch (Exception e) {
            outputFormatter.printError("Error generating budget summary: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    /**
     * Generate budget vs actual report
     */
    private void generateBudgetVsActualReport() {
        outputFormatter.printHeader("Budget vs Actual Report");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();
            List<Budget> budgets = budgetService.getBudgetsForCompany(companyId);

            if (budgets.isEmpty()) {
                outputFormatter.printWarning("No budgets found for comparison.");
                inputHandler.waitForEnter();
                return;
            }

            // For now, show basic comparison (would need actual transaction data)
            Budget budget = budgets.get(0);

            System.out.println("🏢 Company: " + applicationState.getCurrentCompany().getName());
            System.out.println("📅 Budget Year: " + budget.getBudgetYear());
            System.out.println("💰 Budgeted Revenue: R" + budget.getTotalRevenue());
            System.out.println("💸 Budgeted Expenses: R" + budget.getTotalExpenses());
            System.out.println("📊 Budgeted Net: R" + budget.getNetBudget());
            System.out.println("� Actual Amount: [Not implemented - would compare with actual transactions]");
            System.out.println("📈 Variance: [Not implemented]");

            outputFormatter.printInfo("Budget vs Actual comparison feature coming soon!");
            outputFormatter.printInfo("This will compare budgeted amounts with actual transaction data.");

        } catch (Exception e) {
            outputFormatter.printError("Error generating budget vs actual report: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    /**
     * Generate strategic plan report
     */
    private void generateStrategicPlanReport() {
        outputFormatter.printHeader("Strategic Plan Report");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();
            List<StrategicPlan> plans = strategicPlanningService.getStrategicPlansForCompany(companyId);

            if (plans.isEmpty()) {
                outputFormatter.printWarning("No strategic plans found for this company.");
                inputHandler.waitForEnter();
                return;
            }

            // For now, show first strategic plan
            StrategicPlan plan = plans.get(0);

            System.out.println("🏢 Company: " + applicationState.getCurrentCompany().getName());
            System.out.println("👁️  Vision: " + plan.getVisionStatement());
            System.out.println("🎯 Mission: " + plan.getMissionStatement());
            System.out.println("🎯 Goals: " + plan.getGoals());
            System.out.println();

            List<StrategicPriority> priorities = strategicPlanningService.getStrategicPriorities(plan.getId());
            if (priorities != null && !priorities.isEmpty()) {
                System.out.println("🎯 Strategic Priorities:");
                priorities.forEach(priority ->
                    System.out.println("  • " + priority.getName() + " (" + priority.getPriorityOrder() + ") - " + priority.getDescription())
                );
            }

        } catch (Exception e) {
            outputFormatter.printError("Error generating strategic plan report: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    /**
     * Handle PDF reports submenu
     */
    private void handlePdfReports() {
        boolean back = false;
        while (!back) {
            displayPdfReportsMenu();
            int choice = inputHandler.getInteger("Enter your choice", 1, PDF_REPORT_MENU_MAX_OPTION);

            switch (choice) {
                case PDF_REPORT_OPTION_BUDGET_SUMMARY_PDF:
                    generateBudgetSummaryPdf();
                    break;
                case PDF_REPORT_OPTION_BUDGET_VS_ACTUAL_PDF:
                    generateBudgetVsActualPdf();
                    break;
                case PDF_REPORT_OPTION_STRATEGIC_PLAN_PDF:
                    generateStrategicPlanPdf();
                    break;
                case PDF_REPORT_OPTION_BACK:
                    back = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Display PDF reports menu
     */
    private void displayPdfReportsMenu() {
        outputFormatter.printSubHeader("Print PDF Reports");
        System.out.println("1. Print Budget Summary PDF");
        System.out.println("2. Print Budget vs Actual PDF");
        System.out.println("3. Print Strategic Plan PDF");
        System.out.println("4. Back");
        System.out.print("Enter your choice (1-4): ");
    }

    /**
     * Generate budget summary PDF
     */
    private void generateBudgetSummaryPdf() {
        outputFormatter.printHeader("Generate Budget Summary PDF");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();

            budgetReportService.generateBudgetSummaryReport(companyId);

            outputFormatter.printSuccess("Budget Summary PDF generated successfully!");
            outputFormatter.printInfo("File saved to: reports/budget_summary_report.pdf");

        } catch (Exception e) {
            outputFormatter.printError("Error generating budget summary PDF: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    /**
     * Generate budget vs actual PDF
     */
    private void generateBudgetVsActualPdf() {
        outputFormatter.printHeader("Generate Budget vs Actual PDF");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();

            budgetReportService.generateBudgetVsActualReport(companyId);

            outputFormatter.printSuccess("Budget vs Actual PDF generated successfully!");
            outputFormatter.printInfo("File saved to: reports/budget_vs_actual_report.pdf");

        } catch (Exception e) {
            outputFormatter.printError("Error generating budget vs actual PDF: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    /**
     * Generate strategic plan PDF
     */
    private void generateStrategicPlanPdf() {
        outputFormatter.printHeader("Generate Strategic Plan PDF");

        try {
            Long companyId = applicationState.getCurrentCompany().getId();

            budgetReportService.generateStrategicPlanReport(companyId);

            outputFormatter.printSuccess("Strategic Plan PDF generated successfully!");
            outputFormatter.printInfo("File saved to: reports/strategic_plan_report.pdf");

        } catch (Exception e) {
            outputFormatter.printError("Error generating strategic plan PDF: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }
}