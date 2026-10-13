# Ownership and home/work commands

Hand-feed an untamed Peepo or Jughead once to become its owner. The first meal is accepted even at full health/energy; later meals keep the existing health/95%-energy rules. Ground food never assigns ownership. Feeding an already tamed companion does not transfer ownership. Existing companions without saved owner data are untamed until fed. Taming sets home and work to the taming location, defaults to Home, and uses an 8-block radius.

Shift-right-click opens a vanilla-style gray inventory panel with health, energy, the player inventory and command buttons. This works with any held item; shift-click does not feed. Untamed companions instead show a feeding hint. Normal empty-hand petting/blushing remains.

- Follow me: follows the issuing authorized player in the same dimension. Stops if that player goes offline, changes dimensions or loses party permission. No teleport or chunk loading.
- Stay: releases furniture/work, then holds the resulting ground position. Can eat hand-fed food or loose food already within reach, without seeking food elsewhere.
- Go home: returns toward the saved home, then remains within its radius; can rest on local beds/seats. Does not generate power.
- Work: uses wheels inside the saved work radius, returns to the home area when exhausted, and uses rest furniture inside the home radius. Existing fullness/exhaustion/food rules remain. This does not add new workstation jobs.
- Set home / Set work: records the companion's current position in its current dimension. To move a center, lead the companion there with Follow, then set it.
- Radius - / +: shared home/work radius, 4 to 16 blocks in steps of 4.
- Party commands: owner-only toggle, off by default. When enabled, current members of the owner's Jugcraft party may issue commands; ownership stays with the original feeder. The Follow command follows the authorized player who issued it.

Ownership, mode, follow target, home/work/stay positions with their dimensions, radius and party access persist across saves. Commands are checked server-side against current owner/party permission, entity life, same dimension and an eight-block interaction distance. Non-owners cannot change costumes without permission. Other players may still help feed a hungry companion.

Home/work destinations in another dimension or unloaded chunks are not followed or force-loaded; the companion waits for a usable destination/new command. Normal pathfinding must be able to reach the destination. Following and return paths are refreshed at most every 40–49 ticks, not every tick. Existing staggered station discovery and bounded surface searches are retained.

Compiled with assemble only. No automated tests, in-game checks or multiplayer benchmarks were run. Suggested manual checks: first feed at full energy; second player cannot steal control; party permission toggle; each command; reset home/work after leading to another spot; reload; owner logout/dimension change; commands while on a wheel, stool or upper bunk.
