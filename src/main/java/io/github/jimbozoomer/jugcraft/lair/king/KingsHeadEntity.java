package io.github.jimbozoomer.jugcraft.lair.king;

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
 * The Pumpkin King's head, lifted from his shoulders and set rolling (his Head Roll, docs/features/pumpkin-king.md): it
 * rolls along the line he fixed, its length over the ticks it is given, burning, and each player it meets (within
 * {@value PumpkinKingEntity#ROLL_HALF_WIDTH} blocks of its path) takes {@value PumpkinKingEntity#ROLL_DAMAGE} once and is
 * thrown aside. Then it is back on his shoulders: he discards it when his roll is over, or it goes when he does. It cannot
 * be stopped or harmed, and is never saved.
 */
public class KingsHeadEntity extends Entity implements GeoEntity {
	private static final RawAnimation ROLL = RawAnimation.begin().thenLoop("animation.kings_head.roll");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable UUID king;
	private Vec3 from = Vec3.ZERO;
	private Vec3 way = new Vec3(0.0, 0.0, 1.0);
	private double length;
	private int ticks = 1;
	private int rolled;
	private final Set<UUID> struck = new HashSet<>();

	public KingsHeadEntity(EntityType<? extends KingsHeadEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code king}'s head rolls from {@code from} along {@code way} (flat, of length 1) for {@code length} blocks, over {@code ticks}. */
	public static KingsHeadEntity roll(ServerLevel level, PumpkinKingEntity king, Vec3 from, Vec3 way, double length, int ticks) {
		KingsHeadEntity head = new KingsHeadEntity(JugcraftPumpkinKing.KINGS_HEAD, level);
		head.king = king.getUUID();
		head.from = from;
		head.way = way;
		head.length = length;
		head.ticks = Math.max(1, ticks);
		head.snapTo(from.x, from.y, from.z, (float) (Math.toDegrees(Math.atan2(way.z, way.x)) - 90.0), 0.0F);
		level.addFreshEntity(head);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 1.5F, 1.2F);
		return head;
	}

	/** Where it is {@code tick} ticks into its roll. */
	public Vec3 along(int tick) {
		return from.add(way.scale(length * Math.min(1.0, tick / (double) ticks)));
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		PumpkinKingEntity owner = king != null && level.getEntity(king) instanceof PumpkinKingEntity found && found.isAlive() ? found : null;
		if (owner == null || rolled > ticks + 2) {
			discard();
			return;
		}
		rolled++;
		Vec3 next = along(rolled);
		setPos(next.x, next.y, next.z);
		strike(level, owner);
		if (rolled % 2 == 0) {
			level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 3, 0.3, 0.3, 0.3, 0.01);
		}
		if (rolled % 6 == 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 1.0F, 1.4F);
		}
	}

	/** Each player it meets, once: hurt, and thrown aside off its path. */
	private void strike(ServerLevel level, PumpkinKingEntity owner) {
		for (ServerPlayer player : level.players()) {
			Vec3 to = new Vec3(player.getX() - getX(), 0.0, player.getZ() - getZ());
			if (!LairBosses.eligible(player) || to.length() > PumpkinKingEntity.ROLL_HALF_WIDTH + player.getBbWidth() / 2.0
					|| Math.abs(player.getY() - getY()) > 2.0 || !struck.add(player.getUUID())) {
				continue;
			}
			DamageSource source = damageSources().mobProjectile(this, owner);
			if (player.hurtServer(level, source, PumpkinKingEntity.damage(PumpkinKingEntity.ROLL_DAMAGE))) {
				double side = to.x * way.z - to.z * way.x >= 0.0 ? 1.0 : -1.0;
				Vec3 aside = new Vec3(way.z * side, 0.0, -way.x * side).scale(PumpkinKingEntity.ROLL_THROW);
				PumpkinKingEntity.push(player, aside.add(0.0, 0.35, 0.0));
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
		controllers.add(new AnimationController<KingsHeadEntity>("main", 0, test -> test.setAndContinue(ROLL)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
