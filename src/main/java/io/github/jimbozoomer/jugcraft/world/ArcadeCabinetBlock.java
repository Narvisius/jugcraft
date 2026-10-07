package io.github.jimbozoomer.jugcraft.world;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The arcade cabinet: the Retro Trader's workstation, two blocks tall like a real one. The lower half is the job
 * site (see {@link RetroTrader}); the upper half holds the lit screen and marquee. Removing either half removes
 * the other, and only the lower half drops the cabinet (its loot table).
 */
public class ArcadeCabinetBlock extends Block {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	/** Shapes of a north-facing cabinet (tools/retro_models.py): the body with its control deck, then the screen and marquee. */
	private static final Map<Direction, VoxelShape> LOWER = shapes(1.5, 0, 1, 14.5, 16, 15);
	private static final Map<Direction, VoxelShape> UPPER = shapes(1.5, 0, 4.5, 14.5, 16, 15.5);

	public ArcadeCabinetBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF);
	}

	/** Faces the player; needs room for the upper half. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos above = context.getClickedPos().above();
		Level level = context.getLevel();
		if (above.getY() > level.getMaxY() || !level.getBlockState(above).canBeReplaced(context)) {
			return null;
		}
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER).get(state.getValue(FACING));
	}

	/** Village pieces are rotated and mirrored when placed; the cabinet turns with them. */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/**
	 * A creative player breaking the upper half takes the lower half away first, without drops, as vanilla does for
	 * doors and tall plants; otherwise removing the upper half below would destroy the lower half with its drop.
	 */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos lower = pos.below();
			if (level.getBlockState(lower).is(this)) {
				level.setBlock(lower, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** Removing either half removes the other; the lower half drops the cabinet (see its loot table). */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
		BlockPos other = lower ? pos.above() : pos.below();
		BlockState otherState = level.getBlockState(other);
		if (otherState.is(this) && otherState.getValue(HALF) != state.getValue(HALF)) {
			level.destroyBlock(other, !lower);
		}
	}

	/** One box, turned to each horizontal facing the way the block states turn the model. */
	private static Map<Direction, VoxelShape> shapes(double x0, double y0, double z0, double x1, double y1, double z1) {
		Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
		out.put(Direction.NORTH, Block.box(x0, y0, z0, x1, y1, z1));
		out.put(Direction.SOUTH, Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0));
		out.put(Direction.EAST, Block.box(16 - z1, y0, x0, 16 - z0, y1, x1));
		out.put(Direction.WEST, Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0));
		return out;
	}
}
