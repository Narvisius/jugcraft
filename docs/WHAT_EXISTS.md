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
| ADVANCED_SOLAR_PANEL | advanced_solar_panel | **pedestal + 3×3 layer (10 parts)**, 64 JE/t in sun (sky checked above the array) | 400k / 0 / 512 / – | – | – |
| ADVANCED_ENGINE | advanced_engine | **2×1×1** KE generator: gasoline/diesel → up to 1,024 KE/t out of the master's back | none (KE) | – (8,000 mB fuel tank) | – |
| STEAM_GENERATOR | steam_generator | coal/bitumen + water → 64 JE/t | 40k / 0 / 128 / – | fuel, water bucket, empty bucket | – |
| ALLOY_SMELTER | alloy_smelter | **3×2×6 multi-block (36 parts)**, power socket only | 10k / 128 / 0 / 20 | 2 in, out | `jugcraft:alloying` (multi) |
| METAL_PRESS | metal_press | ingot → plate | 10k / 128 / 0 / 16 | in, out | `jugcraft:pressing` |
| WIRE_DRAWER | wire_drawer | ingot → 3 wires | 10k / 128 / 0 / 12 | in, out | `jugcraft:wire_drawing` |
| CIRCUIT_ASSEMBLER | circuit_assembler | circuits | 20k / 256 / 0 / 32 | 3 in, out | `jugcraft:circuit_assembly` (multi) |
| GEOTHERMAL_GENERATOR | geothermal_generator | **2×2×2**, lava → 64 JE/t | 30k / 0 / 128 / – | – (lava tank) | – |
| WIND_TURBINE | wind_turbine | **9 tall**, 12–72 JE/t; rotor drawn by `client/WindTurbineRenderer` | 48k / 0 / 192 / – | – | – |
| PULVERIZER | pulverizer | ore → 2 dust + byproduct | 10k / 128 / 0 / 20 | in, out, 2 byproduct | `jugcraft:pulverizing` |
| ORE_WASHER | ore_washer | ore + 500 mB water → 3 washed ore | 10k / 128 / 0 / 16 | in, out (water tank) | `jugcraft:ore_washing` |
| SIEVE | sieve | gravel → flint + finds | 10k / 128 / 0 / 8 | in, out, 2 byproduct | `jugcraft:sifting` |
| SAWMILL | sawmill | log → 6 planks + sawdust | 10k / 128 / 0 / 12 | in, out, 2 byproduct | `jugcraft:sawing` |
| CAPACITOR_BANK | capacitor_bank | **2×2** battery: charges any side, outputs from its front | 4M / 4,096 / 4,096 / – | – | – |
| LITHIUM_BATTERY_BANK | lithium_battery_bank | **3×2×1** battery (electric look): charges any side, outputs from all six front faces | 32M / 16,384 / 16,384 / – | – | – |
| CRYSTAL_GROWER | crystal_grower | **1×2** (cyan look): 4 silicon + phosphate → silicon boule | 60k / 512 / 0 / 128 | 2 in, out | `jugcraft:crystal_growing` (multi) |
| LITHOGRAPHY_STATION | lithography_station | **3×2×2** fluid processor (cyan look): wafer + 2 copper wire + 100 mB acid → 4 microchips | 60k / 1,024 / 0 / 192 | 2 in, out, 4,000 mB tank | `jugcraft:lithography` |
| GAS_HOLDER | gas_holder | **3×3×3** tank, 1,024 buckets of one gas (`GasFluid` only), no power, no screen | none | – (gas) | – |
| STEEL_TANK | steel_tank | **2×2** tank, 128 buckets, no power, no screen; drops with its fluid (`MachineKind.keepsContents`) | none | – (fluid) | – |
| COKE_OVEN | coke_oven | **2×2, 2 tall + chimney block (9 parts)**, unpowered: coal → coke | none | in, out | `jugcraft:coking` |
| STEEL_FOUNDRY | steel_foundry | **2×2×5**, unpowered: iron + coke → steel | none | 2 in, out | `jugcraft:steelmaking` (multi) |
| COBBLESTONE_GENERATOR | cobblestone_generator | water + lava beside it → cobblestone, 20 ticks | 4k / 64 / 0 / 4 | 1 result slot (no inputs) | – |
| CROP_HARVESTER | crop_harvester | **1×2**: harvests and replants ripe `CropBlock`s in the 9×9 field in front (`MachineBlockEntity.tickHarvester`, `harvestTarget`) | 20k / 256 / 0 / 24 | 3 result slots | – |
| TREE_FARM | tree_farm | sapling → 6 logs, sapling back + extra | 10k / 128 / 0 / 16 | in, out, 2 byproduct | `jugcraft:tree_growing` |
| WATER_WHEEL | water_wheel | **2 tall** generator: 8–12 JE/t per flowing-water block on its right | 8k / 0 / 64 / – | – | – |
| AUTO_CRAFTER | auto_crafter | crafts the vanilla recipe in its 3×3 grid; each slot keeps one item as the pattern | 10k / 128 / 0 / 8 | 9 grid, out, 1 remainder | vanilla `crafting` |
| LARGE_STEAM_ENGINE | large_steam_engine | **2×2×2**: fuel + water → 256 KE/t out of the back of part 7 | none | fuel, water bucket, empty bucket | – |
| DEPOSIT_DRILL | deposit_drill | **3×3×2**: takes `DEPOSIT_UNITS` (1) per `DEPOSIT_TICKS` (300) from one `DepositBlock` of each kind under it or `DEPOSIT_REACH` (1) round it, `DEPOSIT_DEPTH` (3) deep; all faces output, eject on (`SideConfig.allOutputs`) | 20k / 256 / 0 / 16 | 3 result slots (no inputs) | – |
| ORE_DRILL | ore_drill | **2 tall**: mines `c:ores` in a 9×9 column below, 40 ticks each (`OreDrilling`) | 20k / 256 / 0 / 32 | 3 result slots (no inputs) | – |

