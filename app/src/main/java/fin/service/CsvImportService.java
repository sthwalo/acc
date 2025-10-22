package fin.service;

import fin.model.BankTransaction;
import fin.model.FiscalPeriod;
import fin.config.DatabaseConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
public final class CsvImportService {
    private final String dbUrl;
    private final AccountService accountService;
    private final CompanyService companyService;

    // SQL Parameter index constants for PreparedStatement operations
    // saveTransaction method parameters (12 total)
    private static final int SAVE_TRANSACTION_COMPANY_ID = 1;
    private static final int SAVE_TRANSACTION_BANK_ACCOUNT_ID = 2;
    private static final int SAVE_TRANSACTION_DATE = 3;
    private static final int SAVE_TRANSACTION_DETAILS = 4;
    private static final int SAVE_TRANSACTION_DEBIT_AMOUNT = 5;
    private static final int SAVE_TRANSACTION_CREDIT_AMOUNT = 6;
    private static final int SAVE_TRANSACTION_BALANCE = 7;
    private static final int SAVE_TRANSACTION_SERVICE_FEE = 8;
    private static final int SAVE_TRANSACTION_STATEMENT_PERIOD = 9;
    private static final int SAVE_TRANSACTION_SOURCE_FILE = 10;
    private static final int SAVE_TRANSACTION_FISCAL_PERIOD_ID = 11;
    private static final int SAVE_TRANSACTION_ACCOUNT_NUMBER = 12;

    // importCsvFile method parameters (for CSV field access)
    private static final int CSV_FIELD_DATE = 0;
    private static final int CSV_FIELD_DETAILS = 1;
    private static final int CSV_FIELD_DEBIT = 2;
    private static final int CSV_FIELD_CREDIT = 3;
    private static final int CSV_FIELD_BALANCE = 4;
    private static final int CSV_FIELD_SERVICE_FEE = 5;
    private static final int CSV_FIELD_ACCOUNT_NUMBER = 6;
    private static final int CSV_FIELD_STATEMENT_PERIOD = 7;
    private static final int CSV_FIELD_SOURCE_FILE = 8;
    private static final int CSV_FIELD_FISCAL_PERIOD = 9;

    // getTransactionsByFiscalPeriod method parameters
    private static final int GET_TRANSACTIONS_FISCAL_PERIOD_ID = 1;

    // getTransactions method parameters
    private static final int GET_TRANSACTIONS_COMPANY_ID = 1;
    private static final int GET_TRANSACTIONS_FISCAL_PERIOD_ID_PARAM = 2;

    // CSV parsing constants
    private static final int MIN_CSV_FIELDS = 10;  // Minimum required CSV fields
    private static final int FY_PERIOD_NAME_START_INDEX = 2;  // "FY" prefix length
    private static final int FY_PERIOD_NAME_END_INDEX = 6;    // "FY" + 4-digit year
    private static final int MIN_FISCAL_PERIOD_STR_LENGTH = 9; // "FY2024-2025" minimum length
    private static final int FISCAL_PERIOD_END_YEAR_START = 7; // Start index for end year in "FY2024-2025"
    private static final int MIN_SELECTED_PERIOD_LENGTH = 6;   // "FY" + 4-digit year minimum length
    
