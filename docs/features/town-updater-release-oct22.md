# Town maintenance and updater release — October 22

Owner-authorized scope: publish the updater fix and all prepared town fixes to the
server and client update channel. AI assistance: OpenAI Codex (GPT-6).

This isolated branch starts from main `94035ab761ef974fb7dfe2d9c3c99eef5d3537af`,
which already contains the Windows updater handoff fix (PR #456).

## Player-visible changes

- Public doors, trapdoors, gates, controls, seating and inventory-free workstations
  work inside Spawn City. Shared inventories and stored resources remain private;
  the bank Ender Chest still opens each visitor's personal storage. Held blocks,
  buckets and tools cannot turn ordinary interaction into placement/modification.
- Sleeping townsfolk stop navigation, movement and work goals until they wake.
- City flak filters for direct firing solutions and cannot use the player gun's
  high-angle fallback to lob shells upward at unreachable nearby ground targets.
- Guard walkers animate from movement across ticks, including client interpolation,
  ignore teleport displacement and render a seated uniformed pilot in the cockpit.

All changes apply to existing town blocks/entities. There are no registry changes,
world resets, new dependencies or town regeneration in this release. The unfinished
graves, aircraft, furniture and Nether repair batch remains in its separate local
checkout and is excluded.

## Validation and acceptance

The isolated release compiled the client and passed all **17 required server
tests** across TownGameTests, TownLifeGameTests and CityDefenseGameTests on
October 22 (`town-release-tests.log`). The public-use fixture uses existing
household furniture, not the excluded new furniture registry. The earlier broad
local batch also passed its rendered town review. Repository/documentation checks
passed. Hosted deployment evidence is recorded separately after the release gates.

The exact merged updater helper passed 79 Windows/Java 25 assertions, including
delayed OS-lock release, a full 30-second conflicting-lock timeout, startup
exclusion and rollback/recovery. It also passed the complete main CI suite.

Windows launcher screenshot/input capture repeatedly failed on October 22 even
after the owner brought Modrinth forward. The owner explicitly requested rollout
without that click-through and will report issues. Actual Modrinth apply/quit/
relaunch and hosted human acceptance remain unperformed; do not call them passed.

Deployment order: wait for exact-source checks; produce the numbered release;
stop and back up the server; replace only changed managed files; start and verify;
then promote the matching immutable release manifest. Preserve world/config/player
data and retain the previous JAR plus backup for rollback.
