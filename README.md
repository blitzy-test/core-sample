# core-sample

A production-ready, full-stack web application that demonstrates best practices in modern software architecture. The system consists of a Java-based backend (sample-core module) and a React frontend, providing a comprehensive foundation for enterprise applications with an integrated payments platform.

## Overview

Core-sample serves as a reference implementation for enterprise-grade web applications, demonstrating:

- Clean separation of concerns between frontend and backend components
- RESTful API design following industry standards
- Containerized deployment for environment consistency
- Type-safe development across the technology stack
- Modern, responsive user interfaces with React
- Comprehensive payments platform with robust transaction management capabilities
- Centralized payments warehouse for unified transaction data storage

## Features

### Backend Features

- Java 21 with JAX-RS/Jersey for RESTful API implementation
- PostgreSQL 17.4 database integration with HikariCP connection pooling
- Configuration-driven behavior through YAML files and environment variables
- Health monitoring endpoints for operational visibility
- Cross-origin resource sharing (CORS) support
- Payment transaction processing with full lifecycle management
- Advanced payment data querying with filtering, sorting, and pagination
- Payment event tracking and history management

### Frontend Features

- React 18.2+ with TypeScript for type-safe UI development
- Tailwind CSS 3.3+ for responsive design
- Vite 5.1+ for fast development and optimized builds
- React Router 6.21+ for client-side routing
- Redux Toolkit for state management
- React Query 4.0+ for efficient data fetching
- Specialized payment management interfaces for transaction operations

## Getting Started

### Prerequisites

- Java 21 JDK
- Node.js 18+
- PostgreSQL 17.4
- Docker and Docker Compose (for containerized development)

### Development Environment Setup

#### Using Docker Compose

The easiest way to get started is using Docker Compose, which sets up the entire development environment including PostgreSQL 17.4 with the required payment schema:

1. Clone the repository:
   ```
   git clone https://github.com/briklabs/core-sample.git
   cd core-sample
   ```

2. Start the development environment:
   ```
   docker-compose up -d
   ```

This will start:
- PostgreSQL 17.4 database with the required schemas
- Backend service with HikariCP connection pooling configured
- Frontend development server

#### Manual Setup

If you prefer to set up components individually:

1. **Database Setup**:
   - Install PostgreSQL 17.4
   - Create a database named `sample`
   - Create schemas: `sample` and `payment`
   - Run the migration scripts in `backend/src/main/resources/db/migration/`

2. **Backend Setup**:
   - Navigate to the backend directory: `cd backend`
   - Configure database connection in `config.yaml`
   - Configure HikariCP in `hikari-config.properties`
   - Build the application: `mvn clean package`
   - Run the application: `java -Dhikaricp.configurationFile=hikari-config.properties -jar target/app.jar`

3. **Frontend Setup**:
   - Navigate to the frontend directory: `cd frontend`
   - Install dependencies: `npm install`
   - Start the development server: `npm run dev`

### Configuration

#### Backend Configuration

The backend is configured through several files:

- `backend/config.yaml`: Main configuration file for database connection, API settings, etc.
- `backend/hikari-config.properties`: HikariCP connection pool configuration for optimal database performance
- `backend/payment-config.yaml`: Payment module-specific configuration
- `backend/payment-logging.xml`: Specialized logging configuration for payment operations

#### HikariCP Configuration

The payment module uses HikariCP for high-performance database connection pooling. Key configuration parameters include:

- `maximumPoolSize`: 30 connections (configurable up to 50 for high-volume deployments)
- `minimumIdle`: 10 connections to maintain ready capacity
- `connectionTimeout`: 20000ms to prevent transaction bottlenecks
- `idleTimeout`: 300000ms for optimal connection hygiene
- `maxLifetime`: 1200000ms to prevent stale connections
- `leakDetectionThreshold`: 60000ms to identify connection leaks

## API Documentation

### Core API Endpoints

- `/up`: Health check endpoint
- `/sample/hello`: Sample endpoint returning both text and JSON formats

### Payment API Endpoints

The payment module exposes RESTful endpoints following this pattern:
- `/organizations/{org_id}/accounts/{account_id}/transactions/`: List transactions
- `/organizations/{org_id}/accounts/{account_id}/transactions/{transaction_id}`: Get transaction details
- `/organizations/{org_id}/accounts/{account_id}/transactions/{transaction_id}/capture`: Capture a transaction
- `/organizations/{org_id}/accounts/{account_id}/transactions/{transaction_id}/refund`: Refund a transaction

Special `_all` placeholder is supported to retrieve data for all accounts or transactions.

## Testing

### Backend Testing

The backend includes comprehensive test coverage:

- Unit tests for core components
- Integration tests for API endpoints
- Database integration tests
- Payment module-specific tests for transaction processing

Run tests with:
```
cd backend
mvn test
```

### Frontend Testing

The frontend includes:

- Component tests with React Testing Library
- Integration tests for payment workflows
- End-to-end tests for critical user journeys

Run tests with:
```
cd frontend
npm test
```

## Deployment

### Docker Deployment

The application can be deployed using Docker:

```
docker build -t core-sample-backend ./backend
docker run -p 5900:5900 -e BRIK_CONFIG=/config/config.yaml -e HIKARI_CONFIG=/config/hikari-config.properties core-sample-backend
```

### Kubernetes Deployment

For production deployments, Kubernetes manifests are provided in the `k8s` directory.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.