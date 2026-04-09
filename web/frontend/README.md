# PayFlow Checkout UI

A polished fintech checkout demo built with React, Vite, and Tailwind CSS. This is the frontend component of the PayFlow enterprise payment gateway.

> **Note**: This is part of the [PayFlow](https://github.com/Drive10/payflow) monorepo. See the [root README](../README.md) for full project details.

## Highlights

- Premium single-page checkout with polished visual design
- Card flow with formatting, Luhn validation, and inline form feedback
- UPI payment option with generated QR code
- Animated processing and success states
- Downloadable PDF receipt with persisted transaction data
- GitHub-ready contributor files, issue templates, PR template, and CI

## Tech Stack

- React 18
- Vite 5
- Tailwind CSS 3
- Framer Motion
- jsPDF
- ESLint 8
- Prettier 3

## Project Structure

```
web/frontend/
├── .github/               # CI workflow, issue templates, PR template
├── src/
│   ├── components/        # Reusable UI building blocks
│   ├── lib/              # Payment helpers and transaction utilities
│   ├── pages/            # Route-level screens
│   ├── App.jsx           # Router setup
│   ├── index.css         # Global styles
│   └── main.jsx          # Vite entrypoint
├── CONTRIBUTING.md
├── eslint.config.js
└── README.md
```

## Getting Started

### Prerequisites

- Node.js 18+ (22+ recommended)
- npm 9+

### Install

```bash
cd web/frontend
npm install
```

### Run Locally

```bash
npm run dev
```

Visit [http://localhost:5173](http://localhost:5173).

## Available Scripts

```bash
npm run dev          # Start the Vite dev server
npm run build        # Create a production build
npm run preview      # Preview the production build locally
npm run lint         # Run ESLint
npm run format       # Format the repo with Prettier
npm run format:check # Verify formatting
npm run check        # lint + build
npm run test         # Run tests
npm run test:coverage # Run tests with coverage
```

## Demo Flow

1. Choose card or UPI payment method
2. Fill in the card form or scan the QR code
3. Review the processing state
4. Land on the success screen
5. Download a PDF receipt

## Integration with Backend

The checkout UI connects to the PayFlow API Gateway. Infrastructure (PostgreSQL, Redis, Kafka, etc.) runs in Docker by default.

### Backend Local Development (Recommended)
1. Start infrastructure:
   ```bash
   # From root directory
   docker compose --profile infra up -d
   ```
2. Start backend services in your IDE with Spring profile set to `local`
3. Start frontend:
   ```bash
   npm run dev
   ```

### Full Docker Mode
```bash
# From root directory
docker compose --profile infra --profile services up -d
```

Visit [http://localhost:5173](http://localhost:5173) for the frontend.

API endpoints:
- `POST /api/v1/orders` - Create order
- `POST /api/v1/payments` - Process payment

## GitHub Polish

- Automated CI workflow for linting and production builds
- Structured GitHub issue templates for bugs and features
- Pull request template with verification checklist
- Contributor guide, license, editor config, ESLint, and Prettier

## Notes

- This is a frontend demo and does not process real payments.
- Transaction data is stored locally in the browser for the receipt flow.
- For full payment processing, see the [PayFlow backend services](../README.md).

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for the local workflow and expectations.
Or see [root CONTRIBUTING.md](../CONTRIBUTING.md) for project-wide guidelines.

## License

This project is available under the [MIT License](../LICENSE).