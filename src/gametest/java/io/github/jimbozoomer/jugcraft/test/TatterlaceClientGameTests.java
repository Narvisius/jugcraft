package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.JugcraftTatterlace;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.SpiderlingEntity;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.SpindleLoft;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.TatterEggSacEntity;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.TatterlaceEntity;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.TatterlaceLoot;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for Madame Tatterlace (docs/features/tatterlace.md), in a real Spindle Loft: unlike a game-test
 * server's world, it has the lair dimension. Her fight from end to end, with the one player in survival (shielded by
 * Resistance, so it can watch every attack): she waits on her silk over the doily, stepping onto the lace brings her
 * down, Taking In the Seams spits her egg sacs round the doily and takes her up into the threads, Unravel drops a
 * segment of the doily away from under the player, her Brood comes out of the sacs, she is frenzied at the last, and
 * when she falls the player has their loot and Unravelled, the doily is whole again and Grey Mist stands in its middle,
 * and a fresh instance in her slot no longer has it. Along the way, pictures of each part of the fight, the world frozen
 * for each so the pose holds. CI job {@code client}.
 */
public class TatterlaceClientGameTests implements FabricClientGameTest {
	private static final Lair LOFT = Lair.SPINDLE_LOFT;
	/** A standing player's eyes over their feet, where the camera looks from. */
	private static final double EYE = 1.62;
	/** The lace cell (template position) where the player stands in front of her, which Unravel takes from under them. */
	private static final BlockPos UNDERFOOT = new BlockPos(41, SpindleLoft.LACE, 41);

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
			server.runCommand("time set midnight");

			// An instance opens: the loft is placed and she waits, sewing, on her silk over the doily.
			server.runOnServer(minecraft -> {
				LairInstance instance = Lairs.open(minecraft, LOFT);
				check(instance != null, "No Spindle Loft opened");
				check(Lairs.enter(player(minecraft), instance), "The player did not go in");
			});
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos o = LOFT.origin(0);
			String loft = LOFT.dimension.identifier().toString();
			Vec3 centre = TatterlaceEntity.centre(o);
			server.runOnServer(minecraft -> {
				TatterlaceEntity tatterlace = tatterlace(minecraft);
				check(tatterlace.phase() == TatterlaceEntity.Phase.WAITING, "She is not waiting but " + tatterlace.phase());
				check(tatterlace.position().distanceTo(tatterlace.perch()) < 0.1, "She is not on her silk but at " + tatterlace.position());
			});
			double perch = TatterlaceEntity.PERCH - SpindleLoft.LACE_TOP;
			shot(context, singleplayer, loft, centre, 0.0, perch + 1.0, 5.5, 0.0, perch + 0.5, 0.0, false, "jugcraft_tatterlace_waiting");

