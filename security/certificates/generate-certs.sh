#!/usr/bin/env bash

# Certificate Generation Script for EmploymentVC
# Generates TLS and mTLS certificates for development and production

set -euo pipefail

CERT_DIR="${1:-.}"
DAYS_VALID="${2:-365}"
ENVIRONMENT="${3:-development}"

echo "Generating certificates for EmploymentVC ($ENVIRONMENT)"
echo "Certificate valid for $DAYS_VALID days"
echo "Saving to: $CERT_DIR"

mkdir -p "$CERT_DIR"/{ca,server,client,mtls}

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

# 1. Generate CA certificate and key
echo "Generating Root CA..."
openssl req -x509 -newkey rsa:4096 -sha256 -days $DAYS_VALID \
  -out "$CERT_DIR/ca/ca.crt" \
  -keyout "$CERT_DIR/ca/ca.key" \
  -nodes -subj "/C=US/ST=CA/L=SF/O=EmploymentVC/CN=EmploymentVC-CA"

echo -e "${GREEN}✓${NC} Root CA generated"

# 2. Generate Server Certificate (for API Gateway/Services)
echo "Generating Server Certificate..."
openssl req -new -newkey rsa:2048 -sha256 \
  -out "$CERT_DIR/server/server.csr" \
  -keyout "$CERT_DIR/server/server.key" \
  -nodes -subj "/C=US/ST=CA/L=SF/O=EmploymentVC/CN=*.provenly.local"

# Create server certificate extensions
cat > "$CERT_DIR/server/server.ext" << EOF
subjectAltName = DNS:*.provenly.local,DNS:localhost,DNS:*.local,IP:127.0.0.1
keyUsage = digitalSignature,keyEncipherment
extendedKeyUsage = serverAuth
EOF

# Sign server certificate
openssl x509 -req -in "$CERT_DIR/server/server.csr" \
  -CA "$CERT_DIR/ca/ca.crt" -CAkey "$CERT_DIR/ca/ca.key" \
  -out "$CERT_DIR/server/server.crt" \
  -days $DAYS_VALID -CAcreateserial \
  -extfile "$CERT_DIR/server/server.ext"

echo -e "${GREEN}✓${NC} Server Certificate generated"

# 3. Generate Client Certificate (for mTLS)
echo "Generating Client Certificate..."
openssl req -new -newkey rsa:2048 -sha256 \
  -out "$CERT_DIR/client/client.csr" \
  -keyout "$CERT_DIR/client/client.key" \
  -nodes -subj "/C=US/ST=CA/L=SF/O=EmploymentVC/CN=employmentvc-client"

# Create client certificate extensions
cat > "$CERT_DIR/client/client.ext" << EOF
keyUsage = digitalSignature,keyEncipherment
extendedKeyUsage = clientAuth
EOF

# Sign client certificate
openssl x509 -req -in "$CERT_DIR/client/client.csr" \
  -CA "$CERT_DIR/ca/ca.crt" -CAkey "$CERT_DIR/ca/ca.key" \
  -out "$CERT_DIR/client/client.crt" \
  -days $DAYS_VALID -CAcreateserial \
  -extfile "$CERT_DIR/client/client.ext"

echo -e "${GREEN}✓${NC} Client Certificate generated"

# 4. Generate Service mTLS Certificates
for service in auth-service wallet-api issuer-api verifier-api api-gateway; do
  echo "Generating $service mTLS certificate..."
  
  openssl req -new -newkey rsa:2048 -sha256 \
    -out "$CERT_DIR/mtls/${service}.csr" \
    -keyout "$CERT_DIR/mtls/${service}.key" \
    -nodes -subj "/C=US/ST=CA/L=SF/O=EmploymentVC/CN=${service}"
  
  # Create service certificate extensions
  cat > "$CERT_DIR/mtls/${service}.ext" << EOF
subjectAltName = DNS:${service},DNS:${service}.default,DNS:${service}.provenly
keyUsage = digitalSignature,keyEncipherment
extendedKeyUsage = serverAuth,clientAuth
EOF
  
  # Sign service certificate
  openssl x509 -req -in "$CERT_DIR/mtls/${service}.csr" \
    -CA "$CERT_DIR/ca/ca.crt" -CAkey "$CERT_DIR/ca/ca.key" \
    -out "$CERT_DIR/mtls/${service}.crt" \
    -days $DAYS_VALID -CAcreateserial \
    -extfile "$CERT_DIR/mtls/${service}.ext"
  
  echo -e "${GREEN}✓${NC} $service mTLS certificate generated"
done

# 5. Combine certificates for nginx/WAF
echo "Creating combined certificates for nginx..."
cat "$CERT_DIR/server/server.crt" "$CERT_DIR/ca/ca.crt" > "$CERT_DIR/server/chain.pem"
cp "$CERT_DIR/server/server.key" "$CERT_DIR/server/key.pem"
cp "$CERT_DIR/server/server.crt" "$CERT_DIR/server/cert.pem"

echo -e "${GREEN}✓${NC} Combined certificates created"

# 6. Create certificate bundle for client applications
echo "Creating certificate bundles..."
mkdir -p "$CERT_DIR/bundles"

# Bundle for client apps (CA + Client cert/key)
cat "$CERT_DIR/ca/ca.crt" "$CERT_DIR/client/client.crt" > "$CERT_DIR/bundles/client-bundle.crt"
cp "$CERT_DIR/client/client.key" "$CERT_DIR/bundles/client-bundle.key"

# Bundle for service-to-service (CA + all service certs)
cat "$CERT_DIR/ca/ca.crt" "$CERT_DIR/mtls"/*.crt > "$CERT_DIR/bundles/services-ca.crt"

echo -e "${GREEN}✓${NC} Certificate bundles created"

# 7. Show certificate details
echo ""
echo "================================"
echo "Certificate Summary"
echo "================================"
echo ""
echo "Root CA:"
openssl x509 -in "$CERT_DIR/ca/ca.crt" -noout -text | grep -E "Subject:|Issuer:|Not Before|Not After"

echo ""
echo "Server Certificate:"
openssl x509 -in "$CERT_DIR/server/server.crt" -noout -text | grep -E "Subject:|Not Before|Not After|DNS:"

echo ""
echo "Client Certificate:"
openssl x509 -in "$CERT_DIR/client/client.crt" -noout -text | grep -E "Subject:|Not Before|Not After"

echo ""
echo "================================"
echo -e "${GREEN}✓ All certificates generated successfully${NC}"
echo ""
echo "Next steps:"
echo "  1. Copy ca.crt to client applications and add to trust store"
echo "  2. For docker-compose: copy chain.pem, key.pem to infra/security/certs/"
echo "  3. For Kubernetes: create TLS secrets:"
echo "     kubectl create secret tls provenly-tls --cert=server.crt --key=server.key -n provenly"
echo "  4. For mTLS: mount service certificates into pods"
echo ""
