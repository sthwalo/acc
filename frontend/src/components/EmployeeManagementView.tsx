import { useState, useEffect, useCallback } from 'react';
import { Users, UserPlus, Edit, Trash2, Search, SortAsc, SortDesc } from 'lucide-react';
import { useApi } from '../hooks/useApi';
import ApiMessageBanner from './shared/ApiMessageBanner';
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
    let filtered = employees;

    // Filter by active status
    if (showActiveOnly) {
      filtered = filtered.filter(emp => emp.isActive);
    }

    // Filter by search term
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(emp =>
        emp.firstName.toLowerCase().includes(term) ||
        emp.lastName.toLowerCase().includes(term) ||
        emp.employeeNumber.toLowerCase().includes(term) ||
        emp.position.toLowerCase().includes(term) ||
        emp.department?.toLowerCase().includes(term) ||
        emp.email?.toLowerCase().includes(term)
      );
    }

    // Sort employees
    filtered.sort((a, b) => {
      let aValue: string;
      let bValue: string;

      switch (sortBy) {
        case 'name':
          aValue = `${a.lastName} ${a.firstName}`.toLowerCase();
          bValue = `${b.lastName} ${b.firstName}`.toLowerCase();
          break;
        case 'employeeNumber':
          aValue = a.employeeNumber.toLowerCase();
          bValue = b.employeeNumber.toLowerCase();
          break;
        case 'position':
          aValue = a.position.toLowerCase();
          bValue = b.position.toLowerCase();
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
              placeholder="Search employees..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={showActiveOnly}
              onChange={(e) => setShowActiveOnly(e.target.checked)}
            />
            Show active employees only
          </label>
        </div>

        <button className="action-button primary">
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
                      <button className="action-button edit" title="Edit employee">
                        <Edit size={16} />
                      </button>
                      <button className="action-button delete" title="Deactivate employee">
                        <Trash2 size={16} />
                      </button>
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
            <div className="summary-value">{employees.length}</div>
            <div className="summary-label">Total Employees</div>
          </div>
        </div>
        <div className="summary-item">
          <Users size={20} />
          <div>
            <div className="summary-value">{employees.filter(e => e.isActive).length}</div>
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
    </div>
  );
}