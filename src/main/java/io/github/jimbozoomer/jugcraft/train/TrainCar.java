package io.github.jimbozoomer.jugcraft.train;

import io.github.jimbozoomer.jugcraft.SmoothFlight;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A rail vehicle of the train framework (docs/features/trains.md): a locomotive or a car that runs on any rails. It
 * keeps its place on the line as a {@link TrackPoint} (its middle) and stands between two bogies a little in from each
 * end, so it leans into curves and up slopes as a train does. A car coupled into a train is moved by its locomotive
 * (the train moves as one, each car along its own stretch of line); a car on its own stays where it is set. Breaking
 * the rail under a car knocks it off as an item. Players take a car apart by hitting it, as they do a minecart.
 */
public abstract class TrainCar extends Entity {
	/** How far in from each end the bogies are, in blocks. */
	public static final double BOGIE_INSET = 0.6;
	/** The gap between two coupled cars' ends, in blocks. */
	public static final double COUPLING_GAP = 0.25;

	protected @Nullable TrackPoint track;
	/** The locomotive this car is coupled behind, if any (a locomotive's own is null). */
	protected @Nullable UUID locomotive;
	private float damage;
	/** Client side: how far the wheels have rolled, in blocks (for the renderer). */
	private float wheelRun;
	private float previousWheelRun;

	protected TrainCar(EntityType<? extends TrainCar> type, Level level) {
		super(type, level);
	}

	/** The car's length, end to end over its buffers, in blocks. */
	public abstract double length();

	/** The item it comes apart into. */
	public abstract ItemStack asItem();

	/** How many can ride it. */
	public abstract int seats();

	/** Where seat {@code index} is, in blocks: x across, y up, z forwards (the way the train faces). */
	protected abstract Vec3 seat(int index);

	/** The health a car has before it comes apart. */
	protected float health() {
		return 40.0F;
	}

	public @Nullable TrackPoint track() {
		return track;
	}

	public @Nullable UUID coupledTo() {
		return locomotive;
	}

	void coupleTo(@Nullable UUID locomotive) {
		this.locomotive = locomotive;
	}

	/** Client side: how far the wheels have rolled, in blocks. */
	public float wheelRun(float partialTick) {
		return Mth.lerp(partialTick, previousWheelRun, wheelRun);
	}

	/** Puts the car on the line with its middle at {@code point}: between its bogies, leaning with the line under them. */
	public void place(TrackPoint point) {
		track = point;
		Level level = level();
		double half = length() / 2 - BOGIE_INSET;
		Vec3 front = point.move(level, half).point().position(level);
		Vec3 back = point.move(level, -half).point().position(level);
		Vec3 middle = front.add(back).scale(0.5);
		Vec3 along = front.subtract(back);
		setPos(middle.x, middle.y, middle.z);
		if (along.lengthSqr() > 1.0E-6) {
			setYRot((float) Math.toDegrees(Math.atan2(-along.x, along.z)));
			setXRot((float) -Math.toDegrees(Math.atan2(along.y, Math.hypot(along.x, along.z))));
		}
	}

	/** The point on the line {@code distance} blocks ahead of this car's middle (behind, if negative). */
	public TrackPoint.Move ahead(double distance) {
		return track == null ? new TrackPoint.Move(new TrackPoint(blockPosition(), net.minecraft.core.Direction.NORTH,
				net.minecraft.core.Direction.SOUTH, 0.5), 0, true) : track.move(level(), distance);
	}

	@Override
	public LinearInterpolationHandler createInterpolationHandler() {
		return SmoothFlight.handler(this);
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel level) {
			if (track == null) {
				// Set down without a place on the line yet (an old save, or a command): find the rail it stands on.
				BlockPos at = blockPosition();
				TrackPoint found = TrackPoint.onRail(level, at, Vec3.directionFromRotation(0, getYRot()));
				if (found == null) {
					found = TrackPoint.onRail(level, at.below(), Vec3.directionFromRotation(0, getYRot()));
				}
				if (found != null) {
					place(found);
				}
			} else if (!track.valid(level)) {
				derail(level);
				return;
			}
			serverTick(level);
			return;
		}
		// Client side: roll the wheels by how far it moved, forwards or back.
		previousWheelRun = wheelRun;
		Vec3 forward = Vec3.directionFromRotation(0, getYRot());
		double moved = (getX() - xo) * forward.x + (getZ() - zo) * forward.z;
		if (Math.abs(moved) < 2.0) {
			wheelRun += (float) moved;
		}
		clientTick();
	}

	/** Each server tick, once the car knows its place on the line. */
	protected void serverTick(ServerLevel level) {
	}

	/** Each client tick. */
	protected void clientTick() {
	}

	/** The rail under it is gone: it comes off the line as an item, leaving its train. */
	protected void derail(ServerLevel level) {
		spawnAtLocation(level, asItem());
		dropContents(level);
		discard();
	}

	/** Anything the car carries, spilled when it comes apart. */
	protected void dropContents(ServerLevel level) {
	}

	/** Use: climb aboard, if there is a seat. */
	public InteractionResult use(Player player, InteractionHand hand) {
		if (seats() == 0 || player.isPassenger() || getPassengers().size() >= seats()) {
			return InteractionResult.PASS;
		}
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		return player.startRiding(this) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (reason.shouldDestroy() && level() instanceof ServerLevel level && locomotive != null
				&& level.getEntity(locomotive) instanceof Locomotive engine) {
			engine.uncouple(this);
		}
		super.remove(reason);
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().size() < seats();
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		int index = Math.max(0, getPassengers().indexOf(passenger));
		return seat(Math.min(index, Math.max(0, seats() - 1))).yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Player attacker
				&& !io.github.jimbozoomer.jugcraft.finance.LandRegistry.mayBuild(attacker, level, blockPosition())) {
			return false;
		}
		if (isRemoved() || !(source.getEntity() instanceof Player player) || player.getRootVehicle() == this) {
			return false;
		}
		if (player.getAbilities().instabuild) {
			dropContents(level);
			discard();
			return true;
		}
		damage += amount;
		if (damage >= health()) {
			spawnAtLocation(level, asItem());
			dropContents(level);
			discard();
		}
		return true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		track = input.read("track", TrackPoint.CODEC).orElse(null);
		locomotive = input.read("locomotive", UUIDUtil.CODEC).orElse(null);
		damage = input.getFloatOr("damage", 0.0F);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		if (track != null) {
			output.store("track", TrackPoint.CODEC, track);
		}
		if (locomotive != null) {
			output.store("locomotive", UUIDUtil.CODEC, locomotive);
		}
		output.putFloat("damage", damage);
	}
}
