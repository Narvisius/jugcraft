package io.github.jimbozoomer.jugcraft.lair.tatterlace;

import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Madame Tatterlace's Binding Thread: a thread shot from her spinnerets at {@value #SPEED} blocks a tick, which flies for
 * at most {@value #LIFE} ticks. If it catches a player (and their Golden Thimble does not turn it aside), it holds them
 * for {@value #TETHER_TICKS} ticks: they cannot get more than {@value #TETHER_REACH} blocks from where it holds, and that
 * point is reeled toward her {@value #REEL} blocks a tick. Hitting the thread ({@value #HEALTH} damage is enough), or
 * sprint-jumping against it at its full stretch, snaps it. It is drawn as its own particles, and never saved.
 */
public class BindingThreadEntity extends Entity {
	public static final double SPEED = 1.2;
	public static final int LIFE = 30;
	public static final int TETHER_TICKS = 60;
	public static final double TETHER_REACH = 4.0;
	public static final double REEL = 0.05;
	public static final float HEALTH = 1.0F;

	private @Nullable UUID owner;
	private @Nullable UUID held;
	private Vec3 velocity = Vec3.ZERO;
	private Vec3 anchor = Vec3.ZERO;
	private int tethered;

	public BindingThreadEntity(EntityType<? extends BindingThreadEntity> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	/** {@code boss} shoots a thread from her spinnerets at {@code target}. */
	public static BindingThreadEntity shoot(ServerLevel level, TatterlaceEntity boss, Entity target) {
		BindingThreadEntity thread = new BindingThreadEntity(JugcraftTatterlace.BINDING_THREAD, level);
		thread.owner = boss.getUUID();
		Vec3 from = boss.spinnerets();
		thread.velocity = target.getBoundingBox().getCenter().subtract(from).normalize().scale(SPEED);
		thread.snapTo(from.x, from.y, from.z, 0.0F, 0.0F);
		level.addFreshEntity(thread);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.8F);
		return thread;
	}

	/** Whether a Binding Thread already holds {@code player}. */
	public static boolean tethered(ServerLevel level, Player player) {
		for (BindingThreadEntity thread : level.getEntitiesOfClass(BindingThreadEntity.class, player.getBoundingBox().inflate(TETHER_REACH * 2.0))) {
			if (player.getUUID().equals(thread.held)) {
				return true;
			}
		}
		return false;
	}

	/** The player it holds, or null while it flies. */
	public @Nullable UUID held() {
		return held;
	}

	public Vec3 anchor() {
		return anchor;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		TatterlaceEntity boss = owner != null && level.getEntity(owner) instanceof TatterlaceEntity found && found.isAlive() ? found : null;
		if (boss == null) {
			discard();
			return;
		}
		if (held == null) {
			fly(level, boss);
		} else {
			hold(level, boss);
		}
	}

	private void fly(ServerLevel level, TatterlaceEntity boss) {
		if (tickCount > LIFE) {
			discard();
			return;
		}
		Vec3 next = position().add(velocity);
		AABB sweep = getBoundingBox().expandTowards(velocity).inflate(0.4);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, sweep)) {
			if (!LairBosses.eligible(player)) {
				continue;
			}
			if (GoldenThimble.turnAside(level, player)) {
				snap(level);
				return;
			}
			held = player.getUUID();
			anchor = player.position();
			tethered = 0;
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOOL_HIT, SoundSource.HOSTILE, 1.5F, 0.6F);
			return;
		}
		if (!level.noCollision(this, getBoundingBox().move(velocity))) {
			discard();
			return;
		}
		setPos(next.x, next.y, next.z);
		line(level, boss.spinnerets(), position(), 0.5);
	}

	/**
	 * Holds its player: the point it holds is reeled toward her, and a player straining past its reach is pulled back,
	 * unless they sprint-jump against it, which snaps it.
	 */
	private void hold(ServerLevel level, TatterlaceEntity boss) {
		Player player = level.getPlayerByUUID(held);
		if (player == null || !LairBosses.eligible(player) || player.level() != level || ++tethered > TETHER_TICKS) {
			discard();
			return;
		}
		Vec3 toBoss = new Vec3(boss.getX() - anchor.x, 0.0, boss.getZ() - anchor.z);
		if (toBoss.length() > REEL) {
			anchor = anchor.add(toBoss.normalize().scale(REEL));
		}
		Vec3 off = new Vec3(player.getX() - anchor.x, 0.0, player.getZ() - anchor.z);
		double stretch = off.length();
		if (stretch > TETHER_REACH) {
			if (player.isSprinting() && !player.onGround()) {
				snap(level);
				return;
			}
			Vec3 back = off.normalize().scale(-Math.min(0.6, (stretch - TETHER_REACH) * 0.5 + 0.1));
			player.setDeltaMovement(back.x, Math.min(player.getDeltaMovement().y, 0.1), back.z);
			if (player instanceof ServerPlayer server) {
				server.connection.send(new ClientboundSetEntityMotionPacket(server));
			}
		}
		Vec3 middle = anchor.add(player.position()).scale(0.5).add(0.0, 0.6, 0.0);
		setPos(middle.x, middle.y, middle.z);
		if (tethered % 2 == 0) {
			line(level, boss.spinnerets(), anchor.add(0.0, 0.2, 0.0), 0.8);
			line(level, anchor.add(0.0, 0.2, 0.0), player.position().add(0.0, 1.0, 0.0), 0.5);
		}
	}

	/** The thread snaps. */
	public void snap(ServerLevel level) {
		level.sendParticles(ParticleTypes.ITEM_COBWEB, getX(), getY(), getZ(), 8, 0.3, 0.3, 0.3, 0.05);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_BREAK, SoundSource.PLAYERS, 1.5F, 1.4F);
		discard();
	}

	private static void line(ServerLevel level, Vec3 from, Vec3 to, double step) {
		int points = Math.max(2, (int) (from.distanceTo(to) / step));
		for (int i = 0; i <= points; i++) {
			Vec3 at = from.add(to.subtract(from).scale(i / (double) points));
			level.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	/** Struck: enough of a blow snaps it. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (amount < HEALTH && !(source.getEntity() instanceof Player)) {
			return false;
		}
		snap(level);
		return true;
	}

	/** Players can strike it (their client picks it out), in flight or while it holds someone. */
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
}
