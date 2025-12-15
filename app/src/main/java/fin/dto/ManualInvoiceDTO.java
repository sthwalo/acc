package fin.dto;

import fin.entity.ManualInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for exposing manual invoice data over the API
 */
public class ManualInvoiceDTO {
    private Long id;
    private Long companyId;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String description;
    private BigDecimal amount;

    public ManualInvoiceDTO() {}

    public ManualInvoiceDTO(Long id, Long companyId, String invoiceNumber, LocalDate invoiceDate, String description, BigDecimal amount) {
        this.id = id;
        this.companyId = companyId;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.description = description;
        this.amount = amount;
    }

    public static ManualInvoiceDTO fromEntity(ManualInvoice mi) {
        if (mi == null) return null;
        return new ManualInvoiceDTO(mi.getId(), mi.getCompanyId(), mi.getInvoiceNumber(), mi.getInvoiceDate(), mi.getDescription(), mi.getAmount());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
