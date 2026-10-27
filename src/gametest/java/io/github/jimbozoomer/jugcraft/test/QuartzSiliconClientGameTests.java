package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.workshop.*;
import io.github.jimbozoomer.jugcraft.kinetic.*;
import io.github.jimbozoomer.jugcraft.client.WorkshopScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.MouseHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.*;
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
public class QuartzSiliconClientGameTests implements FabricClientGameTest {
	private record Sample(Set<BlockPos> underground, List<BlockPos> surface, int eligible, int chunks) { }
	@Override public void runTest(ClientGameTestContext context) {
		if (!JugcraftConfig.isFeatureEnabled("silicon")) { disabled(context); return; }
		designed(context);
		for (String seed : List.of("jugcraft-quartz-1", "jugcraft-quartz-2")) natural(context, seed, seed.endsWith("1"));
	}

	private static SurfaceLinkedOreFeature feature(ServerLevel level) {
		return (SurfaceLinkedOreFeature) level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Jugcraft.id("ore_quartz"));
	}
	private static boolean ore(ServerLevel level, BlockPos pos) {
		var state = level.getBlockState(pos);
		return state.is(JugcraftMaterials.QUARTZ.ore) || state.is(JugcraftMaterials.QUARTZ.deepslateOre);
	}

	private static void natural(ClientGameTestContext context, String seed, boolean export) {
		TestWorldSave save; Path source; Sample sample; BlockPos outcrop;
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(creator -> {
			creator.setName(export ? "Quartz and Starter Silicon" : "Quartz Sampling");
			creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
			creator.setSeed(seed);
		}).create()) {
			BlockPos center = world.getServer().computeOnServer(server -> {
				var level = server.overworld();
				// The exported tour needs a sizeable high rocky biome: a tiny low hill may have no marker,
				// and markers above Y104 are unambiguously separate from the underground vein range.
				var target=export ? Biomes.STONY_PEAKS : Biomes.WINDSWEPT_HILLS;
				var found = level.findClosestBiome3d(b -> b.is(target), BlockPos.ZERO.atY(80), 8192, 32, 64);
				check(found != null, "No " + target + " on seed " + seed);
				return found.getFirst();
			});
			sample = world.getServer().computeOnServer(server -> {
				var nearby = sample(server.overworld(), center, 8, seed);
				// Rare deposits need not expose a marker in every small biome patch. Expand the survey,
				// keeping the production rarity and outcrop chance unchanged.
				return nearby.surface.isEmpty() ? sample(server.overworld(), center, 16, seed + " expanded survey") : nearby;
			});
			check(!sample.underground.isEmpty(), "No natural quartz on " + seed);
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
				workshop(context, world);
				world.getServer().runCommand("tp @a 512.5 240 508.5 0 15");
				world.getServer().runCommand("setworldspawn 512 240 508");
				world.getServer().runCommand("gamerule minecraft:respawn_radius 0");
				context.waitTicks(20); context.getInput().lookAt(WORK);
				world.getServer().runCommand("give @a minecraft:iron_pickaxe");
				world.getServer().runCommand("give @a jugcraft:prospector");
			}
			source = world.getServer().computeOnServer(server -> server.getWorldPath(LevelResource.ROOT));
			// Save a readable itinerary beside the exported save. Ores in this save are natural, not fixtures.
			if (export) writeItinerary(sample, seed, outcrop);
			save = world.getWorldSave();
		}

		try (TestSingleplayerContext reopened = save.open()) {
			reopened.getServer().runOnServer(server -> {
                if(export) check(work(server.overworld()).progress()==savedWork && work(server.overworld()).fuelCredit()==savedHeat,"Manual silicon changed across save/reopen");
				for (BlockPos pos : sample.underground) check(ore(server.overworld(), pos), "Underground ore changed after reopening: " + pos);
				for (BlockPos pos : sample.surface) check(ore(server.overworld(), pos), "Outcrop changed after reopening: " + pos);
			});
		}
		if (export) copySave(source, Path.of("../../quartz-silicon-preview", "Quartz and Starter Silicon"));
		Jugcraft.LOGGER.info("[quartz] {}: persisted {} underground and {} surface blocks", seed, sample.underground.size(), sample.surface.size());
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
				for (int y = -24; y <= Math.max(104, top); y++) if (ore(level, cursor.set(x, y, z))) {
					if (y <= 96 && y < top - 3) underground.add(cursor.immutable());
					else if (y > 104 && y >= top - 1 && x >= minX * 16 + 18 && x < (minX + radius * 2) * 16 - 18
							&& z >= minZ * 16 + 18 && z < (minZ + radius * 2) * 16 - 18) surface.add(cursor.immutable());
				}
			}
		}
		List<Integer> sizes = components(underground).stream().map(Set::size).sorted().toList();
		Jugcraft.LOGGER.info("[quartz] {}: {} chunks, {} center columns eligible, {} underground blocks, components {}, {} surface blocks",
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
			creator.setSeed("jugcraft-quartz-designed"); creator.setGenerateStructures(false);
			creator.updateDimensions((registries, dimensions) -> {
				var grid = new DesignGrid(-512, -512, 32, 33, Collections.nCopies(33 * 33, 96f));
				var cells = new ArrayList<Integer>();
				for (int z = 0; z < 33; z++) for (int x = 0; x < 33; x++) cells.add(x < 10 ? 1 : x < 22 ? 2 : 3);
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
						List.of(biomes.getOrThrow(Biomes.WINDSWEPT_HILLS), biomes.getOrThrow(Biomes.FOREST), biomes.getOrThrow(Biomes.DESERT)), cells,
						new BlockPos(-128, 0, 0), Optional.empty());
				var generator = new NoiseBasedChunkGenerator(source, Holder.direct(NoiseGeneratorSettings.DIRECT_CODEC.parse(ops, settings).getOrThrow()));
				return dimensions.replaceOverworldGenerator(registries, ChunkGenerator.CODEC.parse(ops,
						ChunkGenerator.CODEC.encodeStart(ops, generator).getOrThrow()).getOrThrow());
			});
		}).create()) {
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var rocky = sample(level, new BlockPos(-320, 80, 0), 5, "Designed rocky surface / lush cave underground");
				check(!rocky.underground.isEmpty(), "Painted rocky land did not generate deposits");
				check(!sample(level, new BlockPos(0,80,0),5,"Designed forest / lush caves").underground.isEmpty(), "Secondary forest quartz absent");
				var dry = sample(level, new BlockPos(320, 80, 0), 4, "Designed desert");
				check(dry.underground.isEmpty() && dry.surface.isEmpty(), "Quartz leaked into painted desert");
				controlledPlacements(level);
			});
		}
	}

	private static void fill(ServerLevel level, BlockPos from, BlockPos to, Block block) {
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) level.setBlock(pos, block.defaultBlockState(), 2);
	}
	private static void controlledPlacements(ServerLevel level) {
		var configured = feature(level);
		var alwaysOutcrop = new SurfaceLinkedOreFeature(configured.ore(), configured.surfaceBiomes(), configured.outcrop(), 1, configured.outcropBiomes(), 6);
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
			check(supported ? count >= 3 && count <= 6 && stone > count && buried >= 10 : count == 0 && stone == 0,
					"Outcrop safety mismatch: " + scenario + " has " + count + " ore / " + stone + " stone / " + buried + " buried");
			Jugcraft.LOGGER.info("[quartz] fixture {}: underground {}, surface {}, stone {}, buried {}", scenario, placed, count, stone, buried);
		}
        var secondary=(SurfaceLinkedOreFeature)level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Jugcraft.id("ore_quartz_secondary"));
        BlockPos forest=new BlockPos(0,40,-320);
        fill(level,forest.offset(-12,-8,-12),forest.offset(12,8,12),Blocks.STONE);
        fill(level,new BlockPos(-12,197,-332),new BlockPos(12,207,-308),Blocks.AIR);
        fill(level,new BlockPos(-12,197,-332),new BlockPos(12,200,-308),Blocks.GRASS_BLOCK);
        check(secondary.place(level,level.getChunkSource().getGenerator(),RandomSource.create(721),forest),"Secondary vein uses the forest surface over caves");
        for(BlockPos p:BlockPos.betweenClosed(new BlockPos(-12,197,-332),new BlockPos(12,207,-308)))
            check(!ore(level,p)&&!level.getBlockState(p).is(Blocks.STONE),"Secondary quartz must never create a stone outcrop");
	}

	private static BlockPos openViewingRoom(ServerLevel level, Sample sample, BlockPos outcrop) {
		Set<BlockPos> pocket = components(sample.underground).stream().filter(p -> p.size() >= 5 && p.stream().allMatch(v -> v.getY() < 70))
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
		Jugcraft.LOGGER.info("[quartz] Viewing room camera {}: surrounding rock removed, all natural quartz preserved", camera);
		try { Files.writeString(Path.of("quartz-gallery.txt"), xyz(camera)); } catch (IOException e) { throw new RuntimeException(e); }
		return camera;
	}

	private static void disabled(ClientGameTestContext context) {
		TestWorldSave save; BlockPos fixture;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			fixture = world.getServer().computeOnServer(server -> {
				var level = server.overworld();
				var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getValue(Jugcraft.id("ore_quartz"));
				var secondary = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getValue(Jugcraft.id("ore_quartz_secondary"));
				for (var biome : level.registryAccess().lookupOrThrow(Registries.BIOME).listElements().toList()) {
					check(!level.getChunkSource().getGenerator().getBiomeGenerationSettings(biome).hasFeature(placed), "Disabled quartz remains in biome decoration");
					check(!level.getChunkSource().getGenerator().getBiomeGenerationSettings(biome).hasFeature(secondary), "Silicon switch must disable secondary quartz acquisition");
				}
				check(level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, Jugcraft.id("starter_workshop/silicon"))).isEmpty(), "Disabled starter silicon recipe still acquired");
				BlockPos pos = server.getPlayerList().getPlayers().getFirst().blockPosition().offset(2, 0, 0);
				level.setBlock(pos, JugcraftMaterials.QUARTZ.ore.defaultBlockState(), 2);
				level.setBlock(pos.above(), JugcraftMaterials.QUARTZ.deepslateOre.defaultBlockState(), 2);
				server.getPlayerList().getPlayers().getFirst().getInventory().add(new net.minecraft.world.item.ItemStack(JugcraftMaterials.QUARTZ.item, 7));
				return pos;
			});
			save = world.getWorldSave();
		}
		try (TestSingleplayerContext reopened = save.open()) {
			reopened.getServer().runOnServer(server -> {
				check(ore(server.overworld(), fixture) && ore(server.overworld(), fixture.above()), "Disabled quartz blocks lost across save/reopen");
				var inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
				check(inventory.countItem(JugcraftMaterials.QUARTZ.item) == 7, "Disabled quartz items lost across save/reopen");
			});
		}
		Jugcraft.LOGGER.info("[quartz] Disabled acquisition: no biome decoration/starter silicon; saved ores and seven items survived reopen");
	}

	private static void writeItinerary(Sample sample, String seed, BlockPos outcrop) {
		try {
			Path folder = Path.of("../../quartz-silicon-preview"); Files.createDirectories(folder);
			StringBuilder text = new StringBuilder("Quartz and Starter Silicon\nSeed: " + seed + "\nCreative mode; commands enabled. Surveyed deposits generated naturally; the two workshop ore samples are placed. A lit viewing room removes surrounding rock from one pocket; ore is preserved.\n\nStart at the workshop: /tp @s 512 240 508\nPump with the left button while the menu stays open. The furnace marked SHAFT SILICON demonstrates shaft-powered air.\n\nNatural surface outcrop: /tp @s " + xyz(outcrop.offset(6, 4, 8)) + "\n\nLit underground viewing room: /tp @s " + Files.readString(Path.of("quartz-gallery.txt")) + "\n\nFor natural underground pockets (including the viewing-room pocket): /gamemode spectator\n");
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
    private static final BlockPos WORK=new BlockPos(512,240,512);
    private static int savedWork,savedHeat;
    private static WorkshopBlockEntity work(ServerLevel l){return (WorkshopBlockEntity)l.getBlockEntity(WORK);}
    private static ItemStack stack(String id,int n){return StarterWorkshopGameTests.stack(id,n);}
    private static void workshop(ClientGameTestContext c,TestSingleplayerContext world){
        world.getServer().runOnServer(s->{
            var l=s.overworld();
            for(int x=-4;x<17;x++)for(int z=-6;z<6;z++){
                l.setBlock(WORK.offset(x,-1,z),(z%4==0?Blocks.STONE_BRICKS:Blocks.OAK_PLANKS).defaultBlockState(),3);
                for(int y=0;y<5;y++)l.setBlock(WORK.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
            }
            for(int dx:List.of(0,8)){
                var pos=WORK.east(dx);var block=JugcraftWorkshop.BLOCKS[2];var state=block.defaultBlockState();
                l.setBlock(pos,state,3);block.setPlacedBy(l,pos,state,null,ItemStack.EMPTY);
                var f=(WorkshopBlockEntity)l.getBlockEntity(pos);check(f.select(Jugcraft.id("starter_workshop/silicon")),"Silicon catalog entry");
                f.setItem(0,new ItemStack(Items.QUARTZ));f.setItem(1,new ItemStack(Items.CHARCOAL));f.setItem(4,new ItemStack(Items.COAL,8));
                f.setItem(6,stack("jugcraft:basic_ceramic_lining",1));f.setItem(7,stack("jugcraft:ceramic_crucible",1));f.setItem(8,stack("jugcraft:crucible_heat_insert",1));
                l.setBlock(pos.east(),JugcraftWorkshop.BELLOWS.defaultBlockState().setValue(BellowsBlock.FACING,Direction.WEST),3);
                sign(l,pos.south(2),dx==0?"MANUAL SILICON":"SHAFT SILICON","Quartz + charcoal","Separate fuel","10 seconds / each");
            }
            l.setBlock(WORK.east(10),JugcraftKinetics.IRON_SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS,Direction.Axis.X),3);
            l.setBlock(WORK.east(11),JugcraftKinetics.HAND_CRANK.defaultBlockState().setValue(HandCrankBlock.FACING,Direction.WEST),3);
            ((HandCrankBlockEntity)l.getBlockEntity(WORK.east(11))).addTurns(240);KineticNetworks.invalidate(l);
            BlockPos chest=WORK.offset(4,0,-4);l.setBlock(chest,Blocks.CHEST.defaultBlockState(),3);var supplies=(ChestBlockEntity)l.getBlockEntity(chest);int slot=0;
            for(String id:List.of("minecraft:quartz","minecraft:charcoal","minecraft:coal","jugcraft:quartz_ore","jugcraft:deepslate_quartz_ore","jugcraft:crucible_bellows","jugcraft:crucible_heat_insert","jugcraft:ceramic_crucible","jugcraft:basic_ceramic_lining","jugcraft:iron_shaft","jugcraft:hand_crank","jugcraft:small_crucible_furnace")) supplies.setItem(slot++,stack(id,32));
            l.setBlock(chest.east(),Blocks.CRAFTING_TABLE.defaultBlockState(),3);
            l.setBlock(WORK.west(3),JugcraftMaterials.QUARTZ.ore.defaultBlockState(),3);
            l.setBlock(WORK.west(3).south(),JugcraftMaterials.QUARTZ.deepslateOre.defaultBlockState(),3);
            sign(l,chest.west(),"QUARTZ / SILICON","Supplies here","Handbook: B","Mine with stone+");
        });
        world.getServer().runCommand("tp @a 513.5 240 510 0 15");c.waitTicks(30);world.getConnection().waitForChunksRender();
        c.getInput().lookAt(WORK.east());c.getInput().pressKey(o->o.keyUse);c.waitForScreen(WorkshopScreen.class);c.waitTicks(5);
        clickPump(c);c.waitTicks(45);clickPump(c);c.waitTicks(5);
        int paused=world.getServer().computeOnServer(s->work(s.overworld()).progress());check(paused>0&&paused<200,"Real toggle starts silicon");
        c.waitTicks(20);check(world.getServer().computeOnServer(s->work(s.overworld()).progress())==paused,"Toggle pauses");
        c.takeScreenshot("quartz_silicon_manual_paused");clickPump(c);c.waitTicks(210-paused);close(c);
        world.getServer().runOnServer(s->{var l=s.overworld();check(work(l).getItem(5).is(JugcraftMaterials.SILICON)&&work(l).getItem(5).getCount()==1,"Manual 1:1 result");
            var powered=(WorkshopBlockEntity)l.getBlockEntity(WORK.east(8));check(powered.getItem(5).is(JugcraftMaterials.SILICON)&&powered.getItem(5).getCount()==1,"Shaft 1:1 result");
            work(l).setItem(0,new ItemStack(Items.QUARTZ,8));work(l).setItem(1,new ItemStack(Items.CHARCOAL,8));
        });
        c.getInput().pressKey(o->o.keyUse);c.waitForScreen(WorkshopScreen.class);c.waitTicks(5);clickPump(c);c.waitTicks(35);close(c);
        savedWork=world.getServer().computeOnServer(s->work(s.overworld()).progress());savedHeat=world.getServer().computeOnServer(s->work(s.overworld()).fuelCredit());
        c.waitTicks(25);check(world.getServer().computeOnServer(s->work(s.overworld()).progress())==savedWork,"Menu close pauses pumping");
        c.getInput().pressKey(o->o.keyUse);c.waitForScreen(WorkshopScreen.class);c.waitTicks(15);check(world.getServer().computeOnServer(s->work(s.overworld()).progress())==savedWork,"Reopen starts disabled");close(c);
        world.getServer().runCommand("tp @a 518 244 504 0 25");c.waitTicks(30);c.getInput().lookAt(WORK.east(5));screenshot(c,"quartz_silicon_workshop");
        for(String item:List.of("jugcraft:engineers_handbook","minecraft:quartz 64","minecraft:charcoal 64","minecraft:coal 64","jugcraft:crucible_bellows","jugcraft:crucible_heat_insert","minecraft:stone_pickaxe"))world.getServer().runCommand("give @a "+item);
    }
    private static void clickPump(ClientGameTestContext c){
        double[] xy=c.computeOnClient(client->{var screen=(WorkshopScreen)client.gui.screen();return new double[]{((screen.width-300)/2.0+50)/MouseHandler.getScaledXPos(client.getWindow(),1),((screen.height-236)/2.0+197)/MouseHandler.getScaledYPos(client.getWindow(),1)};});
        c.getInput().setCursorPos(xy[0],xy[1]);c.waitTick();c.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);c.waitTick();c.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }
    private static void close(ClientGameTestContext c){c.runOnClient(client->client.player.closeContainer());c.waitTicks(5);}
    private static void sign(ServerLevel l,BlockPos p,String... lines){
        l.setBlock(p,Blocks.OAK_SIGN.defaultBlockState(),3);if(l.getBlockEntity(p) instanceof SignBlockEntity sign){var t=Arrays.stream(lines).map(Component::literal).map(v->(Component)v).toList();sign.setText(new SignText(t,t,DyeColor.BLACK,false),SignTextSlot.FRONT);sign.setChanged();}
    }

	private static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); }
}
