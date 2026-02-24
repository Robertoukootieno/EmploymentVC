# Deployment Guide

This guide covers deployment options for the Provenly Employment VC Platform across different environments.

## Prerequisites

### For Docker Deployment
- Docker 20.10+
- Docker Compose 2.0+
- 4GB+ RAM
- 20GB+ disk space

### For Kubernetes Deployment
- Kubernetes cluster 1.24+
- kubectl configured
- Helm 3.0+ (optional)
- 8GB+ RAM per node
- 50GB+ disk space

### For Production
- Load balancer (nginx, HAProxy, or cloud LB)
- SSL certificates
- Monitoring infrastructure
- Backup solution

## Development Deployment

### Quick Start with Docker Compose

1. **Clone and setup**
   ```bash
   git clone <repository-url>
   cd EmploymentVC
   cp .env.development .env
   ```

2. **Start infrastructure**
   ```bash
   COMPOSE_PROJECT_NAME=employmentvc docker compose up -d postgres redis besu-node keycloak
   ```

3. **Start walt.id services**
   ```bash
   COMPOSE_PROJECT_NAME=employmentvc docker compose up -d waltid-core waltid-signatory
   ```

4. **Start monitoring**
   ```bash
   COMPOSE_PROJECT_NAME=employmentvc docker compose up -d prometheus grafana jaeger
   ```

5. **Start microservices**
   ```bash
   COMPOSE_PROJECT_NAME=employmentvc docker compose -f docker-compose.services.yml up -d
   ```

6. **Verify deployment**
   ```bash
   curl http://localhost:3000/health
   ```

### Using the Setup Script

```bash
chmod +x scripts/setup-development.sh
./scripts/setup-development.sh
```

## Staging Deployment

### Docker Compose for Staging

1. **Prepare environment**
   ```bash
   cp .env.production .env.staging
   # Edit .env.staging with staging-specific values
   ```

2. **Deploy with staging configuration**
   ```bash
   COMPOSE_PROJECT_NAME=employmentvc-staging docker compose -f docker-compose.yml -f docker-compose.staging.yml up -d
   ```

3. **Configure load balancer**
   ```nginx
   upstream api_backend {
       server localhost:3000;
   }
   
   server {
       listen 80;
       server_name staging-api.provenly.io;
       
       location / {
           proxy_pass http://api_backend;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
       }
   }
   ```

## Production Deployment

### Kubernetes Deployment

1. **Prepare cluster**
   ```bash
   # Create namespaces
   kubectl apply -f k8s/namespace.yaml
   ```

2. **Configure secrets**
   ```bash
   # Update secrets with production values
   kubectl apply -f k8s/secrets.yaml
   ```

3. **Deploy infrastructure**
   ```bash
   kubectl apply -f k8s/postgres.yaml
   kubectl apply -f k8s/redis.yaml
   kubectl apply -f k8s/configmap.yaml
   ```

4. **Deploy services**
   ```bash
   kubectl apply -f k8s/
   ```

5. **Verify deployment**
   ```bash
   kubectl get pods -n provenly
   kubectl get services -n provenly
   ```

### Production Checklist

#### Security
- [ ] Update all default passwords
- [ ] Configure TLS certificates
- [ ] Set up network policies
- [ ] Enable pod security policies
- [ ] Configure RBAC

#### Monitoring
- [ ] Configure Prometheus alerts
- [ ] Set up Grafana dashboards
- [ ] Configure log aggregation
- [ ] Set up health checks
- [ ] Configure uptime monitoring

#### Backup
- [ ] Database backup strategy
- [ ] Key material backup
- [ ] Configuration backup
- [ ] Disaster recovery plan

#### Performance
- [ ] Configure resource limits
- [ ] Set up horizontal pod autoscaling
- [ ] Configure caching
- [ ] Optimize database queries
- [ ] Set up CDN for static assets

## Cloud Provider Specific Deployments

### AWS EKS

1. **Create EKS cluster**
   ```bash
   eksctl create cluster --name provenly-prod --region us-west-2
   ```

2. **Configure storage classes**
   ```yaml
   apiVersion: storage.k8s.io/v1
   kind: StorageClass
   metadata:
     name: gp3-encrypted
   provisioner: ebs.csi.aws.com
   parameters:
     type: gp3
     encrypted: "true"
   ```

3. **Deploy with AWS-specific configurations**
   ```bash
   kubectl apply -f k8s/aws/
   ```

