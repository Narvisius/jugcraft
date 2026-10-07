package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Knitting;
import io.github.jimbozoomer.jugcraft.agriculture.Knitwear;
import io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlockEntity;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Client game test for knitting: three Spinning Wheels, one bare, one with a skein of orange wool, one half spun with
 * purple, their wheels turning; and beside them, before a campfire, three armour stands in knitwear: a cream beanie,
 * pumpkin sweater and socks; a red striped sweater and green beanie; an orange bat sweater and black socks. By day, up
 * close on the wheels and on the knitwear, and the yarn, needles and garments in frames. CI job {@code client}.
 */
public class KnittingClientGameTests implements FabricClientGameTest {
	private static final String[] FRAMED = {"yarn", "knitting_needles", "knit_beanie", "wool_socks", "knit_sweater", "striped_sweater",
			"pumpkin_sweater", "bat_sweater", "leaf_sweater"};

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 14, x + 14, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 14, x + 14, y + 10, z + 6));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			for (int i = 0; i < FRAMED.length; i++) {
				String item = FRAMED[i];
				int colour = item.equals("yarn") ? 0xE8761C : item.contains("sweater") || item.equals("knit_beanie") || item.equals("wool_socks")
						? 0x6A3A9A : -1;
				String components = colour < 0 ? "" : ",components:{\"minecraft:dyed_color\":%d}".formatted(colour);
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1%s}}"
						.formatted(x + 1 + i, y + 2, z - 9, item, components));
			}
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 7, y + 1, z + 3, 180, 15, "jugcraft_knitting");
			shoot(context, singleplayer, x + 11, y + 1, z + 1, 180, 10, "jugcraft_knitwear");
			// Turn the wheels while the shot is taken, so they are caught mid-spin.
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				for (int i = 0; i < 3; i++) {
					if (level.getBlockEntity(new BlockPos(x + 1 + 3 * i, y, z - 6)) instanceof SpinningWheelBlockEntity wheel && wheel.wool() != null) {
						wheel.turn(level);
					}
				}
			});
			shoot(context, singleplayer, x + 4, y + 1, z - 3, 180, 20, "jugcraft_spinning_wheels");
			shoot(context, singleplayer, x + 5, y + 1, z - 6, 180, 5, "jugcraft_knitting_items");
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
		context.waitTicks(5);
		context.takeScreenshot(name);
	}

	private static void stand(ServerLevel level, double x, int y, double z, float yaw, Object... worn) {
		LivingEntity stand = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.COMMAND);
		if (stand == null) {
			return;
		}
		stand.snapTo(x, y, z, yaw, 0.0F);
		for (int i = 0; i < worn.length; i += 2) {
			Knitwear knit = (Knitwear) worn[i];
			stand.setItemSlot(knit.slot, Knitting.garment(knit, (Integer) worn[i + 1]));
		}
		level.addFreshEntity(stand);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// A wall for the frames.
		for (int dx = 0; dx <= 10; dx++) {
			for (int dy = 0; dy <= 3; dy++) {
				level.setBlock(new BlockPos(x + dx, y + dy, z - 10), Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		// Three wheels: bare, a skein of orange, purple half spun.
		DyeColor[] wools = {null, DyeColor.ORANGE, DyeColor.PURPLE};
		for (int i = 0; i < 3; i++) {
			BlockPos pos = new BlockPos(x + 1 + 3 * i, y, z - 6);
			level.setBlock(pos, JugcraftAgriculture.block("spinning_wheel").defaultBlockState().setValue(SpinningWheelBlock.FACING, Direction.SOUTH),
					Block.UPDATE_ALL);
			if (wools[i] != null && level.getBlockEntity(pos) instanceof SpinningWheelBlockEntity wheel) {
				wheel.loadWool(wools[i]);
				if (i == 2) {
					wheel.turn(level);
					wheel.turn(level);
				}
			}
		}
		// Beside the wheels, three stands in knitwear before a campfire (behind them, so its smoke rises clear of them).
		level.setBlock(new BlockPos(x + 11, y, z - 5), Blocks.CAMPFIRE.defaultBlockState(), Block.UPDATE_ALL);
		stand(level, x + 9.5, y, z - 3.5, 20.0F, Knitwear.BEANIE, Knitting.UNDYED, Knitwear.PUMPKIN_SWEATER, Knitting.UNDYED, Knitwear.SOCKS,
				Knitting.UNDYED);
		stand(level, x + 11.5, y, z - 3.0, 0.0F, Knitwear.BEANIE, 0x3C8A2E, Knitwear.STRIPED_SWEATER, 0xB02E26);
		stand(level, x + 13.5, y, z - 3.5, -20.0F, Knitwear.BAT_SWEATER, 0xE8761C, Knitwear.SOCKS, 0x1D1D21);
	}
}
