# Automated warehouse

Status: parts W1 (inbound), W2 (outbound), W3 (floors and scale) and W4 (liquids) implemented; W5 planned
Proposal issue: none yet (Justin's design, run past him with mockups first)
Owner: Narvisius (Justin)
Target milestone and tier: mid-game logistics (after item pipes and basic circuits)
Primary specialty and supported player role: logistics and storage, for builders and server bases

## Player experience
A warehouse that runs itself. Players pipe items in (or drop them in through a hatch) and drones store them:

1. **In.** Items go into an **Intake Case Packer**: through its back ports, through the four pipe inlets of a **Goods In
   panel** set in the outside wall behind it, or by hand in the panel's GOODS IN screen (a 54-slot drop-off bay). The
   machine packs them into cases, one kind of item to a case, up to four stacks, and runs the cases along its conveyor to
   the stop.
2. **Palletize.** A **Stacker Drone** (a mobile robot arm) carries each case to a **Palletizing Station** and stacks it on
   the pallet there (eight cases, two layers; the station's magazine holds the empty pallets players load into it). A
   full pallet, or one that has waited 30 seconds for more, is stretch-wrapped (cartons, sacks) or strapped (crates,
   drums, ingots) while the station's turntable turns it.
3. **Put away.** A **Pallet Drone** (a turret truck: its forks turn to either side and traverse, so it never turns in an
   aisle) takes the finished pallet to the nearest free place in the **Pallet Rack** (up to five levels) and sets it in.
4. **Books.** The **Warehouse Control Terminal** knows every pallet, case and item and where it is (STOCK), and every
   drone and what it is doing (FLEET). Its three desk monitors show the fleet, the stock and the last pallets put away.

The drones drive on **Warehouse Lane Tiles** (yellow lines show the lanes' edges), park and charge at **Drone Charging
Docks**, go home when idle or low. Players never take from the racks; using a pallet place shows what is on it.

Taking things out (part W2):

5. **Order.** A player asks for an amount of one item: in the terminal's STOCK page (REQUEST, under the item's detail) or
   at the **Goods Out panel**'s kiosk screen, to the WINDOW or to PIPES. The terminal's ORDERS page (and its third desk
   monitor) lists every order: who, what, how much, QUEUED, FETCHING, PICKING, READY or SENT. A player may cancel their
   own, or send one waiting at the window to pipes instead.
6. **Fetch.** A pallet drone brings a pallet holding the item from its rack place to the dispatch hatch of **Ready
   Storage** (a vertical lift module: 3 wide, 4 tall, 2 deep) and sets it on the hatch's roller bed. The shutter rolls up,
   the bed runs the pallet into the pick cell, and a vacuum gantry lifts off whole cases (or splits one) until the order
   has what it asked for; it takes as many pallets as the order needs.
7. **Back.** The pallet rolls back out: with cases left on it, a drone puts it back in its place; emptied, it goes to a
   station that has room for empty pallets, or (none within 15 seconds) up into Ready Storage as a pallet item.
8. **Collect.** What was picked goes into Ready Storage's slots (108; each **Ready Storage Extension** stood on top adds
   108), tagged with the player who ordered it. The Goods Out panel, set in the outside wall in front of it, has the
   **pickup window**: its screen shows Ready Storage's slots (MINE or ALL, a search, TAKE ALL), and its tray slides out
   with the next order's goods when someone opens it. Orders for PIPES go into the **Output Port**'s nine-stack buffer
   instead, and the port pushes them into the pipe on the panel's OUT flange (extractors may pull from it too).
9. **Streams.** The port can also keep up to four **streams**: an item sent out all the time, up to a rate a second,
   while the warehouse keeps at least a reserve of it (the stream shows HELD when it is down to that). The port's mode is
   OFF, ORDERS, or ORDERS + STREAMS.

Floors and scale (part W3):

10. **Floors.** A warehouse can go up floor on floor. Each floor has its own lanes, racks and drones; a **Pallet Lift**
    (a vertical reciprocating conveyor: a landing on each floor, shaft sections between, the drive on top) carries
    pallets between them. Drones set a pallet on a landing's bed, the gates close, the carriage runs up or down, and a
    drone on the other floor takes it off. Aisles are named by floor (2B) and places by floor, aisle, bay and level
    (2B-04-1); the terminal's FLEET shows each drone's floor.
11. **Fittings.** Pedestrian walkways railed off from the drones, crossings with flashing beacons, aisle signs that show
    their aisle, floor, bays and levels, rack end barriers, bollards, high-bay lights, sprinklers with in-rack lines
    (the **Rack Sprinkler Kit**), the floor slab with the metal deck and girders under it, and a control office.
12. **Pre-made warehouses.** The **Small** (32 x 20), **Medium** (40 x 26) and **Large Warehouse** (48 x 32) are
    blueprints. With one in hand, shift+scroll picks how many floors it is staked with (2 up to as many as fit under
    the build limit: about 31 from y 64). Outside they are real buildings: insulated metal wall panels on a precast
    plinth, ribbon windows of framed glass, the parapet and coping, downpipes, wall packs that light at night, a
    membrane roof with rooflights and smoke vents, door canopies, a yard with bollards, and an enclosed stair tower with
    real steel stairs (two blocks wide, a real angle, guard rails you can't walk through, walked like stairs).
13. **ADD A FLOOR.** The terminal's BUILDING page shows the building's floors (what is on each, its places in use), its
    lifts and stairs, the height it takes and how many more floors fit. ADD A FLOOR stakes one more floor over the
    roof: the ladder up to the roof hatch and the lift's drive come off the old top floor (given back to the player),
    and Drone Towers in range (with the player's items or the party's) take the roof and parapet off and build the new
    floor, the roof going back on top. The page shows what it still needs and how far it has got, and can cancel it.
    It works for any of the pre-made warehouses, the medium nuclear plant's included.
14. **Fire.** The office's releasing panel and valve station run the building's sprinklers: smoke detectors on every
    floor, the heads over a fire open, the in-rack lines too, and the roof's smoke vents open on the alarm (and close
    on RESET).

Liquids (part W4):

15. **Drums.** Liquids are stored in drums of 20 buckets, four to a pallet (80 B), ring-strapped. The liquid picks the
    drum by the kind the storage tanks use: a blue **Steel Drum** (general), a red **Flammables Drum**, a white **HDPE
    Drum** (corrosive) or a stainless **Liquid Dewar** (cryogenic). Gases and lava are turned away. A drum holds one
    liquid; the pallet's last drum may be part full. A drum taken off a pallet keeps its liquid and can be emptied or
    filled like a tank item.
16. **In.** The **Liquids In panel** (3 x 2 x 1, in the outside wall) has four flanged inlets LIQ 1-4 and a tanker
    coupling for fluid pipes, and a funnel: its LIQUIDS IN screen takes buckets or other fluid containers (nine
    slots, TIP ALL, TAKE BACK) and shows each inlet's liquid and rate. Behind it the **Drum Filling Station** (3 x 3 x
    2) takes the liquid into its 80 B buffer tank (one liquid at a time; the other inlets show WAITS), and its gantry
    robot takes an empty drum of the right kind from its magazine, sets it on the pallet on the weigh scale, seats the
    fill head on the bung and fills it (8 B a second, bottom-up through the lance), drum after drum. A pallet of four
    (or one that waited) is strapped, and a pallet drone puts it in the racks like any pallet. Players load empty drums
    and pallets by using them on the station (eight drum places, eight pallets); a liquid waits while its drum kind has
    run out.
