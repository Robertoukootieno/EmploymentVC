#!/bin/bash

# Provenly Employment VC Platform - Microservices Setup Script
# This script creates the basic structure for all microservices

set -e

echo "🚀 Setting up Provenly Employment VC Platform Microservices..."

# Define services
SERVICES=(
    "auth-service"
    "schema-registry"
    "provenly-issuer-service"
    "provenly-verifier-service"
    "provenly-holder-wallet"
    "orchestration-service"
)

# Define service ports
declare -A SERVICE_PORTS=(
    ["auth-service"]="3001"
    ["schema-registry"]="8086"
    ["provenly-issuer-service"]="3004"
    ["provenly-verifier-service"]="3005"
    ["provenly-holder-wallet"]="3006"
    ["orchestration-service"]="3007"
)

# Function to create basic service structure
create_service_structure() {
    local service=$1
    local port=${SERVICE_PORTS[$service]}
    
    echo "📦 Creating structure for $service..."
    
    # Create directories
    mkdir -p "provenly-services/$service/src/"{config,controllers,services,models,middleware,routes,utils,types}
    mkdir -p "provenly-services/$service/"{tests,docs}
    
    # Create Dockerfile
    cat > "provenly-services/$service/Dockerfile" << EOF
# Multi-stage build for production optimization
FROM node:18-alpine AS base

# Install dependencies only when needed
FROM base AS deps
RUN apk add --no-cache libc6-compat
WORKDIR /app

# Install dependencies based on the preferred package manager
COPY package.json package-lock.json* ./
RUN npm ci --only=production && npm cache clean --force

# Development stage
FROM base AS dev
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
EXPOSE $port
CMD ["npm", "run", "dev"]

# Production build stage
FROM base AS builder
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

# Production runtime stage
FROM base AS runner
WORKDIR /app

ENV NODE_ENV=production

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# Copy built application
COPY --from=builder --chown=nextjs:nodejs /app/dist ./dist
COPY --from=deps --chown=nextjs:nodejs /app/node_modules ./node_modules
COPY --chown=nextjs:nodejs package.json ./

USER nextjs

EXPOSE $port

ENV PORT $port

CMD ["npm", "start"]
EOF

    # Create package.json
    cat > "provenly-services/$service/package.json" << EOF
{
  "name": "@provenly/$service",
  "version": "1.0.0",
  "description": "Provenly Employment VC Platform - $(echo $service | sed 's/-/ /g' | sed 's/\b\w/\u&/g')",
  "main": "dist/index.js",
  "scripts": {
    "build": "tsc",
    "start": "node dist/index.js",
    "dev": "ts-node-dev --respawn --transpile-only src/index.ts",
    "test": "jest",
    "test:watch": "jest --watch",
    "test:coverage": "jest --coverage",
    "lint": "eslint src/**/*.ts",
    "lint:fix": "eslint src/**/*.ts --fix",
    "format": "prettier --write src/**/*.ts",
    "migrate": "npx prisma migrate dev",
    "db:generate": "npx prisma generate",
    "db:push": "npx prisma db push"
  },
  "keywords": [
    "verifiable-credentials",
    "did",
    "ssi",
    "microservices",
    "employment"
  ],
  "author": "Provenly Team",
  "license": "MIT",
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5",
    "helmet": "^7.1.0",
    "morgan": "^1.10.0",
    "compression": "^1.7.4",
    "express-rate-limit": "^7.1.5",
    "express-validator": "^7.0.1",
    "axios": "^1.6.2",
    "@prisma/client": "^5.7.1",
    "prisma": "^5.7.1",
    "redis": "^4.6.10",
    "winston": "^3.11.0",
    "dotenv": "^16.3.1",
    "joi": "^17.11.0",
    "jsonwebtoken": "^9.0.2",
    "prom-client": "^15.1.0"
  },
  "devDependencies": {
    "@types/node": "^20.10.4",
    "@types/express": "^4.17.21",
    "@types/cors": "^2.8.17",
    "@types/morgan": "^1.9.9",
    "@types/compression": "^1.7.5",
    "@types/jsonwebtoken": "^9.0.5",
    "@types/jest": "^29.5.8",
    "@typescript-eslint/eslint-plugin": "^6.13.1",
    "@typescript-eslint/parser": "^6.13.1",
    "eslint": "^8.54.0",
    "eslint-config-prettier": "^9.0.0",
    "eslint-plugin-prettier": "^5.0.1",
    "jest": "^29.7.0",
    "ts-jest": "^29.1.1",
    "ts-node-dev": "^2.0.0",
    "typescript": "^5.3.2",
    "prettier": "^3.1.0",
    "supertest": "^6.3.3",
    "@types/supertest": "^2.0.16"
  },
  "engines": {
    "node": ">=18.0.0",
    "npm": ">=8.0.0"
  }
}
EOF

    # Create tsconfig.json
    cat > "provenly-services/$service/tsconfig.json" << EOF
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "commonjs",
    "lib": ["ES2020"],
    "outDir": "./dist",
    "rootDir": "./src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true,
    "resolveJsonModule": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true,
    "removeComments": true,
    "noImplicitAny": true,
    "noImplicitReturns": true,
    "noImplicitThis": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "exactOptionalPropertyTypes": true,
    "noImplicitOverride": true,
    "noPropertyAccessFromIndexSignature": true,
    "noUncheckedIndexedAccess": true,
    "experimentalDecorators": true,
    "emitDecoratorMetadata": true,
    "baseUrl": "./src",
    "paths": {
      "@/*": ["*"],
      "@/config/*": ["config/*"],
      "@/controllers/*": ["controllers/*"],
      "@/services/*": ["services/*"],
      "@/models/*": ["models/*"],
      "@/middleware/*": ["middleware/*"],
      "@/routes/*": ["routes/*"],
      "@/utils/*": ["utils/*"],
      "@/types/*": ["types/*"]
    }
  },
  "include": [
    "src/**/*"
  ],
  "exclude": [
    "node_modules",
    "dist",
    "**/*.test.ts",
    "**/*.spec.ts"
  ]
}
EOF

    # Create basic index.ts
    cat > "provenly-services/$service/src/index.ts" << EOF
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import compression from 'compression';
import rateLimit from 'express-rate-limit';
import { config } from './config/config';
import { logger } from './utils/logger';
import { errorHandler } from './middleware/errorHandler';
import { metricsMiddleware, register } from './middleware/metrics';
import { healthRouter } from './routes/health';

