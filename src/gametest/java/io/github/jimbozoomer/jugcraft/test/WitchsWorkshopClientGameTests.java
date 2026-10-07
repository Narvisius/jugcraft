package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.BatJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BroomRackBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Candelabra;
import io.github.jimbozoomer.jugcraft.agriculture.CandelabrumBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CuriosityCabinetBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DustpanBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DustpanBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.EnchantedBroomBlock;
import io.github.jimbozoomer.jugcraft.agriculture.EnchantedBroomBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HandJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HornedSkullCauldronBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HornedSkullCauldronBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MothCaseBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ShowcaseBlockEntity;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Witch's Workshop: a dark oak room open to the south. Two Horned Skull Cauldrons stand on Ember
 * Beds, brewing green and pink potions with ingredients floating in them, and by the door an empty one and one a third
 * full, looked down into; a Curiosity Cabinet of oddments stands against
 * the back wall between a Moth Display Case and a Broom Rack; the five Oddity Jars and a Bell Jar stand on a table with a
 * table candelabrum; a floor candelabrum, a wall girandole and a branching chandelier burn in four waxes and four
 * flames; and an Enchanted Broom sweeps beside a Dustpan. By day, close up, and by candlelight at night. CI job
 * {@code client}.
 */
public class WitchsWorkshopClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 10, y - 3, z - 12, x + 10, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 12, x + 10, y + 10, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x, y + 1, z + 2, 180, 12, "jugcraft_witchs_workshop");
			watchFrom(context, singleplayer, origin, new Vec3(-4.5, 1.3, -3.2), 200.0F, 32.0F, "jugcraft_witchs_workshop_cauldrons");
			watchFrom(context, singleplayer, origin, new Vec3(-3.5, 2.4, -0.1), 180.0F, 50.0F, "jugcraft_witchs_workshop_cauldrons_inside");
			watchFrom(context, singleplayer, origin, new Vec3(0.5, 0.9, -3.5), 180.0F, 12.0F, "jugcraft_witchs_workshop_cabinet");
			watchFrom(context, singleplayer, origin, new Vec3(3.5, 0.6, -3.0), 180.0F, 26.0F, "jugcraft_witchs_workshop_jars");
			watchFrom(context, singleplayer, origin, new Vec3(4.0, 0.4, -1.5), 140.0F, 24.0F, "jugcraft_witchs_workshop_broom");
			server.runCommand("time set 18000");
			context.waitTicks(20);
			shoot(context, singleplayer, x, y + 1, z + 2, 180, 12, "jugcraft_witchs_workshop_night");
			watchFrom(context, singleplayer, origin, new Vec3(-1.0, 0.8, -1.8), 180.0F, -8.0F, "jugcraft_witchs_workshop_candelabra");
			watchFrom(context, singleplayer, origin, new Vec3(3.0, 1.0, -4.0), 180.0F, 10.0F, "jugcraft_witchs_workshop_moths");
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

	private static BlockState fitting(String id, Candelabra.Wax wax, Candelabra.Flame flame) {
		return block(id).defaultBlockState().setValue(Candelabra.WAX, wax).setValue(Candelabra.FLAME, flame).setValue(Candelabra.LIT, true);
	}

	private static void cauldron(ServerLevel level, BlockPos origin, int dx, int dz, Holder<Potion> potion, List<ItemStack> floating) {
		put(level, origin, dx, 0, dz, block("ember_bed").defaultBlockState());
		put(level, origin, dx, 1, dz, block("horned_skull_cauldron").defaultBlockState().setValue(HornedSkullCauldronBlock.FACING, Direction.SOUTH)
				.setValue(HornedSkullCauldronBlock.LEVEL, 3).setValue(HornedSkullCauldronBlock.POTION, true).setValue(HornedSkullCauldronBlock.HEATED, true));
		if (level.getBlockEntity(origin.offset(dx, 1, dz)) instanceof HornedSkullCauldronBlockEntity pot) {
			pot.setContents(new PotionContents(potion));
			floating.forEach(pot::addFloating);
		}
	}

	private static void build(ServerLevel level, BlockPos origin) {
		// The workshop: a dark oak floor, back and side walls five high and a ceiling, open to the south.
		for (int dx = -7; dx <= 7; dx++) {
			for (int dz = -8; dz <= 0; dz++) {
				put(level, origin, dx, -1, dz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
				put(level, origin, dx, 5, dz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
			for (int dy = 0; dy < 5; dy++) {
				put(level, origin, dx, dy, -8, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
		for (int dz = -8; dz <= 0; dz++) {
			for (int dy = 0; dy < 5; dy++) {
				put(level, origin, -7, dy, dz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
				put(level, origin, 7, dy, dz, Blocks.DARK_OAK_PLANKS.defaultBlockState());
			}
		}
		// Two cauldrons brewing over ember beds on the west side, ingredients floating in them.
		cauldron(level, origin, -5, -5, Potions.POISON, List.of(new ItemStack(Items.SPIDER_EYE), new ItemStack(Items.BONE), new ItemStack(Items.RED_MUSHROOM)));
		cauldron(level, origin, -3, -6, Potions.REGENERATION, List.of(new ItemStack(Items.APPLE), new ItemStack(Items.FEATHER)));
		// By the door, an empty cold cauldron and one holding a single bottle of swiftness, to look down into.
		put(level, origin, -5, 0, -2, block("horned_skull_cauldron").defaultBlockState().setValue(HornedSkullCauldronBlock.FACING, Direction.SOUTH));
		put(level, origin, -3, 0, -2, block("horned_skull_cauldron").defaultBlockState().setValue(HornedSkullCauldronBlock.FACING, Direction.SOUTH)
				.setValue(HornedSkullCauldronBlock.LEVEL, 1).setValue(HornedSkullCauldronBlock.POTION, true));
		if (level.getBlockEntity(origin.offset(-3, 0, -2)) instanceof HornedSkullCauldronBlockEntity pot) {
			pot.setContents(new PotionContents(Potions.SWIFTNESS));
		}
		// The Curiosity Cabinet on the back wall, full of oddments, with the moth case and the broom rack either side.
		BlockState cabinet = block("curiosity_cabinet").defaultBlockState().setValue(CuriosityCabinetBlock.FACING, Direction.SOUTH);
		put(level, origin, 0, 0, -7, cabinet.setValue(CuriosityCabinetBlock.HALF, DoubleBlockHalf.LOWER));
		put(level, origin, 0, 1, -7, cabinet.setValue(CuriosityCabinetBlock.HALF, DoubleBlockHalf.UPPER));
		if (level.getBlockEntity(origin.offset(0, 0, -7)) instanceof ShowcaseBlockEntity shelves) {
			List<ItemStack> oddments = List.of(new ItemStack(Items.SKELETON_SKULL), new ItemStack(Items.AMETHYST_CLUSTER), new ItemStack(Items.GOLDEN_APPLE),
					new ItemStack(Items.BOOK), new ItemStack(Items.SPIDER_EYE), new ItemStack(Items.CLOCK), new ItemStack(Items.WITHER_ROSE),
					PotionContents.createItemStack(Items.POTION, Potions.NIGHT_VISION), new ItemStack(Items.NAUTILUS_SHELL));
			for (int i = 0; i < oddments.size(); i++) {
				shelves.put(i, oddments.get(i));
			}
		}
		put(level, origin, 2, 2, -7, block("moth_display_case").defaultBlockState().setValue(MothCaseBlock.FACING, Direction.SOUTH));
		put(level, origin, 3, 2, -7, block("moth_display_case").defaultBlockState().setValue(MothCaseBlock.FACING, Direction.SOUTH)
				.setValue(MothCaseBlock.MOTH, MothCaseBlock.Moth.DEATHS_HEAD));
		put(level, origin, 4, 2, -7, block("moth_display_case").defaultBlockState().setValue(MothCaseBlock.FACING, Direction.SOUTH)
				.setValue(MothCaseBlock.MOTH, MothCaseBlock.Moth.ATLAS));
		put(level, origin, -2, 2, -7, block("broom_rack").defaultBlockState().setValue(BroomRackBlock.FACING, Direction.SOUTH));
		if (level.getBlockEntity(origin.offset(-2, 2, -7)) instanceof ShowcaseBlockEntity rack) {
			rack.put(0, new ItemStack(JugcraftAgriculture.item("witchs_broom")));
			rack.put(2, new ItemStack(JugcraftAgriculture.item("enchanted_broom")));
		}
		// A table of spruce slabs on the east side with the oddity jars, a bell jar and a table candelabrum.
		BlockState top = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
		for (int dx = 2; dx <= 6; dx++) {
			put(level, origin, dx, 0, -5, top);
		}
		put(level, origin, 2, 1, -5, block("jar_of_eyeballs").defaultBlockState());
		put(level, origin, 3, 1, -5, block("beating_heart_jar").defaultBlockState());
		put(level, origin, 4, 1, -5, block("bat_in_a_jar").defaultBlockState().setValue(BatJarBlock.AWAKE, true));
		put(level, origin, 5, 1, -5, block("two_headed_snake_jar").defaultBlockState());
		put(level, origin, 6, 1, -5, block("hand_in_a_jar").defaultBlockState().setValue(HandJarBlock.POWERED, false));
		put(level, origin, 4, 1, -6, Blocks.SPRUCE_PLANKS.defaultBlockState());
		put(level, origin, 4, 2, -6, block("bell_jar").defaultBlockState());
		if (level.getBlockEntity(origin.offset(4, 2, -6)) instanceof ShowcaseBlockEntity jar) {
			jar.put(0, new ItemStack(Items.JACK_O_LANTERN));
		}
		put(level, origin, 6, 1, -6, Blocks.SPRUCE_PLANKS.defaultBlockState());
		put(level, origin, 6, 2, -6, fitting("table_candelabrum", Candelabra.Wax.IVORY, Candelabra.Flame.ORDINARY)
				.setValue(CandelabrumBlock.FACING, Direction.SOUTH));
		// A floor candelabrum in purple witchfire by the cabinet, a black girandole in soul flame on the west wall, a red-waxed
		// table candelabrum in ghostfire, and a branching chandelier from the ceiling.
		BlockState floor = fitting("floor_candelabrum", Candelabra.Wax.PURPLE, Candelabra.Flame.WITCHFIRE).setValue(CuriosityCabinetBlock.FACING, Direction.SOUTH);
		put(level, origin, -1, 0, -6, floor.setValue(CuriosityCabinetBlock.HALF, DoubleBlockHalf.LOWER));
		put(level, origin, -1, 1, -6, floor.setValue(CuriosityCabinetBlock.HALF, DoubleBlockHalf.UPPER));
		put(level, origin, -6, 2, -3, fitting("wall_girandole", Candelabra.Wax.BLACK, Candelabra.Flame.SOUL).setValue(CandelabrumBlock.FACING, Direction.EAST)
				.setValue(Candelabra.DRIPS, 3));
		put(level, origin, 1, 0, -6, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
		put(level, origin, 1, 1, -6, fitting("table_candelabrum", Candelabra.Wax.RED, Candelabra.Flame.GHOSTFIRE)
				.setValue(CandelabrumBlock.FACING, Direction.SOUTH).setValue(Candelabra.DRIPS, 2));
		put(level, origin, 0, 4, -4, fitting("branching_chandelier", Candelabra.Wax.IVORY, Candelabra.Flame.ORDINARY).setValue(Candelabra.DRIPS, 1));
		// An Enchanted Broom, awake and sweeping, beside a Dustpan with sweepings in it.
		put(level, origin, 4, 0, -2, block("enchanted_broom").defaultBlockState().setValue(EnchantedBroomBlock.CHARGED, true));
		if (level.getBlockEntity(origin.offset(4, 0, -2)) instanceof EnchantedBroomBlockEntity broom) {
			broom.anoint();
		}
		put(level, origin, 5, 0, -3, block("dustpan").defaultBlockState().setValue(DustpanBlock.FACING, Direction.WEST));
		if (level.getBlockEntity(origin.offset(5, 0, -3)) instanceof DustpanBlockEntity pan) {
			pan.setItem(0, new ItemStack(Items.OAK_LEAVES, 3));
			pan.setItem(1, new ItemStack(Items.BONE, 2));
			pan.setItem(2, new ItemStack(Items.FEATHER));
		}
	}
}
