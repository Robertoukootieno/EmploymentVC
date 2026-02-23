#!/bin/bash

# Script to create common files for all microservices

set -e

echo "🔧 Setting up common files for all microservices..."

# Define services (including api-gateway and did-registry)
SERVICES=(
    "api-gateway"
    "auth-service"
    "did-registry"
    "schema-registry"
    "provenly-issuer-service"
    "provenly-verifier-service"
    "provenly-holder-wallet"
    "orchestration-service"
)

# Function to create common files for a service
create_common_files() {
    local service=$1
    local port
    
    # Define service ports
    case $service in
        "api-gateway") port="3000" ;;
        "auth-service") port="3001" ;;
        "did-registry") port="3002" ;;
        "schema-registry") port="8086" ;;
        "provenly-issuer-service") port="3004" ;;
        "provenly-verifier-service") port="3005" ;;
        "provenly-holder-wallet") port="3006" ;;
        "orchestration-service") port="3007" ;;
    esac
    
    echo "📝 Creating common files for $service..."
    
    # Create config.ts
    cat > "provenly-services/$service/src/config/config.ts" << EOF
import dotenv from 'dotenv';

dotenv.config();

export const config = {
  nodeEnv: process.env.NODE_ENV || 'development',
  port: parseInt(process.env.PORT || '$port', 10),
  
  // Database Configuration
  database: {
    url: process.env.DATABASE_URL || 'postgresql://provenly_dev:dev_password@localhost:5432/provenly_dev',
  },

  // Redis Configuration
  redis: {
    url: process.env.REDIS_URL || 'redis://localhost:6379',
  },

  // JWT Configuration
  jwt: {
    secret: process.env.JWT_SECRET || 'your-secret-key',
    expiresIn: process.env.JWT_EXPIRES_IN || '24h',
  },

  // CORS Configuration
  cors: {
    origin: process.env.CORS_ORIGIN || '*',
  },

  // Logging Configuration
  logging: {
    level: process.env.LOG_LEVEL || 'info',
    format: process.env.LOG_FORMAT || 'json',
  },

  // External Services
  services: {
    besuRpcUrl: process.env.BESU_RPC_URL || 'http://localhost:8545',
    ebsiApiUrl: process.env.EBSI_API_BASE_URL || 'https://api-pilot.ebsi.eu',
    waltidCoreUrl: process.env.WALTID_CORE_API_URL || 'http://localhost:7000',
    didRegistryUrl: process.env.DID_REGISTRY_URL || 'http://localhost:3002',
    schemaRegistryUrl: process.env.SCHEMA_REGISTRY_URL || 'http://localhost:8086',
  },
};
EOF

    # Create logger.ts
    cat > "provenly-services/$service/src/utils/logger.ts" << EOF
import winston from 'winston';
import { config } from '../config/config';

const logFormat = winston.format.combine(
  winston.format.timestamp(),
  winston.format.errors({ stack: true }),
  config.logging.format === 'json' 
    ? winston.format.json()
    : winston.format.combine(
        winston.format.colorize(),
        winston.format.simple()
      )
);

export const logger = winston.createLogger({
  level: config.logging.level,
  format: logFormat,
  defaultMeta: { service: '$service' },
  transports: [
    new winston.transports.Console(),
  ],
});

// If we're not in production, log to the console with a simple format
if (config.nodeEnv !== 'production') {
  logger.add(new winston.transports.Console({
    format: winston.format.combine(
      winston.format.colorize(),
      winston.format.simple()
    )
  }));
}
EOF

    # Create errorHandler.ts
    cat > "provenly-services/$service/src/middleware/errorHandler.ts" << EOF
import { Request, Response, NextFunction } from 'express';
import { logger } from '../utils/logger';

export interface AppError extends Error {
  statusCode?: number;
  isOperational?: boolean;
}

