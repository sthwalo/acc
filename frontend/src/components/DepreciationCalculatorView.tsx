import { useState } from 'react';
import { Calculator, TrendingDown, DollarSign, Calendar, AlertCircle } from 'lucide-react';
import type { Company } from '../types/api';

interface DepreciationCalculatorViewProps {
  selectedCompany: Company;
}

interface DepreciationResult {
  year: number;
  beginningValue: number;
  depreciationExpense: number;
  accumulatedDepreciation: number;
  endingValue: number;
}

export default function DepreciationCalculatorView({ selectedCompany }: DepreciationCalculatorViewProps) {
  const [assetCost, setAssetCost] = useState<string>('');
  const [salvageValue, setSalvageValue] = useState<string>('0');
  const [usefulLife, setUsefulLife] = useState<string>('5');
  const [depreciationMethod, setDepreciationMethod] = useState<'straight-line' | 'declining-balance'>('straight-line');
  const [depreciationRate, setDepreciationRate] = useState<string>('20');
  const [results, setResults] = useState<DepreciationResult[]>([]);
  const [error, setError] = useState<string | null>(null);

  const calculateDepreciation = () => {
    setError(null);

    const cost = parseFloat(assetCost);
    const salvage = parseFloat(salvageValue);
    const life = parseInt(usefulLife);
    const rate = parseFloat(depreciationRate) / 100;

    if (!cost || cost <= 0) {
      setError('Please enter a valid asset cost greater than 0');
      return;
    }

    if (salvage < 0) {
      setError('Salvage value cannot be negative');
      return;
    }

    if (cost <= salvage) {
      setError('Asset cost must be greater than salvage value');
      return;
    }

    if (!life || life <= 0) {
      setError('Please enter a valid useful life greater than 0');
      return;
    }

    if (depreciationMethod === 'declining-balance' && (!rate || rate <= 0 || rate >= 1)) {
      setError('Please enter a valid depreciation rate between 1% and 99%');
      return;
    }

    const depreciationResults: DepreciationResult[] = [];
    let accumulatedDepreciation = 0;

    if (depreciationMethod === 'straight-line') {
      const annualDepreciation = (cost - salvage) / life;

      for (let year = 1; year <= life; year++) {
        const beginningValue = year === 1 ? cost : results[year - 2]?.endingValue || cost;
        accumulatedDepreciation += annualDepreciation;
        const endingValue = cost - accumulatedDepreciation;

        depreciationResults.push({
          year,
          beginningValue,
          depreciationExpense: annualDepreciation,
          accumulatedDepreciation,
          endingValue: Math.max(endingValue, salvage) // Don't go below salvage value
        });
      }
    } else if (depreciationMethod === 'declining-balance') {
      let currentValue = cost;

      for (let year = 1; year <= life; year++) {
        const beginningValue = currentValue;
        const depreciationExpense = beginningValue * rate;
        accumulatedDepreciation += depreciationExpense;
        currentValue = Math.max(beginningValue - depreciationExpense, salvage);

        depreciationResults.push({
          year,
          beginningValue,
          depreciationExpense,
          accumulatedDepreciation,
          endingValue: currentValue
        });

        // Stop if we've reached salvage value
        if (currentValue <= salvage) break;
      }
    }

    setResults(depreciationResults);
  };

  const resetCalculator = () => {
    setAssetCost('');
    setSalvageValue('0');
    setUsefulLife('5');
    setDepreciationMethod('straight-line');
    setDepreciationRate('20');
    setResults([]);
    setError(null);
  };

  return (
    <div className="depreciation-calculator-view">
      <div className="view-header">
        <h2>Depreciation Calculator</h2>
        <p>Calculate depreciation schedules for fixed assets of {selectedCompany?.name}</p>
      </div>

      {error && (
        <div className="alert alert-error">
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      <div className="calculator-content">
        {/* Input Form */}
        <div className="calculator-form">
          <h3>
            <Calculator size={20} />
            Asset Information
          </h3>

          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="assetCost">
                <DollarSign size={16} />
                Asset Cost (R)
              </label>
              <input
                id="assetCost"
                name="assetCost"
                type="number"
                value={assetCost}
                onChange={(e) => setAssetCost(e.target.value)}
                placeholder="Enter asset cost"
                min="0"
                step="0.01"
              />
            </div>

            <div className="form-group">
              <label htmlFor="salvageValue">
                <DollarSign size={16} />
                Salvage Value (R)
              </label>
              <input
                id="salvageValue"
                name="salvageValue"
                type="number"
                value={salvageValue}
                onChange={(e) => setSalvageValue(e.target.value)}
                placeholder="Enter salvage value"
                min="0"
                step="0.01"
              />
            </div>

            <div className="form-group">
              <label htmlFor="usefulLife">
                <Calendar size={16} />
                Useful Life (Years)
              </label>
              <input
                id="usefulLife"
                name="usefulLife"
                type="number"
                value={usefulLife}
                onChange={(e) => setUsefulLife(e.target.value)}
                placeholder="Enter useful life"
                min="1"
              />
            </div>

            <div className="form-group">
              <label htmlFor="depreciationMethod">Depreciation Method</label>
              <select
                id="depreciationMethod"
                name="depreciationMethod"
                value={depreciationMethod}
                onChange={(e) => setDepreciationMethod(e.target.value as 'straight-line' | 'declining-balance')}
              >
                <option value="straight-line">Straight-Line</option>
                <option value="declining-balance">Declining Balance</option>
              </select>
            </div>

            {depreciationMethod === 'declining-balance' && (
              <div className="form-group">
                <label htmlFor="depreciationRate">
                  <TrendingDown size={16} />
                  Depreciation Rate (%)
                </label>
                <input
                  id="depreciationRate"
                  name="depreciationRate"
                  type="number"
                  value={depreciationRate}
                  onChange={(e) => setDepreciationRate(e.target.value)}
                  placeholder="Enter depreciation rate"
                  min="1"
                  max="99"
                  step="0.1"
                />
              </div>
            )}
          </div>

          <div className="form-actions">
            <button className="action-button primary" onClick={calculateDepreciation}>
              <Calculator size={20} />
              Calculate Depreciation
            </button>
            <button className="action-button secondary" onClick={resetCalculator}>
              Reset
            </button>
          </div>
        </div>

        {/* Results Table */}
        {results.length > 0 && (
          <div className="depreciation-results">
            <h3>Depreciation Schedule</h3>
            <div className="results-table-container">
              <table className="depreciation-table">
                <thead>
                  <tr>
                    <th>Year</th>
                    <th>Beginning Value</th>
                    <th>Depreciation Expense</th>
                    <th>Accumulated Depreciation</th>
                    <th>Ending Value</th>
                  </tr>
                </thead>
                <tbody>
                  {results.map((result) => (
                    <tr key={result.year}>
                      <td>{result.year}</td>
                      <td>R {result.beginningValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                      <td>R {result.depreciationExpense.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                      <td>R {result.accumulatedDepreciation.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                      <td>R {result.endingValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Summary */}
            <div className="depreciation-summary">
              <h4>Summary</h4>
              <div className="summary-grid">
                <div className="summary-item">
                  <span className="label">Total Depreciation:</span>
                  <span className="value">
                    R {results[results.length - 1]?.accumulatedDepreciation.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </span>
                </div>
                <div className="summary-item">
                  <span className="label">Final Asset Value:</span>
                  <span className="value">
                    R {results[results.length - 1]?.endingValue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </span>
                </div>
                <div className="summary-item">
                  <span className="label">Method:</span>
                  <span className="value">
                    {depreciationMethod === 'straight-line' ? 'Straight-Line' : `Declining Balance (${depreciationRate}%)`}
                  </span>
                </div>
                <div className="summary-item">
                  <span className="label">Useful Life:</span>
                  <span className="value">{usefulLife} years</span>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}