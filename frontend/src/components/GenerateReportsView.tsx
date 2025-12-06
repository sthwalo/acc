import { useState, useEffect, useCallback } from 'react';
import { FileText, Download, Calendar, BookOpen } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
import AuditTrailView from './AuditTrailView';
import type { Company, FiscalPeriod } from '../types/api';

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
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showAuditTrail, setShowAuditTrail] = useState(false);
  const [activeReportView, setActiveReportView] = useState<string | null>(null);

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

  const generateReport = async () => {
    if (!selectedReport || !selectedPeriod || !selectedFormat) {
      setError('Please select a report type, fiscal period, and format');
      return;
    }

    // If View format is selected, show the view component instead
    if (selectedFormat === 'View') {
      setActiveReportView(selectedReport.id);
      return;
    }

    setIsGenerating(true);
    setError(null);
    setSuccess(null);

    try {
      let reportData;

      // Call the appropriate API method based on report type
      switch (selectedReport.id) {
        case 'trial-balance':
          reportData = await api.generateTrialBalance(Number(selectedCompany.id), selectedPeriod.id, selectedFormat.toLowerCase());
          break;
        case 'income-statement':
          reportData = await api.generateIncomeStatement(Number(selectedCompany.id), selectedPeriod.id, selectedFormat.toLowerCase());
          break;
        case 'balance-sheet':
          reportData = await api.generateBalanceSheet(Number(selectedCompany.id), selectedPeriod.id, selectedFormat.toLowerCase());
          break;
        case 'cash-flow':
          reportData = await api.generateCashFlow(Number(selectedCompany.id), selectedPeriod.id, selectedFormat.toLowerCase());
          break;
        case 'general-ledger':
          reportData = await api.generateGeneralLedger(Number(selectedCompany.id), selectedPeriod.id, selectedFormat.toLowerCase());
          break;
        case 'cashbook':
          reportData = await api.generateCashbook(Number(selectedCompany.id), selectedPeriod.id, selectedFormat.toLowerCase());
          break;
        case 'audit-trail':
          reportData = await api.generateAuditTrail(Number(selectedCompany.id), selectedPeriod.id, selectedFormat.toLowerCase());
          break;
        default:
          throw new Error(`Unknown report type: ${selectedReport.id}`);
      }

      // Handle the report data (for now, just show success)
      // TODO: Implement file download for PDF/Excel/CSV formats
      console.log('Report generated:', reportData);

      setSuccess(`${selectedReport.name} report generated successfully in ${selectedFormat} format`);

      // Handle file download for all formats
      if (reportData) {
        let mimeType = 'text/plain';
        let extension = 'txt';

        switch (selectedFormat.toLowerCase()) {
          case 'pdf':
            mimeType = 'application/pdf';
            extension = 'pdf';
            break;
          case 'excel':
          case 'xlsx':
            mimeType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
            extension = 'xlsx';
            break;
          case 'csv':
            mimeType = 'text/csv';
            extension = 'csv';
            break;
          case 'text':
          default:
            mimeType = 'text/plain';
            extension = 'txt';
            break;
        }

        // Create a blob and download it
        const blob = new Blob([reportData.content], { type: mimeType });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${selectedReport.name.replace(/\s+/g, '_')}_${selectedPeriod.periodName}.${extension}`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      }

    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to generate report';
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
    } finally {
      setIsGenerating(false);
    }
  };

  // If a report view is active, render it instead of the main form
  if (activeReportView) {
    return (
      <div className="report-view-container">
        <button
          onClick={() => setActiveReportView(null)}
          className="back-to-reports-button"
        >
          ← Back to Reports
        </button>
        {activeReportView === 'trial-balance' && (
          <div className="report-content">
            <h2>Trial Balance - {selectedPeriod?.periodName}</h2>
            <p>Trial Balance view component will be implemented here</p>
          </div>
        )}
        {activeReportView === 'income-statement' && (
          <div className="report-content">
            <h2>Income Statement - {selectedPeriod?.periodName}</h2>
            <p>Income Statement view component will be implemented here</p>
          </div>
        )}
        {activeReportView === 'balance-sheet' && (
          <div className="report-content">
            <h2>Balance Sheet - {selectedPeriod?.periodName}</h2>
            <p>Balance Sheet view component will be implemented here</p>
          </div>
        )}
        {activeReportView === 'cash-flow' && (
          <div className="report-content">
            <h2>Cash Flow Statement - {selectedPeriod?.periodName}</h2>
            <p>Cash Flow Statement view component will be implemented here</p>
          </div>
        )}
        {activeReportView === 'general-ledger' && (
          <div className="report-content">
            <h2>General Ledger - {selectedPeriod?.periodName}</h2>
            <p>General Ledger view component will be implemented here</p>
          </div>
        )}
        {activeReportView === 'cashbook' && (
          <div className="report-content">
            <h2>Cashbook Report - {selectedPeriod?.periodName}</h2>
            <p>Cashbook Report view component will be implemented here</p>
          </div>
        )}
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
                <button
                  key={report.id}
                  className={`report-type-card ${selectedReport?.id === report.id ? 'active' : ''}`}
                  onClick={() => setSelectedReport(report)}
                >
                  <Icon size={24} />
                  <div className="report-info">
                    <h4>{report.name}</h4>
                    <p>{report.description}</p>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        {/* Format Selection */}
        {selectedReport && (
          <div className="form-section">
            <h3>
              <Download size={20} />
              Select Format
            </h3>
            <div className="format-selector">
              {selectedReport.formats.map((format) => (
                <button
                  key={format}
                  className={`format-button ${selectedFormat === format ? 'active' : ''}`}
                  onClick={() => setSelectedFormat(format)}
                >
                  {format}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Generate Button */}
        <div className="form-actions">
          <button
            className="generate-button"
            onClick={generateReport}
            disabled={!selectedReport || !selectedPeriod || !selectedFormat || isGenerating}
          >
            {isGenerating ? (
              <>
                <div className="spinner small"></div>
                Generating Report...
              </>
            ) : (
              <>
                <Download size={20} />
                Generate Report
              </>
            )}
          </button>
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