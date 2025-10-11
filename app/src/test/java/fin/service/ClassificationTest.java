package fin.service;

import fin.TestConfiguration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Test class to demonstrate the enhanced comprehensive transaction classification system
 */
public class ClassificationTest {
    private static final Logger LOGGER = Logger.getLogger(ClassificationTest.class.getName());

    public static void main(String[] args) {
        try {
            // Setup test database
            TestConfiguration.setupTestDatabase();

            String dbUrl = TestConfiguration.TEST_DB_URL_WITH_CREDENTIALS;

            LOGGER.info("Starting Enhanced Classification Test...");

            // Create service instance - using AccountClassificationService as single source of truth
            AccountClassificationService service = new AccountClassificationService(dbUrl);

            // Get company ID (assuming Xinghizana Group is company 1)
            Long companyId = 1L;

            // Show current classification status
            showClassificationStatus(dbUrl, companyId);

            // Run enhanced classification
            LOGGER.info("Running enhanced comprehensive classification...");
            int classified = service.classifyAllUnclassifiedTransactions(companyId, "ENHANCED-CLASSIFICATION-TEST");

            LOGGER.info("Enhanced classification completed. Classified " + classified + " transactions.");

            // Generate journal entries for double-entry accounting
            LOGGER.info("Generating journal entries for double-entry accounting...");
            service.generateJournalEntriesForClassifiedTransactions(companyId, "ENHANCED-CLASSIFICATION-TEST");

            LOGGER.info("Journal entries generation completed.");

            // Show updated classification status
            showClassificationStatus(dbUrl, companyId);

            // Show detailed account creation results
            showDetailedAccountsCreated(dbUrl, companyId);

            // Show roll-up capability demonstration
            showRollupDemonstration(dbUrl, companyId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during classification test", e);
        }
    }

    private static void showClassificationStatus(String dbUrl, Long companyId) {
        String sql = """
            SELECT
                'CLASSIFICATION STATUS' as status,
                COUNT(*) as total_transactions,
                COUNT(CASE WHEN account_code IS NOT NULL THEN 1 END) as classified,
                COUNT(CASE WHEN account_code IS NULL THEN 1 END) as unclassified,
                ROUND(
                    COUNT(CASE WHEN account_code IS NOT NULL THEN 1 END) * 100.0 / COUNT(*), 1
                ) as classified_percentage
            FROM bank_transactions
            WHERE company_id = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("\n=== " + rs.getString("status") + " ===");
                    System.out.println("Total Transactions: " + rs.getInt("total_transactions"));
                    System.out.println("Classified: " + rs.getInt("classified"));
                    System.out.println("Unclassified: " + rs.getInt("unclassified"));
                    System.out.println("Classification Rate: " + rs.getDouble("classified_percentage") + "%");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error showing classification status", e);
        }
    }

    private static void showDetailedAccountsCreated(String dbUrl, Long companyId) {
        String sql = """
            SELECT
                'DETAILED ACCOUNTS CREATED - DOUBLE-ENTRY CLASSIFICATION' as header,
                a.account_code,
                a.account_name,
                CASE
                    WHEN a.account_code LIKE '4000-%' THEN 'OPERATIONAL REVENUE (Income Statement)'
                    WHEN a.account_code LIKE '2000-%' THEN 'DIRECTOR LOANS (Balance Sheet - Liabilities)'
                    WHEN a.account_code LIKE '1000-%' THEN 'LOAN RECEIVABLES (Balance Sheet - Assets)'
                    WHEN a.account_code LIKE '5000-%' THEN 'OTHER INCOME (Income Statement)'
                    WHEN a.account_code LIKE '6000-%' THEN 'REVERSALS & ADJUSTMENTS (Income Statement)'
                    WHEN a.account_code LIKE '8100-%' THEN 'EMPLOYEE SALARIES (Income Statement)'
                    WHEN a.account_code LIKE '8500-%' THEN 'VEHICLE EXPENSES (Income Statement)'
                    WHEN a.account_code LIKE '8800-%' THEN 'INSURANCE EXPENSES (Income Statement)'
                    WHEN a.account_code LIKE '9600-%' THEN 'BANK FEES (Income Statement)'
                    ELSE 'OTHER ACCOUNTS'
                END as financial_statement_category,
                COUNT(*) as transactions_classified
            FROM accounts a
            JOIN bank_transactions bt ON bt.account_code = a.account_code
            WHERE a.company_id = ?
            AND a.account_code LIKE '%-%'
            AND LENGTH(SPLIT_PART(a.account_code, '-', 2)) > 2
            GROUP BY a.account_code, a.account_name
            ORDER BY a.account_code
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);

            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println("\n=== DOUBLE-ENTRY ACCOUNT CLASSIFICATION ===");
                boolean hasResults = false;
                while (rs.next()) {
                    if (!hasResults) {
                        hasResults = true;
                    }
                    System.out.println(rs.getString("account_code") + " - " +
                                     rs.getString("account_name"));
                    System.out.println("  → " + rs.getString("financial_statement_category") +
                                     " (" + rs.getInt("transactions_classified") + " transactions)");
                    System.out.println();
                }
                if (!hasResults) {
                    System.out.println("No detailed accounts created yet.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error showing detailed accounts", e);
        }
    }

