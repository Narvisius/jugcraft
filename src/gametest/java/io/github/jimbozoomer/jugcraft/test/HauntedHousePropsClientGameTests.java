package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MonsterHeadBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PlushBlock;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the haunted house's props: a parlour open to the south, its dark oak walls and ceiling strung with
 * spider webs, pillar candles of each count lit on a long table, two monster's heads on pedestals (one awake), flying
 * eyeballs hovering about the room staring at the player, and the five harvest plushes on a shelf behind the candles. By day, close up,
 * and by candlelight at night, with the eyeballs close up at night and one seen from behind; last, the eyeball item held in
 * the hand and in the hotbar. CI job {@code client}.
 */
public class HauntedHousePropsClientGameTests implements FabricClientGameTest {
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
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 9, y - 3, z - 12, x + 9, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 9, y, z - 12, x + 9, y + 10, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x, y + 1, z + 1, 180, 10, "jugcraft_haunted_house_props");
			shoot(context, singleplayer, x + 1, y + 1, z, 180, 8, "jugcraft_haunted_house_eyeballs");
			watchFrom(context, singleplayer, origin, new Vec3(-2.5, 0.2, -3.0), 180.0F, 14.0F, "jugcraft_haunted_house_candles");
			watchFrom(context, singleplayer, origin, new Vec3(4.0, 0.0, -3.0), 180.0F, 6.0F, "jugcraft_haunted_house_monster_heads");
			watchFrom(context, singleplayer, origin, new Vec3(-3.5, 0.6, -4.0), 135.0F, -25.0F, "jugcraft_haunted_house_webs");
			watchFrom(context, singleplayer, origin, new Vec3(-2.5, 0.6, -4.5), 180.0F, 0.0F, "jugcraft_haunted_house_plushes");
			server.runCommand("time set 18000");
			context.waitTicks(20);
			shoot(context, singleplayer, x, y + 1, z + 1, 180, 10, "jugcraft_haunted_house_props_night");
			// The eyeballs by candlelight, close: each glowing iris on its white, with nothing glowing round it.
			shoot(context, singleplayer, x + 1, y + 1, z, 180, 8, "jugcraft_haunted_house_eyeballs_night");
			// One from behind (it stares at the player, not this camera): its wings' backs and its nerve. The camera stands
			// in the open block between the monster's head on its pedestal and the eyeball at (3, 2, -4), its eye level with
			// the eyeball's middle, a block and a half behind it.
			watchFrom(context, singleplayer, origin, new Vec3(3.5, 0.75, -5.0), 0.0F, 0.0F, "jugcraft_haunted_house_eyeball_back");
			server.runCommand("time set noon");
			// The item by day, held in the hand and in the hotbar (the HUD shown): its wings are two faces 0.1 pixel apart,
			// so they must show cleanly whether or not the item renderer culls back faces.
			server.runOnServer(minecraft -> minecraft.getPlayerList().getPlayers().get(0)
					.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftAgriculture.item("flying_eyeball"))));
			context.runOnClient(client -> {
				if (client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			shoot(context, singleplayer, x, y + 1, z + 1, 180, 10, "jugcraft_haunted_house_eyeball_item");
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

	private static void watchFrom(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos origin, Vec3 at, float yaw,
			float pitch, String name) {
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

	private static void build(ServerLevel level, BlockPos origin) {
		// The parlour: a dark oak floor, back and side walls four high and a ceiling, open to the south.
		for (int dx = -7; dx <= 7; dx++) {
			for (int dz = -8; dz <= 0; dz++) {
				put(level, origin, dx, -1, dz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
				put(level, origin, dx, 4, dz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
			for (int dy = 0; dy < 4; dy++) {
				put(level, origin, dx, dy, -8, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
		for (int dz = -8; dz <= 0; dz++) {
			for (int dy = 0; dy < 4; dy++) {
				put(level, origin, -7, dy, dz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
				put(level, origin, 7, dy, dz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
		// Spider webs in the back corners, on both walls and the ceiling, and along the back wall's top.
		BlockState web = block("spider_web").defaultBlockState();
		put(level, origin, -6, 3, -7, web.setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.WEST, true)
				.setValue(BlockStateProperties.UP, true));
		put(level, origin, -6, 2, -7, web.setValue(BlockStateProperties.WEST, true));
		put(level, origin, -5, 3, -7, web.setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.UP, true));
		put(level, origin, 6, 3, -7, web.setValue(BlockStateProperties.NORTH, true).setValue(BlockStateProperties.EAST, true)
				.setValue(BlockStateProperties.UP, true));
		put(level, origin, -6, 3, -5, web.setValue(BlockStateProperties.WEST, true));
		put(level, origin, 1, 3, -7, web.setValue(BlockStateProperties.NORTH, true));
		// A long table of spruce slabs with pillar candles lit, ivory and black, one to four in a cluster.
		BlockState top = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
		for (int dx = -5; dx <= 0; dx++) {
			put(level, origin, dx, 0, -6, top);
		}
		String[] colours = {"ivory_pillar_candle", "black_pillar_candle"};
		for (int i = 0; i < 4; i++) {
			BlockState candles = block(colours[i % 2]).defaultBlockState().setValue(CandleBlock.CANDLES, i + 1).setValue(CandleBlock.LIT, true);
			put(level, origin, -5 + i, 1, -6, candles);
		}
		put(level, origin, -1, 1, -6, block("ivory_pillar_candle").defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
		put(level, origin, 0, 1, -6, block("black_pillar_candle").defaultBlockState().setValue(CandleBlock.CANDLES, 2));
		// Two monster's heads on stone brick pedestals, the second awake, a redstone block hidden behind it in the wall.
		for (int i = 0; i < 2; i++) {
			put(level, origin, 3 + i * 2, 0, -6, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
			put(level, origin, 3 + i * 2, 1, -6, block("monster_head").defaultBlockState().setValue(MonsterHeadBlock.FACING, Direction.SOUTH)
					.setValue(MonsterHeadBlock.POWERED, i == 1));
		}
		put(level, origin, 5, 1, -7, Blocks.REDSTONE_BLOCK.defaultBlockState());
		// Flying eyeballs hovering about the room.
		BlockState eyeball = block("flying_eyeball").defaultBlockState();
		put(level, origin, -2, 2, -4, eyeball);
		put(level, origin, 1, 1, -3, eyeball);
		put(level, origin, 3, 2, -4, eyeball);
		put(level, origin, -4, 3, -3, eyeball);
		// The harvest plushes on a shelf against the back wall, behind the candles.
		String[] plushes = {"owl_plush", "hedgehog_plush", "acorn_plush", "corn_plush", "maple_leaf_plush"};
		for (int i = 0; i < plushes.length; i++) {
			put(level, origin, -5 + i, 1, -7, Blocks.SPRUCE_PLANKS.defaultBlockState());
			put(level, origin, -5 + i, 2, -7, block(plushes[i]).defaultBlockState().setValue(PlushBlock.FACING, Direction.SOUTH));
		}
	}
}
