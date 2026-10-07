package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Ferris wheel (fall addition 27): the turning part of a fairground big wheel, standing over its booth
 * ({@link FerrisWheelBlock}), which it belongs to and which drives it. Two A-frames hold its hub {@value #HUB} blocks up;
 * its {@value #CARS} cars hang {@value #RADIUS} blocks round, {@value #SEATS} seats to a car. The client draws it
 * (client/FerrisWheelRenderer.java) from tools/ferris_wheel_data.py's quads.
 *
 * <p>The booth hands it the kinetic energy it takes ({@link #power}): {@value #NEED} KE a tick turns it at full speed,
 * once round in {@value #TURN_TICKS} ticks, and less turns it slower in proportion; it speeds up and slows down gently.
 * The server turns it and syncs its speed, and its angle now and then; the client turns it in between.
 *
 * <p>Riders sit in the seats ({@link #seatOf}): a player boards through the booth ({@link #board}) into the car nearest
 * the bottom. Getting off sets them down in front of the booth. If a block stands where a car would go next, the wheel
 * stops, jammed, until it is cleared. Riding once round earns Round and Round, and once round with someone in the seat
 * beside you, Two to a Car. Its numbers are tools/ferris_wheel.py's.
 */
public class FerrisWheel extends Entity {
	public static final int CARS = 8;
	public static final int SEATS = 2;
	public static final double HUB = 9.5;
	public static final double RADIUS = 6.0;
	public static final double SEAT_ACROSS = 0.34;
	public static final double SEAT_DOWN = 1.75;
	public static final double SEAT_BACK = 0.25;
	public static final long NEED = 12;
	public static final int TURN_TICKS = 800;
	public static final float FULL_SPEED = (float) (2.0 * Math.PI / TURN_TICKS);
	public static final float ACCEL = FULL_SPEED / 40.0F;
	public static final float DECEL = FULL_SPEED / 60.0F;
	public static final double BOARD_REACH = 4.0;
	/** How often (ticks) the server sends the angle to correct the clients' turning. */
	private static final int SYNC_EVERY = 20;
	/** Angles and speeds go to clients in millionths of a radian. */
	private static final float MICRO = 1_000_000.0F;
	private static final float TURN = (float) (2.0 * Math.PI);

	private static final EntityDataAccessor<Integer> FACING = SynchedEntityData.defineId(FerrisWheel.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SPEED = SynchedEntityData.defineId(FerrisWheel.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ANGLE = SynchedEntityData.defineId(FerrisWheel.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> JAMMED = SynchedEntityData.defineId(FerrisWheel.class, EntityDataSerializers.BOOLEAN);
	/** Who sits in each seat (car * SEATS + side): their entity id, or -1. */
	private static final List<EntityDataAccessor<Integer>> RIDERS = riders();

	private float angle;
	private float angleO;
	private float speed;
	private int seenAngle = Integer.MIN_VALUE;
	/** KE the booth has handed over since the last tick. */
	private long power;
	/** How far each seat's rider has gone round, and how far with someone beside them (server only, in radians). */
	private final float[] travelled = new float[CARS * SEATS];
	private final float[] together = new float[CARS * SEATS];

	public FerrisWheel(EntityType<? extends FerrisWheel> type, Level level) {
		super(type, level);
		noPhysics = true;
		setNoGravity(true);
	}

	private static List<EntityDataAccessor<Integer>> riders() {
		List<EntityDataAccessor<Integer>> out = new ArrayList<>();
		for (int seat = 0; seat < CARS * SEATS; seat++) {
			out.add(SynchedEntityData.defineId(FerrisWheel.class, EntityDataSerializers.INT));
		}
		return List.copyOf(out);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(FACING, Direction.NORTH.get2DDataValue());
		builder.define(SPEED, 0);
		builder.define(ANGLE, 0);
		builder.define(JAMMED, false);
		for (EntityDataAccessor<Integer> rider : RIDERS) {
			builder.define(rider, -1);
		}
	}

	/** Raises a wheel over the booth at {@code booth}, facing {@code facing}; returns it, or null if it couldn't be made. */
	public static @Nullable FerrisWheel raise(ServerLevel level, BlockPos booth, Direction facing) {
		FerrisWheel wheel = JugcraftAgriculture.FERRIS_WHEEL.create(level, EntitySpawnReason.TRIGGERED);
		if (wheel == null) {
			return null;
		}
		wheel.setPos(booth.getX() + 0.5, booth.getY(), booth.getZ() + 0.5);
		wheel.setFacing(facing);
		return level.addFreshEntity(wheel) ? wheel : null;
	}

	// ---------------------------------------------------------------- state

	/** The way the wheel's front faces (towards whoever placed it). */
	public Direction facing() {
		return Direction.from2DDataValue(entityData.get(FACING));
	}

	public void setFacing(Direction facing) {
		entityData.set(FACING, facing.get2DDataValue());
		setYRot(facing.toYRot());
	}

	/** How far round the wheel has turned, in radians (0 to 2 pi), between the last tick and this one. */
	public float angle(float partialTick) {
		float from = angleO;
		float to = angle;
		if (to < from - Math.PI) {
			to += TURN;
		}
		return Mth.lerp(partialTick, from, to);
	}

	public float angle() {
		return angle;
	}

	/** Its speed, in radians a tick (full speed {@link #FULL_SPEED}). */
	public float speed() {
		return level().isClientSide() ? entityData.get(SPEED) / MICRO : speed;
	}

	public boolean jammed() {
		return entityData.get(JAMMED);
	}

	/** Whether its lights are on: while it turns, or is driven, or anyone rides it. */
	public boolean lit() {
		return speed() > 0.0F || isVehicle();
	}

	/** Turns the wheel to {@code angle} at once (tests and the client test's pictures). */
	public void setAngle(float angle) {
		this.angle = Mth.positiveModulo(angle, TURN);
		angleO = this.angle;
		entityData.set(ANGLE, Math.round(this.angle * MICRO));
	}

	/** Sets its speed at once (tests and the client test's pictures). */
	public void setSpeed(float speed) {
		this.speed = Mth.clamp(speed, 0.0F, FULL_SPEED);
		entityData.set(SPEED, Math.round(this.speed * MICRO));
	}

	/** The booth hands over {@code ke} kinetic energy for this tick. */
	public void power(long ke) {
		power += ke;
	}

	/** Where the booth it stands over is. */
	public BlockPos booth() {
		return BlockPos.containing(getX(), getY() + 0.01, getZ());
	}

	// ---------------------------------------------------------------- geometry

	/** Where car {@code car} hangs round the wheel turned to {@code angle}, in radians: car 0 starts at the bottom. */
	public static float carAngle(int car, float angle) {
		return angle - (float) (Math.PI / 2) + car * TURN / CARS;
	}

	/** A point in the wheel's own frame (x along it, y up from the booth's foot, z along the axle, its front at -z) in the world. */
	public Vec3 toWorld(double x, double y, double z) {
		return position().add(new Vec3(x, y, z).yRot((float) Math.toRadians(-facingDegrees(facing()))));
	}

	/** How far a facing turns a north-facing model clockwise from above, as a blockstate's "y" does. */
	public static int facingDegrees(Direction facing) {
		return switch (facing) {
			case EAST -> 90;
			case SOUTH -> 180;
			case WEST -> 270;
			default -> 0;
		};
	}

	/** Where car {@code car}'s pivot is, in the wheel's own frame, with the wheel at {@code angle}. */
	public static Vec3 pivot(int car, float angle) {
		float a = carAngle(car, angle);
		return new Vec3(RADIUS * Mth.cos(a), HUB + RADIUS * Mth.sin(a), 0.0);
	}

	/** Where seat {@code seat}'s rider sits, in the wheel's own frame, with the wheel at {@code angle}. */
	public static Vec3 seat(int seat, float angle) {
		Vec3 pivot = pivot(seat / SEATS, angle);
		double across = seat % SEATS == 0 ? -SEAT_ACROSS : SEAT_ACROSS;
		return pivot.add(across, -SEAT_DOWN, SEAT_BACK);
	}

	/** The car nearest the bottom of the wheel at {@code angle}. */
	public static int bottomCar(float angle) {
		int best = 0;
		double nearest = Double.MAX_VALUE;
		for (int car = 0; car < CARS; car++) {
			double off = Math.abs(Mth.wrapDegrees(Math.toDegrees(carAngle(car, angle) + Math.PI / 2)));
			if (off < nearest) {
				nearest = off;
				best = car;
			}
		}
		return best;
	}

	// ---------------------------------------------------------------- riders

	/** Which seat {@code entity} sits in, or -1. */
	public int seatOf(Entity entity) {
		for (int seat = 0; seat < RIDERS.size(); seat++) {
			if (entityData.get(RIDERS.get(seat)) == entity.getId()) {
				return seat;
			}
		}
		return -1;
	}

	/** Who sits in {@code seat}, if anyone. */
	public @Nullable Entity rider(int seat) {
		int id = entityData.get(RIDERS.get(seat));
		if (id < 0) {
			return null;
		}
		for (Entity passenger : getPassengers()) {
			if (passenger.getId() == id) {
				return passenger;
			}
		}
		return null;
	}

	/** An empty seat in the car at the bottom, or -1. */
	public int freeSeat() {
		int car = bottomCar(angle);
		for (int side = 0; side < SEATS; side++) {
			if (entityData.get(RIDERS.get(car * SEATS + side)) < 0) {
				return car * SEATS + side;
			}
		}
		return -1;
	}

	/**
	 * {@code player} boards the car at the bottom if they are within {@value #BOARD_REACH} blocks of the booth, ride
	 * nothing already and a seat is free there; returns whether they did. Server side only.
	 */
	public boolean board(ServerPlayer player) {
		if (player.isPassenger() || player.isSpectator() || player.distanceToSqr(Vec3.atCenterOf(booth())) > BOARD_REACH * BOARD_REACH) {
			return false;
		}
		if (freeSeat() < 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.ferris_wheel.full"));
			return false;
		}
		return player.startRiding(this);
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return level().isClientSide() || passenger instanceof Player && freeSeat() >= 0;
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		if (!level().isClientSide() && seatOf(passenger) < 0) {
			int seat = freeSeat();
			if (seat >= 0) {
				entityData.set(RIDERS.get(seat), passenger.getId());
				travelled[seat] = 0.0F;
				together[seat] = 0.0F;
			}
		}
	}

	@Override
	protected void removePassenger(Entity passenger) {
		int seat = seatOf(passenger);
		super.removePassenger(passenger);
		if (!level().isClientSide() && seat >= 0) {
			entityData.set(RIDERS.get(seat), -1);
			travelled[seat] = 0.0F;
			together[seat] = 0.0F;
		}
	}

	/** Each rider sits in their seat in their car, wherever the wheel has turned it. */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		int seat = seatOf(passenger);
		Vec3 local = seat < 0 ? new Vec3(0.0, 1.0, 0.0) : seat(seat, angle);
		return local.yRot((float) Math.toRadians(-facingDegrees(facing())));
	}

	/** Getting off, however high their car, a rider is set down on the ground in front of the booth. */
	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		BlockPos front = booth().relative(facing());
		for (int up = 0; up <= 3; up++) {
			Vec3 spot = Vec3.atBottomCenterOf(front.above(up));
			AABB box = passenger.getBoundingBox().move(spot.subtract(passenger.position()));
			if (level().noCollision(passenger, box)) {
				return spot;
			}
		}
		return Vec3.atBottomCenterOf(booth().above());
	}

	// ---------------------------------------------------------------- turning

	@Override
	public void tick() {
		super.tick();
		angleO = angle;
		if (level() instanceof ServerLevel server) {
			serverTick(server);
		} else {
			int synced = entityData.get(ANGLE);
			if (synced != seenAngle) {
				seenAngle = synced;
				// The server's angle, once a second: a small correction to the client's own turning, or where it is on loading.
				angle = synced / MICRO;
				angleO = angle;
			}
			angle = Mth.positiveModulo(angle + speed(), TURN);
			if (angle < angleO - Math.PI) {
				angleO -= TURN;
			}
		}
	}

	private void serverTick(ServerLevel level) {
		if (tickCount % SYNC_EVERY == 0 && !(level.getBlockState(booth()).getBlock() instanceof FerrisWheelBlock)) {
			// Its booth is gone (an explosion, a command): the wheel goes too.
			ejectPassengers();
			discard();
			return;
		}
		long given = power;
		power = 0;
		float target = FULL_SPEED * Math.min(1.0F, given / (float) NEED);
		float next = speed + Mth.clamp(target - speed, -DECEL, ACCEL);
		boolean jammed = false;
		if (next > 0.0F && blocked(level, angle + next)) {
			next = 0.0F;
			jammed = true;
		}
		if (jammed && !jammed()) {
			level.playSound(null, getX(), getY() + HUB, getZ(), Midway.sound("block.anvil.place", SoundEvents.CHAIN_PLACE), SoundSource.BLOCKS, 0.5F, 0.6F);
			for (Entity passenger : getPassengers()) {
				if (passenger instanceof ServerPlayer rider) {
					rider.sendOverlayMessage(Component.translatable("message.jugcraft.ferris_wheel.jammed"));
				}
			}
		}
		entityData.set(JAMMED, jammed);
		if (Math.abs(next - speed) > 1.0E-7F || next == 0.0F && speed != 0.0F) {
			speed = next;
			entityData.set(SPEED, Math.round(speed * MICRO));
		}
		if (speed > 0.0F) {
			advance(speed);
		}
		if (tickCount % SYNC_EVERY == 0) {
			entityData.set(ANGLE, Math.round(angle * MICRO));
		}
		if (speed > 0.0F && tickCount % 40 == 0) {
			level.playSound(null, getX(), getY() + 1.0, getZ(), Midway.sound("entity.minecart.riding", SoundEvents.WOOD_STEP), SoundSource.BLOCKS, 0.15F, 0.5F);
		}
	}

	/** Turns the wheel on by {@code step} radians, counting the way its riders go round (the server's turning; tests). */
	public void advance(float step) {
		angle = Mth.positiveModulo(angle + step, TURN);
		ride(step);
	}

	/** Counts each rider's way round, and gives the advancements for a whole turn. */
	private void ride(float step) {
		for (int seat = 0; seat < RIDERS.size(); seat++) {
			if (!(rider(seat) instanceof ServerPlayer rider)) {
				continue;
			}
			float before = travelled[seat];
			travelled[seat] += step;
			if (before < TURN && travelled[seat] >= TURN) {
				TrickOrTreat.award(rider, "round_and_round");
			}
			int beside = seat % SEATS == 0 ? seat + 1 : seat - 1;
			if (rider(beside) instanceof Player) {
				float was = together[seat];
				together[seat] += step;
				if (was < TURN && together[seat] >= TURN) {
					TrickOrTreat.award(rider, "two_to_a_car");
				}
			} else {
				together[seat] = 0.0F;
			}
		}
	}

	/**
	 * Whether a block would stand in a car (or a rider) with the wheel at {@code next}: its floor, its seats, its riders'
	 * heads and its canopy are checked, for every car.
	 */
	public boolean blocked(Level level, float next) {
		BlockPos booth = booth();
		for (int car = 0; car < CARS; car++) {
			Vec3 pivot = pivot(car, next);
			double[][] points = {{0.0, -2.15, 0.0}, {-SEAT_ACROSS, -SEAT_DOWN + 0.3, SEAT_BACK}, {SEAT_ACROSS, -SEAT_DOWN + 0.3, SEAT_BACK},
					{0.0, -1.0, 0.0}, {0.0, -0.4, 0.0}};
			for (double[] p : points) {
				BlockPos pos = BlockPos.containing(toWorld(pivot.x + p[0], pivot.y + p[1], pivot.z + p[2]));
				if (!pos.equals(booth) && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	// ---------------------------------------------------------------- an entity apart

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	/** The space the whole wheel and its frame take up, for the client to tell when any of it is in view. */
	public AABB extent() {
		double reach = RADIUS + 1.5;
		return new AABB(getX() - reach, getY(), getZ() - reach, getX() + reach, getY() + HUB + reach, getZ() + reach);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		setFacing(Direction.from2DDataValue(input.getIntOr("facing", Direction.NORTH.get2DDataValue())));
		setAngle(input.getFloatOr("angle", 0.0F));
		setSpeed(input.getFloatOr("speed", 0.0F));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("facing", facing().get2DDataValue());
		output.putFloat("angle", angle);
		output.putFloat("speed", speed);
	}
}
