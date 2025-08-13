# Security

- **Auth**: OAuth2/JWT for inter-service and client-to-gateway auth.
- **Transport**: TLS termination at gateway/ingress. mTLS inside the mesh (Istio/Linkerd) recommended in prod.
- **Secrets**: Use environment variables locally; in prod use Vault or AWS Secrets Manager.
- **Headers**: Add security headers via gateway filter (CSP, HSTS, etc.).
- **OWASP**: Validate inputs, sanitize logs (no PII), and rotate keys regularly.