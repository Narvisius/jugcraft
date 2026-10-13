package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import io.github.jimbozoomer.jugcraft.concordance.worker.Navigation;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyPlayersSensor;
import org.jspecify.annotations.Nullable;

/**
 * What familiars, spirits and constructs share (roadmap step 17), and nothing more: each has its own server-owned model
 * (a {@link io.github.jimbozoomer.jugcraft.concordance.worker.Bond}, an
 * {@link io.github.jimbozoomer.jugcraft.concordance.worker.Agreement} or a
 * {@link io.github.jimbozoomer.jugcraft.concordance.worker.Body}), saved with it and independent of its brain.
 * <p>
 * <b>Decisions</b>: every {@value #THINK_TICKS} ticks the worker {@link #think}s on the server and leaves one
 * {@link Status}: what it is doing, or exactly why it is not. <b>Movement</b> is SmartBrainLib's: the decision sets the
 * walk-target memory, SmartBrainLib's {@code MoveToWalkTarget} behaviour walks it there, its players sensor fills the
 * nearby-players memory, and the vanilla can't-reach memory it keeps is how a worker knows it cannot navigate (after
 * {@link Navigation#GIVE_UP} tries it stops and says so, rather than retrying every tick). The brain's memories are
 * transient; nothing a worker owes or holds lives in them. <b>Presentation</b>: GeckoLib plays the animation for its
 * synced status. No worker loads a chunk: one in an unloaded chunk simply waits.
 */
public abstract class WorkerEntity<T extends WorkerEntity<T>> extends PathfinderMob implements GeoEntity, SmartBrainOwner<T> {
	public static final int THINK_TICKS = 20;
	/** The least time between two signs from one worker, so one that wavers between two states cannot flood its watchers. */
	public static final int SIGN_TICKS = 200;
	private static final EntityDataAccessor<String> STATUS = SynchedEntityData.defineId(WorkerEntity.class, EntityDataSerializers.STRING);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	protected @Nullable UUID owner;
	private Navigation navigation = Navigation.FINE;
	private long lastSign = Long.MIN_VALUE / 2;

	protected WorkerEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STATUS, Status.IDLE.id);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public void setOwner(@Nullable UUID owner) {
		this.owner = owner;
	}

	public Status status() {
		Status status = Status.fromId(entityData.get(STATUS));
		return status == null ? Status.IDLE : status;
	}

	protected void setStatus(Status status) {
		if (!entityData.get(STATUS).equals(status.id)) {
			entityData.set(STATUS, status.id);
			if (!isRemoved() && level() instanceof ServerLevel server) {
				if (owner != null) {
					WorkerRoster.of(server.getServer()).note(owner, getUUID(), kind(), status, server.dimension().identifier().toString(), blockPosition());
				}
				Sign sign = sign(status);
				long now = server.getGameTime();
				if (sign != null && now - lastSign >= SIGN_TICKS) {
					lastSign = now;
					Signs.show(server, position().add(0.0, getBbHeight() + 0.3, 0.0), sign, null);
				}
			}
		}
	}

	/**
	 * What a change to {@code status} shows over the worker (roadmap step 27), once, when it happens: shortage when it
	 * lacks something a player can give or mend (resources, access, a way there, repair, energy, room, a loaded
	 * destination); success when it has finished. Other changes show only in its body's animation.
	 */
	public static @Nullable Sign sign(Status status) {
		return switch (status) {
			case WAITING_FOR_RESOURCES, BLOCKED_BY_ACCESS, CANNOT_NAVIGATE, NEEDS_REPAIR, NO_ENERGY, FULL, DESTINATION_UNLOADED -> Sign.WANT;
			case FINISHED -> Sign.DONE;
			default -> null;
		};
	}

	/** The kind of worker ("familiar", "spirit", "construct"), for the roster and messages. */
	public abstract String kind();

	/** One decision at the level's own times (tests call {@link #think(ServerLevel, long, long)} with theirs). */
	public Status think(ServerLevel level) {
		return think(level, level.getOverworldClockTime(), level.getGameTime());
	}

	/** One decision at world time {@code time} and game time {@code gameTime}; returns the status it leaves. */
	public abstract Status think(ServerLevel level, long time, long gameTime);

	// ---------------------------------------------------------------- movement through SmartBrainLib

	/**
	 * Asks the brain to walk to {@code target}. Returns false (and the caller says "cannot navigate") once the brain has
	 * failed to reach a target {@link Navigation#GIVE_UP} times in a row, until the retry period has passed.
	 */
	protected boolean walkTo(Vec3 target, float speed, int closeEnough, long gameTime) {
		Brain<?> brain = getBrain();
		if (brain.hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
			navigation = navigation.failed(gameTime);
			brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
		}
		if (navigation.givenUp(gameTime)) {
			brain.eraseMemory(MemoryModuleType.WALK_TARGET);
			return false;
		}
		brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, speed, closeEnough));
		return true;
	}

	/** Arrived: forgets the walk target and any record of failing to reach it. */
	protected void arrived() {
		navigation = navigation.succeeded();
		getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
	}

	protected void stop() {
		getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
	}

	protected boolean near(Vec3 target, double reach) {
		return position().distanceToSqr(target) <= reach * reach;
	}

	protected static Vec3 centre(BlockPos pos) {
		return Vec3.atBottomCenterOf(pos);
	}

	/** SmartBrainLib builds the brain from these for every {@link SmartBrainOwner} and ticks it after each AI step. */
	@Override
	public List<? extends ExtendedSensor<?>> getSensors(T owner) {
		return List.of(new NearbyPlayersSensor<T>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getAlwaysRunningBehaviours(T owner) {
		return List.of(new LookAtTarget<T>(), new MoveToWalkTarget<T>());
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		if ((tickCount + getId()) % THINK_TICKS == 0) {
			think(level);
		}
		super.customServerAiStep(level);
	}

	// ---------------------------------------------------------------- talking to players

	/** An empty hand: anyone may ask what it is doing; the subclass may add its own uses. */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server && player.getItemInHand(hand).isEmpty()) {
			server.sendSystemMessage(describe());
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	/** Its name, its status in words and what it holds (the subclass adds its own model's numbers). */
	public Component describe() {
		return Component.translatable("message.jugcraft.concordance.workers.status", getDisplayName(),
				Component.translatable("compose.jugcraft.worker.status." + status().id));
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	/** No worker crosses into another dimension: a familiar waits for its person, a spirit and a construct for their places. */
	@Override
	public boolean canUsePortal(boolean allowPassengers) {
		return false;
	}

	// ---------------------------------------------------------------- saving

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		Saved.stamp(output, 1);
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putString("status", status().id);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		Status status = Status.fromId(input.getStringOr("status", Status.IDLE.id));
		entityData.set(STATUS, (status == null ? Status.IDLE : status).id);
	}

	// ---------------------------------------------------------------- animation

	/** The looping clip for a status (the subclass's names, from its .animation.json). */
	protected abstract RawAnimation animation(Status status);

	/**
	 * The clip for what the body is doing now: its status's, except that a worker on its way (or following) that is not
	 * moving, held up on its path or already beside its owner, shows itself still. Its animation never claims progress it
	 * is not making (roadmap step 27).
	 */
	public static Status shown(Status status, boolean moving) {
		return !moving && (status == Status.TRAVELLING || status == Status.RETURNING || status == Status.FOLLOWING) ? Status.IDLE : status;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<T>("main", 8, test -> test.setAndContinue(
				test.animatable().animation(shown(test.animatable().status(), test.isMoving())))));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
