package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.GiantBeatingHeartBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MultiDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpecimenJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpecimenVesselBlock;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Witch's Workshop's bigger jars, each its small jar made bigger: a mad scientist's laboratory
 * floor before a stone wall, with the Giant's Beating Heart (the Beating Heart Jar three times over, its heart beating),
 * a Specimen Tank (the Specimen Jar three times over) holding a brain, two Tall Specimen Jars (twice over: an eye and a
 * tentacle) and, for scale, the Specimen Jar and Beating Heart Jar they grew from, which each should look just like, with
 * their items framed on the wall (each specimen should tell apart). By day, close up, and at night (the jars glowing; the
 * giant heart's jar lit like the room). CI job {@code client}.
 */
public class BiggerJarsClientGameTests implements FabricClientGameTest {
	/** The items framed on the wall: {item, specimen} (none for the Giant's Beating Heart). */
	private static final String[][] FRAMED = {{"specimen_tank", "eye"}, {"specimen_tank", "brain"}, {"specimen_tank", "tentacle"},
			{"specimen_tank", "pumpkin"}, {"tall_specimen_jar", "eye"}, {"giant_beating_heart", ""}};

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 12, y - 3, z - 14, x + 12, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 14, x + 12, y + 10, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			// Their items in frames on the wall: the Specimen Tank with each specimen, a Tall Specimen Jar with an eye and the
			// Giant's Beating Heart.
			for (int i = 0; i < FRAMED.length; i++) {
				String components = FRAMED[i][1].isEmpty() ? "" : ",components:{\"minecraft:block_state\":{specimen:\"%s\"}}".formatted(FRAMED[i][1]);
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1%s}}"
						.formatted(x + 4 + i, y + 2, z - 10, FRAMED[i][0], components));
			}
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			watchFrom(context, singleplayer, origin, new Vec3(-0.5, 2.6, 4.5), 180.0F, 14.0F, "jugcraft_bigger_jars");
			watchFrom(context, singleplayer, origin, new Vec3(-4.5, 2.2, 1.5), 180.0F, 10.0F, "jugcraft_bigger_jars_giant_heart");
			watchFrom(context, singleplayer, origin, new Vec3(-3.0, 4.6, -0.8), 205.0F, 48.0F, "jugcraft_bigger_jars_giant_heart_above");
			watchFrom(context, singleplayer, origin, new Vec3(1.5, 1.7, 0.2), 180.0F, 10.0F, "jugcraft_bigger_jars_specimens");
			watchFrom(context, singleplayer, origin, new Vec3(6.5, 0.9, -7.2), 180.0F, 0.0F, "jugcraft_bigger_jars_items");
			server.runCommand("time set 18000");
			context.waitTicks(20);
			watchFrom(context, singleplayer, origin, new Vec3(-0.5, 2.6, 4.5), 180.0F, 14.0F, "jugcraft_bigger_jars_night");
			watchFrom(context, singleplayer, origin, new Vec3(-4.5, 2.2, 1.5), 180.0F, 10.0F, "jugcraft_bigger_jars_giant_heart_night");
			server.runCommand("time set noon");
			server.runCommand("gamerule minecraft:advance_time true");
		}
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

	/** Every block of a prop of several blocks, its first at {@code master}, facing {@code facing}, from {@code state}. */
	private static void whole(ServerLevel level, BlockPos master, Direction facing, BlockState state) {
		MultiDecorationBlock prop = (MultiDecorationBlock) state.getBlock();
		for (int part = 0; part < prop.cells().length; part++) {
			level.setBlock(prop.partPos(master, facing, part), state.setValue(MultiDecorationBlock.FACING, facing).setValue(prop.partProperty(), part),
					Block.UPDATE_ALL);
		}
	}

	private static void build(ServerLevel level, BlockPos origin) {
		// A laboratory floor of polished deepslate before a stone-brick wall, a lamp each end.
		for (int dx = -9; dx <= 9; dx++) {
			for (int dz = -10; dz <= 1; dz++) {
				put(level, origin, dx, -1, dz, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
			}
			for (int dy = 0; dy < 6; dy++) {
				put(level, origin, dx, dy, -11, Blocks.STONE_BRICKS.defaultBlockState());
			}
		}
		put(level, origin, -9, 3, -10, Blocks.LANTERN.defaultBlockState());
		put(level, origin, 9, 3, -10, Blocks.LANTERN.defaultBlockState());
		// The Giant's Beating Heart, facing out (south), on the west.
		whole(level, origin.offset(-6, 0, -4), Direction.SOUTH, block(JugcraftAgriculture.GIANT_HEART).defaultBlockState()
				.setValue(GiantBeatingHeartBlock.TEMPO, 2));
		// A Specimen Tank (three blocks across, to x + 1) with a brain, and two Tall Specimen Jars with an eye and a tentacle.
		whole(level, origin.offset(-1, 0, -4), Direction.SOUTH, block(JugcraftAgriculture.SPECIMEN_TANK).defaultBlockState()
				.setValue(SpecimenVesselBlock.SPECIMEN, SpecimenJarBlock.Specimen.BRAIN));
		whole(level, origin.offset(2, 0, -4), Direction.SOUTH, block(JugcraftAgriculture.TALL_SPECIMEN_JAR).defaultBlockState());
		whole(level, origin.offset(3, 0, -4), Direction.SOUTH, block(JugcraftAgriculture.TALL_SPECIMEN_JAR).defaultBlockState()
				.setValue(SpecimenVesselBlock.SPECIMEN, SpecimenJarBlock.Specimen.TENTACLE));
		// For scale, the small jars they grew from.
		put(level, origin, 4, 0, -4, block("specimen_jar").defaultBlockState().setValue(SpecimenJarBlock.SPECIMEN, SpecimenJarBlock.Specimen.PUMPKIN));
		put(level, origin, 5, 0, -4, block("beating_heart_jar").defaultBlockState());
	}
}
