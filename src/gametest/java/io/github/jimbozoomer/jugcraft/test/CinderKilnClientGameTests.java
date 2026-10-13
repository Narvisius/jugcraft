package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.KilnSealRite;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairRules;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MistGateEntity;
import io.github.jimbozoomer.jugcraft.lair.SluiceGateBlock;
import io.github.jimbozoomer.jugcraft.lair.TroughStoneBlock;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Cinder Kiln and the Kiln Seal (docs/features/cinder-kiln.md), in a real world, which has the
 * lair dimensions and the Nether: first the Kiln Seal from end to end with the one player in survival ({@link #endToEnd});
 * then pictures: the vent on a magma block on a volcano's slope, and, in the kiln, the view from the arrival ledge, the
 * bowl from the foot of the stair, the crucible and the heat channel under the forge mouth, the slag falling over the
 * forge's lip, a sluice open over its flooded trough, a shelf, the dome and its vent overhead and the Grey Mist's arch;
 * then a seal pressed into the Nether's magma, and the vent there. CI job {@code client}.
 */
public class CinderKilnClientGameTests implements FabricClientGameTest {
	private static final Lair KILN = Lair.CINDER_KILN;
	/** Where tools/cinder_kiln.py puts things (template positions). */
	private static final BlockPos UNDER_ARRIVAL = new BlockPos(41, 12, 63);
	private static final BlockPos[] EXIT_MIST = {new BlockPos(41, 13, 68), new BlockPos(42, 14, 68), new BlockPos(41, 15, 68)};
	private static final BlockPos STAIR_HALF = new BlockPos(44, 12, 60);
	private static final BlockPos BOWL_FLOOR = new BlockPos(35, 5, 37);
	private static final BlockPos CHANNEL = new BlockPos(35, 5, 17);
	private static final BlockPos CRUCIBLE_DEEP = new BlockPos(35, 3, 25);
	private static final BlockPos LIP = new BlockPos(31, 8, 10);
	private static final BlockPos WEST_WHEEL = new BlockPos(7, 7, 35);
	private static final BlockPos WEST_TROUGH = new BlockPos(16, 5, 37);
	private static final BlockPos EAST_TROUGH = new BlockPos(54, 5, 37);
	private static final BlockPos SHELF = new BlockPos(24, 6, 26);
	private static final int BOWL = 5;

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
			// A slope of blackstone and basalt to press the seal into, its land plains for now.
			server.runCommand("fill %d %d %d %d %d %d minecraft:blackstone".formatted(x - 8, y - 3, z - 10, x + 12, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 10, x + 12, y + 8, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:basalt".formatted(x - 2, y - 1, z - 8, x + 6, y - 1, z - 2));
			server.runCommand("fillbiome %d %d %d %d %d %d minecraft:plains".formatted(x - 16, y - 4, z - 16, x + 16, y + 12, z + 12));
			context.waitTicks(10);

			BlockPos spot = new BlockPos(x + 2, y, z - 5);
			endToEnd(context, server, spot);

			// The pictures, in creative, the vent held open long enough to take them all.
			server.runCommand("gamemode creative @a");
			String gateSeconds = JugcraftConfig.textOption("lairs.gate_seconds");
			try {
				JugcraftConfig.setTextOption("lairs.gate_seconds", "600");
				AtomicReference<LairInstance> instance = new AtomicReference<>();
				server.runOnServer(minecraft -> {
					ServerPlayer player = player(minecraft);
					ServerLevel overworld = minecraft.overworld();
					player.teleportTo(overworld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.KILN_SEAL));
					KilnSealRite.Press press = KilnSealRite.press(overworld, player, spot.north().below(), player.getMainHandItem());
					check(press.missing() == null, "The seal opened no kiln for the pictures: " + press);
					instance.set(press.instance());
				});
				context.waitTicks(20);
				// The vent on the magma (the camera leaves the kiln by command, which ends the visit; the vent stays).
				shoot(context, singleplayer, "minecraft:overworld", spot.getX() + 3.0, spot.getY() + 0.4, spot.getZ() + 3.0, 135, 18,
						"jugcraft_kiln_seal_vent");
				server.runOnServer(minecraft -> check(Lairs.enter(player(minecraft), instance.get()), "The camera could not go back in"));
				context.waitTicks(40);
				singleplayer.getConnection().waitForChunksRender();
				BlockPos o = KILN.origin(0);
				String kiln = KILN.dimension.identifier().toString();
				shoot(context, singleplayer, kiln, o.getX() + 41.5, o.getY() + 13, o.getZ() + 63.5, 165, 14, "jugcraft_cinder_kiln_arrival");
				shoot(context, singleplayer, kiln, o.getX() + 49.5, o.getY() + BOWL + 1, o.getZ() + 53.5, 140, 6, "jugcraft_cinder_kiln_bowl");
				shoot(context, singleplayer, kiln, o.getX() + 35.5, o.getY() + BOWL + 2, o.getZ() + 41.5, 180, 8, "jugcraft_cinder_kiln_crucible");
				shoot(context, singleplayer, kiln, o.getX() + 35.5, o.getY() + BOWL + 4, o.getZ() + 20.5, 180, 12, "jugcraft_cinder_kiln_forge");
				server.runOnServer(minecraft -> SluiceGateBlock.turn(minecraft.getLevel(KILN.dimension), o.offset(WEST_WHEEL), null));
				shoot(context, singleplayer, kiln, o.getX() + 17.5, o.getY() + BOWL + 2, o.getZ() + 41.5, 112, 10, "jugcraft_cinder_kiln_sluice");
				shoot(context, singleplayer, kiln, o.getX() + 30.5, o.getY() + BOWL + 2, o.getZ() + 32.5, 135, 14, "jugcraft_cinder_kiln_shelf");
				shoot(context, singleplayer, kiln, o.getX() + 35.5, o.getY() + BOWL + 1, o.getZ() + 44.5, 180, -55, "jugcraft_cinder_kiln_dome");
				shoot(context, singleplayer, kiln, o.getX() + 41.5, o.getY() + 13, o.getZ() + 64.5, 0, 6, "jugcraft_cinder_kiln_exit");

				// A seal pressed into the Nether's magma: a second kiln opens, and its vent rises there. The camera goes first, so
				// the chunks there are loaded for the pad of netherrack and its magma block.
				server.runCommand("execute in minecraft:the_nether run tp @a %d 101 %d".formatted(x + 2, z + 2));
				context.waitTicks(60);
				singleplayer.getConnection().waitForChunksRender();
				server.runCommand("execute in minecraft:the_nether run fill %d 100 %d %d 104 %d minecraft:air".formatted(x - 6, z - 6, x + 6, z + 6));
				server.runCommand("execute in minecraft:the_nether run fill %d 99 %d %d 99 %d minecraft:netherrack".formatted(x - 6, z - 6, x + 6, z + 6));
				server.runCommand("execute in minecraft:the_nether run setblock %d 99 %d minecraft:magma_block".formatted(x, z));
				context.waitTicks(10);
				server.runOnServer(minecraft -> {
					ServerPlayer player = player(minecraft);
					ServerLevel nether = minecraft.getLevel(Level.NETHER);
					check(nether != null && player.level() == nether, "The camera is not in the Nether");
					BlockPos magma = new BlockPos(x, 99, z);
					check(nether.getBlockState(magma).is(Blocks.MAGMA_BLOCK), "No magma block in the Nether");
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.KILN_SEAL));
					KilnSealRite.Press press = KilnSealRite.press(nether, player, magma, player.getMainHandItem());
					check(press.missing() == null && press.instance() != null && press.instance().slot == 1,
							"The seal opened no second kiln from the Nether: " + press);
					check(KilnSealRite.vent(nether, magma) != null, "No vent on the Nether's magma");
				});
				context.waitTicks(20);
				shoot(context, singleplayer, "minecraft:the_nether", x + 3.0, 100.4, z + 3.0, 135, 18, "jugcraft_kiln_seal_vent_nether");
			} finally {
				JugcraftConfig.setTextOption("lairs.gate_seconds", gateSeconds);
				server.runOnServer(minecraft -> Lairs.reset());
			}
		}
	}

	/**
	 * The Kiln Seal from end to end, step by step, with the player in survival (the instances are the whole server's, so
	 * this one test owns them):
	 * <ol>
	 * <li>pressed into basalt, or into magma in the plains, the seal is refused and kept;</li>
	 * <li>the land made a volcano's, the seal pressed into the magma opens an instance in the kiln's own dimension, places
	 * the kiln fresh (the ledge under the arrival and a half step of its stair, the Grey Mist in its arch, the bowl's cracked
	 * basalt, the slag in the channel and deep in the crucible, the forge's lip, the west sluice's wheel and its dry trough,
	 * a shelf), uses up the seal and rests it, opens a vent on the magma, and takes the player in, unable to build;</li>
	 * <li>in the kiln the sluice's wheel is a fixture and the floor is not to be changed; turned, the west sluice floods its
	 * trough and not the east one's; the slag sets the player burning, and the flooded trough puts it out; falling out of
	 * the kiln throws the player back to the ledge for the mist's toll;</li>
	 * <li>leaving takes the player back to where they stood; while the vent is open a second seal pressed there is refused
	 * and kept, and using the vent takes them back into the same kiln;</li>
	 * <li>when the instance closes the player goes home and the vent is gone.</li>
	 * </ol>
	 */
	private static void endToEnd(ClientGameTestContext context, TestServerContext server, BlockPos spot) {
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		AtomicReference<LairInstance> first = new AtomicReference<>();
		AtomicReference<Vec3> stood = new AtomicReference<>();
		// The block the seal is pressed into, in front of where the player stands (not under their feet: magma burns).
		BlockPos magma = spot.north().below();

		// 1. Refused on basalt, and on magma in the plains.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.teleportTo(overworld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.KILN_SEAL, 2));
			check(KilnSealRite.press(overworld, player, magma, player.getMainHandItem()).missing() == KilnSealRite.Missing.NOT_MAGMA
					&& player.getMainHandItem().getCount() == 2, "Pressed into basalt, the seal was not refused, or was used up");
			overworld.setBlockAndUpdate(magma, Blocks.MAGMA_BLOCK.defaultBlockState());
			check(KilnSealRite.press(overworld, player, magma, player.getMainHandItem()).missing() == KilnSealRite.Missing.NOT_VOLCANIC
					&& player.getMainHandItem().getCount() == 2, "Pressed into magma in the plains, the seal was not refused, or was used up");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.KILN_SEAL));
		});
		server.runCommand("fillbiome %d %d %d %d %d %d jugcraft:volcano".formatted(spot.getX() - 12, spot.getY() - 4, spot.getZ() - 12,
				spot.getX() + 12, spot.getY() + 12, spot.getZ() + 12));
		context.waitTicks(10);

		// 2. In a volcano's land, the seal opens the kiln and the player sinks into it.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			stood.set(player.position());
			KilnSealRite.Missing ready = KilnSealRite.check(overworld, magma);
			check(ready == null, "The seal could not open the kiln here: " + ready + ", in " + overworld.getBiome(magma).getRegisteredName());
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(magma).add(0.0, 0.5, 0.0), Direction.UP, magma, false);
			InteractionResult pressed = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
			check(pressed.consumesAction(), "Pressing the seal did not succeed: " + pressed + " (may build: "
					+ player.getAbilities().mayBuild + ")");
		});
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			LairInstance instance = Lairs.open(KILN).isEmpty() ? null : Lairs.open(KILN).getFirst();
			check(instance != null, "The seal opened no instance");
			first.set(instance);
			check(player.getMainHandItem().isEmpty(), "The seal was not used up: " + player.getMainHandItem());
			check(player.getCooldowns().isOnCooldown(new ItemStack(JugcraftLairs.KILN_SEAL)), "The seal was not rested after its pressing");
			ServerLevel lair = minecraft.getLevel(KILN.dimension);
			check(lair != null && lair.getMinY() == 0, "The Cinder Kiln's dimension: from y 0");
			BlockPos o = instance.origin();
			check(lair.getBlockState(o.offset(UNDER_ARRIVAL)).is(Blocks.POLISHED_BLACKSTONE), "No ledge under the arrival");
			check(lair.getBlockState(o.offset(STAIR_HALF)).is(Blocks.POLISHED_BLACKSTONE_SLAB), "No half step on the stair");
			for (BlockPos mist : EXIT_MIST) {
				check(lair.getBlockState(o.offset(mist)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist in the arch at " + mist);
			}
			check(lair.getBlockState(o.offset(BOWL_FLOOR)).is(JugcraftLairs.CRACKED_BASALT), "No cracked basalt in the bowl");
			check(lair.getBlockState(o.offset(CHANNEL)).is(JugcraftLairs.MOLTEN_SLAG), "No slag in the heat channel");
			check(lair.getBlockState(o.offset(CRUCIBLE_DEEP)).is(JugcraftLairs.MOLTEN_SLAG), "The crucible's slag is not deep");
			check(lair.getBlockState(o.offset(LIP)).is(JugcraftLairs.KILN_BRICK), "No lip of kiln brick before the forge");
			check(lair.getBlockState(o.offset(WEST_WHEEL)).is(JugcraftLairs.SLUICE_GATE)
					&& lair.getBlockState(o.offset(WEST_WHEEL)).getValue(SluiceGateBlock.PART) == SluiceGateBlock.Part.WHEEL
					&& lair.getBlockState(o.offset(WEST_WHEEL)).getValue(SluiceGateBlock.FLOW) == SluiceGateBlock.Flow.READY,
					"No ready wheel on the west sluice");
			check(lair.getBlockState(o.offset(WEST_TROUGH)).is(JugcraftLairs.TROUGH_STONE)
					&& !lair.getBlockState(o.offset(WEST_TROUGH)).getValue(TroughStoneBlock.FLOODED), "No dry trough before the west sluice");
			check(lair.getBlockState(o.offset(SHELF)).is(Blocks.SMOOTH_BASALT), "No shelf in the bowl");
			check(player.level() == lair && player.position().distanceTo(instance.arrival()) < 0.5,
					"The player did not land on the ledge: " + player.level().dimension() + " " + player.position());
			check(!player.getAbilities().mayBuild, "In a lair a player cannot build");
			MistGateEntity vent = KilnSealRite.vent(overworld, magma);
			check(vent != null && vent.instance() == instance && vent.lair() == KILN, "No vent on the magma where the seal was pressed");
		});
		context.waitTicks(40);

		// 3. The wheel is a fixture; the floor is not to be changed; the sluice, the slag and the trough; the kiln's edge.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(KILN.dimension);
			BlockPos o = first.get().origin();
			BlockPos wheel = o.offset(WEST_WHEEL);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(wheel), Direction.EAST, wheel, false))
					== InteractionResult.PASS, "The sluice's wheel cannot be used");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
			BlockPos floor = o.offset(BOWL_FLOOR);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false))
					== InteractionResult.FAIL, "A block could be placed on the bowl's floor");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			check(SluiceGateBlock.turn(lair, wheel, player) == SluiceGateBlock.Turn.OPENED, "The west sluice did not open");
			check(lair.getBlockState(o.offset(WEST_TROUGH)).getValue(TroughStoneBlock.FLOODED), "The west sluice's trough did not flood");
			check(!lair.getBlockState(o.offset(EAST_TROUGH)).getValue(TroughStoneBlock.FLOODED), "The east sluice's trough flooded too");
			player.setHealth(20.0F);
			player.teleportTo(o.getX() + CHANNEL.getX() + 0.5, o.getY() + CHANNEL.getY() + 1.0, o.getZ() + CHANNEL.getZ() + 0.5);
		});
		context.waitTicks(30);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			check(player.getRemainingFireTicks() > 0, "Standing in the slag did not set the player burning");
			BlockPos o = first.get().origin();
			player.teleportTo(o.getX() + WEST_TROUGH.getX() + 0.5, o.getY() + WEST_TROUGH.getY() + 1.0, o.getZ() + WEST_TROUGH.getZ() + 0.5);
			player.setRemainingFireTicks(200);
		});
		context.waitTicks(30);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			LairInstance instance = first.get();
			BlockPos o = instance.origin();
			check(player.getRemainingFireTicks() <= 0, "Standing in the flooded trough did not put the player out");
			Vec3 arrival = instance.arrival();
			player.teleportTo(o.getX() + 35.5, o.getY() + KILN.floor - 2.0, o.getZ() + 37.5);
			player.setHealth(20.0F);
			check(Lairs.edges(player, instance) && player.position().distanceTo(arrival) < 0.5
					&& player.getHealth() == 20.0F - Lairs.EDGE_DAMAGE, "Falling out of the kiln did not end on the ledge");
			player.setHealth(20.0F);
		});

		// 4. Out, a second seal refused at the open vent, and back in through the vent.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			check(Lairs.leave(player) && player.level() == minecraft.overworld() && player.position().distanceTo(stood.get()) < 1.0E-6,
					"The player did not come back to where they stood: " + player.position());
		});
		context.waitTicks(40);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.KILN_SEAL));
			check(KilnSealRite.press(overworld, player, magma, player.getMainHandItem()).missing() == KilnSealRite.Missing.VENTING
					&& player.getMainHandItem().getCount() == 1, "A second seal at the open vent was not refused, or was used up");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			MistGateEntity vent = KilnSealRite.vent(overworld, magma);
			check(vent != null, "The vent closed early");
			vent.use(player, InteractionHand.MAIN_HAND);
			check(player.level() == minecraft.getLevel(KILN.dimension) && Lairs.instance(player.getAttached(Lairs.VISIT)) == first.get(),
					"The vent did not take the player back into the same kiln");
		});
		context.waitTicks(40);

		// 5. Closed, the player goes home and the vent is gone.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			Lairs.close(minecraft, first.get());
			check(player.level() == overworld, "Closing the kiln did not send the player home");
			check(KilnSealRite.vent(overworld, magma) == null
					&& overworld.getEntitiesOfClass(MistGateEntity.class, new AABB(magma).inflate(3)).isEmpty(), "The vent outlived its kiln");
			player.setRemainingFireTicks(0);
			player.setHealth(20.0F);
			Lairs.reset();
		});
		context.waitTicks(20);
		Jugcraft.LOGGER.info("[lairs] client game test: the Kiln Seal passed end to end");
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().get(0);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
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
		server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.1f %.1f %.1f %d %d", dimension, x, y, z, yaw, pitch));
		context.waitTicks(30);
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(10);
		context.takeScreenshot(name);
	}
}
