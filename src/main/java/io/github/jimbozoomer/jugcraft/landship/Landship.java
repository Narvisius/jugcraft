package io.github.jimbozoomer.jugcraft.landship;

import java.util.List;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Landship (batch 49, docs/features/landship.md): a rideable crawler tank for three, driven by the server from the
 * driver's keys ({@link LandshipInputPayload}). Forward and back drive it, left and right turn it (on the spot too),
 * and it climbs one-block steps by itself; its long hull bridges narrow trenches. Attack fires the turret's cannon
 * where the driver looks, using a cannon shell from their inventory (none in creative); holding use fires the sponson
 * guns. Shells burst in a damage-only blast and never break a block. Driving burns {@value #FUEL_PER_SECOND} mB of
 * diesel or kerosene a second; use a fuel bucket on it to refuel. Players knock it down into an item.
 */
public class Landship extends Entity {
	public static final double SPEED = 0.15;
	public static final float TURN = 2.5F;
	public static final int CANNON_COOLDOWN = 40;
	public static final double SHELL_SPEED = 2.5;
	public static final double SHELL_RADIUS = 3.5;
	public static final float SHELL_DAMAGE = 22F;
	public static final int GUN_INTERVAL = 10;
	public static final float GUN_DAMAGE = 4F;
	public static final int GUN_RANGE = 24;
	public static final float CRUSH_DAMAGE = 4F;
	public static final int FUEL_TANK = 6000;
	public static final int FUEL_PER_BUCKET = 1000;
	public static final int FUEL_PER_SECOND = 5;
	public static final int HEALTH = 120;
	public static final int SEATS = 3;
	public static final float WIDTH = 3.5F;
	public static final float HEIGHT = 2.75F;
	/** The cannon's elevation limits, in degrees (negative is up). */
	public static final float BARREL_UP = -25.0F;
	public static final float BARREL_DOWN = 10.0F;
	/** Half the distance between the tracks' centres, in blocks (for the tracks' animation). */
	public static final float TRACK_HALF_SPAN = 1.42F;
	public static final List<String> FUELS = List.of("diesel_bucket", "premium_diesel_bucket", "kerosene_bucket");
	private static final int INPUT_TIMEOUT = 10;
	/** Where the riders sit, in blocks: the driver in the turret hatch, two more on the engine deck. */
	private static final Vec3[] SEAT_SPOTS = {new Vec3(0, 1.9, -0.1), new Vec3(0.55, 1.7, -1.6), new Vec3(-0.55, 1.7, -1.6)};
	/** The cannon's muzzle height above the ground, and the sponson guns' muzzles (x, y, z), in blocks. */
	private static final double MUZZLE_HEIGHT = 2.6;
	private static final Vec3[] GUN_MUZZLES = {new Vec3(1.94, 0.94, 1.56), new Vec3(-1.94, 0.94, 1.56)};
	private static final EntityDataAccessor<Integer> FIRED_AT = SynchedEntityData.defineId(Landship.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> GUNS = SynchedEntityData.defineId(Landship.class, EntityDataSerializers.BOOLEAN);

	private int fuel;
	private int forward;
	private int turn;
	private int cannon;
	private int guns;
	private int inputTick = -INPUT_TIMEOUT - 1;
	private int lastShot = -CANNON_COOLDOWN;
	private boolean cannonHeld;
	private float damage;
	/** Client side: how far each track has run (blocks; the left track is on the +x side) and the tick of the last shot. */
	private float leftTrack;
	private float rightTrack;
	private int shotAt = -100;

	public Landship(EntityType<? extends Landship> type, Level level) {
		super(type, level);
	}

	public int fuel() {
		return fuel;
	}

	public void setFuel(int fuel) {
		this.fuel = Mth.clamp(fuel, 0, FUEL_TANK);
	}

	/** Whether the sponson guns are firing, for the renderer's muzzle flashes. */
	public boolean gunsFiring() {
		return entityData.get(GUNS);
	}

	/** Client side: how far the left (+x) and right tracks have run, in blocks. */
	public float leftTrack() {
		return leftTrack;
	}

	public float rightTrack() {
		return rightTrack;
	}

	/** Client side: ticks since the cannon last fired. */
	public int sinceShot() {
		return tickCount - shotAt;
	}

	/** The driver's keys; anyone else's are ignored. */
	public void steer(ServerPlayer player, int forward, int turn, int cannon, int guns) {
		if (getFirstPassenger() != player) {
			return;
		}
		this.forward = Mth.clamp(forward, -1, 1);
		this.turn = Mth.clamp(turn, -1, 1);
		this.cannon = Mth.clamp(cannon, 0, 1);
		this.guns = Mth.clamp(guns, 0, 1);
		inputTick = tickCount;
	}

	/** Use: refuel from a bucket, else climb aboard. */
	public InteractionResult use(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		Identifier id = BuiltInRegistries.ITEM.getKey(held.getItem());
		if (id.getNamespace().equals("jugcraft") && FUELS.contains(id.getPath())) {
			if (!level().isClientSide()) {
				if (fuel + FUEL_PER_BUCKET > FUEL_TANK) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.landship.full"));
				} else {
					fuel += FUEL_PER_BUCKET;
					if (!player.getAbilities().instabuild) {
						player.setItemInHand(hand, new ItemStack(Items.BUCKET));
					}
					level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 0.8F);
					player.sendOverlayMessage(Component.translatable("message.jugcraft.landship.refuelled", fuel * 100 / FUEL_TANK));
				}
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

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel level) {
			drive(level);
			return;
		}
		// Client side: run the tracks by how far each side moved, and puff smoke from the stacks while it works.
		double yaw = Math.toRadians(getYRot());
		double moved = (getX() - xo) * -Math.sin(yaw) + (getZ() - zo) * Math.cos(yaw);
		float turned = (float) Math.toRadians(Mth.wrapDegrees(getYRot() - yRotO)) * TRACK_HALF_SPAN;
		leftTrack += (float) moved + turned;
		rightTrack += (float) moved - turned;
		if (Math.abs(moved) + Math.abs(turned) > 0.01 && tickCount % 3 == 0) {
			for (int side = -1; side <= 1; side += 2) {
				double x = getX() + 0.5 * side * Math.cos(yaw) + 1.7 * Math.sin(yaw);
				double z = getZ() + 0.5 * side * Math.sin(yaw) - 1.7 * Math.cos(yaw);
				level().addParticle(ParticleTypes.LARGE_SMOKE, x, getY() + 2.3, z, 0, 0.04, 0);
			}
		}
	}

	private void drive(ServerLevel level) {
		Entity driver = getFirstPassenger();
		boolean listening = driver instanceof ServerPlayer && tickCount - inputTick <= INPUT_TIMEOUT;
		int f = listening ? forward : 0;
		int t = listening ? turn : 0;
		boolean fuelled = fuel > 0;
		if (fuelled && t != 0) {
			setYRot(getYRot() - t * TURN);
		}
		double yaw = Math.toRadians(getYRot());
		Vec3 heading = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		double speed = !fuelled ? 0 : f > 0 ? SPEED : f < 0 ? -SPEED / 2 : 0;
		Vec3 motion = getDeltaMovement();
		double vy = motion.y - 0.08;
		if (onGround()) {
			vy = Math.max(vy, -0.1);
			if (speed != 0 && horizontalCollision) {
				vy = 0.45; // Climb a one-block step.
			}
		}
		setDeltaMovement(heading.x * speed, vy * 0.98, heading.z * speed);
		move(MoverType.SELF, getDeltaMovement());
		resetFallDistance();
		for (Entity passenger : getPassengers()) {
			passenger.resetFallDistance();
		}

		if (driver instanceof ServerPlayer player) {
			if (speed > 0) {
				crush(level, player, heading);
			}
			boolean pressed = listening && cannon > 0;
			if (pressed && !cannonHeld && tickCount - lastShot >= CANNON_COOLDOWN) {
				fireCannon(level, player);
			}
			cannonHeld = pressed;
			boolean firing = listening && guns > 0;
			if (firing && tickCount % GUN_INTERVAL == 0) {
				fireGuns(level, player);
			}
			entityData.set(GUNS, firing);
		} else {
			cannonHeld = false;
			entityData.set(GUNS, false);
		}

		boolean working = speed != 0 || t != 0;
		if (working && fuelled && tickCount % (20 / FUEL_PER_SECOND) == 0) {
			fuel--;
			if (fuel == 0 && driver instanceof Player player) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.landship.empty"));
			}
		}
		if (working && tickCount % 8 == 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, 0.6F, 0.5F);
		}
		if (driver instanceof Player player && tickCount % 40 == 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.landship.fuel", fuel * 100 / FUEL_TANK));
		}
		if (damage > 0 && tickCount % 10 == 0) {
			damage = Math.max(0, damage - 1);
		}
	}

	/** Anything living the hull drives into is hurt and shoved aside, as if the driver struck it. */
	private void crush(ServerLevel level, ServerPlayer driver, Vec3 heading) {
		Vec3 front = position().add(heading.scale(WIDTH / 2 + 0.6));
		AABB box = new AABB(front.x - 1.6, getY(), front.z - 1.6, front.x + 1.6, getY() + 1.5, front.z + 1.6);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e.isAlive() && e.getRootVehicle() != this)) {
			if (target.hurtServer(level, level.damageSources().playerAttack(driver), CRUSH_DAMAGE)) {
				shove(target, target.getDeltaMovement().add(heading.x * 0.8, 0.3, heading.z * 0.8));
			}
		}
	}

	/** The direction the cannon points: where the driver looks, within the barrel's elevation limits. */
	public static Vec3 aim(Entity driver) {
		double yaw = Math.toRadians(driver.getYRot());
		double pitch = Math.toRadians(Mth.clamp(driver.getXRot(), BARREL_UP, BARREL_DOWN));
		return new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
	}

	private void fireCannon(ServerLevel level, ServerPlayer driver) {
		if (!driver.getAbilities().instabuild && !takeShell(driver.getInventory())) {
			driver.sendOverlayMessage(Component.translatable("message.jugcraft.landship.no_shells"));
			return;
		}
		lastShot = tickCount;
		entityData.set(FIRED_AT, tickCount);
		Vec3 aim = aim(driver);
		Vec3 muzzle = position().add(0, MUZZLE_HEIGHT, 0).add(aim.scale(2.6));
		LandshipShell shell = new LandshipShell(level, driver);
		shell.setPos(muzzle);
		shell.shoot(aim.x, aim.y, aim.z, (float) SHELL_SPEED, 0.5F);
		level.addFreshEntity(shell);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 8, 0.2, 0.2, 0.2, 0.02);
		level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 4, 0.1, 0.1, 0.1, 0.01);
		level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5F, 1.6F);
	}

	/** Takes one cannon shell from the inventory; false if there is none. */
	private static boolean takeShell(Inventory inventory) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(JugcraftLandships.CANNON_SHELL)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	/** One burst of the sponson guns: a hit on the first living thing along the driver's aim, as the driver's attack. */
	private void fireGuns(ServerLevel level, ServerPlayer driver) {
		Vec3 aim = aim(driver);
		Vec3 from = driver.getEyePosition();
		Vec3 to = from.add(aim.scale(GUN_RANGE));
		var blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, driver));
		to = blockHit.getLocation();
		LivingEntity hit = null;
		double nearest = Double.MAX_VALUE;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.0),
				e -> e.isAlive() && e.getRootVehicle() != this)) {
			var clip = target.getBoundingBox().inflate(0.3).clip(from, to);
			if (clip.isPresent() && clip.get().distanceToSqr(from) < nearest) {
				nearest = clip.get().distanceToSqr(from);
				hit = target;
			}
		}
		Vec3 impact = hit != null ? from.add(aim.scale(Math.sqrt(nearest))) : to;
		if (hit != null) {
			hit.hurtServer(level, level.damageSources().playerAttack(driver), GUN_DAMAGE);
		}
		level.sendParticles(ParticleTypes.CRIT, impact.x, impact.y, impact.z, 4, 0.1, 0.1, 0.1, 0.1);
		double yaw = Math.toRadians(getYRot());
		for (Vec3 spot : GUN_MUZZLES) {
			Vec3 muzzle = position().add(spot.yRot((float) -yaw));
			level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 1, 0.05, 0.05, 0.05, 0.01);
		}
		level.playSound(null, getX(), getY() + 1, getZ(), SoundEvents.DISPENSER_LAUNCH, SoundSource.PLAYERS, 0.8F, 1.9F);
	}

	/** Sets an entity's motion; a player's client is told, since a player moves themselves. */
	private static void shove(Entity entity, Vec3 motion) {
		entity.setDeltaMovement(motion);
		if (entity instanceof ServerPlayer player) {
			player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (FIRED_AT.equals(key) && level().isClientSide() && entityData.get(FIRED_AT) >= 0) {
			shotAt = tickCount;
		}
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().size() < SEATS;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		int seat = Math.max(0, getPassengers().indexOf(passenger));
		return SEAT_SPOTS[Math.min(seat, SEAT_SPOTS.length - 1)].yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved() || !(source.getEntity() instanceof Player player) || player.getRootVehicle() == this) {
			return false;
		}
		if (player.getAbilities().instabuild) {
			discard();
			return true;
		}
		damage += amount;
		if (damage >= HEALTH) {
			spawnAtLocation(level, new ItemStack(JugcraftLandships.LANDSHIP_ITEM));
			discard();
		}
		return true;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(FIRED_AT, -100);
		builder.define(GUNS, false);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		fuel = Mth.clamp(input.getInt("fuel").orElse(0), 0, FUEL_TANK);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("fuel", fuel);
	}
}
