package io.github.jimbozoomer.jugcraft.town;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * The town in this world (data/jugcraft_town.dat in the Overworld): where its corner is (null until it is placed), the
 * chunks already built, the theme each decor site shows, the townsfolk spawned for each place, and the players already
 * welcomed. Everything here is the server's.
 */
public final class TownState extends SavedData {
	public static final Codec<TownState> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.optionalFieldOf("origin").forGetter(s -> Optional.ofNullable(s.origin)),
			Codec.LONG.listOf().optionalFieldOf("built", List.of()).forGetter(s -> List.copyOf(s.built)),
			Codec.STRING.listOf().optionalFieldOf("site_themes", List.of()).forGetter(s -> List.copyOf(s.siteThemes)),
			Codec.unboundedMap(Codec.STRING, UUIDUtil.STRING_CODEC).optionalFieldOf("townsfolk", Map.of()).forGetter(TownState::townsfolkSaved),
			UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("welcomed", List.of()).forGetter(s -> List.copyOf(s.welcomed)),
			Codec.STRING.optionalFieldOf("theme", "").forGetter(s -> s.theme),
			Codec.LONG.optionalFieldOf("theme_since", 0L).forGetter(s -> s.themeSince),
			Codec.BOOL.optionalFieldOf("styx_district", false).forGetter(s -> s.styxDistrict))
			.apply(i, TownState::new));
	static final SavedDataType<TownState> TYPE = new SavedDataType<>(Jugcraft.id("town"), TownState::new, CODEC, null);

	private @Nullable BlockPos origin;
	private final Set<Long> built = new HashSet<>();
	private final List<String> siteThemes = new ArrayList<>();
	private final Map<Integer, UUID> townsfolk = new HashMap<>();
	private final Set<UUID> welcomed = new HashSet<>();
	private String theme;
	private long themeSince;
	private boolean styxDistrict;

	TownState() {
		this(Optional.empty(), List.of(), List.of(), Map.of(), List.of(), "", 0L, false);
	}

	TownState(Optional<BlockPos> origin, List<Long> built, List<String> siteThemes, Map<String, UUID> townsfolk,
			List<UUID> welcomed, String theme, long themeSince, boolean styxDistrict) {
		this.styxDistrict = styxDistrict;
		this.origin = origin.orElse(null);
		this.built.addAll(built);
		this.siteThemes.addAll(siteThemes);
		townsfolk.forEach((spot, id) -> {
			try {
				this.townsfolk.put(Integer.parseInt(spot), id);
			} catch (NumberFormatException ignored) {
				// A bad key is dropped; that place gets a new townsperson.
			}
		});
		this.welcomed.addAll(welcomed);
		this.theme = theme;
		this.themeSince = themeSince;
	}

	public static TownState get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public static TownState get(ServerLevel level) {
		return get(level.getServer());
	}

	private Map<String, UUID> townsfolkSaved() {
		Map<String, UUID> out = new HashMap<>();
		townsfolk.forEach((spot, id) -> out.put(Integer.toString(spot), id));
		return out;
	}

	/** The world position of the town's (0, 0, 0): its north-west corner at ground height; null if not placed. */
	public @Nullable BlockPos origin() {
		return origin;
	}

	public void place(BlockPos origin) {
		place(origin, false);
	}

	/** Only newly planned towns reserve the east district; missing fields in old saves stay false. */
	public void place(BlockPos origin, boolean styxDistrict) {
		this.styxDistrict = styxDistrict;
		this.origin = origin.immutable();
		built.clear();
		siteThemes.clear();
		townsfolk.clear();
		setDirty();
	}

	/** Forgets the town (for tests and `/jugcraft town forget`): its blocks stay, its protection and NPC care end. */
	public void forget() {
		styxDistrict = false;
		origin = null;
		built.clear();
		siteThemes.clear();
		townsfolk.clear();
		setDirty();
	}

	public boolean built(long chunk) {
		return built.contains(chunk);
	}

	public boolean styxDistrict() {
		return styxDistrict;
	}

	public void markBuilt(long chunk) {
		if (built.add(chunk)) {
			setDirty();
		}
	}

	public String siteTheme(int site) {
		return site < siteThemes.size() ? siteThemes.get(site) : "";
	}

	public void setSiteTheme(int site, String theme) {
		while (siteThemes.size() <= site) {
			siteThemes.add("");
		}
		siteThemes.set(site, theme);
		setDirty();
	}

	public @Nullable UUID townsperson(int spot) {
		return townsfolk.get(spot);
	}

	public void setTownsperson(int spot, UUID id) {
		townsfolk.put(spot, id);
		setDirty();
	}

	/** Records a player's first visit; true the first time. */
	public boolean welcome(UUID player) {
		if (welcomed.add(player)) {
			setDirty();
			return true;
		}
		return false;
	}

	public String theme() {
		return theme;
	}

	public long themeSince() {
		return themeSince;
	}

	public void setTheme(String theme, long gameTime) {
		this.theme = theme;
		this.themeSince = gameTime;
		setDirty();
	}
}
