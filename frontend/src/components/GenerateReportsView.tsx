import { useState, useEffect, useCallback } from 'react';
import { FileText, Calendar, BookOpen, Eye, File } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
import AuditTrailView from './AuditTrailView';
import type { Company, FiscalPeriod, AuditTrailDTO } from '../types/api';

interface GenerateReportsViewProps {
  selectedCompany: Company;
}

interface ReportType {
  id: string;
  name: string;
  description: string;
  icon: React.ComponentType<{ size?: number; className?: string }>;
  formats: string[];
}

const reportTypes: ReportType[] = [
  {
    id: 'trial-balance',
    name: 'Trial Balance',
    description: 'Summary of all general ledger accounts and their balances',
    icon: FileText,
    formats: ['View', 'PDF', 'Excel', 'CSV']
  },
  {
    id: 'income-statement',
    name: 'Income Statement',
    description: 'Revenue, expenses, and profit/loss for the period',
    icon: FileText,
    formats: ['View', 'PDF', 'Excel', 'CSV']
  },
  {
    id: 'balance-sheet',
    name: 'Balance Sheet',
    description: 'Assets, liabilities, and equity at a specific point in time',
    icon: FileText,
    formats: ['View', 'PDF', 'Excel', 'CSV']
  },
  {
    id: 'cash-flow',
    name: 'Cash Flow Statement',
    description: 'Cash inflows and outflows during the period',
    icon: FileText,
    formats: ['View', 'PDF', 'Excel', 'CSV']
  },
  {
    id: 'general-ledger',
    name: 'General Ledger',
    description: 'Complete record of all financial transactions',
    icon: FileText,
    formats: ['View', 'PDF', 'Excel', 'CSV']
  },
  {
    id: 'cashbook',
    name: 'Cashbook Report',
    description: 'Detailed cash transaction records and balances',
    icon: FileText,
    formats: ['View', 'PDF', 'Excel', 'CSV']
  }
];

