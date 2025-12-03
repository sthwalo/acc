**Task:** Commercial Readiness — Codebase Cleanup & Liability Assessment

**Author:** Internal scan by engineering
**Date:** 2025-11-27

**Purpose**: Document the repository cleanup performed to prepare the FIN product for commercial deployment. This file summarizes findings (potential liabilities), severity, actions taken or recommended, concrete commands for remediation, verification steps, and follow-up checklist for legal/security teams.

**Scope**:
- Repository root: `/Users/sthwalonyoni/FIN`
- Focus areas: licensing claims, hardcoded secrets, backups/data dumps, PII handling, logging, third-party integrations, and release packaging obligations.

**Summary of Findings**
- **Commercial-license headers & trade secret notices**
  - Files: `dev-tools/scripts/frontend-licensing-complete.sh`, `dev-tools/scripts/frontend-licensing-setup.sh`, `LICENSE_HEADER.txt`, many Java source headers (e.g., files in `spring-app/`).
  - Risk: These headers assert commercial licensing and trade-secret claims; before commercial release, you must ensure the legal infrastructure (license templates, sales/support processes, warranty disclaimers) matches what's claimed.

- **Hardcoded credentials & default secrets**
  - Files: `dev-tools/import_transactions.py`, `dev-tools/import_complete_transactions.py` (default user `sthwalonyoni`, password `drimPro1823`), `spring-app/src/main/resources/application.properties` (default DB password `drimPro1823` and `fin.jwt.secret=fin-secret-key-change-in-production`), `dev-tools/scripts/reset_password.sql`.
  - Risk: High — committed secrets create immediate security liability and must be removed and rotated.

- **Backups & database dumps in repo**
  - Directory: `backups/` (many `.dump` / `.sql` files such as `drimacc_db_backup_2025-11-07_002038.dump`).
  - Risk: Critical — these likely contain real PII and financial data. Storing them in source control is a severe compliance and breach risk.

- **PII handling, payroll & tax data**
  - Directories & files: `payslips/`, `reports/`, `output/`, `backups/`, and code: `spring-app/src/main/java/fin/service/spring/SpringPayslipPdfService.java`, `spring-app/src/main/java/fin/service/spring/EmailService.java`.
  - Risk: High — payroll and tax numbers are sensitive personal data. Transmission (email), storage, logging, and backups must follow POPIA/GDPR-like requirements.

- **Logging PII**
  - Examples: `SpringPayslipPdfService` logs employee number and payslip number; `EmailService` logs recipient names/emails on success/failure.
  - Risk: Medium-High — logs are often accessible to operators and third-party systems; PII should be masked or removed from logs.

- **Third-party libraries & licensing**
  - Confirmed usage: Apache PDFBox (Apache 2.0), PostgreSQL JDBC (BSD), Spring Boot (Apache 2.0), libharu (native). No AGPL/iText detected in initial scan, but full dependency list must be generated.
  - Risk: Medium — must include proper license notices and ensure no viral/copyleft obligations are overlooked.

- **Personal owner contact details in headers**
  - Many files contain personal email and phone of the owner.
  - Risk: Low (operational/PR); consider replacing with corporate contact details for commercial distribution.

**Actions Taken (scan & immediate safe adjustments)**
- Completed an automated repository scan for: vendor strings, license terms, secret-like tokens, PII indicators, and backup files.
- Identified specific files and directories (listed above) requiring immediate remediation.
- Prepared an action list and recommended commands (below). No destructive changes were pushed to remote — all recommendations are staged for your approval.

**Immediate Remediation (Critical) — run these as soon as possible**
1) Remove backups from git tracking and stop including them in future commits:
```bash
git rm --cached -r backups
echo "backups/" >> .gitignore
git add .gitignore
git commit -m "Remove backups from repo and add to .gitignore"
```

After this, plan a history rewrite to purge those files from the repository (coordinate with team):
```bash
# Using BFG Repo-Cleaner (example workflow)
git clone --mirror git@github.com:sthwalo/acc.git repo.git
bfg --delete-folders backups --no-blob-protection repo.git
cd repo.git
git reflog expire --expire=now --all && git gc --prune=now --aggressive
# Review the cleaned mirror before force-pushing:
git push --force
# NOTE: Rewriting history requires coordination and informing all contributors.
```

2) Remove hardcoded credentials and default secrets (example edits):
- Replace defaults with environment variables or placeholders in `spring-app/src/main/resources/application.properties`:
```properties
# Change this:
# spring.datasource.password=${DATABASE_PASSWORD:drimPro1823}
# fin.jwt.secret=${JWT_SECRET:fin-secret-key-change-in-production}

# To this (require env vars):
spring.datasource.password=${DATABASE_PASSWORD}
fin.jwt.secret=${JWT_SECRET}
```
- Remove defaults from `dev-tools/import_transactions.py` and `dev-tools/import_complete_transactions.py` (replace fallback with empty string or require env var).

3) Rotate any credentials that were used in production/testing that matched values found in repo (DB, SMTP, API keys). Notify the team and revoke old secrets.

4) Stop logging PII — patch code locations that log identifying info (mask or remove):
- Example change in `SpringPayslipPdfService.java`:
  - From: `LOGGER.info("Generating PDF payslip for employee: " + employee.getEmployeeNumber() ...);`
  - To: `LOGGER.info("Generating PDF payslip for employee: [REDACTED] for payslip: [REDACTED]");`

