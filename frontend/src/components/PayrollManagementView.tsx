import { useState, useEffect } from 'react';
import { Calculator, FileText, Users, AlertCircle, CheckCircle, Download } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import type { Company, FiscalPeriod } from '../types/api';

interface PayrollManagementViewProps {
  selectedCompany: Company;
}

interface PayrollPeriod {
  id: number;
  name: string;
  start_date: string;
  end_date: string;
  status: 'draft' | 'processing' | 'completed';
  employee_count: number;
  total_gross: number;
  total_deductions: number;
  total_net: number;
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
      // TODO: Implement actual payroll periods API call
      // For now, simulate loading payroll periods
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Mock data for demonstration
      const mockPayrollPeriods: PayrollPeriod[] = [
        {
          id: 1,
          name: 'November 2025',
          start_date: '2025-11-01',
          end_date: '2025-11-30',
          status: 'completed',
          employee_count: 25,
          total_gross: 250000.00,
          total_deductions: 75000.00,
          total_net: 175000.00
        },
        {
          id: 2,
          name: 'October 2025',
          start_date: '2025-10-01',
          end_date: '2025-10-31',
          status: 'processing',
          employee_count: 24,
          total_gross: 240000.00,
          total_deductions: 72000.00,
          total_net: 168000.00
        }
      ];

      setPayrollPeriods(mockPayrollPeriods);
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
      // TODO: Implement actual payroll processing API call
      // For now, simulate the process
      await new Promise(resolve => setTimeout(resolve, 3000));

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
      // TODO: Implement actual payslip generation API call
      // For now, simulate the process
      console.log(`Generating payslips for payroll period ${payrollPeriodId}`);
      await new Promise(resolve => setTimeout(resolve, 2000));

      setSuccess('Payslips generated successfully');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate payslips');
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'completed': return 'success';
      case 'processing': return 'warning';
      case 'draft': return 'info';
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
                      <div className="period-name">{period.name}</div>
                      <div className="period-dates">
                        {new Date(period.start_date).toLocaleDateString()} - {new Date(period.end_date).toLocaleDateString()}
                      </div>
                    </div>
                  </td>
                  <td>
                    <span className={`status-badge ${getStatusColor(period.status)}`}>
                      {period.status.toUpperCase()}
                    </span>
                  </td>
                  <td>{period.employee_count}</td>
                  <td>R {period.total_gross.toLocaleString()}</td>
                  <td>R {period.total_deductions.toLocaleString()}</td>
                  <td>R {period.total_net.toLocaleString()}</td>
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