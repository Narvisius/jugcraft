package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.concordance.spirits.PactMenu;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D (D-04, D-05; D-S14): the client's side of a pact perch's menu, for {@link PactScreen}. It keeps the
 * last state the server sent the open pact menu and sends the screen's requests: the menu's container id, a rising
 * sequence, an action, an index into a list the server sent, and the terms revision the screen was shown. It never sends
 * text, a player id or a total; the server checks each request again (concordance/spirits/PactMenu). It decides nothing.
 * A state for a menu that is no longer open is dropped. While a request waits for its answer the screen offers no other,
 * for at most {@value #PATIENCE_TICKS} client ticks. Each answer is read aloud, and an answer that his terms changed sends
 * the screen back to them. Everything is forgotten on leaving a world.
 */
public final class PactClient {
	/** Client ticks a request may wait for its answer before the screen's controls come back (one the server dropped). */
	public static final int PATIENCE_TICKS = 100;
	private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
	private static @Nullable PactStatePayload state;
	private static int sequence;
	private static int pending = -1;
	private static int waited;
	private static int heard;
	private static boolean changedTerms;
	private static volatile boolean left;

	private PactClient() {
	}

	static void register() {
		MenuScreens.register(Workers.PERCH_MENU, PactScreen::new);
		ClientPlayNetworking.registerGlobalReceiver(PactStatePayload.TYPE, (payload, context) -> received(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> left = true);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (left) {
				left = false;
				reset();
			}
			if (pending >= 0 && ++waited >= PATIENCE_TICKS) {
				pending = -1;
				changed();
			}
		});
	}

	/** Whether this server keeps pacts at all (one without the Concordance cannot). */
	public static boolean available() {
		return ClientPlayNetworking.canSend(PactRequestPayload.TYPE);
	}

	/** The last state the server sent {@code menu}, or null before its first. */
	public static @Nullable PactStatePayload state(PactMenu menu) {
		PactStatePayload shown = state;
		return shown != null && shown.containerId() == menu.containerId ? shown : null;
	}

	/** The last state of the pact menu the player has open, or null (tests read it). */
	public static @Nullable PactStatePayload state() {
		Minecraft client = Minecraft.getInstance();
		return client.player != null && client.player.containerMenu instanceof PactMenu menu ? state(menu) : null;
	}

	public static boolean waiting() {
		return pending >= 0;
	}

	/** The sequence of the last answer heard (tests wait for it to rise). */
	public static int lastAnswer() {
		return heard;
	}

	/**
	 * Sends {@code action} (with {@code choice}, an index into a list the last state sent) for the open {@code menu}, naming
	 * the revision the screen shows; returns its sequence, or -1 if nothing was sent (no state yet, another request waiting,
	 * or a server that keeps no pacts). CHOOSE never waits for an answer.
	 */
	public static int request(PactMenu menu, int action, int choice) {
		PactStatePayload shown = state(menu);
		if (shown == null || action != PactRequestPayload.CHOOSE && pending >= 0 || !available()) {
			return -1;
		}
		sequence++;
		ClientPlayNetworking.send(new PactRequestPayload(menu.containerId, sequence, action, choice, shown.revision()));
		if (action != PactRequestPayload.CHOOSE) {
			pending = sequence;
			waited = 0;
		}
		changed();
		return sequence;
	}

	/** Whether an answer said his terms changed (the screen shows them again); reading it clears it. */
	static boolean takeChanged() {
		boolean was = changedTerms;
		changedTerms = false;
		return was;
	}

	static void listen(Runnable listener) {
		LISTENERS.add(listener);
	}

	static void unlisten(Runnable listener) {
		LISTENERS.remove(listener);
	}

	private static void changed() {
		LISTENERS.forEach(Runnable::run);
	}

	private static void received(PactStatePayload payload) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null || !(client.player.containerMenu instanceof PactMenu menu) || menu.containerId != payload.containerId()) {
			return;
		}
		if (state == null || state.containerId() != payload.containerId()) {
			pending = -1;
		}
		state = payload;
		payload.result().ifPresent(result -> {
			if (pending >= 0 && result.sequence() >= pending) {
				pending = -1;
			}
			if (result.sequence() > heard) {
				heard = result.sequence();
				changedTerms |= PactMenu.CHANGED.equals(result.reason());
				client.getNarrator().saySystemNow(result.message());
			}
		});
		changed();
	}

	private static void reset() {
		state = null;
		sequence = 0;
		pending = -1;
		waited = 0;
		heard = 0;
		changedTerms = false;
		changed();
	}
}
