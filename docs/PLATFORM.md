# Platform decision and bootstrap gate

## Selected platform

The owner has selected **Minecraft Java Edition 26.3 + Fabric** for Jugcraft. Build original systems from scratch; availability of third-party content mods does not determine this choice. Jugcraft remains one original mod with internal feature packages, using Fabric API where appropriate.

Do not use a floating `latest` version or assume Minecraft's version number is the Java/JDK version. Do not automatically upgrade the project when Minecraft releases a new version.

## Current pins (compile-verified)

The bootstrap scaffold pins these in `gradle.properties`:

| Component | Pin | Source |
| --- | --- | --- |
| Minecraft | 26.3 | Owner decision (PR #1) |
| Java | 25 | Minecraft 26.1+ requirement |
| Gradle wrapper | 9.6.0, sha256 `bbaeb2fef8710818cf0e261201dab964c572f92b942812df0c3620d62a529a01` | Fabric 26.3 announcement; checksum computed from the downloaded distribution |
| Fabric Loom | 1.17 (`net.fabricmc.fabric-loom`, resolved to 1.17.21; no mappings: 26.x is unobfuscated) | Fabric 26.3 announcement |
| Fabric API | 0.161.0+26.3 | Modrinth listing |
| Fabric Loader | 0.19.5 | Required by the actual PAL, spell, lighting, and GuiLib artifacts in the owner-approved framework foundation |
| Kotlin compiler | 2.4.20 | GuiLib authoring support, matching the selected Fabric Language Kotlin runtime |

The 5 October framework foundation raises only the loader patch level from 0.19.3 and adds Kotlin authoring support. The original bootstrap verification below is historical evidence for the earlier pins. Current foundation verification is recorded in [features/framework-foundation.md](features/framework-foundation.md); exact library artifacts are in [FRAMEWORKS.md](FRAMEWORKS.md).

All pins resolved and the mod compiled in the Build workflow on 30 September 2026 (`./gradlew build` → BUILD SUCCESSFUL, Temurin JDK 25.0.4; PR #4). That proved compilation only; see step 4 and 6 below for what has run since.

## Bootstrap status

1. ~~Generate a Minecraft 26.3 Fabric project~~ Done in PR #4 (Loom project, `jugcraft` metadata, separate client source set).
2. ~~Pin and verify the full toolchain~~ Done: see the table above. There is no mapping configuration, because 26.x is unobfuscated.
3. ~~Add `jugcraft` metadata, client code and a minimal item/recipe~~ Done, and expanded by PRs #4–#7.
4. Document tested Windows and Unix build, client launch, dedicated-server launch and game-test commands. **Partly done:** `./gradlew build` and `./gradlew runClientGameTest` (a real client with rendering) are verified in CI on Ubuntu; the client test also starts the game's own dedicated server in-process and joins it (first passed in Build run 36815372169, PR #53). `runClient`/`runServer` themselves and Windows are not yet run.
5. ~~Replace the foundation-only source gate with actual compilation~~ Done: the Build workflow compiles the mod and runs the data checks. GameTests are not written yet. Requiring the Build check on `main` is a GitHub settings step for the owner.
6. Run a dedicated server with two clients, test save/restart, and record exact versions and evidence. **Partly done:** CI joins an in-process dedicated server with one client (machine screen, Retro Trader trades, leave and rejoin) and saves and reopens a world (`JugcraftServerClientGameTests`, Build run 36815372169). Two clients and a restart of the dedicated server itself are manual; the checklist is in [TESTING.md](TESTING.md#dedicated-server-and-two-clients).
7. ~~Select the project license~~ Done: MIT (see [LICENSE_POLICY.md](../LICENSE_POLICY.md)). Publish accurate installation requirements with the first release.

Next, establish shared material, recipe, resource, progression and persistence interfaces before accepting disconnected large systems. Follow the owner-directed [design](DESIGN.md) and [specialties](CONTENT_BRANCHES.md).

Official references used for platform selection:
- https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3
- https://www.fabricmc.net/2026/09/15/263.html
- https://docs.fabricmc.net/develop/getting-started/creating-a-project
