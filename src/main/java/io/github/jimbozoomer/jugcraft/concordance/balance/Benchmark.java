package io.github.jimbozoomer.jugcraft.concordance.balance;

import io.github.jimbozoomer.jugcraft.concordance.compose.Component;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Roadmap step 11: a deterministic benchmark of characters against controlled encounters, run over the same compiled
 * plans the server casts. It is a model, not the game: positions are one line (the character, the foes and an
 * objective on it), time is server ticks, and every random element of combat (critical hits, aim) is left out, so a
 * run always gives the same result. What it takes from the game is measured there: the per-cast numbers come from the
 * compiled plans and Spell Engine's cast and cooldown times, and the rules it applies (armour, absorption, a dash's
 * reach, the Spell Power bonus) are checked against the server by the calibration game tests. Pure: no Minecraft
 * types.
 * <p>
 * What the model does on each tick, in order: the character finishes or starts an action by a fixed priority (shield,
 * support, escape, ranged damage, melee, then close in; damage spells keep back the Focus a shield or support spell
 * due within 5 seconds will need), foes move and strike by their behaviour, Focus returns, and lasting effects and
 * hurt immunity tick down.
 */
public final class Benchmark {
	public static final int TICKS = 20;
	public static final double PLAYER_HEALTH = 20.0;
	/** A player's walking speed, blocks a tick (vanilla: 4.317 blocks a second). */
	public static final double WALK = 0.215;
	/**
	 * Blocks a push carries a creature for each block a tick of push speed, on flat ground. Measured: Flashstep's push
	 * of 1.2 blocks a tick carried a pig 7.05 blocks in CI; the calibration test keeps the model between that and 30%
	 * below it (ConcordanceBaselineGameTests.aPushCarriesAsFarAsTheModelSays).
	 */
	public static final double PUSH_REACH = 5.5;
	/** How much further than the model a real push may carry before the model counts as wrong. */
	public static final double PUSH_TOLERANCE = 1.3;
	/** How far a melee hit knocks a foe back, blocks (vanilla's knockback of 0.4, on the ground). */
	public static final double KNOCKBACK = 0.8;
	/**
	 * Vanilla's hurt immunity: for this many ticks after a hit, another hit counts only for what it exceeds the last
	 * (raw, before armour), and knocks nothing back. So three blows landing together hurt as one.
	 */
	public static final int HURT_IMMUNITY = 10;

	private Benchmark() {
	}

	/** Armour points and toughness (vanilla sets: leather 7, chain 12, iron 15). */
	public record Armour(String name, int points, int toughness) {
		public static final Armour NONE = new Armour("none", 0, 0);
		public static final Armour LEATHER = new Armour("leather", 7, 0);
		public static final Armour CHAIN = new Armour("chain", 12, 0);
		public static final Armour IRON = new Armour("iron", 15, 0);
	}

	/**
	 * A melee weapon: damage at full strength, ticks between full-strength swings, reach in blocks, and whether it
	 * sweeps (a sword's sweep deals 1 to other foes within reach).
	 */
	public record Weapon(String name, double damage, int interval, double reach, boolean sweeps) {
		public static final Weapon FIST = new Weapon("fist", 1.0, 5, 3.0, false);
		public static final Weapon WOODEN_SWORD = new Weapon("wooden sword", 4.0, 13, 3.0, true);
		public static final Weapon IRON_SWORD = new Weapon("iron sword", 6.0, 13, 3.0, true);
	}

	/**
	 * Damage left after armour, vanilla's formula: the armour counted is the points less a share of the damage
	 * (toughness lowers that share), kept between a fifth of the points and 20, and each counted point takes 4%.
	 */
	public static double afterArmour(double damage, int points, int toughness) {
		double counted = Math.clamp(points - damage / (2.0 + toughness / 4.0), points * 0.2, 20.0);
		return damage * (1.0 - counted / 25.0);
	}

	/**
	 * Health and absorption left after raw hits landing on the same tick, by the model's rules (hurt immunity, then
	 * armour, then absorption first): what the calibration test compares with the server.
	 */
	public static double[] afterHits(double health, double absorption, int armour, int toughness, double... raw) {
		Guard guard = new Guard();
		for (double hit : raw) {
			double after = afterArmour(guard.admit(hit), armour, toughness);
			double soaked = Math.min(absorption, after);
			absorption -= soaked;
			health -= after - soaked;
		}
		return new double[] {health, absorption};
	}

