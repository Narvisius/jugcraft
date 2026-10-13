package io.github.jimbozoomer.jugcraft.concordance.balance;

import io.github.jimbozoomer.jugcraft.concordance.balance.Benchmark.Ability;
import io.github.jimbozoomer.jugcraft.concordance.balance.Benchmark.Armour;
import io.github.jimbozoomer.jugcraft.concordance.balance.Benchmark.Behaviour;
import io.github.jimbozoomer.jugcraft.concordance.balance.Benchmark.Character;
import io.github.jimbozoomer.jugcraft.concordance.balance.Benchmark.Encounter;
import io.github.jimbozoomer.jugcraft.concordance.balance.Benchmark.Foe;
import io.github.jimbozoomer.jugcraft.concordance.balance.Benchmark.Goal;
import io.github.jimbozoomer.jugcraft.concordance.balance.Benchmark.Outcome;
import io.github.jimbozoomer.jugcraft.concordance.balance.Benchmark.Weapon;
import io.github.jimbozoomer.jugcraft.concordance.compose.Authored;
import io.github.jimbozoomer.jugcraft.concordance.compose.Compiler;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The Concordance's combat and progression baselines (roadmap step 11): five encounter shapes, a roster of
 * characters built from the loaded rules, seven measures for each character, and the acceptance checks. The same code
 * runs in the standalone harness over the generated data and in a game test over the rules and Spell Engine spells the
 * server actually loaded ({@code ConcordanceBaselineGameTests}), so the numbers recorded in
 * docs/features/arcane-concordance-baselines.md are the server's.
 */
public final class Baselines {
	public static final String WAND = "jugcraft:initiate_wand";
	/** Health left that counts as all but unharmed: at most a heart lost. */
	public static final double UNHARMED = Benchmark.PLAYER_HEALTH - 2.0;
	/** The arcane Spell Power above the base that the geared striker's equipment gives (a +4 item, or two +2). */
	public static final double GEARED_POWER = 4.0;
	/**
	 * The fire Spell Power above the base of the most the Hearthbinder's regalia gives with the default slots: a Focus of
	 * Fire and the whole Pyromancer's set (concordance/ember/EmberGear: 4 + 4 x 0.5).
	 */
	public static final double GEARED_FIRE_POWER = 6.0;
	private static final String FIRE = "spell_power:fire";

	private Baselines() {
	}

	/** Spell Engine's cast and cooldown times for a spell, in ticks (read from the spell data). */
	public record Timing(int castTicks, int cooldownTicks) {
	}

	// ---------------------------------------------------------------------------------------------------- the foes

	/** A zombie-like brute: 20 health, 2 armour, 2.3 blocks a second, 3 damage a second. */
	public static final Foe BRUTE = new Foe("brute", Behaviour.BRUTE, 20.0, 2, 0.115, 3.0, 20, 1.5, 12.0);
	/** A fast skirmisher that strikes and retreats: 16 health, 5 blocks a second, 4 damage a strike. */
	public static final Foe RUNNER = new Foe("runner", Behaviour.RUNNER, 16.0, 0, 0.25, 4.0, 20, 1.5, 14.0);
	/** A guard in iron: 30 health, 6 armour, 4 damage a second once you come within 4 blocks of it. */
	public static final Foe GUARD = new Foe("guard", Behaviour.GUARD, 30.0, 6, 0.1, 4.0, 20, 1.5, 10.0);
	/** An archer behind its guard: 20 health, 2 damage every 1.5 seconds within 16 blocks. */
	public static final Foe ARCHER = new Foe("archer", Behaviour.ARCHER, 20.0, 0, 0.0, 2.0, 30, 0.0, 14.0);

	/** The five controlled encounter shapes, each a minute at most. */
	public static List<Encounter> encounters() {
		int minute = 60 * Benchmark.TICKS;
		return List.of(
				new Encounter("isolated", "Isolated target", List.of(BRUTE), 0.0, minute, Goal.DEFEAT_ALL),
				new Encounter("cluster", "Clustered group", List.of(BRUTE.at(10.0), BRUTE.at(10.5), BRUTE.at(11.0)), 0.0, minute, Goal.DEFEAT_ALL),
				new Encounter("mobile", "Mobile opponent", List.of(RUNNER), 0.0, minute, Goal.DEFEAT_ALL),
				new Encounter("protected", "Protected target", List.of(GUARD, ARCHER), 0.0, minute, Goal.DEFEAT_PROTECTED),
				new Encounter("objective", "Objective defense", List.of(BRUTE.at(12.0), BRUTE.at(16.0), BRUTE.at(20.0)), 20.0, minute,
						Goal.DEFEND));
	}

