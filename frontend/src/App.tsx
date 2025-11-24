import { useState, useEffect, useCallback } from 'react';
import { Building2, Calendar, Receipt, Upload, Activity, Menu, X, LogOut, User, FileText, Database, Calculator, Clock, Settings } from 'lucide-react';
import { useApi } from './hooks/useApi';
import { useAuth } from './hooks/useAuth';
import type { Company, FiscalPeriod, HealthResponse } from './types/api';
import CompaniesView from './components/CompaniesView';
import FiscalPeriodsView from './components/FiscalPeriodsView';
import TransactionsView from './components/TransactionsView';
import UploadView from './components/UploadView';
import GenerateReportsView from './components/GenerateReportsView';
import DataManagementView from './components/DataManagementView';
import PayrollManagementView from './components/PayrollManagementView';
import BudgetManagementView from './components/BudgetManagementView';
import EmployeeManagementView from './components/EmployeeManagementView';
import DepreciationCalculatorView from './components/DepreciationCalculatorView';
import CurrentTimeView from './components/CurrentTimeView';
import SystemLogsView from './components/SystemLogsView';
import PayslipsView from './components/PayslipsView';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import './App.css';

type View = 'companies' | 'fiscal-periods' | 'upload' | 'transactions' | 'generate-reports' | 'data-management' | 'payroll-management' | 'budget-management' | 'employee-management' | 'depreciation-calculator' | 'current-time' | 'system-logs' | 'payslips';
type AuthView = 'login' | 'register';

