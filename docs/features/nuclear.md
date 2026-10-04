# Nuclear power (stage 1)

Status: implemented on the drone-tower branch (stage 1 of the approved plan). It has passed focused game tests on the
owner's PC and is waiting for his review in game. Not merged.
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
  - a whole showcase unit forming four loops and making power.
- **Screenshots** of every piece, the console screen (running and tripped), the open vessel and the fuel machines.

## World and event applicability
- **Placement:** player-built industry only. Nothing generates in the world.
- **Radiation:** only from items and sources players make.

## Rollout and open questions
- **Placement:** equipment bigger than 5x5 is placed from one item in this stage. The proposal's drone-built
  equipment blueprints come with the pre-made plants.
- **Stage 2 and later:** the spent fuel pool, dry casks and hardened drones; cooling towers and the water intake port;
  HALEU and MOX; meltdown, blast, fallout and corium; the small modular and EPR plants.
- **Operator commands:** `/nuclear showcase` builds a running unit; `/radiation dose|check` are for testing.
