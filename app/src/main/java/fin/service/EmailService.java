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

package fin.service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for sending emails with payslip attachments
 */
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    private final String smtpHost;
    private final String smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final boolean smtpAuth;
    private final boolean smtpTls;
    private final boolean smtpSsl;
    private final String fromEmail;
    private final String fromName;

    /**
     * Constructor with SMTP configuration
     */
    public EmailService(String initialSmtpHost, String initialSmtpPort, String initialSmtpUsername,
                       String initialSmtpPassword, boolean initialSmtpAuth, boolean initialSmtpTls, boolean initialSmtpSsl,
                       String initialFromEmail, String initialFromName) {
        this.smtpHost = initialSmtpHost;
        this.smtpPort = initialSmtpPort;
        this.smtpUsername = initialSmtpUsername;
        this.smtpPassword = initialSmtpPassword;
        this.smtpAuth = initialSmtpAuth;
        this.smtpTls = initialSmtpTls;
        this.smtpSsl = initialSmtpSsl;
        this.fromEmail = initialFromEmail;
        this.fromName = initialFromName;
    }

    /**
     * Default constructor using environment variables or system properties
     */
    public EmailService() {
        this(
            System.getProperty("fin.smtp.host",
                System.getenv("SMTP_HOST") != null ? System.getenv("SMTP_HOST") : "smtp.gmail.com"),
            System.getProperty("fin.smtp.port",
                System.getenv("SMTP_PORT") != null ? System.getenv("SMTP_PORT") : "587"),
            System.getProperty("fin.smtp.username",
                System.getenv("SMTP_USERNAME") != null ? System.getenv("SMTP_USERNAME") : ""),
            System.getProperty("fin.smtp.password",
                System.getenv("SMTP_PASSWORD") != null ? System.getenv("SMTP_PASSWORD") : ""),
            Boolean.parseBoolean(System.getProperty("fin.smtp.auth",
                System.getenv("SMTP_AUTH") != null ? System.getenv("SMTP_AUTH") : "true")),
            Boolean.parseBoolean(System.getProperty("fin.smtp.tls",
                System.getenv("SMTP_TLS") != null ? System.getenv("SMTP_TLS") : "true")),
            Boolean.parseBoolean(System.getProperty("fin.smtp.ssl",
                System.getenv("SMTP_SSL") != null ? System.getenv("SMTP_SSL") : "false")),
            System.getProperty("fin.email.from",
                System.getenv("EMAIL_FROM") != null ? System.getenv("EMAIL_FROM") : "noreply@company.com"),
            System.getProperty("fin.email.fromName",
                System.getenv("EMAIL_FROM_NAME") != null ? System.getenv("EMAIL_FROM_NAME") : "Payroll System")
        );
    }

    /**
     * Sends a payslip email to an employee
     *
     * @param toEmail Employee's email address
     * @param employeeName Employee's full name
     * @param payslipPdfPath Path to the payslip PDF file
     * @param payrollPeriodName Name of the payroll period
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendPayslipEmail(String toEmail, String employeeName, String payslipPdfPath, String payrollPeriodName) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            LOGGER.warning("Cannot send email: no email address provided for employee " + employeeName);
            return false;
        }

        try {
            // Create session
            Session session = createSession();

            // Create message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail, fromName));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Your Payslip - " + payrollPeriodName);

            // Create multipart message
            Multipart multipart = new MimeMultipart();

            // Add text part
            BodyPart textPart = new MimeBodyPart();
            String emailBody = createEmailBody(employeeName, payrollPeriodName);
            textPart.setText(emailBody);
            multipart.addBodyPart(textPart);

            // Add attachment
            if (payslipPdfPath != null && !payslipPdfPath.trim().isEmpty()) {
                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(new File(payslipPdfPath));
                attachmentPart.setFileName("Payslip_" + payrollPeriodName.replaceAll("\\s+", "_") + ".pdf");
                multipart.addBodyPart(attachmentPart);
            }

            // Set content
            message.setContent(multipart);

            // Send message
            Transport.send(message);

            LOGGER.info("Payslip email sent successfully to " + toEmail + " for employee " + employeeName);
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send payslip email to " + toEmail + " for employee " + employeeName, e);
            return false;
        }
    }

    /**
     * Sends a bulk payslip email to multiple employees
     *
     * @param emailRequests List of email requests containing email, name, pdf path, and period
     * @return EmailSendResult with success/failure counts
     */
    public EmailSendResult sendBulkPayslipEmails(java.util.List<EmailRequest> emailRequests) {
        int successCount = 0;
        int failureCount = 0;
        java.util.List<String> failedEmails = new java.util.ArrayList<>();

        for (EmailRequest request : emailRequests) {
            boolean success = sendPayslipEmail(
                request.toEmail,
                request.employeeName,
                request.payslipPdfPath,
                request.payrollPeriodName
            );

            if (success) {
                successCount++;
            } else {
                failureCount++;
                failedEmails.add(request.toEmail + " (" + request.employeeName + ")");
            }
        }

        return new EmailSendResult(successCount, failureCount, failedEmails);
    }

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", String.valueOf(smtpAuth));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtpTls));
        props.put("mail.smtp.ssl.enable", String.valueOf(smtpSsl));

        // Additional SSL properties if SSL is enabled
        if (smtpSsl) {
            props.put("mail.smtp.socketFactory.port", smtpPort);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        }

        if (smtpAuth && smtpUsername != null && !smtpUsername.trim().isEmpty()) {
            return Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });
        } else {
            return Session.getInstance(props);
        }
    }

    private String createEmailBody(String employeeName, String payrollPeriodName) {
        String contactEmail = System.getProperty("fin.email.contact",
            System.getenv("EMAIL_CONTACT") != null ? System.getenv("EMAIL_CONTACT") : "payroll@company.com");
        String contactPhone = System.getProperty("fin.email.phone",
            System.getenv("EMAIL_PHONE") != null ? System.getenv("EMAIL_PHONE") : "+1 555 123 4567");

        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(employeeName).append(",\n\n");
        body.append("Please find attached your payslip for ").append(payrollPeriodName).append(".\n\n");
        body.append("This is a computer-generated payslip. Please keep this document for your records.\n\n");
        body.append("If you have any questions about your payslip, please contact the HR department.\n\n");
        body.append("Best regards,\n");
        body.append("Payroll Department\n");
        body.append("Email: ").append(contactEmail).append("\n");
        body.append("Phone: ").append(contactPhone).append("\n");

        return body.toString();
    }

    /**
     * Data class for email request
     */
    public static class EmailRequest {
        public final String toEmail;
        public final String employeeName;
        public final String payslipPdfPath;
        public final String payrollPeriodName;

        public EmailRequest(String valueToEmail, String valueEmployeeName, String valuePayslipPdfPath, String valuePayrollPeriodName) {
            this.toEmail = valueToEmail;
            this.employeeName = valueEmployeeName;
            this.payslipPdfPath = valuePayslipPdfPath;
            this.payrollPeriodName = valuePayrollPeriodName;
        }
    }

    /**
     * Result class for bulk email operations
     */
    public static class EmailSendResult {
        public final int successCount;
        public final int failureCount;
        public final java.util.List<String> failedEmails;

        public EmailSendResult(int valueSuccessCount, int valueFailureCount, java.util.List<String> valueFailedEmails) {
            this.successCount = valueSuccessCount;
            this.failureCount = valueFailureCount;
            this.failedEmails = valueFailedEmails;
        }

        @Override
        public String toString() {
            return String.format("EmailSendResult{success=%d, failures=%d, failedEmails=%s}",
                               successCount, failureCount, failedEmails);
        }
    }
}