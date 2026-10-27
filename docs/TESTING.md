# Testing and release evidence

## Current foundation

Run `python scripts/check_repository.py` with Python 3.11+. The Foundation / repository job checks required files, relative Markdown links, and the phase declaration. It is not a Java compiler, mod test, security audit, or gameplay approval. In the `bootstrap` phase, Java/Gradle sources are allowed. The Build workflow compiles the mod (`./gradlew build`), checks generated JSON is current and runs `tools/check_mod_data.py`, which validates material data and audits recipes offline. None of these is a game test.

Game tests run in the Build workflow too. The `mod` job's `./gradlew build` runs every server game test on every change. The client game tests start a real game and photograph showrooms. Eight jobs distribute that work; the expanded all-gun gallery alone took about 30 minutes in October 16 CI, so:

- **A pull request runs only the client test classes that show what it changed.** The `choose client tests` job runs `tools/select_client_tests.py`, which compares the pull request with its base:
  - Docs, Markdown, `tools/` and `scripts/`, the art sources in `art/`, data, the language file, the optional resource packs, unit tests and `.claude/` pick nothing. The generators' output is committed and judged as the files it writes.
  - A model, blockstate or texture picks the classes that name its ID.
  - A Java class picks the classes that show it, and the classes that show the Jugcraft classes using it. A class only gaining code picks by the names and IDs it gained, such as a registry registering a new feature.
  - A class that a quarter of the classes or more show, such as the registries `JugcraftAgriculture` and `Jugcraft`, picks by the names and IDs on the lines it lost or gained. Changing a registry's corn line selects the corn tests. If changed lines identify no feature, retain the known consumer tests instead of selecting zero.
  - Build files, the workflow, mixins and the test mod's helpers run every class.
  - The job's log says why each file picked what it did.
- **`main` (after each merge) and a manual run of the Build workflow run every class.** Run it on a branch from the Actions tab ("Run workflow") to test a pull request in full.

Sixteen client jobs share the chosen classes out by their rough running time (`./gradlew runClientGameTest -PclientTests=<Class,Class,...>`). Sources are resolved from each registered class's full package, including `local.peepo`; timed companion AI suites and the all-gun gallery have explicit runtime estimates. A job with nothing to run passes at once. The `client` job passes only when the choice and all sixteen jobs pass. Each job may run for 60 minutes. `python -m unittest discover -s scripts/tests -p test_client_test_selection.py -v` verifies source discovery, companion selection, complete duplicate-free sharding and isolation of the long gun suite. No test classes are dropped to fit the time budget.

