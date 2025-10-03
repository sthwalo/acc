package fin.repository;

import fin.model.BankTransaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

/**
 * Repository implementation for BankTransaction entities.
 * Handles all database operations related to transactions.
 */
public class BankTransactionRepository implements BaseRepository<BankTransaction, Long> {
    private final String dbUrl;

    public BankTransactionRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    @Override
    public BankTransaction save(BankTransaction transaction) {
        String sql = transaction.getId() == null ? 
            "INSERT INTO bank_transactions (company_id, fiscal_period_id, transaction_date, details, debit_amount, credit_amount, balance, service_fee, account_number, statement_period, source_file) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" :
            "UPDATE bank_transactions SET company_id = ?, fiscal_period_id = ?, transaction_date = ?, details = ?, debit_amount = ?, credit_amount = ?, balance = ?, service_fee = ?, account_number = ?, statement_period = ?, source_file = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            int paramIndex = 1;
            stmt.setLong(paramIndex++, transaction.getCompanyId());
            stmt.setLong(paramIndex++, transaction.getFiscalPeriodId());
            stmt.setDate(paramIndex++, Date.valueOf(transaction.getTransactionDate()));
            stmt.setString(paramIndex++, transaction.getDetails());
            stmt.setBigDecimal(paramIndex++, transaction.getDebitAmount());
            stmt.setBigDecimal(paramIndex++, transaction.getCreditAmount());
            stmt.setBigDecimal(paramIndex++, transaction.getBalance());
            stmt.setBoolean(paramIndex++, transaction.isServiceFee());
            stmt.setString(paramIndex++, transaction.getAccountNumber());
            stmt.setString(paramIndex++, transaction.getStatementPeriod());
            stmt.setString(paramIndex++, transaction.getSourceFile());
            
            if (transaction.getId() != null) {
                stmt.setLong(paramIndex, transaction.getId());
            }

            stmt.executeUpdate();

            if (transaction.getId() == null) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    transaction.setId(rs.getLong(1));
                }
            }

            return transaction;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving bank transaction", e);
        }
    }

    @Override
    public Optional<BankTransaction> findById(Long id) {
        String sql = "SELECT * FROM bank_transactions WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToTransaction(rs));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error finding bank transaction by id", e);
        }
    }

    @Override
    public List<BankTransaction> findAll() {
        String sql = "SELECT * FROM bank_transactions";
        List<BankTransaction> transactions = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
            
            return transactions;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all bank transactions", e);
        }
    }

    @Override
    public void delete(BankTransaction transaction) {
        if (transaction.getId() != null) {
            deleteById(transaction.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM bank_transactions WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting bank transaction", e);
        }
    }

    @Override
    public boolean exists(Long id) {
        String sql = "SELECT 1 FROM bank_transactions WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Error checking bank transaction existence", e);
        }
    }

    private BankTransaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(rs.getLong("id"));
        transaction.setCompanyId(rs.getLong("company_id"));
        transaction.setFiscalPeriodId(rs.getLong("fiscal_period_id"));
        transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
        transaction.setDetails(rs.getString("details"));
        transaction.setDebitAmount(rs.getBigDecimal("debit_amount"));
        transaction.setCreditAmount(rs.getBigDecimal("credit_amount"));
        transaction.setBalance(rs.getBigDecimal("balance"));
        transaction.setServiceFee(rs.getBoolean("service_fee"));
        transaction.setAccountNumber(rs.getString("account_number"));
        transaction.setStatementPeriod(rs.getString("statement_period"));
        transaction.setSourceFile(rs.getString("source_file"));
        return transaction;
    }

    // Additional query methods
    
    public List<BankTransaction> findByCompanyAndFiscalPeriod(Long companyId, Long fiscalPeriodId) {
        String sql = "SELECT * FROM bank_transactions WHERE company_id = ? AND fiscal_period_id = ?";
        List<BankTransaction> transactions = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            stmt.setLong(2, fiscalPeriodId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
            
            return transactions;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transactions by company and fiscal period", e);
        }
    }

    public List<BankTransaction> findByDateRange(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT * FROM bank_transactions WHERE transaction_date BETWEEN ? AND ?";
        List<BankTransaction> transactions = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
            
            return transactions;
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transactions by date range", e);
        }
    }
}
