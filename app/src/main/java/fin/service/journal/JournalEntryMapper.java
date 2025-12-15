package fin.service.journal;

import fin.dto.JournalEntryDTO;
import fin.entity.Account;
import fin.entity.JournalEntry;
import fin.entity.JournalEntryLine;
import fin.repository.AccountRepository;
import fin.repository.JournalEntryLineRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class JournalEntryMapper {

    private final JournalEntryLineRepository journalEntryLineRepository;
    private final AccountRepository accountRepository;

    public JournalEntryMapper(JournalEntryLineRepository journalEntryLineRepository,
                              AccountRepository accountRepository) {
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.accountRepository = accountRepository;
    }

    public JournalEntryDTO toDto(JournalEntry entry) {
        List<JournalEntryLine> lines = journalEntryLineRepository.findByJournalEntryId(entry.getId());

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int lineCount = lines.size();

        for (JournalEntryLine line : lines) {
            if (line.getDebitAmount() != null) totalDebit = totalDebit.add(line.getDebitAmount());
            if (line.getCreditAmount() != null) totalCredit = totalCredit.add(line.getCreditAmount());
        }

        String description = buildDescriptionFromLines(entry, lines);

        return new JournalEntryDTO(
            entry.getId(),
            entry.getReference(),
            entry.getEntryDate(),
            description,
            "Bank Transaction",
            entry.getCreatedBy() != null ? entry.getCreatedBy() : "FIN",
            entry.getCreatedAt(),
            totalDebit,
            totalCredit,
            lineCount
        );
    }

    private String buildDescriptionFromLines(JournalEntry entry, List<JournalEntryLine> lines) {
        if (entry.getDescription() != null) {
            String headerDesc = entry.getDescription().trim();
            if (!headerDesc.equalsIgnoreCase("null") && !headerDesc.matches("(?i)\\s*null\\s*(-\\s*null\\s*)?")) {
                if (!headerDesc.isEmpty()) return headerDesc;
            }
        }
        if (lines == null || lines.isEmpty()) return "No description";
        String debitAccount = null;
        String creditAccount = null;
        for (JournalEntryLine line : lines) {
            Account account = accountRepository.findById(line.getAccountId()).orElse(null);
            if (account != null) {
                if (line.getDebitAmount() != null && line.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
                    debitAccount = account.getAccountName();
                } else if (line.getCreditAmount() != null && line.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
                    creditAccount = account.getAccountName();
                }
            }
        }
        if (debitAccount != null && creditAccount != null) return debitAccount + " - " + creditAccount;
        if (debitAccount != null) return debitAccount;
        if (creditAccount != null) return creditAccount;
        return "No description";
    }
}
