package io.github.jimbozoomer.jugcraft.chemistry;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/**
 * A petroleum fluid (see {@link PetroFluids}): placeable from its bucket and flowing like a thick liquid. It never
 * makes new source blocks, so oil can't be multiplied the way water can.
 */
public abstract class OilFluid extends FlowingFluid {
	protected final PetroFluids.Entry entry;

	protected OilFluid(PetroFluids.Entry entry) {
		this.entry = entry;
	}

	@Override
	public Fluid getFlowing() {
		return entry.flowing();
	}

	@Override
	public Fluid getSource() {
		return entry.source();
	}

	@Override
	public Item getBucket() {
		return entry.bucket();
	}

	@Override
	protected boolean canConvertToSource(ServerLevel level) {
		return false;
	}

	@Override
	protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
		BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
		Block.dropResources(state, level, pos, blockEntity);
	}

	@Override
	protected int getSlopeFindDistance(LevelReader level) {
		return entry.slope();
	}

	@Override
	protected int getDropOff(LevelReader level) {
		return entry.dropOff();
	}

	@Override
	public int getTickDelay(LevelReader level) {
		return entry.tickDelay();
	}

	@Override
	protected BlockState createLegacyBlock(FluidState state) {
		return entry.block().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
	}

	@Override
	public boolean isSame(Fluid fluid) {
		return fluid == entry.source() || fluid == entry.flowing();
	}

	@Override
	protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
		return direction == Direction.DOWN && !isSame(fluid);
	}

	@Override
	protected float getExplosionResistance() {
		return 100.0F;
	}

	@Override
	public Optional<SoundEvent> getPickupSound() {
		return Optional.of(SoundEvents.BUCKET_FILL);
	}

	/** The flowing form, with a level from 1 to 8. */
	public static class Flowing extends OilFluid {
		public Flowing(PetroFluids.Entry entry) {
			super(entry);
		}

		@Override
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getAmount(FluidState state) {
			return state.getValue(LEVEL);
		}

		@Override
		public boolean isSource(FluidState state) {
			return false;
		}
	}

	/** The source block: a full bucket's worth. */
	public static class Source extends OilFluid {
		public Source(PetroFluids.Entry entry) {
			super(entry);
		}

		@Override
		public int getAmount(FluidState state) {
			return 8;
		}

		@Override
		public boolean isSource(FluidState state) {
			return true;
		}
	}
}
