# TASK 8.1: Dual-API Architecture Cleanup & Consolidation
**Status:** 📋 PLANNED (Created: 2025-11-15)
**Risk Level:** HIGH - Architectural consolidation and maintenance overhead
**Estimated Effort:** 2-3 weeks (40-60 hours)
**Priority:** HIGH - Eliminates code duplication and maintenance complexity

## 🎯 Objective

Consolidate the dual-API architecture (Spark Java on port 8080 + Spring Boot on port 8081) by resolving architectural conflicts, standardizing patterns, and eliminating redundant implementations while preserving all functionality.

## 📋 Current State Analysis

### Architecture Overview
- **Spark Application (Port 8080):** Production API server with extensive service layer
- **Spring Application (Port 8081):** Development/prototype with Spring Data JPA
- **Shared Database:** Both connect to `drimacc_db` with identical credentials
- **API Contracts:** Similar REST endpoints but different implementations

### Key Conflicts Identified

#### 1. Repository Pattern Inconsistency (CRITICAL)
**Spark App Conflicts:**
- `BankTransactionRepository`: Manual JDBC with `ResultSet` mapping
- `CompanyRepository`: Spring Data JPA (conflicting patterns)
- **Impact:** Mixed access patterns, maintenance overhead, testing complexity

**Spring App Consistency:**
- `BankTransactionRepository`: Clean JPA interface extending `JpaRepository`
- **Impact:** Predictable, testable, maintainable

#### 2. Model Type Inconsistencies (HIGH)
**ID Type Disparity:**
- Spark POJOs: `Long` IDs
- Spring Entities: `Integer` IDs
- **Impact:** Type conversion issues, database compatibility problems

**Annotation Differences:**
- Spark: Plain POJOs with manual getters/setters
- Spring: `@Entity` annotated with JPA relationships
- **Impact:** Serialization inconsistencies, ORM conflicts

#### 3. Database Schema Management (MEDIUM)
**Spark Approach:**
- Script-based schema management (`scripts/docker-init.sql`)
- Manual schema evolution
- **Impact:** Version drift, deployment inconsistencies

**Spring Approach:**
- Flyway migrations (`V1__baseline_schema.sql`)
- Versioned schema changes
- **Impact:** Predictable deployments, rollback capability

#### 4. Service Layer Imbalance (MEDIUM)
**Spark Services:** Comprehensive implementation
- `TransactionService`, `BankStatementProcessingService`
- `TransactionClassificationService`, `PayrollService`
- Extensive business logic (2k+ lines in classification services)

**Spring Services:** Minimal implementation
- `SpringTransactionService` only
- **Impact:** Feature disparity, maintenance overhead

#### 5. Configuration Management (LOW)
**Spark:** `.env` file with environment variables
**Spring:** `application.properties` with Spring conventions
**Impact:** Credential management inconsistency

## 🔧 Cleanup Strategy

### Phase 1: Repository Consolidation (Priority: CRITICAL)
**Objective:** Unify repository patterns across both applications

**Option A: Standardize on JDBC (Spark Pattern)**
- Remove JPA repositories from Spark app
- Keep manual JDBC implementations
- **Pros:** Maintains current Spark functionality, no ORM overhead
- **Cons:** Loses Spring Data JPA benefits

**Option B: Migrate Spark to Spring Data JPA**
- Replace manual JDBC with JPA repositories
- Align with Spring app patterns
- **Pros:** Consistent patterns, easier testing, Spring ecosystem benefits
- **Cons:** Major refactoring of Spark services

**Option C: Hybrid Approach**
- Keep Spark as-is (JDBC repositories)
- Remove Spring app entirely
- **Pros:** Minimal disruption, preserves working code
- **Cons:** Misses Spring benefits

### Phase 2: Model Standardization (Priority: HIGH)
**Objective:** Unify model classes and ID types

**Required Changes:**
- Standardize all IDs to `Long` (PostgreSQL `BIGSERIAL` compatibility)
- Create unified model classes with consistent annotations
- Implement proper JPA relationships where needed
- Ensure serialization compatibility

### Phase 3: Database Schema Unification (Priority: MEDIUM)
**Objective:** Choose single schema management approach

**Recommendation:** Migrate to Flyway
- Move Spark schema scripts to Flyway migrations
- Implement version numbering
- Test migration rollback

### Phase 4: Service Consolidation (Priority: MEDIUM)
**Objective:** Balance service implementations

