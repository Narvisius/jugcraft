# What exists in Jugcraft

A map of everything built so far, written for AI agents and contributors who need to add features **alongside** or **on top of** the existing systems. It lists content, the shared APIs to call, the data formats, where each thing lives, and the rules the checks enforce.

- Player-facing explanations: [TECH_TREE.md](TECH_TREE.md).
- Per-feature records: [features/](features/).
- History of changes: [CHANGELOG.md](../CHANGELOG.md).

> **Status.** Everything here compiles and loads in CI. Where a feature has an automated game test, that test passes on a headless server. Nothing has been play-tested in a client or on a dedicated server with two players yet.
>
> This document describes `main` after PRs #4–#24. Update it whenever you add, rename or remove a system, so it stays the map other contributors rely on.

## Quick facts

| | |
| --- | --- |
| Platform | Minecraft Java 26.3, Fabric Loader 0.19.3, Fabric API 0.161.0+26.3, Loom 1.17, JDK 25 (pins in [PLATFORM.md](PLATFORM.md); do not change them without a platform PR) |
| Mod ID / namespace | `jugcraft` (`Jugcraft.MOD_ID`, `Jugcraft.id(path)`) |
| Java package | `io.github.jimbozoomer.jugcraft` |
| Mappings | Mojang names (e.g. `Identifier`, `BlockEntity`, `AbstractContainerMenu`) |
| Energy unit | **JE** (Jugcraft Energy), `long`, per tick |
| Fluid unit | **mB** in Jugcraft numbers. Fabric counts droplets: `FluidNetworks.DROPLETS_PER_MB` = 81 |
| Metal accounting | nugget units: nugget 1, ingot/raw/ore/dust/washed ore/plate 9, wire 3, gear 36, block 81 |
| Authority | All logic runs on the server; screens only show synced `ContainerData` |
| Registered IDs | 172 items/blocks under `jugcraft:` (the checker counts them) |

## Build, generate, check

Most JSON (models, blockstates, lang, loot, recipes, tags, worldgen) and every texture are **generated**. Edit the Python source of truth, not the JSON.

| Command | Does |
| --- | --- |
| `python3 tools/generate_textures.py` | Draws every texture (original pixel art, deterministic seeds) |
| `python3 tools/generate_material_data.py` | Writes all generated JSON, both machine styles and the built-in alternate pack |
| `python3 tools/check_mod_data.py` | Offline audit: IDs, assets, loot, tags, worldgen, recipe metal balance, Python ↔ Java sync |
| `python scripts/check_repository.py` | Repository structure and Markdown links |
| `./gradlew build` | Compiles and runs the **game tests** on a headless server (needs Fabric/Mojang Maven access; CI has it) |

CI runs all of these and fails if generated files are out of date.

## Content inventory

### Materials (`materials/`, `tools/materials.py`)

| Metal | Forms | Source | Feature switch |
| --- | --- | --- | --- |
| tin, zinc, lead, silver, nickel, tungsten, uranium | ore, deepslate ore, raw, raw block, ingot, nugget, block | worldgen ores | own name (`tin`, `zinc`, …) |
| aluminum | ingot, nugget, block | bauxite (arc furnace; blast-furnace stand-in gives a nugget) | `aluminum` |
| bronze | ingot, nugget, block, `bronze_blend` | 3 copper + 1 tin (hand blend or alloy smelter) | `tin` |
| brass, invar, solder | ingot, nugget, block | alloy smelter only | `zinc`, `nickel`, `lead` |
| steel | ingot, nugget, block; plate, gear | steel foundry (iron + coke) | `machines` |

| Other | IDs | Notes |
| --- | --- | --- |
| Minerals | `salt`, `phosphate`, `lepidolite`, `monazite` (+ `_ore`, `deepslate_…_ore`, `…_block`) | ores drop several items |
| Rocks | `bauxite` (block), `oil_sand` (block, drops `bitumen`) | biome-limited surface worldgen |
| Items | `bitumen`, `sulfur_dust`, `silicon`, `lithium_carbonate`, `rare_earth_oxide`, `coke` (`c:coal_coke`) | several are chemistry stand-ins; coke is fuel and steel's carbon |
| Components | `<metal>_plate` ×11, `<metal>_gear` ×5, `<metal>_wire` ×3, `basic_circuit`, `advanced_circuit` | `JugcraftComponents`; tags `c:plates/…`, `c:gears/…`, `c:wires/…` |
| Ore processing | `<metal>_dust` ×10 (copper, iron, gold, tin, zinc, lead, silver, nickel, tungsten, uranium), `washed_<metal>_ore` ×10, `sawdust` | tags `c:dusts/<metal>` |

