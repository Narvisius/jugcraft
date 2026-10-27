# Overworld quartz and starter silicon

Status: implemented and locally validated on `speedygroyper:codex/overworld-quartz-starter-silicon`, 21 October 2026; draft [PR #449](https://github.com/jimbozoomer-byte/jugcraft/pull/449). Inspection world opened in the ordinary client; awaiting owner preview approval. AI assistance: OpenAI Codex (GPT-6).

## Scope

Finite stone/deepslate quartz veins supply existing vanilla quartz before Nether travel. Primary surface biomes: Highland, Shield, Basin, Windswept Hills, Windswept Gravelly Hills, Windswept Forest, Stony Peaks and Stony Shore. Secondary biomes: Forest, Birch Forest, Taiga, Old Growth Pine Taiga, Old Growth Spruce Taiga, Woodland, Coniferous Forest, Maple Woods and Aspen Glade. Centers Y -16..96, triangular peak Y40. Primary size20 at one attempt per four eligible chunks; secondary size12 at one per twelve. Primary successful pockets attempt the existing supported stone outcrop one third of the time, exposing 3–6 quartz blocks. Size and attempt counts are not guaranteed ore yields. Stone pickaxe, ordinary Fortune/Silk Touch; new chunks only.

Extend the existing Small Crucible Furnace with a permanent heat insert (two brick blocks and a fired basic ceramic lining) and a grounded side-mounted bellows (four planks, two leather, one iron shaft). One quartz and one charcoal produce one silicon in 200 active ticks, consuming ordinary heating fuel separately. Manual pumping toggles while the operator keeps the menu open; shaft drive takes 8 KE/t, with equal speed/yield and no stacking. Missing airflow/fuel and blocked output pause without losing committed ingredients, progress or unused fuel. No wear or replacement upkeep. Existing alloys remain independent of the upgrade.

The five-second Blast Furnace silicon stand-in remains during preview review. Retire it only after the new route is tested and the owner approves the preview. The Arc Furnace retains two silicon per quartz. No waterwheel, gear mold, steel or other batch work is included.

## Integration and validation plan

Reuses surface-linked deposits, shared workshop recipes/work ledger, server permissions, kinetic consumers, native menus and existing material/model generators. The existing eight inventory slots keep their positions; the insert appends slot eight. Old recipe/save data defaults `high_heat` to false. Bellows carry only one tick of paid airflow, never saved rotation. Sources may refill that buffer after the furnace ticks so block-entity order cannot halve throughput; only the furnace ticker advances work. Multiple sources cannot overfill the buffer or add processing steps.

The silicon feature gates both quartz distributions and the new process. Registered blocks remain loadable when disabled. Prospector readings use the shared quartz ore tag; crushing either ore yields two vanilla quartz. The handbook and JEI show the ceramics, insert and bellows as reusable equipment. New ores have explicit alchemy values covering Fortune III output; the upgrade parts derive values from their crafting recipes. The acquisition audit uses a fresh runtime item registry.

Focused validation covers mining, worldgen, workshop/kinetic behavior, recipes, models, compilation and rendered interaction. The inspection test samples two normal seeds and rocky/forest/desert Designed-world regions with lush cave biomes underground. Controlled fixtures check snow, gentle slopes, water, roofs, obstructions, hollow foundations and absent underground rock; a separate forest fixture checks that secondary veins never create markers.

## Art and provenance

The supplied owner-library catalog was searched for quartz and bellows. The new cream/white crystal overlays reuse the existing authored `ore_b` mineral map and layered stone/deepslate model; vanilla host textures are referenced at runtime. The new native bellows and insert reuse project `sp_wood`, `sp_iron`, `sp_firebrick` and `balloon_leather` textures. Four compression states animate the bellows; their feet remain at Y0. The furnace insert adds a visible rim. No library originals or existing mineral palettes/layouts were changed, no copied Minecraft image bytes, and no new dependency.

Related: [starter workshop](starter-workshop-batch-1.md), [selected planning](industrial-starter-workshop-plan.md), [supported mineral outcrops](spodumene-deposits.md).

## Validation, 21 October 2026

- Main, client and game-test Java compilation passed on the pinned Minecraft 26.3 / Fabric toolchain.
- All 26 focused server tests passed: `QuartzSiliconGameTests`, `StarterWorkshopGameTests`, `FluoriteGameTests` and `SpodumeneGameTests`. After the airflow display correction, all nine quartz/silicon server tests passed again. Coverage includes stone-tool mining, vanilla drops, Fortune/Silk Touch, biome policy, exact input/heat accounting, manual pause/reload, missing equipment/fuel, full output, cancellation, installed-part extraction locks, operator exclusivity, protection, feature disabling, real shaft-network production and old inventory/alloy compatibility. The display regression distinguishes a consumed stroke from reusable energy.
- Eleven alchemy value checks passed; eight acquisition/starter recipe checks passed after refreshing the item registry from the game. Acquisition audit: zero invalid ingredients or unresolved active routes; the existing 78 explicitly deferred designs remain deferred. No loop-detection rule was relaxed.
- `tools/check_mod_data.py` passed the resource, recipe and art audit. After aligning the bellows' rear socket with the shaft at 8/16 block height, all 13,197 block/item models passed the geometry rules. All 55 changed runtime resource files matched the final production JAR byte-for-byte; test classes are absent.
- `QuartzSiliconClientGameTests` passed with the Complete-pack dependencies: primary/secondary/desert Designed-world checks, safe outcrop fixtures, two normal seeds, actual mouse-toggle operation through the bellows menu, manual and shaft silicon outputs, menu-close/reopen pause, and save/reopen preservation of sampled ore and partial work. Screenshots were visually inspected. The first natural seed's tiny windswept patch had no marker; the inspection survey moved to Stony Peaks without increasing rarity or changing placement rules.

On `jugcraft-quartz-1`, 256 sampled chunks included 27 primary-biome center columns, 369 buried ore blocks and 11 high surface-marker blocks. On `jugcraft-quartz-2`, the expanded 1,024-chunk survey retained 396 buried ore blocks and no high marker. These are bounded observations, not guaranteed yields: the survey counts buried blocks at Y<=96 and unambiguous markers above Y104, and checks persistence for that exact set. Controlled rocky/forest/desert samples found 745/32/0 buried quartz blocks. Snow and slope fixtures exposed four and five quartz blocks respectively; unsafe fixtures placed no surface stone or quartz.

## Inspection world

`Quartz and Starter Silicon`, normal seed `jugcraft-quartz-1`, creative mode with commands enabled. Natural deposits are preserved; the underground viewing room removes surrounding rock. The workshop platform and its two ore samples are explicit fixtures.

- Workshop start: `/tp @s 512 240 508`. The manual furnace contains a partial silicon batch to resume; the second furnace demonstrates shaft operation. Supplies, a crafting table and handbook are provided.
- Natural stone outcrop: `/tp @s -333 159 -450`.
- Lit underground viewing room: `/tp @s -334 30 -460`.
- Another untouched pocket: spectator mode, `/tp @s -260 35 -521`.

The separate delivery copy preserves all 104 saved files. Cleanup scanned 4,647 region records and every other saved file; no test content was present. It removed only two development datapack selections from `level.dat`/its backup and five unused test-registry names from the Fabric presence snapshot. Every other NBT value and saved file was verified unchanged. External launcher import, independent multiplayer sessions and representative server load are not claimed by these checks.

The final production sources loaded a separate copy of that cleaned save in ordinary `runClient`, with the player at the workshop and the full pinned dependency set. The local delivery folder is `../quartz-silicon-preview-2026-10-21/`, containing the world ZIP, matching preview JAR, `.mrpack` with the save, itinerary and verification hashes. The original Blast Furnace silicon fallback remains pending the owner's approval of this preview.
