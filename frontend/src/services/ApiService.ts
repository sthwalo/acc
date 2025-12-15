import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type {
  Account,
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
  PlanSelection,
  PayrollPeriod,
  PayrollProcessingResult,
  Budget,
  BudgetVariance,
  Employee,
  EmployeeCreateRequest,
  EmployeeUpdateRequest,
  FiscalPeriodPayrollConfigRequest,
  FiscalPeriodPayrollConfigResponse,
  FiscalPeriodPayrollStatusResponse,
  PayrollDocument,
  AuditTrailDTO,
  JournalEntryDetailDTO,
  PayPalCreateOrderRequest,
  PayPalCaptureOrderRequest,
  PayPalOrderResponse,
  PayPalCaptureResponse,
  BackendPayslip,
  FiscalPeriodSetupDTO,
  Industry,
  Invoice
} from '../types/api';

/**
 * Backend employee response interface (matches actual API response)
 */
interface BackendEmployee {
  id: number;
  companyId: number;
  employeeNumber?: string;
  employeeCode?: string;
  title?: string;
  firstName: string;
  secondName?: string;
  lastName: string;
  email?: string;
  phone?: string;
  position: string;
  department?: string;
  hireDate?: string;
  dateEngaged?: string;
  terminationDate?: string;
  active: boolean;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  province?: string;
  postalCode?: string;
  country?: string;
  bankName?: string;
  accountHolderName?: string;
  accountNumber?: string;
  branchCode?: string;
  accountType?: string;
  employmentType: string;
  salaryType: string;
  basicSalary?: number;
  overtimeRate?: number;
  taxNumber?: string;
  idNumber?: string;
  taxRebateCode?: string;
  uifNumber?: string;
  medicalAidNumber?: string;
  pensionFundNumber?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
  displayName?: string;
  fullName?: string;
  currentEmployee?: boolean;
}

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
          // Don't automatically clear token and redirect - let AuthContext handle it
          // This prevents race conditions and gives more control over auth flow
          throw new Error('Authentication failed. Token may be expired.');
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
          'No companies found. Please create your first company to get started. ' +
          'Use the "Create Company" button above to add a new company.'
        );
      }

      // Add logo URLs for companies that have logo paths
      const companiesWithLogos = companies.map(company => ({
        ...company,
        logoUrl: company.logoPath ? `/api/v1/logos?path=${encodeURIComponent(company.logoPath)}` : undefined
      }));

      return companiesWithLogos;
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

  async updateCompany(id: number, company: Partial<Company>): Promise<void> {
    try {
      await this.client.put(`/v1/companies/update/${id}`, company);
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
      const response = await this.client.get<ApiResponse<FiscalPeriod[]>>(`/v1/fiscal-periods/companies/${companyId}/fiscal-periods`);
      const periods = response.data.data;

      // Don't throw error for empty arrays - let backend handle database-first logic
      // The backend will throw SQLException if no fiscal periods exist, which will be
      // handled as a specific error code by the frontend
      return periods || [];
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
      const response = await this.client.post<ApiResponse<FiscalPeriod>>(`/v1/fiscal-periods/companies/${companyId}/fiscal-periods`, period);
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
      await this.client.get(`/v1/fiscal-periods/companies/${companyId}/fiscal-periods/${id}/select`);
    } catch (error) {
      this.handleError('Select fiscal period', error);
    }
  }

  async setupFiscalPeriod(companyId: number, setupDTO: FiscalPeriodSetupDTO): Promise<FiscalPeriod> {
    try {
      const response = await this.client.post<ApiResponse<FiscalPeriod>>(`/v1/fiscal-periods/setup?companyId=${companyId}`, setupDTO);
      return response.data.data;
    } catch (error) {
      this.handleError('Setup fiscal period', error);
    }
  }

  async hasFiscalPeriods(companyId: number): Promise<boolean> {
    try {
      const response = await this.client.get<ApiResponse<boolean>>(`/v1/fiscal-periods/company/${companyId}/exists`);
      return response.data.data;
    } catch (error) {
      this.handleError('Check fiscal periods existence', error);
    }
  }

  async getFiscalPeriodsForCompany(companyId: number): Promise<FiscalPeriod[]> {
    try {
      const response = await this.client.get<ApiResponse<FiscalPeriod[]>>(`/v1/fiscal-periods/company/${companyId}`);
      return response.data.data || [];
    } catch (error) {
      this.handleError('Get fiscal periods for company', error);
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
      const response = await this.client.get<ApiResponse<ApiTransaction[]>>(`/v1/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/transactions`);
      const apiResponse = response.data;

      // Transform ApiResponse to expected format
      const result = {
        data: apiResponse.data || [],
        count: apiResponse.count || 0,
        company_id: companyId,
        timestamp: new Date(apiResponse.timestamp || Date.now()).getTime(),
        note: apiResponse.message || 'Transactions loaded successfully'
      };

      // Don't throw error for empty data - that's a valid response
      // The UI component will handle displaying empty state appropriately
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
      const response = await this.client.get(`/v1/export/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/transactions/csv`, {
        responseType: 'blob',
      });
      return response.data;
    } catch (error) {
      this.handleError('Export transactions to CSV', error);
    }
  }

  async exportTransactionsToPdf(companyId: number, fiscalPeriodId: number): Promise<Blob> {
    try {
      const response = await this.client.get(`/v1/export/companies/${companyId}/fiscal-periods/${fiscalPeriodId}/transactions/pdf`, {
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
  public readonly companies: CompanyApiService;
  public readonly fiscalPeriods: FiscalPeriodApiService;

  constructor(companies: CompanyApiService, fiscalPeriods: FiscalPeriodApiService) {
    super();
    this.companies = companies;
    this.fiscalPeriods = fiscalPeriods;
  }
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

  // ============================================================================
  // TASK_008: Report Download Methods
  // ============================================================================

  /**
   * Download Trial Balance in specified format
   */
  async downloadTrialBalance(companyId: number, fiscalPeriodId: number, format: 'PDF' | 'EXCEL' | 'CSV' = 'PDF'): Promise<void> {
    try {
      // Fetch company and period details for filename via direct API call
      const [companyResp, periodResp] = await Promise.all([
        this.client.get(`/v1/companies/${companyId}`),
        // Fiscal period lookup uses the company-less path (controller maps to /api/v1/companies/fiscal-periods/{id})
        this.client.get(`/v1/companies/fiscal-periods/${fiscalPeriodId}`),
      ]);
      const company = companyResp.data?.data;
      const period = periodResp.data?.data;

      const response = await this.client.get(`/v1/reports/trial-balance/company/${companyId}/fiscal-period/${fiscalPeriodId}/export`, {
        params: { format },
        responseType: 'blob'
      });

      // Create download link
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      
      // Determine correct file extension
      const extension = format === 'EXCEL' ? 'xlsx' : format.toLowerCase();
      
      // Use company name and period name in filename
      const companyName = (company?.name || `company_${companyId}`).replace(/[^a-zA-Z0-9]/g, '_');
      const periodName = (period?.periodName || `period_${fiscalPeriodId}`).replace(/[^a-zA-Z0-9]/g, '_');
      link.setAttribute('download', `TrialBalance_${companyName}_${periodName}.${extension}`);
      
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

    } catch (error) {
      this.handleError('Download trial balance', error);
    }
  }

  async downloadIncomeStatement(companyId: number, fiscalPeriodId: number, format: 'PDF' | 'EXCEL' | 'CSV' = 'PDF'): Promise<void> {
    try {
      const [companyResp, periodResp] = await Promise.all([
        this.client.get(`/v1/companies/${companyId}`),
        this.client.get(`/v1/companies/fiscal-periods/${fiscalPeriodId}`),
      ]);
      const company = companyResp.data?.data;
      const period = periodResp.data?.data;

      const response = await this.client.get(`/v1/reports/income-statement/company/${companyId}/fiscal-period/${fiscalPeriodId}/export`, {
        params: { format },
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      const extension = format === 'EXCEL' ? 'xlsx' : format.toLowerCase();
      const companyName = (company?.name || `company_${companyId}`).replace(/[^a-zA-Z0-9]/g, '_');
      const periodName = (period?.periodName || `period_${fiscalPeriodId}`).replace(/[^a-zA-Z0-9]/g, '_');
      link.setAttribute('download', `income_statement_${companyName}_${periodName}.${extension}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

    } catch (error) {
      this.handleError('Download income statement', error);
    }
  }

  async downloadBalanceSheet(companyId: number, fiscalPeriodId: number, format: 'PDF' | 'EXCEL' | 'CSV' = 'PDF'): Promise<void> {
    try {
      const [companyResp, periodResp] = await Promise.all([
        this.client.get(`/v1/companies/${companyId}`),
        // Fiscal period lookup uses the company-less path (controller maps to /api/v1/companies/fiscal-periods/{id})
        this.client.get(`/v1/companies/fiscal-periods/${fiscalPeriodId}`),
      ]);
      const company = companyResp.data?.data;
      const period = periodResp.data?.data;

      const response = await this.client.get(`/v1/reports/balance-sheet/company/${companyId}/fiscal-period/${fiscalPeriodId}/export`, {
        params: { format },
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      const extension = format === 'EXCEL' ? 'xlsx' : format.toLowerCase();
      const companyName = (company?.name || `company_${companyId}`).replace(/[^a-zA-Z0-9]/g, '_');
      const periodName = (period?.periodName || `period_${fiscalPeriodId}`).replace(/[^a-zA-Z0-9]/g, '_');
      link.setAttribute('download', `balance_sheet_${companyName}_${periodName}.${extension}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

    } catch (error) {
      this.handleError('Download balance sheet', error);
    }
  }

  async downloadGeneralLedger(companyId: number, fiscalPeriodId: number, format: 'PDF' | 'EXCEL' | 'CSV' = 'PDF'): Promise<void> {
    try {
      const [companyResp, periodResp] = await Promise.all([
        this.client.get(`/v1/companies/${companyId}`),
        // Fiscal period lookup uses the company-less path (controller maps to /api/v1/companies/fiscal-periods/{id})
        this.client.get(`/v1/companies/fiscal-periods/${fiscalPeriodId}`),
      ]);
      const company = companyResp.data?.data;
      const period = periodResp.data?.data;

      const response = await this.client.get(`/v1/reports/general-ledger/company/${companyId}/fiscal-period/${fiscalPeriodId}/export`, {
        params: { format },
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      const extension = format === 'EXCEL' ? 'xlsx' : format.toLowerCase();
      const companyName = (company?.name || `company_${companyId}`).replace(/[^a-zA-Z0-9]/g, '_');
      const periodName = (period?.periodName || `period_${fiscalPeriodId}`).replace(/[^a-zA-Z0-9]/g, '_');
      link.setAttribute('download', `general_ledger_${companyName}_${periodName}.${extension}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      this.handleError('Download general ledger', error);
    }
  }

  async downloadCashbook(companyId: number, fiscalPeriodId: number, format: 'PDF' | 'EXCEL' | 'CSV' = 'PDF'): Promise<void> {
    try {
      const [companyResp, periodResp] = await Promise.all([
        this.client.get(`/v1/companies/${companyId}`),
        // Fiscal period lookup uses the company-less path (controller maps to /api/v1/companies/fiscal-periods/{id})
        this.client.get(`/v1/companies/fiscal-periods/${fiscalPeriodId}`),
      ]);
      const company = companyResp.data?.data;
      const period = periodResp.data?.data;

      const response = await this.client.get(`/v1/reports/cashbook/company/${companyId}/fiscal-period/${fiscalPeriodId}/export`, {
        params: { format },
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      const extension = format === 'EXCEL' ? 'xlsx' : format.toLowerCase();
      const companyName = (company?.name || `company_${companyId}`).replace(/[^a-zA-Z0-9]/g, '_');
      const periodName = (period?.periodName || `period_${fiscalPeriodId}`).replace(/[^a-zA-Z0-9]/g, '_');
      link.setAttribute('download', `cashbook_${companyName}_${periodName}.${extension}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      this.handleError('Download cashbook', error);
    }
  }

  async downloadAuditTrail(companyId: number, fiscalPeriodId: number, format: 'PDF' | 'EXCEL' | 'CSV' = 'PDF'): Promise<void> {
    try {
      const [companyResp, periodResp] = await Promise.all([
        this.client.get(`/v1/companies/${companyId}`),
        // Fiscal period lookup uses the company-less path (controller maps to /api/v1/companies/fiscal-periods/{id})
        this.client.get(`/v1/companies/fiscal-periods/${fiscalPeriodId}`),
      ]);
      const company = companyResp.data?.data;
      const period = periodResp.data?.data;

      const response = await this.client.get(`/v1/reports/audit-trail/company/${companyId}/fiscal-period/${fiscalPeriodId}/export`, {
        params: { format },
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      const extension = format === 'EXCEL' ? 'xlsx' : format.toLowerCase();
      const companyName = (company?.name || `company_${companyId}`).replace(/[^a-zA-Z0-9]/g, '_');
      const periodName = (period?.periodName || `period_${fiscalPeriodId}`).replace(/[^a-zA-Z0-9]/g, '_');
      link.setAttribute('download', `audit_trail_${companyName}_${periodName}.${extension}`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      this.handleError('Download audit trail', error);
    }
  }

  // ============================================================================
  // TASK_007: Structured Audit Trail Methods
  // ============================================================================

  /**
   * Get structured audit trail with pagination and filtering.
   * Returns JSON response with journal entries, pagination metadata, and filter info.
   * 
   * @param companyId Company identifier
   * @param fiscalPeriodId Fiscal period identifier
   * @param page Page number (0-indexed, default: 0)
   * @param pageSize Number of entries per page (default: 50)
   * @param startDate Optional start date filter (ISO format: yyyy-MM-dd)
   * @param endDate Optional end date filter (ISO format: yyyy-MM-dd)
   * @param searchTerm Optional search term for description/reference
   * @returns Structured audit trail response with pagination
   */
  async getAuditTrail(
    companyId: number,
    fiscalPeriodId: number,
    page: number = 0,
    pageSize: number = 50,
    startDate?: string,
    endDate?: string,
    searchTerm?: string
  ): Promise<AuditTrailDTO[]> {
    try {
      const params: Record<string, string | number> = {
        page,
        pageSize,
      };

      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;
      if (searchTerm) params.searchTerm = searchTerm;

      const response = await this.client.get(
        `/v1/reports/audit-trail/company/${companyId}/fiscal-period/${fiscalPeriodId}`,
        { params }
      );

      return response.data;
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 400) {
        throw new Error(`Invalid company or fiscal period. Please ensure both exist.`);
      }
      this.handleError('Get audit trail', error);
    }
  }

  /**
   * Get detailed journal entry with all line items.
   * Returns JSON response with full journal entry details including debits and credits.
   * 
   * @param journalEntryId Journal entry identifier
   * @returns Detailed journal entry with all lines
   */
  async getJournalEntryDetail(journalEntryId: number): Promise<JournalEntryDetailDTO> {
    try {
      const response = await this.client.get(
        `/v1/reports/audit-trail/journal-entry/${journalEntryId}`
      );

      return response.data;
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 404) {
        throw new Error(`Journal entry with ID ${journalEntryId} not found.`);
      }
      this.handleError('Get journal entry detail', error);
    }
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
        timeout: 300000, // 5 minutes for file processing (increased from 2 minutes)
        onUploadProgress: (progressEvent) => {
          // Optional: Add upload progress tracking if needed
          if (progressEvent.total) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            console.log(`Upload progress: ${percentCompleted}%`);
          }
        },
      });

      return response.data;
    } catch (error) {
      if (error instanceof AxiosError && error.response?.status === 413) {
        throw new Error('File too large. Please upload a smaller file (max 10MB).');
      }
      if (error instanceof AxiosError && error.response?.status === 415) {
        throw new Error('Unsupported file type. Please upload a PDF, CSV, or Excel file.');
      }
      if (error instanceof AxiosError && error.code === 'ECONNABORTED') {
        throw new Error('Upload timed out. The file is being processed in the background. Please check back in a few minutes.');
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
  async initializeChartOfAccounts(companyId: number): Promise<{success: boolean, message: string, data: unknown}> {
    try {
      const response = await this.client.post(`/v1/import/chart-of-accounts/company/${companyId}/initialize`);
      return response.data;
    } catch (error) {
      this.handleError('Initialize chart of accounts', error);
    }
  }

  async initializeTransactionMappingRules(companyId: number): Promise<{success: boolean, message: string, data: unknown}> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/classification/initialize-mapping-rules`);
      return response.data;
    } catch (error) {
      this.handleError('Initialize mapping rules', error);
    }
  }

  async performFullInitialization(companyId: number): Promise<{success: boolean, message: string, data: unknown}> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/classification/full-initialization`);
      return response.data;
    } catch (error) {
      this.handleError('Full initialization', error);
    }
  }

  async autoClassifyTransactions(companyId: number): Promise<{success: boolean, message: string, data: unknown}> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/classification/auto-classify`);
      return response.data;
    } catch (error) {
      this.handleError('Auto classify transactions', error);
    }
  }

  async syncJournalEntries(companyId: number): Promise<{success: boolean, message: string, data: unknown}> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/classification/sync-journal-entries`);
      return response.data;
    } catch (error) {
      this.handleError('Sync journal entries', error);
    }
  }

  async regenerateAllJournalEntries(companyId: number): Promise<{success: boolean, message: string, data: number}> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/classification/regenerate-journal-entries`);
      return response.data;
    } catch (error) {
      this.handleError('Regenerate journal entries', error);
    }
  }

  async getClassificationSummary(companyId: number): Promise<{success: boolean, message: string, data: string}> {
    try {
      const response = await this.client.get(`/v1/companies/${companyId}/classification/summary`);
      return response.data;
    } catch (error) {
      this.handleError('Get classification summary', error);
    }
  }

  async getUnclassifiedTransactions(companyId: number, fiscalPeriodId: number): Promise<{success: boolean, message: string, data: unknown}> {
    try {
      const response = await this.client.get(`/v1/companies/${companyId}/classification/unclassified/${fiscalPeriodId}`);
      return response.data;
    } catch (error) {
      this.handleError('Get unclassified transactions', error);
    }
  }

  async classifyTransaction(transactionId: number, accountCode: string): Promise<{success: boolean, message: string, data: unknown}> {
    try {
      const response = await this.client.post(`/v1/transactions/${transactionId}/classify`, { accountCode });
      return response.data;
    } catch (error) {
      this.handleError('Classify transaction', error);
    }
  }

  async createClassificationRule(companyId: number, rule: unknown): Promise<{success: boolean, message: string, data: unknown}> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/classification/rules`, rule);
      return response.data;
    } catch (error) {
      this.handleError('Create classification rule', error);
    }
  }

  /**
   * Update transaction classification by assigning debit and credit accounts.
   * Creates or updates journal entry for the transaction.
   * 
   * @param companyId - Company ID
   * @param transactionId - Transaction ID to classify
   * @param debitAccountId - Debit account ID
   * @param creditAccountId - Credit account ID
   * @returns API response with success status and message
   */
  async updateTransactionClassification(
    companyId: number,
    transactionId: number,
    debitAccountId: number,
    creditAccountId: number
  ): Promise<{success: boolean, message: string, data: unknown}> {
    try {
      const response = await this.client.put(
        `/v1/companies/${companyId}/classification/transactions/${transactionId}`,
        {
          debitAccountId,
          creditAccountId
        }
      );
      return response.data;
    } catch (error) {
      this.handleError('Update transaction classification', error);
    }
  }
}

