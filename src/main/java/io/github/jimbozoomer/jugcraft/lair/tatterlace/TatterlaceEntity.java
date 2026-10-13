package io.github.jimbozoomer.jugcraft.lair.tatterlace;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.DoilyLaceBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import io.github.jimbozoomer.jugcraft.lair.LairExitBlock;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Madame Tatterlace (docs/features/tatterlace.md): the Spindle Loft's boss, a great spider seamstress. She waits, sewing,
 * on the white silk over the doily, and lowers herself onto it when someone steps onto the lace, her health scaled for
 * the party.
 *
 * <p>Her rule: the floor is her work. Phase 1, the Fitting, on the lace: Needlepoint, Thimble Toss, Binding Thread, Lace
 * Snare and Spool Roll. At half health, Taking In the Seams: she climbs into the threads, the light dims and she spits a
 * ring of egg sacs round the doily. Phase 2, the Final Fitting, hanging in the threads: Pin Rain, Unravel (a ring or a
 * wedge of the doily frays and drops away, and is knitted back {@value #KNIT_TICKS} ticks later; never the dense ring
 * round a spool, nor the tape's foot), Drop Strike (she drops onto a player and lies open on the lace) and Brood
 * (spiderlings out of the egg sacs). Below a fifth of her health, Frenzied Stitching: her cuffs glow red, she comes down
 * to the lace for good, every cooldown is a third shorter and she unravels two segments at once.
 *
 * <p>Every attack has a wind-up you can read, a strike and a recovery ({@link Attack}); the numbers are here, in one place,
 * and in tools/tatterlace.py. Each participant (whoever hurt her or her brood) gets their own loot
 * ({@link TatterlaceLoot}); left alone she knits her doily whole, goes back up to her silk and heals. Bound to a lair
 * instance she vanishes with it; one with no instance (a test's) keeps to the loft laid out from the origin she was given.
 * She walks on her own lace and threads: no gravity holds her, and no web.
 */
public class TatterlaceEntity extends Monster implements GeoEntity {
	// ---------------------------------------------------------------- numbers (tools/tatterlace.py)
	public static final float HEALTH = 360.0F;
	public static final double ARMOR = 8.0;
	public static final double LEASH = 30.0;
	public static final double PARTY_STEP = 0.5;
	public static final double PARTY_MAX = 2.5;
	public static final int DESCEND_TICKS = 40;
	public static final int ABANDON_TICKS = 200;
	public static final int GLOBAL_COOLDOWN = 20;
	public static final double SPEED = 0.26;
	public static final double PERCH = 14.5625;
	public static final double HANG = 9.0;
	public static final double HANG_SPEED = 0.2;
	public static final double KEEP_DISTANCE = 2.0;
	public static final float FRENZY_AT = 0.2F;
	public static final double FRENZY_COOLDOWN = 0.6667;
	public static final float STAB_DAMAGE = 8.0F;
	public static final double STAB_REACH = 3.5;
	public static final double STAB_HALF_ANGLE = 40.0;
	public static final int STAB_GAP = 5;
	public static final float SPOOL_DAMAGE = 10.0F;
	public static final double SPOOL_KNOCKBACK = 1.6;
	public static final int TAKE_IN_TICKS = 60;
	public static final int EGG_SACS = 6;
	public static final int PINS = 12;
	public static final float PIN_DAMAGE = 4.0F;
	public static final double PIN_AREA = 2.5;
	public static final int FRAY_TICKS = 40;
	public static final int KNIT_TICKS = 240;
	public static final double SEGMENT_WIDTH = 3.0;
	public static final double WEDGE_DEGREES = 60.0;
	public static final float DROP_DAMAGE = 14.0F;
	public static final double DROP_RADIUS = 3.0;
	public static final int DROP_OPEN_TICKS = 60;
	public static final int BROOD = 4;
	public static final int MAX_SPIDERLINGS = 6;
	public static final int EXPERIENCE = 300;
	/** How close a falling pin must land to strike a player, and how long her climb back to the threads takes. */
	private static final double PIN_REACH = 0.8;
	private static final int CLIMB_TICKS = 10;

	/**
	 * Her attacks: wind-up, the active part and recovery in ticks, the cooldown, the target's nearest and farthest distance,
	 * and the phases she uses it in (the Fitting, the Final Fitting, Frenzied Stitching). The wind-up is what players read;
	 * the strike lands as it ends.
	 */
	public enum Attack {
		NEEDLEPOINT(10, 6, 10, 40, 0.0, 3.5, true, false, true),
		THIMBLE_TOSS(10, 1, 10, 100, 4.0, 16.0, true, false, true),
		BINDING_THREAD(12, 1, 10, 160, 3.0, 16.0, true, false, true),
		LACE_SNARE(10, 1, 10, 140, 3.0, 14.0, true, false, true),
		SPOOL_ROLL(16, 1, 12, 240, 0.0, 40.0, true, false, true),
		PIN_RAIN(20, 10, 10, 100, 0.0, 40.0, false, true, true),
		UNRAVEL(40, 1, 10, 200, 0.0, 40.0, false, true, true),
		DROP_STRIKE(16, 1, 60, 160, 0.0, 40.0, false, true, false),
		BROOD(20, 1, 10, 300, 0.0, 40.0, false, true, true);

		public final int windup;
		public final int active;
		public final int recovery;
		public final int cooldown;
		public final double near;
		public final double far;
		public final boolean fitting;
		public final boolean last;
		public final boolean frenzy;

		Attack(int windup, int active, int recovery, int cooldown, double near, double far, boolean fitting, boolean last, boolean frenzy) {
			this.windup = windup;
			this.active = active;
			this.recovery = recovery;
			this.cooldown = cooldown;
			this.near = near;
			this.far = far;
			this.fitting = fitting;
			this.last = last;
			this.frenzy = frenzy;
		}

		public int length() {
			return windup + active + recovery;
		}
	}

	/** The fight's phases. */
	public enum Phase {
		WAITING, DESCENDING, FITTING, TAKING_IN, FINAL, FRENZY
	}

	/** What her body is doing, synced for her animations. */
	public enum Action {
		SEWING, DESCEND, IDLE, STAB_WINDUP, STAB, TOSS, THREAD_WINDUP, THREAD, SNARE, BRACE, KICK, CLIMB, HANG, PINS, UNRAVEL,
		DROP_WINDUP, DROP, OPEN, CALL
	}

	/** A segment of the doily she has unravelled: its cells, what each held, and when it is knitted back. */
	private record Segment(List<BlockPos> cells, List<BlockState> states, long knitAt) {
	}

	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(TatterlaceEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> FRENZIED = SynchedEntityData.defineId(TatterlaceEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation[] BODY = new RawAnimation[Action.values().length];
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.tatterlace.walk");
	private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("animation.tatterlace.death");
	private static final RawAnimation CUFFS_CALM = RawAnimation.begin().thenLoop("animation.tatterlace.cuffs_calm");
	private static final RawAnimation CUFFS_RED = RawAnimation.begin().thenLoop("animation.tatterlace.cuffs_red");

	static {
		String prefix = "animation.tatterlace.";
		BODY[Action.SEWING.ordinal()] = RawAnimation.begin().thenLoop(prefix + "sewing");
		BODY[Action.DESCEND.ordinal()] = RawAnimation.begin().thenLoop(prefix + "hang");
		BODY[Action.IDLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "idle");
		BODY[Action.STAB_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "stab_windup");
		BODY[Action.STAB.ordinal()] = RawAnimation.begin().then(prefix + "stab", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.TOSS.ordinal()] = RawAnimation.begin().then(prefix + "toss", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.THREAD_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "thread_windup");
		BODY[Action.THREAD.ordinal()] = RawAnimation.begin().then(prefix + "thread", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.SNARE.ordinal()] = RawAnimation.begin().then(prefix + "snare", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.BRACE.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "brace");
		BODY[Action.KICK.ordinal()] = RawAnimation.begin().then(prefix + "kick", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.CLIMB.ordinal()] = RawAnimation.begin().thenLoop(prefix + "climb");
		BODY[Action.HANG.ordinal()] = RawAnimation.begin().thenLoop(prefix + "hang");
		BODY[Action.PINS.ordinal()] = RawAnimation.begin().thenLoop(prefix + "pins");
		BODY[Action.UNRAVEL.ordinal()] = RawAnimation.begin().thenLoop(prefix + "unravel");
		BODY[Action.DROP_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "drop_windup");
		BODY[Action.DROP.ordinal()] = RawAnimation.begin().thenLoop(prefix + "drop");
		BODY[Action.OPEN.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "open");
		BODY[Action.CALL.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "brood");
	}

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.jugcraft.tatterlace"),
			BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
	/** The lair instance she belongs to (null for one put down by a test or a command), and the loft's origin. */
	private @Nullable UUID instance;
	private int slot = -1;
	private BlockPos origin = BlockPos.ZERO;
	private Phase phase = Phase.WAITING;
	private int phaseTicks;
	private @Nullable Attack attack;
	private int attackTicks;
	private int globalCooldown;
	private final int[] cooldowns = new int[Attack.values().length];
	private @Nullable UUID foe;
	private int abandoned;
	private final Map<UUID, Float> damageBy = new HashMap<>();
	private final List<UUID> sacs = new ArrayList<>();
	private final List<UUID> brood = new ArrayList<>();
	/** Her thrown and flung things (thimbles, threads, snares, spools), gone with her. */
	private final List<UUID> things = new ArrayList<>();
	private final List<Segment> unravelled = new ArrayList<>();
	private final List<List<BlockPos>> fraying = new ArrayList<>();
	private final List<Vec3> pins = new ArrayList<>();
	private Vec3 dropAt = Vec3.ZERO;
	private Vec3 kick = Vec3.ZERO;
	/** Set when she is loaded from a save: the doily is knitted whole on her first tick. */
	private boolean knitOnLoad;

	public TatterlaceEntity(EntityType<? extends TatterlaceEntity> type, Level level) {
		super(type, level);
		xpReward = 0;
		setNoGravity(true);
		setPersistenceRequired();
		bossBar.setDarkenScreen(true);
		bossBar.setVisible(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ARMOR, ARMOR)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, STAB_DAMAGE);
	}

	/**
	 * Sets her on her silk over the doily of {@code instance}'s Spindle Loft. Called as the instance is placed. Placing
	 * the template puts back only its own blocks, so the Grey Mist a fallen seamstress opened in this slot before still
	 * stands where the template has none: it goes first.
	 */
	public static TatterlaceEntity summon(ServerLevel level, LairInstance instance) {
		BlockPos origin = instance.origin();
		for (BlockPos at : exitCells(centre(origin))) {
			if (level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT)) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		return summon(level, origin, instance);
	}

	/** Sets her on her silk over the doily of the loft laid out from {@code origin} (tests pass no instance). */
	public static TatterlaceEntity summon(ServerLevel level, BlockPos origin, @Nullable LairInstance instance) {
		TatterlaceEntity tatterlace = new TatterlaceEntity(JugcraftTatterlace.TATTERLACE, level);
		tatterlace.origin = origin.immutable();
		if (instance != null) {
			tatterlace.instance = instance.id;
			tatterlace.slot = instance.slot;
		}
		Vec3 perch = tatterlace.perch();
		tatterlace.snapTo(perch.x, perch.y, perch.z, 0.0F, 0.0F);
		tatterlace.face(0.0F);
		level.addFreshEntity(tatterlace);
		return tatterlace;
	}

	// ---------------------------------------------------------------- where things are

	/** The doily's centre, on its lace, for the loft laid out from {@code origin}. */
	public static Vec3 centre(BlockPos origin) {
		return new Vec3(origin.getX() + SpindleLoft.DOILY_X, origin.getY() + SpindleLoft.LACE + SpindleLoft.LACE_TOP,
				origin.getZ() + SpindleLoft.DOILY_Z);
	}

	public Vec3 centre() {
		return centre(origin);
	}

	public BlockPos origin() {
		return origin;
	}

	/** Where she waits, sewing, on her silk over the doily's centre. */
	public Vec3 perch() {
		return new Vec3(origin.getX() + SpindleLoft.DOILY_X, origin.getY() + SpindleLoft.LACE + PERCH, origin.getZ() + SpindleLoft.DOILY_Z);
	}

	/** The height of the lace players stand on, and the height her feet hang at in the threads. */
	public double floorY() {
		return origin.getY() + SpindleLoft.LACE + SpindleLoft.LACE_TOP;
	}

	public double hangY() {
		return origin.getY() + SpindleLoft.LACE + HANG;
	}

	// ---------------------------------------------------------------- state

	public Phase phase() {
		return phase;
	}

	public @Nullable Attack attack() {
		return attack;
	}

	public int attackTicks() {
		return attackTicks;
	}

	public Action action() {
		int id = entityData.get(ACTION);
		return id >= 0 && id < Action.values().length ? Action.values()[id] : Action.IDLE;
	}

	private void setAction(Action action) {
		entityData.set(ACTION, action.ordinal());
	}

	public boolean frenzied() {
		return entityData.get(FRENZIED);
	}

	public @Nullable UUID instanceId() {
		return instance;
	}

	/** Whether she is in the threads (hanging), not on the lace. */
	public boolean hanging() {
		return phase == Phase.FINAL && attack != Attack.DROP_STRIKE;
	}

	/** Whether she is fighting (down, not waiting, descending or taking in her seams). */
	public boolean fighting() {
		return phase == Phase.FITTING || phase == Phase.FINAL || phase == Phase.FRENZY;
	}

	/** Damage she deals: the attack's own, times {@code lairs.boss_damage} ({@link LairBosses}). */
	public static float damage(float base) {
		return LairBosses.damage(base);
	}

	/** Her health for a party of {@code players}: half as much again for each beyond the first, at most 2.5 times. */
	public static double partyScale(int players) {
		return LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX);
	}

	/** A cooldown as it is in this phase: a third shorter in Frenzied Stitching. */
	public int cooldown(int ticks) {
		return frenzied() ? (int) Math.round(ticks * FRENZY_COOLDOWN) : ticks;
	}

	// ---------------------------------------------------------------- the server's tick

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!bound(level)) {
			discard();
			return;
		}
		if (knitOnLoad) {
			knitOnLoad = false;
			knitAll(level);
		}
		resetFallDistance();
		bossBar.setProgress(getHealth() / getMaxHealth());
		prune(level);
		knitDue(level);
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] > 0) {
				cooldowns[i]--;
			}
		}
		phaseTicks++;
		switch (phase) {
			case WAITING -> waiting(level);
			case DESCENDING -> descending();
			case FITTING, FINAL, FRENZY -> fight(level);
			case TAKING_IN -> takingIn();
		}
	}

	/** Whether her instance (if she has one) is still open. */
	private boolean bound(ServerLevel level) {
		return instance == null || lairInstance(level) != null;
	}

	private @Nullable LairInstance lairInstance(ServerLevel level) {
		Lair lair = Lair.of(level.dimension());
		LairInstance open = lair == null || instance == null ? null : Lairs.instance(lair, slot);
		return open != null && open.id.equals(instance) ? open : null;
	}

	private void waiting(ServerLevel level) {
		setDeltaMovement(Vec3.ZERO);
		Vec3 perch = perch();
		if (position().distanceToSqr(perch) > 0.01) {
			setPos(perch.x, perch.y, perch.z);
		}
		face(0.0F);
		setAction(Action.SEWING);
		if (tickCount % 10 == 0 && intruder(level) != null) {
			wake(level);
		}
	}

	/** Someone who may fight her, standing on the doily. */
	private @Nullable Player intruder(ServerLevel level) {
		Vec3 centre = centre();
		for (Player player : level.players()) {
			double dx = player.getX() - centre.x;
			double dz = player.getZ() - centre.z;
			if (LairBosses.eligible(player) && dx * dx + dz * dz <= SpindleLoft.DOILY_RADIUS * SpindleLoft.DOILY_RADIUS
					&& Math.abs(player.getY() - centre.y) < 4.0) {
				return player;
			}
		}
		return null;
	}

	/** She lowers herself onto the doily: her health scaled for the players near it, her bar shown, and the fight begins. */
	public void wake(ServerLevel level) {
		if (phase != Phase.WAITING) {
			return;
		}
		double scale = partyScale(Math.max(1, nearby(level).size()));
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(HEALTH * scale);
		}
		setHealth(getMaxHealth());
		phase = Phase.DESCENDING;
		phaseTicks = 0;
		setAction(Action.DESCEND);
		bossBar.setVisible(true);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.5F);
		tell(level, "message.jugcraft.tatterlace.wakes");
	}

	private void descending() {
		Vec3 perch = perch();
		double share = Math.min(1.0, phaseTicks / (double) DESCEND_TICKS);
		setDeltaMovement(Vec3.ZERO);
		setPos(perch.x, perch.y + (floorY() - perch.y) * share, perch.z);
		if (phaseTicks >= DESCEND_TICKS) {
			phase = Phase.FITTING;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
		}
	}

	/** Players near enough to her doily to be fighting (or about to). */
	public List<ServerPlayer> nearby(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (LairBosses.eligible(player) && player.position().distanceTo(centre) <= LEASH + 8.0) {
				out.add(player);
			}
		}
		return out;
	}

	// ---------------------------------------------------------------- fighting

	private void fight(ServerLevel level) {
		keepToTheLoft(level);
		Player target = target(level);
		if (target == null) {
			attack = null;
			setAction(phase == Phase.FINAL ? Action.HANG : Action.IDLE);
			move(centre(), phase == Phase.FINAL ? HANG_SPEED : SPEED);
			if (++abandoned >= ABANDON_TICKS) {
				reset(level);
			}
			return;
		}
		abandoned = 0;
		if (attack == null && phase == Phase.FITTING && getHealth() <= getMaxHealth() * 0.5F) {
			startTakingIn(level);
			return;
		}
		if (attack == null && phase == Phase.FINAL && getHealth() <= getMaxHealth() * FRENZY_AT) {
			startFrenzy(level);
			return;
		}
		if (attack != null) {
			attackTicks++;
			perform(level, target);
			return;
		}
		face(target);
		if (globalCooldown > 0) {
			globalCooldown--;
			approach(target);
			return;
		}
		Attack next = choose(level, target);
		if (next == null) {
			approach(target);
		} else {
			begin(level, next, target);
		}
	}

	/** Strayed farther than her leash, or fallen below her lace, she pulls herself back up her thread to the doily. */
	private void keepToTheLoft(ServerLevel level) {
		Vec3 centre = centre();
		double dx = getX() - centre.x;
		double dz = getZ() - centre.z;
		if (dx * dx + dz * dz <= LEASH * LEASH && getY() > floorY() - 2.0) {
			return;
		}
		level.sendParticles(ParticleTypes.ITEM_COBWEB, getX(), getY() + 0.8, getZ(), 12, 0.5, 0.5, 0.5, 0.02);
		double y = phase == Phase.FINAL ? hangY() : floorY();
		teleportTo(centre.x, y, centre.z);
		level.sendParticles(ParticleTypes.ITEM_COBWEB, getX(), getY() + 0.8, getZ(), 12, 0.5, 0.5, 0.5, 0.02);
	}

	/** Her target: the one she has while they are still in reach of her doily, else the nearest who may fight her. */
	private @Nullable Player target(ServerLevel level) {
		Vec3 centre = centre();
		if (foe != null) {
			Player held = level.getPlayerByUUID(foe);
			if (held != null && LairBosses.eligible(held) && held.position().distanceTo(centre) <= LEASH + 8.0) {
				return held;
			}
		}
		Player nearest = null;
		double best = Double.MAX_VALUE;
		for (ServerPlayer player : nearby(level)) {
			double distance = player.distanceToSqr(this);
			if (distance < best) {
				best = distance;
				nearest = player;
			}
		}
		foe = nearest == null ? null : nearest.getUUID();
		return nearest;
	}

	/** Scuttles toward a point {@value #KEEP_DISTANCE} blocks short of {@code target}, or (hanging) along her threads over it. */
	private void approach(Player target) {
		if (phase == Phase.FINAL) {
			move(new Vec3(target.getX(), hangY(), target.getZ()), HANG_SPEED);
			return;
		}
		Vec3 to = target.position().subtract(position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 goal = flat > KEEP_DISTANCE + 0.5
				? new Vec3(target.getX() - to.x / flat * KEEP_DISTANCE, floorY(), target.getZ() - to.z / flat * KEEP_DISTANCE)
				: new Vec3(getX(), floorY(), getZ());
		move(goal, SPEED);
	}

	/**
	 * Moves toward {@code goal} at {@code speed}: on the lace kept at its height and within her leash; in the threads kept
	 * at her hanging height and within the white silk's square.
	 */
	private void move(Vec3 goal, double speed) {
		Vec3 centre = centre();
		double x = goal.x;
		double z = goal.z;
		double y;
		if (phase == Phase.FINAL) {
			double[] low = SpindleLoft.SPOOLS.get(0);
			double[] high = SpindleLoft.SPOOLS.get(3);
			x = Math.clamp(x, origin.getX() + low[0], origin.getX() + high[0]);
			z = Math.clamp(z, origin.getZ() + low[1], origin.getZ() + high[1]);
			y = hangY();
		} else {
			double dx = x - centre.x;
			double dz = z - centre.z;
			double flat = Math.sqrt(dx * dx + dz * dz);
			if (flat > LEASH) {
				x = centre.x + dx / flat * LEASH;
				z = centre.z + dz / flat * LEASH;
			}
			y = floorY();
		}
		Vec3 to = new Vec3(x - getX(), 0.0, z - getZ());
		double length = to.length();
		Vec3 step = length < 0.05 ? Vec3.ZERO : to.scale(Math.min(speed, length) / length);
		setDeltaMovement(step.x, y - getY(), step.z);
	}

	private void face(Entity target) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		if (dx * dx + dz * dz > 1.0E-4) {
			face((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
		}
	}

	private void face(float yaw) {
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
	}

	/** The way she faces, flat. */
	public Vec3 facing() {
		double yaw = Math.toRadians(getYRot());
		return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
	}

	/** Picks an attack she can use now on {@code target}, or null. */
	private @Nullable Attack choose(ServerLevel level, Player target) {
		double distance = target.distanceTo(this);
		List<Attack> ready = new ArrayList<>();
		for (Attack candidate : Attack.values()) {
			boolean phaseAllows = switch (phase) {
				case FITTING -> candidate.fitting;
				case FINAL -> candidate.last;
				case FRENZY -> candidate.frenzy;
				default -> false;
			};
			if (!phaseAllows || cooldowns[candidate.ordinal()] > 0 || distance < candidate.near || distance > candidate.far) {
				continue;
			}
			boolean usable = switch (candidate) {
				case BINDING_THREAD -> !BindingThreadEntity.tethered(level, target);
				case UNRAVEL -> unravelled.size() + fraying.size() < 2;
				case BROOD -> !sacs(level).isEmpty() && brood.size() < MAX_SPIDERLINGS;
				default -> true;
			};
			if (usable) {
				ready.add(candidate);
				if (candidate == Attack.NEEDLEPOINT || candidate == Attack.DROP_STRIKE) {
					ready.add(candidate);  // close in her needle comes most often; hanging, she likes to drop
					ready.add(candidate);
				}
			}
		}
		return ready.isEmpty() ? null : ready.get(getRandom().nextInt(ready.size()));
	}

	/** Starts {@code next} on {@code target}: its wind-up begins. */
	public void begin(ServerLevel level, Attack next, Player target) {
		attack = next;
		attackTicks = 0;
		foe = target.getUUID();
		cooldowns[next.ordinal()] = cooldown(next.cooldown);
		setDeltaMovement(Vec3.ZERO);
		face(target);
		boolean down = phase != Phase.FINAL;
		switch (next) {
			case NEEDLEPOINT -> {
				setAction(Action.STAB_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.4F);
			}
			case THIMBLE_TOSS -> setAction(Action.TOSS);
			case BINDING_THREAD -> setAction(Action.THREAD_WINDUP);
			case LACE_SNARE -> setAction(Action.SNARE);
			case SPOOL_ROLL -> {
				setAction(Action.BRACE);
				Vec3 to = target.position().subtract(position());
				double flat = Math.sqrt(to.x * to.x + to.z * to.z);
				kick = flat < 1.0E-3 ? facing() : new Vec3(to.x / flat, 0.0, to.z / flat);
			}
			case PIN_RAIN -> {
				setAction(down ? Action.CALL : Action.PINS);
				pins.clear();
				for (int i = 0; i < PINS; i++) {
					double angle = getRandom().nextDouble() * Math.PI * 2.0;
					double reach = Math.sqrt(getRandom().nextDouble()) * PIN_AREA;
					pins.add(new Vec3(target.getX() + Math.cos(angle) * reach, floorY(), target.getZ() + Math.sin(angle) * reach));
				}
			}
			case UNRAVEL -> {
				setAction(down ? Action.CALL : Action.UNRAVEL);
				fraying.clear();
				fraying.add(segment(level, getRandom().nextBoolean(), target.position()));
				if (frenzied()) {
					List<ServerPlayer> others = nearby(level);
					others.removeIf(other -> other.getUUID().equals(target.getUUID()));
					Vec3 second = others.isEmpty() ? centre().add(centre().subtract(target.position()))
							: others.get(getRandom().nextInt(others.size())).position();
					fraying.add(segment(level, getRandom().nextBoolean(), second));
				}
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
			}
			case DROP_STRIKE -> {
				setAction(Action.DROP_WINDUP);
				dropAt = new Vec3(target.getX(), floorY(), target.getZ());
			}
			case BROOD -> setAction(down ? Action.CALL : Action.HANG);
		}
	}

	private void perform(ServerLevel level, Player target) {
		Attack current = attack;
		if (current == null) {
			return;
		}
		int t = attackTicks;
		if (current != Attack.DROP_STRIKE) {
			setDeltaMovement(Vec3.ZERO);
		}
		switch (current) {
			case NEEDLEPOINT -> {
				if (t < current.windup) {
					face(target);
				} else if (t == current.windup) {
					setAction(Action.STAB);
					stab(level);
				} else if (t == current.windup + STAB_GAP) {
					stab(level);
				}
			}
			case THIMBLE_TOSS -> {
				if (t < current.windup) {
					face(target);
				} else if (t == current.windup) {
					things.add(TossedThimbleEntity.toss(level, this, target).getUUID());
				}
			}
			case BINDING_THREAD -> {
				if (t < current.windup) {
					face(target);
					if (t % 3 == 0) {
						Vec3 from = spinnerets();
						level.sendParticles(ParticleTypes.END_ROD, from.x, from.y, from.z, 1, 0.05, 0.05, 0.05, 0.0);
					}
				} else if (t == current.windup) {
					setAction(Action.THREAD);
					things.add(BindingThreadEntity.shoot(level, this, target).getUUID());
				}
			}
			case LACE_SNARE -> {
				if (t < current.windup) {
					face(target);
				} else if (t == current.windup) {
					things.add(LaceSnareEntity.fling(level, this, target.position()).getUUID());
				}
			}
			case SPOOL_ROLL -> {
				if (t == current.windup) {
					setAction(Action.KICK);
					things.add(RollingSpoolEntity.kick(level, this, kick).getUUID());
				}
			}
			case PIN_RAIN -> pinRain(level, t - current.windup);
			case UNRAVEL -> {
				if (t < current.windup) {
					if (t % 4 == 0) {
						for (List<BlockPos> segment : fraying) {
							fray(level, segment);
						}
					}
					if (t % 10 == 0) {
						level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 1.5F, 0.5F);
					}
				} else if (t == current.windup) {
					for (List<BlockPos> segment : fraying) {
						unravel(level, segment);
					}
					fraying.clear();
				}
			}
			case DROP_STRIKE -> dropStrike(level, t);
			case BROOD -> {
				if (t == current.windup) {
					broodOut(level);
				}
			}
		}
		if (attack == current && attackTicks >= current.length()) {
			attack = null;
			attackTicks = 0;
			globalCooldown = cooldown(GLOBAL_COOLDOWN);
			Action action = action();
			if (action != Action.STAB && action != Action.TOSS && action != Action.THREAD && action != Action.SNARE && action != Action.KICK) {
				setAction(phase == Phase.FINAL ? Action.HANG : Action.IDLE);
			}
		}
	}

	/** Needlepoint's stab: every player within {@value #STAB_REACH} blocks and {@value #STAB_HALF_ANGLE} degrees of where she faces. */
	public int stab(ServerLevel level) {
		int hit = 0;
		Vec3 facing = facing();
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(STAB_REACH + 1.5, 2.0, STAB_REACH + 1.5))) {
			if (!LairBosses.eligible(player)) {
				continue;
			}
			Vec3 to = player.position().subtract(position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > STAB_REACH + getBbWidth() / 2.0 + player.getBbWidth() / 2.0 || Math.abs(to.y) > 2.5) {
				continue;
			}
			double cos = flat < 1.0E-3 ? 1.0 : (to.x * facing.x + to.z * facing.z) / flat;
			if (cos < Math.cos(Math.toRadians(STAB_HALF_ANGLE))) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(STAB_DAMAGE))) {
				hit++;
			}
		}
		Vec3 tip = position().add(facing.scale(2.6)).add(0.0, 0.9, 0.0);
		level.sendParticles(ParticleTypes.CRIT, tip.x, tip.y, tip.z, 6, 0.2, 0.2, 0.2, 0.1);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 1.0F, 1.6F);
		return hit;
	}

	/** Where her thread leaves her: the spinnerets at the end of her abdomen. */
	public Vec3 spinnerets() {
		return position().add(facing().scale(-1.6)).add(0.0, 1.2, 0.0);
	}

	/** Where she keeps her thimbles: on her abdomen's back. */
	public Vec3 back() {
		return position().add(facing().scale(-0.8)).add(0.0, 1.8, 0.0);
	}

	/**
	 * Pin Rain, {@code t} ticks after its wind-up: during the wind-up each pin's shadow shows where it will fall; then over
	 * the active ticks the pins fall one by one, {@value #PIN_DAMAGE} damage to a player within reach of where one lands.
	 */
	private void pinRain(ServerLevel level, int t) {
		Attack rain = Attack.PIN_RAIN;
		if (t < 0) {
			if ((t + rain.windup) % 4 == 0) {
				for (Vec3 pin : pins) {
					level.sendParticles(ParticleTypes.SMOKE, pin.x, pin.y + 0.05, pin.z, 2, 0.1, 0.0, 0.1, 0.0);
				}
			}
			return;
		}
		for (int i = 0; i < pins.size(); i++) {
			if (t != i * rain.active / pins.size()) {
				continue;
			}
			Vec3 pin = pins.get(i);
			level.sendParticles(ParticleTypes.CRIT, pin.x, pin.y + 1.2, pin.z, 6, 0.05, 0.6, 0.05, 0.0);
			level.sendParticles(ParticleTypes.END_ROD, pin.x, pin.y + 0.1, pin.z, 2, 0.05, 0.05, 0.05, 0.02);
			level.playSound(null, pin.x, pin.y, pin.z, SoundEvents.CHAIN_HIT, SoundSource.HOSTILE, 0.8F, 1.8F);
			for (ServerPlayer player : nearby(level)) {
				double dx = player.getX() - pin.x;
				double dz = player.getZ() - pin.z;
				if (dx * dx + dz * dz <= PIN_REACH * PIN_REACH && Math.abs(player.getY() - pin.y) < 2.5) {
					player.hurtServer(level, damageSources().thrown(this, this), damage(PIN_DAMAGE));
				}
			}
		}
	}

	public List<Vec3> pins() {
		return List.copyOf(pins);
	}

	/**
	 * Drop Strike, {@code t} ticks into it: in the wind-up she moves over the marked player while her shadow swells on the
	 * lace; then she drops, {@value #DROP_DAMAGE} damage to everyone within {@value #DROP_RADIUS} blocks; then she lies open
	 * on the lace for {@value #DROP_OPEN_TICKS} ticks, and climbs back up her thread at the end.
	 */
	private void dropStrike(ServerLevel level, int t) {
		Attack drop = Attack.DROP_STRIKE;
		if (t < drop.windup) {
			move(dropAt, HANG_SPEED * 2.0);
			double radius = 0.5 + DROP_RADIUS * t / drop.windup;
			if (t % 3 == 0) {
				ring(level, dropAt, radius, ParticleTypes.SMOKE, 16);
			}
		} else if (t == drop.windup) {
			setAction(Action.DROP);
			teleportTo(dropAt.x, floorY(), dropAt.z);
			dropLands(level);
		} else if (t < drop.windup + drop.active + drop.recovery - CLIMB_TICKS) {
			setDeltaMovement(Vec3.ZERO);
			setAction(Action.OPEN);
		} else {
			setAction(Action.CLIMB);
			double rise = (hangY() - floorY()) / CLIMB_TICKS;
			setDeltaMovement(0.0, Math.min(rise, hangY() - getY()), 0.0);
		}
	}

	/** She lands: {@value #DROP_DAMAGE} damage to everyone within {@value #DROP_RADIUS} blocks. */
	public int dropLands(ServerLevel level) {
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			double dx = player.getX() - getX();
			double dz = player.getZ() - getZ();
			if (dx * dx + dz * dz <= DROP_RADIUS * DROP_RADIUS && Math.abs(player.getY() - getY()) < 3.0
					&& player.hurtServer(level, damageSources().mobAttack(this), damage(DROP_DAMAGE))) {
				hit++;
			}
		}
		ring(level, position(), DROP_RADIUS, ParticleTypes.ITEM_COBWEB, 24);
		level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3, getZ(), 12, 1.0, 0.2, 1.0, 0.02);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SPIDER_STEP, SoundSource.HOSTILE, 3.0F, 0.5F);
		return hit;
	}

	public Vec3 dropAt() {
		return dropAt;
	}

	/** Brood: up to {@value #BROOD} egg sacs split, a spiderling from each, never more than {@value #MAX_SPIDERLINGS} at once. */
	private void broodOut(ServerLevel level) {
		List<TatterEggSacEntity> ready = sacs(level);
		java.util.Collections.shuffle(ready, new java.util.Random(getRandom().nextLong()));
		int hatched = 0;
		for (TatterEggSacEntity sac : ready) {
			if (hatched >= BROOD || brood.size() + hatched >= MAX_SPIDERLINGS) {
				break;
			}
			if (sac.hatch(level)) {
				hatched++;
			}
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 2.0F, 1.6F);
	}

	/** A spiderling has come out of one of her egg sacs. */
	public void hatched(SpiderlingEntity spiderling) {
		brood.add(spiderling.getUUID());
	}

	// ---------------------------------------------------------------- her floor

	/**
	 * A segment of the doily round {@code at}: a ring {@value #SEGMENT_WIDTH} blocks wide at its distance from the centre,
	 * or a wedge of {@value #WEDGE_DEGREES} degrees towards it (past the centre's flower). Only lace still there, never the
	 * dense ring round a spool nor the tape's foot.
	 */
	public List<BlockPos> segment(ServerLevel level, boolean ring, Vec3 at) {
		double ax = at.x - origin.getX() - SpindleLoft.DOILY_X;
		double az = at.z - origin.getZ() - SpindleLoft.DOILY_Z;
		double reach = Math.hypot(ax, az);
		double inner = Math.clamp(reach - SEGMENT_WIDTH / 2.0, 0.0, SpindleLoft.DOILY_RADIUS - SEGMENT_WIDTH);
		double toward = Math.atan2(az, ax);
		List<BlockPos> cells = new ArrayList<>();
		int r = (int) Math.ceil(SpindleLoft.DOILY_RADIUS);
		for (int x = (int) Math.floor(SpindleLoft.DOILY_X) - r; x <= (int) Math.floor(SpindleLoft.DOILY_X) + r; x++) {
			for (int z = (int) Math.floor(SpindleLoft.DOILY_Z) - r; z <= (int) Math.floor(SpindleLoft.DOILY_Z) + r; z++) {
				if (SpindleLoft.lace(x, z) < 0 || SpindleLoft.safe(x, z) || SpindleLoft.tapeFoot(x, z)) {
					continue;
				}
				double dx = x + 0.5 - SpindleLoft.DOILY_X;
				double dz = z + 0.5 - SpindleLoft.DOILY_Z;
				double distance = Math.hypot(dx, dz);
				boolean in;
				if (ring) {
					in = distance >= inner && distance < inner + SEGMENT_WIDTH;
				} else {
					double off = Math.abs(Math.IEEEremainder(Math.atan2(dz, dx) - toward, Math.PI * 2.0));
					in = distance >= 2.5 && Math.toDegrees(off) <= WEDGE_DEGREES / 2.0;
				}
				BlockPos cell = origin.offset(x, SpindleLoft.LACE, z);
				if (in && level.getBlockState(cell).is(JugcraftLairs.DOILY_LACE)) {
					cells.add(cell);
				}
			}
		}
		return cells;
	}

	/** Loosening threads along a segment about to drop away. */
	private void fray(ServerLevel level, List<BlockPos> segment) {
		for (int i = 0; i < Math.min(10, segment.size()); i++) {
			BlockPos cell = segment.get(getRandom().nextInt(segment.size()));
			level.sendParticles(ParticleTypes.ITEM_COBWEB, cell.getX() + 0.5, cell.getY() + 0.2, cell.getZ() + 0.5, 1, 0.3, 0.05, 0.3, 0.01);
		}
	}

	/** The segment drops away into the dark, to be knitted back {@value #KNIT_TICKS} ticks from now. */
	public void unravel(ServerLevel level, List<BlockPos> segment) {
		List<BlockPos> cells = new ArrayList<>();
		List<BlockState> states = new ArrayList<>();
		for (BlockPos cell : segment) {
			BlockState state = level.getBlockState(cell);
			if (state.is(JugcraftLairs.DOILY_LACE)) {
				cells.add(cell);
				states.add(state);
				level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		for (int i = 0; i < Math.min(16, cells.size()); i++) {
			BlockPos cell = cells.get(getRandom().nextInt(cells.size()));
			level.sendParticles(ParticleTypes.ITEM_COBWEB, cell.getX() + 0.5, cell.getY(), cell.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.05);
		}
		if (!cells.isEmpty()) {
			BlockPos first = cells.getFirst();
			level.playSound(null, first, SoundEvents.SHEEP_SHEAR, SoundSource.HOSTILE, 2.0F, 0.6F);
		}
		unravelled.add(new Segment(cells, states, level.getGameTime() + KNIT_TICKS));
	}

	/** Knits back every segment whose time has come. */
	private void knitDue(ServerLevel level) {
		long now = level.getGameTime();
		unravelled.removeIf(segment -> {
			if (now < segment.knitAt()) {
				return false;
			}
			knit(level, segment);
			return true;
		});
	}

	private void knit(ServerLevel level, Segment segment) {
		for (int i = 0; i < segment.cells().size(); i++) {
			BlockPos cell = segment.cells().get(i);
			if (level.getBlockState(cell).isAir()) {
				level.setBlock(cell, segment.states().get(i), Block.UPDATE_CLIENTS);
			}
		}
		if (!segment.cells().isEmpty()) {
			BlockPos first = segment.cells().getFirst();
			level.playSound(null, first, SoundEvents.WOOL_PLACE, SoundSource.HOSTILE, 1.5F, 1.2F);
		}
	}

	/** Knits the whole doily back at once: each segment she unravelled, and any lace cell of the template found empty. */
	public void knitAll(ServerLevel level) {
		for (Segment segment : unravelled) {
			knit(level, segment);
		}
		unravelled.clear();
		fraying.clear();
		int r = (int) Math.ceil(SpindleLoft.DOILY_RADIUS);
		for (int x = (int) Math.floor(SpindleLoft.DOILY_X) - r; x <= (int) Math.floor(SpindleLoft.DOILY_X) + r; x++) {
			for (int z = (int) Math.floor(SpindleLoft.DOILY_Z) - r; z <= (int) Math.floor(SpindleLoft.DOILY_Z) + r; z++) {
				int pattern = SpindleLoft.lace(x, z);
				BlockPos cell = origin.offset(x, SpindleLoft.LACE, z);
				if (pattern >= 0 && level.isLoaded(cell) && level.getBlockState(cell).isAir()) {
					level.setBlock(cell, JugcraftLairs.DOILY_LACE.defaultBlockState().setValue(DoilyLaceBlock.PATTERN, pattern), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** How many segments of her doily are down now. */
	public int unravelledSegments() {
		return unravelled.size();
	}

	// ---------------------------------------------------------------- Taking In the Seams, and Frenzied Stitching

	/** Taking In the Seams: she climbs into the threads, the light dims, and she spits a ring of egg sacs round the doily. */
	public void startTakingIn(ServerLevel level) {
		attack = null;
		phase = Phase.TAKING_IN;
		phaseTicks = 0;
		setAction(Action.CLIMB);
		for (ServerPlayer player : nearby(level)) {
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, TAKE_IN_TICKS, 0), this);
		}
		for (TatterEggSacEntity sac : sacs(level)) {
			sac.discard();
		}
		sacs.clear();
		for (Vec3 spot : SpindleLoft.sacs()) {
			Vec3 at = spot.add(origin.getX(), origin.getY(), origin.getZ());
			sacs.add(TatterEggSacEntity.spit(level, this, at).getUUID());
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.4F);
		tell(level, "message.jugcraft.tatterlace.take_in");
	}

	private void takingIn() {
		Vec3 centre = centre();
		double share = Math.min(1.0, phaseTicks / (double) TAKE_IN_TICKS);
		Vec3 goal = new Vec3(centre.x, floorY() + (hangY() - floorY()) * share, centre.z);
		Vec3 to = goal.subtract(position());
		double length = to.length();
		setDeltaMovement(length < 0.05 ? Vec3.ZERO : to.scale(Math.min(0.6, length) / length));
		if (phaseTicks >= TAKE_IN_TICKS) {
			phase = Phase.FINAL;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.HANG);
		}
	}

	/** Frenzied Stitching: her cuffs glow red, she comes down to the lace for good, and her cooldowns shorten by a third. */
	public void startFrenzy(ServerLevel level) {
		attack = null;
		entityData.set(FRENZIED, true);
		phase = Phase.FRENZY;
		phaseTicks = 0;
		teleportTo(getX(), floorY(), getZ());
		setAction(Action.IDLE);
		globalCooldown = cooldown(GLOBAL_COOLDOWN);
		level.sendParticles(ParticleTypes.ITEM_COBWEB, getX(), getY() + 0.5, getZ(), 20, 1.0, 0.3, 1.0, 0.05);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.7F);
		tell(level, "message.jugcraft.tatterlace.frenzy");
	}

	// ---------------------------------------------------------------- her brood and her things

	/** Her living egg sacs. */
	public List<TatterEggSacEntity> sacs(ServerLevel level) {
		List<TatterEggSacEntity> out = new ArrayList<>();
		for (UUID id : sacs) {
			if (level.getEntity(id) instanceof TatterEggSacEntity sac && sac.isAlive()) {
				out.add(sac);
			}
		}
		return out;
	}

	public List<UUID> brood() {
		return List.copyOf(brood);
	}

	/** Forgets what is gone. */
	private void prune(ServerLevel level) {
		sacs.removeIf(id -> !(level.getEntity(id) instanceof TatterEggSacEntity sac) || !sac.isAlive());
		brood.removeIf(id -> !(level.getEntity(id) instanceof SpiderlingEntity spiderling) || !spiderling.isAlive());
		things.removeIf(id -> level.getEntity(id) == null);
	}

	/** Whoever hurt her or her brood has taken part. */
	public void took(ServerPlayer player, float amount) {
		damageBy.merge(player.getUUID(), amount, Float::sum);
	}

	/** Her participants: everyone who hurt her or her brood and is still near her doily. */
	public List<ServerPlayer> participants(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (player.isAlive() && damageBy.getOrDefault(player.getUUID(), 0.0F) > 0.0F && player.position().distanceTo(centre) <= 64.0) {
				out.add(player);
			}
		}
		return out;
	}

	/** Who is fighting her, as her brood needs to know. */
	public @Nullable Player foe(ServerLevel level) {
		return foe == null ? null : level.getPlayerByUUID(foe);
	}

	// ---------------------------------------------------------------- abandoned, and defeated

	/** Left alone: she knits her doily whole, goes back up to her silk, sewing, and heals; her brood and things are gone. */
	public void reset(ServerLevel level) {
		dismiss(level);
		knitAll(level);
		entityData.set(FRENZIED, false);
		phase = Phase.WAITING;
		phaseTicks = 0;
		attack = null;
		abandoned = 0;
		foe = null;
		damageBy.clear();
		Arrays.fill(cooldowns, 0);
		setHealth(getMaxHealth());
		Vec3 perch = perch();
		teleportTo(perch.x, perch.y, perch.z);
		face(0.0F);
		setAction(Action.SEWING);
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.tatterlace.reset");
	}

	/** Her egg sacs and spiderlings shrivel, and her thimbles, threads, snares and spools are gone. */
	private void dismiss(ServerLevel level) {
		for (TatterEggSacEntity sac : sacs(level)) {
			sac.shrivel(level);
		}
		sacs.clear();
		for (UUID id : List.copyOf(brood)) {
			if (level.getEntity(id) instanceof SpiderlingEntity spiderling) {
				spiderling.shrivel(level);
			}
		}
		brood.clear();
		for (UUID id : List.copyOf(things)) {
			Entity thing = level.getEntity(id);
			if (thing instanceof LaceSnareEntity snare) {
				snare.end();
			} else if (thing != null) {
				thing.discard();
			}
		}
		things.clear();
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			defeated(level);
		}
	}

	/**
	 * She has fallen: her brood and things go with her, her doily is knitted whole, each participant gets their loot, her
	 * lair's instance ends (it closes once everyone has left) and a gate of Grey Mist opens in the middle of the doily.
	 */
	private void defeated(ServerLevel level) {
		dismiss(level);
		knitAll(level);
		TatterlaceLoot.reward(level, this, participants(level));
		LairInstance open = lairInstance(level);
		if (open != null) {
			Lairs.end(open);
			BlockState mist = JugcraftLairs.LAIR_EXIT.defaultBlockState().setValue(LairExitBlock.AXIS, Direction.Axis.X);
			for (BlockPos at : exitCells(centre())) {
				level.setBlock(at, mist, Block.UPDATE_ALL);
			}
		}
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.tatterlace.defeated");
	}

	/** Where the Grey Mist opens when she falls: two blocks wide and two high, standing on the middle of the doily. */
	public static List<BlockPos> exitCells(Vec3 centre) {
		BlockPos at = BlockPos.containing(centre.x, centre.y + 1.0, centre.z);
		return List.of(at.west(), at, at.west().above(), at.above());
	}

	/** Tells everyone near her doily. */
	private void tell(ServerLevel level, String key) {
		Vec3 centre = centre();
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= LEASH + 32.0) {
				player.sendSystemMessage(Component.translatable(key));
			}
		}
	}

	// ---------------------------------------------------------------- what she is immune to

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, amount);
		}
		if (phase == Phase.WAITING) {
			if (source.getEntity() instanceof Player player && LairBosses.eligible(player)) {
				wake(level);
			}
			return false;
		}
		if (phase == Phase.DESCENDING || phase == Phase.TAKING_IN) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && source.getEntity() instanceof ServerPlayer player) {
			took(player, amount);
		}
		return hurt;
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
	}

	@Override
	public void makeStuckInBlock(BlockState state, Vec3 multiplier) {
		// No web holds her, nor anything else: she walks on her own threads.
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	protected boolean canRide(Entity vehicle) {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossBar.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossBar.removePlayer(player);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return phase == Phase.WAITING ? null : SoundEvents.SPIDER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.SPIDER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SPIDER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.6F;
	}

	// ---------------------------------------------------------------- particles

	static void ring(ServerLevel level, Vec3 centre, double radius, ParticleOptions particle, int points) {
		for (int i = 0; i < points; i++) {
			double angle = Math.PI * 2.0 * i / points;
			level.sendParticles(particle, centre.x + Math.cos(angle) * radius, centre.y + 0.1, centre.z + Math.sin(angle) * radius, 1, 0.0, 0.02,
					0.0, 0.0);
		}
	}

	// ---------------------------------------------------------------- saving and syncing

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, Action.SEWING.ordinal());
		builder.define(FRENZIED, false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (instance != null) {
			output.store("instance", UUIDUtil.CODEC, instance);
			output.putInt("slot", slot);
		}
		output.store("origin", BlockPos.CODEC, origin);
		output.putString("phase", phase.name());
		output.putBoolean("frenzied", frenzied());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		instance = input.read("instance", UUIDUtil.CODEC).orElse(null);
		slot = input.getIntOr("slot", -1);
		origin = input.read("origin", BlockPos.CODEC).orElse(BlockPos.containing(getX() - SpindleLoft.DOILY_X, getY() - SpindleLoft.LACE,
				getZ() - SpindleLoft.DOILY_Z));
		try {
			phase = Phase.valueOf(input.getStringOr("phase", Phase.WAITING.name()));
		} catch (IllegalArgumentException e) {
			phase = Phase.WAITING;
		}
		if (phase == Phase.DESCENDING) {
			phase = Phase.FITTING;  // a passing moment is not resumed
		} else if (phase == Phase.TAKING_IN) {
			phase = Phase.FINAL;
		}
		entityData.set(FRENZIED, input.getBooleanOr("frenzied", false));
		knitOnLoad = true;
		bossBar.setVisible(phase != Phase.WAITING);
	}

	// ---------------------------------------------------------------- her animations

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<TatterlaceEntity>("body", 4, test -> {
			TatterlaceEntity tatterlace = test.animatable();
			if (tatterlace.isDeadOrDying()) {
				return test.setAndContinue(DEATH);
			}
			Action action = tatterlace.action();
			return test.setAndContinue(action == Action.IDLE && test.isMoving() ? WALK : BODY[action.ordinal()]);
		}));
		controllers.add(new AnimationController<TatterlaceEntity>("cuffs", 8,
				test -> test.setAndContinue(test.animatable().frenzied() ? CUFFS_RED : CUFFS_CALM)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
