package io.github.jimbozoomer.jugcraft.rocketry;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

/**
 * Boosted minecarts (batch 42, docs/features/booster-rails.md). A booster rail starts a boost; for
 * {@link BoosterRailBlockEntity#BOOST_TICKS} ticks after, the cart is held at full speed on the rails (the minecart's own
 * speed limit still applies), up slopes too, and trails smoke and flame. Boosts are not saved: one running when the
 * server stops just ends.
 */
public final class BoosterRails {
	/** Faster than any minecart's limit; the cart's own physics clamp it to its maximum. */
	public static final double FULL_SPEED = 1.0;

	private record Boost(ServerLevel level, int ends) {
	}

	private static final Map<UUID, Boost> BOOSTS = new HashMap<>();

	private BoosterRails() {
	}

	/** Whether {@code cart} is boosted right now. */
	public static boolean boosted(AbstractMinecart cart) {
		Boost boost = BOOSTS.get(cart.getUUID());
		return boost != null && boost.ends() > cart.tickCount;
	}

	/** Starts (or restarts) a boost on {@code cart} heading along {@code direction} (a unit vector on the level). */
	static void boost(ServerLevel level, AbstractMinecart cart, Vec3 direction) {
		BOOSTS.put(cart.getUUID(), new Boost(level, cart.tickCount + BoosterRailBlockEntity.BOOST_TICKS));
		push(cart, direction);
	}

	private static void push(AbstractMinecart cart, Vec3 direction) {
		Vec3 velocity = cart.getDeltaMovement();
		cart.setDeltaMovement(direction.x * FULL_SPEED, velocity.y, direction.z * FULL_SPEED);
	}

	static void tick(MinecraftServer server) {
		if (BOOSTS.isEmpty()) {
			return;
		}
		for (Iterator<Map.Entry<UUID, Boost>> it = BOOSTS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Boost> entry = it.next();
			Boost boost = entry.getValue();
			Entity entity = boost.level().getEntity(entry.getKey());
			if (!(entity instanceof AbstractMinecart cart) || cart.isRemoved() || cart.tickCount >= boost.ends()) {
				it.remove();
				continue;
			}
			Vec3 velocity = cart.getDeltaMovement();
			Vec3 flat = new Vec3(velocity.x, 0, velocity.z);
			if (flat.lengthSqr() < 1.0E-6) {
				continue;
			}
			// Keep it at full speed the way it is going, as long as it is on the rails.
			if (cart.isOnRails()) {
				push(cart, flat.normalize());
			}
			if (cart.tickCount % 2 == 0) {
				Vec3 back = cart.position().subtract(flat.normalize().scale(0.8));
				boost.level().sendParticles(ParticleTypes.FLAME, back.x, back.y + 0.3, back.z, 1, 0.05, 0.05, 0.05, 0.0);
				boost.level().sendParticles(ParticleTypes.SMOKE, back.x, back.y + 0.4, back.z, 2, 0.1, 0.1, 0.1, 0.01);
			}
		}
	}

	static void clear() {
		BOOSTS.clear();
	}
}
