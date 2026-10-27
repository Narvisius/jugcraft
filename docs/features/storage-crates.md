# Metal storage crates

Implemented on 23 October 2026 from the owner's selected plan, using OpenAI Codex (GPT-6).
Base: `f904df3fd`. Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25.
Owner: @jimbozoomer-byte. No additional dependency or ore-processing route.

## Player experience

Seven single-block, mixed-item inventories use normal stack limits. They open from the front, including under solid blocks, never merge, require no power, and cannot be pushed by pistons.

| Block name | Slots | Single-chest capacity |
|---|---:|---:|
| Bronze Storage Crate | 54 | 2× |
| Steel Storage Crate | 108 | 4× |
| Invar Storage Crate | 162 | 6× |
| Stainless Steel Storage Crate | 216 | 8× |
| Aluminum Storage Crate | 270 | 10× |
| Titanium Storage Crate | 324 | 12× |
| Tungsten Storage Crate | 378 | 14× |

The screen has nine columns, up to six visible rows, a mouse-wheel/drag scrollbar, search, and a fixed player inventory. Smaller GUI heights show fewer storage rows. Search matches item display names, custom names and registry IDs without moving stacks. Filtered empty slots are hidden; clear search to place into an empty slot. Shift-click deposits across the whole inventory regardless of search. Occupied-slot and match counts explain the current view. Each viewer has independent search and scroll state.

## Recipes and progression

Bronze costs four bronze plates in a cross around an ordinary chest. Every higher tier has two crafting recipes:

- Four destination-metal plates in a cross around the preceding empty crate.
- Eight destination-metal plates surrounding an ordinary chest, bypassing preceding tiers.

Each higher tier also has an upgrade kit: four destination plates in a cross around one iron nugget. Use it on the preceding placed crate. The successful transition consumes one kit and preserves all stacks, components, custom name and facing. New slots start empty. Wrong-tier use consumes nothing and names the required crate. Active viewers close safely during an upgrade.

All 19 recipes use the specific `c:plates/<metal>` tags and existing machine/material feature conditions. The Engineer's Handbook and encyclopedia document the routes; the ordinary crafting recipe browser can discover both recipes and the kits.

Disabling a recipe's feature switch removes that acquisition route. Registrations and placed inventories remain available, so toggling a feature does not discard saved contents. All seven block IDs and the shared block-entity ID are new; existing Item Crate saves need no migration.

Aluminum deliberately follows Stainless Steel to retain Invar at 6× and Stainless Steel at 8×. This is a capacity choice, not a processing unlock gate. Existing bauxite blasting (one nugget per ore) and arc smelting (one ingot per ore) can supply aluminum before the later refinery. Eight aluminum plates and a chest directly unlock its 10× storage when those materials are available. Direct recipes have no research or prior-crate achievement requirement. The entire empty-crate ladder costs 28 plates across seven metals; the Tungsten direct route costs eight tungsten plates.

The original 32-stack, single-variant Item Crate keeps its existing ID, recipe, save data and behavior. It remains useful as an automation buffer.

## Inventory, automation and protection

The server owns every transfer. All physical menu slots have stable IDs; the client only changes coordinates/visibility when filtering or scrolling. View mappings freeze during mouse gestures. Native menu synchronization and item-component handling remain in use. Shift-click merges matching stacks before filling empty slots, across the whole inventory.

Hoppers and Fabric item-transfer consumers reach every slot from every face through the shared sided-container adapter. Existing pipe/hopper claim-boundary policy remains responsible for unattended access. Comparators measure the whole inventory with normal item stack limits. The optional Jade integration reports occupied slots only after server-side access validation.

Opening, menu actions and upgrades recheck live distance, player mode, land and structure permissions. Upgrades construct a complete replacement before replacing the placed block, close old viewers, suppress the old block's spill, invalidate stale automation references, then notify neighbors. Old menu clicks are rejected.

Breaking spills contents once; the dropped block is empty, including with Silk Touch. Creative pick-block also copies an empty crate and cannot invoke the game's 256-slot item-container codec. Correct mining requires a stone-or-better pickaxe. No fireproofing or explosion immunity is added.

The save format uses integer slot indices under `storage_items` with version 1. Vanilla's unsigned-byte container slot codec cannot represent the final slots of Aluminum, Titanium and Tungsten. Complete ItemStack codecs preserve names, durability and other components in all 378 slots. The block entity is passive and has no ticker.

## Assets and generation

Models share identical geometry across all seven tiers: four corner posts, eight connecting edge rails, three separate metal boards per face, a simple latch, and diagonal metal straps on both sides and the rear. The straps sit just 0.05 model pixels above the boards, recessed beneath the rim, with their ends extended into the corner fittings to conceal the end caps and remove corner gaps. Half-pixel recessed dark seams and highlighted board edges distinguish the individual boards. Tier-specific ribs, trim, handle variants and frame thicknesses have been removed. Model UVs sample the existing metal-block sprites' base, highlight and shadow colors without displaying their embossed square motifs; panels, frames and braces all match the corresponding metal. No new textures are needed. OpenAI Codex (GPT-6) updated the models and their generator. No tests were run for this owner-directed visual revision.

