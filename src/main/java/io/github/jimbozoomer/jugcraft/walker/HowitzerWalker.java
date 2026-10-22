package io.github.jimbozoomer.jugcraft.walker;

import io.github.jimbozoomer.jugcraft.artillery.ArtilleryShell;
import io.github.jimbozoomer.jugcraft.artillery.Ballistics;
import io.github.jimbozoomer.jugcraft.artillery.JugcraftArtillery;
import io.github.jimbozoomer.jugcraft.artillery.Spotting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Howitzer Walker (docs/features/howitzer-walker.md), after the owner's own model of 9 October 2026: a tall
 * two-legged artillery walker, a riveted olive hull carrying a big howitzer in an open cradle, on reverse-jointed,
 * coil-sprung legs. It walks, turns, climbs and burns fuel as the {@link DieselWalker} does.
 * <ul>
 * <li><b>Use, held: lay and fire the howitzer.</b> It aims at the pilot's own Range Finder mark ({@link Spotting}),
 * else at the block the pilot looks at (up to {@value #AIM_REACH} blocks), else straight along their look. The gun is
 * fixed in the hull, so the whole walker turns to face the target, and the barrel elevates on its trunnion
 * ({@value #MIN_ELEVATION} to {@value #MAX_ELEVATION} degrees, {@value #LAY_RATE} a tick) to the arc that lands a Heavy
 * Shell there ({@link Ballistics}). Once laid and reloaded it fires, every {@value #GUN_COOLDOWN} ticks, a shell from
 * the pilot's own (none in creative): a damage-only blast, as every gun's. Let go, and the barrel settles back to
	 * {@value #REST_ELEVATION} degrees when unmanned; while piloted it keeps tracking without firing.</li>
 * <li><b>Jump:</b> it crouches for {@value #CROUCH_TICKS} ticks, then springs about three blocks up.</li>
 * <li><b>Attack: the stomp.</b> It crouches, hops and comes down hard (at once, if it is already in the air), and the
 * landing hurts and throws everything within {@value #STOMP_RADIUS} blocks: {@value #STOMP_DAMAGE} damage, counted as
 * the pilot's attack. It never breaks a block.</li>
 * </ul>
	 * The pilot's model is hidden inside the cockpit. The client draws its walk, shot and jump from the curves in
 * tools/howitzer_walker.py, the same curves its Blockbench clips are sampled from.
 */
public class HowitzerWalker extends DieselWalker {
	public static final int GUN_COOLDOWN = 80;
	public static final float LAY_RATE = 3.0F;
	public static final float MIN_ELEVATION = -15.0F;
	public static final float MAX_ELEVATION = 60.0F;
	public static final float REST_ELEVATION = 25.0F;
	public static final int AIM_REACH = 250;
	public static final double LEAP = 0.7;
	public static final int CROUCH_TICKS = 6;
	public static final double STOMP_HOP = 0.45;
	public static final double SLAM = -1.2;
	public static final int STOMP_DAMAGE = 14;
	public static final double STOMP_RADIUS = 3.5;
	public static final double STOMP_KNOCKBACK = 1.2;
	public static final int STOMP_COOLDOWN = 40;
	public static final int HEALTH = 80;
	public static final float WIDTH = 2.75F;
	public static final float HEIGHT = 5.4F;
	/** The gun's trunnion, in blocks before turning with the walker (TRUNNION in tools/howitzer_walker.py, over 16). */
	public static final Vec3 TRUNNION = new Vec3(-0.4375, 4.375, 0.25);
	/** From the trunnion to the muzzle along the barrel, in blocks. */
	public static final double BARREL = 2.25;
	/** The roof hatch the pilot sits in, head out (the hatch ring in tools/howitzer_walker.py). */
	private static final Vec3 HATCH = new Vec3(-0.5, 3.8, -0.4375);
	/** Ticks a crouch may wait for the ground before it is called off (it ran out of fuel, or stepped off an edge). */
	private static final int CROUCH_LIMIT = CROUCH_TICKS + 4;
	/** Ticks in the air before a landing thuds. */
	private static final int THUD_AIR_TICKS = 8;
	private static final EntityDataAccessor<Float> ELEVATION = SynchedEntityData.defineId(HowitzerWalker.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> FIRED_AT = SynchedEntityData.defineId(HowitzerWalker.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CROUCHED_AT = SynchedEntityData.defineId(HowitzerWalker.class, EntityDataSerializers.INT);

	private int lastShot = -GUN_COOLDOWN;
	/** Whether the controls are active this tick, and whether the trigger was held last tick. */
	private boolean laying;
	private boolean wasLaying;
	private boolean firing;
	private boolean warnedRange;
	private boolean warnedShells;
	/** Server side: the tick a crouch began (-1 when not crouching), how fast it will leap, and whether it will stomp. */
	private int crouchAt = -1;
	private double pendingLeap = LEAP;
	private boolean stomping;
	private int airTicks;
	/** Client side: the jump clip, the tick the gun last fired, and the barrel's elevation eased between ticks. */
	private final WalkerLeap leapClip = new WalkerLeap();
	private int shotAt = -1000;
	private float previousElevation = REST_ELEVATION;
	private float shownElevation = REST_ELEVATION;

	public HowitzerWalker(EntityType<? extends HowitzerWalker> type, Level level) {
		super(type, level);
	}

	/** The barrel's elevation in degrees, up positive. */
	public float elevation() {
		return entityData.get(ELEVATION);
	}

	/** Client side: the barrel's elevation this frame. */
	public float elevation(float partialTick) {
		return Mth.lerp(partialTick, previousElevation, shownElevation);
	}

	/** Client side: ticks since the gun last fired. */
	public float sinceShot(float partialTick) {
		return tickCount - shotAt + partialTick;
	}

	/** Client side: where the jump clip is, in its ticks, or -1 when it is not playing. */
	public float jumpTime(float partialTick) {
		return leapClip.time(this, partialTick, LEAP);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		// tickCount 0 is the data sent with the walker as it comes into view: an old shot or crouch, not a new one.
		if (!level().isClientSide() || tickCount == 0) {
			return;
		}
		if (FIRED_AT.equals(key)) {
			shotAt = tickCount;
		} else if (CROUCHED_AT.equals(key)) {
			leapClip.crouched(tickCount);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel level)) {
			previousElevation = shownElevation;
			shownElevation = elevation();
			leapClip.tick(this);
			return;
		}
		if (crouchAt >= 0 && (!onGround() || tickCount - crouchAt > CROUCH_LIMIT)) {
			crouchAt = -1;
			if (onGround()) {
				stomping = false;
			}
		}
		if (!onGround()) {
			airTicks++;
		} else if (airTicks > 0) {
			land(level);
			airTicks = 0;
		}
		if (!laying) {
			// Unmanned, out of fuel or timed-out controls: settle back to rest.
			float elevation = elevation();
			if (elevation != REST_ELEVATION) {
				entityData.set(ELEVATION, elevation + Mth.clamp(REST_ELEVATION - elevation, -LAY_RATE, LAY_RATE));
			}
		}
		if (!firing) {
			warnedRange = false;
			warnedShells = false;
		}
		wasLaying = firing;
		firing = false;
		laying = false;
	}

	/**
	 * Track the pilot's look independently of the trigger. Holding use fires once laid and reloaded. Returns
	 * whether it turned (turning burns fuel, as the turn keys do).
	 */
	@Override
	protected boolean operateTool(ServerLevel level, ServerPlayer pilot, boolean use, boolean manualTurn) {
		laying = true;
		firing = use;
		Vec3 pivot = pivot();
		BlockPos mark = Spotting.own(level, pilot);
		Vec3 target = mark != null ? Vec3.atCenterOf(mark).add(0, 0.5, 0) : lookedAt(level, pilot);
		if (mark != null && use && !wasLaying) {
			pilot.sendOverlayMessage(Component.translatable("message.jugcraft.howitzer_walker.on_mark"));
		}
		float wantedYaw;
		float wantedPitch;
		boolean reachable = true;
		if (target == null) {
			// Nothing in reach where the pilot looks (open sky): fire straight along the look.
			wantedYaw = pilot.getYRot();
			wantedPitch = Mth.clamp(-pilot.getXRot(), MIN_ELEVATION, MAX_ELEVATION);
		} else {
			float[] aim = solve(target, pivot);
			if (aim == null) {
				if (use && !warnedRange) {
					pilot.sendOverlayMessage(Component.translatable(mark != null ? "message.jugcraft.artillery.out_of_range"
							: "message.jugcraft.howitzer_walker.out_of_range"));
					warnedRange = true;
				}
				// An unsolvable arc must not freeze the controls. Unmarked targets fall back to direct aim;
				// an unreachable mark still turns the gun toward it, but must never consume ammunition.
				reachable = mark == null;
				Vec3 direction = target.subtract(pivot);
				wantedYaw = mark == null ? pilot.getYRot() : (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
				wantedPitch = Mth.clamp(mark == null ? -pilot.getXRot()
						: (float) Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())), MIN_ELEVATION, MAX_ELEVATION);
			} else {
				warnedRange = false;
				wantedYaw = aim[0];
				wantedPitch = aim[1];
			}
		}
		// A/D retain priority for deliberate hull steering; release them to resume mouse steering.
		float turn = manualTurn ? 0 : Mth.clamp(Mth.wrapDegrees(wantedYaw - getYRot()), -TURN, TURN);
		setYRot(getYRot() + turn);
		float elevation = elevation();
		float elevated = elevation + Mth.clamp(wantedPitch - elevation, -LAY_RATE, LAY_RATE);
		entityData.set(ELEVATION, elevated);
		boolean laid = Math.abs(Mth.wrapDegrees(wantedYaw - getYRot())) < 1.0F && Math.abs(wantedPitch - elevated) < 1.0F;
		if (use && reachable && laid && tickCount - lastShot >= GUN_COOLDOWN) {
			fire(level, pilot);
		}
		return Math.abs(turn) > 0.05F;
	}

	/** The trunnion in the world. */
	private Vec3 pivot() {
		return position().add(TRUNNION.yRot(-getYRot() * Mth.DEG_TO_RAD));
	}

	/** The way the barrel points. */
	private Vec3 barrelDirection() {
		double yaw = Math.toRadians(getYRot());
		double pitch = Math.toRadians(elevation());
		return new Vec3(-Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
	}

	/**
	 * The point on the block the pilot looks at, up to {@value #AIM_REACH} blocks, or null if there is none. The look is
	 * traced a chunk at a time and stops at the first chunk that is not loaded, so aiming never loads one.
	 */
	private @Nullable Vec3 lookedAt(ServerLevel level, ServerPlayer pilot) {
		Vec3 from = pilot.getEyePosition();
		// Use the rotation vanilla receives from the mounted player's camera, not the animated head yaw.
		Vec3 look = Vec3.directionFromRotation(pilot.getXRot(), pilot.getYRot());
		for (int travelled = 0; travelled < AIM_REACH; travelled += 16) {
			Vec3 to = from.add(look.scale(16));
			if (!level.isLoaded(BlockPos.containing(to))) {
				return null;
			}
			BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pilot));
			if (hit.getType() == HitResult.Type.BLOCK) {
				return hit.getLocation();
			}
			from = to;
		}
		return null;
	}

	/** The yaw and elevation that land a Heavy Shell on {@code point}, low arc first, or null if it is out of reach. */
	private float @Nullable [] solve(Vec3 point, Vec3 pivot) {
		Vec3 delta = point.subtract(pivot);
		float yaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
		double distance = delta.horizontalDistance() - BARREL;
		Float pitch = Ballistics.solve(JugcraftArtillery.HEAVY_SPEED, JugcraftArtillery.HEAVY_GRAVITY, distance, delta.y, false,
				MIN_ELEVATION, MAX_ELEVATION);
		if (pitch == null) {
			pitch = Ballistics.solve(JugcraftArtillery.HEAVY_SPEED, JugcraftArtillery.HEAVY_GRAVITY, distance, delta.y, true,
					MIN_ELEVATION, MAX_ELEVATION);
		}
		return pitch == null ? null : new float[] {yaw, pitch};
	}

	/** Fires a Heavy Shell along the barrel, taking one from the pilot (none in creative). */
	private void fire(ServerLevel level, ServerPlayer pilot) {
		if (!pilot.getAbilities().instabuild) {
			int slot = pilot.getInventory().findSlotMatchingItem(new ItemStack(JugcraftArtillery.HEAVY_SHELL_ITEM));
			if (slot < 0) {
				if (!warnedShells) {
					pilot.sendOverlayMessage(Component.translatable("message.jugcraft.artillery.no_shells",
							new ItemStack(JugcraftArtillery.HEAVY_SHELL_ITEM).getHoverName()));
					warnedShells = true;
				}
				return;
			}
			pilot.getInventory().removeItem(slot, 1);
		}
		warnedShells = false;
		lastShot = tickCount;
		entityData.set(FIRED_AT, tickCount);
		Vec3 dir = barrelDirection();
		Vec3 muzzle = pivot().add(dir.scale(BARREL));
		ArtilleryShell shell = new ArtilleryShell(JugcraftArtillery.HEAVY_SHELL, level, pilot);
		shell.setPos(muzzle);
		shell.shoot(dir.x, dir.y, dir.z, (float) JugcraftArtillery.HEAVY_SPEED, 0.0F);
		level.addFreshEntity(shell);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 12, 0.3, 0.3, 0.3, 0.03);
		level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 6, 0.1, 0.1, 0.1, 0.02);
		level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3.0F, 0.7F);
	}

	/** Jump crouches first: it leaps {@value #CROUCH_TICKS} ticks after the press. */
	@Override
	protected boolean leap(boolean jumpHeld) {
		if (crouchAt < 0) {
			if (jumpHeld) {
				crouch(LEAP);
			}
			return false;
		}
		if (tickCount - crouchAt < CROUCH_TICKS) {
			return false;
		}
		crouchAt = -1;
		return true;
	}

	@Override
	protected double leapSpeed() {
		return pendingLeap;
	}

	private void crouch(double speed) {
		crouchAt = tickCount;
		pendingLeap = speed;
		entityData.set(CROUCHED_AT, tickCount);
		level().playSound(null, getX(), getY(), getZ(), SoundEvents.PISTON_CONTRACT, SoundSource.NEUTRAL, 0.8F, 0.6F);
	}

	/** Attack: the stomp. On the ground it crouches and hops; in the air it drives straight down. It lands hard. */
	@Override
	protected void attack(ServerLevel level, ServerPlayer pilot, Vec3 heading) {
		stomping = true;
		if (onGround()) {
			if (crouchAt < 0) {
				crouch(STOMP_HOP);
			}
		} else {
			Vec3 motion = getDeltaMovement();
			setDeltaMovement(motion.x, Math.min(motion.y, SLAM), motion.z);
		}
	}

	@Override
	protected int attackCooldown() {
		return STOMP_COOLDOWN;
	}

	/** Down again: a stomp's shockwave, or after a long enough leap or fall a heavy thud. */
	private void land(ServerLevel level) {
		if (stomping) {
			stomping = false;
			shockwave(level);
		} else if (airTicks >= THUD_AIR_TICKS) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_STEP, SoundSource.NEUTRAL, 1.5F, 0.5F);
		}
	}

	/** Hurts and throws every living thing within {@value #STOMP_RADIUS} blocks of its feet, for the pilot. */
	private void shockwave(ServerLevel level) {
		Vec3 centre = position();
		if (getFirstPassenger() instanceof ServerPlayer pilot) {
			AABB area = new AABB(centre.x - STOMP_RADIUS, centre.y - 1, centre.z - STOMP_RADIUS,
					centre.x + STOMP_RADIUS, centre.y + 3, centre.z + STOMP_RADIUS);
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive() && e.getRootVehicle() != this)) {
				Vec3 away = target.position().subtract(centre);
				double distance = away.horizontalDistance();
				if (distance > STOMP_RADIUS || !target.hurtServer(level, level.damageSources().playerAttack(pilot), STOMP_DAMAGE)) {
					continue;
				}
				double push = distance < 0.01 ? 0 : STOMP_KNOCKBACK / distance;
				target.setDeltaMovement(target.getDeltaMovement().add(away.x * push, 0.5, away.z * push));
				if (target instanceof ServerPlayer hit) {
					// A player moves themselves, so their client is told.
					hit.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(hit));
				}
			}
		}
		BlockState ground = level.getBlockState(blockPosition().below());
		if (!ground.isAir()) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), centre.x, centre.y + 0.1, centre.z, 40,
					STOMP_RADIUS / 2, 0.1, STOMP_RADIUS / 2, 0.15);
		}
		level.sendParticles(ParticleTypes.POOF, centre.x, centre.y + 0.3, centre.z, 16, STOMP_RADIUS / 2, 0.1, STOMP_RADIUS / 2, 0.05);
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.5F);
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.5F);
	}

	/** Client side, while laying turns it: the engine works, and the rear vent puffs smoke. */
	@Override
	protected void workingEffects() {
		Vec3 vent = position().add(new Vec3(0, 4.2, -1.0).yRot(-getYRot() * Mth.DEG_TO_RAD));
		level().addParticle(ParticleTypes.SMOKE, vent.x, vent.y, vent.z, 0, 0.03, 0);
	}

	@Override
	protected int health() {
		return HEALTH;
	}

	@Override
	protected ItemStack dropStack() {
		return new ItemStack(JugcraftWalkers.HOWITZER_WALKER_ITEM);
	}

	@Override
	protected Vec3 seat() {
		return HATCH;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ELEVATION, REST_ELEVATION);
		builder.define(FIRED_AT, -100);
		builder.define(CROUCHED_AT, -100);
	}
}
