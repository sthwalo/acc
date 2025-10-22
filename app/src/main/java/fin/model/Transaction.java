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

    public Transaction(Long initialId, String initialDescription, java.math.BigDecimal initialAmount, String initialDebitCredit) {
        this.id = initialId;
        this.description = initialDescription;
        this.amount = initialAmount;
        this.debitCredit = initialDebitCredit;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long newId) { this.id = newId; }

    public String getDescription() { return description; }
    public void setDescription(String newDescription) { this.description = newDescription; }

    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal newAmount) { this.amount = newAmount; }

    public String getDebitCredit() { return debitCredit; }
    public void setDebitCredit(String newDebitCredit) { this.debitCredit = newDebitCredit; }
}