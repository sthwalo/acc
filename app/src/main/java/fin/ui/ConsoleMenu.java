package fin.ui;

/**
 * Console menu display component
 * Extracted from monolithic App.java to separate UI concerns
 */
public class ConsoleMenu {

    // Display constants
    private static final int HEADER_WIDTH = 50;
    private static final int SEPARATOR_WIDTH = 80;
    private static final int FOOTER_WIDTH = 50;
    private static final int HEADER_PADDING_ADJUSTMENT = 2;
    
    public void displayMainMenu() {
        System.out.println("\n===== FIN Application Menu =====");
        System.out.println("1. Company Setup");
        System.out.println("2. Fiscal Period Management");
        System.out.println("3. Import Bank Statement");
        System.out.println("4. Import CSV Data");
        System.out.println("5. View Imported Data");
        System.out.println("6. Generate Reports");
        System.out.println("7. Data Management");
        System.out.println("8. Payroll Management");
        System.out.println("9. Budget Management");
        System.out.println("10. Depreciation Calculator");
        System.out.println("11. Show current time");
        System.out.println("12. System Logs");
        System.out.println("13. Exit");
        System.out.print("Enter your choice (1-13): ");
    }

    public void displayCompanyMenu() {
        System.out.println("\n===== Company Setup =====");
        System.out.println("1. Create new company");
        System.out.println("2. Select existing company");
        System.out.println("3. View company details");
        System.out.println("4. Edit company details");
        System.out.println("5. Delete company");
        System.out.println("6. Back to main menu");
        System.out.print("Enter your choice (1-6): ");
    }
    
    public void displayFiscalPeriodMenu() {
        System.out.println("\n===== Fiscal Period Management =====");
        System.out.println("1. Create new fiscal period");
        System.out.println("2. Select existing fiscal period");
        System.out.println("3. View fiscal period details");
        System.out.println("4. Back to main menu");
        System.out.print("Enter your choice (1-4): ");
    }
    
    public void displayReportMenu() {
        System.out.println("\n===== Financial Reports =====");
        System.out.println("1. Cashbook Report");
        System.out.println("2. General Ledger Report");
        System.out.println("3. Trial Balance Report");
        System.out.println("4. Income Statement");
        System.out.println("5. Balance Sheet");
        System.out.println("6. Audit Trail");
        System.out.println("7. Cash Flow Statement");
        System.out.println("8. Back to main menu");
        System.out.print("Enter your choice (1-8): ");
    }
    
    public void displayDataManagementMenu() {
        System.out.println("\n===== Data Management =====");
        System.out.println("1. Create Manual Invoice");
        System.out.println("2. Create Manual Journal Entry");
        System.out.println("3. Transaction Classification");
        System.out.println("4. Correct Transaction Categories");
        System.out.println("5. View Transaction History");
        System.out.println("6. Reset Company Data");
        System.out.println("7. Export to CSV");
        System.out.println("8. Back to main menu");
        System.out.print("Enter your choice (1-8): ");
    }
    
    public void displayImportMenu() {
        System.out.println("\n===== Import Bank Statement =====");
        System.out.println("1. Import single bank statement");
        System.out.println("2. Import multiple bank statements (batch)");
        System.out.println("3. Back to main menu");
        System.out.print("Enter your choice (1-3): ");
    }
    
    public void displayTransactionClassificationMenu() {
        System.out.println("\n===== Transaction Classification =====");
        System.out.println("1. Interactive Classification (new transactions)");
        System.out.println("2. Auto-Classify Unclassified Transactions");
        System.out.println("3. Reclassify ALL Transactions (apply updated rules)");
        System.out.println("4. Initialize Chart of Accounts & Mapping Rules");
        System.out.println("5. Sync Journal Entries (new transactions only)");
        System.out.println("6. Regenerate ALL Journal Entries (after reclassification)");
        System.out.println("7. Back to Data Management");
        System.out.print("Enter your choice (1-7): ");
    }
    
    public void displayAccountInitializationMenu() {
        System.out.println("\n===== Initialize Chart of Accounts =====");
        System.out.println("1. Initialize Chart of Accounts Only");
        System.out.println("2. Initialize Transaction Mapping Rules Only");
        System.out.println("3. Perform Full Initialization");
        System.out.println("4. Back");
        System.out.print("Enter choice (1-4): ");
    }
    
