# 🚀 TypeScript Frontend Integration Strategy

## System Overview

Your **drimacc** repository contains a **world-class TypeScript-based accounting frontend** that perfectly complements your Java backend system. This integration will create a powerful full-stack financial management platform.

## 🎯 Frontend Architecture Analysis

### **Current Frontend Tech Stack:**
- ✅ **React 18.2.0** - Modern React with hooks and concurrent features
- ✅ **TypeScript 5.2.2** - Strong typing and excellent developer experience
- ✅ **Vite 4.4.9** - Lightning-fast build tool and dev server
- ✅ **Tailwind CSS 3.3.3** - Utility-first styling
- ✅ **Zustand** - Lightweight state management
- ✅ **Tesseract.js** - OCR processing capabilities
- ✅ **TensorFlow.js** - Machine learning integration
- ✅ **ExcelJS & jsPDF** - Advanced export capabilities

### **Frontend Features (Already Implemented):**
- 📄 **Document Upload & OCR** - `DocumentUpload.tsx`
- 📊 **Financial Reports** - `FinancialReports.tsx`
- 🔄 **Bank Reconciliation** - `Reconciliation.tsx`
- 💰 **Multi-Currency Support** - `CurrencySelector.tsx`
- 📈 **Chart of Accounts** - `ChartOfAccounts.tsx`
- 📝 **Transaction Management** - `TransactionList.tsx`
- 🔔 **Notification System** - `Notifications.tsx`
- 📊 **Tax Compliance** - `TaxCompliance.tsx`
- 💾 **Data Export** - Excel, PDF, CSV export utilities

## 🔗 Integration Strategy

### **Phase 1: Backend API Connector (2-3 hours)**

#### 1.1 Create TypeScript API Client
```typescript
// Create src/services/api/financeApi.ts
import { Transaction, Account, Company, FiscalPeriod } from '../../types';

export interface ApiResponse<T> {
  data: T;
  status: number;
  message?: string;
}

export interface PaginatedResponse<T> extends ApiResponse<T[]> {
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
}

export class FinanceApiClient {
  private baseUrl: string;
  private headers: Record<string, string>;

  constructor(baseUrl = 'http://localhost:8080/api/v1') {
    this.baseUrl = baseUrl;
    this.headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
  }

  private async request<T>(
    endpoint: string, 
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseUrl}${endpoint}`;
    
    const config: RequestInit = {
      headers: this.headers,
      ...options,
    };

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();
      return {
        data,
        status: response.status,
      };
    } catch (error) {
      console.error(`API Request failed: ${endpoint}`, error);
      throw error;
    }
  }

  // Company Management
  async getCompanies(): Promise<ApiResponse<Company[]>> {
    return this.request<Company[]>('/companies');
  }

  async createCompany(company: Omit<Company, 'id' | 'createdAt'>): Promise<ApiResponse<Company>> {
    return this.request<Company>('/companies', {
      method: 'POST',
      body: JSON.stringify(company),
    });
  }

  async getCompanyById(id: number): Promise<ApiResponse<Company>> {
    return this.request<Company>(`/companies/${id}`);
  }

  // Fiscal Periods
  async getFiscalPeriods(companyId: number): Promise<ApiResponse<FiscalPeriod[]>> {
    return this.request<FiscalPeriod[]>(`/companies/${companyId}/fiscal-periods`);
  }

  async createFiscalPeriod(
    companyId: number, 
    period: Omit<FiscalPeriod, 'id' | 'createdAt'>
  ): Promise<ApiResponse<FiscalPeriod>> {
    return this.request<FiscalPeriod>(`/companies/${companyId}/fiscal-periods`, {
      method: 'POST',
      body: JSON.stringify(period),
    });
  }

  // Accounts
  async getAccounts(companyId: number): Promise<ApiResponse<Account[]>> {
    return this.request<Account[]>(`/companies/${companyId}/accounts`);
  }

  async createAccount(
    companyId: number, 
    account: Omit<Account, 'id' | 'created' | 'modified'>
  ): Promise<ApiResponse<Account>> {
    return this.request<Account>(`/companies/${companyId}/accounts`, {
      method: 'POST',
      body: JSON.stringify(account),
    });
  }

  // Transactions
  async getTransactions(
    companyId: number, 
    page = 1, 
    limit = 50
  ): Promise<PaginatedResponse<Transaction>> {
    return this.request<Transaction[]>(`/companies/${companyId}/transactions?page=${page}&limit=${limit}`);
  }

  async createTransaction(
    companyId: number, 
    transaction: Omit<Transaction, 'id' | 'created' | 'modified'>
  ): Promise<ApiResponse<Transaction>> {
    return this.request<Transaction>(`/companies/${companyId}/transactions`, {
      method: 'POST',
      body: JSON.stringify(transaction),
    });
  }

  // CSV Import
  async importCsv(companyId: number, file: File): Promise<ApiResponse<{ processed: number; errors: string[] }>> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${this.baseUrl}/companies/${companyId}/import-csv`, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`Upload failed: ${response.statusText}`);
    }

    const data = await response.json();
    return { data, status: response.status };
  }

  // PDF Bank Statement Processing
  async processBankStatement(companyId: number, file: File): Promise<ApiResponse<Transaction[]>> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${this.baseUrl}/companies/${companyId}/process-statement`, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`Processing failed: ${response.statusText}`);
    }

    const data = await response.json();
    return { data, status: response.status };
  }

  // Financial Reports
  async getTrialBalance(companyId: number, fiscalPeriodId: number): Promise<ApiResponse<any>> {
    return this.request(`/companies/${companyId}/reports/trial-balance?fiscal_period_id=${fiscalPeriodId}`);
  }

  async getIncomeStatement(companyId: number, fiscalPeriodId: number): Promise<ApiResponse<any>> {
    return this.request(`/companies/${companyId}/reports/income-statement?fiscal_period_id=${fiscalPeriodId}`);
  }

  async getBalanceSheet(companyId: number, fiscalPeriodId: number): Promise<ApiResponse<any>> {
    return this.request(`/companies/${companyId}/reports/balance-sheet?fiscal_period_id=${fiscalPeriodId}`);
  }

  // Authentication (for future use)
  setAuthToken(token: string): void {
    this.headers['Authorization'] = `Bearer ${token}`;
  }

  removeAuthToken(): void {
    delete this.headers['Authorization'];
  }
}

