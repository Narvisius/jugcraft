# City defense and performance batch

Owner-requested local work, 22 October 2026, by OpenAI Codex (GPT-6).
Not deployed or promoted to the player updater.

## Raids and defenders

Automatic raids use the **town centre**, never a selected player's location. The
existing grace period, raid interval and strength progression remain. An eligible
observer must be within 160 blocks of the centre; without a placed city there is
no automatic raid. Parties gather on loaded dry ground 120–144 blocks out, outside
city protection. Nothing force-loads terrain. Existing saved raids aimed at player
locations withdraw when loaded. The public explicit-objective API remains for
scripted encounters/tests, but uses the same guard-only combat policy.

All raider infantry, gunners, walkers and blimps target city guards only. The shared
server target/damage gates also reject player retaliation, civilians and pets.
Raider grenades and bombs filter collision and splash independently, including
after reload. Player-directed progression raids are deliberately deferred.

Town revision **3** adds six flak crews on raised, supported platforms on the six
open-topped wall towers, plus four armed guard walkers near the square. Existing
residents retain their indices, names, skins and beds. The mechas reuse the
existing Diesel Walker body, core, legs, fist and arm housing; the drill bit is
replaced by the existing flak receiver/barrel parts, with walking animation and
shot recoil. Original asset sources/notices remain those recorded in
[the walker](diesel-walker.md) and [big guns](big-guns.md) feature records.

Guards and guard walkers retain the town's combat immunity. City flak cannot be
damaged, boarded by players, salvaged or connected to player fire-control tables.
It receives service ammunition only while a matching city crew occupies it; it
has no extractable ammunition inventory. The resident manager restores destroyed
NPCs and reuses station-bound guns. Guns retain their station binding in NBT.
Operator removal/void behavior remains distinct from losing combat.

Defense acquisition uses loaded entities, a 250-block radius, line of sight and
one scan per second. Automatic defense shells spare players, pets, civilians and
friendly summons. They do not break terrain. Gun solutions and spotter marks use
250 blocks; heavier shell speeds and flak fuse duration now support that reach.
Tank machine guns and Howitzer Walker aiming also use 250 blocks. Low-arc aiming
solves height at the horizontal target crossing, including rising shots at blimps;
mortars can elevate to 89 degrees to retain close-range shots with the longer-range
charges. Flak proximity and blast falloff measure distance to the target's hull,
so an airburst beside a tall blimp can actually damage it. Other grenade/rocket
damage falloff is unchanged. Gun depression limits still apply to nearby downhill
targets. Handheld player
firearms retain their own specifications. Effective reach still depends on loaded
chunks, ballistics, elevation and obstructions; this is not a chunk-loading system.

## Bank and existing towns

The bank contains a vanilla Ender Chest, open to every visitor under town
protection. Its contents are the player's own vanilla ender inventory; there is
no communal storage or inventory migration.

Revision 2 is frozen before the blueprint change. Follow the normal
[existing-town upgrade procedure](living-town-upgrades.md): after a backup and
installing this build, run `/jugcraft town upgrade preview`, then
`/jugcraft town upgrade apply`. Only changed authored blocks that still match their
previous state are replaced. Edited blocks and existing containers remain intact.
Seasonal flag sites keep their saved indices but are retired; the blueprint now
owns their platform cells, and decorators cannot erase the new gun supports.
The narrowly bounded flak footprints also migrate where towers protrude into the
terrain-blending ring. Surrounding slopes are not flattened. Only unchanged plain
authored flags are removed; named/patterned banners and containers are preserved.
New crews wait for the corresponding chunk's upgrade. Installing the JAR alone
does not rebuild the city or reset the world.

## Automatically installed optimization mods

Official Fabric 26.3 releases and their embedded manifests were checked on
22 October. Exact files, hashes, licenses and sides are in
`distribution/frameworks.lock.json`, used by both development and release tools.

| Mod | Pin | Installed on |
| --- | --- | --- |
| Sodium | 0.9.2+mc26.3, already included | Client |
| Lithium | 0.26.2+mc26.3 | Client and server |
| FerriteCore | 9.0.0 | Client and server |
| Krypton | 0.3.2 | Client and server |
| ImmediatelyFast | 1.17.1+26.3 | Client only |

These are established, heavily downloaded projects, not a claimed subjective
rating leaderboard. Sources: [Lithium](https://modrinth.com/mod/lithium),
[FerriteCore](https://modrinth.com/mod/ferrite-core),
[Krypton](https://modrinth.com/mod/krypton),
[ImmediatelyFast](https://modrinth.com/mod/immediatelyfast).
Server-only installation is supported by the first three, but each also benefits
client/integrated-server operation, so both sides receive them. None is a hard
standalone Jugcraft dependency. No experimental asynchronous world-generation
optimizer is added. No percentage performance improvement is claimed without a
representative hosted workload benchmark.

The existing managed-file updater installs these new pins in the **same profile**
at the next coordinated release. Dedicated-server bundles omit client-only mods.
No live server files or public update manifests were changed during this work.

## Validation

Local validation, 22 October 2026, Minecraft 26.3 / Fabric Loader 0.19.5 / Java 25:

- All 191 Python packaging/support tests pass, including the read-only chunk
  survey and town roster/blueprint checks.
- Dependency-lock check, original-download hash verification and local `.mrpack`
  assembly pass with 26 pinned libraries. This is not a launcher-import claim.
- The full data/art audit passed its model checks; its one remaining mortar-angle
  mismatch was corrected in the generator. Focused artillery, tower-gun, town and
  dependency-matrix rechecks report zero errors. Repository/document links pass.
- All four `CityDefenseGameTests` pass with the optimization mods installed:
  guard-only targeting, immunity, actual flak/mecha firing, bystander protection,
  range and bank access. Initial live tests exposed and led to fixes for elevated
  aiming, hull-distance flak damage and navigation interrupting walker gun aim.
- `TownClientGameTests` passes with optional integrations both installed and absent, in a disposable
  integrated-server world (seed `jugcraft`, town centre `96 86 656`). Reviewed
  screenshots show supported crewed flak and the mining-walker variant in town.
- Full server sweeps exercised 2,260 tests in each configuration. The last broad
  installed-mod run had 2,259 passes and one exterior-terrain fixture mismatch;
  the broad absent-mod run had two failures (tower migration and a test assuming
  the optional backpack slot exists). These were fixed and the affected town and
  personal-inventory suites rerun: **all 15 selected tests and builds pass in both
  configurations**. Full-suite green status is not being inferred from a filtered
  build. The final test-only terrain comparison fixes were verified by these
  focused reruns rather than another entire sweep.
- The town regression compares 173,127 authored blocks, observes zero drift after
  ticking, preserves edited blocks/resident identities, checks retired tower flags
  becoming supported platforms, and round-trips saved migration progress. Natural
  terrain in exterior blueprint-air cells is deliberately preserved. The inventory
  test always covers Jugcraft's core focus slot and additionally covers the back
  slot when the optional backpack is installed.

The survey also read the stopped disposable test world successfully (zero Candy
Cavity candidates, zero unreadable entries); positive candy detection and
corruption handling are covered by synthetic region tests. It has not surveyed
the hosted world's caves.

Hosted two-player acceptance, an upgrade rehearsal on a copy of the owner's town,
clean Modrinth import and representative load measurements remain release gates.
No live server or public update channel has been changed.
