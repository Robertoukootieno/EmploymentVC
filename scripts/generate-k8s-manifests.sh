#!/bin/bash

# Script to generate Kubernetes manifests for all microservices

set -e

echo "🚀 Generating Kubernetes manifests for Provenly microservices..."

# Define services with their configurations
declare -A SERVICES=(
    ["auth-service"]="3001:authentication:backend"
    ["did-registry"]="3002:did-registry:backend"
    ["schema-registry"]="8086:schema-registry:backend"
    ["provenly-issuer-service"]="3004:issuer:backend"
    ["provenly-verifier-service"]="3005:verifier:backend"
    ["provenly-holder-wallet"]="3006:wallet:backend"
    ["orchestration-service"]="3007:orchestration:backend"
)

# Function to generate deployment manifest for a service
generate_service_manifest() {
    local service=$1
    local config=$2
    
    IFS=':' read -r port component tier <<< "$config"
    
    echo "📝 Generating manifest for $service..."
    
    cat > "k8s/$service.yaml" << EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $service
  namespace: provenly
  labels:
    app: $service
    component: $component
    tier: $tier
spec:
  replicas: 2
  selector:
    matchLabels:
      app: $service
  template:
    metadata:
      labels:
        app: $service
        component: $component
        tier: $tier
    spec:
      containers:
      - name: $service
        image: ghcr.io/robertoukootieno/employmentvc/$service:latest
        ports:
        - containerPort: $port
        env:
        - name: NODE_ENV
          valueFrom:
            configMapKeyRef:
              name: provenly-config
              key: NODE_ENV
        - name: PORT
          value: "$port"
        - name: DATABASE_URL
          value: "postgresql://\$(POSTGRES_USER):\$(POSTGRES_PASSWORD)@postgres-service:5432/\$(POSTGRES_DB)"
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: provenly-secrets
              key: POSTGRES_USER
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: provenly-secrets
              key: POSTGRES_PASSWORD
        - name: POSTGRES_DB
          valueFrom:
            secretKeyRef:
              name: provenly-secrets
              key: POSTGRES_DB
        - name: REDIS_URL
          value: "redis://redis-service:6379"
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: provenly-secrets
              key: JWT_SECRET
        - name: BESU_RPC_URL
          valueFrom:
            configMapKeyRef:
              name: provenly-config
              key: BESU_RPC_URL
        - name: EBSI_API_BASE_URL
          valueFrom:
            configMapKeyRef:
              name: provenly-config
              key: EBSI_API_BASE_URL
        - name: EBSI_DID_REGISTRY_URL
          valueFrom:
            configMapKeyRef:
              name: provenly-config
              key: EBSI_DID_REGISTRY_URL
        - name: WALTID_CORE_API_URL
          valueFrom:
            configMapKeyRef:
              name: provenly-config
              key: WALTID_CORE_API_URL
        - name: WALTID_SIGNATORY_API_URL
          valueFrom:
            configMapKeyRef:
              name: provenly-config
              key: WALTID_SIGNATORY_API_URL
        - name: WALTID_API_KEY
          valueFrom:
            secretKeyRef:
              name: provenly-secrets
              key: WALTID_API_KEY
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health/live
            port: $port
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health/ready
            port: $port
          initialDelaySeconds: 5
          periodSeconds: 5
        securityContext:
          runAsNonRoot: true
          runAsUser: 1001
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          capabilities:
            drop:
            - ALL
---
apiVersion: v1
kind: Service
metadata:
  name: $service-service
  namespace: provenly
  labels:
    app: $service
    component: $component
    tier: $tier
spec:
  selector:
    app: $service
  ports:
  - port: $port
    targetPort: $port
    protocol: TCP
    name: http
  type: ClusterIP
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: $service-hpa
  namespace: provenly
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: $service
  minReplicas: 1
  maxReplicas: 5
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
EOF

    echo "✅ Generated manifest for $service"
}

# Generate manifests for all services
for service in "${!SERVICES[@]}"; do
    generate_service_manifest "$service" "${SERVICES[$service]}"
done

echo "🎉 All Kubernetes manifests generated successfully!"
echo ""
echo "Generated files:"
ls -la k8s/*.yaml
echo ""
echo "Next steps:"
echo "1. Review and customize the manifests as needed"
echo "2. Apply namespace and secrets: kubectl apply -f k8s/namespace.yaml -f k8s/secrets.yaml"
echo "3. Apply infrastructure: kubectl apply -f k8s/postgres.yaml -f k8s/redis.yaml"
echo "4. Apply services: kubectl apply -f k8s/"
echo "5. Check deployment status: kubectl get pods -n provenly"
