# Contributing to PayFlow

Thank you for considering contributing to PayFlow! We welcome contributions from the community.

## How to Contribute

### Reporting Bugs

Before creating bug reports, please check if the issue has already been reported. When you are creating a bug report, please include as many details as possible:

- Use a clear and descriptive title
- Describe the exact steps to reproduce the problem
- Provide specific examples to demonstrate the steps
- Describe the behavior you observed and explain why it's incorrect
- Explain which version of PayFlow you're using and under what circumstances
- Include screenshots or videos if applicable

### Suggesting Features

Feature requests are welcome! Please provide:

- A clear and descriptive title
- A detailed description of the proposed feature
- The motivation behind the feature
- Any potential drawbacks or considerations
- Examples of how the feature would be used

### Pull Requests

1. Fork the repository and create your branch from `main`
2. If you've added code that should be tested, add tests
3. Ensure your code follows the existing code style
4. Make sure your changes pass all tests
5. Update documentation as needed
6. Submit your pull request with a clear description of your changes

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

### JavaScript/TypeScript

- Follow ESLint and Prettier configurations
- Use semicolons
- Prefer const over let, let over var
- Write JSDoc for public functions
- Keep components small and focused

## Testing

- Write unit tests for new functionality
- Ensure existing tests pass before submitting
- Aim for high test coverage
- Test both positive and negative cases
- Mock external dependencies appropriately

## Documentation

- Keep README.md up to date
- Update API documentation when endpoints change
- Document breaking changes in release notes
- Add comments to complex logic
- Update architectural diagrams when needed

## Community

- Be respectful and considerate of others
- Welcome newcomers and help them get started
- Give credit where credit is due
- Stay constructive in discussions
- Follow the project's Code of Conduct

## Getting Help

If you need help with your contribution, please:
- Check existing issues and documentation
- Ask questions in the issue tracker
- Reach out to maintainers

Thank you again for contributing to PayFlow!