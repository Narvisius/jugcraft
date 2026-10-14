package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.FenLampBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.hag.HagLikenessEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.HagRootsEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.JugcraftHag;
import io.github.jimbozoomer.jugcraft.lair.hag.LanternFen;
import io.github.jimbozoomer.jugcraft.lair.hag.MireGraspEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.MireHagEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.MireHagLoot;
import io.github.jimbozoomer.jugcraft.lair.hag.PoisonBrewEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.RootKnotEntity;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Mire Hag (docs/features/mire-hag.md), in a real Lantern Fen: unlike a game-test server's world,
 * it has the lair dimension. Her fight from end to end, with the one player in survival (shielded by Resistance, so it can
 * watch every attack): she waits on her porch over her cauldron; coming into the marsh wakes her and she steps down into
 * the water, her Likenesses forming round her; the Lure's attacks; a lamp's light finds her; Lights Out puts out two
 * lamps and sends the Root Knot crawling on a third; the Drowned Dark, more Likenesses, the Mire Grasp and the Snuff; the
 * Last Light, her lantern flaring; and when she falls the player has their loot and Seen Through, her Likenesses are gone
 * and Grey Mist stands in the marsh's middle, and a fresh instance in her slot has none of it and her on her porch again.
 * Along the way, pictures of each part of the fight, the world frozen for each so the pose holds. CI job {@code client}.
 */
public class MireHagClientGameTests implements FabricClientGameTest {
	private static final Lair FEN = Lair.LANTERN_FEN;
	/** A standing player's eyes over their feet, where the camera looks from. */
	private static final double EYE = 1.62;
	/**
	 * How long, once she has come down, her Likenesses may take to form: she has none until there is a place for one out
	 * of the lamps' light, and looks again every second. Far short of the 10 seconds a faded one takes to form again.
	 */
	private static final int LIKENESSES_FORM = 60;

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
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("difficulty normal");  // she is a monster: in peaceful she would be gone

			// An instance opens: the fen is placed and she waits on her porch over her cauldron.
			server.runOnServer(minecraft -> {
				LairInstance instance = Lairs.open(minecraft, FEN);
				check(instance != null, "No Lantern Fen opened");
				check(Lairs.enter(player(minecraft), instance), "The player did not go in");
			});
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos o = FEN.origin(0);
			String fen = FEN.dimension.identifier().toString();
			Vec3 centre = MireHagEntity.centre(o);
			server.runOnServer(minecraft -> {
				MireHagEntity hag = hag(minecraft);
				check(hag.phase() == MireHagEntity.Phase.WAITING, "She is not waiting but " + hag.phase());
				check(hag.position().distanceTo(hag.waiting()) < 0.1, "She is not on her porch but at " + hag.position());
			});
			shot(context, singleplayer, fen, o, 43.0, LanternFen.STAND, 25.0, LanternFen.WAIT_X, LanternFen.WAIT_Y + 1.4, LanternFen.WAIT_Z, false,
					"jugcraft_mire_hag_waiting");

