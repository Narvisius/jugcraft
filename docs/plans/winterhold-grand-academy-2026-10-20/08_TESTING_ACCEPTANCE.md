# Verification matrix and release gate
The ZIP includes no passing test claims. These are **future acceptance tests** to implement/run in Jugcraft.

## Build/data
- `python scripts/check_repository.py`
- Applicable `python tools/check_mod_data.py` plus feature/data schema validators
- `./gradlew build` with pinned Java 25; Java-less environment = unverified, not passed
- `./gradlew runClientGameTest -PclientTests=<actual academy client class>` once named/implemented
- Existing relevant server GameTests and repository-prescribed generator checks

## Structural
- Structure can generate in a supported world, deterministic for same seed/settings; large templates remain inside generation bounding box.
- Cliff anchored and bridge meets navigable land. Towers, atrium, windows and underground rooms have no unsupported floating pieces.
- Every public room accessible via survival-safe walking/stairs; every restricted room has solvable authorization clue; every dungeon has safe exit.
- NPCs do not spawn in walls, fall off cliffs, or duplicate on chunk reload; no mobs in living areas.
- Lighting and snow effects preserve room function; no doors sealed by physical snow accumulation.

## Gameplay
- Instructor offers a real legitimate research/invocation/workstation interaction with correct resource cost.
- Teacher identities, robe skins, dialogues, stations and availability persist across restart.
- Secrets have clues, bounded attempts, once-only claim rules and appropriate feedback; simultaneous activation cannot dupe rewards.
- World records and saved characters persist after repeated unload/reopen without leaking between worlds.

## Climate
- Test academy in at least cold, temperate and hot biomes; always locally snowy, while world and neighbors retain original weather/biome.
- Two clients at boundary see correct independent atmosphere. Fog fade, interior limits and low-particle mode function.
- Record performance before/after from stated scene settings; no unbounded particle or per-tick area scans.

## Evidence
Submit screenshot gallery: aerial approach, bridge gate, courtyard, central hall, dorm room, faculty lab, library, Arch-Mage garden, Midden forge, fog/snow transition and at least one functioning NPC interaction. Provide screenshots only where actually produced. Record test logs, exact commit SHA and manual tests unverified. Keep PR draft until quality gates and maintainer review.
