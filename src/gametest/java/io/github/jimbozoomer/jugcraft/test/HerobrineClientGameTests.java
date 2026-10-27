package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.HerobrineRenderer;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.Herobrine;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineDirector;
import io.github.jimbozoomer.jugcraft.creatures.herobrine.HerobrineEncounters;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Isolated rendered-client evidence: daylight/darkness, actual entity sync, a witnessed encounter and approach removal. */
public final class HerobrineClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder()
				.adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			var server = world.getServer();
			world.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(c -> new BlockPos(c.player.getBlockX(), 220, c.player.getBlockZ()));
			int x = origin.getX(), y = origin.getY(), z = origin.getZ();
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("weather clear");
			server.runCommand("time set noon");
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 5, y - 1, z - 24, x + 5, y - 1, z + 3));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 5, y, z - 24, x + 5, y + 6, z + 3));
			server.runCommand("tp @p %s %d %s 180 0".formatted(x + 0.5, y, z + 0.5));
			context.runOnClient(c -> {
				c.options.setCameraType(CameraType.FIRST_PERSON);
				c.options.fov().set(60);
				if (!c.gui.hud.isHidden()) c.gui.hud.toggle();
			});
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			server.runCommand("tick freeze");
			int daylight = spawn(context, world, origin.offset(0, 0, -16), Herobrine.Variant.WATCHER);
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_herobrine_daylight");

			// Exercise an actual server-to-client encounter, then approach it before the stare timer expires.
			server.runCommand("tick unfreeze");
			context.waitTicks(Herobrine.SEEN_TICKS + 3);
			server.runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().getFirst();
				var entity = s.overworld().getEntity(daylight);
				check(entity instanceof Herobrine h && h.witnesses().contains(p.getUUID()), "A sustained client view should be witnessed");
				p.snapTo(x + 0.5, y, z - 11.5, 180, 0);
			});
			context.waitFor(c -> c.level.getEntity(daylight) == null);
			server.runOnServer(s -> check(s.overworld().getEntity(daylight) == null, "Approach removal must originate on the server"));

			// An enclosed dark tunnel makes the full-bright eye layer visible independently of daylight.
			server.runCommand("tp @p %s %d %s 180 0".formatted(x + 0.5, y, z + 0.5));
			server.runCommand("time set midnight");
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 2, y, z - 21, x - 2, y + 3, z + 2));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x + 2, y, z - 21, x + 2, y + 3, z + 2));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 2, y + 3, z - 21, x + 2, y + 3, z + 2));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 2, y, z - 21, x + 2, y + 3, z - 21));
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			server.runCommand("tick freeze");
			int darkness = spawn(context, world, origin.offset(0, 0, -16), Herobrine.Variant.TUNNEL);
			context.waitTicks(20);
			context.takeScreenshot("jugcraft_herobrine_tunnel_eyes");
			server.runOnServer(s -> {
				var entity = s.overworld().getEntity(darkness);
				if (entity != null) entity.discard();
			});
			server.runCommand("tick unfreeze");
		}
	}

	private static int spawn(ClientGameTestContext context, TestSingleplayerContext world, BlockPos at, Herobrine.Variant variant) {
		AtomicInteger id = new AtomicInteger();
		world.getServer().runOnServer(s -> {
			var player = s.getPlayerList().getPlayers().getFirst();
			var attempt = HerobrineEncounters.attemptAt(player,
					new HerobrineDirector.Request(player.getUUID(), Herobrine.Kind.DISTANT, HerobrineDirector.FORCED),
					Vec3.atBottomCenterOf(at), variant);
			check(attempt.placed(), "Test encounter placement failed: " + attempt.failure());
			id.set(attempt.herobrine().getId());
		});
		context.waitFor(c -> c.level != null && c.level.getEntity(id.get()) instanceof Herobrine);
		context.runOnClient(c -> {
			var entity = (Herobrine) c.level.getEntity(id.get());
			check(c.getEntityRenderDispatcher().getRenderer(entity) instanceof HerobrineRenderer, "Client must use the Herobrine renderer");
			check(c.getResourceManager().getResource(Jugcraft.id("textures/entity/herobrine_eyes.png")).isPresent(), "Eye layer resource must exist");
		});
		return id.get();
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