**Strategy:**
- Audit Spark services for completeness
- Migrate missing functionality to Spring services
- Eliminate redundant implementations
- Ensure feature parity

### Phase 5: Configuration Standardization (Priority: LOW)
**Objective:** Unify configuration management

**Implementation:**
- Migrate both apps to environment variables
- Create shared configuration patterns
- Implement proper credential management

## 📁 Files Requiring Changes

### Repository Files
```
Spark App:
├── app/src/main/java/fin/repository/BankTransactionRepository.java (JDBC)
├── app/src/main/java/fin/repository/CompanyRepository.java (JPA - CONFLICT)
└── app/src/main/java/fin/repository/[OtherRepositories].java

Spring App:
├── spring-app/src/main/java/fin/repository/BankTransactionRepository.java (JPA)
└── spring-app/src/main/java/fin/repository/[OtherRepositories].java
```

### Model Files
```
Spark App Models (POJOs):
├── app/src/main/java/fin/model/BankTransaction.java (Long id)
├── app/src/main/java/fin/model/Company.java (Long id)
└── app/src/main/java/fin/model/[OtherModels].java

Spring App Models (Entities):
├── spring-app/src/main/java/fin/model/BankTransaction.java (Integer id)
├── spring-app/src/main/java/fin/model/Company.java (Integer id)
└── spring-app/src/main/java/fin/model/[OtherModels].java
```

### Service Files
```
Spark App Services (Comprehensive):
├── app/src/main/java/fin/service/TransactionService.java
├── app/src/main/java/fin/service/BankStatementProcessingService.java
├── app/src/main/java/fin/service/TransactionClassificationService.java (2k+ lines)
└── app/src/main/java/fin/service/[OtherServices].java

Spring App Services (Minimal):
└── spring-app/src/main/java/fin/service/SpringTransactionService.java
```

### Configuration Files
```
Spark App:
└── .env

Spring App:
├── spring-app/src/main/resources/application.properties
└── spring-app/src/main/resources/db/migration/V1__baseline_schema.sql
```

## 🎯 Implementation Plan

### Step 1: Decision Point - Architecture Direction
**Required:** Choose consolidation strategy
- **Option A:** Keep Spark, remove Spring
- **Option B:** Migrate to Spring, remove Spark
- **Option C:** Maintain both with unified patterns

### Step 2: Repository Pattern Resolution
**For Chosen Architecture:**
- Remove conflicting JPA repositories from Spark
- Standardize on single repository pattern
- Update service dependencies

### Step 3: Model Unification
**Tasks:**
- Change all Integer IDs to Long
- Create unified model classes
- Update repository queries
- Test serialization compatibility

### Step 4: Schema Management Migration
**Tasks:**
- Convert Spark scripts to Flyway migrations
- Implement version numbering
- Test migration rollback

### Step 5: Service Consolidation
**Tasks:**
- Audit feature completeness
- Migrate missing services
- Remove duplicates
- Update dependency injection

### Step 6: Configuration Unification
**Tasks:**
- Standardize environment variables
- Update connection strings
- Test configuration loading

## ✅ Success Criteria

### Functional Requirements
- [ ] All API endpoints work identically
- [ ] Database operations function correctly
- [ ] No data loss or corruption
- [ ] All business logic preserved

### Code Quality Requirements
- [ ] Single repository pattern across application
- [ ] Consistent model ID types
- [ ] Unified configuration management
- [ ] No duplicate service implementations

### Testing Requirements
- [ ] All existing tests pass
- [ ] API contract compatibility maintained
- [ ] Database schema migrations work
- [ ] Configuration loading works in all environments

## ⚠️ Risk Assessment

### High Risk
- **Data Compatibility:** ID type changes could break existing data
- **API Contract Breaking:** Consolidation might change endpoint behavior
- **Service Dependencies:** Complex service interactions could break

### Mitigation Strategies
- **Database Backup:** Full backup before schema changes
- **Gradual Migration:** Phase-by-phase implementation with testing
- **API Versioning:** Maintain backward compatibility during transition
- **Comprehensive Testing:** Full regression test suite

## 📊 Effort Estimation

