package fin.service;

import fin.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development for TransactionBatchProcessor module
 * This module handles batch processing of transactions for classification
 *
 * STATUS: Service implemented, tests enabled
 * TODO: Verify integration with all dependent services
 */
public class TransactionBatchProcessorTest {

    @Mock
    private RuleMappingService mockRuleService;

    @Mock
    private JournalEntryGenerator mockJournalGenerator;

    private TransactionBatchProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new TransactionBatchProcessor(mockRuleService, mockJournalGenerator);
    }

    @Test
    void testProcessBatch_AllTransactionsClassifiedByRules() {
        // Given
        List<BankTransaction> transactions = createTestTransactions(3);
        Map<String, RuleMapping> rules = Map.of(
            "SALARY PAYMENT", new RuleMapping("8100", "Employee Costs"),
            "BANK FEE", new RuleMapping("9600", "Bank Charges")
        );

        when(mockRuleService.loadTransactionMappingRules(1L)).thenReturn(rules);
        when(mockRuleService.findMatchingRule(anyString(), eq(rules)))
            .thenReturn(new RuleMapping("8100", "Employee Costs"))
            .thenReturn(new RuleMapping("9600", "Bank Charges"))
            .thenReturn(new RuleMapping("4000", "Service Revenue")); // All transactions matched by rules

        when(mockJournalGenerator.createJournalEntryForTransaction(any(BankTransaction.class), any(ClassificationResult.class)))
            .thenReturn(true);

        // When
        BatchProcessingResult result = processor.processBatch(transactions, 1L);

        // Then
        assertEquals(3, result.getProcessedCount());
        assertEquals(3, result.getClassifiedCount());
        assertEquals(0, result.getFailedCount());
        assertTrue(result.isSuccess());

        verify(mockRuleService).loadTransactionMappingRules(1L);
        verify(mockRuleService, times(3)).findMatchingRule(anyString(), eq(rules));
        verify(mockJournalGenerator, times(3)).createJournalEntryForTransaction(any(BankTransaction.class), any(ClassificationResult.class));
    }

    @Test
    void testProcessBatch_SomeTransactionsFailClassification() {
        // Given
        List<BankTransaction> transactions = createTestTransactions(2);
        Map<String, RuleMapping> emptyRules = Map.of();

        when(mockRuleService.loadTransactionMappingRules(1L)).thenReturn(emptyRules);
        when(mockRuleService.findMatchingRule(anyString(), eq(emptyRules))).thenReturn(null);

        // When
        BatchProcessingResult result = processor.processBatch(transactions, 1L);

        // Then
        assertEquals(2, result.getProcessedCount());
        assertEquals(0, result.getClassifiedCount());
        assertEquals(2, result.getFailedCount());
        assertFalse(result.isSuccess());

        verify(mockJournalGenerator, never()).createJournalEntryForTransaction(any(BankTransaction.class), any(ClassificationResult.class));
    }

    @Test
    void testProcessBatch_JournalEntryCreationFails() {
        // Given
        List<BankTransaction> transactions = createTestTransactions(1);
        Map<String, RuleMapping> rules = Map.of("SALARY", new RuleMapping("8100", "Employee Costs"));

        when(mockRuleService.loadTransactionMappingRules(1L)).thenReturn(rules);
        when(mockRuleService.findMatchingRule(anyString(), eq(rules)))
            .thenReturn(new RuleMapping("8100", "Employee Costs"));

        when(mockJournalGenerator.createJournalEntryForTransaction(any(BankTransaction.class), any(ClassificationResult.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When
        BatchProcessingResult result = processor.processBatch(transactions, 1L);

        // Then
        assertEquals(1, result.getProcessedCount());
        assertEquals(1, result.getClassifiedCount()); // Classification succeeded
        assertEquals(1, result.getFailedCount()); // But journal entry creation failed
        assertFalse(result.isSuccess());
    }

    @Test
    void testProcessBatchWithStatistics() {
        // Given
        List<BankTransaction> transactions = createTestTransactions(4);
        Map<String, RuleMapping> rules = Map.of("SALARY", new RuleMapping("8100", "Employee Costs"));

        when(mockRuleService.loadTransactionMappingRules(1L)).thenReturn(rules);
        when(mockRuleService.findMatchingRule(anyString(), eq(rules)))
            .thenReturn(new RuleMapping("8100", "Employee Costs"))
            .thenReturn(new RuleMapping("8100", "Employee Costs"))
            .thenReturn(null)
            .thenReturn(null);

        when(mockJournalGenerator.createJournalEntryForTransaction(any(BankTransaction.class), any(ClassificationResult.class)))
            .thenReturn(true);

        // When
        BatchProcessingStatistics stats = processor.processBatchWithStatistics(transactions, 1L);

        // Then
        assertEquals(4, stats.getTotalTransactions());
        assertEquals(2, stats.getClassifiedTransactions()); // Only 2 matched by rules
        assertEquals(2, stats.getUnclassifiedTransactions());
        assertEquals(50.0, stats.getClassificationRate(), 0.1);
    }

    @Test
    void testValidateTransactions_ValidTransactions() {
        // Given
        List<BankTransaction> transactions = createValidTestTransactions(2);

        // When
        List<String> errors = processor.validateTransactions(transactions);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void testValidateTransactions_InvalidTransactions() {
        // Given - each transaction has only one validation error
        List<BankTransaction> transactions = Arrays.asList(
            createTransactionWithMissingReference(),
            createTransactionWithMissingDetails(),
            createTransactionWithMissingAmount(),
            createTransactionWithMissingDate()
        );

        // When
        List<String> errors = processor.validateTransactions(transactions);

        // Then
        assertEquals(4, errors.size());
        assertTrue(errors.get(0).contains("Missing reference number"));
        assertTrue(errors.get(1).contains("Missing transaction details"));
        assertTrue(errors.get(2).contains("Missing transaction amount"));
        assertTrue(errors.get(3).contains("Missing transaction date"));
    }

    @Test
    void testProcessBatchValidated_ValidTransactions() {
        // Given
        List<BankTransaction> transactions = createValidTestTransactions(2);
        Map<String, RuleMapping> rules = Map.of("SALARY", new RuleMapping("8100", "Employee Costs"));

        when(mockRuleService.loadTransactionMappingRules(1L)).thenReturn(rules);
        when(mockRuleService.findMatchingRule(anyString(), eq(rules)))
            .thenReturn(new RuleMapping("8100", "Employee Costs"));

        when(mockJournalGenerator.createJournalEntryForTransaction(any(BankTransaction.class), any(ClassificationResult.class)))
            .thenReturn(true);

        // When
        BatchProcessingResult result = processor.processBatchValidated(transactions, 1L);

        // Then
        assertEquals(2, result.getProcessedCount());
        assertEquals(2, result.getClassifiedCount());
    }

    @Test
    void testProcessBatchValidated_InvalidTransactions() {
        // Given
        List<BankTransaction> transactions = Arrays.asList(createTransactionWithMissingReference());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            processor.processBatchValidated(transactions, 1L)
        );

        assertTrue(exception.getMessage().contains("validation failed"));
    }

    // Helper methods
    private List<BankTransaction> createTestTransactions(int count) {
        List<BankTransaction> transactions = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            BankTransaction tx = new BankTransaction();
            tx.setReference("REF" + i);
            tx.setDetails("TRANSACTION " + i + " DETAILS");
            tx.setDebitAmount(BigDecimal.valueOf(100.00 * (i + 1)));
            tx.setTransactionDate(LocalDate.now().minusDays(i));
            transactions.add(tx);
        }
        return transactions;
    }

    private List<BankTransaction> createValidTestTransactions(int count) {
        List<BankTransaction> transactions = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            BankTransaction tx = new BankTransaction();
            tx.setReference("VALID_REF" + i);
            tx.setDetails("VALID TRANSACTION " + i);
            tx.setDebitAmount(BigDecimal.valueOf(100.00));
            tx.setTransactionDate(LocalDate.now());
            transactions.add(tx);
        }
        return transactions;
    }

    private BankTransaction createTransactionWithMissingReference() {
        BankTransaction tx = new BankTransaction();
        tx.setReference(null);
        tx.setDetails("VALID DETAILS");
        tx.setDebitAmount(BigDecimal.valueOf(100.00));
        tx.setTransactionDate(LocalDate.now());
        return tx;
    }

    private BankTransaction createTransactionWithMissingDetails() {
        BankTransaction tx = new BankTransaction();
        tx.setReference("VALID_REF");
        tx.setDetails(null);
        tx.setDebitAmount(BigDecimal.valueOf(100.00));
        tx.setTransactionDate(LocalDate.now());
        return tx;
    }

    private BankTransaction createTransactionWithMissingAmount() {
        BankTransaction tx = new BankTransaction();
        tx.setReference("VALID_REF");
        tx.setDetails("VALID DETAILS");
        tx.setDebitAmount(null);
        tx.setCreditAmount(null);
        tx.setTransactionDate(LocalDate.now());
        return tx;
    }

    private BankTransaction createTransactionWithMissingDate() {
        BankTransaction tx = new BankTransaction();
        tx.setReference("VALID_REF");
        tx.setDetails("VALID DETAILS");
        tx.setDebitAmount(BigDecimal.valueOf(100.00));
        tx.setTransactionDate(null);
        return tx;
    }
}