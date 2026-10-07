package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Harvest Scale, for weigh-offs. Put it beside a full-grown giant pumpkin (giant pumpkins cannot be
 * moved, so the scale comes to them) and use it with an empty hand: it weighs the pumpkin, keeps the
 * heaviest three it has weighed on its board, and gives a prize ribbon the first time a pumpkin places
 * ({@link HarvestScaleBlockEntity}). A comparator reads the last weight (15 at the heaviest a pumpkin grows).
 */
public class HarvestScaleBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	/** A low platform with a dial post at the back, by facing (the dial faces the player who placed it). */
	private static final VoxelShape[] SHAPES = new VoxelShape[4];

	static {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			// The dial post sits on the side away from where the scale faces.
			VoxelShape post = switch (direction) {
				case SOUTH -> Block.box(5.0, 4.0, 1.0, 11.0, 14.0, 4.0);
				case EAST -> Block.box(1.0, 4.0, 5.0, 4.0, 14.0, 11.0);
				case WEST -> Block.box(12.0, 4.0, 5.0, 15.0, 14.0, 11.0);
				default -> Block.box(5.0, 4.0, 12.0, 11.0, 14.0, 15.0);
			};
			SHAPES[direction.get2DDataValue()] = Shapes.or(Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0), post);
		}
	}

	public HarvestScaleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(FACING).get2DDataValue()];
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HarvestScaleBlockEntity(pos, state);
	}

	/** Weighs the full-grown giant pumpkin beside (or on) the scale; see {@link HarvestScaleBlockEntity#weigh}. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof HarvestScaleBlockEntity scale)) {
			return InteractionResult.SUCCESS;
		}
		GiantPumpkinBlockEntity pumpkin = pumpkinBeside(level, pos);
		if (pumpkin == null) {
			serverPlayer.sendOverlayMessage(Component.translatable("message.jugcraft.harvest_scale.no_pumpkin"));
			return InteractionResult.SUCCESS;
		}
		scale.weigh(serverPlayer, pumpkin);
		level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	/** The first full-grown giant pumpkin touching the scale's sides or top, if any: at most 5 blocks read. */
	static @Nullable GiantPumpkinBlockEntity pumpkinBeside(Level level, BlockPos pos) {
		for (Direction direction : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP}) {
			BlockPos next = pos.relative(direction);
			BlockState state = level.getBlockState(next);
			if (state.getBlock() instanceof GiantPumpkinBlock) {
				GiantPumpkinBlockEntity master = GiantPumpkinBlock.master(level, next, state);
				if (master != null && master.fullGrown()) {
					return master;
				}
			}
		}
		return null;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof HarvestScaleBlockEntity scale ? scale.signal() : 0;
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
