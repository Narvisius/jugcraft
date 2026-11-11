# Nuclear power (stage 1)

Status: implemented, draft pull request. Stage 1 passed focused game tests on Narvisius's PC. The pre-made plants,
their buildings and interiors, storage tanks and the rest below were played in single-player test builds up to the
switchgear room (7a-7c); the later control-building rooms (switchgear 7d-7f, the DC rooms, battery rooms, relay room,
control room HVAC, Technical Support Center, operations office, restroom, the live control boards) and `/nuclear plant` have not been played yet. Game tests are written for
them; not all have been run. No dedicated-server test yet.
Proposal: the "Nuclear Power Plant" proposal (draft 5) and "Nuclear, stage 1" design in the project notes.
Owner: Narvisius
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
  - A benchboard section three wide, the same height and profile as the control room's system boards, so it stands in
    line with them. Since round 30 it is a **live board** like them (`tools/board_live.py` `console_board`; Justin: the
    rods driven by holding the lever, "Overlap, shutdown on reset", "AUTO holds power", the NIS blocks "Working, no new
    trips"); it reads the reactor it is linked to:
    - **Annunciator:** three boxes. Reactor and rod control (reactor tripped, trip breakers open, rod at bottom,
      shutdown banks not fully out, protection bypassed, AUTO deviation, control at the remote panel, Tavg, core exit
      temperature, safety injection, turbine tripped, a loop out, no turbine connected); the nuclear instruments and the
      core (source range, intermediate range and power range low setpoint high flux while not blocked, power range
      positive and negative rate, startup rate high, channel deviation, quadrant tilt, circulating water, fuel failed or
      used up, core not loaded, refuelling, unit incomplete, containment radiation); and a white bypass and permissive
      box (P-4, P-6, P-7, P-8, P-9, P-10, P-13, the three blocks, source range high voltage off, rods in AUTO or MAN,
      shutdown banks fully out, protection normal).
    - **Nuclear instruments:** source range N31 and N32 (log, 1 to 10⁶ counts a second; they read nothing while blocked,
      their high voltage off) and the intermediate range N35 and N36 (log, 10⁻¹¹ to 10⁻³ amps), each with its startup
      rate meter (-1 to +5 decades a minute); the power range N41 to N44 (0 to 120%) and the reactor power dial; the
      NIS recorder (intermediate range and power range). The instruments follow the neutrons: up at most half a decade
      a second through the ranges on a startup; on a trip a prompt drop to about 6%, then a third of a decade a minute
      (about twenty minutes back to the source range). The four power range channels read the core's quadrants from
      the fuel loaded (a part-loaded or unevenly used-up core tilts: channel deviation and quadrant tilt alarms).
    - **Digital rod position indication:** a core map with its 53 rods (shutdown banks SA to SD, control banks A to D),
      each a column of twelve green lights at its bank's position (0 to 228 steps) with a red rod bottom light.
    - **Rod control:** the eight banks' step counters, the rod speed (steps a minute) and AUTO's power setpoint; the
      plant computer's reactor page (power and startup rate, rod control, the banks, the shutdown banks and the trip, the
      fuel or what is not right).
    - **The desk, every switch working:** MAN/AUTO; the **IN-HOLD-OUT lever** (hold use on its left side to drive the
      rods in, its right side to drive them out; they stop when it is let go; IN and OUT lights while they move); the
      **SR, IR and PR LO trip block** buttons (they block only above P-6, or P-10, and clear by themselves below it;
      pressed again they reset; blocking the source range turns its high voltage off); the **reactor protection** switch
      (NORMAL or BYPASS, the screen's protection switch); the two **reactor trip switches** under their guards (first
      click lifts the guard, the second turns it to TRIP, or with the reactor tripped to CLOSE: the trip breakers close,
      the trip resets, refused while the core is hot or safety injection is in); the annunciator's buttons; and the
      startup procedure lying open on the desk. While a remote shutdown panel has control only the trip switches work.
    - Clicking the console anywhere off its controls opens its screen, as before.
  - **Rods:** the control banks A to D move in their real overlap (bank B starts when A is 128 steps out, and so on;
    all four out is 100% withdrawn), at the game's rod speed (734 steps a minute). The shutdown banks fall in with a
    trip and come out by themselves within four seconds of the reset. In AUTO the rods step to hold the power the rods
    gave when AUTO was selected (in the game's model power moves only as assemblies are used up, so AUTO mostly steps
    then); a trip drops rod control to MAN. Rods set from the console's screen block the instruments' trips themselves
    as the power passes P-6 and P-10, so a plant run from its screen alone shows no alarms; nothing new trips the
    reactor.
  - Link it with a **Reactor Link Card**: use the card on the vessel, then on the console.
  - Its screen follows the approved design: core map (193 assemblies, shaded by heat, with rod clusters), rod slider,
    plant readouts, a 10-minute core-temperature trend with the alarm and SCRAM lines, annunciator windows, the
    protection switch, the SCRAM button, RESET TRIP, a Personal/Party switch, and the fuel line with REFUEL.
- **The live control boards** (round 27; `nuclear/ControlBoard.java`, `nuclear/BoardPlant.java`,
  `client/LiveBoards.java`, laid out by `tools/board_live.py`, the horn by `tools/board_sounds.py`). The Reactor
  Coolant, Turbine Generator and Electrical boards read the plant: each watches the nearest reactor (as the status
  screens do), the two nearest diesel generators (A and B by where they stand), the nearest 480 V load centre and the
  DC rooms' motor control centres, within 256 blocks.
  - **Annunciators:** six boxes of fifteen windows on each board, real Westinghouse legends (red for a trip's first
    out, amber for alarms, white for status lights such as P-8 and P-9 armed or the turbine on its turning gear).
    A window whose alarm comes in flashes and the horn sounds until ACKNOWLEDGE on the board's desk; it stays lit while
    the alarm lasts; once it clears it flashes slowly until RESET; TEST lights them all for three seconds. Most windows
    stay dark at full power, as on a real "dark board"; the ones the game works out light from the plant (loop flows,
    pressurizer pressure and level, Tavg-Tref deviation, core exit temperature, fuel failure, the turbine's trip and
    valves, steam dumps, the generator breaker, the diesels' start, day tank and sequencer, the load centre's and
    motor control centres' buses...).
  - **Instruments:** edgewise meters with their real scales, round meters, the turbine speed and synchroscope dials,
    seven-segment loop temperatures and generator readouts, controllers' LED bar graphs, strip chart recorders drawing
    the last five minutes, and plant computer screens (RCS, the turbine's DEH control, the generator). Values the game
    does not simulate are worked out from the plant (Tavg and Tref, pressurizer pressure and level, the loops' hot and
    cold leg temperatures from their flows, the generator's megavars and amps, the diesels' volts and load from their
    start sequence). Every meter wanders a little while live. The legends, scales and nameplates are on their own
    texture, `textures/entity/control_board_legends.png`, drawn at 512 px a block.
  - **Controls that work** (right-click the switch on the desk; the board follows your look onto its sloping desk):
    - RCP 1A to 1D stop and start. A stopped pump's loop flow coasts down over about ten seconds; above P-8 (48%) one
      loop below 90% flow trips the reactor, above P-7 (10%) two do. The other loops carry the heat.
    - TURBINE TRIP under its red guard: the first use lifts the guard (for five seconds), the second trips the turbine
      and opens the generator breaker. Above P-9 (50%) the reactor trips with it; below, the reactor runs on and the
      steam dumps take the heat. The turbine coasts down to rest (about two minutes); someone at the turbine puts it on
      its turning gear (turbine hall part 1), or a hot rotor standing still bows and ECCENTRICITY HI comes in. LATCH (on
      the DEH push buttons) relatches it once the reactor is reset, and it runs back up to 1,800 rpm in about half a
      minute, rolling off its turning gear.
    - GEN BKR opens the generator breaker (no output; the turbine holds its speed). Closing it arms the synchronizer:
      the breaker closes when the synchroscope comes round to 12 o'clock, with the turbine latched at speed and the
      reactor making power.
    - EDG A and B START and STOP run the real diesel generators, as their own panels do (they need fuel in their day
      tanks, and their MODE at AUTO with the 86G and emergency stop reset: round 31 part 2).
    - Every other switch's lamps show the truth (red running or open, green stopped or shut).
  - The console alone works as before: a reactor trip shows the turbine and generator tripped on the boards, and the
    output comes back when the reactor is reset and its rods withdrawn. Only a trip or breaker opening from the boards
    has to be undone from the boards.
- **The safety systems, steam and feedwater, and radiation boards** (round 28; the same code, laid out by
  `tools/board_live.py` `esf_board`, `secondary_board`, `radiation_board`; the legend texture is now 2048 x 4096).
  - **Engineered Safety Features:** each safeguards train (A orange, B blue) with its charging, safety injection and
    residual heat removal pumps' flows and pressures, a recorder, the RWST's level and its accumulators; a mimic of the
    pumps from the RWST through the boron injection tank and the RHR heat exchanger to the cold legs (every pump and
    valve lit as it stands); controllers, readouts, the containment's fan coolers. Between the trains the ESF status
    panel (48 windows, each lit when its equipment is where safety injection puts it) and the containment and coolant
    pressures. The pumps' flows follow the coolant's pressure: at 2,235 psig only the charging pumps inject (through
    the BIT), the SI pumps run on miniflow until it falls below 1,520 psig, the RHR pumps below 190.
  - **Manual SAFETY INJECTION** (two switches under red guards: the first click lifts the guard, the second actuates;
    either train's actuates both): the reactor trips (first out SAFETY INJECTION) and the turbine with it, both diesel
    generators start, the SI, RHR and second charging pumps run, the containment isolates (letdown and the
    mini-purge shut), main feedwater isolates (the main feed pumps trip), the auxiliary feed pumps start, and the
    control room HVAC goes to emergency on AUTO (a reactor trip does that already). SI RESET (on the desk, lit once
    possible) works a minute after: it clears the isolations and stands the SI and RHR pumps down; aux feed and the
    diesels run on until stopped. The reactor can't be reset at the console while SI is in.
  - **Steam Generators and Feedwater:** each generator's narrow and wide range levels, steam and feed flows, a
    recorder of its level and feed, its mimic (the ARV and MSIV on the steam line, the FRV and the aux feed valve on the
    feed line), controllers and readouts; the condensate storage tank, the three aux feed pumps and their header to
    every generator, the main feed pumps. The steam generators now hold water: each loop's heat boils it, main feed
    keeps it at 50% narrow range, and a generator below 15% of its water carries less heat (none dry).
    - **MSIV 1-4** close and open from the desk: a shut MSIV's steam goes out its relief valve instead of to the
      turbine (one shut at power: three quarters of the power); all four shut trip the turbine and, above P-9, the
      reactor.
    - **AFW PP A, B and TD** start and stop: A feeds generators 1 and 2, B 3 and 4, the turbine-driven pump all four
      (on steam, while the plant is hot), each throttled to hold the level. A generator at low-low level (11%) trips
      the reactor and starts the motor pumps (two at low-low start the turbine-driven one too).
    - **MFP A and B** (turbine hall part 2) start and stop the hall's main feed pumps where it has them (a tripped one
      is reset at its station first); MFP A and B TRIP, CONDENSATE HDR PRESS LO and COND PUMP AUTO START come in from
      the hall's pumps. With none at the hall the switches only say so and main feed runs by itself.
    - A plant run from its console alone never isolates its feed, so nothing here changes it.
  - **Radiation Monitoring and Ventilation:** twelve monitor drawers, each reading the game's radiation where it
    stands: the control room (in front of the board), the containment area (above the reactor's head) and high range
    (beside its core), the spent fuel pool (at the pool's surface over the nearest rack), the fuel handling machine,
    the auxiliary building (beside a heat exchanger outside the containment), the turbine building, the cask storage;
    in counts a minute the control room's intakes (the HVAC room's own monitors), the plant vent and the containment's
    air (with the release from damaged fuel, as the HVAC monitors read it). Readings in scientific figures, log
    meters, HIGH and ALERT lamps and windows at real setpoints (2.5 and 10 mR/h in the control room, 100 mR/h and
    1 R/h in the containment, 1,000 and 10,000 cpm at the intakes...); a monitor whose spot isn't there shows FAIL
    (a white light, no horn). The control room HVAC mimic shows its panel's dampers, filtration units, air handlers,
    chillers and pumps as they stand and its mode; the containment mini-purge and the buildings' exhausts to the vent.
- **The remote shutdown panel, live** (round 29; laid out again by `tools/board_live.py` `rsp_board`, a live board like
  the control room's: every meter, lamp, recorder and its 15-window annunciator read the plant). Its switches work
  where they stand on its face (the player's look meets the face; clicking anywhere else still opens its status
  screen, whose TRANSFER and TRIP buttons work as before):
  - **Control transfer keys:** any key turns control of the unit's transferable equipment over to the panel (amber
    lamps) or back (white). While the panel has it, the console and the boards refuse the aux feed pumps, the aux feed
    valves and the ARVs (and, as before, RCP starts, LATCH and closing the generator breaker); the trips, safety
    injection and the MSIVs still work from the control room.
  - **REACTOR TRIP A and B** under flip-up covers (first click lifts, second trips), whatever the transfer.
  - With control at the panel: **AFW PP A, TDAFW PP, AFW PP B**, the **flow control valves FCV 1-4** (shut, a
    generator gets no aux feed) and the **SG ARVs 1-4** (opened by hand, a generator steams to atmosphere: each open
    ARV cools the plant about 30 °C and boils its generator off faster, so the panel can cool the plant down). The
    steam and feedwater board's ARV and AUX FEED VALVE switches now work the same valves.
  - Indication: pressurizer pressure and level, a loop's hot and cold leg temperatures, the steam generators' levels,
    the boric acid tank, aux feed flow, the condensate storage tank, source range flux (log, counts a second) and
    startup rate, the generators' steam pressure on recorders; the unit status screen; control and trip lamps.
- **The remote shutdown panel** (round 24, as first built): a vertical switchboard six wide, for tripping the reactor and holding it
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
- **The 480 V load center** (Switchgear Lineup, round 25; rebuilt in real 3D in older rooms round 1:
  `tools/load_centre.py`, `nuclear/LoadCentre.java`, `client/LoadCentreParts.java`): eight blocks long, a Class 1E
  metal-enclosed lineup built as a real one: a black sill, steel cubicles bolted together, their posts, kick plates and
  top trim, lifting rails with the breaker trolley on top, gland plates over the feeder sections where its cables come
  down. On the left the station service transformer's section (4160 V to 480 V): louvred covers, its two cooling fans
  behind their guards, its temperature monitor (three windings and the fan, alarm and trip lamps) and nameplate. Next
  the main breaker's section with its power meter (volts, amps, kW, the bus's charge) and ammeter and voltmeter; then
  five feeder sections, each with two draw-out power circuit breakers. Each breaker's door has a cut-out onto the
  breaker itself (PUSH OPEN and PUSH CLOSE, the contacts and closing spring flags, the operations counter, the charging
  handle, the racking window and port, its rating plate and its trip unit: its display of the amps, pickup and trip
  LEDs, setting knobs, RESET and the trip target), and on the door its control switch (a pistol grip: TRIP, NORMAL,
  CLOSE), red, green and amber lamps, its engraved tag (52-1A-03 and so on) and the load it feeds: MCC 1 to 4, INVERTER,
  DC MCC, CR HVAC, LIGHTING and two spares (racked out, tagged). Warnings, arc flash labels and the lineup's nameplate
  are where a real one has them. The plant's five lineups are LC 1A and 1B (the control building's switchgear room)
  and LC 1C and 1D (the turbine building's switchgear room), and LC 1E (the pump house's electrical room).
  - Its bus holds **2,000,000 JE** and keeps it.
  - **In:** a cable into the back of its transformer section (either of the two left-hand blocks as you face it, any
    side) charges the bus while the **main breaker** is closed. It takes up to 16,384 JE/t.
  - **Out:** each closed, racked-in feeder breaker sends the bus's power out of the back of its own section, an upper
    breaker's out of the top block, a lower breaker's down through the floor; up to 4,096 JE/t each, but see the trip
    unit below.
  - **The control switch** (right-click it): turned to TRIP it opens its breaker; to CLOSE it closes it, if the breaker
    has control power (it is racked to CONNECT or TEST), its closing spring is charged (it charges again for four
    seconds after each close) and its trip unit's target is reset. Closed in TEST, a breaker carries nothing.
  - **Racking** (right-click its racking port): a breaker racks out a position at a time, CONNECT, TEST, DISCONNECT,
    and from DISCONNECT back in. A closed breaker cannot be racked (the interlock): open it first. The breaker's front
    moves back in its cubicle as it racks out.
  - **The trip unit:** every breaker's amps are its feeder's real power (1,800 A for 16,384 JE/t) plus the load the
    plant puts on it (worked out once a second: the motor control centres' pumps, fans and heaters by the reactor's
    power, the inverter's steady load, the control room HVAC train's machines that are running, the lighting, more at
    night). Above its 225 A pickup its amber pickup LED lights and its long delay counts toward a trip (about six seconds
    at twice its pickup, under three at three times); it trips the breaker open, drops its target and lights its trip
    lamp. It will not close again until **RESET** on its trip unit is pressed. Spares and racked-out breakers carry none.
  - **The transformer** warms with its load (its windings head for their temperature over about a minute): its fans
    run above 110 °C and stop below 90 °C (AUTO), or always with its fan switch at MANUAL (right-click it); it alarms at
    150 °C and lights its trip lamp at 180 °C. The bus volts sag a little under load.
  - Right-clicking anywhere else shows the bus's charge, the main breaker's state and what is coming in. Only the owner,
    or their party in Party mode, can work it.
  - In the medium plant, a buried cable from the main transformer comes up through the floor under each of the
    switchgear room's load centres. Each one's **DC MCC** breaker (the lower breaker of its third section) feeds its
    train's motor control centre in the DC equipment room by a cable in the floor slab (round 26). A plant built with
    `fuelled` starts with every load center, motor control center and station battery full.
- **The 480 V motor control center** (480 V Motor Control Center, round 26): four sections, four blocks long, one deep
  and three high, standing in a DC equipment room. Each section has a top and a bottom wireway cover and a column of
  plug-in units, with a tall narrow vertical wireway door at its right.
  - The first section is the incoming section: the main breaker's unit (its trip unit window and rating plug), the
    metering unit (the bus's charge and volts, live, and the phase selector) and the incoming cables' bolted cover.
  - Then the feeders: REG XF 1 and REG XF 2 (the inverters' regulating transformers), a SPARE, CHGR 1 and CHGR 2 (the
    battery chargers), and two combination starters, EXH FAN and DC AHU, with START and STOP buttons, a HAND-OFF-AUTO
    switch and an overload reset.
  - Every breaker's unit door has its operating handle in a slotted escutcheon (up ON, down OFF), a red and a green
    pilot lamp, an engraved nameplate and quarter-turn fasteners. The handles and lamps are live.
  - Its bus holds **500,000 JE**. **In:** a cable under its first section charges the bus while the main breaker is
    closed (up to 4,096 JE/t). **Out:** each closed feeder sends up to 1,024 JE/t out of its own terminal: CHGR 1 down
    through the floor under the third section, CHGR 2 out of the back of the fourth section's bottom block (the others
    out of their sections' tops or backs). **Right-click a unit's door** to switch its breaker (the owner, or their
    party in Party mode); anywhere else shows the bus's charge, the main breaker's state and what is coming in.
  - In the medium plant each DC equipment room has one, fed from its train's load centre's DC MCC breaker. Its CHGR 1
    and CHGR 2 breakers each feed one of the room's two battery chargers by its own cable in the floor (in Room 1B,
    CHGR 2's cable is buried a level deeper where it passes under the others).
- **Station Battery** (rebuilt in round 25b): a two-step seismic rack four blocks long holding 30 flooded lead-acid
  cells, every part modelled. Each cell is a clear jar on its foot, and you can see in: the plates standing in the
  acid (brown positives, grey negatives) on the sediment space, the acid's level with its MAX and MIN marks, the
  lead bus bars and the posts rising from them. On top are the black cover, a flame-arrester vent, the posts with
  red and black collars and bolts, and bolted intercell links; rubber spacers sit between the jars. Restraint rails
  run along the front and back of both steps, and the engraved nameplate reads 125 VDC STATION BATTERY, 60 CELLS -
  NO SMOKING. Every cell has the battery monitor's sensor clipped to its positive post (a small black module with a
  green light and its lead to the post), daisy-chained along the front of the covers on each step; at the rack's right
  end both chains go back to the cell sense junction box on the wall behind it, and its blue-grey sense cable goes up
  the riser beside the power cables and along the DC tray to the Ground Detector & Battery Monitor (round 26).
- **Station battery labels and cells** (older rooms round 4; Justin: "Battery rooms"; `tools/battery_rooms.py`): a
  rack is labelled by its battery, string and cells: 1A STR 1 CELLS 1-30, 1A STR 1 CELLS 31-60, 1A STR 2 CELLS 1-30
  and so on to 1B STR 2 CELLS 31-60. Its nameplate says so (125 VDC STATION BATTERY 1A STRING 1, CELLS 1-30 OF 60, NO
  SMOKING), every cell has its number on a white label on the cover's front (from the right along the upper step,
  down the jumper at its left end and back along the lower step, the way the current runs), and the eighth cell of each rack is its
  pilot cell, with a yellow PILOT CELL tag and a thermometer standing in it. Red and black boots cover the end
  terminals. The spill pan under the rack reads ACID SPILL CONTAINMENT - KEEP ABSORBENT IN PLACE and DANGER 125 VDC -
  SHOCK AND ARC FLASH on its lip, with absorbent pillows along it. Right-click a cell to read it: its number, its
  battery and string, its volts (its share of the bus while its string is on it, about 2.20 V floating; its own
  open-circuit volts off the bus, 1.95 V flat to 2.15 V full), its specific gravity (1.100 flat to 1.215 full) and
  its temperature (25 °C, warming a little on a hard charge); no two cells read quite alike. Right-click anywhere
  else for the rack's charge.
