package fin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO holding the minimal data needed to generate an invoice PDF.
 */
public class InvoicePdfData {
    private final String invoiceNumber;
    private final LocalDate invoiceDate;
    private final String description;
    private final BigDecimal amount;
    private final String debitCode;
    private final String debitName;
    private final String creditCode;
    private final String creditName;

    public InvoicePdfData(String invoiceNumber, LocalDate invoiceDate, String description, BigDecimal amount,
                          String debitCode, String debitName, String creditCode, String creditName) {
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.description = description;
        this.amount = amount;
        this.debitCode = debitCode;
        this.debitName = debitName;
        this.creditCode = creditCode;
        this.creditName = creditName;
    }

    public String getInvoiceNumber() { return invoiceNumber; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getDebitCode() { return debitCode; }
    public String getDebitName() { return debitName; }
    public String getCreditCode() { return creditCode; }
    public String getCreditName() { return creditName; }
}
