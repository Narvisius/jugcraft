# Regional vanadium-bearing iron deposits

Status: implemented and visually accepted by the owner on 14 October 2026 after delivery of the inspection world. Public branch `speedygroyper:codex/vanadium-iron-deposits`, [PR #378](https://github.com/jimbozoomer-byte/jugcraft/pull/378). The owner approved this scope and requested tests and an inspection world. Actual implementation tool: OpenAI Codex (GPT-6).

## Approved scope

- Primary surface biomes: Shield and Volcano; native ore size 48, one placement attempt per twelve eligible chunks on average.
- Secondary surface biomes: Highland, Basin, Windswept Hills, Windswept Gravelly Hills, Windswept Forest and Stony Peaks; size 40, one attempt per twenty-four eligible chunks.
- Overworld centers Y -48..32, triangular peak Y -8. Irregular native ore pockets, cave exposure allowed. Size is geometry, not a guaranteed block count.
- One third of successful pockets attempt the existing stone-supported outcrop: 3-8 exposed ore blocks, approximately 4-6 blocks across with buried foundations. Volcano has underground deposits only. Existing fluorite/spodumene placement remains unchanged.
- Surface biome selection also applies below cave biomes and in Designed worlds. No retrogeneration or replenishing reserves.
- Stone/deepslate ores, raw feed and reversible nine-item raw storage. Iron-tier mining, fixed one-feed-unit drops even with Fortune; Silk Touch and the Ore Drill recover the whole ore block. Prospecting uses the shared survey.
- One raw feed or intact ore block -> one existing `jugcraft:vanadium_iron_concentrate` in the Crusher: 200 ticks at its base 16 JE/t. No ore multipliers or free iron coproduct.
- Retain the existing concentrate -> oxide Ore Washer route: 500 mB water, 200 ticks at base 16 JE/t. Oxide and two bricks supply the reusable contact bed without sulfuric acid.
- Fresh servers use the mined supply. `vanadium.legacy_iron_recipes=true` explicitly restores the two ordinary-iron shortcuts; disabling vanadium acquisition also restores them. Existing saved concentrate, oxides and beds retain their identities. The bulk lye route gains a named-concentrate input, preserving its pentoxide output and other costs.
- Later iron recovery and battery-grade electrolyte refining remain separate work.

## Connections and compatibility

This is an iron-tool mining and workshop processing route into the steel-era acid catalyst. The existing Crusher, Ore Washer, electrical supply and ceramics are independently reachable. Trade and the Ore Drill are optional. No new processing machine, dependency, platform change or automatic world rewrite is needed.

`vanadium.enabled=false` stops new ore decoration and new mined-feed processing after restart while retaining registered ore, raw stock and saved content. Existing concentrate extraction remains usable. The legacy recipe option is a server startup setting, not a per-player unlock.

## Automated validation

- Full Gradle build: all **1,647 required server game tests passed**, including mining tier, fixed ordinary/Fortune drops, Silk Touch, reversible storage, exact biome tags, survey and finite drill extraction. Actual powered Crushers handled all three feeds at one concentrate each, paid 3,200 JE, retained feed while full/unpowered, resumed once after clearing output, and did not multiply yield with speed upgrades. The Ore Washer paid 500 mB water and 3,200 JE for one oxide; the oxide crafted the existing bed with two bricks. The bulk concentrate/lye route also passed its machine test.
- `VanadiumIronClientGameTests`: passed with the complete integration set and again with `-PjugcraftOptionalIntegrations=false`. Two normal seeds, both Designed biome groups over Lush Caves, save/reopen persistence, and controlled outcrop fixtures were exercised. Volcano rejected even a forced outcrop attempt on otherwise suitable artificial ground while retaining its underground pocket. Air, water, roofs, obstructions and hollow foundations produced no outcrop; snow and gentle slopes retained stone-supported, exposed ore.
- Existing `FluoriteClientGameTests` and `SpodumeneClientGameTests` completed their generation, outcrop and persistence checks. The first vanadium inspection search then stopped because a seven-chunk Shield patch naturally contained no pocket. The harness now searches up to eight distinct regions; production rarity and placement rules were not changed. The subsequent vanadium runs passed.
- Disabled acquisition, with optional integrations and custom biome placement off: no vanadium decoration, compaction or mined-feed Crusher recipes; existing concentrate washing and both ordinary-iron fallback recipes remained available. Both saved ore forms and seven raw items survived reopening.
- Explicit `vanadium.legacy_iron_recipes=true`: passed with optional integrations absent; both ordinary-iron shortcuts loaded while mined-feed processing and existing concentrate washing remained available.
- Data audit: **2,652 material IDs**, data and recipes passed. Icon-map lint passed with 27 existing warnings; repository/link check passed. All **34 Python script tests** passed. The pack lock and all **18 dependency hashes** passed verification.

Representative final client samples (bounded surveyed areas, not guaranteed deposit counts):

| Terrain / seed | Surveyed chunks | Eligible center columns | Underground ore blocks | Surface ore blocks |
| --- | ---: | ---: | ---: | ---: |
| Designed Windswept Hills over Lush Caves | 144 | 144 | 944 | 0 |
| Designed Shield over Lush Caves | 144 | 144 | 3,635 | 12 |
| Designed Volcano over Lush Caves | 64 | 64 | 1,297 | 0 |
| Designed Desert | 64 | 0 | 0 | 0 |
| Normal `jugcraft-vanadium_iron-1`, selected Windswept region | 256 | 63 | 421 | 5 |
| Normal `jugcraft-vanadium_iron-2` | 256 | 10 | 472 | 0 |

The first normal world's selected region had two connected 186-block pockets plus smaller fragments; the second had 109-, 168- and 195-block pockets. Every sampled underground and surface ore block survived reopening. Generation can cross a sampled chunk boundary; these are measured blocks in the sampled area, not a promise about every deposit.

## Inspection delivery

The local `vanadium-preview-2026-10-14` delivery contains the matching Modrinth pack, standalone world ZIP, JAR, tour, validation notes, screenshots/logs and SHA-256 checksums. Builds and worlds stay out of Git. The artifact README records the source commit.

All ore in **Vanadium Field Trip** generated naturally on normal seed `jugcraft-vanadium_iron-1`, at the production rates. The lit cutaway removes surrounding rock from one pocket while retaining its ore. Creative mode, commands, a fixed clear noon, an iron pickaxe and a prospector support inspection.

- Surface outcrop view: `/tp @s 3988 102 -37`.
- Lit underground cutaway: `/tp @s 3982 -4 -60`.
- Untouched 186-block pocket: `/gamemode spectator`, then `/tp @s 3901 -36 32`.

Only development datapack selections and five unused test-only entries in Fabric's content-presence snapshot were removed from the exported save. A scan of **8,223 saved region records** and all other save files found no actual test content; the cleanup preserved every other NBT value and world file. The cleaned export then loaded successfully through ordinary `runClient`, with the complete integration set and without the test mod; the player joined at the surface tour start. The client remains open for inspection.

The owner accepted the delivered preview on 14 October 2026 with "Looks good". This acceptance covers the preview built from gameplay commit `7eab931e80ee1c874042a6e7dcbae7e9132f113c`; the acceptance update changes documentation only. Launcher UI import, a two-independent-client dedicated-server session and representative server load remain untested. No merge or server deployment is included.

## Art provenance

The owner-library catalog was searched for vanadium and magnetite; no named matching asset was found. Use the established mineral overlay and raw-item maps with a dark iron-grey mineral palette; preserve existing palettes and the original library. Runtime host stone/deepslate references use the established layered models.
