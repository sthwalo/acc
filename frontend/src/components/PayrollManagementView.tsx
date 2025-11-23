import { useState, useEffect } from 'react';
import { Calculator, FileText, Users, AlertCircle, Download, Plus } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
import type { Company, FiscalPeriod, PayrollPeriod } from '../types/api';

interface PayrollManagementViewProps {
  selectedCompany: Company;
  onViewChange?: (view: 'companies' | 'fiscal-periods' | 'upload' | 'transactions' | 'generate-reports' | 'data-management' | 'payroll-management' | 'budget-management' | 'employee-management' | 'depreciation-calculator' | 'current-time' | 'system-logs') => void;
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

  useEffect(() => {
    loadFiscalPeriods();
  }, [selectedCompany.id]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (selectedPeriod) {
      loadPayrollPeriods();
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
      // In the unified model, FiscalPeriod IS the payroll period
      // Map FiscalPeriod to match PayrollPeriod interface expectations
      const payrollPeriod = {
        ...selectedPeriod,
        status: 'OPEN' as const, // Default status since FiscalPeriod doesn't have payrollStatus
        fiscalPeriodId: selectedPeriod.id, // For compatibility
        payDate: selectedPeriod.endDate, // Add required PayrollPeriod fields
        periodType: 'MONTHLY' as const,
        totalGrossPay: 0,
        totalDeductions: 0,
        totalNetPay: 0,
        employeeCount: 0,
        updatedAt: selectedPeriod.updatedAt || new Date().toISOString(), // Ensure it's not null
        createdBy: selectedPeriod.createdBy?.toString() // Convert number|null to string|undefined
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

  const processPayroll = async () => {
    if (!selectedPayrollPeriod) return;

    setIsProcessing(true);
    setError(null);
    setSuccess(null);

    try {
      // Process payroll for the selected payroll period
      await api.processPayroll(Number(selectedPayrollPeriod.id));
      setSuccess('Payroll processing completed successfully');
      loadPayrollPeriods(); // Refresh the list
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
        periodType: 'MONTHLY',
        payrollStatus: 'OPEN',
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
                          title="View details"
                        >
                          <FileText size={16} />
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

      {/* SARS Compliance Notice */}
      <div className="compliance-notice">
        <AlertCircle size={20} />
        <div>
          <h4>SARS Compliance</h4>
          <p>All payroll calculations comply with South African Revenue Service (SARS) regulations including PAYE, UIF, and SDL requirements.</p>
        </div>
      </div>
    </div>
  );
}