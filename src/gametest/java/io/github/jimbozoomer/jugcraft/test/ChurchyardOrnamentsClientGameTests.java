package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.BonePileBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Epitaph;
import io.github.jimbozoomer.jugcraft.agriculture.GiantBoneHandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.OssuaryWallBlock;
import java.util.List;
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
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the churchyard's ornaments: a catacomb corner of ossuary walls with bone piles heaped at their
 * foot, a gargoyle on its plinth (one weathered), two giant bone hands out of the grass (one clenched), and witch's
 * lanterns standing and hanging from a beam. By day, close up, and at night by the lanterns. CI job {@code client}.
 */
public class ChurchyardOrnamentsClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
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
			server.runCommand("time set 10500");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 12, x + 10, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 12, x + 10, y + 10, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 1, y + 3, z + 4, 180, 22, "jugcraft_churchyard_ornaments");
			watchFrom(context, singleplayer, origin, new Vec3(-2.0, 0.9, -3.0), 180.0F, 18.0F, "jugcraft_churchyard_ossuary");
			watchFrom(context, singleplayer, origin, new Vec3(4.0, 0.2, 1.0), 180.0F, 5.0F, "jugcraft_churchyard_gargoyles");
			watchFrom(context, singleplayer, origin, new Vec3(7.5, 1.0, 0.5), 180.0F, 10.0F, "jugcraft_churchyard_bone_hands");
			watchFrom(context, singleplayer, origin, new Vec3(-4.0, 0.0, 2.5), 180.0F, -5.0F, "jugcraft_churchyard_lanterns");
			server.runCommand("time set 18000");
			context.waitTicks(20);
			shoot(context, singleplayer, x + 1, y + 3, z + 4, 180, 22, "jugcraft_churchyard_ornaments_night");
			server.runCommand("time set noon");
			server.runCommand("gamerule minecraft:advance_time true");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

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

	private static void build(ServerLevel level, BlockPos origin) {
		// The catacomb corner: ossuary walls three high along the back and down the west side, bone piles at their foot.
		BlockState south = block("ossuary_wall").defaultBlockState().setValue(OssuaryWallBlock.FACING, Direction.SOUTH);
		BlockState east = block("ossuary_wall").defaultBlockState().setValue(OssuaryWallBlock.FACING, Direction.EAST);
		for (int dy = 0; dy < 3; dy++) {
			for (int dx = -7; dx <= 2; dx++) {
				put(level, origin, dx, dy, -7, south);
			}
			for (int dz = -6; dz <= -3; dz++) {
				put(level, origin, -7, dy, dz, east);
			}
		}
		int[][] piles = {{-6, -6, 4}, {-5, -6, 3}, {-4, -6, 2}, {-6, -5, 3}, {-6, -4, 1}, {-2, -6, 1}, {0, -6, 2}, {-3, -5, 1}};
		for (int[] p : piles) {
			put(level, origin, p[0], 0, p[1], block("bone_pile").defaultBlockState().setValue(BonePileBlock.LAYERS, p[2]));
		}
		// Two gargoyles on their plinths, one weathered to moss.
		for (int i = 0; i < 2; i++) {
			HeadstoneBlock gargoyle = (HeadstoneBlock) block("gargoyle");
			BlockPos pos = origin.offset(2 + i * 3, 0, -4);
			BlockState state = gargoyle.defaultBlockState().setValue(HeadstoneBlock.FACING, Direction.SOUTH);
			level.setBlock(pos, state, Block.UPDATE_ALL);
			gargoyle.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
			gargoyle.setWeather(level, pos, i * 2, false);
			if (level.getBlockEntity(pos) instanceof HeadstoneBlockEntity stone) {
				stone.engrave(Epitaph.of(List.of(i == 0 ? "MEMENTO" : "VIGILATE", "MORI")));
			}
		}
		// Two giant bone hands out of the grass, the second clenched.
		// The clenched one is held shut by a redstone block hidden under it.
		put(level, origin, 9, -1, -3, Blocks.REDSTONE_BLOCK.defaultBlockState());
		for (int i = 0; i < 2; i++) {
			BlockState hand = block("giant_bone_hand").defaultBlockState().setValue(GiantBoneHandBlock.FACING, Direction.SOUTH)
					.setValue(GiantBoneHandBlock.POWERED, i == 1);
			put(level, origin, 6 + i * 3, 0, -3, hand.setValue(GiantBoneHandBlock.HALF, DoubleBlockHalf.LOWER));
			put(level, origin, 6 + i * 3, 1, -3, hand.setValue(GiantBoneHandBlock.HALF, DoubleBlockHalf.UPPER));
		}
		// Witch's lanterns: two standing on the bone piles' corner, three hanging from a dark oak beam on posts.
		BlockState standing = block("witchs_lantern").defaultBlockState().setValue(LanternBlock.HANGING, false);
		BlockState hanging = block("witchs_lantern").defaultBlockState().setValue(LanternBlock.HANGING, true);
		put(level, origin, -2, 0, -4, standing);
		put(level, origin, -4, 0, -3, standing);
		for (int dy = 0; dy < 4; dy++) {
			put(level, origin, -6, dy, -1, Blocks.DARK_OAK_FENCE.defaultBlockState());
			put(level, origin, -2, dy, -1, Blocks.DARK_OAK_FENCE.defaultBlockState());
		}
		for (int dx = -6; dx <= -2; dx++) {
			put(level, origin, dx, 4, -1, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		}
		for (int dx : new int[] {-5, -4, -3}) {
			put(level, origin, dx, 3, -1, hanging);
		}
	}
}
