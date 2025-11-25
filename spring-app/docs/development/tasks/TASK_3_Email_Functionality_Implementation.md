# TASK 3: Email Functionality Implementation
**Status:** 🔄 ACTIVE DEVELOPMENT - Implementation Phase
**Created:** 2025-11-24
**Priority:** HIGH - Core Business Feature
**Risk Level:** HIGH - Email Security & Compliance
**Estimated Effort:** 5-7 days (40-56 hours)

## 🎯 Task Overview

Implement comprehensive email functionality for payslip distribution with multi-company support, security, compliance, and scalability considerations.

## 📋 Requirements

### Core Functionality
- [ ] Send individual payslip emails with PDF attachments
- [ ] Bulk payslip email distribution
- [ ] On-demand PDF generation for emails
- [ ] Email status tracking and audit logging

### Security & Compliance
- [ ] SMTP credentials encryption in database
- [ ] Rate limiting (max emails per hour/day per company)
- [ ] POPIA compliance (consent, unsubscribe links)
- [ ] Secure credential management
- [ ] Audit logging of all sent emails

### Multi-Company Architecture
- [ ] Global Limelight defaults for current implementation
- [ ] Database-driven email templates per company
- [ ] Company branding support (logos, colors, signatures)
- [ ] Future-ready for company-specific SMTP overrides

### Technical Implementation
- [ ] Update SpringPayrollController to use EmailService
- [ ] Implement email queue for bulk operations
- [ ] Add email configuration tables
- [ ] Create email template management
- [ ] Add rate limiting and security measures

## 🏗️ Implementation Architecture

### Database Schema Changes

#### 1. Email Templates Table
```sql
CREATE TABLE email_templates (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    template_type VARCHAR(50) NOT NULL, -- 'PAYSLIP', 'WELCOME', etc.
    language VARCHAR(10) DEFAULT 'en', -- 'en', 'af'
    subject_template TEXT NOT NULL,
    body_template TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 2. Email Configurations Table
```sql
CREATE TABLE email_configurations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id), -- NULL for global defaults
    smtp_host VARCHAR(255) NOT NULL,
    smtp_port INTEGER NOT NULL,
    smtp_username VARCHAR(255),
    smtp_password_encrypted TEXT, -- Encrypted
    smtp_auth BOOLEAN DEFAULT TRUE,
    smtp_tls BOOLEAN DEFAULT TRUE,
    smtp_ssl BOOLEAN DEFAULT FALSE,
    from_email VARCHAR(255) NOT NULL,
    from_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(255),
    rate_limit_hourly INTEGER DEFAULT 100,
    rate_limit_daily INTEGER DEFAULT 1000,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 3. Email Audit Log Table
