# Drone Tower (tower core, nine tiers, hangars, pickups, building blocks)

Status: implemented on the drone branch (draft PR); not yet play-tested with two players
Proposal: Drone Tower proposal (project doc), design approved as v9
Owner: Narvisius
Primary specialty: logistics / building. Supports builders and anyone running a drone depot.

## Player experience
- Craft a **Drone Tower Core** (aluminium plates, processors, advanced circuits and a Drone Depot Terminal) and set it in the middle of a **15×15 plinth of chiseled stone bricks**.
- Right-click the core (or press TOWER on the depot terminal) to open the **tower screen**. It shows:
  - a picture of the tower so far, with the next tier as a dashed red outline;
  - the current tier and the drone tier it unlocks;
  - the modules the next tier needs (held / needed);
  - the build progress;
  - the **UPGRADE DRONE TOWER** button.
- Feed it **tower modules** by right-clicking the core or the terminal with them, or with any item pipe or conveyor (the core is an item storage). There are four modules: Structural, Hangar, Armour and Avionics.
- **Tier 1, the Command Post,** is built by the core itself (16 blocks a tick):
  - eight pads round a command building;
  - inside is a real operations room: a plotting table over the core, a video wall, the depot terminal desk, console desks with operator chairs, equipment racks, ceiling light panels and cable trays.
- **Tier 1 also builds the two exchanges.** They straddle the field's edge on opposite sides, and each has a port:
  - the **Energy Exchange** (west edge): a substation with transformers, cooling fins, insulator stacks and a busbar gantry. Cables and generators touching its **Energy Exchange Port** power the depot.
  - the **Storage Exchange** (east edge): a loading dock with a bay door, intake funnels and a ramp. Pipes, conveyors and hoppers touching its **Cargo Exchange Port** fill the depot's cargo packager, which every supply pickup draws from.
  - Either port also works placed by hand within 48 blocks of a depot terminal.
- **Tiers 2–9 are flown in by the depot's own drones.** Each 4×4 tile of the new tier is a build job that drones carry from the pickup and drop into place, from the bottom up.
  - **Tier 2, the Hangar Deck,** is a wide deck over the pads and command post. It stands on legs that players walk between.
  - **Tiers 3–9** form one 26×26 octagonal hangar core round a hollow atrium. It is held up by four thick, sloping, asymmetric buttresses and topped by a slender spire (y 214).
- **Hangars:** each has a landing pad (Hangar Pad plates with a red charger glow, inside a hazard-striped border) and a roll-up bay door. The door rolls up as its drone is about to leave or is within 10 blocks of coming home, and rolls down once the drone is clear or back on standby. Doors are drawn by the client from the depot's flight plan, so they cost the server nothing.
- **Uneven ground:** the tower does not fill in under itself. Where the plinth, landing field or buttress feet overhang a drop, players build their own supports or embankment.
- **Rib pickups** stand on a solid tapering bracket back into their buttress.
- **Site clearance:** stray blocks inside the tower (dirt and trees in rooms and hangars, anything under the hangar deck, the atrium) are cleared tile by tile as each tier goes in. Block entities such as chests are never cleared, and cleared blocks drop nothing.
- **Every drone has its own hangar.** Drones launch out of the hangar door and fly to the nearest **pickup**: five on the buttresses at different heights, one on the deck and one on the ground. They return the same way. All eight tier 9 drones live inside.
- **Tower tier N unlocks drone tier N.** The tower is the only way to get a depot: a terminal without one flies no drones, and the old hand-built platform and control room are gone. Capacity grows from 32 drones (tier 1) to 133 (tier 9). Tier 9 adds 9 more large hangars stacked up the spire, one per level a block apart, doors turning round the spire as it rises. Each of the 8 ground pads holds four small drones round its charger (or two medium, or one large).
- Lighting is red, set flush into floors on a neat 6-block grid. Every tier is checked (`tools/tower_lights.py`) so no floor, roof, ledge or buttress step is left at block light 0, which means no hostile mob spawns on the tower; the landing field glows faintly (light 4) for the same reason. The command room has a red light band round its walls and fifteen ceiling panels.
- The Storage Exchange's warehouse is solid inside, so nothing spawns in it.
- The plotting-table hologram shows a miniature of the tower, tier by tier, with the tier being built filling in amber as drones bring it.
- Flying drones have a rotor sound (original, made by `tools/drone_sounds.py`). It plays under **Friendly Creatures** in Music & Sounds, so that slider mutes it. Only the 12 nearest flying drones within 48 blocks make sound.
- The tower comes with 19 building materials anyone can use for other builds (concrete, girders, armour plates, blast glass, hangar doors, cladding, composite panels, light strips and more, nine of them with stairs and slabs) and five furniture blocks.

| Tier | Name | Drones | Modules (structural / hangar / armour / avionics) |
|---|---|---|---|
| 1 | Command Post | 32 | 13 / 50 / 9 / 19 |
| 2 | Hangar Deck | 56 | 30 / 2 / 4 / 2 |
| 3 | Armoured Block | 68 | 18 / 1 / 84 / 11 |
| 4 | Logistics Spire | 80 | 104 / 6 / 4 / 11 |
| 5 | Frame Tower | 90 | 360 / 5 / 2 / 11 |
| 6 | Composite Tower | 100 | 84 / 5 / 3 / 11 |
| 7 | Ceramic Bastion | 108 | 1 / 10 / 145 / 10 |
| 8 | Uranium Citadel | 116 | 1 / 9 / 450 / 10 |
| 9 | Graphene Spire | 133 | 347 / 22 / 7 / 98 |

