package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CreepyDollBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DustSheetBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedChandelierBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MourningAngelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PipeOrganBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RockingChairBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpiritMirrorBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SuitOfArmorBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TatteredCurtainsBlock;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Client game test for the haunted house inside: a dark room open at the front with a lit Haunted Chandelier under its
 * ceiling, the Phantom Pipe Organ against the back wall playing, a Suit of Armor by the left wall, Dust Sheets over a
 * rocking chair, a chest and a stair, a Spirit Mirror on the right wall, Tattered Curtains at the two back windows
 * (one drawn open) and the Creepy Doll on a bookshelf; photographed by day and at night, the mirror when its face
 * shows (CI job {@code client}).
 */
public class Decor7ClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 20, x + 24, y - 1, z + 14));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 20, x + 24, y + 12, z + 14));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			BlockPos organ = new BlockPos(x + 7, y, z - 11);
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				PipeOrganBlock.play(level, organ, level.getBlockState(organ));
			});
			shoot(context, singleplayer, x + 7, y + 2, z + 5, 180, 10, "jugcraft_haunted_room");
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				PipeOrganBlock.play(level, organ, level.getBlockState(organ));
			});
			shoot(context, singleplayer, x + 7, y + 1, z - 5, 180, 10, "jugcraft_pipe_organ");
			shoot(context, singleplayer, x + 8, y + 2, z - 1, 180, 35, "jugcraft_dust_sheets");
			shoot(context, singleplayer, x + 10, y + 1, z - 3, 270, 10, "jugcraft_creepy_doll");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 7, y + 2, z + 5, 180, 10, "jugcraft_haunted_room_night");
			shoot(context, singleplayer, x + 7, y + 1, z - 3, 180, -35, "jugcraft_haunted_chandelier_night");
			shoot(context, singleplayer, x + 5, y + 1, z - 5, 90, 5, "jugcraft_suit_of_armor_night");

			// The mirror: wait in front of it for its face to show, then photograph it.
			BlockPos mirror = new BlockPos(x + 13, y + 2, z - 9);
			server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x + 9, y + 1, z - 9));
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 9.5, y + 2, z - 8.5, 270, 0));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.waitFor(client -> SpiritMirrorBlock.face(mirror, MourningAngelBlock.night(client.level),
					client.level.getGameTime() % 24000) > 0.95F, SpiritMirrorBlock.PERIOD + 100);
			context.takeScreenshot("jugcraft_spirit_mirror_night");
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

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		BlockState planks = Blocks.DARK_OAK_PLANKS.defaultBlockState();
		// A room from x to x + 14 and z - 12 to z - 2, open at the front (south), with a ceiling.
		for (int dx = 0; dx <= 14; dx++) {
			for (int dz = -12; dz <= -2; dz++) {
				set(level, new BlockPos(x + dx, y - 1, z + dz), planks);
				set(level, new BlockPos(x + dx, y + 6, z + dz), planks);
			}
			for (int dy = 0; dy <= 5; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 12), planks);
			}
		}
		for (int dz = -12; dz <= -2; dz++) {
			for (int dy = 0; dy <= 5; dy++) {
				set(level, new BlockPos(x, y + dy, z + dz), planks);
				set(level, new BlockPos(x + 14, y + dy, z + dz), planks);
			}
		}
		// Two windows in the back wall, cheesecloth before them: the left drawn shut, the right open.
		for (int wx : new int[] {2, 12}) {
			for (int dy = 2; dy <= 3; dy++) {
				set(level, new BlockPos(x + wx, y + dy, z - 12), Blocks.GLASS.defaultBlockState());
				set(level, new BlockPos(x + wx, y + dy, z - 11), state("tattered_curtains").setValue(TatteredCurtainsBlock.FACING, Direction.SOUTH)
						.setValue(TatteredCurtainsBlock.PART, dy == 3 ? TatteredCurtainsBlock.Part.TOP : TatteredCurtainsBlock.Part.BOTTOM)
						.setValue(TatteredCurtainsBlock.OPEN, wx == 12));
			}
		}
		// The organ against the back wall, its keyboard to the room.
		BlockPos organ = new BlockPos(x + 7, y, z - 11);
		for (int part = 0; part < PipeOrganBlock.WIDTH * PipeOrganBlock.HEIGHT; part++) {
			set(level, PipeOrganBlock.partPos(organ, Direction.SOUTH, part), state("phantom_pipe_organ").setValue(PipeOrganBlock.FACING, Direction.SOUTH)
					.setValue(PipeOrganBlock.PART, part));
		}
		// The chandelier, lit, under the ceiling in the middle of the room.
		set(level, new BlockPos(x + 7, y + 5, z - 7), state("haunted_chandelier").setValue(HauntedChandelierBlock.LIT, true)
				.setValue(HauntedChandelierBlock.BURNING, HauntedChandelierBlock.CANDLES));
		// The suit of armor by the left wall, looking into the room.
		BlockState suit = state("suit_of_armor").setValue(SuitOfArmorBlock.FACING, Direction.EAST);
		set(level, new BlockPos(x + 1, y, z - 6), suit.setValue(SuitOfArmorBlock.HALF, DoubleBlockHalf.LOWER));
		set(level, new BlockPos(x + 1, y + 1, z - 6), suit.setValue(SuitOfArmorBlock.HALF, DoubleBlockHalf.UPPER));
		// Furniture under dust sheets: a rocking chair, a chest and a stair.
		BlockPos chair = new BlockPos(x + 4, y, z - 4);
		BlockPos chest = new BlockPos(x + 11, y, z - 7);
		BlockPos stair = new BlockPos(x + 12, y, z - 4);
		set(level, chair, state("rocking_chair").setValue(RockingChairBlock.FACING, Direction.SOUTH));
		set(level, chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
		set(level, stair, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
		for (BlockPos pos : new BlockPos[] {chair, chest, stair}) {
			DustSheetBlock.cover(level, pos, null);
		}
		// The mirror on the right wall, and the doll on a bookshelf by the front.
		set(level, new BlockPos(x + 13, y + 2, z - 9), state("spirit_mirror").setValue(SpiritMirrorBlock.FACING, Direction.WEST));
		set(level, new BlockPos(x + 13, y, z - 3), Blocks.BOOKSHELF.defaultBlockState());
		set(level, new BlockPos(x + 13, y + 1, z - 3), state("creepy_doll").setValue(CreepyDollBlock.FACING, Direction.WEST));
	}
}
