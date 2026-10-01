package io.github.jimbozoomer.jugcraft.machine;

import io.github.jimbozoomer.jugcraft.chemistry.FluidMachineSpec;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

/**
 * Every Jugcraft machine and its balance numbers. Values mirror STATS in tools/machines.py.
 * Energy is in JE; rates are per tick.
 */
public enum MachineKind implements StringRepresentable {
	//                    capacity  input  output  use  slots
	COAL_GENERATOR("coal_generator", 16_000, 0, 64, 0, 1),
	BATTERY_BOX("battery_box", 400_000, 256, 256, 0, 0),
	ELECTRIC_FURNACE("electric_furnace", 10_000, 128, 0, 10, 2),
	CRUSHER("crusher", 10_000, 128, 0, 16, 2),
	ARC_FURNACE("arc_furnace_controller", 50_000, 512, 0, 64, 2),
	SOLAR_PANEL("solar_panel", 4_000, 0, 32, 0, 0),
	STEAM_GENERATOR("steam_generator", 40_000, 0, 128, 0, 3),
	ALLOY_SMELTER("alloy_smelter", 10_000, 128, 0, 20, 3),
	METAL_PRESS("metal_press", 10_000, 128, 0, 16, 2),
	WIRE_DRAWER("wire_drawer", 10_000, 128, 0, 12, 2),
	CIRCUIT_ASSEMBLER("circuit_assembler", 20_000, 256, 0, 32, 4),
	// Multi-block machines (see Footprint and LargeMachineBlock).
	GEOTHERMAL_GENERATOR("geothermal_generator", 30_000, 0, 128, 0, 0),
	WIND_TURBINE("wind_turbine", 48_000, 0, 192, 0, 0),
	// Processing depth. Pulverizer, sieve and sawmill have an input, an output and two byproduct slots.
	PULVERIZER("pulverizer", 10_000, 128, 0, 20, 4),
	ORE_WASHER("ore_washer", 10_000, 128, 0, 16, 2),
	SIEVE("sieve", 10_000, 128, 0, 8, 4),
	SAWMILL("sawmill", 10_000, 128, 0, 12, 4),
	// Steel tier: unpowered brick multi-blocks.
	COKE_OVEN("coke_oven", 0, 0, 0, 0, 2),
	STEEL_FOUNDRY("steel_foundry", 0, 0, 0, 0, 3),
	// Storage: a 2x2 capacitor bank (outputs from its front, like the battery box) and a 2x2 steel tank.
	CAPACITOR_BANK("capacitor_bank", 4_000_000, 4_096, 4_096, 0, 0),
	STEEL_TANK("steel_tank", 0, 0, 0, 0, 0),
	// Mining: a 2-tall derrick that mines the ores in a 9x9 column below it. No inputs; three result slots.
	ORE_DRILL("ore_drill", 20_000, 256, 0, 32, 3),
	// A 3x3x2 rig over a surface deposit: every 15 s, one unit from each kind of deposit under and around it.
	DEPOSIT_DRILL("deposit_drill", 20_000, 256, 0, 16, 3),
	// Renewables: a cobblestone generator (no inputs, one result slot), a tree farm (sapling in; logs out, with the
	// sapling and extras in two byproduct slots) and a 2-tall water wheel that generates from flowing water.
	COBBLESTONE_GENERATOR("cobblestone_generator", 4_000, 64, 0, 4, 1),
	TREE_FARM("tree_farm", 10_000, 128, 0, 16, 4),
	WATER_WHEEL("water_wheel", 8_000, 0, 64, 0, 0),
	// Auto-crafter: a 3x3 pattern grid (each slot keeps one item as the pattern), the result and a remainder slot.
	AUTO_CRAFTER("auto_crafter", 10_000, 128, 0, 8, 11),
	// Kinetic: a 2x2x2 steam engine turning a shaft out of its back (fuel, water bucket, empty bucket).
	LARGE_STEAM_ENGINE("large_steam_engine", 0, 0, 0, 0, 3),
	// Petrochemistry (chemistry/, docs/branches/CHEMISTRY.md): a pumpjack, 1 wide, 3 tall and 3 long, that pumps the
	// conventional oil reservoir under its chunk into its output tank.
	PUMPJACK("pumpjack", 20_000, 256, 0, 32, 0),
	// A 2x2x2 hot-water extraction plant: oil sand or bitumen and water in, crude oil and sand out.
	OIL_SAND_EXTRACTOR("oil_sand_extractor", 20_000, 256, 0, 32, 2),
	// A 2x2 column seven blocks tall: crude oil in, four fractions out, each drawn off at its own height.
	DISTILLATION_TOWER("distillation_tower", 40_000, 512, 0, 128, 0),
	// A 2x2x4 fluid catalytic cracker: heavy fuel oil, water (steam) and catalyst in; diesel, naphtha and gas out.
	CATALYTIC_CRACKER("catalytic_cracker", 40_000, 512, 0, 160, 1),
	// A 2x2x3 vacuum distillation unit: heavy fuel oil in; lubricant and asphalt binder out.
	VACUUM_DISTILLATION_UNIT("vacuum_distillation_unit", 30_000, 512, 0, 96, 1),
	// A 3x2x2 catalytic reformer: naphtha in; gasoline (base) and refinery gas (top) out.
	CATALYTIC_REFORMER("catalytic_reformer", 30_000, 512, 0, 120, 0),
	// A 2x2x2 stirred mixing vessel: water and powders in, mixtures (fracking fluid) out.
	CHEMICAL_MIXER("chemical_mixer", 20_000, 256, 0, 64, 2),
	// A 3x3x5 fracking derrick over a shale reservoir: fracking fluid down; crude oil, gas and flowback water up.
	FRACKING_RIG("fracking_rig", 80_000, 1_024, 0, 256, 0),
	// A 3x1x2 row of settling basins and a filter press: flowback water in; clean water and salt out.
	FLOWBACK_TREATMENT_UNIT("flowback_treatment_unit", 20_000, 256, 0, 48, 1),
	// A 3x2x2 six-cylinder diesel engine and generator: burns diesel or heavy fuel oil for 256 JE/t.
	DIESEL_GENERATOR("diesel_generator", 60_000, 0, 1_024, 0, 0),
	GAS_TURBINE("gas_turbine", 120_000, 0, 2_048, 0, 0),
	// A 2x2x3 jacketed reactor: refinery gas in, plastic pellets out.
	POLYMERIZATION_REACTOR("polymerization_reactor", 30_000, 512, 0, 96, 1),
	// A 2x2x3 V8 diesel engine: burns diesel or heavy fuel oil and turns a shaft out of its back.
	DIESEL_ENGINE("diesel_engine", 0, 0, 0, 0, 0),
	// A 3x3x2 electrolysis house: brine in; chlorine, hydrogen and lye out (and alumina + coke into aluminum).
	ELECTROLYTIC_CELL("electrolytic_cell", 60_000, 1_024, 0, 256, 3),
	// A 2x2x2 acid-proof reactor: sulfur + water -> sulfuric acid; later bauxite digestion and fertilizer.
	CHEMICAL_REACTOR("chemical_reactor", 30_000, 512, 0, 96, 3),
	// Nitrogen chemistry (batch 12): a 2x2x6 cold box drawing nitrogen and oxygen out of the air (no recipes), and a
	// 3x4x2 high-pressure converter making ammonia (Haber-Bosch) and nitric acid (Ostwald).
	AIR_SEPARATION_UNIT("air_separation_unit", 40_000, 512, 0, 64, 0),
	SYNTHESIS_CONVERTER("synthesis_converter", 60_000, 1_024, 0, 128, 0),
	// A one-block hydrogen fuel cell (electric look): hydrogen in, JE out.
	FUEL_CELL("fuel_cell", 40_000, 0, 512, 0, 0),
	// Storage (batch 6): a 3x2 lithium battery bank, one deep, giving power out of its front like the capacitor bank.
	LITHIUM_BATTERY_BANK("lithium_battery_bank", 32_000_000, 16_384, 16_384, 0, 0),
	// Electronics (batch 7, the cyan look): a two-block crystal grower pulling doped silicon boules.
	CRYSTAL_GROWER("crystal_grower", 60_000, 512, 0, 128, 3),
	// A 3x2x2 cleanroom with a monitor bank: wafers etched with sulfuric acid into microchips.
	LITHOGRAPHY_STATION("lithography_station", 60_000, 1_024, 0, 192, 3),
	// Fluid logistics (batch 8): a 3x3x3 Horton sphere holding 1,024 buckets of one gas. No power.
	GAS_HOLDER("gas_holder", 0, 0, 0, 0, 0),
	// Power (batch 10, the electric look): a pedestal carrying a 3x3 array of solar cells on the layer above.
	ADVANCED_SOLAR_PANEL("advanced_solar_panel", 400_000, 0, 512, 0, 0),
	// A 2x1x1 four-cylinder engine (electric look): burns gasoline or diesel and turns a shaft out of its back.
	ADVANCED_ENGINE("advanced_engine", 0, 0, 0, 0, 0),
	// Farming (batch 9): a two-block gantry that harvests and replants ripe crops in the 9x9 field in front of it.
	// No inputs; three result slots.
	CROP_HARVESTER("crop_harvester", 20_000, 256, 0, 24, 3),
	// Chemistry (batch 17, the electric look): a 3x3x2 vanadium redox flow battery. Its charge is capped by the
	// electrolyte in its tanks (FLOW_BATTERY_JE_PER_MB a millibucket); it gives power out of its front.
	FLOW_BATTERY("flow_battery", 64_000_000, 8_192, 8_192, 0, 0);

