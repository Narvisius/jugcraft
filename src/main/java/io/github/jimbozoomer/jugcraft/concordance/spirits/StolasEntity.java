package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.worker.PactDefinition;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToLongFunction;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-04 (D-S7): Stolas, a worker (here in spirits/, so check_workers' bans on chunk loading,
 * dimension changes and brain memories cover him; MISC, so transport crates refuse him). While a conversation is open at
 * his perch and nothing is kept there he is a visitor, bound to no one and on no roster; he leaves when he has neither a
 * pact nor a live conversation (declined, closed, timed out, its player gone, a restart), with the Concordance on or off:
 * a visitor is saved with his chunk (his perch only) and leaves at his first think after it loads. Once a pact is sealed
 * he is its one presence; he leaves when the pact is ended, kept elsewhere or answered by another. Only damage that
 * bypasses invulnerability harms him (Styx's rule); if he is lost all the same, the pact is not: its holder calls him
 * again at no cost. He keeps to his perch.
 * <p>
 * His body (D-06, D-S5): the owner's Flashjack rig in his raven form (tools/stolas_body.py). His main controller (the
 * worker's) plays a {@link Loop} for what the server synced: perched, listening while a conversation is open at his
 * perch, astir while an offering is due, hunched while his pact is suspended, waiting while he is off his service for any
 * other reason. His gestures play on a controller of their own over it, each fired by the server through
 * {@link #gesture} (GeckoLib sends it to those watching him then; one who starts watching later sees his state, never a
 * replay). He arrives when he is summoned to a perch; when he leaves he makes his departing gesture and is gone
 * {@link Gesture#DEPART}'s length of game time later: struck off his holder's roster at once, never saved meanwhile,
 * discarded by his own tick or, where his chunk no longer ticks him, by the server's sweep ({@link #registerSweep}); one
 * who starts watching him while he leaves sees him already gone (his synced departure, the unit review).
 * <p>
 * His cues (D-06): each gesture and his listening is fired by the server's action, once, with its sound and subtitle, at
 * most one of a kind every {@link #CUE_TICKS} and none while the Concordance is off but his departure; the pact screen
 * marks each answer, and Jade reads his pact (PactReading).
 */
public class StolasEntity extends WorkerEntity<StolasEntity> implements AgreementHolder.Presence {
	/** His pacts' counterpart (the pact definition's id). */
	public static final String COUNTERPART = PactDefinition.STOLAS;
	// ---------------------------------------------------------------- his body (D-06, D-S5; tools/concordance_pacts.py LOOPS, GESTURES)
	/** The controller his gestures play on, registered after the worker's own "main" one, so a gesture's bones win while it plays. */
	public static final String GESTURE_CONTROLLER = "gesture";
	/** Whether a conversation is open at his perch (the server's; never saved). */
	private static final EntityDataAccessor<Boolean> LISTENING = SynchedEntityData.defineId(StolasEntity.class, EntityDataSerializers.BOOLEAN);
	/** Whether he is leaving (the server's, set with his departing gesture; never saved): the client hides one it never saw go. */
	private static final EntityDataAccessor<Boolean> DEPARTING = SynchedEntityData.defineId(StolasEntity.class, EntityDataSerializers.BOOLEAN);
	/** His sound events, jugcraft:stolas.<id>, in tools/concordance_pacts.py SOUND_EVENTS' order: each gesture's and his listening's. */
	public static final List<String> SOUND_IDS = List.of("arrive", "listen", "inspect", "assent", "refuse", "depart");
	/**
	 * The least time between two of his cues of one kind (tools/concordance_pacts.py CUE_TICKS): a player's cues come from
	 * rate-gated actions; this bounds what any number of players can make him do (08:145-155).
	 */
	public static final int CUE_TICKS = 10;
	/** How many recent cues he keeps for reading (tests); never saved. */
	private static final int SHOWN = 8;
	private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();
	/**
	 * Every Stolas leaving now, by identity, for the server's sweep: it ends a departure on time where his own ticks do not
	 * (his chunk tracked but not ticking, or unloaded around him, since a departing Stolas is never saved and so never
	 * unloaded with it; the unit review). Memory only, emptied when the server stops.
	 */
	private static final Set<StolasEntity> LEAVING = Collections.newSetFromMap(new IdentityHashMap<>());

	/** His loops on the worker's main controller, for what the server synced (tools/concordance_pacts.py LOOPS). */
	public enum Loop {
		IDLE(RawAnimation.begin().thenLoop("animation.stolas.idle")),
		LISTEN(RawAnimation.begin().thenLoop("animation.stolas.listen")),
		WAITING(RawAnimation.begin().thenLoop("animation.stolas.waiting")),
		SUSPENDED(RawAnimation.begin().thenLoop("animation.stolas.suspended")),
		ASTIR(RawAnimation.begin().thenLoop("idle.flapping"));

		final RawAnimation animation;

		Loop(RawAnimation animation) {
			this.animation = animation;
		}
	}

	/** His gestures: each a one-shot clip on {@link #GESTURE_CONTROLLER}, its length in ticks (tools/concordance_pacts.py GESTURE_TIMES). */
	public enum Gesture {
		ARRIVE("arrive", 40, RawAnimation.begin().thenPlay("animation.stolas.arrive")),
		INSPECT("inspect", 50, RawAnimation.begin().thenPlay("animation.stolas.inspect")),
		ASSENT("assent", 40, RawAnimation.begin().thenPlay("animation.stolas.assent")),
		REFUSE("refuse", 40, RawAnimation.begin().thenPlay("animation.stolas.refuse")),
		DEPART("depart", 40, RawAnimation.begin().thenPlayAndHold("animation.stolas.depart"));

		/** Its name on the gesture controller (what the server triggers). */
		public final String id;
		public final int ticks;
		public final RawAnimation animation;

		Gesture(String id, int ticks, RawAnimation animation) {
			this.id = id;
			this.ticks = ticks;
			this.animation = animation;
		}

		/** The clip it plays (its name in stolas.animation.json). */
		public String clip() {
			return animation.getAnimationStages().getFirst().animationName();
		}
	}

	private @Nullable BlockPos perch;
	private @Nullable UUID record;
	/**
	 * The game time his departure ends at; 0 while he is not leaving. Never saved: a departing Stolas is not written to the
	 * world. His own tick or the server's sweep discards him once it is reached, whether or not his chunk ticks him.
	 */
	private long departAt;
	/** When each of his cues last showed (game time, by SOUND_IDS' index); never saved. */
	private final long[] cued = new long[SOUND_IDS.size()];
	/** His recent cues, oldest first; never saved. */
	private final ArrayDeque<String> shown = new ArrayDeque<>(SHOWN);

	public StolasEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
		setNoGravity(true);
		Arrays.fill(cued, Long.MIN_VALUE / 2);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.FOLLOW_RANGE, 8.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(LISTENING, false);
		builder.define(DEPARTING, false);
	}

	@Override
	public String kind() {
		return "pact";
	}

	/**
	 * Stolas joins his holder's roster only when his pact is sealed, he is called again or the pact is seated
	 * (AgreementHolder's note); a change of status never puts one replaced, or loaded while the Concordance is off, back
	 * on it (unit 1's unit review fix 2, as for the Gathering Shade; applied at the merge of unit 2).
	 */
	@Override
	protected boolean joinsRosterOnStatus() {
		return false;
	}

	public @Nullable BlockPos perch() {
		return perch;
	}

	public void setPerch(BlockPos at) {
		perch = at.immutable();
	}

	/** The Bound Will he serves (null for a visitor, and for a presence until it first thinks). */
	public @Nullable UUID record() {
		return record;
	}

	@Override
	public @Nullable BlockPos seat() {
		return perch;
	}

	/** Whether a conversation is open at his perch (both sides: the client draws him listening). */
	public boolean listening() {
		return entityData.get(LISTENING);
	}

	/** Whether he is leaving (the server's): his departing gesture is playing and he will be gone when it ends. */
	public boolean departing() {
		return departAt != 0;
	}

	/** Whether he is leaving, as synced (both sides): the client draws him only where it saw his departing gesture fired. */
	public boolean leaving() {
		return entityData.get(DEPARTING);
	}

	/** Gone, or going: removed, or departing. No perch, roster, pact or test counts him from then on. */
	public boolean gone() {
		return isRemoved() || departAt != 0;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, amount);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public Status think(ServerLevel level, long time, long gameTime) {
		Status next = decide(level, gameTime);
		if (!gone()) {
			// Leaving, he keeps his last status, as he did when he left at once: no sign over him, no roster entry touched.
			setStatus(next);
			// D-06: he listens while a conversation is open at his perch (its screen open, its time not past).
			entityData.set(LISTENING, perch != null && next != Status.DISABLED
					&& PactSessions.live(level.getServer(), GlobalPos.of(level.dimension(), perch), gameTime));
		}
		return next;
	}

	private Status decide(ServerLevel level, long gameTime) {
		if (owner == null && record == null && !awaited(level, gameTime)) {
			// A visitor no conversation waits on leaves whether or not the Concordance is on: he holds no record, and one
			// saved with his chunk is no perch's visitor when it loads (the unit review).
			leave(level, null, blockPosition());
			return Status.FINISHED;
		}
		if (!Workers.enabled()) {
			stop();
			return Status.DISABLED;
		}
		if (perch == null) {
			leave(level, null, blockPosition());
			return Status.FINISHED;
		}
		if (!level.isLoaded(perch)) {
			stop();
			return Status.DESTINATION_UNLOADED;
		}
		PactPerchBlockEntity home = level.getBlockEntity(perch) instanceof PactPerchBlockEntity found ? found : null;
		if (home == null) {
			leave(level, null, perch);
			return Status.FINISHED;
		}
		stay();
		if (record == null) {
			if (owner != null && home.holder().agreement() != null && getUUID().equals(home.holder().spirit())) {
				record = home.holder().agreement().record();
			} else if (owner == null && getUUID().equals(home.visitor())
					&& PactSessions.live(level.getServer(), GlobalPos.of(level.dimension(), perch), gameTime)) {
				return Status.IDLE;
			} else {
				leave(level, home, perch);
				return Status.FINISHED;
			}
		}
		AgreementHolder.Standing standing = home.holder().verify(level, gameTime);
		if (standing == AgreementHolder.Standing.UNREADABLE) {
			stop();
			return Status.OUTSIDE_AGREEMENT;
		}
		if (standing != AgreementHolder.Standing.VALID || !getUUID().equals(home.holder().spirit())
				|| !record.equals(home.holder().agreement().record())) {
			leave(level, home, perch);
			return Status.FINISHED;
		}
		// Its pact's state as its Bound Will says (D-S3, D-S9): suspended by its holder, an offering due, or held for upkeep.
		Status state = home.holder().state(level);
		if (state != null) {
			return state;
		}
		return Authority.presentNear(level, owner, perch, Pacts.REACH) == null ? Status.OWNER_OFFLINE : Status.IDLE;
	}

	/** Whether a conversation still waits on this visitor: his perch loaded, he its visitor, a conversation there live. */
	private boolean awaited(ServerLevel level, long gameTime) {
		return perch != null && level.isLoaded(perch) && level.getBlockEntity(perch) instanceof PactPerchBlockEntity home
				&& getUUID().equals(home.visitor()) && PactSessions.live(level.getServer(), GlobalPos.of(level.dimension(), perch), gameTime);
	}

	/** Pushed or led off his perch, he is back on it (within his own level, check_workers). */
	private void stay() {
		double x = perch.getX() + 0.5, y = perch.getY() + 1.0, z = perch.getZ() + 0.5;
		if (distanceToSqr(x, y, z) > 1.0) {
			teleportTo(x, y, z);
		}
	}

	@Override
	public void leave(ServerLevel level, AgreementHolder.@Nullable Holds home, BlockPos at) {
		if (gone()) {
			return;
		}
		if (owner != null) {
			WorkerRoster.of(level.getServer()).remove(owner, getUUID());
		}
		// D-06: he makes his departing gesture, and is discarded when it ends (customServerAiStep, or the server's sweep where
		// his chunk no longer ticks him); he listens no more, and one who starts watching him now sees him gone (the unit review).
		departAt = level.getGameTime() + Gesture.DEPART.ticks;
		LEAVING.add(this);
		entityData.set(LISTENING, false);
		entityData.set(DEPARTING, true);
		stop();
		gesture(Gesture.DEPART);
	}

	/** Departing, he only waits for his going; otherwise he thinks as every worker does. */
	@Override
	protected void customServerAiStep(ServerLevel level) {
		if (departAt != 0) {
			departed(level.getGameTime());
			return;
		}
		super.customServerAiStep(level);
	}

	/**
	 * Departing, he is not written to the world: a chunk saved or unloaded, or a server stopped, mid-departure leaves him
	 * out. An unsaved entity is not unloaded with its chunk, so the server's sweep ({@link #registerSweep}) discards him on
	 * time, ticking or not.
	 */
	@Override
	public boolean shouldBeSaved() {
		return departAt == 0 && super.shouldBeSaved();
	}

	/** Whether his departure is over by {@code gameTime}; if it is, he is discarded. */
	private boolean departed(long gameTime) {
		if (departAt == 0 || gameTime < departAt) {
			return false;
		}
		discard();
		return true;
	}

	/**
	 * Registers, once (Workers.register), the server's sweep at the end of every tick: each departure ends on time, at his
	 * own level's game time, his entity ticking or not.
	 */
	public static void registerSweep() {
		ServerTickEvents.END_SERVER_TICK.register(server -> sweep(stolas -> stolas.level().getGameTime()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LEAVING.clear());
	}

	/** The sweep at game time {@code gameTime}, as if the server's ran then (tests: no entity ticks meanwhile). */
	public static void sweep(long gameTime) {
		sweep(stolas -> gameTime);
	}

	/** Each Stolas leaving whose departure is over by {@code time} is discarded, and one already removed is dropped. */
	private static void sweep(ToLongFunction<StolasEntity> time) {
		if (LEAVING.isEmpty()) {
			return;
		}
		for (StolasEntity stolas : List.copyOf(LEAVING)) {
			if (stolas.isRemoved() || stolas.departed(time.applyAsLong(stolas))) {
				LEAVING.remove(stolas);
			}
		}
	}

	/** Killed or discarded: struck off his holder's roster, which is how the perch knows he is lost (D-S18's rule). */
	@Override
	public void remove(RemovalReason reason) {
		if (reason.shouldDestroy() && owner != null && level() instanceof ServerLevel server) {
			WorkerRoster.of(server.getServer()).remove(owner, getUUID());
		}
		super.remove(reason);
	}

	/**
	 * Plays one of his gestures for everyone watching him now, with its sound (server side only: GeckoLib sends it to those
	 * tracking him; one who starts watching later sees his state, never a replay). Shown only as his cues allow
	 * ({@link #cueing}): never while the Concordance is off, at most one of a kind every CUE_TICKS; his departure always.
	 * Nothing else follows from it.
	 */
	public void gesture(Gesture gesture) {
		if (!isRemoved() && level() instanceof ServerLevel level && cueing(level, gesture.id, gesture != Gesture.DEPART)) {
			triggerAnim(GESTURE_CONTROLLER, gesture.id);
		}
	}

	/** Registers his sound events once (Workers.register); sounds.json and their subtitles are tools/concordance_pacts.py's. */
	public static void registerSounds() {
		for (String id : SOUND_IDS) {
			Identifier key = Jugcraft.id("stolas." + id);
			SOUNDS.put(id, Registry.register(BuiltInRegistries.SOUND_EVENT, key, SoundEvent.createVariableRangeEvent(key)));
		}
	}

	/** His recent cues, oldest first (server side; never saved: one reloaded or newly watched has none). */
	public List<String> cues() {
		return List.copyOf(shown);
	}

	/**
	 * A conversation opens at his perch (PactPerchBlockEntity.heard): he listens at once (his loop, synced) and is heard to,
	 * unless he has only just alighted, whose wing beats carry the moment. Switched off, nothing.
	 */
	void listen() {
		if (gone() || !Workers.enabled() || !(level() instanceof ServerLevel level)) {
			return;
		}
		entityData.set(LISTENING, true);
		if (level.getGameTime() - cued[SOUND_IDS.indexOf(Gesture.ARRIVE.id)] >= CUE_TICKS) {
			cueing(level, "listen", true);
		}
	}

	/**
	 * Whether cue {@code id} shows now; if so its sound plays at him (entity-attached, NEUTRAL, heard within 16 blocks:
	 * sounds.json) and it is noted. Never while the Concordance is off, and at most one of a kind every CUE_TICKS, when
	 * {@code bounded} (his departure is neither: it happens once, when he leaves).
	 */
	private boolean cueing(ServerLevel level, String id, boolean bounded) {
		int kind = SOUND_IDS.indexOf(id);
		long now = level.getGameTime();
		if (kind < 0 || bounded && (!Workers.enabled() || now - cued[kind] < CUE_TICKS)) {
			return false;
		}
		cued[kind] = now;
		SoundEvent sound = SOUNDS.get(id);
		if (sound != null) {
			level.playSound(null, this, sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
		}
		shown.addLast(id);
		while (shown.size() > SHOWN) {
			shown.removeFirst();
		}
		return true;
	}

	/**
	 * The gesture with which he answers a pact screen's action (PactMenu.receive through PactPerchBlockEntity.react), from its
	 * answer: an acceptance sealed, assent; refused, refuse; a reckoning done, inspect (his astrolabe); refused, refuse; an
	 * offering paid ahead, inspect; a resumption, assent. None for the menu's own "not offered" ("changed" and "too fast"
	 * never reach an action), hearing his terms (he listens), declining and ending (he departs), suspending (his loop shows
	 * it) or seating (he arrives).
	 */
	public static @Nullable Gesture reaction(int action, String reason, boolean done) {
		if (PactMenu.NOT_OFFERED.equals(reason)) {
			return null;
		}
		return switch (action) {
			case PactRequestPayload.ACCEPT -> done ? Gesture.ASSENT : Gesture.REFUSE;
			case PactRequestPayload.RECALL -> done ? Gesture.INSPECT : Gesture.REFUSE;
			case PactRequestPayload.DEPOSIT -> done ? Gesture.INSPECT : null;
			case PactRequestPayload.RESUME -> done ? Gesture.ASSENT : null;
			default -> null;
		};
	}

	/** His loop for {@code status}: listening over all else while he serves; never a claim of work he is not doing. */
	public static Loop loop(Status status, boolean listening) {
		if (status == Status.DISABLED || status == Status.FINISHED) {
			return Loop.WAITING;
		}
		if (listening) {
			return Loop.LISTEN;
		}
		return switch (status) {
			case TRAVELLING, RETURNING, WORKING, UPKEEP_DUE -> Loop.ASTIR;
			case SUSPENDED, UPKEEP_HELD -> Loop.SUSPENDED;
			case OWNER_OFFLINE, OTHER_DIMENSION, OUTSIDE_AGREEMENT, DESTINATION_UNLOADED -> Loop.WAITING;
			default -> Loop.IDLE;
		};
	}

	@Override
	protected RawAnimation animation(Status status) {
		return loop(status, listening()).animation;
	}

	/** The worker's main controller (his loops), then his gestures' over it: triggered only, it draws nothing between them. */
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		super.registerControllers(controllers);
		AnimationController<StolasEntity> gestures = new AnimationController<StolasEntity>(GESTURE_CONTROLLER, 0, test -> PlayState.STOP);
		for (Gesture gesture : Gesture.values()) {
			gestures.triggerableAnim(gesture.id, gesture.animation);
		}
		controllers.add(gestures);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (perch != null) {
			output.store("perch", BlockPos.CODEC, perch);
		}
		if (record != null) {
			output.store("record", UUIDUtil.CODEC, record);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		perch = input.read("perch", BlockPos.CODEC).orElse(null);
		record = input.read("record", UUIDUtil.CODEC).orElse(null);
	}
}
