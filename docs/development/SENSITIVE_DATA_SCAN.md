Sensitive Data Scan — FIN repository

Date: 2025-11-27
Scope: Full repository scan for hardcoded secrets, credentials, private keys, backups, and other sensitive data that could create legal/security liability.

Summary of matches (concrete findings):

1) Hardcoded/default database credentials
- `dev-tools/import_transactions.py` and `dev-tools/import_complete_transactions.py`:
  - Use `os.getenv('DATABASE_USER', 'sthwalonyoni')` and `os.getenv('DATABASE_PASSWORD', 'drimPro1823')`.
  - Action: Remove default values; require environment variables or provide a local `.env.example` without real credentials.

2) Application defaults
- `spring-app/src/main/resources/application.properties` contains:
  - `spring.datasource.password=${DATABASE_PASSWORD:drimPro1823}` (default present)
  - `fin.jwt.secret=${JWT_SECRET:fin-secret-key-change-in-production}` (weak default)
  - Action: Replace defaults to require env vars and document required secrets.

3) Dev scripts and credentials
- `dev-tools/scripts/enhanced_export.sh` contains:
  - `DB_CONNECTION="postgresql://sthwalonyoni:LeZipho24#@localhost:5432/drimacc_db"` (embedded password)
- `dev-tools/scripts/reset_password.sql` includes a bcrypt hash and owner email `sthwaloe@gmail.com`.
- Several scripts reference `psql -U sthwalonyoni` and other user-specific commands.
- Action: Remove credentials from scripts or read from environment/secured config. Archive SQL scripts containing hashes securely.

4) Backups and exports
- Directory: `backups/` contains multiple `.dump` and `.sql` files (e.g., `drimacc_db_backup_2025-11-07_002038.dump`) and `exports/` contains export files.
- Action: Remove from repo tracking and store encrypted backups in secure storage. Treat these as potential data-breach artifacts until validated.

5) PII and payroll data
- `payslips/`, `reports/`, and output files contain payroll and employee-related info.
- Code writing or transmitting PII: `SpringPayslipPdfService.java` and `EmailService.java`.
- Action: Implement data handling policies, mask PII in logs, secure email transmission, and review retention.

6) IDE/workspace info
- `.idea/workspace.xml` and similar may leak local paths and user names.
- Action: Add IDE directories to `.gitignore` and remove sensitive workspace files.

No private keys/AWS tokens discovered in this scan (no `-----BEGIN PRIVATE KEY-----`, no `AKIA` tokens found).

Severity & priority
- Critical: Backups in `backups/`, default DB and SMTP credentials present in code/scripts.
- High: JWT default, dev script embedded DB connection strings, PII in reports and outputs.
- Medium: Logging PII, owner personal contact in headers.

Concrete git cleanup plan (do NOT run history rewrite without team coordination)

1) Remove sensitive files from tracking and add to `.gitignore` (safe local steps):

```bash
# 1. Remove tracked backups and export files from git
git rm --cached -r backups exports
# 2. Add to .gitignore
echo "backups/" >> .gitignore
echo "exports/" >> .gitignore
# 3. Remove IDE files
echo ".idea/" >> .gitignore
# 4. Commit the ignore changes (review before committing)
# IMPORTANT: We will not commit code changes automatically without confirmation.
git add .gitignore
git status --porcelain
# Review changes, then:
# git commit -m "Stop tracking sensitive backups, exports and IDE files"
```

2) Remove embedded credentials from files (example edits to make, do not commit automatically):
- `spring-app/src/main/resources/application.properties`:
  - Change `spring.datasource.password=${DATABASE_PASSWORD:drimPro1823}` → `spring.datasource.password=${DATABASE_PASSWORD}`
  - Change `fin.jwt.secret=${JWT_SECRET:fin-secret-key-change-in-production}` → `fin.jwt.secret=${JWT_SECRET}`
- `dev-tools/import_transactions.py` and `dev-tools/import_complete_transactions.py`:
  - Replace fallback `'drimPro1823'` with `''` or remove default value.
- `dev-tools/scripts/enhanced_export.sh`:
  - Remove embedded `DB_CONNECTION` or read from env: `DB_CONNECTION=${DB_CONNECTION}`

3) Purge sensitive files from history (coordinated & destructive):
- Prefer `git-filter-repo` (faster & recommended) or BFG. Example (do on a mirror clone):

```bash
# Mirror clone
git clone --mirror git@github.com:sthwalo/acc.git repo.git
# Use git-filter-repo to remove paths
cd repo.git
git filter-repo --path backups --path exports --invert-paths
# Or with BFG: bfg --delete-folders backups --no-blob-protection repo.git
# After cleaning, expire reflog and garbage collect
git reflog expire --expire=now --all && git gc --prune=now --aggressive
# Review, then force push the cleaned history (coordinate with team)
git push --force
```

4) Rotate secrets and revoke credentials
- Rotate DB passwords, SMTP credentials, any tokens found in logs or backups. Replace in environment store and CI secret manager.

5) Replace PII logging and mask identifiers
- Example code change: `SpringPayslipPdfService.java` and `EmailService.java` — mask employee numbers and emails in logs.

Verification steps
- Grep again for obvious secrets:
```bash
grep -R --line-number -E "PASSWORD|PASSWORD:|JWT_SECRET|fin.jwt.secret|drimPro1823|SMTP_PASSWORD|SMTP_USERNAME|API_KEY|SECRET|LeZipho24#" . || echo "no obvious secrets found"
```
- Ensure backups not tracked:
```bash
git ls-files | grep -i "backups" || echo "no backups tracked"
```

Notes & next actions
- I can prepare the exact patch files to remove defaults and mask logs and stage them locally for your review (I will not commit/push without your explicit confirmation). 
- I can also prepare a branch and commits if you want me to perform the cleanup and you confirm to proceed.

If you'd like, I will now prepare the safe patches (replace defaults, mask logs, add `.gitignore` entries) and stage them for review. Reply: "Prepare patches and stage" to proceed, or "Prepare patches and commit" to proceed with commits (I will still NOT rewrite history until you confirm).