17. **Stock.** The terminal's STOCK page filters ALL, ITEMS or LIQUIDS; liquids show their amount in buckets, their
    drums and pallets, the drum kind, an NFPA diamond and UN number. Racks show the drum pallets.
18. **Out.** A liquid order (terminal or Liquids Out screen; an amount in buckets, +1 B, +DRUM, PALLET, ALL) goes to the
    FILL POINT or to PIPES. A pallet drone sets the pallet of drums on the **Drum Emptying Station** (3 x 3 x 2, by
    Ready Storage), whose jib crane swings the drum pump from drum to drum, least-full first, into its 80 B holding
    tank. What is left goes back to its rack place; emptied drums go to a filling station's magazine. The **Liquids Out
    panel** outside has the bucket fill point (hold an empty container under the nozzle, or FILL MY BUCKETS on the screen) and
    the OUT flange, which pushes orders for pipes and up to four **liquid streams** (a liquid at 0.1 to 10 B a second,
    keeping a reserve) into the pipe. Its screen shows the player's orders, the holding tank and the flange.
19. **Pre-made warehouses.** The Small, Medium and Large Warehouse and the medium plant's warehouse have the liquid line
    in their front row (the Small Warehouse is six blocks wider for it).

Fleet and control (part W5):

20. **Power.** A warehouse runs on power brought in by cable. The **Warehouse Switchboard** (3 x 2 x 2) stands with its
    back row in an outer wall: outside, the service entrance (the meter cabinet with the live meter, the red main
    switch, the CABLE IN box: bring a Jugcraft cable to any of its faces); inside, the main distribution board (the
    incomer's breaker and POWER screen, six outgoing breakers with their lamps and toggles: DOCKS, LIFTS, STATIONS,
    READY STORAGE, LIQUIDS, LIGHTS · CONTROL; the UPS screen). It stores 100,000 JE (up to 4,096 JE/t in). The warehouse
    draws 8 JE/t, 24 per drone charging, 64 per moving lift, 8-32 per working machine. Short of power, charging slows
    first, then the machines' circuits are shed (a cycle under way finishes; nothing new starts; the lamp turns amber).
    Its screen switches the circuits and has MAIN OFF (stop all) for maintenance. **A warehouse without a switchboard
    does not run** (existing ones need one); `/warehouse demo` has unlimited power.
