package io.github.jimbozoomer.jugcraft.guide.encyclopedia;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.encyclopedia.FirstLightRoute;
import io.github.jimbozoomer.jugcraft.concordance.encyclopedia.InspectPage;
import io.github.jimbozoomer.jugcraft.concordance.encyclopedia.SpellcraftPage;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.LairDossiers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Jugcraft Encyclopedia's server side (Concordance Chunk A, A-04): it builds a player's pages when they ask (opening
 * the encyclopedia, or asking again), from what the server knows about that player and nothing else. Pages come in a
 * fixed order (Discover, Spellcraft, Expeditions, Inspect; the client adds its own Keys page last); a page that has
 * nothing to show for this player is left out, so there is never an empty tab, and one that fails says so and the log
 * says why, without hiding the others.
 * <p>
 * A player may ask at most once every {@value #REQUEST_TICKS} ticks; one who asks sooner is answered at the first
 * allowed tick (only their newest request waits). The request names no page and claims nothing, so a modified client
 * can only ask for its own encyclopedia, and not often. Building reads the rules, the player's own records and
 * inventory, the server's recipes and at most one block entity the player stands near; it writes nothing.
 */
public final class Encyclopedia {
	/** How often a player may ask for their encyclopedia (ticks). */
	public static final int REQUEST_TICKS = 10;
	public static final String RATE_ACTION = "encyclopedia";
	private static final List<Contributor> PAGES = new ArrayList<>();
	/** Requests that came too soon, newest only, by player. */
	private static final Map<UUID, EncyclopediaRequestPayload> WAITING = new HashMap<>();

	/** Writes one page for one player's request. */
	@FunctionalInterface
	public interface Writer {
		void write(ServerPlayer player, EncyclopediaRequestPayload request, PageWriter out);
	}

	private record Contributor(String id, Writer writer) {
	}

	private Encyclopedia() {
	}

	public static void register() {
		// The pages, in the order they are shown (tools/check_mod_data.py pins the ids to tools/encyclopedia.py).
		page("discover", FirstLightRoute::write);
		page("spellcraft", SpellcraftPage::write);
		page("expedition", LairDossiers::write);
		page("inspect", InspectPage::write);
		PayloadTypeRegistry.serverboundPlay().register(EncyclopediaRequestPayload.TYPE, EncyclopediaRequestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EncyclopediaPayload.TYPE, EncyclopediaPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(EncyclopediaRequestPayload.TYPE, (payload, context) -> receive(context.player(), payload));
		ServerTickEvents.END_SERVER_TICK.register(Encyclopedia::tick);
		// On the server thread, as the player leaves (not on the network thread that reports a disconnect).
		ServerPlayerEvents.LEAVE.register(player -> WAITING.remove(player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> WAITING.clear());
	}

	/** Adds a page, in the order pages are shown (each feature at its registration). */
	public static void page(String id, Writer writer) {
		PAGES.add(new Contributor(id, writer));
	}

	/** The page ids, in order (the audit and tests read them). */
	public static List<String> pageIds() {
		return PAGES.stream().map(Contributor::id).toList();
	}

	/** A request from {@code player} (the receiver calls this, and so do tests). */
	public static void receive(ServerPlayer player, EncyclopediaRequestPayload request) {
		WAITING.remove(player.getUUID());
		if (!answer(player, request)) {
			WAITING.put(player.getUUID(), request);
		}
	}

	/** Whether a request from {@code player} is waiting for its allowed tick. */
	public static boolean waiting(ServerPlayer player) {
		return WAITING.containsKey(player.getUUID());
	}

	private static boolean answer(ServerPlayer player, EncyclopediaRequestPayload request) {
		if (!RateGate.allow(player, RATE_ACTION, REQUEST_TICKS)) {
			return false;
		}
		EncyclopediaPayload payload = build(player, request, PageWriter::of);
		// A test's stand-in player has no game to send to.
		if (ServerPlayNetworking.canSend(player, EncyclopediaPayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
		return true;
	}

	private static void tick(MinecraftServer server) {
		if (WAITING.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, EncyclopediaRequestPayload>> waiting = WAITING.entrySet().iterator();
		while (waiting.hasNext()) {
			Map.Entry<UUID, EncyclopediaRequestPayload> request = waiting.next();
			ServerPlayer player = server.getPlayerList().getPlayer(request.getKey());
			if (player == null || answer(player, request.getValue())) {
				waiting.remove();
			}
		}
	}

	/**
	 * The encyclopedia as {@code player} sees it now, answering {@code request}; {@code writers} makes each page's writer
	 * (game tests pass one whose server lacks some recipes).
	 */
	public static EncyclopediaPayload build(ServerPlayer player, EncyclopediaRequestPayload request, Function<ServerPlayer, PageWriter> writers) {
		List<Component> notices = new ArrayList<>();
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			notices.add(Component.translatable("message.jugcraft.concordance.disabled"));
		}
		List<EncyclopediaPayload.Page> pages = new ArrayList<>();
		for (Contributor page : PAGES) {
			if (pages.size() >= EncyclopediaPayload.MAX_PAGES) {
				break;
			}
			PageWriter out = writers.apply(player);
			try {
				page.writer().write(player, request, out);
			} catch (RuntimeException failure) {
				// One page's fault must not hide the others: the page says it could not be read, the log says why.
				Jugcraft.LOGGER.warn("Encyclopedia: the {} page failed for {}", page.id(), player.getName().getString(), failure);
				out = writers.apply(player);
				out.fact(Component.translatable("encyclopedia.jugcraft.unreadable"));
			}
			if (out.hasContent()) {
				pages.add(new EncyclopediaPayload.Page(page.id(), title(page.id()), out.rows()));
			}
		}
		return new EncyclopediaPayload(request.sequence(), notices, pages);
	}

	public static Component title(String page) {
		return Component.translatable("encyclopedia.jugcraft.page." + page);
	}
}
