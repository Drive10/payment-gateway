#!/bin/bash
set -e

TLS_DIR="$(dirname "$0")/tls"
mkdir -p "$TLS_DIR"

PASSWORD="${REDIS_SSL_PASSWORD:-changeit}"

echo "Generating CA..."
openssl genrsa -out "$TLS_DIR/ca.key" 2048 2>/dev/null
openssl req -new -x509 -key "$TLS_DIR/ca.key" -out "$TLS_DIR/ca.crt" -days 365 \
    -subj "/C=US/ST=CA/L=SF/O=Redis/CA" 2>/dev/null

echo "Generating Redis server certificate..."
openssl genrsa -out "$TLS_DIR/redis.key" 2048 2>/dev/null
openssl req -new -key "$TLS_DIR/redis.key" -out "$TLS_DIR/redis.csr" \
    -subj "/CN=redis,O=Redis,L=SF,ST=CA,C=US" 2>/dev/null

cat > "$TLS_DIR/openssl.cnf" <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req

[req_distinguished_name]

[v3_req]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth, clientAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
DNS.2 = redis
IP.1 = 127.0.0.1
IP.2 = 0.0.0.0
EOF

openssl x509 -req -in "$TLS_DIR/redis.csr" -CA "$TLS_DIR/ca.crt" -CAkey "$TLS_DIR/ca.key" \
    -CAcreateserial -out "$TLS_DIR/redis.crt" -days 365 \
    -extensions v3_req -extfile "$TLS_DIR/openssl.cnf" 2>/dev/null

chmod 600 "$TLS_DIR/redis.key" "$TLS_DIR/ca.key"
chmod 644 "$TLS_DIR/redis.crt" "$TLS_DIR/ca.crt"

rm -f "$TLS_DIR/redis.csr" "$TLS_DIR/ca.key" "$TLS_DIR/openssl.cnf"

echo "TLS certificates generated in $TLS_DIR"
ls -la "$TLS_DIR"
