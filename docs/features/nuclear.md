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
  body. Breaking any part breaks the whole piece and drops its item.

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
  | Reactor Control Console | 3x2x2 |
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
  - Link it with a **Reactor Link Card**: use the card on the vessel, then on the console.
  - Its screen follows the approved design: core map (193 assemblies, shaded by heat, with rod clusters), rod slider,
    plant readouts, a 10-minute core-temperature trend with the alarm and SCRAM lines, annunciator windows, the
    protection switch, the SCRAM button, RESET TRIP, a Personal/Party switch, and the fuel line with REFUEL.
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
- **Building:** drones build from the lowest unfinished layer up a few layers at a time (see the blueprint system
  doc). In creative, `/nuclear build <blueprint> [fuelled]` builds one at once.
- **Checked before building:** `tools/nuclear_plants.py` follows the pipes the way `ReactorUnit` does and refuses to
  write a plant whose units would not form (loops, turbines, condensers, transformers, and the conduit reaching both
  the lake and the towers). It also re-reads the written blueprints the way the game does.

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
