package fin.service;

import fin.entity.BankTransaction;
import fin.entity.Company;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test for BankStatementProcessingService functionality
 */
class BankStatementProcessingServiceTest {

    @Test
    void basicInstantiationTest() {
        // This test just verifies that our classes can be imported and basic objects created
        Company company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        assertNotNull(company, "Company should be created");
        assertEquals(1L, company.getId(), "Company ID should be set");
        assertEquals("Test Company", company.getName(), "Company name should be set");
    }

    @Test
    void bankTransactionCreationTest() {
        // Test that we can create BankTransaction objects
        BankTransaction transaction = new BankTransaction();
        transaction.setCompanyId(1L);
        transaction.setDetails("Test transaction");
        transaction.setDebitAmount(new java.math.BigDecimal("100.00"));

        assertNotNull(transaction, "BankTransaction should be created");
        assertEquals(1L, transaction.getCompanyId(), "Company ID should be set");
        assertEquals("Test transaction", transaction.getDetails(), "Details should be set");
        assertEquals(new java.math.BigDecimal("100.00"), transaction.getDebitAmount(), "Debit amount should be set");
    }

    @Test
    void transactionTypeEnumTest() {
        // Test that our enum works
        fin.model.parser.TransactionType credit = fin.model.parser.TransactionType.CREDIT;
        fin.model.parser.TransactionType debit = fin.model.parser.TransactionType.DEBIT;
        fin.model.parser.TransactionType serviceFee = fin.model.parser.TransactionType.SERVICE_FEE;

        assertNotNull(credit, "CREDIT enum should exist");
        assertNotNull(debit, "DEBIT enum should exist");
        assertNotNull(serviceFee, "SERVICE_FEE enum should exist");

        assertEquals("CREDIT", credit.name(), "CREDIT enum name should be correct");
        assertEquals("DEBIT", debit.name(), "DEBIT enum name should be correct");
        assertEquals("SERVICE_FEE", serviceFee.name(), "SERVICE_FEE enum name should be correct");
    }
}