Locally:
- Companion-only: `./gradlew runClientGameTest -PclientTests=PeepoCompanionClientTests`. See [coverage and results](features/companion-tests.md), including the one-client dedicated-server check and explicit limits.
- Hearth-only (including companion spawn/render prerequisites): `./gradlew runClientGameTest -PclientTests=PeepoHearthClientTests`. This selects only the Hearth Oven regression class; it does not execute the full companion suite. See [Hearth Oven validation](features/companion-hearth.md#validation).
- `./gradlew runClientGameTest` runs every class.
- `python3 tools/select_client_tests.py --base origin/main` shows what a branch would run.
- `-PclientTestShard=<n> -PclientTestShards=<count>` still keeps every count-th class.
- The Concordance workshop load card (Chunk C, C-10): `JAVA_TOOL_OPTIONS=-Dfabric-api.gametest.filter=jugcraft-test:concordance_workshop_load_game_tests_load_card ./gradlew runGameTest --no-daemon`. In PowerShell, set `$env:JAVA_TOOL_OPTIONS='-Dfabric-api.gametest.filter=jugcraft-test:concordance_workshop_load_game_tests_load_card'` first, then run `.\gradlew.bat runGameTest --no-daemon`, then `Remove-Item Env:JAVA_TOOL_OPTIONS`. Fabric's filter makes it the only server game test that runs, and only then does it measure; in every other run it passes at once. Its lines begin `Concordance load card |`. `JAVA_TOOL_OPTIONS` reaches every Java program started while it is set, Gradle's own included. A manual Build run with `load_card` ticked runs it in its own job, once that workflow change is approved.
- The Concordance pact hall load card (Chunk D, D-09): `JAVA_TOOL_OPTIONS=-Dfabric-api.gametest.filter=jugcraft-test:concordance_pact_hall_load_game_tests_load_card ./gradlew runGameTest --no-daemon` (in PowerShell as above, with this id). It measures only when the filter names it, and passes at once otherwise; its lines begin `Pact hall load card |`. Run one card a `runGameTest`: each needs a server of its own. A manual Build run with `load_card` ticked runs both, the pact hall's after the workshop's, in the `load-card` job, once that workflow change is approved.
- Concordance Chunk D's pact client tests without the optional integrations (D-09): `./gradlew runClientGameTest --no-daemon -PjugcraftOptionalIntegrations=false -PclientTests=ConcordancePactClientGameTests,ConcordanceRosterClientGameTests,ConcordancePactNetworkTests`. The two-client test's second client keeps its log (`peer.log`) and its pictures under `build/run/clientGameTest/owner-network-evidence/`. A manual Build run with `clients_without_optional` ticked runs them in a job of their own, once that workflow change is approved.

The client tests a pull request skips still run on `main` after it merges. A break they catch there is fixed in a follow-up pull request.

## Gameplay PR evidence after bootstrap

For framework/pack changes, also run `python scripts/package_modrinth.py check`, `python -m unittest discover -s scripts/tests -v`, `python scripts/package_modrinth.py verify`, and `python scripts/package_modrinth.py build` after compilation. Test `./gradlew build -PjugcraftOptionalIntegrations=false` and the corresponding client run. The [distribution guide](DISTRIBUTION.md) explains clean launcher import and client/server file flags. A generated `.mrpack` is not proof that importing or playing it passed.

Record commit SHA, exact client/server/dependency versions, test world origin, commands, observed results, and test date. Do not commit worlds or logs with player information; attach redacted evidence when needed.

- Build and relevant unit/game tests.
- Dedicated server startup and two independent clients joining.
- Actual survival acquisition path, recipes, progression locks, and automation.
- Concurrent use by two players, permissions, disconnect/reconnect, save and restart.
- Existing systems interacting with the new feature; dependency present/absent for optional integrations.
- Invalid requests, repeated actions, full inventories, interrupted crafts, chunk unloads, and duplicate reward attempts where relevant.
- Before/after performance under a stated workload: machine count, players, hardware, median/p95 tick time and memory observations. Agree per-feature limits before merge; do not claim support for an untested player count. The Concordance workshop's before/after measurement is its load card (above; [Chunk C's contract](features/concordance-chunk-c.md), C-P19 for the proposed limits). The pact hall's is its own card (above; [Chunk D's contract](features/concordance-chunk-d.md), D-P129 for the proposed limits).
- Upgrade from the prior released world version and safe feature-disable behavior where persisted content changes.

## Dedicated server and two clients

