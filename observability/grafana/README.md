# Grafana Configuration

- Place custom dashboards in `dashboards/` (JSON format).
- Place datasource configs in `datasources/` (YAML format).
- Example datasources: Prometheus, Loki, Tempo.

## Adding Dashboards
- Export dashboards from Grafana UI as JSON and place them here.
- Example: `EmploymentVC-Overview.json` for service uptime, logs, traces.

## Adding Datasources
- Add YAML files for each datasource (see `prometheus-datasource.yml`, `loki-datasource.yml`, `tempo-datasource.yml`).

## Access Grafana
- URL: `http://localhost:3000`
- Username: `admin`
- Password: `admin`

## Validate Provisioned Datasources
1. Open Grafana → **Connections** → **Data sources**.
2. Open each datasource and click **Save & test**:
	- Prometheus (`http://prometheus:9090`)
	- Loki (`http://loki:3100`)
	- Tempo (`http://tempo:3200`)

## Notes
- Root URLs for Loki/Tempo on host (`/`) may return `404 page not found`.
- This is expected; health checks should use `/ready`.

---
