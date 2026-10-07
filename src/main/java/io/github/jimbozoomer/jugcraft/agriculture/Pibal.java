package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.SmoothFlight;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A pibal (a pilot balloon, fall addition 29): a small latex balloon let go to see the winds aloft. It rises
 * {@value #RISE} blocks a tick and drifts with the wind at its height ({@link FiestaWinds}), so its path bends where the
 * layers change; after {@value #LIFE} ticks (or above the world, or in a block) it pops. The server moves it; clients
 * ease after it (see {@link SmoothFlight}). It is light enough that nothing is hurt by it, and a blow pops it.
 */
public class Pibal extends Entity {
	public static final double RISE = 0.12;
	public static final int LIFE = 600;

	public Pibal(EntityType<? extends Pibal> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public LinearInterpolationHandler createInterpolationHandler() {
		return SmoothFlight.handler(this);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel server)) {
			SmoothFlight.step(this);
			return;
		}
		Vec3 wind = FiestaWinds.at(server, getY());
		double bob = Math.sin((tickCount + getId() * 5) * 0.15) * 0.01;
		setDeltaMovement(wind.x, RISE + bob, wind.z);
		move(MoverType.SELF, getDeltaMovement());
		if (tickCount >= LIFE || getY() > server.getMaxY() + 8 || verticalCollision) {
			pop(server);
		}
	}

	/** It pops: a snap and a puff, and it is gone. */
	public void pop(ServerLevel level) {
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.8F, 1.8F);
		level.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.01);
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (!isRemoved()) {
			pop(level);
		}
		return true;
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		tickCount = input.getIntOr("age", 0);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("age", tickCount);
	}
}
