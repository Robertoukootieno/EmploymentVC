# Observability Stack for EmploymentVC

This directory contains configuration and deployment files for monitoring, logging, and tracing across all services.

## Components

- **Prometheus**: Metrics collection and alerting
  - `prometheus/` for config and rules
- **Grafana**: Dashboards and visualization
  - `grafana/dashboards/` for JSON dashboards
  - `grafana/datasources/` for data source configs
- **Loki**: Centralized log aggregation
  - `loki/` for config
- **Tempo**: Distributed tracing
  - `tempo/` for config
- **alerts/**: Alerting rules and notification configs

## Quick Start

1. Deploy the stack (example Compose):
   ```sh
   docker compose -f observability/docker-compose.observability.yml up -d
   ```
2. Access Grafana:
   - URL: http://localhost:3000
   - Default user: admin / admin
3. Prometheus:
   - URL: http://localhost:9090
4. Loki:
   - Base URL: http://localhost:3100
   - Health: http://localhost:3100/ready
   - API status: http://localhost:3100/loki/api/v1/status/buildinfo
5. Tempo:
   - Base URL: http://localhost:3200
   - Health: http://localhost:3200/ready

Note: Loki and Tempo return `404 page not found` on `/` by default. Use the health/API endpoints above to validate availability.

## Adding Dashboards & Alerts
- Place custom dashboards in `grafana/dashboards/`
- Add Prometheus alert rules in `prometheus/rules/`
- Add Grafana alert configs in `alerts/`

## What Has Been Implemented

### 1) Alerting integrations
- Prometheus alert rules are enabled from `prometheus/rules/`.
- Alertmanager is configured for email + Slack + webhook routing.
- Extra PagerDuty integration example is available in:
  - `alerts/alertmanager.pagerduty.example.yml`

### 2) Real-time alert rules
- Core service alerts:
  - `ServiceDown`
  - `HighErrorRate`
  - `HighLatency`
- Infrastructure/runtime alerts:
  - `ContainerMemoryUsage`
  - `ContainerRestarted`
  - `HighCPUUsage`
  - `DatabaseConnectionFailures`
  - `SlowServiceResponse`

### 3) Dashboards added
- `grafana/dashboards/EmploymentVC-Overview.json`
- `grafana/dashboards/EmploymentVC-Infra-Health.json`

### 4) Loki and Tempo startup fixes
- `docker-compose.observability.yml` now mounts explicit config files and passes explicit `-config.file` arguments.
- Loki is configured for local single-node use and updated schema compatibility.
- Tempo config is corrected to a compatible local single-binary config.

### 5) Grafana datasource schema warning fix
- Datasource files were renamed to avoid editor schema auto-matching collisions:
  - `grafana/datasources/prometheus-datasource.yml`
  - `grafana/datasources/loki-datasource.yml`
  - `grafana/datasources/tempo-datasource.yml`

## End-to-End Bring-Up Checklist

1. Start stack:
   ```sh
   docker compose -f observability/docker-compose.observability.yml up -d
   ```
2. Check containers:
   ```sh
   docker compose -f observability/docker-compose.observability.yml ps
   ```
3. Validate health endpoints:
   ```sh
   curl -i http://localhost:9090/-/ready
   curl -i http://localhost:3100/ready
   curl -i http://localhost:3200/ready
   ```
4. Open Grafana:
   - URL: `http://localhost:3000`
   - User: `admin`
   - Password: `admin`
5. Verify datasources in Grafana:
   - Connections → Data sources → **Save & test** for Prometheus, Loki, Tempo.
6. Open dashboards:
   - Dashboards → Browse → `EmploymentVC Overview`
   - Dashboards → Browse → `EmploymentVC Infra Health`

## Known URL Behavior
- `http://localhost:3100/` can return `404 page not found` even when Loki is healthy.
- `http://localhost:3200/` can return `404 page not found` even when Tempo is healthy.
- Use `/ready` endpoints for health validation.

## Troubleshooting Datasources
- Confirm containers are up:
   ```sh
   docker compose -f observability/docker-compose.observability.yml ps
   ```
- Validate service health endpoints (host-side):
   ```sh
   curl -s -o /dev/null -w "Loki /ready: %{http_code}\n" http://localhost:3100/ready
   curl -s -o /dev/null -w "Tempo /ready: %{http_code}\n" http://localhost:3200/ready
   curl -s -o /dev/null -w "Prometheus /-/ready: %{http_code}\n" http://localhost:9090/-/ready
   ```
- Re-load Grafana provisioning after datasource file changes:
   ```sh
   docker compose -f observability/docker-compose.observability.yml restart grafana
   ```
- In Grafana, go to **Connections → Data sources** and click **Save & test** for:
   - Prometheus (`http://prometheus:9090`)
   - Loki (`http://loki:3100`)
   - Tempo (`http://tempo:3200`)

Note: `http://localhost:3100/` and `http://localhost:3200/` can return `404 page not found`; this is expected. Use `/ready` (and Loki build info endpoint) for health checks.

## Production Notes
- Use persistent volumes for all data
- Secure Grafana and Prometheus with strong passwords and OAuth
- Integrate with cloud monitoring as needed

---
