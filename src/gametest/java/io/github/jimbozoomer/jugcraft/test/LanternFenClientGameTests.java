package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.FenLampBlock;
import io.github.jimbozoomer.jugcraft.lair.HagStoneRite;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairRules;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MistGateEntity;
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
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Lantern Fen and the Hag Stone (docs/features/lantern-fen.md), in a real world, which has the
 * lair dimensions: first the Hag Stone from end to end with the one player in survival ({@link #endToEnd}); then pictures:
 * the mist rising from a marsh where the stone was looked through, and, in the fen, the view from the jetty, the marsh
 * from a lamp's platform, a lamp burning and one gone out, the Hag's hut and porch, the fen from above, the dead willows
 * on a bank and the Grey Mist between the willows behind the jetty; last, the game's own block light on every open cell
 * of the marsh. CI job {@code client}.
 */
public class LanternFenClientGameTests implements FabricClientGameTest {
	private static final Lair FEN = Lair.LANTERN_FEN;
	/** Where tools/lantern_fen.py puts things (template positions). */
	private static final BlockPos UNDER_ARRIVAL = new BlockPos(40, 9, 80);
	private static final BlockPos[] EXIT_MIST = {new BlockPos(39, 10, 83), new BlockPos(40, 11, 83), new BlockPos(39, 12, 83)};
	private static final BlockPos[] LAMPS = {new BlockPos(30, 12, 33), new BlockPos(49, 12, 33), new BlockPos(30, 12, 52),
			new BlockPos(49, 12, 52)};
	private static final BlockPos CAULDRON = new BlockPos(38, 12, 18);
	private static final BlockPos PORCH = new BlockPos(40, 11, 17);
	private static final BlockPos MARSH_WATER = new BlockPos(40, 9, 43);
	private static final BlockPos RING_DECK = new BlockPos(40, 9, 57);
	private static final int MUD = 8;
	private static final int STAND = 10;
	private static final double CENTRE_X = 40.0;
	private static final double CENTRE_Z = 43.0;
	private static final double MARSH_RADIUS = 22.0;
	/** The least block light on any open cell of the marsh (tools/lantern_fen.py MARSH_LIGHT, checked by the audit). */
	private static final int MARSH_LIGHT = 8;

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
			// A patch of marsh to stand in: mud under a pool one block deep, a bank of grass beside it; its land plains for now.
			server.runCommand("fill %d %d %d %d %d %d minecraft:mud".formatted(x - 8, y - 3, z - 10, x + 12, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 10, x + 12, y + 8, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:water".formatted(x - 2, y - 1, z - 8, x + 6, y - 1, z - 2));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x + 8, y - 1, z - 8, x + 12, y - 1, z - 2));
			server.runCommand("fillbiome %d %d %d %d %d %d minecraft:plains".formatted(x - 16, y - 4, z - 16, x + 16, y + 12, z + 12));
			context.waitTicks(10);

			// Standing in the pool: the feet in its water, on the mud under it.
			BlockPos spot = new BlockPos(x + 2, y - 1, z - 5);
			BlockPos dry = new BlockPos(x + 10, y, z - 5);
			endToEnd(context, server, spot, dry);

			// The pictures, in creative, the mist held open long enough to take them all.
			server.runCommand("gamemode creative @a");
			String gateSeconds = JugcraftConfig.textOption("lairs.gate_seconds");
			try {
				JugcraftConfig.setTextOption("lairs.gate_seconds", "600");
				AtomicReference<LairInstance> instance = new AtomicReference<>();
				server.runOnServer(minecraft -> {
					ServerPlayer player = player(minecraft);
					ServerLevel overworld = minecraft.overworld();
					player.teleportTo(overworld, spot.getX() + 0.5, spot.getY() - 1 + 14 / 16.0, spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HAG_STONE));
					HagStoneRite.Look look = HagStoneRite.look(overworld, player, player.getMainHandItem());
					check(look.missing() == null, "The stone opened no fen for the pictures: " + look);
					instance.set(look.instance());
				});
				context.waitTicks(40);
				// The mist over the pool (the camera leaves the fen by command, which ends the visit; the mist stays).
				shoot(context, singleplayer, "minecraft:overworld", spot.getX() + 3.5, spot.getY() + 1.0, spot.getZ() + 3.5, 135, 14,
						"jugcraft_hag_stone_mist");
				server.runOnServer(minecraft -> check(Lairs.enter(player(minecraft), instance.get()), "The camera could not go back in"));
				context.waitTicks(40);
				singleplayer.getConnection().waitForChunksRender();
				BlockPos o = FEN.origin(0);
				String fen = FEN.dimension.identifier().toString();
				shoot(context, singleplayer, fen, o.getX() + 40.0, o.getY() + STAND, o.getZ() + 80.5, 180, 8, "jugcraft_lantern_fen_arrival");
				shoot(context, singleplayer, fen, o.getX() + 28.5, o.getY() + STAND, o.getZ() + 54.5, 225, 6, "jugcraft_lantern_fen_marsh");
				shoot(context, singleplayer, fen, o.getX() + 46.5, o.getY() + STAND, o.getZ() + 55.5, 225, -10, "jugcraft_lantern_fen_lamp");
				server.runOnServer(minecraft -> FenLampBlock.snuff(minecraft.getLevel(FEN.dimension), o.offset(LAMPS[3])));
				shoot(context, singleplayer, fen, o.getX() + 46.5, o.getY() + STAND, o.getZ() + 55.5, 225, -10, "jugcraft_lantern_fen_lamp_out");
				server.runOnServer(minecraft -> FenLampBlock.light(minecraft.getLevel(FEN.dimension), o.offset(LAMPS[3])));
				shoot(context, singleplayer, fen, o.getX() + 40.0, o.getY() + STAND, o.getZ() + 26.5, 180, -8, "jugcraft_lantern_fen_hut");
				shoot(context, singleplayer, fen, o.getX() + 40.0, o.getY() + STAND + 22, o.getZ() + 82.5, 180, 32, "jugcraft_lantern_fen_above");
				shoot(context, singleplayer, fen, o.getX() + 25.5, o.getY() + STAND + 1, o.getZ() + 44.5, 92, -16, "jugcraft_lantern_fen_willows");
				shoot(context, singleplayer, fen, o.getX() + 40.0, o.getY() + STAND, o.getZ() + 78.5, 0, 4, "jugcraft_lantern_fen_exit");

				// The game's own light on every open cell of the marsh, where the Hag and whoever hunts her are seen.
				server.runOnServer(minecraft -> {
					ServerLevel lair = minecraft.getLevel(FEN.dimension);
					int least = 15;
					int cells = 0;
					BlockPos darkest = null;
					for (int dx = 0; dx < FEN.width; dx++) {
						for (int dz = 0; dz < FEN.length; dz++) {
							double rx = dx + 0.5 - CENTRE_X;
							double rz = dz + 0.5 - CENTRE_Z;
							if (rx * rx + rz * rz > MARSH_RADIUS * MARSH_RADIUS) {
								continue;
							}
							BlockPos cell = o.offset(dx, STAND, dz);
							BlockState state = lair.getBlockState(cell);
							if (!state.isAir() && !state.is(Blocks.LIGHT) && !state.getCollisionShape(lair, cell).isEmpty()
									&& !state.is(Blocks.LILY_PAD)) {
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
					Jugcraft.LOGGER.info("[lairs] the Lantern Fen's marsh: {} open cells, the darkest at block light {} ({})", cells, least, darkest);
					check(cells > 1200 && least >= MARSH_LIGHT, "The Lantern Fen's marsh has " + cells + " open cells, the darkest at block light "
							+ least + " (" + darkest + "), under " + MARSH_LIGHT);
				});
			} finally {
				JugcraftConfig.setTextOption("lairs.gate_seconds", gateSeconds);
				server.runOnServer(minecraft -> Lairs.reset());
			}
		}
	}

	/**
	 * The Hag Stone from end to end, step by step, with the player in survival (the instances are the whole server's, so
	 * this one test owns them):
	 * <ol>
	 * <li>looked through in the pool in the plains, by day in a bog's land, or at night on stone out of the water, the
	 * stone is refused and kept;</li>
	 * <li>at night, in the pool, the land a bog's, the stone opens an instance in the fen's own dimension, places the fen
	 * fresh (the jetty under the arrival, the Grey Mist between the willows, the four lamps burning, the Hag's cauldron on
	 * her porch, the marsh's water one block deep on mud), uses up the stone and rests it, raises a mist over the pool, and
	 * takes the player in, unable to build;</li>
	 * <li>in the fen a lamp is a fixture and the boardwalk is not to be built on; a lamp put out is lit again by the
	 * player's hand; in the marsh the player wades, knee-deep on the mud; falling off the fen throws them back to the jetty
	 * for the mist's toll;</li>
	 * <li>leaving takes the player back to where they stood; while the mist is risen a second stone looked through there
	 * is refused and kept, and using the mist takes them back into the same fen;</li>
	 * <li>when the instance closes the player goes home and the mist is gone.</li>
	 * </ol>
	 */
	private static void endToEnd(ClientGameTestContext context, TestServerContext server, BlockPos spot, BlockPos dry) {
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		AtomicReference<LairInstance> first = new AtomicReference<>();
		AtomicReference<Vec3> stood = new AtomicReference<>();

		// 1. Refused in the plains, by day, and out of the water.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.teleportTo(overworld, spot.getX() + 0.5, spot.getY() - 1 + 14 / 16.0, spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HAG_STONE, 2));
			check(HagStoneRite.feet(player).equals(spot) && overworld.getBlockState(spot).is(Blocks.WATER),
					"The player is not standing in the pool: feet " + HagStoneRite.feet(player));
			HagStoneRite.Look look = HagStoneRite.look(overworld, player, player.getMainHandItem());
			check(look.missing() == HagStoneRite.Missing.NOT_WETLAND && player.getMainHandItem().getCount() == 2,
					"In the plains, the stone was not refused, or was used up: " + look + ", " + player.getMainHandItem());
		});
		// The pool lies a block below the ground, so its land is a bog's from no lower than the plains' (the world's floor here).
		server.runCommand("fillbiome %d %d %d %d %d %d jugcraft:bog".formatted(spot.getX() - 12, spot.getY() - 3, spot.getZ() - 12,
				spot.getX() + 12, spot.getY() + 12, spot.getZ() + 12));
		server.runCommand("time set noon");
		context.waitTicks(10);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			check(overworld.getBiome(spot).is(HagStoneRite.LANDS) && overworld.getBiome(dry).is(HagStoneRite.LANDS),
					"The land did not become a bog's: the pool's is " + overworld.getBiome(spot).getRegisteredName() + ", the stone's "
							+ overworld.getBiome(dry).getRegisteredName());
			HagStoneRite.Look look = HagStoneRite.look(overworld, player, player.getMainHandItem());
			check(look.missing() == HagStoneRite.Missing.NOT_NIGHT && player.getMainHandItem().getCount() == 2,
					"By day, the stone was not refused, or was used up: " + look + ", " + player.getMainHandItem());
		});
		server.runCommand("time set midnight");
		context.waitTicks(10);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.teleportTo(overworld, dry.getX() + 0.5, dry.getY(), dry.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			HagStoneRite.Look look = HagStoneRite.look(overworld, player, player.getMainHandItem());
			check(look.missing() == HagStoneRite.Missing.NOT_WET && player.getMainHandItem().getCount() == 2,
					"On stone, out of the water, the stone was not refused, or was used up: " + look + ", " + player.getMainHandItem());
			player.teleportTo(overworld, spot.getX() + 0.5, spot.getY() - 1 + 14 / 16.0, spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HAG_STONE));
		});
		context.waitTicks(10);

		// 2. At night in a bog's pool, the stone opens the fen and the mist takes the player.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			stood.set(player.position());
			HagStoneRite.Missing ready = HagStoneRite.check(overworld, HagStoneRite.feet(player), !overworld.isBrightOutside(),
					overworld.getBiome(spot).is(HagStoneRite.LANDS));
			check(ready == null, "The stone could not open the fen here: " + ready + ", in " + overworld.getBiome(spot).getRegisteredName());
			InteractionResult looked = player.getMainHandItem().use(overworld, player, InteractionHand.MAIN_HAND);
			check(looked.consumesAction(), "Looking through the stone did not succeed: " + looked);
		});
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			LairInstance instance = Lairs.open(FEN).isEmpty() ? null : Lairs.open(FEN).getFirst();
			check(instance != null, "The stone opened no instance");
			first.set(instance);
			check(player.getMainHandItem().isEmpty(), "The stone was not used up: " + player.getMainHandItem());
			check(player.getCooldowns().isOnCooldown(new ItemStack(JugcraftLairs.HAG_STONE)), "The stone was not rested after its looking");
			ServerLevel lair = minecraft.getLevel(FEN.dimension);
			check(lair != null && lair.getMinY() == 0, "The Lantern Fen's dimension: from y 0");
			BlockPos o = instance.origin();
			check(lair.getBlockState(o.offset(UNDER_ARRIVAL)).is(blockOf("willow_planks")), "No jetty under the arrival");
			for (BlockPos mist : EXIT_MIST) {
				check(lair.getBlockState(o.offset(mist)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist between the willows at " + mist);
			}
			for (BlockPos lamp : LAMPS) {
				check(lair.getBlockState(o.offset(lamp)).is(JugcraftLairs.FEN_LAMP) && lair.getBlockState(o.offset(lamp)).getValue(FenLampBlock.LIT),
						"No lamp burning at " + lamp);
			}
			check(lair.getBlockState(o.offset(CAULDRON)).is(blockOf("bubbling_cauldron")), "No cauldron on the Hag's porch");
			check(lair.getBlockState(o.offset(PORCH)).is(blockOf("dead_planks")), "No porch where the Hag waits");
			check(lair.getBlockState(o.offset(MARSH_WATER)).is(Blocks.WATER) && lair.getBlockState(o.offset(MARSH_WATER).above()).isAir()
					&& lair.getBlockState(o.offset(MARSH_WATER).below()).is(Blocks.MUD), "The marsh is not water one block deep on mud");
			check(player.level() == lair && player.position().distanceTo(instance.arrival()) < 0.5,
					"The player did not wake on the jetty: " + player.level().dimension() + " " + player.position());
			check(!player.getAbilities().mayBuild, "In a lair a player cannot build");
			MistGateEntity mist = HagStoneRite.mist(overworld, spot);
			check(mist != null && mist.instance() == instance && mist.lair() == FEN, "No mist over the pool where the stone was looked through");
		});
		context.waitTicks(40);

		// 3. A lamp is a fixture; the boardwalk is not to be built on; a lamp put out is lit again by hand; the marsh is
		// waded; the fen's edge.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(FEN.dimension);
			BlockPos o = first.get().origin();
			BlockPos lamp = o.offset(LAMPS[0]);
			BlockHitResult atLamp = new BlockHitResult(Vec3.atCenterOf(lamp), Direction.SOUTH, lamp, false);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, atLamp) == InteractionResult.PASS, "A lamp cannot be used");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
			BlockPos deck = o.offset(RING_DECK);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(deck), Direction.UP, deck, false))
					== InteractionResult.FAIL, "A block could be placed on the boardwalk");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			check(FenLampBlock.snuff(lair, lamp) && !lair.getBlockState(lamp).getValue(FenLampBlock.LIT), "The lamp did not go out");
			player.teleportTo(lamp.getX() + 0.5, lamp.getY() - 2.0, lamp.getZ() + 2.5);
			InteractionResult lit = player.gameMode.useItemOn(player, lair, ItemStack.EMPTY, InteractionHand.MAIN_HAND, atLamp);
			check(lit.consumesAction() && lair.getBlockState(lamp).getValue(FenLampBlock.LIT), "The player's hand did not light the lamp: " + lit);
			BlockPos water = o.offset(MARSH_WATER);
			player.teleportTo(water.getX() + 0.5, water.getY() + 0.5, water.getZ() + 0.5);
		});
		context.waitTicks(30);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			LairInstance instance = first.get();
			BlockPos o = instance.origin();
			check(player.isInWater() && Math.abs(player.getY() - (o.getY() + MUD + 14 / 16.0)) < 0.2,
					"The player is not wading on the marsh's mud: in water " + player.isInWater() + ", at " + (player.getY() - o.getY()));
			Vec3 arrival = instance.arrival();
			player.teleportTo(o.getX() + 40.5, o.getY() + FEN.floor - 2.0, o.getZ() + 43.5);
			player.setHealth(20.0F);
			check(Lairs.edges(player, instance) && player.position().distanceTo(arrival) < 0.5
					&& player.getHealth() == 20.0F - Lairs.EDGE_DAMAGE, "Falling off the fen did not end on the jetty");
			player.setHealth(20.0F);
		});

		// 4. Out, a second stone refused at the risen mist, and back in through the mist.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			check(Lairs.leave(player) && player.level() == minecraft.overworld() && player.position().distanceTo(stood.get()) < 1.0E-6,
					"The player did not come back to where they stood: " + player.position());
		});
		context.waitTicks(40);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HAG_STONE));
			check(HagStoneRite.look(overworld, player, player.getMainHandItem()).missing() == HagStoneRite.Missing.MISTING
					&& player.getMainHandItem().getCount() == 1, "A second stone at the risen mist was not refused, or was used up");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			MistGateEntity mist = HagStoneRite.mist(overworld, spot);
			check(mist != null, "The mist sank early");
			mist.use(player, InteractionHand.MAIN_HAND);
			check(player.level() == minecraft.getLevel(FEN.dimension) && Lairs.instance(player.getAttached(Lairs.VISIT)) == first.get(),
					"The mist did not take the player back into the same fen");
		});
		context.waitTicks(40);

		// 5. Closed, the player goes home and the mist is gone.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			Lairs.close(minecraft, first.get());
			check(player.level() == overworld, "Closing the fen did not send the player home");
			check(HagStoneRite.mist(overworld, spot) == null
					&& overworld.getEntitiesOfClass(MistGateEntity.class, new AABB(spot).inflate(3)).isEmpty(), "The mist outlived its fen");
			player.setHealth(20.0F);
			Lairs.reset();
		});
		context.waitTicks(20);
		Jugcraft.LOGGER.info("[lairs] client game test: the Hag Stone passed end to end");
	}

	private static net.minecraft.world.level.block.Block blockOf(String id) {
		return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Jugcraft.id(id));
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
