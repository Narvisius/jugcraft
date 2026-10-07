package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.SeasonalLeavesBlock;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import io.github.jimbozoomer.jugcraft.world.AlpineSpawn;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alpine Spawn in a real, normally generated world (a fixed seed): a new world starts in the biome, at an alpine
 * village. Logs how much of the land around it is Alpine Spawn. Screenshots of the start, the view from above it, and
 * three larches grown in spring, autumn and winter.
 */
public class AlpineClientGameTests implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("jugcraft-test");
	private static final String SEED = "jugcraft";
	/** The start should be at an alpine village: within this many blocks of its start chunk. */
	private static final int VILLAGE_DISTANCE = 128;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().adjustSettings(creator -> {
			creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
			creator.setSeed(SEED);
			// Fabric's consistent test settings switch structures off; villages are part of what this checks.
			creator.setGenerateStructures(true);
		}).create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			BlockPos start = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			boolean alpine = server.computeOnServer(minecraft -> minecraft.overworld().getBiome(start).is(AlpineSpawn.BIOME));
			String biome = server.computeOnServer(minecraft -> minecraft.overworld().getBiome(start).unwrapKey()
					.map(key -> key.identifier().toString()).orElse("?"));
			BlockPos village = server.computeOnServer(minecraft -> minecraft.overworld()
					.findNearestMapStructure(AlpineSpawn.VILLAGES, start, 2, false));
			int villageDistance = village == null ? -1 : (int) Math.sqrt(village.distSqr(start.atY(village.getY())));
			LOGGER.info("Alpine Spawn, seed {}: the player starts at {} {} {} in {}; nearest alpine village {}", SEED, start.getX(),
					start.getY(), start.getZ(), biome, village == null ? "none within 2 grid cells"
					: village.getX() + " " + village.getZ() + " (" + villageDistance + " blocks)");
			LOGGER.info("Alpine Spawn, seed {}: {} of the 2 km square around the start, {} of a 16 km square around the origin", SEED,
					server.computeOnServer(minecraft -> share(minecraft.overworld(), start, 1024, 32)),
					server.computeOnServer(minecraft -> share(minecraft.overworld(), new BlockPos(0, 128, 0), 8192, 256)));
			// The start seen from just above head height (standing at the start may face a terrace wall), then from above.
			shoot(context, singleplayer, start.getX(), start.getY() + 4, start.getZ(), 135, 15, "jugcraft_alpine_spawn_start");
			shoot(context, singleplayer, start.getX(), start.getY() + 40, start.getZ(), 135, 25, "jugcraft_alpine_spawn_overview");

			// Three larches grown in spring, autumn and winter, with random ticks off so each keeps its look.
			int x = start.getX();
			int y = start.getY();
			int z = start.getZ();
			server.runCommand("gamerule randomTickSpeed 0");
			server.runCommand("gamerule minecraft:random_tick_speed 0");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 4, y - 1, z - 14, x + 24, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 14, x + 24, y + 16, z + 8));
			String grown = server.computeOnServer(minecraft -> growLarches(minecraft, new BlockPos(x, y, z)));
			LOGGER.info("Larches grown in spring, autumn and winter (green/gold/bare needles each): {}", grown);
			shoot(context, singleplayer, x + 10, y + 4, z + 6, 180, 0, "jugcraft_larch_seasons");
			if (!alpine) {
				throw new AssertionError("A new world (seed " + SEED + ") starts in " + biome + ", not Alpine Spawn");
			}
			if (village == null || villageDistance > VILLAGE_DISTANCE) {
				throw new AssertionError("A new world (seed " + SEED + ") does not start at an alpine village: " + village);
			}
		}
	}

	/** Stands the player on an invisible barrier at a spot and takes a screenshot. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z,
			int yaw, int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	/** How much of a square (half-width {@code radius}) is Alpine Spawn, sampled every {@code step} blocks, from the generator. */
	private static String share(ServerLevel level, BlockPos center, int radius, int step) {
		int alpine = 0;
		int total = 0;
		for (int dx = -radius; dx <= radius; dx += step) {
			for (int dz = -radius; dz <= radius; dz += step) {
				total++;
				if (level.getBiome(center.offset(dx, 0, dz)).is(AlpineSpawn.BIOME)) {
					alpine++;
				}
			}
		}
		return String.format(Locale.ROOT, "%.1f%% (%d of %d samples)", 100.0 * alpine / total, alpine, total);
	}

	/** Grows a larch in spring, one in autumn and one in winter, in a row; returns each tree's needles by look. */
	private static String growLarches(MinecraftServer server, BlockPos origin) {
		ServerLevel level = server.overworld();
		SeasonCalendar.Settings before = JugcraftSeasons.settings();
		SeasonCalendar.Mode[] modes = {SeasonCalendar.Mode.SPRING, SeasonCalendar.Mode.AUTUMN, SeasonCalendar.Mode.WINTER};
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < modes.length; i++) {
			BlockPos trunk = origin.offset(2 + 8 * i, 0, -6);
			JugcraftSeasons.setMode(server, modes[i]);
			level.setBlock(trunk, JugcraftAgriculture.block("larch_sapling").defaultBlockState(), Block.UPDATE_ALL);
			boolean grew = JugcraftAgriculture.LARCH_GROWER.growTree(level, level.getChunkSource().getGenerator(), trunk,
					level.getBlockState(trunk), level.getRandom());
			int[] counts = new int[SeasonalLeavesBlock.Foliage.values().length];
			for (BlockPos pos : BlockPos.betweenClosed(trunk.offset(-3, 0, -3), trunk.offset(3, 16, 3))) {
				BlockState state = level.getBlockState(pos);
				if (state.is(JugcraftAgriculture.block("larch_needles"))) {
					counts[state.getValue(SeasonalLeavesBlock.SEASON).ordinal()]++;
				}
			}
			out.append(i == 0 ? "" : "; ").append(modes[i]).append(grew ? "" : " (did not grow)").append(' ')
					.append(counts[0]).append('/').append(counts[1]).append('/').append(counts[2]);
		}
		JugcraftSeasons.setMode(server, before.mode());
		return out.toString();
	}
}
