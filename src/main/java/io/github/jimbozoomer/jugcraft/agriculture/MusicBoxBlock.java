package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Spooky Music Box: a little lacquered box with brass corners. Power it with redstone and its lid opens on a tiny
 * dancing ghost while it plays its tune ({@link MusicBoxBlockEntity#TUNE}, an original waltz in A minor on note-block
 * sounds), over and over while the power stays on, finishing the tune when it goes. Use it with an empty hand to wind
 * it for one play, or to shut it while it plays. The server plays the notes; nothing ticks while the lid is shut.
 */
public class MusicBoxBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 4.0, 13.0, 6.0, 12.0);

	public MusicBoxBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(POWERED, false));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MusicBoxBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || !state.getValue(OPEN) || type != JugcraftAgriculture.MUSIC_BOX_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((MusicBoxBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	/** A rising redstone signal opens the lid and starts the tune; it plays on while the power stays. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		boolean powered = level.hasNeighborSignal(pos);
		if (level.isClientSide() || powered == state.getValue(POWERED)) {
			return;
		}
		if (powered && !state.getValue(OPEN)) {
			play(level, pos, state.setValue(POWERED, true));
		} else {
			level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
		}
	}

	/** Winds the box for one play, or shuts it while it plays. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			if (state.getValue(OPEN)) {
				level.setBlock(pos, state.setValue(OPEN, false), Block.UPDATE_ALL);
				level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.4F, 1.6F);
			} else {
				level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, 1.8F);
				play(level, pos, state);
			}
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	private static void play(Level level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof MusicBoxBlockEntity box) {
			box.start();
		}
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
		builder.add(FACING, OPEN, POWERED);
	}
}
