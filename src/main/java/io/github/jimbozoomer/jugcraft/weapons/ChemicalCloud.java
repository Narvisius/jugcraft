package io.github.jimbozoomer.jugcraft.weapons;

import io.github.jimbozoomer.jugcraft.building.GasCurtainBlock;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What a chlorine, smoke or thermite grenade leaves behind (batch 31): an invisible point that works on the living
 * things around it for a few seconds, drawn with particles, then goes. It never touches a block or a non-living entity,
 * and is not saved with the world (a cloud in an unloading chunk just ends).
 * <ul>
 * <li>Chlorine hurts everything that needs air (not fish, axolotls or the undead) once a second, through armor; a gas
 * mask or a sealed scuba set keeps it out.</li>
 * <li>Smoke hides anyone inside from mobs (they lose their target) and blinds players inside without a mask.</li>
 * <li>Thermite burns on the floor: it hurts and sets alight whatever stands in it, once a second, through armor.</li>
 * </ul>
 * Chlorine and smoke do not reach a target behind a hanging Gas Curtain (batch 59, {@link GasCurtainBlock#shields}).
 */
public class ChemicalCloud extends Entity {
	public enum Kind {
		CHLORINE(FieldChemistry.CHLORINE_RADIUS, FieldChemistry.CHLORINE_TICKS),
		SMOKE(FieldChemistry.SMOKE_RADIUS, FieldChemistry.SMOKE_TICKS),
		THERMITE(FieldChemistry.THERMITE_RADIUS, FieldChemistry.THERMITE_TICKS);

		final double radius;
		final int ticks;

		Kind(double radius, int ticks) {
			this.radius = radius;
			this.ticks = ticks;
		}
	}

	/** Ticks between pulses of damage, wear on masks and blinding. */
	public static final int PULSE = 20;
	/** Ticks between smoke's sweeps for mobs hunting someone inside. */
	private static final int SMOKE_SWEEP = 5;
	/** How far smoke reaches out to mobs whose target is inside it. */
	private static final double SMOKE_SWEEP_RANGE = 32.0;
	/** Thermite only reaches what stands within this height of the pool. */
	private static final double THERMITE_HEIGHT = 1.5;

	private Kind kind = Kind.SMOKE;
	private @Nullable UUID ownerId;
	private int age;

	public ChemicalCloud(EntityType<? extends ChemicalCloud> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	/** Leaves a cloud of {@code kind} at {@code center}, credited to {@code owner}. */
	public static ChemicalCloud spawn(ServerLevel level, Vec3 center, Kind kind, @Nullable Entity owner) {
		ChemicalCloud cloud = new ChemicalCloud(FieldChemistry.CHEMICAL_CLOUD, level);
		cloud.kind = kind;
		cloud.ownerId = owner == null ? null : owner.getUUID();
		cloud.snapTo(center.x, center.y, center.z, 0.0F, 0.0F);
		level.addFreshEntity(cloud);
		level.playSound(null, center.x, center.y, center.z, kind == Kind.THERMITE ? SoundEvents.FIRECHARGE_USE
				: SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.2F, kind == Kind.THERMITE ? 0.6F : 0.8F);
		cloud.pulse(level);
		return cloud;
	}

	public Kind kind() {
		return kind;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		age++;
		if (age >= kind.ticks) {
			discard();
			return;
		}
		if (age % SMOKE_SWEEP == 0) {
			particles(level);
			if (kind == Kind.SMOKE) {
				hide(level);
			}
		}
		if (age % PULSE == 0) {
			pulse(level);
		}
	}

	/** Whether {@code target} is in the cloud: within its radius, and for thermite standing in the pool. */
	public boolean contains(Entity target) {
		Vec3 center = position();
		if (kind == Kind.THERMITE) {
			double dx = target.getX() - center.x;
			double dz = target.getZ() - center.z;
			double dy = target.getY() - center.y;
			return dx * dx + dz * dz <= kind.radius * kind.radius && dy > -0.5 && dy < THERMITE_HEIGHT;
		}
		Vec3 middle = target.getBoundingBox().getCenter();
		return middle.distanceToSqr(center) <= kind.radius * kind.radius;
	}

	/** Once a second: chlorine and thermite hurt, smoke blinds, and masks wear. */
	void pulse(ServerLevel level) {
		@Nullable Entity owner = ownerId == null ? null : level.getEntity(ownerId);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(kind.radius + 1.0),
				target -> target.isAlive() && !target.isSpectator() && target.getType() != EntityTypes.ARMOR_STAND
						&& contains(target))) {
			if (!FieldChemistry.mayAffect(owner, target)) {
				continue;
			}
			// A hanging gas curtain between the cloud and the target keeps chlorine and smoke out (not thermite's fire).
			if (kind != Kind.THERMITE && GasCurtainBlock.shields(level, position(), target.getEyePosition())) {
				continue;
			}
			switch (kind) {
				case CHLORINE -> {
					if (!target.canBreatheUnderwater() && !FieldChemistry.breathesFiltered(target, true)) {
						DamageSource source = FieldChemistry.damage(level, FieldChemistry.CHLORINE_DAMAGE_TYPE, this, owner);
						target.hurtServer(level, source, FieldChemistry.CHLORINE_DAMAGE);
					}
				}
				case SMOKE -> {
					if (target instanceof Player player && !FieldChemistry.breathesFiltered(player, true)) {
						player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, PULSE + 20, 0, false, false));
					}
				}
				case THERMITE -> {
					DamageSource source = FieldChemistry.damage(level, FieldChemistry.THERMITE_DAMAGE_TYPE, this, owner);
					if (target.hurtServer(level, source, FieldChemistry.THERMITE_DAMAGE)) {
						target.igniteForSeconds(FieldChemistry.THERMITE_FIRE_SECONDS);
					}
				}
			}
		}
	}

	/** Smoke: mobs inside forget their target, and so do mobs outside hunting someone inside. */
	private void hide(ServerLevel level) {
		for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(SMOKE_SWEEP_RANGE), Mob::isAlive)) {
			LivingEntity target = mob.getTarget();
			if (target != null && (contains(mob) || contains(target))) {
				mob.setTarget(null);
			}
		}
	}

	private void particles(ServerLevel level) {
		double r = kind.radius;
		switch (kind) {
			case CHLORINE -> {
				send(level, ParticleTypes.SNEEZE, 60, r * 0.6, r * 0.4, r * 0.6, 0.01);
				send(level, ParticleTypes.SPORE_BLOSSOM_AIR, 10, r * 0.6, r * 0.3, r * 0.6, 0.0);
			}
			case SMOKE -> {
				send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, 12, r * 0.5, r * 0.3, r * 0.5, 0.005);
				send(level, ParticleTypes.CLOUD, 30, r * 0.6, r * 0.4, r * 0.6, 0.01);
			}
			case THERMITE -> {
				send(level, ParticleTypes.LAVA, 6, r * 0.5, 0.05, r * 0.5, 0.0);
				send(level, ParticleTypes.FLAME, 24, r * 0.5, 0.1, r * 0.5, 0.01);
				send(level, ParticleTypes.LARGE_SMOKE, 6, r * 0.4, 0.2, r * 0.4, 0.01);
			}
		}
	}

	private void send(ServerLevel level, SimpleParticleType type, int count, double dx, double dy, double dz, double speed) {
		level.sendParticles(type, getX(), getY() + (kind == Kind.THERMITE ? 0.1 : 0.0), getZ(), count, dx, dy, dz, speed);
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
