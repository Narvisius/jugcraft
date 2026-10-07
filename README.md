# Jugcraft

A community-built Minecraft Java 26.3 + Fabric experience about factories, magical workshops, cozy homes, and dangerous frontiers.

**Status: early development.** The Minecraft 26.3 + Fabric mod compiles in CI and contains the base materials, electricity and first machines, but it has not been play-tested yet. Feature proposals and documentation contributions are welcome.

## The vision

Build factories and magical workshops, specialize in agriculture or a school of magic, and trade with other players. Settle in cozy biomes, brave dangerous caves and dungeons, raise friendly companions, find rare equipment, launch rockets to moons and planets, and open portals to magical realms. Halloween and Christmas-esque seasonal content should grow from these permanent systems. Branches stand on their own while supporting each other at meaningful milestones.

Jugcraft's original gameplay ships as one mod, organized into internal feature packages. Third-party mods, if approved, remain separately installed dependencies in an accompanying pack. We do not copy their code or bundle their JARs into Jugcraft.

## Frameworks and automatic installation

**Jugcraft Complete** is the companion Modrinth `.mrpack`: import it in Modrinth App and confirm installation to download Jugcraft and the selected pinned libraries together. Successful Build workflow runs provide it in the **jugcraft-installers** artifact. A public Modrinth listing is not configured yet; see [installation and publishing](docs/DISTRIBUTION.md).

**Contributors and AI agents:** use the [approved framework catalog](docs/FRAMEWORKS.md) before building new animation, texture, spell, creature, or UI infrastructure. The foundation includes GeckoLib, Player Animation Library, Spell Engine/Spell Power, Modonomicon, SmartBrainLib, GuiLib, Fusion, and the supporting libraries, plus Jade/JEI and optional visual integrations. Their availability does not mean existing content has already been migrated. Jade's adapter reports shared-machine energy and processing progress. Exact versions and sides come from one [dependency lock](distribution/frameworks.lock.json).

Minecraft remains **26.3 + Fabric**, with **Loader 0.19.5** required by the selected framework artifacts. Read the [feature record](docs/features/framework-foundation.md) for actual verification and outstanding playtests.

## Start here

- [Design a new world's terrain, biomes, spawn, city and villages with World Designer](docs/WORLD_DESIGNER.md).

- [Start contributing now: fork, connect your AI, and open a PR](docs/COMMUNITY_QUICKSTART.md).
- [Propose a feature](../../issues/new?template=feature.yml): no coding experience needed.
- [Propose an existing mod integration](../../issues/new?template=integration.yml).
- [Contribute code, art, or documentation](CONTRIBUTING.md).
- [Browse the shared owner asset library](art/owner-library/README.md): reusable textures, reference sheets, sounds and animations for every content branch, including building, industry, equipment, vehicles, biomes and farming.
- [Read the connected gameplay design](docs/DESIGN.md).
- [See everything that exists so far](docs/WHAT_EXISTS.md) (content, APIs and file map, for contributors and AI agents) and [what changed](CHANGELOG.md).
- [See how the technology tree works](docs/TECH_TREE.md), including the planned [Chemistry branch](docs/branches/CHEMISTRY.md).
- [Grow crops in the Agriculture branch](docs/branches/AGRICULTURE.md): 3-block corn for fields and mazes, sunflowers, beans, sweet potatoes, flax and sickles; a kitchen garden with trellis tomatoes, peppers, onions, garlic, cabbage, oats and barley, and a Cooking Pot for soups and chili; festival crops with squash and gourds, Turnip Lanterns, cranberry bogs and a chestnut tree; a Carving Knife to carve any face into a pumpkin; plus the plan for what comes next.
- [Explore specialties, magic schools, creatures and seasonal briefs](docs/CONTENT_BRANCHES.md).
- [See architecture and integration rules](docs/ARCHITECTURE.md).
- [Check the roadmap](docs/ROADMAP.md), [owner-requested TODO list](docs/TODO.md) and [platform decision](docs/PLATFORM.md).
- [Plan the Jugcraft Encyclopedia](docs/features/jugcraft-encyclopedia.md): inventory/keybind access, technology and magic pathways, integrated quests and ten UI references.
- [Maintainer setup and review guide](docs/MAINTAINERS.md).

## How additions become part of Jugcraft

Idea → fork and feature branch → focused prototype and commits → draft pull request → checks and maintainer review → multiplayer playtest → acceptance and merge → numbered release. You may start a prototype without an approved issue.

One feature per PR. Include the proposal, integration contract, and actual test evidence. An approved idea is permission to develop it, not a promise to merge it. Maintainers decide fit, quality, and release timing.

## AI-assisted contributions

Use **Claude Opus 5.5** for AI-assisted feature implementation and review your output yourself. Follow [CLAUDE.md](CLAUDE.md). Declare model use honestly; documentation, ideas, and hand-made art do not require an AI subscription. Git history cannot prove model provenance. This initial repository foundation was prepared with OpenAI Codex, not Opus 5.5.

## Building the mod

The first content, tin and bronze plus the base materials (zinc, lead, silver, nickel, tungsten, uranium, aluminum, salt, phosphate, lithium, rare earths, sulfur, silicon and oil sand; see [docs/features/base-materials.md](docs/features/base-materials.md)), has **not been played yet**. The first machines and electricity (coal generator, copper cable, battery box, electric furnace, crusher, arc furnace multiblock) are described in [docs/features/machines-and-power.md](docs/features/machines-and-power.md). With JDK 25 installed:

```sh
./gradlew build          # Windows: gradlew.bat build
./gradlew runClient
./gradlew runServer
```

`build` is verified in CI, and CI also runs a real client (`./gradlew runClientGameTest`) that joins an in-process dedicated server; `runClient` and `runServer` are the standard Loom commands and have not been tried by hand yet. The two-client checklist is in [docs/TESTING.md](docs/TESTING.md#dedicated-server-and-two-clients). Data checks that do not need Minecraft:

```sh
python3 -m pip install pillow
python3 tools/check_mod_data.py
```

## Local repository checks

Install Python 3.11 or newer, then run:

```sh
python scripts/check_repository.py
```

This checks the contribution foundation only. It does **not** compile Minecraft code or certify multiplayer compatibility. The pinned Gradle build already exists; use the Build workflow and documented build commands for compilation. Multiplayer playtesting remains separate.

## Rights and conduct

Be constructive and credit contributors. Follow [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) and [SECURITY.md](SECURITY.md). Jugcraft is licensed under the [MIT License](LICENSE); contributions are accepted under the same license. See [LICENSE_POLICY.md](LICENSE_POLICY.md) for third-party material and asset rules.
