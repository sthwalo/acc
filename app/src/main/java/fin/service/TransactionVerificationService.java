package fin.service;

import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.FiscalPeriod;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionVerificationService {
    private final String dbUrl;
    private final CompanyService companyService;
    private final CsvImportService csvImportService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    // Constants for CSV field indices
    @SuppressWarnings("MagicNumber")
    private static final int MINIMUM_FIELDS_REQUIRED = 4;
    @SuppressWarnings("MagicNumber")
    private static final int DEBIT_AMOUNT_FIELD_INDEX = 2;
    @SuppressWarnings("MagicNumber")
    private static final int CREDIT_AMOUNT_FIELD_INDEX = 3;
    @SuppressWarnings("MagicNumber")
    private static final int BALANCE_FIELD_INDEX = 4;

    public TransactionVerificationService(String dbUrl, CompanyService companyService, CsvImportService csvImportService) {
        this.dbUrl = dbUrl;
        this.companyService = companyService;
        this.csvImportService = csvImportService;
    }

    public static class VerificationResult {
        private final boolean isValid;
        private final BigDecimal totalDebits;
        private final BigDecimal totalCredits;
        private final BigDecimal finalBalance;
        private final List<String> discrepancies;
        private final List<BankTransaction> missingTransactions;
        private final List<BankTransaction> extraTransactions;
        private final Map<String, BigDecimal> differences;

        public VerificationResult(boolean isValid, BigDecimal totalDebits, BigDecimal totalCredits, 
                                BigDecimal finalBalance, List<String> discrepancies,
                                List<BankTransaction> missingTransactions, List<BankTransaction> extraTransactions,
                                Map<String, BigDecimal> differences) {
            this.isValid = isValid;
            this.totalDebits = totalDebits;
            this.totalCredits = totalCredits;
            this.finalBalance = finalBalance;
            this.discrepancies = discrepancies;
            this.missingTransactions = missingTransactions;
            this.extraTransactions = extraTransactions;
            this.differences = differences;
        }

        /**
         * Returns whether the transaction verification is valid.
         * @return true if verification passed, false otherwise
         */
        public boolean isValid() 
        { 
            return isValid; 
        }
        
        /**
         * Returns the total debits from the verification.
         * @return total debits as BigDecimal
         */
        public BigDecimal getTotalDebits() 
        { 
            return totalDebits; 
        }
        
        /**
         * Returns the total credits from the verification.
         * @return total credits as BigDecimal
         */
        public BigDecimal getTotalCredits() 
        { 
            return totalCredits; 
        }
        
        /**
         * Returns the final balance from the verification.
         * @return final balance as BigDecimal
         */
        public BigDecimal getFinalBalance() 
        { 
            return finalBalance; 
        }
        
        /**
         * Returns the list of discrepancies found during verification.
         * @return list of discrepancy messages
         */
        public List<String> getDiscrepancies() 
        { 
            return discrepancies; 
        }
        
        /**
         * Returns transactions that are missing from the CSV but present in bank statement.
         * @return list of missing transactions
         */
        public List<BankTransaction> getMissingTransactions() 
        { 
            return missingTransactions; 
        }
        
        /**
         * Returns transactions that are extra in the CSV but not in bank statement.
         * @return list of extra transactions
         */
        public List<BankTransaction> getExtraTransactions() 
        { 
            return extraTransactions; 
        }
        
        /**
         * Returns the differences found between bank statement and CSV totals.
         * @return map of difference types to amounts
         */
        public Map<String, BigDecimal> getDifferences() 
        { 
            return differences; 
        }
    }

    private static class TransactionTotals {
        final BigDecimal totalDebits;
        final BigDecimal totalCredits;

        TransactionTotals(BigDecimal totalDebits, BigDecimal totalCredits) {
            this.totalDebits = totalDebits;
            this.totalCredits = totalCredits;
        }
    }

    private static class VerificationData {
        final List<String> discrepancies;
        final Map<String, BigDecimal> differences;

        VerificationData(List<String> discrepancies, Map<String, BigDecimal> differences) {
            this.discrepancies = discrepancies;
            this.differences = differences;
        }
    }

    private static class TransactionComparisonResult {
        final List<BankTransaction> missingTransactions;
        final List<BankTransaction> extraTransactions;

        TransactionComparisonResult(List<BankTransaction> missingTransactions, 
                                  List<BankTransaction> extraTransactions) {
            this.missingTransactions = missingTransactions;
            this.extraTransactions = extraTransactions;
        }
    }

    public VerificationResult verifyTransactions(String bankStatementPath, Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> bankStatementTransactions = readBankStatement(bankStatementPath);
        List<BankTransaction> csvTransactions = csvImportService.getTransactions(companyId, fiscalPeriodId);

        // Sort transactions for comparison
        sortTransactionsForComparison(bankStatementTransactions, csvTransactions);

        // Calculate totals
        TransactionTotals bankTotals = calculateTransactionTotals(bankStatementTransactions);
        TransactionTotals csvTotals = calculateTransactionTotals(csvTransactions);

        // Get final balances
        BigDecimal bankFinalBalance = getFinalBalance(bankStatementTransactions);
        BigDecimal csvFinalBalance = getFinalBalance(csvTransactions);

        // Find discrepancies and differences
        VerificationData verificationData = findTransactionDiscrepancies(
            bankTotals, csvTotals, bankFinalBalance, csvFinalBalance);

        // Find missing and extra transactions
        TransactionComparisonResult comparisonResult = compareTransactionLists(
            bankStatementTransactions, csvTransactions);

        boolean isValid = verificationData.discrepancies.isEmpty() && 
                         comparisonResult.missingTransactions.isEmpty() && 
                         comparisonResult.extraTransactions.isEmpty();

        return new VerificationResult(
            isValid,
            bankTotals.totalDebits,
            bankTotals.totalCredits,
            bankFinalBalance,
            verificationData.discrepancies,
            comparisonResult.missingTransactions,
            comparisonResult.extraTransactions,
            verificationData.differences
        );
    }

    private void sortTransactionsForComparison(List<BankTransaction> bankTransactions, 
                                            List<BankTransaction> csvTransactions) {
        Comparator<BankTransaction> comparator = Comparator
            .comparing(BankTransaction::getTransactionDate)
            .thenComparing(t -> t.getDebitAmount() != null ? t.getDebitAmount() : BigDecimal.ZERO)
            .thenComparing(t -> t.getCreditAmount() != null ? t.getCreditAmount() : BigDecimal.ZERO);

        bankTransactions.sort(comparator);
        csvTransactions.sort(comparator);
    }

    private TransactionTotals calculateTransactionTotals(List<BankTransaction> transactions) {
        BigDecimal totalDebits = transactions.stream()
            .map(t -> t.getDebitAmount() != null ? t.getDebitAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = transactions.stream()
            .map(t -> t.getCreditAmount() != null ? t.getCreditAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TransactionTotals(totalDebits, totalCredits);
    }

    private BigDecimal getFinalBalance(List<BankTransaction> transactions) {
        return transactions.isEmpty() 
            ? BigDecimal.ZERO 
            : transactions.get(transactions.size() - 1).getBalance();
    }

    private VerificationData findTransactionDiscrepancies(TransactionTotals bankTotals, 
                                                       TransactionTotals csvTotals,
                                                       BigDecimal bankFinalBalance, 
                                                       BigDecimal csvFinalBalance) {
        List<String> discrepancies = new ArrayList<>();
        Map<String, BigDecimal> differences = new HashMap<>();

        if (!bankTotals.totalDebits.equals(csvTotals.totalDebits)) {
            differences.put("debits", bankTotals.totalDebits.subtract(csvTotals.totalDebits));
            discrepancies.add(String.format("Debit totals differ by %s", 
                bankTotals.totalDebits.subtract(csvTotals.totalDebits).abs().toString()));
        }

        if (!bankTotals.totalCredits.equals(csvTotals.totalCredits)) {
            differences.put("credits", bankTotals.totalCredits.subtract(csvTotals.totalCredits));
            discrepancies.add(String.format("Credit totals differ by %s", 
                bankTotals.totalCredits.subtract(csvTotals.totalCredits).abs().toString()));
        }

        if (!bankFinalBalance.equals(csvFinalBalance)) {
            differences.put("balance", bankFinalBalance.subtract(csvFinalBalance));
            discrepancies.add(String.format("Final balances differ by %s", 
                bankFinalBalance.subtract(csvFinalBalance).abs().toString()));
        }

        return new VerificationData(discrepancies, differences);
    }

    private TransactionComparisonResult compareTransactionLists(List<BankTransaction> bankTransactions,
                                                             List<BankTransaction> csvTransactions) {
        List<BankTransaction> missingTransactions = new ArrayList<>();
        List<BankTransaction> extraTransactions = new ArrayList<>();
        Set<String> processedTransactions = new HashSet<>();
        
        for (BankTransaction bankTx : bankTransactions) {
            boolean found = false;
            for (BankTransaction csvTx : csvTransactions) {
                if (transactionsMatch(bankTx, csvTx)) {
                    found = true;
                    processedTransactions.add(getTransactionKey(csvTx));
                    break;
                }
            }
            if (!found) {
                missingTransactions.add(bankTx);
            }
        }

        for (BankTransaction csvTx : csvTransactions) {
            if (!processedTransactions.contains(getTransactionKey(csvTx))) {
                extraTransactions.add(csvTx);
            }
        }

        return new TransactionComparisonResult(missingTransactions, extraTransactions);
    }

    private boolean transactionsMatch(BankTransaction tx1, BankTransaction tx2) {
        return tx1.getTransactionDate().equals(tx2.getTransactionDate()) &&
               Objects.equals(tx1.getDebitAmount(), tx2.getDebitAmount()) &&
               Objects.equals(tx1.getCreditAmount(), tx2.getCreditAmount()) &&
               Objects.equals(tx1.getBalance(), tx2.getBalance());
    }

    private String getTransactionKey(BankTransaction tx) {
        return String.format("%s_%s_%s_%s",
            tx.getTransactionDate(),
            tx.getDebitAmount() != null 
                ? tx.getDebitAmount() 
                : "0",
            tx.getCreditAmount() != null 
                ? tx.getCreditAmount() 
                : "0",
            tx.getBalance() != null 
                ? tx.getBalance() 
                : "0"
        );
    }

    private List<BankTransaction> readBankStatement(String filePath) {
        List<BankTransaction> transactions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;  // Skip header
                }
                
                String[] fields = line.split(",");
                if (fields.length >= MINIMUM_FIELDS_REQUIRED) {
                    BankTransaction transaction = new BankTransaction();
                    transaction.setTransactionDate(LocalDate.parse(fields[0].trim(), DATE_FORMATTER));
                    transaction.setDetails(fields[1].trim());
                    
                    // Parse debit amount
                    String debitStr = fields[DEBIT_AMOUNT_FIELD_INDEX].trim();
                    if (!debitStr.isEmpty()) {
                        transaction.setDebitAmount(new BigDecimal(debitStr.replace(",", "")));
                    }
                    
                    // Parse credit amount
                    String creditStr = fields[CREDIT_AMOUNT_FIELD_INDEX].trim();
                    if (!creditStr.isEmpty()) {
                        transaction.setCreditAmount(new BigDecimal(creditStr.replace(",", "")));
                    }
                    
                    // Parse balance if available
                    if (fields.length > BALANCE_FIELD_INDEX && !fields[BALANCE_FIELD_INDEX].trim().isEmpty()) {
                        transaction.setBalance(new BigDecimal(fields[BALANCE_FIELD_INDEX].trim().replace(",", "")));
                    }
                    
                    transactions.add(transaction);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading bank statement: " + e.getMessage(), e);
        }
        
        return transactions;
    }
}
