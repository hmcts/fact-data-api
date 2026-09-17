#!/bin/bash
set -e

if [ ! -f .env ]; then
  echo "Missing .env file. Create one in the repository root using the README variable table."
  exit 1
fi

echo "Building Java application..."
./gradlew clean build

echo "Building Docker image and starting containers..."
docker compose --env-file .env up --build -d

echo "Containers running:"
docker compose ps
