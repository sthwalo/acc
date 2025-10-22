package fin.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BankTransaction {
    private Long id;
    private Long companyId;
    private Long bankAccountId;
    private LocalDate transactionDate;
    private String details;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal balance;
    private boolean serviceFee;
    private String reference;
    private String statementPeriod;
    private String sourceFile;
    private Long fiscalPeriodId;
    private LocalDateTime createdAt;
    private String accountNumber;
    
    // Account classification fields
    private String accountCode;
    private String accountName;
    
    // Constructors, getters, and setters
    public BankTransaction() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long newId) { this.id = newId; }
    
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long newCompanyId) { this.companyId = newCompanyId; }
    
    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long newBankAccountId) { this.bankAccountId = newBankAccountId; }
    
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate newTransactionDate) { this.transactionDate = newTransactionDate; }
    
    public String getDetails() { return details; }
    public void setDetails(String newDetails) { this.details = newDetails; }
    
    public BigDecimal getDebitAmount() { return debitAmount; }
    public void setDebitAmount(BigDecimal newDebitAmount) { this.debitAmount = newDebitAmount; }
    
    public BigDecimal getCreditAmount() { return creditAmount; }
    public void setCreditAmount(BigDecimal newCreditAmount) { this.creditAmount = newCreditAmount; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal newBalance) { this.balance = newBalance; }
    
    public boolean isServiceFee() { return serviceFee; }
    public void setServiceFee(boolean newServiceFee) { this.serviceFee = newServiceFee; }
    
    public String getStatementPeriod() { return statementPeriod; }
    public void setStatementPeriod(String newStatementPeriod) { this.statementPeriod = newStatementPeriod; }
    
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String newSourceFile) { this.sourceFile = newSourceFile; }
    
    public Long getFiscalPeriodId() { return fiscalPeriodId; }
    public void setFiscalPeriodId(Long newFiscalPeriodId) { this.fiscalPeriodId = newFiscalPeriodId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime newCreatedAt) { this.createdAt = newCreatedAt; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String newAccountNumber) { this.accountNumber = newAccountNumber; }
    
    public String getReference() { return reference; }
    public void setReference(String newReference) { this.reference = newReference; }
    
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String newAccountCode) { this.accountCode = newAccountCode; }
    
    public String getAccountName() { return accountName; }
    public void setAccountName(String newAccountName) { this.accountName = newAccountName; }
    
    @Override
    public String toString() {
        return "BankTransaction{" +
                "id=" + id +
                ", transactionDate=" + transactionDate +
                ", details='" + details + '\'' +
                ", debitAmount=" + debitAmount +
                ", creditAmount=" + creditAmount +
                ", balance=" + balance +
                ", statementPeriod='" + statementPeriod + '\'' +
                ", sourceFile='" + sourceFile + '\'' +
                ", fiscalPeriodId=" + fiscalPeriodId +
                ", accountNumber='" + accountNumber + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
