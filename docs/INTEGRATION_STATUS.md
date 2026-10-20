# Jugcraft implementation and remaining work

## October 15 integration candidate

The [October 15 integration record](features/oct15-community-integration.md) lists the ready source branches, conflict repairs, validation and excluded drafts. It remains a local candidate until the integration PR is published, passes its checks and merges.

## October 13 integration

The [October 13 integration record](features/oct13-community-integration.md) tracks the 31 source PRs,
their exact heads, integration corrections and verification. It includes community chemistry, Peepo automation,
guns, bosses/lairs, Concordance/encyclopedia, mineral deposits, power lines/wall runs and nuclear power.
Check the integration PR's merge state before treating these as shipped on main. The owner's separate local machine
consolidation and recipe-picker playtest remains outside this batch.


Updated 11 October 2026 for [integration PR #320](https://github.com/jimbozoomer-byte/jugcraft/pull/320) (source PRs #299–308 and #310–319), following [integration PR #277](https://github.com/jimbozoomer-byte/jugcraft/pull/277) and [follow-up PR #298](https://github.com/jimbozoomer-byte/jugcraft/pull/298). This is the owner-facing index: feature records contain the detailed behavior and historical test evidence. The PRs' current GitHub states determine which combined work has reached `main`.

## October 11 additions

The 20 source PRs below are combined on `integrate/oct11-ready-content` for validation against current main. The integration PR's merge state determines whether this batch has reached main. Individual feature records retain their earlier branch test evidence; the combined PR records the final tested commit and results.

| Area | Implemented change | Source PRs / guide |
| --- | --- | --- |
| Guns | Eighteen more guns: launchers, coil/plasma, double-barrels, sculk and six Nether guns; 54 guns in total | #299, #301, #305–306, #314, #318; [guns](features/guns.md) |
| Gun systems | Six-cell charging rack, Raider Gunners in camps/raids, and dual wielding of compatible one-handed guns | #307–308, #311; [guns](features/guns.md) |
| Bosses and lairs | Glacier Hall/Frost Horn and the Yeti King; Cinder Kiln/Kiln Seal and the Cinder Tyrant; adds, encounter mechanics, loot and return routes | #300, #304, #313, #317; [Yeti](features/yeti-king.md), [Tyrant](features/cinder-tyrant.md) |
| Industrial machines | Shared multi-block forms with ports, saved batches and status screens; the Electrolytic Separator; the hydrogen Gas Burning Generator; the bioethanol and biogas Bio Generator; the Chemical Infuser (hydrogen chloride); the Chemical Oxidizer (sulfur dioxide); on the contributor's fork: the Industrial Fermenter (bioethanol and collected CO2), the Bulk Chemical Reactor (hydrochloric acid and chloride leaching), the Anaerobic Digester (biogas), coal gasification (Gasifier, cleanup rack, CO methanation), contact sulfuric acid (vanadium bed, SO3, absorption) and the PVC route with HCl recovery | #312, #316, #319; [foundation](features/industrial-machine-foundation.md), [separator](features/industrial-electrolytic-separator.md), [generator](features/industrial-gas-burning-generator.md), [bio generator](features/industrial-bio-generator.md), [biogas power](features/industrial-biogas-power.md), [infuser](features/industrial-chemical-infuser.md), [oxidizer](features/industrial-chemical-oxidizer.md), [fermenter](features/industrial-fermenter.md), [bulk reactor](features/industrial-bulk-chemical-reactor.md), [digester](features/industrial-anaerobic-digester.md), [gasification](features/industrial-coal-gasification.md), [contact acid](features/industrial-contact-sulfuric-acid.md), [PVC route](features/industrial-pvc-hcl-recovery.md) |
| Trinket presentation | Belt/boots hide under covering armor; each player's Show my worn trinkets setting is synchronized to viewers | #315; [Wayfaring](features/arcane-concordance-trinkets.md) |
| Fixes and integration | Thrown arms hit targets they start a tick inside; all shared registrations/audits retained; the five-client-job scheduler carries the expanded suite | #303 plus integration resolution; [Arms VIII](features/arms-viii.md) |
| Planning | Factory construction/operation, 36 production cards and eight example layouts; shared boss delivery playbook | #302, #310; [factory plan](features/industrial-factory-implementation-plan.md), [boss playbook](branches/BOSS_PLAYBOOK.md); proposals beyond the machines above remain unimplemented |

The generator's original repository-check failure was the four-job scheduling estimate reaching its 25-minute budget. The boss stack's five-job expansion resolves it without dropping classes or relaxing that check. The combined local repository/pack tests verify complete, duplicate-free class coverage.

Current limits: the generator burns hydrogen only (methane awaits its producer); the wider industrial plan is still being delivered. The Yeti and Tyrant are playable encounter implementations, while the October 10 magic planning pack contains further proposed school/quest interactions. Human two-client play, survival balance and owner art acceptance remain separate work.

## October 10 integration retained

| Area | Implemented content | Source PRs / guide |
| --- | --- | --- |
| Guns and attachments | Scopes; six sidearms/service guns; heavy, energy, marksman, automatic and pump weapons; Tactical Grip and Laser Sight; first-person aiming fixes; five grenade kinds for the Trench Lobber | #265, #268–269, #278, #280, #285–287, #289–291, #293; [guns](features/guns.md) |
| Farming and cooking | Ten pies/tarts, seven milkshakes, crop presentation updates, wild roots, mushroom colonies, rotten tomatoes, more vegetables, herbs, spices and cinnamon | #266–267, #270, #279, #281; [Agriculture](branches/AGRICULTURE.md) |
| Peepo and Jughead | Taming, ownership and care; Planner assignments; gardens, livestock and kitchen work; workshop assistance, physical deliveries, shared supplies, porter routes and transport crates | #282; [player setup guide](PEEPO_COMPANION_GUIDE.md) |
| Bosses and lairs | Shared lair entry/return framework, Hollow Acre and Vesperine, Spindle Loft and Madame Tatterlace, their encounter mechanics and special loot | #284, #288, #292, #294; [feature records](features/) |
| Armor and weapons | Thirteen owner armor sets in Blockbench projects with runtime integration, improved Sentinel/Frost Knight/Reaper/Paladin/Templar presentation, four matching weapons and Sentinel Shield | #273; acquisition and visual review limits below |
| Fire magic | Hearthbinding research; Hearthspark, Hearthguard, Cinderbolt and Hearthflare; Smoulder; fire foci, Fire Bangle, Pyromaniac and Pyromancer armor using the supplied assets | #295; [fire spells](features/arcane-concordance-ember.md), [fire equipment](features/arcane-concordance-ember-regalia.md) |
| Wearable magic | Leather Belt, Angelic Feather, Kraken Shell, Infernal Claws, Angelheart Vial, Phoenix Down, Amphibian Boot and Ice Breaker; belt and boot rendered on the wearer | #295; [Wayfaring](features/arcane-concordance-trinkets.md) |
| Integration and tests | Projectile aiming and village-test setup fixes; four client-test shards with 60-minute limits; companion test discovery; shared-code selection fallback; isolated, repeated javelin validation; preserved wild-crop/spice generation, shared enchantment tags, weapon/armor registrations, complete boss-test flooring and ticking area, fresh local test worlds, Windows Blockbench checks, giant-pumpkin identity persistence, valid single-cell crafting output and costume/attachment resource-load corrections | #271–272, #283 and #277 integration commits |
| Industrial planning | Owner-selected metals, polymer/electronics, grid-storage, fuel and cryogenics directions; 79 machine/module art briefs and 23 larger-variant specifications | #264, planning updates in #295 and #297; [machine art specifications](features/industrial-machine-models-and-textures.md); **documentation, not implemented models or new factory gameplay** |

This combines 28 source PRs (#264–273 and #278–295) through #277, plus the later documentation PR #297 through #298: **29 source PRs in total**, alongside integration fixes. #277 and #298 both merged on 10 October 2026, so all 29 are in `main`. (A source PR can show as merged into an integration branch before its parent PR reaches `main`; that alone does not make it available in a main build.)

## Existing foundation retained

| Area | What is already present | Remaining boundary |
| --- | --- | --- |
| Technology | Shared materials, machines, power, kinetic machinery, pipes/conveyors, logistics, chemistry/refining, tools and drones | Shared forms, the separator and hydrogen generator are now in the October 11 batch; the remaining production chains still require implementation |
| Magic | Concordance research, journal/codex, invocations, rituals, alchemy, cultivation, magical helpers/logistics, relics, progression and shared projects | A framework and some traditions being built does not complete every requested school or every owner-library asset |
| World and settlement | Surface/cave content, towns, Styx and his botanical/observatory content, plus an offline World Designer for new worlds | Existing chunks are not automatically rebuilt; inspect actual structures and layouts in a new test world |
| Halloween | A substantial permanent collection of farming, decorations, activities, creatures, equipment and encounters | Public-server event tuning and playtesting still needed; earned builds/items must survive the event ending |
| Rocketry | Rocket workshop, propellant supply, survey/weather rockets, flares and terrestrial rocket applications | Full planet/moon travel and galactic exploration are future work; terrestrial development was prioritized |
| Installation | Minecraft 26.3, Fabric Loader 0.19.5, pinned libraries and the Jugcraft Complete Modrinth import pack | Use an artifact from a successful combined build; no public Modrinth listing or server deployment is implied |

See [the source/API map](WHAT_EXISTS.md), [technology guide](TECH_TREE.md), [Concordance contract](ARCANE_CONCORDANCE.md), [rocketry](features/rocketry.md) and [distribution](DISTRIBUTION.md).

## Next work, in a useful order

| Priority | Task | Completion evidence |
| --- | --- | --- |
| 1 — Playtest this combination | Two independent clients on a dedicated staging server: join/rejoin, saves/restart, claims/PvP, trading, companion inventories/deliveries, new boss entry/death/return and worn equipment | A written run record with exact build, failures and fixes; automated client screenshots are not a substitute for this session |
| 1 — Review the new art in game | Check the 13 armor sets and their held weapons, fire sets, worn trinkets, crops, guns and boss presentation | Owner visual acceptance; no claim that Blockbench screenshots alone prove runtime appearance |
| 2 — Survival and balance | Give creative-only armor/weapon additions reviewed acquisition routes; test a fresh world's route to electricity and magic; balance guns, bosses, farming and automation together | Reachable recipes/loot, no circular gates or duplication; the 2–4 hour first-electricity target measured rather than assumed |
| 2 — Factory expansion | Implement the selected industrial plan in focused increments: independent starter reagents, gas/acid processing, metals, polymers/electronics, storage and rocket fuels | Exact recipes and resource/energy accounting, usable consumers, bounded processing and save tests |
| 2 — Blueprint completion | Comparator output, any-material/tag slots and the exported design kit with its in-JAR AI guide are added on the contributor's fork, pending review | [Remaining blueprint scope](features/blueprint-system.md#not-yet-done-from-23); imports, holograms and drone construction already exist |
| 2 — Encyclopedia | Build the requested inventory/keybind UI for both technology and magic, pathways and integrated quests. Its first slice (both ways in, one screen, the First Light route) is implemented in Concordance Chunk A and passes its server and client tests in CI (run 38435014953); not yet merged or played by a person ([record](features/arcane-concordance-first-route.md)) | The [Encyclopedia checklist](TODO.md#jugcraft-encyclopedia), including server-authoritative quest progress and rewards |
| 3 — More magic and supplied assets | Continue fire equipment/familiar work, remaining school content and further imports of appropriate owner assets | Each import has actual gameplay, provenance and tests; files in the library alone are not implemented content |
| 3 — Stolas and Ars Goetia | Start the shared summoning/pact system with Stolas and then cover the requested 72 spirits | Connected botanical/astronomical/mineral pilot with Styx; shared definitions, costs, persistence and protections |
| 3 — Space and December | Planet/moon exploration and Christmas-style seasonal additions | Focused feature proposals/implementations on the existing systems, while preserving earlier progression and earned content |
| Before a public server | Representative factory/companion load test, backup/restore, permissions and an identified release candidate | Staging evidence and installation instructions for that exact build |

Detailed scope: [owner TODO](TODO.md), [roadmap and Ars Goetia](ROADMAP.md), [multiplayer tests](TESTING.md#dedicated-server-and-two-clients).

## Owner decisions and approved next routes

The newest #264 update records all eight answers in [industrial batch 13](features/industrial-chemistry-and-fuels-plan.md#resource-reagent-and-residue-decisions-thirteenth-batch): galena; spodumene first and lithium brines later; separate primary cobalt ore; chromite concentrate; vanadium-bearing iron feed; a short powered peroxide game recipe; novolac/DNQ photoresist; and paid gypsum-residue preparation for construction. These are selected game directions, not implemented recipes. The [connected production map](features/industrial-chemical-catalog-and-routes.md) groups the proposed implementation work.

The Encyclopedia still needs quest/reward policy and visual direction. The new equipment needs owner visual review and survival acquisition choices where its feature record says creative-only. Those decisions do not block the completed content above from being tested and integrated.

## Save compatibility correction

Giant pumpkins now save their contest identity as `pumpkin_id`, separate from Minecraft's reserved block-entity `id`. Old custom UUID tags still load. If an older full world save already replaced that UUID with the block type, the lost UUID cannot be reconstructed; the pumpkin receives an identity while its growth, weight, carving and light data remain readable. Keep normal world backups before testing alpha updates.

## Verification and scope

The October 10 full combined validation is recorded in #277. The October 11 integration PR records this batch's combined validation; subsequent correction checks are available in the [Build history](https://github.com/jimbozoomer-byte/jugcraft/actions/workflows/build.yml). Individual branch successes are historical evidence, not a substitute for the combined build. Check the exact commit's results for compilation, data regeneration, both server configurations and the selected client tests. Human multiplayer/load testing remains outstanding unless a separate run record is supplied.

Integration work includes actual conflict resolution, generator corrections and test reliability fixes; it does not change platform pins, remove review protections, execute owner-library programs or deploy to the live server. Integration and this status index were prepared with OpenAI Codex; feature authors and their original attribution are retained.

## Bulk-factory integration — 20 October 2026

The owner selected PRs #361, #362, #364, #365 and #374 for combined integration on current main. See [the integration record](features/bulk-factory-integration-oct20.md) for scope, compatibility repairs and validation. Historical contributor-only descriptions above refer to their original checkpoints. Final merge evidence will be recorded on the integration PR.
