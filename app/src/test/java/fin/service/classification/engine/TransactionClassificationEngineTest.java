package fin.service.classification.engine;

import fin.entity.*;
import fin.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class TransactionClassificationEngineTest {

    @Autowired
    private TransactionClassificationEngine engine;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private JournalEntryLineRepository journalEntryLineRepository;

    @Test
    public void updateTransactionClassification_createsJournalEntry() {
        Company company = new Company("TestCo");
        companyRepository.save(company);

        FiscalPeriod fp = new FiscalPeriod();
        fp.setCompanyId(company.getId());
        fp.setStartDate(LocalDate.of(2025,3,1));
        fp.setEndDate(LocalDate.of(2026,2,28));
        fiscalPeriodRepository.save(fp);

        Account debit = new Account("1500", "Test Debit", company.getId());
        Account credit = new Account("4000", "Test Credit", company.getId());
        accountRepository.save(debit);
        accountRepository.save(credit);

        BankTransaction tx = new BankTransaction();
        tx.setCompanyId(company.getId());
        tx.setFiscalPeriodId(fp.getId());
        tx.setTransactionDate(LocalDate.of(2025,3,8));
        tx.setDescription("Test transaction");
        tx.setDebitAmount(BigDecimal.ZERO);
        tx.setCreditAmount(new BigDecimal("2500.00"));
        bankTransactionRepository.save(tx);

        // Call the method under test
        engine.updateTransactionClassification(company.getId(), tx.getId(), debit.getId(), credit.getId(), "tester");

        BankTransaction updated = bankTransactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(updated.getDebitAccountId()).isEqualTo(debit.getId());
        assertThat(updated.getCreditAccountId()).isEqualTo(credit.getId());

        JournalEntry je = journalEntryRepository.findByCompanyIdAndReference(company.getId(), "TXN-" + tx.getId());
        assertThat(je).isNotNull();

        List<JournalEntryLine> lines = journalEntryLineRepository.findBySourceTransactionId(tx.getId());
        assertThat(lines).hasSize(2);
        assertThat(lines).extracting("accountId").containsExactlyInAnyOrder(debit.getId(), credit.getId());
    }
}