- **Battery Disconnect Switch** (older rooms round 4): one for each string, labelled 1A-1, 1A-2, 1B-1, 1B-2, standing
  on its strut frame beside the racks with its conduit pair going up into the DC tray (a **Battery Disconnect
  Conduit**, two blocks, over it). A grey fused switch two blocks high: its engraved nameplate, the window onto its two
  fuses, its ratings (125 VDC, 400 A, 2 pole, fused), a red ON lamp and green OFF lamp, the red handle (up ON, turned
  left OFF), DANGER and arc flash labels, its hinges. Right-click the handle to switch it: OFF takes that string's
  racks off the switchboard's bus (the other string carries the load and takes the charge; its racks' cells read
  open-circuit), ON puts them back. With both open the battery is off the bus as if the main were open. The laptop's
  battery line reads STR 1 OPEN (or 2), and the switchboard's battery meter and the bus volts follow only the string
  on the bus. Right-click anywhere else to see how it stands and the bus volts. It links its train's switchboard in
  reach; the string it switches is by its label (-1 string 1, -2 string 2).
- **Station batteries hold charge** (round 25): a Station Battery rack holds 1,500,000 JE. Any cable touching it can
  charge it or draw on it (4,096 JE/t). Right-click it to see its charge and whether it is charging, discharging or
  on float. It does not tick (25 October 2026: the battery rooms only look live; Justin chose "The plant's batteries
  still store and give power when cabled, just without live calculating"): only while energy moves through it does
  it take its rate over the last second and send it with its charge, at most once a second, and two seconds after
  energy stops moving its rate reads zero.
  - **Strings in parallel** (round 26): a DC switchboard has every rack in reach that is nearer to it than to any
    other switchboard on its bus, and keeps their charge even, each second moving charge from the fullest rack to the
    emptiest (up to 81,920 JE), so a charger feeding one rack charges them all. Its BATT AMPS meter reads all of
    them together. Racks standing in one line, end to end, are one string; each line is another in parallel.
  - A **Battery Charger** or **Station Battery Charger** takes AC from a cable into any block of it (it keeps a
    40,000 JE buffer). It charges the nearest station battery within 24 blocks that no other charger has, at up to
    1,024 JE/t (its 150 A limit).
  - Away from a diesel generator, the chargers' and the DC switchboard's meters read their battery:
    - Charging: up to 150 A, and the volts rise towards 135 V (equalize).
    - Full: float at 132 V.
    - With no AC: the AC lamp goes out, CHARGER FAIL comes in, and the volts fall with the charge.
  - **Station Battery Charger** (round 26): the DC equipment rooms' own charger, one block wide and two high, every
    part that stands out of its door modelled: hinges and a locking T-handle, the engraved nameplate, DC VOLTS and DC
    AMPS meters in black cases with the scale behind glass, four pilot lamps in chrome bezels, a six-window alarm
    annunciator with TEST and RESET buttons, the FLOAT/EQUAL mode switch, the equalize timer, the float and equalize
    voltage adjusters, the AC input and DC output breakers through their door cut-outs, the louvred air inlet's
    slats and the exhaust hood on top. Everything on it works (Justin: "any meters and such are actually animated to
    work realistically"): the needles read the volts and amps, the AC ON, DC ON, FLOAT and EQUALIZE lamps light, the
    timer's pointer shows the hours of equalize left, and the windows come in for AC FAIL and CHGR FAIL (no supply),
    DC LOW (the battery run down), DC HIGH and EQUALIZING (GND FAULT stays dark: no ground faults are modelled).
  - Its switches work (older rooms round 3; Justin: "Charger switches", "Annunciator buttons"; `nuclear/DcRoom.java`,
    `StationChargerBlock.java`). Use the **AC INPUT** or **DC OUTPUT** breaker (its handle up ON, down OFF) to switch
    it: with the AC breaker open nothing comes in from its supply (AC FAIL); with the DC breaker open it puts nothing
    out (CHGR FAIL) and charges nothing. Use the **MODE** knob to turn it between FLOAT and EQUALIZE: equalizing holds
    the bus at 139.8 V and runs the EQ TIMER down from 24 game hours (a game day), then it goes back to FLOAT by
    itself. GND FAULT comes in while the room's ground detector has a test on the bus. Its output amps are its share
    of the bus's load and what it charges the battery with.
  - Its annunciator latches (Justin: "Lit until RESET"): a new alarm flashes (with a chime) until **ACK**, then stays
    lit; once its condition has cleared it stays lit until **RESET**; **TEST** lights every window for three seconds.
    Use anywhere else on it to hear how it stands.
- **Vital AC Inverter** (rebuilt in round 26 to match the Station Battery Charger): the station battery's 125 VDC
  made into 120 VAC for the vital instrument buses, one block wide and two high. Its door: DC VOLTS, AC VOLTS and AC
  AMPS meters behind glass, the output frequency in green digits, a mimic of its power path (DC in, the inverter, the
  static switch and the load; the bypass into the static switch), DC IN, INVERTER, ON INV and BYPASS lamps, a
  six-window annunciator with ALARM RESET, the manual bypass switch (at NORMAL), the DC input breaker, the louvred
  inlet, the exhaust hood, hinges and a T-handle. All of it is live, with a real meter's small wander:
  - On its battery's DC it runs and carries the load: 120 V, about 46 A, 60.00 Hz; the mimic's DC, inverter and load
    lines lit green.
  - With the battery flat its static switch moves the load onto the bypass, the Regulating Transformer beside it (the
    nearest in reach): the transformer's 119 V or so on the grid's frequency, BYPASS and ON BYPASS lit, INV FAIL and
    LOW DC in, the load line amber.
  - With the transformer dead too (its MCC breaker open or the MCC empty), the output is dead: the meters drop to zero
    and the display goes dark. BYP SRC FAIL comes in whenever the bypass is gone, and OUT OF SYNC while the inverter
    runs with no bypass to follow.
  - It only reads its battery; it draws nothing from it.
  - Its switches work (older rooms round 3; Justin: "Inverter bypass switch"; `VitalInverterBlock.java`): its DC comes
    from its switchboard's feeder 01 (the -1 inverters) or 02 (the -2) through its own **DC INPUT** breaker; open
    either and it loses its DC (its static switch puts the load on the bypass). The **manual bypass switch** turns
    between NORMAL and BYPASS: on BYPASS the load is on the Regulating Transformer whatever the inverter does (with the
    transformer dead, the vital panel loses its power). Its annunciator latches and has ACK, RESET and TEST as the
    charger's. (One with no switchboard in reach reads its battery, as before.)
- **Regulating Transformer** (round 26): each Vital AC Inverter's standby source, a 10 kVA constant-voltage
  transformer, 480 V in from a motor control centre's REG XF breaker and a steady 120 V out to the inverter's static
  switch. One block wide and two high, it stands beside its inverter in the same cabinet but a transformer's: a bolted
  front cover with two lifting handles and its bolts, a big louvred air inlet low down, louvred vents in both sides and
  on the top, the hub its feeder conduit comes down into. Its cover: the engraved nameplate; INPUT VOLTS, OUTPUT VOLTS
  and OUTPUT AMPS meters behind glass; the WINDING TEMP gauge (a red band from 130 °C); the POWER ON lamp; a FED FROM
  tag (MCC 1A, REG XF 1 and so on); a DANGER 480 VOLTS sign and a riveted data plate. It links the nearest motor
  control centre in reach and the REG XF breaker its label says (1A-1 and 1B-1 REG XF 1, 1A-2 and 1B-2 REG XF 2; one
  put down by hand is 1A-1), and the nearest Vital AC Inverter. It only reads the breaker; it draws nothing.
  - Breaker closed and the MCC's bus charged: POWER ON lit, about 480 V in (with a slow wander), a very steady
    120 V out, 0 A while its inverter runs on its battery.
  - Its inverter on bypass: the output amps read the inverter's load (about 46 A, the same as the inverter's AC AMPS)
    and the output sags a little, to about 119.3 V.
  - Breaker open or the MCC empty: every meter at zero, the lamp out, and its inverter has no bypass.
  - The winding temperature follows slowly (two thirds of the way in about a minute and a half, settled in a few
    minutes): room temperature (25 °C) dead, about 58 °C alive on no load, 96 °C carrying the load.
- **120 VAC Vital Instrument Panel** (round 26): each Vital AC Inverter's distribution panelboard, hung on a wall,
  one block wide and two high, labelled like the inverters (1A-1 and so on). Closed: the engraved nameplate and the
  POWER ON lamp over the door (lit while its inverter's output is alive, on its battery or its bypass, and its main
  is closed), the hinged door with its latch and lock, the circuit directory in a clear holder and a WARNING 120 VAC
  label. Use it to open the door (it swings out on its hinges): the two-pole main breaker at the top and twelve branch
  breakers in two columns, odd on the left and even on the right, each with its number, short name and rating beside
  it; handles toward the middle are ON, outward OFF (the main up ON). Use a breaker (or the name beside it) to switch
  it; use anywhere else to close the door. Its circuits are its channel's instruments (NIS power range, process
  protection rack, SSPS input, steam generator level, pressurizer pressure and level, RCS flow, steam generator
  pressure, RVLIS, PAM, AFW flow) and two spares (open). Each closed circuit carries its share of the load (46 A with
  all ten): the inverter's AC AMPS, and its Regulating Transformer's OUTPUT AMPS while it is on bypass, read the closed
  breakers' sum; open the main and the load is gone. It links the nearest inverter of its label in reach (the inverter
  links it back); one put down by hand is 1A-1.
