# Tri-Pillar Drone Tower and the new drone fleet

Owner-approved redesign (Narvisius, October 2026), implemented with Claude (Opus 5.5). Design decisions and mockups:
the project doc `jugcraft-drone-tower-redesign.md` (rounds R1-R12).

The Tri-Pillar is now the only Drone Tower design. Towers built in an earlier design are rebuilt as the Tri-Pillar
automatically (see Conversion).

## The holograms (28 October 2026)

Justin: "make sure people can't craft any of the holograms. I want those to be exclusive to the tower ... once they're
down ... They stay there unless destroyed in creative mode" ("Those four + Holo Panel Emitter", "Nothing: they come
free", "Only creative can place"). The Hologram Table (the model table's sections), the Holo Command Table, the Holo
Projector, the Big Map Table and the Holo Panel Emitter (`tower/Holograms`):
- have no recipe (tools/advanced_crafting.py TOWER_ONLY deletes any that is written); the tower puts them in as it
  builds and doesn't charge for them (TowerCosts.CHARGED leaves them out; the frozen designs that cost every block skip
  them too), so the foundation costs about 140 SIE less (21,115);
- can't be mined in survival (as bedrock), stand any explosion, aren't moved by pistons, and are in the wither_immune
  and dragon_immune tags; a creative player breaks one as any block (the multi-block ones come down whole);
- place from their items only for a creative player; in survival the item says "Holograms are the Drone Tower's own"
  and places nothing.
The core and the consoles, which had emitters in them, take the emitter's glass pane, glowstone and circuits instead
(tools/advanced_crafting.py). Test: TowerGameTests.hologramsAreTheTowersOwn.

## Placing the core and loading the tower (28 October 2026)

Justin: "When somebody ... attempt[s] to put down the drone tower core, I want you to make a pop-up on their screen that
says, warning, be sure this is the location you want to select for your drone tower. It is a extremely large structure,
and then give them the dimensions of the tier 9 building"; then "Just after they do place tower here, just have it
advance to the next screen where they can shove blocks into the drone tower core ... they can only put in up to as many
as it requires", "After you build the first layer, then putting any more blocks in, it has to go through the cargo
storage", and the tower storage "10 double chests worth". Answers: "Foundation + tier 1, one list"; the cargo hall's
packing is only its look ("I don't want it actually doing anything"). Mockups approved: tower_core_warning_mockup,
tower_core_loading_mockup.

- **The warning** (`tower/TowerPlacement`, `client/TowerPlaceScreen`). Using the core on the ground places nothing at
  first. The usual checks run first (room for the block, another of your towers too close: the old message, no
  warning); then the server asks your client, which shows WARNING, "Be sure this is the location you want to select for
  your Drone Tower. It is an extremely large structure.", a plan of the finished tower (every column, shaded by height,
  the core in red) and its size, worked out from the tower's own data (`TowerData.extent`): **105 blocks wide
  (east-west), 99 long (north-south), 265 tall (261 up from the core, 3 down)**; it reaches 52 east and west, 53 north
  and 45 south of the core. CANCEL or Esc: nothing happens. PLACE TOWER HERE (greyed for its first second, so a double
  click can't place it) has the server place the core where you clicked, as the click would have (reach, the core in
  that hand, claims and spawn protection are checked again), and opens the loading screen. A client that can't show it
  (a test's mock player) gets the size in chat and places it by using the core on the same spot again within 10 s.
  Dispensers and automation's fake players place it without asking.
- **The first layer** (`tower/TowerLoadMenu`, `client/TowerLoadScreen`): the foundation and tier 1, which the core
  builds itself, as one list (11 kinds, 47,709 blocks: in / needed, a bar, finished rows green) over your inventory.
  Click or shift-click stacks in, drop a stack on PUT BLOCKS IN (a right-click puts one), or LOAD ALL; only what is still
  needed goes in, the rest stays with you. Right-clicking the core opens it again; until tier 1 starts the core also
  takes these blocks from the tower's terminal, a sneak-right-click with an empty hand, and pipes or hoppers. In
  creative LOAD ALL fills it. **BUILD FIRST LAYER**, live once everything is in, pays the foundation and builds it, then
  goes straight on to tier 1 from the blocks already in (`TowerCoreBlockEntity.buildFirstLayer`, `first_layer_queued`).
- **After the first layer** the core takes no blocks by hand, terminal or pipe (it says to use the Cargo Exchange).
  They go in through the **Cargo Exchange** into the **tower storage**: right-clicking the exchange opens the same
  screen for the storage (the next tier's blocks, how many of its 540 stacks are used, LOAD ALL), and pipes, conveyors
  and hoppers on its port fill it too. It takes the tower's blocks only, up to what the rest of the tower needs (more
  than the next tier is fine) and what fits in 540 stacks (10 double chests, `STORAGE_STACKS`); anything else put into
  the exchange still goes to the depot's packager as before (the drones' blueprint supply). UPGRADE is at the Tower
  Control console's TOWER page (the tower screen shows the storage's stacks); tiers 2-9 are built by the drones.
- The cargo hall (the belt from the exchange, the Case Packer, the stacker, the pallet drone and the goods lift) stays
  look-only: it already runs while drones are out and moves nothing.
- Tests: TowerGameTests.coreAsksBeforeItIsPlaced, firstLayerLoadsOnlyWhatItNeedsThenBuildsBoth,
  afterTheFirstLayerTheCargoExchangeFillsTheStorage, blocksOnTheTerminalLoadTheTower (now during the first layer);
  TowerPlacementClientGameTests (the real popup, CANCEL, PLACE TOWER HERE, the loading screen, LOAD ALL and BUILD, with
  screenshots).

## Starting a tower (October 2026: no blueprint, no plinth, no modules)

- Craft a **Drone Tower Core** on the Advanced Workbench (mid game: 4 steel blocks, 4 steel plates, 2 processors, 3 advanced
  circuits, a basic circuit, 2 glass panes, 2 glowstone dust and a Drone Depot Terminal).
  Holding it shows, in amber, the space the tower will take where you aim (the foundation; shift+scroll steps through
  the tiers to the finished tower), so you can leave room (`client/TowerCorePreview`).
- Place it on the ground where the middle of the hall should be. Set on top of level ground it moves down into the ground
  block (`TowerCoreBlock.sinks`), so the hall floor it builds is level with the land; set into a hole it stays.
- **Every step costs exactly the blocks it places** (`TowerData.cost`, `TowerCosts`): the foundation about 44,300 items
  (13.2k reinforced concrete, 10.7k ceiling tiles, 5.9k epoxy floor, 5.4k access floor, 3.2k armour plate, 2.2k framed
  glass... down to the desks, chairs, screens, both exchanges, the terminal and packager), tier 1 the roof's 294 hangar
  pads, tiers 2-9 from 7.3k to 13.1k items. A fitting one item places whole (an exchange, the master console, a storage
  rack) counts once; door halves once; a speed gate's pedestals nothing.
- The core takes the first layer's blocks only (the foundation and tier 1), and no more; after that the tower's blocks
  go in through the Cargo Exchange into the tower storage (see "Placing the core and loading the tower" below). The
  tower screen lists the next step's blocks (held / needed, scrolls) and UPGRADE pays them.
- A broken core keeps its store in its item (`jugcraft:tower_store`), so nothing is spilled.
- The tower modules are gone (a core saved with modules simply no longer has them). The Drone Tower Foundation
  blueprints are gone.

## What the player gets

- **New drones.** All nine drone tiers have new detailed models (T1 Courier Quad, T2 Survey Hexacopter, T3 Lifter
  Octocopter, T4 Fan-Wing Runner, T5 Tiltrotor Carrier, T6 Disc Freighter, T7 Spine Carrier, T8 Halo Ion Carrier, T9
  Armoured Dropship). At a ground depot (no tower) the bigger ones are drawn smaller so they stay on their part of a 5x5
  pad (`DroneModel.groundFit`); in the tower's bays they are full size (tools/drone_mesh: the approved mockup models
  exported to `assets/jugcraft/drone_mesh/t1..t9.bin` and `textures/entity/drone_fleet.png`). Each tier has parked
  (gear down), hover, cruise and empty (crate on the winch) variants; rotors and fans turn about their own axes.
  `DroneMesh` reads them; `DroneModel` draws them. Flight, sizes and capacities are unchanged.
- **The foundation** (R12): the control building, a hexagon 104 blocks across with two storeys. The core
  places it itself after the first UPGRADE (once all its blocks are in) (64 blocks a tick, about 35 seconds). It holds the framed-glass hall with the
  central holo table, master console, curved video walls on the three cut pillar feet, the flight-ops trees (the operator
  rows until layout 44), reception, the
  atrium open up to the roof (and, from tier 2, to the glass lantern), stairs to the upper floor and the roof, on the
  ground floor the crew cafe, the security office, the restroom and washroom, the locker room with its showers and the
  battery and UPS room (layout 24), and on the upper floor the map room with its Big Map Table (layout 23) and twelve
  rooms fitted out from the room kits (layout 25: briefing, manager, lab, first aid and crew rest, planning (taken out
  in layout 33), two offices, the crew lounge, simulators, training, meeting, the restrooms (taken out in layout 38) and the custodian's
  closet).
  Since layout 37 (work list item 20) the stations are part of it: the Energy and Cargo Exchanges in the east point
  with their ports in the intake face, the depot terminal and the packager in dispatch (under the cargo hall's floor
  since layout 40), the goods lift up to the supply pickup (on its house's top since layout 40; the roof's landing
  platform before) (the exchanges bind only within 48 blocks of the terminal). Until layout 36 they stood outside in a
  yard east of the building with a 5x5 pad, the pickup and the packager.
- **Tier 1, roof pads**: six landing pads on the roof for 24 tier-1 drones, also placed by the core.
- **Tiers 2-9**: three chamfered pillars with hangar bays on their outer faces (small at tiers 2-3, medium at 4-6,
  large at 7-9), a tie deck at every tier line, a glass lift core from the tier-2 deck up, service ladders, supply
  cradles at tiers 2, 4, 6 and 8 (each on a braced pair of raked struts, layout 22) and a mast on top. Tier 2 also closes the atrium with the glass lantern. Drones fly
  these tiers in tile by tile, as before.
- **Bays** have iris doors (four petals, drawn by the depot renderer from each dock's door), a lit bay number tag and a
  status lamp on the right jamb (green: drone home, blinking amber: out, dim red: empty).
- **Hall fittings** (R8 version A + R9/R10 version B), all live from the tower's depot:
  - neon floor lines from the table's ring to the entrance and the operator areas; glass floor strips over lit cable
    trays out to the video walls; thin neon white tubes up the pillar feet's corners (layout 21; end rods until 20); two
    rings of ceiling light bars;
  - five **touch consoles** round the central table (they open the depot screen like a desk);
  - the **Holo Projector** in the lobby past the gates, throwing up a big hologram head that watches whoever is nearest
    (see below; layouts 8-18 had floating holo panels FLEET / TOWER / SUPPLY over each operator area instead);
  - the **data hall** in the hall's west corner: the nuclear plant's racks in a glass room with a contained cold
    aisle (see below; layouts 8-16 had glass-door equipment racks behind each operator area instead);
  - two **Drone Display Cases** past the gates (a tier-4 and a tier-1 drone turning under glass, the name running round
    the light hood; see below), an **info totem**,
    four-lane **security gates** in a glass vestibule, a lounge round the **Long Coffee Table** and the **Crystal Tree**
    (see below);
  - a **drone service bay**: a pad, a scissor-lift cradle with a tier-4 drone and a 6-axis **robot arm** working
    over it (see below), and the parts wall: two units of the **Drone Parts Shelving** (see below);
  - the **model table**: the small 3x3 holo table by the lounge bearing the **tower diorama**, with its desk row (working
    the tower's build) and chair (see below).
- **The tower hologram** (R11, October 2026): the central table shows the tower as built, a solid amber model at 1:12
  floating over the table, lit from within (colours baked per face, drawn with every normal up so the world's shading
  leaves them; dark bands at the tier lines, the tier being built brighter as it goes in, a faint glow round it), turning
  slowly in place (one turn in 90 seconds), under a projector that
  hangs on three trusses from the atrium's ceiling (the roof's skylight, then the lantern's top from tier 2). A status
  ticker runs round the atrium's amber slab edge: tier, build progress, fleet, drones out, battery, supply, jobs.
- **The tower diorama** (redesign 3; until layout 25 the small table showed R10's topographic map): a still scale model
  on the small table: the finished Tri-Pillar in 20 chunks of plain Minecraft land, true colours, lit by the room (see
  the fixes below).
- **No mob spawns**: every floor of the tower has block light. `tools/tripillar_lights.py` works out the light the
  tower's own lamps give and sets flush deck lights into any floor left darker than 2 (on a diagonal lattice on the
  decks, so they look laid out; about 150 in all), neon floor lines since layout 21.

## The Holo Command Table (work list item 5, approved 22 October 2026)

The central 3x3 holo table and the five touch consoles round it are replaced by one round **Holo Command Table**
(`tower/HoloCommandTableBlock`, generator `tools/holo_command_table.py`): 11 blocks across at standing height, a graphite
top on a recessed drum with a lit toe kick, a brushed aluminium edge with an amber LED line, a smoked-glass projection
field the size of the hologram's footprint with 36 projector lenses round it, and five touch stations set into the rim
on the south half. One item places it whole (109 parts, the middle one over the core holds the pinned depot display);
the tower's cost counts it once. `client/HoloCommandTableRenderer` draws its mesh (`assets/jugcraft/tower_mesh/
holo_command_table.bin.gz`, texture `textures/entity/holo_command_table.png`) and the live parts: the site plan at 1:12
in the field (the hexagon, the pillars once tier 2 stands, the roof pads, every drone within the site, a radar sweep),
the five screens (POWER, SUPPLY, FLEET, JOBS, TOWER) with status lamps, and over the core the tower hologram, projector
and ticker as before. A click on it opens the depot screen. The master console moved back to z -10 to make room (its
redo is item 6, below).

Layout 9 (`TowerData.TRIPILLAR_HOLO_TABLE_LAYOUT`) is this revision; layout 8 is frozen as
`tower_tripillar_v8.json.gz`. A saved layout-8 tower is refitted when it loads (the conversion): its old tiers and its
foundation are cleared where the new design differs, blocks the new design has in the same place stay (a storage rack
keeps what it held), and it is rebuilt to the tier it had; the depot is handed over and back.

## The Tower Control Console (work list item 6, approved 22 October 2026)

The old 3x2 master console is replaced by the **Tower Control Console** (`tower/TowerControlConsoleBlock`, generator
`tools/tower_control_console.py`), a curved three-position control-room console at the head of the north nave, facing
north (its operators look south over their monitors to the table): a light laminate worksurface with a black soft edge,
a technology riser (monitor rail, task light under its lip, amber accent line), vented equipment bays to the table side
with backlit position signs (FLEET + JOBS, TOWER CONTROL, POWER + SUPPLY), pedestals and footrests in the knee space,
end panels. Each position has three monitors on arms, a keyboard and mouse, a loop panel, a desk phone or (TOWER
CONTROL) the switch box with guarded UPGRADE ARM / RECALL ALL switches, the MODE knob and eight lamps, a notepad, a mug
and a headset on a hook. One item places it whole (37 parts: the cells of its carpeted floor band, whose curved step
follows the table; part 0, under TOWER CONTROL, holds the pinned depot display); the tower's cost counts it once.
`client/TowerControlConsoleRenderer` draws its mesh (`tower_mesh/tower_control_console.bin.gz`, shaded per corner with
baked ambient occlusion and the task lights' pools; texture `textures/entity/tower_control_console.png`) and its live
parts: nine monitor pages (FLEET, SITE radar, JOBS; TOWER, ALERTS, BAYS; POWER with the battery trace, SUPPLY, LINK), three
clocks (the world's time and day), three annunciator modules and the switch box's lamps (BUILDING blinks while a tier
builds), the phones' displays, the loop panels' TALK key; and the floor band's step wherever it ends with nothing to
carry the floor on. A click on any part opens the depot screen; it has a placement preview.

With it come two blocks: the **Raised Floor** (`tower/RaisedFloorBlock`, 3 px, carpet tiles; aluminium nosing and an
amber step light on each side that steps down) filling the nave wall to wall behind the console back to z -12, and the
**Console Chair** (`tower/ConsoleChairBlock`, 16 rotations, drawn pulled up to its desk by `client/ConsoleChairRenderer`,
sit-able; set on a raised floor it takes the floor's cell and carries it on, and gives both back when broken; the tower
counts it as the chair and the floor tile). Three chairs stand behind the positions.

Layout 10 (`TowerData.TRIPILLAR_CONSOLE_LAYOUT`) is this revision; layout 9 is frozen as `tower_tripillar_v9.json.gz`.
A saved layout-9 (or 8) tower is refitted on load as before (`TowerConversionGameTests.holoTableTriPillarIsRefittedWithTheControlConsole`).

## The rotunda and the Curved Video Walls (work list items 19 and 7, approved 22 October 2026)

**The rotunda cut** (item 19): on the ground storey the three pillar feet are cut back to r 15 round the hall's axis
(the core cell's middle), so the hall round the Holo Command Table is a round rotunda; the next ring of cells (out to
16.2) is each foot's new concrete face. Over the cut the soffit is a storey's slab higher (y 11), so the slab edge at
y 10 runs on round the circle over the faces, lit like the rest of the edge, and the status ticker (drawn by the
table's renderer where its edge is built: `HoloMapRenderer.edgeBuilt`) now runs all the way round. The hall floor runs
on under the cut, ceiling lights are set in each soffit, and the glass floor strips over the cable trays run out
to the walls' plinths. The riser ladders' feet were in the cut: each foot's ground-storey ladder now goes up in its
outer corner through the slab to the riser room over it, where the old ladder goes on up from the raised floor over
the cut. The old 3x2 control screens on the feet are gone.

**The Curved Video Walls** (item 7; `tower/CurvedVideoWallBlock` and `CurvedVideoWallPartBlock`, generator
`tools/curved_video_wall.py`): a fine-pitch LED wall on each cut face, built concave round the hall's axis: 30 cabinet
columns by 22 rows of 0.6 x 0.3375 cabinets (17.9 blocks along the curve, 7.4 high, the LED face 14.62 from the axis),
no bezels, in a black powder-coated surround (a plinth of vented controller doors with green status LEDs over a kick
recess, bottom and top trims each with an amber LED line, a bulkhead to the top, end posts with an amber line closing
back to the concrete). One item places a wall whole: its master (the bottom of the middle column, holding the pinned
depot display) and about 250 parts, each knowing its offset from the master and colliding with the wall's body in its
cell. A wall is aimed at its axis: one of 84 whole-block vectors from the master cell to the axis cell, 14.5 to 15.4
long, so the walls of a ring share an axis exactly (in the tower, the core's cell); placed by a player it faces them,
its axis 15 blocks in front. Using it with an empty hand turns to the next board. The walls give light 7.
`client/CurvedVideoWallRenderer` draws the mesh (`tower_mesh/curved_video_wall.bin.gz`, texture
`textures/entity/curved_video_wall.png`) turned to its aim, and on its 30 facets (a 2400 x 1000 canvas, split at the
facets' edges) the board, live from the depot, in the layout of the approved mockup:

- **OPERATIONS** (wall A, south, facing the console): the site from above (the hall, the pillars, the roof pads, every
  drone docked or out with its track; the map widens to take in the farthest drone), the tower by tier (the status
  screen's tier-9 picture tinted: built, the tier building and how far, still to come), status tiles (fleet, airborne,
  jobs open, covered), the alerts with the time each began, a ticker;
- **FLEET BOARD** (wall B, north-east): every flight (drone, tier, model, where it is going, what it is doing, when it
  will be home, how far through), drones at home, every hangar bay of each pillar home, out or empty, the roof pads; a
  ticker of drones going out and coming home;
- **LOGISTICS** (wall C, north-west): what the supply store holds most of (the depot view now sends its 12 most
  plentiful kinds), the battery and the draw over the last in-game day, the two exchanges (net energy and cargo
  coming in), what the tower is short of, a ticker.

Unlinked, a wall says so. The tower's cost counts each wall once (its parts have no item).

Layout 11 (`TowerData.TRIPILLAR_VIDEO_WALL_LAYOUT`) was this revision; layout 10 is frozen as `tower_tripillar_v10.json.gz`.
A saved layout-10 (or older) tower is refitted on load (`TowerConversionGameTests.consoleTriPillarIsRefittedWithTheVideoWalls`).

## The Drone Display Cases and the Ticker Rail (work list item 1, approved 22 October 2026)

**The Drone Display Case** (`tower/DroneDisplayCaseBlock` and `DroneDisplayCasePartBlock`, generator
`tools/drone_display_case.py`): a museum vitrine for a drone, built the way gallery showcases are, 5 x 5 cells and 3.37
high. A graphite powder-coated plinth 0.62 high on a shadow-gap kick (an amber LED line in it washing the floor round
it), three access panels a side (climate vents at the sides, a key lock at the back), a brushed stainless edge band, the
glass standing in a gasketed channel round a charcoal fabric deck; a frameless hood of low-iron glass 4.8 x 4.8 x 2.45
(green edges where the panes meet, fine black silicone joints); a light hood 0.30 high on top (stainless bands, two
linear LED diffusers and four spots under it), and between its bands the ticker rail: one LED dot band, 187 columns by
9 rows a side, running round all four sides. Inside, a turntable with a light ring; the drone stands on it on its gear
and turns once a minute (a drone too big for the glass is drawn smaller to fit). The drone's name runs round the rail
in four evenly spaced spots ("T4 · FAN-WING RUNNER"), reading on round the corners as a visitor in front of each side
reads it; there is no placard (Justin: the rail names the drone). One item places the case whole, facing the player
(the master is the middle cell on the floor, the other 99 cells are parts that know their offset from it and collide
with the plinth, the glass panes and the light hood in their cell); using it with an empty hand shows the next tier's
drone. The light hood's cells give light 12. `client/DroneDisplayCaseRenderer` draws the mesh
(`tower_mesh/drone_display_case.bin.gz`, texture `textures/entity/drone_display_case.png`; the mesh format now has glass
and glow quads), the drone and the rail. The tower's cost counts each case once.

In the Tri-Pillar the two cases stand past the security gates in the lit floor rings the first lit plinths stood in
(T4 west at (-14, -38), T1 east at (13, -38)), their fronts towards the entrance.

**The Ticker Rail** (`client/TickerRail`): a system for running text along a path, for putting on things (Justin asked
for it as a system, not a block). A rail is a path of straight pieces and arcs at one height, read from one side (each
piece from the right-hand side of the way it runs, so text reads on round a loop's corners), in one of two styles:
**FONT** (smooth lettering in the game's font, a character a slot, on a dark band) or **DOTS** (a 5 x 7 LED dot matrix,
`tools/dotfont.py`, stepping a column at a time). The content is the text over and over end to end, or a number of
evenly spaced copies of it; a gate can leave out the parts of the rail that are not there. The hologram's status ring
round the rotunda (FONT, repeating, gated on its slab edge) and the display cases' hoods (DOTS, four copies) run on it.

Layout 12 (`TowerData.TRIPILLAR_DISPLAY_CASE_LAYOUT`) is this revision; layout 11 is frozen as `tower_tripillar_v11.json.gz`.
A saved layout-11 (or older) tower is refitted on load (`TowerConversionGameTests.videoWallTriPillarIsRefittedWithTheDisplayCases`).

## The Crystal Tree (work list item 2, approved 22 October 2026)

**The Crystal Tree** (`tower/CrystalTreeBlock` and `CrystalTreePartBlock`, generator `tools/crystal_tree.py`): the
lobby's feature tree, an alien tree of our own design (Justin asked for something in the spirit of a TV show's glowing
tree, but better; the show's design is not copied). A trunk of four dark glossy strands braided round each other with
gold veins glowing in the grooves between them; limbs that twist and wander like a real tree's, some long and low, some
short, branching as they go, bowing over but never plunging; gnarled roots out of the strands, diving under black basalt
chippings and coming up again; about 400 thin dark woody threads hanging from the limbs at all lengths, carrying about
1,300 faceted gold crystals lit from within, each with a slight glow (a soft halo turned to the camera); a few basalt
rocks and clumps of glowing bead moss. Pulses of light run up the veins, out along the limbs and down through the
crystals; the threads sway, each with its crystals. It stands about 8 blocks high in a round planter 7.1 across (precast
graphite wall 0.88 high, stainless cap, the gravel at 0.70) with an iroko-slatted bench round it (seat 0.46 high, 8.2
across) and an amber under-glow on the floor.

**Every tree placed is its own** (Justin: "make a different variant for each tree you put down"): the tree is grown in
the client (`client/CrystalTreeGeometry`, about 50 ms, 48-53k quads; a bigger tree spaces its threads wider, so it costs no more to draw)
from a seed made of the master's position (`CrystalTreeBlock.seed`), so the same tree always grows in the same place and
no two places grow the same tree. `client/CrystalTreeRenderer` keeps the last six trees grown, draws the planter and
bench (`tower_mesh/crystal_tree_planter.bin.gz`), the bark in the world's light, and the veins, crystals and halos
fullbright, their brightness read each frame from where they are along the tree (`CrystalTreeGeometry.pulse`). One item
places the tree whole (recipe: glowstone dust, amethyst, dark oak logs, an oak sapling, reinforced concrete and dark
oak planks); the master is the middle cell on the floor; its 71 parts are the planter's and bench's cells round it
(colliding with the gravel, wall and seat on an eighth-of-a-block grid) and three trunk cells over it. The crown does
not collide. The master gives light 9, the trunk's cells light 12. The tower's cost counts the tree once.

In the Tri-Pillar one Crystal Tree at (14, -27) replaces the lobby's four planters.

Layout 13 (`TowerData.TRIPILLAR_CRYSTAL_TREE_LAYOUT`) is this revision; layout 12 is frozen as `tower_tripillar_v12.json.gz`.
A saved layout-12 (or older) tower is refitted on load (`TowerConversionGameTests.displayCaseTriPillarIsRefittedWithTheCrystalTree`).

## The Long Coffee Table (work list item 3, approved 22 October 2026)

**The Long Coffee Table** (`tower/LongCoffeeTableBlock` and `LongCoffeeTablePartBlock`, generator
`tools/long_coffee_table.py`): one table for the lobby's lounge in place of the three small glass ones. A 4.0 x 1.1 slab
of solid iroko (the planter bench's timber), four boards glued up, 50 mm thick with a knife edge (22 mm of edge, bevelled
back underneath), its top 0.40 high; a runner routed down its middle, 3.39 x 0.30, lined with stainless steel, holding
smoked glass over an amber LED dot board in two lines of 226 x 9; two graphite powder-coated sled frames on felt glides;
an amber LED line under each long edge washing the floor softly. Along the runner run the words Justin gave it, "DRONE
TOWER BROUGHT TO YOU BY NARV-TECH.", over and over: each line reads to the sofa on its side (`client/LongCoffeeTableRenderer`
lays a `TickerRail` of dots flat on each line's live face in the mesh). On it: two of the nuclear plant's mugs (Justin's
pick: `tools/plant_office4.mug`, the plain white mug with coffee in it), three magazines, a tablet showing the fleet, and a
1:6 model of the T1 drone on a stand. Its middle is the north-west corner of its master cell (facing north; `facing` turns
it about the master cell's middle), so it covers 4 x 2 cells: the master and 7 parts, each colliding with the top in its
cell. One item places it whole (recipe: three jungle slabs, two tinted glass, an advanced circuit, two steel plates). The
nuclear plant's own oval table keeps the id `lounge_table`.

In the Tri-Pillar the lounge is west of the lobby where the floor plan puts it: two rows of six sofas facing each other
(z -29 and -26, x -17..-12) with the table between them, centred on (-14, -27); the two lounges by the entrance are gone
(the east one stood among the workshop's racks).

Layout 14 (`TowerData.TRIPILLAR_LONG_COFFEE_TABLE_LAYOUT`) is this revision; layout 13 is frozen as
`tower_tripillar_v13.json.gz`. A saved layout-13 (or older) tower is refitted on load
(`TowerConversionGameTests.crystalTreeTriPillarIsRefittedWithTheLongCoffeeTable`).

## The speed gates remade (work list item 4, approved 22 October 2026)

The vestibule's four lanes (in at x -3 and -1, out at 1 and 3) keep their cells and their blocks; the gate itself is
remade realistic, the way real sliding speed gates are built (Boon Edam Speedlane Slide, Automatic Systems SlimLane):
brushed stainless cabinets 0.66 wide and 1.04 high under a black glass top with the card reader's mark glowing under it,
white/green guide lines along the lane, infrared sensor windows down the lane faces, pictograms on the cabinet ends
(green arrow the way in, red cross the wrong way), 1.55 m tempered glass doors (frosted dots, rubber safety edge, a status
light on top) that slide into stainless-framed glass housings over the cabinets. No sign gantry. The same blocks stand
in every Nuclear Center gatehouse, which Justin asked to have the new gates too, so both changed together (see
`docs/features/nuclear.md`, Speed Gate). Tower layout unchanged (14).

## The service arm remade (work list item 8, approved 22 October 2026)

**The Drone Service Arm** (`drone_service_arm`; drawn by `client/ServiceArm`, meshes `tools/service_arm.py`): a real
6-axis industrial robot in the tower's livery (graphite castings, amber joint rings, no maker's look), about 2.1 m of
reach. A steel floor plate with four anchor bolts and a 0.30 riser with a hazard band; the base casting with its cable
connector at the back; the turret (axis 1) and its motor; the shoulder (axis 2) with its gearbox discs, motor and a
gas-spring counterbalance; the upper arm (0.95); the elbow (axis 3) and its motor; the forearm housing with the wrist
motors and the forearm tube (1.02 to the wrist); the wrist and the tool flange with a nut runner and a camera with a
ring light. A dress pack of cables runs from the base's connector up the turret, along the upper arm in two clamps and
round the elbow; it and the counterbalance flex as the arm moves. Each part is a mesh in its own joint's frame, posed
every frame by the arm's inverse kinematics.

It works the T4 on the cradle in a 25-second cycle: over the near rear fan, down onto its motor's nut with the runner
turning, up, across to the near front fan, down and up again, a camera pass along the airframe with the ring light on,
and back to its parked pose. Justin's note on the first mockup was that the arm went through the drone's body; tail-on
to the arm (as the bay had it) the arm's links would always cross the tail fins, so the drone now lies along its cradle
side-on to the arm, and every pose and every move between them was checked against the drone's own mesh: no part of
the arm comes within 9 cm of the airframe, only the socket goes down into a fan's duct onto the hub. On the cradle the
drone's plan is centred (`DroneMesh.middle`; the T4's origin is 0.31 behind its middle).

A yellow and black keep-clear square (1.7 m) is painted round the base. Like a robot cell's light curtain, the arm stops
where it is while a player stands in it and carries on when they step out. The arm's cell collides with its base and
turret, a block high. Its item is an icon (`tools/item_icons/drone_service_arm.txt`); the block has no model of its own.

Layout 15 (`TowerData.TRIPILLAR_SERVICE_ARM_LAYOUT`) is this revision: the service bay's stand faces north (it faced
east, the drone tail-on to the arm). Layout 14 is frozen as `tower_tripillar_v14.json.gz`; a saved layout-14 (or older)
tower is refitted on load (`TowerConversionGameTests.longCoffeeTableTriPillarIsRefittedWithTheServiceBayTurned`).

## The Drone Parts Shelving (work list item 9, approved 22 October 2026)

**The Drone Parts Shelving** (`tower/DronePartsShelvingBlock` and `DronePartsShelvingPartBlock`, generator
`tools/drone_parts_shelving.py` with `tools/parts_shelving_parts.py` and `tools/parts_shelving_art.py`, renderer
`client/DronePartsShelvingRenderer`): the workshop's parts wall, in place of the two warehouse racks behind the service
bay. Industrial longspan shelving 2.94 wide, 0.60 deep and 2.30 high: graphite perforated uprights in three frames with
zig-zag bracing on the end frames, amber step beams with the bay labels (location, description, barcode) in clear
holders, galvanised wire-mesh decks, a pegboard back. Justin turned the first mockup down ("some of these just look like
plain boxes with a label slapped on", the extinguisher and the batteries too, and a barcode over its text), so every part
on it is modelled the way the real thing is made: slotted corrugated cartons printed straight on the kraft, with their
flaps, tape and shipping labels (one opened, retail motor boxes in it); hopper bins with their parts heaped in them and
label windows; moulded cases (latches in recesses, fold-down handle, hasps, purge valve, an engraved plate on the lid);
smart packs standing in a battery station; LiPo packs charging in an open steel LiPo safe, their leads to a four-channel
balance charger; motors sunk in pick-and-pluck foam; carbon arms and booms in 2x2 twill; props in a comb rack; the T4's
fan rotors on posts and its fan ducts; a lathe-turned 6 kg extinguisher (label round its front, valve, gauge, pin and
seal, hose and horn) on a plate across the end frame, the fire-point sign over it. Prints are laid out to fit: nothing
runs over anything else.

Two stockings (`stock`): a (frames, batteries, motors and hardware, props and rotors, cartons and cases, the
extinguisher) and b (booms, fan ducts, gimbal cases, big motors, long props, cartons). What is live: each smart pack's
four-LED gauge fills as it charges towards storage level (the next LED blinking), the station's slot lights are amber
while a pack charges and green when it is ready, the station's screen counts the ready packs and the minutes to the next,
and the charger's four channels fill their bars and show their percentages, starting over with a fresh pack when done.

One item places it whole (3 cells across, 3 high; master the middle cell at the bottom, `facing` its front); each cell
collides with the shelving's depth against its back edge. Recipe: six steel plates, two barrels and a storage rack.

Layout 16 (`TowerData.TRIPILLAR_PARTS_SHELVING_LAYOUT`) is this revision: unit a at (23, -35), unit b at (27, -35), both
facing south to the bay. Layout 15 is frozen as `tower_tripillar_v15.json.gz`; a saved layout-15 (or older) tower is
refitted on load (`TowerConversionGameTests.serviceArmTriPillarIsRefittedWithThePartsShelving`).

## The data hall (work list item 13, approved 22 October 2026)

Justin asked for the server racks to be redone and moved into a proper glass-walled data hall, and picked "Reuse the
plant's racks": the pieces of the nuclear plant's computer room (`tools/server_room.py`, `nuclear/ServerRoom`; the
Tri-Pillar branch now carries the plant's branch). Layout 17 takes out the twelve glass-door racks behind the operator
areas and puts the data hall in the hall's west corner. It fills the space from the hexagon's two west walls to x -38,
z -9..8 (`tools/tripillar_hall.py` `data_hall`):

- **The room**: a raised access floor (perforated tiles in the cold aisle) and its own ceiling at 4 blocks clear. Framed
  glass to the ceiling on its three inner sides, with steel posts only at the corners and the door's jambs and a steel
  band on top. A pair of reader glass doors with closers sits on the cold aisle's axis. The **Data Hall Sign** is on the
  band over them: an amber-lit light box reading DATA HALL, AUTHORISED PERSONNEL ONLY and CLEAN AGENT PROTECTED.
- **The racks**: two rows of eight of the plant's 42U racks (servers, network, SAN). Row A at z -2 fronts south and
  row B at z 1 fronts north, facing each other across the cold aisle. Their lights run with the traffic, they number
  themselves A-01.. and B-01.., and their doors open on a click. The cable ladder and fiber runway run over each row, the
  cross ladder runs over the west end, and its riser comes up at hot aisle A's end.
- **The containment** (`tools/aisle_containment.py`):
  - The **Aisle Containment Door** (`tower/AisleContainmentDoorBlock`, `AisleContainmentDoorPartBlock`,
    `AisleContainmentDoorBlockEntity`, `client/AisleContainmentDoorRenderer`) is at each end of the aisle: bi-parting
    automatic sliding doors with aluminium frames, tinted glass and brush seals. The header and track run over both end
    racks, so each leaf slides a block open in front of its rack. Its sensor under the header, each side, opens it for
    anyone walking up from either side; it closes 0.8 s after they have gone. The ready light is green, amber while open.
    The label (COLD AISLE, the aisle's tag, each row's tag over its rack) is drawn live, the rows read from the racks'
    own numbers. It is 2 x 2 cells, master bottom left from outside, `facing` its outside. Shut, it closes the doorway;
    open, only the header collides.
  - The **Aisle Containment Roof** panels lie over the aisle on the racks' tops: a rail on each rack's top front edge, a
    tee every metre with red fusible drop-away links, frosted panels, and the lit LED bar (light 12) down the aisle's
    middle.
- **Cooling and power**: CRAC-1 and CRAC-2 stand against the west walls at the north and south ends, fronts east, their
  screens live. The UPS lineup (two battery cabinets, the UPS module, the maintenance bypass, the PDU) is in the south
  zone. The UPS module is the room's hub, and the room is always fed (it only looks live: Redesign 9).
- **FM-200**: three cylinders with their manifold and riser in the north zone. The pipe runs under the ceiling with
  their nozzles, and there are smoke detectors in the room and over the aisle. At the door: inside, the releasing
  panel, the horn/strobe, the abort station, the vacate sign, an emergency light and an extinguisher; outside, the
  manual release, the clean-agent sign and the no-food sign.

Layout 17 (`TowerData.TRIPILLAR_DATA_HALL_LAYOUT`) is this revision. Layout 16 is frozen as
`tower_tripillar_v16.json.gz`, and a saved layout-16 (or older) tower is refitted on load
(`TowerConversionGameTests.partsShelvingTriPillarIsRefittedWithTheDataHall`). Recipes:
- the door: six iron ingots, two glass panes and redstone;
- the roof panels: three glass panes, two iron ingots and glowstone dust, for four;
- the sign: three steel plates, two glowstone dust and orange dye.

## The desks remade, each with its role's live pages (work list item 14, approved 22 October 2026)

Justin on the first mockup: "you have one chair, three keyboards, three mouses, and three monitors. Tilt the monitors
inwards a little bit towards the chair and get rid of the two keyboards on the side, and make the desk look like one big
desk." He approved the second sheet as shown, for every desk in the hall.

- **The Console Desk** (`tower/ConsoleDeskBlock`, models from `tools/ops_desk.py`): desks set down side by side, facing
  the same way, join into one long bench desk. `left` and `right` say whether a desk of the same facing stands on its
  operator's left or right; the joins follow whatever is beside it. The bench has one top with its rubber front edge, a
  modesty panel and cable tray down the back, end panels on feet only at the outer ends, and a T-leg under a middle
  desk. Each desk carries one monitor on a stand. At a row's ends the monitor is turned 22.5 degrees in toward the
  chair. There is one keyboard and one mouse, on the middle desk (a desk on its own has them too).
- **Roles** (`role`, 0 to 14): the row's job. Using a desk still opens the depot screen. Sneaking and using one with an
  empty hand turns its whole row to the next role, and a desk added to a row takes the row's role. The hall's rows:
  - the west operator rows (facing east): FLIGHT DIRECTOR, AIRSPACE, FLEET, DISPATCH, MISSIONS, TOWER BUILD;
  - the east rows (facing west): POWER, SUPPLY, DOCKS, MAINTENANCE, DATA HALL, COMMS;
  (both sets of rows until layout 44: the flight-ops trees stand there since layout 45, and these twelve roles are for
  desks players set);
  - reception: RECEPTION and SECURITY;
  - the map table's desk: MAP TABLE.
- **The pages** (`client/ConsoleDeskRenderer`, `DeskPages`, `DeskScreen`, `DeskData`): each role has three pages, on the
  left, middle and right monitors as the operator faces them (a desk on its own shows the middle one). The pages are
  drawn on the monitor's glass in screen units, 246 x 149, with flat colour shapes and the game's font. They are laid
  out as the approved sheet, and every figure on them is live:
  - from the tower terminal's view: fleet, flights (their state, bearing, distance and time home from their paths),
    hangar bays and their doors, roof pads, power, packager, pickups, jobs, materials and stock;
  - from the core: the tiers, the build and its store;
  - from the sky: time, weather and moon;
  - from the hall's own pieces, found once in the layout: the service arm's cycle, the parts wall's battery station, the
    data hall's UPS, racks, CRACs and FM-200, the lobby's speed gates and doors, and the map table's view.
  What the depot keeps no history of, the desks record while someone is near enough to see them: the last hour's
  launches and landings, the power draw every 10 s, open targets every minute, flights by tier, the event log, the
  security log and the build's rate for its ETA. Each record says when it started.
- **Drawing**: each shape goes on its own layer just off the glass, one above whatever it overlaps (`DeskScreen`'s layer
  grid), so no two share a plane, and the text goes over them all. The layers spread further apart for desks seen from
  further off. From a few metres off the smallest print is left out, and from further off all but the big lines. Nothing
  is drawn for anyone behind the monitor. A desk near the camera redraws every frame, the rest five times a second. A page that fails says so on its
  screen. The depot terminal's own monitor keeps its seven-line readout.

Layout 18 (`TowerData.TRIPILLAR_DESK_SCREENS_LAYOUT`) is this revision: every desk placed joined and with its row's role
(`tools/tower_tripillar.py` `DESK_ROLES`, `tools/tripillar_hall.py` `desk_state`). Layout 17 is frozen as
`tower_tripillar_v17.json.gz`, and a saved layout-17 (or older) tower is refitted on load
(`TowerConversionGameTests.dataHallTriPillarIsRefittedWithTheDeskRows`). Desks of the older towers join into benches too
(a placed desk looks at what is beside it a tick later).

## The Holo Projector (work list item 15, approved 23 October 2026)

Justin asked for a big hologram head of his character, angry, watching the players, with "You are being watched"
going round it. It took six mockup sheets. Along the way he asked for:

- a more angular face, not "a bubble head";
- a plain font;
- light that opens out from the base and fades "before it actually reaches the actual hologram";
- only one, "preferably near the entrance";
- a face that is not "way too cartoony": "model it off a real person's face";
- slimmer cheeks;
- a narrower, slightly pointed chin, tight under the jaw;
- better hair;
- a clean neck under the choker that fades "from visible to not visible".

He approved the sixth sheet as shown.

- **The block** (`tower/HoloProjectorBlock`, `HoloProjectorPartBlock`):
  - A 3 x 3 floor projector: a steel slab 0.20 high with a dark deck on it to 0.26, an amber edge strip, twelve
    emitters round a glass lens.
  - The master is the middle cell; the other eight are parts that know their offset (`ox`, `oz`). Every cell collides
    with the slab and deck.
  - It is placed whole, facing the player who places it, and only into a clear 3 x 3 space (otherwise it says so). The
    placement preview shows all nine cells.
  - Breaking any cell (in creative: see "The holograms") takes it all down.
  - No recipe: a hologram, the tower's own.
- **The head** (`tools/holo_head.py`, `tools/holo_head_hair.py`):
  - Made from the CC0 MakeHuman base mesh (provenance in `tools/data/README.md`), its head and neck shaped to his
    character: feminine, angry, slimmer cheeks, a narrower, slightly pointed chin, tight under the jaw, a finer nose.
  - Purple hair as textured cards (bangs to the eyes, side locks, a messy bun), brow cards, lash ribbons and a choker
    with a ring.
  - The neck is cut level under the choker; a short straight skirt below it fades out.
  - It stands 3.4 blocks from chin to crown, its turning point 5.2 blocks over the floor.
- **Watching** (`client/HoloProjectorRenderer`):
  - The head turns to the nearest player within 20 blocks, smoothly, at most 2.4 radians a second, pitching up to
    0.9 radians either way. The eyes look at that player too: the iris and pupil are drawn on the eyeballs round the
    gaze, at most 40 degrees off.
  - With nobody near, it faces where the projector faces (north in the Tri-Pillar, to the entrance), a little down.
  - The head stays on its spot.
- **The ring**: "You are being watched · " twice round, in DejaVu Sans Bold solid letters (`tools/holo_head_letters.py`).
  - It is 3.4 blocks out, 4.25 blocks up and leans back 24 degrees to face the people below.
  - It turns once every 24 seconds; letters seen from behind are fainter.
- **The light**: a streaked cone opening out from the lens, twelve rays from the emitters and 46 motes rising in it. All
  of it fades out well under the head, by 62% of the way up to the neck.
- **Drawing** (`client/HoloHeadMesh`, `HoloProjectorGeometry`): the whole hologram is one see-through, sorted batch
  (`entityTranslucentEmissive`).
  - It is drawn as the approved sheet:
    - brighter at its edges;
    - lit softly from a light fixed to the head;
    - scan lines and a slow band running up it.
  - Without a depth pass, a see-through head would show its far side through its near one. So
    `tools/holo_projector.py` works out, from 162 directions, which triangles, eye triangles and card quads are the
    nearest surface. The renderer draws only those seen from a direction within 15 degrees of the camera's (the
    mesh's `seen` bits, looked up through a cube map).
  - Every corner gets its colour from the camera. The quads, some 35,000, are rebuilt 30 times a second, and the
    buffer is drawn every frame in between.

Layout 19 (`TowerData.TRIPILLAR_HOLO_HEAD_LAYOUT`) is this revision. It removes the six holo emitters (and their
floating panels) and places the projector at (0, 1, -33), facing north (`tools/tripillar_hall.py` `holo_projector`).
Layout 18 is frozen as `tower_tripillar_v18.json.gz`, and a saved layout-18 (or older) tower is refitted on load
(`TowerConversionGameTests.deskScreensTriPillarIsRefittedWithTheHoloProjector`). The Holo Emitter block itself stays in
the mod.

## The Maintenance Door (work list item 10, approved 23 October 2026)

From Justin's walk-through: the maintenance doorways need "a solid (not glass) door". He approved the first sheet as
shown, for every pillar doorway.

- **The door** (`tower/MaintenanceDoorBlock`, models from `tools/maintenance_door.py`): a plant door
  (`nuclear/PlantDoorBlock`, the plant doors' kit in `tools/plant_doors.py`).
  - It has the pressed steel frame and stops, hinges, a parallel-arm closer, and kick plates on both faces.
  - The leaves are solid flush steel with no glass: steel grey in a gunmetal frame, over a threshold saddle.
  - In a pair, the left leaf as you come up to it is the active one. It has a lever with a key cylinder outside, a
    lever inside, the astragal over the meeting edge, and a NOTICE plate at eye height ("MAINTENANCE / AUTHORISED
    PERSONNEL ONLY"). The other leaf is flush.
- **The badge reader**, on the wall right of the pair: from outside it lets in the tower's owner and their party. The
  core gives every maintenance door it builds its owner. Anyone can leave.
- **The pair**: the leaves swing into the pillar. Using either leaf opens both, and the reader is asked once. The
  closers shut them again, but wait while anyone stands in either doorway.
- **Hood**: the doors out on the roof and the tie decks have a steel rain hood with a warm LED downlight. The upper
  half of a hooded door gives light 9.
- **Swinging**: `client/PlantDoorRenderer` swings the leaves as the steel door's, using part models 32 to 35 of
  `plant_door_part` (`nuclear/PlantDoors.leafPart`; tools/plant_doors.py `PART_COUNT` is 36).
- **The item**: crafted from steel plates and a basic circuit (3 doors). The creative tab also has it with the
  reader, with the hood, and with both.

Layout 20 (`TowerData.TRIPILLAR_MAINTENANCE_DOOR_LAYOUT`) is this revision. It puts a pair in all 27 pillar doorways
(`tools/tower_tripillar.py` `maintenance_door`):
- the three riser rooms off the hall, in the pillar feet (no hood);
- the tier-2 pillars' doors off the roof;
- each tie deck's three doors, which face the lift core.

`TriPillarArena.MAINTENANCE_DOORS` lists them. Layout 19 is frozen as `tower_tripillar_v19.json.gz`, and a saved
layout-19 (or older) tower is refitted on load
(`TowerConversionGameTests.holoHeadTriPillarIsRefittedWithTheMaintenanceDoors`).

## The neon light lines (work list item 11, approved 23 October 2026)

From Justin's walk-through: "end rods must go" and "the amber light boxes look bad in numbers". On the first mockup
(one plain amber line) he asked for "more of a neon look", for the blocks to "blend in with the surrounding blocks'
colour schemes, even if you have to create multiple types", and for "some neon white thin ones that aren't end rods"
(he liked the rods' thin white glow on the dark blocks). He approved the second sheet as shown.

The blocks (the layout's conversion in `tools/neon_lines.py`; since layout 28 real 3D neon, models and textures in
`tools/neon_3d.py`):
- **Neon Line** (`neon_line_concrete`, `_steel`, `_carbon`; `NeonLineBlock`):
  - The wall's or slab edge's own block: the same texture at the same phase (`TowerPanelBlock.PHASE`) and the same
    shading as its neighbours, nothing painted on it.
  - Round each side open to the air, a groove 2.4 px deep and 3 px tall, lined in satin aluminium, with a round glass
    neon tube (2 px) on clips in it: amber, its middle white-hot, glowing (light 12).
  - Each side is `open`, `line` (another line beside it: the tube runs on) or `wall` (a plain opaque block: the tube
    ends in a cap). Round a convex corner the tube bends; where another line touches it only at a corner (a chamfer one
    block thick) a 45 degree piece takes it on to that line (`ne`, `se`, `sw`, `nw`). It works these out from its
    neighbours, and lets them draw their faces against it, so a groove ending at a plain block is closed by its face.
  - Its blockstate is a multipart: the material by its open sides and phase, the fitting (lining, tube, clips) by its
    sides and joins, shared by the three materials.
- **Neon Floor Line** (`neon_floor_concrete`, `_steel`, `_epoxy`, `_dock`; `NeonFloorBlock`): the floor's own block with
  the same profile as a channel in its top, joined to the floor lines beside it (`north`, `east`, `south`, `west`):
  straight, round a corner (the tube bends), a tee, a cross; closed at a run's end; on its own a short dash.
- **Inset Floor Light** (`inset_light_concrete`, `_steel`, `_epoxy`, `_dock`; `InsetFloorLightBlock`, layout 28): a round
  frosted lens in a brushed steel square trim plate flush in the floor, glowing (light 12): wherever the layout had a
  floor line on its own (the apron's, the decks' and the roof's single lights).
- **Neon Tube** (`NeonTubeBlock`): a thin neon white tube with a soft halo, on a dark clip rail in the corner of its cell.
  Facing is away from the wall; side is the corner it stands in. Light 14, as the end rods'.

Layout 21 (`TowerData.TRIPILLAR_NEON_LAYOUT`) is this revision. `tools/tower_tripillar.py` converts each step's lights
after it places them (`neon_lines.convert`):
- The 1,017 amber boxes become lines or floor lines.
  - A box with open air over it, or in a floor at or under the hall's, becomes a floor line in its floor's material,
    along its run.
  - Any other box becomes a line in the material round it (steel before carbon before concrete). The atrium's slab
    edge, the roof line and the decks' edges are steel; the pillar feet's band and the edge over the rotunda cut are
    concrete.
- The 2,366 end rods become tubes, in the corner toward where their wall ends (the chamfer).
- The light is the same, so `tools/tripillar_lights.py` adds no lights and takes none away.

The status ticker still runs on the atrium's edge: `HoloMapRenderer.edgeBuilt` takes a neon line for the edge, as it
took an amber box.

**Redesign 2: real 3D neon** (Justin's walk through, 24 October 2026: the strips' material showed lighter than the floor
and walls round them, the display case's strips didn't join, the lone strip outside should be "a solid light", and "make
the lights be 3D modeled ... neon light"; approved with the round inset light). The painted slots and their glow on the
material are gone: the lines are the 3D grooves and channels above, the display case's ring joins at its corners, and
each single floor line is an Inset Floor Light. `tools/neon_lines.shape` writes every line's sides and joins and every
floor line's joins as the blocks work them out in game (bootcheck's LadderCheck compares them at every step: all 7,388
agree). Layout 28 (`TowerData.TRIPILLAR_NEON3D_LAYOUT`) is this revision; layout 27 is frozen as
`tower_tripillar_v27.json.gz`. Tests: `TowerGameTests.neonLinesShapeThemselves` and `assertNeonLines` (every line and
floor line in the built tower as the blocks round it have it; the inset lights). Layout 20 is frozen as `tower_tripillar_v20.json.gz`, and a saved layout-20 (or older) tower is
refitted on load (`TowerConversionGameTests.maintenanceDoorTriPillarIsRefittedWithTheNeonLines`). The Amber Light Strip
stays in the mod for building with.

## The joined girders and the Cradle Struts (work list item 12, approved 23 October 2026)

From Justin's walk-through: "the cradle's steel girders look tacky stacked: girders should join into one solid-looking
beam". On the first mockup he said the strut didn't sit right against what held it ("it looks sloppy") and asked for
bracing; on the second, that its head should land on a cap plate under the cradle. He approved the third sheet as
shown.

- **Steel Girder, joined** (`TowerGirderBlock`, models and textures from `tools/joined_girders.py`):
  - Every girder is the same wide-flange section: 14 px deep, 11.2 px across the flanges, with root fillets. Its paint
    texture tiles along the member, so a run of girders on one axis reads as one beam with no seams.
  - `join_lo` / `join_hi` say the run carries on past that end (the next block along the axis is a girder on the same
    axis). The block works them out from its neighbours.
  - Where a run ends it ends on a plate: a column on a full-width base plate with four anchor bolts between the
    flanges, under a cap plate; a beam on bolted end plates. A beam stands its web upright.
  - Collision follows the section and plates. The girder no longer counts as a floor or a light-blocker in
    `tools/tripillar_lights.py`.
- **Cradle Struts** (`CradleStrutsBlock` + `CradleStrutsPartBlock`, mesh and data from `tools/cradle_struts.py`, drawn by
  `client/CradleStrutsRenderer`): the pair under each supply cradle.
  - Each strut is one member from the pillar's chamfer up under the cradle, its ends cut in the planes they land on.
  - The foot sits flush on an end plate, bolted to a bracket plate on the chamfer, with a seat angle under it.
  - The head lands a block in under the cradle (off its hazard edge), cut flat on an end plate and a bearing plate
    bolted up into the deck.
  - The bracing: gusset plates welded to the webs at a fifth, half and four fifths of the length, with flat-bar braces
    bolted onto them (a tie at mid-length and an X in each bay).
  - The master is the left strut's foot cell (seen from the pillar). The 57 other cells the struts pass through are
    parts that know their index in the footprint (`assets/jugcraft/tower_mesh/cradle_struts.json`, read on both sides)
    and collide with what of the struts is in them (1/8-block voxels). `facing` turns the canonical pair (south: the
    diagonal -x +z) a quarter at a time.
  - One item places the pair whole and breaking any cell takes it all; recipe: 4 steel girders, 4 steel plates and iron
    bars. The mesh has ambient occlusion traced against its own triangles, with the chamfer and the deck as
    occluders.

Layout 22 (`TowerData.TRIPILLAR_CRADLE_STRUTS_LAYOUT`) is this revision:
- each of the four cradles' two staircases of girder blocks (14 blocks) becomes one pair of struts;
- the entrance canopy's posts and the crown's mast become joined columns, with base and cap plates;
- `tools/neon_lines.py` no longer takes a girder or a strut for the wall a neon tube stands against. Four tubes beside the
  old staircases had turned to face the girders; they face their pillar again.

Layout 21 is frozen as `tower_tripillar_v21.json.gz`, and a saved layout-21 (or older) tower is refitted on load
(`TowerConversionGameTests.neonTriPillarIsRefittedWithTheJoinedGirders`). Girders already standing outside a tower show
their plates until a neighbour changes; a girder placed by hand joins as it goes.

## The map room and the Big Map Table (work list item 16, approved 23 October 2026)

From the work list: "Big map table for the topographic hologram, in its own map room upstairs". On the first take Justin
asked for it "bigger and make it show actual minecraft terrain from the world its in", the edge fading "from visible to
not visible", trees and the topography, and progress as things change (no ores). On the second, that it show everything
at its true height ("if there's a floating island, like, it'll show it floating"), and that the map zoom out as the
drones go out. He approved the third take, asking that "to keep any lag from happening" it refresh only the area it maps
and only every 15 seconds. The small 3x3 map table downstairs stays.

- **The room** (`tools/tower_tripillar.py` `map_room`, part of the foundation): the upper floor's north side, inside x
  -10..12, z -44..-23, walled off with carbon panels on the west and east. Its front on the gallery (z -22) is framed
  glass with glass double doors in the middle (hinges outer). A solid band (y 17..18) runs over the glass, and the MAP
  ROOM sign (a lit box like the data hall's, `map_room_sign`) hangs on it over the doors, never on the glass. The four
  ceiling panels over the map are taken out.
- **The Big Map Table** (`BigMapTableBlock` + `BigMapTablePartBlock`, mesh and data from `tools/big_map_table.py`, drawn
  by `client/BigMapTableRenderer`): a 13 x 13 octagon 0.86 high.
  - A dark plinth set in under the top, with a lit toe-kick.
  - A composite top with a brushed steel edge band and a stainless lean rail on brackets.
  - A glass well 12 across with the projector's emitter grid under the glass, behind a steel lip.
  - A tilted touch panel in each chamfered corner. Each panel's page is live: SITE / LAND / RADIUS (the view the drones
    call for), how far the map reaches, the drones out, the clock, and which layers are on (TERRAIN, ROUTES, TOWER,
    LIVE).
  - The master is the middle cell. The 164 other cells under the octagon are parts that know their index in
    `assets/jugcraft/tower_mesh/big_map_table.json` and collide with the plinth and top (1/8-block voxels).
  - One item places the table whole and breaking any cell takes it all (in creative: see "The holograms"). No recipe: a
    hologram, the tower's own. The tower pins it to its terminal; standing alone it shows the nearest terminal
    within 96 blocks. Using it opens the terminal's screen.
- **The map** (`tower/MapScan` on the server, `client/BigMap` on the client): the world round the tower in miniature,
  full bright over the well.
  - **One scale across and up.** The view follows the farthest drone: the site (72 blocks), the land (420) or the
    build radius. It widens until the tallest thing in view stands under 5 blocks over the map, so the finished tower
    takes about 300 blocks round it.
  - **What a scan reads.** Each sample (about 72 from the middle to the edge, at most 80) is the tallest column in its
    square, read from the top down. Every run of blocks over open air is a span at its true height: a floating island,
    the tower's decks and cradles, a canopy beside its trunk. The first run 16 blocks thick is the ground. Nothing under
    the ground is read, so no ores or caves show. Air shut in under a run (no sky light: a cave, a room) up to 12 tall
    counts as solid.
  - **Colours.** Tops take the block's map colour, sides darker, undersides darker still. Water is a softer blue, see
    through; glass is pale blue, see through. The outer fifth of the map fades out to nothing.
  - **Land not loaded.** Where a chunk isn't loaded, the map uses the world generator's surface on a coarser lattice,
    interpolated, coloured by its biome. No chunk is ever loaded or generated for it, and those points are kept.
  - **Against lag.** A scan covers only the round area in view. It starts at most every 15 seconds, only while a
    player is within 64 blocks, and is spread over the ticks at 0.5 ms a tick. A finished map that differs from the
    last goes to the players near (about 100 KB for the finished tower). The client turns it into quads off the render
    thread. Each frame draws only the faces turned to the camera, and a coarse copy from 11 blocks off.
  - **Overlays:** the drones out with their drop lines, dashed routes home and stops ahead, the rim and its ticks, and
    the compass.

Layout 23 (`TowerData.TRIPILLAR_MAP_ROOM_LAYOUT`) is this revision: the room, its sign and the table (652 blocks
changed in the foundation, 4 ceiling panels out). Layout 22 is frozen as `tower_tripillar_v22.json.gz`, and a saved
layout-22 (or older) tower is refitted on load (`TowerConversionGameTests.strutsTriPillarIsRefittedWithTheMapRoom`).

## The ground floor's rooms (work list item 22 part A, approved 23 October 2026)

From the work list: the floor plans' empty ground-floor rooms fitted out ("crew cafe south, battery + UPS south-east,
lockers + showers south-west, security office west of pillar C, restrooms north-west"). Justin approved the first take as
shown. The upper floor's rooms are part B, with their own sheet.

Each room is a box inside the hall like the data hall (`tools/tripillar_rooms.py`, called from `control_building`):
painted walls y 1..4 (since layout 49 faced in concrete where they look onto the hall, with the pillars' light line: the
first-floor look-over, below), framed glass where it looks onto the hall, a steel band and its own ceiling tiles at y 5 with
ceiling panels under them (the hall's lights over a room come out). Over every door the wall runs on to the band (glass
over a glass door). Doors in pairs open and shut together (`paired`): glass for the cafe and the security office, steel
for the rest. Each has the plant's room plaque beside it, and over a glass door on the band, never on the glass. Where a
room backs onto the curtain wall, the restroom's glass there is the dark skin of the upper storey (no windows into it).
Every fitting is the nuclear plant's interior kit (the same blocks and the same rooms Justin approved there) except one
new piece, the shower stall.

- **Crew cafe** (south, x -23..23, z 31..44): the glass front on the hall with two pairs of doors, the kitchen on the
  west wall (the upper lounge's kitchen kit: the Kitchen Run and the Fridge Freezer, since layout 32), 24 tables for two, a lounge
  corner on the east wall (the three-seat sofa facing the **Lounge TV**, the **Long Glass Coffee Table** between them
  with a block to walk through; since layout 31), the water cooler, plants. It keeps its windows.
- **Security office** (east of pillar B in the hall's south-east corner by the curtain wall, x 31..38, z 11..18, since
  layout 40; south of dispatch, x 29..36, z -4..3, in layouts 38 and 39; till then west of pillar C, in front of the
  data hall's glass): glass onto the hall with its doors on the west, its other walls solid.
  The secondary alarm station's console (`sec_alarm_console`, live) under the live screen on the north wall, three
  console desks working SECURITY (live), the weapons locker, the report desk
  for two, the supervisor's desk by the doors, files, the officers' lockers, a kitchenette under the clock and the
  radios, the fridge, the AED; carpet.
- **Restroom and washroom** (north-west, off the lobby): the plant's restroom as approved (two stalls, one accessible,
  the urinal and its screen, two sinks, the dryer, the paper towels, the baby changing station), turned to the lobby;
  and an accessible washroom (toilet with grab bars, sink, mirror, towels, dryer, a baby changer) north of it. Both
  tiled.
- **Locker room** (south-west, x -40..-25, z 22..44): full-height lockers along the north and east walls, the open ones
  crew lockers (since layout 30), and benches. The three shower stalls stand on the east wall, with the sinks, mirrors,
  dryer and towels south of them. The floor is tiled from the showers south and vinyl elsewhere.
- **Battery and UPS room** (south-east, x 25..40, z 22..44; its doors in its west wall, on the hall, since layout 49:
  in its north wall till then): the UPS lineup along the north wall (batteries, module,
  bypass, PDUs), two strings of station batteries, their disconnects, the hydrogen monitor and exhaust fans, the eyewash
  and its sign, the spill kit, the PPE cabinet, the cell log, the sump, the acid and NO SMOKING signs.
- **The shower stall** (new; `shower_stall`, `nuclear/RestroomBlocks.Shower`, models from `tools/shower_stall.py`): one
  block whose panels run up into the block above (to 30 px), as the plant's toilet partitions do. A tiled back, the
  restroom's phenolic partitions down both sides, a tiled tray with its drain, the chrome head on its arm, the mixer
  valve and lever, a soap shelf, a curtain on its rail drawn back. You walk in between the partitions. Right-click turns
  it on (the lever swings up, water falls from the head and churns on the tray, the spray's sound, `restroom.shower`),
  again to turn it off; it shuts itself off after a minute. Standing under it puts you out if you are on fire. Recipe:
  steel plates, white wool, an iron ingot and restroom floor tile.

The foundation now uses many of the plant's pieces, and some of their recipes had the same shape as another item's (only
one of each could ever be crafted, so the foundation could not be paid). Each one the tower uses now has a recipe of its
own: the hydrogen monitor (a glass bottle), the toilet partition (white dye), its front panel (iron nuggets), the alarm
console (a note block), the locker bench (iron ingots), the filing cabinet (a chest), the locker (iron bars), the
eyewash, NO SMOKING, agent discharge, clean agent and no food signs, the agent release panel (a lever) and the power
cable tray (iron nuggets).

Layout 24 (`TowerData.TRIPILLAR_GROUND_ROOMS_LAYOUT`) is this revision: the rooms (3,761 blocks changed in the
foundation, 42 hall ceiling panels out). Layout 23 is frozen as `tower_tripillar_v23.json.gz`, and a saved layout-23 (or
older) tower is refitted on load (`TowerConversionGameTests.mapRoomTriPillarIsRefittedWithTheGroundFloorRooms`).

## The upper floor's rooms (work list item 22 part B, approved 23-24 October 2026)

From the floor plans: the upper floor's twelve empty rooms fitted out. Each room had its own mockup sheet (in-game
renders, real-world scale: 1 pixel is 6.25 cm) and was approved one at a time, from the briefing room at its fourth
take to the restrooms at their first ("Next: build it all"). Layout 25 builds them all.

The rooms (`tools/tripillar_rooms_upper.py`, called from `control_building` after the ground floor's; the same `Rooms`
shell as part A, counted from the upper floor's slab):

- **Briefing room** (north-east, x 12..27): carpeted tiers (`tier_riser`: where a riser steps down, an amber step light
  set into a channel in its face under a flush aluminium nosing, running unbroken from riser to riser; `aisle_step` half
  steps in the aisles, lit the same) with rows of training desks and chairs, the lectern,
  the AV credenza, the large wall screen and two smaller ones, ceiling speakers, the clocks; its lit BRIEFING sign over
  the glass doors on the band (`briefing_sign`).
- **Manager's office**: the executive desk and chair, credenza, bookcase, a leather sofa and armchairs round a coffee
  table, a bistro table, the coat stand, a drone on its walnut plinth, framed certificates, the floor lamp.
- **Engineering lab**: butcher-block workbenches, the CAD desks, the electronics bench, pegboards, the tool cabinets,
  the LiPo cabinet, parts racks, the plan chest, the 3D printer farm, the plotter, thrust test stands and the netted
  flight test cage (its doors, the foam floor and the pad with a drone parked on it).
- **First aid and crew rest**: the exam couch, the crash cart, the dressing trolley, the medicine cupboard and fridge,
  the scrub sink, the eyewash, the column scale, the oxygen trolley, the IV pole, the spine board, the wheelchair; next
  door the bunks, recliners, a sofa round a coffee table, the wall TV, a kitchenette, the fridge, the lockers.
- **Flight planning** (until layout 33): the dispatch counter, the chart table, the area chart, the NOTAM and flight
  strip boards, the weather kiosk, the zone clocks, the copier, the filing cabinets and binders. Taken out since: its
  space is open floor from the gallery through to the crew lounge's front (the planning kit's pieces stay craftable).
- **Offices** (south-east and south-west): benching desks and single desks, the phone booth, the team table, storage,
  the noticeboard and year planner, the mail sorter and the recycling station.
- **Crew lounge**: the kitchen run and its island, the fridges, the vending machine, dining tables and chairs, bar
  stools, the foosball table, the media console under the **Lounge TV** (since layout 31), a rug.
- **Simulators**: six Pilot Simulators (2 x 2 x 2: the seat reclined 22.5 degrees, three screens folded in round the
  pilot), each flying its own scenario (`sim` 0..5: the far row 1-3 from the west, the near row 4-6), the instructor's
  station, the headset rack, acoustic panels, the waiting bench.
- **Training room**: training tables, the instructor's desk, the class whiteboard and the touch display cart, study
  carrels, the build table, controller racks, the parts board, bag cubbies, posters; its large wall screen shows the
  lesson (the WIND ROUND BUILDINGS slide).
- **Meeting room**: the boardroom table (2 x 5) with its laptops, pendant lights over it, the logo wall, the
  refreshment credenza, the video call camera over the wall screen.
- **The custodian's closet** (north-west): the mop sink, mop rack, chemical dispenser, janitor cart, wet floor sign,
  supply shelves, water heater, floor scrubber on charge; the bottle filler on the gallery against its south wall. (The
  men's and women's rooms in front of it went in layout 38.)

Every plaque has its own label (`PlantSignBlock.Label`: men, women, custodian, manager, engineering lab, first aid, crew
rest, simulators, crew lounge, offices, training room, meeting room), on the wall beside a steel door and on the band
over glass ones, never on the glass. Over a room with its own lower ceiling, its walls run on up to the hall's ceiling;
the hall's ceiling lights stay in that closed space over it, so it is lit and nothing spawns there. The hall's small
3x3 map table, its desk row and chair are taken out (Justin: "Trash it"): the Big Map Table upstairs maps the land.

**The room kits as blocks** (`tower/RoomKit`, `RoomKitBlock`, `RoomKitDisplayBlock`): the 133 pieces are made by the
kits' generators (`tools/training_kit.py`, `manager_kit`, `lab_kit`, `rest_kit`, `planning_kit`, `office_kit`,
`lounge_kit`, `sim_kit`, `class_kit`, `meeting_kit`, `wash_kit`, `room_extra_kit`), and `tools/room_kit.py` runs them all
and writes the manifest `jugcraft/room_kit.json` the game registers them from: each piece's properties (facing, part,
half, variant, colour, its own), the cells one item places and every state's hitbox (from its models, merged into at
most 20 boxes). So:

- one item places the whole piece facing the player, its variant or colour chosen by where it goes; the placement
  preview shows its cells (red where something is in the way, and then it is not placed);
- any cell broken takes the whole piece and drops the one item (an explosion or a command too);
- put in by its anchor alone (the tower's build, a command), the rest of it comes too;
- pieces in rows join up (left and right: desks, sofas, benches); a tier riser shows its nosing where it steps down;
- materials by what each is made of (wood, wool, stone, metal: sound, map colour, hardness); lamps and screens light;
- the item is the whole piece at its true size (scaled only by its displays), or the three blocks of it nearest its
  anchor for the longest pieces;
- each has a recipe of its own shape (`tools/room_kit_recipes.py`: no other recipe in the game matches it, mirrored or
  not; checked against vanilla's and the mod's), loot, name and the pickaxe tag; the tower counts a piece once, at its
  anchor (`TowerCosts`, `tools/tower_costs.py`).

The wall screens (`ops_wall_screen` 3 x 2, `ops_wall_screen_large` 5 x 3) are drawn live (`client/RoomKitDisplayRenderer`,
with `DeskPages` and `DeskScreen`): a role of the desks' fifteen (their three pages in turn, 8 seconds each) or the
lesson; sneak-click one to change it. The tower pins them to its terminal like the desks.

Every other display on the pieces is live too (part B2). The kits mark the parts of their pictures that change
(`tools/training_kit.py` `live`, `live_text`, `live_part`); `tools/room_kit.py` finds each one's corners on the model's
faces for every state and writes them to the manifest (`live`), and each cell that shows one holds a block entity
(`RoomKitBlockEntity`, pinned to the tower's terminal) that `client/RoomKitDisplayRenderer` draws it over, a hair off
the model's face, with `client/LiveFaces`:

- laptops and monitors: their page of the desks' (`DeskPages`, the tower's data), close to (a few blocks; further off
  the baked picture of the same page shows);
- clocks: the seven-segment wall and zone clocks (the tower's time of day, UTC an hour behind, the colon blinking each
  second), the ovens', microwaves', the dispatch board's and the weather kiosk's; the desk phones show the day and time;
- timers: the calls in progress (the meeting room's and the office's phones), the presenter's lesson clock, the theory
  exam's time left;
- readouts: the power supply's volts and amps, the multimeters, the soldering stations, the LiPo chargers (filling
  over 40 minutes), the thrust stand, the medicine fridge, the blood pressure monitor's pulse, the ice maker, the
  training transmitters' batteries (running down through the lessons);
- the oscilloscope's two traces, the flight controller's motor test bars, the 3D printers' and the plotter's progress;
- the simulators: each pod's and its repeater's speed, height, battery and wind (the same figures on both; their views
  are drawn without them, `tools/data/sim_world`);
- the bottle filler's counter (it fills a glass bottle with water, and counts it) and the floor scrubber's battery
  gauge (charging each morning after the night's cleaning).

Changed fittings: the toilet takes the stall partition along its left side (`partition`, for two stalls side by side:
use a toilet partition on a toilet; it drops again with it); the wall tile has a carbon back (`back=carbon`, the tower's
carbon composite panel) for a tiled wall on the tower's carbon walls. The eye protection sign (the lab's) gets a recipe
of its own shape (a glass pane in it; it had ten other signs' shape).

Layout 25 (`TowerData.TRIPILLAR_UPPER_ROOMS_LAYOUT`) is this revision: 9,355 blocks of the foundation changed (the
upper floor) and the hall's map table out; the tiers are as before. Layout 24 is frozen as
`tower_tripillar_v24.json.gz`, and a saved layout-24 (or older) tower is refitted on load
(`TowerConversionGameTests.groundRoomsTriPillarIsRefittedWithTheUpperFloorRooms`).

## Fixes from Justin's walk through (24 October 2026)

Justin walked the built tower (r106) and listed what was wrong; the first batch of fixes:

- **Furniture collides as it is drawn.** For a while (24 to 27 October 2026) the sofas, tables and the rest stood
  taller to a player than drawn, so nobody walked up onto them; Justin had it taken out again on 27 October, so the
  game's own step-up applies to them as to any block. The engineering lab's service cradle stand collides as its
  platform (`HallFixtureBlock.Stand`), so nobody runs through it.
- **Screens drawn whole from every side.** The desks' and the room kits' live screens used translucent render types,
  which this version sorts per layer, so overlapping quads showed only parts of a picture from some angles; they draw
  as cutout now, lit full bright (`DeskScreen.SCREEN`, `DeskScreen.litNormal`).
- **Ladders.** Every service ladder (the riser rooms, the pillars, the lift core) is the plant's Caged Steel Ladder:
  its cage starts two lengths over each landing a climber steps off onto and has a flared foot (`bottom`), with
  handholds at the top (`SteelLadderBlock.shaped`; `tools/tower_tripillar.caged` writes the same states). The plant's
  tank ladders and the tank kits get the flared foot too.
- **Rooms.** The ground floor's lounge sofas sit a block's walk from the long coffee table; the restrooms' baby changing
  stations and the locker room's floor drain are gone. Upstairs, the lab's, lounge's and meeting room's glass walls
  stand in line with their doors; the briefing room's double doors hinge at their outer edges, so the handles meet in
  the middle; the lounge's bookcases stand with their backs to the wall; the recliners face the glass between the
  window's mullions; the manager's second desk (the credenza behind the chair) is gone and the waste basket stands by
  the desk with its paper inside it; the media console's board games stand clear of its speaker.

Layout 26 (`TowerData.TRIPILLAR_REVIEW_LAYOUT`) is this revision. Layout 25 is frozen as `tower_tripillar_v25.json.gz`,
and a saved layout-25 (or older) tower is refitted on load. Tests: `RoomKitGameTests.playersDontStepOntoFurniture`,
`StorageTankGameTests.ladderCageStartsOverEachLanding`.

**Walls up to the ceiling** (redesign 1, Justin chose option B of the mockup). Every room's walls on both floors, and the
data hall's, go on up from the steel band to the hall's ceiling in the dark carbon panel of the building's skin
(`tools/tripillar_rooms.py` `Rooms.bulkhead`, `tools/tripillar_hall.py` `data_hall`): 1,854 blocks, over 4 blocks on the
ground floor and 2 upstairs. The space over each room's own ceiling is closed, and lit inside (`Rooms.plenum_lights`:
the hall's lights there stay, and another goes in over any corner none of them reaches), so nothing spawns in it. The
lights pass (`tools/tripillar_lights.py`) now lights the rest of a step when one patch can't be lit, and says which.
Layout 27 (`TowerData.TRIPILLAR_WALLS_LAYOUT`) is this revision; layout 26 is frozen as `tower_tripillar_v26.json.gz`
(`TowerGameTests.assertWallsToCeiling`).

**Redesign 3: the tower diorama** (Justin's walk through: the small 3x3 table by the desk "does nothing"; he chose, over
a live hologram of the land, "a diorama of the tower and generic Minecraft surroundings, something still that doesn't
move and reflects true colors", 10 chunks round the tower, the square plot, always the finished tower). The table, its
desk row and chair are back at their old spot north-west of the pillar feet (`tools/tripillar_hall.py` `map_table`); the
tower pins the table to its terminal like its other displays, and `HoloMapRenderer` draws `TowerDiorama` on it: the
finished tower from this tree's data in 2-block voxels, each the colour its blocks' models show (every face by its size,
glass over a pale sky), in a 2.75-block square plot of made-up land 320 blocks across (plains, a river with sandy banks
through a gorge, a lake, oak, birch and spruce woods, a mountain with a snowy top, a gravel path from the south door),
cut at its sides so the soil shows (grass, dirt, stone, deepslate; no ores), on a thin dark plate. One scale across and
up: a block on the table is 116 in the world; the tower stands about 2.5 blocks over the table. Nothing moves and nothing
is scanned; a frame draws only the faces turned to the camera (about half of its 25,343). `tools/tower_diorama.py` writes
its mesh and texture, and `tools/tower_tripillar.py` runs it after writing the tower's data, so the model follows the
design. The desks work TOWER BUILD (the MAP TABLE role is left for desks players set; its pages follow the Big Map
Table's views). The land grids the old topographic map drew from (`TowerTerrain`, `TopoMap`) are gone, and with them
their sampling on the core's ticks. Layout 29 (`TowerData.TRIPILLAR_DIORAMA_LAYOUT`) is this revision; layout 28 is
frozen as `tower_tripillar_v28.json.gz` (`TowerGameTests.assertModelTable`, `TowerRenderChecks.towerDiorama`).

**Redesign 4: real 3D lockers and filing cabinet** (Justin: the security room's pieces "as real 3D models; override the
model so the admin building updates too"; the locker room "no scientist uniforms ... crew lockers"; and of the coat rack
by the sinks, "get rid of the thing that has the orange vest and scientist coat beside that sink"). Every locker, in the
tower and in the plant, is now a pair of full-height steel lockers in 3D, and the filing cabinet (the small drawers by
the gun locker) is a three-drawer lateral cabinet in 3D. Both changed in the plant kit, so the plant's buildings change
too (see `docs/features/nuclear.md`). The locker room's open lockers are the new **Open Crew Locker**, holding the
crew's flight jacket, a headset and a kit bag; the coat rack is gone. Layout 30 (`TowerData.TRIPILLAR_LOCKERS_LAYOUT`)
is this revision, and layout 29 is frozen as `tower_tripillar_v29.json.gz` (`TowerGameTests.assertGroundFloorRooms`:
crew lockers, none with a lab coat, no coat rack).

**Redesign 5: flat screen TVs and a long glass table** (Justin's walk through, notes 16 and 20: the break area's small
table by the sofa to become a long glass table and its control screen a flat screen TV; the lounge's ops wall screen a
flat screen TV too). The **Lounge TV** (room kit, `tools/lounge_kit.py`, 3 x 1 on a wall) is a 75 inch
class flat screen: its picture 1.65 m by 93 cm in a thin dark frame, the panel 2.8 cm deep on a flat bracket, centred on
its middle block and hung with its bottom 1 m off the floor. It hangs over the break area's sofa and over the upper
lounge's media console. Its picture is **TOWER TV**, the tower's own channel, drawn live from the tower terminal
(`client/TowerTv`, the desks' page role 16, `tower/RoomKitDisplayBlock.TV`):
- the day and the clock;
- the weather now over the land and the tower (the sun, or the moon in its phase and the stars; rain, a storm's
  lightning; the beacon blinking at night);
- the drones in the air, with the three due soonest, what each is doing, its home and when it is due;
- the tier being built;
- a news ticker of the desks' event log and what the build is short of.
A TV placed away from any tower shows NO SIGNAL. It is drawn live as far as 12 of its widths; further off, its baked
picture (`tools/data/desk_pages/16_0.png`) shows. The **Long Glass Coffee Table** (plant kit, `tools/plant_tables.py`,
3 x 1) is 2.4 m by 60 cm and 42 cm high, made of the plant's glass tables' 19 mm glass on two black steel sled frames,
with the plant's mugs, magazines and the TV's remote on it. Layout 31 (`TowerData.TRIPILLAR_TV_LAYOUT`) is this
revision, and layout 30 is frozen as `tower_tripillar_v30.json.gz` (`TowerGameTests.assertGroundFloorRooms` and
`assertUpperFloorRooms`: both TVs whole and pinned, the table whole with room to walk, no control screen, small table
or ops screen left).

**Redesign 6: the downstairs kitchen from the upstairs kit** (Justin's walk through, note 18: the ground floor kitchen
made of the upstairs kitchen's cabinets and appliances). The crew cafe's servery (the plant's counters, coffee counters,
microwave counter, sink, stove and two fridges) is now the upper lounge's **Kitchen Run** (10 x 1, two high: cupboards
and drawers, the hob with the oven under it and its hood, the sink, the dishwasher, the coffee corner, the open shelves
of mugs, the microwave, the wall cupboards over a tiled splashback) along the same west wall, with its **Fridge
Freezer** at the south end by the window. The tower no longer uses the plant's kitchen stove (an agriculture block, so
it was missing from builds without that feature). Layout 32 (`TowerData.TRIPILLAR_KITCHEN_LAYOUT`) is this revision,
and layout 31 is frozen as `tower_tripillar_v31.json.gz` (`TowerGameTests.assertGroundFloorRooms`: the run and the
fridge freezer whole, none of the plant's kitchen pieces left).

**Redesign 7: flight planning taken out** (Justin, 25 October 2026, asked about the planning office's walls: "just get
rid of flight planning"). The upper floor's flight planning office is gone: its dispatch counter, weather kiosk, console
desks and big wall screen, boards, chart tables, files, copier and clocks, its carpet and its back wall. Its space is
open floor (the gallery's access floor) from the gallery through to the walkway and the crew lounge's glass front, under
the hall's ceiling and its lights. Layout 33 (`TowerData.TRIPILLAR_NO_PLANNING_LAYOUT`) is this revision, and layout 32
is frozen as `tower_tripillar_v32.json.gz` (`TowerGameTests.assertUpperFloorRooms`: none of its pieces or desks, nor
its back wall, left).

**Redesign 8: the briefing room's step lights** (Justin's walk through, note 27: the orange strips at the risers' foot
to run all the way across, glow, and be part of the risers so the chairs don't hit them). The **Tier Riser** and the
**Aisle Step** are remodelled (`tools/riser_models.py`, written by `tools/room_extra_kit.py`). Where a riser steps down,
its face has a 7.5 cm channel under the top edge, with the amber light set 2 cm back in it and lit by itself (full
bright). The aluminium nosing over it is flush with the face and stands a hair over the tread, so nothing stands out of
the block and a chair against it touches nothing. The light runs the block's whole width and on into the next riser's,
with no line at the joint, and it wraps round outside corners. There is one model for each of the 16 sets of edges,
built whole so that corners mitre. Only the models changed (the blocks and their states are the same), so built towers
show the new risers as they are. Where risers make an inside corner (none do in the tower), the two lights stop either
side of it: a riser knows only its four sides.

**Redesign 9: the cosmetic rooms only look live** (Justin's walk through, notes 5, 12 and 17; he answered: "I don't want
to put a lot of strain on the server, so I don't want it to be actually calculating anything. Just make it look like
it's doing something", and chose to have the same pieces behave the same everywhere; then, of the holograms: they are
part of the base and stay live). The plant computer room's pieces (the data hall's and the battery room's UPS lineups,
racks, CRACs, FM-200 system), the battery rooms' hydrogen monitor, exhaust fan, string disconnects and station
batteries, and the operations office's wall clock and radio rack no longer tick on the server; their numbers are drawn
by the players' games, and what they do happens when someone uses them or a fire is put down near them
(`docs/features/nuclear.md`, the plant computer room's pieces and the station batteries). The batteries still store and
give power through cables. The lobby's speed gates and the data hall's aisle doors still open for whoever walks up, but
look for anyone only while a player is within 8 blocks (the tower's chunks stay loaded with nobody there). Everything the
tower runs on stays live: the drones, the terminal, the docks and pickups, the exchanges, the energy, the holograms and
the map table. The office desk computers and the security office's alarm console stay as they were (Justin: neither).

**Work list item 21: the lift core's door centred** (the floor plans' work list; take 1 approved by
Justin, 25 October 2026). On every deck from tier 2's up the way into the glass lift core was a two-block gap at one
side of its five-block south face. It is now one **Glass Door** in the middle of the face (x 0, closing by itself),
with framed glass either side under the glass transom. The core's glass also stood on nothing at each deck: a
one-block slot ran round its foot, under the doorway too (on tier 2's deck you could see the lantern's glass roof a
block down through it). The glass now stands on a steel sill all round at every deck, so the threshold is solid. The
rest of item 21 was built before: the Maintenance Doors in every pillar doorway (layout 20). Layout 34
(`TowerData.TRIPILLAR_LIFT_DOOR_LAYOUT`) is this revision. Layout 33 is frozen as `tower_tripillar_v33.json.gz`, and
built towers are refitted to it on load. `TowerGameTests.liftCoreDoorIsCentredOnEveryDeck` checks the door, the glass
beside it and the solid ring on every deck.

**Work list item 21's last part: the pillar rooms fitted out** (take 1 approved by Justin, 26 October 2026). Every room
behind a pillar's maintenance doors, three off the roof and 21 off the tie decks, was an empty shaft with the backs of
that row's hangar bays. Each is now a drone service floor, laid out from the room's own bays by `tools/pillar_rooms.py`
(run last in `tower_tripillar.pillar_tier`), so every pillar and tier gets its own:
- a work station along the back of each bay on the row, its user facing the bay: the LiPo charging cabinet, workbenches
  under pegboards, a roll cab and, by the medium and large bays, parts racks (the engineering lab's pieces), longer the
  wider the bay. Tiers 8 and 9's pillar C have no bay on their bottom row, so their station runs along the wall across
  from the doors;
- a green pedestrian walkway with yellow edge lines (the warehouse's Walkway tiles) from the doors in, a strip in front
  of each station and the way to the ladder's foot, never over a station;
- a cable tray under the ceiling over each station, both ends capped;
- a fire extinguisher on the wall beside the doors, inside;
- on the small and medium rows (tiers 2 to 6) a drone of the row's tier on a service stand near the first station. A
  large drone on a stand would fill the room, so the large rows have none.

The rows above (up the ladder) and the riser rooms in the pillars' feet stay as they were. Each tier costs a little more
(the pieces it places). Layout 35 (`TowerData.TRIPILLAR_PILLAR_ROOMS_LAYOUT`) is this revision. Layout 34 is frozen as
`tower_tripillar_v34.json.gz`, and built towers are refitted to it on load. `TowerGameTests.pillarRoomsAreServiceFloors`
checks all 24 rooms.

**The roof plant and walkways** (take 1 approved by Justin, 26 October 2026; he chose all four kinds of plant, painted
walkways with step-overs, no screen round the plant, and the supply pickup and cargo lift house left for work list item
20). The control building's roof was bare concrete round the pads. It now carries the plant a building like it has,
put down with the roof pads (tier 1) by `tools/roof_plant_layout.py`, from fifteen new pieces (`tools/roof_plant.py`,
room kit pieces of the "roof" room: one item each, placed whole and broken as one):
- **Rooftop Air Handler** (AHU-R1, AHU-R2; 3 x 8 blocks, 3 high), north-west over the upper floor: a double-wall RAL 7035
  casing with panel joints and a standing-seam roof cap, on steel W-beam dunnage on concrete pier pads; four hinged
  access doors with cam latches (fan, coil, filters, mixing box), the fan door's warning label, the filters' gauge, a
  control panel (RUN lit) with its isolator and conduit to a pitch pocket, the coil's insulated pipes into a pipe
  portal, the condensate trap to a splash block, the outside-air hood with its bird screen and louvre. The two face
  each other across a two-block service aisle; each one's supply duct (off its discharge end) and return duct (into
  its mixing box) are HVAC duct elbows turning down into a **Duct Roof Curb** beside it.
- **Dry Cooler** (DC-1, DC-2; 2 x 6 blocks, 2 high), by the west vertex over the data hall: legs on rubber feet, the
  finned coil, 12 EC fans in nozzle rings under wire guards whose rotors turn (on the client), the header end's
  insulated drops, butterfly valves with red levers and the fan controller. Their four pipes run east on **Roof Pipe
  Supports** into two **Pipe Doghouses**, and the walkway crosses them on a **Pipe Crossover** (stairs each end, a
  grating deck a metre up between channel stringers, yellow handrails).
- Six exhaust fans over the rooms they serve: **Roof Exhaust Fans** (spun aluminium downblast hoods) for the restrooms
  (EF-1, EF-2), the crew kitchen (EF-4) and the showers (EF-5), the **Kitchen Exhaust Fan** (an upblast fan with its
  grease box) for the kitchen hood (EF-3), a **Roof Upblast Fan** for the battery room's hydrogen (EF-6), each on its
  curb with its isolator.
- The comms mast on the stair house's west end: a **Comms Mast Base**, eight **Comms Mast** sections (step bolts, the
  feeder ladder with its coax), two of them **Comms Mast Wall Ties** to the house's wall, the **Comms Mast Top** (three
  sector panels with their radio units, two whips, the steady red obstruction light, the GPS puck, the lightning
  finial), two **Microwave Dishes**, and the **RF Cabinet** at its foot with its cable bridge to the mast.
- The walkways: the warehouse walkway paint in the slab, straight runs with right-angle turns: a ring round the pillars
  outside every bay's flight path, spurs to every pad's edge, the three pillar doors, the plant and the stair house's
  door; east of pillar B it runs down past the goods lift's house (it ran over the supply pickup's landing platform in
  layouts 37 to 39).

All of it only looks live (the server load rule): nothing ticks; the dry coolers' fans turn on the client
(`RoomKitRotorBlock` gives the anchor a tickless block entity, `client/RoomKitRotorRenderer` turns the rotors from
the manifest's `rotors`). Tier 1 then cost 709 items instead of 296 (since 26 October only its hangar pads are paid for). Layout 36 (`TowerData.TRIPILLAR_ROOF_PLANT_LAYOUT`)
is this revision. Layout 35 is frozen as `tower_tripillar_v35.json.gz`, and built towers are refitted to it on load.
`TowerGameTests.roofPlantStandsOnTheRoof` checks the plant and the walkway from the door.

**The stations inside** (work list item 20, take 1 approved by Justin, 26 October 2026: the intake wall a flat face on
the ground floor only under the upper floor's point, and the big goods lift, "put multiple crates in the lift when it's
going up, it's just cosmetic anyways"). The exchanges, the terminal, the packager and the pickup stood outside in a yard
east of the building; they are part of it now (`tools/tripillar_stations.py`, with the foundation), and the yard is gone:
- **The intake face**: the ground floor's east point steps back to a straight wall (x 45, z -10..10) of the exterior
  kit's anthracite wall panels on a plinth, under the upper floor, whose point roofs over it as a canopy on one steel
  column (item 12's girder), a yard-concrete apron under it with downlights in the soffit. The Energy Exchange (north)
  and the Cargo Exchange (south), the same formed stations, stand side by side behind it, both facing east, their
  fronts in the face, so a cable or pipe from outside plugs straight into the port (z -5 and 5, two up): an **Energy
  Port Plate** and a **Cargo Port Plate** over them, hazard plating in front, a badge-reader maintenance door between.
  Behind them the intake room: the aisle along their backs, painted end walls, a framed-glass wall onto dispatch
  (taller than a room's: the stations stand five high) with a steel door.
- **Dispatch**, east of pillar B: the depot terminal (the dispatcher faces north, the lift on the right) with its chair,
  the cargo packager, two **Pallets of Drone Crates** waiting, bollards at the lift's corners. The security office
  stands south of it (fix 9, layout 38).
- **The goods lift** (`tools/goods_lift.py`, room kit pieces of the "dispatch" room): a glass shaft (girder corners, a
  steel band at each slab) from a pit in dispatch up through the engineering lab to its house on the roof, two **Lift
  Guide Rails** on its back wall; a **Goods Lift Door** at each landing (3 wide and 4 high, the top block its housing:
  vertical bi-parting leaves with a wired-glass vision panel and a hazard edge, the floor indicator, the call station,
  the GOODS LIFT placard), facing dispatch at the bottom and the roof's platform at the top. The lift is look-only
  (`client/GoodsLiftRenderer`, through `client/RoomKitDisplayRenderer`): while the depot has drones out, the cantilevered
  car (chequer plate, toe guards, three lifting mesh gates, a car light) takes two pallets of crates up, waits while
  they are unloaded and comes down empty, the doors opening and shutting and the indicators following it; with nothing
  out it waits at the bottom, shut. Nobody rides it: its doorways never let anyone through (`GoodsLiftDoorBlock`).
- **The roof**: a 9 x 9 landing platform (x 33..41, z -10..-2) with the supply pickup in front of the lift house (the
  shaft's head: anthracite panels, parapet, roof deck, a smoke vent, a steady red obstruction light on top); the
  walkway runs over it, clear of pillar B's bay exits.

The depot terminal finds that platform and the packager through its tower (`TowerData.platform` and `packager`, the
layout's "platform" and "packager"; `DroneTerminalBlockEntity.towerPoint`), and shows "Platform 9x9 · the supply
pickup" instead of asking for a pad; the exchanges bind at their new masters (`ExchangeStations.masterOffset` and
`facing`, layout 37: both east). Layout 37 (`TowerData.TRIPILLAR_STATIONS_LAYOUT`) is this revision; layout 36 is frozen
as `tower_tripillar_v36.json.gz` and built towers are refitted on load: anything connected to the old exchanges' ports
has to be connected again at the intake face. `TowerGameTests.stationsStandInsideTheBuilding` checks it. Layout 40
reworked dispatch, the lift and the roof: see the cargo area below.

## Fixes from Justin's 26 October 2026 playtest

Taken one at a time, each from a sheet of renders of the real build that Justin approved first.

1. **The training room closed at both ends** (layout 38, `TowerData.TRIPILLAR_TRAINING_WALLS_LAYOUT`): its front wall
   (x -38) stopped short of the curtain wall at both ends, so the room was open round them ("cut off on both sides").
   At the north end a return wall in line with the meeting room's north wall (z -17) now closes it. At the south end the
   front wall runs on to the south-west offices' wall (z 22), and the room takes in the window bay there (carpet,
   ceiling). The passage between it and the simulators now runs straight to its end, with a light in the hall's
   ceiling over that end. Layout 37 is frozen as `tower_tripillar_v37.json.gz`, and built towers are refitted on load.
   `TowerGameTests.assertUpperFloorRooms` checks the walls.
2. **The simulator room's corner cut back** (fixes 2 to 9 are in layout 38 too): its north-east glass corner stood out past the
   atrium's rail, over the void, and the room cut the gallery's ring. A 4 x 6 piece of that corner is gallery floor now,
   with the rail back along the atrium's edge; the room's glass steps back round it (on the gallery's floor, as its other
   glass stands), so the gallery runs all the way round, at least two blocks wide (the step straightened out in layout
   39: fix list 3 below). The waiting bench inside the doors,
   with its side table, is gone, and a plant stands in the new corner.
3. **Plants against the walls**: the offices' plants that stood loose on the floor (one between the phone booth and the
   desks) stand against the walls, the tip's in its corner behind the phone booth; the lab's two the same.
4. **The gallery's rail joined round its steps**: where the atrium's stepped edge turns, a rail cell has the atrium on
   two sides; it had glass on one only, leaving a gap (and a way to step off). Each of those is an L now (the Framed
   Glass's corner: its glass round both sides), so the rail runs on unbroken (`tower_tripillar.RAIL_CORNERS`).
5. **The crew rest's recliners out**: the two recliners facing the windows, and their side table, are gone.
6. **The manager's coat stand and plant**: the coat stand in the corner inside the door, against the west wall and the
   glass front; the floor lamp at the sofa's end; the plant in the far corner by the windows.
7. **The map room furnished**: a large Ops Wall Screen on each side wall (the airspace on the west, the missions on the
   east, live), the clock over the west one, acoustic panels either side of each, a leather sofa either side of the
   doors facing the map, a plant in each corner.
8. **The upper restrooms taken out**: the men's and women's rooms are open gallery floor; the custodian's closet stays,
   with its own south wall, and the bottle filler stands against it.
9. **The security office moved** (ground floor): it stood west of pillar C in front of the data hall's glass ("it looks
   really ugly with that one in front of the other one"). It is east of pillar B now, south of dispatch, in the plot
   item 20 kept free: its glass front and doors on the hall, its other walls solid (the intake room's partition is
   painted wall behind it, not glass). Its old place is open hall floor, the data hall's glass and racks in view. The
   plant's CCTV desks (five little painted monitors each) gave way to a row of the hall's console desks working
   SECURITY: the gates and doors, who is in, the alarms, live.

## Fix list 3 (Justin's 27 October 2026 playtest)

Small fixes, done straight from his screenshots (layout 39, `TowerData.TRIPILLAR_FIX3_LAYOUT`; layout 38 is frozen as
`tower_tripillar_v38.json.gz`, and built towers are refitted on load):

1. **The custodian's closet walled over its door**: the wall above its door (x -26, z -40) was missing; it is painted
   wall up to the band now, as over every other door.
2. **The simulator room's east glass one straight run**: after fix 2 the glass stepped out to x -13 south of z 12, a
   pane "out in the middle of the open". The room ends at x -18 all the way to its back wall now (16 x 16 inside); the
   gallery is open from there to the pillar. The water cooler and the files stand by the east glass inside, a plant
   beside them.
3. **The upper lounge's TV a Flat Screen TV**: the TOWER TV channel over the media console became a 3 x 2 Flat Screen
   TV on the east wall (`tv/FlatTvBlock`), anyone's to put a video on. `TowerGameTests.assertFlatTv` checks it whole.
   The ground floor cafe keeps its Lounge TV.

Two fixes outside the tower's layout came with it:

- **Rain no longer falls indoors** (`building/RainCover`, `mixin/HeightmapCoverMixin`). Since 26.3 the game does not ask
  whether a block is solid to know where rain stops (and where it is raining at all, where snow settles, where the
  surface is): its motion-blocking heightmaps take the blocks in `#minecraft:blocks_motion`, and only vanilla's are in
  it, so every Jugcraft roof let the rain through. Now a Jugcraft block that is solid by the rule the game used before
  (`BlockState.isSolid()`) counts in those heightmaps too, on the server and on clients, and a chunk saved with the old
  heightmaps has them worked out again when it loads. `RoomKitGameTests.roofsKeepTheRainOut` checks it.
- **The no-step-on furniture taken out** (Justin, 27 October: "just revert the changes with the whole walking up on
  things"): see the walk-through fixes above.

## The cargo area reworked (Justin's 27 October 2026 playtest, take 2 approved)

Justin: put the supply pickup on top of the goods lift's house ("make the lift just go all the way up"), take the
platform off the roof, fix the gaps at the intake face and the intake room's door, and make the cargo area "look not
shit": the exchange feeds things in, "little drones, maybe similar to the warehouse" take them to the lift; no packager
or crate pallets standing about, the old terminal gone, the consoles moved aside and the security office in their
place. Take 1's sheet moved the office south; his notes ("move it over more ... closer to the main wall", the roof
house "symmetrical": a white strip with no panels on it looked "kind of tacky") made take 2, which he approved. Layout 40
(`TowerData.TRIPILLAR_CARGO_LAYOUT`; layout 39 frozen as `tower_tripillar_v39.json.gz`, built towers refitted on load):

- **The security office** moved into the hall's south-east corner by the curtain wall (x 30..39, z 10..19 with its
  walls: `tripillar_rooms.SEC_DX, SEC_DZ`), next to the intake room's south end; the battery room's doors are reached
  along z 20 (a slot one block wide: closed in layout 49, the doors turned to the hall). The east operator rows' outer pair (DATA HALL and COMMS, x 30) is a third row, x 20 and 25 at z 17..19.
- **The cargo hall** (dispatch, `tripillar_stations.py`): the **Case Packer** (`cargo_case_packer`, a room kit piece
  with the warehouse's Intake Case Packer's model, 3 x 2 x 2, look-only: no bay, nothing to put in) against the intake
  room's wall, which is solid anthracite behind it (the cargo exchange feeds it through the wall); the **pack spot** in
  front of its conveyor's stop; a warehouse drone lane (x 33) from the goods lift's landing down past it; the bollards by
  the landing; the **DISPATCH** and **SUPPLY** console rows (x 31, facing the lane and the glass). The old depot
  terminal, its chair, the cargo packager and the crate pallets are out of sight: the terminal and the packager (the
  depot's store, which the cargo exchange fills) stand under the hall's floor (`TowerData.terminal`, `packager`), and
  the consoles open and show the terminal as before.
- **Its drones** (`client/warehouse/CargoHallRenderer`, drawn with the lift's ground door, on the lift's round; look-only
  as the lift is): cases run along the packer's conveyor to the stop; the stacker drone beside the pack spot lifts them
  one by one (its arm reaching by the warehouse's own inverse kinematics) onto the pallet the pallet drone holds there,
  eight to a pallet; the drone carries the full pallet up its lane to the lift's landing and waits. When the lift's
  doors open it drives the pallet into the car, backs out, takes the empty pallet the car brought down and carries it
  back to the pack spot while the car takes the full one up to the pickup. The packer's HMI shows its status, the
  conveyor, the store and the drones out (live); its stack light is green while the lift runs. With no drones out the
  lift waits at the bottom, and so does the hall: the drone at the landing with a full pallet, the stacker folded.
- **The intake face**: a dark carbon-composite surround over the stations and between them (their models stand in from
  their cells' sides, so a panel's pale liner edge showed there, "you can see inside"), the bay over the door's way in
  and the room's north end dark. The intake room's door stands in its glass's plane (facing west, its hinge on the
  right), not a pixel out of line.
- **The goods lift** runs all the way up into its house: no roof door; the car stops under the house's top
  (`GoodsLiftRenderer`: with no door over the lowest, the top of its run is the well's ceiling less the car's height).
- **The roof**: the lift house is the same on all four sides: anthracite panels on a plinth between **steel corner
  posts**, a steel band round its top, an obstruction light on each corner; on its top, over the car, the 3 x 3
  **supply pickup** on its landing platform, where the drones load. The roof slab's 9 x 9 platform is gone (the roof
  and the walkway are back as in layout 36).

**After playing it** (Justin, 27 October 2026; layout 41, `TowerData.TRIPILLAR_CARGO2_LAYOUT`, layout 40 frozen as
`tower_tripillar_v40.json.gz`):

- **The packer connected to the cargo exchange** ("it's not connected to the cargo exchange, it just looks stupid"): the
  **Packer Infeed Conveyor** (`cargo_infeed`, a room kit piece of the dispatch kit, `tools/goods_lift.infeed`): out of
  the cargo exchange's back under a discharge hood (on a bracket bolted to it), a belt conveyor over the intake room's
  aisle (three blocks of headroom under it), through the room's wall in a galvanized sleeve (the wall panel cut round it,
  flashed both sides), to its head over the packer: the drive on its side, the head under a stainless hood, a spout
  down into the packer's hopper. Sacks and crates ride it while the lift runs (`CargoHallRenderer`, look-only).
- **No more seeing through beside the exchanges** (the slots beside the door between them, from the hall to outside, and
  at their ends): the large exchanges' models (`tools/tower_large_exchanges.py`) stood 2 to 6 px in from their sides; each
  has an end plate on both sides now, out to its cells' edge (the cargo exchange's side cassettes, which they cover,
  gone). That is in the models, so every tower has it at once.
- **The boxes on the pallets solid** (you saw through them): the pallets' stretch film is see-through, and drawn in one
  go with the cartons, a film face drawn before the cartons behind it hid them. The warehouse's moving parts and loads
  (`client/warehouse/WhQuads`, the warehouse's own drones too) and the goods lift's live parts are drawn solid first, the
  see-through faces after.

A tower's own platform may be as small as its pickup and needs no pad (`PlatformLayout.scan(..., minSide)`,
`DroneTerminalBlockEntity.platformReady`): its drones dock in the tower's bays and on its roof pads. (Until now a tower
depot's platform, with no pad of its own, was not "valid", so its drones were never sent out: fixed with it.)
`TowerGameTests.stationsStandInsideTheBuilding` and `towerCoreBuildsTheFoundationThenTheRoofPads` check it; the hall's
round was run outside the game (every case place and the conveyor's stop within the stacker's reach, the arm never
jumping) and its poses rendered over the real build.

**Symmetry, and the diorama back** (Justin, 27 October 2026; layout 42, `TowerData.TRIPILLAR_SYMMETRY_LAYOUT`, layout 41
frozen as `tower_tripillar_v41.json.gz`):

- **Each wall panel run's two ends mirrored** ("you're putting the corner ones down in one singular direction"): a corner
  panel is an L, its skin on its facing and the face clockwise of it, the flashing on that edge. Layout 41 gave both
  ends of a run the same facing, so one end had its return and flashing and the other showed the panel's pale core
  (and a seam line on one side only). Now each end turns its return outwards (`tripillar_stations.end_facing`): the
  intake face's (x 45) north end faces east, its south end south; the intake room's glass wall's solid stretch (x 37,
  behind the Case Packer) north end north, south end west.
- **The intake room's two ends alike**: dark beside the cargo exchange at the south end as beside the energy exchange at
  the north (the south end was painted).
- **The diorama back**: the hall's small hologram table was placed as nine loose sections that form when the ninth goes
  down. A conversion re-places every block of the design, and setting a formed section back to the loose state is the
  same block, so it doesn't re-form: after layout 40's conversion the table was nine small tables and the diorama
  (drawn by the formed table's middle) was gone. The table is placed formed now (its parts in the layout), so a
  conversion leaves it as it is. `TowerConversionGameTests.cargo2TriPillarKeepsItsDioramaTableFormed` converts a
  layout-41 tower and checks the table and the mirrored ends.

**The atrium dome** (Justin, 27 October 2026: "for the pillars ... just the internals ... cylindrical at the bottom, but
then it becomes more of like a sphere as it goes up and then blends into the rest of the architecture"; option A of the
inside-pillars sheet, approved; layout 43, `TowerData.TRIPILLAR_DOME_LAYOUT`, layout 42 frozen as
`tower_tripillar_v42.json.gz`; `tools/atrium_dome.py`):

- Over the rotunda the three pillar feet used to hang into the atrium as square concrete masses with flat soffits (y 11)
  up to the roof slab. Now the rotunda's cylinder (R 15, the curved video walls on its face) goes on up as one surface: at
  the ticker ring it springs into a sphere (its centre on the hall's axis at y 7.5, through r 15 at y 11, leaning in to
  r 9 at the roof) that the feet are cut back to. Formwork ring joints every 1.5 blocks up the curve, a warm light line
  at its foot and where it meets the roof. Nothing changes outside the building.
- **Smooth, not stepped.** A cell the sphere passes through is an **Atrium Dome** block (`jugcraft:dome_shell[index=n]`,
  no item; it breaks to reinforced concrete): its look is the concrete of that cell outside the sphere, the sphere's
  surface in it cut exactly at the cell's sides and the cell's own faces cut round the sphere, baked into the chunk like
  any block model (`client/DomeShellModel`, shaded by the surface's own normals so the curve reads as one surface); its
  collision and outline the same at a quarter block. 843 cells, 22,088 quads, all in
  `data/jugcraft/drone_tower/atrium_dome.bin.gz` (`tower/DomeShellData`, read on both sides). The feet's cells inside
  the sphere are open; the cut's downlights, which hung from the soffit, are hidden light blocks where they were.
- **The riser ladders** go up the feet's outer corners from the ground to the roof now (the inner corner's ladder stood
  where the dome is), and the pillars' tiers carry them on up from that corner (`pillar_ladder` tries it first).
- `TowerGameTests.assertAtriumDome` (every shell cell in place with its index, the dome open, the ladders up the outer
  corners) runs in the build test and in `cargo2TriPillarKeepsItsDioramaTableFormed`'s conversion.

**The diorama's own case** (Justin, 27 October 2026: "you can make the diorama its own little multi-block structure. It
doesn't need to be like these hologram tables"; option A of the stand sheet, approved; layout 44,
`TowerData.TRIPILLAR_DIORAMA_CASE_LAYOUT`, layout 43 frozen as `tower_tripillar_v43.json.gz`): the hall's small hologram
table gives way to the **Tower Diorama Case** (`tools/diorama_case.py`, a room kit piece of the hall, one item for its
36 cells): a 3 x 3 white lacquered plinth on a recessed black kick, a brushed steel lip, the model on a graphite tray
under a clear acrylic cover on slim black corner frames, a warm light strip under its top on two sides (light 6). The
model is the same still diorama (`client/TowerDiorama`), drawn on the tray by the case's anchor
(`client/RoomKitDisplayRenderer`, display "diorama"), so any case anywhere shows it. The desk and chair stay in front.

## The flight-ops trees (Justin, 5 November 2026)

Justin, of the desks in the middle of the west floor: "i wanna replace these computers in the middle of the area with
new things, i wanna put other exotic trees like the other we made. crate me a few types of options for me to choose
from". Of the options sheet (four trees of our own design, drawn in the hall) he chose "option A for one of them, and
option D for the other", on both floors. Layout 45 (`TowerData.TRIPILLAR_OPS_TREES_LAYOUT`; layout 44 frozen as
`tower_tripillar_v44.json.gz`, built towers refitted on load): the twelve rows of Console Desks and their chairs are
gone; the **Ember Oak** stands on the west floor (master cell (-26, 1, 11)) and the **Drift Isle** on the east (master
(23, 1, 13)), each in a round bed about 9 blocks across.

Layout 46 (`TowerData.TRIPILLAR_TREES_CENTRED_LAYOUT`, Justin, 6 November 2026: "center that tree in that room more.
It's like sitting way too close to one wall. Then this tree needs to be more centered as well"; layout 45 frozen as
`tower_tripillar_v45.json.gz`, built towers refitted on load): each tree stands in the middle of its floor, as far from
everything round it as it can be. The Drift Isle moves to (19, 1, 12): its bed had 2 blocks to the security room's glass,
now 6, with about as much to the pillar foot, the lift core and the battery room. The Ember Oak moves to (-23, 1, 12),
out from the windows and the data hall into the middle of the floor between the locker room and the pillar foot. Only
the trees' cells change (and two night lamps near them).

- **The Ember Oak** (`jugcraft:ember_oak`): charred black bark split by cracks that glow like embers, a trunk of three
  gnarled strands opening into six wide, low limbs, a flat crown of ember-red leaves, some glowing hot, about 6 blocks
  up; sparks drift up out of the crown to the ceiling. It stands in a stepped bed of hexagonal basalt columns: an inner
  ring stepping between 0.6 and 1.1 high round black volcanic gravel with glowing coals in it, the outer ring at seat
  height (0.46). Light breathes through the cracks (a slow wave up the trunk and out along the limbs, a quick flicker on
  it); the hot leaves and the coals flicker; soft ember glows hang in the crown.
- **The Drift Isle** (`jugcraft:drift_isle`): an old twisted tree with violet foliage on a mossy rock island floating
  over a dark slate basin (its wall 0.6 high, the floor 0.2); the island's jagged underside tapers to a point, roots
  hang from it with glowing blue tips; two emitter rings in the basin's floor send a soft blue lift beam up to it; five
  small stones circle it; a timber seat (0.46) runs round the basin. The island bobs gently (7 cm, 7.5 s) and turns once
  in five minutes; the rings pulse outwards, waves of light go up the beam, the root tips pulse, the stones tumble as
  they go round.

How they are made and drawn:

- `tools/ops_trees.py` (textures in `tools/ops_trees_tex.py`; the mockups' generator ported) bakes **three variants of
  each tree** (the approved mockup's first, then two picked from a sheet of candidates), the texture atlas
  (`textures/entity/ops_trees.png`, 1024 x 1024; tiles that repeat are cut at their repeats) and its map
  (`tower_mesh/ops_trees.json`, which also holds each variant's glows, sparks, root tips and stone orbits), with ambient
  occlusion baked per corner: `tower_mesh/ops_ember_bed.bin.gz` (the columns and gravel, 943 quads) and
  `ops_ember_oak_0..2.bin.gz` (12-14k quads each), `ops_drift_isle_base.bin.gz` (799), `ops_drift_isle_0..2.bin.gz`
  (5.6-5.8k each) and `ops_drift_stone.bin.gz`; the item icons; the data files.
- `client/OpsTreeRenderer` picks a placed tree's variant and turns it by an angle of its own, both from its seed (the
  master's position, `OpsTreeBlock.seed`), so two trees side by side differ. The Ember Oak's bed is never turned (it
  collides column by column). Leaves and bark are shaded by the world; the coals, hot leaves and emitter rings are full
  bright; the cracks, beam, glows, sparks and root tips are translucent glow (`entityTranslucentEmissive`). Everything
  moving is the client's: the server keeps only the blocks.
- `tower/OpsTreeBlock` (the master, the middle cell on the floor; `Kind` EMBER_OAK or DRIFT_ISLE) and
  `OpsTreePartBlock` (one block for each tree): one item places a tree whole (it needs its cells clear and shows the
  placing ghost). The parts are the bed's 68 cells round the master, plus the Ember Oak's two trunk cells over it, or
  the Drift Isle's 21 island cells two up (to stand on) and two trunk cells over them. They collide on an
  eighth-of-a-block grid: the Ember Oak with each basalt column at its own height (read from
  `data/jugcraft/ops_trees/ember_oak_bed.json`, which the generator writes), the gravel and the trunk; the Drift Isle
  with the basin, its floor and the seat, and the island's thick middle and thinner rim. The crowns do not collide.
  Light: the Ember Oak's master 10 and trunk cells 13; the Drift Isle's master 9 and island cells 6.
- Recipes (the Advanced Workbench, two trees a craft): the Ember Oak from blaze powder, an acacia sapling, dark oak logs,
  magma blocks, basalt and coal blocks; the Drift Isle from phantom membranes, a flowering azalea, moss blocks, ender
  pearls, polished deepslate and dark oak planks. The tower doesn't charge for them (not charged blocks).

## The tower crew (Justin, 5 November 2026)

Justin: "i want to make some villager-like NPCs to run around the place. i want it to look like the NPCs have different
tasks and uniforms depending on what theyre doing". He chose real mobs with simple AI, about 20-30 of them, in four
departments (flight ops, security, technicians and mechanics, cafe and cleaning); of the first sheet (realistic people)
"they should look like minecraft characters, not real humans"; of the second, approved, "make them all white, and only
blue or green eyes" and "make females be the janitors, everyone else is male"; and they can be killed, the tower hiring
a replacement.

- **The crew** (`jugcraft:tower_crew`, `tower/crew/TowerCrew`): player-shaped, at a player's size, each in a skin of
  their own (`tools/crew_skins.py`: 38 skins in nine uniforms, the tower's amber badge on every left chest; the janitors'
  arms three pixels wide, everyone else's four). Their gear stands out as boxes (headsets, cap peaks, the mechanics'
  hard hats, buns and ponytails) and they carry box-model props (tablets, clipboards, radios, toolboxes, mugs, trays,
  the facilities cart). Each has a name, shown when you look at them; click one and they say something of their job.
- **Their posts** (`tools/tripillar_crew.py`, the layout's `crew` list, 27 posts, all in with the foundation): flight ops
  8 (the master console's three chairs, its tablet controller, reception, dispatch, supply, the flight director walking
  the hall), security 5 (the alarm console's two chairs, the security desk at reception, the gate guard at the speed
  gates on his radio, a patrol round the hall), technicians 8 (the build desk, two in the data hall, three mechanics at
  the drone service pad, two on their break in the cafe), cafe and facilities 6 (the cook on the line, two servers with
  trays, three janitors pushing carts round the hall). A post is a chair (they walk to it and sit, typing; while a
  player has it they wait beside it), a station (spots they stand at in turn, facing the work: reading, cooking,
  inspecting, on the radio) or a patrol (spots they walk round, pausing at each). Every spot is checked to be free floor
  reachable from the others without a door.
- **Life and death**: they can be hurt and killed and drop nothing. Security officers and the gate guard fight back and
  see off monsters; everyone else runs. A crew member who dies (or goes missing) leaves their post empty; five minutes
  later (`TowerCrewCare.REPLACE_TICKS`) the tower hires someone new for it, in a face nobody in the tower wears
  (`tower/crew/TowerCrewState` remembers who stands for each post). They never despawn, can't be leashed, and leave
  if their tower's core is broken; one who strays or gets stuck is put back at their post.
- **Drawn** by `client/TowerCrewRenderer` with `client/TowerCrewModel` (the player's boxes, both arm widths, the outer
  layers stood off each other so no faces share a plane; the gear from `assets/jugcraft/crew/gear.json`) and
  `client/TowerCrewPropsModel` (`crew/props.json`, `textures/entity/tower_crew_props.png`), posed for what they are
  doing (typing, reading, on the radio, cooking, pointing, pushing, carrying a tray or a mug). They sit on the chairs'
  own seats (`SeatEntity.seat`), as a player would.

### Four more jobs, ten more crew (Justin, 5 November 2026)

He chose "More crew jobs", all four of drone pilots, engineers, a medic and managers, and "add in an additional 10 people,
spread them between jobs appropriately ... only 1 manager"; he approved the new-jobs sheet as shown. 37 posts now (the
ten new ones upstairs, all men):

- **Drone pilots** (4; an olive flight suit with its pockets, flying gloves, a headset): two in the Pilot Simulators,
  leaned back with the seat (22.5 degrees), the right hand on the stick, the left on the throttle; one briefing at the
  lectern; one listening at his desk in the briefing room.
- **Engineers** (4; a white lab coat to the knee over a blue shirt and tie, some in glasses): two at the CAD desks, one at
  the lab bench, one watching the flight test cage.
- **The medic** (scrubs, a stethoscope, a green first-aid cross): on rounds in the first aid room with a clipboard.
- **The manager** (a charcoal suit, the tower's amber tie): at his desk.

The office chairs, the training chairs and the simulators are room kit pieces, not seats for players: their posts carry
the seat's point and facing (`seat_at`, `seat_yaw`, worked out by `tools/tripillar_crew.py` from each piece's cushion), the
crew sit there by the same `SeatEntity`, and it stays under them while they sit (`SeatEntity.tick`). If the piece is
broken they stand up.

### The night shift and breaks (Justin, 5 November 2026)

He chose "Crew routines", "Night shift" (about a third stay on duty at the key posts, the rest go off to the bunks,
sofas and recliners and sleep) and "Now and then" (one or two short breaks a day each, at the nearest cafe, dining room
or lounge), and approved the routines sheet as shown. `tools/tripillar_crew.py` (`routines`) writes each post's routine
into the layout's `crew` list; `CrewPost.restAt` reads it against the world's clock:

- **The night watch** (12 of 37): both console controllers, dispatch, one alarm console, the security desk, the gate
  guard, the hall patrol, one tech on the data hall rounds, one mechanic on the pad rounds, the north janitor, the pilot
  in simulator A and the medic. They work through the night.
- **The other 25 sleep on their own floor** (the crew can't climb the riser ladders): the lobby lounge's two sofas (10),
  the cafe's chairs (the cook, the servers and the two on their break, 5), the east cafe sofa (2), the crew rest room
  upstairs (an engineer in the lower bunk under a blanket, three on its sofa), the upstairs hall's sofas (the briefing
  pilots), the lounge recliner by the window, and the manager on his office sofa. Each sleep spot is the nearest free one
  (a bed or the recliner worth a longer walk). They go off a few at a time from 18:30 (the furthest to walk last, by
  about 20:00) and are up from 05:00, back at their posts by 06:00. The upper bunk stays made up: its folded blanket lies where
  a sleeper's legs would go.
- **Asleep**: sitting up dozing, each in one of four ways (head nodded, tilted, chin down, tipped back; from their id),
  hands in the lap; laid back in the recliner with the legs on its footrest and the forearms on its arms; or lying on
  their back under a navy blanket with a turned-down sheet, the head on the pillow (`TowerCrewRenderer.setupRotations`
  lays the body down about the hips' back onto the seat's point; arms and legs under the blanket). Eyelids shut (an
  `eyelids` gear group on every skin, shown only asleep), headsets and hard hats off, nothing in their hands, still,
  square to the seat. Clicked, they mumble (`message.jugcraft.tower_crew.asleep.*`); hurt, they wake, stand up and stay
  up for a minute (`TowerCrew.WOKEN_TICKS`) before settling again.
- **Breaks** (halved on 10 November 2026, r204, "The crew's spread" below: one a day at most, 88 over the rota's three
  days where there were 175, about three away at once): as first built, two a day each (none for the cafe's two already on theirs), the second every other day; about 40 seconds
  sat with a coffee plus the walk there and back (8 ticks a block), at the nearest cafe, dining or lounge seat nobody
  else is on then. Partners are never away together (the master console's three, the alarm consoles, the security desk
  and the gates, the simulators, the CAD desks, the cafe's three, the pad crew, the data hall, dispatch and supply, the
  briefing room). At most 9 are away at once, usually about 6. Clicked on a break, they talk about the break
  (`message.jugcraft.tower_crew.break.*`). The manager's office seats are his alone.
- **Doors**: the crew open the glass and steel doors (`PlantDoorBlock`, opened by hand) on the way
  (`PathNavigation.setCanOpenDoors`, `OpenDoorGoal`); their closers shut them behind.
- Sitters sit in the middle of a cushion clear of any armrest, a pixel back towards the backrest (`hips`, from each
  block's models). The briefing desk's pilot moved to the back row's chair: the middle chairs' free cells are shut in by
  the next chairs and the riser, so he couldn't walk out. The props texture is 128 x 128 now (the blanket came in).

### Greetings and giving way (Justin, 5 November 2026)

He chose "Crew greet players" and, on the sheet, "Add a word too" with "occasionally they insult the player, or say
something like fuck off". `tower/crew/CrewGreeting` decides, `client/CrewGreetings` draws it for the client's own player
(the server isn't asked anything; each player sees the crew greet them):

- Awake crew within 4 blocks, facing within 70 degrees of the player, greet them at most once a minute each: a wave (the
  raised hand rocking out and back); security and the gate guards salute; a nod (twice) with their hands full (a radio,
  a toolbox, a mug on the move, a tray, the cart) or seated at work; seated with a coffee, the coffee raised; asleep,
  nothing (`TowerCrewModel.greet`, over whatever they are doing, 30 ticks).
- With a greeting, now and then a word in chat ("Name (Job): ..."): at most once in ten minutes from each crew member and
  once in 20 seconds from anyone; for the time of day (morning, afternoon, evening, night, four lines each), or one in
  ten rude (twelve lines, some swearing, e.g. "Fuck off, I'm busy."), and then no friendly gesture. The lines are
  `message.jugcraft.tower_crew.greet.*` in the language file, so they can be changed there; insults stay swearing and
  rudeness, nothing aimed at who anyone is.
- **Giving way** (`TowerCrew.GiveWayGoal`): walking, with a player within 2.2 blocks ahead (within about 45 degrees),
  they step to a free cell beside them (the side away from the player), turn to the player, and walk on once the
  player is past or after 4 seconds; not again for 10 seconds. A check every half second while they walk.

### Narv-Tech uniforms (Justin, 6 November 2026)

"Change the uniforms ... for the roles that exist, but I want the color scheme to be black and purple and I want all
uniforms to say Narv-Tech somewhere on it" and "look futuristic". Approved after rounds on security: "make security look
more like a soldier, with a full helmet on", "make the security guy wear an exoskeleton suit as well", "bulkier", "even
bulkier", "bigger helmet to match". `tools/crew_skins.py` paints them (`uniform`):

- All 13 jobs in black, charcoal and graphite with purple: high collars trimmed in purple, graphite shoulder plates,
  glowing piping down the arms and legs, LED cuffs, boots with a glowing welt, cap bands. Flight director: a long black
  command coat, silver shoulder bars, lit facings. Controller: zip jacket, crew card on a purple cord, headset. Pilot:
  flight suit, graphite panels. Engineer: black lab coat on purple lining. Tech: purple yoke. Mechanic: coverall, pads, a
  graphite hard hat with a lit ridge. Gate guard: chest plate and a light vest (purple light bands) in place of hi-vis.
  Medic: black scrubs, a lit cross on each sleeve. Manager: black suit, lavender shirt, purple tie. Cook: black chef's
  jacket, black and purple check, apron. Server: purple apron. Janitor: coverall, purple gloves.
- **NARV-TECH** across every uniform's back and a hexagon N emblem on the left breast: flat decals on the props layer
  (`props.json` groups `wordmark` and `emblem`, `prop` 0: always shown), drawn finer than the skin (their boxes in
  texels, the part scaled down: 0.148 and 0.18), 0.1 px off the jacket. Off under a blanket.
- **The glow**: each skin has a `<skin>_glow.png` and the props a `tower_crew_props_glow.png`, drawn full bright and
  added over the faces (`client/TowerCrewRenderer.GlowLayer`, `RenderTypes.eyes`): violet by day, standing out at
  night; drawn on the client only.
- **Security** (`GEAR_HELMET` 64, `GEAR_EXO` 128 in the pool): a helmet in place of the head and power armour over a
  black ribbed undersuit. Since r195 the helmet and armour are the Narv-Tech power armour's mesh (see "Narv-Tech power
  armour and the plasma blade" below), not boxes on the props layer; the props' `helmet` and `exo_*` groups stay, empty,
  so the bits keep their meaning (the uniform's print and emblem are `hide`: 128, the suit carries its own). Both come
  off to sleep; the pack on the back is put by while they sit, so it doesn't go through the chair. A crew member loaded from a save takes their skin's gear from the pool now
  (`readAdditionalSaveData`), so security already hired get the helmet and armour.

### Security's plasma rifle (Justin, 6 November 2026)

"Make a plasma rifle for them to have", the firing sound like a plasma gun ("deeper, I want it to sound deeply
intimidating"); security shoot "mobs, and players who attack"; players get it "creative only". `tower/crew/PlasmaRifle`:

- Security (whoever wears the armour, `TowerCrew.armed`) carry it on duty: at the low ready when they stand or walk (the
  client draws `ACT_READY` for an armed crew member standing idle with nothing in hand), put by while they sit.
- `TowerCrew.RifleGoal`: at their target (a hostile mob they see, from the existing target goals, or a player who hurt
  any of the crew within 24 blocks: `hurtServer` sets them on that player for 30 seconds, or until the player is out of
  range, creative or gone), they get up (awake if they slept), turn square to it, raise the rifle (`ACT_AIM`) and fire a
  bolt every second while they see it within 24 blocks and nobody else (crew or another player) is in the line; closer
  if they can't see it; back to work when it's gone or unseen for five seconds. (The gate guards carry the plasma baton:
  below.)
- `PlasmaBolt`: 3 blocks a tick, no gravity, 20 ticks at most; 24 damage to the first creature it hits (`mobProjectile`;
  6 at first, then 10 so a zombie dropped in two: "dropping in like 2 hits", r162; 24 since r178 so one shot kills a
  zombie, for the crew and players alike: "I want it to like one shot a zombie"),
  nothing to blocks; it passes the crew, and a crew member's bolt passes players it wasn't fired at. Client: the bolt
  (`client/PlasmaBoltRenderer`, a near-white core in a purple glow on the eyes layer), a purple muzzle flash and a trail
  of sparks (its own particles); the server sends the hit's sparks and sound.
- `PlasmaRifleItem`: in the creative menu (Combat), not craftable; fires the same bolt, once a second. Since r190 it's
  the NT-7, worked like the guns (the attack button fires, right click aims) and drawn in the crew's hands as the item
  itself: see "The NT-7 plasma rifle" below. `tools/plasma_rifle.py` writes the item (`tools/plasma_rifle_nt7.py`) and
  the sounds.
- The sounds (r160): Justin's picks from a sound library he subscribes to and is licensed to use, replacing the first,
  synthesised ones. `plasma_rifle.fire` is "Future Weapons 3 - Plasma Hand Gun - Shot 1" cut where he wanted it to stop
  ("I don't like the end of it"): 0.915 s, at the quiet after the shot, a 25 ms fade; and its gun clinks taken out ("it's
  like a shotgun. There's that clink sound. Get rid of that"): it starts at the shot itself (the two clicks before it
  gone), and from the metal clink after it (0.61 s, ringing at 9 and 10.5 kHz) only the plasma's low tail under 1.2 kHz
  is kept. `plasma_bolt.hit` is "Robotic
  Lifeforms 2 - Impact - Energy Cyborg Impact 02", whole. The library files aren't kept in the repository, only the
  sounds made from them: `python3 tools/plasma_rifle.py FOLDER` rebuilds them from a folder holding the two files.
- Tests: `securityShootAHostileMob`, `securityFireBackAtAPlayer`, `plasmaBoltsHitTheCrewUnlessTheCrewFiredThem` (r189).

### The gate guards' plasma baton (Justin, 6 November 2026)

"Give the gate guards plasma batons, make sounds for it ... when it's activated, as well as an animation of it
activating. When not in use they holster it on their side." The design (his take 2): a grip, a thinner rod with a
slightly wider head, the plasma enveloping it as it lights rather than a blade growing out. He used it on "mobs and
players who attack crew", and players get it "creative only". `tower/crew/PlasmaBaton`:

- The look: rebuilt on 10 November 2026 (r203, "The plasma baton rebuilt as real 3D gear" below: the item itself in
  the guards' hands and holster, its plasma drawn as the bolt's is). As first built it was boxes on the props layer
  (`tools/crew_skins.py` `BATON_GRIP`, `BATON_ROD`): a ribbed rubber grip with a lit pommel, a lit collar, a graphite
  rod and a wider head, two plasma sheaths on the glow layer scaled up out of the collar. The gate guards' gear `BATON`
  = 256 is the holster, down the back of the right thigh.
- `TowerCrew.BatonGoal` (whoever wears the holster, `TowerCrew.batoned`): at their target (a hostile mob they see, or a
  player who hurt any of the crew within 24 blocks: the same alert as security's) they get up (awake if they slept)
  and draw it as they go (`ACT_DRAW`, 13 ticks: the hand to the holster, its catch at 4, the baton up to the guard, its
  rod thrown out at 6 (r203), the plasma lighting over ticks 8-13 with the ignition at 7), close in on guard (`ACT_BATON`), and in reach strike
  (`ACT_STRIKE`, 8 ticks: up over the shoulder, down hard; the swing's sound, the blow at 4: 7 damage, `mobAttack`, a
  purple flash, sparks and a wisp of smoke, the sear's sound), 12 ticks between strikes. When the target's dead, let go
  or unseen for five seconds they put it away (`ACT_HOLSTER`, 12 ticks: the power-down; the plasma out over 4, the hand
  to the holster, in at 8) and go back to work. No greeting while it's out.
- Client: each client times what a crew member's doing from when it saw it start (`BatonHum.track`), which drives the
  arms (`TowerCrewModel.batonPose`), the baton in hand or holstered and how far the plasma's out (`TowerCrewModel.baton`).
  The hum (`client/BatonHum`): a looping sound that comes in as the ignition's sound ends (22 ticks into the draw) at its
  level and eases to a quiet idle (-16 dB) over 12 ticks, follows them, and stops the moment the baton goes out.
- `PlasmaBatonItem`: in the creative menu (Combat), not craftable; always lit; a diamond sword's blow (7) and speed, with
  the sear's sound and sparks on a hit.
- The sounds (approved take 12, after 11 takes): from two sounds out of a library Justin subscribes to and is licensed to
  use ("Energy Swords": Scifi_Whoosh_Short ...-24 and ...-15), not kept in the repository; `python3
  tools/plasma_baton.py FOLDER` rebuilds them sample for sample. `plasma_baton.ignite`: -24 itself. `.hum`: a seamless
  2.4 s loop stretched out of -24's held part (WSOLA grains), its air above 2.5 kHz eased down. `.off`: the hum failing
  (slowing, sputtering, arcs building), then a hard stop on a discharge snap and a pop, a few dying sparks, the holster's
  catch. `.swing`: the held part Doppler-swept with a whoosh of air. `.hit`: -15 (its swell trimmed so it lands on
  contact) with superheated plasma going into flesh (a wet tearing crunch, a sputtering flash of steam, the "popcorn"
  bursts of steam pockets rupturing tissue, a few char crackles). `.catch`: the holster's catch.
- How the sounds were tuned without hearing them: a hearing model (MoSQITo: ISO 532-1 loudness, DIN 45692 sharpness,
  roughness) calibrated against his verdicts on each take, then against his own sound (its weight at 40-160 Hz, smooth
  air above 2.5 kHz, little at 600 Hz-2.5 kHz).
- Tests: `gateGuardDrawsTheBatonAndPutsItAway`, `gateGuardGoesForAPlayerWhoHurtsTheCrew`, `plasmaBatonHitsLikeASword`.

### Plasma wounds (Justin, 6 November 2026; the sheet's take 2 approved)

(Since r196 a swing's wound is the fan its edge swept, parts cut right through come off and burn, and a cut can kill
outright: see "Plasma cuts follow the weapon; what's cut off falls and burns; what kills outright".)

"Wherever it hits, it takes a chunk out of the mob ... a hole through it ... if it's a really bulky mob, then it puts a
hole in there, but maybe not all the way through ... glowing, like it's hot, it's like seared"; take 1 (flat decals)
was "a PNG": "I want it to be like 3D ... you can also see through it to ... what's behind the zombie"; and "make sure
it's applicable to the plasma baton too". For any living thing, from any mod, made later too. `tower/crew/PlasmaWounds`
(server) and `client/PlasmaWoundsClient`:

- Server: where a bolt hits (`PlasmaBolt.onHitEntity`) or a baton blow lands (`TowerCrew.BatonGoal`,
  `PlasmaBatonItem.hurtEnemy`) it works out the entry point on the hit one's box and the way the plasma went, in its own
  frame (from its feet, its body's yaw taken out), and whether it burns through (under 0.8 blocks along the shot: a zombie,
  a skeleton, a player) or leaves a crater (thicker: an iron golem, a ravager); a baton blow leaves a slash; one small packet
  (`plasma_wound`) to the players who can see it. Only for show: the bolt still stops at the first thing it hits.
- Client: each wound is fixed, the first time its mob is drawn, to the model part under the hit (so it moves with that
  head, arm or leg). From then on every box of that part (and of the parts on it, a hat) that the channel pierces is
  drawn cut: a mixin on `ModelPart.Cube.compile` (fed the cuts by a wrap round `ModelFeatureRenderer.prepareModel`, from
  the render state) redraws those faces in half-pixel cells with the ones in the hole left out, so it's open and you see
  what's behind. A layer on every living mob's renderer (`LivingEntityRenderLayerRegistrationCallback`) draws the
  channel's stepped walls, a crater's bottom, and the rim and char round each mouth (0.09 px off the surface), with their
  glow on the eyes layer: white-hot when fresh, orange by 2 s, embers by 6 s, charred from about 20 s; a wisp of smoke for
  the first 3 s. Up to 8 a mob (the oldest go first); they last until it's gone (dead, unloaded). Mobs drawn some other
  way than from model parts get nothing.
- Sizes: a bolt's hole 2 px in radius; a bolt's crater 2.4 px in radius, 5 deep.
- The baton's slash (r164; Justin: "it shouldn't just look straight up like the plasma rifle's ... the attack is different
  looking", then "make it three-dimensional, I don't like PNGs"): a trench cut 3.5 px in where the rod struck, 7 px long
  and 2 px wide, lying across the blow (the rod's line, level and square to it) tilted 15-45 degrees either way so blows
  cross; its walls shaded as the body is, hot at the top, darker to a dull-red floor; along its edge a raised lip of
  little blocks (0.5 px across, 0.3-0.6 px proud), the flesh pushed up and blistered, molten then charred; only a thin
  scorch past it. (A hole's shape is a capsule in the client's cut: a circle when its length is 0.)
- Test: `plasmaBurnsThroughThinAndCratersBulky`. Checked offline: the client's placing, cutting and walls run on a
  zombie-shaped model and drawn (through, crater, fresh and cooled).
- Remade in r169 (a bolt goes right through anything, through every part and layer in its way; no more craters): see
  "The drop ship finished, crew paths, wounds right through, and a faster tower" below.

### The crew drop ship (Justin, 6 November 2026; the sheet's take 4 and the guns' close-ups approved)

"I don't want the people to just randomly spawn to the tower. I want you to make a large drop ship"; then, after takes 1-3
("it kind of just looks like a tin can"; "not a straight up box of a ship ... angular, like the wings"): the ramp and two
VTOL thrusters at the back, a door each side that opens with a gun to shoot out of, a turret on top, lasers on the small
front wings "for strafing runs" later, and a turret inside the bay for anything behind; big enough for the whole crew,
the pilots and the gunners. An original Narv-Tech design (Justin's reference pictures were of another game's ship: their
layout and the angular style, not their shapes). `tower/crew/CrewShip`, `CrewShipFlight`, `CrewShipSite`,
`CrewShipLights`; `client/CrewShipRenderer`, `CrewShipMesh`, `CrewShipSounds`; `tools/crew_ship.py`, `tools/crew_ship_sounds.py`.

- When it comes (`TowerCrewCare`): new hands and replacements no longer appear at their posts. A ship comes on the half
  hour (every 36000 ticks) if 5 or more posts are waiting, or a whole job's open posts (all the janitors), or the
  manager; a new tower's (or a new tier's) never-staffed posts get a ship 1200 ticks after they open. One ship per tower
  at a time; it brings everyone waiting, security at the head and the tail of the file. With no player within 160 blocks
  of the core there is no ship: posts fill as before (at once for a new post, 6000 ticks after one falls empty), so the
  crew are at their posts when someone comes back. `/crewship` (operators) sends one to the nearest tower now, with
  whoever it's waiting for (maybe nobody: a fly-in to watch).
- Where it lands (`CrewShipSite`): the nearest spot to the gates (core + (0, 1, -56)) within 100 blocks, rings outward,
  in front of the gates first; the ship stern to the gates. Its footprint and ramp (33 x 59 blocks, sampled every 2.5) must
  be loaded, clear of the tower, flat within a block, without leaves (the two heightmaps agree) or water, and within 8 of
  the gates' level. Nowhere: it hovers off the tier-1 roof's south edge (origin core + (10, 20.7, 73.3), facing south)
  and runs the ramp out over the glass rail onto the deck (the leaf's end on the deck, clearing the rail); the crew make
  for the south pillar's maintenance door and its caged ladder. No roof yet: hired as before.
- The visit (`CrewShipFlight`, worked out alike on the server and every client from the phase and when it began; the
  entity itself stands on the landing point, so players near the tower see it all): in from 700 blocks out and up to 300
  high beyond the landing point, nose down, wrapped in fire (10 s); braking, the thrusters swinging down (7 s); a hover in
  that turns it round so the stern faces the gates, gear and side doors coming down and open, floodlights on, guns on
  watch (4 s); down onto its gear (2.5 s); the stern door swings up and out (an awning, its lamps under it), the platform
  slides out from under the deck, the leaf folded on it unfolds up, over and down to the ground (4 s); the crew; then all
  in again, a lift to 12 blocks and away nose first, climbing out of sight (15 s).
- The crew (`TowerCrew.ArriveGoal`): each comes off 30 ticks after the last (not while a hostile mob is within 14 blocks of
  the ramp's foot), hired in the bay, and is walked along the deck, the platform and down the leaf (the ship is drawn, not
  solid), then walks to the gates (or the roof door), then to work; for 4800 ticks after they aren't put back for being far
  from their post. Escort (`TowerCrew.EscortGoal`): security in the batch stand guard at the ramp's foot once off; if none
  came, two of the tower's security walk out to stand there. They face out, rifles ready, until the last is off, then walk
  back in. If they're stuck the usual putting-back still applies.
- Guns: a twin plasma gun on a swing arm in each side door (out past the sponson when the doors open), a turret on the roof
  ridge, one hanging in the bay over the ramp (only with the ramp down). From the hover until it leaves each takes the
  nearest target it can see within 32 blocks on its side (the door guns: their own side; the top turret only past 18
  blocks, where the hull isn't in the way; the bay's only behind the stern), turns to it and fires a plasma bolt every 15
  ticks (the rifle's bolt: 10, and the holes). Targets: hostile mobs, and players as below; never the crew or tamed animals.
  The aim is sent to the clients.
- Light (`CrewShipLights`): from the hover until the lift, invisible light blocks (level 15) in the air under each lamp's
  beam, in the bay, at the ramp's foot and every 6 blocks over the landing zone, the ramp and its approach (on the ground,
  or the deck); only where there was air; all taken away as it lifts, and any left behind (a crash, a lost chunk) cleared
  when the server starts (a ship still down puts its own back).
- The ship (`tools/crew_ship.py`, from the approved mockup's geometry; `assets/jugcraft/crew_ship/ship.bin.gz`, 2768 quads in
  20 parts, 19 materials; textures at 32 texels to the block laid on each face at its true size): 43 blocks long (56 with the ramp down), 32 across the wings, 13 high on its gear; a
  faceted lofted hull with a ridge, a sponson out of each flank with its sliding door, thick cranked wings with twin laser
  pods, the troop box stepped up behind a narrower cockpit with a small raked windscreen, two heavy VTOL thrusters on pylons
  (flat to straight down), 42 jump seats (the full crew is 37; 2 pilots and 4 gunners aboard, not shown). Drawn with every
  corner lit by the world's light where it is (a grid sampled round the ship every 8 ticks), lights full bright; the
  exhaust, the floodlights' beams and the fire of entry drawn translucent and full bright.
- Sound (`tools/crew_ship_sounds.py`; NASA launch recordings, not copyrighted, credit: NASA, from
  https://www.nasa.gov/historical-sounds/ : STS-131's "Sound of Launch" and an Atlas V's launch): the roar coming in
  (13 s), the thrusters' burn (a loop, swelling and easing with their thrust), a low idle loop in the hover and on the ground
  (-27 LUFS, quieter in game too), the lift-off (16 s); each follows the ship where it's drawn and falls off with distance
  (128 blocks for the roar, 32 for the idle). The ramp: the piston's sound, lower. Justin left out NASA's SLS test fire
  ("horribly loud").
- The keep-out ring (Justin, before playing r165: "that circle thing that goes around spinning with the letters on it ...
  hologrammy ... stay back ... black letters, red circle around the ship ... if they come within like 10 blocks of the
  ship, it opens fire on them"; then "float at like a second block level, and like go in a circle ... so people can see it
  ... instead of them just not paying attention on the ground"): from the touchdown until it closes up (and always on a
  landed one), an ellipse round the ship's origin, half-widths 30 across and 40 along since r178 (Justin saw loot left
  just outside it: "Maybe expand the radius"; 26 and 35.5 before, 10 blocks past the wings, nose and ramp's foot; 38.5
  along, 3 aft, until the ramp's hinge was put right on 6 November), drawn as a standing band floating from 1 to 2 blocks over the landing ground (the
  second block, at eye level), translucent glowing red with STAY BACK in black along it, readable from outside and from
  inside, turning round the ship, flickering a little (`CrewShipRenderer.zone`, `textures/entity/crew_ship/zone_ring.png`). A player inside it (lower than
  12 blocks over it) is a target for the guns. While it is down the guns fire at nothing outside the ring, mobs and
  players who hurt the crew included (r178, his pick "Bigger ring, guns stay inside"; `CrewShip.canShoot`). Where the ship sets down right in front of the gates the ring can reach the
  gates.
- Hurting the crew (Justin: "if I shoot any of the Narv-Tech people, the ship becomes hostile to me and it'll kill me"): a
  player who hurts any crew member is a target for every drop ship within 128 blocks for 1200 ticks, inside the ring or not
  (`CrewShip.alert`, from `TowerCrew.hurtServer`). A ship's bolt hits a player only if it was fired at them. Never players
  in creative or spectator (as the crew).
- Drop Ship (Landed) (Justin: "put in a landed version of the ship ... make it active so it'll shoot mobs"): a creative
  menu item (Combat). Used on the ground it sets a ship down beyond that spot to stay, the ramp's foot there and the stern
  toward you: down, open, lit, its ring out, its guns on guard (phase PARKED, never leaving; it isn't any tower's ship). Used
  while sneaking it takes away the nearest one set down within 64 blocks. `CrewShipItem`.
- Fixes from Justin's first look in game (6 November 2026, seven screenshots):
  - "the spotlights, you just have black lights here": the beams (and the exhaust and the fire of entry) were drawn with
    the eyes render type as if it added light, but in this game it blends: their colours, darkened toward their ends at
    full opacity, came out black. Now light colours at an opacity that fades (`colour`): pale cones of light.
  - "when it lands, have lights that go outward, so they point in all directions, so people can see": ten more
    floodlights, each in its own housing: the two nose lamps ahead, two on each sponson's flank out and fore and aft, one
    under each wing tip out and ahead, one on each thruster pylon's cap out and astern (`CrewShip.LAMPS`, 17). Their beams
    reach the ground or 34 blocks (`BEAM_REACH`), and their light blocks go at each pool, round it and on the way out.
  - "the flames ... they're not actual flames ... It needs to be, like actual thrusters": the exhaust is a rocket's fire,
    a white-hot core in a yellow flame in an orange one, blue heat round the lip, fire streaking down it
    (`textures/entity/crew_ship/flame.png`, slid down the plume) and flickering in length; idling only a short glow. Flame
    and smoke stream out of the nozzles and, low over the ground, the blast throws dust and smoke out in a ring
    (`client/CrewShipEffects`).
  - "when I fly into the ship, I'm just flying through it": a landed ship is solid (`CrewShipHull`,
    `mixin/CrewShipCollisionMixin`): its shell in quarter-block cells from the exporter (`data/jugcraft/crew_ship/
    collision.bin.gz`, the hull, the gear, the thrusters down, the stern door up, the platform out), turned into boxes in
    the world once per landing on both sides, added where an entity's move gathers collision boxes; the leaf a ramp of
    quarter-block steps to walk up into the bay (which is open inside). Projectiles pass.
  - "the texture, it looks super stretched across ... the texture in general just looks kind of shitty": every face now
    carries its texture at its true size, flat on its own plane (`world_uv`), at 32 texels to the block on the hull; new
    textures: plating in panels of different sizes with recessed seams, rivet rows, a shade between panels, scuffs and a
    little grime; armour plates bolted at the corners; louvres; brushed steel; tinted glass; tread plate on the deck and
    ramp; worn hazard stripes; the name sharper and only its letters.
  - "this green piece right here ... what the fuck is that doing there?" and "a red piece that's off": the nav lights stood
    off the cockpit's flanks. Now on the wing tips (red port, green starboard), each with a white strobe aft of it.
  - "that little striped black box thing ... not lined up well": the vent behind the cockpit is flush on the step now.
  - "This thing on top of here, by this antenna": the windscreen's brow was a bar standing off the roof's sides; now a trim
    strip following the roof, and the sensor a raked fairing on the ridge with its antenna.
  - "On top of the thruster ... you can see through part of it": the intake's duct had no wall between its lip and the
    fan (and the nozzle none between its lip and the core); both closed.
  - "on the underside ... I can see into the belly of the ship through there": the hull's lowest facet was left out
    beside each side door; closed. A check now draws the export from twelve sides with every face's back in magenta:
    none shows.
  - Found on the way: the crew's way off the ramp, the escorts, the landing site's check and Drop Ship (Landed) went by a
    hinge six blocks aft of where the drawn leaf hangs; all by `CrewShipFlight.HINGE` (the stern) now.
- Tests: `dropShipFliesForFiveAWholeStaffOrTheManager`, `dropShipComesInLandsAndLeaves`,
  `dropShipLightsTheGroundAndItsGunsShoot`, `parkedDropShipShootsInsideItsRingAndWhoeverHurtsTheCrew`, `landedDropShipIsSolid`. Checked offline: the exported mesh read back and drawn posed as the game poses it.
  Not covered by a test: the site search over real terrain, and the crew's walk off (both need a built Tri-Pillar).

## Night lighting (Justin, 5 November 2026)

He chose "Night lighting" and approved the night lighting sheet as shown. The tower's ceiling lights (the Ceiling Light
Panel and the Sealed Linear Ceiling Light, now `TowerLightBlock`) have a `mode`: on (light 15), night (12), low (10) or
off (0). The diffuser glows at the mode's light; off, it is an unlit, greyer diffuser (`tower_fixture_opal_off`).
`tools/tower_night_lights.py` writes the lamps' mode models and blockstates (run at the end of
`tower_realism_runtime.write_assets`) and the layout's `night_lights` (494 of the 549 lamps on the two crew floors, each
with its night mode; `tower_tripillar.export`):

- **On**: within 7 blocks of a night-watch post's chair or spots, in that post's room (the console, the front desk and
  gates, dispatch, the security office, first aid, simulator A). Rounds (the hall patrol, the data hall and pad rounds,
  the north janitor) don't light their whole round.
- **Night**: the ground floor's hall, and rooms a round or a watch goes through (the data hall).
- **Low**: in the hall within 6 blocks of a sleep spot (the lobby lounge); big rooms someone sleeps in (the cafe, the
  upstairs lounge and hall).
- **Off**: small rooms someone sleeps in (the crew rest room with its bunk, the manager's office) and every room nobody
  is in at night (offices, briefing, boardroom, lab, the locker room and so on).
- **Outdoors, never switched** (r174): the five downlights over the apron under the intake face's overhang were going off
  with the rooms nobody is in, leaving the apron dark enough for mobs at night (the lighting test found it). A lamp over a
  floor that opens onto the sky stays on all night.

The Tower Core switches them (`TowerNightLights`): at 20:00, once the last of the off-shift crew have gone off, to their
night modes; at 05:00, as they get up, back on; 48 a tick, so under a second for all 494, then nothing until the next
change. It checks the clock every second, so `/time set` takes effect too; it saves which way the lamps are
(`lights_night`), and a switch cut short by a save runs again from the start (each lamp already right costs a check).
Only the tower's lamps at the listed places are touched: one a player broke and put back is on until the next switch,
and lamps placed anywhere else are always on. A refit puts the lamps back on. The neon lines, the screens and video
walls, the holo table, the crystal tree, the exit signs and the hangar levels above stay as they are.

## Mobs kept out of the tower, and two fixes (Justin, 5 November 2026)

- "theres a lot of mobs spawning inside the tower, make it so mobs do not spawn on/in the tower": no mob spawns
  naturally in or on a Drone Tower (`mixin/TowerSpawnMixin` on `NaturalSpawner.isValidPositionForMob`, asking
  `TowerRegistry.inTower`: inside the tower's footprint from its floor to three blocks over its roof); when a tower is
  refitted, stray mobs inside it are cleared (`TowerCoreBlockEntity.clearStrays`: not pets, named, leashed or riding
  mobs). The crew are spawned by the tower, so they are not affected.
- Since 6 November 2026 (Justin: "it says no tower with a crew near ... Is it just not detecting the one that's already
  placed?"): a tower only counted once it was in its dimension's `TowerRegistry`, and a core got there only when a player
  placed it by hand, so a tower set any other way (by a command, or in a world from before the list, as in Justin's test
  world) had no crew, no spawn guard and no /crewship. Every core now lists itself when it loads (`TowerRegistry.track`,
  under its owner or under "nobody" until it has one; `claim` goes through it, so a core is only ever under one name).
  /crewship says which it is: no tower found, or a tower still being refitted.
- Mobs (Justin, the same day, after a first try that swept every mob out every 2 seconds: "I want to be able to manually
  spawn mobs in here ... use spawn eggs to test out security's working ... I want them to be able to walk into the tower
  from the outside ... I shouldn't like just delete any mob that walks in there ... why don't cows spawn in underground
  caves? ... do that type of thing for inside the tower and on the tower"): nothing spawns in or on a tower of itself, as
  nothing spawns where its rules don't allow (`TowerSpawnMixin`, which works now that every tower is listed), and that
  is all: a mob that walks in through the doors, or comes from a spawn egg, stays for the crew to deal with. Only a refit
  still clears the wild strays left from before (`clearStrays`).
- The security room: its Control Screen goes up a block (y 3-4, clear of the monitors), and the single-block desks are
  gone (the glass desk row, the office desk and their chairs).
- Upstairs, the length of wall west of the simulators that went nowhere (x -17..-13 at z 22) is gone.

## The crew get about properly, the detail outside, and live clocks (Justin, 6 November 2026)

- Teleporting ("I see the people like teleporting around a lot ... Maybe you need to do pre-scripted pathfinding. I
  don't really care how you do it. Just fix that") and the guard "bouncing up and down in front of a window": the game's
  path finder took any block that isn't a whole cube as a place to walk unless it says otherwise, and the mod's pieces
  never said so, so walks went into Framed Glass (a thin pane at the back of its cell), desks, shelves and parts of big
  pieces; the walker bumped and hopped against them (the game makes a mob jump inside a cell with a block in it), never
  arrived, and the "stuck" timer teleported them back. Now any piece of the mod taller than a step and starting low in
  its cell is in the way for every walker (`tower/crew/CrewPaths.obstacle`, `mixin/PiecePathMixin`); floor tiles and
  emitters are still walked over, ceiling fittings walked under. Also: a crew member more than 48 blocks from where they
  were headed (the hall patrol on the far side of its round, anyone on the way to a break in the cafe) was teleported
  there, now only one truly lost (160 blocks) is put back; walks can be planned 96 blocks and twice as hard; a spot that
  can't be reached is given up for the next one; and nobody is ever put back where a player within 64 blocks could
  see it (`TowerCrew.seen`) — they keep walking it.
- The service bay's lifted cradle ("make the drone platform collidable. Don't be able to walk through this little drone
  platform"): it reaches out over the cells round its stand, and the game only gathered such big shapes from the cells
  straight across from a mover, so its corners were soft; a service stand now counts from the diagonal cells too
  (`mixin/LargeShapeEdgeMixin`), and the crew walk round it (`CrewPaths.Navigation`). The mechanic's inspection spot that
  was in under its deck moved off its south west corner (23, 1, -28).
- More to say ("they say just a coffee, then back to it. Maybe give them more lines than just that"): eight lines a job,
  twelve on a break, six asleep, and never the same line twice running (`TowerCrew.LINES`, `BREAK_LINES`, `ASLEEP_LINES`).
- A three-day rota (a "schedule over like 3 days"): each crew member's breaks are on a three-day cycle, one a day and a
  second on two days of the three, at other times each day (`tools/tripillar_crew.py` CYCLE, `plan_day`;
  `CrewPost.CYCLE`); partners still never away together, no seat booked twice.
- The security detail ("I want to see a security detail or patrolling outside the perimeter, as well as the top floor of
  the tier 1 area ... the big wide open area ... add in a couple more"): four more security officers, eight in all. Two
  walk the ground round the control building the opposite ways (posts `perimeter_east`, `perimeter_west`, kind
  `perimeter`: their spots are on the world's ground, at its height there), two the Roof Pads deck (`roof_north`,
  `roof_south`). They keep the watch day and night with no breaks, and shoot hostiles as security do. Four new faces in
  security's pool (nine).
- The clocks ("it's just saying 12. It needs to be the actual in-game time. And the same for those digital clocks
  too"): every clock in the tower reads the world's clock, which the nuclear showcase had stopped at noon in the test
  world. The showcase no longer stops it, and a world with a `jugcraft/clock-live.todo` file in it gets its clock going
  again when joined (`nuclear/PlantShowcase`; 26-fresh-plant-world.bat leaves one in both test worlds).
- Tests: `crewPostsAreSound` (41 posts, eight in security, two outside), `crewRoutinesAreSound` (sixteen on the night
  watch, five breaks over the rota), `piecesAreInTheWayForWalkers`, `serviceCradleIsSolidAtItsCorners`.

## The drop ship finished, crew paths, wounds right through, and a faster tower (Justin, 6 November 2026)

- The ramp ("it unfolds through itself"): the ramp leaf now slides straight out of the hull along its rails first, lifted
  clear of the sill, and only tips down once it's out (`CrewShipFlight` pose `leafSlide`, `leafLift`; `tools/crew_ship.py`
  `ramp()`), so it never passes through the hull.
- The thrusters ("I want it to look like a much more realistic thruster"; he picked B, the rocket plume): three nested
  plumes, a long faint outer plume, the orange flame and a short white-hot core, each coloured along its length and
  scrolling, more smoke off the flame's end and a ring of dust where it hits the ground (`CrewShipRenderer`
  `FLAME_LAYERS`, `CrewShipEffects`).
- Its sounds ("it didn't play any of the sounds from the rocket ... I want you to be able to hear it when it's coming in
  from a distance"): the engine loops start silent and swell, and the game dropped any sound that starts at nothing; they
  now may (`CrewShipSounds` `canStartSilent`). The approach and lift-off carry 800 blocks, the thrust 320.
- Lights ("at nighttime, it needs to have the lights on already"): at night the floodlights are on all the way in and out.
- Its guns ("not killing some of the mobs that are really tough ... the rotary cannons ... would shoot way faster"): the
  door guns hit for 14 a bolt every 2 ticks, the cannons 60 every 10, and a hit no longer waits out the mob's moment of
  grace after the last one (`CrewShip.GUN_COOLDOWN`, `GUN_DAMAGE`; `PlasmaBolt.damage`).
- Collision ("there's still no collision on the ship"): the hull is solid while it flies too, to the metre (a coarse
  hull of one-block boxes, turned with the ship in 3 degree steps, only when a player is within 96 blocks), and to the
  pixel once it's down, as before (`CrewShipHull`). The crew path round a landed ship (`CrewPaths.Navigation`).
- The crew ("one of the dudes is sitting there just jumping up and down outside of the pass-through thing ... two guys
  out here that are just like walking into each other"): the crew walk through the speed gates, which open for them
  either way (`CrewPaths` gate lanes, `nuclear/SpeedGateBlockEntity`); a walker that hops twice on the spot stops trying
  that way for five seconds and plans another (`TowerCrew.jumpFromGround`); nobody takes a short cut over the furniture
  (`CrewPaths.onAPiece`); two meeting head on each keep right (`GiveWayGoal`), and a spot already taken is passed over.
  The perimeter guards are only ever put back at their post if lost and out of everyone's sight.
- Plasma wounds remade ("it's not shown through the clothes ... there should be just a hole in its head ... even at an
  angle, it should be missing stuff ... I want this to be able to apply to any mob ... it stretched the texture way out
  ... that long cylinder going through it"; "theres no hole through creepers"): a bolt's wound is now a tunnel right
  through the mob along the shot's own line, however thick, through every part in its way (a pillager's nose and head,
  a body and the arm behind it), cut the same in every layer drawn over those parts (a drowned's outer skin, armour, a
  warden's glowing spots), an oval or a groove when the shot is angled or grazing; each face's own texture is laid on
  the cut cells from its corners, never stretched; the tunnel's walls are clipped to the inside of each box, so nothing
  shows outside the mob. Works on any mob drawn from model parts (creepers, wardens too). The baton's gash is 2.5 px
  deep (3.5 broke through a zombie's 4 px back), its blistered lip kept. All of it is worked out once a wound and kept; a frame only places and
  colours it (about a quarter of a millisecond a wounded mob) (`client/PlasmaWoundsClient`, `tower/crew/PlasmaWounds`).
  Checked offline on vanilla's own models: `plasma_wounds_check.png` with r169.
- Frame rate ("Build pieces once", his pick after the recording: the tower ran at 27 a second, its drawing spent mostly
  putting the same pieces together every frame): the drones on the stands, in the display cases, on the coffee table and
  at the depots are worked out once a part and kept (normals, colours, the golden tint), their triangles paired into
  quads wherever two share an edge (40% fewer corners, the very same triangles); and every piece drawn through the
  shared drawing helper (the trees, shelving, consoles, cradle struts, video walls, the hologram's housing, the drones)
  is moved into place with the pose joined once rather than for every corner and handed to the buffer whole, without the
  game's per-corner helpers that make two new vectors a corner (`DroneDepotRenderer.Geometry`, `DroneModel.Ready`). The
  drop ship's hull is drawn the same way. Offline, a drone draws in 45% of the time it took. Moving parts (rotors, the
  trees' pulses and sway, the hologram) still animate every frame.
- The hologram's drawing (r170; his pick): its 140,000 corners go to the buffer the same fast way, each placed by the pose in
  one step, its one normal worked out once (`HoloProjectorRenderer.emit`). It looks exactly the same and is still
  rebuilt 30 times a second; only the waste is gone (checked offline: the same corners to the last bit).
- Tests: `crewWalkThroughSpeedGatesAndNotOverFurniture`, `plasmaBurnsRightThrough`, the ship's pose test (the leaf slides
  out level, never turns up).
- Fixes from his focus run (r171; his 13 run on r168 failed 58 server tests, 43 already failing on 28 October; the
  rest were this work's, run here on a local game test server until they passed):
  - A crash: a crew member planning a way over a block of the mod with nothing to stand on (a lamp) threw and took the
    server down (`CrewPaths.onAPiece` now passes such blocks over).
  - Giving way never stepped aside: the cell beside counted as "near enough" for the game's one-block reach, so the
    path was just where they stood; it now goes right onto the cell (`GiveWayGoal.start`).
  - Lithium (in the pack) checks block and entity collisions its own way, round the game's code the cradle's corners
    and the ship's hull hooked into: the same hooks are now made in Lithium's paths too (`LithiumCradleEdgeMixin`,
    `LithiumShipHullMixin`; they do nothing without Lithium).
  - A parked drop ship (its phase after GONE in order) wasn't tracked for its hull, so it was never solid on the ground
    (`CrewShipHull.track`).
  - The tests themselves: the game's in-level mock player is always creative (the crew and the ship leave creative
    players alone), so the tests that need it use a real survival player (`TowerGameTests.survivalPlayer`); a crew
    member set up by a test without a tower core no longer walks off (`TowerCrew.setTestNoCore`, tests only); the posts,
    night lights, baton (armour takes a little off), routine crew's floor, drop ship lights and the hall's mobs checks
    corrected to what's built now; the diorama-case refit hires 41 (the posts as they are now); the video wall client
    check wanted 500 quads but an idle depot's LOGISTICS board draws 491 (FLEET 474, OPERATIONS 818, "NO DEPOT LINK"
    91), so it wants 300.
- The tower tests that had failed since 28 October (r172; "Fix the 19 old failures", his pick; all of TowerGameTests and
  TowerConversionGameTests now pass on the local test server):
  - In the game: the goods lift's two rails had a gap at the roof slab (y 20), where the car runs up into the house
    (`tools/tripillar_stations.py`; the layout's foundation now has them, 2 blocks more). Neon lines lost the chamfer
    onto a line touching them only at a corner (it hangs on a diagonal block, which the game never tells them about):
    once each step is up, every line takes its shape again (`TowerCoreBlockEntity.reshapeNeonLines`). A conversion
    clears the old design silently, so blocks kept from it kept shapes they took from what was cleared beside them: at
    the end every block takes its shape from what is round it now (`reshapeAfterConversion`). What was in a storage
    rack (or any store) the conversion clears comes back on the terminal with the drones that had no dock, kept
    through a save (`keptStores`), instead of being lost.
  - The tests: ten refit tests had a bare `@GameTest` (20 ticks, the empty structure), so they never got to refit;
    `JugcraftTower.BLOCKS` doesn't hold the plant interior's blocks (nuclear/PlantInterior: the sofas, glass coffee
    tables, storage racks, framed glass), so checks on those could never pass (`TowerGameTests.modBlock` reads the game's registry); the restroom stands on
    the west old lounge table's spot (gone means no glass table there, not air); the Lounge TV's live screen (its block
    entity) is on its third cell; the landing door check knew the ground door only by y; the industrial rebuild's
    control building is counted without the cells the roof pads replace; the map table test lays its own ground (a
    floor over sky-lit air is a span on the map); the diorama-case refit hires as with nobody near
    (`TowerCrewCare.hireAtOnce`: another test's player within 160 blocks would have a drop ship bring them) with the
    posts' chunks kept ticking; the big map table client check allows its tilted corner panels (to 0.955 high), and the
    diorama client check looks for the Tower Diorama Case drawing it (r173; it still wanted the hologram table); the
    Tri-Pillar's foundation is checked to y 26 (the lift house), its 18 desks (six rows of three) and its 6 hall fixtures
    (the touch consoles and holo emitters long gone).
- The other eight focus tests (r174; "Fix the other 8", his pick; all failing since 28 October; every focus class now
  passes on the local test server):
  - In the game: the five downlights over the apron under the intake face's overhang went off at night with the rooms
    nobody is in, leaving the apron dark enough for mobs; outdoor lamps now stay on (see Night lighting).
  - The tests, to the tower as it is now: the yard's landing pad (four tier 1 slots) went with the yard's platform
    (layout 40), so tier 1 is its 24 roof docks; the hall's six 3x2 wall screens became the three Curved Video Walls
    (the control building's screens test checks those and the security office's 3x2 screen); the free hall spot for
    the loose desk and console is in the hall's west (the old one is the upper floor's glass); the hall's master is the
    Tower Control Console (the master console test checks it); the lighting test's well-lit hall is a circle round the
    table, as the rotunda is, and by night rooms nobody is in are dark on purpose, so a floor needs light only outside
    the tower's spawn guard; the energy exchange's outside face is the way it faces (east since layout 37), and the
    foreign terminal stands outside it (it was put in the exchange) and goes before the legacy ports are tried; the
    cargo pipes test turns an iron pipe's outlet to the socket, leaves out the pipes that never fill a store from a
    plain chest (sandstone, daizuli, emzuli) and lets a slow pipe take more than one push; the 270-cell shape test
    compares the shapes, not the objects (FerriteCore, in the pack, shares them its own way).
  - His runs: r172 and r173 passed all of TowerGameTests and TowerConversionGameTests, and r173 the client tests too;
    r174 passed everything but `parkedDropShipShootsInsideItsRingAndWhoeverHurtsTheCrew` (its second shot came just
    after its 300 ticks on the busy test server: 600 since r175), and the client tests.

## Frame rate, round 2 (Justin's recording of 7 November 2026, r176)

His recording in the tower (56 s) averaged 32 frames a second (27 before r169), dipping to 14, with the server easy at
5 ms a tick. The render thread spent about 60% of its time putting the tower's decorations together every frame (the
drones on their stands, the crystal and ember trees, the shelving, the consoles, the hologram), and 83% of its garbage
(about 290 MB a second; 17 collections, 3.4 s in all, the dips) came from the game sorting every translucent quad back
to front before each frame. He picked all four fixes; this round has three:

- Glows unsorted (`client/GlowTypes`): the tower's glow layers (the hologram, the command table, the holo and big map
  tables, the hall's screens, the trees' crystals and cracks, the console and video wall glows, the display case and
  coffee table) are drawn in the game's translucent emissive type without its sort: the same pipeline, blending and
  order-independent pipelines, no depth writes, so glows still never hide what's behind them; where two overlap the
  shade can differ a little.
- The hologram rebuilt less (`HoloProjectorRenderer.REBUILD`): 15 times a second while it follows someone (was 30),
  5 times a second when nobody is near and its head has settled, or the camera is more than 40 blocks off.
- Less detail far away:
  - Drones beyond 24 blocks are drawn from their mesh with corners closer than 0.035 blocks run together (beyond 48,
    0.07), the triangles that shrink to nothing left out, the rest paired into quads as before (`DroneModel.coarse`):
    the T1 draws 43% of its corners at the middle distance and 20% far off; the T9 73% and 55%.
  - The console meshes the same way beyond 32 and 48 blocks (`ConsoleMesh.coarse`): the parts shelving 28% then 15%
    of its corners, the Tower Control Console 74% then 61%; the trees' beds and planters little (their quads are big).
  - The crystal trees leave out their veins' pulses and their halos beyond 40 blocks; the ember oaks their halos and
    sparks. The ember oak's crack light is worked out once a quad instead of at every corner (the quads are a pixel or
    two; it looks the same at a quarter of the sums).
- Static parts baked in (r177, his fourth pick; `client/BakedFixtures`): the fixtures' meshes that never move are now
  in the master block's model, built once with the chunk and drawn from its buffers like any block, instead of being
  put together by their renderers every frame: the Drone Parts Shelving (both stocks, some 23,000 and 9,000 quads),
  the Tower Control Console's body, the Cradle Struts, the Crystal Tree's planter, the Ember Oak's bed and bark, the
  Drift Isle's base, the Big Map Table's table. Their masters' render shape is a model now (their block models have no
  elements of their own); their textures are in the block atlas (the shelving's and the console's at half size,
  `*_baked.png`, still 680 and 100 pixels a block). Lit as before: the master's light, each corner's own shade, no
  ambient occlusion, the full-bright ones emissive. What moves or is live stays with the renderers (the shelving's LEDs,
  charger bars, labels and glass; the console's screens and floor edges; the trees' pulses, crystals, halos, sparks and
  the floating island; the map). `-Djugcraft.bakedFixtures=false` puts it all back on the renderers. Check:
  `TowerRenderChecks` (both shelving units' models are the baked ones and put out every quad of their meshes).

## Fixes from Justin's 7 November 2026 playtest (r178)

- The drop ship is solid to walk on at last ("the ship still doesn't have collision. Like, I'm trying to walk on it"):
  the landed ships the hull's collision looks through were kept in one set for both sides, and in singleplayer the
  client's copy of a ship counts as the server's (the game compares entities by their network number), so the client's
  ship was never in it and the player, moved by the client, fell through. Each side keeps its own set now
  (`CrewShipHull.landed`). Check: `TowerClientGameTests` parks a ship and finds its flank solid and its bay open on the
  client.
- The keep-out ring is 30 by 40 blocks across its half-widths (was 26 by 35.5), and while the ship is down its guns fire
  only inside it (above).
- The plasma rifle kills a zombie with one shot: 24 damage, for the crew and players alike (above).
- Plasma wounds on a mob shot many times ("the inside starts to get really fucked up ... some of the holes will just like
  disappear out of like the warden"): a mob keeps up to 40 wounds (8 before; the oldest went first, which is why holes
  vanished); a tunnel's wall leaves out the pieces that fall inside another wound's hole, so crossing tunnels open into
  each other instead of filling with walls, and a tunnel with others near it has 20 sides of even length. Checked
  offline on the warden (14 and 40 wounds) and a zombie (8).
- The crew get up off the sofas in the morning ("whenever people force time of day, they are now just sitting around on
  the couch"): after `/time set day` a crew member still sitting on a sofa from a night break stood up only if their
  post was itself a seat; now anyone whose post isn't a seat gets up and goes to it (`TowerCrew.WorkGoal`). And a walk
  that ends a block short of its spot (the game's paths stop as soon as they are within a block) no longer leaves them
  standing idle there until they're put back minutes later: they take the last step straight in (`walkTo`). Check:
  `stationCrewGetUpOffTheirSleepSeatInTheMorning`.
- Drones through the tower storage ("tell them to insert drones into the tower's inventory. And then whenever somebody
  puts drones inside the tower inventory, it'll drag them to the appropriate drone pad. Also, I want people to be able
  to use any drone for any pad. But the exception here is they can't use a drone tier past the tier of the tower"; his
  pick "Nearest free pad that fits"):
  - The tower storage (the Cargo Exchange's way in, by hand or by pipe) takes drone items up to the tower's tier, as
    many as the fleet has room for (`TowerCoreBlockEntity.droneRoom`); every 2 seconds the core links each one waiting
    there to the fleet and takes it out of storage (`linkWaitingDrones`, `DroneTerminalBlockEntity.linkFromStorage`).
  - Any drone takes any pad big enough for it (small, medium, large), the lowest floor first and then the one nearest
    the middle; the pads no longer go by their tier (`towerSpots`). A drone past the tower's tier is still refused, by
    the storage and by a pad.
  - The fleet tab says so when there are no drones: "PUT DRONES IN THE TOWER STORAGE AT THE CARGO EXCHANGE (UP TO TIER
    N); THEY GO TO A FREE PAD THEMSELVES".
  - Checks: `dronesPutInTheTowerStorageGoToTheirPads`, `anyDroneTakesTheLowestFreeDockThatFits`,
    `towerTierGatesDroneTiers`, `finishedTowerHoldsItsWholeFleet`.

## The drop ship reworked (Justin, 7–8 November 2026; r179)

Justin went over the ship in close-ups, one area at a time ("The long rod things and the mount they're on still don't
look right, I don't like textures that look stretched either"), and approved every pass; then picked "Build, with
distance cutoff". The mockup generator `tools/crew_ship.py` is the approved design, exported straight into the game.

- Round parts (thruster pivots, ducts, booms, barrels) are paneled, and the panel lines run on from the skin beside
  them: a round part's texture is unrolled along it or laid out from the hull's own, never stretched round it. No
  service labels on small round parts.
- Openings are cut through the skin, not stuck on: the vents, flare bays, chin and wing lamps sit in holes the hull is
  cut around (several holes to a span), with the skin's shading carried across the cut so no lighter band shows.
- The stern: the rear platform and the stern door are gone; one ramp, 7.5 long, hinged low at the back of the bay,
  lowers straight to the ground and is walked on (the hull's collision and the crew's way off follow its slope).
- Two more guns, under the wings, hung from pylons: each covers its own side behind the bay (40 damage, every 8
  ticks). The bay gun moved forward to the new bay.
- 17 lamps from the design (nav, flood, ramp, chin and wing lamps), lit at night as before.
- Frame rate: the model has 17.7k faces near (12.7k before). Past 48 blocks a simpler copy is drawn (9.0k: fewer sides
  on round parts, no bolts), and the inside of the bay and flight deck only within 40 blocks. Each corner carries its
  own normal now (the file's second format), so curved parts shade smoothly.
- Glass and the HUD lenses draw see-through; the rest cut out.
- Saves from before keep their ships; the ramp angle is read in the new range.
- Checks: `dropShipComesInLandsAndLeaves` (the ramp lowers to the ground and comes back up), `landedDropShipIsSolid`
  (the hull and ramp are solid, the bay open). The exported model was rendered offline from the game's file and
  matched the approved sheets. Not yet seen in the game.

## A frame rate check that runs itself (Justin, 8 November 2026; r180)

"I'm not at my pc, do a check for FPS on your own in a way that I don't have to control it myself": `28-fps-check.bat`
(beside the other bats) copies "Admin Review 6" to a throwaway world `JugcraftFpsCheck`, applies `sync.zip`, builds,
and starts the game straight into the copy with the check armed (`client/FpsCheck`, views from `client/FpsCheckPlan`).
The check is dormant unless the game folder holds `jugcraft-fps-check.txt` when it starts (the bat writes it; it is
deleted as soon as it is read), and it runs in no world but `JugcraftFpsCheck`.

- Once the world is in, it turns the frame cap and vsync off (his settings are put back before the game closes),
  keeps the game from dropping to its idle frame rate or pausing, and maximizes the window on the main screen.
- It finds the tower nearest the player and puts the camera, in spectator, at each view: the tower from outside and
  from the gates, one view of each big fixture (the holo projector, the parts shelving, the control console, the
  crystal tree and ember oak, the big map table, the command table, the video wall), and a drop ship parked where one
  would land: 22 blocks off (its bay drawn), 44 (between the two cut-offs), 70 (the plainer model) and inside the bay,
  each first without the ship and then with it; then the overview with the ship, and the ship and the lobby at night.
- At each view it waits for the chunks to finish building, times every frame for 8 seconds (the time between frames,
  and the game's own work time for each), notes the server's tick time and takes a screenshot; at the overview, the
  lobby, the near ship and the bay it also makes the game's own 10-second profile (the F3+L report). A Java Flight
  Recorder recording covers the whole run.
- Results: `fps-check\` beside the bats (`report.txt`, `frames.csv`, `views.txt`, `profile.jfr`, the screenshots,
  the profiles and the game's log); then the game closes, and the bat deletes the copy.
- The view planning was checked offline against a built tier 9 tower (every fixture but the display case got a clear
  view).

### Frame rate, round 3: the first run (8 November 2026, r180, on his PC)

RTX 5080, 32 threads, render distance 32, frame cap and vsync off for the run; the window stayed at 854 x 480 (the
maximize call did nothing), which matters little because every view was limited by the render thread, not the
graphics card (about 11% busy). Medians:

- Inside the tower: 51 to 64 frames a second (15.1 to 19.0 ms a frame), 1% lows 30 to 41. The game's own profile of
  the lobby: 61% of each frame building and 16% gathering the block entities' drawing (the mod's fixtures putting
  their shapes together again every frame), 10% uploading that to the graphics card; terrain under 2%. By renderer
  (share of the render thread, lobby, console, crystal tree and shelving views): crystal trees 14%, hall fixtures 8%,
  ember oak and drift isle 5.5%, video wall 4.4%, diorama 4.0%, holo projector 3.8%, iris doors 3.5%, TVs 3.3%, display
  case 2.6%, holo map 2.3%, animals 3.6%, live panels 2.2%, room-kit displays 2.0%; the game itself about 25%.
- Outside: the whole tower from 160 blocks 460 frames a second; from the gates 77 (the inside shows through the glass).
- The drop ship (r179): 2.5 ms a frame 22 blocks off (99 to 80 frames a second at that view), 2.2 ms at 44, 1.5 ms at
  70 (the plainer model), 1.1 ms from 95 blocks, 2.6 ms from inside the bay; nearly all of it the 17,700 faces (9,000
  far) sent again every frame. Night made no difference.
- The server: 3.5 to 4.5 ms a tick throughout. Garbage collection: the longest pause 25 ms.
- The ship drew right in the game (its first look there): bay, ramp, thrusters, far model.

## Fixes from Justin's 8 November 2026 playtest, part 1 (r181)

His notes (tower and drop ship, with pictures) are worked through a few at a time; he picked the tower's quick fixes
first, then the trees one at a time, then the ship.

- The hologram head ("whatever you did to the hologram of this head, you fucked the textures up"): since r176 its glow
  drew unsorted (to spare the game's sort every frame), so the inside of the head, the mouth and the hair behind came
  out on top of the face. Its quads are put in order again, farthest from the camera first, once a build (15 times a
  second, 5 when idle or far) and without garbage (`HoloProjectorRenderer.backToFront`), so the face draws over what
  is behind it as before r176, without the per-frame sort coming back.
- The Drift Isle's floating island ("you can see into the texture"): where its rim draws in, the rock's underneath
  starts lower than the band of rock round the rim reached, leaving gaps to see inside through. The band now reaches
  down to the underneath all the way round (tools/ops_trees.py, the three islands rebaked); checked offline from four
  sides of each.
- The rock hanging under the island ("you can walk into the mountain thing that's down there") is solid: the island's
  cells collide down its underside's curve to three quarters of a block below them in the middle
  (`OpsTreeBlock.underside`); under the rim stays clear. Placed trees get it as they are. Check: `assertOpsTrees`.
- The Ember Oak's planter ("some gaps in here and between what I'm guessing is basalt and the soil"): a layer of the
  same gravel under the columns, just below the outer ring's seat, so the gaps between the columns and round the
  gravel's edge show gravel, not the floor (the bed rebaked; the trees' shading by it a touch different).
- Still to come, one at a time: the floating tree's leaves (done next, r182); the Ember Oak's soil, its glow flowing up into its leaves,
  and its bark in real depth (r183); then the drop ship's list.

## The floating tree's leaves (Justin, 8 November 2026; take 1 approved; r182)

"It just looks like leaves messily put together and shoved through each other ... I like the idea of there being like
really thick leaves": the Drift Isle's tree keeps its colours and its thick clouds of leaves, but each cloud is now a
dark, leafy core (a rounded box, each face showing its own patch of a dense foliage texture, so nothing shows through
and nothing stretches) with single leaves laid over it like shingles, lying along it from the crown and down its sides,
their tips brought down onto its curve so none sticks out or hovers; fewer underneath, where the core reads as shade.
Each pad is a broad flat-topped cloud and two or three smaller ones round it, kept under the hall's ceiling and inside
the tree's 4 blocks. The crossed leaf sprays on the island are round shrubs of small leaves the same way, kept off the
trunk's foot and the boulders. The trunk, limbs, roots, boulders and island are unchanged (the random draws they came
from are kept). tools/ops_trees.py (`Mesh.clump`; textures `single_leaf`, `foliage_tile` in ops_trees_tex.py); the
three islands rebaked, about 11,000 faces each (5,600 before). Checked offline: whole, close up, from below and as parts.

## The Ember Oak's bark, cast in one piece (Justin, 8 November 2026; take 3 approved; r183)

Take 1 ("all of the stuff is just square ... where the limbs go into the trunk ... looks really sloppy") and take 2
("you need to work on like where the roots go to the trunk, where the trunk go to the limbs") were turned down; take 3
was approved. The whole tree is now one surface: the trunk's three strands, the roots, the limbs and the limbs' own
branches, each the plain union of its round cones, blended into what it grows from by a soft minimum (broad where a
root or a limb leaves the trunk, smaller where a branch leaves a limb) and polygonised on a 0.03 grid, so every root and
limb swells out of the trunk with a fillet, as if cast in a mould, with no seam or crease. Each branch is cast for at
least its first third of a block (thickened to 0.075 there if it is thinner), so even the thinnest leaves its limb in one
piece; past where it gets thinner than 0.075 it goes on as the plain bark and glowing cracks of before, starting under
the last plates.

Over the surface, raised plates of charred bark, each its own shape: the cells of seeds spread over the surface about
0.6 r + 0.1 apart (r the branch's radius there; at least four round a thin branch; smaller down in the hollows of a
crotch), the cells' sides cut straight along the planes between seeds and simplified the same way from both sides. Each
plate is a low fan raised 0.045 (thinner near the thin ends), drawn in by half the crack; its sides go down to the line
it shares with its neighbour, so each crack is a V, closed at the bottom, molten at the foot and cooling to the char at
the lip; a thin strip of the pulsing glow lies in each crack. A plate that would fold over a groove is cut into flat
triangles instead, or drawn with a second ring on the surface; a corner dipping into a groove is lifted a little so the
plate stays stiff. The sides are their own mesh (`ops_ember_oak_<k>_bright` since r184, which holds the leaves too),
baked into the chunk full bright with the bark (`BakedFixtures`), drawn by `OpsTreeRenderer` when baking is off.

Faces per tree, about: 9,600 solid (bark and leaves), 4,450 glow (the per-frame pulse: no more than before) and 8,700
full-bright sides (baked). tools/ops_trees_molded.py (the cast and the plates; needs scikit-image and scipy), wired in
by `molded_trunk` in tools/ops_trees.py; textures `ember_plate`, `ember_edge`, `ember_core` in ops_trees_tex.py. The
atlas gained those three, so every tree mesh was rewritten (the Drift Isle's only in where its texture sits). Checked
offline: whole, every join close up, from below, the crotches, in the dark, all three variants. Check:
`assertOpsTrees` (each oak's meshes are in the mod).

## The Ember Oak's leaves, between a solid leaf and flames (Justin, 8 November 2026; look A of three; r184)

"I want them to be between a solid leaf and flames": of the mockup's three looks (A molten-veined leaves with small
flames licking off the tips, B half leaf half flame, C flames in leaf shapes) Justin chose A. Every spray is now a card
of pointed leaves, dark red going orange to the tip, their veins and rims molten, drawn full bright (baked into the
chunk as light with the plates' sides, `ops_ember_oak_<k>_bright`, both sides of each card; the sprays low in a pad use
a shaded copy of the texture, as their baked shade was before). They lie in the canopy where the old sprays lay (the
same draws, so the halos and sparks are unchanged). About one in ten (more along each pad's top, a few at its edges,
few deep in it; roughly 250 to 290 a tree) stand up instead, their leaves pointing up, and small flames lick off the
tips: a card of their own (`ops_ember_oak_<k>_flame`, glow, soft-edged, none over the leaves) whose tile holds four
moments side by side (`flame_frames`, `flame_step` in ops_trees.json). `OpsTreeRenderer` steps each through its moments
at its own pace and start, so they flicker apart, and lights it with the wave that climbs the trunk (the cracks' wave),
so the flames burn up brighter as it reaches them. The old hot leaves (14% of the sprays, drawn every frame) are gone:
the tree now draws about 290 flames, 480 coals and its cracks each frame, a little less than before. Textures
`ember_spray`, `ember_spray_dark`, `ember_stand`, `ember_flame` (tools/ops_trees_tex.py, 128 texels a card);
`ember_leaves` in tools/ops_trees.py. Check: `assertOpsTrees` (each oak's meshes are in the mod).

## The Ember Oak's soil (Justin, 8 November 2026; look C of three; r185)

"I'd like the soil to look a little bit better": of the mockup's three looks (A volcanic ash with obsidian and glowing
fissures, B a cooled lava crust, C burnt soil with ash, coals and fallen leaves) Justin chose C. The flat gravel inside
the columns (which looked like a paved floor) is now dark, burnt earth with soft drifts of pale ash (`burnt_loam`),
rising in a mound to the trunk's foot (0.17 high there) with low ridges along the roots, which sink into it
(`soil_height`); it is part of each variant's tree mesh, as it follows that tree's roots (the bed keeps the columns and
the same soil under them, for the gaps). On it, where the stones always were: the coals and about a third of the other
stones as glowing coals, a little bigger and rounder, cracked all over with orange (`ember_coal`; baked full bright
with the leaves now, so no longer drawn every frame), the rest dark basalt; 170 sparks of coal in the soil, thicker
near the trunk (`ember_speck`), and 55 fallen leaves (all baked full bright). The soil collides with its mound
(`OpsTreeBlock.mound`, MOUND 0.17; the ridges are left to the eye). Each frame the tree now draws its cracks and its
leaves' flames only. Fixed on the way: every rock's top cap faced inwards (`Mesh.oquad` could not tell which way a quad
whose first two corners meet faced), so the chunk showed a ring where a stone should be; the Drift Isle's boulders had
the same fault and are fixed with it. tools/ops_trees.py (`ember_soil`), tools/ops_trees_tex.py.

## Frame rate, round 4: what can't be seen, garbage, the crystal trees, and a truer check (Justin's JFR of 8 November 2026; r186)

Justin recorded a JFR on r180 (far off, then in close and round the tower): 60 a second far off (vsync), 31 to 53 inside
(about 42, 24 ms a frame), where the check had read 54 to 64 in the same rooms. Where both drew the same thing they
agreed; the gap was what the check didn't have: his drones (3.1 ms a frame; the check's world had none), the drop ship
(1.3 ms; no ship in the check's room views) and more of the game's own world drawing (a bigger window, crew about).
Those two and the crystal trees (2.6 ms) were drawn every frame wherever he looked. His four picks, all made:

- **What can't be seen is skipped** (`client/Sight`): one test for everything, whether a box in the world could be on
  screen this frame: in the camera's view and in a chunk section the game's own occlusion graph reaches (not shut off
  behind solid walls). Every one of the mod's block entities drawn off its own section (nearly all the fixtures) was
  drawn every frame; now each one's extent is learned from what it actually draws (every vertex, on its first frames
  and now and then after: no renderer has to say how big it is) and it is drawn only while that box could be seen
  (out of sight it is still drawn once a second, so something that moved off its box shows up and its box grows).
  With Sodium (his pack has it), which replaces the game's chunk sections, the occlusion half is Sodium's own test
  (`client/SightSodium`, the one it culls entities with).
  The depot tests each drone (docked or flying, with its cargo) and each hangar door itself; the drop ship is drawn
  while a box round where it is now could be seen (with its beams and landing zone when they show); the reactor's
  renderer, which also puffs smoke, is always run. Mixins: `SightExtractMixin`, `SightSubmitMixin`, `SightFrameMixin`,
  `SightLevelRendererAccessor`.
- **Less garbage** (about 400 MB a second, a 10-16 ms collector pause every 4-5 s): the displays' per-frame buffers
  (`HoloGeometry.Quads.perFrame`) come from `client/FrameArrays` and are used again the next frame; plain strings laid
  out for drawing are kept (`client/Texts`, 41 places; **r197**: from r186 to r196 `Texts.of` called itself, because the
  replace that moved the 41 places over rewrote its own two lines too, and the game closed with a stack overflow on the
  first frame that drew a display (Justin's first run since r180, 10 November 2026, at the drone parts shelving). It lays
  the string out itself now, and `tools/check_self_calls.py` fails on any method that hands its own arguments straight
  back to itself, over every source file); the depot's boxes and transform stack no longer make arrays
  and matrices per call; the video wall's lettering no longer makes a string per character; a wall of screens no
  longer makes a render state per block (only a screen's bottom-left block is extracted).
- **The crystal trees' still bark is baked** into the chunk with the planter (`BakedFixtures`'s new extra quads,
  `CrystalTreeRenderer.bakeBark`): about 45,000 of each tree's 83,000 bark corners a frame; what sways on the threads,
  the veins' pulses, the crystals and the halos stay with the renderer.
- **The check now plays like him** (`client/FpsCheck`, `FpsCheckPlan`, new `FpsCheckBreakdown`): it fills the
  hangars (best tier first, until no pad that fits is free; `DroneTerminalBlockEntity.fillForCheck`) and sends drones
  out a few at a time on empty round trips (`flyForCheck`: nothing placed, taken or handed back); the ship stays parked
  for the indoor views; a view close in front of a wall (Sight off, then on) shows what is drawn unseen; a 30-second
  walk round the lobby's fixtures and up to the floor above, looking about, runs with Sight off, on, and on with his
  own frame cap and vsync (what he would see); the window is sized to his main screen (the r180 run stayed at
  854 x 480); and the report gives, per view, the window size, how many block entities Sight skipped of how many, the
  drones docked and flying, and each renderer's milliseconds a frame worked out from its own recording (checked
  against my hand count from his JFR: the same figures).

### Shapes kept on the graphics card (Justin's fourth pick; r187)

"Keep shapes on the GPU" (his JFR: about half the render thread's own time went on sending the same corners to the
graphics card every frame). `client/Retained`: a shape that stays put is written into a buffer on the card once,
turned and lit as it stands, and from then on each frame only says where it is (one draw from the kept buffer, with
the camera's offset). It fits the game's own drawing: a renderer marks the frame as it submits (`Retained.mark`, a
turn for this feature in the solid pass, registered with Fabric's `FeatureRendererRegistry`); when the game asks for
its corners, a shape that can be kept queues itself instead (`Geometry.retained`, `Retained.queue`); once every
shape has been asked (`finishPrepare`) new ones are built and each queued one's place written; in the solid pass they
are drawn with the render type's own pipeline, textures and lightmap. A shape is kept per render type, kind, light and
turn (all baked in), so one that is lit differently or turned another way just gets another buffer; buffers unused
for 900 frames are let go (and the oldest whenever more than 256 MB are kept).

- Kept now: every drone at rest, whole, as one draw: docked in the hangars, on the hall's stands, in the display case
  and on the coffee table (`DroneModel.Kept`: its parts, detail level and whether it's the golden one). Drones in
  flight (spinning rotors, turning) are drawn as before.
- Off, with everything drawn as before: while a shader pack is in use (Iris replaces the pipelines and vertex formats),
  with `-Djugcraft.keptShapes=false`, and for the rest of the session if building, placing or drawing ever fails or a
  frame's queued shapes weren't drawn (logged once). The frame rate check's `sight_off` views turn it off too, so their
  numbers are the "before".
- What differs: a kept drone isn't touched by distance fog (the game's shader measures fog from the corners' own
  places, now the drone's middle); at the distances drones are drawn (160 blocks at most) that fog isn't there anyway.
- Not tried here (no graphics card in my workspace): the first in-game run is the test; the check's report shows
  "kept" per view.

## Doors and displays (Justin, 8 November 2026; r188)

Three items of his second list (`justin_notes_r182_list2.md`: T1, T3 and D1).

- **The maps' drones (T1).** On the Big Map Table the drones and the blueprint's stops were see-through squares, and
  green on the green land. Now solid, lit cubes (`BigMapTableRenderer.solidBox`, drawn with the map's solid render
  type at full light) in colours that stand out: flying orange (`FLYING`), carrying yellow (`CARRYING`), the stops
  ahead blueprint blue (`STOP`).
- **The drones' cargo (T3, look A of the cargo box sheet).** "The dark box idea is good but they look bad": a
  chamfered charcoal hard case (`DroneModel.crate`): ribbed sides, rubber corner guards, a reflective orange band round
  the middle, steel latches, carry handles, a label; under a drone, four webbing straps from its top corners to a steel
  ring on the winch's hook (the straps flatten as it's winched up close). The goods lift's crate is the same case.

### Automatic sliding doors (D1, take 1 approved with clear glass inside; layout 47)

"I really like these server doors ... add similar ones to the front of the facility and then other places." One block
family for every kind (`tower/SlidingDoorBlock`, its cells `SlidingDoorPartBlock`, the sensors `SlidingDoorBlockEntity`;
the kinds and their numbers `SlidingDoorStyle`; drawn by `client/SlidingDoorRenderer`):

| Block | Size | What it is |
| --- | --- | --- |
| Automatic Entrance Door | 6 x 3 | a building's entrance: dark anodised jambs and the operator header (ENTRANCE on its face, an amber line under it, the motion sensor) flush with the wall's face, two fixed framed-glass sidelights and two framed-glass leaves sliding in front of them (a clear opening 2.88 wide), a stainless threshold |
| Automatic Sliding Door (Glass) | 2 x 2 | a room's pair: clear framed-glass leaves with a mid rail and kick plates |
| Automatic Sliding Door (Steel) | 2 x 2 | the same in steel: a vision panel, kick plates, recessed pulls |
| Single Automatic Sliding Door (Glass / Steel) | 1 x 2 | one leaf; `mirrored` slides it right instead of left |

- A room door's leaves hang on the wall's outside face (the side you walk up from; the block's `facing`) from a satin
  track cover over the doorway and the wall either side, each sliding a block along it; the lining round the doorway is
  as deep as the wall beside it (a painted wall's whole block, framed glass's thin panel: read from the blocks there).
  The sensor's light is green when shut, amber while open.
- Anyone walking up from either side opens it (players, and any mob that isn't hostile: the crew walk through, monsters
  stay out); it stays open while anyone is in the doorway or near it and slides shut a second after they've gone.
  Shut, the leaves collide; open, only the frame does. Mobs plan their way through its doorway cells (the entrance's
  middle two; not its sidelights' or header's).
- Placed by hand like the aisle containment door (it needs its whole space clear; a ghost shows it); breaking any cell
  takes the door whole and drops it. Crafted at the Advanced Workbench (machines and drones on).
- In the tower (layout 47, `tools/sliding_doors.py`: one helper places any door whole wherever a generator puts one,
  and checks the finished tower): the Automatic Entrance Door in the north face in place of the three pairs of glass
  doors and the glass row over them; every room's door (`Rooms.door`: the washroom, custodian, offices, first aid and
  crew rest singles in steel, the manager's and meeting room's in glass; the security, cafe, engineering lab,
  simulators, crew lounge and training room pairs in glass, the locker and battery rooms' in steel) and the map room's
  and briefing room's pairs. The room plaques all moved to the band over the doors (clear of the leaves, never on the
  glass); the battery room's NO SMOKING sign one block east. The generator asserts every room door's slide is clear
  (the cells outside the wall beside the doorway and the row over it empty, the wall there standing). Kept as they
  were: the lift core's doors, the data hall's badge-reader doors, the roof door and the intake room's door.
- Checked offline: `crate/game_door.py` renders the doors exactly as the game's code builds them (`DoorDump`), in a
  wall like the tower's (`doors_built_check.jpg`, `doors_built_check2.jpg`). Game tests:
  `slidingDoorOpensForTheCrewNotForMonsters`, `entranceDoorIsWholeAndOpensItsMiddle`, and the rooms' and map room's
  door checks updated.

## The plasma weapons, round 2 (Justin, 8 November 2026; r189)

The plasma items of his second list (`justin_notes_r182_list2.md`: P1-P5, P7, P8; the rifle's new look, P6, is "The NT-7
plasma rifle" below).

- **The baton's damage (P2).** Up to three quarters of the rifle's: 18 (`PlasmaBaton.DAMAGE`), at a diamond sword's speed.
- **The crew can be shot (P7).** A player's bolts hit the tower's own soldiers and crew, and kill them; bolts fired by the
  crew or the drop ship still pass their own people (`PlasmaBolt.canHitEntity`). Test
  `plasmaBoltsHitTheCrewUnlessTheCrewFiredThem` (in place of `plasmaRifleFiresPastTheCrew`).
- **Cooked meat (P8).** Whatever plasma kills (a bolt, or a blow from a plasma baton in a player's or a gate guard's hand)
  drops its food cooked: each food drop goes through its smoker recipe, or the furnace's when that makes food too, the same
  count (`PlasmaWounds.cook`, from `LootTableEvents.MODIFY_DROPS`), so any mod's meat cooks as its own furnace would.
  Test `plasmaCooksTheMeatOfWhatItKills`.
- **The baton's wound where it lands (P3).** It was always low and in the same spot on a big mob: now on the line the
  attacker is looking along, where it meets the target (`PlasmaWounds.aim`); a sweep that misses that line lands at the
  attacker's eye height on it.
- **The baton slashes through every layer (P1, his pick "slash through all layers").** Still a slash, but cut through the
  wool, armour or clothes into the body under them, with walls in every layer and nothing see-through: the outer layers'
  boxes (`WoundLayer`'s) are cut with the body's own wounds, in the body part's frame, and the walls follow the slash
  through them all (`PlasmaWoundsClient.walls`, `trench`).
- **Every mob, small ones too (P4).** One generic path, no per-mob code:
  - The size of a wound follows the mob's: from the smaller of its bounding box's width and height, a bolt's hole or a
    slash is scaled down on small creatures (`sizeFor`: at most 0.21 blocks of hole at full size, never under 0.15 of it),
    so a silverfish or a fly gets a nick, not a hole wider than itself.
  - Mobs not drawn from vanilla model parts (GeckoLib's: the scarecrow, the Fable Corpse Eater) are cut from the geometry
    they submit (`client/PlasmaWoundsMesh`): a mixin on the entity render dispatcher (`PlasmaWoundsDispatchMixin`) wraps the
    collector handed to the renderer, and each quad of the mob's own geometry is cut in cells with the ones in a hole left
    out; where the channel crosses the surface it's recorded, and from those crossings the tunnel's and the slash's walls,
    rims and glow are drawn, as on the vanilla mobs. Vanilla mobs keep the model-part path.
  - Checked offline on a sheep with its wool, a silverfish, a zombie and a warden from the side, many wounds on one zombie,
    and a generic (submitted-geometry) warden and sheep (`woundcheck`).
- **The bolt looks like plasma (P5, look A "ionised air").** What real plasma in air looks like: excited nitrogen glows
  blue-violet, the hottest part white, a soft edge, thin crackling filaments, and as the air behind it cools it fades
  through violet to a dim orange. `client/PlasmaBoltRenderer`, all added light (the eyes layer, two new textures
  `plasma_glow.png` and `plasma_filament.png`): a white-hot core stretched along the flight in a blue-violet sheath and a
  wider, dimmer corona (each a camera-facing glow, flickering), a round glow at its head so it reads head-on, six
  filaments crackling off it (new each tick), and a wake of up to 3.2 blocks behind it cooling from blue-violet to
  orange; the trail's motes blue-violet and ember orange, the muzzle flash blue-violet (`PlasmaBolt`). Checked offline:
  `crate/game_bolt.py` draws the game code's bolt (`BoltDump`) next to the approved mockup (`bolt_built_check.jpg`).

## The NT-7 plasma rifle (Justin, 8-9 November 2026; take 3 of rifle A; r190)

P6 of his second list ("better, cooler, futuristic, militaristic, realistic"). Of take 1's three he picked A, the bullpup,
"but the purple barrel remains dark until it's producing plasma for the shot ... once you shoot, it then suddenly glows
purple and becomes darker until the next shot is ready. Don't change the fire rate". Take 2 ("make the thing look
cleaner, make the side text more visible, and make the parts of the rifle more defined") was "not good enough": "The
NT-7 is too large, I want the holographic sight to look more realistic and be accurately lined up with where shots land,
and there to be a good first person view using it. The trigger and guard ... needs work, the grip needs work too". Take
3 approved, with his picks: it works like the guns, his own arms hold it, the full-hand guard.

- **The rifle** (`tools/plasma_rifle_nt7.py`: one description of its parts, 63 boxes, some turned, at 4 texels to its
  pixel, 8 on the body's sides and the grip): one moulded body from the butt to the guard (tungsten grey), its floor
  stepping up behind the grip in a 45-degree fillet, NT-7 small over the cell (five texels tall); a dark metal upper
  with a rail its whole length; a holographic sight (a clamp and its throw lever, a body with its battery cap and two
  buttons, a hood sloping at the front over two windows, the front one coated); a pistol grip leaned back 18 degrees
  (stippled sides, palm swell, finger ridges, flared base, beavertail); a full-hand guard moulded with the body (a front
  post with fillets, a cut corner, a bar into the grip's base) and a curved steel trigger in it; a selector, cross pins,
  sling points; the power cell in the stock's floor; the accelerator between six gunmetal fins; an angled fore grip; a
  bright steel muzzle and its emitter. Its frame's origin is the grip's top, which is the item's centre.
- **Dark until it fires.** Five models by how much of the cooldown is left (`STATES`): ready, the accelerator dark
  smoked glass and the cell's four bars lit; the shot, the accelerator and emitter flaring purple and the bars empty;
  then darker purple as the bars refill, one a quarter second. The item definition picks them with
  `jugcraft:plasma_charge` (`client/PlasmaCharge`): a player's cooldown, or for anyone else (the tower's security) the
  time since this client saw a bolt of theirs appear. Fire rate unchanged (a shot a second).
- **Controls, as the guns.** The attack button is the trigger (held, it fires each time the rifle is ready; it never mines
  or punches): `client/PlasmaRifleClient` asks the server (`PlasmaRifleItem.TriggerPayload`), which decides
  (`PlasmaRifleItem.trigger`). Right click held aims down the sight (the item is used: you walk at 60%, as aiming a gun).
- **Where shots land.** Exactly where the middle of the screen is: the server finds the first block or creature on the
  line from the eye along the look, as far as a bolt flies (`aimPoint`), and the bolt flies straight from the muzzle
  (just ahead of the eye, under the sight when aimed, at the gun's side from the hip, `muzzle`) to that point with no
  spread. Bolts don't fall, so it lands on the dot at any range. (Before, it started below the eye with a little spread.)
  The crew keep their spread.
- **First person** (`PlasmaRifleClient.firstPerson`, from `mixin/client/PlasmaRifleHandsMixin`): placed from the rifle's
  own numbers, low on the right at the hip with the player's own arms on it (their skin and sleeves, wide or slim; the
  right hand on the grip, the left on the fore grip, drawn as the guns draw theirs); aiming eases it over four ticks onto
  the sight, the rear window's middle on the view's axis 2.8 of its pixels from the eye, the arms smaller, the view
  narrowed to 85%. Near full aim the holographic reticle (a violet ring, four ticks and a dot,
  `textures/gui/plasma_reticle.png`) is drawn on the middle of the screen in place of the crosshair, sized as the front
  window shows it: a real holographic reticle shows only to the eye looking through the window, so it isn't on the model
  (the guns' reflex sight draws its dot the same way). Each shot kicks the rifle back and up a little. A left-handed
  player holds it on the left; in the other hand it's drawn at that side's hip without arms.
- **Seen from outside**: a player holds it as a two-handed gun (`GunPose`), the barrel out along the raised arm; it owns
  the arms as a gun does (`CastPoses`).
- **The crew's rifle is the item**: `TowerCrewRenderer`'s rifle layer draws the NT-7 at the props' rifle group (on the
  right forearm, the grip in the fist, turned out along the look while aiming), a crew pixel to the rifle's; its
  accelerator flares after each of their shots. The old rifle's boxes are gone from the props (`crew_skins.py`: the group
  keeps only where it hangs), and the old bolt texture (`plasma_bolt.png`, unused since bolt A) is deleted.
- Checked offline: `crate/check_nt7.py` renders the item exactly as written (its model JSON and texture, laid out as the
  game lays faces and UVs) beside the approved sheet, and the first person from the same numbers the Java uses
  (`nt7_built_check.jpg`). Test: `plasmaRifleShotsLandWhereThePlayerLooks` (right click aims without firing at a gun's
  pace; the aimed point is on the eye's line and on the creature looked at; the bolt flies straight at it, no spread, and
  hits; it won't fire again until ready); `plasmaBoltsHitTheCrewUnlessTheCrewFiredThem` fires with the trigger now.

## The crew list (Justin, 9 November 2026; sheet take 1 approved; layout 48; r191)

C1 to C6 of his second list. The visual parts went on a sheet first (`crew2/crew_take1.jpg`: the new security posts and
their routes, the bay's seats, the carts in the custodian's closet); his answer added an armed patrol on each floor.

- **C1, nobody went upstairs**: `CrewPaths.obstacle` counted the mod's stairs as furniture, so no walk was ever planned
  up them; the crew stayed on the ground floor, and the one security man who went up got there by being put back at his
  post. Stairs are walkable now. Test: `crewWalkUpTheStairs`.
- **C2, more security** (his picks "all three outside", and "one security with a plasma gun patrolling the first
  floor, one on the second, and another on the roof"): the two outside patrollers now stand either side of the front
  entrance facing out (`perimeter_west`, `perimeter_east`, ids kept); two stand at the energy and storage inputs on the
  east side facing out (`inputs_north`, `inputs_south`); three walk circuits round the outside at three distances, the
  middle one the other way round (`circuit_inner`, `_middle`, `_outer`: `tripillar_crew.circuit` pushes a hexagon out
  until it clears the building); and a new armed patrol walks the upper floor (`upper_patrol`), with the ground floor's
  hall patrol and the roof's two. Security 8 to 14, 47 posts; the security pool has 18 faces (`crew_skins.py`). None of
  the new posts takes breaks; they're on through the night.
- **C3, the landing sound**: the ship's sounds swell as it comes near (`CrewShipSounds.Follow.near`: (40 / (40 + d))^0.8,
  at least 0.12), on the loops and the one-shots.
- **C4, seated aboard, security stay** (his pick): the crew it brings are chosen as it's sent (`TowerCrewCare.hires`,
  each in a face nobody in the tower wears nor anyone before them aboard) and ride in the bay's seats, the security in
  the ones nearest the ramp (`TowerCrewCare.file`: all the security first). Coming in, with the side doors open, they're
  seen sitting there: the ship tells the client who (`CrewShip.passengerLooks`) and `CrewShipRenderer` draws a stand-in
  dressed as each, as the tower's crew are drawn. As it opens up the server seats them for real in the same places
  (`CrewShip.board`, `TowerCrewCare.board`, `TowerCrew.board`: a seat of their own at each of the 34 wall seats and the
  six by the flight deck's door, `CrewShip.SEATS`; more than 40 stand in the aisle), and they sit still
  (`TowerCrew.AboardGoal`). With the ramp down they're called off one at a time, in seat order (`TowerCrew.disembark`):
  up, out into the aisle, aft and down the ramp (`CrewShip.wayFrom`), waiting while hostiles are near its foot. The
  ship's own security (off first) hold the ramp's foot facing out, rifles ready (`EscortGoal`, up to
  `CrewShip.ESCORTS` = 6), until it lifts off (`CrewShip.guarded`), then go in to their posts. A ship bringing no security
  still calls two of the tower's out to cover it. Saved: who's aboard and where (the crew), who it brings (the ship).
- **C5, carts from the custodian's closet** (his pick "custodian room only"): the three janitors' carts stand side by
  side in the custodian's closet upstairs (`tripillar_rooms_upper`), each post knowing its cart and the cell before it
  (`CrewPost.cart`, `cartStand`). A janitor comes off the ship empty-handed, walks up to the closet and takes their cart
  (its two blocks lifted, kept as they stood), works the round pushing it, and at bedtime pushes it back up and parks it
  where it was. A cart that isn't there (broken, or a filled cell to park in) and they go without and try again later;
  one who dies or is sent away with a cart out puts it back.
- **C6, nobody left outside**: everyone off the ship walks in to their post (C1 let them get there; C4's ordered walk
  off keeps them out of each other's way).
- Layout 48 (`TowerData.TRIPILLAR_CREW_DETAIL_LAYOUT`; 47 frozen). Tests: `crewPostsAreSound` (47 posts, 14 security, the
  posts and circuits as above, the carts in place), `crewRoutinesAreSound`, `crewWalkUpTheStairs`,
  `dropShipCrewRideInSeatedAndSecurityHoldTheRampTillLiftOff` (seated still in the seat; up and down the ramp past its
  foot; guarding with the rifle ready while it's down, stood down as it lifts), `janitorsFetchTheirCartsAndParkThemAtNight`
  (no cart till fetched; both blocks gone from the closet; parked back exactly as they stood at bedtime).

## The drop ship list (Justin's r180 playtest, items 8 to 24; sheets approved take by take; r192)

His drop ship items, then three more rounds on the sheets: the turrets ("check for realistic designs", then "refine it ...
rotating barrels on the minigun one ... sounds and animations accurately lined up"), the interior ("the walls and floors
and such, make it look better"), the side gun bays and mounts ("put some stuff into the area the side guns sit, work on
the side gun mounts too"), and "optimize what you can". All in the generator (`tools/crew_ship.py`, its sheets'
scripts in the scratchpad's `ds/`) and the ship's Java.

- **Plain fixes**: the jump lights clear of the frame (8); the ceiling frames stop at the bay gun (10, `CEILING_HUNG`);
  the wing root cut at the skin, out of the bay and the flight deck (13, 18, `Ship.clip_outside`); the waist glow and the
  stern cheeks' panels line up (16); the wing fence, the roof's blade aerials and the fin's wicks gone (17, 20, 22); the
  thruster pylon one moulded fairing (19); the fin's beacon and tail light set in flush, the fin's shade matched (22, 23);
  the top turret's mound flares into the roof with no crease (24).
- **The doorway to the flight deck** (11): jambs, sill and trims standing off the bulkhead (nothing in its planes), steps
  up from the bay, a lit FLIGHT DECK sign, and a door (`fdoor`) that slides aside into the bulkhead as a player comes
  near (`CrewShip.clientTick`: within 2.8 blocks; the renderer slides it `DOOR_SLIDE`); it isn't in the collision cells,
  so anyone can walk through.
- **Seats** (9, his pick "Right-click to sit"): troop seats with a padded pan, back and headrest on rails, struts and a
  harness; right-click one while the ship is down with its ramp open (`CrewShip.seatsOpen`) to sit
  (`client/CrewShipSeatUse` finds the seat looked at, `CrewShipSeatUseMixin` at the start of the game's right-click,
  `CrewShipSeatPayload`, `CrewShip.sit`: free, within 4.5 blocks; a ship seat is a `SeatEntity` that knows its ship).
  Sneak to stand up, in front of the seat; everyone is stood up as it closes, before it lifts off.
- **The kit on the front wall** (12, 14, 15): the mod's own first aid box and fire extinguisher (`model_faces`: any block
  or item model placed in the ship from its JSON, its textures cropped to what it uses), a rifle rack holding three real
  NT-7s.
- **The gear** (21): four telescoping legs in wells (five stages each, a footpad), each well shut by two doors that rise
  into the hull and slide fore and aft; down, the doors open first, then the legs run out (`gear_phase`; the renderer
  moves `gear_s1`-`gear_s5`, `gear_door_a`/`_b` from the flight's gear).
- **Turrets**: drawn after real guns. The door guns rotary cannons on a remote head (six barrels in a rotor, clamps, a
  slotted flash hider, the housing in a saddle cradle, the drive motor slung under it, the feed chute from its cell);
  the top turret a remote weapon station carrying two rail cannons; the bay gun a chin turret with one long cannon in a
  finned jacket; the wing guns ball turrets. Moving parts: `spin` (the door guns' barrel clusters), `recoil_l`/`_r` (the
  heavy guns' barrels). The door guns spin up for `CrewShip.SPIN_UP` (8 ticks) before their first shot and run on
  `SPIN_DOWN` (20) after; the server sends the spin (`SPIN`), the client turns the cluster one barrel a shot at full
  speed, so the turning and the shots keep step, and plays the drive's whine with it (`crew_ship.door_gun_motor`,
  `tools/crew_ship_motor.py`: made, a seamless loop; pitch and loudness with the speed). Each heavy gun's shots are
  counted to the client (`SHOTS`) and the barrel that fired kicks back and runs out over 6 ticks. Muzzles follow each
  gun's barrels (`Gun.spread`).
- **The interior**: the walls kick panels under a metal trim and insulation blankets above, one to each bay between the
  frames (now every 1.2, in the seats' gaps); the deck grit-coated plates with recessed tie-down rings and two roller
  tracks; the ceiling and its chamfers perforated panels, LED strips where the walls meet them, the lights moved into the
  bays between the frames (one that would meet the bay gun on to the next); the front wall lined to match; grab rails on
  brackets.
- **The side gun bays**: a flat deck and a sill at the door; down the middle the boom's bed (a plinth, two rails with
  rubber-faced stops, a power rail on insulators between them); framed at both ends. Forward wall: the gun's PLASMA
  CELLS magazine (four glowing cells behind bars, its feed under a cable guard to the bed) and the DOOR GUN control box
  (guarded switches, lamps, a knob). Aft wall: a spare barrel cluster on padded brackets, a SPARE CELLS crate strapped
  to the deck, a fire extinguisher, the gunner's tether on a clip from its anchor on the head; a light, a red night lamp
  and a cable tray overhead.
- **The mounts**: a cast boom (chamfered, three lightening pockets a side) riding the rails on two trucks, its shoe on
  the power rail, a flared pedestal moulded under a round bolted ring, a conduit clamped along its top. Two faults
  fixed: the side door slid through the gun (both moved at once), and the stowed gun's barrels poked out through the
  shut door. Now the door opens over the first half of the side doors' travel and then the boom runs out
  (`CrewShip.door`, `boom`, `gunX`); going in, the gun turns down its bay toward the bay's inner forward corner while
  still out (`drawnYaw`, `drawnPitch`: `DOOR_GUN_STOW`), comes in barrels first, and the door shuts after. The door guns
  shoot only once right out (boom over 0.85) and spun up.
- **Optimising** (his "optimize what you can"), for everything at once:
  - In the generator: anything round gets as many sides as its size wants (an edge of 0.08 near, 0.25 far; never more
    than it asked for); the far model leaves out anything under 0.1 across; faces that can never be seen (against an
    opaque face turned the other way that covers them, within one moving part; exact duplicates) are left out of the
    export (`hidden_faces`: no pixel changed in the checks); the bay's and gun bays' fittings are drawn only near (with
    the interior); the guns get far models too. Near, about 51,000 quads became 37,000 (and 13,000 for the guns 9,700);
    far, 15,700 became 9,300 (and 4,800 for the guns).
  - In the renderer (`CrewShipRenderer.Draw`): a part that has stood where it is for a few frames (the ship down, a gun
    at rest) is kept on the graphics card and only placed each frame (`Retained`, as the drones at rest; not its glass);
    otherwise its corners are written with those of faces turned from the camera skipped (the game culls them anyway:
    `CrewShipMesh.Batch.face`), each corner's light kept from the last frame while nothing has moved, and the light grid
    kept as it was while it reads the same.
  - The collision cells leave out anything smaller than a cell or thinner than 0.1 (struts, bolts, rings: the seats'
    struts caught the crew standing up).
- Tests: `dropShipDoorGunsSpinUpStowAndPlayersSit` (the doors then the booms, the stowed aim; the door gun's barrels
  spinning before its first hit; a player sits in a free seat, not in a taken one, and is stood up as it closes);
  `dropShipLightsTheGroundAndItsGunsShoot` holds the ship down while its gun works; the C4 test in its own batch as
  before. The drawing (kept on the card, the culling) is for the in-game test: there's no graphics card here.

## Drop pod squads, release (a) (Justin, 9 November 2026; sheets 3 to 5 approved; r193)

A squad of power-armoured legionaries called down by drop pod onto a spot the player marks (sheet 3: pod C, the
"dart"; sheet 4: the designator, ranks, requisitions; sheet 5: his notes on sheet 4). Release (b) adds the leader's
orders (follow, hold, patrol 200, send home) and the pickup drop ship. Code in `tower/squad/`; the plan and its
progress in the scratchpad's `justin_notes_dropsquads.md`.

- **LTD-7 laser designator** (`LaserDesignatorItem`; model and sight from `tools/laser_designator.py`; Advanced Crafting
  Table recipe): hold right-click to raise it to the eye: its own sight (`client/LaserDesignatorClient`, the round field
  in the dark housing, `textures/gui/ltd_sight.png`), the reticle, the heading strip, the range under the reticle, the
  zoom and its bar. Scroll zooms 1x to 5x (the mouse slows to match; `DesignatorScrollMixin`), crouch and scroll picks
  the squad (3, 5 or 7) with what's on the player's account for it; left-click marks the spot (no beam; only that player
  sees the mark: green brackets and the pods' time to land).
- **The mark** (`SquadDrop.mark`): refused out of range (256 blocks), under a roof or in a world with a roof of rock
  (open sky at the spot), with a squad of the player's still out (one at a time), or with no charge for that squad; else
  the charge is spent and a pod launched for each soldier. Where they land (`SquadDrop.spots`): the leader's within 3 to
  6 blocks of the mark, the rest 10 to 20 blocks apart round it, on open sturdy ground (no leaves, water or lava) within
  6 blocks up or down.
- **The pods** (`DropPod`, drawn by `client/DropPodRenderer` from `assets/jugcraft/drop_pod/pod.bin.gz`,
  `tools/drop_pod_export.py`): one flare hangs over the mark for 3 seconds, then each pod splits off and weaves down its
  own path (`DropPod.path`: a curve from the flare, two side-to-side waves fading at both ends), heat shield first in a
  sheath of re-entry fire with a burning trail, the eight retros firing over the last 14 ticks; about 20 seconds from
  mark to landing. Landed it sits a block in the ground, scorched and smoking; 12 ticks later the hatch blows off on its
  bolts and tumbles out, and the soldier comes out dazed. Once its soldier is 10 blocks clear (or gone) the pod blows
  itself apart. Nothing the pods do breaks a block or hurts anyone. Sounds from `tools/drop_pod_sounds.py`: the scream
  (following the pod down), the retros, the impact, the hatch's bolts, the self-destruct, the designator's mark tone.
- **Legionaries** (`Legionary`, drawn by `client/LegionaryRenderer` with the crew's model, the security detail's skins,
  power armour, helmet and the NT-7): named as the crew are; the first pod's is the leader, his helmet crested by rank
  (decanus for 3, tesserarius for 5, optio for 7 with two plumes; the props' `crest_*` and `plume_*` groups, gear bits
  512, 1024, 2048, from `tools/crew_skins.py`). Out of the pod they stand dazed a few seconds, then form a wedge behind the
  leader, who keeps near the player who called them. They shoot hostile mobs they see, whoever hurts one of them or the
  player, never the tower crew, tamed animals, each other or their player, and players only where the server allows
  players to fight (never creative ones). Their bolts and the crew's pass each other. Wounds close slowly out of a fight;
  if the leader falls the next in line takes the crest. They stay until killed or sent home (release (b)); a squad left
  behind out of reach (none of
  it loaded) is recalled when its player calls another (`SquadAccounts.listed`).
- **Requisitions** (`Requisitions`, `client/RequisitionScreen`): using the tower manager's executive desk opens the page
  (a generic hook for room kit pieces, `RoomKitBlock.USES`) for whoever may run the tower (owner, party in Party mode).
  A squad costs iron, copper and gold ingots and diamonds (3: 96/64/24/6; 5: 160/112/40/10; 7: 224/160/64/16), taken
  from the tower's store first and then the logistics network its terminal reaches (the cargo packager), used up. Each
  is a charge on the player's account (`SquadAccounts`, saved with the overworld) that any designator they hold spends.
- **Tests** (`SquadGameTests`): payment store first then network; the mark's refusals and a taken mark; the drop (five
  pods apart, landing, soldiers out, the leader's crest, the pods blowing once clear, nothing broken, the next in line
  leading); a legionary shooting a husk and nobody else.

## Drop pod squads, release (b) (the leader's orders and the pickup; sheet 4 approved; r194)

- **The orders panel** (`client/SquadOrdersScreen`, sheet 4's): the player who called the squad uses (right-clicks) its
  leader: over the darkened view, his rank and name and the squad's strength, its order, whether it's in a fight, the
  roster with each soldier's health, and the orders. Others who use him (and anyone using the rest) get a word from
  them. Each order is the whole squad's (`SquadOrders`, `Legionary.command`).
- **FOLLOW ME**: as release (a): the leader keeps near his player, the rest in a wedge behind him.
- **HOLD THIS POSITION**: the leader on the spot where he stood, the rest in a ring round it (3.5 blocks) facing out;
  they engage what comes in range but don't leave the ring for it (a target gone out of range of the spot is let go).
- **PATROL AREA**: the leader walks from point to point round the spot within the radius (50 to 400 blocks, 200 at
  first, set with - and + on the panel; a change while patrolling takes at once), on open loaded ground, pausing 2 to 7
  seconds at each, the squad in the wedge behind him.
- **SEND HOME**: not in a fight (any of them with a target, or hurt a moment ago), nor with no landing zone: a drop ship
  (`CrewShip.setUpPickup`) comes for them, setting down where `CrewShipSite.near` finds flat, clear ground (the crew
  ship's own test: 34 by 60 within a block) 36 to 96 blocks from the leader, off any tower, its ramp toward them.
  They hold till it's down, gather just off the ramp's end, and it calls them aboard one at a time (the next once the
  last is on the ramp; the leader last; waiting while any is fighting or hostiles are near the ramp): each walks up the
  ramp and along the aisle to a seat (moved along the way, as the crew come off), and from there the client draws them
  seated, crest and all, as it closes up and leaves. Aboard, they're off the player's record: the next squad can be
  called. One who can't reach the ramp in a minute is left behind holding; the ship waits at most 150 seconds. A pickup
  shoots hostile mobs but never a player for coming near (only one who hurt the crew).
- **Tests** (`SquadGameTests`): the orders (the ring round the spot with the player gone off, no home in a fight, the
  patrol's radius at least 50, the leader coming up to his player following); a squad of three sent home boarding in
  order, the decanus last, all three seated, off the record, and the ship leaving.

## Narv-Tech power armour and the plasma blade (the power armour sheets, takes 1 to 8; r195)

Justin, 9 November 2026: "make the full set of Narv-Tech power armor for the legionaries/security. I want it to look
cool, be black and purple themed. They should already have some so, redesign it"; then, of the helmet, "Take some
inspiration from any description from the undying mercenary series"; "I don't like how fat the helmets seem ... Find a
balance"; of the blade, "Build it with the armour" and "Have it do the rifles burn through thing for a thrust, but the
batons burn through thing for a slash"; built on his "Build it". The series gives ideas only (Roman-styled legions, a
visor that overlays, a sealed suit with its own air, a powered frame, a blade in the suit's arm): no names, text or
designs of it are used.

- **The suit** (`tools/power_armor.py` writes `assets/jugcraft/crew/armor.bin.gz`, in `client/CrewShipMesh`'s format, and
  `textures/entity/power_armor/armor.png` with `armor_glow.png`; `client/PowerArmor` draws it): about 1,400 faceted
  faces worn, a group for each bone in that bone's own space, drawn on the bones as they stand, so every plate rides its
  own limb. Satin black plate with lighter worn edges, gunmetal fittings, purple painted plates (pauldrons, knees, toe
  caps, the helmet's brow and stripe), thin violet lights added again full bright from the glow sheet. Gorget, cuirass
  with chevron pectorals and the hexagon emblem, segmented abdomen, belt, back plate and power pack (NARV-TECH across
  it), pauldrons, elbow hubs, vambraces (a screen on the left wrist), tassets, cuisses, powered knee hubs with assist
  struts, greaves, armoured boots. Security and the drop squads' legionaries both wear it (the same gear bits as before).
- **The helmet** (the legion's helm, 9.1 px across its bowl on the 8 px head): a legionary's helmet made a sealed one. A
  bowl with a purple brow band over a gunmetal peak, a recessed face with a black visor and the eyes' light behind it,
  two hinged cheek guards closing on the rebreather, a comms pod at each ear, a two-lame guard down the neck with status
  lights, an aerial. A squad leader's crest is a group of its own on it by his rank's gear bit (`Legionary.CRESTS`: a
  comb taller up the ranks, the optio's with two plumes).
- **The undersuit**: the security skins (`tools/crew_skins.py`) are a black bodyglove, ribbed at the neck, waist, elbows
  and knees, a deep violet ring at each elbow and knee. The box exoskeleton, helmet and crests are gone from the props.
- **The plasma blade** (`tower/crew/PlasmaBlade`, shared by `TowerCrew`'s and `Legionary`'s rifle goals): when what
  they're fighting gets within 3 blocks (of its side) and in sight, the blade comes out of its arm mount on the right
  vambrace in 12 ticks, the rifle dropped to the left hand; no bolt is fired with it out. They stand while it deploys;
  then a move every 20 ticks (10 for the move, its blow landing at 5): a **thrust** at 2 blocks or more (a lunge in
  behind the point; 24 damage, the rifle's; a tunnel wound, `PlasmaWounds.burn` as a bolt's) or a **slash** nearer (18,
  the baton's; a gash). After the first move they close to 1.3 blocks (a legionary holding a position stands his
  ground). With nothing in reach for 20 ticks, or the fight over, it's stowed and they go back to the rifle. A blade's
  kill counts as plasma (`PlasmaWounds.plasma`: cooked drops). Its sounds are the baton's, played at 0.75 of their
  pitch; lit, it hums (`PowerArmor.Hum`, the client's). Gate guards keep their batons.
- **The arm mount and its deployment** (`PowerArmor.Layer.blade`; the numbers are `tools/power_armor.py`'s): a saddle
  clamped round the forearm by two bolted bands; on it, from the elbow, the breech (the plasma cell across its top, a
  power line in from the elbow's hub, cooling fins in its flanks), the barrel shroud (two rails for the carriage, a vent
  in each flank) and the muzzle block (the emitter's ring, the shutters hinged at its corners). Each client times the
  deployment from when it first sees the blade out: the two shutters swing 100 degrees to stand as prongs and the fins
  slide out (to 0.17 of it); the carriage runs down its rails driving the spine out, three telescoping segments behind
  a point (0.17 to 0.59); the plasma runs up the spine (0.62 to 0.95). Stowing is the same in reverse; the fins' ends
  glow while it's lit and a second after. As the baton's rod, the plasma is held round a solid core.
- **Acts** (`TowerCrew.ACT_BLADE` 23, `ACT_THRUST` 24, `ACT_SLASH` 25): on guard as the baton's guard; a thrust out and
  back; a slash up over the shoulder, down across and back (`TowerCrewModel.bladePose`).
- **Tests** (a batch of their own, `plasma_blade`: their husks stand a long while, and a drop ship's guns or another
  test's soldiers near would take them for targets): `SquadGameTests.plasmaBladeThrustsAtArmsLengthSlashesClose` (the
  blade driven by hand: out at once for a husk in reach, no move while it deploys, a thrust at arm's length, a slash
  close, stowed with nothing in reach); `TowerGameTests.securityTakeTheBladeToWhatGetsClose` and
  `SquadGameTests.legionaryTakesTheBladeToWhatGetsClose` (through their own goals: the blade out, no bolt with it out,
  its blows landing, stowed when the husk's gone). The exported mesh was read back and drawn outside the game with the
  client's own moves (deployment included) and matches the approved sheets. The look in the real pipeline is for the
  owner's test.
- **Since r196** the blade's distances and its wounds are as the next section has them (the cut follows the blade; the
  moves were brought round so that the blade passes through what's ahead of them).

## Plasma cuts follow the weapon; what's cut off falls and burns; what kills outright (takes 5 to 8; r196)

Justin, 9 November 2026, of the power armour sheets: "i want the damage to the body to be reflected in the path of the
sword or projectile. If the sword swings across a certain place, everything it hits I want to be affected"; "Cut through
thin parts. Make sure to make this compatable with any creature modded or vanilla, if it results in something like let's
say for example an arm being cut through completely, then either have the arm disappear or maybe even fall to the ground
on fire and burns out destroyed"; "Cool now make that work for all the plasma weaponry. And make it so if it does
something like cut a torso in half or a head is decimated that the creature simply dies"; the blow that kills cuts it
apart ("Yes"); outright kills for "Ordinary creatures only"; "Leave the launcher as it is"; "Build it".

- **The cut is the weapon's path** (`tower/crew/PlasmaWounds`): a bolt (the NT-7's, a player's or the crew's, and a drop
  ship's guns') and a blade's thrust burn a tunnel along their own line, as before (a thrust's 1.5 px in radius, from
  the wearer's shoulder at what it's aimed at). A swing (the blade's slash, the guards' baton, the baton in a player's
  hand) is a `Swing`: the fan the edge swept, about the swordsman's shoulder, its middle through where it's aimed, the
  edge coming down across that at the weapon's own tilt, burning from where the plasma starts to where it ends (the
  blade 0.60 to 1.29 blocks out, 55 degrees either side, tilted 35 degrees to the wearer's left; the baton 0.66 to 0.94
  (the rebuilt baton's plasma, r203; 0.62 to 0.87 before), 50 degrees, a guard's chop near upright, a player's tilted 15 to 45 degrees either way). The wound sent is the fan
  itself (kind `FAN`, in the hit creature's own frame); the client burns away whatever of the model lies in it, part by
  part. A swing that landed always bites at least 2.5 px in from where the fan first meets the creature, however short
  the edge fell (as the baton's gash always did). **Every foe in the path** takes the blow too (`sweep`, `thrust`: a
  hostile mob, or a mob that's after the swordsman; never the crew, legionaries or anyone else): its damage, its wound.
  The Industrial Plasma Launcher is left as it is.
- **The blade's reach made honest** (`PlasmaBlade`): the cut is where the blade went, so the blade has to go through
  what it hits. It still lights at 3 blocks and stands while it deploys; then they close to 0.9 of a block from its
  side; a slash is thrown from within 1.2, a thrust from 1.2 to 2.2, each with a step in behind it sized to bring them
  to 0.9 by the time the blow lands (a push of (gap - 0.9) / 2.1, at most 0.6); a blow lands on what's within 1.6 when
  it falls. A legionary held to his post lights it only for what comes within the blade's own reach (1.29). The moves
  themselves were turned (`TowerCrewModel.bladePose`): the slash now comes from over the right shoulder down across to
  the left hip and the thrust is turned in to the middle, both aimed up or down where they look (before, the slash came
  down 0.4 to 1 block to the wearer's right of what stood dead ahead: it would have missed it).
- **What's cut off** (`client/PlasmaWoundsDebris`, any creature drawn from model parts, vanilla or a mod's): each part of
  a model a hole goes through is taken as cells (`Flesh`, half a pixel; coarser for a huge box), those in the holes
  burned out; what's left falls into pieces where a cut goes right through (flesh right beside a burn doesn't hold a
  piece on: a seared sliver is as good as cut). The piece at the part's own pivot stays; any other has come off, and so
  has a part that hangs from a piece that came off. It works on boxes, never on names. What comes off becomes a `Body`
  in the world: the very faces the model drew for it, taken as they're drawn that frame with each one's own render type
  and texture (the skin, clothes, armour, wool, glowing spots), and its seared cut faces. Pieces that touch are one
  body. It falls the way the blow went and away from the creature (a tall thing ends lying down, a flat one settles),
  burns about three seconds (flames, then smoke; charring black from 0.6 s, lit by its own fire), shrinks to nothing
  from 4.5 s and is gone at 5; at most 48 at once. What came off stays off, and the wound that cut it isn't dropped for
  a newer one (the stump keeps its seared face). All of this runs in the middle of drawing every creature, so a fault in
  it turns the cutting-off off for the session with one line in the log ("Plasma wounds: cutting parts off is turned
  off"), the wounds going on as they always were, rather than taking the game down.
- **Trunk and head** (`PlasmaWoundsClient.Vital`): nothing comes off a creature's head (the part named so; failing that
  the part nearest its eyes) or its trunk (the parts named a body, torso or chest; failing that its biggest box that
  isn't its head), nor what lies over those (a jacket, a hat), unless the blow was the death of it.
- **What kills outright** (`PlasmaWounds.strike`, `Vitals`; ordinary creatures only): the server has no models, so it
  judges on its own measure of the creature: its game box in small cells, a column up its middle for its trunk (a squat
  creature's, a beam along it) and a cube about its eyes for its head (on top; at a squat creature's front), with the
  same tunnels and fans burned through them, and kept, so wounds add up. Dead at once when its trunk is in two (the
  lesser piece at least 15 parts in 100), when more than half its head is burned away, or when its head is off its
  trunk; the kill is the attacker's (the blow's own damage source, so plasma's cooked drops and the rest follow).
  Anything less it lives through, and its health decides as ever. **Never outright**: players, the Wither, the Ender
  Dragon, the Warden, anything tagged `c:bosses`, and anything with 200 health or more (`GUARDED_HEALTH`); on those a
  swing only gashes the trunk and head a bite deep (they can still lose a limb), and they die by health.
- **The blow that kills cuts it apart**: a wound on something the blow killed (outright, or by its damage) is marked
  fatal; on the client every piece of the creature then comes off at once, along the cut where the cut went through and
  whole where it didn't (pieces that touch falling as one: a creature shot dead falls as it stood and burns), and the
  creature itself is drawn no more (`PlasmaWoundsDispatchMixin`). If nothing of it could be gathered it dies as ever.
- **Limits known**: a mod's creature drawn some other way than from model parts (GeckoLib's) gets the swing's cut as a
  gash (`PlasmaWoundsMesh`) but nothing comes off it and it dies as ever; the power armour's plates aren't model boxes,
  so they aren't cut along a wound: a limb's plates go when some of that limb does (`PlasmaWoundsClient.lost`); the
  server's measure of a creature is its game box, not its model, so now and then a kill and the look of the cut won't
  quite agree (the model follows the server: a trunk the server says holds stays whole).
- **Tests**: `TowerGameTests.plasmaCutsThatSeverKillOrdinaryCreaturesOutright` (the server's judgement: a slash right
  through a husk's trunk and a level swing through its neck kill it at once; the same slash from a block off doesn't;
  three bolts through its head do, one doesn't; a bolt through an arm past its trunk doesn't; a blow that kills by
  damage is marked fatal; a husk with 400 health, a warden and a player are guarded; the fan meets only what's in its
  path); the blade tests as before (`plasmaBladeThrustsAtArmsLengthSlashesClose` with the new distances). The client's
  cuts were run outside the game on baked vanilla models (a zombie, skeleton, cow, sheep in its wool, a player in a
  jacket, iron golem, spider, warden: swings and bolts as the server sends them, what came off followed to 3.6 s) and
  looked at; the look in the real pipeline is for the owner's test.
- **The drop ship's gun tests** (they failed now and then since r192; found here): the tests stand at a random place
  each run, so whether a target off beyond the test's own plot, and the bolts on their way to it, lay in a chunk that
  ticks was chance (the bolts were fired and never arrived): each now keeps the chunks round it loaded
  (`TowerGameTests.keepLoaded`). And the wing turrets added in r192 took the spin-up test's zombie too, with no barrels
  to spin, their bolt there first as often as not: that zombie now stands where only the port door gun bears. Each of
  the four runs in a batch of its own and clears stray hostile mobs first. No fault in the ship's guns.

## The first-floor look-over: faced walls, the line carried round the rooms, the gaps closed (Justin, 10 November 2026; layout 49; r198)

Justin: "Do a look over the first floor of the drone tower. If you can, make some suggestions on how we can make the
layout look nicer. Currently there are gaps between rooms and some of the walls don't really look that great with the
theme of the building. I am not sure how to make everything flow together and look less messy." The look-over's three
sheets showed what read as messy and what to do about it; from them he picked finish A (concrete, like the pillars),
both gaps closed, all three extras, and "Build it".

What was wrong, as built (layout 48):

- **Two kits met.** Every ground-floor room's lower wall (y 1..4) was the nuclear plant's cream painted block, under the
  tower's steel band and dark carbon skin and beside the concrete pillars. (The data hall and the cafe fit already:
  their fronts are glass.)
- **A slot one block wide** (z 20, x 30..39) between the security office and the battery room, the only way to the
  battery room's doors (x 36..37 in its north wall). It dates from the office's move to the curtain wall in layout 40.
- **A dead-end wedge** (13 cells) between the restrooms' west wall and the curtain wall, beside the stair's foot; and a
  slot two wide between the goods lift's glass house and the curtain wall.

What layout 49 does (`tools/tripillar_rooms.py`: `close_gaps` with the rooms, `hall_side` as the last thing the control
building does; `tools/faced_walls.py`):

- **Faced walls.** The rooms' walls are one block thick, so a wall can't simply be made of concrete: the room behind it
  would get the concrete too. Two new blocks do it (`tower/FacedWallBlock`, `tower/FacedLineBlock`; one pair per
  material in `faced_walls.MATS`, concrete for now):
  - **Concrete-Faced Wall** (`faced_wall_concrete`): `facing` is its face onto the hall, drawn as the concrete beside it
    is (its `phase`, the two-metre sheet running on from block to block); the face opposite is the room's (`inside`:
    `painted`, or the plant restroom's `tile` or its `tile_cap` course, so the washroom and restroom keep their tiles);
    the rest is painted. An opaque full cube.
  - **Neon Line (Concrete-Faced Wall)** (`faced_line_concrete`): the same wall with the Neon Line's groove and tube
    along its face (the Neon Line's own body and fittings, so it is the pillars' line exactly), every other face
    painted: the room never sees it. Its tube runs on into a line beside it (`left`, `right`, as you look at the face:
    another faced line or a Neon Line) and ends in a cap where there is none; a Neon Line takes a faced line as a line
    too (`NeonLineBlock.sideOf`), so the line turns a room's corner onto it. Light 12, as the lines.
  - Models: the wall's from the material's phase models, the line's body from the Neon Line's with only that side
    open, its outside faces repainted; the fittings are the Neon Line's (`neon_line_fit_*`). 129 models, written by
    `python3 tools/faced_walls.py` with the blockstates, items, loot, names, tag and recipes (Advanced Workbench: a
    painted wall and a block of concrete make 4 faced walls; 6 faced walls, glowstone and orange dye make 12 lines).
    They come free with the foundation (not among `TowerCosts.CHARGED`): it is paid 239 fewer painted walls and 112 more
    concrete than layout 48.
- **Which walls** (`faced_walls.face`, a rule, not a list: any room added later is faced the same way). The hall's air
  is filled from the entrance through everything that isn't a wall, glass or a door (`hall_air`: so not the rooms).
  A painted or tiled wall block at y 1..4 with exactly one face on that air and a room on another becomes a faced wall
  (a tiled one only if its tiles face the other way); with the hall on two sides, or no room behind it (a corner, a
  pier, the closed gaps), plain concrete. A face beside a glass front counts as on the hall (the reveal), the building's
  own curtain wall never. 160 faced walls, 53 faced lines, 32 concrete, 10 corner lines.
- **The line.** At y 3, the height of the pillars' line (`core_feet`): the faced line or, on plain concrete, the
  concrete Neon Line, wherever the hall's cell in front is empty. It runs over the doors (0.17 above a sliding door's
  track cover) and stops at glass with a cap: **framed glass is a wall to a Neon Line now** (`NeonLineBlock.sideOf`,
  `neon_lines.side_of`), so a line never turns onto the reveal beside a pane, where the room behind the glass would see
  it. No line anywhere else in the tower changed shape by that (checked over all nine tiers).
- **The gaps closed** (`close_gaps`): the slot and the wedge filled as the rooms' walls stand (concrete to the band,
  the band, the skin on up to the hall's ceiling), so the security office and the battery room are one block and the
  restrooms run flush to the stair, whose foot (z -30..-29) stays clear.
- **The battery room's doors** are in its west wall, on the hall (the pair at x 24, z 23..24, where the room is clear
  inside; plaque on the band over them, NO SMOKING beside them). Its old doorway is wall.
- **The line on the stair** (`faced_walls.line`): along the stair's concrete side from where the wall is a block higher
  than the line (x -27) to its end, which stands flush with the restrooms' wall, so the line runs from that wall round
  the stair's end and back along its side. The same pass puts the line on the closed gaps' faces. (Only those: the
  pillars' faces onto the rotunda carry the video walls, not a line.)
- **The goods lift's nook closed**: its house's north wall carried on to the curtain wall, a pane of its glass and a
  steel post like the curtain wall's mullion (glass in both cells would have joined the curtain wall's own pane and
  taken its frame away).
- **Benches and plants** along the long blank walls, clear of every door's leaves: the lounge's sofas, backs to the
  wall, three either side of the locker room's doors and four along its east wall, four along the battery room's north
  wall and three south of its doors, a plant at each end. The crew use them: the planner (`tripillar_crew`) gives each
  post the nearest seat, so dispatch, supply, the alarm desks, the data hall's technicians and the east janitor take
  their breaks there, and four of them doze there at night (the night lights over them go to low). The hall patrol's
  third spot moved two blocks north, off the bench.
- Layout 49 (`TowerData.TRIPILLAR_FIRST_FLOOR_LAYOUT`; 48 frozen as `tower_tripillar_v48.json.gz`, built towers refitted
  on load). Against layout 48: 524 of the foundation's cells differ, none in tiers 1 to 9.
- **Tests**: `TowerGameTests.assertFirstFloorHallSide` (run with the ground floor's rooms by the foundation test and
  the three refit tests that check them: the locker room's faced wall and its line running on both ways; the washroom's
  wall with its tile and cap course inside; the corner's line turning with the faced lines as lines to it; the line
  stopping at the security office's glass; the slot filled to the band and the skin with its end carrying the line; the
  old doorway wall; the wedge filled and the stair's foot clear; the stair's line; the lift's nook; a bench and a plant;
  every faced line as `FacedLineBlock.shaped` gives it and every faced wall and line with its place's phase; 53 and
  160). The doors' test has the battery room's pair walking in east, and over a steel door the line and the faced wall.
- The pictures (`ot/blocks.py`, the look-over's renderer) now give a block its phase where it stands, so concrete
  draws as the game draws it; the built floor was rendered from the layout itself with the game's own models.
- Not run on a client here: the two blocks' models were drawn by the same loader the look-over used, not by the game.

## The upper floor's walls: the same finish on the gallery, the pillars' line upstairs (Justin, 10 November 2026; layout 50; r199)

After the first floor was built Justin picked "Upper floor walls" as the next thing. Three sheets showed the upper
storey as built, the ground floor's finish run on it, and one question (should the pillars up there carry the line
too); he answered "Build it" and "B: pillars too".

As built (layout 49): the upper storey's rooms (slab y 11, walls y 12..15) showed 792 cream painted blocks to the
gallery and its corridors (the west corridor between the training room and the offices, the two beside the lounge),
under the same steel band and dark skin as downstairs; and there was no line at eye height up there at all (the lines
ran at y 3, on the slab's edge at y 10 and at the roof).

What layout 50 does (`tools/tripillar_rooms_upper.py`: `gallery_side`, the last thing the control building does, after
the ground floor's `hall_side`):

- **The same rule, not a second one.** `faced_walls.hall_air` fills the gallery's air from four cells of it (the upper
  patrol's spots, `GALLERY_SEEDS`) through everything that isn't a wall, glass or a door; `faced_walls.face` then does
  to y 12..15 exactly what it does downstairs: a painted wall block with one face on that air and a room behind it
  becomes a Concrete-Faced Wall, a corner or pier that shows on two sides plain concrete, and three blocks up (y 14,
  `tripillar_rooms.LINE_Y` over the floor, as on the ground floor) the faced line or, on plain concrete, the concrete
  Neon Line. 552 faced walls, 189 faced lines, 39 concrete, 12 corner lines; no painted block is left facing the
  gallery. The rooms inside keep their paint; the map room's carbon walls, the briefing room's acoustic panels and
  every glass front are as they were (only painted blocks are touched). The line runs over the doors and stops at
  glass with a cap, as downstairs.
- **The pillars carry it too** (his pick B): `faced_walls.line` over the three pillars' own cells at y 14 puts the
  concrete Neon Line on every pillar face that looks onto the gallery (121 blocks), so the line reads as one round the
  floor, from pillar to room wall and on. (Downstairs the pillars had theirs already.)
- **A guard for both floors** (`tripillar_rooms.rooms_shut`): the generator stops if the hall's or the gallery's air
  reaches into any room (a missing wall or door would make the rule take a room's own side for the hall's).
- Layout 50 (`TowerData.TRIPILLAR_UPPER_WALLS_LAYOUT`; 49 frozen as `tower_tripillar_v49.json.gz`, built towers
  refitted on load). Against layout 49: 913 of the foundation's cells differ, all at y 12..15, none in tiers 1 to 9;
  no crew post and no night light moved. The foundation is paid 792 fewer painted walls and 82 fewer concrete (the
  faced blocks and the lines come free, as in layout 49).
- **Tests**: `TowerGameTests.assertUpperFloorGallerySide` (run with the upper floor's rooms by the foundation test and
  the refit tests that check them): the west corridor's two walls faced towards it with the line joined both ways at
  y 14; a pillar's line over plain concrete; every line upstairs as its neighbours shape it, every faced wall and line
  with its place's phase; 552, 189 and 133 (the pillars' 121 and the corners' 12). `assertFirstFloorHallSide` counts
  the ground floor's own (y up to 4). The training room's end walls are checked as walled, whichever finish
  (`walled`).
- **The tower diorama's texture.** The diorama's mesh gives each quad a place in its texture's palette, so the two files
  are one thing. r198 shipped the new mesh with the old texture (it was put back as "the same picture, only encoded
  differently", which was true of two other files and not of this one): four of its 140 colours had changed places, so
  15 of its 25,529 quads would have drawn a neighbouring grey or brown. r199 ships the pair as generated, and the
  generators now leave a file alone when what it holds hasn't changed (`tools/keep_same.py`: PNGs by their pixels,
  gzips by their contents; used by `tower_tripillar.py`, `atrium_dome.py`, `tower_diorama.py`), so nothing has to be
  put back by hand again: a file that changed on disk has changed in content.
- Not run on a client here: as with the first floor, the pictures were drawn outside the game from the layout itself
  with the game's own models.

## The drop ship's hull pass: one paint over the whole outside, the fin's root, nothing left in one plane (Justin, 10 November 2026; r200)

Justin, on his first run of the game since r180 (r199 loaded): "Do a pass over the hull on the ship. There's still
issues with the texture, like here at the wing on top. See what you can improve.", with a picture taken on the roof at
the tail fin's foot and one of the striped bar under the bay's seats (of that one, asked: "z fighting"). Three sheets
(now and fixed: the fin, the wings and nose, the stripe and the stern); he picked "Build all of it" and, for the fin's
root, "Panels like the roof" over the sheet's plain casting.

What was wrong, measured over the whole ship instead of by eye (`tools/crew_ship_seams.py`, below): every part of the
outside carried the hull's panels its own way (the skin by its girth and its length, fairings by rules that carried the
skin's pattern outward along one direction, everything else flat on each face's own plane). 1,141 square blocks of
paint were not at their true size (the wings' tops stretched nearly three times, in zigzags, where a face had been cut
into strips and each strip laid by itself; the nose, where the girth shrinks under the same panels, its screws ovals;
the gun bays' boxes; the stern's cheeks; the roof ridge at half size: its fairing paint was the hull's texture laid
over half the blocks), and along 261 blocks of line the panels of one face didn't carry on into the next. At the fin:
a fillet each side and a strake laid through each other, the fillets' tops standing off the fin's thin ends (an open
slit at the trailing edge), their feet overhanging the ridge's sides.

What r200 does:

- **One paint for the whole outside** (`tools/crew_ship_paint.py`, `Ship.paint`, the last thing `Ship.build` does;
  for whatever is built on the hull later too):
  - The troop box (from `BODY_FROM`, where every station has one section) keeps the skin's own mapping, which is exact
    there, and **anchors** the rest.
  - Everything else painted is **unfolded** from it as sheet metal would be bent: two faces that meet within 50 degrees
    (one surface to the eye, and to the shading) share their panels across the line where they meet, each at its true
    size. A corner of one face lying on another's edge breaks that edge there, so pieces cut round an opening, or a
    wing's strips along its outer panel, are one sheet.
  - A part that only **stands on** another (a fairing's foot on a facet, a face's corner on another's) starts where it
    lies flattest against what it touches and takes the panels there; the skin grows outward from what is laid, and
    what touches nothing laid starts from its largest face as that lies flat on its own plane (as it was drawn before:
    flat parts look as they did).
  - **Seams** where a shape can't be flat: the keel and the ring where the nose leaves the troop box (`PAINT_CUTS`);
    the nose's chines (a crease of 18 degrees or more at a corner whose angles don't add up to a full turn: each strip
    of the nose is its own strake of plates, with round screws); and wherever the unfolding comes round and meets
    itself out of step by more than a break would show (a tube, a dome: found by unfolding, cut there, unfolded again).
    A quad is never cut along its own diagonal.
  - Then every triangle is asked to be a turned copy of its own true shape and the corners are solved for together
    (as-rigid-as-possible), which spreads what stretch a dome still needs thinly.
  - `PETALS`: faces of a faceted fairing that each carry the panels of what they stand on; the paint is laid over the
    creases inside a group, never from one group to another (the fin's root, the roof ridge's top against its sides).
  - The **far model** takes the near model's panels on every face it shares with it (1,872 of its 2,174 painted
    faces, to the texel), so nothing shifts when the game swaps them.
  - Result: 1.3 square blocks a little off (the turret's mound, the sensor's hump), 58 blocks of line left (where the
    sponsons' fairings, the wings' outer panels and the fin's root stand on a face they don't run on from). Parts that
    move (gear, guns, thrusters, doors) and the bay's fittings keep their own.
- **The fin's root** (`Ship.fin`): one fairing each side instead of the fillet and the strake: its foot the roof
  ridge's top edge, its top the fin's own section three quarters of a block up, so it stands on the one and closes on
  the other all round; ahead a ridge to the turret's plinth, astern closed behind the trailing edge. Panelled like the
  roof (his pick): each of its faces bent up from the ridge along its own foot, so the seams run with the roof's.
- **The roof ridge** stands on the roof (its feet and its ends lay under the skin, where the paint had nothing to carry
  on from), in the roof's paint at the roof's size, sides at 52 degrees as before; its top with its end slopes is one
  sheet, started from the roof at its ends. For the game's collision shell it keeps its old outline: the top turret's
  dead ground is that shell, and with the feet on the roof `dropShipDoorGunsSpinUpStowAndPlayersSit` failed (the turret
  reached the target that only the door gun should).
- **Nothing left in one plane** (`tools/crew_ship_planes.py`, `Ship.unfight`, before the paint): two faces in one
  plane, turned the same way, one over the other, flicker (z-fighting). Under the seats the strut feet lay flush in the
  striped kick bar's face, all along both walls (70 pairs); the finder turned up 198 pairs in all (the pilots' seats'
  shells against their cushions, the gear doors' seals, the ramp's lip, the steps' treads, fittings in the gun bays).
  The feet now stand 2 cm proud of the bar; and for everything, now and later, the build stands the smaller face of
  every such pair 8 mm off the other and looks again until none are left (124 faces moved, 0 left).
- **Striped bars' thin edges are plain** (`box_faces`, `HAZARD_EDGE`): each edge carried the stripes at its own place,
  yellow teeth against the black of the face.
- Generated files are written through `tools/keep_same.py` here too. Only `crew_ship/ship.bin.gz` and
  `data/jugcraft/crew_ship/collision.bin.gz` changed (the shell: 158 cells gone and 86 new, all at the fin's root but
  two at the ridge's forward end); no texture, no Java.
- **Checks** (run by `tools/check_mod_data.py`): `crew_ship_seams.check` (the hull's paint within 6 square blocks off
  size and 75 blocks of broken line) and `crew_ship_planes.check` (no pair of faces in one plane).

## Two faults from the first real play test: the game closing as a ship lands, the black plasma bolt (Justin, 10 November 2026; r201)

Justin, playing r199 (the first run of anything since r186 in the game itself): "the projectile of the plasma gun is
black for some reason, also my game crashed while the ship was landing".

- **The crash** (`crash-2026-11-10_09.30.04-client.txt`: `IllegalStateException: Tried to access entity ID before ID
  assignment`, from `CrewShipRenderer.riders`). The ship's seated crew are drawn from one stand-in crew member that is
  never added to the world. Since 26.x an entity has no id until the world gives it one and asking for it throws;
  drawing a living thing asks (its held items are resolved by it). So the first frame a ship with crew aboard came into
  view closed the game. Now every entity the client makes only to draw gets an id of its own (`client/StandIns.id`,
  below any the server hands out): the ship's stand-in, and the frozen copies of `jimtafacts/JimtaFrozenRenderer`, the
  one other place that had none (the five others set one already). `check_mod_data.check_stand_ins` keeps to it: client
  code that creates an entity gives it an id or adds it to the world.
- **The black bolt.** The bolt's glows were written as added light: bright on black textures with no see-through part,
  dimmer colours for fainter light. The game's eyes layer, which they are drawn on, lays its picture over the scene by
  its opacity (26.3's `RenderPipelines.EYES` blends `TRANSLUCENT`; read from the game's own class), so the black was
  drawn. (The pictures here were made by a renderer that did add them.) Now a glow's strength is its opacity
  (`client/Glows.added`: its hue at full brightness, as opaque as it is strong), in the colour the bolt's quads are
  drawn in and in its two textures (`plasma_glow.png`, `plasma_filament.png`), converted by `tools/glow_alpha.py`.
  The power armour's glow sheet was painted the same way (opaque, black wherever a tile doesn't glow) and is converted
  too, in place and in its generator (`tools/power_armor.py`): its glowing faces' dark texels would have been drawn
  black over the armour. `glow_alpha.check` fails on any glow texture that is opaque all over and largely dark. The
  mod's other glows already carried their strength as opacity (`CrewShipRenderer.colour`, the crew's and creatures'
  glow masks).

## The crew find their way: the tower's walk map (Justin, 10 November 2026; r202)

Justin, playing r201: "I saw a bunch of NPCs that should have been inside walking outside ... I only saw like 2 NPCs
upstairs, and they're on the simulators", with pictures of a controller, the cook and a server standing outside against
the walls.

**What was wrong** (measured with the crew's own path finder on a built tower: the whole crew put down outside the gates
at midday, four minutes watched): 28 of 47 reached their posts, seven stayed outside against the walls, and nobody
whose post is upstairs got upstairs. Three causes:

- **The game's path finder looks a bounded way** (`TowerCrew.PATH_LENGTH`, a few thousand cells) and, when it doesn't
  find the whole walk, hands back the way to the cell nearest the goal in a straight line. In a building that is the
  wrong place: off a drop ship, the outside of the wall nearest the post (the cook outside the kitchen's wall, the
  servers outside the cafe's: his pictures); back from a break downstairs, the hall floor right under an upstairs desk.
  With a player near nobody is put back (that rule stands: "I see the people like teleporting around a lot"), so they
  stayed there.
- **A spot upstairs counted as reached from the floor under it**: a station's and a patrol's spot were tested across
  the floor only, not by height (`WorkGoal.station`, `patrol`), so the engineers, the medic and the pilots at the
  lectern stood in the hall below their rooms and took themselves to be at work.
- **Shut speed gates and the data hall's aisle door were walls to the path finder.** A gate's lane was a way through at
  the feet but the cell over it counted as the top of the gate to stand on (which the crew don't: `CrewPaths.onAPiece`),
  so no walk was planned through a shut gate; they only got through once someone stood near enough to open it. The
  Aisle Containment Door opened for players only and never said it could be planned through, so the technicians had no
  way into the cold aisle they work in.

**What is built**

- **The walk map** (`tower/crew/CrewRoutes`). Each tower has a map of every cell the crew can stand in round it and
  every step the game lets them take from one to the next, found by the crew's own path finder's rules
  (`CrewPaths.Evaluator.getNeighbors`: the doors that open for them, the gates' lanes, the stairs, not over the
  furniture), from every post outwards to 40 blocks round them. It is made a little each tick (at most 3 ms of one;
  some 43,000 cells round a Tri-Pillar, about half a second of work in all, logged when done), kept ten minutes, and
  made again at once when the tower is refitted or another tier is built. It knows nothing of the tower's rooms: it is
  whatever the path finder says can be walked, so it holds for any layout, any tier and whatever a player builds in the
  way.
- **Long walks go by it** (`TowerCrew.wayPoint`). A walk of more than 12 blocks, or to another floor, is planned over
  the map (no bound but the map's size) and walked as way points six cells apart, each a short walk the game's own path
  finder plans with ease; a way point they can't get to has the walk planned again from where they stand. Everything
  the crew walk goes through it: to work, to a break, to bed, to the janitors' carts (`WorkGoal.walkTo`), off a drop
  ship to the entrance and back in from the ramp's guard (`walkLong`). The patrols outside keep to their own round,
  laid out point to point (the map's shortest way from one point to the next can be through the building). Where the
  map has no way to give (still being made, or they are out past its edge) they walk as before and ask again.
- **Arriving on the right floor**: a station's or a patrol's spot is reached only on its own floor (`onFloorOf`).
- **Doors and gates planned through**: the cell over a gate's lane is head room (`CrewPaths.Evaluator`); the Aisle
  Containment Door says it can be planned through and opens for anyone the sliding doors open for
  (`SlidingDoorBlockEntity.opens`: players, and walkers that aren't hostile).

The same measure with the walk map: everyone in through the entrance within half a minute, the upstairs crew at their
posts within two, nobody put back.

**The spread he asked about** (not changed in r202; his picks are r204, the section after the next). By the posts as laid out then: 31 work on the ground floor,
11 upstairs (four pilots, four engineers, the medic, the manager, a patrol), two on the roof, three outside. The cafe
downstairs has the cook, two servers and two who sit there all day (`cafe_break_a`, `cafe_break_b`); the kitchen
upstairs has no post. By day six or seven are on a break at any time, two to five of them on the lobby's sofas (fifteen
of the ground floor's crew take their breaks there); at night 25 sleep, eight on those sofas. Much of what he saw was
the upstairs crew unable to get back up.

## The plasma baton rebuilt as real 3D gear (Justin, 10 November 2026; r203)

"Make the plasama baton look better." Four sheets (how it was, and three ways to rebuild it); he picked **A, "Patrol"**.
The baton is now one thing, the item, made of round moulded parts, and the guards carry that same item; its plasma is
drawn the way the rifle's bolt is.

- **The item** (`tools/plasma_baton.py`, with the new `tools/item_kit.py`). In its own frame (x across, y up, z along
  it from the middle of the grip; a sixteenth of a block): a steel stud, the pommel, a lit ring, a knurled rubber grip
  with five moulded rings, the guard, a tungsten emitter housing (a lit band, the safety switch and three charge lamps
  on its top flat), a bezel, a ceramic insulator of two sheds, a slim rod through three focusing rings, a slotted head
  and its end cap; 15.5 px long. Three models from one description and one 128x64 texture: `plasma_baton` (lit: the rod
  hot, the band, ring and lamps full bright), `plasma_baton_off` (the rod out, cold) and `plasma_baton_stowed` (the rod
  telescoped into the handle, its three rings stacked on the insulator under the head; 10.6 px long). The item
  definition (`items/plasma_baton.json`) picks by custom model data ("stowed", "off", "lit": a guard's, set by the
  renderer); with none (a player's) it is the lit model with the plasma over it, `flat` in a menu's slot.
- **`tools/item_kit.py`** (generic: any item built this way). Parts as boxes in the item's own frame, round sections as
  eight-sided prisms (`Kit.prism`, `Kit.lathe` for a run of sections end to end), each face painted once into one
  texture (`Atlas`; flat colours share a 2x2 swatch), `elements()` writing the model. A prism's sides are four boxes,
  two turned 45 degrees, only their outer faces drawn; a capped end is a cross of three boxes with four corner pieces
  standing 0.11 px back, so no two faces that look the same way ever lie within 0.09 px of each other, and `check()`
  asserts that with the repository's own rule (`tools/zfight.py`) on every model written. Painters: `flat`, `turned`,
  `knurl`, `ribbed`, `slotted`, `dots`.
- **The plasma** (`client/BatonPlasma`, with `client/PlasmaGlows`: the bolt's soft glows, now shared with
  `PlasmaBoltRenderer`). Glows on the eyes layer, each turned to whoever looks: the air round the rod (one long glow and
  four swells that wander), an uneven sheath of seven overlapping lengths that swell and thin on their own, a hot core,
  a bloom round the wider head with a veil of it in front, a glow at the emitter's mouth, 26 fine streamers standing off
  the rod (new every other tick) and four arcs (new every tick); about 115 quads. What wraps the rod is drawn 0.56 px
  towards the eye so the rod, rings and head don't cut it in two. It is drawn in the item's model space, so one piece
  of code serves the item anywhere (`client/BatonPlasmaItem`, a special model renderer `jugcraft:baton_plasma`) and
  the guards. Where the eye is: the world and a hand are posed about the camera, so it is the pose's origin brought
  back through the pose; a menu draws square on (the item's `flat`, or an orthographic projection in force: the figure
  in the inventory), so there the look is straight in and both faces are drawn; a mirrored pose (a left hand) turns
  the winding over.
- **In a guard's hand and holster** (`TowerCrewRenderer.BatonLayer`, `TowerCrewPropsModel.toBaton`,
  `toBatonHolstered`; `crew/props.json` keeps only where: the groups `baton` in the right fist and `baton_holstered` on
  the holster's mouth, no boxes). A crew pixel to the item's, so a guard's baton is the players' to the pixel. Its
  plasma grows out of the emitter as it lights (`batonExt`) and the model turns from off to lit once it's half out.
- **Why it is carried stowed.** At its full length the baton is longer than a leg (15.5 px against 12). A leg turns
  about the hip joint, so whatever of a thing strapped to the thigh stands above that joint swings the other way, into
  the guard's back, at every stride: the full-length grip would have stood 4.5 px above it. So the rod telescopes into
  the handle for carrying and the holster sits low: the whole handle below the hip joint but the butt stud. On the draw
  the hand takes it stowed (the catch, 4 ticks), the rod is thrown out as the arm comes up (`PlasmaBaton.DRAW_OUT`, 6),
  the plasma lights (8 to 13); putting it away the plasma goes out (0 to 4), the rod goes back in
  (`PlasmaBaton.HOLSTER_IN`, 6) and it is in the holster at 8. (As first built the holstered grip was drawn the wrong
  way up inside the sheath, so this never showed.)
- **The holster** (`tools/crew_skins.py` `HOLSTER`, `HOLSTERED_AT`): a cup from mid thigh to above the ankle that
  takes the stowed baton from its guard to its end cap, a steel clip round its mouth (the baton's guard rests a tenth
  inside it: never a shared plane), a strap round the thigh and one under it, each joined to the cup by a hanger, and
  a drop strap up to the belt.
- **Held by a player** (`display()` in the tool): in third person the middle of the grip is in the fist exactly where
  the guards hold theirs, pointing ahead of it, its top up the arm; in first person up and in from the lower right, the
  switch and lamps towards the eye (the sheet's hold); in a menu on the slant.
- **The cut** follows the new plasma: `PlasmaBaton.SWING_INNER`, `SWING_OUTER` 0.66 to 0.94 blocks from the shoulder.

## The crew's spread: staff for the kitchen upstairs, half the breaks (Justin, 10 November 2026; r204)

His report: "a bunch of them sitting on the couches downstairs ... people just sitting down there doing a whole bunch of
nothing ... it doesn't seem evenly spread ... a bunch of people in the kitchen down there, but not ... in the kitchen up
here as far as like servers and stuff go". Shown the numbers (the end of the walk map section), he picked "Move staff up
from the cafe" (one server and one of the two who sit, no new crew) and "Keep sofas, fewer breaks" (half as many).

- **Two move up** (`tools/tripillar_crew.py` `POSTS`). The second server (`cafe_east`) waits on the crew lounge's
  dining tables upstairs: down the aisle by the kitchen island, back between the two rows of tables, round the first
  row. The technician with his mug (`cafe_break_b`) sits at the crew kitchen's island, on its second bar stool, facing
  it. Both posts keep their ids: a tower's crew are tied to their posts by id, so the same two people walk up (over the
  walk map) where a new id would have had them dismissed and replaced. The cafe keeps its cook, a server and the
  mechanic with his mug. The two sleep upstairs as the upper floor's crew do (the lounge's two recliners; the simulator
  pilot who had one takes a lounge sofa). Posts by floor: 29 on the ground floor, 13 upstairs, 2 on the roof, 3 outside
  (31, 11, 2, 3 before).
- **Any rest seat can be a post's seat** (a dining chair, a bar stool, a sofa): `kit_seat` gives its point and the way
  its sitter faces as it does a room kit chair's (`rest_point`), and the game sits them there (`CrewPost.seatAt`).
- **Half the breaks** (`BREAKS`: how many a post takes on each day of the rota). One a day, and every other post has one
  day of the three without: 88 breaks over the three days where there were 175 (each had one a day and a second on two
  days of the three). Away at a time by day: about 3 (5 at most) where there were about 6 (8 at most); on the lobby's
  sofas about 1.3 at a time where there were 2.5. The sofas stay break seats. `schedule` and `plan_day` plan whoever
  has a break that day; the game's code is unchanged (the layout's `crew` list carries each post's breaks).
- **No layout version bump**: no block changes. A tower reads its posts from the layout's data, so standing towers have
  this at once: their server and technician walk upstairs, everyone's breaks thin out.
- Test: `crewRoutinesAreSound` (two or three breaks each over the rota and never two in a day, 88 in all, never more
  than five away at once on any day; the two who moved are where they should be, the three who stay are downstairs).

## What the tower pays from, the tower storage by hand, the lounges' TVs, repair, and sound past furniture (Justin, 10 November 2026; layout 51; r205)

His report after r204: a Murmur shot through the head showed no wound there; the drop pod screen said there weren't
enough resources with a chest full of them piped to the Cargo Exchange ("One of the biggest points of this mod is so you
can take stuff in your storage and then have the drone tower use it"), and nothing could be put into the tower's
storage by hand; a few of his tower's blocks broken by accident, and the Cargo Exchange; the upstairs sofas off centre
from the TV, the old TV downstairs ("I don't want to use that shitty TV at all, anywhere. It should be actual TVs"); the
TV's sound cut standing by the upstairs sofa. His picks: read storage through pipes, take from a warehouse at once, a
storage screen, and the Lounge TV removed.

- **A creature's own parts take wounds** (`tower/crew/PlasmaWounds` `partOf`, `hitPart`; `client/PlasmaWoundsMesh`
  `PART`). Some of the menagerie's beasts are drawn in pieces: the Murmur's head is an entity of its own, drawn by its
  own renderer, so a bolt through it hurt the Murmur but the wound was looked for on the body. A hit on a part drawn
  apart (not the dragon's, which are hit boxes only) now burns the wound into that part, in its own frame, and its
  renderer cuts it as a body's is (the head's hair too); a sweep or thrust that crosses parts marks each. Test:
  `plasmaBoltWoundsACreaturesOwnPart` (a real bolt fired at a Murmur's head).
- **What the tower pays from** (`tower/TowerSupply`). A tier, the first layer, a drop pod squad: everything the tower
  buys is checked and paid from, in turn, its own storage, the depot's packager, every inventory hooked to a Cargo
  Exchange bound to it (next to its marked port, or at the far end of item pipes from it: `logistics/ItemNetworks`
  `inventories`) and every automatic warehouse hooked the same way (its racks' free pallets, taken at once:
  `warehouse/TerminalBlockEntity` `free`, `takeNow`). All or nothing: nothing is taken unless all of it is there. A
  double chest counts once; only plain items count (no names, enchantments or damage). `tower/squad/Requisitions` and
  the core's tier and first-layer paths all use it.
- **The tower storage by hand** (`tower/TowerStorageMenu`, `client/TowerStorageScreen`): right-click the Cargo
  Exchange and a chest's grid of what the tower holds opens in the tower screens' colours, six rows of nine scrolled
  with the wheel or the bar, how many of its 540 stacks are used and the next tier over it. Click to take a stack
  (right-click half), put anything plain in by clicking it onto the grid or shift-clicking it from the inventory, LOAD
  BLOCKS puts in the inventory's building blocks. Test: `towerStorageTakesAnythingByHand`; paying from piped chests:
  `dropPodsArePaidFromChestsPipedToTheCargoExchange`; from a warehouse: `WarehouseGameTests.aTowerTakesFromTheRacksAtOnce`.
- **The lounges' TVs** (layout 51; `tools/tripillar_rooms.py` `flat_tv`, `cafe`; `tools/tripillar_rooms_upper.py`).
  Downstairs, the break area's Lounge TV is now a Flat Screen TV three wide and two high on the east wall over the media
  console; upstairs the sofas, rug, coffee table and floor lamp moved one block south, centred on the Flat Screen TV.
  Anyone's videos and streams play on both.
- **The Lounge TV retired** (`tower/RetiredBlocks`; `tools/room_kit_recipes.py` `RETIRED`). No recipe, not in the
  creative menu; the block stays registered so worlds with one load, and any in the world turns into a working Flat
  Screen TV when its chunk loads or when one is put down (an old stack, a command): facing the same way, two high where
  the row above is clear, hung on what's behind it or standing on its foot. One call retires any block the same way.
  Test: `aLoungeTvBecomesAFlatScreenTv` (both ways, nothing dropped).
- **Repair** (`/dronetest repair`, operators; `TowerCoreBlockEntity.repair`). Puts back every block of what the nearest
  tower has built that is gone (air, water, grass where the design has a block), leaves a block someone put in its
  place, then pins the displays, owns the doors, links the hall and binds the Cargo Exchanges again, as when a tier
  finishes. Standing towers get the same on load this time: layout 51 makes every tower refit, which puts back what is
  missing. Test: `repairPutsBackTheTowersMissingBlocks` (two walls and the whole Cargo Exchange back and bound, the dirt
  left, a second repair with nothing to do).
- **Sound past furniture** (`sound/SoundWalls`, moved out of `client/SoundOcclusion` so the server's tests can check
  it). A block that doesn't fill its cell (a sofa, a slab, a door, a TV's panel) counted a third of a wall wherever the
  sound's line crossed its cell, over the seat or in front of the panel too; it now counts only if the line goes
  through its shape. Test: `soundGoesOverWhatIsntInTheWay` (over a slab nothing, through it a third, stone one; a 3 x 2
  Flat Screen TV heard plainly from in front of it). The sound stopping altogether was with one YouTube video (a stream
  played fine); his log shows that video starting over and over as HLS, nothing of ours failing.

## Drop pods: real sounds, staggered landings, wider and clear flight paths; quicker boarding (Justin, 10 November 2026; r206)

His notes from playing r205: "the sound on the drop pods coming in sounds so fucking bad stop with the high pitched
retro slop ... take sounds from like real life re entry of stuff or a simulation of such sounds"; "make sure to add real
rocket sounds for the boosters when they kick in"; the pods "disappear if you look at the wrong angles"; "make sure the
pods don't run into each other on the way down"; "the ships shouldn't land in sync, it should be randomized within like
7 seconds total time for the landing, and they should have more spread out flight paths while still landing around as
close as they are now"; the soldiers slow to board the pickup; "i want the ground impacts to be off real sounds as well".
Two takes went to him (A: the descent from real NASA launch recordings; B: simulated to match them); he picked A and
asked for real booms and impacts, the browser on his PC fetching the recordings.

- **The sounds** (`tools/drop_pod_sounds.py`, its sources and licences listed at its top; `client/DropPodSounds`). The
  old made-up shriek (a sine sweep on a 7 second loop over a 17 second fall) is gone. A pod's roar follows it down in two
  layers, both real: far, the Atlas V's distant launch rumble; near, the Shuttle's roar and crackle (NASA's historical
  sounds), the near one taking over inside about 150 blocks the way the air takes the highs out of a far sound; louder
  as it nears; pitched by the Doppler shift of its speed toward the listener. A moment after it sets off its sonic boom:
  a Falcon Heavy's two side boosters coming home, recorded from the NASA Causeway at the Roman Space Telescope launch
  (their triple boom and the rumble of their landing burns), at one of three pitches. The retros kicking in: a Falcon 9's
  engines lighting, from a pad camera's microphone at the Crew-13 launch, cut off as the pod hits. The impact (three
  takes): NASA Langley's gantry drop of an air taxi body onto its impact pad, with a heavy metal thud and a heavy bag on
  dirt (CC0, Freesound) a little slowed, and the dirt and stones falling back. Where a microphone ran out (the booms'
  and the ignition's peaks) the wave is carried on by a cubic, not left squared off. The hatch, the self-destruct and the
  designator's chirp are as they were.
- **Staggered landings** (`DropPod` `delay`, `setsOff`, `land`, `FLIGHT`, `SPREAD_TICKS` 140). Each pod sets off at its
  own time, so the squad lands over seven seconds in no set order, at least 12 ticks apart.
- **Wider paths, never meeting** (`SquadDrop.plan`, `clearance`). The leader's pod sets off from the flare, the others
  from their own points in the sky about it: up to 60 blocks across the way they come, 30 along it, 28 up or down. Their
  landing spots are as before. Each pod's point and weave are drawn again (up to 48 times) until it stays 3.5 blocks clear
  side to side, or 6 above or below, of every pod already planned, over the whole time both are in the air.
- **Pods no longer vanish** (`DropPodRenderer.getBoundingBoxForCulling`). The entity stands where it will land, so a
  culling test of its own box (Sodium tests every entity's) hid a falling pod whenever its landing spot was out of view.
  The box tested is now round the pod where it is drawn and its trail, and the flare while it shows.
- **Quicker boarding** (`CrewShip` `PICKUP_HOLD_NEAR` 6, `PICKUP_EVERY` 10; his pick "Board faster"). The pickup now
  holds only for a monster within 6 blocks of the ramp's foot (14 before) and calls the next soldier 10 ticks after the
  last is on the ramp (30 before); a soldier in a fight is called all the same.
- The designator's countdown is to the first pod down, and keeps the mark through the last.
- Tests: `podFlightsAreSpreadStaggeredAndNeverMeet` (a hundred drops of every size: the landings span 140 ticks with
  none together, each lands on its spot, no two ever within reach, the points in the sky spread over 60 blocks),
  `podsLandApartSoldiersOutPodsBlowWhenClear` (now with staggered landings), `podSoundsAreAllInPlace` (every pod sound
  event registered and its files in the mod, the scream gone), `sentHomeAShipComesTheyBoardAndItLeaves`.

## Drop pods: the hatch and the self-destruct from real recordings too (Justin, 10 November 2026; r207)

His pick after r206: "Real hatch & self-destruct", the last two of the pod's made-up sounds (`tools/drop_pod_sounds.py`,
recordings fetched with the browser on his PC; CC0 from Freesound, listed at the tool's top).

- **The hatch** (`hatch.ogg`): its three explosive bolts are a firecracker's crack (a raw city recording) three times,
  at 0, 60 and 110 ms, each a little faster or slower and quieter than the last; the hatch slamming down as its edge hits
  at 170 ms (a heavy metal door's slam, a little slowed) and clattering (a sheet of metal dropped on concrete).
- **The self-destruct** (`selfdestruct.ogg`, 5.6 s): a demolition charge (4 kg of TNT, a land mine cleared in Cambodia),
  its roll over the land, and the debris it threw coming back down (from later in the same recording, brought in 1.8
  seconds after the blast).
- The designator's chirp stays made here: it is electronic. The other pod sounds are as r206 left them (their files
  kept byte for byte: the tool writes them again the same).
- Test: `podSoundsAreAllInPlace` (unchanged: every pod sound and its files in the mod).

## The plasma baton held upright in first person (Justin, 11 November 2026; r208)

"the plasma batton i dont like the way you hold it i want it to be angled more toward the waist like its being held in
the characters hand" (first person, his answer). Shown four holds on a sheet (now, A gripped upright with the handle
down to the waist, B lowered tip forward, C low and angled in), he picked A: `firstperson_righthand` rotation
(0, 100, 55), translation (-0.5, 2.5, 0.6), on all three models (`tools/plasma_baton.py` `display`). Third person, the
guards and the menus are as they were.

## The squad film: a drop, the squad forming up, the pods blowing, a Warden taken down (Justin, 11 November 2026; r209)

"Make a mp4 clip of the video and audio of a 7 drop pod drop to them forming up and all the pods blowing up", then
"have them also fight a warden, and have it show off the hole in its body as they take it down", "Real game footage but
you do it all, I'm not on my pc right now". `29-film.bat` (beside the other bats) shoots it in the game by itself, like
the FPS check: it copies "Admin Review 6" to the throwaway `JugcraftFilm`, starts the game into it with the film armed
(`jugcraft-film.txt`, deleted as soon as it is read; the film runs in no other world) and OpenAL Soft's wave writer
taking the game's sound for that run only (`ALSOFT_CONF`, a fresh Gradle daemon so the game gets it), and closes the game
when done. Everything filmed is the game's own; only the camera and the cues are the film's.

- **The scene** (`tower/squad/SquadFilm`): open, flat, dry, untouched ground 110 to 240 blocks from the nearest tower,
  among the loaded chunks (scored for unevenness, water, leaves over the ground, anything built, cliffs), the tower
  behind the drop as the camera sees it and the flare put over the tower's side (the flights' seed picked so the plan's
  first draw points there). Seven pods called down exactly as a designator calls them (`SquadDrop.launch`); each soldier
  out is ordered to hold a spot behind the drop, 15 blocks or more from every pod, as the leader's panel orders it, so
  each pod blows itself apart once its soldier walks clear. Then a Warden comes up out of the ground (spawned as one
  does when called, `TRIGGERED`, so it rises) 25 blocks to one side of the spot, just inside the nearest rifles' reach,
  is set angry at the squad once it's up, and they shoot it down or take their blades to it. Monsters and his squads near
  are cleared first; time stops at 7000 with clear weather, no monsters spawn.
- **The camera** (`tower/squad/SquadFilmCamera`; the player in spectator, put there each tick by `client/FilmShoot`):
  springs (critically damped: it eases off and settles, never jumps) toward each shot's place and point of view: on the
  ground behind the squad's spot looking up at the flare and following the pods down (their middle, the falling ones
  counting most); beside the landing as the hatches blow, the soldiers walk out and the pods blow apart behind them; on
  the squad formed up with the Warden's ground behind them (calling the Warden); low before the Warden as it rises and
  roars; swinging round behind it as it goes for them (its back to the camera, so the tunnels the bolts burn show the
  light through); on it as it falls; up over the squad. Never in the ground or inside anyone. HUD and name tags hidden,
  music off, the other sounds full, his main screen filled (the game's borderless fullscreen); his settings put back.
- **The recording** (`client/FilmCapture`, `mixin/client/FilmCaptureMixin`): each frame's world (before any overlay) up
  to 30 a second, copied off the graphics card in turn through a few buffers (the game isn't held up) and written as
  JPEG by six threads (`frames-T-N.mjpg`, each frame's file, place and time in `frames.csv`); every sound played listed
  with its time in `sounds.csv`; a marker sound before it rolls and after it cuts lines the wave file up with the
  pictures. The hook is optional: if another mod moved the call it records nothing and the game plays on.
- **Put together outside the game** (`tools/film_assemble.py`): the frames laid out at 30 a second by their times, the
  game's sound lined up by the markers (the second checks the clocks), H.264 and AAC.

## Squads: a day in the field, then home; prices tripled and rising 10% a buy (Justin, 11 November 2026; r210)

"I want you to make it so that the legionaries will automatically call a drop ship to return one in game day after
they're called in. Also I want the price on each individual call in to go up 10% every time it's bought." His answers:
each player's own prices, each size its own; warned a minute ahead and when the ship's called, the time left on the
leader's panel; "also I want you to triple the base cost of the call ins".

- **A day in the field** (`Legionary.FIELD_TICKS`, 24,000 ticks: 20 minutes of play, counted in game ticks, so sleeping
  doesn't shorten it): each soldier keeps the game tick his squad was called in (its pods' launch; saved as
  `called_at`; a soldier from before this update starts his day the next time he's about). A minute before it's up the
  leader tells their player in chat ("one minute left in the field"); when it's up he calls a drop ship home himself, as
  SEND HOME does (`SquadOrders.order`): not in a fight (he tries again every 5 seconds till it's over), and only
  somewhere the ship can land (every 30 seconds otherwise, till they're somewhere it can); "our day in the field is up.
  A drop ship is coming" when it's on its way. A squad already sent home is left be; if the leader falls, the next in line
  keeps the watch (every soldier has the time). The leader's orders panel shows `HOME IN m:ss`, amber in the last
  minute, then `DAY UP · CALLING THE SHIP`.
- **Prices** (`JugcraftSquads.COST`, `price`; `SquadAccounts.bought`): the first of each size now 288/192/72/18 (3),
  480/336/120/30 (5) and 672/480/192/48 (7) iron, copper, gold, diamonds: three times what they were. Each squad a player
  requisitions raises that size's price for them by 10%, compounding, each payment rounded up (a squad of 3: 288 iron,
  then 317, then 349; diamonds 18, 20, 22); their other sizes and other players' prices don't move. The count is saved
  with their account (`bought`, beside their charges); the desk's page shows their own prices and both rules.
- Tests: `squadPricesTripledAndRiseTenPercentEachBuy`, `squadCallsTheShipHomeWhenItsDayIsUp` (warned a minute before;
  the ship called within the retry once the day's up, a landing zone found on its own); `requisitionPaysStoreFirstThenNetwork`
  and `dropPodsArePaidFromChestsPipedToTheCargoExchange` at the new prices.

## Drop pods: a fast entry from high in the sky (Justin, 11 November 2026; r211)

"Id like you to make the drop pods faster, like they should look like they're coming from high up in the atmosphere and
making a rapid entrance, only slowing at the last moment."

- **Higher**: the flare (and the pods' points in the sky about it) hangs 520 blocks over the mark and 120 out toward the
  ship that fires them (`SquadDrop.FLARE_UP`, `FLARE_OUT`; was 210 and 45), far over the clouds; the points spread wider
  to match (90 across, 50 along, 60 up or down; was 60, 30, 28). They come in on a long slant, turning to drop straight
  down at the end. The flare is drawn two and a half times the size, for the distance.
- **Faster**: the fall takes 6 seconds, not 17 (`JugcraftSquads.FALL_TICKS` 120): about 100 blocks a second at speed (was
  14), so a squad is down 9 to 16 seconds after the mark, not 20 to 27. The landings still spread over seven seconds.
- **Braking only at the end**: the retros light about 48 blocks up for the last 0.8 seconds (`DropPod.RETRO` 16) and brake
  it to under a tenth of its speed as it sets down (`TOUCHDOWN`; was a quarter): about 12 blocks a second at the ground.
- The re-entry trail is drawn over its last 24 ticks a tick at a time (was 36, every other tick), some 120 blocks of fire
  behind a pod at speed. The pods' clearance of each other is checked every half tick (they go some 5 blocks a tick).
  Sounds follow the flight as before (the roar from the moment it sets off, the boom 0.8 seconds in, the retros' burn).
- Tests: the squad tests unchanged pass (100 planned drops never touch and land on their spots; the film's camera
  keeps the falling pods in the picture for 85% of their fall).

## GitHub's main merged in; the core's height and a taller overworld; guards and squads; the Ember Oak's bed (Justin, 11 November 2026; r212)

- **GitHub's main of 3 November** merged into the PC's work (Herobrine, Peepo companions, the living city, the harvest and
  Halloween batches, board games, Thanksgiving and the rest): delivered as a git bundle and `30-update-from-github.bat`,
  which saves the folder first on branch `pc-backup-1111`. Both sides kept wherever they met (language strings, sounds,
  tags, mixin and test lists entry by entry; `SeatEntity` keeps the ship's seats and gains upstream's 16-way chairs).
- **The core's height** ("Make it so the core will not let you place it if it is too high"): the tower reaches 261
  blocks over its core and 3 under, so the core can't be set where it wouldn't fit between the world's floor and its
  build limit (`TowerCoreBlock.fits`, from the world's own limits: in the taller overworld below, y 330 at most; it
  was 58 at the game's height). Trying says how far past the limit the mast would
  go and the highest y that works; `/drone` builds refuse the same way.
- **A taller overworld** ("can you change the build limit without messing up world generation? like extending it
  conditionally for stuff like this or player builds?"; his pick after the costs were measured, the top at y 591): the
  mod ships the game's own overworld dimension type with its height 656 (`data/minecraft/dimension_type/overworld.json`:
  from y -64 to 591; was 384, to 319). Generation has its own height (the noise settings, -64 to 320) and is untouched:
  the same terrain, caves, ores and structures, open sky above. It's the whole overworld's (a build limit is the
  dimension's, not a place's), for player builds as much as the tower, whose core now fits as high as y 330. An existing
  world takes it as it loads: tried on a world saved at the old height, its blocks at y 300, 318 and -63 were all there
  afterwards and a block went in at 591. Measured on 1,089 loaded chunks of a flat test world: about 20 KB more memory a
  loaded chunk (about 90 MB with ten players spread out at view distance 10), about 3% more sent per chunk, about 60
  bytes more on disk, no change in tick time. Taking the mod (or this file) out again puts the top back at 319, and
  anything built above it is lost.
- **Guards** (in survival at the entrance at night, a costumed villain and a skeleton on him, "these are hostile mobs,
  let's kill them"): who the armed crew and the squads take on is one rule now for every mob, the game's and every mod's
  (`tower/crew/Hostiles`): anything hostile (a neutral one only once angered), and any mob going for a player in
  survival, one of the crew or a soldier; never pets or anything someone owns. Security and the gate guards look first
  for a mob on a player or one of their own within 24 blocks, seen or not (they go to where they can see it). A shooter
  with someone in the line of fire, most often the very player the mob is on, now moves after a second and a half to the
  nearest spot to the side with a clear shot (`PlasmaRifle.aside`), where before they held their fire as long as the
  player stood there.
- **Squads following** ("players might have an ability to fly later on, so I don't want anything to interfere with ...
  these guys following them"): they take on only what's within 24 blocks of their player, and let a fight go once he's
  more than 20 blocks from them (`Legionary.LEASH`); a target they'd let go of is dropped at the same 36 blocks they
  take one on (it was 40, and one between 36 and 40 held a squad still for good). Further than 40 blocks behind him, or
  falling behind and not gaining for six seconds (a wall, a cliff, water, a player flying or riding), they're set down
  behind him where there's room, on the ground under him if he's flying (`catchUpSpot`). The leader walks to the ground
  under a flying player, and a soldier who missed an order takes the leader's.
- **The Ember Oak's bed** ("it's discolored there in between the original circle and the basalt ... you overextended it
  outwards so that it's going through the basalt on the outside"): the sheet of soil r181 laid under the columns is gone.
  The tree's own soil now runs on, level, to under the columns (all of it the columns leave open is within 3.18 blocks of
  the middle; the seat's columns start at 3.42), and the hairlines between the columns show basalt (each column's top
  again, 0.03 lower and just wide enough to meet its neighbours'). Nothing reaches out past the columns.
- Tests: `entranceGuardShootsWhatComesForAPlayer` (the real foundation and post; a zombie, a skeleton and four costumed
  villains on a survival player out front: the guard hits every one), `squadKeepsUpWithAPlayerWhoFliesOff`,
  `followingSquadLetsAFightGoToKeepUp`, `overworldReachesY591AndTheCoreFitsUpTo330`, with the crew's and the squads'
  tests as before.

## Progression

| Step | Docks | Capacity | Cost (items, kinds) |
| --- | --- | --- | --- |
| Foundation (control building) | none | 0 | 48,080 items, 15 kinds (with the 4 holo pieces) |
| 1 Roof Pads | 24 small | 24 | 294, 1 kind (the hangar pads; the roof plant and walkways come free) |
| 2 Pillar Feet | 20 small | 44 | 6,817, 9 kinds |
| 3 Small Hangar Rows | 18 small | 62 | 8,189, 9 kinds |
| 4 Medium Hangars | 16 medium | 78 | 9,252, 9 kinds |
| 5 Logistics Levels | 14 medium | 92 | 9,606, 9 kinds |
| 6 Composite Levels | 12 medium | 104 | 10,596, 9 kinds |
| 7 Large Hangars | 10 large | 114 | 11,081, 9 kinds |
| 8 Heavy Levels | 8 large | 122 | 11,481, 9 kinds |
| 9 Crown | 6 large | 128 | 12,925, 9 kinds |

Each step costs the blocks it places (`TowerData.cost`), since 26 October 2026 only the charged ones (Justin: "the main
12 blocks", plus the holo parts; `TowerCosts.CHARGED`, `tools/tower_costs.py` CHARGED): carbon composite panel,
reinforced concrete, steel armor plate, ceiling tile, access floor tile, dock plating, framed glass, epoxy floor, hangar
pad, neon tube, ceiling light panel and painted wall (about 93% of the tower), and the foundation's hologram table
sections, holo projector, holo command table and Big Map Table (made on the Advanced Crafting Table, fairly expensive).
Everything else a step places (furniture, screens, the other lights, signs, pipes, the roof plant) comes free; the frozen
earlier designs still cost every block. Before, the foundation took 277 kinds. The core still builds the foundation and
tier 1 itself once it holds the step's price; every tier is worth more than the one below
(`tools/tower_costs.check`, which also makes sure every charged block has a crafting recipe of its own shape and that
`TowerCosts.CHARGED` matches the tool's list). Input
producer: the tower's building blocks (crafted), loaded into the core. Output consumer: the depot's drones (docking, charging, building), the
exchanges (power and cargo in). Unlocks: each tier unlocks the next drone tier. Since layout 37 the depot takes drones
from tier 1 on (the roof pads): its landing platform has the supply pickup but no pad of its own (the lift house's
3 x 3 since layout 40; until layout 36, between the foundation and tier 1 the depot worked as a ground depot, the yard's
pad holding 4 small drones).

## Conversion

A core saved in an earlier design (layouts 1-7) is rebuilt as the Tri-Pillar from its first server tick after
loading (`TowerCoreBlockEntity.startConversion` / `convertTick` / `endConversion`):

- a core with nothing built just takes the new design;
- otherwise the depot is handed over (`DroneTerminalBlockEntity.handOver`: owner, mode, energy, drones, the packager's
  store), a tier in progress is credited to the core's store as the Tri-Pillar tier's blocks, the old design's blocks are cleared (only blocks that are still
  that design's block; nothing else within the 64-block footprint is touched apart from what the Tri-Pillar places),
  then the foundation and the tiers up to the old tier are built at 2048 blocks a tick;
- at the end the new terminal takes the depot back (`takeOver`): drones that fit are linked again, the store goes back
  into the packager, and whatever does not fit (drones over the tier's capacity, items) is dropped over the terminal
  as items that never despawn.

`/dronetest build [tier]` refuses a converting core.

## Behaviour and limits

- The core builds the foundation and tier 1 itself at 64 blocks a tick; cells already empty cost nothing.
- The tower stands 261 blocks tall from the core: on ground much above y 60 the mast passes the build limit and its
  top blocks are skipped (nothing breaks).
- Chunk loading: footprint radius 64 for layout 8 (`TRIPILLAR_FOOTPRINT`) and while converting.
- Displays the core pins to its terminal (hall desks, master console, tables, video walls, hall fittings) work from up to
  128 blocks (`DepotDisplayBlockEntity.PINNED_RANGE`): the hall is about 50 blocks from the terminal.
- The tower model for the hologram is worked out off the render thread when the tower changes.
- `/dronetest build [tier]` places a core (if none is near) and builds it instantly (it loads the footprint's chunks
  first).

## Files

- Generators: `tools/tripillar_geometry.py`, `tools/tower_tripillar.py` (layout, pictures), `tools/tripillar_hall.py`
  (hall fittings), `tools/tripillar_fixtures.py` (fitting blocks, bay tag sheet, rack textures),
  `tools/holo_command_table.py`, `tools/tower_control_console.py` (console, chair and raised floor: meshes, texture, data),
  `tools/curved_video_wall.py` (the video wall: mesh, texture, aims, footprint, data),
  `tools/drone_display_case.py` (the display case: mesh, texture, data), `tools/dotfont.py` (the ticker rail's dot font),
  `tools/crystal_tree.py` (the Crystal Tree's planter and bench, atlas, data), `tools/ops_trees.py` (and
  `tools/ops_trees_molded.py`, the Ember Oak's cast bark, r183) with
  `tools/ops_trees_tex.py` (the flight-ops trees: their variants' meshes, atlas and map, the Ember Oak's columns, data,
  icons), `tools/crew_skins.py` (the crew's skins, gear and props boxes, props texture, hiring pool) and
  `tools/tripillar_crew.py` (the crew's posts and routines, checked), `tools/plasma_rifle.py` (security's rifle: the item's
  model and texture, the bolt's texture, its sounds from Justin's library files), `tools/plasma_baton.py` (the gate
  guards' baton: the item's three models, definition and texture, its sounds from Justin's library files) with
  `tools/item_kit.py` (items as round moulded parts: prisms, lathes, the texture atlas, the no-shared-plane check), `tools/tower_night_lights.py` (the lamps' night
  modes: models, blockstates, the layout's `night_lights`), `tools/long_coffee_table.py` (the
  lounge's table: mesh, atlas, data), `tools/service_arm.py` (the service arm's joint meshes and atlas, its icon), `tools/drone_parts_shelving.py` with
  `tools/parts_shelving_parts.py` and `tools/parts_shelving_art.py` (the parts shelving: meshes, atlas, data),
  `tools/ops_desk.py` (the Console Desk's joined bench models and its keyboard),
  `tools/maintenance_door.py` (the Maintenance Door's models, textures, leaves, data),
  `tools/neon_lines.py` (the neon light lines: textures from the materials', models, data, the layout's conversion),
  `tools/joined_girders.py` (the joined girder's models, textures and blockstate), `tools/cradle_struts.py` (the Cradle
  Struts' mesh, atlas, footprint and collision, data), `tools/big_map_table.py` (the Big Map Table's mesh, atlas,
  footprint and collision, the map room's sign, data), `tools/tripillar_rooms.py` (the ground floor's rooms from the
  plant's interior kit; since layout 49 the gaps between them closed and their hall side: `close_gaps`, `hall_side`),
  `tools/faced_walls.py` (the faced walls and their lines: models from the materials' and the Neon Lines', data, and the
  rule that faces a room's walls onto the hall; `tower/FacedWallBlock`, `tower/FacedLineBlock`),
  `tools/check_self_calls.py` (no method hands its own arguments straight back to itself: r197),
  `tools/keep_same.py` (a generated PNG or gzip is rewritten only when its content differs: r199),
  `tools/crew_ship_paint.py` (the drop ship's paint laid as one skin over its whole outside), `tools/crew_ship_seams.py`
  (measures it: size, stretch, breaks) and `tools/crew_ship_planes.py` (finds faces lying in one plane and stands them
  apart): r200,
  `tools/glow_alpha.py` (glow textures carry their brightness as opacity; `client/Glows`, `client/StandIns`): r201,
  `tools/drop_pod_sounds.py` (the drop pods' sounds from real recordings; its sources at its top), `client/DropPodSounds`
  (the roar's two layers, the boom, the retros): r206,
  `tower/TowerSupply` (what the tower pays from), `tower/TowerStorageMenu` and `client/TowerStorageScreen` (the tower
  storage by hand), `tower/RetiredBlocks` (retired blocks become their replacements), `sound/SoundWalls` (sound through
  walls, for both sides): r205,
  `client/BatonPlasma`, `client/BatonPlasmaItem`, `client/PlasmaGlows` (the baton's plasma, the item's special renderer
  for it, the glows it shares with the bolt: r203), `tower/crew/CrewRoutes` (the tower's walk map and the crew's long walks over it; game test arena
  `drone_tower_grounds` 200x48x204: a tower on open ground): r202,
  `tools/shower_stall.py` (the shower stall's models, curtain, data; its sound in
  `tools/restroom_sounds.py`), `tools/tripillar_rooms_upper.py` (the upper floor's rooms; since layout 50 their gallery side:
  `gallery_side`), `tools/room_kit.py` with
  `tools/room_kit_recipes.py` and the kits (`tools/training_kit.py`, `manager_kit.py`, `lab_kit.py`, `rest_kit.py`,
  `planning_kit.py`, `office_kit.py`, `lounge_kit.py`, `sim_kit.py`, `class_kit.py`, `meeting_kit.py`, `wash_kit.py`,
  `room_extra_kit.py`, the roof plant's `roof_plant.py` with `roof_plant_art.py`, and the goods lift's `goods_lift.py`
  with its live parts' table `jugcraft/goods_lift.json`; the screens' pictures
  `tools/data/desk_pages`) → the pieces' models, textures and `jugcraft/room_kit.json`, `tools/roof_plant_layout.py`
  (the roof plant and walkways on the roof), `tools/tripillar_stations.py` (the stations inside: the intake face and
  room, the cargo hall, the goods lift's shaft and house with the pickup on its top),
  `tools/holo_projector.py` with `tools/holo_head.py`, `tools/holo_head_hair.py` and `tools/holo_head_letters.py` (the
  Holo Projector's housing, the head and its visibility, the ring's letters, atlas, data; source `tools/data/makehuman_head.npz`),
  `tools/tripillar_lights.py` (deck lights) → `data/jugcraft/drone_tower/tower_tripillar.json.gz`,
  `textures/gui/tower_tripillar/` (foundation and tier pictures); then `tools/tower_diorama.py` (the tower diorama) →
  `tower_mesh/tower_diorama.bin.gz`, `textures/entity/tower_diorama.png`.
- Java: `TowerData` (layout 8, foundation; the terminal's `platform` and `packager` from layout 37), `TowerCoreBlockEntity` (foundation step, conversion, footprint, the doors' owner),
  `GoodsLiftDoorBlock` and `client/GoodsLiftRenderer` (the goods lift), `client/warehouse/CargoHallRenderer` (the cargo
  hall's drones, cases and packer, with `warehouse/DroneRenderer`'s drones), `ExchangeStations` (each layout's masters and facings),
  `MaintenanceDoorBlock` (the pillar doorways' doors; the plant doors' `PlantDoorRenderer` swings them),
  `NeonFloorBlock`, `NeonTubeBlock` (the neon light lines; the lines are `TowerPanelBlock`s, which take their phase by
  position as any block with `TowerPanelBlock.PHASE`),
  `TowerGirderBlock` (the joined girder), `CradleStrutsBlock` + `CradleStrutsPartBlock` (the cradles' struts),
  `BigMapTableBlock` + `BigMapTablePartBlock` + `BigMapTableBlockEntity` + `MapScan` (the map room's table and its scans),
  `nuclear/RestroomBlocks.Shower` (the shower stall), `RoomKit` + `RoomKitBlock` + `RoomKitDisplayBlock` +
  `RoomKitRotorBlock` + `RoomKitRotorBlockEntity` (the room kits' pieces from their manifest; the dry cooler's rotors), `HallFixtureBlock`, `DroneTerminalBlockEntity` (hand-over, bay status),
  `DepotDisplayBlockEntity` (pinned reach), `ExchangeStations.masterOffset(block, version)`, `DroneDevCommands`,
  `crew/TowerCrew` + `crew/CrewPost` + `crew/CrewRoster` + `crew/TowerCrewState` + `crew/TowerCrewCare` (the crew),
  `SeatEntity.seat` (the crew in the chairs), `TowerRegistry.inTower` + `mixin/TowerSpawnMixin` (no mob spawns in a tower),
  `TowerLightBlock` + `TowerNightLights` (the ceiling lights' night modes, switched by the core);
  `crew/CrewGreeting` (how the crew greet a player);
  client: `HoloMapRenderer` + `HoloGeometry` (hologram, projector, ticker), `TowerDiorama` (the small table's model), `TickerRail`
  (the ticker rails), `CurvedVideoWallRenderer`, `DroneDisplayCaseRenderer`, `CrystalTreeGeometry` +
  `CrystalTreeRenderer` (the tree, grown from its seed), `LongCoffeeTableRenderer` (the table and its ticker lines),
  `HallFixtureRenderer`, `ServiceArm` (the service bay's robot: its cycle, inverse kinematics, flexing tubes, stop),
  `DronePartsShelvingRenderer` (the parts shelving and its live gauges and screens), `ConsoleDeskRenderer` + `DeskPages` +
  `DeskScreen` + `DeskData` (the desks' live pages), `HoloProjectorRenderer` + `HoloProjectorGeometry` + `HoloHeadMesh`
  (the Holo Projector's watching head), `CradleStrutsRenderer` (the cradles' struts), `BigMapTableRenderer` + `BigMap`
  (the Big Map Table, its map and panels), `RoomKitDisplayRenderer` (the rooms' wall screens), `RoomKitRotorRenderer`
  (the dry coolers' turning fans), `DroneDepotRenderer` (iris doors,
  bay tags and lamps), `DroneMesh`, `DroneModel`, `TowerCrewRenderer` + `TowerCrewModel` + `TowerCrewPropsModel` +
  `CrewBoxes` + `CrewGreetings` (the crew).
- Drone meshes: `tools/drone_mesh/export.py` (Playwright + Chromium; uses `lib/three.min.js` r128, or the bundled
  `lib/three_subset.js` served over loopback when that file is missing).

## Validation

- `python tools/check_mod_data.py`: no new problems against the previous run.
- Game tests (Tri-Pillar arenas `drone_tower_tri` 128x48x112 and `drone_tower_tri_full` 128x272x112, helper
  `TriPillarArena`): `TowerGameTests`, `TowerConversionGameTests`, `IndustrialTowerGameTests` (lighting, desks,
  screens, design data), `MasterConsoleGameTests`, the exchange tests, `TowerClientGameTests` (with
  `TowerRenderChecks.hologramFitsTable`), `RoomKitGameTests` (a piece placed whole and broken as one, facing its
  placer, rows and risers, the wall screen's role, the toilet's partition, the live faces' block entities and the
  bottle filler's count). Focus word: `tower`. Written and compiled here; run on the owner's PC.
- The flight-ops trees and the crew (layout 45, 5 November 2026): the main, client and game-test code compiles here
  against the owner's build classpath (no new errors: the only ones are the Kotlin objects this compiler can't see).
  The crew's models were assembled outside the game from `crew/gear.json`, `crew/props.json` and the skins exactly as
  `TowerCrewModel` builds them (the game's box UVs and rotation order) and drawn in every activity, front and back.
  `tools/tripillar_crew.py` checks every post against the layout (chairs where they should be, every spot free floor,
  each post's spots reachable without a door); `tools/crew_skins.py` checks no two boxes share texels. Game tests:
  `TowerGameTests.crewPostsAreSound` (37 posts, one manager), `crewMemberSitsAtTheirChairAndCanBeKilled`,
  `crewSitInTheSimulatorsAndOfficeChairs` (a pilot flying in a Pilot Simulator, its seat staying under him; an engineer
  typing at an office chair), `assertOpsTrees` (in the build test)
  and `TowerConversionGameTests.dioramaCaseTriPillarIsRefittedWithTheOpsTreesAndHiresItsCrew` (layout 44 to 45: the
  trees whole, the desk rows left, the security office's single desks and the wall stub gone, a stray zombie cleared,
  then 37 crew hired in 37 faces; since layout 46 refitted to the current layout, the trees centred). The new jobs were drawn in their rooms from the shipped skins (the new-jobs sheet). `check_mod_data.py` needs the owner's art library, so it runs on the owner's PC.
- The trees centred, the self-listing cores and the mob sweep (layout 46, 6 November 2026): the main, client and game-test
  code compiles here (a full compile). `tools/tower_tripillar.py` regenerated: against layout 45 only the trees' cells
  differ (and two night lamps), every crew post the same. The new spots were drawn in the hall from the views of Justin's
  screenshots before and after. Game test: `TowerGameTests.towerListsItselfAndLeavesMobsThatComeIn` (a core set without
  a player lists itself; a zombie and a pig in the hall stay).
- The crew's routines (5 November 2026): the main, client and game-test code compiles here against the game's classes.
  The sleep poses, the eyelids and the blanket were assembled outside the game from the shipped skins, `gear.json` and
  `props.json` with `TowerCrewModel`'s numbers and the renderer's lying transform, and drawn in their rooms (the routines
  sheet). `tools/tripillar_crew.py` checks every sleep spot and break seat can be walked to from the post through the
  doors, and that no partners are away together. Game tests: `TowerGameTests.crewRoutinesAreSound` (12 on the night
  watch, every sleep spot and break seat on its floor and of the right furniture, no seat shared at once, partners never
  away together, `restAt` at given times), `crewSleepAtNightAndWakeWhenHurt`, `crewTakeTheirBreaksWithACoffee` and
  `crewLieDownToSleep`.
- The night lighting (5 November 2026): compiles here against the game's classes. The rooms were drawn by day and by
  night outside the game with the light levels the night modes give (the night lighting sheet); the exported
  `night_lights` match the sheet's plan (one lamp at the data hall's edge differs: night, not off). Game tests:
  `TowerGameTests.nightLightsAreSound` (every switched lamp a tower lamp the foundation places, the console's lamps kept
  on, the manager's office dark) and `towerLampsDimAtNight` (the modes' light levels, only tower lamps switched, the
  night's hours).
- The greetings (5 November 2026): compiles here against the game's classes. The poses were drawn outside the game from
  the shipped skins with `TowerCrewModel.greet`'s numbers (the greetings sheet). Game tests:
  `TowerGameTests.crewGreetingsFitWhatTheyDo` (the greeting for each kind of crew member and what they're doing; every
  word's key in the language file; rude only on its roll; the time of day's words) and `crewGiveWayToAPlayer` (a patrol
  stepping aside for a player in a three-wide strip and walking on past).
- The live faces' pictures (`LiveFaces`) were drawn outside the game by a harness and laid on the as-built rooms'
  models where the manifest puts them, to check they sit on the baked pictures.
- The hologram mesher and the map's relief (`HoloGeometry`) were run outside the game on the real tower data and
  rendered in the hall geometry for review.
- The desks' 45 pages (`DeskPages`, `DeskScreen`) were drawn outside the game with a stand-in for `DeskData`
  (claude/src/dronetower/tools/mockups/ds_harness): laid side by side with the approved sheet, every line fitting its
  screen, no two overlapping shapes on one layer.
- The Holo Projector's hologram (`HoloHeadMesh`, `HoloProjectorGeometry`) was built outside the game from the shipped
  `holo_head.bin.gz` and drawn with a stand-in for the game's renderer: it matches the approved sheet. Offline it builds
  in about 7-9 ms. From in front, 61% of the head's triangles are drawn; from behind, 13%. Every corner stays between
  the deck and 7.9 blocks up, within 3.7 blocks of the middle. The in-game check is `TowerRenderChecks.holoProjector`,
  run with `hologramFitsTable`. In-game speed and the look in the real pipeline are for the owner's test.
- The Cradle Struts' mesh was read back from `cradle_struts.bin.gz` and drawn in the real tier-2 layout outside the game:
  it matches the approved sheet. All 58 cells are clear in all four cradles. The registries were booted with the
  tower's registration: every state builds; every shape of every cell, turned four ways, stays in its cell; the girder's
  12 states have their plates. The art checks pass on the girder's 12 models and on runs of them along each axis. The
  in-game checks are `TowerGameTests.assertCradleStruts` and `assertCanopyPosts`, `cradleStrutsArePlacedWholeAndGoWhole`
  and `TowerRenderChecks.cradleStruts`.
- The Big Map Table's map was checked outside the game. The column reader (`MapScan.read`) was run on made-up columns:
  flat ground, a canopy, a cave, a floating island, the tower's shut-in halls, a deck over open air, a tall hall, the
  void, and more spans than a column keeps. The take-3 world with the finished tower and the floating island went
  through the payload (about 105 KB, the same both ways) and into `BigMap`'s mesh. Drawn on the table's shipped mesh, it
  matches the approved sheet: about 31,000 quads, of which about 18,000 face a camera at the table; the coarse copy is
  about 9,400. It builds in about 30 ms off the render thread. The registries were booted with the tower's registration:
  all 165 cells' shapes stay in their cells up to the top, and the layout has the table whole and the sign's 4 parts.
  The in-game checks are `TowerGameTests.assertMapRoom`, `bigMapTableIsPlacedWholeAndGoesWhole` and
  `bigMapTableReadsTheWorldAtItsTrueHeights`, and `TowerRenderChecks.bigMapTable`. In-game frame time and the look in
  the real pipeline are for the owner's test.
- The ground floor's rooms were drawn outside the game in the built layout and match the approved sheet; every room's
  walls and ceiling are whole (no gap over a door). The registries were booted with the tower's and the interior kit's
  registration: every state of the layout's palette parses (0 bad), the shower stall's 8 states build. The art checks
  pass on the shower stall's two models and three stalls side by side. `tower_costs.check` finds every tower block
  craftable from a recipe of its own. The in-game checks are `TowerGameTests.assertGroundFloorRooms` and
  `showerStallRunsAndPutsOutFire`, with review photos of each room.
- The roof plant was drawn outside the game from the built layout and the shipped models (the dry coolers' rotors from
  the manifest) and matches the approved sheet. The art checks pass on its models (no shared planes, no holes). The
  registries were booted with the room kits' registration: all 149 pieces' states and cells check out, and only the dry
  cooler's anchor states (8) make the rotor entity, which has no ticker; the layout's palette parses (0 bad). The
  in-game check is `TowerGameTests.roofPlantStandsOnTheRoof`; the fans turning is for the owner's test.
- The first-floor look-over (layout 49, 10 November 2026; r198): the main, client and game-test code compiles here
  against the game's classes; the tower's 158 game tests pass (the ground floor's checks, with the new hall-side ones,
  in the foundation test and three refit tests). `tools/tower_tripillar.py` regenerated: against layout 48 only the
  foundation differs (524 cells, y -1 to 9), and no Neon Line elsewhere changed its shape with framed glass counting as
  a wall to a line. `tower_costs.check` passes (0). The floor was drawn outside the game from the regenerated layout
  with the shipped blockstates and models (the look-over's renderer, now phase-true): the walls, the line's turns, caps
  and joins, the closed gaps, the doors, the benches. The two new blocks' looks in the game itself are for the owner's
  test.
- The upper floor's walls (layout 50, 10 November 2026; r199): the main, client and game-test code compiles here
  against the game's classes (a full compile: the layout's number is a constant other classes hold a copy of); the
  tower's 158 game tests pass (the upper floor's checks, with the new gallery-side ones, in the foundation test and the
  refit tests). `tools/tower_tripillar.py` regenerated: against layout 49 only the foundation differs (913 cells, y 12
  to 15), the crew's posts and the night lights not at all; run a second time it changes no file.
  `tower_costs.check` passes (0); `tools/check_self_calls.py` passes (0 endless). The diorama's mesh and texture were
  rebuilt from layout 49 as well: the mesh is r198's byte for byte, the texture differs from the one r198 shipped in
  four palette texels (the mismatch above). The floor was drawn outside the game from the regenerated layout with the
  shipped blockstates and models. In the game itself: for the owner's test.
- The drop ship's hull pass (10 November 2026; r200): no Java changed. `tools/crew_ship.py` exported: the ship's mesh
  and its collision shell changed, nothing else (written through `keep_same`). The paint was measured over the whole
  hull before and after with one tool (1,141 square blocks off size and 261 blocks of broken line; 1.3 and 58), the
  far model against the near (identical on every shared face), and the planes finder run on the built ship (0 pairs
  left of 198). The tower's 158 game tests pass with the new shell (the drop ship's four among them); on the way one
  run failed `dropShipDoorGunsSpinUpStowAndPlayersSit`, traced to the roof ridge's feet and put right (above), and one
  run failed a crew test (`...refitted with the ops trees and hires its crew`: 46 of 47 posts filled at tick 56) that
  nothing here touches and that passed on the next two runs: a test to watch. The sheets were drawn outside the game
  from the generator; his own screenshot of the fin was matched first. In the game itself: for the owner's test.
- The two play test faults (10 November 2026; r201): both read from his PC's files (the crash report's trace; the
  eyes pipeline's blend from the 26.3 client's class). The main, client and game-test code compiles; the seven places
  the client creates an entity all give it an id (the new check), and no glow texture is black-backed any more (three
  were). Client drawing can't be run here: whether the ship now lands with its crew drawn, and how the bolt looks
  laid on by opacity instead of added, are for the owner's test.
- The crew's walks (10 November 2026; r202): found by measuring, not from his save (it sits too deep in his folders for
  the file tools here to read): a tower built in the game-test server on open ground, the whole crew put outside the
  gates, their places logged every ten seconds, before and after. Before: 28 of 47 at post after four minutes, seven
  outside, none upstairs who had to climb, and the outside ones exactly where his pictures show them. After: nobody
  outside after 70 seconds, 38 at post and the rest on their rounds after four minutes, nobody put back. Kept as
  `crewWalkInFromTheGatesAndUpstairs` (the seated and stationed crew walk in and up, the map has a way on through
  every gate's lane, nobody is put back); the gate test now asks the path finder about the whole of a walker at a shut
  gate, the aisle door's test that it is planned through and opens for a walker who isn't a player. The tower's 159
  game tests pass. On the way the suite showed a test that failed now and then (the drop ship's door gun test lost its
  zombie on its first tick): put down then to the morning sun, and the three drop ship tests' zombies kept from
  burning; the real cause was found with r204 (below). Not seen in the game itself: for the owner's test.
- The plasma baton rebuilt (10 November 2026; r203): the three models pass the kit's no-shared-plane check as they are
  written. The item definition was read back with the game's own codec (the 26.3 client's `ClientItem.CODEC`, the
  mod's special renderer registered): the three cases and both plasma variants parse. The models were drawn outside the
  game exactly as written, on a stand, in a guard's fist and holster with the renderer's own transforms (standing,
  at both ends of a stride, seated, through the draw), in first person and as a menu icon. The plasma shown is what
  `client/BatonPlasma` really emits: the compiled class run outside the game, its quads dumped and drawn (115; every
  one wound to face the eye, in a mirrored pose too; in a menu both faces, lifted towards the viewer), next to the
  approved sheet's. Where a held item sits in a player's hand was read from the 26.3 client (`ItemInHandLayer`), which
  showed the first third-person hold had the grip 3 px ahead of the fist: corrected. Main, client and game-test code
  compiles; the tower's 159 game tests pass (the swing's new reach included). Client drawing can't be run here: how it
  looks in the game, in a hand, in the menus and on the guards, is for the owner's test.
- The crew's spread (10 November 2026; r204): the layout written again differs from r203's only in its `crew` list (the
  blocks, tiers, palette and night lights are the same); in it only the two posts moved, 16 sleep spots or times
  shifted and every break list changed. The tool's own checks hold (every spot free floor and reachable, a sleep spot
  for every sleeper on their floor, partners never away together, no seat double booked). Measured from the layout:
  6.2 away at a time by day before (8 at most), 3.1 after (5 at most). Game tests: `crewRoutinesAreSound`,
  `crewPostsAreSound` and `crewWalkInFromTheGatesAndUpstairs` (the technician is now one of those who walk in from
  outside the gates and up the stairs) pass. Not seen in the game itself: for the owner's test.
- The door gun test that failed now and then (found with r204): `dropShipDoorGunsSpinUpStowAndPlayersSit` failed in one
  run of the suite in three. Made to say what its zombie's "first hit" was, and the drop ship tests run alone eight
  times: three failures, each on the first tick, with no damage and no source. The zombie had despawned: the game
  removes a monster whose nearest player is more than 128 blocks off, a player another test put in the world is enough,
  and the order the tests' batches run in differs from run to run. The three drop ship tests' zombies are now kept
  (`keepInTest`: persistent, and still kept from burning); the same eight runs then all pass.
- What the tower pays from, the storage screen, the lounges' TVs, repair and sound (10 November 2026; layout 51; r205):
  the layout written again differs from layout 50's in 44 cells (the break area's TV and console, the upper lounge's
  rug, sofas, table and lamp), no Lounge TV left in its palette; layout 50 kept as `tower_tripillar_v50.json.gz` for
  conversion. Main, client and game-test code compiles; the tower's 165 game tests pass, with the warehouse test run
  with them once (176). Not seen in the game itself (the Murmur's head wound, the storage screen, the TVs): for the
  owner's test.
- Drop pods (10 November 2026; r206): the takes were rendered outside the game by a mix of the game's own rules (the
  flights from DropPod.path and SquadDrop.plan ported, the layers' loudness and Doppler pitch as DropPodSounds sets
  them) and checked by measurement: no tones in any layer (peaks under 4 dB above the spectrum's median), the
  simulated take's spectrum within 1.3 dB of the recordings' and its crackle (the skew of the wave's slope) 5.0 against
  the Shuttle's 4.9. The flight planning was checked first in a port (spread entries with staggered starts: a quarter of
  drops touched without the search, none with it). Main, client and game-test code compiles; the focused squad tests
  pass, and the tower's full suite: 167 game tests. The first full run caught two test assumptions the staggered
  landings broke (a soldier out early may wander from his pod; a Lounge TV may straddle two chunks): fixed in the tests.
  Not heard in the game itself: for the owner's test.
- The hatch and self-destruct (10 November 2026; r207): measured from the files written: the three bolt cracks peak at
  0.90, 0.77 and 0.67 of full scale over the slam's 0.68 (each crack sharp, the slam broad); the other eight pod sounds
  decode to the same samples as r206's. The tower's game tests pass (167). Not heard in the game itself: for the
  owner's test.
- The baton's first-person hold (11 November 2026; r208): drawn outside the game with the game's first-person placement
  (the arm's point, then the model's display transform) from the build's own models; only the three models' first-person
  transform changed. Not seen in the game itself: for the owner's test.
- The squad film (11 November 2026; r209): the scene and its camera flown in a game test alongside a real drop and
  fight on open ground (`squadFilmDropFormUpAndTakeDownAWarden`): ten runs in a row every pod blew itself apart, the squad
  formed up, the Warden came up, was hit 20 or 21 times and died, three to five of the seven standing; the falling pods
  in the picture for 94% of their fall, the Warden for 95% or more of its rise and fight; the camera never in a block,
  never nearer the Warden than 4.3 blocks or anyone's outline than 0.8. With the Warden 30 blocks out it reached them with
  more health left (two standing); 25 lets the rifles work through its rise and roar. Two early runs left one pod
  waiting (a soldier who didn't walk off; not seen again in twelve runs): the scene now gives such a soldier the order
  again and writes down what he was doing. The assembler was checked on made-up frames and sound (both markers found to
  the sample, a test tone where it belongs). The recording itself happens only in the game on his PC.
- A squad's day in the field and the rising prices (11 November 2026; r210): the squad tests, the payment tests and the
  film's scene pass together (14 game tests, focused): the price rule checked to the ingot over three buys and three sizes,
  another player's and other sizes' untouched; a squad back-dated to a minute short of its day is warned and, once its
  day is up, its leader calls the ship within five seconds, which finds open ground to land on its own. The desk page and
  the panel's countdown are drawn by the client and not seen here: for the owner's test.
- The fast entry (11 November 2026; r211): the flight worked through in the path's port first (14 blocks a second at
  speed before, about 100 now; the retros from 48 blocks up; 12 blocks a second at the ground), then the squad tests and
  the film's scene (14 game tests, focused) pass with it. Not seen in the game: for the owner's test.
- Guards, squads following, the Ember Oak's bed (11 November 2026; r212): the crew's and the squads' game tests pass together
  (29, focused), with the three new ones. The bed re-baked: only its mesh and the three trees' change (the Drift Isle and
  everything else come out byte for byte as before), drawn from the user's angles before and after. GitHub's main merged:
  the whole tree compiles (4,087 main and client files, 773 test files) and a server test run on it passed (170).
