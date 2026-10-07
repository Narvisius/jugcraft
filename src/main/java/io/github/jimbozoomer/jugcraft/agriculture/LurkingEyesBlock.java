package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Lurking Eyes: tuck them against a hedge, a bush, a tree or a wall, and at night a pair of glowing eyes peers out of
 * it, blinking now and then. Come within {@value #HIDE_DISTANCE} blocks and they are gone. By day there is nothing to
 * see. Each client draws them for its own player (client/LurkingEyesRenderer.java), so nothing runs on the server.
 */
public class LurkingEyesBlock extends BaseEntityBlock {
	public static final double HIDE_DISTANCE = 4.0;
	/** Every so many ticks the eyes blink, shut for {@value #BLINK_TICKS} ticks. */
	public static final int BLINK_PERIOD = 90;
	public static final int BLINK_TICKS = 4;
	public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
	/** A thin slab against the block the eyes peer out of, by the way they look. */
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Block.box(3.0, 4.0, 15.0, 13.0, 12.0, 16.0), Direction.SOUTH, Block.box(3.0, 4.0, 0.0, 13.0, 12.0, 1.0),
			Direction.WEST, Block.box(15.0, 4.0, 3.0, 16.0, 12.0, 13.0), Direction.EAST, Block.box(0.0, 4.0, 3.0, 1.0, 12.0, 13.0),
			Direction.UP, Block.box(3.0, 0.0, 4.0, 13.0, 1.0, 12.0), Direction.DOWN, Block.box(3.0, 15.0, 4.0, 13.0, 16.0, 12.0));

	public LurkingEyesBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.LURKING_EYES_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	/** They peer out of leaves, or any solid side. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos behind = pos.relative(facing.getOpposite());
		BlockState holder = level.getBlockState(behind);
		return holder.is(BlockTags.LEAVES) || holder.isFaceSturdy(level, behind, facing);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** Whether eyes at {@code pos} are shut in a blink at {@code gameTime}; each pair blinks in its own rhythm. */
	public static boolean blinking(BlockPos pos, long gameTime) {
		return Math.floorMod(gameTime + pos.hashCode(), BLINK_PERIOD) < BLINK_TICKS;
	}

	/** Whether eyes show at all: at night, to a viewer at least {@link #HIDE_DISTANCE} away. */
	public static boolean showing(boolean night, double viewerDistance) {
		return night && viewerDistance >= HIDE_DISTANCE;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