	/** JE produced per tick while the coal generator burns. */
	public static final int GENERATION_PER_TICK = 32;
	/** JE per tick from a solar panel in full sun; halved in rain. */
	public static final int SOLAR_PER_TICK = 8;
	/** JE per tick from an advanced solar panel in full sun (eight solar panels' worth); halved in rain. */
	public static final int ADVANCED_SOLAR_PER_TICK = 64;
	/** JE per tick while the steam generator boils water. */
	public static final int STEAM_PER_TICK = 64;
	/** Water (mB) the steam generator boils per tick of generation. */
	public static final int STEAM_WATER_PER_TICK = 10;
	/** Steam generator water tank (mB); one bucket is 1000 mB. */
	public static final int STEAM_TANK = 8_000;
	/** mB per tick drawn from a water source block directly beneath the steam generator. */
	public static final int STEAM_SOURCE_REFILL = 20;
	/** JE per tick while the geothermal generator has lava. */
	public static final int GEOTHERMAL_PER_TICK = 64;
	/** Lava (mB) the geothermal generator uses per tick: one bucket lasts 1,000 ticks (64,000 JE). */
	public static final int GEOTHERMAL_LAVA_PER_TICK = 1;
	/** Geothermal generator lava tank (mB). */
	public static final int GEOTHERMAL_TANK = 4_000;
	/** Wind turbine JE per tick with its rotor at sea level; one more per 2 blocks higher. */
	public static final int WIND_BASE_PER_TICK = 12;
	/** Wind turbine output cap before weather. */
	public static final int WIND_MAX_PER_TICK = 72;
	/** The rotor's reach in blocks from the hub: the square it sweeps in front of the top block must be clear. */
	public static final int WIND_ROTOR_REACH = 3;
	/** Ticks between checks that the wind turbine's rotor has room to turn. */
	public static final int WIND_CHECK_INTERVAL = 100;
	/** Steel tank capacity (mB): 128 buckets. */
	public static final int STEEL_TANK_CAPACITY = 128_000;
	/** Gas holder: 1,024 buckets (mB) of one gas, and only gases. */
	public static final int GAS_HOLDER_CAPACITY = 1_024_000;
	/** Flow battery electrolyte tanks (mB): 64 buckets, filling them gives the full 64,000,000 JE. */
	public static final int FLOW_BATTERY_TANK = 64_000;
	/** JE the flow battery can hold for each millibucket of vanadium electrolyte in it. */
	public static final long FLOW_BATTERY_JE_PER_MB = 1_000;
	/** Ore washer water tank (mB). */
	public static final int WASHER_TANK = 8_000;
	/** Water (mB) the ore washer uses per operation, taken when the operation finishes. */
	public static final int WASHER_WATER_PER_OPERATION = 500;
	/** mB per tick drawn from a water source block directly beneath the ore washer. */
	public static final int WASHER_SOURCE_REFILL = 20;
	/** Ore drill: blocks mined in each direction from the drill's column, so 4 means a 9x9 area. */
	public static final int DRILL_RADIUS = 4;
	/** Ore drill: ticks to mine one ore block (before speed upgrades). */
	public static final int DRILL_TICKS = 40;
	/** Crop harvester: powered ticks per crop harvested. */
	public static final int HARVEST_TICKS = 20;
	/** Crop harvester: blocks either side of the middle of its field; the field is 9x9, starting the block in front. */
	public static final int HARVEST_RADIUS = 4;
	/** Crop harvester: field blocks checked per tick while looking for a ripe crop. */
	public static final int HARVEST_SCAN_PER_TICK = 9;
	/** Ore drill: blocks the drill head checks per tick while looking for the next ore (one layer). */
	public static final int DRILL_SCAN_PER_TICK = (2 * DRILL_RADIUS + 1) * (2 * DRILL_RADIUS + 1);
	/**
	 * Deposit drill: ticks per cycle (15 seconds, before speed upgrades), and the units (items) each cycle takes from
	 * each kind of deposit in reach.
	 */
	public static final int DEPOSIT_TICKS = 300;
	public static final int DEPOSIT_UNITS = 1;
	/** Deposit drill: how far past its own 3x3 it reaches on each side, and how many layers down. */
	public static final int DEPOSIT_REACH = 1;
	public static final int DEPOSIT_DEPTH = 3;
	/** Cobblestone generator: ticks per cobblestone (before speed upgrades), with water and lava beside it. */
	public static final int COBBLE_TICKS = 20;
	/** Water wheel: JE per tick for each block of flowing water at the wheel; falling water gives more. */
	public static final int WATER_WHEEL_FLOWING = 8;
	public static final int WATER_WHEEL_FALLING = 12;
	/** Ticks between checks of the water at the wheel (and of the cobblestone generator's water and lava). */
	public static final int SOURCE_CHECK_INTERVAL = 20;
	/** Large steam engine: KE per tick out of its back, water per tick, tank, and burn ticks used per tick. */
	public static final int LARGE_ENGINE_OUTPUT = 256;
	public static final int LARGE_ENGINE_WATER_PER_TICK = 40;
	public static final int LARGE_ENGINE_TANK = 16_000;
	public static final int LARGE_ENGINE_BURN_PER_TICK = 4;
	/** The large steam engine's output: the upper right back block (part 7), through its back face. */
	public static final int LARGE_ENGINE_OUTPUT_PART = 7;
	/** Auto-crafter: ticks per craft (before speed upgrades). */
	public static final int CRAFT_TICKS = 40;
	/** Pumpjack: mB of crude oil pumped per powered tick (a bucket every 25 seconds), and its tank. */
	public static final int PUMPJACK_RATE = 2;
	public static final int PUMPJACK_TANK = 16_000;
	/** Oil sand extractor: its water tank and its crude oil tank. */
	public static final int EXTRACTOR_TANK = 8_000;
	/** Distillation tower: its crude oil tank and each fraction's tank. */
	public static final int TOWER_INPUT_TANK = 16_000;
	public static final int TOWER_OUTPUT_TANK = 8_000;
	/** Distillation tower: the height (block layer) each fraction is drawn off at: gas at the top, heavy oil at the base. */
	private static final int[] TOWER_DRAW_OFFS = {6, 4, 2, 0};
	/** Catalytic cracker: each tank's capacity, and the layer each product is drawn off at (diesel, naphtha, gas). */
	public static final int CRACKER_TANK = 8_000;
	private static final int[] CRACKER_DRAW_OFFS = {0, 2, 3};
	/** Vacuum distillation unit: its heavy fuel oil tank and its lubricant tank. */
	public static final int VACUUM_TANK = 8_000;
	/** Catalytic reformer: each tank, and the layer each product is drawn off at (gasoline, refinery gas). */
	public static final int REFORMER_TANK = 8_000;
	private static final int[] REFORMER_DRAW_OFFS = {0, 1};
	/** Chemical mixer: its water tank and its product tank. */
	public static final int MIXER_TANK = 8_000;
	/**
	 * Fracking rig, per powered tick over shale: fracking fluid pumped down, oil freed from the reservoir (three
	 * quarters crude oil, a quarter refinery gas) and flowback water returned. A quarter of the fluid stays in the rock.
	 */
	public static final int FRACK_FLUID_PER_TICK = 4;
	public static final int FRACK_OIL_PER_TICK = 8;
	public static final int FRACK_FLOWBACK_PER_TICK = 3;
	public static final int FRACK_INPUT_TANK = 16_000;
	public static final int FRACK_OIL_TANK = 16_000;
	public static final int FRACK_GAS_TANK = 8_000;
	public static final int FRACK_FLOWBACK_TANK = 16_000;
	/** Fracking rig draw-offs: crude oil at the base, flowback water one block up, gas at the top. */
	private static final int[] FRACK_DRAW_OFFS = {0, 4, 1};
	/** Flowback treatment unit: its flowback tank and its clean water tank. */
	public static final int TREATMENT_TANK = 8_000;
	/** Diesel generator: JE per tick while running, and its fuel tank. Fuel values: chemistry/FluidFuels. */
	public static final int DIESEL_OUTPUT = 256;
	public static final int DIESEL_TANK = 8_000;
	/** Gas turbine: JE per tick while running, its fuel tank and its lubricant tank (FluidFuels.LUBRICANT_TICKS). */
	public static final int TURBINE_OUTPUT = 512;
	public static final int TURBINE_TANK = 16_000;
	public static final int TURBINE_LUBRICANT_TANK = 4_000;
	/** Diesel engine: KE per tick out of its back, its fuel tank, and the part the shaft leaves from (upper right back). */
	public static final int DIESEL_ENGINE_OUTPUT = 512;
	public static final int DIESEL_ENGINE_TANK = 8_000;
	public static final int DIESEL_ENGINE_OUTPUT_PART = 11;
	/** Advanced combustion engine: KE per tick at most, out of the back of its master block. */
	public static final int ADVANCED_ENGINE_OUTPUT = 1_024;
	public static final int ADVANCED_ENGINE_TANK = 8_000;
	/** Electrolytic cell: each tank, and the layers its outputs leave from (chlorine top, hydrogen middle, lye base). */
	public static final int CELL_TANK = 8_000;
	private static final int[] CELL_DRAW_OFFS = {2, 1, 0};
	/** Air separation unit: nitrogen and oxygen per powered tick, its tanks, and where each is drawn off (nitrogen
	 * boils off the top of the column, liquid oxygen collects at the base). */
	public static final int ASU_NITROGEN_PER_TICK = 8;
	public static final int ASU_OXYGEN_PER_TICK = 2;
	/** Argon is scarce: one mB every this many ticks, drawn off the middle of the column (batch 13). */
	public static final int ASU_ARGON_INTERVAL = 2;
	public static final int ASU_TANK = 16_000;
	private static final int[] ASU_DRAW_OFFS = {5, 0, 2};
	/** Boost gases (batch 13): oxygen blown into the steel foundry, argon around the crystal grower's melt. */
	public static final int BOOST_TANK = 8_000;
	public static final int FOUNDRY_OXYGEN_PER_TICK = 2;
	public static final int GROWER_ARGON_PER_TICK = 1;
	/** Synthesis converter: each input tank and the output tank. */
	public static final int CONVERTER_TANK = 8_000;
	/** Fuel cell: JE per tick while running, and its hydrogen tank. Fuel value: chemistry/FluidFuels. */
	public static final int FUEL_CELL_OUTPUT = 128;
	public static final int FUEL_CELL_TANK = 8_000;
	/** Chemical reactor: its input and output tanks. */
	public static final int CHEM_REACTOR_TANK = 8_000;
	/** Polymerization reactor: its refinery gas tank. */
	public static final int REACTOR_TANK = 8_000;
	/** Lithography station: its sulfuric acid (etchant) tank. */
	public static final int LITHOGRAPHY_TANK = 4_000;
	/** Ticks the electric furnace needs per item (the vanilla furnace needs 200). */
	public static final int ELECTRIC_FURNACE_TICKS = 100;

