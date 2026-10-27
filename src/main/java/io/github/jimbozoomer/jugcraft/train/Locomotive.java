package io.github.jimbozoomer.jugcraft.train;

import io.github.jimbozoomer.jugcraft.landship.Landship;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The diesel locomotive (docs/features/trains.md): it pulls (or pushes) the train behind it along the line. Its driver
 * sits in the cab at the back and works the throttle with the movement keys: forwards notches it up to
 * {@value #NOTCHES}, back down through idle to {@value #REVERSE_NOTCHES} notches of reverse, each notch
 * {@value #NOTCH_SPEED} blocks a tick; jump brakes. It picks up speed and slows gradually, coasts to a stand with the
 * throttle at idle, and stops at the end of the line, or at another train. Working burns {@value #FUEL_PER_NOTCH_SECOND}
 * mB of diesel or kerosene a second for each notch; use a fuel bucket on it to refuel. Up to {@value #MAX_CARS} cars
 * couple behind it (the Coupler).
 */
public class Locomotive extends TrainCar {
	public static final double LENGTH = 4.5;
	public static final float WIDTH = 1.4F;
	public static final float HEIGHT = 2.75F;
	public static final int NOTCHES = 4;
	public static final int REVERSE_NOTCHES = 2;
	public static final double NOTCH_SPEED = 0.08;
	public static final double ACCELERATION = 0.004;
	public static final double BRAKING = 0.02;
	public static final double COASTING = 0.002;
	public static final int FUEL_TANK = 8000;
	public static final int FUEL_PER_BUCKET = 1000;
	public static final int FUEL_PER_NOTCH_SECOND = 2;
	public static final int MAX_CARS = 8;
	public static final int SEATS = 2;
	public static final float HEALTH = 80.0F;
	/** How near the end of the train a car must stand to couple, in blocks. */
	public static final double COUPLE_REACH = 2.5;
	private static final int INPUT_TIMEOUT = 10;
	/** Ticks between throttle notches while the key is held. */
	private static final int NOTCH_DELAY = 6;
	/** Ticks a coupled car may be missing (unloaded or gone) before the train leaves it behind. */
	private static final int MISSING_LIMIT = 200;
	/** The cab's two seats, in blocks (x across, y up, z forwards): the cab is at the back. */
	private static final Vec3[] SEAT_SPOTS = {new Vec3(0.3, 1.05, -1.35), new Vec3(-0.3, 1.05, -1.35)};
	/** The exhaust stack's top, in blocks from the middle, where the smoke rises. */
	private static final Vec3 STACK = new Vec3(0, 2.6, 0.625);
	private static final EntityDataAccessor<Integer> NOTCH = SynchedEntityData.defineId(Locomotive.class, EntityDataSerializers.INT);

	private final List<UUID> cars = new ArrayList<>();
	private double speed;
	private int fuel;
	private int burnTicks;
	private int missingTicks;
	private int throttleInput;
	private boolean brakeInput;
	private int inputTick = -INPUT_TIMEOUT - 1;
	private int lastNotchTick = -NOTCH_DELAY;

	public Locomotive(EntityType<? extends Locomotive> type, Level level) {
		super(type, level);
	}

	@Override
	public double length() {
		return LENGTH;
	}

	@Override
	public ItemStack asItem() {
		return new ItemStack(JugcraftTrains.LOCOMOTIVE_ITEM);
	}

	@Override
	public int seats() {
		return SEATS;
	}

	@Override
	protected Vec3 seat(int index) {
		return SEAT_SPOTS[index];
	}

	@Override
	protected float health() {
		return HEALTH;
	}

	public int notch() {
		return entityData.get(NOTCH);
	}

	/** Signed speed along the line, in blocks a tick (forwards positive). */
	public double speed() {
		return speed;
	}

	public int fuel() {
		return fuel;
	}

	public void setFuel(int fuel) {
		this.fuel = Mth.clamp(fuel, 0, FUEL_TANK);
	}

	/** For game tests: set the throttle. */
	public void setNotch(int notch) {
		entityData.set(NOTCH, Mth.clamp(notch, -REVERSE_NOTCHES, NOTCHES));
	}

	/** The cars coupled behind it, in order. */
	public List<UUID> cars() {
		return List.copyOf(cars);
	}

	/** The driver's keys (throttle -1, 0 or 1; brake); anyone else's are ignored. */
	public void steer(ServerPlayer player, int throttle, boolean brake) {
		if (getFirstPassenger() != player) {
			return;
		}
		throttleInput = Mth.clamp(throttle, -1, 1);
		brakeInput = brake;
		inputTick = tickCount;
	}

	/** Use: refuel from a bucket, else climb into the cab. */
	@Override
	public InteractionResult use(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		Identifier id = BuiltInRegistries.ITEM.getKey(held.getItem());
		if (id.getNamespace().equals("jugcraft") && Landship.FUELS.contains(id.getPath())) {
			if (!level().isClientSide()) {
				if (fuel + FUEL_PER_BUCKET > FUEL_TANK) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.train.full"));
				} else {
					fuel += FUEL_PER_BUCKET;
					if (!player.getAbilities().instabuild) {
						player.setItemInHand(hand, new ItemStack(Items.BUCKET));
					}
					level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 0.8F);
					player.sendOverlayMessage(Component.translatable("message.jugcraft.train.refuelled", fuel * 100 / FUEL_TANK));
				}
			}
			return InteractionResult.SUCCESS;
		}
		return super.use(player, hand);
	}

	@Override
	protected void serverTick(ServerLevel level) {
		if (track == null) {
			return;
		}
		Entity driver = getFirstPassenger();
		boolean listening = driver instanceof ServerPlayer && tickCount - inputTick <= INPUT_TIMEOUT;
		int notch = notch();
		if (listening && throttleInput != 0 && tickCount - lastNotchTick >= NOTCH_DELAY) {
			notch = Mth.clamp(notch + throttleInput, -REVERSE_NOTCHES, NOTCHES);
			lastNotchTick = tickCount;
		}
		boolean braking = listening && brakeInput;
		if (braking) {
			notch = 0;
		}
		double target = fuel > 0 ? notch * NOTCH_SPEED : 0;
		double rate = braking ? BRAKING : target == 0 ? COASTING : ACCELERATION;
		if (speed != 0 && target != 0 && Math.signum(target) != Math.signum(speed)) {
			rate = BRAKING; // Reversing: brake to a stand first.
		}
		speed = speed < target ? Math.min(target, speed + rate) : Math.max(target, speed - rate);
		if (Math.abs(speed) > 1.0E-4) {
			double moved = run(level, speed);
			if (Math.abs(moved) < Math.abs(speed) - 1.0E-6) {
				speed = 0;
				notch = 0;
				if (driver instanceof Player player) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.train.stopped"));
				}
			}
		} else {
			speed = 0;
		}
		entityData.set(NOTCH, notch);
		if (notch != 0 && fuel > 0 && ++burnTicks >= 20) {
			burnTicks = 0;
			fuel = Math.max(0, fuel - FUEL_PER_NOTCH_SECOND * Math.abs(notch));
			if (fuel == 0 && driver instanceof Player player) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.train.empty"));
			}
		}
		if (driver instanceof Player player && (tickCount % 40 == 0 || speed != 0 && tickCount % 10 == 0)) {
			player.sendOverlayMessage(status());
		}
		if (Math.abs(speed) > 0.02 && tickCount % 10 == 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, 0.5F, 0.6F);
		}
	}

	/** The cab's readout: the throttle, the speed (blocks a second) and the fuel. */
	public Component status() {
		int notch = notch();
		Component throttle = notch == 0 ? Component.translatable("message.jugcraft.train.notch.idle")
				: Component.translatable(notch > 0 ? "message.jugcraft.train.notch.forward" : "message.jugcraft.train.notch.reverse", Math.abs(notch));
		return Component.translatable("message.jugcraft.train.status", throttle, String.format("%.1f", Math.abs(speed) * 20), fuel * 100 / FUEL_TANK);
	}

	/**
	 * Moves the whole train {@code distance} blocks along the line (backwards if negative), as far as its leading end can
	 * go: the end of the line, or another train, stops it. Returns how far it went.
	 */
	public double run(ServerLevel level, double distance) {
		List<TrainCar> train = train(level);
		if (train == null || distance == 0) {
			return 0;
		}
		TrainCar lead = distance > 0 ? this : train.getLast();
		TrackPoint.Move end = lead.ahead(distance > 0 ? lead.length() / 2 : -lead.length() / 2);
		if (end.blocked()) {
			return 0;
		}
		TrackPoint.Move tip = end.point().move(level, distance);
		double allowed = tip.moved();
		if (allowed != 0 && obstructed(level, train, tip.point().position(level))) {
			allowed = 0;
		}
		if (allowed == 0) {
			return 0;
		}
		for (TrainCar car : train) {
			car.place(car.ahead(allowed).point());
		}
		return allowed;
	}

	/** Whether another train's car (or one not coupled) stands where this train's leading end is going. */
	private static boolean obstructed(ServerLevel level, List<TrainCar> train, Vec3 tip) {
		AABB near = new AABB(tip, tip).inflate(3.0, 1.5, 3.0);
		for (TrainCar car : level.getEntitiesOfClass(TrainCar.class, near, car -> !train.contains(car) && car.isAlive())) {
			if (car.position().distanceTo(tip) < car.length() / 2) {
				return true;
			}
		}
		return false;
	}

	/**
	 * The locomotive and its cars in order, or null while one of them is not loaded (the train waits for it; after
	 * {@value #MISSING_LIMIT} ticks it leaves the missing car, and those behind it, uncoupled).
	 */
	public @Nullable List<TrainCar> train(ServerLevel level) {
		List<TrainCar> train = new ArrayList<>();
		train.add(this);
		for (int i = 0; i < cars.size(); i++) {
			if (level.getEntity(cars.get(i)) instanceof TrainCar car && car.track() != null) {
				train.add(car);
				continue;
			}
			if (++missingTicks > MISSING_LIMIT) {
				missingTicks = 0;
				detachFrom(level, i);
				return train;
			}
			return null;
		}
		missingTicks = 0;
		return train;
	}

	/**
	 * Couples {@code car} behind the train's last car, if it stands on the line just behind it (within
	 * {@value #COUPLE_REACH} blocks of its place): it is set exactly at the coupling distance. False if it cannot.
	 */
	public boolean couple(ServerLevel level, TrainCar car) {
		if (car == this || car instanceof Locomotive || car.coupledTo() != null || track == null || car.track() == null
				|| cars.size() >= MAX_CARS) {
			return false;
		}
		List<TrainCar> train = train(level);
		if (train == null) {
			return false;
		}
		TrainCar last = train.getLast();
		TrackPoint.Move behind = last.ahead(-(last.length() / 2 + COUPLING_GAP + car.length() / 2));
		if (behind.blocked() || car.position().distanceTo(behind.point().position(level)) > COUPLE_REACH) {
			return false;
		}
		car.place(behind.point());
		car.coupleTo(getUUID());
		cars.add(car.getUUID());
		return true;
	}

	/** Uncouples {@code car} and every car behind it, leaving them where they stand. */
	public void uncouple(TrainCar car) {
		int index = cars.indexOf(car.getUUID());
		if (index >= 0 && level() instanceof ServerLevel level) {
			detachFrom(level, index);
		}
	}

	/** Uncouples every car. */
	public void uncoupleAll() {
		if (level() instanceof ServerLevel level) {
			detachFrom(level, 0);
		}
	}

	private void detachFrom(ServerLevel level, int index) {
		for (UUID id : cars.subList(index, cars.size())) {
			if (level.getEntity(id) instanceof TrainCar car) {
				car.coupleTo(null);
			}
		}
		cars.subList(index, cars.size()).clear();
	}

	@Override
	protected void clientTick() {
		if (notch() != 0 && tickCount % 4 == 0) {
			Vec3 stack = position().add(STACK.yRot(-getYRot() * Mth.DEG_TO_RAD));
			level().addParticle(ParticleTypes.LARGE_SMOKE, stack.x, stack.y, stack.z, 0, 0.06, 0);
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		if (reason.shouldDestroy()) {
			uncoupleAll();
		}
		super.remove(reason);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(NOTCH, 0);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		cars.clear();
		cars.addAll(input.read("cars", UUIDUtil.CODEC.listOf()).orElse(List.of()));
		speed = input.getDoubleOr("speed", 0.0);
		fuel = Mth.clamp(input.getIntOr("fuel", 0), 0, FUEL_TANK);
		entityData.set(NOTCH, Mth.clamp(input.getIntOr("notch", 0), -REVERSE_NOTCHES, NOTCHES));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("cars", UUIDUtil.CODEC.listOf(), List.copyOf(cars));
		output.putDouble("speed", speed);
		output.putInt("fuel", fuel);
		output.putInt("notch", notch());
	}
}
