# EMI Structures

EMI Structures adds adaptive structure information to EMI. It discovers data-driven structures from the active game and datapacks, then shows where each structure generates and the materials it can contain.

Material amounts are presented as practical estimates based on multiple lightweight generation passes. Container loot is intentionally left to dedicated loot-viewing addons.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1 or newer
- EMI 1.1.24 or newer

## Using the mod

Use EMI's normal recipe lookup on a material to see structures that can generate it. Each structure is kept as one recipe with internal material pages, so both uses and recipes lead to the same entry.

## Version 0.1

This first release focuses on adaptive analysis of data-driven jigsaw structures, including structures added by mods and datapacks. Results are created when the integrated server starts, so the initial analysis time depends on the size of the installed structure set.

EMI Structures does not add or alter world generation.

## License

EMI Structures is licensed under GPL-3.0-only.
