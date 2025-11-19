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
          // Clear invalid token and redirect to login
          localStorage.removeItem('auth_token');
          window.location.href = '/login';
          throw new Error('Authentication failed. Please log in again.');
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
      const response = await this.client.get<ApiResponse<Company[]>>('/v1/companies/user');
      const companies = response.data.data;

      if (!companies || companies.length === 0) {
        throw new Error(
          'No companies found for your account. Please contact your administrator to grant company access. ' +
          'SQL: INSERT INTO user_companies (user_id, company_id, role, created_by, updated_by) VALUES (?, ?, ?, ?, ?)'
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
      const response = await this.client.get(`/v1/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/export/csv`, {
        responseType: 'blob'
      });
      return response.data;
    } catch (error) {
      this.handleError('Export transactions to CSV', error);
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
  ): Promise<{ reportType: string, companyId: number, fiscalPeriodId: number, format: string, content: string }> {
    try {
      const response = await this.client.get(`/v1/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/reports/${reportType}`, {
        params: { format }
      });

      const result = response.data;
      if (!result.success || !result.data || !result.data.content) {
        throw new Error('Report generation failed: No content returned from server');
      }

      return result.data;
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
        ? `/v1/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/imports/csv`
        : `/v1/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/imports/bank-statement`;

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

      const response = await this.client.post(`/v1/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/imports/bank-statements/batch`, formData, {
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
 * Main ApiService - Composition/Inheritance principle
 * Composes all specialized services for a unified API interface
 * Provides backwards compatibility while enabling OOP structure
 */
export class ApiService {
  // Composition - Encapsulation principle
  public readonly auth: AuthApiService;
  public readonly companies: CompanyApiService;
  public readonly fiscalPeriods: FiscalPeriodApiService;
  public readonly transactions: TransactionApiService;
  public readonly reports: ReportApiService;
  public readonly uploads: UploadApiService;
  public readonly plans: PlanApiService;
  public readonly system: SystemApiService;

  constructor() {
    // Initialize all specialized services
    this.auth = new AuthApiService();
    this.companies = new CompanyApiService();
    this.fiscalPeriods = new FiscalPeriodApiService();
    this.transactions = new TransactionApiService();
    this.reports = new ReportApiService();
    this.uploads = new UploadApiService();
    this.plans = new PlanApiService();
    this.system = new SystemApiService();
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
}