21. **Status Board** (3 x 2 x 1, on a wall): the live floor map (lanes, walkways, the racks coloured by how full their
    places are, the pieces, the drones as they move) and the day's numbers (places, orders, drones, power, counts, what
    housekeeping is doing). Using it shows the next floor, or each in turn.
22. **LAYOUT** (terminal): the live floor map, floor by floor, FILL or COUNT colours; hover a rack for its places,
    click a drone for what it is doing and its buttons: LOCATE (its beacon flashes blue for 10 s), PAUSE / GO ON, SEND
    HOME, OUT OF SERVICE (it stays on its dock). **CONTROL**: JOB PRIORITIES (ORDERS OUT, PUT AWAY, LIQUIDS,
    HOUSEKEEPING, moved up and down; lift transfers and pallets coming back off hatches and stations always first; a RUSH
    order, set on the ORDERS page, ahead of everything); HOUSEKEEPING (consolidate OFF / WHEN IDLE; cycle count OFF /
    DAILY / NONSTOP; counted today; the last adjustments); POWER (in, drawn, stored, each circuit with its breaker);
    ACCESS; PAUSE ALL DRONES (they finish their move and stop).
23. **Consolidation.** When the fleet has had nothing else to do for 30 seconds, two part pallets of one item that fit
    on one are put together: a pallet drone sets the bigger on a palletizing station's turntable, another holds the
    smaller over the lane beside the stacker, which moves its cases across one by one; the pallet is wrapped again and
    put away, the empty one goes into another station's magazine. One place freed.
24. **Cycle counts.** Idle pallet drones drive to each rack column and read its places' labels with their scanners
    (red scan lines), once a day (DAILY) or round and round (NONSTOP); what is on a place is checked against the books,
    a difference is put right and logged.
25. **Signal Box** (1 block on a wall, its screen and PWR, LINK, IN, OUT LEDs): one rule a box. OUTPUT gives a redstone
    signal from every side: stock below or above a number (its item set by using the box with it), the stock's level
    (0-15), an order ready, the fleet busy, low power, the places full, the fire alarm. INPUT: while it gets redstone,
    PAUSE FLEET, STREAMS OFF, INTAKE OFF, LIQUIDS IN OFF, CONSOLIDATE NOW. **Comparators** read a rack place (how full
    its pallet is), Ready Storage (its slots), the filling station (its tank), the emptying station (pumping, a pallet
    on it), the switchboard (stored power) and the terminal (places in use).
26. **Access.** A warehouse is its terminal owner's: who placed the terminal (a pre-made one's is claimed by the first
    player to open it). PERSONAL: only the owner uses its panels, terminal, switchboard and signal boxes; PARTY: the
    owner's party too; only the owner changes ACCESS. Others are told whose it is. The Status Board shows to anyone.
27. **Pre-made warehouses**: the switchboard in the office's front wall (its service entrance outside, by the office
    door), the Status Board on the office's east wall.

## Connections
- Existing input producer: item pipes (brass item pipes and extractors), any player's inventory; fluid pipes and
  buckets (the Liquids In panel).
- Existing output consumer: the Goods Out panel's pickup window (players) and its Output Port (item pipes, extractors);
  the Liquids Out panel's fill point (buckets) and OUT flange (fluid pipes).
- Technology connection: crafted from steel plates, electric motors, basic and advanced circuits, processors, item pipes.
- Magic connection: none (infrastructure).
- Reachable entry path: steel and circuits come before it; nothing it makes is needed to make it.
- Which connections are required vs optional: an intake, a station with pallets, a stacker drone, a pallet drone, lanes,
  rack places and a terminal are the minimum working set; the Goods In panel and trunking are optional.
- For infrastructure: storage does not create or convert items; everything put in can be got back (W2).

## Balance and automation
- A case holds up to four stacks of one item; a pallet holds eight cases (32 stacks).
- The case packer packs one case every 2 seconds (a part case once nothing more of its item has come for 5 seconds); its
  conveyor holds five cases.
- Drones drive a lane tile in 8 ticks and turn a quarter in 8; their batteries run down while they work and charge at
  their docks (they go home below 20 % and work again at 90 %).
- Capacity: one pallet per rack place; the terminal finds up to 40,000 warehouse blocks within 96 blocks of it.
- Outbound: a hatch cycle is a second for the shutter, 1.4 seconds for the bed each way, 2 seconds a whole case and 4 a
  split one. Ready Storage holds 108 stacks per module; the port's buffer nine stacks and it pushes up to 32 items each
  half second. A player may have 16 orders open at once; an order is cut to what is stored and not already promised.
