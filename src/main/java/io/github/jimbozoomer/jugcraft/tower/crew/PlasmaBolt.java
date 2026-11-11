package io.github.jimbozoomer.jugcraft.tower.crew;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A plasma rifle's bolt ({@link PlasmaRifle}): flies straight and fast (no gravity) for {@value PlasmaRifle#LIFE} ticks
 * at most, and stops at the first thing it hits: a creature takes {@value PlasmaRifle#DAMAGE}, a block nothing (no fire,
 * no damage). The crew's bolts pass the crew (a player's hit them), and a crew member's bolt passes players other than
 * the one it was fired at; whatever it kills that has meat drops it cooked ({@link PlasmaWounds#cook}). Its glow,
 * trail and muzzle flash are the client's own (client/PlasmaBoltRenderer and {@link #tick}); the server sends the hit's
 * burst and sound.
 */
public class PlasmaBolt extends ThrowableProjectile {
	/** Plasma in air (the bolt sheet, take 1 A): white-hot, the nitrogen's blue-violet glow, the embers it cools to. */
	private static final int CORE = 0xF4EEFF, GLOW = 0x766EFF, EMBER = 0xFF7238;
	private @Nullable UUID aimedAt;
	/** Client: where it was first seen (the rifle's muzzle, near enough), for how far its wake reaches back. */
	private @Nullable Vec3 origin;
	/** What it does to what it hits (a rifle's {@link PlasmaRifle#DAMAGE}; a drop ship's guns more: CrewShip.GUN_DAMAGE). */
	float damage = PlasmaRifle.DAMAGE;

	public PlasmaBolt(EntityType<? extends PlasmaBolt> type, Level level) {
		super(type, level);
	}

	public PlasmaBolt(Level level, LivingEntity shooter, Vec3 from, @Nullable Entity target) {
		super(PlasmaRifle.BOLT, from.x, from.y, from.z, level);
		setOwner(shooter);
		aimedAt = target == null ? null : target.getUUID();
	}

	/** A bolt from one of a crew drop ship's guns, at {@code target} (it may hit a player only if fired at them). */
	public PlasmaBolt(Level level, CrewShip ship, Vec3 from, @Nullable Entity target) {
		super(PlasmaRifle.BOLT, from.x, from.y, from.z, level);
		setOwner(ship);
		aimedAt = target == null ? null : target.getUUID();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	protected float getAirDrag() {
		return 1.0F;
	}

	@Override
	public void tick() {
		super.tick();
		Level level = level();
		if (level.isClientSide()) {
			Vec3 v = getDeltaMovement();
			if (origin == null) {
				origin = position().subtract(v);
			}
			if (tickCount == 1) {
				// the muzzle flash where it left the rifle
				Vec3 back = position().subtract(v);
				level.addParticle(ColorParticleOption.create(ParticleTypes.FLASH, 0xFF766EFF), back.x, back.y, back.z, 0, 0, 0);
			}
			// the glowing wake is drawn with it (client/PlasmaBoltRenderer); along it, a few motes of the cooling air: blue-violet
			// where it's just been, embers further back
			for (int i = 0; i < 3; i++) {
				double k = random.nextDouble();
				boolean ember = i > 0;
				level.addParticle(new DustParticleOptions(ember ? EMBER : GLOW, ember ? 0.35F : 0.5F), getX() - v.x * (k + (ember ? 0.6 : 0)) + random.nextGaussian() * 0.04,
						getY() + 0.125 - v.y * (k + (ember ? 0.6 : 0)) + random.nextGaussian() * 0.04, getZ() - v.z * (k + (ember ? 0.6 : 0)) + random.nextGaussian() * 0.04,
						0, 0, 0);
			}
		} else if (tickCount > PlasmaRifle.LIFE) {
			discard();
		}
	}

	/** Client: how far it has come from where it was first seen (blocks; 0 not yet seen). */
	public double flown(float partialTick) {
		if (origin == null) {
			return 0;
		}
		return new Vec3(net.minecraft.util.Mth.lerp(partialTick, xo, getX()), net.minecraft.util.Mth.lerp(partialTick, yo, getY()),
				net.minecraft.util.Mth.lerp(partialTick, zo, getZ())).distanceTo(origin);
	}

	@Override
	protected boolean canHitEntity(Entity e) {
		if (!super.canHitEntity(e)) {
			return false;
		}
		// the crew's own bolts (theirs and their drop ships') pass the crew: no friendly fire; anyone else's hit them
		// (Justin, 8 November 2026: "they must be hittable and killable")
		if (e instanceof TowerCrew && (getOwner() instanceof TowerCrew || getOwner() instanceof CrewShip)) {
			return false;
		}
		// a drop squad's soldiers (tower/squad/Legionary) and the crew are on the same side: their bolts pass each other,
		// a soldier's pass tamed animals and players it wasn't fired at
		boolean legion = getOwner() instanceof io.github.jimbozoomer.jugcraft.tower.squad.Legionary;
		if (e instanceof io.github.jimbozoomer.jugcraft.tower.squad.Legionary && (legion || getOwner() instanceof TowerCrew || getOwner() instanceof CrewShip)
				|| legion && (e instanceof TowerCrew || e instanceof CrewShip || e instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame()
						|| e instanceof Player && !e.getUUID().equals(aimedAt))) {
			return false;
		}
		// a drop ship's bolt passes tamed animals, the ship itself, and players it wasn't fired at
		if (getOwner() instanceof CrewShip) {
			return !(e instanceof CrewShip || e instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame()
					|| e instanceof Player && !e.getUUID().equals(aimedAt));
		}
		// a crew member's bolt passes players it wasn't fired at
		return !(getOwner() instanceof TowerCrew && e instanceof Player && !e.getUUID().equals(aimedAt));
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (level() instanceof ServerLevel level) {
			Entity owner = getOwner();
			DamageSource source = owner instanceof LivingEntity shooter ? damageSources().mobProjectile(this, shooter)
					: damageSources().thrown(this, owner);
			if (owner instanceof CrewShip && hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity l) {
				l.setInvulnerableTime(0);             // a drop ship's guns fire faster than a mob's moment of grace after a hit
			}
			hit.getEntity().hurtServer(level, source, damage);
			if (hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
				// its tunnel burned right through along the shot; and what that does to it (PlasmaWounds.strike)
				PlasmaWounds.strike(level, living, source, PlasmaWounds.wound(living, hit.getLocation(), getDeltaMovement(), false, level.getRandom()));
			} else {
				// a part of a creature (a Murmur's head is an entity of its own): the tunnel through that
				PlasmaWounds.hitPart(level, hit.getEntity(), source, hit.getLocation(), getDeltaMovement(), PlasmaWounds.BOLT_RADIUS);
			}
		}
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel level && !isRemoved()) {
			Vec3 at = hit.getLocation();
			// the hit: a flash and a burst of purple sparks, a sizzling pop
			level.sendParticles(new DustParticleOptions(GLOW, 1.1F), at.x, at.y, at.z, 14, 0.15, 0.15, 0.15, 0.02);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 8, 0.12, 0.12, 0.12, 0.25);
			level.playSound(null, at.x, at.y, at.z, PlasmaRifle.HIT, SoundSource.HOSTILE, 1.0F, 0.9F + random.nextFloat() * 0.2F);
			discard();
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
	}
}
