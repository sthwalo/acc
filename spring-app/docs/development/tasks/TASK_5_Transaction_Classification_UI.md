# TASK 5: Transaction Classification UI with Double-Entry Account Selection
**Status:** 🚧 IN PROGRESS - Backend & Frontend Integration
**Created:** 2025-12-05
**Priority:** HIGH - Core Accounting Feature
**Risk Level:** MEDIUM - Data Integrity & User Experience
**Estimated Effort:** 5-7 days (40-56 hours)

## 🎯 Task Overview

Implement comprehensive double-entry accounting classification UI in the Data Management view, allowing users to manually select debit and credit accounts from the Chart of Accounts when editing transactions. This replaces the generic "Category" and "Reference" fields with proper accounting classification, integrating `AccountClassificationService` with the frontend for drill-down account selection.

## 📋 Current State

### Completed (Frontend Foundation)
✅ **UI Structure Updated**: Table headers changed from "Category/Reference" to "Debit Account/Credit Account"
✅ **Type Definitions**: Transaction interface extended with classification fields (debit_account_id, credit_account_id, account_code, account_name)
✅ **Display Logic**: Account cells show code + name with proper styling
✅ **CSS Styling**: Added `.account-cell` styles for account code/name display
✅ **Build Verification**: Frontend builds successfully with new structure

### Current Limitations
❌ **No Backend Integration**: Classification fields currently mapped to `null` - no data from API
❌ **Text Input Only**: Edit mode shows plain text inputs instead of account selectors
❌ **No Chart of Accounts**: No API endpoint to fetch available accounts for dropdown
❌ **No Mapping Rules**: `AccountClassificationService.getStandardMappingRules()` not exposed via REST API
❌ **Manual Update Missing**: No backend endpoint to update transaction classification and create journal entries

## 🎯 Requirements

### Backend API Requirements

#### 1. Extend Transaction DTO
- [ ] Add `debitAccountId` (Long) - Account ID for debit side of journal entry
- [ ] Add `creditAccountId` (Long) - Account ID for credit side of journal entry
- [ ] Add `debitAccountCode` (String) - Account code for display (e.g., "1000")
- [ ] Add `creditAccountCode` (String) - Account code for display (e.g., "4100")
- [ ] Add `debitAccountName` (String) - Account name for display (e.g., "Cash at Bank")
- [ ] Add `creditAccountName` (String) - Account name for display (e.g., "Sales Revenue")
- [ ] Query journal_entry_lines to populate these fields when fetching transactions

#### 2. Chart of Accounts Endpoint
```
GET /api/v1/companies/{companyId}/accounts
Response: {
  "success": true,
  "data": [
    {
      "id": 1,
      "code": "1000",
      "name": "Cash at Bank",
      "category": "CURRENT_ASSETS",
      "type": "ASSET",
      "isActive": true
    },
    ...
  ]
}
```

#### 3. Mapping Rules Endpoint
```
GET /api/v1/companies/{companyId}/classification/mapping-rules
Response: {
  "success": true,
  "data": [
    {
      "id": 1,
      "pattern": "ATM.*DEPOSIT",
      "debitAccountCode": "1000",
      "creditAccountCode": "2100",
      "priority": 10,
      "description": "ATM Cash Deposits"
    },
    ...
  ]
}
```

#### 4. Update Transaction Classification
```
PUT /api/v1/transactions/{transactionId}/classification
Request: {
  "debitAccountId": 1,
  "creditAccountId": 5
}
Response: {
  "success": true,
  "message": "Transaction classification updated and journal entry created"
}
```

### Frontend UI Requirements

#### 1. Account Selector Dropdown
- [ ] **Searchable Dropdown**: Filter accounts by code or name
- [ ] **Grouped by Category**: Show accounts organized by asset/liability/equity/revenue/expense
- [ ] **Display Format**: "1000 - Cash at Bank" (code + name)
- [ ] **Current Selection Highlight**: Show currently selected account
- [ ] **Validation**: Ensure debit and credit accounts are different

