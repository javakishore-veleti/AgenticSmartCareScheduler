#!/bin/bash
echo "Stopping all local Docker services..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
for dir in Airflow VectorDB Temporal Kibana Jaeger Grafana Prometheus OpenSearch Kafka Redis MongoDB Postgres; do
  if [ -f "$SCRIPT_DIR/$dir/docker-compose.yml" ]; then
    echo "  Stopping $dir..."
    docker-compose -f "$SCRIPT_DIR/$dir/docker-compose.yml" down
  fi
done
echo "All services stopped."
