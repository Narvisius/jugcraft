package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;

/** The owner's pine timber approval gallery: nine wood forms only, with no trees or new world generation. */
public class PineWoodClientGameTests implements FabricClientGameTest {
	private static final List<String> PINE = List.of("pine_log", "pine_wood", "stripped_pine_log", "stripped_pine_wood",
			"pine_planks", "pine_stairs", "pine_slab", "pine_fence", "pine_fence_gate");
	private static final String[] TITLES = {"Pine log", "Pine wood", "Stripped log", "Stripped wood", "Pine planks"};

	@Override
	public void runTest(ClientGameTestContext context) {
		TestWorldSave save;
		Path source;
		BlockPos origin;
		Map<BlockPos, BlockState> expected;
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(creator -> {
			creator.setName("Pine Wood Gallery");
			creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			creator.setSeed("jugcraft-pine-wood-approval");
			creator.setGenerateStructures(false);
			for (WorldCreationUiState.WorldTypeEntry entry : creator.getNormalPresetList()) {
				if (entry.preset().is(WorldPresets.FLAT)) creator.setWorldType(entry);
			}
		}).create()) {
			world.getConnection().waitForChunksRender();
			origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			var server = world.getServer();
			for (String command : List.of("difficulty peaceful", "time set noon", "weather clear",
					"gamerule minecraft:send_command_feedback false", "gamerule minecraft:advance_time false",
					"gamerule minecraft:advance_weather false", "gamerule minecraft:random_tick_speed 0",
					"gamerule minecraft:spawn_mobs false", "gamerule minecraft:respawn_radius 0")) server.runCommand(command);
			expected = server.computeOnServer(minecraft -> build(minecraft.overworld(), origin));
			for (String id : PINE) server.runCommand("give @a jugcraft:" + id + " 16");
			server.runOnServer(minecraft -> assertGallery(minecraft.overworld(), expected));
			context.waitTicks(30);

			shoot(context, world, origin.offset(0, 5, 5), origin.offset(0, 2, -17), "jugcraft_pine_01_nine_forms");
			shoot(context, world, origin.offset(0, 4, -24), origin.offset(0, 2, -46), "jugcraft_pine_02_warm_wood_comparison");
			shoot(context, world, origin.offset(0, 4, -47), origin.offset(0, 2, -69), "jugcraft_pine_03_conifer_comparison");
			shoot(context, world, origin.offset(45, 7, 7), origin.offset(32, 3, -12), "jugcraft_pine_04_building_materials");

			// Export at a comfortable walking height, facing the pine samples; screenshots use temporary flying cameras.
			BlockPos start = origin.offset(0, 0, 5);
			server.runCommand("tp @a " + xyz(start) + " 180 0");
			server.runCommand("setworldspawn " + xyz(start));
			server.runOnServer(minecraft -> {
				var player = minecraft.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = false;
				player.onUpdateAbilities();
			});
			context.waitTicks(20);
			source = server.computeOnServer(minecraft -> minecraft.getWorldPath(LevelResource.ROOT));
			save = world.getWorldSave();
		}
		try (TestSingleplayerContext reopened = save.open()) {
			reopened.getConnection().waitForChunksRender();
			reopened.getServer().runOnServer(minecraft -> {
				assertGallery(minecraft.overworld(), expected);
				var inventory = minecraft.getPlayerList().getPlayers().getFirst().getInventory();
				for (String id : PINE) check(inventory.countItem(state("jugcraft:" + id).getBlock().asItem()) == 16,
						"Pine inventory failed to persist: " + id);
			});
		}
		Path destination = exportFolder();
		copySave(source, destination.resolve("Pine Wood Gallery"));
		writeTour(destination, origin);
		Jugcraft.LOGGER.info("[pine wood] All nine forms, axes, end grain and inventory survived save/reopen; {} checked blocks. Export: {}",
				expected.size(), destination.resolve("Pine Wood Gallery"));
	}

	private static Map<BlockPos, BlockState> build(ServerLevel level, BlockPos origin) {
		// Flat creative showroom. The floor uses neutral stone so surrounding colour does not tint timber comparisons.
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-27, -1, -75), origin.offset(45, -1, 14))) {
			level.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
		}
		List<BlockPos> checked = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			int x = -14 + i * 6;
			BlockState sample = state("jugcraft:" + PINE.get(i));
			for (int dx = 0; dx < 3; dx++) for (int dy = 1; dy <= 3; dy++) {
				BlockState shown = sample.hasProperty(BlockStateProperties.AXIS)
						? sample.setValue(BlockStateProperties.AXIS, Direction.Axis.values()[dx]) : sample;
				put(level, origin.offset(x + dx, dy, -20), shown, checked);
			}
			label(level, origin.offset(x + 1, 0, -19), TITLES[i], i < 4 ? "X / Y / Z axes" : "3 x 3 tiling");
		}
		// Distinct samples of the remaining four forms, with connected fences and an open/closed gate pair.
		put(level, origin.offset(-12, 0, -12), state("jugcraft:pine_stairs").setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), checked);
		put(level, origin.offset(-11, 0, -12), state("jugcraft:pine_stairs").setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), checked);
		put(level, origin.offset(-5, 0, -12), state("jugcraft:pine_slab"), checked);
		put(level, origin.offset(-4, 0, -12), state("jugcraft:pine_slab").setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP), checked);
		put(level, origin.offset(-3, 0, -12), state("jugcraft:pine_slab").setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE), checked);
		for (int x = 2; x <= 4; x++) put(level, origin.offset(x, 0, -12), state("jugcraft:pine_fence"), checked);
		put(level, origin.offset(9, 0, -12), state("jugcraft:pine_fence_gate").setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), checked);
		put(level, origin.offset(11, 0, -12), state("jugcraft:pine_fence_gate").setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
				.setValue(BlockStateProperties.OPEN, true), checked);
		label(level, origin.offset(-11, 0, -10), "Pine stairs", "Walk / inspect");
		label(level, origin.offset(-4, 0, -10), "Pine slabs", "Low / high / full");
		label(level, origin.offset(3, 0, -10), "Pine fence", "Connected rails");
		label(level, origin.offset(10, 0, -10), "Pine gates", "Closed / open");
		comparisonRow(level, origin, -46, new String[] {"jugcraft:chestnut", "jugcraft:eucalyptus", "jugcraft:pine", "minecraft:birch", "minecraft:oak"});
		comparisonRow(level, origin, -69, new String[] {"jugcraft:fir", "jugcraft:larch", "jugcraft:pine", "jugcraft:cedar", "minecraft:spruce"});
		building(level, origin);
		label(level, origin.offset(0, 1, 10), "PINE WOOD", "Approval gallery", "Samples north", "Pavilion east");
		Map<BlockPos, BlockState> snapshot = new LinkedHashMap<>();
		// Capture after neighbours settle: connected fences should be compared in their final state.
		for (BlockPos pos : checked) snapshot.put(pos.immutable(), level.getBlockState(pos));
		return snapshot;
	}

	static void comparisonRow(ServerLevel level, BlockPos origin, int z, String[] woods) {
		for (int i = 0; i < woods.length; i++) {
			String wood = woods[i];
			int x = -20 + i * 9;
			String namespace = wood.substring(0, wood.indexOf(':') + 1);
			String name = wood.substring(namespace.length());
			String stripped = namespace + "stripped_" + name;
			// Nether timber is also a useful native colour reference (batch 6's juniper).
			boolean fungus = namespace.equals("minecraft:") && (name.equals("crimson") || name.equals("warped"));
			String trunk = fungus ? "_stem" : "_log", bark = fungus ? "_hyphae" : "_wood";
			String[] blocks = {wood + trunk, stripped + trunk, wood + "_planks", wood + bark, stripped + bark};
			for (int column = 0; column < blocks.length; column++) for (int y = 1; y <= 3; y++) {
				BlockState block = state(blocks[column]);
				if (block.hasProperty(BlockStateProperties.AXIS)) block = block.setValue(BlockStateProperties.AXIS,
						y == 3 ? Direction.Axis.Z : Direction.Axis.Y);
				level.setBlock(origin.offset(x + column, y, z), block, 3);
			}
			level.setBlock(origin.offset(x, 0, z + 1), state(wood + "_stairs").setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 3);
			level.setBlock(origin.offset(x + 1, 0, z + 1), state(wood + "_slab"), 3);
			level.setBlock(origin.offset(x + 3, 0, z + 1), state(wood + "_fence"), 3);
			level.setBlock(origin.offset(x + 4, 0, z + 1), state(wood + "_fence_gate"), 3);
			label(level, origin.offset(x + 2, 0, z + 2), name.toUpperCase(Locale.ROOT), namespace.equals("minecraft:") ? "Vanilla reference" : "Jugcraft timber");
		}
	}

	private static void building(ServerLevel level, BlockPos origin) {
		// An open pavilion shows repeated planks, exposed log ends, full-bark beams, stripped framing and a gabled roof.
		for (int x = 27; x <= 37; x++) for (int z = -19; z <= -7; z++) {
			level.setBlock(origin.offset(x, 0, z), state("jugcraft:pine_planks"), 3);
		}
		for (int x : new int[] {28, 36}) for (int z : new int[] {-18, -8}) for (int y = 1; y <= 4; y++) {
			level.setBlock(origin.offset(x, y, z), state(x == 28 ? "jugcraft:pine_log" : "jugcraft:stripped_pine_log"), 3);
		}
		for (int x = 28; x <= 36; x++) for (int z : new int[] {-18, -8}) {
			level.setBlock(origin.offset(x, 4, z), state("jugcraft:pine_wood").setValue(BlockStateProperties.AXIS, Direction.Axis.X), 3);
		}
		for (int z = -18; z <= -8; z++) for (int x : new int[] {28, 36}) {
			level.setBlock(origin.offset(x, 4, z), state("jugcraft:stripped_pine_wood").setValue(BlockStateProperties.AXIS, Direction.Axis.Z), 3);
		}
		for (int step = 0; step < 5; step++) for (int z = -20; z <= -6; z++) {
			level.setBlock(origin.offset(27 + step, 5 + step, z), state("jugcraft:pine_stairs")
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), 3);
			level.setBlock(origin.offset(37 - step, 5 + step, z), state("jugcraft:pine_stairs")
					.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), 3);
			level.setBlock(origin.offset(32, 9, z), state("jugcraft:pine_slab"), 3);
		}
		for (int z = -18; z <= -8; z++) for (int x : new int[] {27, 37}) level.setBlock(origin.offset(x, 1, z), state("jugcraft:pine_fence"), 3);
		for (int x = 27; x <= 37; x++) {
			level.setBlock(origin.offset(x, 1, -19), state("jugcraft:pine_fence"), 3);
			level.setBlock(origin.offset(x, 1, -7), state(x == 32 ? "jugcraft:pine_fence_gate" : "jugcraft:pine_fence"), 3);
		}
		for (int x = 31; x <= 33; x++) level.setBlock(origin.offset(x, 0, -6), state("jugcraft:pine_stairs")
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 3);
		label(level, origin.offset(32, 1, -3), "PINE PAVILION", "Roof / deck", "Logs / framing", "Fence / gate");
	}

	static void label(ServerLevel level, BlockPos pos, String... words) {
		level.setBlock(pos.north(), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
		level.setBlock(pos, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.SOUTH), 3);
		check(level.getBlockEntity(pos) instanceof SignBlockEntity, "Missing gallery label at " + pos);
		var sign = (SignBlockEntity) level.getBlockEntity(pos);
		List<Component> lines = new ArrayList<>();
		for (int i = 0; i < 4; i++) lines.add(Component.literal(i < words.length ? words[i] : ""));
		sign.setText(new SignText(lines, lines, DyeColor.BLACK, false), SignTextSlot.FRONT);
		sign.setWaxed(true);
		sign.setChanged();
		level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
	}

	private static void put(ServerLevel level, BlockPos pos, BlockState state, List<BlockPos> checked) {
		level.setBlock(pos, state, 3);
		checked.add(pos.immutable());
	}

	private static BlockState state(String id) {
		Identifier key = Identifier.parse(id);
		check(BuiltInRegistries.BLOCK.containsKey(key), "Unregistered gallery block: " + id);
		check(BuiltInRegistries.ITEM.containsKey(key), "Unregistered gallery item: " + id);
		return BuiltInRegistries.BLOCK.getValue(key).defaultBlockState();
	}

	private static void assertGallery(ServerLevel level, Map<BlockPos, BlockState> expected) {
		for (var entry : expected.entrySet()) check(level.getBlockState(entry.getKey()).equals(entry.getValue()),
				"Gallery block changed at " + entry.getKey() + ": expected " + entry.getValue());
		for (String id : PINE) check(expected.values().stream().anyMatch(block -> block.is(state("jugcraft:" + id).getBlock())),
				"Gallery does not demonstrate " + id);
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext world, BlockPos camera, BlockPos target, String name) {
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

	private static Path exportFolder() {
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.isRegularFile(root.resolve("build.gradle"))) root = root.getParent();
		check(root != null, "Could not locate repository for the gallery export");
		return root.resolve("build/pine-wood-preview");
	}

	private static void copySave(Path source, Path destination) {
		try (var files = Files.walk(source)) {
			for (Path file : files.toList()) {
				Path target = destination.resolve(source.relativize(file).toString());
				if (Files.isDirectory(file)) Files.createDirectories(target);
				else if (!file.getFileName().toString().equals("session.lock")) Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException exception) { throw new RuntimeException(exception); }
	}

	private static void writeTour(Path destination, BlockPos origin) {
		String text = "Pine Wood Gallery\nCreative mode; this is an authored timber comparison showroom, not a world-generation sample.\n"
				+ "Nine pine forms are supplied in the hotbar. Trees, needles, saplings and biome generation are outside this approval preview.\n\n"
				+ "Pine samples and spawn: /tp @s " + xyz(origin.offset(0, 0, 5)) + " 180 0\n"
				+ "Warm comparison (chestnut / eucalyptus / pine / vanilla birch / vanilla oak): /tp @s " + xyz(origin.offset(0, 0, -36)) + " 180 0\n"
				+ "Conifer comparison (fir / larch / pine / cedar / vanilla spruce): /tp @s " + xyz(origin.offset(0, 0, -59)) + " 180 0\n"
				+ "Pine pavilion: /tp @s " + xyz(origin.offset(32, 0, 0)) + " 180 0\n\n"
				+ "The save was reopened and all nine pine forms, axes and inventory checked before export.\n"
				+ "Development test metadata must be cleaned and the export opened with the ordinary matching client before distributing.\n";
		try { Files.writeString(destination.resolve("TOUR.txt"), text); }
		catch (IOException exception) { throw new RuntimeException(exception); }
	}

	private static String xyz(BlockPos pos) { return pos.getX() + " " + pos.getY() + " " + pos.getZ(); }
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
