package io.github.jimbozoomer.jugcraft.pets;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Pumpkling;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity;
import io.github.jimbozoomer.jugcraft.creatures.chima.Chima;
import io.github.jimbozoomer.jugcraft.menagerie.MenagerieAnimal;
import io.github.jimbozoomer.jugcraft.nouveau.NouveauSummons;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import local.peepo.PeepoEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What a registered pet does while it is out with its owner: the pet roster's server loop calls {@link #tick} once a
 * second for each active pet, and {@link #detach} when the pet is stored or unloaded; clear removes bonuses on explicit release.
 *
 * <p>Modes:
 * <ul>
 * <li>{@link #FOLLOW}: keeps up with its owner, and otherwise behaves as its species always does.</li>
 * <li>{@link #STAY}: holds where it was told. Pets that can sit (tamed vanilla animals, menagerie animals, Chima,
 * Pumpkling) sit, with their own sitting animation. Others are held in place by a command goal.</li>
 * <li>{@link #DEFEND}: follows, and turns on whatever threatens its owner.</li>
 * <li>{@link #PASSIVE}: follows, and never fights.</li>
 * </ul>
 *
 * <p>It works through what the pet already has. It steers the pet's own navigation, uses its own sitting state,
 * sets or clears its target, and applies level bonuses as persistent attribute modifiers. Between the once-a-second
 * ticks, two command goals of its own keep STAY and PASSIVE from being overridden by the pet's native goals:
 * <ul>
 * <li>one holds a STAY pet that cannot sit;</li>
 * <li>one drops any target a PASSIVE pet picks up before it can strike.</li>
 * </ul>
 * They are installed once per pet at priority -2, the same as Peepo's livestock holds, and are removed by
 * {@link #clear}. They go inert by themselves within {@link #COMMAND_GRACE} ticks if the loop stops ticking the pet.
 * They take no JUMP flag, so a pet still floats and swims. Ticking a pet any number of times never stacks anything. It never sets a block, drops an item or loads a chunk. A pet never
 * turns on a player or on anyone's pet, and fights other creatures only where its owner could strike them by hand
 * ({@link Authority#mayStrike}). Catching up with a far owner takes the same care as the menagerie's pet flute. A pet
 * that lives in water (or lava) only ever arrives in that same fluid, never on land.
 *
 * <p>Peepo companions keep their own orders ({@code local.peepo.CompanionOrders}). Only the level bonuses apply to them.
 */
public final class PetBehaviors {
	public static final String FOLLOW = "FOLLOW", STAY = "STAY", DEFEND = "DEFEND", PASSIVE = "PASSIVE";
	public static final int MIN_LEVEL = 1, MAX_LEVEL = 20;
	/** Stable named modifiers persist with pet stats; repeated application never stacks. */
	public static final Identifier VITALITY = Jugcraft.id("pet_vitality"), STRENGTH = Jugcraft.id("pet_strength");
	/** Entity tag on a pet that STAY sat down, so it is stood up again even after a save, a recall or a summon. */
	public static final String SAT_FOR_STAY = "jugcraft.pet_stay";
	/** A pet further than this (blocks) for {@link #CATCH_UP_AFTER} seconds in a row catches up with its owner. */
	public static final double CATCH_UP = 24;
	public static final int CATCH_UP_AFTER = 3;
	/** Ticks between catch-up attempts, successful or not: the search near the owner is bounded and rare. */
	public static final int CATCH_UP_COOLDOWN = 200;
	/** How far (blocks) a non-sitting STAY pet may wander from where it was told to stay. */
	public static final double STAY_LEASH = 3;
	/** Ticks a pet must go unhurt before it starts to recover. */
	public static final int CALM_TICKS = 100;
	/** Ticks within which a blow (to the owner, by the owner, or to the pet) still calls a defender. */
	public static final int RECENT = 100;
	private static final int FORGET_AFTER = 6000;
	/** Ticks after the loop's last tick during which the command goals still act (one missed second is tolerated). */
	public static final int COMMAND_GRACE = 40;
	/** The command goals' priority: ahead of every native goal, as Peepo's holds are. */
	public static final int COMMAND_PRIORITY = -2;

	/** How a pet gets about, from its own navigation (species-aware without a list of species). */
	public enum Movement {
		GROUND, AIR, FLUID;

		public static Movement of(Mob pet) {
			return pet.getNavigation() instanceof WaterBoundPathNavigation ? FLUID
					: pet.getNavigation() instanceof FlyingPathNavigation ? AIR : GROUND;
		}
	}

	/** What the loop remembers about a pet between ticks; nothing here needs saving. */
	private static final class State {
		String mode = "";
		@Nullable BlockPos anchor;
		@Nullable LivingEntity guarding;
		int farSeconds;
		long lastCatchUp = Long.MIN_VALUE / 2;
		long lastSeen;
		long lastHurt = Long.MIN_VALUE / 2;
        long lastRecovery = Long.MIN_VALUE / 2;
		float lastHealth = -1;
		/** The entity the command goals were installed on (a summoned pet is a new entity with the same UUID). */
		@Nullable Mob commanded;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();
	private static long lastSweep;

	private PetBehaviors() {
	}
    public static void reset(){for(var state:List.copyOf(STATES.values()))if(state.commanded!=null)detach(state.commanded);STATES.clear();lastSweep=0;}

	// ---------------------------------------------------------------- level bonuses (1..20, modest)

	public static int level(int level) {
		return Math.clamp(level, MIN_LEVEL, MAX_LEVEL);
	}

	/** Extra maximum health, a fraction of the species' own: none at level 1, +38% at 20. */
	public static double vitality(int level) {
		return 0.02 * (level(level) - 1);
	}

	/** Extra attack damage, a fraction of the species' own: none at level 1, +47.5% at 20. */
	public static double strength(int level) {
		return 0.025 * (level(level) - 1);
	}

	/** Health recovered each second once calm: a heart in 40 s at level 1, in about 7 s at 20. */
	public static float recovery(int level) {
		return 0.05F + 0.0125F * (level(level) - 1);
	}

	/** Navigation speed when following: the species' own at level 1, +19% at 20. */
	public static double pace(int level) {
		return 1.0 + 0.01 * (level(level) - 1);
	}

	/** How far round its owner a defender looks for threats: 8 blocks, up to 12. */
	public static double guardRadius(int level) {
		return 8 + (level(level) - 1) / 4;
	}

	/** From this level a defender makes the monsters stalking its owner glow, so the owner sees them coming. */
	public static final int WARNING_LEVEL = 5;

	// ---------------------------------------------------------------- the loop's entry points

	/** One second of a registered pet's life with its owner. Unknown modes count as FOLLOW. */
	public static void tick(ServerPlayer owner, Mob pet, String mode, int level) {
		if (owner == null || pet == null || !pet.isAlive() || pet.isRemoved() || !(pet.level() instanceof ServerLevel world)) {
			return;
		}
		int lv = level(level);
		String wanted = mode(mode);
		State state = STATES.computeIfAbsent(pet.getUUID(), id -> new State());
		long now = world.getGameTime();
		state.lastSeen = now;
		bonuses(pet, lv);
		recover(pet, lv, state, now);
		if (pet.getTarget() == owner) {
			pet.setTarget(null);
		}
		if (pet instanceof PeepoEntity) {
			state.mode = wanted;
		} else {
			if (state.commanded != pet) {
				install(pet);
				state.commanded = pet;
			}
			if (!wanted.equals(state.mode)) {
				leave(pet, state);
				state.mode = wanted;
			}
			if (!wanted.equals(STAY)) {
				standUp(pet);   // also a pet saved or recalled while staying, then summoned in another mode
			}
			switch (wanted) {
				case STAY -> stay(pet, state);
				case DEFEND -> {
					guard(owner, pet, lv, state);
					follow(owner, pet, lv, state, now);
				}
				case PASSIVE -> {
					calm(pet);
					follow(owner, pet, lv, state, now);
				}
				default -> follow(owner, pet, lv, state, now);
			}
		}
		if (now - lastSweep >= 1200 || now < lastSweep) {
			lastSweep = now;
			STATES.values().removeIf(s -> now - s.lastSeen > FORGET_AFTER);
		}
	}

	/**
	 * Undoes everything {@link #tick} did to a pet: its level bonuses (clamping its health to its own maximum), the sit
	 * STAY put it in, and the target DEFEND gave it. Call it before the pet is stored, and whenever it leaves the loop.
	 */
	public static void clear(Mob pet) {
		if (pet == null) {
			return;
		}
		State state = STATES.get(pet.getUUID());
		modifier(pet.getAttribute(Attributes.MAX_HEALTH), VITALITY, 0);
		modifier(pet.getAttribute(Attributes.ATTACK_DAMAGE), STRENGTH, 0);
		if (pet.getHealth() > pet.getMaxHealth()) {
			pet.setHealth(pet.getMaxHealth());
		}
		standUp(pet);
		if (state != null && state.guarding != null && pet.getTarget() == state.guarding) {
			pet.setTarget(null);
		}
		detach(pet);
	}

    /** Drop runtime goals/references without changing the stats serialized with an unloaded pet. */
    public static void detach(Mob pet) {
        STATES.remove(pet.getUUID());
		GoalSelector goals = pet.getGoalSelector();
		for (Goal goal : goals.getAvailableGoals().stream().map(WrappedGoal::getGoal).filter(g -> g instanceof Command).toList()) {
			goals.removeGoal(goal);
		}
	}

	/** Whether the loop is holding any state for this pet (for tests and the roster's diagnostics). */
	public static boolean tracking(Mob pet) {
		return STATES.containsKey(pet.getUUID());
	}

	static String mode(@Nullable String mode) {
		String m = mode == null ? FOLLOW : mode.toUpperCase(Locale.ROOT);
		return switch (m) {
			case STAY, DEFEND, PASSIVE -> m;
			default -> FOLLOW;
		};
	}

	// ---------------------------------------------------------------- modes

	/** Leaving a mode: stand up from STAY's sit, and drop the target DEFEND gave. */
	private static void leave(Mob pet, State state) {
		if (state.mode.equals(STAY)) {
			standUp(pet);
			state.anchor = null;
		}
		if (state.guarding != null && pet.getTarget() == state.guarding) {
			pet.setTarget(null);
		}
		state.guarding = null;
		state.farSeconds = 0;
	}

	private static void stay(Mob pet, State state) {
		if (pet.isVehicle() || pet.isPassenger() || pet.isLeashed()) {
			return;   // a rider, a cart or a lead is in charge
		}
		if (state.anchor == null) {
			state.anchor = pet.blockPosition();
		}
		if (!sitting(pet) && sit(pet, true)) {
			pet.addTag(SAT_FOR_STAY);
		}
		// A pet that cannot sit is held at its anchor by the Hold goal, every tick.
	}

	/** PASSIVE: no fighting. A pet that can fight forgets who hit it, so it does not hit back. */
	private static void calm(Mob pet) {
		if (pet.getTarget() != null) {
			pet.setTarget(null);
		}
		pet.setAggressive(false);
		if (fights(pet)) {
			pet.setLastHurtByMob(null);
			if (pet instanceof NeutralMob neutral) {
				neutral.stopBeingAngry();
			}
		}
	}

	private static void follow(ServerPlayer owner, Mob pet, int level, State state, long now) {
		if (pet.level() != owner.level() || !owner.isAlive() || owner.isSpectator() || pet.isPassenger() || pet.isVehicle()
				|| pet.isLeashed() || sitting(pet)) {
			state.farSeconds = 0;   // a sitting pet the owner sat down by hand stays sitting
			return;
		}
		double distance = Math.sqrt(pet.distanceToSqr(owner));
		LivingEntity fighting = pet.getTarget();
		if (fighting != null && fighting.isAlive() && distance < CATCH_UP) {
			return;   // let it finish the fight unless the owner has left it far behind
		}
		Movement movement = Movement.of(pet);
		state.farSeconds = distance > CATCH_UP ? state.farSeconds + 1 : 0;
		if (state.farSeconds >= CATCH_UP_AFTER && now - state.lastCatchUp >= CATCH_UP_COOLDOWN) {
			state.lastCatchUp = now;
			if (catchUp(owner, pet, movement)) {
				state.farSeconds = 0;
				return;
			}
		}
		double start = Math.max(movement == Movement.AIR ? 8 : movement == Movement.FLUID ? 5 : 6, pet.getBbWidth() + 3);
		if (distance > start) {
			// A water-bound pet's navigation never leaves its fluid: it comes to the nearest shore and waits there.
			pet.getNavigation().moveTo(owner, pace(level) * (distance > 12 ? 1.2 : 1.0));
		}
	}

	private static void guard(ServerPlayer owner, Mob pet, int level, State state) {
		if (state.guarding != null && (!state.guarding.isAlive() || pet.getTarget() != state.guarding)) {
			state.guarding = null;
		}
		double radius = guardRadius(level);
		List<Mob> stalkers = pet.level() == owner.level() ? owner.level().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(radius),
				m -> m instanceof Enemy && m.isAlive() && m.getTarget() == owner) : List.of();
		if (level >= WARNING_LEVEL) {
			for (int i = 0; i < Math.min(8, stalkers.size()); i++) {
				if(mayFight(owner,pet,stalkers.get(i),radius))stalkers.get(i).addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false), pet);
			}
		}
		if (!fights(pet) || state.guarding != null || pet.isVehicle() || pet.isPassenger()) {
			return;
		}
		LivingEntity threat = threat(owner, pet, radius, stalkers);
		if (threat != null) {
			pet.setTarget(threat);
			state.guarding = threat;
		}
	}

	/** The nearest creature that threatens the owner and that this pet may fight, or null. */
	static @Nullable LivingEntity threat(ServerPlayer owner, Mob pet, double radius, List<Mob> stalkers) {
		List<LivingEntity> candidates = new ArrayList<>(stalkers);
		if (owner.getLastHurtByMob() != null && owner.tickCount - owner.getLastHurtByMobTimestamp() <= RECENT) {
			candidates.add(owner.getLastHurtByMob());
		}
		if (pet.getLastHurtByMob() != null && pet.tickCount - pet.getLastHurtByMobTimestamp() <= RECENT) {
			candidates.add(pet.getLastHurtByMob());
		}
		// What the owner has just struck (it remembers the blow itself).
		LivingEntity struck = owner.getLastHurtMob();
		if (struck != null && struck.getLastHurtByMob() == owner && struck.tickCount - struck.getLastHurtByMobTimestamp() <= RECENT) {
			candidates.add(struck);
		}
		candidates.sort(Comparator.comparingDouble(pet::distanceToSqr));
		for (LivingEntity candidate : candidates) {
			if (mayFight(owner, pet, candidate, radius)) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Whether a pet may fight {@code target} for its owner: never a player (no PvP through pets), never anyone's pet,
	 * mount or companion, never the owner or itself, only near both, and only where the owner could strike it by hand.
	 */
	public static boolean mayFight(ServerPlayer owner, Mob pet, @Nullable LivingEntity target, double radius) {
		if (target == null || !target.isAlive() || target == owner || target == pet || target instanceof Player
				|| target.level() != pet.level() || owned(target)) {
			return false;
		}
		if (target.distanceToSqr(owner) > radius * radius * 1.5 || target.distanceToSqr(pet) > 24 * 24) {
			return false;
		}
		return Authority.mayStrike(owner, target);
	}

	/**
	 * Whether a creature belongs to someone: a tamed animal or mount, a menagerie pet, Chima, Pumpkling, a companion, a
	 * spirit or familiar, or a spell's summon.
	 */
	public static boolean owned(LivingEntity entity) {
		return entity instanceof TamableAnimal tame && tame.isTame()
				|| entity instanceof AbstractHorse horse && horse.isTamed()
				|| entity instanceof MenagerieAnimal animal && animal.owner() != null
				|| entity instanceof Chima chima && chima.tame()
				|| entity instanceof Pumpkling pumpkling && pumpkling.owner() != null
				|| entity instanceof OwnableEntity ownable && ownable.getOwnerReference() != null
				|| entity instanceof PeepoEntity
				|| entity instanceof WorkerEntity<?> worker && worker.owner() != null
				|| NouveauSummons.owned(entity);
	}

	/** Whether a pet can fight at all (passive species have no attack, so DEFEND only warns and follows). */
	static boolean fights(Mob pet) {
		AttributeInstance damage = pet.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage == null || damage.getValue() <= 0) {
			return false;
		}
		return !(pet instanceof MenagerieAnimal animal && animal.species().mood().equals("passive"));
	}

	// ---------------------------------------------------------------- sitting

	/** Whether the pet is sitting by its own rules (a menagerie animal, Chima or Pumpkling's sit, or ordered to sit). */
	public static boolean sitting(Mob pet) {
		if (pet instanceof MenagerieAnimal animal) {
			return animal.sitting();
		}
		if (pet instanceof Chima chima) {
			return chima.sitting();
		}
		if (pet instanceof Pumpkling pumpkling) {
			return pumpkling.sitting();
		}
		return pet instanceof TamableAnimal tame && tame.isOrderedToSit();
	}

	/** Sits or stands a pet by its own rules; false when its kind cannot sit. */
	public static boolean sit(Mob pet, boolean value) {
		if (pet instanceof MenagerieAnimal animal) {
			animal.sit(value);
			return true;
		}
		if (pet instanceof Chima chima) {
			chima.setSitting(value);
			return true;
		}
		if (pet instanceof Pumpkling pumpkling) {
			pumpkling.setSitting(value);
			return true;
		}
		if (pet instanceof TamableAnimal tame && tame.isTame()) {
			tame.setOrderedToSit(value);
			tame.setInSittingPose(value);
			return true;
		}
		return false;
	}

	/** Stands up a pet only if STAY sat it down (never one its owner sat down by hand). */
	private static void standUp(Mob pet) {
		if (pet.entityTags().contains(SAT_FOR_STAY)) {
			pet.removeTag(SAT_FOR_STAY);
			sit(pet, false);
		}
	}

	// ---------------------------------------------------------------- abilities

	/** Level bonuses use stable modifier IDs and persist across recall and chunk unloading. */
	public static void bonuses(Mob pet, int level) {
		modifier(pet.getAttribute(Attributes.MAX_HEALTH), VITALITY, vitality(level));
		modifier(pet.getAttribute(Attributes.ATTACK_DAMAGE), STRENGTH, strength(level));
		if (pet.getHealth() > pet.getMaxHealth()) {
			pet.setHealth(pet.getMaxHealth());
		}
	}

	private static void modifier(@Nullable AttributeInstance attribute, Identifier id, double amount) {
		if (attribute == null) {
			return;
		}
		AttributeModifier current = attribute.getModifier(id);
		if (current != null && current.amount() == amount) {
			return;
		}
		if (current != null) {
			attribute.removeModifier(id);
		}
		if (amount > 0) {
			attribute.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	/**
	 * A calm pet recovers a little each second. Any loss of health (a blow, fire, drying out) restarts the calm, so
	 * recovery never outpaces harm. It never heals past the maximum, and it gives nothing a recall or summon could farm.
	 */
	private static void recover(Mob pet, int level, State state, long now) {
		float health = pet.getHealth();
        if(state.lastHealth < 0)state.lastHurt=now;
		if (state.lastHealth >= 0 && health < state.lastHealth - 0.001F) {
			state.lastHurt = now;
		}
		if (now - state.lastRecovery >= 20 && now - state.lastHurt >= CALM_TICKS && health < pet.getMaxHealth() && pet.getTarget() == null && !pet.isOnFire()) {
			pet.heal(recovery(level)); state.lastRecovery=now;
		}
		state.lastHealth = pet.getHealth();
	}

	// ---------------------------------------------------------------- command goals

	/** Adds the command goals to a pet once (never twice, whatever the loop does). */
	private static void install(Mob pet) {
		GoalSelector goals = pet.getGoalSelector();
		if (goals.getAvailableGoals().stream().noneMatch(w -> w.getGoal() instanceof Command command && command.pet == pet)) {
			goals.addGoal(COMMAND_PRIORITY, new Hold(pet));
			goals.addGoal(COMMAND_PRIORITY, new Calm(pet));
		}
	}

	/** The loop's current state for a pet, while the loop is still ticking it. */
	private static @Nullable State live(Mob pet) {
		State state = STATES.get(pet.getUUID());
		return state != null && state.commanded == pet && pet.level().getGameTime() - state.lastSeen <= COMMAND_GRACE ? state : null;
	}

	/** A goal that carries out the owner's command between the loop's ticks. */
	private abstract static class Command extends Goal {
		final Mob pet;

		Command(Mob pet) {
			this.pet = pet;
		}
	}

	/**
	 * STAY, for a pet that cannot sit: holds movement (not jumping, so it still floats), and walks it back to where
	 * it was told to stay if it is pushed or drifts more than a block and a half away.
	 */
	static final class Hold extends Command {
		Hold(Mob pet) {
			super(pet);
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		private boolean holding() {
			State state = live(pet);
			return state != null && state.mode.equals(STAY) && state.anchor != null && !sitting(pet) && !pet.isVehicle()
					&& !pet.isPassenger() && !pet.isLeashed();
		}

		@Override
		public boolean canUse() {
			return holding();
		}

		@Override
		public boolean canContinueToUse() {
			return holding();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void start() {
			pet.getNavigation().stop();
		}

		@Override
		public void tick() {
			State state = live(pet);
			if (state == null || state.anchor == null) {
				return;
			}
			BlockPos at = state.anchor;
			if (pet.distanceToSqr(at.getX() + 0.5, at.getY(), at.getZ() + 0.5) > 2.25) {
				if (pet.getNavigation().isDone()) {
					pet.getNavigation().moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.8);
				}
			} else {
				pet.getNavigation().stop();
			}
		}

		@Override
		public void stop() {
			pet.getNavigation().stop();
		}
	}

	/**
	 * PASSIVE: the moment any native goal gives the pet a target, this takes the pet's movement ahead of its attack
	 * and drops the target, so it never strikes. It also stops a STAY pet from leaving its place to fight.
	 */
	static final class Calm extends Command {
		Calm(Mob pet) {
			super(pet);
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			State state = live(pet);
			// A pet sitting by its own rules is held by its own sit goal; interrupting that would flicker its pose.
			return state != null && pet.getTarget() != null && (state.mode.equals(PASSIVE) || state.mode.equals(STAY) && !sitting(pet));
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}

		@Override
		public void start() {
			calm(pet);
			pet.getNavigation().stop();
		}
	}

	// ---------------------------------------------------------------- catching up

	/**
	 * Moves a far pet to a safe place two to four blocks from its owner, as the menagerie's pet flute does:
	 * <ul>
	 * <li>only into loaded chunks, within the world border;</li>
	 * <li>only where the owner may change every cell the pet would occupy (claims), with no fire;</li>
	 * <li>a land pet onto a solid floor, out of any liquid;</li>
	 * <li>a flying pet into clear air;</li>
	 * <li>a water-bound pet only into the fluid it is in now, so it is never left on land (if it is out of its fluid,
	 * it is not moved at all).</li>
	 * </ul>
	 * Never a very large pet, a ridden or riding one, or one on a lead.
	 */
	static boolean catchUp(ServerPlayer owner, Mob pet, Movement movement) {
		ServerLevel level = owner.level();
		if (pet.level() != level || pet.getBbWidth() > 4 || pet.getBbHeight() > 4 || pet.isPassenger() || pet.isVehicle() || pet.isLeashed()) {
			return false;
		}
		FluidState home = pet.level().getFluidState(pet.blockPosition());
		boolean fluid = movement == Movement.FLUID || pet.canBreatheUnderwater() && pet.isInWater();
		if (fluid && home.isEmpty()) {
			return false;
		}
		for (int radius = 2; radius <= 4; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
						continue;
					}
					for (int dy = fluid ? -2 : 0; dy <= 2; dy++) {
						BlockPos at = owner.blockPosition().offset(dx, dy, dz);
						AABB box = pet.getBoundingBox().move(at.getX() + 0.5 - pet.getX(), at.getY() - pet.getY(), at.getZ() + 0.5 - pet.getZ());
						if (!level.hasChunksAt(BlockPos.containing(box.minX, box.minY - 1, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))
								|| !level.getWorldBorder().isWithinBounds(box) || !level.noCollision(pet, box)) {
							continue;
						}
						if (fluid ? !filled(level, box, home) : level.containsAnyLiquid(box)
								|| movement == Movement.GROUND && !level.getBlockState(at.below()).isCollisionShapeFullBlock(level, at.below())) {
							continue;
						}
						if (!permitted(level, owner, box)) {
							continue;
						}
						if (!pet.teleportTo(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, Set.of(), pet.getYRot(), pet.getXRot(), true)) {
							continue;
						}
						pet.setDeltaMovement(Vec3.ZERO);
						pet.fallDistance = 0;
						pet.getNavigation().stop();
						return true;
					}
				}
			}
		}
		return false;
	}

	/** Every cell of the box holds the same fluid as {@code home}. */
	static boolean filled(ServerLevel level, AABB box, FluidState home) {
		for (BlockPos cell : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
			if (!level.getFluidState(cell).getType().isSame(home.getType())) {
				return false;
			}
		}
		return true;
	}

	/** The owner may change every cell of the box, and none of it is on fire. */
	static boolean permitted(ServerLevel level, ServerPlayer owner, AABB box) {
		for (BlockPos cell : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
			if (!Authority.mayChange(level, owner, cell) || (level.getBlockState(cell).is(net.minecraft.tags.BlockTags.FIRE) || level.getBlockState(cell).is(Blocks.CACTUS) || level.getBlockState(cell).is(Blocks.SWEET_BERRY_BUSH) || level.getBlockState(cell).is(Blocks.POWDER_SNOW))) {
				return false;
			}
		}
		return true;
	}
}
