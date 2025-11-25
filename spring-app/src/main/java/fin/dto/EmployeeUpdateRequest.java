package fin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating employees
 * All fields are optional to support partial updates
 */
public class EmployeeUpdateRequest {
    @JsonProperty("title")
    private String title;
    @JsonProperty("firstName")
    private String firstName;
    @JsonProperty("secondName")
    private String secondName;
    @JsonProperty("lastName")
    private String lastName;
    @JsonProperty("email")
    private String email;
    @JsonProperty("phone")
    private String phone;
    @JsonProperty("position")
    private String position;
    @JsonProperty("department")
    private String department;
    @JsonProperty("hireDate")
    private LocalDate hireDate;
    @JsonProperty("terminationDate")
    private LocalDate terminationDate;
    @JsonProperty("isActive")
    private Boolean isActive;
    @JsonProperty("addressLine1")
    private String addressLine1;
    @JsonProperty("addressLine2")
    private String addressLine2;
    @JsonProperty("city")
    private String city;
    @JsonProperty("province")
    private String province;
    @JsonProperty("postalCode")
    private String postalCode;
    @JsonProperty("country")
    private String country;
    @JsonProperty("bankName")
    private String bankName;
    @JsonProperty("accountHolderName")
    private String accountHolderName;
    @JsonProperty("accountNumber")
    private String accountNumber;
    @JsonProperty("branchCode")
    private String branchCode;
    @JsonProperty("accountType")
    private String accountType;
    @JsonProperty("employmentType")
    private String employmentType;
    @JsonProperty("salaryType")
    private String salaryType;
    @JsonProperty("basicSalary")
    private BigDecimal basicSalary;
    @JsonProperty("overtimeRate")
    private BigDecimal overtimeRate;
    @JsonProperty("taxNumber")
    private String taxNumber;
    @JsonProperty("taxRebateCode")
    private String taxRebateCode;
    @JsonProperty("uifNumber")
    private String uifNumber;
    @JsonProperty("medicalAidNumber")
    private String medicalAidNumber;
    @JsonProperty("pensionFundNumber")
    private String pensionFundNumber;

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getSecondName() { return secondName; }
    public void setSecondName(String secondName) { this.secondName = secondName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public String getSalaryType() { return salaryType; }
    public void setSalaryType(String salaryType) { this.salaryType = salaryType; }

    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }

    public BigDecimal getOvertimeRate() { return overtimeRate; }
    public void setOvertimeRate(BigDecimal overtimeRate) { this.overtimeRate = overtimeRate; }

    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }

    public String getTaxRebateCode() { return taxRebateCode; }
    public void setTaxRebateCode(String taxRebateCode) { this.taxRebateCode = taxRebateCode; }

    public String getUifNumber() { return uifNumber; }
    public void setUifNumber(String uifNumber) { this.uifNumber = uifNumber; }

    public String getMedicalAidNumber() { return medicalAidNumber; }
    public void setMedicalAidNumber(String medicalAidNumber) { this.medicalAidNumber = medicalAidNumber; }

    public String getPensionFundNumber() { return pensionFundNumber; }
    public void setPensionFundNumber(String pensionFundNumber) { this.pensionFundNumber = pensionFundNumber; }
}