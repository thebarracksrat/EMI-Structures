# EMI Structures

Published Silent Vector Studios EMI addon for adaptive structure acquisition and recipe information. This repository is independent of the Architects mod family.

## Map

- `common/` — shared analysis, mixins, EMI plugin/recipes/stacks, and assets.
- `fabric/` — Fabric initializer, server lifecycle events, and `fabric.mod.json`.
- `forge/` — Forge `@Mod` entry, events, and `mods.toml`.
- `art/` and `design/` — project-owned visual/design material.
- `docs/` — durable project documentation.
- `releases/` — deliberately retained release records tracked by the project.
- `build.gradle`, `gradle.properties`, `settings.gradle`, `gradle/`, `gradlew*` — build definition and wrapper.

## Versioning

Gradle properties in `gradle.properties`:

- `mod_version` — semver (currently `0.1.1`)
- `minecraft_version` — game target (currently `1.20.1`)
- `loader` — Forge jar suffix only (`forge`; not a separate semver channel). The Fabric jar suffix is `fabric`.

Published version name: `${mod_version}+${minecraft_version}` (example: `0.1.1+1.20.1`)
Forge jar: `emi-structures-${mod_version}+${minecraft_version}-forge.jar`
Fabric jar: `emi-structures-${mod_version}+${minecraft_version}-fabric.jar`

## Commands

- Build and test on Windows: `.\gradlew.bat build`
- Forge only: `.\gradlew.bat :forge:build`
- Fabric only: `.\gradlew.bat :fabric:build`
- Tests only: `.\gradlew.bat test`

## Rules

- Preserve Git history, public IDs, adaptive discovery behavior, compatibility boundaries, licenses, and notices.
- Prefer canonical files; do not create duplicate CLEAN/FINAL documents.
- Do not commit `build/`, `run/`, `.gradle/`, logs, caches, downloaded mods, or scratch/audit output.
- Keep changes narrow and search for existing implementations first.
- Do not publish, tag, push, or release without explicit authorization.