export const errorHandler = (
  error: AppError,
  req: Request,
  res: Response,
  next: NextFunction
) => {
  const statusCode = error.statusCode || 500;
  const message = error.message || 'Internal Server Error';

  // Log error details
  logger.error('Error occurred:', {
    error: error.message,
    stack: error.stack,
    url: req.url,
    method: req.method,
    ip: req.ip,
    userAgent: req.get('User-Agent'),
    statusCode,
  });

  // Don't leak error details in production
  const isDevelopment = process.env.NODE_ENV === 'development';

  const errorResponse = {
    error: statusCode >= 500 ? 'Internal Server Error' : message,
    message: isDevelopment ? message : 'Something went wrong',
    ...(isDevelopment && { stack: error.stack }),
    timestamp: new Date().toISOString(),
    path: req.url,
  };

  res.status(statusCode).json(errorResponse);
};

export const createError = (message: string, statusCode: number = 500): AppError => {
  const error = new Error(message) as AppError;
  error.statusCode = statusCode;
  error.isOperational = true;
  return error;
};
EOF

    # Create metrics.ts
    cat > "provenly-services/$service/src/middleware/metrics.ts" << EOF
import { Request, Response, NextFunction } from 'express';
import client from 'prom-client';

// Create a Registry to register the metrics
export const register = new client.Registry();

// Add a default label which is added to all metrics
register.setDefaultLabels({
  app: 'provenly-$service'
});

// Enable the collection of default metrics
client.collectDefaultMetrics({ register });

// Create custom metrics
const httpRequestDuration = new client.Histogram({
  name: 'http_request_duration_seconds',
  help: 'Duration of HTTP requests in seconds',
  labelNames: ['method', 'route', 'status_code'],
  buckets: [0.1, 0.3, 0.5, 0.7, 1, 3, 5, 7, 10]
});

const httpRequestTotal = new client.Counter({
  name: 'http_requests_total',
  help: 'Total number of HTTP requests',
  labelNames: ['method', 'route', 'status_code']
});

const activeConnections = new client.Gauge({
  name: 'http_active_connections',
  help: 'Number of active HTTP connections'
});

// Register custom metrics
register.registerMetric(httpRequestDuration);
register.registerMetric(httpRequestTotal);
register.registerMetric(activeConnections);

export const metricsMiddleware = (req: Request, res: Response, next: NextFunction) => {
  const start = Date.now();
  
  // Increment active connections
  activeConnections.inc();

  // Override res.end to capture metrics
  const originalEnd = res.end;
  res.end = function(chunk?: any, encoding?: any) {
    const duration = (Date.now() - start) / 1000;
    const route = req.route?.path || req.path;
    
    // Record metrics
    httpRequestDuration
      .labels(req.method, route, res.statusCode.toString())
      .observe(duration);
    
    httpRequestTotal
      .labels(req.method, route, res.statusCode.toString())
      .inc();

    // Decrement active connections
    activeConnections.dec();

    // Call original end method
    originalEnd.call(this, chunk, encoding);
  };

  next();
};
EOF

    # Create health.ts
    cat > "provenly-services/$service/src/routes/health.ts" << EOF
import { Router, Request, Response } from 'express';
import { config } from '../config/config';

const router = Router();

interface HealthCheck {
  status: 'healthy' | 'unhealthy';
  timestamp: string;
  uptime: number;
  service: string;
  version: string;
}

router.get('/', (req: Request, res: Response) => {
  const healthCheck: HealthCheck = {
    status: 'healthy',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    service: '$service',
    version: '1.0.0',
  };

  res.status(200).json(healthCheck);
});

router.get('/ready', (req: Request, res: Response) => {
  // Readiness check - service is ready to accept traffic
  res.status(200).json({
    status: 'ready',
    timestamp: new Date().toISOString(),
  });
});

router.get('/live', (req: Request, res: Response) => {
  // Liveness check - service is alive
  res.status(200).json({
    status: 'alive',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
  });
});

export { router as healthRouter };
EOF

    echo "✅ Created common files for $service"
}

# Create common files for all services
for service in "${SERVICES[@]}"; do
    create_common_files "$service"
done

echo "🎉 All common files created successfully!"