Every Jugcraft metal and part carries `c:` convention tags (`c:ingots/tin`, `c:ores/tin`, `c:raw_materials/tin`, `c:storage_blocks/tin`, `c:nuggets/tin`, `c:dusts/tin`, …). **Use tags in recipes** so other mods' equivalents work.

### Machines (`machine/MachineKind`)

Every machine is one `MachineBlock` + `MachineBlockEntity` whose behavior comes from its `MachineKind`.

- **Slot order:** inputs, the output, any byproduct slots, then (powered processors only) two upgrade slots.
- `kind.slots` ends before the upgrade slots, and `kind.containerSize()` includes them.

| `MachineKind` | ID | Role | Energy (cap / in / out / use per tick) | Slots | Recipe type |
| --- | --- | --- | --- | --- | --- |
| COAL_GENERATOR | coal_generator | burns coal → 32 JE/t | 16k / 0 / 64 / – | fuel | – |
| BATTERY_BOX | battery_box | storage; outputs from the front only | 400k / 256 / 256 / – | – | – |
| ELECTRIC_FURNACE | electric_furnace | vanilla smelting, 100 ticks | 10k / 128 / 0 / 10 | in, out | vanilla `smelting` |
| CRUSHER | crusher | ore ×2, minerals, gravel/sand | 10k / 128 / 0 / 16 | in, out | `jugcraft:crushing` |
| ARC_FURNACE | arc_furnace_controller | 3×3×3 casing structure | 50k / 512 / 0 / 64 | in, out | `jugcraft:arc_smelting` |
| SOLAR_PANEL | solar_panel | 8 JE/t in sun | 4k / 0 / 32 / – | – | – |
| STEAM_GENERATOR | steam_generator | coal/bitumen + water → 64 JE/t | 40k / 0 / 128 / – | fuel, water bucket, empty bucket | – |
| ALLOY_SMELTER | alloy_smelter | **2×2 multi-block**, power socket only | 10k / 128 / 0 / 20 | 2 in, out | `jugcraft:alloying` (multi) |
| METAL_PRESS | metal_press | ingot → plate | 10k / 128 / 0 / 16 | in, out | `jugcraft:pressing` |
| WIRE_DRAWER | wire_drawer | ingot → 3 wires | 10k / 128 / 0 / 12 | in, out | `jugcraft:wire_drawing` |
| CIRCUIT_ASSEMBLER | circuit_assembler | circuits | 20k / 256 / 0 / 32 | 3 in, out | `jugcraft:circuit_assembly` (multi) |
| GEOTHERMAL_GENERATOR | geothermal_generator | **2 wide**, lava → 64 JE/t | 30k / 0 / 128 / – | – (lava tank) | – |
| WIND_TURBINE | wind_turbine | **3 tall**, 4–24 JE/t | 16k / 0 / 64 / – | – | – |
| PULVERIZER | pulverizer | ore → 2 dust + byproduct | 10k / 128 / 0 / 20 | in, out, 2 byproduct | `jugcraft:pulverizing` |
| ORE_WASHER | ore_washer | ore + 500 mB water → 3 washed ore | 10k / 128 / 0 / 16 | in, out (water tank) | `jugcraft:ore_washing` |
| SIEVE | sieve | gravel → flint + finds | 10k / 128 / 0 / 8 | in, out, 2 byproduct | `jugcraft:sifting` |
| SAWMILL | sawmill | log → 6 planks + sawdust | 10k / 128 / 0 / 12 | in, out, 2 byproduct | `jugcraft:sawing` |
| CAPACITOR_BANK | capacitor_bank | **2×2** battery: charges any side, outputs from its front | 4M / 4,096 / 4,096 / – | – | – |
| STEEL_TANK | steel_tank | **2×2** tank, 128 buckets, no power, no screen | none | – (fluid) | – |
| COKE_OVEN | coke_oven | **2 tall**, unpowered: coal → coke | none | in, out | `jugcraft:coking` |
| STEEL_FOUNDRY | steel_foundry | **3 tall**, unpowered: iron + coke → steel | none | 2 in, out | `jugcraft:steelmaking` (multi) |

