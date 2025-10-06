#!/bin/bash
set -e

echo "Building Java application..."
./gradlew build

echo "Building Docker image and starting containers..."
docker compose up --build -d

echo "Containers running:"
docker compose ps