/**
 * Account Management Service - Handles chart of accounts operations.
 */
class AccountApiService extends BaseApiService {
  /**
   * Get chart of accounts for a company.
   * Returns all active accounts for dropdown selection in UI.
   */
  async getChartOfAccounts(companyId: number): Promise<ApiResponse<Account[]>> {
    try {
      const response = await this.client.get<ApiResponse<Account[]>>(
        `/v1/companies/${companyId}/accounts`
      );
      return response.data;
    } catch (error) {
      this.handleError('Get chart of accounts', error);
    }
  }
}

/**
 * Data Management Service - Handles data management operations
 */
class DataManagementApiService extends BaseApiService {
  async syncInvoiceJournalEntries(companyId: number): Promise<{success: boolean, message: string, data: unknown}> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/data-management/sync-invoice-journal-entries`);
      return response.data;
    } catch (error) {
      this.handleError('Sync invoice journal entries', error);
    }
  }

  async generateInvoicePdf(companyId: number, invoiceId: number): Promise<{success: boolean, message: string}> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/data-management/invoices/${invoiceId}/generate-pdf`);
      // Debug: log raw axios response for diagnosis of client-side issues when server returns 200
      // eslint-disable-next-line no-console
      console.debug('API generateInvoicePdf response:', response);
      const data = response.data;
      if (typeof data === 'string') {
        return { success: true, message: data };
      }
      return data as {success: boolean, message: string};
    } catch (error) {
      this.handleError('Generate invoice PDF', error);
    }
  }

  async downloadInvoicePdf(companyId: number, invoiceId: number): Promise<Blob> {
    try {
      const response = await this.client.get(`/v1/companies/${companyId}/data-management/invoices/${invoiceId}/pdf`, { responseType: 'blob' });
      return response.data as Blob;
    } catch (error) {
      this.handleError('Download invoice PDF', error);
    }
  }

  /**
   * Download invoice PDF and save it with a useful filename using company and invoice metadata when available
   */
  async downloadInvoice(companyId: number, invoiceId: number): Promise<void> {
    try {
      // Try to fetch company and invoice metadata for filename
      const [companyResp, invoiceResp] = await Promise.all([
        this.client.get(`/v1/companies/${companyId}`),
        // invoice details endpoint may exist; if not, this will 404 and be handled
        this.client.get(`/v1/companies/${companyId}/data-management/invoices/${invoiceId}`)
      ].map(p => p.catch((e) => e)));

      const company = companyResp && !(companyResp instanceof Error) ? companyResp.data?.data : null;
      const invoice = invoiceResp && !(invoiceResp instanceof Error) ? invoiceResp.data?.data : null;

      const response = await this.client.get(`/v1/companies/${companyId}/data-management/invoices/${invoiceId}/pdf`, { responseType: 'blob' });
      // Debug: log response type/headers when downloading invoice
      // eslint-disable-next-line no-console
      console.debug('API downloadInvoice response headers:', response.headers);

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;

      const companyName = (company?.name || `company_${companyId}`).replace(/[^a-zA-Z0-9]/g, '_');
      const invoiceRef = (invoice?.invoiceNumber || invoice?.id || invoiceId).toString().replace(/[^a-zA-Z0-9]/g, '_');
      link.setAttribute('download', `Invoice_${companyName}_${invoiceRef}.pdf`);

      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      this.handleError('Download invoice', error);
    }
  }

  async createManualInvoice(companyId: number, invoice: { invoiceNumber: string; invoiceDate: string; description?: string; amount: number; debitAccountId: number; creditAccountId: number; fiscalPeriodId: number }): Promise<ApiResponse<Invoice> | Invoice> {
    try {
      const response = await this.client.post(`/v1/companies/${companyId}/data-management/invoices`, invoice);
      return response.data;
    } catch (error) {
      this.handleError('Create manual invoice', error);
    }
  }

  async getManualInvoices(companyId: number): Promise<ApiResponse<Invoice[]>> {
    try {
      const response = await this.client.get(`/v1/companies/${companyId}/data-management/invoices`);
      return response.data;
    } catch (error) {
      // If server returns 5xx (server error), or the global interceptor turned it into a
      // plain Error('Server error...'), fall back to the public invoices endpoint which
      // historically returns a plain list of invoices. This avoids blocking the UI while the
      // backend `data-management` endpoint is investigated.
      const isServerError = (error instanceof AxiosError && error.response && error.response.status >= 500)
        || (error instanceof Error && /server error/i.test(error.message || ''));

      if (isServerError) {
        try {
          const alt = await this.client.get<Invoice[]>(`/v1/companies/${companyId}/invoices`);
          return { success: true, data: alt.data, message: 'Invoices retrieved (fallback endpoint)' } as ApiResponse<Invoice[]>;
        } catch (altErr) {
          // fall through to generic handler
          this.handleError('Get manual invoices (fallback)', altErr);
        }
      }

      this.handleError('Get manual invoices', error);
    }
  }

  async resetCompanyData(companyId: number, includeTransactions: boolean): Promise<{success: boolean, message: string, data: unknown}> {
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

class PayPalApiService extends BaseApiService {
  async createOrder(request: PayPalCreateOrderRequest): Promise<PayPalOrderResponse> {
    try {
      const response = await this.client.post<ApiResponse<PayPalOrderResponse>>('/v1/paypal/orders', request);
      return response.data.data;
    } catch (error) {
      this.handleError('Create PayPal order', error);
    }
  }

  async captureOrder(request: PayPalCaptureOrderRequest): Promise<PayPalCaptureResponse> {
    try {
      const response = await this.client.post<ApiResponse<PayPalCaptureResponse>>('/v1/paypal/orders/capture', request);
      return response.data.data;
    } catch (error) {
      this.handleError('Capture PayPal order', error);
    }
  }
}

/**
 * Industry Management Service - Encapsulation principle
 * Handles all industry-related operations
 */
class IndustryApiService extends BaseApiService {
  async getIndustries(): Promise<Industry[]> {
    try {
      const response = await this.client.get<ApiResponse<Industry[]>>('/v1/industries');
      const industries = response.data.data;

      if (!industries) {
        throw new Error('No industries found. Please contact support.');
      }

      return industries;
    } catch (error) {
      this.handleError('Load industries', error);
    }
  }

  async getIndustry(id: number): Promise<Industry> {
    try {
      const response = await this.client.get<ApiResponse<Industry>>(`/v1/industries/${id}`);
      return response.data.data;
    } catch (error) {
      this.handleError('Get industry details', error);
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
  public readonly industries: IndustryApiService;
  public readonly fiscalPeriods: FiscalPeriodApiService;
  public readonly transactions: TransactionApiService;
  public readonly reports: ReportApiService;
  public readonly uploads: UploadApiService;
  public readonly plans: PlanApiService;
  public readonly system: SystemApiService;
  public readonly classification: ClassificationApiService;
  public readonly accounts: AccountApiService;
  public readonly dataManagement: DataManagementApiService;
  public readonly paypal: PayPalApiService;

  constructor() {
    super(); // Call BaseApiService constructor
    // Initialize all specialized services
    this.auth = new AuthApiService();
    this.companies = new CompanyApiService();
    this.industries = new IndustryApiService();
    this.fiscalPeriods = new FiscalPeriodApiService();
    this.transactions = new TransactionApiService();
    this.reports = new ReportApiService(this.companies, this.fiscalPeriods);
    this.uploads = new UploadApiService();
    this.plans = new PlanApiService();
    this.system = new SystemApiService();
    this.classification = new ClassificationApiService();
    this.accounts = new AccountApiService();
    this.dataManagement = new DataManagementApiService();
    this.paypal = new PayPalApiService();
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
  async getPayrollPeriods(companyId: number): Promise<PayrollPeriod[]> {
    try {
      const response = await this.client.get<PayrollPeriod[]>(`/v1/payroll/periods?companyId=${companyId}`);
      return response.data;
    } catch (error) {
      this.handleError('Get payroll periods', error);
    }
  }

  async processPayroll(payrollPeriodId: number): Promise<PayrollProcessingResult> {
    try {
      const response = await this.client.post<PayrollProcessingResult>('/v1/payroll/process', {
        fiscalPeriodId: payrollPeriodId,
        reprocess: false
      });
      return response.data;
    } catch (error) {
      this.handleError('Process payroll', error);
    }
  }

  async reprocessPayroll(payrollPeriodId: number): Promise<PayrollProcessingResult> {
    try {
      const response = await this.client.post<PayrollProcessingResult>('/v1/payroll/reprocess', {
        fiscalPeriodId: payrollPeriodId,
        reprocess: true
      });
      return response.data;
    } catch (error) {
      this.handleError('Reprocess payroll', error);
    }
  }

  async createPayrollPeriod(fiscalPeriod: Omit<FiscalPeriod, 'id' | 'createdAt' | 'createdBy' | 'updatedBy' | 'updatedAt'>): Promise<FiscalPeriod> {
    try {
      const response = await this.client.post<FiscalPeriod>('/v1/payroll/periods', fiscalPeriod);
      return response.data;
    } catch (error) {
      this.handleError('Create payroll period', error);
    }
  }

  // Payroll Configuration methods
  async getFiscalPeriodPayrollConfig(fiscalPeriodId: number): Promise<FiscalPeriodPayrollConfigResponse> {
    try {
      const response = await this.client.get<ApiResponse<FiscalPeriodPayrollConfigResponse>>(`/v1/fiscal-periods/${fiscalPeriodId}/payroll-config`);
      return response.data.data;
    } catch (error) {
      this.handleError('Get fiscal period payroll config', error);
    }
  }

  async updateFiscalPeriodPayrollConfig(fiscalPeriodId: number, config: FiscalPeriodPayrollConfigRequest): Promise<FiscalPeriodPayrollConfigResponse> {
    try {
      const response = await this.client.put<ApiResponse<FiscalPeriodPayrollConfigResponse>>(`/v1/fiscal-periods/${fiscalPeriodId}/payroll-config`, config);
      return response.data.data;
    } catch (error) {
      this.handleError('Update fiscal period payroll config', error);
    }
  }

  async getFiscalPeriodsPayrollStatus(): Promise<FiscalPeriodPayrollStatusResponse[]> {
    try {
      const response = await this.client.get<ApiResponse<FiscalPeriodPayrollStatusResponse[]>>(`/v1/fiscal-periods/payroll-status`);
      return response.data.data;
    } catch (error) {
      this.handleError('Get fiscal periods payroll status', error);
    }
  }

  async resetFiscalPeriodPayrollConfig(fiscalPeriodId: number): Promise<string> {
    try {
      const response = await this.client.delete<ApiResponse<string>>(`/v1/fiscal-periods/${fiscalPeriodId}/payroll-config`);
      return response.data.data;
    } catch (error) {
      this.handleError('Reset fiscal period payroll config', error);
    }
  }

  // Budget methods
  async getBudgetsByCompany(companyId: number): Promise<Budget[]> {
    try {
      const response = await this.client.get<Budget[]>(`/v1/budgets/company/${companyId}`);
      return response.data;
    } catch (error) {
      this.handleError('Get budgets by company', error);
    }
  }

  async getBudgetsByFiscalPeriod(fiscalPeriodId: number): Promise<Budget[]> {
    try {
      const response = await this.client.get<Budget[]>(`/v1/budgets/fiscal-period/${fiscalPeriodId}`);
      return response.data;
    } catch (error) {
      this.handleError('Get budgets by fiscal period', error);
    }
  }

  async getBudgetVariance(budgetId: number): Promise<BudgetVariance> {
    try {
      const response = await this.client.get<BudgetVariance>(`/v1/budgets/${budgetId}/variance`);
      return response.data;
    } catch (error) {
      this.handleError('Get budget variance', error);
    }
  }

  async createBudget(budget: Omit<Budget, 'id' | 'createdAt' | 'updatedAt'>): Promise<Budget> {
    try {
      const response = await this.client.post<Budget>('/v1/budgets', budget);
      return response.data;
    } catch (error) {
      this.handleError('Create budget', error);
    }
  }

  // Employee methods
  async getEmployeesByCompany(companyId: number): Promise<Employee[]> {
    try {
      // First, fetch the first page to get pagination info
      const firstResponse = await this.client.get(`/v1/payroll/employees?companyId=${companyId}&size=100`);
      const firstPageData = firstResponse.data;
      const totalPages = firstPageData.totalPages || 1;
      let allEmployees: BackendEmployee[] = [...(firstPageData.content || [])];

      // Fetch remaining pages if any
      for (let page = 1; page < totalPages; page++) {
        const response = await this.client.get(`/v1/payroll/employees?companyId=${companyId}&size=100&page=${page}`);
        const pageData = response.data;
        const pageEmployees = pageData.content || [];
        allEmployees = allEmployees.concat(pageEmployees);
      }

      // Map backend field names to frontend interface
      return allEmployees.map((emp: BackendEmployee) => ({
        id: emp.id,
        companyId: emp.companyId,
        employeeNumber: emp.employeeCode || emp.employeeNumber || `EMP-${emp.id}`,
        title: emp.title,
        firstName: emp.firstName,
        secondName: emp.secondName,
        lastName: emp.lastName,
        email: emp.email,
        phone: emp.phone,
        position: emp.position,
        department: emp.department,
        hireDate: emp.dateEngaged || emp.hireDate,
        terminationDate: emp.terminationDate,
        active: emp.active,
        addressLine1: emp.addressLine1,
        addressLine2: emp.addressLine2,
        city: emp.city,
        province: emp.province,
        postalCode: emp.postalCode,
        country: emp.country,
        bankName: emp.bankName,
        accountHolderName: emp.accountHolderName,
        accountNumber: emp.accountNumber,
        branchCode: emp.branchCode,
        accountType: emp.accountType,
        employmentType: emp.employmentType as 'PERMANENT' | 'CONTRACT' | 'TEMPORARY',
        salaryType: emp.salaryType as 'MONTHLY' | 'WEEKLY' | 'HOURLY' | 'DAILY',
        basicSalary: emp.basicSalary,
        overtimeRate: emp.overtimeRate,
        taxNumber: emp.idNumber || emp.taxNumber,
        taxRebateCode: emp.taxRebateCode,
        uifNumber: emp.uifNumber,
        medicalAidNumber: emp.medicalAidNumber,
        pensionFundNumber: emp.pensionFundNumber,
        createdAt: emp.createdAt,
        updatedAt: emp.updatedAt,
        createdBy: emp.createdBy,
        updatedBy: emp.updatedBy,
      }));
    } catch (error) {
      this.handleError('Get employees by company', error);
    }
  }

  async getActiveEmployeesByCompany(companyId: number): Promise<Employee[]> {
    try {
      // First, fetch the first page to get pagination info
      const firstResponse = await this.client.get(`/v1/payroll/employees?companyId=${companyId}&active=true&size=100`);
      const firstPageData = firstResponse.data;
      const totalPages = firstPageData.totalPages || 1;
      let allEmployees: BackendEmployee[] = [...(firstPageData.content || [])];

      // Fetch remaining pages if any
      for (let page = 1; page < totalPages; page++) {
        const response = await this.client.get(`/v1/payroll/employees?companyId=${companyId}&active=true&size=100&page=${page}`);
        const pageData = response.data;
        const pageEmployees = pageData.content || [];
        allEmployees = allEmployees.concat(pageEmployees);
      }

      // Map backend field names to frontend interface
      return allEmployees.map((emp: BackendEmployee) => ({
        id: emp.id,
        companyId: emp.companyId,
        employeeNumber: emp.employeeCode || emp.employeeNumber || `EMP-${emp.id}`,
        title: emp.title,
        firstName: emp.firstName,
        secondName: emp.secondName,
        lastName: emp.lastName,
        email: emp.email,
        phone: emp.phone,
        position: emp.position,
        department: emp.department,
        hireDate: emp.dateEngaged || emp.hireDate,
        terminationDate: emp.terminationDate,
        active: emp.active,
        addressLine1: emp.addressLine1,
        addressLine2: emp.addressLine2,
        city: emp.city,
        province: emp.province,
        postalCode: emp.postalCode,
        country: emp.country,
        bankName: emp.bankName,
        accountHolderName: emp.accountHolderName,
        accountNumber: emp.accountNumber,
        branchCode: emp.branchCode,
        accountType: emp.accountType,
        employmentType: emp.employmentType as 'PERMANENT' | 'CONTRACT' | 'TEMPORARY',
        salaryType: emp.salaryType as 'MONTHLY' | 'WEEKLY' | 'HOURLY' | 'DAILY',
        basicSalary: emp.basicSalary,
        overtimeRate: emp.overtimeRate,
        taxNumber: emp.idNumber || emp.taxNumber,
        taxRebateCode: emp.taxRebateCode,
        uifNumber: emp.uifNumber,
        medicalAidNumber: emp.medicalAidNumber,
        pensionFundNumber: emp.pensionFundNumber,
        createdAt: emp.createdAt,
        updatedAt: emp.updatedAt,
        createdBy: emp.createdBy,
        updatedBy: emp.updatedBy,
      }));
    } catch (error) {
      this.handleError('Get active employees by company', error);
    }
  }

  async getEmployeeById(employeeId: number): Promise<Employee> {
    try {
      const response = await this.client.get<BackendEmployee>(`/v1/payroll/employees/${employeeId}`);
      const emp = response.data;

      // Map backend field names to frontend interface
      return {
        id: emp.id,
        companyId: emp.companyId,
        employeeNumber: emp.employeeCode || emp.employeeNumber || `EMP-${emp.id}`,
        title: emp.title,
        firstName: emp.firstName,
        secondName: emp.secondName,
        lastName: emp.lastName,
        email: emp.email,
        phone: emp.phone,
        position: emp.position,
        department: emp.department,
        hireDate: emp.dateEngaged || emp.hireDate,
        terminationDate: emp.terminationDate,
        active: emp.active,
        addressLine1: emp.addressLine1,
        addressLine2: emp.addressLine2,
        city: emp.city,
        province: emp.province,
        postalCode: emp.postalCode,
        country: emp.country,
        bankName: emp.bankName,
        accountHolderName: emp.accountHolderName,
        accountNumber: emp.accountNumber,
        branchCode: emp.branchCode,
        accountType: emp.accountType,
        employmentType: emp.employmentType as 'PERMANENT' | 'CONTRACT' | 'TEMPORARY',
        salaryType: emp.salaryType as 'MONTHLY' | 'WEEKLY' | 'HOURLY' | 'DAILY',
        basicSalary: emp.basicSalary,
        overtimeRate: emp.overtimeRate,
        taxNumber: emp.idNumber || emp.taxNumber,
        taxRebateCode: emp.taxRebateCode,
        uifNumber: emp.uifNumber,
        medicalAidNumber: emp.medicalAidNumber,
        pensionFundNumber: emp.pensionFundNumber,
        createdAt: emp.createdAt,
        updatedAt: emp.updatedAt,
        createdBy: emp.createdBy,
        updatedBy: emp.updatedBy,
      };
    } catch (error) {
      this.handleError('Get employee by ID', error);
    }
  }

  async createEmployee(employee: Omit<Employee, 'id' | 'createdAt' | 'updatedAt'>): Promise<Employee> {
    try {
      // Map Employee interface to EmployeeCreateRequest for backend API
      const createRequest: EmployeeCreateRequest = {
        companyId: employee.companyId,
        employeeCode: employee.employeeNumber, // Map employeeNumber to employeeCode for backend
        title: employee.title,
        firstName: employee.firstName,
        secondName: employee.secondName,
        lastName: employee.lastName,
        email: employee.email,
        phone: employee.phone,
        position: employee.position,
        department: employee.department,
        hireDate: employee.hireDate || '',
        addressLine1: employee.addressLine1,
        addressLine2: employee.addressLine2,
        city: employee.city,
        province: employee.province,
        postalCode: employee.postalCode,
        country: employee.country,
        bankName: employee.bankName,
        accountHolderName: employee.accountHolderName,
        accountNumber: employee.accountNumber,
        branchCode: employee.branchCode,
        accountType: employee.accountType,
        employmentType: employee.employmentType,
        salaryType: employee.salaryType,
        basicSalary: employee.basicSalary,
        overtimeRate: employee.overtimeRate,
        taxNumber: employee.taxNumber,
        taxRebateCode: employee.taxRebateCode,
        uifNumber: employee.uifNumber,
        medicalAidNumber: employee.medicalAidNumber,
        pensionFundNumber: employee.pensionFundNumber,
      };

      const response = await this.client.post<Employee>('/v1/payroll/employees', createRequest);
      return response.data;
    } catch (error) {
      this.handleError('Create employee', error);
    }
  }

  async updateEmployee(employeeId: number, employee: EmployeeUpdateRequest): Promise<Employee> {
    try {
      const response = await this.client.put<Employee>(`/v1/payroll/employees/${employeeId}`, employee);
      return response.data;
    } catch (error) {
      this.handleError('Update employee', error);
    }
  }

  async deactivateEmployee(employeeId: number): Promise<void> {
    try {
      await this.client.delete(`/v1/payroll/employees/${employeeId}`);
    } catch (error) {
      this.handleError('Deactivate employee', error);
    }
  }

  async activateEmployee(employeeId: number): Promise<void> {
    try {
      await this.client.put(`/v1/payroll/employees/${employeeId}/activate`);
    } catch (error) {
      this.handleError('Activate employee', error);
    }
  }

  async changeEmployeeStatus(employeeId: number, active: boolean): Promise<void> {
    try {
      await this.client.put(`/v1/payroll/employees/${employeeId}/status?active=${active}`);
    } catch (error) {
      this.handleError('Change employee status', error);
    }
  }

  async hardDeleteEmployee(employeeId: number): Promise<string> {
    try {
      const response = await this.client.delete(`/v1/payroll/employees/${employeeId}/hard`);
      return response.data;
    } catch (error) {
      this.handleError('Hard delete employee', error);
    }
  }

  // Payslips methods
  async getPayslipsByFiscalPeriod(fiscalPeriodId: number): Promise<BackendPayslip[]> {
    try {
      const response = await this.client.get(`/v1/payroll/payslips/period/${fiscalPeriodId}`);
      return response.data;
    } catch (error) {
      this.handleError('Get payslips by fiscal period', error);
    }
  }

  async generatePayslipPDF(payslipId: number): Promise<Blob> {
    try {
      const response = await this.client.get(`/v1/payroll/payslips/${payslipId}/pdf`, {
        responseType: 'blob'
      });
      return response.data;
    } catch (error) {
      this.handleError('Generate payslip PDF', error);
    }
  }

  async exportBulkPDFs(payslipIds: number[]): Promise<Blob> {
    try {
      const response = await this.client.post('/v1/payroll/payslips/bulk-pdf', 
        { payslipIds },
        { responseType: 'blob' }
      );
      return response.data;
    } catch (error) {
      this.handleError('Export bulk PDFs', error);
    }
  }

  async sendPayslipsByEmail(payslipIds: number[]): Promise<void> {
    try {
      await this.client.post('/v1/payroll/payslips/send-email', { payslipIds });
    } catch (error) {
      this.handleError('Send payslips by email', error);
    }
  }

  // Document Management methods
  async getPayrollDocuments(): Promise<PayrollDocument[]> {
    try {
      const response = await this.client.get<PayrollDocument[]>('/v1/payroll/documents');
      return response.data;
    } catch (error) {
      this.handleError('Get payroll documents', error);
    }
  }

  async uploadPayrollDocument(formData: FormData): Promise<PayrollDocument> {
    try {
      const response = await this.client.post<PayrollDocument>('/v1/payroll/documents', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      return response.data;
    } catch (error) {
      this.handleError('Upload payroll document', error);
    }
  }

  async downloadPayrollDocument(documentId: number): Promise<Blob> {
    try {
      const response = await this.client.get(`/v1/payroll/documents/${documentId}/download`, {
        responseType: 'blob'
      });
      return response.data;
    } catch (error) {
      this.handleError('Download payroll document', error);
    }
  }

  async deletePayrollDocument(documentId: number): Promise<void> {
    try {
      await this.client.delete(`/v1/payroll/documents/${documentId}`);
    } catch (error) {
      this.handleError('Delete payroll document', error);
    }
  }

  async searchPayrollDocuments(params: { query?: string; type?: string }): Promise<PayrollDocument[]> {
    try {
      const response = await this.client.get<PayrollDocument[]>('/v1/payroll/documents/search', {
        params
      });
      return response.data;
    } catch (error) {
      this.handleError('Search payroll documents', error);
    }
  }

  // Payroll Reports methods
  async generatePayrollSummaryReport(fiscalPeriodId: number): Promise<Blob> {
    try {
      const response = await this.client.get(`/v1/payroll/reports/summary`, {
        params: { fiscalPeriodId, format: 'PDF' },
        responseType: 'blob'
      });
      return response.data;
    } catch (error) {
      this.handleError('Generate payroll summary report', error);
    }
  }

  async generateEmployeePayrollReport(employeeId: number, fiscalPeriodId: number): Promise<Blob> {
    try {
      const response = await this.client.get(`/v1/payroll/reports/employee`, {
        params: { employeeId, fiscalPeriodId, format: 'PDF' },
        responseType: 'blob'
      });
      return response.data;
    } catch (error) {
      this.handleError('Generate employee payroll report', error);
    }
  }

  async generateEMP201Report(fiscalPeriodId: number): Promise<Blob> {
    try {
      const response = await this.client.get(`/v1/payroll/reports/emp201`, {
        params: { fiscalPeriodId, format: 'PDF' },
        responseType: 'blob'
      });
      return response.data;
    } catch (error) {
      this.handleError('Generate EMP 201 report', error);
    }
  }
}