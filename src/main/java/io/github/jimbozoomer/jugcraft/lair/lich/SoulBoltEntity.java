package io.github.jimbozoomer.jugcraft.lair.lich;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Soul Bolt (docs/features/crypt-lich.md): a skull of soul-fire the Crypt Lich looses from his hand. It drifts at his
 * foe at {@value #SPEED} blocks a tick, turning a little toward them, and strikes for {@value CryptLichEntity#BOLT_DAMAGE}
 * damage and Wither for {@value CryptLichEntity#WITHER_TICKS} ticks. Struck back (any blow or arrow) it turns and flies at
 * him, faster: warded, it staggers him; exposed, it hurts him ({@link CryptLichEntity#boltReturned}). It bursts on
 * anything solid and fizzles after {@value #LIFE} ticks; it goes with him, and is never saved.
 */
public class SoulBoltEntity extends Entity implements GeoEntity {
	public static final double SPEED = 0.3;
	public static final double REFLECTED_SPEED = 0.6;
	public static final double TURN = 0.08;
	public static final int LIFE = 160;
	/** Where it leaves his hand: ahead of his feet and over them (tools/crypt_lich.py BOLT). */
	public static final double HAND_AHEAD = 1.3125;
	public static final double HAND_UP = 1.71875;
	private static final RawAnimation DRIFT = RawAnimation.begin().thenLoop("animation.soul_bolt.drift");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private @Nullable UUID target;
	private @Nullable UUID reflector;
	private Vec3 velocity = Vec3.ZERO;
	private int life;

	public SoulBoltEntity(EntityType<? extends SoulBoltEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code lich} looses a bolt from his hand at {@code target}. */
	public static SoulBoltEntity loose(ServerLevel level, CryptLichEntity lich, Entity target) {
		SoulBoltEntity bolt = new SoulBoltEntity(JugcraftLich.SOUL_BOLT, level);
		bolt.owner = lich.getUUID();
		bolt.target = target.getUUID();
		Vec3 from = lich.hand();
		bolt.velocity = target.getBoundingBox().getCenter().subtract(from).normalize().scale(SPEED);
		bolt.snapTo(from.x, from.y, from.z, 0.0F, 0.0F);
		bolt.faceAlong();
		level.addFreshEntity(bolt);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.0F, 0.6F);
		return bolt;
	}

	public boolean reflected() {
		return reflector != null;
	}

	public Vec3 velocity() {
		return velocity;
	}

	private @Nullable CryptLichEntity lich(ServerLevel level) {
		return owner != null && level.getEntity(owner) instanceof CryptLichEntity found && found.isAlive() ? found : null;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		CryptLichEntity lich = lich(level);
		if (lich == null || ++life > LIFE) {
			burst(level);
			return;
		}
		Entity goal = reflected() ? lich : target == null ? null : level.getEntity(target);
		double speed = reflected() ? REFLECTED_SPEED : SPEED;
		if (goal != null && goal.isAlive()) {
			Vec3 want = goal.getBoundingBox().getCenter().subtract(position()).normalize().scale(speed);
			double turn = reflected() ? 0.3 : TURN;
			velocity = velocity.add(want.subtract(velocity).scale(turn)).normalize().scale(speed);
		}
		Vec3 next = position().add(velocity);
		LivingEntity hit = hitAlong(level, lich);
		if (hit != null) {
			strike(level, lich, hit);
			return;
		}
		if (!level.noCollision(this, getBoundingBox().move(velocity))) {
			burst(level);
			return;
		}
		setPos(next.x, next.y, next.z);
		faceAlong();
		if (tickCount % 2 == 0) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.25, getZ(), 1, 0.06, 0.06, 0.06, 0.0);
		}
	}

	/** Turns it to face the way it flies (its skull's front leads). */
	private void faceAlong() {
		if (velocity.x * velocity.x + velocity.z * velocity.z > 1.0E-6) {
			setYRot((float) (Math.toDegrees(Math.atan2(velocity.z, velocity.x)) - 90.0));
		}
	}

	/** What it strikes on its way: a player who may fight him, or (struck back) the Lich himself. */
	private @Nullable LivingEntity hitAlong(ServerLevel level, CryptLichEntity lich) {
		AABB sweep = getBoundingBox().expandTowards(velocity).inflate(0.3);
		if (reflected()) {
			return sweep.intersects(lich.getBoundingBox()) ? lich : null;
		}
		for (Player player : level.getEntitiesOfClass(Player.class, sweep, Player::isAlive)) {
			if (LairBosses.eligible(player)) {
				return player;
			}
		}
		return null;
	}

	private void strike(ServerLevel level, CryptLichEntity lich, LivingEntity hit) {
		if (reflected()) {
			lich.boltReturned(level, this, reflector == null ? null : level.getPlayerByUUID(reflector));
		} else if (hit.hurtServer(level, damageSources().mobProjectile(this, lich), CryptLichEntity.damage(CryptLichEntity.BOLT_DAMAGE))) {
			hit.addEffect(new MobEffectInstance(MobEffects.WITHER, CryptLichEntity.WITHER_TICKS, 0), lich);
		}
		burst(level);
	}

	private void burst(ServerLevel level) {
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.25, getZ(), 12, 0.2, 0.2, 0.2, 0.04);
		level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.25, getZ(), 4, 0.15, 0.15, 0.15, 0.02);
		discard();
	}

	/** Struck by a player (a blow, or an arrow they loosed): it turns and flies back at the Lich. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (reflected() || !(source.getEntity() instanceof ServerPlayer player)) {
			return false;
		}
		reflect(level, player);
		return true;
	}

	/** {@code player} strikes it back: it turns on the Lich, faster. */
	public void reflect(ServerLevel level, Player player) {
		reflector = player.getUUID();
		CryptLichEntity lich = lich(level);
		Vec3 back = lich != null ? lich.getBoundingBox().getCenter().subtract(position()) : player.getLookAngle();
		velocity = back.normalize().scale(REFLECTED_SPEED);
		life = 0;
		faceAlong();
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 0.6F);
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

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<SoulBoltEntity>("main", 0, test -> test.setAndContinue(DRIFT)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
