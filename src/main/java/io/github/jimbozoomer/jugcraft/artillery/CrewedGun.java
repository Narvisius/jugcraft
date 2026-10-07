package io.github.jimbozoomer.jugcraft.artillery;

import io.github.jimbozoomer.jugcraft.building.FireControl;
import io.github.jimbozoomer.jugcraft.building.FireControlTableBlock;
import io.github.jimbozoomer.jugcraft.building.Fortifications;
import io.github.jimbozoomer.jugcraft.building.ReadyRackBlock;
import java.util.Comparator;
import java.util.List;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A crewed gun (batch 51): the siege mortar, the self-propelled howitzer and the flak gun. The server works the gun
 * from its gunner's keys ({@link ArtilleryInputPayload}) and view:
 * <ul>
 * <li>With a target marked by a Range Finder ({@link Spotting}), the gun turns to it and finds the elevation that
 * lands its shell there ({@link Ballistics}).</li>
 * <li>Otherwise it aims where the gunner looks; a gun that only fires high (the mortar) lobs at the block the gunner
 * looks at.</li>
 * </ul>
 * It traverses and elevates at a limited rate, and fires when the gunner presses attack (or, for an automatic gun,
 * holds it) once it is on target and reloaded, using a shell from the gunner's inventory or a ready rack beside the gun
 * (none in creative). Players knock it down into its item.
 *
 * <p>A gun linked to a Fire Control Table (batch 56, {@link FireControlTableBlock}) that has nobody at its controls is
 * laid and fired by the table's orders instead, using shells from ready racks only: on a fire mission it lays on its
 * point and fires one round each time the table orders a salvo; on sentry it picks the nearest hostile mob in the
 * table's sector and fires at it by itself. A gunner aboard always has the gun to themselves.
 */
