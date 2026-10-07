package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingFace;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.FloatingCandleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinVineBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LuminariaBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.agriculture.SkeletonHandSconceBlock;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Client game test for the second five Halloween decorations: a row of lit Luminarias in eight colours, Floating
 * Candles (one to four) in front of a dark wall, Skeleton Hand Sconces on a stone wall (two burning, one snuffed),
 * carved pumpkins lit by a soul torch, a torch and nothing, a giant pumpkin lit by a soul torch, and Bat Bunting with
 * string lights on fence posts; photographed by day and at night (CI job {@code client}). The test keeps the hooks
 * charged between shots; server tests check how they draw power.
 */
public class Decor2ClientGameTests implements FabricClientGameTest {
	private static final List<DyeColor> COLORS = List.of(DyeColor.WHITE, DyeColor.ORANGE, DyeColor.PURPLE, DyeColor.BLACK, DyeColor.LIME,
			DyeColor.RED, DyeColor.YELLOW, DyeColor.LIGHT_BLUE);

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 26, x + 34, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 26, x + 34, y + 12, z + 10));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, origin, x + 12, y + 6, z + 8, 180, 25, "jugcraft_decorations_2");
			shoot(context, singleplayer, origin, x + 2, y + 2, z - 14, 180, 0, "jugcraft_bat_bunting");
			shoot(context, singleplayer, origin, x + 13, y + 2, z - 12, 180, 5, "jugcraft_skeleton_hand_sconces");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, origin, x + 7, y + 3, z + 4, 180, 20, "jugcraft_luminarias_night");
			shoot(context, singleplayer, origin, x + 5, y + 2, z - 5, 180, 5, "jugcraft_floating_candles_night");
			shoot(context, singleplayer, origin, x + 13, y + 2, z - 12, 180, 5, "jugcraft_skeleton_hand_sconces_night");
			shoot(context, singleplayer, origin, x + 21, y + 1, z - 3, 180, 10, "jugcraft_soul_carvings_night");
			shoot(context, singleplayer, origin, x + 27, y + 2, z - 4, 180, 10, "jugcraft_soul_giant_pumpkin_night");
			shoot(context, singleplayer, origin, x + 2, y + 2, z - 14, 180, 0, "jugcraft_bat_bunting_night");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos origin, int x, int y, int z, int yaw,
			int pitch, String name) {
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

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// A path of lit luminarias in eight colours.
		for (int i = 0; i < COLORS.size(); i++) {
			set(level, new BlockPos(x + i * 2, y, z - 4), state("luminaria").setValue(LuminariaBlock.COLOR, COLORS.get(i)).setValue(LuminariaBlock.LIT, true));
		}

		// One to four floating candles, lit, in front of a dark wall.
		for (int dx = 1; dx <= 9; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 12), Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
		for (int n = 1; n <= FloatingCandleBlock.MAX; n++) {
			set(level, new BlockPos(x + n * 2, y + 2, z - 10), state("floating_candle").setValue(FloatingCandleBlock.CANDLES, n)
					.setValue(FloatingCandleBlock.LIT, true));
		}

		// Sconces on a stone wall: two burning, one snuffed.
		for (int dx = 10; dx <= 16; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 17), Blocks.STONE_BRICKS.defaultBlockState());
			}
		}
		for (int i = 0; i < 3; i++) {
			set(level, new BlockPos(x + 11 + i * 2, y + 2, z - 16), state("skeleton_hand_sconce").setValue(SkeletonHandSconceBlock.FACING, Direction.SOUTH)
					.setValue(SkeletonHandSconceBlock.LIT, i < 2));
		}

		// Carved pumpkins: soul-lit, candle-lit and dark.
		PumpkinCarving carving = PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(0).face());
		for (int i = 0; i < 3; i++) {
			BlockPos pos = new BlockPos(x + 20 + i, y, z - 6);
			set(level, pos, state("hand_carved_pumpkin").setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH).setValue(CarvedPumpkinBlock.GLOW, carving.glow()));
			if (level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity pumpkin) {
				pumpkin.setCarving(carving, null);
			}
			set(level, pos, level.getBlockState(pos).setValue(CarvedPumpkinBlock.LIT, i < 2).setValue(CarvedPumpkinBlock.SOUL, i == 0));
		}

		// A giant pumpkin grown from its vine, carved on its south side and lit by a soul torch.
		BlockPos vinePos = new BlockPos(x + 25, y, z - 10);
		set(level, vinePos.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
		GiantPumpkinVineBlock vine = (GiantPumpkinVineBlock) JugcraftAgriculture.block("giant_pumpkin_vine");
		set(level, vinePos, vine.defaultBlockState().setValue(GiantPumpkinVineBlock.AGE, GiantPumpkinVineBlock.MAX_AGE));
		if (vine.growFruit(level, vinePos, Direction.EAST) && level.getBlockEntity(vinePos.east()) instanceof GiantPumpkinBlockEntity seedling) {
			seedling.feed(level, GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
			GiantPumpkinBlockEntity giant = GiantPumpkinBlock.master(level, vinePos.east(), level.getBlockState(vinePos.east()));
			if (giant != null) {
				giant.feed(level, GiantPumpkinBlockEntity.GROW_TO_THREE - GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
			}
			giant = GiantPumpkinBlock.master(level, vinePos.east(), level.getBlockState(vinePos.east()));
			if (giant != null) {
				giant.setFace(Direction.SOUTH, CarvingFace.scale(CarvingTemplates.ALL.get(0).face(), GiantPumpkinBlockEntity.FACE_SIZE), null);
				giant.setLit(true, true);
			}
		}

		// Fence posts with hooks: bat bunting between the first two, string lights on to the third.
		List<BlockPos> hooks = hooks(origin);
		for (BlockPos hook : hooks) {
			set(level, hook.below(2), Blocks.DARK_OAK_FENCE.defaultBlockState());
			set(level, hook.below(), Blocks.DARK_OAK_FENCE.defaultBlockState());
			set(level, hook, state("string_light_hook").setValue(StringLightHookBlock.FACE, AttachFace.FLOOR));
		}
		StringLightsItem.string(level, hooks.get(0), hooks.get(1), StringLightHookBlockEntity.Strand.BUNTING);
		StringLightsItem.string(level, hooks.get(1), hooks.get(2), StringLightHookBlockEntity.Strand.LIGHTS);
		charge(level, origin);
	}

	private static List<BlockPos> hooks(BlockPos origin) {
		return List.of(origin.offset(-4, 2, -20), origin.offset(2, 2, -20), origin.offset(8, 2, -20));
	}

	/** Keeps the hooks charged so the string lights glow beside the bunting through the shots. */
	private static void charge(ServerLevel level, BlockPos origin) {
		for (BlockPos hook : hooks(origin)) {
			if (level.getBlockEntity(hook) instanceof StringLightHookBlockEntity entity) {
				entity.energy().setAmount(StringLightHookBlockEntity.CAPACITY);
				entity.update(level);
			}
		}
	}
}
