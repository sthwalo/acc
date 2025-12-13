import { useState, useCallback, useEffect, memo } from 'react';
import { Building2, Phone, Mail, Plus, Edit, Trash2, Eye, ArrowLeft, Save } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
import type { Company } from '../types/api';

interface CompaniesViewProps {
  onCompanySelect: (company: Company) => void;
}

type MenuMode = 'list' | 'create' | 'view' | 'edit';

interface CompanyFormProps {
  isEdit: boolean;
  formData: Partial<Company>;
  onFormDataChange: (data: Partial<Company>) => void;
  onSave: () => void;
  onCancel: () => void;
  saving: boolean;
}

const CompanyForm = memo(({ isEdit, formData, onFormDataChange, onSave, onCancel, saving }: CompanyFormProps) => {
  const handleNameChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    onFormDataChange({ ...formData, name: e.target.value });
  }, [formData, onFormDataChange]);

  const handleRegistrationChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    onFormDataChange({ ...formData, registrationNumber: e.target.value });
  }, [formData, onFormDataChange]);

  const handleTaxNumberChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    onFormDataChange({ ...formData, taxNumber: e.target.value });
  }, [formData, onFormDataChange]);

  const handleAddressChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
    onFormDataChange({ ...formData, address: e.target.value });
  }, [formData, onFormDataChange]);

  const handleEmailChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    onFormDataChange({ ...formData, contactEmail: e.target.value });
  }, [formData, onFormDataChange]);

  const handlePhoneChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    onFormDataChange({ ...formData, contactPhone: e.target.value });
  }, [formData, onFormDataChange]);

  const handleBankNameChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    onFormDataChange({ ...formData, bankName: e.target.value });
  }, [formData, onFormDataChange]);

  const handleAccountNumberChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    onFormDataChange({ ...formData, accountNumber: e.target.value });
  }, [formData, onFormDataChange]);

  const handleAccountTypeChange = useCallback((e: React.ChangeEvent<HTMLSelectElement>) => {
    onFormDataChange({ ...formData, accountType: e.target.value });
  }, [formData, onFormDataChange]);

  const handleBranchCodeChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    onFormDataChange({ ...formData, branchCode: e.target.value });
  }, [formData, onFormDataChange]);

  const handleVatRegisteredChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    onFormDataChange({ ...formData, vatRegistered: e.target.checked });
  }, [formData, onFormDataChange]);

  return (
    <div className="company-form">
      <div className="form-header">
        <h3>{isEdit ? 'Edit Company' : 'Create New Company'}</h3>
      </div>

      <div className="form-grid">
        <div className="form-group">
          <label htmlFor="name">Company Name *</label>
          <input
            id="name"
            name="name"
            type="text"
            value={formData.name || ''}
            onChange={handleNameChange}
            placeholder="Enter company name"
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="registrationNumber">Registration Number</label>
          <input
            id="registrationNumber"
            name="registrationNumber"
            type="text"
            value={formData.registrationNumber || ''}
            onChange={handleRegistrationChange}
            placeholder="Enter registration number"
          />
        </div>

        <div className="form-group">
          <label htmlFor="taxNumber">Tax Number</label>
          <input
            id="taxNumber"
            name="taxNumber"
            type="text"
            value={formData.taxNumber || ''}
            onChange={handleTaxNumberChange}
            placeholder="Enter tax number"
          />
        </div>

        <div className="form-group">
          <label htmlFor="address">Address</label>
          <textarea
            id="address"
            name="address"
            value={formData.address || ''}
            onChange={handleAddressChange}
            placeholder="Enter company address"
            rows={3}
          />
        </div>

        <div className="form-group">
          <label htmlFor="contactEmail">Contact Email</label>
          <input
            id="contactEmail"
            name="contactEmail"
            type="email"
            value={formData.contactEmail || ''}
            onChange={handleEmailChange}
            placeholder="Enter contact email"
          />
        </div>

        <div className="form-group">
          <label htmlFor="contactPhone">Contact Phone</label>
          <input
            id="contactPhone"
            name="contactPhone"
            type="tel"
            value={formData.contactPhone || ''}
            onChange={handlePhoneChange}
            placeholder="Enter contact phone"
          />
        </div>

        <div className="form-group">
          <label htmlFor="bankName">Bank Name</label>
          <input
            id="bankName"
            name="bankName"
            type="text"
            value={formData.bankName || ''}
            onChange={handleBankNameChange}
            placeholder="Enter bank name"
          />
        </div>

        <div className="form-group">
          <label htmlFor="accountNumber">Account Number</label>
          <input
            id="accountNumber"
            name="accountNumber"
            type="text"
            value={formData.accountNumber || ''}
            onChange={handleAccountNumberChange}
            placeholder="Enter account number"
          />
        </div>

        <div className="form-group">
          <label htmlFor="accountType">Account Type</label>
          <select
            id="accountType"
            name="accountType"
            value={formData.accountType || ''}
            onChange={handleAccountTypeChange}
          >
            <option value="">Select account type</option>
            <option value="Cheque">Cheque</option>
            <option value="Savings">Savings</option>
            <option value="Business">Business</option>
            <option value="Transmission">Transmission</option>
          </select>
        </div>

        <div className="form-group">
          <label htmlFor="branchCode">Branch Code</label>
          <input
            id="branchCode"
            name="branchCode"
            type="text"
            value={formData.branchCode || ''}
            onChange={handleBranchCodeChange}
            placeholder="Enter branch code"
          />
        </div>

        <div className="form-group">
          <label htmlFor="vatRegistered" className="checkbox-label">
            <input
              id="vatRegistered"
              name="vatRegistered"
              type="checkbox"
              checked={formData.vatRegistered || false}
              onChange={handleVatRegisteredChange}
            />
            VAT Registered (15% VAT on invoices)
          </label>
        </div>
      </div>

      <div className="form-actions">
        <button
          type="button"
          onClick={onCancel}
          className="cancel-button"
          disabled={saving}
        >
          <ArrowLeft size={16} />
          Cancel
        </button>
        <button
          type="button"
          onClick={onSave}
          className="save-button"
          disabled={saving || !formData.name?.trim()}
        >
          <Save size={16} />
          {saving ? 'Saving...' : isEdit ? 'Update Company' : 'Create Company'}
        </button>
      </div>
    </div>
  );
});

