import { useState, useCallback, useEffect } from 'react';
import { X, Save, Edit } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import type { Employee, EmployeeUpdateRequest } from '../types/api';

interface EmployeeEditModalProps {
  employee: Employee;
  isOpen: boolean;
  onClose: () => void;
  onEmployeeUpdated: (employee: Employee) => void;
}

interface EmployeeFormData {
  employeeNumber: string;
  title: string;
  firstName: string;
  secondName: string;
  lastName: string;
  email: string;
  phone: string;
  position: string;
  department: string;
  hireDate: string;
  isActive: boolean;
  addressLine1: string;
  addressLine2: string;
  city: string;
  province: string;
  postalCode: string;
  country: string;
  bankName: string;
  accountHolderName: string;
  accountNumber: string;
  branchCode: string;
  accountType: string;
  employmentType: 'PERMANENT' | 'CONTRACT' | 'TEMPORARY';
  salaryType: 'MONTHLY' | 'WEEKLY' | 'HOURLY' | 'DAILY';
  basicSalary: number;
  overtimeRate: number;
  taxNumber: string;
  taxRebateCode: string;
  uifNumber: string;
  medicalAidNumber: string;
  pensionFundNumber: string;
}

export default function EmployeeEditModal({
  employee,
  isOpen,
  onClose,
  onEmployeeUpdated
}: EmployeeEditModalProps) {
  const api = useApi();
  const [formData, setFormData] = useState<EmployeeFormData>({
    employeeNumber: '',
    title: '',
    firstName: '',
    secondName: '',
    lastName: '',
    email: '',
    phone: '',
    position: '',
    department: '',
    hireDate: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    province: '',
    postalCode: '',
    country: 'South Africa',
    bankName: '',
    accountHolderName: '',
    accountNumber: '',
    branchCode: '',
    accountType: '',
    employmentType: 'PERMANENT',
    salaryType: 'MONTHLY',
    basicSalary: 0,
    overtimeRate: 0,
    taxNumber: '',
    taxRebateCode: '',
    uifNumber: '',
    medicalAidNumber: '',
    pensionFundNumber: '',
    isActive: true, // Add default value to prevent undefined.toString() error
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Populate form data when employee changes
  useEffect(() => {
    if (employee && isOpen) {
      setFormData({
        employeeNumber: employee.employeeNumber || '',
        title: employee.title || '',
        firstName: employee.firstName || '',
        secondName: employee.secondName || '',
        lastName: employee.lastName || '',
        email: employee.email || '',
        phone: employee.phone || '',
        position: employee.position || '',
        department: employee.department || '',
        hireDate: employee.hireDate || '',
        addressLine1: employee.addressLine1 || '',
        addressLine2: employee.addressLine2 || '',
        city: employee.city || '',
        province: employee.province || '',
        postalCode: employee.postalCode || '',
        country: employee.country || 'South Africa',
        bankName: employee.bankName || '',
        accountHolderName: employee.accountHolderName || '',
        accountNumber: employee.accountNumber || '',
        branchCode: employee.branchCode || '',
        accountType: employee.accountType || '',
        employmentType: employee.employmentType || 'PERMANENT',
        salaryType: employee.salaryType || 'MONTHLY',
        basicSalary: employee.basicSalary || 0,
        overtimeRate: employee.overtimeRate || 0,
        taxNumber: employee.taxNumber || '',
        taxRebateCode: employee.taxRebateCode || '',
        uifNumber: employee.uifNumber || '',
        medicalAidNumber: employee.medicalAidNumber || '',
        pensionFundNumber: employee.pensionFundNumber || '',
        isActive: employee.active || true, // Changed from isActive to active to match API response
      });
      setError(null);
    }
  }, [employee, isOpen]);

  const handleInputChange = useCallback((field: keyof EmployeeFormData) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
      const value = e.target.type === 'number' ? Number(e.target.value) : e.target.value;
      setFormData(prev => ({ ...prev, [field]: value }));
    }, []);

  const validateForm = useCallback((): string | null => {
    if (!formData.employeeNumber.trim()) return 'Employee number is required';
    if (!formData.firstName.trim()) return 'First name is required';
    if (!formData.lastName.trim()) return 'Last name is required';
    if (!formData.position.trim()) return 'Position is required';
    if (!formData.hireDate) return 'Hire date is required';

    // Validate South African tax number (10-13 digits)
    if (formData.taxNumber && !/^\d{10,13}$/.test(formData.taxNumber.replace(/\s/g, ''))) {
      return 'Tax number must be 10-13 digits';
    }

    // Validate phone number (South African format)
    if (formData.phone && !/^(\+27|0)[6-8][0-9]{8}$/.test(formData.phone.replace(/\s/g, ''))) {
      return 'Phone number must be a valid South African number';
    }

    // Validate email
    if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      return 'Email must be a valid email address';
    }

    return null;
  }, [formData]);

  const handleSave = useCallback(async () => {
    const validationError = validateForm();
    if (validationError) {
      setError(validationError);
      return;
    }

    setSaving(true);
    setError(null);

    try {
      // Prepare employee update data for API call
      const updateData: EmployeeUpdateRequest = {
        employeeCode: formData.employeeNumber,
        title: formData.title || undefined,
        firstName: formData.firstName,
        secondName: formData.secondName || undefined,
        lastName: formData.lastName,
        email: formData.email || undefined,
        phone: formData.phone || undefined,
        position: formData.position,
        department: formData.department || undefined,
        hireDate: formData.hireDate,
        addressLine1: formData.addressLine1 || undefined,
        addressLine2: formData.addressLine2 || undefined,
        city: formData.city || undefined,
        province: formData.province || undefined,
        postalCode: formData.postalCode || undefined,
        country: formData.country || undefined,
        bankName: formData.bankName || undefined,
        accountHolderName: formData.accountHolderName || undefined,
        accountNumber: formData.accountNumber || undefined,
        branchCode: formData.branchCode || undefined,
        accountType: formData.accountType || undefined,
        employmentType: formData.employmentType,
        salaryType: formData.salaryType,
        basicSalary: formData.basicSalary || undefined,
        overtimeRate: formData.overtimeRate || undefined,
        taxNumber: formData.taxNumber || undefined,
        taxRebateCode: formData.taxRebateCode || undefined,
        uifNumber: formData.uifNumber || undefined,
        medicalAidNumber: formData.medicalAidNumber || undefined,
        pensionFundNumber: formData.pensionFundNumber || undefined,
        isActive: formData.isActive,
      };

      const updatedEmployee = await api.updateEmployee(employee.id, updateData);
      onEmployeeUpdated(updatedEmployee);

      // Close modal
      onClose();
    } catch (err) {
      let message = 'Failed to update employee';
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
  }, [formData, employee.id, api, validateForm, onEmployeeUpdated, onClose]);

  const handleClose = useCallback(() => {
    if (!saving) {
      setError(null);
      onClose();
    }
  }, [saving, onClose]);

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div className="modal-content employee-edit-modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>
            <Edit size={20} />
            Edit Employee
          </h3>
          <button
            onClick={handleClose}
            className="close-button"
            disabled={saving}
          >
            <X size={20} />
          </button>
        </div>

        <div className="modal-body">
          {error && (
            <div className="error-message">
              {error}
            </div>
          )}

          <div className="form-grid">
            {/* Basic Information */}
            <div className="form-section">
              <h4>Basic Information</h4>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="employeeNumber">Employee Number *</label>
                  <input
                    id="employeeNumber"
                    name="employeeNumber"
                    type="text"
                    value={formData.employeeNumber}
                    onChange={handleInputChange('employeeNumber')}
                    placeholder="e.g., EMP001"
                    required
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="title">Title</label>
                  <select
                    id="title"
                    name="title"
                    value={formData.title}
                    onChange={handleInputChange('title')}
                    disabled={saving}
                  >
                    <option value="">Select title</option>
                    <option value="Mr">Mr</option>
                    <option value="Mrs">Mrs</option>
                    <option value="Ms">Ms</option>
                    <option value="Dr">Dr</option>
                    <option value="Prof">Prof</option>
                  </select>
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="firstName">First Name *</label>
                  <input
                    id="firstName"
                    name="firstName"
                    type="text"
                    value={formData.firstName}
                    onChange={handleInputChange('firstName')}
                    placeholder="Enter first name"
                    required
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="secondName">Second Name</label>
                  <input
                    id="secondName"
                    name="secondName"
                    type="text"
                    value={formData.secondName}
                    onChange={handleInputChange('secondName')}
                    placeholder="Enter second name"
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="lastName">Last Name *</label>
                  <input
                    id="lastName"
                    name="lastName"
                    type="text"
                    value={formData.lastName}
                    onChange={handleInputChange('lastName')}
                    placeholder="Enter last name"
                    required
                    disabled={saving}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="email">Email</label>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    value={formData.email}
                    onChange={handleInputChange('email')}
                    placeholder="employee@company.com"
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="phone">Phone</label>
                  <input
                    id="phone"
                    name="phone"
                    type="tel"
                    value={formData.phone}
                    onChange={handleInputChange('phone')}
                    placeholder="+27 71 234 5678"
                    disabled={saving}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="position">Position *</label>
                  <input
                    id="position"
                    name="position"
                    type="text"
                    value={formData.position}
                    onChange={handleInputChange('position')}
                    placeholder="e.g., Software Developer"
                    required
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="department">Department</label>
                  <input
                    id="department"
                    name="department"
                    type="text"
                    value={formData.department}
                    onChange={handleInputChange('department')}
                    placeholder="e.g., IT Department"
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="hireDate">Hire Date *</label>
                  <input
                    id="hireDate"
                    name="hireDate"
                    type="date"
                    value={formData.hireDate}
                    onChange={handleInputChange('hireDate')}
                    required
                    disabled={saving}
                  />
                </div>
              </div>
            </div>

            {/* Employment Details */}
            <div className="form-section">
              <h4>Employment Details</h4>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="employmentType">Employment Type</label>
                  <select
                    id="employmentType"
                    name="employmentType"
                    value={formData.employmentType}
                    onChange={handleInputChange('employmentType')}
                    disabled={saving}
                  >
                    <option value="PERMANENT">Permanent</option>
                    <option value="CONTRACT">Contract</option>
                    <option value="TEMPORARY">Temporary</option>
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="salaryType">Salary Type</label>
                  <select
                    id="salaryType"
                    name="salaryType"
                    value={formData.salaryType}
                    onChange={handleInputChange('salaryType')}
                    disabled={saving}
                  >
                    <option value="MONTHLY">Monthly</option>
                    <option value="WEEKLY">Weekly</option>
                    <option value="HOURLY">Hourly</option>
                    <option value="DAILY">Daily</option>
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="basicSalary">Basic Salary</label>
                  <input
                    id="basicSalary"
                    name="basicSalary"
                    type="number"
                    min="0"
                    step="0.01"
                    value={formData.basicSalary || ''}
                    onChange={handleInputChange('basicSalary')}
                    placeholder="0.00"
                    disabled={saving}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="overtimeRate">Overtime Rate</label>
                  <input
                    id="overtimeRate"
                    name="overtimeRate"
                    type="number"
                    min="0"
                    step="0.01"
                    value={formData.overtimeRate || ''}
                    onChange={handleInputChange('overtimeRate')}
                    placeholder="0.00"
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="isActive">Employee Status</label>
                  <select
                    id="isActive"
                    name="isActive"
                    value={formData.isActive.toString()}
                    onChange={(e) => setFormData(prev => ({ ...prev, isActive: e.target.value === 'true' }))}
                    disabled={saving}
                  >
                    <option value="true">Active</option>
                    <option value="false">Inactive</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Address Information */}
            <div className="form-section">
              <h4>Address Information</h4>

              <div className="form-row">
                <div className="form-group full-width">
                  <label htmlFor="addressLine1">Address Line 1</label>
                  <input
                    id="addressLine1"
                    name="addressLine1"
                    type="text"
                    value={formData.addressLine1}
                    onChange={handleInputChange('addressLine1')}
                    placeholder="Street address"
                    disabled={saving}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group full-width">
                  <label htmlFor="addressLine2">Address Line 2</label>
                  <input
                    id="addressLine2"
                    name="addressLine2"
                    type="text"
                    value={formData.addressLine2}
                    onChange={handleInputChange('addressLine2')}
                    placeholder="Apartment, suite, etc."
                    disabled={saving}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="city">City</label>
                  <input
                    id="city"
                    name="city"
                    type="text"
                    value={formData.city}
                    onChange={handleInputChange('city')}
                    placeholder="City"
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="province">Province</label>
                  <input
                    id="province"
                    name="province"
                    type="text"
                    value={formData.province}
                    onChange={handleInputChange('province')}
                    placeholder="Province"
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="postalCode">Postal Code</label>
                  <input
                    id="postalCode"
                    name="postalCode"
                    type="text"
                    value={formData.postalCode}
                    onChange={handleInputChange('postalCode')}
                    placeholder="Postal code"
                    disabled={saving}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="country">Country</label>
                  <input
                    id="country"
                    name="country"
                    type="text"
                    value={formData.country}
                    onChange={handleInputChange('country')}
                    placeholder="Country"
                    disabled={saving}
                  />
                </div>
              </div>
            </div>

            {/* Banking Information */}
            <div className="form-section">
              <h4>Banking Information</h4>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="bankName">Bank Name</label>
                  <input
                    id="bankName"
                    name="bankName"
                    type="text"
                    value={formData.bankName}
                    onChange={handleInputChange('bankName')}
                    placeholder="e.g., ABSA"
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="accountHolderName">Account Holder Name</label>
                  <input
                    id="accountHolderName"
                    name="accountHolderName"
                    type="text"
                    value={formData.accountHolderName}
                    onChange={handleInputChange('accountHolderName')}
                    placeholder="Account holder name"
                    disabled={saving}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="accountNumber">Account Number</label>
                  <input
                    id="accountNumber"
                    name="accountNumber"
                    type="text"
                    value={formData.accountNumber}
                    onChange={handleInputChange('accountNumber')}
                    placeholder="Bank account number"
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="branchCode">Branch Code</label>
                  <input
                    id="branchCode"
                    name="branchCode"
                    type="text"
                    value={formData.branchCode}
                    onChange={handleInputChange('branchCode')}
                    placeholder="Branch code"
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="accountType">Account Type</label>
                  <select
                    id="accountType"
                    name="accountType"
                    value={formData.accountType}
                    onChange={handleInputChange('accountType')}
                    disabled={saving}
                  >
                    <option value="">Select account type</option>
                    <option value="SAVINGS">Savings</option>
                    <option value="CHEQUE">Cheque/Current</option>
                    <option value="TRANSMISSION">Transmission</option>
                  </select>
                </div>
              </div>
            </div>

            {/* Tax and Compliance Information */}
            <div className="form-section">
              <h4>Tax & Compliance Information</h4>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="taxNumber">Tax Number</label>
                  <input
                    id="taxNumber"
                    name="taxNumber"
                    type="text"
                    value={formData.taxNumber}
                    onChange={handleInputChange('taxNumber')}
                    placeholder="10-13 digit tax number"
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="taxRebateCode">Tax Rebate Code</label>
                  <input
                    id="taxRebateCode"
                    name="taxRebateCode"
                    type="text"
                    value={formData.taxRebateCode}
                    onChange={handleInputChange('taxRebateCode')}
                    placeholder="Tax rebate code"
                    disabled={saving}
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="uifNumber">UIF Number</label>
                  <input
                    id="uifNumber"
                    name="uifNumber"
                    type="text"
                    value={formData.uifNumber}
                    onChange={handleInputChange('uifNumber')}
                    placeholder="UIF number"
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="medicalAidNumber">Medical Aid Number</label>
                  <input
                    id="medicalAidNumber"
                    name="medicalAidNumber"
                    type="text"
                    value={formData.medicalAidNumber}
                    onChange={handleInputChange('medicalAidNumber')}
                    placeholder="Medical aid number"
                    disabled={saving}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="pensionFundNumber">Pension Fund Number</label>
                  <input
                    id="pensionFundNumber"
                    name="pensionFundNumber"
                    type="text"
                    value={formData.pensionFundNumber}
                    onChange={handleInputChange('pensionFundNumber')}
                    placeholder="Pension fund number"
                    disabled={saving}
                  />
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="modal-footer">
          <button
            onClick={handleClose}
            className="cancel-button"
            disabled={saving}
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            className="save-button primary"
            disabled={saving}
          >
            {saving ? (
              <>
                <div className="spinner small"></div>
                Updating...
              </>
            ) : (
              <>
                <Save size={16} />
                Update Employee
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}