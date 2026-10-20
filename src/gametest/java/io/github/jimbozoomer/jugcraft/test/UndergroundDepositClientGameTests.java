package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.creatures.chima.Chima;
import io.github.jimbozoomer.jugcraft.creatures.chima.ChimaMod;
import io.github.jimbozoomer.jugcraft.deposit.*;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;

/** Real chunk decoration, rendered cutaways, a working rig, and a save/reopen of finite reserves and progress. */
public class UndergroundDepositClientGameTests implements FabricClientGameTest {
	private static final Path EXPORT = Path.of("../../underground-deposits-preview");
	private static final BlockPos ORIGIN = new BlockPos(512, 0, 512);
	private static final BlockPos RIG = ORIGIN.offset(8, 247, 8), RESERVE = ORIGIN.offset(7, 239, 9);
	private static final BlockPos START = ORIGIN.offset(4, 249, -1);
	@Override public void runTest(ClientGameTestContext context) {
		TestWorldSave save; Path source; UUID[] companion;
		Map<UndergroundDepositFeature.Family, Set<BlockPos>> natural = new EnumMap<>(UndergroundDepositFeature.Family.class);
		Map<String, BlockPos> stops = new LinkedHashMap<>();
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(creator -> {
			creator.setName("Underground Deposit Inspection");
			creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
			creator.setSeed("jugcraft-underground-deposits-1");
		}).create()) {
			world.getServer().runCommand("gamerule minecraft:send_command_feedback false");
			world.getServer().runCommand("gamerule minecraft:advance_time false");
			world.getServer().runCommand("gamerule minecraft:advance_weather false");
			world.getServer().runCommand("time set noon"); world.getServer().runCommand("weather clear");
			for (var family : UndergroundDepositFeature.Family.values()) {
				var bodies = world.getServer().computeOnServer(server -> findNatural(server.overworld(), family));
				natural.put(family, bodies);
				stops.put(family.id(), world.getServer().computeOnServer(server -> cutaway(server.overworld(), bodies)));
				Jugcraft.LOGGER.info("[bulk-deposits] {} natural body: {} blocks, camera {}", family, bodies.size(), stops.get(family.id()));
			}
			world.getServer().runOnServer(server -> gallery(server.overworld()));
			companion = world.getServer().computeOnServer(server -> {
				var owner = server.getPlayerList().getPlayers().getFirst();
				var cub = ChimaMod.CHIMA.create(server.overworld(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				check(cub != null, "Chima must be registered beside the drill");
				cub.tameTo(owner); cub.setSkin(Chima.Skin.SHARK); cub.setHealth(10);
				cub.snapTo(RIG.getX() - 2.5, RIG.getY(), RIG.getZ() - 1.5, 180, 0);
				server.overworld().addFreshEntity(cub);
				return new UUID[]{cub.getUUID(), owner.getUUID()};
			});
			for (var stop : stops.entrySet()) {
				world.getServer().runCommand("tp @a " + xyz(stop.getValue()));
				fly(world); context.waitTicks(30); world.getConnection().waitForChunksRender();
				context.getInput().lookAt(stop.getValue().offset(0, -1, 8)); context.waitTicks(6);
				context.takeScreenshot("jugcraft_bulk_" + stop.getKey() + "_natural_cutaway");
			}
			world.getServer().runCommand("tp @a " + xyz(START)); fly(world);
			context.waitTicks(40); world.getConnection().waitForChunksRender();
			context.getInput().lookAt(RIG.above(2)); context.waitTicks(6);
			context.takeScreenshot("jugcraft_bulk_deep_drill_gallery");
			world.getServer().runCommand("tp @a " + xyz(RIG.offset(-4, 1, -5)));
			context.waitTicks(10); context.getInput().lookAt(RIG.above(2)); context.waitTicks(6);
			context.takeScreenshot("jugcraft_bulk_tall_mast_and_support");
			world.getServer().runCommand("tp @a " + xyz(RIG.north(3)));
			context.waitTicks(10); context.getInput().lookAt(RIG.above()); context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(io.github.jimbozoomer.jugcraft.client.MachineScreen.class);
			context.waitTicks(10); context.takeScreenshot("jugcraft_bulk_deep_drill_progress");
			context.runOnClient(client -> client.player.closeContainer());
			world.getServer().runCommand("tp @a " + xyz(START)); fly(world);
			context.waitTicks(10); context.getInput().lookAt(RIG.above(2));
			world.getServer().runCommand("setworldspawn " + xyz(START));
			world.getServer().runCommand("gamerule minecraft:respawn_radius 0");
			for (String item : List.of("jugcraft:prospector", "jugcraft:deep_drilling_assembly", "jugcraft:deposit_drill",
					"jugcraft:coal_generator", "minecraft:coal 64", "minecraft:chest", "jugcraft:copper_cable 32"))
				world.getServer().runCommand("give @a " + item);
			source = world.getServer().computeOnServer(server -> server.getWorldPath(LevelResource.ROOT));
			save = world.getWorldSave();
		}
		try (var reopened = save.open()) {
			context.waitTicks(40); reopened.getConnection().waitForChunksRender();
			reopened.getServer().runOnServer(server -> {
				var level = server.overworld();
				check(Deposits.remaining(level, RESERVE) == 777, "Finite reserve changed across real save/reopen");
				var drill = (MachineBlockEntity) level.getBlockEntity(RIG);
				check(drill != null && drill.depositDepth() == 8 && drill.processingProgress() == 123, "Drill depth/partial progress not persisted");
				check(DeepDrillingAssemblyBlock.installed(level, RIG, drill.getBlockState()), "Two-block mast lost on reopen");
				check(level.getEntity(companion[0]) instanceof Chima, "Companion lost beside the saved drill");
				var cub = (Chima) level.getEntity(companion[0]);
				check(cub.tame() && companion[1].equals(cub.owner()) && cub.sitting()
						&& cub.skin() == Chima.Skin.SHARK && cub.getHealth() == 10,
						"Chima owner, sitting, appearance or health changed across save/reopen");
				for (var body : natural.values()) for (var pos : body)
					check(level.getBlockState(pos).getBlock() instanceof DepositBlock, "Natural deposit lost on reopen: " + pos);
			});
		}
		copySave(source, EXPORT.resolve("Underground Deposit Inspection"));
		try {
			StringBuilder tour = new StringBuilder("UNDERGROUND DEPOSIT INSPECTION\nSeed: jugcraft-underground-deposits-1\n\nStart /tp @s " + xyz(START)
					+ "\nThe elevated gallery shows artificial cutaways of the production shapes. Each natural stop below was found through ordinary chunk generation; surrounding rock was removed for viewing, preserving every deposit block.\n\n");
			stops.forEach((family, pos) -> tour.append(family).append(": /tp @s ").append(xyz(pos)).append('\n'));
			tour.append("\nThe tall rig is paused at depth 8 with 123/300 extraction ticks saved and 777 iron units left. Place the supplied coal generator at 521 247 520 and fill it with coal to resume. The chest at 520 247 519 receives output. Right-click the mast to read depth; open the rig for progress. The shorter rig shows the unchanged basic model. Right-click it with the supplied assembly to upgrade it.\n\nNatural deposits hold 1,000 units/block. Stone and deepslate share one item/family/15 seconds. Deep mode: 32 JE/t, 9x9, 128 below base, one boring layer/second. Speed/efficiency cards affect extraction. No holes are cut into terrain. At mountain elevations, move the rig down if a deposit is beyond 128 blocks.\n");
			Files.writeString(EXPORT.resolve("TOUR.txt"), tour);
		} catch (IOException e) { throw new RuntimeException(e); }
	}

	private static Set<BlockPos> findNatural(ServerLevel level, UndergroundDepositFeature.Family family) {
		String biome = switch (family) { case COAL -> "minecraft:forest"; case IRON, TIN -> "jugcraft:shield"; case COPPER -> "minecraft:badlands"; };
		var feature = (UndergroundDepositFeature) level.registryAccess().lookupOrThrow(Registries.FEATURE)
				.getValue(Jugcraft.id("underground_" + family.id() + "_deposit"));
		check(feature != null, "Missing registered feature " + family);
		var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getValue(Jugcraft.id("underground_" + family.id() + "_deposit"));
		for (var key : List.of(net.minecraft.world.level.biome.Biomes.FOREST, net.minecraft.world.level.biome.Biomes.LUSH_CAVES)) {
			var holder = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key);
			check(level.getChunkSource().getGenerator().getBiomeGenerationSettings(holder).hasFeature(placed), "Missing surface/cave biome attachment: " + family + " / " + key);
		}
		for (int attempt = 0; attempt < 3; attempt++) {
			var found = level.findClosestBiome3d(holder -> holder.is(net.minecraft.resources.Identifier.parse(biome)),
					new BlockPos(attempt * 6000, 80, 0), 8192, 32, 64);
			check(found != null, "No target biome " + biome);
			int cx = found.getFirst().getX() >> 4, cz = found.getFirst().getZ() >> 4;
			int radius = 9 + attempt * 3;
			for (int x = cx - radius; x <= cx + radius; x++) for (int z = cz - radius; z <= cz + radius; z++) level.getChunk(x, z);
			Set<BlockPos> positions = new LinkedHashSet<>();
			for (int x = (cx - radius + 1) * 16; x < (cx + radius) * 16; x++)
				for (int z = (cz - radius + 1) * 16; z < (cz + radius) * 16; z++) {
					int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
					for (int y = family.minY - family.maxHeight; y <= Math.min(surface - 13, family.maxY + family.maxHeight); y++) {
						BlockPos p = new BlockPos(x, y, z);
						if (level.getBlockState(p).is(JugcraftDeposits.BLOCKS.get(family.id() + "_deposit"))
								|| level.getBlockState(p).is(JugcraftDeposits.BLOCKS.get("deepslate_" + family.id() + "_deposit"))) positions.add(p);
					}
				}
			for (Set<BlockPos> body : components(positions)) if (body.size() >= family.minBlocks && body.size() <= family.maxBlocks) return body;
			Jugcraft.LOGGER.info("[bulk-deposits] {} search {}: {} blocks, expanding survey", family, attempt, positions.size());
		}
		throw new AssertionError("No full naturally generated " + family + " body after bounded search");
	}

