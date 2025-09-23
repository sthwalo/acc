package fin.service;

import fin.model.RuleMapping;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * RuleMappingService - Manages database-driven transaction classification rules
 *
 * This service handles the creation, retrieval, and application of transaction
 * mapping rules that automatically classify bank transactions based on their
 * descriptions. It integrates with the database to persist and retrieve rules.
 */
public class RuleMappingService {
    private static final Logger LOGGER = Logger.getLogger(RuleMappingService.class.getName());

    private final String dbUrl;

    public RuleMappingService(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /**
     * Load all active transaction mapping rules for a company
     *
     * @param companyId The company ID
     * @return Map of pattern text to RuleMapping objects
     */
    public Map<String, RuleMapping> loadTransactionMappingRules(Long companyId) {
        Map<String, RuleMapping> rules = new HashMap<>();

        String sql = """
            SELECT r.match_value, a.account_code, a.account_name
            FROM transaction_mapping_rules r
            JOIN accounts a ON r.account_id = a.id
            WHERE r.company_id = ? AND r.is_active = true
            ORDER BY r.priority DESC, r.created_at DESC
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String matchValue = rs.getString("match_value");
                    String accountCode = rs.getString("account_code");
                    String accountName = rs.getString("account_name");

                    rules.put(matchValue.toUpperCase(), new RuleMapping(accountCode, accountName));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading transaction mapping rules for company: " + companyId, e);
        }

        return rules;
    }

    /**
     * Find a matching rule for the given transaction description
     *
     * @param description The transaction description
     * @param rules The map of rules to search
     * @return The matching RuleMapping or null if no match found
     */
    public RuleMapping findMatchingRule(String description, Map<String, RuleMapping> rules) {
        if (description == null || rules == null || rules.isEmpty()) {
            return null;
        }

        String upperDescription = description.toUpperCase();

        // Check for exact matches first
        RuleMapping exactMatch = rules.get(upperDescription);
        if (exactMatch != null) {
            return exactMatch;
        }

        // Check for partial matches
        for (Map.Entry<String, RuleMapping> entry : rules.entrySet()) {
            String pattern = entry.getKey();
            if (upperDescription.contains(pattern)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Create standard mapping rules for a company
     *
     * @param companyId The company ID
     * @return The number of rules created
     */
    public int createStandardMappingRules(Long companyId) {
        List<StandardRule> standardRules = Arrays.asList(
            new StandardRule("SALARY", "8100", "Employee Costs"),
            new StandardRule("INSURANCE PREMIUM", "8800", "Insurance"),
            new StandardRule("BANK FEE", "9600", "Bank Charges"),
            new StandardRule("FUEL", "9500", "Fuel Expenses"),
            new StandardRule("DIRECTOR LOAN", "2689327", "Director Loans"),
            new StandardRule("SERVICE FEE", "9600", "Bank Charges"),
            new StandardRule("ATM", "9600", "Bank Charges"),
            new StandardRule("EFT", "9600", "Bank Charges")
        );

        int created = 0;
        for (StandardRule rule : standardRules) {
            try {
                createMappingRule(companyId, rule.pattern, rule.accountCode, rule.accountName);
                created++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to create rule for pattern: " + rule.pattern, e);
            }
        }

        return created;
    }

    /**
     * Create a new mapping rule
     *
     * @param companyId The company ID
     * @param pattern The pattern to match
     * @param accountCode The account code
     * @param accountName The account name
     */
    public void createMappingRule(Long companyId, String pattern, String accountCode, String accountName) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // Check if rule already exists
            if (ruleExists(conn, companyId, pattern)) {
                LOGGER.info("Rule already exists for pattern: " + pattern);
                return;
            }

            // Get or create account
            Long accountId = getOrCreateAccount(conn, accountCode, accountName, companyId);

            // Create the rule
            String sql = """
                INSERT INTO transaction_mapping_rules (company_id, rule_name, match_type, match_value, account_id, is_active, priority, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, true, 0, ?, ?)
                """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, companyId);
                stmt.setString(2, "Auto-generated rule for " + pattern);
                stmt.setString(3, "CONTAINS");
                stmt.setString(4, pattern.toUpperCase());
                stmt.setLong(5, accountId);
                stmt.setTimestamp(6, Timestamp.valueOf(java.time.LocalDateTime.now()));
                stmt.setTimestamp(7, Timestamp.valueOf(java.time.LocalDateTime.now()));

                stmt.executeUpdate();
            }

            conn.commit();
            LOGGER.info("Created mapping rule: " + pattern + " -> " + accountCode);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating mapping rule for pattern: " + pattern, e);
            throw new RuntimeException("Failed to create mapping rule", e);
        }
    }

    /**
     * Clear all existing mapping rules for a company
     *
     * @param companyId The company ID
     */
    public void clearExistingMappingRules(Long companyId) {
        String sql = "DELETE FROM transaction_mapping_rules WHERE company_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            int deleted = stmt.executeUpdate();
            LOGGER.info("Cleared " + deleted + " existing mapping rules for company: " + companyId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error clearing mapping rules for company: " + companyId, e);
            throw new RuntimeException("Failed to clear mapping rules", e);
        }
    }

    /**
     * Create the transaction mapping rules table if it doesn't exist
     */
    public void createTransactionMappingRulesTable() {
        try (Connection conn = getConnection()) {
            // Check if table exists
            if (tableExists(conn, "transaction_mapping_rules")) {
                LOGGER.info("Transaction mapping rules table already exists");
                return;
            }

            // Create the table
            String createTableSql = """
                CREATE TABLE transaction_mapping_rules (
                    id SERIAL PRIMARY KEY,
                    company_id BIGINT NOT NULL,
                    rule_name VARCHAR(255) NOT NULL,
                    description TEXT,
                    match_type VARCHAR(20) NOT NULL DEFAULT 'CONTAINS',
                    match_value VARCHAR(500) NOT NULL,
                    account_id BIGINT NOT NULL,
                    is_active BOOLEAN NOT NULL DEFAULT true,
                    priority INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (company_id) REFERENCES companies(id),
                    FOREIGN KEY (account_id) REFERENCES accounts(id)
                )
                """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSql);

                // Create indexes
                stmt.execute("CREATE INDEX idx_tmr_company_active ON transaction_mapping_rules(company_id, is_active)");
                stmt.execute("CREATE INDEX idx_tmr_match_value ON transaction_mapping_rules(match_value)");
                stmt.execute("CREATE INDEX idx_tmr_priority ON transaction_mapping_rules(priority DESC)");
            }

            LOGGER.info("Created transaction mapping rules table");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating transaction mapping rules table", e);
            throw new RuntimeException("Failed to create transaction mapping rules table", e);
        }
    }

    /**
     * Get database connection - can be overridden for testing
     */
    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private boolean ruleExists(Connection conn, Long companyId, String pattern) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transaction_mapping_rules WHERE company_id = ? AND UPPER(match_value) = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, companyId);
            stmt.setString(2, pattern.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Long getOrCreateAccount(Connection conn, String accountCode, String accountName, Long companyId) throws SQLException {
        // Check if account exists
        String selectSql = "SELECT id FROM accounts WHERE company_id = ? AND account_code = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setLong(1, companyId);
            stmt.setString(2, accountCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }

        // Create account
        String insertSql = """
            INSERT INTO accounts (company_id, account_code, account_name, account_type, category, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, companyId);
            stmt.setString(2, accountCode);
            stmt.setString(3, accountName);
            stmt.setString(4, getAccountType(accountCode));
            stmt.setString(5, getAccountCategory(accountCode));
            stmt.setTimestamp(6, Timestamp.valueOf(java.time.LocalDateTime.now()));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("Failed to create account: " + accountCode);
    }

    private String getAccountType(String accountCode) {
        if (accountCode.startsWith("1")) return "ASSET";
        if (accountCode.startsWith("2")) return "LIABILITY";
        if (accountCode.startsWith("3")) return "EQUITY";
        if (accountCode.startsWith("4") || accountCode.startsWith("5") || accountCode.startsWith("6")) return "INCOME";
        if (accountCode.startsWith("7") || accountCode.startsWith("8") || accountCode.startsWith("9")) return "EXPENSE";
        return "EXPENSE";
    }

    private String getAccountCategory(String accountCode) {
        String mainCode = accountCode.substring(0, Math.min(4, accountCode.length()));
        return switch (mainCode) {
            case "8100" -> "Employee Costs";
            case "8800" -> "Insurance";
            case "9600" -> "Bank Charges";
            case "9500" -> "Fuel Expenses";
            default -> "Other";
        };
    }

    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName.toLowerCase(), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    /**
     * Inner class for standard rules
     */
    private static class StandardRule {
        final String pattern;
        final String accountCode;
        final String accountName;

        StandardRule(String pattern, String accountCode, String accountName) {
            this.pattern = pattern;
            this.accountCode = accountCode;
            this.accountName = accountName;
        }
    }
}