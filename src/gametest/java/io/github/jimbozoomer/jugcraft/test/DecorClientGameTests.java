package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CoffinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FogMachineBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FogMachineBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedPortraitBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightHookBlock;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightHookBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightsItem;
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
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Client game test for the first five Halloween decorations: String Light Hooks on fence posts with lit strands
 * between them, three Candy Bowls (empty, half full, heaped), an open and a closed Coffin, the four Haunted Portraits
 * on a wall, and a running Fog Machine; photographed by day and at night (CI job {@code client}). The test keeps the
 * hooks and the fog machine charged between shots; server tests check how they draw power.
 */
public class DecorClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 24, x + 34, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 24, x + 34, y + 12, z + 10));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, origin, x + 12, y + 5, z + 8, 180, 20, "jugcraft_decorations");
			shoot(context, singleplayer, origin, x + 13, y + 1, z - 1, 180, 35, "jugcraft_candy_bowls");
			shoot(context, singleplayer, origin, x + 19, y + 2, z - 2, 180, 35, "jugcraft_coffins");
			shoot(context, singleplayer, origin, x + 25, y + 1, z - 5, 180, 0, "jugcraft_haunted_portraits");
			shoot(context, singleplayer, origin, x + 29, y + 1, z - 6, 135, 0, "jugcraft_haunted_portraits_side");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, origin, x + 2, y + 2, z + 1, 180, 8, "jugcraft_string_lights_night");
			shoot(context, singleplayer, origin, x + 8, y + 3, z - 3, 180, 18, "jugcraft_fog_machine_night");
			shoot(context, singleplayer, origin, x + 25, y + 1, z - 5, 180, 0, "jugcraft_haunted_portraits_night");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos origin, int x, int y, int z,
			int yaw, int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runOnServer(minecraft -> charge(minecraft.overworld(), origin));
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

	private static List<BlockPos> hooks(BlockPos origin) {
		return List.of(origin.offset(-4, 2, -6), origin.offset(2, 2, -6), origin.offset(8, 2, -6));
	}

	/** Keeps the hooks and the fog machine charged so they stay lit and running through the shots. */
	private static void charge(ServerLevel level, BlockPos origin) {
		for (BlockPos hook : hooks(origin)) {
			if (level.getBlockEntity(hook) instanceof StringLightHookBlockEntity entity) {
				entity.energy().setAmount(StringLightHookBlockEntity.CAPACITY);
				entity.update(level);
			}
		}
		if (level.getBlockEntity(origin.offset(8, 0, -12)) instanceof FogMachineBlockEntity machine) {
			machine.energy().setAmount(FogMachineBlockEntity.CAPACITY);
			machine.update(level);
		}
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// Fence posts with hooks on top, strung one to the next.
		List<BlockPos> hooks = hooks(origin);
		for (BlockPos hook : hooks) {
			set(level, hook.below(2), Blocks.DARK_OAK_FENCE.defaultBlockState());
			set(level, hook.below(), Blocks.DARK_OAK_FENCE.defaultBlockState());
			set(level, hook, state("string_light_hook").setValue(StringLightHookBlock.FACE, AttachFace.FLOOR));
		}
		StringLightsItem.string(level, hooks.get(0), hooks.get(1));
		StringLightsItem.string(level, hooks.get(1), hooks.get(2));

		// Candy bowls: empty, half full and heaped, facing the camera.
		int[] fills = {0, 20, 64};
		for (int i = 0; i < fills.length; i++) {
			BlockPos bowl = new BlockPos(x + 12 + i, y, z - 4);
			set(level, bowl, state("candy_bowl").setValue(CandyBowlBlock.FACING, Direction.SOUTH));
			if (fills[i] > 0 && level.getBlockEntity(bowl) instanceof CandyBowlBlockEntity entity) {
				entity.add(new ItemStack(JugcraftAgriculture.item("candy_corn"), fills[i] / 2));
				entity.add(new ItemStack(JugcraftAgriculture.item("caramel"), fills[i] - fills[i] / 2));
			}
		}

		// Two coffins, head to the north: one open, one shut.
		for (int i = 0; i < 2; i++) {
			BlockPos foot = new BlockPos(x + 18 + i * 3, y, z - 6);
			BlockState coffin = state("coffin").setValue(CoffinBlock.FACING, Direction.NORTH).setValue(CoffinBlock.OPEN, i == 0);
			set(level, foot.north(), coffin.setValue(CoffinBlock.PART, BedPart.HEAD));
			set(level, foot, coffin.setValue(CoffinBlock.PART, BedPart.FOOT));
		}

		// A plank wall with the four portraits, facing the camera.
		for (int dx = 23; dx <= 28; dx++) {
			for (int dy = 0; dy <= 3; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 10), Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
		HauntedPortraitBlock.Portrait[] portraits = HauntedPortraitBlock.Portrait.values();
		for (int i = 0; i < portraits.length; i++) {
			set(level, new BlockPos(x + 24 + i, y + 1, z - 9), state("haunted_portrait").setValue(HauntedPortraitBlock.FACING, Direction.SOUTH)
					.setValue(HauntedPortraitBlock.PORTRAIT, portraits[i]));
		}

		// The fog machine, switched on, nozzle toward the camera, with a wide radius.
		set(level, new BlockPos(x + 8, y, z - 12), state("fog_machine").setValue(FogMachineBlock.FACING, Direction.SOUTH).setValue(FogMachineBlock.ENABLED, true)
				.setValue(FogMachineBlock.RADIUS, 2));
		charge(level, origin);
	}
}
