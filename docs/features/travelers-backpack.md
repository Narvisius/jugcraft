# Traveler's Backpack integration

Status: implemented; validation results recorded below.
Owner approval: direct request, 17 October 2026; Back slot and revised capacities approved before implementation.
AI assistance: OpenAI Codex, GPT-6.

## Player experience and progression

Jugcraft Complete includes Traveler's Backpack Fabric **26.3–11.4.1** and Forge Config API Port **26.3.1**. Equip one backpack in Trinkets Updated's **chest/back** slot, leaving armor free. Press **N** to open it; the pack seeds this binding in `options.txt` and Jugcraft also supplies N as the Controls Reset default because upstream's B conflicts with the Encyclopedia. Diorama's new-waypoint default is comma. Players can remap it in Controls; existing development options are preserved. Spare backpacks can be carried and placed, but portable opening requires the equipped bag. Placed bags remain usable and expose native Fabric storage for logistics.

The starter backpack requires no machinery. Bigger bags consume Jugcraft industrial materials, reaching brass, steel/invar, aluminum/plastics and titanium/lithium production. All are ordinary material recipes: players can produce or trade the ingredients; no new research flag, seasonal gate, boss drop, magic mastery or circular backpack requirement is added. Backpack tiers describe this integration's progression, not new machine tiers.

| Backpack | Storage slots | Tool slots | Upgrade slots | Buckets per tank | Total across both tanks |
| --- | ---: | ---: | ---: | ---: | ---: |
| Leather | 27 | 2 | 2 | 2 | 4 |
| Iron | 45 | 3 | 3 | 5 | 10 |
| Gold | 63 | 4 | 4 | 7 | 14 |
| Diamond | 81 | 5 | 5 | 9 | 18 |
| Netherite | 99 | 6 | 6 | 11 | 22 |

Tanks require the tanks upgrade. The starter backpack includes its sleeping bag but no free tanks or crafting upgrade. Native tool-slot validation accepts eligible nonstackable items, not only Jugcraft tools. Upgrades use the mod's native slot compatibility rules.

## Exact recipes

Each recipe produces one item. All recipes below are shapeless unless specified. Materials without a mod prefix are vanilla; plates, wires, gears, circuits, rubber, pipes, gaskets, plastic, magnets, cells and processors are Jugcraft items. Backpack parts and upgrades are Traveler's Backpack items.

