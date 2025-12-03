# Security Quick Reference - FIN Financial Management System

**Last Updated**: December 3, 2025  
**Purpose**: Quick reference for secure development practices

---

## 🔒 Golden Rules

1. **NEVER commit `.env` file** - All real credentials ONLY in `.env`
2. **NEVER hardcode passwords** - Use `System.getenv()` or `${ENV_VAR}`
3. **NEVER hardcode database names** - Use environment variables
4. **NEVER hardcode usernames** - Use environment variables
5. **NEVER hardcode file paths with usernames** - Use dynamic detection

---

## ✅ Secure Coding Patterns

### **Java (Backend)**

```java
// ✅ CORRECT - Fetch from environment
String dbUrl = System.getenv("DATABASE_URL");
String dbUser = System.getenv("DATABASE_USER");
String dbPassword = System.getenv("DATABASE_PASSWORD");

if (dbUrl == null || dbUser == null || dbPassword == null) {
    throw new IllegalStateException("Database credentials missing");
}

Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
```

### **Spring Boot Properties**

```properties
# ✅ CORRECT - No defaults with real credentials
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USER}
spring.datasource.password=${DATABASE_PASSWORD}
fin.jwt.secret=${JWT_SECRET}
```

### **TypeScript (Frontend)**

```typescript
// ✅ CORRECT - Use Vite environment variables
const apiUrl = import.meta.env.VITE_API_URL || '/api';
```

### **Shell Scripts**

```bash
# ✅ CORRECT - Source from .env
source .env
psql -U $DATABASE_USER -d $(basename $DATABASE_URL)
```

### **Docker Compose**

```yaml
# ✅ CORRECT - Use environment variables
environment:
  DATABASE_URL: ${DATABASE_URL}
  DATABASE_USER: ${DATABASE_USER}
  DATABASE_PASSWORD: ${DATABASE_PASSWORD}
```

---

## ❌ Forbidden Patterns

### **NEVER Do This**

```java
// ❌ FORBIDDEN
String dbUrl = "jdbc:postgresql://localhost:5432/drimacc_db";
String dbUser = "sthwalonyoni";
String dbPassword = "DrimPro1823";

// ❌ FORBIDDEN
String basePath = "/Users/sthwalonyoni/FIN";

// ❌ FORBIDDEN
fin.jwt.secret=${JWT_SECRET:fin-secret-key-change-in-production}
```

---

## 📁 File Security Matrix

| File Type | Real Credentials | Committed to Git | Gitignored |
|-----------|-----------------|------------------|------------|
| `.env` | ✅ YES | ❌ NEVER | ✅ YES |
| `application.properties` | ❌ NEVER | ✅ YES | ❌ NO |
| `application.properties.example` | ❌ NEVER | ✅ YES | ❌ NO |
| `docker-compose.yml` | ❌ NEVER | ✅ YES | ❌ NO |
| `*.java` | ❌ NEVER | ✅ YES | ❌ NO |
| `*.ts` | ❌ NEVER | ✅ YES | ❌ NO |
| `secrets/*` | ✅ YES | ❌ NEVER | ✅ YES |
| `*.md` (docs) | ❌ NEVER | ✅ YES | ❌ NO |

---

## 🔍 Pre-Commit Security Checklist

Before every commit, verify:

```bash
# 1. Check for hardcoded credentials
grep -rn --exclude-dir={.git,node_modules,build,dist} \
  -E "(password|PASSWORD|secret|SECRET).*=.*['\"].*['\"]" .

# 2. Check for database names
grep -rn --exclude-dir={.git,node_modules,build,dist} \
  -E "(drimacc_db|fin_db)" .

# 3. Check for usernames
grep -rn --exclude-dir={.git,node_modules,build,dist} \
  -E "sthwalonyoni" .

# 4. Check for hardcoded paths
grep -rn --exclude-dir={.git,node_modules,build,dist} \
  -E "/Users/[a-zA-Z0-9]+/" .

# 5. Verify .env is not staged
git status | grep -q ".env" && echo "⚠️  WARNING: .env is staged!" || echo "✅ Safe"
```

