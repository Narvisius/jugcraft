# Jimtafacts owner collection — 23 October 2026

The owner supplied `Downloads/Magic Mods/Jimtafacts` and authorized integrating the collection into Jugcraft, explaining that old names remain from editing. Preserve suitable art/models/audio/data directly and adapt runtime copies to the pinned platform. Do not request the same per-file permission again. Follow the existing [provenance guidance](../../PROVENANCE.md) and retain applicable notices.

All **17,598 files / 65,070,712 bytes** are preserved under [originals/Oct23/Jimtafacts](../../originals/Oct23/Jimtafacts), with per-file SHA-256 verification in [manifest.json](manifest.json). No supplied classes, JARs or Lua programs have been executed. Compiled classes are static references for reconstructing native behavior and geometry. The embedded EJML dependency JAR stays local and is excluded from Git/runtime; its hash remains in the manifest.

| Collection | Files | Integration direction |
| --- | ---: | --- |
| Root accessories and spellbooks | 4,977 | Native Trinkets accessories; spellbooks, spells, equipment, enemies and structures through Concordance/Nouveau and shared protection |
| Applied Energentics | 5,086 | Stored-item network, terminals, cells, crafting and automation using existing inventory/energy APIs |
| BuildCraft | 3,930 | Pipes, pumps, transport, quarry/builders and robotics integrated with existing logistics/drilling |
| IndustrialCraft | 1,972 | Reuse machine families, materials and power; add missing processing, equipment and systems |
| Computers | 1,633 | Computers, terminals, peripherals and turtles; bounded execution, storage and permissions need a dedicated port |

[content.csv](content.csv) indexes 794 English item/block/entity labels and their current runtime status. This is a searchable label index, **not a complete registry census**: computed variants, spell registries and legacy `.lang` files require separate inspection. [audit.json](audit.json) records namespace/class-version counts, 14 strict-JSON parse issues in source files and the excluded dependency. Preserve source files unchanged; resolve their comments/format differences only in runtime copies.

Current native slice: 26 functional accessories and two reusable foods. [runtime-copies.json](runtime-copies.json) records byte-identical item texture copies. The remaining source content is preserved and pending implementation, not silently registered as inert items. Worn 3D geometry/animation recovery is separate from inventory textures.

[Existing-name overlaps](existing-name-overlaps.json) identifies six candidate matches with current Jugcraft items. Review their behavior and recipes before choosing shared registration; a matching name is not enough to alias items automatically.

Reproduce with `python tools/catalog_jimtafacts.py` and `python tools/jimtafacts.py` from the repository root. The canonical material generator invokes the latter after shared resources, retaining existing tags and translations. Implementation/testing status is in [the feature record](../../../../docs/features/jimtafacts-integration.md).

AI assistance: OpenAI Codex (GPT-6). No claim of Claude model use.
