import { useState, useEffect } from 'react';
import type { Company, FiscalPeriod, Account, Invoice } from '../types/api';


import { useApi } from '../hooks/useApi';

interface InvoiceFormProps {
  company: Company;
  selectedPeriod: FiscalPeriod | null;
  onClose: () => void;
  onCreated: (invoice: Invoice) => void;
}

export default function InvoiceForm({ company, selectedPeriod, onClose, onCreated }: InvoiceFormProps) {
  const api = useApi();
  const [invoiceNumber, setInvoiceNumber] = useState('');
  const [invoiceDate, setInvoiceDate] = useState(selectedPeriod ? selectedPeriod.startDate : new Date().toISOString().slice(0,10));
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState<number | ''>('');
  const [debitAccountId, setDebitAccountId] = useState<number | null>(null);
  const [creditAccountId, setCreditAccountId] = useState<number | null>(null);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const resp = await api.accounts.getChartOfAccounts(company.id);
        if (mounted) setAccounts(resp.data || []);
      } catch {
        // ignore
      }
    })();
    return () => { mounted = false; };
  }, [api, company.id]);

  const submit = async () => {
    setError(null);
    if (!invoiceNumber || !invoiceDate || !amount || !debitAccountId || !creditAccountId || !selectedPeriod) {
      setError('Please fill all required fields');
      return;
    }
    setIsSubmitting(true);
    try {
      const payload = {
        companyId: company.id,
        invoiceNumber,
        invoiceDate,
        description,
        amount: Number(amount),
        debitAccountId,
        creditAccountId,
        fiscalPeriodId: selectedPeriod.id
      };
      const result = await api.dataManagement.createManualInvoice(company.id, payload);
      // API returns envelope or direct object — normalize to invoice
      const invoice: Invoice = (result && typeof result === 'object' && 'data' in result) ? (result.data as Invoice) : (result as Invoice);
      onCreated(invoice);
      onClose();
    } catch (error: unknown) {
      if (error instanceof Error) setError(error.message);
      else setError('Failed to create invoice');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="invoice-form-modal">
      <div className="modal-content">
        <div className="modal-header">
          <h3>Create Manual Invoice</h3>
          <button onClick={onClose}>Close</button>
        </div>

        <div className="modal-body">
          {error && <div className="alert error">{error}</div>}
          <div className="form-row">
            <label>Invoice Number</label>
            <input value={invoiceNumber} onChange={(e) => setInvoiceNumber(e.target.value)} />
          </div>
          <div className="form-row">
            <label>Invoice Date</label>
            <input type="date" value={invoiceDate} onChange={(e) => setInvoiceDate(e.target.value)} />
          </div>
          <div className="form-row">
            <label>Description</label>
            <input value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>
          <div className="form-row">
            <label>Amount</label>
            <input type="number" step="0.01" value={amount === '' ? '' : amount} onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))} />
          </div>
          <div className="form-row">
            <label>Debit Account</label>
            <select value={debitAccountId ?? ''} onChange={(e) => setDebitAccountId(e.target.value ? Number(e.target.value) : null)}>
              <option value="">Select account</option>
              {accounts.map(a => (<option key={a.id} value={a.id}>{a.code} - {a.name}</option>))}
            </select>
          </div>
          <div className="form-row">
            <label>Credit Account</label>
            <select value={creditAccountId ?? ''} onChange={(e) => setCreditAccountId(e.target.value ? Number(e.target.value) : null)}>
              <option value="">Select account</option>
              {accounts.map(a => (<option key={a.id} value={a.id}>{a.code} - {a.name}</option>))}
            </select>
          </div>
        </div>

        <div className="modal-actions">
          <button onClick={onClose} disabled={isSubmitting}>Cancel</button>
          <button onClick={submit} className="primary" disabled={isSubmitting}>{isSubmitting ? 'Creating...' : 'Create Invoice'}</button>
        </div>
      </div>
    </div>
  );
}
