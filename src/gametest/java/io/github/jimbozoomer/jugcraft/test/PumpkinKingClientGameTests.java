package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.Crowfield;
import io.github.jimbozoomer.jugcraft.lair.Crowfield.Hill;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.KingsGourdBlock;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.king.JackOStrawEntity;
import io.github.jimbozoomer.jugcraft.lair.king.JugcraftPumpkinKing;
import io.github.jimbozoomer.jugcraft.lair.king.PumpkinKingEntity;
import io.github.jimbozoomer.jugcraft.lair.king.PumpkinKingEntity.Attack;
import io.github.jimbozoomer.jugcraft.lair.king.PumpkinKingEntity.Fire;
import io.github.jimbozoomer.jugcraft.lair.king.PumpkinKingEntity.Phase;
import io.github.jimbozoomer.jugcraft.lair.king.PumpkinKingLoot;
import java.util.List;
import java.util.Locale;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Pumpkin King (docs/features/pumpkin-king.md), in a real Crowfield: unlike a game-test server's
 * world, it has the lair dimension. His fight from end to end, with the one player in survival (shielded by Resistance,
 * so it can watch every attack): he hangs on his post, his fire dark; stepping into the field wakes him, he tears free
 * and his head bursts into flame; the Harvest ripens the two gourds nearest the player, crows circling over them; the
 * player picks both by hand and he is bereft, his fire guttering; the Reaping's attacks; at half health the Crowning, his
 * head white-hot and two Jacks climbing out of the rows; the Hallowed Harvest's Vine Snare and Head Roll; a Jack close
 * to; and when he falls the player has their loot and Uncrowned, his Jacks are gone, his gourds green and Grey Mist
 * stands by his post, and a fresh instance in his slot has none of it and him on his post again. Along the way, pictures
 * of each part of the fight, the world frozen for each so the pose holds. CI job {@code client}.
 */
public class PumpkinKingClientGameTests implements FabricClientGameTest {
	private static final Lair FIELD = Lair.CROWFIELD;
	/** A standing player's eyes over their feet, where the camera looks from. */
	private static final double EYE = 1.62;
	/** Where he is posed for his attacks (the template's x, z): on the farmyard's arm of the cross, facing south down it. */
	private static final double POSE_X = 34.5;
	private static final double POSE_Z = 39.5;
	/** How far in front of him the player stands for a pose. */
	private static final double FRONT = 6.0;

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

			// An instance opens: the field is placed and he hangs on his post; the player comes to in the farmyard.
			server.runOnServer(minecraft -> {
				LairInstance instance = Lairs.open(minecraft, FIELD);
				check(instance != null, "No Crowfield opened");
				check(Lairs.enter(player(minecraft), instance), "The player did not go in");
			});
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos o = FIELD.origin(0);
			String dim = FIELD.dimension.identifier().toString();
			double stand = Crowfield.STAND;
			Vec3 at = new Vec3(o.getX() + POSE_X, o.getY() + stand, o.getZ() + POSE_Z);
			server.runOnServer(minecraft -> {
				PumpkinKingEntity king = king(minecraft);
				check(king.phase() == Phase.WAITING, "He is not waiting but " + king.phase());
				check(king.position().distanceTo(king.waiting()) < 0.1, "He is not on his post but at " + king.position());
				check(king.fire() == Fire.DARK, "Waiting, his fire burns");
			});
			shot(context, singleplayer, dim, o, 37.5, stand + 1.0, 45.5, Crowfield.WAIT_X, Crowfield.WAIT_Y + 3.5, Crowfield.WAIT_Z, false,
					"jugcraft_pumpkin_king_waiting");

