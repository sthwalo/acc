# Payroll PDF Reports - Corruption Issue

## Issue Summary
Payroll PDF reports generated from all three report endpoints are corrupt and unreadable. The API responses appear to be working and returning data, but the generated PDFs cannot be opened or viewed.

**Date Reported**: 26 November 2025
**Environment**: Spring Boot backend + React frontend, Docker containers
**Status**: INVESTIGATING
**Affected Endpoints**:
- `GET /api/v1/payroll/reports/summary?fiscalPeriodId={id}&format=PDF`
- `GET /api/v1/payroll/reports/employee?employeeId={id}&fiscalPeriodId={id}&format=PDF`
- `GET /api/v1/payroll/reports/emp201?fiscalPeriodId={id}&format=PDF`

## Test Data
**Test Parameters**:
- `employeeId=48`
- `fiscalPeriodId=15`
- `companyId=11`
- `size=100`

## Symptoms
- API endpoints return HTTP 200 status
- Response headers indicate PDF content type
- Frontend receives response but PDF viewer shows corruption error
- Downloaded PDF files are unreadable/invalid format
- File size appears normal but content is corrupted

## Root Cause Analysis

## Root Cause Analysis

### API Response Analysis
- **HTTP Status**: 406 Not Acceptable (when requesting PDF content type)
- **Issue**: Endpoints return JSON but claim to be PDFs
- **Content-Type Mismatch**: Server sends JSON but headers claim PDF

### PDF Generation Process
**Current Implementation Status**:
- **Summary Report**: ❌ Returns JSON data as "PDF" (Content-Type: application/pdf but body is JSON bytes)
- **Employee Report**: ❌ Returns JSON List<Payslip> (no PDF generation implemented)
- **EMP 201 Report**: ❌ Returns JSON String (no PDF generation implemented)

**Code Evidence**:
```java
// Summary Report - WRONG: Returns JSON as PDF
String jsonData = "{\"totalGross\":...}";
return ResponseEntity.ok()
    .header("Content-Type", "application/pdf")  // Claims PDF
    .body(jsonData.getBytes());                 // But sends JSON

// Employee Report - WRONG: Returns JSON with TODO
return ResponseEntity.ok(payslips); // JSON, not PDF
// TODO: Implement PDF/Excel format generation

// EMP 201 Report - WRONG: Returns JSON with TODO  
return ResponseEntity.ok(report); // JSON, not PDF
// TODO: Implement PDF format generation
```

### Frontend PDF Handling
**Current Status**: Frontend expects PDF binary data but receives JSON/text
**Impact**: JSON data displayed as "corrupt PDF" in PDF viewers

## Testing Results

### Backend Testing Results
```bash
# Summary Report - Returns JSON, not PDF
curl "http://localhost:8080/api/v1/payroll/reports/summary?fiscalPeriodId=15&format=PDF"
# Response: {"totalGross":100000.00,"totalPAYE":22588.00,...}  # JSON!

# Employee Report - Returns JSON, not PDF  
curl "http://localhost:8080/api/v1/payroll/reports/employee?employeeId=48&fiscalPeriodId=15&format=PDF"
# Response: [{"id":1318,"companyId":11,...}]  # JSON array!

# EMP 201 Report - Returns text, not PDF
curl "http://localhost:8080/api/v1/payroll/reports/emp201?fiscalPeriodId=15&format=PDF"  
# Response: "EMP201 SARS Tax Submission Report..."  # Plain text!

# PDF Content-Type Request - Rejected
curl -H "Accept: application/pdf" "..."
# Response: HTTP 406 Not Acceptable  # Server rejects PDF requests
```

### Data Validation Results
```bash
# Test data exists and is valid
curl "http://localhost:8080/api/v1/payroll/employees?companyId=11&size=100" | jq '.data | length'
# Response: 2  # ✓ Employees exist

curl "http://localhost:8080/api/v1/companies/11/fiscal-periods" | jq '.data[] | select(.id == 15)'
# Response: {...}  # ✓ Fiscal period exists

curl "http://localhost:8080/api/v1/payroll/employees/48" | jq '.data.id'
# Response: 48  # ✓ Employee exists
```

## Root Cause - IMPLEMENTATION MISSING

**Critical Finding**: **PDF generation is not implemented** in any of the report endpoints.

### Current Implementation Status
- ✅ **Data Retrieval**: All endpoints successfully query and return payroll data
- ✅ **API Responses**: Endpoints return HTTP 200 with correct data structure
- ❌ **PDF Generation**: No PDF generation logic implemented (all marked with TODO)
- ❌ **Format Parameter**: `format=PDF` parameter accepted but ignored

### Why "Corrupt PDFs"
1. **Summary Report**: Returns JSON bytes with PDF headers → JSON text file
2. **Employee Report**: Returns JSON array → JSON text file  
3. **EMP 201 Report**: Returns plain text → Text file
4. **Frontend**: Receives text data, tries to display as PDF → "Corruption"

### PDF Generation Process
**Report Generation Flow**:
1. Controller receives request with parameters
2. Service layer queries database for payroll data
3. PDF generation service creates PDF using template/data
4. Response is streamed back to client

**Potential Issues**:
- PDF generation library configuration
- Template/data corruption
- Stream handling issues
- Character encoding problems
- Memory/buffer issues in PDF creation

