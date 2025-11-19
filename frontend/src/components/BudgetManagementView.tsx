import { useState, useEffect } from 'react';
import { TrendingUp, Target, AlertTriangle, CheckCircle, Plus, Edit, Trash2 } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import type { Company, FiscalPeriod } from '../types/api';

interface BudgetManagementViewProps {
  selectedCompany: Company;
}

interface BudgetItem {
  id: number;
  category: string;
  budgeted_amount: number;
  actual_amount: number;
  variance: number;
  variance_percentage: number;
}

interface Budget {
  id: number;
  fiscal_period_id: number;
  name: string;
  total_budgeted: number;
  total_actual: number;
  total_variance: number;
  items: BudgetItem[];
}

export default function BudgetManagementView({ selectedCompany }: BudgetManagementViewProps) {
  const api = useApi();
  const [fiscalPeriods, setFiscalPeriods] = useState<FiscalPeriod[]>([]);
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [selectedPeriod, setSelectedPeriod] = useState<FiscalPeriod | null>(null);
  const [selectedBudget, setSelectedBudget] = useState<Budget | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadFiscalPeriods();
  }, [selectedCompany.id]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (selectedPeriod) {
      loadBudgets();
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

  const loadBudgets = async () => {
    if (!selectedPeriod) return;

    try {
      setIsLoading(true);
      setError(null);
      // TODO: Implement actual budgets API call
      // For now, simulate loading budgets
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Mock data for demonstration
      const mockBudgets: Budget[] = [
        {
          id: 1,
          fiscal_period_id: selectedPeriod.id,
          name: 'Operating Budget 2025',
          total_budgeted: 1200000.00,
          total_actual: 1150000.00,
          total_variance: 50000.00,
          items: [
            {
              id: 1,
              category: 'Salaries',
              budgeted_amount: 600000.00,
              actual_amount: 580000.00,
              variance: 20000.00,
              variance_percentage: 3.33
            },
            {
              id: 2,
              category: 'Rent',
              budgeted_amount: 120000.00,
              actual_amount: 125000.00,
              variance: -5000.00,
              variance_percentage: -4.17
            },
            {
              id: 3,
              category: 'Marketing',
              budgeted_amount: 80000.00,
              actual_amount: 75000.00,
              variance: 5000.00,
              variance_percentage: 6.25
            }
          ]
        }
      ];

      setBudgets(mockBudgets);
      if (mockBudgets.length > 0 && !selectedBudget) {
        setSelectedBudget(mockBudgets[0]);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load budgets');
    } finally {
      setIsLoading(false);
    }
  };

  const getVarianceColor = (variance: number) => {
    if (variance > 0) return 'positive';
    if (variance < 0) return 'negative';
    return 'neutral';
  };

  const getVarianceIcon = (variance: number) => {
    if (variance > 0) return <CheckCircle size={16} />;
    if (variance < 0) return <AlertTriangle size={16} />;
    return <Target size={16} />;
  };

  return (
    <div className="budget-management-view">
      <div className="view-header">
        <h2>Budget Management</h2>
        <p>Strategic planning, variance analysis, and cash flow forecasting for {selectedCompany.name}</p>
      </div>

      {error && (
        <div className="alert alert-error">
          <AlertTriangle size={20} />
          <span>{error}</span>
        </div>
      )}

      {/* Fiscal Period Selection */}
      <div className="form-section">
        <h3>
          <TrendingUp size={20} />
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

      {/* Budget Actions */}
      <div className="budget-actions">
        <button className="action-button primary">
          <Plus size={20} />
          Create New Budget
        </button>
        <button className="action-button secondary">
          <TrendingUp size={20} />
          Import Budget Data
        </button>
      </div>

      <div className="budget-content">
        {/* Budget List */}
        <div className="budget-list">
          <h3>Budgets</h3>
          {isLoading ? (
            <div className="loading-state">
              <div className="spinner large"></div>
              <p>Loading budgets...</p>
            </div>
          ) : (
            <div className="budget-cards">
              {budgets.map((budget) => (
                <div
                  key={budget.id}
                  className={`budget-card ${selectedBudget?.id === budget.id ? 'active' : ''}`}
                  onClick={() => setSelectedBudget(budget)}
                >
                  <div className="budget-header">
                    <h4>{budget.name}</h4>
                    <div className={`variance-indicator ${getVarianceColor(budget.total_variance)}`}>
                      {getVarianceIcon(budget.total_variance)}
                      <span>R {Math.abs(budget.total_variance).toLocaleString()}</span>
                    </div>
                  </div>
                  <div className="budget-summary">
                    <div className="summary-item">
                      <span className="label">Budgeted:</span>
                      <span className="value">R {budget.total_budgeted.toLocaleString()}</span>
                    </div>
                    <div className="summary-item">
                      <span className="label">Actual:</span>
                      <span className="value">R {budget.total_actual.toLocaleString()}</span>
                    </div>
                    <div className="summary-item">
                      <span className="label">Variance:</span>
                      <span className={`value ${getVarianceColor(budget.total_variance)}`}>
                        R {budget.total_variance.toLocaleString()}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {budgets.length === 0 && !isLoading && (
            <div className="empty-state">
              <Target size={48} />
              <h3>No budgets found</h3>
              <p>Create a new budget to start planning and tracking expenses.</p>
            </div>
          )}
        </div>

        {/* Budget Details */}
        {selectedBudget && (
          <div className="budget-details">
            <div className="details-header">
              <h3>{selectedBudget.name}</h3>
              <div className="header-actions">
                <button className="action-button edit">
                  <Edit size={16} />
                  Edit Budget
                </button>
                <button className="action-button delete">
                  <Trash2 size={16} />
                  Delete Budget
                </button>
              </div>
            </div>

            <div className="budget-items-table">
              <table>
                <thead>
                  <tr>
                    <th>Category</th>
                    <th>Budgeted</th>
                    <th>Actual</th>
                    <th>Variance</th>
                    <th>Variance %</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedBudget.items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.category}</td>
                      <td>R {item.budgeted_amount.toLocaleString()}</td>
                      <td>R {item.actual_amount.toLocaleString()}</td>
                      <td className={getVarianceColor(item.variance)}>
                        R {item.variance.toLocaleString()}
                      </td>
                      <td className={getVarianceColor(item.variance)}>
                        {item.variance_percentage > 0 ? '+' : ''}{item.variance_percentage.toFixed(2)}%
                      </td>
                      <td>
                        <div className={`status-indicator ${getVarianceColor(item.variance)}`}>
                          {getVarianceIcon(item.variance)}
                          <span>
                            {item.variance > 0 ? 'Under Budget' :
                             item.variance < 0 ? 'Over Budget' : 'On Budget'}
                          </span>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr className="total-row">
                    <td><strong>Total</strong></td>
                    <td><strong>R {selectedBudget.total_budgeted.toLocaleString()}</strong></td>
                    <td><strong>R {selectedBudget.total_actual.toLocaleString()}</strong></td>
                    <td className={getVarianceColor(selectedBudget.total_variance)}>
                      <strong>R {selectedBudget.total_variance.toLocaleString()}</strong>
                    </td>
                    <td className={getVarianceColor(selectedBudget.total_variance)}>
                      <strong>
                        {selectedBudget.total_budgeted > 0
                          ? `${((selectedBudget.total_variance / selectedBudget.total_budgeted) * 100).toFixed(2)}%`
                          : '0.00%'
                        }
                      </strong>
                    </td>
                    <td>
                      <div className={`status-indicator ${getVarianceColor(selectedBudget.total_variance)}`}>
                        {getVarianceIcon(selectedBudget.total_variance)}
                        <span>
                          {selectedBudget.total_variance > 0 ? 'Under Budget' :
                           selectedBudget.total_variance < 0 ? 'Over Budget' : 'On Budget'}
                        </span>
                      </div>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}