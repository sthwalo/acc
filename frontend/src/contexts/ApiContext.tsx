import React, { createContext, useMemo } from 'react';
import { ApiService } from '../services/ApiService';
import { serviceRegistry } from '../services/ServiceRegistry';

interface ApiContextType {
  apiService: ApiService;
}

const ApiContext = createContext<ApiContextType | undefined>(undefined);

interface ApiProviderProps {
  children: React.ReactNode;
}

export function ApiProvider({ children }: ApiProviderProps) {
  // Get API service from registry (following backend pattern)
  const apiService = useMemo(() => {
    return serviceRegistry.get<ApiService>('apiService');
  }, []);

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