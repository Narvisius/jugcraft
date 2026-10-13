package io.github.jimbozoomer.jugcraft.lair.vesperine;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A soul of the harvest, in Death's Harvest: it rises from the black wheat and streams to Vesperine; reaching her, it
 * heals her ({@link VesperineEntity#SOUL_HEAL} of her health). One blow strikes it down, and it burns up passing within
 * {@value VesperineEntity#WARD_REACH} blocks of a lit ward brazier. Drawn as its own particles, and never saved.
 */
public class HarvestSoulEntity extends Entity {
	public static final double SPEED = 0.3;
	public static final int LIFE = 240;

	private @Nullable UUID boss;

	public HarvestSoulEntity(EntityType<? extends HarvestSoulEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** A soul rises at {@code from}, bound for {@code boss}. */
	public static HarvestSoulEntity release(ServerLevel level, VesperineEntity boss, Vec3 from) {
		HarvestSoulEntity soul = new HarvestSoulEntity(JugcraftVesperine.HARVEST_SOUL, level);
		soul.boss = boss.getUUID();
		soul.snapTo(from.x, from.y, from.z, 0.0F, 0.0F);
		level.addFreshEntity(soul);
		level.sendParticles(ParticleTypes.SOUL, from.x, from.y, from.z, 4, 0.2, 0.2, 0.2, 0.02);
		return soul;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		VesperineEntity vesperine = boss != null && level.getEntity(boss) instanceof VesperineEntity found && found.isAlive() ? found : null;
		if (vesperine == null || tickCount > LIFE) {
			fade(level);
			return;
		}
		for (BlockPos ward : vesperine.wards(level)) {
			if (VesperineEntity.lit(level, ward) && position().distanceTo(Vec3.atCenterOf(ward)) <= VesperineEntity.WARD_REACH) {
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.05);
				fade(level);
				return;
			}
		}
		Vec3 to = vesperine.position().add(0.0, 1.5, 0.0).subtract(position());
		if (to.length() <= 1.5) {
			vesperine.soulArrived(this);
			discard();
			return;
		}
		Vec3 step = to.normalize().scale(SPEED);
		setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
		level.sendParticles(ParticleTypes.SOUL, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
		if (tickCount % 3 == 0) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
		}
	}

	/** It fades away (struck down, burnt up, or the harvest is over). */
	public void fade(ServerLevel level) {
		level.sendParticles(ParticleTypes.SOUL, getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.03);
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (!(source.getEntity() instanceof ServerPlayer player)) {
			return false;
		}
		if (boss != null && level.getEntity(boss) instanceof VesperineEntity vesperine) {
			vesperine.took(player, 0.5F);
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 1.6F);
		fade(level);
		return true;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public boolean shouldBeSaved() {
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
