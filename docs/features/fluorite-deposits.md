# Snow-country fluorite deposits

Status: implemented on `codex/fluorite-deposits`, 13 October 2026. Public [PR #343](https://github.com/jimbozoomer-byte/jugcraft/pull/343). The owner accepted the first inspection world, then requested stone around and beneath the surface ore. The revised outcrops passed the rendered generation/persistence suite before starting spodumene. Original implementation commit: `5bc2af7af84824456ed61864c7f3a62597ce3b65`.

## Approved player result

The 13 October stone-outcrop follow-up passed `runClientGameTest -PclientTests=FluoriteClientGameTests`: two normal seeds, Designed snow/desert boundaries, save/reopen, snowy and sloped foundations, and rejection of air-only deposits, water, roofs, glass obstructions and hollow ground. The rock includes at least twice as many above-ground stone blocks as ore. Underground generation settings are unchanged. Repository/link and whitespace checks passed. The older artifact hashes below identify the original preview; the [combined spodumene/fluorite preview](spodumene-deposits.md#inspection-delivery) supersedes it for visual review. AI-assisted follow-up: OpenAI Codex (GPT-6), explicitly authorized by the owner.

Find large, finite fluorite ore pockets below permanently snowy inland biomes. Small surface outcrops occasionally point to a successfully generated pocket below. Mine with an iron pickaxe, locate supplies with the existing prospector and automate extraction with the existing ore drill. Preserve the shared mineral, tag, configuration and world-generation systems.

- Vanilla: snowy plains, snowy taiga, ice spikes, grove, snowy slopes, frozen peaks and jagged peaks.
- Jugcraft: snowy coniferous forest, snowy forest, muskeg, frostlight garden and snowpetal grove.
- Eligibility comes from the surface biome above the deposit, including authored World Designer paint; cave biomes below do not remove eligibility. Seasonal snow, Alpine Spawn, Tundra, Cold Desert, beaches, rivers and oceans do not qualify.
- Underground centers: Y -32 through 48, triangular distribution peaking at Y 8; native ore size 48; one attempt in eight eligible chunks on average; exposed cave faces allowed.
- Outcrops: attempt above one quarter of successful deposits, three to eight exposed ore blocks within eight horizontal blocks of the deposit center. The ore sits in an irregular, mostly-stone outcrop approximately four to six blocks across, rising one or two blocks above local ground with one or two buried stone layers. Gentle slopes are followed; unsupported ground, steep ledges, water, structures and obstructions reject the entire surface placement. Never create an outcrop for a failed underground placement.
- Stone/deepslate forms and one canonical fluorite item. New chunks only, no regeneration or rewriting existing saves. Disabling acquisition must retain registrations and saved items.

## Progression and integration

Primary specialty: regional mining and future industrial reagents. Reachable entry: iron pickaxe and snowy-country exploration; prospector and powered ore drill are optional existing workshop/steel automation. Solo mining and trade are both supported. The approved future HF/PTFE, electronics and electrolyte consumers remain separate chemistry work; this slice does not claim those recipes exist. No new dependency, currency, dimension or machine is required.

The current size/frequency are starting balance values. Ore-feature size is a shape parameter, not a guaranteed block count. Real generated-world measurements, cave intersections and final consumer costs determine tuning.

## Implementation and validation checklist

- [x] Shared mineral registrations, data, tags, feature switch, loot and owner-library textures.
- [x] Bounded placement using Minecraft's seeded random source, with surface-biome-aware deposits and linked outcrops, including Designed worlds.
- [x] Generation, exclusion, harvesting, prospecting/drilling and persistence checks.
- [x] Real-world sampling on multiple seeds and a rendered client check.
- [x] Local build, repository/data checks and applicable server/client tests.
- [x] Saved inspection world and matching mod/pack for the owner, delivered outside Git.

## Compatibility and evidence

No terrain, generator preset, platform pin or existing mineral identity will be changed. The inspection save is disposable development output and will not be committed. Existing generated chunks remain unchanged. Human visual acceptance and two-independent-client testing must be distinguished from automated evidence.

AI-assisted implementation: OpenAI Codex (GPT-6), under the owner's direct instruction in this chat. No claim of Claude use.

## Implementation notes

`tools/materials.py` owns the mineral, feature switch and balance values. The shared generator writes both ore forms, their loot, iron-tier and `c:ores/fluorite` tags, reversible nine-item storage, and the existing crusher's standard two-item ore output. `OreSurvey` includes the new tag; the ore drill requires no special-case mining code. These are finite ordinary blocks with normal Silk Touch and Fortune behavior.

`SurfaceLinkedOreFeature` delegates underground geometry to Minecraft's `OreFeature`. The placed feature supplies rarity, horizontal spread and triangular depth. It deliberately has no underground `minecraft:biome` filter: the wrapper samples the actual surface biome from the world generation region. The Overworld dimension check prevents reused biomes in other dimensions acquiring deposits. Outcrops vary their elliptical footprint, trimmed edges and upper shoulder using the worldgen random source. All foundation, rock and clearance cells are checked before any surface write, with at most eight 6x6 candidates. Water, non-natural supports, block entities, obstructions and native structure pieces reject it; structure lookups use the generation region's cached starts. Ore replaces exposed top cells with an open side and stone directly beneath, so settled snow still leaves a visible mineral face. Only new chunks receive the revision; existing outcrops are retained.

Surface heights use the live `OCEAN_FLOOR` map, which is maintained during decoration, rather than the earlier terrain-only `_WG` map. Grass/podzol support uses 26.3's separate `grass_blocks` tag. The pocket's center determines regional eligibility; the native ore shape can extend across a neighboring chunk or biome edge. Each outcrop cell independently checks the eligible surface biome.

The switch `fluorite.enabled=false` takes effect after restart, stopping new decoration and acquisition recipes while retaining registry identities, loot and saved materials. There is no load-time rescan or retrogeneration. An outcrop is a clue at generation time; later mining or player edits can remove the pocket or marker.

## Art provenance

- `art/owner-library/originals/Blocks/block_fluorite.png` is copied unchanged to `textures/block/fluorite_block.png` through `tools/owner_art.py`. Source SHA-256: `65e7a0b79bf42371b3b319a6028ac2fcc0fcff1d9d57d7f59ab5967fc9ffb8d8`.
- The ores use the existing shared hand-drawn ore overlay maps over referenced vanilla stone/deepslate. The fluorite crystal uses the existing `raw` pixel map. Their new icy-cyan palette matches the supplied storage block. No Mojang texture is copied, and adding the palette at the end preserves all existing ore layouts.
- The owner's `deepslate_fluorite_ore.png` was inspected; its opaque host does not fit the shared host/overlay model. The source remains untouched. Runtime art is validated by the existing material-map and owner-import checks.

## Validation commands

`./gradlew build` runs the server suite, including `FluoriteGameTests` for iron-tier harvesting, Silk Touch, Fortune, storage, biome policy, prospecting and the actual powered ore drill. `./gradlew runClientGameTest -PclientTests=FluoriteClientGameTests` generates two ordinary seeds and a Designed world, checks cave-biome behavior and excluded land, exercises failed underground placement/snow/water/roof/obstruction fixtures, saves/reopens natural ores, renders screenshots and exports a disposable inspection save under the test run directory. With fluorite disabled in the test process's startup configuration, the same client suite instead verifies absent decoration/recipes and persistent registered ores/items.

## Recorded results — 13 October 2026

- Final `gradlew build`: **PASS, all 1,382 server GameTests**. The three new server tests exercise the actual mining drops and powered drill, not just registration.
- `runClientGameTest -PclientTests=FluoriteClientGameTests`: **PASS**. Both normal seeds generated pockets/outcrops and preserved the sampled ore blocks after save/reopen. Rendered screenshots were inspected: the cyan ores render correctly on stone/deepslate, and the second seed has a clear outcrop in the snowfield.
- The same full generation/client suite with `-PjugcraftOptionalIntegrations=false`: **PASS**.
- A separate startup with `fluorite.enabled=false`, `biomes.enabled=false`, and optional integrations absent: **PASS**. No biome included fluorite decoration, compaction was absent, and both saved ore forms plus seven inventory crystals survived reopening. The local Gradle test setup writes configuration to `build/run/clientGameTest/config/jugcraft.properties` after test-directory cleanup.
- `check_mod_data.py`: **PASS**, 2,193 material IDs and the data/recipe/art audit. `check_icon_maps.py`: **PASS** with 27 pre-existing warnings. Repository/documentation checks and 14 packaging unit tests: **PASS**. All 18 locked dependencies passed download/size/hash verification; pack assembly and archive integrity checks passed.
- The cleaned exported save also loaded in ordinary `runClient`, without `jugcraft-test`: a new player joined at the preview spawn and received the world. This is a runtime startup check; launcher UI import and a two-independent-client playtest were not performed.

| Ordinary seed | Generated chunks sampled | Eligible chunk-center columns | Underground ore blocks | Surface ore blocks in guarded interior |
| --- | ---: | ---: | ---: | ---: |
| `jugcraft-fluorite-1` | 256 | 187 | 5,949 | 13 |
| `jugcraft-fluorite-2` | 256 | 131 | 2,562 | 6 |

Connected pockets include cave-cut and boundary fragments; the largest connected groups were 338 and 333 blocks. Native feature size is not a guaranteed yield. Surface counting excludes an 18-block boundary margin so every counted marker's parent pocket can be checked inside the sample. These observations support retaining the approved starting balance, rather than treating two seeds as an exhaustive distribution study.

The Designed-world check found 3,537 underground blocks across 144 snowy chunks, including deep ore where the underground biome is explicitly Lush Caves. An interior 64-chunk painted desert sample contained none. Controlled fixtures verified that failed underground placement produces no marker; thin snow permits a 3-block exposed marker; water, a roof, and glass obstruction permit the underground pocket but produce no marker.

## Inspection delivery

The local delivery contains `Jugcraft-Fluorite-Preview.mrpack`, `Fluorite-Field-Trip.zip`, the matching JAR, instructions, tour coordinates, screenshots, logs and SHA-256 checksums. The preview pack embeds the save under `overrides/saves/Fluorite Field Trip/`; third-party libraries remain official locked download entries. None of these binaries or saves is committed to Git.

The world uses normal terrain, seed `jugcraft-fluorite-2`, creative mode and commands enabled. All fluorite generated naturally. A lit cutaway removes surrounding rock from one pocket while preserving its ore; other pockets remain untouched. Time and weather are fixed for inspection. Development-only datapack selections (`jugcraft-test` and `fabric-gametest-api-v1`) were removed from the exported `level.dat` and its backup, with every other NBT value preserved, before the ordinary-client load check.

- Surface view: `/tp @s -58 83 -469 143 27`
- Lit underground cutaway: `/tp @s -67 28 -482 0 0`
- Untouched larger pocket: `/gamemode spectator`, then `/tp @s -234 20 -457`

Artifact SHA-256:

| Artifact | SHA-256 |
| --- | --- |
| Preview pack | `4e235c27376e93cc72ac4591e9b58370604d2c70ddbea654a11ed9fa09ca5f98` |
| World ZIP | `216242d18068d4f463c15ca9c1eb361e7837281f24cbd28d3fd701fa945d7569` |
| Matching JAR | `106ee23e93da20d1100ee8abc5cb0d506ff59eda2df9c5999ae8c0bd94194c13` |
