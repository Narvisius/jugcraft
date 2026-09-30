# Changelog

Every change to Jugcraft, newest first, grouped by pull request. For what each thing does in game, see [docs/TECH_TREE.md](docs/TECH_TREE.md). For a full inventory with APIs and file locations, see [docs/WHAT_EXISTS.md](docs/WHAT_EXISTS.md).

**Testing so far:** everything compiles in CI, and the in-game tests (from #14 on) pass on a headless server. Nothing has been play-tested in a client or with two players on a dedicated server yet.

## Unreleased

No numbered release yet. Everything below is on `main`.

### #37 Spinning shafts and closer screenshots
- Shafts, belt pulleys, the hand crank, the electric motor's shaft and the steam engine's flywheel now really spin (a block entity renderer) instead of scrolling a texture. Shafts placed with earlier builds need re-placing to spin.
- The client test photographs a belt-and-motor line and the multi-blocks from closer, in three views.

### #36 Belts and the Electric Motor
- **Belt Pulley** and **Leather Belt:** link two pulleys up to 16 blocks apart to carry rotation; the belt is drawn between them.
- **Electric Motor:** JE → KE at 75%, up to 96 KE/t.
- Three game tests.

### #35 Bigger machines, a spinning wind turbine, the Large Steam Engine and JEI
- Machines can now fill up to 64 blocks. Resized:
  - **Alloy Smelter:** 3×2×6, with a big copper crucible tank pouring into one funnel over the furnace.
  - **Geothermal Generator:** 2×2×2.
  - **Steel Foundry:** 2×2×5.
  - **Coke Oven:** 2×2×2, with its chimney in a block on top.
  - **Wind Turbine:** 9 tall, with a 7-block rotor that spins (block entity renderer); 12–72 JE/t.
- **Large Steam Engine** (2×2×2): 256 KE/t, four times the small one.
- Machine screens: amber energy readout without a shadow; vanilla tooltips on gauges.
- **JEI:** a recipe page per machine (optional; EMI has no 26.3 build yet).
- Multi-blocks placed with earlier builds need re-placing.

### #32 Polish
- Machines with a fire, and the steam engine, smoke and crackle while running.
- Hovering the energy bar or a tank gauge shows exact JE or mB.
- The eject button reads "Eject" (green on, gray off) instead of a cut-off "Eject: off".
- The CI screenshots no longer show the chat log.

### #30 Auto-Crafter
- **Auto-Crafter:** crafts any crafting-table recipe laid out in its 3×3 grid, one every 2 seconds.
  - Each grid slot keeps one item as the pattern, and pipes and hoppers only top up matching slots.
  - Remainders such as empty bottles get their own slot.
- A powered processor with upgrades, sides, eject, redstone and kinetic power; a new grid layout on its screen.
- Three game tests and a client screenshot of its screen.

### #29 Kinetic power
- A mechanical power layer in **KE** per tick. **Iron Shafts** carry it along their axis and **Brass Gearboxes** out of all six sides; both animate while turning.
- Sources:
  - **Hand Crank:** 16 KE/t while cranked.
  - **Steam Engine:** 64 KE/t from fuel and water, burning only while something takes the power.
- Every powered machine runs straight off a shaft (1 KE = 1 JE). The **Dynamo** bridges KE into JE cables at 75%.
- Three game tests, a client screenshot of a running line, and handbook pages under Power.

### #28 Renewable resources
- **Water Wheel** (2 tall): up to 24 JE/t from flowing or falling water beside its wheel, with no fuel.
- **Cobblestone Generator:** one cobblestone a second from water and lava touching it; neither is used up.
- **Tree Farm:** grows a sapling into six logs and gives the sapling back, sometimes with an extra (apple, cocoa beans, …). Recipes are data for all nine vanilla trees.
- Steampunk models (timber water wheel with a coil dynamo, cistern-and-crucible generator, open brass growth cabinet with a grow lamp) and classic models.
- The handbook gains a "Renewables" chapter; its chapter buttons are packed tighter to fit 11 chapters.
- Three game tests.

### #25 Mining and prospecting
- **Geo-Resonance Prospector:** a hand tool that surveys the 3×3 chunks around you. It opens a steampunk-digital screen: a brass instrument with an amber CRT, valve-tube signal bars, a sweeping scan line and a resonance needle gauge.
  - Readings are deliberately vague: 1–5 bars and shallow, middle or deep for each ore family, never a chunk or block.
- **Ore Drill** (2-tall derrick): mines every ore in a 9×9 column below it, down to the bottom of the world, and refills the holes with rock. It gives whole ore blocks, so ore processing still decides the yield. It has upgrades, side configuration, eject and redstone control.
- Two game tests, a prospector screenshot in the client test, and handbook pages under Materials.

### #24 Storage
- **Capacitor Bank** (2×2): 4,000,000 JE. It charges from any side and gives power out of its front sockets at 4,096 JE/t.
- **Steel Tank** (2×2 squat riveted tank): 128 buckets.
- **Item Crate:** 32 stacks of one item, with right-click in and out and support for pipes, hoppers and comparators.
- Steampunk models (Leyden-jar bank, domed tank, banded crate) and classic models.
- Three game tests, a handbook "Storage" chapter, and a feature record.

### #22 Transmitter tiers
- **Silver Cable** (1,024 JE/t) and **Aluminum Cable** (4,096 JE/t). All cable tiers join one network, which runs at its slowest cable.
- **High-Pressure Extractor** (steel): 32 items every 4 ticks, four times the brass extractor.
- There is no faster fluid pipe: pumps (100 mB/t) are the limit, not pipes.
- Two game tests; handbook pages.

### #20 Engineer's Handbook and in-game screenshots
- **Engineer's Handbook** (book + copper ingot): an in-game guide with 9 chapters and 36 pages. Each page gives what a block does, its power use, its crafting grid and example recipes, and you can hover over items.
- The content is generated from the mod's own tables, so it can't go out of date.
- **Client game tests:** CI starts a real game client, builds a showroom of every machine, opens a machine screen and the handbook, and saves screenshots as a build artifact.

### #19 Machine control
- **Upgrades:** every powered processing machine gets two upgrade slots.
  - **Speed Upgrade:** 4 cards make it 3× as fast for twice the energy per item.
  - **Efficiency Upgrade:** 4 cards bring it to 41% of the energy.
  - Both are made from steel.
- **Redstone mode button:** ignored, run with a signal, or run without one.
- **Comparators** read stored energy (generators, battery box) or how full a machine is, from any block of a multi-block.
- Energy readouts are shortened (for example "12.5k / 20k JE") to fit the new slots.
- Four new game tests.

### #18 Steel tier
- **Coke Oven** (2 tall) bakes coal into **Coal Coke**. Coke is a 3,200-tick generator fuel and the carbon for steel.
- **Steel Foundry** (3 tall) turns 1 iron ingot + 1 coke into 1 **steel ingot**.
- Both are unpowered brick multi-blocks with steampunk and classic models.
- New steel items: ingot, nugget, block, plate and gear.
- **Fix:** cables drew a connection arm to every face of a machine, even where no power goes in (for example all around the alloy smelter). Now the arm and the energy flow use the same check.
- Three new game tests, including one for the cable fix.

### Merged 30 September 2026: PRs #8–#17

These were built as a stack, each on the one before, and merged in order (#8 first).

#### #17 Changelog and "What Exists" guide
- Adds this `CHANGELOG.md` and [`docs/WHAT_EXISTS.md`](docs/WHAT_EXISTS.md). WHAT_EXISTS is a map of all content, shared APIs, data formats, file locations and check rules, so contributors and AI agents can build alongside the existing systems.
- Adds feature records for [item logistics](docs/features/item-logistics.md) and [ore processing](docs/features/ore-processing.md).

#### #16 Ore processing depth
- **New machines:**
  - **Pulverizer:** ore → 2 dust, plus a byproduct roll.
  - **Ore Washer:** ore + water → 3 washed ore, which the pulverizer grinds.
  - **Sieve:** gravel → flint; soul sand → soul soil; both with small finds.
  - **Sawmill:** log → 6 planks + sawdust; planks → 3 sticks.
- **Three ore routes:** smelt (×1), crush or pulverize (×2), wash then pulverize (×3).
- **New items:**
  - 10 metal dusts (`c:dusts/<metal>`), which smelt into ingots. Nickel, tungsten and uranium dust use the arc furnace.
  - 10 washed ores.
  - Sawdust (4 → paper).
- **Byproducts:**
  - Machine recipes can list byproducts: chance, count, and an optional feature switch.
  - The pulverizer, sieve and sawmill have two byproduct slots, and a machine waits rather than lose a byproduct.
  - Byproduct pairs follow real ores, for example copper → gold and lead → silver.
- **Looks:** steampunk models (ball mill, washing vat with a water wheel, shaker sieve, sawbench) and classic textures.
- **Balance checks:** only ores get a bonus (×3 at most); byproducts add at most 25% of the input's metal; renewable sieve finds average under a nugget per operation.
- Seven new game tests.

#### #15 Item logistics
- **Side configuration:** every processing machine's screen has six face buttons (front, back, left, right, top, bottom). Each cycles between In, Out, Both and Off. The defaults keep the old hopper behavior.
- **Eject:** when on, the machine pushes its results out of its Out faces, 16 items every 8 ticks.
- **New blocks and items:**
  - **Brass Item Pipe:** 6 px. Items go first to matching sorters, then round-robin to the other inventories.
  - **Pneumatic Extractor:** 16 items every 8 ticks; a redstone signal pauses it.
  - **Item Sorter:** 9-slot filter.
  - **Brass Wrench:** turns machines; sneak to dismantle.
- Three new game tests.
- Fix: a multi-block machine's eject never feeds back into its own other blocks.

#### #14 Machine recipes as data, and in-game tests
- Machine recipes are now real Minecraft recipe types (`jugcraft:crushing`, `alloying`, …), one JSON file each under `data/<ns>/recipe/<type>/`. Data packs can add, change or remove them, and ingredients may be tags.
- In-game tests (Fabric game test API) run in CI with `./gradlew build`.
- **Launch-blocking fixes found by those tests** (before this PR, `main` could not start a server):
  - Fabric Loader 0.18.4 → **0.19.3**, which Fabric API 0.161.0+26.3 requires.
  - Ore worldgen moved to the 26.x format: `worldgen/feature/`, no `config` wrapper, block states as plain IDs.

#### #13 Steampunk machines
- Every machine redrawn as a detailed steampunk model: brass, copper, riveted iron, gauges, gears, valve wheels, and fireboxes that glow while running. Includes 37 original textures.
- The previous look stays available as the built-in resource pack **Jugcraft: Classic Machines**, so the change can be reverted.

#### #12 2×2 alloy smelter and power sockets
- The alloy smelter becomes a 2×2 multi-block with a visible copper **power socket**, the only place cables connect to it.
- Clear power-connection rules for every machine: each one either has a socket or takes power on any face that touches a cable.

#### #11 Multi-block machines
- Machines can occupy several blocks and are placed as one item, in the style of Immersive Engineering. Breaking any part removes the whole machine.
- **Geothermal Generator** (2 wide, lava → 64 JE/t) and **Wind Turbine** (3 tall, 4–24 JE/t by height and weather).

#### #10 Fluids, and tech-mod style cables and pipes
- **Bronze Fluid Pipe**, **Tinplate Tank** (16 buckets) and **Electric Pump**.
- Cables and pipes are thin (4 px) and connect to each other and to anything with energy or fluid storage, as in other tech mods. Items show as 3D segments.

#### #9 Parts and circuits
- **Metal Press** (ingot → plate), **Wire Drawer** (ingot → 3 wires), **Circuit Assembler** (basic and advanced circuits).
- Gears are crafted from four plates.

#### #8 Alloy smelter
- **Alloy Smelter** makes bronze, **brass**, **invar** and **solder** at metal-conserving ratios.

### Merged earlier on 30 September 2026

#### #7 Solar panel and steam generator
- **Solar Panel** (8 JE/t in sun) and **Steam Generator** (coal or bitumen plus water → 64 JE/t), plus a machine roadmap.

#### #6 Electricity and the first machines
- Jugcraft Energy (JE) with a shared `EnergyStorage` API, **Copper Cable** networks, **Coal Generator**, **Battery Box**, **Electric Furnace**, **Crusher** (ore doubling) and the **Arc Furnace** 3×3×3 multiblock.

#### #5 Base materials
- Zinc, lead, silver, nickel, tungsten, uranium, aluminum (bauxite), salt, phosphate, lithium (lepidolite), rare earths (monazite), sulfur, silicon and oil sand (bitumen).
- Feature switches in `config/jugcraft.properties`.

#### #4 Fabric 26.3 scaffold, tin and bronze
- Pinned toolchain and the CI Build workflow.
- Tin ore, raw tin, ingots and blocks, and bronze from a 3:1 copper/tin blend.
- The offline data generator and checker (`tools/`).

#### #1 Vision and development setup
- Project vision, contribution rules, design and architecture documents, and owner-directed Fabric development.