```sql
CREATE TABLE email_audit_log (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    employee_id BIGINT REFERENCES employees(id),
    payslip_id BIGINT REFERENCES payslips(id),
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL, -- 'SENT', 'FAILED', 'QUEUED'
    error_message TEXT,
    sent_at TIMESTAMP,
    smtp_response TEXT,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 4. Email Queue Table
```sql
CREATE TABLE email_queue (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    batch_id VARCHAR(100), -- For bulk operations
    priority INTEGER DEFAULT 1, -- 1=normal, 2=high, 3=urgent
    recipient_email VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    attachment_path TEXT, -- Path to PDF file
    attachment_filename VARCHAR(255),
    status VARCHAR(50) DEFAULT 'QUEUED', -- 'QUEUED', 'PROCESSING', 'SENT', 'FAILED'
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    next_retry_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Service Layer Changes

#### EmailTemplateService
- Load templates by company and type
- Template variable substitution
- Multi-language support

#### EmailConfigurationService
- Load SMTP config by company (with fallback to global)
- Decrypt credentials securely
- Rate limiting enforcement

#### EmailAuditService
- Log all email operations
- Track delivery status
- Generate compliance reports

#### Enhanced EmailService
- Async email sending
- Queue integration
- Rate limiting
- POPIA compliance features

### Controller Updates

#### SpringPayrollController
- Inject EmailService and related services
- Update `/payslips/send-email` endpoint
- Add email status endpoints
- Implement proper error handling

## 🔒 Security Implementation

### Credential Encryption
- Use AES-256 encryption for SMTP passwords
- Store encryption key securely (environment variable)
- Implement proper key rotation strategy

### Rate Limiting
- Per-company hourly and daily limits
- Redis-based counters (if available)
- Database fallback for rate limiting
- Clear error messages when limits exceeded

### POPIA Compliance
- Consent verification before sending emails
- Unsubscribe links in all emails
- Data minimization in email content
- Audit trail for all email operations

## 📊 Implementation Phases

### Phase 1: Database Schema (Day 1)
- [ ] Create email_templates table
- [ ] Create email_configurations table
- [ ] Create email_audit_log table
- [ ] Create email_queue table
- [ ] Add Flyway migration scripts

### Phase 2: Core Services (Days 2-3)
- [ ] Implement EmailTemplateService
- [ ] Implement EmailConfigurationService
- [ ] Implement EmailAuditService
- [ ] Enhance EmailService with async support

### Phase 3: Controller Integration (Day 4)
- [ ] Update SpringPayrollController
- [ ] Implement `/payslips/send-email` endpoint
- [ ] Add email status endpoints
- [ ] Implement error handling

### Phase 4: Security & Compliance (Days 5-6)
- [ ] Implement credential encryption
- [ ] Add rate limiting
- [ ] Add POPIA compliance features
- [ ] Implement audit logging

### Phase 5: Testing & Validation (Day 7)
- [ ] Unit tests for all services
- [ ] Integration tests for email sending
- [ ] Security testing
- [ ] Performance testing

## 🧪 Testing Strategy

### Unit Tests
- EmailService functionality
- Template processing
- Configuration loading
- Rate limiting logic

### Integration Tests
- End-to-end email sending
- Queue processing
- Database operations
- Error scenarios

### Security Tests
- Credential encryption/decryption
- Rate limiting enforcement
- POPIA compliance validation

## 📈 Success Criteria

- [ ] Emails sent successfully with PDF attachments
- [ ] Rate limiting prevents abuse
- [ ] All emails logged for compliance
- [ ] Company-specific branding supported
- [ ] POPIA compliance features implemented
- [ ] Secure credential management
- [ ] Comprehensive error handling
- [ ] Full test coverage

## 🚨 Risk Mitigation

### High Risk Items
- **SMTP Credential Security:** Implement encryption and secure key management
- **Rate Limiting:** Prevent email abuse while allowing legitimate bulk operations
- **POPIA Compliance:** Ensure all emails include required consent and unsubscribe features
- **Email Delivery:** Handle SMTP failures gracefully with retry mechanisms

### Contingency Plans
- **SMTP Failure:** Implement multiple SMTP provider support
- **Rate Limiting:** Database-based fallback if Redis unavailable
- **Compliance Issues:** Template validation and audit trails

## 📋 Dependencies

- **Database:** PostgreSQL 17+ with Flyway migrations
- **Security:** AES encryption for credentials
- **Async Processing:** Spring @Async for email queue
- **Caching:** Redis for rate limiting (optional)
- **Email Templates:** Thymeleaf or FreeMarker for template processing

## 🔗 Related Tasks

- **TASK 0:** Database-First Architecture (ensure email templates don't use fallbacks)
- **TASK 1:** Data Management APIs (email configuration management)
- **TASK 2:** Payroll APIs (integration with payslip generation)
- **TASK INTEGRATION:** Dashboard (email status UI components)

## 📚 References

- [POPIA Compliance Guidelines](https://popia.co.za/)
- [SMTP Security Best Practices](https://tools.ietf.org/html/rfc3207)
- [Email Template Standards](https://www.emailonacid.com/blog/article/email-development)
- [Spring Boot Email Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.email)