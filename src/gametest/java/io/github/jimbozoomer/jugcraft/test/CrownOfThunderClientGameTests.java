package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.CrownOfThunder;
import io.github.jimbozoomer.jugcraft.lair.CrownOfThunder.Rod;
import io.github.jimbozoomer.jugcraft.lair.HowlingTotemRite;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.LairRules;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MistGateEntity;
import io.github.jimbozoomer.jugcraft.lair.StormRodBlock;
import io.github.jimbozoomer.jugcraft.lair.ThunderVaneRite;
import java.util.List;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Crown of Thunder and the Thunder Vane (docs/features/crown-of-thunder.md), in a real world,
 * which has the lair dimensions: first the vane from end to end with the one player in survival ({@link #endToEnd}); then
 * pictures: the Grey Mist torn open where the vane was raised, and, on the Crown, the terrace from the arrival, the
 * summit from the head of the terrace's stairs, the north Storm Rod charged, the nest on its crag, the east ledge and its
 * steps, the whole Crown from over the terrace and the Grey Mist in its arch; last, the game's own block light on every
 * open cell of the summit, the ledges and the terrace. CI job {@code client}.
 */
public class CrownOfThunderClientGameTests implements FabricClientGameTest {
	private static final Lair CROWN = Lair.CROWN_OF_THUNDER;
	/**
	 * The least block light on any open cell of the summit, the ledges and the terrace, and how many open cells they have
	 * (tools/crown_of_thunder.py SUMMIT_LIGHT and its open cells, checked by the audit).
	 */
	private static final int SUMMIT_LIGHT = 7;
	private static final int CROWN_CELLS = 1257;
	/** The height the test's mountain pad stands at, and the low pad's: one over the vane's least height, one under it. */
	private static final int HIGH = ThunderVaneRite.MIN_Y + 10;
	private static final int LOW = ThunderVaneRite.MIN_Y - 10;

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
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("difficulty peaceful");
			server.runCommand("weather clear");
			// A stone pad high over the spawn, open to the sky, with a spot under a roof; a lower pad beside it; the land
			// plains for now.
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, LOW - 2, z - 10, x + 12, HIGH + 30, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, HIGH + 31, z - 10, x + 12, HIGH + 90, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, HIGH + 91, z - 10, x + 12, HIGH + 150, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 10, HIGH - 1, z - 10, x + 6, HIGH - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 9, HIGH + 3, z - 9, x - 5, HIGH + 3, z - 5));
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x + 8, LOW - 1, z - 8, x + 12, LOW - 1, z - 4));
			server.runCommand("fillbiome %d %d %d %d %d %d minecraft:plains".formatted(x - 16, LOW - 4, z - 16, x + 16, HIGH + 12, z + 12));
			context.waitTicks(10);

			BlockPos spot = new BlockPos(x + 2, HIGH, z - 3);
			BlockPos low = new BlockPos(x + 10, LOW, z - 6);
			BlockPos roofed = new BlockPos(x - 7, HIGH, z - 7);
			endToEnd(context, server, spot, low, roofed);

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
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.THUNDER_VANE));
					ThunderVaneRite.Raising raising = ThunderVaneRite.raise(overworld, player, player.getMainHandItem());
					check(raising.missing() == null, "The vane opened no Crown for the pictures: " + raising);
					instance.set(raising.instance());
				});
				context.waitTicks(40);
				// The mist on the pad (the camera leaves the Crown by command, which ends the visit; the mist stays).
				shootAt(context, singleplayer, "minecraft:overworld", spot.getX() + 3.5, HIGH, spot.getZ() + 4.5, spot.getX() + 0.5, HIGH + 1.0,
						spot.getZ() + 0.5, "jugcraft_thunder_vane_mist");
				server.runOnServer(minecraft -> check(Lairs.enter(player(minecraft), instance.get()), "The camera could not go back in"));
				context.waitTicks(40);
				singleplayer.getConnection().waitForChunksRender();
				BlockPos o = CROWN.origin(0);
				String dim = CROWN.dimension.identifier().toString();
				int stand = CrownOfThunder.STAND;
				int terrace = CrownOfThunder.TERRACE_TOP + 1;
				shoot(context, singleplayer, dim, o.getX() + 32.5, o.getY() + terrace, o.getZ() + 55.5, 180, -8, "jugcraft_crown_of_thunder_arrival");
				shoot(context, singleplayer, dim, o.getX() + 32.5, o.getY() + stand, o.getZ() + 49.5, 180, -4, "jugcraft_crown_of_thunder_summit");
				// The north rod charged, held so for the picture.
				server.runOnServer(minecraft -> CrownOfThunder.charge(minecraft.getLevel(CROWN.dimension), o, Rod.NORTH, true));
				BlockPos north = Rod.NORTH.base(o);
				shootAt(context, singleplayer, dim, o.getX() + 36.5, o.getY() + stand, o.getZ() + 25.5, north.getX() + 0.5, north.getY() + 1.5,
						north.getZ() + 0.5, "jugcraft_crown_of_thunder_rod_charged");
				server.runOnServer(minecraft -> CrownOfThunder.charge(minecraft.getLevel(CROWN.dimension), o, Rod.NORTH, false));
				shootAt(context, singleplayer, dim, o.getX() + 36.5, o.getY() + stand, o.getZ() + 22.5, o.getX() + CrownOfThunder.WAIT_X,
						o.getY() + CrownOfThunder.WAIT_Y, o.getZ() + CrownOfThunder.WAIT_Z, "jugcraft_crown_of_thunder_nest");
				shootAt(context, singleplayer, dim, o.getX() + 54.0, o.getY() + CrownOfThunder.LEDGE_TOP + 1, o.getZ() + 28.5, o.getX() + 46.5,
						o.getY() + CrownOfThunder.LEDGE_TOP + 3.0, o.getZ() + 36.5, "jugcraft_crown_of_thunder_ledge");
				// From over the terrace (within the Crown's bounds), the whole summit and the clouds below.
				shoot(context, singleplayer, dim, o.getX() + 32.5, o.getY() + stand + 22, o.getZ() + 64.5, 180, 38, "jugcraft_crown_of_thunder_above");
				shoot(context, singleplayer, dim, o.getX() + 32.5, o.getY() + terrace, o.getZ() + 54.5, 0, 0, "jugcraft_crown_of_thunder_exit");

				// The game's own light on every open cell of the summit, the ledges and the terrace, where the Roc fights.
				server.runOnServer(minecraft -> {
					ServerLevel lair = minecraft.getLevel(CROWN.dimension);
					int least = 15;
					int cells = 0;
					BlockPos darkest = null;
					for (int dx = 0; dx < CROWN.width; dx++) {
						for (int dz = 0; dz < CROWN.length; dz++) {
							double cx = dx + 0.5;
							double cz = dz + 0.5;
							int height = CrownOfThunder.onSummit(cx, cz) ? stand : CrownOfThunder.onLedge(cx, cz)
									? CrownOfThunder.LEDGE_TOP + 1 : CrownOfThunder.onTerrace(cx, cz) ? terrace : -1;
							if (height < 0) {
								continue;
							}
							BlockPos cell = o.offset(dx, height, dz);
							if (lair.getBlockState(cell.below()).isAir() || !standable(lair, cell) || !standable(lair, cell.above())) {
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
					Jugcraft.LOGGER.info("[lairs] the Crown of Thunder: {} open cells, the darkest at block light {} ({})", cells, least, darkest);
					check(cells == CROWN_CELLS && least >= SUMMIT_LIGHT, "The Crown has " + cells + " open cells (not " + CROWN_CELLS
							+ "), the darkest at block light " + least + " (" + darkest + "), under " + SUMMIT_LIGHT);
				});
			} finally {
				JugcraftConfig.setTextOption("lairs.gate_seconds", gateSeconds);
				server.runOnServer(minecraft -> Lairs.reset());
				server.runCommand("weather clear");
			}
		}
	}

	/**
	 * The Thunder Vane from end to end, step by step, with the player in survival (the instances are the whole server's,
	 * so this one test owns them):
	 * <ol>
	 * <li>raised in fair weather the vane is refused and kept; in the rain in the plains, the same; the land made the
	 * mountains', a pad below the vane's height and a spot under a roof, the same;</li>
	 * <li>raised by the player's hand in the rain on the open mountain pad, it opens an instance in the Crown's own
	 * dimension, places the Crown fresh (the terrace under the arrival, the Grey Mist in its arch, each Storm Rod whole and
	 * calm on its plinth, the nest under where the Roc waits), is used up and rested, tears the Grey Mist open where it was
	 * raised, and brings the player down on the terrace, unable to build;</li>
	 * <li>on the Crown a Storm Rod is no fixture; charged, it lights the summit round it, and calms again; a player on a
	 * ledge below the summit is where they may be; falling off the Crown throws them back to the terrace for the mist's
	 * toll;</li>
	 * <li>leaving takes the player back to where they stood; while the mist is open a second vane raised there is refused
	 * and kept, and using the mist takes them back onto the same Crown;</li>
	 * <li>when the instance closes the player goes home and the mist is gone.</li>
	 * </ol>
	 */
	private static void endToEnd(ClientGameTestContext context, TestServerContext server, BlockPos spot, BlockPos low, BlockPos roofed) {
		server.runCommand("gamemode survival @a");
		context.waitTicks(5);
		AtomicReference<LairInstance> first = new AtomicReference<>();
		AtomicReference<Vec3> stood = new AtomicReference<>();

		// 1. Refused in fair weather, then in the rain in the plains, then in the mountains too low and under a roof.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.teleportTo(overworld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.THUNDER_VANE, 2));
			check(ThunderVaneRite.check(overworld, spot, true, true) == null, "The pad is no open place over the vane's height: "
					+ ThunderVaneRite.check(overworld, spot, true, true));
			ThunderVaneRite.Raising raising = ThunderVaneRite.raise(overworld, player, player.getMainHandItem());
			check(raising.missing() == ThunderVaneRite.Missing.FAIR && player.getMainHandItem().getCount() == 2,
					"In fair weather, the vane was not refused, or was used up: " + raising + ", " + player.getMainHandItem());
		});
		// The rain takes a few seconds to set in.
		server.runCommand("weather rain");
		context.waitTicks(100);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			check(overworld.isRaining(), "It is not raining");
			ThunderVaneRite.Raising raising = ThunderVaneRite.raise(overworld, player, player.getMainHandItem());
			check(raising.missing() == ThunderVaneRite.Missing.NOT_MOUNTAINS && player.getMainHandItem().getCount() == 2,
					"In the plains, the vane was not refused, or was used up: " + raising + ", " + player.getMainHandItem());
		});
		server.runCommand("fillbiome %d %d %d %d %d %d minecraft:jagged_peaks".formatted(spot.getX() - 18, low.getY() - 4, spot.getZ() - 12,
				spot.getX() + 14, spot.getY() + 12, spot.getZ() + 12));
		context.waitTicks(10);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			check(overworld.getBiome(spot).is(ThunderVaneRite.LANDS) && overworld.getBiome(low).is(ThunderVaneRite.LANDS)
					&& overworld.getBiome(roofed).is(ThunderVaneRite.LANDS), "The land did not become the mountains': "
					+ overworld.getBiome(spot).getRegisteredName());
			player.teleportTo(overworld, low.getX() + 0.5, low.getY(), low.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			ThunderVaneRite.Raising raising = ThunderVaneRite.raise(overworld, player, player.getMainHandItem());
			check(raising.missing() == ThunderVaneRite.Missing.LOW && player.getMainHandItem().getCount() == 2,
					"Below the vane's height, it was not refused, or was used up: " + raising + ", " + player.getMainHandItem());
			player.teleportTo(overworld, roofed.getX() + 0.5, roofed.getY(), roofed.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			raising = ThunderVaneRite.raise(overworld, player, player.getMainHandItem());
			check(raising.missing() == ThunderVaneRite.Missing.ROOFED && player.getMainHandItem().getCount() == 2,
					"Under a roof, the vane was not refused, or was used up: " + raising + ", " + player.getMainHandItem());
			player.teleportTo(overworld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(), 180.0F, 0.0F, true);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.THUNDER_VANE));
		});
		context.waitTicks(50);

		// 2. In the rain on the open mountain pad, the vane opens the Crown and the wind takes the player.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			stood.set(player.position());
			ThunderVaneRite.Missing ready = ThunderVaneRite.check(overworld, HowlingTotemRite.feet(player), overworld.isRaining(),
					overworld.getBiome(spot).is(ThunderVaneRite.LANDS));
			check(ready == null, "The vane could not open the Crown here: " + ready + ", in " + overworld.getBiome(spot).getRegisteredName());
			InteractionResult used = player.gameMode.useItem(player, overworld, player.getMainHandItem(), InteractionHand.MAIN_HAND);
			check(used.consumesAction(), "Raising the vane did not succeed: " + used);
		});
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			LairInstance instance = Lairs.open(CROWN).isEmpty() ? null : Lairs.open(CROWN).getFirst();
			check(instance != null, "The vane opened no instance");
			first.set(instance);
			check(player.getMainHandItem().isEmpty(), "The vane was not used up: " + player.getMainHandItem());
			check(player.getCooldowns().isOnCooldown(new ItemStack(JugcraftLairs.THUNDER_VANE)), "The vane was not rested after its use");
			ServerLevel lair = minecraft.getLevel(CROWN.dimension);
			check(lair != null && lair.getMinY() == 0, "The Crown of Thunder's dimension: from y 0");
			BlockPos o = instance.origin();
			BlockPos underArrival = BlockPos.containing(CROWN.arrival).below().offset(o);
			check(lair.getBlockState(underArrival).isFaceSturdy(lair, underArrival, Direction.UP), "No terrace under the arrival");
			for (BlockPos mist : CrownOfThunder.exitCells()) {
				check(lair.getBlockState(o.offset(mist)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist in the terrace's arch at " + mist);
			}
			for (Rod rod : Rod.values()) {
				List<BlockPos> parts = rod.parts(o);
				for (int i = 0; i < parts.size(); i++) {
					BlockState state = lair.getBlockState(parts.get(i));
					check(state.is(JugcraftLairs.STORM_ROD) && state.getValue(StormRodBlock.PART) == StormRodBlock.Part.values()[i]
							&& !state.getValue(StormRodBlock.CHARGED), "No calm " + StormRodBlock.Part.values()[i] + " of the " + rod.title()
							+ " rod at " + parts.get(i).subtract(o) + ": " + state);
				}
				check(lair.getBlockState(rod.base(o).below()).is(Blocks.CHISELED_STONE_BRICKS), "The " + rod.title() + " rod stands on no plinth");
			}
			check(CrownOfThunder.charged(lair, o) == null, "A rod was charged before the fight");
			BlockPos nest = BlockPos.containing(CrownOfThunder.waiting()).below().offset(o);
			check(lair.getBlockState(nest).is(JugcraftLairs.ROC_NEST), "No nest under where the Roc waits");
			check(player.level() == lair && player.position().distanceTo(instance.arrival()) < 0.5,
					"The player did not come down on the terrace: " + player.level().dimension() + " " + player.position());
			check(!player.getAbilities().mayBuild, "In a lair a player cannot build");
			MistGateEntity mist = ThunderVaneRite.mist(overworld, spot);
			check(mist != null && mist.instance() == instance && mist.lair() == CROWN, "No mist torn open where the vane was raised");
		});
		context.waitTicks(40);

		// 3. A rod is no fixture; charged, it lights the summit; a ledge is in bounds; falling off throws the player back.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(CROWN.dimension);
			BlockPos o = first.get().origin();
			BlockPos tip = Rod.EAST.parts(o).getLast();
			check(LairRules.useBlock(player, lair, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(tip), Direction.WEST, tip, false))
					== InteractionResult.FAIL, "A Storm Rod can be used");
			check(CrownOfThunder.charge(lair, o, Rod.EAST, true) && CrownOfThunder.charged(lair, o) == Rod.EAST, "The east rod did not charge");
		});
		context.waitTicks(10);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel lair = minecraft.getLevel(CROWN.dimension);
			BlockPos o = first.get().origin();
			BlockPos tip = Rod.EAST.parts(o).getLast();
			int light = lair.getBrightness(LightLayer.BLOCK, tip);
			check(lair.getBlockState(tip).getLightEmission() == StormRodBlock.Part.TIP.light && light >= StormRodBlock.Part.TIP.light,
					"The charged rod's tip burns at light " + light + ", not " + StormRodBlock.Part.TIP.light);
			check(CrownOfThunder.charge(lair, o, Rod.EAST, false) && CrownOfThunder.charged(lair, o) == null, "The east rod did not calm");
			player.teleportTo(o.getX() + CrownOfThunder.CENTRE_X + 20.5, o.getY() + CrownOfThunder.LEDGE_TOP + 1, o.getZ() + CrownOfThunder.CENTRE_Z);
			Vec3 onLedge = player.position();
			check(!Lairs.edges(player, first.get()) && player.position().distanceTo(onLedge) < 1.0E-6, "A player on the east ledge was thrown back");
			Vec3 arrival = first.get().arrival();
			player.teleportTo(o.getX() + CrownOfThunder.CENTRE_X, o.getY() + CROWN.floor - 2.0, o.getZ() + CrownOfThunder.CENTRE_Z);
			player.setHealth(20.0F);
			check(Lairs.edges(player, first.get()) && player.position().distanceTo(arrival) < 0.5
					&& player.getHealth() == 20.0F - Lairs.EDGE_DAMAGE, "Falling off the Crown did not end on the terrace");
			player.setHealth(20.0F);
		});

		// 4. Out, a second vane refused at the open mist, and back in through the mist.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			check(Lairs.leave(player) && player.level() == minecraft.overworld() && player.position().distanceTo(stood.get()) < 1.0E-6,
					"The player did not come back to where they stood: " + player.position());
		});
		context.waitTicks(40);
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			ServerLevel overworld = minecraft.overworld();
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.THUNDER_VANE));
			check(ThunderVaneRite.raise(overworld, player, player.getMainHandItem()).missing() == ThunderVaneRite.Missing.MISTING
					&& player.getMainHandItem().getCount() == 1, "A second vane at the open mist was not refused, or was used up");
			MistGateEntity mist = ThunderVaneRite.mist(overworld, spot);
			check(mist != null, "The mist closed early");
			mist.use(player, InteractionHand.MAIN_HAND);
			check(player.level() == minecraft.getLevel(CROWN.dimension) && Lairs.instance(player.getAttached(Lairs.VISIT)) == first.get(),
					"The mist did not take the player back onto the same Crown");
		});
		context.waitTicks(40);

		// 5. Closed, the player goes home and the mist is gone.
		server.runOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			Lairs.close(minecraft, first.get());
			check(player.level() == minecraft.overworld(), "Closing the Crown did not send the player home");
		});
		context.waitTicks(50);
		server.runOnServer(minecraft -> {
			ServerLevel overworld = minecraft.overworld();
			check(ThunderVaneRite.mist(overworld, spot) == null
					&& overworld.getEntitiesOfClass(MistGateEntity.class, new AABB(spot).inflate(3)).isEmpty(), "The mist outlived its Crown");
			player(minecraft).setHealth(20.0F);
			Lairs.reset();
		});
		context.waitTicks(20);
		Jugcraft.LOGGER.info("[lairs] client game test: the Thunder Vane passed end to end");
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
