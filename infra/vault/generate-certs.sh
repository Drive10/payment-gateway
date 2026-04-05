#!/bin/bash
set -e

TLS_DIR="$(dirname "$0")/tls"
mkdir -p "$TLS_DIR"

echo "Generating self-signed TLS certificates for Vault..."

openssl req -x509 -nodes -new -sha256 -days 365 \
    -newkey rsa:4096 \
    -keyout "$TLS_DIR/vault.key" \
    -out "$TLS_DIR/vault.crt" \
    -subj "/C=US/ST=CA/L=San Francisco/O=PaymentGateway/CN=vault" \
    2>/dev/null

echo "TLS certificates generated in $TLS_DIR"
echo "  - vault.crt (public certificate)"
echo "  - vault.key (private key)"

chmod 600 "$TLS_DIR/vault.key"
chmod 644 "$TLS_DIR/vault.crt"

echo "Done!"
