package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.FrostHornRite;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairRules;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MistGateEntity;
import io.github.jimbozoomer.jugcraft.lair.TrampledSnowBlock;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Glacier Hall and the Frost Horn (docs/features/glacier-hall.md), in a real world, which has
 * the lair dimensions: first the Frost Horn from end to end with the one player in survival ({@link #endToEnd}); then
 * pictures: the whirl of white mist in the snow at midnight, and, in the hall, the view from the arrival ledge, the lake
 * from the snow ramp's foot, the throne under its tusks, the vault overhead, an ice column on the lake, a den and the
 * Grey Mist's arch. CI job {@code client}.
 */
public class GlacierHallClientGameTests implements FabricClientGameTest {
	private static final Lair HALL = Lair.GLACIER_HALL;
	/** Where tools/glacier_hall.py puts things (template positions). */
	private static final BlockPos UNDER_ARRIVAL = new BlockPos(39, 14, 81);
	private static final BlockPos[] EXIT_MIST = {new BlockPos(44, 15, 80), new BlockPos(44, 16, 81), new BlockPos(44, 17, 80)};
	private static final BlockPos LAKE_DRIFT = new BlockPos(39, 4, 30);
	private static final BlockPos LAKE_GLARE = new BlockPos(39, 4, 36);
	private static final BlockPos COLUMN_RING = new BlockPos(31, 4, 23);
	private static final BlockPos RAMP_HALF = new BlockPos(39, 5, 59);
	private static final BlockPos THRONE_SEAT = new BlockPos(39, 8, 6);
	private static final int LAKE = 4;

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:snow_block".formatted(x - 8, y - 3, z - 10, x + 12, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 10, x + 12, y + 8, z + 6));
			context.waitTicks(10);

			BlockPos spot = new BlockPos(x + 2, y, z - 5);
			endToEnd(context, server, spot);

			// The pictures, in creative, the whirl held open long enough to take them all.
			server.runCommand("gamemode creative @a");
			String gateSeconds = JugcraftConfig.textOption("lairs.gate_seconds");
			try {
				JugcraftConfig.setTextOption("lairs.gate_seconds", "600");
				AtomicReference<LairInstance> instance = new AtomicReference<>();
				server.runOnServer(minecraft -> {
					ServerPlayer player = player(minecraft);
					ServerLevel overworld = minecraft.overworld();
					player.teleportTo(overworld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.FROST_HORN));
					FrostHornRite.Call call = FrostHornRite.blow(overworld, player, player.getMainHandItem(), true);
					check(call.missing() == null, "The horn opened no hall for the pictures: " + call);
					instance.set(call.instance());
					player.setTicksFrozen(0);
				});
				context.waitTicks(20);
				// The whirl in the snow (the camera leaves the hall by command, which ends the visit; the whirl stays).
				shoot(context, singleplayer, "minecraft:overworld", spot.getX() + 3.0, spot.getY() + 0.4, spot.getZ() + 3.0, 135, 18,
						"jugcraft_frost_horn_whirl");
				server.runOnServer(minecraft -> check(Lairs.enter(player(minecraft), instance.get()), "The camera could not go back in"));
				context.waitTicks(40);
				singleplayer.getConnection().waitForChunksRender();
				BlockPos o = HALL.origin(0);
				String hall = HALL.dimension.identifier().toString();
				shoot(context, singleplayer, hall, o.getX() + 39.5, o.getY() + 15, o.getZ() + 81.5, 180, 14, "jugcraft_glacier_hall_arrival");
				shoot(context, singleplayer, hall, o.getX() + 39.5, o.getY() + LAKE + 1, o.getZ() + 55.5, 180, 4, "jugcraft_glacier_hall_lake");
				shoot(context, singleplayer, hall, o.getX() + 39.5, o.getY() + LAKE + 2, o.getZ() + 24.5, 180, 6, "jugcraft_glacier_hall_throne");
				shoot(context, singleplayer, hall, o.getX() + 39.5, o.getY() + LAKE + 1, o.getZ() + 44.5, 180, -55, "jugcraft_glacier_hall_vault");
				shoot(context, singleplayer, hall, o.getX() + 34.5, o.getY() + LAKE + 3, o.getZ() + 31.5, 135, 20, "jugcraft_glacier_hall_column");
				shoot(context, singleplayer, hall, o.getX() + 20.5, o.getY() + LAKE + 1, o.getZ() + 36.5, 90, 4, "jugcraft_glacier_hall_den");
				shoot(context, singleplayer, hall, o.getX() + 40.5, o.getY() + 15, o.getZ() + 80.5, 270, 8, "jugcraft_glacier_hall_exit");
			} finally {
				JugcraftConfig.setTextOption("lairs.gate_seconds", gateSeconds);
				server.runOnServer(minecraft -> Lairs.reset());
			}
		}
	}

	/**
	 * The Frost Horn from end to end, step by step, with the player in survival (the instances are the whole server's, so
	 * this one test owns them):
	 * <ol>
	 * <li>the horn blown on the snow at midnight opens an instance in the hall's own dimension, places the hall fresh (the
	 * ledge under the arrival, the Grey Mist in its arch, the lake's drift snow, glare ice and trampled snow, the ramp's half
	 * steps and the throne where the template puts them), uses up the horn and rests it, opens a whirl in the snow where
	 * it was blown, and takes the player in, frosted and unable to build;</li>
	 * <li>the arch's mist is a fixture, the lake is not to be changed; falling out of the hall throws the player back to
	 * the ledge for the mist's toll;</li>
	 * <li>leaving takes the player back to where they stood; while the whirl is open a second horn blown there is refused
	 * and kept, and using the whirl takes them back into the same hall, frosted again;</li>
	 * <li>when the instance closes the player goes home and the whirl is gone.</li>
	 * </ol>
	 */
	private static void endToEnd(ClientGameTestContext context, TestServerContext server, BlockPos spot) {
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		AtomicReference<LairInstance> first = new AtomicReference<>();
		AtomicReference<Vec3> stood = new AtomicReference<>();

		// 1. The horn on the snow: an instance opens and the player falls into it.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.teleportTo(overworld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			stood.set(player.position());
			check(overworld.getBlockState(spot.below()).is(Blocks.SNOW_BLOCK), "The player does not stand on snow");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.FROST_HORN));
			// use() asks the world whether it is night: the command above set midnight.
			check(JugcraftLairs.FROST_HORN.use(overworld, player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS,
					"Blowing the horn did not succeed");
		});
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			LairInstance instance = Lairs.open(HALL).isEmpty() ? null : Lairs.open(HALL).getFirst();
			check(instance != null, "The horn opened no instance");
			first.set(instance);
			check(player.getMainHandItem().isEmpty(), "The horn was not used up: " + player.getMainHandItem());
			check(player.getCooldowns().isOnCooldown(new ItemStack(JugcraftLairs.FROST_HORN)), "The horn was not rested after its call");
			ServerLevel lair = minecraft.getLevel(HALL.dimension);
			check(lair != null && lair.getMinY() == 0, "The Glacier Hall's dimension: from y 0");
			BlockPos o = instance.origin();
			check(lair.getBlockState(o.offset(UNDER_ARRIVAL)).is(JugcraftLairs.TRAMPLED_SNOW), "No trampled snow under the arrival");
			for (BlockPos mist : EXIT_MIST) {
				check(lair.getBlockState(o.offset(mist)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist in the arch at " + mist);
			}
			check(lair.getBlockState(o.offset(LAKE_DRIFT)).is(JugcraftLairs.DRIFT_SNOW), "No drift snow on the lake");
			check(lair.getBlockState(o.offset(LAKE_GLARE)).is(JugcraftLairs.GLARE_ICE), "No glare ice in the lake's middle");
			check(lair.getBlockState(o.offset(COLUMN_RING)).is(JugcraftLairs.TRAMPLED_SNOW), "No trampled snow round a column");
			check(lair.getBlockState(o.offset(RAMP_HALF)).is(JugcraftLairs.TRAMPLED_SNOW)
					&& lair.getBlockState(o.offset(RAMP_HALF)).getValue(TrampledSnowBlock.HEIGHT) == 1, "No half step at the ramp's foot");
			check(lair.getBlockState(o.offset(THRONE_SEAT)).is(Blocks.PACKED_ICE), "No throne on the dais");
			check(player.level() == lair && player.position().distanceTo(instance.arrival()) < 0.5,
					"The player did not land on the ledge: " + player.level().dimension() + " " + player.position());
			check(player.getTicksFrozen() > 60 && !player.isFullyFrozen(), "The player landed without frost, or frozen through: "
					+ player.getTicksFrozen());
			check(!player.getAbilities().mayBuild, "In a lair a player cannot build");
			MistGateEntity whirl = FrostHornRite.whirl(overworld, spot);
			check(whirl != null && whirl.instance() == instance && whirl.lair() == HALL, "No whirl in the snow where the horn was blown");
		});
		context.waitTicks(40);

		// 2. The arch's mist is the hall's own exit; the lake is not to be changed; falling out of the hall.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(HALL.dimension);
			LairInstance instance = first.get();
			BlockPos o = instance.origin();
			BlockPos mist = o.offset(EXIT_MIST[0]);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(mist), Direction.WEST, mist, false))
					== InteractionResult.PASS, "The arch's mist cannot be used");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
			BlockPos snow = o.offset(LAKE_DRIFT);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(snow), Direction.UP, snow, false))
					== InteractionResult.FAIL, "A block could be placed on the lake");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			Vec3 arrival = instance.arrival();
			player.teleportTo(o.getX() + 39.5, o.getY() + HALL.floor - 2.0, o.getZ() + 36.5);
			player.setHealth(20.0F);
			check(Lairs.edges(player, instance) && player.position().distanceTo(arrival) < 0.5
					&& player.getHealth() == 20.0F - Lairs.EDGE_DAMAGE, "Falling out of the hall did not end on the ledge");
			player.setHealth(20.0F);
		});

		// 3. Out, a second horn refused at the open whirl, and back in through the whirl.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			check(Lairs.leave(player) && player.level() == minecraft.overworld() && player.position().distanceTo(stood.get()) < 1.0E-6,
					"The player did not come back to where they stood: " + player.position());
		});
		context.waitTicks(40);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.setTicksFrozen(0);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.FROST_HORN));
			check(FrostHornRite.blow(overworld, player, player.getMainHandItem(), true).missing() == FrostHornRite.Missing.WHIRLING
					&& player.getMainHandItem().getCount() == 1, "A second horn at the open whirl was not refused, or was used up");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			MistGateEntity whirl = FrostHornRite.whirl(overworld, spot);
			check(whirl != null, "The whirl closed early");
			whirl.use(player, InteractionHand.MAIN_HAND);
			check(player.level() == minecraft.getLevel(HALL.dimension) && Lairs.instance(player.getAttached(Lairs.VISIT)) == first.get()
					&& player.getTicksFrozen() > 60, "The whirl did not take the player back into the same hall, frosted");
		});
		context.waitTicks(40);

		// 4. Closed, the player goes home and the whirl is gone.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			Lairs.close(minecraft, first.get());
			check(player.level() == overworld, "Closing the hall did not send the player home");
			check(FrostHornRite.whirl(overworld, spot) == null
					&& overworld.getEntitiesOfClass(MistGateEntity.class, new AABB(spot).inflate(3)).isEmpty(), "The whirl outlived its hall");
			player.setTicksFrozen(0);
			Lairs.reset();
		});
		context.waitTicks(20);
		Jugcraft.LOGGER.info("[lairs] client game test: the Frost Horn passed end to end");
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
