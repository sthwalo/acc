# FIN Financial Management System - Company Setup & Registration Task Document

## Executive Summary
This document outlines the comprehensive task plan for implementing the **complete user onboarding flow**: registration → plan selection → company setup → chart of accounts initialization → fiscal period setup → transaction rules setup, with all operations properly bound to company and fiscal period, and feature limitations enforced based on pricing plans.

**Primary Objectives:**
1. **Endpoint Testing**: Ensure all API endpoints return valid responses
2. **Registration Flow**: User registration with plan selection and login
3. **Company Setup**: Full company initialization (chart of accounts, fiscal periods, rules)
4. **Data Binding**: All operations scoped to company + fiscal period
5. **Plan Enforcement**: Feature limitations based on selected pricing plan
6. **CRUD Operations**: Complete create/read/update/delete for all business entities

**Key Requirements:**
- Chart of accounts based on company line of trade
- Rule creation engine with full CRUD operations
- Company + fiscal period binding on all operations
- Plan-based feature limitations
- Complete API endpoint validation

## User Onboarding Flow

### Primary User Journey
The complete user onboarding workflow follows this sequence:

1. **User Registration & Login**
   - User registers with email/password
   - User selects pricing plan during registration
   - User logs in with JWT authentication
   - FIN validates plan selection and user status

2. **Full Company Setup Process**
   - User creates company with business details
   - User initializes chart of accounts based on company line of trade
   - User creates fiscal periods for the company
   - User sets up transaction mapping rules
   - All data is bound to the specific company and its fiscal periods

3. **Plan-Based Feature Limitations**
   - All subsequent functionalities are limited by the selected pricing plan
   - API endpoints enforce plan restrictions (transaction limits, user counts, feature access)
   - System prevents access to premium features for basic plans

### Key Constraints
- **Company Binding**: All business data and operations must be scoped to a specific company
- **Fiscal Period Binding**: All financial operations must be constrained to fiscal periods
- **Plan Enforcement**: Feature access must be controlled by subscription plan
- **Data Isolation**: Multi-tenant architecture with proper data separation

## API Endpoint Testing Requirements

### Testing Objectives
- **All endpoints must return valid responses** (HTTP 200/201 for success, proper error codes for failures)
- **Complete CRUD coverage** for all business entities
- **Authentication enforcement** on protected endpoints
- **Company scoping validation** on all company-specific operations
- **Plan limitation enforcement** on premium features

### Testing Scope
- User registration and authentication endpoints
- Company management endpoints
- Chart of accounts CRUD operations
- Transaction rule management endpoints
- Fiscal period operations
- Plan-related endpoints
- All business logic endpoints

## Current System Analysis

### ✅ EXISTING COMPONENTS (Ready to Use)

#### 1. User Management
- **Entities**: `User`, `UserCompany` (many-to-many relationship)
- **Services**: `UserService`, `AuthController` (registration/login), `PayPalController`
- **Features**: Complete user registration with PayPal integration, plan selection, authentication, JWT tokens, proper company scoping
- **Status**: ✅ Complete and functional with PayPal checkout and user-specific company access

#### 2. Company Management
- **Entities**: `Company` (with banking details, VAT registration, contact info, audit fields)
- **Services**: `CompanyService`, `CompanyController`
- **Features**: CRUD operations for companies with proper user-specific access control, audit trail tracking (createdAt, updatedAt, createdBy, updatedBy)
- **Status**: ✅ Complete with comprehensive company data model, user isolation, and audit trails

#### 3. Pricing Plans
- **Entities**: `Plan` (with pricing, features, limits)
- **Services**: `PlanService`, `PlanRepository`
- **Features**: Plan definitions with feature access control
- **Status**: ✅ Complete pricing plan structure

#### 4. Fiscal Periods
- **Entities**: `FiscalPeriod`, `FiscalPeriodSummary`
- **Services**: `FiscalPeriodController`, `FiscalPeriodBoundaryValidator`
- **Features**: Fiscal period management with validation
- **Status**: ✅ Complete fiscal period system

