# Phase 3: Vault, ELK, and mTLS Deployment Guide

## 1. HashiCorp Vault (Secrets Management)
- **Files:**
  - infra/vault/vault-config.hcl
  - infra/vault/docker-compose.vault.yml
- **How to deploy:**
  ```bash
  cd infra/vault
  # (optional) set project name in environment
  # export COMPOSE_PROJECT_NAME=employmentvc-vault
  COMPOSE_PROJECT_NAME=employmentvc-vault docker compose -f docker-compose.vault.yml up -d
  # Access UI: http://localhost:8200 (token: root)
  ```
- **Integrate with Spring Boot:**
  - Add dependency: `spring-cloud-starter-vault-config`
  - Example config:
    ```yaml
    spring:
      cloud:
        vault:
          uri: http://vault:8200
          token: root
          kv:
            enabled: true
            backend: secret
    ```
  - Store secrets:
    ```bash
    vault kv put secret/db username=myuser password=mypassword
    ```

## 2. ELK Stack (Centralized Logging)
- **Files:**
  - infra/elk/docker-compose.elk.yml
  - infra/elk/logstash.conf
- **How to deploy:**
  ```bash
  cd infra/elk
  cp .env.example .env
  COMPOSE_PROJECT_NAME=employmentvc-elk docker compose -f docker-compose.elk.yml up -d
  # Access Kibana: http://localhost:5601
  ```
- **Log shipping:**
  - Use Filebeat or Logback TCP appender to send logs to Logstash (port 5044/5000)
  - Example Logback config:
    ```xml
    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
      <destination>logstash:5000</destination>
      <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    <root level="INFO">
      <appender-ref ref="LOGSTASH"/>
    </root>
    ```

## 3. mTLS (Mutual TLS) Between Services
- **Files:**
  - infra/mtls/README.md
  - infra/mtls/generate-mtls-certs.sh
- **How to use:**
  ```bash
  cd infra/mtls
  ./generate-mtls-certs.sh
  # Distribute .crt, .key, .p12 files to each service
  ```
- **Spring Boot config:**
  See README.md for application.yml example
- **nginx config:**
  Add:
  ```nginx
  ssl_client_certificate /etc/nginx/ssl/ca.crt;
  ssl_verify_client on;
  ```

## 4. Testing & Monitoring
- Vault: Store and retrieve secrets via UI or API
- ELK: Send logs, search in Kibana
- mTLS: Curl with client cert, check service-to-service calls

## 5. Next Steps
- Integrate Vault with all microservices for DB/API secrets
- Ship all logs to ELK for search/alerting
- Enforce mTLS in all internal service calls

---

**All infrastructure and scripts are ready. See each subfolder for details and runbooks.**
