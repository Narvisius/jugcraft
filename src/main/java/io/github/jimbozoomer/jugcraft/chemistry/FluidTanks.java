package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The tanks of one fluid processing machine (see {@link FluidMachineSpec}): input tanks first, then output tanks.
 * Pipes, pumps and buckets see them as one storage that takes fluid into the inputs and gives it from the outputs.
 */
public class FluidTanks {
	/** A block of the machine and the faces of it that touch the outside, where outputs are pushed. */
	public record Port(BlockPos pos, List<Direction> sides) {
	}

	private final FluidMachineSpec spec;
	private final List<FluidTank> tanks = new ArrayList<>();
	private final Storage<FluidVariant> exposed;

	/** {@code accepts} decides which fluids input tank {@code i} takes from outside (the machine's recipes). */
	public FluidTanks(FluidMachineSpec spec, BiPredicate<Integer, FluidVariant> accepts, Runnable onChange) {
		this.spec = spec;
		for (int i = 0; i < spec.tanks(); i++) {
			int index = i;
			tanks.add(new FluidTank(spec.capacity(i), spec.isInput(i), variant -> accepts.test(index, variant), onChange));
		}
		this.exposed = new CombinedStorage<>(List.copyOf(tanks));
	}

	public FluidMachineSpec spec() {
		return spec;
	}

	public Storage<FluidVariant> exposed() {
		return exposed;
	}

	public int size() {
		return tanks.size();
	}

	public FluidTank tank(int index) {
		return tanks.get(index);
	}

	/** Input tank {@code index}. */
	public FluidTank input(int index) {
		return tanks.get(index);
	}

	/** Output tank {@code index}. */
	public FluidTank output(int index) {
		return tanks.get(spec.inputTanks().size() + index);
	}

	/**
	 * Pushes each output tank's fluid out of the machine's outer faces: into neighbouring fluid storages, or through
	 * pipes like a pump does. Returns whether anything moved.
	 */
	public boolean pushOutputs(Level level, List<Port> ports, long maxDroplets) {
		boolean moved = false;
		for (int i = 0; i < spec.outputTanks().size(); i++) {
			FluidTank tank = output(i);
			for (Port port : ports) {
				if (tank.amount <= 0) {
					break;
				}
				moved |= FluidNetworks.pushToNeighbors(level, port.pos(), tank, maxDroplets, port.sides()) > 0;
			}
		}
		return moved;
	}

	/** 0-15 for comparators: how full the tanks are on average. */
	public int comparatorSignal() {
		float fill = 0;
		for (FluidTank tank : tanks) {
			fill += tank.amount / (float) tank.getCapacity();
		}
		return fill <= 0 ? 0 : 1 + (int) (fill / tanks.size() * 14);
	}

	/** Two synced values per tank (see the machine menu): the fluid's registry id, then millibuckets. */
	public int data(int index) {
		FluidTank tank = tanks.get(index / 2);
		if (index % 2 == 1) {
			return tank.millibuckets();
		}
		return tank.variant.isBlank() ? 0 : BuiltInRegistries.FLUID.getId(tank.variant.getFluid());
	}

	public void save(ValueOutput output) {
		for (int i = 0; i < tanks.size(); i++) {
			FluidTank tank = tanks.get(i);
			output.store("tank" + i + "_fluid", FluidVariant.CODEC, tank.variant);
			output.putLong("tank" + i + "_amount", tank.amount);
		}
	}

	public void load(ValueInput input) {
		for (int i = 0; i < tanks.size(); i++) {
			FluidTank tank = tanks.get(i);
			tank.variant = input.read("tank" + i + "_fluid", FluidVariant.CODEC).orElseGet(FluidVariant::blank);
			tank.amount = Math.min(tank.getCapacity(), Math.max(0, input.getLongOr("tank" + i + "_amount", 0L)));
			if (tank.variant.isBlank()) {
				tank.amount = 0;
			}
		}
	}
}
