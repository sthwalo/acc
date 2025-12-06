import React, { useState, useEffect, useMemo } from 'react';
import { useApi } from '../../hooks/useApi';
import type { Account } from '../../types/api';

interface AccountSelectorProps {
  companyId: number;
  value: number | null | undefined;
  onChange: (accountId: number | null, account: Account | null) => void;
  label?: string;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  id?: string;
  name?: string;
}

/**
 * AccountSelector Component
 * 
 * Displays a searchable dropdown of accounts for transaction classification.
 * Matches legacy console app pattern from DataManagementController.selectAccount()
 * 
 * Features:
 * - Fetches accounts via API: GET /api/v1/companies/{companyId}/accounts
 * - Display format: "[code] name" (e.g., "[8400] Communication")
 * - Searchable/filterable by account code or name
 * - Handles loading and error states
 * 
 * @param companyId - Company ID to fetch accounts for
 * @param value - Selected account ID (controlled component)
 * @param onChange - Callback when account selection changes
 * @param label - Optional label for the selector
 * @param placeholder - Placeholder text when no account selected
 * @param disabled - Disable the selector
 * @param className - Additional CSS classes
 * @param id - HTML id attribute for accessibility
 * @param name - HTML name attribute for forms
 */
const AccountSelector: React.FC<AccountSelectorProps> = ({
  companyId,
  value,
  onChange,
  label,
  placeholder = 'Select account...',
  disabled = false,
  className = '',
  id,
  name
}) => {
  const api = useApi();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Fetch accounts on mount or when companyId changes
  useEffect(() => {
    const fetchAccounts = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await api.accounts.getChartOfAccounts(companyId);
        
        if (response.success && response.data) {
          // Sort accounts by code for easier navigation
          const sortedAccounts = response.data.sort((a, b) => {
            const codeA = a.code || a.accountCode || '';
            const codeB = b.code || b.accountCode || '';
            return codeA.localeCompare(codeB);
          });
          setAccounts(sortedAccounts);
        } else {
          setError('Failed to load accounts');
        }
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to fetch accounts';
        setError(errorMessage);
        console.error('Error fetching accounts:', err);
      } finally {
        setIsLoading(false);
      }
    };

    if (companyId) {
      fetchAccounts();
    }
  }, [companyId, api.accounts]);

  // Filter accounts based on search term
  const filteredAccounts = useMemo(() => {
    if (!searchTerm.trim()) {
      return accounts;
    }

    const term = searchTerm.toLowerCase();
    return accounts.filter(account => {
      const code = (account.code || account.accountCode || '').toLowerCase();
      const name = (account.name || account.accountName || '').toLowerCase();
      return code.includes(term) || name.includes(term);
    });
  }, [accounts, searchTerm]);

  // Format account display text: "[code] name"
  const formatAccountDisplay = (account: Account): string => {
    const code = account.code || account.accountCode || '';
    const name = account.name || account.accountName || 'Unnamed Account';
    return `[${code}] ${name}`;
  };

  // Handle account selection
  const handleSelect = (account: Account) => {
    onChange(account.id, account);
    setSearchTerm('');
  };

  // Handle clear selection
  const handleClear = () => {
    onChange(null, null);
    setSearchTerm('');
  };

  // Render loading state
  if (isLoading) {
    return (
      <div className={`account-selector ${className}`}>
        {label && <label htmlFor={id}>{label}</label>}
        <select disabled className="account-selector-loading">
          <option>Loading accounts...</option>
        </select>
      </div>
    );
  }

  // Render error state
  if (error) {
    return (
      <div className={`account-selector ${className}`}>
        {label && <label htmlFor={id}>{label}</label>}
        <select disabled className="account-selector-error">
          <option>{error}</option>
        </select>
      </div>
    );
  }

  // Render account selector (using native select for simplicity)
  return (
    <div className={`account-selector ${className}`}>
      {label && <label htmlFor={id}>{label}</label>}
      <select
        id={id}
        name={name}
        value={value || ''}
        onChange={(e) => {
          const accountId = parseInt(e.target.value);
          if (accountId) {
            const account = accounts.find(acc => acc.id === accountId);
            if (account) {
              handleSelect(account);
            }
          } else {
            handleClear();
          }
        }}
        disabled={disabled || accounts.length === 0}
        className="account-selector-dropdown"
      >
        <option value="">{placeholder}</option>
        {filteredAccounts.map((account) => (
          <option key={account.id} value={account.id}>
            {formatAccountDisplay(account)}
          </option>
        ))}
      </select>
      {accounts.length === 0 && (
        <p className="account-selector-empty">
          No accounts available. Please initialize chart of accounts.
        </p>
      )}
    </div>
  );
};

export default AccountSelector;