- **125 VDC Distribution Switchboard** (rebuilt in older rooms round 3; Justin: "Real 3D, breakers work";
  `tools/dc_room.py`, `nuclear/DcSwitchboardBlock.java`, `DcRoom.java`): two sections, labelled 1A or 1B. The first's
  door: BUS VOLTS and BATTERY AMPS meters in black cases, their faces behind glass; the ground detector's + BUS GND,
  BATT DISCH and - BUS GND lamps in chrome bezels; the main breaker 72-1A through its door's cut-out (its moulded
  front, trip unit settings and PUSH TO TRIP button), DANGER and arc flash labels, hinges and a T-handle. The
  second's dead front: twelve feeder breakers, each with its engraved tag (72-1A-01 INVERTER 1A-1 ... 12 SPARE), and
  the louvred inlet. Every handle is the game's own (up ON, down OFF): use one to switch its breaker.
  - The main puts the battery on the bus; the chargers whose breakers are closed and whose supply is on feed it. The
    bus is alive while either does: floating 132 V, equalizing 139.8 V, a little lower while the battery takes a
    recharge, on the battery alone 105 V flat to 129 V full. The chargers charge the battery only through a closed
    main. BATTERY AMPS reads the battery's charge, or with no charger feeding the load it carries (BATT DISCH lit).
  - Each feeder cuts what it feeds (Justin: "Cut what it feeds"): 01 and 02 the vital inverters (onto their bypass),
    03 and 04 the DC distribution panels, 05 its train's 480 V load centre's control power (its breakers can't be
    worked from their switches and its lamps go dark), 07 its train's diesel generator's control power (it won't
    start); the rest show on the laptop. A load centre or diesel with no switchboard of its train within 256 blocks
    keeps its control power.
- **125 VDC Distribution Panel** (older rooms round 3; Justin: "DC distribution panels"; `DcPanelBlock.java`): built
  as the 120 VAC vital panels (the same enclosure, door and breakers), labelled 1A-1 to 1B-2, on each DC room's side
  wall where the relay cabinets stood. Fed from its switchboard's feeder 03 (-1) or 04 (-2): POWER ON while that
  feeder is alive and its main closed. Its circuits feed the train's DC control loads (control room boards, reactor
  trip switchgear, SSPS logic, relay room cabinets, remote shutdown panel, ESF sequencer, annunciator, HVAC control,
  fire detection on -1; AFW, MSIV, PORV block, CCW, service water and containment isolation valves, the diesel's field
  flash, turbine building emergency lighting, security and plant page on -2).
- **DC Room Log Desk (Laptop)** (older rooms round 3; Justin: "Log desk laptop"): the switchgear room's desk with the
  DC system monitor on its laptop, reading its nearest switchboard live. Page 1: the bus's volts; a line each for the
  battery (FLOAT, RECHARGE, DISCH or ISOLATED, its charge and amps), the two chargers (FLOAT, EQUALIZE, AC FAIL, AC
  OPEN, DC OPEN; their amps), the two inverters (ON INV, ON BYPASS, MAN BYPASS, NO OUTPUT; their load), the main,
  the feeders closed and open, the two distribution panels (LIVE, DEAD, MAIN OPEN; their load); the ground
  detector's test and the alarm windows lit at its foot. Pages 2 and 3: every feeder, closed, open or with no
  volts, and its amps. Use the laptop to step its page.
- **The DC rooms' fittings** (older rooms round 3; Justin: "Lights and safety", "One-line and signs", "Desk, phone,
  drawings", "Mats and work gear"): the framed **125 VDC One-Line Diagram** (three blocks by two: both trains'
  batteries, chargers, switchboards, feeders, inverters and panels), the **125 VDC Train A** and **Train B** signs
  and the **125 VDC Danger Sign**, the **Battery Service Board** (two blocks: a pegboard with the hydrometer,
  thermometer, insulated gloves, wrist strap, face shield and apron, the shelf with the neutralizer tub and the
  distilled water jug) and the **Battery Test Cart** (the load bank, the clamp meter, the cable reels).
- **DC Ground Detector & Battery Monitor** (round 26): one for each DC bus, labelled 1A or 1B, a cabinet like the
  chargers' standing at the end of the equipment row. It links the nearest station battery in reach, the one its
  switchboard is on (one put down by hand is 1A), and only reads it. Its door, top to bottom:
  - The engraved nameplate.
  - The battery monitor's screen (green on black): the string's VOLTS and AMPS (in positive, out negative), CHARGE %,
    TEMP °C and what the battery is doing (FLOAT, RECHARGE, EQUALIZE, DISCHARGE, OPEN CKT), then a bar for each of
    its 60 cells (two rows of 30) on a 2.00 to 2.35 V scale with the 2.20 V float level dashed behind them. A healthy
    battery's cells sit round the float level with a small spread; a cell below 2.10 V turns amber. With two strings
    in parallel it shows one at a time (STR 1/2: its own amps and charge), and right-clicking MENU steps to the next.
    MENU and ALARM ACK keys, and the ALARM lamp (it flashes with a ground on the bus or the bus under 124 V).
  - The ground detector: + BUS TO GROUND and - BUS TO GROUND voltmeters (0 to 150 V), each reading half the bus while
    neither side is grounded (no ground faults are modelled: the bus is healthy); + GND and - GND lamps (amber, that
    side grounded) and DC ON (white).
  - The + BUS and - BUS TEST buttons. Hold right-click on one to put a ground on that side through the test resistor
    for as long as you hold it: that side's meter drops to about 2 V and the other reads the rest of the bus, its GND
    lamp lights, the screen reads + GND FAULT or - GND FAULT, ALARM flashes and the alarm beeps. Let go and it clears.
    Use it anywhere else for the bus and the battery in a line.
  - The louvred inlet, hinges and the T-handle; its conduit goes up from its top into the tray.
- **The battery rooms' fittings** (round 26), each labelled by train where it has one:
  - **Hydrogen Monitor**: a wall transmitter two blocks high, the display at eye level and its conduit up the wall
    to the **Hydrogen Sensor Head** near the ceiling (hydrogen rises). It reads the hydrogen in the room's air in % of
    its lower explosive limit (red digits) with NORMAL, WARN (10 %LEL) and ALARM (25 %LEL) lamps. A floating battery
    gives off a little and a battery charging hard gives off more with its current; the room's exhaust fan carries it
    away, more on high speed, and with the fan off the reading creeps up. The reading follows slowly (about half a
    minute). It keeps to its battery and the nearest exhaust fan (worked out where it is drawn: the battery rooms'
    monitor, fan and string disconnects only look live and do not tick on the server, 25 October 2026).
  - **Battery Room Exhaust Fan**: a wall fan two blocks high, its upper block over the room's hydrogen exhaust louvre:
    the housing and round shroud, the motor on its cross bars, four blades that turn (live) behind a wire guard, and
    below it its HAND-OFF-AUTO station with RUN and HIGH lamps. Right-click the selector to turn it (HAND, OFF, AUTO).
    AUTO runs it slowly while the battery floats and fast while it charges hard (over 20 A); HAND runs it fast; OFF
    stops it. The blades spin up and run down over a second or two.
  - **Emergency Eyewash Station**: a plumbed wall eyewash, its stainless bowl with two spray heads under yellow dust
    covers, fed from the wall through a valve with a green PUSH flag, the drain back into the wall. Right-click it and it
    runs for 5 seconds: water jets up out of both heads and splashes in the bowl.
  - **Acid Spill Kit**: a yellow wall cabinet (neutralizer, absorbent pillows, gloves, apron, goggles).
  - Signs: **No Smoking Sign** (no smoking, no open flames, no sparks), **Battery Acid Danger Sign**, **Hydrogen
    Warning Sign** and **Emergency Eyewash Sign**, each a plate on four stand-off screws.
  - Older rooms round 4 (Justin: "Battery rooms"): the ceiling lights are **Vapor-Tight Lights** (an enclosed,
    gasketed 4 ft fixture: the fibreglass housing, the frosted lens, stainless clips along both sides, the conduit hub), an **Emergency Light** over the racks
    and a **Smoke Detector**; the **Battery Room PPE Cabinet** by the door (a yellow wall cabinet with a clear door: the
    face shield, the acid apron, the rubber gloves, the goggles), the room's **Station Battery 1A** or **1B Sign** over it (its
    train's band, 125 VDC, 2 strings of 60 cells, hydrogen - no open flames) and the **Battery Log Board** (the monthly
    cell log on a clipboard, the pen on its cord); the **Acid Neutralization Sump** (its cover in the floor: the
    hazard-striped rim, the grating, NO DRAIN TO SEWER) by the eyewash and a floor drain set in the epoxy floor.
  - **DC Cable Tray (Bend)** and **(Tee)**, and the riser and its top **(With Sense Cable)**: a second string's run
    turning a corner and joining the first's on its way to the switchboard.
