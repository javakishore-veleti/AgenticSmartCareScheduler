#!/bin/bash
# Start without Kafka — H2 in-memory + message broker Spring Boot.
# Optionally enable Postgres/Airflow if their Docker containers are running.

export POSTGRES=${POSTGRES:-false}
export REDIS=${REDIS:-false}
export AIRFLOW=${AIRFLOW:-false}
export LOCAL_KAFKA=false

echo "=== Starting dockerless mode (no Kafka) ==="
echo "  POSTGRES=$POSTGRES"
echo "  REDIS=$REDIS"
echo "  AIRFLOW=$AIRFLOW"
echo "  LOCAL_KAFKA=$LOCAL_KAFKA — message broker Spring Boot handles pub/sub"
echo ""

# Step 1: Optionally start Docker services
if [ "$POSTGRES" = "true" ] && [ -f DevOps/Local/Postgres/docker-compose.yml ]; then
  echo "  Starting Postgres..."
  docker-compose -f DevOps/Local/Postgres/docker-compose.yml up -d
fi
if [ "$AIRFLOW" = "true" ] && [ -f DevOps/Local/Airflow/docker-compose.yml ]; then
  echo "  Starting Airflow..."
  docker-compose -f DevOps/Local/Airflow/docker-compose.yml up -d
fi

# Step 2: Build UI
echo ">>> Building Angular portals..."
npm run build:ui

# Step 3: Maven build
echo ">>> Building Java modules..."
mvn install -DskipTests -q

# Step 4: Compose Spring profiles
PROFILES=""
[ "$POSTGRES" = "true" ] && PROFILES="${PROFILES}local-postgres,"
[ "$REDIS" = "true" ] && PROFILES="${PROFILES}local-redis,"
PROFILES=${PROFILES%,}

BROKER_PROFILES=""
[ "$POSTGRES" = "true" ] && BROKER_PROFILES="local-postgres"

echo ">>> Spring profiles: ${PROFILES:-default}"
echo ">>> Broker profiles: ${BROKER_PROFILES:-default}"
echo ""

# Step 5: Start app + message broker
echo ">>> Starting Spring Boot app (port 8080) + Message Broker (port 8081)"
if [ -z "$PROFILES" ]; then
  npx concurrently \
    "mvn -pl app-web spring-boot:run" \
    "mvn -pl app-extensions/app-message-broker spring-boot:run"
else
  npx concurrently \
    "SPRING_PROFILES_ACTIVE=$PROFILES mvn -pl app-web spring-boot:run" \
    "SPRING_PROFILES_ACTIVE=$BROKER_PROFILES mvn -pl app-extensions/app-message-broker spring-boot:run"
fi
