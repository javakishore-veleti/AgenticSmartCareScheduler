#!/bin/bash
echo "Starting local Docker services (Postgres, Redis, Airflow)..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
for dir in Postgres Redis Airflow; do
  if [ -f "$SCRIPT_DIR/$dir/docker-compose.yml" ]; then
    echo "  Starting $dir..."
    docker-compose -f "$SCRIPT_DIR/$dir/docker-compose.yml" up -d
  else
    echo "  Skipping $dir (no docker-compose.yml yet)"
  fi
done
echo "Done."
