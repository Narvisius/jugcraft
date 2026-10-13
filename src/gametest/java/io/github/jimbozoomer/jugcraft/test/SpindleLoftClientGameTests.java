package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlockEntity;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairRules;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.SpindleRite;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Spindle Loft and its ritual (docs/features/spindle-loft.md), in a real world, which has the
 * lair dimensions: first the Cursed Spindle from end to end with the one player in survival ({@link #endToEnd}); then
 * pictures: the wheel spinning wild at midnight, and, in the loft, the view from the pincushion down the tape, the doily
 * from above, its spools and threads, the needle's eye, the thimble, the shears and the cushion from the doily. CI job
 * {@code client}.
 */
public class SpindleLoftClientGameTests implements FabricClientGameTest {
	private static final Lair LOFT = Lair.SPINDLE_LOFT;
	/** Where tools/spindle_loft.py puts things (template positions). */
	private static final BlockPos UNDER_ARRIVAL = new BlockPos(40, 37, 82);
	private static final BlockPos[] NEEDLE_EYE = {new BlockPos(45, 39, 82), new BlockPos(45, 40, 82), new BlockPos(45, 41, 82)};
	private static final BlockPos DOILY_CENTRE = new BlockPos(40, 28, 36);
	private static final BlockPos GREEN_SPOOL_THREAD = new BlockPos(30, 31, 23);
	private static final BlockPos TAPE_TOP = new BlockPos(40, 38, 79);
	private static final int LACE = 28;

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
			server.runCommand("time set midnight");
			server.runCommand("weather clear");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 10, x + 12, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 10, x + 12, y + 8, z + 6));
			context.waitTicks(10);

			BlockPos wheel = new BlockPos(x + 2, y, z - 5);
			server.runOnServer(minecraft -> minecraft.overworld().setBlock(wheel, block("spinning_wheel").defaultBlockState()
					.setValue(SpinningWheelBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL));
			endToEnd(context, server, wheel);

			// The pictures, in creative, the gate held open long enough to take them all.
			server.runCommand("gamemode creative @a");
			String gateSeconds = JugcraftConfig.textOption("lairs.gate_seconds");
			try {
				JugcraftConfig.setTextOption("lairs.gate_seconds", "600");
				AtomicReference<LairInstance> instance = new AtomicReference<>();
				server.runOnServer(minecraft -> {
					ServerPlayer player = player(minecraft);
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.CURSED_SPINDLE));
					SpindleRite.Prick prick = SpindleRite.prick(minecraft.overworld(), player, wheel, player.getMainHandItem(), true);
					check(prick.missing() == null, "The spindle opened no loft for the pictures: " + prick);
					instance.set(prick.instance());
					player.removeEffect(MobEffects.BLINDNESS);
				});
				context.waitTicks(20);
				// The wheel spinning wild (the camera leaves the loft by command, which ends the visit; the gate stays).
				shoot(context, singleplayer, "minecraft:overworld", wheel.getX() + 2.6, wheel.getY() + 0.6, wheel.getZ() + 2.6, 135, 12,
						"jugcraft_spindle_wheel");
				server.runOnServer(minecraft -> check(Lairs.enter(player(minecraft), instance.get()), "The camera could not go back in"));
				context.waitTicks(40);
				singleplayer.getConnection().waitForChunksRender();
				BlockPos o = LOFT.origin(0);
				String loft = LOFT.dimension.identifier().toString();
				shoot(context, singleplayer, loft, o.getX() + 40.5, o.getY() + 38, o.getZ() + 82.5, 180, 14, "jugcraft_spindle_loft_arrival");
				shoot(context, singleplayer, loft, o.getX() + 40.5, o.getY() + LACE + 16, o.getZ() + 68.5, 180, 32, "jugcraft_spindle_loft_doily");
				shoot(context, singleplayer, loft, o.getX() + 46.5, o.getY() + LACE + 1, o.getZ() + 43.5, 135, -28, "jugcraft_spindle_loft_spools");
				shoot(context, singleplayer, loft, o.getX() + 42.5, o.getY() + 38, o.getZ() + 82.5, 270, -6, "jugcraft_spindle_loft_needle");
				shoot(context, singleplayer, loft, o.getX() + 45.5, o.getY() + LACE + 1, o.getZ() + 36.5, 270, -4, "jugcraft_spindle_loft_thimble");
				shoot(context, singleplayer, loft, o.getX() + 40.5, o.getY() + LACE + 1, o.getZ() + 33.5, 180, -26, "jugcraft_spindle_loft_shears");
				shoot(context, singleplayer, loft, o.getX() + 40.5, o.getY() + LACE + 2, o.getZ() + 40.5, 0, -6, "jugcraft_spindle_loft_cushion");
			} finally {
				JugcraftConfig.setTextOption("lairs.gate_seconds", gateSeconds);
				server.runOnServer(minecraft -> Lairs.reset());
			}
		}
	}

	/**
	 * The Cursed Spindle from end to end, step by step, with the player in survival (the instances are the whole
	 * server's, so this one test owns them):
	 * <ol>
	 * <li>the spindle used on the wheel at midnight opens an instance in the loft's own dimension, places the loft fresh
	 * (the leaf under the arrival, the needle's eye, the doily, a spool, the tape where the template puts them), uses up
	 * the spindle, sets the wheel spinning wild as the gate, and takes the player in, blind for a moment and unable to
	 * build;</li>
	 * <li>the needle's eye is a fixture, the lace is not; falling through the doily throws the player back to the
	 * pincushion for the mist's toll;</li>
	 * <li>leaving takes the player back to where they stood; while the gate is open an empty hand on the wheel takes them
	 * in again, and a second spindle there is refused and kept;</li>
	 * <li>when the instance closes the wheel calms and is no gate: an empty hand works its treadle again.</li>
	 * </ol>
	 */
	private static void endToEnd(ClientGameTestContext context, TestServerContext server, BlockPos wheel) {
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		AtomicReference<LairInstance> first = new AtomicReference<>();
		AtomicReference<Vec3> stood = new AtomicReference<>();

		// 1. The spindle on the wheel: an instance opens and the player wakes in it.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.teleportTo(overworld, wheel.getX() + 0.5, wheel.getY(), wheel.getZ() + 2.5, Set.of(), 180.0F, 20.0F, true);
			stood.set(player.position());
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.CURSED_SPINDLE));
			check(SpindleRite.useBlock(player, overworld, InteractionHand.MAIN_HAND, hit(wheel)) == InteractionResult.SUCCESS,
					"The spindle's use was not the rite's");
		});
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			// useBlock asks the world whether it is night: the command above set midnight.
			LairInstance instance = Lairs.open(LOFT).isEmpty() ? null : Lairs.open(LOFT).getFirst();
			check(instance != null, "The spindle opened no instance");
			first.set(instance);
			check(player.getMainHandItem().isEmpty(), "The spindle was not used up: " + player.getMainHandItem());
			ServerLevel lair = minecraft.getLevel(LOFT.dimension);
			check(lair != null && lair.getMinY() == 0, "The Spindle Loft's dimension: from y 0");
			BlockPos o = instance.origin();
			check(lair.getBlockState(o.offset(UNDER_ARRIVAL)).is(JugcraftLairs.PINCUSHION_LEAF), "No felt leaf under the arrival");
			for (BlockPos eye : NEEDLE_EYE) {
				check(lair.getBlockState(o.offset(eye)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist in the needle's eye at " + eye);
			}
			check(lair.getBlockState(o.offset(DOILY_CENTRE)).is(JugcraftLairs.DOILY_LACE), "No lace at the doily's centre");
			check(lair.getBlockState(o.offset(GREEN_SPOOL_THREAD)).is(JugcraftLairs.SPOOL_THREAD), "No thread on the green spool");
			check(lair.getBlockState(o.offset(TAPE_TOP)).is(JugcraftLairs.MEASURING_TAPE), "No tape at the cushion's top");
			check(player.level() == lair && player.position().distanceTo(instance.arrival()) < 0.5,
					"The player did not wake on the pincushion: " + player.level().dimension() + " " + player.position());
			check(player.hasEffect(MobEffects.BLINDNESS), "The player woke wide awake");
			check(!player.getAbilities().mayBuild, "In a lair a player cannot build");
			check(SpindleRite.gate(overworld, wheel) == instance && ((SpinningWheelBlockEntity) overworld.getBlockEntity(wheel))
					.isWild(overworld.getGameTime()), "The wheel is not the gate, spinning wild");
		});
		context.waitTicks(40);

		// 2. The needle's eye is the loft's own exit; the lace is not to be changed; the fall through the doily.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(LOFT.dimension);
			LairInstance instance = first.get();
			BlockPos o = instance.origin();
			BlockPos eye = o.offset(NEEDLE_EYE[1]);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(eye), Direction.WEST, eye, false))
					== InteractionResult.PASS, "The needle's eye cannot be used");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
			BlockPos lace = o.offset(DOILY_CENTRE);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(lace), Direction.UP, lace, false))
					== InteractionResult.FAIL, "A block could be placed on the doily");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			Vec3 arrival = instance.arrival();
			player.teleportTo(o.getX() + 40.5, o.getY() + LOFT.floor - 2.0, o.getZ() + 36.5);
			player.setHealth(20.0F);
			check(Lairs.edges(player, instance) && player.position().distanceTo(arrival) < 0.5
					&& player.getHealth() == 20.0F - Lairs.EDGE_DAMAGE, "Falling through the doily did not end on the pincushion");
			player.setHealth(20.0F);
		});

		// 3. Out, back in through the wheel with an empty hand, and a second spindle refused.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			check(Lairs.leave(player) && player.level() == minecraft.overworld() && player.position().distanceTo(stood.get()) < 1.0E-6,
					"The player did not come back to where they stood: " + player.position());
		});
		context.waitTicks(40);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.removeEffect(MobEffects.BLINDNESS);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.CURSED_SPINDLE));
			check(SpindleRite.prick(overworld, player, wheel, player.getMainHandItem(), true).missing() == SpindleRite.Missing.SPINNING
					&& player.getMainHandItem().getCount() == 1, "A second spindle on the spinning wheel was not refused, or was used up");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			check(SpindleRite.useBlock(player, overworld, InteractionHand.MAIN_HAND, hit(wheel)) == InteractionResult.SUCCESS,
					"An empty hand on the spinning wheel did not follow");
			check(player.level() == minecraft.getLevel(LOFT.dimension) && Lairs.instance(player.getAttached(Lairs.VISIT)) == first.get()
					&& player.hasEffect(MobEffects.BLINDNESS), "Following through the wheel did not take the player into the same loft");
		});
		context.waitTicks(40);

		// 4. Closed, the gate shuts at once and the wheel calms (if its chunk was unloaded while the player was away, once the
		// player is home and it is loaded again), and is a wheel again.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			Lairs.close(minecraft, first.get());
			check(player.level() == overworld, "Closing the loft did not send the player home");
			check(SpindleRite.gate(overworld, wheel) == null, "The wheel is still a gate");
		});
		context.waitTicks(20);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			SpinningWheelBlockEntity entity = (SpinningWheelBlockEntity) overworld.getBlockEntity(wheel);
			check(!entity.isWild(overworld.getGameTime()), "The wheel still spins wild after the loft closed");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			check(SpindleRite.useBlock(player, overworld, InteractionHand.MAIN_HAND, hit(wheel)) == InteractionResult.PASS,
					"An empty hand on the calm wheel was taken by the rite");
			player.removeEffect(MobEffects.BLINDNESS);
			Lairs.reset();
		});
		context.waitTicks(20);
		Jugcraft.LOGGER.info("[lairs] client game test: the Cursed Spindle passed end to end");
	}

	private static BlockHitResult hit(BlockPos wheel) {
		return new BlockHitResult(Vec3.atCenterOf(wheel).relative(Direction.SOUTH, 0.5), Direction.SOUTH, wheel, false);
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().get(0);
	}

	private static Block block(String id) {
		return BuiltInRegistries.BLOCK.getValue(Jugcraft.id(id));
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