	// ----------------------------------------------------------------------------------------------- the abilities

	/** Builds abilities from the rules, the instrument and Spell Engine's timings. */
	public record Source(ConcordanceRules rules, String instrument, Map<String, Timing> timings) {
		/**
		 * An invocation as a caster whose First Light research has reached {@code reached} casts it, with a tuning (or
		 * none) and their arcane Spell Power above the base; null if it does not fit the instrument.
		 */
		public @Nullable Ability invocation(String id, @Nullable String tuning, ResearchState reached, double arcaneAboveBase) {
			return invocation(id, tuning, reached, "spell_power:arcane", arcaneAboveBase);
		}

		/** The same, with {@code aboveBase} points of Spell Power in {@code school} (and none in any other). */
		public @Nullable Ability invocation(String id, @Nullable String tuning, ResearchState reached, String school, double aboveBase) {
			Definitions.Invocation invocation = rules.invocation(id);
			Authored form = invocation == null ? null : rules.authored(id, instrument);
			Plan plan = form == null ? null : form.plan(tuning);
			Timing timing = invocation == null ? null : timings.get(invocation.spell());
			if (plan == null || timing == null) {
				return null;
			}
			int focus = invocation.cost(reached) + (tuning == null ? 0 : form.tuningFocus(tuning));
			Plan scaled = plan.scaled(scaling -> scaling.equals(school) ? aboveBase : 0.0);
			return Ability.of(tuning == null ? id : id + "+" + tuning, invocation.role(), scaled, focus, timing.castTicks(), timing.cooldownTicks());
		}

		/** A composition inscribed on the instrument by a master of First Light, cast through the carrier spell. */
		public @Nullable Ability composed(String text, Definitions.Role role) {
			Instrument wand = rules.catalog().instruments().get(instrument);
			Timing timing = timings.get("jugcraft:composed");
			Plan plan = wand == null || timing == null ? null
					: Compiler.compile(text, rules.catalog(), research -> ResearchState.MASTERED, wand).plan();
			return plan == null ? null : Ability.of("composed:" + text, role, plan, plan.focus(), timing.castTicks(), timing.cooldownTicks());
		}

		private List<Ability> all(Object... abilities) {
			List<Ability> out = new ArrayList<>();
			for (Object ability : abilities) {
				if (!(ability instanceof Ability present)) {
					throw new IllegalStateException("a baseline ability does not load: " + List.of(abilities));
				}
				out.add(present);
			}
			return out;
		}

