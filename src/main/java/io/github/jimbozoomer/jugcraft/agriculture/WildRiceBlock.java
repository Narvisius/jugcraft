package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * Wild rice (the kitchen and cooking expansion's slice 4; tools/rice.py WILD_RICE): a two-block plant found in swamps and
 * along rivers, its foot in a still water source one block deep over bog soil (as the paddy crop's) and its top in the
 * air. Like vanilla seagrass the foot holds its water, so the plant leaves its water when it goes. Broken at either half,
 * it gives 1-2 rice from its foot's loot (shears take the plant), a seed source as the other wild plants are.
 */
public class WildRiceBlock extends DoublePlantBlock implements LiquidBlockContainer {
	public WildRiceBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(CranberryBushBlock.BOG_SOIL);
	}

	/** The foot needs its water source (its own, once it stands); the top needs the foot. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
			FluidState fluid = level.getFluidState(pos);
			if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) {
				return false;
			}
		}
		return super.canSurvive(state, level, pos);
	}

	/** Planted only into shallow water with air above it, where the top goes. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return CranberryBushBlock.isShallowWater(context.getLevel(), context.getClickedPos()) ? super.getStateForPlacement(context) : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
		if (state.getValue(HALF) == DoubleBlockHalf.LOWER && !updated.isAir()) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return updated;
	}

	/**
	 * Breaking the top drops the foot's loot now, with the player's tool (so shears take the plant from either half), and
	 * leaves the foot's water; a creative player gets nothing. The tall crops do the same.
	 */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos below = pos.below();
			BlockState foot = level.getBlockState(below);
			if (foot.is(this) && foot.getValue(HALF) == DoubleBlockHalf.LOWER) {
				if (!player.preventsBlockDrops()) {
					Block.dropResources(foot, level, below, null, player, player.getMainHandItem());
				}
				level.setBlock(below, foot.getFluidState().createLegacyBlock(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				level.levelEvent(player, LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, below, Block.getId(foot));
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
	}

	@Override
	public boolean canPlaceLiquid(@Nullable LivingEntity entity, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
		return false;
	}

	@Override
	public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
		return false;
	}
}
