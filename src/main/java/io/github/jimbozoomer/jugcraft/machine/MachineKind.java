package io.github.jimbozoomer.jugcraft.machine;

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
	// Renewables: a cobblestone generator (no inputs, one result slot), a tree farm (sapling in; logs out, with the
	// sapling and extras in two byproduct slots) and a 2-tall water wheel that generates from flowing water.
	COBBLESTONE_GENERATOR("cobblestone_generator", 4_000, 64, 0, 4, 1),
	TREE_FARM("tree_farm", 10_000, 128, 0, 16, 4),
	WATER_WHEEL("water_wheel", 8_000, 0, 64, 0, 0),
	// Auto-crafter: a 3x3 pattern grid (each slot keeps one item as the pattern), the result and a remainder slot.
	AUTO_CRAFTER("auto_crafter", 10_000, 128, 0, 8, 11),
	// Kinetic: a 2x2x2 steam engine turning a shaft out of its back (fuel, water bucket, empty bucket).
	LARGE_STEAM_ENGINE("large_steam_engine", 0, 0, 0, 0, 3);

	/** JE produced per tick while the coal generator burns. */
	public static final int GENERATION_PER_TICK = 32;
	/** JE per tick from a solar panel in full sun; halved in rain. */
	public static final int SOLAR_PER_TICK = 8;
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
	/** Ore drill: blocks the drill head checks per tick while looking for the next ore (one layer). */
	public static final int DRILL_SCAN_PER_TICK = (2 * DRILL_RADIUS + 1) * (2 * DRILL_RADIUS + 1);
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
				|| this == COKE_OVEN || this == STEEL_FOUNDRY || this == ORE_DRILL
				|| this == COBBLESTONE_GENERATOR || this == TREE_FARM || this == AUTO_CRAFTER;
	}

	/** Stores energy and gives it out of its front face only. */
	public boolean isBattery() {
		return this == BATTERY_BOX || this == CAPACITOR_BANK;
	}

	/** Whether the machine runs on JE at all. Unpowered machines have no battery and cables never connect to them. */
	public boolean usesPower() {
		return capacity > 0;
	}

	/** Processors whose recipes combine several ingredient stacks placed in any input slots. */
	public boolean isMultiInput() {
		return this == ALLOY_SMELTER || this == CIRCUIT_ASSEMBLER || this == STEEL_FOUNDRY;
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

	/** The output slot of a processor: after the inputs, before any byproduct slots. */
	public int outputSlot() {
		return slots - 1 - byproductSlots();
	}

	/** Slots after the output that collect recipe byproducts (see {@link MachineRecipe#byproducts()}). */
	public int byproductSlots() {
		// The ore drill has no inputs; its "byproduct" slots are just two more result slots.
		if (this == AUTO_CRAFTER) {
			return 1; // Container remainders, such as the empty bucket from a cake.
		}
		return this == PULVERIZER || this == SIEVE || this == SAWMILL || this == ORE_DRILL || this == TREE_FARM ? 2 : 0;
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
				|| this == GEOTHERMAL_GENERATOR || this == WIND_TURBINE || this == WATER_WHEEL;
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
			case LARGE_STEAM_ENGINE -> Footprint.cuboid(2, 2, 2);
			case WATER_WHEEL -> Footprint.tall(2);
			case STEEL_FOUNDRY -> Footprint.cuboid(2, 5, 2);
			// Two wide, two tall.
			case CAPACITOR_BANK -> Footprint.of(Vec3i.ZERO, new Vec3i(-1, 0, 0), new Vec3i(0, 1, 0), new Vec3i(-1, 1, 0));
			// Two wide, two deep, one tall (plus its dome).
			case STEEL_TANK -> Footprint.of(Vec3i.ZERO, new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 1));
			// Three wide, six tall, two deep: the furnace column (left) and a 2x2 crucible tank tower (right).
			case ALLOY_SMELTER -> Footprint.cuboid(3, 6, 2);
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

	/** Boilers: a fuel slot, a water-bucket slot and an empty-bucket slot, and a water tank. */
	public boolean isBoiler() {
		return this == STEAM_GENERATOR || this == LARGE_STEAM_ENGINE;
	}

	/** Machines with a real fire: they smoke and crackle while running (client-side effects only). */
	public boolean burnsFuel() {
		return this == COAL_GENERATOR || this == STEAM_GENERATOR || this == GEOTHERMAL_GENERATOR
				|| this == LARGE_STEAM_ENGINE || this == COKE_OVEN || this == STEEL_FOUNDRY || this == ARC_FURNACE;
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
