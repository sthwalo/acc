import { useState, useEffect, useCallback } from 'react';
import { Receipt, ArrowUpCircle, ArrowDownCircle, Search, Filter, Download, FileText, ChevronLeft, ChevronRight } from 'lucide-react';
import { serviceRegistry } from '../services/ServiceRegistry';
import { ApiService } from '../services/ApiService';
import ApiMessageBanner from './shared/ApiMessageBanner';
import type { ApiTransaction, Company, FiscalPeriod } from '../types/api';

interface TransactionsViewProps {
  selectedCompany: Company;
  selectedFiscalPeriod?: FiscalPeriod;
}

const ITEMS_PER_PAGE = 50;

export default function TransactionsView({ selectedCompany, selectedFiscalPeriod }: TransactionsViewProps) {
  const [transactions, setTransactions] = useState<ApiTransaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<'all' | 'debit' | 'credit'>('all');
  const [apiMessage, setApiMessage] = useState<string>('');
  const [currentPage, setCurrentPage] = useState(1);

  const loadTransactions = useCallback(async () => {
    if (!selectedFiscalPeriod) return;

    try {
      setLoading(true);
      const apiService = serviceRegistry.get<ApiService>('apiService');
      const response: { data: ApiTransaction[], count: number, company_id: number, timestamp: number, note: string } = await apiService.getTransactions(Number(selectedCompany.id), Number(selectedFiscalPeriod.id));

      setTransactions(response.data);
      setApiMessage(response.note || '');
      setError(null);
      setCurrentPage(1); // Reset to first page when loading new data
    } catch (err) {
      let message = 'Failed to load transactions';
      try {
        const anyErr: unknown = err;
        if (anyErr && typeof anyErr === 'object' && 'response' in anyErr) {
          const axiosErr = anyErr as { response?: { data?: { message?: string } } };
          if (axiosErr.response?.data?.message) {
            message = axiosErr.response.data.message;
          }
        } else if (err instanceof Error) {
          message = err.message;
        }
      } catch {
        // fallback below
      }
      setError(message);
      console.error('Error loading transactions:', err);
    } finally {
      setLoading(false);
    }
  }, [selectedCompany.id, selectedFiscalPeriod]);

  useEffect(() => {
    loadTransactions();
  }, [loadTransactions]);

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-ZA', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-ZA', {
      style: 'currency',
      currency: 'ZAR'
    }).format(Math.abs(amount));
  };

  const formatDateTime = (dateTimeString: string) => {
    return new Date(dateTimeString).toLocaleString('en-ZA');
  };

  /**
   * Get the main account for classification display.
   * Logic: Show the non-cash/non-bank account
   * - Credit transaction (money IN) → show credit account (revenue/income)
   * - Debit transaction (money OUT) → show debit account (expense)
   */
  const getMainAccountClassification = (transaction: ApiTransaction): string => {
    if (transaction.creditAmount > 0) {
      // Credit transaction → main account is the credit account (revenue/income)
      if (transaction.creditAccountCode && transaction.creditAccountName) {
        return `[${transaction.creditAccountCode}] ${transaction.creditAccountName}`;
      }
    } else if (transaction.debitAmount > 0) {
      // Debit transaction → main account is the debit account (expense)
      if (transaction.debitAccountCode && transaction.debitAccountName) {
        return `[${transaction.debitAccountCode}] ${transaction.debitAccountName}`;
      }
    }
    // Fallback to original category if not classified
    return transaction.category || 'Not classified';
  };

  const filteredTransactions = transactions.filter(transaction => {
    const matchesSearch = (transaction.description || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
                         (transaction.reference || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
                         transaction.category.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         (transaction.accountNumber || '').toLowerCase().includes(searchTerm.toLowerCase());

    const matchesType = filterType === 'all' ||
                       (filterType === 'debit' && transaction.debitAmount > 0) ||
                       (filterType === 'credit' && transaction.creditAmount > 0);

    return matchesSearch && matchesType;
  });

  // Pagination logic
  const totalPages = Math.ceil(filteredTransactions.length / ITEMS_PER_PAGE);
  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
  const endIndex = startIndex + ITEMS_PER_PAGE;
  const paginatedTransactions = filteredTransactions.slice(startIndex, endIndex);

  const totalDebits = filteredTransactions
    .filter(t => t.debitAmount > 0)
    .reduce((sum, t) => sum + t.debitAmount, 0);

  const totalCredits = filteredTransactions
    .filter(t => t.creditAmount > 0)
    .reduce((sum, t) => sum + t.creditAmount, 0);

  const handleExportCsv = async () => {
    if (!selectedFiscalPeriod) return;

    try {
      const apiService = serviceRegistry.get<ApiService>('apiService');
      const blob = await apiService.exportTransactionsToCsv(Number(selectedCompany.id), Number(selectedFiscalPeriod.id));
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `transactions_${selectedCompany.name}_${selectedFiscalPeriod.periodName}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to export transactions to CSV';
      try {
        const anyErr: unknown = error;
        if (anyErr && typeof anyErr === 'object' && 'response' in anyErr) {
          const axiosErr = anyErr as { response?: { data?: { message?: string } } };
          if (axiosErr.response?.data?.message) {
            message = axiosErr.response.data.message;
          }
        } else if (error instanceof Error) {
          message = error.message;
        }
      } catch {
        // ignore parsing error
      }
      setError(message);
      console.error('Export error:', error);
    }
  };

  const handleExportPdf = async () => {
    if (!selectedFiscalPeriod) return;

    try {
      const apiService = serviceRegistry.get<ApiService>('apiService');
      const blob = await apiService.exportTransactionsToPdf(Number(selectedCompany.id), Number(selectedFiscalPeriod.id));
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `transactions_${selectedCompany.name}_${selectedFiscalPeriod.periodName}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to export transactions to PDF';
      try {
        const anyErr: unknown = error;
        if (anyErr && typeof anyErr === 'object' && 'response' in anyErr) {
          const axiosErr = anyErr as { response?: { data?: { message?: string } } };
          if (axiosErr.response?.data?.message) {
            message = axiosErr.response.data.message;
          }
        } else if (error instanceof Error) {
          message = error.message;
        }
      } catch {
        // ignore parsing error
      }
      setError(message);
      console.error('Export error:', error);
    }
  };

  if (loading) {
    return (
      <div className="loading">
        <div className="spinner"></div>
        <p>Loading transactions...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="transactions-view">
        <div className="view-header">
          <h2>Transactions - {selectedCompany.name}</h2>
          {selectedFiscalPeriod && (
            <p>Fiscal Period: {selectedFiscalPeriod.periodName}</p>
          )}
        </div>
        <ApiMessageBanner message={error} type="error" />
        <div className="error">
          <button onClick={loadTransactions}>Retry</button>
        </div>
      </div>
    );
  }

  return (
    <div className="transactions-view">
      <div className="view-header">
        <h2>Transactions - {selectedCompany.name}</h2>
        {selectedFiscalPeriod && (
          <p>Fiscal Period: {selectedFiscalPeriod.periodName}</p>
        )}
        <div className="header-actions">
          <button
            className="action-button export"
            onClick={handleExportCsv}
            disabled={!selectedFiscalPeriod}
            title="Export transactions to CSV"
          >
            <Download size={16} />
            Export CSV
          </button>
          <button
            className="action-button export"
            onClick={handleExportPdf}
            disabled={!selectedFiscalPeriod}
            title="Export transactions to PDF"
          >
            <FileText size={16} />
            Export PDF
          </button>
        </div>
      </div>
      <ApiMessageBanner message={apiMessage} type="info" />
      <ApiMessageBanner message={error} type="error" />

      <div className="transactions-summary">
        <div className="summary-card">
          <ArrowUpCircle size={24} className="debit-icon" />
          <div>
            <div className="summary-label">Total Debits</div>
            <div className="summary-value debit">{formatCurrency(totalDebits)}</div>
          </div>
        </div>

        <div className="summary-card">
          <ArrowDownCircle size={24} className="credit-icon" />
          <div>
            <div className="summary-label">Total Credits</div>
            <div className="summary-value credit">{formatCurrency(totalCredits)}</div>
          </div>
        </div>

        <div className="summary-card">
          <Receipt size={24} className="balance-icon" />
          <div>
            <div className="summary-label">Net Balance</div>
            <div className={`summary-value ${totalCredits - totalDebits >= 0 ? 'positive' : 'negative'}`}>
              {formatCurrency(totalCredits - totalDebits)}
            </div>
          </div>
        </div>
      </div>

      <div className="filters-section">
        <div className="search-box">
          <Search size={20} />
          <input
            type="text"
            placeholder="Search transactions..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>

        <div className="filter-buttons">
          <Filter size={20} />
          <button
            className={`filter-btn ${filterType === 'all' ? 'active' : ''}`}
            onClick={() => setFilterType('all')}
          >
            All
          </button>
          <button
            className={`filter-btn ${filterType === 'debit' ? 'active' : ''}`}
            onClick={() => setFilterType('debit')}
          >
            Debits
          </button>
          <button
            className={`filter-btn ${filterType === 'credit' ? 'active' : ''}`}
            onClick={() => setFilterType('credit')}
          >
            Credits
          </button>
        </div>
      </div>

      <div className="transactions-table-container">
        <table className="transactions-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Date</th>
              <th>Details</th>
              <th>Debit</th>
              <th>Credit</th>
              <th>Balance</th>
              <th>Classification</th>
              <th>Created At</th>
            </tr>
          </thead>
          <tbody>
            {paginatedTransactions.map((transaction) => (
              <tr key={transaction.id}>
                <td>{transaction.id}</td>
                <td>{formatDate(transaction.transactionDate)}</td>
                <td>{transaction.description || ''}</td>
                <td className="debit-cell">{transaction.debitAmount > 0 ? formatCurrency(transaction.debitAmount) : ''}</td>
                <td className="credit-cell">{transaction.creditAmount > 0 ? formatCurrency(transaction.creditAmount) : ''}</td>
                <td>{transaction.balance ? formatCurrency(transaction.balance) : ''}</td>
                <td className="classification-cell">{getMainAccountClassification(transaction)}</td>
                <td>{formatDateTime(transaction.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Pagination Controls */}
        {totalPages > 1 && (
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
              Page {currentPage} of {totalPages} ({filteredTransactions.length} transactions)
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
        )}
      </div>

      {filteredTransactions.length === 0 && (
        <div className="empty-state">
          <Receipt size={48} />
          <h3>No Transactions Found</h3>
          <p>
            {searchTerm || filterType !== 'all'
              ? 'No transactions match your search criteria.'
              : apiMessage || 'No transactions have been recorded for this company yet.'}
          </p>
        </div>
      )}
    </div>
  );
}