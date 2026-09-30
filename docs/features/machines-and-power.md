# Machines and electricity

Status: implemented in source; **not yet played**. Compilation is checked by the Build workflow.
Proposal issue: none yet; requested directly by the owner on 30 September 2026.
Owner: @jimbozoomer-byte
Target milestone and tier: Workshops (technology), first powered tier
Primary specialty and supported player role: engineering

## Player experience
Burn coal in a **Coal Generator**, run **Copper Cable** to your machines, and store surplus in a **Battery Box**. Every machine has its own internal battery that holds charge, so machines keep working briefly after the power stops, and a generator placed directly against a machine powers it without cable.

Machines look steampunk by default. The original look is a built-in resource pack, **Jugcraft: Classic Machines**; see [machine looks](../TECH_TREE.md#machine-looks-steampunk-and-classic).

| Block | What it does | Energy (JE) |
| --- | --- | --- |
| Coal Generator | Burns coal/charcoal (1600 ticks) or coal blocks (16000 ticks) | Makes 32/t while burning, holds 16,000, outputs 64/t to every side. Stops burning when full, so fuel is never wasted |
| Solar Panel | Generates under open sky in daylight; no fuel. Crafted with silicon (from the arc furnace) | 8/t in sun, 4/t in rain, 0 at night; holds 4,000; outputs 32/t |
| Steam Generator | Boils water with coal, charcoal, coal blocks or **bitumen** (800 ticks) | 64/t while boiling (twice the coal generator per fuel); uses 10 mB water per tick from an 8-bucket tank; holds 40,000; outputs 128/t. Water comes from buckets (top slot, or right-click the generator with one), a **water source block directly beneath** (20 mB/t), or any pump or pipe on any side; empty buckets come out the bottom |
| Copper Cable | Connects generators, batteries and machines; shows connections visually | Up to 256/t per push; energy is split evenly between receivers |
| Battery Box | Stores energy | Holds 400,000; charges from any side except the front, discharges 256/t out of the **front** |
| Electric Furnace | Smelts anything the vanilla furnace can | 10/t, 100 ticks per item (twice the vanilla furnace's speed); holds 10,000 |
| Crusher | Ore → 2 raw ore; mineral ores → extra minerals; sulfur → 6 sulfur dust; oil sand → 3 bitumen; cobblestone → gravel → sand | 16/t; holds 10,000 |
| Alloy Smelter (3 wide, 2 deep, 6 tall; power only through its copper socket on the outer side of the lower right front block) | Two ingredient slots, either order: 3 copper + 1 tin → 4 **bronze**; 3 copper + 1 zinc → 4 **brass**; 2 iron + 1 nickel → 3 **invar**; 1 tin + 1 lead → 2 **solder**. Every ratio conserves metal | 20/t; holds 10,000; 120–240 ticks per batch |
| Metal Press | Ingot → plate (copper, iron, tin, bronze, brass, invar, aluminum, nickel, lead, tungsten). Four plates craft a gear (iron, bronze, brass, invar) | 16/t; holds 10,000; 100 ticks |
| Wire Drawer | Ingot → 3 wires (copper, silver, aluminum) | 12/t; holds 10,000; 100 ticks |
| Circuit Assembler | Three ingredient slots, any order: silicon + 3 copper wire + solder → basic circuit; 2 basic circuits + 3 silver wire + invar plate → advanced circuit | 32/t; holds 20,000; 200–300 ticks |
| Electric Pump | Pulls 100 mB/t from a tank or water/lava source below; pushes out of its top and sides into pipes or storages (see [TECH_TREE.md](../TECH_TREE.md#fluids)) | 8/t while moving fluid; holds 4,000 |
| Geothermal Generator (2×2×2) | Burns lava from its tank, filled by bucket, pump or pipe (see [large machines](../TECH_TREE.md#multi-block-machines)) | 64/t using 1 mB lava per tick (a bucket lasts 1,000 ticks); 4,000 mB tank; holds 30,000; outputs 128/t from any of its blocks |
| Wind Turbine (9 blocks tall, 7-block rotor) | Turns when the 7×7 square its rotor sweeps, in front of the top, is clear (checked every 5 seconds); the rotor is drawn by the client and spins while it runs | 12/t at sea level, +1 per 2 blocks higher, up to 72/t; ×1.5 in rain, ×2 in thunder; holds 48,000; outputs 192/t |
| Arc Furnace (multiblock) | High-temperature processing: quartz → 2 silicon, bauxite → aluminum ingot, raw nickel/tungsten/uranium → ingots, lepidolite → 2 lithium carbonate, monazite → 2 rare earth oxide | 64/t; holds 50,000 |

**Arc furnace structure:** a solid 3×3×3 cube of 26 Arc Furnace Casing blocks, with the Arc Furnace Controller in the center of one face, facing outward. Feed power into the controller's front. The screen shows whether the structure is formed; the structure is rechecked every second.

Hoppers work with every machine: by default they insert into the input (or fuel) slot from the top or sides and pull results from the bottom. Processing machines' faces can be reconfigured on their screen ([item logistics](item-logistics.md)).

## Crafting (every part uses Jugcraft metals)
- **Machine Casing:** bronze and zinc.
- **Copper Cable:** 2 copper and 1 tin make 6 cables.
- **Coal Generator:** bronze, cable, furnace and a casing.
- **Battery Box:** lead, cable, a redstone block and a casing.
- **Electric Furnace:** bronze, redstone, cable, furnace and a casing.
- **Crusher:** flint, cable, a casing, bronze and redstone.
- **Arc Furnace Casing:** bricks and nickel make 8.
- **Arc Furnace Controller:** nickel, cable, redstone, a casing and a blast furnace.
- **Alloy Smelter:** bronze, cable, two furnaces, a casing and redstone.
- **Metal Press:** bronze, a piston, cable, a casing and an anvil.
- **Wire Drawer:** brass, shears, cable, a casing and redstone.
- **Circuit Assembler:** tin plates, a bronze gear, cable, a casing and redstone.
- **Geothermal Generator:** invar plates, a tinplate tank, bronze gears, a casing and a basic circuit. **Wind Turbine:** aluminum plates, bronze gears, a casing, bronze plates and cable.
- **Bronze Fluid Pipe** (×4): two bronze plates and glass. **Tinplate Tank:** eight tin plates and glass. **Electric Pump:** bronze plates, a bucket, two iron gears, a casing and cable.
- **Solar Panel:** glass, silicon, bronze and cable.
- **Steam Generator:** bronze, a bucket, cable, a casing and a coal generator (an upgrade of it).

The first powered setup (generator, cable and electric furnace) needs only tin, zinc, copper, iron-tier tools and vanilla items. Nickel for the arc furnace is the step up.

How everything connects is documented in [../TECH_TREE.md](../TECH_TREE.md); future machines are proposed in [../MACHINE_ROADMAP.md](../MACHINE_ROADMAP.md).

## Connections
- Input producers: coal/charcoal; every Jugcraft ore and material.
- Output consumers: the crusher and arc furnace replace the blast-furnace stand-ins with better yields. Bronze, zinc, lead, tin and nickel finally have uses.
- Required vs optional: nothing here gates vanilla or magic progression. The blast-furnace stand-ins remain as slower, lossy routes.

## Balance and automation
- Coal in a generator gives 51,200 JE, enough for 5,120 electric-furnace ticks: about 51 items, versus 8 in a vanilla furnace.
- The **ore-processing bonus** (crusher ore → 2 raw) is defined once (`ORE_PROCESSING_MULTIPLIER`) and applies to every ore the same way. `tools/check_mod_data.py` audits every crusher and arc furnace recipe: only ores may gain, and nothing else creates metal.
- The electric furnace does not award smelting XP (a deliberate simplification that also prevents XP duplication).
- Idle machines only check their input; cables have no block entities.

## Multiplayer and persistence
- **Server-authoritative:** all logic runs on the server, the screens only display synced values, and clients can't write machine state.
- **Energy transfers** use Fabric transactions, so energy is never duplicated or lost by a failed transfer.
- **Cable networks** are found by a search capped at 2,048 cables, cached per world, and rebuilt when any cable or machine next to a cable changes.
- **Saving:** machines save items, energy, progress and burn time.
- **Breaking a machine** drops its items; the stored energy is lost.
- **Config switch:** `machines.enabled=false` in `config/jugcraft.properties` removes the machine crafting recipes and the crusher and arc furnace recipes. Placed machines keep existing.

Not yet designed: ownership and permissions (anyone can open any machine) and chunk-unload behavior beyond vanilla block entities (machines pause when unloaded). Per-side configuration now exists; see [item logistics](item-logistics.md).

## Dependencies and assets
- **Dependencies:** Fabric API only. Jugcraft defines its own energy API (`EnergyStorage.SIDED`, JE); an adapter to Team Reborn Energy can come later, once that library supports 26.3.
- **Textures:** all original, generated by `tools/generate_textures.py` (MIT).

## Verification
Run in a sandbox without Minecraft (30 September 2026):
- `python3 tools/check_mod_data.py`: PASS (87 IDs, including machine models, loot, crafting, the machine recipe audit and Java stats sync).
- Negative test: a crusher recipe tripling raw tin was flagged three ways (stale file, bonus on a non-ore, metal gain).
- `javac`: no syntax errors.
- The Minecraft/Fabric APIs used were taken from Minecraft 26.3 classes decompiled in CI (the vanilla furnace, menu and screen) and from the fabric-api 0.161.0+26.3 source.

Not run: in-game use, dedicated server with two clients, cable networks under load, performance.

## Open questions
- Should the battery box keep its charge when broken?
- Should machines have ownership or locking?
- Faster cable tiers, more generators (solar, steam), upgrades.
- Should the electric furnace award XP?
