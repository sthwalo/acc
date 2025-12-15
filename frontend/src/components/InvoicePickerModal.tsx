import { useEffect, useState } from 'react';
import type { Company, Invoice } from '../types/api';
import { useApi } from '../hooks/useApi';

interface InvoicePickerModalProps {
  company: Company;
  onClose: () => void;
  onSelect: (invoiceId: number) => void;
}

export default function InvoicePickerModal({ company, onClose, onSelect }: InvoicePickerModalProps) {
  const api = useApi();
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    (async () => {
      setLoading(true);
      try {
        const resp = await api.dataManagement.getManualInvoices(company.id);
        if (mounted) setInvoices(resp.data || []);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load invoices');
      } finally {
        setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, [api, company.id]);

  return (
    <div className="invoice-picker-modal">
      <div className="modal-content">
        <div className="modal-header">
          <h3>Select an Invoice</h3>
          <button onClick={onClose}>Close</button>
        </div>
        <div className="modal-body">
          {error && <div className="alert error">{error}</div>}
          {loading ? (
            <div>Loading invoices...</div>
          ) : (
            <div className="invoice-list">
              {invoices.length === 0 && <div className="empty">No invoices found</div>}
              {invoices.map(inv => (
                <div key={inv.id} className="invoice-row">
                  <div className="invoice-info">
                    <div className="invoice-number">{inv.invoiceNumber || `#${inv.id}`}</div>
                    <div className="invoice-date">{inv.invoiceDate}</div>
                  </div>
                  <div className="invoice-actions">
                    <button onClick={() => onSelect(inv.id)}>Generate PDF</button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
