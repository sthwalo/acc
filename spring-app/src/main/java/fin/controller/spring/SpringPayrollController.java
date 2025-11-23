package fin.controller.spring;

import fin.model.Employee;
import fin.model.FiscalPeriod;
import fin.model.Payslip;
import fin.service.spring.SpringPayrollService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Spring REST Controller for payroll operations.
 */
@RestController
@RequestMapping("/api/v1/payroll")
public class SpringPayrollController {

    private final SpringPayrollService payrollService;

    public SpringPayrollController(SpringPayrollService payrollService) {
        System.out.println("=== DEBUG: SpringPayrollController constructor called ===");
        this.payrollService = payrollService;
        System.out.println("DEBUG: payrollService injected: " + (payrollService != null));
        if (payrollService != null) {
            System.out.println("DEBUG: payrollService class: " + payrollService.getClass().getName());
        }
    }

    // ===== EMPLOYEE MANAGEMENT ENDPOINTS =====

    /**
     * List employees with pagination and filtering
     */
    @GetMapping("/employees")
    public ResponseEntity<Page<Employee>> listEmployees(
            @RequestParam Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "firstName") String sort,
            @RequestParam(required = false) String search) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
            Page<Employee> employees = payrollService.getEmployeesByCompany(companyId, pageable, search);
            return ResponseEntity.ok(employees);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create a new employee
     */
    @PostMapping("/employees")
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        try {
            Employee createdEmployee = payrollService.createEmployee(
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getCompanyId(),
                employee.getBasicSalary(),
                employee.getDateOfBirth(),
                employee.getDateEngaged(),
                employee.getIdNumber(),
                employee.getEmail()
            );
            return ResponseEntity.ok(createdEmployee);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get employee by ID
     */
    @GetMapping("/employees/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        Optional<Employee> employee = payrollService.getEmployeeById(id);
        return employee.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an employee
     */
    @PutMapping("/employees/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        try {
            Employee updatedEmployee = payrollService.updateEmployee(id, employee);
            return ResponseEntity.ok(updatedEmployee);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete (deactivate) an employee
     */
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> deactivateEmployee(@PathVariable Long id) {
        try {
            payrollService.deactivateEmployee(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== PAYROLL PERIOD MANAGEMENT ENDPOINTS =====

    /**
     * List payroll periods for a company
     */
    @GetMapping("/periods")
    public ResponseEntity<List<FiscalPeriod>> getPayrollPeriods(
            @RequestParam Long companyId,
            @RequestParam(required = false) String status) {
        try {
            List<FiscalPeriod> periods = payrollService.getPayrollPeriodsByCompany(companyId);
            // Filter by status if provided
            if (status != null) {
                periods = periods.stream()
                    .filter(p -> status.equals("processed") ? p.isPayrollProcessed() : !p.isPayrollProcessed())
                    .toList();
            }
            return ResponseEntity.ok(periods);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create a payroll period
     */
    @PostMapping("/periods")
    public ResponseEntity<FiscalPeriod> createPayrollPeriod(@RequestBody FiscalPeriod period) {
        try {
            FiscalPeriod createdPeriod = payrollService.createPayrollPeriod(
                period.getCompanyId(),
                period.getPeriodName(),
                period.getStartDate(),
                period.getEndDate(),
                period.getPayDate()
            );
            return ResponseEntity.ok(createdPeriod);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a payroll period
     */
    @DeleteMapping("/periods/{id}")
    public ResponseEntity<Void> deletePayrollPeriod(@PathVariable Long id, @RequestParam Long companyId) {
        try {
            payrollService.deletePayrollPeriod(id, companyId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Force delete all payroll periods (admin only)
     */
    @DeleteMapping("/periods")
    public ResponseEntity<Void> forceDeleteAllPayrollPeriods(@RequestParam Long companyId) {
        try {
            payrollService.forceDeleteAllPayrollPeriods(companyId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== PAYROLL PROCESSING ENDPOINTS =====

    /**
     * Process payroll for a fiscal period
     */
    @PostMapping("/process/{fiscalPeriodId}")
    public ResponseEntity<SpringPayrollService.PayrollProcessingResult> processPayroll(
            @PathVariable Long fiscalPeriodId) {
        System.out.println("=== DEBUG: processPayroll method ENTERED ===");
        System.out.println("DEBUG: payrollService is null? " + (payrollService == null));
        System.out.println("DEBUG: Fiscal period ID: " + fiscalPeriodId);
        try {
            System.out.println("Controller: Processing payroll for fiscal period: " + fiscalPeriodId);
            SpringPayrollService.PayrollProcessingResult result = payrollService.processPayroll(fiscalPeriodId);
            System.out.println("Controller: Payroll processing result: " + result);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            System.out.println("Controller: IllegalArgumentException: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.out.println("Controller: Unexpected exception: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ===== PAYSLIP GENERATION ENDPOINTS =====

    /**
     * Generate payslips for a fiscal period
     */
    @PostMapping("/payslips/generate")
    public ResponseEntity<Void> generatePayslips(@RequestBody PayslipGenerationRequest request) {
        try {
            payrollService.generatePayslips(request.getFiscalPeriodId(), request.getEmployeeIds());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get payslips for a fiscal period
     */
    @GetMapping("/payslips/period/{fiscalPeriodId}")
    public ResponseEntity<List<Payslip>> getPayslipsByPeriod(@PathVariable Long fiscalPeriodId) {
        try {
            List<Payslip> payslips = payrollService.getPayslipsByPeriod(fiscalPeriodId);
            return ResponseEntity.ok(payslips);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get payslips for an employee
     */
    @GetMapping("/payslips/employee/{employeeId}")
    public ResponseEntity<List<Payslip>> getPayslipsByEmployee(@PathVariable Long employeeId) {
        try {
            List<Payslip> payslips = payrollService.getPayslipsByEmployee(employeeId);
            return ResponseEntity.ok(payslips);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== PAYROLL REPORTS ENDPOINTS =====

    /**
     * Get payroll summary report
     */
    @GetMapping("/reports/summary")
    public ResponseEntity<SpringPayrollService.PayrollSummary> getPayrollSummaryReport(
            @RequestParam Long fiscalPeriodId,
            @RequestParam(defaultValue = "PDF") String format) {
        try {
            SpringPayrollService.PayrollSummary summary = payrollService.getPayrollSummary(fiscalPeriodId);
            // TODO: Implement PDF/Excel format generation
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get employee payroll report
     */
    @GetMapping("/reports/employee")
    public ResponseEntity<List<Payslip>> getEmployeePayrollReport(
            @RequestParam Long employeeId,
            @RequestParam Long fiscalPeriodId,
            @RequestParam(defaultValue = "PDF") String format) {
        try {
            List<Payslip> payslips = payrollService.getPayslipsByEmployee(employeeId);
            // Filter by fiscal period if specified
            if (fiscalPeriodId != null) {
                payslips = payslips.stream()
                    .filter(p -> p.getFiscalPeriodId().equals(fiscalPeriodId))
                    .toList();
            }
            // TODO: Implement PDF/Excel format generation
            return ResponseEntity.ok(payslips);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get EMP 201 SARS tax submission report
     */
    @GetMapping("/reports/emp201")
    public ResponseEntity<String> getEmp201Report(
            @RequestParam Long fiscalPeriodId,
            @RequestParam(defaultValue = "PDF") String format) {
        try {
            String report = payrollService.generateEmp201Report(fiscalPeriodId);
            // TODO: Implement PDF format generation
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== DOCUMENT MANAGEMENT ENDPOINTS =====

    /**
     * Upload payroll document
     */
    @PostMapping("/documents")
    public ResponseEntity<Void> uploadPayrollDocument(@RequestBody DocumentUploadRequest request) {
        try {
            payrollService.uploadPayrollDocument(request.getEmployeeId(), request.getFiscalPeriodId(),
                                               request.getFileName(), request.getFileData(),
                                               request.getDocumentType());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * List payroll documents
     */
    @GetMapping("/documents")
    public ResponseEntity<List<SpringPayrollService.PayrollDocument>> listPayrollDocuments(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long fiscalPeriodId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<SpringPayrollService.PayrollDocument> documents = payrollService.listPayrollDocuments(employeeId, fiscalPeriodId, type, page, size);
            return ResponseEntity.ok(documents);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Download payroll document
     */
    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> downloadPayrollDocument(@PathVariable Long id) {
        try {
            SpringPayrollService.PayrollDocument document = payrollService.getPayrollDocument(id);
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + document.getFileName() + "\"")
                .body(document.getFileData());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete payroll document
     */
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deletePayrollDocument(@PathVariable Long id) {
        try {
            payrollService.deletePayrollDocument(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== REQUEST/RESPONSE CLASSES =====

    public static class PayrollProcessRequest {
        private Long fiscalPeriodId;

        public Long getFiscalPeriodId() { return fiscalPeriodId; }
        public void setFiscalPeriodId(Long fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }
    }

    public static class PayslipGenerationRequest {
        private Long fiscalPeriodId;
        private List<Long> employeeIds;

        public Long getFiscalPeriodId() { return fiscalPeriodId; }
        public void setFiscalPeriodId(Long fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }

        public List<Long> getEmployeeIds() { return employeeIds; }
        public void setEmployeeIds(List<Long> employeeIds) { this.employeeIds = employeeIds; }
    }

    public static class DocumentUploadRequest {
        private Long employeeId;
        private Long fiscalPeriodId;
        private String fileName;
        private byte[] fileData;
        private String documentType;

        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

        public Long getFiscalPeriodId() { return fiscalPeriodId; }
        public void setFiscalPeriodId(Long fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public byte[] getFileData() { return fileData; }
        public void setFileData(byte[] fileData) { this.fileData = fileData; }

        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
    }
}