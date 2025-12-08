// Type definitions for API payloads
// Use unique names to avoid collisions with existing global types
// Mirror of Java DTOs used by the API

export interface AuthUser {
  id: number | string;
  email: string;
  firstName?: string;
  lastName?: string;
}

export interface AuthResponsePayload {
  token: string;
  user: AuthUser;
}

export interface ApiEnvelope<T> {
  success: boolean;
  data: T | null;
  message: string | null;
  timestamp: number;
}

// API Types
export interface Company {
  id: number;
  name: string;
  registrationNumber?: string;
  taxNumber?: string;
  address?: string;
  contactEmail?: string;
  contactPhone?: string;
  logoPath?: string;
  logoUrl?: string;
  bankName?: string;
  accountNumber?: string;
  accountType?: string;
  branchCode?: string;
  vatRegistered?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface FiscalPeriod {
  id: number;
  companyId: number;
  periodName: string;
  startDate: string;
  endDate: string;
  createdAt: string;
  createdBy: number | null;
  updatedBy: number | null;
  updatedAt: string | null;
  closed: boolean;
  // Payroll-related fields (added to FiscalPeriod in backend)
  payDate?: string | null;
  periodType?: string;
  payrollStatus?: 'OPEN' | 'PROCESSED' | 'APPROVED' | 'PAID' | 'CLOSED';
  totalGrossPay?: number;
  totalDeductions?: number;
  totalNetPay?: number;
  employeeCount?: number;
  processedAt?: string | null;
  processedBy?: string | null;
  approvedAt?: string | null;
  approvedBy?: string | null;
  payrollProcessed?: boolean;
  paymentDate?: string | null;
  payrollActive?: boolean;
  processed?: boolean;
}

export interface Transaction {
  id: number;
  company_id: number;
  fiscal_period_id: number;
  date: string;
  description: string;
  amount: number;
  type: 'debit' | 'credit';
  category: string;
  reference: string;
  balance: number;
  created_at: string;
  // Double-entry classification fields
  debit_account_id?: number | null;
  credit_account_id?: number | null;
  debit_account_name?: string | null;
  credit_account_name?: string | null;
  debit_account_code?: string | null;
  credit_account_code?: string | null;
}

export interface ApiTransaction {
  id: number;
  companyId: number;
  bankAccountId: number | null;
  fiscalPeriodId: number;
  accountCode: string | null;
  accountNumber: string | null;
  sourceFile: string | null;
  serviceFee: boolean;
  transactionDate: string;
  debitAmount: number;
  creditAmount: number;
  balance: number;
  reference: string | null;
  createdAt: string;
  transactionTypeId: number | null;
  category: string;
  subcategory: string;
  isReconciled: boolean;
  updatedAt: string;
  updatedBy: string | null;
  accountName: string | null;
  description: string;
  statementPeriod: string | null;
  // Double-entry classification fields (from journal_entry_lines)
  debitAccountId: number | null;
  creditAccountId: number | null;
  debitAccountCode: string | null;
  creditAccountCode: string | null;
  debitAccountName: string | null;
  creditAccountName: string | null;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp: number;
  count?: number;
  company_id?: number;
  note?: string;
}

export interface HealthResponse {
  status: string;
  database: string;
  timestamp: number;
}

export interface UploadResponse {
  success: boolean;
  message: string;
  summary: {
    totalLinesProcessed: number;
    validTransactions: number;
    duplicateTransactions: number;
    outOfPeriodTransactions: number;
    validationErrors: number;
  };
  savedTransactions: any[];
  rejectedTransactions: any[];
  errors: string[];
}

// Authentication Types
export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  planId: number;
  plan: Plan;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  role: string;
  passwordHash: string;
  salt: string;
  createdBy: string;
  updatedBy: string;
  lastLoginAt: string | null;
  token?: string;
  username: string;
  fullName: string;
  admin: boolean;
  user: boolean;
}

