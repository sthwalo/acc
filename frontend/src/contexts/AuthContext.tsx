import React, { createContext, useState, useEffect, useMemo, useCallback } from 'react';
import type { ReactNode } from 'react';
import { serviceRegistry } from '../services/ServiceRegistry';
import { ApiService } from '../services/ApiService';
import type { User } from '../types/api';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (userData: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    planId: number;
    paypalOrderId?: string;
    paypalCaptureId?: string;
  }) => Promise<void>;
  logout: () => void;
  handleAuthError: () => void;
  selectPlan: (planId: number, billingCycle: 'monthly' | 'yearly') => Promise<void>;
}

// Helper function to check if JWT token is expired
const isTokenExpired = (token: string): boolean => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const exp = payload.exp;
    if (!exp) return true;

    // Convert exp to milliseconds and compare with current time
    const expiryTime = exp * 1000; // JWT exp is in seconds
    return Date.now() >= expiryTime;
  } catch (error) {
    console.warn('Error parsing token:', error);
    return true; // Consider invalid tokens as expired
  }
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Export the context for use in hooks
export { AuthContext };

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Get API service from registry (following backend pattern)
  const apiService = useMemo(() => {
    return serviceRegistry.get<ApiService>('apiService');
  }, []);

  const checkAuthStatus = useCallback(async () => {
    try {
      const token = localStorage.getItem('auth_token');
      if (token) {
        // First validate token format and expiry locally before API call
        if (isTokenExpired(token)) {
          console.warn('Token expired locally, clearing...');
          localStorage.removeItem('auth_token');
          setUser(null);
          setIsLoading(false);
          return;
        }

        const currentUser = await apiService.getCurrentUser();
        setUser(currentUser);
      } else {
        setUser(null);
      }
    } catch (error: unknown) {
      console.warn('Authentication check failed:', error);

      // Only clear token for authentication errors, not network errors
      if (error && typeof error === 'object' && 'response' in error) {
        const axiosError = error as { response?: { status?: number } };
        if (axiosError.response?.status === 401 || axiosError.response?.status === 403) {
          console.warn('Authentication failed, clearing token');
          handleAuthError();
        }
      } else {
        // Network error or server error - keep token and retry later
        console.warn('Network/server error, keeping token for retry');
        setUser(null); // Show login screen but keep token
      }
    } finally {
      setIsLoading(false);
    }
  }, [apiService]);

  useEffect(() => {
    checkAuthStatus();

    // Set up periodic token validation (every 5 minutes)
    const interval = setInterval(() => {
      const token = localStorage.getItem('auth_token');
      if (token && isTokenExpired(token)) {
        console.warn('Token expired during session, logging out...');
        handleAuthError();
      }
    }, 5 * 60 * 1000); // 5 minutes

    return () => clearInterval(interval);
  }, [checkAuthStatus]);

  const login = async (email: string, password: string) => {
    const response = await apiService.login({ email, password });
    localStorage.setItem('auth_token', response.token);
    setUser(response.user);
  };

  const register = async (userData: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    planId: number;
    paypalOrderId?: string;
    paypalCaptureId?: string;
  }) => {
    const response = await apiService.register({
      email: userData.email,
      password: userData.password,
      firstName: userData.firstName,
      lastName: userData.lastName,
      planId: userData.planId,
      paypalOrderId: userData.paypalOrderId,
      paypalCaptureId: userData.paypalCaptureId,
    });
    localStorage.setItem('auth_token', response.token);
    setUser(response.user);
  };

  const logout = () => {
    localStorage.removeItem('auth_token');
    setUser(null);
  };

  const handleAuthError = () => {
    console.warn('Authentication error detected, clearing session');
    localStorage.removeItem('auth_token');
    setUser(null);
  };

  const selectPlan = async (planId: number, billingCycle: 'monthly' | 'yearly') => {
    const response = await apiService.selectPlan({ planId, billingCycle });
    localStorage.setItem('auth_token', response.token);
    setUser(response.user);
  };

  const value: AuthContextType = {
    user,
    isAuthenticated: !!user,
    isLoading,
    login,
    register,
    logout,
    handleAuthError,
    selectPlan,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
