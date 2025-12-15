import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import DataManagementView from '../../components/DataManagementView';
import { vi } from 'vitest';
import type { Company } from '../../types/api';

// Mock useApi to provide necessary methods
vi.mock('../../hooks/useApi', () => {
  return {
    useApi: () => ({
      dataManagement: {
        generateInvoicePdf: vi.fn(async (companyId: number, invoiceId: number) => ({ success: true, message: 'PDF generated successfully' })),
        downloadInvoice: vi.fn(async (companyId: number, invoiceId: number) => { throw new Error('download failed'); }),
        getManualInvoices: async (companyId: number) => ({ success: true, data: [{ id: 3, companyId, invoiceNumber: 'INV-3', invoiceDate: '2025-12-01', amount: 123 }] })
      },
      classification: {
        initializeChartOfAccounts: async () => ({ success: true, message: 'ok' })
      },
      fiscalPeriods: {
        getFiscalPeriods: async () => [{ id: 10, companyId: 5, periodName: '2025-12', startDate: '2025-12-01', endDate: '2025-12-31', createdAt: '', createdBy: 1, updatedAt: '', updatedBy: 1, closed: false }]
      },
      transactions: {
        getTransactions: async () => ({ success: true, data: [] })
      }
    })
  };
});

describe('DataManagementView generate invoice flow', () => {
  it('shows fallback message when download fails', async () => {
    const company: Company = { id: 5, name: 'TestCo' } as any;
    render(<DataManagementView selectedCompany={company} />);

    // Wait for fiscal periods to load
    await waitFor(() => screen.getByText(/Operations/i));

    // Click the Operations tab, then Generate Invoice PDF (opens picker)
    const operationsTab = screen.getByRole('button', { name: /Operations/i });
    fireEvent.click(operationsTab);

    const generateBtn = await screen.findByRole('button', { name: /Generate Invoice PDF/i });
    fireEvent.click(generateBtn);

    // Wait for modal and click the modal's Generate PDF button
    await waitFor(() => screen.getByText('INV-3'));
    const generateButtons = await screen.findAllByRole('button', { name: /Generate PDF/i });
    // The first match is the menu header; pick the actual modal button with exact text
    const modalGenerate = generateButtons.find(b => b.textContent?.trim() === 'Generate PDF');
    if (!modalGenerate) throw new Error('Modal generate button not found');
    fireEvent.click(modalGenerate);

    // Wait for operation result to show fallback message
    await waitFor(() => screen.getByText(/download failed/i));
    expect(screen.getByText(/download failed/i)).toBeTruthy();
  });
});
