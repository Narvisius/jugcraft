package io.github.jimbozoomer.jugcraft.client.compat;

import com.klikli_dev.modonomicon.client.gui.BookGuiManager;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Opens an entry of the Arcane Concordance codex (Modonomicon, a required dependency) from an encyclopedia link
 * (Concordance Chunk A, A-04): the encyclopedia's only call into Modonomicon. The server offers a link only once the
 * player's research has opened the entry, so the codex item need not be carried (A-D12). If the codex cannot open it
 * (the book failed to load, the entry is gone), the player is told so and the log says why, once.
 */
public final class CodexLinks {
	public static final Identifier BOOK = Jugcraft.id("arcane_concordance");
	private static boolean warned;

	private CodexLinks() {
	}

	/** Opens the codex at {@code entry} (its id within the book, such as {@code composition/composing}); false if it could not. */
	public static boolean open(Minecraft client, String entry) {
		Identifier id = Identifier.tryParse(Jugcraft.MOD_ID + ":" + entry);
		try {
			if (id == null) {
				throw new IllegalArgumentException("not an entry id: " + entry);
			}
			BookGuiManager.get().openEntry(BOOK, id, 0);
			return true;
		} catch (RuntimeException | LinkageError failure) {
			if (!warned) {
				warned = true;
				Jugcraft.LOGGER.warn("Encyclopedia: the codex could not open {}", entry, failure);
			}
			if (client.player != null) {
				client.player.sendOverlayMessage(Component.translatable("encyclopedia.jugcraft.codex_unavailable"));
			}
			return false;
		}
	}
}