Other blocks:

| ID | Class | What |
| --- | --- | --- |
| `machine_casing`, `arc_furnace_casing` | plain blocks | crafting part; arc furnace structure |
| `copper_cable`, `silver_cable`, `aluminum_cable` | `energy/CableBlock` | 4 px energy transmitters: 256 / 1,024 / 4,096 JE/t; a network runs at its slowest cable |
| `bronze_fluid_pipe` | `fluid/FluidPipeBlock` | 4 px fluid transmitter, 250 mB per push |
| `fluid_tank` | `fluid/FluidTankBlock(Entity)` | 16,000 mB, one fluid, comparator output |
| `electric_pump` | `fluid/ElectricPumpBlock(Entity)` | pulls from below, 100 mB/t, 8 JE/t |
| `brass_item_pipe` | `logistics/ItemPipeBlock` | 6 px item transmitter |
| `pneumatic_extractor`, `high_pressure_extractor` | `logistics/PneumaticExtractorBlock` | pull 16 items / 8 ticks (brass) or 32 / 4 (steel) from what they face |
| `item_sorter` | `logistics/ItemSorterBlock(Entity)` | 9-slot filter into the inventory it faces |
| `brass_wrench` (item) | `logistics/BrassWrenchItem` | rotate; sneak to dismantle |
| `item_crate` | `storage/CrateBlock(Entity)`, `JugcraftStorage` | 32 stacks of one item; Fabric `SingleItemStorage` |
| `speed_upgrade`, `efficiency_upgrade` (items) | `machine/MachineUpgrades` | upgrade slots of powered processors |
| `engineers_handbook` (item) | `guide/EngineersHandbookItem`, `client/HandbookScreen` | in-game guide generated by `tools/handbook.py` |

## Shared systems and how to plug in

### Energy (`energy/`)

- **Interface:** `EnergyStorage` has `insert`/`extract` in Fabric transactions, plus `getAmount` and `getCapacity`.
- **Lookup:** `EnergyStorage.SIDED` is a `BlockApiLookup<EnergyStorage, Direction>` with ID `jugcraft:energy`.
  - **To make any block take or give power, register a provider on `EnergyStorage.SIDED`.** Cables, generators and machines then connect automatically, and cables draw a connection arm to it.
- **Base implementation:** `SimpleEnergyStorage(capacity, maxInsert, maxExtract, onChange)`, with `setAmount` for tests and loading.
- **Pushing:** `EnergyNetworks.pushToNeighbors(level, pos, source, maxAmount, sides)` sends into adjacent storages and cable networks. `EnergyNetworks.move(from, to, max)` is transactional.
- **Caching:** networks are cached per level. Call `EnergyNetworks.invalidate(level)` when cable layout changes. There are at most 2,048 cables per network.
- **Tiers:** `CableBlock(properties, rate)`. A network's rate is the minimum `transferRate()` of its cables.
- **Marker:** `EnergyConnectable` marks blocks cables always connect to (cables, generators, machines). Other blocks connect through the lookup alone.
- **Rule:** use this one energy system. Don't add another power unit (CLAUDE.md: no incompatible power systems).

### Fluids (`fluid/`)

- Uses **Fabric's** `FluidStorage.SIDED` and `Storage<FluidVariant>`. Any block exposing it works with pipes, pumps and buckets.
- `FluidNetworks.pushToNeighbors(level, pos, source, maxDroplets, sides)` pushes. Networks are cached (`invalidate(level)`), with at most 1,024 pipes. The marker is `FluidConnectable`.
- **Only pumps (and generators pushing out) move fluid.** Pipes and tanks are passive.
- **Steel tank:** a `MachineKind` with a full `SingleFluidStorage` (`reservoir()`), returned by `fluidFor(side)` on every block.
- **Machine tanks:** `MachineBlockEntity` has one `tank` (mB, saved as `"water"`) exposed through `fluidFor(side)`.
  - Kinds with a tank: steam generator (water), geothermal generator (lava), ore washer (water). Capacity is `MachineKind.tankCapacity()`.
  - A water source block directly below the steam generator or ore washer is a spring: 20 mB/t, never used up.