			// Coming into the marsh wakes her: the player, in survival in the marsh, and she looks up from her cauldron and
			// steps down into the water; caught as she steps.
			server.runCommand("effect give @a minecraft:resistance infinite 4 true");
			server.runCommand("effect give @a minecraft:saturation infinite 0 true");
			server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.1f %.3f %.1f 180 0", fen, centre.x + 2.5, centre.y, centre.z - 8.0));
			server.runCommand("gamemode survival @a");
			context.waitTicks(30);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> check(hag(minecraft).phase() == MireHagEntity.Phase.WAKING,
					"Coming into the marsh did not wake her: " + hag(minecraft).phase()));
			shot(context, singleplayer, fen, o, 46.0, LanternFen.STAND + 1.0, 28.0, 41.5, LanternFen.WAIT_Y, 21.0, true, "jugcraft_mire_hag_wakes");
			// The camera goes back into the dark middle of the marsh, out of the lamps' light, for her Likenesses to walk round.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> player(minecraft).teleportTo(centre.x + 0.5, centre.y, centre.z + 5.0));
			context.waitTicks(MireHagEntity.WAKE_TICKS + 10);
			// Her Likenesses form as soon as there is a place for one out of the lamps' light (she looks again every second):
			// a second or so. One that had walked into a lamp's light would not form again for 10 seconds.
			for (int wait = 0; wait < LIKENESSES_FORM && server.computeOnServer(minecraft -> hag(minecraft).likenesses().size())
					< MireHagEntity.LIKENESSES; wait++) {
				context.waitTick();
			}
			server.runOnServer(minecraft -> {
				MireHagEntity hag = hag(minecraft);
				check(hag.phase() == MireHagEntity.Phase.LURE, "She did not come down to fight: " + hag.phase());
				check(LanternFen.fromCentre(hag.getX() - o.getX(), hag.getZ() - o.getZ()) <= LanternFen.MARSH_RADIUS,
						"She is not in the marsh but at " + hag.position());
				check(hag.likenesses().size() == MireHagEntity.LIKENESSES, hag.likenesses().size() + " Likenesses walk with her, not "
						+ MireHagEntity.LIKENESSES);
			});
			// She and her Likenesses, from behind the player: which is she? She is put in the marsh's middle with the player
			// five blocks in front of her, as for her attacks, and each Likeness at its place round the player.
			pose(context, server, centre, (hag, level, player) -> {
				for (UUID id : hag.likenesses()) {
					if (level.getEntity(id) instanceof HagLikenessEntity likeness) {
						Vec3 place = hag.likenessSpot(level, likeness.slot());
						if (place != null) {
							likeness.teleportTo(place.x, place.y, place.z);
						}
					}
				}
			}, 10);
			shotFrom(context, singleplayer, fen, centre, 0.5, 3.0, 10.0, 0.0, 1.2, 0.0, "jugcraft_mire_hag_likenesses");

			// The Lure, in the marsh: each attack caught as it lands, she in the marsh's middle facing south, the player five
			// blocks in front of her; the camera then stands where it shows the attack best, turned towards the point named.
			pose(context, server, centre, (hag, level, player) -> hag.begin(level, MireHagEntity.Attack.CLAWING_SWIPE, player),
					MireHagEntity.Attack.CLAWING_SWIPE.windup + 2);
			shotFrom(context, singleplayer, fen, centre, 4.5, 1.5, 3.0, 0.0, 1.6, 1.0, "jugcraft_mire_hag_swipe");
			pose(context, server, centre, (hag, level, player) -> hag.begin(level, MireHagEntity.Attack.ROOT_LANES, player),
					MireHagEntity.Attack.ROOT_LANES.windup + 6);
			server.runOnServer(minecraft -> {
				int roots = lair(minecraft).getEntitiesOfClass(HagRootsEntity.class, slot()).size();
				check(roots >= MireHagEntity.LANES * 4, "Only " + roots + " clumps of roots burst along her lanes");
			});
			shotFrom(context, singleplayer, fen, centre, -2.5, 4.0, -5.0, 0.0, 0.0, 6.0, "jugcraft_mire_hag_root_lanes");
			pose(context, server, centre, (hag, level, player) -> hag.begin(level, MireHagEntity.Attack.POISON_BREW, player),
					MireHagEntity.Attack.POISON_BREW.windup - 4);
			// Her flask is in the air for under half a second, and the server can run a few ticks ahead of the client's
			// count: the world runs on until a flask has flown a few ticks, and the server freezes it in the step that sees it.
			server.runCommand("tick unfreeze");
			boolean flying = false;
			for (int wait = 0; wait < 40 && !flying; wait++) {
				context.waitTick();
				flying = server.computeOnServer(minecraft -> {
					boolean thrown = lair(minecraft).getEntitiesOfClass(PoisonBrewEntity.class, slot()).stream().anyMatch(brew -> brew.tickCount >= 3);
					if (thrown) {
						minecraft.tickRateManager().setFrozen(true);
					}
					return thrown;
				});
			}
			check(flying, "No flask of her brew is flying");
			shotFrom(context, singleplayer, fen, centre, 5.5, 2.0, 2.5, 0.0, 2.0, 2.5, "jugcraft_mire_hag_poison_brew");
			server.runCommand("tick unfreeze");
			context.waitTicks(20);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> check(!hag(minecraft).pools().isEmpty(), "Her brew left no pool of venom"));
			shotFrom(context, singleplayer, fen, centre, 3.0, 2.5, 8.0, 0.0, 0.0, 5.0, "jugcraft_mire_hag_venom_pool");
			pose(context, server, centre, (hag, level, player) -> hag.begin(level, MireHagEntity.Attack.LIKENESS_EXCHANGE, player),
					MireHagEntity.Attack.LIKENESS_EXCHANGE.windup - 12);
			shotFrom(context, singleplayer, fen, centre, 3.0, 1.0, 6.5, 0.0, 1.5, 0.0, "jugcraft_mire_hag_exchange");
			// The Exchange is caught in its wind-up: the world runs on until it is done (she has traded places), so it does not
			// move her out of the next pose. She then rests a second before she can strike again.
			server.runCommand("tick unfreeze");
			boolean done = false;
			for (int wait = 0; wait < 60 && !done; wait++) {
				context.waitTick();
				done = server.computeOnServer(minecraft -> {
					boolean over = hag(minecraft).attack() == null;
					if (over) {
						minecraft.tickRateManager().setFrozen(true);
					}
					return over;
				});
			}
			check(done, "Her Likeness Exchange never ended");

			// Her rule: in a lamp's light her outline shows, wherever her Likenesses walk.
			BlockPos northEast = o.offset(LanternFen.LAMPS.get(1));
			pose(context, server, centre, (hag, level, player) -> {
				hag.teleportTo(northEast.getX() - 2.0, centre.y, northEast.getZ() + 3.0);
				player.teleportTo(northEast.getX() - 6.0, centre.y, northEast.getZ() + 6.0);
			}, 12);
			server.runOnServer(minecraft -> check(hag(minecraft).revealed() && hag(minecraft).hasEffect(net.minecraft.world.effect.MobEffects.GLOWING),
					"In a lamp's light she does not glow"));
			Vec3 lampSide = new Vec3(northEast.getX() - 7.5, centre.y + 2.5, northEast.getZ() + 9.0);
			Vec3 lampAt = new Vec3(northEast.getX() - 1.5, centre.y + 1.5, northEast.getZ() + 2.5);
			shoot(context, singleplayer, fen, lampSide.x, lampSide.y, lampSide.z, aimYaw(lampSide, lampAt), aimPitch(lampSide, lampAt), true,
					"jugcraft_mire_hag_lamplight");

			// Lights Out, past half her health: she cackles, the two lamps nearest the player go out, and the Root Knot bursts
			// from the ring and crawls on a third.
			pose(context, server, centre, (hag, level, player) -> {
				hag.setHealth(hag.getMaxHealth() * 0.5F);
				hag.lightsOut(level, player);
			}, 14);
			server.runOnServer(minecraft -> {
				MireHagEntity hag = hag(minecraft);
				ServerLevel level = lair(minecraft);
				check(hag.phase() == MireHagEntity.Phase.DARKENING, "Lights Out did not begin: " + hag.phase());
				long out = hag.lamps().stream().filter(lamp -> !MireHagEntity.burning(level, lamp)).count();
				check(out == MireHagEntity.DARK_SNUFFS, out + " lamps went out, not " + MireHagEntity.DARK_SNUFFS);
				check(hag.knot() != null && level.getEntity(hag.knot()) instanceof RootKnotEntity crawling && crawling.lamp() != null
						&& MireHagEntity.burning(level, crawling.lamp()), "No Root Knot crawls on a lamp still burning");
			});
			shotFrom(context, singleplayer, fen, centre, 0.0, 1.5, 10.0, 0.0, 2.0, 0.0, "jugcraft_mire_hag_lights_out");
			server.runCommand("tick unfreeze");
			context.waitTicks(30);
			server.runCommand("tick freeze");
			AtomicReference<Vec3> knotAt = new AtomicReference<>();
			server.runOnServer(minecraft -> {
				MireHagEntity hag = hag(minecraft);
				check(hag.knot() != null && lair(minecraft).getEntity(hag.knot()) instanceof RootKnotEntity, "The Root Knot is gone");
				knotAt.set(lair(minecraft).getEntity(hag.knot()).position());
			});
			Vec3 knot = knotAt.get();
			Vec3 outward = new Vec3(knot.x - centre.x, 0.0, knot.z - centre.z).normalize();
			Vec3 overKnot = knot.add(outward.scale(-4.5)).add(0.0, 2.5, 0.0);
			Vec3 knotLook = knot.add(0.0, 0.4, 0.0);
			shoot(context, singleplayer, fen, overKnot.x, overKnot.y, overKnot.z, aimYaw(overKnot, knotLook), aimPitch(overKnot, knotLook), true,
					"jugcraft_mire_hag_root_knot");

			// The Drowned Dark: up to four Likenesses, and her Mire Grasp; the Snuff puts out the lamp burning nearest her.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> player(minecraft).teleportTo(centre.x + 0.5, centre.y, centre.z + 5.0));
			context.waitTicks(MireHagEntity.DARK_TICKS);
			server.runOnServer(minecraft -> {
				MireHagEntity hag = hag(minecraft);
				check(hag.phase() == MireHagEntity.Phase.DROWNED, "The Drowned Dark did not begin: " + hag.phase());
				check(hag.likenessCap() == MireHagEntity.DARK_LIKENESSES
						&& hag.likenesses().size() + hag.returns().size() == MireHagEntity.DARK_LIKENESSES,
						"In the Drowned Dark " + hag.likenesses().size() + " Likenesses walk and " + hag.returns().size() + " will, not "
								+ MireHagEntity.DARK_LIKENESSES);
			});
			pose(context, server, centre, (hag, level, player) -> hag.begin(level, MireHagEntity.Attack.MIRE_GRASP, player),
					MireHagEntity.Attack.MIRE_GRASP.windup + 8);
			server.runOnServer(minecraft -> check(!lair(minecraft).getEntitiesOfClass(MireGraspEntity.class, slot()).isEmpty(),
					"No hands of mud rose round the player"));
			shotFrom(context, singleplayer, fen, centre, 4.0, 2.0, 9.0, 0.0, 0.5, 5.0, "jugcraft_mire_hag_mire_grasp");
			AtomicReference<BlockPos> snuffed = new AtomicReference<>();
			pose(context, server, centre, (hag, level, player) -> {
				// Whatever she or her knot has put out since, every lamp burns for this Snuff.
				hag.lamps().forEach(lamp -> FenLampBlock.light(level, lamp));
				snuffed.set(hag.nearestLitLamp(level, hag.position()));
				hag.begin(level, MireHagEntity.Attack.SNUFF, player);
			}, MireHagEntity.Attack.SNUFF.windup + 3);
			server.runOnServer(minecraft -> check(snuffed.get() != null && !MireHagEntity.burning(lair(minecraft), snuffed.get()),
					"The Snuff did not put out the lamp burning nearest her"));
			shotFrom(context, singleplayer, fen, centre, -7.0, 2.0, 7.0, 0.0, 1.5, 0.0, "jugcraft_mire_hag_drowned_dark");

			// The Last Light, below a fifth of her health: her lantern flares, her Likenesses burst, and she glows.
			pose(context, server, centre, (hag, level, player) -> {
				hag.setHealth(hag.getMaxHealth() * 0.15F);
				hag.lastLight(level);
			}, 14);
			server.runOnServer(minecraft -> {
				MireHagEntity hag = hag(minecraft);
				check(hag.phase() == MireHagEntity.Phase.LAST && hag.lantern() == MireHagEntity.Lantern.FLARE, "The Last Light did not come");
				check(lair(minecraft).getEntitiesOfClass(HagLikenessEntity.class, slot()).isEmpty(), "A Likeness outlived the Last Light");
			});
			shotFrom(context, singleplayer, fen, centre, 2.5, 0.5, 4.5, 0.0, 2.0, 0.0, "jugcraft_mire_hag_last_light");

			// She falls: the player's loot and advancement, her Likenesses and things gone with her, and Grey Mist in the
			// marsh's middle. Caught as she falls.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				MireHagEntity hag = hag(minecraft);
				check(MireHagLoot.firstKill(player(minecraft)), "The player had already won Seen Through");
				hag.took(player(minecraft), 10.0F);
				hag.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			});
			context.waitTicks(6);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				ServerPlayer player = player(minecraft);
				ServerLevel level = lair(minecraft);
				check(player.getInventory().countItem(JugcraftHag.HAGMOSS) >= 3, "No Hagmoss");
				int trophies = player.getInventory().countItem(JugcraftHag.hagthorn()) + player.getInventory().countItem(JugcraftHag.bogfang());
				check(trophies == 1, "A first kill must bring one of her trophies, not " + trophies);
				check(!MireHagLoot.firstKill(player), "Seen Through was not awarded");
				for (BlockPos cell : LanternFen.exitCells()) {
					check(level.getBlockState(cell.offset(o)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist in the marsh's middle at " + cell);
				}
				check(level.getEntitiesOfClass(HagLikenessEntity.class, slot()).isEmpty(), "Her Likenesses outlived her");
				check(level.getEntitiesOfClass(RootKnotEntity.class, slot()).isEmpty(), "Her Root Knot outlived her");
			});
			shotFrom(context, singleplayer, fen, centre, 3.0, 1.0, 7.5, 0.0, 1.0, 0.0, "jugcraft_mire_hag_defeated");
			server.runCommand("tick unfreeze");
			context.waitTicks(30);
			server.runOnServer(minecraft -> check(lair(minecraft).getEntitiesOfClass(MireHagEntity.class, slot()).stream()
					.noneMatch(MireHagEntity::isAlive), "She is still there"));

			// A fresh instance in her slot is placed anew: the Grey Mist she left is gone with the old one, every lamp burns,
			// and, once the player has gone in again (onto the jetty, far from her), she waits on her porch again.
			server.runOnServer(minecraft -> {
				LairInstance used = Lairs.instance(FEN, 0);
				check(used != null, "Her instance is not open");
				Lairs.close(minecraft, used);
				LairInstance fresh = Lairs.open(minecraft, FEN);
				check(fresh != null && fresh.slot == 0, "No fresh instance opened in her slot");
				ServerLevel level = lair(minecraft);
				for (BlockPos cell : LanternFen.exitCells()) {
					check(!level.getBlockState(cell.offset(o)).is(JugcraftLairs.LAIR_EXIT), "Her Grey Mist outlived her instance at " + cell);
				}
				for (BlockPos lamp : LanternFen.LAMPS) {
					check(level.getBlockState(lamp.offset(o)).getValue(FenLampBlock.LIT), "A lamp of the fresh fen is out at " + lamp);
				}
				check(Lairs.enter(player(minecraft), fresh), "The player did not go into the fresh fen");
			});
			context.waitTicks(40);
			server.runOnServer(minecraft -> {
				MireHagEntity again = hag(minecraft);
				check(again.phase() == MireHagEntity.Phase.WAITING && again.position().distanceTo(again.waiting()) < 0.1,
						"She does not wait on the fresh fen's porch: " + again.phase() + " at " + again.position());
			});
			System.out.println("[hag] client game test: her fight passed end to end");
			server.runOnServer(minecraft -> Lairs.reset());
		}
	}

	@FunctionalInterface
	private interface Posing {
		void pose(MireHagEntity hag, ServerLevel level, ServerPlayer player);
	}

	/**
	 * Unfreezes the world, puts her in the marsh's middle facing south, the player five blocks in front of her (so her
	 * attacks land in view) and her Likenesses behind her, does what {@code posing} asks, waits {@code ticks} for the pose
	 * and freezes the world on it.
	 * Every pose waits at least 10 ticks: the client learns where she was put only on the server's next updates, and a
	 * frozen world sends none.
	 */
	private static void pose(ClientGameTestContext context, TestServerContext server, Vec3 centre, Posing posing, int ticks) {
		server.runCommand("tick unfreeze");
		server.runOnServer(minecraft -> {
			ServerLevel level = lair(minecraft);
			MireHagEntity hag = hag(minecraft);
			ServerPlayer player = player(minecraft);
			player.teleportTo(centre.x + 0.5, centre.y, centre.z + 5.0);
			hag.teleportTo(centre.x, centre.y, centre.z);
			hag.setYRot(0.0F);
			hag.setYBodyRot(0.0F);
			hag.setYHeadRot(0.0F);
			// Her Likenesses stand behind her in the dark, out of the camera's way; while she strikes they hold still there.
			List<UUID> walking = hag.likenesses();
			for (int i = 0; i < walking.size(); i++) {
				if (level.getEntity(walking.get(i)) instanceof HagLikenessEntity likeness) {
					double side = i % 2 == 0 ? 1.0 : -1.0;
					likeness.teleportTo(centre.x + side * (i < 2 ? 3.0 : 5.5), centre.y, centre.z - (i < 2 ? 3.5 : 1.5));
				}
			}
			posing.pose(hag, level, player);
		});
		context.waitTicks(Math.max(10, ticks));
		server.runCommand("tick freeze");
	}

	/**
	 * A picture from ({@code dx}, {@code up}, {@code dz}) off the marsh's middle (the camera's feet), turned to look at
	 * ({@code ax}, {@code ay}, {@code az}) off it, the world frozen.
	 */
	private static void shotFrom(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, Vec3 centre, double dx,
			double up, double dz, double ax, double ay, double az, String name) {
		Vec3 camera = centre.add(dx, up, dz);
		Vec3 at = centre.add(ax, ay, az);
		shoot(context, singleplayer, dimension, camera.x, camera.y, camera.z, aimYaw(camera, at), aimPitch(camera, at), true, name);
	}

	/** A picture from (x, y, z) in the fen's own coordinates (the camera's feet), turned to look at (ax, ay, az) in them. */
	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, BlockPos o, double x,
			double y, double z, double ax, double ay, double az, boolean frozen, String name) {
		Vec3 camera = new Vec3(o.getX() + x, o.getY() + y, o.getZ() + z);
		Vec3 at = new Vec3(o.getX() + ax, o.getY() + ay, o.getZ() + az);
		shoot(context, singleplayer, dimension, camera.x, camera.y, camera.z, aimYaw(camera, at), aimPitch(camera, at), frozen, name);
	}

	/** The yaw that turns a player standing at {@code feet} towards {@code at}. */
	private static int aimYaw(Vec3 feet, Vec3 at) {
		return (int) Math.round(Math.toDegrees(Math.atan2(-(at.x - feet.x), at.z - feet.z)));
	}

	/** The pitch that turns the eyes of a player standing at {@code feet} towards {@code at} (down is positive). */
	private static int aimPitch(Vec3 feet, Vec3 at) {
		double flat = Math.hypot(at.x - feet.x, at.z - feet.z);
		return (int) Math.round(Math.toDegrees(-Math.atan2(at.y - (feet.y + EYE), flat)));
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().get(0);
	}

	private static ServerLevel lair(MinecraftServer minecraft) {
		return minecraft.getLevel(FEN.dimension);
	}

	/** The first slot's fen, with room round it. */
	private static AABB slot() {
		BlockPos o = FEN.origin(0);
		return new AABB(o.getX() - 8, o.getY() - 16, o.getZ() - 8, o.getX() + FEN.width + 8, o.getY() + FEN.height + 8, o.getZ() + FEN.length + 8);
	}

	/** She, alive, in the first slot's fen. */
	private static MireHagEntity hag(MinecraftServer minecraft) {
		List<MireHagEntity> found = lair(minecraft).getEntitiesOfClass(MireHagEntity.class, slot(), MireHagEntity::isAlive);
		check(!found.isEmpty(), "The Mire Hag is not in her Lantern Fen");
		return found.getFirst();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	/**
	 * Stands the camera at (x, y, z) in {@code dimension}, looking along yaw and pitch, waits for the world to draw, and
	 * shoots.
	 */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, double x, double y,
			double z, int yaw, int pitch, boolean frozen, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runOnServer(minecraft -> {
			// Flying, so the camera stays where it is put (set on the server, which tells the client).
			ServerPlayer player = player(minecraft);
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		});
		server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.2f %.2f %.2f %d %d", dimension, x, y, z, yaw, pitch));
		context.waitTicks(frozen ? 20 : 30);
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(10);
		context.takeScreenshot(name);
	}
}
