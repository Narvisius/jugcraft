package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;

/** An exported comparison garden plus untouched natural biome stops in the same normal world. */
public class TreeBatchTwoClientGameTests implements FabricClientGameTest {
	protected static final BlockPos GARDEN = new BlockPos(1000, 180, -1000);
	private static final List<String> BIOMES = List.of("lush_desert", "outback", "eucalyptus_forest", "hot_springs",
			"karst_pinnacles", "shield", "jacaranda_glade", "floodplain", "subtropics", "flower_isle");

	protected List<String> shapes() { return TreeBatchTwoGameTests.SHAPES; }
	protected List<String> biomes() { return BIOMES; }
	protected String worldName() { return "Tree Batch 2 Inspection"; }
	protected String exportName() { return "tree-batch-2-preview"; }
	protected String screenshotPrefix() { return "jugcraft_tree2_"; }
	protected void extraDisplays(ClientGameTestContext context, TestSingleplayerContext world, List<String> tour) { }
	protected void validateSurvey(String biome, Map<String, Integer> survey) { }
	protected BlockPos surveySpot(ServerLevel level, BlockPos first, ResourceKey<Biome> biome) { return first; }
	protected boolean includeSubmergedColumns() { return false; }
	protected List<BlockPos> surveyAnchors(String biome) {
		var anchors = new ArrayList<BlockPos>();
		anchors.add(BlockPos.ZERO);
		// Dry River has very little exposed bank. It is valid for the closest
		// occurrences to have no dry placement candidates, even with trees enabled.
		int[] rings = biome.equals("dry_river") ? new int[] {4096, 8192, 16384, 32768} : new int[] {4096, 8192};
		for (int reach : rings) for (int[] direction : new int[][] {{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1},{1,-1},{-1,1}})
			anchors.add(new BlockPos(reach * direction[0], 0, reach * direction[1]));
		return anchors;
	}

	protected void validateReopened(ServerLevel level) { }
	protected void beforeReopenValidation(ClientGameTestContext context, TestSingleplayerContext world) { }

