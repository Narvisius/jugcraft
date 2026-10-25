# Jimtafacts collection integration

## October 23 continuation status (local, unvalidated)

The current continuation adds native transport/gate variants, four-bank Emzuli filtering, heat/rotor/reactor devices, laser tables, 32 independently saved cable attachment types, item/fluid cell fill/empty and partitions, paid spatial anchors, joined plane geometry, asynchronous crafting reservations/cancellation, and the native computer/turtle/peripheral system. Original asset pixels and supplied meshes are retained where available; programmatic source geometry is adapted explicitly. The earlier encounter and 236-template/nine-structure implementations are also local, with their explicit asset/behavior limitations. Testing was deferred during authoring; the combined validation records below supersede that earlier hold. Earlier validation results below apply only to their named earlier slices. The full collection is not yet complete: larger/entity spatial migration, Lua/CraftOS compatibility and remaining industrial mappings remain open in the linked feature records.


Owner-approved scope, 23 October 2026: integrate all supplied Jimtafacts content into the single Jugcraft mod, retaining the edited assets and adapting platform-specific behavior. This is a multi-package port, not installation of the old binaries. AI assistance: OpenAI Codex (GPT-6).

## Source inventory and boundaries

The [verified collection](../../art/owner-library/catalog/edited-2026-10-23-jimtafacts/README.md) preserves 17,598 files, including 7,472 compiled classes. Artifacts, Applied Energentics and Computers classes use Java 25; the spellbook classes use Java 21; BuildCraft and IndustrialCraft contain much older Java 8/6 classes. Existing namespace labels remain in originals. No supplied executable code is run or added as a dependency. The source dependency JAR is not committed or shipped.

The branch starts from the planes/furniture/graves candidate `c459523ddac9fa1488c2802b140f41a4be20f441`. That preceding candidate and PR #461 remain independently testable. This collection has not been released, merged, uploaded to the live server or promoted to the client updater.

## Functional accessory slice

### October 23 continuation — implementation only

The current working tree goes beyond the previously validated 28-item slice below. All 49 supplied accessory/item IDs and the mimic/campsite code are now implemented locally, with original wearable geometry, 62 copied wearable textures/sidecars, source-derived poses and movement animation. The source defaults disable accessory damage/breakage, so adding durability is not a missing requirement. See [wearable fidelity](jimtafacts-wearables.md).

The continuation also adds [digital storage/workshops](jimtafacts-network.md), native [workshop computers](jimtafacts-computers.md), 14 industrial machines and 81 mapped recipes, and an expanding spellbook/armor/workshop port. The machine port reuses shared JE, recipe selection/locking, automation, heat and persistence. Original source art is retained; distinct unsupported mechanics remain explicitly tracked rather than represented by inert item registrations.

**October 23 release preparation:** the owner authorized testing and publishing the implemented portion. Combined validation is now underway; the older validation numbers below still apply only to their named accessory slice. The complete six-collection port remains in progress. See [remaining work and release gates](jimtafacts-remaining.md) for the current limitations, including nine explicitly deferred survival acquisitions.

Stable IDs use `jugcraft:jimta_<source_name>` to avoid clashes with existing Concordance relics. The data and concise player descriptions live in `jugcraft/jimtafacts/accessories.json`; `tools/jimtafacts.py` is their source of truth.

- Health, attack, knockback, breaking speed, size, jump/fall and water-movement accessories use native attributes managed by Trinkets. Duplicate copies use the same modifier ID. There is no research gate; inventory-only and cosmetic copies give no abilities.
- Snorkel, goggles, invisibility scarf, antidote, panic necklace, obsidian skull and cross necklace use server-owned effects and cooldowns. Night vision naturally fades within 12 seconds after removal, preserving potion effects. Invisibility does not hide armor. Antidote removes poison/wither, not beneficial effects.
- Fire gauntlet, vampiric glove, withered bracelet and retaliation pendants act only after nonzero unblocked damage. Permission/PvP/party checks run before secondary effects; retaliation cannot recurse. Vampiric healing is capped at two health per second. The shock pendant uses direct damage, avoiding world-damaging lightning.
- Running shoes add sprint-only speed and step height, then remove their transient modifiers. Rooted boots require standing still on grass for ten seconds per food point. The sinking charm adds oxygen reserve and controlled downward water motion.
- Universal attractor scans at most 24 nearby drops every eight ticks, checks line of sight, pickup delay, reservation ownership and both endpoint claim permissions. It never opens containers or moves reserved loot.
- Everlasting Beef and Eternal Steak are reusable food with a one-minute item-type cooldown. Three vanilla cooking routes convert beef into steak with zero recipe XP.
- Existing native grave capture handles equipped items; personal/global keep-inventory exemptions remain authoritative.