export interface Plan {
  id: number;
  name: string;
  description: string;
  price_monthly: number;
  price_yearly: number;
  features: string[];
  max_companies: number;
  max_transactions: number;
  support_level: 'basic' | 'premium' | 'enterprise';
  is_active: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface Account {
  id: number;
  code: string;
  name: string;
  category: string;
  type: string;
  isActive: boolean;
  companyId?: number;
  categoryId?: number;
  description?: string;
  parentAccountId?: number | null;
  accountCode?: string;
  accountName?: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  planId: number;
}

export interface AuthResponse {
  success: boolean;
  user: User;
  token: string;
  message?: string;
}

export interface PlanSelection {
  planId: number;
  billingCycle: 'monthly' | 'yearly';
}

// Payroll Types
export interface PayrollPeriod {
  id: number;
  companyId: number;
  fiscalPeriodId: number; // Required, not optional
  periodName: string;
  payDate: string;
  startDate: string;
  endDate: string;
  periodType: 'WEEKLY' | 'MONTHLY' | 'QUARTERLY';
  status: 'OPEN' | 'PROCESSED' | 'APPROVED' | 'PAID' | 'CLOSED';
  totalGrossPay: number;
  totalDeductions: number;
  totalNetPay: number;
  employeeCount: number;
  processedAt?: string;
  processedBy?: string;
  approvedAt?: string;
  approvedBy?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
}

// Payroll Configuration Types
export interface FiscalPeriodPayrollConfigRequest {
  payDate?: string;
  periodType: 'WEEKLY' | 'MONTHLY' | 'QUARTERLY';
  payrollStatus: 'OPEN' | 'PROCESSED' | 'APPROVED' | 'PAID' | 'CLOSED';
}

export interface FiscalPeriodPayrollConfigResponse {
  fiscalPeriodId: number;
  periodName: string;
  startDate: string;
  endDate: string;
  payDate: string | null;
  periodType: 'WEEKLY' | 'MONTHLY' | 'QUARTERLY';
  payrollStatus: 'OPEN' | 'PROCESSED' | 'APPROVED' | 'PAID' | 'CLOSED';
  totalGrossPay: number;
  totalDeductions: number;
  totalNetPay: number;
  employeeCount: number;
  closed: boolean;
}

export interface FiscalPeriodPayrollStatusResponse {
  id: number;
  periodName: string;
  startDate: string;
  endDate: string;
  payDate: string | null;
  payrollStatus: 'OPEN' | 'PROCESSED' | 'APPROVED' | 'PAID' | 'CLOSED';
  totalGrossPay: number;
  totalNetPay: number;
  employeeCount: number;
  closed: boolean;
}

export interface PayrollPeriodCreateRequest {
  companyId: number;
  periodName: string;
  startDate: string;
  endDate: string;
  paymentDate: string;
}

export interface PayrollProcessingResult {
  success: boolean;
  message: string;
  data: {
    processedEmployees: number;
    totalGrossPay: number;
    totalDeductions: number;
    totalNetPay: number;
    payslipsGenerated: number;
  };
}

// Budget Types
export interface Budget {
  id: number;
  companyId: number;
  fiscalPeriodId: number;
  title: string; // Added for component compatibility
  budgetYear: number; // Added for component compatibility
  status: string; // Added for component compatibility
  totalRevenue: number; // Added for component compatibility
  totalExpenses: number; // Added for component compatibility
  name: string;
  description?: string;
  totalBudget: number;
  totalSpent: number;
  categories: BudgetCategory[];
  createdAt: string;
  updatedAt: string;
}

export interface BudgetCategory {
  categoryId: number; // Changed from id to categoryId for component compatibility
  budgetId: number;
  categoryName: string;
  budgeted: number; // Changed from budgetedAmount to budgeted for component compatibility
  actual: number; // Changed from spentAmount to actual for component compatibility
  variance: number;
  variancePercentage: number; // Added for component compatibility
}

export interface BudgetVariance {
  budgetId: number;
  totalBudget: number;
  totalSpent: number;
  totalVariance: number;
  totalBudgeted: number; // Added for component compatibility
  totalActual: number; // Added for component compatibility
  variancePercentage: number; // Added for component compatibility
  categories: BudgetCategory[];
}

// Employee Types
export interface Employee {
  id: number;
  companyId: number;
  employeeNumber: string;
  title?: string;
  firstName: string;
  secondName?: string;
  lastName: string;
  email?: string;
  phone?: string;
  position: string;
  department?: string;
  hireDate?: string;
  terminationDate?: string;
  active: boolean; // Changed from isActive to match API response
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
  employmentType: 'PERMANENT' | 'CONTRACT' | 'TEMPORARY';
  salaryType: 'MONTHLY' | 'WEEKLY' | 'HOURLY' | 'DAILY';
  basicSalary?: number;
  overtimeRate?: number;
  taxNumber?: string;
  taxRebateCode?: string;
  uifNumber?: string;
  medicalAidNumber?: string;
  pensionFundNumber?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
  employeeCode?: string; // Added to match API response
  currentEmployee?: boolean; // Added to match API response
  dateOfBirth?: string; // Added to match API response
  dateEngaged?: string; // Added to match API response
  idNumber?: string; // Added to match API response
  displayName?: string; // Added to match API response
  fullName?: string; // Added to match API response
}

export interface EmployeeCreateRequest {
  companyId: number;
  employeeCode: string;
  title?: string;
  firstName: string;
  secondName?: string;
  lastName: string;
  email?: string;
  phone?: string;
  position: string;
  department?: string;
  hireDate: string;
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
  employmentType: 'PERMANENT' | 'CONTRACT' | 'TEMPORARY';
  salaryType: 'MONTHLY' | 'WEEKLY' | 'HOURLY' | 'DAILY';
  basicSalary?: number;
  overtimeRate?: number;
  taxNumber?: string;
  taxRebateCode?: string;
  uifNumber?: string;
  medicalAidNumber?: string;
  pensionFundNumber?: string;
}

export interface EmployeeUpdateRequest {
  employeeCode?: string;
  title?: string;
  firstName?: string;
  secondName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  position?: string;
  department?: string;
  hireDate?: string;
  terminationDate?: string;
  isActive?: boolean; // Keep isActive for updates as that's what the API expects
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
  employmentType?: 'PERMANENT' | 'CONTRACT' | 'TEMPORARY';
  salaryType?: 'MONTHLY' | 'WEEKLY' | 'HOURLY' | 'DAILY';
  basicSalary?: number;
  overtimeRate?: number;
  taxNumber?: string;
  taxRebateCode?: string;
  uifNumber?: string;
  medicalAidNumber?: string;
  pensionFundNumber?: string;
}

// Error Types
export interface ApiError {
  response?: {
    data?: {
      message?: string;
    };
    status?: number;
  };
  message: string;
}

// Document Management Types
export interface PayrollDocument {
  id: number;
  employeeId: number;
  periodId: number;
  fileName: string;
  fileData: string | null;
  documentType: 'PAYSLIP' | 'TAX_CERTIFICATE' | 'EMPLOYEE_CONTRACT' | 'BANK_STATEMENT' | 'OTHER';
  uploadDate: string;
}

// Journal Entry & Audit Trail Types
export interface JournalEntryLineDTO {
  id: number;
  accountId: number;
  accountCode: string;
  accountName: string;
  description: string;
  debitAmount: number;
  creditAmount: number;
  lineNumber: number;
}

export interface JournalEntryDTO {
  id: number;
  reference: string;
  entryDate: string;
  description: string;
  totalDebit: number;
  totalCredit: number;
  lineCount: number;
  createdBy: string;
  createdAt: string;
  lastModifiedBy: string;
  lastModifiedAt: string;
}

export interface JournalEntryDetailDTO {
  id: number;
  reference: string;
  entryDate: string;
  description: string;
  totalDebit: number;
  totalCredit: number;
  lineCount: number;
  createdBy: string;
  createdAt: string;
  lastModifiedBy: string;
  lastModifiedAt: string;
  companyName: string;
  fiscalPeriodName: string;
  lines: JournalEntryLineDTO[];
}

export interface PaginationMetadata {
  currentPage: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
}

export interface FilterMetadata {
  startDate: string | null;
  endDate: string | null;
  searchTerm: string | null;
  hasFilters: boolean;
}

export interface AuditTrailResponse {
  entries: JournalEntryDTO[];
  pagination: PaginationMetadata;
  filters: FilterMetadata;
}