package io.github.jimbozoomer.jugcraft.rocketry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The trolley a rider hangs from on a zipline (batch 40, docs/features/zipline.md): invisible, it runs along the line
 * from one anchor to the other with its passenger hanging below, faster on a steeper drop, and lets them go at the far
 * end. Sneaking lets go early. It goes away as soon as it has no passenger, the line comes down or it is reloaded from
 * a save (a ride is not resumed; it must still be saveable, or the game will not let anyone ride it).
 */
public class ZiplineRider extends Entity {
	/** Blocks a tick on the level, and the most a steep drop adds. */
	public static final double SPEED = 0.4;
	public static final double DROP_BONUS = 0.6;
	/** How far below the line the rider's feet hang. */
	public static final double HANG = 2.0;
	/** The ride starts and ends this far from each anchor, so the rider is clear of the posts. */
	public static final double MARGIN = 1.0;
	private static final int CHECK_EVERY = 5;

	private @Nullable BlockPos fromAnchor;
	private @Nullable BlockPos toAnchor;
	private Vec3 start = Vec3.ZERO;
	private Vec3 end = Vec3.ZERO;
	private double travelled;
	private boolean arrived;

	public ZiplineRider(EntityType<? extends ZiplineRider> type, Level level) {
		super(type, level);
		noPhysics = true;
	}

	/** Puts {@code rider} on the line from the anchor at {@code from} to its other end. False if they cannot ride. */
	public static boolean ride(ServerLevel level, BlockPos from, Entity rider) {
		if (rider.isPassenger() || rider.isVehicle() || !(level.getBlockEntity(from) instanceof ZiplineAnchorBlockEntity anchor)) {
			return false;
		}
		BlockPos to = anchor.link();
		if (to == null || !level.isLoaded(to) || !(level.getBlockEntity(to) instanceof ZiplineAnchorBlockEntity)) {
			return false;
		}
		Vec3 a = ZiplineAnchorBlockEntity.attachPoint(from);
		Vec3 b = ZiplineAnchorBlockEntity.attachPoint(to);
		double length = a.distanceTo(b);
		if (length <= 2 * MARGIN) {
			return false;
		}
		Vec3 direction = b.subtract(a).normalize();
		ZiplineRider trolley = new ZiplineRider(JugcraftRocketry.ZIPLINE_RIDER, level);
		trolley.fromAnchor = from.immutable();
		trolley.toAnchor = to.immutable();
		trolley.start = a.add(direction.scale(MARGIN));
		trolley.end = b.subtract(direction.scale(MARGIN));
		float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
		Vec3 at = trolley.start.subtract(0, HANG, 0);
		trolley.snapTo(at.x, at.y, at.z, yaw, 0);
		level.addFreshEntity(trolley);
		rider.setYRot(yaw);
		rider.setYHeadRot(yaw);
		if (!rider.startRiding(trolley, true, true)) {
			trolley.discard();
			return false;
		}
		level.playSound(null, from, SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.0F, 1.4F);
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		if (getPassengers().isEmpty() || fromAnchor == null || toAnchor == null) {
			discard();
			return;
		}
		if (tickCount % CHECK_EVERY == 0 && !lineIntact(level)) {
			ejectPassengers();
			discard();
			return;
		}
		Vec3 line = end.subtract(start);
		double length = line.length();
		double drop = Math.max(0.0, -line.y / length);
		travelled += SPEED + DROP_BONUS * drop;
		if (travelled >= length) {
			arrived = true;
			ejectPassengers();
			discard();
			return;
		}
		Vec3 at = start.add(line.scale(travelled / length)).subtract(0, HANG, 0);
		setPos(at.x, at.y, at.z);
	}

	/** Whether both anchors are still there and joined to each other. */
	private boolean lineIntact(ServerLevel level) {
		return level.getBlockEntity(fromAnchor) instanceof ZiplineAnchorBlockEntity a && toAnchor.equals(a.link())
				&& level.getBlockEntity(toAnchor) instanceof ZiplineAnchorBlockEntity;
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		passenger.resetFallDistance();
		if (!(passenger instanceof LivingEntity)) {
			Vec3 at = dropPoint();
			passenger.setPos(at.x, at.y, at.z);
		}
	}

	/** Where riders get off: beside the far anchor when they reach it, else just below the line where they let go. */
	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		return dropPoint();
	}

	private Vec3 dropPoint() {
		if (!arrived || toAnchor == null) {
			return position();
		}
		// Beside the far anchor, on the side the ride came from.
		Vec3 back = start.subtract(end).multiply(1, 0, 1);
		Vec3 side = back.lengthSqr() < 1.0E-6 ? Vec3.ZERO : back.normalize();
		return Vec3.atBottomCenterOf(toAnchor).add(side);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