    private static void showRollupDemonstration(String dbUrl, Long companyId) {
        String sql = """
            SELECT
                'FINANCIAL STATEMENT ROLL-UP - DOUBLE-ENTRY SYSTEM' as header,
                CASE
                    WHEN a.account_code LIKE '4000-%' THEN '4000 - OPERATIONAL REVENUE'
                    WHEN a.account_code LIKE '2000-%' THEN '2000 - DIRECTOR LOANS (LIABILITY)'
                    WHEN a.account_code LIKE '1000-%' THEN '1000 - LOAN RECEIVABLES (ASSET)'
                    WHEN a.account_code LIKE '5000-%' THEN '5000 - OTHER INCOME'
                    WHEN a.account_code LIKE '6000-%' THEN '6000 - REVERSALS & ADJUSTMENTS'
                    WHEN a.account_code LIKE '8100-%' THEN '8100 - EMPLOYEE COSTS'
                    WHEN a.account_code LIKE '8500-%' THEN '8500 - MOTOR VEHICLE EXPENSES'
                    WHEN a.account_code LIKE '8800-%' THEN '8800 - INSURANCE EXPENSES'
                    WHEN a.account_code LIKE '9600-%' THEN '9600 - BANK CHARGES'
                    ELSE 'OTHER CATEGORIES'
                END as financial_statement_category,
                COUNT(DISTINCT a.account_code) as detailed_accounts,
                COUNT(bt.id) as total_transactions,
                SUM(CASE WHEN bt.debit_amount > 0 THEN bt.debit_amount ELSE bt.credit_amount END) as total_amount,
                CASE
                    WHEN a.account_code LIKE '4000-%' THEN 'INCOME STATEMENT - OPERATIONAL'
                    WHEN a.account_code LIKE '2000-%' THEN 'BALANCE SHEET - LIABILITY'
                    WHEN a.account_code LIKE '1000-%' THEN 'BALANCE SHEET - ASSET'
                    WHEN a.account_code LIKE '5000-%' THEN 'INCOME STATEMENT - NON-OPERATIONAL'
                    WHEN a.account_code LIKE '6000-%' THEN 'INCOME STATEMENT - ADJUSTMENTS'
                    WHEN a.account_code LIKE '8100-%' THEN 'INCOME STATEMENT - OPERATIONAL'
                    WHEN a.account_code LIKE '8500-%' THEN 'INCOME STATEMENT - OPERATIONAL'
                    WHEN a.account_code LIKE '8800-%' THEN 'INCOME STATEMENT - OPERATIONAL'
                    WHEN a.account_code LIKE '9600-%' THEN 'INCOME STATEMENT - OPERATIONAL'
                    ELSE 'OTHER'
                END as statement_type
            FROM accounts a
            JOIN bank_transactions bt ON bt.account_code = a.account_code
            WHERE a.company_id = ?
            AND bt.account_code IS NOT NULL
            GROUP BY financial_statement_category, statement_type
            ORDER BY financial_statement_category
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);

            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println("\n=== FINANCIAL STATEMENT ROLL-UP DEMONSTRATION ===");
                boolean hasResults = false;
                while (rs.next()) {
                    if (!hasResults) {
                        hasResults = true;
                    }
                    System.out.println(rs.getString("financial_statement_category"));
                    System.out.println("  → " + rs.getString("statement_type"));
                    System.out.println("  → " + rs.getInt("detailed_accounts") + " detailed accounts, " +
                                     rs.getInt("total_transactions") + " transactions, " +
                                     "Total R" + rs.getBigDecimal("total_amount"));
                    System.out.println();
                }
                if (!hasResults) {
                    System.out.println("No roll-up data available yet.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error showing roll-up demonstration", e);
        }

        // Cleanup test database
        try {
            TestConfiguration.cleanupTestDatabase();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cleaning up test database", e);
        }
    }
}