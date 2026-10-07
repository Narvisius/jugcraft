package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CostumeTrunkBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CostumeTrunkBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.List;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Client game test for costumes: a row of armor stands in the six outfits; a zombie crouching with its vampire cape
 * wrapped round it, a skeleton in a werewolf mask and a husk in mummy wraps; an armor stand floating in the air with its
 * bat wings spread; an open Costume Trunk. Photographed from the front and the back by day, and at night, when the
 * skeleton suit's bones glow (CI job {@code client}).
 */
public class Decor14ClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 16, x + 18, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 16, x + 18, y + 10, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 6, y + 1, z, 180, 10, "jugcraft_outfits");
			shoot(context, singleplayer, x + 6, y + 1, z - 12, 0, 10, "jugcraft_outfits_back");
			shoot(context, singleplayer, x + 15, y + 1, z - 2, 180, 8, "jugcraft_outfits_on_mobs");
			shoot(context, singleplayer, x + 15, y + 2, z - 14, 0, 12, "jugcraft_bat_wings_flying");
			shoot(context, singleplayer, x - 2, y + 2, z - 2, 180, 45, "jugcraft_costume_trunk");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 6, y + 1, z, 180, 10, "jugcraft_outfits_night");
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

	private static Entity vanilla(ServerLevel level, String id) {
		return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id)).create(level, EntitySpawnReason.COMMAND);
	}

	/** A vanilla entity wearing {@code outfit}, at (x, y, z) facing {@code yaw}, standing still. */
	private static Entity dressed(ServerLevel level, String id, String outfit, double x, double y, double z, float yaw) {
		Entity entity = vanilla(level, id);
		if (entity instanceof LivingEntity wearer) {
			wearer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JugcraftAgriculture.item(outfit)));
			wearer.snapTo(x, y, z, yaw, 0.0F);
			wearer.setYBodyRot(yaw);
			wearer.setYHeadRot(yaw);
		}
		if (entity instanceof Mob mob) {
			mob.setNoAi(true);
			mob.setPersistenceRequired();
		}
		return entity;
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The six outfits on armor stands, facing south (toward the front camera).
		List<String> outfits = JugcraftAgriculture.OUTFITS;
		for (int i = 0; i < outfits.size(); i++) {
			level.addFreshEntity(dressed(level, "armor_stand", outfits.get(i), x + 1.5 + i * 2, y, z - 5.5, 0.0F));
		}
		// Mobs in costume: the zombie crouches, so its cape wraps round it.
		Entity zombie = dressed(level, "zombie", "vampire_cape", x + 13.5, y, z - 6.5, 0.0F);
		zombie.setPose(Pose.CROUCHING);
		level.addFreshEntity(zombie);
		level.addFreshEntity(dressed(level, "skeleton", "werewolf_mask", x + 15.5, y, z - 6.5, 0.0F));
		level.addFreshEntity(dressed(level, "husk", "mummy_wraps", x + 17.5, y, z - 6.5, 0.0F));
		// A stand floating in the air, its bat wings spread, its back to the camera behind it.
		Entity flyer = dressed(level, "armor_stand", "bat_wings", x + 15.5, y + 1.5, z - 9.5, 0.0F);
		flyer.setNoGravity(true);
		level.addFreshEntity(flyer);
		// An open trunk with three outfits in it.
		BlockPos trunk = new BlockPos(x - 2, y, z - 5);
		level.setBlock(trunk, JugcraftAgriculture.block("costume_trunk").defaultBlockState().setValue(CostumeTrunkBlock.FACING, Direction.SOUTH)
				.setValue(CostumeTrunkBlock.OPEN, true), Block.UPDATE_ALL);
		if (level.getBlockEntity(trunk) instanceof CostumeTrunkBlockEntity chest) {
			for (String outfit : List.of("vampire_cape", "cat_ears_and_tail", "witch_hat")) {
				chest.store(new ItemStack(JugcraftAgriculture.item(outfit)));
			}
		}
		level.setBlock(new BlockPos(x - 2, y, z - 6), Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
	}
}
