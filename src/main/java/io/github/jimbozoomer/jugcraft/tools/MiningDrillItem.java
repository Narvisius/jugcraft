package io.github.jimbozoomer.jugcraft.tools;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The mining drill: a JE-powered pickaxe and shovel in one. Sneak and use it to switch between three
 * modes: one block, a 3×3 square facing the way you mine, or a whole ore vein (every touching block
 * of the same ore, up to {@link #VEIN_LIMIT}).
 */
public class MiningDrillItem extends PoweredToolItem {
	public static final int SINGLE = 0;
	public static final int AREA = 1;
	public static final int VEIN = 2;
	public static final int VEIN_LIMIT = 32;
	private static final String[] MODE_NAMES = {"single", "area", "vein"};
	private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));

	public MiningDrillItem(Properties properties, long capacity, long energyPerBlock) {
		super(properties, capacity, energyPerBlock);
	}

	public static int mode(ItemStack stack) {
		return Math.floorMod(stack.getOrDefault(JugcraftTools.DRILL_MODE, SINGLE), MODE_NAMES.length);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			int mode = (mode(stack) + 1) % MODE_NAMES.length;
			stack.set(JugcraftTools.DRILL_MODE, mode);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.drill_mode",
					Component.translatable("message.jugcraft.drill_mode." + MODE_NAMES[mode])));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void afterMining(ItemStack stack, ServerLevel level, BlockState state, BlockPos pos, ServerPlayer player) {
		switch (mode(stack)) {
			case AREA -> {
				Direction.Axis facing = miningAxis(player);
				for (int a = -1; a <= 1; a++) {
					for (int b = -1; b <= 1; b++) {
						if (a != 0 || b != 0) {
							breakExtra(stack, level, offset(pos, facing, a, b), player);
						}
					}
				}
			}
			case VEIN -> {
				if (state.is(ORES)) {
					mineVein(stack, level, state.getBlock(), pos, player);
				}
			}
			default -> {
			}
		}
	}

	/** The axis the player mines along: vertical when looking steeply up or down, else the way they face. */
	static Direction.Axis miningAxis(Player player) {
		return Math.abs(player.getXRot()) > 45 ? Direction.Axis.Y : player.getDirection().getAxis();
	}

	/** {@code pos} moved by (a, b) in the plane square to {@code axis}. */
	static BlockPos offset(BlockPos pos, Direction.Axis axis, int a, int b) {
		return switch (axis) {
			case X -> pos.offset(0, a, b);
			case Y -> pos.offset(a, 0, b);
			case Z -> pos.offset(a, b, 0);
		};
	}

	private void mineVein(ItemStack stack, ServerLevel level, Block ore, BlockPos start, ServerPlayer player) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(start);
		queue.add(start);
		int mined = 0;
		while (!queue.isEmpty() && mined < VEIN_LIMIT) {
			BlockPos pos = queue.poll();
			for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
				if (seen.size() > VEIN_LIMIT * 8 || mined >= VEIN_LIMIT) {
					return;
				}
				if (seen.add(next.immutable()) && level.getBlockState(next).is(ore)) {
					BlockPos found = next.immutable();
					if (breakExtra(stack, level, found, player)) {
						mined++;
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
		tooltip.accept(Component.translatable("tooltip.jugcraft.drill_mode",
				Component.translatable("message.jugcraft.drill_mode." + MODE_NAMES[mode(stack)])).withStyle(ChatFormatting.GRAY));
	}
}
