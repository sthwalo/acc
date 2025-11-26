import { useState, useEffect } from 'react';
import { Calculator, FileText, Users, AlertCircle, Download, Plus, Settings, Search, Trash2, File } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
import type { Company, FiscalPeriod, PayrollPeriod, FiscalPeriodPayrollConfigRequest, FiscalPeriodPayrollConfigResponse, Employee } from '../types/api';

interface PayrollDocument {
  id: number;
  employeeId: number;
  periodId: number;
  fileName: string;
  fileData: string | null;
  documentType: 'PAYSLIP' | 'TAX_CERTIFICATE' | 'EMPLOYEE_CONTRACT' | 'BANK_STATEMENT' | 'OTHER';
  uploadDate: string;
}

interface PayrollManagementViewProps {
  selectedCompany: Company;
  onViewChange?: (view: 'companies' | 'fiscal-periods' | 'upload' | 'transactions' | 'generate-reports' | 'data-management' | 'payroll-management' | 'budget-management' | 'employee-management' | 'depreciation-calculator' | 'current-time' | 'system-logs' | 'payslips') => void;
}

export default function PayrollManagementView({ selectedCompany, onViewChange }: PayrollManagementViewProps) {
  const api = useApi();
  const [fiscalPeriods, setFiscalPeriods] = useState<FiscalPeriod[]>([]);
  const [payrollPeriods, setPayrollPeriods] = useState<PayrollPeriod[]>([]);
  const [selectedPeriod, setSelectedPeriod] = useState<FiscalPeriod | null>(null);
  const [selectedPayrollPeriod, setSelectedPayrollPeriod] = useState<PayrollPeriod | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [showCreatePeriod, setShowCreatePeriod] = useState(false);
  const [payrollConfig, setPayrollConfig] = useState<FiscalPeriodPayrollConfigResponse | null>(null);
  const [isConfigLoading, setIsConfigLoading] = useState(false);
  const [showConfigForm, setShowConfigForm] = useState(false);
  const [configFormData, setConfigFormData] = useState<FiscalPeriodPayrollConfigRequest>({
    payDate: '',
    periodType: 'MONTHLY',
    payrollStatus: 'OPEN'
  });

  // Document Management State
  const [documents, setDocuments] = useState<PayrollDocument[]>([]);
  const [isDocumentsLoading, setIsDocumentsLoading] = useState(false);
  const [documentSearchQuery, setDocumentSearchQuery] = useState('');
  const [selectedDocumentType, setSelectedDocumentType] = useState<string>('ALL');
  const [isUploadingDocument, setIsUploadingDocument] = useState(false);

  // Payroll Reports Modal State
  const [showPayrollReportsModal, setShowPayrollReportsModal] = useState(false);
  const [selectedPayrollPeriodForReports, setSelectedPayrollPeriodForReports] = useState<PayrollPeriod | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [isEmployeesLoading, setIsEmployeesLoading] = useState(false);
  const [selectedEmployeeForReport, setSelectedEmployeeForReport] = useState<Employee | null>(null);

  useEffect(() => {
    loadFiscalPeriods();
  }, [selectedCompany.id]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (selectedPeriod) {
      loadPayrollPeriods();
      loadPayrollConfig();
      loadDocuments();
    }
  }, [selectedPeriod?.id]); // eslint-disable-line react-hooks/exhaustive-deps

  const loadFiscalPeriods = async () => {
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
  };

  const loadPayrollPeriods = async () => {
    if (!selectedPeriod) return;

    try {
      setIsLoading(true);
      setError(null);
      // In the unified model, FiscalPeriod now contains payroll data
      // Use the selected fiscal period directly as it now includes payroll fields
      const payrollPeriod = {
        ...selectedPeriod,
        // Map payroll fields from FiscalPeriod to PayrollPeriod interface
        status: (selectedPeriod.payrollStatus || 'OPEN') as 'OPEN' | 'PROCESSED' | 'APPROVED' | 'PAID' | 'CLOSED',
        fiscalPeriodId: selectedPeriod.id,
        payDate: selectedPeriod.payDate || selectedPeriod.endDate,
        periodType: (selectedPeriod.periodType || 'MONTHLY') as 'WEEKLY' | 'MONTHLY' | 'QUARTERLY',
        totalGrossPay: selectedPeriod.totalGrossPay || 0,
        totalDeductions: selectedPeriod.totalDeductions || 0,
        totalNetPay: selectedPeriod.totalNetPay || 0,
        employeeCount: selectedPeriod.employeeCount || 0,
        processedAt: selectedPeriod.processedAt || undefined,
        processedBy: selectedPeriod.processedBy || undefined,
        approvedAt: selectedPeriod.approvedAt || undefined,
        approvedBy: selectedPeriod.approvedBy || undefined,
        updatedAt: selectedPeriod.updatedAt || new Date().toISOString(),
        createdBy: selectedPeriod.createdBy?.toString()
      };
      setPayrollPeriods([payrollPeriod]);
      if (!selectedPayrollPeriod) {
        setSelectedPayrollPeriod(payrollPeriod);
      }
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to load payroll periods';
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
  };

  const loadPayrollConfig = async () => {
    if (!selectedPeriod) return;

    try {
      setIsConfigLoading(true);
      setError(null);
      const config = await api.getFiscalPeriodPayrollConfig(selectedPeriod.id);
      setPayrollConfig(config);
      // Initialize form data with current config
      setConfigFormData({
        payDate: config.payDate || '',
        periodType: config.periodType,
        payrollStatus: config.payrollStatus
      });
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to load payroll configuration';
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
      setIsConfigLoading(false);
    }
  };

  const processPayroll = async () => {
    if (!selectedPayrollPeriod) return;

    setIsProcessing(true);
    setError(null);
    setSuccess(null);

    try {
      // Process payroll for the selected payroll period
      await api.processPayroll(Number(selectedPayrollPeriod.id));
      setSuccess('Payroll processing completed successfully');
      // Refresh both fiscal periods and payroll periods to show updated data
      await loadFiscalPeriods();
      await loadPayrollPeriods();
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to process payroll';
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
      setIsProcessing(false);
    }
  };

  const reprocessPayroll = async () => {
    if (!selectedPayrollPeriod) return;

    // Confirm reprocessing action
    const confirmed = window.confirm(
      'Are you sure you want to reprocess this payroll period? This will delete all existing payslips and recalculate them. This action cannot be undone.'
    );

    if (!confirmed) return;

    setIsProcessing(true);
    setError(null);
    setSuccess(null);

    try {
      // Reprocess payroll for the selected payroll period
      await api.reprocessPayroll(Number(selectedPayrollPeriod.id));
      setSuccess('Payroll reprocessing completed successfully');
      // Refresh both fiscal periods and payroll periods to show updated data
      await loadFiscalPeriods();
      await loadPayrollPeriods();
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to reprocess payroll';
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
      setIsProcessing(false);
    }
  };

  const generatePayslips = async (payrollPeriodId: number) => {
    try {
      setError(null);
      // Generate payslips for the payroll period (processing payroll generates payslips)
      await api.processPayroll(payrollPeriodId);
      setSuccess('Payslips generated successfully');
    } catch (err) {
      // Prefer structured API/axios error message where possible
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
    }
  };

  const createPayrollPeriod = async () => {
    if (!selectedPeriod) return;

    try {
      setError(null);
      setSuccess(null);
      
      // Create a new fiscal period for payroll (unified model)
      const newFiscalPeriod = {
        companyId: Number(selectedCompany.id),
        periodName: `${selectedPeriod.periodName} Payroll`,
        startDate: selectedPeriod.startDate,
        endDate: selectedPeriod.endDate,
        payDate: selectedPeriod.endDate,
        periodType: 'MONTHLY' as const,
        payrollStatus: 'OPEN' as const,
        closed: false
      };

      await api.createPayrollPeriod(newFiscalPeriod);
      setSuccess('Payroll period created successfully');
      setShowCreatePeriod(false);
      // Reload fiscal periods to include the new one
      loadFiscalPeriods();
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to create payroll period';
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
  };

  const updatePayrollConfig = async () => {
    if (!selectedPeriod) return;

    try {
      setError(null);
      setSuccess(null);
      setIsConfigLoading(true);

      const updatedConfig = await api.updateFiscalPeriodPayrollConfig(selectedPeriod.id, configFormData);
      setPayrollConfig(updatedConfig);
      setSuccess('Payroll configuration updated successfully');
      setShowConfigForm(false);
      // Refresh fiscal periods to show updated data
      await loadFiscalPeriods();
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to update payroll configuration';
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
      setIsConfigLoading(false);
    }
  };

  const resetPayrollConfig = async () => {
    if (!selectedPeriod) return;

    // Confirm reset action
    const confirmed = window.confirm(
      'Are you sure you want to reset the payroll configuration? This will clear pay date and reset status to OPEN. This action cannot be undone.'
    );

    if (!confirmed) return;

    try {
      setError(null);
      setSuccess(null);
      setIsConfigLoading(true);

      await api.resetFiscalPeriodPayrollConfig(selectedPeriod.id);
      setSuccess('Payroll configuration reset successfully');
      // Reload config and fiscal periods
      await loadPayrollConfig();
      await loadFiscalPeriods();
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to reset payroll configuration';
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
      setIsConfigLoading(false);
    }
  };

  // Document Management Functions
  const loadDocuments = async () => {
    try {
      setIsDocumentsLoading(true);
      setError(null);
      const docs = await api.getPayrollDocuments();
      setDocuments(docs);
    } catch (err) {
      let message = 'Failed to load documents';
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
      setIsDocumentsLoading(false);
    }
  };

  const uploadDocument = async (file: File, documentType: string) => {
    if (!selectedPeriod) return;

    setIsUploadingDocument(true);
    setError(null);
    setSuccess(null);

    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('employeeId', '9'); // Default employee for now
      formData.append('fiscalPeriodId', selectedPeriod.id.toString());
      formData.append('fileName', file.name);
      formData.append('documentType', documentType);

      await api.uploadPayrollDocument(formData);
      setSuccess('Document uploaded successfully');
      await loadDocuments(); // Refresh document list
    } catch (err) {
      let message = 'Failed to upload document';
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
      setIsUploadingDocument(false);
    }
  };

  const downloadDocument = async (documentId: number, fileName: string) => {
    try {
      setError(null);
      const blob = await api.downloadPayrollDocument(documentId);
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      
      setSuccess('Document downloaded successfully');
    } catch (err) {
      let message = 'Failed to download document';
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
  };

  const deleteDocument = async (documentId: number) => {
    const confirmed = window.confirm('Are you sure you want to delete this document? This action cannot be undone.');
    if (!confirmed) return;

    try {
      setError(null);
      setSuccess(null);
      await api.deletePayrollDocument(documentId);
      setSuccess('Document deleted successfully');
      await loadDocuments(); // Refresh document list
    } catch (err) {
      let message = 'Failed to delete document';
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
  };

  const searchDocuments = async () => {
    try {
      setIsDocumentsLoading(true);
      setError(null);
      const searchParams = {
        query: documentSearchQuery,
        type: selectedDocumentType === 'ALL' ? undefined : selectedDocumentType
      };
      const docs = await api.searchPayrollDocuments(searchParams);
      setDocuments(docs);
    } catch (err) {
      let message = 'Failed to search documents';
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
      setIsDocumentsLoading(false);
    }
  };

  // Payroll Reports Functions
  const loadEmployees = async () => {
    try {
      setIsEmployeesLoading(true);
      setError(null);
      const emps = await api.getEmployeesByCompany(Number(selectedCompany.id));
      setEmployees(emps);
    } catch (err) {
      let message = 'Failed to load employees';
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
      setIsEmployeesLoading(false);
    }
  };

  const openPayrollReportsModal = (payrollPeriod: PayrollPeriod) => {
    setSelectedPayrollPeriodForReports(payrollPeriod);
    setShowPayrollReportsModal(true);
    loadEmployees(); // Load employees for employee report selection
  };

  const generatePayrollSummaryReport = async () => {
    if (!selectedPayrollPeriodForReports) return;

    try {
      setError(null);
      const blob = await api.generatePayrollSummaryReport(selectedPayrollPeriodForReports.fiscalPeriodId);
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Payroll_Summary_${selectedPayrollPeriodForReports.periodName}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      
      setSuccess('Payroll Summary Report downloaded successfully');
    } catch (err) {
      let message = 'Failed to generate Payroll Summary Report';
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
  };

  const generateEmployeePayrollReport = async () => {
    if (!selectedPayrollPeriodForReports || !selectedEmployeeForReport) return;

    try {
      setError(null);
      const blob = await api.generateEmployeePayrollReport(selectedEmployeeForReport.id, selectedPayrollPeriodForReports.fiscalPeriodId);
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Employee_Payroll_${selectedEmployeeForReport.firstName}_${selectedEmployeeForReport.lastName}_${selectedPayrollPeriodForReports.periodName}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      
      setSuccess('Employee Payroll Report downloaded successfully');
    } catch (err) {
      let message = 'Failed to generate Employee Payroll Report';
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
  };

  const generateEMP201Report = async () => {
    if (!selectedPayrollPeriodForReports) return;

    try {
      setError(null);
      const blob = await api.generateEMP201Report(selectedPayrollPeriodForReports.fiscalPeriodId);
      
      // Create download link
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `EMP201_${selectedPayrollPeriodForReports.periodName}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      
      setSuccess('EMP 201 Report downloaded successfully');
    } catch (err) {
      let message = 'Failed to generate EMP 201 Report';
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
  };

  const getDocumentTypeIcon = (type: string) => {
    switch (type) {
      case 'PAYSLIP': return '💰';
      case 'TAX_CERTIFICATE': return '📄';
      case 'EMPLOYEE_CONTRACT': return '📋';
      case 'BANK_STATEMENT': return '🏦';
      default: return '📁';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'OPEN': return 'info';
      case 'PROCESSED': return 'warning';
      case 'APPROVED': return 'success';
      case 'PAID': return 'success';
      case 'CLOSED': return 'default';
      default: return 'default';
    }
  };

  return (
    <div className="payroll-management-view">
      <div className="view-header">
        <h2>Payroll Management</h2>
        <p>SARS-compliant payroll processing for {selectedCompany.name}</p>
      </div>

      <ApiMessageBanner message={error} type="error" />
      <ApiMessageBanner message={success} type="success" />

      {/* Fiscal Period Selection */}
      <div className="form-section">
        <h3>
          <Calculator size={20} />
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

      {/* Payroll Configuration Section */}
      {selectedPeriod && (
        <div className="form-section">
          <h3>
            <Settings size={20} />
            Payroll Configuration for {selectedPeriod.periodName}
          </h3>

          {isConfigLoading ? (
            <div className="loading-state">
              <div className="spinner small"></div>
              <p>Loading configuration...</p>
            </div>
          ) : payrollConfig ? (
            <div className="payroll-config-display">
              <div className="config-grid">
                <div className="config-item">
                  <label>Pay Date:</label>
                  <span>{payrollConfig.payDate ? new Date(payrollConfig.payDate).toLocaleDateString() : 'Not set'}</span>
                </div>
                <div className="config-item">
                  <label>Period Type:</label>
                  <span>{payrollConfig.periodType}</span>
                </div>
                <div className="config-item">
                  <label>Payroll Status:</label>
                  <span className={`status-badge ${getStatusColor(payrollConfig.payrollStatus)}`}>
                    {payrollConfig.payrollStatus}
                  </span>
                </div>
                <div className="config-item">
                  <label>Total Gross Pay:</label>
                  <span>R {payrollConfig.totalGrossPay.toLocaleString()}</span>
                </div>
                <div className="config-item">
                  <label>Total Deductions:</label>
                  <span>R {payrollConfig.totalDeductions.toLocaleString()}</span>
                </div>
                <div className="config-item">
                  <label>Total Net Pay:</label>
                  <span>R {payrollConfig.totalNetPay.toLocaleString()}</span>
                </div>
                <div className="config-item">
                  <label>Employee Count:</label>
                  <span>{payrollConfig.employeeCount}</span>
                </div>
                <div className="config-item">
                  <label>Closed:</label>
                  <span>{payrollConfig.closed ? 'Yes' : 'No'}</span>
                </div>
              </div>

              <div className="config-actions">
                <button
                  className="action-button secondary"
                  onClick={() => setShowConfigForm(true)}
                  disabled={payrollConfig.payrollStatus === 'PROCESSED'}
                >
                  <Settings size={16} />
                  Edit Configuration
                </button>
                <button
                  className="action-button warning"
                  onClick={resetPayrollConfig}
                  disabled={payrollConfig.payrollStatus === 'PROCESSED'}
                >
                  Reset Configuration
                </button>
              </div>
            </div>
          ) : (
            <div className="empty-state">
              <Settings size={48} />
              <h3>No payroll configuration found</h3>
              <p>Set up payroll configuration for this fiscal period.</p>
              <button
                className="action-button primary"
                onClick={() => setShowConfigForm(true)}
              >
                Configure Payroll
              </button>
            </div>
          )}

          {showConfigForm && (
            <div className="config-form-modal">
              <div className="modal-content">
                <h4>Edit Payroll Configuration</h4>
                <div className="form-group">
                  <label htmlFor="payDate">Pay Date:</label>
                  <input
                    id="payDate"
                    type="date"
                    value={configFormData.payDate}
                    onChange={(e) => setConfigFormData(prev => ({ ...prev, payDate: e.target.value }))}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="periodType">Period Type:</label>
                  <select
                    id="periodType"
                    value={configFormData.periodType}
                    onChange={(e) => setConfigFormData(prev => ({ ...prev, periodType: e.target.value as 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' }))}
                  >
                    <option value="WEEKLY">Weekly</option>
                    <option value="MONTHLY">Monthly</option>
                    <option value="QUARTERLY">Quarterly</option>
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="payrollStatus">Payroll Status:</label>
                  <select
                    id="payrollStatus"
                    value={configFormData.payrollStatus}
                    onChange={(e) => setConfigFormData(prev => ({ ...prev, payrollStatus: e.target.value as 'OPEN' | 'PROCESSED' | 'APPROVED' | 'PAID' | 'CLOSED' }))}
                  >
                    <option value="OPEN">Open</option>
                    <option value="PROCESSED">Processed</option>
                    <option value="APPROVED">Approved</option>
                    <option value="PAID">Paid</option>
                    <option value="CLOSED">Closed</option>
                  </select>
                </div>
                <div className="form-actions">
                  <button
                    className="action-button primary"
                    onClick={updatePayrollConfig}
                    disabled={isConfigLoading}
                  >
                    {isConfigLoading ? 'Updating...' : 'Update Configuration'}
                  </button>
                  <button
                    className="action-button secondary"
                    onClick={() => setShowConfigForm(false)}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Payroll Period Selection */}
      {selectedPeriod && (
        <div className="form-section">
          <h3>
            <FileText size={20} />
            Payroll Periods for {selectedPeriod.periodName}
          </h3>
          
          {payrollPeriods.length > 0 ? (
            <div className="period-selector">
              {payrollPeriods.map((period) => (
                <button
                  key={period.id}
                  className={`period-button ${selectedPayrollPeriod?.id === period.id ? 'active' : ''}`}
                  onClick={() => setSelectedPayrollPeriod(period)}
                >
                  <div className="period-name">{period.periodName}</div>
                  <div className="period-dates">
                    {new Date(period.startDate).toLocaleDateString()} - {new Date(period.endDate).toLocaleDateString()}
                  </div>
                  <div className="period-status">
                    <span className={`status-badge ${getStatusColor(period.status)}`}>
                      {period.status}
                    </span>
                  </div>
                </button>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <FileText size={48} />
              <h3>No payroll periods found</h3>
              <p>Create a payroll period to start processing payroll for this fiscal period.</p>
              <button 
                className="action-button primary"
                onClick={() => setShowCreatePeriod(true)}
              >
                Create Payroll Period
              </button>
            </div>
          )}

          {showCreatePeriod && (
            <div className="create-period-form">
              <h4>Create Payroll Period</h4>
              <p>This will create a payroll period for the selected fiscal period.</p>
              <div className="form-actions">
                <button 
                  className="action-button primary"
                  onClick={createPayrollPeriod}
                >
                  Create Period
                </button>
                <button 
                  className="action-button secondary"
                  onClick={() => setShowCreatePeriod(false)}
                >
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Payroll Actions */}
      <div className="payroll-actions">
        <button
          className="action-button primary"
          onClick={processPayroll}
          disabled={isProcessing || !selectedPayrollPeriod || selectedPayrollPeriod.status !== 'OPEN'}
        >
          {isProcessing ? (
            <>
              <div className="spinner small"></div>
              Processing Payroll...
            </>
          ) : (
            <>
              <Calculator size={20} />
              Process Payroll
            </>
          )}
        </button>
        <button
          className="action-button warning"
          onClick={reprocessPayroll}
          disabled={isProcessing || !selectedPayrollPeriod || selectedPayrollPeriod.status !== 'PROCESSED'}
        >
          {isProcessing ? (
            <>
              <div className="spinner small"></div>
              Reprocessing Payroll...
            </>
          ) : (
            <>
              <Calculator size={20} />
              Reprocess Payroll
            </>
          )}
        </button>
        <button 
          className="action-button secondary"
          onClick={() => onViewChange?.('employee-management')}
        >
          <Users size={20} />
          Manage Employees
        </button>
        {payrollPeriods.length === 0 && selectedPeriod && (
          <button 
            className="action-button secondary"
            onClick={() => setShowCreatePeriod(true)}
          >
            <Plus size={20} />
            Create Period
          </button>
        )}
      </div>

      {/* Payroll Periods Table */}
      {selectedPeriod && (
        <div className="payroll-periods-container">
          <h3>Payroll Periods</h3>

          {isLoading ? (
            <div className="loading-state">
              <div className="spinner large"></div>
              <p>Loading payroll periods...</p>
            </div>
          ) : (
            <table className="payroll-periods-table">
              <thead>
                <tr>
                  <th>Period</th>
                  <th>Status</th>
                  <th>Employees</th>
                  <th>Gross Pay</th>
                  <th>Deductions</th>
                  <th>Net Pay</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {payrollPeriods.map((period) => (
                  <tr key={period.id}>
                    <td>
                      <div className="period-info">
                        <div className="period-name">{period.periodName}</div>
                        <div className="period-dates">
                          {new Date(period.startDate).toLocaleDateString()} - {new Date(period.endDate).toLocaleDateString()}
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className={`status-badge ${getStatusColor(period.status)}`}>
                        {period.status}
                      </span>
                    </td>
                    <td>{period.employeeCount}</td>
                    <td>R {period.totalGrossPay.toLocaleString()}</td>
                    <td>R {period.totalDeductions.toLocaleString()}</td>
                    <td>R {period.totalNetPay.toLocaleString()}</td>
                    <td>
                      <div className="action-buttons">
                        <button
                          className="action-button view"
                          onClick={() => openPayrollReportsModal(period)}
                          title="View payroll reports"
                        >
                          <FileText size={16} />
                        </button>
                        <button
                          className="action-button payslips"
                          onClick={() => onViewChange?.('payslips')}
                          title="View payslips"
                          disabled={period.status !== 'PROCESSED'}
                        >
                          <Users size={16} />
                        </button>
                        <button
                          className="action-button download"
                          onClick={() => generatePayslips(period.id)}
                          title="Generate payslips"
                          disabled={period.status !== 'PROCESSED'}
                        >
                          <Download size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {payrollPeriods.length === 0 && !isLoading && selectedPeriod && (
            <div className="empty-state">
              <Calculator size={48} />
              <h3>No payroll periods found</h3>
              <p>Create a payroll period to start processing payroll for this fiscal period.</p>
              <button 
                className="action-button primary"
                onClick={() => setShowCreatePeriod(true)}
              >
                Create Payroll Period
              </button>
            </div>
          )}
        </div>
      )}

      {/* Document Management Section */}
      <div className="form-section">
        <h3>
          <File size={20} />
          Document Management
        </h3>

        {/* Document Upload */}
        <div className="document-upload-section">
          <h4>Upload Document</h4>
          <div className="upload-form">
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="documentFile">Select File:</label>
                <input
                  id="documentFile"
                  type="file"
                  accept=".pdf,.txt,.doc,.docx,.jpg,.jpeg,.png"
                  onChange={(e) => {
                    const file = e.target.files?.[0];
                    if (file) {
                      const documentType = window.prompt('Enter document type (PAYSLIP, TAX_CERTIFICATE, EMPLOYEE_CONTRACT, BANK_STATEMENT, OTHER):', 'PAYSLIP');
                      if (documentType) {
                        uploadDocument(file, documentType);
                      }
                    }
                  }}
                  disabled={isUploadingDocument}
                />
              </div>
            </div>
            {isUploadingDocument && (
              <div className="loading-state">
                <div className="spinner small"></div>
                <p>Uploading document...</p>
              </div>
            )}
          </div>
        </div>

        {/* Document Search and Filter */}
        <div className="document-search-section">
          <div className="search-controls">
            <div className="form-group">
              <label htmlFor="searchQuery">Search:</label>
              <input
                id="searchQuery"
                type="text"
                placeholder="Search documents..."
                value={documentSearchQuery}
                onChange={(e) => setDocumentSearchQuery(e.target.value)}
              />
            </div>
            <div className="form-group">
              <label htmlFor="documentTypeFilter">Filter by Type:</label>
              <select
                id="documentTypeFilter"
                value={selectedDocumentType}
                onChange={(e) => setSelectedDocumentType(e.target.value)}
              >
                <option value="ALL">All Types</option>
                <option value="PAYSLIP">Payslip</option>
                <option value="TAX_CERTIFICATE">Tax Certificate</option>
                <option value="EMPLOYEE_CONTRACT">Employee Contract</option>
                <option value="BANK_STATEMENT">Bank Statement</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <button
              className="action-button secondary"
              onClick={searchDocuments}
              disabled={isDocumentsLoading}
            >
              <Search size={16} />
              Search
            </button>
            <button
              className="action-button secondary"
              onClick={() => {
                setDocumentSearchQuery('');
                setSelectedDocumentType('ALL');
                loadDocuments();
              }}
            >
              Clear Filters
            </button>
          </div>
        </div>

        {/* Document List */}
        <div className="document-list-section">
          <h4>Documents ({documents.length})</h4>

          {isDocumentsLoading ? (
            <div className="loading-state">
              <div className="spinner large"></div>
              <p>Loading documents...</p>
            </div>
          ) : documents.length > 0 ? (
            <div className="document-grid">
              {documents.map((doc) => (
                <div key={doc.id} className="document-card">
                  <div className="document-icon">
                    {getDocumentTypeIcon(doc.documentType)}
                  </div>
                  <div className="document-info">
                    <h5>{doc.fileName}</h5>
                    <p className="document-type">{doc.documentType.replace('_', ' ')}</p>
                    <p className="document-date">
                      Uploaded: {new Date(doc.uploadDate).toLocaleDateString()}
                    </p>
                    <p className="document-meta">
                      Employee ID: {doc.employeeId} | Period ID: {doc.periodId}
                    </p>
                  </div>
                  <div className="document-actions">
                    <button
                      className="action-button view"
                      onClick={() => downloadDocument(doc.id, doc.fileName)}
                      title="Download document"
                    >
                      <Download size={16} />
                    </button>
                    <button
                      className="action-button delete"
                      onClick={() => deleteDocument(doc.id)}
                      title="Delete document"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <File size={48} />
              <h3>No documents found</h3>
              <p>Upload payroll documents to get started.</p>
            </div>
          )}
        </div>
      </div>

      {/* SARS Compliance Notice */}
      <div className="compliance-notice">
        <AlertCircle size={20} />
        <div>
          <h4>SARS Compliance</h4>
          <p>All payroll calculations comply with South African Revenue Service (SARS) regulations including PAYE, UIF, and SDL requirements.</p>
        </div>
      </div>

      {/* Payroll Reports Modal */}
      {showPayrollReportsModal && selectedPayrollPeriodForReports && (
        <div className="modal-overlay" onClick={() => setShowPayrollReportsModal(false)}>
          <div className="modal-content payroll-reports-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Payroll Reports - {selectedPayrollPeriodForReports.periodName}</h3>
              <button
                className="modal-close"
                onClick={() => setShowPayrollReportsModal(false)}
              >
                ×
              </button>
            </div>
            <div className="modal-body">
              <div className="reports-list">
                <div className="report-item">
                  <div className="report-info">
                    <h4>Payroll Summary Report</h4>
                    <p>Aggregates payroll data for the fiscal period (gross pay, deductions, net pay, employee count)</p>
                  </div>
                  <button
                    className="action-button primary"
                    onClick={generatePayrollSummaryReport}
                  >
                    <Download size={16} />
                    Generate PDF
                  </button>
                </div>

                <div className="report-item">
                  <div className="report-info">
                    <h4>Employee Payroll Report</h4>
                    <p>Detailed payroll history for specific employees within the fiscal period</p>
                    {isEmployeesLoading ? (
                      <div className="loading-state">
                        <div className="spinner small"></div>
                        <p>Loading employees...</p>
                      </div>
                    ) : (
                      <div className="form-group">
                        <label htmlFor="employeeSelect">Select Employee:</label>
                        <select
                          id="employeeSelect"
                          value={selectedEmployeeForReport?.id || ''}
                          onChange={(e) => {
                            const empId = parseInt(e.target.value);
                            const emp = employees.find(emp => emp.id === empId);
                            setSelectedEmployeeForReport(emp || null);
                          }}
                        >
                          <option value="">Select an employee...</option>
                          {employees.map((emp) => (
                            <option key={emp.id} value={emp.id}>
                              {emp.firstName} {emp.lastName} ({emp.employeeNumber})
                            </option>
                          ))}
                        </select>
                      </div>
                    )}
                  </div>
                  <button
                    className="action-button primary"
                    onClick={generateEmployeePayrollReport}
                    disabled={!selectedEmployeeForReport}
                  >
                    <Download size={16} />
                    Generate PDF
                  </button>
                </div>

                <div className="report-item">
                  <div className="report-info">
                    <h4>EMP 201 Report (SARS Tax Submission)</h4>
                    <p>SARS-compliant tax submission format required for South African tax authorities</p>
                  </div>
                  <button
                    className="action-button primary"
                    onClick={generateEMP201Report}
                  >
                    <Download size={16} />
                    Generate PDF
                  </button>
                </div>
              </div>

              <div className="modal-actions">
                <button
                  className="action-button secondary"
                  onClick={() => setShowPayrollReportsModal(false)}
                >
                  Back to Payroll Management
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}