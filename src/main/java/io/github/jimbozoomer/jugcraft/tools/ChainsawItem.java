package io.github.jimbozoomer.jugcraft.tools;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The chainsaw: a JE-powered axe that also cuts leaves. Cutting a log fells the whole tree: every log
 * touching it (diagonals too), up to {@link #TREE_LIMIT}. Sneak to cut a single log.
 */
public class ChainsawItem extends PoweredToolItem {
	public static final int TREE_LIMIT = 128;

	public ChainsawItem(Properties properties, long capacity, long energyPerBlock) {
		super(properties, capacity, energyPerBlock);
	}

	@Override
	protected void afterMining(ItemStack stack, ServerLevel level, BlockState state, BlockPos pos, ServerPlayer player) {
		if (!state.is(BlockTags.LOGS) || player.isShiftKeyDown()) {
			return;
		}
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(pos);
		queue.add(pos);
		int cut = 0;
		while (!queue.isEmpty() && cut < TREE_LIMIT) {
			BlockPos log = queue.poll();
			for (BlockPos next : BlockPos.betweenClosed(log.offset(-1, 0, -1), log.offset(1, 1, 1))) {
				if (cut >= TREE_LIMIT || seen.size() > TREE_LIMIT * 8) {
					return;
				}
				if (seen.add(next.immutable()) && level.getBlockState(next).is(BlockTags.LOGS)) {
					BlockPos found = next.immutable();
					if (breakExtra(stack, level, found, player)) {
						cut++;
						queue.add(found);
					}
				}
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		super.appendHoverText(stack, context, display, tooltip, flag);
		tooltip.accept(Component.translatable("tooltip.jugcraft.chainsaw").withStyle(ChatFormatting.GRAY));
	}
}
