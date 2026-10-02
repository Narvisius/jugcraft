# Drone Depot (the drone engine inside the Drone Tower: pads, hangars, supply pickups, visible drones)

Status: implemented in source as a **draft prototype**; not yet played.
Proposal: design notes in #23's roadmap comment; builds on Parties (#31).
Owner: @Narvisius
Target milestone and tier: Specialization (technology), after steel and aluminum
Primary specialty and supported player role: factory engineering, logistics and construction

## Player experience
**A depot is part of a Drone Tower** ([drone-tower.md](drone-tower.md)). Players don't build landing platforms or control rooms by hand any more. Tier 1 of the tower, the Command Post, builds the depot itself:
- the base floor (landing platform);
- eight ground pads;
- the ground supply pickup with its Cargo Packager;
- the terminal building, with the Drone Depot Terminal, the screen wall and the hologram table.

A terminal that isn't part of a tower flies no drones. Its status says to build a Tower Core. The Landing Platform, Landing Pad and Supply Pickup Plate blocks have no recipes; the tower places them. The old control room template and its Tech Wall Panel, Tech Floor Tile, Tech Ceiling Light and Tech Window blocks are gone.

**How the depot works:** the depot engine still reads the layout from the blocks:
- a 5×5 of pad plates forms a pad with a charger port;
- a 3×3 of pickup plates forms a supply pickup with a lift hatch.

From tier 2 on, the tower adds hangars and raised pickups.

**The pickup animation:** when a drone comes for a crate, the hatch doors slide open, the lift raises a crate, the drone hovers above and winches it up on its cable, and the hatch closes after it leaves.

**Command room screens:**
- **Control Screen Panels:** six in a 3 wide × 2 tall wall form one live display.
- **Hologram Table:** nine sections in a 3×3 form one table that projects a live map of the depot and its flights. It is drawn from data the client already has.

**Setting it up:**
- Power the terminal with any cable.
- Pipe building blocks into the packager.
- Use drones on the terminal or a pad to link them. Tower tier N allows drone tier N.

**What the drones do:** they leave their hangar or pad, fly to the nearest supply pickup and winch up a crate, fly to open build positions over hills (never through them), drop the material as the block fills in, then fly home and recharge.

Drones are visible, and each tier has its own 3D look in the tower's graphite-and-dull-red theme, sized to its hangar (small, medium or large). Rotors spin in flight and stop while charging, and the crate hangs under the drone on its cable. All nine tiers are craftable.

**Capacity:** 8 drones at tower tier 1, up to **100** at tier 9, each in its own hangar.

