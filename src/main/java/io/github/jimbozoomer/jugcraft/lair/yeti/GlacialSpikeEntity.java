package io.github.jimbozoomer.jugcraft.lair.yeti;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
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

/**
 * A spike of ice bursting up from the Yeti King's lake in his Glacial Spikes: as it bursts, every player within
 * {@value #WIDTH} block of it takes {@value YetiKingEntity#SPIKE_DAMAGE} damage and is thrown up. It rises over
 * {@value #RISE_TICKS} ticks, stands {@value #HOLD_TICKS} more, and is gone. It cannot be harmed and is never saved.
 */
public class GlacialSpikeEntity extends Entity implements GeoEntity {
	public static final int RISE_TICKS = 4;
	public static final int HOLD_TICKS = 20;
	public static final double WIDTH = 0.8;
	private static final RawAnimation BURST = RawAnimation.begin().thenPlayAndHold("animation.glacial_spike.burst");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public GlacialSpikeEntity(EntityType<? extends GlacialSpikeEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** A spike bursts up at {@code at} (on the lake's surface), for {@code king}: it strikes as it bursts. */
	public static GlacialSpikeEntity burst(ServerLevel level, YetiKingEntity king, Vec3 at) {
		GlacialSpikeEntity spike = new GlacialSpikeEntity(JugcraftYeti.GLACIAL_SPIKE, level);
		spike.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0F, 0.0F);
		level.addFreshEntity(spike);
		spike.strike(level, king);
		level.sendParticles(ParticleTypes.ITEM_SNOWBALL, at.x, at.y + 0.3, at.z, 8, 0.3, 0.3, 0.3, 0.1);
		level.playSound(null, at.x, at.y, at.z, SoundType.GLASS.getPlaceSound(), SoundSource.HOSTILE, 1.0F, 0.6F);
		return spike;
	}

	/** Every player within {@value #WIDTH} block takes {@value YetiKingEntity#SPIKE_DAMAGE} damage and is thrown up. */
	public int strike(ServerLevel level, YetiKingEntity king) {
		int hit = 0;
		for (ServerPlayer player : king.nearby(level)) {
			double dx = player.getX() - getX();
			double dz = player.getZ() - getZ();
			double reach = WIDTH + player.getBbWidth() / 2.0;
			if (dx * dx + dz * dz <= reach * reach && Math.abs(player.getY() - getY()) < 2.0
					&& player.hurtServer(level, damageSources().mobAttack(king), YetiKingEntity.damage(YetiKingEntity.SPIKE_DAMAGE))) {
				hit++;
				YetiKingEntity.push(player, Vec3.ZERO, 0.0, YetiKingEntity.SPIKE_LIFT);
			}
		}
		return hit;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel && tickCount > RISE_TICKS + HOLD_TICKS) {
			discard();
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
		controllers.add(new AnimationController<GlacialSpikeEntity>("main", 0, test -> test.setAndContinue(BURST)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