export default function GenerateReportsView({ selectedCompany }: GenerateReportsViewProps) {
  const api = useApi();
  const [fiscalPeriods, setFiscalPeriods] = useState<FiscalPeriod[]>([]);
  const [selectedPeriod, setSelectedPeriod] = useState<FiscalPeriod | null>(null);
  const [selectedReport, setSelectedReport] = useState<ReportType | null>(null);
  const [selectedFormat, setSelectedFormat] = useState<string>('');
  // no global generating state; each download/view action handles own state if necessary
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showAuditTrail, setShowAuditTrail] = useState(false);
  const [activeReportView, setActiveReportView] = useState<string | null>(null);
  const [reportContent, setReportContent] = useState<string>('');
  const [auditTrailJsonData, setAuditTrailJsonData] = useState<AuditTrailDTO[] | null>(null);

  const loadFiscalPeriods = useCallback(async () => {
    try {
      setError(null);
      const periods = await api.getFiscalPeriods(Number(selectedCompany.id));
      setFiscalPeriods(periods);
      if (periods.length > 0 && !selectedPeriod) {
        setSelectedPeriod(periods[0]);
      }
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to load fiscal periods';
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
        // ignore parsing error
      }
      setError(message);
    }
  }, [api, selectedCompany.id, selectedPeriod]);

  useEffect(() => {
    loadFiscalPeriods();
  }, [loadFiscalPeriods]);

  // generateReport removed - main Generate button removed per request

  // Quick download handler used by the report cards (PDF/Excel/CSV)
  const handleQuickDownload = async (reportId: string, format: 'PDF' | 'EXCEL' | 'CSV') => {
    if (!selectedCompany || !selectedPeriod) {
      setError('Please select a company and fiscal period first');
      return;
    }

    setError(null);
    setSuccess(null);
    try {
      switch (reportId) {
        case 'trial-balance':
          await api.reports.downloadTrialBalance(Number(selectedCompany.id), selectedPeriod.id, format);
          break;
        case 'income-statement':
          await api.reports.downloadIncomeStatement(Number(selectedCompany.id), selectedPeriod.id, format);
          break;
        case 'balance-sheet':
          await api.reports.downloadBalanceSheet(Number(selectedCompany.id), selectedPeriod.id, format);
          break;
        case 'general-ledger':
          await api.reports.downloadGeneralLedger(Number(selectedCompany.id), selectedPeriod.id, format);
          break;
        case 'cashbook':
          await api.reports.downloadCashbook(Number(selectedCompany.id), selectedPeriod.id, format);
          break;
        default:
          throw new Error(`Download not implemented for: ${reportId}`);
      }
      setSuccess(`${reportId.replace('-', ' ')} downloaded (${format})`);
    } catch (err) {
      setError(`Failed to download ${reportId.replace('-', ' ')}: ${err instanceof Error ? err.message : String(err)}`);
    } finally {
      // done
    }
  };

  // If the user selects 'View' format, auto-generate the text report and display it
  useEffect(() => {
    const autoGenerateView = async () => {
      if (!selectedReport || !selectedPeriod || selectedFormat !== 'View') return;
      setError(null);
      setSuccess(null);
      try {
        let reportData;
        switch (selectedReport.id) {
          case 'trial-balance':
            reportData = await api.generateTrialBalance(Number(selectedCompany.id), selectedPeriod.id, 'text');
            break;
          case 'income-statement':
            reportData = await api.generateIncomeStatement(Number(selectedCompany.id), selectedPeriod.id, 'text');
            break;
          case 'balance-sheet':
            reportData = await api.generateBalanceSheet(Number(selectedCompany.id), selectedPeriod.id, 'text');
            break;
          case 'cash-flow':
            reportData = await api.generateCashFlow(Number(selectedCompany.id), selectedPeriod.id, 'text');
            break;
          case 'general-ledger':
            reportData = await api.generateGeneralLedger(Number(selectedCompany.id), selectedPeriod.id, 'text');
            break;
          case 'cashbook':
            reportData = await api.generateCashbook(Number(selectedCompany.id), selectedPeriod.id, 'text');
            break;
          case 'audit-trail': {
            // For audit trail, get structured JSON data instead of text
            const auditTrailData = await api.reports.getAuditTrail(Number(selectedCompany.id), selectedPeriod.id);
            setAuditTrailJsonData(auditTrailData);
            setReportContent(''); // Clear text content for JSON view
            break;
          }
          default:
            return;
        }
        // Only set report content for non-audit-trail reports
        if (selectedReport.id !== 'audit-trail' && reportData) {
          setReportContent(reportData.content);
        }
        setActiveReportView(selectedReport.id);
        setSuccess(`${selectedReport.name} report generated successfully`);
      } catch (err) {
        if (err instanceof Error) setError(err.message);
        else setError('Failed to generate view report');
      }
    };
    autoGenerateView();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedFormat, selectedReport, selectedPeriod]);

  // If a report view is active, render it instead of the main form
  if (activeReportView) {
    function downloadTextFile(text: string, filename: string) {
      const blob = new Blob([text], { type: 'text/plain' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    }

    return (
      <div className="report-view-container">
        <button
          onClick={() => setActiveReportView(null)}
          className="back-to-reports-button"
        >
          ← Back to Reports
        </button>
        <div className="report-content">
          <h2>
            {selectedReport?.name} - {selectedPeriod?.periodName}
          </h2>

          {activeReportView === 'audit-trail' && auditTrailJsonData ? (
            <div className="audit-trail-json-view">
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
                    {auditTrailJsonData.map((entry: AuditTrailDTO, index: number) => {
                      const totalDebit = entry.lines.reduce((sum, line) => sum + (line.debit || 0), 0);
                      const totalCredit = entry.lines.reduce((sum, line) => sum + (line.credit || 0), 0);

                      return (
                        <tr key={`${entry.reference}-${index}`}>
                          <td className="reference-cell">{entry.reference}</td>
                          <td>{new Date(entry.entryDate).toLocaleDateString('en-ZA')}</td>
                          <td>{entry.description}</td>
                          <td className="text-right amount-cell">
                            {new Intl.NumberFormat('en-ZA', {
                              style: 'currency',
                              currency: 'ZAR'
                            }).format(totalDebit)}
                          </td>
                          <td className="text-right amount-cell">
                            {new Intl.NumberFormat('en-ZA', {
                              style: 'currency',
                              currency: 'ZAR'
                            }).format(totalCredit)}
                          </td>
                          <td className="text-center">{entry.lines.length}</td>
                          <td>{entry.createdBy}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          ) : (
            <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'monospace', maxHeight: 600, overflow: 'auto' }}>
              {reportContent}
            </pre>
          )}

          {reportContent && (
            <button
              onClick={() => downloadTextFile(
                reportContent,
                `${selectedReport?.id}_${selectedCompany.id}_${selectedPeriod?.id}.txt`
              )}
              style={{ marginTop: '1rem' }}
            >
              Download as TXT
            </button>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="generate-reports-view">
      <div className="view-header">
        <h2>Generate Reports</h2>
        <p>Generate comprehensive financial reports for {selectedCompany.name}</p>
      </div>

      <ApiMessageBanner message={error} type="error" />
      <ApiMessageBanner message={success} type="success" />

      <div className="report-generation-form">
        {/* Fiscal Period Selection */}
        <div className="form-section">
          <h3>
            <Calendar size={20} />
            Select Fiscal Period
          </h3>
          <div className="period-selector">
            {fiscalPeriods.map((period) => (
              <button
                key={period.id}
                className={`period-button ${selectedPeriod?.id === period.id ? 'active' : ''}`}
                onClick={() => setSelectedPeriod(period)}
              >
                <div className="period-name">{period.periodName}</div>
                <div className="period-dates">
                  {new Date(period.startDate).toLocaleDateString()} - {new Date(period.endDate).toLocaleDateString()}
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Report Type Selection */}
        <div className="form-section">
          <h3>
            <FileText size={20} />
            Select Report Type
          </h3>
          <div className="report-types-grid">
            {reportTypes.map((report) => {
              const Icon = report.icon;
              return (
                <div key={report.id} className={`report-type-card ${selectedReport?.id === report.id ? 'active' : ''}`}>
                  <div className="report-main">
                    <button
                      type="button"
                      className="report-select-button"
                      aria-label={`Select ${report.name}`}
                      onClick={() => setSelectedReport(report)}
                    >
                      <Icon size={24} />
                      <div className="report-info">
                        <h4>{report.name}</h4>
                        <p>{report.description}</p>
                      </div>
                    </button>
                  </div>
                  <div className="report-card-footer">
                          <div className="quick-download-icons">
                            <button
                              className="download-icon"
                              title="View Report"
                              data-type="view"
                              aria-label={`View ${report.name}`}
                              disabled={!selectedPeriod}
                              type="button"
                              onClick={() => { setSelectedReport(report); setSelectedFormat('View'); }}
                            >
                              <Eye size={10} />
                              <span className="download-label">View</span>
                            </button>
                            <button
                              className="download-icon"
                              title="Download PDF"
                              data-type="pdf"
                              aria-label={`Download ${report.name} as PDF`}
                              disabled={!selectedPeriod}
                              type="button"
                              onClick={() => handleQuickDownload(report.id, 'PDF')}
                            >
                              <FileText size={10} />
                              <span className="download-label">PDF</span>
                            </button>
                            <button
                              className="download-icon"
                              title="Download Excel"
                              data-type="excel"
                              aria-label={`Download ${report.name} as Excel`}
                              disabled={!selectedPeriod}
                              type="button"
                              onClick={() => handleQuickDownload(report.id, 'EXCEL')}
                            >
                              <File size={10} />
                              <span className="download-label">XLSX</span>
                            </button>
                            <button
                              className="download-icon"
                              title="Download CSV"
                              data-type="csv"
                              aria-label={`Download ${report.name} as CSV`}
                              disabled={!selectedPeriod}
                              type="button"
                              onClick={() => handleQuickDownload(report.id, 'CSV')}
                            >
                              <FileText size={10} />
                              <span className="download-label">CSV</span>
                            </button>
                          </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="form-actions">
          <button
            className="generate-button"
            onClick={() => setShowAuditTrail(true)}
            disabled={!selectedPeriod}
            style={{ marginLeft: '1rem', backgroundColor: '#059669' }}
          >
            <BookOpen size={20} />
            View Audit Trail
          </button>
        </div>
      </div>

      {/* Audit Trail View */}
      {showAuditTrail && selectedPeriod && (
        <div className="mt-6">
          <AuditTrailView
            selectedCompany={selectedCompany}
            selectedPeriod={selectedPeriod}
            onClose={() => setShowAuditTrail(false)}
          />
        </div>
      )}
    </div>
  );
}