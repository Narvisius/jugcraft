package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Pinata;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Client game test for the piñata party (fall addition 28): a pumpkin, a star and a bat piñata hanging from an oak
 * pergola; the three torn after half their hits; the star bursting in confetti, its candy on the ground; and the view
 * through the Blindfold. CI job {@code client}.
 */
public class PinataClientGameTests implements FabricClientGameTest {
	private static final Pinata.Kind[] KINDS = {Pinata.Kind.PUMPKIN, Pinata.Kind.STAR, Pinata.Kind.BAT};

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
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 10, x + 8, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 10, x + 8, y + 8, z + 6));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x, y + 1, z, 180, -8, "jugcraft_pinatas");
			server.runOnServer(minecraft -> hit(minecraft.overworld(), origin, false));
			context.waitTicks(60);
			// Three blocks from the beam, all three in view.
			shoot(context, singleplayer, x, y + 1, z - 2, 180, -4, "jugcraft_pinatas_torn");
			place(context, singleplayer, x, y + 1, z, 180, -2);
			server.runOnServer(minecraft -> hit(minecraft.overworld(), origin, true));
			context.waitTicks(4);
			context.takeScreenshot("jugcraft_pinata_burst");

			// Through the Blindfold: the screen dark but for a sliver at its foot. The overlay is part of the HUD.
			// Wait out the Piñata Party toast first, so it doesn't cover the view.
			server.runCommand("item replace entity @p armor.head with jugcraft:blindfold");
			context.waitTicks(120);
			context.runOnClient(client -> client.gui.hud.toggle());
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_blindfold");
			context.runOnClient(client -> client.gui.hud.toggle());
			server.runCommand("item replace entity @p armor.head with minecraft:air");
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

	/** The pergola's beam, five blocks north of the start, from which the three piñatas hang. */
	private static BlockPos beam(BlockPos origin, int i) {
		return origin.offset(-2 + 2 * i, 4, -5);
	}

	/**
	 * Half the hits each piñata takes, from the test's player; or, {@code burst}, the star's last ones, so it bursts.
	 */
	private static void hit(ServerLevel level, BlockPos origin, boolean burst) {
		ServerPlayer player = level.getServer().getPlayerList().getPlayers().get(0);
		float full = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		for (int i = 0; i < KINDS.length; i++) {
			List<Pinata> found = level.getEntitiesOfClass(Pinata.class, new AABB(beam(origin, i)).inflate(0.5, 3.0, 0.5));
			for (Pinata pinata : found) {
				if (burst && pinata.kind() == Pinata.Kind.STAR) {
					while (!pinata.isRemoved()) {
						pinata.swingAt(level, player, full);
					}
				} else if (!burst) {
					while (!pinata.torn()) {
						pinata.swingAt(level, player, full);
					}
				}
			}
		}
	}

	/**
	 * An oak pergola: two posts and a beam of planks, the three piñatas hanging from it filled with candy; pumpkins and
	 * hay bales round about.
	 */
	private static void build(ServerLevel level, BlockPos origin) {
		for (int dy = 0; dy < 4; dy++) {
			level.setBlock(origin.offset(-4, dy, -5), Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(origin.offset(4, dy, -5), Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_ALL);
		}
		for (int dx = -4; dx <= 4; dx++) {
			level.setBlock(origin.offset(dx, 4, -5), Blocks.OAK_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
		}
		for (int i = 0; i < KINDS.length; i++) {
			Pinata pinata = Pinata.hang(level, beam(origin, i), KINDS[i], null);
			if (pinata != null) {
				pinata.fill(new ItemStack(JugcraftAgriculture.item("candy_corn"), 8));
				pinata.fill(new ItemStack(JugcraftAgriculture.item("lollipop"), 3));
				pinata.fill(new ItemStack(Items.COOKIE, 6));
			}
		}
		level.setBlock(origin.offset(-3, 0, -3), Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(origin.offset(3, 0, -3), Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(origin.offset(-3, 1, -3), Blocks.CARVED_PUMPKIN.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(origin.offset(3, 0, -2), Blocks.PUMPKIN.defaultBlockState(), Block.UPDATE_ALL);
	}
}