### Items (`logistics/`)

- Uses **Fabric's** `ItemStorage.SIDED`. Machines expose their slots via `WorldlyContainer`, following the side configuration.
- `ItemNetworks.push(level, pos, side, source, maxItems, exclude)` pushes out of one side, into a pipe network or directly into the neighbor.
  - Routing sends items to matching sorters first, then round-robin to the other inventories. The pusher and `exclude` never receive.
  - Networks are cached (`invalidate(level)`), with at most 1,024 pipes. The marker is `ItemConnectable`.
- **Side configuration:** `machine/SideConfig` holds per-face `Mode` (INPUT, OUTPUT, BOTH, NONE), relative to the machine's front (`Face.of(side, facing)`), plus an `eject` flag.
  - It is packed into one synced int (`DATA_SIDES`). Menu buttons 0–5 cycle faces and 6 toggles eject (`MachineMenu.clickMenuButton` → `MachineBlockEntity.clickSideButton`).
  - When eject is on, a processor pushes all output and byproduct slots every 8 ticks (16 items).
  - `SideConfig` also holds the **redstone mode** (`Redstone`: IGNORED, HIGH, LOW; button 7), packed into bits 13–14.

### Machines (`machine/`)

- **`MachineKind`** is the single place for a machine's numbers and behavior switches:
  - `isProcessor()`, `isGenerator()`, `isMultiInput()`
  - `recipeType()`, `outputSlot()`, `byproductSlots()`, `tankCapacity()`, `usesPower()`, `upgradeSlots()`, `containerSize()`
  - `footprint()`, `powerPort()`
- **`MachineBlockEntity`** holds energy, items, progress, tank and side config. `serverTick` switches on the kind. Every processor uses `tickProcessor`: find a recipe → check the output and byproduct room (and water for the washer) → use energy → finish.
- **Multi-block machines:** `Footprint` gives the offsets (facing north). `LargeMachineBlock` has a `PART` 0..3 property. Only the master block has the block entity.
  - Breaking any part removes the whole machine. `MachineBlock.machineAt(level, pos, state)` resolves any part to its master.
  - Each block renders its own slice of one big model, which the generator cuts up.
- **Power ports:** `PowerPort(part, face)`. `MachineBlock.acceptsPower(state, side)` combines it with `usesPower()`, and both the energy lookup and the cable arms use that one check. Today only the alloy smelter has a port; the coke oven and steel foundry take no power at all.
- **Screens:** `MachineMenu` (server/common) and `client/MachineScreen` (client only). Slot positions come from `MachineMenu` constants.
- **Fuels:** `GeneratorFuels.burnTicks` and `steamBurnTicks`.
- **Upgrades:** `MachineUpgrades` holds the SPEED and EFFICIENCY items.
  - `effect(slots)` returns an `Effect` with `ticks(base)` and `use(base)`.
  - `MachineBlockEntity.upgrades()` reads a machine's cards.
- **Comparators:** `MachineBlockEntity.comparatorSignal()` returns energy for generators and batteries, and slot fullness for processors. It works from any part.

### Recipes (`machine/MachineRecipe*`, `data/jugcraft/recipe/<type>/`)

Each machine recipe is a normal Minecraft recipe file, so a data pack can add, replace or remove recipes, and `/reload` applies the change.

```json
{"type": "jugcraft:crushing", "ingredient": "#c:ores/tin", "result": {"id": "jugcraft:raw_tin", "count": 2}, "time": 160}

{"type": "jugcraft:alloying",
 "ingredients": [{"ingredient": "minecraft:copper_ingot", "count": 3}, {"ingredient": "#c:ingots/tin", "count": 1}],
 "result": {"id": "jugcraft:bronze_ingot", "count": 4}, "time": 200}

{"type": "jugcraft:pulverizing", "ingredient": "jugcraft:tin_ore", "result": {"id": "jugcraft:tin_dust", "count": 2}, "time": 200,
 "byproducts": [{"result": {"id": "jugcraft:tungsten_dust"}, "chance": 0.05, "feature": "tungsten"}]}
```

