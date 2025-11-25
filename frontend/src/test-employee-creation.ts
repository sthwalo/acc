import { ApiService } from './services/ApiService';

// Example usage of the updated createEmployee method
async function testCreateEmployee() {
  const apiService = new ApiService();

  // Example employee data matching the Employee interface
  const newEmployee = {
    companyId: 1,
    employeeNumber: 'EMP001', // This will be mapped to employeeCode in the request
    firstName: 'John',
    lastName: 'Doe',
    position: 'Software Developer',
    hireDate: '2024-01-15',
    isActive: true, // Required field
    employmentType: 'PERMANENT' as const,
    salaryType: 'MONTHLY' as const,
    basicSalary: 50000,
    email: 'john.doe@company.com',
    phone: '+27123456789',
    taxNumber: '1234567890123', // Valid 13-digit South African tax number
    bankName: 'ABSA',
    accountNumber: '1234567890',
    branchCode: '632005',
    accountType: 'SAVINGS',
    addressLine1: '123 Main Street',
    city: 'Johannesburg',
    province: 'Gauteng',
    postalCode: '2000',
    country: 'South Africa'
  };

  try {
    console.log('Creating employee with data:', newEmployee);
    const createdEmployee = await apiService.createEmployee(newEmployee);
    console.log('Employee created successfully:', createdEmployee);
  } catch (error) {
    console.error('Failed to create employee:', error);
  }
}

// Export for potential use in components
export { testCreateEmployee };