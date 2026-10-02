# Changelog

Every change to Jugcraft, newest first, grouped by pull request. For what each thing does in game, see [docs/TECH_TREE.md](docs/TECH_TREE.md). For a full inventory with APIs and file locations, see [docs/WHAT_EXISTS.md](docs/WHAT_EXISTS.md).

**Testing so far:** everything compiles in CI, and the in-game tests (from #14 on) pass on a headless server. Nothing has been play-tested in a client or with two players on a dedicated server yet.

## Unreleased

No numbered release yet. Everything below is on `main`.

### Unmerged: Turbocharger and flywheel, batch 19
- **Turbocharger** in the advanced engine's new slot, with coolant water in its new second tank: up to 1,536 KE/t and 10% more KE from each mB of fuel.
- **Flywheel:** stores up to 2,000,000 KE of rotation and drives its front shaft from it; friction runs it down slowly.

### #74 Explosive weapons, batch 18
- **Guncotton** (2 cotton + 250 mB nitric acid, chemical reactor).
- **Grenades**, thrown by hand, and the **grenade launcher**, which fires them further. The blast hurts living things only: up to 16 damage, walls shield, and no block, armor stand, frame or dropped item is ever touched.
- New switch `explosives.enabled`. An advancement, a handbook page and a game test.

### #73 Flow batteries, batch 17
- **Vanadium electrolyte:** two asphalt binder and a bucket of sulfuric acid in the chemical reactor.
- **Flow battery** (3×3×2): 1,000 JE per mB of electrolyte in it, up to 64,000,000 JE with 64 buckets; 8,192 JE/t in and out. Keeps its electrolyte when broken.
- An advancement, a handbook page and a game test.

### #72 Glass chemistry, batch 16
- **Tincal**, natural borax, in desert and badlands sand; **borax**.
- **Borosilicate glass** (2 sand + borax, alloy smelter) drawn into **optical fibre**, which can replace gold in processors.
- **Ferroboron** (iron + borax): with a rare earth oxide it makes **two** neodymium magnets.
- Multi-input recipes now try the one with the most ingredients first.

### #70 Chlorine and lye, batch 15
- **PVC:** refinery gas + chlorine → vinyl chloride (synthesis converter) → PVC resin (polymerization reactor) → two plastic sheets each (metal press).
- **Soap** from lye and rotten flesh; a bar washes off every status effect.
- Two advancements, a handbook page and game tests.

### #69 Rubber and polymers, batch 14
- **Butadiene** from naphtha (chemical reactor) and **synthetic rubber** from butadiene (polymerization reactor).
- **Gaskets** (rubber + steel plate); rubber belts; gasketed steel pipe, four for two plates.
- An advancement, a handbook page and a game test.

### #68 Oxygen-blown steel and argon, batch 13
- The air separation unit also makes **argon**.
- **Boost gases:** oxygen piped into the steel foundry, or argon into the crystal grower, doubles its speed.

### #65 Nitrogen chemistry, batch 12: air separation, ammonia and nitric acid
- **Air separation unit** (2×2, six tall): splits air into nitrogen and oxygen, four to one, needing only power.
- **Synthesis converter** (3×4×2): Haber–Bosch ammonia (hydrogen + nitrogen) and Ostwald nitric acid (ammonia + oxygen + water).
- New gases nitrogen, oxygen and ammonia; nitric acid with a bucket.
- Ammonia + phosphate → 6 fertilizer; nitric acid etches microchips with half the acid.
- Three advancements, a handbook section and game tests.

### #62 Surface deposits, batch 11
- **Coal, Iron, Copper and Tin Deposits:** flat patches in the top layer of stony hills (windswept hills, stony peaks, stony shores). Picks only break them, for nothing; each block holds 1,000 units.
- **Deposit drill** (3×3, two tall): takes one coal or raw ore of each kind every 15 seconds from the deposits under it and one block round it, and pushes them into a chest, pipe, conveyor or machine beside it. Empty deposit blocks turn to stone.
- `deposits.enabled` switch, an advancement, a handbook page and game tests.

### #60 Advanced power, batch 10: big solar, a four-cylinder engine and tanks that keep their fluid
- **Advanced solar panel:** a white pedestal carrying a 3×3 array of cells, 64 JE/t in full sun (eight solar panels).
- **Advanced combustion engine** (2 long): gasoline or diesel → up to 1,024 KE/t on a shaft; through a magnet dynamo, the best JE per mB of either fuel.
- **Tanks** have a new look (white with checker bands) and **keep their fluid when broken**: the item carries the fluid and amount, shown in its tooltip.
- **Fix:** loot tables now use the Minecraft 26.x format; the old keys were silently ignored, so ore drop counts, the charging station's upper half and slab doubles were wrong. A data check and a game test guard it.
- Two advancements, handbook pages and game tests.

### #58 Farming, batch 9: harvesters, sprinklers and cotton
- **Crop harvester** (1×2): harvests and replants the ripe crops in the 9×9 field in front of it.
- **Sprinkler:** pipe-fed water gives nearby crops extra growth ticks; it also spreads fertilizer from its hopper.
- **Cotton:** a new crop; seeds from sifting coarse dirt; cotton spins into string.
- Three advancements, a Farming handbook chapter and a game test for each.

### #57 Fluid logistics, batch 8: gas holders, valves and filters
- **Gas holder** (3×3×3 Horton sphere): 1,024 buckets of one gas, and only gases.
- **Fluid valve:** a steel pipe segment that a redstone signal closes, splitting the line in two.
- **Fluid filter:** a steel pipe segment whose neighbouring tanks and machines only receive its chosen fluid; set it with a bucket or from a tank beside it (for gases).
- Three advancements, handbook pages and a game test for each.

### #56 Electronics, batch 7: silicon, chips and the cyan look
- **The cyan look** for the electronics tier, following the owner's references: near-black casings with cyan seams, cyan glass that glows while working, cyan screens, violet conduits, and a beige retro computer.
- **Crystal grower** (1×2): 4 silicon + a phosphate dopant → a silicon boule; the sawmill cuts it into 8 **silicon wafers**.
- **Lithography station** (3×2×2, a cleanroom with a monitor bank): wafer + copper wire + sulfuric acid → 4 **microchips**.
- **Processors:** the third circuit tier (circuit assembler).
- **Network terminal:** a beige retro computer that reads out the power network it is cabled to.
- Four advancements, an Electronics handbook chapter and game tests for each.

### #54 Chemistry, batch 6: advanced materials
- **Titanium:** a new mined metal (deep ore, iron pickaxe, `titanium.enabled`). No furnace smelts it.
- **The Kroll process:** raw titanium + coke + 250 mB chlorine → titanium sponge (chemical reactor); the arc furnace melts it into ingots. Chlorine's first use.
- **Leaching:** lepidolite or monazite + sulfuric acid → 2 lithium carbonate or 2 rare earth oxide.
- **Lithium battery bank** (3×2×1, electric look): 32,000,000 JE, 16,384 JE/t out of its front; built from lithium cells and titanium.
- **Neodymium magnets** (alloy smelter), and the **magnet dynamo** and **magnet motor**: 95% each way, four times the copper-wound rates, cyan-banded.
- Four advancements, handbook pages and game tests for each.

### #52 Chemistry, batch 5: electrochemistry and acids
- **Brine** (chemical mixer: salt + water) and the **electrolytic cell** (3×3×2): brine → **chlorine**, **hydrogen** (gases) and **lye**, each out of its own row.
- **Chemical reactor** (2×2×2): sulfur dust + water → **sulfuric acid**.
- **Alumina and real aluminum:** bauxite + lye → 2 alumina; 2 alumina + coke → 2 aluminum ingots in the cell. Two ingots per bauxite, twice the arc furnace.
- **Fertilizer:** phosphate + sulfuric acid; ripens every crop in a 5×5 area.
- **Fuel cell** (one block, electric look): hydrogen → 128 JE/t.
- Five advancements, a Chemistry chapter in the handbook, and a metal audit for fluid recipes.

### #51 Oil line, batch 4: industry, and the electric look
- **Polymerization reactor** (2×2×3): refinery gas → plastic pellets; the metal press makes **plastic sheets**.
- **Asphalt**, **asphalt slab** and **asphalt road line**: walking on them is 1.3× as fast.
- **Diesel engine** (2×2×3): up to 512 KE/t into a shaft line from diesel or heavy fuel oil, burning only for what is used.
- Nine oil **advancements**, a **Fuel Values** handbook page, JEI categories for every **fluid machine**, and the oil audit in [docs/BALANCE.md](docs/BALANCE.md#oil).
- **Electric look** (owner request): cables are 6 px graphite with a glowing green core (emissive), with copper, silver or aluminum collars; the battery box, capacitor bank, charging station, solar panel, electric pump, electric motor and dynamo are restyled in graphite and green light.
- Fix: a dynamo took up to twice its 128 KE/t on a strong shaft line; it is now capped per tick.
- Screenshots: a power-gear scene, and the multi-block showroom spaced to fit the oil machines.

### #50 Oil line, batch 3: fracking and diesel power
- New fluids: **fracking fluid** and **flowback water** (with buckets).
- **Chemical mixer** (2×2×2): water + sand + dried kelp → fracking fluid.
- **Fracking rig** (3×3×5): over shale oil, pumps fracking fluid down and brings up crude oil, refinery gas and flowback water.
- **Flowback treatment unit** (3×1×2): flowback water → clean water (a quarter lost) + salt.
- **Diesel generator** (3×2×2): 256 JE/t from diesel (256 JE/mB) or heavy fuel oil (128 JE/mB).
- **Gas turbine** (4×2×2): 512 JE/t from gasoline (384 JE/mB) or refinery gas (192 JE/mB), with lubricant upkeep.
- Game tests for each.

### #49 Oil line, batch 2: refining
- **Steel fluid pipes** (1,000 mB/t) and the **heavy pump** (1,000 mB/t); a pipe line now carries as much as its slowest pipe.
- New fluids: **naphtha, diesel, heavy fuel oil, lubricant, gasoline** (with buckets) and **refinery gas** (a gas: tanks and pipes only).
- **Distillation tower** (2×2×7): crude oil → gas, naphtha, diesel and heavy fuel oil, each drawn off at its own height.
- **Catalytic cracker** (2×2×4): heavy fuel oil + steam + catalyst → diesel, naphtha and gas. **Cracking catalyst** from bauxite, sand and nickel.
- **Vacuum distillation unit** (2×2×3): heavy fuel oil → lubricant + asphalt binder.
- **Catalytic reformer** (3×2×2): naphtha → gasoline + gas.
- Game tests for each.

### #47 Oil line, batch 1: oil in the world
The first five commits of the dieselpunk Chemistry branch ([plan](docs/branches/CHEMISTRY.md#petrochemistry-the-dieselpunk-oil-line)).
- **Crude oil:** a real fluid with a bucket; slow, thick, never makes new sources; works in every tank and pipe.
- **Fluid processing machines:** machines with input and output tanks, data-driven fluid recipes and tank gauges on their screens.
- **Oil reservoirs:** hidden, finite oil under Overworld chunks (pumpable or shale), fixed by the seed; the prospector reports them.
- **Pumpjack:** a 1×3×3 dieselpunk nodding donkey that pumps crude oil from the reservoir under it.
- **Oil sand extractor:** a 2×2×2 hot-water plant that washes crude oil out of oil sand and bitumen.
- Game tests for each; a new Oil chapter in the handbook.

### #46 Balance review
- New [docs/BALANCE.md](docs/BALANCE.md): every generator, conversion, store and cost in one place, with the loops that were checked.
- Charcoal burns three quarters as long as coal in Jugcraft's generators and engines. This makes tree-farm wood power slightly weaker (net about +545 JE/t per tree farm, down from +737), as the owner chose. Vanilla furnaces are unchanged.

### #44 Conveyor slopes
- **Conveyor Slope:** carries items one block up or down; use it with an empty hand to switch. Slopes join conveyor runs, and items climb and descend them visibly.
- A game test and a client screenshot.

### #43 Advancements
- A **Jugcraft** advancement tab: 22 steps from the first tin to the rocket pack, earned by having each item. Goals for steel, the steel foundry and the large steam engine; a challenge for the rocket pack.
- The handbook's Getting Started chapter lists the steps on its Milestones pages.
- A game test checks the tree loads; the data checker checks every step's items, title and parent.

### #42 Tool upgrades and a 3D rocket pack
- **Upgrade modules** for the powered tools, fitted at the charging station: Overclock, Range (5×5 drilling), Capacity, Silk Touch and Fortune.
- The worn rocket pack is now a 3D model on the wearer's back.
- The mining drill sits higher in first person.
- Four game tests.

### #41 Dieselpunk steel machines
- The steel foundry, capacitor bank, steel tank, ore drill and high-pressure extractor now look dieselpunk: gunmetal and olive paint, hazard stripes, chrome, phosphor gauges and caged lamps, an exhaust stack on the foundry and a diesel motor on the drill. Same footprints and ports; looks only.

### #40 Powered tools (the first dieselpunk gear)
- **Mining Drill:** a JE pickaxe and shovel, faster than netherite; modes for one block, 3×3 or a whole ore vein.
- **Chainsaw:** a JE axe that also cuts leaves and fells whole trees.
- **Rocket Pack:** worn on the chest; hold jump in the air to fly.
- **Charging Station:** a two-block-tall station that charges the tool on its cradle from cables.
- The tools hold JE instead of wearing out; empty, they mine like a bare hand.
- New dieselpunk textures and detailed 3D item models; [docs/ART_DIRECTION.md](docs/ART_DIRECTION.md) records the rule that higher tiers look dieselpunk.
- Five game tests and three client screenshots.

### #38 Conveyors
- **Conveyor:** carries items (drawn riding on it) the way it faces, 2.5 blocks a second, while rotation drives it: 1 KE per conveyor per tick for a whole joined run. Pipes, hoppers, machines and dropped items load it; it unloads into the conveyor or inventory ahead, or onto the ground. It carries players and mobs too.
- **Conveyor Splitter:** sends items left, straight on and right in turn.
- Four game tests and a client screenshot.

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
