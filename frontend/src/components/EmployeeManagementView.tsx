import { useState, useEffect, useCallback } from 'react';
import { Users, UserPlus, Edit, Trash2, Search, SortAsc, SortDesc, AlertTriangle, UserCheck, UserX } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
import EmployeeCreateModal from './EmployeeCreateModal';
import EmployeeEditModal from './EmployeeEditModal';
import type { Company, Employee } from '../types/api';

interface EmployeeManagementViewProps {
  selectedCompany: Company;
}

export default function EmployeeManagementView({ selectedCompany }: EmployeeManagementViewProps) {
  const api = useApi();
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [filteredEmployees, setFilteredEmployees] = useState<Employee[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'name' | 'employeeNumber' | 'position' | 'department'>('name');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
  const [showActiveOnly, setShowActiveOnly] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null);
  const [showDeactivateConfirm, setShowDeactivateConfirm] = useState(false);
  const [showActivateConfirm, setShowActivateConfirm] = useState(false);
  const [showHardDeleteConfirm, setShowHardDeleteConfirm] = useState(false);
  const [employeeToDelete, setEmployeeToDelete] = useState<Employee | null>(null);

  const loadEmployees = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const allEmployees = await api.getEmployeesByCompany(Number(selectedCompany.id));
      setEmployees(allEmployees);
    } catch (err) {
      // Prefer structured API/axios error message where possible
      let message = 'Failed to load employees';
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
  }, [api, selectedCompany.id]);

  const filterAndSortEmployees = useCallback(() => {
    // Ensure employees is an array
    if (!Array.isArray(employees)) {
      setFilteredEmployees([]);
      return;
    }

    let filtered = [...employees]; // Create a copy to avoid mutating state

    // Filter by active status
    if (showActiveOnly) {
      filtered = filtered.filter(emp => emp && emp.isActive);
    }

    // Filter by search term
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(emp =>
        emp &&
        (emp.firstName?.toLowerCase().includes(term) ||
        emp.lastName?.toLowerCase().includes(term) ||
        emp.employeeNumber?.toLowerCase().includes(term) ||
        emp.position?.toLowerCase().includes(term) ||
        emp.department?.toLowerCase().includes(term) ||
        emp.email?.toLowerCase().includes(term))
      );
    }

    // Sort employees
    filtered.sort((a, b) => {
      // Ensure both employees exist
      if (!a || !b) return 0;

      let aValue: string;
      let bValue: string;

      switch (sortBy) {
        case 'name':
          aValue = `${a.lastName || ''} ${a.firstName || ''}`.toLowerCase();
          bValue = `${b.lastName || ''} ${b.firstName || ''}`.toLowerCase();
          break;
        case 'employeeNumber':
          aValue = (a.employeeNumber || '').toLowerCase();
          bValue = (b.employeeNumber || '').toLowerCase();
          break;
        case 'position':
          aValue = (a.position || '').toLowerCase();
          bValue = (b.position || '').toLowerCase();
          break;
        case 'department':
          aValue = (a.department || '').toLowerCase();
          bValue = (b.department || '').toLowerCase();
          break;
        default:
          return 0;
      }

      if (sortOrder === 'asc') {
        return aValue.localeCompare(bValue);
      } else {
        return bValue.localeCompare(aValue);
      }
    });

    setFilteredEmployees(filtered);
  }, [employees, searchTerm, sortBy, sortOrder, showActiveOnly]);

  useEffect(() => {
    loadEmployees();
  }, [loadEmployees]);

  useEffect(() => {
    filterAndSortEmployees();
  }, [filterAndSortEmployees]);

  const handleSort = (column: typeof sortBy) => {
    if (sortBy === column) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortOrder('asc');
    }
  };

  const getSortIcon = (column: typeof sortBy) => {
    if (sortBy !== column) return null;
    return sortOrder === 'asc' ? <SortAsc size={16} /> : <SortDesc size={16} />;
  };

  const getFullName = (employee: Employee) => {
    return `${employee.firstName} ${employee.lastName}`;
  };

  const getStatusBadge = (employee: Employee) => {
    return employee.isActive ? (
      <span className="status-badge success">Active</span>
    ) : (
      <span className="status-badge default">Inactive</span>
    );
  };

  const handleOpenCreateModal = useCallback(() => {
    setShowCreateModal(true);
  }, []);

  const handleCloseCreateModal = useCallback(() => {
    setShowCreateModal(false);
  }, []);

  const handleOpenEditModal = useCallback((employee: Employee) => {
    setSelectedEmployee(employee);
    setShowEditModal(true);
  }, []);

  const handleCloseEditModal = useCallback(() => {
    setShowEditModal(false);
    setSelectedEmployee(null);
  }, []);

  const handleEmployeeCreated = useCallback((newEmployee: Employee) => {
    // Add the new employee to the list
    setEmployees(prev => [...prev, newEmployee]);
    // Show success message
    setError(null);
  }, []);

  const handleEmployeeUpdated = useCallback((updatedEmployee: Employee) => {
    // Update the employee in the list
    setEmployees(prev => prev.map(emp =>
      emp.id === updatedEmployee.id ? updatedEmployee : emp
    ));
    // Show success message
    setError(null);
  }, []);

  const handleOpenDeactivateConfirm = useCallback((employee: Employee) => {
    setEmployeeToDelete(employee);
    setShowDeactivateConfirm(true);
  }, []);

  const handleOpenActivateConfirm = useCallback((employee: Employee) => {
    setEmployeeToDelete(employee);
    setShowActivateConfirm(true);
  }, []);

  const handleOpenHardDeleteConfirm = useCallback((employee: Employee) => {
    setEmployeeToDelete(employee);
    setShowHardDeleteConfirm(true);
  }, []);

  const handleCloseConfirmations = useCallback(() => {
    setShowDeactivateConfirm(false);
    setShowActivateConfirm(false);
    setShowHardDeleteConfirm(false);
    setEmployeeToDelete(null);
  }, []);

  const handleDeactivateEmployee = useCallback(async () => {
    if (!employeeToDelete) return;

    try {
      await api.deactivateEmployee(employeeToDelete.id);
      // Update the employee in the list
      setEmployees(prev => prev.map(emp =>
        emp.id === employeeToDelete.id ? { ...emp, isActive: false } : emp
      ));
      setError(null);
      handleCloseConfirmations();
    } catch (err) {
      let message = 'Failed to deactivate employee';
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
  }, [employeeToDelete, api, handleCloseConfirmations]);

  const handleActivateEmployee = useCallback(async () => {
    if (!employeeToDelete) return;

    try {
      await api.activateEmployee(employeeToDelete.id);
      // Update the employee in the list
      setEmployees(prev => prev.map(emp =>
        emp.id === employeeToDelete.id ? { ...emp, isActive: true } : emp
      ));
      setError(null);
      handleCloseConfirmations();
    } catch (err) {
      let message = 'Failed to activate employee';
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
  }, [employeeToDelete, api, handleCloseConfirmations]);

  const handleHardDeleteEmployee = useCallback(async () => {
    if (!employeeToDelete) return;

    try {
      await api.hardDeleteEmployee(employeeToDelete.id);
      // Remove the employee from the list
      setEmployees(prev => prev.filter(emp => emp.id !== employeeToDelete.id));
      setError(null);
      handleCloseConfirmations();
    } catch (err) {
      let message = 'Failed to permanently delete employee';
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
  }, [employeeToDelete, api, handleCloseConfirmations]);

  return (
    <div className="employee-management-view">
      <div className="view-header">
        <h2>Employee Management</h2>
        <p>Manage employees for {selectedCompany.name}</p>
      </div>

      <ApiMessageBanner message={error} type="error" />

      {/* Controls */}
      <div className="employee-controls">
        <div className="search-filter-section">
          <div className="search-input">
            <Search size={20} />
            <input
              type="text"
              id="employee-search"
              name="employee-search"
              placeholder="Search employees..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              autoComplete="off"
            />
          </div>
          <label className="checkbox-label">
            <input
              type="checkbox"
              id="show-active-only"
              name="show-active-only"
              checked={showActiveOnly}
              onChange={(e) => setShowActiveOnly(e.target.checked)}
            />
            Show active employees only
          </label>
        </div>

        <button className="action-button primary" onClick={handleOpenCreateModal}>
          <UserPlus size={20} />
          Add Employee
        </button>
      </div>

      {/* Employee Table */}
      <div className="employee-table-container">
        {isLoading ? (
          <div className="loading-state">
            <div className="spinner large"></div>
            <p>Loading employees...</p>
          </div>
        ) : (
          <table className="employee-table">
            <thead>
              <tr>
                <th onClick={() => handleSort('name')} className="sortable">
                  Name {getSortIcon('name')}
                </th>
                <th onClick={() => handleSort('employeeNumber')} className="sortable">
                  Employee # {getSortIcon('employeeNumber')}
                </th>
                <th onClick={() => handleSort('position')} className="sortable">
                  Position {getSortIcon('position')}
                </th>
                <th onClick={() => handleSort('department')} className="sortable">
                  Department {getSortIcon('department')}
                </th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredEmployees.map((employee) => (
                <tr key={employee.id}>
                  <td>
                    <div className="employee-info">
                      <div className="employee-name">{getFullName(employee)}</div>
                      <div className="employee-email">{employee.email}</div>
                    </div>
                  </td>
                  <td>{employee.employeeNumber}</td>
                  <td>{employee.position}</td>
                  <td>{employee.department || '-'}</td>
                  <td>{getStatusBadge(employee)}</td>
                  <td>
                    <div className="action-buttons">
                      <button className="action-button view" title="View details">
                        <Users size={16} />
                      </button>
                      <button className="action-button edit" title="Edit employee" onClick={() => handleOpenEditModal(employee)}>
                        <Edit size={16} />
                      </button>
                      {employee.isActive ? (
                        <button className="action-button deactivate" title="Deactivate employee" onClick={() => handleOpenDeactivateConfirm(employee)}>
                          <UserX size={16} />
                        </button>
                      ) : (
                        <>
                          <button className="action-button activate" title="Activate employee" onClick={() => handleOpenActivateConfirm(employee)}>
                            <UserCheck size={16} />
                          </button>
                          <button className="action-button hard-delete" title="Permanently delete employee" onClick={() => handleOpenHardDeleteConfirm(employee)}>
                            <Trash2 size={16} />
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {filteredEmployees.length === 0 && !isLoading && (
          <div className="empty-state">
            <Users size={48} />
            <h3>No employees found</h3>
            <p>
              {searchTerm || !showActiveOnly
                ? 'Try adjusting your search or filters.'
                : 'Add your first employee to get started with payroll processing.'}
            </p>
            {!searchTerm && showActiveOnly && (
              <button className="action-button primary">
                <UserPlus size={20} />
                Add First Employee
              </button>
            )}
          </div>
        )}
      </div>

      {/* Summary */}
      <div className="employee-summary">
        <div className="summary-item">
          <Users size={20} />
          <div>
            <div className="summary-value">{Array.isArray(employees) ? employees.length : 0}</div>
            <div className="summary-label">Total Employees</div>
          </div>
        </div>
        <div className="summary-item">
          <Users size={20} />
          <div>
            <div className="summary-value">{Array.isArray(employees) ? employees.filter(e => e && e.isActive).length : 0}</div>
            <div className="summary-label">Active Employees</div>
          </div>
        </div>
        <div className="summary-item">
          <Users size={20} />
          <div>
            <div className="summary-value">{filteredEmployees.length}</div>
            <div className="summary-label">Filtered Results</div>
          </div>
        </div>
      </div>

      {/* Employee Create Modal */}
      <EmployeeCreateModal
        company={selectedCompany}
        isOpen={showCreateModal}
        onClose={handleCloseCreateModal}
        onEmployeeCreated={handleEmployeeCreated}
      />

      {/* Employee Edit Modal */}
      {selectedEmployee && (
        <EmployeeEditModal
          employee={selectedEmployee}
          isOpen={showEditModal}
          onClose={handleCloseEditModal}
          onEmployeeUpdated={handleEmployeeUpdated}
        />
      )}

      {/* Deactivate Confirmation Modal */}
      {showDeactivateConfirm && employeeToDelete && (
        <div className="modal-overlay" onClick={handleCloseConfirmations}>
          <div className="modal-content confirmation-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>
                <AlertTriangle size={20} />
                Deactivate Employee
              </h3>
            </div>
            <div className="modal-body">
              <p>Are you sure you want to deactivate <strong>{employeeToDelete.firstName} {employeeToDelete.lastName}</strong>?</p>
              <p className="warning-text">
                This employee will be hidden from active lists but can be reactivated later.
                Payroll history will be preserved.
              </p>
            </div>
            <div className="modal-footer">
              <button onClick={handleCloseConfirmations} className="cancel-button">
                Cancel
              </button>
              <button onClick={handleDeactivateEmployee} className="delete-button">
                Deactivate
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Activate Confirmation Modal */}
      {showActivateConfirm && employeeToDelete && (
        <div className="modal-overlay" onClick={handleCloseConfirmations}>
          <div className="modal-content confirmation-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>
                <UserCheck size={20} />
                Activate Employee
              </h3>
            </div>
            <div className="modal-body">
              <p>Are you sure you want to reactivate <strong>{employeeToDelete.firstName} {employeeToDelete.lastName}</strong>?</p>
              <p className="info-text">
                This employee will be restored to active status and appear in payroll processing.
              </p>
            </div>
            <div className="modal-footer">
              <button onClick={handleCloseConfirmations} className="cancel-button">
                Cancel
              </button>
              <button onClick={handleActivateEmployee} className="primary-button">
                Activate
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Hard Delete Confirmation Modal */}
      {showHardDeleteConfirm && employeeToDelete && (
        <div className="modal-overlay" onClick={handleCloseConfirmations}>
          <div className="modal-content confirmation-modal danger-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>
                <AlertTriangle size={20} />
                Permanently Delete Employee
              </h3>
            </div>
            <div className="modal-body">
              <p className="danger-text">
                ⚠️ WARNING: This action cannot be undone!
              </p>
              <p>Are you sure you want to permanently delete <strong>{employeeToDelete.firstName} {employeeToDelete.lastName}</strong>?</p>
              <p className="warning-text">
                This will completely remove the employee from the database.
                Only employees with no payroll history can be hard deleted.
              </p>
            </div>
            <div className="modal-footer">
              <button onClick={handleCloseConfirmations} className="cancel-button">
                Cancel
              </button>
              <button onClick={handleHardDeleteEmployee} className="danger-button">
                Permanently Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}