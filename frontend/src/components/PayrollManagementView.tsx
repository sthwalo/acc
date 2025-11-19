import { useState, useEffect } from 'react';
import { Calculator, FileText, Users, AlertCircle, CheckCircle, Download } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import type { Company, FiscalPeriod } from '../types/api';

interface PayrollManagementViewProps {
  selectedCompany: Company;
}

interface PayrollPeriod {
  id: number;
  companyId: number;
  fiscalPeriodId: number;
  periodName: string;
  payDate: string;
  startDate: string;
  endDate: string;
  periodType: 'WEEKLY' | 'MONTHLY' | 'QUARTERLY';
  status: 'OPEN' | 'PROCESSED' | 'APPROVED' | 'PAID' | 'CLOSED';
  totalGrossPay: number;
  totalDeductions: number;
  totalNetPay: number;
  employeeCount: number;
  processedAt?: string;
  processedBy?: string;
  approvedAt?: string;
  approvedBy?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
}

export default function PayrollManagementView({ selectedCompany }: PayrollManagementViewProps) {
  const api = useApi();
  const [fiscalPeriods, setFiscalPeriods] = useState<FiscalPeriod[]>([]);
  const [payrollPeriods, setPayrollPeriods] = useState<PayrollPeriod[]>([]);
  const [selectedPeriod, setSelectedPeriod] = useState<FiscalPeriod | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

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
      setError(err instanceof Error ? err.message : 'Failed to load fiscal periods');
    }
  };

  const loadPayrollPeriods = async () => {
    if (!selectedPeriod) return;

    try {
      setIsLoading(true);
      setError(null);
      // Get payroll periods for the selected company
      const periods = await api.getPayrollPeriods(Number(selectedCompany.id));
      setPayrollPeriods(periods);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load payroll periods');
    } finally {
      setIsLoading(false);
    }
  };

  const processPayroll = async () => {
    if (!selectedPeriod) return;

    setIsProcessing(true);
    setError(null);
    setSuccess(null);

    try {
      // Process payroll for the selected fiscal period
      // Note: This might need to create a payroll period first if one doesn't exist
      await api.processPayroll(Number(selectedPeriod.id));
      setSuccess('Payroll processing completed successfully');
      loadPayrollPeriods(); // Refresh the list
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to process payroll');
    } finally {
      setIsProcessing(false);
    }
  };

  const generatePayslips = async (payrollPeriodId: number) => {
    try {
      setError(null);
      // Generate payslips for the payroll period
      await api.generatePayslips(payrollPeriodId);
      setSuccess('Payslips generated successfully');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate payslips');
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

      {error && (
        <div className="alert alert-error">
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="alert alert-success">
          <CheckCircle size={20} />
          <span>{success}</span>
        </div>
      )}

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

      {/* Payroll Actions */}
      <div className="payroll-actions">
        <button
          className="action-button primary"
          onClick={processPayroll}
          disabled={isProcessing || !selectedPeriod}
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
        <button className="action-button secondary">
          <Users size={20} />
          Manage Employees
        </button>
      </div>

      {/* Payroll Periods Table */}
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

        {payrollPeriods.length === 0 && !isLoading && (
          <div className="empty-state">
            <Calculator size={48} />
            <h3>No payroll periods found</h3>
            <p>Process payroll to create payroll periods for this fiscal period.</p>
          </div>
        )}
      </div>

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