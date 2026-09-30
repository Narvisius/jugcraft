# Jugcraft technology tree

How every implemented material, machine and part works and connects. **Implemented** means it is in the mod and compiles in CI; nothing here has been play-tested yet. Planned work is marked **planned** and links to its branch document.

## Branches

| Branch | Status | Scope |
| --- | --- | --- |
| **Materials** | Implemented | Ores, raw materials, ingots and alloys. See [base-materials.md](features/base-materials.md). |
| **Power** | Implemented | Generators, batteries and cables (JE energy). See [machines-and-power.md](features/machines-and-power.md). |
| **Mechanical processing** | Implemented | Physical transformation of materials: smelting, crushing, alloying, pressing, drawing, assembling. |
| **Fluids** | Implemented | Pipes, tanks and pumps that move and store water, lava and other mods' fluids; physical only, no reactions. See [Fluids](#fluids) below. |
| **Chemistry** | **Planned** | Reactions that change what a substance *is*: electrolysis, acids, fertilizer, refining. See [branches/CHEMISTRY.md](branches/CHEMISTRY.md). |

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

1. **Mine and smelt** tin, zinc, lead and copper with a stone pickaxe and a vanilla furnace. Make **bronze** by hand: 3 copper + 1 tin, crafted into bronze blend, then smelted.
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
   - **Wind Turbine** (3 blocks tall, aluminum plates): free power that grows with height.
   - **Geothermal Generator** (2 blocks wide, needs a basic circuit): runs on lava. An electric pump on lava feeds it through pipes.
9. **Fluids:** **Bronze Fluid Pipes** and the **Tinplate Tank** are crafted from press-made plates; the **Electric Pump** adds iron gears, a bucket and a casing. A pump on water, piped to a steam generator, keeps the boiler full without buckets.

## Machines

