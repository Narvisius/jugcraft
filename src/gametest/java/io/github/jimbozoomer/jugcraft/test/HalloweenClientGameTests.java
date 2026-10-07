package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingFace;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinVineBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarvings;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import io.github.jimbozoomer.jugcraft.client.CarvingScreen;
import java.util.Arrays;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Client game test for the Halloween harvest: a pumpkin patch with full-grown giant pumpkins (one carved on
 * its 48x48 side), a Harvest Scale, the giant's growth stages, scarecrows in four shirts wearing carved heads,
 * heirloom pumpkins, ornamental corn and corn shocks, a shed hung with corn bundles and gourd birdhouses, and
 * mums; photographed by day and lit at midnight. Then the giant carving screen, opened by the server as the
 * knife does with a Pumpkin Stencil in the other hand: the stencil is pressed in, photographed and finished,
 * after which the server must hold it, blown up to 48x48 (CI job {@code client}).
 */
public class HalloweenClientGameTests implements FabricClientGameTest {
	private static final DyeColor[] SHIRTS = {DyeColor.RED, DyeColor.BLUE, DyeColor.GREEN, DyeColor.PURPLE};

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 4, y - 1, z - 20, x + 32, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 20, x + 32, y + 14, z + 8));
			server.runOnServer(minecraft -> buildPatch(minecraft.overworld(), origin));
			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			System.out.println("[halloween test] giant pumpkin blocks: "
					+ server.computeOnServer(minecraft -> countGiant(minecraft.overworld(), origin)));

			shoot(context, singleplayer, x + 14, y + 12, z + 7, 180, 38, "jugcraft_halloween_harvest");
			shoot(context, singleplayer, x + 11, y + 4, z - 4, 180, 18, "jugcraft_giant_pumpkins");
			shoot(context, singleplayer, x + 7, y + 1, z - 1, 180, 4, "jugcraft_scarecrows");
			shoot(context, singleplayer, x + 2, y + 1, z - 3, 180, 14, "jugcraft_scarecrow_head");
			shoot(context, singleplayer, x + 19, y + 3, z - 4, 180, 30, "jugcraft_heirloom_pumpkins");
			shoot(context, singleplayer, x + 18, y + 2, z + 3, 180, 10, "jugcraft_harvest_decorations");

			carveGiantWithStencil(context, server, origin, y);

			server.runOnServer(minecraft -> lightUp(minecraft.overworld(), origin));
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 6, y + 2, z - 6, 180, 12, "jugcraft_giant_pumpkin_night");
			shoot(context, singleplayer, x + 7, y + 1, z - 1, 180, 4, "jugcraft_scarecrows_night");
			shoot(context, singleplayer, x + 2, y + 1, z - 3, 180, 14, "jugcraft_scarecrow_head_night");
		}
	}

	/**
	 * The giant carving screen end to end: the server opens it for the south side of the uncarved giant, as the
	 * knife does, with a stencil in the player's other hand; Apply presses the stencil in, Done sends the face.
	 */
	private static void carveGiantWithStencil(ClientGameTestContext context, TestServerContext server, BlockPos origin, int y) {
		BlockPos giant = origin.offset(10, 0, -13);
		int[] design = CarvingTemplates.ALL.get(3).face();
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 10", giant.getX() + 0.5, y, giant.getZ() + 2.5));
		context.waitTicks(20);
		server.runOnServer(minecraft -> {
			ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftAgriculture.item("carving_knife")));
			ItemStack stencil = new ItemStack(JugcraftAgriculture.item("pumpkin_stencil"));
			stencil.set(JugcraftAgriculture.STENCIL, PumpkinCarving.BLANK.withFace(0, design));
			player.setItemInHand(InteractionHand.OFF_HAND, stencil);
			PumpkinCarvings.open(player, giant, Direction.SOUTH);
		});
		context.waitForScreen(CarvingScreen.class);
		context.clickScreenButton("Apply");
		context.clickScreenButton("Candle: Off");
		context.waitTicks(5);
		context.takeScreenshot("jugcraft_carving_giant_screen");
		context.clickScreenButton("Done");
		context.waitForScreen(null);
		context.waitTicks(20);
		boolean carved = server.computeOnServer(minecraft -> {
			ServerLevel level = minecraft.overworld();
			GiantPumpkinBlockEntity master = GiantPumpkinBlock.master(level, giant, level.getBlockState(giant));
			return master != null && Arrays.equals(master.face(Direction.SOUTH), CarvingFace.scale(design, GiantPumpkinBlockEntity.FACE_SIZE));
		});
		System.out.println("[halloween test] giant carved from a stencil over the network: " + carved);
		if (!carved) {
			throw new AssertionError("The giant carving screen's face did not reach the giant pumpkin");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z,
			int yaw, int pitch, String name) {
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

	private static void farmland(ServerLevel level, BlockPos pos) {
		set(level, pos, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
	}

	/** Plants a vine at {@code vine}, lets it set its fruit to the east and feeds the fruit to {@code size}; returns the master. */
	private static GiantPumpkinBlockEntity giant(ServerLevel level, BlockPos vine, int size) {
		farmland(level, vine.below());
		GiantPumpkinVineBlock block = (GiantPumpkinVineBlock) JugcraftAgriculture.block("giant_pumpkin_vine");
		set(level, vine, block.defaultBlockState().setValue(GiantPumpkinVineBlock.AGE, GiantPumpkinVineBlock.MAX_AGE));
		block.growFruit(level, vine, Direction.EAST);
		BlockPos fruit = vine.east();
		if (size >= 2) {
			master(level, fruit).feed(level, GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
		}
		if (size >= 3) {
			master(level, fruit).feed(level, GiantPumpkinBlockEntity.GROW_TO_THREE - GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
			master(level, fruit).feed(level, 200, level.getRandom());
		}
		return master(level, fruit);
	}

	private static GiantPumpkinBlockEntity master(ServerLevel level, BlockPos pos) {
		return GiantPumpkinBlock.master(level, pos, level.getBlockState(pos));
	}

	private static int countGiant(ServerLevel level, BlockPos origin) {
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(0, 0, -18), origin.offset(24, 3, -10))) {
			if (level.getBlockState(pos).getBlock() instanceof GiantPumpkinBlock) {
				count++;
			}
		}
		return count;
	}

	/** A hand-carved pumpkin of one kind with a starter face on its front, facing south. */
	private static void carvedPumpkin(ServerLevel level, BlockPos pos, String id, int template) {
		PumpkinCarving carving = PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(template).face());
		set(level, pos, state(id).setValue(CarvedPumpkinBlock.FACING, Direction.SOUTH).setValue(CarvedPumpkinBlock.GLOW, carving.glow()));
		if (level.getBlockEntity(pos) instanceof CarvedPumpkinBlockEntity pumpkin) {
			pumpkin.setCarving(carving, null);
		}
	}

	/** A hand-carved pumpkin item of one kind with a starter face, for a scarecrow to wear. */
	private static ItemStack carvedHead(String id, int template) {
		ItemStack head = new ItemStack(JugcraftAgriculture.item(id));
		head.set(JugcraftAgriculture.CARVING, PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(template).face()));
		return head;
	}

	private static void buildPatch(ServerLevel level, BlockPos origin) {
		// Giant pumpkins, all grown east of their vines: one to carve with a jack o'lantern face, one by the
		// scale (carved later through the screen), one 2x2x2 and a seedling; the vine's stages behind them.
		GiantPumpkinBlockEntity carved = giant(level, origin.offset(2, 0, -14), 3);
		carved.setFace(Direction.SOUTH, CarvingFace.scale(CarvingTemplates.ALL.get(0).face(), GiantPumpkinBlockEntity.FACE_SIZE), null);
		giant(level, origin.offset(8, 0, -14), 3);
		set(level, origin.offset(12, -1, -13), Blocks.STONE.defaultBlockState());
		set(level, origin.offset(12, 0, -13), state("harvest_scale").setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		giant(level, origin.offset(14, 0, -14), 2);
		giant(level, origin.offset(19, 0, -14), 1);
		for (int age = 0; age <= GiantPumpkinVineBlock.MAX_AGE; age++) {
			BlockPos vine = origin.offset(2 + age, 0, -18);
			farmland(level, vine.below());
			set(level, vine, state("giant_pumpkin_vine").setValue(GiantPumpkinVineBlock.AGE, age));
		}

		// Scarecrows in four shirts, with a hand-carved pumpkin, a carved white pumpkin, a jack o'lantern and a
		// hand-carved Cinderella pumpkin for heads.
		for (int i = 0; i < SHIRTS.length; i++) {
			BlockPos foot = origin.offset(2 + i * 3, 0, -6);
			BlockState lower = state("scarecrow").setValue(TallDecorationBlock.FACING, Direction.SOUTH).setValue(ScarecrowBlock.SHIRT, SHIRTS[i]);
			set(level, foot, lower);
			set(level, foot.above(), lower.setValue(TallDecorationBlock.HALF, DoubleBlockHalf.UPPER));
			ItemStack head = switch (i) {
				case 0 -> carvedHead("hand_carved_pumpkin", 0);
				case 1 -> carvedHead("hand_carved_white_pumpkin", 2);
				case 2 -> new ItemStack(Blocks.JACK_O_LANTERN);
				default -> carvedHead("hand_carved_cinderella_pumpkin", 1);
			};
			if (level.getBlockEntity(foot.above()) instanceof ScarecrowBlockEntity scarecrow) {
				scarecrow.setHead(head);
			}
		}

		// Heirloom pumpkins and bottle gourds in a row, and hand-carved ones on hay bales behind them.
		String[] heirlooms = {"white_pumpkin", "jarrahdale_pumpkin", "cinderella_pumpkin", "bottle_gourd"};
		for (int i = 0; i < 8; i++) {
			set(level, origin.offset(15 + i, 0, -9), state(heirlooms[i % 4]).setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		}
		String[] carvedKinds = {"hand_carved_white_pumpkin", "hand_carved_jarrahdale_pumpkin", "hand_carved_cinderella_pumpkin"};
		for (int i = 0; i < 3; i++) {
			BlockPos bale = origin.offset(16 + i * 2, 0, -11);
			set(level, bale, Blocks.HAY_BLOCK.defaultBlockState());
			carvedPumpkin(level, bale.above(), carvedKinds[i], i + 1);
		}

		// Ripe ornamental corn, and corn shocks in front of it.
		TallCropBlock corn = JugcraftAgriculture.TALL_CROPS.get(TallCrop.ORNAMENTAL_CORN);
		for (int dx = 24; dx <= 29; dx++) {
			for (int dz = -17; dz <= -13; dz++) {
				BlockPos pos = origin.offset(dx, 0, dz);
				farmland(level, pos.below());
				for (int section = 0; section < 3; section++) {
					set(level, pos.above(section), corn.defaultBlockState().setValue(TallCropBlock.AGE, TallCropBlock.MAX_AGE)
							.setValue(TallCropBlock.SECTION, section));
				}
			}
		}
		for (int dx = 24; dx <= 29; dx += 2) {
			BlockPos foot = origin.offset(dx, 0, -10);
			set(level, foot, state("corn_shock"));
			set(level, foot.above(), state("corn_shock").setValue(TallDecorationBlock.HALF, DoubleBlockHalf.UPPER));
		}

		// A shed wall hung with ornamental corn bundles and gourd birdhouses under its eaves, with potted mums.
		for (int dx = 14; dx <= 22; dx++) {
			for (int dy = 0; dy <= 3; dy++) {
				set(level, origin.offset(dx, dy, -3), state("chestnut_planks"));
			}
			set(level, origin.offset(dx, 4, -2), state("chestnut_slab"));
		}
		for (int dx = 15; dx <= 21; dx += 3) {
			set(level, origin.offset(dx, 2, -2), state("ornamental_corn_bundle").setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
			set(level, origin.offset(dx + 1, 3, -2), state("gourd_birdhouse").setValue(LanternBlock.HANGING, true));
			set(level, origin.offset(dx, 0, -1), Blocks.SPRUCE_PLANKS.defaultBlockState());
		}
		String[] mums = {"yellow_mum", "orange_mum", "red_mum", "purple_mum"};
		for (int i = 0; i < 3; i++) {
			set(level, origin.offset(15 + i * 3, 1, -1), state("potted_" + mums[i]));
		}
		for (int i = 0; i < 12; i++) {
			set(level, origin.offset(24 + i % 4, 0, -4 + i / 4), state(mums[(i * 3) % 4]));
		}
	}

	/** Lights the carved giant and every carved pumpkin head (as a torch does), for the night photographs. */
	private static void lightUp(ServerLevel level, BlockPos origin) {
		GiantPumpkinBlockEntity carved = master(level, origin.offset(3, 0, -14));
		if (carved != null) {
			carved.setLit(true);
		}
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(0, 0, -12), origin.offset(24, 3, -5))) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof CarvedPumpkinBlock && state.getValue(CarvedPumpkinBlock.GLOW) > 0) {
				set(level, pos, state.setValue(CarvedPumpkinBlock.LIT, true));
			}
			// A candle in each carved head the scarecrows wear.
			if (level.getBlockEntity(pos) instanceof ScarecrowBlockEntity scarecrow && scarecrow.head().has(JugcraftAgriculture.CARVING)) {
				ItemStack lit = scarecrow.head().copy();
				lit.set(DataComponents.BLOCK_STATE, lit.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
						.with(CarvedPumpkinBlock.LIT, true));
				scarecrow.setHead(lit);
			}
		}
	}
}
