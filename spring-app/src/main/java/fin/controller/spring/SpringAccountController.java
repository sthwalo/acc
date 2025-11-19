/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 *
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fin.controller.spring;

import fin.model.Account;
import fin.service.spring.SpringAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Spring REST Controller for account management operations.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class SpringAccountController {

    private final SpringAccountService accountService;

    public SpringAccountController(SpringAccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Create a new account
     */
    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        try {
            Account createdAccount = accountService.createAccount(
                account.getAccountCode(),
                account.getAccountName(),
                account.getCompanyId(),
                account.getCategoryId(),
                account.getDescription(),
                account.getParentAccountId()
            );
            return ResponseEntity.ok(createdAccount);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get account by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        Optional<Account> account = accountService.getAccountById(id);
        return account.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all accounts for a company
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<Account>> getAccountsByCompany(@PathVariable Long companyId) {
        try {
            List<Account> accounts = accountService.getAccountsByCompany(companyId);
            return ResponseEntity.ok(accounts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get active accounts for a company
     */
    @GetMapping("/company/{companyId}/active")
    public ResponseEntity<List<Account>> getActiveAccountsByCompany(@PathVariable Long companyId) {
        try {
            List<Account> accounts = accountService.getActiveAccountsByCompany(companyId);
            return ResponseEntity.ok(accounts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get accounts by type for a company
     */
    @GetMapping("/company/{companyId}/type/{accountType}")
    public ResponseEntity<List<Account>> getAccountsByType(@PathVariable Long companyId,
                                                         @PathVariable String accountType) {
        try {
            List<Account> accounts = accountService.getAccountsByType(companyId, accountType);
            return ResponseEntity.ok(accounts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an account
     */
    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @RequestBody Account account) {
        try {
            Account updatedAccount = accountService.updateAccount(id, account);
            return ResponseEntity.ok(updatedAccount);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Deactivate an account
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateAccount(@PathVariable Long id) {
        try {
            accountService.deactivateAccount(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get asset accounts for a company
     */
    @GetMapping("/company/{companyId}/assets")
    public ResponseEntity<List<Account>> getAssetAccountsByCompany(@PathVariable Long companyId) {
        try {
            List<Account> accounts = accountService.getAssetAccountsByCompany(companyId);
            return ResponseEntity.ok(accounts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get liability accounts for a company
     */
    @GetMapping("/company/{companyId}/liabilities")
    public ResponseEntity<List<Account>> getLiabilityAccountsByCompany(@PathVariable Long companyId) {
        try {
            List<Account> accounts = accountService.getLiabilityAccountsByCompany(companyId);
            return ResponseEntity.ok(accounts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get equity accounts for a company
     */
    @GetMapping("/company/{companyId}/equity")
    public ResponseEntity<List<Account>> getEquityAccountsByCompany(@PathVariable Long companyId) {
        try {
            List<Account> accounts = accountService.getEquityAccountsByCompany(companyId);
            return ResponseEntity.ok(accounts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get income accounts for a company
     */
    @GetMapping("/company/{companyId}/income")
    public ResponseEntity<List<Account>> getIncomeAccountsByCompany(@PathVariable Long companyId) {
        try {
            List<Account> accounts = accountService.getIncomeAccountsByCompany(companyId);
            return ResponseEntity.ok(accounts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get expense accounts for a company
     */
    @GetMapping("/company/{companyId}/expenses")
    public ResponseEntity<List<Account>> getExpenseAccountsByCompany(@PathVariable Long companyId) {
        try {
            List<Account> accounts = accountService.getExpenseAccountsByCompany(companyId);
            return ResponseEntity.ok(accounts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}