All machines hold their own internal battery and accept power from cables or directly from an adjacent generator or battery. Where power goes in is described in [Power connections](#power-connections). Hoppers insert into input slots from the top and sides and extract results from the bottom.

| Machine | Branch | Does | Power | Built from |
| --- | --- | --- | --- | --- |
| Coal Generator | Power | Burns coal, charcoal or coal blocks → 32 JE/t | produces | bronze, cable, furnace, casing |
| Steam Generator | Power | Boils water with coal or bitumen → 64 JE/t | produces | coal generator, bronze, bucket, cable, casing |
| Solar Panel | Power | Daylight under open sky → 8 JE/t (4 in rain) | produces | glass, silicon, bronze, cable |
| Battery Box | Power | Stores 400,000 JE; outputs from its front | stores | lead, cable, redstone block, casing |
| Electric Furnace | Mechanical | Any vanilla smelting recipe, 100 ticks | 10 JE/t | bronze, redstone, cable, furnace, casing |
| Crusher | Mechanical | Ore → 2 raw; minerals, sulfur, oil sand, cobble → gravel → sand | 16 JE/t | flint, cable, casing, bronze, redstone |
| Alloy Smelter (2×2 multi-block) | Mechanical | Two ingredients (any order) → bronze, brass, invar, solder. Power **only** through its copper socket | 20 JE/t | bronze, cable, 2 furnaces, casing, redstone |
| Metal Press | Mechanical | Ingot → plate (1:1) | 16 JE/t | bronze, piston, cable, casing, anvil |
| Wire Drawer | Mechanical | Ingot → 3 wires | 12 JE/t | brass, shears, cable, casing, redstone |
| Circuit Assembler | Mechanical | Up to three ingredient stacks (any order) → circuits | 32 JE/t | tin plates, bronze gear, cable, casing, redstone |
| Geothermal Generator (2 blocks wide) | Power | Lava → 64 JE/t (1 mB/t; a bucket lasts 1,000 ticks) | produces | invar plates, tinplate tank, bronze gears, casing, basic circuit |
| Wind Turbine (3 blocks tall) | Power | 4–24 JE/t by height above sea level; more in rain and thunder; rotor needs clear air | produces | aluminum plates, bronze gears, casing, bronze plates, cable |
| Arc Furnace (3×3×3 multiblock) | Mechanical (with chemistry stand-ins) | Quartz → 2 silicon; raw nickel, tungsten or uranium → ingot; bauxite, lepidolite and monazite stand-ins | 64 JE/t | 26 arc furnace casings (bricks + nickel) + controller |

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
2. Author its model once in `tools/large_machines.py`. The model is written in structure coordinates, with the main block at 0–16 on each axis.
3. Run `python3 tools/generate_material_data.py`. It slices the model into one model per block and writes the blockstates for every facing and part, plus a scaled-down model for the inventory.
4. `tools/check_mod_data.py` checks that the Python and Java footprints match and that no model leaves Minecraft's −16…32 limit.

Code: `machine/Footprint.java` (offsets and rotation) and `machine/LargeMachineBlock.java` (placing, breaking, forwarding to the main block).

## Power connections

Every powered block follows one of two rules, and a cable shows which by where it connects:

| Rule | Machines | What you see |
| --- | --- | --- |
| **Any side** | Every one-block machine, generator and battery; the electric pump; the geothermal generator and wind turbine (any face of any of their blocks) | A cable next to any face bends to it and connects. A generator or battery placed directly against the machine also powers it. |
| **Power socket only** | The Alloy Smelter | Its **copper socket in a yellow-and-black frame**, on the outer side of its lower right block (seen from the front). Cables connect only there; a cable along any other face doesn't bend toward it. |

The battery box is "any side" for charging. It gives power out only through its front, and a cable at the front still connects.

In code, one check decides both the drawn connection and the flow. `MachineKind.powerPort()` (null means any side) is read by the energy lookup, and cables connect only where that lookup answers. `tools/large_machines.py` (`POWER_PORTS`) places the socket in the model, and `check_mod_data.py` keeps the two in sync.

## Cables and pipes

Copper Cable and the Bronze Fluid Pipe are *transmitters*, built like the ones in other tech mods (Mekanism, Thermal, IC2):

- **Thin, not full blocks.** Each is a 4-pixel (¼-block) core. An arm reaches out toward each connected neighbor, and the hitbox follows the same shape. For comparison, Mekanism's cables and pipes are 6 pixels.
- **Connect automatically.**
  - A cable joins other cables and any block that stores or uses energy on the touching face.
  - A pipe joins other pipes and any block with a fluid storage on that face, including tanks, pumps, the steam generator, vanilla cauldrons and other mods' fluid blocks.
  - Cables and pipes never connect to each other, so they can run side by side.
- **Visual connection = real connection.** The arms use the same lookup (`EnergyStorage.SIDED` / `FluidStorage.SIDED`) as the transfer code, so if it looks connected it is connected.
![Cables between a coal generator and machines; a pipe from a pump to a tank](images/transmitters.png)

*Approximate isometric render made from the mod's own textures and model boxes, not a game screenshot.*

- **Look:**
  - The cable is black rubber insulation with copper connectors. The pipe is bronze, darker at its flanged ends.
  - In the inventory both show as a short 3D segment.

## Fluids

The fluid branch moves liquids around. It never changes what a liquid *is*: that is chemistry.

| Block | Does | Numbers | Built from |
| --- | --- | --- | --- |
| Bronze Fluid Pipe | Carries fluid pushed into it by a pump to every fluid storage it touches | 250 mB/t per push, up to 1,024 pipes per network | 2 bronze plates + glass → 4 |
| Tinplate Tank | Stores one fluid; fill or empty with buckets, right-click with an empty hand to read it, comparators show how full it is | 16,000 mB (16 buckets); contents are lost if broken | 8 tin plates + glass |
| Electric Pump | Pulls from below, pushes out of its top and four sides | 100 mB/t, 8 JE per tick it moves fluid, 4,000 JE battery, 4,000 mB buffer | bronze plates, bucket, 2 iron gears, casing, cable |

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

## Components

| Component | Made by | Metals | Used for |
| --- | --- | --- | --- |
| Plate | Metal Press, 1 ingot → 1 plate | copper, iron, tin, bronze, brass, invar, aluminum, nickel, lead, tungsten | Gears; the Circuit Assembler (tin plates); advanced circuits (invar); pipes (bronze) and tanks (tin); future casings and rocket hulls |
| Gear | Crafting, 4 plates of one metal | iron, bronze, brass, invar | The Circuit Assembler (bronze gear); the Electric Pump (iron); future mechanical machines |
| Wire | Wire Drawer, 1 ingot → 3 wires | copper, silver, aluminum | Circuits (copper for basic, silver for advanced); future cable tiers |
| Basic Circuit | Circuit Assembler | silicon, copper wire, solder | Future higher-tier machines and upgrades |
| Advanced Circuit | Circuit Assembler | basic circuits, silver wire, invar plate | Future high-tier machines, rocketry |

Every part carries `c:` convention tags (`c:plates/bronze`, `c:gears/iron`, `c:wires/copper` and so on), so other mods' parts can be used, and Jugcraft parts work in their recipes.

## Rules that keep it balanced

- **No free metal.** Every recipe keeps or loses metal: plates 1:1, 4 plates → 1 gear, 1 ingot → 3 wires, and alloys at exact ratios. The only gain is the crusher's ore doubling, defined once for all ores. `tools/check_mod_data.py` audits every recipe, including two- and three-input machine recipes.
- **Nothing is hand-only or machine-only without reason.** Bronze has a hand route; plates, wires and circuits need their machines, because processing is what those machines are for.
- **Stand-ins are temporary.** Blast-furnace and arc-furnace recipes that really need chemistry are listed in [branches/CHEMISTRY.md](branches/CHEMISTRY.md) and will move there without changing item IDs.

## Where to change things

| To change | Edit | Then run |
| --- | --- | --- |
| Materials, parts, tags, worldgen | `tools/materials.py` | `python3 tools/generate_material_data.py` |
| Machines, machine recipes, crafting | `tools/machines.py` (plus the matching Java: `MachineKind`, `JugcraftComponents`) |
| Fluid blocks and their numbers | `tools/machines.py` (`PIPES`, `FLUID_BLOCKS`, `FLUID_STATS`) plus the constants in `fluid/FluidPipeBlock`, `FluidTankBlockEntity` and `ElectricPumpBlockEntity` | `python3 tools/generate_material_data.py` |
| Textures | `tools/generate_textures.py` | `python3 tools/generate_textures.py` |
| Verify | — | `python3 tools/check_mod_data.py` and `./gradlew build` |