#### 5. Chart of Accounts (Basic)
- **Entities**: `Account`, `AccountCategory`, `AccountType`
- **Services**: Basic account CRUD via `CompanyAccountController`
- **Features**: Account management with company binding
- **Status**: ✅ Basic structure exists, needs template system

#### 6. Transaction Rules (Basic)
- **Entities**: `TransactionMappingRule` (with match types: CONTAINS, STARTS_WITH, ENDS_WITH, EQUALS, REGEX)
- **Services**: Basic rule management
- **Features**: Rule-based transaction classification
- **Status**: ✅ Basic structure exists, needs CRUD engine

#### 7. Validation Framework
- **Components**: `ModelValidator<T>`, `ValidationResult`, `ValidationError`
- **Validators**: `BankTransactionValidator`, `EmployeeValidator`
- **Features**: Generic validation system
- **Status**: ✅ Framework exists, needs expansion

### ❌ MISSING COMPONENTS (Need Implementation)

#### 1. Registration Workflow Integration
- **Status**: ✅ FULLY IMPLEMENTED - Complete User → Company → Fiscal Period binding security
- **Features**: PayPal checkout integration with plan selection, automatic UserCompany relationship creation, access control enforcement, data isolation
- **Security**: Authentication required for all company operations, users only access their own companies, proper multi-tenant architecture
- **Gap**: Full company setup wizard (chart of accounts, fiscal periods, rules) still needed for complete onboarding

#### 2. Chart of Accounts Templates
- **Gap**: No templates based on company line of business
- **Required**: Industry-specific chart of accounts templates
- **Impact**: Companies need proper starting accounts

#### 3. Rule Creation Engine
- **Gap**: No CRUD interface for transaction mapping rules
- **Required**: Full rule management system with company binding
- **Impact**: Users cannot customize transaction classification

#### 4. Company-Fiscal Period Binding
- **Gap**: Not all operations properly constrained to company + fiscal period
- **Required**: All data operations must include company_id and fiscal_period_id
- **Impact**: Data isolation and proper multi-tenancy

#### 5. Plan-Based Feature Limitations
- **Gap**: No enforcement of plan limits in API endpoints
- **Required**: Feature access control based on selected plan
- **Impact**: Cannot enforce subscription tiers

#### 6. Complete API Endpoint Testing
- **Gap**: Endpoints exist but not systematically tested
- **Required**: Comprehensive endpoint validation
- **Impact**: Cannot guarantee system reliability

## Progress Assessment

### **Completion Status**: ~25% of total tasks completed

### **✅ COMPLETED COMPONENTS**

#### **Security & User Management Foundation** 
- **✅ Registration Workflow Integration**: "FULLY IMPLEMENTED" - Complete User → Company → Fiscal Period binding security
- **✅ Authentication Required**: All `/api/**` endpoints now require JWT authentication  
- **✅ Access Control**: UserCompany relationships automatically created, access validation on all operations
- **✅ Data Isolation**: Complete separation between user data, no cross-user access
- **✅ PayPal Integration**: Registration process with plan selection working
- **✅ User-Specific Access**: Frontend shows only user's companies, backend enforces access control

#### **Company Audit Fields (Recently Completed)**
- **✅ Company Entity**: Added `createdAt`, `updatedAt`, `createdBy`, `updatedBy` fields
- **✅ CompanyService**: Updated to set audit fields on create/update operations  
- **✅ CompanyController**: Modified to pass user context to service methods
- **✅ Database Migration**: V8 migration added user tracking columns to companies table
- **✅ Frontend**: CompaniesView updated to display audit information