	public final String id;
	public final long capacity;
	public final long maxInput;
	public final long maxOutput;
	public final int usePerTick;
	public final int slots;

	MachineKind(String id, long capacity, long maxInput, long maxOutput, int usePerTick, int slots) {
		this.id = id;
		this.capacity = capacity;
		this.maxInput = maxInput;
		this.maxOutput = maxOutput;
		this.usePerTick = usePerTick;
		this.slots = slots;
	}

	/** Processing machines have input slots first and one output slot last. */
	public boolean isProcessor() {
		return this == ELECTRIC_FURNACE || this == CRUSHER || this == ARC_FURNACE || this == ALLOY_SMELTER
				|| this == METAL_PRESS || this == WIRE_DRAWER || this == CIRCUIT_ASSEMBLER
				|| this == PULVERIZER || this == ORE_WASHER || this == SIEVE || this == SAWMILL
				|| this == COKE_OVEN || this == STEEL_FOUNDRY || this == ORE_DRILL || this == DEPOSIT_DRILL
				|| this == COBBLESTONE_GENERATOR || this == TREE_FARM || this == AUTO_CRAFTER || this == CRYSTAL_GROWER
				|| this == CROP_HARVESTER;
	}

	/** Stores energy and gives it out of its front face only. */
	public boolean isBattery() {
		return this == BATTERY_BOX || this == CAPACITOR_BANK || this == LITHIUM_BATTERY_BANK || this == FLOW_BATTERY;
	}

