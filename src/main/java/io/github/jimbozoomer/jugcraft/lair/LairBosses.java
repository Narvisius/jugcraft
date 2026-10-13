package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.world.entity.player.Player;

/**
 * What every lair boss shares (Vesperine, Madame Tatterlace): who may fight one, its health for a party, and its damage,
 * each scaled by the server's {@code lairs.boss_health} and {@code lairs.boss_damage} (0.25 to 4, 1 by default).
 */
public final class LairBosses {
	private LairBosses() {
	}

	/** Damage a boss deals: the attack's own, times {@code lairs.boss_damage}. */
	public static float damage(float base) {
		return (float) (base * Lairs.decimal("lairs.boss_damage", 1.0, 0.25, 4.0));
	}

	/**
	 * A boss's health for a party of {@code players}: {@code step} as much again for each beyond the first, at most
	 * {@code max} times, then times {@code lairs.boss_health}.
	 */
	public static double partyScale(int players, double step, double max) {
		return Math.min(max, 1.0 + step * Math.max(0, players - 1)) * Lairs.decimal("lairs.boss_health", 1.0, 0.25, 4.0);
	}

	/** Whoever may fight a boss: alive, not a spectator, not invulnerable (creative). */
	public static boolean eligible(Player player) {
		return player.isAlive() && !player.isSpectator() && !player.getAbilities().invulnerable;
	}
}
