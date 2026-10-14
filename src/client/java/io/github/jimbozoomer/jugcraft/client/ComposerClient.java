package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.concordance.Composing;
import io.github.jimbozoomer.jugcraft.concordance.compose.Draft;
import io.github.jimbozoomer.jugcraft.concordance.compose.Grammar;
import io.github.jimbozoomer.jugcraft.concordance.compose.Slot;
import io.github.jimbozoomer.jugcraft.concordance.composer.ComposerRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.composer.ComposerResultPayload;
import io.github.jimbozoomer.jugcraft.concordance.composer.ComposerStatePayload;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/**
 * The Spell Composer on the client (Chunk A, A-03): the spell being written (one {@link Draft}, shared by both views,
 * so it survives switching between them and closing the screen), what the server last said the composer may offer
 * ({@link ComposerStatePayload}) and what its last check came to ({@link ComposerResultPayload}); and the requests:
 * open, a check a moment after editing pauses, inscribe and wipe. It opens from the journal or from its own key
 * (Controls: Open the Spell Composer, unbound until a player binds it). With GuiLib installed, and the simple screens
 * not chosen in the Concordance settings, it opens as a GuiLib workspace ({@link ComposerWorkspace}); otherwise as
 * {@link ComposerScreen}. Nothing here decides anything: the server checks every word, and only Inscribe and Wipe
 * change an item. Everything is forgotten on leaving a world.
 */
public final class ComposerClient {
	/** Client ticks after the last edit before the draft is checked. */
	public static final int CHECK_DELAY = 8;
	private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
	private static final Draft DRAFT = new Draft();
	private static KeyMapping open;
	private static @Nullable ComposerStatePayload state;
	private static @Nullable ComposerResultPayload result;
	/** The last request's sequence. */
	private static int sequence;
	/** Answers to requests before this are ignored (the draft was emptied after they were sent). */
	private static int floor;
	/** The check on its way, or -1. */
	private static int checking = -1;
	/** The inscribe or wipe on its way, or -1. */
	private static int writing = -1;
	/** Ticks until the draft is checked; -1 when no check is due. */
	private static int due = -1;
	/** Whether the next state should load what is inscribed into an empty draft (set when the composer opens). */
	private static boolean loading;
	/** Set when the player leaves a world (Fabric may say so on a network thread); the client tick then forgets it all. */
	private static volatile boolean left;