Other blocks:

| ID | Class | What |
| --- | --- | --- |
| `machine_casing`, `arc_furnace_casing` | plain blocks | crafting part; arc furnace structure |
| `copper_cable`, `silver_cable`, `aluminum_cable` | `energy/CableBlock` | 6 px energy transmitters (glowing green core, emissive strips): 256 / 1,024 / 4,096 JE/t; a network runs at its slowest cable |
| `bronze_fluid_pipe` | `fluid/FluidPipeBlock` | 4 px fluid transmitter, 250 mB per push |
| `fluid_tank` | `fluid/FluidTankBlock(Entity)` | 16,000 mB, one fluid, comparator output |
| `sprinkler` | `farming/SprinklerBlock(Entity)`, `JugcraftFarming` | water tank (pipes) and fertilizer hopper; pulses random ticks on nearby crops; `wet` state shows spray |
| `cotton_crop`, `cotton_seeds`, `cotton` | `farming/CottonCropBlock`, `JugcraftFarming` | a `CropBlock` with no item of its own (the seeds place it); `CROPS` in `tools/machines.py` drives its models, loot and tags |
| `fluid_valve` | `fluid/FluidValveBlock` | a steel pipe; `carries(state)` is false while `powered`, so `FluidNetworks` stops there |
| `fluid_filter` | `fluid/FluidFilterBlock(Entity)` | a steel pipe whose touching storages only take its saved `FluidVariant`; `FluidNetworks` endpoints remember their pipe |
| `electric_pump` | `fluid/ElectricPumpBlock(Entity)` | pulls from below, 100 mB/t, 8 JE/t |
| `brass_item_pipe` | `logistics/ItemPipeBlock` | 6 px item transmitter |
| `pneumatic_extractor`, `high_pressure_extractor` | `logistics/PneumaticExtractorBlock` | pull 16 items / 8 ticks (brass) or 32 / 4 (steel) from what they face |
| `item_sorter` | `logistics/ItemSorterBlock(Entity)` | 9-slot filter into the inventory it faces |
| `conveyor_slope` | `logistics/ConveyorSlopeBlock` (`ascending`) | up slopes hand items to the block in front one higher; down slopes take them from one higher behind |
| `conveyor`, `conveyor_splitter` | `logistics/ConveyorBlock(Entity)` | KE-driven belts; 4 stacks each; `ConveyorBlockEntity.accept(stack, progress)`; insert-only `ItemStorage`; client `ConveyorRenderer` |
| `brass_wrench` (item) | `logistics/BrassWrenchItem` | rotate; sneak to dismantle |
| `mining_drill`, `chainsaw` (items) | `tools/MiningDrillItem`, `ChainsawItem` (`PoweredToolItem`) | JE in the `jugcraft:energy` component; unbreakable; extra blocks via `ServerPlayerGameMode.destroyBlock`; drill mode in `jugcraft:drill_mode` |
| `rocket_pack` (item) | `tools/RocketPackItem`, `RocketThrustPayload`; client `RocketPackClient` | equippable chest; client thrust, server pays (one per tick) |
| `*_module` (5 items) | `tools/UpgradeModuleItem`, `ToolUpgrades` | fitted at the charging station; counts in `jugcraft:overclock`/`range`/`capacity`, silk touch and fortune as vanilla enchantments |
| advancements (22) | `data/jugcraft/advancement/`, generated by `tools/advancements.py` | quest line earned by having items; `advancementTreeLoads` test |
| `charging_station` | `tools/ChargingStationBlock(Entity)`; client `ChargingStationRenderer` | 2-tall (`half`), `lit`; `EnergyStorage` on both halves; charges `Chargeable` items |
| `item_crate` | `storage/CrateBlock(Entity)`, `JugcraftStorage` | 32 stacks of one item; Fabric `SingleItemStorage` |
| `iron_shaft`, `brass_gearbox` | `kinetic/ShaftBlock(Entity)`, `GearboxBlock` | carry KE (along the axis / out of all sides); `turning` block state |
| `hand_crank`, `steam_engine` | `kinetic/HandCrankBlock(Entity)`, `SteamEngineBlock(Entity)` | KE sources: 16 KE/t cranked, 64 KE/t from fuel + water |
| `dynamo` | `kinetic/DynamoBlock(Entity)` | KE → JE at 75%; any `MachineBlockEntity` also takes KE directly (`KineticConsumer`) |
| `belt_pulley`, `belt` (item) | `kinetic/BeltPulleyBlock(Entity)`, `BeltItem` | a shaft whose BE saves `link`; `BeltPulleyBlockEntity.cannotLink`/`connect`; drawn by client `BeltRenderer` |
| `electric_motor` | `kinetic/ElectricMotorBlock(Entity)` | JE → KE at 75%, 96 KE/t out of `FACING`; 8,000 JE buffer, 256 JE/t in |
| `magnet_dynamo`, `magnet_motor` | the same `DynamoBlock`/`ElectricMotorBlock` classes and block entity types, built with `DynamoBlockEntity.MAGNET` / `ElectricMotorBlockEntity.MAGNET` `Stats` | KE → JE at 95%, 512/t, 32,000 JE buffer; JE → KE at 95%, 384 KE/t out, 1,024 JE/t in |
| `network_terminal` | `electronics/NetworkTerminalBlock`, `JugcraftElectronics` | beige retro computer; `read(level, pos)` sums the cabled network (`EnergyNetworks.view`), counting each device (multi-block machines by master) once; overlay message on use |
| `speed_upgrade`, `efficiency_upgrade` (items) | `machine/MachineUpgrades` | upgrade slots of powered processors |
| `prospector` (item) | `prospecting/ProspectorItem`, `OreSurvey`, `SurveyPayload`, `client/ProspectorScreen` | vague 3×3-chunk ore survey; server → client payload `jugcraft:ore_survey` |
| `engineers_handbook` (item) | `guide/EngineersHandbookItem`, `client/HandbookScreen` | in-game guide generated by `tools/handbook.py` |