#### **Company CRUD Operations (Just Completed)**
- **✅ CompanyUpdateDTO**: Created dedicated DTO for update operations following SOC principles
- **✅ Hibernate Cascade Fix**: Resolved "collection with cascade='all-delete-orphan'" error by preserving userCompanies relationships
- **✅ Service Layer Refactoring**: CompanyService.updateCompany() now uses DTO pattern with proper relationship preservation
- **✅ Controller Integration**: CompanyController.updateCompany() accepts DTO and passes to service
- **✅ Architecture Compliance**: Implementation follows SOC, DRY, and small methods principles
- **✅ Testing Validation**: All tests pass, compilation successful, server running without errors
#### **Fiscal Period Setup Logic (Active Development)**
- **✅ Year-End Month Selection**: Users select month (Jan-Dec) representing fiscal year-end (when fiscal year closes)
- **✅ Fiscal Year Calculation**: System calculates 12-month fiscal period from following month to selected month
- **✅ Date Logic**: Start = First day of month (M+1) in year (Y-1), End = Last day of month M in year Y
- **✅ Example**: November 2024 year-end → Fiscal year: Dec 1, 2023 - Nov 30, 2024
- **✅ UI Flow**: Month dropdown → Year selection → Auto-calculate and display dates
- **✅ Validation**: Prevent overlapping fiscal periods per company, ensure proper date ranges
### **❌ REMAINING COMPONENTS**

#### **PHASE 1: Database Schema Extensions** (HIGH PRIORITY)
- ❌ **Task 1.1**: Chart of Accounts Templates Table
- ❌ **Task 1.2**: Rule Templates Table  
- ❌ **Task 1.3**: Company Setup Status Table

#### **PHASE 2: Service Layer Extensions** (HIGH PRIORITY)
- ❌ **Task 2.2**: Company Setup Service (Guided company initialization)
- ❌ **Task 2.3**: Chart of Accounts Template Service
- ❌ **Task 2.4**: Rule Engine Service (Complete CRUD for transaction rules)

#### **PHASE 3: Controller Layer Extensions** (HIGH PRIORITY)
- ❌ **Task 3.2**: Company Setup Controller (Setup wizard API)
- ❌ **Task 3.4**: Rules Controller (Rule management API)

#### **PHASE 4: Validation & Security** (MEDIUM PRIORITY)
- ❌ **Task 4.1**: Company-Fiscal Period Validators (All operations properly scoped)
- ❌ **Task 4.2**: Plan-Based Access Control (Feature limitations by subscription)

#### **PHASE 5: Frontend Integration Points** (LOW PRIORITY)
- ❌ **Task 5.2**: Company Setup Wizard (Multi-step setup UI)

#### **PHASE 6: Testing & Validation** (LOW PRIORITY)
- ❌ **Task 6.1**: API Endpoint Testing Suite
- ❌ **Task 6.2**: End-to-End Workflow Testing

### **🎯 Current State Assessment**

**What's Working:**
- ✅ User registration with plan selection
- ✅ Company creation with audit trails and user tracking
- ✅ Basic authentication and access control
- ✅ Multi-tenant data isolation
- ✅ Timestamp and user tracking in company records
- ✅ **Company CRUD Operations**: Full create/read/update/delete functionality with proper relationship management

**What's Missing:**
- ❌ Industry-specific chart of accounts templates
- ❌ Transaction rule CRUD operations  
- ❌ Company setup wizard (chart of accounts, rules, fiscal periods)
- ❌ Plan-based feature limitations
- ❌ Complete API endpoint testing

**Next Priority**: **ACTIVE DEVELOPMENT** - Fiscal Period Setup Logic implementation following SOC/DRY principles. Focus on clean date calculation service, proper company binding, and maintainable code structure.

## Detailed Task Breakdown

### PHASE 1: Database Schema Extensions

#### Task 1.1: Chart of Accounts Templates Table
**Objective**: Create database table for industry-specific chart of accounts templates
**Requirements**:
- Table: `chart_of_accounts_templates`
- Fields: `id`, `industry_code`, `industry_name`, `account_code`, `account_name`, `category_id`, `is_required`, `sort_order`
- Relationship: Templates → Accounts (many-to-many via template_id)
**Entities**: New `ChartOfAccountsTemplate` entity
**Impact**: Enables industry-specific account setup

