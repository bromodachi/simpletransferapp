#!/usr/bin/env bash
set -x
set -eo pipefail

# Build the app
./gradlew build

# Now finally, start compose
docker-compose up
