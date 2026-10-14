package io.github.jimbozoomer.jugcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.jimbozoomer.jugcraft.client.compat.CodexLinks;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceCommand;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.EncyclopediaRequestPayload;
import io.github.jimbozoomer.jugcraft.guide.encyclopedia.Navigator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Jugcraft Encyclopedia on the client (Concordance Chunk A, A-04): its two ways in, the pages the server last sent
 * with the client's own Keys page after them, and the page being read (in memory only, forgotten on leaving a world).
 * <ul>
 * <li>A key (Controls: Jugcraft Encyclopedia, B unless remapped) opens it from the game, never while another screen is
 * open, so it cannot fire while typing; inside the encyclopedia the same key, or Esc, closes it.</li>
 * <li>A scroll button in the survival inventory ({@link EncyclopediaInventoryButton}) closes the inventory as Esc would
 * (the carried stack and the crafting grid go back) and opens it; Back opens a fresh inventory.</li>
 * </ul>
 * Opening asks the server again; so does Refresh. Opened from the game while looking at a block, the request names that
 * block (the server checks the reach) and the Inspect page is shown if the server sends one. Links open the codex, the
 * Spell Composer (whose Back returns here), the Concordance Journal or another page. Nothing here decides anything.
 */
public final class EncyclopediaClient {
	/** The proposed default key (A-D7): B, which vanilla and the locked pack's mods leave free (Iris has I). */
	public static final int DEFAULT_KEY = InputConstants.KEY_B;
	/** The server's Inspect page (concordance.encyclopedia.InspectPage.PAGE). */
	private static final String INSPECT = "inspect";
	private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
	private static final Navigator NAVIGATOR = new Navigator();
	private static KeyMapping open;
	private static List<EncyclopediaPayload.Page> server = List.of();
	private static List<Component> notices = List.of();
	private static int sequence;
	/** The block named in the last request opened from the game, if any. */
	private static Optional<BlockPos> looking = Optional.empty();
	/** Whether the next answer should turn to the Inspect page (the encyclopedia was opened looking at a block). */
	private static boolean inspect;
	/** Set when the player leaves a world (Fabric may say so on a network thread); the client tick then forgets it all. */
	private static volatile boolean left;

	private EncyclopediaClient() {
	}

	static void register() {
		ClientPlayNetworking.registerGlobalReceiver(EncyclopediaPayload.TYPE, (payload, context) -> received(context.client(), payload));
		// The encyclopedia belongs to the world it came from (forgotten on the client thread, at its next tick).
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> left = true);
		open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.jugcraft.encyclopedia", DEFAULT_KEY, PartyClient.CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (left) {
				left = false;
				reset();
			}
			while (open.consumeClick()) {
				// Only from the game itself: never while typing in chat, a sign, an anvil or a book.
				if (client.gui.screen() == null && client.player != null) {
					open(client, EncyclopediaScreen.Back.GAME);
				}
			}
		});
		EncyclopediaInventoryButton.register();
	}

	/** The key that opens the encyclopedia (and closes it from inside); tests press it as a player would. */
	public static KeyMapping key() {
		return open;
	}

	/** Opens the encyclopedia and asks the server for it; {@code back} says where Back and Esc lead. */
	public static void open(Minecraft client, EncyclopediaScreen.Back back) {
		looking = back.inventory() ? Optional.empty() : looked(client);
		inspect = looking.isPresent();
		request();
		local(client);
		client.gui.setScreen(new EncyclopediaScreen(back));
	}

	/** The inventory's scroll button: closes the inventory as Esc does, then opens the encyclopedia. */
	public static void openFromInventory(Minecraft client) {
		if (client.player == null) {
			return;
		}
		client.player.closeContainer();
		open(client, EncyclopediaScreen.Back.INVENTORY);
	}

	/** The latest request's number: its answer is the one the navigator takes with it ({@link Navigator#applied}). */
	public static int sequence() {
		return sequence;
	}

	/** Asks the server again for this player's pages (it answers at most twice a second). */
	public static void request() {
		sequence++;
		if (ClientPlayNetworking.canSend(EncyclopediaRequestPayload.TYPE)) {
			ClientPlayNetworking.send(new EncyclopediaRequestPayload(sequence, looking));
		}
	}

	/** Whether the server sends encyclopedia pages at all (one without Jugcraft's cannot). */
	public static boolean available() {
		return ClientPlayNetworking.canSend(EncyclopediaRequestPayload.TYPE);
	}

	/** The pages and the one being read. */
	public static Navigator navigator() {
		return NAVIGATOR;
	}

	/** What the server said about the encyclopedia as a whole (a switched-off feature, say). */
	public static List<Component> notices() {
		return notices;
	}

	/** Rebuilds the client's own pages (the Keys page changes when a key is bound), keeping the server's. */
	public static void local(Minecraft client) {
		NAVIGATOR.local(server, List.of(EncyclopediaKeys.page(client)));
	}

	/** Follows a row's link from {@code from}: the codex, the Spell Composer, the journal or another page. */
	public static void follow(Minecraft client, EncyclopediaPayload.Link link, EncyclopediaScreen from) {
		switch (link.kind()) {
			case EncyclopediaPayload.Link.CODEX -> CodexLinks.open(client, link.target());
			case EncyclopediaPayload.Link.COMPOSER -> ComposerClient.open(client, from);
			case EncyclopediaPayload.Link.JOURNAL -> JournalClient.open(client);
			case EncyclopediaPayload.Link.PAGE -> {
				if (NAVIGATOR.go(link.target())) {
					changed();
				}
			}
			default -> {
			}
		}
	}

	/** Tells the screens that the page or the pages changed. */
	public static void changed() {
		LISTENERS.forEach(Runnable::run);
	}

	/** Tells {@code listener} (on the client thread) whenever the pages or the page being read change. */
	public static void listen(Runnable listener) {
		LISTENERS.add(listener);
	}

	public static void unlisten(Runnable listener) {
		LISTENERS.remove(listener);
	}

	private static void received(Minecraft client, EncyclopediaPayload payload) {
		if (!NAVIGATOR.receive(payload.sequence(), payload.pages(), List.of(EncyclopediaKeys.page(client)), null)) {
			return;
		}
		server = payload.pages();
		notices = payload.notices();
		if (inspect && payload.sequence() == sequence) {
			inspect = false;
			NAVIGATOR.go(INSPECT);
		}
		changed();
	}

	/**
	 * The block the player is looking at, if any, as far as the server reads a circle (the crosshair's own target stops at
	 * the player's reach); the server checks the distance again.
	 */
	private static Optional<BlockPos> looked(Minecraft client) {
		Entity camera = client.getCameraEntity();
		HitResult hit = camera == null ? null : camera.pick(ConcordanceCommand.CIRCLE_READ_RANGE, 1.0F, false);
		return hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK ? Optional.of(block.getBlockPos()) : Optional.empty();
	}

	private static void reset() {
		NAVIGATOR.reset();
		server = List.of();
		notices = List.of();
		looking = Optional.empty();
		inspect = false;
	}
}
