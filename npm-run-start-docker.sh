#!/bin/bash
# Start with full Docker infrastructure.
# All flags default to true. Override with: POSTGRES=false ./npm-run-start-docker.sh

export POSTGRES=${POSTGRES:-true}
export REDIS=${REDIS:-true}
export AIRFLOW=${AIRFLOW:-true}
export LOCAL_KAFKA=${LOCAL_KAFKA:-true}

echo "=== Starting with Docker infrastructure ==="
echo "  POSTGRES=$POSTGRES       (port 5432)"
echo "  REDIS=$REDIS          (port 6379)"
echo "  AIRFLOW=$AIRFLOW       (port 8082)"
echo "  LOCAL_KAFKA=$LOCAL_KAFKA  (port 9092)"
echo ""

# Step 1: Start Docker services
echo ">>> Starting Docker containers..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
for dir in Postgres Redis Airflow; do
  FLAG_VAR=$(echo "$dir" | tr '[:lower:]' '[:upper:]')
  case "$dir" in
    Postgres) FLAG_VAL=$POSTGRES ;;
    Redis)    FLAG_VAL=$REDIS ;;
    Airflow)  FLAG_VAL=$AIRFLOW ;;
  esac
  if [ "$FLAG_VAL" = "true" ] && [ -f "DevOps/Local/$dir/docker-compose.yml" ]; then
    echo "  Starting $dir..."
    docker-compose -f "DevOps/Local/$dir/docker-compose.yml" up -d
  fi
done
# TODO: Add Kafka docker-compose when available
if [ "$LOCAL_KAFKA" = "true" ]; then
  echo "  Kafka: docker-compose not yet configured (placeholder)"
fi

# Step 2: Build UI
echo ">>> Building Angular portals..."
npm run build:ui

# Step 3: Maven build
echo ">>> Building Java modules..."
mvn install -DskipTests -q

# Step 4: Compose Spring profiles from flags
PROFILES=""
[ "$POSTGRES" = "true" ] && PROFILES="${PROFILES}local-postgres,"
[ "$REDIS" = "true" ] && PROFILES="${PROFILES}local-redis,"
[ "$LOCAL_KAFKA" = "true" ] && PROFILES="${PROFILES}local-kafka,"
PROFILES=${PROFILES%,}  # trim trailing comma

echo ">>> Spring profiles: ${PROFILES:-default}"
echo ""

# Step 5: Start app (no message broker when Kafka is active)
if [ "$LOCAL_KAFKA" = "true" ]; then
  echo ">>> Starting Spring Boot app only (port 8080)"
  echo ">>> Message broker skipped — LOCAL_KAFKA=true, Kafka handles pub/sub"
  SPRING_PROFILES_ACTIVE=$PROFILES mvn -pl app-web spring-boot:run
else
  echo ">>> Starting Spring Boot app (port 8080) + Message Broker (port 8081)"
  BROKER_PROFILES=""
  [ "$POSTGRES" = "true" ] && BROKER_PROFILES="local-postgres"
  npx concurrently \
    "SPRING_PROFILES_ACTIVE=$PROFILES mvn -pl app-web spring-boot:run" \
    "SPRING_PROFILES_ACTIVE=$BROKER_PROFILES mvn -pl app-extensions/app-message-broker spring-boot:run"
fi
