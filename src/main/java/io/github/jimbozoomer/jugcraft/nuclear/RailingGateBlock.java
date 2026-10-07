package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Map;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A self-closing safety gate in a railing, as at the top of a tank's ladder: it swings open (towards the side it
 * faces, the platform) as a player comes up to it, from either side, and its spring swings it shut behind them. It
 * can also be opened or shut by hand. Its posts are in the block model; the gate itself is drawn by
 * client/RailingGateRenderer, swinging.
 */
public class RailingGateBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	private static final Map<Direction, VoxelShape> CLOSED = Shapes.rotateHorizontal(Shapes.or(Block.box(0.4, 0, 7.1, 15.6, 24, 8.9)));
	private static final Map<Direction, VoxelShape> POSTS = Shapes.rotateHorizontal(
			Shapes.or(Block.box(0.4, 0, 7.1, 2.2, 24, 8.9), Block.box(13.8, 0, 7.1, 15.6, 24, 8.9)));
	private static final Map<Direction, VoxelShape> OUTLINE = Shapes.rotateHorizontal(Block.box(0.4, 0, 7.1, 15.6, 16, 8.9));

	public RailingGateBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// It swings away from whoever places it (they stand on the side it opens from).
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return OUTLINE.get(state.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (state.getValue(OPEN) ? POSTS : CLOSED).get(state.getValue(FACING));
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
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			swing(level, pos, state, !state.getValue(OPEN));
			if (level.getBlockEntity(pos) instanceof RailingGateBlockEntity gate) {
				gate.held = 40;
			}
		}
		return InteractionResult.SUCCESS;
	}

	static void swing(Level level, BlockPos pos, BlockState state, boolean open) {
		if (state.getValue(OPEN) == open) {
			return;
		}
		level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
		level.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6F,
				open ? 1.2F : 1.0F);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RailingGateBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != StorageTanks.GATE_ENTITY) {
			return null;
		}
		return (l, pos, s, entity) -> ((RailingGateBlockEntity) entity).serverTick((ServerLevel) l, pos, s);
	}
}