- **Single-input format** (`MachineRecipe`): `ingredient`, `result`, `time` (default 200), and optional `byproducts`. Types: crushing, arc_smelting, pressing, wire_drawing, pulverizing, ore_washing, sifting, sawing.
- **Multi-input format** (`MultiMachineRecipe`): `ingredients` is a list of `{ingredient, count}`, matched in any slot order; every other input slot must be empty. Types: alloying, circuit_assembly.
- **Byproducts** (`MachineRecipe.Byproduct`):
  - Each is rolled once per operation.
  - It is skipped if its `feature` switch is off.
  - The machine waits unless every possible byproduct fits in its byproduct slots.
- **Lookup:** use `MachineRecipes.find(serverLevel, kind, stack)` and `findMulti(...)`. Types and serializers are registered per kind in `MachineRecipeTypes`.
- **Resource condition:** `{"condition": "jugcraft:feature_enabled", "feature": "<name>"}` in `fabric:load_conditions` gates any JSON by a feature switch.
- **Recipe viewers:** no EMI/JEI/REI plugin exists yet, because no viewer build for 26.3 has been confirmed.

### Feature switches (`config/`)

- `config/jugcraft.properties` holds `<feature>.enabled`. The features are the `JugcraftConfig.FEATURES` list: 15 materials plus `machines`.
- A switch disables **acquisition only** (worldgen, recipes, byproducts). It never unregisters items or blocks, so saves survive.
- Check a switch with `JugcraftConfig.isFeatureEnabled(name)`.

### Registration (`materials/`)

- `JugcraftRegistry.item(path)` and `block(path, copyFrom)` register simple items and blocks.
- `MetalFamily.builder(name).mined().extraItem(...).build()` registers a whole metal set. `MineralFamily.register(name)` does the same for minerals.
- `JugcraftWorldgen` adds placed features to biomes. In 26.x, configured features live in `data/jugcraft/worldgen/feature/` (there is no `configured_feature` folder), with no `config` wrapper and with block states written as plain IDs.
- Initialization order is in `Jugcraft.onInitialize()`: config → materials → components → machines → fluids → logistics → conditions → worldgen → style pack.

### Looks (`tools/model_writer.py`, `tools/steampunk_*.py`)

- There are two machine styles. **Steampunk** is the default (`DEFAULT_STYLE`). **Classic** ships as the built-in pack `jugcraft:alternate_machines`.
- Steampunk models are built from Python helpers: `box`, `cyl`, `gear`, `wheel`, `dial`, `pipe`.
- **Rules:** no coplanar overlapping faces; near the middle of each side, the body comes within about 1 px of the edge, so cables meet it; glowing textures are listed in `GLOW` (and have `_on` variants).
- Every new machine needs a model in `steampunk_models.MODELS` **and** classic front textures (`<id>_front`, plus `_front_on` if lit). The checker verifies that both styles cover every block state.

## How to add things

**A new processing machine** (single input):

1. `tools/machines.py`: add the entry to `MACHINES`, `STATS`, `CRAFTING`, and a recipe list in `machine_recipes()`.
2. `generate_material_data.py`: add it to `RECIPE_TYPES`.
3. `MachineKind`: add the enum constant, and include it in `isProcessor()`, `recipeType()` and, if needed, `byproductSlots()` or `tankCapacity()`.
4. Draw the textures (`generate_textures.py` for classic, `steampunk_textures.py`) and the model (`steampunk_models.py`).
5. Run the generators and the checker.
6. Add a game test.

