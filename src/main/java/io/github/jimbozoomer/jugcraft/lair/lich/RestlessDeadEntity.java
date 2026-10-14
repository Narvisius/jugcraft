package io.github.jimbozoomer.jugcraft.lair.lich;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
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
 * A Restless Dead (docs/features/crypt-lich.md): a skeleton a little shorter than a man, hunched in a torn grave-shroud,
 * its eyes cold soul-fire, that the Crypt Lich raises from his archive's floor graves, or that claws its way out of an
 * alcove's sarcophagus when a dark name's seal is used. It climbs out for {@value #CLIMB} ticks, unhurt, then shambles
 * at the nearest player. Whoever hurts it takes part in the Lich's fight. It crumbles when he falls, resets or is gone,
 * drops nothing, gives no experience, and is never saved.
 */
public class RestlessDeadEntity extends Monster implements GeoEntity {
	public static final float HEALTH = 20.0F;
	public static final float DAMAGE = 4.0F;
	public static final double SPEED = 0.24;
	public static final double ARMOR = 2.0;
	public static final int CLIMB = 20;

	private static final EntityDataAccessor<Boolean> CLIMBING = SynchedEntityData.defineId(RestlessDeadEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation CLIMB_OUT = RawAnimation.begin().thenPlayAndHold("animation.restless_dead.climb");
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.restless_dead.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.restless_dead.walk");
	private static final RawAnimation ATTACK = RawAnimation.begin().then("animation.restless_dead.attack", LoopType.PLAY_ONCE);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID lich;
	private int lonely;

	public RestlessDeadEntity(EntityType<? extends RestlessDeadEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ATTACK_DAMAGE, DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, SPEED).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ARMOR, ARMOR);
	}

	/** A Restless Dead climbs out at {@code spot}, in {@code lich}'s service. */
	public static RestlessDeadEntity rise(ServerLevel level, CryptLichEntity lich, Vec3 spot) {
		RestlessDeadEntity risen = new RestlessDeadEntity(JugcraftLich.RESTLESS_DEAD, level);
		risen.lich = lich.getUUID();
		risen.snapTo(spot.x, spot.y, spot.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		risen.entityData.set(CLIMBING, true);
		risen.setNoAi(true);
		level.addFreshEntity(risen);
		level.sendParticles(ParticleTypes.SOUL, spot.x, spot.y + 0.2, spot.z, 10, 0.3, 0.2, 0.3, 0.02);
		level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.BONE_BLOCK_BREAK, SoundSource.HOSTILE, 1.2F, 0.6F);
		return risen;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	public boolean climbing() {
		return entityData.get(CLIMBING);
	}

	/** The Lich it serves, if he is still about. */
	public @Nullable CryptLichEntity lich(ServerLevel level) {
		return lich != null && level.getEntity(lich) instanceof CryptLichEntity found && found.isAlive() ? found : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (climbing() && tickCount >= CLIMB) {
			entityData.set(CLIMBING, false);
			setNoAi(false);
		}
		if (tickCount % 20 == 0) {
			CryptLichEntity master = lich(level);
			boolean serving = master != null && master.phase() != CryptLichEntity.Phase.WAITING;
			lonely = serving ? 0 : lonely + 20;
			if (lonely > 40) {
				crumble(level);
				return;
			}
		}
		if (tickCount % 10 == 0) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getEyeY(), getZ(), 1, 0.15, 0.05, 0.15, 0.0);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			triggerAnim("main", "attack");
		}
		return hit;
	}

	/** Falls to bones and dust. */
	public void crumble(ServerLevel level) {
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 16, 0.3, 0.6, 0.3, 0.02);
		level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.8, getZ(), 6, 0.3, 0.6, 0.3, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SKELETON_DEATH, SoundSource.HOSTILE, 0.8F, 1.2F);
		discard();
	}

	/** Unhurt while it climbs out; whoever hurts it takes part in the Lich's fight. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (climbing()) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, amount);
		CryptLichEntity master = lich(level);
		if (hurt && master != null && source.getEntity() instanceof ServerPlayer player) {
			master.took(player, amount);
		}
		return hurt;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SKELETON_DEATH;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CLIMBING, false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (lich != null) {
			output.store("lich", UUIDUtil.CODEC, lich);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		lich = input.read("lich", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		AnimationController<RestlessDeadEntity> main = new AnimationController<RestlessDeadEntity>("main", 4,
				test -> test.setAndContinue(test.animatable().climbing() ? CLIMB_OUT : test.isMoving() ? WALK : IDLE));
		main.triggerableAnim("attack", ATTACK);
		controllers.add(main);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
