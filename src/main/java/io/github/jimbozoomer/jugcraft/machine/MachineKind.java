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
	WIND_TURBINE("wind_turbine", 16_000, 0, 64, 0, 0);

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
	/** Wind turbine JE per tick at or below sea level; one more per 4 blocks higher. */
	public static final int WIND_BASE_PER_TICK = 4;
	/** Wind turbine output cap before weather. */
	public static final int WIND_MAX_PER_TICK = 24;
	/** Ticks between checks that the wind turbine's rotor has room to turn. */
	public static final int WIND_CHECK_INTERVAL = 100;
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
				|| this == METAL_PRESS || this == WIRE_DRAWER || this == CIRCUIT_ASSEMBLER;
	}

	/** Processors whose recipes combine several ingredient stacks placed in any input slots. */
	public boolean isMultiInput() {
		return this == ALLOY_SMELTER || this == CIRCUIT_ASSEMBLER;
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
			default -> null;
		};
	}

	/** The output slot of a processor (its last slot). */
	public int outputSlot() {
		return slots - 1;
	}

	/** Generators only produce energy; they never accept it. */
	public boolean isGenerator() {
		return this == COAL_GENERATOR || this == SOLAR_PANEL || this == STEAM_GENERATOR
				|| this == GEOTHERMAL_GENERATOR || this == WIND_TURBINE;
	}

	/**
	 * The blocks this machine fills, facing north. The geothermal generator's lava tank sits to
	 * the right of its body (seen from the front); the wind turbine is three blocks tall.
	 */
	public Footprint footprint() {
		return switch (this) {
			case GEOTHERMAL_GENERATOR -> Footprint.of(Vec3i.ZERO, new Vec3i(-1, 0, 0));
			case WIND_TURBINE -> Footprint.tall(3);
			// Two wide and two tall: furnace body, crucible tower on its right, hoppers above.
			case ALLOY_SMELTER -> Footprint.of(Vec3i.ZERO, new Vec3i(-1, 0, 0), new Vec3i(0, 1, 0), new Vec3i(-1, 1, 0));
			default -> Footprint.SINGLE;
		};
	}

	/**
	 * Where cables connect, or null when every face of every block takes power. The alloy
	 * smelter's copper power socket is on the outer side of its lower right block.
	 */
	public @Nullable PowerPort powerPort() {
		return this == ALLOY_SMELTER ? new PowerPort(1, Direction.WEST) : null;
	}

	public boolean isLarge() {
		return footprint().size() > 1;
	}

	@Override
	public String getSerializedName() {
		return id;
	}
}