	/**
	 * An invocation or composed spell as the model casts it: its plan (already tuned and scaled by the caster's Spell
	 * Power), the Focus it costs, Spell Engine's cast time, and its cooldown (Spell Engine's, never shorter than the
	 * plan's: the floor {@code ConcordanceSpells} enforces).
	 */
	public record Ability(String id, Definitions.Role role, Plan plan, int focus, int castTicks, int cooldownTicks) {
		public static Ability of(String id, Definitions.Role role, Plan plan, int focus, int castTicks, int spellCooldownTicks) {
			return new Ability(id, role, plan, focus, castTicks, Math.max(spellCooldownTicks, plan.cooldown()));
		}
	}

	/** A benchmark character: what they wear and carry, and what they can cast. */
	public record Character(String name, Armour armour, Weapon weapon, List<Ability> abilities) {
		public Character {
			abilities = List.copyOf(abilities);
		}
	}

	/** How a foe behaves. */
	public enum Behaviour {
		/** Walks to its target and strikes it on its interval (a zombie). */
		BRUTE,
		/** Closes fast, strikes once, retreats to 8 blocks, waits two seconds and comes again. */
		RUNNER,
		/** Holds its place in front of what it protects until the character comes within 4 blocks, then fights. */
		GUARD,
		/** Keeps its distance and shoots the character within 16 blocks on its interval. */
		ARCHER,
		/** Stands still, is not knocked back and never strikes (for measurements). */
		DUMMY
	}

	/**
	 * One foe: its health, armour points, speed (blocks a tick), the damage and interval of its attacks, its reach, and
	 * where it starts (blocks from the character).
	 */
	public record Foe(String name, Behaviour behaviour, double health, int armour, double speed, double damage, int interval, double reach,
			double start) {
		public Foe at(double distance) {
			return new Foe(name, behaviour, health, armour, speed, damage, interval, reach, distance);
		}
	}

	/** What wins an encounter. */
	public enum Goal {
		/** Every foe defeated in time. */
		DEFEAT_ALL,
		/** The last foe listed (the protected one) defeated in time. */
		DEFEAT_PROTECTED,
		/** The objective still standing when time runs out, or every foe defeated. */
		DEFEND
	}

	/**
	 * A controlled scenario: its foes, an objective's health if it has one (it stands where the character starts, and
	 * foes go for it instead of the character), how long it lasts and what wins it.
	 */
	public record Encounter(String id, String name, List<Foe> foes, double objective, int ticks, Goal goal) {
		public Encounter {
			foes = List.copyOf(foes);
		}
	}

	/**
	 * What happened: won or not, when it ended, damage dealt (by spells and by weapons), damage taken (before
	 * absorption), absorption and healing gained, Focus spent, casts by ability, the character's health at the end,
	 * blocks moved by abilities, foe-ticks of control (slowed or pushed), and the health and absorption a spell that
	 * chooses allies gave each of them (the character included).
	 */
	public record Outcome(boolean won, int ticks, double spellDamage, double weaponDamage, double taken, double absorbed, double healed,
			int focusSpent, Map<String, Integer> casts, double health, double moved, double control, double support) {
		public double damage() {
			return spellDamage + weaponDamage;
		}
	}

	// ----------------------------------------------------------------------------------------------- the simulation

	/** Hurt immunity, for a foe or the character (vanilla's invulnerableTime and lastHurt). */
	private static final class Guard {
		int ticks;
		double last;

		/** The raw damage a hit of {@code amount} actually deals now, recording it. */
		double admit(double amount) {
			if (ticks > 0) {
				if (amount <= last) {
					return 0.0;
				}
				double extra = amount - last;
				last = amount;
				return extra;
			}
			ticks = HURT_IMMUNITY;
			last = amount;
			return amount;
		}

		boolean fresh() {
			return ticks == HURT_IMMUNITY;
		}

		void tick() {
			if (ticks > 0) {
				ticks--;
			}
		}
	}