    public CsvImportService(String initialDbUrl, CompanyService initialCompanyService) {
        // Input validation
        if (initialDbUrl == null || initialDbUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Database URL is required");
        }
        if (initialCompanyService == null) {
            throw new IllegalArgumentException("CompanyService is required");
        }

        // Safe field assignments first
        this.dbUrl = initialDbUrl;
        this.companyService = initialCompanyService;

        // Risky operation with error handling
        try {
            this.accountService = new AccountService(initialDbUrl, initialCompanyService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize AccountService", e);
        }
        
        // Safe operation
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

        // This should not happen since we only use PostgreSQL now
        System.out.println("❌ Unsupported database type. Only PostgreSQL is supported.");
        // Don't throw exception - allow constructor to complete
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
                statement.setLong(GET_TRANSACTIONS_COMPANY_ID, companyId);
                statement.setLong(GET_TRANSACTIONS_FISCAL_PERIOD_ID_PARAM, fiscalPeriodId);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        BankTransaction transaction = new BankTransaction();
                        transaction.setId(resultSet.getLong("id"));
                        transaction.setCompanyId(resultSet.getLong("company_id"));
                        
                        Long bankAccountId = resultSet.getObject("bank_account_id") != null ? 
                                resultSet.getLong("bank_account_id") : null;
                        transaction.setBankAccountId(bankAccountId);
                        
                        // Parse date from database
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

        try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
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
                if (fields.length < MIN_CSV_FIELDS) continue; // Skip invalid lines (need at least 10 columns including fiscal period)

                try {
                    BankTransaction transaction = new BankTransaction();
                    transaction.setCompanyId(companyId);

                    // Parse date (DD/MM)
                    String dateStr = fields[CSV_FIELD_DATE].trim();
                    
                    // Get the fiscal period from the CSV
                    String csvFiscalPeriodStr = fields[CSV_FIELD_FISCAL_PERIOD].trim(); // FY2024-2025 format
                    
                    // Use fiscal year from selected period for date parsing
                    String fiscalYear;
                    if (selectedPeriod.getPeriodName().startsWith("FY")) {
                        fiscalYear = selectedPeriod.getPeriodName().substring(FY_PERIOD_NAME_START_INDEX, FY_PERIOD_NAME_END_INDEX);
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
                    transaction.setDetails(fields[CSV_FIELD_DETAILS].trim());

                    // Parse debit amount (if present)
                    if (fields[CSV_FIELD_DEBIT] != null && !fields[CSV_FIELD_DEBIT].trim().isEmpty()) {
                        String debitStr = fields[CSV_FIELD_DEBIT].replaceAll("[^\\d.]", "");
                        if (!debitStr.isEmpty()) {
                            transaction.setDebitAmount(new BigDecimal(debitStr));
                        }
                    }

                    // Parse credit amount (if present)
                    if (fields[CSV_FIELD_CREDIT] != null && !fields[CSV_FIELD_CREDIT].trim().isEmpty()) {
                        String creditStr = fields[CSV_FIELD_CREDIT].replaceAll("[^\\d.]", "");
                        if (!creditStr.isEmpty()) {
                            transaction.setCreditAmount(new BigDecimal(creditStr));
                        }
                    }

                    // Parse balance
                    if (fields[CSV_FIELD_BALANCE] != null && !fields[CSV_FIELD_BALANCE].trim().isEmpty()) {
                        String balanceStr = fields[CSV_FIELD_BALANCE].replaceAll("[^\\d.]", "");
                        if (!balanceStr.isEmpty()) {
                            transaction.setBalance(new BigDecimal(balanceStr));
                        }
                    }

                    // Set service fee
                    transaction.setServiceFee("Y".equalsIgnoreCase(fields[CSV_FIELD_SERVICE_FEE].trim()));

                    // Set account number
                    if (fields[CSV_FIELD_ACCOUNT_NUMBER] != null && !fields[CSV_FIELD_ACCOUNT_NUMBER].trim().isEmpty()) {
                        String accNum = fields[CSV_FIELD_ACCOUNT_NUMBER].trim();
                        transaction.setAccountNumber(accNum);
                    }

                    // Set statement period
                    if (fields[CSV_FIELD_STATEMENT_PERIOD] != null && !fields[CSV_FIELD_STATEMENT_PERIOD].trim().isEmpty()) {
                        transaction.setStatementPeriod(fields[CSV_FIELD_STATEMENT_PERIOD].trim());
                    }

                    // Set source file
                    if (fields[CSV_FIELD_SOURCE_FILE] != null && !fields[CSV_FIELD_SOURCE_FILE].trim().isEmpty()) {
                        transaction.setSourceFile(fields[CSV_FIELD_SOURCE_FILE].trim());
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
                        if (csvFiscalPeriodStr.length() >= MIN_FISCAL_PERIOD_STR_LENGTH) {
                            csvEndYear = csvFiscalPeriodStr.substring(FISCAL_PERIOD_END_YEAR_START); // Get "2025" from "FY2024-2025"
                        }
                        
                        // If selected period is FY2025, check if CSV has FY2024-2025
                        if (selectedPeriodName.length() >= MIN_SELECTED_PERIOD_LENGTH && 
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
            
            pstmt.setLong(SAVE_TRANSACTION_COMPANY_ID, transaction.getCompanyId());
            if (transaction.getBankAccountId() != null) {
                pstmt.setLong(SAVE_TRANSACTION_BANK_ACCOUNT_ID, transaction.getBankAccountId());
            } else {
                pstmt.setNull(SAVE_TRANSACTION_BANK_ACCOUNT_ID, Types.INTEGER);
            }
            // Store date in ISO format
            pstmt.setString(SAVE_TRANSACTION_DATE, transaction.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            pstmt.setString(SAVE_TRANSACTION_DETAILS, transaction.getDetails());
            if (transaction.getDebitAmount() != null) {
                pstmt.setBigDecimal(SAVE_TRANSACTION_DEBIT_AMOUNT, transaction.getDebitAmount());
            } else {
                pstmt.setNull(SAVE_TRANSACTION_DEBIT_AMOUNT, Types.DECIMAL);
            }
            if (transaction.getCreditAmount() != null) {
                pstmt.setBigDecimal(SAVE_TRANSACTION_CREDIT_AMOUNT, transaction.getCreditAmount());
            } else {
                pstmt.setNull(SAVE_TRANSACTION_CREDIT_AMOUNT, Types.DECIMAL);
            }
            if (transaction.getBalance() != null) {
                pstmt.setBigDecimal(SAVE_TRANSACTION_BALANCE, transaction.getBalance());
            } else {
                pstmt.setNull(SAVE_TRANSACTION_BALANCE, Types.DECIMAL);
            }
            pstmt.setBoolean(SAVE_TRANSACTION_SERVICE_FEE, transaction.isServiceFee());
            pstmt.setString(SAVE_TRANSACTION_STATEMENT_PERIOD, transaction.getStatementPeriod());
            pstmt.setString(SAVE_TRANSACTION_SOURCE_FILE, transaction.getSourceFile());
            if (transaction.getFiscalPeriodId() != null) {
                pstmt.setLong(SAVE_TRANSACTION_FISCAL_PERIOD_ID, transaction.getFiscalPeriodId());
            } else {
                pstmt.setNull(SAVE_TRANSACTION_FISCAL_PERIOD_ID, Types.INTEGER);
            }
            pstmt.setString(SAVE_TRANSACTION_ACCOUNT_NUMBER, transaction.getAccountNumber());
            
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
            
            pstmt.setLong(GET_TRANSACTIONS_FISCAL_PERIOD_ID, fiscalPeriodId);
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
