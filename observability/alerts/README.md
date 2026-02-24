# Alerting Configuration

Place Prometheus alert rules and Grafana alert configs in this directory.

- Use `prometheus/rules/` for Prometheus alert rules (YAML).
- Use this directory for Grafana alert notification configs.

Example Prometheus alert rule:
```yaml
# prometheus/rules/service-alerts.yml
groups:
  - name: employmentvc-service-alerts
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service is down"
          description: "{{ $labels.job }} is not responding."
```

## Example Alerting Integrations

### Prometheus Alertmanager
- Deploy Alertmanager alongside Prometheus for notification routing.
- Configure `alertmanager.yml` in `alerts/` for email, Slack, PagerDuty, etc.
- Reference Alertmanager in Prometheus config:
  ```yaml
  alerting:
    alertmanagers:
      - static_configs:
          - targets: ['alertmanager:9093']
  ```

### Grafana Alerts
- Use Grafana UI to create alerts on dashboards.
- Configure notification channels in Grafana (email, Slack, webhook).
- Export alert configs to `alerts/` for versioning.

### Example Alertmanager Config
```yaml
# alerts/alertmanager.yml
route:
  receiver: 'default'
receivers:
  - name: 'default'
    email_configs:
      - to: 'ops@example.com'
        from: 'grafana@example.com'
        smarthost: 'smtp.example.com:587'
        auth_username: 'grafana@example.com'
        auth_password: 'yourpassword'
        require_tls: true
```

---
