package io.github.jimbozoomer.jugcraft.chemistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * The tanks and item slots of a fluid processing machine (see {@link io.github.jimbozoomer.jugcraft.machine.MachineKind#fluidSpec()}).
 * Input tanks take fluid from pipes, pumps and buckets; output tanks give it out and push it into neighbouring pipes and
 * tanks. Item slots are the inputs first, then the outputs. Capacities are in millibuckets.
 *
 * <p>Tanks are saved by position. When a later version of a machine adds or reorders tanks, it raises
 * {@link #layout()} and declares a {@link Migration} from each earlier layout (see {@link #migratedFrom}), so a world
 * saved before the change loads every tank's contents into the tank with the same role, and an output can never land
 * in an input merely because the list grew. Layout 0 is every machine as first released.
 */
public record FluidMachineSpec(List<Integer> inputTanks, List<Integer> outputTanks, int itemInputs, int itemOutputs, int layout,
		List<Migration> migrations) {
	/** Most tanks one machine may have (inputs and outputs together); the menu syncs this many. */
	public static final int MAX_TANKS = 6;

	/**
	 * How tanks saved by layout {@code from} load: old tank {@code i} goes to tank {@code newIndex[i]}. The first
	 * {@code oldInputs} old tanks were inputs and must stay inputs; the rest were outputs and must stay outputs.
	 */
	public record Migration(int from, int oldInputs, int[] newIndex) {
		public Migration {
			newIndex = newIndex.clone();
		}

		/** Where old tank {@code index} loads now. */
		public int target(int index) {
			return newIndex[index];
		}

		public int oldTanks() {
			return newIndex.length;
		}
	}

	public FluidMachineSpec(List<Integer> inputTanks, List<Integer> outputTanks, int itemInputs, int itemOutputs) {
		this(inputTanks, outputTanks, itemInputs, itemOutputs, 0, List.of());
	}

	public FluidMachineSpec {
		inputTanks = List.copyOf(inputTanks);
		outputTanks = List.copyOf(outputTanks);
		migrations = List.copyOf(migrations);
		if (inputTanks.size() + outputTanks.size() > MAX_TANKS) {
			throw new IllegalArgumentException("A fluid machine has at most " + MAX_TANKS + " tanks");
		}
		for (Migration migration : migrations) {
			if (migration.from() < 0 || migration.from() >= layout) {
				throw new IllegalArgumentException("A migration must come from an earlier layout than " + layout);
			}
			Set<Integer> targets = new HashSet<>();
			for (int old = 0; old < migration.oldTanks(); old++) {
				int target = migration.target(old);
				boolean wasInput = old < migration.oldInputs();
				boolean isInput = target >= 0 && target < inputTanks.size();
				if (target < 0 || target >= inputTanks.size() + outputTanks.size() || wasInput != isInput || !targets.add(target)) {
					throw new IllegalArgumentException("Layout " + migration.from() + " tank " + old
							+ " has no single tank of the same role to load into");
				}
			}
		}
	}

	/**
	 * This spec as the next layout: tanks saved by layout {@code from}, whose first {@code oldInputs} tanks were
	 * inputs, load old tank {@code i} into tank {@code newIndex[i]}.
	 */
	public FluidMachineSpec migratedFrom(int from, int oldInputs, int... newIndex) {
		List<Migration> all = new ArrayList<>(migrations);
		all.add(new Migration(from, oldInputs, newIndex));
		return new FluidMachineSpec(inputTanks, outputTanks, itemInputs, itemOutputs, Math.max(layout, from + 1), all);
	}

	/** The migration for tanks saved by layout {@code saved}, or null when none is declared. */
	public @Nullable Migration migration(int saved) {
		for (Migration migration : migrations) {
			if (migration.from() == saved) {
				return migration;
			}
		}
		return null;
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
