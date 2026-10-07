package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.client.SeasonColors;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import io.github.jimbozoomer.jugcraft.season.SeasonalSnow;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seasons in a real client: a grove of oaks on the plains of a flat world, seen in each season the server sets, with
 * screenshots, and the leaves' and grass's tints read back from the client's level; then winter snow falling and lying.
 */
public class SeasonClientGameTests implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("jugcraft-test");
	/** Trees in front of the camera, as (x, z) offsets from the player's start. */
	private static final int[][] TREES = {{-7, -7}, {-2, -9}, {3, -7}, {8, -10}, {-5, -13}, {1, -14}, {6, -15}, {-10, -11}};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runOnServer(minecraft -> buildGrove(minecraft.overworld(), origin));
			// The server told the client the season when it joined.
			int joined = context.computeOnClient(client -> SeasonColors.day());
			int expected = server.computeOnServer(minecraft -> JugcraftSeasons.today());
			if (joined != expected) {
				throw new AssertionError("The client has season day " + joined + " after joining; the server has " + expected);
			}
			// The HUD is left as the previous client test left it (hidden): only the screenshots' looks depend on it.
			server.runCommand("tp @p %d %d %d 180 -4".formatted(x, y, z + 1));

			BlockPos leaf = origin.offset(TREES[2][0], 4, TREES[2][1]);
			BlockPos ground = origin.offset(0, -1, -4);
			Map<SeasonCalendar.Mode, int[]> tints = new EnumMap<>(SeasonCalendar.Mode.class);
			SeasonCalendar.Mode configured = server.computeOnServer(minecraft -> JugcraftSeasons.settings().mode());
			for (SeasonCalendar.Mode mode : List.of(SeasonCalendar.Mode.SUMMER, SeasonCalendar.Mode.SPRING, SeasonCalendar.Mode.AUTUMN,
					SeasonCalendar.Mode.WINTER, SeasonCalendar.Mode.OFF, SeasonCalendar.Mode.AUTO)) {
				server.runOnServer(minecraft -> JugcraftSeasons.setMode(minecraft, mode));
				context.waitTicks(20);
				singleplayer.getConnection().waitForChunksRender();
				int day = context.computeOnClient(client -> SeasonColors.day());
				int serverDay = server.computeOnServer(minecraft -> JugcraftSeasons.today());
				if (day != serverDay) {
					throw new AssertionError(mode + ": the client has season day " + day + ", the server " + serverDay);
				}
				int[] tint = context.computeOnClient(client -> new int[] {
						client.level.getBlockTint(leaf, BiomeColors.FOLIAGE_COLOR_RESOLVER),
						client.level.getBlockTint(ground, BiomeColors.GRASS_COLOR_RESOLVER)});
				tints.put(mode, tint);
				LOGGER.info("Season {} (day {}): foliage #{}, grass #{}", mode, day,
						Integer.toHexString(tint[0] & 0xFFFFFF), Integer.toHexString(tint[1] & 0xFFFFFF));
				context.takeScreenshot("jugcraft_season_" + mode.name().toLowerCase(Locale.ROOT));
			}
			// Winter snow (opt-in): rain falls as snow over the grove, and the season's snow covers the ground and crowns.
			server.runCommand("weather rain");
			server.runOnServer(minecraft -> {
				JugcraftSeasons.setMode(minecraft, SeasonCalendar.Mode.WINTER);
				JugcraftSeasons.setSnow(minecraft, true);
				ServerLevel level = minecraft.overworld();
				for (int layer = 0; layer < 2; layer++) {
					for (int dx = -16; dx <= 16; dx++) {
						for (int dz = -20; dz <= 4; dz++) {
							BlockPos column = origin.offset(dx, 0, dz);
							SeasonalSnow.snowAt(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column), 2);
						}
					}
				}
			});
			context.waitTicks(120);
			singleplayer.getConnection().waitForChunksRender();
			boolean snowFalls = context.computeOnClient(client -> client.level.getBiome(ground.above()).value()
					.getPrecipitationAt(ground.above(), client.level.getSeaLevel()) == Biome.Precipitation.SNOW);
			boolean snowyGrass = server.computeOnServer(minecraft -> minecraft.overworld().getBlockState(ground)
					.getValue(BlockStateProperties.SNOWY));
			LOGGER.info("Winter snow: client sees snow falling {}, grass under the season's snow is snowy {}", snowFalls, snowyGrass);
			context.takeScreenshot("jugcraft_season_winter_snow");
			server.runOnServer(minecraft -> JugcraftSeasons.setSnow(minecraft, false));
			server.runCommand("weather clear");
			if (!snowFalls || !snowyGrass) {
				throw new AssertionError("Winter snow: falling on the client " + snowFalls + ", snowy grass " + snowyGrass);
			}

			server.runOnServer(minecraft -> JugcraftSeasons.setMode(minecraft, configured));

			int[] summer = tints.get(SeasonCalendar.Mode.SUMMER);
			int[] off = tints.get(SeasonCalendar.Mode.OFF);
			int[] autumn = tints.get(SeasonCalendar.Mode.AUTUMN);
			if (summer[0] != off[0] || summer[1] != off[1]) {
				throw new AssertionError("Summer is not vanilla: summer " + hex(summer) + ", off " + hex(off));
			}
			if ((autumn[0] >> 16 & 0xFF) <= (summer[0] >> 16 & 0xFF) + 40 || autumn[1] == summer[1]) {
				throw new AssertionError("Autumn did not turn the plains: autumn " + hex(autumn) + ", summer " + hex(summer));
			}
			for (SeasonCalendar.Mode mode : List.of(SeasonCalendar.Mode.SPRING, SeasonCalendar.Mode.WINTER)) {
				if (tints.get(mode)[0] == summer[0]) {
					throw new AssertionError(mode + " foliage is the summer colour " + hex(summer));
				}
			}
		}
	}

	private static String hex(int[] tint) {
		return "#" + Integer.toHexString(tint[0] & 0xFFFFFF) + "/#" + Integer.toHexString(tint[1] & 0xFFFFFF);
	}

	/** Grass with nothing above it around the camera, and small oaks (persistent leaves) with short grass between. */
	private static void buildGrove(ServerLevel level, BlockPos origin) {
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int dx = -16; dx <= 16; dx++) {
			for (int dz = -20; dz <= 4; dz++) {
				level.setBlock(origin.offset(dx, -1, dz), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
				for (int dy = 0; dy <= 10; dy++) {
					BlockPos pos = origin.offset(dx, dy, dz);
					if (!level.getBlockState(pos).isAir()) {
						level.setBlock(pos, air, 2);
					}
				}
				if ((dx * 7 + dz * 13 & 7) == 0 && dz < -1) {
					level.setBlock(origin.offset(dx, 0, dz), Blocks.SHORT_GRASS.defaultBlockState(), 2);
				}
			}
		}
		BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		for (int[] tree : TREES) {
			BlockPos base = origin.offset(tree[0], 0, tree[1]);
			for (int dy = 2; dy <= 5; dy++) {
				int radius = dy <= 3 ? 2 : 1;
				for (int dx = -radius; dx <= radius; dx++) {
					for (int dz = -radius; dz <= radius; dz++) {
						boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
						if (!corner || dy == 2) {
							level.setBlock(base.offset(dx, dy, dz), leaves, 2);
						}
					}
				}
			}
			for (int dy = 0; dy <= 3; dy++) {
				level.setBlock(base.above(dy), Blocks.OAK_LOG.defaultBlockState(), 2);
			}
		}
	}
}
