package io.github.jimbozoomer.jugcraft.lair.yeti;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A block of ice the Yeti King tears out of his lake and hurls: it arcs over to where his foe stood and shatters where it
 * lands (or on the first player it strikes), {@value YetiKingEntity#BOULDER_DAMAGE} damage to every player within
 * {@value YetiKingEntity#BOULDER_RADIUS} blocks. It cannot be stopped or harmed, lasts at most {@value #LIFE} ticks, and is
 * never saved.
 */
public class HurledBoulderEntity extends Entity implements GeoEntity {
	public static final double SPEED = 0.9;
	public static final int LIFE = 80;
	public static final double GRAVITY = 0.04;
	/** How high over his feet the block leaves his hands: where he holds it over his head (tools/yeti_king_models.py). */
	public static final double HELD = 4.5;
	private static final RawAnimation TUMBLE = RawAnimation.begin().thenLoop("animation.hurled_boulder.tumble");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private Vec3 velocity = Vec3.ZERO;
	private double floor;

	public HurledBoulderEntity(EntityType<? extends HurledBoulderEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code king} hurls a block of ice from over his head at where {@code target} stands, in an arc. */
	public static HurledBoulderEntity hurl(ServerLevel level, YetiKingEntity king, Entity target) {
		HurledBoulderEntity boulder = new HurledBoulderEntity(JugcraftYeti.HURLED_BOULDER, level);
		boulder.owner = king.getUUID();
		boulder.floor = king.floorY();
		Vec3 from = king.position().add(king.facing().scale(0.6)).add(0.0, HELD, 0.0);
		Vec3 to = target.position();
		double dx = to.x - from.x;
		double dz = to.z - from.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		double ticks = Math.max(8.0, flat / SPEED);
		boulder.velocity = new Vec3(dx / ticks, (to.y - from.y + 0.5 * GRAVITY * ticks * ticks) / ticks, dz / ticks);
		boulder.snapTo(from.x, from.y, from.z, king.getYRot(), 0.0F);
		level.addFreshEntity(boulder);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 2.0F, 0.4F);
		return boulder;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		YetiKingEntity king = owner != null && level.getEntity(owner) instanceof YetiKingEntity found && found.isAlive() ? found : null;
		if (king == null || tickCount > LIFE) {
			discard();
			return;
		}
		velocity = velocity.add(0.0, -GRAVITY, 0.0);
		Vec3 next = position().add(velocity);
		AABB sweep = getBoundingBox().expandTowards(velocity).inflate(0.2);
		boolean struck = !level.getEntitiesOfClass(ServerPlayer.class, sweep, LairBosses::eligible).isEmpty();
		if (struck || (next.y <= floor && velocity.y < 0.0)) {
			setPos(next.x, Math.max(floor, next.y), next.z);
			shatter(level, king);
			return;
		}
		setPos(next.x, next.y, next.z);
		if (tickCount % 2 == 0) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.5, getZ(), 1, 0.2, 0.2, 0.2, 0.0);
		}
	}

	/** It shatters: {@value YetiKingEntity#BOULDER_DAMAGE} damage to every player within its radius. */
	public int shatter(ServerLevel level, YetiKingEntity king) {
		int hit = 0;
		for (ServerPlayer player : king.nearby(level)) {
			if (player.position().distanceTo(position()) <= YetiKingEntity.BOULDER_RADIUS
					&& player.hurtServer(level, damageSources().mobProjectile(this, king), YetiKingEntity.damage(YetiKingEntity.BOULDER_DAMAGE))) {
				hit++;
			}
		}
		level.sendParticles(ParticleTypes.ITEM_SNOWBALL, getX(), getY() + 0.4, getZ(), 30, 1.0, 0.4, 1.0, 0.2);
		level.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.4, getZ(), 20, 1.2, 0.4, 1.2, 0.05);
		level.playSound(null, getX(), getY(), getZ(), SoundType.GLASS.getBreakSound(), SoundSource.HOSTILE, 2.0F, 0.6F);
		discard();
		return hit;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
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
		controllers.add(new AnimationController<HurledBoulderEntity>("main", 0, test -> test.setAndContinue(TUMBLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
