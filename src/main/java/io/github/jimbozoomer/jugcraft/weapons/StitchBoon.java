package io.github.jimbozoomer.jugcraft.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Stitch boon (the Needle Rapier, Madame Tatterlace's trophy): {@value #HITS} hits on the same foe within
 * {@value #WINDOW} ticks stitch it, Slowness II ({@value #SLOW_TICKS} ticks), and the count starts again. A hit on
 * another foe, or after the window, starts a new count. Each wielder has one count, which lives only while the server
 * runs; tools/tatterlace.py holds the numbers and tools/check_mod_data.py checks them.
 */
public final class StitchBoon {
	public static final int HITS = 3;
	public static final int WINDOW = 80;
	public static final int SLOW_TICKS = 40;
	public static final int SLOW_AMPLIFIER = 1;

	/** A wielder's count: the foe, how many times it has been struck, and when the first of those hits was. */
	private record Count(UUID foe, int hits, long since) {
	}

	private static final Map<UUID, Count> COUNTS = new HashMap<>();

	private StitchBoon() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> COUNTS.clear());
	}

	/** The rapier has struck {@code target}: one more stitch, and the third in the window pulls the thread tight. */
	static void struck(ServerLevel level, LivingEntity target, LivingEntity attacker) {
		long now = level.getGameTime();
		Count count = COUNTS.get(attacker.getUUID());
		boolean same = count != null && count.foe().equals(target.getUUID()) && now - count.since() < WINDOW;
		int hits = same ? count.hits() + 1 : 1;
		double y = target.getY() + target.getBbHeight() * 0.6;
		if (hits >= HITS) {
			COUNTS.remove(attacker.getUUID());
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, SLOW_AMPLIFIER), attacker);
			level.sendParticles(ParticleTypes.ITEM_COBWEB, target.getX(), y, target.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 1.6F);
			return;
		}
		COUNTS.put(attacker.getUUID(), new Count(target.getUUID(), hits, same ? count.since() : now));
		level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), y, target.getZ(), 3, 0.2, 0.3, 0.2, 0.02);
	}

	/** How many stitches {@code wielder} has put in {@code foe} so far (0 when their count is for another foe). */
	public static int stitches(LivingEntity wielder, LivingEntity foe) {
		Count count = COUNTS.get(wielder.getUUID());
		return count != null && count.foe().equals(foe.getUUID()) ? count.hits() : 0;
	}
}
