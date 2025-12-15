package fin.validation;

import fin.entity.Account;
import fin.entity.Company;
import fin.repository.AccountRepository;
import fin.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class BankAccountResolverTest {

    @Autowired
    private BankAccountResolver resolver;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    public void finds_bank_account_by_name() {
        // create company and account via repositories
        Company company = new Company("ResolverTestCo");
        companyRepository.save(company);

        Account a = new Account("1230", "Bank", company.getId());
        accountRepository.save(a);

        assertThat(resolver.getDefaultCashAccount(company.getId())).isPresent();
        assertThat(resolver.getDefaultCashAccount(company.getId()).get().getAccountName().toUpperCase()).contains("BANK");
    }
}
