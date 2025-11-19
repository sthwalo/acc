# Frontend Coding Standards - FIN Financial Management System

## Overview
This document outlines the coding standards and architectural patterns for the FIN frontend application. These standards are designed to align with the backend architecture principles outlined in `copilot-instructions.md` and ensure consistency, maintainability, and production readiness.

## Table of Contents
1. [Architecture Principles](#architecture-principles)
2. [Container-First Development](#container-first-development)
3. [Service Layer Architecture](#service-layer-architecture)
4. [Error Handling Standards](#error-handling-standards)
5. [Component Patterns](#component-patterns)
6. [State Management](#state-management)
7. [TypeScript Standards](#typescript-standards)
8. [Testing Standards](#testing-standards)
9. [Build and Deployment](#build-and-deployment)
10. [Code Quality](#code-quality)

## Architecture Principles

### 1. Database-First Policy (MANDATORY)
**Rule**: The frontend must treat the backend API as the single source of truth. NO fallback data or hardcoded business values allowed.

```typescript
// ✅ CORRECT: Always fetch from API
const loadCompanies = async () => {
  try {
    const companies = await apiService.getCompanies();
    setCompanies(companies);
  } catch (error) {
    // Throw clear error - NO FALLBACK DATA
    throw new Error(`Failed to load companies from database. Please ensure backend is running and database is accessible. Error: ${error.message}`);
  }
};

// ❌ WRONG: Never use fallback data
const loadCompanies = async () => {
  try {
    const companies = await apiService.getCompanies();
    setCompanies(companies);
  } catch (error) {
    // FORBIDDEN: No fallback data allowed
    setCompanies(DEFAULT_COMPANIES); // ❌ VIOLATION
  }
};
```

### 2. Container-First Development (MANDATORY)
**Rule**: All frontend development MUST use the containerized backend. No direct localhost connections.

```typescript
// vite.config.ts - REQUIRED configuration
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // Containerized backend
        changeOrigin: true,
        secure: false
      }
    }
  }
});
```

### 3. Service Registration Pattern
**Rule**: Follow backend pattern of registering services in a central context.

```typescript
// services/index.ts - Central service registry
export class ServiceRegistry {
  private static instance: ServiceRegistry;
  private services: Map<string, any> = new Map();

  static getInstance(): ServiceRegistry {
    if (!ServiceRegistry.instance) {
      ServiceRegistry.instance = new ServiceRegistry();
    }
    return ServiceRegistry.instance;
  }

  register<T>(name: string, service: T): void {
    this.services.set(name, service);
  }

  get<T>(name: string): T {
    const service = this.services.get(name);
    if (!service) {
      throw new Error(`Service '${name}' not found in registry`);
    }
    return service as T;
  }
}

// Usage in App.tsx
import { ServiceRegistry } from './services';

const serviceRegistry = ServiceRegistry.getInstance();
serviceRegistry.register('apiService', apiService);
serviceRegistry.register('authService', authService);
```

## Service Layer Architecture

### API Service Standards
```typescript
// services/api.ts
import axios, { AxiosError } from 'axios';

export class ApiService {
  private client = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
    timeout: 30000,
  });

  constructor() {
    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    // Request interceptor for auth
    this.client.interceptors.request.use((config) => {
      const token = localStorage.getItem('auth_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        if (error.response?.status === 401) {
          // Handle unauthorized
          localStorage.removeItem('auth_token');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  // Business methods
  async getCompanies(): Promise<Company[]> {
    try {
      const response = await this.client.get<ApiResponse<Company[]>>('/companies');
      return response.data.data;
    } catch (error) {
      throw new Error(`Failed to fetch companies: ${error.message}`);
    }
  }
}
```

### Repository Pattern
```typescript
// repositories/CompanyRepository.ts
export class CompanyRepository {
  constructor(private apiService: ApiService) {}

  async findAll(): Promise<Company[]> {
    return this.apiService.getCompanies();
  }

  async findById(id: number): Promise<Company> {
    return this.apiService.getCompany(id);
  }
}
```

## Error Handling Standards

### 1. Fail-Fast Policy
**Rule**: Throw exceptions immediately when data cannot be loaded. No silent failures.

```typescript
// ✅ CORRECT: Fail fast with clear error
const loadData = async () => {
  try {
    const data = await apiService.getData();
    if (!data || data.length === 0) {
      throw new Error('No data found in database. Please ensure data exists in the backend database.');
    }
    return data;
  } catch (error) {
    // Re-throw with context
    throw new Error(`Data loading failed: ${error.message}`);
  }
};

// ❌ WRONG: Silent failure
const loadData = async () => {
  try {
    const data = await apiService.getData();
    return data || []; // ❌ VIOLATION
  } catch (error) {
    console.error(error); // ❌ VIOLATION
    return []; // ❌ VIOLATION
  }
};
```

### 2. Error Boundary Components
```typescript
// components/ErrorBoundary.tsx
import React from 'react';

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends React.Component<
  React.PropsWithChildren<{}>,
  ErrorBoundaryState
> {
  constructor(props: React.PropsWithChildren<{}>) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-boundary">
          <h2>Something went wrong</h2>
          <p>{this.state.error?.message}</p>
          <button onClick={() => window.location.reload()}>
            Reload Application
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
```

## Component Patterns

### 1. Component Structure
```typescript
// components/CompaniesView.tsx
import { useState, useEffect } from 'react';
import { Company } from '../types/api';
import { useApi } from '../hooks/useApi';

interface CompaniesViewProps {
  onCompanySelect: (company: Company) => void;
}

export default function CompaniesView({ onCompanySelect }: CompaniesViewProps) {
  const [companies, setCompanies] = useState<Company[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const api = useApi();

  useEffect(() => {
    loadCompanies();
  }, []);

  const loadCompanies = async () => {
    try {
      setLoading(true);
      const data = await api.getCompanies();
      if (!data || data.length === 0) {
        throw new Error('No companies found in database. Please add companies through the backend.');
      }
      setCompanies(data);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load companies');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="loading">Loading companies...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="companies-view">
      {companies.map(company => (
        <div key={company.id} className="company-card">
          <h3>{company.name}</h3>
          <button onClick={() => onCompanySelect(company)}>
            Select
          </button>
        </div>
      ))}
    </div>
  );
}
```

### 2. Custom Hooks
```typescript
// hooks/useApi.ts
import { useContext } from 'react';
import { ApiContext } from '../contexts/ApiContext';

export function useApi() {
  const context = useContext(ApiContext);
  if (!context) {
    throw new Error('useApi must be used within ApiProvider');
  }
  return context.apiService;
}

// hooks/useCompanies.ts
import { useState, useEffect } from 'react';
import { Company } from '../types/api';
import { useApi } from './useApi';

export function useCompanies() {
  const [companies, setCompanies] = useState<Company[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const api = useApi();

  const loadCompanies = async () => {
    try {
      setLoading(true);
      const data = await api.getCompanies();
      if (!data || data.length === 0) {
        throw new Error('No companies found. Please add companies to the database.');
      }
      setCompanies(data);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load companies');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCompanies();
  }, []);

  return { companies, loading, error, refetch: loadCompanies };
}
```

## State Management

### Context Provider Pattern
```typescript
// contexts/ApiContext.tsx
import React, { createContext, useMemo } from 'react';
import { ApiService } from '../services/ApiService';

interface ApiContextType {
  apiService: ApiService;
}

const ApiContext = createContext<ApiContextType | undefined>(undefined);

interface ApiProviderProps {
  children: React.ReactNode;
}

export function ApiProvider({ children }: ApiProviderProps) {
  const apiService = useMemo(() => new ApiService(), []);

  const value = useMemo(() => ({
    apiService,
  }), [apiService]);

  return (
    <ApiContext.Provider value={value}>
      {children}
    </ApiContext.Provider>
  );
}

export { ApiContext };
```

## TypeScript Standards

### 1. Strict Type Checking
```typescript
// tsconfig.json - REQUIRED settings
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "strictFunctionTypes": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedIndexedAccess": true,
    "exactOptionalPropertyTypes": true
  }
}
```

### 2. Interface Definitions
```typescript
// types/api.ts
export interface Company {
  readonly id: number;
  readonly name: string;
  readonly registration_number: string;
  readonly tax_number: string;
  readonly address: string;
  readonly contact_email: string;
  readonly contact_phone: string;
  readonly bank_name?: string;
  readonly account_number?: string;
  readonly account_type?: string;
  readonly branch_code?: string;
  readonly vat_registered?: boolean;
  readonly created_at: string;
  readonly updated_at: string;
}

export interface ApiResponse<T> {
  readonly success: boolean;
  readonly data: T;
  readonly message?: string;
  readonly timestamp: number;
}
```

## Testing Standards

### 1. Component Testing
```typescript
// __tests__/CompaniesView.test.tsx
import { render, screen, waitFor } from '@testing-library/react';
import { ApiProvider } from '../contexts/ApiContext';
import CompaniesView from '../components/CompaniesView';

const mockApiService = {
  getCompanies: jest.fn(),
};

jest.mock('../services/ApiService', () => ({
  ApiService: jest.fn().mockImplementation(() => mockApiService),
}));

describe('CompaniesView', () => {
  it('displays companies from API', async () => {
    const mockCompanies = [
      { id: 1, name: 'Test Company', registration_number: '123' },
    ];
    mockApiService.getCompanies.mockResolvedValue(mockCompanies);

    render(
      <ApiProvider>
        <CompaniesView onCompanySelect={() => {}} />
      </ApiProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Test Company')).toBeInTheDocument();
    });
  });

  it('shows error when no companies found', async () => {
    mockApiService.getCompanies.mockResolvedValue([]);

    render(
      <ApiProvider>
        <CompaniesView onCompanySelect={() => {}} />
      </ApiProvider>
    );

    await waitFor(() => {
      expect(screen.getByText(/No companies found/)).toBeInTheDocument();
    });
  });
});
```

## Build and Deployment

### 1. Build Verification (MANDATORY)
```json
// package.json
{
  "scripts": {
    "build": "tsc -b && vite build",
    "build:verify": "npm run build && npm run lint && npm run test",
    "precommit": "npm run build:verify"
  }
}
```

### 2. Environment Configuration
```typescript
// config/environments.ts
export const environments = {
  development: {
    apiUrl: 'http://localhost:8080/api/v1',
    enableDevTools: true,
  },
  production: {
    apiUrl: '/api/v1',
    enableDevTools: false,
  },
} as const;

export type Environment = keyof typeof environments;
```

## Code Quality

### 1. ESLint Configuration
```javascript
// eslint.config.js
export default [
  {
    files: ['**/*.ts', '**/*.tsx'],
    languageOptions: {
      parser: '@typescript-eslint/parser',
    },
    plugins: {
      '@typescript-eslint': typescriptEslint,
      'react-hooks': reactHooks,
    },
    rules: {
      // TypeScript strict rules
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-unused-vars': 'error',
      '@typescript-eslint/prefer-const': 'error',

      // React rules
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',

      // Custom rules for FIN
      'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'warn',
    },
  },
];
```

### 2. Pre-commit Hooks
```json
// .husky/pre-commit
#!/usr/bin/env sh
. "$(dirname -- "$0")/_/husky.sh"

npm run build:verify
```

## Development Workflow

### 1. Container-First Development
```bash
# Start backend in container
docker-compose up fin-app

# Start frontend (proxies to container)
npm run dev

# Test against container
curl http://localhost:3000/api/v1/health
```

### 2. Code Change Protocol
1. Make code changes
2. Run build verification: `npm run build:verify`
3. Test functionality against containerized backend
4. Get user confirmation: "Changes tested and working"
5. Commit with descriptive message

### 3. Collaboration Standards
- Always test against containerized backend
- Never commit unverified code
- Document all API changes
- Update types when backend changes

## Compliance Checklist

### Pre-Commit Verification
- [ ] Build passes: `npm run build`
- [ ] Lint passes: `npm run lint`
- [ ] Tests pass: `npm run test`
- [ ] Tested against containerized backend
- [ ] No hardcoded fallback data
- [ ] Error handling follows fail-fast policy
- [ ] User confirmed changes work

### Code Review Standards
- [ ] Follows service registration pattern
- [ ] Uses proper TypeScript types
- [ ] Implements error boundaries
- [ ] No console.log in production code
- [ ] Component follows established patterns
- [ ] API calls have proper error handling

---

**Document Version**: 1.0
**Last Updated**: 8 November 2025
**Aligned with**: copilot-instructions.md