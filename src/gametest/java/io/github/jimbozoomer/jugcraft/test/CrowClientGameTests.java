package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.Crow;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

/**
 * Client game test for crows and working scarecrows: a ripe wheat field guarded by a scarecrow wearing a pumpkin head,
 * crows wheeling over a carrot patch just out of its reach, and one of them down on the carrots, pecking (it was sent after the
 * crop and stepped once, so it pecks for real); the others are posed in flight. Photographed by day (CI job
 * {@code client}).
 */
public class CrowClientGameTests implements FabricClientGameTest {
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
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 4, y - 3, z - 16, x + 18, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 16, x + 18, y + 10, z + 4));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			System.out.println("[crows test] crows on the client: " + context.computeOnClient(client -> client.level.getEntitiesOfClass(Crow.class,
					client.player.getBoundingBox().inflate(40.0)).size()) + ", pecking: " + context.computeOnClient(client -> client.level
					.getEntitiesOfClass(Crow.class, client.player.getBoundingBox().inflate(40.0)).stream().filter(Crow::pecking).count()));

			shoot(context, singleplayer, x + 7, y + 2, z + 3, 180, 14, "jugcraft_crows_and_scarecrow");
			shoot(context, singleplayer, x + 13, y + 1, z - 5, 180, 50, "jugcraft_crows");
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		place(context, singleplayer, x, y, z, yaw, pitch);
		context.waitTicks(20);
		context.takeScreenshot(name);
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	/** A ripe crop on wet farmland at (x, y, z) to (x2, y, z2). */
	private static void field(ServerLevel level, Block crop, int x, int y, int z, int x2, int z2) {
		for (int dx = x; dx <= x2; dx++) {
			for (int dz = z; dz <= z2; dz++) {
				BlockPos pos = new BlockPos(dx, y, dz);
				set(level, pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
				set(level, pos, ((CropBlock) crop).getStateForAge(((CropBlock) crop).getMaxAge()));
			}
		}
	}

	/** A crow posed at (x, y, z) facing {@code yaw}, with no AI (it holds its pose). */
	private static @Nullable Crow crow(ServerLevel level, double x, double y, double z, float yaw) {
		Crow crow = JugcraftAgriculture.CROW.create(level, EntitySpawnReason.COMMAND);
		if (crow == null) {
			return null;
		}
		crow.setNoAi(true);
		crow.setPersistenceRequired();
		crow.snapTo(x, y, z, yaw, 0.0F);
		crow.setYHeadRot(yaw);
		crow.setYBodyRot(yaw);
		level.addFreshEntity(crow);
		return crow;
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// A wheat field, and a scarecrow in it wearing a pumpkin head (it guards eight blocks round), standing on a grass
		// block: it needs a solid top under it, and farmland's isn't.
		field(level, Blocks.WHEAT, x, y, z - 12, x + 8, z - 6);
		BlockPos scarecrow = new BlockPos(x + 4, y, z - 9);
		set(level, scarecrow.below(), Blocks.GRASS_BLOCK.defaultBlockState());
		BlockState standing = JugcraftAgriculture.block("scarecrow").defaultBlockState().setValue(TallDecorationBlock.FACING, Direction.SOUTH);
		set(level, scarecrow, standing);
		set(level, scarecrow.above(), standing.setValue(TallDecorationBlock.HALF, DoubleBlockHalf.UPPER));
		if (level.getBlockEntity(scarecrow.above()) instanceof ScarecrowBlockEntity dressed) {
			dressed.setHead(new ItemStack(Items.CARVED_PUMPKIN));
		}
		// A carrot patch just out of its reach, where the crows are (carrots are short, so the pecking crow shows).
		field(level, Blocks.CARROTS, x + 12, y, z - 10, x + 15, z - 7);
		// The pecking crow is on the front row, nearest the camera, so the leaves behind it don't hide it.
		BlockPos crop = new BlockPos(x + 13, y, z - 7);
		Crow pecking = crow(level, crop.getX() + 0.5, crop.getY() + 0.1, crop.getZ() + 0.5, 200.0F);
		if (pecking != null) {
			boolean sent = pecking.raid(level, crop);
			pecking.step(level, true);
			System.out.println("[crows test] sent after the crop: " + sent + ", pecking: " + pecking.pecking());
		}
		crow(level, x + 11.8, y + 1.5, z - 6.8, 60.0F);
		crow(level, x + 15.0, y + 2.0, z - 7.8, -130.0F);
		crow(level, x + 11.0, y + 3.6, z - 11.5, 100.0F);
		crow(level, x + 15.6, y + 4.4, z - 12.5, -60.0F);
	}
}
