package fin.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test-Driven Development for TransactionParsingContext
 * This class provides context information for transaction parsing operations
 *
 * STATUS: Test coverage gap identified - TransactionParsingContext has no dedicated tests
 * TODO: Create comprehensive tests for Builder pattern and validation logic
 */
public class TransactionParsingContextTest {

    @Test
    @DisplayName("Should build context with all required fields")
    void testBuild_WithAllFields() {
        // Given
        LocalDate statementDate = LocalDate.of(2025, 9, 22);
        String accountNumber = "1234567890";
        String statementPeriod = "SEP2025";
        String sourceFile = "statement.pdf";

        // When
        TransactionParsingContext context = new TransactionParsingContext.Builder()
            .statementDate(statementDate)
            .accountNumber(accountNumber)
            .statementPeriod(statementPeriod)
            .sourceFile(sourceFile)
            .build();

        // Then
        assertNotNull(context);
        assertEquals(statementDate, context.getStatementDate());
        assertEquals(accountNumber, context.getAccountNumber());
        assertEquals(statementPeriod, context.getStatementPeriod());
        assertEquals(sourceFile, context.getSourceFile());
    }

    @Test
    @DisplayName("Should build context with minimal required fields")
    void testBuild_WithMinimalFields() {
        // Given
        LocalDate statementDate = LocalDate.of(2025, 9, 22);

        // When
        TransactionParsingContext context = new TransactionParsingContext.Builder()
            .statementDate(statementDate)
            .build();

        // Then
        assertNotNull(context);
        assertEquals(statementDate, context.getStatementDate());
        assertNull(context.getAccountNumber());
        assertNull(context.getStatementPeriod());
        assertNull(context.getSourceFile());
    }

    @Test
    @DisplayName("Should throw exception when statement date is null")
    void testBuild_ThrowsException_WhenStatementDateIsNull() {
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            new TransactionParsingContext.Builder()
                .accountNumber("1234567890")
                .statementPeriod("SEP2025")
                .sourceFile("statement.pdf")
                .build()
        );

        assertEquals("Statement date is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should support fluent builder pattern")
    void testBuilder_FluentPattern() {
        // Given & When
        TransactionParsingContext context = new TransactionParsingContext.Builder()
            .statementDate(LocalDate.of(2025, 9, 22))
            .accountNumber("1234567890")
            .statementPeriod("SEP2025")
            .sourceFile("statement.pdf")
            .build();

        // Then
        assertNotNull(context);
        assertEquals("1234567890", context.getAccountNumber());
        assertEquals("SEP2025", context.getStatementPeriod());
        assertEquals("statement.pdf", context.getSourceFile());
    }

    @Test
    @DisplayName("Should handle null optional fields gracefully")
    void testBuilder_WithNullOptionalFields() {
        // When
        TransactionParsingContext context = new TransactionParsingContext.Builder()
            .statementDate(LocalDate.of(2025, 9, 22))
            .accountNumber(null)
            .statementPeriod(null)
            .sourceFile(null)
            .build();

        // Then
        assertNotNull(context);
        assertNull(context.getAccountNumber());
        assertNull(context.getStatementPeriod());
        assertNull(context.getSourceFile());
    }

    @Test
    @DisplayName("Should create immutable context object")
    void testImmutability() {
        // Given
        LocalDate originalDate = LocalDate.of(2025, 9, 22);
        TransactionParsingContext context = new TransactionParsingContext.Builder()
            .statementDate(originalDate)
            .accountNumber("1234567890")
            .build();

        // When - try to modify the original date
        LocalDate modifiedDate = originalDate.plusDays(1); // This creates a new LocalDate, doesn't modify original

        // Then - context should still have original date
        assertEquals(LocalDate.of(2025, 9, 22), context.getStatementDate());
        assertNotEquals(modifiedDate, context.getStatementDate()); // Verify the dates are different
    }

    @Test
    @DisplayName("Should handle edge case with empty strings")
    void testBuilder_WithEmptyStrings() {
        // When
        TransactionParsingContext context = new TransactionParsingContext.Builder()
            .statementDate(LocalDate.of(2025, 9, 22))
            .accountNumber("")
            .statementPeriod("")
            .sourceFile("")
            .build();

        // Then
        assertNotNull(context);
        assertEquals("", context.getAccountNumber());
        assertEquals("", context.getStatementPeriod());
        assertEquals("", context.getSourceFile());
    }
}