#### 2. Drill-Down UI
- [ ] **Show Mapping Rules**: Display suggested accounts based on transaction description patterns
- [ ] **Rule Priority Indicator**: Show why a particular account was suggested (CRITICAL/HIGH/STANDARD)
- [ ] **Manual Override**: Allow user to override suggested classification
- [ ] **Save Confirmation**: Show what journal entry will be created before saving

#### 3. Data Management View Updates
- [ ] **Fetch Chart of Accounts**: Load accounts on component mount
- [ ] **Account State Management**: Store accounts in component state
- [ ] **Edit Mode Account Selectors**: Replace text inputs with dropdowns
- [ ] **Save Handler**: Call classification update endpoint
- [ ] **Success Feedback**: Show confirmation when classification saved

## 🏗️ Implementation Plan

### Phase 1: Backend API Extensions (Days 1-3)

#### Step 1.1: Extend BankTransactionDTO
**File**: `spring-app/src/main/java/fin/dto/BankTransactionDTO.java`

```java
// Add fields to BankTransactionDTO
private Long debitAccountId;
private Long creditAccountId;
private String debitAccountCode;
private String creditAccountCode;
private String debitAccountName;
private String creditAccountName;

// Add getters/setters
```

#### Step 1.2: Update Transaction Mapper
**File**: `spring-app/src/main/java/fin/service/spring/SpringTransactionService.java`

```java
// In getTransactionsByFiscalPeriod() method
private BankTransactionDTO enrichWithJournalEntryAccounts(BankTransaction transaction) {
    // Query journal_entry_lines WHERE source_transaction_id = transaction.id
    List<JournalEntryLine> lines = journalEntryLineRepository
        .findBySourceTransactionId(transaction.getId());
    
    // Find debit line (debitAmount > 0)
    JournalEntryLine debitLine = lines.stream()
        .filter(line -> line.getDebitAmount().compareTo(BigDecimal.ZERO) > 0)
        .findFirst().orElse(null);
    
    // Find credit line (creditAmount > 0)
    JournalEntryLine creditLine = lines.stream()
        .filter(line -> line.getCreditAmount().compareTo(BigDecimal.ZERO) > 0)
        .findFirst().orElse(null);
    
    // Set DTO fields from accounts
    if (debitLine != null) {
        Account debitAccount = accountRepository.findById(debitLine.getAccountId()).orElse(null);
        if (debitAccount != null) {
            dto.setDebitAccountId(debitAccount.getId());
            dto.setDebitAccountCode(debitAccount.getCode());
            dto.setDebitAccountName(debitAccount.getName());
        }
    }
    
    if (creditLine != null) {
        Account creditAccount = accountRepository.findById(creditLine.getAccountId()).orElse(null);
        if (creditAccount != null) {
            dto.setCreditAccountId(creditAccount.getId());
            dto.setCreditAccountCode(creditAccount.getCode());
            dto.setCreditAccountName(creditAccount.getName());
        }
    }
    
    return dto;
}
```

#### Step 1.3: Create AccountController
**File**: `spring-app/src/main/java/fin/api/AccountController.java`

```java
@RestController
@RequestMapping("/api/v1/companies/{companyId}/accounts")
public class AccountController {
    
    private final SpringAccountService accountService;
    
    @GetMapping
    public ApiResponse<List<AccountDTO>> getChartOfAccounts(
            @PathVariable Long companyId) {
        // Fetch all active accounts for company
        List<Account> accounts = accountService.getAccountsByCompany(companyId);
        
        // Map to DTOs
        List<AccountDTO> dtos = accounts.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        
        return ApiResponse.success(dtos);
    }
    
    private AccountDTO toDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setCode(account.getCode());
        dto.setName(account.getName());
        dto.setCategory(account.getCategory().name());
        dto.setType(account.getType().name());
        dto.setIsActive(account.getIsActive());
        return dto;
    }
}
```

#### Step 1.4: Create Classification Endpoints
**File**: `spring-app/src/main/java/fin/api/ClassificationController.java`

