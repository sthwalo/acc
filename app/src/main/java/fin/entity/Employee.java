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
 */

package fin.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Employee entity representing an employee in the payroll system
 * Fully integrated with the FIN financial management system
 */
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "employee_number")
    private String employeeNumber;
    @Column(name = "title")
    private String title;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "second_name")
    private String secondName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "email")
    private String email;
    @Column(name = "phone")
    private String phone;
    @Column(name = "position")
    private String position;
    @Column(name = "department")
    private String department;
    @Column(name = "hire_date")
    private LocalDate hireDate;
    @Column(name = "termination_date")
    private LocalDate terminationDate;
    @Column(name = "is_active")
    private boolean isActive = true;
    
    // Address Information
    @Column(name = "address_line1")
    private String addressLine1;
    @Column(name = "address_line2")
    private String addressLine2;
    @Column(name = "city")
    private String city;
    @Column(name = "province")
    private String province;
    @Column(name = "postal_code")
    private String postalCode;
    @Column(name = "country")
    private String country = "ZA";
    
    // Banking Information
    @Column(name = "bank_name")
    private String bankName;
    @Column(name = "account_holder_name")
    private String accountHolderName;
    @Column(name = "account_number")
    private String accountNumber;
    @Column(name = "branch_code")
    private String branchCode;
    @Column(name = "account_type")
    private String accountType = "SAVINGS";
    
    // Employment Details
    @Column(name = "employment_type")
    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType = EmploymentType.PERMANENT;
    @Column(name = "salary_type")
    @Enumerated(EnumType.STRING)
    private SalaryType salaryType = SalaryType.MONTHLY;
    @Column(name = "basic_salary")
    private BigDecimal basicSalary;
    @Column(name = "overtime_rate")
    private BigDecimal overtimeRate = new BigDecimal("1.5");
    
    // Tax Information
    @Column(name = "tax_number")
    private String taxNumber;
    @Column(name = "tax_rebate_code")
    private String taxRebateCode = "A";
    @Column(name = "uif_number")
    private String uifNumber;
    @Column(name = "medical_aid_number")
    private String medicalAidNumber;
    @Column(name = "pension_fund_number")
    private String pensionFundNumber;
    
    // Audit Fields
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "created_by")
    private String createdBy;
    @Column(name = "updated_by")
    private String updatedBy;
    
    // Enums
    public enum EmploymentType {
        PERMANENT, CONTRACT, TEMPORARY
    }
    
    public enum SalaryType {
        MONTHLY, WEEKLY, HOURLY, DAILY
    }
    
    // Constructors
    public Employee() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Employee(Long initialCompanyId, String initialEmployeeNumber, String initialFirstName, String initialLastName, 
                   String initialPosition, BigDecimal initialBasicSalary) {
        this();
        this.companyId = initialCompanyId;
        this.employeeNumber = initialEmployeeNumber;
        this.firstName = initialFirstName;
        this.lastName = initialLastName;
        this.position = initialPosition;
        this.basicSalary = initialBasicSalary;
    }
    
    // Calculated properties
    /**
     * Returns the full name of the employee by concatenating first and last names.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * how the full name is constructed (e.g., including middle names or honorifics).
     * When overriding, ensure the returned string is never null and represents
     * a meaningful full name for the employee.
     * </p>
     *
     * @return the full name as "firstName lastName"
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    /**
     * Returns a display name combining employee number and full name.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * the display format. When overriding, ensure the returned string uniquely
     * identifies the employee and includes both the employee number and name.
     * </p>
     *
     * @return display name in format "employeeNumber - fullName"
     */
    public String getDisplayName() {
        return employeeNumber + " - " + getFullName();
    }
    
    /**
     * Determines if this employee is currently employed (active and not terminated).
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom logic for determining current employment status (e.g., considering
     * leave of absence or probation periods). When overriding, ensure the logic
     * accurately reflects the employee's current employment state.
     * </p>
     *
     * @return true if employee is active and has no termination date, false otherwise
     */
    public boolean isCurrentEmployee() {
        return isActive && terminationDate == null;
    }
    
    // Getters and Setters
    /**
     * Gets the unique identifier for this employee.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * ID retrieval logic. When overriding, ensure the returned value uniquely
     * identifies the employee instance.
     * </p>
     *
     * @return the employee ID, may be null for unsaved employees
     */
    public Long getId() {
        return id;
    }
    
    /**
     * Sets the unique identifier for this employee.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom ID validation or side effects. When overriding, ensure the ID is
     * properly validated and stored.
     * </p>
     *
     * @param newId the new employee ID to set
     */
    public void setId(Long newId) {
        this.id = newId;
    }
    
    /**
     * Gets the company ID this employee belongs to.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * company ID retrieval. When overriding, ensure the returned value correctly
     * identifies the associated company.
     * </p>
     *
     * @return the company ID
     */
    public Long getCompanyId() {
        return companyId;
    }
    
    /**
     * Sets the company ID this employee belongs to.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom company ID validation. When overriding, ensure the company ID is
     * valid and properly associated.
     * </p>
     *
     * @param newCompanyId the new company ID to set
     */
    public void setCompanyId(Long newCompanyId) {
        this.companyId = newCompanyId;
    }
    
    /**
     * Gets the employee number (unique identifier within the company).
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * employee number retrieval. When overriding, ensure the returned value
     * uniquely identifies the employee within the company.
     * </p>
     *
     * @return the employee number
     */
    public String getEmployeeNumber() {
        return employeeNumber;
    }
    
    /**
     * Sets the employee number (unique identifier within the company).
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom employee number validation. When overriding, ensure the employee
     * number is unique within the company.
     * </p>
     *
     * @param newEmployeeNumber the new employee number to set
     */
    public void setEmployeeNumber(String newEmployeeNumber) {
        this.employeeNumber = newEmployeeNumber;
    }
    
    /**
     * Gets the employee's title (e.g., Mr, Mrs, Dr).
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * title retrieval. When overriding, ensure the returned value is a valid
     * honorific title.
     * </p>
     *
     * @return the employee's title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Sets the employee's title (e.g., Mr, Mrs, Dr).
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom title validation. When overriding, ensure the title is a valid
     * honorific.
     * </p>
     *
     * @param newTitle the new title to set
     */
    public void setTitle(String newTitle) {
        this.title = newTitle;
    }
    
    /**
     * Gets the employee's first name.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * first name retrieval. When overriding, ensure the returned value represents
     * the employee's given name.
     * </p>
     *
     * @return the employee's first name
     */
    public String getFirstName() {
        return firstName;
    }
    
    /**
     * Sets the employee's first name.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom first name validation. When overriding, ensure the name is properly
     * formatted and validated.
     * </p>
     *
     * @param newFirstName the new first name to set
     */
    public void setFirstName(String newFirstName) {
        this.firstName = newFirstName;
    }
    
    /**
     * Gets the employee's second name (middle name).
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * second name retrieval. When overriding, ensure the returned value represents
     * the employee's middle name if present.
     * </p>
     *
     * @return the employee's second name, may be null
     */
    public String getSecondName() {
        return secondName;
    }
    
    /**
     * Sets the employee's second name (middle name).
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom second name validation. When overriding, ensure the name is properly
     * formatted if provided.
     * </p>
     *
     * @param newSecondName the new second name to set, may be null
     */
    public void setSecondName(String newSecondName) {
        this.secondName = newSecondName;
    }
    
    /**
     * Gets the employee's last name (surname).
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * last name retrieval. When overriding, ensure the returned value represents
     * the employee's family name.
     * </p>
     *
     * @return the employee's last name
     */
    public String getLastName() {
        return lastName;
    }
    
    /**
     * Sets the employee's last name (surname).
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom last name validation. When overriding, ensure the name is properly
     * formatted and validated.
     * </p>
     *
     * @param newLastName the new last name to set
     */
    public void setLastName(String newLastName) {
        this.lastName = newLastName;
    }
    
    /**
     * Gets the employee's email address.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * email retrieval. When overriding, ensure the returned value is a valid
     * email address format if present.
     * </p>
     *
     * @return the employee's email address, may be null
     */
    public String getEmail() {
        return email;
    }
    
    /**
     * Sets the employee's email address.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom email validation. When overriding, ensure the email format is
     * validated before storage.
     * </p>
     *
     * @param newEmail the new email address to set, may be null
     */
    public void setEmail(String newEmail) {
        this.email = newEmail;
    }
    
    /**
     * Gets the employee's phone number.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * phone number retrieval. When overriding, ensure the returned value represents
     * a valid phone number if present.
     * </p>
     *
     * @return the employee's phone number, may be null
     */
    public String getPhone() {
        return phone;
    }
    
    /**
     * Sets the employee's phone number.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom phone number validation. When overriding, ensure the phone number
     * format is validated before storage.
     * </p>
     *
     * @param newPhone the new phone number to set, may be null
     */
    public void setPhone(String newPhone) {
        this.phone = newPhone;
    }
    
    /**
     * Gets the employee's job position.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * position retrieval. When overriding, ensure the returned value accurately
     * represents the employee's job role.
     * </p>
     *
     * @return the employee's position
     */
    public String getPosition() {
        return position;
    }
    
    /**
     * Sets the employee's job position.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom position validation. When overriding, ensure the position is valid
     * within the organizational structure.
     * </p>
     *
     * @param newPosition the new position to set
     */
    public void setPosition(String newPosition) {
        this.position = newPosition;
    }
    
    /**
     * Gets the employee's department.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * department retrieval. When overriding, ensure the returned value represents
     * a valid department within the organization.
     * </p>
     *
     * @return the employee's department, may be null
     */
    public String getDepartment() {
        return department;
    }
    
    /**
     * Sets the employee's department.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom department validation. When overriding, ensure the department exists
     * within the organizational structure.
     * </p>
     *
     * @param newDepartment the new department to set, may be null
     */
    public void setDepartment(String newDepartment) {
        this.department = newDepartment;
    }
    
    /**
     * Gets the employee's hire date.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * hire date retrieval. When overriding, ensure the returned date represents
     * when the employee officially started employment.
     * </p>
     *
     * @return the employee's hire date, may be null
     */
    public LocalDate getHireDate() {
        return hireDate;
    }
    
    /**
     * Sets the employee's hire date.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom hire date validation. When overriding, ensure the date is valid and
     * represents the actual start of employment.
     * </p>
     *
     * @param newHireDate the new hire date to set, may be null
     */
    public void setHireDate(LocalDate newHireDate) {
        this.hireDate = newHireDate;
    }
    
    /**
     * Gets the employee's termination date.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * termination date retrieval. When overriding, ensure the returned date represents
     * when the employee officially ended employment, if applicable.
     * </p>
     *
     * @return the employee's termination date, may be null
     */
    public LocalDate getTerminationDate() {
        return terminationDate;
    }
    
    /**
     * Sets the employee's termination date.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom termination date validation. When overriding, ensure the date is valid
     * and represents the actual end of employment.
     * </p>
     *
     * @param newTerminationDate the new termination date to set, may be null
     */
    public void setTerminationDate(LocalDate newTerminationDate) {
        this.terminationDate = newTerminationDate;
    }
    
    /**
     * Gets whether the employee is currently active.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * active status determination. When overriding, consider both hire date and
     * termination date to determine current employment status.
     * </p>
     *
     * @return true if the employee is currently active, false otherwise
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Sets whether the employee is currently active.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom active status validation. When overriding, ensure the status accurately
     * reflects the employee's current employment state.
     * </p>
     *
     * @param newActive the new active status to set
     */
    public void setActive(boolean newActive) {
        this.isActive = newActive;
    }
    
    // Address Information
    /**
     * Gets the employee's address line 1.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * address line 1 retrieval. When overriding, ensure the returned value represents
     * the primary address line if present.
     * </p>
     *
     * @return the employee's address line 1, may be null
     */
    public String getAddressLine1() {
        return addressLine1;
    }
    
    /**
     * Sets the employee's address line 1.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom address line 1 validation. When overriding, ensure the address line
     * is properly formatted before storage.
     * </p>
     *
     * @param newAddressLine1 the new address line 1 to set, may be null
     */
    public void setAddressLine1(String newAddressLine1) {
        this.addressLine1 = newAddressLine1;
    }
    
    /**
     * Gets the employee's address line 2.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * address line 2 retrieval. When overriding, ensure the returned value represents
     * the secondary address line if present.
     * </p>
     *
     * @return the employee's address line 2, may be null
     */
    public String getAddressLine2() {
        return addressLine2;
    }
    
    /**
     * Sets the employee's address line 2.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom address line 2 validation. When overriding, ensure the address line
     * is properly formatted before storage.
     * </p>
     *
     * @param newAddressLine2 the new address line 2 to set, may be null
     */
    public void setAddressLine2(String newAddressLine2) {
        this.addressLine2 = newAddressLine2;
    }
    
    /**
     * Gets the employee's province.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * province retrieval. When overriding, ensure the returned value represents
     * a valid province name if present.
     * </p>
     *
     * @return the employee's province, may be null
     */
    public String getProvince() {
        return province;
    }
    
    /**
     * Sets the employee's province.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom province validation. When overriding, ensure the province is valid
     * within the geographic context.
     * </p>
     *
     * @param newProvince the new province to set, may be null
     */
    public void setProvince(String newProvince) {
        this.province = newProvince;
    }
    
    /**
     * Gets the employee's city.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * city retrieval. When overriding, ensure the returned value represents
     * a valid city name if present.
     * </p>
     *
     * @return the employee's city, may be null
     */
    public String getCity() {
        return city;
    }
    
    /**
     * Sets the employee's city.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom city validation. When overriding, ensure the city name is valid
     * within the geographic context.
     * </p>
     *
     * @param newCity the new city to set, may be null
     */
    public void setCity(String newCity) {
        this.city = newCity;
    }
    
    /**
     * Gets the employee's postal code.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * postal code retrieval. When overriding, ensure the returned value represents
     * a valid postal code format if present.
     * </p>
     *
     * @return the employee's postal code, may be null
     */
    public String getPostalCode() {
        return postalCode;
    }
    
    /**
     * Sets the employee's postal code.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom postal code validation. When overriding, ensure the postal code format
     * is valid for the geographic region.
     * </p>
     *
     * @param newPostalCode the new postal code to set, may be null
     */
    public void setPostalCode(String newPostalCode) {
        this.postalCode = newPostalCode;
    }
    
    /**
     * Gets the employee's country.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * country retrieval. When overriding, ensure the returned value represents
     * a valid country name if present.
     * </p>
     *
     * @return the employee's country, may be null
     */
    public String getCountry() {
        return country;
    }
    
    /**
     * Sets the employee's country.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom country validation. When overriding, ensure the country name is valid
     * and recognized internationally.
     * </p>
     *
     * @param newCountry the new country to set, may be null
     */
    public void setCountry(String newCountry) {
        this.country = newCountry;
    }
    
    // Banking Information
    /**
     * Gets the employee's bank name.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * bank name retrieval. When overriding, ensure the returned value represents
     * a valid bank name if present.
     * </p>
     *
     * @return the employee's bank name, may be null
     */
    public String getBankName() {
        return bankName;
    }
    
    /**
     * Sets the employee's bank name.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom bank name validation. When overriding, ensure the bank name is valid
     * and recognized.
     * </p>
     *
     * @param newBankName the new bank name to set, may be null
     */
    public void setBankName(String newBankName) {
        this.bankName = newBankName;
    }
    
    /**
     * Gets the account holder name for the employee's bank account.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * account holder name retrieval. When overriding, ensure the returned value
     * represents the authorized account holder.
     * </p>
     *
     * @return the account holder name, may be null
     */
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    /**
     * Sets the account holder name for the employee's bank account.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom account holder name validation. When overriding, ensure the name
     * matches the bank account authorization.
     * </p>
     *
     * @param newAccountHolderName the new account holder name to set, may be null
     */
    public void setAccountHolderName(String newAccountHolderName) {
        this.accountHolderName = newAccountHolderName;
    }
    
    /**
     * Gets the employee's bank account number.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * account number retrieval. When overriding, ensure the returned value represents
     * a valid account number format if present.
     * </p>
     *
     * @return the employee's account number, may be null
     */
    public String getAccountNumber() {
        return accountNumber;
    }
    
    /**
     * Sets the employee's bank account number.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom account number validation. When overriding, ensure the account number
     * format is valid for the specified bank.
     * </p>
     *
     * @param newAccountNumber the new account number to set, may be null
     */
    public void setAccountNumber(String newAccountNumber) {
        this.accountNumber = newAccountNumber;
    }
    
    /**
     * Gets the employee's bank branch code.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * branch code retrieval. When overriding, ensure the returned value represents
     * a valid branch code for the specified bank.
     * </p>
     *
     * @return the employee's branch code, may be null
     */
    public String getBranchCode() {
        return branchCode;
    }
    
    /**
     * Sets the employee's bank branch code.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom branch code validation. When overriding, ensure the branch code is
     * valid for the specified bank.
     * </p>
     *
     * @param newBranchCode the new branch code to set, may be null
     */
    public void setBranchCode(String newBranchCode) {
        this.branchCode = newBranchCode;
    }
    
    /**
     * Gets the employee's bank account type.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * account type retrieval. When overriding, ensure the returned value represents
     * a valid account type (e.g., SAVINGS, CHECKING).
     * </p>
     *
     * @return the employee's account type, defaults to "SAVINGS"
     */
    public String getAccountType() {
        return accountType;
    }
    
    /**
     * Sets the employee's bank account type.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom account type validation. When overriding, ensure the account type is
     * valid for banking operations.
     * </p>
     *
     * @param newAccountType the new account type to set
     */
    public void setAccountType(String newAccountType) {
        this.accountType = newAccountType;
    }
    
    // Employment Details
    /**
     * Gets the employee's employment type.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * employment type retrieval. When overriding, ensure the returned value represents
     * a valid employment type (PERMANENT, CONTRACT, TEMPORARY).
     * </p>
     *
     * @return the employee's employment type, defaults to PERMANENT
     */
    public EmploymentType getEmploymentType() {
        return employmentType;
    }
    
    /**
     * Sets the employee's employment type.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom employment type validation. When overriding, ensure the employment type
     * is valid for payroll processing.
     * </p>
     *
     * @param newEmploymentType the new employment type to set
     */
    public void setEmploymentType(EmploymentType newEmploymentType) {
        this.employmentType = newEmploymentType;
    }
    
    /**
     * Gets the employee's salary type.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * salary type retrieval. When overriding, ensure the returned value represents
     * a valid salary type (MONTHLY, WEEKLY, HOURLY, DAILY).
     * </p>
     *
     * @return the employee's salary type, defaults to MONTHLY
     */
    public SalaryType getSalaryType() {
        return salaryType;
    }
    
    /**
     * Sets the employee's salary type.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom salary type validation. When overriding, ensure the salary type is
     * valid for payroll calculations.
     * </p>
     *
     * @param newSalaryType the new salary type to set
     */
    public void setSalaryType(SalaryType newSalaryType) {
        this.salaryType = newSalaryType;
    }
    
    /**
     * Gets the employee's basic salary.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * basic salary retrieval. When overriding, ensure the returned value represents
     * the employee's base compensation amount.
     * </p>
     *
     * @return the employee's basic salary, may be null
     */
    public BigDecimal getBasicSalary() {
        return basicSalary;
    }
    
    /**
     * Sets the employee's basic salary.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom basic salary validation. When overriding, ensure the salary amount is
     * valid and within organizational limits.
     * </p>
     *
     * @param newBasicSalary the new basic salary to set, may be null
     */
    public void setBasicSalary(BigDecimal newBasicSalary) {
        this.basicSalary = newBasicSalary;
    }
    
    /**
     * Gets the employee's overtime rate multiplier.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * overtime rate retrieval. When overriding, ensure the returned value represents
     * a valid overtime multiplier (typically 1.5 for time-and-a-half).
     * </p>
     *
     * @return the employee's overtime rate, defaults to 1.5
     */
    public BigDecimal getOvertimeRate() {
        return overtimeRate;
    }
    
    /**
     * Sets the employee's overtime rate multiplier.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom overtime rate validation. When overriding, ensure the rate is valid
     * for payroll calculations and complies with labor regulations.
     * </p>
     *
     * @param newOvertimeRate the new overtime rate to set, may be null
     */
    public void setOvertimeRate(BigDecimal newOvertimeRate) {
        this.overtimeRate = newOvertimeRate;
    }
    
    // Tax Information
    /**
     * Gets the employee's tax number.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * tax number retrieval. When overriding, ensure the returned value represents
     * a valid tax identification number if present.
     * </p>
     *
     * @return the employee's tax number, may be null
     */
    public String getTaxNumber() {
        return taxNumber;
    }
    
    /**
     * Sets the employee's tax number.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom tax number validation. When overriding, ensure the tax number format
     * is valid for tax authority requirements.
     * </p>
     *
     * @param newTaxNumber the new tax number to set, may be null
     */
    public void setTaxNumber(String newTaxNumber) {
        this.taxNumber = newTaxNumber;
    }
    
    /**
     * Gets the employee's tax rebate code.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * tax rebate code retrieval. When overriding, ensure the returned value represents
     * a valid tax rebate code for tax calculations.
     * </p>
     *
     * @return the employee's tax rebate code, defaults to "A"
     */
    public String getTaxRebateCode() {
        return taxRebateCode;
    }
    
    /**
     * Sets the employee's tax rebate code.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom tax rebate code validation. When overriding, ensure the rebate code
     * is valid for tax authority requirements.
     * </p>
     *
     * @param newTaxRebateCode the new tax rebate code to set, may be null
     */
    public void setTaxRebateCode(String newTaxRebateCode) {
        this.taxRebateCode = newTaxRebateCode;
    }
    
    /**
     * Gets the employee's UIF (Unemployment Insurance Fund) number.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * UIF number retrieval. When overriding, ensure the returned value represents
     * a valid UIF number if present.
     * </p>
     *
     * @return the employee's UIF number, may be null
     */
    public String getUifNumber() {
        return uifNumber;
    }
    
    /**
     * Sets the employee's UIF (Unemployment Insurance Fund) number.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom UIF number validation. When overriding, ensure the UIF number format
     * is valid for unemployment insurance requirements.
     * </p>
     *
     * @param newUifNumber the new UIF number to set, may be null
     */
    public void setUifNumber(String newUifNumber) {
        this.uifNumber = newUifNumber;
    }
    
    /**
     * Gets the employee's medical aid number.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * medical aid number retrieval. When overriding, ensure the returned value
     * represents a valid medical aid membership number if present.
     * </p>
     *
     * @return the employee's medical aid number, may be null
     */
    public String getMedicalAidNumber() {
        return medicalAidNumber;
    }
    
    /**
     * Sets the employee's medical aid number.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom medical aid number validation. When overriding, ensure the medical aid
     * number is valid for the specified medical aid scheme.
     * </p>
     *
     * @param newMedicalAidNumber the new medical aid number to set, may be null
     */
    public void setMedicalAidNumber(String newMedicalAidNumber) {
        this.medicalAidNumber = newMedicalAidNumber;
    }
    
    /**
     * Gets the employee's pension fund number.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * pension fund number retrieval. When overriding, ensure the returned value
     * represents a valid pension fund membership number if present.
     * </p>
     *
     * @return the employee's pension fund number, may be null
     */
    public String getPensionFundNumber() {
        return pensionFundNumber;
    }
    
    /**
     * Sets the employee's pension fund number.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom pension fund number validation. When overriding, ensure the pension fund
     * number is valid for the specified pension scheme.
     * </p>
     *
     * @param newPensionFundNumber the new pension fund number to set, may be null
     */
    public void setPensionFundNumber(String newPensionFundNumber) {
        this.pensionFundNumber = newPensionFundNumber;
    }
    
    // Audit Fields
    /**
     * Gets the timestamp when this employee record was created.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * creation timestamp retrieval. When overriding, ensure the returned value
     * represents when the employee record was first created in the system.
     * </p>
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the timestamp when this employee record was created.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom creation timestamp validation. When overriding, ensure the timestamp
     * represents a valid creation time.
     * </p>
     *
     * @param newCreatedAt the new creation timestamp to set, may be null
     */
    public void setCreatedAt(LocalDateTime newCreatedAt) {
        this.createdAt = newCreatedAt;
    }
    
    /**
     * Gets the timestamp when this employee record was last updated.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * update timestamp retrieval. When overriding, ensure the returned value
     * represents when the employee record was last modified in the system.
     * </p>
     *
     * @return the last update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Sets the timestamp when this employee record was last updated.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom update timestamp validation. When overriding, ensure the timestamp
     * represents a valid update time.
     * </p>
     *
     * @param newUpdatedAt the new update timestamp to set, may be null
     */
    public void setUpdatedAt(LocalDateTime newUpdatedAt) {
        this.updatedAt = newUpdatedAt;
    }
    
    /**
     * Gets the username of the user who created this employee record.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * created by user retrieval. When overriding, ensure the returned value
     * represents a valid system user who created the record.
     * </p>
     *
     * @return the username of the creator, may be null
     */
    public String getCreatedBy() {
        return createdBy;
    }
    
    /**
     * Sets the username of the user who created this employee record.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom creator validation. When overriding, ensure the username represents
     * a valid system user.
     * </p>
     *
     * @param newCreatedBy the new creator username to set, may be null
     */
    public void setCreatedBy(String newCreatedBy) {
        this.createdBy = newCreatedBy;
    }
    
    /**
     * Gets the username of the user who last updated this employee record.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to customize
     * updated by user retrieval. When overriding, ensure the returned value
     * represents a valid system user who last modified the record.
     * </p>
     *
     * @return the username of the last updater, may be null
     */
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    /**
     * Sets the username of the user who last updated this employee record.
     * <p>
     * <b>Override Guidelines:</b> Subclasses may override this method to implement
     * custom updater validation. When overriding, ensure the username represents
     * a valid system user.
     * </p>
     *
     * @param newUpdatedBy the new updater username to set, may be null
     */
    public void setUpdatedBy(String newUpdatedBy) {
        this.updatedBy = newUpdatedBy;
    }
    
    public String getEmployeeCode() {
        return employeeNumber;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeNumber = employeeCode;
    }

    public LocalDate getDateOfBirth() {
        // TODO: Add date of birth field
        return null;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        // TODO: Add date of birth field
    }

    public LocalDate getDateEngaged() {
        return hireDate;
    }

    public void setDateEngaged(LocalDate dateEngaged) {
        this.hireDate = dateEngaged;
    }

    public String getIdNumber() {
        return taxNumber;
    }

    public void setIdNumber(String idNumber) {
        this.taxNumber = idNumber;
    }
}
