package fin.service;

import fin.model.*;
import fin.repository.JdbcBaseRepository;
import fin.exception.TransactionMappingException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing transaction mapping rules.
 * Follows Single Responsibility Principle - handles only mapping rule CRUD operations.
 */
public class TransactionMappingRuleService extends JdbcBaseRepository {
    private static final Logger LOGGER = Logger.getLogger(TransactionMappingRuleService.class.getName());

    // Cache for better performance
    private final Map<Long, List<TransactionMappingRule>> mappingRulesByCompany = new HashMap<>();

    public TransactionMappingRuleService(String dbUrl) {
        super(dbUrl);
    }

    /**
     * Saves a transaction mapping rule to the database.
     */
    public void saveTransactionMappingRule(TransactionMappingRule rule) {
        String sql =
            "INSERT INTO transaction_mapping_rules " +
            "(company_id, rule_name, description, match_type, match_value, " +
            "account_id, is_active, priority) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "rule_name = excluded.rule_name, " +
            "description = excluded.description, " +
            "match_type = excluded.match_type, " +
            "match_value = excluded.match_value, " +
            "account_id = excluded.account_id, " +
            "is_active = excluded.is_active, " +
            "priority = excluded.priority, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "RETURNING id, created_at, updated_at";

        try {
            executeQuery(sql, rs -> {
                if (rs.next()) {
                    rule.setId(rs.getLong("id"));
                    rule.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    rule.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
                return rule;
            }, rule.getCompany().getId(), rule.getRuleName(), rule.getDescription(),
               rule.getMatchType().name(), rule.getMatchValue(), rule.getAccount().getId(),
               rule.isActive(), rule.getPriority());

            // Clear cache
            mappingRulesByCompany.remove(rule.getCompany().getId());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving transaction mapping rule", e);
            throw new TransactionMappingException("Failed to save transaction mapping rule", e);
        }
    }

    /**
     * Retrieves all transaction mapping rules for a company.
     */
    public List<TransactionMappingRule> getTransactionMappingRules(Long companyId) {
        // Check cache first
        if (mappingRulesByCompany.containsKey(companyId)) {
            return mappingRulesByCompany.get(companyId);
        }

        String sql =
            "SELECT r.*, a.account_code, a.account_name, " +
            "c.name as category_name, t.code as type_code " +
            "FROM transaction_mapping_rules r " +
            "JOIN accounts a ON r.account_id = a.id " +
            "JOIN account_categories c ON a.category_id = c.id " +
            "JOIN account_types t ON c.account_type_id = t.id " +
            "WHERE r.company_id = ? AND r.is_active = 1 " +
            "ORDER BY r.priority DESC, r.rule_name";

        try {
            List<TransactionMappingRule> rules = executeQuery(sql, rs -> buildRuleFromResultSet(rs, companyId), companyId);

            // Update cache
            mappingRulesByCompany.put(companyId, rules);

            return rules;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching transaction mapping rules", e);
            throw new TransactionMappingException("Failed to fetch transaction mapping rules", e);
        }
    }

    /**
     * Finds the best matching account for a transaction description
     * by applying the transaction mapping rules in priority order.
     */
    public Optional<Account> findMatchingAccount(Long companyId, String description) {
        if (description == null || description.trim().isEmpty()) {
            return Optional.empty();
        }

        return getTransactionMappingRules(companyId).stream()
            .filter(rule -> rule.matches(description))
            .findFirst()
            .map(TransactionMappingRule::getAccount);
    }

    /**
     * Builds a TransactionMappingRule object from a ResultSet.
     */
    private TransactionMappingRule buildRuleFromResultSet(ResultSet rs, Long companyId) throws SQLException {
        TransactionMappingRule rule = new TransactionMappingRule();
        rule.setId(rs.getLong("id"));
        rule.setRuleName(rs.getString("rule_name"));
        rule.setDescription(rs.getString("description"));
        rule.setMatchType(TransactionMappingRule.MatchType.valueOf(
            rs.getString("match_type")));
        rule.setMatchValue(rs.getString("match_value"));
        rule.setActive(rs.getBoolean("is_active"));
        rule.setPriority(rs.getInt("priority"));
        rule.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        rule.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        // Set account (lightweight)
        Account account = new Account();
        account.setId(rs.getLong("account_id"));
        account.setAccountCode(rs.getString("account_code"));
        account.setAccountName(rs.getString("account_name"));

        // Set category (lightweight)
        AccountCategory category = new AccountCategory();
        category.setName(rs.getString("category_name"));
        category.setAccountType(AccountType.fromCode(rs.getString("type_code")));
        account.setCategory(category);

        rule.setAccount(account);

        // Set company (lightweight, just ID)
        Company company = new Company();
        company.setId(companyId);
        rule.setCompany(company);

        return rule;
    }

    /**
     * Clears the cache for a specific company.
     */
    public void clearCache(Long companyId) {
        mappingRulesByCompany.remove(companyId);
    }
}