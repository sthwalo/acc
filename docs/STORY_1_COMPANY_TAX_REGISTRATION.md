# Story 1: Company Tax Registration Framework

**Epic:** VAT Accounting System Reform  
**Story Points:** 8  
**Priority:** Critical  
**Sprint:** 1  
**Dependencies:** None

## User Story
**As a** system administrator  
**I want** companies to have tax registration flags (VAT, Income Tax)  
**So that** the system can apply appropriate tax calculations and validations based on company registration status

## Acceptance Criteria

### AC1: Database Schema Update
- [ ] Add `vat_registered` boolean field to companies table (default: FALSE)
- [ ] Add `income_tax_registered` boolean field to companies table (default: TRUE)  
- [ ] Add `vat_registration_number` varchar(20) field to companies table
- [ ] Add `tax_registration_date` date field to companies table
- [ ] Create database migration script with rollback capability

### AC2: Company Model Enhancement
- [ ] Update `Company.java` with new tax registration fields
- [ ] Add appropriate getters and setters
- [ ] Update `toString()` method to include tax registration status
- [ ] Add validation for VAT registration number format (if provided)

### AC3: Service Layer Integration
- [ ] Extend `CompanyService.java` with tax registration methods:
  - `isVATRegistered(Long companyId)`
  - `updateTaxRegistration(Long companyId, TaxRegistrationInfo info)`
  - `validateVATRegistration(String vatNumber)`
- [ ] Update `CompanyRepository.java` with new field persistence
- [ ] Add tax registration validation logic

### AC4: Console Application Integration  
- [ ] Update company creation wizard to ask for tax registration
- [ ] Add menu option to update existing company tax registration
- [ ] Display tax registration status in company details
- [ ] Add validation prompts for VAT registration number format

### AC5: Application Context Update
- [ ] Register any new services in `ApplicationContext.java`
- [ ] Ensure proper dependency injection for tax-related services
- [ ] Update service initialization order if needed

## Technical Implementation Tasks

### Database Migration
```sql
-- Migration: Add tax registration fields to companies
ALTER TABLE companies 
ADD COLUMN vat_registered BOOLEAN DEFAULT FALSE,
ADD COLUMN income_tax_registered BOOLEAN DEFAULT TRUE,
ADD COLUMN vat_registration_number VARCHAR(20),
ADD COLUMN tax_registration_date DATE;

-- Update existing companies to be income tax registered by default
UPDATE companies SET income_tax_registered = TRUE WHERE income_tax_registered IS NULL;

-- Add indexes for performance
CREATE INDEX idx_companies_vat_registered ON companies(vat_registered);
CREATE INDEX idx_companies_vat_number ON companies(vat_registration_number);
```

### Model Update Example
```java
public class Company {
    // Existing fields...
    private boolean vatRegistered = false;
    private boolean incomeTaxRegistered = true;
    private String vatRegistrationNumber;
    private LocalDate taxRegistrationDate;
    
    // New getters/setters
    public boolean isVATRegistered() { return vatRegistered; }
    public void setVATRegistered(boolean vatRegistered) { this.vatRegistered = vatRegistered; }
    
    // Validation methods
    public boolean isValidVATNumber(String vatNumber) {
        return vatNumber != null && vatNumber.matches("\\d{10}");
    }
}
```

### Service Integration Example
```java
public class CompanyService {
    public boolean isVATRegistered(Long companyId) {
        Company company = repository.getCompanyById(companyId);
        return company != null && company.isVATRegistered();
    }
    
    public void updateTaxRegistration(Long companyId, boolean vatRegistered, String vatNumber) {
        Company company = repository.getCompanyById(companyId);
        if (company != null) {
            company.setVATRegistered(vatRegistered);
            if (vatRegistered && company.isValidVATNumber(vatNumber)) {
                company.setVATRegistrationNumber(vatNumber);
                company.setTaxRegistrationDate(LocalDate.now());
            }
            repository.updateCompany(company);
        }
    }
}
```