	/** Whether the machine runs on JE at all. Unpowered machines have no battery and cables never connect to them. */
	public boolean usesPower() {
		return capacity > 0;
	}

	/** Processors whose recipes combine several ingredient stacks placed in any input slots. */
	public boolean isMultiInput() {
		return this == ALLOY_SMELTER || this == CIRCUIT_ASSEMBLER || this == STEEL_FOUNDRY || this == CRYSTAL_GROWER;
	}

	/**
	 * This machine's recipe type (jugcraft:&lt;name&gt;; see {@link MachineRecipeTypes}), or null for machines
	 * without their own recipes (the electric furnace uses vanilla smelting).
	 */
	public String recipeType() {
		return switch (this) {
			case CRUSHER -> "crushing";
			case ARC_FURNACE -> "arc_smelting";
			case ALLOY_SMELTER -> "alloying";
			case METAL_PRESS -> "pressing";
			case WIRE_DRAWER -> "wire_drawing";
			case CIRCUIT_ASSEMBLER -> "circuit_assembly";
			case PULVERIZER -> "pulverizing";
			case ORE_WASHER -> "ore_washing";
			case SIEVE -> "sifting";
			case SAWMILL -> "sawing";
			case COKE_OVEN -> "coking";
			case STEEL_FOUNDRY -> "steelmaking";
			case TREE_FARM -> "tree_growing";
			case OIL_SAND_EXTRACTOR -> "oil_sand_extraction";
			case DISTILLATION_TOWER -> "distillation";
			case CATALYTIC_CRACKER -> "catalytic_cracking";
			case VACUUM_DISTILLATION_UNIT -> "vacuum_distillation";
			case CATALYTIC_REFORMER -> "reforming";
			case CHEMICAL_MIXER -> "chemical_mixing";
			case FLOWBACK_TREATMENT_UNIT -> "water_treatment";
			case POLYMERIZATION_REACTOR -> "polymerization";
			case ELECTROLYTIC_CELL -> "electrolysis";
			case SYNTHESIS_CONVERTER -> "gas_synthesis";
			case CHEMICAL_REACTOR -> "chemical_reaction";
			case CRYSTAL_GROWER -> "crystal_growing";
			case LITHOGRAPHY_STATION -> "lithography";
			default -> null;
		};
	}