	private static List<Set<BlockPos>> components(Set<BlockPos> blocks) {
		Set<BlockPos> remaining = new LinkedHashSet<>(blocks); List<Set<BlockPos>> result = new ArrayList<>();
		while (!remaining.isEmpty()) {
			Set<BlockPos> part = new LinkedHashSet<>(); ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			var first = remaining.iterator().next(); remaining.remove(first); queue.add(first);
			while (!queue.isEmpty()) {
				var pos = queue.remove(); part.add(pos);
				for (var direction : Direction.values()) if (remaining.remove(pos.relative(direction))) queue.add(pos.relative(direction));
			}
			result.add(part);
		}
		return result;
	}

	private static BlockPos cutaway(ServerLevel level, Set<BlockPos> body) {
		int x0 = body.stream().mapToInt(BlockPos::getX).min().orElseThrow() - 3, x1 = body.stream().mapToInt(BlockPos::getX).max().orElseThrow() + 3;
		int y0 = body.stream().mapToInt(BlockPos::getY).min().orElseThrow() - 2, y1 = body.stream().mapToInt(BlockPos::getY).max().orElseThrow() + 4;
		int z0 = body.stream().mapToInt(BlockPos::getZ).min().orElseThrow() - 10, z1 = body.stream().mapToInt(BlockPos::getZ).max().orElseThrow() + 3;
		for (BlockPos p : BlockPos.betweenClosed(new BlockPos(x0, y0, z0), new BlockPos(x1, y1, z1))) {
			if (level.getBlockState(p).getBlock() instanceof DepositBlock) continue;
			boolean wall = p.getX() == x0 || p.getX() == x1 || p.getY() == y1 || p.getZ() == z0 || p.getZ() == z1;
			level.setBlock(p, (p.getY() == y0 ? Blocks.SEA_LANTERN : wall ? Blocks.GLASS : Blocks.AIR).defaultBlockState(), 2);
		}
		return new BlockPos((x0 + x1) / 2, y0 + 4, z0 + 1);
	}

