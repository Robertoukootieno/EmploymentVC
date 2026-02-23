# mTLS (Mutual TLS) for EmploymentVC Microservices

## Overview

mTLS ensures that all service-to-service communication is encrypted and both client and server authenticate each other using certificates.

## Steps

1. Generate a root CA
2. Generate server and client certificates signed by the CA
3. Distribute certs to services
4. Configure Spring Boot and nginx for mTLS

## 1. Generate Root CA

```bash
openssl genrsa -out ca.key 4096
openssl req -x509 -new -key ca.key -sha256 -days 3650 -out ca.crt -subj "/CN=EmploymentVC-RootCA"
```

## 2. Generate Service Certificates

Example for api-gateway:
```bash
openssl genrsa -out api-gateway.key 2048
openssl req -new -key api-gateway.key -out api-gateway.csr -subj "/CN=api-gateway"
openssl x509 -req -in api-gateway.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out api-gateway.crt -days 365 -sha256
```

Repeat for each service (auth-service, credential-registry, etc).

## 3. Distribute Certificates

- Place `ca.crt` in all services (as trust anchor)
- Place each service's `.crt` and `.key` in its container/VM

## 4. Spring Boot mTLS Configuration

In `application.yml`:
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:api-gateway.p12
    key-store-password: changeit
    key-store-type: PKCS12
    trust-store: classpath:ca.p12
    trust-store-password: changeit
    trust-store-type: PKCS12
    client-auth: need
```

Convert PEM to PKCS12:
```bash
openssl pkcs12 -export -in api-gateway.crt -inkey api-gateway.key -out api-gateway.p12 -name api-gateway -CAfile ca.crt -caname root -password pass:changeit
openssl pkcs12 -export -in ca.crt -nokeys -out ca.p12 -name root -password pass:changeit
```

## 5. nginx mTLS Configuration

In nginx.conf:
```
ssl_client_certificate /etc/nginx/ssl/ca.crt;
ssl_verify_client on;
```

## 6. Test mTLS

- Curl with client cert:
```bash
curl --cert client.crt --key client.key --cacert ca.crt https://localhost:443/api/health
```

## 7. Automate with scripts

See `infra/mtls/generate-mtls-certs.sh` for automation.
