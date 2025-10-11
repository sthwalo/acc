# Quick Reference Guide - VAT Accounting Reform Tasks

**Epic:** VAT Accounting System Reform  
**Total Effort:** 21-34 Story Points | 3 Sprints | 6-7 weeks

## 📋 Task Overview

| Phase | Story | Points | Priority | Duration | Status |
|-------|--------|--------|----------|----------|--------|
| **Foundation** | | | | | |
| 1 | Company Tax Registration Framework | 8 | Critical | 3-5 days | 📝 Ready |
| 1 | Enhanced SARSTaxCalculator | 5 | High | 2-3 days | 📝 Ready |  
| 1 | Chart of Accounts Reform | 3 | Medium | 1-2 days | 📝 Ready |
| **Core Engine** | | | | | |
| 2 | Transaction Classification Overhaul | 8 | Critical | 3-4 days | ⏳ Waiting |
| 2 | Journal Entry Generation Reform | 5 | Critical | 2-3 days | ⏳ Waiting |
| **Reporting** | | | | | |
| 3 | Financial Statement Classification | 3 | Medium | 1-2 days | ⏳ Waiting |
| 3 | Historical Data Migration | 5 | Medium | 2-3 days | ⏳ Waiting |

## 🎯 Current Priority: Story 1

**Next Task:** Company Tax Registration Framework (8 points)

### Immediate Actions Required:
1. **Database Migration** - Add tax registration fields to companies table
2. **Model Update** - Extend Company.java with VAT/Income Tax flags  
3. **Service Extension** - Add tax registration logic to CompanyService
4. **UI Integration** - Update console application for tax registration

### Quick Start Commands:
```bash
# 1. Create migration script
touch scripts/migration_001_add_tax_registration.sql

# 2. Update Company model  
# Edit: app/src/main/java/fin/model/Company.java

# 3. Extend CompanyService
# Edit: app/src/main/java/fin/service/CompanyService.java

# 4. Test changes
./gradlew clean build -x test
```

## 🔄 Workflow Checklist

### Before Starting Any Story:
- [ ] Read complete story documentation
- [ ] Understand dependencies and prerequisites  
- [ ] Create feature branch: `git checkout -b vat-reform/story-X`
- [ ] Set up test data and backup current database
- [ ] Review acceptance criteria and definition of done

### During Development:
- [ ] Write tests first (TDD approach recommended)
- [ ] Make incremental commits with clear messages
- [ ] Run tests frequently: `./gradlew test`
- [ ] Validate Trial Balance remains at R650,995.54
- [ ] Document any deviations or issues discovered

### After Completing Story:
- [ ] Run full test suite: `./gradlew clean build`
- [ ] Manual testing of complete user workflow
- [ ] Code review with team member
- [ ] Update documentation if needed
- [ ] Merge to main branch after approval

## 📁 Key Files Reference

### Core Business Logic:
- `Company.java` - Business entity with tax registration
- `CompanyService.java` - Tax registration business logic
- `SARSTaxCalculator.java` - VAT calculations and validation
- `TransactionMappingService.java` - VAT transaction classification
- `JournalEntryGenerator.java` - VAT journal entry creation

### Financial Reporting:
- `IncomeStatementService.java` - VAT expenses presentation
- `BalanceSheetService.java` - VAT liabilities presentation  
- `TrialBalanceService.java` - Account categorization
- `GeneralLedgerService.java` - Account balance calculations

### Data Access:
- `CompanyRepository.java` - Company persistence with tax fields
- `ChartOfAccountsService.java` - VAT account definitions
- Database migration scripts in `/scripts/`

### Application Context:
- `ApplicationContext.java` - Service dependency injection
- `ConsoleApplication.java` - UI for tax registration

## 💡 Problem Being Solved

### Current Issue:
```
ACCOUNT: 3100 - VAT Output  
2024-03-02: SARS VAT Payment → R 46,095.65 DR (WRONG - reducing liability)
2024-03-13: SARS VAT Payment → R 41,719.43 DR (WRONG - reducing liability)  
Closing Balance: R 87,815.08 DR (WRONG - liability should be CR)
```

### Target Solution:
```
ACCOUNT: 9700 - VAT Input (Expense)
2024-03-02: SARS VAT Payment → R 46,095.65 DR (CORRECT - expense)
2024-03-13: SARS VAT Payment → R 41,719.43 DR (CORRECT - expense)

ACCOUNT: 3100 - VAT Output (Liability)  
Shows actual VAT liability to SARS (CR balance)

ACCOUNT: 3150 - VAT Control Account (Liability)
Reconciles VAT Input vs Output for accurate liability position
```

## 🚨 Critical Success Factors

1. **Mathematical Integrity** - Trial Balance must equal R650,995.54 after all changes
2. **Backward Compatibility** - Existing non-VAT functionality must remain unchanged
3. **SARS Compliance** - VAT calculations must follow South African tax law
4. **Data Safety** - No loss of historical transaction data during migration
5. **Performance** - No significant degradation in application performance

## 📞 Support & Escalation

### When to Ask for Help:
- Trial Balance doesn't balance after changes
- VAT calculations don't match SARS requirements  
- Database migration fails or causes data loss
- Breaking changes affect existing functionality
- Performance degrades significantly

### Documentation References:
- `VAT_ACCOUNTING_REFORM_ROADMAP.md` - Complete implementation plan
- `STORY_1_COMPANY_TAX_REGISTRATION.md` - Detailed first story guide
- `SPRINT_PLANNING_VAT_REFORM.md` - Sprint-by-sprint breakdown
- `.github/copilot-instructions.md` - System architecture reference

## 🎯 Definition of Complete Epic Success

**The VAT Accounting Reform is complete when:**

✅ **Financial Accuracy**
- Trial Balance = R650,995.54 (unchanged total)
- VAT payments show as expenses in Income Statement  
- VAT liabilities show correctly in Balance Sheet
- All account balances mathematically correct

✅ **System Functionality**  
- Companies can be marked as VAT registered
- VAT calculations only apply to registered companies
- Transaction classification respects company registration
- Journal entries properly post VAT transactions

✅ **Data Integrity**
- All historical data migrated correctly
- No loss of existing transaction information
- Audit trail maintained for all changes
- Database relationships intact

✅ **Compliance & Standards**
- VAT calculations comply with SARS requirements
- Accounting entries follow GAAP principles
- Financial statements present VAT correctly
- Tax registration validation works properly

**Ready to start with Story 1: Company Tax Registration Framework?** 🚀