package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.GraspingHandsBlock;
import io.github.jimbozoomer.jugcraft.agriculture.InflatableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MultiDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PorchWitchBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PorchWitchBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.PoseableSkeletonBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpookySignBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpookySignBlockEntity;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Client game test for the yard and porch: a house front with a porch (the Porch Witch at her pot, Bone Wind Chimes
 * under the porch roof, a Weathervane at each end of the roof), a lawn with the four Yard Inflatables blown up, Grasping
 * Hands, Spooky Signs, the Poseable Skeleton in its four poses (one hanging from a gallows beam), the Haunted Archway
 * at the gate and the Dead Hollow Tree; photographed by day and at night (CI job {@code client}).
 */
public class Decor9ClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 20, x + 26, y - 1, z + 16));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 20, x + 26, y + 12, z + 16));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(80);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 8, y + 4, z + 11, 180, 15, "jugcraft_yard");
			shoot(context, singleplayer, x + 4, y + 1, z + 3, 180, 5, "jugcraft_inflatables");
			BlockPos witch = new BlockPos(x + 4, y, z - 10);
			place(context, singleplayer, x + 5, y + 1, z - 5, 165, 10);
			server.runOnServer(minecraft -> {
				if (minecraft.overworld().getBlockEntity(witch) instanceof PorchWitchBlockEntity porchWitch) {
					porchWitch.cackle(minecraft.overworld());
				}
			});
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_porch_witch");
			shoot(context, singleplayer, x + 9, y + 1, z - 6, 180, -10, "jugcraft_wind_chimes");
			shoot(context, singleplayer, x + 12, y + 2, z + 3, 180, 15, "jugcraft_skeletons");
			shoot(context, singleplayer, x + 11, y + 1, z + 3, 180, 20, "jugcraft_spooky_signs_and_hands");
			shoot(context, singleplayer, x + 13, y + 2, z + 9, 180, 10, "jugcraft_archway_and_tree");
			shoot(context, singleplayer, x + 8, y + 8, z - 2, 180, 30, "jugcraft_weathervanes");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 8, y + 4, z + 11, 180, 15, "jugcraft_yard_night");
			shoot(context, singleplayer, x + 13, y + 2, z + 9, 180, 10, "jugcraft_archway_and_tree_night");
			shoot(context, singleplayer, x + 4, y + 1, z + 3, 180, 5, "jugcraft_inflatables_night");
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

	/** Both halves of a two-block decoration, its lower half at {@code pos}. */
	private static void tall(ServerLevel level, BlockPos pos, BlockState state) {
		set(level, pos, state.setValue(TallDecorationBlock.HALF, DoubleBlockHalf.LOWER));
		set(level, pos.above(), state.setValue(TallDecorationBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** Every block of a lantern-lit prop, its master at {@code master}. */
	private static void prop(ServerLevel level, BlockPos master, String id, Direction facing) {
		MultiDecorationBlock block = (MultiDecorationBlock) JugcraftAgriculture.block(id);
		BlockState state = block.defaultBlockState().setValue(MultiDecorationBlock.FACING, facing);
		for (int part = 0; part < block.cells().length; part++) {
			set(level, block.partPos(master, facing, part), state.setValue(block.partProperty(), part));
		}
	}

	private static BlockState state(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState();
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		BlockState planks = Blocks.DARK_OAK_PLANKS.defaultBlockState();
		// The house front: a wall with a door, a porch floor, three posts and a flat roof over the porch.
		for (int dx = -1; dx <= 17; dx++) {
			for (int dy = 0; dy <= 6; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 13), planks);
			}
			for (int dz = -12; dz <= -8; dz++) {
				set(level, new BlockPos(x + dx, y - 1, z + dz), Blocks.SPRUCE_PLANKS.defaultBlockState());
				set(level, new BlockPos(x + dx, y + 4, z + dz), planks);
			}
		}
		set(level, new BlockPos(x + 8, y, z - 13), Blocks.AIR.defaultBlockState());
		set(level, new BlockPos(x + 8, y + 1, z - 13), Blocks.AIR.defaultBlockState());
		for (int dx : new int[] {-1, 8, 17}) {
			for (int dy = 0; dy <= 3; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 8), Blocks.DARK_OAK_FENCE.defaultBlockState());
			}
		}
		// On the porch: the witch at her pot, wind chimes under the roof; weathervanes on the roof.
		tall(level, new BlockPos(x + 4, y, z - 10), state("porch_witch").setValue(PorchWitchBlock.FACING, Direction.SOUTH));
		set(level, new BlockPos(x + 8, y + 3, z - 9), state("bone_wind_chimes"));
		set(level, new BlockPos(x + 11, y + 3, z - 9), state("bone_wind_chimes"));
		set(level, new BlockPos(x + 1, y + 5, z - 10), state("bat_weathervane"));
		set(level, new BlockPos(x + 15, y + 5, z - 10), state("witch_weathervane"));
		// A skeleton sitting on the porch.
		tall(level, new BlockPos(x + 13, y, z - 10), state("poseable_skeleton").setValue(PoseableSkeletonBlock.FACING, Direction.SOUTH));
		// The lawn: the four inflatables, blown up.
		int dx = 0;
		for (String design : JugcraftAgriculture.INFLATABLE_DESIGNS) {
			tall(level, new BlockPos(x + dx, y, z - 4), state("inflatable_" + design).setValue(InflatableBlock.FACING, Direction.SOUTH)
					.setValue(InflatableBlock.ON, true));
			dx += 2;
		}
		// Grasping hands and spooky signs.
		set(level, new BlockPos(x + 9, y, z - 1), state("grasping_hands"));
		set(level, new BlockPos(x + 11, y, z), state("grasping_hands").setValue(GraspingHandsBlock.FACING, Direction.EAST));
		BlockState sign = state("spooky_sign").setValue(SpookySignBlock.FACING, Direction.SOUTH);
		set(level, new BlockPos(x + 9, y, z + 1), sign);
		set(level, new BlockPos(x + 10, y, z - 2), sign.setValue(SpookySignBlock.WORDS, SpookySignBlock.Words.KEEP_OUT));
		BlockPos painted = new BlockPos(x + 12, y, z + 1);
		set(level, painted, sign.setValue(SpookySignBlock.WORDS, SpookySignBlock.Words.TURN_BACK));
		if (level.getBlockEntity(painted) instanceof SpookySignBlockEntity words) {
			words.paint("Trick or treat?");
		}
		// The skeleton waving, lounging, and hanging from a gallows beam.
		BlockState skeleton = state("poseable_skeleton").setValue(PoseableSkeletonBlock.FACING, Direction.SOUTH);
		tall(level, new BlockPos(x + 10, y, z - 5), skeleton.setValue(PoseableSkeletonBlock.POSE, PoseableSkeletonBlock.Pose.WAVING));
		tall(level, new BlockPos(x + 12, y, z - 5), skeleton.setValue(PoseableSkeletonBlock.POSE, PoseableSkeletonBlock.Pose.LOUNGING));
		for (int dy = 0; dy <= 1; dy++) {
			set(level, new BlockPos(x + 13, y + dy, z - 3), Blocks.DARK_OAK_FENCE.defaultBlockState());
			set(level, new BlockPos(x + 15, y + dy, z - 3), Blocks.DARK_OAK_FENCE.defaultBlockState());
		}
		for (int beam = 13; beam <= 15; beam++) {
			set(level, new BlockPos(x + beam, y + 2, z - 3), Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		}
		tall(level, new BlockPos(x + 14, y, z - 3), skeleton.setValue(PoseableSkeletonBlock.POSE, PoseableSkeletonBlock.Pose.HANGING));
		// The archway at the gate and the dead tree.
		prop(level, new BlockPos(x + 6, y, z + 3), "haunted_archway", Direction.SOUTH);
		prop(level, new BlockPos(x + 18, y, z - 4), "dead_hollow_tree", Direction.SOUTH);
	}
}
