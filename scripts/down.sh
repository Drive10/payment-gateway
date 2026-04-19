#!/bin/bash
# PayFlow Down Helper
# Usage: ./scripts/down.sh

set -e

echo "Stopping PayFlow..."
docker compose down "$@"
echo "Done!"