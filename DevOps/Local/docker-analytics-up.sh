#!/bin/bash
echo "Starting analytics Docker services (Postgres + Airflow)..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
for dir in Postgres Airflow; do
  if [ -f "$SCRIPT_DIR/$dir/docker-compose.yml" ]; then
    echo "  Starting $dir..."
    docker-compose -f "$SCRIPT_DIR/$dir/docker-compose.yml" up -d
  else
    echo "  Skipping $dir (no docker-compose.yml yet)"
  fi
done
echo ""
echo "Services:"
echo "  Postgres:          localhost:5432  (smartcare/smartcare)"
echo "  Airflow Webserver: localhost:8082  (admin/admin)"
echo "  Airflow Postgres:  localhost:5433  (airflow/airflow)"
echo ""
echo "Done."
