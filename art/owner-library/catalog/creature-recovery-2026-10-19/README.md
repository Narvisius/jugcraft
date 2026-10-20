# October 19 original creature recovery

Local implementation by OpenAI Codex (GPT-6), using the owner's supplied `jims mobs`, `jims caves`, `mutant monster things`, `familiar fauna`, `rotten creature thing`, and `legendary creatures` folders. Nothing in this milestone is authorization to push, merge, publish, or operate an owner playtest world.

## Preserved sources

The six `originals/Creature-*-2026-10-19.zip` archives preserve the supplied files. [source-manifest.json](source-manifest.json) records source-relative paths, sizes and SHA-256 hashes. Legacy names and notices remain in those archives. Only static class inspection/decompilation was used; supplied programs and mod JARs were not executed.

The supplied source models define texture sizes, cube UV origins, deformation, mirroring, pivots and bone ancestry. The converter preserves those declarations instead of selecting approximate areas from the skins. Runtime PNGs are copied unchanged; [bindings-report.json](bindings-report.json) records the source of each copied skin and the original entity sizes recovered from registrations.

## What the recovery counts mean

- 278 source model classes converted without geometry-parser failures. Generated files also include constructor variants, multipart pieces and vanilla model delegates.
- 167 creature species now use the recovered native renderer: 90 beasts, 43 cavern creatures, nine goblins, seven mutants, six Familiar Fauna animals and 12 newly registered Rotten creatures. The earlier 47-species Naturalist recovery remains intact.
- 59 existing Gecko creature bindings retain their supplied geometry/animations; the newly supplied Legendary geometry and animation files matched the existing assets.
- Source locomotion arithmetic, conditional poses, keyframes, native held-item transforms, static render scales and selected glow layers are connected to native movement/action state. Original bird wingbeat arithmetic, the Notor propeller, scarab motion graphs and mutant-zombie slam/roar helpers have explicit adapters.
- Leap/glide/run-amount aliases preserve the supplied walking/running blends for Candicorn, Forsaken, Watcher, Vallumraptor and Sugar Glider. The current bound models contain 203 keyframe clips, 1,892 gait channels, 487 expressions and 962 poses. The extractor reports 1,018 unbound calls for manual review (some have separate native adapters); this is not a count of fully implemented gameplay systems. Unknown local variables invalidate earlier aliases rather than accidentally treating source physics values as head yaw.
- Transparent blood, moss, snow, honey, tear, shell-pattern, bandana, crown, glass and other renderer overlays are separate passes over full base skins. Nucleeper buttons cycle through their original three frames. Legacy variant IDs remain valid instead of renumbering saved animals. Ferrouslime gel UVs, bounded vertex waves and attack-spike geometry use the supplied renderer/shader arithmetic; infant gel and heads scale together.
- The creeper minion explicitly delegates to Minecraft's creeper skin and the supplied renderer's native baby mesh transformer. It does not reuse the incompatible mutant-creeper atlas. Rotten inventory eggs reference current Minecraft egg models; their supplied item/block textures have explicit atlas registrations.
- Body segments for the anaconda, cave centipede, bone serpent, void worm, gum worm and Murmur use the recovered parts and parent health. They are transient, parent-owned entities rather than independent loot-producing animals.
- The ferrouslime receives a gel rendering pass, orbiting original heads, saved head count, size/health scaling and bounded merging.
- Murmur hair retains the supplied paired rest rotations and idle sway; its entity-specific hair-lag solver remains outstanding. Multipart renderers separate body orientation from head-look angles, avoiding a second application of world yaw.

See [model-coverage.csv](model-coverage.csv) and [animation-coverage.json](animation-coverage.json) for actual bindings and known unsupported animation calls. Parsed geometry is **not** evidence that every accessory is wired, every special animation is triggered, or every original gameplay system has been ported. The coverage tool reports known unbound calls and does not hide them with placeholder success counts.

## New Rotten creature gameplay

The supplied 12-species family has native hostile entities, original dimensions/attribute values, variant/equipment setup, original skins, sounds, loot, spawn eggs, foods, spear, Freeze/Channelled effects, potions, TNT barrel, treasure storage and a separate Hunter Wolf. All 48 supplied audio references resolve beneath the preserved runtime sound folder. Native encounter adapters include burned lava/water states, arrow immunity where applicable, storm-only Immortal spawning, storm lightning attacks, Zap support, bounded mummy scarabs and Dead Beard lackeys, hunter support, mining drops and environmental zombie conversion.

Native authority, loaded-area bounds, owned-creature rules and finite summoned-minion lifetimes remain in force. These adaptations do not establish exact source-AI parity.

Channelled redirects an existing natural lightning target to a nearby afflicted player under open sky, preserving the supplied 64-block limit. It does not add extra lightning ticks or bypass a roof. Multipart cleanup runs synchronously when the parent is removed, including segments outside the ticking area.

## Still outstanding

- Specialized source animation conditions/helper methods and corresponding state machines remain in the coverage report. Some source models contain additional animations that current native gameplay never triggers. The static Familiar Fauna snail has no original walking clip in the supplied model.
- Four `jimbologics` species (camel, coconut crab, penguin and squirrel) still lack original entity geometry in these folders and retain their previous models.
- Accessory/armor/projectile models converted by the geometry tool are not all connected to native renderers. Remaining custom layers, equipment geometry and special renderer effects require individual integration.
- Exact special-encounter parity remains unfinished: worm splitting/portals/constriction, complete mutant attack/death state machines, source-specific creature interactions, ferrouslime splitting/merge interpolation, and Rotten head decorations, Dead Beard barrel/treasure sequence and cauldron interactions.
- Terrain-changing source behaviors require integration with Jugcraft claims and existing world rules before enabling them. Current native combat does not silently import those destructive behaviors.
- Full variant visual acceptance, survival balance, population-scale multiplayer and an owner playtest remain separate from automated validation.

## Reproduction

Run `recover_creature_models.py`, `rotten_creatures_content.py`, `bind_recovered_creatures.py`, `recover_mutant_poses.py`, then `creature_recovery_coverage.py` from `tools/`. The recovery scripts use the preserved static source workspace generated from the supplied files; they do not download or execute those mods.

`tools/test_creature_recovery.py` checks the hierarchy, action timelines, original texture hashes and source-condition conversion. `RecoveredCreatureGameTests` covers native integration, and `RecoveredCreatureClientTests` captures a disposable rendered gallery. See the [validation record](VALIDATION.md) and [local artifact hashes](local-validation.json) for completed runs and their limits.

Texture provenance checks resolve the archived source manifest, so they do not require the owner's Downloads directory on another contributor's PC. Original source paths are retained as audit information. The generated gameplay resources use the current Minecraft loot schema; source loot modifiers must not be silently ignored as legacy keys.
