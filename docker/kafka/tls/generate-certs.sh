#!/bin/bash
set -e

TLS_DIR="$(dirname "$0")/tls"
mkdir -p "$TLS_DIR"

PASSWORD="${KAFKA_SSL_PASSWORD:-changeit}"
KEYSTORE="$TLS_DIR/kafka.keystore.jks"
TRUSTSTORE="$TLS_DIR/kafka.truststore.jks"
CA_CERT="$TLS_DIR/ca.crt"
CA_KEY="$TLS_DIR/ca.key"
CA_CONFIG="$TLS_DIR/ca-config.cnf"

echo "Generating CA..."
openssl genrsa -out "$CA_KEY" 2048 2>/dev/null
openssl req -new -x509 -key "$CA_KEY" -out "$CA_CERT" -days 365 \
    -subj "/C=US/ST=CA/L=SF/O=Kafka/CA" 2>/dev/null

echo "Creating truststore..."
keytool -importcert -alias CARoot -file "$CA_CERT" -keystore "$TRUSTSTORE" \
    -storepass "$PASSWORD" -noprompt 2>/dev/null

echo "Generating Kafka broker keystore..."
keytool -genkey -alias kafka -keyalg RSA -keystore "$KEYSTORE" \
    -storepass "$PASSWORD" -keypass "$PASSWORD" \
    -dname "CN=kafka,O=Kafka,L=SF,ST=CA,C=US" 2>/dev/null

echo "Signing certificate..."
openssl req -new -out "$TLS_DIR/kafka.csr" -key "$TLS_DIR/kafka.key" \
    -subj "/CN=kafka,O=Kafka,L=SF,ST=CA,C=US" 2>/dev/null

cat > "$CA_CONFIG" <<EOF
[distinguished_name]
countryName = US
stateOrProvinceName = CA
localityName = San Francisco
organizationName = Kafka

[req]
distinguished_name = distinguished_name

[ext]
basicConstraints=CA:FALSE
keyUsage=nonRepudiation,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth,clientAuth
EOF

openssl x509 -req -in "$TLS_DIR/kafka.csr" -CA "$CA_CERT" -CAkey "$CA_KEY" \
    -CAcreateserial -out "$TLS_DIR/kafka.crt" -days 365 \
    -extfile "$CA_CONFIG" -extensions ext 2>/dev/null

keytool -importcert -alias kafka -file "$TLS_DIR/kafka.crt" -keystore "$KEYSTORE" \
    -storepass "$PASSWORD" -keypass "$PASSWORD" 2>/dev/null

echo "Adding CA to truststore..."
keytool -importcert -alias CARoot -file "$CA_CERT" -keystore "$TRUSTSTORE" \
    -storepass "$PASSWORD" -noprompt 2>/dev/null

chmod 600 "$KEYSTORE" "$CA_KEY"
chmod 644 "$TRUSTSTORE" "$CA_CERT"

rm -f "$TLS_DIR/kafka.csr" "$TLS_DIR/kafka.key" "$CA_KEY"

echo "TLS certificates generated in $TLS_DIR"
ls -la "$TLS_DIR"
