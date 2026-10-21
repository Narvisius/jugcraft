package io.github.jimbozoomer.jugcraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Adam-ondi-Ahman in this world (data/jugcraft_adam_ondi_ahman.dat in the Overworld): the valley near the start (its
 * site, and whether it has been chosen, once, on the world's first start; a world with no suitable land near its start
 * keeps none), saved so that it stays where it was chosen whatever later moves the world spawn; and the knolls
 * (world/AltarKnolls): which are built, and for those not yet built, which chunks under them were newly generated.
 */
public final class AdamOndiAhmanState extends SavedData {
	public static final Codec<AdamOndiAhmanState> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.optionalFieldOf("start").forGetter(s -> Optional.ofNullable(s.start)),
			Codec.BOOL.optionalFieldOf("chosen", false).forGetter(s -> s.chosen),
			Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf()).optionalFieldOf("new_chunks", Map.of()).forGetter(AdamOndiAhmanState::newChunksSaved),
			Codec.LONG.listOf().optionalFieldOf("built", List.of()).forGetter(s -> List.copyOf(s.built)))
			.apply(i, AdamOndiAhmanState::new));
	static final SavedDataType<AdamOndiAhmanState> TYPE = new SavedDataType<>(Jugcraft.id("adam_ondi_ahman"), AdamOndiAhmanState::new,
			CODEC, null);

	private @Nullable BlockPos start;
	private boolean chosen;
	/** For each knoll not yet built (by site), the chunks under it that were newly generated. */
	private final Map<Long, Set<Long>> newChunks = new HashMap<>();
	private final Set<Long> built = new HashSet<>();

	AdamOndiAhmanState() {
		this(Optional.empty(), false, Map.of(), List.of());
	}

	AdamOndiAhmanState(Optional<BlockPos> start, boolean chosen, Map<String, List<Long>> newChunks, List<Long> built) {
		this.start = start.orElse(null);
		this.chosen = chosen;
		newChunks.forEach((site, chunks) -> {
			try {
				this.newChunks.put(Long.parseLong(site), new HashSet<>(chunks));
			} catch (NumberFormatException ignored) {
				// A bad key is dropped; that knoll's chunks are no longer new to it, so it is never built over old ones.
			}
		});
		this.built.addAll(built);
	}

	private Map<String, List<Long>> newChunksSaved() {
		Map<String, List<Long>> out = new HashMap<>();
		newChunks.forEach((site, chunks) -> out.put(Long.toString(site), List.copyOf(chunks)));
		return out;
	}

	/** Whether the knoll at a site (AltarKnolls.key) is built. */
	public boolean built(long site) {
		return built.contains(site);
	}

	void markBuilt(long site) {
		built.add(site);
		newChunks.remove(site);
		setDirty();
	}

	/** Whether a chunk under a knoll was newly generated (since this world had knolls). */
	public boolean isNew(long site, long chunk) {
		Set<Long> chunks = newChunks.get(site);
		return chunks != null && chunks.contains(chunk);
	}

	void markNew(long site, long chunk) {
		if (newChunks.computeIfAbsent(site, key -> new HashSet<>()).add(chunk)) {
			setDirty();
		}
	}

	/** The state of this Overworld. */
	public static AdamOndiAhmanState get(ServerLevel overworld) {
		return overworld.getDataStorage().computeIfAbsent(TYPE);
	}

	/** The site of the valley near the start; null if there is none (yet). */
	public @Nullable BlockPos start() {
		return start;
	}

	public boolean chosen() {
		return chosen;
	}

	void choose(@Nullable BlockPos site) {
		start = site;
		chosen = true;
		setDirty();
	}
}
