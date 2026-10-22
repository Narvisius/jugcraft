# In-game pack browser and quiet map defaults

Requested 21 October 2026; implemented with OpenAI Codex (GPT-6).

Jugcraft Complete installs Resourcify 1.8.7 for Fabric 26.3 on clients. Resource
Packs and Iris Shader Packs have a **+** browser for choosing downloads in-game.
Iris is already installed; no shader or resource pack is preselected. This is
separate from Jugcraft's own managed-mod update prompt.

The exact official release is pinned in `distribution/frameworks.lock.json`,
including SHA-1/SHA-512, size, source and license. Its required Fabric API and
Fabric Language Kotlin are already supplied. The installed filename is normalized
to `resourcify-fabric-26.3-1.8.7.jar` for the updater's safe filename policy; bytes
and official download URL are unchanged. The upstream filename is also recorded.
The artifact declares Minecraft `~26.3-rc`, client environment, and LGPL 3.0.
Resourcify is optional for standalone Jugcraft and excluded from server installs.

The first real browser test exposed a shutdown watchdog crash: Resourcify 1.8.7
creates non-daemon cached workers which stay alive after browsing. An optional
client-only `@Pseudo` mixin marks its newly created workers daemon before they
start, retaining the upstream context classloader and task behavior. This lets
Minecraft exit normally after closing the browser. It has no target when
Resourcify is absent and must be rechecked when upgrading that dependency.

New packs ship `client-overrides/diorama/config.json` with only `minimap: false`.
This is the pinned Diorama 0.1.1+26.3 artifact's actual settings location and field;
its other defaults, fullscreen map and waypoints remain available. Players may
enable the minimap themselves. Development clients seed the same file only when
missing. Existing profiles' settings are not rewritten, including through the
managed-mod updater. A newly promoted release can add Resourcify through that
updater without managing player configs, shaders or resource packs.

## Validation

Official release metadata and artifact hashes verified. Packaging regression
coverage checks client-only installation, config placement and Kotlin dependency
closure. `PackBrowserClientGameTests` checks the real Diorama settings loader,
user re-enabling persistence, and renders the actual resource-pack and shader
browsers in a disposable client. The full 2,233-test server suite and all 183 Python tests passed. The first
browser run exposed the shutdown bug above; after the fix, the client assertions,
both rendered catalogs and clean process shutdown passed. Screenshots were
inspected. The actual local pack passed ZIP, updater inventory/hash, side and
minimap-default checks; the JAR contains all 70 declared mixin classes.
The standalone build with optional integrations disabled also passed all 2,233
server tests and the selected client startup/menu/shutdown tests. No clean
Modrinth launcher import or resource/shader installation is claimed by these
checks. The preview is local and unpublished, held for the next coordinated batch.
No pack download/purchase is triggered by the browser smoke test, and browser
catalog availability depends on the upstream services. No live server or public
update-channel promotion is part of this change.
