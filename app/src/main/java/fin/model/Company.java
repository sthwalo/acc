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
package fin.model;

import java.time.LocalDateTime;

public class Company {
    private Long id;
    private String name;
    private String registrationNumber;
    private String taxNumber;
    private String address;
    private String contactEmail;
    private String contactPhone;
    private String logoPath;
    private LocalDateTime createdAt;
    
    // Constructors, getters, and setters
    public Company() {}
    
    public Company(String initialName) {
        this.name = initialName;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Copy constructor for defensive copying.
     * Creates a deep copy of all Company fields to prevent external modification.
     */
    public Company(Company other) {
        if (other == null) {
            return;
        }
        
        this.id = other.id;
        this.name = other.name;
        this.registrationNumber = other.registrationNumber;
        this.taxNumber = other.taxNumber;
        this.address = other.address;
        this.contactEmail = other.contactEmail;
        this.contactPhone = other.contactPhone;
        this.logoPath = other.logoPath;
        this.createdAt = other.createdAt;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long newId) { this.id = newId; }
    
    public String getName() { return name; }
    public void setName(String newName) { this.name = newName; }
    
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String newRegistrationNumber) { this.registrationNumber = newRegistrationNumber; }
    
    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String newTaxNumber) { this.taxNumber = newTaxNumber; }
    
    public String getAddress() { return address; }
    public void setAddress(String newAddress) { this.address = newAddress; }
    
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String newContactEmail) { this.contactEmail = newContactEmail; }
    
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String newContactPhone) { this.contactPhone = newContactPhone; }
    
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String newLogoPath) { this.logoPath = newLogoPath; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime newCreatedAt) { this.createdAt = newCreatedAt; }
    
    @Override
    public String toString() {
        return "Company{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", registrationNumber='" + registrationNumber + '\'' +
                ", taxNumber='" + taxNumber + '\'' +
                '}';
    }
}
