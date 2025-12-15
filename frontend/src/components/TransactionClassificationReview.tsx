import { useState, useEffect, useCallback } from 'react';
import { CheckCircle, AlertTriangle, Edit, X, Plus, Search, HelpCircle, Info } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import type { Company, Transaction, FiscalPeriod, Account } from '../types/api';

interface TransactionClassificationReviewProps {
  selectedCompany: Company;
  selectedPeriod: FiscalPeriod | null;
}

interface UnclassifiedTransaction extends Transaction {
  suggestedClassification?: {
    accountCode: string;
    accountName: string;
    confidence: number;
  };
}

interface ClassificationRule {
  id?: number;
  ruleName: string;
  matchType: 'CONTAINS' | 'STARTS_WITH' | 'ENDS_WITH' | 'EQUALS' | 'REGEX';
  matchValue: string;
  accountCode: string;
  accountName: string;
  priority: number;
}

export default function TransactionClassificationReview({
  selectedCompany,
  selectedPeriod
}: TransactionClassificationReviewProps) {
  const api = useApi();
  const [unclassifiedTransactions, setUnclassifiedTransactions] = useState<UnclassifiedTransaction[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingAccounts, setIsLoadingAccounts] = useState(false);
  const [selectedTransaction, setSelectedTransaction] = useState<UnclassifiedTransaction | null>(null);
  const [showRuleCreator, setShowRuleCreator] = useState(false);
  const [newRule, setNewRule] = useState<ClassificationRule>({
    ruleName: '',
    matchType: 'CONTAINS',
    matchValue: '',
    accountCode: '',
    accountName: '',
    priority: 50
  });
  const [searchTerm, setSearchTerm] = useState('');
  const [filterBy, setFilterBy] = useState<'all' | 'high-confidence' | 'low-confidence'>('all');

  // Load accounts for the company
  const loadAccounts = useCallback(async () => {
    if (!selectedCompany?.id) return;

    try {
      setIsLoadingAccounts(true);
      const response = await api.accounts.getChartOfAccounts(selectedCompany.id);
      setAccounts(response.data || []);
    } catch (error) {
      console.error('Failed to load accounts:', error);
      setAccounts([]);
    } finally {
      setIsLoadingAccounts(false);
    }
  }, [api, selectedCompany?.id]);

  // Load unclassified transactions
  const loadUnclassifiedTransactions = useCallback(async () => {
    if (!selectedPeriod) return;

    setIsLoading(true);
    try {
      // This would need a new API endpoint to get unclassified transactions
      const response = await api.classification.getUnclassifiedTransactions(selectedCompany.id, selectedPeriod.id);
      setUnclassifiedTransactions((response.data as UnclassifiedTransaction[]) || []);
    } catch (error) {
      console.error('Failed to load unclassified transactions:', error);
    } finally {
      setIsLoading(false);
    }
  }, [api, selectedCompany.id, selectedPeriod]);

  useEffect(() => {
    loadAccounts();
    loadUnclassifiedTransactions();
  }, [loadAccounts, loadUnclassifiedTransactions]);

  // Filter transactions based on search and filter criteria
  const filteredTransactions = unclassifiedTransactions.filter(transaction => {
    const matchesSearch = searchTerm === '' ||
      transaction.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
      transaction.amount.toString().includes(searchTerm);

    const matchesFilter = filterBy === 'all' ||
      (filterBy === 'high-confidence' && transaction.suggestedClassification && transaction.suggestedClassification.confidence >= 0.7) ||
      (filterBy === 'low-confidence' && (!transaction.suggestedClassification || transaction.suggestedClassification.confidence < 0.7));

    return matchesSearch && matchesFilter;
  });

  // Handle manual classification
  const handleManualClassification = async (transaction: UnclassifiedTransaction, accountCode: string) => {
    try {
      await api.classification.classifyTransaction(transaction.id, accountCode);
      // Remove from unclassified list
      setUnclassifiedTransactions(prev => prev.filter(t => t.id !== transaction.id));
      setSelectedTransaction(null);
    } catch (error) {
      console.error('Failed to classify transaction:', error);
    }
  };

  // Create new classification rule
  const handleCreateRule = async () => {
    if (!newRule.ruleName || !newRule.matchValue || !newRule.accountCode) return;

    try {
      await api.classification.createClassificationRule(selectedCompany.id, newRule);
      setShowRuleCreator(false);
      setNewRule({
        ruleName: '',
        matchType: 'CONTAINS',
        matchValue: '',
        accountCode: '',
        accountName: '',
        priority: 50
      });
      // Optionally re-run auto-classification
      await api.classification.autoClassifyTransactions(selectedCompany.id);
      loadUnclassifiedTransactions();
    } catch (error) {
      console.error('Failed to create classification rule:', error);
    }
  };

  // Generate rule suggestion from transaction
  const generateRuleFromTransaction = (transaction: UnclassifiedTransaction) => {
    const description = transaction.description;
    // Simple rule generation - look for common patterns
    let matchValue = description;
    const matchType: ClassificationRule['matchType'] = 'CONTAINS';

    // Try to find meaningful patterns
    if (description.toUpperCase().includes('SALARY')) {
      matchValue = 'SALARY';
    } else if (description.toUpperCase().includes('RENT')) {
      matchValue = 'RENT';
    } else if (description.toUpperCase().includes('ELECTRICITY') || description.toUpperCase().includes('ESKOM')) {
      matchValue = 'ELECTRICITY';
    }
    // Add more patterns as needed

    setNewRule({
      ruleName: `Rule for: ${description.substring(0, 30)}...`,
      matchType,
      matchValue,
      accountCode: '',
      accountName: '',
      priority: 60 // Higher than default rules
    });
    setShowRuleCreator(true);
  };

  if (!selectedPeriod) {
    return (
      <div className="classification-review">
        <div className="empty-state">
          <AlertTriangle size={48} />
          <h3>Select Fiscal Period</h3>
          <p>Please select a fiscal period to review transaction classifications.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="classification-review-container">
      <div className="classification-header">
        <h2>Transaction Classification Review</h2>
        <div className="classification-stats">
          <div className="stat-item">
            <span className="stat-label">Unclassified:</span>
            <span className="stat-value">{unclassifiedTransactions.length}</span>
          </div>
          <div className="stat-item">
            <span className="stat-label">Filtered:</span>
            <span className="stat-value">{filteredTransactions.length}</span>
          </div>
          <div className="stat-item">
            <span className="stat-label">High Confidence:</span>
            <span className="stat-value">
              {unclassifiedTransactions.filter(t => t.suggestedClassification && t.suggestedClassification.confidence >= 0.7).length}
            </span>
          </div>
        </div>
      </div>

      <div className="classification-filters">
        <div className="filter-group">
          <label>Search:</label>
          <div style={{ position: 'relative' }}>
            <Search size={16} style={{ position: 'absolute', left: '0.5rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-secondary)' }} />
            <input
              type="text"
              placeholder="Search transactions..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{ paddingLeft: '2rem' }}
            />
          </div>
        </div>
        <div className="filter-group">
          <label>Filter:</label>
          <select
            value={filterBy}
            onChange={(e) => setFilterBy(e.target.value as typeof filterBy)}
          >
            <option value="all">All Transactions</option>
            <option value="high-confidence">High Confidence Suggestions</option>
            <option value="low-confidence">Low Confidence / No Suggestions</option>
          </select>
        </div>
        <button
          className="action-button primary"
          onClick={() => setShowRuleCreator(true)}
        >
          <Plus size={16} />
          Create Rule
        </button>
      </div>

      {/* Rule Creator Modal */}
      {showRuleCreator && (
        <div className="rule-creation-modal">
          <div className="rule-modal-content">
            <div className="rule-modal-header">
              <h3>Create Classification Rule</h3>
              <button className="rule-modal-close" onClick={() => setShowRuleCreator(false)}>
                <X size={20} />
              </button>
            </div>

            {/* Help Section */}
            <div className="rule-help-section">
              <div className="help-item">
                <Info size={16} />
                <div>
                  <strong>How Classification Rules Work:</strong> Rules automatically categorize bank transactions based on text patterns in their descriptions. When a transaction matches your rule, it gets assigned to the selected account.
                </div>
              </div>
              <div className="help-item">
                <HelpCircle size={16} />
                <div>
                  <strong>Examples:</strong>
                  <ul style={{ margin: '0.5rem 0', paddingLeft: '1.2rem' }}>
                    <li><strong>"ATM"</strong> matches any transaction with "ATM" in the description</li>
                    <li><strong>"SALARY"</strong> matches payroll deposits</li>
                    <li><strong>"OFFICE SUPPLIES"</strong> matches expense transactions</li>
                  </ul>
                </div>
              </div>
              <div className="help-item">
                <AlertTriangle size={16} />
                <div>
                  <strong>Tip:</strong> Start with broad patterns and use priority to control which rules apply first. Test your rules on a few transactions before applying them widely.
                </div>
              </div>
            </div>

            <div className="rule-form">
              <div className="form-group">
                <label>Rule Name:</label>
                <input
                  type="text"
                  value={newRule.ruleName}
                  onChange={(e) => setNewRule(prev => ({ ...prev, ruleName: e.target.value }))}
                  placeholder="e.g., ATM Withdrawals, Office Supplies"
                />
                <small className="field-help">Give your rule a descriptive name that explains what it does</small>
              </div>

              <div className="rule-form">
                <div className="form-row">
                  <div className="form-group">
                    <label>Match Type:</label>
                    <select
                      value={newRule.matchType}
                      onChange={(e) => setNewRule(prev => ({ ...prev, matchType: e.target.value as ClassificationRule['matchType'] }))}
                    >
                      <option value="CONTAINS">Contains</option>
                      <option value="STARTS_WITH">Starts With</option>
                      <option value="ENDS_WITH">Ends With</option>
                      <option value="EQUALS">Equals</option>
                      <option value="REGEX">Regular Expression</option>
                    </select>
                    <small className="field-help">How to match the transaction description</small>
                  </div>
                  <div className="form-group">
                    <label>Priority:</label>
                    <input
                      type="number"
                      value={newRule.priority}
                      onChange={(e) => setNewRule(prev => ({ ...prev, priority: parseInt(e.target.value) || 50 }))}
                      min="1"
                      max="100"
                    />
                    <small className="field-help">Higher numbers = higher priority (1-100)</small>
                  </div>
                </div>
              </div>

              <div className="form-group">
                <label>Match Value:</label>
                <input
                  type="text"
                  value={newRule.matchValue}
                  onChange={(e) => setNewRule(prev => ({ ...prev, matchValue: e.target.value }))}
                  placeholder="e.g., ATM, OFFICE SUPPLIES, INTERNET"
                />
                <small className="field-help">Text to search for in transaction descriptions</small>
              </div>

              <div className="form-group">
                <label>Account to Assign:</label>
                {isLoadingAccounts ? (
                  <div className="loading-spinner small"></div>
                ) : (
                  <select
                    value={`${newRule.accountCode}|${newRule.accountName}`}
                    onChange={(e) => {
                      const [code, name] = e.target.value.split('|');
                      setNewRule(prev => ({
                        ...prev,
                        accountCode: code,
                        accountName: name
                      }));
                    }}
                    className="account-select"
                  >
                    <option value="">Select an account...</option>
                    {accounts
                      .filter(account => account.isActive)
                      .sort((a, b) => a.code.localeCompare(b.code))
                      .map(account => (
                        <option key={account.id} value={`${account.code}|${account.name}`}>
                          {account.code} - {account.name}
                        </option>
                      ))}
                  </select>
                )}
                {newRule.accountCode && (
                  <div className="selected-account-info">
                    <small className="field-help">
                      Selected: <strong>{newRule.accountCode} - {newRule.accountName}</strong>
                    </small>
                  </div>
                )}
                <small className="field-help">Choose which account transactions matching this rule should be assigned to</small>
              </div>
            </div>

            <div className="rule-modal-actions">
              <button className="cancel-button" onClick={() => setShowRuleCreator(false)}>Cancel</button>
              <button
                className="create-button"
                onClick={handleCreateRule}
                disabled={!newRule.ruleName || !newRule.matchValue || !newRule.accountCode || isLoadingAccounts}
              >
                Create Rule
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Transaction List */}
      <div className="transactions-grid">
        {isLoading ? (
          <div className="loading-state">
            <div className="loading-spinner"></div>
            <p>Loading unclassified transactions...</p>
          </div>
        ) : filteredTransactions.length === 0 ? (
          <div className="empty-state">
            <CheckCircle size={48} />
            <h3>All Transactions Classified!</h3>
            <p>No unclassified transactions found for the selected period.</p>
          </div>
        ) : (
          filteredTransactions.map((transaction) => (
            <div key={transaction.id} className="transaction-card">
              <div className="transaction-header">
                <div>
                  <div className="transaction-description">{transaction.description}</div>
                  <div className="transaction-meta">
                    <span className={`transaction-amount ${transaction.amount < 0 ? 'negative' : ''}`}>
                      R {Math.abs(transaction.amount).toFixed(2)}
                    </span>
                    <span className="transaction-date">
                      {new Date(transaction.date).toLocaleDateString()}
                    </span>
                    <span className="transaction-type">{transaction.type.toUpperCase()}</span>
                  </div>
                </div>
                {transaction.suggestedClassification && (
                  <div className="transaction-confidence">
                    <span className={`transaction-confidence ${transaction.suggestedClassification.confidence >= 0.7 ? 'high' : transaction.suggestedClassification.confidence >= 0.4 ? 'medium' : 'low'}`}>
                      {Math.round(transaction.suggestedClassification.confidence * 100)}%
                    </span>
                  </div>
                )}
              </div>

              {transaction.suggestedClassification && (
                <div className="transaction-suggestion">
                  <div className="suggestion-header">
                    <span className="suggestion-label">AI Suggestion</span>
                    <span className="suggestion-confidence">
                      {Math.round(transaction.suggestedClassification.confidence * 100)}% confidence
                    </span>
                  </div>
                  <div className="suggestion-account">
                    {transaction.suggestedClassification.accountCode} - {transaction.suggestedClassification.accountName}
                  </div>
                </div>
              )}

              <div className="transaction-actions">
                {transaction.suggestedClassification ? (
                  <>
                    <button
                      className="action-button success"
                      onClick={() => handleManualClassification(transaction, transaction.suggestedClassification!.accountCode)}
                    >
                      <CheckCircle size={16} />
                      Accept Suggestion
                    </button>
                    <button
                      className="action-button secondary"
                      onClick={() => setSelectedTransaction(transaction)}
                    >
                      <Edit size={16} />
                      Manual Classify
                    </button>
                  </>
                ) : (
                  <button
                    className="action-button primary"
                    onClick={() => setSelectedTransaction(transaction)}
                  >
                    <Edit size={16} />
                    Classify Manually
                  </button>
                )}
                <button
                  className="action-button warning"
                  onClick={() => generateRuleFromTransaction(transaction)}
                >
                  <Plus size={16} />
                  Create Rule
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Manual Classification Modal */}
      {selectedTransaction && (
        <div className="rule-creation-modal">
          <div className="rule-modal-content">
            <div className="rule-modal-header">
              <h3>Classify Transaction</h3>
              <button className="rule-modal-close" onClick={() => setSelectedTransaction(null)}>
                <X size={20} />
              </button>
            </div>
            <div className="manual-classification">
              <div className="transaction-details">
                <div className="transaction-description">{selectedTransaction.description}</div>
                <div className="transaction-meta">
                  <span className="transaction-date">Date: {new Date(selectedTransaction.date).toLocaleDateString()}</span>
                  <span className={`transaction-amount ${selectedTransaction.amount < 0 ? 'negative' : ''}`}>
                    Amount: R {Math.abs(selectedTransaction.amount).toFixed(2)}
                  </span>
                  <span className="transaction-type">Type: {selectedTransaction.type.toUpperCase()}</span>
                </div>
              </div>

              <div className="classification-form">
                <div className="form-group">
                  <label>Account Code:</label>
                  <input
                    type="text"
                    placeholder="e.g., 4100, 3110"
                    id="manual-account-code"
                  />
                </div>
                <div className="form-group">
                  <label>Account Name (Optional):</label>
                  <input
                    type="text"
                    placeholder="e.g., Office Supplies, Rent Expense"
                    id="manual-account-name"
                  />
                </div>
              </div>

              <div className="form-actions">
                <button
                  className="action-button secondary"
                  onClick={() => setSelectedTransaction(null)}
                >
                  Cancel
                </button>
                <button
                  className="action-button primary"
                  onClick={() => {
                    const accountCode = (document.getElementById('manual-account-code') as HTMLInputElement)?.value;
                    if (accountCode) {
                      handleManualClassification(selectedTransaction, accountCode);
                    }
                  }}
                >
                  Classify Transaction
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}