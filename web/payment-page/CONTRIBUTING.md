# Contributing to PayFlow Frontend

Thanks for helping improve the PayFlow checkout UI!

## Local Setup

1. Install dependencies:
```bash
cd web/payment-page
npm install
```

2. Start the dev server:
```bash
npm run dev
```

3. Run quality checks:
```bash
npm run check
```

## Development Guidelines

- Keep components small and focused
- Prefer accessible UI patterns and semantic HTML
- Run `npm run lint` before opening a pull request
- If you change behavior, update the documentation in `README.md`

## Pull Requests

- Use clear titles and follow conventional commits
- Include screenshots for UI changes
- Mention any follow-up work or tradeoffs in the PR description

## Testing

```bash
npm run test           # Run tests
npm run test:coverage  # Run with coverage
```

## Code Style

- Follow ESLint and Prettier configurations
- Use functional components with hooks
- Keep components focused on single responsibility
- Use semantic HTML and accessible patterns

## Related Docs

- See [root CONTRIBUTING.md](../../CONTRIBUTING.md) for project-wide guidelines
- See [DEVELOPER_GUIDE.md](../../DEVELOPER_GUIDE.md) for full setup instructions