#### Task 1.2: Rule Templates Table
**Objective**: Create database table for standard rule templates
**Requirements**:
- Table: `rule_templates`
- Fields: `id`, `industry_code`, `rule_name`, `match_type`, `match_value`, `target_account_code`, `priority`, `is_active`
- Relationship: Templates → TransactionMappingRules (via template_id)
**Entities**: New `RuleTemplate` entity
**Impact**: Enables industry-specific rule setup

#### Task 1.3: Company Setup Status Table
**Objective**: Track company initialization progress
**Requirements**:
- Table: `company_setup_status`
- Fields: `company_id`, `setup_step`, `completed_at`, `completed_by`
- Steps: `USER_REGISTERED`, `PLAN_SELECTED`, `COMPANY_CREATED`, `CHART_OF_ACCOUNTS_INITIALIZED`, `RULES_INITIALIZED`, `FISCAL_PERIOD_CREATED`, `SETUP_COMPLETE`
**Entities**: New `CompanySetupStatus` entity
**Impact**: Enables setup progress tracking

### PHASE 2: Service Layer Extensions

#### Task 2.1: Registration Service Enhancement
**Objective**: Extend UserService for complete registration workflow
**Requirements**:
- Method: `registerUserWithPlan(UserRegistrationDto)`
- Integration: User creation → Plan assignment → Initial company setup
- Validation: Email uniqueness, password strength, plan availability
**Services**: Extend `UserService`
**Impact**: Complete registration experience

#### Task 2.2: Company Setup Service
**Objective**: Create dedicated service for company initialization
**Requirements**:
- Method: `initializeCompany(CompanySetupRequest)`
- Steps: Company creation → Chart of accounts → Rules → Fiscal period
- Validation: Required fields, business logic constraints
**Services**: New `CompanySetupService`
**Impact**: Guided company setup process

#### Task 2.3: Chart of Accounts Template Service
**Objective**: Service for managing industry-specific account templates
**Requirements**:
- Method: `getTemplatesByIndustry(String industryCode)`
- Method: `initializeAccountsFromTemplate(Long companyId, String industryCode)`
- Validation: Template existence, account code uniqueness
**Services**: New `ChartOfAccountsTemplateService`
**Impact**: Industry-appropriate account setup

#### Task 2.4: Rule Engine Service
**Objective**: Complete CRUD service for transaction mapping rules
**Requirements**:
- Methods: `createRule()`, `updateRule()`, `deleteRule()`, `getRulesByCompany()`
- Validation: Rule uniqueness, account existence, company binding
- Templates: `initializeRulesFromTemplate(Long companyId, String industryCode)`
**Services**: Extend existing rule services
**Impact**: Customizable transaction classification

### PHASE 3: Controller Layer Extensions

#### Task 3.1: Registration Controller Enhancement
**Objective**: Complete registration API endpoints
**Requirements**:
- Endpoint: `POST /api/v1/auth/register-with-plan`
- Request: User details + plan selection + initial company info
- Response: User created, JWT token, setup status
**Controllers**: Extend `AuthController`
**Impact**: Single-step registration with plan

#### Task 3.2: Company Setup Controller
**Objective**: REST API for company initialization workflow
**Requirements**:
- Endpoint: `POST /api/v1/companies/{companyId}/setup`
- Endpoint: `GET /api/v1/companies/{companyId}/setup/status`
- Steps: Chart of accounts, rules, fiscal period creation
**Controllers**: New `CompanySetupController`
**Impact**: API-driven setup process

#### Task 3.3: Chart of Accounts Controller Enhancement
**Objective**: Complete CRUD operations for accounts
**Requirements**:
- Endpoints: Full CRUD for accounts with company binding
- Templates: `GET /api/v1/accounts/templates/{industryCode}`
- Initialization: `POST /api/v1/accounts/initialize/{companyId}`
**Controllers**: Extend `CompanyAccountController`
**Impact**: Complete account management

