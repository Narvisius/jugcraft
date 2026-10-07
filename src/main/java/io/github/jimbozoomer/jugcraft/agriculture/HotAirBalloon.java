package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.SmoothFlight;
import io.github.jimbozoomer.jugcraft.machine.GeneratorFuels;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A hot-air balloon (fall addition 29): a wicker basket under a twin-coil burner and an envelope in one of three
 * designs. Up to {@value #RIDERS} players stand in the basket; the first aboard is its pilot.
 *
 * <p>The server flies it. The pilot's client sends only two keys ({@link BalloonControlPayload}): jump fires the burner,
 * which heats the envelope by {@value #FIRE} a tick and burns a unit of fuel; back opens the vent at the crown, cooling
 * it by {@value #VENT} more. It cools by {@value #COOL} a tick of itself. Above {@value #NEUTRAL} heat it rises, below
 * it sinks: {@value #CLIMB} blocks a tick for each unit over or under, at most {@value #MAX_CLIMB} up and
 * {@value #MAX_SINK} down, less lift the higher it is ({@value #THIN} blocks above sea level take away a unit). It can't
 * be steered: aloft it drifts on the wind at its envelope's height ({@link FiestaWinds}); on the ground it stays put;
 * in water its basket floats. With nobody aboard its vent opens of itself, so a balloon left aloft comes down near where
 * it was left. A block where its envelope, burner or riders' heads would go stops it moving that way.
 *
 * <p>Fuel is what a generator burns ({@link GeneratorFuels}): a unit for every {@value #FUEL_PER_BURN_TICK} ticks a
 * generator would burn it, up to {@value #MAX_FUEL}. A Mooring Post can tether it ({@link MooringPostBlock}): then it
 * goes no further than {@value #ROPE} blocks across from the post and {@value #TETHER_HEIGHT} above it.
 *
 * <p>Advancements: Up, Up and Away ({@value #UP_HEIGHT} blocks over the ground), The Box (home within
 * {@value #BOX_HOME} blocks of where it took off, having been {@value #BOX_AWAY} away), Mass Ascension (aloft with
 * {@value #CROWD} other ridden balloons aloft within {@value #CROWD_RANGE} blocks).
 */
public class HotAirBalloon extends Entity {
	public static final int RIDERS = 4;
	public static final double BASKET = 1.5;
	public static final double BASKET_HEIGHT = 1.125;
	public static final double BURNER = 2.25;
	public static final double THROAT = 3.25;
	public static final double ENVELOPE_HEIGHT = 11.0;
	public static final double ENVELOPE_RADIUS = 4.5;
	public static final float FIRE = 0.004F;
	public static final float COOL = 0.0008F;
	public static final float VENT = 0.006F;
	public static final float NEUTRAL = 0.5F;
	public static final double CLIMB = 0.5;
	public static final double MAX_CLIMB = 0.25;
	public static final double MAX_SINK = 0.15;
	public static final double THIN = 400.0;
	public static final double RESPONSE = 0.04;
	public static final double DRIFT = 0.02;
	public static final int FUEL_PER_BURN_TICK = 4;
	public static final int MAX_FUEL = 12000;
	public static final int GAUGE_TICKS = 10;
	public static final int UP_HEIGHT = 32;
	public static final int BOX_HOME = 16;
	public static final int BOX_AWAY = 64;
	public static final int ALOFT = 8;
	public static final int CROWD = 2;
	public static final int CROWD_RANGE = 128;
	public static final double MOOR_REACH = 10.0;
	public static final double ROPE = 8.0;
	public static final double TETHER_HEIGHT = 16.0;
	/** Where riders stand: in from the basket's corners, and their feet on its floor (the vehicle attachment is 0.6 up). */
	public static final double STAND = 0.33;
	public static final double FLOOR = 1.5 / 16.0;
	public static final int SLOW_FALL_TICKS = 200;

	/** The three designs: the id the client draws, and the item that places one. */
	public enum Kind {
		HARVEST("harvest", "harvest_balloon"), PUMPKIN("pumpkin", "pumpkin_balloon"), MOON("moon", "harvest_moon_balloon");

		public final String id;
		public final String item;

		Kind(String id, String item) {
			this.id = id;
			this.item = item;
		}

		public static Kind of(int ordinal) {
			return values()[Mth.clamp(ordinal, 0, values().length - 1)];
		}
	}

	private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(HotAirBalloon.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> HEAT = SynchedEntityData.defineId(HotAirBalloon.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> BURNING = SynchedEntityData.defineId(HotAirBalloon.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> MOORED = SynchedEntityData.defineId(HotAirBalloon.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> MOOR_X = SynchedEntityData.defineId(HotAirBalloon.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> MOOR_Y = SynchedEntityData.defineId(HotAirBalloon.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> MOOR_Z = SynchedEntityData.defineId(HotAirBalloon.class, EntityDataSerializers.INT);

	private int fuel;
	private boolean burnerKey;
	private boolean ventKey;
	private @Nullable Vec3 takeoff;
	private double farthest;
	private boolean wasAloft;
	private final java.util.Set<Entity> lastRiders = new java.util.HashSet<>();

	public HotAirBalloon(EntityType<? extends HotAirBalloon> type, Level level) {
		super(type, level);
	}

	// ---------------------------------------------------------------- state

	public Kind kind() {
		return Kind.of(entityData.get(KIND));
	}

	public void setKind(Kind kind) {
		entityData.set(KIND, kind.ordinal());
	}

	/** How hot its envelope is, from 0 (cold) to 1. */
	public float heat() {
		return entityData.get(HEAT);
	}

	public void setHeat(float heat) {
		entityData.set(HEAT, Mth.clamp(heat, 0.0F, 1.0F));
	}

	/** Whether its burner fired this tick (the client draws the flame, and lights the envelope). */
	public boolean burning() {
		return entityData.get(BURNING);
	}

	public int fuel() {
		return fuel;
	}

	public void setFuel(int fuel) {
		this.fuel = Mth.clamp(fuel, 0, MAX_FUEL);
	}

	/** The post it is moored to, or null. */
	public @Nullable BlockPos mooring() {
		return entityData.get(MOORED) ? new BlockPos(entityData.get(MOOR_X), entityData.get(MOOR_Y), entityData.get(MOOR_Z)) : null;
	}

	public void moor(@Nullable BlockPos post) {
		entityData.set(MOORED, post != null);
		if (post != null) {
			entityData.set(MOOR_X, post.getX());
			entityData.set(MOOR_Y, post.getY());
			entityData.set(MOOR_Z, post.getZ());
		}
	}

	public @Nullable Player pilot() {
		return getFirstPassenger() instanceof Player player ? player : null;
	}

	/** The pilot's keys, as their client last sent them (checked to come from the pilot by the payload's receiver). */
	public void control(boolean burner, boolean vent) {
		burnerKey = burner;
		ventKey = vent;
	}

	public boolean burnerKey() {
		return burnerKey;
	}

	public boolean ventKey() {
		return ventKey;
	}

	/** The fuel units one of {@code stack} gives the burner; 0 if it isn't fuel. */
	public static int fuelUnits(ItemStack stack) {
		return GeneratorFuels.burnTicks(stack) / FUEL_PER_BURN_TICK;
	}

	/**
	 * Loads one of {@code stack} into the tanks, if it is fuel and there is room for all of it; returns whether it
	 * did. Its user is told the burner time it now has (or that the tanks are full).
	 */
	public boolean refuel(ItemStack stack, @Nullable Player user) {
		int units = fuelUnits(stack);
		if (units <= 0) {
			return false;
		}
		if (fuel + units > MAX_FUEL) {
			if (user instanceof ServerPlayer player) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.balloon.fuel_full"));
			}
			return false;
		}
		setFuel(fuel + units);
		stack.consume(1, user);
		level().playSound(null, getX(), getY() + 0.5, getZ(), SoundEvents.BUCKET_FILL_LAVA, SoundSource.PLAYERS, 0.6F, 1.4F);
		if (user instanceof ServerPlayer player) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.balloon.fuelled", Broomstick.seconds(fuel)));
		}
		return true;
	}

	/** The balloon as its item, keeping its fuel. */
	public ItemStack item() {
		ItemStack stack = new ItemStack(JugcraftAgriculture.item(kind().item));
		stack.set(JugcraftAgriculture.BALLOON_FUEL, fuel);
		return stack;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(KIND, 0);
		builder.define(HEAT, 0.0F);
		builder.define(BURNING, false);
		builder.define(MOORED, false);
		builder.define(MOOR_X, 0);
		builder.define(MOOR_Y, 0);
		builder.define(MOOR_Z, 0);
	}

	// ---------------------------------------------------------------- riders

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().size() < RIDERS && passenger instanceof Player;
	}

	/** One rider in each corner of the basket. */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		int i = Math.max(0, getPassengers().indexOf(passenger));
		double x = (i % 2 == 0 ? -1 : 1) * STAND;
		double z = (i < 2 ? -1 : 1) * STAND;
		return new Vec3(x, FLOOR + 0.6, z).yRot((float) Math.toRadians(-getYRot()));
	}

	/**
	 * The way each corner's rider sits facing, relative to the basket: along a wall, round the basket like a pinwheel, so
	 * that the game's seated pose (every rider is drawn seated) keeps their legs inside the wicker.
	 */
	private static final float[] CORNER_FACING = {-90.0F, 0.0F, 180.0F, 90.0F};
	/** How far a rider can look either way from the way they sit, as in a boat. */
	public static final float LOOK = 105.0F;

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction move) {
		super.positionRider(passenger, move);
		faceAlongTheWall(passenger);
	}

	@Override
	public void onPassengerTurned(Entity passenger) {
		faceAlongTheWall(passenger);
	}

	/** Sits a rider facing along the wall from their corner, looking no more than {@link #LOOK} degrees either way. */
	private void faceAlongTheWall(Entity passenger) {
		int i = getPassengers().indexOf(passenger);
		if (i < 0 || i >= CORNER_FACING.length) {
			return;
		}
		float along = getYRot() + CORNER_FACING[i];
		passenger.setYBodyRot(along);
		float turn = Mth.wrapDegrees(passenger.getYRot() - along);
		float held = Mth.clamp(turn, -LOOK, LOOK);
		passenger.yRotO += held - turn;
		passenger.setYRot(passenger.getYRot() + held - turn);
		passenger.setYHeadRot(passenger.getYRot());
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		if (passenger instanceof ServerPlayer rider && getPassengers().size() == 1) {
			rider.sendOverlayMessage(Component.translatable("message.jugcraft.balloon.controls"));
		}
	}

	/** Getting out on the ground, a rider steps out beside the basket. */
	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		Vec3 side = new Vec3(0.0, 0.0, BASKET / 2 + 0.6).yRot((float) Math.toRadians(-getYRot()));
		Vec3 spot = position().add(side);
		AABB box = passenger.getBoundingBox().move(spot.subtract(passenger.position()));
		return level().noCollision(passenger, box) ? spot : super.getDismountLocationForPassenger(passenger);
	}

	/**
	 * A rider who leaves the basket aloft floats down slowly for a while: the balloon's own drag rope, as it were. The
	 * server gives it the tick after they leave.
	 */
	private void softLandings() {
		for (Entity rider : lastRiders) {
			if (!getPassengers().contains(rider) && rider instanceof LivingEntity living && living.isAlive() && !living.isPassenger()
					&& !living.onGround()) {
				living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS));
			}
		}
		lastRiders.clear();
		lastRiders.addAll(getPassengers());
	}

	// ---------------------------------------------------------------- flying

	public boolean grounded() {
		return !level().noCollision(this, getBoundingBox().move(0.0, -0.05, 0.0));
	}

	/** How far it is above the ground (the highest block that stops it) below its basket. */
	public double heightAboveGround() {
		int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(getX()), Mth.floor(getZ()));
		return Math.max(0.0, getY() - ground);
	}

	/** The wind its envelope is in. */
	public Vec3 wind(ServerLevel level) {
		return FiestaWinds.at(level, getY() + THROAT + ENVELOPE_HEIGHT / 2);
	}

	/**
	 * The speed it heads for, up or down, at {@code heat} and height {@code y}: less lift the higher it is above a sea
	 * level of {@code seaLevel} (none lost below it).
	 */
	public static double climbFor(float heat, double y, int seaLevel) {
		double lift = (heat - NEUTRAL - Math.max(0.0, y - seaLevel) / THIN) * CLIMB;
		return Mth.clamp(lift, -MAX_SINK, MAX_CLIMB);
	}

	/** The client eases after the server's flight instead of jumping a tick at a time (see {@link SmoothFlight}). */
	@Override
	public LinearInterpolationHandler createInterpolationHandler() {
		return SmoothFlight.handler(this);
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel server) {
			serverTick(server);
			return;
		}
		SmoothFlight.step(this);
		if (burning()) {
			// The burner's roar: flame and heat shimmer at its jets.
			double yaw = Math.toRadians(-getYRot());
			for (int side = -1; side <= 1; side += 2) {
				Vec3 jet = position().add(new Vec3(side * 0.25, BURNER + 0.5, 0.0).yRot((float) yaw));
				level().addParticle(ParticleTypes.FLAME, jet.x, jet.y, jet.z, 0.0, 0.12, 0.0);
			}
		}
	}

	private void serverTick(ServerLevel level) {
		softLandings();
		Player pilot = pilot();
		if (pilot == null) {
			burnerKey = false;
			ventKey = false;
		}
		boolean firing = burnerKey && fuel > 0;
		float heat = heat();
		if (firing) {
			heat += FIRE;
			fuel--;
			if (tickCount % 8 == 0) {
				level.playSound(null, getX(), getY() + BURNER, getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.35F, 0.55F);
			}
		} else if (burnerKey && pilot instanceof ServerPlayer flier && tickCount % 40 == 0) {
			flier.sendOverlayMessage(Component.translatable("message.jugcraft.balloon.no_fuel"));
		}
		heat -= COOL;
		if (ventKey || pilot == null) {
			heat -= VENT;
		}
		setHeat(heat);
		entityData.set(BURNING, firing);
		fly(level);
		flightLog(level);
		if (pilot instanceof ServerPlayer flier && tickCount % GAUGE_TICKS == 0) {
			gauges(level, flier);
		}
		for (Entity rider : getPassengers()) {
			rider.resetFallDistance();
		}
		resetFallDistance();
	}

	/** One tick of flight: towards the climb its heat gives, with the wind aloft, held by its mooring and kept clear of blocks. */
	private void fly(ServerLevel level) {
		Vec3 motion = getDeltaMovement();
		boolean grounded = grounded();
		// Sat on the ground it isn't sinking: its lift builds from standing still, not from the settling nudge below.
		double from = grounded ? Math.max(motion.y, 0.0) : motion.y;
		double vy = from + (climbFor(heat(), getY(), level.getSeaLevel()) - from) * RESPONSE;
		double vx = motion.x;
		double vz = motion.z;
		if (isInWater()) {
			vy = Math.max(vy, 0.02);
		}
		if (grounded && vy <= 0.0) {
			vx *= 0.5;
			vz *= 0.5;
			vy = -0.01;
		} else {
			Vec3 wind = wind(level);
			vx += (wind.x - vx) * DRIFT;
			vz += (wind.z - vz) * DRIFT;
		}
		BlockPos post = mooring();
		if (post != null) {
			if (tickCount % 20 == 0 && !(level.getBlockState(post).getBlock() instanceof MooringPostBlock)) {
				moor(null);
			} else {
				Vec3 tether = tether(post, getX(), getY(), getZ(), vx, vy, vz);
				vx = tether.x;
				vy = tether.y;
				vz = tether.z;
			}
		}
		Vec3 step = clear(level, new Vec3(vx, vy, vz));
		move(MoverType.SELF, step);
		if (horizontalCollision) {
			step = new Vec3(0.0, step.y, 0.0);
		}
		setDeltaMovement(step);
	}

	/**
	 * A tethered balloon's motion: none of it further out than {@link #ROPE} blocks across from the post or
	 * {@link #TETHER_HEIGHT} above it, and, beyond the rope (tied from further off), the rope pulls it in.
	 */
	public static Vec3 tether(BlockPos post, double x, double y, double z, double vx, double vy, double vz) {
		double px = post.getX() + 0.5;
		double pz = post.getZ() + 0.5;
		double dx = x + vx - px;
		double dz = z + vz - pz;
		double reach = Math.sqrt(dx * dx + dz * dz);
		if (reach > ROPE) {
			double ox = dx / reach;
			double oz = dz / reach;
			double out = vx * ox + vz * oz;
			if (out > 0.0) {
				vx -= out * ox;
				vz -= out * oz;
			}
			double now = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz));
			if (now > ROPE) {
				vx -= ox * 0.05;
				vz -= oz * 0.05;
			}
		}
		double top = post.getY() + 1 + TETHER_HEIGHT;
		if (y + vy > top) {
			vy = Math.min(vy, top - y);
		}
		return new Vec3(vx, vy, vz);
	}

	/** Points on the balloon (from its basket's floor) that mustn't go into a block: riders' heads, the burner, the envelope. */
	private static final double[][] CLEARANCE = clearance();

	private static double[][] clearance() {
		List<double[]> out = new java.util.ArrayList<>();
		for (int i = 0; i < 4; i++) {
			out.add(new double[] {(i % 2 == 0 ? -1 : 1) * STAND, FLOOR + 1.9, (i < 2 ? -1 : 1) * STAND});
		}
		out.add(new double[] {0.0, BURNER + 0.3, 0.0});
		out.add(new double[] {0.0, THROAT + ENVELOPE_HEIGHT - 0.2, 0.0});
		for (int k = 0; k < 8; k++) {
			double a = k * Math.PI / 4;
			out.add(new double[] {Math.cos(a) * ENVELOPE_RADIUS * 0.95, THROAT + ENVELOPE_HEIGHT * 0.62, Math.sin(a) * ENVELOPE_RADIUS * 0.95});
			out.add(new double[] {Math.cos(a) * ENVELOPE_RADIUS * 0.6, THROAT + ENVELOPE_HEIGHT * 0.9, Math.sin(a) * ENVELOPE_RADIUS * 0.6});
		}
		for (int k = 0; k < 4; k++) {
			double a = k * Math.PI / 2;
			out.add(new double[] {Math.cos(a) * 1.6, THROAT + 0.6, Math.sin(a) * 1.6});
		}
		return out.toArray(new double[0][]);
	}

	/** Whether a point on the balloon, moved by {@code offset}, would be in a block it isn't in now. */
	public boolean blocked(Level level, Vec3 offset) {
		for (double[] point : CLEARANCE) {
			Vec3 now = position().add(point[0], point[1], point[2]);
			BlockPos at = BlockPos.containing(now.add(offset));
			if (!at.equals(BlockPos.containing(now)) && !level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** {@code step}, less whatever part of it would push the envelope, burner or riders into a block. */
	private Vec3 clear(Level level, Vec3 step) {
		if (!blocked(level, step)) {
			return step;
		}
		Vec3 upOrDown = new Vec3(0.0, step.y, 0.0);
		if (!blocked(level, upOrDown)) {
			return upOrDown;
		}
		Vec3 across = new Vec3(step.x, Math.min(step.y, 0.0), step.z);
		if (!blocked(level, across)) {
			return across;
		}
		return new Vec3(0.0, Math.min(step.y, 0.0), 0.0);
	}

	/** Takeoffs, landings and heights, for the advancements. */
	private void flightLog(ServerLevel level) {
		boolean aloft = !grounded() && !isInWater();
		if (aloft && !wasAloft) {
			takeoff = position();
			farthest = 0.0;
		}
		if (aloft && takeoff != null) {
			farthest = Math.max(farthest, position().subtract(takeoff).horizontalDistance());
		}
		if (!aloft && wasAloft && takeoff != null) {
			if (boxed(takeoff, position(), farthest)) {
				awardRiders("the_box");
			}
			takeoff = null;
		}
		wasAloft = aloft;
		if (tickCount % 20 != 0 || getPassengers().isEmpty()) {
			return;
		}
		double above = heightAboveGround();
		if (above >= UP_HEIGHT) {
			awardRiders("up_up_and_away");
		}
		if (tickCount % 40 == 0 && above >= ALOFT && crowd(level) >= CROWD) {
			awardRiders("mass_ascension");
		}
	}

	/** Whether a flight from {@code from} that went {@code farthest} blocks away and came down at {@code to} was a box. */
	public static boolean boxed(Vec3 from, Vec3 to, double farthest) {
		return farthest >= BOX_AWAY && to.subtract(from).horizontalDistance() <= BOX_HOME;
	}

	/** How many other ridden balloons are aloft within {@link #CROWD_RANGE} blocks across. */
	public int crowd(ServerLevel level) {
		AABB around = new AABB(getX() - CROWD_RANGE, level.getMinY(), getZ() - CROWD_RANGE, getX() + CROWD_RANGE, level.getMaxY(),
				getZ() + CROWD_RANGE);
		int count = 0;
		for (HotAirBalloon other : level.getEntitiesOfClass(HotAirBalloon.class, around)) {
			if (other != this && other.isVehicle() && other.heightAboveGround() >= ALOFT) {
				count++;
			}
		}
		return count;
	}

	private void awardRiders(String advancement) {
		for (Entity rider : getPassengers()) {
			if (rider instanceof ServerPlayer player) {
				TrickOrTreat.award(player, advancement);
			}
		}
	}

	/** The pilot's gauges, over the hotbar: height, envelope heat, the wind and the burner time left. */
	private void gauges(ServerLevel level, ServerPlayer pilot) {
		int heat = Math.round(heat() * 100.0F);
		if (mooring() != null) {
			pilot.sendOverlayMessage(Component.translatable("message.jugcraft.balloon.moored", heat, Broomstick.seconds(fuel)));
			return;
		}
		Vec3 wind = wind(level);
		pilot.sendOverlayMessage(Component.translatable("message.jugcraft.balloon.gauges", (int) heightAboveGround(), heat,
				FiestaWinds.compass(wind), String.format(Locale.ROOT, "%.1f", wind.horizontalDistance() * 20.0), Broomstick.seconds(fuel)));
	}

	// ---------------------------------------------------------------- an entity apart

	/** A blow from a player packs an empty balloon on the ground back into its item (keeping its fuel). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved() || isVehicle() || !grounded() || !(source.getEntity() instanceof Player player)) {
			return false;
		}
		if (!player.hasInfiniteMaterials()) {
			spawnAtLocation(level, item());
		}
		discard();
		return true;
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	/** The whole balloon, for the client to tell when any of it is in view. */
	public AABB extent() {
		double r = ENVELOPE_RADIUS + 0.5;
		return new AABB(getX() - r, getY(), getZ() - r, getX() + r, getY() + THROAT + ENVELOPE_HEIGHT + 1.5, getZ() + r);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		setKind(Kind.of(input.getIntOr("kind", 0)));
		setHeat(input.getFloatOr("heat", 0.0F));
		setFuel(input.getIntOr("fuel", 0));
		moor(input.read("mooring", BlockPos.CODEC).orElse(null));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("kind", kind().ordinal());
		output.putFloat("heat", heat());
		output.putInt("fuel", fuel);
		BlockPos post = mooring();
		if (post != null) {
			output.store("mooring", BlockPos.CODEC, post);
		}
	}
}
