package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenBonfireBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.ToiletPaperStreamerBlock;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Client game test for night events: a house front with a jack o'lantern porch light, a Candy Bowl and three
 * village children in costume at its door (posed, as trick-or-treaters stand there); two trees and a fence hung with
 * toilet paper; a lit Halloween Bonfire roasting chestnuts and corn; a Haunted Hayride on its rails with three children in
 * costume aboard. Photographed by day and at night (CI job {@code client}).
 */
public class Decor12ClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 20, x + 30, y - 1, z + 14));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 20, x + 30, y + 12, z + 14));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin, minecraft.getPlayerList().getPlayers().getFirst()));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();

			// The bonfire's food roasts in 300 ticks, so it is photographed early.
			shoot(context, singleplayer, x + 12, y + 4, z + 6, 180, 18, "jugcraft_night_events");
			shoot(context, singleplayer, x + 4, y + 1, z - 2, 180, 8, "jugcraft_toilet_papered_yard");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 14, y + 1, z - 3, 180, 20, "jugcraft_halloween_bonfire_night");
			shoot(context, singleplayer, x + 4, y + 1, z - 4, 180, 5, "jugcraft_trick_or_treaters_night");
			shoot(context, singleplayer, x + 22, y + 3, z - 2, 180, 32, "jugcraft_haunted_hayride_night");
			shoot(context, singleplayer, x + 12, y + 4, z + 6, 180, 18, "jugcraft_night_events_night");
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

	private static BlockState state(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState();
	}

	private static Entity vanilla(ServerLevel level, String id) {
		return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id)).create(level, EntitySpawnReason.COMMAND);
	}

	private static void tree(ServerLevel level, int x, int y, int z) {
		BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 3; dy <= 4; dy++) {
					if (Math.abs(dx) + Math.abs(dz) < 4) {
						set(level, new BlockPos(x + dx, y + dy, z + dz), leaves);
					}
				}
			}
		}
		set(level, new BlockPos(x, y + 5, z), leaves);
		for (int dy = 0; dy <= 4; dy++) {
			set(level, new BlockPos(x, y + dy, z), Blocks.OAK_LOG.defaultBlockState());
		}
	}

	private static void build(ServerLevel level, BlockPos origin, ServerPlayer player) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The house front: a plank wall with a door, a jack o'lantern by the door and a candy bowl in front of it.
		for (int dx = 0; dx <= 8; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 11), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
		BlockPos door = new BlockPos(x + 4, y, z - 11);
		set(level, door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		set(level, door.above(), Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		set(level, new BlockPos(x + 5, y, z - 10), Blocks.JACK_O_LANTERN.defaultBlockState());
		BlockPos bowl = new BlockPos(x + 3, y, z - 10);
		set(level, bowl, state("candy_bowl").setValue(CandyBowlBlock.FACING, Direction.SOUTH));
		if (level.getBlockEntity(bowl) instanceof CandyBowlBlockEntity treats) {
			treats.add(new ItemStack(JugcraftAgriculture.item("candy_corn"), 20));
		}
		// Three children in costume at the door, posed.
		String[] costumes = {"jugcraft:witch_hat", "minecraft:carved_pumpkin", "jugcraft:scarecrow_hat"};
		for (int i = 0; i < costumes.length; i++) {
			if (vanilla(level, "villager") instanceof Mob kid) {
				if (kid instanceof AgeableMob young) {
					young.setAge(-24000);
				}
				kid.setItemSlot(EquipmentSlot.HEAD, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(costumes[i]))));
				kid.snapTo(x + 3.0 + i * 0.9, y, z - 8.7, 180.0F, 0.0F);
				kid.setYHeadRot(180.0F);
				kid.setNoAi(true);
				level.addFreshEntity(kid);
			}
		}
		// Two trees and a fence, toilet-papered.
		tree(level, x + 1, y, z - 6);
		tree(level, x + 8, y, z - 7);
		for (int dx = 0; dx <= 9; dx++) {
			set(level, new BlockPos(x + dx, y, z - 3), Blocks.OAK_FENCE.defaultBlockState());
		}
		RandomSource random = RandomSource.create(1031);
		ToiletPaperStreamerBlock.drape(level, new BlockPos(x + 1, y + 2, z - 6), 3, 8, random);
		ToiletPaperStreamerBlock.drape(level, new BlockPos(x + 8, y + 2, z - 7), 3, 8, random);
		ToiletPaperStreamerBlock.drape(level, new BlockPos(x + 4, y + 1, z - 3), 4, 5, random);
		// The bonfire, roasting chestnuts and corn.
		BlockPos bonfire = new BlockPos(x + 14, y, z - 6);
		set(level, bonfire, state("halloween_bonfire"));
		if (level.getBlockEntity(bonfire) instanceof HalloweenBonfireBlockEntity fire) {
			for (ItemStack food : new ItemStack[] {new ItemStack(JugcraftAgriculture.item("chestnut")), new ItemStack(JugcraftAgriculture.item("corn")),
					new ItemStack(JugcraftAgriculture.item("chestnut")), new ItemStack(Items.POTATO)}) {
				fire.cook(level, food, player);
			}
		}
		// The hayride on a stretch of rail, three children in costume aboard.
		for (int dx = 18; dx <= 27; dx++) {
			set(level, new BlockPos(x + dx, y, z - 6), Blocks.RAIL.defaultBlockState());
		}
		Entity ride = JugcraftAgriculture.HAUNTED_HAYRIDE.create(level, EntitySpawnReason.COMMAND);
		if (ride != null) {
			// A minecart along x faces 0 degrees.
			ride.snapTo(x + 22.5, y + 0.0625, z - 5.5, 0.0F, 0.0F);
			level.addFreshEntity(ride);
			String[] riders = {"jugcraft:ghost_sheet", "jugcraft:witch_hat", "minecraft:carved_pumpkin"};
			for (String costume : riders) {
				if (vanilla(level, "villager") instanceof Mob rider) {
					if (rider instanceof AgeableMob young) {
						young.setAge(-24000);
					}
					rider.setItemSlot(EquipmentSlot.HEAD, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(costume))));
					rider.snapTo(x + 22.5, y + 1, z - 5.5, 180.0F, 0.0F);
					rider.setNoAi(true);
					level.addFreshEntity(rider);
					rider.startRiding(ride);
				}
			}
		}
	}
}
