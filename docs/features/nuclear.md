# Nuclear power (stage 1)

Status: implemented on the drone-tower branch (stage 1 of the approved plan). Stage 1 passed focused game tests on the
owner's PC. The pre-made plants, cooling towers and new reactors (below) are built and compile, with game tests
written, but have not been run in game yet. Not merged.
Proposal: the "Nuclear Power Plant" proposal (draft 5) and "Nuclear, stage 1" design in the project notes.
Owner: Justin (jimbozoomer)
Target milestone and tier: endgame power (after steel, aluminium, titanium, tungsten and electronics).
Primary specialty and supported player role: engineers who build big plants; the radiation gear supports anyone near one.

## Player experience
Stage 1 adds one working reactor unit, built from real plant equipment and run from a control console.

- **Fuel:** uranium ingots go into the **Gas Centrifuge Cascade**. Eight ingots make one **Enriched Uranium Cylinder**.
  The **Fuel Fabricator** turns a cylinder and two steel plates into two **LEU Fuel Assemblies**.
- **Equipment:** each piece is placed whole from one item. Its front left corner goes where you click, and it reaches
  to your right and away from you. The client draws each piece in 3D with depth, and invisible frame blocks give it a
  body. Breaking any part breaks the whole piece and drops its item. Every part answers to the piece's own name: Jade
  names it (a board, say, is "Reactor Coolant Control Board", not "Nuclear Equipment"), and picking any part with the
  middle button gives its item.

  | Piece | Size (wide x deep x tall) |
  | --- | --- |
  | Reactor Pressure Vessel | 7x7x20 |
  | Nuclear Steam Generator | 7x7x21 |
  | Reactor Coolant Pump | 5x3x10 |
  | Pressurizer | 5x3x18 |
  | Turbine-Generator | 11x50x10 |
  | Condenser | 12x6x6 |
  | Feedwater Pump | 3x2x3 |
  | Main Transformer | 5x5x8 |
  | Reactor Control Console | 3x2x3 |
  | EPR Reactor Pressure Vessel | 8x8x22 |
  | Small Modular Reactor | 5x5x22 |
  | Small Turbine-Generator | 3x5x4 |

- **Building a unit:** nothing fills in automatically; the vessel works out its unit from the world.
  - **Reactor Coolant Pipe** joins the vessel to its steam generators, coolant pumps and pressurizer. Equipment
    touching the vessel directly counts too.
  - **Insulated Steam Line** joins the steam generators to the turbine-generator, the feedwater pump and the condenser.
  - The condenser and the main transformer may also simply touch the turbine-generator.
  - Each steam generator with a coolant pump is one loop. Up to four loops carry a quarter of the heat each.
  - The unit is looked at again whenever equipment or a nuclear pipe is placed or broken, never every tick.
- **Cooling water:** the condenser needs 4,000 mB/t at full power (once-through cooling).
  - The **Circulating Water Conduit** draws 1,000 mB/t from each water source block it touches, up to 16,000 mB/t a
    run. Lay it into a lake.
  - Ordinary fluid pipes, pumps and buckets work too.
- **Power out:** the main transformer gets the unit's power and pushes it into cables, or into the terminal of a Unit
  Substation touching it, and from there onto power lines.
  - Rated output is 524,288 JE/t at 100% on standard fuel with four loops.
- **The console:**
  - A benchboard section three wide (round 23), the same height and profile as the control room's system boards, so
    it stands in line with them: the digital rod position indicator, the nuclear instruments' recorders, three live
    screens and a strip of status lamps (red and flashing after a trip), and on the desk the rod bank selector, the
    rods' in-hold-out lever, the rod speed meters and the two reactor trip switches under red guards.
  - Link it with a **Reactor Link Card**: use the card on the vessel, then on the console.
  - Its screen follows the approved design: core map (193 assemblies, shaded by heat, with rod clusters), rod slider,
    plant readouts, a 10-minute core-temperature trend with the alarm and SCRAM lines, annunciator windows, the
    protection switch, the SCRAM button, RESET TRIP, a Personal/Party switch, and the fuel line with REFUEL.
