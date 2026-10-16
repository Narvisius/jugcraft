package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolves;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.HowlingTotemRite;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairRules;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MistGateEntity;
import io.github.jimbozoomer.jugcraft.lair.MoonlitRing;
import io.github.jimbozoomer.jugcraft.lair.MoonlitRing.Bell;
import io.github.jimbozoomer.jugcraft.lair.WardenBellBlock;
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
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Moonlit Ring and the Howling Totem (docs/features/moonlit-ring.md), in a real world, which has
 * the lair dimensions: first the totem from end to end with the one player in survival ({@link #endToEnd}); then
 * pictures: the Grey Mist rising where the totem was used, and, in the ring, the camp from the arrival and the camp's
 * lean-to and firepit, the clearing from its south edge with the moon over the northern trees, the Howling Stone, the
 * west bell's clearing, the west bell swinging, the north bell from the clearing, the whole ring from over the camp and
 * the Grey Mist at the end of the trail; last, the game's own block light on every open cell of the clearing and the bell
 * clearings. CI job {@code client}.
 */
public class MoonlitRingClientGameTests implements FabricClientGameTest {
	private static final Lair RING = Lair.MOONLIT_RING;
	/** Where tools/moonlit_ring.py puts things (template positions), beside what MoonlitRing.java gives. */
	private static final BlockPos UNDER_ARRIVAL = new BlockPos(37, 4, 67);
	private static final BlockPos LANTERN = new BlockPos(42, 6, 62);
	/**
	 * The least block light on any open cell of the clearing and the bell clearings, and how many open cells they have
	 * (tools/moonlit_ring.py RING_LIGHT and its open cells, checked by the audit).
	 */
	private static final int RING_LIGHT = 7;
	private static final int RING_CELLS = 1557;

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
			// A level clearing of grass open to the sky, a stone pad, and a spot under a roof; its land plains for now.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 10, x + 12, y + 40, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:dirt".formatted(x - 10, y - 3, z - 10, x + 12, y - 2, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 10, y - 1, z - 10, x + 12, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x + 8, y - 1, z - 8, x + 12, y - 1, z - 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 9, y + 3, z - 9, x - 5, y + 3, z - 5));
			server.runCommand("fillbiome %d %d %d %d %d %d minecraft:plains".formatted(x - 16, y - 4, z - 16, x + 16, y + 12, z + 12));
			context.waitTicks(10);

			BlockPos spot = new BlockPos(x + 2, y, z - 3);
			BlockPos stone = new BlockPos(x + 10, y, z - 6);
			BlockPos roofed = new BlockPos(x - 7, y, z - 7);
			endToEnd(context, server, spot, stone, roofed);

			// The pictures, in creative, the mist held open long enough to take them all.
			server.runCommand("gamemode creative @a");
			String gateSeconds = JugcraftConfig.textOption("lairs.gate_seconds");
			try {
				JugcraftConfig.setTextOption("lairs.gate_seconds", "600");
				AtomicReference<LairInstance> instance = new AtomicReference<>();
				server.runOnServer(minecraft -> {
					ServerPlayer player = player(minecraft);
					ServerLevel overworld = minecraft.overworld();
					player.teleportTo(overworld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HOWLING_TOTEM));
					HowlingTotemRite.Howl howl = HowlingTotemRite.howl(overworld, player, player.getMainHandItem());
					check(howl.missing() == null, "The totem opened no ring for the pictures: " + howl);
					instance.set(howl.instance());
					// A hunter's fire beside the spot, so the night's picture shows the mist.
					overworld.setBlock(spot.offset(3, 0, 1), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 3);
				});
				context.waitTicks(40);
				// The mist over the spot (the camera leaves the ring by command, which ends the visit; the mist stays).
				shootAt(context, singleplayer, "minecraft:overworld", spot.getX() + 3.5, y, spot.getZ() + 4.5, spot.getX() + 0.5, y + 1.0,
						spot.getZ() + 0.5, "jugcraft_howling_totem_mist");
				server.runOnServer(minecraft -> check(Lairs.enter(player(minecraft), instance.get()), "The camera could not go back in"));
				context.waitTicks(40);
				singleplayer.getConnection().waitForChunksRender();
				BlockPos o = RING.origin(0);
				String dim = RING.dimension.identifier().toString();
				int stand = MoonlitRing.STAND;
				shoot(context, singleplayer, dim, o.getX() + 37.5, o.getY() + stand, o.getZ() + 69.5, 180, -4, "jugcraft_moonlit_ring_arrival");
				shootAt(context, singleplayer, dim, o.getX() + 41.5, o.getY() + stand, o.getZ() + 69.5, o.getX() + 34.5, o.getY() + stand + 0.8,
						o.getZ() + 64.0, "jugcraft_moonlit_ring_camp");
				shoot(context, singleplayer, dim, o.getX() + 37.5, o.getY() + stand, o.getZ() + 58.5, 180, -14, "jugcraft_moonlit_ring_clearing");
				shootAt(context, singleplayer, dim, o.getX() + 30.5, o.getY() + stand, o.getZ() + 43.5, o.getX() + 37.5, o.getY() + stand + 2.0,
						o.getZ() + 36.5, "jugcraft_moonlit_ring_stone");
				BlockPos west = Bell.WEST.at(o);
				shootAt(context, singleplayer, dim, o.getX() + 17.5, o.getY() + stand, o.getZ() + 39.5, west.getX() + 0.5, west.getY() + 0.3,
						west.getZ() + 0.5, "jugcraft_moonlit_ring_west_bell");
				// The west bell swinging, held so for the picture (no rest is scheduled, so it does not come back by itself).
				server.runOnServer(minecraft -> {
					ServerLevel lair = minecraft.getLevel(RING.dimension);
					lair.setBlock(west, lair.getBlockState(west).setValue(WardenBellBlock.RINGING, true), 3);
				});
				shootAt(context, singleplayer, dim, west.getX() + 3.5, o.getY() + stand, west.getZ() + 1.5, west.getX() + 0.5, west.getY() + 0.4,
						west.getZ() + 0.5, "jugcraft_moonlit_ring_bell_ringing");
				server.runOnServer(minecraft -> {
					ServerLevel lair = minecraft.getLevel(RING.dimension);
					lair.setBlock(west, lair.getBlockState(west).setValue(WardenBellBlock.RINGING, false), 3);
				});
				BlockPos north = Bell.NORTH.at(o);
				shootAt(context, singleplayer, dim, o.getX() + 37.5, o.getY() + stand, o.getZ() + 20.5, north.getX() + 0.5, north.getY() + 0.3,
						north.getZ() + 0.5, "jugcraft_moonlit_ring_north_bell");
				// From over the camp (within the ring's bounds), the whole ring.
				shoot(context, singleplayer, dim, o.getX() + 37.5, o.getY() + stand + 32, o.getZ() + 74.5, 180, 40, "jugcraft_moonlit_ring_above");
				shoot(context, singleplayer, dim, o.getX() + 37.5, o.getY() + stand, o.getZ() + 69.5, 0, 0, "jugcraft_moonlit_ring_exit");

				// The game's own light on every open cell of the clearing and the bell clearings, where the Alpha hunts.
				server.runOnServer(minecraft -> {
					ServerLevel lair = minecraft.getLevel(RING.dimension);
					int least = 15;
					int cells = 0;
					BlockPos darkest = null;
					for (int dx = 0; dx < RING.width; dx++) {
						for (int dz = 0; dz < RING.length; dz++) {
							if (!MoonlitRing.inRing(dx + 0.5, dz + 0.5) && MoonlitRing.gladeAt(dx + 0.5, dz + 0.5) == null) {
								continue;
							}
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
					Jugcraft.LOGGER.info("[lairs] the Moonlit Ring's clearings: {} open cells, the darkest at block light {} ({})", cells, least, darkest);
					check(cells == RING_CELLS && least >= RING_LIGHT, "The ring's clearings have " + cells + " open cells (not " + RING_CELLS
							+ "), the darkest at block light " + least + " (" + darkest + "), under " + RING_LIGHT);
				});
			} finally {
				JugcraftConfig.setTextOption("lairs.gate_seconds", gateSeconds);
				server.runOnServer(minecraft -> Lairs.reset());
			}
		}
	}

	/**
	 * The Howling Totem from end to end, step by step, with the player in survival (the instances are the whole server's,
	 * so this one test owns them):
	 * <ol>
	 * <li>used in the plains the totem is refused and kept; the land made a forest's, by day the same; at night under a
	 * roof, on stone, and with wolfsbane growing near, the same;</li>
	 * <li>used by the player's hand at night on grass under the open sky in the forest, it opens an instance in the ring's
	 * own dimension, places the ring fresh (the camp's floor under the arrival, the Grey Mist at the end of the trail, each
	 * bell at rest, the camp's lantern), is used up and rested, raises Grey Mist where it was used, and brings the player to
	 * in the camp, unable to build;</li>
	 * <li>in the ring a bell is a fixture and the leaves are not; a bell rung by hand swings and rests again; falling off
	 * the ring throws them back to the camp for the mist's toll;</li>
	 * <li>leaving takes the player back to where they stood; while the mist is risen a second totem used there is refused
	 * and kept, and using the mist takes them back into the same ring;</li>
	 * <li>when the instance closes the player goes home and the mist is gone.</li>
	 * </ol>
	 */
	private static void endToEnd(ClientGameTestContext context, TestServerContext server, BlockPos spot, BlockPos stone, BlockPos roofed) {
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		AtomicReference<LairInstance> first = new AtomicReference<>();
		AtomicReference<Vec3> stood = new AtomicReference<>();

		// 1. Refused in the plains, then by day in the forest, then at night under a roof, on stone and by wolfsbane.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.teleportTo(overworld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HOWLING_TOTEM, 2));
			check(HowlingTotemRite.feet(player).equals(spot), "The player is not standing on the grass: feet " + HowlingTotemRite.feet(player));
			HowlingTotemRite.Howl howl = HowlingTotemRite.howl(overworld, player, player.getMainHandItem());
			check(howl.missing() == HowlingTotemRite.Missing.NOT_WOODS && player.getMainHandItem().getCount() == 2,
					"In the plains, the totem was not refused, or was used up: " + howl + ", " + player.getMainHandItem());
		});
		server.runCommand("fillbiome %d %d %d %d %d %d minecraft:forest".formatted(spot.getX() - 14, spot.getY() - 4, spot.getZ() - 12,
				spot.getX() + 14, spot.getY() + 12, spot.getZ() + 12));
		server.runCommand("time set noon");
		context.waitTicks(10);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			check(overworld.getBiome(spot).is(Werewolves.HAUNTS) && overworld.getBiome(stone).is(Werewolves.HAUNTS)
					&& overworld.getBiome(roofed).is(Werewolves.HAUNTS), "The land did not become a forest's: " + overworld.getBiome(spot).getRegisteredName());
			HowlingTotemRite.Howl howl = HowlingTotemRite.howl(overworld, player, player.getMainHandItem());
			check(howl.missing() == HowlingTotemRite.Missing.NOT_NIGHT && player.getMainHandItem().getCount() == 2,
					"By day, the totem was not refused, or was used up: " + howl + ", " + player.getMainHandItem());
		});
		server.runCommand("time set midnight");
		context.waitTicks(10);
		BlockPos ward = spot.offset(-Werewolves.WARD_REACH + 1, 0, 2);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.teleportTo(overworld, roofed.getX() + 0.5, roofed.getY(), roofed.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			HowlingTotemRite.Howl howl = HowlingTotemRite.howl(overworld, player, player.getMainHandItem());
			check(howl.missing() == HowlingTotemRite.Missing.ROOFED && player.getMainHandItem().getCount() == 2,
					"Under a roof, the totem was not refused, or was used up: " + howl + ", " + player.getMainHandItem());
			player.teleportTo(overworld, stone.getX() + 0.5, stone.getY(), stone.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			howl = HowlingTotemRite.howl(overworld, player, player.getMainHandItem());
			check(howl.missing() == HowlingTotemRite.Missing.NOT_GROUND && player.getMainHandItem().getCount() == 2,
					"On stone, the totem was not refused, or was used up: " + howl + ", " + player.getMainHandItem());
			overworld.setBlock(ward, JugcraftAgriculture.block(Werewolves.WOLFSBANE).defaultBlockState(), 3);
			player.teleportTo(overworld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			howl = HowlingTotemRite.howl(overworld, player, player.getMainHandItem());
			check(howl.missing() == HowlingTotemRite.Missing.WOLFSBANE && player.getMainHandItem().getCount() == 2,
					"With wolfsbane growing near, the totem was not refused, or was used up: " + howl + ", " + player.getMainHandItem());
			overworld.setBlock(ward, Blocks.AIR.defaultBlockState(), 3);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HOWLING_TOTEM));
		});
		context.waitTicks(10);

		// 2. At night in the forest, on grass under the open sky, the totem opens the ring and the mist takes the player.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			stood.set(player.position());
			HowlingTotemRite.Missing ready = HowlingTotemRite.check(overworld, HowlingTotemRite.feet(player), !overworld.isBrightOutside(),
					overworld.getBiome(spot).is(Werewolves.HAUNTS));
			check(ready == null, "The totem could not open the ring here: " + ready + ", in " + overworld.getBiome(spot).getRegisteredName());
			InteractionResult used = player.gameMode.useItem(player, overworld, player.getMainHandItem(), InteractionHand.MAIN_HAND);
			check(used.consumesAction(), "Using the totem did not succeed: " + used);
		});
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			LairInstance instance = Lairs.open(RING).isEmpty() ? null : Lairs.open(RING).getFirst();
			check(instance != null, "The totem opened no instance");
			first.set(instance);
			check(player.getMainHandItem().isEmpty(), "The totem was not used up: " + player.getMainHandItem());
			check(player.getCooldowns().isOnCooldown(new ItemStack(JugcraftLairs.HOWLING_TOTEM)), "The totem was not rested after its use");
			ServerLevel lair = minecraft.getLevel(RING.dimension);
			check(lair != null && lair.getMinY() == 0, "The Moonlit Ring's dimension: from y 0");
			BlockPos o = instance.origin();
			check(lair.getBlockState(o.offset(UNDER_ARRIVAL)).isFaceSturdy(lair, o.offset(UNDER_ARRIVAL), Direction.UP), "No camp floor under the arrival");
			for (BlockPos mist : MoonlitRing.exitCells()) {
				check(lair.getBlockState(o.offset(mist)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist at the end of the trail at " + mist);
			}
			for (Bell bell : Bell.values()) {
				BlockState state = lair.getBlockState(bell.at(o));
				check(state.is(JugcraftLairs.WARDEN_BELL) && !state.getValue(WardenBellBlock.RINGING), "No Warden Bell at rest in the "
						+ bell.title() + " clearing");
			}
			check(lair.getBlockState(o.offset(LANTERN)).is(Blocks.LANTERN), "No lantern on the camp's stump");
			check(player.level() == lair && player.position().distanceTo(instance.arrival()) < 0.5,
					"The player did not come to in the camp: " + player.level().dimension() + " " + player.position());
			check(!player.getAbilities().mayBuild, "In a lair a player cannot build");
			MistGateEntity mist = HowlingTotemRite.mist(overworld, spot);
			check(mist != null && mist.instance() == instance && mist.lair() == RING, "No mist risen where the totem was used");
		});
		context.waitTicks(40);

		// 3. A bell is a fixture, the leaves are not; a bell rung by hand swings and rests; the ring's edge.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(RING.dimension);
			BlockPos o = first.get().origin();
			BlockPos bell = Bell.WEST.at(o);
			BlockHitResult atBell = new BlockHitResult(Vec3.atCenterOf(bell), Direction.EAST, bell, false);
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, atBell) == InteractionResult.PASS, "A bell cannot be used");
			BlockPos leaves = o.offset(37, MoonlitRing.STAND + 12, 2);
			check(lair.getBlockState(leaves).is(JugcraftLairs.MOONWOOD_LEAVES), "No Moonwood Leaves in the canopy at " + leaves.subtract(o));
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(leaves), Direction.SOUTH, leaves, false))
					== InteractionResult.FAIL, "Moonwood Leaves can be used");
			player.teleportTo(bell.getX() + 2.5, o.getY() + MoonlitRing.STAND, bell.getZ() + 0.5);
			InteractionResult rung = player.gameMode.useItemOn(player, lair, ItemStack.EMPTY, InteractionHand.MAIN_HAND, atBell);
			check(rung.consumesAction() && lair.getBlockState(bell).getValue(WardenBellBlock.RINGING), "A player's hand did not ring the bell: " + rung);
		});
		context.waitTicks(WardenBellBlock.SWING_TICKS + 5);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(RING.dimension);
			BlockPos o = first.get().origin();
			check(!lair.getBlockState(Bell.WEST.at(o)).getValue(WardenBellBlock.RINGING), "The bell never came to rest");
			Vec3 arrival = first.get().arrival();
			player.teleportTo(o.getX() + MoonlitRing.CENTRE_X, o.getY() + RING.floor - 2.0, o.getZ() + MoonlitRing.CENTRE_Z);
			player.setHealth(20.0F);
			check(Lairs.edges(player, first.get()) && player.position().distanceTo(arrival) < 0.5
					&& player.getHealth() == 20.0F - Lairs.EDGE_DAMAGE, "Falling out of the ring did not end in the camp");
			player.setHealth(20.0F);
		});

		// 4. Out, a second totem refused at the risen mist, and back in through the mist.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			check(Lairs.leave(player) && player.level() == minecraft.overworld() && player.position().distanceTo(stood.get()) < 1.0E-6,
					"The player did not come back to where they stood: " + player.position());
		});
		context.waitTicks(40);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HOWLING_TOTEM));
			check(HowlingTotemRite.howl(overworld, player, player.getMainHandItem()).missing() == HowlingTotemRite.Missing.MISTING
					&& player.getMainHandItem().getCount() == 1, "A second totem at the risen mist was not refused, or was used up");
			MistGateEntity mist = HowlingTotemRite.mist(overworld, spot);
			check(mist != null, "The mist sank early");
			mist.use(player, InteractionHand.MAIN_HAND);
			check(player.level() == minecraft.getLevel(RING.dimension) && Lairs.instance(player.getAttached(Lairs.VISIT)) == first.get(),
					"The mist did not take the player back into the same ring");
		});
		context.waitTicks(40);

		// 5. Closed, the player goes home and the mist is gone.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			Lairs.close(minecraft, first.get());
			check(player.level() == minecraft.overworld(), "Closing the ring did not send the player home");
		});
		context.waitTicks(50);
		server.runOnServer(minecraft -> {
			ServerLevel overworld = minecraft.overworld();
			check(HowlingTotemRite.mist(overworld, spot) == null
					&& overworld.getEntitiesOfClass(MistGateEntity.class, new AABB(spot).inflate(3)).isEmpty(), "The mist outlived its ring");
			player(minecraft).setHealth(20.0F);
			Lairs.reset();
		});
		context.waitTicks(20);
		Jugcraft.LOGGER.info("[lairs] client game test: the Howling Totem passed end to end");
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
