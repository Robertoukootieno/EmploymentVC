#!/bin/bash
# Scan all built images for vulnerabilities using Trivy
# Usage: ./infra/docker/image-scan.sh [image1 image2 ...]

set -e

if ! command -v trivy &> /dev/null; then
  echo "Trivy is not installed. Install it from https://aquasecurity.github.io/trivy/v0.18.3/installation/"
  exit 1
fi

IMAGES=("$@")
if [ ${#IMAGES[@]} -eq 0 ]; then
  echo "No images specified. Scanning all local images."
  IMAGES=($(docker images --format '{{.Repository}}:{{.Tag}}'))
fi

for IMAGE in "${IMAGES[@]}"; do
  echo "Scanning $IMAGE ..."
  trivy image --severity HIGH,CRITICAL "$IMAGE"
done
