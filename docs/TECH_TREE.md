# Jugcraft technology tree

How every implemented material, machine and part works and connects. **Implemented** means it is in the mod and compiles in CI; nothing here has been play-tested yet. Planned work is marked **planned** and links to its branch document.

## Branches

| Branch | Status | Scope |
| --- | --- | --- |
| **Materials** | Implemented | Ores, raw materials, ingots and alloys. See [base-materials.md](features/base-materials.md). |
| **Power** | Implemented | Generators, batteries and cables (JE energy). See [machines-and-power.md](features/machines-and-power.md). |
| **Mechanical processing** | Implemented | Physical transformation of materials: smelting, crushing, alloying, pressing, drawing, assembling. |
| **Fluids** | Implemented | Pipes, tanks and pumps that move and store water, lava and other mods' fluids; physical only, no reactions. See [Fluids](#fluids) below. |
| **Chemistry** | **In progress** | Reactions that change what a substance *is*: electrolysis, acids, fertilizer, refining. The dieselpunk oil line is being built first: see [branches/CHEMISTRY.md](branches/CHEMISTRY.md) and [petrochemistry.md](features/petrochemistry.md). |
| **Agriculture** | Three slices implemented | Crops, seeds, food and hand farm tools; its own starting branch, needing no machines. Corn grows 3 blocks tall for fields and mazes; tomatoes climb trellises; a Cooking Pot makes meals over a campfire; squash grows from stems, cranberries in bogs and chestnuts on a fruiting tree. See [branches/AGRICULTURE.md](branches/AGRICULTURE.md). |

The mechanical branch changes the **shape or mix** of materials (crush, melt, alloy, press, draw, assemble). Anything that needs a chemical reaction belongs to the Chemistry branch, even when it currently has a temporary blast-furnace or arc-furnace stand-in.

## How it fits together

```mermaid
flowchart LR
    Ore[Ores and rocks] -->|pickaxe| Raw[Raw ore]
    Ore -->|Crusher x2| Raw
    Raw -->|furnace / blast furnace / Electric Furnace| Ingot[Ingots]
    Ingot -->|Alloy Smelter| Alloy[Bronze, brass, invar, solder]
    Ingot -->|Metal Press| Plate[Plates]
    Alloy -->|Metal Press| Plate
    Plate -->|crafting, 4 plates| Gear[Gears]
    Ingot -->|Wire Drawer| Wire[Wires]
    Quartz[Vanilla quartz] -->|Arc Furnace| Silicon
    Silicon & Wire & Alloy -->|Circuit Assembler| Circuit[Circuits]
    Coal[Coal / water / sun] -->|Generators| Power[JE power]
    Power -->|Copper Cable / Battery Box| Machines[All machines]
    Plate & Gear -->|crafting| Machines
    Plate -->|crafting| Fluid[Pipes, tanks, pump]
    Water[Water / lava source or tank] -->|Electric Pump| Fluid
    Fluid -->|water| Steam[Steam Generator]
    Power --> Fluid
```

## Progression, step by step

1. **Mine and smelt** tin, zinc, lead and copper with a stone pickaxe and a vanilla furnace. Make **bronze** by hand: 3 copper + 1 tin, crafted into bronze blend, then smelted. **Thallite**, the Earth school's green metal, is mined and smelted the same way; it is richest in Lush Caves and the Glowcap Grotto and makes no alloy ([thallite.md](features/thallite.md)).
2. **Craft a Machine Casing** (bronze + zinc), **Copper Cable** (copper + tin) and a **Coal Generator**. This is the first power.
3. **Early machines:**
   - **Electric Furnace:** twice the vanilla furnace's speed.
   - **Crusher:** doubles ore.
   - **Battery Box:** stores power; needs lead.
4. **Alloy Smelter:** makes bronze without crafting, and unlocks brass, invar and solder.
5. **Metal Press** (bronze, piston, anvil) → **plates**; 4 plates → **gear**. **Wire Drawer** (brass, shears) → **wires**.
6. **Arc Furnace multiblock** (nickel casings) → **silicon** from quartz, plus aluminum, lithium carbonate and rare-earth oxide.
7. **Circuit Assembler** (built from tin plates and a bronze gear): silicon + copper wire + solder → **basic circuit**; 2 basic circuits + silver wire + an invar plate → **advanced circuit**.
8. **Better power:**
   - **Steam Generator:** an upgraded coal generator that also burns bitumen.
   - **Solar Panel:** needs silicon.
   - **Wind Turbine** (9 blocks tall with a 7-block rotor, aluminum plates): free power that grows with height.
   - **Geothermal Generator** (2×2×2, needs a basic circuit): runs on lava. An electric pump on lava feeds it through pipes.
9. **Fluids:** **Bronze Fluid Pipes** and the **Tinplate Tank** are crafted from press-made plates; the **Electric Pump** adds iron gears, a bucket and a casing. A pump on water, piped to a steam generator, keeps the boiler full without buckets.

## Machines

