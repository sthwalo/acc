import { useState, useEffect } from 'react';
import { TrendingUp, Target, AlertTriangle, CheckCircle, Plus, Edit, Trash2 } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
import type { Company, FiscalPeriod, Budget, BudgetVariance } from '../types/api';

interface BudgetManagementViewProps {
  selectedCompany: Company;
}

export default function BudgetManagementView({ selectedCompany }: BudgetManagementViewProps) {
  const api = useApi();
  const [fiscalPeriods, setFiscalPeriods] = useState<FiscalPeriod[]>([]);
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [selectedPeriod, setSelectedPeriod] = useState<FiscalPeriod | null>(null);
  const [selectedBudget, setSelectedBudget] = useState<Budget | null>(null);
  const [budgetVariance, setBudgetVariance] = useState<BudgetVariance | null>(null);
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

  useEffect(() => {
    if (selectedBudget) {
      loadBudgetVariance();
    }
  }, [selectedBudget?.id]); // eslint-disable-line react-hooks/exhaustive-deps

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
        } else if (anyErr && typeof anyErr === 'object' && 'message' in anyErr && typeof anyErr.message === 'string') {
          message = anyErr.message;
        }
      } catch {
        // fallback below
      }
      setError(message);
    }
  };

  const loadBudgets = async () => {
    if (!selectedPeriod) return;

    try {
      setIsLoading(true);
      setError(null);
      // Get budgets for the selected fiscal period
      const budgetsData = await api.getBudgetsByFiscalPeriod(Number(selectedPeriod.id));
      setBudgets(budgetsData);
      if (budgetsData.length > 0 && !selectedBudget) {
        setSelectedBudget(budgetsData[0]);
      }
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to load budgets';
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
      setIsLoading(false);
    }
  };

  const loadBudgetVariance = async () => {
    if (!selectedBudget) return;

    try {
      setError(null);
      // Get budget variance analysis
      const varianceData = await api.getBudgetVariance(Number(selectedBudget.id));
      setBudgetVariance(varianceData);
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to load budget variance';
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
      setBudgetVariance(null);
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

      <ApiMessageBanner message={error} type="error" />

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
                    <h4>{budget.title}</h4>
                    <div className={`variance-indicator ${getVarianceColor(budget.totalRevenue - budget.totalExpenses)}`}>
                      {getVarianceIcon(budget.totalRevenue - budget.totalExpenses)}
                      <span>R {Math.abs(budget.totalRevenue - budget.totalExpenses).toLocaleString()}</span>
                    </div>
                  </div>
                  <div className="budget-summary">
                    <div className="summary-item">
                      <span className="label">Revenue:</span>
                      <span className="value">R {budget.totalRevenue.toLocaleString()}</span>
                    </div>
                    <div className="summary-item">
                      <span className="label">Expenses:</span>
                      <span className="value">R {budget.totalExpenses.toLocaleString()}</span>
                    </div>
                    <div className="summary-item">
                      <span className="label">Net:</span>
                      <span className={`value ${getVarianceColor(budget.totalRevenue - budget.totalExpenses)}`}>
                        R {(budget.totalRevenue - budget.totalExpenses).toLocaleString()}
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
              <h3>{selectedBudget.title}</h3>
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

            {budgetVariance && (
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
                    {budgetVariance.categories.map((category) => (
                      <tr key={category.categoryId}>
                        <td>{category.categoryName}</td>
                        <td>R {category.budgeted.toLocaleString()}</td>
                        <td>R {category.actual.toLocaleString()}</td>
                        <td className={getVarianceColor(category.variance)}>
                          R {category.variance.toLocaleString()}
                        </td>
                        <td className={getVarianceColor(category.variance)}>
                          {category.variancePercentage > 0 ? '+' : ''}{category.variancePercentage.toFixed(2)}%
                        </td>
                        <td>
                          <div className={`status-indicator ${getVarianceColor(category.variance)}`}>
                            {getVarianceIcon(category.variance)}
                            <span>
                              {category.variance > 0 ? 'Under Budget' :
                               category.variance < 0 ? 'Over Budget' : 'On Budget'}
                            </span>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr className="total-row">
                      <td><strong>Total</strong></td>
                      <td><strong>R {budgetVariance.totalBudgeted.toLocaleString()}</strong></td>
                      <td><strong>R {budgetVariance.totalActual.toLocaleString()}</strong></td>
                      <td className={getVarianceColor(budgetVariance.totalVariance)}>
                        <strong>R {budgetVariance.totalVariance.toLocaleString()}</strong>
                      </td>
                      <td className={getVarianceColor(budgetVariance.totalVariance)}>
                        <strong>{budgetVariance.variancePercentage.toFixed(2)}%</strong>
                      </td>
                      <td>
                        <div className={`status-indicator ${getVarianceColor(budgetVariance.totalVariance)}`}>
                          {getVarianceIcon(budgetVariance.totalVariance)}
                          <span>
                            {budgetVariance.totalVariance > 0 ? 'Under Budget' :
                             budgetVariance.totalVariance < 0 ? 'Over Budget' : 'On Budget'}
                          </span>
                        </div>
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            )}

            {!budgetVariance && selectedBudget && (
              <div className="loading-state">
                <div className="spinner medium"></div>
                <p>Loading budget variance analysis...</p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}