package io.github.jimbozoomer.jugcraft.concordance.dreaming;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Oneiric Censer's block (roadmap step 22): a hanging brass censer on a stand that smokes while someone dreams by
 * it. Used, it begins a dream ({@link Dreaming#begin}) or, for a dreamer, wakes them. GeckoLib draws it.
 */
public class OneiricCenserBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 15.0, 13.0);

	public OneiricCenserBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new OneiricCenserBlockEntity(pos, state);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		return use(level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return use(level, pos, player);
	}

	private static InteractionResult use(Level level, BlockPos pos, Player player) {
		if (player instanceof ServerPlayer server && level instanceof ServerLevel serverLevel) {
			if (!RateGate.allow(server, "dream", 20)) {
				return InteractionResult.FAIL;
			}
			Dreaming.use(server, serverLevel, pos);
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
