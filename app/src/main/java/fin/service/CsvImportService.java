package fin.service;

import fin.model.BankTransaction;
import fin.model.FiscalPeriod;
import fin.config.DatabaseConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
public class CsvImportService {
    private final String dbUrl;
    private final AccountService accountService;
    private final CompanyService companyService;
    
    public CsvImportService(String dbUrl, CompanyService companyService) {
        this.dbUrl = dbUrl;
        this.companyService = companyService;
        this.accountService = new AccountService(dbUrl, companyService);
        initializeDatabase();
    }
    
    public AccountService getAccountService() {
        return accountService;
    }
    
    public void initializeDatabase() {
        // Skip table creation if using PostgreSQL - tables are created via migration script
        if (DatabaseConfig.isUsingPostgreSQL()) {
            System.out.println("📊 Using PostgreSQL - bank_transactions table already exists");
            return;
        }
        
        // SQLite table creation (for testing only)
        try (Connection connection = DriverManager.getConnection(dbUrl)) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS bank_transactions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "company_id INTEGER NOT NULL, " +
                    "bank_account_id INTEGER, " +
                    "fiscal_period_id INTEGER NOT NULL, " +
                    "transaction_date DATE NOT NULL, " +  // Changed to DATE type
                    "details TEXT, " +
                    "debit_amount REAL, " +
                    "credit_amount REAL, " +
                    "balance REAL, " +
                    "service_fee INTEGER DEFAULT 0, " +
                    "account_number TEXT, " +
                    "statement_period TEXT, " +
                    "source_file TEXT, " +
                    "created_at DATETIME DEFAULT (datetime('now')), " +  // Use SQLite datetime function
                    "FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                    "FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id) ON DELETE RESTRICT ON UPDATE CASCADE, " +
                    "FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id) ON DELETE SET NULL ON UPDATE CASCADE)";
            
            try (PreparedStatement statement = connection.prepareStatement(createTableSQL)) {
                statement.executeUpdate();
            }
            
            // Create indices for better performance
            String[] indices = {
                "CREATE INDEX IF NOT EXISTS idx_bank_transactions_company ON bank_transactions(company_id)",
                "CREATE INDEX IF NOT EXISTS idx_bank_transactions_date ON bank_transactions(transaction_date)",
                "CREATE INDEX IF NOT EXISTS idx_bank_transactions_fiscal_period ON bank_transactions(fiscal_period_id)"
            };
            