## Files to Modify

### Core Files
- `app/src/main/java/fin/model/Company.java`
- `app/src/main/java/fin/service/CompanyService.java`
- `app/src/main/java/fin/repository/CompanyRepository.java`

### Configuration Files  
- `app/src/main/java/fin/context/ApplicationContext.java`

### UI Files
- `app/src/main/java/fin/ConsoleApplication.java`
- `app/src/main/java/fin/ui/ConsoleMenu.java` (if tax registration menu needed)

### New Files to Create
- `scripts/migration_001_add_tax_registration.sql`
- `scripts/rollback_001_tax_registration.sql`
- `app/src/test/java/fin/service/CompanyTaxRegistrationTest.java`

## Testing Strategy

### Unit Tests
- Test Company model with new fields
- Test CompanyService tax registration methods
- Test CompanyRepository persistence of new fields
- Test VAT number validation logic

### Integration Tests
- Test complete company creation with tax registration
- Test company update with tax registration changes
- Test database persistence and retrieval
- Test console application tax registration flow

### Test Data
```java
// Test companies with different tax registration scenarios
Company vatRegisteredCompany = new Company("VAT Registered Corp", true, true, "1234567890");
Company incomeTaxOnlyCompany = new Company("Small Business", false, true, null);
Company noTaxCompany = new Company("Non-profit", false, false, null);
```

## Definition of Done

### Functional Requirements
- [ ] Companies can be marked as VAT registered or not
- [ ] Companies can be marked as Income Tax registered or not  
- [ ] VAT registration number can be stored and validated
- [ ] Tax registration date is automatically set when VAT registration is enabled
- [ ] Console application allows setting tax registration during company creation
- [ ] Console application allows updating tax registration for existing companies

### Technical Requirements
- [ ] All unit tests pass (minimum 90% code coverage for new code)
- [ ] Integration tests validate complete tax registration workflow
- [ ] Database migration runs successfully and is reversible
- [ ] No breaking changes to existing company functionality
- [ ] Code follows project coding standards and passes checkstyle

### Quality Assurance
- [ ] Manual testing of complete tax registration workflow
- [ ] Validation that existing companies are not affected by changes
- [ ] Performance testing shows no degradation in company operations
- [ ] Documentation updated for new tax registration features

## Risk Assessment

### High Risk
- **Database Migration:** Changes to core company table could affect existing data
- **Backward Compatibility:** Must ensure existing company functionality continues to work

### Medium Risk  
- **Console UI Changes:** New prompts might confuse existing users
- **Service Dependencies:** New tax validation might impact other services

### Mitigation Strategies
- Create comprehensive backup before migration
- Test migration on copy of production data first
- Keep rollback scripts ready
- Extensive testing of existing functionality
- Gradual rollout with ability to disable new features

## Acceptance Testing Scenarios

### Scenario 1: Create New VAT-Registered Company
```
GIVEN I am creating a new company
WHEN I choose to register for VAT
AND I provide a valid VAT number "1234567890"
THEN the company should be created with VAT registration enabled
AND the VAT number should be stored
AND the tax registration date should be set to today
```

### Scenario 2: Update Existing Company Tax Registration
```
GIVEN I have an existing company that is not VAT registered
WHEN I update the company to be VAT registered
AND I provide a valid VAT number
THEN the company should be updated with VAT registration
AND all existing company data should remain unchanged
```

### Scenario 3: Invalid VAT Number Validation
```
GIVEN I am registering a company for VAT
WHEN I provide an invalid VAT number "ABC123"
THEN the system should reject the registration
AND prompt me to enter a valid 10-digit VAT number
```

## Ready for Development

This story is fully scoped and ready for development. The implementation should start with the database migration, followed by model updates, then service layer integration, and finally console application updates.

**Estimated Development Time:** 2-3 days  
**Estimated Testing Time:** 1-2 days  
**Total Story Duration:** 3-5 days