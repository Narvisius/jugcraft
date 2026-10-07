package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.GroundCoverBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HangingPlantBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Mandrakes;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the graveyard flora: a little haunted churchyard planted with all of it. Three weathered
 * headstones with spider lilies, snowdrops, deadly nightshade and bleeding hearts at their feet; black roses, foxgloves,
 * funeral lilies and asphodel standing between; withered grass, ghost ferns, grave moss and dead man's fingers in the
 * grass; ghost pipes in a clump; a crypt wall grown over with creeping ivy, potted flowers along its top; an old oak
 * hung with shroud moss; and a row of mandrakes from seedling to ripe, beside a wild one and a root. Shot at dusk, at
 * night (the ghost pipes glow), and up close from an invisible camera stand. CI job {@code client}.
 */
public class GraveyardFloraClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Hide the HUD, hand and chat whatever an earlier test in this client left (CI shares the client tests out).
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set 11000");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 9, y - 3, z - 14, x + 13, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 9, y, z - 14, x + 13, y + 10, z + 8));
			context.waitTicks(10);
			server.runCommand("gamerule minecraft:random_tick_speed 0");
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			// A mandrake root on show in front of its row, half again life size.
			server.runCommand(String.format(Locale.ROOT, "summon minecraft:item_display %.1f %.1f %.1f {item:{id:\"jugcraft:%s\",count:1},"
					+ "item_display:\"fixed\",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],"
					+ "scale:[1.5f,1.5f,1.5f]}}", x - 1.5, y + 0.7, z + 2.5, Mandrakes.ROOT));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 1, y + 5, z + 5, 180, 30, "jugcraft_graveyard_flora");
			watchFrom(context, singleplayer, origin, new Vec3(1.5, 0.8, -2.2), 180.0F, 24.0F, "jugcraft_graveyard_flora_flowers");
			watchFrom(context, singleplayer, origin, new Vec3(1.5, 1.6, -3.0), 180.0F, 6.0F, "jugcraft_graveyard_flora_tall");
			watchFrom(context, singleplayer, origin, new Vec3(-2.5, 1.2, -6.0), 200.0F, 22.0F, "jugcraft_graveyard_flora_wall");
			watchFrom(context, singleplayer, origin, new Vec3(7.0, 0.6, 1.5), 222.0F, -14.0F, "jugcraft_graveyard_flora_shroud_moss");
			watchFrom(context, singleplayer, origin, new Vec3(-2.0, 1.4, 4.5), 180.0F, 34.0F, "jugcraft_graveyard_flora_mandrakes");
			server.runCommand("time set 18000");
			context.waitTicks(20);
			shoot(context, singleplayer, x + 1, y + 5, z + 5, 180, 30, "jugcraft_graveyard_flora_night");
			watchFrom(context, singleplayer, origin, new Vec3(4.0, 1.0, 0.4), 180.0F, 38.0F, "jugcraft_graveyard_flora_ghost_pipes");
			server.runCommand("time set noon");
			server.runCommand("gamerule minecraft:advance_time true");
			server.runCommand("gamerule minecraft:random_tick_speed 3");
		}
	}

	/** Stands the player at (x, y, z) looking along yaw and pitch on a barrier in the air, and takes a picture. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	/** Looks through an invisible armor stand at `at` (from the origin) along yaw and pitch, then gives the camera back. */
	private static void watchFrom(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos origin, Vec3 at, float yaw,
			float pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		int id = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			ArmorStand stand = new ArmorStand(level, origin.getX() + at.x, origin.getY() + at.y, origin.getZ() + at.z);
			stand.snapTo(stand.getX(), stand.getY(), stand.getZ(), yaw, pitch);
			stand.setYHeadRot(yaw);
			stand.setInvisible(true);
			stand.setNoGravity(true);
			level.addFreshEntity(stand);
			return stand.getId();
		});
		context.waitTicks(5);
		context.runOnClient(client -> {
			Entity stand = client.level.getEntity(id);
			if (stand != null) {
				client.setCameraEntity(stand);
			}
		});
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
		context.runOnClient(client -> client.setCameraEntity(client.player));
		server.runOnServer(minecraft -> {
			Entity stand = minecraft.overworld().getEntity(id);
			if (stand != null) {
				stand.discard();
			}
		});
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void put(ServerLevel level, BlockPos origin, int dx, int dy, int dz, BlockState state) {
		level.setBlock(origin.offset(dx, dy, dz), state, Block.UPDATE_ALL);
	}

	private static void plant(ServerLevel level, BlockPos origin, int dx, int dz, String id) {
		put(level, origin, dx, 0, dz, block(id).defaultBlockState());
	}

	private static void tall(ServerLevel level, BlockPos origin, int dx, int dz, String id) {
		DoublePlantBlock.placeAt(level, block(id).defaultBlockState(), origin.offset(dx, 0, dz), Block.UPDATE_ALL);
	}

	/**
	 * The churchyard, north of the origin: headstones along z - 6, flowers at their feet, tall flowers between and behind,
	 * the crypt wall at z - 10 with ivy and pots, the mossy oak to the east, mandrakes in a farmland row near the front.
	 */
	private static void build(ServerLevel level, BlockPos origin) {
		// Three weathered headstones facing the camera.
		String[] stones = {"gothic_headstone", "willow_urn_headstone", "winged_skull_headstone"};
		for (int i = 0; i < stones.length; i++) {
			HeadstoneBlock stone = (HeadstoneBlock) block(stones[i]);
			BlockPos pos = origin.offset(-3 + i * 4, 0, -6);
			BlockState state = stone.defaultBlockState().setValue(HeadstoneBlock.FACING, Direction.SOUTH);
			level.setBlock(pos, state, Block.UPDATE_ALL);
			stone.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
			stone.setWeather(level, pos, 1 + i % 2, false);
		}
		// At their feet, a row of small flowers each.
		String[] small = {"spider_lily", "snowdrop", "deadly_nightshade", "bleeding_heart"};
		for (int i = 0; i < 12; i++) {
			plant(level, origin, -4 + i, -5, small[i % small.length]);
		}
		plant(level, origin, -1, -4, "spider_lily");
		plant(level, origin, 0, -4, "spider_lily");
		plant(level, origin, 4, -4, "snowdrop");
		// Tall flowers standing between and behind the stones.
		tall(level, origin, -1, -6, "black_rose");
		tall(level, origin, 3, -6, "foxglove");
		tall(level, origin, 7, -6, "funeral_lily");
		tall(level, origin, -5, -6, "asphodel");
		tall(level, origin, -1, -8, "foxglove");
		tall(level, origin, 3, -8, "asphodel");
		tall(level, origin, 5, -7, "black_rose");
		tall(level, origin, 1, -8, "funeral_lily");
		// The ground: withered grass, ghost ferns (and two grown tall), grave moss, dead man's fingers, ghost pipes.
		int[][] grass = {{-6, -3}, {-5, -2}, {6, -3}, {8, -5}, {-7, -7}, {9, -8}, {-2, -9}, {2, -9}, {6, -9}, {-6, -9}};
		for (int[] at : grass) {
			plant(level, origin, at[0], at[1], "withered_grass");
		}
		tall(level, origin, -7, -5, "tall_withered_grass");
		tall(level, origin, 9, -6, "tall_withered_grass");
		int[][] ferns = {{-6, -6}, {8, -7}, {-4, -8}, {4, -9}, {0, -9}};
		for (int[] at : ferns) {
			plant(level, origin, at[0], at[1], "ghost_fern");
		}
		tall(level, origin, -7, -9, "large_ghost_fern");
		tall(level, origin, 8, -9, "large_ghost_fern");
		BlockState moss = block("grave_moss").defaultBlockState();
		for (int[] at : new int[][] {{-3, -7}, {1, -7}, {5, -5}, {-2, -3}, {6, -8}}) {
			put(level, origin, at[0], 0, at[1], moss.setValue(GroundCoverBlock.AMOUNT, 1 + Math.floorMod(at[0], 4)));
		}
		for (int[] at : new int[][] {{-5, -8}, {-3, -9}, {7, -8}}) {
			plant(level, origin, at[0], at[1], "dead_mans_fingers");
		}
		for (int[] at : new int[][] {{3, -2}, {4, -2}, {3, -3}, {5, -3}}) {
			plant(level, origin, at[0], at[1], "ghost_pipe");
		}
		// The crypt wall, two high, ivy on its face and potted flowers along its top.
		BlockState crypt = block("crypt_stone").defaultBlockState();
		BlockState ivy = block("creeping_ivy").defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.NORTH), true);
		String[] pots = {"potted_spider_lily", "potted_snowdrop", "potted_ghost_pipe", "potted_deadly_nightshade", "potted_bleeding_heart"};
		for (int dx = -8; dx <= 10; dx++) {
			put(level, origin, dx, 0, -10, crypt);
			put(level, origin, dx, 1, -10, crypt);
			if (Math.floorMod(dx, 3) != 1) {
				put(level, origin, dx, 1, -9, ivy);
				if (Math.floorMod(dx, 2) == 0 && level.getBlockState(origin.offset(dx, 0, -9)).isAir()) {
					put(level, origin, dx, 0, -9, ivy);
				}
			}
			if (Math.floorMod(dx, 4) == 0) {
				put(level, origin, dx, 2, -10, block(pots[Math.floorMod(dx / 4, pots.length)]).defaultBlockState());
			}
		}
		// An old oak to the east, hung with shroud moss.
		BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
		BlockState leaves = Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		for (int dy = 0; dy < 5; dy++) {
			put(level, origin, 10, dy, -3, log);
		}
		for (int dx = 7; dx <= 12; dx++) {
			for (int dz = -5; dz <= -1; dz++) {
				put(level, origin, dx, 5, dz, leaves);
				if (Math.abs(dx - 10) + Math.abs(dz + 3) <= 2) {
					put(level, origin, dx, 6, dz, leaves);
				}
			}
		}
		BlockState strand = block("shroud_moss").defaultBlockState();
		int[][] strands = {{7, -2, 3}, {8, -4, 2}, {9, -1, 3}, {11, -2, 2}, {12, -4, 1}, {8, -1, 1}, {11, -5, 2}};
		for (int[] s : strands) {
			for (int i = 0; i < s[2]; i++) {
				put(level, origin, s[0], 4 - i, s[1], strand.setValue(HangingPlantBlock.TIP, i == s[2] - 1));
			}
		}
		// Mandrakes in a farmland row by the front, seedling to ripe, a wild one beside them.
		CropBlock mandrake = (CropBlock) block(Mandrakes.CROP);
		int[] ages = {0, 2, 3, 5, 7, 7};
		for (int i = 0; i < ages.length; i++) {
			put(level, origin, -5 + i, -1, 1, Blocks.FARMLAND.defaultBlockState());
			put(level, origin, -5 + i, 0, 1, mandrake.getStateForAge(ages[i]));
		}
		put(level, origin, -6, -1, 1, Blocks.WATER.defaultBlockState());
		plant(level, origin, 1, 1, Mandrakes.WILD);
	}
}
