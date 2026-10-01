# EMI Structures

Published Silent Vector Studios EMI addon for adaptive structure acquisition and recipe information. This repository is independent of the Architects mod family.

## Map

- `common/` — shared analysis, mixins, EMI plugin/recipes/stacks, and assets.
- `neoforge/` — NeoForge `@Mod` entry, events, and `neoforge.mods.toml`.
- `art/` and `design/` — project-owned visual/design material.
- `docs/` — durable project documentation.
- `releases/` — deliberately retained release records tracked by the project.
- `build.gradle`, `gradle.properties`, `settings.gradle`, `gradle/`, `gradlew*` — build definition and wrapper.

## Versioning

Gradle properties in `gradle.properties`:

- `mod_version` — semver (currently `0.1.1`)
- `minecraft_version` — game target (currently `1.21.1`)
- `loader` — jar suffix only (`neoforge`; not a separate semver channel)

Published version name: `${mod_version}+${minecraft_version}` (example: `0.1.1+1.21.1`)  
NeoForge jar: `emi-structures-${mod_version}+${minecraft_version}-${loader}.jar`

## Commands

- Build and test on Windows: `.\gradlew.bat build`
- NeoForge only: `.\gradlew.bat :neoforge:build`
- Tests only: `.\gradlew.bat test`

## Rules

- Preserve Git history, public IDs, adaptive discovery behavior, compatibility boundaries, licenses, and notices.
- Prefer canonical files; do not create duplicate CLEAN/FINAL documents.
- Do not commit `build/`, `run/`, `.gradle/`, logs, caches, downloaded mods, or scratch/audit output.
- Keep changes narrow and search for existing implementations first.
- Do not publish, tag, push, or release without explicit authorization.
