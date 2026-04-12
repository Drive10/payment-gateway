# Dashboard Service

NestJS-based backend service for the PayFlow dashboard, providing APIs for payment monitoring and analytics.

## Features

- Payment monitoring and analytics APIs
- Authentication with JWT
- Database integration with Prisma and PostgreSQL
- Real-time updates with BullMQ and Redis
- Docker support
- API documentation with Swagger
- Health checks
- Rate limiting
- Logging and monitoring

## Technology Stack

- **Framework**: NestJS
- **Language**: TypeScript
- **Database**: PostgreSQL with Prisma ORM
- **Cache**: Redis
- **Message Queue**: BullMQ
- **Authentication**: JWT
- **API Documentation**: Swagger/OpenAPI
- **Containerization**: Docker

## Getting Started

### Prerequisites

- Node.js 18+
- PostgreSQL
- Redis
- Docker (optional)

### Installation

```bash
# Install dependencies
npm install

# Set up environment variables
cp .env.example .env

# Run database migrations
npx prisma migrate dev

# Start development server
npm run start:dev
```

### Environment Variables

Create a `.env` file based on `.env.example`:

```env
# Server
PORT=3000
NODE_ENV=development

# Database
DB_HOST=localhost
DB_PORT=5432
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_NAME=payflow_dashboard

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
JWT_SECRET=your-secret-key
JWT_EXPIRES_IN=1h

# Stripe (for payment processing)
STRIPE_SECRET_KEY=sk_test_...
```

## API Endpoints

### Authentication
- `POST /auth/login` - Login user
- `POST /auth/logout` - Logout user
- `POST /auth/refresh` - Refresh token

### Payments
- `GET /payments/overview` - Get payment statistics
- `GET /payments/revenue` - Get revenue metrics
- `GET /payments/trends` - Get payment trends
- `GET /payments/recent` - Get recent transactions
- `GET /payments/:id` - Get payment by ID

### Analytics
- `GET /analytics/merchant/:id` - Get merchant analytics
- `GET /analytics/summary` - Get summary analytics

## Docker Deployment

```bash
# Build Docker image
docker build -t payflow/dashboard-service .

# Run container
docker run -p 3000:3000 --env-file .env payflow/dashboard-service
```

## Testing

```bash
# Run unit tests
npm test

# Run tests with coverage
npm run test:cov

# Run end-to-end tests
npm run test:e2e
```

## Project Structure

```
/src - Source code
  /app - Application module
  /auth - Authentication module
  /dashboard - Dashboard module
  /analytics - Analytics module
  /common - Shared utilities
/prisma - Prisma schema and migrations
/test - Test files
```

## License

MIT