The frame texture is an unmodified copy of the authorized owner asset `art/owner-library/originals/Blocks/crate_block_site_1_new.png`, SHA-256 `c66ea663c34514a2c145dcb00496a5dd025929501002d9bc169feb8fca3067e3`, at `assets/jugcraft/textures/block/storage_crate_frame.png`. The original and library notices are preserved. No library program was executed.

`tools/storage_crates.py` owns the models, recipes, mining tags, language and handbook chapter. The normal material generator calls it after shared tags. `python tools/write_storage_crates.py` refreshes just these resources and the guide indexes. The resource manifest and data checker's registered-item catalog include the new family.

## Verification

Verified locally on 23 October 2026, on Windows 11 with JDK 25.0.4.1+1 and Gradle 9.6.0:

- Common, client and game-test sources compile; the playable JAR builds.
- All seven focused server tests pass with the normal integrations and with `-PjugcraftOptionalIntegrations=false`.
- The rendered client test passes with integrations present and absent. Actual keyboard search (including the inventory key while focused), Escape, mouse pickup, scrollbar dragging, wheel bounds, GUI scales 1 and 4, and a complete world restart pass.
- The complete `tools/check_mod_data.py` audit passes: 6,253 material IDs, data and recipes. Crate generation matches 61 JSON resources and 23 language entries. The copied owner texture matches its recorded hash.
- `scripts/check_repository.py` and `git diff --check` pass.
- The delivered JAR was compared with the generated crate resources and compiled crate classes; game-test classes are absent. SHA-256: `01bd35cf9ac7e5cf686c397672250a488fe2aa589f8554a645ef91ff4794a046`.

Client selection is `runClientGameTest -PclientTests=StorageCrateClientGameTests`; repeat with `-PjugcraftOptionalIntegrations=false` for the absence path. The focused server run sets the Java system property `fabric-api.gametest.filter=*:storage_crate_game_tests*` on `runGameTest` through a local Gradle init file. All seven remain registered for the normal CI suite.

The Windows runs used `build-local.ps1`. A local init file used full Java compilation and excluded processed resource directories from the compile classpath to avoid repeatedly hashing the existing asset collection; runtime resources stayed present. After language-only polish, the language file was copied into the existing processed resources and those two copy tasks were skipped on reruns. The packaged-resource comparison above checks the delivered result against source. No build configuration or dependency version was changed for this optimization.

Focused server coverage is in `StorageCrateGameTests`: exact capacities; all six automation faces; rollback; full comparator; final-slot save/load; stack limits; complete upgrade ladder; repeated upgrades; stale menus; hidden-slot shift-click; live permission/distance changes; competing viewers; claim ownership; all 19 real crafting recipes; and removal drops.

`StorageCrateClientGameTests` uses a disposable normal world with seed `jugcraft-storage-crates`, a gallery at (512, 220, 512), actual screen opening, filtered mouse pickup from slot 377, scrolling and replacement, no-match search, and complete world close/reopen. Screenshots were inspected at both scales and the entire seven-crate gallery rendered correctly. Screenshots and a separate inspection-world export are local evidence, not tracked player data.

## Two-client multiplayer and load validation

The opt-in `StorageCrateMultiplayerClientGameTests` launches **three separate JVMs**: two rendered clients and a normal dedicated server. Both synthetic player identities join over loopback TCP. The server is bound to `127.0.0.1`, runs offline for the local identities, and uses a fresh disposable flat world with seed `storage-crate-network`. The test controller prepares fixtures and collects evidence through local files; crate interactions travel through the game's native client/server protocol. The server does not run Fabric's synchronized game-test clock.

The 23 October run verifies independent searches on the same crate; twelve competing mouse-extraction rounds with exactly one winner and seven diamonds each; a survival upgrade while another viewer has the menu open; consumed-kit, name and content preservation; rejected stale packets after upgrade and claim revocation; and a cursor-held disconnect/reconnect. Vanilla drops a disconnected player's cursor stack into the world: this run found exactly seven dropped diamonds, and includes those entities in conservation checks.

The dedicated JVM then saves and exits, a new JVM opens the same world, and both clients reconnect. The test verifies the upgraded Steel crate and its sixteen pearls, the named/damaged pickaxe in Tungsten slot 377, and conservation of the seven diamonds across that process restart.

Load methodology: two clients keep their Tungsten menus open throughout every measurement. Two functional fixture crates remain present. Additional Tungsten crates are filled in all 378 slots with alternating 64-item cobblestone/iron stacks: 256 crates hold 96,768 stacks; 1,024 hold 387,072 stacks (24,772,608 individual items). The final stage adds 64 ordinary hoppers and 64 comparators, with comparator outputs pointing away from the hopper rows. It asserts that every hopper is enabled and transfers at least 150 items, and that all 64 comparators report signal 14 after extraction. The same 36-chunk region stays forced, with view and simulation distances both 4, mob spawning disabled and random ticks disabled. Every stage has 200 warmup ticks followed by 1,200 consecutive measurements from Minecraft's completed-tick timing ring. Used Java heap and garbage collections are observed without forced GC. Setup, population, restart and final save are outside these measurement windows. The final conservation check includes all load crates and hopper inventories.