#### Task 3.4: Rules Controller
**Objective**: CRUD API for transaction mapping rules
**Requirements**:
- Endpoints: Full CRUD for rules with company binding
- Templates: `GET /api/v1/rules/templates/{industryCode}`
- Initialization: `POST /api/v1/rules/initialize/{companyId}`
**Controllers**: New `TransactionRuleController`
**Impact**: Rule management interface

### PHASE 4: Validation & Security

#### Task 4.1: Company-Fiscal Period Validators
**Objective**: Ensure all operations are properly scoped
**Requirements**:
- Validator: `CompanyFiscalPeriodValidator`
- Checks: Company existence, fiscal period validity, user access
- Integration: All business operations
**Validators**: New validator classes
**Impact**: Data security and isolation

#### Task 4.2: Plan-Based Access Control
**Objective**: Enforce subscription limits
**Requirements**:
- Interceptor: Plan limit checking on API calls
- Features: Transaction count, user count, storage limits
- Integration: All premium features
**Security**: New `@PlanLimit` annotation
**Impact**: Subscription enforcement

### PHASE 5: Frontend Integration Points

#### Task 5.1: Registration Flow UI
**Objective**: Complete user onboarding interface
**Requirements**:
- Components: Registration form with plan selection
- Flow: Register → Select Plan → Create Company → Setup Complete
- Validation: Real-time form validation
**Frontend**: Registration components
**Impact**: User-friendly onboarding

#### Task 5.2: Company Setup Wizard
**Objective**: Step-by-step company configuration
**Requirements**:
- Wizard: Multi-step setup (industry, accounts, rules, fiscal period)
- Progress: Visual progress indicator
- Validation: Step-by-step validation
**Frontend**: Setup wizard components
**Impact**: Guided company setup

### PHASE 6: Testing & Validation

#### Task 6.1: API Endpoint Testing Suite
**Objective**: Comprehensive endpoint validation
**Requirements**:
- Tests: All endpoints return valid responses
- Scenarios: Success cases, error cases, edge cases
- Automation: Integration test suite
**Testing**: New test classes
**Impact**: System reliability assurance

#### Task 6.2: End-to-End Workflow Testing
**Objective**: Complete user journey validation
**Requirements**:
- Test: Register → Setup Company → Use Features
- Data: Test data isolation and cleanup
- Performance: Response time validation
**Testing**: E2E test scenarios
**Impact**: Workflow reliability

## Implementation Priority Matrix

### HIGH PRIORITY (Foundation)
1. **Task 1.1**: Chart of Accounts Templates Table
2. **Task 2.2**: Company Setup Service
3. **Task 3.2**: Company Setup Controller
4. **Task 4.1**: Company-Fiscal Period Validators

### MEDIUM PRIORITY (Enhancement)
5. **Task 1.2**: Rule Templates Table
6. **Task 2.4**: Rule Engine Service
7. **Task 3.4**: Rules Controller
8. **Task 4.2**: Plan-Based Access Control

### LOW PRIORITY (Polish)
9. **Task 1.3**: Company Setup Status Table
10. **Task 2.1**: Registration Service Enhancement
11. **Task 2.3**: Chart of Accounts Template Service
12. **Task 3.1**: Registration Controller Enhancement
13. **Task 3.3**: Chart of Accounts Controller Enhancement
14. **Task 5.1**: Registration Flow UI
15. **Task 5.2**: Company Setup Wizard
16. **Task 6.1**: API Endpoint Testing Suite
17. **Task 6.2**: End-to-End Workflow Testing

## Success Criteria

### Functional Requirements
- ✅ Users can register and select pricing plans
- ✅ Companies can be created with industry-specific setup
- ✅ Chart of accounts initializes based on line of business
- ✅ Transaction rules are created and customizable
- ✅ All operations are bound to company + fiscal period
- ✅ Plan limits are enforced across all features
- ✅ All API endpoints return valid responses

### Technical Requirements
- ✅ Database schema supports multi-tenant architecture
- ✅ Services follow single responsibility principle
- ✅ Controllers provide RESTful API design
- ✅ Validation framework prevents invalid data
- ✅ Security enforces proper access control
- ✅ Tests cover all critical paths