	@Override
	public void runTest(ClientGameTestContext context) {
		Path source;
		TestWorldSave save;
		Map<BlockPos, TreeBatchTwoGameTests.Size> expected = new LinkedHashMap<>();
		List<String> tour = new ArrayList<>(List.of(worldName() + " — normal world, seed jugcraft.",
				"The elevated garden is an authored comparison area. Listed biome stops are naturally generated.",
				"Three seeds per new shape; all use the real configured features. New terrain only, no retrogen.", ""));
		try (var world = context.worldBuilder().adjustSettings(creator -> {
			creator.setName(worldName());
			creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			creator.setSeed("jugcraft");
			creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
		}).create()) {
			world.getConnection().waitForChunksRender();
			var server = world.getServer();
			for (String command : List.of("difficulty peaceful", "time set noon", "weather clear",
					"gamerule minecraft:send_command_feedback false", "gamerule minecraft:advance_time false",
					"gamerule minecraft:advance_weather false", "gamerule minecraft:random_tick_speed 0",
					"gamerule minecraft:spawn_mobs false", "gamerule minecraft:respawn_radius 0")) server.runCommand(command);
			server.runOnServer(minecraft -> JugcraftSeasons.setMode(minecraft, SeasonCalendar.Mode.SUMMER));
			for (int i = 0; i < shapes().size(); i++) {
				String shape = shapes().get(i);
				BlockPos row = GARDEN.offset(i * 40, 0, 0);
				for (int sample = 0; sample < 3; sample++) {
					BlockPos at = row.offset(0, 0, -sample * 40);
					long seed = sample + 1;
					var size = server.computeOnServer(minecraft -> {
						var level = minecraft.overworld();
						TreeBatchTwoGameTests.reset(level, at, shape.equals("mallee") ? Blocks.RED_SAND : Blocks.GRASS_BLOCK);
						// Support the comparison plots, including falling sand, and make them safe to walk between.
						for (BlockPos support : BlockPos.betweenClosed(at.offset(-20, -2, -20), at.offset(20, -2, 20)))
							level.setBlock(support, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
						check(TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(),
								RandomSource.create(seed), at), "Garden tree failed: " + shape);
						return TreeBatchTwoGameTests.measure(level, at);
					});
					expected.put(at, size);
				}
				server.runOnServer(minecraft -> PineWoodClientGameTests.label(minecraft.overworld(), row.offset(0, 0, 10),
						shape.replace('_', ' '), "Seeds 1 / 2 / 3", "Walk north"));
				tour.add(shape + ": /tp @s " + xyz(row.offset(0, 0, 12)) + " 180 0");
				shoot(context, world, row.offset(20, shape.equals("giant_eucalyptus") ? 24 : 7, 24),
						row.above(shape.equals("giant_eucalyptus") ? 22 : 5), screenshotPrefix() + shape);
			}
			// A natural-world survey, separate from the authored garden. No tree commands at these stops.
			int found = 0;
			for (String name : biomes()) {
				ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Jugcraft.id(name));
				BlockPos place = null;
				var checked = new java.util.HashSet<BlockPos>();
				// A small surface occurrence need not contain every rare tree. Survey
				// distinct regions, retaining the actual natural-species assertions.
				for (BlockPos anchor : surveyAnchors(name)) {
					place = server.computeOnServer(minecraft -> {
						var level = minecraft.overworld();
						var nearest = level.findClosestBiome3d(holder -> holder.is(key), anchor, 12800, 64, 64);
						if (nearest == null || !checked.add(nearest.getFirst())) return null;
						BlockPos first = BiomeClientGameTests.surfaceSpot(level, nearest.getFirst(), key);
						return first == null ? null : surveySpot(level, first, key);
					});
					// Permit normal chunk unloading between distant generation surveys.
					context.waitTicks(5);
					if (place != null) break;
				}
				final BlockPos surveyedPlace = place;
				if (place == null) { tour.add(name + ": no surface stop found within the survey radius"); continue; }
				found++;
				var survey = server.computeOnServer(minecraft -> survey(minecraft.overworld(), surveyedPlace, key, includeSubmergedColumns()));
				validateSurvey(name, survey);
				Jugcraft.LOGGER.info("[tree batch 2 survey] {} at {}: {}", name, place, survey);
				if (name.equals("lush_desert")) {
					double grassShare = survey.getOrDefault("surface:minecraft:grass_block", 0) / (double) survey.get("dry_columns");
					check(grassShare >= .10 && grassShare <= .15, "Desert grass island coverage outside 10-15% in this seed: " + grassShare);
					// Carvers run after surface rules: they can cut through dirt and expose a one-block grass floor.
					// Check the island population, while the generator regression verifies identical patch conditions.
					double dirtShare = survey.getOrDefault("grass_subsoil:minecraft:dirt", 0) /
							(double) survey.get("surface:minecraft:grass_block");
					check(dirtShare >= .95, "Most desert grass islands should have dirt beneath: " + dirtShare);
				}
				tour.add(name + ": /tp @s " + xyz(place.above(18)) + " 135 25; " + survey);
				shoot(context, world, place.above(22), place.offset(-20, 2, -20), screenshotPrefix() + "biome_" + name);
			}
			check(found == biomes().size(), "Missing natural biome stops: " + found);
			extraDisplays(context, world, tour);
			BlockPos start = GARDEN.offset(Math.min(3, shapes().size() - 1) * 40, 0, 12);
			server.runCommand("tp @a " + xyz(start) + " 180 0");
			server.runCommand("setworldspawn " + xyz(start));
			server.runOnServer(minecraft -> {
				var player = minecraft.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = false;
				player.onUpdateAbilities();
			});
			source = server.computeOnServer(minecraft -> minecraft.getWorldPath(LevelResource.ROOT));
			save = world.getWorldSave();
		}
		try (var reopened = save.open()) {
			reopened.getConnection().waitForChunksRender();
			beforeReopenValidation(context, reopened);
			reopened.getServer().runOnServer(minecraft -> expected.forEach((at, size) ->
					check(TreeBatchTwoGameTests.measure(minecraft.overworld(), at).equals(size), "Tree changed across save/reopen at " + at)));
			reopened.getServer().runOnServer(minecraft -> validateReopened(minecraft.overworld()));
		}
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.isRegularFile(root.resolve("build.gradle"))) root = root.getParent();
		check(root != null, "Missing export root");
		Path out = root.resolve("build/" + exportName());
		try (var files = Files.walk(source)) {
			for (Path file : files.toList()) {
				Path dest = out.resolve(worldName()).resolve(source.relativize(file).toString());
				if (Files.isDirectory(file)) Files.createDirectories(dest);
				else if (!file.getFileName().toString().equals("session.lock")) Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
			}
			Files.write(out.resolve("TOUR.txt"), tour);
		} catch (IOException e) { throw new RuntimeException(e); }
		// Loom clears clientGameTest during later builds; preserve the visual evidence with the exported world.
		try (var screenshots = Files.list(root.resolve("build/run/clientGameTest/screenshots"))) {
			Path images = out.resolve("screenshots");
			Files.createDirectories(images);
			for (Path file : screenshots.filter(p -> p.getFileName().toString().contains(screenshotPrefix())).toList())
				Files.copy(file, images.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) { throw new RuntimeException(e); }
		Jugcraft.LOGGER.info("[tree batch 2] Exported {} after save/reopen of {} trees", out, expected.size());
	}

	static Map<String, Integer> survey(ServerLevel level, BlockPos at, ResourceKey<Biome> biome) {
		return survey(level, at, biome, false);
	}

	protected static Map<String, Integer> survey(ServerLevel level, BlockPos at, ResourceKey<Biome> biome, boolean includeSubmerged) {
		Map<String, Integer> counts = new java.util.TreeMap<>();
		// 16,641 columns, at most 81 chunks; count only this biome, ignore canopy in surface coverage.
		for (int dx = -64; dx <= 64; dx++) for (int dz = -64; dz <= 64; dz++) {
			int x = at.getX() + dx, z = at.getZ() + dz;
			level.getChunk(x >> 4, z >> 4);
			int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
			BlockPos floor = new BlockPos(x, top, z);
			Map<String, Integer> logs = new java.util.TreeMap<>();
			boolean submerged = false;
			for (int y = top; y >= Math.max(level.getMinY(), top - 64); y--) {
				BlockPos pos = new BlockPos(x, y, z);
				var state = level.getBlockState(pos);
				if (!state.getFluidState().isEmpty()) submerged = true;
				if (state.is(BlockTags.LOGS)) logs.merge("log:" + BuiltInRegistries.BLOCK.getKey(state.getBlock()), 1, Integer::sum);
				if (state.is(BlockTags.LEAVES)) logs.merge("leaf:" + BuiltInRegistries.BLOCK.getKey(state.getBlock()), 1, Integer::sum);
				if (!state.isAir() && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS) && state.isSolidRender()) { floor = pos; break; }
			}
			if (!level.getBiome(floor).is(biome) || (submerged && !includeSubmerged)) continue;
			logs.forEach((name, count) -> counts.merge(name, count, Integer::sum));
			if (includeSubmerged) {
				counts.merge(submerged ? "wet_columns" : "dry_columns", 1, Integer::sum);
				if (submerged) logs.forEach((name, count) -> counts.merge("wet_" + name, count, Integer::sum));
			}
			if (biome.identifier().getPath().equals("lush_desert")) {
				double noise = level.getChunkSource().randomState().getOrCreateNoise(net.minecraft.world.level.levelgen.Noises.SURFACE).get(x, 0, z);
				counts.merge("dry_columns", 1, Integer::sum);
				for (double threshold : new double[] {0, .1, .2, .25, .3, .35, .4, .5, .6, .65})
					if (noise >= threshold) counts.merge("noise_at_least:" + threshold, 1, Integer::sum);
			}
			counts.merge("surface:" + BuiltInRegistries.BLOCK.getKey(level.getBlockState(floor).getBlock()), 1, Integer::sum);
			if (level.getBlockState(floor).is(Blocks.GRASS_BLOCK) && biome.identifier().getPath().equals("lush_desert")) {
				counts.merge("grass_subsoil:" + BuiltInRegistries.BLOCK.getKey(level.getBlockState(floor.below()).getBlock()), 1, Integer::sum);

			}
		}
		return counts;
	}

	protected static void shoot(ClientGameTestContext context, TestSingleplayerContext world, BlockPos camera, BlockPos target, String name) {
		world.getServer().runCommand("tp @a " + xyz(camera));
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		});
		context.waitTicks(30);
		world.getConnection().waitForChunksRender();
		context.getInput().lookAt(target);
		context.runOnClient(client -> { if (!client.gui.hud.isHidden()) client.gui.hud.toggle(); });
		context.waitTicks(10);
		context.takeScreenshot(name);
		context.runOnClient(client -> { if (client.gui.hud.isHidden()) client.gui.hud.toggle(); });
	}
	private static String xyz(BlockPos pos) { return pos.getX() + " " + pos.getY() + " " + pos.getZ(); }
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
