# Secrets Rotation and Verification

This document lists immediate steps to rotate exposed credentials, verify the repository cleanup, and instructions for collaborators after a history rewrite.

## 1) Immediate secret rotation (high priority)

Rotate any credentials that were present in the repository (or might have been exposed):

- Database credentials (all environments)
  - Change the DB user/password in your database server for any accounts found in the repo.
  - Update application connection strings in secrets stores (CI/CD, Docker secrets, Kubernetes secrets).
- SMTP / Email credentials
  - Reset the SMTP account password and update secrets in CI and deployment.
- API keys and any third-party tokens
  - Revoke and recreate tokens; update deployment secrets.

After rotating, ensure new credentials are active and working before invalidating old ones.

## 2) Update secret storage locations

- GitHub Actions / CI: add secrets in repository settings (Settings → Secrets) or organization secret store.
- Docker Compose / Docker secrets: use `docker secret` or environment variables sourced from a secure file outside VCS.
- Kubernetes: use `kubectl create secret` or your secret manager (HashiCorp Vault, AWS Secrets Manager, Azure Key Vault).

## 3) Verify repository history cleanup (local checks)

Run these checks on the cleaned mirror clone and on a fresh clone of the remote after the force-push.

### In the mirror (where filter-repo ran):

```bash
cd ../acc-mirror.git
# check objects for remaining paths
git rev-list --all --objects | grep -E "backups/|\\.env($|/)|import_transactions.py|\\.dump|\\.sql" || echo "no matches"
# check commit stats
git log --all --stat | grep -E "backups/|\\.env|import_transactions.py" || echo "no matches"
```

### On a fresh clone of the remote (sanity-check):

```bash
# from an empty directory
git clone git@github.com:sthwalo/acc.git clean-acc-check
cd clean-acc-check
git rev-list --all --objects | grep -E "backups/|\\.env($|/)|import_transactions.py|\\.dump|\\.sql" || echo "no matches"
```

If any matches appear, stop and contact the team — the purge must be re-run with updated paths.

## 4) Notify and instruct collaborators

Required message (copy/paste to team):

```
Subject: REPO HISTORY REWRITE — action required (re-clone)

Hi team,

I rewrote the repository history to remove sensitive files (backups, .env, DB dumps). You MUST re-clone the repository to continue working safely.

Steps:
1) Backup any local unpushed branches (e.g. `git branch -vv` and `git format-patch -o /tmp/my-branches origin/branchname`).
2) Re-clone the cleaned repository:
   git clone git@github.com:sthwalo/acc.git
3) Recreate any local branches from your backups if needed.

If you have uncommitted work, stash or save patches first. If you need help preserving local branches, reply and I will provide exact commands.

Thanks — this was done to remove secrets from history. Rotate any external credentials you use.
```

## 5) Post-cleanup validation

- Run CI pipelines with new secrets and ensure they pass.
- Start the application in a test environment using the rotated credentials.
- Search across the repository and object database for any remaining secret patterns:

```bash
# repo root or fresh clone
grep -R --line-number -E "PASSWORD|SECRET|API_KEY|BEGIN PRIVATE KEY|drimPro1823|LeZipho24|SMTP_PASSWORD|DATABASE_PASSWORD" . || echo "no obvious secrets found"
```

## 6) Optional: rotate DB connection string names and secrets in code

- Ensure `application.properties` and other config do not contain fallback defaults (we removed those). Confirm that the environment variables are configured in your deployment.

---

If you want, I can also draft the collaborator email in GitHub issue format and prepare a small `push-and-notify.sh` helper that performs verification commands (created but not executed). Let me know which you prefer.
