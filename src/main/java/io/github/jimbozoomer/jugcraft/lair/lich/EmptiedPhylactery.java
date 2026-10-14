package io.github.jimbozoomer.jugcraft.lair.lich;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Emptied Phylactery (docs/features/crypt-lich.md): the Crypt Lich's urn, cracked and empty. Held in the offhand,
 * Wither never takes hold of whoever holds it: the effect is lifted from them at the end of every tick, and the harm it
 * would do meanwhile is turned aside. It does nothing against any other harm. Decided on the server; the work is bounded:
 * a look at each player's offhand once a tick.
 */
public final class EmptiedPhylactery {
	private EmptiedPhylactery() {
	}

	static void register() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(source.is(DamageTypes.WITHER) && holds(entity)));
		ServerTickEvents.END_SERVER_TICK.register(EmptiedPhylactery::tick);
	}

	/** Whether {@code living} holds an Emptied Phylactery in their offhand. */
	public static boolean holds(LivingEntity living) {
		return JugcraftLich.EMPTIED_PHYLACTERY != null && living.getOffhandItem().is(JugcraftLich.EMPTIED_PHYLACTERY);
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (holds(player) && player.hasEffect(MobEffects.WITHER)) {
				player.removeEffect(MobEffects.WITHER);
			}
		}
	}
}
