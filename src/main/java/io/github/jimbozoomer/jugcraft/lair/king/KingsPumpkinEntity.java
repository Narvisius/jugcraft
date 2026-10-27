package io.github.jimbozoomer.jugcraft.lair.king;

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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The flaming pumpkin the Pumpkin King plucks from the rows and tosses (his Gourd Toss, docs/features/pumpkin-king.md):
 * it arcs from his hand onto the ring of embers he marked, over the ticks it is given, and bursts there:
 * {@value PumpkinKingEntity#TOSS_DAMAGE} to each player within {@value PumpkinKingEntity#TOSS_SPLASH} blocks, set alight
 * for {@value PumpkinKingEntity#TOSS_FIRE_SECONDS} seconds. It never breaks or lights a block, cannot be stopped or
 * harmed, and is never saved; without its King it burns out.
 */
public class KingsPumpkinEntity extends Entity implements GeoEntity {
	private static final RawAnimation TUMBLE = RawAnimation.begin().thenLoop("animation.kings_pumpkin.tumble");
	/** How high its arc rises over the straight line from his hand to the mark, at the middle. */
	private static final double ARC = 4.0;

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID king;
	private Vec3 from = Vec3.ZERO;
	private Vec3 to = Vec3.ZERO;
	private int flight = 1;
	private int flown;

	public KingsPumpkinEntity(EntityType<? extends KingsPumpkinEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code king} tosses a flaming pumpkin from {@code from} onto {@code to}, landing {@code ticks} ticks later. */
	public static KingsPumpkinEntity toss(ServerLevel level, PumpkinKingEntity king, Vec3 from, Vec3 to, int ticks) {
		KingsPumpkinEntity pumpkin = new KingsPumpkinEntity(JugcraftPumpkinKing.KINGS_PUMPKIN, level);
		pumpkin.king = king.getUUID();
		pumpkin.from = from;
		pumpkin.to = to;
		pumpkin.flight = Math.max(1, ticks);
		pumpkin.snapTo(from.x, from.y, from.z, king.getYRot(), 0.0F);
		level.addFreshEntity(pumpkin);
		level.playSound(null, from.x, from.y, from.z, PumpkinKingEntity.sound("entity.blaze.shoot", SoundEvents.PARROT_AMBIENT), SoundSource.HOSTILE,
				1.5F, 0.7F);
		return pumpkin;
	}

	/** Where it is {@code tick} ticks into its flight. */
	public Vec3 along(int tick) {
		double share = Math.min(1.0, tick / (double) flight);
		Vec3 at = from.add(to.subtract(from).scale(share));
		return at.add(0.0, ARC * 4.0 * share * (1.0 - share), 0.0);
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		PumpkinKingEntity owner = king != null && level.getEntity(king) instanceof PumpkinKingEntity found && found.isAlive() ? found : null;
		if (owner == null) {
			level.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.3, getZ(), 6, 0.2, 0.2, 0.2, 0.02);
			discard();
			return;
		}
		flown++;
		Vec3 next = along(flown);
		setPos(next.x, next.y, next.z);
		level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.4, getZ(), 2, 0.15, 0.15, 0.15, 0.01);
		if (flown >= flight) {
			burst(level, owner);
			discard();
		}
	}

	/** It bursts on the mark: each player near is hurt and set alight; returns how many. */
	public int burst(ServerLevel level, PumpkinKingEntity owner) {
		int hit = 0;
		DamageSource source = damageSources().mobProjectile(this, owner);
		for (ServerPlayer player : level.players()) {
			double flat = Math.hypot(player.getX() - to.x, player.getZ() - to.z);
			if (!LairBosses.eligible(player) || flat > PumpkinKingEntity.TOSS_SPLASH + player.getBbWidth() / 2.0
					|| Math.abs(player.getY() - to.y) > 3.0) {
				continue;
			}
			if (player.hurtServer(level, source, PumpkinKingEntity.damage(PumpkinKingEntity.TOSS_DAMAGE))) {
				hit++;
				player.igniteForSeconds(PumpkinKingEntity.TOSS_FIRE_SECONDS);
			}
		}
		level.sendParticles(ParticleTypes.FLAME, to.x, to.y + 0.3, to.z, 40, 1.0, 0.4, 1.0, 0.05);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, to.x, to.y + 0.3, to.z, 10, 0.6, 0.3, 0.6, 0.02);
		level.playSound(null, to.x, to.y, to.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.4F);
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
		controllers.add(new AnimationController<KingsPumpkinEntity>("main", 0, test -> test.setAndContinue(TUMBLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