            for (String index : indices) {
                try (PreparedStatement statement = connection.prepareStatement(index)) {
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    /**
     * Retrieves transactions for a specific company and fiscal period
     * 
     * @param companyId The ID of the company
     * @param fiscalPeriodId The ID of the fiscal period
     * @return List of bank transactions
     */
    public List<BankTransaction> getTransactions(Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> transactions = new ArrayList<>();
        
        try (Connection connection = DriverManager.getConnection(dbUrl)) {
            String sql = "SELECT * FROM bank_transactions WHERE company_id = ? AND fiscal_period_id = ? ORDER BY transaction_date";
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, companyId);
                statement.setLong(2, fiscalPeriodId);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        BankTransaction transaction = new BankTransaction();
                        transaction.setId(resultSet.getLong("id"));
                        transaction.setCompanyId(resultSet.getLong("company_id"));
                        
                        Long bankAccountId = resultSet.getObject("bank_account_id") != null ? 
                                resultSet.getLong("bank_account_id") : null;
                        transaction.setBankAccountId(bankAccountId);
                        
                        // Parse date from SQLite ISO8601 string
                        String dateStr = resultSet.getString("transaction_date");
                        if (dateStr != null) {
                            transaction.setTransactionDate(LocalDate.parse(dateStr));
                        }
                        
                        transaction.setDetails(resultSet.getString("details"));
                        
                        // Handle BigDecimal values
                        BigDecimal debitAmount = resultSet.getBigDecimal("debit_amount");
                        if (debitAmount != null) {
                            transaction.setDebitAmount(debitAmount);
                        }
                        
                        BigDecimal creditAmount = resultSet.getBigDecimal("credit_amount");
                        if (creditAmount != null) {
                            transaction.setCreditAmount(creditAmount);
                        }
                        
                        BigDecimal balance = resultSet.getBigDecimal("balance");
                        if (balance != null) {
                            transaction.setBalance(balance);
                        }
                        
                        transaction.setServiceFee(resultSet.getBoolean("service_fee"));
                        transaction.setAccountNumber(resultSet.getString("account_number"));
                        transaction.setStatementPeriod(resultSet.getString("statement_period"));
                        transaction.setSourceFile(resultSet.getString("source_file"));
                        
                        transactions.add(transaction);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving transactions: " + e.getMessage());
            e.printStackTrace();
        }
        
        return transactions;
    }
    
    public List<BankTransaction> importCsvFile(String filePath, Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> transactions = new ArrayList<>();
        FiscalPeriod selectedPeriod = companyService.getFiscalPeriodById(fiscalPeriodId);
        if (selectedPeriod == null) {
            throw new RuntimeException("Selected fiscal period not found: " + fiscalPeriodId);
        }

        int importedCount = 0;
        int skippedCount = 0;
        boolean isFY2025 = "FY2025".equals(selectedPeriod.getPeriodName());

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;

            System.out.println("Starting import with fiscal period: " + selectedPeriod.getPeriodName() + 
                               " (" + selectedPeriod.getStartDate() + " to " + selectedPeriod.getEndDate() + ")");
            System.out.println("IMPORTANT: For FY2025, we will also import FY2024-2025 transactions");

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header line
                }

                String[] fields = parseCsvLine(line);
                if (fields.length < 10) continue; // Skip invalid lines (need at least 10 columns including fiscal period)

                try {
                    BankTransaction transaction = new BankTransaction();
                    transaction.setCompanyId(companyId);

                    // Parse date (DD/MM)
                    String dateStr = fields[0].trim();
                    
                    // Get the fiscal period from the CSV
                    String csvFiscalPeriodStr = fields[9].trim(); // FY2024-2025 format
                    
                    // Use fiscal year from selected period for date parsing
                    String fiscalYear;
                    if (selectedPeriod.getPeriodName().startsWith("FY")) {
                        fiscalYear = selectedPeriod.getPeriodName().substring(2, 6);
                    } else {
                        // Extract year from start date if period name format is different
                        fiscalYear = String.valueOf(selectedPeriod.getStartDate().getYear());
                    }

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    LocalDate transactionDate = LocalDate.parse(dateStr + "/" + fiscalYear, formatter);

                    // Adjust year if date falls outside fiscal period
                    if (transactionDate.isBefore(selectedPeriod.getStartDate())) {
                        transactionDate = transactionDate.plusYears(1);
                    } else if (transactionDate.isAfter(selectedPeriod.getEndDate())) {
                        transactionDate = transactionDate.minusYears(1);
                    }

                    transaction.setTransactionDate(transactionDate);
                    transaction.setDetails(fields[1].trim());

                    // Parse debit amount (if present)
                    if (fields[2] != null && !fields[2].trim().isEmpty()) {
                        String debitStr = fields[2].replaceAll("[^\\d.]", "");
                        if (!debitStr.isEmpty()) {
                            transaction.setDebitAmount(new BigDecimal(debitStr));
                        }
                    }

                    // Parse credit amount (if present)
                    if (fields[3] != null && !fields[3].trim().isEmpty()) {
                        String creditStr = fields[3].replaceAll("[^\\d.]", "");
                        if (!creditStr.isEmpty()) {
                            transaction.setCreditAmount(new BigDecimal(creditStr));
                        }
                    }

                    // Parse balance
                    if (fields[4] != null && !fields[4].trim().isEmpty()) {
                        String balanceStr = fields[4].replaceAll("[^\\d.]", "");
                        if (!balanceStr.isEmpty()) {
                            transaction.setBalance(new BigDecimal(balanceStr));
                        }
                    }

                    // Set service fee
                    transaction.setServiceFee("Y".equalsIgnoreCase(fields[5].trim()));

                    // Set account number
                    if (fields[6] != null && !fields[6].trim().isEmpty()) {
                        String accNum = fields[6].trim();
                        transaction.setAccountNumber(accNum);
                    }

                    // Set statement period
                    if (fields[7] != null && !fields[7].trim().isEmpty()) {
                        transaction.setStatementPeriod(fields[7].trim());
                    }

                    // Set source file
                    if (fields[8] != null && !fields[8].trim().isEmpty()) {
                        transaction.setSourceFile(fields[8].trim());
                    }

                    // IMPORTANT: Import based on fiscal period column in CSV, not just date
                    // Check if this transaction belongs to our target fiscal period
                    // Match by period name with flexible matching for FY formats (FY2025 vs FY2024-2025)
                    boolean fiscalPeriodMatch = false;
                    String selectedPeriodName = selectedPeriod.getPeriodName();
                    
                    // Direct match
                    if (csvFiscalPeriodStr.equals(selectedPeriodName)) {
                        fiscalPeriodMatch = true;
                    } 
                    // Special case: If CSV has FY2024-2025 and we selected FY2025 (using our flag)
                    else if (isFY2025 && "FY2024-2025".equals(csvFiscalPeriodStr)) {
                        fiscalPeriodMatch = true;
                        System.out.println("Found FY2024-2025 transaction to include in FY2025: " + 
                                          transaction.getDetails() + ", Date: " + transactionDate);
                    }
                    // Handle FY2025 vs FY2024-2025 format differences (general case)
                    else if (selectedPeriodName.startsWith("FY") && csvFiscalPeriodStr.contains("-")) {
                        // If selected is FY2025, extract the end year from FY2024-2025 to compare
                        String csvEndYear = "";
                        if (csvFiscalPeriodStr.length() >= 9) {
                            csvEndYear = csvFiscalPeriodStr.substring(7); // Get "2025" from "FY2024-2025"
                        }
                        
                        // If selected period is FY2025, check if CSV has FY2024-2025
                        if (selectedPeriodName.length() >= 6 && 
                            selectedPeriodName.equals("FY" + csvEndYear)) {
                            fiscalPeriodMatch = true;
                            System.out.println("Pattern matched transaction: " + csvFiscalPeriodStr + 
                                               " for period " + selectedPeriodName);
                        }
                    }

                    // Debug: Output when period mismatches occur but we should probably include them
                    if (!fiscalPeriodMatch && csvFiscalPeriodStr.contains("2024-2025") && selectedPeriodName.equals("FY2025")) {
                        System.out.println("Potential match missed: CSV period=" + csvFiscalPeriodStr + 
                                          ", Selected period=" + selectedPeriodName +
                                          ", Transaction date=" + transactionDate);
                    }

                    // Import if period matches OR date is within range OR special case for FY2025
                    boolean isSpecialFY2025Case = isFY2025 && "FY2024-2025".equals(csvFiscalPeriodStr);
                    
                    if (fiscalPeriodMatch || isSpecialFY2025Case || 
                        (!transactionDate.isBefore(selectedPeriod.getStartDate()) && 
                         !transactionDate.isAfter(selectedPeriod.getEndDate()))) {
                        
                        transaction.setFiscalPeriodId(fiscalPeriodId);
                        transactions.add(transaction);
                        saveTransaction(transaction);
                        importedCount++;
                        
                        // Track credits total for FY2024-2025 records specifically
                        if ("FY2024-2025".equals(csvFiscalPeriodStr) && transaction.getCreditAmount() != null) {
                            System.out.println("Adding credit from FY2024-2025: " + transaction.getCreditAmount() + 
                                              " - " + transaction.getDetails());
                        }
                    } else {
                        skippedCount++;
                        // Debug output for skipped FY2024-2025 transactions
                        if ("FY2024-2025".equals(csvFiscalPeriodStr)) {
                            System.out.println("SKIPPED FY2024-2025 transaction! " + 
                                              "Date: " + transactionDate + 
                                              ", Details: " + transaction.getDetails());
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Error processing line: " + line);
                    e.printStackTrace();
                    // Continue processing other lines
                }
            }

            return transactions;

        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            throw new RuntimeException("Failed to read CSV file", e);
        } finally {
            System.out.println("Import summary: " + importedCount + " transactions imported, " + 
                               skippedCount + " transactions skipped.");
        }
    }
    
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        
        result.add(field.toString());
        return result.toArray(new String[0]);
    }
    


    private void saveTransaction(BankTransaction transaction) {
        String sql = "INSERT INTO bank_transactions (company_id, bank_account_id, transaction_date, " +
                "details, debit_amount, credit_amount, balance, service_fee, statement_period, " +
                "source_file, fiscal_period_id, account_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setLong(1, transaction.getCompanyId());
            if (transaction.getBankAccountId() != null) {
                pstmt.setLong(2, transaction.getBankAccountId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            // Store date in SQLite ISO8601 format: YYYY-MM-DD
            pstmt.setString(3, transaction.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            pstmt.setString(4, transaction.getDetails());
            if (transaction.getDebitAmount() != null) {
                pstmt.setBigDecimal(5, transaction.getDebitAmount());
            } else {
                pstmt.setNull(5, Types.DECIMAL);
            }
            if (transaction.getCreditAmount() != null) {
                pstmt.setBigDecimal(6, transaction.getCreditAmount());
            } else {
                pstmt.setNull(6, Types.DECIMAL);
            }
            if (transaction.getBalance() != null) {
                pstmt.setBigDecimal(7, transaction.getBalance());
            } else {
                pstmt.setNull(7, Types.DECIMAL);
            }
            pstmt.setBoolean(8, transaction.isServiceFee());
            pstmt.setString(9, transaction.getStatementPeriod());
            pstmt.setString(10, transaction.getSourceFile());
            if (transaction.getFiscalPeriodId() != null) {
                pstmt.setLong(11, transaction.getFiscalPeriodId());
            } else {
                pstmt.setNull(11, Types.INTEGER);
            }
            pstmt.setString(12, transaction.getAccountNumber());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    transaction.setId(generatedKeys.getLong(1));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
            // Just log the error but continue processing other transactions
        }
    }
    
    public List<BankTransaction> getTransactionsByFiscalPeriod(Long fiscalPeriodId) {
        String sql = "SELECT * FROM bank_transactions WHERE fiscal_period_id = ?";
        List<BankTransaction> transactions = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                BankTransaction transaction = new BankTransaction();
                transaction.setId(rs.getLong("id"));
                transaction.setCompanyId(rs.getLong("company_id"));
                if (rs.getObject("bank_account_id") != null) {
                    transaction.setBankAccountId(rs.getLong("bank_account_id"));
                }
                transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
                transaction.setDetails(rs.getString("details"));
                if (rs.getObject("debit_amount") != null) {
                    transaction.setDebitAmount(rs.getBigDecimal("debit_amount"));
                }
                if (rs.getObject("credit_amount") != null) {
                    transaction.setCreditAmount(rs.getBigDecimal("credit_amount"));
                }
                if (rs.getObject("balance") != null) {
                    transaction.setBalance(rs.getBigDecimal("balance"));
                }
                transaction.setServiceFee(rs.getBoolean("service_fee"));
                transaction.setStatementPeriod(rs.getString("statement_period"));
                transaction.setSourceFile(rs.getString("source_file"));
                transaction.setFiscalPeriodId(rs.getLong("fiscal_period_id"));
                transaction.setAccountNumber(rs.getString("account_number"));
                transaction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                transactions.add(transaction);
            }
            
            return transactions;
            
        } catch (SQLException e) {
            System.err.println("Error getting transactions: " + e.getMessage());
            throw new RuntimeException("Failed to get transactions", e);
        }
    }
}
