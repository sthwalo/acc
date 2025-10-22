package fin.repository;

import java.sql.*;
import java.util.logging.Logger;

/**
 * AccountRepository - Data access layer for account operations
 * Moved from fin.service.AccountManager for proper separation of concerns
 * 
 * Handles account CRUD operations at the repository level.
 */
public class AccountRepository {
    private static final Logger LOGGER = Logger.getLogger(AccountRepository.class.getName());

    private final String jdbcUrl;

    // PreparedStatement parameter indices for accounts table
    private static final int PARAM_COMPANY_ID = 1;
    private static final int PARAM_ACCOUNT_CODE = 2;
    private static final int PARAM_ACCOUNT_NAME = 3;
    private static final int PARAM_PARENT_ACCOUNT_ID = 4;
    private static final int PARAM_CATEGORY_ID = 5;

    // Category ID constants for different account types
    private static final int CATEGORY_CURRENT_ASSETS_COMPANY1 = 4;
    private static final int CATEGORY_CURRENT_ASSETS_COMPANY2 = 7;
    private static final int CATEGORY_NON_CURRENT_ASSETS_COMPANY2 = 8;
    private static final int CATEGORY_LIABILITIES_COMPANY1 = 5;
    private static final int CATEGORY_CURRENT_LIABILITIES_COMPANY2 = 9;
    private static final int CATEGORY_NON_CURRENT_LIABILITIES_COMPANY2 = 10;
    private static final int CATEGORY_EQUITY_COMPANY2 = 11;
    private static final int CATEGORY_REVENUE_EXPENSES_COMPANY1 = 6;
    private static final int CATEGORY_OPERATING_REVENUE_COMPANY2 = 12;
    private static final int CATEGORY_OTHER_INCOME_COMPANY2 = 13;
    private static final int CATEGORY_OPERATING_EXPENSES_COMPANY2 = 14;
    private static final int CATEGORY_FINANCE_COSTS_COMPANY2 = 16;

    public AccountRepository(String initialJdbcUrl) {
        this.jdbcUrl = initialJdbcUrl;
    }

    /**
     * Get or create a detailed account with full information
     * Uses PostgreSQL UPSERT (INSERT ... ON CONFLICT) for thread-safe operation
     *
     * @param accountCode The account code (e.g., "2000-001")
     * @param accountName The account name
     * @param parentCode The parent account code (e.g., "2000")
     * @param categoryName The account category name
     * @return The account ID
     */
    public Long getOrCreateDetailedAccount(String accountCode, String accountName,
                                          String parentCode, String categoryName) {
        Long companyId = 2L; // Use company 2 which has full category set
        
        try (Connection conn = getConnection()) {
            // First try to get existing account
            try (PreparedStatement selectStmt = conn.prepareStatement(
                     "SELECT id FROM accounts WHERE company_id = ? AND account_code = ?")) {

                selectStmt.setLong(1, companyId);
                selectStmt.setString(2, accountCode);

                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        LOGGER.fine("Found existing account: " + accountCode + " with ID: " + rs.getLong("id"));
                        return rs.getLong("id");
                    }
                }
            }

            // Account doesn't exist, use UPSERT to handle race conditions
            int categoryId = getCategoryIdForAccountCode(accountCode, companyId);
            
            // Look up parent account ID from parent code if provided
            Long parentAccountId = null;
            if (parentCode != null && !parentCode.isEmpty()) {
                parentAccountId = getAccountIdByCode(companyId, parentCode);
            }

            try (PreparedStatement upsertStmt = conn.prepareStatement(
                     "INSERT INTO accounts (company_id, account_code, account_name, " +
                     "parent_account_id, category_id, is_active, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP) " +
                     "ON CONFLICT (company_id, account_code) DO NOTHING " +
                     "RETURNING id")) {

                upsertStmt.setLong(PARAM_COMPANY_ID, companyId);
                upsertStmt.setString(PARAM_ACCOUNT_CODE, accountCode);
                upsertStmt.setString(PARAM_ACCOUNT_NAME, accountName);
                if (parentAccountId != null) {
                    upsertStmt.setLong(PARAM_PARENT_ACCOUNT_ID, parentAccountId);
                } else {
                    upsertStmt.setNull(PARAM_PARENT_ACCOUNT_ID, java.sql.Types.INTEGER);
                }
                upsertStmt.setInt(PARAM_CATEGORY_ID, categoryId);

                try (ResultSet rs = upsertStmt.executeQuery()) {
                    if (rs.next()) {
                        // Successfully inserted new account
                        Long newId = rs.getLong("id");
                        LOGGER.info("Created new account: " + accountCode + " with ID: " + newId);
                        return newId;
                    }
                }
            }

