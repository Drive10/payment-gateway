# Contributing to PayFlow

Thank you for considering contributing to PayFlow! We welcome contributions from the community.

## Quick Start

1. **Fork** the repository
2. **Clone** your fork: `git clone https://github.com/yourusername/payflow.git`
3. **Create** a feature branch: `git checkout -b feature/your-feature`
4. **Make** your changes and commit following our conventions
5. **Push** to your fork and **submit** a Pull Request

## Reporting Bugs

Before creating bug reports, please check if the issue has already been reported. When you are creating a bug report, please include as many details as possible:

- Use a clear and descriptive title
- Describe the exact steps to reproduce the problem
- Provide specific examples to demonstrate the steps
- Describe the behavior you observed and explain why it's incorrect
- Explain which version of PayFlow you're using and under what circumstances
- Include screenshots or videos if applicable

### Bug Report Template

Use the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md) when filing issues.

## Suggesting Features

Feature requests are welcome! Please provide:

- A clear and descriptive title
- A detailed description of the proposed feature
- The motivation behind the feature
- Any potential drawbacks or considerations
- Examples of how the feature would be used

### Feature Request Template

Use the [feature request template](.github/ISSUE_TEMPLATE/feature_request.md) when filing issues.

## Pull Requests

### PR Workflow

1. Fork the repository and create your branch from `main`
2. If you've added code that should be tested, add tests
3. Ensure your code follows the existing code style
4. Make sure your changes pass all tests
5. Update documentation as needed
6. Submit your pull request with a clear description of your changes

### Branch Naming

- `feature/` - New features (e.g., `feature/add-payment-provider`)
- `bugfix/` - Bug fixes (e.g., `bugfix/fix-auth-timeout`)
- `hotfix/` - Critical production fixes (e.g., `hotfix/security-patch`)
- `refactor/` - Code refactoring (e.g., `refactor/optimize-database-queries`)
- `docs/` - Documentation updates (e.g., `docs/update-api-docs`)

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `perf`, `ci`

Examples:
```
feat(payment): add Stripe payment provider
fix(auth): resolve JWT token expiration issue
docs(readme): update installation instructions
perf(api): optimize database query performance
```

### PR Description

Include in your PR description:
- **Summary**: What does this PR do?
- **Related Issues**: Link any related issues (e.g., "Fixes #123")
- **Testing**: How was this tested?
- **Screenshots**: If UI changes, include screenshots

## Development Setup

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Node.js 22+ (for web apps)
- Maven 3.9+

### Getting Started

1. Clone the repository
```bash
git clone https://github.com/yourusername/payflow.git
cd payflow
```

2. Start infrastructure services
```bash
docker compose --profile infra up -d
```

3. Build all services
```bash
mvn clean package -DskipTests
```

4. Run services locally
```bash
./scripts/dev.sh start
```

5. Start the frontend
```bash
cd web/frontend && npm run dev
```

## Code Style

### Java

- Follow the Google Java Style Guide
- Use 4 spaces for indentation
- Import ordering: standard Java, third-party, project-specific
- Keep lines under 100 characters when possible
- Write meaningful Javadoc for public APIs
- Use Lombok annotations where appropriate

### JavaScript/TypeScript

- Follow ESLint and Prettier configurations
- Use semicolons
- Prefer const over let, let over var
- Write JSDoc for public functions
- Keep components small and focused

### Naming Conventions

- **Classes**: PascalCase (e.g., `PaymentService`)
- **Methods/variables**: camelCase (e.g., `processPayment`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
- **Packages**: lowercase (e.g., `com.payflow.payment`)

## Testing

### Backend Tests

```bash
# Run all tests
mvn test

# Run specific service tests
mvn test -pl services/payment-service

# Run with coverage
mvn test -pl services/payment-service -Djacoco.skip=false
```

### Frontend Tests

```bash
cd web/frontend
npm run test
npm run test:coverage
```

### Test Requirements

- Write unit tests for new functionality
- Ensure existing tests pass before submitting
- Aim for high test coverage (>80% for new code)
- Test both positive and negative cases
- Mock external dependencies appropriately
- Include integration tests for API endpoints

## Security

### Sensitive Data

- **NEVER** commit secrets, API keys, or credentials to version control
- Use environment variables for configuration
- Reference `.env.example` for required variables
- Report security vulnerabilities via the [Security Policy](SECURITY.md)

### Payment Security

- Never log sensitive payment data (card numbers, CVV, etc.)
- Follow PCI DSS compliance guidelines
- Use tokenization for payment methods

## Documentation

- Keep README.md up to date
- Update API documentation when endpoints change
- Document breaking changes in release notes
- Add Javadoc comments to complex logic
- Update architectural diagrams when needed
- Add inline code comments for non-obvious logic

## Community

- Be respectful and considerate of others
- Welcome newcomers and help them get started
- Give credit where credit is due
- Stay constructive in discussions
- Follow the project's [Code of Conduct](CODE_OF_CONDUCT.md)

## Getting Help

If you need help with your contribution, please:
- Check existing issues and documentation
- Ask questions in the issue tracker
- Reach out to maintainers

## Recognition

Contributors will be recognized in the README.md file and release notes.

---

Thank you again for contributing to PayFlow!