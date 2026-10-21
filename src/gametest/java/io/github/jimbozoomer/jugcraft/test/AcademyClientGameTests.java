package io.github.jimbozoomer.jugcraft.test;

import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Real-client views for review; these are not human playtest evidence. */
public final class AcademyClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(c -> c.options.renderDistance().set(10));
		try (TestSingleplayerContext world = context.worldBuilder()
				.adjustSettings(c -> {
					c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
					// An isolated architecture fixture: surrounding terrain generation is tested separately.
					for (var preset : c.getNormalPresetList()) {
						if (preset.preset().is(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT)) c.setWorldType(preset);
					}
					c.setGenerateStructures(false);
				}).create()) {
			TestServerContext server = world.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("forceload add 0 0 144 160");
			// Only this disposable test world: explicitly clear the preview volume above the landscape.
			server.runOnServer(s -> {
				for (int x = 0; x < 145; x++) {
					for (int z = 0; z < 161; z++) {
						for (int y = 224; y < 300; y++) {
							BlockPos pos = new BlockPos(x, y, z);
							if (!s.overworld().getBlockState(pos).isAir()) {
								s.overworld().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
							}
						}
					}
				}
			});
			server.runCommand("place template jugcraft:academy/winterhold 0 224 0");
			server.runOnServer(s -> {
				if (s.overworld().getBlockEntity(new BlockPos(57, 249, 28)) == null) {
					throw new AssertionError("Academy placement did not create the study bench");
				}
			});
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.runOnClient(c -> c.options.fov().set(75));
			view(context, server, 130, 306, 150, 137, 18);
			// Verify received geometry across every wing and the roof, rather than unrelated view-distance corners.
			context.waitFor(c -> c.level != null
					&& c.level.getBlockState(new BlockPos(68, 248, 160)).is(Blocks.POLISHED_ANDESITE)
					&& c.level.getBlockState(new BlockPos(27, 248, 76)).is(Blocks.POLISHED_DIORITE)
					&& c.level.getBlockState(new BlockPos(109, 248, 92)).is(Blocks.POLISHED_DIORITE)
					&& c.level.getBlockState(new BlockPos(68, 248, 33)).is(Blocks.POLISHED_ANDESITE)
					&& c.level.getBlockState(new BlockPos(112, 248, 34)).is(Blocks.POLISHED_DIORITE)
					&& c.level.getBlockState(new BlockPos(68, 296, 33)).is(Blocks.STONE_BRICK_SLAB)
					&& c.level.getBlockState(new BlockPos(68, 251, 85)).is(io.github.jimbozoomer.jugcraft.academy.JugcraftAcademy.WARD));
			try {
				world.getConnection().waitForChunksRender(false);
			} catch (AssertionError failure) {
				context.runOnClient(c -> io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.error(
						"Academy render wait: native sections ready={}, effective view distance={}",
						c.levelRenderer.hasRenderedAllSections(), c.options.getEffectiveRenderDistance()));
				context.takeScreenshot("jugcraft_academy_render_timeout");
				throw failure;
			}
			context.takeScreenshot("jugcraft_academy_overview");
			view(context, server, 68, 251, 151, 180, -5);
			context.takeScreenshot("jugcraft_academy_bridge");
			view(context, server, 68, 254, 108, 180, 5);
			context.takeScreenshot("jugcraft_academy_cloister");
			view(context, server, 68, 252, 45, 180, 0);
			context.takeScreenshot("jugcraft_academy_hall");
			view(context, server, 59, 251, 36, 180, 20);
			context.takeScreenshot("jugcraft_academy_radiance_instructor");
			server.runOnServer(s -> {
				var home = new BlockPos(59, 249, 31);
				var instructors = s.overworld().getEntitiesOfClass(io.github.jimbozoomer.jugcraft.town.Townsfolk.class,
						new net.minecraft.world.phys.AABB(home).inflate(8), t -> t.role().equals("academy_radiance"));
				if (instructors.size() != 1) throw new AssertionError("Expected one persistent Radiance instructor");
				if (s.overworld().isRaining() || s.overworld().isThundering()) throw new AssertionError("Academy altered global clear weather");
			});
			view(context, server, 112, 252, 44, 180, 5);
			context.takeScreenshot("jugcraft_academy_library");
			view(context, server, 68, 278, 42, 180, 10);
			context.takeScreenshot("jugcraft_academy_archmage");
			view(context, server, 43, 244, 38, 180, 10);
			context.takeScreenshot("jugcraft_academy_midden_forge");
			view(context, server, 71, 244, 69, 155, 5);
			context.takeScreenshot("jugcraft_academy_echo_chamber");
			server.runCommand("forceload remove 0 0 144 160");
			context.getInput().pressKey(options -> options.keyToggleGui);
		}
	}

	private static void view(ClientGameTestContext context, TestServerContext server,
			double x, double y, double z, float yaw, float pitch) {
		server.runOnServer(s -> {
			var player = s.getPlayerList().getPlayers().getFirst();
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		});
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.runOnClient(c -> {
			c.player.getAbilities().flying = true;
			c.player.onUpdateAbilities();
		});
		context.waitTicks(20);
	}
}
