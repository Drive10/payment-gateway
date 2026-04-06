# Nova Checkout

A polished fintech checkout demo built with React, Vite, Tailwind CSS, Framer Motion, and jsPDF. The goal of this repository is to feel production-minded on GitHub while staying simple enough to explore quickly.

## Highlights

- Premium single-page checkout with a more intentional visual design
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

```text
.
├── .github/               # CI workflow, issue templates, PR template
├── src/
│   ├── components/        # Reusable UI building blocks
│   ├── lib/               # Payment helpers and transaction utilities
│   ├── pages/             # Route-level screens
│   ├── App.jsx            # Router setup
│   ├── index.css          # Global styles
│   └── main.jsx           # Vite entrypoint
├── CONTRIBUTING.md
├── eslint.config.js
└── README.md
```

## Getting Started

### Prerequisites

- Node.js 18+ recommended
- npm 9+ recommended

### Install

```bash
npm install
```

### Run Locally

```bash
npm run dev
```

Visit [http://localhost:5173](http://localhost:5173).

## Available Scripts

```bash
npm run dev          # start the Vite dev server
npm run build        # create a production build
npm run preview      # preview the production build locally
npm run lint         # run ESLint
npm run format       # format the repo with Prettier
npm run format:check # verify formatting
npm run check        # lint + build
```

## Demo Flow

1. Choose card or UPI payment.
2. Fill in the card form or scan the QR code.
3. Review the processing state.
4. Land on the success screen.
5. Download a PDF receipt.

## GitHub Polish Included

- Automated CI workflow for linting and production builds
- Structured GitHub issue templates for bugs and features
- Pull request template with verification checklist
- Contributor guide, license, editor config, ESLint, and Prettier

## Notes

- This is a frontend demo and does not process real payments.
- Transaction data is stored locally in the browser for the receipt flow.
- The repository currently contains some extra root-level files from earlier iterations; the active app entrypoint is under `src/`.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for the local workflow and expectations.

## License

This project is available under the [MIT License](./LICENSE).
