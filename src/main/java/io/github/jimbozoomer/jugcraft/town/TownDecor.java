package io.github.jimbozoomer.jugcraft.town;

import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The town's seasonal decoration. Its theme is the running event (December, then Halloween, then the Harvest Feast)
 * or else the season, from the server's own calendar ({@link JugcraftSeasons}); with seasons off it is summer's.
 * When the theme changes, every decor site (window boxes, banners, awnings, planters, lamps, flags and the square's
 * centrepiece; tools/town_decor.py) is due a change. The town's decorators walk round and change the sites near them
 * one by one ({@link Townsfolk}); a site in a loaded chunk that no decorator has reached after {@link #GRACE} ticks is
 * changed directly, a few a tick, and a chunk that loads with stale sites gets the current ones at once (no one was
 * there to watch). Earned items and the town's blocks are never touched: only the sites' own blocks change.
 */
public final class TownDecor {
	/** How often (ticks) the theme is checked. */
	public static final int CHECK_TICKS = 100;
	/** How long decorators have before the rest is changed directly (5 minutes). */
	public static final int GRACE = 6000;
	public static final int DIRECT_PER_TICK = 4;
	private static final Map<String, BlockState> PARSED = new HashMap<>();
	/** Sites a decorator is walking to: site -> decorator. */
	private static final Map<Integer, UUID> CLAIMS = new HashMap<>();
	private static @Nullable String forced;

	private TownDecor() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(TownDecor::tick);
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
			if (level == level.getServer().overworld()) {
				TownState state = TownState.get(level);
				if (state.origin() != null && state.built(chunk.getPos().pack())) {
					chunkBuilt(level, chunk.getPos());
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			CLAIMS.clear();
			forced = null;
		});
	}

	/** Today's theme. */
	public static String theme() {
		if (forced != null) {
			return forced;
		}
		if (JugcraftSeasons.isActive(SeasonCalendar.Event.DECEMBER)) {
			return "december";
		}
		if (HalloweenSeason.active()) {
			return "halloween";
		}
		if (JugcraftSeasons.isActive(SeasonCalendar.Event.HARVEST_FEAST)) {
			return "harvest";
		}
		String season = SeasonCalendar.seasonName(JugcraftSeasons.today());
		return season.equals("off") ? "summer" : season;
	}

	/** Holds a theme until the server stops (null: follow the calendar again), for operators and tests. */
	public static void force(@Nullable String theme) {
		forced = theme;
	}

	private static void tick(MinecraftServer server) {
		ServerLevel level = server.overworld();
		TownState state = TownState.get(level);
		BlockPos origin = state.origin();
		if (origin == null) {
			return;
		}
		long now = level.getGameTime();
		if (server.getTickCount() % CHECK_TICKS == 0) {
			String theme = theme();
			if (!theme.equals(state.theme())) {
				boolean first = state.theme().isEmpty();
				state.setTheme(theme, now);
				CLAIMS.clear();
				if (!first) {
					announce(level, theme);
				}
			}
			welcome(level, state);
		}
		if (now - state.themeSince() >= GRACE && server.getTickCount() % 20 == 0) {
			int done = 0;
			for (TownData.Site site : TownData.get().sites) {
				if (done >= DIRECT_PER_TICK) {
					break;
				}
				if (!state.siteTheme(site.index()).equals(state.theme()) && !CLAIMS.containsKey(site.index()) && loaded(level, origin, site)) {
					apply(level, site.index());
					done++;
				}
			}
		}
	}

	/** Sets every stale site in a chunk that was just built or loaded. */
	public static void chunkBuilt(ServerLevel level, ChunkPos chunk) {
		TownState state = TownState.get(level);
		BlockPos origin = state.origin();
		if (origin == null) {
			return;
		}
		if (state.theme().isEmpty()) {
			state.setTheme(theme(), level.getGameTime());
		}
		for (TownData.Site site : TownData.get().sites) {
			BlockPos anchor = origin.offset(site.anchor());
			if ((anchor.getX() >> 4) == chunk.x() && (anchor.getZ() >> 4) == chunk.z()
					&& !state.siteTheme(site.index()).equals(state.theme()) && loaded(level, origin, site)) {
				apply(level, site.index());
			}
		}
	}

	/** The nearest site still due a change within `reach` of `from`, claimed for `decorator`; -1 if none. */
	public static int claim(ServerLevel level, BlockPos from, UUID decorator, int reach) {
		TownState state = TownState.get(level);
		BlockPos origin = state.origin();
		if (origin == null) {
			return -1;
		}
		CLAIMS.values().removeIf(decorator::equals);
		int best = -1;
		double bestDistance = (double) reach * reach;
		for (TownData.Site site : TownData.get().sites) {
			if (state.siteTheme(site.index()).equals(state.theme()) || CLAIMS.containsKey(site.index())) {
				continue;
			}
			double d = origin.offset(site.anchor()).distSqr(from);
			if (d < bestDistance && loaded(level, origin, site)) {
				best = site.index();
				bestDistance = d;
			}
		}
		if (best >= 0) {
			CLAIMS.put(best, decorator);
		}
		return best;
	}

	public static void release(int site) {
		CLAIMS.remove(site);
	}

	/** Where a site is in the world (its first block). */
	public static @Nullable BlockPos where(ServerLevel level, int site) {
		BlockPos origin = TownState.get(level).origin();
		return origin == null ? null : origin.offset(TownData.get().sites.get(site).anchor());
	}

	/** Whether a site still shows an older theme. */
	public static boolean stale(ServerLevel level, int site) {
		TownState state = TownState.get(level);
		return !state.siteTheme(site).equals(state.theme());
	}

	/** Changes a site to the current theme. */
	public static void apply(ServerLevel level, int index) {
		TownState state = TownState.get(level);
		BlockPos origin = state.origin();
		if (origin == null) {
			return;
		}
		TownData data = TownData.get();
		TownData.Site site = data.sites.get(index);
		String theme = state.theme();
		for (int i = 0; i < site.blocks().length; i++) {
			int[] b = site.blocks()[i];
			BlockState wanted = stateFor(data, site, theme, b[3]);
			BlockPos pos = origin.offset(b[0], b[1], b[2]);
			if (level.getBlockState(pos) != wanted) {
				TownBuilder.set(level, pos, wanted);
			}
		}
		state.setSiteTheme(index, theme);
		CLAIMS.remove(index);
	}

	/** What a site's block shows in a theme (air where the theme leaves it empty). */
	public static BlockState stateFor(TownData data, TownData.Site site, String theme, int slot) {
		if (site.kind().equals("centerpiece")) {
			Map<Integer, String> scene = data.centerpiece.get(theme);
			String text = scene == null ? null : scene.get(slot);
			return text == null ? Blocks.AIR.defaultBlockState() : parse(text, site.facing());
		}
		Map<String, List<List<String>>> kind = data.decor.get(site.kind());
		List<List<String>> slots = kind == null ? null : kind.get(theme);
		if (slots == null || slots.isEmpty()) {
			return Blocks.AIR.defaultBlockState();
		}
		List<String> choices = slots.get(Math.min(slot, slots.size() - 1));
		return parse(choices.get(site.index() % choices.size()), site.facing());
	}

	private static BlockState parse(String text, Direction facing) {
		String filled = text.replace("{facing}", facing.getSerializedName()).replace("{rotation}", Integer.toString(rotation(facing)));
		return PARSED.computeIfAbsent(filled, TownData::parse);
	}

	/** A standing banner's rotation facing this way (0 is south, counting clockwise in sixteenths). */
	static int rotation(Direction facing) {
		return switch (facing) {
			case WEST -> 4;
			case NORTH -> 8;
			case EAST -> 12;
			default -> 0;
		};
	}

	private static boolean loaded(ServerLevel level, BlockPos origin, TownData.Site site) {
		for (int[] b : site.blocks()) {
			int x = origin.getX() + b[0];
			int z = origin.getZ() + b[2];
			if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) {
				return false;
			}
		}
		return true;
	}

	private static void announce(ServerLevel level, String theme) {
		for (ServerPlayer player : level.players()) {
			if (Town.isInside(level, player.blockPosition())) {
				player.sendSystemMessage(Component.translatable("message.jugcraft.town.decorating",
						Component.translatable("theme.jugcraft." + theme)));
			}
		}
	}

	/** A player's first time inside the wall: the town welcomes them with a few Jugs. */
	private static void welcome(ServerLevel level, TownState state) {
		for (ServerPlayer player : level.players()) {
			if (Town.isInside(level, player.blockPosition()) && state.welcome(player.getUUID())) {
				long jugs = TownShops.get().welcomeJugs;
				Jugs.add(level.getServer(), player.getUUID(), jugs);
				player.sendSystemMessage(Component.translatable("message.jugcraft.town.welcome", jugs));
			}
		}
	}
}
