package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A sky lantern let go: a paper lantern in its colour, glowing, that rises {@value #RISE} blocks a tick (bobbing a
 * little) and drifts with the wind, which every lantern in the world shares and which turns full circle every
 * {@value #WIND_PERIOD} ticks, so lanterns let go together drift together. It burns for {@value #LIFETIME} ticks and up
 * to {@value #LIFETIME_SPREAD} more, dimming over its last {@value #FADE_TICKS}, and is gone when it burns out or rises
 * above the world. It carries its wish (the item's name) as its own name. A blow tears it and puts it out. It lights no
 * blocks (entities can't) and sets nothing alight.
 */
public class SkyLantern extends Entity {
	public static final double RISE = 0.035;
	public static final double WIND = 0.015;
	public static final int WIND_PERIOD = 72000;
	public static final int LIFETIME = 2400;
	public static final int LIFETIME_SPREAD = 600;
	public static final int FADE_TICKS = 100;
	/** A lantern's paper colour when it isn't dyed: a warm red. */
	public static final int DEFAULT_COLOUR = 0xE8642A;
	private static final EntityDataAccessor<Integer> COLOUR = SynchedEntityData.defineId(SkyLantern.class, EntityDataSerializers.INT);
	/** The game time it burns out (sent to clients, which dim it towards then). */
	private static final EntityDataAccessor<Long> BURNS_OUT = SynchedEntityData.defineId(SkyLantern.class, EntityDataSerializers.LONG);

	public SkyLantern(EntityType<? extends SkyLantern> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public SkyLantern(Level level, Vec3 at, int colour) {
		this(JugcraftAgriculture.SKY_LANTERN, level);
		setPos(at);
		entityData.set(COLOUR, colour);
		entityData.set(BURNS_OUT, level.getGameTime() + LIFETIME + level.getRandom().nextInt(LIFETIME_SPREAD + 1));
	}

	public int colour() {
		return entityData.get(COLOUR);
	}

	public long burnsOut() {
		return entityData.get(BURNS_OUT);
	}

	/** How brightly it burns now: 1 until its last {@value #FADE_TICKS} ticks, then down to 0. */
	public float brightness(long gameTime) {
		return (float) Math.max(0.0, Math.min(1.0, (burnsOut() - gameTime) / (double) FADE_TICKS));
	}

	/** The wind at {@code gameTime}: {@value #WIND} blocks a tick, its direction turning full circle every {@value #WIND_PERIOD} ticks. */
	public static Vec3 wind(long gameTime) {
		double angle = Math.floorMod(gameTime, (long) WIND_PERIOD) / (double) WIND_PERIOD * Math.PI * 2.0;
		return new Vec3(Math.cos(angle) * WIND, 0.0, Math.sin(angle) * WIND);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(COLOUR, DEFAULT_COLOUR);
		builder.define(BURNS_OUT, 0L);
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 wind = wind(level().getGameTime());
		double bob = Math.sin((tickCount + getId() * 7) * 0.08) * 0.01;
		setDeltaMovement(wind.x, RISE + bob, wind.z);
		move(MoverType.SELF, getDeltaMovement());
		if (!level().isClientSide() && (level().getGameTime() >= burnsOut() || getY() > level().getMaxY() + 8)) {
			discard();
		}
	}

	/** A blow tears it: it goes out with a hiss and a puff of smoke. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved()) {
			return false;
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.5F, 1.6F);
		level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 6, 0.15, 0.15, 0.15, 0.01);
		discard();
		return true;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		entityData.set(COLOUR, input.getIntOr("colour", DEFAULT_COLOUR));
		entityData.set(BURNS_OUT, input.getLongOr("burns_out", level().getGameTime() + LIFETIME));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("colour", colour());
		output.putLong("burns_out", burnsOut());
	}
}