- **The remote shutdown panel** (round 24): a vertical switchboard six wide, for tripping the reactor and holding it
  shut down from outside the control room, laid out as a real one is (three sections bolted together: Train A in
  orange, the reactor in the middle, Train B in blue). Each train has its indication (edgewise meters, digital meters,
  dials, a recorder), its control transfer switches in a yellow line, an auxiliary feedwater mimic with its switches,
  and the reactor coolant system's switches. Every device has an engraved nameplate; each switch's red or green lamp
  shows its component's state. The middle has the panel's title, a live status screen and lamps, two guarded reactor
  trip switches, the plant telephone and the sound-powered phone jacks. The evacuation procedure lies open on the
  ledge; caution, danger and number plates are on the doors below.
  - It needs no link card: it runs the **nearest reactor within 256 blocks**, found again whenever equipment changes.
  - Its screen shows that reactor live: power, rods in, core temperature, output, tripped or running, and whether
    the control room (MCR) or the panel (RSP) has control. The lamps show the same: white means the control room has
    control, amber means the panel has it, red means tripped.
  - Using it opens its window, with the same readings and two controls:
    - **Reactor Trip** drives every rod in (reset from the console).
    - **Take Control / Return Control** is the key transfer switch. While the panel has control, the keys stand
      turned and the amber lamps are lit. The console's screen reads CONTROL AT REMOTE SHUTDOWN PANEL, and the console
      can only trip the reactor until control is handed back.
  - It only works a reactor its user may run (the reactor's owner, or their party in Party mode).
- **The 480 V load center** (Switchgear Lineup, round 25): eight blocks long and holding power, built as a real one
  is: a black sill, then steel cubicles bolted together. On the left is the station service transformer's section
  (4160 V to 480 V), with louvred bolted covers, its temperature monitor and red voltage warnings. Next is the main
  breaker's section, with the bus's charge and volts meters above it. Then come five feeder sections, each with two
  draw-out breakers behind hinged doors and a pair of digital ammeters on top. Each breaker door shows the breaker's
  front through its cut-out (open and close buttons, the open/closed flag, the spring-charged flag, the charging
  handle and the racking slot), a red and a green lamp, and an engraved nameplate naming its feeder: MCC 1 to 4,
  INVERTER, CHARGER, HVAC, LIGHTING and two spares. The breaker lifting trolley rides rails along the top; the
  transformer section's top is its ventilation screen (the supply comes up from below). Each upper feeder in use has a
  conduit hub on top of its section; a spare's knockout is a blank cover.
  - Its bus holds **2,000,000 JE** and keeps it.
  - **In:** a cable into the back of its transformer section (either of the two left-hand blocks as you face it, any
    side) charges the bus while the **main breaker** is closed. It takes up to 16,384 JE/t.
  - **Out:** each closed feeder breaker sends the bus's power out of the back of its own section, up to 2,048 JE/t
    each. An upper breaker's power goes out of the top block; a lower breaker's power goes down through the floor.
  - **Right-click a breaker's door** to open or close it. Only the owner, or their party in Party mode, can do this.
    Right-clicking anywhere else shows the bus's charge, the main breaker's state and what is coming in.
  - The lamps, flags and meters are live. A closed breaker has its red lamp lit and a red flag; an open one has its
    green lamp lit and a green flag. The meters read the charge (%), the bus volts, and each feeder's amps.
  - In the medium plant, a buried cable from the main transformer comes up through the floor under each of the
    switchgear room's load centres. Each one's **CHARGER** breaker (the lower breaker of its third section) feeds its
    train's two battery chargers by a cable in the floor slab. A plant built with `fuelled` starts with every load
    center and station battery full.
- **Station batteries hold charge** (round 25): a Station Battery rack holds 1,500,000 JE. Any cable touching it can
  charge it or draw on it (4,096 JE/t). Right-click it to see its charge and whether it is charging, discharging or
  on float.
  - A **Battery Charger** takes AC from a cable into any block of it (it keeps a 40,000 JE buffer). It charges the
    nearest station battery within 24 blocks that no other charger has, at up to 1,024 JE/t (its 150 A limit).
  - Away from a diesel generator, the chargers' and the DC switchboard's meters read their battery:
    - Charging: up to 150 A, and the volts rise towards 135 V (equalize).
    - Full: float at 132 V.
    - With no AC: the AC lamp goes out, CHARGER FAIL comes in, and the volts fall with the charge.
- **Refuelling:**
  - Shut the reactor down (rods fully in, core below 120 °C), then press REFUEL. The vessel's head comes off and the
    core glows blue under its water.
  - Use a stack of assemblies on the vessel to load it. Use it with an empty hand to take assemblies out, spent ones
    first, up to 16 at a time.
  - Spent fuel is deadly: about 20,000 mSv/h a block away. An open vessel shines its spent fuel out of the top.
- **Radiation:** sources fall off with distance (strength / (1 + d²)), a little with the air they cross (by e every
  130 blocks), and with what is in the way. Each block lets a share through for its full thickness (a metre), less for
  a thinner slice or a part-filled block (slabs, doors), as real shielding thins radiation exponentially:
  - lead blocks, shielding concrete, uranium metal and depleted uranium: 0.1%; lead glass 1%; metal blocks 1%;
  - stone, concrete, brick and ore 10%; dirt, sand and gravel 20%; glass and water 30%; wood 50%; wool, leaves and
    the like 85%. (A real metre of stone stops nearly everything; these keep ore findable through a block or two.)
  - **Reach grows with the source:** a source counts as far as it can still be measured over background, up to 192
    blocks. Near you every radioactive block and container counts by itself; further off, each chunk section's
    radioactive blocks count as one source, a pile shielding its own middle, so a big pile reads like its near face. One
    uranium block fades into background within a few dozen blocks; a cube of a thousand still reads over 1 µSv/h 150
    blocks away.
  - The four-piece hazmat suit stops 90% of nuclear radiation (22.5% a piece). It is drawn in 3D: visor, respirator
    filters, an air tank on the back with its hose into the hood, harness, gloves and boots.
  - Dose builds up and slowly recovers. 500 mSv brings weakness, 1,000 nausea and hunger, 2,000 damage, and more
    above 4,000 and 8,000.
  - What is radioactive: a working core through its vessel (with its power; its decay gamma after shutdown), spent fuel
    (racks, casks, the open vessel), uranium and thorium from the ore up (ore, raw, dust, ingots, blocks, monazite),
    depleted uranium, enriched uranium and fuel, the check source. Counted placed, carried, dropped, stored in a
    container (a chest lets 80% out; a shulker box or bundle counts what is in it) or hung in an item frame.
  - The **Geiger counter** clicks at random, as real counts arrive, generated live from the server's dose rate with no
    recorded sound: a tick every few seconds on background, a patter by uranium ore, a rattle at a few mSv/h, a roar by
    spent fuel. Its **dial** is a real meter: a needle on a log scale from 0.1 µSv/h to 100 Sv/h with three bands,
    SAFE (green, below 50 µSv/h, where real plants rope off a radiation area), CAUTION (amber, to 1 Sv/h) and LETHAL
    (red). The needle swings like a real ratemeter's: it hunts about on background and steadies as the rate climbs.
  - The **readout** (top right, while you hold the counter or wear the hood or coverall) shows the rate (µSv/h below
    1 mSv/h) with its band, a live gauge with the same scale and needle, and the dose taken so far on a bar under it.
    It reads in creative too; only the dose is not taken.
  - **Storing radioactive material** (round 14): shielded stores let only a little of what is in them out (a chest
    lets 80% out):
    - **lead pig** (9 slots, 0.1%): a squat lead flask with a wire bail; picked up with what is in it, like a shulker
      box, and still shielding it in your inventory;
    - **lead-lined locker** (27 slots, 0.1%): a steel cabinet whose doors swing open while it is in use, showing the
      lead lining;
    - **shielded waste drum** (18 slots, 0.2%): a concrete-lined 208-litre drum, a full block high so drums stack;
    - **lead-lined door**: closed it stops 99.9%, open nothing. It closes itself (its door closer) five seconds after
      it was opened, once the doorway is clear. Lead-lined doors within six blocks of each other are an **airlock**:
      one won't open while another is open (a pair side by side counts as one door);
    - **lead glass**: amber shielding glass, 1% a block;
    - **warning beacon**: a wall-mounted beacon (steel bracket, ribbed lens, wire guard) that gives light; use it to
      change its colour (amber, red, green). A red or green beacon by a lead-lined door shows the airlock's interlock:
      green, the door may be opened; red, another door of the airlock is open. (In the plants it replaces the red cube
      warning light; the drone tower keeps its own.)
  - **The radwaste building** in every pre-made plant (medium: beside the auxiliary building's health physics
    entrance; large: where the waste treatment hall stood; small: across the road from the radwaste treatment hall),
    23 x 35: two layers of wall round its vault (the building's concrete wall, then the vault of shielding concrete 2
    thick with its own 2-thick roof) with an inspection corridor between them and lead-glass windows into the vault;
    the entry hall (health physics checkpoint: protective clothing lockers and bench, sample counter, the technician's
    desk, a step-off pad); the only way into the vault an airlock of two lead-lined doors with a shielded chamber
    between. In the vault: 52 waste drums two high along the back, 6 lead-lined lockers, 6 lead pigs on a steel bench,
    all empty, and the middle clear.
  - **The plants keep it in:** with every core at full power, nowhere in the open round a pre-made plant reads over a
    tenth of background (checked by a game test). The spent fuel pool has shielding-concrete walls round its liner;
    the small modular plant's modules stand under water in a shielded pool, in a shielding-concrete building.

## Pre-made plants
Three full-size plants (one block = one metre), approved from layout previews. Each is a **blue** blueprint with every
building, all the reactor equipment, the pipes and the conduits. Each large building is also its own **green** part
blueprint (for players who lay out their own plant).

| Plant | Site | Reactors | Cooling |
| --- | --- | --- | --- |
| Small Modular Nuclear Plant | 270 x 196 (+10 over the lake) | 6 Small Modular Reactors, each with its own Small Turbine-Generator | 2 mechanical-draft banks of 6 cells |
| Medium Nuclear Plant (4-loop PWR) | 336 x 262 | the stage-1 four-loop unit, 1 turbine-generator | 2 natural-draft towers, 150 tall |
| Large Nuclear Plant (EPR) | 440 x 336 | the EPR vessel, 4 loops, 2 turbine-generators | 2 large natural-draft towers, 175 tall |

- **Where:** stake it on a lake or river shore, facing inland. The intake pump house reaches 10 blocks out over the
  water, and its conduit legs go 4 blocks down into it. The rest of the plant reaches inland away from you.
- **Parts:** containment / reactor building, safeguard buildings (EPR), turbine hall, fuel, auxiliary, control,
  diesel, waste and office buildings, transformer yard, switchyard, intake pump house, and the cooling towers.
- **What the blue blueprint adds:** the equipment (placed whole by drones), Reactor Coolant Pipe, Insulated Steam Line
  through the walls to the turbines, Circulating Water Conduit from the lake to the condensers and on to the cooling
  tower controllers, and the roads. Equipment bodies cut through any building in their way.
- **Linking:** the consoles are in the control rooms; link each to its reactor with a Reactor Link Card (the build
  command does it for you).
- **Diesel generator rooms:** use the emergency diesel generator to start or stop it. Its room's panels follow it live.
  Each panel links itself to the nearest generator within 24 blocks that has no panel of its kind yet, so a generator
  has at most one of each of the six panels: put two generators by one set and the set follows one of them; put down a
  second set and it takes the other. A panel whose generator is broken links to another free one, or reads stopped. The
  panels show: engine speed, start air, lube oil and jacket water gauges, the hour meter (an
  hour every 1,000 game ticks run, counted from when the panel linked to its generator, so it starts at zero), the
  alarm windows (LOW DAY TANK LEVEL below a quarter; FAIL TO START when it had no fuel), the generator's volts, amps, kilowatts, kilovars, frequency and
  power factor, the breaker's lamps and mimic, the regulator's display and field meters, the protection relays'
  readings, the load sequencer's screen and step lamps, and the battery charger. A start plays out as a real
  emergency start on a loss of offsite power: cranking on air, running up to 514 rpm and 4.16 kV in about 4 s, the
  breaker closing at 5 s, then the sequencer starting the safety loads every 5 s (each motor start dipping the voltage
  and frequency for a moment) up to full load, 6,800 kW, with the battery chargers back last. A stop opens the breaker
  and coasts the engine down.
- **Diesel fuel:** the generator burns diesel as a real 7 MW emergency diesel does, a millibucket being a litre and an
  hour a Minecraft hour (1,000 ticks): about 110 mB an hour running light and about 1,690 mB an hour at full load
  (0.23 mB per kWh more), following the load sequence. It burns from its day tank: the nearest formed storage tank of
  diesel within 24 blocks, drawn through the tank's outlet (shut the valve and it starves). With no diesel it cranks
  but won't fire; run dry it stops. In the pre-made plant each room's day tank is kept topped up from the fuel oil
  storage tank outside through its fill line (while that tank's outlet valve is open), so keep the big tank filled. A
  storage tank holds four buckets for every block of space inside. With its roof hatch lid up, a tank also takes liquid poured in from a
  bucket (or any full fluid container) right-clicked on the hatch; it holds one liquid, so another is refused.
- **The medium plant's control building** (round 18, `tools/medium_plant.py` `control_building`), four floors laid
  out like a real plant's (NRC fire hazards analyses of TMI, Susquehanna and Peach Bottom; IEEE 484 battery rooms):
  - Grade: Battery Rooms 1A and 1B, each a 60-cell 125 VDC station battery on two step racks (Station Battery, four
    blocks long, 30 cells in clear jars), its cables up a DC Cable Riser from each rack's end into a DC Cable Tray under
    the ceiling, through the wall and down into the 125 VDC Distribution Switchboard in the DC Equipment Room next door,
    beside the battery chargers and Vital AC Inverters, with the 480 V load centre feeding them; the switchgear room
    (both trains' 480 V load centers face to face across an aisle of rubber mats, fed from the main transformer and
    feeding the chargers, above; their feeder conduits up through the ceiling into the cable spreading room's trays;
    emergency lights, an extinguisher);
    the remote shutdown room (the remote shutdown panel on its north wall over a rubber mat, battery emergency lights
    above it, an operator console three wide on the east wall, an extinguisher by the door); the locker room; and the
    secure entrance (a vestibule; the security counter, its chairs
    clear of the wall, and the camera desk; an entry and an exit Speed Gate, below, with framed glass from them to both
    walls). Hydrogen exhaust louvres high in the battery rooms' outside walls. The vestibule, the glass along the speed
    gates and the shift supervisor's office are Framed Glass (below).
  - First floor: the cable spreading room under the control room (three tiers of tray, risers up into it), the relay
    room, the server room on its raised floor.
  - Second floor: the main control room (round 23), laid out as a Westinghouse four-loop plant's horseshoe: along the
    north wall the Safety Systems, Reactor Coolant, the reactor control console in the middle (in line with the
    boards), Steam and Feedwater and Turbine Generator boards; down the west wall the Electrical board, down the east
    wall Radiation and Ventilation; the plant display wall over the middle. In front, three Operator Consoles three
    blocks wide (the reactor operator's, the balance of plant operator's and the senior reactor operator's behind
    them), each with its own screens, keyboards and phone; the shift supervisor's glazed office with the executive
    desks along its glass; procedures on shelves along the back wall and the office copier. Across the corridor the
    Technical Support Center, kitchen, washroom and operations office, with windows.
  - Top floor: the control room's emergency filtration and air conditioning (air handling units, chillers, chilled
    water pumps, ducts, louvres).
  - The stair tower serves all four floors; it is laid last so no floor cuts into it. The switchboard's and chargers'
    meters are live panels: with no diesel generator within 24 blocks they read the battery on float (132 V, 12 A).
- **Framed Glass** (`nuclear/FramedGlassBlock.java`, `tools/framed_glass.py`; Justin, 8 October 2026: the vanilla
  panes "don't connect all the way" to the glass doors; "as continuous as possible, just glass, until it meets some
  kind of corner"): a dark bronze aluminium storefront partition. Its panel is 3 px deep, at the edge of the block
  nearest whoever places it, exactly where a door placed the same way has its leaf; placed beside a door, or on or
  under other framed glass, it takes that plane by itself, so glass and doors make one face. The glass runs on unbroken
  through framed glass beside, above and below it (no mullions or rails), and the frame shows only where the glass
  needs support: a jamb at an end (a wall, a door, nothing), a corner post where a run turns inside the block (the
  block in front is framed glass at right angles; a stub of glass runs along the side to it), two jambs where runs
  meet at an outside corner, a sill and a head along the bottom and top, and round a door set in it (jambs up to the
  door's head, the head over it, glass above). Clear glass with a faint tint, no sheen. The Glass Door is built from
  the same parts: a 3D frame, real glass and a push bar. Recipe: 4 glass panes with an iron nugget between each pair
  make 4.
- **Control boards** (round 23; `tools/control_boards.py`, `client/ControlBoards.java`; Justin, 8 October 2026: "a
  realistic reactor control board and varied boards"): six benchboards 6x2x3 for show, each its own system's layout
  rather than one board repeated: **Safety Systems** (Train A in orange and Train B in blue, the pumps from the
  refuelling water tank, the ESF status panel, the manual safety injection switches under guards), **Reactor
  Coolant** (the four loops round the vessel, the pressurizer, charging and letdown), **Steam and Feedwater** (each
  steam generator's meters, recorder, steam and feed lines and switches), **Turbine Generator** (the generator's
  meters and synchroscope, the turbine mimic, the control system's lit push buttons, the turbine trip), **Electrical**
  (the switchyard and station busses with breaker switches set into the mimic, the diesel generators' controls) and
  **Radiation and Ventilation** (the monitors' drawers, the ventilation mimic, fire detection). Every part is real
  geometry standing off the board: switches with their red and green lamps and labels, edgewise meters, recorders
  with lit charts, controllers, mimic lines with pumps and valves, annunciator windows (a few in alarm, one flashing
  now and then). The reactor control console is drawn the same way. The boards' parts are checked never to share a
  face's plane (no z-fighting).
- **Operator Console**: a black desk with a raised shelf for its screens that joins up side by side; each block has
  its own setup (two screens and a keyboard; one wide screen, keyboard and phone; stacked screens and an open
  procedure; a screen, phone and papers), everything inside its own block.
- **Feeder Conduit** (round 25b): 4-inch rigid steel conduit for a load center's feeders, in pieces that stack in a
  column: the hub piece over a load center's section, plain lengths with couplings, the ceiling piece (its red
  fire-stop and sleeve), the floor piece (sleeve, fire-stop and a yellow strut clamp), and the tray piece, which turns
  in under a cable tray on its south side through two cast fittings. In the medium plant, each in-use upper feeder
  of the switchgear room's load centers runs up into the cable spreading room's lowest tray.
- **Emergency Light** (round 24): an 8-hour battery emergency light for high on a wall. It has a cream case with
  louvres, a test button and a green charging lamp, and two sealed-beam lamp heads on swivels on top.
- **Speed Gate** (`nuclear/SpeedGateBlock.java`, `SpeedGatePedestalBlock.java`, `SpeedGateBlockEntity.java`,
  `client/SpeedGateRenderer.java`, `tools/speed_gate.py`; Justin, 8 October 2026, after the sliding glass fare gates
  at Grand Central): a one-way security lane. Placed looking the way through, it stands on the block you come in by,
  keeps the block ahead clear and puts a brushed stainless pedestal either side of both (two blocks long, rounded
  ends with a card reader and screen, an arch over the door line). Two tall glass doors, edge-lit with green light
  bars, meet in the middle; walk up to them the way through and they slide back into slots in the pedestals (brush
  seals either side of each slot) and close a moment after the lane is clear. From the other side they stay shut, and
  shut they cannot be jumped. Lights along the pedestals show blue waiting and green open; a sign gantry over the lane
  shows a green arrow to the way in and a red cross to the other side. Lanes stand side by side on a shared pedestal:
  an exit lane beside an entry lane facing the other way (as at the plants), or more of either. Each pedestal has two
  slots, one each side of the line, and the lanes along a row take them in turn, so the doors of two lanes never meet
  inside a pedestal. Breaking the lane block takes the pedestals only it used; breaking a pedestal takes the lanes
  beside it. One item, crafted from glass panes, steel plates and redstone.
- **Building:** drones build from the lowest unfinished layer up a few layers at a time (see the blueprint system
  doc). In creative, `/nuclear build <blueprint> [fuelled]` builds one at once.
- **Checked before building:** `tools/nuclear_plants.py` follows the pipes the way `ReactorUnit` does and refuses to
  write a plant whose units would not form (loops, turbines, condensers, transformers, and the conduit reaching both
  the lake and the towers). It also re-reads the written blueprints the way the game does.

### Storage tank ratings
Justin, 7 October 2026: "at the very least... one for caustic, one for flammable, one for general liquids", with the
painted side filling itself in from what is inside, and every tank placeable as one item. Design sheet approved the same
day ("silo design sheet looks fine").
- **One tank, four ratings.** The outlet (the tank's controller) carries the rating (`nuclear/TankRating.java`); forming
  paints every plate for it.
  - **General:** white, the checker band, the gooseneck vent. Takes water, brine, nutrient solution, fracking fluid,
    flowback water and lubricant.
  - **Flammable:** a red product stripe under the band, a pressure/vacuum vent on a flame arrester, two foam chambers on
    the top course with their foam lines down the shell, and an earthing boss at the foot. Takes crude oil, naphtha, the
    gasolines, bioethanol, kerosene, the diesels and heavy fuel oil, and any general liquid.
  - **Corrosive:** rubber-lined, so the shell is a darker grey with a yellow stripe and a scrubber vent; the kit adds a
    spill bund round the foundation. Takes lye, sulfuric acid, nitric
    acid and vanadium electrolyte, and any general liquid.
  - **Cryogenic:** a tall white vacuum-insulated tank on legs, with relief valves on its roof and an ambient vaporizer
    beside it; no ladder or hatch. Takes liquid oxygen only. Made only as its own kit.
- **The wrong liquid is refused** at the inlet, the outlet's line and the roof hatch, with a message naming the rating it
  needs. Gases go in no storage tank (the gas holder or gas cylinders take them), and nor does lava (the steel tank does).
  A liquid from another mod counts as general. A tank already holding a liquid before the ratings came in keeps
  taking that liquid.
- **The label** (`nuclear/TankProducts.java`, drawn by `client/StorageTankRenderer`) is painted on the widest clear
  side. It shows the product's name, its UN number, the NFPA 704 fire diamond with its real health, fire and
  instability numbers and special mark (OX oxidizer, W don't add water), and its warning lines (FLAMMABLE, NO SMOKING,
  NO NAKED FLAMES; CORROSIVE, DO NOT ADD WATER, WEAR FACE SHIELD; OXIDIZER; CRYOGENIC LIQUID). Liquids rated 0-0-0
  show their name alone. The label keeps the last product while the tank is empty and changes when a new liquid goes
  in. A named name tag used on the tank adds a title above the product (the tank screen shows it too). The plant's
  tanks keep their tags (CST-01, DT-1A...).
- **NFPA numbers** come from NFPA 49 tables and Wikipedia's NFPA 704 page. For mixtures without a published rating
  they are estimates: fracking fluid and flowback water 1-0-0, lubricant 0-1-0, naphtha 1-4-0, vanadium electrolyte
  3-0-0.
- **Re-rating.** A flame arrester or a rubber lining used on an empty tank re-rates it: repainted, its vent swapped,
  foam chambers added or taken off. A tank holding anything is refused ("empty the tank first"). A cryogenic tank
  keeps its rating, and no other tank can become cryogenic.
- **Tank kits.** One item places a whole tank, level with the ground and reaching away from the player: its foundation
  set into the ground, shell, roof, nozzles, fill line, manway, caged ladder, railing and gate, hatch and the fittings of
  its rating. The tank forms at once. It needs clear room: grass and flowers go, anything solid refuses the kit with
  where it is in the way. Structures: `data/jugcraft/tank_kit/*.jugbp.json`, written by `tools/tank_kits.py` with the
  plant's own tank code. Justin allowed this one exception to the 5×5 MBS rule. While a kit is held, a see-through copy
  of the formed tank shows where it would go (red where something is in the way). Breaking any block of a kit's tank
  takes the whole tank down and gives the empty kit back; its liquid is lost (Oct 8; see
  docs/features/blueprint-system.md, "MBS placement preview and breaking").

  | Kit | Shell across | Height | Holds |
  |---|---|---|---|
  | Small (general, flammable, corrosive) | 5 | 6 | 216 buckets |
  | Medium | 9 | 8 | 1,184 buckets |
  | Large | 13 | 10 | 3,560 buckets |
  | Cryogenic | 5 | 11 | 220 buckets |

  The large kit holds more than the design sheet's "about 2,500": the sheet's figure was a rough guess.
- **Recipes.** Tank shell section = 9 storage tank plates. Small general kit = 4 shell sections, the outlet, inlet,
  caged ladder and 2 railings. Medium = 4 small; large = 4 medium. A flammable kit = a general kit + a flame arrester;
  a corrosive kit = a general kit + a rubber lining. Flame arrester = steel plates, iron bars and a fluid valve. Rubber
  lining = 8 rubber round a steel plate. Cryogenic kit = 2 relief valves, 2 ceramic insulators, 2 shell sections, a
  small general kit and 2 vaporizers.
- **In the plant:** the diesel fuel oil tank (DFO-01) and both day tanks are flammable-rated. The day tanks indoors
  have no foam chambers.

### The new reactors
- **EPR vessel:** 241 assemblies (a 17-wide core) and 1.6 times the standard output. Each turbine-generator takes one
  standard unit's output, so it needs two to use it all. The console's core map shrinks its cells to fit.
- **Small Modular Reactor:** the reactor, steam generator and pressurizer in one module (natural circulation: no
  pumps). 37 assemblies, an eighth of the standard output. An Insulated Steam Line joins it to a **Small
  Turbine-Generator**, which is its own condenser (cooling water in) and transformer (power out into cables).

### Cooling towers
- Each tower blueprint has a **Cooling Tower Controller** in its basin. It checks its tower against the tower's
  blueprint every five seconds (160 blocks spread over it; 90% must be in place).
- A condenser whose Circulating Water Conduit reaches a formed controller sends its heat up the tower: for that share of
  the heat it only needs **make-up water** (5%) instead of the full once-through flow.
- Capacity, in standard units of heat: natural-draft tower 1.0, large tower 0.9, mechanical bank 0.4. Reactors on the
  same tower share it.
- A busy tower steams: a white plume (always drawn, however far away) as strong as the tower is busy.
- The basins are left dry (drones can't carry water).

## How it runs (the model)
- **Rods:** they move at 2% a second toward the console's setting, and drop fully in within two seconds on a SCRAM.
  Fission follows the rods and the fuel loaded.
- **Decay heat:** after shutdown the core keeps making decay heat, starting at 6.5% of the power before and fading
  over minutes. It is part of the rated heat, so the plant never has to carry more than 100%.
- **Cooling and temperature:**
  - Heat carried away is the smaller of the heat made and (loops ÷ 4) × the share of cooling water supplied.
  - The core settles near 300 °C. Any heat not carried away raises it.
  - It alarms at 350 °C. With protection on, it SCRAMs at 370 °C.
  - With protection off, 1,200 °C damages the fuel. The core then shuts down until it is refuelled. Meltdown and the
    blast are stage 4.
- **Standard fuel can't overheat with full cooling, even with every rod out.** This is checked by a game test.
- **Power and burnup:**
  - Electric output is the heat carried away × 524,288 JE/t, but only while a turbine-generator, a condenser and a
    main transformer are in the unit.
  - A full core at 100% lasts about a day of play. Each assembly carries its share of the power.

## Connections
- **Inputs:**
  - uranium ingots (the uranium feature);
  - steel, tungsten, nickel and aluminium parts, processors and advanced circuits for the equipment;
  - water.
- **Outputs:** electricity into cables and power lines (the Unit Substation), and spent fuel.
- **Entry path:** mine uranium, then build the cascade and fabricator (electronics tier), then the equipment.
- **Radiation is shared:** future micro black holes will add Hawking-radiation sources, which the normal suit only
  slows down.

## Balance and automation
- **Fuel:** a full core is 193 assemblies, which is 97 cylinders or 776 uranium ingots, for about a day at 524,288 JE/t.
- **Machine power:** the cascade uses 1,024 JE/t for 60 s per cylinder; the fabricator 256 JE/t for 20 s.
- **Equipment recipes:** each costs several blocks of steel. The vessel also needs tungsten plates, a processor and a
  fuel assembly.
- **No duplication:** fuel and spent fuel are only made from uranium. Spent fuel can't go back in.
- **Gaps:** there is no automation of refuelling yet. Hardened drones and spent fuel pools come in later stages.

## Multiplayer and persistence
- **Server authority:** all simulation runs on the server, in the vessel's block entity. Clients only draw the synced
  readouts, sent every half second.
- **Console permissions:** console actions are a payload the server checks for reach (10 blocks) and permission:
  - the owner, a creative-mode player, or the owner's party when the console is in Party mode;
  - only the owner can switch that mode.
- **Persistence:** the core state, fuel burnup, condenser water and transformer charge are saved. Links are block
  positions.
- **Unloading:** while unloaded nothing ticks. The unit is worked out again when it loads.

## Performance
- Each piece's geometry is built once per kind and detail level and kept (`NuclearRenderer.Mesh`), then replayed
  every frame, turned to its facing. Only the vessel's drive rods (one mesh per 5% of travel) and the console's
  lamps change.
- Beyond 40 blocks a piece draws its low-detail mesh: half the sides on round shapes, no weld bands, signs,
  flanges or nameplates, and a sparser drive forest.
- Textures are 64 px a block, drawn at 128 px and reduced to a small palette (`tools/nuclear_materials.py`), so they
  carry real detail (brushed metal, rust at seams, chipped paint, poured concrete) without standing out from vanilla.

## Dependencies and assets
- **Dependencies:** none beyond Fabric API.
- **Assets:** all art, models and the Geiger sound model are original and generated by `tools/nuclear.py`:
  - the hazmat sheet;
  - the 512 px equipment material sheet;
  - the fuel machine models;
  - icons, pipe models and the console's SCRAM button.

## Verification
- `tools/check_mod_data.py` checks that the Java matches `tools/nuclear.py`: equipment ids and sizes, pipe ids, and
  the renderer's and hazmat layer's material regions.
- **Focused game tests** (`13-test-focus.bat` with focus `nuclear`):
  - radiation falloff, shielding and the suit;
  - dose sickness and carried sources;
  - the core model: no overheat on standard fuel, loss of cooling trips at 370 °C, protection off damages the fuel;
  - equipment placing and breaking whole;
  - a unit joining through coolant pipe;
  - console permission and refuel rules;
  - the Geiger sound following the rate;
  - a whole showcase unit forming four loops and making power;
  - written, not yet run: the plant blueprints load whole, the EPR and module cores and cooling towers behave, and
    a built small modular plant forms six units with both tower banks (`NuclearPlantGameTests`); a screenshot tour of
    all three plants running (`NuclearClientGameTests`).
- **Screenshots** of every piece, the console screen (running and tripped), the open vessel and the fuel machines.

## World and event applicability
- **Placement:** player-built industry only. Nothing generates in the world.
- **Radiation:** from what players make and from natural uranium and thorium ore (a counter finds it underground).

## Rollout and open questions
- **Placement:** equipment bigger than 5x5 is placed from one item in this stage. The proposal's drone-built
  equipment blueprints come with the pre-made plants.
- **Stage 2 and later:** the spent fuel pool, dry casks and hardened drones; cooling towers and the water intake port;
  HALEU and MOX; meltdown, blast, fallout and corium; the small modular and EPR plants.
- **Operator commands:** `/nuclear showcase` builds a running unit; `/nuclear build <blueprint> [fuelled]` builds any
  blueprint at once; `/radiation dose|check` are for testing.
- **Not yet:** the plants' transformer yards and switchyards are scenery: connect the main transformers (or the small
  turbines) to power lines yourself. Basins are dry.
