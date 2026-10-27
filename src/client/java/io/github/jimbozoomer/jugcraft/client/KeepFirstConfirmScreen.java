package io.github.jimbozoomer.jugcraft.client;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

/**
 * Concordance Chunk D (D-06, D-07): vanilla's confirmation whose keyboard starts on its "no", so Enter on arrival keeps what
 * it asks about (Esc keeps it too, ConfirmScreen's own rule). The pact screen's End and the roster's End ask through it.
 */
final class KeepFirstConfirmScreen extends ConfirmScreen {
	KeepFirstConfirmScreen(BooleanConsumer callback, Component title, Component message, Component yes, Component no) {
		super(callback, title, message, yes, no);
	}

	@Override
	protected void setInitialFocus() {
		// Vanilla's own initial focus acts only for keyboard input too (Screen.setInitialFocus); a mouse user starts unfocused.
		if (noButton != null && minecraft.getLastInputType().isKeyboard()) {
			setInitialFocus(noButton);
		} else {
			super.setInitialFocus();
		}
	}
}
