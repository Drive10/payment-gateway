# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

Please report security vulnerabilities by emailing security@payflow.dev or by creating a private issue in this repository.

We will acknowledge receipt of your report within 24 hours and will provide a detailed response within 7 days indicating how we are addressing the vulnerability.

For critical vulnerabilities, we will work with you to develop a mitigation strategy while we prepare a patch.

## Security Best Practices

This project follows these security practices:

- No hardcoded secrets in the codebase
- Regular dependency updates via Dependabot
- Automated security scanning in CI/CD pipeline
- Container image scanning for vulnerabilities
- Regular dependency vulnerability checks
- Secure default configurations
- Input validation and sanitization
- Principle of least privilege
- Regular security audits

## Dependencies

We use automated tools to keep our dependencies up to date and secure:
- Dependabot for automatic updates
- Trivy for container and filesystem scanning
- Maven Central Repository for Java dependencies
- npm registry for JavaScript dependencies

Please ensure you're using the latest versions of our dependencies to benefit from security patches.