	private ComposerClient() {
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ComposerStatePayload.TYPE, (payload, context) -> received(payload));
		ClientPlayNetworking.registerGlobalReceiver(ComposerResultPayload.TYPE, (payload, context) -> answered(payload));
		// A composer belongs to the world it came from (forgotten on the client thread, at its next tick).
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> left = true);
		open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.concordance_composer", InputConstants.UNKNOWN.getValue(),
				PartyClient.CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (left) {
				left = false;
				reset();
			}
			while (open.consumeClick()) {
				if (client.gui.screen() == null && client.player != null) {
					open(client, null);
				}
			}
			tick();
		});
	}

	/** The key that opens the composer (unbound unless the player binds one). */
	public static KeyMapping key() {
		return open;
	}

	/** Opens the composer and asks the server for its state; {@code parent} is the screen Back returns to, if any. */
	public static void open(Minecraft client, @Nullable Screen parent) {
		loading = true;
		request(ComposerRequestPayload.OPEN, 0, "");
		if (!ConcordanceClientOptions.simpleJournal() && FabricLoader.getInstance().isModLoaded("guilib")) {
			ComposerWorkspace.open(parent);
		} else {
			client.gui.setScreen(new ComposerScreen(parent));
		}
	}

	/** Whether this server composes spells at all (one without the Concordance cannot). */
	public static boolean available() {
		return ClientPlayNetworking.canSend(ComposerRequestPayload.TYPE);
	}

	public static Draft draft() {
		return DRAFT;
	}

	public static @Nullable ComposerStatePayload state() {
		return state;
	}

	public static @Nullable ComposerResultPayload result() {
		return result;
	}

	/** Whether the server's state says a spell can be composed now (an instrument in hand, the Concordance on). */
	public static boolean ready() {
		return state != null && state.status() == ComposerStatePayload.READY;
	}

	/** Whether a check is due or on its way (the last result may be for an earlier draft). */
	public static boolean checking() {
		return due >= 0 || checking >= 0;
	}

	/** Whether an inscribe or wipe is on its way. */
	public static boolean writing() {
		return writing >= 0;
	}

	/**
	 * Whether the draft, as it stands, was checked and found valid against what the screen shows now (the same
	 * instrument and rules), so it may be inscribed.
	 */
	public static boolean inscribable() {
		return ready() && !checking() && !writing() && result != null && result.what() == Composing.Outcome.VALID
				&& result.text().equals(DRAFT.text()) && result.revision() == state.revision();
	}

	/** Whether there is an inscription to wipe. */
	public static boolean wipeable() {
		return ready() && !writing() && !state.inscribed().isEmpty();
	}

	/** A word this player has learned, by full id; null for one they have not (or the server did not send). */
	public static ComposerStatePayload.@Nullable Word word(String id) {
		if (state != null) {
			for (ComposerStatePayload.Word word : state.words()) {
				if (word.id().equals(id)) {
					return word;
				}
			}
		}
		return null;
	}

	/** The slot of a learned word, by full id; null for a word not learned. */
	public static @Nullable Slot slotOf(String id) {
		ComposerStatePayload.Word word = word(id);
		return word == null ? null : slot(word);
	}

	/** A word's slot; null for a number this game does not know. */
	public static @Nullable Slot slot(ComposerStatePayload.Word word) {
		Slot[] slots = Slot.values();
		return word.slot() >= 0 && word.slot() < slots.length ? slots[word.slot()] : null;
	}

	/** The capacity the draft's learned words take (the server's check gives the exact figure). */
	public static int capacity() {
		int used = 0;
		for (List<Draft.Part> parts : DRAFT.levels()) {
			for (Draft.Part part : parts) {
				used += cost(part.id());
				for (String modifier : part.modifiers()) {
					used += cost(modifier);
				}
			}
		}
		return used;
	}

	private static int cost(String id) {
		ComposerStatePayload.Word word = word(id);
		return word == null ? 0 : word.capacity();
	}

	/** The views call this after every edit of the draft: it is checked a moment after editing pauses. */
	public static void edited() {
		if (DRAFT.isEmpty()) {
			// Nothing to check: the views say how to begin instead.
			due = -1;
			checking = -1;
			result = null;
			floor = sequence + 1;
		} else {
			// A server without the composer cannot check: no check is promised that would never come.
			due = available() ? CHECK_DELAY : -1;
		}
		changed();
	}

	/** Inscribes the checked draft on the instrument the server last showed. */
	public static void inscribe() {
		if (inscribable()) {
			checking = -1;
			writing = request(ComposerRequestPayload.INSCRIBE, state.revision(), DRAFT.text());
			changed();
		}
	}

	/** Wipes the inscription on the instrument the server last showed. */
	public static void wipe() {
		if (wipeable()) {
			checking = -1;
			writing = request(ComposerRequestPayload.CLEAR, state.revision(), "");
			changed();
		}
	}

	/** Writes what is inscribed into the draft (undoable). */
	public static void load() {
		if (state != null && !state.inscribed().isEmpty()) {
			DRAFT.read(state.inscribed(), true);
			edited();
		}
	}

	/** Tells {@code listener} (on the client thread) whenever the state, an answer or the draft changes. */
	public static void listen(Runnable listener) {
		LISTENERS.add(listener);
	}

	public static void unlisten(Runnable listener) {
		LISTENERS.remove(listener);
	}

	/** Runs the listeners (the views call this after changing which part is chosen). */
	public static void changed() {
		LISTENERS.forEach(Runnable::run);
	}

	private static int request(int action, int revision, String text) {
		sequence++;
		if (available()) {
			ClientPlayNetworking.send(new ComposerRequestPayload(sequence, action, revision, text));
		}
		return sequence;
	}

	private static void tick() {
		if (due < 0) {
			return;
		}
		if (due > 0) {
			due--;
			return;
		}
		if (writing >= 0) {
			return; // checked once the write is answered
		}
		due = -1;
		// Words added with buttons have no length bound, and the request carries at most MAX_TEXT characters; past the
		// grammar's own limit the server answers "too long" all the same.
		String text = DRAFT.text();
		checking = request(ComposerRequestPayload.CHECK, 0, text.length() > Grammar.MAX_TEXT ? text.substring(0, Grammar.MAX_TEXT + 1) : text);
	}

	private static void received(ComposerStatePayload payload) {
		state = payload;
		if (loading) {
			loading = false;
			if (DRAFT.isEmpty() && !payload.inscribed().isEmpty()) {
				// A new composer starts from what the instrument already holds, and explains it.
				DRAFT.read(payload.inscribed(), false);
			}
			if (!DRAFT.isEmpty()) {
				// Checked again against the instrument and rules this composer shows now (they may not be the last ones).
				due = CHECK_DELAY;
			}
		}
		changed();
	}

	private static void answered(ComposerResultPayload payload) {
		boolean wrote = payload.sequence() == writing;
		if (payload.sequence() == checking) {
			checking = -1;
		}
		if (wrote) {
			writing = -1;
			if (payload.what() != Composing.Outcome.INSCRIBED && !DRAFT.isEmpty()) {
				// After a wipe or a refused write the unchanged draft is checked again (a wipe may have replaced a check).
				due = CHECK_DELAY;
			}
		}
		if (payload.sequence() < floor || (result != null && payload.sequence() < result.sequence())) {
			changed();
			return;
		}
		boolean news = wrote || result == null || !result.heading().getString().equals(payload.heading().getString());
		result = payload;
		if (news) {
			// The narrator, when on, reads what a write or a changed verdict came to, in either view.
			Minecraft.getInstance().getNarrator().saySystemNow(payload.heading());
		}
		changed();
	}

	private static void reset() {
		state = null;
		result = null;
		sequence = 0;
		floor = 0;
		checking = -1;
		writing = -1;
		due = -1;
		loading = false;
		DRAFT.reset();
		changed();
	}
}