### Google GKE

1. **Create GKE cluster**
   ```bash
   gcloud container clusters create provenly-prod \
     --zone us-central1-a \
     --num-nodes 3 \
     --enable-autoscaling \
     --min-nodes 1 \
     --max-nodes 10
   ```

2. **Configure workload identity**
   ```bash
   kubectl apply -f k8s/gcp/workload-identity.yaml
   ```

### Azure AKS

1. **Create AKS cluster**
   ```bash
   az aks create \
     --resource-group provenly-rg \
     --name provenly-prod \
     --node-count 3 \
     --enable-addons monitoring \
     --generate-ssh-keys
   ```

2. **Configure Azure-specific resources**
   ```bash
   kubectl apply -f k8s/azure/
   ```

## Environment Configuration

### Environment Variables

Key production environment variables:

```bash
# Application
NODE_ENV=production
LOG_LEVEL=info

# Database
POSTGRES_HOST=postgres-service
POSTGRES_PORT=5432
DATABASE_URL=postgresql://user:pass@host:5432/db

# Security
JWT_SECRET=<strong-random-secret>
ENCRYPTION_KEY=<32-byte-key>

# External Services
EBSI_API_BASE_URL=https://api.ebsi.eu
WALTID_CORE_API_URL=http://waltid-core:7000

# Monitoring
PROMETHEUS_ENABLED=true
JAEGER_ENDPOINT=http://jaeger:14268/api/traces
```

### Secrets Management

#### Kubernetes Secrets
```bash
kubectl create secret generic provenly-secrets \
  --from-literal=jwt-secret=<secret> \
  --from-literal=db-password=<password> \
  --namespace=provenly
```

#### External Secret Management
- AWS Secrets Manager
- Azure Key Vault
- Google Secret Manager
- HashiCorp Vault

## Scaling Configuration

### Horizontal Pod Autoscaling

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: api-gateway-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### Vertical Pod Autoscaling

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: api-gateway-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: api-gateway
  updatePolicy:
    updateMode: "Auto"
```

## Monitoring and Alerting

### Prometheus Alerts

```yaml
groups:
- name: provenly-alerts
  rules:
  - alert: HighErrorRate
    expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.1
    for: 5m
    annotations:
      summary: High error rate detected
      
  - alert: DatabaseDown
    expr: up{job="postgres"} == 0
    for: 1m
    annotations:
      summary: Database is down
```

### Grafana Dashboards

Import dashboards for:
- Application metrics
- Infrastructure metrics
- Business metrics
- SLA monitoring

## Backup and Recovery

### Database Backup

```bash
# Automated backup script
kubectl create cronjob postgres-backup \
  --image=postgres:15 \
  --schedule="0 2 * * *" \
  -- pg_dump -h postgres-service -U user dbname > backup.sql
```

### Key Material Backup

```bash
# Backup DID keys and certificates
kubectl get secrets -n provenly -o yaml > secrets-backup.yaml
```

## Troubleshooting

### Common Issues

1. **Services not starting**
   ```bash
   kubectl describe pod <pod-name> -n provenly
   kubectl logs <pod-name> -n provenly
   ```

2. **Database connection issues**
   ```bash
   kubectl exec -it postgres-pod -- psql -U user -d dbname
   ```

3. **Network connectivity**
   ```bash
   kubectl exec -it api-gateway-pod -- nslookup postgres-service
   ```

### Health Checks

```bash
# Check all services
kubectl get pods -n provenly
kubectl get services -n provenly

# Check specific service health
curl http://api-gateway-service/health
```

## Maintenance

### Rolling Updates

```bash
# Update service image
kubectl set image deployment/api-gateway api-gateway=new-image:tag -n provenly

# Check rollout status
kubectl rollout status deployment/api-gateway -n provenly
```

### Database Migrations

```bash
# Run migrations
kubectl exec -it api-gateway-pod -- npm run migrate
```

### Certificate Renewal

```bash
# Using cert-manager
kubectl apply -f k8s/certificates.yaml
```

## Performance Tuning

### Database Optimization
- Connection pooling
- Query optimization
- Index management
- Partitioning

### Application Optimization
- Caching strategies
- Connection pooling
- Resource limits
- JVM tuning (if applicable)

### Network Optimization
- Service mesh (Istio)
- Load balancing
- CDN configuration
- Compression
