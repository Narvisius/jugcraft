package io.github.jimbozoomer.jugcraft.concordance.encyclopedia;

import io.github.jimbozoomer.jugcraft.concordance.ComposedSpells;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Inscription;
import io.github.jimbozoomer.jugcraft.concordance.Invocations;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.compose.Composition;
import io.github.jimbozoomer.jugcraft.concordance.compose.Slot;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.composer.Composer;
import io.github.jimbozoomer.jugcraft.concordance.composer.ComposerStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload.Link;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaRequestPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.PageWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The encyclopedia's Spellcraft page (Concordance Chunk A, A-04): what the player can cast and compose now. The
 * instrument in their main hand and its limits (the Spell Composer's own reading, {@link Composer#state}), their Focus,
 * what is inscribed, the invocations they know with what each costs them, the words they have learned for composing and
 * how many are still to learn, and the way into the Spell Composer. Shown once the player has begun First Light or
 * carries an instrument. Words and invocations not yet learned are only counted. It only reads.
 */
public final class SpellcraftPage {
	public static final String PAGE = "spellcraft";

	private SpellcraftPage() {
	}

	public static void write(ServerPlayer player, EncyclopediaRequestPayload request, PageWriter out) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			return;
		}
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		boolean begun = knowledge.state(FirstLightRoute.FIRST_LIGHT) != ResearchState.NONE;
		if (!begun && !FirstLightRoute.carries(player, stack -> stack.is(JugcraftConcordance.INSTRUMENTS))) {
			return;
		}
		ComposerStatePayload state = Composer.state(player, 0);

		out.heading(Component.translatable("encyclopedia.jugcraft.spellcraft.instrument_heading"));
		ItemStack held = player.getMainHandItem();
		if (state.instrument().isPresent()) {
			ComposerStatePayload.InstrumentView instrument = state.instrument().get();
			out.fact(Component.translatable("encyclopedia.jugcraft.spellcraft.instrument", held.getHoverName()),
					Component.translatable("encyclopedia.jugcraft.spellcraft.instrument.limits", instrument.capacity(), instrument.targets(),
							instrument.work(), instrument.branches(), Text.seconds(instrument.duration())), List.of(held.getItem()));
			Inscription inscription = ComposedSpells.inscription(held);
			out.fact(inscription == null ? Component.translatable("encyclopedia.jugcraft.spellcraft.no_inscription")
					: Component.translatable("compose.jugcraft.inscription", inscription.text()),
					inscription == null ? null : Component.translatable("compose.jugcraft.inscription_cost", inscription.focus(), Text.seconds(inscription.cooldown())),
					List.of());
		} else {
			out.fact(Component.translatable("encyclopedia.jugcraft.spellcraft.no_instrument"), null, carried(player));
		}
		out.fact(Component.translatable("journal.jugcraft.overview.focus", ConcordanceProgress.currentFocus(player), ConcordanceProgress.focus(player).capacity()),
				Component.translatable("encyclopedia.jugcraft.spellcraft.focus_regen", Text.seconds(FocusPool.REGEN_TICKS)), List.of());
		out.codex("foundations/focus", null, null);

		out.heading(Component.translatable("encyclopedia.jugcraft.spellcraft.invocations"));
		for (Map.Entry<String, Integer> invocation : knowledge.invocations().entrySet()) {
			Identifier spell = Identifier.tryParse(invocation.getKey());
			out.fact(Component.translatable("encyclopedia.jugcraft.route.cast.invocation", Component.translatable(Invocations.nameKey(invocation.getKey())),
					invocation.getValue()), spell == null ? null : Component.translatable("spell." + spell.getNamespace() + "." + spell.getPath() + ".description"),
					List.of());
		}
		int unknown = Math.max(0, ConcordanceData.rules().invocations().size() - knowledge.invocations().size());
		if (knowledge.invocations().isEmpty()) {
			out.fact(Component.translatable("encyclopedia.jugcraft.spellcraft.no_invocations"));
		}
		if (unknown > 0) {
			out.fact(Component.translatable("encyclopedia.jugcraft.spellcraft.invocations_unknown", unknown));
		}
		out.codex("invocations/kindle", "jugcraft:first_light", ResearchState.UNDERSTOOD);

		out.heading(Component.translatable("encyclopedia.jugcraft.spellcraft.words"));
		for (Slot slot : Slot.values()) {
			MutableComponent words = Component.empty();
			int count = 0;
			for (ComposerStatePayload.Word word : state.words()) {
				if (word.slot() != slot.ordinal()) {
					continue;
				}
				if (count++ > 0) {
					words.append(", ");
				}
				words.append(name(word.id())).append(" ").append(Component.translatable("screen.jugcraft.composer.word_cost", word.capacity(), word.focus()));
			}
			if (count > 0) {
				out.fact(Component.translatable("encyclopedia.jugcraft.spellcraft.slot", Component.translatable("screen.jugcraft.composer.slot." + slot.id()), words));
			}
		}
		if (state.words().isEmpty()) {
			out.fact(Component.translatable("encyclopedia.jugcraft.spellcraft.no_words"));
		}
		if (state.locked() > 0) {
			out.fact(Component.translatable("screen.jugcraft.composer.locked", state.locked()));
		}
		if (state.status() == ComposerStatePayload.READY) {
			out.link(Component.translatable("encyclopedia.jugcraft.link.composer"), Link.composer());
		} else {
			out.fact(Component.translatable("encyclopedia.jugcraft.spellcraft.compose_needs"));
		}
		out.codex("composition/composing", "jugcraft:first_light", ResearchState.UNDERSTOOD);
	}

	/** A word's name, as the Spell Composer shows it. */
	private static Component name(String id) {
		Identifier parsed = Identifier.tryParse(id);
		String key = parsed == null ? "component.jugcraft." + id : "component." + parsed.getNamespace() + "." + parsed.getPath().replace('/', '.');
		return Component.translatableWithFallback(key, Composition.shortName(id));
	}

	/** The instruments the player carries anywhere but the main hand. */
	private static List<Item> carried(ServerPlayer player) {
		List<Item> out = new ArrayList<>();
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack stack = player.getInventory().getItem(slot);
			if (stack.is(JugcraftConcordance.INSTRUMENTS) && !out.contains(stack.getItem())) {
				out.add(stack.getItem());
			}
		}
		return out;
	}
}
