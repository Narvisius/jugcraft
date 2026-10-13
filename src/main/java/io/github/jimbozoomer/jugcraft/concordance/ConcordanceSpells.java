package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.cost.Ammo;
import org.jspecify.annotations.Nullable;

/**
 * The bridge to Spell Engine, Jugcraft's only use of its internals (kept in this one class, so a Spell Engine update
 * touches one file). The division of authority for a Concordance cast:
 * <ul>
 * <li><b>Spell Engine</b> owns the cast timeline, targeting, the cooldown ({@code cost.cooldown} in the spell), the
 * animations and the casting HUD.</li>
 * <li><b>Jugcraft</b> owns who may cast (an instrument in the main hand, the invocation understood, enough Focus), the
 * Focus spent and the effect. {@link #attempt} refuses a cast before it starts, on the client for the HUD and on the
 * server for real; the {@code CUSTOM} impact re-checks on the server and does the work ({@link Invocations},
 * {@link ComposedSpells}) and says what the cast owes ({@link #owe}); and that is settled once, in {@link #consume},
 * which Spell Engine calls only after the impact succeeded: the Focus is taken and the cooldown made at least the
 * composition's own, whatever shortened Spell Engine's. A refused or failed cast costs nothing and starts no
 * cooldown.</li>
 * </ul>
 * Spell Engine runs its event listeners without a try/finally: a listener that threw would switch the event off for
 * the rest of the session. Every listener here therefore catches what it throws.
 */
public final class ConcordanceSpells {
	/** The spell container source that offers a player their learned invocations while they hold an instrument. */
	public static final String SOURCE = "jugcraft:concordance";
	public static final TagKey<Spell> SPELLS = TagKey.create(SpellRegistry.KEY, Jugcraft.id("concordance"));
	/** Pseudo-tags whose translation names what is missing on Spell Engine's HUD ("Missing Focus"). */
	public static final TagKey<Item> MISSING_FOCUS = TagKey.create(Registries.ITEM, Jugcraft.id("concordance/focus"));
	public static final TagKey<Item> MISSING_INSTRUMENT = TagKey.create(Registries.ITEM, Jugcraft.id("concordance/instrument"));

	private static final Map<UUID, Settlement> PENDING = new HashMap<>();

	private ConcordanceSpells() {
	}

	/**
	 * What a cast that took effect owes, settled once when Spell Engine consumes its cost: the Focus, and the cooldown
	 * ({@code exact}: replaces Spell Engine's, for the inscribed-spell carrier; otherwise the least it may be).
	 */
	private record Settlement(String spell, int focus, int cooldown, boolean exact) {
	}

