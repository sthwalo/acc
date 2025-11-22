import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type {
  Company,
  FiscalPeriod,
  ApiTransaction,
  ApiResponse,
  HealthResponse,
  UploadResponse,
  User,
  Plan,
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  PlanSelection
} from '../types/api';

/**
 * Abstract base class for API services - Abstraction principle
 * Provides common HTTP functionality and error handling
 */
abstract class BaseApiService {
  protected client = axios.create({
    baseURL: import.meta.env.VITE_API_URL || '/api',
    timeout: 30000,
  });

  constructor() {
    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    // Request interceptor for auth token
    this.client.interceptors.request.use(
      (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
        const token = localStorage.getItem('auth_token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error: AxiosError) => {
        return Promise.reject(error);
      }
    );

    // Response interceptor for centralized error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        if (error.response?.status === 401) {
          // If this request had an Authorization header then this 401 is
          // likely due to an expired/invalid token, so clear and redirect.
          // However, if the request wasn't sent with Authorization (race/unauth),
          // we don't want to clear/redirect, because that can cause a login race
          // where another component may successfully log in simultaneously.
          const authHeader = (error.config as any)?.headers?.Authorization || (error.config as any)?.headers?.authorization;
          if (authHeader) {
            // Clear invalid token and redirect to login
            localStorage.removeItem('auth_token');
            window.location.href = '/login';
            throw new Error('Authentication failed. Please log in again.');
          }
        }

        if (error.response?.status === 403) {
          throw new Error('Access denied. You do not have permission to perform this action.');
        }

        if (error.response && error.response.status >= 500) {
          throw new Error('Server error. Please try again later or contact support.');
        }

        // Re-throw with original message for client errors
        throw error;
      }
    );
  }

  protected handleError(operation: string, error: unknown): never {
    if (error instanceof AxiosError) {
      if (error.response?.status === 404) {
        throw new Error(`${operation} not found. Please ensure the backend API is running.`);
      }
      throw new Error(`${operation} failed: ${error.response?.data?.message || error.message}`);
    }
    throw new Error(`${operation} failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
  }
}

/**
 * Authentication Service - Encapsulation principle
 * Handles all authentication-related operations
 */
class AuthApiService extends BaseApiService {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    try {
      const response = await this.client.post('/v1/auth/login', credentials);
      return response.data;
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 401) {
        throw new Error('Invalid email or password. Please check your credentials.');
      }
      this.handleError('Login', error);
    }
  }

  async register(userData: RegisterRequest): Promise<AuthResponse> {
    try {
      const response = await this.client.post('/v1/auth/register', userData);
      return response.data;
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 409) {
        throw new Error('An account with this email already exists.');
      }
      this.handleError('Registration', error);
    }
  }

  async getCurrentUser(): Promise<User> {
    try {
      const response = await this.client.get<ApiResponse<User>>('/v1/auth/me');
      return response.data.data;
    } catch (error) {
      this.handleError('Get current user', error);
    }
  }

  async logout(): Promise<void> {
    try {
      await this.client.post('/v1/auth/logout');
    } catch (error) {
      // Logout should not fail silently, but we don't want to block the UI
      console.warn('Logout API call failed, but proceeding with local logout:', error);
    }
  }

  async selectPlan(planSelection: PlanSelection): Promise<AuthResponse> {
    try {
      const response = await this.client.post('/v1/auth/select-plan', planSelection);
      return response.data;
    } catch (error) {
      this.handleError('Plan selection', error);
    }
  }
}

/**
 * Company Management Service - Encapsulation principle
 * Handles all company-related operations
 */
class CompanyApiService extends BaseApiService {
  async getCompanies(): Promise<Company[]> {
    try {
      const response = await this.client.get<ApiResponse<Company[]>>('/v1/companies');
      const companies = response.data.data;

      if (!companies || companies.length === 0) {
        throw new Error(
          'No companies found. Please create your first company to get started. ' +
          'Use the "Create Company" button above to add a new company.'
        );
      }

      return companies;
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 404) {
        throw new Error('Companies endpoint not found. Please ensure the backend API is running.');
      }
      this.handleError('Load companies', error);
    }
  }

  async getCompany(id: number): Promise<Company> {
    try {
      const response = await this.client.get<ApiResponse<Company>>(`/v1/companies/${id}`);
      return response.data.data;
    } catch (error) {
      this.handleError('Get company details', error);
    }
  }

  async createCompany(company: Omit<Company, 'id'>): Promise<Company> {
    try {
      const response = await this.client.post<ApiResponse<Company>>('/v1/companies', company);
      return response.data.data;
    } catch (error) {
      this.handleError('Create company', error);
    }
  }

  async updateCompany(id: number, company: Partial<Company>): Promise<Company> {
    try {
      const response = await this.client.put<ApiResponse<Company>>(`/v1/companies/${id}`, company);
      return response.data.data;
    } catch (error) {
      this.handleError('Update company', error);
    }
  }

  async deleteCompany(id: number): Promise<void> {
    try {
      await this.client.delete(`/v1/companies/${id}`);
    } catch (error) {
      this.handleError('Delete company', error);
    }
  }

  async selectCompany(id: number): Promise<void> {
    try {
      await this.client.get(`/v1/companies/${id}/select`);
    } catch (error) {
      this.handleError('Select company', error);
    }
  }
}

/**
 * Fiscal Period Management Service - Encapsulation principle
 * Handles all fiscal period-related operations
 */
class FiscalPeriodApiService extends BaseApiService {
  async getFiscalPeriods(companyId: number): Promise<FiscalPeriod[]> {
    try {
      const response = await this.client.get<ApiResponse<FiscalPeriod[]>>(`/v1/companies/${companyId}/fiscal-periods`);
      const periods = response.data.data;

      if (!periods || periods.length === 0) {
        throw new Error(
          `No fiscal periods found for company ${companyId}. Please create fiscal periods in the database. ` +
          'SQL: INSERT INTO fiscal_periods (company_id, name, start_date, end_date, is_active) VALUES (?, ?, ?, ?, ?)'
        );
      }

      return periods;
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 404) {
        throw new Error(`Company ${companyId} not found. Please verify the company exists.`);
      }
      this.handleError('Load fiscal periods', error);
    }
  }

  async getFiscalPeriod(companyId: number, id: number): Promise<FiscalPeriod> {
    try {
      const response = await this.client.get<ApiResponse<FiscalPeriod>>(`/v1/companies/${companyId}/fiscal-periods/${id}`);
      return response.data.data;
    } catch (error) {
      this.handleError('Get fiscal period details', error);
    }
  }

  async createFiscalPeriod(companyId: number, period: Omit<FiscalPeriod, 'id'>): Promise<FiscalPeriod> {
    try {
      const response = await this.client.post<ApiResponse<FiscalPeriod>>(`/v1/companies/${companyId}/fiscal-periods`, period);
      return response.data.data;
    } catch (error) {
      this.handleError('Create fiscal period', error);
    }
  }

  async updateFiscalPeriod(id: number, period: Partial<FiscalPeriod>): Promise<FiscalPeriod> {
    try {
      const response = await this.client.put<ApiResponse<FiscalPeriod>>(`/v1/companies/fiscal-periods/${id}`, period);
      return response.data.data;
    } catch (error) {
      this.handleError('Update fiscal period', error);
    }
  }

  async deleteFiscalPeriod(id: number): Promise<void> {
    try {
      await this.client.delete(`/v1/companies/fiscal-periods/${id}`);
    } catch (error) {
      this.handleError('Delete fiscal period', error);
    }
  }

  async closeFiscalPeriod(id: number): Promise<FiscalPeriod> {
    try {
      const response = await this.client.post<ApiResponse<FiscalPeriod>>(`/v1/companies/fiscal-periods/${id}/close`);
      return response.data.data;
    } catch (error) {
      this.handleError('Close fiscal period', error);
    }
  }

  async selectFiscalPeriod(companyId: number, id: number): Promise<void> {
    try {
      await this.client.get(`/v1/companies/${companyId}/fiscal-periods/${id}/select`);
    } catch (error) {
      this.handleError('Select fiscal period', error);
    }
  }
}

/**
 * Transaction Management Service - Encapsulation principle
 * Handles all transaction-related operations
 */
class TransactionApiService extends BaseApiService {
  async getTransactions(companyId: number, fiscalPeriodId: number): Promise<{ data: ApiTransaction[], count: number, company_id: number, timestamp: number, note: string }> {
    try {
      const response = await this.client.get(`/v1/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/transactions`);
      const result = response.data;

      if (!result.data || result.data.length === 0) {
        throw new Error(
          `No transactions found for company ${companyId}. Please ensure transaction data exists in the database. ` +
          'Check tables: transactions, bank_statements, transaction_classifications'
        );
      }

      return result;
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 404) {
        throw new Error(`Company ${companyId} not found or has no transactions.`);
      }
      this.handleError('Load transactions', error);
    }
  }

  async exportTransactionsToCsv(companyId: number, fiscalPeriodId: number): Promise<Blob> {
    try {
      const response = await this.client.get(`/v1/import/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/transactions/export/csv`, {
        responseType: 'blob',
      });
      return response.data;
    } catch (error) {
      this.handleError('Export transactions to CSV', error);
    }
  }

  async exportTransactionsToPdf(companyId: number, fiscalPeriodId: number): Promise<Blob> {
    try {
      const response = await this.client.get(`/v1/import/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/transactions/export/pdf`, {
        responseType: 'blob',
      });
      return response.data;
    } catch (error) {
      this.handleError('Export transactions to PDF', error);
    }
  }
}

/**
 * Report Generation Service - Encapsulation principle
 * Handles all financial report generation operations
 */
class ReportApiService extends BaseApiService {
  private async generateReport(
    companyId: number,
    fiscalPeriodId: number,
    reportType: string,
    format: string = 'text'
  ): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string, filename?: string }> {
    try {
      // Map frontend report types to backend endpoint paths
      const endpointMap: { [key: string]: string } = {
        'trial-balance': 'trial-balance',
        'income-statement': 'income-statement',
        'balance-sheet': 'balance-sheet',
        'cash-flow': 'financial', // Cash flow is under financial endpoint
        'general-ledger': 'general-ledger',
        'cashbook': 'cashbook',
        'audit-trail': 'audit-trail'
      };

      const endpoint = endpointMap[reportType];
      if (!endpoint) {
        throw new Error(`Unknown report type: ${reportType}`);
      }

      // For now, we'll get text format and handle downloads on the frontend
      // TODO: Update backend to support different formats directly
      const response = await this.client.get(`/v1/reports/${endpoint}/company/${companyId}/fiscal-period/${fiscalPeriodId}`, {
        params: { exportToFile: false }
      });

      // The backend returns plain text content directly
      const content = response.data;
      const filename = `${reportType.replace('-', '_')}_company_${companyId}_period_${fiscalPeriodId}.${format === 'text' ? 'txt' : format.toLowerCase()}`;

      return {
        reportType,
        companyId,
        fiscalPeriodId,
        format,
        content,
        filename
      };
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 404) {
        throw new Error(`${reportType.replace('-', ' ')} report not available. Please ensure financial data exists for company ${companyId} and fiscal period ${fiscalPeriodId}.`);
      }
      this.handleError(`Generate ${reportType.replace('-', ' ')} report`, error);
    }
  }

  async generateTrialBalance(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.generateReport(companyId, fiscalPeriodId, 'trial-balance', format);
  }

  async generateIncomeStatement(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.generateReport(companyId, fiscalPeriodId, 'income-statement', format);
  }

  async generateBalanceSheet(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.generateReport(companyId, fiscalPeriodId, 'balance-sheet', format);
  }

  async generateCashFlow(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.generateReport(companyId, fiscalPeriodId, 'cash-flow', format);
  }

  async generateGeneralLedger(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.generateReport(companyId, fiscalPeriodId, 'general-ledger', format);
  }

  async generateCashbook(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.generateReport(companyId, fiscalPeriodId, 'cashbook', format);
  }

  async generateAuditTrail(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.generateReport(companyId, fiscalPeriodId, 'audit-trail', format);
  }
}

/**
 * File Upload Service - Encapsulation principle
 * Handles all file upload operations
 */
class UploadApiService extends BaseApiService {
  async uploadFile(companyId: number, fiscalPeriodId: number, file: File, importType: 'bank-statement' | 'csv' = 'bank-statement'): Promise<UploadResponse> {
    try {
      const formData = new FormData();
      formData.append('file', file);

      const endpoint = importType === 'csv'
        ? `/v1/import/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/imports/csv`
        : `/v1/import/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/imports/bank-statement`;

      const response = await this.client.post(endpoint, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      return response.data;
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 413) {
        throw new Error('File too large. Please upload a smaller file.');
      }
      if (error instanceof AxiosError && error.response?.status === 415) {
        throw new Error('Unsupported file type. Please upload a PDF, CSV, or Excel file.');
      }
      this.handleError('File upload', error);
    }
  }

  async uploadBatchFiles(companyId: number, fiscalPeriodId: number, files: File[]): Promise<UploadResponse> {
    try {
      const formData = new FormData();
      files.forEach((file, index) => {
        formData.append(`file${index}`, file);
      });

      const response = await this.client.post(`/v1/import/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/imports/bank-statements/batch`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      return response.data;
    } catch (error) {
      this.handleError('Batch file upload', error);
    }
  }
}

/**
 * Plan Management Service - Encapsulation principle
 * Handles all plan-related operations
 */
class PlanApiService extends BaseApiService {
  async getPlans(): Promise<Plan[]> {
    try {
      const response = await this.client.get<ApiResponse<Plan[]>>('/plans');
      const plans = response.data.data;

      if (!plans || plans.length === 0) {
        throw new Error(
          'No plans found in database. Please add plans through the backend administration. ' +
          'SQL: INSERT INTO plans (name, description, price_monthly, price_yearly, features, max_companies, max_transactions, support_level, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
      }

      return plans;
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 404) {
        throw new Error('Plans endpoint not found. Please ensure the backend API is running and plans table exists.');
      }
      this.handleError('Load plans', error);
    }
  }
}

/**
 * System Service - Encapsulation principle
 * Handles all system-related operations
 */
class SystemApiService extends BaseApiService {
  async getHealth(): Promise<HealthResponse> {
    try {
      const response = await this.client.get('/v1/health');
      return response.data;
    } catch (error) {
      this.handleError('Health check', error);
    }
  }

  async getCurrentTime(): Promise<{ timestamp: number, formatted: string }> {
    try {
      const response = await this.client.get('/v1/system/time');
      return response.data;
    } catch (error) {
      this.handleError('Get current time', error);
    }
  }
}

/**
 * Classification Service - Handles transaction classification operations
 */
class ClassificationApiService extends BaseApiService {
  async initializeChartOfAccounts(companyId: number): Promise<string> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/classification/initialize-chart-of-accounts`);
      return response.data;
    } catch (error) {
      this.handleError('Initialize chart of accounts', error);
    }
  }

  async initializeTransactionMappingRules(companyId: number): Promise<string> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/classification/initialize-mapping-rules`);
      return response.data;
    } catch (error) {
      this.handleError('Initialize mapping rules', error);
    }
  }

  async performFullInitialization(companyId: number): Promise<string> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/classification/full-initialization`);
      return response.data;
    } catch (error) {
      this.handleError('Full initialization', error);
    }
  }

  async autoClassifyTransactions(companyId: number): Promise<string> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/classification/auto-classify`);
      return response.data;
    } catch (error) {
      this.handleError('Auto classify transactions', error);
    }
  }

  async syncJournalEntries(companyId: number): Promise<string> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/classification/sync-journal-entries`);
      return response.data;
    } catch (error) {
      this.handleError('Sync journal entries', error);
    }
  }

  async regenerateAllJournalEntries(companyId: number): Promise<string> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/classification/regenerate-journal-entries`);
      return response.data;
    } catch (error) {
      this.handleError('Regenerate journal entries', error);
    }
  }

  async getClassificationSummary(companyId: number): Promise<string> {
    try {
      const response = await this.client.get(`/v1/companies/${companyId}/classification/summary`);
      return response.data;
    } catch (error) {
      this.handleError('Get classification summary', error);
    }
  }

  async getUncategorizedTransactions(companyId: number): Promise<string> {
    try {
      const response = await this.client.get(`/v1/companies/${companyId}/classification/uncategorized`);
      return response.data;
    } catch (error) {
      this.handleError('Get uncategorized transactions', error);
    }
  }
}

/**
 * Data Management Service - Handles data management operations
 */
class DataManagementApiService extends BaseApiService {
  async syncInvoiceJournalEntries(companyId: number): Promise<string> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/data-management/sync-invoice-journal-entries`);
      return response.data;
    } catch (error) {
      this.handleError('Sync invoice journal entries', error);
    }
  }

  async resetCompanyData(companyId: number, includeTransactions: boolean): Promise<string> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/data-management/reset`, {
        includeTransactions
      });
      return response.data;
    } catch (error) {
      this.handleError('Reset company data', error);
    }
  }
}

/**
 * Main ApiService - Composition/Inheritance principle
 * Composes all specialized services for a unified API interface
 * Provides backwards compatibility while enabling OOP structure
 */
export class ApiService extends BaseApiService {
  // Composition - Encapsulation principle
  public readonly auth: AuthApiService;
  public readonly companies: CompanyApiService;
  public readonly fiscalPeriods: FiscalPeriodApiService;
  public readonly transactions: TransactionApiService;
  public readonly reports: ReportApiService;
  public readonly uploads: UploadApiService;
  public readonly plans: PlanApiService;
  public readonly system: SystemApiService;
  public readonly classification: ClassificationApiService;
  public readonly dataManagement: DataManagementApiService;

  constructor() {
    super(); // Call BaseApiService constructor
    // Initialize all specialized services
    this.auth = new AuthApiService();
    this.companies = new CompanyApiService();
    this.fiscalPeriods = new FiscalPeriodApiService();
    this.transactions = new TransactionApiService();
    this.reports = new ReportApiService();
    this.uploads = new UploadApiService();
    this.plans = new PlanApiService();
    this.system = new SystemApiService();
    this.classification = new ClassificationApiService();
    this.dataManagement = new DataManagementApiService();
  }

  // Backwards compatibility methods - Polymorphism principle
  // These delegate to the appropriate specialized service
  async getHealth(): Promise<HealthResponse> {
    return this.system.getHealth();
  }

  async login(credentials: LoginRequest): Promise<AuthResponse> {
    return this.auth.login(credentials);
  }

  async register(userData: RegisterRequest): Promise<AuthResponse> {
    return this.auth.register(userData);
  }

  async getCurrentUser(): Promise<User> {
    return this.auth.getCurrentUser();
  }

  async logout(): Promise<void> {
    return this.auth.logout();
  }

  async selectPlan(planSelection: PlanSelection): Promise<AuthResponse> {
    return this.auth.selectPlan(planSelection);
  }

  async getPlans(): Promise<Plan[]> {
    return this.plans.getPlans();
  }

  async getCompanies(): Promise<Company[]> {
    return this.companies.getCompanies();
  }

  async getFiscalPeriods(companyId: number): Promise<FiscalPeriod[]> {
    return this.fiscalPeriods.getFiscalPeriods(companyId);
  }

  async getTransactions(companyId: number, fiscalPeriodId: number): Promise<{ data: ApiTransaction[], count: number, company_id: number, timestamp: number, note: string }> {
    return this.transactions.getTransactions(companyId, fiscalPeriodId);
  }

  async exportTransactionsToCsv(companyId: number, fiscalPeriodId: number): Promise<Blob> {
    return this.transactions.exportTransactionsToCsv(companyId, fiscalPeriodId);
  }

  async exportTransactionsToPdf(companyId: number, fiscalPeriodId: number): Promise<Blob> {
    return this.transactions.exportTransactionsToPdf(companyId, fiscalPeriodId);
  }

  async uploadFile(companyId: number, fiscalPeriodId: number, file: File): Promise<UploadResponse> {
    return this.uploads.uploadFile(companyId, fiscalPeriodId, file);
  }

  async generateTrialBalance(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.reports.generateTrialBalance(companyId, fiscalPeriodId, format);
  }

  async generateIncomeStatement(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.reports.generateIncomeStatement(companyId, fiscalPeriodId, format);
  }

  async generateBalanceSheet(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.reports.generateBalanceSheet(companyId, fiscalPeriodId, format);
  }

  async generateCashFlow(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.reports.generateCashFlow(companyId, fiscalPeriodId, format);
  }

  async generateGeneralLedger(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.reports.generateGeneralLedger(companyId, fiscalPeriodId, format);
  }

  async generateCashbook(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.reports.generateCashbook(companyId, fiscalPeriodId, format);
  }

  async generateAuditTrail(companyId: number, fiscalPeriodId: number, format: string = 'text'): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    return this.reports.generateAuditTrail(companyId, fiscalPeriodId, format);
  }

  // Payroll methods
  async getPayrollPeriods(companyId: number): Promise<any[]> {
    try {
      const response = await this.client.get(`/v1/payroll/companies/${companyId}/periods`);
      return response.data;
    } catch (error) {
      this.handleError('Get payroll periods', error);
    }
  }

  async processPayroll(payrollPeriodId: number): Promise<any> {
    try {
      const response = await this.client.post(`/v1/payroll/periods/${payrollPeriodId}/process`);
      return response.data;
    } catch (error) {
      this.handleError('Process payroll', error);
    }
  }

  async generatePayslips(payrollPeriodId: number): Promise<any> {
    try {
      const response = await this.client.post(`/v1/payroll/payslips/generate/${payrollPeriodId}`);
      return response.data;
    } catch (error) {
      this.handleError('Generate payslips', error);
    }
  }

  // Budget methods
  async getBudgetsByCompany(companyId: number): Promise<any[]> {
    try {
      const response = await this.client.get(`/v1/budgets/company/${companyId}`);
      return response.data;
    } catch (error) {
      this.handleError('Get budgets by company', error);
    }
  }

  async getBudgetsByFiscalPeriod(fiscalPeriodId: number): Promise<any[]> {
    try {
      const response = await this.client.get(`/v1/budgets/fiscal-period/${fiscalPeriodId}`);
      return response.data;
    } catch (error) {
      this.handleError('Get budgets by fiscal period', error);
    }
  }

  async getBudgetVariance(budgetId: number): Promise<any> {
    try {
      const response = await this.client.get(`/v1/budgets/${budgetId}/variance`);
      return response.data;
    } catch (error) {
      this.handleError('Get budget variance', error);
    }
  }

  async createBudget(budget: any): Promise<any> {
    try {
      const response = await this.client.post('/v1/budgets', budget);
      return response.data;
    } catch (error) {
      this.handleError('Create budget', error);
    }
  }
}
