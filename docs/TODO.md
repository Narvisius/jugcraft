# Jugcraft TODO

## Overnight integration — October 21

- [x] Combine starter workshop and Frostward College M0 with main; reconcile older tech drafts with canonical packages. [Exact scope](features/remaining-release-oct21.md).
- [x] Add optional alloy cladding and drone propeller recipes using existing Aluminum-Magnesium Alloy Plates.
- [x] Prepare JUGCRAFT player/server packaging, integrity manifest, Windows import helper and checked-main draft release workflow. [Install/update procedure](SERVER_RELEASES.md).
- [ ] Confirm final combined CI/merge results in the integration PR before promoting artifacts.
- [ ] Add food-grade phosphoric-acid/cola production separately from industrial acid; extend MMH/methylamine propellant chemistry on shared machinery. These residual draft ideas are not implemented.
- [ ] Review eight-group fertilizer/legume tuning against existing nitrogen/phosphate tags; retain current behavior until tested.
- [ ] Finish college room furnishing and stair-edge/railing review, then the larger curriculum/campus plan.
- [ ] Owner: configure Modrinth Server Project/address, stage files, exercise two-account joins/restart and native updating, choose deployment window and promote the tested release.
- [ ] Custom JUGCRAFT main menu and main server Play button: wait until owner is awake, as requested.

**19 October publication authorization:** the owner approved publishing the current local content and merging ready PRs. Earlier local-only checkpoints are historical; remaining behavioral parity and independent multiplayer work stay open.

## Adam-ondi-Ahman and Cydonian adventure — October 20 integration

- [x] Combine the altar valley, both Altar Keystone crafting routes, Cydonian Sanctum, Sentinel encounter, participant loot and return gates from #370, #372 and #382. Preserve all existing lairs and equipment.
- [x] Reconcile the shared registrations, generators, mist-gate effects, boss audits and acquisition routes against current main. Add a saved-placement codec regression for the valley's chosen site and new-chunk evidence.
- Merge gate: combined Build/Foundation workflows and inspection of the rendered valley, dungeon and boss gallery. The integration PR records final results and whether this group has reached main.
- [ ] Human survival balancing, multiple seeds and a two-account dedicated-server playtest. Automated client tests do not replace these checks.
- Feature records: [valley](features/adam-ondi-ahman.md), [Sanctum](features/cydonian-sanctum.md), [Sentinel](features/cydonian-sentinel.md).

## Supplied creature models, animations and systems — October 19, local

- [x] Preserve all six newly supplied collections with source hashes; recover 278 source model classes without geometry-parser failures and bind 167 species to original geometry/UVs and unchanged supplied skins.
- [x] Restore native multipart bodies with shared parent health, original locomotion/keyframe declarations, held items and selected source render layers. Add the ferrouslime gel/head-count/merge implementation and 12-species Rotten family with native encounter adapters.
- [ ] Complete the remaining specialized source-animation helpers, state machines, accessory renderers and encounter systems. The [recovery catalog](../art/owner-library/catalog/creature-recovery-2026-10-19/README.md) distinguishes implemented adapters from outstanding parity; [model coverage](../art/owner-library/catalog/creature-recovery-2026-10-19/model-coverage.csv) lists exact known gaps. Do not mark this as a complete port of every source behavior.
- [x] Finish the local validation pass and package a preview: 2,004/2,005 broad server tests passed, then the corrected skylight fixture passed in the seven-test focused build; ten Python checks, offline data audit, final 269-image client gallery and 48 in-game audio lookups pass. Exact evidence and checksum are in the [validation record](../art/owner-library/catalog/creature-recovery-2026-10-19/VALIDATION.md) and [artifact report](../art/owner-library/catalog/creature-recovery-2026-10-19/local-validation.json). The full server suite was not repeated after the fixture correction.
- [ ] Owner-controlled playtest and visual acceptance before pushing. Keep the build local and preserve all existing owner saves.
- [ ] Locate the original camel, coconut-crab, penguin and squirrel geometry for the four `jimbologics` species; these supplied folders do not contain those models.

## Original wildlife model recovery — October 19, local

- [x] Recover the newly supplied Naturalist model data: 73 native layers, 441 animation clips, 47 existing wildlife species. Preserve source files/hashes and original texture pixels. Replace palette-patch wildlife rendering with authored geometry/UVs, baby layers and separate dye overlays.
- [x] Verify all runtime base/overlay atlas pairings, preserved snail shell/eye geometry, part hierarchy and bounded declarative extraction with focused offline tests.
- [x] Complete the new rendered wildlife gallery: all 47 renderer bindings pass, with 49 captured views reviewed for original anatomy and skin placement.
- [ ] Owner-playtest and accept the recovered animal appearance before publishing. Automated screenshot completion does not establish owner acceptance.
- [ ] Obtain missing baby-bird skins and compatible missing baby dye overlays. Current safe display uses scaled original adult birds and normal infant skins where an overlay is incompatible.
- [x] Recover newly supplied model data for the other collections; see the October 19 recovery section above. Specialized behavioral animation and remaining original-model gaps are tracked there.
- Source and limitations: [wildlife recovery catalog](../art/owner-library/catalog/naturalist-models-2026-10-19/README.md).

## Vehicle assembly bays — local, awaiting playtest

- [x] Implement 6x6x6 Mecha and 6x6x3 Tank workbenches, existing-vehicle construction/refit, shared power, exact input previews and durable chassis options. See [vehicle assembly bays](features/vehicle-assembly-bays.md).
- [x] Add grounded animated gantries, attached robot arms, control cabinets and visible power sockets.
- [x] Pass all 1,999 server tests, the rendered client/menu gallery, full offline data/model checks and the updated 5,759-item acquisition audit. See the feature record for evidence.
- [ ] Owner visual/gameplay playtest and two-human-client interaction checks before release. Keep this milestone local until the owner approves pushing.
- [ ] Consider live-vehicle docking, cosmetic palettes and chassis-specific weapon modules after this local milestone.

## Survival recipes and acquisition chains — 19 October 2026

- [x] Audit all 5,757 registered items, with recipe prerequisites, stations, tags, fluids and documented harvesting/loot routes. Keep boss rewards and other intentional loot noncraftable. See the [chain report](features/survival-acquisition-audit-oct19.md) and [sortable inventory](audits/survival-recipes.csv).
- [x] Add 43 confirmed recipes covering kitchen/storage/garden utilities, insect habitats, food processing, End carpentry/boat fittings, cave equipment and building materials. Reuse existing stations and add recipe-book unlocks.
- [x] Restore six ordinary-animal material drops; fix the Capsid music-disc tag and all 12 insect habitat capture outputs. Preserve existing creature ownership protections.
- [x] Validate all 43 recipe grids/outputs in the game, the Capsid and actual item registry (3 tests); run all 20 menagerie regressions and 5 Python graph tests; assemble the local JAR and check repository structure/documentation.
- [x] Resolve the remaining active acquisition-review queue through native-interaction evidence and real source repairs: 81 more explicit recipes (124 total), 44 additional source pools, breeding eggs, variant starfish capture, Firebird feathers, occupied ant colonies and a Candy Cavity soda source. Keep boss/loot acquisition intact.
- [x] Connect 77 runtime structure containers to real loot tables, including custom barrels and outdated keep/camp table paths; preserve filled inventories and original supplied files. Make unopened cabinet loot persist and roll only once.
- [x] Audit base resources and process prerequisites, with zero invalid ingredients or unresolved active chains in the current graph. Publish the [source/consumer catalog](audits/survival-base-sources.csv); exclude unused chest tables from evidence.
- [ ] Design survival acquisition for the 72 armor pieces and six matching weapons explicitly deferred earlier. These 78 entries remain labeled as deferred, not counted as obtainable.
- [x] Finish the combined acquisition validation: all 1,990 server tests pass, including all 124 recipes, 44 source pools, 77 container links, collectible soda, breeding eggs, all starfish captures/releases and Firebird production. Five graph regressions, offline data/model checks and repository documentation checks also pass. Evidence is in the feature report.
- [ ] Owner survival/balance playtest of the completed chains, including quantities, station progression and encounter availability. Current graph reachability is qualitative.
- **Local only:** no push, merge or owner-profile installation in this pass.

## October 18 owner collections — native gameplay implemented locally

