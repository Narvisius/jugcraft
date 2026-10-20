# Diorama default client installation — October 18, 2026

Owner request: include Diorama automatically in Jugcraft's Modrinth packs.
Implemented with OpenAI Codex (GPT-6).

The shared dependency lock pins the author's Modrinth project `kwpTgbCk`, release
`kWas7LpC`, artifact `diorama-0.1.1+26.3.jar`. Its SHA-1, SHA-512 and size were
verified against the downloaded official file. Both the catalog and JAR declare
MIT. The JAR requires Minecraft 26.3, Java 25, Fabric Loader >=0.19.5 and Fabric
API, already covered by Jugcraft's pins. No third-party binary is committed or
nested in Jugcraft.

Every generated Complete pack uses `client: required`, `server: unsupported` for
Diorama. Standalone Jugcraft only suggests it and loads without it. The existing
lock-driven Gradle configuration also includes it in optional client development
runs; no optional class is imported by common gameplay code.

The existing client key-default hook assigns Diorama's new-waypoint action to comma
instead of B, preserving B for the encyclopedia and N for Traveler's Backpack. Controls Reset uses comma. M opens
the map, U lists waypoints; saved custom bindings are left intact. Players with
an earlier Diorama installation can reset its waypoint binding individually.

Previously exported packs and installed profiles remain unchanged until the
player imports or updates to the new pack. No game startup downloader is added.

## Validation

Completed local checks:

- All 19 official dependency downloads verified by size, SHA-1 and SHA-512;
  all nine packaging contract tests passed, covering installation and sides.
- Full Gradle build and all 1,960 required server game tests passed.
- `DioramaClientGameTests`, `EncyclopediaClientGameTests` and
  `GunControlsClientGameTests` passed with the full client stack. Physical M/N/U
  opened the expected map/waypoint screens, B opened the encyclopedia, and R
  reloaded the existing guns. The map screenshots were inspected in the flat
  disposable test world; this is not an exploration performance benchmark.
- Assembly and gun controls passed with optional integrations disabled; the
  Diorama check confirmed absence of its controls and skipped map interaction.
- Repository links and whitespace checks passed. The full Python suite reports
  74 passing tests and one existing alchemy catalogue coverage failure, first
  identifying `jugcraft:bookwyrm_charm`; it remains recorded in the TODO list.
- Generated `jugcraft-complete-diorama-oct18.mrpack` with 19 pinned downloads;
  checked ZIP integrity, exact embedded Jugcraft bytes and Diorama side flags.

Modrinth App import, owner playtesting, independent multiplayer and large-world
map performance remain unverified. The owner's profile was not operated. This
is a local build, not a GitHub push or public release. The newly supplied October
18 animal/mount/pet asset collection is preserved separately and is not gameplay
content in this package.

Sources: [author's Modrinth release](https://modrinth.com/mod/diorama-map/version/kWas7LpC),
[source repository](https://github.com/SunkenInTime/minecraft-orthogonal-map),
[CurseForge listing supplied by owner](https://www.curseforge.com/minecraft/mc-mods/diorama).

October 19 combined-pack correction: the backpack uses N, so the waypoint default
now uses comma. The client regression also compares active bindings, not only
reset defaults. Existing customized options are preserved; an upgraded profile
with the previous N waypoint binding can reset that action in Controls.