Acquisition: a 20% chance of one item in new dungeon, mineshaft, desert/jungle temple, stronghold corridor, mansion, ancient-city and End City chest loot. Existing opened/generated loot is not rewritten. Eternal Steak comes from cooking Everlasting Beef. These are exploration rewards, not new ores or currencies. They support farming/exploration/workshop use and are not added to convenience transmutation outputs.

The previously validated slice had 28 registered items and supplied icons copied byte-for-byte. The continuation above supersedes its old missing-item/model inventory, but has not superseded its test evidence. Do not label the complete Jimtafacts collection finished.

## Remaining integration packages

1. Accessory parity: remaining movement/loot/fishing/trading effects, umbrella, airborne movement/double jump, chorus rescue, portable warp, mimic/campsite spawning, source wearables and animations, original rarity/config tuning.
2. Spellbooks: map the supplied spell registry to existing magic schools; equipment, crafting, resources, mana/cooldowns, visual/audio effects, enemies/bosses, structures and progression. Preserve shared spell UI rather than add typing requirements.
3. Applied Energentics: network controller/power, cells/drives, terminals/search, item/fluid transfer, crafting jobs/CPUs, patterns, monitoring, security, spatial systems and supplied art. Persistence and no-duplication tests are prerequisites.
4. IndustrialCraft: catalogue legacy registry/config recipes, consolidate equivalent existing machines, adapt missing processing, electricity/storage, equipment, agriculture and nuclear content. Never create a parallel material or energy economy by blind import.
5. BuildCraft: legacy recipes and fluids, pipes/filters/gates, pumping/quarry/builders/blueprints, engines and robotics. Integrate claims, bounded work and existing transport APIs.
6. Computers: terminal UI, computers, networking/peripherals, turtle inventory/movement/actions and disk persistence. Supply a bounded offline runtime; do not execute source programs during intake or expose unrestricted host/network access.
7. Combined survival reachability, economy, client asset review, two-player claims, graves/keep-inventory, restart/save and performance validation; then package a local owner playtest before any release.

## Validation

- Source intake and repeated hash audit: passed for all 17,598 supplied files.
- Native accessory Java compilation: passed on the pinned toolchain.
- **32 focused server tests passed** (`accessories-final-tests.log`): Jimtafacts, existing Wayfaring equipment, graves and player rules. Includes real dungeon loot, all three cooking routes, no research prerequisite, carried/equipped distinction, sprint modifier removal, antidote and combat cooldowns, bounded vampiric healing, party protection, exclusion of player-owned spell damage from melee bonuses, reserved-drop exclusion and exactly-once grave recovery. The first run's grave assertion included starter items; clearing the mock inventory corrected the fixture.
- **Rendered client test passed** (`accessories-client.log`): all 28 stacks synchronized into the test gallery; screenshot inspected with all supplied icons visible and no missing-texture checkerboards. This checks inventory art, not the still-pending worn models.
- **Four asset/resource tests passed**: byte-identical texture provenance, resolvable modern item models, additive slot tags and idempotent regeneration preserving existing shared tags/language. Repository structure/documentation links passed.
- **Full resource/recipe audit passed** (`resource-audit-final.log`): 6,584 IDs, 22,190 block/item models, 3,729 quad parts and 176 combined models. The audit now includes the exact new registry IDs, additive equipment tags, metal-free food conversions and the existing Concordance recipe switch; no validation was removed.
- Git's staged blobs were verified against every original file: 17,597 byte-identical files, with only the documented third-party JAR intentionally excluded from Git. The local original remains preserved.
- Two-account dedicated-server gameplay, load/performance measurement, reconnect persistence and full collection parity remain untested. No hosted test or release readiness is claimed.
- No server restart or client updater promotion is authorized by this implementation checkpoint. Obtain the owner's confirmation before any later live restart and warn players for a full 30 seconds.

