package io.github.jimbozoomer.jugcraft.world;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The Pixel Hollows Map the Retro Trader sells: an unmarked map that, used, looks for the nearest Pixel Hollows
 * from where the player stands ({@link PixelHollowsMaps}, which only samples biome noise) and becomes an explorer map
 * marked with it. The search runs off the server thread, so a long one does not stall the game; the map is then
 * made on the server thread. With no cave in reach it says so and stays as it is, so it is never turned into a blank
 * or wrong map. A cooldown stops repeated searches.
 */
public class PixelHollowsMapItem extends Item {
	public static final int COOLDOWN_TICKS = 100;

	public PixelHollowsMapItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server && player instanceof ServerPlayer searcher) {
			player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
			BlockPos origin = player.blockPosition();
			CompletableFuture.supplyAsync(() -> PixelHollowsMaps.find(server, origin))
					.thenAcceptAsync(target -> deliver(server, searcher, target), server.getServer());
		}
		return InteractionResult.SUCCESS;
	}

	/** On the server thread: turns one of the player's unmarked maps into the marked map, or says none is in reach. */
	private static void deliver(ServerLevel level, ServerPlayer player, Optional<BlockPos> target) {
		if (player.isRemoved() || player.level() != level) {
			return;
		}
		if (target.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.pixel_hollows_map.none",
					String.format("%,d", PixelHollowsMaps.RADIUS)));
			return;
		}
		ItemStack unmarked = unmarkedMap(player);
		if (unmarked.isEmpty()) {
			return;
		}
		ItemStack map = PixelHollowsMaps.map(level, target.get());
		unmarked.shrink(1);
		if (!player.getInventory().add(map)) {
			Block.popResource(level, player.blockPosition(), map);
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.pixel_hollows_map.found", target.get().getY()));
	}

	/** The unmarked map in the player's hands, or else anywhere in their inventory (the search took a moment). */
	private static ItemStack unmarkedMap(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			if (player.getItemInHand(hand).is(RetroTrader.PIXEL_HOLLOWS_MAP)) {
				return player.getItemInHand(hand);
			}
		}
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			if (player.getInventory().getItem(slot).is(RetroTrader.PIXEL_HOLLOWS_MAP)) {
				return player.getInventory().getItem(slot);
			}
		}
		return ItemStack.EMPTY;
	}
}
