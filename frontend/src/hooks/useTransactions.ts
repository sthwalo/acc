import { useCallback, useEffect, useState } from 'react';
import { useApi } from '../hooks/useApi';
import type { FiscalPeriod, Transaction, ApiTransaction } from '../types/api';

export function useTransactions(companyId: number) {
  const api = useApi();
  const [fiscalPeriods, setFiscalPeriods] = useState<FiscalPeriod[]>([]);
  const [selectedPeriod, setSelectedPeriod] = useState<FiscalPeriod | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [apiMessage, setApiMessage] = useState<string | null>(null);

  const loadFiscalPeriods = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const result = await api.fiscalPeriods.getFiscalPeriods(companyId);
      setFiscalPeriods(result || []);
      if (result && result.length > 0 && !selectedPeriod) {
        setSelectedPeriod(result[0]);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load fiscal periods');
    } finally {
      setIsLoading(false);
    }
  }, [api, companyId, selectedPeriod]);

  const loadTransactions = useCallback(async (period?: FiscalPeriod) => {
    const usePeriod = period ?? selectedPeriod;
    if (!usePeriod) return;

    try {
      setIsLoading(true);
      setError(null);
      setApiMessage(null);
      const result = await api.transactions.getTransactions(Number(companyId), usePeriod.id);
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
        debit_account_id: apiTransaction.debitAccountId,
        credit_account_id: apiTransaction.creditAccountId,
        debit_account_name: apiTransaction.debitAccountName,
        credit_account_name: apiTransaction.creditAccountName,
        debit_account_code: apiTransaction.debitAccountCode,
        credit_account_code: apiTransaction.creditAccountCode,
      }));
      setTransactions(mappedTransactions.map(t => ({ ...t, isEditing: false })) as unknown as Transaction[]);
      // Show any API note/message returned by backend
      if (result && typeof result === 'object' && 'note' in result && typeof result.note === 'string') {
        setApiMessage(result.note);
      }
    } catch (err) {
      let message = 'Failed to load transactions';
      try {
        const anyErr: unknown = err;
        if (anyErr && typeof anyErr === 'object' && 'response' in anyErr) {
          const axiosErr = anyErr as { response?: { data?: { message?: string } } };
          if (axiosErr.response?.data?.message) {
            message = axiosErr.response.data.message;
          }
        } else if (anyErr && typeof anyErr === 'object' && 'message' in anyErr && typeof (anyErr as { message?: unknown }).message === 'string') {
          message = (anyErr as { message?: string }).message!;
        }
      } catch {
        // ignore parsing error
      }
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }, [api, companyId, selectedPeriod]);

  useEffect(() => {
    loadFiscalPeriods();
  }, [loadFiscalPeriods]);

  useEffect(() => {
    if (selectedPeriod) loadTransactions();
  }, [selectedPeriod, loadTransactions]);

  return {
    fiscalPeriods,
    selectedPeriod,
    setSelectedPeriod,
    transactions,
    isLoading,
    error,
    apiMessage,
    loadFiscalPeriods,
    loadTransactions
  };
}

export default useTransactions;
