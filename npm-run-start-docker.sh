#!/bin/bash
# Start with full Docker infrastructure: Postgres, Redis, Airflow, Kafka
# When LOCAL_KAFKA=true, message broker Spring Boot is not needed —
# all publish/consume goes through Kafka.

export POSTGRES=true
export REDIS=true
export AIRFLOW=true
export LOCAL_KAFKA=true

echo "=== Starting with Docker infrastructure ==="
echo "  POSTGRES=$POSTGRES  (port 5432)"
echo "  REDIS=$REDIS     (port 6379)"
echo "  AIRFLOW=$AIRFLOW  (port 8082)"
echo "  LOCAL_KAFKA=$LOCAL_KAFKA (port 9092) — message broker Spring Boot skipped"
echo ""

# Step 1: Start Docker services
echo ">>> Starting Docker containers..."
bash DevOps/Local/docker-all-up.sh

# Step 2: Build UI
echo ">>> Building Angular portals..."
npm run build:ui

# Step 3: Maven build
echo ">>> Building Java modules..."
mvn install -DskipTests -q

# Step 4: Start Spring Boot app only (no message broker — Kafka handles messaging)
echo ">>> Starting Spring Boot app (port 8080)..."
echo ">>> Message broker skipped — LOCAL_KAFKA=true, using Kafka for pub/sub"
SPRING_PROFILES_ACTIVE=local-postgres mvn -pl app-web spring-boot:run
