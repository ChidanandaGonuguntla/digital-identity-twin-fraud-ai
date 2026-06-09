#!/usr/bin/env bash
set -euo pipefail
export DTI_PUBLISH_ON_STARTUP=true
export DTI_DATASET_PATH=data/seed-known-good-10k.jsonl
export DTI_PUBLISH_RATE=${DTI_PUBLISH_RATE:-1000}
mvn spring-boot:run
