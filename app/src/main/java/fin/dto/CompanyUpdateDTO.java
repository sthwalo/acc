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
package fin.dto;

import java.util.Optional;

/**
 * Data Transfer Object for updating company information.
 * Contains only the fields that can be modified via API.
 * Uses Optional to distinguish between "not provided" and "provided as null".
 */
public class CompanyUpdateDTO {
    private Optional<String> name = Optional.empty();
    private Optional<String> registrationNumber = Optional.empty();
    private Optional<String> taxNumber = Optional.empty();
    private Optional<String> address = Optional.empty();
    private Optional<String> contactEmail = Optional.empty();
    private Optional<String> contactPhone = Optional.empty();
    private Optional<String> logoPath = Optional.empty();
    private Optional<String> bankName = Optional.empty();
    private Optional<String> accountNumber = Optional.empty();
    private Optional<String> accountType = Optional.empty();
    private Optional<String> branchCode = Optional.empty();
    private Optional<Boolean> vatRegistered = Optional.empty();
    private Optional<Long> industryId = Optional.empty();

    // Constructors
    public CompanyUpdateDTO() {}

    // Getters and setters
    public Optional<String> getName() { return name; }
    public void setName(String name) { this.name = Optional.ofNullable(name); }

    public Optional<String> getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = Optional.ofNullable(registrationNumber); }

    public Optional<String> getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = Optional.ofNullable(taxNumber); }

    public Optional<String> getAddress() { return address; }
    public void setAddress(String address) { this.address = Optional.ofNullable(address); }

    public Optional<String> getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = Optional.ofNullable(contactEmail); }

    public Optional<String> getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = Optional.ofNullable(contactPhone); }

    public Optional<String> getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = Optional.ofNullable(logoPath); }

    public Optional<String> getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = Optional.ofNullable(bankName); }

    public Optional<String> getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = Optional.ofNullable(accountNumber); }

    public Optional<String> getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = Optional.ofNullable(accountType); }

    public Optional<String> getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = Optional.ofNullable(branchCode); }

    public Optional<Boolean> getVatRegistered() { return vatRegistered; }
    public void setVatRegistered(boolean vatRegistered) { this.vatRegistered = Optional.of(vatRegistered); }

    public Optional<Long> getIndustryId() { return industryId; }
    public void setIndustryId(Long industryId) { this.industryId = Optional.ofNullable(industryId); }
}