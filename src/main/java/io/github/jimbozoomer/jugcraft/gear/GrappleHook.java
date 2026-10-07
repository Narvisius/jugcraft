package io.github.jimbozoomer.jugcraft.gear;

import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The pneumatic grapple's hook on its line (batch 30). In flight it is an ordinary projectile. Stuck in a block, it reels
 * its owner in; stuck in a mob, it pulls the mob to its owner. Everything is decided on the server: the owner's pull is
 * sent to their client as motion, like a knockback, so the client never moves itself. The hook lets go when its owner
 * arrives, after a few seconds, when they put the grapple away, or when they use it again.
 */
public class GrappleHook extends ThrowableItemProjectile {
	/** Speed the line reels a player in, in blocks a tick, and how close counts as arrived. */
	public static final double REEL_SPEED = 0.9;
	public static final double ARRIVE = 1.6;
	/** The little hop given on arrival, to clear a ledge. */
	public static final double ARRIVAL_HOP = 0.45;
	/** Speed a hooked mob is dragged in, and how close it comes. */
	public static final double DRAG_SPEED = 0.7;
	public static final double DRAG_STOP = 2.5;
	/** Ticks a hook holds on before it lets go by itself (in a block, and in a mob). */
	public static final int MAX_ANCHOR_TICKS = 100;
	public static final int MAX_DRAG_TICKS = 60;
	/** Mobs with this much health or more (bosses, iron golems) are too heavy to drag. */
	public static final float MAX_DRAG_HEALTH = 100.0F;

	private static final EntityDataAccessor<Boolean> ANCHORED = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> HOOKED = SynchedEntityData.defineId(GrappleHook.class, EntityDataSerializers.INT);
	/** The hook each player has out (server only), so a second use lets go instead of firing again. */
	private static final Map<UUID, GrappleHook> ACTIVE = new HashMap<>();

	/** Ticks since it stuck. */
	private int held;

	public GrappleHook(EntityType<? extends GrappleHook> type, Level level) {
		super(type, level);
	}

	public GrappleHook(Level level, LivingEntity owner, ItemStack stack) {
		super(JugcraftGrapple.GRAPPLE_HOOK, owner, level, stack);
	}

	/** The hook {@code player} has out, or null. */
	public static @Nullable GrappleHook active(Player player) {
		GrappleHook hook = ACTIVE.get(player.getUUID());
		return hook != null && !hook.isRemoved() ? hook : null;
	}

	static void track(Player player, GrappleHook hook) {
		ACTIVE.put(player.getUUID(), hook);
	}

	@Override
	protected Item getDefaultItem() {
		return JugcraftGrapple.PNEUMATIC_GRAPPLE;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ANCHORED, false);
		builder.define(HOOKED, -1);
	}

	/** Whether the hook is stuck in a block (synced, for the renderer's taut line). */
	public boolean isAnchored() {
		return entityData.get(ANCHORED);
	}

	/** The entity id of the mob on the hook, or -1. */
	public int hookedId() {
		return entityData.get(HOOKED);
	}

	@Override
	public void tick() {
		if (!level().isClientSide() && !ownerStillHolds()) {
			discard();
			return;
		}
		if (isAnchored() || hookedId() >= 0) {
			// Stuck: the server holds it still (or on its mob); the client is sent where it is.
			setDeltaMovement(Vec3.ZERO);
			if (level() instanceof ServerLevel server) {
				held++;
				if (isAnchored()) {
					reelOwner();
				} else {
					dragHooked(server);
				}
			}
			return;
		}
		super.tick();
		if (!level().isClientSide() && getOwner() != null && distanceToSqr(getOwner()) > (double) JugcraftGrapple.RANGE * JugcraftGrapple.RANGE) {
			discard(); // The line ran out.
		}
	}

	/** The owner is alive, here, close enough and still holding a grapple. */
	private boolean ownerStillHolds() {
		if (!(getOwner() instanceof Player owner) || !owner.isAlive() || owner.level() != level()
				|| distanceToSqr(owner) > (JugcraftGrapple.RANGE + 8.0) * (JugcraftGrapple.RANGE + 8.0)) {
			return false;
		}
		return owner.getMainHandItem().is(JugcraftGrapple.PNEUMATIC_GRAPPLE) || owner.getOffhandItem().is(JugcraftGrapple.PNEUMATIC_GRAPPLE);
	}

	/** Pulls the owner along the line; on arrival (or after a while) gives a little hop and lets go. */
	private void reelOwner() {
		Player owner = (Player) getOwner();
		Vec3 line = position().subtract(owner.position().add(0, owner.getBbHeight() * 0.5, 0));
		owner.resetFallDistance();
		if (line.length() < ARRIVE || held > MAX_ANCHOR_TICKS) {
			push(owner, owner.getDeltaMovement().multiply(0.5, 0, 0.5).add(0, ARRIVAL_HOP, 0));
			discard();
			return;
		}
		push(owner, owner.getDeltaMovement().scale(0.3).add(line.normalize().scale(REEL_SPEED)));
	}

	/** Drags the hooked mob toward the owner until it is close, then lets go. */
	private void dragHooked(ServerLevel level) {
		Entity target = level.getEntity(hookedId());
		Entity owner = getOwner();
		if (target == null || !target.isAlive() || owner == null || held > MAX_DRAG_TICKS) {
			discard();
			return;
		}
		setPos(target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ());
		Vec3 line = owner.position().subtract(target.position());
		if (line.length() < DRAG_STOP) {
			discard();
			return;
		}
		push(target, line.normalize().scale(DRAG_SPEED).add(0, 0.1, 0));
		target.resetFallDistance();
	}

	/** Sets an entity's motion; a player's client is told, since a player moves themselves. */
	private static void push(Entity entity, Vec3 motion) {
		entity.setDeltaMovement(motion);
		if (entity instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return entity != getOwner() && super.canHitEntity(entity);
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		if (!level().isClientSide() && !isRemoved()) {
			Vec3 at = hit.getLocation();
			setPos(at.x, at.y, at.z);
			setDeltaMovement(Vec3.ZERO);
			setNoGravity(true);
			entityData.set(ANCHORED, true);
			held = 0;
			level().playSound(null, at.x, at.y, at.z, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 1.2F);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (level().isClientSide() || isRemoved()) {
			return;
		}
		Entity target = hit.getEntity();
		if (canDrag(target)) {
			entityData.set(HOOKED, target.getId());
			setNoGravity(true);
			held = 0;
			level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8F, 1.0F);
		} else {
			discard();
		}
	}

	/** Living things light enough to drag; another player only where the owner may hurt them, and never a party member. */
	private boolean canDrag(Entity target) {
		if (!(target instanceof LivingEntity living) || living.getMaxHealth() >= MAX_DRAG_HEALTH) {
			return false;
		}
		if (target instanceof Player victim) {
			return getOwner() instanceof Player owner && owner.canHarmPlayer(victim)
					&& !JugcraftParties.sameParty(owner.getUUID(), victim.getUUID());
		}
		return true;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (getOwner() instanceof Player owner) {
			ACTIVE.remove(owner.getUUID(), this);
		}
		super.remove(reason);
	}
}