- [x] Finish the remaining native household, ecology/fauna, mutant, creature, landscape/cave, equipment and resource-loop implementations before combined testing. The full roster now contains 265 species.
- [x] Run the full server suite (1,980 passing tests), then focused menagerie regressions (19 passing tests) after final metadata/plant corrections. Combined rendered client/armor/UI galleries pass; exact final evidence is in [the validation record](../art/owner-library/catalog/edited-2026-10-18-part2/local-validation-oct19.json).
- [x] Repair worldgen ordering, runtime registrations/loot formats, sided generator APIs, unloaded-chunk equipment access, petal behavior, connected/powered block states, bucket/particle atlases, wearable/audio metadata, pie models and overlapping UI captions.
- [x] Refine 206 reconstructed species and all 498 wildlife adult/baby mappings; repair hollow furniture, legacy item models and retained geometry diagnostics. The latest block/item audit checks 11,666 models with zero diagnostics. Supplied pixels and 59 supplied creature rigs remain intact.
- [ ] Owner visual acceptance of the reconstructed models and all skin variants. Automated checks and sampled galleries do not replace this review.
- [ ] Owner-controlled survival/balance playtest, including biomes/structures on representative new terrain and the machinery/resource loops.
- [x] Run two independent client JVMs against a disposable loopback server: ownership rejection, capture synchronization and reconnect persistence pass. Record synthetic 0/64/192 live-AI population timings; 192 animals averaged 2.57 ms/tick with 3.04 ms p95 on this PC.
- [ ] Longer multiplayer/restart soak and representative hosting-capacity validation before release. The short local animal benchmark does not establish a player limit.
- Latest follow-up evidence: [local polish validation](../art/owner-library/catalog/edited-2026-10-18-part2/local-polish-validation-oct19.json).
- **Local only:** no push, merge, deployment or automated control of an owner Modrinth profile. Use the existing pinned dependencies with the local JAR. See [the implementation record](features/owner-menagerie-oct18.md#october-19-combined-native-implementation) for native adaptations and remaining scope.

## Owner-edited collection, part two — 18 October 2026

- [x] Preserve and SHA-256 verify all 26,765 files from the nine newly supplied collections. Record direct-use authorization, legacy namespace provenance, model coverage and existing-name overlaps in the [part-two catalog](../art/owner-library/catalog/edited-2026-10-18-part2/README.md).
- [ ] Owner-playtest the locally implemented nine cnb creatures, variants, equipment and interactions. Combined server testing passed 1,980 cases, followed by focused regressions and client/armor galleries. Full variant/balance acceptance remains.
- [x] Implement native furniture through shared inventories, seating, utilities and machine interfaces, including construction, appliances, fans, switches, remotes and mats. Owner visual/balance acceptance remains separate.
- [x] Implement jimbologics and jug_fauna ecology, wildlife, materials, wood families, boats, sap/fruit loops and six supplied ruins.
- [x] Implement native landscape/cave generation, structures, progression and hazards with stable saved IDs; deterministic biome reachability and a normal-world client load are included in final validation.
- [ ] Owner-playtest the locally implemented 20 legendary creatures and 16 complete companions inside jims mobs. Combined server tests and the client gallery pass; full variant/balance acceptance remains. The additional 90-species roster, equipment and resource loops now have native implementations; the reconstructed anatomy/skin mapping pass is complete locally, with owner visual acceptance still pending.
- [x] Implement seven native mutant encounters/minions, Chemical X, equipment, combat and survival acquisition.
- [x] Reconcile native registrations/resources and validate behavior through server tests and rendered galleries. Full variant/balance and art acceptance remains above.

## Owner-edited animals, mounts, inventory pets and structures — 18 October 2026

- [x] Preserve and checksum-verify all 3,502 supplied files, including the added 501 animal data records. Record the owner's declaration, legacy metadata, animation pairs and missing references in the [collection audit](../art/owner-library/catalog/edited-2026-10-18/README.md).
- [x] Owner authorized new native Jugcraft gameplay and reconstructed animal geometry/buildings using the supplied resources on 18 October. No further source-project approval is needed; originals remain preserved.
- [x] Implement the 70 inventory-pet abilities, food/costs, storage interfaces, recipes, persistence and shared protections through native Jugcraft systems.
- [x] Implement the animal roster, variants, ecology, farming/hunting products, capture/transport and breeding locally, with supplied or reconstructed models.
- [x] Refine reconstructed wildlife anatomy and all 498 adult/baby UV mappings; verify bounded UVs and rendered adult/baby views.
- [ ] Obtain owner acceptance of the reconstructed wildlife and every skin variant; unavailable original geometry is not claimed as recovered.
- [x] Implement all fourteen supplied mount model/animation pairs with taming, riding, movement, ownership, saves and acquisition.
- [x] Implement supplied ghost/pumpkin/decorative/gear assets, all six supplied pocket-dungeon templates and thirteen reconstructed structure concepts. Missing original templates are not claimed as recovered.
- [x] Validate native gameplay, extend the encyclopedia and preserve provenance/notices. Art acceptance and owner playtesting remain above. See [the feature record](features/owner-menagerie-oct18.md).

- [x] Resolve Nouveau client-loader warnings about unused `#all` aliases, obsolete entity parents and legacy spawn eggs. Preserve Gecko item renderers, restore the ordinary notebook icon and verify all 425 Nouveau sprite bindings. The corrected client load has no missing-model/texture errors for this group.

## Diorama default map — 18 October 2026

- [x] Add the verified Fabric 26.3 release to the shared pack lock: automatic client installation, excluded from dedicated servers, optional for standalone Jugcraft.
- [x] Reserve B for the encyclopedia, N for the backpack and comma for Diorama's new waypoint. Preserve saved custom controls.
- [x] Full build and 1,960 server tests passed; map, encyclopedia and gun client tests passed, plus optional-absent assembly/gun startup. Generated the updated local pack. See [Diorama integration](features/diorama-pack.md).
- [ ] Owner-controlled Modrinth import/playtest and representative exploration performance check.

## Existing validation follow-up — 18 October 2026

- [ ] Reconcile the new Jug Nouveau item registrations with the universal alchemy-value catalogue: the Python coverage check currently first reports `jugcraft:bookwyrm_charm`. Do not assign arbitrary values just to satisfy coverage; validate recipes and conversion conservation.

## Gun controls and crash repair — 18 October 2026

- [x] Repair invalid Nouveau unbound keys, including saved profiles, and reserve R for gun reload. Move shader reload to F8, JEI recipes to Y and encyclopedia/journal refresh to F6.
- [x] Remove gun inventory/hotbar ammunition bars while retaining the held-gun counter.
- [x] Targeted gun client validation passed with and without optional integrations; Encyclopedia F6 refresh regression passed. Prepared the local repaired pack and replacement JAR; owner playtest remains.
- Evidence and migration details: [Gun controls repair](features/gun-controls-oct18.md).

## Jug Nouveau owner-library port — 18 October 2026

**Owner scope update:** implement the entire core `ars_jimbaux` collection before
testing. This complete implementation pass is finished locally; combined
validation began afterward. The full build, all 1,960 required server game tests
and all three relevant client suites passed. The client suites also passed with
optional integrations disabled. See the full implementation and validation
record for scope, evidence and remaining human playtesting.
Keep this work local. A copied asset or registered item without its intended
behavior does not complete its checklist entry.

- [x] Inventory all 3,607 core `ars_jimbaux` source files and preserve their original paths/hashes. These resources are already in the owner library; no gameplay source code was supplied.
- [x] Cross-check both compiled owner archives: all 4,847 resource entries accounted for, including 28 elemental shared-path variants now preserved separately. Keep original loader metadata/notices; no old archive code executed or bundled.
- [x] Local Book Wyrm/Storage Lectern implementation: supplied six colors, geometry and animations, craftable charm/wand/lectern, linked real chest inventories, ownership/party access and saved bindings.
- [x] First storage integration validated: all 1,937 server tests, rendered six-color models/lectern/menu, page synchronization, GUI scales, data/recipe checks and unchanged-owner-art checks passed.
- [ ] Owner-controlled Book Wyrm storage playtest before publishing; independent multiplayer/restart field testing and larger-network performance remain to do.
- [x] Author the full storage/familiar integration locally: aggregate catalog, real crafting, named groups, remote terminals and redstone-controlled imports.
- [x] Local Starbuncle delivery: supplied model, idle/run animations and all 16 dye colors, charm, Dominion Wand setup, real saved cargo, owner/party controls and recovery; reuse shared workers/navigation/authority.
- [x] Starbuncle validation: all 1,945 server tests, actual client walking/two delivery trips, sixteen-color gallery, Book Wyrm storage regression and data/art audits passed locally.
- [ ] Owner-controlled combined Jug Nouveau playtest and independent survival/multiplayer feedback before publication.
- [x] Author extended Starbuncles: wild token acquisition, gathering, filters/routes, preferred paths, stacked cargo, beds, accessories/special appearances and familiar effects; restore apparatus charm recipe.
- [x] Implement locally: port remaining helper jobs: Drygmy, Whirlisprig, Wixie and Amethyst Golem, reusing worker/logistics systems where suitable.
- [x] Implement locally: port enchanting, imbuement, alteration, glyph and potion workshops with real recipes, input/output previews and usable interfaces.
- [x] Implement locally: port source transport and spell devices through existing magical power/compiler/authority systems.
- [x] Implement locally: port archwood, magical crops/fruit, sourcestone and decorative building families with complete survival routes and compatible generation.
- [x] Implement locally: port missing gear/familiar behavior and creature/encounter families, including Weald creatures, Wilden and Alakarkinos.
- [x] Author full glyph/ritual adapters and resource-family mapping; copied resources are distinguished from native behavior.
- [x] Author all fifty preset tomes and their Scribe’s Table acquisition; connect archaeological weights, scrying tags, bat summons, dispels, potted plants and invisible redstone pulses. Resolve starter Source Gem progression and add acquisition guidance.
- [x] Complete the combined post-implementation validation: full generation/build, 1,960 server tests, three client suites with and without optional integrations, final data/art audit and pack verification. Fixed combined-run failures and prepared the local Modrinth playtest; owner playtesting remains separate.
- [ ] Audit the elemental/companion resource namespaces after the core port and record overlap with existing Jugcraft content.
- Scope, limits, source manifest and evidence: [Jug Nouveau integration](features/jug-nouveau.md).

## Alchemical equipment — 16 October 2026

- [x] Local interface/access revision: engraved alchemy panels, consistent controls and selected states; remove equipment-use research gates, including traded charms and old owned factories. Gem controls work directly after equipping. See [interface/access record](features/alchemy-interface-and-access.md).
- [x] Interface/access validation: 1,929 server checks, rendered fresh-player menus and packet interactions, and data/art/repository audits passed locally.
- [x] Owner accepted the Alchemy Refined presentation and authorized publishing all alchemy additions and UI fixes on 18 October 2026. Earlier per-pack publication checkpoints are superseded by this combined approval; detailed survival balance and independent multiplayer remain follow-up work.

- [x] Local first slice: Philosopher's Stone interface, paid Repair Talisman and Black Hole Band, using supplied owner icons and the existing Prima Materia ledger.
- [x] Automated validation: 1,771 server tests, full data/art audit, rendered interface and live equipment checks.
- [x] Replace the transmutation book with a stone tablet and rename its currency to essence across Assay, gear and guides, preserving existing balances.
- [x] Local Dark Matter workshop: three fuel tiers, three covalence dusts, Dark Matter, pickaxe/hammer/axe modes, essence costs and anvil repair, using ten supplied icons unchanged.
- [x] Dark Matter validation: 1,787 server tests, full data/art checks, rendered mode switching/mining and held-model inspection passed locally.
- [x] Local stars/amulets: six Klein Stars, storage tablet, charge-preserving smithing upgrades and Evertide/Volcanite utility amulets using eight supplied icons unchanged.
- [x] Stars/amulets: 1,801 server tests, rendered transfers/amulet use, full data audit and original-art checks passed locally.
- [x] Local implementation of remaining Dark Matter equipment, Red Matter tools, three armor sets and material/fuel blocks with supplied art. Combined automated validation passed; owner accepted the combined presentation on 18 October. See [complete alchemy](features/complete-alchemy.md).
- [x] Local collectors/condensers: three daylight collectors, two material condensers, retained targets, essence/star transfers, hopper faces, pause controls and charge-preserving upgrades using supplied assets.
- [x] Collector/condenser validation: 1,820 server tests, rendered menus/models, live hoppers, daylight/roof behavior, full data/art audit and showroom ownership binding passed locally.
- [x] Local implementation of all remaining alchemy content: 82 entries across equipment, relays/furnaces/pedestals/wards/Nova charges, bags/chest/tablets/books/XP storage, mobility/combat/farming/support rings and mining/building artifacts. Existing 26 entries remain integrated.
- [x] Complete the requested combined validation phase after all 82 entries are integrated: 1,886 server tests, 67 Python tests, full data/art audits and the rendered client check passed. Final layout changes were rebuilt and client-tested. See [complete alchemy](features/complete-alchemy.md).
- [x] Local connected alchemy: learned patterns/favorites/search, Stone terrain/crafting, filtered converters and storage artifacts, expanded pedestals, collector fuel tiers, carried-star power, Gem controls, learned Eye building, remaining UI/state art and animated chest lids. See [connected alchemy](features/connected-alchemy.md).
- [x] Connected Alchemy automated validation: all 1,909 server tests, 67 Python tests, full data/art audit and real rendered interface checks passed locally.
- [x] Local alchemy playability pass: universal default-item values, one-screen dissolve/learn/withdraw, exact-value bulk withdrawal, wallet/star HUD, Gem direct controls/state, Density limits/reserve/status, storage sort/consolidation, factory offline operation, pedestal diagnostics/range and building/mining previews. See [playability record](features/alchemy-playability.md).
- [x] Replace retained-sample learning and the 25% formation surcharge in the active tablet workflow; preserve saved balances and existing learned identities.
- [x] Alchemy playability validation: all 1,923 server tests, 75 Python tests, full data/art checks, Stone and complete-showroom rendered checks passed. Final interface corrections were rebuilt and client-tested.
- [ ] Owner survival/economy feedback: tune provisional custom-item root values; confirm industrial recipe/byproduct economics and the first Star/collector/automation milestones.
- Scope and evidence: [alchemical gear](features/alchemical-gear.md).

## Spellcraft and encyclopedia UI — 16 October 2026

- [x] Local implementation: replace spell text entry with an animated, keyboard-accessible constellation editor over the existing compiler.
- [x] Local implementation: illustrated two-page encyclopedia, simple contents, topic directories and focused detail/recipe pages.
- [x] Rendered interaction/scale checks, full server regression and optional-library absence checks passed locally.
- [ ] Owner visual review and manual playtest before pushing this redesign.
- [ ] After UI acceptance, expand the bestiary beyond encountered-lair dossiers and consider optional search/favorites.
- Evidence and scope: [constellation and field-guide record](features/constellation-and-field-guide.md).

## Spawn-city refinement — 16 October 2026

- [x] Local architectural pass: repair church roof/rear enclosure, refine facade
  and interior, fix house flue collisions, coordinate timber palettes, furnish
  upper floors, and refine the bank and town hall. See the
  [city refinement record](features/town-architecture-refinement.md).
- [ ] Owner visual walkthrough of the new city before publication; existing
  generated towns are not automatically rebuilt.
- [x] Local town NPC/wayfinding slice: Nordic-inspired 18-skin town roster,
  13 readable shop/civic/directory signs, a framed north-up town plan and free
  portable copies from guards (crouch + use), and conversation pauses.
  See [town NPCs and wayfinding](features/town-npcs-and-wayfinding.md).
- [ ] Owner review of NPC appearances, sign readability and map navigation.
- [ ] NPC quality: daytime work, evening tavern gatherings and night rest, with
  posted shop hours and an always-available essential-trade fallback.
- [ ] NPC quality: role-specific idle/work animations (baker kneading, florist
  tending, guards watching gates), prioritizing readable actions over constant motion.
- [ ] NPC quality: local dialogue about their work, directions and recent events;
  make bakery/tavern services real before advertising them as functional shops.
- [ ] NPC quality: small civic commissions using Jugs and the existing contribution
  systems; rewards should introduce farming, technology and magic without gating them.
- [ ] NPC quality: give repeated residents individual hairstyle/accessory variants
  and relationships; preserve names, persistent identities and the approved Styx likeness.
- [ ] NPC quality: branch mentors with short, practical lessons using existing
  research and encyclopedia routes. Let Styx demonstrate botany/astronomy and a
  public workshop demonstrate electrical power charging a Ley Pylon; avoid a
  second mandatory quest gate or a duplicate magic progression system.
- [ ] Proposed next slice: one public workshop demonstrating connected technology
  and magic using existing machines, benches and encyclopedia guidance.
- [ ] Design civic commissions, NPC routines and an outer player workshop/shop
  district using existing currency and protection hooks. Plot ownership still
  needs a design or compatible claim integration. These are proposals,
  not implemented systems.

## Selected technology completion queue — 14 October 2026

**16 October owner instruction:** finish and reconcile packages 1–7 with main and overlapping drafts, run local validation and prepare a separate owner-controlled playtest. Do not push or merge this work to main before owner review. Current evidence and draft decisions are recorded in [the local integration record](features/tech-seven-local-integration.md).

| Package | Implementation status | Remaining work |
|---|---|---|
| 1. Advanced ceramics and precision components | Integrated locally; automated checks passed | Owner playtest / release checklist; see [feature record](features/advanced-ceramics-and-precision.md) |
| 2. Stainless steel and structural alloys | Integrated locally; automated checks passed | Owner playtest / release checklist; see [feature record](features/stainless-steel-and-structural-alloys.md) |
| 3. Batteries and power management | Integrated locally; automated checks passed | Owner playtest / release checklist; see [feature record](features/batteries-and-power-management.md) |
| 4. Advanced electronics and automation | Integrated locally; automated checks passed | Owner playtest / release checklist; see [feature record](features/advanced-electronics-and-automation.md) |
| 5. Phosphate processing and fertilizers | Integrated locally; automated checks passed | Owner playtest / release checklist; see [feature record](features/phosphate-processing-and-fertilizers.md) |
| 6. Synthetic fuels and recovery | Integrated locally; automated checks passed | Owner playtest / release checklist; see [feature record](features/synthetic-fuels-and-recovery.md) |
| 7. Cryogenics and rocket fuels | Integrated locally; automated checks passed | Owner playtest / release checklist; see [feature record](features/cryogenics-and-rocket-fuels.md) |

- [x] Package 1: reuse Pulverizer, Conditioner, Metal Press and Industrial Oven; add
  one shared Precision Finisher for ceramics, shafts and lenses, with working-head
  animation, real connection points, recipe previews and persistent locks through
  the existing machinery systems.
- [x] Add reachable porcelain/alumina/zirconia routes, reusable finishing heads,
  finished components and immediate motor/upgrade/cable/optics/polymer-tool consumers.
  Preserve old recipes, machine IDs, inventories and port layouts.
- [x] Add the Ceramics and Precision Handbook/Encyclopedia chapter and component art.
- [x] Package 2: regional chromite, concentrate and ferrochromium feed stainless
  steel; existing aluminum and magnesium feed structural alloy. Reuse the Mineral
  Conditioner, Arc Furnace, Alloy Smelter, Metal Press and Precision Finisher.
- [x] Add stainless fittings, ceramic-lined vessels, finished valves, lightweight
  frames and panels with alternate chemical equipment, pipe/tank, rocket casing,
  Zeppelin and Howitzer recipes. Preserve all original construction routes.
- [x] Add alloy material/storage forms, component art and the Stainless and
  Structural Alloys Handbook/Encyclopedia chapter. See the combined integration record for current validation.
- [x] Package 3: lead-acid buffering; spodumene/LiPF6/formulated-electrolyte/LFP cells;
  mineral vanadyl-sulfate electrolyte; bounded paired flow tank modules and a separate
  cell-stack rate upgrade. Reuse existing processors and preserve old battery recipes.
- [x] Add local battery modes, charge priority, output limits and minimum-charge
  reserves, shared transactional tick budgets, module status and battery guide pages.
  Local implementation only; compatibility and playability remain unverified.
- [x] Package 4: high-purity silicon, shared crystal pulling/slicing/polishing,
  sulfuric/peroxide cleaning, Novolac-DNQ resist, TMAH development and HF etching.
  One new Wet-processing Bench; reuse Conditioner, Bulk/compact Reactors,
  Electrolytic Separator, Arc Furnace, Precision Finisher, Lithography and Assembler.
- [x] Add reusable pattern tooling, advanced chips and useful speed/efficiency/card
  assemblies. Existing circuits, recipes, machine slots and ports stay compatible;
  Lithography appends one retained pattern slot. Compatibility is still unverified.
- [x] Add server-authoritative local recipe priorities and stock targets to formed
  machines, eight saved rules, card gating, reset and in-flight output accounting.
  Add icons, connected bench geometry/animation and Handbook/Encyclopedia pages.
- [ ] Package 4 follow-up after 1–7: old lithography saves, tool/slot isolation,
  blocked reagent/waste handling, multi-lane targets, priority/lock interaction,
  card removal/reload, two-player controls and UI/visual playtest. Remote warehouse
  controls and silane/deposition remain later; spent-wafer water recovery is locally implemented in 6.
- [x] Package 5: phosphate grinding, dissolution, slurry filtration and industrial
  phosphoric acid; reuse Pulverizer, Dissolution Chamber, Filter Press, Conditioner,
  Bulk/compact Reactors and Industrial Oven. No new machines.
- [x] Add crop-group nitrogen and triple-superphosphate fertilizers, mixed-farm blend,
  compost/digestate carriers, typed sprinkler loads and existing hydroponic nutrient
  and battery precursor consumers. Preserve ordinary fertilizer and compost routes.
- [x] Add paid gypsum-residue preparation, plaster/plasterboard with slabs/stairs,
  native icons and Handbook/Encyclopedia integration. Implementation only, unverified.
- [ ] Package 5 follow-up after 1–7: tag membership and farm balance, tall-crop
  deduplication, protection boundaries, typed/legacy sprinkler saves, hopper and
  companion insertion, unload/drop duplication, blocked outputs and gypsum recovery.
- [x] Package 6: separate primary cobaltite ore, survey discovery, shared mineral
  preparation and reusable cobalt/ceramic catalyst; clean CO/H2 synthesis and
  optional gas balancing in the existing Chemical Infuser. No new machines.
- [x] Refine Synthetic Crude through the existing tower into standard fuel fractions;
  retain existing finishing, vehicle/generator fuel IDs and upstream cleaning routes.
- [x] Treat wafer mixture, recover process water and stabilize its concentrate using
  Conditioner/Distillation Station; explicit clay/lye/calcite inputs and construction
  consumer. Add native resources, owner cobalt art and Handbook/Encyclopedia pages.
- [ ] Package 6 follow-up after 1–7: cobalt terrain/startup, tool retention, output
  backpressure and Infuser water drain, refinery balances, waste recovery specificity,
  complete upgraded energy/material economy, saves and owner-led visual playtest.
- [x] Package 7: reuse the Cryogenic Liquefier with retained methane and advanced
  hydrogen cooling modules; keep the original untooled LOX recipe. Reuse the
  Distillation Station for powered, volume-matched gas recovery. No new processors.
- [x] Add shared 32/64-bucket insulated tank variants with centered connectors and
  retained fluid drops; preserve existing tanks and avoid boil-off maintenance.
- [x] Add hydrazine/N2O4 through the Bulk Reactor, residue recovery through the
  Conditioner, sealed propellant ingredients through the compact Reactor, and four
  loaded motor assemblies through the Rocket Workshop. Extend existing survey,
  weather, delivery and line payload recipes while preserving ordinary motors.
- [x] Add native item art, silver/olive tank models and Cryogenics and Rocket Fuels
  Handbook/Encyclopedia guidance. All seven packages remain local; automated results are in the integration record.
- [ ] Package 7 follow-up when authorized: old liquefier saves, tool insertion/removal
  and hopper isolation, tier gating, energy/volume round trips, blocked outputs,
  mixed-capacity tank groups, break/place/save preservation, motor substitutions,
  incorrect propellant pairs, recipe-view/tool previews and owner-led visual review.
- [x] Owner-authorized local compilation, full data/icon audits, shared recipe ledgers,
  battery/automation/cooling regressions, and server tests with optional mods present
  and absent. Focused client rendering and a separate 19-station showroom.
- [ ] Manual survival progression, full factory economy, representative performance
  and broader release cases in the feature checklists. Automated coverage does not
  mark every manual scenario in those checklists complete.
- [ ] Owner-led local review followed by two-player manual verification before release.
- [x] Reconcile current main and overlapping draft proposals locally. Main
  `2804ba015` is included; canonical chains and the separate deferred power-plant,
  ruthenium and boron/solar drafts are listed in the integration record. No draft
  was remotely merged, rewritten or closed as part of this local playtest work.

## Howitzer-Walker (owner request, 13 October 2026)

- [x] **Howitzer-Walker implemented and owner playtested.** Main includes its model/animation integration, crafting, fuel/ammunition, movement, aiming, shelling, jump/stomp and server-authoritative controls. PR #356 adds continuous mouse aiming and hides the pilot model while driving. See [Howitzer-Walker](features/howitzer-walker.md). Two-player manual verification remains open.

## MUST: consolidated machine recipe interface

Owner requirement, 13 October 2026, for the Concentrator, Gas Scrubber, Filter Press and Industrial Oven, then other multi-recipe machinery. See [consolidation and compatibility](features/machine-consolidation-playtest.md). Implemented in the [recipe interface](features/machine-recipe-interface-playtest.md) and included in the owner-authorized [factory integration](features/owner-machine-integration.md). Automatic mode remains available; choosing a row only previews it until Lock recipe is clicked.

- [x] **MUST provide a searchable recipe/product picker** so players can choose what to make before inserting materials; show supported recipes and any unmet upgrade requirements.
- [x] **MUST preview exact inputs, retained catalysts/tools, outputs, byproducts, time and power**, including which tank/hatch receives each output.
- [x] **MUST implement a persistent, server-authoritative recipe lock for automation.** A locked machine may only start the selected recipe; it must not silently switch when other inputs arrive. Save/reload and multiplayer must preserve this selection.
- [x] **MUST explain why work cannot start**: missing ingredients, incompatible leftovers, missing upgrade, full/wrong output, power or pause. Never delete tank contents or consumed batch inputs when changing selection.
- [x] **MUST preserve running batches when selection changes** and apply a new selection only to subsequent work; validate permissions, recipe eligibility and all client requests on the server.
- [ ] **MUST complete a two-player manual server playtest** of selection, lock, automation and reload before release. Automated server tests cover selection, persistence, input/output blocking and shared viewer state; the native client test covers controls and readable layouts. Automatic remains an explicit selectable mode.

Local consolidation checkpoint: four recommended machines share the former nine specialists' processes. Old block IDs and construction recipes remain supported. Included in the owner-authorized factory integration; combined human multiplayer verification remains open.

Local October 13 checkpoint: [fluorochemistry and PTFE](features/fluorochemistry-playtest.md) uses main's regional fluorite deposits and adds concentrated acid, HF, named fluorocarbon intermediates, PTFE powder/shaping/sintering, useful seals/fittings/liners and a larger two-lane reactor. It extends the [ordinary polymer factory](features/polymer-factory-playtest.md). Included in the owner-authorized factory integration.


Local implementation, 12 October: [coastal magnesium, staged titanium and Polymer Molding Press playtest](features/coastal-metals-and-polymer-playtest.md). Eight new stations, six reusable molds and current product consumers; validation/package details are tracked in that record. Local-only, not merged to main. Broader alloys, polymer grades and bulk plants remain follow-ups.

**12 October local implementation:** [Expanded fermentation and first aluminum refinery](features/biorefinery-and-aluminum-playtest.md) adds seven stations, connected ethanol/stillage processing and staged bauxite/lye recovery to existing aluminum products. Local playtest branch; not published to main. Larger variants, demanding fuel dehydration, alloys and new specialized aluminum products remain future work.

This list tracks concrete owner-requested follow-ups. An unchecked task is planned work, not implemented gameplay. See the [roadmap](ROADMAP.md) for the wider delivery direction and [existing-content map](WHAT_EXISTS.md) for current source. Feature details and dependencies belong in their linked briefs.

See the [organized implementation and next-work index](INTEGRATION_STATUS.md) for the current integration and priorities.

## Ars Goetia summoning and pacts

- [ ] Implement a shared summoning and pact system for all 72 Ars Goetia spirits, including Stolas.
- [ ] Begin with Stolas connected to Styx, botany, astronomy and minerals; expand through shared definitions and server-authoritative costs, permissions and persistence.

This is future gameplay, as recorded in the [roadmap](ROADMAP.md#owner-requested-todo-ars-goetia-summoning-and-pacts).

## Jugcraft Encyclopedia

Local implementation, 12 October: [acid factory and first Encyclopedia](features/acid-factory-and-encyclopedia.md) adds inventory/K access, searchable canonical guides, suggested route diagrams and personal advancement-backed goals. Await local review; the broader polish/editorial checklist below is not all complete.

Owner-requested 6 October 2026. Independent feature brief: [Jugcraft Encyclopedia](features/jugcraft-encyclopedia.md). All ten supplied screenshots are included in the [reference gallery](#reference-gallery) below and archived in the [reference folder](images/jugcraft-encyclopedia/README.md).

- [ ] **Build the full Jugcraft Encyclopedia UI**, covering technology, magic, detailed pathways and integrated quests.
- [x] Add an inventory entry button and a configurable keybind. Both open the same Encyclopedia without crafting, carrying or using an Encyclopedia item. (Concordance Chunk A's first slice; its client test passed in CI run 38435014953; not yet merged; [record](features/arcane-concordance-first-route.md).)
- [ ] Provide a home view explaining the overall machinery/technology route and the overall magic route.
- [ ] Add chapter/specialty navigation and in-depth pages for each pathway within both routes, including useful products, entry requirements, process steps and optional branches.
- [ ] Explain the essential technology path and the substantial pre-electric workshop's sideways possibilities. Show steel and electricity as parallel capabilities under the owner's planning direction.
- [ ] Cover technology specialties such as ceramics/construction, mechanical power, metals, electricity, logistics, industrial agriculture, mineral separation, chemical refining, recovery/pollution and later precision/expedition systems as those features become available.
- [ ] Give magic its own overview and detailed school/ritual/spell/apparatus/reagent pathways, with useful crafting, cultivation, exploration and combat applications.
- [ ] Design route graphs with readable connections, grouped nodes and prominent milestones. Distinguish actual required materials/capabilities from recommendations, trade alternatives and suggested quest order.
- [ ] Provide detailed machine/material/spell/ritual entries and recipe/process links using canonical game data; clearly label planned versus implemented content.
- [ ] Include quests linked to their pathway explanations, with objectives and visible progress/completion. Keep the guide useful without requiring every player to complete every branch.
- [ ] Decide quest objective types, detection/consumption rules, reward policy, spoiler rules and individual/shared-party progress before implementing those behaviors.
- [ ] Implement authoritative quest completion and any approved rewards with stable identifiers, persistence and duplicate-claim prevention.
- [ ] Design search, navigation/history, accessibility and readable states across supported GUI scales; consider optional bookmarks and pinned goals.
- [ ] Decide compatibility for the existing Engineer's Handbook while preserving saved registrations. The new Encyclopedia itself is not an item.
- [ ] Build an original Jugcraft visual presentation informed by the ten references, without importing their depicted artwork or requiring an external quest mod by assumption.
- [ ] Verify both entry points, route/content accuracy, GUI behavior, bounded performance, and relevant two-client/restart/reconnect/quest persistence behavior.

A [first-slice prototype](features/jugcraft-encyclopedia.md#first-slice-entry-points-and-route-home) of the entry points and the home's two routes is proposed in a community draft PR. These boxes stay open until the owner accepts it and it is verified in game. Exact visuals, the final default key, quests/rewards and release order remain to design. The feature is broader than the industrial plan: it must independently explain magic and the deeper pathways in both routes.

## Progression and balance

- [ ] Tune the minimum fresh-world route to first electricity toward **2–4 active hours when following known recipes**, excluding optional building detours. This is an approximate playtest target, not a timed gate or a measured result.
- [ ] Map the reachable essential ceramics/mechanical workshop/electrical component route and its bootstrap requirements, with parallel steel progression.
- [ ] Place substantial construction, agricultural, mineral and recovery choices beside the essential route; do not require completing every specialty before moving to another capability.
- [ ] Validate the route in representative solo and trade scenarios, then adjust material quantities, processing time and power costs from actual playtest evidence.

Related owner planning records: [industrial agriculture, PR #208](https://github.com/jimbozoomer-byte/jugcraft/pull/208), [mineral sands/refining, PR #209](https://github.com/jimbozoomer-byte/jugcraft/pull/209), and [waste/recycling/pollution, PR #210](https://github.com/jimbozoomer-byte/jugcraft/pull/210). These are planning work, not claims that those proposed systems are implemented.

- Regional mineral-sand world generation and finite reserves are implemented and visually accepted by the owner in [PR #354](https://github.com/jimbozoomer-byte/jugcraft/pull/354), public branch `speedygroyper:codex/regional-mineral-sands`; not yet merged. See the [deposit implementation record](features/regional-mineral-sands.md). Dedicated extraction and chemical separation/refining remain separate work.

## Art flagged for redo (contributor review, 19 October 2026)

The contributor reviewed the textures Claude made for their fork and flagged these for better versions. Some are still only on unmerged fork branches. Show redrawn art to the contributor before applying it, and follow [ITEM_ICONS.md](ITEM_ICONS.md) and [NATURAL_TEXTURES.md](NATURAL_TEXTURES.md).

- [ ] Fluid textures (`textures/block/<fluid>_still.png`, and `_flow.png` for liquids): biogas, carbon dioxide, carbon monoxide, concentrated sulfuric acid, dinitrogen tetroxide, electronic-grade sulfuric acid, ethylene dichloride, hydrazine, hydrochloric acid, hydrogen chloride, liquid hydrogen, liquid methane, methane, methanol, methyl chloride, methylamine, monomethylhydrazine, phosphoric acid, raw syngas, sodium hypochlorite, sulfur dioxide, sulfur trioxide, vinyl acetate.
- [ ] Bucket icons (`textures/item/<fluid>_bucket.png`): concentrated sulfuric acid, dinitrogen tetroxide, electronic-grade sulfuric acid, ethylene dichloride, hydrazine, hydrochloric acid, liquid hydrogen, liquid methane, methanol, methylamine, monomethylhydrazine, phosphoric acid, sodium hypochlorite.
- [ ] Blocks: `aged_maple_planks`, `magnalium_block`, and the lead-acid battery's `el_battery_case`, `el_lead` and `el_terminal_red`.
- [ ] Items: `cola`, `control_rod_assembly`, `glucose_syrup`.
- [ ] Titanium tools: axe, hoe, paxel, pickaxe, shovel, sword.
- PVAc glue keeps its redrawn icon (`claude/pvac-glue`, e1092c00b); it is not flagged.

## Industrial machine models textures and dimensions

Owner-requested 10 October 2026. Independent art specification: [industrial machine models, textures and dimensions](features/industrial-machine-models-and-textures.md). It covers 79 unbuilt machine/module forms and later candidates, with 23 additional larger variants. All sizes are **width × depth × height**, each axis 2–6 blocks; several installations are 6×6×6.

- [x] Describe silhouettes, modeled mechanisms, textures/palettes, interfaces and bounded motion for the unbuilt industrial forms across the connected plans. Distinguish existing foundations, future forms and optional later candidates.
- [ ] Turn the briefs into detailed models and textures in focused implementation work, preserving the familiar family appearance in larger variants and the specified 2–6-block envelopes.
- [ ] Reuse or recolour suitable owner-library art with exact source/transform provenance and preserved animation metadata. Finish backs, undersides, interior surfaces and UVs.
- [ ] Define actual multiblock occupancy, entry parts, placement/formation and moving-group bounds alongside independently reachable construction recipes. Keep shared processing roles rather than compulsory duplicate machines for every product.
- [x] Build the shared form foundation, package 1 of the [factory implementation plan (PR #302)](https://github.com/jimbozoomer-byte/jugcraft/pull/302): validated 2–6-block envelopes with structure/clearance/access positions, placement that names obstructions, port-only transfers, protected tool sockets, eight-state status, durable batch escrow with reserved outputs, entry/expanded/bulk lanes and legacy tank-layout migration. See [industrial machine foundation](features/industrial-machine-foundation.md); tested on test-mod forms only.
- [x] Package 2, first slice: the Electrolytic Separator, the first real form, with its sliced model from the art brief, the selected 20-plate construction, capability recipes for water and brine at the selected baseline, and lye kept with brine stopping when full. See [Electrolytic Separator](features/industrial-electrolytic-separator.md).
- [x] Package 2, second slice: the Gas Burning Generator, the shared hydrogen/methane generator and the first generator form, burning hydrogen at the selected 128 JE/t from the plan's steel bill, with its turning coupling and fan. Generator forms join the foundation: fuels listed on the form and power-out ports. See [Gas Burning Generator](features/industrial-gas-burning-generator.md).
- [x] Package 2, third slice: the Chemical Infuser, the shared gas-reaction form, with its sliced model and turning fan from the art brief, the plan's starter bill, its water drain and hydrogen chloride from hydrogen and chlorine at the plan's 9,600 JE a batch. See [Chemical Infuser](features/industrial-chemical-infuser.md).
- [x] Package 2, methane power: carbon dioxide from coal in the Oxidizer, CO2 methanation over the Nickel Catalyst Bed in the Infuser with its water drain, and methane as the Gas Burning Generator's second fuel at 256 JE/t. See [methane power](features/industrial-methane-power.md).
- [x] Factory payoffs: the Combined Cycle Power Plant (2,048 JE/t on methane or raw syngas, the big power spike), the Fermenter's, contact acid's and hydrochloric leaching's advantages over the simple routes, clearer Infuser tank names, and advancements for the three big milestones ([factory payoffs](features/industrial-factory-payoffs.md)).
- [ ] Package 2, the rest of the starter gas and acid factory:
  - hydrogen chloride absorption into aqueous acid, and its consumers (the [Bulk Chemical Reactor](features/industrial-bulk-chemical-reactor.md) now makes hydrochloric acid and leaches ores with it; the Chemical Reactor also dissolves calcite in it for [carbon dioxide](features/calcite-co2.md) and zinc for [hydrogen](features/zinc-hydrogen.md); later consumers remain);
  - contact conversion and absorption, for the sulfuric acid chain (implemented in [contact sulfuric acid](features/industrial-contact-sulfuric-acid.md), with the vanadium contact bed; the retained carrier-acid charge remains);
  - the Coal Gasifier with Gas Cleanup and Separation (implemented in [coal gasification](features/industrial-coal-gasification.md); its mixed residue and road/filler use are in [gasification residue](features/industrial-gasification-residue.md); numeric pollution remains);
  - portable handling.

  Local owner-playtest branch, 12 October: [starter chemistry slice](features/starter-chemistry-playtest.md) implements the four entry forms, gas recipes, permanent catalyst beds, recovered-water control, methane generation and a mixed-residue construction route. Existing portable gas cylinders handle the new gases. Await the owner's local visual/gameplay review before merging. The [acid factory follow-up](features/acid-factory-and-encyclopedia.md) now implements wet absorption in the Chemical Reactor and its retained acid carrier locally. Broader pollution integration and larger variants remain open; owner acceptance is still pending.

  Enforce the electrolysis energy floor before any expanded or bulk Separator. The data check now holds every expanded or bulk form to it ([electrolysis floor](features/industrial-electrolysis-floor.md)).
- [ ] Inspect every completed form from all sides in-game, including idle/running states, module arrangements and lighting; verify bounds, placement and persistence. The current brief provides descriptions only.

## Industrial factory implementation planning

Owner-requested autonomous planning pass, 10 October 2026. [Delivery blueprint](features/industrial-factory-implementation-plan.md), [construction and operation](features/industrial-machine-construction-and-operation.md), and [production lines and layouts](features/industrial-production-lines-and-factory-layouts.md). New numeric defaults and layout choices are proposals; earlier owner selections remain authoritative.

- [x] Prepare a substantial connected plan with nine delivery packages, entry/expanded/bulk operating profiles, shared machine construction patterns, protected tooling/atomic work, 36 production routes and eight sample factories with 71 machine placements.
- [ ] Review the proposed package order, profile savings/buffer sizes, large-plant construction investment and one-pair reference flow-bank arrangement with the owner.
- [ ] Implement the shared form/port/tool/work foundation and legacy mappings before expanding incompatible machine layouts. Preserve the six-process-tank limit unless a separate reviewed schema change is needed.
- [ ] Complete explicit mineral/solution/fuel/precursor ledgers and independently reachable construction receipts, then deliver the starter gas/acid slice with rollback/restart/upgrade checks.
- [ ] Continue the selected useful metals/fertilizers/polymers, agricultural manufacture, large fuels, precision chips, grid storage and cryogenic consumer packages through focused tested changes. Integrate recovery, pollution measurement and Encyclopedia guidance alongside relevant packages.
- [ ] Build and measure the proposed small/medium/stress factory fixtures; validate art, transfers, persistent work/storage and all supported module combinations. Layout and arithmetic checks are documentation checks, not gameplay evidence.

## Industrial chemistry, gas fuels and advanced materials

Owner-requested 7 October 2026. Independent briefs: [industrial chemistry and fuels](features/industrial-chemistry-and-fuels-plan.md) and the [complete chemical, machine and consumer catalog](features/industrial-chemical-catalog-and-routes.md). These follow-ups are planning work, not implemented recipes or tested balance.

- [ ] Map a reachable **steel-built electrical chemistry entry**, preserving the earlier workshop/first-electricity route and prioritizing gas processing, aluminum and titanium.
- [x] Record all ten owner-selected A choices in the [eighth starter-factory batch](features/industrial-starter-gas-and-acid-factory.md#recorded-starter-factory-choices-eighth-batch): forms, Separator/nickel costs, automatic heat, full first acid chain, water/lye overflow, one residue and provisional processing/generation rates. This completes planning choices, not gameplay implementation.
- [x] Record all eight A selections in the [ninth catalyst/coal batch](features/industrial-starter-gas-and-acid-factory.md#recorded-acid-catalyst-and-coal-processing-choices-ninth-batch): independent mineral catalyst supply, recirculating initial acid, shared contact conversion, sulfur/carbon feeds, optional oxygen assistance, combined cleanup and useful stabilized residue. Planning only.
- [ ] Specify the selected starter catalyst/mineral preparation and absorber carrier accounting, coal carbon/heat/yields, residue binder/processing, and remaining construction/unit/energy limits before gameplay implementation.
- [x] Record all ten A selections in the [tenth metals-processing batch](features/industrial-chemistry-and-fuels-plan.md#magnesium-aluminum-and-titanium-processing-decisions-tenth-batch), including seawater pumping from **beach or ocean biomes**, concentration, staged dry chloride feed, titanium salt recovery/cleanup, aluminum carbon/lye handling, shared purification, whole-ingot alloys and stabilized bauxite filler. Planning only.
- [ ] Specify exact beach/ocean source IDs/tags and intake validation; magnesium hydroxide/HCl preparation, drying and electrolysis receipts; titanium salt conditioning/recovery and cleanup; lye makeup; carbon/CO2 quantities; alloy ratios; and bauxite binder/yields. Audit independent startup, material conservation and complete energy costs before implementation.
- [x] Record all eight A selections in the [eleventh polymer/electronics batch](features/industrial-chemistry-and-fuels-plan.md#polymer-and-advanced-electronics-decisions-eleventh-batch): later renewable ethylene, compatible ordinary PE/PVC products, clean same-grade offcut recycling, distinct high-purity silicon, shared precision cutting, reusable pattern plates, function-related module parts and one initial spent mixture. Planning only.
- [ ] Specify renewable ethylene feed/byproduct/energy receipts, product compatibility tags, conserved product/offcut allocation and recycling losses; high-purity silicon preparation, cutting and pattern-tooling entry; module bills/caps; and spent-mixture composition/output handling and future recovery. Preserve independent startup and current consumer routes.
- [x] Record all ten A selections in the [twelfth storage/fuel/cryogenic batch](features/industrial-chemistry-and-fuels-plan.md#grid-storage-synthetic-fuels-and-cryogenics-decisions-twelfth-batch): steel-era lead-acid, staged lithium electrolyte, mineral vanadium, attached paired flow modules, local priorities/reserves, reusable cobalt tooling, synthetic-crude refining, methane before hydrogen cooling, shared insulated tanks and hydrazine first. Planning only.
- [x] Implement cryogenic tiers/conversions, shared insulated tanks and hydrazine/oxidizer/motor recipes locally in [package 7](features/cryogenics-and-rocket-fuels.md). Cobalt supply/tooling and synthetic-crude refining are local in [package 6](features/synthetic-fuels-and-recovery.md); battery preparation/control is in [package 3](features/batteries-and-power-management.md). Full energy/material and compatibility validation remains deferred; this is not a tested-complete claim.
- [x] Record **A, A, B, A, A, A, A, A** in the [thirteenth resource/reagent/residue batch](features/industrial-chemistry-and-fuels-plan.md#resource-reagent-and-residue-decisions-thirteenth-batch): galena, spodumene first, **separate primary cobalt ore (3 B)**, chromite concentrate, vanadium-bearing iron concentrates, electrochemical peroxide, novolac/DNQ and prepared gypsum-rich residue ingredients. Planning only.
- [ ] Specify source identities where still unnamed, regional IDs/tags/discovery, galena/lithium/chromite refining, independent cobalt preparation, earlier non-sulfuric vanadium contact-bed preparation and later electrolyte refining. Preserve current feed/stock IDs and consumer entry; ordinary iron/nickel is not an unaccounted coproduct source.
  - Spodumene deposits implemented and visually accepted by the owner: [PR #351](https://github.com/jimbozoomer-byte/jugcraft/pull/351), branch `speedygroyper:codex/spodumene-deposits`; [scope, tests and combined mineral preview](features/spodumene-deposits.md). Expanded lithium refining remains separate.
  - Vanadium-bearing iron deposits and the mined-feed-to-concentrate route are implemented and the inspection world was visually accepted by the owner on 14 October 2026. [PR #378](https://github.com/jimbozoomer-byte/jugcraft/pull/378), branch `speedygroyper:codex/vanadium-iron-deposits`, awaits review and merge; [scope and validation](features/vanadium-iron-deposits.md). Later electrolyte refining remains separate.
- [ ] Specify powered oxygen/water peroxide capability/reagents and novolac/DNQ precursor/formulation/developer compatibility; add paid fictional gypsum-rich residue purification/preparation and finished plaster/board ingredients with conserved material/energy receipts. No real manufacturing/cleanup procedure is selected.
- [x] Consolidate the selected routes into a [connected production map and proposed implementation work packages](features/industrial-chemistry-and-fuels-plan.md#delivery-and-remaining-decisions). Work-package grouping is an assistant proposal; gameplay implementation and exact recipe/statistics specification remain unchecked.
- [ ] Deliver the selected **first starter gas/acid factory milestone**: water/brine electrolysis, CO/H2 preparation, synthesis, HCl/sulfuric supply, portable tanks/shared pipes and usable H2/methane generation. Use the [first-factory map](features/industrial-chemistry-and-fuels-plan.md#first-delivery-milestone-starter-gas-and-acid-factory); specify reachable construction/first acid catalyst and complete budgets before gameplay. Magnesium/aluminum/titanium and alloy expansion follows.
- [ ] Build the selected shared hydrogen/methane gas-burning generator role and separate lower-output **Bio-Generator for cleaned biogas and bioethanol**; methane improves electricity per tank and upgraded gas-generator output. Audit complete processing/generation energy to prevent closed positive-power loops. The [Bio Generator](features/industrial-bio-generator.md) now burns bioethanol at 96 JE/t; cleaned biogas waits for the Digester. It also burns the [Anaerobic Digester](features/industrial-anaerobic-digester.md)'s biogas at 64 JE/t ([biogas power](features/industrial-biogas-power.md)). The 5×6×5 [Large Bio Generator](features/industrial-large-bio-generator.md) burns both a quarter better and four times as fast. The 5×6×5 [Large Gas Burning Generator](features/industrial-large-gas-burning-generator.md) burns hydrogen and methane four times as fast.
- [ ] Define distinct CO2 + 4 H2 and CO + 3 H2 methanation recipes, the reusable first catalyst bed from existing nickel/ceramic supplies, optional later ruthenium improvement, readable heat requirements and recovered water. The CO2 recipe, the Nickel Catalyst Bed and the water drain are implemented ([methane power](features/industrial-methane-power.md)), and the CO + 3 H2 recipe ([coal gasification](features/industrial-coal-gasification.md)); the optional ruthenium bed is built ([ruthenium bed](features/industrial-ruthenium-bed.md)); heat requirements remain.
- [ ] Keep gas-pipe pressurization automatic, with no player-managed compressor machines/modules, pressure tiers/settings or per-pipe pressure simulation.
- [ ] Add the earlier steel-era coal gasifier's CO/hydrogen mixture, cleanup and separation into useful gases, with substantial numeric pollution; keep CO separate from digester CO2. The Gasifier, the cleanup rack and CO methanation are implemented ([coal gasification](features/industrial-coal-gasification.md)); numeric pollution waits for a pollution system.
- [ ] Design the substantial first bulk Anaerobic Digester, with larger versions later, using the selected 60/40 methane/CO2 game mixture, usable digestate, direct cleaned-biogas Bio-Generator use and optional hydrogen upgrading into separated methane. The [Anaerobic Digester](features/industrial-anaerobic-digester.md) and the Infuser's biogas upgrading are implemented; Bio-Generator biogas burning, cleanup and larger versions remain.
- [ ] Develop the earlier milling/mashing -> fermentation -> distillation Bio-Generator fuel route, adding dehydration for later demanding fuel/blending uses and brewery CO2 collection/cleanup; preserve existing bioethanol IDs and consumers. The [Industrial Fermenter](features/industrial-fermenter.md) now ferments crops into bioethanol and collects the CO2, venting what is not collected; the 5×5×6 [Bulk Fermenter](features/industrial-bulk-fermenter.md) runs four batches at once; milling/mashing, distillation and dehydration remain.
- [ ] Add optional CO2 capture/release to industrial carbonate cement processing and optional Coke Oven gas/condensate recovery. Full CO2 capture storage defaults to continued production/excess venting, with optional stop-instead; account for material, energy and recovered/released amounts once. The stop-instead setting is built: every form with a vented tank now has an "Excess: vent / hold" switch on its screen ([vent or hold](features/industrial-vent-or-hold.md)). Coal can now be coked in the Chemical Reactor with its gas recovered as methane ([coke gas recovery](features/coke-gas-recovery.md)); the condensate remains. Cement can also be burnt in the Chemical Reactor with its CO2 captured ([cement CO2 capture](features/cement-co2-capture.md)).
- [ ] Add the earlier steel-era coal gasifier's CO/hydrogen mixture, cleanup and separation into useful gases, with substantial numeric pollution; keep CO separate from digester CO2.
- [ ] Design the substantial first bulk Anaerobic Digester, with larger versions later, using the selected 60/40 methane/CO2 game mixture, usable digestate, direct cleaned-biogas Bio-Generator use and optional hydrogen upgrading into separated methane.
  - Local implementation: [digester factory playtest](features/anaerobic-digester-playtest.md) includes the 6×6×6 digester, biogas cleaner, digestate separator, Bio-Generator, usable fertilizer residue and optional Infuser methane upgrading. The owner tested the local factory previews and authorized integration. The expanded fermentation/biofuel routes are also included; larger variants remain open.
- [ ] Develop the earlier milling/mashing -> fermentation -> distillation Bio-Generator fuel route, adding dehydration for later demanding fuel/blending uses and brewery CO2 collection/cleanup; preserve existing bioethanol IDs and consumers.
- [ ] Add optional CO2 capture/release to industrial carbonate cement processing and optional Coke Oven gas/condensate recovery. Full CO2 capture storage defaults to continued production/excess venting, with optional stop-instead; account for material, energy and recovered/released amounts once.
- [ ] Use earlier copper conductors in entry aluminum-processing equipment, with aluminum improvements later; verify construction and reagent producers without self-output gates. The entry line (Electrolytic Separator, bauxite digester, aluminate press, alumina calciner, reduction cell and arc furnace) already uses copper; `check_aluminum_bootstrap` now verifies it and its lye and coke stay reachable with aluminum withheld ([aluminum bootstrap check](features/aluminum-bootstrap-check.md)). Aluminum improvements remain.
- [ ] Verify filled gas tanks retain exact content through break, pickup and placement and reconnect through shared pipes; cover joined-tank partitioning, simultaneous transfers and multiplayer pickup. Server game tests (`GasTankHandlingGameTests`) now cover a gas tank's exact contents through break and place, breaking one tank of a joined group, a placed tank rejoining its group and its pipe line, two pushes into a nearly full tank in one tick, and tanks of two fluids side by side; and two mock players reaching one dropped tank in the same tick (exactly one gets it, with its exact contents); a real two-client playtest remains.
- [ ] Detail H2/chlorine -> HCl and selected refinery-derived ethylene -> ethylene dichloride -> vinyl chloride -> PVC, with appropriate HCl recovery/reuse; retain existing PVC, butadiene rubber and natural-rubber routes. The ethylene dichloride route with HCl reuse is implemented ([PVC with HCl recovery](features/industrial-pvc-hcl-recovery.md)); the existing routes are kept.
- [ ] Expand process-specific reagents, useful rigid/flexible/protective/heat-resistant polymers and advanced ceramic linings/insulators/cutting/precision parts. Limit purity grades to meaningful consumers; prioritize refining/electronics over broad new everyday finishes. Silicone rubber: [methyl chloride → silicone → gaskets](features/silicone.md).
- [x] Develop titanium mineral treatment -> crude titanium chloride -> purification -> magnesium reduction -> titanium sponge -> melting using compatible shared equipment. Supply first magnesium through **seawater pumped from beach or ocean biomes -> concentration -> magnesium hydroxide/HCl chloride preparation -> dry MgCl2 item -> internally heated molten-salt electrolysis**. Reuse shared distillation, integrate paid sponge cleanup and recycle separated MgCl2 from the first titanium installation. Define source validation and account for chlorine, water/residues and shared alloy demand, with independent earlier construction and no magnesium from freshwater/pure sodium-chloride brine. Preserve existing routes until replacements work.
- [ ] Add aluminum's affordable bulk electrical/lightweight structural/panel/vehicle roles; titanium advanced process/precision equipment and vehicle components; and stronger titanium tools/armor above steel with balanced statistics. Aluminum plates now stand in for steel in a bulk aluminum-sheathed cable, the zeppelin's frame and the heliostat ([aluminum roles](features/aluminum-roles.md)). Titanium plates now make rocket casings at twice steel's yield ([titanium rocket casings](features/titanium-rocket-casing.md)). Titanium tools, the paxel and 3D knight armor are implemented ([titanium gear](features/titanium-gear.md)); final integration evidence is recorded on the integration PR.

- [x] Build the selected shared **Polymer Molding Press with reusable molds** for housings, fittings, insulation, gaskets, flexible hoses and vehicle panels; use earlier supplies for the first station/tooling.
- [ ] Implement ceramic powders -> blanks -> firing -> shared grinding/polishing where needed: clay foundation, porcelain-style insulators and alumina, then specialist zirconia; shared finishing also serves metals/optics and clay-based first steel casting remains reachable.
- [ ] Develop selected **chromium/nickel stainless steel** for advanced chemical vessels/fittings/components and **aluminum-magnesium structural alloy** for frames/panels/vehicles on compatible shared alloy/forming equipment. Regional chromium-bearing ore deposits feed named chromite concentrate and shared refining; define regions, exact grades/ratios and independent producer receipts. Titanium alloys follow later.
- [ ] Begin refining with compact functional stations and optional larger bulk plants. Larger chemical machines gain **higher throughput and lower energy per unit**; upgraded small machines remain useful for smaller production lines. Balance complete process chains and retain the separately selected substantial first Anaerobic Digester.
- [x] Locally implement general storage: lead-acid, expanded LiPF6 lithium cells and modular vanadium flow storage; preserve all earlier routes. See [package 3](features/batteries-and-power-management.md). **Unbuilt, untested and unmerged.** Portable packs and specialty high-output banks stay later.
- [ ] After packages 1–7, validate all battery recipes, old saves, shared tick budgets, reserve/priority controls and module detach/unload/pickup accounting. Review lossless transfer compatibility and old/new recipe balance before release.
- [ ] Map the requested Electrolytic Separator, Chemical Infuser, Chemical Oxidizer, Chemical Dissolution Chamber and Rotary Condensator roles onto existing shared systems; preserve current IDs and automatic gas-pipe pressure. Develop **both installed upgrade chips/modules and larger advanced physical versions**, with explicit compatible capabilities and independently reachable first controls. The 4×6×6 [Bulk Chemical Infuser](features/industrial-bulk-chemical-infuser.md), the 4×4×6 [Bulk Chemical Oxidizer](features/industrial-bulk-chemical-oxidizer.md) and the 4×4×6 [Bulk Electrolytic Separator](features/industrial-bulk-electrolytic-separator.md) are the Infuser's, Oxidizer's and Separator's larger physical versions.
- [ ] Make sulfuric acid a central industrial supply with reachable sulfur oxidation/conversion/absorption and useful fertilizer/battery/mineral/petroleum consumers. Implement selected **ordinary, concentrated and electronic-grade supplies**, distinguishing concentration from purity and accounting for water/impurities/losses. Avoid first-acid catalyst dependency cycles.
- [x] Locally map electronic-grade sulfuric purification to the existing Mineral Conditioner: paid water/acid conversion with accounted spent mixture, no new pure-water currency or self-output construction gate. See [package 4](features/advanced-electronics-and-automation.md). **Unbuilt and untested.** Additional electronic-grade reagents remain later.
- [ ] Design **substantial underground fluorite deposits in snow biomes with occasional surface outcrops**, with eligible biome IDs/tags, abundance/depth, discovery and existing-world behavior explicitly reviewed. Add independently reachable fluorite CaF2 -> heated concentrated-sulfuric processing -> HF plus accounted calcium sulfate; define gas/aqueous recovery and compatible starter hardware before HF/PTFE exists.
  - Deposit/worldgen slice implemented and locally tested, including the owner's follow-up stone-outcrop improvement: [PR #343](https://github.com/jimbozoomer-byte/jugcraft/pull/343), branch `speedygroyper:codex/fluorite-deposits`; [scope, validation and inspection-world coordinates](features/fluorite-deposits.md). HF/PTFE processing remains separate.
- [x] Local implementation: **snow-biome fluorite deposits and upper exposed veins**, explicit biome tags, depth/abundance defaults, Ore Survey support and new-chunks-only placement. Independent concentrated-acid/dissolution machines supply HF gas and accounted calcium sulfate; optional powered preparation makes gypsum construction binder. See [fluorochemistry](features/fluorochemistry-playtest.md). Owner terrain/balance playtest and publication remain; aqueous/electronic-grade HF belongs to the later wafer chain.
- [ ] Add phosphate-rock wet/hydrometallurgical dissolution, slurry filtration and phosphoric-acid recovery, with triple-superphosphate fertilizer and advanced technology consumers. Add the selected optional paid gypsum-rich residue preparation into plaster/board ingredients, with explicit inputs/losses/energy/yields rather than raw-residue placement.
- [ ] Extend nitric/ammonia chemistry for ammonium-nitrate fertilizer and compatible finished game ammunition/rocketry products; review owner-requested aged pine/maple finishes separately. Ammonium nitrate is built: a three-dose fertilizer and the filling of grenades and HE rockets ([ammonium nitrate](features/industrial-ammonium-nitrate.md)). Aged pine/maple planks, stairs and slabs are implemented ([aged wood](features/aged-wood.md)).
- [x] Locally implement **named nitrogen/phosphate fertilizers, crop-group tags and mixed-farm blends**, with ammonium-nitrate/triple-superphosphate, compost and digestate inputs. Hand use and typed sprinkler loads retain optional farm specialization; ordinary compost/fertilizer remain available. See [package 5](features/phosphate-processing-and-fertilizers.md).
- [ ] After 1–7, verify fertilizer crop groups, costs, benefits and agriculture feedback loops in survival and automated farms; implementation is not balance approval.
- [ ] Connect HCl-assisted corn-starch hydrolysis to finished glucose syrup; acetic acid/vinegar to acetate monomers and PVAc adhesives/appropriate fibre applications; citrus/citric acid to preservation, sour candy and drinks; food-grade phosphoric acid to cola. The vinyl acetate/PVAc glue route is implemented ([PVAc wood glue](features/pvac-glue.md)). Glucose syrup and citric acid sour candy are implemented ([sour candy and corn syrup](features/sour-candy.md)).
- [ ] Plan acetylsalicylic-acid medicine with independently reachable precursors and defined game effects; keep pharmacy optional to industrial progression. Aspirin is built from willow or birch bark and cider vinegar in the Chemical Reactor ([aspirin](features/aspirin.md)).
- [ ] Implement the selected first advanced-chip core: **sulfuric/peroxide cleaner, TMAH developer and selective HF oxide etching**, with **novolac resin + DNQ photoactive additive -> one initial photoresist**, and a dedicated powered oxygen/water peroxide recipe on compatible shared electrochemical equipment. Connect separate shared wet-processing and lithography stations to wafer finishing/assembly; add silane/deposition later. Specify exact upstream precursors, formulation/developer compatibility, ancillary reagents/capabilities and earlier controls without self-output chip gates.
- [ ] Assemble **one general advanced chip into distinct speed, efficiency and automation upgrades** using shared circuit/assembly systems. Speed raises throughput/power draw; efficiency reduces energy per batch; first automation adds recipe priorities/stock targets, with remote/coordinated controls later. Define costs, compatibility, caps/curves and bounded stock accounting; preserve existing chip/circuit IDs and consumers during transition.
- [x] Local playtest: ethylene from refinery/bioethanol, staged PVC, HDPE/LDPE pellets, the **Polymer Extruder**, reusable dies, compatible molding and clean same-grade recovery. See [polymer factory](features/polymer-factory-playtest.md); included in the owner-authorized factory integration.
- [x] Local implementation: chloroform -> chlorodifluoromethane -> TFE -> PTFE powder -> shaped blanks -> sintered **seals, fittings and liners**, used together to build the larger Lined Fluorochemical Reactor. Shared press, kettle, paste extrusion and clean offcut recovery are connected. No retrofit or replacement-upkeep requirement; first equipment remains reachable. See [fluorochemistry](features/fluorochemistry-playtest.md); included in the owner-authorized factory integration.
- [ ] Prepare independently supplied primary cobalt ore -> compatible cobalt stock -> reusable cobalt/ceramic Fischer-Tropsch tooling. Connect coal/biomass/gas -> cleaned/conditioned CO/H2 syngas -> catalytic synthesis -> one synthetic-crude mixture -> shared refining/upgrading into gasoline and other fuels; audit complete material/energy allocations.
- [ ] Connect existing RP-1/LOX and requested methane/LOX, LH2/LOX and hydrazine/MMH/N2O4 families to compatible game rocket/station roles. Use selected **ordinary Rotary Condensator and upgraded Cryogenic Liquefier roles** with existing oxygen cooling -> methane -> more advanced hydrogen capability and insulated shared-tank variants. Hydrazine precedes MMH; define engine/oxidizer compatibility and preserve old routes and account for powered conversion without gas/liquid duplication.
- [ ] Extend existing vanadium electrolyte/Flow Battery for VOSO4-based formulated electrolyte and major grid storage, with paired functional sides, **modular tank capacity additions and cell-stack charge/discharge output upgrades**. Add the selected specifically vanadium-bearing iron-concentrate supply alongside refinery residue, attached paired modules around a bounded controller/cell stack and optional local priorities/minimum-charge reserve. Define sizes, limits, costs, control semantics, losses and atomic shared storage accounting; no routine cycle aging, no free energy from electrolyte.
- [ ] Add boric-acid/boron connections for suitable nuclear control and advanced solar manufacturing, plus later sulfamic/sulfamate terminal plating with reviewed nickel/zinc compatibility. Boric acid and the reactor's control rod assembly are built ([boric acid](features/boric-acid.md)); boron in the Advanced Solar Panel is built ([boron in solar cells](features/boron-solar.md)); plating remains.
- [ ] Deliver industrial fertilizers, refining and polymers before the new optional chemical drinks, preservatives, adhesives and medicine; preserve independently useful earlier agriculture, food and adhesive routes.
- [ ] Document actual machine roles, recipes, consumer links and optional paths in the Encyclopedia; audit reachability, gas/element units, energy loops, pollution, bounded factory work and persistence before gameplay delivery.

## Reference gallery

All ten original owner-attached UI screenshots are preserved unchanged. They are documentation/design references, not Jugcraft runtime textures. Descriptions and individual file links are in the [independent feature brief](features/jugcraft-encyclopedia.md#visual-references-and-provenance); provenance and integrity metadata are in [the manifest](images/jugcraft-encyclopedia/reference-manifest.json).

<details>
<summary>Show all ten Encyclopedia UI references</summary>

| Reference 01: chapter sidebar / quest board | Reference 02: completed magic route |
| --- | --- |
| ![Chapter sidebar and grouped quest-board icons](images/jugcraft-encyclopedia/01-chapter-sidebar-bounty-board.png) | ![Branching magic route with completion checks](images/jugcraft-encyclopedia/02-completed-magic-route-graph.png) |

| Reference 03: resource/process dependencies | Reference 04: tier and specialty navigation |
| --- | --- |
| ![Resource and process dependencies with multiple branches](images/jugcraft-encyclopedia/03-branching-resource-process-graph.png) | ![Tier and specialty chapter navigation beside progress nodes](images/jugcraft-encyclopedia/04-tier-and-specialty-navigation.png) |

| Reference 05: main and side pathways | Reference 06: thematic route columns |
| --- | --- |
| ![Main route and workshop or magic specialty chapters](images/jugcraft-encyclopedia/05-main-and-side-pathways.png) | ![Themed route columns with prominent milestones](images/jugcraft-encyclopedia/06-thematic-route-columns.png) |

| Reference 07: focused major-milestone route | Reference 08: technology, magic and encyclopedia |
| --- | --- |
| ![Focused advanced route with grouped prerequisites](images/jugcraft-encyclopedia/07-focused-endgame-route.png) | ![Technology, magic, daily-life and encyclopedia navigation](images/jugcraft-encyclopedia/08-tech-magic-encyclopedia-sections.png) |

| Reference 09: illustrated chapter overview | Reference 10: grouped logistics/resource routes |
| --- | --- |
| ![Illustrated chapter overview with guidance markers](images/jugcraft-encyclopedia/09-illustrated-chapter-overview.png) | ![Hierarchical chapters with grouped logistics and resource nodes](images/jugcraft-encyclopedia/10-grouped-logistics-resource-paths.png) |

</details>

Reference rights and exceptions to the repository's default MIT grant are recorded in the [reference README](images/jugcraft-encyclopedia/README.md) and manifest. The screenshots inform navigation, grouping, route explanations and quest states; final Jugcraft visuals remain to design.
