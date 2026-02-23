#!/bin/bash
# Generate mTLS certificates for EmploymentVC microservices
set -e

CA_DIR="$(pwd)/infra/mtls"
DAYS=3650
PASSWORD=changeit

mkdir -p "$CA_DIR"
cd "$CA_DIR"

# 1. Root CA
openssl genrsa -out ca.key 4096
openssl req -x509 -new -key ca.key -sha256 -days $DAYS -out ca.crt -subj "/CN=EmploymentVC-RootCA"

# 2. For each service
for SERVICE in api-gateway auth-service credential-registry did-registry wallet-api; do
  openssl genrsa -out $SERVICE.key 2048
  openssl req -new -key $SERVICE.key -out $SERVICE.csr -subj "/CN=$SERVICE"
  openssl x509 -req -in $SERVICE.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out $SERVICE.crt -days 365 -sha256
  # Convert to PKCS12
  openssl pkcs12 -export -in $SERVICE.crt -inkey $SERVICE.key -out $SERVICE.p12 -name $SERVICE -CAfile ca.crt -caname root -password pass:$PASSWORD
  echo "Generated cert for $SERVICE"
done
# Trust store
openssl pkcs12 -export -in ca.crt -nokeys -out ca.p12 -name root -password pass:$PASSWORD

ls -l *.crt *.key *.p12
