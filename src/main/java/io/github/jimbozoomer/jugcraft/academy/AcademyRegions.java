package io.github.jimbozoomer.jugcraft.academy;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Per-dimension persistence means bridge ambience survives the central chunk unloading. */
public final class AcademyRegions extends SavedData {
	public static final int MAX_SITES = 32;
	public static final Codec<AcademyRegions> CODEC = AcademySite.CODEC.listOf(0, MAX_SITES)
			.xmap(AcademyRegions::new, state -> List.copyOf(state.sites));
	private static final SavedDataType<AcademyRegions> TYPE = new SavedDataType<>(Jugcraft.id("academy_regions"),
			() -> new AcademyRegions(List.of()), io.github.jimbozoomer.jugcraft.concordance.Saved.versioned("academy_regions", CODEC), null);
	private final List<AcademySite> sites;
	private AcademyRegions(List<AcademySite> sites) { this.sites = new ArrayList<>(sites); }
	public static AcademyRegions get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
	public void remember(AcademySite site) {
		if (sites.contains(site)) return;
		sites.removeIf(old -> old.anchor().equals(site.anchor()));
		if (sites.size() < MAX_SITES) { sites.add(site); setDirty(); }
	}
	public List<AcademySite> nearby(ServerPlayer player) {
		// Only inspect already-loaded anchors. This never loads or tickets a chunk.
		if (sites.removeIf(site -> player.level().hasChunkAt(site.anchor())
				&& !player.level().getBlockState(site.anchor()).is(JugcraftAcademy.WARD))) setDirty();
		return sites.stream().filter(s -> s.anchor().distSqr(player.blockPosition()) < 256*256)
				.sorted(Comparator.comparingDouble(s -> s.anchor().distSqr(player.blockPosition())))
				.limit(16).toList();
	}
}
