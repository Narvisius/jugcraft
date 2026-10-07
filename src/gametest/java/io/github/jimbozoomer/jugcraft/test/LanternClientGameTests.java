package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.SkyLanternItem;
import io.github.jimbozoomer.jugcraft.agriculture.SkyLanterns;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the sky lantern festival: by day, mooncakes and a sky lantern in item frames on a wall; at night,
 * a sky full of lanterns in many colours drifting up over a field, one carrying a wish, let go together for a festival
 * (its message in the chat), photographed from the ground looking up (CI job {@code client}).
 */
public class LanternClientGameTests implements FabricClientGameTest {
	private static final int[] COLOURS = {0xE8642A, 0xF0B030, 0xD8303A, 0xF4E8C8, 0x3A6AE0, 0x8A4AD0, 0x3AB86A};

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 12, y - 3, z - 20, x + 12, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 20, x + 12, y + 30, z + 6));
			// A spruce wall with the mooncakes and a sky lantern in frames.
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 2, y, z - 6, x + 2, y + 2, z - 6));
			String[] shown = {"jugcraft:red_bean_mooncake", "jugcraft:chestnut_mooncake", "jugcraft:pumpkin_mooncake", "jugcraft:sky_lantern"};
			for (int i = 0; i < shown.length; i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Item:{id:\"%s\",count:1}}".formatted(x - 2 + i + (i > 1 ? 1 : 0), y + 1,
						z - 5, shown[i]));
			}
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			shoot(context, singleplayer, x, y, z - 2, 180, 15, "jugcraft_mooncakes");

			// At night, lanterns let go together over the field.
			server.runCommand("time set 14000");
			place(context, singleplayer, x, y, z + 2, 180, -32);
			server.runOnServer(minecraft -> release(minecraft.overworld(), new Vec3(x + 0.5, y, z - 10.5)));
			context.waitTicks(30);
			context.takeScreenshot("jugcraft_sky_lanterns");
		}
	}

	/** Lanterns in many colours at many heights over the field, one with a wish: enough together for a festival. */
	private static void release(ServerLevel level, Vec3 centre) {
		SkyLanterns.forget();
		RandomSource random = RandomSource.create(1015L);
		for (int i = 0; i < 18; i++) {
			ItemStack lantern = new ItemStack(JugcraftAgriculture.item("sky_lantern"));
			lantern.set(DataComponents.DYED_COLOR, new DyedItemColor(COLOURS[i % COLOURS.length]));
			if (i == 4) {
				lantern.set(DataComponents.CUSTOM_NAME, Component.literal("A good harvest"));
			}
			Vec3 at = centre.add(random.nextDouble() * 14.0 - 7.0, 2.0 + random.nextDouble() * 12.0, random.nextDouble() * 8.0 - 4.0);
			SkyLanternItem.release(level, i == 4 ? centre.add(0.0, 3.0, 4.0) : at, lantern);
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
}