The standard backpack is shaped as follows (`L` = `#c:leathers`, `S` = `#c:strings`, `C` = chest, `B` = any Traveler's Backpack sleeping bag):

```text
L S L
L C L
S B .
```

It consumes four leather, two string, one chest and one sleeping bag, preserving the sleeping bag's color. Every sleeping bag color is crafted from a horizontal row of three matching wool blocks.

| Output | Ingredients |
| --- | --- |
| Blank upgrade | 2 bronze plates + 2 string + iron nugget |
| Backpack tank | 2 tin plates + 2 glass + gasket |
| Hose nozzle | 2 brass plates + glass + lever |
| Hose | 2 rubber + bronze fluid pipe + gasket + hose nozzle |
| Iron tier upgrade | Blank upgrade + 4 brass plates + 2 bronze gears + redstone |
| Gold tier upgrade | Blank upgrade + 4 steel plates + 2 invar plates + advanced circuit + gold ingot |
| Diamond tier upgrade | Blank upgrade + 2 aluminum plates + 2 plastic sheets + 2 gaskets + advanced circuit + diamond |
| Netherite tier upgrade | Blank upgrade + 4 titanium plates + 2 lithium cells + processor + netherite ingot |
| Crafting upgrade | Blank upgrade + crafting table + 2 bronze plates |
| Lantern upgrade | Blank upgrade + lantern + glass + copper wire |
| Jukebox upgrade | Blank upgrade + note block + diamond + copper wire + redstone |
| Pickup upgrade | Blank upgrade + hopper + 2 brass plates + basic circuit |
| Refill upgrade | Blank upgrade + dropper + 2 brass plates + basic circuit + redstone |
| Feeding upgrade | Blank upgrade + bowl + hopper + 2 brass plates + basic circuit |
| Furnace upgrade | Blank upgrade + furnace + 2 steel plates + basic circuit + 2 bricks |
| Smoker upgrade | Blank upgrade + smoker + 2 steel plates + basic circuit + 2 bricks |
| Tanks upgrade | Blank upgrade + 2 backpack tanks + 2 gaskets + plastic sheet |
| Magnet upgrade | Blank upgrade + 2 neodymium magnets + 2 copper wires + 2 invar plates + advanced circuit |

Use the smithing table with **one leather in the template slot, the existing backpack in the base slot, and the next tier's upgrade kit in the addition slot**. Order: Leather → Iron → Gold → Diamond → Netherite. Native smithing preserves components and rejects skipping tiers. Native cosmetic conversion/dye recipes remain available. There is no kit refund or material recovery loop.

The always-enabled `jugcraft:travelers_backpack` built-in pack replaces the named recipes and filters the three alternative starter recipes plus void and blast furnace upgrades. It is registered only when Traveler's Backpack is present. No replacement serializer or custom inventory code is needed.

## Features and limits

- Enabled: crafting, lantern, jukebox, pickup, refill, feeding, furnace, smoker, tanks, magnet, sleeping bag, tool swapping, and native sorting/quick transfer/slot memory. Sleeping bags do not change the player's spawn point.
- Disabled: void and blast furnace upgrades, all variant abilities, backpack chest loot, backpack-wearing mob spawns and backpack villager trading. Config flags also disable creative-sourced void/blast upgrades; recipe removal alone is insufficient.
- No shulker boxes inside backpacks; native backpack nesting restrictions remain. No carrying slowness. This does not impose a global limit on spare bags in inventory.
- Death placement enabled; forced replacement of occupied blocks disabled. Native despawn, fire, explosion, void and invulnerability protections enabled.
- Pickup/refill/feeding/magnet filters: 9 slots in rows of 3. Refill every 5 ticks, feeding every 100 ticks, magnet every 10 ticks within range 3. Native furnace/smoker timing and fuel costs remain. No new tick loop, power conversion, fluid creation or charging is added.

The server owns native inventory/upgrade handling. Placed storage uses Traveler's Backpack's Fabric transfer implementation, permitting compatible Jugcraft logistics without a new transfer adapter. This integration does not add owner/team access controls to placed bags or make claims about all companion task behaviors. Two-player concurrency and full production loads still require playtesting.

## Owner configuration and existing worlds

Complete pack files:

- [`distribution/overrides/defaultconfigs/travelersbackpack-server.toml`](../../distribution/overrides/defaultconfigs/travelersbackpack-server.toml): seeds `config/travelersbackpack-server.toml` when missing. An existing world's `serverconfig/travelersbackpack-server.toml` takes priority over the global file in the locked Forge Config API Port release.
- [`distribution/overrides/config/travelersbackpack-common.toml`](../../distribution/overrides/config/travelersbackpack-common.toml): loot and trading switches.
- [`distribution/overrides/options.txt`](../../distribution/overrides/options.txt): initial backpack key N. When upgrading an existing instance, preserve personal options and merge this binding if desired.

For an **existing instance**, stop the server and back up the world, then merge the approved settings into `config/travelersbackpack-server.toml`, or into the world's `serverconfig/travelersbackpack-server.toml` if that override already exists. The defaultconfigs template does not overwrite an existing active config. The same applies to an owner's subsequent capacity changes. Development/test launch tasks copy the pack templates only when missing.

Capacity editing needs no Java changes. Edit `inventorySlotCount`, `toolSlotCount`, `upgradeSlotCount` and `tankCapacity` under each tier, then restart. In this Fabric release, `tankCapacity` is **Fabric units per row**, with **81,000 units = one bucket**; the upstream config's mB wording is misleading. For up to 81 storage slots, rows = ceil(slots / 9). Above 81, rows = ceil(slots / 11) + 2. Each tank's capacity is rows × tankCapacity. Leather uses 54,000 per row; the other approved tiers use 81,000. Recalculate this if storage rows change.

Slot counts are saved in individual backpacks. Config changes apply when a new backpack initializes or when smithing into a new tier; they do **not** resize every existing backpack automatically. Tank capacity is calculated when its wrapper loads, so reopening/reloading after restart can change it. Do not shrink a populated bag/tank as an unattended migration. Empty affected inventories and tanks first and test on a backup. Automatically resizing all existing bags safely would require a separate migration feature, which this integration does not implement.

Removing the external mod from a world containing its items/blocks is not a supported feature toggle. Keep it installed to preserve saved bags; disabling abilities or upgrade availability preserves the mod's registrations. Restore a world backup to reverse removal damage.

## Dependencies and distribution

Exact Modrinth version IDs, SHA-1/SHA-512 hashes, sizes, license metadata and original download URLs are in [`frameworks.lock.json`](../../distribution/frameworks.lock.json). Sources: [Traveler's Backpack](https://modrinth.com/mod/travelersbackpack/version/SmhjfeWr), [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port/version/JpKvrr9J), [upstream code](https://github.com/Tiviacz1337/Travelers-Backpack).

Both dependencies are installed on client and server by Complete. Both remain optional for standalone Jugcraft: without Traveler's Backpack, this recipe pack is not registered. Standalone users installing it separately must also copy the two supplied config templates to receive the Complete balance settings. The Jugcraft JAR ships only the authored recipe integration, not third-party JARs, assets, decompiled code or a downloader.

## Verification

Validation on Windows, 17 October 2026, from base commit `2804ba015b76e99ff341687801cb01fb908a9670` plus this integration: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, JDK 25.0.4.1, Gradle 9.6.0 and the 20 locked dependencies.

- `gradlew.bat build`: **1,757 server game tests passed**, including all three `BackpackGameTests`. They craft every changed recipe, verify filtered upstream recipes are absent, check all five capacities in Fabric units, reject tier skipping, preserve native components, confirm the Back slot and insert/extract items and fluids through placed storage. A Leather tank accepts exactly two buckets. All 66 supplied TOML values also match the native loaded configuration.
- `gradlew.bat runClientGameTest -PclientTests=BackpackClientGameTests,EncyclopediaClientGameTests`: passed with the complete optional runtime. N opens the native backpack menu; B opens the Encyclopedia without a key conflict; chest armor remains equipped; capacities synchronize to the client.
- A subsequent focused `BackpackClientGameTests` run passed with expanded tool slots at 1280×900. The Leather and Netherite screenshots were visually inspected: 27/99 storage, 2/6 tools and 2/6 upgrades fit. Screenshots are local build evidence, not redistributed dependency assets.
- `python scripts/package_modrinth.py check`, `verify` and `build`: passed; all 20 downloads match pinned sizes and hashes. The generated `.mrpack` includes both TOML templates, the versioned N-key preset and both dependencies on client/server. Clean launcher import remains a separate check.
- `python -m unittest discover -s scripts/tests -v`: **56 passed** with DejaVu 2.37 configured through `JUGCRAFT_FONT_DIR`. The packaging subset was rerun after the key preset change: 10 passed.
- `python scripts/check_repository.py`, `python tools/check_mod_data.py` and `git diff --check`: passed. The data audit covered 2,777 material IDs, 8,403 block/item models, 3,707 quad parts and 171 combined models; it is not a gameplay test.

- `gradlew.bat build runClientGameTest -PjugcraftOptionalIntegrations=false -PclientTests=BackpackClientGameTests --continue`: passed, including **1,757 server tests** and a real client creating/joining a world without the optional mods. The first attempt had one failure in the unchanged `LaunchersGameTests.rocketsBurstOnWhatTheyHit` timing test; the full rerun passed without changing that test or rocket code.

Not yet certified: clean launcher import, two independent clients, save/restart of an existing production world, owner-driven downsizing, native death/void recovery under all conditions and performance at scale. No production server is modified by this change.

### PR check repair

The first GitHub Actions run exposed two integration issues beyond the initial focused tests: material regeneration removed the authored backpack pack, and dedicated-server reconnect tests lost Forge Config API Port's shared config state. The generator now cleans only its two generated packs; a cleanup regression checks that authored backpack recipes survive. The reconnect fixtures reload the original server config files after the in-process client disconnects, as explained in [Testing](../TESTING.md#dedicated-server-and-two-clients). No production mixin, config-sync bypass or skipped assertion is added. The backpack client test now includes three dedicated-server connections.

Repair validation with OpenAI Codex (GPT-6), 17 October 2026: all 57 Python tests passed, as did repository and dependency-pin checks. Full material regeneration retained the authored backpack pack unchanged. The Windows generator also reproduced unrelated platform-dependent differences in one existing wind-chime model's floating-point coordinates and 16 townsfolk PNGs; those generated changes were discarded. Linux CI remains the authoritative full regeneration comparison.

`gradlew.bat runClientGameTest -PclientTests=BackpackClientGameTests,JugcraftServerClientGameTests,PeepoCompanionClientTests --no-daemon` with `CI=true` passed after the repair. This covers the three backpack connections, the companion suite's dedicated reconnect, and machine/trader world save/reopen and reconnect assertions with the locked dependencies present.

The next Linux run passed the build, regeneration, packaging, optional-absent server tests and seven client shards, but shard 3 hit its 60-minute limit. Five thread dumps showed the same saved-world-close deadlock: the render thread waited in `IntegratedServer.halt` / `executeBlocking`, while the server and test threads waited at Fabric's phase barrier. The test-only shutdown adapter described in [Testing](../TESTING.md#dedicated-server-and-two-clients) schedules native shutdown on the server thread and preserves the existing save and completion checks. The persistence regression now verifies three successive reopen/close cycles. No production backpack settings, recipes or runtime code change in this repair.

Shutdown repair validation with OpenAI Codex (GPT-6), 17 October 2026 (America/Chicago): `CI=true gradlew.bat runClientGameTest -PclientTests=JugcraftServerClientGameTests,BackpackClientGameTests --no-daemon` passed in 1m 48s. All three saved-world close markers completed, the fixture state matched after every reopen, and dedicated-server reconnect and backpack checks passed. All 57 Python tests, repository checks and `git diff --check` also passed. The PR check results record the subsequent Linux validation.
