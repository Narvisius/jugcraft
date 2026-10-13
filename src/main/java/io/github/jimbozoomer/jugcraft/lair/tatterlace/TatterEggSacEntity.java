package io.github.jimbozoomer.jugcraft.lair.tatterlace;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One of the egg sacs Madame Tatterlace spits round her doily as she takes in her seams: a cluster of silk-white sacs that
 * pulses where it lies. When she calls her Brood, it swells and splits over {@value #HATCH_TICKS} ticks and a spiderling
 * comes out; it can hatch again. Players can break it ({@value #HEALTH} health) to stop her brood. It shrivels when she
 * falls, resets or is gone, drops nothing and gives no experience, and is never saved.
 */
public class TatterEggSacEntity extends Monster implements GeoEntity {
	public static final float HEALTH = 12.0F;
	public static final int HATCH_TICKS = 20;

	private static final EntityDataAccessor<Boolean> HATCHING = SynchedEntityData.defineId(TatterEggSacEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation PULSE = RawAnimation.begin().thenLoop("animation.tatter_egg_sac.pulse");
	private static final RawAnimation HATCH = RawAnimation.begin().thenPlayAndHold("animation.tatter_egg_sac.hatch");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID boss;
	private int hatchAt = -1;

	public TatterEggSacEntity(EntityType<? extends TatterEggSacEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
		setNoGravity(true);
		setNoAi(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.MOVEMENT_SPEED, 0.0);
	}

	/** {@code boss} spits an egg sac to lie at {@code at}. */
	public static TatterEggSacEntity spit(ServerLevel level, TatterlaceEntity boss, Vec3 at) {
		TatterEggSacEntity sac = new TatterEggSacEntity(JugcraftTatterlace.TATTER_EGG_SAC, level);
		sac.boss = boss.getUUID();
		sac.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(sac);
		level.sendParticles(ParticleTypes.ITEM_COBWEB, at.x, at.y + 0.4, at.z, 10, 0.4, 0.3, 0.4, 0.05);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.HOSTILE, 1.0F, 0.6F);
		return sac;
	}

	public boolean hatching() {
		return entityData.get(HATCHING);
	}

	/** Starts it hatching (if it is not already): a spiderling comes out {@value #HATCH_TICKS} ticks from now. */
	public boolean hatch(ServerLevel level) {
		if (hatchAt >= 0) {
			return false;
		}
		hatchAt = tickCount + HATCH_TICKS;
		entityData.set(HATCHING, true);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.HOSTILE, 1.0F, 0.4F);
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		TatterlaceEntity mother = boss != null && level.getEntity(boss) instanceof TatterlaceEntity found && found.isAlive() ? found : null;
		if (tickCount % 20 == 0 && (mother == null || mother.phase() == TatterlaceEntity.Phase.WAITING)) {
			shrivel(level);
			return;
		}
		if (hatchAt >= 0 && tickCount >= hatchAt) {
			hatchAt = -1;
			entityData.set(HATCHING, false);
			if (mother != null) {
				SpiderlingEntity.hatch(level, mother, position().add(0.0, 0.3, 0.0));
			}
			level.sendParticles(ParticleTypes.ITEM_COBWEB, getX(), getY() + 0.5, getZ(), 8, 0.3, 0.3, 0.3, 0.05);
		}
	}

	/** Shrivels away. */
	public void shrivel(ServerLevel level) {
		level.sendParticles(ParticleTypes.ITEM_COBWEB, getX(), getY() + 0.4, getZ(), 10, 0.3, 0.3, 0.3, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.HOSTILE, 0.8F, 1.4F);
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof ServerPlayer player && boss != null && level.getEntity(boss) instanceof TatterlaceEntity mother) {
			mother.took(player, amount);
		}
		return hurt;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SLIME_SQUISH_SMALL;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SLIME_SQUISH_SMALL;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(HATCHING, false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (boss != null) {
			output.store("boss", UUIDUtil.CODEC, boss);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		boss = input.read("boss", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<TatterEggSacEntity>("main", 4,
				test -> test.setAndContinue(test.animatable().hatching() ? HATCH : PULSE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