const app = express();

// Security middleware
app.use(helmet());
app.use(cors({
  origin: config.cors.origin,
  credentials: true,
}));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: 'Too many requests from this IP, please try again later.',
});
app.use(limiter);

// General middleware
app.use(compression());
app.use(morgan('combined', { stream: { write: (message) => logger.info(message.trim()) } }));
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Metrics middleware
app.use(metricsMiddleware);

// Health check
app.use('/health', healthRouter);

// Metrics endpoint
app.get('/metrics', async (req, res) => {
  res.set('Content-Type', register.contentType);
  res.end(await register.metrics());
});

// API routes
app.get('/', (req, res) => {
  res.json({
    service: '$service',
    version: '1.0.0',
    status: 'running',
    timestamp: new Date().toISOString(),
  });
});

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({
    error: 'Not Found',
    message: 'The requested resource was not found',
    path: req.originalUrl,
  });
});

// Error handling middleware
app.use(errorHandler);

// Start server
const port = config.port;
app.listen(port, () => {
  logger.info(\`$service started on port \${port}\`);
  logger.info(\`Environment: \${config.nodeEnv}\`);
  logger.info(\`Health Check: http://localhost:\${port}/health\`);
  logger.info(\`Metrics: http://localhost:\${port}/metrics\`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
  logger.info('SIGTERM received, shutting down gracefully');
  process.exit(0);
});

process.on('SIGINT', () => {
  logger.info('SIGINT received, shutting down gracefully');
  process.exit(0);
});

export default app;
EOF

    echo "✅ Created basic structure for $service"
}

# Create services
for service in "${SERVICES[@]}"; do
    create_service_structure "$service"
done

echo "🎉 All microservices structure created successfully!"
echo ""
echo "Next steps:"
echo "1. Run 'npm install' in each service directory"
echo "2. Implement service-specific logic"
echo "3. Set up database schemas with Prisma"
echo "4. Configure environment variables"
echo "5. Run 'docker-compose up' to start all services"
