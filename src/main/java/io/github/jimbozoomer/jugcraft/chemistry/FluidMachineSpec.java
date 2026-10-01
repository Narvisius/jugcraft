package io.github.jimbozoomer.jugcraft.chemistry;

import java.util.List;

/**
 * The tanks and item slots of a fluid processing machine (see {@link io.github.jimbozoomer.jugcraft.machine.MachineKind#fluidSpec()}).
 * Input tanks take fluid from pipes, pumps and buckets; output tanks give it out and push it into neighbouring pipes and
 * tanks. Item slots are the inputs first, then the outputs. Capacities are in millibuckets.
 */
public record FluidMachineSpec(List<Integer> inputTanks, List<Integer> outputTanks, int itemInputs, int itemOutputs) {
	/** Most tanks one machine may have (inputs and outputs together); the menu syncs this many. */
	public static final int MAX_TANKS = 6;

	public FluidMachineSpec {
		inputTanks = List.copyOf(inputTanks);
		outputTanks = List.copyOf(outputTanks);
		if (inputTanks.size() + outputTanks.size() > MAX_TANKS) {
			throw new IllegalArgumentException("A fluid machine has at most " + MAX_TANKS + " tanks");
		}
	}

	public int tanks() {
		return inputTanks.size() + outputTanks.size();
	}

	public int itemSlots() {
		return itemInputs + itemOutputs;
	}

	/** Capacity of tank {@code index}, counting input tanks first. */
	public int capacity(int index) {
		return index < inputTanks.size() ? inputTanks.get(index) : outputTanks.get(index - inputTanks.size());
	}

	public boolean isInput(int index) {
		return index < inputTanks.size();
	}
}