## Shared systems and how to plug in

### Energy (`energy/`)

- **Interface:** `EnergyStorage` has `insert`/`extract` in Fabric transactions, plus `getAmount` and `getCapacity`.
- **Lookup:** `EnergyStorage.SIDED` is a `BlockApiLookup<EnergyStorage, Direction>` with ID `jugcraft:energy`.
  - **To make any block take or give power, register a provider on `EnergyStorage.SIDED`.** Cables, generators and machines then connect automatically, and cables draw a connection arm to it.
- **Base implementation:** `SimpleEnergyStorage(capacity, maxInsert, maxExtract, onChange)`, with `setAmount` for tests and loading.
- **Pushing:** `EnergyNetworks.pushToNeighbors(level, pos, source, maxAmount, sides)` sends into adjacent storages and cable networks. `EnergyNetworks.move(from, to, max)` is transactional.
- **Belts:** discovery follows a pulley's `link` to its partner and continues both ways along the partner's axis.
- **Caching:** networks are cached per level. Call `EnergyNetworks.invalidate(level)` when cable layout changes. There are at most 2,048 cables per network.
- **Tiers:** `CableBlock(properties, rate)`. A network's rate is the minimum `transferRate()` of its cables.
- **Marker:** `EnergyConnectable` marks blocks cables always connect to (cables, generators, machines). Other blocks connect through the lookup alone.
- **Rule:** use this one energy system for anything that uses or stores power (CLAUDE.md: no incompatible power systems).
  - Kinetic power (below) is a compatible layer, not a second currency: every JE machine takes KE 1:1 through `KineticConsumer`, and the dynamo bridges KE into JE. New machines should use `EnergyStorage`; they then run off shafts automatically.

