package io.github.jimbozoomer.jugcraft.lair.vesperine;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairBosses;
import io.github.jimbozoomer.jugcraft.lair.LairBrazierBlock;
import io.github.jimbozoomer.jugcraft.lair.LairExitBlock;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairMoonBlock;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Vesperine, the Last Reaper (docs/features/vesperine.md): the Hollow Acre's boss. She waits seated on the Bone Throne
 * and rises when someone steps into the Mown Circle, her health scaled for the party, with her two skulls, Dirge and
 * Requiem ({@link ReaperSkullEntity}), at her shoulders. She glides rather than walks and keeps to her arena.
 *
 * <p>Her rule: while both skulls live she takes half damage (a double halo shows it), and while her scythe is thrown
 * she takes a quarter more. Phase 1, the Reaping: Reaping Arc, Harvest Lunge, Scythe Throw and Grave Call, with the
 * skulls' Grief Bolts. At half health, the Last Toll: she rises, a bell tolls, the moon turns red, Darkness falls and
 * any slain skull re-forms at half its health. Phase 2, the Reaping Moon: Twin Beam, Crop Circles and Shadow Step join
 * her arc, lunge and throw, and once, at a quarter health, Death's Harvest: souls stream out of the black wheat to heal
 * her until they are struck down or the four ward braziers are lit again, and then she slams down.
 *
 * <p>Every attack has a wind-up you can read, a strike and a recovery ({@link Attack}); the numbers are here, in one
 * place, and in tools/vesperine.py. Each participant (whoever hurt her or her skulls) gets their own loot
 * ({@link VesperineLoot}); left alone in her arena she goes back to her throne and heals. Bound to a lair instance she
 * vanishes with it; one with no instance (a test's) keeps to where she was put.
 */
public class VesperineEntity extends Monster implements GeoEntity {
	// ---------------------------------------------------------------- numbers (tools/vesperine.py)
	public static final float HEALTH = 400.0F;
	public static final double ARMOR = 10.0;
	public static final double LEASH = 32.0;
	public static final float GUARD = 0.5F;
	public static final float UNARMED = 1.25F;
	public static final double PARTY_STEP = 0.5;
	public static final double PARTY_MAX = 2.5;
	public static final int RISE_TICKS = 40;
	public static final int ABANDON_TICKS = 200;
	public static final int GLOBAL_COOLDOWN = 20;
	public static final double GLIDE_SPEED = 0.22;
	public static final double HOVER = 0.25;
	public static final double KEEP_DISTANCE = 2.5;
	public static final float ARC_DAMAGE = 14.0F;
	public static final double ARC_REACH = 4.5;
	public static final double ARC_HALF_ANGLE = 135.0;
	public static final int ARC_WITHER_TICKS = 60;
	public static final float LUNGE_DAMAGE = 10.0F;
	public static final double LUNGE_DISTANCE = 8.0;
	public static final double LUNGE_SPEED = 1.0;
	public static final double LUNGE_WIDTH = 1.5;
	public static final double THROW_RANGE = 16.0;
	public static final int REARM_TICKS = 160;
	public static final int CALL_THRALLS = 3;
	public static final int MAX_THRALLS = 4;
	public static final int TOLL_TICKS = 60;
	public static final int TOLL_DARKNESS = 60;
	public static final double TOLL_RISE = 6.0;
	public static final float BEAM_DAMAGE = 4.0F;
	public static final int BEAM_INTERVAL = 5;
	public static final int BEAM_SWEEP = 30;
	public static final double BEAM_LENGTH = 16.0;
	public static final double BEAM_WIDTH = 1.0;
	public static final int CIRCLES = 3;
	public static final double CIRCLE_RADIUS = 2.0;
	public static final float CIRCLE_DAMAGE = 12.0F;
	public static final double STEP_BEHIND = 2.0;
	public static final float HARVEST_AT = 0.25F;
	public static final int HARVEST_TICKS = 120;
	public static final int HARVEST_DARKNESS = 120;
	public static final double HARVEST_RISE = 10.0;
	public static final int SOUL_INTERVAL = 6;
	public static final float SOUL_HEAL = 0.02F;
	public static final double WARD_REACH = 3.0;
	public static final float SLAM_DAMAGE = 16.0F;
	public static final double SLAM_RADIUS = 10.0;
	public static final int EXPERIENCE = 300;

	/**
	 * Her attacks: wind-up, the active part and recovery in ticks, the cooldown, the target's nearest and farthest
	 * distance, and the phases she uses it in. The wind-up is what players read; the strike lands as it ends.
	 */
	public enum Attack {
		REAPING_ARC(12, 1, 10, 40, 0.0, 4.0, true, true),
		HARVEST_LUNGE(10, 8, 10, 80, 4.0, 12.0, true, true),
		SCYTHE_THROW(14, 1, 10, 160, 5.0, 16.0, true, true),
		GRAVE_CALL(30, 1, 10, 300, 0.0, 40.0, true, false),
		TWIN_BEAM(30, 40, 10, 200, 0.0, 24.0, false, true),
		CROP_CIRCLES(30, 1, 10, 160, 0.0, 40.0, false, true),
		SHADOW_STEP(10, 1, 0, 140, 0.0, 40.0, false, true);

		public final int windup;
		public final int active;
		public final int recovery;
		public final int cooldown;
		public final double near;
		public final double far;
		public final boolean reaping;
		public final boolean moon;

		Attack(int windup, int active, int recovery, int cooldown, double near, double far, boolean reaping, boolean moon) {
			this.windup = windup;
			this.active = active;
			this.recovery = recovery;
			this.cooldown = cooldown;
			this.near = near;
			this.far = far;
			this.reaping = reaping;
			this.moon = moon;
		}

		public int length() {
			return windup + active + recovery;
		}
	}

	/** The fight's phases. */
	public enum Phase {
		SEATED, RISING, REAPING, TOLL, MOON, HARVEST
	}

	/** What her body is doing, synced for her animations. */
	public enum Action {
		SEATED, RISING, IDLE, ARC_WINDUP, ARC_STRIKE, LUNGE_WINDUP, LUNGE, THROW_WINDUP, THROW, CALL, TOLL, BEAM, CIRCLES, STEP,
		HARVEST, SLAM
	}

	private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(VesperineEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> GUARDS = SynchedEntityData.defineId(VesperineEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> ARMED = SynchedEntityData.defineId(VesperineEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation[] BODY = new RawAnimation[Action.values().length];
	private static final RawAnimation GLIDE = RawAnimation.begin().thenLoop("animation.vesperine.glide");
	private static final RawAnimation[] HALO = {RawAnimation.begin().thenLoop("animation.vesperine.halo_none"),
			RawAnimation.begin().thenLoop("animation.vesperine.halo_cracked"), RawAnimation.begin().thenLoop("animation.vesperine.halo_double")};
	private static final RawAnimation WIELDED = RawAnimation.begin().thenLoop("animation.vesperine.armed");
	private static final RawAnimation THROWN = RawAnimation.begin().thenLoop("animation.vesperine.unarmed");

	static {
		String prefix = "animation.vesperine.";
		BODY[Action.SEATED.ordinal()] = RawAnimation.begin().thenLoop(prefix + "seated");
		BODY[Action.RISING.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "rise");
		BODY[Action.IDLE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "idle");
		BODY[Action.ARC_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "arc_windup");
		BODY[Action.ARC_STRIKE.ordinal()] = RawAnimation.begin().then(prefix + "arc_strike", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.LUNGE_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "lunge_windup");
		BODY[Action.LUNGE.ordinal()] = RawAnimation.begin().thenLoop(prefix + "lunge");
		BODY[Action.THROW_WINDUP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "throw_windup");
		BODY[Action.THROW.ordinal()] = RawAnimation.begin().then(prefix + "throw", LoopType.PLAY_ONCE).thenLoop(prefix + "idle");
		BODY[Action.CALL.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "call");
		BODY[Action.TOLL.ordinal()] = RawAnimation.begin().thenLoop(prefix + "toll");
		BODY[Action.BEAM.ordinal()] = RawAnimation.begin().thenLoop(prefix + "beam");
		BODY[Action.CIRCLES.ordinal()] = RawAnimation.begin().thenLoop(prefix + "circles");
		BODY[Action.STEP.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "step");
		BODY[Action.HARVEST.ordinal()] = RawAnimation.begin().thenLoop(prefix + "harvest");
		BODY[Action.SLAM.ordinal()] = RawAnimation.begin().thenPlayAndHold(prefix + "slam");
	}

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private final ServerBossEvent bossBar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.jugcraft.vesperine"),
			BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
	/** The lair instance she belongs to (null for one put down by a test or a command). */
	private @Nullable UUID instance;
	private int slot = -1;
	/** The arena's centre, at its floor, and her seat on the throne. */
	private Vec3 anchor = Vec3.ZERO;
	private Vec3 seat = Vec3.ZERO;
	private float seatYaw;
	private Phase phase = Phase.SEATED;
	private int phaseTicks;
	private boolean tolled;
	private boolean harvested;
	private @Nullable Attack attack;
	private int attackTicks;
	private int globalCooldown;
	private final int[] cooldowns = new int[Attack.values().length];
	private @Nullable UUID foe;
	private int abandoned;
	private @Nullable UUID dirge;
	private @Nullable UUID requiem;
	private final List<UUID> thralls = new ArrayList<>();
	private final List<UUID> souls = new ArrayList<>();
	private @Nullable UUID scythe;
	private int unarmedTicks;
	private final Map<UUID, Float> damageBy = new HashMap<>();
	private @Nullable UUID lastAttacker;
	private Vec3 lungeDirection = Vec3.ZERO;
	private double lunged;
	private final Set<UUID> struck = new HashSet<>();
	private final List<Vec3> circles = new ArrayList<>();
	private final List<Vec3> spots = new ArrayList<>();
	private Vec3 beamFrom = Vec3.ZERO;
	private Vec3 beamTo = Vec3.ZERO;

	public VesperineEntity(EntityType<? extends VesperineEntity> type, Level level) {
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
				.add(Attributes.ATTACK_DAMAGE, ARC_DAMAGE);
	}

	/**
	 * Seats her on the throne of {@code instance}'s Hollow Acre, facing the circle, with her skulls at her shoulders.
	 * Called as the instance is placed. Placing the template puts back only its own blocks, so the Grey Mist a fallen
	 * reaper opened in this slot before still stands where the template has none: it goes first.
	 */
	public static VesperineEntity summon(ServerLevel level, LairInstance instance) {
		BlockPos origin = instance.origin();
		Vec3 arena = new Vec3(origin.getX() + HollowAcre.ARENA_X, origin.getY() + HollowAcre.FLOOR, origin.getZ() + HollowAcre.ARENA_Z);
		for (BlockPos at : exitCells(arena)) {
			if (level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT)) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		BlockPos throne = origin.offset(HollowAcre.THRONE);
		Vec3 seat = new Vec3(throne.getX() + 0.5, throne.getY() + 0.3, throne.getZ() + 0.5);
		return summon(level, seat, 0.0F, arena, instance);
	}

	/** Seats her at {@code seat}, facing {@code yaw}, with her arena round {@code arena} (tests pass no instance). */
	public static VesperineEntity summon(ServerLevel level, Vec3 seat, float yaw, Vec3 arena, @Nullable LairInstance instance) {
		VesperineEntity vesperine = new VesperineEntity(JugcraftVesperine.VESPERINE, level);
		vesperine.seat = seat;
		vesperine.seatYaw = yaw;
		vesperine.anchor = arena;
		if (instance != null) {
			vesperine.instance = instance.id;
			vesperine.slot = instance.slot;
		}
		vesperine.snapTo(seat.x, seat.y, seat.z, yaw, 0.0F);
		vesperine.setYHeadRot(yaw);
		vesperine.setYBodyRot(yaw);
		level.addFreshEntity(vesperine);
		vesperine.spawnSkulls(level, 1.0);
		return vesperine;
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

	/** How many of her skulls live (2 is the double halo, her guard). */
	public int guards() {
		return entityData.get(GUARDS);
	}

	public boolean armed() {
		return entityData.get(ARMED);
	}

	public boolean tolled() {
		return tolled;
	}

	public boolean harvested() {
		return harvested;
	}

	public Vec3 anchor() {
		return anchor;
	}

	public @Nullable UUID instanceId() {
		return instance;
	}

	/** Damage she deals: the attack's own, times {@code lairs.boss_damage} ({@link LairBosses}). */
	public static float damage(float base) {
		return LairBosses.damage(base);
	}

	/** Her health (and her skulls') for a party of {@code players}: half as much again for each beyond the first, at most 2.5 times. */
	public static double partyScale(int players) {
		return LairBosses.partyScale(players, PARTY_STEP, PARTY_MAX);
	}

	/** Whoever may fight her: alive, not a spectator, not invulnerable (creative). */
	public static boolean eligible(Player player) {
		return LairBosses.eligible(player);
	}

	// ---------------------------------------------------------------- the server's tick

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (!bound(level)) {
			discard();
			return;
		}
		resetFallDistance();
		bossBar.setProgress(getHealth() / getMaxHealth());
		entityData.set(GUARDS, skullsAlive(level));
		prune(level);
		if (!armed() && ++unarmedTicks > REARM_TICKS) {
			catchScythe();  // the scythe was lost (its chunk unloaded): she draws it back to her
		}
		for (int i = 0; i < cooldowns.length; i++) {
			if (cooldowns[i] > 0) {
				cooldowns[i]--;
			}
		}
		phaseTicks++;
		switch (phase) {
			case SEATED -> seated(level);
			case RISING -> rising();
			case REAPING, MOON -> fight(level);
			case TOLL -> toll(level);
			case HARVEST -> harvest(level);
		}
	}

	/** Whether her instance (if she has one) is still open. */
	private boolean bound(ServerLevel level) {
		if (instance == null) {
			return true;
		}
		Lair lair = Lair.of(level.dimension());
		LairInstance open = lair == null ? null : Lairs.instance(lair, slot);
		return open != null && open.id.equals(instance);
	}

	private @Nullable LairInstance lairInstance(ServerLevel level) {
		Lair lair = Lair.of(level.dimension());
		LairInstance open = lair == null || instance == null ? null : Lairs.instance(lair, slot);
		return open != null && open.id.equals(instance) ? open : null;
	}

	private void seated(ServerLevel level) {
		setDeltaMovement(Vec3.ZERO);
		if (position().distanceToSqr(seat) > 0.01) {
			setPos(seat.x, seat.y, seat.z);
		}
		face(seatYaw);
		setAction(Action.SEATED);
		if (tickCount % 10 == 0 && intruder(level) != null) {
			wake(level);
		}
	}

	/** Someone who may fight her, standing in the Mown Circle. */
	private @Nullable Player intruder(ServerLevel level) {
		for (Player player : level.players()) {
			double dx = player.getX() - anchor.x;
			double dz = player.getZ() - anchor.z;
			if (eligible(player) && dx * dx + dz * dz <= HollowAcre.ARENA_RADIUS * HollowAcre.ARENA_RADIUS
					&& Math.abs(player.getY() - anchor.y) < 8.0) {
				return player;
			}
		}
		return null;
	}

	/**
	 * She rises from her throne: her health (and her skulls') scaled for the players in her arena, her bar shown, and
	 * the fight begins.
	 */
	public void wake(ServerLevel level) {
		if (phase != Phase.SEATED) {
			return;
		}
		double scale = partyScale(Math.max(1, nearby(level).size()));
		AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(HEALTH * scale);
		}
		setHealth(getMaxHealth());
		spawnSkulls(level, scale);
		for (ReaperSkullEntity skull : skulls(level)) {
			skull.scale(scale, 1.0F);
		}
		phase = Phase.RISING;
		phaseTicks = 0;
		setAction(Action.RISING);
		bossBar.setVisible(true);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 2.0F, 0.5F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2.0F, 0.6F);
		tell(level, "message.jugcraft.vesperine.rises");
	}

	private void rising() {
		setDeltaMovement(0.0, phaseTicks < RISE_TICKS / 2 ? 0.03 : 0.0, 0.0);
		if (phaseTicks >= RISE_TICKS) {
			phase = tolled ? Phase.MOON : Phase.REAPING;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
		}
	}

	/** Players near enough to her arena to be fighting (or about to). */
	private List<ServerPlayer> nearby(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (eligible(player) && player.position().distanceTo(anchor) <= LEASH + 8.0) {
				out.add(player);
			}
		}
		return out;
	}

	// ---------------------------------------------------------------- fighting

	private void fight(ServerLevel level) {
		Player target = target(level);
		if (target == null) {
			attack = null;
			setAction(Action.IDLE);
			glide(new Vec3(anchor.x, anchor.y + HOVER, anchor.z), GLIDE_SPEED);
			if (++abandoned >= ABANDON_TICKS) {
				reset(level);
			}
			return;
		}
		abandoned = 0;
		if (attack == null && !tolled && getHealth() <= getMaxHealth() * 0.5F) {
			startToll(level);
			return;
		}
		if (attack == null && tolled && !harvested && getHealth() <= getMaxHealth() * HARVEST_AT) {
			startHarvest(level);
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

	/** Her target: the one she has while they are still in reach of her arena, else the nearest who may fight her. */
	private @Nullable Player target(ServerLevel level) {
		if (foe != null) {
			Player held = level.getPlayerByUUID(foe);
			if (held != null && eligible(held) && held.position().distanceTo(anchor) <= LEASH + 8.0) {
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

	/** Glides toward a point {@value #KEEP_DISTANCE} blocks short of {@code target}, never past her leash. */
	private void approach(Player target) {
		Vec3 to = target.position().subtract(position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 goal = position();
		if (flat > KEEP_DISTANCE + 0.5) {
			goal = new Vec3(target.getX() - to.x / flat * KEEP_DISTANCE, anchor.y + HOVER, target.getZ() - to.z / flat * KEEP_DISTANCE);
		} else {
			goal = new Vec3(getX(), anchor.y + HOVER, getZ());
		}
		glide(leashed(goal), GLIDE_SPEED);
	}

	/** {@code goal}, pulled back within her leash of the arena's centre. */
	private Vec3 leashed(Vec3 goal) {
		Vec3 off = goal.subtract(anchor);
		double flat = Math.sqrt(off.x * off.x + off.z * off.z);
		if (flat <= LEASH) {
			return goal;
		}
		return new Vec3(anchor.x + off.x / flat * LEASH, goal.y, anchor.z + off.z / flat * LEASH);
	}

	private void glide(Vec3 goal, double speed) {
		Vec3 to = goal.subtract(position());
		double length = to.length();
		setDeltaMovement(length < 0.05 ? Vec3.ZERO : to.scale(Math.min(speed, length) / length));
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
			boolean phaseAllows = phase == Phase.MOON ? candidate.moon : candidate.reaping;
			if (!phaseAllows || cooldowns[candidate.ordinal()] > 0 || distance < candidate.near || distance > candidate.far) {
				continue;
			}
			boolean usable = switch (candidate) {
				case REAPING_ARC, HARVEST_LUNGE, SCYTHE_THROW -> armed();
				case GRAVE_CALL -> thralls.size() < MAX_THRALLS;
				case TWIN_BEAM -> skullsAlive(level) > 0;
				case CROP_CIRCLES -> true;
				case SHADOW_STEP -> armed() && stepTarget(level) != null;
			};
			if (usable) {
				ready.add(candidate);
				if (candidate == Attack.REAPING_ARC) {
					ready.add(candidate);  // close in, the arc comes most often
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
		cooldowns[next.ordinal()] = next.cooldown;
		setDeltaMovement(Vec3.ZERO);
		face(target);
		switch (next) {
			case REAPING_ARC -> {
				setAction(Action.ARC_WINDUP);
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8F, 0.5F);
			}
			case HARVEST_LUNGE -> {
				setAction(Action.LUNGE_WINDUP);
				Vec3 to = target.position().subtract(position());
				double flat = Math.sqrt(to.x * to.x + to.z * to.z);
				lungeDirection = flat < 1.0E-3 ? facing() : new Vec3(to.x / flat, 0.0, to.z / flat);
				lunged = 0.0;
				struck.clear();
			}
			case SCYTHE_THROW -> setAction(Action.THROW_WINDUP);
			case GRAVE_CALL -> {
				setAction(Action.CALL);
				spots.clear();
				for (int i = 0; i < CALL_THRALLS; i++) {
					double angle = getRandom().nextDouble() * Math.PI * 2.0;
					double reach = 3.0 + getRandom().nextDouble() * 3.0;
					spots.add(leashed(new Vec3(target.getX() + Math.cos(angle) * reach, anchor.y, target.getZ() + Math.sin(angle) * reach)));
				}
				level.playSound(null, getX(), getY(), getZ(), SoundEvents.SKELETON_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.5F);
			}
			case TWIN_BEAM -> {
				setAction(Action.BEAM);
				Vec3 across = new Vec3(-facing().z, 0.0, facing().x);
				Vec3 middle = new Vec3(target.getX(), anchor.y, target.getZ());
				beamFrom = middle.subtract(across.scale(BEAM_LENGTH / 2.0));
				beamTo = middle.add(across.scale(BEAM_LENGTH / 2.0));
			}
			case CROP_CIRCLES -> {
				setAction(Action.CIRCLES);
				circles.clear();
				List<ServerPlayer> marked = nearby(level);
				java.util.Collections.shuffle(marked, new java.util.Random(getRandom().nextLong()));
				for (ServerPlayer player : marked.subList(0, Math.min(CIRCLES, marked.size()))) {
					circles.add(new Vec3(player.getX(), anchor.y, player.getZ()));
				}
			}
			case SHADOW_STEP -> {
				setAction(Action.STEP);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 30, 0.5, 1.0, 0.5, 0.02);
				Player behind = stepTarget(level);
				if (behind != null) {
					foe = behind.getUUID();
					level.playSound(null, behind.getX(), behind.getY(), behind.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.5F, 1.4F);
				}
			}
		}
	}

	/** The player who last hurt her, while they may still be fought, for Shadow Step. */
	private @Nullable Player stepTarget(ServerLevel level) {
		if (lastAttacker == null) {
			return null;
		}
		Player player = level.getPlayerByUUID(lastAttacker);
		return player != null && eligible(player) && player.position().distanceTo(anchor) <= LEASH + 8.0 ? player : null;
	}

	private void perform(ServerLevel level, Player target) {
		Attack current = attack;
		if (current == null) {
			return;
		}
		int t = attackTicks;
		switch (current) {
			case REAPING_ARC -> {
				setDeltaMovement(Vec3.ZERO);
				if (t < current.windup) {
					face(target);
				} else if (t == current.windup) {
					setAction(Action.ARC_STRIKE);
					reapingArc(level);
				}
			}
			case HARVEST_LUNGE -> {
				if (t < current.windup) {
					setDeltaMovement(Vec3.ZERO);
				} else if (t < current.windup + current.active && lunged < LUNGE_DISTANCE) {
					setAction(Action.LUNGE);
					lunge(level);
				} else {
					setDeltaMovement(Vec3.ZERO);
					setAction(Action.IDLE);
				}
			}
			case SCYTHE_THROW -> {
				setDeltaMovement(Vec3.ZERO);
				if (t < current.windup) {
					face(target);
				} else if (t == current.windup) {
					setAction(Action.THROW);
					throwScythe(level, target);
				}
			}
			case GRAVE_CALL -> {
				setDeltaMovement(Vec3.ZERO);
				if (t < current.windup) {
					if (t % 4 == 0) {
						for (Vec3 spot : spots) {
							level.sendParticles(ParticleTypes.SOUL, spot.x, spot.y + 0.1, spot.z, 4, 0.4, 0.1, 0.4, 0.02);
							level.sendParticles(ParticleTypes.LARGE_SMOKE, spot.x, spot.y + 0.1, spot.z, 2, 0.3, 0.05, 0.3, 0.01);
						}
						level.playSound(null, spots.getFirst().x, spots.getFirst().y, spots.getFirst().z, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
								SoundSource.HOSTILE, 0.5F, 0.5F);
					}
				} else if (t == current.windup) {
					graveCall(level);
				}
			}
			case TWIN_BEAM -> {
				setDeltaMovement(Vec3.ZERO);
				twinBeam(level, t - current.windup);
			}
			case CROP_CIRCLES -> {
				setDeltaMovement(Vec3.ZERO);
				if (t < current.windup) {
					if (t % 3 == 0) {
						for (Vec3 circle : circles) {
							ring(level, circle, CIRCLE_RADIUS, ParticleTypes.END_ROD, 12);
						}
					}
				} else if (t == current.windup) {
					cropCircles(level);
				}
			}
			case SHADOW_STEP -> {
				setDeltaMovement(Vec3.ZERO);
				if (t == current.windup) {
					Player behind = stepTarget(level);
					if (behind != null) {
						shadowStep(level, behind);
						Attack arc = Attack.REAPING_ARC;
						begin(level, arc, behind);
						cooldowns[arc.ordinal()] = 0;
						return;
					}
				}
			}
		}
		if (attack == current && attackTicks >= current.length()) {
			attack = null;
			attackTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			if (action() != Action.ARC_STRIKE && action() != Action.THROW) {
				setAction(Action.IDLE);
			}
		}
	}

	/** Reaping Arc: every player within {@value #ARC_REACH} blocks and {@value #ARC_HALF_ANGLE} degrees of where she faces. */
	public int reapingArc(ServerLevel level) {
		int hit = 0;
		Vec3 facing = facing();
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(ARC_REACH + 1.0, 3.0, ARC_REACH + 1.0))) {
			if (!eligible(player)) {
				continue;
			}
			Vec3 to = player.position().subtract(position());
			double flat = Math.sqrt(to.x * to.x + to.z * to.z);
			if (flat > ARC_REACH + player.getBbWidth() / 2.0 || Math.abs(to.y) > 3.0) {
				continue;
			}
			double cos = flat < 1.0E-3 ? 1.0 : (to.x * facing.x + to.z * facing.z) / flat;
			if (cos < Math.cos(Math.toRadians(ARC_HALF_ANGLE))) {
				continue;
			}
			if (player.hurtServer(level, damageSources().mobAttack(this), damage(ARC_DAMAGE))) {
				player.addEffect(new MobEffectInstance(MobEffects.WITHER, ARC_WITHER_TICKS, 0), this);
				hit++;
			}
		}
		for (int i = -6; i <= 6; i++) {
			double angle = Math.toRadians(getYRot()) + Math.toRadians(ARC_HALF_ANGLE) * i / 6.0;
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() - Math.sin(angle) * 3.5, getY() + 1.2, getZ() + Math.cos(angle) * 3.5, 1,
					0.0, 0.0, 0.0, 0.0);
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.6F);
		return hit;
	}

	/** One tick of the Harvest Lunge: a dash along its line, striking each player she passes once. */
	private void lunge(ServerLevel level) {
		double step = Math.min(LUNGE_SPEED, LUNGE_DISTANCE - lunged);
		setDeltaMovement(lungeDirection.scale(step));
		lunged += step;
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 3, 0.3, 0.4, 0.3, 0.0);
		level.sendParticles(new DustParticleOptions(0x1A1020, 1.4F), getX(), getY() + 1.0, getZ(), 4, 0.4, 0.6, 0.4, 0.0);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(LUNGE_WIDTH, 1.0, LUNGE_WIDTH))) {
			if (eligible(player) && struck.add(player.getUUID())) {
				player.hurtServer(level, damageSources().mobAttack(this), damage(LUNGE_DAMAGE));
			}
		}
		if (lunged == step) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 2.0F, 0.6F);
		}
	}

	/** Scythe Throw: the scythe spins out at {@code target} and back; she is unarmed until it returns. */
	private void throwScythe(ServerLevel level, Player target) {
		Vec3 hand = hand();
		Vec3 aim = target.getBoundingBox().getCenter().subtract(hand);
		ThrownScytheEntity thrown = ThrownScytheEntity.launch(level, this, hand, aim);
		scythe = thrown.getUUID();
		entityData.set(ARMED, false);
		unarmedTicks = 0;
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 1.5F, 0.5F);
	}

	/** Where her right hand holds the scythe. */
	public Vec3 hand() {
		Vec3 facing = facing();
		Vec3 right = new Vec3(-facing.z, 0.0, facing.x);
		return position().add(right.scale(-0.6)).add(facing.scale(0.4)).add(0.0, 1.6, 0.0);
	}

	/** The scythe is back in her hand. */
	public void catchScythe() {
		entityData.set(ARMED, true);
		scythe = null;
		unarmedTicks = 0;
	}

	/** Grave Call: thralls climb out where the soil broke, never more than {@value #MAX_THRALLS} at once. */
	private void graveCall(ServerLevel level) {
		for (Vec3 spot : spots) {
			if (thralls.size() >= MAX_THRALLS) {
				break;
			}
			GraveThrallEntity thrall = GraveThrallEntity.rise(level, this, spot);
			thralls.add(thrall.getUUID());
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SKELETON_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.4F);
	}

	/**
	 * Twin Beam, {@code t} ticks after its wind-up (the red line it traced): the skulls' soul fire sweeps along the line
	 * for {@value #BEAM_SWEEP} ticks and the swept part burns until it ends, {@value #BEAM_DAMAGE} damage every
	 * {@value #BEAM_INTERVAL} ticks to anyone standing in it.
	 */
	private void twinBeam(ServerLevel level, int t) {
		if (t < 0) {
			if ((t + Attack.TWIN_BEAM.windup) % 3 == 0) {
				line(level, beamFrom, beamTo, 1.0, new DustParticleOptions(0xC0141E, 1.0F));
			}
			return;
		}
		if (t >= Attack.TWIN_BEAM.active) {
			return;
		}
		double swept = Math.min(1.0, (t + 1) / (double) BEAM_SWEEP);
		Vec3 head = beamFrom.add(beamTo.subtract(beamFrom).scale(swept));
		line(level, beamFrom, head, swept, ParticleTypes.SOUL_FIRE_FLAME);
		for (ReaperSkullEntity skull : skulls(level)) {
			line(level, skull.position().add(0.0, 0.5, 0.0), head, 0.25, ParticleTypes.SOUL_FIRE_FLAME);
		}
		if (t % BEAM_INTERVAL == 0) {
			for (ServerPlayer player : nearby(level)) {
				if (distanceToSegment(player.position(), beamFrom, head) <= BEAM_WIDTH && Math.abs(player.getY() - anchor.y) < 2.5) {
					player.hurtServer(level, damageSources().mobAttack(this), damage(BEAM_DAMAGE));
				}
			}
			level.playSound(null, head.x, head.y, head.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.6F, 0.6F);
		}
	}

	/** The flat distance from {@code point} to the segment {@code a}-{@code b}. */
	public static double distanceToSegment(Vec3 point, Vec3 a, Vec3 b) {
		double abx = b.x - a.x;
		double abz = b.z - a.z;
		double length = abx * abx + abz * abz;
		double s = length < 1.0E-6 ? 0.0 : Math.clamp(((point.x - a.x) * abx + (point.z - a.z) * abz) / length, 0.0, 1.0);
		double dx = point.x - (a.x + abx * s);
		double dz = point.z - (a.z + abz * s);
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** Crop Circles erupt: {@value #CIRCLE_DAMAGE} damage to anyone still inside one. */
	public int cropCircles(ServerLevel level) {
		int hit = 0;
		for (Vec3 circle : circles) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, circle.x, circle.y + 0.5, circle.z, 40, CIRCLE_RADIUS * 0.5, 0.8, CIRCLE_RADIUS * 0.5, 0.05);
			level.playSound(null, circle.x, circle.y, circle.z, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2.0F, 0.8F);
			for (ServerPlayer player : nearby(level)) {
				double dx = player.getX() - circle.x;
				double dz = player.getZ() - circle.z;
				if (dx * dx + dz * dz <= CIRCLE_RADIUS * CIRCLE_RADIUS && Math.abs(player.getY() - circle.y) < 3.0
						&& player.hurtServer(level, damageSources().mobAttack(this), damage(CIRCLE_DAMAGE))) {
					hit++;
				}
			}
		}
		return hit;
	}

	public List<Vec3> circles() {
		return List.copyOf(circles);
	}

	/** Shadow Step: she is behind {@code player}, {@value #STEP_BEHIND} blocks back from where they look. */
	private void shadowStep(ServerLevel level, Player player) {
		double yaw = Math.toRadians(player.getYRot());
		Vec3 behind = new Vec3(player.getX() + Math.sin(yaw) * STEP_BEHIND, anchor.y + HOVER, player.getZ() - Math.cos(yaw) * STEP_BEHIND);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 30, 0.5, 1.0, 0.5, 0.02);
		teleportTo(behind.x, behind.y, behind.z);
		face(player);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 30, 0.5, 1.0, 0.5, 0.02);
	}

	// ---------------------------------------------------------------- the Last Toll and Death's Harvest

	/** The Last Toll: she rises, the bell tolls, the moon turns red, Darkness falls and slain skulls re-form at half health. */
	public void startToll(ServerLevel level) {
		tolled = true;
		attack = null;
		phase = Phase.TOLL;
		phaseTicks = 0;
		setAction(Action.TOLL);
		moon(level, true);
		for (ServerPlayer player : nearby(level)) {
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, TOLL_DARKNESS, 0), this);
		}
		reformSkulls(level);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 4.0F, 0.5F);
		tell(level, "message.jugcraft.vesperine.toll");
	}

	private void toll(ServerLevel level) {
		glide(new Vec3(anchor.x, anchor.y + TOLL_RISE, anchor.z), 0.2);
		if (phaseTicks == 20 || phaseTicks == 40) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 4.0F, 0.5F);
		}
		if (phaseTicks >= TOLL_TICKS) {
			phase = Phase.MOON;
			phaseTicks = 0;
			globalCooldown = GLOBAL_COOLDOWN;
			setAction(Action.IDLE);
		}
	}

	/** Death's Harvest: she rises toward the moon, the wards go out and souls stream from the black wheat to heal her. */
	public void startHarvest(ServerLevel level) {
		harvested = true;
		attack = null;
		phase = Phase.HARVEST;
		phaseTicks = 0;
		setAction(Action.HARVEST);
		for (ServerPlayer player : nearby(level)) {
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, HARVEST_DARKNESS, 0), this);
		}
		for (BlockPos ward : wards(level)) {
			BlockState state = level.getBlockState(ward);
			if (state.is(JugcraftLairs.LAIR_BRAZIER)) {
				level.setBlock(ward, state.setValue(LairBrazierBlock.LIT, false), Block.UPDATE_ALL);
			}
		}
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 4.0F, 0.4F);
		tell(level, "message.jugcraft.vesperine.harvest");
	}

	private void harvest(ServerLevel level) {
		glide(new Vec3(anchor.x, anchor.y + HARVEST_RISE, anchor.z), 0.25);
		if (phaseTicks % SOUL_INTERVAL == 0 && phaseTicks < HARVEST_TICKS) {
			Vec3 from = soulSource(level);
			if (from != null) {
				souls.add(HarvestSoulEntity.release(level, this, from).getUUID());
			}
		}
		List<BlockPos> wards = wards(level);
		boolean warded = !wards.isEmpty() && wards.stream().allMatch(ward -> lit(level, ward));
		if (phaseTicks >= HARVEST_TICKS || warded) {
			slam(level);
		}
	}

	/** Where the next soul rises: a stalk of black wheat in the field (or, with no lair, a ring round her arena). */
	private @Nullable Vec3 soulSource(ServerLevel level) {
		LairInstance open = lairInstance(level);
		if (open == null) {
			double angle = getRandom().nextDouble() * Math.PI * 2.0;
			return new Vec3(anchor.x + Math.cos(angle) * 14.0, anchor.y + 0.5, anchor.z + Math.sin(angle) * 14.0);
		}
		BlockPos origin = open.origin();
		for (int tries = 0; tries < 8; tries++) {
			int x = 4 + getRandom().nextInt(56);
			if (x >= HollowAcre.PATH_WEST - 1 && x <= HollowAcre.PATH_EAST + 1) {
				continue;
			}
			int z = HollowAcre.FIELD_NORTH + getRandom().nextInt(HollowAcre.FIELD_SOUTH - HollowAcre.FIELD_NORTH + 1);
			BlockPos at = origin.offset(x, HollowAcre.FLOOR, z);
			if (level.getBlockState(at).is(JugcraftLairs.BLACK_WHEAT)) {
				return Vec3.atBottomCenterOf(at).add(0.0, 0.5, 0.0);
			}
		}
		return null;
	}

	/** A soul reached her: she heals {@value #SOUL_HEAL} of her health. */
	public void soulArrived(HarvestSoulEntity soul) {
		heal(getMaxHealth() * SOUL_HEAL);
		souls.remove(soul.getUUID());
		if (level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.5, getZ(), 6, 0.4, 0.6, 0.4, 0.02);
		}
	}

	/** The four ward braziers of her lair (none with no lair). */
	public List<BlockPos> wards(ServerLevel level) {
		LairInstance open = lairInstance(level);
		if (open == null) {
			return List.of();
		}
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos ward : HollowAcre.WARDS) {
			out.add(open.origin().offset(ward));
		}
		return out;
	}

	public static boolean lit(ServerLevel level, BlockPos ward) {
		BlockState state = level.getBlockState(ward);
		return state.is(JugcraftLairs.LAIR_BRAZIER) && state.getValue(LairBrazierBlock.LIT);
	}

	/** The harvest ends: the souls fade and she slams down, {@value #SLAM_DAMAGE} damage at the centre, none past {@value #SLAM_RADIUS} blocks. */
	public int slam(ServerLevel level) {
		for (UUID id : List.copyOf(souls)) {
			if (level.getEntity(id) instanceof HarvestSoulEntity soul) {
				soul.fade(level);
			}
		}
		souls.clear();
		teleportTo(anchor.x, anchor.y + HOVER, anchor.z);
		setAction(Action.SLAM);
		int hit = 0;
		for (ServerPlayer player : nearby(level)) {
			double distance = Math.sqrt(player.distanceToSqr(anchor.x, player.getY(), anchor.z));
			float amount = slamDamage(distance);
			if (amount > 0.0F && Math.abs(player.getY() - anchor.y) < 4.0
					&& player.hurtServer(level, damageSources().mobAttack(this), damage(amount))) {
				hit++;
			}
		}
		ring(level, anchor, 3.0, ParticleTypes.SOUL_FIRE_FLAME, 30);
		ring(level, anchor, 6.0, ParticleTypes.SOUL_FIRE_FLAME, 40);
		ring(level, anchor, 9.0, ParticleTypes.LARGE_SMOKE, 50);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 0.6F);
		phase = Phase.MOON;
		phaseTicks = 0;
		globalCooldown = GLOBAL_COOLDOWN * 2;
		return hit;
	}

	/** The slam's damage {@code distance} blocks from the centre: {@value #SLAM_DAMAGE} there, falling to none at {@value #SLAM_RADIUS}. */
	public static float slamDamage(double distance) {
		return distance >= SLAM_RADIUS ? 0.0F : (float) (SLAM_DAMAGE * (1.0 - distance / SLAM_RADIUS));
	}

	/** Turns her lair's moon red (or pale again). */
	private void moon(ServerLevel level, boolean red) {
		LairInstance open = lairInstance(level);
		if (open == null || open.lair.moon == null) {
			return;
		}
		BlockPos centre = open.origin().offset(open.lair.moon);
		int r = open.lair.moonRadius;
		for (int dx = -r; dx <= r; dx++) {
			for (int dy = -r; dy <= r; dy++) {
				BlockPos at = centre.offset(dx, dy, 0);
				BlockState state = level.getBlockState(at);
				if (state.is(JugcraftLairs.LAIR_MOON) && state.getValue(LairMoonBlock.RED) != red) {
					level.setBlock(at, state.setValue(LairMoonBlock.RED, red), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	// ---------------------------------------------------------------- her skulls and her servants

	private void spawnSkulls(ServerLevel level, double scale) {
		if (dirge == null || !(level.getEntity(dirge) instanceof ReaperSkullEntity)) {
			dirge = ReaperSkullEntity.summon(level, JugcraftVesperine.DIRGE, this, scale, 1.0F).getUUID();
		}
		if (requiem == null || !(level.getEntity(requiem) instanceof ReaperSkullEntity)) {
			requiem = ReaperSkullEntity.summon(level, JugcraftVesperine.REQUIEM, this, scale, 1.0F).getUUID();
		}
	}

	/** Slain skulls re-form at half their health (the Last Toll). */
	private void reformSkulls(ServerLevel level) {
		double scale = partyScale(Math.max(1, nearby(level).size()));
		if (dirge == null || !(level.getEntity(dirge) instanceof ReaperSkullEntity skull) || !skull.isAlive()) {
			dirge = ReaperSkullEntity.summon(level, JugcraftVesperine.DIRGE, this, scale, 0.5F).getUUID();
		}
		if (requiem == null || !(level.getEntity(requiem) instanceof ReaperSkullEntity skull) || !skull.isAlive()) {
			requiem = ReaperSkullEntity.summon(level, JugcraftVesperine.REQUIEM, this, scale, 0.5F).getUUID();
		}
	}

	/** Her living skulls. */
	public List<ReaperSkullEntity> skulls(ServerLevel level) {
		List<ReaperSkullEntity> out = new ArrayList<>();
		for (UUID id : new UUID[] {dirge, requiem}) {
			if (id != null && level.getEntity(id) instanceof ReaperSkullEntity skull && skull.isAlive()) {
				out.add(skull);
			}
		}
		return out;
	}

	public int skullsAlive(ServerLevel level) {
		return skulls(level).size();
	}

	/** Where a skull drifts: beside her shoulder (Dirge on her left, Requiem on her right), bobbing. */
	public Vec3 shoulder(boolean left) {
		Vec3 facing = facing();
		Vec3 right = new Vec3(-facing.z, 0.0, facing.x);
		double bob = Math.sin((tickCount + (left ? 0 : 20)) * 0.08) * 0.3;
		return position().add(right.scale(left ? 2.3 : -2.3)).add(facing.scale(-0.4)).add(0.0, 2.8 + bob, 0.0);
	}

	/** Who is fighting her, as her skulls and her bolts need to know. */
	public @Nullable Player foe(ServerLevel level) {
		return foe == null ? null : level.getPlayerByUUID(foe);
	}

	/** Whether she is fighting (risen, not tolling or resetting). */
	public boolean fighting() {
		return phase == Phase.REAPING || phase == Phase.MOON || phase == Phase.HARVEST;
	}

	public List<UUID> thralls() {
		return List.copyOf(thralls);
	}

	/** Forgets servants that are gone. */
	private void prune(ServerLevel level) {
		thralls.removeIf(id -> !(level.getEntity(id) instanceof GraveThrallEntity thrall) || !thrall.isAlive());
		souls.removeIf(id -> !(level.getEntity(id) instanceof HarvestSoulEntity));
	}

	/** Whoever hurt her or her skulls has taken part. */
	public void took(ServerPlayer player, float amount) {
		damageBy.merge(player.getUUID(), amount, Float::sum);
		lastAttacker = player.getUUID();
	}

	/** Her participants: everyone who hurt her or her skulls and is still in her arena. */
	public List<ServerPlayer> participants(ServerLevel level) {
		List<ServerPlayer> out = new ArrayList<>();
		for (ServerPlayer player : level.players()) {
			if (player.isAlive() && damageBy.getOrDefault(player.getUUID(), 0.0F) > 0.0F && player.position().distanceTo(anchor) <= 64.0) {
				out.add(player);
			}
		}
		return out;
	}

	// ---------------------------------------------------------------- abandoned, and defeated

	/** Left alone in her arena: she goes back to her throne and heals, and everything she called up is gone. */
	public void reset(ServerLevel level) {
		dismiss(level);
		catchScythe();
		phase = Phase.SEATED;
		phaseTicks = 0;
		tolled = false;
		harvested = false;
		attack = null;
		abandoned = 0;
		foe = null;
		damageBy.clear();
		lastAttacker = null;
		java.util.Arrays.fill(cooldowns, 0);
		setHealth(getMaxHealth());
		for (ReaperSkullEntity skull : skulls(level)) {
			skull.discard();
		}
		dirge = null;
		requiem = null;
		spawnSkulls(level, partyScale(1));
		moon(level, false);
		for (BlockPos ward : wards(level)) {
			BlockState state = level.getBlockState(ward);
			if (state.is(JugcraftLairs.LAIR_BRAZIER)) {
				level.setBlock(ward, state.setValue(LairBrazierBlock.LIT, true), Block.UPDATE_ALL);
			}
		}
		teleportTo(seat.x, seat.y, seat.z);
		face(seatYaw);
		setAction(Action.SEATED);
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.vesperine.reset");
	}

	/** Her thralls crumble, her souls fade and her thrown scythe is gone. */
	private void dismiss(ServerLevel level) {
		for (UUID id : List.copyOf(thralls)) {
			if (level.getEntity(id) instanceof GraveThrallEntity thrall) {
				thrall.crumble(level);
			}
		}
		thralls.clear();
		for (UUID id : List.copyOf(souls)) {
			if (level.getEntity(id) instanceof HarvestSoulEntity soul) {
				soul.fade(level);
			}
		}
		souls.clear();
		if (scythe != null && level.getEntity(scythe) instanceof ThrownScytheEntity thrown) {
			thrown.discard();
		}
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level) {
			defeated(level);
		}
	}

	/**
	 * She has fallen: her servants and skulls go with her, each participant gets their loot, her lair's instance ends
	 * (it closes once everyone has left) and a gate of Grey Mist opens in the middle of the circle.
	 */
	private void defeated(ServerLevel level) {
		dismiss(level);
		for (ReaperSkullEntity skull : skulls(level)) {
			skull.crumble(level);
		}
		VesperineLoot.reward(level, this, participants(level));
		moon(level, false);
		LairInstance open = lairInstance(level);
		if (open != null) {
			Lairs.end(open);
			BlockState mist = JugcraftLairs.LAIR_EXIT.defaultBlockState().setValue(LairExitBlock.AXIS, Direction.Axis.X);
			for (BlockPos at : exitCells(anchor)) {
				level.setBlock(at, mist, Block.UPDATE_ALL);
			}
		}
		bossBar.setVisible(false);
		tell(level, "message.jugcraft.vesperine.defeated");
	}

	/** Where the Grey Mist opens when she falls: two blocks wide and two high in the middle of her arena. */
	public static List<BlockPos> exitCells(Vec3 arena) {
		BlockPos centre = BlockPos.containing(arena.x, arena.y, arena.z);
		return List.of(centre.west(), centre, centre.west().above(), centre.above());
	}

	/** Tells everyone near her arena. */
	private void tell(ServerLevel level, String key) {
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(anchor) <= LEASH + 32.0) {
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
		if (phase == Phase.SEATED) {
			if (source.getEntity() instanceof Player player && eligible(player)) {
				wake(level);
			}
			return false;
		}
		if (phase == Phase.RISING || phase == Phase.TOLL) {
			return false;
		}
		float scaled = taken(amount);
		boolean hurt = super.hurtServer(level, source, scaled);
		if (hurt && source.getEntity() instanceof ServerPlayer player) {
			took(player, scaled);
		}
		return hurt;
	}

	/** What a blow of {@code amount} does to her now: half while both skulls live, a quarter more while she is unarmed. */
	public float taken(float amount) {
		return amount * (guards() >= 2 ? GUARD : 1.0F) * (armed() ? 1.0F : UNARMED);
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		return !effect.is(MobEffects.WITHER) && super.canBeAffected(effect);
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
	public boolean canBreatheUnderwater() {
		return true;
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
		return phase == Phase.SEATED ? null : SoundEvents.WITHER_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.WITHER_SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITHER_DEATH;
	}

	// ---------------------------------------------------------------- particles

	private static void line(ServerLevel level, Vec3 from, Vec3 to, double share, net.minecraft.core.particles.ParticleOptions particle) {
		int points = Math.max(2, (int) (from.distanceTo(to) * 2.0));
		for (int i = 0; i <= points; i++) {
			Vec3 at = from.add(to.subtract(from).scale(i / (double) points));
			level.sendParticles(particle, at.x, at.y + 0.1, at.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	private static void ring(ServerLevel level, Vec3 centre, double radius, net.minecraft.core.particles.ParticleOptions particle, int points) {
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
		builder.define(ACTION, Action.SEATED.ordinal());
		builder.define(GUARDS, 2);
		builder.define(ARMED, true);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (instance != null) {
			output.store("instance", UUIDUtil.CODEC, instance);
			output.putInt("slot", slot);
		}
		output.store("anchor", Vec3.CODEC, anchor);
		output.store("seat", Vec3.CODEC, seat);
		output.putFloat("seat_yaw", seatYaw);
		output.putString("phase", phase.name());
		output.putBoolean("tolled", tolled);
		output.putBoolean("harvested", harvested);
		if (dirge != null) {
			output.store("dirge", UUIDUtil.CODEC, dirge);
		}
		if (requiem != null) {
			output.store("requiem", UUIDUtil.CODEC, requiem);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		instance = input.read("instance", UUIDUtil.CODEC).orElse(null);
		slot = input.getIntOr("slot", -1);
		anchor = input.read("anchor", Vec3.CODEC).orElse(position());
		seat = input.read("seat", Vec3.CODEC).orElse(position());
		seatYaw = input.getFloatOr("seat_yaw", 0.0F);
		tolled = input.getBooleanOr("tolled", false);
		harvested = input.getBooleanOr("harvested", false);
		try {
			phase = Phase.valueOf(input.getStringOr("phase", Phase.SEATED.name()));
		} catch (IllegalArgumentException e) {
			phase = Phase.SEATED;
		}
		if (phase == Phase.RISING || phase == Phase.TOLL || phase == Phase.HARVEST) {
			phase = tolled ? Phase.MOON : Phase.REAPING;  // a passing moment is not resumed
		}
		dirge = input.read("dirge", UUIDUtil.CODEC).orElse(null);
		requiem = input.read("requiem", UUIDUtil.CODEC).orElse(null);
		bossBar.setVisible(phase != Phase.SEATED);
	}

	// ---------------------------------------------------------------- her animations

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<VesperineEntity>("body", 4, test -> {
			Action action = test.animatable().action();
			return test.setAndContinue(action == Action.IDLE && test.isMoving() ? GLIDE : BODY[action.ordinal()]);
		}));
		controllers.add(new AnimationController<VesperineEntity>("halo", 8,
				test -> test.setAndContinue(HALO[Math.clamp(test.animatable().guards(), 0, 2)])));
		controllers.add(new AnimationController<VesperineEntity>("scythe", 0,
				test -> test.setAndContinue(test.animatable().armed() ? WIELDED : THROWN)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
