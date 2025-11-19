import { useState, useEffect, useCallback } from 'react';
import { Building2, Phone, Mail } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import type { Company } from '../types/api';

interface CompaniesViewProps {
  onCompanySelect: (company: Company) => void;
}

export default function CompaniesView({ onCompanySelect }: CompaniesViewProps) {
  const [companies, setCompanies] = useState<Company[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const api = useApi();

  const loadCompanies = useCallback(async () => {
    try {
      setLoading(true);
      const data = await api.getCompanies();
      setCompanies(data);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load companies');
      console.error('Error loading companies:', err);
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    loadCompanies();
  }, [loadCompanies]);

  if (loading) {
    return (
      <div className="loading">
        <div className="spinner"></div>
        <p>Loading companies...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="error">
        <p>{error}</p>
        <button onClick={loadCompanies}>Retry</button>
      </div>
    );
  }

  return (
    <div className="companies-view">
      <div className="view-header">
        <h2>Companies</h2>
        <p>Select a company to view details and manage financial data</p>
      </div>

      <div className="companies-grid">
        {companies.map((company) => (
          <div key={company.id} className="company-card">
            <div className="company-header">
              <Building2 size={24} />
              <h3>{company.name}</h3>
            </div>

            <div className="company-details">
              <div className="detail-row">
                <span className="label">Registration:</span>
                <span className="value">{company.registrationNumber || 'Not provided'}</span>
              </div>

              <div className="detail-row">
                <span className="label">Tax Number:</span>
                <span className="value">{company.taxNumber || 'Not provided'}</span>
              </div>

              {company.address && (
                <div className="detail-row">
                  <span className="label">Address:</span>
                  <span className="value">{company.address}</span>
                </div>
              )}

              {company.contactEmail && (
                <div className="detail-row">
                  <Mail size={16} />
                  <span className="value">{company.contactEmail}</span>
                </div>
              )}

              {company.contactPhone && (
                <div className="detail-row">
                  <Phone size={16} />
                  <span className="value">{company.contactPhone}</span>
                </div>
              )}

              {company.logoPath && (
                <div className="detail-row">
                  <span className="label">Logo:</span>
                  <span className="value">{company.logoPath}</span>
                </div>
              )}

              {/* Banking Details Section */}
              {(company.bankName || company.accountNumber || company.accountType || company.branchCode) && (
                <>
                  <div className="detail-section">
                    <span className="section-label">📊 Banking Details</span>
                  </div>

                  {company.bankName && (
                    <div className="detail-row">
                      <span className="label">Bank Name:</span>
                      <span className="value">{company.bankName}</span>
                    </div>
                  )}

                  {company.accountNumber && (
                    <div className="detail-row">
                      <span className="label">Account Number:</span>
                      <span className="value">{company.accountNumber}</span>
                    </div>
                  )}

                  {company.accountType && (
                    <div className="detail-row">
                      <span className="label">Account Type:</span>
                      <span className="value">{company.accountType}</span>
                    </div>
                  )}

                  {company.branchCode && (
                    <div className="detail-row">
                      <span className="label">Branch Code:</span>
                      <span className="value">{company.branchCode}</span>
                    </div>
                  )}
                </>
              )}

              {/* VAT Registration Section */}
              <div className="detail-section">
                <span className="section-label">💰 VAT Registration</span>
              </div>

              <div className="detail-row">
                <span className="label">VAT Registered:</span>
                <span className={`badge ${company.vatRegistered ? 'success' : 'warning'}`}>
                  {company.vatRegistered ? 'Yes (15% VAT on invoices)' : 'No (0% VAT)'}
                </span>
              </div>

              <div className="detail-row">
                <span className="label">Created:</span>
                <span className="value">
                  {company.createdAt ? new Date(company.createdAt.replace(' ', 'T')).toLocaleDateString('en-ZA') : 'Not available'}
                </span>
              </div>
            </div>

            <button
              className="select-button"
              onClick={() => onCompanySelect(company)}
            >
              Select Company
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}