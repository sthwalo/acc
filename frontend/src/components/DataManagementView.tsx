import { useState, useEffect, useCallback, useMemo } from 'react';
import { Database, Edit, Trash2, Plus, Save, X, AlertCircle, CheckCircle, Calendar, Settings, FileText, Receipt, FileCheck, BookOpen, AlertTriangle, History, RotateCcw, Download, Loader, ChevronLeft, ChevronRight } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
import AccountSelector from './shared/AccountSelector';
import type { Company, Transaction, FiscalPeriod, ApiTransaction, ApiError, Account } from '../types/api';

interface DataManagementViewProps {
  selectedCompany: Company;
}

interface EditableTransaction extends Transaction {
  isEditing?: boolean;
  originalData?: Transaction;
}

interface MenuItem {
  id: string;
  name: string;
  description: string;
  icon: React.ComponentType<{ size?: number; className?: string }>;
  action: () => void;
  disabled?: boolean;
}

type TabType = 'manual-entry' | 'classification' | 'operations';

const ITEMS_PER_PAGE = 50;

export default function DataManagementView({ selectedCompany }: DataManagementViewProps) {
  const api = useApi();
  const [activeTab, setActiveTab] = useState<TabType>('manual-entry');
  const [transactions, setTransactions] = useState<EditableTransaction[]>([]);
  const [fiscalPeriods, setFiscalPeriods] = useState<FiscalPeriod[]>([]);
  const [selectedPeriod, setSelectedPeriod] = useState<FiscalPeriod | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [apiMessage, setApiMessage] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [editingTransaction, setEditingTransaction] = useState<EditableTransaction | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [currentOperation, setCurrentOperation] = useState<string | null>(null);
  const [operationResult, setOperationResult] = useState<{ success: boolean; message: string } | null>(null);
  const [currentPage, setCurrentPage] = useState(1);

  const handleOperation = useCallback(async (operation: string, action: () => Promise<unknown>) => {
    setIsProcessing(true);
    setCurrentOperation(operation);
    setOperationResult(null);

    try {
      const response = await action();
      
      // Handle structured API responses
      if (response && typeof response === 'object' && 'message' in response) {
        const apiResponse = response as { success: boolean; message: string; data?: unknown };
        setOperationResult({ success: apiResponse.success ?? true, message: apiResponse.message });
      } else {
        // Handle plain string responses (legacy)
        setOperationResult({ success: true, message: String(response) || `${operation} completed successfully` });
      }
    } catch (error: unknown) {
      const apiError = error as ApiError;
      setOperationResult({
        success: false,
        message: apiError.response?.data?.message || (error as Error).message || `Failed to ${operation.toLowerCase()}`
      });
    } finally {
      setIsProcessing(false);
      setCurrentOperation(null);
    }
  }, []);

  // Memoize menu item actions to prevent infinite re-renders
  const initializeAccountsAction = useCallback(() => handleOperation('Initialize Chart of Accounts', async () => {
    const response = await api.classification.initializeChartOfAccounts(selectedCompany.id);
    return response;
  }), [handleOperation, api, selectedCompany.id]);

  const initializeRulesAction = useCallback(() => handleOperation('Initialize Mapping Rules', async () => {
    const response = await api.classification.initializeTransactionMappingRules(selectedCompany.id);
    return response;
  }), [handleOperation, api, selectedCompany.id]);

  const fullInitializationAction = useCallback(() => handleOperation('Full Initialization', async () => {
    const response = await api.classification.performFullInitialization(selectedCompany.id);
    return response;
  }), [handleOperation, api, selectedCompany.id]);

  const autoClassifyAction = useCallback(() => handleOperation('Auto-Classify', async () => {
    const response = await api.classification.autoClassifyTransactions(selectedCompany.id);
    return response;
  }), [handleOperation, api, selectedCompany.id]);

  const syncEntriesAction = useCallback(() => handleOperation('Sync Journal Entries', async () => {
    const response = await api.classification.syncJournalEntries(selectedCompany.id);
    return response;
  }), [handleOperation, api, selectedCompany.id]);

  const regenerateEntriesAction = useCallback(() => handleOperation('Regenerate Journal Entries', async () => {
    const response = await api.classification.regenerateAllJournalEntries(selectedCompany.id);
    return response;
  }), [handleOperation, api, selectedCompany.id]);

  const viewSummaryAction = useCallback(() => handleOperation('View Summary', async () => {
    const response = await api.classification.getClassificationSummary(selectedCompany.id);
    return response;
  }), [handleOperation, api, selectedCompany.id]);

  const viewUnclassifiedAction = useCallback(() => handleOperation('View Unclassified', async () => {
    const response = await api.classification.getUncategorizedTransactions(selectedCompany.id);
    return response;
  }), [handleOperation, api, selectedCompany.id]);

  const classificationMenuItems: MenuItem[] = useMemo(() => [
    {
      id: 'initialize-accounts',
      name: 'Initialize Chart of Accounts',
      description: 'Set up standard accounting chart of accounts',
      icon: Settings,
      action: initializeAccountsAction
    },
    {
      id: 'initialize-rules',
      name: 'Initialize Mapping Rules',
      description: 'Set up transaction classification rules',
      icon: Settings,
      action: initializeRulesAction
    },
    {
      id: 'full-initialization',
      name: 'Full Initialization',
      description: 'Initialize both accounts and rules',
      icon: Settings,
      action: fullInitializationAction
    },
    {
      id: 'auto-classify',
      name: 'Auto-Classify Transactions',
      description: 'Automatically classify unclassified transactions',
      icon: Database,
      action: autoClassifyAction
    },
    {
      id: 'sync-entries',
      name: 'Sync Journal Entries',
      description: 'Create journal entries for classified transactions',
      icon: FileCheck,
      action: syncEntriesAction
    },
    {
      id: 'regenerate-entries',
      name: 'Regenerate All Journal Entries',
      description: 'Recreate all journal entries after reclassification',
      icon: RotateCcw,
      action: regenerateEntriesAction
    },
    {
      id: 'view-summary',
      name: 'View Classification Summary',
      description: 'View current classification status',
      icon: FileText,
      action: viewSummaryAction
    },
    {
      id: 'view-unclassified',
      name: 'View Unclassified Transactions',
      description: 'List transactions that need classification',
      icon: AlertTriangle,
      action: viewUnclassifiedAction
    }
  ], [initializeAccountsAction, initializeRulesAction, fullInitializationAction, autoClassifyAction, syncEntriesAction, regenerateEntriesAction, viewSummaryAction, viewUnclassifiedAction]);

  // Memoize operations menu item actions to prevent infinite re-renders
  const createInvoiceAction = useCallback(() => handleOperation('Create Invoice', async () => {
    // TODO: Implement invoice creation form
    return 'Invoice creation not yet implemented';
  }), [handleOperation]);

  const generateInvoicePdfAction = useCallback(() => handleOperation('Generate Invoice PDF', async () => {
    // TODO: Implement PDF generation
    return 'PDF generation not yet implemented';
  }), [handleOperation]);

  const syncInvoiceJournalEntriesAction = useCallback(() => handleOperation('Sync Invoice Journal Entries', async () => {
    const response = await api.dataManagement.syncInvoiceJournalEntries(selectedCompany.id);
    return response;
  }), [handleOperation, api, selectedCompany.id]);

  const createJournalEntryAction = useCallback(() => handleOperation('Create Journal Entry', async () => {
    // TODO: Implement journal entry creation form
    return 'Journal entry creation not yet implemented';
  }), [handleOperation]);

  const correctCategoriesAction = useCallback(() => handleOperation('Correct Categories', async () => {
    // TODO: Implement category correction
    return 'Category correction not yet implemented';
  }), [handleOperation]);

  const viewHistoryAction = useCallback(() => handleOperation('View History', async () => {
    // TODO: Implement history view
    return 'History view not yet implemented';
  }), [handleOperation]);

  const resetDataAction = useCallback(() => handleOperation('Reset Data', async () => {
    if (!confirm('Are you sure you want to reset all transaction data? This cannot be undone.')) {
      throw new Error('Operation cancelled');
    }
    const response = await api.dataManagement.resetCompanyData(selectedCompany.id, true);
    return response;
  }), [handleOperation, api, selectedCompany.id]);

  const exportCsvAction = useCallback(() => handleOperation('Export CSV', async () => {
    // TODO: Implement CSV export
    return 'CSV export not yet implemented';
  }), [handleOperation]);

  const operationsMenuItems: MenuItem[] = useMemo(() => [
    {
      id: 'create-invoice',
      name: 'Create Manual Invoice',
      description: 'Create a new invoice manually',
      icon: Receipt,
      action: createInvoiceAction
    },
    {
      id: 'generate-invoice-pdf',
      name: 'Generate Invoice PDF',
      description: 'Generate PDF for existing invoices',
      icon: FileText,
      action: generateInvoicePdfAction
    },
    {
      id: 'sync-invoice-journal-entries',
      name: 'Sync Invoice Journal Entries',
      description: 'Create journal entries for invoices',
      icon: FileCheck,
      action: syncInvoiceJournalEntriesAction
    },
    {
      id: 'create-journal-entry',
      name: 'Create Manual Journal Entry',
      description: 'Create a journal entry manually',
      icon: BookOpen,
      action: createJournalEntryAction
    },
    {
      id: 'correct-categories',
      name: 'Correct Transaction Categories',
      description: 'Fix incorrectly categorized transactions',
      icon: AlertTriangle,
      action: correctCategoriesAction
    },
    {
      id: 'view-history',
      name: 'View Transaction History',
      description: 'View transaction correction history',
      icon: History,
      action: viewHistoryAction
    },
    {
      id: 'reset-data',
      name: 'Reset Company Data',
      description: 'Reset transaction data for this company',
      icon: RotateCcw,
      action: resetDataAction
    },
    {
      id: 'export-csv',
      name: 'Export to CSV',
      description: 'Export transaction data to CSV file',
      icon: Download,
      action: exportCsvAction
    }
  ], [createInvoiceAction, generateInvoicePdfAction, syncInvoiceJournalEntriesAction, createJournalEntryAction, correctCategoriesAction, viewHistoryAction, resetDataAction, exportCsvAction]);

  const loadTransactions = useCallback(async () => {
    if (!selectedPeriod) return;

    try {
      setIsLoading(true);
      setError(null);
      setApiMessage(null);
      const result = await api.transactions.getTransactions(Number(selectedCompany.id), selectedPeriod.id);
      const mappedTransactions: Transaction[] = result.data.map((apiTransaction: ApiTransaction) => ({
        id: apiTransaction.id,
        company_id: apiTransaction.companyId,
        fiscal_period_id: apiTransaction.fiscalPeriodId,
        date: apiTransaction.transactionDate,
        description: apiTransaction.description || '',
        amount: apiTransaction.debitAmount > 0 ? apiTransaction.debitAmount : (apiTransaction.creditAmount || 0),
        type: apiTransaction.debitAmount > 0 ? 'debit' : 'credit',
        category: apiTransaction.category || '',
        reference: apiTransaction.reference || '',
        balance: apiTransaction.balance,
        created_at: apiTransaction.createdAt,
        // Map double-entry classification fields from API response
        debit_account_id: apiTransaction.debitAccountId,
        credit_account_id: apiTransaction.creditAccountId,
        debit_account_name: apiTransaction.debitAccountName,
        credit_account_name: apiTransaction.creditAccountName,
        debit_account_code: apiTransaction.debitAccountCode,
        credit_account_code: apiTransaction.creditAccountCode,
      }));
      setTransactions(mappedTransactions.map(t => ({ ...t, isEditing: false })));
      setCurrentPage(1); // Reset to first page when loading new data
      // Show any API note/message returned by backend
      if (result && typeof result === 'object' && 'note' in result && typeof result.note === 'string') {
        setApiMessage(result.note);
      }
    } catch (err) {
      // Extract error message where possible
      let message = 'Failed to load transactions';
      try {
        const anyErr: unknown = err;
        if (anyErr && typeof anyErr === 'object' && 'response' in anyErr) {
          const axiosErr = anyErr as { response?: { data?: { message?: string } } };
          if (axiosErr.response?.data?.message) {
            message = axiosErr.response.data.message;
          }
        } else if (anyErr && typeof anyErr === 'object' && 'message' in anyErr && typeof anyErr.message === 'string') {
          message = anyErr.message;
        }
      } catch {
        // ignore parsing error
      }
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, [api, selectedCompany.id, selectedPeriod]);

  const loadFiscalPeriods = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const result = await api.fiscalPeriods.getFiscalPeriods(selectedCompany.id);
      setFiscalPeriods(result || []);
      if (result && result.length > 0 && !selectedPeriod) {
        setSelectedPeriod(result[0]);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load fiscal periods');
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
      
      // If both debit and credit accounts are selected, update classification
      if (editingTransaction.debit_account_id && editingTransaction.credit_account_id) {
        const response = await api.classification.updateTransactionClassification(
          selectedCompany.id,
          editingTransaction.id,
          editingTransaction.debit_account_id,
          editingTransaction.credit_account_id
        );

        if (!response.success) {
          throw new Error(response.message || 'Failed to update transaction classification');
        }

        setSuccess('Transaction classification updated successfully');
      } else {
        // TODO: Implement general transaction update API call for non-classification fields
        setSuccess('Transaction updated successfully (classification pending)');
      }

      // Update local state
      setTransactions(prev =>
        prev.map(t =>
          t.id === editingTransaction.id
            ? { ...editingTransaction, isEditing: false }
            : t
        )
      );

      setEditingTransaction(null);
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
            name="fiscal-period-select"
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
      <ApiMessageBanner message={apiMessage} type="info" />
      <ApiMessageBanner message={error} type="error" />
      <ApiMessageBanner message={success} type="success" />

      {/* Tab Navigation */}
      <div className="tab-navigation">
        <button
          className={`tab-button ${activeTab === 'manual-entry' ? 'active' : ''}`}
          onClick={() => setActiveTab('manual-entry')}
        >
          <Database size={20} />
          Manual Entry
        </button>
        <button
          className={`tab-button ${activeTab === 'classification' ? 'active' : ''}`}
          onClick={() => setActiveTab('classification')}
        >
          <Settings size={20} />
          Transaction Classification
        </button>
        <button
          className={`tab-button ${activeTab === 'operations' ? 'active' : ''}`}
          onClick={() => setActiveTab('operations')}
        >
          <FileCheck size={20} />
          Operations
        </button>
      </div>

      {/* Tab Content */}
      {activeTab === 'classification' && (
        <div className="tab-content">
          <div className="section-header">
            <h3>Transaction Classification</h3>
            <p>Manage transaction classification and journal entry creation</p>
          </div>

          <div className="menu-grid">
            {classificationMenuItems.map((item) => (
              <button
                key={item.id}
                onClick={item.action}
                disabled={item.disabled || isProcessing}
                className="menu-item"
              >
                <div className="menu-item-content">
                  <item.icon className="menu-icon" />
                  <div className="menu-text">
                    <h4>{item.name}</h4>
                    <p>{item.description}</p>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'operations' && (
        <div className="tab-content">
          <div className="section-header">
            <h3>Data Operations</h3>
            <p>Manage invoices, journal entries, and data operations</p>
          </div>

          <div className="menu-grid">
            {operationsMenuItems.map((item) => (
              <button
                key={item.id}
                onClick={item.action}
                disabled={item.disabled || isProcessing}
                className="menu-item"
              >
                <div className="menu-item-content">
                  <item.icon className="menu-icon" />
                  <div className="menu-text">
                    <h4>{item.name}</h4>
                    <p>{item.description}</p>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'manual-entry' && (
        <div className="tab-content">
          <div className="section-header">
            <h3>Manual Transaction Entry</h3>
            <p>Edit and manage individual transactions manually</p>
          </div>

          {/* Fiscal Period Selector */}
          <div className="fiscal-period-selector">
            <label htmlFor="fiscal-period-select">
              <Calendar size={20} />
              Select Fiscal Period:
            </label>
            <select
              id="fiscal-period-select"
              name="fiscal-period-select"
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
                  <th>Debit Account</th>
                  <th>Credit Account</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {(() => {
                  // Pagination logic
                  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
                  const endIndex = startIndex + ITEMS_PER_PAGE;
                  const paginatedTransactions = transactions.slice(startIndex, endIndex);
                  return paginatedTransactions;
                })().map((transaction) => (
                  <tr key={transaction.id}>
                    <td>
                      {transaction.isEditing ? (
                        <input
                          type="date"
                          id={`transaction-date-${transaction.id}`}
                          name={`transaction-date-${transaction.id}`}
                          value={editingTransaction?.date || transaction.date}
                          onChange={(e) => updateEditingTransaction('date', e.target.value)}
                          autoComplete="off"
                        />
                      ) : (
                        new Date(transaction.date).toLocaleDateString()
                      )}
                    </td>
                    <td>
                      {transaction.isEditing ? (
                        <input
                          type="text"
                          id={`transaction-description-${transaction.id}`}
                          name={`transaction-description-${transaction.id}`}
                          value={editingTransaction?.description || transaction.description}
                          onChange={(e) => updateEditingTransaction('description', e.target.value)}
                          autoComplete="off"
                        />
                      ) : (
                        transaction.description
                      )}
                    </td>
                    <td>
                      {transaction.isEditing ? (
                        <input
                          type="number"
                          id={`transaction-amount-${transaction.id}`}
                          name={`transaction-amount-${transaction.id}`}
                          step="0.01"
                          value={editingTransaction?.amount || transaction.amount || 0}
                          onChange={(e) => updateEditingTransaction('amount', parseFloat(e.target.value))}
                          autoComplete="off"
                        />
                      ) : (
                        `R ${(transaction.amount || 0).toFixed(2)}`
                      )}
                    </td>
                    <td>
                      {transaction.isEditing ? (
                        <select
                          id={`transaction-type-${transaction.id}`}
                          name={`transaction-type-${transaction.id}`}
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
                        <AccountSelector
                          companyId={selectedCompany.id}
                          value={editingTransaction?.debit_account_id || transaction.debit_account_id || null}
                          onChange={(accountId, account) => {
                            if (!editingTransaction) return;
                            setEditingTransaction(prev => prev ? {
                              ...prev,
                              debit_account_id: accountId,
                              debit_account_code: account?.code || account?.accountCode || null,
                              debit_account_name: account?.name || account?.accountName || null
                            } : null);
                          }}
                          placeholder="Select debit account..."
                          id={`transaction-debit-account-${transaction.id}`}
                          name={`transaction-debit-account-${transaction.id}`}
                        />
                      ) : (
                        <div className="account-cell">
                          {transaction.debit_account_code && (
                            <span className="account-code">{transaction.debit_account_code}</span>
                          )}
                          {transaction.debit_account_name && (
                            <span className="account-name">{transaction.debit_account_name}</span>
                          )}
                          {!transaction.debit_account_code && !transaction.debit_account_name && (
                            <span className="text-muted">Not classified</span>
                          )}
                        </div>
                      )}
                    </td>
                    <td>
                      {transaction.isEditing ? (
                        <AccountSelector
                          companyId={selectedCompany.id}
                          value={editingTransaction?.credit_account_id || transaction.credit_account_id || null}
                          onChange={(accountId, account) => {
                            if (!editingTransaction) return;
                            setEditingTransaction(prev => prev ? {
                              ...prev,
                              credit_account_id: accountId,
                              credit_account_code: account?.code || account?.accountCode || null,
                              credit_account_name: account?.name || account?.accountName || null
                            } : null);
                          }}
                          placeholder="Select credit account..."
                          id={`transaction-credit-account-${transaction.id}`}
                          name={`transaction-credit-account-${transaction.id}`}
                        />
                      ) : (
                        <div className="account-cell">
                          {transaction.credit_account_code && (
                            <span className="account-code">{transaction.credit_account_code}</span>
                          )}
                          {transaction.credit_account_name && (
                            <span className="account-name">{transaction.credit_account_name}</span>
                          )}
                          {!transaction.credit_account_code && !transaction.credit_account_name && (
                            <span className="text-muted">Not classified</span>
                          )}
                        </div>
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

            {/* Pagination Controls */}
            {(() => {
              const totalPages = Math.ceil(transactions.length / ITEMS_PER_PAGE);
              return totalPages > 1 && (
                <div className="pagination-controls">
                  <button
                    className="pagination-btn"
                    onClick={() => setCurrentPage(prev => Math.max(1, prev - 1))}
                    disabled={currentPage === 1}
                  >
                    <ChevronLeft size={16} />
                    Previous
                  </button>

                  <span className="pagination-info">
                    Page {currentPage} of {totalPages} ({transactions.length} transactions)
                  </span>

                  <button
                    className="pagination-btn"
                    onClick={() => setCurrentPage(prev => Math.min(totalPages, prev + 1))}
                    disabled={currentPage === totalPages}
                  >
                    Next
                    <ChevronRight size={16} />
                  </button>
                </div>
              );
            })()}

            {transactions.length === 0 && (
              <div className="empty-state">
                <Database size={48} />
                <h3>No transactions found</h3>
                <p>{apiMessage || 'There are no transactions to manage for this company.'}</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Operation Results */}
      {operationResult && (
        <div className={`operation-result ${operationResult.success ? 'success' : 'error'}`}>
          <div className="result-header">
            {operationResult.success ? (
              <CheckCircle size={20} />
            ) : (
              <AlertCircle size={20} />
            )}
            <span>{operationResult.success ? 'Success' : 'Error'}</span>
          </div>
          <pre className="result-message">{operationResult.message}</pre>
        </div>
      )}

      {/* Processing Indicator */}
      {isProcessing && (
        <div className="processing-indicator">
          <Loader className="animate-spin" size={24} />
          <span>Processing {currentOperation}...</span>
        </div>
      )}
    </div>
  );
}