function App() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();
  const api = useApi();
  const [currentView, setCurrentView] = useState<View>('companies');
  const [authView, setAuthView] = useState<AuthView>('login');
  const [selectedCompany, setSelectedCompany] = useState<Company | null>(null);
  const [selectedFiscalPeriod, setSelectedFiscalPeriod] = useState<FiscalPeriod | null>(null);
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const checkHealth = useCallback(async () => {
    try {
      const healthData = await api.getHealth();
      setHealth(healthData);
    } catch (error) {
      console.error('Failed to check API health:', error);
    }
  }, [api]);

  useEffect(() => {
    if (isAuthenticated) {
      checkHealth();
    }
  }, [isAuthenticated, checkHealth]);

  const handleCompanySelect = useCallback((company: Company) => {
    setSelectedCompany(company);
    setCurrentView('fiscal-periods');
  }, []);

  const handleFiscalPeriodSelect = useCallback((fiscalPeriod: FiscalPeriod | null) => {
    setSelectedFiscalPeriod(fiscalPeriod);
    if (fiscalPeriod) {
      setCurrentView('transactions');
    }
  }, []);

  const navigation = [
    { id: 'companies' as View, label: 'Company Setup', icon: Building2 },
    { id: 'fiscal-periods' as View, label: 'Fiscal Period Management', icon: Calendar },
    { id: 'upload' as View, label: 'Import Bank Statement', icon: Upload },
    { id: 'transactions' as View, label: 'View Imported Data', icon: Receipt },
    { id: 'generate-reports' as View, label: 'Generate Reports', icon: FileText },
    { id: 'data-management' as View, label: 'Data Management', icon: Database },
    { id: 'payroll-management' as View, label: 'Payroll Management', icon: Calculator },
    { id: 'employee-management' as View, label: 'Employee Management', icon: User },
    { id: 'budget-management' as View, label: 'Budget Management', icon: Settings },
    { id: 'depreciation-calculator' as View, label: 'Depreciation Calculator', icon: Calculator },
    { id: 'current-time' as View, label: 'Show current time', icon: Clock },
    { id: 'system-logs' as View, label: 'System Logs', icon: Activity },
  ];

  const renderView = () => {
    switch (currentView) {
      case 'companies':
        return <CompaniesView onCompanySelect={handleCompanySelect} />;
      case 'fiscal-periods':
        return selectedCompany ? (
          <FiscalPeriodsView 
            selectedCompany={selectedCompany} 
            onFiscalPeriodSelect={handleFiscalPeriodSelect}
          />
        ) : (
          <div className="empty-state">
            <Building2 size={48} />
            <h3>Please select a company first</h3>
            <p>Choose a company from the Company Setup view to manage fiscal periods.</p>
          </div>
        );
      case 'upload':
        return selectedCompany ? (
          selectedFiscalPeriod ? (
            <UploadView 
              selectedCompany={selectedCompany} 
              selectedFiscalPeriod={selectedFiscalPeriod}
            />
          ) : (
            <div className="empty-state">
              <Calendar size={48} />
              <h3>Please select a fiscal period first</h3>
              <p>Choose a fiscal period from the Fiscal Period Management view to import bank statements.</p>
            </div>
          )
        ) : (
          <div className="empty-state">
            <Building2 size={48} />
            <h3>Please select a company first</h3>
            <p>Choose a company from the Company Setup view to import bank statements.</p>
          </div>
        );
      case 'transactions':
        return selectedCompany ? (
          selectedFiscalPeriod ? (
            <TransactionsView 
              selectedCompany={selectedCompany} 
              selectedFiscalPeriod={selectedFiscalPeriod}
            />
          ) : (
            <div className="empty-state">
              <Calendar size={48} />
              <h3>Please select a fiscal period first</h3>
              <p>Choose a fiscal period from the Fiscal Period Management view to view imported data.</p>
            </div>
          )
        ) : (
          <div className="empty-state">
            <Building2 size={48} />
            <h3>Please select a company first</h3>
            <p>Choose a company from the Company Setup view to view imported data.</p>
          </div>
        );
      case 'generate-reports':
        return selectedCompany ? (
          <GenerateReportsView selectedCompany={selectedCompany} />
        ) : (
          <div className="empty-state">
            <Building2 size={48} />
            <h3>Please select a company first</h3>
            <p>Choose a company from the Company Setup view to generate reports.</p>
          </div>
        );
      case 'data-management':
        return selectedCompany ? (
          <DataManagementView selectedCompany={selectedCompany} />
        ) : (
          <div className="empty-state">
            <Building2 size={48} />
            <h3>Please select a company first</h3>
            <p>Choose a company from the Company Setup view to manage data.</p>
          </div>
        );
      case 'payroll-management':
        return selectedCompany ? (
          <PayrollManagementView 
            selectedCompany={selectedCompany} 
            onViewChange={setCurrentView}
          />
        ) : (
          <div className="empty-state">
            <Building2 size={48} />
            <h3>Please select a company first</h3>
            <p>Choose a company from the Company Setup view to manage payroll.</p>
          </div>
        );
      case 'payslips':
        return selectedCompany ? (
          selectedFiscalPeriod ? (
            <PayslipsView 
              selectedFiscalPeriod={selectedFiscalPeriod}
            />
          ) : (
            <div className="empty-state">
              <Calendar size={48} />
              <h3>Please select a fiscal period first</h3>
              <p>Choose a fiscal period from the Fiscal Period Management view to view payslips.</p>
            </div>
          )
        ) : (
          <div className="empty-state">
            <Building2 size={48} />
            <h3>Please select a company first</h3>
            <p>Choose a company from the Company Setup view to view payslips.</p>
          </div>
        );
      case 'employee-management':
        return selectedCompany ? (
          <EmployeeManagementView selectedCompany={selectedCompany} />
        ) : (
          <div className="empty-state">
            <Building2 size={48} />
            <h3>Please select a company first</h3>
            <p>Choose a company from the Company Setup view to manage employees.</p>
          </div>
        );
      case 'budget-management':
        return selectedCompany ? (
          <BudgetManagementView selectedCompany={selectedCompany} />
        ) : (
          <div className="empty-state">
            <Building2 size={48} />
            <h3>Please select a company first</h3>
            <p>Choose a company from the Company Setup view to manage budgets.</p>
          </div>
        );
      case 'depreciation-calculator':
        return selectedCompany ? (
          <DepreciationCalculatorView selectedCompany={selectedCompany} />
        ) : (
          <div className="empty-state">
            <Building2 size={48} />
            <h3>Please select a company first</h3>
            <p>Choose a company from the Company Setup view to use the depreciation calculator.</p>
          </div>
        );
      case 'current-time':
        return selectedCompany ? (
          <CurrentTimeView selectedCompany={selectedCompany} />
        ) : (
          <div className="empty-state">
            <Building2 size={48} />
            <h3>Please select a company first</h3>
            <p>Choose a company from the Company Setup view to view the current time.</p>
          </div>
        );
      case 'system-logs':
        return selectedCompany ? (
          <SystemLogsView selectedCompany={selectedCompany} />
        ) : (
          <div className="empty-state">
            <Building2 size={48} />
            <h3>Please select a company first</h3>
            <p>Choose a company from the Company Setup view to view system logs.</p>
          </div>
        );
      default:
        return <CompaniesView onCompanySelect={handleCompanySelect} />;
    }
  };

  const renderAuthView = () => {
    switch (authView) {
      case 'login':
        return <Login onSwitchToRegister={() => setAuthView('register')} />;
      case 'register':
        return <Register onSwitchToLogin={() => setAuthView('login')} />;
      default:
        return <Login onSwitchToRegister={() => setAuthView('register')} />;
    }
  };

  if (isLoading) {
    return (
      <div className="loading-screen">
        <div className="loading-content">
          <Building2 size={64} className="loading-icon" />
          <h2>Loading FIN...</h2>
          <div className="spinner large"></div>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return renderAuthView();
  }

  return (
    <div className="app">
      <header className="header">
        <div className="header-content">
          <div className="logo">
            <Building2 size={32} />
            <h1>FIN Financial Management</h1>
          </div>

          <div className="user-info">
            <div className="user-details">
              <User size={20} />
              <span>{user?.firstName} {user?.lastName}</span>
            </div>
            <button
              className="logout-button"
              onClick={logout}
              title="Logout"
            >
              <LogOut size={20} />
            </button>
          </div>

          <div className="health-status">
            {health && (
              <div className={`status ${health.database === 'connected' ? 'healthy' : 'unhealthy'}`}>
                <Activity size={16} />
                <span>API: {health.database === 'connected' ? 'Connected' : 'Disconnected'}</span>
              </div>
            )}
          </div>

          <button
            className="menu-toggle"
            onClick={() => setIsMenuOpen(!isMenuOpen)}
          >
            {isMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>
      </header>

      <nav className={`navigation ${isMenuOpen ? 'open' : ''}`}>
        {navigation.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            className={`nav-item ${currentView === id ? 'active' : ''}`}
            onClick={() => {
              setCurrentView(id);
              setIsMenuOpen(false);
            }}
          >
            <Icon size={20} />
            <span>{label}</span>
          </button>
        ))}
      </nav>

      <main className="main-content">
        {selectedCompany && currentView !== 'companies' && (
          <div className="company-header">
            <h2>{selectedCompany.name}</h2>
            <p className="company-details">
              Registration: {selectedCompany.registrationNumber} |
              Tax: {selectedCompany.taxNumber}
            </p>
          </div>
        )}
        {renderView()}
      </main>
    </div>
  );
}

export default App;
