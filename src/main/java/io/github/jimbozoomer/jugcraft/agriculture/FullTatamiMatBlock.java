package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * A Full Tatami Mat (the kitchen and cooking expansion's slice 4; tools/rice.py TATAMI_MATS): a tatami mat two blocks long,
 * laid as a bed is, its foot where the player uses it and its head ahead in the direction they face (the owner's even and
 * odd halves). Each half needs the other; it drops once, from its foot.
 */
public class FullTatamiMatBlock extends TatamiMatBlock {
	public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;

	public FullTatamiMatBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT));
	}

	/** Where the other half of a half at {@code pos} lies. */
	private static BlockPos partner(BlockPos pos, BlockState state) {
		Direction facing = state.getValue(FACING);
		return pos.relative(state.getValue(PART) == BedPart.FOOT ? facing : facing.getOpposite());
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		BlockPos head = context.getClickedPos().relative(facing);
		Level level = context.getLevel();
		BlockState foot = defaultBlockState().setValue(FACING, facing).setValue(PART, BedPart.FOOT);
		return level.getBlockState(head).canBeReplaced(context) && level.getWorldBorder().isWithinBounds(head)
				&& foot.setValue(PART, BedPart.HEAD).canSurvive(level, head) ? foot : null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide()) {
			level.setBlock(pos.relative(state.getValue(FACING)), state.setValue(PART, BedPart.HEAD), Block.UPDATE_ALL);
		}
	}

	/** A half whose other half is gone goes too (the foot dropping the mat). */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (neighborPos.equals(partner(pos, state)) && !(neighborState.is(this) && neighborState.getValue(PART) != state.getValue(PART))) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	/** A creative player takes the whole mat with no drop, as a bed. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.preventsBlockDrops()) {
			BlockPos other = partner(pos, state);
			BlockState otherState = level.getBlockState(other);
			if (otherState.is(this) && otherState.getValue(PART) != state.getValue(PART)) {
				level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART);
	}
}