**Terminal controls:**
- **Right-click:** opens the terminal screen, with OVERVIEW, FLEET, JOBS and POWER tabs and a PERSONAL/PARTY button (owner only).
- **Sneak-right-click:** the owner switches the depot between **Personal** and **Party** use (the Parties rule from #31).
- **Sneak-right-click a pad with an empty hand:** returns a docked drone.

| Tier | Drone | Blocks/trip | Speed (blocks/s) | Upkeep (JE/t) | Pad size |
| --- | --- | --- | --- | --- | --- |
| 1 | Courier Quad | 1 | 4 | 8 | small |
| 2 | Survey Hexacopter | 2 | 4 | 12 | small |
| 3 | Lifter Octocopter | 4 | 4 | 20 | small |
| 4 | Ducted-Fan Runner | 4 | 8 | 32 | medium |
| 5 | Tiltrotor Carrier | 8 | 8 | 48 | medium |
| 6 | Tandem Freighter | 16 | 8 | 64 | medium |
| 7 | Hybrid Aerostat | 32 | 8 | 96 | large |
| 8 | Ion-Wind Glider | 32 | 16 | 128 | large |
| 9 | Superconducting Ring Lifter | 64 | 20 | 192 | large |

Tiers 5–9 have items and models but no recipe yet, because they need materials that don't exist (neodymium magnets, plastic composites, hydrogen, high-voltage ion emitters, superconductors).

## Connections
- **Input producers:**
  - copper wire, iron, bronze, brass, invar, aluminum, nickel and lead plates;
  - steel gears and plates (#18) and the Speed Upgrade (#19);
  - basic and advanced circuits, lithium carbonate and sulfur dust;
  - power over cables, and building blocks over item pipes (#15).
- **Output consumer:** build jobs through `BuildJobs`. The Blueprint System (#23) is the intended source. Until it exists, the development-only `/dronetest fill <from> <to> <block> [party]` command creates test jobs (it is registered only in `runClient`/`runServer`).
- **Required vs optional:** nothing requires drones. Building by hand always works.
- **Party rule:** Personal/Party is decided by `JugcraftParties.mayServe`. Job sources check it when they offer jobs, and again on arrival.

## Balance and automation
- **No resource creation:** every block delivered is taken from the packager. Anything not placed goes back into it (or drops by the terminal if it is full).
- **Pooled power, as designed:**
  - standby = 2 + 1 per drone;
  - working = the sum of tier upkeep.
  - Both are cached and recalculated only when drones are linked or unlinked.
  - The terminal holds 200,000 JE and accepts 4,096 JE/t.
  - Examples: 100 tier-1 drones = 800 JE/t while working; 100 tier-4 = 3,200 JE/t; one drone of every tier = 600 JE/t, which empties a full terminal in about 17 seconds without a power supply.
- **Low power:** new departures wait, and flights slow down in proportion. Drones never drop cargo.
- **Automation:** every recipe is a shaped crafting recipe (the vanilla Crafter can mass-produce drones), and the packager is a plain item inventory for pipes and hoppers.

## Multiplayer, persistence and performance
- **Server-authoritative, drawn on the client.** The server decides every flight. Players near the terminal get a small snapshot (`DepotView`: pads, pickup, docks, each flight's path and progress, power and jobs) when a flight starts, a drone lands or the depot changes, and every 2 seconds for the power readout. Each client then moves the drones along the planned path by itself, so everyone sees what the owner sees, and nothing is sent per tick.
- **Flights are timed records, not mobs.** Each tick costs one power calculation per terminal and one progress update per active flight.
- **Bounded work:**
  - at most 5 launches per tick per depot;
  - job requests once a second (at most 64 positions);
  - platform rescans at most once a second, and only after a platform, pad or packager block changed nearby;
  - routes take at most 64 height-map reads per leg, in loaded chunks only; unloaded chunks in between are flown over unread (each leg climbs to one cruise height over the highest ground under it, flies level, and comes straight down);
  - no chunk loading.
- **Covered positions** (under a roof or in a cave) are skipped and counted. Underground routing is part 2.
- **Reservations** stop two depots from delivering the same block.
- **Saving:** the terminal saves owner, mode, energy, fleet and in-flight drones (with their cargo and path).
  - After a restart, in-flight drones finish their trip and return their cargo, because job sources aren't saved with the flight.
  - Breaking the terminal drops every drone and returns or drops all cargo.
- **Ownership:** only the owner links, unlinks and switches mode.
- **Disable:** `drones.enabled=false` removes the recipes and stops new departures. Blocks and items stay registered.

## Dependencies and assets
- Fabric API only.
- Textures are original, drawn by `tools/drone_textures.py` in the sci-fi palette (white panels, navy glass, cyan lights, amber marks). The formed pad is one 80×80 picture sliced into 25 tiles (`landing_pad_formed_1` to `_25`).
- Models are simple cubes, except the pad plates (a 3-pixel plate, `pad_plate.json`, with a `part` state 0–25).
- No drone rendering yet.

## Verification
Recorded honestly in the PR.

Local checks:
- `tools/check_mod_data.py` passes, including a new sync check between `DroneTier.java`, `JugcraftDrones.PARTS`, `PlatformLayout.MAX_DRONES` and `tools/drones.py`.
- The core logic passes in plain Java.
- The code compiles against the real 26.3 jars.

In-game tests on a real machine are listed in the PR.

## Remaining work
- **Sounds:** rotor hum, lift and hatch sounds.
- **Underground routing:** routes through cave openings, and the NO FLIGHT PATH display.
- **Blueprint integration:** the Blueprint System (#23) as the real job source.
- **More than one supply pickup per depot** (the first one found is used for now).

## Routing round blocks (1 Oct 2026)
- Every leg's cruise height is read from the height map at every block along the line and one block either side, so drones climb over walls, roofs and hills instead of clipping them.
- A stop under a roof, overhang or the tower's own floors is reached from the side: the drone comes down through the nearest open sky within 12 blocks and flies in level (`DroneRoutes.approach`). Only stops with no such way in are skipped as covered.
- **Keeping every drone busy:** jobs are shared out so every idle drone gets one before any drone takes a second stop. When the launch limit (5 a tick) stops a dispatch with idle drones and jobs left, the depot dispatches again the next tick, so a full fleet is out within a second or two.
