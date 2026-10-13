package io.github.jimbozoomer.jugcraft.concordance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Circle Anchor: where a ritual is worked (roadmap step 12). Use an item on it to offer it; use it empty-handed to
 * begin the ritual its offerings answer (or join one gathering), to take a completed result, or to hear how the circle
 * stands; sneak empty-handed to take the offerings back, or, leading a ritual, to call it off. GeckoLib draws it
 * (client/concordance/CircleAnchorRenderer); its block model is only the breaking particles. Everything else is
 * {@link CircleAnchorBlockEntity}.
 */
public class CircleAnchorBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(1.0, 0.0, 1.0, 15.0, 4.0, 15.0), Block.box(3.0, 4.0, 3.0, 13.0, 10.0, 13.0));

	public CircleAnchorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CircleAnchorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftConcordance.ANCHOR_ENTITY, (tickLevel, pos, tickState, anchor) -> anchor.serverTick((ServerLevel) tickLevel));
	}

	/** An item used on the anchor (main hand) is offered to it. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.isEmpty() || hand != InteractionHand.MAIN_HAND) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof CircleAnchorBlockEntity anchor) {
			anchor.offer(server, stack);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel
				&& level.getBlockEntity(pos) instanceof CircleAnchorBlockEntity anchor) {
			anchor.use(server, serverLevel, player.isShiftKeyDown());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}
}
