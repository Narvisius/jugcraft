package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.HotAirBalloon;
import io.github.jimbozoomer.jugcraft.agriculture.HotAirBalloonItem;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MooringPostBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PibalItem;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the hot-air balloon fiesta (fall addition 29): a basket up close; the test player climbing aboard
 * the Jack-o'-Lantern, moored to a post, and firing its burner (holding jump) until the rope holds it; the view from up
 * there and its rider seen from outside; then, from a camera stand on the ground, the field with the Jack-o'-Lantern aloft
 * over the other two and pibals rising, and the night glow with its burner roaring. CI job {@code client}.
 */
public class BalloonClientGameTests implements FabricClientGameTest {
	/** Where the overview camera stands: in front of the field, looking north across it. */
	private static final Vec3 OVERVIEW = new Vec3(0.5, 2.0, 14.5);

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
			server.runCommand("time set 1000");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 24, y - 3, z - 34, x + 24, y - 1, z + 16));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 24, y, z - 34, x + 24, y + 30, z + 16));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x - 8, y, z - 6, 180, -12, "jugcraft_balloon_basket");

			// Aboard the moored Jack-o'-Lantern as its pilot: hold the burner until the rope holds it up.
			place(context, singleplayer, x + 2, y, z - 12, 180, 0);
			server.runCommand("execute as @p at @s run ride @s mount @e[type=jugcraft:hot_air_balloon,limit=1,sort=nearest]");
			context.waitTicks(10);
			// Left without a pilot it vented while the other shots were taken: warm it again, as if the burner had been on.
			server.runOnServer(minecraft -> pumpkin(minecraft.overworld(), origin).ifPresent(b -> b.setHeat(0.8F)));
			context.getInput().holdKey(options -> options.keyJump);
			context.waitTicks(200);
			context.getInput().releaseKey(options -> options.keyJump);
			double up = server.computeOnServer(minecraft -> pumpkin(minecraft.overworld(), origin).map(b -> b.getY() - y).orElse(-1.0));
			System.out.println("[jugcraft balloon client test] the Jack-o'-Lantern is " + up + " blocks up");

			// The view from up there, out over the field; then its rider seen from outside.
			server.runCommand("rotate @p 200 35");
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_balloon_ride");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			server.runCommand("rotate @p 160 15");
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_balloon_riders");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

			// From the ground: the field, the Jack-o'-Lantern aloft on its rope, pibals let go a second apart.
			for (int i = 0; i < 3; i++) {
				int k = i;
				server.runOnServer(minecraft -> PibalItem.release(minecraft.overworld(), Vec3.atCenterOf(origin.offset(4 + k, 1, 2))));
				context.waitTicks(20);
			}
			watchFrom(context, singleplayer, origin, OVERVIEW, 180.0F, -18.0F, "jugcraft_balloons");

			// The night glow: the pilot fires the burner.
			server.runCommand("time set 18000");
			context.getInput().holdKey(options -> options.keyJump);
			context.waitTicks(20);
			watchFrom(context, singleplayer, origin, OVERVIEW, 180.0F, -18.0F, "jugcraft_balloons_night");
			watchFrom(context, singleplayer, origin, new Vec3(2.5, 6.0, -2.5), 180.0F, -38.0F, "jugcraft_balloon_glow");
			context.getInput().releaseKey(options -> options.keyJump);
			server.runCommand("time set 1000");
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
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

	/**
	 * Looks through an invisible armor stand at {@code at} (from the origin) along yaw and pitch, so the player can stay
	 * aboard their balloon, burner and all, while the camera stands elsewhere; then gives the camera back.
	 */
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
		context.waitTicks(10);
		context.runOnClient(client -> {
			Entity stand = client.level.getEntity(id);
			if (stand != null) {
				client.setCameraEntity(stand);
			}
		});
		context.waitTicks(20);
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

	/** The Jack-o'-Lantern balloon, wherever it has got to. */
	private static Optional<HotAirBalloon> pumpkin(ServerLevel level, BlockPos origin) {
		return level.getEntitiesOfClass(HotAirBalloon.class, new AABB(origin).inflate(48.0), b -> b.kind() == HotAirBalloon.Kind.PUMPKIN).stream()
				.findFirst();
	}

	/**
	 * The fiesta field: Harvest Stripes and Harvest Moon cold on the ground, and the Jack-o'-Lantern, fuelled, tied to a
	 * Mooring Post; a few pumpkins and hay bales about.
	 */
	private static void build(ServerLevel level, BlockPos origin) {
		BlockPos ground = origin.below();
		HotAirBalloonItem.setUp(level, ground.offset(-8, 0, -12), HotAirBalloon.Kind.HARVEST, 0.0F, 400);
		HotAirBalloonItem.setUp(level, ground.offset(12, 0, -26), HotAirBalloon.Kind.MOON, 0.0F, 400);
		BlockPos post = origin.offset(-1, 0, -14);
		level.setBlock(post, JugcraftAgriculture.block(MooringPostBlock.ID).defaultBlockState(), Block.UPDATE_ALL);
		HotAirBalloon pumpkin = HotAirBalloonItem.setUp(level, ground.offset(2, 0, -16), HotAirBalloon.Kind.PUMPKIN, 0.0F, 6000);
		if (pumpkin != null) {
			pumpkin.moor(post);
		}
		for (int i = 0; i < 5; i++) {
			level.setBlock(origin.offset(-14 + i * 7, 0, -4 - (i % 2) * 3), (i % 2 == 0 ? Blocks.HAY_BLOCK : Blocks.PUMPKIN).defaultBlockState(),
					Block.UPDATE_ALL);
		}
	}
}
