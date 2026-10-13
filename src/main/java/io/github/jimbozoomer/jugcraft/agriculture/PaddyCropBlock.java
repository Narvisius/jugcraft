package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * A paddy crop ({@link TallCrop#paddy}: rice, the kitchen and cooking expansion's slice 4; tools/rice.py): a {@link TallCropBlock}
 * whose bottom stands in a still water source one block deep, rooted in bog soil (block tag {@code jugcraft:bog_soil}, as the
 * cranberry bog's), with open air above the water for the plant to grow into. Like vanilla seagrass the bottom always holds its
 * water, so a broken plant leaves the paddy flooded; the sections above are dry. Flooded soil counts as moist farmland
 * ({@link CropGrowth#paddySpeed}). Picking, sickles and bone meal work as for any tall crop.
 */
public class PaddyCropBlock extends TallCropBlock implements LiquidBlockContainer {
	public PaddyCropBlock(Properties properties, TallCrop crop) {
		super(properties, crop);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(CranberryBushBlock.BOG_SOIL);
	}

	/** The bottom needs its water source (its own, once it stands) as well as the soil and light every tall crop needs. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(SECTION) == 0) {
			FluidState fluid = level.getFluidState(pos);
			if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) {
				return false;
			}
		}
		return super.canSurvive(state, level, pos);
	}

	/** Planted only into shallow water, as cranberries are. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return CranberryBushBlock.isShallowWater(context.getLevel(), context.getClickedPos()) ? super.getStateForPlacement(context) : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		// A bottom that cannot stay is destroyed as usual (dropping its loot), and leaves its water: the level puts back
		// what getFluidState says it held.
		BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
		if (state.getValue(SECTION) == 0 && !updated.isAir()) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return updated;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(SECTION) == 0 ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
	}

	@Override
	public boolean canPlaceLiquid(@Nullable LivingEntity entity, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
		return false;
	}

	@Override
	public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
		return false;
	}

	@Override
	protected float growthSpeed(ServerLevel level, BlockPos pos) {
		return CropGrowth.paddySpeed(level, pos);
	}
}
