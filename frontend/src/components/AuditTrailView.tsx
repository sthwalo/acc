import { useState, useEffect, useCallback } from 'react';
import { BookOpen, Loader, AlertCircle, Eye, FileText, File } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
import type { Company, FiscalPeriod, AuditTrailDTO } from '../types/api';

interface AuditTrailViewProps {
  selectedCompany: Company;
  selectedPeriod: FiscalPeriod;
  onClose: () => void;
}

export default function AuditTrailView({ selectedCompany, selectedPeriod, onClose }: AuditTrailViewProps) {
  const api = useApi();
  const [auditTrailData, setAuditTrailData] = useState<AuditTrailDTO[] | null>(null);
  const [selectedEntry, setSelectedEntry] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showTextView, setShowTextView] = useState(false);
  const [textViewContent, setTextViewContent] = useState('');

  const fetchAuditTrail = useCallback(async () => {
    if (!selectedCompany || !selectedPeriod) return;

    setIsLoading(true);
    setError(null);

    try {
      const response = await api.reports.getAuditTrail(
        selectedCompany.id,
        selectedPeriod.id
      );

      // API returns AuditTrailDTO[] directly
      setAuditTrailData(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An unexpected error occurred');
    } finally {
      setIsLoading(false);
    }
  }, [api, selectedCompany, selectedPeriod]);

  useEffect(() => {
    fetchAuditTrail();
  }, [fetchAuditTrail]);

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
      month: 'short',
      day: 'numeric'
    });
  };

  // Print behaviour removed — use PDF / download flow instead

  const handleAuditTrailView = async () => {
    setError(null);
    try {
      const report = await api.reports.generateAuditTrail(Number(selectedCompany.id), selectedPeriod.id, 'text');
      if (report && report.content) {
        setTextViewContent(report.content);
        setShowTextView(true);
      } else {
        setError('No audit trail content returned');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate audit trail view');
    }
  };

  const handleAuditTrailDownload = async (format: 'PDF' | 'EXCEL' | 'CSV') => {
    setError(null);
    try {
      await api.reports.downloadAuditTrail(Number(selectedCompany.id), selectedPeriod.id, format);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to download audit trail');
    }
  };

  // Legacy CSV download handler removed; quick-download icons use API downloads

  return (
    <div className="audit-trail-view">
      {/* Header */}
      <div className="audit-trail-header">
        <div className="header-left">
          <BookOpen className="header-icon" size={24} />
          <div>
            <h2>Audit Trail</h2>
            <p className="header-subtitle">
              {selectedCompany.name} - {selectedPeriod.periodName}
            </p>
          </div>
        </div>
        <div className="header-actions">
          <div className="quick-download-icons">
            <button
              type="button"
              className="download-icon"
              data-type="view"
              aria-label="View Audit Trail"
              title="View Report"
              onClick={handleAuditTrailView}
              disabled={isLoading}
            >
              <Eye size={14} />
              <span className="download-label">View</span>
            </button>
            <button
              type="button"
              className="download-icon"
              data-type="pdf"
              aria-label="Download Audit Trail as PDF"
              title="Download PDF"
              disabled={isLoading}
              onClick={() => handleAuditTrailDownload('PDF')}
            >
              <FileText size={14} />
              <span className="download-label">PDF</span>
            </button>
            <button
              type="button"
              className="download-icon"
              data-type="excel"
              aria-label="Download Audit Trail as Excel"
              title="Download Excel"
              disabled={isLoading}
              onClick={() => handleAuditTrailDownload('EXCEL')}
            >
              <File size={14} />
              <span className="download-label">XLSX</span>
            </button>
            <button
              type="button"
              className="download-icon"
              data-type="csv"
              aria-label="Download Audit Trail as CSV"
              title="Download CSV"
              disabled={isLoading}
              onClick={() => handleAuditTrailDownload('CSV')}
            >
              <FileText size={14} />
              <span className="download-label">CSV</span>
            </button>
          </div>
          {/* Print button removed — use PDF download / print workflow via exported PDF */}
          {/* Legacy download button removed; use quick download icons for format downloads */}
          <button
            onClick={onClose}
            className="close-button"
            type="button"
          >
            Close
          </button>
        </div>
      </div>

      {/* Text View Modal */}
      {showTextView && (
        <div className="audit-trail-text-view">
          <div className="text-view-actions">
            <button type="button" className="close-button" onClick={() => setShowTextView(false)}>Close</button>
            <button type="button" className="action-button" onClick={() => {
              const blob = new Blob([textViewContent], { type: 'text/plain' });
              const url = window.URL.createObjectURL(blob);
              const link = document.createElement('a');
              link.href = url;
              const filename = `audit_trail_${selectedCompany.name}_${selectedPeriod.periodName}.txt`.replace(/[^a-zA-Z0-9_.-]/g, '_');
              link.setAttribute('download', filename);
              document.body.appendChild(link);
              link.click();
              link.remove();
              window.URL.revokeObjectURL(url);
            }}>Download as TXT</button>
          </div>
          <div className="text-view-content">
            <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace', maxHeight: 600, overflow: 'auto' }}>{textViewContent}</pre>
          </div>
        </div>
      )}

      {/* API Message Banner */}
      {error && (
        <ApiMessageBanner
          message={error}
          type="error"
        />
      )}

      {/* Loading State */}
      {isLoading && (
        <div className="loading-state">
          <Loader className="spinner-icon" size={32} />
          <span>Loading audit trail...</span>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && auditTrailData && auditTrailData.length === 0 && (
        <div className="empty-state">
          <AlertCircle size={48} />
          <p className="empty-title">No journal entries found</p>
          <p className="empty-subtitle">Try adjusting your filters or date range</p>
        </div>
      )}

      {/* Data Table */}
      {!isLoading && auditTrailData && auditTrailData.length > 0 && (
        <>
          <div className="audit-trail-table-container">
            <table className="audit-trail-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Date</th>
                  <th>Description</th>
                  <th className="text-right">Total Debit</th>
                  <th className="text-right">Total Credit</th>
                  <th className="text-center">Lines</th>
                  <th>Created By</th>
                </tr>
              </thead>
              <tbody>
                {auditTrailData.map((entry: AuditTrailDTO, index: number) => {
                  // Calculate totals from lines
                  const totalDebit = entry.lines.reduce((sum, line) => sum + (line.debit || 0), 0);
                  const totalCredit = entry.lines.reduce((sum, line) => sum + (line.credit || 0), 0);
                  
                  return (
                    <tr
                      key={`${entry.reference}-${index}`}
                      onClick={() => setSelectedEntry(index)}
                      className="table-row-clickable"
                    >
                      <td className="reference-cell">{entry.reference}</td>
                      <td>{formatDate(entry.entryDate)}</td>
                      <td>{entry.description}</td>
                      <td className="text-right amount-cell">{formatCurrency(totalDebit)}</td>
                      <td className="text-right amount-cell">{formatCurrency(totalCredit)}</td>
                      <td className="text-center">{entry.lines.length}</td>
                      <td>{entry.createdBy}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* Detail Modal */}
      {selectedEntry !== null && auditTrailData && (
        <div className="journal-entry-detail-modal">
          <div className="modal-content">
            <div className="modal-header">
              <h3>Journal Entry Details</h3>
              <button onClick={() => setSelectedEntry(null)} className="close-button">×</button>
            </div>
            <div className="modal-body">
              {(() => {
                const entry = auditTrailData[selectedEntry];
                return (
                  <div className="entry-details">
                    <div className="entry-header">
                      <div className="entry-info">
                        <strong>Reference:</strong> {entry.reference}<br/>
                        <strong>Date:</strong> {formatDate(entry.entryDate)}<br/>
                        <strong>Description:</strong> {entry.description}<br/>
                        <strong>Created By:</strong> {entry.createdBy}<br/>
                        <strong>Created At:</strong> {formatDate(entry.createdAt)}
                      </div>
                    </div>
                    <div className="entry-lines">
                      <h4>Journal Entry Lines</h4>
                      <table className="lines-table">
                        <thead>
                          <tr>
                            <th>Account Code</th>
                            <th>Account Name</th>
                            <th>Description</th>
                            <th className="text-right">Debit</th>
                            <th className="text-right">Credit</th>
                          </tr>
                        </thead>
                        <tbody>
                          {entry.lines.map((line, lineIndex) => (
                            <tr key={lineIndex}>
                              <td>{line.accountCode}</td>
                              <td>{line.accountName}</td>
                              <td>{line.description}</td>
                              <td className="text-right">{line.debit ? formatCurrency(line.debit) : ''}</td>
                              <td className="text-right">{line.credit ? formatCurrency(line.credit) : ''}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                );
              })()}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
