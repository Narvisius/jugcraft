package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Examining a specimen: sneak and use one ({@code #jugcraft:concordance_specimens}: the luminous specimens First Light
 * learns from, and each later entry's own). The server reads the light where the player's eyes are and records an
 * {@link Evidence.Examined}; for a luminous specimen ({@code #jugcraft:luminous_specimens}), in darkness (light
 * {@value #DARK_LIGHT} or less, as in a cave or the open air at night) the glow is the specimen's own. Sneaking keeps eating and other uses of these
 * items (glow berries are food) on the plain use key.
 * <p>
 * One examination per player per {@value #COOLDOWN_TICKS} ticks; extra requests in between are ignored, so holding the
 * key does no work. Keep DARK_LIGHT equal to tools/concordance.py.
 */
public final class Examination {
	public static final int DARK_LIGHT = 4;
	public static final int COOLDOWN_TICKS = 10;
	private static final Map<UUID, Integer> LAST = new HashMap<>();

	private Examination() {
	}

	static void register() {
		UseItemCallback.EVENT.register(Examination::use);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST.remove(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LAST.clear());
	}

	/** Forgets when the player last examined (tests examine several times in a row). */
	public static void forget(UUID player) {
		LAST.remove(player);
	}

	private static InteractionResult use(Player player, Level level, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!player.isShiftKeyDown() || !stack.is(JugcraftConcordance.SPECIMENS) || player.isSpectator()) {
			return InteractionResult.PASS;
		}
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS; // the server examines; the client must not eat the berries meanwhile
		}
		examine(server, serverLevel, stack);
		return InteractionResult.SUCCESS;
	}

	/** Examines the specimen the player holds; returns the research result (or null when refused or too soon). */
	public static ResearchEngine.@Nullable Result examine(ServerPlayer player, ServerLevel level, ItemStack stack) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return null;
		}
		int tick = level.getServer().getTickCount();
		Integer last = LAST.get(player.getUUID());
		if (last != null && tick - last < COOLDOWN_TICKS && tick >= last) {
			return null;
		}
		LAST.put(player.getUUID(), tick);
		BlockPos eyes = BlockPos.containing(player.getEyePosition());
		int light = level.getMaxLocalRawBrightness(eyes);
		String item = ConcordanceProgress.itemId(stack);
		ResearchEngine.Result result = ConcordanceProgress.record(player, new Evidence.Examined(item, light));
		Component name = stack.getHoverName();
		boolean dark = light <= DARK_LIGHT;
		if (result.transitions().isEmpty()) {
			if (stack.is(JugcraftConcordance.ALCHEMY_SPECIMENS) && !stack.is(JugcraftConcordance.LUMINOUS)) {
				// The Alembic Arts learn from what a thing is made of.
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.examine.substance", name));
			} else if (stack.is(JugcraftConcordance.EMBER_SPECIMENS) && !stack.is(JugcraftConcordance.LUMINOUS)) {
				// Hearthbinding learns from the fire a thing holds.
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.examine.heat", name));
			} else if (!stack.is(JugcraftConcordance.LUMINOUS)) {
				// Circle Lore learns from how a thing is made, not from its light.
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.examine.form", name));
			} else {
				player.sendOverlayMessage(Component.translatable(dark ? (result.recorded() ? "message.jugcraft.concordance.examine.dark"
						: "message.jugcraft.concordance.examine.known") : "message.jugcraft.concordance.examine.bright", name));
			}
		}
		level.playSound(null, player.blockPosition(), JugcraftConcordance.EXAMINE_SOUND, SoundSource.PLAYERS, 0.6F,
				dark ? 1.0F : 0.8F);
		return result;
	}
}
