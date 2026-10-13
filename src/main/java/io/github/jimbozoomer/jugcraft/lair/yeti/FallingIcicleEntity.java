package io.github.jimbozoomer.jugcraft.lair.yeti;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An icicle the Yeti King's roar shakes from the vault in his Icicle Fall: it drops from {@value #HEIGHT} blocks over its
 * mark, its shadow on the snow below, and shatters where it lands, {@value #DAMAGE} damage to a player within
 * {@value #REACH} block of it. It cannot be stopped or harmed and is never saved.
 */
public class FallingIcicleEntity extends Entity implements GeoEntity {
	public static final double HEIGHT = 12.0;
	public static final double FALL_SPEED = 0.9;
	public static final double REACH = 1.0;
	public static final float DAMAGE = 6.0F;
	private static final RawAnimation FALL = RawAnimation.begin().thenLoop("animation.falling_icicle.fall");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID owner;
	private Vec3 mark = Vec3.ZERO;

	public FallingIcicleEntity(EntityType<? extends FallingIcicleEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** Drops an icicle from over {@code mark} (on the lake's surface), for {@code king}. */
	public static FallingIcicleEntity drop(ServerLevel level, YetiKingEntity king, Vec3 mark) {
		FallingIcicleEntity icicle = new FallingIcicleEntity(JugcraftYeti.FALLING_ICICLE, level);
		icicle.owner = king.getUUID();
		icicle.mark = mark;
		icicle.snapTo(mark.x, mark.y + HEIGHT, mark.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(icicle);
		return icicle;
	}

	public Vec3 mark() {
		return mark;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		YetiKingEntity king = owner != null && level.getEntity(owner) instanceof YetiKingEntity found && found.isAlive() ? found : null;
		if (king == null || tickCount > 60) {
			discard();
			return;
		}
		double y = getY() - FALL_SPEED;
		if (tickCount % 3 == 0) {
			level.sendParticles(ParticleTypes.SMOKE, mark.x, mark.y + 0.05, mark.z, 2, 0.2, 0.0, 0.2, 0.0);
		}
		if (y <= mark.y) {
			setPos(mark.x, mark.y, mark.z);
			shatter(level, king);
			return;
		}
		setPos(mark.x, y, mark.z);
	}

	/** It shatters on the snow: {@value #DAMAGE} damage to each player within {@value #REACH} block of where it lands. */
	public int shatter(ServerLevel level, YetiKingEntity king) {
		int hit = 0;
		for (ServerPlayer player : king.nearby(level)) {
			double dx = player.getX() - mark.x;
			double dz = player.getZ() - mark.z;
			if (dx * dx + dz * dz <= (REACH + player.getBbWidth() / 2.0) * (REACH + player.getBbWidth() / 2.0)
					&& Math.abs(player.getY() - mark.y) < 2.5
					&& player.hurtServer(level, damageSources().mobProjectile(this, king), YetiKingEntity.damage(DAMAGE))) {
				hit++;
			}
		}
		level.sendParticles(ParticleTypes.ITEM_SNOWBALL, mark.x, mark.y + 0.2, mark.z, 14, 0.4, 0.2, 0.4, 0.15);
		level.playSound(null, mark.x, mark.y, mark.z, SoundType.GLASS.getBreakSound(), SoundSource.HOSTILE, 1.2F, 1.2F);
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
		controllers.add(new AnimationController<FallingIcicleEntity>("main", 0, test -> test.setAndContinue(FALL)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
