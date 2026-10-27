package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.worker.Agreement;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-06 (G11): how an agreement kept at a block stands, and Stolas's pact, as Jade shows them. Read only
 * (AgreementHolder.peek: nothing verified, claimed, forgotten, written or called); naming no player (no holder, owner or
 * id); nothing of the escrow but when the next offering falls due. Built on the server for the one block or entity looked
 * at; compat/jade's providers send it and the client words it (tooltip.jugcraft.concordance.jade.pact.*). No Jade type here.
 */
public final class PactReading {
	/** Every key a reading may carry (tools/concordance_pacts.py JADE_KEYS). */
	public static final List<String> KEYS = List.of("standing", "visiting", "departing", "counterpart", "service", "state", "due_days",
			"grace_days", "waiting");
	public static final List<String> STANDINGS = List.of("none", "kept", "copy", "unreadable", "disabled");
	public static final List<String> STATES = List.of("active", "suspended", "upkeep_due", "upkeep_held");
	/** Waiting reason of a presence struck off its holder's roster (D-S18): its holder calls it back near its place. */
	public static final String LOST = "lost";

	private PactReading() {
	}

	/**
	 * A block keeping agreements (the pact perch, the Spirit Anchor) at game time {@code now}. A perch keeping nothing says
	 * "visiting" only while its own visitor waits there (critique 7: never the presence of a record ended elsewhere).
	 */
	public static CompoundTag holder(ServerLevel level, AgreementHolder.Holds holds, long now) {
		CompoundTag reading = new CompoundTag();
		if (!Workers.enabled()) {
			reading.putString("standing", "disabled");
			return reading;
		}
		AgreementHolder holder = holds.holder();
		AgreementHolder.Standing standing = holder.peek(level);
		reading.putString("standing", switch (standing) {
			case NONE -> "none";
			case VALID -> "kept";
			case ELSEWHERE -> "copy";
			case UNREADABLE -> "unreadable";
		});
		if (standing == AgreementHolder.Standing.NONE && holds instanceof PactPerchBlockEntity perch && perch.visitor() != null) {
			StolasEntity visiting = perch.stolas(level);
			if (visiting != null && visiting.getUUID().equals(perch.visitor())) {
				reading.putBoolean("visiting", true);
			}
		}
		if (standing != AgreementHolder.Standing.VALID) {
			return reading;
		}
		BoundWill record = holder.record(level);
		reading.putString("counterpart", Pacts.counterpart(record.counterpart()));
		terms(reading, record, holder.agreement(), now);
		String waiting = waiting(level, holder, record);
		if (waiting != null) {
			reading.putString("waiting", waiting);
		}
		return reading;
	}

	/** Stolas: departing, visiting, or his pact as his perch keeps it (his own status is Jade's first line, so no "waiting"). */
	public static CompoundTag stolas(ServerLevel level, StolasEntity stolas, long now) {
		CompoundTag reading = new CompoundTag();
		if (!Workers.enabled()) {
			reading.putString("standing", "disabled");
			return reading;
		}
		if (stolas.gone()) {
			reading.putString("standing", "none");
			reading.putBoolean("departing", true);
			return reading;
		}
		BlockPos at = stolas.perch();
		PactPerchBlockEntity home = at != null && level.isLoaded(at) && level.getBlockEntity(at) instanceof PactPerchBlockEntity found ? found : null;
		if (home == null || stolas.owner() == null) {
			reading.putString("standing", "none");
			if (home != null && stolas.getUUID().equals(home.visitor())) {
				reading.putBoolean("visiting", true);
			}
			return reading;
		}
		AgreementHolder.Standing standing = home.holder().peek(level);
		BoundWill record = standing == AgreementHolder.Standing.VALID ? home.holder().record(level) : null;
		if (record == null || !stolas.getUUID().equals(presence(record, home.holder()))) {
			reading.putString("standing", standing == AgreementHolder.Standing.UNREADABLE ? "unreadable" : "none");
			return reading;
		}
		reading.putString("standing", "kept");
		terms(reading, record, home.holder().agreement(), now);
		return reading;
	}

	/** Its presence: the one its Bound Will names, else the block's (a record from before Chunk D, not yet claimed). */
	private static @Nullable UUID presence(BoundWill record, AgreementHolder holder) {
		return record.pact() != null && record.pact().presence() != null ? record.pact().presence() : holder.spirit();
	}

	private static void terms(CompoundTag reading, BoundWill record, Agreement agreement, long now) {
		BoundWill.Pact pact = record.pact();
		if (pact == null) {
			// A record from before Chunk D not yet claimed (D-P3): its block's own terms say its work and whether it is held.
			reading.putString("service", agreement.work());
			reading.putString("state", agreement.suspended() ? Status.SUSPENDED.id : "active");
			return;
		}
		if (pact.service() != null) {
			reading.putString("service", pact.service().work());
		}
		Status status = Pacts.status(record);
		reading.putString("state", status == null ? "active" : status.id);
		BoundWill.Upkeep upkeep = pact.upkeep();
		if (upkeep != null && !Pacts.held(record)) {
			if (upkeep.warned()) {
				reading.putInt("grace_days", (int) AgreementHolder.days(upkeep.graceUntil() - now));
			} else {
				reading.putInt("due_days", (int) AgreementHolder.days(upkeep.due() - now));
			}
		}
	}

	/**
	 * Why its presence waits: its own status if loaded, as its roster last noted it if not, LOST if struck off; none while it
	 * serves or idles at its place, or while the state line already says it.
	 */
	private static @Nullable String waiting(ServerLevel level, AgreementHolder holder, BoundWill record) {
		UUID spirit = presence(record, holder);
		if (spirit == null) {
			return LOST;
		}
		if (level.getEntity(spirit) instanceof WorkerEntity<?> worker && !worker.isRemoved()) {
			return reason(worker.status());
		}
		WorkerRoster.Entry entry = WorkerRoster.of(level.getServer()).workers(record.holder()).get(spirit);
		if (entry == null) {
			return LOST;
		}
		Status noted = Status.fromId(entry.status());
		return noted == null ? null : reason(noted);
	}

	private static @Nullable String reason(Status status) {
		return status.active || status == Status.IDLE || status == Status.SUSPENDED || status == Status.UPKEEP_DUE
				|| status == Status.UPKEEP_HELD ? null : status.id;
	}
}
