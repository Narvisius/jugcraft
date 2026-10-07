package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.HayGolem;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.CropBlock;

/**
 * Client game test for the Hay Golem: three golems in a field of ripe wheat and carrots, one wearing a carved pumpkin,
 * one bent over the carrots harvesting with a jack o'lantern for a head, one wearing a lit hand-carved white pumpkin with
 * a cat's face; beside them a T of hay bales with a carved pumpkin on top, as a golem is built. By day, up close, and at
 * night, when the lit heads glow. CI job {@code client}.
 */
public class HayGolemClientGameTests implements FabricClientGameTest {
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
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 14, x + 16, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 14, x + 16, y + 10, z + 6));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 5, y + 1, z + 3, 180, 10, "jugcraft_hay_golems");
			shoot(context, singleplayer, x + 7, y + 1, z - 2, 180, 5, "jugcraft_hay_golem_close");
			server.runCommand("time set 15000");
			shoot(context, singleplayer, x + 5, y + 1, z + 3, 180, 10, "jugcraft_hay_golems_night");
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

	private static void golem(ServerLevel level, double x, int y, double z, float yaw, ItemStack head, boolean working) {
		HayGolem golem = JugcraftAgriculture.HAY_GOLEM.create(level, EntitySpawnReason.COMMAND);
		if (golem == null) {
			return;
		}
		golem.snapTo(x, y, z, yaw, 0.0F);
		golem.setYHeadRot(yaw);
		golem.setYBodyRot(yaw);
		golem.setNoAi(true);
		golem.setHead(head);
		golem.setWorking(working);
		golem.setPost(BlockPos.containing(x, y, z));
		level.addFreshEntity(golem);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// A field: rows of ripe wheat, and carrots at the front, on farmland.
		for (int dx = 0; dx <= 9; dx++) {
			for (int dz = -10; dz <= -7; dz++) {
				BlockPos pos = new BlockPos(x + dx, y, z + dz);
				level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);
				CropBlock crop = (CropBlock) (dz == -7 ? Blocks.CARROTS : Blocks.WHEAT);
				level.setBlock(pos, crop.getStateForAge(crop.getMaxAge()), Block.UPDATE_ALL);
			}
		}
		// A golem in a carved pumpkin, looking out over the field.
		golem(level, x + 1.5, y, z - 4.5, 0.0F, new ItemStack(Items.CARVED_PUMPKIN), false);
		// A golem with a jack o'lantern for a head, bent over the carrots, harvesting them.
		golem(level, x + 4.5, y, z - 5.6, 200.0F, new ItemStack(Items.JACK_O_LANTERN), true);
		// A golem wearing a lit hand-carved white pumpkin with a cat's face.
		ItemStack cat = new ItemStack(JugcraftAgriculture.item("hand_carved_white_pumpkin"));
		cat.set(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(1).face()));
		cat.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock.LIT, true));
		golem(level, x + 7.5, y, z - 4.5, 20.0F, cat, false);
		// How a golem is built: a T of four hay bales, a carved pumpkin to put on top.
		int tx = x + 11;
		int tz = z - 5;
		for (BlockPos pos : new BlockPos[] {new BlockPos(tx, y, tz), new BlockPos(tx, y + 1, tz), new BlockPos(tx - 1, y + 1, tz), new BlockPos(tx + 1, y + 1, tz)}) {
			level.setBlock(pos, Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		}
		level.setBlock(new BlockPos(tx, y + 2, tz), Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH),
				Block.UPDATE_ALL);
		// The chest at the first golem's post.
		level.setBlock(new BlockPos(x - 1, y, z - 4), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
	}
}
