import { useState, useEffect, useCallback } from 'react';
import { Calendar } from 'lucide-react';
import { serviceRegistry } from '../services/ServiceRegistry';
import { ApiService } from '../services/ApiService';
import { formatDate } from '../utils/date';
import type { FiscalPeriod, Company } from '../types/api';

interface FiscalPeriodsViewProps {
  selectedCompany: Company;
  onFiscalPeriodSelect?: (fiscalPeriod: FiscalPeriod | null) => void;
}

export default function FiscalPeriodsView({ selectedCompany, onFiscalPeriodSelect }: FiscalPeriodsViewProps) {
  const [fiscalPeriods, setFiscalPeriods] = useState<FiscalPeriod[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadFiscalPeriods = useCallback(async () => {
    try {
      setLoading(true);
      const apiService = serviceRegistry.get<ApiService>('apiService');
      const data = await apiService.getFiscalPeriods(Number(selectedCompany.id));
      setFiscalPeriods(data);
      setError(null);
    } catch (err) {
      setError('Failed to load fiscal periods');
      console.error('Error loading fiscal periods:', err);
    } finally {
      setLoading(false);
    }
  }, [selectedCompany.id]);

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
      <div className="error">
        <p>{error}</p>
        <button onClick={loadFiscalPeriods}>Retry</button>
      </div>
    );
  }

  return (
    <div className="fiscal-periods-view">
      <div className="view-header">
        <h2>Fiscal Periods - {selectedCompany.name}</h2>
        <p>Financial periods and performance metrics</p>
      </div>

      <div className="fiscal-periods-grid">
        {fiscalPeriods.map((period) => (
          <div key={period.id} className="fiscal-period-card">
            <div className="period-header">
              <Calendar size={24} />
              <h3>{period.periodName}</h3>
              <span className={`status-badge ${!period.closed ? 'active' : 'inactive'}`}>
                {!period.closed ? 'Active' : 'Inactive'}
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
            </div>
          </div>
        ))}
      </div>

      {fiscalPeriods.length === 0 && (
        <div className="empty-state">
          <Calendar size={48} />
          <h3>No Fiscal Periods Found</h3>
          <p>No fiscal periods have been set up for this company yet.</p>
        </div>
      )}
    </div>
  );
}