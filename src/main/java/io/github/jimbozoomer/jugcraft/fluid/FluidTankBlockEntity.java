package io.github.jimbozoomer.jugcraft.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Holds up to 16 buckets of a single fluid. It does no ticking: buckets and pumps fill and empty it. Broken, it drops
 * with its fluid ({@link StoredFluid}), and placed again it holds the same.
 */
public class FluidTankBlockEntity extends BlockEntity {
	public static final long CAPACITY = 16 * FluidConstants.BUCKET;

	public final SingleFluidStorage storage = SingleFluidStorage.withFixedCapacity(CAPACITY, this::setChanged);

	public FluidTankBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftFluids.TANK_ENTITY, pos, state);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		StoredFluid stored = StoredFluid.of(storage);
		if (stored != null) {
			components.set(JugcraftFluids.STORED_FLUID, stored);
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		StoredFluid stored = components.get(JugcraftFluids.STORED_FLUID);
		if (stored != null) {
			stored.restore(storage);
		}
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