			// Stepping into the field wakes him: the player, in survival south of his post, and he tears free, his head
			// bursting into flame.
			server.runCommand("effect give @a minecraft:resistance infinite 4 true");
			server.runCommand("effect give @a minecraft:saturation infinite 0 true");
			server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.1f %.3f %.1f 180 0", dim, o.getX() + 34.5, o.getY() + stand,
					o.getZ() + 46.5));
			server.runCommand("gamemode survival @a");
			context.waitTicks(36);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				PumpkinKingEntity king = king(minecraft);
				check(king.phase() == Phase.WAKING && king.fire() == Fire.LIT, "Stepping into the field did not wake him: " + king.phase());
			});
			shot(context, singleplayer, dim, o, 40.5, stand + 0.5, 42.5, Crowfield.WAIT_X, Crowfield.WAIT_Y + 3.0, Crowfield.WAIT_Z, true,
					"jugcraft_pumpkin_king_wakes");
			server.runCommand("tick unfreeze");
			context.waitTicks(PumpkinKingEntity.WAKE_TICKS);
			server.runOnServer(minecraft -> check(king(minecraft).phase() == Phase.REAPING, "He did not step down to fight: " + king(minecraft).phase()));

			// The Harvest: by the south hills, he calls it on the gourd nearest the player and its neighbour toward them, and
			// they ripen, crows circling over them.
			server.runOnServer(minecraft -> {
				PumpkinKingEntity king = king(minecraft);
				ServerPlayer player = player(minecraft);
				player.teleportTo(o.getX() + 30.5, o.getY() + stand, o.getZ() + 48.5);
				king.teleportTo(at.x, at.y, at.z);
				king.harvestIn(1);
			});
			boolean called = false;
			for (int wait = 0; wait < 80 && !called; wait++) {
				context.waitTick();
				called = server.computeOnServer(minecraft -> !king(minecraft).harvest().isEmpty());
			}
			check(called, "He did not call the harvest");
			server.runOnServer(minecraft -> {
				PumpkinKingEntity king = king(minecraft);
				check(king.harvest().equals(List.of(Hill.SOUTH, Hill.SOUTH_WEST)), "He called " + king.harvest() + ", not the south hills");
				king.ripenTicks(PumpkinKingEntity.SWELL_TICKS + 20);
			});
			context.waitTicks(10);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				for (Hill hill : List.of(Hill.SOUTH, Hill.SOUTH_WEST)) {
					check(level.getBlockState(hill.at(o)).getValue(KingsGourdBlock.STAGE) == KingsGourdBlock.RIPENING, "The " + hill.title()
							+ " gourd is not ripening");
				}
			});
			shot(context, singleplayer, dim, o, 27.5, stand + 2.5, 53.5, 27.5, stand + 0.5, 45.5, true, "jugcraft_pumpkin_king_harvest");

			// The player picks both by hand: he is bereft, sagging, his fire guttering.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				PumpkinKingEntity king = king(minecraft);
				ServerPlayer player = player(minecraft);
				for (Hill hill : List.of(Hill.SOUTH, Hill.SOUTH_WEST)) {
					BlockPos gourd = hill.at(o);
					player.teleportTo(gourd.getX() + 0.5, o.getY() + stand, gourd.getZ() + 2.5);
					player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(gourd), Direction.SOUTH, gourd, false));
				}
				check(king.bereft() && king.fire() == Fire.GUTTER, "Picking the whole harvest did not leave him bereft");
				player.teleportTo(at.x, at.y, at.z + FRONT);
				king.teleportTo(at.x, at.y, at.z);
				king.setYRot(0.0F);
				king.setYBodyRot(0.0F);
				king.setYHeadRot(0.0F);
			});
			context.waitTicks(12);
			server.runCommand("tick freeze");
			shotFrom(context, singleplayer, dim, at, 4.0, 0.5, 6.0, 0.0, 3.0, 0.0, "jugcraft_pumpkin_king_bereft");

			// The Reaping's attacks, each caught as it lands, he facing south, the player in front of him.
			pose(context, server, at, (king, level, player) -> king.begin(level, Attack.SCYTHE_SWEEP, player), Attack.SCYTHE_SWEEP.windup + 2);
			shotFrom(context, singleplayer, dim, at, 7.0, 0.5, 2.0, 0.0, 2.5, 1.5, "jugcraft_pumpkin_king_sweep");
			pose(context, server, at, (king, level, player) -> king.begin(level, Attack.GOURD_TOSS, player), Attack.GOURD_TOSS.windup - 3);
			server.runOnServer(minecraft -> check(king(minecraft).holding(), "He does not hold a pumpkin overhead"));
			shotFrom(context, singleplayer, dim, at, 7.0, 0.5, 3.0, 0.0, 3.5, 3.0, "jugcraft_pumpkin_king_toss");
			pose(context, server, at, (king, level, player) -> king.begin(level, Attack.CROW_STORM, player), Attack.CROW_STORM.windup + 10);
			shotFrom(context, singleplayer, dim, at, 7.0, 1.0, 4.0, 0.0, 2.0, 5.0, "jugcraft_pumpkin_king_crow_storm");

			// At half his health, the Crowning: back on his post, his head white-hot, two Jacks climbing out of the rows.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> {
				PumpkinKingEntity king = king(minecraft);
				king.harvestIn(2000);
				king.setHealth(king.getMaxHealth() * PumpkinKingEntity.CROWN_AT - 1.0F);
			});
			context.waitTicks(26);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				PumpkinKingEntity king = king(minecraft);
				check(king.phase() == Phase.CROWNING && king.fire() == Fire.WHITE, "He was not crowned: " + king.phase());
				check(jacks(minecraft).size() >= PumpkinKingEntity.CROWN_JACKS, "No Jacks climbed out at the Crowning: " + jacks(minecraft).size());
			});
			shot(context, singleplayer, dim, o, 40.5, stand + 0.5, 42.5, Crowfield.WAIT_X, Crowfield.WAIT_Y + 3.0, Crowfield.WAIT_Z, true,
					"jugcraft_pumpkin_king_crowning");
			server.runCommand("tick unfreeze");
			context.waitTicks(PumpkinKingEntity.CROWNING_TICKS);
			server.runOnServer(minecraft -> check(king(minecraft).phase() == Phase.HALLOWED, "The Hallowed Harvest did not follow: "
					+ king(minecraft).phase()));

			// The Hallowed Harvest's Vine Snare and Head Roll.
			pose(context, server, at, (king, level, player) -> king.begin(level, Attack.VINE_SNARE, player), Attack.VINE_SNARE.windup - 4);
			shotFrom(context, singleplayer, dim, at, 6.0, 1.0, 3.0, 0.0, 1.0, 4.0, "jugcraft_pumpkin_king_vine_snare");
			pose(context, server, at, (king, level, player) -> king.begin(level, Attack.HEAD_ROLL, player), Attack.HEAD_ROLL.windup + 8);
			server.runOnServer(minecraft -> check(king(minecraft).headless(), "His head did not leave his shoulders"));
			shotFrom(context, singleplayer, dim, at, 6.0, 0.5, 2.0, 0.0, 2.0, 3.0, "jugcraft_pumpkin_king_head_roll");

			// A Jack, close to.
			server.runCommand("tick unfreeze");
			context.waitTicks(Attack.HEAD_ROLL.active + Attack.HEAD_ROLL.recovery);
			Vec3 jack = server.computeOnServer(minecraft -> {
				List<JackOStrawEntity> found = jacks(minecraft);
				check(!found.isEmpty(), "No Jack to picture");
				JackOStrawEntity one = found.getFirst();
				one.teleportTo(at.x - 4.0, at.y, at.z + 2.0);
				return one.position();
			});
			context.waitTicks(4);
			server.runCommand("tick freeze");
			shotFrom(context, singleplayer, dim, jack, 2.5, 0.0, 2.5, 0.0, 1.0, 0.0, "jugcraft_pumpkin_king_jack");

			// He falls: the player's loot and advancement, his Jacks gone with him, his gourds green, and Grey Mist by his post.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				PumpkinKingEntity king = king(minecraft);
				check(PumpkinKingLoot.firstKill(player(minecraft)), "The player had already won Uncrowned");
				king.took(player(minecraft), 10.0F);
				king.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			});
			context.waitTicks(10);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				ServerPlayer player = player(minecraft);
				ServerLevel level = lair(minecraft);
				check(player.getInventory().countItem(JugcraftPumpkinKing.KINGS_EMBER) >= 1, "No King's Ember");
				int trophies = player.getInventory().countItem(JugcraftPumpkinKing.hallowreap()) + player.getInventory().countItem(JugcraftPumpkinKing.crowbill());
				check(trophies == 1, "A first win must bring one of his trophies, not " + trophies);
				check(!PumpkinKingLoot.firstKill(player), "Uncrowned was not awarded");
				for (BlockPos cell : Crowfield.postExitCells()) {
					check(level.getBlockState(cell.offset(o)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist by his post at " + cell);
				}
				check(jacks(minecraft).isEmpty(), "His Jacks outlived him");
				for (Hill hill : Hill.values()) {
					check(level.getBlockState(hill.at(o)).getValue(KingsGourdBlock.STAGE) == KingsGourdBlock.GREEN, "The " + hill.title()
							+ " gourd did not go green");
				}
			});
			shot(context, singleplayer, dim, o, 40.5, stand + 0.5, 46.5, POSE_X, stand + 0.5, POSE_Z, true, "jugcraft_pumpkin_king_defeated");
			server.runCommand("tick unfreeze");
			context.waitTicks(30);
			server.runOnServer(minecraft -> check(lair(minecraft).getEntitiesOfClass(PumpkinKingEntity.class, slot()).stream()
					.noneMatch(PumpkinKingEntity::isAlive), "He is still there"));

			// A fresh instance in his slot is placed anew: the Grey Mist he left is gone with the old one and, once the player
			// has gone in again (into the farmyard), he hangs on his post again.
			server.runOnServer(minecraft -> {
				LairInstance used = Lairs.instance(FIELD, 0);
				check(used != null, "His instance is not open");
				Lairs.close(minecraft, used);
				LairInstance fresh = Lairs.open(minecraft, FIELD);
				check(fresh != null && fresh.slot == 0, "No fresh instance opened in his slot");
				ServerLevel level = lair(minecraft);
				for (BlockPos cell : Crowfield.postExitCells()) {
					check(!level.getBlockState(cell.offset(o)).is(JugcraftLairs.LAIR_EXIT), "His Grey Mist outlived his instance at " + cell);
				}
				check(Lairs.enter(player(minecraft), fresh), "The player did not go into the fresh field");
			});
			context.waitTicks(40);
			server.runOnServer(minecraft -> {
				PumpkinKingEntity again = king(minecraft);
				check(again.phase() == Phase.WAITING && again.position().distanceTo(again.waiting()) < 0.1,
						"He does not hang on the fresh field's post: " + again.phase() + " at " + again.position());
			});
			System.out.println("[pumpkin_king] client game test: his fight passed end to end");
			server.runOnServer(minecraft -> Lairs.reset());
		}
	}

	@FunctionalInterface
	private interface Posing {
		void pose(PumpkinKingEntity king, ServerLevel level, ServerPlayer player);
	}

	/**
	 * Unfreezes the world, puts him at {@code spot} facing south and the player {@value #FRONT} blocks in front of him (so
	 * his attacks land in view), puts his next harvest off, does what {@code posing} asks, waits {@code ticks} for the pose
	 * and freezes the world on it. Every pose waits at least 10 ticks: the client learns where he was put only on the
	 * server's next updates, and a frozen world sends none.
	 */
	private static void pose(ClientGameTestContext context, TestServerContext server, Vec3 spot, Posing posing, int ticks) {
		server.runCommand("tick unfreeze");
		server.runOnServer(minecraft -> {
			ServerLevel level = lair(minecraft);
			PumpkinKingEntity king = king(minecraft);
			ServerPlayer player = player(minecraft);
			player.teleportTo(spot.x, spot.y, spot.z + FRONT);
			king.teleportTo(spot.x, spot.y, spot.z);
			king.setYRot(0.0F);
			king.setYBodyRot(0.0F);
			king.setYHeadRot(0.0F);
			king.harvestIn(2000);
			posing.pose(king, level, player);
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

	/** A picture from (x, y, z) in the field's own coordinates (the camera's feet), turned to look at (ax, ay, az) in them. */
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
		return minecraft.getLevel(FIELD.dimension);
	}

	/** The first slot's field, with room round it. */
	private static AABB slot() {
		BlockPos o = FIELD.origin(0);
		return new AABB(o.getX() - 8, o.getY() - 16, o.getZ() - 8, o.getX() + FIELD.width + 8, o.getY() + FIELD.height + 8, o.getZ() + FIELD.length + 8);
	}

	/** He, alive, in the first slot's field. */
	private static PumpkinKingEntity king(MinecraftServer minecraft) {
		List<PumpkinKingEntity> found = lair(minecraft).getEntitiesOfClass(PumpkinKingEntity.class, slot(), PumpkinKingEntity::isAlive);
		check(!found.isEmpty(), "The Pumpkin King is not in his Crowfield");
		return found.getFirst();
	}

	/** His Jacks, alive. */
	private static List<JackOStrawEntity> jacks(MinecraftServer minecraft) {
		return lair(minecraft).getEntitiesOfClass(JackOStrawEntity.class, slot(), JackOStrawEntity::isAlive);
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
