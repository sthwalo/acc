import { useState, useEffect, useCallback } from 'react';
import { Calendar, Plus, Edit, Trash2, Lock } from 'lucide-react';
import { serviceRegistry } from '../services/ServiceRegistry';
import { ApiService } from '../services/ApiService';
import { formatDate } from '../utils/date';
import ApiMessageBanner from './shared/ApiMessageBanner';
import type { FiscalPeriod, Company } from '../types/api';

interface FiscalPeriodsViewProps {
  selectedCompany: Company;
  onFiscalPeriodSelect?: (fiscalPeriod: FiscalPeriod | null) => void;
}

interface FiscalPeriodFormData {
  periodName: string;
  startDate: string;
  endDate: string;
}

export default function FiscalPeriodsView({ selectedCompany, onFiscalPeriodSelect }: FiscalPeriodsViewProps) {
  const [fiscalPeriods, setFiscalPeriods] = useState<FiscalPeriod[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingPeriod, setEditingPeriod] = useState<FiscalPeriod | null>(null);
  const [formData, setFormData] = useState<FiscalPeriodFormData>({
    periodName: '',
    startDate: '',
    endDate: ''
  });
  const [submitting, setSubmitting] = useState(false);

  const loadFiscalPeriods = useCallback(async () => {
    try {
      setLoading(true);
      const apiService = serviceRegistry.get<ApiService>('apiService');
      const data = await apiService.fiscalPeriods.getFiscalPeriods(Number(selectedCompany.id));
      setFiscalPeriods(data);
      setError(null);
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
        } else if (anyErr && typeof anyErr === 'object' && 'message' in anyErr && typeof anyErr.message === 'string') {
          message = anyErr.message;
        }
      } catch {
        // fallback below
      }
      setError(message);
      console.error('Error loading fiscal periods:', err);
    } finally {
      setLoading(false);
    }
  }, [selectedCompany.id]);

  const handleCreateFiscalPeriod = async () => {
    if (!formData.periodName || !formData.startDate || !formData.endDate) {
      setError('Please fill in all required fields');
      return;
    }

    try {
      setSubmitting(true);
      const apiService = serviceRegistry.get<ApiService>('apiService');
      await apiService.fiscalPeriods.createFiscalPeriod(Number(selectedCompany.id), {
        companyId: Number(selectedCompany.id),
        periodName: formData.periodName,
        startDate: formData.startDate,
        endDate: formData.endDate,
        closed: false,
        createdAt: new Date().toISOString(),
        createdBy: null,
        updatedBy: null,
        updatedAt: null
      });
      setShowCreateForm(false);
      setFormData({ periodName: '', startDate: '', endDate: '' });
      await loadFiscalPeriods();
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to create fiscal period';
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
      console.error('Error creating fiscal period:', err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditFiscalPeriod = (period: FiscalPeriod) => {
    setEditingPeriod(period);
    setFormData({
      periodName: period.periodName,
      startDate: period.startDate,
      endDate: period.endDate
    });
  };

  const handleUpdateFiscalPeriod = async () => {
    if (!editingPeriod || !formData.periodName || !formData.startDate || !formData.endDate) {
      setError('Please fill in all required fields');
      return;
    }

    try {
      setSubmitting(true);
      const apiService = serviceRegistry.get<ApiService>('apiService');
      await apiService.fiscalPeriods.updateFiscalPeriod(editingPeriod.id, {
        periodName: formData.periodName,
        startDate: formData.startDate,
        endDate: formData.endDate
      });
      setEditingPeriod(null);
      setFormData({ periodName: '', startDate: '', endDate: '' });
      await loadFiscalPeriods();
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to update fiscal period';
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
      console.error('Error updating fiscal period:', err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteFiscalPeriod = async (period: FiscalPeriod) => {
    if (!confirm(`Are you sure you want to delete fiscal period "${period.periodName}"? This action cannot be undone.`)) {
      return;
    }

    try {
      const apiService = serviceRegistry.get<ApiService>('apiService');
      await apiService.fiscalPeriods.deleteFiscalPeriod(period.id);
      await loadFiscalPeriods();
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to delete fiscal period';
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
      console.error('Error deleting fiscal period:', err);
    }
  };

  const handleCloseFiscalPeriod = async (period: FiscalPeriod) => {
    if (!confirm(`Are you sure you want to close fiscal period "${period.periodName}"? This will prevent further modifications.`)) {
      return;
    }

    try {
      const apiService = serviceRegistry.get<ApiService>('apiService');
      await apiService.fiscalPeriods.closeFiscalPeriod(period.id);
      await loadFiscalPeriods();
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to close fiscal period';
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
      console.error('Error closing fiscal period:', err);
    }
  };

  const cancelEdit = () => {
    setEditingPeriod(null);
    setShowCreateForm(false);
    setFormData({ periodName: '', startDate: '', endDate: '' });
    setError(null);
  };

  useEffect(() => {
    if (selectedCompany) {
      loadFiscalPeriods();
    }
  }, [selectedCompany, loadFiscalPeriods]);

  if (loading) {
    return (
      <div className="loading">
        <div className="spinner"></div>
        <p>Loading fiscal periods...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="fiscal-periods-view">
        <div className="view-header">
          <h2>Fiscal Periods - {selectedCompany.name}</h2>
          <p>Financial periods and performance metrics</p>
        </div>
        <ApiMessageBanner message={error} type="error" />
        <div className="error">
          <button onClick={loadFiscalPeriods}>Retry</button>
        </div>
      </div>
    );
  }

  return (
    <div className="fiscal-periods-view">
      <div className="view-header">
        <h2>Fiscal Periods - {selectedCompany.name}</h2>
        <p>Financial periods and performance metrics</p>
        <button
          className="create-button"
          onClick={() => setShowCreateForm(true)}
          disabled={showCreateForm || editingPeriod !== null}
        >
          <Plus size={16} />
          Create Fiscal Period
        </button>
      </div>

      {(showCreateForm || editingPeriod) && (
        <div className="form-overlay">
          <div className="form-container">
            <h3>{editingPeriod ? 'Edit Fiscal Period' : 'Create New Fiscal Period'}</h3>
            <form onSubmit={(e) => {
              e.preventDefault();
              if (editingPeriod) {
                handleUpdateFiscalPeriod();
              } else {
                handleCreateFiscalPeriod();
              }
            }}>
              <div className="form-group">
                <label htmlFor="periodName">Period Name *</label>
                <input
                  id="periodName"
                  type="text"
                  value={formData.periodName}
                  onChange={(e) => setFormData(prev => ({ ...prev, periodName: e.target.value }))}
                  placeholder="e.g., FY2025-2026"
                  required
                  disabled={submitting}
                />
              </div>

              <div className="form-group">
                <label htmlFor="startDate">Start Date *</label>
                <input
                  id="startDate"
                  type="date"
                  value={formData.startDate}
                  onChange={(e) => setFormData(prev => ({ ...prev, startDate: e.target.value }))}
                  required
                  disabled={submitting}
                />
              </div>

              <div className="form-group">
                <label htmlFor="endDate">End Date *</label>
                <input
                  id="endDate"
                  type="date"
                  value={formData.endDate}
                  onChange={(e) => setFormData(prev => ({ ...prev, endDate: e.target.value }))}
                  required
                  disabled={submitting}
                />
              </div>

              <div className="form-actions">
                <button type="submit" disabled={submitting}>
                  {submitting ? 'Saving...' : (editingPeriod ? 'Update' : 'Create')}
                </button>
                <button type="button" onClick={cancelEdit} disabled={submitting}>
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="fiscal-periods-grid">
        {fiscalPeriods.map((period) => (
          <div key={period.id} className={`fiscal-period-card ${period.closed ? 'closed' : 'active'}`}>
            <div className="period-header">
              <Calendar size={24} />
              <h3>{period.periodName}</h3>
              <span className={`status-badge ${!period.closed ? 'active' : 'closed'}`}>
                {!period.closed ? 'Active' : 'Closed'}
              </span>
            </div>

            <div className="period-details">
              <div className="detail-row">
                <span className="label">Start Date:</span>
                <span className="value">{formatDate(period.startDate)}</span>
              </div>

              <div className="detail-row">
                <span className="label">End Date:</span>
                <span className="value">{formatDate(period.endDate)}</span>
              </div>

              <div className="detail-row">
                <span className="label">Created:</span>
                <span className="value">{formatDate(period.createdAt)}</span>
              </div>
            </div>

            <div className="period-actions">
              <button
                className="select-button"
                onClick={() => onFiscalPeriodSelect?.(period)}
              >
                Select Period
              </button>
              <button className="view-reports-button">
                View Reports
              </button>
              <button className="manage-transactions-button">
                Manage Transactions
              </button>
              {!period.closed && (
                <>
                  <button
                    className="edit-button"
                    onClick={() => handleEditFiscalPeriod(period)}
                    disabled={showCreateForm || editingPeriod !== null}
                  >
                    <Edit size={16} />
                    Edit
                  </button>
                  <button
                    className="close-button"
                    onClick={() => handleCloseFiscalPeriod(period)}
                  >
                    <Lock size={16} />
                    Close
                  </button>
                  <button
                    className="delete-button"
                    onClick={() => handleDeleteFiscalPeriod(period)}
                  >
                    <Trash2 size={16} />
                    Delete
                  </button>
                </>
              )}
              {period.closed && (
                <button className="closed-indicator" disabled>
                  <Lock size={16} />
                  Period Closed
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {fiscalPeriods.length === 0 && (
        <div className="empty-state">
          <Calendar size={48} />
          <h3>No Fiscal Periods Found</h3>
          <p>No fiscal periods have been set up for this company yet.</p>
          <button
            className="create-button"
            onClick={() => setShowCreateForm(true)}
          >
            <Plus size={16} />
            Create First Fiscal Period
          </button>
        </div>
      )}
    </div>
  );
}