# Prometheus Configuration

- Place main config in `prometheus.yml`.
- Place alert rules in `rules/` (YAML format).
- Example rule: `service-alerts.yml` for service down alerts.

## Adding Targets
- Add service endpoints to `scrape_configs` in `prometheus.yml`.

## Adding Alert Rules
- Add YAML files to `rules/` for custom alerts.

---
