# Regional mineral-sand deposits

Status: implemented and visually accepted by the owner after reviewing the inspection world; not merged or deployed. Implementation and local validation completed 13 October 2026. Public branch `speedygroyper:codex/regional-mineral-sands`, based on the tested fluorite/spodumene work in PRs [#343](https://github.com/jimbozoomer-byte/jugcraft/pull/343) and [#351](https://github.com/jimbozoomer-byte/jugcraft/pull/351). Actual tool: OpenAI Codex (GPT-6), explicitly authorized by the owner.

The owner approved all four families in the [mineral-sands plan](mineral-sands-and-refining-plan.md), with shallow irregular sediment lenses, visible exposures, regional biome eligibility, terrain protection and finite reserves, and accepted the completed preview with “Looks good.” Public coordination is in [PR #354](https://github.com/jimbozoomer-byte/jugcraft/pull/354).

| Family | Length × width × thickness (blocks) | Buried top | Attempt frequency |
| --- | --- | --- | --- |
| Titanium | 24–40 × 12–24 × 3–6 | 0–2 | 1 per 48 eligible chunks |
| Zircon | 20–36 × 10–20 × 3–5 | 1–3 | 1 per 64 |
| Rare earth | 16–28 × 10–18 × 3–5 | 1–4 | 1 per 96 |
| Garnet | 20–32 × 10–18 × 2–4 | 0–2 | 1 per 48 |

Titanium favors Dune Beach, Overgrown Beach and Beach. Zircon favors Outback, Lush Desert, Dry River, Desert and sandy Badlands terraces. Rare earth favors Basin, Lake District, Lush River and forest/taiga river banks. Garnet favors Gravel Beach, Highland, Shield, Stony Shore and Windswept Gravelly Hills. Actual suitable ground is required; biome names do not establish flat terrain.

## Reserve contract

The saved mineral-bearing blocks are the reserve. Each block is one unit of unprocessed feed, recovered once by mining or powered extraction. Moving and replacing harvested feed moves that unit; it does not create another reserve or reset a hidden capacity. Ordinary sand and depleted terrain never replenish it. There is no 1,000-unit-per-block multiplier. World generation only adds reserves in new chunks.

Existing titanium/monazite routes remain available. Sand separation/refining machinery and a dedicated shaft-powered extractor are later work; this slice provides natural acquisition, stockpiling and compatible existing powered extraction. Exact mineral recovery yields are not selected here.

## Implemented behavior

`tools/mineral_sands.py` owns the four stable block IDs, geometry, biome tags, rarity and source textures. Dimensions describe the randomized lens envelope, not a guaranteed rectangular volume. The edges are irregular, beds taper, thin discontinuous host-sediment bands remain within them, and a wandering surface ribbon connects to the shallow buried stock. Coastal families align with a nearby water gradient when available. These are compact sediment blocks: they stay stable when exposed by mining.

Generation evaluates the surface biome, even above a different underground cave biome, and works with serialized World Designer terrain/biome paint. Each attempt is centered in its chunk and bounded to the neighboring 3×3 chunk region. The feature rejects steep, fragmented, unsupported and entirely submerged sites. At least 20% of accepted columns must be sediment; eligible Badlands consequently require sandy/dirt terraces, not bare terracotta. Vanilla River requires a nearby forest/taiga sample. No blocks are written until the candidate has passed the checks. Each written column excludes fluid, block entities and structure pieces; ordinary terrain is replaced in place without raising the surface. Attempt frequency precedes terrain rejection, so actual deposits are rarer and seed-dependent.

Players can shovel surface feed and follow it underground without machinery. Every harvested block yields exactly itself, with no Fortune bonus; Silk Touch gives the same unit. The existing prospector displays each family's own name/icon and a rough signal. The powered Ore Drill spends its existing energy/tick cost, recovers one block and leaves ordinary sand; full output waits without spending the reserve. Sand is tagged separately from `c:ores`, so generic ore multipliers never produce valuable outputs from it. The older Deposit Drill is not a sand extractor. Current consumer: transport/storage; future consumers: the family-specific separation/refining routes above.

`mineral_sands.enabled=false` disables new acquisition through biome decoration while keeping blocks/items, tags and saved stock registered. No retrogen or periodic refill exists. Already generated and depleted chunks are unchanged. No platform/dependency changes, new network messages or chunk-loading loop are introduced. The existing prospector, energy/inventory interfaces, machine authority and ordinary chunk persistence are reused; no new inspection overlay is needed.

## Owner-library provenance

All four 16×16 runtime block textures are unmodified byte copies from `art/owner-library/originals/Blocks/`, covered by the [owner's provenance declaration](../../art/owner-library/PROVENANCE.md). The originals and their supplied notices remain intact. Reproduction is recorded in `tools/owner_art.py` and `tools/mineral_sands.py`.

| Runtime texture | Source path within that library | SHA-256 |
| --- | --- | --- |
| `titanium_sand.png` | `biomes and tree blocks/black_sand.png` | `b01d1b9261554fa30b2d90d9a2be2030415e4a469aee345b1f6d20e0782d612b` |
| `zircon_sand.png` | `biomes and tree blocks/white_sand.png` | `b4c98d9f418a3b8bc781ca2ea41eb938a9a30596abff9855b298d15026674574` |
| `rare_earth_sand.png` | `Big Cannons and Mounted Guns/textures/block/blastsand.png` | `81d4bb34246ee3f9a78df5a5a7d4ad9728092e40d7c40f314d087e3e3249c3e7` |
| `garnet_sand.png` | `Big Cannons and Mounted Guns/textures/block/blastredsand.png` | `6802ffe8150d9d7802b18b9612a1edbd7a2ff3af2a20aa9ff958ca8393379423` |

## Validation

Validation on 13 October 2026 uses Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, JDK 25 and the unchanged 18-library lock.

- `gradlew.bat build --no-daemon`: passed all **1,391 required server tests**. Six mineral-sand cases check solid collision/heightmap participation, exactly one feed unit with ordinary/Fortune/Silk Touch/bare-hand loot, family identity, excluded biome tags, no generic ore bonus recipe, prospector discovery, all four powered drill outputs, full-output backpressure and overlapping drills sharing one reserve.
- `python tools/check_mod_data.py`: passed **2,201 material IDs**, data and recipe audit. Source/data registration audit was repeated after the solidity correction. `python scripts/check_repository.py` and `python tools/owner_art.py --check` passed.
- `python scripts/package_modrinth.py check`, `verify`, and `python -m unittest discover -s scripts/tests -v`: passed the 18 locked dependencies and all 14 packaging tests.
- `gradlew.bat runClientGameTest -PclientTests=MineralSandClientGameTests --no-daemon`: passed. Rendered normal worlds on seeds `jugcraft-mineral-sands-1` and `jugcraft-mineral-sands-2` contained all four families at production frequencies, with exposed blocks and shallow buried stock. Each world retained the four harvested inventory units, four depleted positions and every other sampled block after reopening. The second seed's four 64-chunk surveys contained 2,298 / 877 / 1,378 / 779 reserve blocks respectively. An earlier bounded second-seed search found no rare-earth deposit; surveying larger eligible Basin/Lake District regions resolved that test-discovery limit without changing production rules.
- The same rendered suite round-tripped serialized World Designer density/biome codecs and generated all four families in painted surface quadrants over a Lush Caves fallback. It rejected wrong-family painted regions. Controlled terrain fixtures accepted flat sediment and rejected deep water, unsupported sediment, an artificial roof, a chest and steep terrain.
- The client suite also passed with `-PjugcraftOptionalIntegrations=false` and a test init script writing `mineral_sands.enabled=false` after Loom's run-directory cleanup. Every biome omitted the four placed features, while all four saved block types and seven inventory units per family survived reopening. The fixture also disables custom biomes, alpine spawn and towns to isolate this check; the normal-world tests above use the normal generation configuration.
- `gradlew.bat assemble runClient -I build/sands-runtime.init.gradle --no-daemon`: assembled the matching JAR and opened a disposable copy of the cleaned export in the ordinary client, without the gametest mod. The player joined `Mineral Sands Field Trip`; the titanium seam, four feed blocks, shovel and prospector were visible. Only Minecraft's first-run welcome needed dismissal. This confirms ordinary-client loading, not a launcher import. The client was left open after the owner stopped Computer Use.

A natural-world test caught compact sand being omitted from solid-ground heightmaps: Minecraft 26.3 uses motion tags independently of collision shape. The four blocks now join `minecraft:blocks_motion_no_leaves` without joining generic sand recipes; the server regression checks the resolved heightmap tags and the height above an actual placed block. A transient existing crow/crop assertion failed in an earlier full-suite run; it passed on the clean complete run above without changing crow code.

### Inspection world

`Mineral Sands Field Trip` uses the normal preset and seed `jugcraft-mineral-sands-1`, at the production frequencies above. The four 64-chunk inspection surveys contained 898 titanium, 750 zircon, 411 rare-earth and 202 garnet blocks before one block per family was extracted for the persistence check. All remaining sampled blocks, the four inventory units and the four depleted positions survived saving and reopening. These are survey counts, not promised deposit sizes or population estimates.

The save starts in creative mode with an iron shovel, prospector and the four recovered feed units. Its `TOUR.txt` supplies teleport commands and the depleted coordinates; spectator mode permits inspecting the buried beds. Small leaf windows around the forested targets improve visibility. Those leaves and the four documented depleted blocks are inspection preparation; generation does not clear leaves, and the other mineral blocks and terrain are preserved.

The downloadable inspection package contains the world, matching Jugcraft JAR, an installable Modrinth pack with the unchanged 18 locked dependencies, screenshots, test logs and SHA-256 checksums. Only unused test datapack/registry names are removed from the exported save, after scanning saved region records for test content and verifying every other NBT value is preserved. Worlds, builds and logs stay outside Git.

Small qualifying regions can contain no successful attempt. Garnet required surveying several highland regions on the inspection seed. This implementation keeps the approved attempt rates and terrain protections; owner inspection should include whether exploration distance feels appropriate. Separation/refining, a dedicated shaft-powered extractor, two independent multiplayer clients and a full launcher import are not claimed by this change.
