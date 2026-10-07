package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.Broomstick;
import io.github.jimbozoomer.jugcraft.agriculture.BubblingCauldronBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the flying broomstick (fall addition 22): two brooms waiting by a witch's cauldron of flying
 * ointment, one anointed and hovering, one dry on the grass, the broom item framed on a wall; then the player in a
 * witch hat riding one high over the grass, seen from behind and from the front, and at midnight. CI job {@code client}.
 */
public class BroomstickClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Hide the HUD, hand and chat whatever an earlier test in this client left: CI shares the client tests out
			// between parallel jobs, and only the first job's opening test hides them.
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
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 14, x + 14, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 14, x + 14, y + 14, z + 10));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
					.formatted(x + 6, y + 2, z - 5, Broomstick.ITEM));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 3, y + 1, z + 1, 180, 20, "jugcraft_broomsticks");

			// Up on a broom, in a witch hat, over the grass.
			server.runCommand("item replace entity @p armor.head with jugcraft:witch_hat");
			place(context, singleplayer, x + 3, y + 6, z - 1, 180, 10);
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
				Broomstick broom = new Broomstick(level, player.position(), 180.0F, Broomstick.MAX_CHARGE);
				level.addFreshEntity(broom);
				player.startRiding(broom);
			});
			server.runCommand("setblock %d %d %d minecraft:air".formatted(x + 3, y + 5, z - 1));
			context.waitTicks(20);
			context.runOnClient(client -> {
				client.player.setYRot(180.0F);
				client.player.setXRot(10.0F);
				client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			});
			context.waitTicks(30);
			context.takeScreenshot("jugcraft_broomstick_riding");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_broomstick_rider");
			server.runCommand("time set midnight");
			context.waitTicks(30);
			context.takeScreenshot("jugcraft_broomstick_night");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("time set noon");
			server.runOnServer(minecraft -> minecraft.getPlayerList().getPlayers().get(0).stopRiding());
			server.runCommand("item replace entity @p armor.head with minecraft:air");
			context.waitTicks(10);
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch) {
		TestServerContext server = singleplayer.getServer();
		// Only into air, so standing on the ground leaves no hole in it for the later pictures.
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
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

	/**
	 * A witch's corner: a cobbled wall, a cauldron of flying ointment over a campfire, pumpkins; an anointed broom
	 * hovering beside it, a dry one on the grass.
	 */
	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		for (int dx = 0; dx <= 7; dx++) {
			for (int dy = 0; dy <= 3; dy++) {
				level.setBlock(new BlockPos(x + dx, y + dy, z - 6), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		BlockPos pot = new BlockPos(x + 1, y + 1, z - 4);
		level.setBlock(pot.below(), Blocks.CAMPFIRE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(pot, JugcraftAgriculture.block("bubbling_cauldron").defaultBlockState()
				.setValue(BubblingCauldronBlock.CONTENTS, BubblingCauldronBlock.Brew.FLYING), Block.UPDATE_ALL);
		level.setBlock(new BlockPos(x + 6, y, z - 4), Blocks.CARVED_PUMPKIN.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(new BlockPos(x + 7, y, z - 3), Blocks.PUMPKIN.defaultBlockState(), Block.UPDATE_ALL);
		Broomstick hovering = new Broomstick(level, new Vec3(x + 3.5, y + 0.6, z - 3.0), 60.0F, Broomstick.MAX_CHARGE);
		level.addFreshEntity(hovering);
		Broomstick dry = new Broomstick(level, new Vec3(x + 4.8, y, z - 1.5), 150.0F, 0);
		level.addFreshEntity(dry);
	}
}
