package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Concordance Chunk D, D-02 (D-S6): each holder's last {@value Pacts#HISTORY} departures (an agreement ended by its holder,
 * or released by its place broken), saved with the world (data/jugcraft/pact_history.dat in the Overworld), read holder by
 * holder: a list that cannot be read is kept as written (roadmap step 30). A separate record because a non-UUID key in
 * the Bound Wills would be kept as unread and never updated.
 */
public final class PactHistory extends SavedData {
	private static final Codec<Pacts.Departure> DEPARTURE = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("record").forGetter(Pacts.Departure::record), Codec.STRING.fieldOf("agreement").forGetter(Pacts.Departure::agreement),
			Codec.STRING.fieldOf("counterpart").forGetter(Pacts.Departure::counterpart), Codec.STRING.fieldOf("reason").forGetter(Pacts.Departure::reason),
			Codec.LONG.fieldOf("time").forGetter(Pacts.Departure::time),
			CourierLedger.PLACE.optionalFieldOf("place").forGetter(d -> Optional.ofNullable(d.place())))
			.apply(i, (record, agreement, counterpart, reason, time, place) -> new Pacts.Departure(record, agreement, counterpart, reason, time,
					place.orElse(null))));
	/** Versioned, and read holder by holder (roadmap step 30). */
	public static final Codec<PactHistory> CODEC = Saved.versioned("pact_history",
			Saved.keeping("pact_history", UUIDUtil.STRING_CODEC, DEPARTURE.listOf())
					.xmap(PactHistory::new, history -> new Saved.Kept<>(history.history, history.unread)));
	static final SavedDataType<PactHistory> TYPE = new SavedDataType<>(Jugcraft.id("pact_history"), PactHistory::new, CODEC, null);

	private final Map<UUID, List<Pacts.Departure>> history = new LinkedHashMap<>();
	/** Lists saved in a form this version cannot read, kept to be written back unchanged. */
	private final Map<String, Dynamic<?>> unread = new LinkedHashMap<>();

	PactHistory() {
	}

	private PactHistory(Saved.Kept<UUID, List<Pacts.Departure>> kept) {
		kept.read().forEach((holder, list) -> history.put(holder, Pacts.bounded(list)));
		unread.putAll(kept.unread());
	}

	public static PactHistory of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** {@code holder}'s departures, oldest first. */
	public List<Pacts.Departure> history(UUID holder) {
		return history.getOrDefault(holder, List.of());
	}

	public void add(UUID holder, Pacts.Departure departure) {
		history.put(holder, Pacts.departed(history(holder), departure));
		setDirty();
	}
}
