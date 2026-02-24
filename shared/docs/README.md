# Shared Artifact Registry

This directory defines the governance model for root-level `shared/` artifacts.

## Scope

The root `shared/` directory is an **artifact input layer only**. It must not contain executable service code.

- Shared executable Java/Kotlin logic stays in `backend-libraries/`
- Shared runtime services stay in `backend-services/`
- Shared contract/schemas/contexts artifacts stay in `shared/`

## Artifact Types

- `shared/schemas/credentials/*.json` - VC JSON Schemas
- `shared/schemas/contexts/*.{json,jsonld}` - JSON-LD contexts
- `shared/contracts/**/*.abi.json` - Smart contract ABIs

## Governance Files

- `shared/docs/OWNERSHIP_AND_VERSIONING.md` - ownership and SemVer policy
- `shared/docs/artifact-catalog.json` - machine-readable artifact metadata
- `shared/contracts/abi-checksums.sha256` - ABI integrity manifest

## CI Gates

CI enforces:

1. JSON syntax and schema/context structure validation
2. Artifact ownership + SemVer metadata checks
3. ABI checksum validation (when ABI files exist)
4. Breaking-change gate requiring catalog updates for artifact changes

Run locally:

```bash
bash scripts/validate-shared-artifacts.sh
bash scripts/check-shared-breaking-changes.sh
```
