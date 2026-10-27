package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterStatePayload;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-07 (D-S11): the client's side of the roster ({@link RosterScreen}). Its key (not bound at first,
 * listed on the encyclopedia's Keys page) opens the screen from the game only. It keeps the last roster the server sent
 * and sends the screen's requests: a rising sequence, an action, a row the server listed and that row's revision; never
 * text, a player, an item or a total; the server checks each again. It decides nothing. While the screen is open it asks
 * again every {@value #REFRESH_TICKS} client ticks; while an action waits for its answer the screen offers no other, for at
 * most {@value #PATIENCE_TICKS}. Each answer is read aloud. Everything is forgotten on leaving a world.
 */
public final class RosterClient {
	/** Client ticks an action may wait for its answer before the screen's controls come back (one the server dropped). */
	public static final int PATIENCE_TICKS = 100;
	/** Client ticks between the open screen's own requests for the roster again. */
	public static final int REFRESH_TICKS = 40;
	private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
	private static KeyMapping open;
	private static @Nullable RosterStatePayload state;
	/** The answer to the last action, kept until the next is sent or the screen opens again. */
	private static PactStatePayload.@Nullable Result result;
	private static int sequence;
	private static int pending = -1;
	private static int waited;
	private static int heard;
	private static int quiet;
	private static volatile boolean left;

	private RosterClient() {
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(RosterStatePayload.TYPE, (payload, context) -> received(payload));
		// A roster belongs to the world it came from (forgotten on the client thread, at its next tick).
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> left = true);
		open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.concordance_roster", InputConstants.UNKNOWN.getValue(), PartyClient.CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (left) {
				left = false;
				reset();
			}
			while (open.consumeClick()) {
				if (client.gui.screen() == null && client.player != null) {
					open(client);
				}
			}
			tick(client);
		});
	}

	/** The key that opens the roster (unbound unless the player binds one); tests bind and press it. */
	public static KeyMapping key() {
		return open;
	}

	/** Opens the roster and asks the server for it. */
	public static void open(Minecraft client) {
		result = null;
		client.gui.setScreen(new RosterScreen());
		refresh();
	}

	/** Whether this server keeps a roster at all (one without the Concordance cannot). */
	public static boolean available() {
		return ClientPlayNetworking.canSend(RosterRequestPayload.TYPE);
	}

	public static @Nullable RosterStatePayload state() {
		return state;
	}

	public static PactStatePayload.@Nullable Result result() {
		return result;
	}

	public static boolean waiting() {
		return pending >= 0;
	}

	/** The sequence of the last answer heard (tests wait for it to rise). */
	public static int lastAnswer() {
		return heard;
	}

	/** Asks for the roster again (the screen's Refresh, its opening, and every REFRESH_TICKS while it is open); not while an action waits. */
	public static void refresh() {
		quiet = 0;
		if (pending < 0 && available()) {
			sequence++;
			ClientPlayNetworking.send(new RosterRequestPayload(sequence, RosterRequestPayload.OPEN, RosterRequestPayload.NO_ROW, 0));
		}
	}

	/**
	 * Sends {@code action} on the row {@code row} at the {@code revision} the player chose it at; returns its sequence, or -1
	 * if nothing was sent (no roster yet, another action waiting, or a server without one).
	 */
	public static int request(int action, UUID row, int revision) {
		RosterStatePayload shown = state;
		if (shown == null || action <= RosterRequestPayload.OPEN || action >= RosterRequestPayload.ACTIONS.size() || pending >= 0 || !available()) {
			return -1;
		}
		sequence++;
		ClientPlayNetworking.send(new RosterRequestPayload(sequence, action, row, revision));
		pending = sequence;
		waited = 0;
		result = null;
		changed();
		return sequence;
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

	private static void tick(Minecraft client) {
		if (pending >= 0 && ++waited >= PATIENCE_TICKS) {
			// An answer the server never sent: the screen's controls come back.
			pending = -1;
			changed();
		}
		if (client.gui.screen() instanceof RosterScreen && ++quiet >= REFRESH_TICKS) {
			refresh();
		}
	}

	private static void received(RosterStatePayload payload) {
		RosterStatePayload before = state;
		state = payload;
		boolean wasPending = pending >= 0;
		boolean heardNow = false;
		if (payload.result().isPresent()) {
			PactStatePayload.Result answer = payload.result().get();
			if (pending >= 0 && answer.sequence() >= pending) {
				pending = -1;
			}
			if (answer.sequence() > heard) {
				heard = answer.sequence();
				result = answer;
				heardNow = true;
				Minecraft.getInstance().getNarrator().saySystemNow(answer.message());
			}
		}
		// A refresh that changes nothing rebuilds nothing.
		if (heardNow || wasPending && pending < 0 || before == null || before.enabled() != payload.enabled() || before.more() != payload.more()
				|| !before.rows().equals(payload.rows())) {
			changed();
		}
	}

	private static void reset() {
		state = null;
		result = null;
		sequence = 0;
		pending = -1;
		waited = 0;
		heard = 0;
		quiet = 0;
		RosterScreen.forget();
		changed();
	}
}
