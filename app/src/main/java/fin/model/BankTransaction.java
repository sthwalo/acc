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
    
    // Constructors, getters, and setters
    public BankTransaction() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    
    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }
    
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    
    public BigDecimal getDebitAmount() { return debitAmount; }
    public void setDebitAmount(BigDecimal debitAmount) { this.debitAmount = debitAmount; }
    
    public BigDecimal getCreditAmount() { return creditAmount; }
    public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    
    public boolean isServiceFee() { return serviceFee; }
    public void setServiceFee(boolean serviceFee) { this.serviceFee = serviceFee; }
    
    public String getStatementPeriod() { return statementPeriod; }
    public void setStatementPeriod(String statementPeriod) { this.statementPeriod = statementPeriod; }
    
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
    
    public Long getFiscalPeriodId() { return fiscalPeriodId; }
    public void setFiscalPeriodId(Long fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    
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