	private static void gallery(ServerLevel level) {
		UndergroundDepositGameTests.fill(level, ORIGIN.offset(-8, 244, -6), ORIGIN.offset(88, 259, 26), Blocks.AIR);
		UndergroundDepositGameTests.fill(level, ORIGIN.offset(-8, 246, -6), ORIGIN.offset(88, 246, 26), Blocks.SMOOTH_STONE);
		for (int x = -6; x <= 86; x += 4) for (int z = -4; z <= 24; z += 4)
			level.setBlock(ORIGIN.offset(x, 246, z), Blocks.SEA_LANTERN.defaultBlockState(), 2);
		int x = 22;
		for (var family : UndergroundDepositFeature.Family.values()) {
			for (var p : UndergroundDepositFeature.plan(family, RandomSource.create(300 + family.ordinal()), ORIGIN.offset(x, 251, 12))) {
				boolean deep = p.getX() >= ORIGIN.getX() + x;
				level.setBlock(p, JugcraftDeposits.BLOCKS.get((deep ? "deepslate_" : "") + family.id() + "_deposit").defaultBlockState(), 2);
			}
			sign(level, ORIGIN.offset(x, 247, 1), family.id().toUpperCase(Locale.ROOT), "CUTAWAY", "Stone / Deepslate", "1000 units/block");
			x += 19;
		}
		UndergroundDepositGameTests.placeRig(level, ORIGIN.offset(0, 247, 8), false);
		var drill = UndergroundDepositGameTests.placeRig(level, RIG, true);
		level.setBlock(RIG.north(), Blocks.CHEST.defaultBlockState(), 3);
		level.setBlock(RESERVE, JugcraftDeposits.BLOCKS.get("deepslate_iron_deposit").defaultBlockState(), 3);
		Deposits.extract(level, RESERVE, 223);
		UndergroundDepositGameTests.tick(level, drill, 8 * 20 + 7 + 123, true);
		((SimpleEnergyStorage) drill.energyFor(null)).setAmount(0);
		check(drill.depositDepth() == 8 && drill.processingProgress() == 123, "Gallery setup progress");
		sign(level, ORIGIN.offset(0, 247, 4), "BASIC DRILL", "5 x 5 x 3", "16 JE/t", "Use mast to upgrade");
		sign(level, ORIGIN.offset(8, 247, 4), "DEEP DRILL", "9 x 9 x 128", "32 JE/t", "Power goes on right");
	}

	private static void sign(ServerLevel level, BlockPos pos, String... lines) {
		level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState(), 3);
		if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
			var text = java.util.Arrays.stream(lines).map(Component::literal).map(c -> (Component)c).toList();
			sign.setText(new net.minecraft.world.level.block.entity.SignText(text, text, net.minecraft.world.item.DyeColor.BLACK, false),
					net.minecraft.world.level.block.entity.SignTextSlot.FRONT); sign.setChanged();
		}
	}
	private static void fly(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst(); p.getAbilities().flying = true; p.onUpdateAbilities(); });
	}
	private static String xyz(BlockPos p) { return p.getX() + ".5 " + p.getY() + " " + p.getZ() + ".5"; }
	private static void copySave(Path source, Path destination) {
		try (var paths = Files.walk(source)) {
			for (Path file : paths.toList()) {
				Path target = destination.resolve(source.relativize(file).toString());
				if (Files.isDirectory(file)) Files.createDirectories(target);
				else if (!file.getFileName().toString().equals("session.lock")) Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) { throw new RuntimeException(e); }
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
