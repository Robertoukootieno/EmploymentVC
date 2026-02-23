#!/bin/bash
# Automated cleanup for old Docker images, containers, and volumes
# Usage: ./infra/docker/cleanup.sh

echo "Pruning unused Docker objects..."
docker system prune -af --volumes

echo "Cleanup complete."
