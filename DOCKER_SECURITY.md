# Docker Security Configuration Guide

This document explains how to run the FIN application with proper security practices, avoiding hardcoded sensitive data and vulnerable images in Docker configurations.

## 🔒 Security Improvements Made

### PostgreSQL Container Updates

- ❌ **Before**: `postgres:15-alpine` (contained 2 high vulnerabilities)
- ✅ **After**: `postgres:17-alpine` (latest with enhanced security features and patches)
- ✅ **Ultra-secure**: Pinned digest to prevent supply chain attacks

### 2. Removed Hardcoded Credentials from Dockerfile
- ❌ **Before**: `ENV POSTGRES_PASSWORD=postgres` (hardcoded in image)
- ✅ **After**: Credentials passed via environment variables or secrets

### 2. Secure Environment Variable Usage
- Credentials come from `.env` file (not committed to Git)
- No sensitive data baked into Docker images
- Proper fallback to secure defaults

### 3. Docker Secrets Support
- Added `docker-compose.secure.yml` for production deployments
- Secrets stored in separate files outside of Git
- PostgreSQL supports both environment variables and secret files

## 🚀 How to Run Securely

### Option 1: Using Environment Variables (Development)
```bash
# Your existing .env file provides the credentials
docker compose up -d
```

### Option 2: Using Docker Secrets (Production)
```bash
# 1. Set up your secrets files first
echo "your_username" > secrets/db_user.txt
echo "your_secure_password" > secrets/db_password.txt
echo "your_smtp_password" > secrets/smtp_password.txt

# 2. Run with secure configuration
docker compose -f docker-compose.secure.yml up -d
```

### Option 3: Ultra-Secure with Pinned Images (Production)
```bash
# Uses specific image digests to prevent supply chain attacks
docker compose -f docker-compose.ultra-secure.yml up -d
```

### Option 4: External Secrets (Enterprise Production)
For production environments, use external secret management:
```yaml
secrets:
  db_password:
    external: true
    name: fin_db_password_v1
```

## 📁 File Structure
```
FIN/
├── .env                           # Your credentials (not in Git)
├── docker-compose.yml             # Development (uses .env)
├── docker-compose.secure.yml      # Production (uses secrets)
├── docker-compose.ultra-secure.yml # Ultra-secure with pinned digests
├── secrets/                       # Secret files (not in Git)
│   ├── db_user.txt
│   ├── db_password.txt
│   └── smtp_password.txt
└── .dockerignore                  # Excludes secrets from build
```

## 🛡️ Security Features

1. **Updated Base Images**: PostgreSQL 16-alpine (fixes 2 high vulnerabilities from v15)
2. **No Hardcoded Credentials**: All sensitive data comes from external sources
3. **Secrets Isolation**: Secret files are excluded from Docker build context
4. **Environment Separation**: Different configurations for dev/prod
5. **Fallback Security**: Secure defaults when environment variables are missing
6. **Docker Secrets Support**: Native Docker secret management for production
7. **Pinned Image Digests**: Prevents supply chain attacks (ultra-secure mode)
8. **Security Options**: no-new-privileges, tmpfs, non-root users

## ⚠️ Important Security Notes

1. **Never commit secrets**: Add `secrets/` to `.gitignore`
2. **Use strong passwords**: Generate secure random passwords
3. **Rotate credentials regularly**: Update passwords periodically
4. **Limit access**: Use proper file permissions on secret files
5. **Use external secrets in production**: Integrate with HashiCorp Vault, AWS Secrets Manager, etc.

## 🔧 Commands

### Check for security warnings:
```bash
# Dockerfile linting
docker run --rm -i hadolint/hadolint < Dockerfile

# Build without cache to verify
docker-compose build --no-cache
```

### Run with your existing credentials:
```bash
# Uses your .env file credentials automatically
docker-compose up -d postgres
```

### Verify security:
```bash
# Check that no secrets are in the image
docker history fin-fin-app | grep -i password
# (Should return nothing)
```

This setup eliminates the Docker security warnings while maintaining compatibility with your existing `.env` configuration.