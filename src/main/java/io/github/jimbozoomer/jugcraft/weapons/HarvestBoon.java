package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.lair.vesperine.ReapingCrescentEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Harvest boon (the Vesper Scythe, Vesperine's trophy): a kill with it heals the wielder {@value #HEAL} health (two
 * hearts), at most once every {@value #HEAL_COOLDOWN} ticks, and every {@value #CRESCENT_KILLS}th kill charges the
 * scythe, so that its next blow looses a pale crescent ({@link ReapingCrescentEntity}) on along the wielder's look,
 * {@value #CRESCENT_RANGE} blocks, {@value #CRESCENT_DAMAGE} damage to each foe it passes. The count lives only while
 * the server runs.
 */
public final class HarvestBoon {
	public static final float HEAL = 4.0F;
	public static final int HEAL_COOLDOWN = 100;
	public static final int CRESCENT_KILLS = 5;
	public static final double CRESCENT_RANGE = 12.0;
	public static final float CRESCENT_DAMAGE = 8.0F;

	/** Each wielder's kills, whether the scythe is charged, and when it last healed them. */
	private static final Map<UUID, int[]> KILLS = new HashMap<>();
	private static final Map<UUID, Long> HEALED = new HashMap<>();

	private HarvestBoon() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			KILLS.clear();
			HEALED.clear();
		});
	}

	/** The scythe has struck {@code target}: a charged blow looses its crescent; a kill heals and counts. */
	static void struck(ServerLevel level, LivingEntity target, LivingEntity attacker) {
		int[] state = KILLS.computeIfAbsent(attacker.getUUID(), id -> new int[2]);
		if (state[1] == 1) {
			state[1] = 0;
			ReapingCrescentEntity.loose(level, attacker);
		}
		if (!target.isDeadOrDying()) {
			return;
		}
		long now = level.getGameTime();
		Long healed = HEALED.get(attacker.getUUID());
		if (healed == null || now - healed >= HEAL_COOLDOWN) {
			attacker.heal(HEAL);
			HEALED.put(attacker.getUUID(), now);
			level.sendParticles(ParticleTypes.SOUL, attacker.getX(), attacker.getY() + 1.0, attacker.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
		}
		if (++state[0] % CRESCENT_KILLS == 0) {
			state[1] = 1;
			level.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
			level.sendParticles(ParticleTypes.END_ROD, attacker.getX(), attacker.getY() + 1.2, attacker.getZ(), 10, 0.4, 0.5, 0.4, 0.02);
		}
	}

	/** Whether the wielder's scythe is charged. */
	public static boolean charged(LivingEntity wielder) {
		int[] state = KILLS.get(wielder.getUUID());
		return state != null && state[1] == 1;
	}

	/** Whether a crescent loosed by {@code player} may strike {@code foe} (as their own blow could). */
	public static boolean mayStrike(ServerPlayer player, ServerLevel level, LivingEntity foe) {
		return TwoHanded.target(player, foe) && TwoHanded.allowed(player, level, foe);
	}
}
