package io.github.jimbozoomer.jugcraft.rocketry;

import io.github.jimbozoomer.jugcraft.weapons.Blast;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A rocket fired from the rocket launcher (batch 41, docs/features/rocket-launcher.md). It flies straight, untouched by
 * gravity, and bursts when it hits anything or after {@link #LIFETIME} ticks. The burst is a {@link Blast}: it hurts
 * living things only and never breaks, moves or burns a block. A homing rocket turns towards the hostile mob it was
 * locked on to, a little each tick.
 */
public class CombatRocket extends ThrowableItemProjectile {
	public static final int LIFETIME = 100;
	/** How much of the way to the target's direction a homing rocket turns each tick. */
	private static final double TURN = 0.18;

	private @Nullable UUID target;

	public CombatRocket(EntityType<? extends CombatRocket> type, Level level) {
		super(type, level);
	}

	public CombatRocket(Level level, LivingEntity owner, ItemStack stack) {
		super(JugcraftRocketry.COMBAT_ROCKET, owner, level, stack);
	}

	@Override
	protected Item getDefaultItem() {
		return JugcraftRocketry.HE_ROCKET;
	}

	/** Locks a homing rocket on to {@code mob}. */
	public void lockOn(LivingEntity mob) {
		target = mob.getUUID();
	}

	public boolean homing() {
		return getItem().is(JugcraftRocketry.HOMING_ROCKET);
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	public void tick() {
		super.tick();
		if (isRemoved()) {
			return;
		}
		if (level().isClientSide()) {
			Vec3 back = position().subtract(getDeltaMovement().scale(0.5));
			level().addParticle(ParticleTypes.SMOKE, back.x, back.y, back.z, 0, 0.02, 0);
			level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0, 0);
			return;
		}
		ServerLevel level = (ServerLevel) level();
		if (tickCount >= LIFETIME) {
			burst(level, position());
			return;
		}
		if (target != null && level.getEntity(target) instanceof LivingEntity mob && mob.isAlive()) {
			Vec3 velocity = getDeltaMovement();
			double speed = velocity.length();
			Vec3 wanted = mob.getBoundingBox().getCenter().subtract(position()).normalize();
			Vec3 heading = velocity.normalize();
			Vec3 turned = heading.add(wanted.subtract(heading).scale(TURN)).normalize().scale(speed);
			setDeltaMovement(turned);
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel server && !isRemoved()) {
			Vec3 center = hit.getLocation();
			if (hit instanceof BlockHitResult block) {
				// Just off the face it hit, so the block itself does not shield the blast.
				Direction face = block.getDirection();
				center = center.add(face.getStepX() * 0.3, face.getStepY() * 0.3, face.getStepZ() * 0.3);
			}
			burst(server, center);
		}
	}

	private void burst(ServerLevel level, Vec3 center) {
		Entity owner = getOwner();
		if (homing()) {
			Blast.detonate(level, center, this, owner, RocketLauncherItem.HOMING_RADIUS, RocketLauncherItem.HOMING_DAMAGE);
		} else {
			Blast.detonate(level, center, this, owner, RocketLauncherItem.HE_RADIUS, RocketLauncherItem.HE_DAMAGE);
		}
		discard();
	}
}
