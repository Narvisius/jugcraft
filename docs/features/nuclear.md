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
- **Radiation:** sources fall off with distance (strength / (1 + d²)) and with blocks in the way:
  - shielding concrete and lead blocks let 5% through, water 50% and other solid blocks 60%.
  - The four-piece hazmat suit stops 90% of nuclear radiation (22.5% a piece). It is drawn in 3D: visor, respirator
    filters, an air tank on the back with its hose into the hood, harness, gloves and boots.
  - Dose builds up and slowly recovers. 500 mSv brings weakness, 1,000 nausea and hunger, 2,000 damage, and more
    above 4,000 and 8,000.
  - The **Geiger counter** clicks at random, as real counts arrive, generated live from the server's dose rate with no
    recorded sound. A readout shows the rate and dose while you hold it or wear the hood or coverall.

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
- **Radiation:** only from items and sources players make.

## Rollout and open questions
- **Placement:** equipment bigger than 5x5 is placed from one item in this stage. The proposal's drone-built
  equipment blueprints come with the pre-made plants.
- **Stage 2 and later:** the spent fuel pool, dry casks and hardened drones; cooling towers and the water intake port;
  HALEU and MOX; meltdown, blast, fallout and corium; the small modular and EPR plants.
- **Operator commands:** `/nuclear showcase` builds a running unit; `/nuclear build <blueprint> [fuelled]` builds any
  blueprint at once; `/radiation dose|check` are for testing.
- **Not yet:** the plants' transformer yards and switchyards are scenery: connect the main transformers (or the small
  turbines) to power lines yourself. Basins and the reactor pool are dry.
