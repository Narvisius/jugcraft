# Machine roadmap

Status: **proposals for discussion, not approved or implemented.** Every entry here needs its own issue and review. The implemented machines are listed in [machines-and-power.md](features/machines-and-power.md).

## What exists now

| Role | Implemented |
| --- | --- |
| Generation | Coal Generator (32 JE/t), Steam Generator (64 JE/t, needs water, burns coal or bitumen), Solar Panel (8 JE/t in daylight) |
| Storage and transport | Battery Box (400k JE), Copper Cable (256 JE/t) |
| Processing | Electric Furnace, Crusher (ore doubling), Arc Furnace multiblock, Alloy Smelter |

## Design rules for new machines
1. **Each machine is a consumer for something that already exists.** Name the materials it uses, and what uses its output.
2. **Shared systems come before the machines that need them.** Fluids and item logistics each get one interface, like the energy API, instead of every machine inventing its own.
3. **No new free resources.** Any yield bonus is defined once and audited by `tools/check_mod_data.py`, the way ore doubling is today.
4. **Early machines stay useful.** Later tiers are faster or more efficient, but earlier routes stay valid and the IDs never change.
5. **Keep the server cheap.** No per-tick world scans, bounded networks, and idle machines do almost nothing.

## Shared systems needed next
These unlock whole groups of machines, so they should be designed first.

| System | Why | Unlocks |
| --- | --- | --- |
| **Fluid API and pipes/tanks** (transactional, like energy) | Crude oil, steam, acids, water and lava need to move | Refinery, chemical reactor, pump, geothermal, fluid steam input |
| **Item pipes / conveyor** (optional; hoppers work today) | Factories larger than hopper chains | Automated multi-machine lines |
| **Machine upgrades** (speed, efficiency, energy) | Progression without a new block each time | A use for rare earths, silver and lithium |
| **Energy tiers** (low / medium / high voltage, transformers) | Stops one cable type carrying unlimited power; gives aluminum and silver cable a purpose | High-demand machines, the reactor |
| **Ownership and access** | Multiplayer servers | Every machine |

## Proposed machines by tier

### Tier 1: Workshops (current power levels)
| Machine | Does | Uses | Feeds |
| --- | --- | --- | --- |
| **Alloy Smelter** ✅ implemented | Two-input alloying: bronze, brass (copper + zinc), solder (tin + lead), invar (iron + nickel) | Existing metals | Machine casings, circuits, magic instruments (bell bronze, brass astrolabes) |
| **Metal Press** | Ingots → plates; plates → gears | All metals | Casings, rocket hulls, better machine recipes |
| **Wire Drawer** | Ingots → wire (copper, silver, aluminum) | Copper, silver, aluminum | Cables, circuits, motors |
| **Water Pump** | Moves water into tanks and the steam generator | Fluid API | Steam, agriculture (irrigation), chemistry |

### Tier 2: Specialization
| Machine | Does | Uses | Feeds |
| --- | --- | --- | --- |
| **Electrolytic Cell** | Brine → lye + chlorine; alumina → aluminum (replaces the arc furnace shortcut) | Salt, bauxite, power | Aluminum, soap and glass chemistry, bleach, plastics |
| **Chemical Reactor** | Sulfur + water → sulfuric acid; phosphate + acid → fertilizer; lithium carbonate → battery compounds | Sulfur, phosphate, lithium | Farming (fertilizer), batteries, ore leaching |
| **Circuit Assembler** | Silicon + silver/copper wire + solder → circuits (basic, advanced) | Silicon, silver, solder | Every higher-tier machine, rockets, magic-tech bridges |
| **Lithium Battery** | Small high-density battery block and a portable battery item | Lithium, aluminum | Portable tools, rovers, space |
| **Geothermal Generator** | Lava → power | Fluid API | Nether and cave bases |
| **Wind Turbine** (multiblock) | Height- and weather-scaled power | Aluminum, bronze gears | Remote and cozy off-grid bases |

### Tier 3: Industry and expeditions
| Machine | Does | Uses | Feeds |
| --- | --- | --- | --- |
| **Oil Pump and Refinery** (multiblock) | Crude oil → fuel, lubricant, plastic, asphalt; adds liquid crude oil | Fluid API, oil deposits | Rocket fuel, plastics, roads; lubricant must stay compatible with the planned oilseed farming route |
| **Combustion Generator** | Burns refined fuel for high output | Refinery | High-demand factories |
| **Rare Earth Separator** | Rare earth oxide → neodymium and friends | Monazite, acids | Magnets (motor/generator upgrades), lasers, phosphors |
| **Precision Fabricator** | Rocket parts, navigation, habitat modules | Circuits, plates, aluminum, tungsten | The rocketry and space pillar |
| **Automated Farm Machines** (harvester, planter, fertilizer spreader) | Bounded-area crop work | Fertilizer, power | The agriculture pillar |

### Tier 4: Shared wonders
| Machine | Does | Uses | Feeds |
| --- | --- | --- | --- |
| **Fission Reactor** (multiblock) | Uranium → very high power | Uranium, lead shielding, tungsten, circuits. **Needs its own hazard design first** | Late-game factories, space launch |
| **Launch Pad** (multiblock) | Rocket launches | Fuel, fabricated parts | Moons and planets |

## Connections to magic and other specialties
Each connection below works both ways, but no machine requires magic to work.
- **Magic → technology:**
  - Workshop mages could inscribe machine upgrades, for example an efficiency rune costing essence instead of rare earths.
  - A dark-magic path could supply the same parts through rituals: Blood rituals producing catalysts, Necromancy supplying bounded labor.
- **Technology → magic:**
  - The alloy smelter makes bell bronze and brass for magical instruments.
  - The crusher and chemical reactor prepare reagents: salt for wards, phosphorescent inks.
- **Power ↔ essence:** any converter between power and magical essence must follow the design's rules. It's one-directional or lossy, with defined units and caps, so neither economy can print the other.
- **Agriculture:** fertilizer (phosphate + sulfuric acid), irrigation pumps and harvester machines. Farmers supply bio-lubricant and plant oils that later compete with the refinery.
- **Space:** the precision fabricator, refinery, lithium batteries and solar panels all feed rocketry and off-world bases.

## Suggested next steps, in order
1. ~~**Alloy Smelter**~~ Done: bronze, brass, invar and solder.
2. **Fluid API, with Water Pump and tanks:** the foundation for steam, chemistry and oil.
3. **Electrolytic Cell and Chemical Reactor:** replace the remaining stand-ins; give salt, sulfur and phosphate real uses.
4. **Metal Press, Wire Drawer and Circuit Assembler:** the parts economy for everything above tier 2.
5. **Energy tiers and upgrades:** once demand justifies them.
6. **Oil and refinery, and farm machines:** each tied to its pillar's own proposals.

Each step should be one issue and one PR, with the checker extended to audit the new recipes.
