#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SHARED_DIR="$ROOT_DIR/shared"
CATALOG_FILE="$SHARED_DIR/docs/artifact-catalog.json"
ABI_MANIFEST="$SHARED_DIR/contracts/abi-checksums.sha256"

fail() {
  echo "[shared-validation] ERROR: $1" >&2
  exit 1
}

echo "[shared-validation] Validating shared artifact registry..."

[[ -d "$SHARED_DIR/schemas" ]] || fail "Missing shared/schemas directory"
[[ -d "$SHARED_DIR/contracts" ]] || fail "Missing shared/contracts directory"
[[ -f "$CATALOG_FILE" ]] || fail "Missing catalog: shared/docs/artifact-catalog.json"

mapfile -t JSON_FILES < <(find "$SHARED_DIR/schemas" "$SHARED_DIR/contracts" -type f -name '*.json' | sort)
mapfile -t SCHEMA_FILES < <(find "$SHARED_DIR/schemas/credentials" -type f -name '*.json' 2>/dev/null | sort)
mapfile -t CONTEXT_FILES < <(find "$SHARED_DIR/schemas/contexts" -type f \( -name '*.json' -o -name '*.jsonld' \) 2>/dev/null | sort)
mapfile -t ABI_FILES < <(find "$SHARED_DIR/contracts" -type f -name '*.abi.json' | sort)

if [[ ${#JSON_FILES[@]} -gt 0 ]]; then
  for file in "${JSON_FILES[@]}"; do
    python3 - <<'PY' "$file"
import json, pathlib, sys
path = pathlib.Path(sys.argv[1])
with path.open('r', encoding='utf-8') as f:
    json.load(f)
PY
  done
fi

if [[ ${#SCHEMA_FILES[@]} -gt 0 ]]; then
  for file in "${SCHEMA_FILES[@]}"; do
    python3 - <<'PY' "$file"
import json, pathlib, sys
path = pathlib.Path(sys.argv[1])
with path.open('r', encoding='utf-8') as f:
    data = json.load(f)
if not isinstance(data, dict):
    raise SystemExit(f"Schema must be JSON object: {path}")
for key in ("type", "properties", "required"):
    if key not in data:
        raise SystemExit(f"Schema missing '{key}': {path}")
if not isinstance(data.get("properties"), dict):
    raise SystemExit(f"Schema properties must be object: {path}")
if not isinstance(data.get("required"), list):
    raise SystemExit(f"Schema required must be array: {path}")
PY
  done
fi

if [[ ${#CONTEXT_FILES[@]} -gt 0 ]]; then
  for file in "${CONTEXT_FILES[@]}"; do
    python3 - <<'PY' "$file"
import json, pathlib, sys
path = pathlib.Path(sys.argv[1])
with path.open('r', encoding='utf-8') as f:
    data = json.load(f)
if not isinstance(data, dict):
    raise SystemExit(f"Context must be JSON object: {path}")
if "@context" not in data:
    raise SystemExit(f"Context missing '@context': {path}")
PY
  done
fi

python3 - <<'PY' "$CATALOG_FILE" "$ROOT_DIR"
import json, pathlib, re, sys

catalog_path = pathlib.Path(sys.argv[1])
root = pathlib.Path(sys.argv[2])
semver = re.compile(r"^\d+\.\d+\.\d+$")

with catalog_path.open('r', encoding='utf-8') as f:
    catalog = json.load(f)

items = catalog.get("artifacts")
if not isinstance(items, list):
    raise SystemExit("Catalog must contain an 'artifacts' array")

catalog_paths = set()
for item in items:
    if not isinstance(item, dict):
        raise SystemExit("Each artifact catalog entry must be an object")
    for field in ("path", "owner", "version", "type"):
        if field not in item or not str(item[field]).strip():
            raise SystemExit(f"Catalog entry missing field '{field}': {item}")
    rel = item["path"]
    if not rel.startswith("shared/"):
        raise SystemExit(f"Catalog path must start with 'shared/': {rel}")
    if not semver.match(item["version"]):
        raise SystemExit(f"Catalog version must be SemVer MAJOR.MINOR.PATCH: {rel} -> {item['version']}")
    if rel in catalog_paths:
        raise SystemExit(f"Duplicate catalog path: {rel}")
    catalog_paths.add(rel)
    if not (root / rel).exists():
        raise SystemExit(f"Catalog points to missing artifact file: {rel}")

artifact_paths = []
for directory, pattern in (
    (root / "shared/schemas/credentials", "*.json"),
    (root / "shared/schemas/contexts", "*.json"),
    (root / "shared/schemas/contexts", "*.jsonld"),
    (root / "shared/contracts", "*.abi.json"),
):
    if directory.exists():
        artifact_paths.extend(path.relative_to(root).as_posix() for path in directory.rglob(pattern))

for artifact in sorted(set(artifact_paths)):
    if artifact not in catalog_paths:
        raise SystemExit(f"Artifact missing from catalog: {artifact}")
PY

if [[ ${#ABI_FILES[@]} -gt 0 ]]; then
  [[ -f "$ABI_MANIFEST" ]] || fail "ABI files exist, but checksum manifest is missing"

  mapfile -t ABI_REL_PATHS < <(
    for file in "${ABI_FILES[@]}"; do
      realpath --relative-to="$SHARED_DIR/contracts" "$file"
    done | sort
  )

  mapfile -t MANIFEST_REL_PATHS < <(
    grep -Ev '^\s*#|^\s*$' "$ABI_MANIFEST" | awk '{print $2}' | sed 's#^\./##' | sort
  )

  if [[ "${ABI_REL_PATHS[*]}" != "${MANIFEST_REL_PATHS[*]}" ]]; then
    fail "ABI checksum manifest entries must exactly match ABI files in shared/contracts"
  fi

  (
    cd "$SHARED_DIR/contracts"
    sha256sum -c "$(basename "$ABI_MANIFEST")"
  )
fi

echo "[shared-validation] All checks passed."
