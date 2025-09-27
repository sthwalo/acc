package fin.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a journal entry header in the double-entry accounting system.
 * Each journal entry contains multiple lines that must balance (debits = credits).
 */
public class JournalEntry {
    private Long id;
    private String reference;
    private LocalDate entryDate;
    private String description;
    private Long transactionTypeId;
    private Long fiscalPeriodId;
    private Long companyId;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Associated journal entry lines
    private List<JournalEntryLine> lines;
    
    public JournalEntry() {
        this.lines = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public JournalEntry(String reference, LocalDate entryDate, String description, 
                       Long fiscalPeriodId, Long companyId, String createdBy) {
        this();
        this.reference = reference;
        this.entryDate = entryDate;
        this.description = description;
        this.fiscalPeriodId = fiscalPeriodId;
        this.companyId = companyId;
        this.createdBy = createdBy;
    }
    
    /**
     * Calculates the total debit amount from all lines
     */
    public BigDecimal getTotalDebits() {
        return lines.stream()
                .map(JournalEntryLine::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculates the total credit amount from all lines
     */
    public BigDecimal getTotalCredits() {
        return lines.stream()
                .map(JournalEntryLine::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Checks if the journal entry is balanced (debits = credits)
     */
    public boolean isBalanced() {
        return getTotalDebits().compareTo(getTotalCredits()) == 0;
    }
    
    /**
     * Adds a journal entry line to this entry
     */
    public void addLine(JournalEntryLine line) {
        line.setJournalEntryId(this.id);
        this.lines.add(line);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getReference() {
        return reference;
    }
    
    public void setReference(String reference) {
        this.reference = reference;
    }
    
    public LocalDate getEntryDate() {
        return entryDate;
    }
    
    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Long getTransactionTypeId() {
        return transactionTypeId;
    }
    
    public void setTransactionTypeId(Long transactionTypeId) {
        this.transactionTypeId = transactionTypeId;
    }
    
    public Long getFiscalPeriodId() {
        return fiscalPeriodId;
    }
    
    public void setFiscalPeriodId(Long fiscalPeriodId) {
        this.fiscalPeriodId = fiscalPeriodId;
    }
    
    public Long getCompanyId() {
        return companyId;
    }
    
    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<JournalEntryLine> getLines() {
        return new ArrayList<>(lines);
    }
    
    public void setLines(List<JournalEntryLine> lines) {
        this.lines = new ArrayList<>(lines);
    }
    
    @Override
    public String toString() {
        return "JournalEntry{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", entryDate=" + entryDate +
                ", description='" + description + '\'' +
                ", totalDebits=" + getTotalDebits() +
                ", totalCredits=" + getTotalCredits() +
                ", balanced=" + isBalanced() +
                '}';
    }
}
