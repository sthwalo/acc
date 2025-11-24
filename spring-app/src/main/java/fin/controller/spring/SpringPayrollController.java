package fin.controller.spring;

import fin.model.Company;
import fin.model.Employee;
import fin.model.FiscalPeriod;
import fin.model.Payslip;
import fin.service.spring.EmailService;
import fin.service.spring.SpringPayslipPdfService;
import fin.service.spring.SpringPayrollService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;

/**
 * Spring REST Controller for payroll operations.
 */
@RestController
@RequestMapping("/api/v1/payroll")
public class SpringPayrollController {

    private final SpringPayrollService payrollService;
    private final SpringPayslipPdfService payslipPdfService;
    private static final Logger LOGGER = Logger.getLogger(SpringPayrollController.class.getName());

    public SpringPayrollController(SpringPayrollService payrollService, SpringPayslipPdfService payslipPdfService) {
        this.payrollService = payrollService;
        this.payslipPdfService = payslipPdfService;
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
     * @param fiscalPeriodId the ID of the fiscal period to process payroll for
     * @return the payroll processing response
     */
    @PostMapping("/process/{fiscalPeriodId}")
    public ResponseEntity<PayrollProcessingResponse> processPayroll(
            @PathVariable Long fiscalPeriodId,
            @RequestParam(defaultValue = "false") boolean reprocess) {
        try {
            SpringPayrollService.PayrollProcessingResult result;

            if (reprocess) {
                result = payrollService.reprocessPayroll(fiscalPeriodId);
            } else {
                result = payrollService.processPayroll(fiscalPeriodId);
            }

            // Convert to frontend-expected format
            PayrollProcessingResponse response = new PayrollProcessingResponse(
                true,
                "Payroll " + (reprocess ? "re" : "") + "processed successfully for " + result.getEmployeeCount() + " employees",
                new PayrollProcessingData(
                    result.getEmployeeCount(),
                    result.getTotalGross().doubleValue(),
                    result.getTotalDeductions().doubleValue(),
                    result.getTotalNet().doubleValue(),
                    result.getEmployeeCount() // payslipsGenerated = employeeCount for now
                )
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            PayrollProcessingResponse errorResponse = new PayrollProcessingResponse(
                false,
                e.getMessage(),
                null
            );
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            PayrollProcessingResponse errorResponse = new PayrollProcessingResponse(
                false,
                "An unexpected error occurred while processing payroll",
                null
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Reprocess payroll for a fiscal period (delete existing payslips and recalculate)
     * @param fiscalPeriodId the ID of the fiscal period to reprocess payroll for
     * @return the payroll reprocessing response
     */
    @PostMapping("/reprocess/{fiscalPeriodId}")
    public ResponseEntity<PayrollProcessingResponse> reprocessPayroll(
            @PathVariable Long fiscalPeriodId) {
        try {
            SpringPayrollService.PayrollProcessingResult result = payrollService.reprocessPayroll(fiscalPeriodId);

            // Convert to frontend-expected format
            PayrollProcessingResponse response = new PayrollProcessingResponse(
                true,
                "Payroll reprocessed successfully for " + result.getEmployeeCount() + " employees",
                new PayrollProcessingData(
                    result.getEmployeeCount(),
                    result.getTotalGross().doubleValue(),
                    result.getTotalDeductions().doubleValue(),
                    result.getTotalNet().doubleValue(),
                    result.getEmployeeCount() // payslipsGenerated = employeeCount for now
                )
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            PayrollProcessingResponse errorResponse = new PayrollProcessingResponse(
                false,
                e.getMessage(),
                null
            );
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            PayrollProcessingResponse errorResponse = new PayrollProcessingResponse(
                false,
                "An unexpected error occurred while reprocessing payroll",
                null
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // ===== PAYSLIP GENERATION ENDPOINTS =====

    /**
     * Generate payslips
     */
    @PostMapping("/payslips/generate")
    public ResponseEntity<byte[]> generatePayslips(PayslipGenerationRequest request) {
        try {
            List<byte[]> pdfs = payrollService.generatePayslips(request.getFiscalPeriodId(), request.getEmployeeIds());
            if (pdfs.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            // Return the first PDF with proper headers
            // TODO: Implement ZIP download for multiple PDFs
            byte[] pdfData = pdfs.get(0);
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"payslip.pdf\"")
                .header("Content-Type", "application/pdf")
                .body(pdfData);
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

    /**
     * Generate PDF for a specific payslip
     */
    @GetMapping("/payslips/{payslipId}/pdf")
    public ResponseEntity<byte[]> generatePayslipPDF(@PathVariable Long payslipId) {
        try {
            // Get payslip data from service
            Optional<Payslip> payslipOpt = payrollService.getPayslipById(payslipId);
            if (payslipOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Payslip payslip = payslipOpt.get();

            // Get related data
            Optional<Employee> employeeOpt = payrollService.getEmployeeById(payslip.getEmployeeId());
            if (employeeOpt.isEmpty()) {
                return ResponseEntity.internalServerError().build();
            }
            Employee employee = employeeOpt.get();

            Optional<Company> companyOpt = payrollService.getCompanyById(payslip.getCompanyId());
            if (companyOpt.isEmpty()) {
                return ResponseEntity.internalServerError().build();
            }
            Company company = companyOpt.get();

            Optional<FiscalPeriod> fiscalPeriodOpt = payrollService.getFiscalPeriodById(payslip.getFiscalPeriodId());
            if (fiscalPeriodOpt.isEmpty()) {
                return ResponseEntity.internalServerError().build();
            }
            FiscalPeriod fiscalPeriod = fiscalPeriodOpt.get();

            // Generate PDF using the service
            byte[] pdfBytes = payslipPdfService.generatePayslipPdf(payslip, employee, company, fiscalPeriod);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payslip-" + payslipId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate payslip PDF for ID: " + payslipId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate bulk PDF for multiple payslips
     */
    @PostMapping("/payslips/bulk-pdf")
    public ResponseEntity<byte[]> exportBulkPDFs(@RequestBody BulkPdfRequest request) {
        try {
            // For now, return a placeholder - in real implementation, we'd generate a ZIP with all PDFs
            byte[] zipBytes = "ZIP content with multiple PDFs would go here".getBytes();

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payslips-bulk.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export all payslips for a fiscal period as ZIP
     */
    @GetMapping("/payslips/bulk-export")
    public ResponseEntity<byte[]> exportBulkPayslipsByFiscalPeriod(@RequestParam Long fiscalPeriodId) {
        try {
            LOGGER.info("Starting bulk export for fiscal period: " + fiscalPeriodId);

            // Get all payslips for the fiscal period
            List<Payslip> payslips = payrollService.getPayslipsByPeriod(fiscalPeriodId);
            LOGGER.info("Found " + payslips.size() + " payslips for fiscal period: " + fiscalPeriodId);

            if (payslips.isEmpty()) {
                LOGGER.warning("No payslips found for fiscal period: " + fiscalPeriodId);
                return ResponseEntity.notFound().build();
            }

            // Get fiscal period details for filename
            Optional<FiscalPeriod> fiscalPeriodOpt = payrollService.getFiscalPeriodById(fiscalPeriodId);
            String periodName = fiscalPeriodOpt.map(FiscalPeriod::getPeriodName).orElse("Unknown");
            LOGGER.info("Fiscal period name: " + periodName);

            // Create ZIP file containing all payslip PDFs
            ByteArrayOutputStream zipOutputStream = new ByteArrayOutputStream();
            int successfulPdfs = 0;

            try (ZipOutputStream zipOut = new ZipOutputStream(zipOutputStream)) {
                for (Payslip payslip : payslips) {
                    try {
                        LOGGER.fine("Processing payslip ID: " + payslip.getId());

                        // Get employee details for filename
                        Optional<Employee> employeeOpt = payrollService.getEmployeeById(payslip.getEmployeeId());
                        if (employeeOpt.isEmpty()) {
                            LOGGER.warning("Employee not found for payslip ID: " + payslip.getId() + ", employee ID: " + payslip.getEmployeeId());
                            continue; // Skip if employee not found
                        }
                        Employee employee = employeeOpt.get();

                        // Get company details
                        Optional<Company> companyOpt = payrollService.getCompanyById(payslip.getCompanyId());
                        if (companyOpt.isEmpty()) {
                            LOGGER.warning("Company not found for payslip ID: " + payslip.getId() + ", company ID: " + payslip.getCompanyId());
                            continue; // Skip if company not found
                        }
                        Company company = companyOpt.get();

                        // Generate PDF for this payslip
                        byte[] pdfBytes = payslipPdfService.generatePayslipPdf(payslip, employee, company,
                            fiscalPeriodOpt.orElse(null));
                        LOGGER.fine("Generated PDF for payslip ID: " + payslip.getId() + ", size: " + pdfBytes.length + " bytes");

                        // Create filename: EmployeeName_EmployeeCode_PeriodName.pdf
                        String fileName = String.format("%s_%s_%s.pdf",
                            employee.getFirstName() + "_" + employee.getLastName(),
                            employee.getEmployeeCode(),
                            periodName.replace("/", "-"));

                        // Add PDF to ZIP
                        ZipEntry zipEntry = new ZipEntry(fileName);
                        zipOut.putNextEntry(zipEntry);
                        zipOut.write(pdfBytes);
                        zipOut.closeEntry();
                        successfulPdfs++;

                        LOGGER.fine("Added PDF to ZIP: " + fileName);

                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to generate PDF for payslip ID: " + payslip.getId(), e);
                        // Continue with other payslips
                    }
                }
            }

            byte[] zipBytes = zipOutputStream.toByteArray();
            LOGGER.info("ZIP file created with " + successfulPdfs + " PDFs, total size: " + zipBytes.length + " bytes");

            if (zipBytes.length == 0) {
                LOGGER.warning("ZIP file is empty, no PDFs were successfully generated");
                return ResponseEntity.internalServerError().build();
            }

            if (zipBytes.length == 22) { // Empty ZIP file (only contains ZIP header)
                LOGGER.warning("ZIP file contains only header, no actual content");
                return ResponseEntity.internalServerError().build();
            }

            LOGGER.info("Returning ZIP file with " + successfulPdfs + " payslip PDFs");
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    String.format("attachment; filename=\"Payslips_%s.zip\"", periodName.replace("/", "-")))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid argument for bulk export: " + fiscalPeriodId, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate bulk payslip ZIP for fiscal period: " + fiscalPeriodId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Send payslips by email
     */
    @PostMapping("/payslips/send-email")
    public ResponseEntity<Void> sendPayslipsByEmail(@RequestBody EmailPayslipsRequest request) {
        try {
            // TODO: Implement email sending logic
            // EmailService emailService = new EmailService();
            // emailService.sendPayslipEmails(request.getPayslipIds());
            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== PAYROLL REPORTS ENDPOINTS =====

    /**
     * Get payroll summary report
     */
    @GetMapping("/reports/summary")
    public ResponseEntity<byte[]> getPayrollSummaryReport(
            @RequestParam Long fiscalPeriodId,
            @RequestParam(defaultValue = "PDF") String format) {
        try {
            // TODO: Implement PDF generation for summary report
            // For now, return JSON as PDF (temporary)
            SpringPayrollService.PayrollSummary summary = payrollService.getPayrollSummary(fiscalPeriodId);
            String jsonData = String.format("{\"totalGross\":%s,\"totalPAYE\":%s,\"totalUIF\":%s,\"totalSDL\":%s,\"totalNet\":%s,\"employeeCount\":%d}",
                summary.getTotalGross(), summary.getTotalPAYE(), summary.getTotalUIF(),
                summary.getTotalSDL(), summary.getTotalNet(), summary.getEmployeeCount());

            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"payroll_summary.pdf\"")
                .header("Content-Type", "application/pdf")
                .body(jsonData.getBytes());
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
        private boolean reprocess;

        public Long getFiscalPeriodId() { return fiscalPeriodId; }
        public void setFiscalPeriodId(Long fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }

        public boolean isReprocess() { return reprocess; }
        public void setReprocess(boolean reprocess) { this.reprocess = reprocess; }
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

    public static class EmailPayslipsRequest {
        private List<Long> payslipIds;

        public List<Long> getPayslipIds() { return payslipIds; }
        public void setPayslipIds(List<Long> payslipIds) { this.payslipIds = payslipIds; }
    }

    public static class BulkPdfRequest {
        private List<Long> payslipIds;

        public List<Long> getPayslipIds() { return payslipIds; }
        public void setPayslipIds(List<Long> payslipIds) { this.payslipIds = payslipIds; }
    }

    /**
     * Response class matching frontend PayrollProcessingResult interface
     */
    public static class PayrollProcessingResponse {
        private final boolean success;
        private final String message;
        private final PayrollProcessingData data;

        public PayrollProcessingResponse(boolean success, String message, PayrollProcessingData data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public PayrollProcessingData getData() { return data; }
    }

    /**
     * Data class matching frontend expectations
     */
    public static class PayrollProcessingData {
        private final int processedEmployees;
        private final double totalGrossPay;
        private final double totalDeductions;
        private final double totalNetPay;
        private final int payslipsGenerated;

        public PayrollProcessingData(int processedEmployees, double totalGrossPay,
                                   double totalDeductions, double totalNetPay, int payslipsGenerated) {
            this.processedEmployees = processedEmployees;
            this.totalGrossPay = totalGrossPay;
            this.totalDeductions = totalDeductions;
            this.totalNetPay = totalNetPay;
            this.payslipsGenerated = payslipsGenerated;
        }

        public int getProcessedEmployees() { return processedEmployees; }
        public double getTotalGrossPay() { return totalGrossPay; }
        public double getTotalDeductions() { return totalDeductions; }
        public double getTotalNetPay() { return totalNetPay; }
        public int getPayslipsGenerated() { return payslipsGenerated; }
    }
}