	static void register() {
		// Each impact registers itself: Invocations (jugcraft:invocation) and ComposedSpells (jugcraft:composed).
		SpellContainerSource.addSource(new SpellContainerSource.Entry(SOURCE, ConcordanceSpells::containers, ConcordanceSpells::sourceState));
		SpellEvents.CASTING_ATTEMPT.PRE.register(ConcordanceSpells::attempt);
		SpellEvents.COST_CONSUME.register(ConcordanceSpells::consume);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PENDING.remove(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
	}

	/** Records what a cast that took effect owes (server only), for {@link #consume} to settle. */
	static void owe(ServerPlayer player, String spell, int focus, int cooldown, boolean exact) {
		PENDING.put(player.getUUID(), new Settlement(spell, focus, cooldown, exact));
	}

	public static boolean holdsInstrument(Player player) {
		return player.getMainHandItem().is(JugcraftConcordance.INSTRUMENTS);
	}

	/** What the container source depends on; Spell Engine compares it each tick and rebuilds the source when it changes. */
	private record SourceState(boolean instrument, Map<String, Integer> invocations, @Nullable String inscription) {
	}

	private static Object sourceState(Player player) {
		Inscription inscription = ComposedSpells.inscription(player.getMainHandItem());
		return new SourceState(holdsInstrument(player), ConcordanceProgress.knowledge(player).invocations(),
				inscription == null ? null : inscription.text());
	}

	/**
	 * The player's learned invocations as one spell container, offered only while an instrument is in the main hand:
	 * Spell Engine accepts a cast request for any spell in any of a player's containers, so a container that exists only
	 * with an instrument in hand is what keeps invocations to instruments. An instrument with a spell inscribed on it adds
	 * the carrier {@value ComposedSpells#SPELL}. Runs on both sides; the client reads its synced copy of the player's
	 * knowledge and the item's inscription.
	 */
	private static List<SpellContainerSource.SourcedContainer> containers(Player player, String name) {
		try {
			if (!holdsInstrument(player)) {
				return List.of();
			}
			List<String> spells = new ArrayList<>(ConcordanceProgress.knowledge(player).invocations().keySet());
			if (ComposedSpells.inscription(player.getMainHandItem()) != null) {
				spells.add(ComposedSpells.SPELL);
			}
			if (spells.isEmpty()) {
				return List.of();
			}
			return List.of(new SpellContainerSource.SourcedContainer(name, null,
					new SpellContainer(SpellContainer.ContentType.NONE, "", "", "", 0, spells, 0)));
		} catch (RuntimeException problem) {
			Jugcraft.LOGGER.error("Arcane Concordance: could not list invocations", problem);
			return List.of();
		}
	}

	public static @Nullable String spellId(Holder<Spell> spell) {
		return spell.unwrapKey().map(key -> key.identifier().toString()).orElse(null);
	}

	/** Whether this is a Concordance spell (its tag, or one the player learned from Concordance research). */
	public static boolean isConcordance(Holder<Spell> spell, Knowledge knowledge) {
		String id = spellId(spell);
		return id != null && (spell.is(SPELLS) || knowledge.invocations().containsKey(id));
	}

	/** Why the player may not cast this Concordance spell now, or null if they may. Shared by the gate and the impact. */
	public static SpellCast.@Nullable Attempt refusal(Player player, Holder<Spell> spell) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			return SpellCast.Attempt.none();
		}
		if (!holdsInstrument(player)) {
			return SpellCast.Attempt.failMissingItem(new SpellCast.Attempt.MissingItemInfo(new Ammo.Searched(MISSING_INSTRUMENT, null)));
		}
		String id = spellId(spell);
		int cost;
		if (ComposedSpells.SPELL.equals(id)) {
			// The inscription's own Focus (the client's view); the server compiles it again in the impact.
			Inscription inscription = ComposedSpells.inscription(player.getMainHandItem());
			cost = inscription == null ? -1 : inscription.focus();
		} else {
			cost = id == null ? -1 : ConcordanceProgress.knowledge(player).invocationCost(id);
			// A tuning's Focus as the instrument records it (the client's view); the impact charges the server's.
			Tunings.Tuning tuning = cost < 0 ? null : Invocations.tunings(player.getMainHandItem()).get(id);
			if (tuning != null) {
				cost += tuning.focus();
			}
		}
		if (cost < 0) {
			return SpellCast.Attempt.none();
		}
		if (ConcordanceProgress.currentFocus(player) < cost) {
			return SpellCast.Attempt.failMissingItem(new SpellCast.Attempt.MissingItemInfo(new Ammo.Searched(MISSING_FOCUS, null)));
		}
		return null;
	}

	/** Spell Engine's gate before every cast (client and server). Null lets Spell Engine's own checks continue. */
	private static SpellCast.@Nullable Attempt attempt(SpellEvents.CastingAttemptEvent.Args args) {
		try {
			Player caster = args.caster();
			if (!isConcordance(args.spell(), ConcordanceProgress.knowledge(caster))) {
				return null;
			}
			return refusal(caster, args.spell());
		} catch (RuntimeException problem) {
			Jugcraft.LOGGER.error("Arcane Concordance: casting check failed", problem);
			return SpellCast.Attempt.none();
		}
	}

	/**
	 * After a Concordance cast took effect (server only, once per cast): takes the Focus its impact said it owes, makes
	 * its cooldown at least the composition's own, and records an invocation as evidence, in the chunk the caster stood
	 * in.
	 */
	private static void consume(SpellEvents.SpellCostConsumeEvent.Args args) {
		try {
			if (!(args.caster() instanceof ServerPlayer player)) {
				return;
			}
			Knowledge knowledge = ConcordanceProgress.knowledge(player);
			String id = spellId(args.spell());
			if (id == null || !isConcordance(args.spell(), knowledge)) {
				return;
			}
			Settlement settlement = PENDING.remove(player.getUUID());
			if (settlement == null || !settlement.spell().equals(id)) {
				Jugcraft.LOGGER.warn("Arcane Concordance: {} completed {} with nothing to settle", player.getName().getString(), id);
				return;
			}
			if (!ConcordanceProgress.spendFocus(player, settlement.focus())) {
				// The gate and the impact both checked; reaching here means the state changed in between.
				Jugcraft.LOGGER.warn("Arcane Concordance: {} completed {} without the Focus for it", player.getName().getString(), id);
			}
			// Spell Engine has already set its own cooldown, which haste or equipment may have shortened: never below
			// the composition's.
			((SpellCaster.Player) player).getCooldownManager().set(args.spell(), settlement.cooldown(), settlement.exact());
			if (ComposedSpells.SPELL.equals(id)) {
				return;
			}
			Definitions.Invocation invocation = ConcordanceData.rules().invocationForSpell(id);
			if (invocation != null) {
				ConcordanceProgress.record(player, new Evidence.Invoked(invocation.id(), ChunkPos.containing(player.blockPosition()).pack()));
			}
			Identifier spellId = Identifier.parse(id);
			ConcordanceProgress.award(player, Identifier.fromNamespaceAndPath(spellId.getNamespace(), "concordance_first_" + spellId.getPath()));
		} catch (RuntimeException problem) {
			Jugcraft.LOGGER.error("Arcane Concordance: settling a cast failed", problem);
		}
	}
}
