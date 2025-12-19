package fin.service.upload;

import fin.entity.Company;
import fin.entity.FiscalPeriod;
import fin.service.upload.BankStatementProcessingService;
import fin.service.upload.DocumentTextExtractor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BankStatementProcessingStatementPeriodValidationTest {

    @Test
    void processStatement_shouldFailWhenStatementPeriodDoesNotOverlapFiscalPeriod() throws Exception {
        // Mock dependencies
        DocumentTextExtractor extractor = mock(DocumentTextExtractor.class);
        when(extractor.parseDocument(any(File.class))).thenReturn(List.of("dummy"));
        when(extractor.getStatementPeriod()).thenReturn("16 February 2020 to 18 March 2020");
        when(extractor.parseStatementPeriod("16 February 2020 to 18 March 2020")).thenReturn(new DocumentTextExtractor.StatementPeriod(LocalDate.of(2020,2,16), LocalDate.of(2020,3,18)));

        var transactionRepo = mock(fin.repository.BankTransactionRepository.class);
        var fiscalRepo = mock(fin.repository.FiscalPeriodRepository.class);
        var validator = mock(fin.validation.BankTransactionValidator.class);
        var companyService = mock(fin.service.CompanyService.class);
        var duplicateChecker = mock(fin.service.transaction.TransactionDuplicateChecker.class);
        var fiscalValidator = mock(fin.service.FiscalPeriodBoundaryValidator.class);

        // Create service with mocks; other parsers not needed for this test
        BankStatementProcessingService svc = new BankStatementProcessingService(
            extractor,
            transactionRepo,
            fiscalRepo,
            validator,
            companyService,
            duplicateChecker,
            fiscalValidator,
            mock(fin.service.parser.StandardBankTabularParser.class),
            mock(fin.service.parser.AbsaBankParser.class),
            mock(fin.service.parser.FnbBankParser.class),
            mock(fin.service.parser.CreditTransactionParser.class),
            mock(fin.service.parser.ServiceFeeParser.class)
        );

        // fiscal period that does not overlap (2024)
        FiscalPeriod fiscal = new FiscalPeriod(1L, "FY2024", LocalDate.of(2024,1,1), LocalDate.of(2024,12,31));
        when(fiscalRepo.findById(1L)).thenReturn(java.util.Optional.of(fiscal));

        // Temp file
        java.io.File temp = java.io.File.createTempFile("test_stmt_", ".pdf");
        temp.deleteOnExit();

        // Create a mock MultipartFile from the temp file
        byte[] data = java.nio.file.Files.readAllBytes(temp.toPath());
        org.springframework.mock.web.MockMultipartFile multipart = new org.springframework.mock.web.MockMultipartFile("file", temp.getName(), "application/pdf", data);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            svc.processStatement(multipart, 1L, 1L);
        });

        assertTrue(ex.getMessage().contains("does not overlap specified fiscal period"));
    }
}