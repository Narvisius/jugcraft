# Candy Cavity generation repair

Owner-requested October 21 local release-batch work by **OpenAI Codex (GPT-6)**.
Not deployed to the hosted server or promoted through the player updater.

## Problem and implementation

The original import used `CavernChamber` for all six caves. Candy therefore had a
nearly flat chocolate chamber instead of layered cake plateaus. Lollipops became
generic mushroom-shaped features, Licoroot used a leafy tree implementation, and
gobstoppers became small single-color rocks. Several placement files searched
between bedrock and Y=256; the tower/town/arch/bottle all used fixed Y=-36 starts.
The sideways bottle and all nine supplied lollipop tops were available but unused.
Purple/orange concrete in the bottle was a processor marker, not finished liquid.
Cake, chocolate, Licoroot and candy-cane pillars also inherited an empty furniture
shape and became two-pixel collision slabs. They now use native solid, opaque
rotated pillars with an upright default; existing serialized `axis=x/y/z` values
remain compatible. This fixes ground collision, texture orientation and internal
face culling without changing any PNGs.

`CandyCavity` replaces only the Candy chamber configuration. It creates seeded
terraces of cake layers/chocolate with frosted chocolate tops, rock-candy seams,
continuous purple-soda channels, frosted dough hoops, layered gobstoppers with
exposed interiors, curled Licoroot branches/vines and candy canes. Original
lollipop templates supply their geometry; their grayscale palette markers receive
six candy color combinations. The original sideways sugar-glass bottle has its
liquid markers resolved to real soda, with its open mouth meeting a river.

Terrain and large decorations write only the current chunk. Neighbouring chunks
reconstruct the same coordinate-seeded shapes, avoiding dependence on decoration
order. A per-placement biome resolver/cache samples coordinates without requesting
unavailable neighbouring chunks; the first rendered generation test caught and
removed an unsafe chunk-backed footprint lookup. The terrain pass runs before structures. No tick-time terrain rewriting or
active-world singleton is introduced. Smaller existing candy vegetation, chocolate
ruins, cookie shelves and ice-cream details retain their feature IDs but now search
inside the cavern; ice-cream flavors use their matching blocks.

Licowitch Towers and Gingerbread Towns remain registered, locateable structures.
`CandyLandmark` checks the biome footprint, avoids rivers, chooses height from the
new floor, makes a solid foundation and clears the template's occupied volume.
Saved native pieces retain their template positions. Existing template chest/loot
data is preserved. Original jigsaw pools/definitions remain available for older
saved pieces; obsolete automatic cake/arch/bottle starts are removed because those
shapes are now part of the terrain pass. The town still uses the existing assembled
ten-house template; this change does not port additional witch/gingerbread AI.

## Supplied sources and adaptation

- Preserved source collection: `art/owner-library/originals/Edited-2026-10-18-Part2/jims caves/`.
  Runtime `cavern_*` templates retain the existing namespace/schema conversion.
- Additional class data from the owner's `Downloads/Magic Mods/jims caves/` is
  already preserved in the October 19 creature archives; see the
  [source manifest](../../art/owner-library/catalog/creature-recovery-2026-10-19/source-manifest.json).
  Static CFR inspection covered `CakeCaveStructurePiece`, `DonutArchStructurePiece`,
  `SodaBottleStructurePiece`, `SodaBottleProcessor`, `LollipopFeature`,
  `LicorootTreeFeature` and `GobstopperGeodeFeature` in the supplied
  `com/github/alexmodguy/alexscaves/server/level` tree. No supplied program was run.
- Native terrain uses new seeded coordinate sampling and current Jugcraft biome
  selection rather than executing/loading the old Forge structures. The intended
  visual forms and supplied template geometry are retained; this is not a claim
  of bit-for-bit reproduction of the old mod's world generator.
- No replacement PNGs, sounds, external dependencies or platform upgrades.
  Source names/notices remain preserved in the owner library.
- Reproduce resources with `python tools/candy_cavity_content.py`; both broad
  cave and landmark importers also call this finalization step.

## Validation and rollout

The combined local build passed all 2,244 required server game tests (including
the new viewer-server tools), and `CandyCavityClientGameTests` passed in actual
rendered fixed-Candy and normal Overworld worlds. All 187 Python tests passed;
repository/document links and targeted worldgen, tags and delivery audits passed.
The tower pot schema warnings were resolved. Screenshots were inspected and
peppermint density reduced before the final run.

`CandyCavityGameTests` covers several seeds, continuous
supported soda channels including negative/chunk-border coordinates, terrain
bounds/materials, template loading and native landmark piece serialization.
`CandyCavityClientGameTests` generates a disposable normal-noise world with a fixed
Candy biome, checks actual block populations, photographs the cave and exercises
native tower/town placement. This is separate from a hosted multiplayer playtest.

**New chunks only.** Existing explored caves and player builds are not rewritten.
The owner can inspect a fresh disposable world before release. A future repair of
already-generated caves requires a separately scoped, backed-up migration; deleting
the live world or forcing regeneration is not part of this fix. Publish only with
the coordinated client/server batch after the owner says ready.
