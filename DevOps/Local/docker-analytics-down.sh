#!/bin/bash
echo "Stopping analytics Docker services (Airflow + Postgres)..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
for dir in Airflow Postgres; do
  if [ -f "$SCRIPT_DIR/$dir/docker-compose.yml" ]; then
    echo "  Stopping $dir..."
    docker-compose -f "$SCRIPT_DIR/$dir/docker-compose.yml" down
  fi
done
echo "Done."
