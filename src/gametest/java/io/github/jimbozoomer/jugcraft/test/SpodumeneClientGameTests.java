package io.github.jimbozoomer.jugcraft.test;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.materials.JugcraftMaterials;
import io.github.jimbozoomer.jugcraft.materials.SurfaceLinkedOreFeature;
import io.github.jimbozoomer.jugcraft.world.design.DesignBiomeSource;
import io.github.jimbozoomer.jugcraft.world.design.DesignGrid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;

/** Actual chunk decoration on two normal seeds and a Designed world; exports the first normal save for inspection. */
public class SpodumeneClientGameTests implements FabricClientGameTest {
	private record Sample(Set<BlockPos> underground, List<BlockPos> surface, int eligible, int chunks) { }
	@Override public void runTest(ClientGameTestContext context) {
		if (!JugcraftConfig.isFeatureEnabled("lithium")) { disabled(context); return; }
		designed(context);
		for (String seed : List.of("jugcraft-spodumene-1", "jugcraft-spodumene-2")) natural(context, seed, seed.endsWith("1"));
	}

	private static SurfaceLinkedOreFeature feature(ServerLevel level) {
		return (SurfaceLinkedOreFeature) level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Jugcraft.id("ore_spodumene"));
	}
	private static boolean ore(ServerLevel level, BlockPos pos) {
		var state = level.getBlockState(pos);
		return state.is(JugcraftMaterials.SPODUMENE.ore) || state.is(JugcraftMaterials.SPODUMENE.deepslateOre);
	}

	private static void natural(ClientGameTestContext context, String seed, boolean export) {
		TestWorldSave save; Path source; Sample sample; BlockPos outcrop; Set<BlockPos> fluorite = Set.of();
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(creator -> {
			creator.setName(export ? "Mineral Field Trip" : "Spodumene Sampling");
			creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
			creator.setSeed(seed);
		}).create()) {
			BlockPos center = world.getServer().computeOnServer(server -> {
				var level = server.overworld();
				var found = level.findClosestBiome3d(b -> b.is(Biomes.WINDSWEPT_HILLS), BlockPos.ZERO.atY(80), 8192, 32, 64);
				check(found != null, "No windswept hills on seed " + seed);
				return found.getFirst();
			});
			sample = world.getServer().computeOnServer(server -> {
				var nearby = sample(server.overworld(), center, 8, seed);
				// Rare deposits need not expose a marker in every small biome patch. Expand the survey,
				// keeping the production rarity and outcrop chance unchanged.
				return nearby.surface.isEmpty() ? sample(server.overworld(), center, 16, seed + " expanded survey") : nearby;
			});
			check(!sample.underground.isEmpty(), "No natural spodumene on " + seed);
			check(!export || !sample.surface.isEmpty(), "The inspection save needs a natural outcrop: " + seed);
			// A 25% attempt followed by terrain checks does not guarantee an outcrop in every surveyed region.
			outcrop = sample.surface.isEmpty() ? center : sample.surface.getFirst();
			for (BlockPos marker : sample.surface) {
				check(sample.underground.stream().anyMatch(p -> Math.abs(p.getX() - marker.getX()) <= 18
						&& Math.abs(p.getZ() - marker.getZ()) <= 18), "Outcrop has no nearby underground pocket: " + marker);
			}
			world.getServer().runCommand("gamerule minecraft:send_command_feedback false");
			world.getServer().runCommand("gamerule minecraft:advance_time false");
			world.getServer().runCommand("gamerule minecraft:advance_weather false");
			world.getServer().runCommand("time set noon"); world.getServer().runCommand("weather clear");
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("tp @a " + (outcrop.getX() + 6.5) + " " + (outcrop.getY() + 4) + " " + (outcrop.getZ() + 8.5));
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true; player.onUpdateAbilities();
			});
			context.waitTicks(60); world.getConnection().waitForChunksRender();
			context.getInput().lookAt(outcrop); context.waitTicks(10);
			if (!sample.surface.isEmpty()) screenshot(context, seed + "-outcrop");
			if (export) {
				BlockPos gallery = world.getServer().computeOnServer(server -> openViewingRoom(server.overworld(), sample, outcrop));
				world.getServer().runCommand("tp @a " + xyz(gallery));
				context.waitTicks(40); world.getConnection().waitForChunksRender();
				context.getInput().lookAt(gallery.offset(0, 0, 6)); context.waitTicks(10);
				screenshot(context, seed + "-underground");
				fluorite = FluoriteClientGameTests.addToCombinedTour(context, world);
				world.getServer().runCommand("tp @a " + xyz(outcrop.offset(6, 4, 8)));
				world.getServer().runCommand("setworldspawn " + xyz(outcrop.offset(6, 4, 8)));
				world.getServer().runCommand("gamerule minecraft:respawn_radius 0");
				context.waitTicks(20); context.getInput().lookAt(outcrop);
				world.getServer().runCommand("give @a minecraft:iron_pickaxe");
				world.getServer().runCommand("give @a jugcraft:prospector");
			}
			source = world.getServer().computeOnServer(server -> server.getWorldPath(LevelResource.ROOT));
			// Save a readable itinerary beside the exported save. Ores in this save are natural, not fixtures.
			if (export) writeItinerary(sample, seed, outcrop);
			save = world.getWorldSave();
		}
		Set<BlockPos> persistedFluorite = fluorite;
		try (TestSingleplayerContext reopened = save.open()) {
			reopened.getServer().runOnServer(server -> {
				for (BlockPos pos : persistedFluorite) {
					var state = server.overworld().getBlockState(pos);
					check(state.is(JugcraftMaterials.FLUORITE.ore) || state.is(JugcraftMaterials.FLUORITE.deepslateOre), "Fluorite changed in combined save");
				}
				for (BlockPos pos : sample.underground) check(ore(server.overworld(), pos), "Underground ore changed after reopening: " + pos);
				for (BlockPos pos : sample.surface) check(ore(server.overworld(), pos), "Outcrop changed after reopening: " + pos);
			});
		}
		if (export) copySave(source, Path.of("spodumene-inspection", "Mineral Field Trip"));
		Jugcraft.LOGGER.info("[spodumene] {}: persisted {} underground and {} surface blocks", seed, sample.underground.size(), sample.surface.size());
	}

	private static Sample sample(ServerLevel level, BlockPos center, int radius, String label) {
		Set<BlockPos> underground = new HashSet<>(); List<BlockPos> surface = new ArrayList<>();
		int minX = (center.getX() >> 4) - radius, minZ = (center.getZ() >> 4) - radius;
		int eligible = 0, chunks = 0;
		var cursor = new BlockPos.MutableBlockPos();
		// Finish decoration first: neighboring pockets may cross a chunk edge.
		for (int cx = minX; cx < minX + radius * 2; cx++) for (int cz = minZ; cz < minZ + radius * 2; cz++) level.getChunk(cx, cz);
		for (int cx = minX; cx < minX + radius * 2; cx++) for (int cz = minZ; cz < minZ + radius * 2; cz++) {
			level.getChunk(cx, cz); chunks++;
			int sx = cx * 16 + 8, sz = cz * 16 + 8;
			if (level.getBiome(new BlockPos(sx, level.getHeight(Heightmap.Types.OCEAN_FLOOR, sx, sz), sz)).is(feature(level).surfaceBiomes())) eligible++;
			for (int x = cx * 16; x < cx * 16 + 16; x++) for (int z = cz * 16; z < cz * 16 + 16; z++) {
				int top = Math.min(level.getMaxY(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
				for (int y = -24; y <= Math.max(70, top); y++) if (ore(level, cursor.set(x, y, z))) {
					if (y < 70) underground.add(cursor.immutable());
					else if (x >= minX * 16 + 18 && x < (minX + radius * 2) * 16 - 18
							&& z >= minZ * 16 + 18 && z < (minZ + radius * 2) * 16 - 18) surface.add(cursor.immutable());
				}
			}
		}
		List<Integer> sizes = components(underground).stream().map(Set::size).sorted().toList();
		Jugcraft.LOGGER.info("[spodumene] {}: {} chunks, {} center columns eligible, {} underground blocks, components {}, {} surface blocks",
				label, chunks, eligible, underground.size(), sizes, surface.size());
		return new Sample(Set.copyOf(underground), List.copyOf(surface), eligible, chunks);
	}

	private static List<Set<BlockPos>> components(Set<BlockPos> blocks) {
		Set<BlockPos> remaining = new HashSet<>(blocks); List<Set<BlockPos>> out = new ArrayList<>();
		while (!remaining.isEmpty()) {
			Set<BlockPos> part = new HashSet<>(); ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			BlockPos first = remaining.iterator().next(); remaining.remove(first); queue.add(first);
			while (!queue.isEmpty()) {
				BlockPos pos = queue.removeFirst(); part.add(pos);
				for (var direction : net.minecraft.core.Direction.values()) {
					BlockPos next = pos.relative(direction); if (remaining.remove(next)) queue.addLast(next);
				}
			}
			out.add(part);
		}
		return out;
	}

	private static void designed(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(creator -> {
			creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
			creator.setSeed("jugcraft-spodumene-designed"); creator.setGenerateStructures(false);
			creator.updateDimensions((registries, dimensions) -> {
				var grid = new DesignGrid(-512, -512, 32, 33, Collections.nCopies(33 * 33, 96f));
				var cells = new ArrayList<Integer>();
				for (int z = 0; z < 33; z++) for (int x = 0; x < 33; x++) cells.add(x < 16 ? 1 : 2);
				var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
				var settings = NoiseGeneratorSettings.DIRECT_CODEC.encodeStart(ops,
						registries.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD).value()).getOrThrow().getAsJsonObject();
				var router = settings.getAsJsonObject("noise_router");
				var land = new com.google.gson.JsonObject(); land.addProperty("type", "jugcraft:design_density");
				land.add("grid", DesignGrid.CODEC.encodeStart(ops, grid).getOrThrow()); land.add("input", router.get("final_density"));
				var surface = new com.google.gson.JsonObject(); surface.addProperty("type", "jugcraft:design_density"); surface.addProperty("surface", true);
				surface.add("grid", land.get("grid")); surface.add("input", router.get("chunk_surface_level"));
				router.add("final_density", land); router.add("chunk_surface_level", surface);
				if (settings.has("aquifers")) settings.getAsJsonObject("aquifers").add("surface_level", surface);
				var biomes = registries.lookupOrThrow(Registries.BIOME);
				var source = new DesignBiomeSource(new FixedBiomeSource(biomes.getOrThrow(Biomes.LUSH_CAVES)), grid,
						List.of(biomes.getOrThrow(Biomes.WINDSWEPT_HILLS), biomes.getOrThrow(Biomes.DESERT)), cells,
						new BlockPos(-128, 0, 0), Optional.empty());
				var generator = new NoiseBasedChunkGenerator(source, Holder.direct(NoiseGeneratorSettings.DIRECT_CODEC.parse(ops, settings).getOrThrow()));
				return dimensions.replaceOverworldGenerator(registries, ChunkGenerator.CODEC.parse(ops,
						ChunkGenerator.CODEC.encodeStart(ops, generator).getOrThrow()).getOrThrow());
			});
		}).create()) {
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var rocky = sample(level, new BlockPos(-160, 80, 0), 6, "Designed rocky surface / lush cave underground");
				check(!rocky.underground.isEmpty(), "Painted rocky land did not generate deposits");
				var dry = sample(level, new BlockPos(176, 80, 0), 4, "Designed desert");
				check(dry.underground.isEmpty() && dry.surface.isEmpty(), "Spodumene leaked into painted desert");
				controlledPlacements(level);
			});
		}
	}

	private static void fill(ServerLevel level, BlockPos from, BlockPos to, Block block) {
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) level.setBlock(pos, block.defaultBlockState(), 2);
	}
	private static void controlledPlacements(ServerLevel level) {
		var configured = feature(level);
		var alwaysOutcrop = new SurfaceLinkedOreFeature(configured.ore(), configured.surfaceBiomes(), configured.outcrop(), 1);
		BlockPos center = new BlockPos(-320, -8, -320);
		check(level.getBiome(center).is(Biomes.LUSH_CAVES), "Fixture must test a cave biome below painted rocky land");
		// Isolated artificial fixtures above/inside the Designed world, never included in the inspection save.
		for (String scenario : List.of("air", "snow", "slope", "water", "roof", "obstruction", "hollow")) {
			fill(level, center.offset(-12, -8, -12), center.offset(12, 8, 12), scenario.equals("air") ? Blocks.AIR : Blocks.DEEPSLATE);
			fill(level, new BlockPos(-332, 197, -332), new BlockPos(-308, 207, -308), Blocks.AIR);
			if (!scenario.equals("hollow")) fill(level, new BlockPos(-332, 197, -332), new BlockPos(-308, 199, -308), Blocks.DIRT);
			fill(level, new BlockPos(-332, 200, -332), new BlockPos(-308, 200, -308), Blocks.GRASS_BLOCK);
			if (scenario.equals("slope")) fill(level, new BlockPos(-320, 201, -332), new BlockPos(-308, 201, -308), Blocks.GRASS_BLOCK);
			if (scenario.equals("snow")) fill(level, new BlockPos(-332, 201, -332), new BlockPos(-308, 201, -308), Blocks.SNOW);
			if (scenario.equals("water")) fill(level, new BlockPos(-332, 201, -332), new BlockPos(-308, 201, -308), Blocks.WATER);
			if (scenario.equals("roof")) fill(level, new BlockPos(-332, 204, -332), new BlockPos(-308, 204, -308), Blocks.OAK_PLANKS);
			if (scenario.equals("obstruction")) fill(level, new BlockPos(-332, 201, -332), new BlockPos(-308, 201, -308), Blocks.GLASS);
			boolean placed = alwaysOutcrop.place(level, level.getChunkSource().getGenerator(), RandomSource.create(721), center);
			int count = 0, stone = 0, buried = 0;
			for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-332, 197, -332), new BlockPos(-308, 207, -308))) {
				if (level.getBlockState(pos).is(Blocks.STONE)) { stone++; if (pos.getY() <= 200) buried++; }
				if (ore(level, pos)) {
					count++; int dx = pos.getX() - center.getX(), dz = pos.getZ() - center.getZ();
					check(dx * dx + dz * dz <= 64, "Outcrop beyond eight blocks");
					check(level.getBlockState(pos.below()).is(Blocks.STONE), "Outcrop ore has no stone support");
					boolean exposed = false;
					for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) if (level.getBlockState(pos.relative(direction)).isAir()) exposed = true;
					check(exposed, "Outcrop ore has no visible side for snowy weather");
				}
			}
			check(placed != scenario.equals("air"), "Underground placement mismatch: " + scenario);
			boolean supported = scenario.equals("snow") || scenario.equals("slope");
			check(supported ? count >= 3 && count <= 8 && stone > count && buried >= 10 : count == 0 && stone == 0,
					"Outcrop safety mismatch: " + scenario + " has " + count + " ore / " + stone + " stone / " + buried + " buried");
			Jugcraft.LOGGER.info("[spodumene] fixture {}: underground {}, surface {}, stone {}, buried {}", scenario, placed, count, stone, buried);
		}
	}

	private static BlockPos openViewingRoom(ServerLevel level, Sample sample, BlockPos outcrop) {
		Set<BlockPos> pocket = components(sample.underground).stream().filter(p -> p.size() >= 80)
				.min(Comparator.comparingDouble(p -> p.stream().mapToDouble(pos -> Math.pow(pos.getX() - outcrop.getX(), 2)
						+ Math.pow(pos.getZ() - outcrop.getZ(), 2)).min().orElseThrow())).orElseThrow();
		int x0 = pocket.stream().mapToInt(BlockPos::getX).min().orElseThrow() - 3;
		int x1 = pocket.stream().mapToInt(BlockPos::getX).max().orElseThrow() + 3;
		int y0 = pocket.stream().mapToInt(BlockPos::getY).min().orElseThrow() - 2;
		int y1 = pocket.stream().mapToInt(BlockPos::getY).max().orElseThrow() + 3;
		int z0 = pocket.stream().mapToInt(BlockPos::getZ).min().orElseThrow() - 5;
		int z1 = pocket.stream().mapToInt(BlockPos::getZ).max().orElseThrow() + 3;
		for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(x0, y0, z0), new BlockPos(x1, y1, z1))) {
			if (ore(level, pos)) continue;
			level.setBlock(pos, (pos.getY() == y0 ? Blocks.SEA_LANTERN : Blocks.AIR).defaultBlockState(), 2);
		}
		BlockPos camera = new BlockPos((x0 + x1) / 2, y0 + 2, z0 + 1);
		Jugcraft.LOGGER.info("[spodumene] Viewing room camera {}: surrounding rock removed, all natural spodumene preserved", camera);
		try { Files.writeString(Path.of("spodumene-gallery.txt"), xyz(camera)); } catch (IOException e) { throw new RuntimeException(e); }
		return camera;
	}

	private static void disabled(ClientGameTestContext context) {
		TestWorldSave save; BlockPos fixture;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			fixture = world.getServer().computeOnServer(server -> {
				var level = server.overworld();
				var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getValue(Jugcraft.id("ore_spodumene"));
				var lepidolite = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getValue(Jugcraft.id("ore_lepidolite"));
				for (var biome : level.registryAccess().lookupOrThrow(Registries.BIOME).listElements().toList()) {
					check(!level.getChunkSource().getGenerator().getBiomeGenerationSettings(biome).hasFeature(placed), "Disabled spodumene remains in biome decoration");
					check(!level.getChunkSource().getGenerator().getBiomeGenerationSettings(biome).hasFeature(lepidolite), "Lithium switch must also disable lepidolite acquisition");
				}
				check(level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, Jugcraft.id("spodumene_block"))).isEmpty(), "Disabled compaction recipe still acquired");
				BlockPos pos = server.getPlayerList().getPlayers().getFirst().blockPosition().offset(2, 0, 0);
				level.setBlock(pos, JugcraftMaterials.SPODUMENE.ore.defaultBlockState(), 2);
				level.setBlock(pos.above(), JugcraftMaterials.SPODUMENE.deepslateOre.defaultBlockState(), 2);
				server.getPlayerList().getPlayers().getFirst().getInventory().add(new net.minecraft.world.item.ItemStack(JugcraftMaterials.SPODUMENE.item, 7));
				return pos;
			});
			save = world.getWorldSave();
		}
		try (TestSingleplayerContext reopened = save.open()) {
			reopened.getServer().runOnServer(server -> {
				check(ore(server.overworld(), fixture) && ore(server.overworld(), fixture.above()), "Disabled spodumene blocks lost across save/reopen");
				var inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
				check(inventory.countItem(JugcraftMaterials.SPODUMENE.item) == 7, "Disabled spodumene items lost across save/reopen");
			});
		}
		Jugcraft.LOGGER.info("[spodumene] Disabled acquisition: no biome decoration/compaction; saved ores and seven items survived reopen");
	}

	private static void writeItinerary(Sample sample, String seed, BlockPos outcrop) {
		try {
			Path folder = Path.of("spodumene-inspection"); Files.createDirectories(folder);
			StringBuilder text = new StringBuilder("Mineral Field Trip\nSeed: " + seed + "\nCreative mode; commands enabled. All spodumene generated naturally. A lit viewing room removes surrounding rock from one pocket; ore is preserved.\n\nStart at the surface outcrop: /tp @s " + xyz(outcrop.offset(6, 4, 8)) + "\n\nLit underground viewing room: /tp @s " + Files.readString(Path.of("spodumene-gallery.txt")) + "\n\nFor natural underground pockets (including the viewing-room pocket): /gamemode spectator\n");
			var pockets = components(sample.underground).stream().sorted(Comparator.<Set<BlockPos>>comparingInt(Set::size).reversed()).limit(8).toList();
			for (Set<BlockPos> pocket : pockets) text.append("Pocket (sampled ").append(pocket.size()).append(" blocks): /tp @s ").append(xyz(pocket.iterator().next())).append('\n');
			text.append("\nReturn to mining: /gamemode creative (or survival with an iron pickaxe).\n");
			Files.writeString(folder.resolve("READ-ME.txt"), text);
		} catch (IOException e) { throw new RuntimeException(e); }
	}
	private static String xyz(BlockPos pos) { return pos.getX() + " " + pos.getY() + " " + pos.getZ(); }
	private static void screenshot(ClientGameTestContext context, String name) {
		context.runOnClient(client -> { if (!client.gui.hud.isHidden()) client.gui.hud.toggle(); });
		context.waitTicks(2); context.takeScreenshot(name);
		context.runOnClient(client -> { if (client.gui.hud.isHidden()) client.gui.hud.toggle(); });
	}
	private static void copySave(Path source, Path destination) {
		try (var files = Files.walk(source)) {
			for (Path file : files.toList()) {
				Path target = destination.resolve(source.relativize(file).toString());
				if (Files.isDirectory(file)) Files.createDirectories(target);
				else if (!file.getFileName().toString().equals("session.lock")) Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) { throw new RuntimeException(e); }
	}
	private static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); }
}
