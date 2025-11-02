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

package fin.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Data container for payslip information used in PDF generation
 * Consolidates payslip data from various sources into a unified format
 */
public class PayslipData {

    private String companyName;
    private String payrollPeriod;
    private String employeeName;
    private String employeeNumber;
    private String taxNumber;
    private String department;
    private String paymentMethod;
    private String bankingDetails;
    private List<Earning> earnings;
    private List<Deduction> deductions;
    private BigDecimal netPay;

    // Default constructor
    public PayslipData() {}

    // Constructor with main fields
    public PayslipData(String companyName, String payrollPeriod, String employeeName,
                      String employeeNumber, String taxNumber, String department,
                      String paymentMethod, String bankingDetails,
                      List<Earning> earnings, List<Deduction> deductions, BigDecimal netPay) {
        this.companyName = companyName;
        this.payrollPeriod = payrollPeriod;
        this.employeeName = employeeName;
        this.employeeNumber = employeeNumber;
        this.taxNumber = taxNumber;
        this.department = department;
        this.paymentMethod = paymentMethod;
        this.bankingDetails = bankingDetails;
        this.earnings = earnings != null ? new ArrayList<>(earnings) : new ArrayList<>();
        this.deductions = deductions != null ? new ArrayList<>(deductions) : new ArrayList<>();
        this.netPay = netPay;
    }

    // Getters and setters
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getPayrollPeriod() { return payrollPeriod; }
    public void setPayrollPeriod(String payrollPeriod) { this.payrollPeriod = payrollPeriod; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }

    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getBankingDetails() { return bankingDetails; }
    public void setBankingDetails(String bankingDetails) { this.bankingDetails = bankingDetails; }

    public List<Earning> getEarnings() { 
        return earnings != null ? new ArrayList<>(earnings) : new ArrayList<>();
    }
    public void setEarnings(List<Earning> earnings) { 
        this.earnings = earnings != null ? new ArrayList<>(earnings) : new ArrayList<>();
    }

    public List<Deduction> getDeductions() { 
        return deductions != null ? new ArrayList<>(deductions) : new ArrayList<>();
    }
    public void setDeductions(List<Deduction> deductions) { 
        this.deductions = deductions != null ? new ArrayList<>(deductions) : new ArrayList<>();
    }

    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }

    /**
     * Inner class for earnings data
     */
    public static class Earning {
        private String description;
        private BigDecimal amount;

        public Earning() {}

        public Earning(String description, BigDecimal amount) {
            this.description = description;
            this.amount = amount;
        }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    /**
     * Inner class for deductions data
     */
    public static class Deduction {
        private String description;
        private BigDecimal amount;

        public Deduction() {}

        public Deduction(String description, BigDecimal amount) {
            this.description = description;
            this.amount = amount;
        }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}