```java
@RestController
@RequestMapping("/api/v1/companies/{companyId}/classification")
public class ClassificationController {
    
    private final AccountClassificationService classificationService;
    
    @GetMapping("/mapping-rules")
    public ApiResponse<List<TransactionMappingRuleDTO>> getMappingRules(
            @PathVariable Long companyId) {
        // Get standard mapping rules
        List<TransactionMappingRule> rules = 
            classificationService.getStandardMappingRules();
        
        // Map to DTOs
        List<TransactionMappingRuleDTO> dtos = rules.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        
        return ApiResponse.success(dtos);
    }
    
    @PutMapping("/transactions/{transactionId}")
    public ApiResponse<String> updateTransactionClassification(
            @PathVariable Long companyId,
            @PathVariable Long transactionId,
            @RequestBody ClassificationUpdateRequest request) {
        
        // Validate accounts exist
        Account debitAccount = accountRepository
            .findById(request.getDebitAccountId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Debit account not found: " + request.getDebitAccountId()));
        
        Account creditAccount = accountRepository
            .findById(request.getCreditAccountId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Credit account not found: " + request.getCreditAccountId()));
        
        // Update or create journal entry
        classificationService.updateTransactionClassification(
            transactionId, 
            debitAccount.getId(), 
            creditAccount.getId()
        );
        
        return ApiResponse.success(
            "Transaction classification updated and journal entry created");
    }
}
```

#### Step 1.5: Create AccountClassificationService Method
**File**: `spring-app/src/main/java/fin/service/spring/AccountClassificationService.java`

```java
@Transactional
public void updateTransactionClassification(
        Long transactionId, 
        Long debitAccountId, 
        Long creditAccountId) {
    
    // Get transaction
    BankTransaction transaction = bankTransactionRepository
        .findById(transactionId)
        .orElseThrow(() -> new IllegalArgumentException(
            "Transaction not found: " + transactionId));
    
    // Find existing journal entry for this transaction
    List<JournalEntryLine> existingLines = journalEntryLineRepository
        .findBySourceTransactionId(transactionId);
    
    if (!existingLines.isEmpty()) {
        // Update existing journal entry lines
        JournalEntryLine debitLine = existingLines.stream()
            .filter(line -> line.getDebitAmount().compareTo(BigDecimal.ZERO) > 0)
            .findFirst().orElse(null);
        
        JournalEntryLine creditLine = existingLines.stream()
            .filter(line -> line.getCreditAmount().compareTo(BigDecimal.ZERO) > 0)
            .findFirst().orElse(null);
        
        if (debitLine != null) {
            debitLine.setAccountId(debitAccountId);
            journalEntryLineRepository.save(debitLine);
        }
        
        if (creditLine != null) {
            creditLine.setAccountId(creditAccountId);
            journalEntryLineRepository.save(creditLine);
        }
    } else {
        // Create new journal entry
        JournalEntry entry = new JournalEntry();
        entry.setCompanyId(transaction.getCompanyId());
        entry.setFiscalPeriodId(transaction.getFiscalPeriodId());
        entry.setEntryDate(transaction.getTransactionDate());
        entry.setDescription(transaction.getDescription());
        entry.setReference("MANUAL-CLASS-" + transactionId);
        entry.setCreatedAt(LocalDateTime.now());
        
        JournalEntry savedEntry = journalEntryRepository.save(entry);
        
        // Create debit line
        JournalEntryLine debitLine = new JournalEntryLine();
        debitLine.setJournalEntryId(savedEntry.getId());
        debitLine.setSourceTransactionId(transactionId);
        debitLine.setAccountId(debitAccountId);
        debitLine.setDescription(transaction.getDescription());
        debitLine.setDebitAmount(
            transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0 
                ? transaction.getDebitAmount() 
                : transaction.getCreditAmount()
        );
        debitLine.setCreditAmount(BigDecimal.ZERO);
        journalEntryLineRepository.save(debitLine);
        
        // Create credit line
        JournalEntryLine creditLine = new JournalEntryLine();
        creditLine.setJournalEntryId(savedEntry.getId());
        creditLine.setSourceTransactionId(transactionId);
        creditLine.setAccountId(creditAccountId);
        creditLine.setDescription(transaction.getDescription());
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(
            transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0 
                ? transaction.getDebitAmount() 
                : transaction.getCreditAmount()
        );
        journalEntryLineRepository.save(creditLine);
    }
}
```

