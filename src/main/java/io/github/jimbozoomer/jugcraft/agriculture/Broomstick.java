package io.github.jimbozoomer.jugcraft.agriculture;

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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A flying broomstick (fall addition 22): a witch's broom anointed with Flying Ointment ({@link Hexes}), ridden by one
 * player and steered by looking. Forward flies the way the rider looks, up or down as they look; back brakes; left and
 * right drift sideways; jump climbs. With nothing pressed it slows to a hover. It flies at most {@value #MAX_SPEED}
 * blocks a tick, a quarter faster for a rider in a witch hat.
 *
 * <p>Its ointment is its fuel: {@value #CHARGE_PER_OINTMENT} ticks of flight each, up to {@value #MAX_CHARGE}, burnt
 * only while it is ridden off the ground. Run dry, it can't climb or speed up and sinks gently to the ground. Riderless,
 * it hovers where it was left (or sinks, dry). Its rider takes no fall damage, and gets off in the air with a few
 * seconds of slow falling.
 *
 * <p>Like a boat, its rider's client moves it and the server checks each move (vanilla's vehicle checks). The server
 * also burns its charge, and every {@value #CHECK_TICKS} ticks checks how far it went: faster than any broom can fly
 * (with {@value #TOLERANCE} times the room for lag), or climbing while dry, throws the rider off. Using it with Flying
 * Ointment anoints it; sneak-use takes it back as an item; a blow from a player breaks it back into its item.
 */
public class Broomstick extends Entity {
	public static final String ITEM = "flying_broomstick";
	public static final int CHARGE_PER_OINTMENT = 2400;
	public static final int MAX_CHARGE = 7200;
	/** Forward push a tick along the rider's look, sideways push, and climb on jump, in blocks a tick. */
	public static final double ACCEL = 0.05;
	public static final double STRAFE = 0.025;
	public static final double CLIMB = 0.04;
	/** Speed kept each tick; braking keeps this much more of it. */
	public static final double DRAG = 0.91;
	public static final double BRAKE = 0.8;
	public static final double MAX_SPEED = 0.6;
	public static final double HAT_BONUS = 1.25;
	/** How fast a dry broom sinks, in blocks a tick. */
	public static final double SINK = 0.08;
	public static final int CHECK_TICKS = 20;
	public static final double TOLERANCE = 3.0;
	/** How far a broom may climb in one check while dry (the rider's client may not know yet). */
	public static final double DRY_CLIMB = 2.0;
	public static final int LOW_CHARGE = 400;
	public static final int SLOW_FALL_TICKS = 100;
	/** Over the Moon: this many blocks above sea level on a full-moon night. */
	public static final int MOON_HEIGHT = 48;
	/** The handle's height above the broom's feet, and where its rider's feet are. */
	public static final double HANDLE_Y = 0.55;
	public static final double SEAT_Y = -0.05;

	private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(Broomstick.class, EntityDataSerializers.INT);

	private float inputForward;
	private float inputStrafe;
	private boolean inputUp;
	private @Nullable Vec3 lastCheck;
	private boolean dryAtLastCheck;
	/** Who rode it last tick, so a rider who gets off in the air can be given slow falling. */
	private @Nullable LivingEntity lastRider;

	public Broomstick(EntityType<? extends Broomstick> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public Broomstick(Level level, Vec3 at, float yaw, int charge) {
		this(JugcraftAgriculture.FLYING_BROOMSTICK, level);
		snapTo(at.x, at.y, at.z, yaw, 0.0F);
		setCharge(charge);
	}

	public int charge() {
		return entityData.get(CHARGE);
	}

	public void setCharge(int charge) {
		entityData.set(CHARGE, Mth.clamp(charge, 0, MAX_CHARGE));
	}

	public boolean dry() {
		return charge() <= 0;
	}

	/** The charge of a broom item. */
	public static int charge(ItemStack stack) {
		return stack.getOrDefault(JugcraftAgriculture.BROOM_CHARGE, 0);
	}

	/** The broom as an item, keeping its charge. */
	public ItemStack item() {
		ItemStack stack = new ItemStack(JugcraftAgriculture.item(ITEM));
		stack.set(JugcraftAgriculture.BROOM_CHARGE, charge());
		return stack;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(CHARGE, 0);
	}

	public @Nullable Player pilot() {
		return getFirstPassenger() instanceof Player player ? player : null;
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return pilot();
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().isEmpty() && passenger instanceof Player;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		return new Vec3(0.0, SEAT_Y, 0.0);
	}

	/** The pilot's keys, from their own client: forward 1 or back -1, right 1 or left -1, and jump. */
	public void steer(float forward, float strafe, boolean up) {
		inputForward = forward;
		inputStrafe = strafe;
		inputUp = up;
	}

	public static boolean wearsHat(Player player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(JugcraftAgriculture.item("witch_hat"));
	}

	/** The way a rider looking along {@code yaw} and {@code pitch} looks. */
	public static Vec3 look(float yaw, float pitch) {
		double y = Math.toRadians(yaw);
		double p = Math.toRadians(pitch);
		return new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
	}

	/**
	 * The broom's motion next tick, from its motion now and its rider's look and keys: pushed along the look by
	 * forward, sideways by strafe, up by jump; slowed by the air, more when braking; dry, it only slows and sinks. At
	 * most {@link #MAX_SPEED} (times {@link #HAT_BONUS} in a witch hat) across and up or down.
	 */
	public static Vec3 fly(Vec3 motion, float yaw, float pitch, float forward, float strafe, boolean up, boolean dry, boolean hat) {
		double boost = hat ? HAT_BONUS : 1.0;
		Vec3 next = motion;
		if (dry) {
			next = new Vec3(next.x * DRAG, Math.max(Math.min(next.y, 0.0) - 0.01, -SINK), next.z * DRAG);
		} else {
			if (forward > 0) {
				next = next.add(look(yaw, pitch).scale(ACCEL * boost));
			}
			if (strafe != 0) {
				double y = Math.toRadians(yaw);
				next = next.add(new Vec3(-Math.cos(y), 0.0, -Math.sin(y)).scale(STRAFE * boost * Math.signum(strafe)));
			}
			if (up) {
				next = next.add(0.0, CLIMB * boost, 0.0);
			}
			next = next.scale(forward < 0 ? DRAG * BRAKE : DRAG);
		}
		double most = MAX_SPEED * boost;
		double across = next.horizontalDistance();
		if (across > most) {
			next = new Vec3(next.x * most / across, next.y, next.z * most / across);
		}
		return new Vec3(next.x, Mth.clamp(next.y, -most, most), next.z);
	}

	/** Whether the broom is resting on something. */
	public boolean grounded() {
		return !level().noCollision(this, getBoundingBox().move(0.0, -0.06, 0.0));
	}

	@Override
	public void tick() {
		super.tick();
		Player pilot = pilot();
		if (pilot != null && level().isClientSide() && pilot.isLocalPlayer()) {
			Vec3 motion = fly(getDeltaMovement(), pilot.getYRot(), pilot.getXRot(), inputForward, inputStrafe, inputUp, dry(), wearsHat(pilot));
			setDeltaMovement(clearForRider(pilot, motion));
			setYRot(pilot.getYRot());
			move(MoverType.SELF, getDeltaMovement());
		} else if (pilot == null && !level().isClientSide()) {
			drift();
		}
		resetFallDistance();
		if (pilot != null) {
			pilot.resetFallDistance();
		}
		if (level() instanceof ServerLevel server) {
			serverTick(server, pilot);
		} else {
			trail();
		}
	}

	/** Keeps the rider's head and shoulders out of the blocks: a move that would put them in one is stopped that way. */
	private Vec3 clearForRider(Player rider, Vec3 motion) {
		AABB box = rider.getBoundingBox().deflate(0.05);
		if (!level().noCollision(rider, box)) {
			return motion;
		}
		double x = level().noCollision(rider, box.move(motion.x, 0.0, 0.0)) ? motion.x : 0.0;
		double y = motion.y <= 0 || level().noCollision(rider, box.move(0.0, motion.y, 0.0)) ? motion.y : 0.0;
		double z = level().noCollision(rider, box.move(0.0, 0.0, motion.z)) ? motion.z : 0.0;
		return new Vec3(x, y, z);
	}

	/** Riderless: it slows to a hover, or sinks to the ground when dry. */
	private void drift() {
		Vec3 motion = getDeltaMovement().scale(0.8);
		motion = new Vec3(motion.x, dry() && !grounded() ? -SINK : motion.y * 0.5, motion.z);
		setDeltaMovement(motion);
		if (motion.lengthSqr() > 1.0E-7) {
			move(MoverType.SELF, motion);
		}
	}

	private void serverTick(ServerLevel level, @Nullable Player pilot) {
		if (lastRider != null && lastRider != pilot) {
			softLanding(lastRider);
		}
		lastRider = pilot;
		if (!(pilot instanceof ServerPlayer flier)) {
			lastCheck = null;
			return;
		}
		if (charge() > 0 && !grounded()) {
			setCharge(charge() - 1);
			if (charge() == LOW_CHARGE) {
				flier.sendOverlayMessage(Component.translatable("message.jugcraft.broom.thin"));
			} else if (charge() == 0) {
				flier.sendOverlayMessage(Component.translatable("message.jugcraft.broom.dry"));
			}
		}
		if (lastCheck == null) {
			lastCheck = position();
			dryAtLastCheck = dry();
		}
		if (tickCount % CHECK_TICKS != 0) {
			return;
		}
		if (!withinReason(lastCheck, position(), dryAtLastCheck && dry(), wearsHat(flier))) {
			flier.sendOverlayMessage(Component.translatable("message.jugcraft.broom.bucked"));
			ejectPassengers();
			softLanding(flier);
			lastRider = null;
			lastCheck = null;
			return;
		}
		lastCheck = position();
		dryAtLastCheck = dry();
		if (overTheMoon(level)) {
			TrickOrTreat.award(flier, "over_the_moon");
		}
	}

	/**
	 * Whether a broom could have gone from {@code from} to {@code to} in {@link #CHECK_TICKS} ticks: no further across
	 * or up than its top speed allows (with {@link #TOLERANCE} times the room, for lag), and, dry all along, not
	 * climbing more than {@link #DRY_CLIMB}.
	 */
	public static boolean withinReason(Vec3 from, Vec3 to, boolean dry, boolean hat) {
		double most = MAX_SPEED * (hat ? HAT_BONUS : 1.0) * CHECK_TICKS * TOLERANCE;
		double rose = to.y - from.y;
		return to.subtract(from).horizontalDistance() <= most && rose <= most && !(dry && rose > DRY_CLIMB);
	}

	/** Flying high in the Overworld on a full-moon night. */
	private boolean overTheMoon(ServerLevel level) {
		return level.dimension() == Level.OVERWORLD && MooncakeItem.fullMoonNight(level.getOverworldClockTime())
				&& getY() >= level.getSeaLevel() + MOON_HEIGHT;
	}

	/** Witch's sparkles from the bristles while it flies (smoke when dry); drawn by clients only. */
	private void trail() {
		double moved = new Vec3(getX() - xo, getY() - yo, getZ() - zo).length();
		if (moved < 0.05 || random.nextInt(2) != 0) {
			return;
		}
		double y = Math.toRadians(getYRot());
		double x = getX() + Math.sin(y) * 0.8;
		double z = getZ() - Math.cos(y) * 0.8;
		level().addParticle(dry() ? ParticleTypes.SMOKE : ParticleTypes.WITCH, x, getY() + HANDLE_Y, z, 0.0, 0.0, 0.0);
	}

	@Override
	protected void addPassenger(Entity passenger) {
		super.addPassenger(passenger);
		if (passenger instanceof ServerPlayer rider && !dry()) {
			TrickOrTreat.award(rider, "up_and_away");
			level().playSound(null, getX(), getY(), getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.8F, 1.3F);
		}
	}

	/**
	 * Off the broom and not on the ground, the ointment still clings to its rider: a few seconds of slow falling. The
	 * server gives it the tick after a rider gets off, or at once when the broom throws them.
	 */
	private void softLanding(LivingEntity rider) {
		if (rider.isAlive() && !rider.onGround() && !rider.isPassenger()) {
			rider.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS));
		}
	}

	/**
	 * Using a broom: Flying Ointment anoints it (its bottle comes back); sneaking takes it back as an item; otherwise a
	 * player who isn't riding gets on, if nobody else is. Fabric's UseEntityCallback; the server's own reach check has
	 * already run.
	 */
	public static InteractionResult use(Player player, Level level, InteractionHand hand, Entity entity) {
		if (!(entity instanceof Broomstick broom) || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(Hexes.draught(BubblingCauldronBlock.Brew.FLYING))) {
			if (broom.charge() + CHARGE_PER_OINTMENT > MAX_CHARGE) {
				if (player instanceof ServerPlayer full) {
					full.sendOverlayMessage(Component.translatable("message.jugcraft.broom.full"));
				}
				return InteractionResult.FAIL;
			}
			if (level instanceof ServerLevel server) {
				broom.anoint(server);
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
				if (player instanceof ServerPlayer anointer) {
					anointer.sendOverlayMessage(Component.translatable("message.jugcraft.broom.anointed", seconds(broom.charge())));
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) {
			if (broom.isVehicle()) {
				return InteractionResult.PASS;
			}
			if (level instanceof ServerLevel server) {
				ItemStack item = broom.item();
				if (!player.getInventory().add(item)) {
					player.spawnAtLocation(server, item);
				}
				broom.discard();
			}
			return InteractionResult.SUCCESS;
		}
		if (broom.isVehicle() || player.isPassenger()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel) {
			if (broom.dry() && player instanceof ServerPlayer rider) {
				rider.sendOverlayMessage(Component.translatable("message.jugcraft.broom.needs_ointment"));
			}
			player.startRiding(broom);
		}
		return InteractionResult.SUCCESS;
	}

	/** One Flying Ointment's worth more charge, with a shimmer. */
	public void anoint(ServerLevel level) {
		setCharge(charge() + CHARGE_PER_OINTMENT);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8F, 1.6F);
		level.sendParticles(ParticleTypes.WITCH, getX(), getY() + HANDLE_Y, getZ(), 12, 0.5, 0.15, 0.5, 0.02);
	}

	/** {@code ticks} of flight as minutes and seconds, for messages and tooltips. */
	public static String seconds(int ticks) {
		int seconds = ticks / 20;
		return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}

	/** A blow from a player breaks a riderless broom back into its item (keeping its charge). */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isRemoved() || isVehicle() || !(source.getEntity() instanceof Player player)) {
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
	protected void readAdditionalSaveData(ValueInput input) {
		setCharge(input.getIntOr("charge", 0));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putInt("charge", charge());
	}
}
