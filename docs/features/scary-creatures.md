# Halloween scary creatures

Status: implemented prototype, draft review requested. No approved issue; contributor explicitly authorized this focused fork implementation and draft submission.
Owner/contributor: speedygroyper. Specialty: hunting/exploration, workshop through expedition rewards.

## Player experience

Four hostile fan-inspired mobs: Spectral Cosmonaut (internal ID `space_kook`) (bulky space suit, flashing red chase helmet), Drowned Diver (player-sized glowing diving suit), Emerald Phantom (legless, chained floating ghost with orange emissive face), and Onyx Warden (larger armored enemy with green seams and a right-hand greatsword). Creative/spectator players are ignored. Entities, spawn eggs and resource IDs use `jugcraft:` and are registered unconditionally. No separate mod or dependency is needed.

The cosmonaut laughs less often while idle than during pursuit. The integration restores the contributor's original procedural audio and original skull drawing; see the [asset provenance record](scary-creatures-assets.md). Editable Blockbench models are in `art/scary-creatures/`. Stable internal IDs remain unchanged.

## Connections

- Input producer: ordinary exploration and combat equipment, food and preparation; no new recipe gate.
- Output consumers: existing Jugcraft fabrication, circuits, diving equipment and steel combat gear. Metal drops reuse canonical items; no new metal families.
- Technology: aluminum/titanium, brass, rubber and steel feed existing production. Magic: no mandatory connection.
- Reachable solo/trade routes: all equipment and materials retain existing crafting/processing routes; Halloween is never required for progression. Hunting provides supplemental salvage, not a replacement production chain.

## Spawning and seasons

Natural spawn weights: Kook 3, Cutler 8, Shadow 30, Knight 3; packs 1–2. All use Overworld nighttime ticks 13000–22999, vanilla darkness/collision/player-distance/population rules, and exclude mushroom fields. Kook, Shadow and Knight spawn on the surface, not caves. Cutler requires two water blocks and an outdoor column. Shadow rises 3–5 blocks and glides down for pursuit.

Kook, Cutler and Knight each reject further natural spawns with two living same-type mobs within 64 horizontal blocks, across loaded vertical terrain. This is spawn prevention, not a hard cap after movement or commands. No global per-tick population scan or forced chunk loading is added.

Natural acquisition follows the existing server `HalloweenSeason.active()` (`halloween.mode=auto/on/off`, dates/timezone in Jugcraft config). All four belong to `jugcraft:season/halloween`. Datapacks can additionally disable natural spawns by adding that tag to `jugcraft:disabled_natural_spawns`. Ending the season does not unregister content, delete mobs, remove loot or disable spawn eggs/commands.

## Loot and balance

Independent rolls, fixed chances (Looting does not increase them):

| Monster | Material drops | Player-kill equipment |
| --- | --- | --- |
| Space Kook | 1–3 aluminum ingots; 25% aluminum plate; 10% titanium ingot; 5% basic circuit | None |
| Captain Cutler | 1–3 kelp; 0–2 brass ingots; 15% rubber | 3% scuba mask; 2% empty scuba tank |
| Black Knight | 1–3 steel ingots plus 20% one extra | 4% steel greatsword; 3% one equally selected steel armor piece |
| Phantom Shadow | 0–2 vanilla phantom membranes | None |

Equipment has approximately 25–60% durability remaining; tanks start at zero oxygen. Vanilla recent-player-hit attribution applies. Death hooks respect the mob-drops gamerule. No duplicated loot tables for custom death-hook rewards. The low-rate titanium and steel rewards bypass some processing; deliberate contributor-requested balance, subject to maintainer review. Existing recipe progression remains available outside the season. Automated farms remain possible within vanilla caps and the local spawn limits; equipment needs player attribution.

## Future AI handoff

Space Kook is intended for future Moon/planet habitats. Extend the explicit dimension check and biome entries when those destinations exist; define their time/light policy separately rather than inheriting Earth's clock. Preserve season gating and local rarity. No extra dimension is enabled now.

Black Knight's `jugcraft:black_knight_spawn_structures` structure tag is intentionally empty, per contributor choice. Future custom structures may add registry IDs; the implemented predicate then permits indoor structure-piece positions at night. For structures with monster spawn overrides, add the Knight to those lists too. No mansion or other vanilla structure is enabled specially.

## Multiplayer and persistence

AI, spawn checks and rewards are server-owned. Rendering is in the client source set. No new packets, world generation, save schema, currencies or energy network. Stable registrations preserve mobs and items through season changes/restarts. Existing worlds need no terrain regeneration. The standalone prototype's `scary_creatures:` IDs are not migrated; this draft introduces `jugcraft:` IDs. Back up experimental standalone saves before swapping mods. Removing this integration requires a backup restore if its entities are present.

## Dependencies and assets

No platform changes or new runtime dependencies. Original generated cuboid models/textures and user-edited Kook geometry are contributed under MIT; restored third-party audio and skeleton pixels are separately identified. See [asset provenance](scary-creatures-assets.md). Character names/reference inspiration are from Scooby-Doo; this is an unofficial fan prototype, not an assertion of rights in the underlying characters. Maintainers must review suitability before acceptance. No source archives, reference images or local machine paths are shipped. Runtime character recordings and recolored skeleton pixels are included as documented.

## Verification

On Windows / Java 25, `python scripts/check_repository.py` passed. The targeted `./gradlew runClientGameTest -PclientTestShard=80 -PclientTestShards=81` passed: actual Jugcraft loot IDs/empty tank, sampled probabilities, non-player equipment exclusion, durability, night/day/roof/water spawning, off-season rejection and local caps. All four models were captured and visually inspected; images are in `docs/images/scary-creatures/`.

`./gradlew build` compiled and assembled but failed one existing artillery test, `jugcraft_game_tests_triple_battery_fires_asalvo`: expected two shells remaining, observed five at tick 80. No artillery code/test was changed; this result has not been reproduced on pristine main, so it is not claimed to be a proven baseline failure. See PR for data-audit results. Two-client dedicated-server play, reconnect/restart/save migration, sustained survival chase and long-term farming performance are not yet verified in the integrated build.

## AI attribution and rollout

Prepared by OpenAI Codex (GPT-6); not Claude Opus 5.5. The contributor explicitly authorized implementation, commit/push and draft PR without an approved issue. A maintainer model-policy exception is requested in this draft; none is claimed approved. No merge, release or deployment is authorized. Models, timings and loot are reviewable prototype balance.

Final data audit: python tools/check_mod_data.py passed (1437 material IDs, data files and recipe audit).