### Frontend PDF Handling
**Frontend Processing**:
1. Axios request with responseType: 'blob' or 'arraybuffer'
2. Response handling and blob creation
3. PDF viewer or download initiation

**Potential Issues**:
- Incorrect response type handling
- Blob creation problems
- PDF viewer compatibility
- Download vs display logic

## Testing Plan

### Backend Testing
```bash
# Test payroll summary report
curl -H "Accept: application/pdf" \
     "http://localhost:8080/api/v1/payroll/reports/summary?fiscalPeriodId=15&format=PDF" \
     --output test_summary.pdf

# Test employee report
curl -H "Accept: application/pdf" \
     "http://localhost:8080/api/v1/payroll/reports/employee?employeeId=48&fiscalPeriodId=15&format=PDF" \
     --output test_employee.pdf

# Test EMP 201 report
curl -H "Accept: application/pdf" \
     "http://localhost:8080/api/v1/payroll/reports/emp201?fiscalPeriodId=15&format=PDF" \
     --output test_emp201.pdf

# Verify PDF validity
file test_*.pdf
pdfinfo test_*.pdf || echo "PDF validation failed"
```

### Frontend Testing
```javascript
// Test PDF download from browser
const response = await fetch('/api/v1/payroll/reports/employee?employeeId=48&fiscalPeriodId=15&format=PDF');
const blob = await response.blob();
const url = window.URL.createObjectURL(blob);
// Attempt to open in new tab or download
```

### Data Validation
```bash
# Verify test data exists
curl "http://localhost:8080/api/v1/payroll/employees?companyId=11&size=100" | jq '.data | length'

# Check fiscal period exists
curl "http://localhost:8080/api/v1/companies/11/fiscal-periods" | jq '.data[] | select(.id == 15)'

# Verify employee exists
curl "http://localhost:8080/api/v1/payroll/employees/48" | jq '.data'
```

## Debugging Steps

### 1. Backend PDF Generation
- **Check PDF Service**: Verify PDFBox or iText configuration
- **Template Validation**: Ensure report templates are valid
- **Data Formatting**: Check data passed to PDF generator
- **Stream Handling**: Verify output stream is properly closed

### 2. Response Headers
- **Content-Type**: Must be `application/pdf`
- **Content-Disposition**: Should include filename
- **Content-Length**: Should match actual PDF size
- **Cache-Control**: Appropriate caching headers

### 3. Frontend Blob Handling
- **Response Type**: Ensure correct axios configuration
- **Blob Creation**: Verify blob type is 'application/pdf'
- **URL Creation**: Check object URL generation
- **Download Logic**: Test both download and display methods

### 4. PDF Content Validation
- **File Structure**: Verify PDF header (%PDF-1.x)
- **EOF Marker**: Check for proper %%EOF
- **Compression**: Validate stream compression
- **Font Embedding**: Ensure fonts are properly embedded

## Potential Fixes

### Required Implementation
**PDF Generation Needs to Be Implemented** - This is not a bug, it's missing functionality.

#### Backend Fixes Required
1. **Implement PDF Generation Logic** for all three report endpoints
2. **Use PDFBox Library** (already available in project)
3. **Return Proper PDF Binary Data** with correct headers
4. **Handle Format Parameter** (PDF vs Excel vs JSON)

#### Implementation Approach
```java
// Example: Proper PDF generation implementation
@GetMapping("/reports/summary")
public ResponseEntity<byte[]> getPayrollSummaryReport(
        @RequestParam Long fiscalPeriodId,
        @RequestParam(defaultValue = "PDF") String format) {
    
    SpringPayrollService.PayrollSummary summary = payrollService.getPayrollSummary(fiscalPeriodId);
    
    if ("PDF".equalsIgnoreCase(format)) {
        // Generate actual PDF using PDFBox
        byte[] pdfData = payrollReportService.generateSummaryPdf(summary);
        return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "attachment; filename=\"payroll_summary.pdf\"")
            .body(pdfData);
    } else {
        // Return JSON for API consumers
        return ResponseEntity.ok()
            .header("Content-Type", "application/json")
            .body(summary);
    }
}
```

#### Frontend Compatibility
- **Current Frontend**: Expects PDF binary data - will work once PDFs are generated
- **No Frontend Changes Required**: Issue is entirely backend implementation missing

## Files to Investigate
- `spring-app/src/main/java/fin/controller/spring/SpringPayrollController.java` - Report endpoints (lines 629-675)
- `spring-app/src/main/java/fin/service/spring/SpringPayrollService.java` - Service methods with TODO comments
- PDF generation libraries (PDFBox already available)
- Report templates (may need to be created)

## Next Steps
1. **Implement PDF Generation** for all three report endpoints
2. **Test PDF Output** with real data (employeeId=48, fiscalPeriodId=15)
3. **Verify Frontend Integration** works with actual PDFs
4. **Update Documentation** with implementation details

## Related Issues
- PDF generation implemented for payslips but missing for reports
- Consistent pattern across all report endpoints (all have TODO comments)

## Status
**ROOT CAUSE IDENTIFIED** - PDF generation not implemented, endpoints return JSON/text instead of PDFs.

## Resolution Date
TBD - After PDF generation implementation