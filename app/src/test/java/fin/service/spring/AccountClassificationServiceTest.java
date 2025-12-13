/*
 * FIN Financial Management System
 *
 * Unit test for AccountClassificationService.generateJournalEntriesForClassifiedTransactions
 */
package fin.service.spring;

import fin.entity.*;
import fin.repository.*;
import fin.service.classification.AccountClassificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AccountClassificationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BankTransactionRepository bankTransactionRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private JournalEntryLineRepository journalEntryLineRepository;

    private AccountClassificationService accountClassificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountClassificationService = new AccountClassificationService(
                bankTransactionRepository,
                accountRepository,
                journalEntryRepository,
                journalEntryLineRepository
        );
    }

    @Test
    @DisplayName("Should create journal entry with reference and description for classified transaction")
    public void testGenerateJournalEntriesForClassifiedTransactions_createsJournalEntryHeaderAndLines() {
        // Arrange
        Long companyId = 1L;
        String createdBy = "testuser";

        BankTransaction tx = new BankTransaction();
        tx.setId(100L);
        tx.setCompanyId(companyId);
        tx.setFiscalPeriodId(1L);
        tx.setReference("BANK-REF-100");
        tx.setCategory(null); // simulate case where category is not set
        tx.setAccountCode("5100");
        tx.setTransactionDate(LocalDate.of(2025, 1, 15));
        tx.setCreditAmount(BigDecimal.valueOf(250.00)); // deposit

        // Mock classified transactions without journal entries
        when(bankTransactionRepository.findClassifiedTransactionsWithoutJournalEntries(companyId))
                .thenReturn(List.of(tx));

        // Mock account lookup for classified account
        Account classifiedAccount = new Account();
        classifiedAccount.setId(5100L);
        classifiedAccount.setAccountCode("5100");
        classifiedAccount.setAccountName("Office Supplies");
        when(accountRepository.findByCompanyIdAndAccountCode(companyId, tx.getAccountCode()))
                .thenReturn(Optional.of(classifiedAccount));

        // transaction should contain accountName (classification would set this in flow)
        tx.setAccountName(classifiedAccount.getAccountName());

        // Mock bank account lookup (code "1000")
        Account bankAccount = new Account();
        bankAccount.setId(1000L);
        bankAccount.setAccountCode("1000");
        bankAccount.setAccountName("Bank Account");
        when(accountRepository.findByCompanyIdAndAccountCode(companyId, "1000"))
                .thenReturn(Optional.of(bankAccount));

        // Capture JournalEntry saved
        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        when(journalEntryRepository.save(entryCaptor.capture())).thenAnswer(invocation -> {
            JournalEntry saved = invocation.getArgument(0);
            saved.setId(200L); // assign ID like DB
            return saved;
        });

        // Capture JournalEntryLine saves
        ArgumentCaptor<JournalEntryLine> lineCaptor = ArgumentCaptor.forClass(JournalEntryLine.class);
        when(journalEntryLineRepository.save(lineCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int count = accountClassificationService.generateJournalEntriesForClassifiedTransactions(companyId, createdBy);

        // Assert
        assertEquals(1, count, "Should generate one journal entry for the single classified transaction");

        // Verify journal entry header contains reference and description (captured)
        // The save call is asserted via the captured value below
        JournalEntry savedEntry = entryCaptor.getValue();
        assertNotNull(savedEntry);
        assertEquals(tx.getReference(), savedEntry.getReference(), "JournalEntry.reference must match transaction reference");
        // Since category is null, description should fallback to account name of classified account
        assertEquals(tx.getReference() + " - " + classifiedAccount.getAccountName(), savedEntry.getDescription(), "JournalEntry.description must be reference - accountName when category is null");
        assertEquals(tx.getCompanyId(), savedEntry.getCompanyId());
        assertEquals(tx.getFiscalPeriodId(), savedEntry.getFiscalPeriodId());
        assertEquals(createdBy, savedEntry.getCreatedBy());
        assertNotNull(savedEntry.getCreatedAt());
        assertNotNull(savedEntry.getUpdatedAt());

        // Verify exactly two lines saved for this transaction (Debit bank, Credit classified account)
        List<JournalEntryLine> savedLines = lineCaptor.getAllValues();
        assertEquals(2, savedLines.size());

        JournalEntryLine l1 = savedLines.get(0);
        JournalEntryLine l2 = savedLines.get(1);

        // Ensure lines reference the journal entry ID and have the correct amounts
        assertEquals(200L, l1.getJournalEntryId());
        assertEquals(200L, l2.getJournalEntryId());
        assertEquals(tx.getCreditAmount(), l1.getDebitAmount());
        assertEquals(BigDecimal.ZERO, l1.getCreditAmount());
        assertEquals(BigDecimal.ZERO, l2.getDebitAmount());
        assertEquals(tx.getCreditAmount(), l2.getCreditAmount());
    }
}
