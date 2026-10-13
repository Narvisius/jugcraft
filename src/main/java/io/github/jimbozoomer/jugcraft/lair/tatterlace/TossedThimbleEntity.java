package io.github.jimbozoomer.jugcraft.lair.tatterlace;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import java.util.HashSet;
import java.util.Set;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A gold thimble Madame Tatterlace plucks from her back and tosses: it arcs over to where her foe stood, and bounces
 * {@value #BOUNCES} times on the lace, ringing, {@value #DAMAGE} damage to each player it strikes on each flight. Where her
 * lace is gone it falls away. It cannot be stopped or harmed, lasts at most {@value #LIFE} ticks, and is never saved.
 */
public class TossedThimbleEntity extends Entity implements GeoEntity {
	public static final float DAMAGE = 6.0F;
	public static final int BOUNCES = 2;
	public static final double SPEED = 0.7;
	public static final int LIFE = 120;
	private static final double GRAVITY = 0.05;
	private static final RawAnimation TUMBLE = RawAnimation.begin().thenLoop("animation.tossed_thimble.tumble");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private Vec3 velocity = Vec3.ZERO;
	private double floor;
	private int bounces;
	private final Set<UUID> struck = new HashSet<>();

	public TossedThimbleEntity(EntityType<? extends TossedThimbleEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code boss} tosses a thimble from her back at where {@code target} stands, in an arc. */
	public static TossedThimbleEntity toss(ServerLevel level, TatterlaceEntity boss, Entity target) {
		TossedThimbleEntity thimble = new TossedThimbleEntity(JugcraftTatterlace.TOSSED_THIMBLE, level);
		thimble.owner = boss.getUUID();
		thimble.floor = boss.floorY();
		Vec3 from = boss.back();
		Vec3 to = target.position();
		double dx = to.x - from.x;
		double dz = to.z - from.z;
		double flat = Math.sqrt(dx * dx + dz * dz);
		double ticks = Math.max(6.0, flat / SPEED);
		thimble.velocity = new Vec3(dx / ticks, (to.y - from.y + 0.5 * GRAVITY * ticks * ticks) / ticks, dz / ticks);
		thimble.snapTo(from.x, from.y, from.z, boss.getYRot(), 0.0F);
		level.addFreshEntity(thimble);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 0.8F, 1.6F);
		return thimble;
	}

	public int bounces() {
		return bounces;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		TatterlaceEntity boss = owner != null && level.getEntity(owner) instanceof TatterlaceEntity found && found.isAlive() ? found : null;
		if (boss == null || tickCount > LIFE || getY() < floor - 8.0) {
			discard();
			return;
		}
		velocity = velocity.add(0.0, -GRAVITY, 0.0);
		Vec3 next = position().add(velocity);
		strike(level, boss, next);
		if (next.y <= floor && velocity.y < 0.0 && lace(level, next)) {
			if (bounces >= BOUNCES) {
				level.sendParticles(ParticleTypes.CRIT, next.x, floor + 0.1, next.z, 8, 0.2, 0.1, 0.2, 0.05);
				discard();
				return;
			}
			bounces++;
			struck.clear();
			velocity = new Vec3(velocity.x * 0.85, -velocity.y * 0.6, velocity.z * 0.85);
			next = new Vec3(next.x, floor, next.z);
			level.playSound(null, next.x, next.y, next.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 1.2F, 1.2F + 0.2F * bounces);
		}
		setPos(next.x, next.y, next.z);
		if (tickCount % 2 == 0) {
			level.sendParticles(ParticleTypes.WAX_OFF, getX(), getY() + 0.2, getZ(), 1, 0.05, 0.05, 0.05, 0.0);
		}
	}

	/** Whether there is lace under (x, z) at the doily's height: none where she has unravelled it, or past its rim. */
	private boolean lace(ServerLevel level, Vec3 at) {
		return !level.getBlockState(BlockPos.containing(at.x, floor - 0.05, at.z)).getCollisionShape(level, BlockPos.containing(at.x, floor - 0.05, at.z)).isEmpty();
	}

	/** Strikes each player it passes on its way to {@code next}, once a flight; the Golden Thimble turns it aside. */
	private void strike(ServerLevel level, TatterlaceEntity boss, Vec3 next) {
		AABB sweep = getBoundingBox().expandTowards(velocity).inflate(0.3);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, sweep)) {
			if (!LairBosses.eligible(player) || !struck.add(player.getUUID())) {
				continue;
			}
			DamageSource source = damageSources().mobProjectile(this, boss);
			if (!player.hurtServer(level, source, TatterlaceEntity.damage(DAMAGE))) {
				velocity = new Vec3(-velocity.x * 0.5, Math.abs(velocity.y) * 0.5 + 0.1, -velocity.z * 0.5);
			}
		}
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
		controllers.add(new AnimationController<TossedThimbleEntity>("main", 0, test -> test.setAndContinue(TUMBLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