---

## 🚨 Emergency Response - Credentials Exposed

If you accidentally commit credentials:

```bash
# 1. Immediately rotate ALL exposed credentials
# - Change database password
# - Regenerate JWT secret
# - Update SMTP password

# 2. Remove from Git history (use BFG Repo-Cleaner)
java -jar bfg.jar --replace-text passwords.txt .git
git reflog expire --expire=now --all
git gc --prune=now --aggressive

# 3. Force push cleaned history
git push --force

# 4. Update .env with new credentials
# 5. Notify team and audit access logs
```

---

## 📚 Environment Variables Reference

### **Required Variables** (Must be in `.env`)

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/your_db
DATABASE_USER=your_user
DATABASE_PASSWORD=your_password

# Test Database (optional)
TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/test_db
TEST_DATABASE_USER=test_user
TEST_DATABASE_PASSWORD=test_password

# JWT
JWT_SECRET=your_secret_key_min_32_chars

# SMTP (optional)
SMTP_HOST=mail.example.com
SMTP_PORT=465
SMTP_USERNAME=your_email@example.com
SMTP_PASSWORD=your_smtp_password
```

### **Optional Variables**

```bash
# Application
FIN_BASE_PATH=/path/to/fin
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

---

## 🎯 Security Testing

### **Test Credential Loading**

```bash
# Verify environment variables are loaded
source .env
echo "DB URL: ${DATABASE_URL}"
echo "DB User: ${DATABASE_USER}"
echo "Password set: $([ -n "$DATABASE_PASSWORD" ] && echo "YES" || echo "NO")"
```

### **Test Application Startup**

```bash
# Verify app fails gracefully without credentials
unset DATABASE_URL
./gradlew bootRun  # Should fail with clear error message
```

---

## 📖 Documentation Security

When writing documentation:

```bash
# ❌ BAD - Exposes username
psql -U sthwalonyoni -d drimacc_db

# ✅ GOOD - Uses environment variables
source .env
psql -U $DATABASE_USER -h localhost -d $(basename $DATABASE_URL)

# ✅ GOOD - Generic placeholder
psql -U YOUR_USERNAME -d YOUR_DATABASE
```

---

## 🔐 Password Security

### **User Passwords**

```java
// ✅ Always use BCrypt
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hashedPassword = encoder.encode(plainPassword);
```

### **Database Passwords**

- Minimum 12 characters
- Mix of uppercase, lowercase, numbers, symbols
- Never reuse passwords across environments
- Rotate quarterly

### **JWT Secrets**

- Minimum 32 characters
- Cryptographically random
- Different per environment (dev/staging/prod)
- Rotate annually or after suspected compromise

---

## 📞 Security Contacts

**Report Security Issues**:
- Email: sthwaloe@gmail.com
- Subject: [SECURITY] FIN Vulnerability Report

**Emergency Credential Rotation**:
1. Contact: Immaculate Nyoni
2. Rotate credentials immediately
3. Document in `docs/security/INCIDENT_*.md`

---

## 📊 Security Audit Schedule

- **Daily**: Pre-commit security checks (automated)
- **Weekly**: Review access logs
- **Monthly**: Scan dependencies for vulnerabilities
- **Quarterly**: Full security audit
- **Annually**: External security assessment

---

## ✅ Quick Security Check

Run this before every commit:

```bash
#!/bin/bash
# save as: scripts/security-check.sh

echo "🔍 Running security checks..."

# Check .env is gitignored
if git check-ignore -q .env; then
    echo "✅ .env is gitignored"
else
    echo "❌ ERROR: .env is NOT gitignored!"
    exit 1
fi

# Check for staged credentials
if git diff --cached --name-only | grep -q "\.env"; then
    echo "❌ ERROR: .env is staged for commit!"
    exit 1
fi

# Check for hardcoded passwords
if git diff --cached | grep -i "password.*=.*['\"].*['\"]"; then
    echo "⚠️  WARNING: Possible hardcoded password detected!"
    exit 1
fi

echo "✅ Security checks passed!"
```

---

**Security is Everyone's Responsibility** 🔒
