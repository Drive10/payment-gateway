# PayFlow Dashboard

A payment monitoring dashboard built with Next.js for the PayFlow payment processing system.

## Features

- Real-time payment monitoring
- Revenue analytics and trends
- Transaction history and details
- Authentication and authorization
- Responsive design with Tailwind CSS
- Data visualization with Recharts
- Advanced data tables with TanStack Table
- State management with Zustand
- Data fetching with TanStack Query
- Form handling with React Hook Form + Zod

## Technology Stack

- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **State Management**: Zustand
- **Data Fetching**: TanStack Query
- **Forms**: React Hook Form + Zod validation
- **Data Tables**: TanStack Table
- **Charts**: Recharts
- **Notifications**: Sonner
- **Icons**: Heroicons
- **UI Components**: Headless UI

## Getting Started

### Prerequisites

- Node.js 18+
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

### Environment Variables

Create a `.env.local` file in the root directory:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws
```

## Project Structure

```
/app - Next.js app router pages and layouts
/src/components - Reusable React components
/src/lib - Utility functions and hooks
/src/hooks - Custom React hooks
/tests - Test files
```

## API Integration

The dashboard integrates with the PayFlow payment service through the following endpoints:

- `GET /api/payments/overview` - Payment statistics
- `GET /api/payments/revenue` - Revenue metrics
- `GET /api/payments/trends` - Payment trends over time
- `GET /api/payments/recent` - Recent transactions

## Authentication

The dashboard uses JWT-based authentication. Users must log in to access the dashboard features.

## Real-time Updates

WebSocket connection for real-time payment updates.

## Testing

Run tests with:

```bash
npm test
```

## Building for Production

```bash
npm run build
npm start
```

## License

MIT
