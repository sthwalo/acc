import { useState, useEffect, useCallback } from 'react';
import { Receipt, ArrowUpCircle, ArrowDownCircle, Search, Filter } from 'lucide-react';
import { serviceRegistry } from '../services/ServiceRegistry';
import { ApiService } from '../services/ApiService';
import type { Transaction, ApiTransaction, Company, FiscalPeriod } from '../types/api';

interface TransactionsViewProps {
  selectedCompany: Company;
  selectedFiscalPeriod?: FiscalPeriod;
}

export default function TransactionsView({ selectedCompany, selectedFiscalPeriod }: TransactionsViewProps) {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<'all' | 'debit' | 'credit'>('all');

  const loadTransactions = useCallback(async () => {
    if (!selectedFiscalPeriod) return;

    try {
      setLoading(true);
      const apiService = serviceRegistry.get<ApiService>('apiService');
      const response: { data: ApiTransaction[], count: number, company_id: number, timestamp: number, note: string } = await apiService.getTransactions(Number(selectedCompany.id), Number(selectedFiscalPeriod.id));
      
      // Map API response to expected Transaction format
      const apiTransactions = response.data as ApiTransaction[];
      const mappedTransactions: Transaction[] = apiTransactions.map((apiTransaction) => ({
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
      
      setTransactions(mappedTransactions);
      setError(null);
    } catch (err) {
      setError('Failed to load transactions');
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

  const filteredTransactions = transactions.filter(transaction => {
    const matchesSearch = transaction.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         transaction.reference.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         transaction.category.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesType = filterType === 'all' || transaction.type === filterType;

    return matchesSearch && matchesType;
  });

  const totalDebits = filteredTransactions
    .filter(t => t.type === 'debit')
    .reduce((sum, t) => sum + Math.abs(t.amount), 0);

  const totalCredits = filteredTransactions
    .filter(t => t.type === 'credit')
    .reduce((sum, t) => sum + Math.abs(t.amount), 0);

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
      <div className="error">
        <p>{error}</p>
        <button onClick={loadTransactions}>Retry</button>
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
      </div>

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

      <div className="transactions-list">
        {filteredTransactions.map((transaction) => (
          <div key={transaction.id} className={`transaction-item ${transaction.type}`}>
            <div className="transaction-icon">
              {transaction.type === 'debit' ? (
                <ArrowUpCircle size={20} className="debit-icon" />
              ) : (
                <ArrowDownCircle size={20} className="credit-icon" />
              )}
            </div>

            <div className="transaction-details">
              <div className="transaction-header">
                <h4>{transaction.description}</h4>
                <span className="transaction-amount">
                  {transaction.type === 'debit' ? '-' : '+'}{formatCurrency(transaction.amount)}
                </span>
              </div>

              <div className="transaction-meta">
                <span className="date">{formatDate(transaction.date)}</span>
                <span className="category">{transaction.category}</span>
                <span className="reference">{transaction.reference}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {filteredTransactions.length === 0 && (
        <div className="empty-state">
          <Receipt size={48} />
          <h3>No Transactions Found</h3>
          <p>
            {searchTerm || filterType !== 'all'
              ? 'No transactions match your search criteria.'
              : 'No transactions have been recorded for this company yet.'}
          </p>
        </div>
      )}
    </div>
  );
}