### Business Requirements
- ✅ Complete user onboarding experience
- ✅ Industry-appropriate initial setup
- ✅ Scalable multi-company architecture
- ✅ Subscription-based feature access
- ✅ Data isolation and security
- ✅ Reliable system performance

## Risk Assessment

### HIGH RISK
- **Data Binding Complexity**: Ensuring all operations properly scope to company + fiscal period
- **Template Management**: Creating and maintaining industry-specific templates
- **Plan Enforcement**: Complex business logic for feature limitations

### MEDIUM RISK
- **API Consistency**: Maintaining consistent endpoint patterns across all controllers
- **Validation Coverage**: Ensuring all edge cases are properly validated
- **Performance**: Template initialization for large chart of accounts

### LOW RISK
- **UI Components**: Standard form/validation patterns
- **Testing**: Established testing frameworks and patterns
- **Documentation**: Clear API documentation standards

## Dependencies

### External Dependencies
- PostgreSQL database (✅ Available)
- JWT authentication (✅ Implemented)
- Validation framework (✅ Implemented)
- Spring Boot/Spring Data JPA (✅ Available)

### Internal Dependencies
- User management system (✅ Complete)
- Company management (✅ Complete)
- Fiscal period system (✅ Complete)
- Basic account/rule structures (✅ Complete)

## Next Steps

1. **Review & Approval**: Review this task document and confirm scope
2. **Priority Selection**: Choose starting tasks based on business needs
3. **Implementation Planning**: Break down Phase 1 tasks into specific code changes
4. **Development**: Begin with database schema extensions
5. **Testing**: Validate each component as implemented
6. **Integration**: Connect components into complete workflows

---

**Document Version**: 1.4
**Created**: December 13, 2025
**Updated**: December 13, 2025
**Author**: Immaculate Nyoni
**Review Status**: Updated - Company CRUD Operations Fully Functional

**Changelog v1.4:**
- ✅ **Company CRUD Operations**: Full create/read/update/delete functionality implemented with DTO pattern
- ✅ **CompanyUpdateDTO**: Created dedicated DTO following SOC principles for update operations
- ✅ **Hibernate Cascade Fix**: Resolved "collection with cascade='all-delete-orphan'" error by preserving userCompanies relationships
- ✅ **Service Layer Refactoring**: CompanyService.updateCompany() refactored to use DTO with proper relationship preservation
- ✅ **Controller Integration**: CompanyController.updateCompany() updated to accept DTO and pass to service
- ✅ **Architecture Compliance**: Implementation follows SOC, DRY, and small methods principles
- ✅ **Testing Validation**: All tests pass, compilation successful, server running without errors
- ✅ **Progress Update**: Completion status increased from ~20% to ~25%

**Changelog v1.3:**
- ✅ **Company Audit Fields**: Added createdAt, updatedAt, createdBy, updatedBy fields to Company entity
- ✅ **Audit Trail Service**: CompanyService updated to set audit fields on create/update operations
- ✅ **User Context Integration**: CompanyController modified to pass user context to service methods
- ✅ **Database Migration**: V8 migration added user tracking columns (created_by, updated_by) to companies table
- ✅ **Frontend Audit Display**: CompaniesView updated to display audit information instead of "Not available"
- ✅ **Git Integration**: All changes committed and pushed to main branch

**Changelog v1.2:**
- ✅ **Security Implementation**: User → Company → Fiscal Period binding fully enforced
- ✅ **Authentication Required**: All /api/** endpoints now require JWT authentication
- ✅ **Access Control**: UserCompany relationships automatically created, access validation on all operations
- ✅ **Data Isolation**: Complete separation between user data, no cross-user access
- ✅ **PayPal Integration**: Registration process with plan selection working
- ✅ **User-Specific Access**: Frontend shows only user's companies, backend enforces access control</content>
<parameter name="filePath">/Users/sthwalo/acc/COMPANY_SETUP_TASK_DOCUMENT.md