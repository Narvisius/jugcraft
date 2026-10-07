package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.AutumnWreathBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BobbingTubBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HayBaleSeatBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LeafPileBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCrateBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCrateBlockEntity;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Client game test for the harvest party: a plank wall hung with Autumn Wreaths in all four colours (and one on a
 * door), a bench of Hay Bale Seats, Bobbing for Apples Tubs with none, two and four apples, Pumpkin Crates of
 * pumpkins and of mixed squash and gourds, and Leaf Piles of every colour heaped one to four layers; photographed by day
 * and at night (CI job {@code client}).
 */
public class Decor5ClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 20, x + 24, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 20, x + 24, y + 12, z + 10));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 6, y + 4, z + 4, 180, 25, "jugcraft_harvest_party");
			shoot(context, singleplayer, x + 3, y + 2, z - 1, 180, 40, "jugcraft_bobbing_tubs");
			shoot(context, singleplayer, x + 8, y + 2, z - 2, 180, 45, "jugcraft_pumpkin_crates");
			shoot(context, singleplayer, x + 3, y + 2, z - 3, 180, 12, "jugcraft_hay_bales_and_wreaths");
			shoot(context, singleplayer, x + 10, y + 2, z - 5, 180, 25, "jugcraft_wreath_on_a_door");
			shoot(context, singleplayer, x + 6, y + 2, z + 2, 180, 35, "jugcraft_leaf_piles");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 6, y + 4, z + 4, 180, 25, "jugcraft_harvest_party_night");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static BlockState state(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState();
	}

	private static void fill(ServerLevel level, BlockPos pos, List<Item> produce) {
		set(level, pos, state("pumpkin_crate").setValue(PumpkinCrateBlock.FACING, Direction.SOUTH));
		if (level.getBlockEntity(pos) instanceof PumpkinCrateBlockEntity crate) {
			produce.forEach(piece -> crate.add(new ItemStack(piece)));
		}
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// A plank wall with a door in it, hung with a wreath in each colour and one on the door.
		for (int dx = 0; dx <= 12; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 9), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
		BlockPos door = new BlockPos(x + 10, y, z - 9);
		set(level, door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		set(level, door.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		AutumnWreathBlock.Mums[] colours = AutumnWreathBlock.Mums.values();
		for (int i = 0; i < colours.length; i++) {
			set(level, new BlockPos(x + 1 + i * 2, y + 2, z - 8), state("autumn_wreath").setValue(AutumnWreathBlock.FACING, Direction.SOUTH)
					.setValue(AutumnWreathBlock.FLOWERS, colours[i]));
		}
		set(level, door.above().south(), state("autumn_wreath").setValue(AutumnWreathBlock.FACING, Direction.SOUTH)
				.setValue(AutumnWreathBlock.FLOWERS, AutumnWreathBlock.Mums.RED));
		// A bench of hay bales.
		for (int dx = 1; dx <= 4; dx++) {
			set(level, new BlockPos(x + dx, y, z - 6), state("hay_bale_seat").setValue(HayBaleSeatBlock.FACING, Direction.SOUTH));
		}
		// Bobbing tubs with none, two and four apples.
		int[] apples = {0, 2, 4};
		for (int i = 0; i < apples.length; i++) {
			set(level, new BlockPos(x + 1 + i * 2, y, z - 4), state("bobbing_tub").setValue(BobbingTubBlock.APPLES, apples[i]));
		}
		// Crates of pumpkins and of mixed squash and gourds.
		fill(level, new BlockPos(x + 7, y, z - 5), List.of(Items.PUMPKIN, Items.PUMPKIN, Items.PUMPKIN, Items.PUMPKIN));
		fill(level, new BlockPos(x + 9, y, z - 5), List.of(JugcraftAgriculture.item("white_pumpkin"), JugcraftAgriculture.item("jarrahdale_pumpkin"),
				JugcraftAgriculture.item("butternut_squash"), JugcraftAgriculture.item("warty_gourd")));
		// Leaf piles, one to four layers of each colour.
		String[] piles = {"red_leaf_pile", "orange_leaf_pile", "yellow_leaf_pile"};
		for (int p = 0; p < piles.length; p++) {
			for (int layers = 1; layers <= LeafPileBlock.MAX_LAYERS; layers++) {
				set(level, new BlockPos(x + p * 4 + layers, y, z - 1), state(piles[p]).setValue(LeafPileBlock.LAYERS, layers));
			}
		}
	}
}
