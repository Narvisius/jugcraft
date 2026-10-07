package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Black Light: a violet fluorescent tube in a black fixture, hung on a wall. Switched on by hand ({@link #LIT}) or
 * by a redstone signal ({@link #POWERED}), it glows violet (light {@value #LIGHT}) and makes Glow Paint within
 * {@value #RANGE} blocks blaze out of the dark ({@link #glow}). Which lights are on is known to each client from the
 * block states it already has ({@link BlackLightBlockEntity}); nothing is searched for.
 */
public class BlackLightBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final int LIGHT = 6;
	public static final double RANGE = 6.0;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private final VoxelShape[] shapes = new VoxelShape[4];

	public BlackLightBlock(Properties properties) {
		super(properties);
		shapes[Direction.NORTH.get2DDataValue()] = Block.box(1.0, 9.0, 12.5, 15.0, 12.0, 16.0);
		shapes[Direction.SOUTH.get2DDataValue()] = Block.box(1.0, 9.0, 0.0, 15.0, 12.0, 3.5);
		shapes[Direction.EAST.get2DDataValue()] = Block.box(0.0, 9.0, 1.0, 3.5, 12.0, 15.0);
		shapes[Direction.WEST.get2DDataValue()] = Block.box(12.5, 9.0, 1.0, 16.0, 12.0, 15.0);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(POWERED, false));
	}

	/** Whether it shines, switched on by hand or by redstone. */
	public static boolean shining(BlockState state) {
		return state.getValue(LIT) || state.getValue(POWERED);
	}

	public static int light(BlockState state) {
		return shining(state) ? LIGHT : 0;
	}

	/** How brightly Glow Paint {@code distance} blocks from a black light glows: fully within half its range, fading out by its edge. */
	public static float glow(double distance) {
		return (float) Mth.clamp((RANGE - distance) / (RANGE / 2), 0.0, 1.0);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlackLightBlockEntity(pos, state);
	}

	/** On each client, a shining light says where it is every tick, for Glow Paint to find. */
	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!level.isClientSide() || !shining(state) || type != JugcraftAgriculture.BLACK_LIGHT_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> BlackLightBlockEntity.shine(tickLevel, pos);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes[state.getValue(FACING).get2DDataValue()];
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos wall = pos.relative(facing.getOpposite());
		return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
	}

	/** Hangs on the clicked wall, or the nearest wall the player looks towards. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		for (Direction direction : context.getNearestLookingDirections()) {
			if (direction.getAxis().isHorizontal()) {
				BlockState state = defaultBlockState().setValue(FACING, direction.getOpposite())
						.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
				if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
					return state;
				}
			}
		}
		return null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	/** Switches it on or off. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			boolean on = !state.getValue(LIT);
			level.setBlock(pos, state.setValue(LIT, on), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, on ? 0.9F : 0.7F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** A redstone signal switches it on while it lasts. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (!level.isClientSide()) {
			boolean powered = level.hasNeighborSignal(pos);
			if (powered != state.getValue(POWERED)) {
				level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT, POWERED);
	}
}