	private static final class Live {
		final Foe spec;
		final Guard guard = new Guard();
		double health;
		double position;
		int cooldown;
		/** Runner state: 0 approaching, 1 retreating, then waiting while {@code wait} > 0. */
		int phase;
		int wait;
		boolean engaged;

		Live(Foe spec) {
			this.spec = spec;
			this.health = spec.health();
			this.position = spec.start();
		}

		boolean alive() {
			return health > 0.0;
		}
	}

	private static final class State {
		final Character character;
		final Encounter encounter;
		final List<Live> foes = new ArrayList<>();
		double position;
		final Guard guard = new Guard();
		double health = PLAYER_HEALTH;
		double absorption;
		int absorptionTicks;
		/** The helpful statuses on the character, by status: amplifier and ticks left. */
		final Map<String, int[]> statuses = new LinkedHashMap<>();
		double focus = FocusPool.MAX;
		int focusClock;
		int swing;
		@Nullable Ability casting;
		int castLeft;
		final Map<String, Integer> ready = new LinkedHashMap<>();
		double objective;
		double spellDamage;
		double weaponDamage;
		double taken;
		double absorbed;
		double healed;
		int focusSpent;
		double moved;
		double control;
		double support;
		final Map<String, Integer> casts = new LinkedHashMap<>();

		State(Character character, Encounter encounter) {
			this.character = character;
			this.encounter = encounter;
			this.objective = encounter.objective();
			for (Foe foe : encounter.foes()) {
				foes.add(new Live(foe));
			}
		}

		@Nullable Live nearest() {
			return foes.stream().filter(Live::alive).min(Comparator.comparingDouble(foe -> Math.abs(foe.position - position))).orElse(null);
		}

		double distance(Live foe) {
			return Math.abs(foe.position - position);
		}

		boolean threatened() {
			for (Live foe : foes) {
				if (!foe.alive()) {
					continue;
				}
				boolean shoots = foe.spec.behaviour() == Behaviour.ARCHER && distance(foe) <= 16.0;
				if (shoots || foe.spec.damage() > 0 && distance(foe) <= foe.spec.reach() + 1.5 && targetsCharacter(foe)) {
					return true;
				}
			}
			return false;
		}

		boolean targetsCharacter(Live foe) {
			return encounter.objective() <= 0.0 || foe.spec.behaviour() != Behaviour.BRUTE;
		}
	}

	/** Runs one character through one encounter. */
	public static Outcome run(Character character, Encounter encounter) {
		State state = new State(character, encounter);
		int tick = 0;
		for (; tick < encounter.ticks(); tick++) {
			act(state);
			foes(state);
			tickDown(state);
			if (state.health <= 0.0 || encounter.goal() == Goal.DEFEND && encounter.objective() > 0.0 && state.objective <= 0.0) {
				return outcome(state, false, tick + 1);
			}
			boolean done = switch (encounter.goal()) {
				case DEFEAT_ALL, DEFEND -> state.foes.stream().noneMatch(Live::alive);
				case DEFEAT_PROTECTED -> !state.foes.getLast().alive();
			};
			if (done) {
				return outcome(state, true, tick + 1);
			}
		}
		return outcome(state, encounter.goal() == Goal.DEFEND, tick);
	}

	private static Outcome outcome(State state, boolean won, int ticks) {
		return new Outcome(won, ticks, state.spellDamage, state.weaponDamage, state.taken, state.absorbed, state.healed, state.focusSpent,
				state.casts, Math.max(0.0, state.health), state.moved, state.control, state.support);
	}

	private static boolean ready(State state, Ability ability) {
		return state.ready.getOrDefault(ability.id(), 0) <= 0 && state.focus >= ability.focus();
	}

	private static void act(State state) {
		if (state.casting != null) {
			if (--state.castLeft <= 0) {
				release(state, state.casting);
				state.casting = null;
			}
			return;
		}
		Ability chosen = choose(state);
		if (chosen != null) {
			// Focus is taken and the cooldown starts when the spell takes effect; a cast that would find nothing is not
			// started (the server refuses it at no cost).
			state.casting = chosen;
			state.castLeft = chosen.castTicks();
			if (state.castLeft <= 0) {
				release(state, chosen);
				state.casting = null;
			}
			return;
		}
		Live nearest = state.nearest();
		if (nearest == null) {
			return;
		}
		Weapon weapon = state.character.weapon();
		if (state.distance(nearest) <= weapon.reach()) {
			if (state.swing <= 0) {
				strike(state, nearest, weapon);
				state.swing = weapon.interval();
			}
			return;
		}
		boolean defending = state.encounter.objective() > 0.0;
		if (!defending && !holdsForRanged(state, nearest)) {
			double step = Math.min(WALK, state.distance(nearest) - weapon.reach() + 0.01);
			state.position += Math.signum(nearest.position - state.position) * step;
		}
	}

