package io.github.jimbozoomer.jugcraft.concordance.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bound Will records by id. Immutable: each operation returns the ledger after it with its outcome, and a refused
 * operation returns the ledger unchanged. A holder keeps at most {@value #MAX_PER_HOLDER} records; more are refused,
 * never dropped.
 */
public record BoundWillLedger(Map<UUID, BoundWill> records) {
	public static final int MAX_PER_HOLDER = 32;
	public static final BoundWillLedger EMPTY = new BoundWillLedger(Map.of());

	public BoundWillLedger {
		records = Collections.unmodifiableMap(new LinkedHashMap<>(records));
	}

	public enum Outcome {
		DONE, DUPLICATE, UNKNOWN, NOT_HOLDER, NOT_TRANSFERABLE, HOLDER_FULL
	}

	public record Change(BoundWillLedger ledger, Outcome outcome) {
	}

	public List<BoundWill> heldBy(UUID holder) {
		List<BoundWill> held = new ArrayList<>();
		for (BoundWill record : records.values()) {
			if (record.holder().equals(holder)) {
				held.add(record);
			}
		}
		return held;
	}

	private BoundWillLedger with(BoundWill record) {
		Map<UUID, BoundWill> copy = new LinkedHashMap<>(records);
		copy.put(record.id(), record);
		return new BoundWillLedger(copy);
	}

	public Change seal(BoundWill record) {
		if (records.containsKey(record.id())) {
			return new Change(this, Outcome.DUPLICATE);
		}
		if (heldBy(record.holder()).size() >= MAX_PER_HOLDER) {
			return new Change(this, Outcome.HOLDER_FULL);
		}
		return new Change(with(record), Outcome.DONE);
	}

	/** Hands a record from its holder to another player, if it is theirs and its terms allow it. */
	public Change transfer(UUID id, UUID from, UUID to) {
		BoundWill record = records.get(id);
		if (record == null) {
			return new Change(this, Outcome.UNKNOWN);
		}
		if (!record.holder().equals(from)) {
			return new Change(this, Outcome.NOT_HOLDER);
		}
		if (!record.transferable()) {
			return new Change(this, Outcome.NOT_TRANSFERABLE);
		}
		if (heldBy(to).size() >= MAX_PER_HOLDER) {
			return new Change(this, Outcome.HOLDER_FULL);
		}
		return new Change(with(record.withHolder(to)), Outcome.DONE);
	}

	/** Ends an agreement; only its holder can. */
	public Change release(UUID id, UUID holder) {
		BoundWill record = records.get(id);
		if (record == null) {
			return new Change(this, Outcome.UNKNOWN);
		}
		if (!record.holder().equals(holder)) {
			return new Change(this, Outcome.NOT_HOLDER);
		}
		Map<UUID, BoundWill> copy = new LinkedHashMap<>(records);
		copy.remove(id);
		return new Change(new BoundWillLedger(copy), Outcome.DONE);
	}
}
