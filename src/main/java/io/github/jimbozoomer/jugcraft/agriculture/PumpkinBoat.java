package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A hollowed-out giant pumpkin afloat: the {@link Kind#BARGE} (from a full-grown 3x3x3 pumpkin, four seats,
 * keeps its carving and torch) or the {@link Kind#RACER} (from a 2x2x2 one, one seat). It paddles like a boat,
 * at a speed set by its weight: the lighter, the faster ({@link Kind#speedRatio}). Breaking it gives back its
 * item with everything it keeps ({@link PumpkinBoatData}).
 *
 * <p>Racing. Seated in one and using a {@link RegattaFlagBlock}, the driver starts a three-second countdown,
 * then must pass within {@link #MARK_RADIUS} blocks of each buoy of the flag's course in order and come back
 * within {@link #FINISH_RADIUS} of the flag. Everything is timed and checked on the server, one distance a
 * tick: leaving the boat, more than {@link #MAX_RACE_TICKS} or moving faster than {@link #MAX_SPEED} blocks
 * in a tick (no paddled boat can) ends the run without a time. A run in progress is not saved.
 */
public class PumpkinBoat extends AbstractBoat {
	/** A 2x2x2 giant pumpkin's racer weighs this plus {@link GiantPumpkinBlockEntity#WEIGHT_PER_POINT} kg per growth point. */
	public static final int RACER_BASE_WEIGHT = 30;
	public static final int RACER_LIGHTEST = RACER_BASE_WEIGHT + GiantPumpkinBlockEntity.GROW_TO_TWO * GiantPumpkinBlockEntity.WEIGHT_PER_POINT;
	public static final int RACER_HEAVIEST = RACER_BASE_WEIGHT + GiantPumpkinBlockEntity.GROW_TO_THREE * GiantPumpkinBlockEntity.WEIGHT_PER_POINT;
	/** A boat's speed in water is multiplied by its weight's ratio; vanilla boats keep this much speed a tick in water. */
	public static final double WATER_FRICTION = 0.9;
	public static final double MARK_RADIUS = 5.0;
	public static final double FINISH_RADIUS = 5.0;
	public static final int COUNTDOWN_TICKS = 60;
	public static final int MAX_RACE_TICKS = 12000;
	public static final double MAX_SPEED = 2.0;

	public enum Kind {
		BARGE("pumpkin_barge", 4, 3.0F, 1.875F, 0.625F, 0.9F, 1.0F, GiantPumpkinBlockEntity.START_WEIGHT,
				GiantPumpkinBlockEntity.MAX_WEIGHT, 0.95, 0.70),
		RACER("pumpkin_racer", 1, 2.0F, 1.5F, 0.5F, 0.7F, 0.7F, RACER_LIGHTEST, RACER_HEAVIEST, 1.30, 1.15);

		public final String id;
		public final int seats;
		/** How the shell is drawn: its width and height, the row of its old face it starts at (in blocks from the top), and its floor. */
		public final float width;
		public final float height;
		public final float faceTop;
		public final float floor;
		public final float seat;
		public final int lightest;
		public final int heaviest;
		/** Top speed against a vanilla boat's at the lightest and the heaviest weight. */
		public final double fastest;
		public final double slowest;

		Kind(String id, int seats, float width, float height, float faceTop, float floor, float seat, int lightest, int heaviest,
				double fastest, double slowest) {
			this.id = id;
			this.seats = seats;
			this.width = width;
			this.height = height;
			this.faceTop = faceTop;
			this.floor = floor;
			this.seat = seat;
			this.lightest = lightest;
			this.heaviest = heaviest;
			this.fastest = fastest;
			this.slowest = slowest;
		}

		/** Top speed in water against a vanilla boat's: from {@link #fastest} at the lightest weight to {@link #slowest} at the heaviest. */
		public double speedRatio(int weight) {
			double heavy = Mth.clamp((weight - lightest) / (double) (heaviest - lightest), 0.0, 1.0);
			return fastest + (slowest - fastest) * heavy;
		}

		/**
		 * What the speed is multiplied by each tick in water to reach {@link #speedRatio}: a boat's top speed is
		 * its push over (1 - friction), so the factor f solves {@code (1 - F) / (1 - F f) = ratio}.
		 */
		public double tickFactor(int weight) {
			return (1.0 - (1.0 - WATER_FRICTION) / speedRatio(weight)) / WATER_FRICTION;
		}

		public PumpkinBoatData defaultData() {
			return PumpkinBoatData.plain(lightest);
		}
	}

	private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(PumpkinBoat.class, EntityDataSerializers.ITEM_STACK);

	private final Kind kind;
	private @Nullable BlockPos raceFlag;
	private List<BlockPos> course = List.of();
	private int nextMark;
	private long goTick;
	private boolean started;
	private @Nullable UUID racer;
	private Vec3 lastPos = Vec3.ZERO;

	public PumpkinBoat(EntityType<? extends PumpkinBoat> type, Level level, Kind kind) {
		super(type, level, () -> JugcraftAgriculture.item(kind.id));
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ITEM, ItemStack.EMPTY);
	}

	/** The boat as an item: what it drops, with its weight, carving and torch. */
	public ItemStack item() {
		ItemStack stack = entityData.get(DATA_ITEM);
		return stack.isEmpty() ? new ItemStack(JugcraftAgriculture.item(kind.id)) : stack.copy();
	}

	public void setItem(ItemStack stack) {
		entityData.set(DATA_ITEM, stack.copyWithCount(1));
	}

	public PumpkinBoatData data() {
		PumpkinBoatData data = entityData.get(DATA_ITEM).get(JugcraftAgriculture.PUMPKIN_BOAT);
		return data != null ? data : kind.defaultData();
	}

	@Override
	protected double rideHeight(EntityDimensions dimensions) {
		return kind.seat;
	}

	@Override
	protected int getMaxPassengers() {
		return kind.seats;
	}

	/** The barge seats four, two abreast; the racer one, in the middle. */
	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
		if (kind.seats == 1) {
			return new Vec3(0.0, kind.seat, 0.0);
		}
		int index = Math.max(0, getPassengers().indexOf(passenger));
		double side = index % 2 == 0 ? 0.6 : -0.6;
		double along = index < 2 ? 0.6 : -0.6;
		return new Vec3(side, kind.seat, along).yRot(-getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	public void tick() {
		super.tick();
		if (getControllingPassenger() != null && level().getFluidState(blockPosition()).is(FluidTags.WATER)) {
			double factor = kind.tickFactor(data().weight());
			Vec3 motion = getDeltaMovement();
			setDeltaMovement(motion.x * factor, motion.y, motion.z * factor);
		}
		if (raceFlag != null && level() instanceof ServerLevel level) {
			tickRace(level);
		}
	}

	/** Breaking the boat gives back its item, weight, carving and torch included. */
	@Override
	protected void destroy(ServerLevel level, DamageSource source) {
		kill(level);
		if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
			spawnAtLocation(level, item());
		}
	}

	// ---------------------------------------------------------------- racing

	public boolean racing() {
		return raceFlag != null;
	}

	/** Which mark comes next: 0 to the number of buoys (then the finish). */
	public int nextMark() {
		return nextMark;
	}

	/**
	 * Starts a run for its driver round {@code marks} back to the flag at {@code flag}, after a countdown.
	 * Returns false if the player is not driving this boat or the course has no buoys.
	 */
	public boolean startRace(ServerPlayer player, BlockPos flag, List<BlockPos> marks) {
		if (getControllingPassenger() != player || marks.isEmpty()) {
			return false;
		}
		raceFlag = flag.immutable();
		course = List.copyOf(marks);
		nextMark = 0;
		goTick = level().getGameTime() + COUNTDOWN_TICKS;
		started = false;
		racer = player.getUUID();
		lastPos = position();
		return true;
	}

	private void endRace(@Nullable ServerPlayer player, String why) {
		if (player != null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.regatta." + why));
		}
		raceFlag = null;
		course = List.of();
		racer = null;
	}

	/** One race step: countdown, the checks that void a run, then whether the next mark is reached. */
	void tickRace(ServerLevel level) {
		BlockPos flag = raceFlag;
		if (flag == null) {
			return;
		}
		ServerPlayer player = getControllingPassenger() instanceof ServerPlayer driver && driver.getUUID().equals(racer) ? driver : null;
		if (player == null) {
			endRace(racer == null ? null : level.getServer().getPlayerList().getPlayer(racer), "abandoned");
			return;
		}
		long now = level.getGameTime();
		if (now < goTick) {
			long left = goTick - now;
			if (left % 20 == 0) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.regatta.countdown", left / 20));
				level.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
			}
			lastPos = position();
			return;
		}
		if (!started) {
			started = true;
			player.sendOverlayMessage(Component.translatable("message.jugcraft.regatta.go"));
			level.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0F, 1.5F);
		}
		long elapsed = now - goTick;
		Vec3 here = position();
		double moved = here.subtract(lastPos).horizontalDistance();
		lastPos = here;
		if (elapsed > MAX_RACE_TICKS) {
			endRace(player, "too_slow");
			return;
		}
		if (moved > MAX_SPEED) {
			endRace(player, "too_fast");
			return;
		}
		boolean finish = nextMark >= course.size();
		BlockPos mark = finish ? flag : course.get(nextMark);
		double radius = finish ? FINISH_RADIUS : MARK_RADIUS;
		double dx = getX() - (mark.getX() + 0.5);
		double dz = getZ() - (mark.getZ() + 0.5);
		if (dx * dx + dz * dz > radius * radius || Math.abs(getY() - mark.getY()) > 4.0) {
			return;
		}
		if (!finish) {
			nextMark++;
			player.sendOverlayMessage(Component.translatable("message.jugcraft.regatta.mark", nextMark, course.size(),
					RegattaFlagBlockEntity.time((int) elapsed)));
			level.playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
			return;
		}
		raceFlag = null;
		course = List.of();
		racer = null;
		level.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.0F);
		if (level.getBlockEntity(flag) instanceof RegattaFlagBlockEntity flagEntity) {
			flagEntity.finish(player, (int) elapsed);
		} else {
			player.sendSystemMessage(Component.translatable("message.jugcraft.regatta.no_flag", RegattaFlagBlockEntity.time((int) elapsed)));
		}
		TrickOrTreat.award(player, "pumpkin_regatta");
	}

	// ---------------------------------------------------------------- saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		ItemStack stack = entityData.get(DATA_ITEM);
		if (!stack.isEmpty()) {
			output.store("item", ItemStack.CODEC, stack);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.read("item", ItemStack.CODEC).filter(stack -> stack.is(JugcraftAgriculture.item(kind.id))).ifPresent(this::setItem);
	}
}
