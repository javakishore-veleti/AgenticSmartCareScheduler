#!/bin/bash
# Start without Kafka — uses H2 in-memory DB and message broker Spring Boot.
# No Docker dependency required (H2 embedded).
# Optional: set POSTGRES=true if Postgres Docker is running.

export POSTGRES=${POSTGRES:-false}
export REDIS=${REDIS:-false}
export AIRFLOW=${AIRFLOW:-false}
export LOCAL_KAFKA=false

echo "=== Starting without Kafka (dockerless mode) ==="
echo "  POSTGRES=$POSTGRES"
echo "  REDIS=$REDIS"
echo "  AIRFLOW=$AIRFLOW"
echo "  LOCAL_KAFKA=$LOCAL_KAFKA — message broker Spring Boot handles pub/sub"
echo ""

# Step 1: Optionally start Docker services if flags are set
if [ "$POSTGRES" = "true" ] || [ "$AIRFLOW" = "true" ]; then
  echo ">>> Starting Docker containers (Postgres/Airflow if enabled)..."
  if [ "$POSTGRES" = "true" ] && [ -f DevOps/Local/Postgres/docker-compose.yml ]; then
    docker-compose -f DevOps/Local/Postgres/docker-compose.yml up -d
  fi
  if [ "$AIRFLOW" = "true" ] && [ -f DevOps/Local/Airflow/docker-compose.yml ]; then
    docker-compose -f DevOps/Local/Airflow/docker-compose.yml up -d
  fi
fi

# Step 2: Build UI
echo ">>> Building Angular portals..."
npm run build:ui

# Step 3: Maven build
echo ">>> Building Java modules..."
mvn install -DskipTests -q

# Step 4: Determine Spring profile
PROFILE="default"
if [ "$POSTGRES" = "true" ]; then
  PROFILE="local-postgres"
fi

# Step 5: Start Spring Boot app + message broker concurrently
echo ">>> Starting Spring Boot app (port 8080) + Message Broker (port 8081)..."
echo ">>> Spring profile: $PROFILE"

if [ "$PROFILE" = "default" ]; then
  npx concurrently "mvn -pl app-web spring-boot:run" "mvn -pl app-extensions/app-message-broker spring-boot:run"
else
  npx concurrently \
    "SPRING_PROFILES_ACTIVE=$PROFILE mvn -pl app-web spring-boot:run" \
    "SPRING_PROFILES_ACTIVE=$PROFILE mvn -pl app-extensions/app-message-broker spring-boot:run"
fi
