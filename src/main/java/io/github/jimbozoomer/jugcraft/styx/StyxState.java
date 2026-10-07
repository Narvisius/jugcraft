package io.github.jimbozoomer.jugcraft.styx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** One conservatory and per-player claims, saved in the Overworld. No respawning from an unloaded-entity lookup. */
public final class StyxState extends SavedData {
	public static final Codec<StyxState> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.optionalFieldOf("origin").forGetter(s -> s.origin),
			UUIDUtil.STRING_CODEC.optionalFieldOf("resident").forGetter(s -> s.resident),
			Codec.INT.optionalFieldOf("placed", 0).forGetter(s -> s.placed),
			Codec.intRange(1,StyxConservatory.CURRENT_LAYOUT).optionalFieldOf("layout", 1).forGetter(s -> s.layout),
			UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("claimed", List.of()).forGetter(s -> List.copyOf(s.claimed)),
			Codec.BOOL.optionalFieldOf("town_home", false).forGetter(s -> s.townHome)
			).apply(i, StyxState::new));
	private static final SavedDataType<StyxState> TYPE = new SavedDataType<>(Jugcraft.id("styx_conservatory_v1"), StyxState::new, CODEC, null);
	public Optional<BlockPos> origin;
	public Optional<UUID> resident;
	public int placed;
	public int layout;
	public boolean townHome;
	private final Set<UUID> claimed = new HashSet<>();
	public StyxState() { this(Optional.empty(), Optional.empty(), 0, 1, List.of(), false); }
	private StyxState(Optional<BlockPos> origin, Optional<UUID> resident, int placed, int layout, List<UUID> claimed, boolean townHome) {
		this.townHome=townHome;
		this.layout=layout; this.origin = origin; this.resident = resident; this.placed = Math.max(0, placed); this.claimed.addAll(claimed);
	}
	public static StyxState get(ServerLevel level) { return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE); }
	public boolean claimed(UUID player) { return claimed.contains(player); }
	public void claim(UUID player) { claimed.add(player); setDirty(); }
}
