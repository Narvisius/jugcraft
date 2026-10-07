package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.BarmbrackBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GiantCandyBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PunchBowlBlock;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for treats: a party table set with a full and a half-full Witch's Brew Punch Bowl, a whole and a cut
 * Barmbrack and a Candy Bowl of soul cakes and cupcakes, the four Giant Candy props in front of it, and every treat in an
 * item frame on the wall behind. Photographed by day and at night, when the punch glows (CI job {@code client}).
 */
public class Decor13ClientGameTests implements FabricClientGameTest {
	private static final List<String> TREATS = List.of("soul_cake", "pumpkin_bread", "spiderweb_cupcake", "bat_wing_cookie",
			"pumpkin_spice_latte", "witchs_brew_punch", "barmbrack", "barmbrack_ring");

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 14, x + 16, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 14, x + 16, y + 10, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			// Every treat in an item frame on the wall (the frames hang on the wall's south face).
			for (int i = 0; i < TREATS.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Invisible:0b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 2 + i, y + 2, z - 7, TREATS.get(i)));
			}
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 5, y + 2, z + 2, 180, 18, "jugcraft_halloween_treats");
			shoot(context, singleplayer, x + 5, y + 2, z - 3, 180, 38, "jugcraft_witchs_brew_punch_bowl");
			shoot(context, singleplayer, x + 3, y + 2, z - 3, 180, 35, "jugcraft_barmbrack");
			shoot(context, singleplayer, x + 5, y + 1, z + 1, 180, 20, "jugcraft_giant_candy");
			shoot(context, singleplayer, x + 5, y + 2, z - 4, 180, 15, "jugcraft_treat_items");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 5, y + 2, z - 3, 180, 38, "jugcraft_witchs_brew_punch_bowl_night");
			shoot(context, singleplayer, x + 5, y + 2, z + 2, 180, 18, "jugcraft_halloween_treats_night");
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

	private static BlockState state(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState();
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The wall behind, for the item frames.
		for (int dx = 0; dx <= 11; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 8), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
		// The party table, and what is set on it.
		for (int dx = 1; dx <= 9; dx++) {
			set(level, new BlockPos(x + dx, y, z - 6), Blocks.DARK_OAK_PLANKS.defaultBlockState());
		}
		int top = y + 1;
		set(level, new BlockPos(x + 2, top, z - 6), state("barmbrack").setValue(BarmbrackBlock.FACING, Direction.SOUTH));
		set(level, new BlockPos(x + 3, top, z - 6), state("barmbrack").setValue(BarmbrackBlock.FACING, Direction.SOUTH).setValue(BarmbrackBlock.BITES, 3));
		set(level, new BlockPos(x + 5, top, z - 6), state("witchs_brew_punch_bowl").setValue(PunchBowlBlock.SERVINGS_LEFT, PunchBowlBlock.SERVINGS));
		BlockPos bowl = new BlockPos(x + 7, top, z - 6);
		set(level, bowl, state("candy_bowl").setValue(CandyBowlBlock.FACING, Direction.SOUTH));
		if (level.getBlockEntity(bowl) instanceof CandyBowlBlockEntity treats) {
			treats.add(new ItemStack(JugcraftAgriculture.item("soul_cake"), 10));
			treats.add(new ItemStack(JugcraftAgriculture.item("spiderweb_cupcake"), 10));
		}
		set(level, new BlockPos(x + 9, top, z - 6), state("witchs_brew_punch_bowl").setValue(PunchBowlBlock.SERVINGS_LEFT, 6));
		// The giant candy props, out in front.
		int[] at = {1, 3, 7, 9};
		GiantCandyBlock.Design[] designs = GiantCandyBlock.Design.values();
		for (int i = 0; i < designs.length; i++) {
			set(level, new BlockPos(x + at[i], y, z - 3), state("giant_candy").setValue(GiantCandyBlock.FACING, Direction.SOUTH)
					.setValue(GiantCandyBlock.DESIGN, designs[i]));
		}
	}
}
