#!/bin/bash
# Wait-for script - waits for a service to be available
# Usage: ./scripts/wait-for.sh <host> <port> [<timeout>]

set -e

HOST="$1"
PORT="$2"
TIMEOUT="${3:-60}"

echo "Waiting for $HOST:$PORT to be available..."

START_TIME=$(date +%s)
while true; do
    if nc -z "$HOST" "$PORT" 2>/dev/null; then
        echo "$HOST:$PORT is available!"
        exit 0
    fi
    
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))
    
    if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
        echo "Timeout waiting for $HOST:$PORT after ${TIMEOUT}s"
        exit 1
    fi
    
    echo "Waiting... ($ELAPSED/${TIMEOUT}s)"
    sleep 2
done