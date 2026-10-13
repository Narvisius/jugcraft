package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Every player's workers and the last thing each said (roadmap step 17), saved with the world
 * (data/jugcraft_worker_roster.dat in the Overworld), so the {@code workers} command can say where a worker is and what
 * it last reported even when its chunk is unloaded or it is in another dimension, without loading anything. A worker
 * writes its entry when its status changes; one that dies or departs is struck off.
 */
public final class WorkerRoster extends SavedData {
	/** The most workers one player's roster keeps (a familiar, and spirits and constructs). */
	public static final int MAX_PER_PLAYER = 32;

	/** What the roster knows of one worker. */
	public record Entry(String kind, String status, String dimension, long pos) {
		public BlockPos blockPos() {
			return BlockPos.of(pos);
		}
	}

	private static final Codec<Entry> ENTRY = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("kind").forGetter(Entry::kind), Codec.STRING.fieldOf("status").forGetter(Entry::status),
			Codec.STRING.fieldOf("dimension").forGetter(Entry::dimension), Codec.LONG.fieldOf("pos").forGetter(Entry::pos))
			.apply(i, Entry::new));
	/** Versioned, and read player by player: a roster that cannot be read is kept as written (roadmap step 30). */
	public static final Codec<WorkerRoster> CODEC = Saved.versioned("worker_roster",
			Saved.keeping("worker_roster", UUIDUtil.STRING_CODEC, Codec.unboundedMap(UUIDUtil.STRING_CODEC, ENTRY))
					.xmap(WorkerRoster::new, roster -> new Saved.Kept<>(roster.entries, roster.unread)));
	static final SavedDataType<WorkerRoster> TYPE = new SavedDataType<>(Jugcraft.id("worker_roster"), WorkerRoster::new, CODEC, null);

	private final Map<UUID, Map<UUID, Entry>> entries = new HashMap<>();
	/** Rosters saved in a form this version cannot read, kept to be written back unchanged. */
	private final Map<String, Dynamic<?>> unread = new LinkedHashMap<>();

	WorkerRoster() {
	}

	WorkerRoster(Map<UUID, Map<UUID, Entry>> entries) {
		entries.forEach((owner, workers) -> this.entries.put(owner, new TreeMap<>(workers)));
	}

	private WorkerRoster(Saved.Kept<UUID, Map<UUID, Entry>> saved) {
		this(saved.read());
		unread.putAll(saved.unread());
	}

	public static WorkerRoster of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	/** Records what a worker last said; a roster already holding its limit takes no new worker. */
	public void note(UUID owner, UUID worker, String kind, Status status, String dimension, BlockPos pos) {
		Map<UUID, Entry> workers = entries.computeIfAbsent(owner, unused -> new TreeMap<>());
		if (!workers.containsKey(worker) && workers.size() >= MAX_PER_PLAYER) {
			return;
		}
		workers.put(worker, new Entry(kind, status.id, dimension, pos.asLong()));
		setDirty();
	}

	public void remove(UUID owner, UUID worker) {
		Map<UUID, Entry> workers = entries.get(owner);
		if (workers != null && workers.remove(worker) != null) {
			setDirty();
		}
	}

	public Map<UUID, Entry> workers(UUID owner) {
		return entries.getOrDefault(owner, Map.of());
	}

	/** The player's familiar, if the roster has one alive. */
	public @Nullable UUID familiar(UUID owner) {
		for (Map.Entry<UUID, Entry> worker : workers(owner).entrySet()) {
			if (worker.getValue().kind().equals("familiar")) {
				return worker.getKey();
			}
		}
		return null;
	}
}