**A new recipe for an existing machine:** add it to the list in `tools/machines.py` (Jugcraft's own recipes), or ship a JSON file in any data pack.

**A new metal:** add it to `METALS` in `tools/materials.py` (with `gen` for worldgen) and add the matching `MetalFamily.builder(...)` in `JugcraftMaterials`. The checker compares the two.

**A block that uses power, fluid or items:** register a provider on `EnergyStorage.SIDED`, `FluidStorage.SIDED` or `ItemStorage.SIDED`. Implement the matching `*Connectable` marker if transmitters should visibly connect.

**A handbook page:** add prose to `ABOUT` in `tools/handbook.py` and list the block in a chapter in `build()`. Its numbers, crafting grid and recipes are filled in from the tables.

**A client game test** (real rendering, screenshots): extend `JugcraftClientGameTests.runTest` (Fabric client game test API). CI job `client` runs it.

**A game test:** add a public `@GameTest` method to `src/gametest/java/.../JugcraftGameTests.java`.

- Helpers:
  - `machine(kind)`: the block state to place.
  - `charge(helper, pos, side)`: fills the machine's energy.
  - `processing(helper, pos, kind, input)`: places, charges and loads a machine.
  - `chest(helper, pos)`, `extractor(helper, pos, intake)` and `count(chest, item)`.
- End with `helper.succeed()`, `helper.succeedWhen(...)` or `helper.runAtTickTime(tick, ...)`.

## Rules the checks enforce (`tools/check_mod_data.py`)

- **No free metal.** Every recipe keeps or loses metal (in nugget units). The only exceptions:
  - ore blocks ×2 (crusher, pulverizer) or ×3 (ore washer);
  - byproducts, which average at most 25% of the input's metal;
  - renewable sieve finds, which average under one nugget per operation.
- Bronze is never turned back into its ingredients.
- Every recipe has a feature-switch condition that includes its result's feature.
- Python lists must match Java: `MachineKind` numbers and recipe types, `JugcraftComponents` lists, materials, features, worldgen.
- Every ID has a model, a texture, a name, and a loot table (for blocks). Both machine styles cover every block state. Model elements stay within −16..32.

## File map

| Path | Contents |
| --- | --- |
| `src/main/java/.../Jugcraft.java` | entrypoint, init order, style pack |
| `…/config/` | `JugcraftConfig` (switches), `FeatureEnabledCondition` |
| `…/materials/` | metals, minerals, rocks, components, worldgen, registry helpers |
| `…/energy/` | JE interface, storage, cable block, networks |
| `…/fluid/` | pipe, tank, pump, fluid networks |
| `…/logistics/` | item pipe, extractor, sorter, wrench, item networks |
| `…/machine/` | machine kinds, blocks, block entity, menu, recipes, footprints, power ports, side config, arc furnace structure |
| `src/client/java/.../client/` | `JugcraftClient` (screen registration), `MachineScreen` |
| `src/gametest/java/.../test/JugcraftGameTests.java` | game tests (run by `./gradlew build`) |
| `src/gametest/java/.../test/JugcraftClientGameTests.java` | client game tests with screenshots (CI job `client`) |
| `…/guide/`, `src/client/.../HandbookScreen.java`, `tools/handbook.py` | Engineer's Handbook |
| `src/test/java/.../VanillaReferences.java` | compile-time guard that vanilla items used by recipes still exist |
| `src/main/resources/assets/jugcraft/` | generated models, blockstates, lang, textures |
| `src/main/resources/data/jugcraft/` | generated recipes (`recipe/<type>/` for machines), loot, tags, worldgen |
| `src/main/resources/resourcepacks/alternate_machines/` | classic look pack |
| `tools/materials.py`, `tools/machines.py` | **source of truth** for content and numbers |
| `tools/generate_*.py`, `tools/model_writer.py`, `tools/steampunk_*.py`, `tools/large_machines.py`, `tools/logistics_models.py` | generators |
| `tools/check_mod_data.py` | offline audit |
| `docs/TECH_TREE.md` | player-facing guide |
| `docs/features/` | feature records (required for gameplay features) |
| `docs/MACHINE_ROADMAP.md`, `docs/branches/CHEMISTRY.md` | planned, not built |

## Not built yet

- Chemistry branch: electrolysis, real refining, liquid crude oil. Blast-furnace stand-ins mark the recipes that will move there.
- Recipe viewer plugin (EMI/JEI/REI).
- A faster fluid pipe (pointless until pumps are faster).
- Any magic, farming, creature, travel or seasonal content from [CONTENT_BRANCHES.md](CONTENT_BRANCHES.md).
- Human play-testing, two-client dedicated-server tests and performance measurements (the client game tests render the game but do not play it).
- Handbook translations (English only).
