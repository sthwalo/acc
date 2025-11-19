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

package fin.state;

import fin.model.Company;
import fin.model.FiscalPeriod;
import fin.model.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Application state management component
 * Extracted from monolithic App.java to centralize state handling
 */
public class ApplicationState {
    private Company currentCompany;
    private FiscalPeriod currentFiscalPeriod;
    private User currentUser;
    private Map<String, Object> sessionData;
    
    public ApplicationState() {
        this.sessionData = new HashMap<>();
    }
    
    // User authentication state management
    public void setCurrentUser(User user) {
        this.currentUser = user != null ? new User(user) : null;
    }
    
    public User getCurrentUser() {
        return currentUser != null ? new User(currentUser) : null;
    }
    
    public boolean hasCurrentUser() {
        return currentUser != null;
    }
    
    public boolean isAuthenticated() {
        return hasCurrentUser();
    }
    
    public String getCurrentUserDisplayName() {
        if (currentUser != null) {
            return currentUser.getFirstName() + " " + currentUser.getLastName() + " (" + currentUser.getEmail() + ")";
        }
        return "Not authenticated";
    }
    
    // Company state management
    public void setCurrentCompany(Company company) {
        this.currentCompany = company != null ? new Company(company) : null;
        // Reset fiscal period when company changes
        if (company == null || (currentFiscalPeriod != null && 
                !currentFiscalPeriod.getCompanyId().equals(company.getId()))) {
            this.currentFiscalPeriod = null;
        }
    }
    
    public Company getCurrentCompany() {
        return currentCompany != null ? new Company(currentCompany) : null;
    }
    
    public boolean hasCurrentCompany() {
        return currentCompany != null;
    }
    
    // Fiscal period state management
    public void setCurrentFiscalPeriod(FiscalPeriod fiscalPeriod) {
        if (fiscalPeriod != null && currentCompany != null && 
                !fiscalPeriod.getCompanyId().equals(currentCompany.getId())) {
            throw new IllegalArgumentException("Fiscal period must belong to the current company");
        }
        this.currentFiscalPeriod = fiscalPeriod != null ? new FiscalPeriod(fiscalPeriod) : null;
    }
    
    public FiscalPeriod getCurrentFiscalPeriod() {
        return currentFiscalPeriod != null ? new FiscalPeriod(currentFiscalPeriod) : null;
    }
    
    public boolean hasCurrentFiscalPeriod() {
        return currentFiscalPeriod != null;
    }
    
    public boolean hasRequiredContext() {
        return hasCurrentCompany() && hasCurrentFiscalPeriod();
    }
    
    // Session data management
    public void setSessionData(String key, Object value) {
        sessionData.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getSessionData(String key, Class<T> type) {
        Object value = sessionData.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    public Object getSessionData(String key) {
        return sessionData.get(key);
    }
    
    public boolean hasSessionData(String key) {
        return sessionData.containsKey(key);
    }
    
    public void removeSessionData(String key) {
        sessionData.remove(key);
    }
    
    public void clearSession() {
        sessionData.clear();
    }
    
    public void clearAll() {
        currentCompany = null;
        currentFiscalPeriod = null;
        currentUser = null;
        sessionData.clear();
    }
    
    // Convenience methods for common checks
    public void requireCompany() {
        if (!hasCurrentCompany()) {
            throw new IllegalStateException("No company selected. Please select a company first.");
        }
    }
    
    public void requireFiscalPeriod() {
        if (!hasCurrentFiscalPeriod()) {
            throw new IllegalStateException("No fiscal period selected. Please select a fiscal period first.");
        }
    }
    
    public void requireContext() {
        requireCompany();
        requireFiscalPeriod();
    }
    
    // State information methods
    public String getStateDescription() {
        StringBuilder sb = new StringBuilder();
        if (hasCurrentCompany()) {
            sb.append("Company: ").append(currentCompany.getName());
            if (hasCurrentFiscalPeriod()) {
                sb.append(", Period: ").append(currentFiscalPeriod.getPeriodName());
            }
        } else {
            sb.append("No company selected");
        }
        return sb.toString();
    }
    
    public boolean isStateValid() {
        return hasCurrentCompany() && hasCurrentFiscalPeriod();
    }
    
    @Override
    public String toString() {
        return String.format("ApplicationState{user=%s, company=%s, fiscalPeriod=%s, sessionDataKeys=%s}",
                currentUser != null ? currentUser.getEmail() : "null",
                currentCompany != null ? currentCompany.getName() : "null",
                currentFiscalPeriod != null ? currentFiscalPeriod.getPeriodName() : "null",
                sessionData.keySet());
    }
}