// Create singleton instance
export const financeApi = new FinanceApiClient();
```

#### 1.2 Update TypeScript Types for Backend Integration
```typescript
// Update src/types/index.ts to match backend entities
export interface Company {
  id: number;
  name: string;
  registrationNumber?: string;
  taxNumber?: string;
  address?: string;
  contactEmail?: string;
  contactPhone?: string;
  createdAt: string; // ISO date string
}

export interface FiscalPeriod {
  id: number;
  companyId: number;
  periodName: string;
  startDate: string; // ISO date string
  endDate: string; // ISO date string
  isClosed: boolean;
  createdAt: string;
}

export interface Account {
  id: number;
  accountCode: string;
  accountName: string;
  description?: string;
  categoryId: number;
  parentAccountId?: number;
  companyId: number;
  isActive: boolean;
  balance: number;
  currency: string;
  type: 'asset' | 'liability' | 'equity' | 'revenue' | 'expense';
  created: string;
  modified: string;
}

export interface BankTransaction {
  id: number;
  companyId: number;
  bankAccountId?: number;
  fiscalPeriodId: number;
  transactionDate: string; // ISO date string
  details: string;
  debitAmount?: number;
  creditAmount?: number;
  balance: number;
  serviceFee: boolean;
  accountNumber?: string;
  statementPeriod?: string;
  sourceFile?: string;
  createdAt: string;
}

