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
  created_at: string;
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
  details: string;
  statementPeriod: string | null;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  timestamp: number;
}

export interface HealthResponse {
  status: string;
  database: string;
  timestamp: number;
}

export interface UploadResponse {
  success: boolean;
  message: string;
  files_processed: number;
  transactions_found: number;
  timestamp: number;
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