#!/usr/bin/env bash
set -euo pipefail
BOOTSTRAP=${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}
TOPIC=${DTI_TRANSACTION_TOPIC:-dti.transaction-events}
kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists --topic "$TOPIC" --partitions 6 --replication-factor 1
kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic "$TOPIC"
