# Ownership and Versioning Rules

## Ownership

Every artifact in `shared/` must have an owner recorded in `shared/docs/artifact-catalog.json`.

- Ownership field format: team slug or owner alias (for example `team-ssi-platform`)
- Owner is responsible for review, compatibility notes, and rollout coordination

## Versioning

Artifact versions follow Semantic Versioning (`MAJOR.MINOR.PATCH`).

- `MAJOR`: incompatible/breaking change
- `MINOR`: backward-compatible extension
- `PATCH`: non-breaking correction/clarification

## Change Rules

When changing any artifact under:

- `shared/schemas/credentials/`
- `shared/schemas/contexts/`
- `shared/contracts/`

you must:

1. Update its version in `shared/docs/artifact-catalog.json`
2. Keep an owner entry for the artifact
3. Update ABI checksums in `shared/contracts/abi-checksums.sha256` for ABI changes

## Compatibility Expectations

- Consumer services should treat `MAJOR` bumps as migration events
- `MINOR` and `PATCH` updates should remain backward compatible
- Artifact removals are considered breaking and require explicit approval
