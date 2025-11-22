import { ApiService } from './ApiService';
// import { telemetryService } from './TelemetryService';

/**
 * ServiceRegistry - Central service registry following backend ApplicationContext pattern
 * All services must be registered here for dependency injection
 */
export class ServiceRegistry {
  private static instance: ServiceRegistry;
  private services: Map<string, unknown> = new Map();

  private constructor() {
    this.initializeServices();
  }

  static getInstance(): ServiceRegistry {
    if (!ServiceRegistry.instance) {
      ServiceRegistry.instance = new ServiceRegistry();
    }
    return ServiceRegistry.instance;
  }

  private initializeServices(): void {
    // Register core services following backend pattern
    // Register telemetry first so other services can use it
    // this.register('telemetryService', telemetryService);
    this.register('apiService', new ApiService());

    // Future services will be registered here
    // this.register('authService', new AuthService(this.get('apiService')));
    // this.register('companyService', new CompanyService(this.get('apiService')));
  }

  register<T>(name: string, service: T): void {
    if (this.services.has(name)) {
      throw new Error(`Service '${name}' is already registered`);
    }
    this.services.set(name, service);
  }

  get<T>(name: string): T {
    const service = this.services.get(name);
    if (!service) {
      throw new Error(`Service '${name}' not found in registry. Please ensure it is registered in ServiceRegistry.initializeServices()`);
    }
    return service as T;
  }

  has(name: string): boolean {
    return this.services.has(name);
  }

  // Get all registered service names (for debugging/testing)
  getRegisteredServices(): string[] {
    return Array.from(this.services.keys());
  }
}

// Export singleton instance
export const serviceRegistry = ServiceRegistry.getInstance();