	/** Upgrade slots, after all other slots: powered processors have two (see {@link MachineUpgrades}). */
	public int upgradeSlots() {
		return isProcessor() && usesPower() ? 2 : 0;
	}

	/** Size of the machine's inventory: {@link #slots} (inputs, output, byproducts) plus upgrade slots. */
	public int containerSize() {
		return slots + upgradeSlots();
	}

	/**
	 * The tanks and item slots of a fluid processing machine (the Chemistry branch's oil line: docs/branches/CHEMISTRY.md),
	 * or null for every other machine. Such machines run {@link io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe}s.
	 */
	public @Nullable FluidMachineSpec fluidSpec() {
		return switch (this) {
			case PUMPJACK -> new FluidMachineSpec(List.of(), List.of(PUMPJACK_TANK), 0, 0);
			case OIL_SAND_EXTRACTOR -> new FluidMachineSpec(List.of(EXTRACTOR_TANK), List.of(EXTRACTOR_TANK), 1, 1);
			case DISTILLATION_TOWER -> new FluidMachineSpec(List.of(TOWER_INPUT_TANK),
					List.of(TOWER_OUTPUT_TANK, TOWER_OUTPUT_TANK, TOWER_OUTPUT_TANK, TOWER_OUTPUT_TANK), 0, 0);
			case CATALYTIC_CRACKER -> new FluidMachineSpec(List.of(CRACKER_TANK, CRACKER_TANK),
					List.of(CRACKER_TANK, CRACKER_TANK, CRACKER_TANK), 1, 0);
			case VACUUM_DISTILLATION_UNIT -> new FluidMachineSpec(List.of(VACUUM_TANK), List.of(VACUUM_TANK), 0, 1);
			case CATALYTIC_REFORMER -> new FluidMachineSpec(List.of(REFORMER_TANK), List.of(REFORMER_TANK, REFORMER_TANK), 0, 0);
			case CHEMICAL_MIXER -> new FluidMachineSpec(List.of(MIXER_TANK), List.of(MIXER_TANK), 2, 0);
			case FRACKING_RIG -> new FluidMachineSpec(List.of(FRACK_INPUT_TANK),
					List.of(FRACK_OIL_TANK, FRACK_GAS_TANK, FRACK_FLOWBACK_TANK), 0, 0);
			case FLOWBACK_TREATMENT_UNIT -> new FluidMachineSpec(List.of(TREATMENT_TANK), List.of(TREATMENT_TANK), 0, 1);
			case DIESEL_GENERATOR -> new FluidMachineSpec(List.of(DIESEL_TANK), List.of(), 0, 0);
			case GAS_TURBINE -> new FluidMachineSpec(List.of(TURBINE_TANK, TURBINE_LUBRICANT_TANK), List.of(), 0, 0);
			case POLYMERIZATION_REACTOR -> new FluidMachineSpec(List.of(REACTOR_TANK), List.of(), 0, 1);
			case DIESEL_ENGINE -> new FluidMachineSpec(List.of(DIESEL_ENGINE_TANK), List.of(), 0, 0);
			case ADVANCED_ENGINE -> new FluidMachineSpec(List.of(ADVANCED_ENGINE_TANK), List.of(), 0, 0);
			case ELECTROLYTIC_CELL -> new FluidMachineSpec(List.of(CELL_TANK), List.of(CELL_TANK, CELL_TANK, CELL_TANK), 2, 1);
			case AIR_SEPARATION_UNIT -> new FluidMachineSpec(List.of(), List.of(ASU_TANK, ASU_TANK, ASU_TANK), 0, 0);
			case SYNTHESIS_CONVERTER -> new FluidMachineSpec(List.of(CONVERTER_TANK, CONVERTER_TANK, CONVERTER_TANK),
					List.of(CONVERTER_TANK), 0, 0);
			case CHEMICAL_REACTOR -> new FluidMachineSpec(List.of(CHEM_REACTOR_TANK), List.of(CHEM_REACTOR_TANK), 2, 1);
			case LITHOGRAPHY_STATION -> new FluidMachineSpec(List.of(LITHOGRAPHY_TANK), List.of(), 2, 1);
			case FUEL_CELL -> new FluidMachineSpec(List.of(FUEL_CELL_TANK), List.of(), 0, 0);
			default -> null;
		};
	}

