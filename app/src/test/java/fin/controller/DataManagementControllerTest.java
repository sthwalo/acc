package fin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fin.service.InvoicePdfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataManagementController.class)
public class DataManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoicePdfService invoicePdfService;

    @MockBean
    private fin.service.journal.DataManagementService dataManagementService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void generateInvoicePdf_returnsJsonEnvelope() throws Exception {
        when(invoicePdfService.generateInvoicePdfBytes(anyLong(), any(), any())).thenReturn(new byte[]{1,2,3});

        mockMvc.perform(post("/api/v1/companies/1/data-management/invoices/1/generate-pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PDF generated successfully")));

        verify(invoicePdfService).generateInvoicePdfBytes(anyLong(), any(), any());
    }

    @Test
    public void downloadInvoicePdf_handlesServiceException() throws Exception {
        doThrow(new RuntimeException("pdf error")).when(invoicePdfService).generateInvoicePdfBytes(anyLong(), any(), any());

        mockMvc.perform(get("/api/v1/companies/1/data-management/invoices/999/pdf"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void listInvoices_returnsInvoices() throws Exception {
        fin.entity.ManualInvoice inv = new fin.entity.ManualInvoice();
        inv.setId(3L);
        inv.setCompanyId(1L);
        inv.setInvoiceNumber("INV-3");
        inv.setInvoiceDate(java.time.LocalDate.now());
        inv.setAmount(new java.math.BigDecimal("123.00"));

        when(dataManagementService.getManualInvoicesByCompany(1L)).thenReturn(java.util.Collections.singletonList(inv));

        mockMvc.perform(get("/api/v1/companies/1/data-management/invoices"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invoices retrieved")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("INV-3")));
    }

    @Test
    public void listInvoices_handlesServiceException() throws Exception {
        when(dataManagementService.getManualInvoicesByCompany(anyLong())).thenThrow(new RuntimeException("db error"));

        mockMvc.perform(get("/api/v1/companies/1/data-management/invoices"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("An unexpected error occurred")));
    }

    @Test
    public void getInvoice_returnsInvoiceWhenOwnedByCompany() throws Exception {
        fin.entity.ManualInvoice inv = new fin.entity.ManualInvoice();
        inv.setId(5L);
        inv.setCompanyId(1L);
        inv.setInvoiceNumber("005");
        inv.setInvoiceDate(java.time.LocalDate.now());
        inv.setAmount(new java.math.BigDecimal("20000"));

        when(dataManagementService.getManualInvoiceById(5L)).thenReturn(java.util.Optional.of(inv));

        mockMvc.perform(get("/api/v1/companies/1/data-management/invoices/5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invoice retrieved")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("005")));
    }

    @Test
    public void getInvoice_forbiddenWhenCompanyMismatch() throws Exception {
        fin.entity.ManualInvoice inv = new fin.entity.ManualInvoice();
        inv.setId(6L);
        inv.setCompanyId(99L);
        inv.setInvoiceNumber("X-6");

        when(dataManagementService.getManualInvoiceById(6L)).thenReturn(java.util.Optional.of(inv));

        mockMvc.perform(get("/api/v1/companies/1/data-management/invoices/6"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("does not belong to company")));
    }

    @Test
    public void getInvoice_notFound() throws Exception {
        when(dataManagementService.getManualInvoiceById(999L)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/v1/companies/1/data-management/invoices/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invoice not found")));
    }
}
