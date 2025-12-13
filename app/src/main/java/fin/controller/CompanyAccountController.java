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

package fin.controller;

import fin.dto.ApiResponse;
import fin.exception.ErrorCode;
import fin.entity.Account;
import fin.service.journal.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Spring REST Controller for company-specific account operations.
 * Provides endpoints for retrieving chart of accounts for dropdown selection in UI.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}")
public class CompanyAccountController {

    private final AccountService accountService;

    public CompanyAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Get chart of accounts for a company.
     * Returns all active accounts formatted for UI dropdown selection.
     * 
     * @param companyId the company ID
     * @return list of accounts with id, code, name, category, type
     */
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<Account>>> getChartOfAccounts(@PathVariable Long companyId) {
        try {
            List<Account> accounts = accountService.getActiveAccountsByCompany(companyId);
            
            if (accounts.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.empty(
                    "No accounts found for company " + companyId + ". Please initialize chart of accounts.",
                    null
                ));
            }
            
            return ResponseEntity.ok(ApiResponse.success(
                "Chart of accounts retrieved successfully",
                accounts,
                accounts.size()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Invalid company ID: " + e.getMessage(), 
                    ErrorCode.VALIDATION_ERROR.getCode())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to retrieve chart of accounts: " + e.getMessage(),
                    ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }
}
