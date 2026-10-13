package io.github.jimbozoomer.jugcraft.lair.yeti;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * One of the Yeti King's kin: a young yeti out of the Glacier Hall's dens, {@value #HEALTH} health, whose blows deal
 * {@value #DAMAGE} damage, and quick. It goes for the nearest player. It flees into the snow when he falls, resets or is
 * gone, drops nothing and gives no experience, never freezes, and is never saved.
 */
public class YetiWhelpEntity extends Monster implements GeoEntity {
	public static final float HEALTH = 16.0F;
	public static final float DAMAGE = 3.0F;
	public static final double SPEED = 0.38;

	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.yeti_whelp.walk");
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.yeti_whelp.idle");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID king;

	public YetiWhelpEntity(EntityType<? extends YetiWhelpEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ATTACK_DAMAGE, DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, SPEED).add(Attributes.FOLLOW_RANGE, 40.0);
	}

	/** A whelp climbs out of the den at {@code at}, in {@code king}'s service. */
	public static YetiWhelpEntity climbOut(ServerLevel level, YetiKingEntity king, Vec3 at) {
		YetiWhelpEntity whelp = new YetiWhelpEntity(JugcraftYeti.YETI_WHELP, level);
		whelp.king = king.getUUID();
		whelp.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(whelp);
		level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.6, at.z, 12, 0.4, 0.4, 0.4, 0.05);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.POLAR_BEAR_WARNING, SoundSource.HOSTILE, 1.0F, 1.6F);
		return whelp;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level) || tickCount % 20 != 0) {
			return;
		}
		boolean serving = king != null && level.getEntity(king) instanceof YetiKingEntity found && found.isAlive()
				&& found.phase() != YetiKingEntity.Phase.WAITING;
		if (!serving) {
			flee(level);
		}
	}

	/** Bolts into the snow and is gone. */
	public void flee(ServerLevel level) {
		level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.6, getZ(), 16, 0.4, 0.5, 0.4, 0.05);
		level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.4, getZ(), 6, 0.3, 0.3, 0.3, 0.01);
		discard();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof ServerPlayer player && king != null && level.getEntity(king) instanceof YetiKingEntity found) {
			found.took(player, amount);
		}
		return hurt;
	}

	@Override
	public boolean canFreeze() {
		return false;
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
	protected SoundEvent getAmbientSound() {
		return SoundEvents.POLAR_BEAR_AMBIENT_BABY;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.POLAR_BEAR_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.POLAR_BEAR_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 1.4F;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (king != null) {
			output.store("king", UUIDUtil.CODEC, king);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		king = input.read("king", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<YetiWhelpEntity>("main", 2, test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
