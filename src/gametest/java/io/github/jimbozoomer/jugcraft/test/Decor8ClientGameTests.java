package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.BlackCatBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BlackCatBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LabTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.MummySarcophagusBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RavenPerchBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpecimenJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TeslaCoilBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TeslaCoilBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Client game test for the mad scientist and monsters: a stone-brick lab open at the front with two running Tesla
 * Coils, a powered Lab Table whose patient sits up, four Specimen Jars on a counter, a Mummy Sarcophagus, a Raven on a
 * Perch and two Black Cat Figures (one hissing); photographed by day and at night, the coils as they arc and the eye's jar
 * close up (CI job {@code client}).
 */
public class Decor8ClientGameTests implements FabricClientGameTest {
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

			BlockPos coilA = new BlockPos(x + 4, y, z - 8);
			BlockPos coilB = new BlockPos(x + 8, y, z - 8);
			power(server, coilA, coilB);
			shoot(context, singleplayer, x + 7, y + 2, z + 5, 180, 10, "jugcraft_mad_lab");
			// The coils as they arc: wait for one to strike, then photograph it.
			power(server, coilA, coilB);
			place(context, singleplayer, x + 5, y + 2, z - 4, 180, 10);
			context.waitFor(client -> client.level.getBlockEntity(coilA) instanceof TeslaCoilBlockEntity coil
					&& client.level.getGameTime() - coil.arcTime() <= 1, 200);
			context.takeScreenshot("jugcraft_tesla_coils");
			shoot(context, singleplayer, x + 7, y + 1, z - 5, 270, 10, "jugcraft_lab_table");
			shoot(context, singleplayer, x + 2, y + 1, z - 8, 180, 15, "jugcraft_specimen_jars");
			// The eye's jar close up: a round eye mapped on every side, whichever way it has turned.
			shoot(context, singleplayer, x + 1, y + 1, z - 9, 180, 30, "jugcraft_specimen_eye_close");
			BlockPos sarcophagus = new BlockPos(x + 13, y, z - 9);
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				MummySarcophagusBlock.open(level, sarcophagus, level.getBlockState(sarcophagus));
			});
			shoot(context, singleplayer, x + 9, y, z - 9, 270, 10, "jugcraft_mummy_sarcophagus");
			BlockPos hissing = new BlockPos(x + 7, y, z - 4);
			place(context, singleplayer, x + 8, y + 2, z - 1, 180, 30);
			server.runOnServer(minecraft -> {
				if (minecraft.overworld().getBlockEntity(hissing) instanceof BlackCatBlockEntity cat) {
					cat.hiss(minecraft.overworld());
				}
			});
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_raven_and_cats");
			server.runCommand("time set midnight");
			power(server, coilA, coilB);
			shoot(context, singleplayer, x + 7, y + 2, z + 5, 180, 10, "jugcraft_mad_lab_night");
			shoot(context, singleplayer, x + 6, y + 1, z - 1, 180, 20, "jugcraft_black_cats_night");
		}
	}

	/** Fills both coils' buffers, enough for 200 ticks of running. */
	private static void power(TestServerContext server, BlockPos... coils) {
		server.runOnServer(minecraft -> {
			for (BlockPos pos : coils) {
				if (minecraft.overworld().getBlockEntity(pos) instanceof TeslaCoilBlockEntity coil) {
					coil.energy().setAmount(TeslaCoilBlockEntity.CAPACITY);
				}
			}
		});
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
		BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
		// A lab from x to x + 14 and z - 12 to z - 2, open at the front (south), with a ceiling.
		for (int dx = 0; dx <= 14; dx++) {
			for (int dz = -12; dz <= -2; dz++) {
				set(level, new BlockPos(x + dx, y - 1, z + dz), Blocks.POLISHED_ANDESITE.defaultBlockState());
				set(level, new BlockPos(x + dx, y + 6, z + dz), bricks);
			}
			for (int dy = 0; dy <= 5; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 12), bricks);
			}
		}
		for (int dz = -12; dz <= -2; dz++) {
			for (int dy = 0; dy <= 5; dy++) {
				set(level, new BlockPos(x, y + dy, z + dz), bricks);
				set(level, new BlockPos(x + 14, y + dy, z + dz), bricks);
			}
		}
		// Two Tesla Coils, switched on, out of the way of the jar counter.
		for (int dx : new int[] {4, 8}) {
			BlockState coil = state("tesla_coil").setValue(TeslaCoilBlock.ENABLED, true);
			set(level, new BlockPos(x + dx, y, z - 8), coil.setValue(TeslaCoilBlock.HALF, DoubleBlockHalf.LOWER));
			set(level, new BlockPos(x + dx, y + 1, z - 8), coil.setValue(TeslaCoilBlock.HALF, DoubleBlockHalf.UPPER));
		}
		// The lab table, its head to the north, over a block of redstone set in the floor so the patient sits up.
		BlockPos foot = new BlockPos(x + 10, y, z - 5);
		BlockState table = state("lab_table").setValue(LabTableBlock.FACING, Direction.NORTH);
		set(level, foot, table.setValue(LabTableBlock.PART, BedPart.FOOT));
		set(level, foot.north(), table.setValue(LabTableBlock.PART, BedPart.HEAD));
		set(level, foot.north().below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
		// A counter of jars along the back wall, one of each specimen.
		SpecimenJarBlock.Specimen[] specimens = SpecimenJarBlock.Specimen.values();
		for (int i = 0; i < specimens.length; i++) {
			set(level, new BlockPos(x + 1 + i, y, z - 11), Blocks.DARK_OAK_PLANKS.defaultBlockState());
			set(level, new BlockPos(x + 1 + i, y + 1, z - 11), state("specimen_jar").setValue(SpecimenJarBlock.SPECIMEN, specimens[i]));
		}
		// The sarcophagus by the right wall, looking into the lab.
		BlockState sarcophagus = state("mummy_sarcophagus").setValue(MummySarcophagusBlock.FACING, Direction.WEST);
		set(level, new BlockPos(x + 13, y, z - 9), sarcophagus.setValue(MummySarcophagusBlock.HALF, DoubleBlockHalf.LOWER));
		set(level, new BlockPos(x + 13, y + 1, z - 9), sarcophagus.setValue(MummySarcophagusBlock.HALF, DoubleBlockHalf.UPPER));
		// The raven and two cats at the front.
		set(level, new BlockPos(x + 12, y, z - 4), state("raven_perch").setValue(RavenPerchBlock.FACING, Direction.SOUTH));
		for (int dx : new int[] {5, 7}) {
			set(level, new BlockPos(x + dx, y, z - 4), state("black_cat_figure").setValue(BlackCatBlock.FACING, Direction.SOUTH));
		}
	}
}
