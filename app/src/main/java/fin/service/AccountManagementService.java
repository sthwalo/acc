package fin.service;

import fin.model.*;
import fin.repository.JdbcBaseRepository;
import fin.exception.AccountException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing accounts.
 * Follows Single Responsibility Principle - handles only account CRUD operations.
 */
public class AccountManagementService extends JdbcBaseRepository {
    private static final Logger LOGGER = Logger.getLogger(AccountManagementService.class.getName());

    // Cache for better performance
    private final Map<Long, List<Account>> accountsByCompany = new HashMap<>();

    public AccountManagementService(String dbUrl) {
        super(dbUrl);
    }

    /**
     * Creates a new account.
     */
    public Account createAccount(String code, String name, AccountCategory category,
                               Company company, String description) {
        Account account = new Account(code, name, category, company, description);
        saveAccount(account);
        return account;
    }

    /**
     * Saves an account to the database.
     */
    public void saveAccount(Account account) {
        String sql =
            "INSERT INTO accounts (account_code, account_name, description, " +
            "category_id, parent_account_id, company_id, is_active) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT(company_id, account_code) DO UPDATE SET " +
            "account_name = excluded.account_name, " +
            "description = excluded.description, " +
            "category_id = excluded.category_id, " +
            "parent_account_id = excluded.parent_account_id, " +
            "is_active = excluded.is_active, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "RETURNING id, created_at, updated_at";

        try {
            executeQuery(sql, rs -> {
                if (rs.next()) {
                    account.setId(rs.getLong("id"));
                    account.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    account.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
                return account;
            }, account.getAccountCode(), account.getAccountName(), account.getDescription(),
               account.getCategory().getId(),
               account.getParentAccount() != null ? account.getParentAccount().getId() : null,
               account.getCompany().getId(), account.isActive());

            // Clear cache
            accountsByCompany.remove(account.getCompany().getId());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving account", e);
            throw new AccountException("Failed to save account", e);
        }
    }

    /**
     * Retrieves all accounts for a company.
     */
    public List<Account> getAccountsByCompany(Long companyId) {
        // Check cache first
        if (accountsByCompany.containsKey(companyId)) {
            return accountsByCompany.get(companyId);
        }

        String sql =
            "SELECT a.*, c.id as category_id, c.name as category_name, " +
            "t.code as type_code, t.name as type_name, t.normal_balance " +
            "FROM accounts a " +
            "JOIN account_categories c ON a.category_id = c.id " +
            "JOIN account_types t ON c.account_type_id = t.id " +
            "WHERE a.company_id = ? AND a.is_active = 1 " +
            "ORDER BY a.account_code";

        try {
            List<Account> accounts = executeQuery(sql, rs -> buildAccountFromResultSet(rs, companyId), companyId);

            // Update cache
            accountsByCompany.put(companyId, accounts);

            return accounts;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching accounts", e);
            throw new AccountException("Failed to fetch accounts", e);
        }
    }

    /**
     * Finds an account by its code.
     */
    public Optional<Account> findAccountByCode(Long companyId, String accountCode) {
        return getAccountsByCompany(companyId).stream()
            .filter(a -> a.getAccountCode().equals(accountCode))
            .findFirst();
    }

    /**
     * Builds an Account object from a ResultSet.
     */
    private Account buildAccountFromResultSet(ResultSet rs, Long companyId) throws SQLException {
        Account account = new Account();
        account.setId(rs.getLong("id"));
        account.setAccountCode(rs.getString("account_code"));
        account.setAccountName(rs.getString("account_name"));
        account.setDescription(rs.getString("description"));
        account.setActive(rs.getBoolean("is_active"));

        // Set category
        AccountCategory category = new AccountCategory();
        category.setId(rs.getLong("category_id"));
        category.setName(rs.getString("category_name"));
        category.setAccountType(AccountType.fromCode(rs.getString("type_code")));
        account.setCategory(category);

        // Set company (lightweight, just ID)
        Company company = new Company();
        company.setId(companyId);
        account.setCompany(company);

        return account;
    }

    /**
     * Clears the cache for a specific company.
     */
    public void clearCache(Long companyId) {
        accountsByCompany.remove(companyId);
    }
}