		/**
		 * The roster. Each full kit is a different approach with different equipment; the single-ability characters
		 * (bare-handed, unarmoured) test that no early ability alone trivializes the encounters.
		 */
		public List<Character> characters() {
			ResearchState understood = ResearchState.UNDERSTOOD;
			ResearchState mastered = ResearchState.MASTERED;
			List<Character> roster = new ArrayList<>(List.of(
					new Character("Fighter (no magic)", Armour.IRON, Weapon.IRON_SWORD, List.of()),
					new Character("Initiate (utility)", Armour.LEATHER, Weapon.WOODEN_SWORD, all(
							invocation("jugcraft:aegis", null, understood, 0.0), invocation("jugcraft:kindle", null, understood, 0.0),
							invocation("jugcraft:revelation", null, understood, 0.0))),
					new Character("Striker", Armour.NONE, Weapon.WOODEN_SWORD, all(
							invocation("jugcraft:aegis", null, mastered, 0.0), invocation("jugcraft:flashstep", null, mastered, 0.0),
							invocation("jugcraft:lance", null, mastered, 0.0))),
					new Character("Geared striker (+4 arcane, iron)", Armour.IRON, Weapon.WOODEN_SWORD, all(
							invocation("jugcraft:aegis", null, mastered, GEARED_POWER), invocation("jugcraft:flashstep", null, mastered, GEARED_POWER),
							invocation("jugcraft:lance", null, mastered, GEARED_POWER))),
					new Character("Warden", Armour.CHAIN, Weapon.WOODEN_SWORD, all(
							invocation("jugcraft:lanternward", null, mastered, 0.0), invocation("jugcraft:lance", null, mastered, 0.0))),
					new Character("Skirmisher", Armour.LEATHER, Weapon.IRON_SWORD, all(
							invocation("jugcraft:flashstep", null, mastered, 0.0), invocation("jugcraft:lance", "jugcraft:extend", mastered, 0.0))),
					new Character("Composer", Armour.LEATHER, Weapon.WOODEN_SWORD, all(
							invocation("jugcraft:aegis", null, mastered, 0.0),
							composed("ray struck sear then here creatures dazzle", Definitions.Role.DAMAGE))),
					// The Hearthbinder's regalia at its most: a Focus of Fire and the Pyromancer's set (leather's protection).
					// The Fire Bangle's blow is not modelled (the model's strikes have no on-hit status).
					new Character("Geared hearthbinder (+6 fire, Pyromancer's)", Armour.LEATHER, Weapon.WOODEN_SWORD, all(
							invocation("jugcraft:hearthguard", null, mastered, FIRE, GEARED_FIRE_POWER),
							invocation("jugcraft:cinderbolt", null, mastered, FIRE, GEARED_FIRE_POWER),
							invocation("jugcraft:hearthflare", null, mastered, FIRE, GEARED_FIRE_POWER)))));
			for (Definitions.Invocation invocation : rules.invocations().values()) {
				Ability alone = invocation(invocation.id(), null, invocation.state(), 0.0);
				if (alone != null) {
					roster.add(new Character("Only " + invocation.id(), Armour.NONE, Weapon.FIST, List.of(alone)));
				}
			}
			roster.add(new Character("Initiate without Aegis", Armour.LEATHER, Weapon.WOODEN_SWORD, all(
					invocation("jugcraft:kindle", null, understood, 0.0), invocation("jugcraft:revelation", null, understood, 0.0))));
			return roster;
		}
	}

	// ----------------------------------------------------------------------------------------------- the measures

	/**
	 * The seven measures for a character: sustained output (damage a second over a minute against a dummy within
	 * reach), burst (damage in the first 5 seconds, Focus full), survivability (seconds lasted, at most 60, beside a
	 * brute that cannot be killed), movement (blocks its abilities carry it in a minute), control (foe-seconds slowed or
	 * pushed back in a minute against the dummy), resource efficiency (damage per Focus over the minute; 0 without
	 * spells) and support (health and absorption a spell that chooses allies gives each of them, within a minute
	 * under fire from an archer that cannot be killed).
	 */
	public record Measures(double sustained, double burst, double survival, double movement, double control, double efficiency,
			double support) {
	}

	private static final Foe DUMMY = new Foe("dummy", Behaviour.DUMMY, 1.0E9, 2, 0.0, 0.0, 20, 1.5, 2.5);
	private static final Foe UNKILLABLE = new Foe("brute that cannot be killed", Behaviour.BRUTE, 1.0E9, 2, 0.115, 3.0, 20, 1.5, 2.5);
	private static final Foe UNKILLABLE_ARCHER = new Foe("archer that cannot be killed", Behaviour.ARCHER, 1.0E9, 0, 0.0, 2.0, 30, 0.0, 14.0);