### Kinetic power (`kinetic/`)

- **KE per tick**, carried by `ShaftBlock` (along its axis) and `GearboxBlock` (all sides). A source calls `KineticNetworks.push(level, pos, side, amount)`, which splits power evenly between the `KineticConsumer`s at the ends of the line.
- **Consumers:** `MachineBlockEntity` (as JE, up to `kind.maxInput`, for any powered non-generator, non-battery kind, reached through any block of a multi-block) and `DynamoBlockEntity` (KE → JE at 75%).
- **Caching:** networks are cached per (source, side) and level. Shafts, gearboxes, sources and the dynamo call `KineticNetworks.invalidate(level)` when placed, removed or their neighbors change. There are at most 256 parts per network.
- **Turning look:** `ShaftBlock.TURNING`, set by the network with client-only updates and cleared by a scheduled tick 10–20 ticks after the last push. While it is set (or a steam engine is `lit`), the block's `_active` model leaves out its rotor and client `KineticRotorRenderer` (via `KineticRotors`) draws it spinning, from `assets/jugcraft/kinetic_rotors.json` (exported by `tools/kinetic_rotors.py` from `kinetic_models.ROTORS`). `ShaftBlockEntity` holds no data; it exists only for the renderer.

### Fluids (`fluid/`)

- Uses **Fabric's** `FluidStorage.SIDED` and `Storage<FluidVariant>`. Any block exposing it works with pipes, pumps and buckets.
- `FluidNetworks.pushToNeighbors(level, pos, source, maxDroplets, sides)` pushes. Networks are cached (`invalidate(level)`), with at most 1,024 pipes. The marker is `FluidConnectable`.
- **Only pumps (and generators pushing out) move fluid.** Pipes and tanks are passive.
- **Steel tank:** a `MachineKind` with a full `SingleFluidStorage` (`reservoir()`), returned by `fluidFor(side)` on every block.
- **Machine tanks:** `MachineBlockEntity` has one `tank` (mB, saved as `"water"`) exposed through `fluidFor(side)`.
  - Kinds with a tank: steam generator (water), geothermal generator (lava), ore washer (water). Capacity is `MachineKind.tankCapacity()`.
  - A water source block directly below the steam generator or ore washer is a spring: 20 mB/t, never used up.

### Oil and fluid processing (`chemistry/`, `tools/petro.py`)

