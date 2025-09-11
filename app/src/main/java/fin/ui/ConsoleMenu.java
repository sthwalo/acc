package fin.ui;

/**
 * Console menu display component
 * Extracted from monolithic App.java to separate UI concerns
 */
public class ConsoleMenu {
    
    public void displayMainMenu() {
        System.out.println("\n===== FIN Application Menu =====");
        System.out.println("1. Company Setup");
        System.out.println("2. Fiscal Period Management");
        System.out.println("3. Import Bank Statement");
        System.out.println("4. Import CSV Data");
        System.out.println("5. View Imported Data");
        System.out.println("6. Generate Reports");
        System.out.println("7. Data Management");
        System.out.println("8. Verify Transactions");
        System.out.println("9. Show current time");
        System.out.println("10. Exit");
        System.out.print("Enter your choice (1-10): ");
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
        System.out.println("6. Cash Flow Statement");
        System.out.println("7. Back to main menu");
        System.out.print("Enter your choice (1-7): ");
    }
    
    public void displayDataManagementMenu() {
        System.out.println("\n===== Data Management =====");
        System.out.println("1. Create Manual Invoice");
        System.out.println("2. Create Journal Entry");
        System.out.println("3. Transaction Classification");
        System.out.println("4. Correct Transaction Categorization");
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
        System.out.println("1. Run Interactive Classification");
        System.out.println("2. Auto-Classify Transactions");
        System.out.println("3. Initialize Chart of Accounts");
        System.out.println("4. Synchronize Journal Entries");
        System.out.println("5. Back to Data Management");
        System.out.print("Enter your choice (1-5): ");
    }
    
    public void displayAccountInitializationMenu() {
        System.out.println("\n===== Initialize Chart of Accounts =====");
        System.out.println("1. Initialize Chart of Accounts Only");
        System.out.println("2. Initialize Transaction Mapping Rules Only");
        System.out.println("3. Perform Full Initialization");
        System.out.println("4. Back");
        System.out.print("Enter choice (1-4): ");
    }
    
    public void displayHeader(String title) {
        int totalWidth = 50;
        int titleLength = title.length();
        int paddingLength = (totalWidth - titleLength - 2) / 2;
        String padding = "=".repeat(Math.max(0, paddingLength));
        
        System.out.println("\n" + "=".repeat(totalWidth));
        System.out.println(padding + " " + title + " " + padding);
        System.out.println("=".repeat(totalWidth));
    }
    
    public void displaySeparator() {
        System.out.println("-".repeat(80));
    }
    
    public void displayFooter() {
        System.out.println("-".repeat(50));
    }
}