			// Stepping onto the doily brings her down: the player, in survival on the lace south of her, and she lowers
			// herself on her thread; caught on the way down.
			server.runCommand("effect give @a minecraft:resistance infinite 4 true");
			server.runCommand("effect give @a minecraft:saturation infinite 0 true");
			server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.1f %.2f %.1f 180 0", loft, centre.x + 0.5, centre.y, centre.z + 8.0));
			server.runCommand("gamemode survival @a");
			context.waitTicks(25);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> check(tatterlace(minecraft).phase() == TatterlaceEntity.Phase.DESCENDING,
					"Stepping onto the doily did not bring her down: " + tatterlace(minecraft).phase()));
			shot(context, singleplayer, loft, centre, 2.5, 4.0, 7.0, 0.0, perch / 2.0 + 0.5, 0.0, true, "jugcraft_tatterlace_descending");
			server.runCommand("tick unfreeze");
			context.waitTicks(TatterlaceEntity.DESCEND_TICKS);
			server.runOnServer(minecraft -> {
				TatterlaceEntity tatterlace = tatterlace(minecraft);
				check(tatterlace.phase() == TatterlaceEntity.Phase.FITTING, "She did not come down to fight: " + tatterlace.phase());
				check(Math.abs(tatterlace.getY() - tatterlace.floorY()) < 0.1, "She is not on her lace but at " + tatterlace.getY());
			});

			// The Fitting, on the lace: each attack caught as it lands, she in the middle of the doily facing south, the
			// player five blocks in front of her; the camera then stands where it shows the attack best, turned towards
			// the point named.
			pose(context, server, centre, (tatterlace, level, player) -> tatterlace.begin(level, TatterlaceEntity.Attack.NEEDLEPOINT, player),
					TatterlaceEntity.Attack.NEEDLEPOINT.windup + 2);
			shot(context, singleplayer, loft, centre, -4.5, 0.5, 4.0, 0.0, 1.0, 1.0, true, "jugcraft_tatterlace_needlepoint");
			pose(context, server, centre, (tatterlace, level, player) -> tatterlace.begin(level, TatterlaceEntity.Attack.THIMBLE_TOSS, player),
					TatterlaceEntity.Attack.THIMBLE_TOSS.windup + 5);
			shot(context, singleplayer, loft, centre, 6.5, 1.0, 2.5, 0.0, 1.5, 2.5, true, "jugcraft_tatterlace_thimble_toss");
			pose(context, server, centre, (tatterlace, level, player) -> tatterlace.begin(level, TatterlaceEntity.Attack.LACE_SNARE, player),
					TatterlaceEntity.Attack.LACE_SNARE.windup + 18);
			shot(context, singleplayer, loft, centre, -3.0, 3.0, 10.0, 0.5, 0.0, 4.0, true, "jugcraft_tatterlace_lace_snare");
			pose(context, server, centre, (tatterlace, level, player) -> tatterlace.begin(level, TatterlaceEntity.Attack.SPOOL_ROLL, player),
					TatterlaceEntity.Attack.SPOOL_ROLL.windup + 3);
			shot(context, singleplayer, loft, centre, 3.0, 1.0, 10.0, 0.0, 0.8, 2.5, true, "jugcraft_tatterlace_spool_roll");

			// Taking In the Seams, at half her health: she climbs into the threads, the light dims and she spits her egg
			// sacs round the doily.
			pose(context, server, centre, (tatterlace, level, player) -> {
				tatterlace.setHealth(tatterlace.getMaxHealth() * 0.5F);
				tatterlace.startTakingIn(level);
			}, TatterlaceEntity.TAKE_IN_TICKS / 2);
			server.runCommand("effect clear @a minecraft:darkness");
			server.runOnServer(minecraft -> {
				TatterlaceEntity tatterlace = tatterlace(minecraft);
				check(tatterlace.phase() == TatterlaceEntity.Phase.TAKING_IN, "She is not taking in her seams but " + tatterlace.phase());
				check(tatterlace.sacs(lair(minecraft)).size() == TatterlaceEntity.EGG_SACS, "She did not spit her egg sacs round the doily");
			});
			// From just past the doily's rim, over her south-eastern egg sac, towards her climbing in the middle.
			Vec3 sac = SpindleLoft.sacs().get(1).add(o.getX(), o.getY(), o.getZ());
			fromTheRim(context, singleplayer, loft, centre, sac, "jugcraft_tatterlace_taking_in");
			// Checked as she reaches the threads, before she can drop from them on her own.
			server.runCommand("tick unfreeze");
			context.waitTicks(TatterlaceEntity.TAKE_IN_TICKS / 2 + 5);
			server.runOnServer(minecraft -> {
				TatterlaceEntity tatterlace = tatterlace(minecraft);
				check(tatterlace.phase() == TatterlaceEntity.Phase.FINAL, "She did not take to the threads: " + tatterlace.phase());
				check(Math.abs(tatterlace.getY() - tatterlace.hangY()) < 0.6, "She does not hang in her threads but at " + tatterlace.getY());
			});

			// The Final Fitting, from the threads: Pin Rain, Unravel (the lace under the player drops away, and is knitted
			// back), Drop Strike (she lies open on the lace) and Brood (spiderlings out of the sacs).
			pose(context, server, centre, (tatterlace, level, player) -> tatterlace.begin(level, TatterlaceEntity.Attack.PIN_RAIN, player),
					TatterlaceEntity.Attack.PIN_RAIN.windup + 5);
			shot(context, singleplayer, loft, centre, -6.0, 2.0, 12.0, 0.5, 4.5, 3.0, true, "jugcraft_tatterlace_pin_rain");
			pose(context, server, centre, (tatterlace, level, player) -> tatterlace.begin(level, TatterlaceEntity.Attack.UNRAVEL, player),
					TatterlaceEntity.Attack.UNRAVEL.windup + 3);
			server.runOnServer(minecraft -> {
				check(lair(minecraft).getBlockState(o.offset(UNDERFOOT)).isAir(), "Unravel did not take the lace from under the player");
				check(tatterlace(minecraft).unravelledSegments() >= 1, "No segment of the doily is down");
			});
			shot(context, singleplayer, loft, centre, 0.0, 11.0, 13.0, 0.0, 0.0, 3.0, true, "jugcraft_tatterlace_unravel");
			server.runOnServer(minecraft -> {
				TatterlaceEntity tatterlace = tatterlace(minecraft);
				tatterlace.knitAll(lair(minecraft));
				check(lair(minecraft).getBlockState(o.offset(UNDERFOOT)).is(JugcraftLairs.DOILY_LACE) && tatterlace.unravelledSegments() == 0,
						"The doily was not knitted back");
			});
			pose(context, server, centre, (tatterlace, level, player) -> tatterlace.begin(level, TatterlaceEntity.Attack.DROP_STRIKE, player),
					TatterlaceEntity.Attack.DROP_STRIKE.windup + 10);
			server.runOnServer(minecraft -> {
				TatterlaceEntity tatterlace = tatterlace(minecraft);
				check(tatterlace.action() == TatterlaceEntity.Action.OPEN && Math.abs(tatterlace.getY() - tatterlace.floorY()) < 0.1,
						"After her drop she does not lie open on the lace: " + tatterlace.action() + " at " + tatterlace.getY());
			});
			shot(context, singleplayer, loft, centre, -4.5, 1.0, 10.0, 0.5, 0.6, 5.0, true, "jugcraft_tatterlace_drop_strike");
			pose(context, server, centre, (tatterlace, level, player) -> tatterlace.begin(level, TatterlaceEntity.Attack.BROOD, player),
					TatterlaceEntity.Attack.BROOD.windup + TatterEggSacEntity.HATCH_TICKS + 4);
			AtomicReference<Vec3> hatchling = new AtomicReference<>();
			server.runOnServer(minecraft -> {
				List<UUID> brood = tatterlace(minecraft).brood();
				check(!brood.isEmpty(), "No spiderling came out of her egg sacs");
				Entity newest = lair(minecraft).getEntity(brood.getLast());
				check(newest != null, "Her newest spiderling is not in the loft");
				hatchling.set(newest.position());
			});
			// Caught as they come out: from just past the rim behind the newest, over it, towards her in the threads.
			fromTheRim(context, singleplayer, loft, centre, hatchling.get(), "jugcraft_tatterlace_brood");

			// Frenzied Stitching, below a fifth of her health: her cuffs glow red and she comes down to the lace for good;
			// caught in a stab.
			pose(context, server, centre, (tatterlace, level, player) -> {
				tatterlace.setHealth(tatterlace.getMaxHealth() * 0.15F);
				tatterlace.startFrenzy(level);
				tatterlace.begin(level, TatterlaceEntity.Attack.NEEDLEPOINT, player);
			}, TatterlaceEntity.Attack.NEEDLEPOINT.windup + 2);
			server.runOnServer(minecraft -> {
				TatterlaceEntity tatterlace = tatterlace(minecraft);
				check(tatterlace.frenzied() && tatterlace.phase() == TatterlaceEntity.Phase.FRENZY, "She is not frenzied");
				check(Math.abs(tatterlace.getY() - tatterlace.floorY()) < 0.1, "In her frenzy she is not on the lace");
			});
			shot(context, singleplayer, loft, centre, 3.0, 0.5, 4.5, 0.0, 1.0, 0.5, true, "jugcraft_tatterlace_frenzy");
			server.runCommand("tick unfreeze");

			// She falls, with a segment of her doily down: the player's loot and advancement, the doily whole again, her
			// brood gone with her, and Grey Mist in the doily's middle.
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				TatterlaceEntity tatterlace = tatterlace(minecraft);
				check(TatterlaceLoot.firstKill(player(minecraft)), "The player had already won Unravelled");
				tatterlace.unravel(level, tatterlace.segment(level, true, Vec3.atCenterOf(o.offset(UNDERFOOT))));
				check(level.getBlockState(o.offset(UNDERFOOT)).isAir(), "The segment did not drop away");
				tatterlace.took(player(minecraft), 10.0F);
				tatterlace.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			});
			context.waitTicks(10);
			server.runOnServer(minecraft -> {
				ServerPlayer player = player(minecraft);
				ServerLevel level = lair(minecraft);
				check(player.getInventory().countItem(JugcraftTatterlace.GOSSAMER_SILK) >= 4, "No Gossamer Silk");
				check(player.getInventory().countItem(JugcraftTatterlace.needleRapier()) == 1, "A first kill must bring the Needle Rapier");
				check(!TatterlaceLoot.firstKill(player), "Unravelled was not awarded");
				check(level.getBlockState(o.offset(UNDERFOOT)).is(JugcraftLairs.DOILY_LACE), "Her doily was not knitted whole as she fell");
				for (BlockPos at : TatterlaceEntity.exitCells(centre)) {
					check(level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist in the doily's middle at " + at);
				}
				check(level.getEntitiesOfClass(TatterlaceEntity.class, slot()).stream().noneMatch(TatterlaceEntity::isAlive), "She is still there");
				check(level.getEntitiesOfClass(TatterEggSacEntity.class, slot()).isEmpty()
						&& level.getEntitiesOfClass(SpiderlingEntity.class, slot()).isEmpty(), "Her egg sacs or brood outlived her");
			});
			shot(context, singleplayer, loft, centre, 0.0, 0.5, 6.0, -0.5, 1.5, 0.0, false, "jugcraft_tatterlace_defeated");
			// A fresh instance in her slot is placed anew: the Grey Mist she left is gone with the old one.
			server.runOnServer(minecraft -> {
				LairInstance used = Lairs.instance(LOFT, 0);
				check(used != null, "Her instance is not open");
				Lairs.close(minecraft, used);
				LairInstance fresh = Lairs.open(minecraft, LOFT);
				check(fresh != null && fresh.slot == 0, "No fresh instance opened in her slot");
				ServerLevel level = lair(minecraft);
				for (BlockPos at : TatterlaceEntity.exitCells(centre)) {
					check(!level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT), "Her Grey Mist outlived her instance at " + at);
				}
			});
			System.out.println("[tatterlace] client game test: her fight passed end to end");
			server.runOnServer(minecraft -> Lairs.reset());
		}
	}

	@FunctionalInterface
	private interface Posing {
		void pose(TatterlaceEntity tatterlace, ServerLevel level, ServerPlayer player);
	}

	/**
	 * Unfreezes the world, puts her in the middle of the doily facing south (on the lace, or in the threads over it while
	 * she hangs there) and the player five blocks in front of her (so her attacks land in view), does what
	 * {@code posing} asks, waits {@code ticks} for the pose and freezes the world on it. Every pose waits at least 10
	 * ticks: the client learns where she was put only on the server's next updates, and a frozen world sends none.
	 */
	private static void pose(ClientGameTestContext context, TestServerContext server, Vec3 centre, Posing posing, int ticks) {
		server.runCommand("tick unfreeze");
		server.runOnServer(minecraft -> {
			ServerLevel level = lair(minecraft);
			TatterlaceEntity tatterlace = tatterlace(minecraft);
			player(minecraft).teleportTo(centre.x + 0.5, centre.y, centre.z + 5.0);
			double y = tatterlace.phase() == TatterlaceEntity.Phase.FINAL ? tatterlace.hangY() : tatterlace.floorY();
			tatterlace.teleportTo(centre.x, y, centre.z);
			tatterlace.setYRot(0.0F);
			tatterlace.setYBodyRot(0.0F);
			tatterlace.setYHeadRot(0.0F);
			posing.pose(tatterlace, level, player(minecraft));
		});
		context.waitTicks(Math.max(10, ticks));
		server.runCommand("tick freeze");
	}

	/**
	 * A picture from ({@code dx}, {@code up}, {@code dz}) off the doily's centre (the camera's feet), turned to look at
	 * ({@code ax}, {@code ay}, {@code az}) off it.
	 */
	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, Vec3 centre, double dx,
			double up, double dz, double ax, double ay, double az, boolean frozen, String name) {
		Vec3 camera = centre.add(dx, up, dz);
		Vec3 at = centre.add(ax, ay, az);
		shoot(context, singleplayer, dimension, camera.x, camera.y, camera.z, aimYaw(camera, at), aimPitch(camera, at), frozen, name);
	}

	/**
	 * A picture from just past the doily's rim behind {@code near} (something on the lace), a block above the lace, looking
	 * over it towards the doily's middle, so it is in the foreground and she is beyond it.
	 */
	private static void fromTheRim(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, Vec3 centre,
			Vec3 near, String name) {
		Vec3 outward = new Vec3(near.x - centre.x, 0.0, near.z - centre.z).normalize();
		Vec3 camera = new Vec3(near.x, centre.y, near.z).add(outward.scale(5.5)).add(0.0, 1.0, 0.0);
		Vec3 at = centre.add(0.0, 3.0, 0.0);
		shoot(context, singleplayer, dimension, camera.x, camera.y, camera.z, aimYaw(camera, at), aimPitch(camera, at), true, name);
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
		return minecraft.getLevel(LOFT.dimension);
	}

	/** The first slot's loft, with room round it. */
	private static AABB slot() {
		BlockPos o = LOFT.origin(0);
		return new AABB(o.getX() - 8, o.getY() - 16, o.getZ() - 8, o.getX() + LOFT.width + 8, o.getY() + LOFT.height + 8, o.getZ() + LOFT.length + 8);
	}

	/** She, alive, in the first slot's loft. */
	private static TatterlaceEntity tatterlace(MinecraftServer minecraft) {
		List<TatterlaceEntity> found = lair(minecraft).getEntitiesOfClass(TatterlaceEntity.class, slot(), TatterlaceEntity::isAlive);
		check(!found.isEmpty(), "Madame Tatterlace is not in her Spindle Loft");
		return found.getFirst();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	/** Stands the camera at (x, y, z) in {@code dimension}, looking along yaw and pitch, waits for the world to draw, and shoots. */
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
