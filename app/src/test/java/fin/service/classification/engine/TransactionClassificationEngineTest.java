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

    @Autowired
    private fin.service.classification.rules.TransactionMappingRuleService transactionMappingRuleService;

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

    @Test
    public void autoClassify_persistsAccountCode_whenRuleHasAccount() {
        Company company = new Company("AutoClassCo");
        companyRepository.save(company);

        FiscalPeriod fp = new FiscalPeriod();
        fp.setCompanyId(company.getId());
        fp.setStartDate(LocalDate.of(2025,3,1));
        fp.setEndDate(LocalDate.of(2026,2,28));
        fiscalPeriodRepository.save(fp);

        Account bank = new Account("1000", "Bank", company.getId());
        Account utilities = new Account("4230", "Utilities", company.getId());
        accountRepository.save(bank);
        accountRepository.save(utilities);

        // Create a transaction that matches the rule
        BankTransaction tx = new BankTransaction();
        tx.setCompanyId(company.getId());
        tx.setFiscalPeriodId(fp.getId());
        tx.setTransactionDate(LocalDate.of(2025,3,8));
        tx.setDescription("Magtape Debit MTN Sp A5375351");
        tx.setDebitAmount(new BigDecimal("1114.49"));
        tx.setCreditAmount(BigDecimal.ZERO);
        bankTransactionRepository.save(tx);

        // Create rule that matches 'MTN' and assigns utilities account
        transactionMappingRuleService.createTransactionMappingRule(
            company.getId(),
            "Rule for: Magtape Debit MTN Sp A5375351 ...",
            "CONTAINS",
            "MTN",
            "4230",
            "Utilities",
            100,
            "tester"
        );

        // Run auto-classify for the period and verify transaction is updated
        int classified = engine.autoClassifyTransactions(company.getId(), fp.getId());
        assertThat(classified).isEqualTo(1);

        BankTransaction updated = bankTransactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(updated.getAccountCode()).isEqualTo("4230");
    }

    @Test
    public void autoClassify_handlesDisplayAccountCode_withName() {
        Company company = new Company("AutoClassCo2");
        companyRepository.save(company);

        FiscalPeriod fp = new FiscalPeriod();
        fp.setCompanyId(company.getId());
        fp.setStartDate(LocalDate.of(2025,3,1));
        fp.setEndDate(LocalDate.of(2026,2,28));
        fiscalPeriodRepository.save(fp);

        Account bank = new Account("1000", "Bank", company.getId());
        Account utilities = new Account("4230", "Utilities", company.getId());
        accountRepository.save(bank);
        accountRepository.save(utilities);

        BankTransaction tx = new BankTransaction();
        tx.setCompanyId(company.getId());
        tx.setFiscalPeriodId(fp.getId());
        tx.setTransactionDate(LocalDate.of(2025,3,8));
        tx.setDescription("Magtape Debit MTN Sp A5375351");
        tx.setDebitAmount(new BigDecimal("1114.49"));
        tx.setCreditAmount(BigDecimal.ZERO);
        bankTransactionRepository.save(tx);

        // Create rule but pass display account code '4230 - Utilities'
        transactionMappingRuleService.createTransactionMappingRule(
            company.getId(),
            "Rule for: Magtape Debit MTN ...",
            "CONTAINS",
            "MTN",
            "4230 - Utilities",
            "Utilities",
            100,
            "tester"
        );

        int classified = engine.autoClassifyTransactions(company.getId(), fp.getId());
        assertThat(classified).isEqualTo(1);

        BankTransaction updated = bankTransactionRepository.findById(tx.getId()).orElseThrow();
        assertThat(updated.getAccountCode()).isEqualTo("4230");
    }

    @Test
    public void autoClassify_and_sync_creates_correct_debit_credit_for_debit_transactions() throws Exception {
        Company company = new Company("ExpenseTestCo");
        companyRepository.save(company);

        FiscalPeriod fp = new FiscalPeriod();
        fp.setCompanyId(company.getId());
        fp.setStartDate(LocalDate.of(2025,3,1));
        fp.setEndDate(LocalDate.of(2026,2,28));
        fiscalPeriodRepository.save(fp);

        Account bank = new Account("1000L", "Bank", company.getId());
        Account expense = new Account("4280", "Other Expenses", company.getId());
        accountRepository.save(bank);
        accountRepository.save(expense);

        // Outgoing payment - represented by debit amount in fixtures
        BankTransaction tx = new BankTransaction();
        tx.setCompanyId(company.getId());
        tx.setFiscalPeriodId(fp.getId());
        tx.setTransactionDate(LocalDate.of(2025,3,3));
        tx.setDescription("Internet Pmt To Fuel Allowance");
        tx.setDebitAmount(new BigDecimal("980.00"));
        tx.setCreditAmount(BigDecimal.ZERO);
        bankTransactionRepository.save(tx);

        // Create rule that matches 'fuel' and assigns expense account
        transactionMappingRuleService.createTransactionMappingRule(
            company.getId(),
            "Rule: Fuel",
            "CONTAINS",
            "fuel",
            "4280",
            "Other Expenses",
            100,
            "tester"
        );

        int classified = engine.autoClassifyTransactions(company.getId(), fp.getId());
        assertThat(classified).isEqualTo(1);

        int synced = engine.syncJournalEntries(company.getId());
        assertThat(synced).isEqualTo(1);

        JournalEntry je = journalEntryRepository.findByCompanyIdAndReference(company.getId(), "TXN-" + tx.getId());
        assertThat(je).isNotNull();

        List<JournalEntryLine> lines = journalEntryLineRepository.findBySourceTransactionId(tx.getId());
        assertThat(lines).hasSize(2);

        // Expense (debit) and Bank (credit)
        JournalEntryLine expenseLine = lines.stream()
            .filter(l -> l.getAccountId().equals(expense.getId()))
            .findFirst().orElseThrow();
        assertThat(expenseLine.getDebitAmount()).isEqualTo(new BigDecimal("980.00"));
        assertThat(expenseLine.getCreditAmount()).isEqualTo(BigDecimal.ZERO);

        JournalEntryLine bankLine = lines.stream()
            .filter(l -> l.getAccountId().equals(bank.getId()))
            .findFirst().orElseThrow();
        assertThat(bankLine.getDebitAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(bankLine.getCreditAmount()).isEqualTo(new BigDecimal("980.00"));
    }

    @Test
    public void autoClassify_and_sync_creates_correct_debit_credit_for_credit_transactions() throws Exception {
        Company company = new Company("IncomeTestCo");
        companyRepository.save(company);

        FiscalPeriod fp = new FiscalPeriod();
        fp.setCompanyId(company.getId());
        fp.setStartDate(LocalDate.of(2025,3,1));
        fp.setEndDate(LocalDate.of(2026,2,28));
        fiscalPeriodRepository.save(fp);

        Account bank = new Account("1000", "Bank", company.getId());
        Account income = new Account("4000", "Sales", company.getId());
        accountRepository.save(bank);
        accountRepository.save(income);

        // Incoming payment - represented by credit amount in fixtures
        BankTransaction tx = new BankTransaction();
        tx.setCompanyId(company.getId());
        tx.setFiscalPeriodId(fp.getId());
        tx.setTransactionDate(LocalDate.of(2025,3,4));
        tx.setDescription("Customer payment - sale");
        tx.setDebitAmount(BigDecimal.ZERO);
        tx.setCreditAmount(new BigDecimal("1500.00"));
        bankTransactionRepository.save(tx);

        // Create rule that matches 'sale' and assigns income account
        transactionMappingRuleService.createTransactionMappingRule(
            company.getId(),
            "Rule: Sale",
            "CONTAINS",
            "sale",
            "4000",
            "Sales",
            100,
            "tester"
        );

        int classified = engine.autoClassifyTransactions(company.getId(), fp.getId());
        assertThat(classified).isEqualTo(1);

        int synced = engine.syncJournalEntries(company.getId());
        assertThat(synced).isEqualTo(1);

        JournalEntry je = journalEntryRepository.findByCompanyIdAndReference(company.getId(), "TXN-" + tx.getId());
        assertThat(je).isNotNull();

        List<JournalEntryLine> lines = journalEntryLineRepository.findBySourceTransactionId(tx.getId());
        assertThat(lines).hasSize(2);

        // Bank (debit) and Income (credit)
        JournalEntryLine bankLine = lines.stream()
            .filter(l -> l.getAccountId().equals(bank.getId()))
            .findFirst().orElseThrow();
        assertThat(bankLine.getDebitAmount()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(bankLine.getCreditAmount()).isEqualTo(BigDecimal.ZERO);

        JournalEntryLine incomeLine = lines.stream()
            .filter(l -> l.getAccountId().equals(income.getId()))
            .findFirst().orElseThrow();
        assertThat(incomeLine.getDebitAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(incomeLine.getCreditAmount()).isEqualTo(new BigDecimal("1500.00"));
    }

    @Test
    public void syncJournalEntries_throws_when_no_bank_account() throws Exception {
        Company company = new Company("NoBankCo");
        companyRepository.save(company);

        FiscalPeriod fp = new FiscalPeriod();
        fp.setCompanyId(company.getId());
        fp.setStartDate(LocalDate.of(2025,3,1));
        fp.setEndDate(LocalDate.of(2026,2,28));
        fiscalPeriodRepository.save(fp);

        Account expense = new Account("4280", "Other Expenses", company.getId());
        accountRepository.save(expense);

        BankTransaction tx = new BankTransaction();
        tx.setCompanyId(company.getId());
        tx.setFiscalPeriodId(fp.getId());
        tx.setTransactionDate(LocalDate.of(2025,3,5));
        tx.setDescription("Internet Pmt To Fuel Allowance");
        tx.setDebitAmount(new BigDecimal("980.00"));
        tx.setCreditAmount(BigDecimal.ZERO);
        tx.setAccountCode("4280"); // classified but no bank account exists
        bankTransactionRepository.save(tx);

        // Expect sync to throw SQLException because no bank/cash account could be resolved
        org.junit.jupiter.api.Assertions.assertThrows(
            java.sql.SQLException.class,
            () -> engine.syncJournalEntries(company.getId())
        );
    }
}
