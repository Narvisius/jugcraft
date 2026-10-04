package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * An invisible part of a piece of nuclear equipment. It belongs to the anchor that placed it: using it is using the
 * equipment, and breaking it breaks the whole thing (its anchor drops the item once).
 */
public class EquipmentFrameBlock extends Block {
	public EquipmentFrameBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos anchor = EquipmentRegistry.of(level).anchorOf(pos);
		return anchor == null ? InteractionResult.PASS : EquipmentPlacement.use(level, anchor, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		BlockPos anchor = EquipmentRegistry.of(level).anchorOf(pos);
		return anchor == null ? InteractionResult.PASS : EquipmentPlacement.use(level, anchor, player, stack, hand);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockPos anchor = EquipmentRegistry.of(level).anchorOf(pos);
		if (!level.isClientSide() && anchor != null && !anchor.equals(pos)) {
			level.destroyBlock(anchor, !player.preventsBlockDrops(), player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (EquipmentPlacement.CLEARING.get()) {
			return;
		}
		BlockPos anchor = EquipmentRegistry.of(level).anchorOf(pos);
		if (anchor != null && level.getBlockState(anchor).getBlock() instanceof EquipmentAnchorBlock) {
			level.destroyBlock(anchor, true);
		}
	}
}
