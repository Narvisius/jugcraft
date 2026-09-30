package io.github.jimbozoomer.jugcraft.machine;

import net.minecraft.util.StringRepresentable;

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
	ALLOY_SMELTER("alloy_smelter", 10_000, 128, 0, 20, 3);

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
		return this == ELECTRIC_FURNACE || this == CRUSHER || this == ARC_FURNACE || this == ALLOY_SMELTER;
	}

	/** The output slot of a processor (its last slot). */
	public int outputSlot() {
		return slots - 1;
	}

	/** Generators only produce energy; they never accept it. */
	public boolean isGenerator() {
		return this == COAL_GENERATOR || this == SOLAR_PANEL || this == STEAM_GENERATOR;
	}

	@Override
	public String getSerializedName() {
		return id;
	}
}
