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

    public void setId(Long newId) {
        this.id = newId;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long newJournalEntryId) {
        this.journalEntryId = newJournalEntryId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long newAccountId) {
        this.accountId = newAccountId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String newDescription) {
        this.description = newDescription;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal newDebitAmount) {
        this.debitAmount = newDebitAmount != null ? newDebitAmount : BigDecimal.ZERO;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal newCreditAmount) {
        this.creditAmount = newCreditAmount != null ? newCreditAmount : BigDecimal.ZERO;
    }
}
