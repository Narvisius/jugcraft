package io.github.jimbozoomer.jugcraft.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Holds up to 16 buckets of a single fluid. It does no ticking: buckets and pumps fill and empty it. */
public class FluidTankBlockEntity extends BlockEntity {
	public static final long CAPACITY = 16 * FluidConstants.BUCKET;

	public final SingleFluidStorage storage = SingleFluidStorage.withFixedCapacity(CAPACITY, this::setChanged);

	public FluidTankBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftFluids.TANK_ENTITY, pos, state);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		storage.readValue(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		storage.writeValue(output);
	}
}
