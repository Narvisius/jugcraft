package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A burning jack o'lantern thrown by the {@link HeadlessHorseman}. Where it strikes it bursts: every living thing
 * but the Horseman within {@value #SPLASH_RADIUS} blocks takes {@value #DAMAGE} damage and catches fire for
 * {@value #FIRE_SECONDS} seconds. It never breaks or lights blocks. One still flying after {@value #MAX_FLIGHT}
 * ticks burns out.
 */
public class FlamingPumpkin extends ThrowableItemProjectile {
	public static final float DAMAGE = 6.0F;
	public static final double SPLASH_RADIUS = 2.0;
	public static final float FIRE_SECONDS = 3.0F;
	public static final int MAX_FLIGHT = 100;

	public FlamingPumpkin(EntityType<? extends FlamingPumpkin> type, Level level) {
		super(type, level);
	}

	public FlamingPumpkin(ServerLevel level, LivingEntity thrower) {
		super(JugcraftAgriculture.FLAMING_PUMPKIN, thrower, level, new ItemStack(Items.JACK_O_LANTERN));
		setPos(thrower.getX(), thrower.getY() + 2.4, thrower.getZ());
	}

	@Override
	protected Item getDefaultItem() {
		return Items.JACK_O_LANTERN;
	}

	@Override
	public boolean displayFireAnimation() {
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			level().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.2, getZ(), 0.0, 0.02, 0.0);
		} else if (tickCount > MAX_FLIGHT) {
			discard();
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel level && !isRemoved()) {
			burst(level, hit.getLocation());
			discard();
		}
	}

	/** Damages and sets alight everything living around {@code at} but the Horseman; no block is touched. */
	public void burst(ServerLevel level, Vec3 at) {
		Entity owner = getOwner();
		DamageSource source = owner instanceof LivingEntity thrower ? damageSources().mobProjectile(this, thrower) : damageSources().thrown(this, owner);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(SPLASH_RADIUS),
				living -> !(living instanceof HeadlessHorseman) && living.isAlive())) {
			if (victim.hurtServer(level, source, DAMAGE)) {
				victim.igniteForSeconds(FIRE_SECONDS);
			}
		}
		level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.3, at.z, 30, 0.6, 0.4, 0.6, 0.05);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.3, at.z, 8, 0.4, 0.3, 0.4, 0.02);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.6F, 1.4F);
	}
}
