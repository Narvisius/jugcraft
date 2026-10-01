package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.minecraft.world.level.material.Fluid;

/**
 * One tank of a fluid processing machine. Seen from outside, an input tank only takes fluid (that {@code accepts}
 * allows) and an output tank only gives it; the machine itself fills and drains both directly.
 */
public class FluidTank extends SingleFluidStorage {
	private final long capacity;
	private final boolean input;
	private final Predicate<FluidVariant> accepts;
	private final Runnable onChange;

	public FluidTank(int capacityMb, boolean input, Predicate<FluidVariant> accepts, Runnable onChange) {
		this.capacity = capacityMb * FluidNetworks.DROPLETS_PER_MB;
		this.input = input;
		this.accepts = accepts;
		this.onChange = onChange;
	}

	@Override
	protected long getCapacity(FluidVariant variant) {
		return capacity;
	}

	@Override
	protected boolean canInsert(FluidVariant variant) {
		return input && accepts.test(variant);
	}

	@Override
	protected boolean canExtract(FluidVariant variant) {
		return !input;
	}

	@Override
	protected void onFinalCommit() {
		onChange.run();
	}

	public boolean isInput() {
		return input;
	}

	/** Millibuckets held. */
	public int millibuckets() {
		return (int) (amount / FluidNetworks.DROPLETS_PER_MB);
	}

	public int capacityMb() {
		return (int) (capacity / FluidNetworks.DROPLETS_PER_MB);
	}

	/** Whether the tank holds at least {@code mb} of {@code fluid}. */
	public boolean has(Fluid fluid, int mb) {
		return !variant.isBlank() && variant.isOf(fluid) && millibuckets() >= mb;
	}

	/** Whether {@code mb} of {@code fluid} would fit: the tank is empty or holds that fluid, with room. */
	public boolean fits(Fluid fluid, int mb) {
		return (variant.isBlank() || variant.isOf(fluid)) && millibuckets() + mb <= capacityMb();
	}

	/** The machine's own filling; call {@link #fits} first. */
	public void fill(Fluid fluid, int mb) {
		if (variant.isBlank()) {
			variant = FluidVariant.of(fluid);
		}
		amount += mb * FluidNetworks.DROPLETS_PER_MB;
	}

	/** The machine's own draining; call {@link #has} first. */
	public void drain(int mb) {
		amount = Math.max(0, amount - mb * FluidNetworks.DROPLETS_PER_MB);
		if (amount == 0) {
			variant = FluidVariant.blank();
		}
	}
}