	/**
	 * The block layer (height above the master) that output tank {@code tank} is pushed from, or -1 for every outer
	 * face of the machine.
	 */
	public int outputLayer(int tank) {
		return switch (this) {
			case DISTILLATION_TOWER -> TOWER_DRAW_OFFS[tank];
			case CATALYTIC_CRACKER -> CRACKER_DRAW_OFFS[tank];
			case CATALYTIC_REFORMER -> REFORMER_DRAW_OFFS[tank];
			case FRACKING_RIG -> FRACK_DRAW_OFFS[tank];
			case ELECTROLYTIC_CELL -> CELL_DRAW_OFFS[tank];
			case AIR_SEPARATION_UNIT -> ASU_DRAW_OFFS[tank];
			default -> -1;
		};
	}

	public boolean isFluidProcessor() {
		return fluidSpec() != null;
	}

	/** The output slot of a processor: after the inputs, before any byproduct slots. Fluid processors: the first output slot. */
	public int outputSlot() {
		FluidMachineSpec spec = fluidSpec();
		if (spec != null) {
			return spec.itemInputs();
		}
		return slots - 1 - byproductSlots();
	}

	/** Slots after the output that collect recipe byproducts (see {@link MachineRecipe#byproducts()}). */
	public int byproductSlots() {
		// The ore drill has no inputs; its "byproduct" slots are just two more result slots.
		if (this == AUTO_CRAFTER) {
			return 1; // Container remainders, such as the empty bucket from a cake.
		}
		return this == PULVERIZER || this == SIEVE || this == SAWMILL || this == ORE_DRILL || this == DEPOSIT_DRILL
				|| this == TREE_FARM || this == CROP_HARVESTER ? 2 : 0;
	}