export interface AccountCategory {
  id: number;
  name: string;
  description?: string;
  accountType: AccountType;
  companyId: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AccountType {
  id: number;
  code: string;
  name: string;
  normalBalance: 'D' | 'C';
  description?: string;
}

// API Response Types
export interface TrialBalanceEntry {
  accountCode: string;
  accountName: string;
  debitAmount: number;
  creditAmount: number;
}

export interface FinancialStatement {
  statementType: 'TRIAL_BALANCE' | 'INCOME_STATEMENT' | 'BALANCE_SHEET';
  companyId: number;
  fiscalPeriodId: number;
  generatedDate: string;
  entries: TrialBalanceEntry[];
  totals: {
    totalDebits: number;
    totalCredits: number;
    netIncome?: number;
    totalAssets?: number;
    totalLiabilities?: number;
    totalEquity?: number;
  };
}
```

### **Phase 2: Store Integration (2-3 hours)**

#### 2.1 Create Backend-Integrated Stores
```typescript
// Create src/store/backendIntegratedStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { financeApi } from '../services/api/financeApi';
import { Company, FiscalPeriod, Account, BankTransaction, FinancialStatement } from '../types';

interface BackendState {
  // State
  isLoading: boolean;
  error: string | null;
  
  // Company State
  companies: Company[];
  selectedCompany: Company | null;
  
  // Fiscal Period State
  fiscalPeriods: FiscalPeriod[];
  selectedFiscalPeriod: FiscalPeriod | null;
  
  // Account State
  accounts: Account[];
  
  // Transaction State
  transactions: BankTransaction[];
  transactionsPagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
  
  // Report State
  currentReport: FinancialStatement | null;
  
  // Actions
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  
  // Company Actions
  fetchCompanies: () => Promise<void>;
  createCompany: (company: Omit<Company, 'id' | 'createdAt'>) => Promise<void>;
  selectCompany: (company: Company) => void;
  
  // Fiscal Period Actions
  fetchFiscalPeriods: (companyId: number) => Promise<void>;
  createFiscalPeriod: (companyId: number, period: Omit<FiscalPeriod, 'id' | 'createdAt'>) => Promise<void>;
  selectFiscalPeriod: (period: FiscalPeriod) => void;
  
  // Account Actions
  fetchAccounts: (companyId: number) => Promise<void>;
  createAccount: (companyId: number, account: Omit<Account, 'id' | 'created' | 'modified'>) => Promise<void>;
  
  // Transaction Actions
  fetchTransactions: (companyId: number, page?: number, limit?: number) => Promise<void>;
  createTransaction: (companyId: number, transaction: Omit<BankTransaction, 'id' | 'createdAt'>) => Promise<void>;
  importCsvFile: (companyId: number, file: File) => Promise<{ processed: number; errors: string[] }>;
  processBankStatement: (companyId: number, file: File) => Promise<BankTransaction[]>;
  
  // Report Actions
  generateTrialBalance: (companyId: number, fiscalPeriodId: number) => Promise<void>;
  generateIncomeStatement: (companyId: number, fiscalPeriodId: number) => Promise<void>;
  generateBalanceSheet: (companyId: number, fiscalPeriodId: number) => Promise<void>;
}

export const useBackendStore = create<BackendState>()(
  persist(
    (set, get) => ({
      // Initial State
      isLoading: false,
      error: null,
      companies: [],
      selectedCompany: null,
      fiscalPeriods: [],
      selectedFiscalPeriod: null,
      accounts: [],
      transactions: [],
      transactionsPagination: {
        page: 1,
        limit: 50,
        total: 0,
        totalPages: 0,
      },
      currentReport: null,

      // Utility Actions
      setLoading: (loading) => set({ isLoading: loading }),
      setError: (error) => set({ error }),

      // Company Actions
      fetchCompanies: async () => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.getCompanies();
          set({ companies: response.data, isLoading: false });
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
        }
      },

      createCompany: async (company) => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.createCompany(company);
          const { companies } = get();
          set({ 
            companies: [...companies, response.data],
            selectedCompany: response.data,
            isLoading: false 
          });
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
          throw error;
        }
      },

      selectCompany: (company) => {
        set({ selectedCompany: company });
        // Auto-load related data
        get().fetchFiscalPeriods(company.id);
        get().fetchAccounts(company.id);
      },