## Hangar pads and doors
- Every hangar floor is one joined-up pad, the way the ground landing pads are: each Hangar Pad plate the tower lays shows its own piece (a hazard-striped rim, a cyan edge light, white corner brackets, touchdown markings and a glowing charger). There is one design per hangar size: a ringed charger for small bays, a long dashed box with two chargers for medium bays, and a dashed circle with arrows in for large bays (`tower/HangarPadBlock` part numbers, art from `tools/tower_art.py`). A Hangar Pad placed by hand is a loose plate.
- The roll-up bay doors are drawn in the tower's steel: interlocking slats that ride up with the door into the drum under the lintel, over a hazard-striped bottom rail with a rubber seal.

## Blueprint preview
- Hold the Drone Tower Foundation blueprint and **shift+scroll** to step through the finished tower, tier by tier. The space it will take shows in amber, so you can see how much room it needs before you build.
- Any blueprint with a stages file (`assets/jugcraft/blueprint_stages/<id>.json.gz`, written by `tools/drone_tower.py`) gets the same preview.

## Connections
- **Inputs:**
  - steel and titanium plates;
  - the new building blocks (reinforced concrete, steel girders, hangar doors, armour plates, red light strips);
  - drone motors, advanced circuits and processors;
  - a Drone Depot Terminal for the core.
- **Output:** drone capacity and drone tiers for the [Drone Depot](drone-depot.md). The building materials are also decoration.
- **Entry path:** the Tower Core is the way into drones: its tier 1 builds the first depot (8 pads, tier 1 drones). Every input of the core and the tier 1 modules is craftable before that.
- **Drone recipes:** all nine drones are now craftable (`tools/drones.py`). Tiers 5–9 use new parts made from real materials:
  - neodymium motors, tilt-rotor nacelles and composite rotors;
  - hydrogen lift cells (chemical reactor: 2 plastic sheets + 1000 mB hydrogen);
  - ion emitters;
  - superconducting tape, Stirling cryocoolers, superconducting motors and lift fans.

## Balance and automation
- **Module costs match the blocks.** Each tier's modules are worth what crafting its blocks would cost (`tools/tower_costs.py`). Every block is valued in steel-ingot equivalents by following its recipe down to base materials, the blocks are grouped by module family (structural, armour, hangar, avionics), and a tier needs enough of each module to cover its family's value. The avionics count is never below the tier number. So tiers built of expensive blocks cost more: tier 5's tungsten-steel frames need 360 structural modules, and tier 8's depleted-uranium armour needs 450 armour modules. A Structural Module (5 steel blocks, 2 girders, 2 reinforced concrete) is worth about 48 steel ingots, and an Armour Module (4 steel armour plates, 4 steel blocks, 1 titanium plate) about 46. The data check recomputes all of this from the recipes and fails if a recipe change makes modules cheaper or dearer than the blocks. A core holds up to 1024 of each module.
- Building is bounded:
  - the core places at most 16 blocks a tick for tier 1;
  - tiers 2–9 go one 4×4 tile per drone delivery, inside the depot's normal launch limit (5 a tick);
  - tiles are never offered over an unfilled tile below.
- Tower blocks are ordinary blocks. Breaking them drops them (slabs drop two when doubled); there is no refund of modules.

## Multiplayer, persistence and performance
- **Server authority:** the server decides everything. `TowerUpgradePayload` is checked for reach (≤ 10 blocks), the owner, or a party member when the linked terminal is in Party mode (`JugcraftParties.mayServe`), the plinth, the terminal and the module counts.
- **Saved state:** the core saves its tier, the tier being built, the filled tiles (a bitset), the modules and the owner. The terminal saves its tower link.
- **Unloaded chunks:** building pauses while the chunk is unloaded and resumes after a restart.
- **Data file:** the tower layout is one data file (`data/jugcraft/drone_tower/tower.json.gz`) generated by `tools/drone_tower.py`. It holds each tier's blocks to place and clear, its hangars (docks with exits), pickups and costs. `TowerData` loads it once.
- **Existing blocks:** placing never replaces another block entity.

## Dependencies and assets
- Fabric API only.
- Textures are original, drawn by `tools/tower.py`. Furniture models are box lists, also generated there.
- The tower-screen pictures are drawn by `tools/drone_tower.py screen_image`.

## Verification
- **Local checks:**
  - `tools/check_mod_data.py` passes; its new `check_tower` keeps `JugcraftTower` in sync with `tools/tower.py`.
  - The code compiles against the 26.3 jars.
- **Server game tests (`TowerGameTests`):** data consistency, the core building the Command Post, the upgrade needing the plinth and modules, tower tier gating drone tiers, a finished tower holding 100 drones, and drones flying the next tier in.
- **Client game test (`TowerClientGameTests`):** screenshots of the plinth, the tower screen, tier 1 and the command room, the tier 2 deck, drones building tier 3, and tier 9.
- **Results on Narvisius's Windows PC (1 Oct 2026, commit 7c9d1ed):** `gradlew build` passed with all 125 server game tests, including the six tower tests. `runClientGameTest` passed, including `TowerClientGameTests`; its screenshots were checked by eye. The client receives all 32 docked drones of a full tier 2 tower.
- **The first run found two bugs, both fixed:**
  - tower tiles at the buttress feet were skipped as "under cover", so tier 3 never started;
  - the server test counted the wrong layer.

## Rollout and open questions
- Small drones (tiers 1–3) are small and dark, so they are hard to spot in a hangar from a distance.
- Flights may climb above the tower top on legs that cross it.
- There is no ops command for instant builds yet; the tests use `buildInstantly`.
- **Testing aids (creative only, no recipes):**
  - Creative Energy Cell: endless power on all sides.
  - Creative Supply Crate: a depot within 48 blocks gets every building block its drones ask for.
- **Chairs:** players can sit on Operator Chairs (right-click to sit, sneak to stand).
- Not yet tested with two players on a dedicated server.
