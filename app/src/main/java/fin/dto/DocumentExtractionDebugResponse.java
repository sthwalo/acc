package fin.dto;

import java.time.LocalDate;
import java.util.List;

public class DocumentExtractionDebugResponse {
    private final List<String> extractedLines;
    private final String accountNumber;
    private final String statementPeriodRaw;
    private final LocalDate statementPeriodStart;
    private final LocalDate statementPeriodEnd;
    private final List<String> errors;

    public DocumentExtractionDebugResponse(List<String> extractedLines, String accountNumber, String statementPeriodRaw, LocalDate statementPeriodStart, LocalDate statementPeriodEnd, List<String> errors) {
        this.extractedLines = extractedLines;
        this.accountNumber = accountNumber;
        this.statementPeriodRaw = statementPeriodRaw;
        this.statementPeriodStart = statementPeriodStart;
        this.statementPeriodEnd = statementPeriodEnd;
        this.errors = errors;
    }

    public List<String> getExtractedLines() { return extractedLines; }
    public String getAccountNumber() { return accountNumber; }
    public String getStatementPeriodRaw() { return statementPeriodRaw; }
    public LocalDate getStatementPeriodStart() { return statementPeriodStart; }
    public LocalDate getStatementPeriodEnd() { return statementPeriodEnd; }
    public List<String> getErrors() { return errors; }
}