	/**
	 * The gas this item machine can be boosted with (batch 13), as a {@code jugcraft} fluid id, or null: each tick it
	 * holds {@link #boostPerTick()} mB of it, it burns that much and works twice as fast.
	 */
	public @Nullable String boostGas() {
		return switch (this) {
			case STEEL_FOUNDRY -> "oxygen";
			case CRYSTAL_GROWER -> "argon";
			default -> null;
		};
	}

	/** mB of boost gas a boosted tick uses (0 for machines without a boost). */
	public int boostPerTick() {
		return switch (this) {
			case STEEL_FOUNDRY -> FOUNDRY_OXYGEN_PER_TICK;
			case CRYSTAL_GROWER -> GROWER_ARGON_PER_TICK;
			default -> 0;
		};
	}

	/** mB the machine's fluid tank holds, or 0 without one. */
	public int tankCapacity() {
		return switch (this) {
			case STEAM_GENERATOR -> STEAM_TANK;
			case LARGE_STEAM_ENGINE -> LARGE_ENGINE_TANK;
			case GEOTHERMAL_GENERATOR -> GEOTHERMAL_TANK;
			case ORE_WASHER -> WASHER_TANK;
			default -> 0;
		};
	}

	/** Generators only produce energy; they never accept it. */
	public boolean isGenerator() {
		return this == COAL_GENERATOR || this == SOLAR_PANEL || this == STEAM_GENERATOR
				|| this == GEOTHERMAL_GENERATOR || this == WIND_TURBINE || this == WATER_WHEEL || this == DIESEL_GENERATOR
				|| this == GAS_TURBINE || this == FUEL_CELL || this == ADVANCED_SOLAR_PANEL;
	}

