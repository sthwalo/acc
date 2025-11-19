# TASK 7.1: Business Document Management System Enhancement

## Overview
Enhance the FIN system with comprehensive business document management capabilities including enhanced invoices with multi-line descriptions and recipient details, plus new Quotation, Purchase Order, and Receipt systems.

## Executive Summary
This enhancement adds professional business document management to FIN, expanding beyond basic invoicing to include quotations, purchase orders, and receipts. The system will support multi-line descriptions, recipient details, and maintain company isolation across all document types.

## Business Value
- **Professional Documentation**: Generate industry-standard business documents
- **Enhanced Invoice Management**: Support complex invoices with detailed descriptions
- **Complete Transaction Lifecycle**: Track quotes → orders → invoices → receipts
- **Regulatory Compliance**: Maintain proper business documentation standards
- **Operational Efficiency**: Streamlined document generation and management

## Success Criteria
- ✅ Enhanced invoices support multi-line descriptions and recipient details
- ✅ Quotation system with quote-to-invoice conversion
- ✅ Purchase Order system with vendor management
- ✅ Receipt system for payment confirmations
- ✅ All documents maintain company isolation
- ✅ PDF generation for all document types
- ✅ Database schema supports all document relationships
- ✅ API endpoints for document management
- ✅ Console interface for document operations

## Implementation Phases

### Phase 1: Enhanced Invoice System (Priority: High)
**Objective**: Upgrade existing invoice system with multi-line descriptions and recipient details

**Database Schema Updates**:
```sql
-- Add recipient details to manual_invoices
ALTER TABLE manual_invoices ADD COLUMN recipient_name VARCHAR(255);
ALTER TABLE manual_invoices ADD COLUMN recipient_address TEXT;
ALTER TABLE manual_invoices ADD COLUMN recipient_email VARCHAR(255);
ALTER TABLE manual_invoices ADD COLUMN recipient_phone VARCHAR(50);

-- Add multi-line description support
ALTER TABLE manual_invoices ADD COLUMN description_lines JSONB; -- Array of description objects
ALTER TABLE manual_invoices ADD COLUMN notes TEXT; -- Additional invoice notes

-- Add invoice status tracking
ALTER TABLE manual_invoices ADD COLUMN status VARCHAR(50) DEFAULT 'draft'; -- draft, sent, paid, overdue
ALTER TABLE manual_invoices ADD COLUMN due_date DATE;
ALTER TABLE manual_invoices ADD COLUMN payment_terms VARCHAR(255);
```

**Service Layer Updates**:
- Update `DataManagementService.createManualInvoice()` to accept recipient details and multi-line descriptions
- Update `InvoicePdfService` to render enhanced invoice layout with recipient information
- Add invoice status management methods

**API Endpoints**:
- `POST /api/v1/companies/{id}/invoices` - Create enhanced invoice
- `GET /api/v1/companies/{id}/invoices/{number}` - Get invoice details
- `PUT /api/v1/companies/{id}/invoices/{number}/status` - Update invoice status

### Phase 2: Quotation System (Priority: High)
**Objective**: Implement quotation management with quote-to-invoice conversion

**Database Schema**:
```sql
CREATE TABLE quotations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    quote_number VARCHAR(100) NOT NULL,
    recipient_name VARCHAR(255),
    recipient_address TEXT,
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(50),
    description_lines JSONB,
    total_amount DECIMAL(15,2),
    valid_until DATE,
    status VARCHAR(50) DEFAULT 'draft', -- draft, sent, accepted, rejected, expired
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, quote_number)
);

-- Link quotes to invoices for conversion tracking
ALTER TABLE manual_invoices ADD COLUMN quotation_id BIGINT REFERENCES quotations(id);
```

**Service Implementation**:
- `QuotationService` - CRUD operations for quotations
- `QuotationPdfService` - PDF generation for quotes
- Quote-to-invoice conversion functionality

### Phase 3: Purchase Order System (Priority: Medium)
**Objective**: Add purchase order management for vendor transactions

**Database Schema**:
```sql
CREATE TABLE purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    po_number VARCHAR(100) NOT NULL,
    vendor_name VARCHAR(255),
    vendor_address TEXT,
    vendor_email VARCHAR(255),
    vendor_phone VARCHAR(50),
    description_lines JSONB,
    total_amount DECIMAL(15,2),
    delivery_date DATE,
    status VARCHAR(50) DEFAULT 'draft', -- draft, sent, received, cancelled
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, po_number)
);

-- Link POs to invoices for three-way matching
ALTER TABLE manual_invoices ADD COLUMN purchase_order_id BIGINT REFERENCES purchase_orders(id);
```

