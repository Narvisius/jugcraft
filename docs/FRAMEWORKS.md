# Approved framework foundation

The owner requested these capabilities on **5 October 2026**: Jade support, richer models and textures, better animation, complex spells, more elaborate interfaces, and automatic installation through Modrinth. This is approval to develop against the selected libraries. It is not a claim that Jugcraft's existing content has been migrated to them.

The machine-readable source of truth is [frameworks.lock.json](../distribution/frameworks.lock.json). It records exact Modrinth version IDs (or the immutable upstream GuiLib artifact), hashes, download sizes, provenance, and client/server requirements. Do not paste a version from this document into a separate build file: Gradle already reads the lock.

## Available libraries

| Library | Locked artifact version | Development and distribution role | Use it for |
| --- | --- | --- | --- |
| [Fabric API](https://github.com/FabricMC/fabric) | 0.161.0+26.3 | Required, common; existing platform | Registries, networking, events, rendering hooks, data attachments, transfers |
| [GeckoLib](https://wiki.geckolib.com/docs/geckolib5/) | 5.5.7 | Required, common | Animated creatures, constructs, block entities, items, armor; controllers and transitions |
| [Player Animation Library](https://docs.zigythebird.com/) | 1.2.7+mc.26.3, beta release | Required, common | Player gestures and authored animation; coordinate with ArmsMotion |
| [Modonomicon](https://klikli-dev.github.io/modonomicon/) | 2.16.0 for 26.3 | Required, common | Data-defined books, branching navigation, custom recipe/progression pages |
| [SmartBrainLib](https://github.com/Tslat/SmartBrainLib/wiki) | 2.0.3 | Required, common | Sensors, memories, and complex creature behaviors |
| [Spell Engine](https://github.com/ZsoltMolnarrr/SpellEngine) | 1.10.9+26.3 | Required, common | Spell definitions, casting, targeting, delivery, effects, and presentation |
| [Spell Power Attributes](https://github.com/ZsoltMolnarrr/SpellPower) | 1.6.2+26.3 | Required, common | Shared spell attributes and custom schools |
| [Trinkets Updated](https://github.com/Patbox/trinkets) | 4.2.1+26.3 | Required, common | Equipment slots; the maintained fork required by the selected Spell Engine release |
| [Cloth Config](https://github.com/shedaniel/ClothConfig) | 26.3.159 | Required, common artifact; screens only on clients | Configuration; already required by Spell Engine, so do not add YACL just to duplicate it |
| [Jade](https://github.com/Snownee/Jade) | 26.3.5+fabric | Optional to standalone Jugcraft; included on both sides of the pack | Inspection overlays and server-supplied machine state |
| [JEI](https://github.com/mezz/JustEnoughItems) | 31.8.0.49 | Optional; existing API integration; included in the pack | Recipes, ingredients, uses, catalysts |
| [Fusion](https://github.com/SuperMartijn642/Fusion/wiki) | 1.3.16 for Fabric 26.3 | Optional, client | Connected, scrolling, continuous textures and overlays |
| [LambDynamicLights](https://lambdaurora.dev/projects/lambdynamiclights/docs/v4/) | 4.13.0+26.3 | Optional, client | Moving item/entity illumination; data definitions or API integration |
| [GuiLib](https://skyblockoverhaul.github.io/maven/guilib/index.html) | 0.12.4 for Fabric 26.3 | Optional, client | Composable interfaces, CSS layout, controls and transitions |
| [Fabric Language Kotlin](https://github.com/FabricMC/fabric-language-kotlin) | 1.14.1+kotlin.2.4.20 | Client pack dependency for GuiLib | Kotlin runtime; the Kotlin 2.4.20 compiler plugin is also configured for contributors |
| [Mod Menu](https://github.com/TerraformersMC/ModMenu) | 21.0.0 | Optional, client | Access to library configuration screens |
| [Iris](https://github.com/IrisShaders/Iris) | 1.11.7+mc26.3 | Optional, client | Shader-pack support; no shader pack is supplied |
| [Sodium](https://github.com/CaffeineMC/sodium) | 0.9.2+mc26.3 | Optional, client; required by the selected Iris release | Renderer used by the presentation stack |

All selected client files are installed by Jugcraft Complete. "Optional" means the standalone mod does not require them to load. It does not mean the complete pack silently omits them. Dedicated-server installs omit files marked client-only.

## Use the libraries deliberately

### Animation and assets

Author models and clips in Blockbench and export the appropriate formats. GeckoLib plays authored animation; it does not create the models or poses. Use controllers for independent motions, meaningful preparation/release/recovery phases, and bounded effects. Retain simple existing renderers where they suffice.

The first GeckoLib user is the [guns](features/guns.md): GeckoLib plays the owner's gun animations on each gun and, in the player's own first-person view, on the arms that hold it. The server times every shot and reload, and ArmsMotion does not touch guns.

Jugcraft already has [ArmsMotion](features/arms-motion.md), including first-person and third-person weapon poses. Player Animation Library and Spell Engine do not automatically replace it. A feature using either must specify which system owns each action, transitions, off-hand behavior, handedness, riding, and remote-player playback. Keep hit timing and resource spending server-authoritative.

Fusion requires authored texture/model definitions. High-resolution images and ordinary animated block/item sprites do not inherently require a library. Emissive surfaces, dynamic lighting, and shader bloom are different effects. Use LambDynamicLights only for visual illumination; gameplay lighting rules remain explicit. Test custom rendering with the pinned Sodium/Iris combination and without it.

### Spells and progression

Use Spell Engine's data and APIs for supported casting, targeting, delivery, and impact behaviors. Consult documentation/source for the **locked release**, since upstream default branches can document other Minecraft versions. Extend behavior through supported handlers when needed.

Spell Engine includes its own content, HUD, casting controls, and defaults that may assign abilities to eligible equipment. Review those interactions when integrating Jugcraft items. Installing it does not implement Jugcraft research, rituals, alchemy, spirit agreements, or resource economics. Keep stable Jugcraft IDs and authoritative resource/progression state. Spell Power's school attributes should be reused where appropriate instead of creating duplicate statistics.

The [Arcane Concordance](ARCANE_CONCORDANCE.md) is the reference integration: an instrument resolves a Jugcraft spell tag, a container source offers only learned spells, the casting gate refuses (never approves), a `CUSTOM` impact does the server-side work, and Jugcraft's own resource is paid once in `COST_CONSUME`. Jugcraft weapons opt out of Spell Engine's weapon fallback through `data/jugcraft/spell_assignments/`. Spell Engine runs event listeners without a try/finally, so every Jugcraft listener catches its own exceptions.

Modonomicon presents the codex; it must not become the only owner of unlock state. SmartBrainLib organizes decisions; Jugcraft remains responsible for ownership, inventories, permissions, work limits, and unloaded chunks. Ordinary goals are still suitable for simple mobs.

### Interfaces

GuiLib is a client library. Java and Kotlin source sets are supported by the build; its DSL is designed for Kotlin. Keep screens in the client source set and guard the path that opens a GuiLib screen when the mod is absent. If a future feature truly requires GuiLib, explicitly update the standalone dependency policy and publication plan instead of letting an absent class crash the client.

Kotlin is currently provisioned for those optional client screens. Before introducing common/server Kotlin code, make Fabric Language Kotlin a common required dependency in the lock and test that installed server configuration. The development compiler's standard library is not a substitute for a declared, distributed runtime.

Prototype custom inventory/container integration before replacing existing machine screens. Check keyboard navigation, narration, window/GUI scaling, resource reload, and frame time. A GUI must send a bounded request; the server still validates inventory, permission, distance, costs, and rewards.

### Optional adapters

The [Jade adapter](features/framework-foundation.md) is discovered through Jade's entrypoint, and its tooltip class is resolved only during client registration. It reports energy and processing progress for the shared MachineBlockEntity. Future machine/ritual providers should send a small snapshot, never scan the world or mutate inventories. Discovery-restricted information must stay restricted.

The existing [JEI adapter](features/jei-integration.md) remains optional. Its generated recipe view still does not reflect arbitrary server datapack recipe changes. Installing JEI does not resolve that existing limitation.

The pinned JEI release also needs a client-only compatibility hook: its vanilla registry bootstrap cannot resolve data-pack biomes. `JeiRegistryBootstrapMixin` enters a thread-local scope that excludes custom Overworld and Nether placements only while constructing its vanilla world, loot, and advancement fallback data. Normal world generation stays outside that scope. The scope restores itself even on failure; a game test exercises the full fallback bootstrap and checks that real custom biome registrations and Nether placement survive it. Revalidate these hooks when updating JEI or Fabric's biome API; the scope is never entered by an absent JEI integration.

Libraries available only as optional adapters must not leak into common initialization or saved-state types. Test with `-PjugcraftOptionalIntegrations=false` as well as with the complete development set.

## Dependency ownership and provenance

The selected artifacts require **Fabric Loader 0.19.5** (not the previous 0.19.3). Minecraft remains 26.3, Java remains 25, and Fabric API remains 0.161.0+26.3. The loader increase is part of this owner-authorized foundation.

Upstream nested libraries remain inside their upstream artifacts. JEI includes MezzConfig; Mod Menu includes Placeholder API. Those satisfy the matching project relationships recorded in the lock. Do not extract and recommit their JARs. GuiLib comes from its publisher's Maven repository for development and the byte-identical, commit-pinned GitHub file for pack installation.

The lock records both the catalog license and the artifact's own license declaration. For example, Spell Power's catalog says LGPL-3.0-only while its artifact declares GPL-3.0. Preserve those records and consult upstream terms when changing usage/distribution; Jugcraft's MIT license does not relicense any dependency. Fusion and Sodium have their own terms. The pack references official downloads; no third-party JAR, code, or asset is copied into Git or nested into Jugcraft.

Alternatives discussed but not selected: YACL (Cloth Config already fills the settings role), Continuity (Fusion is the selected block-texture route), and frameworks without a verified Fabric 26.3 release. Generic portability libraries are not needed merely to make new features possible.

## Updating the foundation

1. Select a concrete upstream release for Fabric 26.3 and inspect its actual JAR metadata, including nested dependencies and minimum loader/API versions.
2. Update the one lock entry: version ID, Maven coordinate, original URL, SHA-1/SHA-512, size, sides, relationships, and provenance. Verify the complete dependency closure.
3. Update platform pins only if required and explicitly reviewed; keep `gradle.properties`, source metadata, and the lock consistent.
4. Run the packaging checks, download verification, full build, optional-absent build, and client tests. Test the new `.mrpack` in a clean launcher instance before calling it release-ready.
5. Record actual results in the feature record and update the capability table. Never describe a listed library as an implemented Jugcraft feature.

See [distribution instructions](DISTRIBUTION.md) and [testing requirements](TESTING.md).
