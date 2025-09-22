package fin.app;

import fin.model.BankTransaction;
import fin.service.TransactionMappingService;
import fin.service.FinancialReportingService;
import fin.service.InteractiveCategorizationService;
import fin.service.ExcelFinancialReportService;
import fin.config.DatabaseConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Comprehensive Financial Reporting Application
 * 
 * This application handles:
 * 1. Analysis of unclassified transactions
 * 2. Interactive transaction categorization with user assistance
 * 3. Automated journal entry generation
 * 4. Comprehensive financial report generation
 * 5. Export of reports to reports/ directory
 */
public class FinancialReportingApp {
    private static final Logger LOGGER = Logger.getLogger(FinancialReportingApp.class.getName());
    private final FinancialReportingService reportingService;
    private final TransactionMappingService mappingService;
    private final InteractiveCategorizationService categorizationService;
    private final ExcelFinancialReportService excelReportService;
    private final Scanner scanner;
    private final String dbUrl;
    
    public FinancialReportingApp() {
        // Initialize database URL from configuration
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
        
        // Initialize scanner for user input
        this.scanner = new Scanner(System.in);
        
        // Check database connection and environment before proceeding
        if (!initializeEnvironment()) {
            LOGGER.warning("Financial Reporting App initialized with warnings");
        }
        
        // Initialize services
        this.reportingService = new FinancialReportingService(dbUrl);
        this.mappingService = new TransactionMappingService(dbUrl);
        this.categorizationService = new InteractiveCategorizationService(dbUrl);
        this.excelReportService = new ExcelFinancialReportService(dbUrl);
        
        LOGGER.info("Financial Reporting App initialized successfully");
    }
    
    /**
     * Check environment configuration and setup
     */
    private boolean initializeEnvironment() {
        boolean isValid = true;
        
        // Check database configuration
        if (dbUrl == null || dbUrl.isEmpty()) {
            LOGGER.severe("Database URL is not configured properly");
            System.err.println("❌ Database URL is missing - check environment variables");
            isValid = false;
        }
        
        // Check reports directory
        String reportsDir = "/Users/sthwalonyoni/FIN/reports";
        if (!ensureReportDirectoryExists(reportsDir)) {
            LOGGER.warning("Could not verify or create reports directory: " + reportsDir);
            isValid = false;
        }
        
        return isValid;
    }
    
