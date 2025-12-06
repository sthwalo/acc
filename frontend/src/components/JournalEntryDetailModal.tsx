import { useState, useEffect, useCallback } from 'react';
import { X, FileText, Calendar, User, Loader, AlertCircle } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
import type { JournalEntryDetailDTO, JournalEntryLineDTO } from '../types/api';

interface JournalEntryDetailModalProps {
  entryId: number;
  onClose: () => void;
}

export default function JournalEntryDetailModal({ entryId, onClose }: JournalEntryDetailModalProps) {
  const api = useApi();
  const [entryDetail, setEntryDetail] = useState<JournalEntryDetailDTO | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchEntryDetail = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await api.reports.getJournalEntryDetail(entryId);
      setEntryDetail(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load journal entry details');
    } finally {
      setIsLoading(false);
    }
  }, [api, entryId]);

  useEffect(() => {
    fetchEntryDetail();
  }, [fetchEntryDetail]);

  const formatCurrency = (amount: number): string => {
    return new Intl.NumberFormat('en-ZA', {
      style: 'currency',
      currency: 'ZAR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount);
  };

  const formatDate = (dateString: string): string => {
    return new Date(dateString).toLocaleDateString('en-ZA', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const formatDateTime = (dateString: string): string => {
    return new Date(dateString).toLocaleString('en-ZA', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // Handle backdrop click
  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  // Handle ESC key press
  useEffect(() => {
    const handleEscKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscKey);
    return () => document.removeEventListener('keydown', handleEscKey);
  }, [onClose]);

  return (
    <div
      className="modal-backdrop"
      onClick={handleBackdropClick}
    >
      <div className="journal-entry-modal">
        {/* Header */}
        <div className="modal-header">
          <div className="modal-header-content">
            <FileText size={24} />
            <div>
              <h2>Journal Entry Details</h2>
              {entryDetail && (
                <p className="modal-subtitle">
                  {entryDetail.reference} - {formatDate(entryDetail.entryDate)}
                </p>
              )}
            </div>
          </div>
          <button
            onClick={onClose}
            className="modal-close-button"
            aria-label="Close modal"
          >
            <X size={24} />
          </button>
        </div>

        {/* Content */}
        <div className="modal-content">
          {/* Error Banner */}
          {error && (
            <div className="modal-error">
              <ApiMessageBanner message={error} type="error" />
            </div>
          )}

          {/* Loading State */}
          {isLoading && (
            <div className="loading-state">
              <Loader className="spinner-icon" size={32} />
              <span>Loading journal entry details...</span>
            </div>
          )}

          {/* Entry Details */}
          {!isLoading && entryDetail && (
            <>
              {/* Summary Section */}
              <div className="entry-summary">
                <div className="summary-grid">
                  <div className="summary-field">
                    <label>Company</label>
                    <p>{entryDetail.companyName}</p>
                  </div>
                  <div className="summary-field">
                    <label>Fiscal Period</label>
                    <p>{entryDetail.fiscalPeriodName}</p>
                  </div>
                  <div className="summary-field">
                    <label>Reference</label>
                    <p>{entryDetail.reference}</p>
                  </div>
                  <div className="summary-field">
                    <label>Entry Date</label>
                    <div className="field-with-icon">
                      <Calendar size={16} />
                      <p>{formatDate(entryDetail.entryDate)}</p>
                    </div>
                  </div>
                  <div className="summary-field full-width">
                    <label>Description</label>
                    <p>{entryDetail.description}</p>
                  </div>
                </div>
              </div>

              {/* Totals Section */}
              <div className="totals-section">
                <div className="total-card debit-card">
                  <label>Total Debit</label>
                  <p>{formatCurrency(entryDetail.totalDebit)}</p>
                </div>
                <div className="total-card credit-card">
                  <label>Total Credit</label>
                  <p>{formatCurrency(entryDetail.totalCredit)}</p>
                </div>
              </div>

              {/* Line Items Section */}
              <div className="line-items-section">
                <h3 className="section-title">
                  <FileText size={20} />
                  Line Items ({entryDetail.lineCount})
                </h3>
                <div className="table-container">
                  <table className="entry-details-table">
                    <thead>
                      <tr>
                        <th className="text-center">Line</th>
                        <th>Account Code</th>
                        <th>Account Name</th>
                        <th>Description</th>
                        <th className="text-right">Debit</th>
                        <th className="text-right">Credit</th>
                      </tr>
                    </thead>
                    <tbody>
                      {entryDetail.lines.map((line: JournalEntryLineDTO) => (
                        <tr key={line.id}>
                          <td className="text-center line-number">{line.lineNumber}</td>
                          <td className="account-code">{line.accountCode}</td>
                          <td className="account-name">{line.accountName}</td>
                          <td className="line-description">{line.description}</td>
                          <td className="text-right amount-cell">
                            {line.debitAmount > 0 ? (
                              <span className="debit-amount">
                                {formatCurrency(line.debitAmount)}
                              </span>
                            ) : (
                              <span className="empty-amount">-</span>
                            )}
                          </td>
                          <td className="text-right amount-cell">
                            {line.creditAmount > 0 ? (
                              <span className="credit-amount">
                                {formatCurrency(line.creditAmount)}
                              </span>
                            ) : (
                              <span className="empty-amount">-</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr>
                        <td colSpan={4} className="text-right totals-label">
                          Totals:
                        </td>
                        <td className="text-right total-debit">
                          {formatCurrency(entryDetail.totalDebit)}
                        </td>
                        <td className="text-right total-credit">
                          {formatCurrency(entryDetail.totalCredit)}
                        </td>
                      </tr>
                    </tfoot>
                  </table>
                </div>

                {/* Balance Check */}
                {Math.abs(entryDetail.totalDebit - entryDetail.totalCredit) < 0.01 ? (
                  <div className="balance-alert balanced">
                    <div className="status-indicator"></div>
                    <span>Entry is balanced (Debits = Credits)</span>
                  </div>
                ) : (
                  <div className="balance-alert unbalanced">
                    <AlertCircle size={16} />
                    <span>
                      Entry is not balanced (Difference: {formatCurrency(Math.abs(entryDetail.totalDebit - entryDetail.totalCredit))})
                    </span>
                  </div>
                )}
              </div>

              {/* Metadata Section */}
              <div className="audit-section">
                <h4 className="audit-title">
                  <User size={16} />
                  Audit Information
                </h4>
                <div className="audit-grid">
                  <div className="audit-field">
                    <span className="audit-label">Created By:</span>
                    <span className="audit-value">{entryDetail.createdBy}</span>
                  </div>
                  <div className="audit-field">
                    <span className="audit-label">Created At:</span>
                    <span className="audit-value">{formatDateTime(entryDetail.createdAt)}</span>
                  </div>
                  <div className="audit-field">
                    <span className="audit-label">Last Modified By:</span>
                    <span className="audit-value">{entryDetail.lastModifiedBy || 'N/A'}</span>
                  </div>
                  <div className="audit-field">
                    <span className="audit-label">Last Modified At:</span>
                    <span className="audit-value">
                      {entryDetail.lastModifiedAt ? formatDateTime(entryDetail.lastModifiedAt) : 'N/A'}
                    </span>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>

        {/* Footer */}
        <div className="modal-footer">
          <button
            onClick={onClose}
            className="modal-footer-button"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
