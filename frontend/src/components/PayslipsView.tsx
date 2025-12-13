import { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { FileText, Download, Mail, Users, AlertCircle, CheckCircle, Eye, ArrowLeft } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
import type { FiscalPeriod, Payslip, BackendPayslip } from '../types/api';

interface PayslipsViewProps {
  selectedFiscalPeriod: FiscalPeriod;
  onViewChange?: (view: string) => void;
}

export default function PayslipsView({ selectedFiscalPeriod, onViewChange }: PayslipsViewProps) {
  const api = useApi();
  const [payslips, setPayslips] = useState<Payslip[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [isEmailing, setIsEmailing] = useState(false);
  const [selectedPayslips, setSelectedPayslips] = useState<Set<number>>(new Set());
  const [lastDownloadHash, setLastDownloadHash] = useState<string | null>(null);
  const [lastDownloadSource, setLastDownloadSource] = useState<'fetch' | 'axios' | null>(null);

  const loadPayslips = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);

      // Load actual payslips from the backend API
      const backendPayslips = await api.getPayslipsByFiscalPeriod(selectedFiscalPeriod.id);

      // Transform backend payslips to frontend format
      const transformedPayslips: Payslip[] = backendPayslips.map((payslip: BackendPayslip) => ({
        id: payslip.id,
        employeeId: payslip.employeeId,
        fiscalPeriodId: payslip.fiscalPeriodId,
        employeeNumber: `EMP-${payslip.employeeId}`, // Placeholder until we fetch employee data
        employeeName: `Employee ${payslip.employeeId}`, // Placeholder until we fetch employee data
        grossPay: payslip.grossSalary || payslip.totalEarnings || 0,
        deductions: payslip.totalDeductions || 0,
        netPay: payslip.netPay || 0,
        payDate: payslip.paymentDate || selectedFiscalPeriod.endDate,
        generatedAt: payslip.createdAt || new Date().toISOString(),
        status: payslip.status === 'PAID' ? 'SENT' : payslip.status === 'APPROVED' ? 'GENERATED' : 'GENERATED'
      }));

      setPayslips(transformedPayslips);
    } catch (err) {
      let message = 'Failed to load payslips';
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
      setIsLoading(false);
    }
  }, [selectedFiscalPeriod, api]);

  useEffect(() => {
    loadPayslips();
  }, [loadPayslips]);

  const generatePayslipPDF = async (payslip: Payslip) => {
    try {
      setError(null);
      // Use axios for all PDF downloads
      const base = (import.meta.env.VITE_API_URL as string) || '/api';
      const apiUrl = `${base}/v1/payroll/payslips/${payslip.id}/pdf`;
      const axiosRes = await axios.get(apiUrl, { responseType: 'arraybuffer', headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` } });
      const responseContentType = axiosRes.headers['content-type'] || '';
      if (!responseContentType.includes('application/pdf')) {
        const body = new TextDecoder().decode(new Uint8Array(axiosRes.data).slice(0, 400));
        console.error('Download returned non-PDF content. First 400 chars:', body);
        setError('Download failed: server returned non-PDF content (HTML or error page) - check backend or VITE_API_URL/proxy.');
        return;
      }
      const arrayBuffer = axiosRes.data as ArrayBuffer;
      const blob = new Blob([arrayBuffer], { type: responseContentType });
      // Compute prefix and hash like the fetch path
      try {
        console.log('Payslip blob size:', blob.size);
        const prefixBuffer = await blob.slice(0, 8).arrayBuffer();
        const prefixBytes = new Uint8Array(prefixBuffer);
        const prefixHex = Array.from(prefixBytes).map(b => b.toString(16).padStart(2, '0')).join(' ');
        console.log('Payslip blob prefix (hex):', prefixHex);
        const arrayBuf = await blob.arrayBuffer();
        const hashBuf = await crypto.subtle.digest('SHA-256', arrayBuf);
        const hashArray = Array.from(new Uint8Array(hashBuf));
        const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
        console.log('Payslip blob SHA256:', hashHex);
        setLastDownloadHash(hashHex);
        setLastDownloadSource('axios');
      } catch (e) {
        console.warn('Failed to inspect blob prefix for payslip PDF', e);
      }

      const blobUrl = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = `Payslip_${payslip.employeeNumber}_${selectedFiscalPeriod.periodName}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(blobUrl);
      document.body.removeChild(a);

      // Update payslip status
      setPayslips(prev => prev.map(p => p.id === payslip.id ? { ...p, status: 'DOWNLOADED' as const } : p));

      setSuccess(`Payslip for ${payslip.employeeName} downloaded successfully`);
    } catch (err) {
      let message = 'Failed to generate payslip PDF';
      if (err instanceof Error) message = err.message;
      setError(message);
    }
  };

  const viewPayslip = async (payslip: Payslip) => {
    try {
      setError(null);
      // Use axios to get the PDF and open it in a new tab for viewing
      const base = (import.meta.env.VITE_API_URL as string) || '/api';
      const apiUrl = `${base}/v1/payroll/payslips/${payslip.id}/pdf`;
      const axiosRes = await axios.get(apiUrl, { responseType: 'arraybuffer', headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` } });
      const responseContentType = axiosRes.headers['content-type'] || '';
      if (!responseContentType.includes('application/pdf')) {
        const body = new TextDecoder().decode(new Uint8Array(axiosRes.data).slice(0, 400));
        console.error('View returned non-PDF content. First 400 chars:', body);
        setError('View failed: server returned non-PDF content (HTML or error page) - check backend or VITE_API_URL/proxy.');
        return;
      }
      const arrayBuffer = axiosRes.data as ArrayBuffer;
      const blob = new Blob([arrayBuffer], { type: responseContentType });
      const blobUrl = window.URL.createObjectURL(blob);

      // Open in new tab for viewing
      window.open(blobUrl, '_blank');

      // Clean up the blob URL after a delay to ensure the PDF loads
      setTimeout(() => {
        window.URL.revokeObjectURL(blobUrl);
      }, 1000);

      setSuccess(`Payslip for ${payslip.employeeName} opened for viewing`);
    } catch (err) {
      let message = 'Failed to view payslip PDF';
      if (err instanceof Error) message = err.message;
      setError(message);
    }
  };

  const generateAllPayslips = async () => {
    setIsGenerating(true);
    setError(null);
    setSuccess(null);

    try {
      // Generate all payslips for the fiscal period
      await api.processPayroll(selectedFiscalPeriod.id);
      setSuccess('All payslips generated successfully');
      await loadPayslips(); // Refresh the list
    } catch (err) {
      let message = 'Failed to generate payslips';
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

  const exportBulkPDFs = async () => {
    setIsExporting(true);
    setError(null);
    setSuccess(null);

    try {
      // Export all payslips as a ZIP file using axios
      const base = (import.meta.env.VITE_API_URL as string) || '/api';
      const apiUrl = `${base}/v1/payroll/payslips/bulk-export?fiscalPeriodId=${selectedFiscalPeriod.id}`;
      const axiosRes = await axios.get(apiUrl, { responseType: 'arraybuffer', headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` } });

      const contentType = axiosRes.headers['content-type'] || 'application/octet-stream';
      if (!contentType.includes('application/zip') && !contentType.includes('application/octet-stream')) {
        const body = new TextDecoder().decode(new Uint8Array(axiosRes.data).slice(0, 400));
        console.error('Bulk export returned non-binary content. First 400 chars:', body);
        setError('Bulk export failed: server returned non-binary content (HTML or error page). Check backend or proxy config.');
        return;
      }

      const arrayBuffer = axiosRes.data as ArrayBuffer;
      const blob = new Blob([arrayBuffer], { type: contentType });

      try {
        console.log('Bulk export blob size:', blob.size);
        const prefixBuffer = await blob.slice(0, 8).arrayBuffer();
        const prefixBytes = new Uint8Array(prefixBuffer);
        const prefixHex = Array.from(prefixBytes).map(b => b.toString(16).padStart(2, '0')).join(' ');
        console.log('Bulk export blob prefix (hex):', prefixHex);
        try {
          const arrayBuf = await blob.arrayBuffer();
          const hashBuf = await crypto.subtle.digest('SHA-256', arrayBuf);
          const hashArray = Array.from(new Uint8Array(hashBuf));
          const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
          console.log('Bulk export blob SHA256:', hashHex);
        } catch (e) {
          console.warn('Failed to compute bulk export blob hash', e);
        }
      } catch (e) {
        console.warn('Failed to inspect blob prefix for bulk export', e);
      }

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Payslips_${selectedFiscalPeriod.periodName}.zip`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      setSuccess('All payslips exported successfully');
    } catch (err) {
      let message = 'Failed to export payslips';
      if (axios.isAxiosError(err)) {
        if (err.response?.status === 404) {
          message = 'No payslips found for this fiscal period';
        } else if (err.response?.status === 500) {
          message = 'Server error occurred during bulk export';
        } else if (err.response?.data) {
          const errorData = err.response.data;
          if (typeof errorData === 'string') {
            message = errorData;
          } else if (errorData.message) {
            message = errorData.message;
          }
        }
      } else if (err instanceof Error) {
        message = err.message;
      }
      setError(message);
    } finally {
      setIsExporting(false);
    }
  };

  const sendPayslipsByEmail = async () => {
    if (selectedPayslips.size === 0) {
      setError('Please select payslips to send');
      return;
    }

    setIsEmailing(true);
    setError(null);
    setSuccess(null);

    try {
      const payslipIds = Array.from(selectedPayslips);
      const base = (import.meta.env.VITE_API_URL as string) || '/api';
      const apiUrl = `${base}/v1/payroll/payslips/send-email`;
      await axios.post(apiUrl, {
        payslipIds,
        fiscalPeriodId: selectedFiscalPeriod.id
      }, {
        headers: { Authorization: `Bearer ${localStorage.getItem('auth_token')}` }
      });

      // Update status of sent payslips
      setPayslips(prev => prev.map(p =>
        selectedPayslips.has(p.id) ? { ...p, status: 'SENT' as const } : p
      ));

      setSelectedPayslips(new Set());
      setSuccess(`${selectedPayslips.size} payslip(s) sent successfully`);
    } catch (err) {
      let message = 'Failed to send payslips by email';
      if (axios.isAxiosError(err)) {
        if (err.response?.status === 400) {
          message = 'Invalid request - please check selected payslips';
        } else if (err.response?.status === 404) {
          message = 'Some payslips not found';
        } else if (err.response?.data?.message) {
          message = err.response.data.message;
        }
      } else if (err instanceof Error) {
        message = err.message;
      }
      setError(message);
    } finally {
      setIsEmailing(false);
    }
  };

  const togglePayslipSelection = (payslipId: number) => {
    const newSelection = new Set(selectedPayslips);
    if (newSelection.has(payslipId)) {
      newSelection.delete(payslipId);
    } else {
      newSelection.add(payslipId);
    }
    setSelectedPayslips(newSelection);
  };

  const selectAllPayslips = () => {
    setSelectedPayslips(new Set(payslips.map(p => p.id)));
  };

  const clearSelection = () => {
    setSelectedPayslips(new Set());
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'SENT':
        return <Mail size={16} className="status-icon sent" />;
      case 'DOWNLOADED':
        return <Download size={16} className="status-icon downloaded" />;
      default:
        return <CheckCircle size={16} className="status-icon generated" />;
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'SENT':
        return 'Sent';
      case 'DOWNLOADED':
        return 'Downloaded';
      default:
        return 'Generated';
    }
  };

  return (
    <div className="payslips-view">
      <div className="view-header">
        <div className="header-actions">
          <button
            className="back-button"
            onClick={() => onViewChange?.('payroll-management')}
            title="Back to Payroll Management"
          >
            <ArrowLeft size={20} />
            Back to Payroll Management
          </button>
        </div>
        <h2>Payslips Management</h2>
        <p>Generate and distribute payslips for {selectedFiscalPeriod.periodName}</p>
      </div>

      <ApiMessageBanner message={error} type="error" />
      <ApiMessageBanner message={success} type="success" />

      {/* Actions Bar */}
      <div className="payslips-actions">
        <div className="action-buttons">
          <button
            className="action-button primary"
            onClick={generateAllPayslips}
            disabled={isGenerating}
          >
            {isGenerating ? (
              <>
                <div className="spinner small"></div>
                Generating...
              </>
            ) : (
              <>
                <FileText size={20} />
                Generate All Payslips
              </>
            )}
          </button>

          <button
            className="action-button secondary"
            onClick={exportBulkPDFs}
            disabled={isExporting || payslips.length === 0}
          >
            {isExporting ? (
              <>
                <div className="spinner small"></div>
                Exporting...
              </>
            ) : (
              <>
                <Download size={20} />
                Export All PDFs
              </>
            )}
          </button>

          <button
            className="action-button info"
            onClick={sendPayslipsByEmail}
            disabled={isEmailing || selectedPayslips.size === 0}
          >
            {isEmailing ? (
              <>
                <div className="spinner small"></div>
                Sending...
              </>
            ) : (
              <>
                <Mail size={20} />
                Send Selected ({selectedPayslips.size})
              </>
            )}
          </button>
        </div>

        <div className="selection-controls">
          <button
            className="action-button outline"
            onClick={selectAllPayslips}
            disabled={payslips.length === 0}
          >
            Select All
          </button>
          <button
            className="action-button outline"
            onClick={clearSelection}
            disabled={selectedPayslips.size === 0}
          >
            Clear Selection
          </button>
        </div>
      </div>

      {/* Payslips Table */}
      <div className="payslips-container">
        {isLoading ? (
          <div className="loading-state">
            <div className="spinner large"></div>
            <p>Loading payslips...</p>
          </div>
        ) : (
          <table className="payslips-table">
            <thead>
              <tr>
                <th>
                  <input
                    type="checkbox"
                    checked={selectedPayslips.size === payslips.length && payslips.length > 0}
                    onChange={(e) => e.target.checked ? selectAllPayslips() : clearSelection()}
                  />
                </th>
                <th>Employee</th>
                <th>Employee #</th>
                <th>Gross Pay</th>
                <th>Deductions</th>
                <th>Net Pay</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {payslips.map((payslip) => (
                <tr key={payslip.id}>
                  <td>
                    <input
                      type="checkbox"
                      checked={selectedPayslips.has(payslip.id)}
                      onChange={() => togglePayslipSelection(payslip.id)}
                    />
                  </td>
                  <td>
                    <div className="employee-info">
                      <div className="employee-name">{payslip.employeeName}</div>
                    </div>
                  </td>
                  <td>{payslip.employeeNumber}</td>
                  <td>R {payslip.grossPay.toLocaleString()}</td>
                  <td>R {payslip.deductions.toLocaleString()}</td>
                  <td>R {payslip.netPay.toLocaleString()}</td>
                  <td>
                    <div className="status-cell">
                      {getStatusIcon(payslip.status)}
                      <span>{getStatusText(payslip.status)}</span>
                    </div>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="action-button view"
                        onClick={() => viewPayslip(payslip)}
                        title="View PDF"
                      >
                        <Eye size={16} />
                      </button>
                      <button
                        className="action-button download"
                        onClick={() => generatePayslipPDF(payslip)}
                        title="Download PDF"
                      >
                        <Download size={16} />
                      </button>
                      <button
                        className="action-button email"
                        onClick={() => {
                          setSelectedPayslips(new Set([payslip.id]));
                          sendPayslipsByEmail();
                        }}
                        title="Send by Email"
                      >
                        <Mail size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {payslips.length === 0 && !isLoading && (
          <div className="empty-state">
            <FileText size={48} />
            <h3>No payslips found</h3>
            <p>Generate payslips for this fiscal period to get started.</p>
            <button
              className="action-button primary"
              onClick={generateAllPayslips}
              disabled={isGenerating}
            >
              Generate Payslips
            </button>
          </div>
        )}
      </div>

      {/* Summary */}
      <div className="payslips-summary">
        <div className="summary-item">
          <Users size={20} />
          <div>
            <div className="summary-value">{payslips.length}</div>
            <div className="summary-label">Total Payslips</div>
          </div>
        </div>
        <div className="summary-item">
          <CheckCircle size={20} />
          <div>
            <div className="summary-value">{payslips.filter(p => p.status === 'GENERATED').length}</div>
            <div className="summary-label">Generated</div>
          </div>
        </div>
        <div className="summary-item">
          <Download size={20} />
          <div>
            <div className="summary-value">{payslips.filter(p => p.status === 'DOWNLOADED').length}</div>
            <div className="summary-label">Downloaded</div>
          </div>
        </div>
        <div className="summary-item">
          <Mail size={20} />
          <div>
            <div className="summary-value">{payslips.filter(p => p.status === 'SENT').length}</div>
            <div className="summary-label">Sent by Email</div>
          </div>
        </div>
      </div>

      {/* Debug: Last download info */}
      {lastDownloadHash && (
        <div className="download-debug">
          <div className="debug-item">
            <div className="debug-label">Last Download ({lastDownloadSource}):</div>
            <div className="debug-value">{`sha256:${lastDownloadHash.slice(0, 16)}...`}</div>
          </div>
        </div>
      )}

      {/* Instructions */}
      <div className="instructions-notice">
        <AlertCircle size={20} />
        <div>
          <h4>Payslip Distribution</h4>
          <p>
            Payslips are generated using SARS-compliant templates and can be distributed via PDF download or email.
            All distributions are logged for audit purposes.
          </p>
        </div>
      </div>
    </div>
  );
}