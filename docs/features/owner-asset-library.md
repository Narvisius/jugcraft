# Shared owner asset library

Status: shared library added; assets are not registered as runtime content.
Proposal: project owner request on 6 October 2026 to make the supplied collection accessible to every contributor and AI agent for reference, direct reuse or recoloring.
Owner: project owner; collection described by the owner as textures they created.
Target milestone and tier: contributor infrastructure, all tiers.
Primary specialty: every content branch, including buildings, ores, metals, machines, guns, vehicles, weapons, armor, sounds, animations, biomes, trees and farming.

## Contributor experience

Browse [the shared library](../../art/owner-library/README.md), [paginated file catalogs](../../art/owner-library/catalog/README.md) and [searchable CSV inventory](../../art/owner-library/catalog/files.csv). The collection includes 16,449 supplied files: 15,749 PNGs, 30 JPEG reference sheets, 275 Ogg sounds, 186 animation JSON files and 209 texture metadata sidecars, plus six attached preview sheets.

README.md, CONTRIBUTING.md, AGENTS.md, CLAUDE.md and ART_DIRECTION.md point contributors and AI agents to the library and record the owner's authorization for direct use, adaptation/recoloring or reference. Its scope is not limited to machinery.

## Connections and import contract

The owner-supplied files are the input; feature artwork, sounds and animations are the downstream consumers. Every technology tier, magic branch, equipment line, building set and biome may use suitable files. There is no survival unlock, balance change, dependency or required progression connection.

Keep source files in `art/owner-library/originals/Blocks/` intact. Copy selected files into a feature's runtime resources, adapt stable resource names and paths, preserve required animation sidecars, and register textures/audio/animation through the feature's supported formats. Record the source path or checksum and modifications in feature provenance. The catalog records dimensions so contributors can distinguish individual textures from reference sheets and packed atlases.

## Multiplayer and persistence

The library lives outside `src/main/resources/`; importing it does not add assets to the mod JAR, change runtime behavior, grow registries, modify saved worlds or add server work. No client/server gameplay or migration changes are made. Subsequent integrations require their own feature checks.

## Asset provenance

Source: project owner's supplied collection and six image attachments, 6 October 2026. The owner explicitly permits project contributors to use suitable files as reference, recolor/adapt them, or reuse them directly. Original filenames and relative folder structure are preserved. This records the owner's supplied provenance and authorization; filenames are not independent proof of authorship. Follow [LICENSE_POLICY.md](../../LICENSE_POLICY.md) and preserve any asset-specific attribution when integrating files. No license or permission changes are made for unrelated third-party content.

## Verification

All 16,449 original files and six preview attachments were copied byte-for-byte and matched by SHA-256. The CSV records a checksum for each original; [summary.json](../../art/owner-library/catalog/summary.json) records preview checksums and validation results. Images were verified with Pillow, animation JSON and `.mcmeta` were parsed, Ogg signatures were checked, and every `.mcmeta` was checked for an adjacent texture. No supplied-file validation issues were found.

`python scripts/check_repository.py` passed repository structure and local documentation link validation. Git whitespace checks passed for the documentation changes. No Java, recipes or runtime resources are changed; compilation and gameplay testing are not integration evidence for this library and have not been performed for it.

## Rollout

Publish as one focused art/documentation branch and pull request. The library is usable from the published branch; merge makes its contributor guidance part of the default branch. Future integrations can use their own branches and PRs. Removing the library later would affect contributor references, not saved game data.
