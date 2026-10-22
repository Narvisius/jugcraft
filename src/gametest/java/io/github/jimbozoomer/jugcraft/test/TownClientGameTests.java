package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.town.AtmMenu;
import io.github.jimbozoomer.jugcraft.town.ShopMenu;
import io.github.jimbozoomer.jugcraft.town.Town;
import io.github.jimbozoomer.jugcraft.town.TownData;
import io.github.jimbozoomer.jugcraft.town.TownDecor;
import io.github.jimbozoomer.jugcraft.town.TownPlanner;
import io.github.jimbozoomer.jugcraft.town.TownState;
import io.github.jimbozoomer.jugcraft.town.Townsfolk;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The walled town in a real new world (seed {@link #SEED}): the server must have placed it near the start; the camera
 * flies over it while it builds, then looks round the square and a gate, in two themes, and opens a shop and an ATM.
 * The distances, the chunks built and the townsfolk out are logged.
 */
public class TownClientGameTests implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("jugcraft-town-client-tests");
	private static final String SEED = "jugcraft";

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().adjustSettings(creator -> {
			creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(creator.getSettings().worldgenLoadContext()
					.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL)));
			creator.setSeed(SEED);
		}).create()) {
			// The default render distance (12 chunks) loads, and so builds, the whole town (192 blocks) round the overview
			// spot; a larger one was too slow to render here with software rendering (run 37074095387).
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			BlockPos origin = server.computeOnServer(minecraft -> TownState.get(minecraft).origin());
			if (origin == null) {
				throw new AssertionError("The new world has no town");
			}
			BlockPos start = server.computeOnServer(minecraft -> minecraft.overworld().getRespawnData().pos());
			BlockPos centre = Town.centre(origin);
			double distance = Math.sqrt(centre.distSqr(start.atY(centre.getY())));
			LOGGER.info("Town, seed {}: middle at {} {} {}, {} blocks from the start at {} {} {}", SEED, centre.getX(), centre.getY(),
					centre.getZ(), (int) distance, start.getX(), start.getY(), start.getZ());
			if (distance < TownPlanner.MIN_DISTANCE || distance > TownPlanner.MAX_DISTANCE) {
				throw new AssertionError("The town's middle is " + (int) distance + " blocks from the start, not "
						+ TownPlanner.MIN_DISTANCE + " to " + TownPlanner.MAX_DISTANCE);
			}
			// Over the middle of the town, so that every chunk of it is in reach, while they load and build (run
			// 37076304887, waiting over the south edge, built only 96 of 144); then over the town from the south.
			fly(context, server, origin, 96, 60, 96, 180, 90);
			int size = TownData.get().size;
			if (TownData.get().unknownStates() != 0) {
				throw new AssertionError("The town contains an unknown block state");
			}
			int chunks = (size / 16) * (size / 16);
			int built = 0;
			for (int wait = 0; wait < 90; wait++) {
				context.waitTicks(20);
				built = server.computeOnServer(minecraft -> builtChunks(minecraft.overworld(), origin));
				if (built >= chunks) {
					break;
				}
			}
			LOGGER.info("Town, seed {}: {} of {} chunks built", SEED, built, chunks);
			if (built < chunks) {
				throw new AssertionError("Only " + built + " of " + chunks + " town chunks built");
			}
			// Up to 90 blocks over the town's floor but under the clouds (about y 192; run 37078631445's town in Alpine Spawn,
			// floor at y 106, was shot from above them), looking down at the square.
			int up = Math.max(20, Math.min(90, 180 - origin.getY()));
			int down = (int) Math.round(Math.toDegrees(Math.atan2(up + 1.6, 176 - 104)));
			fly(context, server, origin, 96, up, 176, 180, down);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_town_overview");
			// The square, looking north to the church, in the theme of the day; then at Halloween and in December.
			fly(context, server, origin, 96, 3, 113, 180, -8);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_town_square");
			for (String theme : List.of("halloween", "december")) {
				server.runOnServer(minecraft -> {
					TownDecor.force(theme);
					redecorate(minecraft.overworld());
				});
				context.waitTicks(20);
				singleplayer.getConnection().waitForChunksRender();
				context.takeScreenshot("jugcraft_town_square_" + theme);
			}
			server.runOnServer(minecraft -> {
				TownDecor.force(null);
				redecorate(minecraft.overworld());
			});
			// The south gate from outside, standing on the land there (outside the wall it keeps its own height) and looking
			// at the middle of the gatehouse.
			int gateZ = 184;
			int ground = server.computeOnServer(minecraft -> minecraft.overworld().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					origin.getX() + 96, origin.getZ() + gateZ));
			int stand = ground - origin.getY();
			int pitch = (int) Math.round(Math.toDegrees(Math.atan2(stand + 1.6 - 7, gateZ - 168)));
			LOGGER.info("Town, seed {}: gate shot from 96 {} {} (ground {}), pitch {}", SEED, stand, gateZ, ground, pitch);
			fly(context, server, origin, 96, stand, gateZ, 180, pitch);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_town_gate");
			// The townsfolk, a shop and an ATM.
			String people = server.computeOnServer(minecraft -> townsfolk(minecraft.overworld(), origin));
			LOGGER.info("Town, seed {}: townsfolk out: {}", SEED, people);
			fly(context, server, origin, 96, 2, 104, 270, 10);
			boolean shop = server.computeOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
				List<Townsfolk> keepers = level.getEntitiesOfClass(Townsfolk.class, townBox(origin, size),
						t -> t.shop().equals("seasonal"));
				if (keepers.isEmpty()) {
					return false;
				}
				player.teleportTo(keepers.get(0).getX() + 2, keepers.get(0).getY(), keepers.get(0).getZ());
				ShopMenu.open(player, keepers.get(0));
				return true;
			});
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_town_shop");
			context.runOnClient(client -> client.player.closeContainer());
			boolean atm = server.computeOnServer(minecraft -> {
				ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
				BlockPos at = origin.offset(TownData.get().atms.get(0));
				player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() - 1.5);
				AtmMenu.open(player, at);
				return minecraft.overworld().getBlockState(at).getBlock() instanceof io.github.jimbozoomer.jugcraft.town.AtmBlock;
			});
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_town_atm");
			context.runOnClient(client -> client.player.closeContainer());
			LOGGER.info("Town, seed {}: shop screen opened {}, ATM in place {}", SEED, shop, atm);
			if (!shop || !atm) {
				throw new AssertionError("The town's seasonal stall keeper or its ATM is missing (shop " + shop + ", ATM " + atm + ")");
			}
			// Architectural review in a disposable world, not the owner's manual save.
			server.runCommand("time set noon");
			server.runCommand("gamerule minecraft:advance_time false");
			setHudHidden(context, true);
			int[][] views = {{96, 8, 119, 180, -18}, {96, 2, 77, 180, -8},
					{116, 23, 62, 90, 15}, {96, 19, 30, 0, 10},
					{98, 13, 107, 270, 5}, {90, 12, 107, 90, 12}, {152, 5, 96, 90, -7}};
			String[] names = {"church_front", "church_nave", "church_side", "church_rear",
					"town_hall", "bank", "houses"};
			for (int i = 0; i < views.length; i++) {
				int[] view = views[i];
				fly(context, server, origin, view[0], view[1], view[2], view[3], view[4]);
				singleplayer.getConnection().waitForChunksRender();
				context.takeScreenshot("jugcraft_refined_" + names[i]);
			}
			server.runOnServer(minecraft -> {
				var level = minecraft.overworld();
				for (var entry : TownData.get().signs) {
					var be = level.getBlockEntity(origin.offset(entry.pos()));
					if (!(be instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign)
							|| !sign.isWaxed() || !sign.getText(net.minecraft.world.level.block.entity.SignTextSlot.FRONT)
							.getMessages(false).getFirst().getString().equals(entry.lines().get(0))) {
						throw new AssertionError("Town sign missing or changed after seasonal updates: " + entry.pos());
					}
				}
				var frames = level.getEntitiesOfClass(net.minecraft.world.entity.decoration.ItemFrame.class,
						new AABB(origin.offset(88, 2, 112)).inflate(1));
				if (!frames.isEmpty()) throw new AssertionError("Retired town map must stay absent");
				var player = minecraft.getPlayerList().getPlayers().getFirst();
				// The owner retired town maps; signs remain.
				LOGGER.info("Wayfinding: {} waxed signs, retired map absent", TownData.get().signs.size());
			});
			context.runOnClient(client -> {
				for (var entry : TownData.get().signs) for (String line : entry.lines()) {
					if (client.font.width(line) > 90) throw new AssertionError("Sign text would clip: " + line);
				}
			});
			fly(context, server, origin, 88, 1, 109, 0, 0);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_town_directory");
			fly(context, server, origin, 80, 1, 101, 90, 0);
			singleplayer.getConnection().waitForChunksRender();
			context.takeScreenshot("jugcraft_town_keeper_skin");

            var flakSpot=TownData.get().spots.stream().filter(s -> s.role().equals("flak_guard")).findFirst().orElseThrow().pos();
            fly(context, server, origin, flakSpot.getX()+6, flakSpot.getY()+3, flakSpot.getZ()+8, 143, 15);
            singleplayer.getConnection().waitForChunksRender();
            context.takeScreenshot("jugcraft_city_flak_crew");
            server.runOnServer(minecraft -> {
                var level=minecraft.overworld();
                var crews=level.getEntitiesOfClass(Townsfolk.class, townBox(origin,TownData.get().size), p->p.role().equals("flak_guard"));
                if(crews.isEmpty() || crews.stream().anyMatch(p->!(p.getVehicle() instanceof io.github.jimbozoomer.jugcraft.artillery.FlakGun)))
                    throw new AssertionError("Every loaded tower crew must actually be seated on its gun");
                var walkers=level.getEntitiesOfClass(io.github.jimbozoomer.jugcraft.town.TownGuardMecha.class,townBox(origin,TownData.get().size));
                if(walkers.isEmpty())throw new AssertionError("City guard walkers missing");
                var walker=walkers.getFirst();walker.setNoAi(true);walker.snapTo(origin.getX()+78.5,origin.getY()+1,origin.getZ()+94.5,0,0);
            });
            fly(context,server,origin,84,3,102,143,8);
            singleplayer.getConnection().waitForChunksRender();context.takeScreenshot("jugcraft_city_guard_walker");
			// Leave the exported copy at the square, ready for owner-controlled play.
			setHudHidden(context, false);
			fly(context, server, origin, 96, 1, 89, 180, -12);
			server.runCommand("save-all flush");
		}
	}

	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	/** Flies the disposable test camera without leaving invisible barriers in the saved town. */
	private static void fly(ClientGameTestContext context, TestServerContext server, BlockPos origin, int x, int up, int z, int yaw, int pitch) {
		BlockPos at = origin.offset(x, up, z);
		server.runOnServer(minecraft -> {
			ServerPlayer player = minecraft.getPlayerList().getPlayers().get(0);
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		});
		server.runCommand(String.format(Locale.ROOT, "tp @p %d.5 %d %d.5 %d %d", at.getX(), at.getY(), at.getZ(), yaw, pitch));
		context.waitTicks(40);
	}

	private static int builtChunks(ServerLevel level, BlockPos origin) {
		TownState state = TownState.get(level);
		int size = TownData.get().size;
		int built = 0;
		for (int cx = origin.getX() >> 4; cx <= (origin.getX() + size - 1) >> 4; cx++) {
			for (int cz = origin.getZ() >> 4; cz <= (origin.getZ() + size - 1) >> 4; cz++) {
				if (state.built(new ChunkPos(cx, cz).pack())) {
					built++;
				}
			}
		}
		return built;
	}

	/** Changes every site in loaded chunks to the current theme at once (as the decorators would, given time). */
	private static void redecorate(ServerLevel level) {
		TownState state = TownState.get(level);
		state.setTheme(TownDecor.theme(), level.getGameTime());
		for (TownData.Site site : TownData.get().sites) {
			BlockPos at = state.origin().offset(site.anchor());
			if (level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4) != null) {
				TownDecor.apply(level, site.index());
			}
		}
	}

	/** The town's whole volume, from its corner to {@code size} across and 60 up. */
	private static AABB townBox(BlockPos origin, int size) {
		return new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + size, origin.getY() + 60, origin.getZ() + size);
	}

	private static String townsfolk(ServerLevel level, BlockPos origin) {
		int size = TownData.get().size;
		List<Townsfolk> people = level.getEntitiesOfClass(Townsfolk.class, townBox(origin, size));
		return people.size() + " " + people.stream().map(p -> p.role() + (p.shop().isEmpty() ? "" : "/" + p.shop())).sorted().toList();
	}
}