### Phase 4: Receipt System (Priority: Medium)
**Objective**: Implement payment receipt generation and management

**Database Schema**:
```sql
CREATE TABLE receipts (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    receipt_number VARCHAR(100) NOT NULL,
    invoice_id BIGINT REFERENCES manual_invoices(id),
    recipient_name VARCHAR(255),
    recipient_address TEXT,
    payment_amount DECIMAL(15,2),
    payment_method VARCHAR(100), -- cash, bank_transfer, cheque, etc.
    payment_reference VARCHAR(255),
    payment_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, receipt_number)
);
```

## Technical Architecture

### Service Layer Design
```
BusinessDocumentService (base)
├── InvoiceService (extends BusinessDocumentService)
├── QuotationService (extends BusinessDocumentService)
├── PurchaseOrderService (extends BusinessDocumentService)
└── ReceiptService (extends BusinessDocumentService)
```

### PDF Generation Architecture
```
DocumentPdfService (base)
├── InvoicePdfService (existing, enhanced)
├── QuotationPdfService (new)
├── PurchaseOrderPdfService (new)
└── ReceiptPdfService (new)
```

### Repository Pattern
- `BusinessDocumentRepository` - Base repository with common operations
- Specific repositories for each document type
- Company isolation enforced at repository level

## API Design

### REST Endpoints Structure
```
/api/v1/companies/{companyId}/
├── invoices/           # Invoice management
├── quotations/         # Quotation management
├── purchase-orders/    # Purchase order management
└── receipts/          # Receipt management
```

### Common API Patterns
- POST `/documents` - Create new document
- GET `/documents` - List documents with filtering
- GET `/documents/{number}` - Get specific document
- PUT `/documents/{number}` - Update document
- DELETE `/documents/{number}` - Delete document (if allowed)
- POST `/documents/{number}/pdf` - Generate PDF

## Console Interface Updates

### Data Management Menu Enhancement
```
===== Data Management =====
1. Create Manual Invoice          (enhanced)
2. Generate Invoice PDF           (enhanced)
3. Create Quotation
4. Generate Quotation PDF
5. Create Purchase Order
6. Generate Purchase Order PDF
7. Create Receipt
8. Generate Receipt PDF
9. Convert Quote to Invoice
10. View Document History
...existing options...
```

## Testing Strategy

### Unit Tests
- Service layer tests for all document operations
- PDF generation tests for layout accuracy
- Repository tests for data integrity

### Integration Tests
- End-to-end document lifecycle testing
- API endpoint testing with real database
- PDF generation validation

### Manual Testing
- Console interface testing for all operations
- PDF output validation for professional appearance
- Company isolation verification

## Risk Assessment

### Technical Risks
- **PDF Layout Complexity**: High - Requires precise positioning for professional documents
- **Database Schema Changes**: Medium - Need careful migration planning
- **API Consistency**: Low - Following established patterns

### Business Risks
- **Document Standards**: Medium - Must meet South African business document requirements
- **Performance Impact**: Low - Additional tables with proper indexing
- **User Training**: Low - Building on existing invoice patterns

### Mitigation Strategies
- **PDF Layout**: Use existing InvoicePdfService patterns, extensive testing
- **Database**: Comprehensive backup before schema changes, rollback scripts
- **Standards**: Research SARS and business document requirements
- **Performance**: Implement proper indexing, monitor query performance

## Dependencies
- Apache PDFBox 3.0.0 (existing)
- PostgreSQL JSONB support (existing)
- Existing company isolation framework
- Current PDF generation infrastructure

## Success Metrics
- All document types generate professional PDFs
- Multi-line descriptions render correctly
- Company isolation maintained across all documents
- API endpoints respond correctly
- Console operations work seamlessly
- Database queries perform efficiently

## Rollback Plan
1. Database: Restore from backup if schema changes fail
2. Code: Git revert to previous commit
3. Services: Revert ApplicationContext registrations
4. API: Remove new endpoints (backward compatible)

## Timeline Estimate
- Phase 1 (Enhanced Invoices): 2-3 days
- Phase 2 (Quotations): 2-3 days
- Phase 3 (Purchase Orders): 2 days
- Phase 4 (Receipts): 1-2 days
- Testing & Documentation: 2 days

## Next Steps
1. Confirm requirements and priority order
2. Begin with Phase 1: Enhanced Invoice System
3. Implement database schema updates
4. Update service layer and PDF generation
5. Test thoroughly before proceeding to next phase

---

**Status**: Ready for Implementation
**Priority**: High
**Estimated Effort**: 2 weeks
**Risk Level**: Medium
**Dependencies**: None (builds on existing infrastructure)