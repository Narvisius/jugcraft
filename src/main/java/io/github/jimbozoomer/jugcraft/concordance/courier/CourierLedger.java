package io.github.jimbozoomer.jugcraft.concordance.courier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Event;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Logistics;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Progress;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Ticket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * The world's logistics ledger (roadmap step 18, docs/features/arcane-concordance-logistics.md), saved with the world
 * (data/jugcraft_courier_ledger.dat in the Overworld): the pure {@link Logistics} ledger and each request's exact item
 * (an {@link ItemVariant}: the item and every component it carries). A request asks for one variant, so what it has in
 * transit is fully described by the ledger's carried count of that variant: the ledger is the one authoritative place
 * for items in transit, and a courier holds only its claim's id. Items move through the Fabric Transfer API, as Jugcraft's
 * item pipes do: each pickup and delivery is one transaction that commits only if the ledger accepts the step, so a
 * container and the ledger never disagree.
 */
public final class CourierLedger extends SavedData {
	private static final Codec<Place> PLACE = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("dimension").forGetter(Place::dimension), Codec.INT.fieldOf("x").forGetter(Place::x),
			Codec.INT.fieldOf("y").forGetter(Place::y), Codec.INT.fieldOf("z").forGetter(Place::z)).apply(i, Place::new));
	private static final Codec<Ticket> TICKET = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("id").forGetter(Ticket::id), UUIDUtil.CODEC.fieldOf("requester").forGetter(Ticket::requester),
			PLACE.fieldOf("post").forGetter(Ticket::post), PLACE.fieldOf("destination").forGetter(Ticket::destination),
			Codec.STRING.fieldOf("item").forGetter(Ticket::item), Codec.INT.fieldOf("wanted").forGetter(Ticket::wanted),
			Codec.LONG.fieldOf("filed").forGetter(Ticket::filed)).apply(i, Ticket::new));
	private static final Codec<Progress> PROGRESS = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("delivered").forGetter(Progress::delivered), Codec.INT.fieldOf("carried").forGetter(Progress::carried),
			Codec.INT.fieldOf("reserved").forGetter(Progress::reserved),
			PLACE.optionalFieldOf("source").forGetter(p -> Optional.ofNullable(p.source())),
			UUIDUtil.CODEC.optionalFieldOf("worker").forGetter(p -> Optional.ofNullable(p.worker())),
			Codec.LONG.fieldOf("lease").forGetter(Progress::lease), Codec.BOOL.fieldOf("aboard").forGetter(Progress::aboard),
			Codec.BOOL.fieldOf("returning").forGetter(Progress::returning), Codec.STRING.fieldOf("note").forGetter(Progress::note),
			Codec.LONG.fieldOf("not_before").forGetter(Progress::notBefore))
			.apply(i, (delivered, carried, reserved, source, worker, lease, aboard, returning, note, notBefore) -> new Progress(delivered,
					carried, reserved, source.orElse(null), worker.orElse(null), lease, aboard, returning, note, notBefore)));
	private static final Codec<Request> REQUEST = RecordCodecBuilder.create(i -> i.group(
			TICKET.fieldOf("ticket").forGetter(Request::ticket), PROGRESS.fieldOf("progress").forGetter(Request::progress))
			.apply(i, Request::new));
	private static final Codec<Event> EVENT = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("time").forGetter(Event::time), Codec.LONG.fieldOf("request").forGetter(Event::request),
			Codec.STRING.fieldOf("kind").forGetter(Event::kind), Codec.INT.fieldOf("amount").forGetter(Event::amount),
			Codec.STRING.fieldOf("note").forGetter(Event::note)).apply(i, Event::new));
	private record History(Place post, List<Event> events) {
		static final Codec<History> CODEC = RecordCodecBuilder.create(i -> i.group(PLACE.fieldOf("post").forGetter(History::post),
				EVENT.listOf().fieldOf("events").forGetter(History::events)).apply(i, History::new));
	}
	/** One request's exact item. */
	private record Sample(long id, ItemVariant item) {
		static final Codec<Sample> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.LONG.fieldOf("id").forGetter(Sample::id),
				ItemVariant.CODEC.fieldOf("item").forGetter(Sample::item)).apply(i, Sample::new));
	}
	private record Stored(long nextId, List<Request> requests, List<History> histories, List<Sample> samples) {
		static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.LONG.fieldOf("next_id").forGetter(Stored::nextId),
				REQUEST.listOf().fieldOf("requests").forGetter(Stored::requests),
				History.CODEC.listOf().fieldOf("history").forGetter(Stored::histories),
				Sample.CODEC.listOf().fieldOf("samples").forGetter(Stored::samples)).apply(i, Stored::new));
	}
	/**
	 * Versioned (roadmap step 30). Read whole, not entry by entry: its requests, reservations, cargo and samples refer to
	 * one another, so it is either read as written or not at all.
	 */
	public static final Codec<CourierLedger> CODEC = Saved.versioned("courier_ledger", Stored.CODEC.xmap(CourierLedger::new, CourierLedger::saved));
	static final SavedDataType<CourierLedger> TYPE = new SavedDataType<>(Jugcraft.id("courier_ledger"), CourierLedger::new, CODEC, null);

	private Logistics ledger;
	private final Map<Long, ItemVariant> samples = new HashMap<>();

	CourierLedger() {
		ledger = new Logistics();
	}

	private CourierLedger(Stored saved) {
		Map<Place, List<Event>> histories = new LinkedHashMap<>();
		for (History history : saved.histories()) {
			histories.put(history.post(), history.events());
		}
		ledger = new Logistics(saved.nextId(), saved.requests(), histories);
		for (Sample sample : saved.samples()) {
			if (ledger.get(sample.id()) != null && !sample.item().isBlank()) {
				samples.put(sample.id(), sample.item());
			}
		}
	}

	private Stored saved() {
		List<History> histories = new ArrayList<>();
		ledger.histories().forEach((post, events) -> histories.add(new History(post, events)));
		List<Sample> kept = new ArrayList<>();
		for (Request request : ledger.requests()) {
			ItemVariant item = samples.get(request.id());
			if (item != null) {
				kept.add(new Sample(request.id(), item));
			}
		}
		return new Stored(ledger.nextId(), ledger.requests(), histories, kept);
	}

	public static CourierLedger of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** The pure ledger, for reading (claims, reservations, history). Steps that move items go through this class. */
	public Logistics ledger() {
		return ledger;
	}

	/** The exact item request {@code id} asks for, or null. */
	public @Nullable ItemVariant item(long id) {
		return samples.get(id);
	}

	/**
	 * An item's key in the ledger: its canonical encoding (the item and every component), the same in every session,
	 * so reservations of the same item always add up and a named ingot is never a plain one.
	 */
	public static String key(ItemVariant item, HolderLookup.Provider registries) {
		return ItemVariant.CODEC.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), item).result()
				.map(Object::toString).orElseGet(item::toString);
	}

	/** How many of request {@code id}'s item {@code storage} holds (a transaction that is never committed). */
	public long available(long id, Storage<ItemVariant> storage) {
		ItemVariant item = samples.get(id);
		if (item == null || !storage.supportsExtraction()) {
			return 0L;
		}
		try (Transaction look = Transaction.openOuter()) {
			return storage.extract(item, Long.MAX_VALUE, look);
		}
	}

	/** How many of {@code item} {@code storage} could take (a transaction that is never committed). */
	public static long room(Storage<ItemVariant> storage, ItemVariant item, long most) {
		if (!storage.supportsInsertion()) {
			return 0L;
		}
		try (Transaction look = Transaction.openOuter()) {
			return storage.insert(item, most, look);
		}
	}

	// ---------------------------------------------------------------- steps that move items

	public Logistics.Filed file(UUID requester, Place post, Place destination, ItemStack sample, int wanted, HolderLookup.Provider registries,
			long now) {
		ItemVariant item = ItemVariant.of(sample);
		Logistics.Filed filed = ledger.file(requester, post, destination, key(item, registries), wanted, now);
		if (filed.outcome() == Logistics.Outcome.DONE) {
			samples.put(filed.id(), item);
		}
		setDirty();
		return filed;
	}

	/**
	 * At its source, the courier takes what it reserved, as much as is really there, in one transaction that commits
	 * only if the ledger accepts the step: the items are then in transit, counted by the ledger.
	 */
	public Logistics.Outcome pickUp(long id, UUID worker, Storage<ItemVariant> source, long now) {
		Request request = ledger.get(id);
		ItemVariant item = samples.get(id);
		if (request == null || item == null) {
			return Logistics.Outcome.NOT_FOUND;
		}
		try (Transaction transaction = Transaction.openOuter()) {
			long taken = source.supportsExtraction() ? source.extract(item, request.progress().reserved(), transaction) : 0L;
			Logistics.Outcome outcome = ledger.pickUp(id, worker, (int) taken, now);
			if (outcome == Logistics.Outcome.DONE) {
				transaction.commit();
			}
			setDirty();
			return outcome;
		}
	}

	/** The courier at the destination puts in what fits, in one transaction the ledger's step commits. */
	public Logistics.Outcome deliver(long id, UUID worker, Storage<ItemVariant> destination, long now) {
		return unload(id, worker, destination, now, false);
	}

	/** The courier taking cargo back puts what fits into {@code into} (its source, or the post). */
	public Logistics.Outcome giveBack(long id, UUID worker, Storage<ItemVariant> into, long now) {
		return unload(id, worker, into, now, true);
	}

	private Logistics.Outcome unload(long id, UUID worker, Storage<ItemVariant> into, long now, boolean back) {
		Request request = ledger.get(id);
		ItemVariant item = samples.get(id);
		if (request == null || item == null) {
			return Logistics.Outcome.NOT_FOUND;
		}
		if (!request.claimedBy(worker, now)) {
			return Logistics.Outcome.NOT_YOURS;
		}
		if (back != request.progress().returning()) {
			return Logistics.Outcome.WRONG_STATE;
		}
		try (Transaction transaction = Transaction.openOuter()) {
			long put = into.supportsInsertion() ? into.insert(item, request.progress().carried(), transaction) : 0L;
			Logistics.Outcome outcome = back ? ledger.returned(id, worker, (int) put, now) : ledger.deliver(id, worker, (int) put, now);
			if (outcome == Logistics.Outcome.DONE) {
				transaction.commit();
				forgetClosed(id);
			}
			setDirty();
			return outcome;
		}
	}

	public Logistics.Outcome takeUp(long id, UUID worker, long now) {
		Logistics.Outcome outcome = ledger.takeUp(id, worker, now);
		setDirty();
		return outcome;
	}

	public Logistics.Outcome claim(long id, UUID worker, long now) {
		Logistics.Outcome outcome = ledger.claim(id, worker, now);
		setDirty();
		return outcome;
	}

	public Logistics.Outcome renew(long id, UUID worker, long now) {
		Logistics.Outcome outcome = ledger.renew(id, worker, now);
		setDirty();
		return outcome;
	}

	public Logistics.Outcome reserve(long id, UUID worker, Place source, Storage<ItemVariant> storage, long now) {
		Logistics.Outcome outcome = ledger.reserve(id, worker, source, (int) Math.min(Integer.MAX_VALUE, available(id, storage)), now);
		setDirty();
		return outcome;
	}

	public Logistics.Outcome release(long id, UUID worker, String why, boolean retry, long now) {
		Logistics.Outcome outcome = ledger.release(id, worker, why, retry, now);
		setDirty();
		return outcome;
	}

	public Logistics.Outcome cancel(long id, UUID by, long now) {
		Logistics.Outcome outcome = ledger.cancel(id, by, now);
		forgetClosed(id);
		setDirty();
		return outcome;
	}

	public void destinationGone(Place destination, long now) {
		List<Long> bound = new ArrayList<>();
		for (Request request : ledger.requests()) {
			if (request.ticket().destination().equals(destination)) {
				bound.add(request.id());
			}
		}
		ledger.destinationGone(destination, now);
		bound.forEach(this::forgetClosed);
		setDirty();
	}

	/**
	 * The requester takes stranded cargo themselves: the items go to their inventory (any that do not fit drop at their
	 * feet, as vanilla does) in the same call that closes the request.
	 */
	public Logistics.Outcome recover(long id, ServerPlayer player, long now) {
		ItemVariant item = samples.get(id);
		Logistics.Recovered recovered = ledger.recover(id, player.getUUID(), now);
		if (recovered.outcome() == Logistics.Outcome.DONE && item != null) {
			int left = recovered.amount();
			int stack = Math.max(1, item.toStack().getMaxStackSize());
			while (left > 0) {
				int count = Math.min(left, stack);
				player.getInventory().placeItemBackInInventory(item.toStack(count), Prediction.SERVER_ONLY);
				left -= count;
			}
			samples.remove(id);
		}
		setDirty();
		return recovered.outcome();
	}

	private void forgetClosed(long id) {
		if (ledger.get(id) == null) {
			samples.remove(id);
		}
	}

	/** Problems with the ledger or a request without its item (none expected; the game tests audit this). */
	public List<String> problems() {
		List<String> problems = new ArrayList<>(ledger.problems());
		for (Request request : ledger.requests()) {
			if (!samples.containsKey(request.id())) {
				problems.add("request " + request.id() + ": no item recorded");
			}
		}
		return problems;
	}
}
