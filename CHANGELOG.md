# Changelog

All notable changes to EMI Structures are documented here.

## 0.1.0

- Added adaptive discovery of data-driven jigsaw structures from Minecraft, mods, and datapacks.
- Added one EMI recipe per structure, shared by recipe and usage lookups.
- Added internal material pagination to avoid hundreds of separate recipes.
- Added structure source, dimension, biome, and placement information.
- Added lightweight ten-pass material estimates with concise ranges and rare-material labels.
- Added processor-output discovery for materials that may not appear in every generated arrangement.
- Excluded container loot so it does not overlap with dedicated EMI loot addons.
- Added safeguards to keep analysis from blocking world startup on large structure sets.
