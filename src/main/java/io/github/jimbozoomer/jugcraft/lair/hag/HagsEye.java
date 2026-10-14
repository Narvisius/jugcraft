package io.github.jimbozoomer.jugcraft.lair.hag;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Hag's Eye (docs/features/mire-hag.md): held in the offhand, it sees what hides. Every {@value #EVERY} ticks each
 * invisible creature within {@value #RANGE} blocks of whoever holds it glows for {@value #GLOW} ticks, as a spectral
 * arrow makes it glow (for everyone). Spectators and the holder are passed over. Decided on the server; work is bounded: one
 * look round each holder every {@value #EVERY} ticks.
 */
public final class HagsEye {
	public static final double RANGE = 16.0;
	public static final int EVERY = 20;
	public static final int GLOW = 30;

	private HagsEye() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(HagsEye::tick);
	}

	/** Whether {@code player} holds a Hag's Eye in their offhand. */
	public static boolean holds(ServerPlayer player) {
		return JugcraftHag.HAGS_EYE != null && player.getOffhandItem().is(JugcraftHag.HAGS_EYE);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % EVERY != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isSpectator() && holds(player)) {
				see(player);
			}
		}
	}

	/** Every invisible creature within {@value #RANGE} blocks of {@code player} glows; returns how many. */
	public static int see(ServerPlayer player) {
		int seen = 0;
		for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE),
				other -> other != player && other.isInvisible() && !other.isSpectator() && other.distanceToSqr(player) <= RANGE * RANGE)) {
			if (living.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW, 0, false, false), player)) {
				seen++;
			}
		}
		return seen;
	}
}
