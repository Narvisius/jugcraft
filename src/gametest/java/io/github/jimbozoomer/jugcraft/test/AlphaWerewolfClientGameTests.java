package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MoonlitRing;
import io.github.jimbozoomer.jugcraft.lair.MoonlitRing.Bell;
import io.github.jimbozoomer.jugcraft.lair.WardenBellBlock;
import io.github.jimbozoomer.jugcraft.lair.alpha.AlphaWerewolfEntity;
import io.github.jimbozoomer.jugcraft.lair.alpha.AlphaWerewolfEntity.Attack;
import io.github.jimbozoomer.jugcraft.lair.alpha.AlphaWerewolfEntity.Hunt;
import io.github.jimbozoomer.jugcraft.lair.alpha.AlphaWerewolfEntity.Phase;
import io.github.jimbozoomer.jugcraft.lair.alpha.AlphaWerewolfLoot;
import io.github.jimbozoomer.jugcraft.lair.alpha.JugcraftAlpha;
import io.github.jimbozoomer.jugcraft.lair.alpha.PackmateEntity;
import java.util.List;
import java.util.Locale;
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
 * Client game test for the Alpha Werewolf (docs/features/alpha-werewolf.md), in a real Moonlit Ring: unlike a game-test
 * server's world, it has the lair dimension. His fight from end to end, with the one player in survival (shielded by
 * Resistance, so it can watch every attack): he crouches on the Howling Stone, his eyes dark; stepping onto the turf wakes
 * him, he howls and his pack lopes out of the trees; the Chase's attacks; the Hunt (the mark, his run to a clearing not
 * the player's nearest over a trail of raked turf, and his eyes in its dark), the player ringing that clearing's bell and
 * flushing him out; a second hunt left to run, the shadow spreading and his spring onto the player; the Pack Gathers; the
 * Moon's Fury's Tree Leap and Hamstring Bite; and when he falls the player has their loot and Hunter's Moon, his pack is
 * gone, his trail knit and Grey Mist stands on the stone, and a fresh instance in his slot has none of it and him on his
 * stone again. Along the way, pictures of each part of the fight, the world frozen for each so the pose holds. CI job
 * {@code client}.
 */
public class AlphaWerewolfClientGameTests implements FabricClientGameTest {
	private static final Lair RING = Lair.MOONLIT_RING;
	/** A standing player's eyes over their feet, where the camera looks from. */
	private static final double EYE = 1.62;
	/** Where he is posed for his attacks (the template's x, z): on the south spoke, facing south down it. */
	private static final double POSE_X = 37.5;
	private static final double POSE_Z = 45.5;
	/** How far in front of him the player stands for a pose. */
	private static final double FRONT = 5.0;

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
			server.runCommand("difficulty normal");  // he is a monster: in peaceful he would be gone

			// An instance opens: the ring is placed and he crouches on the stone; the player comes to in the camp.
			server.runOnServer(minecraft -> {
				LairInstance instance = Lairs.open(minecraft, RING);
				check(instance != null, "No Moonlit Ring opened");
				check(Lairs.enter(player(minecraft), instance), "The player did not go in");
			});
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos o = RING.origin(0);
			String dim = RING.dimension.identifier().toString();
			double stand = MoonlitRing.STAND;
			Vec3 at = new Vec3(o.getX() + POSE_X, o.getY() + stand, o.getZ() + POSE_Z);
			server.runOnServer(minecraft -> {
				AlphaWerewolfEntity alpha = alpha(minecraft);
				check(alpha.phase() == Phase.WAITING, "He is not waiting but " + alpha.phase());
				check(alpha.position().distanceTo(alpha.waiting()) < 0.1, "He is not on his stone but at " + alpha.position());
				check(!alpha.eyesLit(), "Waiting, his eyes burn");
			});
			shot(context, singleplayer, dim, o, 37.5, stand + 1.0, 46.5, MoonlitRing.WAIT_X, MoonlitRing.WAIT_Y + 1.5, MoonlitRing.WAIT_Z, false,
					"jugcraft_alpha_werewolf_waiting");

