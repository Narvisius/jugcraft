package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.EvidenceRule;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The server's hold on each player's Concordance state: their {@link Knowledge} and their {@link FocusPool}, both
 * Fabric attachments that are saved with the player, kept through death and sent only to that player. Everything that
 * changes them goes through here, on the server thread, so a reward is never applied from a client's claim and never
 * twice: evidence is recorded by key, Focus is spent only when there is enough.
 */
public final class ConcordanceProgress {
	private static final Map<String, TagKey<Item>> TAGS = new ConcurrentHashMap<>();

	/** Who hears every recorded piece of evidence ({@link #listen}). */
	private static final java.util.List<Listener> LISTENERS = new java.util.concurrent.CopyOnWriteArrayList<>();

	/** Item tag membership for the rules, from the running registries. */
	public static final EvidenceRule.TagLookup TAG_LOOKUP = (item, tag) -> {
		Identifier id = Identifier.tryParse(item);
		Identifier tagId = Identifier.tryParse(tag);
		if (id == null || tagId == null) {
			return false;
		}
		// An unknown id reads as air, which is in no specimen tag.
		TagKey<Item> key = TAGS.computeIfAbsent(tag, unused -> TagKey.create(Registries.ITEM, tagId));
		return BuiltInRegistries.ITEM.getValue(id).builtInRegistryHolder().is(key);
	};

	private ConcordanceProgress() {
	}

	public static String itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	public static Knowledge knowledge(Player player) {
		return player.getAttachedOrElse(JugcraftConcordance.KNOWLEDGE, Knowledge.EMPTY);
	}

	public static FocusPool focus(Player player) {
		return player.getAttachedOrElse(JugcraftConcordance.FOCUS, FocusPool.FULL);
	}

	/** The clock Focus and lanterns count by: the level's game time, which only moves forward. */
	public static long now(Level level) {
		return level.getGameTime();
	}

	public static int currentFocus(Player player) {
		return focus(player).current(now(player.level()));
	}

	/**
	 * Takes {@code cost} Focus from the player if they have it, and says whether it did. The only way Focus is spent,
	 * so there is one place a cast or a working pays.
	 */
	public static boolean spendFocus(ServerPlayer player, int cost) {
		FocusPool after = focus(player).spend(now(player.level()), cost);
		if (after == null) {
			return false;
		}
		player.setAttached(JugcraftConcordance.FOCUS, after);
		return true;
	}

	/** Operators and tests: sets the player's Focus now. */
	public static void setFocus(ServerPlayer player, int amount) {
		player.setAttached(JugcraftConcordance.FOCUS, new FocusPool(amount, now(player.level())));
	}

	/**
	 * Records evidence the server saw the player produce, then tells them about any research it advanced (an action-bar
	 * line, a sound and the advancement the codex reads). Returns the engine's result; a repeat records nothing.
	 */
	public static ResearchEngine.Result record(ServerPlayer player, Evidence evidence) {
		Knowledge before = knowledge(player);
		ResearchEngine.Result result = ResearchEngine.apply(before, ConcordanceData.rules(), evidence, TAG_LOOKUP);
		if (result.recorded()) {
			player.setAttached(JugcraftConcordance.KNOWLEDGE, result.knowledge());
			announce(player, result);
		}
		for (Listener listener : LISTENERS) {
			listener.recorded(player, evidence, result);
		}
		return result;
	}

	/**
	 * Something that hears every piece of evidence the server records for a player, repeats included, and what it changed
	 * (roadmap step 23: the Starbound Conclave recognises research advanced, practices carried through and notes that
	 * taught someone). Listeners only read: the research itself has already been recorded.
	 */
	@FunctionalInterface
	public interface Listener {
		void recorded(ServerPlayer player, Evidence evidence, ResearchEngine.Result result);
	}

	/** Adds a listener (at registration). */
	public static void listen(Listener listener) {
		LISTENERS.add(listener);
	}

	private static void announce(ServerPlayer player, ResearchEngine.Result result) {
		ResearchEngine.Transition last = null;
		for (ResearchEngine.Transition transition : result.transitions()) {
			award(player, advancementFor(transition.research(), transition.to()));
			last = transition;
		}
		if (last != null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.research." + last.to().id(),
					researchName(last.research())));
			player.level().playSound(null, player.blockPosition(), last.to() == ResearchState.ENCOUNTERED
					? JugcraftConcordance.EXAMINE_SOUND : JugcraftConcordance.STUDY_COMPLETE_SOUND, SoundSource.PLAYERS, 0.8F, 1.0F);
		}
	}

	public static Component researchName(String research) {
		Identifier id = Identifier.tryParse(research);
		if (id == null) {
			return Component.literal(research);
		}
		return Component.translatableWithFallback("research." + id.getNamespace() + "." + id.getPath().replace('/', '.'), research);
	}

	/**
	 * The advancement that marks a research state, which the codex's research bridge waits for:
	 * {@code <ns>:concordance_<path>_<state>}. Research from a data pack without one simply has no codex fact.
	 */
	public static Identifier advancementFor(String research, ResearchState state) {
		Identifier id = Identifier.parse(research);
		return Identifier.fromNamespaceAndPath(id.getNamespace(), "concordance_" + id.getPath().replace('/', '_') + "_" + state.id());
	}

	/** Grants a single-criterion ("done") advancement, if it exists. */
	public static void award(ServerPlayer player, Identifier advancement) {
		MinecraftServer server = player.level().getServer();
		AdvancementHolder holder = server == null ? null : server.getAdvancements().get(advancement);
		if (holder != null) {
			player.getAdvancements().award(holder, "done");
		}
	}

	/**
	 * Works out what the player may cast from their research under the current rules, after a reload or on joining,
	 * and re-awards the advancements for states they hold (the codex may have been reset or added since).
	 */
	public static void relearn(ServerPlayer player) {
		Knowledge before = knowledge(player);
		Knowledge after = ResearchEngine.relearn(before, ConcordanceData.rules());
		if (!after.equals(before)) {
			player.setAttached(JugcraftConcordance.KNOWLEDGE, after);
		}
		for (Map.Entry<String, Knowledge.Progress> entry : after.entries().entrySet()) {
			Definitions.Research research = ConcordanceData.rules().research(entry.getKey());
			if (research == null) {
				continue;
			}
			for (ResearchState state : research.states().keySet()) {
				if (entry.getValue().state().atLeast(state)) {
					award(player, advancementFor(entry.getKey(), state));
				}
			}
		}
	}

	public static void relearnAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			relearn(player);
		}
	}

	/** Operators and tests: sets a research state directly, awarding the codex advancements up to it. */
	public static void grant(ServerPlayer player, String research, ResearchState state) {
		ConcordanceRules rules = ConcordanceData.rules();
		player.setAttached(JugcraftConcordance.KNOWLEDGE, ResearchEngine.grant(knowledge(player), rules, research, state));
		relearn(player);
	}

	/** Operators: forgets all the player's Concordance research (their advancements stay; revoke those separately). */
	public static void reset(ServerPlayer player) {
		player.setAttached(JugcraftConcordance.KNOWLEDGE, Knowledge.EMPTY);
	}

	/** The working's definition if the player's research lets them run it, else null. */
	public static Definitions.@Nullable Working knownWorking(Player player, String working) {
		Definitions.Working definition = ConcordanceData.rules().working(working);
		return definition != null && ResearchEngine.knowsWorking(knowledge(player), definition) ? definition : null;
	}

	static void log(String message, Object... args) {
		Jugcraft.LOGGER.info("[concordance] " + message, args);
	}
}