### Phase 2: Frontend Integration (Days 4-5)

#### Step 2.1: Update API Types
**File**: `frontend/src/types/api.ts`

```typescript
export interface ApiTransaction {
  // ... existing fields ...
  debitAccountId: number | null;
  creditAccountId: number | null;
  debitAccountCode: string | null;
  creditAccountCode: string | null;
  debitAccountName: string | null;
  creditAccountName: string | null;
}

export interface Account {
  id: number;
  code: string;
  name: string;
  category: string;
  type: string;
  isActive: boolean;
}

export interface TransactionMappingRule {
  id: number;
  pattern: string;
  debitAccountCode: string;
  creditAccountCode: string;
  priority: number;
  description: string;
}
```

#### Step 2.2: Create API Service Methods
**File**: `frontend/src/hooks/useApi.ts`

```typescript
// Add to accounts section
accounts: {
  getChartOfAccounts: async (companyId: number) => {
    const response = await axiosInstance.get<ApiResponse<Account[]>>(
      `/companies/${companyId}/accounts`
    );
    return response.data;
  }
},

// Add to classification section
classification: {
  // ... existing methods ...
  getMappingRules: async (companyId: number) => {
    const response = await axiosInstance.get<ApiResponse<TransactionMappingRule[]>>(
      `/companies/${companyId}/classification/mapping-rules`
    );
    return response.data;
  },
  
  updateTransactionClassification: async (
    companyId: number, 
    transactionId: number, 
    debitAccountId: number, 
    creditAccountId: number
  ) => {
    const response = await axiosInstance.put<ApiResponse<string>>(
      `/companies/${companyId}/classification/transactions/${transactionId}`,
      { debitAccountId, creditAccountId }
    );
    return response.data;
  }
}
```

#### Step 2.3: Create Account Selector Component
**File**: `frontend/src/components/AccountSelector.tsx`

```typescript
import { useState, useMemo } from 'react';
import { Search } from 'lucide-react';
import type { Account } from '../types/api';

interface AccountSelectorProps {
  accounts: Account[];
  selectedAccountId: number | null;
  onSelect: (accountId: number) => void;
  label: string;
  placeholder?: string;
}

export default function AccountSelector({
  accounts,
  selectedAccountId,
  onSelect,
  label,
  placeholder = 'Select account...'
}: AccountSelectorProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [isOpen, setIsOpen] = useState(false);

  const filteredAccounts = useMemo(() => {
    if (!searchTerm) return accounts;
    const term = searchTerm.toLowerCase();
    return accounts.filter(
      acc => 
        acc.code.toLowerCase().includes(term) ||
        acc.name.toLowerCase().includes(term)
    );
  }, [accounts, searchTerm]);

  const groupedAccounts = useMemo(() => {
    const groups: Record<string, Account[]> = {};
    filteredAccounts.forEach(acc => {
      if (!groups[acc.category]) {
        groups[acc.category] = [];
      }
      groups[acc.category].push(acc);
    });
    return groups;
  }, [filteredAccounts]);

  const selectedAccount = accounts.find(acc => acc.id === selectedAccountId);

  return (
    <div className="account-selector">
      <label>{label}</label>
      <div className="selector-dropdown">
        <button
          type="button"
          onClick={() => setIsOpen(!isOpen)}
          className="selector-trigger"
        >
          {selectedAccount ? (
            <span>
              <span className="account-code">{selectedAccount.code}</span>
              {' - '}
              <span className="account-name">{selectedAccount.name}</span>
            </span>
          ) : (
            <span className="placeholder">{placeholder}</span>
          )}
        </button>

        {isOpen && (
          <div className="selector-menu">
            <div className="search-box">
              <Search size={16} />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search accounts..."
                autoFocus
              />
            </div>

            <div className="accounts-list">
              {Object.entries(groupedAccounts).map(([category, accts]) => (
                <div key={category} className="account-group">
                  <div className="group-header">{category}</div>
                  {accts.map(account => (
                    <button
                      key={account.id}
                      type="button"
                      onClick={() => {
                        onSelect(account.id);
                        setIsOpen(false);
                        setSearchTerm('');
                      }}
                      className={`account-option ${
                        account.id === selectedAccountId ? 'selected' : ''
                      }`}
                    >
                      <span className="account-code">{account.code}</span>
                      {' - '}
                      <span className="account-name">{account.name}</span>
                    </button>
                  ))}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
```

