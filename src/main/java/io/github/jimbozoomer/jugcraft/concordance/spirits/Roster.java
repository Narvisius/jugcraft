package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.journal.Journal;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Logistics;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Progress;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.logistics.RequestState;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-07 (D-S11): the roster screen's server side. It lists, for its reader only, every Bound Will they
 * hold (each pact and agreement, one without its place or not yet claimed by it included, its presence in the same row),
 * every courier request they filed, and every worker on their roster not already shown with its record: at most
 * RosterStatePayload.MAX_ROWS rows, records first, so a pact without its perch shows even when the roster's 32 are full.
 * It owns and saves nothing: each row is read again from WorkerRoster, BoundWills and CourierLedger when asked, from what
 * is loaded only. Its actions go through the existing authorities, as the reader alone, alive and in person: suspend and
 * resume by record id (AgreementHolder.toggle, offered by the record's hold, so nothing toggles back), end
 * (AgreementHolder.end: the escrow back once into the living reader's inventory, its presence departing, the departure
 * kept), cancel and recover (CourierLedger: only the requester; recovery hands over only cargo the ledger holds stranded,
 * never anything a container holds, loaded or not). A request is dropped from a stand-in, out of bounds or not newer than
 * the last this reader sent; then it passes the gate its kind of action shares (the Spirit Anchor's use, or the logistics
 * commands'); names a row listed to this reader in its last state (else not_listed); at that row's revision now (else
 * changed, with the roster again); and asks only what the row offers (else not_offered, or disabled while switched off).
 * A row's type stays here. Sessions are memory only, forgotten when the reader leaves or the server stops. The journal's
 * pacts section reads the same rows (report).
 */
public final class Roster {
	/** Row types, kept in the session only, never sent (tools/concordance_roster.py TYPES). */
	public static final int RECORD = 0;
	public static final int REQUEST = 1;
	public static final int WORKER = 2;
	/** How often a reader's roster may be read again (ticks); a read too soon waits for its turn, as the composer's does. */
	public static final int OPEN_TICKS = 10;
	static final String OPEN_ACTION = "roster";
	/** Suspending, resuming and ending share the Spirit Anchor's own gate (SpiritAnchorBlock.use). */
	static final String AGREEMENT_ACTION = "workers";
	public static final int AGREEMENT_TICKS = 4;
	/** Cancelling and recovering share the logistics commands' gate (Couriers.command). */
	static final String COURIER_ACTION = "logistics";
	public static final int COURIER_TICKS = 10;
	/** The roster's own answers (tools/concordance_roster.py ANSWERS, with PactMenu's CHANGED, TOO_FAST, NOT_OFFERED, DISABLED). */
	public static final String NOT_LISTED = "not_listed";
	public static final String NOT_PERMITTED = "not_permitted";
	/**
	 * An unloaded worker's portrait by its kind (tools/concordance_roster.py KIND_PORTRAITS); any other kind is jugcraft:<kind>.
	 * A spirit has none of its own: a Gathering Shade's kind and a Workshop Hand's are both spirit, so the client draws the
	 * Spirit Anchor rather than the wrong creature (the unit's critique 8).
	 */
	static final Map<String, String> KIND_PORTRAITS = Map.of("familiar", "jugcraft:hearthling", "spirit", "",
			"construct", "jugcraft:clockwork_porter", "pact", "jugcraft:stolas");
	private static final String KEY = "message.jugcraft.concordance.roster.";

	/**
	 * One reader's open roster: the last sequence taken, the rows last listed (id to type), the state last sent and when an
	 * action was last answered "too fast" (game time).
	 */
	private static final class Session {
		private int sequence;
		private final Map<UUID, Integer> listed = new HashMap<>();
		private @Nullable RosterStatePayload lastSent;
		private long tooFastAt = Long.MIN_VALUE / 2;
	}

	/** What a reader is shown (at most MAX_ROWS rows, with each row's type, which stays here) and how many rows there are in all. */
	public record Listing(List<RosterStatePayload.Row> rows, List<Integer> types, int total) {
	}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	/** Opens that came too soon, newest sequence only, by player: served by the server tick once the gate allows. */
	private static final Map<UUID, Integer> PENDING = new HashMap<>();

	private Roster() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(RosterRequestPayload.TYPE, RosterRequestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RosterStatePayload.TYPE, RosterStatePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(RosterRequestPayload.TYPE, (payload, context) -> receive(context.player(), payload));
		ServerTickEvents.END_SERVER_TICK.register(Roster::tick);
		// On the server thread, as the player leaves the list (RateGate's reason; PlayerList.remove fires it first).
		ServerPlayerEvents.LEAVE.register(player -> forget(player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SESSIONS.clear();
			PENDING.clear();
		});
	}

	/** A screen's request (the receiver calls this, and so do tests): the state answered, or null when dropped or waiting to open. */
	public static @Nullable RosterStatePayload receive(ServerPlayer player, RosterRequestPayload request) {
		int action = request.action();
		if (Authority.isStandIn(player) || action < 0 || action >= RosterRequestPayload.ACTIONS.size() || request.revision() < 0) {
			return null;
		}
		UUID me = player.getUUID();
		if (action == RosterRequestPayload.OPEN) {
			PENDING.remove(me);
			if (!RateGate.allow(player, OPEN_ACTION, OPEN_TICKS)) {
				PENDING.put(me, request.sequence());
				return null;
			}
			return open(player, request.sequence());
		}
		Session session = SESSIONS.get(me);
		if (session == null || request.sequence() <= session.sequence) {
			return null;
		}
		session.sequence = request.sequence();
		long now = player.level().getGameTime();
		boolean courier = action == RosterRequestPayload.CANCEL || action == RosterRequestPayload.RECOVER;
		int gate = courier ? COURIER_TICKS : AGREEMENT_TICKS;
		if (!RateGate.allow(player, courier ? COURIER_ACTION : AGREEMENT_ACTION, gate)) {
			// One "too fast" answer a gate's span: the rest are dropped, as the composer drops them, so a client's sequence
			// numbers never make the server resend its roster for every packet (the unit review). A screen has one action
			// in flight, and its patience returns its controls if an answer never comes.
			if (now - session.tooFastAt < gate) {
				return null;
			}
			session.tooFastAt = now;
			return again(player, session, new PactMenu.Answered(PactMenu.TOO_FAST, false, line("answer.too_fast")), request.sequence());
		}
		Integer type = session.listed.get(request.row());
		if (type == null) {
			return answer(player, session, new PactMenu.Answered(NOT_LISTED, false, line("answer.not_listed")), request.sequence(), now);
		}
		RosterStatePayload.Row current = row(player, type, request.row(), now);
		if (current == null || current.revision() != request.revision()) {
			return answer(player, session, new PactMenu.Answered(PactMenu.CHANGED, false, line("answer.changed")), request.sequence(), now);
		}
		if (!current.offers(action)) {
			return answer(player, session, Workers.enabled() ? notOffered() : disabled(), request.sequence(), now);
		}
		PactMenu.Answered done = act(player, type, request.row(), action, now);
		player.inventoryMenu.broadcastChanges();
		return answer(player, session, done, request.sequence(), now);
	}

	/** The last request sequence this reader's roster took (0 before it opened; tests send the next). */
	public static int lastSequence(ServerPlayer player) {
		Session session = SESSIONS.get(player.getUUID());
		return session == null ? 0 : session.sequence;
	}

	/** A courier request's row id: UUID(0, its id). Record and worker ids are random (version 4), so never zero in their high bits. */
	public static UUID rowOf(long request) {
		return new UUID(0L, request);
	}

	static long requestOf(UUID row) {
		return row.getMostSignificantBits() == 0L ? row.getLeastSignificantBits() : -1L;
	}

	private static void tick(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, Integer>> waiting = PENDING.entrySet().iterator();
		while (waiting.hasNext()) {
			Map.Entry<UUID, Integer> next = waiting.next();
			ServerPlayer player = server.getPlayerList().getPlayer(next.getKey());
			if (player == null) {
				waiting.remove();
			} else if (RateGate.allow(player, OPEN_ACTION, OPEN_TICKS)) {
				int sequence = next.getValue();
				waiting.remove();
				open(player, sequence);
			}
		}
	}

	private static void forget(UUID player) {
		SESSIONS.remove(player);
		PENDING.remove(player);
	}

	private static RosterStatePayload open(ServerPlayer player, int sequence) {
		Session session = SESSIONS.computeIfAbsent(player.getUUID(), unused -> new Session());
		// An open served late never lowers the sequence: an action already taken is not taken again.
		session.sequence = Math.max(session.sequence, sequence);
		return answer(player, session, null, sequence, player.level().getGameTime());
	}

	private static RosterStatePayload answer(ServerPlayer player, Session session, PactMenu.@Nullable Answered answered, int sequence, long now) {
		Listing listing = list(player, now);
		session.listed.clear();
		for (int i = 0; i < listing.rows().size(); i++) {
			session.listed.put(listing.rows().get(i).id(), listing.types().get(i));
		}
		RosterStatePayload state = new RosterStatePayload(sequence, Workers.enabled(), listing.rows(), listing.total() - listing.rows().size(),
				Optional.ofNullable(answered).map(done -> new PactStatePayload.Result(sequence, done.reason(), done.done(), done.said())));
		session.lastSent = state;
		send(player, state);
		return state;
	}

	/** Too soon: the rows last sent, again, with the answer (nothing is read). */
	private static RosterStatePayload again(ServerPlayer player, Session session, PactMenu.Answered answered, int sequence) {
		RosterStatePayload last = session.lastSent;
		RosterStatePayload state = new RosterStatePayload(sequence, last != null && last.enabled(), last == null ? List.of() : last.rows(),
				last == null ? 0 : last.more(), Optional.of(new PactStatePayload.Result(sequence, answered.reason(), answered.done(), answered.said())));
		session.lastSent = state;
		send(player, state);
		return state;
	}

	private static void send(ServerPlayer player, RosterStatePayload state) {
		if (ServerPlayNetworking.canSend(player, RosterStatePayload.TYPE)) {
			ServerPlayNetworking.send(player, state);
		}
	}

	/** {@code reader}'s rows at game time {@code now}: records (in the order sealed), their requests (oldest first), then other workers. */
	public static Listing list(ServerPlayer reader, long now) {
		MinecraftServer server = reader.level().getServer();
		UUID me = reader.getUUID();
		Map<UUID, WorkerRoster.Entry> workers = WorkerRoster.of(server).workers(me);
		CourierLedger ledger = CourierLedger.of(server);
		Map<UUID, Request> claims = claims(ledger);
		List<BoundWill> records = held(server, me);
		Set<UUID> shown = new HashSet<>();
		for (BoundWill record : records) {
			UUID presence = presence(record);
			if (presence != null && workers.containsKey(presence)) {
				shown.add(presence);
			}
		}
		List<RosterStatePayload.Row> rows = new ArrayList<>();
		List<Integer> types = new ArrayList<>();
		int total = 0;
		for (BoundWill record : records) {
			total++;
			if (rows.size() < RosterStatePayload.MAX_ROWS) {
				UUID presence = presence(record);
				rows.add(recordRow(server, record, presence == null ? null : workers.get(presence), claims, now));
				types.add(RECORD);
			}
		}
		for (Request request : ledger.ledger().requests()) {
			if (request.ticket().requester().equals(me)) {
				total++;
				if (rows.size() < RosterStatePayload.MAX_ROWS) {
					rows.add(requestRow(server, ledger, request, now));
					types.add(REQUEST);
				}
			}
		}
		for (Map.Entry<UUID, WorkerRoster.Entry> worker : workers.entrySet()) {
			if (!shown.contains(worker.getKey())) {
				total++;
				if (rows.size() < RosterStatePayload.MAX_ROWS) {
					rows.add(workerRow(server, worker.getKey(), worker.getValue(), claims));
					types.add(WORKER);
				}
			}
		}
		return new Listing(List.copyOf(rows), List.copyOf(types), total);
	}

	/**
	 * The journal's pacts lines after Stolas's Regard's (D-07; StolasRegard.report calls this): each Bound Will {@code player}
	 * holds, in the order sealed, named with where it is kept and its next obligation, why it waits as the exact; then their
	 * departures, newest first (at most Pacts.HISTORY). Only reads; nothing for one who holds and lost none.
	 */
	public static void report(ServerPlayer player, Journal.Report out) {
		MinecraftServer server = player.level().getServer();
		long now = player.level().getGameTime();
		UUID me = player.getUUID();
		Map<UUID, WorkerRoster.Entry> workers = WorkerRoster.of(server).workers(me);
		Map<UUID, Request> claims = claims(CourierLedger.of(server));
		for (BoundWill record : held(server, me)) {
			UUID presence = presence(record);
			RosterStatePayload.Row row = recordRow(server, record, presence == null ? null : workers.get(presence), claims, now);
			out.line(Component.translatable("journal.jugcraft.pacts.kept", row.name(), row.place(), row.next()), row.waiting());
		}
		List<Pacts.Departure> history = PactHistory.of(server).history(me);
		for (int i = history.size() - 1; i >= 0; i--) {
			Pacts.Departure departure = history.get(i);
			out.line(Component.translatable("journal.jugcraft.pacts.departed", PactKeeping.name(departure.counterpart()),
					Math.floorDiv(departure.time(), Pacts.DAY), Component.translatable("message.jugcraft.concordance.workers.departed." + departure.reason())));
		}
	}

	/** One row of {@code reader}'s as it stands now (null when it is gone or not theirs). */
	private static RosterStatePayload.@Nullable Row row(ServerPlayer reader, int type, UUID id, long now) {
		MinecraftServer server = reader.level().getServer();
		UUID me = reader.getUUID();
		if (type == RECORD) {
			BoundWill record = BoundWills.of(server).ledger().get(id);
			if (record == null || !record.holder().equals(me)) {
				return null;
			}
			UUID presence = presence(record);
			return recordRow(server, record, presence == null ? null : WorkerRoster.of(server).workers(me).get(presence),
					claims(CourierLedger.of(server)), now);
		}
		if (type == REQUEST) {
			CourierLedger ledger = CourierLedger.of(server);
			long number = requestOf(id);
			Request request = number < 1L ? null : ledger.ledger().get(number);
			return request == null || !request.ticket().requester().equals(me) ? null : requestRow(server, ledger, request, now);
		}
		WorkerRoster.Entry entry = WorkerRoster.of(server).workers(me).get(id);
		return entry == null ? null : workerRow(server, id, entry, claims(CourierLedger.of(server)));
	}

	private static RosterStatePayload.Row recordRow(MinecraftServer server, BoundWill record, WorkerRoster.@Nullable Entry entry,
			Map<UUID, Request> claims, long now) {
		BoundWill.Pact pact = record.pact();
		Place place = pact == null ? null : pact.place();
		boolean loaded = place != null && loaded(server, place);
		UUID presence = presence(record);
		int escrow = BoundWills.of(server).escrow(record.id()).stream().mapToInt(ItemStack::getCount).sum();
		Status held = Pacts.status(record);
		boolean enabled = Workers.enabled();
		int icon;
		Component waiting;
		if (!enabled) {
			icon = RosterStatePayload.DISABLED;
			waiting = line("waiting.disabled");
		} else if (pact == null) {
			icon = RosterStatePayload.UNCLAIMED;
			waiting = line("waiting.unclaimed");
		} else if (place == null) {
			icon = RosterStatePayload.WITHOUT_PLACE;
			waiting = line("waiting.without_place");
		} else if (AgreementHolder.stranded(server, record)) {
			icon = RosterStatePayload.WITHOUT_PLACE;
			waiting = line("waiting.stranded_place");
		} else if (held == Status.SUSPENDED || held == Status.UPKEEP_HELD) {
			icon = icon(held);
			waiting = status(held.id);
		} else if (presence != null && entry == null) {
			icon = RosterStatePayload.LOST;
			waiting = line("waiting.lost");
		} else if (!loaded) {
			icon = RosterStatePayload.UNLOADED;
			waiting = line("waiting.unloaded");
		} else if (entry != null) {
			icon = icon(Status.fromId(entry.status()));
			waiting = status(entry.status());
		} else {
			icon = RosterStatePayload.ACTIVE;
			waiting = line("waiting.active");
		}
		int actions = 0;
		if (enabled) {
			actions = RosterStatePayload.mask(RosterRequestPayload.END);
			if (place != null && pact.hold() == BoundWill.Hold.NONE) {
				actions |= RosterStatePayload.mask(RosterRequestPayload.SUSPEND);
			} else if (place != null && pact.hold() == BoundWill.Hold.HOLDER) {
				actions |= RosterStatePayload.mask(RosterRequestPayload.RESUME);
			}
		}
		String counterpart = Pacts.counterpart(record.counterpart());
		String kind = entry != null ? entry.kind() : counterpart.equals(StolasEntity.COUNTERPART) ? "pact" : "spirit";
		Component name = PactKeeping.name(record.counterpart());
		BoundWill.Service service = pact == null ? null : pact.service();
		Request claim = presence == null ? null : claims.get(presence);
		Component task = claim != null ? task(claim) : entry != null ? status(entry.status()) : line("task.none");
		return new RosterStatePayload.Row(record.id(), portrait(counterpart), kind(kind), name,
				service == null ? line("service.unknown") : Component.translatableWithFallback(KEY + "service." + service.work(), service.work()),
				pact == null ? line("place.unclaimed") : place == null ? line("place.none") : place(place), loaded, task, next(server, record, now),
				waiting, icon, revision(record, escrow), actions, escrow > 0 ? line("confirm.end", name, escrow) : line("confirm.end_none", name));
	}

	/** A record's next obligation: its next offering (or the grace, or what lifts its hold), else its day's tasks. */
	private static Component next(MinecraftServer server, BoundWill record, long now) {
		BoundWill.Pact pact = record.pact();
		if (pact == null) {
			return line("next.none");
		}
		BoundWill.Upkeep upkeep = pact.upkeep();
		if (upkeep == null) {
			BoundWill.Service service = pact.service();
			return service == null ? line("next.none") : line("next.quota", Pacts.done(pact, now), service.quota());
		}
		Component offering = AgreementHolder.offeringName(upkeep.offering());
		return switch (pact.hold()) {
			case UPKEEP -> line("next.held", upkeep.count(), offering);
			case HOLDER -> line("next.suspended");
			case NONE -> upkeep.warned() && upkeep.graceUntil() > now
					? line("next.warned", upkeep.count(), offering, AgreementHolder.days(upkeep.graceUntil() - now))
					: upkeep.due() <= now ? line("next.at_visit", upkeep.count(), offering)
					: line("next.offering", upkeep.count(), offering, AgreementHolder.days(upkeep.due() - now),
							BoundWills.of(server).periods(record.id(), AgreementHolder.offering(upkeep.offering()), upkeep.count()), upkeep.cap());
		};
	}

	private static RosterStatePayload.Row requestRow(MinecraftServer server, CourierLedger ledger, Request request, long now) {
		Progress p = request.progress();
		RequestState state = request.state();
		ItemVariant item = ledger.item(request.id());
		Component what = item == null ? line("request.unknown_item") : item.toStack().getHoverName();
		String portrait = item == null ? "" : portrait(BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
		Place post = request.ticket().post();
		boolean stranded = p.carried() > 0 && !p.aboard();
		boolean reserved = state == RequestState.CLAIMED && p.reserved() > 0 && p.source() != null;
		// Where its next step is: its source while reserved or taken back, its destination in transit, its post otherwise.
		Place needs = stranded ? post : p.returning() || reserved ? (p.source() != null ? p.source() : post)
				: state == RequestState.IN_TRANSIT ? request.ticket().destination() : post;
		boolean loaded = loaded(server, needs);
		boolean enabled = Workers.enabled();
		int icon;
		Component waiting;
		if (!enabled) {
			icon = RosterStatePayload.DISABLED;
			waiting = line("waiting.disabled");
		} else if (stranded) {
			icon = RosterStatePayload.STRANDED;
			waiting = line("waiting.stranded");
		} else if (p.returning()) {
			icon = loaded ? RosterStatePayload.RETURNING : RosterStatePayload.UNLOADED;
			waiting = line("waiting.returning");
		} else if (state == RequestState.IN_TRANSIT) {
			icon = loaded ? RosterStatePayload.IN_TRANSIT : RosterStatePayload.UNLOADED;
			waiting = line(loaded ? "waiting.in_transit" : "waiting.destination_unloaded");
		} else if (reserved) {
			icon = loaded ? RosterStatePayload.WAITING : RosterStatePayload.UNLOADED;
			waiting = line(loaded ? "waiting.reserved" : "waiting.reserved_unloaded", p.reserved(), place(p.source()));
		} else if (p.notBefore() > now && p.note().matches("[a-z_]+")) {
			icon = RosterStatePayload.WAITING;
			waiting = Component.translatable("compose.jugcraft.courier.note." + p.note());
		} else {
			icon = RosterStatePayload.WAITING;
			waiting = Component.translatable("compose.jugcraft.courier.state." + state.id);
		}
		int actions = 0;
		if (enabled) {
			if (!p.returning()) {
				actions |= RosterStatePayload.mask(RosterRequestPayload.CANCEL);
			}
			if (stranded) {
				actions |= RosterStatePayload.mask(RosterRequestPayload.RECOVER);
			}
		}
		return new RosterStatePayload.Row(rowOf(request.id()), portrait, line("kind.request"), line("request", request.id(), request.ticket().wanted(), what),
				line("service.request", place(request.ticket().destination())), place(post), loaded,
				Component.translatable("compose.jugcraft.courier.state." + state.id),
				line("next.delivered", p.delivered(), request.ticket().wanted(), p.carried()), waiting, icon, revision(request), actions,
				p.carried() > 0 ? line("confirm.cancel", request.id(), p.carried()) : line("confirm.cancel_none", request.id()));
	}

	private static RosterStatePayload.Row workerRow(MinecraftServer server, UUID id, WorkerRoster.Entry entry, Map<UUID, Request> claims) {
		ServerLevel where = levelOf(server, entry.dimension());
		Entity found = where == null ? null : where.getEntity(id);
		boolean loaded = found != null && !found.isRemoved();
		Component kind = kind(entry.kind());
		Request claim = claims.get(id);
		boolean enabled = Workers.enabled();
		BlockPos at = entry.blockPos();
		String portrait = loaded ? BuiltInRegistries.ENTITY_TYPE.getKey(found.getType()).toString()
				: KIND_PORTRAITS.getOrDefault(entry.kind(), "jugcraft:" + entry.kind());
		return new RosterStatePayload.Row(id, portrait(portrait), kind, loaded ? found.getName() : kind,
				Component.translatableWithFallback(KEY + "kind_service." + entry.kind(), ""),
				place(new Place(entry.dimension(), at.getX(), at.getY(), at.getZ())), loaded,
				claim != null ? task(claim) : status(entry.status()), Component.empty(),
				enabled ? status(entry.status()) : line("waiting.disabled"), enabled ? icon(Status.fromId(entry.status())) : RosterStatePayload.DISABLED,
				0, 0, Component.empty());
	}

	/** The action itself, re-checked whatever the screen showed (receive calls it after the listing; tests call it directly). */
	public static PactMenu.Answered act(ServerPlayer player, int type, UUID row, int action, long now) {
		if (!Workers.enabled()) {
			return disabled();
		}
		if (Authority.isStandIn(player) || !player.isAlive()) {
			return new PactMenu.Answered(NOT_PERMITTED, false, line("answer.not_permitted"));
		}
		ServerLevel level = player.level();
		if (type == RECORD) {
			return record(level, player, row, action, now);
		}
		if (type == REQUEST) {
			return request(level, player, requestOf(row), action, now);
		}
		return notOffered();
	}

	private static PactMenu.Answered record(ServerLevel level, ServerPlayer player, UUID id, int action, long now) {
		BoundWill record = BoundWills.of(level.getServer()).ledger().get(id);
		if (record == null) {
			return ended(AgreementHolder.Ended.NOTHING);
		}
		if (!record.holder().equals(player.getUUID())) {
			return ended(AgreementHolder.Ended.NOT_HOLDER);
		}
		// Suspend and resume are idempotent (D-P108): one already held by its holder stays so, one not held stays so; a hold for
		// upkeep lifts only by paying; a record not yet claimed has nothing to hold.
		return switch (action) {
			case RosterRequestPayload.SUSPEND -> toggled(Pacts.held(record)
					? (record.pact().hold() == BoundWill.Hold.HOLDER ? AgreementHolder.Toggle.SUSPENDED : AgreementHolder.Toggle.HELD_FOR_UPKEEP)
					: AgreementHolder.toggle(level, player, id, now));
			case RosterRequestPayload.RESUME -> toggled(record.pact() == null ? AgreementHolder.Toggle.NOTHING
					: !Pacts.held(record) ? AgreementHolder.Toggle.RESUMED
					: record.pact().hold() == BoundWill.Hold.UPKEEP ? AgreementHolder.Toggle.HELD_FOR_UPKEEP
					: AgreementHolder.toggle(level, player, id, now));
			case RosterRequestPayload.END -> end(level, player, record, now);
			default -> notOffered();
		};
	}

	private static PactMenu.Answered end(ServerLevel level, ServerPlayer player, BoundWill record, long now) {
		int returned = BoundWills.of(level.getServer()).escrow(record.id()).stream().mapToInt(ItemStack::getCount).sum();
		AgreementHolder.Ended ended = AgreementHolder.end(level, null, player, record.id(), Pacts.ENDED, now);
		if (ended != AgreementHolder.Ended.ENDED) {
			return ended(ended);
		}
		return new PactMenu.Answered("ended", true, Component.translatable("message.jugcraft.concordance.pact.departed",
				PactKeeping.name(record.counterpart()), Component.translatable("message.jugcraft.concordance.workers.departed.ended"), returned));
	}

	private static PactMenu.Answered request(ServerLevel level, ServerPlayer player, long id, int action, long now) {
		if (action != RosterRequestPayload.CANCEL && action != RosterRequestPayload.RECOVER) {
			return notOffered();
		}
		CourierLedger ledger = CourierLedger.of(level.getServer());
		if (id < 1L || ledger.ledger().get(id) == null) {
			return courier(Logistics.Outcome.NOT_FOUND, "", id);
		}
		if (action == RosterRequestPayload.CANCEL) {
			Logistics.Outcome outcome = ledger.cancel(id, player.getUUID(), now);
			return courier(outcome, ledger.ledger().get(id) == null ? "cancelled" : "returning", id);
		}
		return courier(ledger.recover(id, player, now), "recovered", id);
	}

	/** A courier outcome in the logistics commands' own words (Couriers.report). */
	private static PactMenu.Answered courier(Logistics.Outcome outcome, String done, long id) {
		if (outcome == Logistics.Outcome.DONE) {
			return new PactMenu.Answered(done, true, Component.translatable("message.jugcraft.concordance.courier." + done, id));
		}
		String reason = outcome.name().toLowerCase(Locale.ROOT);
		return new PactMenu.Answered(reason, false, Component.translatable("message.jugcraft.concordance.courier.refused." + reason,
				Logistics.MAX_OPEN_PER_PLAYER, Logistics.MAX_WANTED));
	}

	private static PactMenu.Answered ended(AgreementHolder.Ended ended) {
		return new PactMenu.Answered(ended.name().toLowerCase(Locale.ROOT), ended == AgreementHolder.Ended.ENDED, AgreementHolder.told(ended));
	}

	private static PactMenu.Answered toggled(AgreementHolder.Toggle toggle) {
		return new PactMenu.Answered(toggle.name().toLowerCase(Locale.ROOT),
				toggle == AgreementHolder.Toggle.SUSPENDED || toggle == AgreementHolder.Toggle.RESUMED, AgreementHolder.told(toggle));
	}

	private static PactMenu.Answered notOffered() {
		return new PactMenu.Answered(PactMenu.NOT_OFFERED, false, line("answer.not_offered"));
	}

	private static PactMenu.Answered disabled() {
		return new PactMenu.Answered(PactMenu.DISABLED, false, Component.translatable("message.jugcraft.concordance.disabled"));
	}

	/** What an action on a record depends on: where it is kept, its hold, its presence, its escrow and its upkeep's dates. */
	static int revision(BoundWill record, int escrow) {
		BoundWill.Pact pact = record.pact();
		if (pact == null) {
			return Objects.hash(record.id(), escrow) & Integer.MAX_VALUE;
		}
		BoundWill.Upkeep upkeep = pact.upkeep();
		return Objects.hash(record.id(), pact.place(), pact.hold().ordinal(), pact.holdSince(), pact.presence(), escrow,
				upkeep == null ? 0L : upkeep.due(), upkeep == null ? 0L : upkeep.graceUntil()) & Integer.MAX_VALUE;
	}

	/** What an action on a request depends on: its state, counts, courier and source. */
	static int revision(Request request) {
		Progress p = request.progress();
		return Objects.hash(request.id(), request.state().ordinal(), p.delivered(), p.carried(), p.reserved(), p.worker(), p.source())
				& Integer.MAX_VALUE;
	}

	/** A status's icon (RosterStatePayload's numbers): doing something or idle is active; a hold, upkeep, an unloaded place or the switch say so. */
	static int icon(@Nullable Status status) {
		if (status == null) {
			return RosterStatePayload.WAITING;
		}
		return switch (status) {
			case SUSPENDED -> RosterStatePayload.SUSPENDED;
			case UPKEEP_DUE, UPKEEP_HELD -> RosterStatePayload.UPKEEP;
			case DESTINATION_UNLOADED -> RosterStatePayload.UNLOADED;
			case DISABLED -> RosterStatePayload.DISABLED;
			case IDLE -> RosterStatePayload.ACTIVE;
			default -> status.active ? RosterStatePayload.ACTIVE : RosterStatePayload.WAITING;
		};
	}

	/** A portrait id as sent: empty when too long for the payload. */
	private static String portrait(String id) {
		return id.length() <= RosterStatePayload.MAX_PORTRAIT ? id : "";
	}

	private static List<BoundWill> held(MinecraftServer server, UUID holder) {
		List<BoundWill> held = BoundWills.of(server).ledger().heldBy(holder);
		held.sort(Comparator.comparingLong((BoundWill record) -> record.sealedAt()).thenComparing(BoundWill::id));
		return held;
	}

	private static @Nullable UUID presence(BoundWill record) {
		return record.pact() == null ? null : record.pact().presence();
	}

	/** Each courier's claim, by courier. */
	private static Map<UUID, Request> claims(CourierLedger ledger) {
		Map<UUID, Request> claims = new HashMap<>();
		for (Request request : ledger.ledger().requests()) {
			UUID worker = request.progress().worker();
			if (worker != null) {
				claims.put(worker, request);
			}
		}
		return claims;
	}

	private static Component task(Request claim) {
		Progress p = claim.progress();
		return p.aboard() && p.carried() > 0 ? line("task.carrying", claim.id(), p.carried()) : line("task.claimed", claim.id());
	}

	private static Component status(String id) {
		return Component.translatableWithFallback("compose.jugcraft.worker.status." + id, id);
	}

	private static Component kind(String kind) {
		return Component.translatableWithFallback("message.jugcraft.concordance.workers.kind." + kind, kind);
	}

	private static Component place(Place place) {
		return line("place", place.x(), place.y(), place.z(),
				Component.translatableWithFallback(KEY + "dimension." + place.dimension().replace(':', '.'), place.dimension()));
	}

	/** Whether {@code place} is loaded now (nothing is loaded to ask). */
	private static boolean loaded(MinecraftServer server, Place place) {
		ServerLevel where = levelOf(server, place.dimension());
		return where != null && where.isLoaded(Couriers.pos(place));
	}

	private static @Nullable ServerLevel levelOf(MinecraftServer server, String dimension) {
		Identifier id = Identifier.tryParse(dimension);
		return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	private static Component line(String key, Object... args) {
		return Component.translatable(KEY + key, args);
	}
}
