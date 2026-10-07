package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.BrainVatConsoleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CrawlingHandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FeastTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FloatingTableSettingBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HarvestMoonLampBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedDiningChairBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HauntedDiningChairBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LabTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.LightningHarnessBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.MultiDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SilkSpoolStackBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SilkSpoolStackBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Witchlights;
import io.github.jimbozoomer.jugcraft.agriculture.YardSilhouetteBlock;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Laboratory, the Larder and the Dining Room. To the west a laboratory: a Lightning Harness
 * hung from a beam over a Lab Table, the Brain-Vat Console remembering a full signal, and Crawling Hands, one running.
 * Then the spider's larder: Silk Cocoons hung from a beam, Egg Sac Clusters on the floor and wall, a Web Drape across
 * its door and a Silk Spool Stack in three colours on a bench. In the middle the dining room: a Harvest Feast Table laid
 * with Floating Table Settings for dinner, tea and a feast, Haunted Dining Chairs round it and a Grandfather Clock by
 * the wall. To the east the yard: a path lined with Witchlight Path Stakes of every colour, a Witchlight Lamp-Post,
 * Hanging Witchlights under a fence arch, the six Yard Silhouettes on the lawn and the Harvest Moon Lamp behind. By day,
 * the harness caught as it fires; then at midnight in the dining room (the candles lit, a chair slid out, the ghost at
 * the clock's glass) and in the yard (the figures' eyes, the lamps and the moon). CI job {@code client}.
 */
public class LaboratoryLarderDiningClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
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
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("difficulty peaceful");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 16, y - 4, z - 16, x + 16, y - 4, z + 9));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 16, y - 3, z - 16, x + 16, y - 1, z + 9));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 16, y, z - 16, x + 16, y + 12, z + 9));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x, y + 8, z + 7, 180, 32, "jugcraft_laboratory_larder_dining");
			watchFrom(context, singleplayer, origin, new Vec3(-11.5, 2.4, -2.5), 180.0F, 16.0F, true, "jugcraft_laboratory_larder_dining_laboratory");
			watchFrom(context, singleplayer, origin, new Vec3(-4.5, 1.6, -0.5), 180.0F, 8.0F, false, "jugcraft_laboratory_larder_dining_larder");
			watchFrom(context, singleplayer, origin, new Vec3(3.5, 2.6, -2.0), 180.0F, 22.0F, false, "jugcraft_laboratory_larder_dining_dining");
			watchFrom(context, singleplayer, origin, new Vec3(11.5, 2.2, 0.5), 180.0F, 10.0F, false, "jugcraft_laboratory_larder_dining_yard");
			server.runCommand("time set 18000");
			server.runOnServer(minecraft -> nightfall(minecraft.overworld(), origin));
			context.waitTicks(30);
			watchFrom(context, singleplayer, origin, new Vec3(3.5, 2.6, -2.0), 180.0F, 22.0F, false, "jugcraft_laboratory_larder_dining_dining_night");
			watchFrom(context, singleplayer, origin, new Vec3(11.5, 2.2, 0.5), 180.0F, 10.0F, false, "jugcraft_laboratory_larder_dining_yard_night");
			watchFrom(context, singleplayer, origin, new Vec3(-11.5, 2.4, -2.5), 180.0F, 16.0F, false, "jugcraft_laboratory_larder_dining_laboratory_night");
			server.runCommand("time set noon");
			server.runCommand("gamerule minecraft:advance_time true");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	/** Looks from {@code at} (by an invisible armour stand); with {@code fire}, the harness fires just before the picture. */
	private static void watchFrom(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos origin, Vec3 at, float yaw,
			float pitch, boolean fire, String name) {
		TestServerContext server = singleplayer.getServer();
		int id = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			ArmorStand stand = new ArmorStand(level, origin.getX() + at.x, origin.getY() + at.y, origin.getZ() + at.z);
			stand.snapTo(stand.getX(), stand.getY(), stand.getZ(), yaw, pitch);
			stand.setYHeadRot(yaw);
			stand.setInvisible(true);
			stand.setNoGravity(true);
			level.addFreshEntity(stand);
			return stand.getId();
		});
		context.waitTicks(5);
		context.runOnClient(client -> {
			Entity stand = client.level.getEntity(id);
			if (stand != null) {
				client.setCameraEntity(stand);
			}
		});
		context.waitTicks(10);
		singleplayer.getConnection().waitForChunksRender();
		if (fire) {
			server.runOnServer(minecraft -> {
				BlockPos harness = origin.offset(-12, 3, -8);
				if (minecraft.overworld().getBlockEntity(harness) instanceof LightningHarnessBlockEntity rig) {
					rig.fire(minecraft.overworld(), harness);
				}
			});
			context.waitTicks(4);
		}
		context.takeScreenshot(name);
		context.runOnClient(client -> client.setCameraEntity(client.player));
		server.runOnServer(minecraft -> {
			Entity stand = minecraft.overworld().getEntity(id);
			if (stand != null) {
				stand.discard();
			}
		});
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void put(ServerLevel level, BlockPos origin, int dx, int dy, int dz, BlockState state) {
		level.setBlock(origin.offset(dx, dy, dz), state, Block.UPDATE_ALL);
	}

	/** A two-block-tall prop, lower half at (dx, dy, dz). */
	private static void tall(ServerLevel level, BlockPos origin, int dx, int dy, int dz, BlockState lower) {
		put(level, origin, dx, dy, dz, lower.setValue(TallDecorationBlock.HALF, DoubleBlockHalf.LOWER));
		put(level, origin, dx, dy + 1, dz, lower.setValue(TallDecorationBlock.HALF, DoubleBlockHalf.UPPER));
	}

	/** A prop of several blocks, its first block at (dx, dy, dz), facing {@code facing}. */
	private static void multi(ServerLevel level, BlockPos origin, int dx, int dy, int dz, String id, Direction facing) {
		MultiDecorationBlock prop = (MultiDecorationBlock) block(id);
		BlockPos master = origin.offset(dx, dy, dz);
		for (int part = 0; part < prop.cells().length; part++) {
			level.setBlock(prop.partPos(master, facing, part), prop.defaultBlockState().setValue(MultiDecorationBlock.FACING, facing)
					.setValue(prop.partProperty(), part), Block.UPDATE_ALL);
		}
	}

	/** A redstone torch two blocks under (dx, dz), powering the ground block above it and so whatever stands on that. */
	private static void powerFromBelow(ServerLevel level, BlockPos origin, int dx, int dz) {
		put(level, origin, dx, -3, dz, Blocks.STONE.defaultBlockState());
		put(level, origin, dx, -2, dz, BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("redstone_torch")).defaultBlockState());
	}

	private static BlockState witchlight(String id, Witchlights.Colour colour) {
		return block(id).defaultBlockState().setValue(Witchlights.COLOUR, colour);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		// The laboratory: a polished andesite floor, a stone brick back wall, a beam across on two pillars.
		for (int dx = -15; dx <= -9; dx++) {
			for (int dz = -12; dz <= -3; dz++) {
				put(level, origin, dx, -1, dz, Blocks.POLISHED_ANDESITE.defaultBlockState());
			}
			for (int dy = 0; dy < 5; dy++) {
				put(level, origin, dx, dy, -12, Blocks.STONE_BRICKS.defaultBlockState());
			}
			put(level, origin, dx, 4, -8, Blocks.POLISHED_ANDESITE.defaultBlockState());
		}
		for (int dy = 0; dy < 4; dy++) {
			put(level, origin, -15, dy, -8, Blocks.STONE_BRICKS.defaultBlockState());
			put(level, origin, -9, dy, -8, Blocks.STONE_BRICKS.defaultBlockState());
		}
		BlockState table = block("lab_table").defaultBlockState().setValue(LabTableBlock.FACING, Direction.NORTH);
		put(level, origin, -12, 0, -7, table.setValue(LabTableBlock.PART, BedPart.FOOT));
		put(level, origin, -12, 0, -8, table.setValue(LabTableBlock.PART, BedPart.HEAD));
		put(level, origin, -12, 3, -8, block(JugcraftAgriculture.LIGHTNING_HARNESS).defaultBlockState());
		put(level, origin, -14, 0, -10, block(JugcraftAgriculture.BRAIN_VAT_CONSOLE).defaultBlockState().setValue(BrainVatConsoleBlock.FACING, Direction.EAST));
		put(level, origin, -15, 0, -10, Blocks.REDSTONE_BLOCK.defaultBlockState());
		put(level, origin, -14, 0, -6, block(JugcraftAgriculture.BRAIN_VAT_CONSOLE).defaultBlockState().setValue(BrainVatConsoleBlock.FACING, Direction.EAST));
		powerFromBelow(level, origin, -10, -5);
		put(level, origin, -10, 0, -5, block(JugcraftAgriculture.CRAWLING_HAND).defaultBlockState()
				.setValue(CrawlingHandBlock.FACING, Direction.WEST));
		put(level, origin, -10, 0, -10, block(JugcraftAgriculture.CRAWLING_HAND).defaultBlockState()
				.setValue(CrawlingHandBlock.FACING, Direction.SOUTH));
		tall(level, origin, -13, 0, -11, block("tesla_coil").defaultBlockState().setValue(TallDecorationBlock.FACING, Direction.SOUTH));

		// The larder: spruce floor, a dark oak back wall and beam, cocoons hung, sacs, a drape over its door and a bench.
		for (int dx = -7; dx <= -1; dx++) {
			for (int dz = -12; dz <= -3; dz++) {
				put(level, origin, dx, -1, dz, Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
			for (int dy = 0; dy < 5; dy++) {
				put(level, origin, dx, dy, -12, Blocks.DARK_OAK_LOG.defaultBlockState());
			}
			put(level, origin, dx, 3, -9, Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		}
		for (int dy = 0; dy < 3; dy++) {
			put(level, origin, -7, dy, -9, Blocks.DARK_OAK_LOG.defaultBlockState());
			put(level, origin, -1, dy, -9, Blocks.DARK_OAK_LOG.defaultBlockState());
		}
		for (int dx : new int[] {-6, -4, -3}) {
			put(level, origin, dx, 2, -9, block(JugcraftAgriculture.SILK_COCOON).defaultBlockState());
		}
		BlockState sacs = block(JugcraftAgriculture.EGG_SAC_CLUSTER).defaultBlockState();
		put(level, origin, -6, 0, -11, sacs.setValue(MultifaceBlock.getFaceProperty(Direction.NORTH), true)
				.setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true));
		put(level, origin, -3, 1, -11, sacs.setValue(MultifaceBlock.getFaceProperty(Direction.NORTH), true));
		put(level, origin, -5, 0, -6, sacs.setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true));
		put(level, origin, -7, 3, -11, Blocks.COBWEB.defaultBlockState());
		multi(level, origin, -3, 0, -4, JugcraftAgriculture.WEB_DRAPE, Direction.NORTH);
		put(level, origin, -2, 0, -11, Blocks.SPRUCE_PLANKS.defaultBlockState());
		put(level, origin, -2, 1, -11, block(JugcraftAgriculture.SILK_SPOOL_STACK).defaultBlockState().setValue(SilkSpoolStackBlock.FACING, Direction.SOUTH));
		if (level.getBlockEntity(origin.offset(-2, 1, -11)) instanceof SilkSpoolStackBlockEntity spools) {
			spools.setColour(0, DyeColor.ORANGE);
			spools.setColour(1, DyeColor.PURPLE);
			spools.setColour(2, DyeColor.LIME);
		}

		// The dining room: a dark oak floor and wall, a feast table laid three ways, chairs round it and the clock.
		for (int dx = 0; dx <= 7; dx++) {
			for (int dz = -12; dz <= -3; dz++) {
				put(level, origin, dx, -1, dz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
			for (int dy = 0; dy < 5; dy++) {
				put(level, origin, dx, dy, -12, dy == 0 ? Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState() : Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
		for (int dx = 2; dx <= 5; dx++) {
			put(level, origin, dx, 0, -8, block("feast_table").defaultBlockState().setValue(FeastTableBlock.AXIS, Direction.Axis.X));
		}
		FloatingTableSettingBlock.Setting[] laid = {FloatingTableSettingBlock.Setting.DINNER, FloatingTableSettingBlock.Setting.TEA,
				FloatingTableSettingBlock.Setting.FEAST, FloatingTableSettingBlock.Setting.DINNER};
		for (int i = 0; i < 4; i++) {
			put(level, origin, 2 + i, 1, -8, block(JugcraftAgriculture.TABLE_SETTING).defaultBlockState()
					.setValue(FloatingTableSettingBlock.FACING, i % 2 == 0 ? Direction.SOUTH : Direction.NORTH).setValue(FloatingTableSettingBlock.SETTING, laid[i]));
		}
		for (int dx = 2; dx <= 5; dx++) {
			put(level, origin, dx, 0, -7, block(JugcraftAgriculture.DINING_CHAIR).defaultBlockState().setValue(HauntedDiningChairBlock.FACING, Direction.NORTH));
			put(level, origin, dx, 0, -9, block(JugcraftAgriculture.DINING_CHAIR).defaultBlockState().setValue(HauntedDiningChairBlock.FACING, Direction.SOUTH));
		}
		put(level, origin, 1, 0, -8, block(JugcraftAgriculture.DINING_CHAIR).defaultBlockState().setValue(HauntedDiningChairBlock.FACING, Direction.EAST));
		put(level, origin, 6, 0, -8, block(JugcraftAgriculture.DINING_CHAIR).defaultBlockState().setValue(HauntedDiningChairBlock.FACING, Direction.WEST));
		tall(level, origin, 0, 0, -11, block(JugcraftAgriculture.GRANDFATHER_CLOCK).defaultBlockState().setValue(TallDecorationBlock.FACING, Direction.SOUTH));
		put(level, origin, 3, 4, -11, Blocks.DARK_OAK_PLANKS.defaultBlockState());
		put(level, origin, 3, 3, -11, witchlight(JugcraftAgriculture.HANGING_WITCHLIGHT, Witchlights.Colour.ORANGE));

		// The yard: a coarse dirt path lined with witchlights, a lamp-post, a fence arch hung with lanterns, the figures
		// on the lawn and the harvest moon behind.
		for (int dx = 8; dx <= 15; dx++) {
			put(level, origin, dx, -1, -6, Blocks.COARSE_DIRT.defaultBlockState());
		}
		Witchlights.Colour[] colours = Witchlights.Colour.values();
		for (int i = 0; i < 4; i++) {
			int dx = 9 + i * 2;
			for (int dz : new int[] {-7, -5}) {
				powerFromBelow(level, origin, dx, dz);
				put(level, origin, dx, 0, dz, witchlight(JugcraftAgriculture.WITCHLIGHT_STAKE, colours[(i + (dz == -7 ? 0 : 2)) % colours.length]));
			}
		}
		powerFromBelow(level, origin, 15, -4);
		tall(level, origin, 15, 0, -4, witchlight(JugcraftAgriculture.WITCHLIGHT_POST, Witchlights.Colour.PURPLE).setValue(TallDecorationBlock.FACING, Direction.WEST));
		for (int dy = 0; dy < 3; dy++) {
			put(level, origin, 8, dy, -8, Blocks.DARK_OAK_FENCE.defaultBlockState());
			put(level, origin, 8, dy, -4, Blocks.DARK_OAK_FENCE.defaultBlockState());
		}
		for (int dz = -8; dz <= -4; dz++) {
			put(level, origin, 8, 3, dz, Blocks.DARK_OAK_FENCE.defaultBlockState());
		}
		put(level, origin, 8, 2, -7, witchlight(JugcraftAgriculture.HANGING_WITCHLIGHT, Witchlights.Colour.GREEN));
		put(level, origin, 8, 2, -5, witchlight(JugcraftAgriculture.HANGING_WITCHLIGHT, Witchlights.Colour.BLUE));
		YardSilhouetteBlock.Figure[] figures = YardSilhouetteBlock.Figure.values();
		for (int i = 0; i < figures.length; i++) {
			int dx = 9 + i;
			int dz = i % 2 == 0 ? -9 : -10;
			put(level, origin, dx, 0, dz, block(JugcraftAgriculture.YARD_SILHOUETTE).defaultBlockState().setValue(YardSilhouetteBlock.FIGURE, figures[i])
					.setValue(YardSilhouetteBlock.ROTATION, i == 1 ? 12 : i == 4 ? 6 : 8));
		}
		multi(level, origin, 13, 0, -12, JugcraftAgriculture.MOON_LAMP, Direction.SOUTH);
	}

	/** At midnight the candles are lit, the moon lamp lit, and a chair slides out from the table. */
	private static void nightfall(ServerLevel level, BlockPos origin) {
		for (int dx = 2; dx <= 5; dx++) {
			BlockPos at = origin.offset(dx, 1, -8);
			BlockState setting = level.getBlockState(at);
			if (setting.getBlock() instanceof FloatingTableSettingBlock) {
				level.setBlock(at, setting.setValue(FloatingTableSettingBlock.LIT, true), Block.UPDATE_ALL);
			}
		}
		BlockPos chair = origin.offset(4, 0, -7);
		if (level.getBlockEntity(chair) instanceof HauntedDiningChairBlockEntity haunted) {
			haunted.slideOut(level, chair, level.getBlockState(chair));
		}
		HarvestMoonLampBlock moon = (HarvestMoonLampBlock) block(JugcraftAgriculture.MOON_LAMP);
		BlockPos master = origin.offset(13, 0, -12);
		for (int part = 0; part < moon.cells().length; part++) {
			BlockPos at = moon.partPos(master, Direction.SOUTH, part);
			if (level.getBlockState(at).is(moon)) {
				level.setBlock(at, level.getBlockState(at).setValue(MultiDecorationBlock.LIT, true), Block.UPDATE_ALL);
			}
		}
	}
}