	/**
	 * Whether a caster with ranged damage waits where it is rather than closing in: the spell reaches the target, is
	 * ready within 2 seconds, and its Focus (with what returns meanwhile) covers it and the reserve.
	 */
	private static boolean holdsForRanged(State state, Live target) {
		for (Ability ability : state.character.abilities()) {
			int wait = state.ready.getOrDefault(ability.id(), 0);
			if (ability.role() == Definitions.Role.DAMAGE && reaches(ability, state.distance(target)) && wait <= 2 * TICKS
					&& state.focus + wait / (double) FocusPool.REGEN_TICKS >= ability.focus() + reserve(state)) {
				return true;
			}
		}
		return false;
	}

	private static boolean reaches(Ability ability, double distance) {
		Plan.Node root = ability.plan().root();
		return root.form() == Component.Form.HERE ? distance <= root.radius() + 0.5 : distance <= root.range();
	}

	private static @Nullable Ability choose(State state) {
		Live nearest = state.nearest();
		double distance = nearest == null ? Double.MAX_VALUE : state.distance(nearest);
		boolean threatened = state.threatened();
		for (Definitions.Role role : List.of(Definitions.Role.DEFENSE, Definitions.Role.SUPPORT, Definitions.Role.MOVEMENT,
				Definitions.Role.DAMAGE)) {
			for (Ability ability : state.character.abilities()) {
				if (ability.role() != role || !ready(state, ability)) {
					continue;
				}
				boolean use = switch (role) {
					case DEFENSE -> threatened && state.absorption <= 0.0;
					case SUPPORT -> threatened && state.absorption <= 0.0 || state.health <= PLAYER_HEALTH - 4.0 && nearest != null;
					case MOVEMENT -> nearest != null && distance <= 2.0 && hasRanged(state) && state.encounter.objective() <= 0.0;
					case DAMAGE -> nearest != null && reaches(ability, distance) && state.focus - ability.focus() >= reserve(state);
					default -> false;
				};
				if (use) {
					return ability;
				}
			}
		}
		return null;
	}

	/**
	 * Focus kept back for a shield or support spell due within 5 seconds, so damage does not spend what protection will
	 * need.
	 */
	private static double reserve(State state) {
		double reserve = 0.0;
		for (Ability ability : state.character.abilities()) {
			if ((ability.role() == Definitions.Role.DEFENSE || ability.role() == Definitions.Role.SUPPORT)
					&& state.ready.getOrDefault(ability.id(), 0) <= 5 * TICKS) {
				reserve = Math.max(reserve, ability.focus());
			}
		}
		return reserve;
	}

	private static boolean hasRanged(State state) {
		return state.character.abilities().stream().anyMatch(a -> a.role() == Definitions.Role.DAMAGE && a.plan().root().range() > 4);
	}

	/** A spell takes effect: Focus, cooldown, and each node of its plan on what it selects. */
	private static void release(State state, Ability ability) {
		double given = state.absorbed + state.healed;
		boolean applied = node(state, ability.plan().root(), state.position);
		if (!applied) {
			return;
		}
		if (ability.plan().root().pick() == Component.Pick.ALLIES) {
			state.support += state.absorbed + state.healed - given;
		}
		state.focus -= ability.focus();
		state.focusSpent += ability.focus();
		state.ready.put(ability.id(), ability.cooldownTicks());
		state.casts.merge(ability.id(), 1, Integer::sum);
	}

