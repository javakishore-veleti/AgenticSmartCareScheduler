#!/bin/bash
echo "Starting all local Docker services..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
for dir in Postgres MongoDB Redis Kafka OpenSearch Prometheus Grafana Jaeger Kibana Temporal VectorDB Airflow; do
  if [ -f "$SCRIPT_DIR/$dir/docker-compose.yml" ]; then
    echo "  Starting $dir..."
    docker-compose -f "$SCRIPT_DIR/$dir/docker-compose.yml" up -d
  fi
done
echo "All services started."
