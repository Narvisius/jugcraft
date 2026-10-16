# Rocky-upland spodumene deposits

Status: implemented and visually accepted by the owner, 13 October 2026, public branch `speedygroyper:codex/spodumene-deposits` and [PR #351](https://github.com/jimbozoomer-byte/jugcraft/pull/351). Depends on the fluorite stone-outcrop update in [PR #343](https://github.com/jimbozoomer-byte/jugcraft/pull/343), commit `6256dd86`, which was tested and pushed first. The branch and draft PR announced the scope before implementation. OpenAI Codex (GPT-6) is the actual implementation tool, explicitly authorized by the owner.

## Approved scope

- Seven surface biomes: `jugcraft:shield`, `jugcraft:highland`, `jugcraft:basin`, `minecraft:windswept_hills`, `minecraft:windswept_gravelly_hills`, `minecraft:windswept_forest`, `minecraft:stony_peaks`.
- Overworld deposit centers Y -16..64, triangular peak Y 24; native ore size 40; one attempt per twelve eligible chunks on average. Cave exposure allowed. Actual block yield must be sampled; size is not a block-count promise.
- Use the surface-biome selection shared with fluorite, including World Designer paint above cave biomes.
- One quarter of successfully placed pockets attempt the shared stone outcrop: 3-8 visible mineral blocks, roughly 4-6 blocks across, 1-2 blocks above local ground and 1-2 buried stone layers. Preserve bounded placement, fluid/structure/obstruction checks and finite mining.
- Stone/deepslate ore forms, one spodumene item, nine-item storage, iron-tier mining, standard Fortune/Silk Touch, shared crusher, prospector and ore drill.
- Preserve existing lepidolite acquisition and lithium processing. This delivers the natural resource and stockpiling slice before server world generation; the selected expanded spodumene refining chain remains future work under the [industrial plan](industrial-chemistry-and-fuels-plan.md#resource-reagent-and-residue-decisions-thirteenth-batch).
- Use the existing `lithium.enabled` acquisition switch; keep saved registrations. New chunks only, no retrogen. No dependency or platform changes.

## Progression and implementation

Entry is ordinary iron-tier mining in rocky uplands. A player can collect, crush and compact spodumene without completing another specialty; player trade, the existing prospector and powered ore drill are optional. The producer is a finite natural ore pocket. Current consumers are the shared crusher and reversible nine-item storage; future expanded lithium refining remains a separate work package, with the existing lepidolite/lithium route preserved.

`tools/materials.py` owns the identities, balance and existing `lithium` feature ownership. The shared generator writes stone/deepslate ore, one mineral item, storage, iron-tier and conventional ore tags, Fortune/Silk Touch loot, crusher recipes and discovery data. `OreSurvey` adds the conventional spodumene ore family; the shared drill mines it without a new mining implementation. The handbook names all seven eligible biomes and explains the current processing limit.

`SurfaceLinkedOreFeature` samples the surface biome at the attempted center, then delegates underground shape to Minecraft's native `OreFeature`. The actual deposit is an irregular group of overlapping ellipsoids, with cave/rock boundaries affecting yield. Native size 40 is a geometry parameter, not forty guaranteed blocks. The shape can cross a chunk or biome boundary. Cave biomes under eligible surface land remain eligible, including World Designer paint. The feature is explicitly Overworld-only.

Spodumene and fluorite use the same stone-outcrop implementation. Seeded randomness varies the elliptical footprint, trimmed rim, upper shoulder and ore placement. At least two thirds of the above-ground blocks remain ordinary stone. Each ore block has stone directly below and a horizontal exposed face that can remain visible when snow settles. Every candidate's natural foundation, buried roots, clearance and structure intersections are checked before writing; up to eight candidates of at most 6x6 columns are tried. Gentle slopes work; water, block entities, obstructions, native structure pieces, unsupported foundations and steep ledges reject placement. A failed underground pocket never creates an outcrop.

## Save compatibility

Only newly decorated chunks gain these deposits. Existing generated chunks and old fluorite outcrops are not rewritten. `lithium.enabled=false` disables new spodumene and lepidolite decoration plus their acquisition recipes after restart; `fluorite.enabled=false` remains separate. Registry identities, saved ore blocks and inventory items remain available. There are no platform/dependency changes, migrations or global active-world state.

## Validation — 13 October 2026

Platform: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25.0.4.1, Gradle 9.6 and Loom 1.17.21. All 18 dependency versions and hashes remain those in `distribution/frameworks.lock.json`.

- `gradlew build --no-daemon`: **PASS**, all **1,385 required server GameTests**. `SpodumeneGameTests` checks both ore forms, iron/stone tool boundaries, ordinary/Silk Touch/Fortune drops, loaded crusher outputs, storage round-trip, exact biome policy, prospecting and an actual powered ore drill recovering both finite ore blocks. Test maintenance (14 October 2026): `prospectAndDrill` surveys the drill's chunk and the eight around it, not its chunk alone. Game tests stand at random places, and its two ore blocks, three blocks or less from the drill, crossed a chunk border from it in about one placement in 64, so the survey missed them and the test failed by chance; the spodumene twin failed so once in CI ([#373's comment](https://github.com/jimbozoomer-byte/jugcraft/pull/373#issuecomment-6178187056)).
- `gradlew runClientGameTest -PclientTests=SpodumeneClientGameTests --no-daemon`: **PASS**. Two normal seeds and a Designed world; natural ore sampled and preserved after save/reopen. The exported first seed also contains naturally generated fluorite, whose sampled ores survive reopening. Rendered outcrop and cutaway screenshots were inspected.
- The preceding fluorite-only stone revision passed `runClientGameTest -PclientTests=FluoriteClientGameTests` on two normal seeds, Designed terrain and the expanded surface-safety fixtures before spodumene implementation began.
- Both mineral client suites with `-PjugcraftOptionalIntegrations=false`, `fluorite.enabled=false`, `lithium.enabled=false`, `biomes.enabled=false`, `alpine_spawn.start=off` and `town.enabled=false`: **PASS**. Neither mineral remained in biome decoration, compaction recipes were absent, and both ore forms plus seven inventory items of each mineral survived reopening. The lithium test also checks removal of lepidolite decoration. The local ignored init script writes configuration under `build/run/clientGameTest/config/` after test-directory cleanup.
- `python tools/check_mod_data.py`: **PASS**, 2,197 material IDs and the complete data/recipe/art audit. `python tools/check_icon_maps.py`: **PASS**, with 27 pre-existing warnings. Repository/local-link and whitespace checks: **PASS**. All 14 packaging unit tests and all 18 locked dependency size/hash checks: **PASS**.
- The cleaned inspection save loaded in ordinary `runClient`, without `jugcraft-test`: **PASS**. The player joined at the preview spawn with the iron pickaxe and prospector; a built-in screenshot confirmed the natural spodumene outcrop rendered. This checks a disposable copy, not a launcher UI import.

| Normal world survey | Chunks | Eligible center columns | Underground spodumene blocks | Surface blocks in guarded interior |
| --- | ---: | ---: | ---: | ---: |
| `jugcraft-spodumene-1` | 256 | 84 | 1,166 | 10 |
| `jugcraft-spodumene-2`, expanded survey | 1,024 | 191 | 1,628 | 0 |

The second seed's initial 256-chunk survey had 376 underground blocks and no outcrop; expanding the survey found more pockets but still no surface marker. A quarter of successful pockets attempt a marker, and terrain can reject that attempt, so every region need not contain one. The first seed is the delivered inspection world. Its separate 256-chunk snowy-region survey contained 3,284 underground fluorite blocks and 12 surface blocks. These are bounded observations, not an exhaustive distribution or performance benchmark.

The Designed-world survey found 2,357 underground spodumene blocks and nine surface blocks across 144 rocky painted chunks. Its 64-chunk desert interior contained none. A forced placement explicitly checks Lush Caves below eligible painted land. Controlled snowy and sloped platforms each produced three ore blocks with 52 surrounding/supporting stone blocks. Failed underground placement, water, roofs, glass obstruction and hollow ground produced no surface ore or partial stone.

The test survey excludes an 18-block border when checking marker-to-pocket linkage. Connected underground groups include cave-cut and boundary fragments; the largest groups in the two ordinary surveys were 175 and 192 blocks. The approved generation settings were retained.

## Inspection delivery

The local `mineral-deposits-preview-2026-10-13` delivery contains a matching `Jugcraft-Minerals-Preview.mrpack`, standalone `Mineral-Field-Trip.zip`, JAR, instructions, tour coordinates, logs/screenshots and SHA-256 checksums. The pack embeds the save under `overrides/saves/Mineral Field Trip/`; dependencies remain official locked download entries. Builds, worlds and logs are not committed to Git. The artifact README and PR record the source commit.

Normal terrain, seed `jugcraft-spodumene-1`, creative mode and commands enabled. All mineral ores are naturally generated. One spodumene pocket has a lit viewing room with surrounding rock removed and its ore preserved; other pockets remain untouched. Time/weather are fixed for inspection. Development-only datapack selections were removed from exported `level.dat` and its backup while preserving every other NBT value.

Fabric's content-presence snapshot also retained five names belonging to test-only form blocks, items and a block entity. A scan of all 4,667 saved region records and every other save file found no actual test content. Only those five unused names were removed from `data/fabric/registry_entries.dat`, preserving all other NBT values and world files. This removes the irrelevant missing-test-mod warning; the subsequent ordinary-client load passed.

- Spodumene surface view: `/tp @s -105 95 1536`; mineral near `-111 91 1528`.
- Lit spodumene cutaway: `/tp @s -132 25 1478`.
- Untouched spodumene pocket: `/gamemode spectator`, then `/tp @s -37 -8 1523`.
- Fluorite surface view: `/tp @s 180 97 32`; mineral near `173 92 23`.
- Untouched fluorite pocket: `/gamemode spectator`, then `/tp @s 154 -2 123`.

The owner accepted the combined inspection world's spodumene deposits and revised stone outcrops for both minerals on 13 October 2026. Launcher UI import, a two-independent-client dedicated-server session and representative server load remain untested. No merge or server deployment is included.

## Art and provenance

The complete owner catalog was searched for spodumene, kunzite and hiddenite; no named matching asset was found. Reuse the established hand-authored mineral maps and layered stone/deepslate model with a distinct pale-green crystal palette. Preserve the original owner library and existing ore palettes/layouts. The shared outcrop uses vanilla stone by runtime registry reference. No Minecraft image bytes are copied.
