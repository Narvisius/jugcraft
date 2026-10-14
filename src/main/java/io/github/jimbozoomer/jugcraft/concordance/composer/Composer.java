package io.github.jimbozoomer.jugcraft.concordance.composer;

import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.ComposedSpells;
import io.github.jimbozoomer.jugcraft.concordance.Composing;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Inscription;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.compose.Catalog;
import io.github.jimbozoomer.jugcraft.concordance.compose.Compiler;
import io.github.jimbozoomer.jugcraft.concordance.compose.Component;
import io.github.jimbozoomer.jugcraft.concordance.compose.Composition;
import io.github.jimbozoomer.jugcraft.concordance.compose.Grammar;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The composer screen's server side (Chunk A, A-02). A screen opens a session ({@link ComposerRequestPayload#OPEN}) and
 * is sent what it may show ({@link ComposerStatePayload}): the instrument in hand, the words this player has learned
 * and what is inscribed. It then asks for checks while its player edits, and to inscribe or clear; each is answered by
 * {@link Composing}, the service the {@code /jugcraft concordance compose} command uses, so screen and command always
 * agree ({@link ComposerResultPayload}).
 * <ul>
 * <li><b>Sessions.</b> One per player, in memory: a revision and what the screen was last shown ({@link Composing.Held}).
 * An inscribe or clear names the revision it acted on; if that is not the latest, or the instrument or rules have
 * changed since, nothing is written and the screen is sent the state again. Whenever the server's side changes (a write,
 * another instrument, new rules) the screen is told. No identifier is added to any item. Sessions are forgotten when the
 * player leaves and when the server stops; they are not saved.</li>
 * <li><b>Rate.</b> Checks, inscribes and clears (one naming an older revision too) count against the gate the command
 * and tuning use ({@value Composing#COMPOSE_RATE_TICKS} ticks). One that comes too soon waits for the first allowed tick, and a newer
 * one replaces it, so a screen that checks as its player types never makes more work than one compilation a second,
 * and its last request is the one answered. Opening counts against its own gate ({@value #OPEN_TICKS} ticks), the same
 * way.</li>
 * <li><b>Bounds.</b> The request's text decodes to a bounded length and is compiled as a command's is; the answer sends
 * each distinct line once, at most {@value ComposerResultPayload#MAX_LINES}, and never an exception.</li>
 * </ul>
 */
public final class Composer {
	/** How often a screen may open or ask for its state again (ticks). */
	public static final int OPEN_TICKS = 10;
	private static final String OPEN_ACTION = "composer_open";
	/** Problems whose argument is a word as written, which a screen can mark. */
	private static final Set<String> WRITTEN = Set.of("problem.unknown", "problem.bad_name");
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	/** Checks, inscribes and clears that came too soon, newest only, by player. */
	private static final Map<UUID, ComposerRequestPayload> WAITING = new HashMap<>();
	/** Players whose screen asked to open too soon. */
	private static final Set<UUID> OPENING = new HashSet<>();

	private static final class Session {
		private int revision;
		private Composing.Held held;
	}

	private Composer() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(ComposerRequestPayload.TYPE, ComposerRequestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ComposerStatePayload.TYPE, ComposerStatePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ComposerResultPayload.TYPE, ComposerResultPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ComposerRequestPayload.TYPE, (payload, context) -> receive(context.player(), payload));
		ServerTickEvents.END_SERVER_TICK.register(Composer::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			// Fabric may report a leaving player on a network thread; the sessions belong to the server thread.
			UUID id = handler.getPlayer().getUUID();
			server.execute(() -> forget(id));
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SESSIONS.clear();
			WAITING.clear();
			OPENING.clear();
		});
	}

	/** A screen's request from {@code player} (the receiver calls this, and so do tests). Unknown actions are ignored. */
	public static void receive(ServerPlayer player, ComposerRequestPayload request) {
		UUID id = player.getUUID();
		switch (request.action()) {
			case ComposerRequestPayload.OPEN -> {
				OPENING.remove(id);
				if (!open(player)) {
					OPENING.add(id);
				}
			}
			case ComposerRequestPayload.CHECK, ComposerRequestPayload.INSCRIBE, ComposerRequestPayload.CLEAR -> {
				WAITING.remove(id);
				if (!answer(player, request)) {
					WAITING.put(id, request);
				}
			}
			default -> {
			}
		}
	}

	/** Whether a request from {@code player} is waiting for its allowed tick. */
	public static boolean waiting(ServerPlayer player) {
		return WAITING.containsKey(player.getUUID()) || OPENING.contains(player.getUUID());
	}

	/** The player's composer revision: 0 before their first screen opened. */
	public static int revision(ServerPlayer player) {
		Session session = SESSIONS.get(player.getUUID());
		return session == null ? 0 : session.revision;
	}

	private static void forget(UUID player) {
		SESSIONS.remove(player);
		WAITING.remove(player);
		OPENING.remove(player);
	}

	private static void tick(MinecraftServer server) {
		if (OPENING.isEmpty() && WAITING.isEmpty()) {
			return;
		}
		Iterator<UUID> opening = OPENING.iterator();
		while (opening.hasNext()) {
			ServerPlayer player = server.getPlayerList().getPlayer(opening.next());
			if (player == null || open(player)) {
				opening.remove();
			}
		}
		Iterator<Map.Entry<UUID, ComposerRequestPayload>> waiting = WAITING.entrySet().iterator();
		while (waiting.hasNext()) {
			Map.Entry<UUID, ComposerRequestPayload> request = waiting.next();
			ServerPlayer player = server.getPlayerList().getPlayer(request.getKey());
			if (player == null || answer(player, request.getValue())) {
				waiting.remove();
			}
		}
	}

	/** Opens (or refreshes) the player's session; false if it must wait for its gate. */
	private static boolean open(ServerPlayer player) {
		if (!RateGate.allow(player, OPEN_ACTION, OPEN_TICKS)) {
			return false;
		}
		refresh(player, SESSIONS.computeIfAbsent(player.getUUID(), unused -> new Session()));
		return true;
	}

	/** Takes a new look at the instrument in hand and the rules, and sends the screen its new state. */
	private static void refresh(ServerPlayer player, Session session) {
		session.revision++;
		session.held = Composing.Held.of(player);
		send(player, state(player, session.revision));
	}

	/** Answers a check, inscribe or clear; false if it must wait for the rate. */
	private static boolean answer(ServerPlayer player, ComposerRequestPayload request) {
		Session session = SESSIONS.get(player.getUUID());
		if (session == null) {
			// A screen this server has no session for (it never opened here, or the server restarted under it).
			if (!open(player)) {
				return false;
			}
			session = SESSIONS.get(player.getUUID());
		}
		Composing.Result result;
		if (request.action() == ComposerRequestPayload.CHECK) {
			result = Composing.compose(player, request.text(), false, null);
		} else if (request.revision() != session.revision) {
			// A write against an older revision asks, in effect, for the state again: it counts against the shared gate.
			if (!RateGate.allow(player, Composing.RATE_ACTION, Composing.COMPOSE_RATE_TICKS)) {
				return false;
			}
			result = new Composing.Result(Composing.Outcome.CHANGED, ComposedSpells.instrument(player), "", null, List.of());
		} else if (request.action() == ComposerRequestPayload.INSCRIBE) {
			result = Composing.compose(player, request.text(), true, session.held);
		} else {
			result = Composing.clear(player, session.held);
		}
		Composing.Outcome outcome = result.outcome();
		if (outcome == Composing.Outcome.TOO_FAST) {
			return false;
		}
		if (outcome == Composing.Outcome.INSCRIBED || outcome == Composing.Outcome.CLEARED || outcome == Composing.Outcome.CHANGED
				|| !session.held.holds(player)) {
			refresh(player, session);
		}
		send(player, result(request.sequence(), session.revision, result, player));
		return true;
	}

	/** Sends to the player's screen, if their game can receive it (a test's stand-in player cannot). */
	private static void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
		if (ServerPlayNetworking.canSend(player, payload.type())) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	/** What a screen may show and offer this player now. */
	public static ComposerStatePayload state(ServerPlayer player, int revision) {
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			return new ComposerStatePayload(revision, ComposerStatePayload.DISABLED, Optional.empty(), List.of(), 0, "");
		}
		Catalog catalog = ConcordanceData.rules().catalog();
		Knowledge knowledge = ConcordanceProgress.knowledge(player);
		List<ComposerStatePayload.Word> words = new ArrayList<>();
		int locked = 0;
		for (Component component : catalog.components().values()) {
			if (component.authored()) {
				continue;
			}
			Definitions.Requirement needs = component.requires();
			if (!knowledge.state(needs.research()).atLeast(needs.state())) {
				locked++;
			} else if (words.size() < ComposerStatePayload.MAX_WORDS && component.id().length() <= ComposerStatePayload.MAX_ID) {
				words.add(word(component));
			}
		}
		Instrument instrument = ComposedSpells.instrument(player);
		if (instrument == null || instrument.item().length() > ComposerStatePayload.MAX_ID) {
			return new ComposerStatePayload(revision, ComposerStatePayload.NO_INSTRUMENT, Optional.empty(), words, locked, "");
		}
		Inscription inscription = ComposedSpells.inscription(player.getMainHandItem());
		return new ComposerStatePayload(revision, ComposerStatePayload.READY, Optional.of(ComposerStatePayload.InstrumentView.of(instrument)),
				words, locked, inscription == null ? "" : inscription.text());
	}

	private static ComposerStatePayload.Word word(Component component) {
		int aspects = 0;
		if (component.part() instanceof Component.Modifier modifier) {
			aspects = 1 << modifier.aspect().ordinal();
		} else {
			for (Component.Aspect aspect : Component.Aspect.values()) {
				if (Compiler.changes(aspect, component)) {
					aspects |= 1 << aspect.ordinal();
				}
			}
		}
		return new ComposerStatePayload.Word(component.id(), component.slot().ordinal(), component.capacity(), component.focus(), aspects);
	}

	/** The answer a screen is sent for {@code result}: its heading, numbers, each distinct line once, and the words blamed. */
	public static ComposerResultPayload result(int sequence, int revision, Composing.Result result, ServerPlayer player) {
		Plan plan = result.plan();
		Optional<ComposerResultPayload.Summary> summary = plan == null ? Optional.empty() : Optional.of(new ComposerResultPayload.Summary(
				plan.focus(), plan.cooldown(), plan.capacity(), plan.limits().targets(), plan.limits().work()));
		// The compiler names a problem each time it meets it; a screen shows each once.
		List<Text> distinct = new ArrayList<>(new LinkedHashSet<>(result.lines()));
		int room = distinct.size() > ComposerResultPayload.MAX_LINES ? ComposerResultPayload.MAX_LINES - 1 : distinct.size();
		List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
		for (int i = 0; i < room; i++) {
			lines.add(ComposeText.show(distinct.get(i)));
		}
		if (distinct.size() > room) {
			lines.add(net.minecraft.network.chat.Component.translatable("compose.jugcraft.more_problems", distinct.size() - room));
		}
		String text = result.text().length() > ComposerRequestPayload.MAX_TEXT ? result.text().substring(0, ComposerRequestPayload.MAX_TEXT)
				: result.text();
		return new ComposerResultPayload(sequence, revision, result.outcome().ordinal(), text, heading(result, player),
				new ComposerResultPayload.Report(summary, lines, blamed(result.problems())));
	}

	/** What a result came to, in a line: the same words the command uses. */
	private static net.minecraft.network.chat.Component heading(Composing.Result result, ServerPlayer player) {
		String text = result.text().length() > Grammar.MAX_TEXT ? result.text().substring(0, Grammar.MAX_TEXT) + "..." : result.text();
		return switch (result.outcome()) {
			case DISABLED -> net.minecraft.network.chat.Component.translatable("message.jugcraft.concordance.disabled");
			case NO_INSTRUMENT -> net.minecraft.network.chat.Component.translatable("compose.jugcraft.problem.no_instrument");
			case CHANGED -> net.minecraft.network.chat.Component.translatable("compose.jugcraft.problem.changed");
			case TOO_FAST -> net.minecraft.network.chat.Component.translatable("compose.jugcraft.problem.too_fast");
			case NOTHING_INSCRIBED -> net.minecraft.network.chat.Component.translatable("compose.jugcraft.nothing_inscribed");
			case INVALID -> net.minecraft.network.chat.Component.translatable("compose.jugcraft.invalid", text);
			case VALID -> net.minecraft.network.chat.Component.translatable("compose.jugcraft.valid", text);
			case INSCRIBED -> net.minecraft.network.chat.Component.translatable("compose.jugcraft.inscribed",
					player.getMainHandItem().getHoverName(), text);
			case CLEARED -> net.minecraft.network.chat.Component.translatable("compose.jugcraft.cleared");
		};
	}

	/** The written names the problems blame: each component a problem names, and each unknown or malformed word. */
	static List<String> blamed(List<Text> problems) {
		Set<String> names = new LinkedHashSet<>();
		for (Text problem : problems) {
			for (Object arg : problem.args()) {
				if (arg instanceof Text.Ref ref && ref.kind().equals("component")) {
					names.add(Composition.shortName(ref.id()));
				} else if (arg instanceof String name && WRITTEN.contains(problem.key())) {
					names.add(name);
				}
			}
		}
		return names.stream().filter(name -> name.length() <= ComposerResultPayload.MAX_NAME).limit(Grammar.MAX_NAMES).toList();
	}
}
