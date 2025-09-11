package fin.controller;

import fin.model.BankTransaction;
import fin.service.TransactionVerificationService;
import fin.state.ApplicationState;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Transaction verification controller
 * Extracted from monolithic App.java verification-related methods
 */
public class VerificationController {
    private final TransactionVerificationService verificationService;
    private final ApplicationState applicationState;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    
    public VerificationController(TransactionVerificationService verificationService,
                                ApplicationState applicationState,
                                InputHandler inputHandler,
                                OutputFormatter outputFormatter) {
        this.verificationService = verificationService;
        this.applicationState = applicationState;
        this.inputHandler = inputHandler;
        this.outputFormatter = outputFormatter;
    }
    
    public void handleTransactionVerification() {
        try {
            applicationState.requireContext();
            
            outputFormatter.printHeader("Transaction Verification");
            outputFormatter.printCurrentContext(
                applicationState.getCurrentCompany(), 
                applicationState.getCurrentFiscalPeriod());
            
            String bankStatementPath = inputHandler.getFilePath(
                "Enter bank statement file path for verification", ".pdf");
            
            outputFormatter.printProcessing("Verifying transactions against bank statement...");
            
            TransactionVerificationService.VerificationResult result = 
                verificationService.verifyTransactions(
                    bankStatementPath, 
                    applicationState.getCurrentCompany().getId(), 
                    applicationState.getCurrentFiscalPeriod().getId());
            
            displayVerificationResults(result);
            
            inputHandler.waitForEnter("Verification complete");
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error during verification: " + e.getMessage());
        }
    }
    
    public void handleBatchVerification() {
        try {
            applicationState.requireContext();
            
            outputFormatter.printHeader("Batch Transaction Verification");
            outputFormatter.printInfo("This will verify multiple bank statements against imported data");
            
            if (!inputHandler.getBoolean("Continue with batch verification?")) {
                outputFormatter.printInfo("Batch verification cancelled");
                return;
            }
            
            // This would implement batch verification logic
            outputFormatter.printInfo("Batch verification functionality would be implemented here");
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        }
    }
    
    public void handleCustomVerification() {
        try {
            applicationState.requireContext();
            
            outputFormatter.printHeader("Custom Verification Options");
            outputFormatter.printPlain("1. Verify specific date range");
            outputFormatter.printPlain("2. Verify specific account");
            outputFormatter.printPlain("3. Verify transaction types");
            outputFormatter.printPlain("4. Back to main menu");
            
            int choice = inputHandler.getInteger("Select verification type", 1, 4);
            
            switch (choice) {
                case 1:
                    handleDateRangeVerification();
                    break;
                case 2:
                    handleAccountSpecificVerification();
                    break;
                case 3:
                    handleTransactionTypeVerification();
                    break;
                case 4:
                    // Go back
                    break;
                default:
                    outputFormatter.printError("Invalid choice");
            }
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        }
    }
    
    private void handleDateRangeVerification() {
        outputFormatter.printSubHeader("Date Range Verification");
        outputFormatter.printInfo("Verify transactions within a specific date range");
        
        // This would need additional service methods for date-range verification
        outputFormatter.printInfo("Date range verification functionality would be implemented here");
    }
    
    private void handleAccountSpecificVerification() {
        outputFormatter.printSubHeader("Account-Specific Verification");
        outputFormatter.printInfo("Verify transactions for specific accounts");
        
        // This would need account selection and filtering
        outputFormatter.printInfo("Account-specific verification functionality would be implemented here");
    }
    
    private void handleTransactionTypeVerification() {
        outputFormatter.printSubHeader("Transaction Type Verification");
        outputFormatter.printInfo("Verify specific types of transactions");
        
        // This would need transaction type filtering
        outputFormatter.printInfo("Transaction type verification functionality would be implemented here");
    }
    
    private void displayVerificationResults(TransactionVerificationService.VerificationResult result) {
        outputFormatter.printHeader("Verification Results");
        
        // Overall status
        if (result.isValid()) {
            outputFormatter.printSuccess("VERIFICATION PASSED - No discrepancies found");
        } else {
            outputFormatter.printError("VERIFICATION FAILED - Discrepancies found");
        }
        
        outputFormatter.printSeparator();
        
        // Total amounts summary
        outputFormatter.printSubHeader("Financial Summary");
        System.out.printf("Total Debits:  %,.2f%n", result.getTotalDebits());
        System.out.printf("Total Credits: %,.2f%n", result.getTotalCredits());
        System.out.printf("Final Balance: %,.2f%n", result.getFinalBalance());
        
        if (!result.isValid()) {
            displayDiscrepancies(result);
        }
        
        outputFormatter.printSeparator();
    }
    
    private void displayDiscrepancies(TransactionVerificationService.VerificationResult result) {
        // Show general discrepancies
        if (!result.getDiscrepancies().isEmpty()) {
            outputFormatter.printSubHeader("Discrepancies Found");
            for (String discrepancy : result.getDiscrepancies()) {
                outputFormatter.printError(discrepancy);
            }
        }
        
        // Show missing transactions
        if (!result.getMissingTransactions().isEmpty()) {
            outputFormatter.printSubHeader("Missing Transactions");
            outputFormatter.printInfo("Transactions in bank statement but missing in CSV:");
            
            for (BankTransaction tx : result.getMissingTransactions()) {
                System.out.printf("Date: %s | Details: %s | Debit: %s | Credit: %s | Balance: %s%n",
                    tx.getTransactionDate(),
                    truncateString(tx.getDetails(), 40),
                    formatAmount(tx.getDebitAmount()),
                    formatAmount(tx.getCreditAmount()),
                    formatAmount(tx.getBalance()));
            }
        }
        
        // Show extra transactions
        if (!result.getExtraTransactions().isEmpty()) {
            outputFormatter.printSubHeader("Extra Transactions");
            outputFormatter.printInfo("Transactions in CSV but not in bank statement:");
            
            for (BankTransaction tx : result.getExtraTransactions()) {
                System.out.printf("Date: %s | Details: %s | Debit: %s | Credit: %s | Balance: %s%n",
                    tx.getTransactionDate(),
                    truncateString(tx.getDetails(), 40),
                    formatAmount(tx.getDebitAmount()),
                    formatAmount(tx.getCreditAmount()),
                    formatAmount(tx.getBalance()));
            }
        }
        
        // Show differences
        if (!result.getDifferences().isEmpty()) {
            outputFormatter.printSubHeader("Amount Differences");
            for (Map.Entry<String, BigDecimal> diff : result.getDifferences().entrySet()) {
                String label = diff.getKey().substring(0, 1).toUpperCase() + diff.getKey().substring(1);
                System.out.printf("%s Difference: %,.2f%n", label, diff.getValue());
            }
        }
    }
    
    private String formatAmount(BigDecimal amount) {
        return amount != null ? String.format("%,.2f", amount) : "-";
    }
    
    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }
}