**Automated (one client):** the client job's `JugcraftServerClientGameTests` starts the game's own dedicated server in-process (Fabric's client gametest API), joins it, opens a machine's screen and a Retro Trader's trades through the network, leaves and joins again, and checks the inventory and world. It also saves, closes and reopens a singleplayer world holding a trader, a machine and a cabinet. It is not `./gradlew runServer`, and it has one client.

The pinned Minecraft 26.3 / Fabric client gametest combination can deadlock during integrated-server shutdown: `IntegratedServer.halt(false)` waits for a server task while the server is parked at Fabric's phase barrier. The test mod's client-only `SingleplayerShutdownMixin` queues the complete native halt on the server thread, allowing Minecraft's existing shutdown wait loop to advance Fabric's phases. Native player removal, world saving and the wait for completed shutdown remain in place. No timeout or assertion is relaxed. The persistence test reopens and closes the same saved world three times and compares its machine, trader and cabinet state each time. The adapter and its mixin configuration live exclusively in `src/gametest` and are absent from the released mod.

Forge Config API Port's server config registry is shared by both sides in that test JVM. Its remote-client config sync replaces the file-backed config with an in-memory copy, and disconnect unloads it, also affecting the still-running test server. Reconnect tests call the test-only `InProcessServerConfigs.beforeReconnect` helper after disconnect to reload the same global/world config files before the next handshake. Config packets, backpack synchronization and reconnect assertions still run. This helper is absent from the released JAR; it does not certify separate-process multiplayer or config isolation. `BackpackClientGameTests` exercises three consecutive connections with the equipped backpack and approved capacities preserved.

**Automated storage-crate checks (two independent clients):** `./gradlew runClientGameTest -PclientTests=StorageCrateMultiplayerClientGameTests -I tools/storage-crate-multiplayer.init.gradle` explicitly opts into two rendered client JVMs and a third, normally ticking dedicated-server JVM. The test uses distinct synthetic identities, an offline server bound only to loopback, and new disposable directories. It checks competing extraction, independent searches, upgrades, claim changes, disconnect/reconnect, process restart and four measured load stages. Results, limits and hardware are recorded in [the storage-crate feature record](features/storage-crates.md). It is skipped without the opt-in init file; normal client CI does not launch this resource-intensive local benchmark. It does not exercise account authentication or a remote network.

**Automated (two client JVMs, one machine):** `OwnerCollectionsNetworkTests` (#435) and, since Concordance Chunk D (D-09), `ConcordancePactNetworkTests` start a second, separate client JVM with its own temporary profile and an offline name against the first client's loopback-only, disposable dedicated server, coordinated through files under `build/run/clientGameTest/owner-network-evidence/`; the client shards upload each second client's `peer.log`. Chunk D's puts a stranger at a holder's pact perch: it sees Stolas arrive, assent and depart, and is refused Accept (`not_permitted`), End (`not_holder`) and a roster End naming the holder's pact (`not_listed`), while the holder's pact seals once and ends once. These are two automated clients on one machine with offline names and no people: not the manual two-account check below, and not a measure of a public server. Locally: `./gradlew runClientGameTest -PclientTests=ConcordancePactNetworkTests -I scripts/owner-network.init.gradle` (it caps the first client at 3 GiB; the second takes 2 GiB).

**Manual (two clients):** needs two people or two accounts. Record the commit, exact versions and pass/fail for each line, with redacted screenshots or logs.

Setup:
1. `./gradlew build`; the mod is the jar in `build/libs/` without `-sources`.
2. A dedicated server for Minecraft 26.3 with Fabric Loader 0.19.5 (the official installer at https://fabricmc.net/use/server/), with the common files from the exact Jugcraft Complete pack. Use a server installer that honors the `.mrpack` environment flags; see [DISTRIBUTION.md](DISTRIBUTION.md). Accept the EULA and start it.
3. Two clients using that same Jugcraft Complete pack, signed in as different accounts. Both join. Also exercise optional integrations absent as described above.

Checks:
- Both join without a registry or mod-mismatch disconnect, and see each other.
- Machines and power: both see the same machine run; one fills it and the other sees its screen update; both take output; nothing duplicates.
- Ores: a plain pickaxe gives raw ore, Fortune more, Silk Touch the ore block.
- Retro Trader: with a cabinet near an unemployed villager, both trade with the same trader in turn (vanilla lets one trade at a time); buy and use a Pixel Hollows map; the buyback and restock behave.
- Pixel Hollows: `/locate biome jugcraft:pixel_hollows`, both travel there, mine clusters with and without Silk Touch.
- Disconnect and reconnect in the middle of a trade and with a machine screen open.
- Stop and restart the server; both rejoin: inventories, machine contents, trader offers and marked maps are kept.
- Set `pixel_hollows.enabled=false` and `retro_trader.enabled=false` in `config/jugcraft.properties`, restart: existing blocks, caves and traders remain.

## Content-specific scenarios

- **Jugcraft Encyclopedia:** `EncyclopediaClientGameTests` presses the Encyclopedia key and opens both routes. It follows a technology milestone into the handbook and back, opens the codex from a magic milestone, and opens and closes from the survival inventory's button (screenshots `jugcraft_encyclopedia_*`). Not covered: GUI scales, the creative inventory, the narrator, two clients and a dedicated server's codex sync. See the [feature record](features/jugcraft-encyclopedia.md#first-slice-entry-points-and-route-home).
- **World Designer:** run `node --test tools/world-designer/model.test.cjs`, the three `WorldDesignerGameTests`, and `WorldDesignerClientGameTests`. Also load an actual exported ZIP into a new world's datapacks, select `jugcraft:designed`, generate the pinned chunks, and inspect structure starts. The [feature record](features/world-designer.md) distinguishes compiler, browser, real-world and persistence evidence.
- **Stolas's pact (Concordance Chunk D):** `ConcordancePactClientGameTests` (the pact screen at three sizes, by keyboard; Stolas's poses and a timed refusal from his front; Jade), `ConcordanceRosterClientGameTests` (the roster; a workshop hand at work), `ConcordancePactNetworkTests` (two client JVMs at one perch), `ConcordanceJourneyGameTests.thePactHallFromStyxToDeparture` (the whole path on a server) and the server suites named in [the record](features/stolas-pact.md#verification). Not covered: people, sound (CI has none), a player's hardware.

Use the cases relevant to the feature; do not claim a scenario was run just because it appears here.

- Progression: start each affected specialty without advanced goods from itself; test required/optional connections, trade and documented solo routes.
- Agriculture: growth and yield loops, climate conditions, harvest automation, livestock population and outputs feeding both industry and magic.
- Biomes/dungeons: fresh-seed distribution, sensible transitions, old-chunk behavior, hazard signals, spawn caps and encounter containment.
- Pets/bosses: ownership, concurrent interaction, friendly fire, unload/reload, despawn/recovery, summon permissions and loot eligibility.
- Loot/spells: reward weights, duplicate handling, stacking/cooldowns, ability persistence, permission checks and conversion/healing feedback loops.
- Rockets/portals: outbound and return travel, invalid destinations, disconnect during transfer, destination access, stranded-player recovery and dimension/chunk budgets.
- Seasons: server timezone/manual override, activation/deactivation and restart at the boundary, reward replay prevention, and preservation of earned content and occupied destinations.
  - Alpine Spawn ([features/alpine-spawn.md](features/alpine-spawn.md)):
    - `AlpineGameTests` covers the climate table (no meadow left, the cool plateau's forest and taiga taken, lowland forest and taiga kept), tags and seasons, and the alpine village and its grid.
    - It also covers the larch: needles following each season mode (placed and natural), out-of-date needles catching up together, the gradual turn, a sapling grown in winter coming out bare, and the wood set.
    - `AlpineClientGameTests` creates a real world (seed `jugcraft`) and checks that it starts in Alpine Spawn at an alpine village. It logs the biome's share around the start and across a 16 km square, and takes screenshots, including larches grown in spring, autumn and winter.
    - Not covered: other seeds, a dedicated server's first start, needles changing over real days.
  - Biomes branch ([features/biome-regions.md](features/biome-regions.md), [features/seasonal-forests.md](features/seasonal-forests.md), [features/fields-and-meadows.md](features/fields-and-meadows.md), [features/wetlands.md](features/wetlands.md), [features/warm-and-dry.md](features/warm-and-dry.md), [features/big-trees-and-rainforests.md](features/big-trees-and-rainforests.md), [features/mountains-coasts-and-volcanoes.md](features/mountains-coasts-and-volcanoes.md), [features/wonders-and-caves.md](features/wonders-and-caves.md), [features/nether-biomes.md](features/nether-biomes.md), [features/end-biomes.md](features/end-biomes.md)):
    - `BiomeGameTests` covers the recorded layouts (every rule places its biome in each of its layouts) and their unreachable listings, the regions' share, layouts and seed behaviour, maple, aspen, fir, willow, jacaranda, palm, cypress, redwood, eucalyptus, mahogany, cedar and dead trees growing, giant redwoods and mahoganies from four saplings, the tree roster's batch-1 shapes (each placed from four seeds: heights, leaves, autumn looks, the mossy maple's moss and vines, nothing on the wrong soil) and fallen larches, the cedar wood, the wood recipes that load with any of their switches, the batch-1 biomes' tree lists in their vegetation step ([features/trees-batch-1.md](features/trees-batch-1.md)), every seasonal tree's leaves in every season mode, the new woods, and the wild plants (flowers, tall lavender, clover; watergrass, duckweed and cattails; hibiscus and hydrangea; sea oats on sand; glowcaps, glimmerblooms, frost irises and snowpetals), and the Nether and End biomes in their dimensions' biome sources, found near the origin.
    - `BiomeClientGameTests` finds each Jugcraft biome, and vanilla taiga, forest and birch forest, from the start of a real world (seed `jugcraft`), logs the distances (and, for reference, how near vanilla's hot, mangrove, jungle and old-growth climates are), checks that the seasonal leaves generated around each biome are in today's look, and takes screenshots where each biome is on the surface and fills the camera's view.
    - `WoodClientGameTests` grows every tree with its wood on a sample wall (vanilla's spruce beside the cedar's), the tree roster's batch-1 shapes beside their parents and vanilla's spruce and oak (the cedar beside the spruce), and the seasonal trees and shapes in autumn, and logs each tree's logs and leaves ([features/wood-repaint.md](features/wood-repaint.md), [features/trees-batch-1.md](features/trees-batch-1.md)).
    - Not covered: other seeds, a dedicated server, region borders in play, seasons over real days.
  - Seasons ([features/seasons.md](features/seasons.md)): `SeasonGameTests` covers the calendar, zones, overrides, palette, biome tags, events (US and Canadian Thanksgiving, December across New Year), the `/jugcraft season` command, and winter snow (lies, never on farmland, water or vanilla snow, melts in spring). `SeasonClientGameTests` switches each mode in a real client, checks the synced day and tints, then winter snow (falling on the client, snowy grass), and saves `jugcraft_season_<mode>` screenshots. Not covered: a dedicated server with two clients, a restart across midnight, a real winter and thaw.

## Release process

Maintainers test the combined candidate, not just individual PRs, on an isolated staging server. Publish a numbered release with checksums, exact requirements, changelog, known issues, and migration notes. Promote only explicitly approved artifacts. Never automatically deploy arbitrary main or PR builds to the viewer server.

Back up world, player data, configs, and exact previous artifacts before upgrading. Test restoration. If migration is not reversible, restoring the matching backup and old artifacts is the rollback; merely reverting a source commit or replacing a JAR may not restore the world.
