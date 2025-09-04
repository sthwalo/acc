package fin.model;

import java.math.BigDecimal;

/**
 * Represents a single line in a journal entry.
 * Each line contains either a debit or credit amount for a specific account.
 */
public class JournalEntryLine {
    private Long id;
    private Long journalEntryId;
    private Long accountId;
    private String description;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;

    public JournalEntryLine() {
        this.debitAmount = BigDecimal.ZERO;
        this.creditAmount = BigDecimal.ZERO;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount != null ? debitAmount : BigDecimal.ZERO;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount != null ? creditAmount : BigDecimal.ZERO;
    }
}
