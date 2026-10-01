package io.github.jimbozoomer.jugcraft.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Holds the fluid a {@link FluidFilterBlock} lets out (blank: nothing). */
public class FluidFilterBlockEntity extends BlockEntity {
	private FluidVariant filter = FluidVariant.blank();

	public FluidFilterBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftFluids.FILTER_ENTITY, pos, state);
	}

	public FluidVariant filter() {
		return filter;
	}

	public void setFilter(FluidVariant filter) {
		this.filter = filter;
		setChanged();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		filter = input.read("filter", FluidVariant.CODEC).orElseGet(FluidVariant::blank);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!filter.isBlank()) {
			output.store("filter", FluidVariant.CODEC, filter);
		}
	}
}