export default function CompaniesView({ onCompanySelect }: CompaniesViewProps) {
  const [companies, setCompanies] = useState<Company[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [menuMode, setMenuMode] = useState<MenuMode>('list');
  const [selectedCompany, setSelectedCompany] = useState<Company | null>(null);
  const [formData, setFormData] = useState<Partial<Company>>({});
  const [saving, setSaving] = useState(false);
  const api = useApi();

  const loadCompanies = useCallback(async () => {
    try {
      setLoading(true);
      const data = await api.getCompanies();
      setCompanies(data);
      setError(null);
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to load companies';
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
      console.error('Error loading companies:', err);
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    loadCompanies();
  }, [loadCompanies]);

  const handleCreateCompany = useCallback(async () => {
    try {
      setSaving(true);
      const newCompany = await api.companies.createCompany(formData as Omit<Company, 'id'>);
      setCompanies(prev => [...prev, newCompany]);
      setMenuMode('list');
      setFormData({});
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to create company';
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
      setSaving(false);
    }
  }, [api, formData]);

  const handleUpdateCompany = useCallback(async () => {
    if (!selectedCompany) return;
    try {
      setSaving(true);
      const updatedCompany = await api.companies.updateCompany(selectedCompany.id, formData);
      setCompanies(prev => prev.map(c => c.id === selectedCompany.id ? updatedCompany : c));
      setMenuMode('list');
      setSelectedCompany(null);
      setFormData({});
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to update company';
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
      setSaving(false);
    }
  }, [api, selectedCompany, formData]);

  const handleDeleteCompany = async (company: Company) => {
    if (!confirm(`Are you sure you want to delete ${company.name}? This action cannot be undone.`)) {
      return;
    }
    try {
      await api.companies.deleteCompany(company.id);
      setCompanies(prev => prev.filter(c => c.id !== company.id));
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to delete company';
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

  const startCreate = () => {
    setFormData({});
    setMenuMode('create');
  };

  const startEdit = (company: Company) => {
    setSelectedCompany(company);
    setFormData({ ...company });
    setMenuMode('edit');
  };

  const startView = (company: Company) => {
    setSelectedCompany(company);
    setMenuMode('view');
  };

  const handleFormDataChange = useCallback((data: Partial<Company>) => {
    setFormData(data);
  }, []);

  const handleSave = useCallback(() => {
    if (menuMode === 'edit') {
      handleUpdateCompany();
    } else {
      handleCreateCompany();
    }
  }, [menuMode, handleCreateCompany, handleUpdateCompany]);

  const handleCancel = useCallback(() => {
    goBack();
  }, []);

  const goBack = () => {
    setMenuMode('list');
    setSelectedCompany(null);
    setFormData({});
    setError(null);
  };

  if (loading) {
    return (
      <div className="loading">
        <div className="spinner"></div>
        <p>Loading companies...</p>
      </div>
    );
  }

  if (error) {
    const isNoCompaniesError = error.includes('No companies found');

    return (
      <div className="companies-view">
        <div className="view-header">
          <div className="header-content">
            <div>
              <h2>Company Setup</h2>
              <p>Manage your companies - create, view, edit, or delete company records</p>
            </div>
            {isNoCompaniesError && (
              <button onClick={() => setMenuMode('create')} className="primary-button">
                Create Company
              </button>
            )}
          </div>
        </div>
        <ApiMessageBanner message={error} type="error" />
        <div className="error">
          <button onClick={loadCompanies}>Retry</button>
        </div>
      </div>
    );
  }

  // Show form for create/edit
  if (menuMode === 'create' || menuMode === 'edit') {
    return <CompanyForm 
      isEdit={menuMode === 'edit'} 
      formData={formData}
      onFormDataChange={handleFormDataChange}
      onSave={handleSave}
      onCancel={handleCancel}
      saving={saving}
    />;
  }

  // Show detailed view
  if (menuMode === 'view' && selectedCompany) {
    return (
      <div className="company-detail-view">
        <div className="view-header">
          <button onClick={goBack} className="back-button">
            <ArrowLeft size={16} />
            Back to Companies
          </button>
          <h2>{selectedCompany.name}</h2>
        </div>

        <div className="company-details-full">
          <div className="detail-section">
            <h3>Basic Information</h3>
            <div className="detail-grid">
              <div className="detail-item">
                <span className="label">Company Name:</span>
                <span className="value">{selectedCompany.name}</span>
              </div>
              <div className="detail-item">
                <span className="label">Registration Number:</span>
                <span className="value">{selectedCompany.registrationNumber || 'Not provided'}</span>
              </div>
              <div className="detail-item">
                <span className="label">Tax Number:</span>
                <span className="value">{selectedCompany.taxNumber || 'Not provided'}</span>
              </div>
              <div className="detail-item">
                <span className="label">Address:</span>
                <span className="value">{selectedCompany.address || 'Not provided'}</span>
              </div>
              <div className="detail-item">
                <span className="label">Contact Email:</span>
                <span className="value">{selectedCompany.contactEmail || 'Not provided'}</span>
              </div>
              <div className="detail-item">
                <span className="label">Contact Phone:</span>
                <span className="value">{selectedCompany.contactPhone || 'Not provided'}</span>
              </div>
            </div>
          </div>

          {(selectedCompany.bankName || selectedCompany.accountNumber) && (
            <div className="detail-section">
              <h3>Banking Information</h3>
              <div className="detail-grid">
                <div className="detail-item">
                  <span className="label">Bank Name:</span>
                  <span className="value">{selectedCompany.bankName || 'Not provided'}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Account Number:</span>
                  <span className="value">{selectedCompany.accountNumber || 'Not provided'}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Account Type:</span>
                  <span className="value">{selectedCompany.accountType || 'Not provided'}</span>
                </div>
                <div className="detail-item">
                  <span className="label">Branch Code:</span>
                  <span className="value">{selectedCompany.branchCode || 'Not provided'}</span>
                </div>
              </div>
            </div>
          )}

          <div className="detail-section">
            <h3>Tax Information</h3>
            <div className="detail-grid">
              <div className="detail-item">
                <span className="label">VAT Registered:</span>
                <span className={`badge ${selectedCompany.vatRegistered ? 'success' : 'warning'}`}>
                  {selectedCompany.vatRegistered ? 'Yes (15% VAT on invoices)' : 'No (0% VAT)'}
                </span>
              </div>
            </div>
          </div>

          <div className="detail-section">
            <h3>System Information</h3>
            <div className="detail-grid">
              <div className="detail-item">
                <span className="label">Created:</span>
                <span className="value">
                  {selectedCompany.createdAt ? new Date(selectedCompany.createdAt.replace(' ', 'T')).toLocaleString('en-ZA') : 'Not available'}
                </span>
              </div>
              <div className="detail-item">
                <span className="label">Last Updated:</span>
                <span className="value">
                  {selectedCompany.updatedAt ? new Date(selectedCompany.updatedAt.replace(' ', 'T')).toLocaleString('en-ZA') : 'Not available'}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="view-actions">
          <button
            onClick={() => startEdit(selectedCompany)}
            className="edit-button"
          >
            <Edit size={16} />
            Edit Company
          </button>
          <button
            onClick={() => handleDeleteCompany(selectedCompany)}
            className="delete-button"
          >
            <Trash2 size={16} />
            Delete Company
          </button>
          <button
            onClick={() => onCompanySelect(selectedCompany)}
            className="select-button"
          >
            Select Company
          </button>
        </div>
      </div>
    );
  }

  // Default list view with menu
  return (
    <div className="companies-view">
      <div className="view-header">
        <h2>Company Setup</h2>
        <p>Manage your companies - create, view, edit, or delete company records</p>
      </div>

      <div className="company-menu">
        <div className="menu-options">
          <button onClick={startCreate} className="menu-button create">
            <Plus size={20} />
            <span>Create New Company</span>
          </button>

          <div className="menu-divider">
            <span>OR</span>
          </div>

          <p className="menu-instruction">Select a company below to view details, edit, or delete:</p>
        </div>
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

              <div className="detail-row">
                <span className="label">VAT Registered:</span>
                <span className={`badge ${company.vatRegistered ? 'success' : 'warning'}`}>
                  {company.vatRegistered ? 'Yes' : 'No'}
                </span>
              </div>
            </div>

            <div className="card-actions">
              <button
                onClick={() => startView(company)}
                className="view-button"
              >
                <Eye size={16} />
                View Details
              </button>
              <button
                onClick={() => startEdit(company)}
                className="edit-button"
              >
                <Edit size={16} />
                Edit
              </button>
              <button
                onClick={() => handleDeleteCompany(company)}
                className="delete-button"
              >
                <Trash2 size={16} />
                Delete
              </button>
              <button
                className="select-button"
                onClick={() => onCompanySelect(company)}
              >
                Select Company
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}