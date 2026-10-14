package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

/**
 * The encyclopedia's Keys page (Concordance Chunk A, A-04), which the client writes for itself (only it knows the
 * bindings): every Jugcraft key with what it is bound to now, what it does, and any other key bound the same (a debug
 * combination with F3 is not counted: it does not press the key alone); then the vanilla keys the first route uses;
 * then where to change them. Made again each time the encyclopedia opens, so a key just bound shows.
 */
final class EncyclopediaKeys {
	static final String PAGE = "keys";
	private static final String PREFIX = "key.jugcraft.";

	private EncyclopediaKeys() {
	}

	static EncyclopediaPayload.Page page(Minecraft client) {
		List<EncyclopediaPayload.Row> rows = new ArrayList<>();
		rows.add(row(EncyclopediaPayload.Kind.HEADING, Component.translatable("encyclopedia.jugcraft.keys.jugcraft"), null));
		for (KeyMapping mapping : client.options.keyMappings) {
			if (!mapping.getCategory().equals(PartyClient.CATEGORY)) {
				continue;
			}
			Component bound = mapping.isUnbound() ? Component.translatable("encyclopedia.jugcraft.keys.unbound") : mapping.getTranslatedKeyMessage();
			String help = mapping.getName().startsWith(PREFIX) ? "encyclopedia.jugcraft.key." + mapping.getName().substring(PREFIX.length()) : mapping.getName();
			MutableComponent line = Component.translatable("encyclopedia.jugcraft.keys.line", Component.translatable(mapping.getName()), bound,
					Component.translatableWithFallback(help, ""));
			Component shared = shared(client, mapping);
			if (shared != null) {
				// A clash is said in the line itself, not hidden with the exact figures.
				line.append(" ").append(shared);
			}
			rows.add(row(EncyclopediaPayload.Kind.FACT, line, null));
		}
		rows.add(row(EncyclopediaPayload.Kind.HEADING, Component.translatable("encyclopedia.jugcraft.keys.vanilla"), null));
		rows.add(row(EncyclopediaPayload.Kind.FACT, Component.translatable("encyclopedia.jugcraft.keys.examine", client.options.keyShift.getTranslatedKeyMessage(),
				client.options.keyUse.getTranslatedKeyMessage()), null));
		rows.add(row(EncyclopediaPayload.Kind.FACT, Component.translatable("encyclopedia.jugcraft.keys.cast", client.options.keyUse.getTranslatedKeyMessage(),
				client.options.keyHotbarSlots[1].getTranslatedKeyMessage(), client.options.keyHotbarSlots[2].getTranslatedKeyMessage()), null));
		rows.add(row(EncyclopediaPayload.Kind.FACT, Component.translatable("encyclopedia.jugcraft.keys.inventory", client.options.keyInventory.getTranslatedKeyMessage()), null));
		rows.add(row(EncyclopediaPayload.Kind.FACT, Component.translatable("encyclopedia.jugcraft.keys.change"), null));
		return new EncyclopediaPayload.Page(PAGE, Component.translatable("encyclopedia.jugcraft.page.keys"), rows);
	}

	/** The other keys bound the same as {@code mapping} (debug combinations with F3 aside), or null when there are none. */
	private static @Nullable Component shared(Minecraft client, KeyMapping mapping) {
		if (mapping.isUnbound()) {
			return null;
		}
		MutableComponent others = Component.empty();
		int count = 0;
		for (KeyMapping other : client.options.keyMappings) {
			if (other == mapping || other.isUnbound() || !other.same(mapping) || other.getCategory() == KeyMapping.Category.DEBUG) {
				continue;
			}
			if (count++ > 0) {
				others.append(", ");
			}
			others.append(Component.translatable(other.getName()));
		}
		return count == 0 ? null : Component.translatable("encyclopedia.jugcraft.keys.shared", others);
	}

	private static EncyclopediaPayload.Row row(EncyclopediaPayload.Kind kind, Component text, @Nullable Component exact) {
		return new EncyclopediaPayload.Row(kind.ordinal(), 0, 0, text, Optional.ofNullable(exact), List.of(), Optional.empty(), Optional.empty());
	}
}
