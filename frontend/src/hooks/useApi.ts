import { serviceRegistry } from '../services/ServiceRegistry';
import { ApiService } from '../services/ApiService';

export function useApi(): ApiService {
  return serviceRegistry.get<ApiService>('apiService');
}