	/**
	 * The blocks this machine fills, facing north. The geothermal generator's lava tank sits to
	 * the right of its body (seen from the front); the wind turbine is three blocks tall.
	 */
	public Footprint footprint() {
		return switch (this) {
			case GEOTHERMAL_GENERATOR -> Footprint.cuboid(2, 2, 2);
			case WIND_TURBINE -> Footprint.tall(9);
			// A 2x2 beehive two blocks high, with its chimney in one block on top (part 8).
			case COKE_OVEN -> Footprint.of(Vec3i.ZERO, new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 1),
					new Vec3i(0, 1, 0), new Vec3i(-1, 1, 0), new Vec3i(0, 1, 1), new Vec3i(-1, 1, 1), new Vec3i(0, 2, 0));
			case ORE_DRILL -> Footprint.tall(2);
			// Three wide, two tall, three deep, standing on the deposit.
			case DEPOSIT_DRILL -> Footprint.cuboid(3, 2, 3);
			case CROP_HARVESTER -> Footprint.tall(2);
			case LARGE_STEAM_ENGINE -> Footprint.cuboid(2, 2, 2);
			case WATER_WHEEL -> Footprint.tall(2);
			case STEEL_FOUNDRY -> Footprint.cuboid(2, 5, 2);
			// Two wide, two tall.
			case CAPACITOR_BANK -> Footprint.of(Vec3i.ZERO, new Vec3i(-1, 0, 0), new Vec3i(0, 1, 0), new Vec3i(-1, 1, 0));
			// Two wide, two deep, one tall (plus its dome).
			case STEEL_TANK -> Footprint.of(Vec3i.ZERO, new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 1));
			// Three wide, six tall, two deep: the furnace column (left) and a 2x2 crucible tank tower (right).
			case ALLOY_SMELTER -> Footprint.cuboid(3, 6, 2);
			// One wide, three tall, three long: wellhead at the front (the master), samson post, then crank and motor.
			case PUMPJACK -> Footprint.cuboid(1, 3, 3);
			case OIL_SAND_EXTRACTOR -> Footprint.cuboid(2, 2, 2);
			case DISTILLATION_TOWER -> Footprint.cuboid(2, 7, 2);
			case CATALYTIC_CRACKER -> Footprint.cuboid(2, 4, 2);
			case VACUUM_DISTILLATION_UNIT -> Footprint.cuboid(2, 3, 2);
			case CATALYTIC_REFORMER -> Footprint.cuboid(3, 2, 2);
			case CHEMICAL_MIXER -> Footprint.cuboid(2, 2, 2);
			case FRACKING_RIG -> Footprint.cuboid(3, 5, 3);
			case FLOWBACK_TREATMENT_UNIT -> Footprint.cuboid(3, 1, 2);
			case DIESEL_GENERATOR -> Footprint.cuboid(3, 2, 2);
			case GAS_TURBINE -> Footprint.cuboid(4, 2, 2);
			case POLYMERIZATION_REACTOR -> Footprint.cuboid(2, 3, 2);
			case DIESEL_ENGINE -> Footprint.cuboid(2, 2, 3);
			case ADVANCED_ENGINE -> Footprint.cuboid(2, 1, 1);
			case ELECTROLYTIC_CELL -> Footprint.cuboid(3, 3, 2);
			// A two by two cold box six blocks tall, and a three-wide, four-tall converter train.
			case AIR_SEPARATION_UNIT -> Footprint.cuboid(2, 6, 2);
			case SYNTHESIS_CONVERTER -> Footprint.cuboid(3, 4, 2);
			case CHEMICAL_REACTOR -> Footprint.cuboid(2, 2, 2);
			// Three wide, two tall, one deep, so every block's front is a power socket.
			case LITHIUM_BATTERY_BANK -> Footprint.cuboid(3, 2, 1);
			// A control cabinet with the growth chamber and pull head above it.
			case CRYSTAL_GROWER -> Footprint.tall(2);
			// The cleanroom (left) and the operator's desk with its monitor bank (right), two deep.
			case LITHOGRAPHY_STATION -> Footprint.cuboid(3, 2, 2);
			// A sphere on legs, three blocks every way.
			case GAS_HOLDER -> Footprint.cuboid(3, 3, 3);
			// Two electrolyte tanks either side of the cell stack, three wide, three tall and two deep.
			case FLOW_BATTERY -> Footprint.cuboid(3, 3, 2);
			// The pedestal (the master) and the 3x3 array of cells on the layer above it, centred over it.
			case ADVANCED_SOLAR_PANEL -> Footprint.of(Vec3i.ZERO, new Vec3i(0, 1, 0), new Vec3i(-1, 1, 0), new Vec3i(1, 1, 0),
					new Vec3i(0, 1, -1), new Vec3i(0, 1, 1), new Vec3i(-1, 1, -1), new Vec3i(1, 1, -1), new Vec3i(-1, 1, 1),
					new Vec3i(1, 1, 1));
			default -> Footprint.SINGLE;
		};
	}

	/**
	 * Where cables connect, or null when every face of every block takes power. The alloy
	 * smelter's copper power socket is on the outer side of its lower right block.
	 */
	public @Nullable PowerPort powerPort() {
		return this == ALLOY_SMELTER ? new PowerPort(2, Direction.WEST) : null;
	}

	/**
	 * Tanks that keep their fluid when broken. Only their master block has the block entity, so only it drops the item
	 * (with the fluid on it): breaking any other block of one breaks the master too (see LargeMachineBlock).
	 */
	public boolean keepsContents() {
		return this == STEEL_TANK || this == GAS_HOLDER || this == FLOW_BATTERY;
	}

	/** Boilers: a fuel slot, a water-bucket slot and an empty-bucket slot, and a water tank. */
	public boolean isBoiler() {
		return this == STEAM_GENERATOR || this == LARGE_STEAM_ENGINE;
	}

	/** Machines with a real fire: they smoke and crackle while running (client-side effects only). */
	public boolean burnsFuel() {
		return this == COAL_GENERATOR || this == STEAM_GENERATOR || this == GEOTHERMAL_GENERATOR
				|| this == LARGE_STEAM_ENGINE || this == COKE_OVEN || this == STEEL_FOUNDRY || this == ARC_FURNACE
				|| this == DIESEL_GENERATOR || this == GAS_TURBINE || this == DIESEL_ENGINE || this == ADVANCED_ENGINE;
	}

	/** Height of the machine in blocks (the tallest part plus one). */
	public int height() {
		int top = 0;
		for (Vec3i offset : footprint().offsets()) {
			top = Math.max(top, offset.getY());
		}
		return top + 1;
	}

	public boolean isLarge() {
		return footprint().size() > 1;
	}

	@Override
	public String getSerializedName() {
		return id;
	}
}