    public static void main(String[] args) {
        System.out.println("🏢 XINGHIZANA GROUP - FINANCIAL REPORTING SYSTEM");
        System.out.println("================================================");
        System.out.println();

        FinancialReportingApp app = new FinancialReportingApp();

        try {
            // Check for command line arguments
            if (args.length > 0 && "generate-journal-entries".equals(args[0])) {
                // Non-interactive mode: generate journal entries
                System.out.println("🔄 Running in non-interactive mode: Generating journal entries...");

                Long companyId = app.getCompanyId("Xinghizana Group");
                if (companyId == null) {
                    System.err.println("❌ Error: Could not find company 'Xinghizana Group'");
                    return;
                }

                app.generateJournalEntries(companyId);
                System.out.println("✅ Journal entries generated successfully!");
                return;
            }

            // Get company and fiscal period IDs
            Long companyId = app.getCompanyId("Xinghizana Group");
            Long fiscalPeriodId = app.getCurrentFiscalPeriodId(companyId);

            if (companyId == null || fiscalPeriodId == null) {
                System.err.println("❌ Error: Could not find company or fiscal period");
                return;
            }

            // Interactive Menu System
            app.showMainMenu(companyId, fiscalPeriodId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in financial reporting process", e);
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close scanner
            if (app.scanner != null) {
                app.scanner.close();
            }
        }
    }
    
    /**
     * Shows the main interactive menu for the financial reporting system
     */
    public void showMainMenu(Long companyId, Long fiscalPeriodId) {
        while (true) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📊 FINANCIAL REPORTING MAIN MENU");
            System.out.println("=".repeat(60));
            System.out.println("1. 📋 Analyze Unclassified Transactions");
            System.out.println("2. 🎯 Interactive Transaction Categorization");
            System.out.println("3. 📝 Generate Automated Journal Entries");
            System.out.println("4. 📊 Generate All Financial Reports (TXT)");
            System.out.println("5. 📈 Generate Excel Financial Report Template");
            System.out.println("6. 📋 Quick Financial Summary");
            System.out.println("7. 🚪 Exit");
            System.out.println("=".repeat(60));
            System.out.print("Please select an option (1-7): ");
            
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                
                switch (choice) {
                    case 1:
                        System.out.println("\n📊 ANALYZING UNCLASSIFIED TRANSACTIONS");
                        System.out.println("=====================================");
                        analyzeUnclassifiedTransactions(companyId);
                        break;
                        
                    case 2:
                        System.out.println("\n🎯 INTERACTIVE TRANSACTION CATEGORIZATION");
                        System.out.println("=========================================");
                        runInteractiveCategorization(companyId, fiscalPeriodId);
                        break;
                        
                    case 3:
                        System.out.println("\n📝 GENERATING AUTOMATED JOURNAL ENTRIES");
                        System.out.println("=======================================");
                        generateJournalEntries(companyId);
                        break;
                        
                    case 4:
                        System.out.println("\n📊 GENERATING COMPREHENSIVE FINANCIAL REPORTS (TXT)");
                        System.out.println("==================================================");
                        generateAllReports(companyId, fiscalPeriodId);
                        break;
                        
                    case 5:
                        System.out.println("\n📈 GENERATING EXCEL FINANCIAL REPORT TEMPLATE");
                        System.out.println("=============================================");
                        generateExcelFinancialReport(companyId, fiscalPeriodId);
                        break;
                        
                    case 6:
                        System.out.println("\n📋 QUICK FINANCIAL SUMMARY");
                        System.out.println("==========================");
                        showQuickSummary(companyId, fiscalPeriodId);
                        break;
                        
                    case 7:
                        System.out.println("\n👋 Thank you for using Sthwalo Holdings Financial Reporting System!");
                        System.out.println("All changes have been saved to the database.");
                        return;
                        
                    default:
                        System.out.println("\n❌ Invalid option. Please select 1-7.");
                }
                
                if (choice != 7) {
                    System.out.println("\nPress Enter to continue...");
                    scanner.nextLine();
                }
                
            } catch (NumberFormatException e) {
                System.out.println("\n❌ Invalid input. Please enter a number between 1-7.");
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
            } catch (Exception e) {
                System.err.println("\n❌ Error: " + e.getMessage());
                LOGGER.log(Level.SEVERE, "Error in menu operation", e);
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
            }
        }
    }
    
    /**
     * Runs the interactive categorization process
     */
    public void runInteractiveCategorization(Long companyId, Long fiscalPeriodId) {
        System.out.println("Starting interactive transaction categorization...");
        System.out.println("This will help you review and categorize unclassified transactions.");
        System.out.println("You can create new accounts and set up auto-categorization rules.");
        System.out.println();
        
        try {
            categorizationService.runInteractiveCategorization(companyId, fiscalPeriodId);
            System.out.println("✅ Interactive categorization completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Error in interactive categorization: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Generates Excel financial report using the company template format
     */
    public void generateExcelFinancialReport(Long companyId, Long fiscalPeriodId) {
        System.out.println("Generating Excel financial report using your template format...");
        System.out.println("This will create a comprehensive financial report matching your");
        System.out.println("standard Excel template with all 15 sheets and consistent formatting.");
        System.out.println();
        
        try {
            String reportsDir = "/Users/sthwalonyoni/FIN/reports";
            
            // Ensure reports directory exists
            if (!ensureReportDirectoryExists(reportsDir)) {
                System.err.println("❌ Cannot generate Excel reports due to directory issues");
                return;
            }
            
            excelReportService.generateComprehensiveFinancialReport(companyId, fiscalPeriodId, reportsDir);
            
            // Verify report file was created
            File reportDir = new File(reportsDir);
            if (reportDir.exists() && reportDir.isDirectory()) {
                File[] files = reportDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".xlsx"));
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        System.out.println("✅ Verified report file was created: " + file.getAbsolutePath());
                        System.out.println("📊 File size: " + (file.length() / 1024) + " KB");
                    }
                } else {
                    System.err.println("⚠️ Warning: No Excel report files found in " + reportsDir);
                }
            }
            
            System.out.println("✅ Excel Financial Report generated successfully!");
            System.out.println("📁 Location: " + reportsDir);
            System.out.println();
            System.out.println("📋 The Excel report includes all sheets from your template:");
            System.out.println("   • Cover - Company branding and title");
            System.out.println("   • Index - Table of contents");
            System.out.println("   • Co details - Company information");
            System.out.println("   • State of responsibility - Directors' responsibility");
            System.out.println("   • Audit report - Independent auditor's report");
            System.out.println("   • Directors report - Directors' report");
            System.out.println("   • Balance sheet - Statement of Financial Position");
            System.out.println("   • Income statement - Statement of Comprehensive Income");
            System.out.println("   • State of changes - Statement of Changes in Equity");
            System.out.println("   • Cash flow - Statement of Cash Flows");
            System.out.println("   • Notes 1 - Accounting policies");
            System.out.println("   • Notes 2-6 - Revenue and income notes");
            System.out.println("   • Notes 7-10 - Asset and receivables notes");
            System.out.println("   • Notes 11-12 - Cash flow notes");
            System.out.println("   • Detailed IS - Detailed Income Statement");
            
        } catch (Exception e) {
            System.err.println("❌ Error generating Excel financial report: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Detailed error info", e);
            // Don't throw - this prevents the menu from continuing
        }
    }
    
    /**
     * Ensures the reports directory exists, creating it if necessary
     * @return true if directory exists or was created successfully
     */
    private boolean ensureReportDirectoryExists(String reportsDir) {
        File directory = new File(reportsDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("📁 Created reports directory: " + reportsDir);
            } else {
                System.err.println("❌ Failed to create reports directory: " + reportsDir);
                return false;
            }
        } else {
            System.out.println("📁 Using existing reports directory: " + reportsDir);
        }
        
        // Check if directory is writable
        if (!directory.canWrite()) {
            System.err.println("❌ Reports directory is not writable: " + reportsDir);
            return false;
        }
        
        return true;
    }
    
    /**
     * Shows a quick financial summary
     */
    public void showQuickSummary(Long companyId, Long fiscalPeriodId) {
        try {
            // Get total transactions
            int totalTransactions = getTotalTransactionCount(companyId);
            int journalEntries = getJournalEntryCount(companyId);
            int unclassifiedCount = getUnclassifiedTransactionCount(companyId);
            
            System.out.println("📊 QUICK FINANCIAL SUMMARY");
            System.out.println("-".repeat(40));
            System.out.printf("Total Bank Transactions: %,d\n", totalTransactions);
            System.out.printf("Generated Journal Entries: %,d\n", journalEntries);
            System.out.printf("Unclassified Transactions: %,d\n", unclassifiedCount);
            
            if (unclassifiedCount > 0) {
                double percentClassified = ((double)(totalTransactions - unclassifiedCount) / totalTransactions) * 100;
                System.out.printf("Classification Progress: %.1f%%\n", percentClassified);
            } else {
                System.out.println("Classification Progress: 100.0% ✅");
            }
            
            System.out.println("-".repeat(40));
            
            // Show account summary
            showAccountSummary(companyId);
            
        } catch (Exception e) {
            System.err.println("❌ Error generating summary: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Analyzes unclassified transactions and shows mapping suggestions
     */
    public void analyzeUnclassifiedTransactions(Long companyId) {
        System.out.println("Analyzing unclassified bank transactions...");
        
        Map<String, List<BankTransaction>> groupedTransactions = 
            mappingService.analyzeUnclassifiedTransactions(companyId);
        
        if (groupedTransactions.isEmpty()) {
            System.out.println("✅ All transactions are already classified!");
            return;
        }
        
        System.out.println("\\n📊 TRANSACTION CLASSIFICATION ANALYSIS:");
        System.out.println("-".repeat(80));
        System.out.printf("%-40s %10s %15s\\n", "Account Classification", "Count", "Total Amount");
        System.out.println("-".repeat(80));
        
        int totalUnclassified = 0;
        for (Map.Entry<String, List<BankTransaction>> entry : groupedTransactions.entrySet()) {
            String accountName = entry.getKey();
            List<BankTransaction> transactions = entry.getValue();
            
            double totalAmount = transactions.stream()
                .mapToDouble(t -> {
                    if (t.getDebitAmount() != null) return t.getDebitAmount().doubleValue();
                    if (t.getCreditAmount() != null) return t.getCreditAmount().doubleValue();
                    return 0.0;
                })
                .sum();
            
            totalUnclassified += transactions.size();
            
            System.out.printf("%-40s %10d %15.2f\\n", 
                accountName.length() > 38 ? accountName.substring(0, 35) + "..." : accountName,
                transactions.size(),
                totalAmount);
        }
        
        System.out.println("-".repeat(80));
        System.out.printf("TOTAL UNCLASSIFIED TRANSACTIONS: %d\\n", totalUnclassified);
        System.out.println();
    }
    
    /**
     * Generates automated journal entries for unclassified transactions
     */
    public void generateJournalEntries(Long companyId) {
        System.out.println("Generating automated journal entries from bank transactions...");
        
        try {
            mappingService.generateJournalEntriesForUnclassifiedTransactions(companyId, "SYSTEM-AUTO");
            System.out.println("✅ Journal entries generated successfully!");
            
            // Show summary of generated entries
            int entryCount = getJournalEntryCount(companyId);
            System.out.printf("📄 Total journal entries in system: %d\\n", entryCount);
            
        } catch (Exception e) {
            System.err.println("❌ Error generating journal entries: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Generates all financial reports and exports them
     */
    public void generateAllReports(Long companyId, Long fiscalPeriodId) {
        System.out.println("Generating comprehensive financial reports...");
        
        // Verify database connection first
        if (!verifyDatabaseConnection()) {
            System.err.println("❌ Cannot generate reports due to database connection issues");
            return;
        }
        
        try {
            // Ensure reports directory exists
            String reportsDir = "/Users/sthwalonyoni/FIN/reports";
            if (!ensureReportDirectoryExists(reportsDir)) {
                System.err.println("❌ Cannot generate reports due to directory issues");
                return;
            }
            
            // 1. Audit Trail
            System.out.println("\\n📋 Generating Audit Trail...");
            reportingService.generateAuditTrail(companyId, fiscalPeriodId, true);
            System.out.println("✅ Audit Trail generated and exported");
            
            // 2. Cashbook
            System.out.println("\\n💰 Generating Cashbook...");
            reportingService.generateCashbook(companyId, fiscalPeriodId, true);
            System.out.println("✅ Cashbook generated and exported");
            
            // 3. General Ledger
            System.out.println("\\n📚 Generating General Ledger...");
            reportingService.generateGeneralLedger(companyId, fiscalPeriodId, true);
            System.out.println("✅ General Ledger generated and exported");
            
            // 4. Trial Balance
            System.out.println("\\n⚖️ Generating Trial Balance...");
            reportingService.generateTrialBalance(companyId, fiscalPeriodId, true);
            System.out.println("✅ Trial Balance generated and exported");
            
            // 5. Income Statement
            System.out.println("\\n📈 Generating Income Statement...");
            reportingService.generateIncomeStatement(companyId, fiscalPeriodId, true);
            System.out.println("✅ Income Statement generated and exported");
            
            // 6. Balance Sheet
            System.out.println("\\n📊 Generating Balance Sheet...");
            reportingService.generateBalanceSheet(companyId, fiscalPeriodId, true);
            System.out.println("✅ Balance Sheet generated and exported");
            
            // Verify files were created and show file details
            verifyAndPrintReportFiles(reportsDir);
            
            // Show summary in console
            System.out.println("\\n" + "=".repeat(80));
            System.out.println("📋 FINANCIAL REPORTING SUMMARY");
            System.out.println("=".repeat(80));
            
            // Extract key figures from trial balance for summary
            System.out.println("Key Financial Information:");
            System.out.println("• Audit Trail: Complete transaction history with journal entries");
            System.out.println("• Cashbook: All bank transactions with account classifications");
            System.out.println("• General Ledger: Detailed account-by-account transaction listing");
            System.out.println("• Trial Balance: Account balances verification");
            System.out.println("• Income Statement: Revenue and expense analysis");
            System.out.println("• Balance Sheet: Assets, liabilities, and equity positions");
            
        } catch (Exception e) {
            System.err.println("❌ Error generating reports: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Detailed error info", e);
            // Don't throw - this prevents the menu from continuing
        }
    }
    
    /**
     * Verify database connection before running reports
     */
    private boolean verifyDatabaseConnection() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Database connection verified");
                
                // Check if we're using PostgreSQL or SQLite
                String dbType = isPostgreSQLDatabase() ? "PostgreSQL" : "SQLite";
                System.out.println("🔍 Using " + dbType + " database");
                
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection failed", e);
            System.err.println("❌ Database connection error: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Check if the application is using PostgreSQL database
     */
    private boolean isPostgreSQLDatabase() {
        return dbUrl != null && dbUrl.startsWith("jdbc:postgresql:");
    }
    
    /**
     * Verify report files exist and print their details
     */
    private void verifyAndPrintReportFiles(String reportsDir) {
        File reportDir = new File(reportsDir);
        if (reportDir.exists() && reportDir.isDirectory()) {
            File[] files = reportDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
            if (files != null && files.length > 0) {
                System.out.println("\n✅ Successfully generated " + files.length + " report files:");
                for (File file : files) {
                    long fileSize = file.length();
                    String sizeUnit = "B";
                    double displaySize = fileSize;
                    
                    if (fileSize > 1024) {
                        displaySize = fileSize / 1024.0;
                        sizeUnit = "KB";
                    }
                    if (displaySize > 1024) {
                        displaySize = displaySize / 1024.0;
                        sizeUnit = "MB";
                    }
                    
                    System.out.printf("   - %s (%.1f %s)%n", file.getName(), displaySize, sizeUnit);
                }
            } else {
                System.err.println("⚠️ Warning: No report files found in " + reportsDir);
            }
        } else {
            System.err.println("⚠️ Warning: Report directory not found: " + reportsDir);
        }
    }
    
    // Helper methods
    
    private Long getCompanyId(String companyName) {
        String sql = "SELECT id FROM companies WHERE name = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, companyName);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong("id");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting company ID", e);
        }
        
        return null;
    }
    
    private Long getCurrentFiscalPeriodId(Long companyId) {
        String sql = "SELECT id FROM fiscal_periods WHERE company_id = ? AND is_closed = false ORDER BY start_date DESC LIMIT 1";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong("id");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting fiscal period ID", e);
        }
        
        return null;
    }
    
    private int getJournalEntryCount(Long companyId) {
        String sql = "SELECT COUNT(*) FROM journal_entries WHERE company_id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting journal entry count", e);
        }
        
        return 0;
    }
    
    private int getTotalTransactionCount(Long companyId) {
        String sql = "SELECT COUNT(*) FROM bank_transactions WHERE company_id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting total transaction count", e);
        }
        
        return 0;
    }
    
    private int getUnclassifiedTransactionCount(Long companyId) {
        String sql = "SELECT COUNT(*) FROM bank_transactions WHERE company_id = ? AND account_id IS NULL";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting unclassified transaction count", e);
        }
        
        return 0;
    }
    
    private void showAccountSummary(Long companyId) {
        String sql = """
            SELECT ac.account_type, ac.name, 
                   COUNT(bt.id) as transaction_count,
                   COALESCE(SUM(CASE WHEN bt.debit_amount IS NOT NULL THEN bt.debit_amount ELSE 0 END), 0) as total_debits,
                   COALESCE(SUM(CASE WHEN bt.credit_amount IS NOT NULL THEN bt.credit_amount ELSE 0 END), 0) as total_credits
            FROM accounts ac
            LEFT JOIN bank_transactions bt ON ac.id = bt.account_id AND bt.company_id = ?
            WHERE ac.company_id = ?
            GROUP BY ac.account_type, ac.name
            HAVING COUNT(bt.id) > 0
            ORDER BY ac.account_type, COUNT(bt.id) DESC
            LIMIT 10
        """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            System.out.println("\n🏦 TOP ACTIVE ACCOUNTS:");
            System.out.println("-".repeat(80));
            System.out.printf("%-20s %-30s %8s %12s %12s\n", "Type", "Account Name", "Txns", "Debits", "Credits");
            System.out.println("-".repeat(80));
            
            while (rs.next()) {
                String accountType = rs.getString("account_type");
                String accountName = rs.getString("name");
                int txnCount = rs.getInt("transaction_count");
                double debits = rs.getDouble("total_debits");
                double credits = rs.getDouble("total_credits");
                
                if (accountName.length() > 28) {
                    accountName = accountName.substring(0, 25) + "...";
                }
                
                System.out.printf("%-20s %-30s %8d %12.2f %12.2f\n", 
                    accountType, accountName, txnCount, debits, credits);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error showing account summary", e);
        }
    }
}
