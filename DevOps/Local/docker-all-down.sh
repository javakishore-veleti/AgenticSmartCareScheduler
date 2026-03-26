#!/bin/bash
echo "Stopping local Docker services..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
for dir in Airflow Redis Postgres; do
  if [ -f "$SCRIPT_DIR/$dir/docker-compose.yml" ]; then
    echo "  Stopping $dir..."
    docker-compose -f "$SCRIPT_DIR/$dir/docker-compose.yml" down --volumes
  fi
done
echo "Done."
