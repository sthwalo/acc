package fin.controller;

import fin.entity.Company;
import fin.entity.Employee;
import fin.entity.FiscalPeriod;
import fin.entity.Payslip;
import fin.service.PayslipPdfService;
import fin.service.PayrollService;
import fin.service.reporting.PayrollReportService;
import fin.dto.BulkPdfRequest;
import fin.dto.DocumentUploadRequest;
import fin.dto.EmailPayslipsRequest;
import fin.dto.EmployeeCreateRequest;
import fin.dto.EmployeeUpdateRequest;
import fin.dto.ErrorResponse;
import fin.dto.PayrollProcessRequest;
import fin.dto.PayrollProcessingData;
import fin.dto.PayrollProcessingResponse;
import fin.dto.PayslipGenerationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
public class PayrollController {

    private final PayrollService payrollService;
    private final PayslipPdfService payslipPdfService;
    private final PayrollReportService payrollReportService;
    private static final Logger LOGGER = Logger.getLogger(PayrollController.class.getName());

    public PayrollController(PayrollService payrollService, PayslipPdfService payslipPdfService, PayrollReportService payrollReportService) {
        this.payrollService = payrollService;
        this.payslipPdfService = payslipPdfService;
        this.payrollReportService = payrollReportService;
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
    public ResponseEntity<?> createEmployee(@RequestBody EmployeeCreateRequest request) {
        try {
            Employee createdEmployee = payrollService.createEmployee(
                request.getEmployeeCode(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhone(),
                request.getBankName(),
                request.getAccountNumber(),
                request.getBranchCode(),
                request.getTaxNumber(),
                request.getHireDate(),
                request.getBasicSalary(),
                request.getCompanyId()
            );
            return ResponseEntity.ok(createdEmployee);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
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
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody EmployeeUpdateRequest request) {
        try {
            Employee updatedEmployee = payrollService.updateEmployee(
                id,
                request.getTitle(),
                request.getFirstName(),
                request.getSecondName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhone(),
                request.getPosition(),
                request.getDepartment(),
                request.getHireDate(),
                request.getTerminationDate(),
                request.getIsActive(),
                request.getAddressLine1(),
                request.getAddressLine2(),
                request.getCity(),
                request.getProvince(),
                request.getPostalCode(),
                request.getCountry(),
                request.getBankName(),
                request.getAccountHolderName(),
                request.getAccountNumber(),
                request.getBranchCode(),
                request.getAccountType(),
                request.getEmploymentType(),
                request.getSalaryType(),
                request.getBasicSalary(),
                request.getOvertimeRate(),
                request.getTaxNumber(),
                request.getTaxRebateCode(),
                request.getUifNumber(),
                request.getMedicalAidNumber(),
                request.getPensionFundNumber(),
                null // updatedBy - could be extracted from security context in future
            );
            return ResponseEntity.ok(updatedEmployee);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Delete (deactivate) an employee - soft delete preserving payroll history
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

    /**
     * Activate an employee
     */
    @PutMapping("/employees/{id}/activate")
    public ResponseEntity<Void> activateEmployee(@PathVariable Long id) {
        try {
            payrollService.activateEmployee(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Change employee active status
     */
    @PutMapping("/employees/{id}/status")
    public ResponseEntity<Void> changeEmployeeStatus(@PathVariable Long id, @RequestParam boolean active) {
        try {
            payrollService.changeEmployeeStatus(id, active);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Hard delete an employee - permanently remove from database
     * Only allowed if employee has no payroll history
     */
    @DeleteMapping("/employees/{id}/hard")
    public ResponseEntity<String> hardDeleteEmployee(@PathVariable Long id) {
        try {
            payrollService.hardDeleteEmployee(id);
            return ResponseEntity.ok("Employee permanently deleted");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
     * @param request the payroll process request containing fiscal period ID and reprocess flag
     * @return the payroll processing response
     */
    @PostMapping("/process")
    public ResponseEntity<PayrollProcessingResponse> processPayroll(@RequestBody PayrollProcessRequest request) {
        try {
            PayrollService.PayrollProcessingResult result;

            if (request.isReprocess()) {
                result = payrollService.reprocessPayroll(request.getFiscalPeriodId());
            } else {
                result = payrollService.processPayroll(request.getFiscalPeriodId());
            }

            // Convert to frontend-expected format
            PayrollProcessingResponse response = new PayrollProcessingResponse(
                true,
                "Payroll " + (request.isReprocess() ? "re" : "") + "processed successfully for " + result.getEmployeeCount() + " employees",
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
     * @param request the payroll process request containing fiscal period ID (reprocess flag ignored, always true)
     * @return the payroll reprocessing response
     */
    @PostMapping("/reprocess")
    public ResponseEntity<PayrollProcessingResponse> reprocessPayroll(@RequestBody PayrollProcessRequest request) {
        try {
            PayrollService.PayrollProcessingResult result = payrollService.reprocessPayroll(request.getFiscalPeriodId());

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
    public ResponseEntity<byte[]> generatePayslips(@RequestBody PayslipGenerationRequest request) {
        LOGGER.info("DEBUG: generatePayslips method called with request: " + request);
        if (request == null) {
            LOGGER.info("DEBUG: Request is null");
            return ResponseEntity.badRequest().build();
        }
        LOGGER.info("DEBUG: fiscalPeriodId = " + request.getFiscalPeriodId());
        LOGGER.info("DEBUG: employeeIds = " + request.getEmployeeIds());

        LOGGER.info("Received payslip generation request: fiscalPeriodId=" + request.getFiscalPeriodId() +
                   ", employeeIds=" + (request.getEmployeeIds() != null ? request.getEmployeeIds().size() : 0) + " employees");

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
            LOGGER.log(Level.WARNING, "Invalid request for payslip generation: " + e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during payslip generation: " + e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
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
            if (fiscalPeriodOpt.isEmpty()) {
                LOGGER.warning("Fiscal period not found: " + fiscalPeriodId);
                return ResponseEntity.notFound().build();
            }
            FiscalPeriod fiscalPeriod = fiscalPeriodOpt.get();
            String periodName = fiscalPeriod.getPeriodName();
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
                        byte[] pdfBytes = payslipPdfService.generatePayslipPdf(payslip, employee, company, fiscalPeriod);
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
            if ("PDF".equalsIgnoreCase(format)) {
                PayrollService.PayrollSummary summary = payrollService.getPayrollSummary(fiscalPeriodId);
                byte[] pdfData = payrollReportService.generatePayrollSummaryPdf(summary);
                return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"payroll_summary.pdf\"")
                    .body(pdfData);
            } else {
                // Return JSON for API consumers
                PayrollService.PayrollSummary summary = payrollService.getPayrollSummary(fiscalPeriodId);
                return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(summary.toString().getBytes());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get employee payroll report
     */
    @GetMapping("/reports/employee")
    public ResponseEntity<byte[]> getEmployeePayrollReport(
            @RequestParam Long employeeId,
            @RequestParam Long fiscalPeriodId,
            @RequestParam(defaultValue = "PDF") String format) {
        try {
            if ("PDF".equalsIgnoreCase(format)) {
                List<Payslip> payslips = payrollService.getPayslipsByEmployee(employeeId);
                // Filter by fiscal period
                payslips = payslips.stream()
                    .filter(p -> p.getFiscalPeriodId().equals(fiscalPeriodId))
                    .toList();
                byte[] pdfData = payrollReportService.generateEmployeePayrollPdf(payslips);
                return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"employee_payroll.pdf\"")
                    .body(pdfData);
            } else {
                // Return JSON for API consumers
                List<Payslip> payslips = payrollService.getPayslipsByEmployee(employeeId);
                // Filter by fiscal period
                payslips = payslips.stream()
                    .filter(p -> p.getFiscalPeriodId().equals(fiscalPeriodId))
                    .toList();
                return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(payslips.toString().getBytes());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get EMP 201 SARS tax submission report
     */
    @GetMapping("/reports/emp201")
    public ResponseEntity<byte[]> getEmp201Report(
            @RequestParam Long fiscalPeriodId,
            @RequestParam(defaultValue = "PDF") String format) {
        try {
            if ("PDF".equalsIgnoreCase(format)) {
                String emp201Data = payrollService.generateEmp201Report(fiscalPeriodId);
                byte[] pdfData = payrollReportService.generateEmp201Pdf(emp201Data);
                return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"emp201_report.pdf\"")
                    .body(pdfData);
            } else {
                // Return JSON for API consumers
                String emp201Data = payrollService.generateEmp201Report(fiscalPeriodId);
                return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(emp201Data.getBytes());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ===== DOCUMENT MANAGEMENT ENDPOINTS =====

    /**
     * Upload payroll document
     */
    @PostMapping("/documents")
    public ResponseEntity<Void> uploadPayrollDocument(
            @RequestParam Long employeeId,
            @RequestParam Long fiscalPeriodId,
            @RequestParam String fileName,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String documentType) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            byte[] fileData = file.getBytes();
            payrollService.uploadPayrollDocument(employeeId, fiscalPeriodId, fileName, fileData, documentType);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * List payroll documents
     */
    @GetMapping("/documents")
    public ResponseEntity<List<PayrollService.PayrollDocument>> listPayrollDocuments(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long fiscalPeriodId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<PayrollService.PayrollDocument> documents = payrollService.listPayrollDocuments(employeeId, fiscalPeriodId, type, page, size);
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
            PayrollService.PayrollDocument document = payrollService.getPayrollDocument(id);
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

    // Moved to fin.dto package for better organization
}
