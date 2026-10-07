package io.github.jimbozoomer.jugcraft.walker;

import java.util.List;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Diesel Walker (batch 47, docs/features/diesel-walker.md): a rideable mech for one pilot, driven by the server
 * from the pilot's keys ({@link WalkerInputPayload}). Forward and back walk it, left and right turn it, jump makes it
 * hop, and it climbs one-block steps by itself. Holding use drills the block the pilot looks at, up to
 * {@value #DRILL_REACH} blocks away, one block at a time, taking longer for harder blocks: the block breaks as if
 * the pilot broke it by hand (it drops, and protected land and other mods' break checks apply). Pressing attack
 * swings the fist at whatever stands in front. Walking and drilling burn {@value #FUEL_PER_SECOND} mB of diesel or
 * kerosene a second; with an empty tank it only stands. Use a fuel bucket on it to refuel; players knock it down
 * into an item.
 */
public class DieselWalker extends Entity {
	public static final double WALK_SPEED = 0.2;
	public static final float TURN = 4.0F;
	public static final double JUMP = 0.6;
	public static final double DRILL_REACH = 5.0;
	public static final int DRILL_TICKS_PER_HARDNESS = 5;
	public static final int DRILL_MIN_TICKS = 3;
	public static final int DRILL_MAX_HARDNESS = 50;
	public static final int PUNCH_DAMAGE = 12;
	public static final double PUNCH_KNOCKBACK = 1.0;
	public static final int PUNCH_COOLDOWN = 16;
	public static final int FUEL_TANK = 4000;
	public static final int FUEL_PER_BUCKET = 1000;
	public static final int FUEL_PER_SECOND = 4;
	public static final int HEALTH = 60;
	public static final float WIDTH = 2.5F;
	public static final float HEIGHT = 4.25F;
	public static final List<String> FUELS = List.of("diesel_bucket", "premium_diesel_bucket", "kerosene_bucket");
	private static final int INPUT_TIMEOUT = 10;
	/** Where the pilot sits, in blocks: on the cockpit floor in the chest. */
	private static final Vec3 SEAT = new Vec3(0, 1.9, 0);
	private static final EntityDataAccessor<Boolean> DRILLING = SynchedEntityData.defineId(DieselWalker.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> PUNCHED_AT = SynchedEntityData.defineId(DieselWalker.class, EntityDataSerializers.INT);

	private int fuel;
	private int forward;
	private int turn;
	private int jump;
	private int drill;
	private int punch;
	private int inputTick = -INPUT_TIMEOUT - 1;
	private int lastPunch = -1000;
	private boolean punchHeld;
	private BlockPos drillTarget;
	private int drillProgress;
	private float damage;
	/** Client side: how far it has walked (for the legs' swing) and the tick its fist last swung (for the arm). */
	private float stride;
	private int swungAt = -100;

	public DieselWalker(EntityType<? extends DieselWalker> type, Level level) {
		super(type, level);
	}

	public int fuel() {
		return fuel;
	}

	public void setFuel(int fuel) {
		this.fuel = Mth.clamp(fuel, 0, FUEL_TANK);
	}

	public boolean drilling() {
		return entityData.get(DRILLING);
	}

	/** Client side: blocks walked so far, which the renderer turns into the legs' swing. */
	public float stride() {
		return stride;
	}

	/** Client side: ticks since the fist last swung. */
	public int sinceSwing() {
		return tickCount - swungAt;
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (PUNCHED_AT.equals(key) && level().isClientSide() && entityData.get(PUNCHED_AT) >= 0) {
			swungAt = tickCount;
		}
	}

	/** The pilot's keys; anyone else's are ignored. */
	public void steer(ServerPlayer player, int forward, int turn, int jump, int drill, int punch) {
		if (getFirstPassenger() != player) {
			return;
		}
		this.forward = Mth.clamp(forward, -1, 1);
		this.turn = Mth.clamp(turn, -1, 1);
		this.jump = Mth.clamp(jump, 0, 1);
		this.drill = Mth.clamp(drill, 0, 1);
		this.punch = Mth.clamp(punch, 0, 1);
		inputTick = tickCount;
	}

	/** Use: refuel from a bucket, else climb into the cockpit. */
	public InteractionResult use(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		Identifier id = BuiltInRegistries.ITEM.getKey(held.getItem());
		if (id.getNamespace().equals("jugcraft") && FUELS.contains(id.getPath())) {
			if (!level().isClientSide()) {
				if (fuel + FUEL_PER_BUCKET > FUEL_TANK) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.walker.full"));
				} else {
					fuel += FUEL_PER_BUCKET;
					if (!player.getAbilities().instabuild) {
						player.setItemInHand(hand, new ItemStack(Items.BUCKET));
					}
					level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 0.8F);
					player.sendOverlayMessage(Component.translatable("message.jugcraft.walker.refuelled", fuel * 100 / FUEL_TANK));
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (player.isPassenger() || isVehicle()) {
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
			walk(level);
		} else {
			stride += (float) Math.sqrt((getX() - xo) * (getX() - xo) + (getZ() - zo) * (getZ() - zo));
		}
		if (level().isClientSide() && drilling() && tickCount % 2 == 0) {
			double yaw = Math.toRadians(getYRot());
			double x = getX() - 1.3 * Math.cos(yaw) - 2.0 * Math.sin(yaw);
			double z = getZ() - 1.3 * Math.sin(yaw) + 2.0 * Math.cos(yaw);
			level().addParticle(ParticleTypes.SMOKE, x, getY() + 1.9, z, 0, 0.02, 0);
		}
	}

	private void walk(ServerLevel level) {
		Entity pilot = getFirstPassenger();
		boolean listening = pilot instanceof Player && tickCount - inputTick <= INPUT_TIMEOUT;
		int f = listening ? forward : 0;
		int t = listening ? turn : 0;
		boolean fuelled = fuel > 0;
		if (fuelled && t != 0) {
			setYRot(getYRot() - t * TURN);
		}
		double yaw = Math.toRadians(getYRot());
		Vec3 heading = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		double speed = !fuelled ? 0 : f > 0 ? WALK_SPEED : f < 0 ? -WALK_SPEED / 2 : 0;
		Vec3 motion = getDeltaMovement();
		double vy = motion.y - 0.08;
		if (onGround()) {
			vy = Math.max(vy, -0.1);
			if (fuelled && listening && jump > 0) {
				vy = JUMP;
			} else if (speed != 0 && horizontalCollision) {
				vy = 0.45; // Step up one block.
			}
		}
		setDeltaMovement(heading.x * speed, vy * 0.98, heading.z * speed);
		move(MoverType.SELF, getDeltaMovement());
		resetFallDistance();
		for (Entity passenger : getPassengers()) {
			passenger.resetFallDistance();
		}

		boolean drilled = listening && drill > 0 && fuelled && pilot instanceof ServerPlayer player && useHeld(level, player);
		if (!drilled) {
			stopDrilling(level);
		}
		entityData.set(DRILLING, drilled);
		boolean pressed = listening && punch > 0;
		if (pressed && !punchHeld && fuelled && pilot instanceof ServerPlayer player && tickCount - lastPunch >= attackCooldown()) {
			lastPunch = tickCount;
			entityData.set(PUNCHED_AT, tickCount);
			attack(level, player, heading);
		}
		punchHeld = pressed;

		boolean working = speed != 0 || t != 0 || drilled;
		if (working && fuelled && tickCount % (20 / FUEL_PER_SECOND) == 0) {
			fuel--;
			if (fuel == 0 && pilot instanceof Player player) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.walker.empty"));
			}
		}
		if (speed != 0 && tickCount % 10 == 0) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_STEP, SoundSource.NEUTRAL, 1.0F, 0.6F);
		}
		if (pilot instanceof Player player && tickCount % 40 == 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.walker.fuel", fuel * 100 / FUEL_TANK));
		}
		if (damage > 0 && tickCount % 10 == 0) {
			damage = Math.max(0, damage - 1);
		}
	}

	/**
	 * One tick of the pilot holding use: the Diesel Walker drills. Returns whether it is drilling (which burns fuel and
	 * spins the bit); a walker with another tool overrides this.
	 */
	protected boolean useHeld(ServerLevel level, ServerPlayer pilot) {
		return drill(level, pilot);
	}

	/** The pilot pressed attack (at most every {@link #attackCooldown()} ticks): the Diesel Walker punches. */
	protected void attack(ServerLevel level, ServerPlayer pilot, Vec3 heading) {
		punch(level, pilot, heading);
	}

	/** Ticks between attacks. */
	protected int attackCooldown() {
		return PUNCH_COOLDOWN;
	}

	/** Damage a player must deal (with no long break) to knock it down. */
	protected int health() {
		return HEALTH;
	}

	/** What it drops when knocked down. */
	protected ItemStack dropStack() {
		return new ItemStack(JugcraftWalkers.DIESEL_WALKER_ITEM);
	}

	/** Where the pilot sits, in blocks, before turning with the walker. */
	protected Vec3 seat() {
		return SEAT;
	}

	/** One tick of drilling the block the pilot looks at; false if there is nothing it can drill. */
	private boolean drill(ServerLevel level, ServerPlayer pilot) {
		HitResult hit = pilot.pick(DRILL_REACH, 1.0F, false);
		if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
			return false;
		}
		BlockPos pos = blockHit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		float hardness = state.getDestroySpeed(level, pos);
		if (state.isAir() || state.getBlock() instanceof LiquidBlock || hardness < 0 || hardness > DRILL_MAX_HARDNESS
				|| !level.mayInteract(pilot, pos) || !pilot.mayBuild()) {
			return false;
		}
		if (!pos.equals(drillTarget)) {
			stopDrilling(level);
			drillTarget = pos.immutable();
			drillProgress = 0;
		}
		int needed = Math.max(DRILL_MIN_TICKS, Mth.ceil(hardness * DRILL_TICKS_PER_HARDNESS));
		drillProgress++;
		if (tickCount % 4 == 0) {
			level.playSound(null, pos, state.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.8F, 0.7F);
		}
		if (drillProgress < needed) {
			level.destroyBlockProgress(getId(), pos, drillProgress * 10 / needed);
			return true;
		}
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, pilot, pos, state, blockEntity)) {
			level.destroyBlock(pos, true, pilot);
			PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(level, pilot, pos, state, blockEntity);
		}
		stopDrilling(level);
		return true;
	}

	private void stopDrilling(ServerLevel level) {
		if (drillTarget != null) {
			level.destroyBlockProgress(getId(), drillTarget, -1);
			drillTarget = null;
			drillProgress = 0;
		}
	}

	/** The fist: hits every living thing in a box just in front, for the pilot. */
	private void punch(ServerLevel level, ServerPlayer pilot, Vec3 heading) {
		Vec3 centre = position().add(heading.scale(2.2)).add(0, 1.6, 0);
		AABB reach = new AABB(centre, centre).inflate(1.6);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, reach, e -> e != pilot && e.isAlive())) {
			if (target.hurtServer(level, level.damageSources().playerAttack(pilot), PUNCH_DAMAGE)) {
				// Thrown along the walker's heading; a player's client is told, since a player moves themselves.
				target.setDeltaMovement(target.getDeltaMovement().add(heading.x * PUNCH_KNOCKBACK, 0.4, heading.z * PUNCH_KNOCKBACK));
				if (target instanceof ServerPlayer hit) {
					hit.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(hit));
				}
			}
		}
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.0F, 0.7F);
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().isEmpty();
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return seat().yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved() || !(source.getEntity() instanceof Player player) || player.getVehicle() == this) {
			return false;
		}
		if (player.getAbilities().instabuild) {
			discard();
			return true;
		}
		damage += amount;
		if (damage >= health()) {
			spawnAtLocation(level, dropStack());
			discard();
		}
		return true;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (level() instanceof ServerLevel level) {
			stopDrilling(level);
		}
		super.remove(reason);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DRILLING, false);
		builder.define(PUNCHED_AT, -100);
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
