package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.concordance.journal.JournalPayload;
import io.github.jimbozoomer.jugcraft.concordance.journal.JournalRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.journal.JournalSection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * The Concordance Journal on the client (roadmap step 26): the key that opens it (J, remappable in Controls), the last
 * journal the server sent and who is showing it. With GuiLib installed (and the simple journal not chosen) the journal
 * opens as a GuiLib workspace ({@link JournalWorkspace}); otherwise as {@link JournalScreen}. Both only show what the
 * server sent; asking again is all either can do.
 */
public final class JournalClient {
	private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
	private static KeyMapping open;
	private static List<JournalSection> sections = List.of();
	private static boolean received;

	private JournalClient() {
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(JournalPayload.TYPE, (payload, context) -> {
			sections = payload.sections();
			received = true;
			LISTENERS.forEach(Runnable::run);
		});
		// A journal belongs to the world it came from.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			sections = List.of();
			received = false;
		});
		open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.concordance_journal", InputConstants.KEY_J, PartyClient.CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (open.consumeClick()) {
				if (client.gui.screen() == null && client.player != null) {
					open(client);
				}
			}
		});
	}

	/** The key that opens the journal (J unless remapped); tests press it as a player would. */
	public static KeyMapping key() {
		return open;
	}

	/** Opens the journal (and asks the server for it again). */
	public static void open(Minecraft client) {
		request();
		if (!ConcordanceClientOptions.simpleJournal() && FabricLoader.getInstance().isModLoaded("guilib")) {
			JournalWorkspace.open();
		} else {
			client.gui.setScreen(new JournalScreen());
		}
	}

	/** Asks the server for this player's journal (it answers at most twice a second). */
	public static void request() {
		if (ClientPlayNetworking.canSend(JournalRequestPayload.TYPE)) {
			ClientPlayNetworking.send(JournalRequestPayload.INSTANCE);
		}
	}

	/** Whether the server can send a journal at all (a server without the Concordance cannot). */
	public static boolean available() {
		return ClientPlayNetworking.canSend(JournalRequestPayload.TYPE);
	}

	public static List<JournalSection> sections() {
		return sections;
	}

	public static boolean received() {
		return received;
	}

	/** Tells {@code listener} (on the client thread) whenever a new journal arrives. */
	public static void listen(Runnable listener) {
		LISTENERS.add(listener);
	}

	public static void unlisten(Runnable listener) {
		LISTENERS.remove(listener);
	}
}
