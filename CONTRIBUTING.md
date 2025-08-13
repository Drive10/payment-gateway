# Contributing

Thanks for helping improve this project!

## Workflow
- Create a feature branch from `main`.
- Run `./mvnw verify` and ensure tests pass.
- Follow conventional commits: `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`.
- Open a PR with a clear description and screenshots/logs when helpful.

## Code Style
- Java 21, Spring Boot 3 (annotations over XML).
- Avoid static util singletons; prefer dependency injection.
- Unit tests with JUnit 5 + Mockito.
- Test containers for integration tests with PostgreSQL/Kafka/MongoDB.

## Security
- Never commit real secrets. Use `.env.example` and Kubernetes Secrets.
- Dependencies are scanned in CI (Trivy/OWASP/CodeQL). Fix critical issues before merge.

## DCO / CLA
By contributing, you agree your work is licensed under the repository license.
