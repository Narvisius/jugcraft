# Architecture and integration rules

Jugcraft is one original mod with internal feature packages. The following boundaries are a design contract for the platform bootstrap, not existing Java packages.

| Area | Responsibility |
| --- | --- |
| core | Stable registration, configuration, save schema, common interfaces |
| progression | Unlock graph, research state, team/player policy |
| materials | Canonical items, tags, processing definitions |
| technology | Machines, transport, work scheduling and power use |
| magic | Rituals, essences, attunement and transformations |
| integration | Bridges between Jugcraft systems and optional external mods |
| agriculture | Crops, husbandry, cultivation conditions and shared harvest rules |
| world | Coherent biomes, caves, structures and versioned world generation |
| creatures | Wildlife, companions, enemies and bounded boss encounters |
| travel | Rocket journeys, realm portals, destination access and safe returns |
| loot | Weighted rewards, abilities, attunement and duplication prevention |
| seasons | Server-configured event windows and persistent seasonal content |
| client | Rendering, screens, sounds and visual feedback |

Features call small shared interfaces rather than reaching into another feature's internals. Core must not depend on optional integration classes. Keep initialization explicit and deterministic. Agree shared API changes in the proposal before multiple contributors implement against them.

## Data and multiplayer

Server owns state and validates every action. Bound message sizes and frequency, processing per tick, inventories, search radius, and loaded chunks. Define who owns machines and who may access them. Handle unloaded chunks, unavailable owners, reconnects, and restarts without duplication or loss. Avoid scanning all players, blocks, or dimensions every tick.

Use stable `jugcraft:` identifiers. Persist schema versions for custom data. Feature switches disable acquisition or behavior safely; they must not unregister saved blocks/items. Schema changes need migration fixtures and an upgrade playtest.

## External integrations

An external mod proposal supplies the official source/project link, exact version and loader compatibility, license/distribution information, dependencies, overlap, upstream maintenance considerations, and actual two-client test plan. Acceptance does not authorize copying or rehosting that mod.

Optional adapters must not load absent APIs. Test with the dependency present and absent. Required dependencies need maintainer approval and versioned installation manifests. Publish Jugcraft's original code as its own JAR; distribute approved external dependencies via an appropriate pack manifest and authorized download sources.

## Platform changes

Minecraft, loader, Java, Gradle, mappings, and dependencies are centrally pinned during bootstrap. Contributors cannot independently upgrade them. The selected platform is Minecraft Java Edition 26.3 + Fabric. Exact toolchain pins are established and tested in bootstrap; porting to another loader is a separate project decision.
