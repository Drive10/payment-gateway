#!/bin/bash
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 <base-url> [<base-url> ...]"
  exit 1
fi

for base_url in "$@"; do
  echo "[wait] $base_url/actuator/health"
  until curl -fsS "$base_url/actuator/health" >/dev/null; do
    sleep 2
  done
done

echo "[wait] all healthy"