      // Fiscal Period Actions
      fetchFiscalPeriods: async (companyId) => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.getFiscalPeriods(companyId);
          set({ fiscalPeriods: response.data, isLoading: false });
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
        }
      },

      createFiscalPeriod: async (companyId, period) => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.createFiscalPeriod(companyId, period);
          const { fiscalPeriods } = get();
          set({ 
            fiscalPeriods: [...fiscalPeriods, response.data],
            selectedFiscalPeriod: response.data,
            isLoading: false 
          });
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
          throw error;
        }
      },

      selectFiscalPeriod: (period) => {
        set({ selectedFiscalPeriod: period });
        // Auto-load transactions for this period
        const { selectedCompany } = get();
        if (selectedCompany) {
          get().fetchTransactions(selectedCompany.id);
        }
      },

      // Account Actions
      fetchAccounts: async (companyId) => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.getAccounts(companyId);
          set({ accounts: response.data, isLoading: false });
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
        }
      },

      createAccount: async (companyId, account) => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.createAccount(companyId, account);
          const { accounts } = get();
          set({ 
            accounts: [...accounts, response.data],
            isLoading: false 
          });
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
          throw error;
        }
      },

      // Transaction Actions
      fetchTransactions: async (companyId, page = 1, limit = 50) => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.getTransactions(companyId, page, limit);
          set({ 
            transactions: response.data,
            transactionsPagination: response.pagination || {
              page,
              limit,
              total: response.data.length,
              totalPages: Math.ceil(response.data.length / limit),
            },
            isLoading: false 
          });
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
        }
      },

      createTransaction: async (companyId, transaction) => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.createTransaction(companyId, transaction);
          const { transactions } = get();
          set({ 
            transactions: [response.data, ...transactions],
            isLoading: false 
          });
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
          throw error;
        }
      },

      importCsvFile: async (companyId, file) => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.importCsv(companyId, file);
          set({ isLoading: false });
          // Refresh transactions after import
          await get().fetchTransactions(companyId);
          return response.data;
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
          throw error;
        }
      },

      processBankStatement: async (companyId, file) => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.processBankStatement(companyId, file);
          const { transactions } = get();
          set({ 
            transactions: [...response.data, ...transactions],
            isLoading: false 
          });
          return response.data;
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
          throw error;
        }
      },

      // Report Actions
      generateTrialBalance: async (companyId, fiscalPeriodId) => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.getTrialBalance(companyId, fiscalPeriodId);
          set({ 
            currentReport: response.data,
            isLoading: false 
          });
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
        }
      },

      generateIncomeStatement: async (companyId, fiscalPeriodId) => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.getIncomeStatement(companyId, fiscalPeriodId);
          set({ 
            currentReport: response.data,
            isLoading: false 
          });
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
        }
      },

      generateBalanceSheet: async (companyId, fiscalPeriodId) => {
        set({ isLoading: true, error: null });
        try {
          const response = await financeApi.getBalanceSheet(companyId, fiscalPeriodId);
          set({ 
            currentReport: response.data,
            isLoading: false 
          });
        } catch (error) {
          set({ error: error instanceof Error ? error.message : 'Unknown error', isLoading: false });
        }
      },
    }),
    {
      name: 'finance-backend-store',
      partialize: (state) => ({
        selectedCompany: state.selectedCompany,
        selectedFiscalPeriod: state.selectedFiscalPeriod,
      }),
    }
  )
);
```

### **Phase 3: Component Updates (3-4 hours)**

#### 3.1 Update DocumentUpload Component for Backend Integration
```typescript
// Update src/components/DocumentUpload/DocumentUpload.tsx
import React, { useState, useRef } from 'react';
import { useBackendStore } from '../../store/backendIntegratedStore';
import { useNotificationStore } from '../../store/notificationStore';

