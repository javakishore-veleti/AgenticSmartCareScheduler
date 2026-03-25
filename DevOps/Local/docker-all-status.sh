#!/bin/bash
echo "Local Docker services status:"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
for dir in Postgres MongoDB Redis Kafka OpenSearch Prometheus Grafana Jaeger Kibana Temporal VectorDB Airflow; do
  if [ -f "$SCRIPT_DIR/$dir/docker-compose.yml" ]; then
    echo "--- $dir ---"
    docker-compose -f "$SCRIPT_DIR/$dir/docker-compose.yml" ps
  fi
done
