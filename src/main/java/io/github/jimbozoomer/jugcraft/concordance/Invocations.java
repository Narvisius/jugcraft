package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.compose.Authored;
import io.github.jimbozoomer.jugcraft.concordance.compose.Component;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.internals.SpellExecution;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import org.jspecify.annotations.Nullable;

/**
 * Authored invocations (roadmap step 10) on the server. An invocation is a Spell Engine spell (its cast time, gestures,
 * release sound and particles, cooldown) whose one impact is the {@code CUSTOM} handler {@code jugcraft:invocation};
 * what it does is a composition in the shared grammar, compiled when the rules load ({@link ConcordanceRules#authored})
 * under the limits of the instrument it is cast with, exactly as an inscribed spell on that instrument would be. So an
 * invocation is checked by the same compiler, delivered and targeted by the same server code and applied through the
 * same effect boundary as a composed spell, under a ledger of its plan's limits; it has no path of its own around them.
 * <p>
 * The impact, on the server only:
 * <ol>
 * <li>re-checks the cast ({@link ConcordanceSpells#refusal}): the gate may not have seen this request;</li>
 * <li>finds the plan for the instrument in the main hand, with the tuning set on it if the invocation offers that
 * modifier, the instrument holds it and the caster may use it, and scales its damage by the caster's Spell Power
 * where the composition says so ({@link Plan#scaled}, {@link #powerAboveBase});</li>
 * <li>runs it ({@link ComposedSpells#perform}): the delivery traced from the caster's own view, targets chosen and
 * effects applied on the server;</li>
 * <li>if nothing took effect, fails (no cooldown, no Focus) and tells the caster why; otherwise owes its Focus (the
 * invocation's cost for the caster's research, plus the tuning's) and a cooldown no shorter than the composition's
 * own, settled once by {@link ConcordanceSpells}.</li>
 * </ol>
 */
public final class Invocations {
	private static final SpellHandlers.ImpactResult FAILED = new SpellHandlers.ImpactResult(false, false);

	private Invocations() {
	}

	static void register() {
		SpellHandlers.registerCustomImpact(Jugcraft.id("invocation"), Invocations::impact);
	}

	/** The tunings recorded on an instrument (none if it has none). */
	public static Tunings tunings(ItemStack stack) {
		Tunings tunings = stack.get(JugcraftConcordance.TUNINGS);
		return tunings == null ? Tunings.EMPTY : tunings;
	}

	/**
	 * A cast resolved on the server: the invocation, the tuning in use (null for none), the plan it runs (tuned and
	 * scaled) and the Focus it costs this caster.
	 */
	public record Prepared(Definitions.Invocation invocation, @Nullable String tuning, Plan plan, int focus) {
	}

	/**
	 * What {@code player} would cast with this spell now, from the server's rules, research and the instrument in
	 * their main hand; null if it is not an invocation they know or it does not fit that instrument. A tuning the
	 * invocation no longer offers, the instrument cannot hold or the player may not use is left off, never charged.
	 */
	public static @Nullable Prepared prepare(ServerPlayer player, String spell) {
		ConcordanceRules rules = ConcordanceData.rules();
		Definitions.Invocation invocation = rules.invocationForSpell(spell);
		Instrument instrument = ComposedSpells.instrument(player);
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		int cost = knowledge.invocationCost(spell);
		if (invocation == null || instrument == null || cost < 0) {
			return null;
		}
		Authored form = rules.authored(invocation.id(), instrument.id());
		if (form == null) {
			return null;
		}
		Tunings.Tuning tuning = tunings(player.getMainHandItem()).get(spell);
		String modifier = tuning != null && form.tunings().containsKey(tuning.modifier()) && mayUse(knowledge, rules, tuning.modifier())
				? tuning.modifier() : null;
		Plan plan = form.plan(modifier);
		if (plan == null) {
			return null;
		}
		int focus = cost + (modifier == null ? 0 : form.tuningFocus(modifier));
		return new Prepared(invocation, modifier, plan.scaled(school -> powerAboveBase(player, school)), focus);
	}

	/** Whether the player's research lets them use a modifier, as it would in a composition of their own. */
	static boolean mayUse(Knowledge knowledge, ConcordanceRules rules, String modifier) {
		Component component = rules.catalog().component(modifier);
		return component != null && knowledge.state(component.requires().research()).atLeast(component.requires().state());
	}

	/**
	 * The caster's Spell Power in a magic school above that school's base value (0 for a school Spell Power does not
	 * know or one that is not magic): what an operation's scaling multiplies. Spell Power's own rules decide the
	 * value, equipment, enchantments, effects and Trinkets slots included.
	 */
	public static double powerAboveBase(LivingEntity caster, String schoolId) {
		SpellSchool school = SpellSchools.getSchool(schoolId);
		if (school == null || school.archetype != SpellSchool.Archetype.MAGIC) {
			return 0.0;
		}
		return Math.max(0.0, SpellPower.getSpellPower(school, caster).baseValue() - school.attributeBaseValue());
	}

	/** Spell Engine's {@code CUSTOM} impact {@code jugcraft:invocation}. */
	static SpellHandlers.ImpactResult impact(Holder<Spell> spell, SpellPower.Result power, LivingEntity caster, @Nullable Entity target,
			SpellExecution.ImpactContext context) {
		try {
			if (!(caster instanceof ServerPlayer player) || ConcordanceSpells.refusal(player, spell) != null) {
				return FAILED;
			}
			String id = ConcordanceSpells.spellId(spell);
			if (id == null) {
				return FAILED;
			}
			Prepared prepared = prepare(player, id);
			if (prepared == null) {
				if (ConcordanceData.rules().invocationForSpell(id) != null) {
					player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
							"message.jugcraft.concordance.invocation.no_instrument", net.minecraft.network.chat.Component.translatable(nameKey(id))));
				}
				return FAILED;
			}
			if (ConcordanceProgress.currentFocus(player) < prepared.focus()) {
				return FAILED;
			}
			Cause cause = Cause.of(player.getUUID(), Cause.Origin.INVOCATION, id, ConcordanceEffects.nextSerial());
			ComposedSpells.Outcome outcome = ComposedSpells.perform(player.level(), player, prepared.plan(), cause,
					new Ledger(prepared.plan().limits()));
			if (!outcome.applied()) {
				player.sendOverlayMessage(outcome.notAllowed()
						? net.minecraft.network.chat.Component.translatable("message.jugcraft.concordance.invocation.not_allowed")
						: net.minecraft.network.chat.Component.translatableWithFallback(fizzleKey(prepared.invocation().id()),
								"Nothing took effect", prepared.plan().root().range()));
				return FAILED;
			}
			ConcordanceSpells.owe(player, id, prepared.focus(), prepared.plan().cooldown(), false);
			return new SpellHandlers.ImpactResult(true, false);
		} catch (RuntimeException problem) {
			Jugcraft.LOGGER.error("Arcane Concordance: an invocation failed", problem);
			return FAILED;
		}
	}

	/** The translation key of a spell's name ({@code spell.<namespace>.<path>.name}, as Spell Engine names it). */
	public static String nameKey(String spell) {
		Identifier id = Identifier.parse(spell);
		return "spell." + id.getNamespace() + "." + id.getPath() + ".name";
	}

	/** What the caster is told when an invocation found nothing to act on. */
	static String fizzleKey(String invocation) {
		Identifier id = Identifier.parse(invocation);
		return "message." + id.getNamespace() + ".concordance.fizzle." + id.getPath();
	}
}
