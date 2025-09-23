package fin.model;

/**
 * Simple transaction model for the modularized services
 */
public class Transaction {
    private Long id;
    private String description;
    private java.math.BigDecimal amount;
    private String debitCredit; // "D" for debit, "C" for credit

    public Transaction() {}

    public Transaction(Long id, String description, java.math.BigDecimal amount, String debitCredit) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.debitCredit = debitCredit;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }

    public String getDebitCredit() { return debitCredit; }
    public void setDebitCredit(String debitCredit) { this.debitCredit = debitCredit; }
}