# Safety & Ethics Rules

> Security and ethical guidelines for AI agents working on PayFlow

---

## 1. Priority Hierarchy

When rules conflict, apply this priority:

```
🔴 CRITICAL (Level 1): Safety & Security
├── Never commit secrets/credentials
├── Never bypass authentication/authorization
└── Never log sensitive payment data

🟠 HIGH (Level 2): Production Readiness
├── All tests pass before commit
├── Lint/format checks pass
├── No security vulnerabilities
└── Documentation for new features

🟡 MEDIUM (Level 3): Code Quality
├── Follow naming conventions
├── Keep files under size limits
├── Write tests for new code
└── Use appropriate design patterns

🟢 LOW (Level 4): Best Practices
├── Add comments for complex logic
├── Optimize database queries
├── Use caching where appropriate
└── Add logging for debugging
```

---

## 2. Critical Safety Rules

### ❌ NEVER DO

- Execute destructive commands without approval
- Commit secrets, API keys, or credentials
- Bypass authentication/authorization checks
- Log sensitive payment data (card numbers, CVV)
- Create tight coupling between services
- Skip input validation
- Use auto-generated IDs for business entities
- Bypass circuit breakers in production

### ✅ ALWAYS DO

- Validate all inputs before processing
- Use parameterized queries (prevent SQL injection)
- Log with appropriate levels (ERROR, WARN, INFO, DEBUG)
- Request confirmation for production deployments
- Sanitize file paths to prevent directory traversal
- Use environment variables for secrets

---

## 3. Sensitive Data Handling

### Payment Data

| Data Type | Storage | Logging |
|-----------|---------|---------|
| Full card number | NEVER | NEVER |
| CVV | NEVER | NEVER |
| Card token | Allowed | Last 4 only |
| Transaction ID | Allowed | Allowed |
| Amount | Allowed | Allowed |

### PII Data

- Email: Hash for storage, mask for logs
- Phone: Mask for logs
- Address: Encrypt at rest

---

## 4. Require Human Approval

### Must Get Approval For

- Direct database modifications (`DROP`, `DELETE` without `WHERE`)
- Production deployments
- Changes to security configurations
- Removing authentication/authorization
- Modifying environment variables with secrets
- Rolling back releases

### Can Proceed Without Approval

- Bug fixes in development
- Adding unit tests
- Documentation updates
- Code refactoring (non-breaking)
- Linting/formatting fixes

---

## 5. Security Scanning

### Pre-commit Checks

1. No secrets in code
2. No sensitive data logging
3. All tests passing
4. Lint checks passed

### CI Pipeline Checks

1. Trivy vulnerability scan
2. Dependency audit
3. Secret detection

---

## 6. Incident Response

### If Security Issue Found

1. **STOP** - Don't proceed with implementation
2. **REPORT** - Notify user immediately
3. **ASSESS** - Evaluate impact
4. **PLAN** - Propose mitigation
5. **FIX** - Apply fix with review

---

## Quick Reference

| Action | Approval Required |
|--------|------------------|
| Commit secrets | ❌ NEVER |
| Bypass auth | ❌ NEVER |
| Production deploy | ✅ YES |
| DB destructive | ✅ YES |
| Add tests | ❌ NO |
| Fix docs | ❌ NO |
| Refactor code | ❌ NO |