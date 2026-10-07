# Framework foundation, Jade support, and automatic installation

Status: owner-authorized implementation; verification is recorded below. No Modrinth release has been published by this change.

Proposal: on 5 October 2026 the owner asked Codex to add the selected dependencies, make them install with Jugcraft through Modrinth, and document the capabilities so other agents can use them. This direct instruction authorizes this focused foundation and the necessary loader patch. Actual implementation tool: OpenAI Codex (GPT-6), not Claude Opus 5.5.

Owner: @jimbozoomer-byte
Scope: shared development/distribution infrastructure and optional machine inspection; all specialties, no progression tier or new unlock.

## Player-visible result

Jugcraft Complete is an importable `.mrpack`. The launcher downloads every selected pinned library when the player chooses to install it. Jade displays the shared machines' stored energy/capacity and processing percentage from a server snapshot. JEI retains its existing recipe integration. Other installed frameworks are available for development; this does not migrate every model, spell, or screen.

## Implementation and boundaries

- `distribution/frameworks.lock.json` is shared by the Gradle classpaths, generated Fabric dependency metadata, download verifier, and pack builder.
- Minecraft stays 26.3; Fabric Loader moves from 0.19.3 to 0.19.5 because the selected actual library artifacts require it. Java remains 25; Kotlin 2.4.20 authoring support matches the GuiLib runtime requirement.
- The original Jugcraft JAR contains no third-party JARs. The pack carries that original artifact and references authorized, hashed upstream downloads for the libraries.
- Standalone optional integrations stay optional. The complete pack deliberately installs them. Client-only presentation files are excluded from server installation.
- The Jade adapter is reached only through Jade's custom entrypoint. The common plugin registers the server provider; its client callback resolves the separate client tooltip implementation through `jugcraft:jade_client`. No common initializer loads Jade or client rendering types when it is absent.
- The provider reads one machine and sends at most four scalar values. It performs no inventory access, transfer, progression mutation, chunk load, or world scan. It uses the shared energy interface and two read-only processing accessors.
- Loading the selected JEI runtime exposed a recipe-preview disconnect: its vanilla registry bootstrap could not resolve data-pack-only biomes. A client-only `@Pseudo` hook wraps JEI's complete fallback bootstrap in a thread-local scope. During that scope, the Overworld additions and Fabric's extra Nether placements are excluded from the vanilla lookup. Real world generation stays outside the scope. The dedicated regression test covers the world/loot/advancement bootstrap, nested and exceptional cleanup, preserved real biome registrations, and restored custom Nether placement. No third-party code is copied.
- Dependency libraries can have their own content and behavior. Spell Engine's defaults and the existing ArmsMotion renderer must be reviewed when integrating actions. No animation migration is claimed here.

## Progression, saves, and rollback

The Jade overlay changes no acquisition, rewards, resource balances, or saved IDs. The added common frameworks can register their own content; once players obtain that content, removing those frameworks from an existing world requires appropriate backup/restore planning. Removing optional presentation integrations should leave Jugcraft's registrations intact. Do not promise that an old pack version can safely load a world that has acquired content from a newer dependency.

## Verification

- Actual artifact metadata and nested dependency requirements inspected; canonical GuiLib Maven/GitHub artifacts compared byte-for-byte.
- Common and client Java compilation with the pinned framework classpath passed locally on Windows using JDK 25.
- Nine installer contract tests passed: complete file list/sides, reproducibility, missing/exact transitive dependency rejection, download integrity, path/host restrictions, wrong-game-version and nested-JAR rejection, optional Jade publication policy.
- All 18 original library downloads passed exact size, SHA-1 and SHA-512 verification. A real built Jugcraft JAR was packaged successfully.
- Repository/link checks and the material/recipe audit passed.
- The final complete dependency build passed all **821 required server tests** in a fresh generated world, including the Jade snapshot and registry-preview regression tests (Windows, JDK 25). The unchanged baseline passed 819 tests.
- The real client showroom test passed with the complete dependency set: `gradlew.bat build runClientGameTest --continue --no-daemon -PclientTestShard=0 -PclientTestShards=999`. This deliberately selects one existing client test class, not the entire client suite. It produced 62 screenshots; machine and armor renders were visually inspected. Logs confirm Jade's Jugcraft plugin and JEI initialized successfully, with the full rendering stack installed.
- The optional-absent client showroom task also passed and produced 62 screenshots, with the JEI pseudo-mixin skipped and the Jade adapter absent. That combined run failed only its server salvo test. After isolating the salvo fixture, the fresh optional-absent `gradlew.bat build --no-daemon -PjugcraftOptionalIntegrations=false` passed all **821 required server tests**.
- The intermittent salvo failure exposed a shared-world fixture problem: another test's nearby spotter mark could redirect the gun. The salvo test now supplies and clears its own reachable target, preserving the 80-tick deadline and exact three-shell consumption assertion. It adds diagnostics for crew, tick count and aim on failure. No production artillery behavior changed.
- An earlier optional-absent run also failed a crow movement test; that did not recur in the final absence run. Reused test worlds retained completed seasonal-event data and caused two other failures. Fresh generated worlds were used for final checks. No existing assertions were weakened or skipped.
- The final complete-set rerun after the salvo fixture change also passed all **821 required server tests**: `gradlew.bat build --no-daemon`. The public Modrinth import and broader multiplayer checks below remain outstanding.
- Not yet established: clean Modrinth App import, two independent clients on a dedicated server, visual quality of every combined renderer, or migration of existing content to these APIs.

## October 7 integration checks

The updated foundation passed all 881 required server tests against the expanded main branch, and the nine packaging contract tests still pass. Linux client CI now installs the Mesa OpenGL/EGL runtime, asks SDL to use EGL in the virtual display, and writes the OpenGL preference after Loom recreates its disposable test directory. This fixes the GLX-window startup failure without removing any test. The workflows cancel superseded runs for the same PR or branch, keeping runner capacity available for the current commits. Final combined results remain visible in the PR checks; these local results do not imply a public launcher import or a multiplayer playtest.

## Follow-up test plan

Run the full set of client test classes in CI. In a clean Modrinth instance, import the actual pack and check the installed versions against the lock. Inspect a powered processing machine with Jade on client/server, without Jade, and with a client connected to a server without Jade. Exercise the existing weapon animations while the new casting libraries are installed. Check screens at different GUI scales, resource reload, reconnect, and a dedicated-server restart. Record the actual observations rather than treating library availability as gameplay verification. All local tests above used temporary test servers/worlds; no live server was required or deployed.

The contributor capability guide is [FRAMEWORKS.md](../FRAMEWORKS.md); installation and publication are documented in [DISTRIBUTION.md](../DISTRIBUTION.md).
