package fin.state;

import fin.model.Company;
import fin.model.FiscalPeriod;

import java.util.HashMap;
import java.util.Map;

/**
 * Application state management component
 * Extracted from monolithic App.java to centralize state handling
 */
public class ApplicationState {
    private Company currentCompany;
    private FiscalPeriod currentFiscalPeriod;
    private Map<String, Object> sessionData;
    
    public ApplicationState() {
        this.sessionData = new HashMap<>();
    }
    
    // Company state management
    public void setCurrentCompany(Company company) {
        this.currentCompany = company;
        // Reset fiscal period when company changes
        if (company == null || (currentFiscalPeriod != null && 
                !currentFiscalPeriod.getCompanyId().equals(company.getId()))) {
            this.currentFiscalPeriod = null;
        }
    }
    
    public Company getCurrentCompany() {
        return currentCompany;
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
        this.currentFiscalPeriod = fiscalPeriod;
    }
    
    public FiscalPeriod getCurrentFiscalPeriod() {
        return currentFiscalPeriod;
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
        return String.format("ApplicationState{company=%s, fiscalPeriod=%s, sessionDataKeys=%s}",
                currentCompany != null ? currentCompany.getName() : "null",
                currentFiscalPeriod != null ? currentFiscalPeriod.getPeriodName() : "null",
                sessionData.keySet());
    }
}