	private static boolean node(State state, Plan.Node node, double from) {
		// Where it lands: on the caster (here), or on the first foe within range along the line.
		Live struck = null;
		double landing = from;
		if (node.form() != Component.Form.HERE) {
			Live nearest = state.nearest();
			if (nearest == null || Math.abs(nearest.position - from) > node.range()) {
				return false;
			}
			struck = nearest;
			landing = nearest.position;
		}
		boolean applied = false;
		for (Plan.Step step : node.steps()) {
			EffectSpec effect = step.effect();
			if (effect.intent() == Intent.HELPFUL) {
				// Helpful effects in these scenarios reach the character (here struck, or allies: the character alone).
				boolean self = node.pick() == Component.Pick.ALLIES || node.form() == Component.Form.HERE && node.pick() == Component.Pick.STRUCK;
				if (self) {
					applied |= helpful(state, effect);
				}
				continue;
			}
			List<Live> targets = new ArrayList<>();
			if (node.pick() == Component.Pick.STRUCK) {
				if (struck != null) {
					targets.add(struck);
				}
			} else if (node.pick() == Component.Pick.CREATURES) {
				double center = landing;
				state.foes.stream().filter(Live::alive).filter(foe -> Math.abs(foe.position - center) <= node.radius())
						.sorted(Comparator.comparingDouble(foe -> Math.abs(foe.position - center))).limit(node.targets()).forEach(targets::add);
			}
			for (Live foe : targets) {
				applied |= harmful(state, foe, effect);
			}
		}
		if (applied && node.then() != null) {
			node(state, node.then(), landing);
		}
		return applied;
	}

