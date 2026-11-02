package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CakeBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CandyCornDispenserBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CandyCornDispenserBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FacingDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FeastTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PartyServingBlock;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Halloween Party Table, parts 1 to 5 (decor set 8): a party corner on a dark oak floor, a
 * Feast Table along its back with a full Mummy Sausage Tray, Witch-Finger Cauldron, Spider Cupcake Tower, Ghost Meringue
 * Platter and Eyeball Jelly Mould set down on its top, with the Goodie Bag Tree and the Skull Goblets (part 5), and the
 * Bat Table Runner laid along it; a Pumpkin Layer Cake on a block beside it, and
 * three Candy Corn Dispensers standing before it, full, half full and empty (their globes by {@code fill}). Photographed
 * close by at the dispensers' height; then with the full one's crank set turning ten ticks before the shot, half way
 * round, and the half-full one's set turning twenty-six ticks before, its piece dropped and its flap swung out
 * (client/CandyCornDispenserRenderer); from above the dispensers, looking down on the table's cake stands; and from off
 * the table's west end, the runner hanging over it (CI job {@code client}). They go into docs/features/halloween-party-table.md.
 */
public class PartyTableClientGameTests implements FabricClientGameTest {
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
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("fill %d %d %d %d %d %d minecraft:dark_oak_planks".formatted(x - 5, y - 1, z - 5, x + 5, y - 1, z + 5));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 5, y, z - 5, x + 5, y + 4, z + 5));
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			// Close by, at their height: the three globes full, half full and empty.
			shoot(context, singleplayer, x + 1, y, z - 3, 0, 20, "jugcraft_party_table_dispensers");
			// The half-full one's crank set turning twenty-six ticks before the shot, the full one's ten.
			server.runOnServer(minecraft -> turn(minecraft, origin));
			context.waitTicks(16);
			server.runOnServer(minecraft -> turn(minecraft, origin.offset(1, 0, 0)));
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_party_table_crank");
			// From above the dispensers, looking down on the cake stands on the table.
			shoot(context, singleplayer, x + 1, y + 1, z - 1, 0, 35, "jugcraft_party_table_stands");
			// From off the table's west end, looking along it: the runner hanging over the end, the tree and the trays.
			shoot(context, singleplayer, x - 5, y + 1, z + 1, -70, 25, "jugcraft_party_table_runner");
            server.runOnServer(minecraft -> {
                var level=minecraft.overworld();
                for(int dx=-3;dx<=4;dx++) {
                    var at=origin.offset(dx,0,2);
                    level.setBlock(at,level.getBlockState(at).setValue(FeastTableBlock.RUNNER,FeastTableBlock.Runner.LINEN),Block.UPDATE_ALL);
                    level.setBlock(at.above(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                }
                for(int dz=-3;dz<=0;dz++) level.setBlock(origin.offset(4,0,dz),
                    JugcraftAgriculture.block("feast_table").defaultBlockState().setValue(FeastTableBlock.AXIS,Direction.Axis.Z)
                        .setValue(FeastTableBlock.RUNNER,FeastTableBlock.Runner.LINEN),Block.UPDATE_ALL);
                for(int dz=-3;dz<=0;dz++) {
                    var at=origin.offset(4,0,dz);
                    level.setBlock(at,Block.updateFromNeighbourShapes(level.getBlockState(at),level,at),Block.UPDATE_ALL);
                }
            });
            shoot(context,singleplayer,x-5,y+1,z,-65,25,"jugcraft_linen_table_connected");
            shoot(context,singleplayer,x+5,y+1,z-4,0,35,"jugcraft_linen_table_other_axis");
            server.runOnServer(minecraft -> minecraft.overworld().destroyBlock(origin.offset(0,0,2),false));
            shoot(context,singleplayer,x,y+1,z-1,0,35,"jugcraft_linen_table_split");
		}
	}

	/** Stands the player at a spot (on an invisible barrier) and takes a screenshot. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z,
			int yaw, int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		// The feast table along the back, eight blocks long, the Bat Table Runner laid along it (part 5).
		BlockState length = JugcraftAgriculture.block("feast_table").defaultBlockState().setValue(FeastTableBlock.AXIS, Direction.Axis.X)
				.setValue(FeastTableBlock.RUNNER, FeastTableBlock.Runner.BATS);
		for (int dx = -3; dx <= 4; dx++) {
			level.setBlock(origin.offset(dx, 0, 2), length, Block.UPDATE_ALL);
		}
		for (int dx = -3; dx <= 4; dx++) {
			BlockPos at = origin.offset(dx, 0, 2);
			level.setBlock(at, Block.updateFromNeighbourShapes(level.getBlockState(at), level, at), Block.UPDATE_ALL);
		}
		// On the table, set down onto its top and full, from west to east: the goodie bag tree, the sausage tray, the
		// witch-finger cauldron, the cupcake tower, the meringue platter, the jelly mould and the skull goblets; its east end
		// bare, to show the runner.
		String[] stands = {"goodie_bag_tree", "mummy_sausage_tray", "witch_finger_cauldron", "spider_cupcake_tower", "ghost_meringue_platter",
				"eyeball_jelly_mould", "skull_goblets"};
		for (int k = 0; k < stands.length; k++) {
			String id = stands[k];
			BlockPos at = origin.offset(k - 3, 1, 2);
			PartyServingBlock stand = (PartyServingBlock) JugcraftAgriculture.block(id);
			level.setBlock(at, stand.defaultBlockState().setValue(FacingDecorationBlock.FACING, Direction.NORTH)
					.setValue(PartyServingBlock.ON_TABLE, true).setValue(stand.servings(), stand.capacity()), Block.UPDATE_ALL);
		}
		// The pumpkin layer cake on a block of stripped oak beside the dispensers, whole, facing the camera.
		level.setBlock(origin.offset(4, 0, 0), Blocks.STRIPPED_OAK_WOOD.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(origin.offset(4, 1, 0), JugcraftAgriculture.block("pumpkin_layer_cake").defaultBlockState()
				.setValue(CakeBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
		// Before it, facing the camera to the north: half full, full and empty, from west to east.
		int[] counts = {CandyCornDispenserBlock.CAPACITY / 2, CandyCornDispenserBlock.CAPACITY, 0};
		for (int k = 0; k < 3; k++) {
			BlockPos at = origin.offset(k, 0, 0);
			level.setBlock(at, JugcraftAgriculture.block("candy_corn_dispenser").defaultBlockState()
					.setValue(FacingDecorationBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
			if (counts[k] > 0 && level.getBlockEntity(at) instanceof CandyCornDispenserBlockEntity dispenser) {
				dispenser.pour(new ItemStack(JugcraftAgriculture.item("candy_corn"), counts[k]));
				level.setBlock(at, level.getBlockState(at).setValue(CandyCornDispenserBlock.FILL, CandyCornDispenserBlock.fill(counts[k])), Block.UPDATE_ALL);
			}
		}
	}

	/** The player turns the crank of the dispenser at {@code at} with an empty hand. */
	private static void turn(net.minecraft.server.MinecraftServer minecraft, BlockPos at) {
		ServerLevel level = minecraft.overworld();
		ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
		level.getBlockState(at).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(at), Direction.NORTH, at, false));
	}
}