			// Stepping onto the turf wakes him: the player, in survival south of the stone, and he rises and howls at the
			// moon, his eyes blazing, as his pack lopes out of the trees.
			server.runCommand("effect give @a minecraft:resistance infinite 4 true");
			server.runCommand("effect give @a minecraft:saturation infinite 0 true");
			server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.1f %.3f %.1f 180 0", dim, o.getX() + 37.5, o.getY() + stand,
					o.getZ() + 50.5));
			server.runCommand("gamemode survival @a");
			context.waitTicks(36);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				AlphaWerewolfEntity alpha = alpha(minecraft);
				check(alpha.phase() == Phase.WAKING && alpha.eyesLit(), "Stepping onto the turf did not wake him: " + alpha.phase());
				check(pack(minecraft).size() == AlphaWerewolfEntity.PACK_SIZE, "His pack did not lope out: " + pack(minecraft).size());
			});
			shot(context, singleplayer, dim, o, 42.5, stand + 0.5, 44.5, MoonlitRing.WAIT_X, MoonlitRing.WAIT_Y + 2.0, MoonlitRing.WAIT_Z, true,
					"jugcraft_alpha_werewolf_wakes");
			server.runCommand("tick unfreeze");
			context.waitTicks(AlphaWerewolfEntity.WAKE_TICKS);
			server.runOnServer(minecraft -> check(alpha(minecraft).phase() == Phase.CHASE, "He did not leap down to fight: " + alpha(minecraft).phase()));

			// The Chase: each attack caught as it lands, he on the south spoke facing south, the player in front of him; the
			// camera then stands where it shows the attack best, turned towards the point named.
			pose(context, server, at, (alpha, level, player) -> alpha.begin(level, Attack.CLAW_RAKE, player), Attack.CLAW_RAKE.windup + 3);
			shotFrom(context, singleplayer, dim, at, 6.0, 0.5, 3.0, 0.0, 1.8, 1.0, "jugcraft_alpha_werewolf_rake");
			pose(context, server, at, (alpha, level, player) -> alpha.begin(level, Attack.POUNCE, player), Attack.POUNCE.windup + 4);
			server.runOnServer(minecraft -> check(alpha(minecraft).flying(), "His Pounce did not leap"));
			shotFrom(context, singleplayer, dim, at, 7.0, 0.5, 2.0, 0.0, 2.5, 3.0, "jugcraft_alpha_werewolf_pounce");
			pose(context, server, at, (alpha, level, player) -> alpha.begin(level, Attack.RENDING_HOWL, player), Attack.RENDING_HOWL.windup + 4);
			shotFrom(context, singleplayer, dim, at, -5.0, 0.5, 5.0, 0.0, 2.5, 0.0, "jugcraft_alpha_werewolf_howl");

			// The Hunt: he marks the player (the camera) and runs to a clearing not the player's nearest, raking the moss;
			// the picture looks down his trail into the clearing where his eyes burn.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> {
				AlphaWerewolfEntity alpha = alpha(minecraft);
				ServerPlayer player = player(minecraft);
				player.teleportTo(at.x, at.y, at.z + FRONT);
				alpha.teleportTo(at.x, at.y, at.z);
				alpha.huntIn(1);
			});
			// Rending Howl was frozen during its active part. The hunt timer intentionally pauses until its
			// recovery finishes; observe the actual server transition instead of assuming eight client ticks suffice.
			boolean marked = false;
			for (int wait = 0; wait < Attack.RENDING_HOWL.length() + 20 && !marked; wait++) {
				context.waitTick();
				marked = server.computeOnServer(minecraft -> alpha(minecraft).hunt() == Hunt.MARK);
			}
			check(marked, "He did not mark the player after finishing Rending Howl");
			context.waitTicks(AlphaWerewolfEntity.MARK_TICKS + AlphaWerewolfEntity.RUN_TICKS + 4);
			server.runCommand("tick freeze");
			AtomicReference<Bell> glade = new AtomicReference<>();
			server.runOnServer(minecraft -> {
				AlphaWerewolfEntity alpha = alpha(minecraft);
				check(alpha.hunt() == Hunt.HIDE && alpha.glade() != null, "He did not go to ground: " + alpha.hunt());
				check(!alpha.raked().isEmpty(), "He left no trail");
				for (BlockPos cell : alpha.raked()) {
					check(lair(minecraft).getBlockState(cell).is(JugcraftLairs.RAKED_TURF), "His trail is not raked turf at " + cell.subtract(o));
				}
				glade.set(alpha.glade());
			});
			Bell hid = glade.get();
			Vec3 hide = new Vec3(o.getX() + hid.gladeX(), o.getY() + stand, o.getZ() + hid.gladeZ());
			Vec3 down = new Vec3(o.getX() + MoonlitRing.CENTRE_X - hid.wayX * 2.0 + hid.wayZ * 3.0, o.getY() + stand + 2.0,
					o.getZ() + MoonlitRing.CENTRE_Z - hid.wayZ * 2.0 + hid.wayX * 3.0);
			shoot(context, singleplayer, dim, down.x, down.y, down.z, aimYaw(down, hide), aimPitch(down, hide.add(0.0, 0.5, 0.0)), true,
					"jugcraft_alpha_werewolf_hidden");

			// The player rings the bell of his clearing: he is flushed, staggering out along its spoke, the bell swinging.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				AlphaWerewolfEntity alpha = alpha(minecraft);
				ServerPlayer player = player(minecraft);
				BlockPos bell = hid.at(o);
				player.teleportTo(level, bell.getX() + 0.5 - hid.wayX * 3.0, o.getY() + stand, bell.getZ() + 0.5 - hid.wayZ * 3.0, java.util.Set.of(),
						0.0F, 0.0F, true);
				check(WardenBellBlock.ring(level, bell), "The bell of his clearing did not ring");
				check(alpha.bellRung(level, bell, player) && alpha.flushed() && alpha.cowering(), "Its bell did not flush him");
			});
			context.waitTicks(12);
			server.runCommand("tick freeze");
			Vec3 out = new Vec3(o.getX() + MoonlitRing.CENTRE_X + hid.wayX * AlphaWerewolfEntity.FLUSHED_TO, o.getY() + stand,
					o.getZ() + MoonlitRing.CENTRE_Z + hid.wayZ * AlphaWerewolfEntity.FLUSHED_TO);
			Vec3 side = out.add(hid.wayZ * 7.0 - hid.wayX * 3.0, 0.5, hid.wayX * 7.0 - hid.wayZ * 3.0);
			shoot(context, singleplayer, dim, side.x, side.y, side.z, aimYaw(side, out.add(hid.wayX * 3.0, 0.0, hid.wayZ * 3.0)),
					aimPitch(side, out.add(0.0, 1.5, 0.0)), true, "jugcraft_alpha_werewolf_flushed");

			// A second hunt, left to run: the shadow spreads under the player and he springs out of the trees onto it.
			server.runCommand("tick unfreeze");
			context.waitTicks(AlphaWerewolfEntity.STAGGER_TICKS);
			server.runOnServer(minecraft -> {
				AlphaWerewolfEntity alpha = alpha(minecraft);
				ServerPlayer player = player(minecraft);
				player.teleportTo(at.x, at.y, at.z + FRONT);
				alpha.teleportTo(at.x, at.y, at.z);
				alpha.huntIn(1);
			});
			context.waitTicks(AlphaWerewolfEntity.MARK_TICKS + AlphaWerewolfEntity.RUN_TICKS + 6);
			server.runOnServer(minecraft -> {
				AlphaWerewolfEntity alpha = alpha(minecraft);
				check(alpha.hunt() == Hunt.HIDE, "He did not go to ground again: " + alpha.hunt());
				alpha.ambushTicks(AlphaWerewolfEntity.AMBUSH_TICKS - AlphaWerewolfEntity.SHADOW_TICKS - 1);
			});
			context.waitTicks(AlphaWerewolfEntity.SHADOW_TICKS - AlphaWerewolfEntity.SPRING_TICKS + 7);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> check(alpha(minecraft).hunt() == Hunt.SPRING, "He did not spring: " + alpha(minecraft).hunt()));
			Vec3 prey = at.add(0.0, 0.0, FRONT);
			shotFrom(context, singleplayer, dim, prey, 7.0, 0.5, 4.0, 0.0, 2.5, -3.0, "jugcraft_alpha_werewolf_spring");
			server.runCommand("tick unfreeze");
			context.waitTicks(AlphaWerewolfEntity.SPRING_TICKS);
			server.runOnServer(minecraft -> check(alpha(minecraft).hunt() == Hunt.NONE, "His spring did not end the hunt"));

			// At half his health the Pack Gathers: he leaps onto the stone and howls long at the moon.
			server.runOnServer(minecraft -> {
				AlphaWerewolfEntity alpha = alpha(minecraft);
				alpha.huntIn(2000);
				alpha.setHealth(alpha.getMaxHealth() * AlphaWerewolfEntity.GATHER_AT - 1.0F);
			});
			context.waitTicks(26);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				check(alpha(minecraft).phase() == Phase.GATHERING, "The Pack did not Gather: " + alpha(minecraft).phase());
				check(pack(minecraft).size() == AlphaWerewolfEntity.PACK_SIZE, "His howl did not call his pack back up to full");
			});
			shot(context, singleplayer, dim, o, 33.5, stand + 0.5, 45.5, MoonlitRing.WAIT_X, MoonlitRing.WAIT_Y + 2.0, MoonlitRing.WAIT_Z, true,
					"jugcraft_alpha_werewolf_gathers");
			server.runCommand("tick unfreeze");
			context.waitTicks(AlphaWerewolfEntity.GATHER_TICKS);
			server.runOnServer(minecraft -> check(alpha(minecraft).phase() == Phase.FURY, "The Moon's Fury did not follow: " + alpha(minecraft).phase()));

			// The Moon's Fury: the Tree Leap (caught as he drops out of the canopy onto the shadow behind the player) and the
			// Hamstring Bite.
			pose(context, server, at, (alpha, level, player) -> alpha.begin(level, Attack.TREE_LEAP, player), Attack.TREE_LEAP.windup - 5);
			server.runOnServer(minecraft -> check(!alpha(minecraft).isInvisible() && alpha(minecraft).flying(), "He is not dropping out of the trees"));
			shotFrom(context, singleplayer, dim, at.add(0.0, 0.0, FRONT), 8.0, 0.5, 2.0, 0.0, 3.0, -3.0, "jugcraft_alpha_werewolf_tree_leap");
			pose(context, server, at, (alpha, level, player) -> alpha.begin(level, Attack.HAMSTRING_BITE, player), Attack.HAMSTRING_BITE.windup + 2);
			shotFrom(context, singleplayer, dim, at, 5.0, 0.5, 2.5, 0.0, 1.2, 1.5, "jugcraft_alpha_werewolf_bite");

			// He falls: the player's loot and advancement, his pack gone with him, his trail knit, and Grey Mist on the stone.
			server.runCommand("tick unfreeze");
			AtomicReference<List<BlockPos>> trail = new AtomicReference<>();
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				AlphaWerewolfEntity alpha = alpha(minecraft);
				check(AlphaWerewolfLoot.firstKill(player(minecraft)), "The player had already won Hunter's Moon");
				trail.set(List.copyOf(alpha.raked()));
				alpha.took(player(minecraft), 10.0F);
				alpha.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			});
			context.waitTicks(10);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				ServerPlayer player = player(minecraft);
				ServerLevel level = lair(minecraft);
				check(player.getInventory().countItem(JugcraftAlpha.ALPHA_PELT) >= 1, "No Alpha Pelt");
				int trophies = player.getInventory().countItem(JugcraftAlpha.moonfang()) + player.getInventory().countItem(JugcraftAlpha.howler());
				check(trophies == 1, "A first kill must bring one of his trophies, not " + trophies);
				check(!AlphaWerewolfLoot.firstKill(player), "Hunter's Moon was not awarded");
				for (BlockPos cell : MoonlitRing.stoneExitCells()) {
					check(level.getBlockState(cell.offset(o)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist on the stone at " + cell);
				}
				check(level.getEntitiesOfClass(PackmateEntity.class, slot(), PackmateEntity::isAlive).isEmpty(), "His pack outlived him");
				for (BlockPos cell : trail.get()) {
					check(!level.getBlockState(cell).is(JugcraftLairs.RAKED_TURF), "His trail did not knit at " + cell.subtract(o));
				}
			});
			shot(context, singleplayer, dim, o, 41.5, stand + 0.5, 44.5, MoonlitRing.WAIT_X, MoonlitRing.WAIT_Y + 0.5, MoonlitRing.WAIT_Z, true,
					"jugcraft_alpha_werewolf_defeated");
			server.runCommand("tick unfreeze");
			context.waitTicks(30);
			server.runOnServer(minecraft -> check(lair(minecraft).getEntitiesOfClass(AlphaWerewolfEntity.class, slot()).stream()
					.noneMatch(AlphaWerewolfEntity::isAlive), "He is still there"));

			// A fresh instance in his slot is placed anew: the Grey Mist he left is gone with the old one and, once the player
			// has gone in again (into the camp, off the turf), he crouches on his stone again.
			server.runOnServer(minecraft -> {
				LairInstance used = Lairs.instance(RING, 0);
				check(used != null, "His instance is not open");
				Lairs.close(minecraft, used);
				LairInstance fresh = Lairs.open(minecraft, RING);
				check(fresh != null && fresh.slot == 0, "No fresh instance opened in his slot");
				ServerLevel level = lair(minecraft);
				for (BlockPos cell : MoonlitRing.stoneExitCells()) {
					check(!level.getBlockState(cell.offset(o)).is(JugcraftLairs.LAIR_EXIT), "His Grey Mist outlived his instance at " + cell);
				}
				check(Lairs.enter(player(minecraft), fresh), "The player did not go into the fresh ring");
			});
			context.waitTicks(40);
			server.runOnServer(minecraft -> {
				AlphaWerewolfEntity again = alpha(minecraft);
				check(again.phase() == Phase.WAITING && again.position().distanceTo(again.waiting()) < 0.1,
						"He does not crouch on the fresh ring's stone: " + again.phase() + " at " + again.position());
			});
			System.out.println("[alpha_werewolf] client game test: his fight passed end to end");
			server.runOnServer(minecraft -> Lairs.reset());
		}
	}

	@FunctionalInterface
	private interface Posing {
		void pose(AlphaWerewolfEntity alpha, ServerLevel level, ServerPlayer player);
	}

	/**
	 * Unfreezes the world, puts him at {@code spot} facing south and the player {@value #FRONT} blocks in front of him (so
	 * his attacks land in view), puts his next hunt off, does what {@code posing} asks, waits {@code ticks} for the pose
	 * and freezes the world on it. Every pose waits at least 10 ticks: the client learns where he was put only on the
	 * server's next updates, and a frozen world sends none.
	 */
	private static void pose(ClientGameTestContext context, TestServerContext server, Vec3 spot, Posing posing, int ticks) {
		server.runCommand("tick unfreeze");
		server.runOnServer(minecraft -> {
			ServerLevel level = lair(minecraft);
			AlphaWerewolfEntity alpha = alpha(minecraft);
			ServerPlayer player = player(minecraft);
			player.teleportTo(spot.x, spot.y, spot.z + FRONT);
			alpha.teleportTo(spot.x, spot.y, spot.z);
			alpha.setYRot(0.0F);
			alpha.setYBodyRot(0.0F);
			alpha.setYHeadRot(0.0F);
			alpha.huntIn(2000);
			posing.pose(alpha, level, player);
		});
		context.waitTicks(Math.max(10, ticks));
		server.runCommand("tick freeze");
	}

	/**
	 * A picture from ({@code dx}, {@code up}, {@code dz}) off {@code base} (the camera's feet), turned to look at
	 * ({@code ax}, {@code ay}, {@code az}) off it, the world frozen.
	 */
	private static void shotFrom(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, Vec3 base, double dx,
			double up, double dz, double ax, double ay, double az, String name) {
		Vec3 camera = base.add(dx, up, dz);
		Vec3 look = base.add(ax, ay, az);
		shoot(context, singleplayer, dimension, camera.x, camera.y, camera.z, aimYaw(camera, look), aimPitch(camera, look), true, name);
	}

	/** A picture from (x, y, z) in the ring's own coordinates (the camera's feet), turned to look at (ax, ay, az) in them. */
	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, BlockPos o, double x,
			double y, double z, double ax, double ay, double az, boolean frozen, String name) {
		Vec3 camera = new Vec3(o.getX() + x, o.getY() + y, o.getZ() + z);
		Vec3 look = new Vec3(o.getX() + ax, o.getY() + ay, o.getZ() + az);
		shoot(context, singleplayer, dimension, camera.x, camera.y, camera.z, aimYaw(camera, look), aimPitch(camera, look), frozen, name);
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
		return minecraft.getLevel(RING.dimension);
	}

	/** The first slot's ring, with room round it. */
	private static AABB slot() {
		BlockPos o = RING.origin(0);
		return new AABB(o.getX() - 8, o.getY() - 16, o.getZ() - 8, o.getX() + RING.width + 8, o.getY() + RING.height + 8, o.getZ() + RING.length + 8);
	}

	/** He, alive, in the first slot's ring. */
	private static AlphaWerewolfEntity alpha(MinecraftServer minecraft) {
		List<AlphaWerewolfEntity> found = lair(minecraft).getEntitiesOfClass(AlphaWerewolfEntity.class, slot(), AlphaWerewolfEntity::isAlive);
		check(!found.isEmpty(), "The Alpha Werewolf is not in his Moonlit Ring");
		return found.getFirst();
	}

	/** His pack, alive. */
	private static List<PackmateEntity> pack(MinecraftServer minecraft) {
		return lair(minecraft).getEntitiesOfClass(PackmateEntity.class, slot(), PackmateEntity::isAlive);
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
