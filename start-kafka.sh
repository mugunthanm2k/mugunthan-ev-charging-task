#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Downloads and starts a local Kafka broker (no Docker needed).
# Run this once in a separate terminal before starting the Spring Boot app.
# ---------------------------------------------------------------------------
set -e

KAFKA_VERSION="3.6.1"
KAFKA_DIR="kafka_2.13-${KAFKA_VERSION}"
KAFKA_TGZ="${KAFKA_DIR}.tgz"
KAFKA_URL="https://downloads.apache.org/kafka/${KAFKA_VERSION}/${KAFKA_TGZ}"

# Download Kafka if not already present
if [ ! -d "$KAFKA_DIR" ]; then
  echo "Downloading Kafka ${KAFKA_VERSION}..."
  curl -O "$KAFKA_URL"
  tar -xzf "$KAFKA_TGZ"
  rm "$KAFKA_TGZ"
  echo "Kafka downloaded and extracted."
fi

cd "$KAFKA_DIR"

# Use KRaft mode (no ZooKeeper needed)
KAFKA_LOG_DIR="/tmp/csms-kafka-logs"
mkdir -p "$KAFKA_LOG_DIR"

echo "Formatting Kafka storage (KRaft mode)..."
CLUSTER_ID=$(bin/kafka-storage.sh random-uuid)
bin/kafka-storage.sh format -t "$CLUSTER_ID" -c config/kraft/server.properties

echo "Starting Kafka on localhost:9092..."
bin/kafka-server-start.sh config/kraft/server.properties \
  --override log.dirs="$KAFKA_LOG_DIR" \
  --override listeners=PLAINTEXT://localhost:9092 \
  --override advertised.listeners=PLAINTEXT://localhost:9092