Hardware/runtime: Windows 11, AMD Ryzen 7 7800X3D (8 cores/16 threads), Radeon RX 7900 XT, JDK 25.0.4.1+1; each JVM has a 4 GiB maximum heap. Both clients render on the same PC. Minecraft/Fabric and optional-integration versions remain the pinned versions above. These are bounded local measurements, not a supported-player-count claim.

**Final result: PASS**, 23 October 2026 at 12:12 CDT. The complete rerun finished successfully in 8m 45s, including both client exits and the dedicated server's save and exit. The release JAR and production crate code did not need changes.

| Additional filled Tungsten crates | Automation | Median tick (ms) | p95 tick (ms) | Maximum tick (ms) | Observed server heap (MiB) |
|---:|---|---:|---:|---:|---:|
| 0 | None | 0.440 | 0.755 | 1.383 | 496–681 |
| 256 | None | 0.443 | 0.664 | 16.817 | 574–782 |
| 1,024 | None | 0.471 | 0.729 | 21.904 | 523–939 |
| 1,024 | 64 enabled hoppers + 64 comparators | 0.404 | 1.616 | 43.523 | 457–1,005 |

All **4,800 sampled ticks were below 50 ms**, and each 1,200-tick measurement took approximately 60 wall-clock seconds. The active stage transferred **11,264 items** through its 64 hoppers by the end of warmup plus measurement. Every hopper passed the enabled/throughput assertions, all comparator outputs were 14, and total load-fixture contents remained exactly 24,772,608 items. Garbage collections during the four windows were respectively 0, 0, 1 (6 ms total), and 5 (19 ms total). Heap figures are observed used Java heap, not process RAM or retained-memory estimates; differences between passive-stage timings are within this run's small timing variation.

The authoritative local evidence is `storage-crates-multiplayer-run5-build.log` and the `storage-crates-multiplayer-run5` directory beside the checkout. Its final result records distinct process IDs for client A, client B and the restarted dedicated server. Screenshots and machine-readable results are also copied into the local preview's `multiplayer` folder. An earlier automation fixture unintentionally redstone-locked 32 hoppers; its measurements are excluded from the table. The final fixture directs comparator outputs away from both hopper rows and asserts every device's activity.

Reproduce with `./gradlew runClientGameTest -PclientTests=StorageCrateMultiplayerClientGameTests -I tools/storage-crate-multiplayer.init.gradle`. The opt-in init file creates a unique evidence directory under `build/`; the normal client suite skips this benchmark. Evidence includes per-process logs, screenshots, JSON measurements and a final `result.json` only after all assertions and clean exits succeed. The test harness is absent from the playable JAR.

Remote-network latency, account authentication, long-duration operation, component-heavy inventories, more than two clients, optional integrations absent in this separate-process suite, and migration of an owner's existing world remain outside this run. Earlier one-client/server tests cover optional integrations absent.


### Models v4 verification — 23 October 2026

OpenAI Codex (GPT-6) ran the rendered `StorageCrateClientGameTests` against the updated resources: BUILD SUCCESSFUL in 9m 55s. The suite exercised the seven-tier gallery, opening crates, final-slot search/pickup, scrolling, GUI scales 1 and 4, no-match search, and complete save/reopen with item components preserved. The new gallery screenshot was visually inspected.

Focused model checks passed for all seven tiers: identical geometry and UVs, existing texture references, UV range, rotated brace bounds and corner overlap, and exact resource equality with the models-v4 preview JAR. A strict whole-model 0–16 bound check initially flagged the front latch's -0.05-pixel anti-overlap offset from the shared model writer; the brace-specific checks passed without relaxing their bounds. This run did not repeat the earlier multiplayer/load benchmark because only model resources changed. Evidence: `storage-crates-models-v4-client.log` beside the checkout and `storage-crates-preview/models-v4-test-evidence/`.


### PR #467 integration fixes

The initial CI run passed all sixteen client shards and the optional-integrations-absent server job, but found missing crate alchemy values, a stale acquisition-audit registry snapshot, and a pickaxe-tag ordering mismatch. The alchemy generator now supplies the thirteen missing records; the real survival-recipe server test exported exactly thirteen additional crate/kit registrations. Targeted resource refreshes now insert crates before the later vehicle-bay entries, matching full generation. Regression checks cover value conservation, acquisition routes and repeatable tag ordering. Models and Java gameplay code are unchanged.

Local validation: all six survival-recipe server tests passed; full material generation reproduced the intended resource changes only. The full Python run passed 196 of 197 checks; the new acquisition assertion was corrected to recognize the audit's evidence-backed metal-processing classification, then all five survival-graph tests passed. The ten Winterhold tests, pinned-library check and diff whitespace check passed.
