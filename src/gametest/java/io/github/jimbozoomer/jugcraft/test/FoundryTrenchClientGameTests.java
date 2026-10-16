package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.building.Trenchworks;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.DischargeBreakerBlock;
import io.github.jimbozoomer.jugcraft.lair.FoundryTrench;
import io.github.jimbozoomer.jugcraft.lair.FoundryTrench.Pylon;
import io.github.jimbozoomer.jugcraft.lair.GroundTerminalBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairRules;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MistGateEntity;
import io.github.jimbozoomer.jugcraft.lair.OrdersRite;
import io.github.jimbozoomer.jugcraft.lair.PylonCrownBlock;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Foundry Trench and Sealed Orders (docs/features/foundry-trench.md), in a real world, which has
 * the lair dimensions: first the orders from end to end with the one player in survival ({@link #endToEnd}); then
 * pictures: the open line crackling over a telephone in a trench where the orders were used, and, in the trench, the
 * dugout from the arrival and its map table, the fire trench, the pit from over the south rim and from the foot of the
 * ramp, the west pylon with its breaker and terminal, the south pylon live, the east breaker thrown and fouled, the
 * assembly gantry, the pit from the west service gantry and the Grey Mist in the dugout's back doorway; last, the game's
 * own block light on every open cell of the pit's floor. CI job {@code client}.
 */
public class FoundryTrenchClientGameTests implements FabricClientGameTest {
	private static final Lair TRENCH = Lair.FOUNDRY_TRENCH;
	/** Where tools/foundry_trench.py puts things (template positions), beside what FoundryTrench.java gives. */
	private static final BlockPos UNDER_ARRIVAL = new BlockPos(22, 9, 65);
	private static final BlockPos[] EXIT_MIST = {new BlockPos(21, 9, 70), new BlockPos(22, 10, 70), new BlockPos(23, 11, 70)};
	private static final int CATWALK_Y = 9;
	/**
	 * The least block light on any open cell of the pit's floor, and how many open cells it has (tools/foundry_trench.py
	 * PIT_LIGHT and its open cells, checked by the audit).
	 */
	private static final int PIT_LIGHT = 7;
	private static final int PIT_CELLS = 1804;

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
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("difficulty peaceful");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			// Level ground cleared, a telephone in a trench out under the sky, and another in a trench under a roof.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 8, x + 10, y + 8, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 10, y - 1, z - 8, x + 10, y - 1, z + 8));
			context.waitTicks(10);
			BlockPos open = new BlockPos(x + 4, y, z - 3);
			BlockPos roofed = new BlockPos(x - 4, y, z - 3);
			server.runOnServer(minecraft -> {
				trench(minecraft.overworld(), open);
				trench(minecraft.overworld(), roofed);
				for (int dx = -1; dx <= 1; dx++) {
					for (int dz = -1; dz <= 1; dz++) {
						minecraft.overworld().setBlock(roofed.offset(dx, 3, dz), Blocks.STONE.defaultBlockState(), 3);
					}
				}
			});
			context.waitTicks(20);
			endToEnd(context, server, open, roofed);

			// The pictures, in creative, the line held open long enough to take them all.
			server.runCommand("gamemode creative @a");
			String gateSeconds = JugcraftConfig.textOption("lairs.gate_seconds");
			try {
				JugcraftConfig.setTextOption("lairs.gate_seconds", "600");
				AtomicReference<LairInstance> instance = new AtomicReference<>();
				server.runOnServer(minecraft -> {
					ServerPlayer player = player(minecraft);
					ServerLevel overworld = minecraft.overworld();
					player.teleportTo(overworld, open.getX() + 0.5, open.getY(), open.getZ() + 2.5, Set.of(), 180.0F, 0.0F, true);
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.SEALED_ORDERS));
					OrdersRite.Call call = OrdersRite.call(overworld, player, open, player.getMainHandItem());
					check(call.missing() == null, "The orders opened no trench for the pictures: " + call);
					instance.set(call.instance());
				});
				context.waitTicks(40);
				// The open line over the telephone (the camera leaves the trench by command, which ends the visit; the line stays).
				shootAt(context, singleplayer, "minecraft:overworld", open.getX() - 1.5, y, open.getZ() + 2.5, open.getX() + 0.5, y + 0.6,
						open.getZ() + 0.5, "jugcraft_sealed_orders_line");
				server.runOnServer(minecraft -> check(Lairs.enter(player(minecraft), instance.get()), "The camera could not go back in"));
				context.waitTicks(40);
				singleplayer.getConnection().waitForChunksRender();
				BlockPos o = TRENCH.origin(0);
				String dim = TRENCH.dimension.identifier().toString();
				int stand = FoundryTrench.STAND;
				double duck = o.getY() + FoundryTrench.DUGOUT_STAND + 0.2;
				shoot(context, singleplayer, dim, o.getX() + 22.5, duck, o.getZ() + 65.5, 180, -12, "jugcraft_foundry_trench_arrival");
				shootAt(context, singleplayer, dim, o.getX() + 19.5, duck, o.getZ() + 67.5, o.getX() + 25.5, o.getY() + FoundryTrench.DUGOUT_STAND + 1.3,
						o.getZ() + 64.5, "jugcraft_foundry_trench_dugout");
				shoot(context, singleplayer, dim, o.getX() + 31.5, o.getY() + FoundryTrench.TRENCH_STAND + 0.2, o.getZ() + 59.5, 90, 4,
						"jugcraft_foundry_trench_fire_trench");
				shootAt(context, singleplayer, dim, o.getX() + 35.5, o.getY() + FoundryTrench.GROUND + 8, o.getZ() + 66.5, o.getX() + 35.5,
						o.getY() + stand, o.getZ() + 30.5, "jugcraft_foundry_trench_pit");
				shootAt(context, singleplayer, dim, o.getX() + 15.5, o.getY() + stand, o.getZ() + 50.5, o.getX() + 35.5, o.getY() + stand + 2.0,
						o.getZ() + 36.5, "jugcraft_foundry_trench_from_ramp");
				shootAt(context, singleplayer, dim, o.getX() + 25.5, o.getY() + stand, o.getZ() + 39.5, o.getX() + 17.0, o.getY() + stand + 3.0,
						o.getZ() + 36.5, "jugcraft_foundry_trench_west_pylon");
				// The south pylon live (its crown, its breaker's lamp and its terminal burning); the east breaker thrown and fouled.
				server.runOnServer(minecraft -> {
					ServerLevel lair = minecraft.getLevel(TRENCH.dimension);
					check(PylonCrownBlock.setLive(lair, Pylon.SOUTH.crown(o), true)
							&& DischargeBreakerBlock.set(lair, Pylon.SOUTH.breaker(o), DischargeBreakerBlock.LIVE, true)
							&& GroundTerminalBlock.setLive(lair, Pylon.SOUTH.terminal(o), true), "The south pylon did not go live");
					check(DischargeBreakerBlock.set(lair, Pylon.EAST.breaker(o), DischargeBreakerBlock.THROWN, true)
							&& DischargeBreakerBlock.set(lair, Pylon.EAST.breaker(o), DischargeBreakerBlock.FOULED, true),
							"The east breaker was not thrown and fouled");
				});
				shootAt(context, singleplayer, dim, o.getX() + 40.5, o.getY() + stand, o.getZ() + 43.5, o.getX() + 35.5, o.getY() + stand + 4.0,
						o.getZ() + 52.0, "jugcraft_foundry_trench_south_live");
				shootAt(context, singleplayer, dim, o.getX() + 50.5, o.getY() + stand, o.getZ() + 38.5, o.getX() + 53.5, o.getY() + stand + 0.6,
						o.getZ() + 36.5, "jugcraft_foundry_trench_breaker_fouled");
				shootAt(context, singleplayer, dim, o.getX() + 35.5, o.getY() + stand, o.getZ() + 30.5, o.getX() + 35.5, o.getY() + stand + 4.0,
						o.getZ() + 9.5, "jugcraft_foundry_trench_gantry");
				shootAt(context, singleplayer, dim, o.getX() + 12.5, o.getY() + CATWALK_Y + 1, o.getZ() + 26.5, o.getX() + 40.5, o.getY() + stand,
						o.getZ() + 40.5, "jugcraft_foundry_trench_catwalk");
				shoot(context, singleplayer, dim, o.getX() + 22.5, duck, o.getZ() + 66.5, 0, 0, "jugcraft_foundry_trench_exit");

				// The game's own light on every open cell of the pit's floor, where the Dreadnought and whoever fights him are seen.
				server.runOnServer(minecraft -> {
					ServerLevel lair = minecraft.getLevel(TRENCH.dimension);
					int least = 15;
					int cells = 0;
					BlockPos darkest = null;
					for (int dx = FoundryTrench.PIT_WEST; dx <= FoundryTrench.PIT_EAST; dx++) {
						for (int dz = FoundryTrench.PIT_NORTH; dz <= FoundryTrench.PIT_SOUTH; dz++) {
							BlockPos cell = o.offset(dx, stand, dz);
							if (!standable(lair, cell) || !standable(lair, cell.above())) {
								continue;
							}
							int light = lair.getBrightness(LightLayer.BLOCK, cell);
							cells++;
							if (light < least) {
								least = light;
								darkest = cell.subtract(o);
							}
						}
					}
					Jugcraft.LOGGER.info("[lairs] the Foundry Trench's pit: {} open cells, the darkest at block light {} ({})", cells, least, darkest);
					check(cells == PIT_CELLS && least >= PIT_LIGHT, "The trench's pit has " + cells + " open cells (not " + PIT_CELLS
							+ "), the darkest at block light " + least + " (" + darkest + "), under " + PIT_LIGHT);
				});
			} finally {
				JugcraftConfig.setTextOption("lairs.gate_seconds", gateSeconds);
				server.runOnServer(minecraft -> Lairs.reset());
			}
		}
	}

	/**
	 * Sealed Orders from end to end, step by step, with the player in survival (the instances are the whole server's, so
	 * this one test owns them):
	 * <ol>
	 * <li>used on the telephone in the roofed trench, the orders are refused and kept;</li>
	 * <li>used by the player's hand on the telephone in the open trench, they open an instance in the trench's own
	 * dimension, place the trench fresh (the dugout's duckboards under the arrival, the Grey Mist in its back doorway, each
	 * pylon's breaker open and dark facing the pit, its crown and terminal dark, the dugout's telephone), are used up and
	 * rested, open a line over the telephone, and take the player over the top, unable to build;</li>
	 * <li>in the trench a breaker is a fixture and a crown is not; a breaker used with no Dreadnought to route stays open
	 * and dark; falling off the trench throws them back to the dugout for the mist's toll;</li>
	 * <li>leaving takes the player back to where they stood; while the line is open a second set of orders used there is
	 * refused and kept, and using the telephone with an empty hand takes them back into the same trench;</li>
	 * <li>when the instance closes the player goes home and the line is gone.</li>
	 * </ol>
	 */
	private static void endToEnd(ClientGameTestContext context, TestServerContext server, BlockPos phone, BlockPos roofed) {
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		AtomicReference<LairInstance> first = new AtomicReference<>();
		AtomicReference<Vec3> stood = new AtomicReference<>();

		// 1. Refused under a roof.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.teleportTo(overworld, roofed.getX() + 0.5, roofed.getY(), roofed.getZ() + 2.5, Set.of(), 180.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.SEALED_ORDERS, 2));
			OrdersRite.Call call = OrdersRite.call(overworld, player, roofed, player.getMainHandItem());
			check(call.missing() == OrdersRite.Missing.ROOFED && player.getMainHandItem().getCount() == 2,
					"Under a roof the orders were not refused, or were used up: " + call + ", " + player.getMainHandItem());
			player.teleportTo(overworld, phone.getX() + 0.5, phone.getY(), phone.getZ() + 2.5, Set.of(), 180.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.SEALED_ORDERS));
		});
		context.waitTicks(10);

		// 2. In the open trench, the orders used by hand open the trench and the player goes over the top.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			stood.set(player.position());
			OrdersRite.Missing ready = OrdersRite.check(overworld, phone);
			check(ready == null, "The orders could not open the trench here: " + ready);
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(phone), Direction.SOUTH, phone, false);
			InteractionResult used = player.gameMode.useItemOn(player, overworld, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
			check(used.consumesAction(), "Using the orders did not succeed: " + used);
		});
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			LairInstance instance = Lairs.open(TRENCH).isEmpty() ? null : Lairs.open(TRENCH).getFirst();
			check(instance != null, "The orders opened no instance");
			first.set(instance);
			check(player.getMainHandItem().isEmpty(), "The orders were not used up: " + player.getMainHandItem());
			check(player.getCooldowns().isOnCooldown(new ItemStack(JugcraftLairs.SEALED_ORDERS)), "The orders were not rested after their use");
			ServerLevel lair = minecraft.getLevel(TRENCH.dimension);
			check(lair != null && lair.getMinY() == 0, "The trench's dimension: from y 0");
			BlockPos o = instance.origin();
			check(lair.getBlockState(o.offset(UNDER_ARRIVAL)).is(Trenchworks.BLOCKS.get("duckboard")), "No duckboards under the arrival");
			for (BlockPos mist : EXIT_MIST) {
				check(lair.getBlockState(o.offset(mist)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist in the dugout's back doorway at " + mist);
			}
			for (Pylon pylon : Pylon.values()) {
				BlockState breaker = lair.getBlockState(pylon.breaker(o));
				check(breaker.is(JugcraftLairs.DISCHARGE_BREAKER) && !breaker.getValue(DischargeBreakerBlock.THROWN)
						&& !breaker.getValue(DischargeBreakerBlock.LIVE) && !breaker.getValue(DischargeBreakerBlock.FOULED),
						"No open, dark breaker at the " + pylon.title() + " pylon's foot");
				check(lair.getBlockState(pylon.crown(o)).is(JugcraftLairs.PYLON_CROWN) && !lair.getBlockState(pylon.crown(o)).getValue(PylonCrownBlock.LIVE),
						"No dark crown on the " + pylon.title() + " pylon");
				check(lair.getBlockState(pylon.terminal(o)).is(JugcraftLairs.GROUND_TERMINAL)
						&& !lair.getBlockState(pylon.terminal(o)).getValue(GroundTerminalBlock.LIVE), "No dark terminal before the " + pylon.title() + " pylon");
			}
			check(lair.getBlockState(o.offset(FoundryTrench.TELEPHONE)).is(Trenchworks.BLOCKS.get("field_telephone")), "No telephone in the dugout");
			check(player.level() == lair && player.position().distanceTo(instance.arrival()) < 0.5,
					"The player did not come to the dugout: " + player.level().dimension() + " " + player.position());
			check(!player.getAbilities().mayBuild, "In a lair a player cannot build");
			MistGateEntity line = OrdersRite.line(overworld, phone);
			check(line != null && line.instance() == instance && line.lair() == TRENCH, "No line open over the telephone the orders were used on");
		});
		context.waitTicks(40);

		// 3. A breaker is a fixture, a crown is not; a breaker with no Dreadnought stays open and dark; the trench's edge.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(TRENCH.dimension);
			BlockPos o = first.get().origin();
			BlockPos breaker = Pylon.WEST.breaker(o);
			BlockHitResult atBreaker = new BlockHitResult(Vec3.atCenterOf(breaker), Direction.EAST, breaker, false);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, atBreaker) == InteractionResult.PASS, "A breaker cannot be used");
			BlockPos crown = Pylon.WEST.crown(o);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(crown), Direction.EAST, crown, false))
					== InteractionResult.FAIL, "A crown can be used");
			player.teleportTo(breaker.getX() + 2.5, breaker.getY(), breaker.getZ() + 0.5);
			InteractionResult used = player.gameMode.useItemOn(player, lair, ItemStack.EMPTY, InteractionHand.MAIN_HAND, atBreaker);
			check(used.consumesAction() && !lair.getBlockState(breaker).getValue(DischargeBreakerBlock.THROWN)
					&& !lair.getBlockState(breaker).getValue(DischargeBreakerBlock.LIVE),
					"With no Dreadnought to route, the breaker was thrown or lit, or the use was not taken: " + used);
			Vec3 arrival = first.get().arrival();
			player.teleportTo(o.getX() + 35.5, o.getY() + TRENCH.floor - 2.0, o.getZ() + 36.5);
			player.setHealth(20.0F);
			check(Lairs.edges(player, first.get()) && player.position().distanceTo(arrival) < 0.5
					&& player.getHealth() == 20.0F - Lairs.EDGE_DAMAGE, "Falling out of the trench did not end in the dugout");
			player.setHealth(20.0F);
		});

		// 4. Out, second orders refused at the open line, and back in down the line.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			check(Lairs.leave(player) && player.level() == minecraft.overworld() && player.position().distanceTo(stood.get()) < 1.0E-6,
					"The player did not come back to where they stood: " + player.position());
		});
		context.waitTicks(40);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.SEALED_ORDERS));
			check(OrdersRite.call(overworld, player, phone, player.getMainHandItem()).missing() == OrdersRite.Missing.RINGING
					&& player.getMainHandItem().getCount() == 1, "Second orders at the open line were not refused, or were used up");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			check(OrdersRite.line(overworld, phone) != null, "The line went dead early");
			InteractionResult followed = player.gameMode.useItemOn(player, overworld, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(phone), Direction.SOUTH, phone, false));
			check(followed.consumesAction() && player.level() == minecraft.getLevel(TRENCH.dimension)
					&& Lairs.instance(player.getAttached(Lairs.VISIT)) == first.get(), "The telephone did not take the player back into the same trench");
		});
		context.waitTicks(40);

		// 5. Closed, the player goes home and the line is gone.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			Lairs.close(minecraft, first.get());
			check(player.level() == minecraft.overworld(), "Closing the trench did not send the player home");
		});
		context.waitTicks(50);
		server.runOnServer(minecraft -> {
			ServerLevel overworld = minecraft.overworld();
			check(OrdersRite.line(overworld, phone) == null
					&& overworld.getEntitiesOfClass(MistGateEntity.class, new AABB(phone).inflate(3)).isEmpty(), "The line outlived its trench");
			player(minecraft).setHealth(20.0F);
			Lairs.reset();
		});
		context.waitTicks(20);
		Jugcraft.LOGGER.info("[lairs] client game test: Sealed Orders passed end to end");
	}

	/** A field telephone at {@code phone} facing south, sandbags either side of it and behind it, duckboards before it. */
	private static void trench(ServerLevel level, BlockPos phone) {
		level.setBlock(phone, Trenchworks.BLOCKS.get("field_telephone").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
				Direction.SOUTH), 3);
		BlockState sandbags = Trenchworks.BLOCKS.get("sandbags").defaultBlockState();
		for (int dx = -1; dx <= 1; dx++) {
			level.setBlock(phone.offset(dx, 0, -1), sandbags, 3);
			level.setBlock(phone.offset(dx, -1, 1), Trenchworks.BLOCKS.get("duckboard").defaultBlockState(), 3);
		}
		level.setBlock(phone.west(), sandbags, 3);
		level.setBlock(phone.east(), sandbags, 3);
	}

	/** Whether a player could stand in {@code cell}: nothing in it, or nothing to collide with. */
	private static boolean standable(ServerLevel level, BlockPos cell) {
		BlockState state = level.getBlockState(cell);
		return state.isAir() || state.is(Blocks.LIGHT) || state.getCollisionShape(level, cell).isEmpty();
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().get(0);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	/** Stands the camera at (x, y, z) in {@code dimension} (its feet), looking at (tx, ty, tz), and shoots. */
	private static void shootAt(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, double x, double y,
			double z, double tx, double ty, double tz, String name) {
		double dx = tx - x;
		double dy = ty - (y + 1.62);
		double dz = tz - z;
		int yaw = (int) Math.round(Math.toDegrees(Math.atan2(-dx, dz)));
		int pitch = (int) Math.round(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
		shoot(context, singleplayer, dimension, x, y, z, yaw, pitch, name);
	}

	/** Stands the camera at (x, y, z) in {@code dimension}, looking along yaw and pitch, waits for the world to draw, and shoots. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, double x, double y,
			double z, int yaw, int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runOnServer(minecraft -> {
			// Flying, so the camera stays where it is put (set on the server, which tells the client).
			ServerPlayer player = player(minecraft);
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		});
		server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.2f %.2f %.2f %d %d", dimension, x, y, z, yaw, pitch));
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(10);
		context.takeScreenshot(name);
	}
}