| Phase | Effort | Risk | Dependencies |
|-------|--------|------|--------------|
| Architecture Decision | 2-4 hours | Low | None |
| Repository Consolidation | 8-12 hours | High | Architecture decision |
| Model Unification | 6-10 hours | Medium | Repository consolidation |
| Schema Migration | 4-8 hours | Medium | Model unification |
| Service Consolidation | 10-16 hours | High | All previous phases |
| Configuration Unification | 2-4 hours | Low | All previous phases |

**Total Estimated Effort:** 32-54 hours (2-3 weeks)

## 🚀 Next Steps

1. **Review this task document** and provide feedback
2. **Choose consolidation strategy** (Spark vs Spring primary)
3. **Begin with repository consolidation** (highest impact)
4. **Test each phase** before proceeding to next
5. **Document all changes** for future reference

## 📚 References

- **Spark Application:** `app/` directory structure
- **Spring Application:** `spring-app/` directory structure
- **Database Schema:** `scripts/docker-init.sql` and `spring-app/src/main/resources/db/migration/`
- **API Documentation:** Test endpoints on ports 8080 and 8081

---
Read [](file:///Users/sthwalonyoni/FIN/scripts/migrations)

Read [](file:///Users/sthwalonyoni/FIN/spring-app/src/main/resources/application.properties)

Read [](file:///Users/sthwalonyoni/FIN/.env)

Based on my comprehensive examination of both the Spark and Spring applications, I've identified several key architectural differences and cleanup opportunities. Here's what I found:

## **Database Access Patterns**

**Spark Application (Port 8080):**
- Uses manual JDBC with custom repositories (`BankTransactionRepository` with `ResultSet` mapping)
- Has conflicting repository patterns (both JDBC and JPA implementations exist)
- Database schema managed via scripts in scripts directory
- Uses .env file for credentials

**Spring Application (Port 8081):**
- Uses Spring Data JPA with `@Repository` interfaces
- Clean JPA repository pattern (`BankTransactionRepository` extends `JpaRepository`)
- Database migrations managed via Flyway (V1__baseline_schema.sql)
- Uses application.properties for configuration

## **Model Differences**

**Spark Models:**
- Plain POJOs with `Long` IDs
- Manual getters/setters
- No JPA annotations

**Spring Models:**
- `@Entity` annotated classes
- `Integer` IDs (inconsistent with Spark's `Long`)
- JPA relationships and validations

## **Controller/API Patterns**

Both implement similar REST endpoints but with different frameworks:
- Spark: Uses `TransactionRoutes` with Spark Java routing
- Spring: Uses `@RestController` with Spring MVC

## **Service Layer Disparity**

- **Spark**: Extensive service layer (`TransactionService`, `BankStatementProcessingService`, etc.)
- **Spring**: Minimal services (`SpringTransactionService` only)

## **Cleanup Recommendations**

### **1. Repository Consolidation (HIGH PRIORITY)**
The Spark app has conflicting repository patterns. The `CompanyRepository` uses JPA while `BankTransactionRepository` uses JDBC. **Recommendation**: Standardize on JDBC for Spark app (remove JPA dependencies) or migrate entirely to Spring.

### **2. Model Standardization (HIGH PRIORITY)**
ID types are inconsistent: Spark uses `Long`, Spring uses `Integer`. **Recommendation**: Standardize on `Long` for all IDs to match PostgreSQL `SERIAL`/`BIGSERIAL` types.

### **3. Database Schema Management**
- Spark: Uses scripts and manual schema management
- Spring: Uses Flyway migrations
**Recommendation**: Choose one approach. Flyway is more maintainable for versioned schema changes.

### **4. Service Layer Consolidation**
Spark has comprehensive services while Spring has minimal implementation. **Recommendation**: Either enhance Spring services or remove redundant Spark services if migrating to Spring.

### **5. Configuration Cleanup**
Both apps have separate credential management. **Recommendation**: Use environment variables consistently across both apps.

## **Questions for Next Steps**

1. **Which application should be the primary?** Spark (more feature-complete) or Spring (cleaner architecture)?

2. **Should we consolidate to a single API framework?** This would eliminate duplication and maintenance overhead.

3. **Database schema approach?** Migrate Spark to Flyway or keep script-based approach?

4. **Service consolidation strategy?** Move Spark services to Spring or enhance Spring services?

Would you like me to proceed with implementing any of these cleanup recommendations, or do you need more details on specific areas?

**Note:** This task addresses the architectural debt created by maintaining two parallel API implementations. The consolidation will significantly reduce maintenance overhead and eliminate the risk of feature drift between the two applications.