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

        public boolean isValid() { return isValid; }
        public BigDecimal getTotalDebits() { return totalDebits; }
        public BigDecimal getTotalCredits() { return totalCredits; }
        public BigDecimal getFinalBalance() { return finalBalance; }
        public List<String> getDiscrepancies() { return discrepancies; }
        public List<BankTransaction> getMissingTransactions() { return missingTransactions; }
        public List<BankTransaction> getExtraTransactions() { return extraTransactions; }
        public Map<String, BigDecimal> getDifferences() { return differences; }
    }

    public VerificationResult verifyTransactions(String bankStatementPath, Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> bankStatementTransactions = readBankStatement(bankStatementPath);
        List<BankTransaction> csvTransactions = csvImportService.getTransactions(companyId, fiscalPeriodId);

        // Sort transactions by date and amount for comparison
        Comparator<BankTransaction> comparator = Comparator
            .comparing(BankTransaction::getTransactionDate)
            .thenComparing(t -> t.getDebitAmount() != null ? t.getDebitAmount() : BigDecimal.ZERO)
            .thenComparing(t -> t.getCreditAmount() != null ? t.getCreditAmount() : BigDecimal.ZERO);

        bankStatementTransactions.sort(comparator);
        csvTransactions.sort(comparator);

        // Calculate totals from bank statement
        BigDecimal bankTotalDebits = bankStatementTransactions.stream()
            .map(t -> t.getDebitAmount() != null ? t.getDebitAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal bankTotalCredits = bankStatementTransactions.stream()
            .map(t -> t.getCreditAmount() != null ? t.getCreditAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate totals from CSV
        BigDecimal csvTotalDebits = csvTransactions.stream()
            .map(t -> t.getDebitAmount() != null ? t.getDebitAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal csvTotalCredits = csvTransactions.stream()
            .map(t -> t.getCreditAmount() != null ? t.getCreditAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get final balances
        BigDecimal bankFinalBalance = bankStatementTransactions.isEmpty() ? BigDecimal.ZERO :
            bankStatementTransactions.get(bankStatementTransactions.size() - 1).getBalance();
        BigDecimal csvFinalBalance = csvTransactions.isEmpty() ? BigDecimal.ZERO :
            csvTransactions.get(csvTransactions.size() - 1).getBalance();

        // Find missing and extra transactions
        List<BankTransaction> missingTransactions = new ArrayList<>();
        List<BankTransaction> extraTransactions = new ArrayList<>();
        Map<String, BigDecimal> differences = new HashMap<>();
        List<String> discrepancies = new ArrayList<>();

        // Track differences
        if (!bankTotalDebits.equals(csvTotalDebits)) {
            differences.put("debits", bankTotalDebits.subtract(csvTotalDebits));
            discrepancies.add(String.format("Debit totals differ by %s", 
                bankTotalDebits.subtract(csvTotalDebits).abs().toString()));
        }

        if (!bankTotalCredits.equals(csvTotalCredits)) {
            differences.put("credits", bankTotalCredits.subtract(csvTotalCredits));
            discrepancies.add(String.format("Credit totals differ by %s", 
                bankTotalCredits.subtract(csvTotalCredits).abs().toString()));
        }

        if (!bankFinalBalance.equals(csvFinalBalance)) {
            differences.put("balance", bankFinalBalance.subtract(csvFinalBalance));
            discrepancies.add(String.format("Final balances differ by %s", 
                bankFinalBalance.subtract(csvFinalBalance).abs().toString()));
        }

        // Find missing and extra transactions by comparing each transaction
        Set<String> processedTransactions = new HashSet<>();
        
        for (BankTransaction bankTx : bankStatementTransactions) {
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

        // Find extra transactions in CSV
        for (BankTransaction csvTx : csvTransactions) {
            if (!processedTransactions.contains(getTransactionKey(csvTx))) {
                extraTransactions.add(csvTx);
            }
        }

        boolean isValid = discrepancies.isEmpty() && missingTransactions.isEmpty() && extraTransactions.isEmpty();

        return new VerificationResult(
            isValid,
            bankTotalDebits,
            bankTotalCredits,
            bankFinalBalance,
            discrepancies,
            missingTransactions,
            extraTransactions,
            differences
        );
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
            tx.getDebitAmount() != null ? tx.getDebitAmount() : "0",
            tx.getCreditAmount() != null ? tx.getCreditAmount() : "0",
            tx.getBalance() != null ? tx.getBalance() : "0"
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
                if (fields.length >= 4) {
                    BankTransaction transaction = new BankTransaction();
                    transaction.setTransactionDate(LocalDate.parse(fields[0].trim(), DATE_FORMATTER));
                    transaction.setDetails(fields[1].trim());
                    
                    // Parse debit amount
                    String debitStr = fields[2].trim();
                    if (!debitStr.isEmpty()) {
                        transaction.setDebitAmount(new BigDecimal(debitStr.replace(",", "")));
                    }
                    
                    // Parse credit amount
                    String creditStr = fields[3].trim();
                    if (!creditStr.isEmpty()) {
                        transaction.setCreditAmount(new BigDecimal(creditStr.replace(",", "")));
                    }
                    
                    // Parse balance if available
                    if (fields.length > 4 && !fields[4].trim().isEmpty()) {
                        transaction.setBalance(new BigDecimal(fields[4].trim().replace(",", "")));
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
