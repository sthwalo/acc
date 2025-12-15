package fin.service.journal;

import fin.dto.JournalEntryDTO;
import fin.entity.Account;
import fin.entity.JournalEntry;
import fin.entity.JournalEntryLine;
import fin.repository.AccountRepository;
import fin.repository.JournalEntryLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JournalEntryMapperTest {

    @Mock
    private JournalEntryLineRepository journalEntryLineRepository;

    @Mock
    private AccountRepository accountRepository;

    private JournalEntryMapper mapper;

    @BeforeEach
    public void setUp() {
        mapper = new JournalEntryMapper(journalEntryLineRepository, accountRepository);
    }

    @Test
    public void toDto_usesHeaderDescriptionIfPresent() {
        JournalEntry entry = new JournalEntry("REF-1", LocalDate.now(), "Header Desc", 1L, 1L, "tester");
        entry.setId(10L);

        when(journalEntryLineRepository.findByJournalEntryId(10L)).thenReturn(Collections.emptyList());

        JournalEntryDTO dto = mapper.toDto(entry);

        assertEquals("Header Desc", dto.getDescription());
        assertEquals(0, dto.getLineCount());
        assertEquals(BigDecimal.ZERO, dto.getTotalDebit());
        assertEquals(BigDecimal.ZERO, dto.getTotalCredit());
    }

    @Test
    public void toDto_buildsDescriptionFromLines_whenHeaderMissing() {
        JournalEntry entry = new JournalEntry("REF-2", LocalDate.now(), null, 1L, 1L, "tester");
        entry.setId(20L);

        JournalEntryLine debit = new JournalEntryLine();
        debit.setAccountId(101L);
        debit.setDebitAmount(new BigDecimal("150.00"));

        JournalEntryLine credit = new JournalEntryLine();
        credit.setAccountId(202L);
        credit.setCreditAmount(new BigDecimal("150.00"));

        when(journalEntryLineRepository.findByJournalEntryId(20L)).thenReturn(Arrays.asList(debit, credit));

        Account a1 = new Account();
        a1.setId(101L);
        a1.setAccountName("DebAcct");
        Account a2 = new Account();
        a2.setId(202L);
        a2.setAccountName("CredAcct");

        when(accountRepository.findById(101L)).thenReturn(Optional.of(a1));
        when(accountRepository.findById(202L)).thenReturn(Optional.of(a2));

        JournalEntryDTO dto = mapper.toDto(entry);

        assertEquals("DebAcct - CredAcct", dto.getDescription());
        assertEquals(2, dto.getLineCount());
        assertEquals(new BigDecimal("150.00"), dto.getTotalDebit());
        assertEquals(new BigDecimal("150.00"), dto.getTotalCredit());
    }
}
