package io.github.jimbozoomer.jugcraft.concordance.spire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.wonder.SpireState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Every Concord Spire's progress (roadmap step 25), kept by the world rather than by its heart block: one place, saved
 * with the world, so breaking a heart, an unloaded chunk or a restart loses nothing. Keyed by where the heart stands
 * ({@link ConcordSpire#id}).
 */
public final class SpireRecord extends SavedData {
	static final Codec<SpireState> STATE_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("id").forGetter(SpireState::id), Codec.STRING.fieldOf("wonder").forGetter(SpireState::wonder),
			Codec.STRING.fieldOf("configuration").forGetter(SpireState::configuration), UUIDUtil.CODEC.fieldOf("keeper").forGetter(SpireState::keeper),
			Codec.BOOL.fieldOf("communal").forGetter(SpireState::communal), Codec.INT.fieldOf("phase").forGetter(SpireState::phase),
			Codec.LONG.fieldOf("phase_began").forGetter(SpireState::phaseBegan), Codec.INT.fieldOf("practiced").forGetter(SpireState::practiced),
			Codec.BOOL.fieldOf("rite").forGetter(SpireState::rite), Codec.INT.fieldOf("sustained").forGetter(SpireState::sustained),
			Codec.LONG.fieldOf("next_day").forGetter(SpireState::nextDay), Codec.LONG.fieldOf("last_attended").forGetter(SpireState::lastAttended),
			Codec.BOOL.fieldOf("supplied").forGetter(SpireState::supplied), Codec.LONG.fieldOf("founded").forGetter(SpireState::founded),
			UUIDUtil.CODEC.listOf().xmap(TreeSet::new, List::copyOf).fieldOf("contributors").forGetter(state -> new TreeSet<>(state.contributors())))
			.apply(i, SpireState::new));

	/** Versioned, and read spire by spire: one that cannot be read is kept as written (roadmap step 30). */
	public static final Codec<SpireRecord> CODEC = Saved.versioned("spires",
			Saved.keeping("spires", Codec.STRING, STATE_CODEC).xmap(SpireRecord::new, SpireRecord::saved));
	static final SavedDataType<SpireRecord> TYPE = new SavedDataType<>(Jugcraft.id("spires"), SpireRecord::new, CODEC, null);

	private final Map<String, SpireState> spires = new LinkedHashMap<>();
	/** Spires saved in a form this version cannot read, kept to be written back unchanged. */
	private final Map<String, Dynamic<?>> unread = new LinkedHashMap<>();

	SpireRecord() {
	}

	private SpireRecord(Saved.Kept<String, SpireState> saved) {
		spires.putAll(saved.read());
		unread.putAll(saved.unread());
	}

	private Saved.Kept<String, SpireState> saved() {
		return new Saved.Kept<>(spires, unread);
	}

	public static SpireRecord of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public @Nullable SpireState spire(String id) {
		return spires.get(id);
	}

	public void put(SpireState state) {
		spires.put(state.id(), state);
		setDirty();
	}

	/** Forgets a spire (its keeper abandoned it): the blocks they built stay where they are. */
	public void remove(String id) {
		if (spires.remove(id) != null) {
			setDirty();
		}
	}

	public List<SpireState> all() {
		return new ArrayList<>(spires.values());
	}

	/** The spires {@code player} keeps. */
	public List<SpireState> keptBy(UUID player) {
		return spires.values().stream().filter(state -> state.keeper().equals(player)).toList();
	}
}
