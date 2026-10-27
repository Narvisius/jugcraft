package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * Shows the compiler's {@link Text} in the player's language: the key under {@code compose.jugcraft.}, numbers and
 * strings as they are, and named things by their translated names (a component's {@code component.<ns>.<path>}, a
 * research entry's, a status effect's, an item's; a block by its block name, a preparation by
 * {@code preparation.<ns>.<path>}, since C-03 a ritual structure by {@code structure.<ns>.<path>}, and since C-09 a graft by
 * {@code graft.<ns>.<path>}). A line inside a line (a diagnostic's step and refusal, Concordance Chunk C, C-01) is shown the
 * same way.
 */
public final class ComposeText {
	private ComposeText() {
	}

	public static Component show(Text text) {
		Object[] args = new Object[text.args().size()];
		for (int i = 0; i < args.length; i++) {
			Object arg = text.args().get(i);
			args[i] = arg instanceof Text.Ref ref ? name(ref) : arg instanceof Text line ? show(line) : arg instanceof List<?> list ? names(list) : arg;
		}
		return Component.translatable(text.translationKey(), args);
	}

	/** Named things listed together (the ingredients behind a property), separated by commas. */
	private static Component names(List<?> refs) {
		MutableComponent out = Component.empty();
		for (int i = 0; i < refs.size(); i++) {
			if (i > 0) {
				out.append(", ");
			}
			out.append(refs.get(i) instanceof Text.Ref ref ? name(ref) : Component.literal(String.valueOf(refs.get(i))));
		}
		return out;
	}

	/** The translated name of a named thing; its id if it has no name. */
	public static Component name(Text.Ref ref) {
		Identifier id = Identifier.tryParse(ref.id());
		String path = id == null ? ref.id() : id.getPath().replace('/', '.');
		String namespace = id == null ? "jugcraft" : id.getNamespace();
		String key = switch (ref.kind()) {
			case "component" -> "component." + namespace + "." + path;
			case "research" -> "research." + namespace + "." + path;
			case "status" -> "effect." + namespace + "." + path;
			case "item" -> "item." + namespace + "." + path;
			case "state" -> "research_state.jugcraft." + ref.id();
			case "principle" -> "principle.jugcraft." + ref.id();
			case "block" -> "block." + namespace + "." + path;
			case "preparation" -> "preparation." + namespace + "." + path;
			case "structure" -> "structure." + namespace + "." + path;
			case "graft" -> "graft." + namespace + "." + path;
			default -> Text.PREFIX + ref.kind() + "." + ref.id();
		};
		return Component.translatableWithFallback(key, ref.id());
	}
}