- **Petroleum fluids** (`PetroFluids`): `Entry` per fluid with source, flowing, `LiquidBlock` and bucket; `OilFluid` never makes new sources. Client: `PetroFluidsClient` registers the still/flow textures. Fluid tags `c:<fluid>`.
- **Fluid processing machines:** a `MachineKind` with a `fluidSpec()` (`FluidMachineSpec`: input and output tank capacities, item inputs and outputs). `MachineBlockEntity.tanks()` is a `FluidTanks`; `fluidFor(side)` exposes all tanks as one storage (inputs insert-only and filtered by recipes, outputs extract-only). Output tanks push out of every outer face every 4 ticks.
- **Fluid recipes** (`FluidRecipe`, `FluidRecipes`): one recipe type per machine (`MachineKind.recipeType()`), JSON keys `items`, `fluids`, `fluid_results`, `results`, `time`. Written from `tools/petro.py`; `check_mod_data.py` audits them and forbids fluid from nothing (`source` declares fluid released from items).
- **Oil reservoirs** (`OilReservoirs`): seeded per chunk, `get(level, chunk)` and `extract(level, chunk, kind, mb)`; depletion is `SavedData` (`jugcraft:oil_reservoirs`). `overrideForTest` is for game tests only.
- **Gases** (`PetroFluids.Gas`, `GasFluid`): fluids with no block or bucket (refinery gas); Fabric names them from `block.<ns>.<id>`.
- **Draw-offs:** `MachineKind.outputLayer(tank)` makes an output tank push only from the faces of one block layer (distillation tower, cracker, reformer).
- **Items** (`PetroItems`): cracking catalyst, asphalt binder.
- **Machines:** `PUMPJACK` (custom tick), `OIL_SAND_EXTRACTOR` (`jugcraft:oil_sand_extraction`), `DISTILLATION_TOWER` (`distillation`), `CATALYTIC_CRACKER` (`catalytic_cracking`), `VACUUM_DISTILLATION_UNIT` (`vacuum_distillation`), `CATALYTIC_REFORMER` (`reforming`), `CHEMICAL_MIXER` (`chemical_mixing`), `FRACKING_RIG` (custom tick; works over shale), `FLOWBACK_TREATMENT_UNIT` (`water_treatment`).
- **Industrial chemistry:** `ELECTROLYTIC_CELL` (`electrolysis`: brine → chlorine/hydrogen/lye by layer; alumina + coke → aluminum), `CHEMICAL_REACTOR` (`chemical_reaction`: sulfuric acid, alumina, fertilizer), `FUEL_CELL` (hydrogen → JE). Items `alumina`, `fertilizer`, `titanium_sponge`, `lithium_cell`, `neodymium_magnet`, and the electronics items `silicon_boule`, `silicon_wafer`, `microchip` (`chemistry/FertilizerItem`, area bone meal on crops). Fluids `brine`, `lye`, `sulfuric_acid`; gases `chlorine`, `hydrogen`. `check_mod_data` audits metal in fluid recipes.
- **Nitrogen chemistry (batch 12):** `AIR_SEPARATION_UNIT` (no recipes, like the pumpjack: `tickAirSeparation` fills nitrogen `ASU_NITROGEN_PER_TICK` into tank 0, drawn off layer 5, and oxygen `ASU_OXYGEN_PER_TICK` into tank 1, layer 0) and `SYNTHESIS_CONVERTER` (`gas_synthesis`: Haber–Bosch ammonia, Ostwald nitric acid; three input tanks, one output). Gases `nitrogen`, `oxygen`, `ammonia`; fluid `nitric_acid`. Chemical reactor `ammonium_phosphate` and lithography `microchip_nitric` recipes.
- **Rubber (batch 14):** gas `butadiene`; items `rubber`, `gasket` (`PetroItems`); `ALT_CRAFTING` in `tools/machines.py` holds second recipes (`belt_from_rubber`, `steel_fluid_pipe_from_gaskets`).
- **Boost gases (batch 13):** `MachineKind.boostGas()` / `boostPerTick()`: the steel foundry (oxygen) and crystal grower (argon) get a `TankInlet` for the gas; `tickProcessor` takes a second step each tick it can burn `boostPerTick` mB. The air separation unit's third tank is argon (`ASU_ARGON_INTERVAL`). Gas `argon`.
- **Fluid generators:** `DIESEL_GENERATOR`, `GAS_TURBINE` and `FUEL_CELL` burn fuel from input tank 0 (`MachineBlockEntity.tickFluidGenerator`); JE per mB is `FluidFuels.jePerMb(kind, fluid)` (mirrored in `tools/petro.py` `FLUID_FUELS`). The turbine's tank 1 holds lubricant, used 1 mB per `FluidFuels.LUBRICANT_TICKS`.
- **Pumps and pipes:** `ElectricPumpBlockEntity.Tier` (ELECTRIC, HEAVY); `FluidPipeBlock` takes a rate (bronze 250, steel 1,000 mB/t) and a network carries its slowest pipe's rate.

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
- **Recipe viewers:** an optional JEI plugin (`client/compat/JugcraftJeiPlugin`, JEI 31.8 compile-time API) shows one category per machine, read from `assets/jugcraft/recipe_view.json` (`tools/recipe_view.py`). Fluid machines get their own categories with fluid slots (amounts converted with `IPlatformFluidHelper.bucketVolume()`). Refinery gas has a still texture registered so viewers can draw it. EMI and REI have no plugin.