            // If we get here, the INSERT was skipped due to conflict, so get the existing account
            try (PreparedStatement selectStmt = conn.prepareStatement(
                     "SELECT id FROM accounts WHERE company_id = ? AND account_code = ?")) {

                selectStmt.setLong(1, companyId);
                selectStmt.setString(2, accountCode);

                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        Long existingId = rs.getLong("id");
                        LOGGER.info("Retrieved existing account after conflict: " + accountCode + " with ID: " + existingId);
                        return existingId;
                    }
                }
            }

            throw new RuntimeException("Failed to get or create account: " + accountCode);

        } catch (SQLException e) {
            LOGGER.severe("Error getting or creating account " + accountCode + ": " + e.getMessage());
            throw new RuntimeException("Database error", e);
        }
    }

    /**
     * Create a new detailed account
     *
     * @param accountCode The account code
     * @param accountName The account name
     * @param parentCode The parent account code
     * @param categoryName The account category name
     * @return The new account ID
     */
    public Long createDetailedAccount(String accountCode, String accountName,
                                    String parentCode, String categoryName) {
        try (Connection conn = getConnection()) {
            Long companyId = 2L; // Use company 2 which has full category set
            
            // First check if account already exists to prevent duplicate key violation
            try (PreparedStatement checkStmt = conn.prepareStatement(
                     "SELECT id FROM accounts WHERE company_id = ? AND account_code = ?")) {
                checkStmt.setLong(1, companyId);
                checkStmt.setString(2, accountCode);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        LOGGER.info("Account " + accountCode + " already exists, returning existing ID");
                        return rs.getLong("id");
                    }
                }
            }
            
            int categoryId = getCategoryIdForAccountCode(accountCode, companyId);
            
            // Look up parent account ID from parent code if provided
            Long parentAccountId = null;
            if (parentCode != null && !parentCode.isEmpty()) {
                parentAccountId = getAccountIdByCode(companyId, parentCode);
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO accounts (company_id, account_code, account_name, " +
                     "parent_account_id, category_id, is_active, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP) " +
                     "RETURNING id",
                     Statement.RETURN_GENERATED_KEYS)) {

                stmt.setLong(PARAM_COMPANY_ID, companyId); // Use company 2
                stmt.setString(PARAM_ACCOUNT_CODE, accountCode);
                stmt.setString(PARAM_ACCOUNT_NAME, accountName);
                if (parentAccountId != null) {
                    stmt.setLong(PARAM_PARENT_ACCOUNT_ID, parentAccountId);
                } else {
                    stmt.setNull(PARAM_PARENT_ACCOUNT_ID, java.sql.Types.INTEGER);
                }
                stmt.setInt(PARAM_CATEGORY_ID, categoryId);

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }

            throw new RuntimeException("Failed to create account, no ID returned");

        } catch (SQLException e) {
            // If it's a duplicate key error, try to get the existing account
            if (e.getMessage().contains("duplicate key value violates unique constraint")) {
                LOGGER.warning("Duplicate account detected for " + accountCode + ", attempting to retrieve existing");
                try (Connection conn = getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                         "SELECT id FROM accounts WHERE company_id = ? AND account_code = ?")) {
                    stmt.setLong(1, 2L);
                    stmt.setString(2, accountCode);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong("id");
                        }
                    }
                } catch (SQLException ex) {
                    LOGGER.severe("Failed to retrieve existing account after duplicate error: " + ex.getMessage());
                }
            }
            LOGGER.severe("Error creating account: " + e.getMessage());
            throw new RuntimeException("Database error", e);
        }
    }

    /**
     * Get account ID by account code
     *
     * @param companyId The company ID
     * @param accountCode The account code
     * @return The account ID or null if not found
     */
    public Long getAccountIdByCode(Long companyId, String accountCode) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id FROM accounts WHERE company_id = ? AND account_code = ?")) {

            stmt.setLong(1, companyId);
            stmt.setString(2, accountCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }

            return null;

        } catch (SQLException e) {
            LOGGER.severe("Error getting account by code: " + e.getMessage());
            throw new RuntimeException("Database error", e);
        }
    }

    /**
     * Map account code to category ID based on South African Chart of Accounts
     * 1000-1999: Current Assets
     * 2000-2999: Non-Current Assets (Property, Equipment)  
     * 3000-3999: Current Liabilities
     * 4000-4999: Non-Current Liabilities
     * 5000-5999: Owner's Equity
     * 6000-6999: Operating Revenue
     * 7000-7999: Other Income
     * 8000-8999: Operating Expenses
     * 9000-9999: Finance Costs
     *
     * @param accountCode The account code (e.g., "1000-001")
     * @return The category ID
     */
    public int getCategoryIdForAccountCode(String accountCode) {
        return getCategoryIdForAccountCode(accountCode, 2L); // Default to company 2 (has full categories)
    }
    
    public int getCategoryIdForAccountCode(String accountCode, Long companyId) {
        if (accountCode == null || accountCode.length() < 1) {
            return getOperatingExpensesCategoryId(companyId);
        }

        char firstDigit = accountCode.charAt(0);
        
        switch (firstDigit) {
            case '1': // 1000-1999: Current Assets
                return getCurrentAssetsCategoryId(companyId);
            case '2': // 2000-2999: Non-Current Assets  
                return getNonCurrentAssetsCategoryId(companyId);
            case '3': // 3000-3999: Current Liabilities
                return getCurrentLiabilitiesCategoryId(companyId);
            case '4': // 4000-4999: Non-Current Liabilities
                return getNonCurrentLiabilitiesCategoryId(companyId);
            case '5': // 5000-5999: Owner's Equity
                return getEquityCategoryId(companyId);
            case '6': // 6000-6999: Operating Revenue
                return getOperatingRevenueCategoryId(companyId);
            case '7': // 7000-7999: Other Income
                return getOtherIncomeCategoryId(companyId);
            case '8': // 8000-8999: Operating Expenses
                return getOperatingExpensesCategoryId(companyId);
            case '9': // 9000-9999: Finance Costs
                return getFinanceCostsCategoryId(companyId);
            default:
                return getOperatingExpensesCategoryId(companyId);
        }
    }
    
    private int getCurrentAssetsCategoryId(Long companyId) {
        return companyId == 1L ? CATEGORY_CURRENT_ASSETS_COMPANY1 : CATEGORY_CURRENT_ASSETS_COMPANY2;
    }
    
    private int getNonCurrentAssetsCategoryId(Long companyId) {
        return companyId == 1L ? CATEGORY_CURRENT_ASSETS_COMPANY1 : CATEGORY_NON_CURRENT_ASSETS_COMPANY2; // Company 1 doesn't have Non-Current Assets category
    }
    
    private int getCurrentLiabilitiesCategoryId(Long companyId) {
        return companyId == 1L ? CATEGORY_LIABILITIES_COMPANY1 : CATEGORY_CURRENT_LIABILITIES_COMPANY2;
    }
    
    private int getNonCurrentLiabilitiesCategoryId(Long companyId) {
        return companyId == 1L ? CATEGORY_LIABILITIES_COMPANY1 : CATEGORY_NON_CURRENT_LIABILITIES_COMPANY2; // Company 1 doesn't have Non-Current Liabilities category
    }
    
    private int getEquityCategoryId(Long companyId) {
        return companyId == 1L ? CATEGORY_LIABILITIES_COMPANY1 : CATEGORY_EQUITY_COMPANY2; // Company 1 doesn't have Equity category
    }
    
    private int getOperatingRevenueCategoryId(Long companyId) {
        return companyId == 1L ? CATEGORY_REVENUE_EXPENSES_COMPANY1 : CATEGORY_OPERATING_REVENUE_COMPANY2; // Company 1 doesn't have Revenue category, fallback to Operating Expenses
    }
    
    private int getOtherIncomeCategoryId(Long companyId) {
        return companyId == 1L ? CATEGORY_REVENUE_EXPENSES_COMPANY1 : CATEGORY_OTHER_INCOME_COMPANY2; // Company 1 doesn't have Other Income category
    }
    
    private int getOperatingExpensesCategoryId(Long companyId) {
        return companyId == 1L ? CATEGORY_REVENUE_EXPENSES_COMPANY1 : CATEGORY_OPERATING_EXPENSES_COMPANY2;
    }
    
    private int getFinanceCostsCategoryId(Long companyId) {
        return companyId == 1L ? CATEGORY_REVENUE_EXPENSES_COMPANY1 : CATEGORY_FINANCE_COSTS_COMPANY2; // Company 1 doesn't have Finance Costs category
    }

    /**
     * Get database connection - can be overridden for testing
     */
    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }
}
