# Owner-edited collection, 18 October 2026 — part two

The owner supplied and authorized native Jugcraft implementation of this batch on 18 October 2026. The owner identifies the content as heavily edited work and says the remaining namespaces, names and tags are development/testing leftovers. This declaration is recorded as provenance; it is not a reason to strip notices or execute supplied binaries.

All **26,765 files** are preserved byte-identically in [the originals](../../originals/Edited-2026-10-18-Part2). The [SHA-256 manifest](files.json) and [intake audit](audit.json) distinguish preservation from gameplay implementation. The [content inventory](content.json) lists actual named content, models, animation clips, recipes, biomes, structures and overlapping existing item names.

| Collection | Files | Native integration work |
| --- | ---: | --- |
| cnb | 281 | Creature behavior, supplied models/animations, equipment and interactions |
| furniture | 5,915 | Furniture shapes, connections, storage, seats, appliances, menus and utilities |
| jimbologics | 1,566 | Ecology, animals, vegetation, wood families and boats |
| jims biomes | 7,404 | Biomes, terrain features, plants, woods, structures and generation compatibility |
| jims caves | 6,649 | Cave generation, creatures, hazards, machines, equipment and progression |
| jims mobs | 4,299 | Wildlife/monster behavior, model coverage, equipment and resource loops |
| jug_fauna | 90 | Wildlife, legacy format conversion and animal models |
| legendarycreatures | 362 | Supplied animated creatures, variants, combat, drops and summoning |
| mutations | 199 | Mutant encounters, combat, equipment and acquisition |

## Integration rules

- Use suitable owner-supplied art, models, animations, audio and data directly. Do not demand a redesign or repeat approval because a legacy namespace appears.
- Preserve originals and existing notices. Change runtime copies where the pinned Minecraft/Fabric formats require it.
- Implement native Jugcraft behavior using shared protection, inventory, machine, guide and networking systems. Do not load an old mod as a second gameplay implementation.
- Reconcile existing creatures, wood families, materials and furniture before creating duplicate IDs. Keep saved IDs stable.
- The 213 compiled biome classes are reference files, not executable dependencies. No supplied program or class was executed during intake. No Java/Kotlin source project was supplied in this batch.
- Missing behavior or geometry requires implementation; registering an icon is not a completed gameplay feature. Track each collection's actual implementation and validation status separately.
- Keep changes local for owner playtesting. Do not imply that import, compilation or asset inspection constitutes a completed playtest.

At intake, all nine collections were preserved and cataloged with integration pending. The October 19 native implementation now includes furniture, ecology/fauna, mutations, the larger creature roster, landscapes, caves and the nested goblin collection, alongside the previously implemented cnb, legendary and companion content. The combined roster contains 265 species. Runtime manifests record copies and compatibility changes; the originals remain unchanged.

The full server build passed 1,980 tests, followed by 19 focused regressions and combined client/armor/UI galleries. [The October 19 validation record](local-validation-oct19.json) records current evidence, the local artifact hash and remaining art/playtest scope. `local-validation.json` records the **older 106-species subset only**. See [the implementation record](../../../../docs/features/owner-menagerie-oct18.md#october-19-combined-native-implementation) for actual gameplay, native adaptations, validation and remaining visual review.