5) Secure email sending: ensure SMTP credentials are stored in a secrets manager or environment variables; enforce STARTTLS/SSL and do not store credentials in source control.

**Medium-term Remediation (High priority next steps)**
- Perform a full dependency license audit (generate a list from `build.gradle.kts` / `gradle` tooling). Verify that no AGPL/viral licenses are present.
- Prepare a NOTICE file and include third-party license attributions in any distribution.
- Replace personal contact info in headers with corporate/legal contacts for commercial releases.
- Encrypt backups at rest and remove sensitive backups from the repo; move backups to secure storage (S3 with proper IAM, or encrypted vault).
- Implement environment-specific logging configuration to redact PII and centralize logs in a secured logging stack with limited access.

**Compliance & Legal Checklist (must be validated before commercial release)**
- [ ] Legal review of the repository headers that state "Commercial use requires separate licensing" — produce a formal commercial license that aligns with what is claimed.
- [ ] Data Protection Assessment (POPIA/GDPR): document legal basis for personal data processing, retention periods, and consent mechanisms.
- [ ] Incident Response and Breach Notification Plan with contact persons and timelines.
- [ ] Third-party license bundle and NOTICE file included in distribution artifacts.
- [ ] Secrets management and rotation policy implemented (use Vault, AWS Secrets Manager, or similar).
- [ ] Secure email and storage configuration for payslip and payroll artifacts.

**Verification Steps (how to confirm remediation)**
1. After removing backups from tracked files, run:
```bash
# Ensure no large dumps remain in the working tree
git ls-files | grep -i "backups" || echo "no backups tracked"
```
2. Ensure no secrets are present:
```bash
# Quick scan (local):
grep -R --line-number -E "PASSWORD|PASSWORD:|JWT_SECRET|fin.jwt.secret|drimPro1823|SMTP_PASSWORD|SMTP_USERNAME|API_KEY|SECRET" . || echo "no obvious secrets found"
```
3. Run a dependency license scan (example using Gradle plugin or `./gradlew dependencies` and a license auditor):
```bash
# Example using Gradle license plugin (if added to build)
./gradlew generateLicenseReport
```
4. Confirm PII is not logged in production logs by reviewing recent logs and changing logging levels where necessary.

**Owner & Roles (suggested)**
- Engineering lead: implement code patches and secrets removal; coordinate history-rewrite operations with VCS admin.
- Security lead: rotate credentials, set up secrets manager, scan for remaining secrets, review logs.
- Legal counsel: draft commercial license, review headers, confirm third-party license obligations, prepare DPA.
- DevOps: ensure backups are moved to secure storage, TLS enforced for SMTP and services, CI/CD secrets handled securely.

**Follow-up Tasks (recommended immediate items you can assign)**
- [ ] Create a small commit that removes default secrets in `application.properties` and `dev-tools` scripts and replace with placeholders. (Engineering)
- [ ] Purge backups from git (use BFG/git-filter-repo) after offline archive. (Engineering + VCS admin)
- [ ] Run a dependency license audit and produce `THIRD_PARTY_NOTICES.md`. (Engineering + Legal)
- [ ] Create `SECURITY.md` outlining how to report issues, secure coding guidelines, and secrets handling. (Security)
- [ ] Produce a `PRIVACY_GUIDELINES.md` and a data flow diagram for payroll data. (Legal + Engineering)

**Appendix — Quick commands referenced**
- Remove backups from tracking and ignore:
```bash
git rm --cached -r backups
echo "backups/" >> .gitignore
git add .gitignore
git commit -m "Remove backups/ from repo and add to .gitignore"
```

- Replace default secrets (example file edit):
```bash
# Edit application.properties (macOS sed example)
sed -i '' 's/DATABASE_PASSWORD:drimPro1823/DATABASE_PASSWORD:REPLACE_WITH_SECRET/' spring-app/src/main/resources/application.properties
sed -i '' 's/fin-secret-key-change-in-production/REPLACE_WITH_ENV_JWT_SECRET/' spring-app/src/main/resources/application.properties
```

- Mask PII in logging (code-level change):
  - Edit `spring-app/src/main/java/fin/service/spring/SpringPayslipPdfService.java` and `spring-app/src/main/java/fin/service/spring/EmailService.java` to avoid logging unmasked identifiers.

**Notes & Warnings**
- Rewriting git history to remove sensitive files is disruptive — coordinate with all contributors and back up the repository first.
- Do not publish or push the repo to a public remote until backups and secrets are removed.
- If you suspect any of the dumped backups contain real customer data, treat them as a data breach until proven otherwise: consult legal and security, and follow breach notification requirements.

---

If you want, I can now:
- Prepare the small code patches to remove default secrets and mask PII logging and run a quick local check.
- Generate a dependency license report (`build.gradle.kts`) and produce `THIRD_PARTY_NOTICES.md`.
- Draft `SECURITY.md` and `PRIVACY_GUIDELINES.md` templates for your repo.

Tell me which of these you want me to do next and I'll proceed. If you want the immediate patches prepared, say: "Proceed with code patches."