### Latest local continuation — fluid reactors and workshop lasers

Added native coolant/steam/finite RT heat, full vessel/port formation, industrial heat screens and original-art laser/integration tables. See [exact scope and remaining differences](jimtafacts-industry.md#october-23-continuation-fluid-heat-vessel-and-laser-tables). These changes are uncompiled and untested; tests are authored for the final combined batch. Nothing was pushed or deployed. Tool: OpenAI Codex, user-selected Astra High.

### Latest local continuation — transport, gates and workshop systems

Added 32 original-texture native transport/control variants, removable source-capacity gate modifiers with a button-only parameter editor, five gate expansions and animated source icons, incremental network dependencies across real machine jobs, wired inventory/fluid peripherals, paginated printing, advanced-monitor color/touch and linked pocket terminals. Exact native behavior and outstanding source differences are recorded in the industry, network and computer feature records. Asset generators ran to write resources; **no compilation, tests or game launch ran**. No commit, push, release, deployment or restart occurred. Tool: OpenAI Codex, user-selected Astra High.

### Further local implementation — specialized pipes and persisted workshop jobs

The transport roster is now 39: collection, stripes placement/breaking, filtered extraction and color-aware routes are implemented with original textures. Native superheated steam feeds a two-stage turbine/condensate chain. Network jobs have physical ingredient escrow, reordered prerequisites and saved cancellation acknowledgements for disconnected providers. The supplied creative-cell model has Creative-only configuration and no survival acquisition recipe.

Computers gained virtual saved programs, integer registers/branches and stock sensors; original-font 48×24 cell monitors; finite DFPWM speaker clips; loaded cross-dimensional advanced modems; and operator-only Command Computers. Original renderer/audio sources were inspected statically. No supplied executable or ROM script was run.

Resource generators wrote the new local files. The static industry registry inventory now distinguishes duplicate pipe aliases and two unused source declarations. It records 612 families: 47 consolidated native, 286 native adaptations, 277 unresolved families and two unused declarations. Families are not a count of distinct missing machines; unresolved entries still require source-by-source work, not a blanket completion label.

New conservation, cancellation, gate-parameter, pipe-routing, program-storage and steam-chain tests are **authored but not run**. No build or game launch has occurred for this continuation. The broader collection still has genuine gaps, including arbitrary multipart coexistence, large/entity spatial migration and full Lua/CraftOS behavior. Everything remains local; no release, updater change or server operation occurred.

### Further local continuation

Thirty-two supplied network attachment types now share six cable faces with separate real settings/inventories, original face models, owner/party protection, per-face provider job cancellation and stale-access invalidation. Monitors retain their item/count display; planes use their source edge-joining geometry; P2P inherited chassis render once. Item/fluid I/O ports fill or empty finite cells, fluid cells accept bucket-based partitions, and spatial anchors use bounded paid expiring chunk tickets. Emzuli has all four filter/color banks. Turtles gain real hoe farming, cargo comparison/rearrangement, block inspection and programmatic refueling. Additional scenarios are authored for the eventual combined batch; **no builds/tests have run**. The older multipart limitation above is superseded for these 32 native types, not for arbitrary external multipart APIs. AI assistance: OpenAI Codex, user-selected Astra High.

### Combined release validation in progress — 23 October 2026

The owner requested testing the current scope and preparing a coordinated release.
Unfinished systems and nine deferred acquisition placements are recorded in
[jimtafacts-remaining.md](jimtafacts-remaining.md); earlier untested continuation
notes above are historical checkpoints.

The local candidate incorporates ready PRs #460–#463 alongside the Jimtafacts
continuation. Repairs include Minecraft 26.3 loot/worldgen schema and registry IDs,
counted industrial inputs and recipe-view pages, coolant recipe consolidation,
shared equipment tags, bounded original-mesh face repairs, and source-backed
survival acquisition edges. Charged-creeper lightning capture consumes the charge
once and respects claims. Supplied original files remain unchanged.

At this checkpoint common, client and test sources compile; 41 Jimtafacts asset
tests, six recipe-view checks, 11 world-designer tests and verification of all 26
locked libraries pass. The broader Python run had 257 passes and one recipe-view
coverage failure, subsequently fixed and rerun in the six passing viewer checks.
World-loading tests found two remaining legacy loot IDs; both are corrected and
the combined server suite is being rerun. These are not full-suite or deployment
claims. Final runtime, packaged-client and exact release results must be appended
before promotion. No live restart or client-channel promotion has occurred.

AI assistance: OpenAI Codex, user-selected Astra High.
## Combined validation follow-up — 25 October 2026

OpenAI Codex, user-selected Astra High. The first complete combined server run
finished: 2,454 of 2,476 tests passed, with 22 failures. This is **not** a release
approval. The original log is local at `build/jimtafacts-runtime5-oct23.log`.

Repairs under validation include distinct rotor-blade/wood-blank crafts (the
original recipes collided with conduits and vanilla sticks), composable spell
damage reducers, one item/fluid provider per network block-entity type, and
spider tomb collision that excludes only its intended captive. Regression
fixtures now exercise survival durability, actual claim boundaries, filter
extraction, armor-bypassing damage, paginated printing and the expanded fermenter
recipe roster. Source regeneration must also retain the ship horn instrument tag
and the repaired ice-spider egg shell.

The incomplete collection systems remain in [the follow-up list](jimtafacts-remaining.md).
No candidate deployment or player-channel promotion is claimed here. The owner
must confirm the live server restart before the authorized 30-second warning.

Completed October 25 checks so far: common/client/game-test compilation, 79
in-game updater assertions, all 264 Python tests, repository/pack checks, and the
model/data audit (7,797 material IDs, 24,409 models, 3,729 quad parts and 180
assemblies). The survival audit uses the actual exported server registry and
reports zero ingredient diagnostics. All 142 focused Jimtafacts server tests
passed after the repairs. Existing-system integration and rendered regressions
remain pending; these focused results do not replace the full release CI gates.

The follow-up combined runtime selection now passes all **389 server tests**
(`build/jimtafacts-integration2-oct25.log`). It includes every Jimtafacts test,
the actual sided storage providers, 174 crafting fixtures, grounding-strap and
Nouveau defenses, fermenters, encyclopedia and affected wood/assay tests.
All four competing magic-damage reducers now compose instead of returning early
from cancellable return injections. Twenty-nine groups of ambiguous crafting
bills were separated using existing parts; spell-forge bills and crafting-table
bills stay identical. The survival audit still reports zero invalid ingredients.
The full CI suite and rendered-client checks remain required before release.

The final regenerated data audit and 267 Python tests passed locally. Six client
test classes completed successfully: accessories, wearables, computers, JEI
stations, screen fitting and the aircraft/furniture/graves gallery. Screenshot
inspection exposed a wearable fixture that showed the creative catalog instead
of the player; that fixture now uses survival and asserts the correct screen.
The first client startup also exposed eager statue-renderer initialization;
initialization is now lazy. Two static Gecko assets now declare a valid no-motion
idle animation rather than an invalid empty animation file. A focused statue
item and corrected wearable render rerun passed on October 25
(`build/jimtafacts-statue-worn-client-oct25.log`). The 46 corrected screenshots
include the statue item and 45 equipped models; inspected hat/goggle views show
the player wearing the supplied geometry, and the two animation-load errors no
longer appear. The earlier 95-shot batch includes aircraft, furniture, the guard
pilot/grave, JEI pages and fitted machine screens. These isolated tests do not
control the owner's player profile and are not a hosted two-account test.

Linux Foundation exposed three asset-fidelity checks comparing archived CRLF
JSON with normalized runtime LF JSON. Text checks now compare all parsed data;
binary texture hashes remain exact, and source originals remain untouched. The
seven affected checks pass locally; the next CI run must validate Linux as well.

Linux Foundation passed on `cf7bf7f2a4`. The broader Build run then found
platform-dependent directory enumeration in recipe/loot/music reports and tiny
libm differences in compass meshes. Source enumeration is now sorted and rotated
mesh coordinates/normals are serialized to nine decimal places. A regression
compares forward and reversed directory enumeration without changing source files.
The companion machine-port fixture now expects the five additional Jimtafacts
processors. The free-roaming breeding fixture uses a smaller temporary pen so
parents cannot wander beyond Minecraft's eight-block partner search range; the
larger slaughter fixture and all behavior/conservation assertions are retained.

An isolated packaged dedicated-server rehearsal started and restarted successfully
with the complete candidate dependency set. A copied test world's furniture
cabinet retained seven diamonds and its cargo airship survived restart
(`build/jimtafacts-packaged-world-start-oct25.log` and
`build/jimtafacts-packaged-world-restart-oct25.log`). This used localhost port
25576, not the live world. The rehearsal patch from the published October 22.3
bundle contains only `mods/jugcraft.jar`: no removals, configuration overwrites or
world files. Final release files must be rebuilt from the final tested revision.
Live deployment and client-channel promotion still await the release gates and
the owner's explicit server-restart confirmation.

The expanded client suite also exposed a Modonomicon 2.16.0 research-packet race:
`SyncResearchStateMessage` held the live mutable research state while Netty encoded
it on another thread. A narrow compatibility mixin snapshots its four public
collections when the message is constructed on the sender thread. The regression
queues a message, resets and repopulates live state, then round-trips the queued
packet and checks the original facts, nodes, values and stages. This preserves the
pinned library and its progression state; no external dependency code is copied.
The natural garnet survey now searches gravel shores at sea level, avoiding
repeated mountain fragments that correctly reject sediment lenses. Production
biomes, rarity, terrain safety and the two-seed persistence assertions are unchanged.

After these repairs, all 268 Python tests pass. The focused companion client run
passes 43 groups / 737 assertions plus all 36 livestock-population assertions
(`build/jimtafacts-peepo-client-oct25.log`), including a dedicated-server reconnect.
The three framework server tests, including the research snapshot regression,
passed locally; the alchemical client group also passed in Linux CI. The second
mineral seed exposed a further survey limitation: small coastal fragments did not
contain a successful rare garnet attempt. The bounded survey now alternates shores
with the production biome tag and yields server ticks between regions so temporary
chunk tickets can expire. Production generation and every depletion/persistence
assertion remain unchanged. An isolated second-seed run found 446 natural garnet
reserve blocks, including 37 visible blocks, then verified all four mineral
families after reopening the world; the alchemy client test passed in the same run
(`build/jimtafacts-mineral-client3-oct25.log`). The complete mineral client class
then passed in 14m 36s, including designed terrain, both fixed-seed natural worlds,
all four mineral families, depletion, inventory units and remaining reserves after
reopening (`build/jimtafacts-mineral-final-oct25.log`). Final CI gates remain
required before release.

The complete server jobs have a 90-minute wall-clock budget. The earlier local
2,476-test run finished in 76 minutes (71 in the test server), so a 60-minute job
could terminate a still-progressing suite before generation/packaging finished.
No tests, assertions or individual game-test deadlines are removed or relaxed.
The first Linux full-server jobs reached that old 60-minute job limit while still
progressing through batches 37/39, with no logged gameplay assertion failures.
The server test JVM previously had no explicit heap cap; both test JVMs now use
the same 4 GB limit and exit immediately on memory exhaustion. This is a test
runner setting, not a hosted-server configuration change or a full-suite pass.

A fresh local package from `7bde6ba335` includes the research snapshot repair and
passes the distributable check for four mixin configurations / 105 classes. Its
patch against the published October 22.3 bundle again changes only
`mods/jugcraft.jar`. The isolated existing-world server started successfully with
that JAR, retained the cabinet's seven diamonds and the saved cargo airship, and
saved/stopped cleanly (`build/jimtafacts-packaged-world-final-oct25.log`). This
remains a local rehearsal, not a hosted deployment or two-account join test.