All machines hold their own internal battery and accept power from cables or directly from an adjacent generator or battery. Where power goes in is described in [Power connections](#power-connections). By default hoppers insert into input slots from the top and sides and extract results from the bottom; each face can be changed on the machine's screen (see [Item logistics](#item-logistics)).

| Machine | Branch | Does | Power | Built from |
| --- | --- | --- | --- | --- |
| Coal Generator | Power | Burns coal, charcoal (¾ as long), coal blocks or coke → 32 JE/t | produces | bronze, cable, furnace, casing |
| Steam Generator | Power | Boils water with coal or bitumen → 64 JE/t | produces | coal generator, bronze, bucket, cable, casing |
| Solar Panel | Power | Daylight under open sky → 8 JE/t (4 in rain) | produces | glass, silicon, bronze, cable |
| Advanced Solar Panel (pedestal + 3×3 array) | Power | A white pedestal carrying nine blocks of cells: 64 JE/t in full sun (half in rain), 400,000 JE buffer, 512 JE/t out of the pedestal | produces | 3 solar panels, aluminum plates, processor, aluminum cable, titanium |
| Battery Box | Power | Stores 400,000 JE; outputs from its front | stores | lead, cable, redstone block, casing |
| Electric Furnace | Mechanical | Any vanilla smelting recipe, 100 ticks | 10 JE/t | bronze, redstone, cable, furnace, casing |
| Crusher | Mechanical | Ore → 2 raw; minerals, sulfur, oil sand, cobble → gravel → sand | 16 JE/t | flint, cable, casing, bronze, redstone |
| Alloy Smelter (3 wide, 2 deep, 6 tall) | Mechanical | Two ingredients (any order) → bronze, brass, invar, solder. Power **only** through its copper socket | 20 JE/t | bronze, cable, 2 furnaces, casing, redstone |
| Metal Press | Mechanical | Ingot → plate (1:1) | 16 JE/t | bronze, piston, cable, casing, anvil |
| Wire Drawer | Mechanical | Ingot → 3 wires | 12 JE/t | brass, shears, cable, casing, redstone |
| Circuit Assembler | Mechanical | Up to three ingredient stacks (any order) → circuits | 32 JE/t | tin plates, bronze gear, cable, casing, redstone |
| Pulverizer | Mechanical | Ore → 2 dust + byproduct; washed ore, raw metal, ingot → dust ([Ore processing](#ore-processing)) | 20 JE/t | flint, iron gears, bronze plates, cable, casing |
| Ore Washer | Mechanical | Ore + 500 mB water → 3 washed ore | 16 JE/t | invar plates, bucket, bronze gears, basic circuit, casing |
| Sieve | Mechanical | Gravel → flint, soul sand → soul soil, with small finds | 8 JE/t | iron plates, iron bars, hopper, cable, casing |
| Sawmill | Mechanical | Log → 6 planks + sawdust; planks → 3 sticks | 12 JE/t | iron, iron gear, iron plates, cable, casing |
| Coke Oven (2×2, 2 tall, chimney on top) | Steel | Coal → coke, 600 ticks ([Steel tier](#steel-tier)) | none | bricks, iron, furnace |
| Steel Foundry (2×2, 5 tall) | Steel | Iron ingot + coke → steel ingot, 400 ticks | none | bricks, hopper, iron plates, blast furnace |
| Geothermal Generator (2×2×2) | Power | Lava → 64 JE/t (1 mB/t; a bucket lasts 1,000 ticks) | produces | invar plates, tinplate tank, bronze gears, casing, basic circuit |
| Wind Turbine (9 tall, 7-block rotor) | Power | 12–72 JE/t by height above sea level; ×1.5 in rain, ×2 in thunder; the rotor turns (drawn by the client) and needs a clear 7×7 square in front of the top | produces | aluminum plates, bronze gears, casing, bronze plates, cable |
| Arc Furnace (3×3×3 multiblock) | Mechanical (with chemistry stand-ins) | Quartz → 2 silicon; 4 silicon + 1 phosphate → 1 silicon boule (400 ticks; argon piped into the controller, 1 mB/t, doubles its speed; batch 24, from the old crystal grower); raw nickel, tungsten or uranium → ingot; titanium sponge → titanium ingot; bauxite, lepidolite and monazite stand-ins | 64 JE/t | 26 arc furnace casings (bricks + nickel) + controller |

## Machine looks: steampunk and classic

Machines are drawn in a **steampunk** style by default: brass, copper and riveted iron, with gauges, gears, valve wheels, glowing fireboxes and portholes. The look is purely visual. Blocks, recipes, footprints, screens and power connections are the same in both styles.

Power gear (cables, battery box, capacitor bank, charging station, solar panel, electric pump, electric motor and dynamo) has an **electric** look: graphite with glowing green strips, screens and ports. From the steel tier up, other machines are **dieselpunk** instead: gunmetal and olive paint, hazard stripes, chrome, phosphor gauges and caged lamps (see [ART_DIRECTION.md](ART_DIRECTION.md)). The steel foundry, capacitor bank, steel tank, ore drill and high-pressure extractor already use it; the renders below predate that.

![Steampunk machines, each shown from the front-left and front-right](images/steampunk_machines.png)

![Steampunk multi-block machines, from both sides](images/steampunk_large_machines.png)

*Approximate renders of the generated models, not game screenshots. Machines that glow are shown running.*

**Switching back to the classic look**

- **In game, per player:** Options → Resource Packs → turn on **Jugcraft: Classic Machines**.
  - This pack is built into the mod and off by default.
  - Every machine goes back to its original model; turn the pack off to get steampunk back.
  - Nothing in the world or on the server changes.
- **For everyone, permanently:**
  - Set `DEFAULT_STYLE = "classic"` in `tools/model_writer.py` and run `python3 tools/generate_material_data.py`.
  - The mod then ships classic by default, and the built-in pack becomes **Jugcraft: Steampunk Machines**.
- **Remove steampunk entirely:** revert the pull request that added it (a single squash commit).

![The classic look, which the built-in pack restores](images/classic_machines.png)

**How it is built**

- `tools/steampunk_models.py`: every steampunk model. The models are built from helpers for round boilers and drums, gears, valve wheels, gauges, lamps and pipes. One-block machines stay inside their block, and multi-block ones are sliced per block as described below.
- `tools/steampunk_textures.py`: the original steampunk textures (`sp_*`).
- `tools/model_writer.py`: writes either style. The classic style is the earlier output, byte for byte.
- `src/main/resources/resourcepacks/alternate_machines/`: the generated built-in pack.
- `tools/check_mod_data.py` checks three things:
  - both styles cover every machine and every block state;
  - everything the models reference exists;
  - no model leaves Minecraft's −16…32 range.
- **Glowing parts:** firebox doors, portholes and lamps switch to their lit texture while a machine runs.
- **Where cables and pipes meet the machine:** near the middle of each side, every steampunk machine has a terminal, flange or plate within about two pixels of the edge, so a connecting cable or pipe touches the machine. The solar collector's top is the exception, because it has to see the sky.
- **Neighbouring blocks:** because the models are not full cubes, machine blocks no longer hide the faces of the blocks next to them.

## Multi-block machines

Most machines stay **simple one-block machines**. The ones where size is part of what they are (towers, tanks, big engines) are **multi-block machines**: you place one item and it fills two or three blocks with one detailed model. This works like Immersive Engineering's pump and sample drill; no Immersive Engineering code or art is used.

| Alloy Smelter: furnace, crucible tower, hoppers; cable in its socket | Geothermal Generator: body plus a lava tank to its right | Wind Turbine: base, mast and rotor |
| --- | --- | --- |
| ![Alloy smelter with a cable plugged into its power socket](images/alloy_smelter.png) | ![Geothermal generator](images/geothermal_generator.png) | ![Wind turbine](images/wind_turbine.png) |

*Approximate renders made from the generated block models and textures, not game screenshots.*

**How they behave**

- **Placing:**
  - The item places only if every block of the machine is free (air, grass, water and so on) and inside the world.
  - The machine faces you, and its extra blocks turn with it.
- **Breaking:** breaking **any** part removes the whole machine and drops **one** item, so nothing is lost and nothing is duplicated. Pistons leave the parts alone (vanilla never pushes entity blocks; not yet checked in game).
- **Using:**
  - Every part acts as the machine. Right-click any part to open the screen.
  - Hoppers feed and empty it through any part: the top and sides fill the inputs, and the bottom takes the output.
  - Cables and pipes connect to any part, and comparators read the machine through any part.
  - Buckets fill its tank through any part.
- **Server cost:**
  - Only the main block (part 0) has a block entity and ticks. The other parts are plain blocks.
  - Energy and fluid lookups on a part go straight to the main block.
- **Look:** each part renders its own slice of one big model. Blades, stacks and fins may reach into the air beside the machine. The wind turbine needs that air clear to turn.

**How it is built** (to add another one)

1. Give the machine a `MachineKind` with a `footprint()`: the blocks it fills, written for a north-facing machine (`Footprint.tall(3)`, or offsets such as `new Vec3i(-1, 0, 0)` for "one block to the right").
2. Author its model in both styles, in structure coordinates with the main block at 0–16 on each axis: `tools/steampunk_models.py` for the default look and `tools/large_machines.py` for the classic one.
3. Run `python3 tools/generate_material_data.py`. It slices the model into one model per block and writes the blockstates for every facing and part, plus a scaled-down model for the inventory.
4. `tools/check_mod_data.py` checks that the Python and Java footprints match and that no model leaves Minecraft's −16…32 limit.

Code: `machine/Footprint.java` (offsets and rotation) and `machine/LargeMachineBlock.java` (placing, breaking, forwarding to the main block).

## Power connections

Every powered block follows one of two rules, and a cable shows which by where it connects:

| Rule | Machines | What you see |
| --- | --- | --- |
| **Any side** | Every one-block machine, generator and battery; the electric pump; the geothermal generator and wind turbine (any face of any of their blocks) | A cable next to any face bends to it and connects. A generator or battery placed directly against the machine also powers it. |
| **No power** | The Coke Oven and Steel Foundry | Cables never connect; the machines run on the heat of their charge. |
| **Power socket only** | The Alloy Smelter | Its **copper socket in a brass frame** (a yellow-and-black frame in the classic look), on the outer side of its lower right block (seen from the front). Cables connect only there; a cable along any other face doesn't bend toward it. |

The battery box and capacitor bank are "any side" for charging. It gives power out only through its front, and a cable at the front still connects.

In code, one check decides both the drawn connection and the flow: `MachineBlock.acceptsPower(state, side)`, built from `MachineKind.usesPower()` and `MachineKind.powerPort()` (null means any side). The energy lookup and the cable's connection arms both use it. `tools/large_machines.py` (`POWER_PORTS`) places the socket in the model, and `check_mod_data.py` keeps the two in sync.

## Cables and pipes

Cables, the Bronze Fluid Pipe and the Brass Item Pipe are *transmitters*, built like the ones in other tech mods (Mekanism, Thermal, IC2).

**Cable tiers.** Tiers connect to each other, and a network carries as much as its slowest cable:

| Cable | JE per tick | Built from |
| --- | --- | --- |
| Copper Cable | 256 | 2 copper + 1 tin → 6 |
| Silver Cable | 1,024 | 3 silver wire + 3 copper cable → 3 |
| Aluminum Cable | 4,096 | 2 aluminum wire + 1 steel plate + 3 silver cable → 3 |

There is no faster fluid pipe yet. A pump moves 100 mB/t, less than the bronze pipe's 250, so a faster pipe would change nothing until faster pumps exist.

**Transmitter design:**

- **Thin, not full blocks.** Cables are 6 pixels thick, fluid pipes 4 and item pipes 6. An arm reaches out toward each connected neighbor, and the hitbox follows the same shape (Mekanism's are 6 pixels).
- **Connect automatically.**
  - A cable joins other cables and any block that stores or uses energy on the touching face.
  - A pipe joins other pipes and any block with a fluid storage on that face, including tanks, pumps, the steam generator, vanilla cauldrons and other mods' fluid blocks.
  - Cables and pipes never connect to each other, so they can run side by side.
- **Visual connection = real connection.** The arms use the same lookup (`EnergyStorage.SIDED` / `FluidStorage.SIDED`) as the transfer code, so if it looks connected it is connected.
![Cables between a coal generator and machines; a pipe from a pump to a tank](images/transmitters.png)

*Approximate isometric render made from the mod's own textures and model boxes, not a game screenshot.*

- **Look:**
  - Cables have the electric look: a graphite sheath with a glowing green core strip along every side, a glowing cross on the junction, and collars of the tier's metal (copper, silver, aluminum) where they meet a neighbour. The strips are emissive, so they glow in the dark. The pipe is bronze, darker at its flanged ends.
  - In the inventory both show as a short 3D segment.

## Fluids

The fluid branch moves liquids around. It never changes what a liquid *is*: that is chemistry.

| Block | Does | Numbers | Built from |
| --- | --- | --- | --- |
| Bronze Fluid Pipe | Carries fluid pushed into it by a pump to every fluid storage it touches | 250 mB/t per push, up to 1,024 pipes per network | 2 bronze plates + glass → 4 |
| Tinplate Tank | Stores one fluid; fill or empty with buckets, right-click with an empty hand to read it, comparators show how full it is | 16,000 mB (16 buckets); contents are lost if broken | 8 tin plates + glass |
| Electric Pump | Pulls from below, pushes out of its top and four sides | 100 mB/t, 8 JE per tick it moves fluid, 4,000 JE battery, 4,000 mB buffer | bronze plates, bucket, 2 iron gears, casing, cable |
| Steel Fluid Pipe | Like the bronze pipe, for refinery flows | 1,000 mB/t per push; a network carries as much as its slowest pipe | 2 steel plates + bronze pipe → 3 |
| Fluid Valve | A steel pipe segment that redstone closes | Open: a steel pipe. Powered: closed, and the pipes on either side become separate lines. Lamp green open, amber closed. | lever, 2 steel plates, steel pipe, redstone → 2 |
| Fluid Filter | A steel pipe segment that sorts fluids | Carries anything, but the tanks and machines it touches only get its chosen fluid (nothing until set). Set with a filled bucket, or by right-clicking beside a tank of the fluid (for gases); sneak to clear. | 2 steel plates, 2 iron bars, steel pipe |
| Heavy Pump | The steel-tier pump | 1,000 mB/t, 40 JE per tick it moves fluid, 32,000 JE battery, 16,000 mB buffer | steel plates, silver cable, 2 steel gears, electric pump, casing |

**How the pieces work together**

1. **The pump is the only thing that moves fluid.** Pipes and tanks are passive, like cables: nothing ticks in them.
2. **What the pump pulls from, directly below it:**
   - a tank, or any other mod's fluid storage;
   - a **water source block**, which is treated as a spring and not used up (the same rule as the steam generator);
   - a **lava source block**, which *is* used up: one bucket per second, and the block becomes air.
3. **Where it pushes:** out of its top and sides, straight into an adjacent fluid storage, or into a pipe network. The network splits the flow evenly across every storage it touches that has room, except the pump itself. Outside storages cannot push into a pump, so fluid only flows out of it.
4. **Who accepts fluid:**
   - tanks take any single fluid;
   - the **Steam Generator** takes water into its 8,000 mB boiler tank (from any side);
   - vanilla cauldrons and other mods' tanks work through Fabric's fluid API.
5. **Units.** Jugcraft numbers are millibuckets (1 bucket = 1,000 mB). Internally Fabric counts droplets (1 bucket = 81,000), so 1 mB = 81 droplets.

**Performance.** Pipe networks are found once by a bounded search and cached per dimension. They are rebuilt only after a pipe, tank, pump or machine is placed or removed, or a pipe's neighbor changes. A pump does two storage moves per tick at most, plus one per network endpoint.

## Storage

| Block | Holds | Details | Built from |
| --- | --- | --- | --- |
| Item Crate | 32 stacks of one item | Right-click with an item to put it in; with an empty hand to take a stack (sneak to just look). Pipes, extractors and hoppers use it; comparators read how full it is. Breaking it drops everything. | iron plates, planks |
| Capacitor Bank (2 wide, 2 tall) | 4,000,000 JE | Charges from any side; gives power out of the copper sockets on its front, 4,096 JE/t (a job for aluminum cable). Comparators read its charge. | steel plates, 4 battery boxes, advanced circuit |
| Lithium Battery Bank (3 wide, 2 tall, 1 deep) | 32,000,000 JE | Charges from any side; gives power out of the six sockets on its front, 16,384 JE/t (four aluminum cables' worth). Comparators read its charge. Electric look. | titanium ingots, 4 lithium cells, a capacitor bank |
| Gas Holder (3×3×3) | 1,024 buckets of one gas, only gases | A Horton sphere on legs. Pumps and pipes fill and empty it from any face; right-click to read it; comparators read how full it is. | steel plates, 4 steel tanks, steel pipe |
| Steel Tank (2 wide, 2 deep) | 128 buckets of one fluid | Buckets, pumps and pipes fill and empty it from any face; right-click with an empty hand to read it. Comparators read how full it is. | 8 steel plates, tinplate tank |

**Tanks keep their fluid.** Break a tinplate tank, steel tank or gas holder and it drops as one item carrying its fluid (the tooltip shows which and how much); place it again and the fluid is back. Empty tanks still stack.

![Capacitor Bank, Steel Tank and Item Crate](images/storage.png)

*Approximate isometric render made from the mod's own textures and model boxes, not a game screenshot.*

## Mining and prospecting

| Block or tool | What it does | Details | Built from |
| --- | --- | --- | --- |
| Geo-Resonance Prospector (hand tool) | Surveys the 3×3 chunks around you | Shows each ore family found as 1–5 bars with a rough depth (shallow Y ≥ 40, middle 0–39, deep below 0). Readings are deliberately vague: every second column is sampled, a quarter of readings are one bar off, and no positions are given. 3-second cooldown. | brass plates, copper wire, glass pane, basic circuit |
| Deposit Drill (3×3, 2 tall) | Works a surface deposit under it | Built on a patch of Coal, Iron, Copper or Tin Deposit (stony hills). Every 15 seconds at 16 JE/t it takes one coal or raw ore from each kind of deposit under it or one block round it, down to 3 deep. It pushes them out of every side into chests, pipes, conveyors or machines beside it. Each deposit block holds 1,000 and then turns to stone; right-click one to read what is left. Picks break deposits for nothing. Upgrades, side configuration, redstone modes and comparator output as on other machines. | iron plates, bronze gears, 2 iron pickaxes, machine casing, basic circuit |
| Ore Drill (2 tall) | Mines the ores in a 9×9 column below it | One `c:ores` block every 40 ticks at 32 JE/t, from the layer under it down to the bottom of the world. Ores come out whole (like silk touch) into three result slots, and each hole is refilled with stone, deepslate or netherrack. It has upgrades, side configuration, eject, redstone modes and comparator output. It stops when full. | steel plates, steel gear, 2 basic circuits, machine casing, diamond pickaxe |

![Ore Drill](images/mining.png)

*Approximate isometric render made from the mod's own textures and model boxes, not a game screenshot. The prospector's screen appears in the CI client screenshots.*

**Code:** `prospecting/` (`OreSurvey`, `SurveyPayload`, `ProspectorItem`, `JugcraftProspecting`), `client/ProspectorScreen`, and `machine/OreDrilling` with `MachineKind.ORE_DRILL`.

## Pixel Hollows and the Retro Trader

A cave to explore and a villager who points the way; neither needs any machine. Records: [pixel-hollows.md](features/pixel-hollows.md), [retro-trader.md](features/retro-trader.md).

| Block, item or place | What it does | How to get it |
| --- | --- | --- |
| Pixel Hollows (cave biome) | Deep under dry land (deserts, savannas, badlands, dry plains), mostly below Y 32. Circuitstone walls, sparse glowing crystals, hostile mobs in the dark pockets, 1.5× copper and redstone (and tin) | explore, or follow a Pixel Hollows Map |
| Circuitstone, polished circuitstone, circuitstone bricks | building blocks; the stonecutter cuts circuitstone into the other two | mine with any pickaxe; 4 → 4 by crafting like deepslate |
| Pixel crystal cluster | light 3; drops 1–2 pixel shards (Fortune adds up to one per level); Silk Touch keeps the cluster | grows in the cave only, never regrows |
| Pixel lamp | full-light block of coloured pixels | 4 pixel shards + 1 glass |
| Arcade cabinet (2 tall) | the Retro Trader's job site; any unemployed villager can take it | planks, glass pane, redstone, 2 pixel shards; or found in a Retro Game Shop |
| Retro Trader | novice: Pixel Hollows Map (12 emeralds + compass), 8 circuitstone (1 emerald); apprentice: buys 6 shards for 1 emerald, sells a lamp (3); journeyman: 2 shards (4) | a villager at an arcade cabinet |
| Pixel Hollows Map | use it: marks the nearest Pixel Hollows within 2,048 blocks on an explorer map named with its depth, or tells you none is in reach and stays unused | the Retro Trader |
| Retro Game Shop | a storefront in every new village (one each; not zombie villages) | find one |

**Code:** `world/` (`PixelHollows`, `RetroTrader`, `ArcadeCabinetBlock`, `PixelHollowsMaps`, `PixelHollowsMapItem`), `mixin/`, and the data generated from `tools/pixel_hollows.py`.

## Automation

| Block | What it does | Details | Built from |
| --- | --- | --- | --- |
| Auto-Crafter | Crafts the recipe in its 3×3 grid, 40 ticks per craft, 8 JE/t | Each grid slot keeps one item as the pattern, so it crafts while every filled slot has two or more. Pipes and hoppers only top up slots that already hold that item. Remainders (empty buckets, bottles) go to the slot above the output. Any crafting-table recipe the server knows. Unstackable ingredients can't be automated yet. | brass plates, basic circuit, 2 crafting tables, casing, hopper |

**Code:** `MachineKind.AUTO_CRAFTER` and `MachineBlockEntity.tickCrafter` (vanilla `RecipeType.CRAFTING`). The grid layout is in `MachineMenu.inputX/inputY(kind, slot)`.

## Kinetic power

A second, mechanical power system measured in **KE** (kinetic energy) per tick. Shafts carry rotation along their length and gearboxes out of all six sides. Machines at the end of a line run straight off it (1 KE = 1 JE, no loss), and a dynamo bridges it into JE cables at 75%.

| Block | What it does | Details | Built from |
| --- | --- | --- | --- |
| Hand Crank | 16 KE/t into the block it faces | Each right-click turns it for 5 s (up to 20 s); costs a little food. | planks, iron shaft |
| Steam Engine | 64 KE/t out of its back | Burns generator fuel and 10 mB water per tick, only while something takes the power. Loaded by right-click, hoppers, pipes and pumps, or a water source below. | bronze, bucket, 2 pistons, furnace, iron shaft |
| Iron Shaft | Carries rotation along its axis | Placed like a log; shows turning while driven. | 2 iron ingots → 4 |
| Brass Gearbox | Passes rotation out of all six sides | Branches and turns lines; power is shared evenly. | brass plates, bronze gears, iron shaft |
| Dynamo | KE → JE at 75%, 128/t | Pushes JE into cables on every side. | copper, redstone, iron shaft |
| Belt Pulley | A shaft that can hold a belt | Carries rotation along its axis like a shaft, and to the pulley it is belted to. | planks, iron shaft |
| Leather Belt | Links two pulleys | Use on one pulley, then another: same axis, level along it, up to 16 blocks apart. Breaking a pulley drops the belt. | leather, string |
| Electric Motor | JE → KE at 75%, up to 96 KE/t | Takes JE from cables and drives the block it faces. | iron plates, copper wire, iron shaft, copper cable |
| Magnet Dynamo | KE → JE at 95%, 512/t | The dynamo with rare-earth magnets; pushes JE into cables on every side. Cyan-banded. | aluminum plates, 4 neodymium magnets, dynamo, aluminum cable |
| Magnet Motor | JE → KE at 95%, up to 384 KE/t | Takes up to 1,024 JE/t and drives the block it faces. With a magnet dynamo it still loses a tenth each round. | aluminum plates, 4 neodymium magnets, electric motor, aluminum cable |
| Diesel Engine (2×2×3) | Up to 512 KE/t out of the back of its upper right back block | Burns diesel or heavy fuel oil ([Oil](#oil)), only for what the line takes. | steel plates, plastic sheets, steel gears, casing |
| Advanced Combustion Engine (2 long) | Up to 1,024 KE/t out of the back of its right-hand block | Four cylinders; burns gasoline (448 KE/mB) or diesel (320), only for what the line takes. 8-bucket tank. | titanium, processor, 2 neodymium magnets, diesel engine, casing |

![Kinetic blocks](images/kinetic.png)

*Approximate isometric render made from the mod's own textures and model boxes, not a game screenshot.*

**Code:** `kinetic/` (`KineticNetworks`, `KineticConsumer`, `ShaftBlock`, `GearboxBlock`, `HandCrankBlock(Entity)`, `SteamEngineBlock(Entity)`, `DynamoBlock(Entity)`, `BeltPulleyBlock(Entity)`, `BeltItem`, `ElectricMotorBlock(Entity)`, `JugcraftKinetics`; client `BeltRenderer`). `MachineBlockEntity` implements `KineticConsumer`.

## Renewable resources

| Block | What it does | Details | Built from |
| --- | --- | --- | --- |
| Water Wheel (2 tall) | Power from flowing water | The wheel on its right side (seen from the front) turns in the column of blocks beside it: 8 JE/t per block of flowing water there, 12 if falling, up to 24 JE/t. Source water does not count. Cables connect to its house. | planks, sticks, bronze gear, copper cable |
| Cobblestone Generator | 1 cobblestone per 20 ticks, 4 JE/t | Needs water and lava touching any sides; neither is used up. | bronze, water bucket, lava bucket, cable, casing |
| Tree Farm | Sapling → 6 logs in 400 ticks, 16 JE/t | The sapling comes back (byproduct slot) with a 10% chance of the tree's extra (apple, cocoa beans, pink petals, pale moss carpet or a stick). Recipes are data (`jugcraft:tree_growing`) for all nine vanilla trees. | glass, glowstone, dirt, bronze, casing, basic circuit |

### Farming

See [farming.md](features/farming.md).

| Block or item | What it does | Details | Built from |
| --- | --- | --- | --- |
| Crop Harvester (2 tall) | Harvests and replants ripe crops in the 9×9 field in front of it | One crop per 20 ticks at 24 JE/t; keeps the drops less one seed, which it replants; waits when its three result slots are full | steel gears, shears, 2 hoppers, casing, steel plates, basic circuit |
| Sprinkler | Waters the crops within 4 blocks | Every 5 s uses 50 mB of water and gives each growing crop an extra growth tick; with fertilizer loaded, spreads one every 30 s over the 5×5 around it. 4-bucket tank, 16 fertilizer. | bronze pipe, steel plates, tinplate tank, hopper |
| Cotton | A crop like wheat | Seeds from sifting coarse dirt (15%); a ripe plant gives 1–3 cotton and seeds; 1 cotton → 1 string | – |

![Cobblestone Generator, Tree Farm and Water Wheel](images/renewables.png)

*Approximate isometric render made from the mod's own textures and model boxes, not a game screenshot.*

## Machine control

Every **powered processing machine** now has two **upgrade slots** (below the output) and a **redstone button** (the "R" above the face buttons). Comparators read every machine.

**Upgrades** are the first things made from steel. Each slot holds up to 4 cards, and at most 4 of each kind count.

| Upgrade | Per card | Four cards | Built from |
| --- | --- | --- | --- |
| Speed Upgrade | time ÷ (1 + 0.5 per card); energy per item +25% | 3× as fast, twice the energy per item | 4 steel plates, 2 redstone, 2 steel gears, basic circuit |
| Efficiency Upgrade | energy use −20% (compounding) | 41% of the energy | 4 steel plates, 4 copper wire, basic circuit |

- Speed and efficiency combine. Four of each gives 3× the speed at about 82% of the base energy per item.
- The highest draw (four speed cards) stays within each machine's input rate, so an upgraded machine never outruns its own cable intake.
- Upgrade slots are never offered to hoppers or pipes. Shift-click puts upgrades straight into them.

**Redstone mode** (click "R" to cycle):

- **Ignored** (gray): always runs.
- **High** (red): runs only while any block of the machine receives a redstone signal.
- **Low** (dark red): runs only without a signal.

A paused machine keeps its progress. The mode is saved with the side configuration.

**Comparators:**

- **Generators and the battery box:** stored energy (0 empty, 15 full).
- **Processing machines:** how full their input, output and byproduct slots are, like a chest.
- A comparator works against any block of a multi-block machine.

## Steel tier

Steel is the second material tier. It needs **no power** and no new ore, only iron, coal and two brick multi-blocks. Machines placed as one item fill several blocks and break together, like the other [multi-block machines](#multi-block-machines).

![Coke Oven and Steel Foundry (earlier sizes: the render predates the 2×2 versions)](images/steel_tier.png)

*Approximate isometric render made from the mod's own textures and model boxes, not a game screenshot.*

| Block | Does | Numbers | Built from |
| --- | --- | --- | --- |
| Coke Oven (2×2, 2 tall + chimney) | Bakes coal into **Coal Coke** | 600 ticks per coal; no power, no fuel | bricks, iron, furnace |
| Steel Foundry (2×2, 5 tall) | 1 iron ingot + 1 coke → 1 **steel ingot** (either slot) | 400 ticks; no power. Oxygen piped in (2 mB/t) doubles its speed (batch 13) | bricks, hopper, iron plates, blast furnace |

- **Coal Coke** (`c:coal_coke`) burns twice as long as coal in the Coal and Steam Generators (3,200 ticks). It is the carbon for steel.
- **Steel** has the usual ingot, nugget and block, plus a **steel plate** (Metal Press) and a **steel gear**. The first things built from steel are the [machine upgrades](#machine-control).
- **Metal accounting:** one iron ingot's metal becomes one steel ingot's. The coke is carbon, not metal, so nothing is gained.
- **Unpowered machines** have no battery, and cables never connect to them. Their screens show no energy bar.
- Both work with hoppers, pipes, side configuration and eject like any processing machine.

## Powered tools

The first dieselpunk gear (see [ART_DIRECTION.md](ART_DIRECTION.md)). The tools hold JE instead of wearing out, and are charged at a charging station fed by cables.

| Item / block | What it does | Details | Built from |
| --- | --- | --- | --- |
| Mining Drill | JE pickaxe and shovel, faster than netherite, diamond-tier drops | 100,000 JE, 60 JE a block. Sneak + use cycles one block / 3×3 / whole ore vein (32) | tungsten plate, 3 steel plates, steel gear, advanced circuit, lead ingot |
| Chainsaw | JE axe that also cuts leaves; fells whole trees | 100,000 JE, 40 JE a block; sneak to cut one log | 3 tungsten plates, 3 steel plates, steel gear, advanced circuit, lead ingot |
| Rocket Pack | Chest slot: hold jump in the air to fly | 200,000 JE, 50 JE a tick; no fall damage while firing; dedicated servers need `allow-flight=true` | 2 steel plates, advanced circuit, 2 fluid tanks, leather, 2 tungsten plates |
| Charging Station | Two blocks tall; charges the tool on its cradle from cables | 50,000 JE buffer, 1,024 JE/t in, 512 JE/t into the tool; lamp lights while charging | 4 steel plates, redstone lamp, 2 copper cables, advanced circuit, battery box |

Empty tools mine like a bare hand and get no drops.

**Upgrade modules** fit at the charging station (use one on a station holding the tool): Overclock (+50% speed, +100% JE a block; up to 2), Range (drill area mode 5×5), Capacity (base charge again; up to 2), Silk Touch (drill, chainsaw), Fortune (drill, up to III; not with Silk Touch). See [powered tools](features/powered-tools.md).

**Code:** `tools/` (`JugcraftTools`, `Chargeable`, `PoweredToolItem`, `MiningDrillItem`, `ChainsawItem`, `RocketPackItem`, `RocketThrustPayload`, `ChargingStationBlock(Entity)`); client `ChargingStationRenderer`, `RocketPackClient`.

### Tools, armor and paxels (batch 25)

| Item | What it does | Built from |
| --- | --- | --- |
| Bronze sword, pickaxe, axe, shovel, hoe | Iron-tier drops; 320 uses, speed 6.5 | bronze ingots and sticks, shaped like iron tools |
| Steel sword, pickaxe, axe, shovel, hoe | Diamond-tier drops (obsidian); 900 uses, speed 7 | steel ingots and sticks |
| Bronze helmet, chestplate, leggings, boots | Iron's defense, 0.5 toughness | bronze ingots, shaped like iron armor |
| Steel helmet, chestplate, leggings, boots | 3/6/7/3 defense, 1.5 toughness | steel ingots |
| Paxel (wood, stone, iron, gold, diamond, netherite, bronze, steel) | Mines like a pickaxe, axe and shovel; three times the tier's durability | that tier's pickaxe, axe and shovel (shapeless) |

**Code:** `gear/JugcraftGear`; data and art from `tools/gear.py` and `tools/gear_textures.py` ([feature record](features/tools-and-armor.md)).

### Steampunk and Kaiser Armor (batch 60)

The stylized looks bronze and steel armor were first drawn in, as sets of their own. Each piece protects exactly as its plain piece does (same defense, toughness, durability, enchantability and repair), and smithing keeps its enchantments and wear. Bronze and steel armor wear their own 3D look, the owner's knight design ([knight-armor.md](features/knight-armor.md)).

| Item | What it does | Built from |
| --- | --- | --- |
| Steampunk helmet, chestplate, leggings, boots | Bronze armor's protection, in the steampunk look: aviator cap and goggles, pressure gauge, copper boiler | smithing: Steampunk Pattern + bronze piece + copper ingot |
| Kaiser helmet, chestplate, leggings, boots | Steel armor's protection, in the kaiserpunk parade dress of the Winged Cog: spiked helmet, field-grey tunic, jackboots | smithing: Kaiser Pattern + steel piece + gold ingot |
| Steampunk Pattern | Smithing template: bronze armor to Steampunk and, with a bronze ingot, back | 4 copper ingots, 2 leather, 2 glass panes, paper (4) |
| Kaiser Pattern | Smithing template: steel armor to Kaiser and, with a steel ingot, back | Imperial Crest, 4 gold nuggets, 2 black dye, red dye, paper (4) |

**Code:** `gear/JugcraftGear` (`ARMOR_STYLES`, `STYLE_TEMPLATES`, `restyle`, `TEMPLATES`); data from `tools/gear.py` (`ARMOR_STYLES`), armor art from `tools/armor_styles.py`, pattern art from `tools/arms_variants_art.py` ([feature record](features/steampunk-and-kaiser-armor.md)).

### Scuba gear, free runners and power weapons (batch 27)

| Item | What it does | Built from |
| --- | --- | --- |
| Scuba Mask | With the scuba tank, breathe under water | steel plates, rubber, glass pane |
| Scuba Tank | Holds 8,000 mB of oxygen (use on a gas holder to fill); 1 mB a tick under water with the mask | rubber, steel plates, fluid tank |
| Free Runners | No fall damage; step up a full block | leather, steel plates, rubber |
| Power Katana | 11 damage, 1.8 attacks/s; 1,000 JE a hit, 200,000 JE full | tungsten plates, lithium cell, advanced circuit, steel ingot |
| Power Bow | Energy arrows, no ammo, 500 JE a shot, 100,000 JE full; an ordinary bow when empty | steel plates, string, lithium cell, advanced circuit |
| Plastic blocks (16 colours) | Building blocks | 8 plastic sheets around a dye make 8 |

**Code:** `gear/` (`JugcraftGear`, `ScubaTankItem`, `PowerKatanaItem`, `PowerBowItem`); plastic blocks in `chemistry/PetroBlocks`; data from `tools/gear.py` and `tools/plastic.py` ([feature record](features/gear-and-plastic.md)).

### Powered exosuit (batch 28)

| Item | What it does | Built from |
| --- | --- | --- |
| Exosuit Helmet | Netherite armor; night vision in the dark (2 JE/t) | titanium plates, processor, tinted glass |
| Exosuit Chestplate | Netherite armor; regrowing 4-heart shield (4,000 JE a half heart); jetpack | titanium plates, lithium cells, rocket pack, advanced circuit |
| Exosuit Leggings | Netherite armor; +30% speed (1 JE/t) | titanium plates, advanced circuit, neodymium magnets |
| Exosuit Boots | Netherite armor; no fall damage, full-block step (1 JE/t) | titanium plates, free runners, neodymium magnets |
| Ronin Exosuit pieces, Ronin Katana | The same, in the crimson Ronin livery | smithing: Ronin Livery + piece (or power katana) + red dye |
| Ronin / Vanguard Livery | Smithing templates that repaint the suit and katana, keeping charge | dyes around a steel plate (2) |

**Code:** `gear/` (`JugcraftExosuit`, `ExosuitItem`, `Exosuit`), `tools/Jetpack`, client `WornModelLayer`; data and art from `tools/exosuit.py` and `tools/exosuit_art.py` ([feature record](features/exosuit.md)).

### Pneumatic grapple (batch 30)

| Item | What it does | Built from |
| --- | --- | --- |
| Pneumatic Grapple | Holds 4,000 mB of nitrogen (use on a gas holder or the air separation unit to fill); 25 mB a shot. The hook flies up to 32 blocks: in a block it reels you in (no fall damage, a hop at the end), in a mob it drags the mob to you (not 100+ health). Use again to let go | gasket, chain, tripwire hook, 2 steel plates, piston, fluid tank |

**Code:** `gear/` (`JugcraftGrapple`, `PneumaticGrappleItem`, `GrappleHook`), client `GrappleHookRenderer`; data and art from `tools/grapple.py` ([feature record](features/pneumatic-grapple.md)).

### Arms (batch 42)

Each in bronze (from the tin feature) and steel (machines), crafted from the metal's ingots with sticks or leather.

| Item | What it does | Built from |
| --- | --- | --- |
| Longsword | Hits 7 (steel 7.5) at 1.3 a second, reaches 3.25 blocks; hold use to parry 60% of a blow from in front. Sweeps | 4 ingots, leather |
| Greatsword | Hits 10 (10.5) at 0.8 a second, reaches 3.75; extra knockback; stops a shield for 2 s. Sweeps | 6 ingots, leather |
| Rapier | Hits 4.5 (5) at 2 a second with quick thrusts, reaches 3.5; parries 35%. Sweeps | 2 ingots, leather |
| Flanged Mace | Hits 9 (9.5) at 0.9 a second; stops a shield for 3 s | 4 ingots, stick |
| War Hammer | Hits 11 (11.5) at 0.7 a second; knocks back; stops a shield for 5 s | 5 ingots, 2 sticks |
| Glaive | Hits 9 (9.5) at 0.9 a second, sweeping at 4.25 blocks | 3 ingots, 2 sticks |
| Halberd | Hits 10 (10.5) at 0.8 a second, thrusting through every target in line from 1 to 4.5 blocks; stops a shield for 3 s | 4 ingots, 2 sticks |
| Spear | Vanilla's spear in Jugcraft metal: jab, or hold use to charge | 1 ingot, 2 sticks |
| Lance | A spear for horsemen: 1.3 (1.4) times the charge damage, unhorses at lower speeds, reaches 2.5 to 5.5 blocks | 3 ingots, 2 sticks |

**Code:** `weapons/` (`JugcraftArms`, `ArmItem`); data from `tools/arms.py`, art from `tools/arms_art.py` ([feature record](features/arms.md)).

**Motion (batch 43).**
- Each kind holds its own guard and swings a combo of whole-body attacks: longsword forehand, backhand and thrust; greatsword sweep and cleave; rapier lunge and cut; mace smash and swing; war hammer slam and swing; glaive sweep and rising cut; halberd thrust and chop.
- Two-handed arms keep both hands on the grip. First person has its own guard and strokes.
- Looks only: damage, reach and timing are unchanged.
- Client code in `client/arms/` (`ArmsMotion`, `ArmsPose`); moves from `tools/arms_moves.py` on `tools/arms_motion.py` ([feature record](features/arms-motion.md)).

### Arms II (batch 45)

Eight more kinds, in bronze and steel as batch 42's, each with a trait.

| Item | What it does | Built from |
| --- | --- | --- |
| Dagger | Hits 4 (steel 4.5) at 2.3 a second, reaches 2.5; half again as much from behind | 1 ingot, leather |
| Sabre | Hits 5 (5.5) at 1.8 a second, reaches 3; 3 more while riding. Sweeps | 2 ingots, leather |
| Estoc | Hits 6 (6.5) at 1.4 a second with thrusts, reaches 3.5; 0.3 more per point of the foe's armor, at most 6 | 3 ingots, leather |
| Battle Axe | Hits 11 (11.5) at 0.7 a second; chops wood as an axe; stops a shield for 5 s | 4 ingots, 2 sticks |
| Flail | Hits 8 (8.5) at 1 a second; a hit slows the foe for 2 s | 2 ingots, stick |
| Scythe | Hits 8 (8.5) at 1 a second, sweeping at 4 blocks; use on ripe crops to reap and replant them, 3 by 3 | 3 ingots, 2 sticks |
| Quarterstaff | Hits 5 (5.5) at 1.6 a second, reaches 3.5; knocks back; hold use to parry 50% | 2 ingots, stick |
| Pike | Hits 8 (8.5) at 0.8 a second, reaching 2 to 5 blocks; half again against riders and mounts | 1 ingot, 3 sticks |

Each has its own guard, combo and first-person strokes, as batch 43's. **Code:** `weapons/` (`JugcraftArms.Trait`, worked by `ArmItem` on the server); data from `tools/arms.py`, art from `tools/arms_art.py`, motion from `tools/arms_moves.py` ([feature record](features/arms-ii.md)).

### Arms III (batch 46)

Two-handed swings: the greatsword, war hammer, glaive, battle axe, scythe, quarterstaff, pike and these four start their swing on the click and land the blow as it comes round, on every foe in an arc. Each combo ends in a finishing blow, 25% stronger. The wielder is slowed while swinging, and a shield in the off hand stops it. In bronze and steel:

| Item | What it does | Built from |
| --- | --- | --- |
| Zweihander | Hits 10.5 (steel 11) at 0.8 a second, reaches 4; cleaves up to 5 foes; hold use to guard 50%. Sweeps | 4 ingots, leather |
| Maul | Hits 13 (13.5) at 0.55 a second; its finishing blow shakes the ground: foes close by take half and are slowed; stops a shield for 5 s | 6 ingots, stick |
| Executioner's Sword | Hits 11 (11.5) at 0.7 a second; half again as much against a foe at 30% health or less | 5 ingots, leather |
| Bill | Hits 9 (9.5) at 0.9 a second, reaches 4.5; pulls foes towards you and drags riders from the saddle | 2 ingots, 2 sticks |

**Code:** `weapons/` (`TwoHanded`, `TwoHandedSwingPayload`, `JugcraftArms.TWO_HANDED`), client `client/arms/TwoHandedInput`; data from `tools/arms.py`, motion from `tools/arms_moves.py` ([feature record](features/arms-iii.md)).

### Arms IV (batch 47)

Five ornate arms in bronze and steel, set with a garnet (bronze) or a lit phosphor stone (steel). The labrys, battleblade and war fork swing two-handed, as Arms III's.

| Item | What it does | Built from |
| --- | --- | --- |
| Labrys | Hits 11.5 (steel 12) at 0.7 a second; its finishing blow whirls right round, striking up to 6 foes; stops a shield for 3 s | 4 ingots, 3 sticks |
| Battleblade | Hits 10 (10.5) at 0.8 a second; each hit wears every piece of the foe's armor by 4 | 6 ingots, leather |
| War Fork | Hits 8.5 (9) at 0.9 a second, reaches 4.5; half again as much to a foe coming at you | 4 ingots, 2 sticks |
| Kama | Hits 4.5 (5) at 2 a second; use on grass, ferns, vines or leaves to cut them all, 3 by 3 by 3 | 3 ingots, stick |
| War Pick | Hits 6 (6.5) at 1.4 a second; mines stone and ore as its metal's pickaxe | 3 ingots, 2 sticks |

**Code:** `weapons/` (`JugcraftArms`, `ArmItem`, `TwoHanded`); data from `tools/arms.py`, art from `tools/arms_art.py`, motion from `tools/arms_moves.py` ([feature record](features/arms-iv.md)).

### Arms V (batch 48): weapon arts

Six more arms in bronze and steel, each with a **weapon art**: a special move used with the use key, with its own animation and its own way of dealing damage. Afterwards the arm needs a few seconds before its art is ready again (shown on the hotbar); plain blows are not held back. The twinblade, nodachi, earthbreaker and moonblade swing two-handed.

| Item | Plain blows | Weapon art (ready again after) | Built from |
| --- | --- | --- | --- |
| Twinblade | 7 (steel 7.5) at 1.2 a second | **Cyclone** (6 s): three spins, each striking every foe within 3 blocks all round for half a blow and drawing them in | 4 ingots, leather |
| Nodachi | 9.5 (10) at 0.8 a second, reaches 4 | **Iaido** (8 s): a dash of about 6 blocks; every foe passed is cut for 1.3 blows a moment after, all at once | 3 ingots, leather |
| Earthbreaker | 12.5 (13) at 0.55 a second; stops a shield for 5 s | **Leap Slam** (10 s): leap and slam where you land: a blow at the centre, half at 3.5 blocks, +15% for each block landed below the take-off; throws foes up | 5 ingots, 3 sticks |
| Katar | 4.5 (5) at 2 a second | **Flurry** (5 s): five jabs of 0.28 blows that all land, then a finish of 0.9 | 4 ingots, leather |
| Moonblade | 9 (9.5) at 0.9 a second | **Crescent** (7 s): a wave runs 12 blocks ahead through every foe in line (0.9 blows, 15% less for each foe passed) until a wall stops it | 4 ingots, leather |
| Kusarigama | 5 (5.5) at 1.7 a second | **Chain Lash** (6 s): the chain catches the first foe in line up to 9 blocks off (half a blow), hauls it in, and the sickle reaps it (0.8) | 3 ingots, 2 iron nuggets |

Against one foe an art does no better than plain blows over its cooldown; its worth is many foes at once, or getting there.

**Code:** `weapons/` (`WeaponArts`, `WeaponArtPayload`, `JugcraftArms.ARTS`, `ArmItem.use`), client `client/arms/ArmsMotion`; data from `tools/arms.py`, art from `tools/arms_art.py`, motion from `tools/arms_moves.py` ([feature record](features/arms-v.md)).

### Arms VI (batch 55): katana, brazier mace, longbows, arbalests and shields

Two more arms and a war kit in bronze and steel. Shot for shot the longbow and arbalest hit harder and fly flatter than a bow and crossbow, but not a second for a second. A shield covering more than vanilla's is slower to raise.

| Item | What it does | Built from |
| --- | --- | --- |
| Katana | Hits 6 (steel 6.5) at 1.5 a second; **Seven Cuts** (6 s): seven cuts in a breath, each of 0.22 blows across every foe within reach and 110° ahead, leaving arcs of crimson (bronze) or pale gold (steel) in the air | 2 ingots, iron nugget, leather |
| Brazier Mace | Hits 8 (8.5) at 1 a second; sets foes alight for 4 s; use to light campfires, candles or the ground | 3 ingots, coal, stick |
| Longbow | Full draw in 1.3 s; arrows fly at 3.4 (3.7), against a bow's 3.0 | 2 ingots, 3 sticks, 3 string |
| Arbalest | Loads as a crossbow; bolts fly at 3.4 (3.55) with base damage 2.1, against a crossbow's 3.15 and 2.0 | 3 ingots, 2 string, tripwire hook, stick |
| Heater Shield | Blocks as a shield; raised in 0.15 s (0.1) | 2 ingots, 5 planks |
| Tower Shield | Covers 130° either side of ahead; braces against knockback (+0.4, +0.5); 8% slower while held; raised in 0.4 s (0.35) | 6 ingots, 3 planks |

**Code:** `weapons/` (`ArmBowItem`, `ArmCrossbowItem`, `JugcraftArms.RANGED`, `SHIELDS` and `KIT`, `WeaponArts` for the cuts, `ArmItem` for the fire); data from `tools/arms.py` and `tools/arms_kit.py`, art from `tools/arms_art.py` and `tools/arms_kit_art.py`, motion from `tools/arms_moves.py` ([feature record](features/arms-vi.md)).

### Arms VII (batch 56): variant arms, crafted styles and boss trophies

32 named arms, each a variant of a kind above that fights as its kind does, in steel. Sixteen are crafted at a smithing table: a style's pattern, a steel arm of the kind and the style's material, keeping its enchantments and wear. Sixteen are trophies of eight bosses still to be made ([branches/BOSSES.md](branches/BOSSES.md)): no recipe, each boss's loot table ready; creative only until then. No variant deals as much a second as a netherite sword.

| Line | Arms | Perk or boon | Made from |
| --- | --- | --- | --- |
| Gilded | longsword, rapier, sabre, halberd | enchantability 22 (gold's) | Gilder's Pattern (8 gold nuggets, paper) + gold ingot |
| Ironclad | zweihander, maul, war pick, battle axe | lasts twice as long | Ironclad Pattern (dyes, steel plate) + steel plate |
| Bonecarved | dagger, flail, glaive, labrys | Gravebane: 20% harder against the undead | Bonecarver's Pattern (bone, flint, leather, paper) + bone block |
| Runebound | nodachi, moonblade, staff, war hammer | Mark: a struck foe glows for 4 s | Runecarver's Pattern (amethyst, ectoplasm, paper) + ectoplasm |
| The Yeti King | Glacier Maul, Rimeclaw | Frost: Slowness II, 3 s | trophy |
| The Cinder Tyrant | Cinderbrand, Magmaw | Ember: alight 3 s | trophy |
| The Mire Hag | Hagthorn, Bogfang | Venom: Poison, 4 s | trophy |
| The Crypt Lich | Soulreaver, Gravewarden | Drain: heals half a heart a hit; Wither, 3 s | trophy |
| The Iron Dreadnought | Dynamo Halberd, Piston Hammer | Shock: arcs to a foe within 4 blocks for 30% of the blow | trophy |
| The Alpha Werewolf | Moonfang, Howler | Howl: Weakness, 3 s | trophy |
| The Storm Roc | Stormcaller, Galefeather | Gale: throws the foe up and back | trophy |
| The Abyssal Leviathan | Tidebreaker, Leviathan's Hook | Tide: 25% harder against a foe in water or rain | trophy |

Trophies last twice as long as steel. Glowing parts (runes, magma, venom, soul fire, sparks) are lit in the dark.

**Code:** `weapons/ArmVariants` (the variants, boons and numbers), `ArmItem` (`boon`, `boonBonus`, `shock`); data from `tools/arms_variants.py`, art from `tools/arms_variants_art.py` ([feature record](features/arms-vii.md)).

### Arms VIII (batch 59): thrown arms

Four arms in bronze and steel that fight in the hand and are thrown with the use key: hold to wind back, let go to throw, as a trident is thrown. What a throw strikes takes its damage, more for the arm's damage enchantments; then the arm comes down where it struck, as itself, to be picked up (the chakram flies back). No throw hits harder than the trident's 8, and throwing one after another deals less a second than a netherite sword.

| Item | In the hand | Thrown (bronze, steel) | Built from |
| --- | --- | --- | --- |
| Javelin | Hits 5.5 (steel 6) at 1.4 a second | 7, 8; wind 0.5 s; flies far and straight | ingot, 2 sticks, iron nugget |
| Francisca | Hits 7 (7.5) at 1.1 a second; chops wood | 6, 7; wind 0.3 s; tumbles; knocks a raised shield down for 3 s | 2 ingots, stick |
| Chakram | Hits 5 (5.5) at 1.8 a second | 4, 5 to each foe it passes, out and back; flies flat 12 blocks and returns to you | 3 ingots, leather |
| Harpoon | Hits 6 (6.5) at 1.3 a second | 5, 6; keeps its speed underwater; hauls its catch towards you and out of the saddle | 2 ingots, stick, string |

**Code:** `weapons/ThrownArmItem` (the wind and throw), `weapons/ThrownArm` (the flight and strike), `JugcraftArms.THROWN`; client `arms/ThrownArmRenderer` (each drawn as its 3D model, turned to its flight); data from `tools/arms.py`, art from `tools/arms_art.py`, motion from `tools/arms_moves.py` ([feature record](features/arms-viii.md)).

### Construction chemistry (batch 32)

| Item | What it does | Built from |
| --- | --- | --- |
| Foam Sprayer | Fills up to 12 open blocks within 2.5 of where it lands, 16 blocks away, with construction foam | 3 steel plates, iron nugget, rubber, piston |
| Foam Canister | 32 blocks of foam | chemical reactor: 2 plastic pellets, iron nugget, 250 mB ammonia |
| Construction Foam | Weak, drops nothing; cement sets it into concrete | foam sprayer |
| Cement Mix / Cement | Smelt mix into cement | calcite or bone block, clay ball, sand (makes 4 mix) |
| Concrete (+ slab, stairs) | Hardness 2.5, blast resistance 9 | 4 cement, 4 gravel, water bucket (makes 8) |
| Rebar | Steel reinforcing bar | 3 steel ingots (makes 6) |
| Blast-Proof Concrete (+ slab, stairs) | Hardness 15, blast resistance 1200 (obsidian) | 8 concrete, rebar (makes 8) |

**Code:** `chemistry/` (`ConstructionChemistry`, `FoamSprayerItem`, `CementItem`); data and art from `tools/construction.py` ([feature record](features/construction-chemistry.md)).

### Field chemistry (batch 31)

| Item | What it does | Built from |
| --- | --- | --- |
| Chlorine Grenade | 3-block cloud for 10 s: 2 damage a second through armor to what breathes | chemical reactor: steel plate, iron nugget, 500 mB chlorine (makes 2) |
| Smoke Grenade | 4-block screen for 15 s: mobs lose their target, players inside are blinded | chemical reactor: steel plate, 2 sugar, 250 mB ammonia (makes 2) |
| Thermite | Filling for thermite grenades | aluminum ingot, 2 iron dust (makes 3) |
| Thermite Grenade | Burning pool for 6 s: 4 damage a second through armor, sets alight | 2 steel plates, thermite, iron nugget (makes 4) |
| Flashbang | Blinds players who see it (4 s), staggers mobs (3 s), no damage | 2 steel plates, aluminum nugget, glowstone dust, iron nugget (makes 4) |
| Gas Mask | Keeps out chlorine, smoke and the flash; filter lasts 220 s in gas, repaired with charcoal | 3 rubber, 2 glass panes, steel plate, charcoal |
| First Aid Kit | Heals four hearts; 10 s cooldown | chemical reactor: 2 cotton, soap, 250 mB bioethanol (makes 2) |
| Antidote | Clears harmful effects, keeps good ones | chemical reactor: 2 charcoal, glass bottle, 250 mB lye |
| Stimulant | Speed II and Haste II for 60 s, with hunger | chemical reactor: 4 cocoa beans, glass bottle, 250 mB bioethanol |

**Code:** `weapons/` (`FieldChemistry`, `Warhead`, `ChemicalCloud`, `Flash`, `DescribedItem`); data and art from `tools/field_chemistry.py` ([feature record](features/field-chemistry.md)).

## Oil

The first part of the Chemistry branch: the dieselpunk oil line ([plan](branches/CHEMISTRY.md#petrochemistry-the-dieselpunk-oil-line), [feature record](features/petrochemistry.md)).

| Thing | What it does | Details | Built from |
| --- | --- | --- | --- |
| Crude Oil | A thick black fluid with a bucket; flows slowly and never makes new sources | `c:crude_oil` | reservoirs, oil sand |
| Naphtha, Diesel, Heavy Fuel Oil, Lubricant, Gasoline | Refined liquids with buckets, tagged `c:<name>` | runny / amber / thick / golden / thin red-orange | distillation tower, cracker, reformer |
| Bioethanol (batch 26) | Fuel alcohol fermented from crops, with a bucket | gas turbine 192 JE/mB, advanced engine 256 KE/mB | chemical reactor |
| Fracking Fluid | Water thickened with sand and a gelling agent, with a bucket | `c:fracking_fluid` | chemical reactor |
| Flowback Water | Dirty water from a fracked well, with a bucket | `c:flowback_water` | fracking rig |
| Refinery Gas | A gas: tanks and pipes only, no block or bucket | `c:refinery_gas` | distillation tower |
| Oil reservoirs | Hidden under Overworld chunks, fixed by the seed: conventional (about 1 chunk in 12, 50–250 buckets) or shale (about 1 in 4 of the rest, 200–800 buckets, fracking only) | finite; the prospector reports Oil and Shale oil | – |
| Pumpjack | 1 wide, 3 tall, 3 long; pumps the conventional reservoir under its wellhead | 2 mB/t at 32 JE/t, 16-bucket tank, pushes into pipes | 4 steel plates, 2 steel gears, electric pump, casing |
| Distillation Tower | 2×2, seven tall; splits crude oil into fractions, each drawn off at its own height | 1,000 mB crude → 100 refinery gas (top), 250 naphtha (layer 4), 400 diesel (layer 2), 250 heavy fuel oil (base); 100 ticks at 128 JE/t; batch 24: heavy fuel oil piped in instead → 400 mB lubricant (layer 1) + 2 asphalt binder (its slot), 120 ticks | 6 steel plates, steel tank, advanced circuit, blast furnace |
| Catalytic Cracker | 2×2, four tall; cracks heavy fuel oil into lighter fuels | 1,000 mB heavy fuel oil + 250 mB water + 1 catalyst → 500 diesel (base), 300 naphtha (layer 2), 200 refinery gas (top); 160 ticks at 160 JE/t; batch 24: 1,000 mB naphtha + 1 catalyst → 900 gasoline (layer 1) + 100 refinery gas (top), 120 ticks | steel plates, advanced circuit, 2 steel tanks, arc furnace casing, casing |
| Cracking Catalyst | Used up by the cracker, one per bucket | – | 4 bauxite, 4 sand, nickel ingot → 4 |
| Fracking Rig | 3×3, five tall; frees shale oil under its front left block | per tick: 4 mB fracking fluid down; 6 mB crude oil (base), 2 mB refinery gas (top), 3 mB flowback water (one up); 256 JE/t | steel plates, ore drill, 2 heavy pumps, advanced circuit, casing |
| Settling Plant (was Flowback Treatment Unit) | 3 wide, 1 tall, 2 deep: settling basins and a filter press | 1,000 mB flowback water → 750 mB water + 1 salt (80 ticks); batch 24: oil sand + 250 mB water → 500 mB crude oil + sand (160 ticks), bitumen + 100 mB water → 150 mB (80 ticks), mud → 4 clay balls + 250 mB water (60 ticks); 48 JE/t | steel plates, iron bars, 2 tinplate tanks, sieve, casing |
| Diesel Generator | 3 wide, 2 tall, 2 deep; an inline six burning liquid fuel | 256 JE/t: diesel 256 JE/mB (1 mB/t), heavy fuel oil 128 JE/mB (2 mB/t); 8-bucket tank, 60,000 JE buffer | 4 steel plates, electric motor, 2 tinplate tanks, casing, steel gear |
| Gas Turbine | 4 wide, 2 tall, 2 deep; burns light fuels, needs lubricant | 512 JE/t: gasoline 384 JE/mB, refinery gas 192 JE/mB; 1 mB lubricant per 20 ticks of running; 16-bucket fuel tank, 4-bucket lubricant tank, 120,000 JE buffer | 4 steel plates, 2 iron bars, 2 diesel generators, advanced circuit, steel gear |
| Polymerization Reactor | 2×2, three tall; polymerizes refinery gas | 1,000 mB refinery gas → 4 plastic pellets; 100 ticks at 96 JE/t | 4 steel plates, cracking catalyst, 2 steel tanks, glass, casing |
| Diesel Engine | 2 wide, 2 tall, 3 long; a V8 that turns a shaft | up to 512 KE/t out of the back of its upper right back block; diesel 256 KE/mB, heavy fuel oil 128; burns only for what the line takes; 8-bucket tank | 4 steel plates, 2 plastic sheets, 2 steel gears, casing |
| Premium Diesel, Premium Gasoline (batch 29) | Finished fuels with buckets | premium diesel: diesel generator and diesel engine 320, advanced engine 400 KE/mB; premium gasoline: gas turbine 448 JE/mB, advanced engine 512 KE/mB | hydrotreater |
| Hydrogen Sulfide (batch 29) | A gas: the sulfur hydrotreating takes out of diesel | 200 mB → 1 sulfur dust (chemical reactor) | hydrotreater |
| Hydrotreater (batch 29) | 2×2, three tall; its catalyst bed built in | 1,000 mB diesel + 100 mB hydrogen → 1,000 premium diesel (base) + 100 hydrogen sulfide (top), 120 ticks; 900 mB gasoline + 100 mB bioethanol → 1,000 premium gasoline, 40 ticks; 128 JE/t ([feature record](features/refinery-upgrades.md)) | 4 steel plates, cracking catalyst, 2 tinplate tanks, advanced circuit, casing |
| Heat Recovery Unit (batch 29) | One block, two tall; against a running diesel generator or gas turbine | makes 30% of the generator's output again (77 or 154 JE/t) from its exhaust; 1 mB water per 64 JE (pipes, or a water source below it); 1 mB lubricant per 40 ticks; two units share one generator's heat | 2 steel plates, iron bars, 2 copper ingots, tinplate tank, 2 steel pipes, steel gear |
| Asphalt, Asphalt Slab, Asphalt Road Line | Road blocks; walking on them is 1.3× as fast | 8 gravel + asphalt binder → 8; 3 asphalt → 6 slabs; 4 asphalt + yellow dye → 4 road line (faces the placer) | – |
| Plastic Pellets, Plastic Sheet | Pellets from the reactor; the metal press makes a sheet from each (60 ticks) | for later machines (first: the diesel engine) | – |
| Synthetic Rubber, Gasket (batch 14) | Naphtha cracked to butadiene (chemical reactor, 1,000 mB → 500 mB), polymerized to rubber (polymerization reactor, 500 mB → 4 rubber); rubber + steel plate → 4 gaskets | rubber + string → 2 belts; 2 steel plates + gasket → 4 steel fluid pipes | – |
| PVC Resin, Soap (batch 15) | Refinery gas + chlorine → vinyl chloride (synthesis converter, 250 + 250 mB → 250 mB) → 4 PVC resin per 500 mB (polymerization reactor); 2 rotten flesh + 250 mB lye → 4 soap (chemical reactor) | PVC resin → 2 plastic sheets (metal press); soap washes off every status effect | – |
| Tincal, Borax, Borosilicate Glass, Optical Fibre, Ferroboron (batch 16) | Tincal crusts desert and badlands sand, drops 1-3 borax; 2 sand + borax → 2 borosilicate glass (alloy smelter); glass → 4 optical fibre (wire drawer); iron + borax → ferroboron (alloy smelter) | rare earth oxide + ferroboron → 2 neodymium magnets; 4 microchips + advanced circuit + 2 optical fibre → processor (no gold) | – |

### Industrial chemistry

Salt, sulfur, phosphate and bauxite get their real reactions ([feature record](features/industrial-chemistry.md)).

| Thing | What it does | Details | Built from |
| --- | --- | --- | --- |
| Brine, Lye | Fluids with buckets: salt water, and sodium hydroxide solution | `c:brine`, `c:lye` | chemical reactor (2 salt + 1,000 mB water → 1,000 mB brine), electrolytic cell |
| Chlorine, Hydrogen | Gases: tanks and pipes only | `c:chlorine`, `c:hydrogen` | electrolytic cell |
| Electrolytic Cell | 3 wide, 3 tall, 2 deep; splits brine | 1,000 mB brine → 250 chlorine (top row), 250 hydrogen (middle row), 500 lye (bottom row); 200 ticks at 256 JE/t; batch 24: 1,000 mB water → 500 hydrogen (middle row) + 250 oxygen (top row), 800 ticks | 4 steel plates, 2 aluminum cables, 2 steel tanks, advanced circuit, casing |
| Sulfuric Acid | A fluid with a bucket | `c:sulfuric_acid` | chemical reactor |
| Titanium | A metal mined as rutile-bearing ore (Y −64 to −8, iron pickaxe); no furnace smelts it | raw titanium + coke + 250 mB chlorine → titanium sponge (chemical reactor); sponge → ingot (arc furnace) | – |
| Leaching | Lepidolite or monazite dissolved in sulfuric acid | 1 lepidolite + 250 mB acid → 2 lithium carbonate; 1 monazite + 250 mB acid → 2 rare earth oxide (chemical reactor) | – |
| Lithium Cell, Lithium Battery Bank | Cells of lithium carbonate in aluminum cans; a 3×2×1 electric-look battery | 2 lithium carbonate + 4 aluminum plates + copper wire → 2 cells; the bank holds 32,000,000 JE, 16,384 JE/t out of its front (see Storage) | cells: as left; bank: 4 titanium ingots, 4 cells, capacitor bank |
| Alumina | Bauxite digested in lye | 1 bauxite + 250 mB lye → 2 alumina (chemical reactor); 2 alumina + 1 coal coke → 2 aluminum ingots (electrolytic cell): two ingots per bauxite, twice the arc furnace | – |
| Fuel Cell | One block, electric look; hydrogen → JE | 128 JE/t, 1 mB hydrogen a tick (128 JE/mB); 8-bucket tank | 4 aluminum plates, 2 aluminum cables, 2 steel plates, advanced circuit, tinplate tank |
| Neodymium Magnet | Rare earths alloyed with iron | 1 rare earth oxide + 1 iron ingot → 1 (alloy smelter, 200 ticks) | – |
| Fertilizer | Superphosphate: two doses of bone meal on every crop in a 5×5 area | 2 phosphate + 250 mB sulfuric acid → 4, or 2 phosphate + 250 mB ammonia → 6 (ammonium phosphate; chemical reactor) | – |
| Chemical Reactor | 2×2×2 acid plant | 2 sulfur dust + 1,000 mB water → 1,000 mB sulfuric acid; 100 ticks at 96 JE/t; batch 29: 200 mB hydrogen sulfide → 1 sulfur dust (60 ticks); batch 24 (from the old chemical mixer): 2 salt + 1,000 mB water → 1,000 mB brine (60 ticks); 2 sand + 1 dried kelp + 1,000 mB water → 1,000 mB fracking fluid (80 ticks) | 4 steel plates, glass, 2 tinplate tanks, casing, lead ingot |
| Air Separation Unit | 2×2×6 cold box and column (batch 12) | From the air, needing only power: 8 mB/t nitrogen out of the top row, 2 mB/t oxygen out of the bottom row and 1 mB of argon every 2 ticks out of the middle at 64 JE/t; 16-bucket tanks | 4 steel plates, 2 steel pipes, 2 tinplate tanks, casing, 2 electric motors, advanced circuit |
| Synthesis Converter | 3×4×2 high-pressure loop (batch 12) | Haber–Bosch: 300 mB hydrogen + 100 mB nitrogen → 200 mB ammonia. Ostwald: 100 mB ammonia + 200 mB oxygen + 100 mB water → 200 mB nitric acid. 40 ticks at 128 JE/t | 4 steel plates, 2 titanium ingots, 2 steel pipes, casing, advanced circuit |

**Fluid processing machines** (the pumpjack and extractor are the first): input tanks take only fluids the machine's recipes use; output tanks push into neighbouring tanks and pipes; the screen shows a gauge per tank. Recipes are data in `data/jugcraft/recipe/<type>/` (see [petrochemistry.md](features/petrochemistry.md)).

### Electronics

The tier after oil and chemistry, in the **cyan look**: near-black casings with lit cyan seams, cyan glass that glows while a machine works, cyan screens, violet conduits, and a beige retro computer (the owner's references). See [electronics.md](features/electronics.md).

| Thing | What it is | Recipe or use | Built from |
| --- | --- | --- | --- |
| Silicon Wafer | Thin slices of a boule | 1 silicon boule → 8 wafers (sawmill, 200 ticks) | – |
| Lithography Station | 3×2×2 cleanroom with a monitor bank | 1 wafer + 2 copper wire + 100 mB sulfuric acid (or 50 mB nitric acid) → 4 microchips; 200 ticks at 192 JE/t; 4-bucket acid tank | glass, redstone lamp, titanium ingots, advanced circuit, aluminum plates, casing |
| Processor | The third circuit tier | 4 microchips + 1 advanced circuit + 1 gold ingot (circuit assembler, 400 ticks) | – |
| Network Terminal | A beige retro computer | Right-click: the cabled network's cables, rate, devices and stored energy. Uses no power. | glass panes, processor, plastic sheets, redstone, copper cables, button |

## Ore processing

Ore processing gives more metal per ore and turns everyday blocks into useful things, without chemistry. There are three routes for an ore, each needing more machines:

| Route | Machines | Ingots per ore | Extras |
| --- | --- | --- | --- |
| Smelt it | furnace | 1 | — |
| Crush or pulverize | Crusher → furnace, or Pulverizer → furnace | 2 | the Pulverizer rolls a byproduct |
| Wash, then pulverize | Ore Washer (water) → Pulverizer → furnace | 3 | a byproduct roll for each washed ore |
| Dissolve in acid (batch 26) | Chemical Reactor (250 mB sulfuric acid) → Pulverizer → furnace | 4 | a byproduct roll for each washed ore |

![Pulverizer, Ore Washer, Sieve, Sawmill, and the Ore Washer from behind](images/processing_machines.png)

*Approximate isometric render made from the mod's own textures and model boxes, not a game screenshot.*

| Machine | Does | Numbers | Built from |
| --- | --- | --- | --- |
| Pulverizer | Ore → 2 dust (+ byproduct); washed ore, raw metal or an ingot → 1 dust | 20 JE/t; ore takes 200 ticks, the rest 100 | flint, 2 iron gears, 2 bronze plates, cables, casing |
| Ore Washer | Ore + 500 mB water → 3 washed ore | 16 JE/t, 200 ticks; 8,000 mB tank, filled by pipes, buckets or a water source block below (a spring, 20 mB/t) | invar plates, bucket, bronze gears, basic circuit, casing |
| Sieve | Gravel → flint (12% iron nugget, 8% tin nugget); soul sand → soul soil (15% quartz, 8% gold nugget) | 8 JE/t, 100 ticks | iron plates, iron bars, hopper, cables, casing |
| Sawmill | Log → 6 planks (bamboo block → 3), 50% sawdust; planks → 3 sticks | 12 JE/t, 100 ticks (sticks 60) | iron ingots, iron gear, iron plates, cables, casing |

**Dusts** exist for copper, iron, gold, tin, zinc, lead, silver, nickel, tungsten, uranium and thallite, tagged `c:dusts/<metal>`. A dust smelts into one ingot wherever that metal's raw ore can be smelted. Nickel, tungsten and uranium dust melt in the Arc Furnace instead, like their raw ores. **Washed ores** (`washed_<metal>_ore`) are an intermediate: grind them, don't smelt them. Four **sawdust** make a sheet of paper.

**Byproducts** (pulverizing ore or washed ore): copper → gold, iron → nickel, gold → silver, tin → tungsten (5%), zinc → lead, lead → silver, silver → lead, nickel → iron, tungsten → tin, uranium → lead, thallite → iron, each 10% unless marked. The pairs follow ores that really occur together. A byproduct from a disabled feature switch is never made.

**Byproduct slots.** The Pulverizer, Sieve and Sawmill have two byproduct slots above the output. A machine waits rather than lose a byproduct: it only finishes an operation when every byproduct it might roll has room. Hoppers, pipes and *Eject* take from the byproduct slots as well as the output.

**Balance rules** (enforced by `tools/check_mod_data.py`):

- Only ore blocks get a bonus: ×2 for crushing or pulverizing, ×3 for washing.
- Byproducts may add at most 25% of the input's metal on average.
- The Sieve's renewable finds (no metal in) average under one nugget per operation.

**Recipe data.** A machine recipe may list byproducts:

```json
{"type": "jugcraft:pulverizing", "ingredient": "jugcraft:tin_ore", "result": {"id": "jugcraft:tin_dust", "count": 2}, "time": 200,
 "byproducts": [{"result": {"id": "jugcraft:tungsten_dust", "count": 1}, "chance": 0.05, "feature": "tungsten"}]}
```

## Item logistics

Item logistics moves finished goods around without hoppers everywhere. Like power and fluids, the pipes are passive; only *pushers* move items.

![Pneumatic Extractor, Item Sorter and a Brass Item Pipe segment](images/logistics.png)

*Approximate isometric render made from the mod's own textures and model boxes, not a game screenshot.*

| Block / item | Does | Numbers | Built from |
| --- | --- | --- | --- |
| Brass Item Pipe | Joins pushers to every inventory it touches (chests, machines, other mods' storage) | 6-pixel core, up to 1,024 pipes per network; items arrive instantly | 2 brass plates + glass → 6 |
| Pneumatic Extractor | Pulls from the inventory it faces and pushes out of its other five sides into pipes or inventories; a redstone signal pauses it | 16 items every 8 ticks | 4 brass plates, hopper, item pipe |
| High-Pressure Extractor | The same, four times as fast (steel tier) | 32 items every 4 ticks | 4 steel plates, piston, pneumatic extractor |
| Item Sorter | Accepts items from pipes on any side but its front, and passes only items that match its 9-slot filter into the inventory it faces | An empty filter matches nothing | 5 brass plates, comparator, hopper, 2 item pipes |
| Conveyor | Carries items the way it faces while rotation drives it; loaded by pipes, hoppers, machines or dropped items; unloads into the conveyor or inventory ahead, or onto the ground | 2.5 blocks/s, 4 stacks per conveyor; 1 KE per conveyor per tick for the whole joined run (up to 64) | 3 leather belts, 2 iron plates, iron shaft → 6 |
| Conveyor Slope | Carries items one block up or down; use with an empty hand to switch | As the conveyor | 2 conveyors, iron plate → 2 |
| Conveyor Splitter | A conveyor that sends items left, straight on and right in turn | As the conveyor | conveyor, 2 bronze gears, brass plate |
| Brass Wrench | Right-click turns a machine, extractor or sorter; sneak + right-click dismantles a Jugcraft block, dropping it and its contents | Multi-block machines cannot be turned | 4 brass ingots |

**Routing.** A network offers each item first to a sorter whose filter matches it, then to the other inventories in turn (round-robin), so one chest does not fill before the rest. The inventory the items came from never receives them back. If nothing accepts an item, it stays where it was.

**Side configuration.** Every processing machine's screen has six face buttons (F, B, L, R, T, D: front, back, left, right, top, bottom, with left and right as seen from the front). Each click cycles the face between:

- **In** — hoppers and pipes may insert ingredients;
- **Out** — hoppers and pipes may extract results;
- **Both**;
- **Off**.

The defaults keep the old behavior: ingredients in from the top and sides, results out of the bottom. The configuration is saved with the machine and is relative to its front, so turning a machine with the wrench turns its sides too.

**Eject.** The *Eject* button makes the machine push its results itself, 16 items every 8 ticks, out of every face set to Out or Both, into adjacent inventories or pipes. Multi-block machines eject from every block they occupy.

**Conveyors.** Items ride the belt as whole stacks, drawn by the client. A conveyor feeding another from the side puts items onto its middle. A running conveyor carries players and mobs too; sneak to stand still. See [conveyors](features/conveyors.md).

**Code:** `logistics/` (`ItemNetworks`, `ItemPipeBlock`, `PneumaticExtractorBlock`, `ItemSorterBlock(Entity)`, `ConveyorBlock(Entity)`, `BrassWrenchItem`, `JugcraftLogistics`; client `ConveyorRenderer`) and `machine/SideConfig.java`. Items move through Fabric's `ItemStorage.SIDED` lookup, so other mods' inventories take part automatically.

## Components

| Component | Made by | Metals | Used for |
| --- | --- | --- | --- |
| Plate | Metal Press, 1 ingot → 1 plate; thallite also by hand, 2 ingots → 1 plate | copper, iron, tin, bronze, brass, invar, aluminum, nickel, lead, tungsten, steel, titanium, thallite | Gears; the Circuit Assembler (tin plates); advanced circuits (invar); pipes (bronze) and tanks (tin); future casings and rocket hulls |
| Gear | Crafting, 4 plates of one metal | iron, bronze, brass, invar | The Circuit Assembler (bronze gear); the Electric Pump (iron); future mechanical machines |
| Wire | Wire Drawer, 1 ingot → 3 wires | copper, silver, aluminum | Circuits (copper for basic, silver for advanced); future cable tiers |
| Basic Circuit | Circuit Assembler | silicon, copper wire, solder | Future higher-tier machines and upgrades |
| Advanced Circuit | Circuit Assembler | basic circuits, silver wire, invar plate | Future high-tier machines, rocketry |

Every part carries `c:` convention tags (`c:plates/bronze`, `c:gears/iron`, `c:wires/copper` and so on), so other mods' parts can be used, and Jugcraft parts work in their recipes.

## Machine recipes are data

Every machine recipe is an ordinary Minecraft recipe file. A data pack can add, replace or remove recipes without code, and `/reload` applies the change.

| Machine | Recipe type | Folder |
| --- | --- | --- |
| Crusher | `jugcraft:crushing` | `data/<namespace>/recipe/crushing/` |
| Arc Furnace | `jugcraft:arc_smelting` | `…/arc_smelting/` |
| Metal Press | `jugcraft:pressing` | `…/pressing/` |
| Wire Drawer | `jugcraft:wire_drawing` | `…/wire_drawing/` |
| Alloy Smelter | `jugcraft:alloying` (several inputs) | `…/alloying/` |
| Circuit Assembler | `jugcraft:circuit_assembly` (several inputs) | `…/circuit_assembly/` |
| Pulverizer | `jugcraft:pulverizing` (byproducts) | `…/pulverizing/` |
| Ore Washer | `jugcraft:ore_washing` (uses 500 mB water) | `…/ore_washing/` |
| Sieve | `jugcraft:sifting` (byproducts) | `…/sifting/` |
| Sawmill | `jugcraft:sawing` (byproducts) | `…/sawing/` |
| Coke Oven | `jugcraft:coking` | `…/coking/` |
| Steel Foundry | `jugcraft:steelmaking` (several inputs) | `…/steelmaking/` |
| Electric Furnace | vanilla `minecraft:smelting` | (vanilla furnace recipes) |

```json
{"type": "jugcraft:crushing", "ingredient": "#c:ores/tin", "result": {"id": "jugcraft:raw_tin", "count": 2}, "time": 160}
{"type": "jugcraft:alloying",
 "ingredients": [{"ingredient": "minecraft:copper_ingot", "count": 3}, {"ingredient": "jugcraft:tin_ingot", "count": 1}],
 "result": {"id": "jugcraft:bronze_ingot", "count": 4}, "time": 200}
```

- `ingredient` accepts an item, a list or a `#tag`.
- `time` is in ticks.
- Multi-input ingredients can sit in any input slot, and every unused slot must be empty.
- Jugcraft's own recipes are generated from `tools/machines.py`, where the metal audit runs. Edit them there, not in the JSON.
- Code: `machine/MachineRecipe.java` and `MultiMachineRecipe.java` (formats), `MachineRecipeTypes.java` (registration), `MachineRecipes.java` (lookup).
- **Recipe viewers:** with JEI installed, every machine (fluid machines included, with their fluids in and out) has a recipe category. EMI and REI have no plugin yet.

## Engineer's Handbook

An in-game guide: craft a **book + a copper ingot** and right-click it.

- Chapters are on the left: Getting Started, Materials, Power, Processing, Ore Processing, Steel, Fluids, Logistics and Upgrades.
- Each page shows the block's icon, what it does, its power use, its crafting grid and example recipes (with byproduct chances).
- Hover over any item for its name.

The content (`assets/jugcraft/handbook/en_us.json`) is **generated by `tools/handbook.py`** from the same tables as the mod, so numbers and recipes can't drift from the game. Only the prose is written by hand. `check_mod_data.py` verifies every item the book shows. The book is English-only for now.

## Automated game tests

`src/gametest/` holds in-game tests that `./gradlew build` runs on a headless server (Fabric game test API), so CI checks them on every pull request. They cover:

- crushing recipes loading from data;
- the crusher doubling ore;
- a coal generator powering an electric furnace through cables;
- a pump filling a tank through pipes;
- the 3×2×6 alloy smelter placing all 36 blocks, taking power only at its socket, and making bronze;
- a multi-block machine disappearing whole when one block breaks;
- an extractor moving items through pipes into a chest;
- a sorter routing matching items to its inventory and the rest elsewhere;
- a machine ejecting its results into a chest below;
- recipe byproducts loading from data;
- the pulverizer grinding ore into two dusts, and a dust smelting into an ingot;
- the ore washer tripling ore with water from a source below, and waiting when it has none;
- the sawmill and the sieve;
- the coke oven and steel foundry working without power;
- cables connecting only where power goes in (never to unpowered machines; only to the alloy smelter's socket);
- speed and efficiency upgrades, the "high" redstone mode, comparator output and upgrade-slot isolation;
- cable tiers setting a network's rate, and the high-pressure extractor.

**Client game tests** (`JugcraftClientGameTests`) start a real game client with software rendering in CI (three `client (shard n of 3)` jobs share the test classes out; the `client` job passes when all three do). They:

- build a showroom of every machine;
- open a machine screen and the handbook;
- save screenshots, kept as the `client-screenshots-0` to `-2` artifacts of each CI run (one per shard).

`PixelHollowsGameTests` adds the Pixel Hollows and Retro Trader tests: cluster and block drops, the lamp, cabinet and stonecutter recipes, the biome in the Overworld's climate table, the Overworld's feature order, the map, the trader's trades and no-profit rule, the cabinet as a job site, a villager claiming it, and the shop template.

These are real game renders, but they are not play-testing: nobody is steering the game.

To add a test, write a public method annotated `@GameTest` in `JugcraftGameTests` that builds its setup and ends with `helper.succeed()` or `helper.succeedWhen(...)`.

## Rules that keep it balanced

Full numbers, conversion losses and the loops that were checked: [BALANCE.md](BALANCE.md).

- **No free metal.** Every recipe keeps or loses metal: plates 1:1, 4 plates → 1 gear, 1 ingot → 3 wires, and alloys at exact ratios. The only gain is the crusher's ore doubling, defined once for all ores. `tools/check_mod_data.py` audits every recipe, including two- and three-input machine recipes.
- **Nothing is hand-only or machine-only without reason.** Bronze has a hand route; plates, wires and circuits need their machines, because processing is what those machines are for. The one exception is thallite's plate, which also has a dearer hand route (2 ingots → 1 plate) for the Earth school's metal ([thallite.md](features/thallite.md)).
- **Stand-ins are temporary.** Blast-furnace and arc-furnace recipes that really need chemistry are listed in [branches/CHEMISTRY.md](branches/CHEMISTRY.md) and will move there without changing item IDs.

## Where to change things

| To change | Edit | Then run |
| --- | --- | --- |
| Materials, parts, tags, worldgen | `tools/materials.py` | `python3 tools/generate_material_data.py` |
| Pixel Hollows and the Retro Trader (blocks, recipes, worldgen, biome, sounds, trades) | `tools/pixel_hollows.py` (plus `world/` in Java); looks: `tools/pixel_hollows_textures.py`, `tools/retro_models.py`; the shop: `tools/retro_game_shop.py`; the loop: `tools/pixel_hollows_sound.py` | `python3 tools/generate_material_data.py` (and the tool you changed) |
| Machines, machine recipes, crafting | `tools/machines.py` (plus the matching Java: `MachineKind`, `JugcraftComponents`) | `python3 tools/generate_material_data.py` |
| Machine looks | `tools/steampunk_models.py` and `tools/steampunk_textures.py` (steampunk), `tools/large_machines.py` (classic multi-block models), `DEFAULT_STYLE` in `tools/model_writer.py` | `python3 tools/generate_textures.py`, then `python3 tools/generate_material_data.py` |
| Fluid blocks and their numbers | `tools/machines.py` (`PIPES`, `FLUID_BLOCKS`, `FLUID_STATS`) plus the constants in `fluid/FluidPipeBlock`, `FluidTankBlockEntity` and `ElectricPumpBlockEntity` | `python3 tools/generate_material_data.py` |
| Ore processing | `tools/machines.py` (`_pulverizer`, `_ore_washer`, `SIEVE`, `_sawmill`, `BYPRODUCTS`), `COMPONENTS["dust"]` and `WASHED_ORES` in `tools/materials.py`, plus `MachineKind` | `python3 tools/generate_material_data.py` |
| Item logistics | `tools/machines.py` (`ITEM_PIPES`, `LOGISTICS_BLOCKS`, `TOOLS`), `tools/logistics_models.py`, plus the constants in `logistics/` | `python3 tools/generate_material_data.py` |
| Textures | `tools/generate_textures.py` | `python3 tools/generate_textures.py` |
| Crops, seeds, foods, sickles, wild plants, trellis, Cooking Pot recipes, gourds, cranberries, the chestnut tree and its wood (Agriculture) | `tools/agriculture.py` (plus the matching Java in `agriculture/`) | `python3 tools/generate_material_data.py` |
| Crop and farm-item textures | `tools/crop_textures.py` and `tools/kitchen_textures.py` (previews: `tools/render_agriculture.py`) | `python3 tools/generate_textures.py` |
| Material sets: the metals' ingots, nuggets and storage blocks, the ores, raw ores and raw blocks, the bronze and steel tools ([MATERIAL_SETS.md](MATERIAL_SETS.md)) | the maps in `tools/material_icons/*.txt`; ramps and ore models in `tools/material_icons.py` | `python3 tools/generate_textures.py` and `python3 tools/generate_material_data.py` |
| Verify | — | `python3 tools/check_mod_data.py` and `./gradlew build` |
