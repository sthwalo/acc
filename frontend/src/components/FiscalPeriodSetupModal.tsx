import { useState, useCallback } from 'react';
import { Calendar, Save, X } from 'lucide-react';
import { serviceRegistry } from '../services/ServiceRegistry';
import { ApiService } from '../services/ApiService';
import ApiMessageBanner from './shared/ApiMessageBanner';
import type { FiscalPeriodSetupDTO, FiscalPeriod, Company } from '../types/api';

interface FiscalPeriodSetupModalProps {
  company: Company;
  onClose: () => void;
  onSuccess: (fiscalPeriod: FiscalPeriod) => void;
}

const MONTHS = [
  { value: 1, label: 'January' },
  { value: 2, label: 'February' },
  { value: 3, label: 'March' },
  { value: 4, label: 'April' },
  { value: 5, label: 'May' },
  { value: 6, label: 'June' },
  { value: 7, label: 'July' },
  { value: 8, label: 'August' },
  { value: 9, label: 'September' },
  { value: 10, label: 'October' },
  { value: 11, label: 'November' },
  { value: 12, label: 'December' }
];

export default function FiscalPeriodSetupModal({ company, onClose, onSuccess }: FiscalPeriodSetupModalProps) {
  const [formData, setFormData] = useState<FiscalPeriodSetupDTO>({
    yearEndMonth: 2, // Default to February (common for South African businesses)
    fiscalYear: new Date().getFullYear()
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const handleMonthChange = useCallback((e: React.ChangeEvent<HTMLSelectElement>) => {
    setFormData(prev => ({
      ...prev,
      yearEndMonth: parseInt(e.target.value)
    }));
  }, []);

  const handleYearChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const year = parseInt(e.target.value);
    if (year >= 2000 && year <= 2100) {
      setFormData(prev => ({
        ...prev,
        fiscalYear: year
      }));
    }
  }, []);

  const handleSubmit = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formData.yearEndMonth || !formData.fiscalYear) {
      setError('Please select both year-end month and fiscal year');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      setSuccess(null);

      const apiService = serviceRegistry.get<ApiService>('apiService');
      const fiscalPeriod = await apiService.fiscalPeriods.setupFiscalPeriod(company.id, formData);

      setSuccess(`Fiscal period "${fiscalPeriod.periodName}" has been set up successfully!`);
      setTimeout(() => {
        onSuccess(fiscalPeriod);
      }, 1500);

    } catch (err) {
      let message = 'Failed to set up fiscal period';
      try {
        const anyErr: unknown = err;
        if (anyErr && typeof anyErr === 'object' && 'response' in anyErr) {
          const axiosErr = anyErr as { response?: { data?: { message?: string } } };
          if (axiosErr.response?.data?.message) {
            message = axiosErr.response.data.message;
          }
        } else if (anyErr && typeof anyErr === 'object' && 'message' in anyErr && typeof anyErr.message === 'string') {
          message = anyErr.message;
        }
      } catch {
        // fallback below
      }
      setError(message);
    } finally {
      setSubmitting(false);
    }
  }, [formData, company.id, onSuccess]);

  const selectedMonth = MONTHS.find(month => month.value === formData.yearEndMonth);
  const previewPeriodName = selectedMonth ? `${selectedMonth.label} ${formData.fiscalYear}` : '';

  return (
    <div className="modal-overlay">
      <div className="modal-content fiscal-period-setup-modal">
        <div className="modal-header">
          <div className="modal-title">
            <Calendar size={24} />
            <h2>Set Up Fiscal Period</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="close-button"
            disabled={submitting}
          >
            <X size={20} />
          </button>
        </div>

        <div className="modal-body">
          <div className="setup-info">
            <p>
              <strong>{company.name}</strong> needs a fiscal period to get started.
              Choose your year-end month and fiscal year below.
            </p>
            <p className="help-text">
              The fiscal period will be calculated automatically based on your selection.
            </p>
          </div>

          {error && (
            <ApiMessageBanner
              message={error}
              type="error"
            />
          )}

          {success && (
            <ApiMessageBanner
              message={success}
              type="success"
            />
          )}

          <form onSubmit={handleSubmit} className="fiscal-period-form">
            <div className="form-grid">
              <div className="form-group">
                <label htmlFor="yearEndMonth">Year-End Month *</label>
                <select
                  id="yearEndMonth"
                  value={formData.yearEndMonth}
                  onChange={handleMonthChange}
                  disabled={submitting}
                  required
                >
                  {MONTHS.map(month => (
                    <option key={month.value} value={month.value}>
                      {month.label}
                    </option>
                  ))}
                </select>
                <small className="form-help">
                  The month when your financial year ends (e.g., February for tax year ending 28 Feb)
                </small>
              </div>

              <div className="form-group">
                <label htmlFor="fiscalYear">Fiscal Year *</label>
                <input
                  id="fiscalYear"
                  type="number"
                  min="2000"
                  max="2100"
                  value={formData.fiscalYear}
                  onChange={handleYearChange}
                  disabled={submitting}
                  required
                />
                <small className="form-help">
                  The year in which your fiscal period ends
                </small>
              </div>
            </div>

            {previewPeriodName && (
              <div className="period-preview">
                <h4>Period Preview</h4>
                <div className="preview-card">
                  <div className="preview-label">Year-End:</div>
                  <div className="preview-value">{previewPeriodName}</div>
                  <div className="preview-note">
                    Fiscal period will be calculated automatically from your selections
                  </div>
                </div>
              </div>
            )}
          </form>
        </div>

        <div className="modal-footer">
          <button
            type="button"
            onClick={onClose}
            className="cancel-button"
            disabled={submitting}
          >
            Cancel
          </button>
          <button
            type="submit"
            onClick={handleSubmit}
            className="save-button"
            disabled={submitting || !formData.yearEndMonth || !formData.fiscalYear}
          >
            <Save size={16} />
            {submitting ? 'Setting Up...' : 'Set Up Fiscal Period'}
          </button>
        </div>
      </div>
    </div>
  );
}