public abstract class CrewedGun extends Entity {
	private static final int INPUT_TIMEOUT = 10;
	private static final double[] SINGLE_BARREL = {0.0};
	private static final EntityDataAccessor<Float> AIM_YAW = SynchedEntityData.defineId(CrewedGun.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AIM_PITCH = SynchedEntityData.defineId(CrewedGun.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> FIRED_AT = SynchedEntityData.defineId(CrewedGun.class, EntityDataSerializers.INT);

	protected int forward;
	protected int turn;
	private int fire;
	protected int inputTick = -INPUT_TIMEOUT - 1;
	private int lastShot = -1000;
	private boolean fireHeld;
	private boolean ordered;
	private boolean warned;
	private float damage;
	private int shotAt = -100;
	/** The Fire Control Table directing this gun, the last salvo it has seen ordered, and whether one waits to fire. */
	private @Nullable BlockPos director;
	private long seenSalvo;
	private boolean salvoOrdered;
	private @Nullable LivingEntity sentryTarget;

	protected CrewedGun(EntityType<? extends CrewedGun> type, Level level) {
		super(type, level);
	}

	// ---- what each gun defines

	/** The shell it fires and the item it uses for one. */
	protected abstract EntityType<ArtilleryShell> shellType();

	protected abstract Item ammo();

	protected abstract double shellSpeed();

	protected abstract double shellGravity();

	protected abstract int cooldown();

	/** Degrees a tick it traverses and elevates. */
	protected abstract float traverse();

	/** Elevation limits in degrees (up is positive). */
	protected abstract float minPitch();

	protected abstract float maxPitch();

	/** Whether it fires on the high arc (a mortar). */
	protected abstract boolean highArc();

	/** Whether holding attack keeps firing. */
	protected boolean automatic() {
		return false;
	}

	/** The gun's pivot (the point the barrel turns about) above its feet, and the barrel's length, in blocks. */
	protected abstract double pivotHeight();

	protected abstract double barrelLength();

	protected abstract Item dropItem();

	protected abstract int health();

	protected abstract int seats();

	/** Where a passenger sits, before turning with the gun. */
	protected abstract Vec3 seat(int index);

	/**
	 * The point the barrel turns about: {@link #pivotHeight()} above the gun's feet, plus {@link #pivotForward()} along
	 * its aim for a gun whose trunnions sit forward of its turntable.
	 */
	protected Vec3 pivot() {
		double yaw = Math.toRadians(entityData.get(AIM_YAW));
		return position().add(-Math.sin(yaw) * pivotForward(), pivotHeight(), Math.cos(yaw) * pivotForward());
	}

	/** How far forward of the turntable's centre the trunnions sit, in blocks. */
	protected double pivotForward() {
		return 0.0;
	}

	/**
	 * Each barrel's offset across the gun, in blocks (positive to the gun's left). One press fires a shell from each,
	 * using one shell each.
	 */
	protected double[] barrelOffsets() {
		return SINGLE_BARREL;
	}

	/** Limits a wanted aim yaw (for a gun that can't turn all the way round). */
	protected float limitYaw(float yaw) {
		return yaw;
	}

	/** Called every server tick before aiming, with whether the driver's keys are live. */
	protected void move(ServerLevel level, boolean listening) {
		Vec3 motion = getDeltaMovement();
		setDeltaMovement(0, onGround() ? 0 : Math.max(motion.y - 0.08, -1.0), 0);
		move(MoverType.SELF, getDeltaMovement());
	}

	// ---- shared

	public float aimYaw(float partialTick) {
		return entityData.get(AIM_YAW);
	}

	public float aimPitch() {
		return entityData.get(AIM_PITCH);
	}

	/** Client side: ticks since it last fired (for the recoil). */
	public int sinceShot() {
		return tickCount - shotAt;
	}

	public void steer(ServerPlayer player, int forward, int turn, int fire) {
		if (getFirstPassenger() != player) {
			return;
		}
		this.forward = Mth.clamp(forward, -1, 1);
		this.turn = Mth.clamp(turn, -1, 1);
		this.fire = Mth.clamp(fire, 0, 1);
		inputTick = tickCount;
	}

	/** Use: climb aboard. */
	public InteractionResult use(Player player, InteractionHand hand) {
		if (player.isPassenger() || getPassengers().size() >= seats()) {
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
		if (!(level() instanceof ServerLevel level)) {
			return;
		}
		Entity gunner = getFirstPassenger();
		boolean listening = gunner instanceof ServerPlayer && tickCount - inputTick <= INPUT_TIMEOUT;
		move(level, listening);
		resetFallDistance();
		Vec3 pivot = pivot();
		if (gunner instanceof ServerPlayer player) {
			FireControlTableBlock.Entity table = director(level);
			if (table != null) {
				// Orders given while someone crews the gun are theirs to ignore, not to fire later.
				seenSalvo = table.salvo();
				salvoOrdered = false;
			}
			sentryTarget = null;
			float[] wanted = wantedAim(level, player, pivot);
			if (wanted != null) {
				boolean onTarget = slew(wanted);
				boolean pressed = listening && fire > 0;
				// A press orders one shot, fired as soon as the gun is on target and reloaded; an automatic gun fires
				// for as long as attack is held.
				if (pressed && !fireHeld) {
					ordered = true;
				}
				fireHeld = pressed;
				if ((ordered || automatic() && pressed) && onTarget && reloaded()) {
					ordered = false;
					fire(level, player, pivot);
				}
			} else {
				ordered = false;
			}
		} else {
			ordered = false;
			fireHeld = false;
			if (gunner == null) {
				directed(level, pivot);
			}
		}
		if (damage > 0 && tickCount % 10 == 0) {
			damage = Math.max(0, damage - 1);
		}
	}

	/** Turns the gun toward a wanted yaw and elevation at its traverse rate; returns whether it is on them. */
	private boolean slew(float[] wanted) {
		float yaw = entityData.get(AIM_YAW);
		float pitch = entityData.get(AIM_PITCH);
		float newYaw = yaw + Mth.clamp(Mth.wrapDegrees(wanted[0] - yaw), -traverse(), traverse());
		float newPitch = pitch + Mth.clamp(wanted[1] - pitch, -traverse(), traverse());
		entityData.set(AIM_YAW, Mth.wrapDegrees(newYaw));
		entityData.set(AIM_PITCH, newPitch);
		return Math.abs(Mth.wrapDegrees(wanted[0] - newYaw)) < 1.0F && Math.abs(wanted[1] - newPitch) < 1.0F;
	}

	private boolean reloaded() {
		return tickCount - lastShot >= cooldown();
	}

	/** The yaw and elevation that land a shell on {@code point}, or null if it is out of reach. */
	private float @Nullable [] solve(Vec3 point, Vec3 pivot) {
		Vec3 delta = point.subtract(pivot);
		float yaw = limitYaw((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
		double distance = Math.sqrt(delta.x * delta.x + delta.z * delta.z) - barrelLength();
		Float pitch = Ballistics.solve(shellSpeed(), shellGravity(), distance, delta.y, highArc(), minPitch(), maxPitch());
		if (pitch == null && !highArc()) {
			pitch = Ballistics.solve(shellSpeed(), shellGravity(), distance, delta.y, true, minPitch(), maxPitch());
		}
		return pitch == null ? null : new float[] {yaw, pitch};
	}

	// ---- fire control (batch 56)

	/** The table directing this gun, or null: none, its chunk is not loaded, it is too far, or it has dropped the link. */
	private FireControlTableBlock.@Nullable Entity director(ServerLevel level) {
		if (director == null || !level.isLoaded(director)) {
			return null;
		}
		if (!(level.getBlockEntity(director) instanceof FireControlTableBlock.Entity table) || !table.linked(getUUID())) {
			// The table is gone or has cut the link.
			director = null;
			return null;
		}
		return position().distanceToSqr(Vec3.atCenterOf(director)) <= (double) FireControl.LINK_RANGE * FireControl.LINK_RANGE
				? table : null;
	}

	/** Links the gun to a table (dropping any earlier table's link), so it starts on the table's next salvo. */
	public void linkDirector(ServerLevel level, BlockPos pos, FireControlTableBlock.Entity table) {
		if (director != null && !director.equals(pos) && level.isLoaded(director)
				&& level.getBlockEntity(director) instanceof FireControlTableBlock.Entity old) {
			old.unlink(getUUID());
		}
		director = pos.immutable();
		seenSalvo = table.salvo();
		salvoOrdered = false;
	}

	public void unlinkDirector() {
		director = null;
		salvoOrdered = false;
		sentryTarget = null;
	}

	public @Nullable BlockPos director() {
		return director;
	}

	/** The mob this gun is laid on as a sentry, if any. */
	public @Nullable LivingEntity sentryTarget() {
		return sentryTarget;
	}

	/** An uncrewed gun working to its table's orders. */
	private void directed(ServerLevel level, Vec3 pivot) {
		FireControlTableBlock.Entity table = director(level);
		if (table == null) {
			salvoOrdered = false;
			sentryTarget = null;
			return;
		}
		FireControlTableBlock.Mode mode = table.mode();
		if (table.salvo() != seenSalvo) {
			// A salvo ordered since the gun last looked: fire one round once laid (only on a fire mission).
			salvoOrdered = table.salvo() > seenSalvo && mode.mission();
			seenSalvo = table.salvo();
		}
		boolean ready = false;
		if (mode.mission()) {
			sentryTarget = null;
			Vec3 point = table.aimPoint(getUUID());
			float[] wanted = point == null ? null : solve(point, pivot);
			if (wanted == null) {
				salvoOrdered = false;
			} else {
				boolean onTarget = slew(wanted);
				ready = onTarget && reloaded();
				if (salvoOrdered && ready) {
					salvoOrdered = false;
					fire(level, null, pivot);
				}
			}
		} else if (mode == FireControlTableBlock.Mode.SENTRY) {
			salvoOrdered = false;
			if (sentryTarget != null && (!sentryTarget.isAlive() || sentryTarget.isRemoved())) {
				sentryTarget = null;
			}
			if (tickCount % FireControl.SENTRY_SCAN == 0) {
				sentryTarget = pickTarget(level, table, pivot);
			}
			float[] wanted = sentryTarget == null ? null : solve(lead(sentryTarget, pivot), pivot);
			if (wanted == null) {
				sentryTarget = null;
			} else {
				boolean onTarget = slew(wanted);
				ready = onTarget && reloaded();
				if (ready && clearToFire(level, sentryTarget.position())) {
					fire(level, null, pivot);
				}
			}
		} else {
			salvoOrdered = false;
			sentryTarget = null;
		}
		if (ready && tickCount % FireControl.TABLE_INTERVAL == 0 && hasRackShell(level)) {
			table.ready(getUUID(), level.getGameTime());
		}
	}

	/**
	 * Sentry: the nearest hostile mob within {@value FireControl#SENTRY_RANGE} blocks of the gun, no closer than
	 * {@value FireControl#SENTRY_MIN_RANGE}, inside the table's sector, in reach, with no player near it and, for a gun
	 * firing on the low arc, nothing solid in the way.
	 */
	private @Nullable LivingEntity pickTarget(ServerLevel level, FireControlTableBlock.Entity table, Vec3 pivot) {
		double min = (double) FireControl.SENTRY_MIN_RANGE * FireControl.SENTRY_MIN_RANGE;
		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, new AABB(pivot, pivot).inflate(FireControl.SENTRY_RANGE),
				e -> e instanceof Enemy && e.isAlive() && !(e instanceof Player) && e.getRootVehicle() != this);
		candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(pivot)));
		int tried = 0;
		for (LivingEntity candidate : candidates) {
			Vec3 at = candidate.position();
			double dx = at.x - pivot.x;
			double dz = at.z - pivot.z;
			if (dx * dx + dz * dz < min || candidate.distanceToSqr(pivot) > (double) FireControl.SENTRY_RANGE * FireControl.SENTRY_RANGE
					|| !table.inSector(at) || !clearToFire(level, at)) {
				continue;
			}
			// Each solution is a search over simulated flights, so only the nearest few are tried each scan.
			if (++tried > 4) {
				break;
			}
			Vec3 aim = lead(candidate, pivot);
			if (solve(aim, pivot) == null) {
				continue;
			}
			if (!highArc()) {
				Vec3 from = pivot.add(aim.subtract(pivot).normalize().scale(barrelLength() + 0.5));
				BlockHitResult hit = level.clip(new ClipContext(from, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
				if (hit.getType() == HitResult.Type.BLOCK) {
					continue;
				}
			}
			return candidate;
		}
		return null;
	}

	/** Where to aim at a mob: its middle, led by how far it moves while the shell flies (roughly, ignoring drag). */
	private Vec3 lead(LivingEntity target, Vec3 pivot) {
		Vec3 middle = target.position().add(0, target.getBbHeight() / 2.0, 0);
		double flight = Math.sqrt(middle.distanceToSqr(pivot)) / shellSpeed();
		Vec3 motion = target.getDeltaMovement();
		return middle.add(motion.x * flight, 0, motion.z * flight);
	}

	/** Check fire: never shell a point with a player within {@value FireControl#CHECK_FIRE} blocks of it. */
	private static boolean clearToFire(ServerLevel level, Vec3 point) {
		double r = FireControl.CHECK_FIRE;
		return level.getEntitiesOfClass(Player.class, new AABB(point, point).inflate(r),
				p -> !p.isSpectator() && p.distanceToSqr(point) <= r * r).isEmpty();
	}

	private AABB rackReach() {
		int reach = Fortifications.RACK_REACH;
		return getBoundingBox().inflate(reach, 1, reach);
	}

	private boolean hasRackShell(ServerLevel level) {
		return ReadyRackBlock.has(level, rackReach(), ammo());
	}

	/** Knocked down or otherwise destroyed: drop the link from the table. */
	@Override
	public void remove(RemovalReason reason) {
		if (reason.shouldDestroy() && director != null && level() instanceof ServerLevel level && level.isLoaded(director)
				&& level.getBlockEntity(director) instanceof FireControlTableBlock.Entity table) {
			table.unlink(getUUID());
		}
		super.remove(reason);
	}

	/**
	 * The yaw and elevation the gun should turn to, or null to hold still: the gunner's own mark; else, for a gun linked
	 * to a fire control table on a fire mission, the table's point for it; else a nearby spotter's mark, or where the
	 * gunner looks.
	 */
	private float @Nullable [] wantedAim(ServerLevel level, ServerPlayer gunner, Vec3 pivot) {
		if (Spotting.own(level, gunner) == null) {
			FireControlTableBlock.Entity table = director(level);
			Vec3 point = table != null && table.mode().mission() ? table.aimPoint(getUUID()) : null;
			if (point != null) {
				return outOfRange(gunner, solve(point, pivot));
			}
		}
		BlockPos target = Spotting.target(level, gunner, pivot);
		if (target == null && highArc()) {
			// A mortar lobs at the block its gunner looks at.
			Vec3 from = gunner.getEyePosition();
			BlockHitResult hit = level.clip(new ClipContext(from, from.add(gunner.getViewVector(1.0F).scale(JugcraftArtillery.MARK_RANGE)),
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, gunner));
			if (hit.getType() == HitResult.Type.BLOCK) {
				target = hit.getBlockPos();
			}
		}
		if (target == null) {
			// Direct fire where the gunner looks.
			return new float[] {limitYaw(gunner.getYRot()), Mth.clamp(-gunner.getXRot(), minPitch(), maxPitch())};
		}
		return outOfRange(gunner, solve(Vec3.atCenterOf(target).add(0, 0.5, 0), pivot));
	}

	/** Passes an aim through, telling the gunner once when there is none (the point is out of reach). */
	private float @Nullable [] outOfRange(ServerPlayer gunner, float @Nullable [] aim) {
		if (aim == null) {
			if (!warned) {
				gunner.sendOverlayMessage(Component.translatable("message.jugcraft.artillery.out_of_range"));
				warned = true;
			}
			return null;
		}
		warned = false;
		return aim;
	}

	/** The way the barrel points. */
	public Vec3 barrelDirection() {
		double yaw = Math.toRadians(entityData.get(AIM_YAW));
		double pitch = Math.toRadians(entityData.get(AIM_PITCH));
		return new Vec3(-Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
	}

	/** Fires a round from each barrel. {@code gunner} is null when a fire control table fires the gun: racks only. */
	private void fire(ServerLevel level, @Nullable ServerPlayer gunner, Vec3 pivot) {
		Vec3 dir = barrelDirection();
		double yaw = Math.toRadians(entityData.get(AIM_YAW));
		Vec3 across = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		int fired = 0;
		for (double offset : barrelOffsets()) {
			// Each barrel uses a shell; a salvo fires as many barrels as there are shells for.
			if ((gunner == null || !gunner.getAbilities().instabuild) && !takeAmmo(level, gunner == null ? null : gunner.getInventory())) {
				break;
			}
			Vec3 muzzle = pivot.add(across.scale(offset)).add(dir.scale(barrelLength()));
			ArtilleryShell shell = gunner == null ? new ArtilleryShell(shellType(), level) : new ArtilleryShell(shellType(), level, gunner);
			shell.setPos(muzzle);
			shell.shoot(dir.x, dir.y, dir.z, (float) shellSpeed(), 0.0F);
			level.addFreshEntity(shell);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, automatic() ? 2 : 12, 0.3, 0.3, 0.3, 0.03);
			level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, automatic() ? 1 : 6, 0.1, 0.1, 0.1, 0.02);
			fired++;
		}
		if (fired == 0) {
			if (gunner == null) {
				return;
			}
			gunner.sendOverlayMessage(Component.translatable("message.jugcraft.artillery.no_shells", new ItemStack(ammo()).getHoverName()));
			return;
		}
		lastShot = tickCount;
		entityData.set(FIRED_AT, tickCount);
		Vec3 muzzle = pivot.add(dir.scale(barrelLength()));
		level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
				automatic() ? 0.6F : 3.0F, automatic() ? 1.9F : 0.7F);
	}

	/**
	 * Takes one shell: from the gunner's own inventory, or else from a ready rack (batch 55) within
	 * {@value Fortifications#RACK_REACH} blocks of the gun.
	 */
	private boolean takeAmmo(ServerLevel level, @Nullable Inventory inventory) {
		for (int slot = 0; inventory != null && slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(ammo())) {
				stack.shrink(1);
				return true;
			}
		}
		return ReadyRackBlock.take(level, rackReach(), ammo());
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
		return getPassengers().size() < seats();
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		int index = Math.max(0, getPassengers().indexOf(passenger));
		return seat(index).yRot(-seatYaw(index) * Mth.DEG_TO_RAD);
	}

	/** The yaw a seat turns with: the gun's for a gunner on a turning mount. */
	protected float seatYaw(int index) {
		return entityData.get(AIM_YAW);
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
		if (damage >= health()) {
			spawnAtLocation(level, new ItemStack(dropItem()));
			discard();
		}
		return true;
	}

	/** Faces a newly placed gun the way its placer faced. */
	public void face(float yaw) {
		setYRot(yaw);
		entityData.set(AIM_YAW, yaw);
		entityData.set(AIM_PITCH, Mth.clamp(highArc() ? 60.0F : 10.0F, minPitch(), maxPitch()));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(AIM_YAW, 0.0F);
		builder.define(AIM_PITCH, 20.0F);
		builder.define(FIRED_AT, -100);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		entityData.set(AIM_YAW, input.getFloatOr("aim_yaw", getYRot()));
		entityData.set(AIM_PITCH, input.getFloatOr("aim_pitch", 20.0F));
		director = input.read("director", BlockPos.CODEC).orElse(null);
		seenSalvo = input.getLongOr("seen_salvo", 0L);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("aim_yaw", entityData.get(AIM_YAW));
		output.putFloat("aim_pitch", entityData.get(AIM_PITCH));
		if (director != null) {
			output.store("director", BlockPos.CODEC, director);
		}
		output.putLong("seen_salvo", seenSalvo);
	}
}