### Feature switches (`config/`)

- `config/jugcraft.properties` holds `<feature>.enabled`. The features are the `JugcraftConfig.FEATURES` list: 16 materials plus `machines` and `deposits` (surface deposit worldgen).
- A switch disables **acquisition only** (worldgen, recipes, byproducts). It never unregisters items or blocks, so saves survive.
- Check a switch with `JugcraftConfig.isFeatureEnabled(name)`.

### Registration (`materials/`)

- `JugcraftRegistry.item(path)` and `block(path, copyFrom)` register simple items and blocks.
- `MetalFamily.builder(name).mined().extraItem(...).build()` registers a whole metal set. `MineralFamily.register(name)` does the same for minerals.
- `JugcraftWorldgen` adds placed features to biomes. In 26.x, configured features live in `data/jugcraft/worldgen/feature/` (there is no `configured_feature` folder), with no `config` wrapper and with block states written as plain IDs.
- Surface deposits (`deposit/`): `JugcraftDeposits` registers the `DepositBlock`s (mirrors `tools/deposits.py`); `Deposits` keeps how much each touched deposit block has given (`SavedData`, `jugcraft_deposits.dat`) and turns an empty one to stone. `JugcraftWorldgen.addDeposit` adds their disk features to the stony hill biomes at `LOCAL_MODIFICATIONS`.
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

- Chemistry branch: PVC, bleach and enrichment (the oil line, electrochemistry, advanced materials and gas storage exist). Blast-furnace stand-ins remain as the simple routes.
- Electronics beyond processors: a monitor-bank multi-block, computers that control machines, and uses for processors in the tiers above.
- EMI and REI plugins (JEI has one).
- A faster fluid pipe (pointless until pumps are faster).
- Any magic, creature, travel or seasonal content from [CONTENT_BRANCHES.md](CONTENT_BRANCHES.md). Farming has a harvester, sprinkler and cotton; greenhouses and rubber trees are not built.
- Human play-testing, two-client dedicated-server tests and performance measurements (the client game tests render the game but do not play it).
- Handbook translations (English only).

### Stored fluid on items
- `fluid/StoredFluid` (record: `FluidVariant`, droplets) is the `jugcraft:stored_fluid` data component (`JugcraftFluids.STORED_FLUID`, persistent and synced).
- `FluidTankBlockEntity` and `MachineBlockEntity` (steel tank, gas holder reservoirs) write it in `collectImplicitComponents` and read it in `applyImplicitComponents`; loot tables copy it with `copy_components` (multi-block tanks only from part 0, and `LargeMachineBlock` breaks the master when another part goes).
- `client/JugcraftClient` adds its tooltip line through `ItemTooltipCallback`.