    public static void displayHeader(String title) {
        int titleLength = title.length();
        int padding = (HEADER_WIDTH - titleLength - HEADER_PADDING_ADJUSTMENT) / HEADER_PADDING_ADJUSTMENT;
        String leftPadding = " ".repeat(Math.max(0, padding));
        String rightPadding = " ".repeat(Math.max(0, padding));
        System.out.println("╔" + "═".repeat(HEADER_WIDTH) + "╗");
        System.out.println("║" + leftPadding + title + rightPadding + "║");
        System.out.println("╚" + "═".repeat(HEADER_WIDTH) + "╝");
    }
    
    public static void displaySeparator() {
        System.out.println("-".repeat(SEPARATOR_WIDTH));
    }
    
    public void displayFooter() {
        System.out.println("-".repeat(FOOTER_WIDTH));
    }
    
    // Payroll menu methods
    public void displayPayrollMenu() {
        System.out.println("\n===== Payroll Management =====");
        System.out.println("1. Employee Management");
        System.out.println("2. Payroll Period Management");
        System.out.println("3. Process Payroll");
        System.out.println("4. Payroll Reports");
        System.out.println("5. Payroll Setup");
        System.out.println("6. Back to Main Menu");
        System.out.print("Enter your choice (1-6): ");
    }
    
    public void displayEmployeeMenu() {
        System.out.println("\n===== Employee Management =====");
        System.out.println("1. Add New Employee");
        System.out.println("2. View All Employees");
        System.out.println("3. Edit Employee");
        System.out.println("4. Employee Details");
        System.out.println("5. Back");
        System.out.print("Enter your choice (1-5): ");
    }
    
    public void displayPayrollPeriodMenu() {
        System.out.println("\n===== Payroll Period Management =====");
        System.out.println("1. Create New Payroll Period");
        System.out.println("2. View Payroll Periods");
        System.out.println("3. Back");
        System.out.print("Enter your choice (1-3): ");
    }
    
    public void displayPayrollReportsMenu() {
        System.out.println("\n===== Payroll Reports =====");
        System.out.println("1. Payroll Summary");
        System.out.println("2. Employee Payslips");
        System.out.println("3. Tax Summary");
        System.out.println("4. Back");
        System.out.print("Enter your choice (1-4): ");
    }
    
    public void displayPayrollSetupMenu() {
        System.out.println("\n===== Payroll Setup =====");
        System.out.println("1. Initialize Payroll Database Schema");
        System.out.println("2. Tax Configuration");
        System.out.println("3. Benefit Setup");
        System.out.println("4. Deduction Setup");
        System.out.println("5. Back");
        System.out.print("Enter your choice (1-5): ");
    }
    
    // Budget menu methods
    public void displayBudgetMenu() {
        System.out.println("\n===== Budget Management =====");
        System.out.println("1. Strategic Planning");
        System.out.println("2. Budget Creation");
        System.out.println("3. Budget Reports");
        System.out.println("4. Budget vs Actual Analysis");
        System.out.println("5. Back to Main Menu");
        System.out.print("Enter your choice (1-5): ");
    }
    
    public void displayStrategicPlanningMenu() {
        System.out.println("\n===== Strategic Planning =====");
        System.out.println("1. Create Strategic Plan");
        System.out.println("2. View Strategic Plans");
        System.out.println("3. Edit Strategic Plan");
        System.out.println("4. Delete Strategic Plan");
        System.out.println("5. Back to Budget Menu");
        System.out.print("Enter your choice (1-5): ");
    }
    
    public void displayBudgetCreationMenu() {
        System.out.println("\n===== Budget Creation =====");
        System.out.println("1. Create New Budget");
        System.out.println("2. View Existing Budgets");
        System.out.println("3. Edit Budget");
        System.out.println("4. Delete Budget");
        System.out.println("5. Back to Budget Menu");
        System.out.print("Enter your choice (1-5): ");
    }
    
    public void displayBudgetReportsMenu() {
        System.out.println("\n===== Budget Reports =====");
        System.out.println("1. Budget Summary Report");
        System.out.println("2. Budget vs Actual Report");
        System.out.println("3. Strategic Plan Report");
        System.out.println("4. Back to Budget Menu");
        System.out.print("Enter your choice (1-4): ");
    }
}
