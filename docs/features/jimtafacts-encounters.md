# Jimtafacts source encounters

Local implementation by OpenAI Codex, 23 October 2026. Combined build, server and client tests are intentionally deferred until the complete Jimtafacts implementation pass ends. Nothing here has been playtested or published.

## October 25 local follow-up

The [B–H batch](jimtafacts-b-h-local.md) adds transient three-part spider hit regions,
scoped prey-food consumption, synced Keeper rise and a separate compressed ghost
pass. These additions compile but have not received the deferred combined runtime
or visual tests. The original differences below describe the earlier baseline.

## Ice Spider

Uses the supplied `ice_spider.geo.json`, complete `ice_spider.animation.json` and original skin without pixel or geometry changes. The eight-leg gait, ten-tick crouch transition, terrain lean, climbing, emergence, three timed melee attacks, pounce, backwards leap, ranged ice attack, grappling and hostile ice tomb are native implementations of the statically recovered behavior. Hunger thresholds, feeding, delayed regeneration and source combat attributes are retained. The supplied tomb and ice-spike shapes and skins render the attacks.

Villagers are excluded from targeting, claimed/town animals are protected, and hostile tombs/spikes cannot be placed in claims or towns. Tombs expire, release on the spider's death/unload, and can be broken; they never grant the healing or invulnerability of the player's Ice Tomb spell. Hunt placement has a fixed 24-attempt budget and an eight-spider local cap. Pheromones provide an explicit native use for the supplied item art: a ten-second warning followed by the bounded hunt. Source frosted eggs trigger one spider and expose the unfrosted egg, which requires Silk Touch. Prepared and cooked eggs retain the source recipes and four-serving Frostbitten Strikes behavior.

The original NeoForge multi-part collision system is not represented as extra independently damageable entity parts; the native spider has its source body dimensions. Original animal-food drop consumption is not currently reproduced. These are behavioral differences, not missing artwork.

## Citadel Keeper

Uses the original `citadel_keeper.geo.json`, complete `casting_animations.json`, normal/restored skins and hurt-overlay skin. Carries the original hand model of its flamberge. Six original attack clips use their recovered impact frames, including the delayed lunge and single-hit combo logic. Source idle gait, projectile reduction, fall immunity, restored equipment and summon-only loot suppression are implemented. The ghost overlay retains its scrolling UVs; the source per-bone ghost compression and exact rise translucency still need visual parity work.

Both flamberges retain their original model hierarchy, inventory views and textures, with source durability, damage, speed, four held armor points and netherite-scrap repair. Keeper loot includes the original cinder essence/netherite-scrap table and normal equipment drop chance. The native restoration recipe consumes cinder essence and netherite; it is an integration route rather than a claim that the old mod's entire metal progression was ported.

## Sources and integration

Original files remain under `art/owner-library/originals/Oct23/Jimtafacts/`. The `io/redspace/ironsspellbooks/entity/mobs/{ice_spider,keeper}` and source item/block classes were read using trusted CFR; no supplied executable was run. Runtime data copies are authored by `tools/jimtafacts_spellbook_encounters.py`; retained source notices continue to apply. New native class names and identifiers do not replace those notices.

Register `JimtaEncounterItems`, then `JimtaEncounterCreatures`, after base spellbook items. Register `JimtaEncounterClient` on the client and `JimtaSpiderDismountMixin` in the common mixin list. The generator publishes its item, block and entity IDs for the combined resource audit. Original structure integration is tracked in `jimtafacts-structures.md`. Generic fallback habitat spawns have been replaced with source template inhabitants and the recovered storm-only spider patrol schedule: 9,600 ticks divided by grouped players plus up to 3,599 ticks, four loaded-terrain attempts, and a local eight-spider cap. Patrols respect mob/monster/patrol gamerules and avoid towns and claims.