	private static boolean helpful(State state, EffectSpec effect) {
		switch (effect.kind()) {
			case PROTECTION -> {
				double amount = 4.0 * (effect.protectionAmplifier() + 1);
				if (amount < state.absorption || amount == state.absorption && effect.duration() <= state.absorptionTicks) {
					return false;
				}
				state.absorbed += amount;
				state.absorption = amount;
				state.absorptionTicks = effect.duration();
				return true;
			}
			case RESTORATION -> {
				double before = state.health;
				state.health = Math.min(PLAYER_HEALTH, state.health + effect.magnitude());
				state.healed += state.health - before;
				return state.health > before;
			}
			case STATUS -> {
				// It lands by its stacking rule, as on the server (a recast that changes nothing fails, at no cost). The
				// one helpful status, Hearthguard's Fire Resistance, wards against nothing these foes deal, so it changes
				// nothing else here.
				String status = effect.status();
				if (status == null) {
					return false;
				}
				int[] has = state.statuses.getOrDefault(status, new int[] {0, 0});
				Stacking.Result combined = effect.stacking().combine(has[0], has[1], effect.magnitude(), effect.duration());
				if (!combined.changed()) {
					return false;
				}
				state.statuses.put(status, new int[] {combined.amplifier(), combined.ticks()});
				return true;
			}
			case MOVEMENT -> {
				// Carried forward the way the caster faces: here, away from the foes.
				double reach = effect.magnitude() / 10.0 * PUSH_REACH;
				Live nearest = state.nearest();
				state.position -= nearest == null ? reach : Math.signum(nearest.position - state.position) * reach;
				state.moved += reach;
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private static boolean harmful(State state, Live foe, EffectSpec effect) {
		switch (effect.kind()) {
			case DAMAGE -> {
				// Spell Power's damage types bypass armour; nothing here resists magic. Within the foe's hurt immunity
				// the damage may count for nothing, and then the cast fails as on the server (no Focus, no cooldown).
				double dealt = foe.guard.admit(effect.magnitude());
				if (dealt <= 0.0) {
					return false;
				}
				state.spellDamage += Math.min(foe.health, dealt);
				foe.health -= dealt;
				return true;
			}
			case MOVEMENT -> {
				double pushed = effect.magnitude() / 10.0 * PUSH_REACH;
				foe.position += Math.signum(foe.position - state.position) * pushed;
				state.control += pushed / Math.max(0.01, foe.spec.speed());
				return true;
			}
			case STATUS -> {
				// Slowness: the foe loses its next steps for the effect's time, scaled by its level (15% a level).
				int lost = (int) Math.round(effect.duration() * 0.15 * (effect.magnitude() + 1));
				state.control += Math.max(0, lost - foe.wait);
				foe.wait = Math.max(foe.wait, lost);
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private static void strike(State state, Live target, Weapon weapon) {
		double dealt = afterArmour(target.guard.admit(weapon.damage()), target.spec.armour(), 0);
		state.weaponDamage += Math.min(target.health, dealt);
		target.health -= dealt;
		if (target.guard.fresh() && target.spec.behaviour() != Behaviour.DUMMY) {
			target.position += Math.signum(target.position - state.position) * KNOCKBACK;
		}
		if (weapon.sweeps()) {
			for (Live other : state.foes) {
				if (other != target && other.alive() && state.distance(other) <= weapon.reach()) {
					double sweep = afterArmour(other.guard.admit(1.0), other.spec.armour(), 0);
					state.weaponDamage += Math.min(other.health, sweep);
					other.health -= sweep;
				}
			}
		}
	}

	private static void foes(State state) {
		for (Live foe : state.foes) {
			if (!foe.alive()) {
				continue;
			}
			foe.guard.tick();
			if (foe.cooldown > 0) {
				foe.cooldown--;
			}
			if (foe.wait > 0 && foe.spec.behaviour() != Behaviour.RUNNER) {
				foe.wait--;
				continue;
			}
			switch (foe.spec.behaviour()) {
				case DUMMY -> {
				}
				case BRUTE -> {
					double target = state.targetsCharacter(foe) ? state.position : 0.0;
					if (Math.abs(foe.position - target) > foe.spec.reach()) {
						foe.position -= Math.signum(foe.position - target) * Math.min(foe.spec.speed(), Math.abs(foe.position - target) - foe.spec.reach());
					} else if (foe.cooldown <= 0) {
						foe.cooldown = foe.spec.interval();
						if (state.targetsCharacter(foe)) {
							hurt(state, foe.spec.damage());
						} else {
							state.objective -= foe.spec.damage();
						}
					}
				}
				case GUARD -> {
					if (!foe.engaged && state.distance(foe) > 4.0) {
						continue;
					}
					foe.engaged = true;
					if (state.distance(foe) > foe.spec.reach()) {
						foe.position -= Math.signum(foe.position - state.position) * Math.min(foe.spec.speed(), state.distance(foe) - foe.spec.reach());
					} else if (foe.cooldown <= 0) {
						foe.cooldown = foe.spec.interval();
						hurt(state, foe.spec.damage());
					}
				}
				case ARCHER -> {
					if (state.distance(foe) <= 16.0 && foe.cooldown <= 0) {
						foe.cooldown = foe.spec.interval();
						hurt(state, foe.spec.damage());
					}
				}
				case RUNNER -> runner(state, foe);
			}
		}
	}

	private static void runner(State state, Live foe) {
		double distance = state.distance(foe);
		double away = Math.signum(foe.position - state.position);
		if (foe.wait > 0) {
			foe.wait--;
			return;
		}
		if (foe.phase == 0) {
			if (distance > foe.spec.reach()) {
				foe.position -= away * Math.min(foe.spec.speed(), distance - foe.spec.reach());
			} else if (foe.cooldown <= 0) {
				foe.cooldown = foe.spec.interval();
				hurt(state, foe.spec.damage());
				foe.phase = 1;
			}
		} else if (distance < 8.0) {
			foe.position += (away == 0 ? 1 : away) * foe.spec.speed();
		} else {
			foe.phase = 0;
			foe.wait = 2 * TICKS;
		}
	}

	private static void hurt(State state, double damage) {
		double after = afterArmour(state.guard.admit(damage), state.character.armour().points(), state.character.armour().toughness());
		state.taken += after;
		double soaked = Math.min(state.absorption, after);
		state.absorption -= soaked;
		state.health -= after - soaked;
	}

	private static void tickDown(State state) {
		state.guard.tick();
		state.ready.replaceAll((id, left) -> Math.max(0, left - 1));
		if (state.swing > 0) {
			state.swing--;
		}
		if (state.absorptionTicks > 0 && --state.absorptionTicks == 0) {
			state.absorption = 0.0;
		}
		state.statuses.values().removeIf(status -> --status[1] <= 0);
		if (++state.focusClock >= FocusPool.REGEN_TICKS) {
			state.focusClock = 0;
			state.focus = Math.min(FocusPool.MAX, state.focus + 1);
		}
	}
}
