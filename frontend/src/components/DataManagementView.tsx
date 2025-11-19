import { useState, useEffect, useCallback } from 'react';
import { Database, Edit, Trash2, Plus, Save, X, AlertCircle, CheckCircle, Calendar } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import type { Company, Transaction, FiscalPeriod } from '../types/api';

interface DataManagementViewProps {
  selectedCompany: Company;
}

interface EditableTransaction extends Transaction {
  isEditing?: boolean;
  originalData?: Transaction;
}

export default function DataManagementView({ selectedCompany }: DataManagementViewProps) {
  const api = useApi();
  const [transactions, setTransactions] = useState<EditableTransaction[]>([]);
  const [fiscalPeriods, setFiscalPeriods] = useState<FiscalPeriod[]>([]);
  const [selectedPeriod, setSelectedPeriod] = useState<FiscalPeriod | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [editingTransaction, setEditingTransaction] = useState<EditableTransaction | null>(null);

  const loadFiscalPeriods = useCallback(async () => {
    try {
      const data = await api.getFiscalPeriods(Number(selectedCompany.id));
      setFiscalPeriods(data);
      // Auto-select the first active fiscal period if available
      if (data.length > 0 && !selectedPeriod) {
        const activePeriod = data.find(p => !p.closed) || data[0];
        setSelectedPeriod(activePeriod);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load fiscal periods');
    }
  }, [api, selectedCompany.id, selectedPeriod]);

  const loadTransactions = useCallback(async () => {
    if (!selectedPeriod) return;

    try {
      setIsLoading(true);
      setError(null);
      const result = await api.getTransactions(Number(selectedCompany.id), selectedPeriod.id);
      const mappedTransactions: Transaction[] = result.data.map((apiTransaction: any) => ({
        id: apiTransaction.id,
        company_id: apiTransaction.companyId,
        fiscal_period_id: apiTransaction.fiscalPeriodId,
        date: apiTransaction.transactionDate,
        description: apiTransaction.details || '',
        amount: apiTransaction.debitAmount > 0 ? apiTransaction.debitAmount : apiTransaction.creditAmount,
        type: apiTransaction.debitAmount > 0 ? 'debit' : 'credit',
        category: apiTransaction.category || '',
        reference: apiTransaction.reference || '',
        created_at: apiTransaction.createdAt
      }));
      setTransactions(mappedTransactions.map(t => ({ ...t, isEditing: false })));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load transactions');
    } finally {
      setIsLoading(false);
    }
  }, [api, selectedCompany.id, selectedPeriod]);

  useEffect(() => {
    loadFiscalPeriods();
  }, [loadFiscalPeriods]);

  useEffect(() => {
    if (selectedPeriod) {
      loadTransactions();
    }
  }, [selectedPeriod, loadTransactions]);

  const startEditing = (transaction: EditableTransaction) => {
    setEditingTransaction({ ...transaction, originalData: { ...transaction } });
    setTransactions(prev =>
      prev.map(t =>
        t.id === transaction.id
          ? { ...t, isEditing: true }
          : { ...t, isEditing: false }
      )
    );
  };

  const cancelEditing = () => {
    if (editingTransaction?.originalData) {
      setTransactions(prev =>
        prev.map(t =>
          t.id === editingTransaction.id
            ? { ...editingTransaction.originalData!, isEditing: false }
            : t
        )
      );
    }
    setEditingTransaction(null);
  };

  const saveTransaction = async () => {
    if (!editingTransaction) return;

    try {
      setError(null);
      // TODO: Implement actual update API call
      // For now, simulate the update
      await new Promise(resolve => setTimeout(resolve, 1000));

      setTransactions(prev =>
        prev.map(t =>
          t.id === editingTransaction.id
            ? { ...editingTransaction, isEditing: false }
            : t
        )
      );

      setEditingTransaction(null);
      setSuccess('Transaction updated successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update transaction');
    }
  };

  const deleteTransaction = async (transactionId: number) => {
    if (!confirm('Are you sure you want to delete this transaction? This action cannot be undone.')) {
      return;
    }

    try {
      setError(null);
      // TODO: Implement actual delete API call
      // For now, simulate the deletion
      await new Promise(resolve => setTimeout(resolve, 1000));

      setTransactions(prev => prev.filter(t => t.id !== transactionId));
      setSuccess('Transaction deleted successfully');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete transaction');
    }
  };

  const updateEditingTransaction = (field: keyof Transaction, value: string | number) => {
    if (!editingTransaction) return;

    setEditingTransaction(prev => prev ? { ...prev, [field]: value } : null);
  };

  if (isLoading || !selectedPeriod) {
    if (fiscalPeriods.length === 0) {
      return (
        <div className="loading-state">
          <div className="spinner large"></div>
          <p>Loading fiscal periods...</p>
        </div>
      );
    }

    return (
      <div className="data-management-view">
        <div className="view-header">
          <h2>Data Management</h2>
          <p>Manual data entry, corrections, and audit trails for {selectedCompany.name}</p>
        </div>

        {/* Fiscal Period Selector */}
        <div className="fiscal-period-selector">
          <label htmlFor="fiscal-period-select">
            <Calendar size={20} />
            Select Fiscal Period:
          </label>
          <select
            id="fiscal-period-select"
            value={selectedPeriod?.id || ''}
            onChange={(e) => {
              const periodId = parseInt(e.target.value);
              const period = fiscalPeriods.find(p => p.id === periodId);
              setSelectedPeriod(period || null);
            }}
          >
            <option value="">Select a fiscal period...</option>
            {fiscalPeriods.map((period) => (
              <option key={period.id} value={period.id}>
                {period.periodName} ({period.startDate} to {period.endDate})
                {!period.closed ? ' - Active' : ''}
              </option>
            ))}
          </select>
        </div>

        {!selectedPeriod && (
          <div className="empty-state">
            <Calendar size={48} />
            <h3>Please select a fiscal period</h3>
            <p>Choose a fiscal period from the dropdown above to manage transactions.</p>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="data-management-view">
      <div className="view-header">
        <h2>Data Management</h2>
        <p>Manual data entry, corrections, and audit trails for {selectedCompany.name}</p>
      </div>

      {/* Fiscal Period Selector */}
      <div className="fiscal-period-selector">
        <label htmlFor="fiscal-period-select">
          <Calendar size={20} />
          Select Fiscal Period:
        </label>
        <select
          id="fiscal-period-select"
          value={selectedPeriod?.id || ''}
          onChange={(e) => {
            const periodId = parseInt(e.target.value);
            const period = fiscalPeriods.find(p => p.id === periodId);
            setSelectedPeriod(period || null);
          }}
        >
          <option value="">Select a fiscal period...</option>
          {fiscalPeriods.map((period) => (
            <option key={period.id} value={period.id}>
              {period.periodName} ({period.startDate} to {period.endDate})
              {!period.closed ? ' - Active' : ''}
            </option>
          ))}
        </select>
      </div>

      {error && (
        <div className="alert alert-error">
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="alert alert-success">
          <CheckCircle size={20} />
          <span>{success}</span>
        </div>
      )}

      {!selectedPeriod && fiscalPeriods.length > 0 && (
        <div className="alert alert-info">
          <Calendar size={20} />
          <span>Please select a fiscal period to manage transactions.</span>
        </div>
      )}

      <div className="data-actions">
        <button className="action-button primary">
          <Plus size={20} />
          Add New Transaction
        </button>
        <button className="action-button secondary" onClick={loadTransactions}>
          <Database size={20} />
          Refresh Data
        </button>
      </div>

      <div className="transactions-table-container">
        <table className="transactions-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Description</th>
              <th>Amount</th>
              <th>Type</th>
              <th>Category</th>
              <th>Reference</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((transaction) => (
              <tr key={transaction.id}>
                <td>
                  {transaction.isEditing ? (
                    <input
                      type="date"
                      value={editingTransaction?.date || transaction.date}
                      onChange={(e) => updateEditingTransaction('date', e.target.value)}
                    />
                  ) : (
                    new Date(transaction.date).toLocaleDateString()
                  )}
                </td>
                <td>
                  {transaction.isEditing ? (
                    <input
                      type="text"
                      value={editingTransaction?.description || transaction.description}
                      onChange={(e) => updateEditingTransaction('description', e.target.value)}
                    />
                  ) : (
                    transaction.description
                  )}
                </td>
                <td>
                  {transaction.isEditing ? (
                    <input
                      type="number"
                      step="0.01"
                      value={editingTransaction?.amount || transaction.amount}
                      onChange={(e) => updateEditingTransaction('amount', parseFloat(e.target.value))}
                    />
                  ) : (
                    `R ${transaction.amount.toFixed(2)}`
                  )}
                </td>
                <td>
                  {transaction.isEditing ? (
                    <select
                      value={editingTransaction?.type || transaction.type}
                      onChange={(e) => updateEditingTransaction('type', e.target.value as 'debit' | 'credit')}
                    >
                      <option value="debit">Debit</option>
                      <option value="credit">Credit</option>
                    </select>
                  ) : (
                    <span className={`type-badge ${transaction.type}`}>
                      {transaction.type.toUpperCase()}
                    </span>
                  )}
                </td>
                <td>
                  {transaction.isEditing ? (
                    <input
                      type="text"
                      value={editingTransaction?.category || transaction.category}
                      onChange={(e) => updateEditingTransaction('category', e.target.value)}
                    />
                  ) : (
                    transaction.category
                  )}
                </td>
                <td>
                  {transaction.isEditing ? (
                    <input
                      type="text"
                      value={editingTransaction?.reference || transaction.reference}
                      onChange={(e) => updateEditingTransaction('reference', e.target.value)}
                    />
                  ) : (
                    transaction.reference
                  )}
                </td>
                <td>
                  <div className="action-buttons">
                    {transaction.isEditing ? (
                      <>
                        <button
                          className="action-button save"
                          onClick={saveTransaction}
                          title="Save changes"
                        >
                          <Save size={16} />
                        </button>
                        <button
                          className="action-button cancel"
                          onClick={cancelEditing}
                          title="Cancel editing"
                        >
                          <X size={16} />
                        </button>
                      </>
                    ) : (
                      <>
                        <button
                          className="action-button edit"
                          onClick={() => startEditing(transaction)}
                          title="Edit transaction"
                        >
                          <Edit size={16} />
                        </button>
                        <button
                          className="action-button delete"
                          onClick={() => deleteTransaction(transaction.id)}
                          title="Delete transaction"
                        >
                          <Trash2 size={16} />
                        </button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {transactions.length === 0 && (
          <div className="empty-state">
            <Database size={48} />
            <h3>No transactions found</h3>
            <p>There are no transactions to manage for this company.</p>
          </div>
        )}
      </div>
    </div>
  );
}