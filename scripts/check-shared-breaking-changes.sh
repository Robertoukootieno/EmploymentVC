#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

fail() {
  echo "[shared-gate] ERROR: $1" >&2
  exit 1
}

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  fail "This script must run inside a git repository"
fi

DEFAULT_REF="origin/main"
if ! git rev-parse --verify "$DEFAULT_REF" >/dev/null 2>&1; then
  git fetch origin main --depth=1 >/dev/null 2>&1 || true
fi

if ! git rev-parse --verify "$DEFAULT_REF" >/dev/null 2>&1; then
  if git rev-parse --verify main >/dev/null 2>&1; then
    DEFAULT_REF="main"
  else
    echo "[shared-gate] Skipping gate (no main reference available)."
    exit 0
  fi
fi

MERGE_BASE="$(git merge-base HEAD "$DEFAULT_REF")"

CHANGED_STATUS="$(git diff --name-status "$MERGE_BASE"...HEAD -- shared/schemas shared/contracts)"

if [[ -z "$CHANGED_STATUS" ]]; then
  echo "[shared-gate] No shared artifact changes detected."
  exit 0
fi

echo "[shared-gate] Shared artifact changes detected."

if [[ "${ALLOW_SHARED_BREAKING:-0}" != "1" ]]; then
  if echo "$CHANGED_STATUS" | awk '$1 == "D" {print}' | grep -Eq '^D\s+shared/(schemas|contracts)/'; then
    fail "Artifact deletion detected. Set ALLOW_SHARED_BREAKING=1 only for explicitly approved breaking changes."
  fi
fi

if ! git diff --name-only "$MERGE_BASE"...HEAD -- shared/docs/artifact-catalog.json | grep -q .; then
  fail "Artifact changes require updating shared/docs/artifact-catalog.json (version + ownership metadata)."
fi

if echo "$CHANGED_STATUS" | grep -Eq '^.\s+shared/contracts/.*\.abi\.json$'; then
  if ! git diff --name-only "$MERGE_BASE"...HEAD -- shared/contracts/abi-checksums.sha256 | grep -q .; then
    fail "ABI changes require updating shared/contracts/abi-checksums.sha256."
  fi
fi

echo "[shared-gate] Breaking-change gate passed."
