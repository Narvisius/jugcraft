package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWillLedger;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Every sealed agreement's Bound Will record (roadmap steps 7 and 17), saved with the world
 * (data/jugcraft/bound_wills.dat in the Overworld): an agreement with an identity, held by one player, never merged,
 * split or counted. Sealing adds one; its holder's end, or its place broken, removes it. Since Concordance Chunk D (D-02,
 * D-S6, D-S9) an entry also carries its record's pact and its escrow: the offerings paid ahead for its upkeep, exact
 * stacks kept here and never in a block, so no hopper, pipe, explosion or copied block reaches them; releasing a record
 * hands its escrow back once. Both are optional keys, and defaults are not written, so a save from before reads and
 * writes back unchanged and the format stays at version 1.
 */
public final class BoundWills extends SavedData {
	private static final Codec<BoundWill.Hold> HOLD = Codec.STRING.comapFlatMap(id -> {
		BoundWill.Hold hold = BoundWill.Hold.fromId(id);
		return hold == null ? DataResult.error(() -> "unknown hold " + id) : DataResult.success(hold);
	}, hold -> hold.id);
	private static final Codec<String> BENEFICIARIES = Codec.STRING.comapFlatMap(
			id -> Pacts.beneficiaries(id) ? DataResult.success(id) : DataResult.error(() -> "unknown beneficiaries " + id), id -> id);
	private static final Codec<BoundWill.Upkeep> UPKEEP = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("offering").forGetter(BoundWill.Upkeep::offering), Codec.INT.fieldOf("count").forGetter(BoundWill.Upkeep::count),
			Codec.LONG.fieldOf("period").forGetter(BoundWill.Upkeep::period), Codec.LONG.fieldOf("grace").forGetter(BoundWill.Upkeep::grace),
			Codec.INT.fieldOf("cap").forGetter(BoundWill.Upkeep::cap), Codec.LONG.fieldOf("due").forGetter(BoundWill.Upkeep::due),
			Codec.LONG.optionalFieldOf("grace_until").forGetter(u -> unless(u.graceUntil(), BoundWill.Upkeep.NOT_WARNED)),
			Codec.LONG.fieldOf("looked_at").forGetter(BoundWill.Upkeep::lookedAt))
			.apply(i, (offering, count, period, grace, cap, due, graceUntil, lookedAt) -> new BoundWill.Upkeep(offering, count, period, grace, cap,
					due, graceUntil.orElse(BoundWill.Upkeep.NOT_WARNED), lookedAt)));
	private static final Codec<BoundWill.Service> SERVICE = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("work").forGetter(BoundWill.Service::work), Codec.INT.fieldOf("radius").forGetter(BoundWill.Service::radius),
			Codec.INT.fieldOf("from").forGetter(BoundWill.Service::from), Codec.INT.fieldOf("to").forGetter(BoundWill.Service::to),
			Codec.INT.fieldOf("quota").forGetter(BoundWill.Service::quota)).apply(i, BoundWill.Service::new));
	private static final Codec<BoundWill.Pact> PACT = RecordCodecBuilder.create(i -> i.group(
			CourierLedger.PLACE.optionalFieldOf("place").forGetter(p -> Optional.ofNullable(p.place())),
			HOLD.optionalFieldOf("hold").forGetter(p -> unless(p.hold(), BoundWill.Hold.NONE)),
			Codec.LONG.optionalFieldOf("hold_since").forGetter(p -> unless(p.holdSince(), 0L)),
			BENEFICIARIES.optionalFieldOf("beneficiaries").forGetter(p -> unless(p.beneficiaries(), Pacts.PERSONAL)),
			UUIDUtil.CODEC.optionalFieldOf("presence").forGetter(p -> Optional.ofNullable(p.presence())),
			UPKEEP.optionalFieldOf("upkeep").forGetter(p -> Optional.ofNullable(p.upkeep())),
			SERVICE.optionalFieldOf("service").forGetter(p -> Optional.ofNullable(p.service())),
			Codec.LONG.optionalFieldOf("window").forGetter(p -> unless(p.window(), BoundWill.Pact.NO_WINDOW)),
			Codec.INT.optionalFieldOf("done_today").forGetter(p -> unless(p.doneToday(), 0)))
			.apply(i, (place, hold, since, beneficiaries, presence, upkeep, service, window, done) -> new BoundWill.Pact(place.orElse(null),
					hold.orElse(BoundWill.Hold.NONE), since.orElse(0L), beneficiaries.orElse(Pacts.PERSONAL), presence.orElse(null),
					upkeep.orElse(null), service.orElse(null), window.orElse(BoundWill.Pact.NO_WINDOW), done.orElse(0))));

	/** One entry as saved: the record and its escrow. */
	private record Entry(BoundWill will, List<ItemStack> escrow) {
	}

	private static final Codec<Entry> ENTRY = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(e -> e.will().id()), Codec.STRING.fieldOf("agreement").forGetter(e -> e.will().agreement()),
			Codec.STRING.fieldOf("counterpart").forGetter(e -> e.will().counterpart()),
			UUIDUtil.CODEC.fieldOf("holder").forGetter(e -> e.will().holder()), Codec.LONG.fieldOf("sealed_at").forGetter(e -> e.will().sealedAt()),
			Codec.BOOL.fieldOf("transferable").forGetter(e -> e.will().transferable()),
			PACT.optionalFieldOf("pact").forGetter(e -> Optional.ofNullable(e.will().pact())),
			ItemStack.CODEC.listOf().optionalFieldOf("escrow").forGetter(e -> unless(e.escrow(), List.of())))
			.apply(i, (id, agreement, counterpart, holder, sealedAt, transferable, pact, escrow) -> new Entry(
					new BoundWill(id, agreement, counterpart, holder, sealedAt, transferable, pact.orElse(null)), escrow.orElse(List.of()))));
	/** Versioned, and read will by will: one that cannot be read is kept as written (roadmap step 30). Still version 1. */
	public static final Codec<BoundWills> CODEC = Saved.versioned("bound_wills", Saved.keeping("bound_wills", UUIDUtil.STRING_CODEC, ENTRY)
			.xmap(BoundWills::new, BoundWills::saved));
	static final SavedDataType<BoundWills> TYPE = new SavedDataType<>(Jugcraft.id("bound_wills"), BoundWills::new, CODEC, null);

	/** A released record and its escrow, handed back once. */
	public record Released(BoundWillLedger.Outcome outcome, @Nullable BoundWill record, List<ItemStack> escrow) {
	}

	private BoundWillLedger ledger;
	/** Each record's escrow (D-02): offerings paid ahead, exact stacks in the order given. */
	private final Map<UUID, List<ItemStack>> escrow = new LinkedHashMap<>();
	/** Bound Wills saved in a form this version cannot read, kept to be written back unchanged. */
	private final Map<String, Dynamic<?>> unread = new LinkedHashMap<>();

	BoundWills() {
		this(BoundWillLedger.EMPTY);
	}

	BoundWills(BoundWillLedger ledger) {
		this.ledger = ledger;
	}

	private BoundWills(Saved.Kept<UUID, Entry> kept) {
		Map<UUID, BoundWill> records = new LinkedHashMap<>();
		kept.read().forEach((id, entry) -> {
			records.put(id, entry.will());
			List<ItemStack> held = new ArrayList<>();
			entry.escrow().stream().filter(stack -> !stack.isEmpty()).forEach(stack -> held.add(stack.copy()));
			if (!held.isEmpty()) {
				escrow.put(id, held);
			}
		});
		ledger = new BoundWillLedger(records);
		unread.putAll(kept.unread());
	}

	private Saved.Kept<UUID, Entry> saved() {
		Map<UUID, Entry> out = new LinkedHashMap<>();
		ledger.records().forEach((id, will) -> out.put(id, new Entry(will, escrow.getOrDefault(id, List.of()))));
		return new Saved.Kept<>(out, unread);
	}

	private static <T> Optional<T> unless(T value, T absent) {
		return value.equals(absent) ? Optional.empty() : Optional.of(value);
	}

	public static BoundWills of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public BoundWillLedger ledger() {
		return ledger;
	}

	/** Whether record {@code id} is saved in a form this version cannot read (kept as written). */
	public boolean unreadable(UUID id) {
		return unread.containsKey(id.toString());
	}

	public BoundWillLedger.Change seal(BoundWill record) {
		BoundWillLedger.Change change = ledger.seal(record);
		if (change.outcome() == BoundWillLedger.Outcome.DONE) {
			ledger = change.ledger();
			setDirty();
		}
		return change;
	}

	/** A record's next state: the same id and holder only (BoundWillLedger.update). */
	public BoundWillLedger.Change update(BoundWill next) {
		BoundWillLedger.Change change = ledger.update(next);
		if (change.outcome() == BoundWillLedger.Outcome.DONE) {
			ledger = change.ledger();
			setDirty();
		}
		return change;
	}

	/** Ends a record; only its holder can. Its escrow goes with it and is handed to the caller, once. */
	public Released release(UUID id, UUID holder) {
		BoundWill record = ledger.get(id);
		BoundWillLedger.Change change = ledger.release(id, holder);
		if (change.outcome() != BoundWillLedger.Outcome.DONE) {
			return new Released(change.outcome(), null, List.of());
		}
		ledger = change.ledger();
		List<ItemStack> held = escrow.remove(id);
		setDirty();
		return new Released(BoundWillLedger.Outcome.DONE, record, held == null ? List.of() : List.copyOf(held));
	}

	/** A copy of record {@code id}'s escrow. */
	public List<ItemStack> escrow(UUID id) {
		return escrow.getOrDefault(id, List.of()).stream().map(ItemStack::copy).toList();
	}

	/** Whole periods of {@code count} matching offerings in record {@code id}'s escrow. */
	public int periods(UUID id, Predicate<ItemStack> offering, int count) {
		return count(escrow.getOrDefault(id, List.of()), offering) / Math.max(1, count);
	}

	/** Puts what matches and fits (at most {@code most} matching in all) of {@code stack} into the escrow; the rest stays in {@code stack}. */
	public ItemStack deposit(UUID id, ItemStack stack, Predicate<ItemStack> offering, int most) {
		if (!ledger.records().containsKey(id) || stack.isEmpty() || !offering.test(stack)) {
			return stack;
		}
		int room = most - count(escrow.getOrDefault(id, List.of()), offering);
		if (room <= 0) {
			return stack;
		}
		ItemStack taken = stack.split(Math.min(room, stack.getCount()));
		List<ItemStack> held = escrow.computeIfAbsent(id, unused -> new ArrayList<>());
		for (ItemStack slot : held) {
			if (!taken.isEmpty() && ItemStack.isSameItemSameComponents(slot, taken)) {
				int moved = Math.min(taken.getCount(), slot.getMaxStackSize() - slot.getCount());
				slot.grow(moved);
				taken.shrink(moved);
			}
		}
		if (!taken.isEmpty()) {
			held.add(taken);
		}
		setDirty();
		return stack;
	}

	/** Takes {@code count} matching offerings, oldest first; false, taking nothing, if fewer are held. */
	public boolean take(UUID id, Predicate<ItemStack> offering, int count) {
		List<ItemStack> held = escrow.get(id);
		if (held == null || count(held, offering) < count) {
			return false;
		}
		int left = count;
		for (Iterator<ItemStack> it = held.iterator(); it.hasNext() && left > 0;) {
			ItemStack slot = it.next();
			if (offering.test(slot)) {
				int used = Math.min(left, slot.getCount());
				slot.shrink(used);
				left -= used;
				if (slot.isEmpty()) {
					it.remove();
				}
			}
		}
		if (held.isEmpty()) {
			escrow.remove(id);
		}
		setDirty();
		return true;
	}

	private static int count(List<ItemStack> stacks, Predicate<ItemStack> offering) {
		return stacks.stream().filter(offering).mapToInt(ItemStack::getCount).sum();
	}

	/** Pacts.problems, plus an escrow without its record or upkeep, an empty stack, or more than its cap (none expected). */
	public List<String> problems() {
		List<String> problems = new ArrayList<>(Pacts.problems(ledger));
		escrow.forEach((id, stacks) -> {
			BoundWill record = ledger.get(id);
			BoundWill.Upkeep upkeep = record == null || record.pact() == null ? null : record.pact().upkeep();
			if (upkeep == null) {
				problems.add("escrow " + id + ": no record with upkeep");
				return;
			}
			int total = 0;
			for (ItemStack stack : stacks) {
				if (stack.isEmpty()) {
					problems.add("escrow " + id + ": an empty stack");
				}
				total += stack.getCount();
			}
			if (total > upkeep.cap() * upkeep.count()) {
				problems.add("escrow " + id + ": holds " + total + ", over its cap " + upkeep.cap() * upkeep.count());
			}
		});
		return problems;
	}
}
