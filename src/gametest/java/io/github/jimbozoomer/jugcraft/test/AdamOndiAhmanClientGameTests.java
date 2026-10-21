package io.github.jimbozoomer.jugcraft.test;

import com.mojang.datafixers.util.Pair;
import io.github.jimbozoomer.jugcraft.world.AdamOndiAhman;
import io.github.jimbozoomer.jugcraft.world.AdamOndiAhmanState;
import io.github.jimbozoomer.jugcraft.world.AltarKnolls;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adam-ondi-Ahman in a real new world (seed {@link #SEED}): the server must have chosen a valley 800 to 1,600 blocks from
 * the start, the biome must be there, and the knoll must stand in it with the Ancient Altar on top, dormant. The camera
 * looks over the valley, at the knoll, and at the altar; the distances, the nearest rare valleys and where the altar
 * stands are logged.
 */
public class AdamOndiAhmanClientGameTests implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("jugcraft-adam-ondi-ahman-client-tests");
	private static final String SEED = "jugcraft";
	/** How far round a site the knoll's template can reach, whichever way it is turned (tools/adam_ondi_ahman.py SIZE). */
	private static final int KNOLL_REACH = 48;
	/** How far round the start to count the rare valleys, for the log. */
	private static final int RARE_SEARCH = 12000;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().adjustSettings(creator -> {
			creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
			creator.setSeed(SEED);
		}).create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			BlockPos start = server.computeOnServer(minecraft -> minecraft.overworld().getRespawnData().pos());
			AdamOndiAhman.Site site = server.computeOnServer(minecraft -> AdamOndiAhman.startSite());
			if (site == null) {
				throw new AssertionError("The new world has no Adam-ondi-Ahman near its start");
			}
			double distance = Math.hypot(site.x() - start.getX(), site.z() - start.getZ());
			LOGGER.info("Adam-ondi-Ahman, seed {}: the valley near the start is at {} {}, {} blocks from the start at {} {} {}", SEED,
					site.x(), site.z(), (int) distance, start.getX(), start.getY(), start.getZ());
			// The site is a chunk's corner within the rings searched, so it may lie up to a chunk's diagonal outside them.
			if (distance < AdamOndiAhman.START_MIN - 23 || distance > AdamOndiAhman.START_MAX + 23) {
				throw new AssertionError("The valley is " + (int) distance + " blocks from the start, not " + AdamOndiAhman.START_MIN + " to "
						+ AdamOndiAhman.START_MAX);
			}
			// The biome as a search from the start finds it (as /locate biome does), read from the generator.
			Pair<BlockPos, Holder<Biome>> found = server.computeOnServer(minecraft -> minecraft.overworld().findClosestBiome3d(
					biome -> biome.is(AdamOndiAhman.BIOME), start, AdamOndiAhman.START_MAX + AdamOndiAhman.RADIUS + 64, 32, 64));
			if (found == null) {
				throw new AssertionError("No Adam-ondi-Ahman biome within reach of the start");
			}
			LOGGER.info("Adam-ondi-Ahman, seed {}: the nearest of its biome to the start is at {} {}, {} blocks from the site", SEED,
					found.getFirst().getX(), found.getFirst().getZ(), (int) Math.hypot(found.getFirst().getX() - site.x(), found.getFirst().getZ() - site.z()));
			List<AdamOndiAhman.Site> rare = server.computeOnServer(minecraft -> AdamOndiAhman.rareSitesNear(start.getX(), start.getZ(), RARE_SEARCH));
			LOGGER.info("Adam-ondi-Ahman, seed {}: {} rare valleys within {} blocks of the start{}", SEED, rare.size(), RARE_SEARCH,
					rare.isEmpty() ? "" : ", the nearest " + (int) rare.stream().mapToDouble(r -> Math.hypot(r.x() - start.getX(), r.z() - start.getZ())).min().orElse(0)
							+ " blocks away");
			for (AdamOndiAhman.Site valley : rare) {
				boolean biome = server.computeOnServer(minecraft -> minecraft.overworld().getBiome(valley.pos().atY(minecraft.overworld()
						.getChunkSource().getGenerator().getBaseHeight(valley.x(), valley.z(), Heightmap.Types.WORLD_SURFACE_WG, minecraft.overworld(),
								minecraft.overworld().getChunkSource().randomState()))).is(AdamOndiAhman.BIOME));
				if (!biome) {
					throw new AssertionError("The rare valley's site at " + valley.x() + " " + valley.z() + " is not in Adam-ondi-Ahman");
				}
			}
			// Over the site, so the knoll's chunks load and generate; then find the altar on the top of the knoll.
			fly(context, server, new BlockPos(site.x(), 150, site.z()), 180, 60);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos altar = null;
			for (int wait = 0; wait < 60 && altar == null; wait++) {
				context.waitTicks(20);
				altar = server.computeOnServer(minecraft -> findAltar(minecraft.overworld(), site));
			}
			if (altar == null) {
				// What the site holds instead: whether its knoll is built, how many of its chunks are new and loaded, and the
				// ground; and a picture of it from above.
				String held = server.computeOnServer(minecraft -> {
					ServerLevel level = minecraft.overworld();
					AdamOndiAhmanState state = AdamOndiAhmanState.get(level);
					long key = AltarKnolls.key(site);
					int fresh = 0;
					int loaded = 0;
					List<ChunkPos> chunks = AltarKnolls.footprint(site);
					for (ChunkPos pos : chunks) {
						fresh += state.isNew(key, pos.pack()) ? 1 : 0;
						loaded += level.getChunkSource().getChunkNow(pos.x(), pos.z()) != null ? 1 : 0;
					}
					StringBuilder out = new StringBuilder("knoll built: " + state.built(key) + ", chunks new " + fresh + " and loaded " + loaded
							+ " of " + chunks.size() + ", the knoll's height " + AltarKnolls.groundHeight(level, site));
					for (int dx = -24; dx <= 24; dx += 24) {
						for (int dz = -24; dz <= 24; dz += 24) {
							int x = site.x() + dx;
							int z = site.z() + dz;
							BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
							out.append("; ").append(x).append(' ').append(z).append(": ")
									.append(level.getBlockState(ground).getBlock().getDescriptionId()).append(" at y ").append(ground.getY());
						}
					}
					return out.toString();
				});
				LOGGER.info("Adam-ondi-Ahman, seed {}: no altar; {}", SEED, held);
				context.takeScreenshot("jugcraft_adam_ondi_ahman_site");
				throw new AssertionError("No Ancient Altar within " + KNOLL_REACH + " blocks of the valley's site at " + site.x() + " " + site.z()
						+ " (" + held + ")");
			}
			BlockPos top = altar;
			String biome = server.computeOnServer(minecraft -> minecraft.overworld().getBiome(top).unwrapKey().map(key -> key.identifier().toString())
					.orElse("?"));
			LOGGER.info("Adam-ondi-Ahman, seed {}: the Ancient Altar stands at {} {} {}, in {}", SEED, top.getX(), top.getY(), top.getZ(), biome);
			if (!biome.equals("jugcraft:adam_ondi_ahman")) {
				throw new AssertionError("The Ancient Altar stands in " + biome + ", not in Adam-ondi-Ahman");
			}
			// The valley from high over the knoll's south, looking down on it; then the knoll from the land south of it; then the
			// altars from beside the top one, and the altar's carved face from over it. Each camera stands clear of the ground.
			Vec3 centre = Vec3.atCenterOf(top);
			look(context, server, top.offset(0, 0, 70), 30, centre);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_adam_ondi_ahman_valley");
			look(context, server, top.offset(0, 0, 34), 3, centre);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_adam_ondi_ahman_knoll");
			look(context, server, top.offset(0, 0, 7), 2, centre);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_adam_ondi_ahman_altars");
			look(context, server, top.offset(0, 0, 2), 4, centre);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_adam_ondi_ahman_altar");
			// Used, it says it is dormant, and stays.
			boolean dormant = server.computeOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
				level.getBlockState(top).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(top), Direction.UP, top, false));
				return level.getBlockState(top).is(AdamOndiAhman.ANCIENT_ALTAR);
			});
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_adam_ondi_ahman_dormant");
			if (!dormant) {
				throw new AssertionError("Using the Ancient Altar changed it");
			}
		}
	}

	/** The Ancient Altar within {@link #KNOLL_REACH} blocks of a site, near the top of its column; null if not (yet) there. */
	private static BlockPos findAltar(ServerLevel level, AdamOndiAhman.Site site) {
		for (int x = site.x() - KNOLL_REACH; x <= site.x() + KNOLL_REACH; x++) {
			for (int z = site.z() - KNOLL_REACH; z <= site.z() + KNOLL_REACH; z++) {
				if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) {
					return null;
				}
				// The top block of its column, or near it (leaves may hang over it), under the barrier the camera stands on if
				// that is over this column (the first camera stands over the site, which is the altar's column).
				int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
				if (level.getBlockState(new BlockPos(x, surface - 1, z)).is(Blocks.BARRIER)) {
					surface--;
					while (surface > level.getMinY() && level.getBlockState(new BlockPos(x, surface - 1, z)).isAir()) {
						surface--;
					}
				}
				for (int y = surface - 1; y >= surface - 8; y--) {
					BlockPos at = new BlockPos(x, y, z);
					if (level.getBlockState(at).is(AdamOndiAhman.ANCIENT_ALTAR)) {
						return at;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Puts the camera `up` blocks over the ground (or over the altar's level, if that is higher) at the column of `column`,
	 * looking at `target`.
	 */
	private static void look(ClientGameTestContext context, TestServerContext server, BlockPos column, int up, Vec3 target) {
		int ground = server.computeOnServer(minecraft -> minecraft.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(),
				column.getZ()));
		int y = Math.max(ground, (int) Math.floor(target.y) - 1) + up;
		double dx = target.x - (column.getX() + 0.5);
		double dz = target.z - (column.getZ() + 0.5);
		double dy = target.y - (y + 1.62);
		int yaw = (int) Math.round(Math.toDegrees(Math.atan2(-dx, dz)));
		int pitch = (int) Math.round(Math.toDegrees(Math.atan2(-dy, Math.hypot(dx, dz))));
		LOGGER.info("Adam-ondi-Ahman: camera at {} {} {} (ground {}), yaw {}, pitch {}", column.getX(), y, column.getZ(), ground, yaw, pitch);
		fly(context, server, new BlockPos(column.getX(), y, column.getZ()), yaw, pitch);
	}

	/**
	 * Puts the player at `at` looking along `yaw`, `pitch`, flying (set on the server, which tells the client), so the
	 * camera stays put even before the client has the chunks it is moved to; and on a barrier if in the air (never over a
	 * block of the world), the chunk loaded first so that there is a block to test.
	 */
	private static void fly(ClientGameTestContext context, TestServerContext server, BlockPos at, int yaw, int pitch) {
		server.runOnServer(minecraft -> {
			ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			ServerLevel level = minecraft.overworld();
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (level.getBlockState(at.below()).isAir()) {
				level.setBlockAndUpdate(at.below(), Blocks.BARRIER.defaultBlockState());
			}
		});
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 %d %d.5 %d %d", at.getX(), at.getY(), at.getZ(), yaw, pitch));
		context.waitTicks(40);
	}
}