- Liquids: 20 B a drum, 80 B a pallet; the filling station fills 8 B a second, the emptying station pumps 8 B a second;
  each has an 80 B tank. The OUT flange pushes up to 10 B a second.

## Multiplayer and persistence
- Server authority: the terminal plans every drone move and carries out its events (a case taken off the conveyor, a
  pallet set in its place) on the server at their ticks. Clients only animate the moves they are sent.
- Server-light: no entities. Each drone is a record in its terminal; a move is sent once to the players near (one
  packet), and they animate it on the game clock. Racks' pallets are part of their block models (no per-frame cost).
- Persistence: the drones, their current moves, the stock (on the racks' pallets), the stations' pallets, the intake's
  bay and conveyor are saved. After a restart the terminal finds its warehouse again and the moves carry on.
- Breaking a piece drops what it holds (the bay, the pallets, a rack place's pallet and its items, a dock's drone; what a
  drone was carrying when its dock went; the pallet on Ready Storage's bed). Ready Storage's slots and the port's buffer
  are kept by the terminal (one pool for all of a warehouse's Ready Storage) and drop where the terminal is broken.
- Orders, streams, Ready Storage's slots (with their owners) and the port's buffer are saved; an order whose pallet was
  lost on the way goes back to QUEUED.
- Each player sees only their own slots under MINE; anyone with access may take from ALL. Part W5: PERSONAL / PARTY
  access (the owner and access mode, job priorities, housekeeping modes, circuits, the paused fleet, the counts and the
  books they check against are saved with the terminal; a drone's PAUSED and OUT OF SERVICE with the drone).

## Dependencies and assets
No new dependencies. All models and textures are drawn by `tools/warehouse.py`, `tools/warehouse_w3.py`,
`tools/warehouse_w4.py`, `tools/warehouse_w5.py` and
`tools/exterior_kit.py` and written by `tools/warehouse_data.py` and `tools/exterior_data.py` (original art). The
pre-made warehouses are built by `tools/warehouse_bp.py` (their parts and `stacks.json`).

## Verification
- `WarehouseGameTests` (focus `warehouse`): the lanes and racks join up; 2,048 cobblestone put into the intake end up as
  one wrapped pallet of eight cases in a rack place, and the terminal's books show it; an order of 100 coal from a stored
  pallet of 2,048 comes out READY in Ready Storage (100 in its slots for that order, 1,948 back in the books, the hatch
  free again).
- `WarehouseGameTests.aLiquidIsDrummedAndPutAway`: 60 B of water into a filling station become three drums on a pallet
  in a rack place (the books show 60 B), one empty drum left in the magazine. `aLiquidOrderIsPumpedOut`: an order of
  30 B from a pallet of four 20 B drums is READY in the emptying station's tank, the pallet goes back with 50 B.
- `WarehouseGameTests.aPreMadeWarehouseGrowsAFloor`: the Small Warehouse staked with three floors is found by its
  terminal (three floors); ADD A FLOOR stakes the fourth over the roof, the lift's drive comes off and is given back,
  and the old roof's band turns into plain wall.
- `tools/warehouse_bp.py check_stacks`: a pre-made warehouse stacked from its parts is exactly the building built with
  that many floors (2 to 6, every block).
- Startup check (outside the game): every warehouse and exterior block state builds; the stairs' treads step up a sixth
  of a block at a time; every warehouse and exterior state in the stacked blueprints parses and every floor count composes; the stacker's arm reaches every point the planner
  asks of it; drones routed on a lane field never share a cell; the pick cell's plans (whole cases, split ones, room left)
  are right, the gantry's case clears the load and the carriage; the filling station's gantry keeps in its travel for
  every drum kind, magazine place and pallet spot, carries drums over the magazines, and ends parked; the emptying
  station's jib reaches every drum; in a consolidation, the strafed stacker reaches every case of the pallet held out
  to it, facing each way, and the held pallet clears the stacker's deck.
- Part W5: `twoPartPalletsAreConsolidated` (3 + 4 cases of cobblestone end up as one pallet of seven in one place, the
  books unchanged), `aSwitchboardPowersTheWarehouse` (no store: nothing runs; a store: powered; a circuit off; the main
  switch off), `aSignalBoxSignalsLowStock` (STOCK BELOW lights a lamp, then not), `anIdleFleetCountsThePlaces` (every
  place counted). The existing tests' warehouses run with unlimited power.

## World and event applicability
Not applicable: a player-built machine.

## Rollout and open questions
- W5 Fleet and control is built (power by cable, the status board, LAYOUT and CONTROL, priorities and RUSH,
  consolidation, cycle counts, the signal box, comparators, PERSONAL / PARTY access). Existing warehouses need a
  switchboard and a cable after the update.
