package io.github.jimbozoomer.jugcraft.lair.sentinel;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Lowfall Core (docs/features/cydonian-sentinel.md): a fist-sized sphere of the Cydonian Sentinel's red light, banded
 * in iron. Held in the offhand, its holder falls as on Mars: slowly, and so with no fall damage. Slow Falling is kept up
 * on them ({@value #TICKS} ticks, renewed every {@value #REFRESH}), quietly; it lapses soon after the core is put away.
 * Decided on the server; the work is bounded: a look at each player's offhand every {@value #REFRESH} ticks.
 */
public final class LowfallCore {
	public static final int TICKS = 40;
	public static final int REFRESH = 10;

	private LowfallCore() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(LowfallCore::tick);
	}

	/** Whether {@code living} holds a Lowfall Core in their offhand. */
	public static boolean holds(LivingEntity living) {
		return JugcraftSentinel.LOWFALL_CORE != null && living.getOffhandItem().is(JugcraftSentinel.LOWFALL_CORE);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % REFRESH != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			fall(player);
		}
	}

	/** Keeps {@code player} falling slowly while they hold a Lowfall Core. Returns whether they do. */
	public static boolean fall(ServerPlayer player) {
		if (!holds(player)) {
			return false;
		}
		MobEffectInstance slow = player.getEffect(MobEffects.SLOW_FALLING);
		if (slow == null || slow.getDuration() <= TICKS - REFRESH) {
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, TICKS, 0, true, false, true));
		}
		return true;
	}
}