export function DocumentUpload() {
  const [file, setFile] = useState<File | null>(null);
  const [processing, setProcessing] = useState(false);
  const [uploadMode, setUploadMode] = useState<'csv' | 'pdf' | 'auto'>('auto');
  
  const { 
    selectedCompany, 
    selectedFiscalPeriod,
    importCsvFile, 
    processBankStatement,
    isLoading 
  } = useBackendStore();
  
  const { addNotification } = useNotificationStore();

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0];
      setFile(selectedFile);
      
      // Auto-detect file type
      if (selectedFile.name.toLowerCase().includes('.csv')) {
        setUploadMode('csv');
      } else if (selectedFile.name.toLowerCase().includes('.pdf')) {
        setUploadMode('pdf');
      }
    }
  };

  const processFile = async () => {
    if (!file || !selectedCompany) {
      addNotification('Please select a file and company', 'error');
      return;
    }

    setProcessing(true);
    try {
      if (uploadMode === 'csv') {
        const result = await importCsvFile(selectedCompany.id, file);
        addNotification(
          `Successfully processed ${result.processed} transactions. ${result.errors.length} errors.`,
          result.errors.length > 0 ? 'warning' : 'success'
        );
        
        if (result.errors.length > 0) {
          console.error('Import errors:', result.errors);
        }
      } else if (uploadMode === 'pdf') {
        const transactions = await processBankStatement(selectedCompany.id, file);
        addNotification(
          `Successfully extracted ${transactions.length} transactions from PDF`,
          'success'
        );
      }
      
      setFile(null);
      setUploadMode('auto');
    } catch (error) {
      console.error('File processing error:', error);
      addNotification(
        'Error processing file: ' + (error instanceof Error ? error.message : 'Unknown error'),
        'error'
      );
    } finally {
      setProcessing(false);
    }
  };

  const isProcessDisabled = !file || !selectedCompany || processing || isLoading;

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm">
      <h2 className="text-xl font-semibold mb-4">Document Upload</h2>
      
      {!selectedCompany && (
        <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-md">
          <p className="text-yellow-800 text-sm">Please select a company first</p>
        </div>
      )}
      
      <div className="space-y-4">
        {/* File Upload */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Select File (CSV or PDF)
          </label>
          <input
            type="file"
            accept=".csv,.pdf"
            onChange={handleFileChange}
            className="w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4
              file:rounded-md file:border-0 file:text-sm file:font-medium
              file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100"
          />
        </div>

        {/* Upload Mode Selection */}
        {file && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Processing Mode
            </label>
            <select
              value={uploadMode}
              onChange={(e) => setUploadMode(e.target.value as 'csv' | 'pdf' | 'auto')}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
            >
              <option value="auto">Auto-detect</option>
              <option value="csv">CSV Import</option>
              <option value="pdf">PDF Bank Statement</option>
            </select>
          </div>
        )}

        {/* Selected File Info */}
        {file && (
          <div className="p-3 bg-gray-50 rounded-md">
            <p className="text-sm text-gray-600">
              <strong>Selected:</strong> {file.name} ({Math.round(file.size / 1024)} KB)
            </p>
            <p className="text-sm text-gray-600">
              <strong>Mode:</strong> {uploadMode === 'csv' ? 'CSV Import' : 
                                    uploadMode === 'pdf' ? 'PDF Processing' : 'Auto-detect'}
            </p>
          </div>
        )}

        {/* Process Button */}
        <button
          onClick={processFile}
          disabled={isProcessDisabled}
          className={`w-full py-2 px-4 rounded-md font-medium transition-colors ${
            isProcessDisabled
              ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
              : 'bg-blue-600 text-white hover:bg-blue-700'
          }`}
        >
          {processing || isLoading ? 'Processing...' : 'Process File'}
        </button>

        {/* Current Context Display */}
        {selectedCompany && (
          <div className="text-xs text-gray-500 mt-2">
            Company: {selectedCompany.name}
            {selectedFiscalPeriod && ` | Period: ${selectedFiscalPeriod.periodName}`}
          </div>
        )}
      </div>
    </div>
  );
}
```

#### 3.2 Create Company/Period Selector Component
```typescript
// Create src/components/CompanySelector/CompanySelector.tsx
import React, { useEffect, useState } from 'react';
import { useBackendStore } from '../../store/backendIntegratedStore';
import { useNotificationStore } from '../../store/notificationStore';
import { Company, FiscalPeriod } from '../../types';

