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
			server.runCommand("forceload add 0 0 175 160");
			// Only this disposable test world: explicitly clear the preview volume above the landscape.
			server.runOnServer(s -> {
				for (int x = 0; x < 176; x++) {
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
					&& !c.level.getBlockState(new BlockPos(68, 248, 33)).isAir()
					&& c.level.getBlockState(new BlockPos(112, 248, 34)).is(Blocks.POLISHED_DIORITE)
					&& c.level.getBlockState(new BlockPos(150, 248, 34)).is(Blocks.DARK_OAK_PLANKS)
					&& c.level.getBlockState(new BlockPos(90, 260, 34)).is(Blocks.STONE_BRICKS)
					&& c.level.getBlockState(new BlockPos(51, 249, 99)).is(Blocks.CARVED_PUMPKIN)
					&& c.level.getBlockState(new BlockPos(68, 311, 33)).is(Blocks.LANTERN)
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
			server.runCommand("time set midnight");
			view(context, server, 68, 251, 151, 180, -5);
			context.takeScreenshot("jugcraft_academy_bridge_night");
			view(context, server, 68, 254, 108, 180, 5);
			context.takeScreenshot("jugcraft_academy_court_night");
			server.runCommand("time set noon");
			view(context, server, 68, 251, 136, 180, -5);
			context.takeScreenshot("jugcraft_academy_gatehouse");
			view(context, server, 68, 254, 108, 180, 5);
			context.takeScreenshot("jugcraft_academy_cloister");
			view(context, server, 95, 260, 61, 108, 0);
			context.takeScreenshot("jugcraft_academy_hall_court_portal");
			view(context, server, 68, 265, 82, 180, -19);
			context.takeScreenshot("jugcraft_academy_hall_tower_front");
			view(context, server, 68, 267, 102, 180, 25);
			context.takeScreenshot("jugcraft_academy_autumn_gardens");
			view(context, server, 80, 252, 99, 210, 3);
			context.takeScreenshot("jugcraft_academy_autumn_garden_eye_level");
			view(context, server, 68, 252, 45, 180, 0);
			context.takeScreenshot("jugcraft_academy_hall");
			view(context, server, 68, 251, 39, 180, -5);
			context.takeScreenshot("jugcraft_academy_hall_ceiling_dais");
			view(context, server, 68, 252, 36, 180, -53);
			context.takeScreenshot("jugcraft_academy_hall_vault_upward");
			view(context, server, 61, 252, 43, 145, -19);
			context.takeScreenshot("jugcraft_academy_hall_vault_oblique");
			view(context, server, 61, 252, 27, 145, -30);
			context.takeScreenshot("jugcraft_academy_hall_rib_close");
			view(context, server, 68, 252, 32, 180, 1);
			context.takeScreenshot("jugcraft_academy_hall_teaching_apse");
			view(context, server, 72, 252, 34, 270, -5);
			context.takeScreenshot("jugcraft_academy_hall_side_archive");
			view(context, server, 68, 264, 41, 180, 10);
			context.takeScreenshot("jugcraft_academy_upper_gallery");
			view(context, server, 68, 264, 33, 180, -8);
			context.takeScreenshot("jugcraft_academy_hall_north_gallery_detail");
			view(context, server, 76, 264, 40, 135, 4);
			context.takeScreenshot("jugcraft_academy_hall_gallery_oblique");
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
			view(context, server, 112, 252, 39, 180, -25);
			context.takeScreenshot("jugcraft_academy_library_vault");
			view(context, server, 112, 252, 38, 135, -3);
			context.takeScreenshot("jugcraft_academy_library_research_bay");
			view(context, server, 105, 260, 35, 270, 5);
			context.takeScreenshot("jugcraft_academy_library_upper_gallery");
			view(context, server, 157, 252, 41, 180, 3);
			context.takeScreenshot("jugcraft_academy_grand_reading");
			view(context, server, 153, 262, 42, 180, 5);
			context.takeScreenshot("jugcraft_academy_upper_archive");
			view(context, server, 90, 251, 36, 180, 5);
			context.takeScreenshot("jugcraft_academy_scholarly_wing");
			view(context, server, 68, 278, 42, 180, 10);
			context.takeScreenshot("jugcraft_academy_archmage");
			view(context, server, 66, 278, 37, 180, -12);
			context.takeScreenshot("jugcraft_academy_archmage_study_detail");
			view(context, server, 69, 278, 39, 270, -10);
			context.takeScreenshot("jugcraft_academy_archmage_conservatory");
			view(context, server, 43, 277, 39, 180, 5);
			context.takeScreenshot("jugcraft_academy_archmage_annex");
			view(context, server, 46, 278, 40, 145, 2);
			context.takeScreenshot("jugcraft_academy_archmage_bedroom_detail");
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