- **The relay room's cabinets** (round 26, #10), one block wide and two high, each labelled (sneak and right-click one
  with an empty hand to step it to its next label; right-click it for its name and what it reads). All but the relay
  panels read the nearest reactor within 512 blocks once a second:
  - **Process Protection Rack**: the 7300 process protection, four channels of three racks (CH I RACK 1 to CH IV RACK
    3), the band across the top in the channel's colour (I red, II white, III blue, IV yellow). On its test panel two
    switchboard meters (rack 1 Tavg and delta T, rack 2 pressurizer pressure and level, rack 3 steam generator level
    and reactor coolant flow), four bistable lamps (lit tripped: overtemperature and overpower delta T, Tavg low-low,
    high flux; pressurizer pressure low and high, level high, pressure low SI; steam generator level low-low and
    high-high, flow low, RCP underfrequency), POWER and the channel test switch; behind the glass two card cages of
    printed circuit cards, each with its ejector and status light. At full power Tavg reads 572 F and the pressurizer
    2235 psig; cold, Tavg low-low and pressure low trip.
  - **Solid State Protection System Cabinet**: trains A and B, each an INPUT, LOGIC and OUTPUT bay, a 4 by 4 block of
    legend windows on each (lit amber: the input relays by channel; the permissives P-4 to P-13 and the reactor trip,
    safety injection, feedwater and steam line isolation, containment isolation and spray, auxiliary feed start and
    turbine trip; the master and slave relays K601 to K616), INPUT ERROR / GENERAL WARNING (protection off) / OUTPUT
    TEST and POWER ON lamps, the logic test selector, its cards behind the glass.
  - **Nuclear Instrumentation Drawer Rack**: N31 N32 source range (counts a second on a log scale; its high voltage off
    and BLOCKED above P-10), N35 N36 intermediate range (amps, 1E-11 to 1E-3), N41 N42 and N43 N44 power range (0 to
    120 %), NC41 NM41 comparator and startup rate (decades a minute): drawers with their meters, lamps, bypass switches
    and handles, the high voltage supply and cooling fan panel below.
  - **Protective Relay Panel**: the relay cabinet (below) whole, its nameplate lettered with its name (29 of them: the
    RCP motors, main generator and transformers, 4160 V buses, diesels, load centres, switchyard breakers, isolations
    and the pumps' motors).
- **The relay room's pieces** (round 26, #10): **Control Cable Tray** (the ladder tray full of control cables, as a
  straight length with or without a hanger, a **Cable Drop** over a cabinet (a drop-out fitting under the rungs, the
  cabinet's tied bundle of seven cables down out of it), a **Cross**, a **Tee** and a **Fire-Stopped Wall
  Penetration** (the wall's own blocks round a steel-lined opening sealed with fire-stop mortar)); **Control Cable
  Bundle** and **(Cabinet Entry)** (the bundle into a gland on the cabinet's top); the **Relay Room Row A to D Signs**;
  the **Smoke Detector**; the **Drawing Stick File** (the room's prints hanging from it); the **Sequence of Events
  Recorder Printer** on its stand with its fanfold paper.
- **The cable spreading room's pieces** (punch list #15; `tools/spreading_room.py`): **Cable Tray (Power)**,
  **(Control)** and **(Instrument)** (24-inch ladder tray, the power tray's five big three-conductor cables, the control
  tray's layer of small cables with a few over it, the instrument tray's thinner layer of shielded pairs), each also
  as **(Bundle Out)** (a tied bundle leaving over its south rail) and the power tray as **(Bundle Up)**; **Cable
  Bundle**, **(Elbow Up)** and **(Slab Sleeve)** (into a fire-stopped sleeve with its escutcheon on the slab); **Cable
  Tray Riser**, **(Floor Sleeve)** and **(Top)** (up a wall out of the floor, over into a tray); **Cable Tray Hanger
  Rods**; **Cable Tray Train Separation** (blind plates on the runs' ends and the fire-rated barrier board between
  areas); **Cable Tray (Control, Tee)** (the relay room's header). All of these take their safety train: Train A
  orange, Train B blue, non-safety grey (the jackets, the stripe along the rails, the tags 1A-P, 1B-C, NS-I...); the
  trays also come with a trapeze hanger, a paint band and their tag. Put one down beside another and it takes its
  train; right-click it with an empty hand to step through them. **Fire-Wrapped Raceway**, **(Start)**, **(Up)**,
  **(Tee Up)**, **Fire-Wrapped Rise** and **(Slab Sleeve)** (a tray in a one-hour fire barrier blanket with its
  stainless bands). **Sprinkler Branch Line**, **(Pendent Head)** and **(End Head)**, **Sprinkler Cross Main**,
  **(Cross)** and **(End)**, **Sprinkler Riser** and **(Top)**; the **Heat Detector**; the **Train A**, **Train B** and
  **Non-Safety Cable Area Signs** and the **Pre-Action Sprinkler System Sign**; **Carpet Floor** and **Access Floor
  (Bare Slab Under)** (the control room's floor over the room, its underside the painted slab).
- **The pre-action sprinklers** (punch list #15; `nuclear/SpreadingRoomFire.java`; Justin: "Working sprinklers",
  "Alarm and put it out"): the **Pre-Action Releasing Panel** (on a wall, live: AC POWER, ALARM, RELEASED,
  SUPERVISORY, TROUBLE and LOW AIR lamps and a two-line display; its ACK, SILENCE, RESET and DRILL keys work), the
  **Pre-Action Sprinkler Valve Station** (on its riser, live: its water supply and system air gauges) and the **Fire
  Alarm Pull Station**. The panel watches the smoke and heat detectors within 24 blocks: a fire within 4 blocks across
  and 7 below one puts it in alarm (its horns; FIRE CABLE SPREADING on the radiation and ventilation board) and after
  10 seconds it releases: the valve trips (the air to 0, water into the pipes). Every pendent or end head within 4
  blocks across of a fire and above it opens and sprays, putting out the fires under it (FIRE PUMP RUNNING on the
  board while the water flows); 20 seconds after the last fire is out the water is shut off and the panel waits for
  RESET, which brings the air back up and re-arms it. The pull station releases at once; DRILL lights everything and
  sounds the horns for 10 seconds without water; SILENCE quiets the horns; ACK stops the alarm lamp flashing. With no
  valve station in reach it alarms but has no water (SUPERVISORY; FIRE DETECTION TROUBLE on the board with no
  detectors or heads).
- **The control room HVAC** (round 26, #11; `nuclear/HvacRoom.java`, `tools/hvac_boards.py`, `tools/hvac_room.py`). Its
  HVAC control panel runs the room it stands in (32 blocks either way, 3 below and 10 above): once a second it reads
  the nearest reactor and its radiation monitors, then opens and shuts its dampers and starts and stops its machines.
  - **Normal**: train A runs (its air handler, chiller and chilled water pump, its normal outside air damper CR-D-1A
    open); train B stands by with its dampers shut; the filtration units are stopped.
  - **Emergency**: both trains go to filtered air. The normal outside air dampers shut; the emergency intake dampers
    (CR-D-2A, 2B) and the filtration units' outlet dampers (CR-D-3A, 3B) open; both filtration units' booster fans
    start and every air handler, chiller and pump runs. The control room is held at 1/8 inch of water.
  - The panel's **mode selector** (right-click it to turn it right, sneaking to turn it back): NORMAL, AUTO or
    EMERGENCY. On AUTO it goes to emergency on a reactor trip, the reactor's high temperature (350 degrees and up),
    damaged fuel, or HIGH radiation at either intake, and back when that clears (a trip once the reactor is reset).
    Right-click anywhere else on it for the room's state.
  - The panel is live: the mimic of both trains (each damper lit red open or green shut, each fan, chiller and pump red
    running or green stopped, dark where there is none), the NORMAL, EMERG and PRESS OK lamps, the control room's
    differential pressure, temperature, chilled water supply and filtered air flow meters (following what runs), and
    the annunciator (REACTOR TRIP, RX HIGH TEMP, INTAKE HIGH RAD, TOXIC GAS, EMERG MODE, CREFS LOW FLOW, CHILLER TRIP,
    CR LOW DIFF PRESS; a window flashes when it comes in, then stays lit).
  - **Control Room Emergency Filtration Unit** (3 wide, 7 deep, 3 tall): moisture separator, heater, prefilter, HEPA,
    charcoal adsorber and post HEPA in a row with their access doors, differential pressure gauges and test ports, the
    booster fan blowing up out of its far end. **Control Room Air Handling Unit** (3 x 7 x 3): mixing box with its
    return and outside air dampers, filters, chilled water coil (its risers and pressure gauges), supply fan, the
    discharge out of its far end. **Control Room Water Chiller** (3 x 5 x 3): evaporator and condenser shells, the
    compressor on top, its control panel with a live display (its tag, RUNNING or STANDBY, leaving and entering chilled
    water, load and amps), keypad and emergency stop. **Chilled Water Pump** (1 x 3 x 2): the end-suction pump and
    motor on their pad with suction and discharge gauges. The filtration unit, air handler and pump each have a local
    station with RUNNING (red) and STOPPED (green) lamps; every gauge reads as its machine runs, coming up and down
    with it. Labelled by train (CRF-1A, AHU-1B, CH-1A,
    CHP-1B): right-click one for its tag and state, sneak and right-click it with an empty hand for its other train's.
  - **HVAC Isolation Damper**: a section of duct with its blades, actuator, position plates (the pointer to OPEN or
    SHUT) and its tag hanging under it (CR-D-1A to CR-D-3B). Right-click it for its tag, duty and position; sneak and
    right-click it with an empty hand to step its tag.
  - **Air Intake Radiation Monitor** (A and B): reads the dose rate at itself (a GM detector: 40 cpm background, 3,300
    cpm a mR/h) and the plume from a reactor's damaged fuel, on a log meter and a count rate display; NORMAL, ALERT
    (1,000 cpm) and HIGH (10,000 cpm: the HVAC goes to emergency on AUTO). **Air Intake Toxic Gas Analyzer** (A and
    B): chlorine and ammonia (clean air: no toxic gas is modelled). **Refrigerant Monitor**: R-134a at each chiller
    (a few ppm: no leaks are modelled). Sneak and right-click a monitor with an empty hand for its other train's label.
  - The pieces: **HVAC Duct** (straight, with a trapeze hanger, riser, elbows, tees, the labelled lengths with flow
    arrows: OUTSIDE AIR, FILTERED AIR, CR SUPPLY AIR, CR RETURN AIR), **HVAC Duct Floor Sleeve**, **HVAC Duct Riser (Fire
    Damper)**, **Ceiling Supply Diffuser**, **Ceiling Return Grille**, the **Air Intake Plenum** (joining side by side:
    base, outlet, access doors, monitor sample point, top and top outlet), **Chilled Water Pipe** (insulated, with
    hangers and trapezes, riser, elbows, tee, tap, CHWS and CHWR labels), **Chilled Water Expansion Tank**, **Chemical
    Feed Pot**, **Refrigerant Sensor**, **Small Pipe** and the **Spare Filter Rack** (HEPA cells, charcoal trays,
    prefilters in their cartons).
- **The Technical Support Center** (round 26, #12; `nuclear/TscRoom.java`, `nuclear/TscPanelBlock.java`,
  `tools/tsc_room.py`). Its **Emergency Classification Board** is the room's hub: once a second it reads the plant (the
  nearest reactor; the control room HVAC panel and its intake radiation monitors within 48 blocks; the TSC's area
  monitor; the emergency diesel generators, 480 V load centres, motor control centres and main transformer within 192
  blocks), works out the emergency class and keeps the event's clocks and log. Every other TSC panel links to its
  nearest board within 24 blocks.
  - **The class** (a simplified set of emergency action levels): UNUSUAL EVENT on a reactor trip or the CR HVAC in
    emergency; ALERT on the core at its high temperature alarm (350 degrees), HIGH radiation at a CR intake or the
    TSC's area monitor at its alert (2.5 mR/hr); SITE AREA EMERGENCY on damaged fuel; GENERAL EMERGENCY on damaged fuel
    with the core at its damage temperature or 100 mR/hr in the TSC. The class only goes up while the event lasts; once
    every condition has been clear for five minutes the emergency is terminated. The TSC is activated at an ALERT.
    Times are the game's clock (the day starts at 06:00, an hour a 1,000 ticks): the state and local notifications are
    due 15 minutes after the declaration, the NRC's an hour, a briefing every half hour.
  - The board lights the class (NO EMERGENCY to GENERAL EMERGENCY) with the local time, the event clock and when it
    was declared. The **TSC Video Wall Displays** (three, each two blocks square): the safety parameter display (the
    six critical safety functions green or red, the reactor's parameters, REACTOR AT POWER or TRIPPED), radiological
    and met data (the intakes, CR HVAC mode, the TSC's dose rate, the plant vent, the wind and the downwind sector, the
    release), electrical and events (the main generator, main transformer, both load centres and both diesels, the
    last six entries of the log, the class).
  - **TSC Consoles** (call-centre desks three blocks wide, two positions each with two live monitors, keyboards,
    phones, headsets, binders, logs, the position names on plates): Ops Coordinator and Recorder, Rad Coordinator and
    Dose Assessment, Engineering Coordinator and Reactor Engineer, Electrical and Mechanical Engineers, Maintenance
    Coordinator and Planner, Admin and Security Coordinators, Site Emergency Manager and TSC Log Keeper. Each
    position's screens show its own pages (plant status, CSF status, the log, trends, rad monitors, habitability, met
    data, release, core, nuclear instruments, rod positions, electrical, buses, secondary, core cooling, equipment, CR
    HVAC, priorities, notifications, the clocks). The **TSC Communications Console**: the ENS (to the NRC), HPN and
    offsite communicators, each with its phones and screen, and the radio base station. The **NRC Site Team Desk** in
    the glass-walled NRC room: a glass desk with two live monitors, phones and a speakerphone.
  - **TSC Status Boards** (whiteboards): STAFFING (the positions and the plant's roster of names on T-cards; the IN
    column waits for the plant's staff), PRIORITIES (worked out from the plant: the TSC activated, each challenged
    safety function, the CR HVAC, the notifications still due, the next briefing), EVENTS (the sequence of events, the
    protective action recommendation and the wind), NRC (the class, its clocks, the NRC team, what they follow up).
  - **Area Radiation Monitor** (RM-TSC-1): the dose rate where it hangs on a log meter (0.01 to 1,000 mR/hr) and a
    display; NORMAL, ALERT (2.5 mR/hr) and HIGH (15 mR/hr). **Frisking Station**: the ratemeter's count (40 cpm
    background, 3,300 cpm a mR/hr, scattering as a real count does) under its FRISK HERE sign.
  - Right-click a TSC panel for the class, when it was declared, the reactor and the CR HVAC (a monitor for its dose
    rate); sneak and right-click one with labels (a video wall display, a status board, a console) with an empty hand
    to step it to its next.
  - The pieces: **Procedure Shelf** (the emergency plan's binders, procedures and technical specifications, their
    spines labelled), **Drawing Flat File**, **Accountability Card Reader**.
- **The operations office** (round 26, #13; `nuclear/OpsOffice.java`, `nuclear/OpsPanelBlock.java`,
  `tools/ops_office.py`): work control and tagout, on the control building's second floor. Its **Equipment Out of
  Service Board** is the hub: once a second it reads the plant (the nearest reactor; the 480 V load centres and motor
  control centres within 192 blocks; the vital AC panels and battery room exhaust fans within 64) and lists what is out
  of service. Every other office panel links to its nearest board within 16 blocks.
  - **What goes on the board:** each load centre breaker (main or feeder) left open, each motor control centre unit
    off, each vital panel's main breaker or circuit open, each battery room exhaust fan off. Each item gets the next
    clearance number (from 1041) when it first goes out, keeps it while it stays out and leaves the board when it is
    put back. The board shows the number, the equipment, its train, the tags hung, OPEN or OFF and how long it has
    been out; an item that enters a technical specification action (an inverter's feeder: 3.8.7 INVERTERS, 24 hours;
    a main breaker or the DC MCC: 3.8.9 DISTRIBUTION, 8 hours; a charger: 3.8.4 DC SOURCES, 2 hours) lights its LCO lamp.
    The footer gives the plant mode (1 POWER OPERATION to 5 COLD SHUTDOWN, from the reactor), the time and the counts.
  - **Work Control Consoles** (desks three blocks wide, two positions each with two live monitors): Work Control SRO and
    Clerk, Tagout Coordinator and Clearance Writer (sneak and right-click with an empty hand to switch). Their pages:
    the plan of the day, LCO status, work orders, the out-of-service list, the newest clearance, the clearance
    writer's desk, the tag print queue, the clearances. The tagout desk has a tag printer, a tag tray and padlocks.
  - **Shift Schedule Board** (a whiteboard): the six crews' weekly rotation (days 0600 to 1800, nights 1800 to 0600,
    training, off) and the plan of the day: the day, the crew on and its shift manager, SRO and reactor operators (from
    the plant's roster), the mode and the board's counts.
    **Master Clock**: hands that keep the game's time. **Radio Charging Rack** and **PPE Shelf** (hard hats, glasses,
    ear plugs, ear defenders, flashlights on chargers): each charger's lamp red while it charges, green when ready.
  - Right-click a piece for its count, mode, crew or time.
  - The pieces: **Locked Valve Key Locker**, **Clearance Tag Rack**, **Group Lockbox**, **Operations Procedure Shelf**
    (the operating, abnormal and general procedures, the administrative procedures and technical specifications).
- **The restroom** (round 26, #14; `nuclear/RestroomBlocks.java`, `tools/restroom.py`, sounds `tools/restroom_sounds.py`):
  two stalls, a urinal and two sinks, between the kitchen and the operations office, every fixture working.
  - **Toilet** (a commercial floor-mounted toilet with an elongated bowl, an open-front seat and a chrome flush valve;
    the accessible one higher, with rear and side grab bars): right-click the flush valve to flush it (the handle goes
    down, the water churns, three seconds); right-click its **toilet paper dispenser** (twin jumbo rolls, on the
    partition or the side wall) for a Toilet Paper Roll: eight in it, refilled a roll at a time (about one a minute),
    its rolls showing through its windows; anywhere else to sit down.
  - **Urinal** (wall-hung, with its deodorizer screen and flush valve): right-click to flush. **Urinal Privacy Screen**.
  - **Restroom Sink** (a solid-surface counter with an undermount bowl, a sensor faucet, the soap spout, the insulated
    trap and supplies open below): right-click and the faucet runs for five seconds (the stream, its sensor lamp red).
    **Hand Dryer** (high-speed, with its splash guard): right-click and it blows for four seconds (its lamp green).
  - **Trash Can**: right-click to open it; whatever is in it when you close it is thrown away. The paper towels in it
    rise to a third, two thirds and the top as things are thrown in; it is emptied on the custodian's morning round
    (06:00 to 10:00 game time).
  - **Toilet Stall Door** and **Accessible Toilet Stall Door** (powder-coated partitions with pilasters, hinges and a
    latch indicator): open and close by hand; closing one from inside the stall latches it (the indicator shows
    OCCUPIED and it will not open from outside); right-click from inside to unlatch and open. A latched door with
    nobody in the stall unlatches itself.
  - The pieces: **Toilet Partition** and **Toilet Partition Headrail** (their brackets as variants: sneak and
    right-click with an empty hand for the next), **Toilet Partition Panel**, **Restroom Mirror**, **Paper Towel
    Dispenser**, **Baby Changing Station**, **Floor Drain**, **Restroom Exhaust Grille**, the **Hand Washing Sign**;
    **Restroom Wall Tile** (glazed wall tile on its facing side, the lower course with its cove base) and **(Top
    Course)** (the accent band and bullnose cap), **Restroom Floor Tile**. The windows are frosted.
- **Ladder Cable Tray** (round 26): 24-inch galvanized ladder tray (rails, rungs, black power cables on them) hung under
  the ceiling on trapeze hangers, as straight lengths (with or without a hanger), a 90-degree bend (with or without a
  drop in it), and drops where a conduit comes up under it and a cable goes down into it (one or two, each with or
  without a hanger, or one up a wall beside it). In the DC rooms every cabinet's cables run up into it in rigid
  conduit from a hub on its top: the switchboard's (from its second section), each charger's DC output, each
  inverter's two (its DC feed and its AC output), each regulating transformer's and the ground detector's.
  **Feeder Conduits (Pair)**: two conduits side by side, up from a motor control centre section's top; **(Pair,
  Run)**: the same in a full block, up from an inverter's top. **Wall Conduit**: a conduit up a wall on its straps.
- The 125 VDC Distribution Switchboard's ground detector lamps are live: both lit while the bus is alive (no ground
  faults are modelled).
- **Train labels** (round 26): the DC rooms' equipment says which train and which one it is. The chargers, the
  inverters and the regulating transformers are 1A-1, 1A-2, 1B-1 and 1B-2 (charger 1A-1 is the one MCC 1A's CHGR 1
  feeds; transformer 1A-1 the one its REG XF 1 feeds, and inverter 1A-1 the one beside it), the DC switchboards 1A and 1B (their feeders labelled for their train), the motor control centres
  MCC 1A and MCC 1B. The panels carry it as their variant and the MCC as its anchor's; one put down by hand is the
  first (1A-1, 1A).
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
- **Diesel generator rooms** (rebuilt in round 31, `tools/diesel_set.py`; Justin: "rebuild the whole diesel room,
  generator included", "Bigger than real"): the building is 67 x 26, two rooms 32 x 24 inside and 13 high, laid out
  the same (the set can't be mirrored: its gauge board, fuel filter and terminal box are on fixed sides). Each holds:
  - **The V20 set** (10 x 21, 9,000 kW, 4160 V, 514 rpm), drawn from real parts. A twenty-cylinder vee engine
    (crankcase doors with relief valves, heads and rocker covers, injection pumps and lines, fuel, charge air, jacket
    water and starting air headers, the lagged exhaust manifold), two turbochargers on charge air coolers, the exhaust
    up through bellows into a silencer hung from the roof and out through the roof to its stack, the governor and
    overspeed trip, the engine-driven pumps, the duplex fuel filter; the flywheel housing and barring gear, the
    coupling in its guard, the generator with its air hood, terminal box, nameplate and sign by train (1A, 1B), the
    exciter and fan. Walkways at 2 along both sides (Justin: "Decks at 2.0") with steps down beside the generator
    (half-block frames). Live: the coupling and the fan turn, the engine gauge board's six gauges (speed, lube oil
    pressure and temperature, jacket water temperature, fuel oil pressure, starting air) and its RUNNING and READY TO
    START lamps. Its plates, signs and gauge faces are on their own texture (`textures/entity/diesel_legends.png`, 512
    texels a block).
  - **The starting air system** along the north wall: two compressors, the twin-tower dryer, two receivers, their lines
    in the floor to the engine's two starting air headers.
  - Its fuel oil day tank in the south-east corner (its line in the floor to the engine's filter), its panels and
    battery along the south wall, louvres high in the north wall.

  - **The panels** (rebuilt in round 31 part 2, `tools/diesel_panels.py`; Justin: controls "Engine and breaker"): the
    seven pieces along the south wall in real 3D, built like the DC rooms' cabinets (meters in cases behind glass,
    chrome-bezel lamps, push buttons, switch plates with wing knobs, a pistol grip, the E-stop mushroom, relays in
    drawout cases with test switches, the 86G, windows onto card racks), each labelled live with its train (EDG 1A,
    EDG 1B): the engine control panel (engine speed and starting air, lube oil pressure and temperature, jacket water
    and fuel oil in psig and degrees F, the hour meter, 12 alarm windows, READY and RUNNING, LAMP TEST, MODE, START,
    STOP, EMERG STOP), the generator control panel (two wide: switchboard meters, the synchroscope and its SYNC lamps,
    the mimic, the breaker control switch with its flag and lamps, voltage and speed adjust, selector switches), the
    excitation and voltage regulator, the protection relays (eight relays with their targets and test switches, the
    86G), the load sequencer, the battery charger and the diesel's battery (two wide). Their controls
    (`nuclear/DieselPanels.java`, `EdgPanelBlock.java`):
    - **MODE** (click to turn it, sneak-click back): OFF, nothing starts it (and turned to OFF it stops); LOCAL, START
      and STOP on its engine panel only; AUTO, the control room's buttons and safety injection start it (the panel's
      START says to turn it to LOCAL). NOT IN AUTO is lit, and READY out, unless at AUTO.
    - **START / STOP** at the engine panel in LOCAL. A start's breaker closes itself 5 s in either way (Justin: "Closes
      itself").
    - **EMERG STOP** stops it at once from any MODE and latches in (EMERG STOP lit; nothing starts it until it is
      clicked again to pull it out).
    - **The breaker switch** (generator panel): click its left side to TRIP, its right side to CLOSE; it springs back,
      its flag red after a close and green after a trip. Tripped, the loads drop and the engine runs on light (GEN BKR
      TRIPPED); closed again once up to speed, the sequencer starts the loads again from its first step.
    - **The synchroscope** turns SLOW while the engine runs up with the breaker open and stands at 12 once it closes
      (Justin: "Emergency start only").
    - **The protection relays:** click a relay's test switch to test it: its target drops and it trips the 86G (Justin:
      "Handle and relay tests"); click the relay to reset its target. The 86G's handle trips it or resets it. Tripped,
      the breaker opens, the engine stops and nothing starts it or closes its breaker until reset (86G LOCKOUT).
    - **LAMP TEST** lights the engine panel's lamps and windows for 3 s.

  Clicking the set itself says how it stands (Justin: "Show its status"); it is started and stopped from its engine
  panel or the control room. Its room's panels follow it live.
  Each panel links itself to the nearest generator within 24 blocks that has no panel of its kind yet, so a generator
  has at most one of each of the six panels: put two generators by one set and the set follows one of them; put down a
  second set and it takes the other. A panel whose generator is broken links to another free one, or reads stopped. The
  panels show: engine speed, start air, lube oil and jacket water gauges, the hour meter (an
  hour every 1,000 game ticks run, counted from when the panel linked to its generator, so it starts at zero), the
  alarm windows (LOW DAY TANK LEVEL below a quarter; FAIL TO START when it had no fuel or too little starting air), the generator's volts, amps, kilowatts, kilovars, frequency and
  power factor, the breaker's lamps and mimic, the regulator's display and field meters, the protection relays'
  readings, the load sequencer's screen and step lamps, and the battery charger. A start plays out as a real
  emergency start on a loss of offsite power: cranking on air, running up to 514 rpm and 4.16 kV in about 4 s, the
  breaker closing at 5 s, then the sequencer starting the safety loads every 5 s (each motor start dipping the voltage
  and frequency for a moment) up to full load, 6,800 kW, with the battery chargers back last. A stop opens the breaker
  and coasts the engine down.
- **Starting air** (round 31, `nuclear/StartingAir.java`; Justin: "Starting air system ... Yes, it works", in psig):
  starting air receivers, compressors and dryers each join the nearest diesel generator within 40 blocks (use one to
  see its pressure or state and its generator; sneak-use with an empty hand to change its label). The receivers are
  full at 250 psig. Each start takes 40 psig shared over the receivers (two: 20 psig a start, five starts from full);
  a start that would leave less than 150 psig barely turns the engine over and fails (FAIL TO START, and it isn't
  ready for an auto start until the air is back). The lead compressor (A) cuts in below 225 psig, the lag (B) below
  200, each putting back 2 psig a second until the receivers are full. The dryer's POWER lamp is lit while it is
  joined. A generator with no receivers near (one built by hand without them) starts as it always did.
- **The room's own plant and fittings** (round 31 part 3, `tools/diesel_room.py`, `tools/diesel_fittings.py`;
  Justin: "Diesel Part 3"), in each pre-made room and placeable on their own:
  - Along the north wall, the **jacket water keep-warm skid** (its pump and motor, strainer, 30 kW heater, valves,
    controller with HEATER, PUMP and POWER lamps, inlet and outlet gauges) and the **lube oil keep-warm skid** (prelube
    gear pump, duplex filter, 20 kW heater, oil pressure and temperature gauges, controller), and the **jacket water
    expansion tank** on its braced stand with its ladder and sight glass. Along the east wall the **jacket water
    cooler** (shell and tube on its saddles, the service water through its tubes, the three-way temperature valve and
    its bypass, four temperature gauges). In the south-west corner the generator's **terminal and CT cubicle** (its
    bus bars, CTs, surge arresters and capacitors seen through its door windows) and the **neutral grounding
    resistor**. Their lines go down into the floor (the generator's leads too).
  - Each joins the nearest diesel generator within 40 blocks (`nuclear/DieselRoom.java`; use one to see its generator,
    sneak-use with an empty hand to change its label 1A or 1B). Their gauges, lamps and the sight glass read it
    (Justin: keep-warm skids "Looks and live gauges only", the cooler "Live gauges, no trip"): standing by, the
    keep-warm heaters cycle and their pumps run (the prelube at 30 psig) and the cooler is all bypassed; running, the
    skids stand idle and the cooler's valve opens as the jacket water passes 160 degrees F, its outlet falling and the
    service water warming; the expansion tank's level rises as the water warms.
  - **CO2 fire suppression** (`nuclear/Co2System.java`; Justin: "Discharge that hurts"): the cylinder bank by the door,
    its riser up to the piping under the roof (four branches, a nozzle every five blocks), the release station, horns
    and strobes, heat detectors and signs. Pull the station's PULL TO RELEASE handle: 30 seconds of horns and flashing
    strobes, the station counting down (ABORT stops it); then the CO2 discharges: the room (the open space from the
    bank, bounded by full blocks and doors) fogs, the engine is shut down and won't start, and anything breathing in
    it chokes (2 health a second, slowed and sick; creative players excepted) until it gets out. RESET at the station
    starts the purge: the exhaust fans run for 60 seconds and the fog clears, then all is normal.
  - **Exhaust fans** high in the south wall over their louvres: their blades turn while the engine runs and in the purge.
  - **Monorail hoist** (`nuclear/Monorail.java`; Justin: "Trolley moves", "One step per click"): a 5-ton beam over the
    engine's crankshaft; its pendant station on the west wall drives the trolley a block east or west a click and the
    hook a block up or down (STOP holds it), moving at a block a second.
  - Also an eyewash, an extinguisher by the door and floor drains by the skids.
- **Diesel fuel:** the generator burns diesel as a real 7 MW emergency diesel does, a millibucket being a litre and an
  hour a Minecraft hour (1,000 ticks): about 110 mB an hour running light and about 1,690 mB an hour at full load
  (0.23 mB per kWh more), following the load sequence. It burns from its day tank: the nearest formed storage tank of
  diesel within 24 blocks, drawn through the tank's outlet (shut the valve and it starves). With no diesel it cranks
  but won't fire; run dry it stops. In the pre-made plant each room's day tank is kept topped up from the fuel oil
  storage tank outside through its fill line (while that tank's outlet valve is open), so keep the big tank filled. A
  storage tank holds four buckets for every block of space inside. With its roof hatch lid up, a tank also takes liquid poured in from a
  bucket (or any full fluid container) right-clicked on the hatch; it holds one liquid, so another is refused.
- **The turbine hall** (turbine hall part 1, `tools/turbine_hall.py`, `tools/turbine_set.py`, `nuclear/TurbineHall.java`,
  `client/TurbineParts.java`; Justin: "Turbine hall", "Local trip + turning gear", "Three parts", "Build it as shown"):
  the medium plant's turbine building rebuilt at ground level, 100 x 44 and 36 high. Insulated metal panel walls on a
  10-block column grid with their downpipes and wall packs, a Framed Glass clerestory high in the long walls, the roof
  with rooflights, vents and a hatch; a roller door into the laydown bay at the west end. The operating floor at 12 on
  the turbine's pedestal, walkways round the turbine and its reheaters. Two steel stair towers (west and east), three
  flights each with landings at 4, 8 and 12; a caged ladder from the east tower's roof to the hall roof.
  - **The turbine-generator** (11 x 50 x 10, rebuilt from real parts after the Westinghouse four-loop plants'
    tandem-compound six-flow 1,800 rpm machine): the front standard (its gauge board: bearing oil, auto stop oil and EHC
    fluid; the speed in digits; TRIPPED and LATCHED lamps; the manual trip handle under its yellow guard; the oil lines),
    the HP cylinder with a steam chest each side, each with two throttle and two governor valves on their yokes (TV-1 to
    4, GV-1 to 4); three double-flow LP cylinders under their hoods, the crossovers down into their six pairs of
    intercept valves; the bearing pedestals; the turning gear with its lever and local station (ENGAGED, MOTOR RUNNING);
    the hydrogen-cooled generator (1,280 MVA, 22 kV, 0.90 PF, 75 psig H2) with its coolers, terminals, nameplate and
    hydrogen warning; the brushless exciter with its inspection port onto the rotating rectifier. Live: the valve stems
    ride up their yokes as the valves open (the governor valves with the load, all shut tripped), the gauges, speed and
    lamps, the shaft's mark and the exciter's diode wheel turning with the shaft (drawn at up to 40 rpm). Its plates,
    signs and gauge faces are on their own texture (`textures/entity/turbine_legends.png`).
  - **Manual trip:** the first use of the handle lifts its guard (five seconds); the second trips the turbine and opens
    the generator breaker, just as TURBINE TRIP in the control room does (the reactor trips with it above P-9).
  - **Turning gear:** its lever puts the gear in only with the shaft at rest (otherwise it says how fast the shaft is
    still turning); its motor then rolls the shaft at 3 rpm. It comes out any time, and drops out by itself when steam
    rolls the turbine off it. A hot rotor (the reactor above 150 degrees) standing still off its gear bows: TSI
    eccentricity reads high and ECCENTRICITY HI comes in on the turbine board; the DEH screen says STOPPED  ECCEN HI.
  - **Moisture separator reheaters** 1A and 1B (5 x 20 x 5) either side of the turbine on the operating floor, their
    cross-under and crossover piping to the turbine; their shell pressure and reheat steam temperature gauges follow
    the load (and fall away tripped). Sneaking with an empty hand relabels one.
  - **The feed train and the oil plant** (turbine hall part 2, `tools/feed_train.py`; Justin: "One at a time", "Pumps
    you can switch, plant responds"): under the operating floor the turbine's table on its two rows of columns, the
    three condenser shells (1A, the plant's working condenser, with 1B and 1C beside it) each on its hotwell (vacuum and
    hotwell gauges, the hotwell's level glass); the three 50% condensate pumps south of them (each with its local
    station: CP AMPS, RUN, STOP and AUTO lamps, START and STOP); the two 60% turbine-driven main feed pumps in the west
    bay (the barrel pump, its coupling in its guard, the driving turbine with its stop valves, the lube oil console, a
    six-gauge local board with the pump's speed in figures, and its turbine's control station: RUNNING and TRIPPED,
    TRIP and RESET); the EHC fluid supply unit by the front standard; the seal oil unit and the gas control station by
    the generator; the main turbine's lube oil reservoir in its room (its pumps on top, coolers and filters beside it,
    its gauge board and pump panel facing the door, its vapour extractor venting out through the south wall). Each
    joins the nearest turbine-generator within 64 blocks and shows its unit's plant: gauges, lamps, level glasses, a
    pump's coupling turning while it runs. Sneaking with an empty hand relabels a condenser, hotwell or pump.
  - **Pumps that work:** with them at the hall, main feed is no more than the pumps running carry (two condensate
    pumps or one MFP carry the full or 60% of it). A condensate pump's START runs it and STOP stops it; a stopped pump
    stands by in AUTO (after ten seconds, so another standby starts first; STOP again takes it out of AUTO) and starts
    by itself when the pumps running can't carry the feed (COND PUMP AUTO START; CONDENSATE HDR PRESS LO while they
    can't). A main feed pump's TRIP trips it (MFP A or B TRIP comes in) and RESET resets it; it is started and stopped
    from the secondary board's MFP A and B switches (it turns only with steam, the plant above 200 C). An MFP lost above
    60% with the rods in AUTO runs the plant back to 55%; in manual the steam generators' levels fall to the low-low
    trip (in under a minute) and the auxiliary feed pumps start; both lost at power start the motor-driven auxiliary
    feed pumps at once; with no condensate pump running the MFPs trip on low suction. Feedwater isolation trips them. A
    plant without these pumps at its hall runs as before.
  - **The crane, the plant rooms and the fittings** (turbine hall part 3, `tools/hall_crane.py`, `tools/oil_room.py`,
    `tools/shop_room.py`, `tools/tcr_room.py`, `tools/hall_fittings.py`, `nuclear/HallCrane.java`,
    `nuclear/TurbineControlRoom.java`, `client/CraneParts.java`; Justin: "Works from a pendant"):
    - **The overhead crane** (160 / 25 ton, 42 blocks across): a double box-girder bridge on its end trucks, the trolley
      with the main and auxiliary hoists, the hooks on their falls of rope, floodlights and travel beacons, the runway's
      rails, conductor bars and end stops laid along it. It runs from the crane pendant stations (one on each stair tower,
      one in the laydown bay): hold NORTH, SOUTH, WEST or EAST and the bridge or the trolley runs that way (the bridge 2
      blocks a second along the hall to its runway's ends, the trolley 1 across it), hold UP or DOWN for either hook (down
      to the first solid block under it); let go and it stops. The red STOP takes its power off (the pendant's POWER lamp
      goes out) until START; the horn sounds it. Its floodlights are lit with power, its beacons flash while it travels;
      a click on the crane says where everything is.
    - **The switchgear room** (load centres LC 1C and 1D, non-Class 1E, their trays, fan coil, log desk and one-line
      diagram), **the lube oil room** (the reservoir with the lube oil conditioner, the clean and dirty storage tanks
      T-401 and T-402, the transfer pumps, a pre-action sprinkler system lettered for the room, the oil spill kit),
      **the workshop** (benches, pegboards, lathe, drill press, shop press, parts washer, tool chest, a portable gantry,
      a welding booth with its screens, table, MIG welder and torch cart) and **the turbine control room**: the turbine
      control system's power cabinet, its TMR controllers R, S and T with their networks, two I/O cabinets, the TSI rack,
      the excitation cabinet, generator protection A and B, and a console at the TURBINE CONTROL position (pages TURBINE
      GEN, TSI, EHC, TURBINE OIL). The cabinets and the console join the nearest turbine-generator within 64 blocks and
      read it as the boards do: bearing vibration growing with speed and peaking through the critical speed, thrust and
      differential expansion with load, eccentricity high while a hot rotor bows; field breaker 41 closing at 95% speed,
      the field current and volts, kV and MVAR; the 86G lockout tripped (TRIP and 32 lit) after a hot trip, reset when it
      latches again. Sneaking with an empty hand relabels the I/O cabinets and protection panels; the workshop's benches,
      pegboards, lathe and gantry step through their set-ups with a click.
    - **Fittings:** fire hose reels with their page stations and horns, the crane's travel direction signs (N, E, S, W:
      the way the pendant's buttons run it) high on the walls, the laydown bay's floor markings and stencils, hearing and
      eye protection signs.
  - The main steam lines come in under the floor; the generator's bus leaves under the floor through the east end wall
    (site y 9) to the transformer yard.
  - Still to come: part 2, the condenser and feed train (pumps you can switch, the plant responding) and the lube oil,
    EHC, seal oil and hydrogen skids; part 3, the plant rooms, the crane on its pendant and the fittings.
- **The medium plant's control building** (round 18, `tools/medium_plant.py` `control_building`), four floors laid
  out like a real plant's (NRC fire hazards analyses of TMI, Susquehanna and Peach Bottom; IEEE 484 battery rooms):
  - Grade: Battery Rooms 1A and 1B, each a 125 VDC station battery of two 60-cell strings in parallel, each string on
    two step racks along a wall, one string on each long wall (Station Battery, four blocks long, 30 cells in clear jars,
    below; round 26: the second strings, and Battery Room 1B's door moved to its east end to make room), the second
    string's cables up its wall into a tray that turns the corner and joins the first's; the hydrogen monitor, its
    sensor head and the exhaust fan over a louvre on the outside wall, the eyewash with its sign, the acid spill kit, the
    battery acid and hydrogen signs, NO SMOKING outside the door; its cables up a DC Cable Riser from each rack's end into a DC Cable Tray under
    the ceiling, through the wall and down into the 125 VDC Distribution Switchboard in the DC Equipment Room next door,
    beside the battery chargers, Vital AC Inverters and Regulating Transformers (in groups by number: charger, inverter,
    transformer) and, at the row's end, the DC Ground Detector & Battery Monitor, with the room's 480 V motor control
    centre feeding the chargers by cables in the slab and the transformers by the ladder tray under the ceiling (over
    the row, along the side wall and over the MCC, a conduit up into it from the top of every cabinet in the
    row, two from each inverter, and two up from the MCC's REG XF section); on the side wall
    the two 120 VAC vital panels, each fed by its inverter (a conduit up from the inverter's top into the tray, along
    it and down the wall into the panel's top), and the two 125 VDC distribution panels (older rooms round 3, where the
    relay cabinets stood); along the other wall the log desk with its phone, drawings and the DC system monitor's
    laptop, the printer, the drawing rack, the battery service board, the one-line diagram and the train sign, the
    test cart, rubber mats along the cabinets' fronts, the emergency lights and smoke detectors; the switchgear room
    (older rooms round 1, `tools/switchgear_room.py`: both trains' 480 V load centers face to face across an aisle of
    rubber mats, fed from the main transformer and feeding the chargers, above; over each lineup's back an overhead
    power tray in its train's colour with a cable drop into every feeder section's gland plate, its run's end turning
    up through a fire-stopped sleeve in the ceiling: LC 1B's into a Train B riser up to its row's power tray in the cable
    spreading room, LC 1A's into a one-hour fire-wrapped raceway that rises, runs west along the north wall out of the
    Train B area and drops into Train A's power tray; a fan coil unit hung under the ceiling with its supply duct and
    two registers along the aisle; the breaker handling gear at the aisle's east end (a spare breaker on its lift
    truck, the remote racking device on its cart, a breaker test set); a standing log desk three wide (the phone and
    the room's procedure, the rounds logbook, and a laptop whose screen lists the nearest load centre's breakers live:
    each one's state and amps, the transformer's temperature and fans) under the framed one-line diagram of both
    lineups; the lineups' LC 1A and LC 1B signs, the DANGER sign outside the door; lights, smoke detectors, emergency
    lights, an extinguisher);
    the remote shutdown room (the remote shutdown panel on its north wall over a rubber mat, battery emergency lights
    above it; the Remote Shutdown Station three wide on the east wall; along the south wall the AOP procedure rack, an
    SCBA cabinet, the Transfer Key Cabinet by the door, the Plant Communications Station and the radio charging rack;
    the master clock over the door, the Remote Shutdown Area Sign, an extinguisher, a smoke detector); the locker room; and the
    secure entrance (a vestibule, the VITAL AREA and prohibited items signs over its inner doors; the Security Post, its
    officer's chair clear of the wall; an entry and an exit Speed Gate, below, the Hand Geometry Reader at the entry
    lane, with framed glass from them to both walls; inside the gates the Evacuation Plan and the master clock on the
    corridor wall, the AED, smoke detectors and emergency lights). Hydrogen exhaust louvres high in the battery rooms' outside walls. The vestibule, the glass along the speed
    gates and the shift supervisor's office are Framed Glass (below).
  - First floor: the cable spreading room under the control room (punch list #15, below: four rows of tray in three
    tiers, Train A orange, non-safety grey and Train B blue, bundles up into every board, Train B's fire-wrapped
    raceway, risers up the north wall, the bare slab overhead, the pre-action sprinklers), the relay
    room (round 26, #10: four rows back to back either side of the aisle under its door, A the protection racks and
    the RCP relays, B the SSPS bays, nuclear instruments and generator relays, C the electrical relays, D the pumps'
    relays; a control cable tray over every row with a bundle down into every cabinet, the header in from a tee in the
    spreading room's top tray through both corridor walls in fire stops; the row signs at the aisles' ends, lights and
    smoke detectors along the aisles, the drawing rack, the SOE recorder's console three wide and its printer in the
    south-east corner), the server room on its raised floor.
  - Second floor: the main control room (round 23), laid out as a Westinghouse four-loop plant's horseshoe: along the
    north wall the Safety Systems, Reactor Coolant, the reactor control console in the middle (in line with the
    boards), Steam and Feedwater and Turbine Generator boards; down the west wall the Electrical board, down the east
    wall Radiation and Ventilation; the plant display wall over the middle. In front, the Control Room Operator
    Consoles (older rooms round 5: the reactor operator's, the balance of plant operator's and the control room
    supervisor's behind them), the At The Controls line across the floor between them; the shift manager's glazed
    office with his desk along its glass, the EAL chart and the key locker; the procedure racks along the back wall,
    the drawing rack, the SCBA cabinet and an extinguisher by the door, the office copier; two master clocks over the
    boards' ends, the alarm printer by the supervisor. Across the corridor the
    Technical Support Center (round 26, #12, enlarged into the old kitchen: the video wall and classification board on
    its west wall, seven consoles in rows, the communications console under the south windows, the glass-walled NRC
    room, the status boards and procedure shelves on its north wall, the frisker, area monitor and card reader at its
    door), the smaller kitchen, restroom and operations office, with windows.
  - Top floor: the Control Room HVAC Equipment Room (round 26, #11; its sign at the stair door). Each train (A west,
    B east) has its intake plenum against its louvres in the north wall (with the radiation monitor's sample point,
    access door and the monitors on the wall beside it), its emergency filtration unit and air handler, the ducts
    between them with their dampers, and the supply and return ducts down through the floor (sleeves, fire dampers)
    to diffusers and return grilles in the control room's ceiling. In the south bay each train's chiller and chilled
    water pump, the mains overhead on trapezes, each loop's expansion tank and chemical feed pot, a refrigerant sensor
    by each chiller and the refrigerant monitor on the west wall; spare filters on racks along the south wall; the
    HVAC control panel on the north wall between the trains; smoke detectors, extinguishers and a drawing rack.
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
- **Plant Display Wall** (round 32, live; `tools/display_wall.py`, `nuclear/DisplayWall.java`): the control room's
  large display panel over the reactor control console, a video wall of rear-projection cubes (seven across, three
  high, hairline seams) in a graphite cabinet with louvred ends, a service door under each column of cubes (louvres,
  latch, the cube's number, its POWER and LAMP lights) and the fascia's nameplate and exhaust grille. Its picture is
  fixed, as a real plant's, so the whole crew sees the same thing: across the top a banner of the boards' alarms in
  its room as they come in, newest first (red trips, amber alarms; flashing until acknowledged at their board,
  slowly once cleared until reset; the wall never sounds the horn itself), with the count in and not acknowledged; on
  the left the six critical safety functions coloured by the Westinghouse status trees (green, yellow, orange, red;
  the procedure the worst calls for) and the key parameters, the plant's mode and the time; in the middle the plant
  mimic (the reactor and its power, the pressurizer, the four loops with each coolant pump red running or green
  stopped and each loop's flow and temperatures, each steam generator's level, the steam lines through the MSIVs and
  relief valves to the turbine and generator, the condenser, the condensate and main feed pumps, the feed regulating
  valves, auxiliary feed and the condensate storage tank, the ECCS pumps, the reactor's state; every line bright while
  it carries flow and dim while it does not); on the right the electrical lineup (the switchyard, the generator and its
  breaker, the reserve transformers, the 4160 V safety buses with their diesels and breakers, the 480 V, MCC, DC and
  vital buses) and the last five minutes' trends of power, pressurizer pressure and Tavg. Clicking it says what it
  shows. Its lines and symbols are flat colour, its fixed words on its own legend texture
  (`textures/entity/display_wall_legends.png`), its figures in the game's font.
- **Plant Overview Screen** (round 34, live; `tools/overview_display.py`, `tools/overview_screen.py`,
  `nuclear/OverviewScreen.java`): a slim large-format LCD three blocks across and two high on a wall mount (its back
  shell, a graphite bezel standing a hair proud of the black glass, the maker's badge, the remote's window and a lit
  power light on the bottom bezel), with a **Screen Power Raceway** under its middle: a painted steel channel down the
  wall to a duplex receptacle over the floor. In the medium plant there are three: on the control room's east wall, in
  the administration building's board room and in the plant manager's office. Its picture, in the display wall's dark
  style (colour kept for state: green running, light grey standby, amber in alarm, red tripped or on fire), is live:
  - the title bar: the unit, the TSC's emergency class as a banner while one is declared (white for an unusual event,
    amber for an alert, orange for a site area emergency, red for a general emergency, with its declaration time), the
    plant's mode, the time and the data link's heartbeat;
  - the site map, the medium plant from above in its real layout (`medium_site`): each building filled by what runs in
    it (the containment by the reactor, the turbine building by the turbine, the switchyard by the grid, the intake by
    the circulating water pumps, the towers' throats white with steam while they carry heat, each diesel's room by its
    diesel, the auxiliary building by charging and safety injection, the control building by its HVAC and the cable
    spreading room's fire system, the fuel building by its radiation, the tanks by their levels), its tag with its
    live figures and a radiation badge (the worst of its monitors: green, amber, red); the main steam, feedwater,
    auxiliary feed, safety injection, generator output, offsite power and the buried circulating water lines bright
    while they flow; a key, north and a scale;
  - beside the map the plant status (reactor power, generator, grid, mode, diesels, control room HVAC, fire protection,
    site radiation, emergency class) and the last five minutes of reactor power and generator output;
  - the alarm strip: the control room boards' newest four alarms (flashing until acknowledged at their board, no horn)
    and the count in and not acknowledged.
  A click on a building shows its own page for 30 seconds (SITE MAP goes back sooner; a click elsewhere on a page keeps
  it up): the reactor (the vessel with its core glowing by power and its rods hanging in as far as they are, the four
  loops with their steam generators' levels and pumps, the pressurizer and containment figures), the turbine (steam,
  turbine, generator and breaker, condenser and hotwell, condensate, heaters and main feed pumps), the electrical one
  line (switchyard, main and reserve transformers, the 4160 V buses with their diesels, the load centre, the MCCs, the
  DC buses), the diesels (each one's state, speed, load and starting air, its load sequencer's eight steps, voltage,
  frequency and day tank), the control building (HVAC trains, the fire system's lamps, display and riser, the emergency
  class, its notifications' times and the wind), the auxiliary building (the ECCS and auxiliary feed pumps from the
  RWST and CST), the fuel building (the pool and its racks, the dry cask pad, the core's fuel) and the intake and
  towers (the pumps, the condenser, the towers); each with its figures, a trend of two of them and its radiation
  monitors. When a building goes into alarm or trip the screen shows its page by itself for 30 seconds (unless
  someone chose a page in the last half minute). A screen with no control room boards within 256 blocks says NO
  PLANT DATA. (The game's grid is always there: the switchyard stays green; the map's lost-grid colours wait for a
  model of offsite power.)
- **Control Room Operator Console** (older rooms round 5; Justin: "Control room furniture"; `tools/cr_room.py`,
  `nuclear/ControlRoomConsoles.java`, `CrConsoleBlock.java`): one operator's position, three blocks wide and two high,
  labelled REACTOR OPERATOR, BALANCE OF PLANT or CONTROL ROOM SUPERVISOR (on the plate over its screens). A black
  steel console: two drawer pedestals, the worktop with its rubber front edge, the monitor bridge along its back with
  three 27-inch screens of the plant process computer, the keyboard and mouse, the phone set, the plant page station
  (its PAGE and ALL CALL keys, the gooseneck microphone), the emergency procedure open in its holder (E-0 REACTOR TRIP
  OR SAFETY INJECTION), the operator's log, a headset, the computer under the desk. Its screens read the plant live:
  it links the control room's display wall within 32 blocks (else the nearest control board), and each screen shows a
  page of what that board sees, every value in white, amber or red as the wall writes it. Right-click a screen to step
  it to its position's next page:
  - Reactor operator: REACTOR (state, power, Tavg / Tref, core exit, subcooling, rod control, bank D), RCS
    (pressurizer pressure and level, the four loops' flows, Thot / Tcold), NIS (source and intermediate range, N41 to
    N44, startup rate), RODS (the shutdown banks, control banks A to D, rod speed, AUTO or MANUAL).
  - Balance of plant: SECONDARY (the four steam generators' levels, steam header, main feed, MSIVs), TURBINE GEN
    (turbine, speed, MWe, frequency, generator breaker, vacuum, switchyard), ELECTRICAL (4160 V buses, the diesels, the
    load centre, 125 VDC A and B), AUX FEED (the three AFW pumps, total, CST, steam generator lo-lo, safety injection).
  - Supervisor: ALARMS (the display wall's banner: how many in and new, the newest five with their boards, red or
    amber), CSF STATUS (the six critical safety functions green, yellow, orange or red, and the procedure to go to),
    PLANT STATUS (mode, reactor, power, MWe, safety injection, alarms, the time), SAFETY INJ (its state and time,
    containment pressure, RWST, pressurizer pressure, subcooling, feed isolation).
  Right-click anywhere else for what it reads and which page each screen is on.
- **Shift Manager's Desk** (older rooms round 5): the same build with two screens (PLANT STATUS, ALARMS, CSF STATUS,
  stepped by a click), the red ENS phone to the NRC beside his own, the emergency plan's classification binder
  (EPIP-01), the shift log and a desk lamp.
- **Control Room Procedure Rack** (older rooms round 5): steel shelving two blocks high, labelled by section:
  EMERGENCY OPERATING PROCEDURES (E-0 to ECA-3.1), FUNCTION RESTORATION (F-0, FR-S.1 to FR-I.3), ABNORMAL OPERATING
  (AOP-1 to 20), ALARM RESPONSE (each board's), GENERAL & SYSTEM OPERATING (GOP-1 to 6, OP-1 to 14), TECHNICAL
  SPECIFICATIONS (TS 1.0 to 5.0, bases, TRM, ODCM, COLR), EMERGENCY PLAN IMPLEMENTING PROCEDURES (EPIP-01 to 14, the
  EAL tables) and SURVEILLANCE PROCEDURES; four shelves of five binders, each spine in its section's colour with its
  number, each shelf's edge labelled with its range, the section's name across the top.
- **The control room's fittings** (older rooms round 5): the **SCBA Cabinet** (two blocks: two breathing apparatus,
  their cylinders, harnesses and masks, behind its clear door; SCBA - EMERGENCY USE ONLY), the **Emergency Action
  Level Chart** (two blocks across: the recognition categories R, F, H, S against UNUSUAL EVENT, ALERT, SITE AREA and
  GENERAL), the **At The Controls Sign** on its post at each end of the **At The Controls Floor Line** (a red band
  edged white across the carpet: AT THE CONTROLS - LICENSED OPERATORS ONLY).
- **Operator Console**: a black desk with a raised shelf for its screens that joins up side by side; each block has
  its own setup (two screens and a keyboard; one wide screen, keyboard and phone; stacked screens and an open
  procedure; a screen, phone and papers), everything inside its own block.
- **Feeder Conduit** (round 25b): 4-inch rigid steel conduit, in pieces that stack in a column: the hub piece,
  plain lengths with couplings, the ceiling piece (its red fire-stop and sleeve), the floor piece (sleeve, fire-stop
  and a yellow strut clamp), and the tray piece, which turns in under a cable tray on its south side through two cast
  fittings. In the medium plant the DC rooms' cabinets use it (the switchgear room's load centres have overhead trays
  since older rooms round 1).
- **The switchgear room's pieces** (older rooms round 1, `tools/switchgear_room.py`): **Cable Tray (Power, Drop
  Down)**, a power tray (train by its variant as the spreading room's, with or without its hanger) with the cables
  leaving through its rungs; **Switchgear Cable Drop**, the bundle tied down from it into a gland plate; **Cable Tray
  (Power, Up Through Ceiling)**, the run's end, the cables turning up through a fire-stopped sleeve; **Fire-Wrapped
  Rise (Floor Sleeve)**, **Fire-Wrapped Raceway (Elbow)**, **(Hanger)** and **(Into Tray)** for a train's cables
  through the other train's area; the **Switchgear Room Fan Coil Unit** (and its coil end, the chilled water pipes
  into the wall), **Supply Duct**, **(Hanger)**, **Register** and **End Cap**; the **Breaker Lift Truck** (and its
  mast) with a spare breaker on it, the **Remote Racking Device**, the **Breaker Test Set**; the **Switchgear Log
  Desk** (phone end and logbook middle) and **Switchgear Log Desk (Laptop)**, a live piece: its screen is the trip
  unit viewer of the nearest load centre within 24 blocks (a line a breaker: its state in green, white or amber and
  its amps; the bus volts; the transformer's temperature and fans), and a click on it says it in a line; the
  **Switchgear One-Line Diagram**, three blocks by two, framed; the **LC 1A** and **LC 1B** signs and the
  **Switchgear Room Danger Sign**.
- **Lockers and filing cabinets in real 3D** (Justin's walk through of the Drone Tower, 25 October 2026, redesign 4:
  "override the model so the admin building updates too"). A **Locker** block is now a pair of full-height steel
  lockers, two blocks high (`tools/plant_lockers.py`). Each door is its own panel standing proud of the carcass with a
  shadow gap round it. It has real louvre slots under little hoods, a lift handle with a padlock hasp, a card holder
  and hinge knuckles; the top's lip stands over the doors and the toe-kick is set back. The **Open Locker** and
  **Security Locker** have their right-hand door swung right out and a hat shelf inside. Their wearable coat (lab coat,
  security jacket) hangs sideways on a front-to-back rail (`client/LockerRenderer`). The **Open Crew Locker** (the
  Drone Tower's) holds the crew's flight jacket, a headset and a kit bag, all fixed. The **Filing Cabinet** and **Filing
  Cabinet (open)** are a three-drawer lateral cabinet, 75 by 53 cm and 94 cm high (`tools/plant_filing.py`). Its drawer
  fronts stand proud with dark gaps between them; each has a bar pull on posts and a labelled card holder, and the top
  drawer has a lock. Open, the top drawer is pulled out with its suspension files. Its box is now 53 cm deep against
  the wall.
- **Long Glass Coffee Table** (the Drone Tower's break area, 25 October 2026, `tools/plant_tables.py`): three blocks
  long, 2.4 m by 60 cm and 42 cm high. It has the glass tables' 19 mm toughened glass on two black steel sled frames, with
  rails under its long edges; on it two mugs, magazines and a TV remote. Placed whole like the other multi-block pieces.
- **The plant's doors** (doors round, `tools/plant_doors.py`, `nuclear/PlantDoorBlock.java`): the **Steel Door**,
  **Glass Door** and **Lead-Lined Door** are real 3D doors that swing. A steel door hangs in a pressed steel frame
  (jambs and head the frame's depth into the wall, its stops, a face round the opening on the side you come from):
  its leaf has a wired-glass vision lite, butt hinges, lever handles both sides and a stainless kick plate, and a
  surface closer on the push side whose parallel arm folds and unfolds as it swings. A glass door is a narrow stile
  dark bronze storefront door in the framed glass's plane (push bar one side, offset pull the other, its closer in
  the head); the lead-lined door keeps its painted faces and airlock. Opening or shutting, the leaf swings over half
  a second (`client/PlantDoorRenderer.java` draws it turning about its hinge pin from the door's part models), and
  a door with a closer shuts itself 3 seconds after it has swung open (the lead door 5), once nobody is in the
  doorway (redstone holds it open). Two doors side by side, hinged on their outer edges, are a pair: no jamb between
  them, an astragal on one. **Panic bars**: a door can have exit hardware (a rim panic bar on its push side); the
  creative tab and the drop keep it. **Badge readers**: a door can have a badge reader on the wall beside it, on
  the side you push it from: from that side it opens only for its owner (who placed it; the plant's owner when
  `/nuclear plant` built it), the owner's party and players in creative mode (Justin: the reader accepts the owner
  and their party); anyone may leave from the other side. Its light shows green while the door is open. In the
  plants every double door has panic bars, the outside doors swing out with their panic bars inside, and the vital
  rooms' doors (control room, computer room, switchgear, relay, cable spreading, remote shutdown, battery and DC
  rooms, the TSC) have readers. The restroom's stall doors swing too. The Drone Tower's **Maintenance Door** is one of
  these doors too (a solid steel pair; [drone-tower-tripillar.md](drone-tower-tripillar.md)), its leaves the last part
  models (32 to 35). **Roller Door**: guides up the opening's
  ends, a bottom bar on the curtain (at the foot shut, at the head open, riding the curtain's edge as it rolls), the
  barrel's end brackets at the ends only and the motor operator on the wall beside the left end; the **Roller Door
  Push-Button Station** on the wall beside it opens (OPEN) and closes (CLOSE) the nearest roller door (STOP only
  clicks: a door finishes its travel).
- **The plant computer room's pieces** (older rooms round 2, `tools/server_room.py`, `nuclear/ServerRoom.java`):
  the **Server Rack**, **Network Rack**, **Storage Rack** and **Plant Computer Rack**, 42U cabinets a block wide and
  two high with every unit modelled (servers, switches and patch panels with their cords, the storage shelves'
  drives, the process computer's I/O cards); their lights live (power, activity, links, status and drives,
  busier by day, amber with the room hot, dark with the UPS off), their numbers worked out from where they stand
  (A-01 ...), their front doors swinging open on a click. The **UPS Module** (the room's hub), **UPS Battery
  Cabinet**, **UPS Maintenance Bypass Panel** and **Power Distribution Unit**: the UPS is always on its input, its
  battery full. The **Computer Room Air Conditioner** (CRAC-1 and CRAC-2; a click on its isolator turns it off or
  on): with none running the room heats past 27 °C in a few minutes. The **Clean Agent Cylinder**, **Releasing
  Panel**, **Horn and Strobe**, **Abort Station** and **Manual Release Station**, the agent pipe and its nozzles: one
  smoke detector seeing a fire is an alarm, two start a 30 second pre-discharge countdown (the abort station held
  holds it, starting again when it lets go), then the agent floods the room and puts out every fire in it; the
  cylinders are empty until RESET (new cylinders). The manual release starts the countdown at once, and no abort
  stops it; DRILL runs the alarm and countdown with no discharge. The **Plant Process Computer Console** (three
  screens: plant data, alarm summary, computer room), the **Cable Ladder and Fiber Runway**, the **Perforated Access
  Floor Tile** and the row and safety signs.
  - **It only looks live** (25 October 2026; Justin: "make it look like it works, but it doesn't actually need to be
    calculating any kind of live numbers", for these pieces wherever they stand). Nothing in the room ticks on the
    server. Its screens, meters and lights are drawn by the players' games: the UPS's input volts and load, the
    PDU's amps, the racks' traffic, and the console's plant data (a unit at full power, its readings drifting a
    little; no reactor is read). The room's air is worked out where it is drawn, from where it stood when a CRAC was
    last switched. The racks' numbers and each piece's hub are worked out there too. The fire system wakes only when
    something happens: a fire put down near the room has its detectors look, and only while its alarm, a countdown
    or a drill runs does the UPS module tick, once a second. No load centre is read (before this, the UPS was fed
    from LC 1A's breaker 09 and the CRACs from the load centres' CR HVAC breakers).
- **Emergency Light** (round 24, rebuilt in round 25b): an 8-hour battery emergency light for high on a wall, every
  part modelled. A cream steel cabinet on a wall flange, louvred both sides, its front with a TEST push button, the
  AC ON (lit) and HIGH CHARGE pilot lamps, a riveted nameplate and a latch; two round sealed-beam heads on stalks out
  over its top, hanging in yokes with pivot knobs and aimed down and out at the floor. Its feed comes into the top
  of the cabinet through a connector.
- **Emergency Light Conduit** (round 25b): the light's 3/4-inch conduit down the wall from the ceiling, with a one-hole
  strap and a coupling; it lines up with the light's own conduit stub.
- **Relay Cabinet** (rebuilt in round 26, `tools/plant_panels.py` `relay_cabinet_boxes`): a steel cabinet that stacks
  two high, every part modelled, with a glass-front door (hinges, a black gasket, a locking handle and an engraved
  RELAY CABINET nameplate). The block at the bottom of a stack shows the wiring and the plinth; every block above it
  shows the relay panel; the top block also gets the lid.
  - The relay panel holds three protective relays (50/51 phase overcurrent, 51N ground overcurrent, 27 undervoltage).
    Each sits in its drawout case with a cover frame and latches, a tap block with its tap screw, a time dial, the
    induction disc standing out of its slot between the drag magnet's jaws, a target, a seal-in unit and a target
    reset rod. An engraved nameplate is under each, then its test switch: a row of knife blades with red grips on
    the trip circuit and black on the rest. Below are the 86 lockout relay with its handle and the 125 VDC and
    TRIP CKT pilot lamps.
  - The wiring: two numbered strips of terminal blocks on DIN rails (TB1 and TB2, a blue return terminal every
    sixth). Each terminal is wired through the gaps of a slotted wiring duct. Two control cables come up through the
    floor into a clamp, and their cores fan out into the ducts. There are also two miniature breakers on a rail and
    the wireway along the top.
- **Speed Gate** (`nuclear/SpeedGateBlock.java`, `SpeedGatePedestalBlock.java`, `SpeedGateBlockEntity.java`,
  `client/SpeedGateRenderer.java`, `tools/speed_gate.py`; Justin, 8 October 2026, after the sliding glass fare gates
  at Grand Central; remade realistic on 22 October 2026 for the Drone Tower's vestibule, and at Justin's word here too:
  "I like this one a lot better"): a one-way security lane. Placed looking the way through, it stands on the block you
  come in by, keeps the block ahead clear and puts a cabinet either side of both, the way real sliding speed gates are
  built: brushed stainless, 1.8 long, 0.66 wide and 1.04 high with rounded ends on a recessed black kick, a glossy black
  glass top with a stainless rim, two bands of dark infrared sensor windows down each lane face, and over the door line
  a stainless-framed glass housing to 1.74 that takes the doors. Two 10 mm tempered glass doors 1.55 high (too tall to
  jump), with polished edges, a frosted band of dots at 1.40-1.50, a clear rubber safety edge and a status light along
  the top edge, meet in the middle; walk up to them the way through and they slide back into the cabinets (brush seals
  at the slots), showing parked in the housings, and close a moment after the lane is clear. From the other side they
  stay shut. (25 October 2026: shut, a gate looks for whoever is walking up only while a player is within 8 blocks.) The card reader's mark glows under the glass top of the lane's right-hand cabinet at the end you come up
  to; guide lines along the lane's edges of both tops and the doors' top lights show white waiting and green open; a
  pictogram on that cabinet's end shows a green arrow, and on the far end of the other cabinet a red cross faces
  whoever comes the wrong way (the old sign gantry is gone). Lanes stand side by side on a shared cabinet: an exit
  lane beside an entry lane facing the other way (as at the plants), or more of either. Each cabinet has two slots,
  0.6 px either side of the line, and the lanes along a row take them in turn, so two lanes' doors park in one housing
  without meeting. Breaking the lane block takes the pedestals only it used; breaking a pedestal takes the lanes beside
  it. One item, crafted from glass panes, steel plates and redstone.
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
- **Decay heat:** after shutdown the core keeps making decay heat, starting at 6.5% of the power before and falling as a
  real core's does, quickly and then for hours (1 / sqrt(1 + t / 10 min)): 6.2% a minute after a trip from full power,
  4.6% after ten, 2.8% after three quarters of an hour, 1.8% after two hours (diesels make real power, 27 October 2026:
  it used to die away in a few minutes, so nothing that lost its cooling could ever damage its fuel). It is part of the
  rated heat, so the plant never has to carry more than 100%. Left to itself after a trip the plant cools to 175 °C in
  about 45 minutes; open its ARVs and it is there in a few, and below 175 °C the RHR pumps (with power on a safety bus)
  take it down to cold shutdown (refuelling needs under 120 °C). It cools no faster than 2 °C a second.
- **Cooling and temperature:**
  - Heat carried away is the smaller of the heat made and (loops ÷ 4) × the share of cooling water supplied.
  - The core settles near 300 °C. Any heat not carried away raises it.
  - It alarms at 350 °C. With protection on, it SCRAMs at 370 °C.
  - With protection off, 1,200 °C damages the fuel. The core then shuts down until it is refuelled. Meltdown and the
    blast are stage 4.
  - Above 400 °C (the core uncovering, its water boiling off) it heats four times as fast for the heat left in it; a
    molten core gets no hotter than 2,800 °C.
- **Losing power (diesels make real power, 27 October 2026; `nuclear/PlantPower.java`, docs/plans/diesels-real-power.md):**
  - The grid is the switchyard's. In a thunderstorm each surge its arresters count is a lightning bolt onto the yard you
    see (onto the line's gantry, the breaker, the disconnect or SAT-1; it only shows, it burns nothing); one strike in
    eight trips the lines and the grid is lost for 2 to 5 minutes, then they reclose by themselves (the Yard
    Controller says GRID LOST and how long). About half the storms lose it once. A plant with no switchyard always has
    its grid.
  - The plant's own buses (the coolant, condensate and circulating water pumps, main feed) are live while the
    generator carries them or the grid does through SAT-1. Losing the grid at power, the generator loses its load and
    the turbine trips, and the reactor with it; the buses go dead: the coolant pumps coast down (their breakers open:
    they stay off until started from the board, which won't start them on a dead bus), the condensate and circulating
    water pumps stop (they start again with their bus), main feed is lost, the aux feed pumps get their start signal.
  - With every coolant pump stopped, a hot core drives natural circulation (5% flow a loop with water in its steam
    generator), enough for decay heat. With no condenser the steam goes out the relief valves to the air.
  - Safety buses 1A and 1B are fed from the grid through the RES XFMR A and B breakers, else by their own diesel. Losing
    the grid trips the RES breakers; each diesel at AUTO (with its DC, air and fuel) starts by itself and closes onto
    its bus five seconds later; its load sequencer starts the bus's loads a step every five seconds: the RHR pump at
    step 3, service water at 5 (the diesels' own jacket cooling), the motor-driven aux feed pump at 6 (the slot its
    legend gives the containment spray pump, which nothing signals yet), the battery chargers at 8. The turbine-driven aux
    feed pump runs on steam with its DC control power (the batteries carry it while the chargers have no power).
  - The test switch: the electrical board's RES XFMR A and B breaker switches work. Open one and its bus goes to its
    diesel (a one-train test; with the generator on, the plant's own buses stay on it); with the grid back, close it to
    put the bus back on the grid, which takes the diesel off it (it runs on unloaded until stopped). A RES breaker
    won't close with the grid lost.
  - With the generator off, SAT-1 feeds the plant's load centres from the grid (it used to be only a temperature), or
    the diesels do once their sequencers reach the chargers.
  - If nothing feeds the steam generators (no diesels, the turbine-driven pump lost), they boil dry in about 45
    minutes and the core heats on its decay heat until the fuel is damaged, about an hour and a half in.
  - The boards: OFFSITE AVAILABLE, RES AUX XFMRS and the switchyard's mimic follow the grid; 345 KV BUS VOLT LO, LINE 1
    and LINE 2 TRIP light with it lost, RES AUX XFMR A/B TROUBLE with a bus off the grid, BUS 1A/1B UNDER VOLTAGE with
    a bus dead, LOAD SHED on the safety board; the bus kV meters read the buses; each bus's breaker lamps light as the
    sequencer starts them. The overview screens show the grid LOST.
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
- Lettering is kept sharp enough to read (round 26; Justin: "Make sure all the text is clearly legible ... that
  applies to everything"): the plant's panels (diesel room panels, relay cabinets, DC room chargers, inverters,
  transformers, switchboards, vital panels) are drawn at 512 px a block, the battery tags likewise, and the equipment
  sheet's nameplates, titles, placards and binders are drawn at 128 px a cell in bold letters (the sheet's cells are
  128 px; the other materials keep their 64 px texels, doubled).

## Dependencies and assets
- **Dependencies:** none beyond Fabric API.
- **Assets:** all art, models and the Geiger sound model are original and generated by `tools/nuclear.py`:
  - the hazmat sheet;
  - the equipment material sheet (1024 x 1280: 128 px cells);
  - the fuel machine models;
  - icons, pipe models and the console's SCRAM button.

## Verification
- `tools/check_mod_data.py` checks that the Java matches `tools/nuclear.py`: equipment ids and sizes, pipe ids, and
  the renderer's and hazmat layer's material regions.
- `tools/check_mod_data.py` also checks the doors (every door and roller door state has a model; the part models
  `nuclear/PlantDoors.java` numbers; the renderer's hinge pins, closer arm and swing times match `tools/plant_doors.py`)
  and the plant computer room (every piece registered; every live line, lamp and gauge written by
  `client/EdgTelemetry.java`; the racks' doors open).
- **Focused game tests** (`13-test-focus.bat` with focus `nuclear`):
  - radiation falloff, shielding and the suit;
  - dose sickness and carried sources;
  - the core model: no overheat on standard fuel, loss of cooling trips at 370 °C, protection off damages the fuel;
  - diesels make real power: decay heat's curve and the cooldown (`decayHeatFallsAsARealCoresDoes`), a loss of
    offsite power with the diesels carried by natural circulation and aux feed with no condenser
    (`lossOfOffsitePowerCoolsByNaturalCirculation`), a station blackout fed by the turbine-driven pump for the hour
    and with no feed boiling dry at about 45 minutes and damaging the fuel at about 95 (`stationBlackoutDamagesTheFuelWithNoFeed`),
    the switchyard's lines tripping and reclosing (`YardGameTests.linesTripAndReclose`, focus `yards`), and in the
    medium plant the RES breakers shedding, both diesels starting by themselves and taking their buses, RES XFMR A
    refused onto the dead grid and then closing to take diesel A off its bus (`mediumPlantLosesOffsitePower`). The
    core cases were also run in the sandbox (the core alone), with every older core-only test.
  - equipment placing and breaking whole;
  - a unit joining through coolant pipe;
  - console permission and refuel rules;
  - the Geiger sound following the rate;
  - a whole showcase unit forming four loops and making power;
  - written, not yet run: the plant blueprints load whole, the EPR and module cores and cooling towers behave, and
    a built small modular plant forms six units with both tower banks (`NuclearPlantGameTests`); a screenshot tour of
    all three plants running (`NuclearClientGameTests`);
  - written, not yet run (older rooms round 3): the medium plant's DC rooms work: each charger, inverter and
    distribution panel finds its train's switchboard; a feeder opened puts its inverter on the bypass, kills its
    panel, takes the load centre's control power or the diesel's start, and closed brings it back; a charger's
    breakers stop it, its equalize runs out after 24 game hours; the annunciators flash, acknowledge, reset and test;
    the laptop writes the room and the feeders; (older rooms round 4) there are four string disconnects, one
    opened takes its string's two racks off the bus, both take the battery off, and closed bring them back; a rack's
    cell numbers run 1, 15, 16, 60 where they should (`NuclearPlantGameTests.mediumPlantDcRoomsWork`);
  - written, not yet run (older rooms round 5): the medium plant's control room has its three operators' consoles,
    one of each position, and the shift manager's desk, each linking the display wall; a click steps only its screen
    and the pages go round; every page works out its lines (`NuclearPlantGameTests.mediumPlantControlRoomConsoles`);
  - written, not yet run (older rooms round 6): the medium plant's entrance has its security post and hand reader; a
    gate opening writes IN through the entry lane and OUT through the exit to the post, and who is inside follows
    (`NuclearPlantGameTests.mediumPlantEntranceSecurityPost`);
  - written, not yet run (older rooms round 7): the medium plant's remote shutdown room has its station, key cabinet,
    communications station and sign; the station links the remote shutdown panel and its AOP-8 page shows the trip and
    the transfer done once the plant has them (`NuclearPlantGameTests.mediumPlantRemoteShutdownStation`);
  - written, not yet run: the medium plant's security gatehouse has its console, X-ray and metal detector; the detector
    alarms on an iron sword and passes bread, the X-ray sends the sword to be searched, the console links the
    gatehouse's post and a zombie to its north puts zone 1 in alarm (`NuclearPlantGameTests.mediumPlantGatehouseSearchTrain`);
  - written, not yet run: the medium plant's administration building has an EXIT sign and a pull station at the stairs
    on each floor and at the front doors and each floor's evacuation plan, and an office computer's screen reads the
    plant's reactor (`NuclearPlantGameTests.mediumPlantAdminBuildingSafety`);
  - written, not yet run: the medium plant's administration building part 2: the chemistry board works out its eleven
    results and links the gamma station, which counts a sample; eight carriages, two plan files, the boardroom table; a
    carriage's handwheel rolls it into the aisle (`NuclearPlantGameTests.mediumPlantAdminBuildingPart2`);
  - written, not yet run: the operations office's board lists an opened inverter feeder (clearance 1041, its LCO) and
    a fan turned off, drops the feeder once it is closed, and the shift schedule puts one crew on days and one on
    nights each day (`NuclearPlantGameTests.mediumPlantOpsOfficeTracksEquipmentOut`), with five screenshots of the
    office (`NuclearClientGameTests`);
  - written, not yet run: the restroom's fixtures are all there as built, a toilet gives a roll and flushes, a sink
    runs, the trash can throws away what is put in it and fills, a stall door latches from inside, holds against
    outside and opens from inside, and unlatches with nobody in the stall (`mediumPlantRestroomWorks`), with five
    screenshots (`NuclearClientGameTests`).
  - written, not yet run: the live boards' plant model (a coolant pump stopped at power trips the reactor on low flow
    in one loop and its flow coasts down; below P-8 one loop out does not trip, two above P-7 do; a turbine trip above
    P-9 trips the reactor, below it does not, the turbine coasts down, latches, runs up and its breaker closes through
    the synchroscope) and the annunciator's sequence (`NuclearGameTests`); in the medium plant the three boards find the
    reactor, diesels, load centre and motor control centres, RCP 1C stops and starts, the turbine trips through its
    guard and relatches, the generator breaker won't close without steam, and an alarm runs ACKNOWLEDGE and RESET
    (`NuclearPlantGameTests.mediumPlantBoardsLive`), with nine screenshots, three of them tripped
    (`NuclearClientGameTests`).
  - written, not yet run (punch list #15): a fire under a heat detector puts the releasing panel in alarm (the board's
    FIRE CABLE SPREADING), it releases after its delay, the head over the fire opens and puts it out (FIRE PUMP
    RUNNING), RESET waits for the water to be shut off, then re-arms it; the pull station releases by hand
    (`NuclearPlantGameTests.spreadingRoomSprinklers`).
  - written, not yet run (round 34): the medium plant's three overview screens each find the control room's boards and
    the reactor, each with its raceway under it; a click on the diesel building shows its page and SITE MAP goes back;
    a reactor trip turns the containment red and puts the reactor's page up by itself
    (`NuclearPlantGameTests.mediumPlantOverviewScreens`).
  - written, not yet run (round 32): the plant display wall finds the reactor and shows the reactor coolant board's
    protection bypass alarm on its banner, new, and its mimic and safety functions read the plant
    (`NuclearPlantGameTests.mediumPlantBoardsLive`).
  - written, not yet run (round 28): a console-only plant's steam generators hold their level through power, a trip
    and a cooldown; safety injection trips the reactor, isolates feed and the containment and starts aux feed, which
    holds the levels, resets after a minute and holds the reactor's reset until then (`safetyInjectionFullActuation`);
    one MSIV shut gives three quarters of the power, four trip the turbine and the reactor, and feed lost at power trips
    on low-low level and starts aux feed (`msivsAndAuxFeed`); in the medium plant the six boards are found, every
    radiation monitor finds its spot and reads quiet, both boards find the HVAC panel, MSIV 1 and aux feed pump A work,
    and SI actuates through its guard and resets a minute on (`mediumPlantBoardsLive`); nine screenshots, three after
    a safety injection with the MSIVs shut and damaged fuel (`NuclearClientGameTests`).
  - written, not yet run (round 29): a shut aux feed valve starves its generator while the others hold, two ARVs open
    by hand cool the plant about 60 °C and it warms back when they close (`auxFeedValvesAndArvs`); in the medium
    plant the remote shutdown panel refuses aux feed until a transfer key turns, then starts it while the steam and
    feedwater board is locked out, works an FCV and an ARV, and hands control back (`mediumPlantBoardsLive`).
  - written, not yet run (round 30): the shutdown banks fall with a trip and come out within five seconds of the reset;
    the lever held OUT drives bank A out first and B after it in the overlap and the rods stop when it is let go; the
    instruments climb past P-6 and P-10 with a high startup rate; after a trip they drop to about 6% and fall a third of
    a decade a minute, the IR and PR blocks clearing below P-10 and the SR block staying; AUTO steps the rods out to
    hold its power as a quarter of the core is used up and drops to MAN on a trip; a startup from the screen blocks the
    trips itself (`rodControlAndInstruments`, also run outside the game on its own `ReactorCore`); in the medium plant
    the console finds its reactor, trips through its guard and resets with CLOSE, refuses the lever while tripped and in
    AUTO, drives the rods in MAN, refuses the SR block below P-6, bypasses and restores protection, and with the remote
    panel in control only trips (`mediumPlantBoardsLive`); two screenshots of the console and two after the safety
    injection (`NuclearClientGameTests`).
  - written, not yet run (round 31): the set's steps stand on half-block frames on both sides and it starts and stops
    (`stepsWalkAndItStartsAndStops`); with two receivers, two compressors and a dryer by it, each start takes 20 psig,
    five starts and the sixth fails, both compressors refill the receivers and stop at 250, the lead alone runs
    between 200 and 225, and with its air back it starts (`startingAirStartsAndRefills`; its arithmetic also run
    outside the game, `scratchpad/simcheck/AirCheck.java`); in the medium plant each generator takes its own room's
    five pieces labelled for its train, a start takes 20 psig, its steps end in half blocks and each room's engine
    panel links to its own generator (`mediumPlantDieselRoomsStartOnAir`); (round 31 part 2) MODE decides who starts
    it, its breaker closes itself 5 s in, trips and closes again, a relay test trips the 86G and locks it out until
    reset, and the emergency stop stops it and latches (`panelControlsModeBreakerAndLockout`).
  - Round 31 part 3: a room's own plant joins its generator; the CO2 countdown runs (ABORT stops it), the discharge
    shuts the engine down and keeps it from starting, a pig in the room chokes, RESET purges and all is normal again;
    the hoist steps a block a click (`roomPlantCo2AndHoist`); each pre-made room's eight pieces join its generator.
  - written, not yet run (turbine hall part 1): the manual trip trips a latched turbine and opens its breaker (once);
    tripped below P-9 it coasts to rest in about two minutes, its turning gear refusing to go in until then; hot and
    standing still its rotor bows (ECCENTRICITY HI); the gear goes in, rolls it at 3 rpm and the alarm clears;
    relatched, steam rolls it off the gear, which drops out (`turningGearTripAndEccentricity`; also run outside the
    game, `scratchpad/bootcheck/TurbineCheck.java`, all passing); placed, the turbine takes its body's cells and finds
    its trip handle and lever where they are drawn (`turbinePlacesAndFindsItsControls`).
  - written, not yet run (turbine hall part 2): without its pumps at the hall a plant feeds as before; with them two
    condensate pumps and both MFPs carry full power; a condensate pump stopped at full power brings its standby in on
    AUTO (COND PUMP AUTO START), STOP again takes it out of AUTO and START runs it (`feedTrainPumpsAndPlant`); an MFP
    tripped at full power with the rods in AUTO runs the plant back to 55% without a trip, MFP A TRIP in, and it resets
    (`mfpTripRunsBackInAuto`); in manual the levels fall to the low-low trip in about 47 seconds and the auxiliary feed
    pumps start, both MFPs lost start them at once (`mfpTripInManualTripsOnLowLevel`); placed, the pumps find their
    START, STOP, TRIP and RESET where they are drawn and relabel (`feedPumpsFindTheirButtons`). The plant's arithmetic
    is also run outside the game (`scratchpad/bootcheck/FeedCheck.java`, all passing).
  - written, not yet run (turbine hall part 3): the crane runs from its pendant while a button is held and stops when
    let go, its bridge to its runway's ends, its trolley across, its hook down to the floor; STOP takes its power off until
    START (`craneRunsFromItsPendant`); a turbine control room cabinet joins its turbine and reads it as the boards do,
    the 86G locks out after a hot trip and the rotor bows standing (`turbineControlRoomReadsItsTurbine`; also run outside
    the game, `scratchpad/bootcheck/Part3Check.java`, all passing).
- **Screenshots** of every piece, the console screen (running and tripped), the open vessel and the fuel machines.

## World and event applicability
- **Placement:** player-built industry only. Nothing generates in the world.
- **Radiation:** from what players make and from natural uranium and thorium ore (a counter finds it underground).

## Rollout and open questions
- **Placement:** equipment bigger than 5x5 is placed from one item in this stage. The proposal's drone-built
  equipment blueprints come with the pre-made plants.
- **Stage 2 and later:** the spent fuel pool, dry casks and hardened drones; cooling towers and the water intake port;
  HALEU and MOX; meltdown, blast, fallout and corium; the small modular and EPR plants.
- **Commands:** `/nuclear plant small|medium|large [fuelled]` builds that pre-made plant as it is now just in front of
  you (turned the way you face, its near side four blocks ahead, centred on you, its ground at your feet); anyone in
  creative mode can use it, and operators in any mode. Operators only: `/nuclear showcase` builds a running unit;
  `/nuclear build <blueprint> [fuelled]` builds any blueprint at once where you stand; `/radiation dose|check` are for
  testing.
- **Not yet:** the plants' transformer yards and switchyards are scenery: connect the main transformers (or the small
  turbines) to power lines yourself. Basins are dry.
