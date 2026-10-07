package io.github.jimbozoomer.jugcraft.airship;

import java.util.List;
import net.minecraft.core.NonNullList;
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
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The zeppelin (batch 46, docs/features/zeppelin.md): a rigid airship for {@value #SEATS}, lifted by its hydrogen
 * cells and pushed by two diesel engines. The server flies it from the pilot's keys ({@link ZeppelinInputPayload}):
 * forward and back, turn, jump to climb and sprint to sink. The engines burn {@value #FUEL_PER_SECOND} mB of fuel a
 * second while they work; an airship left alone hovers where it is, and one with an empty tank sinks gently to the
 * ground. Use a diesel or kerosene bucket on it to refuel, sneak-use it to open its {@value #CARGO_SLOTS}-slot cargo
 * hold, use it to climb aboard. Players knock it down into an item, dropping the cargo; nothing else can.
 */
public class Zeppelin extends Entity {
	public static final double MAX_SPEED = 0.35;
	public static final double ACCELERATION = 0.01;
	public static final double CLIMB = 0.12;
	public static final float TURN = 1.5F;
	public static final double DRIFT_SINK = 0.03;
	public static final int FUEL_TANK = 8000;
	public static final int FUEL_PER_BUCKET = 1000;
	public static final int FUEL_PER_SECOND = 5;
	public static final int SEATS = 4;
	public static final int CARGO_SLOTS = 27;
	public static final int HEALTH = 40;
	public static final float WIDTH = 5.0F;
	public static final float HEIGHT = 7.5F;
	/** Buckets that fuel it (jugcraft items), emptied into the tank. */
	public static final List<String> FUELS = List.of("diesel_bucket", "premium_diesel_bucket", "kerosene_bucket");
	/** Ticks after the pilot's last input before the airship stops answering to it (a lost connection, say). */
	private static final int INPUT_TIMEOUT = 10;
	/** Where riders sit, in blocks, facing forward (+z): the pilot at the wheel, then down the deck. */
	private static final Vec3[] SEAT_SPOTS = {new Vec3(0, 0.35, 1.3), new Vec3(-0.45, 0.35, 0.4), new Vec3(0.45, 0.35, 0.4),
			new Vec3(0, 0.35, -0.6)};
	private static final EntityDataAccessor<Boolean> ENGINE = SynchedEntityData.defineId(Zeppelin.class, EntityDataSerializers.BOOLEAN);

	private final SimpleContainer cargo = new SimpleContainer(CARGO_SLOTS) {
		@Override
		public boolean stillValid(Player player) {
			return !isRemoved() && player.distanceToSqr(Zeppelin.this) < 100;
		}
	};
	private int fuel;
	private int forward;
	private int turn;
	private int vertical;
	private int inputTick = -INPUT_TIMEOUT - 1;
	private float damage;

	public Zeppelin(EntityType<? extends Zeppelin> type, Level level) {
		super(type, level);
	}

	public int fuel() {
		return fuel;
	}

	/** The cargo hold. */
	public SimpleContainer cargo() {
		return cargo;
	}

	public boolean engineOn() {
		return entityData.get(ENGINE);
	}

	/** The pilot's keys, from {@link ZeppelinInputPayload}; anyone else's are ignored. */
	public void steer(ServerPlayer player, int forward, int turn, int vertical) {
		if (getFirstPassenger() != player) {
			return;
		}
		this.forward = Mth.clamp(forward, -1, 1);
		this.turn = Mth.clamp(turn, -1, 1);
		this.vertical = Mth.clamp(vertical, -1, 1);
		inputTick = tickCount;
	}

	/** Use: refuel from a bucket, sneak for the cargo hold, else climb aboard. */
	public InteractionResult use(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		Identifier id = BuiltInRegistries.ITEM.getKey(held.getItem());
		if (id.getNamespace().equals("jugcraft") && FUELS.contains(id.getPath())) {
			if (!level().isClientSide()) {
				if (fuel + FUEL_PER_BUCKET > FUEL_TANK) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.zeppelin.full"));
				} else {
					fuel += FUEL_PER_BUCKET;
					if (!player.getAbilities().instabuild) {
						player.setItemInHand(hand, new ItemStack(Items.BUCKET));
					}
					level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 0.8F);
					player.sendOverlayMessage(Component.translatable("message.jugcraft.zeppelin.refuelled", fuelPercent()));
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) {
			if (!level().isClientSide()) {
				player.openMenu(new SimpleMenuProvider((id2, inventory, p) -> ChestMenu.threeRows(id2, inventory, cargo),
						Component.translatable("container.jugcraft.zeppelin")));
			}
			return InteractionResult.SUCCESS;
		}
		if (player.isPassenger() || getPassengers().size() >= SEATS) {
			return InteractionResult.PASS;
		}
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		return player.startRiding(this) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	private int fuelPercent() {
		return fuel * 100 / FUEL_TANK;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel level) {
			fly(level);
		} else if (engineOn() && tickCount % 3 == 0) {
			// Exhaust smoke from both engine pods.
			double yaw = Math.toRadians(getYRot());
			for (int side = -1; side <= 1; side += 2) {
				double lx = 2.125 * side;
				double lz = -0.125;
				double x = getX() + lx * Math.cos(yaw) - lz * Math.sin(yaw);
				double z = getZ() + lx * Math.sin(yaw) + lz * Math.cos(yaw);
				level().addParticle(ParticleTypes.LARGE_SMOKE, x, getY() + 2.4, z, 0, 0.05, 0);
			}
		}
	}

	private void fly(ServerLevel level) {
		Entity pilot = getFirstPassenger();
		boolean listening = pilot instanceof Player && tickCount - inputTick <= INPUT_TIMEOUT;
		int f = listening ? forward : 0;
		int t = listening ? turn : 0;
		int v = listening ? vertical : 0;
		boolean engine = fuel > 0 && (f != 0 || t != 0 || v != 0);
		if (engine && tickCount % (20 / FUEL_PER_SECOND) == 0) {
			fuel--;
			if (fuel == 0 && pilot instanceof Player player) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.zeppelin.empty"));
			}
		}
		if (engine && t != 0) {
			setYRot(getYRot() - t * TURN);
		}
		double yaw = Math.toRadians(getYRot());
		Vec3 heading = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 motion = getDeltaMovement();
		double speed = motion.x * heading.x + motion.z * heading.z;
		double target = !engine ? 0 : f > 0 ? MAX_SPEED : f < 0 ? -MAX_SPEED / 2 : 0;
		speed += Mth.clamp(target - speed, -ACCELERATION, ACCELERATION);
		double vy;
		if (engine && v != 0) {
			vy = v * CLIMB;
		} else if (fuel <= 0 && !onGround()) {
			vy = -DRIFT_SINK;
		} else {
			vy = motion.y * 0.8;
		}
		setDeltaMovement(heading.x * speed, vy, heading.z * speed);
		move(MoverType.SELF, getDeltaMovement());
		if (horizontalCollision) {
			setDeltaMovement(0, getDeltaMovement().y, 0);
		}
		entityData.set(ENGINE, engine);
		for (Entity passenger : getPassengers()) {
			passenger.resetFallDistance();
		}
		if (engine && tickCount % 40 == 0) {
			level.playSound(null, getX(), getY() + 2, getZ(), SoundEvents.BEE_LOOP, SoundSource.NEUTRAL, 1.2F, 0.4F);
		}
		if (pilot instanceof Player player && tickCount % 40 == 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.zeppelin.fuel", fuelPercent()));
		}
		if (damage > 0 && tickCount % 10 == 0) {
			damage = Math.max(0, damage - 1);
		}
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().size() < SEATS;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		int index = Math.max(0, Math.min(SEAT_SPOTS.length - 1, getPassengers().indexOf(passenger)));
		return SEAT_SPOTS[index].yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved() || !(source.getEntity() instanceof Player player)) {
			return false;
		}
		if (player.getAbilities().instabuild) {
			Containers.dropContents(level, this, cargo);
			discard();
			return true;
		}
		damage += amount;
		if (damage >= HEALTH) {
			spawnAtLocation(level, new ItemStack(JugcraftAirships.ZEPPELIN_ITEM));
			Containers.dropContents(level, this, cargo);
			discard();
		}
		return true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(ENGINE, false);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		fuel = Mth.clamp(input.getInt("fuel").orElse(0), 0, FUEL_TANK);
		NonNullList<ItemStack> items = NonNullList.withSize(CARGO_SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		for (int i = 0; i < CARGO_SLOTS; i++) {
			cargo.setItem(i, items.get(i));
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("fuel", fuel);
		NonNullList<ItemStack> items = NonNullList.withSize(CARGO_SLOTS, ItemStack.EMPTY);
		for (int i = 0; i < CARGO_SLOTS; i++) {
			items.set(i, cargo.getItem(i));
		}
		ContainerHelper.saveAllItems(output, items);
	}

	/** For game tests: fill the tank. */
	public void setFuel(int fuel) {
		this.fuel = Mth.clamp(fuel, 0, FUEL_TANK);
	}
}
