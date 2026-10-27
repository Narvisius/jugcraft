package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Fright boon (the Hallowreap and the Crowbill, the Pumpkin King's trophies): a struck monster (a creature that hunts
 * players and walks, never a boss: {@code #jugcraft:frightless} names the bosses, the conventional {@code #c:bosses} and
 * every lair boss) panics for {@value #PANIC_TICKS} ticks: it drops whoever it was after and runs {@value #RUN} blocks
 * away from the one who struck it. Each foe can be frightened once every {@value #COOLDOWN} ticks; a hit meanwhile does
 * nothing more. At most {@value #MAX} creatures panic at once, and what is kept lives only while the server runs;
 * tools/arms_variants.py holds the numbers and tools/check_mod_data.py checks them.
 */
public final class FrightBoon {
	public static final int PANIC_TICKS = 30;
	public static final int COOLDOWN = 120;
	public static final double RUN = 8.0;
	/** How fast it runs, as a share of its own pace. */
	public static final double SPEED = 1.4;
	public static final int MAX = 256;
	public static final TagKey<EntityType<?>> FRIGHTLESS = TagKey.create(Registries.ENTITY_TYPE, Jugcraft.id("frightless"));
	/** A crow-black puff over a frightened foe. */
	private static final DustParticleOptions PUFF = new DustParticleOptions(0x14121A, 1.2F);

	/** A creature in a panic: its level, where it runs to, and the tick its panic ends. */
	private record Panic(ResourceKey<Level> level, Vec3 to, long until) {
	}

	private static final Map<UUID, Panic> PANICKED = new HashMap<>();
	/** When each foe was last frightened (forgotten once its cooldown is over). */
	private static final Map<UUID, Long> LAST = new HashMap<>();

	private FrightBoon() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PANICKED.clear();
			LAST.clear();
		});
		ServerTickEvents.END_LEVEL_TICK.register(FrightBoon::tick);
	}

	/** Whether the Fright boon can frighten {@code target}: a monster that walks, never a boss. */
	public static boolean frightens(LivingEntity target) {
		return target instanceof PathfinderMob && target instanceof Enemy && !target.is(FRIGHTLESS);
	}

	/** Whether {@code target} is in a panic now. */
	public static boolean panicking(LivingEntity target) {
		return PANICKED.containsKey(target.getUUID());
	}

	/** The arm has struck {@code target}: a monster not frightened lately panics, running from {@code attacker}. */
	static void struck(ServerLevel level, LivingEntity target, LivingEntity attacker) {
		if (!frightens(target)) {
			return;
		}
		long now = level.getGameTime();
		LAST.values().removeIf(at -> now - at >= COOLDOWN || now < at);
		if (LAST.containsKey(target.getUUID()) || PANICKED.size() >= MAX) {
			return;
		}
		Vec3 away = new Vec3(target.getX() - attacker.getX(), 0.0, target.getZ() - attacker.getZ());
		away = away.lengthSqr() < 1.0E-4 ? attacker.getLookAngle().multiply(1.0, 0.0, 1.0) : away;
		away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
		LAST.put(target.getUUID(), now);
		PANICKED.put(target.getUUID(), new Panic(level.dimension(), target.position().add(away.scale(RUN)), now + PANIC_TICKS));
		((PathfinderMob) target).setTarget(null);
		run((PathfinderMob) target, PANICKED.get(target.getUUID()));
		level.sendParticles(PUFF, target.getX(), target.getY() + target.getBbHeight() + 0.3, target.getZ(), 8, 0.3, 0.2, 0.3, 0.02);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PARROT_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.5F);
	}

	/** Each panicking creature in {@code level}, a tick on: it drops its target and keeps running, until its panic ends. */
	private static void tick(ServerLevel level) {
		if (PANICKED.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		for (Iterator<Map.Entry<UUID, Panic>> it = PANICKED.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Panic> entry = it.next();
			Panic panic = entry.getValue();
			if (!panic.level().equals(level.dimension())) {
				continue;
			}
			Entity entity = level.getEntity(entry.getKey());
			if (!(entity instanceof PathfinderMob mob) || !mob.isAlive() || now >= panic.until() || now < panic.until() - PANIC_TICKS) {
				it.remove();
				continue;
			}
			// Its own mind may have picked a target again this tick, and set off after it: it drops it, and runs on.
			if (mob.getTarget() != null || mob.getNavigation().isDone()) {
				mob.setTarget(null);
				run(mob, panic);
			}
		}
	}

	private static void run(PathfinderMob mob, Panic panic) {
		mob.getNavigation().moveTo(panic.to().x, panic.to().y, panic.to().z, SPEED);
	}
}
