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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
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
 * A spool of Madame Tatterlace's red thread, which she kicks rolling across the doily in a straight line at
 * {@value #SPEED} blocks a tick: {@value TatterlaceEntity#SPOOL_DAMAGE} damage and a heavy knockback to each player within
 * {@value #WIDTH} blocks of its path, once each. It unwinds a trail of thread as it goes, and is gone when it rolls off
 * the lace (where she has unravelled it, or past the rim) or after {@value #LIFE} ticks. It cannot be stopped or harmed,
 * and is never saved.
 */
public class RollingSpoolEntity extends Entity implements GeoEntity {
	public static final double SPEED = 0.5;
	public static final int LIFE = 100;
	public static final double WIDTH = 1.5;
	private static final RawAnimation ROLL = RawAnimation.begin().thenLoop("animation.rolling_spool.roll");
	private static final DustParticleOptions THREAD = new DustParticleOptions(0xA03036, 1.0F);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private Vec3 direction = Vec3.ZERO;
	private double floor;
	private boolean falling;
	private final Set<UUID> struck = new HashSet<>();

	public RollingSpoolEntity(EntityType<? extends RollingSpoolEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code boss} kicks a spool rolling from in front of her along {@code direction} (flat). */
	public static RollingSpoolEntity kick(ServerLevel level, TatterlaceEntity boss, Vec3 direction) {
		RollingSpoolEntity spool = new RollingSpoolEntity(JugcraftTatterlace.ROLLING_SPOOL, level);
		spool.owner = boss.getUUID();
		spool.floor = boss.floorY();
		spool.direction = direction.normalize();
		Vec3 from = boss.position().add(spool.direction.scale(2.0));
		float yaw = (float) (Math.toDegrees(Math.atan2(spool.direction.z, spool.direction.x)) - 90.0);
		spool.snapTo(from.x, spool.floor, from.z, yaw, 0.0F);
		level.addFreshEntity(spool);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.WOOL_HIT, SoundSource.HOSTILE, 2.0F, 0.6F);
		return spool;
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
		Vec3 next = position().add(direction.scale(SPEED));
		if (!falling) {
			BlockPos under = BlockPos.containing(next.x, floor - 0.05, next.z);
			falling = level.getBlockState(under).getCollisionShape(level, under).isEmpty();
		}
		if (falling) {
			next = next.add(0.0, -0.4, 0.0);
		}
		setPos(next.x, next.y, next.z);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(WIDTH, 1.0, WIDTH))) {
			double dx = player.getX() - getX();
			double dz = player.getZ() - getZ();
			if (!LairBosses.eligible(player) || dx * dx + dz * dz > WIDTH * WIDTH || !struck.add(player.getUUID())) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(boss), TatterlaceEntity.damage(TatterlaceEntity.SPOOL_DAMAGE))) {
				Vec3 push = direction.scale(TatterlaceEntity.SPOOL_KNOCKBACK).add(0.0, 0.4, 0.0);
				player.setDeltaMovement(push);
				player.connection.send(new ClientboundSetEntityMotionPacket(player));
			}
		}
		if (!falling) {
			level.sendParticles(THREAD, getX() - direction.x * 0.8, floor + 0.1, getZ() - direction.z * 0.8, 2, 0.15, 0.02, 0.15, 0.0);
			if (tickCount % 6 == 0) {
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_HIT, SoundSource.HOSTILE, 1.0F, 0.8F);
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
		controllers.add(new AnimationController<RollingSpoolEntity>("main", 0, test -> test.setAndContinue(ROLL)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