#### Step 2.4: Update DataManagementView
**File**: `frontend/src/components/DataManagementView.tsx`

```typescript
// Add state for chart of accounts
const [accounts, setAccounts] = useState<Account[]>([]);

// Load chart of accounts on mount
useEffect(() => {
  const loadAccounts = async () => {
    try {
      const result = await api.accounts.getChartOfAccounts(selectedCompany.id);
      setAccounts(result.data || []);
    } catch (err) {
      console.error('Failed to load chart of accounts:', err);
    }
  };
  loadAccounts();
}, [api, selectedCompany.id]);

// Update saveTransaction to include classification
const saveTransaction = async () => {
  if (!editingTransaction) return;

  try {
    setError(null);
    
    // Update classification if changed
    if (editingTransaction.debit_account_id && editingTransaction.credit_account_id) {
      await api.classification.updateTransactionClassification(
        selectedCompany.id,
        editingTransaction.id,
        editingTransaction.debit_account_id,
        editingTransaction.credit_account_id
      );
    }

    // Reload transactions to get updated data
    await loadTransactions();
    
    setEditingTransaction(null);
    setSuccess('Transaction classification updated successfully');
    setTimeout(() => setSuccess(null), 3000);
  } catch (err) {
    setError(err instanceof Error ? err.message : 'Failed to update transaction');
  }
};

// Replace text inputs with AccountSelector in edit mode
// In the table cells for Debit Account and Credit Account:
{transaction.isEditing ? (
  <AccountSelector
    accounts={accounts}
    selectedAccountId={editingTransaction?.debit_account_id || null}
    onSelect={(accountId) => updateEditingTransaction('debit_account_id', accountId)}
    label=""
    placeholder="Select debit account..."
  />
) : (
  // ... existing display logic ...
)}
```

### Phase 3: Testing & Validation (Days 6-7)

#### Test Cases

**Backend API Tests**
- [ ] GET /api/v1/companies/{id}/accounts returns all accounts with correct structure
- [ ] GET /api/v1/companies/{id}/classification/mapping-rules returns all rules with priorities
- [ ] PUT /api/v1/companies/{id}/classification/transactions/{id} creates journal entry correctly
- [ ] PUT /api/v1/companies/{id}/classification/transactions/{id} updates existing journal entry
- [ ] Transaction DTO includes debit/credit account fields from journal_entry_lines
- [ ] Invalid account IDs return proper error messages

**Frontend UI Tests**
- [ ] Chart of accounts loads on component mount
- [ ] Account selector dropdown displays all accounts grouped by category
- [ ] Search filter works for both account code and name
- [ ] Selected account displays correctly in selector
- [ ] Save button calls classification update API with correct account IDs
- [ ] Success message displays after successful update
- [ ] Transaction list refreshes with new classification after save
- [ ] "Not classified" placeholder shows for transactions without classification

**Integration Tests**
- [ ] Edit transaction → select accounts → save → verify journal entry created
- [ ] Edit transaction with existing classification → change accounts → save → verify journal entry updated
- [ ] Verify double-entry balance (debit amount = credit amount)
- [ ] Verify account codes and names display correctly after save

## 📁 Files to Modify/Create

