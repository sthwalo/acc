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

    public AccountRepository(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * Get or create a detailed account with full information
     *
     * @param accountCode The account code (e.g., "2000-001")
     * @param accountName The account name
     * @param parentCode The parent account code (e.g., "2000")
     * @param categoryName The account category name
     * @return The account ID
     */
    public Long getOrCreateDetailedAccount(String accountCode, String accountName,
                                          String parentCode, String categoryName) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id FROM accounts WHERE company_id = ? AND account_code = ?")) {

            stmt.setLong(1, 1L); // Assuming company_id = 1 for now
            stmt.setString(2, accountCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }

            // Account doesn't exist, create it
            return createDetailedAccount(accountCode, accountName, parentCode, categoryName);

        } catch (SQLException e) {
            LOGGER.severe("Error getting or creating account: " + e.getMessage());
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
            int categoryId = getCategoryIdForAccountCode(accountCode);

            try (PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO accounts (company_id, account_code, account_name, " +
                     "parent_account_code, category_id, is_active, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP) " +
                     "RETURNING id",
                     Statement.RETURN_GENERATED_KEYS)) {

                stmt.setLong(1, 1L); // company_id
                stmt.setString(2, accountCode);
                stmt.setString(3, accountName);
                stmt.setString(4, parentCode);
                stmt.setInt(5, categoryId);

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }

            throw new RuntimeException("Failed to create account, no ID returned");

        } catch (SQLException e) {
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
     * Map account code to category ID based on the first digits
     *
     * @param accountCode The account code (e.g., "1000-001")
     * @return The category ID
     */
    public int getCategoryIdForAccountCode(String accountCode) {
        if (accountCode == null || accountCode.length() < 4) {
            return 18; // Default to Operating Expenses
        }

        String prefix = accountCode.substring(0, 4);

        // Current Assets (1000-1999)
        if (prefix.startsWith("1")) {
            return 11; // Current Assets
        }

        // Current Liabilities (2000-2999)
        if (prefix.startsWith("2")) {
            return 13; // Current Liabilities (used for director loans)
        }

        // Operating Revenue (4000-4999)
        if (prefix.startsWith("4")) {
            return 16; // Operating Revenue
        }

        // Other Income (5000-5999)
        if (prefix.startsWith("5")) {
            return 17; // Other Income
        }

        // Reversals & Adjustments (6000-6999)
        if (prefix.startsWith("6")) {
            return 18; // Operating Expenses (adjustments)
        }

        // Operating Expenses (8000-8999)
        if (prefix.startsWith("8")) {
            return 18; // Operating Expenses
        }

        // Finance Costs (9600-9699)
        if (prefix.startsWith("96")) {
            return 20; // Finance Costs
        }

        // Default to Operating Expenses
        return 18;
    }

    /**
     * Get database connection - can be overridden for testing
     */
    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }
}