	public static Measures measure(Character character) {
		int minute = 60 * Benchmark.TICKS;
		Outcome sustained = Benchmark.run(character, new Encounter("dummy", "Dummy", List.of(DUMMY), 0.0, minute, Goal.DEFEAT_ALL));
		Outcome burst = Benchmark.run(character, new Encounter("burst", "Dummy", List.of(DUMMY), 0.0, 5 * Benchmark.TICKS, Goal.DEFEAT_ALL));
		Outcome survival = Benchmark.run(character, new Encounter("survival", "Unkillable brute", List.of(UNKILLABLE), 0.0, minute,
				Goal.DEFEND));
		Outcome support = Benchmark.run(character, new Encounter("support", "Unkillable archer", List.of(UNKILLABLE_ARCHER), 0.0, minute,
				Goal.DEFEND));
		double movement = 0.0;
		for (Ability ability : character.abilities()) {
			if (ability.role() == Definitions.Role.MOVEMENT) {
				double reach = ability.plan().root().steps().getFirst().effect().magnitude() / 10.0 * Benchmark.PUSH_REACH;
				movement += reach * castsInAMinute(ability);
			}
		}
		return new Measures(sustained.damage() / 60.0, burst.damage(), survival.ticks() / (double) Benchmark.TICKS, movement,
				sustained.control() / Benchmark.TICKS, sustained.focusSpent() == 0 ? 0.0 : sustained.spellDamage() / sustained.focusSpent(),
				support.support());
	}

	/** How often an ability alone can be cast in a minute: its cooldown, or its Focus (full, then regenerating). */
	static int castsInAMinute(Ability ability) {
		int minute = 60 * Benchmark.TICKS;
		int byCooldown = 1 + minute / Math.max(1, ability.cooldownTicks());
		int byFocus = (FocusPool.MAX + minute / FocusPool.REGEN_TICKS) / Math.max(1, ability.focus());
		return Math.min(byCooldown, byFocus);
	}

	// ----------------------------------------------------------------------------------------------- the report

	/** Every character's outcome in every encounter and its measures, with the acceptance checks' findings. */
	public record Report(List<Encounter> encounters, Map<Character, Map<String, Outcome>> outcomes, Map<Character, Measures> measures,
			List<String> failures) {
		public boolean accepted() {
			return failures.isEmpty();
		}

		/** The results as Markdown tables, as the baselines record shows them. */
		public String table() {
			StringBuilder out = new StringBuilder("| Character | ");
			for (Encounter encounter : encounters) {
				out.append(encounter.name()).append(" | ");
			}
			out.append("\n|---|").append("---|".repeat(encounters.size())).append('\n');
			for (Map.Entry<Character, Map<String, Outcome>> row : outcomes.entrySet()) {
				out.append("| ").append(row.getKey().name()).append(" | ");
				for (Encounter encounter : encounters) {
					Outcome outcome = row.getValue().get(encounter.id());
					out.append(outcome.won() ? String.format(Locale.ROOT, "won %.1f s, %.0f HP", outcome.ticks() / 20.0, outcome.health())
							: String.format(Locale.ROOT, "lost %.1f s", outcome.ticks() / 20.0)).append(" | ");
				}
				out.append('\n');
			}
			out.append("\n| Character | Sustained (dmg/s) | Burst (5 s) | Survival (s) | Movement (blocks/min) | Control (foe-s/min) "
					+ "| Efficiency (dmg/Focus) | Support (per ally/min) |\n|---|---|---|---|---|---|---|---|\n");
			for (Map.Entry<Character, Measures> row : measures.entrySet()) {
				Measures m = row.getValue();
				out.append(String.format(Locale.ROOT, "| %s | %.2f | %.1f | %.1f | %.1f | %.1f | %.2f | %.1f |%n", row.getKey().name(),
						m.sustained(), m.burst(), m.survival(), m.movement(), m.control(), m.efficiency(), m.support()));
			}
			return out.toString();
		}
	}

