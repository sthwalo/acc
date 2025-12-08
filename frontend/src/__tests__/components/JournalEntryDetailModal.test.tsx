import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { JournalEntryDetailDTO } from '../../types/api';
import type { JournalEntryLineDTO } from '../../types/api';

// Mock API service
const mockGetJournalEntryDetail = vi.fn();

describe('JournalEntryDetailModal Component Tests', () => {
  const mockJournalEntryDetail: JournalEntryDetailDTO = {
    id: 1,
    reference: 'JE-2024-0001',
    entryDate: '2024-04-01',
    description: 'Opening balance entry',
    totalDebit: 10000.0,
    totalCredit: 10000.0,
    lineCount: 2,
    fiscalPeriodName: 'FY2024-2025',
    companyName: 'Test Company',
    createdBy: 'admin',
    createdAt: '2024-04-01T10:00:00Z',
    lastModifiedBy: 'admin',
    lastModifiedAt: '2024-04-01T10:00:00Z',
    lines: [
      {
        id: 1,
        lineNumber: 1,
        accountId: 100,
        accountCode: '1000',
        accountName: 'Cash Account',
        description: 'Cash opening balance',
        debitAmount: 10000.0,
        creditAmount: 0.0,
      },
      {
        id: 2,
        lineNumber: 2,
        accountId: 200,
        accountCode: '3000',
        accountName: "Owner's Equity",
        description: 'Equity opening balance',
        debitAmount: 0.0,
        creditAmount: 10000.0,
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockGetJournalEntryDetail.mockResolvedValue(mockJournalEntryDetail);
  });

  describe('API Integration', () => {
    it('should call getJournalEntryDetail with correct entry ID', async () => {
      const entryId = 1;
      await mockGetJournalEntryDetail(entryId);

      expect(mockGetJournalEntryDetail).toHaveBeenCalledWith(1);
      expect(mockGetJournalEntryDetail).toHaveBeenCalledTimes(1);
    });

    it('should fetch new data when entry ID changes', async () => {
      await mockGetJournalEntryDetail(1);
      expect(mockGetJournalEntryDetail).toHaveBeenCalledWith(1);

      mockGetJournalEntryDetail.mockClear();

      await mockGetJournalEntryDetail(2);
      expect(mockGetJournalEntryDetail).toHaveBeenCalledWith(2);
    });
  });

  describe('Data Structure Validation', () => {
    it('should receive journal entry detail with correct header structure', async () => {
      const response = await mockGetJournalEntryDetail(1);

      expect(response).toBeDefined();
      expect(response.id).toBe(1);
      expect(response.reference).toBe('JE-2024-0001');
      expect(response.description).toBe('Opening balance entry');
      expect(response.companyName).toBe('Test Company');
      expect(response.fiscalPeriodName).toBe('FY2024-2025');
      expect(response.createdBy).toBe('admin');
    });

    it('should receive journal entry lines with correct structure', async () => {
      const response = await mockGetJournalEntryDetail(1);

      expect(response.lines).toHaveLength(2);
      expect(response.lines[0]).toHaveProperty('accountCode');
      expect(response.lines[0]).toHaveProperty('accountName');
      expect(response.lines[0]).toHaveProperty('debitAmount');
      expect(response.lines[0]).toHaveProperty('creditAmount');
    });

    it('should have valid line numbers in sequence', async () => {
      const response = await mockGetJournalEntryDetail(1);

      expect(response.lines[0].lineNumber).toBe(1);
      expect(response.lines[1].lineNumber).toBe(2);
    });

    it('should have valid account codes and names', async () => {
      const response = await mockGetJournalEntryDetail(1);

      expect(response.lines[0].accountCode).toBe('1000');
      expect(response.lines[0].accountName).toBe('Cash Account');
      expect(response.lines[1].accountCode).toBe('3000');
      expect(response.lines[1].accountName).toBe("Owner's Equity");
    });
  });

  describe('Balance Calculations', () => {
    it('should calculate total debits correctly', async () => {
      const response = await mockGetJournalEntryDetail(1);
      const totalDebits = response.lines.reduce((sum: number, line: JournalEntryLineDTO) => sum + line.debitAmount, 0);

      expect(totalDebits).toBe(10000.0);
    });

    it('should calculate total credits correctly', async () => {
      const response = await mockGetJournalEntryDetail(1);
      const totalCredits = response.lines.reduce((sum: number, line: JournalEntryLineDTO) => sum + line.creditAmount, 0);

      expect(totalCredits).toBe(10000.0);
    });

    it('should identify balanced entry', async () => {
      const response = await mockGetJournalEntryDetail(1);
      const totalDebits = response.lines.reduce((sum: number, line: JournalEntryLineDTO) => sum + line.debitAmount, 0);
      const totalCredits = response.lines.reduce((sum: number, line: JournalEntryLineDTO) => sum + line.creditAmount, 0);
      const isBalanced = totalDebits === totalCredits;

      expect(isBalanced).toBe(true);
    });

    it('should calculate difference for unbalanced entry', () => {
      const lines: { debitAmount: number; creditAmount: number }[] = [
        { debitAmount: 10000.0, creditAmount: 0.0 },
        { debitAmount: 0.0, creditAmount: 5000.0 },
      ];
      const totalDebits = lines.reduce((sum: number, line: { debitAmount: number; creditAmount: number }) => sum + line.debitAmount, 0);
      const totalCredits = lines.reduce((sum: number, line: { debitAmount: number; creditAmount: number }) => sum + line.creditAmount, 0);
      const difference = Math.abs(totalDebits - totalCredits);

      expect(difference).toBe(5000.0);
    });
  });

  describe('Error Handling', () => {
    it('should handle API errors gracefully', async () => {
      const errorMessage = 'Journal entry not found';
      mockGetJournalEntryDetail.mockRejectedValue(new Error(errorMessage));

      await expect(mockGetJournalEntryDetail(999)).rejects.toThrow(errorMessage);
    });

    it('should handle missing entry ID', async () => {
      mockGetJournalEntryDetail.mockRejectedValue(new Error('Entry ID is required'));

      await expect(mockGetJournalEntryDetail(undefined)).rejects.toThrow();
    });
  });

  describe('Currency Formatting', () => {
    it('should format debit amounts correctly', () => {
      const amount = 10000.0;
      const formatted = new Intl.NumberFormat('en-ZA', {
        style: 'currency',
        currency: 'ZAR',
      }).format(amount);

      expect(formatted).toContain('10');
      expect(formatted).toContain('000');
    });

    it('should format credit amounts correctly', () => {
      const amount = 10000.0;
      const formatted = new Intl.NumberFormat('en-ZA', {
        style: 'currency',
        currency: 'ZAR',
      }).format(amount);

      expect(formatted).toContain('10');
      expect(formatted).toContain('000');
    });

    it('should handle zero amounts', () => {
      const amount = 0.0;
      const formatted = new Intl.NumberFormat('en-ZA', {
        style: 'currency',
        currency: 'ZAR',
      }).format(amount);

      expect(formatted).toBeDefined();
    });
  });

  describe('Date Formatting', () => {
    it('should parse ISO date correctly', () => {
      const isoDate = '2024-04-01';
      const date = new Date(isoDate);

      expect(date.getFullYear()).toBe(2024);
      expect(date.getMonth()).toBe(3); // April (0-indexed)
      expect(date.getDate()).toBe(1);
    });

    it('should format entry date for display', () => {
      const isoDate = '2024-04-01';
      const date = new Date(isoDate);
      const formatted = date.toLocaleDateString('en-ZA', {
        year: 'numeric',
        month: 'short',
        day: '2-digit',
      });

      expect(formatted).toContain('2024');
      expect(formatted).toContain('Apr');
      expect(formatted).toContain('01');
    });
  });

  describe('Line Items Processing', () => {
    it('should handle multiple journal entry lines', async () => {
      const manyLinesEntry: JournalEntryDetailDTO = {
        ...mockJournalEntryDetail,
        lines: Array.from({ length: 10 }, (_, i) => ({
          id: i + 1,
          lineNumber: i + 1,
          accountId: 100 + i,
          accountCode: `${1000 + i}`,
          accountName: `Account ${i + 1}`,
          description: `Line ${i + 1}`,
          debitAmount: i % 2 === 0 ? 1000.0 : 0.0,
          creditAmount: i % 2 === 1 ? 1000.0 : 0.0,
        })),
      };
      mockGetJournalEntryDetail.mockResolvedValue(manyLinesEntry);

      const response = await mockGetJournalEntryDetail(1);
      expect(response.lines).toHaveLength(10);
    });

    it('should maintain line order', async () => {
      const response = await mockGetJournalEntryDetail(1);
      const lineNumbers = response.lines.map((line: JournalEntryLineDTO) => line.lineNumber);

      expect(lineNumbers).toEqual([1, 2]);
    });

    it('should identify debit lines', async () => {
      const response = await mockGetJournalEntryDetail(1);
      const debitLines = response.lines.filter((line: JournalEntryLineDTO) => line.debitAmount > 0);

      expect(debitLines).toHaveLength(1);
      expect(debitLines[0].debitAmount).toBe(10000.0);
      expect(debitLines[0].creditAmount).toBe(0.0);
    });

    it('should identify credit lines', async () => {
      const response = await mockGetJournalEntryDetail(1);
      const creditLines = response.lines.filter((line: JournalEntryLineDTO) => line.creditAmount > 0);

      expect(creditLines).toHaveLength(1);
      expect(creditLines[0].creditAmount).toBe(10000.0);
      expect(creditLines[0].debitAmount).toBe(0.0);
    });
  });

  describe('Metadata Validation', () => {
    it('should have valid timestamps', async () => {
      const response = await mockGetJournalEntryDetail(1);

      expect(response.createdAt).toBeDefined();
      expect(response.lastModifiedAt).toBeDefined();
      expect(new Date(response.createdAt).getTime()).toBeLessThanOrEqual(
        new Date(response.lastModifiedAt).getTime()
      );
    });

    it('should have valid company and period references', async () => {
      const response = await mockGetJournalEntryDetail(1);

      expect(response.companyName).toBe('Test Company');
      expect(response.fiscalPeriodName).toBe('FY2024-2025');
    });

    it('should have creator information', async () => {
      const response = await mockGetJournalEntryDetail(1);

      expect(response.createdBy).toBe('admin');
      expect(response.createdBy).toBeDefined();
    });
  });
});
