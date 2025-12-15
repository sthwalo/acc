import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import InvoicePickerModal from '../../components/InvoicePickerModal';
import { vi } from 'vitest';

// Mock useApi hook
vi.mock('../../hooks/useApi', () => {
  return {
    useApi: () => ({
      dataManagement: {
        getManualInvoices: async (_companyId: number) => ({ success: true, data: [ { id: 1, companyId: 5, invoiceNumber: 'INV-1', invoiceDate: '2025-12-01', amount: 100 } ] })
      }
    })
  };
});

describe('InvoicePickerModal', () => {
  it('renders invoices and calls onSelect when Generate PDF clicked', async () => {
    const onClose = vi.fn();
    const onSelect = vi.fn();

    render(<InvoicePickerModal company={{ id: 5, name: 'TestCo' }} onClose={onClose} onSelect={onSelect} />);

    expect(await screen.findByText('INV-1')).toBeTruthy();
    const button = screen.getByRole('button', { name: /Generate PDF/i });
    fireEvent.click(button);

    await waitFor(() => expect(onSelect).toHaveBeenCalledWith(1));
  });
});