	/**
	 * Runs the roster through the encounters and checks the acceptance rules of roadmap step 11:
	 * <ol>
	 * <li>no early ability trivializes the encounters: no character with a single invocation wins every one, and no
	 * character wins every one all but unharmed (losing at most a heart in each);</li>
	 * <li>several approaches succeed: every encounter is won by at least two characters whose armour or weapon
	 * differ;</li>
	 * <li>the utility character keeps meaningful survival options: it wins the isolated encounter, and Dawn Aegis lets
	 * it last longer beside the unkillable brute than the same kit without it;</li>
	 * <li>Spell Power changes only what scales with it: the geared striker's Lance deals more than the plain
	 * striker's, and its shield does not; the geared hearthbinder's Cinderbolt and Hearthflare deal more than the plain
	 * ones, and its Hearthguard does not change.</li>
	 * </ol>
	 */
	public static Report report(Source source) {
		List<Encounter> encounters = encounters();
		Map<Character, Map<String, Outcome>> outcomes = new LinkedHashMap<>();
		Map<Character, Measures> measures = new LinkedHashMap<>();
		List<Character> roster = source.characters();
		for (Character character : roster) {
			Map<String, Outcome> row = new LinkedHashMap<>();
			for (Encounter encounter : encounters) {
				row.put(encounter.id(), Benchmark.run(character, encounter));
			}
			outcomes.put(character, row);
			measures.put(character, measure(character));
		}
		List<String> failures = new ArrayList<>();
		for (Map.Entry<Character, Map<String, Outcome>> row : outcomes.entrySet()) {
			Character character = row.getKey();
			boolean all = row.getValue().values().stream().allMatch(Outcome::won);
			if (all && character.abilities().size() == 1) {
				failures.add(character.name() + " wins every encounter with one invocation");
			}
			if (row.getValue().values().stream().allMatch(outcome -> outcome.won() && outcome.health() >= UNHARMED)) {
				failures.add(character.name() + " wins every encounter all but unharmed: nothing is a challenge");
			}
		}
		for (Encounter encounter : encounters) {
			List<Character> winners = outcomes.entrySet().stream().filter(row -> row.getValue().get(encounter.id()).won()).map(Map.Entry::getKey)
					.toList();
			boolean varied = winners.stream().anyMatch(a -> winners.stream().anyMatch(b -> !a.armour().equals(b.armour())
					|| !a.weapon().equals(b.weapon())));
			if (!varied) {
				failures.add(encounter.name() + " is won by " + winners.size() + " kit(s) only: " + winners.stream().map(Character::name).toList());
			}
		}
		Character utility = named(roster, "Initiate (utility)");
		Character bare = named(roster, "Initiate without Aegis");
		if (!outcomes.get(utility).get("isolated").won()) {
			failures.add("the utility character cannot win the isolated encounter");
		}
		if (measures.get(utility).survival() <= measures.get(bare).survival()) {
			failures.add("Dawn Aegis gives the utility character no longer survival: " + measures.get(utility).survival() + " s against "
					+ measures.get(bare).survival() + " s");
		}
		Character striker = named(roster, "Striker");
		Character geared = named(roster, "Geared striker (+4 arcane, iron)");
		if (lance(geared) <= lance(striker) || shield(geared) != shield(striker)) {
			failures.add("Spell Power should raise the Lance only: Lance " + lance(striker) + " -> " + lance(geared) + ", Aegis " + shield(striker)
					+ " -> " + shield(geared));
		}
		// The same for fire: the regalia raises Cinderbolt and Hearthflare, never Hearthguard.
		Character hearthbinder = named(roster, "Geared hearthbinder (+6 fire, Pyromancer's)");
		for (String id : List.of("jugcraft:cinderbolt", "jugcraft:hearthflare")) {
			if (magnitude(hearthbinder, id) <= magnitude(named(roster, "Only " + id), id)) {
				failures.add("Fire Spell Power should raise " + id + ": " + magnitude(named(roster, "Only " + id), id) + " -> "
						+ magnitude(hearthbinder, id));
			}
		}
		if (magnitude(hearthbinder, "jugcraft:hearthguard") != magnitude(named(roster, "Only jugcraft:hearthguard"), "jugcraft:hearthguard")) {
			failures.add("Fire Spell Power should not change Hearthguard");
		}
		return new Report(encounters, outcomes, measures, failures);
	}

	private static Character named(List<Character> roster, String name) {
		return roster.stream().filter(character -> character.name().equals(name)).findFirst().orElseThrow();
	}

	private static int lance(Character character) {
		return character.abilities().stream().filter(a -> a.id().startsWith("jugcraft:lance")).findFirst().orElseThrow().plan().root()
				.steps().getFirst().effect().magnitude();
	}

	/** The first step's magnitude of {@code character}'s invocation {@code id}. */
	private static int magnitude(Character character, String id) {
		return character.abilities().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow().plan().root().steps().getFirst()
				.effect().magnitude();
	}

	private static int shield(Character character) {
		return character.abilities().stream().filter(a -> a.id().equals("jugcraft:aegis")).findFirst().orElseThrow().plan().root()
				.steps().getFirst().effect().magnitude();
	}
}
