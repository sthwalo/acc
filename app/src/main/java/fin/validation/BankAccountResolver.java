/*
 * FIN Financial Management System
 *
 * Helper to resolve the company's default bank/cash account by name.
 */
package fin.validation;

import fin.entity.Account;
import fin.service.journal.AccountService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Component
public class BankAccountResolver {

    private static final Logger LOGGER = Logger.getLogger(BankAccountResolver.class.getName());

    private final AccountService accountService;

    public BankAccountResolver(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Resolve a company's default bank/cash account by searching active account names.
     * Returns Optional.empty() if none found.
     */
    public Optional<Account> getDefaultCashAccount(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }

        List<String> preferredNames = Arrays.asList(
            "BANK",
            "CASH",
            "CASH AND CASH EQUIVALENTS",
            "CASH/CASH EQUIV",
            "PETTY CASH",
            "CASH EQUIVALENTS"
        );

        List<Account> accounts = accountService.getActiveAccountsByCompany(companyId);

        // First try exact or contains match on preferred names
        Optional<Account> match = accounts.stream()
            .filter(a -> a.getAccountName() != null)
            .filter(a -> {
                String name = a.getAccountName().trim().toUpperCase();
                return preferredNames.stream().anyMatch(name::contains);
            })
            .findFirst();

        if (match.isPresent()) {
            return match;
        }

        // No match found - return empty. Caller should fail-fast with clear message.
        LOGGER.info("No default bank/cash account found for company: " + companyId);
        return Optional.empty();
    }
}
