# Instructions for coding agents

Read [CLAUDE.md](CLAUDE.md), [CONTRIBUTING.md](CONTRIBUTING.md), and the relevant feature record before making changes. State the actual AI tool/model used. Direct owner instructions take precedence over repository defaults.

## Approved frameworks and installation

For authored terrain, biome maps or settlement placement, read [World Designer](docs/WORLD_DESIGNER.md) and its [feature record](docs/features/world-designer.md). Extend the shared offline editor/model and serialized generation codecs. Reuse the existing town builder. Do not create a global active-design singleton, silently rewrite generated chunks, or claim native structure pins control exact building layouts.

The owner authorized the framework foundation and automatic-install companion pack on 5 October 2026. Read [docs/FRAMEWORKS.md](docs/FRAMEWORKS.md) before creating animation, texture, spell, creature, UI, or inspection infrastructure. These libraries are deliberately available for contributors to use; proposing the same dependency again is unnecessary. Integration still requires implementation, tests, and review.

- Exact releases, original download URLs, hashes, sides, and licenses live in [distribution/frameworks.lock.json](distribution/frameworks.lock.json). Gradle and the Modrinth pack use that list.
- Minecraft stays at 26.3. Loader 0.19.5 is the minimum required by this foundation. Do not introduce floating versions or independently upgrade the platform.
- Common required libraries are on the common compile/runtime classpath. Optional libraries have compile access and run in development by default. Client-only libraries belong in `src/client/java` or `src/client/kotlin`.
- Keep Jade, JEI, GuiLib, Fusion, lighting, and shader integrations guarded where optional. `-PjugcraftOptionalIntegrations=false` exercises the supported absence path.
- Keep existing ArmsMotion ownership explicit when introducing Player Animation Library or Spell Engine actions. Do not let two systems pose the same action independently.
- A library being installed does not mean every existing Jugcraft feature uses it. Record the actual integration in `docs/features/`.
- Use the launcher-managed pack from [docs/DISTRIBUTION.md](docs/DISTRIBUTION.md). No startup downloader, third-party JARs in Git, or copied dependency assets/code.

Run `python scripts/package_modrinth.py check` and `python -m unittest discover -s scripts/tests -v` for dependency/pack changes, plus applicable compilation, server/client tests, and the checks in [docs/TESTING.md](docs/TESTING.md). Do not claim pack import or gameplay passed merely because packaging succeeded.

Read [CLAUDE.md](CLAUDE.md) and [CONTRIBUTING.md](CONTRIBUTING.md) for the shared contribution, testing and provenance rules. These rules apply regardless of the coding tool; declare actual model use honestly.

Before creating textures, sounds or animations, inspect the [shared owner asset library](art/owner-library/README.md) and its [complete catalog](art/owner-library/catalog/README.md). The owner authorizes suitable files from this collection for direct use, recoloring/adaptation, or reference without another per-asset permission request.

The library serves every content branch: blocks, ores, metals, machines, guns, planes/airships, sounds, animations, weapons, armor, trees, biomes, farming and food. It is not restricted to machinery.

Preserve the source library. Copy selected files into the feature's runtime resources, keep required animation sidecars, record the source path and modifications, and follow [ART_DIRECTION.md](docs/ART_DIRECTION.md) plus [LICENSE_POLICY.md](LICENSE_POLICY.md). Test an actual runtime import as part of the feature that uses it.
