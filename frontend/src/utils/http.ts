import axios, { type AxiosInstance, type AxiosResponse } from 'axios';
import type { ApiEnvelope } from '../types/api';

const API_BASE = (import.meta.env.VITE_API_BASE as string) || 'http://localhost:8080/api/v1';

export const axiosInstance: AxiosInstance = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' }
});

export function setAuthToken(token: string | null) {
  if (token) {
    axiosInstance.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  } else {
    delete axiosInstance.defaults.headers.common['Authorization'];
  }
}

/**
 * Unwraps the project's ApiResponse<T> envelope.
 * Throws an Error when the envelope indicates failure.
 */
export async function unwrapApiResponse<T>(promise: Promise<AxiosResponse>): Promise<T> {
  const res = await promise;
  if (!res || !res.data) {
    throw new Error('Invalid API response');
  }

  const envelope = res.data as ApiEnvelope<T>;
  if (!envelope) throw new Error('Malformed API envelope');

  if (!envelope.success) {
    throw new Error(envelope.message || 'API request failed');
  }

  return envelope.data as T;
}

export default axiosInstance;
