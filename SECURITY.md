# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

We take the security of this payment gateway seriously. If you discover a security vulnerability, please follow these steps:

1. **DO NOT** open a public GitHub issue
2. Email us at: security@payflow.dev (replace with actual security contact)
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

## Response Timeline

- **Acknowledgment**: Within 48 hours
- **Initial Assessment**: Within 5 business days
- **Fix Deployment**: Within 30 days for critical vulnerabilities
- **Public Disclosure**: After fix is deployed and users have had time to update

## Security Best Practices

This project follows OWASP Top 10 guidelines and implements:
- JWT-based authentication with secure token management
- Rate limiting on all API endpoints
- Input validation and sanitization
- SQL injection prevention via parameterized queries
- XSS protection through security headers
- CSRF protection for state-changing operations
- Secure password hashing with bcrypt
- HTTPS enforcement in production
- Regular dependency updates via Dependabot
- Automated security scanning in CI/CD

## Security Checklist

See [SECURITY-CHECKLIST.md](./docs/SECURITY-CHECKLIST.md) for the complete security implementation checklist.