export function CompanySelector() {
  const [showCreateCompany, setShowCreateCompany] = useState(false);
  const [showCreatePeriod, setShowCreatePeriod] = useState(false);
  const [newCompany, setNewCompany] = useState({
    name: '',
    registrationNumber: '',
    taxNumber: '',
    address: '',
    contactEmail: '',
    contactPhone: '',
  });
  const [newPeriod, setNewPeriod] = useState({
    periodName: '',
    startDate: '',
    endDate: '',
  });

  const {
    companies,
    selectedCompany,
    fiscalPeriods,
    selectedFiscalPeriod,
    isLoading,
    error,
    fetchCompanies,
    createCompany,
    selectCompany,
    createFiscalPeriod,
    selectFiscalPeriod,
  } = useBackendStore();

  const { addNotification } = useNotificationStore();

  useEffect(() => {
    fetchCompanies();
  }, [fetchCompanies]);

  const handleCreateCompany = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createCompany(newCompany);
      setShowCreateCompany(false);
      setNewCompany({
        name: '',
        registrationNumber: '',
        taxNumber: '',
        address: '',
        contactEmail: '',
        contactPhone: '',
      });
      addNotification('Company created successfully', 'success');
    } catch (error) {
      addNotification('Failed to create company', 'error');
    }
  };

  const handleCreatePeriod = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCompany) return;

    try {
      await createFiscalPeriod(selectedCompany.id, {
        ...newPeriod,
        companyId: selectedCompany.id,
        isClosed: false,
      });
      setShowCreatePeriod(false);
      setNewPeriod({
        periodName: '',
        startDate: '',
        endDate: '',
      });
      addNotification('Fiscal period created successfully', 'success');
    } catch (error) {
      addNotification('Failed to create fiscal period', 'error');
    }
  };

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-md p-4">
        <p className="text-red-800">Error: {error}</p>
        <button
          onClick={fetchCompanies}
          className="mt-2 text-sm text-red-600 hover:text-red-800"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm">
      <h2 className="text-xl font-semibold mb-4">Company & Period Selection</h2>
      
      <div className="space-y-4">
        {/* Company Selection */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Company
          </label>
          <div className="flex gap-2">
            <select
              value={selectedCompany?.id || ''}
              onChange={(e) => {
                const company = companies.find(c => c.id === parseInt(e.target.value));
                if (company) selectCompany(company);
              }}
              className="flex-1 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              disabled={isLoading}
            >
              <option value="">Select a company...</option>
              {companies.map((company) => (
                <option key={company.id} value={company.id}>
                  {company.name}
                </option>
              ))}
            </select>
            <button
              onClick={() => setShowCreateCompany(true)}
              className="px-3 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm"
            >
              New
            </button>
          </div>
        </div>

        {/* Fiscal Period Selection */}
        {selectedCompany && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Fiscal Period
            </label>
            <div className="flex gap-2">
              <select
                value={selectedFiscalPeriod?.id || ''}
                onChange={(e) => {
                  const period = fiscalPeriods.find(p => p.id === parseInt(e.target.value));
                  if (period) selectFiscalPeriod(period);
                }}
                className="flex-1 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                disabled={isLoading}
              >
                <option value="">Select a period...</option>
                {fiscalPeriods.map((period) => (
                  <option key={period.id} value={period.id}>
                    {period.periodName} ({period.startDate} to {period.endDate})
                  </option>
                ))}
              </select>
              <button
                onClick={() => setShowCreatePeriod(true)}
                className="px-3 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm"
              >
                New
              </button>
            </div>
          </div>
        )}

        {/* Current Selection Display */}
        {selectedCompany && (
          <div className="p-3 bg-blue-50 rounded-md text-sm">
            <p><strong>Selected Company:</strong> {selectedCompany.name}</p>
            {selectedFiscalPeriod && (
              <p><strong>Selected Period:</strong> {selectedFiscalPeriod.periodName}</p>
            )}
          </div>
        )}
      </div>

      {/* Create Company Modal */}
      {showCreateCompany && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold mb-4">Create New Company</h3>
            <form onSubmit={handleCreateCompany} className="space-y-4">
              <input
                type="text"
                placeholder="Company Name *"
                value={newCompany.name}
                onChange={(e) => setNewCompany({ ...newCompany, name: e.target.value })}
                className="w-full rounded-md border-gray-300 shadow-sm"
                required
              />
              <input
                type="text"
                placeholder="Registration Number"
                value={newCompany.registrationNumber}
                onChange={(e) => setNewCompany({ ...newCompany, registrationNumber: e.target.value })}
                className="w-full rounded-md border-gray-300 shadow-sm"
              />
              <input
                type="text"
                placeholder="Tax Number"
                value={newCompany.taxNumber}
                onChange={(e) => setNewCompany({ ...newCompany, taxNumber: e.target.value })}
                className="w-full rounded-md border-gray-300 shadow-sm"
              />
              <input
                type="email"
                placeholder="Contact Email"
                value={newCompany.contactEmail}
                onChange={(e) => setNewCompany({ ...newCompany, contactEmail: e.target.value })}
                className="w-full rounded-md border-gray-300 shadow-sm"
              />
              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={isLoading}
                  className="flex-1 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
                >
                  Create
                </button>
                <button
                  type="button"
                  onClick={() => setShowCreateCompany(false)}
                  className="flex-1 py-2 bg-gray-300 text-gray-700 rounded-md hover:bg-gray-400"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Create Fiscal Period Modal */}
      {showCreatePeriod && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white p-6 rounded-lg max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold mb-4">Create New Fiscal Period</h3>
            <form onSubmit={handleCreatePeriod} className="space-y-4">
              <input
                type="text"
                placeholder="Period Name (e.g., FY 2024-2025) *"
                value={newPeriod.periodName}
                onChange={(e) => setNewPeriod({ ...newPeriod, periodName: e.target.value })}
                className="w-full rounded-md border-gray-300 shadow-sm"
                required
              />
              <input
                type="date"
                placeholder="Start Date *"
                value={newPeriod.startDate}
                onChange={(e) => setNewPeriod({ ...newPeriod, startDate: e.target.value })}
                className="w-full rounded-md border-gray-300 shadow-sm"
                required
              />
              <input
                type="date"
                placeholder="End Date *"
                value={newPeriod.endDate}
                onChange={(e) => setNewPeriod({ ...newPeriod, endDate: e.target.value })}
                className="w-full rounded-md border-gray-300 shadow-sm"
                required
              />
              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={isLoading}
                  className="flex-1 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
                >
                  Create
                </button>
                <button
                  type="button"
                  onClick={() => setShowCreatePeriod(false)}
                  className="flex-1 py-2 bg-gray-300 text-gray-700 rounded-md hover:bg-gray-400"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
```

## 🚀 **Integration Benefits**

### **Immediate Gains:**
- ✅ **Real Backend Integration** - Direct connection to your Java financial system
- ✅ **TypeScript Safety** - Strong typing throughout the integration
- ✅ **Production-Ready UI** - Sophisticated React components already built
- ✅ **Advanced Features** - OCR, ML, multi-currency, exports all working

### **Combined System Power:**
- ✅ **Java Backend** - Robust financial engine with PostgreSQL
- ✅ **TypeScript Frontend** - Modern, type-safe user interface
- ✅ **Real-time Sync** - API integration for live data updates
- ✅ **Full Feature Set** - Complete accounting system from backend to frontend

## 🎯 **Next Steps**

### **Immediate Actions (Today):**
1. **Add REST API to backend** (2-3 hours)
2. **Implement API client in frontend** (1-2 hours)
3. **Test basic integration** (1 hour)

### **This Week:**
1. **Complete component updates** for backend integration
2. **Deploy both systems** to production
3. **Test full-stack functionality**

### **Next Month:**
1. **Advanced features** - Real-time updates, authentication
2. **Performance optimization** - Caching, lazy loading
3. **Mobile responsiveness** - Enhanced mobile experience