### Backend Files
```
spring-app/src/main/java/fin/
├── dto/
│   ├── BankTransactionDTO.java (MODIFY - add account fields)
│   ├── AccountDTO.java (CREATE)
│   ├── TransactionMappingRuleDTO.java (CREATE)
│   └── ClassificationUpdateRequest.java (CREATE)
├── api/
│   ├── AccountController.java (CREATE)
│   └── ClassificationController.java (CREATE)
├── service/spring/
│   ├── SpringTransactionService.java (MODIFY - enrich DTOs with journal entry accounts)
│   └── AccountClassificationService.java (MODIFY - add updateTransactionClassification method)
└── repository/
    └── JournalEntryLineRepository.java (MODIFY - add findBySourceTransactionId method)
```

### Frontend Files
```
frontend/src/
├── types/
│   └── api.ts (MODIFY - add Account, TransactionMappingRule types)
├── hooks/
│   └── useApi.ts (MODIFY - add accounts and classification methods)
├── components/
│   ├── AccountSelector.tsx (CREATE)
│   └── DataManagementView.tsx (MODIFY - integrate account selectors)
└── App.css (MODIFY - add account selector styles)
```

## ⚠️ Critical Implementation Notes

### Backend Considerations
1. **Query Performance**: When enriching transaction DTOs with journal entry accounts, use JOIN queries instead of N+1 queries
2. **Transaction Integrity**: Wrap classification updates in @Transactional to ensure atomicity
3. **Validation**: Ensure debit and credit accounts belong to the same company as the transaction
4. **Audit Trail**: Log all classification changes with timestamp and user ID

### Frontend Considerations
1. **Account Caching**: Load chart of accounts once and cache in component state
2. **Validation**: Prevent saving if debit_account_id === credit_account_id
3. **Loading States**: Show spinner while fetching accounts or saving classification
4. **Error Handling**: Display clear error messages if account selection fails

### Security Considerations
1. **Authorization**: Verify user has permission to modify transactions for the company
2. **Input Validation**: Validate account IDs exist and are active before creating journal entries
3. **SQL Injection**: Use parameterized queries in all repository methods

## 📊 Success Metrics

### Functional Metrics
- [ ] All transactions display current classification (if journal entry exists)
- [ ] User can select debit/credit accounts from dropdown in edit mode
- [ ] Classification updates create or update journal entries correctly
- [ ] Double-entry balance validation passes (debit = credit)
- [ ] Chart of accounts displays all active accounts grouped by category

### Performance Metrics
- [ ] Chart of accounts loads in < 500ms
- [ ] Transaction DTO enrichment adds < 100ms per transaction
- [ ] Classification update completes in < 1 second
- [ ] Account selector search filters in < 50ms

### User Experience Metrics
- [ ] Account selector is intuitive and easy to use
- [ ] Search functionality finds accounts quickly
- [ ] Success/error messages are clear and actionable
- [ ] Edit mode preserves existing classification for editing

## 🔄 Rollback Plan

If critical issues arise:
1. **Frontend Rollback**: Revert to text inputs for debit/credit accounts
2. **Backend Rollback**: Keep new endpoints but make them optional (return null if no journal entry)
3. **Database Rollback**: No schema changes required, journal entries remain intact

## 📝 Documentation Updates Needed

- [ ] Update API documentation with new endpoints (AccountController, ClassificationController)
- [ ] Add AccountSelector component to frontend component library documentation
- [ ] Document double-entry classification workflow in user guide
- [ ] Add troubleshooting section for common classification issues

## 🎓 Learning Outcomes

This task demonstrates:
- **Full-Stack Integration**: Backend API design → Frontend UI implementation
- **Double-Entry Accounting**: Proper debit/credit classification with journal entries
- **Component Reusability**: Searchable dropdown component for future use
- **Data Enrichment**: Joining multiple tables to populate DTOs efficiently
- **User Experience**: Intuitive account selection with search and grouping

---

**Next Steps After Completion:**
1. Implement auto-classification suggestions based on transaction patterns
2. Add bulk classification update for multiple transactions
3. Create classification report showing unclassified vs classified transactions
4. Add keyboard shortcuts for faster account selection (e.g., arrow keys, Enter to select)
