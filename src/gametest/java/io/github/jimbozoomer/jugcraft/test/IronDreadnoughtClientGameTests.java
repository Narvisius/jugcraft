package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.DischargeBreakerBlock;
import io.github.jimbozoomer.jugcraft.lair.FoundryTrench;
import io.github.jimbozoomer.jugcraft.lair.FoundryTrench.Pylon;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.PylonCrownBlock;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.HotRivetEntity;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.IronDreadnoughtEntity;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.IronDreadnoughtEntity.Attack;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.IronDreadnoughtEntity.Phase;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.IronDreadnoughtLoot;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.JugcraftDreadnought;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.MortarShellEntity;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.SparkerEntity;
import java.util.List;
import java.util.Locale;
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
 * Client game test for the Iron Dreadnought (docs/features/iron-dreadnought.md), in a real Foundry Trench: unlike a
 * game-test server's world, it has the lair dimension. His fight from end to end, with the one player in survival
 * (shielded by Resistance, so it can watch every attack): he stands dormant in his assembly gantry; standing on the pit's
 * floor wakes him and he steps down out of it, his eye lit; the Advance's attacks; his charge lighting the pylon nearest
 * the player, and the player throwing its breaker, so that he overloads with his vents blown open; the Torn Plate fouling
 * a breaker; the Overcharge's attacks; Red Line; and when he falls the player has their loot and Grounded, his Sparkers
 * are gone and Grey Mist stands in the gantry, and a fresh instance in his slot has none of it and him in his gantry
 * again. Along the way, pictures of each part of the fight, the world frozen for each so the pose holds. CI job
 * {@code client}.
 */
public class IronDreadnoughtClientGameTests implements FabricClientGameTest {
	private static final Lair TRENCH = Lair.FOUNDRY_TRENCH;
	/** A standing player's eyes over their feet, where the camera looks from. */
	private static final double EYE = 1.62;
	/** Where he is posed for his attacks (the template's x, z): in the pit's north half, facing south down it. */
	private static final double POSE_X = 35.5;
	private static final double POSE_Z = 26.5;
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

			// An instance opens: the trench is placed and he stands in his gantry; the player arrives in the dugout.
			server.runOnServer(minecraft -> {
				LairInstance instance = Lairs.open(minecraft, TRENCH);
				check(instance != null, "No Foundry Trench opened");
				check(Lairs.enter(player(minecraft), instance), "The player did not go in");
			});
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos o = TRENCH.origin(0);
			String dim = TRENCH.dimension.identifier().toString();
			double stand = FoundryTrench.STAND;
			Vec3 at = new Vec3(o.getX() + POSE_X, o.getY() + stand, o.getZ() + POSE_Z);
			server.runOnServer(minecraft -> {
				IronDreadnoughtEntity dreadnought = dreadnought(minecraft);
				check(dreadnought.phase() == Phase.WAITING, "He is not waiting but " + dreadnought.phase());
				check(dreadnought.position().distanceTo(dreadnought.waiting()) < 0.1, "He is not in his gantry but at " + dreadnought.position());
				check(!dreadnought.eyeLit(), "Dormant, his eye burns");
			});
			shot(context, singleplayer, dim, o, 38.5, stand + 1.5, 25.5, FoundryTrench.WAIT_X, stand + 3.0, FoundryTrench.WAIT_Z, false,
					"jugcraft_iron_dreadnought_waiting");

			// Standing on the pit's floor wakes him: the player, in survival on the floor, and his eye lights as his boilers
			// fire; caught as he steps down out of the gantry.
			server.runCommand("effect give @a minecraft:resistance infinite 4 true");
			server.runCommand("effect give @a minecraft:saturation infinite 0 true");
			server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.1f %.3f %.1f 180 0", dim, o.getX() + 35.5, o.getY() + stand,
					o.getZ() + 34.5));
			server.runCommand("gamemode survival @a");
			context.waitTicks(36);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				IronDreadnoughtEntity dreadnought = dreadnought(minecraft);
				check(dreadnought.phase() == Phase.WAKING && dreadnought.eyeLit(), "Standing on the pit's floor did not wake him: " + dreadnought.phase());
			});
			shot(context, singleplayer, dim, o, 41.5, stand + 1.0, 27.5, 35.5, stand + 3.0, 14.0, true, "jugcraft_iron_dreadnought_wakes");
			server.runCommand("tick unfreeze");
			context.waitTicks(IronDreadnoughtEntity.WAKE_TICKS + 10);
			server.runOnServer(minecraft -> {
				IronDreadnoughtEntity dreadnought = dreadnought(minecraft);
				check(dreadnought.phase() == Phase.ADVANCE, "He did not step down to fight: " + dreadnought.phase());
				check(!dreadnought.ventsOpen(), "Fighting, his vents are not shut");
			});

			// The Advance: each attack caught as it lands, he in the pit's north half facing south, the player in front of him;
			// the camera then stands where it shows the attack best, turned towards the point named.
			pose(context, server, at, (dreadnought, level, player) -> dreadnought.begin(level, Attack.PISTON_PUNCH, player),
					Attack.PISTON_PUNCH.windup + 2);
			shotFrom(context, singleplayer, dim, at, 8.0, 1.0, 5.0, 0.0, 2.5, 2.0, "jugcraft_iron_dreadnought_punch");
			pose(context, server, at, (dreadnought, level, player) -> dreadnought.begin(level, Attack.RIVET_FAN, player),
					Attack.RIVET_FAN.windup + 2);
			// Two ticks in the air (the server may be a tick or two ahead): the middle one may already have struck the player.
			server.runOnServer(minecraft -> check(!lair(minecraft).getEntitiesOfClass(HotRivetEntity.class, slot()).isEmpty(),
					"No rivets are in the air"));
			shotFrom(context, singleplayer, dim, at, -7.0, 1.5, 9.0, 0.0, 3.5, 3.0, "jugcraft_iron_dreadnought_rivets");
			pose(context, server, at, (dreadnought, level, player) -> dreadnought.begin(level, Attack.TRENCH_CHARGE, player),
					Attack.TRENCH_CHARGE.windup + 4);
			server.runOnServer(minecraft -> check(dreadnought(minecraft).getZ() > at.z + 1.0, "His Trench Charge did not run"));
			shotFrom(context, singleplayer, dim, at, 9.0, 1.0, 13.0, 0.0, 2.5, 4.0, "jugcraft_iron_dreadnought_charge");
			pose(context, server, at, (dreadnought, level, player) -> dreadnought.begin(level, Attack.COIL_ARC, player),
					Attack.COIL_ARC.windup + 1);
			shotFrom(context, singleplayer, dim, at, 8.0, 1.5, 9.0, 0.0, 4.0, 3.0, "jugcraft_iron_dreadnought_arc");

			// His charge: the pylon nearest the player (the west one) lights, and his coils draw violet threads to its crown;
			// then the player throws its breaker, routing the bolt to ground, and he overloads, his vents blown open.
			pose(context, server, at, (dreadnought, level, player) -> dreadnought.startCharge(level, player), 30);
			server.runOnServer(minecraft -> {
				IronDreadnoughtEntity dreadnought = dreadnought(minecraft);
				check(dreadnought.charging() > 0 && dreadnought.lit().equals(List.of(Pylon.WEST)), "His charge did not light the west pylon: "
						+ dreadnought.lit());
				check(lair(minecraft).getBlockState(Pylon.WEST.crown(o)).getValue(PylonCrownBlock.LIVE), "The west pylon's crown does not burn");
			});
			shotFrom(context, singleplayer, dim, at, 7.0, 2.0, 15.0, -9.0, 6.0, 5.0, "jugcraft_iron_dreadnought_discharge");
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				IronDreadnoughtEntity dreadnought = dreadnought(minecraft);
				BlockPos breaker = Pylon.WEST.breaker(o);
				check(dreadnought.breakerUsed(level, breaker, level.getBlockState(breaker), player(minecraft)), "Throwing the lit breaker was not answered");
				check(level.getBlockState(breaker).getValue(DischargeBreakerBlock.THROWN) && dreadnought.ventsOpen()
						&& dreadnought.overloadTicks() > 0, "Throwing the lit breaker did not overload him");
			});
			context.waitTicks(8);
			server.runCommand("tick freeze");
			shotFrom(context, singleplayer, dim, at, 7.0, 1.0, 8.0, 0.0, 3.0, 0.0, "jugcraft_iron_dreadnought_overload");

			// The Torn Plate, at half his health: he halts, tears a plate from his hull and hurls it into a breaker.
			// The plate is hurled 31 ticks into the 40 of the Torn Plate: caught just after.
			pose(context, server, at, (dreadnought, level, player) -> dreadnought.setHealth(dreadnought.getMaxHealth() * IronDreadnoughtEntity.TORN_AT),
					33);
			server.runOnServer(minecraft -> {
				IronDreadnoughtEntity dreadnought = dreadnought(minecraft);
				check(dreadnought.phase() == Phase.TORN_PLATE, "The Torn Plate did not come: " + dreadnought.phase());
				check(dreadnought.fouled() != null && lair(minecraft).getBlockState(dreadnought.fouled().breaker(o)).getValue(DischargeBreakerBlock.FOULED),
						"The torn plate fouled no breaker");
			});
			shotFrom(context, singleplayer, dim, at, -8.0, 1.0, 9.0, 0.0, 3.5, 0.0, "jugcraft_iron_dreadnought_torn_plate");
			server.runCommand("tick unfreeze");
			context.waitTicks(IronDreadnoughtEntity.TORN_TICKS);
			server.runOnServer(minecraft -> check(dreadnought(minecraft).phase() == Phase.OVERCHARGE,
					"The Overcharge did not begin: " + dreadnought(minecraft).phase()));

			// The Overcharge: his Mortar (the shell in the air over its red mark), his Blowdown and his Sparkers.
			pose(context, server, at, (dreadnought, level, player) -> dreadnought.begin(level, Attack.MORTAR, player), Attack.MORTAR.windup + 8);
			server.runOnServer(minecraft -> check(!lair(minecraft).getEntitiesOfClass(MortarShellEntity.class, slot()).isEmpty(),
					"No mortar shell is in the air"));
			shotFrom(context, singleplayer, dim, at, 13.0, 1.5, 4.0, 0.0, 6.0, 3.0, "jugcraft_iron_dreadnought_mortar");
			pose(context, server, at, (dreadnought, level, player) -> dreadnought.begin(level, Attack.BLOWDOWN, player), Attack.BLOWDOWN.windup + 2);
			shotFrom(context, singleplayer, dim, at, 8.0, 2.5, 8.0, 0.0, 1.0, 0.0, "jugcraft_iron_dreadnought_blowdown");
			pose(context, server, at, (dreadnought, level, player) -> dreadnought.begin(level, Attack.SPARKERS, player), Attack.SPARKERS.windup + 12);
			server.runOnServer(minecraft -> check(dreadnought(minecraft).sparkers().size() == IronDreadnoughtEntity.DROPPED,
					dreadnought(minecraft).sparkers().size() + " Sparkers dropped, not " + IronDreadnoughtEntity.DROPPED));
			shotFrom(context, singleplayer, dim, at, 5.0, 1.0, 7.0, 0.0, 0.8, 1.5, "jugcraft_iron_dreadnought_sparkers");

			// Red Line, at a fifth of his health: his vents jam open for good, the furnaces behind them blazing.
			pose(context, server, at, (dreadnought, level, player) -> dreadnought.setHealth(dreadnought.getMaxHealth() * 0.15F), 14);
			server.runOnServer(minecraft -> {
				IronDreadnoughtEntity dreadnought = dreadnought(minecraft);
				check(dreadnought.phase() == Phase.RED_LINE && dreadnought.ventsOpen(), "Red Line did not come: " + dreadnought.phase());
			});
			shotFrom(context, singleplayer, dim, at, -7.0, 1.0, 8.0, 0.0, 3.0, 0.0, "jugcraft_iron_dreadnought_red_line");

			// He falls: the player's loot and advancement, his Sparkers and things gone with him, and Grey Mist in the gantry he
			// came from. Caught as he falls.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				IronDreadnoughtEntity dreadnought = dreadnought(minecraft);
				check(IronDreadnoughtLoot.firstKill(player(minecraft)), "The player had already won Grounded");
				dreadnought.took(player(minecraft), 10.0F);
				dreadnought.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			});
			context.waitTicks(6);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				ServerPlayer player = player(minecraft);
				ServerLevel level = lair(minecraft);
				check(player.getInventory().countItem(JugcraftDreadnought.SALVAGED_COIL) >= 3, "No Salvaged Coil");
				int trophies = player.getInventory().countItem(JugcraftDreadnought.dynamoHalberd())
						+ player.getInventory().countItem(JugcraftDreadnought.pistonHammer());
				check(trophies == 1, "A first kill must bring one of his trophies, not " + trophies);
				check(!IronDreadnoughtLoot.firstKill(player), "Grounded was not awarded");
				for (BlockPos cell : FoundryTrench.exitCells()) {
					check(level.getBlockState(cell.offset(o)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist in the gantry at " + cell);
				}
				check(level.getEntitiesOfClass(SparkerEntity.class, slot(), SparkerEntity::isAlive).isEmpty(), "His Sparkers outlived him");
				check(level.getEntitiesOfClass(MortarShellEntity.class, slot()).isEmpty(), "His shells outlived him");
				for (Pylon pylon : Pylon.values()) {
					check(!level.getBlockState(pylon.crown(o)).getValue(PylonCrownBlock.LIVE)
							&& !level.getBlockState(pylon.breaker(o)).getValue(DischargeBreakerBlock.FOULED), "A pylon burns or a breaker is fouled after he fell");
				}
			});
			shot(context, singleplayer, dim, o, 40.5, stand + 1.5, 32.5, 35.5, stand + 1.5, FoundryTrench.EXIT_Z + 0.5, true,
					"jugcraft_iron_dreadnought_defeated");
			server.runCommand("tick unfreeze");
			context.waitTicks(30);
			server.runOnServer(minecraft -> check(lair(minecraft).getEntitiesOfClass(IronDreadnoughtEntity.class, slot()).stream()
					.noneMatch(IronDreadnoughtEntity::isAlive), "He is still there"));

			// A fresh instance in his slot is placed anew: the Grey Mist he left is gone with the old one and, once the player
			// has gone in again (into the dugout, off the pit's floor), he stands in his gantry again.
			server.runOnServer(minecraft -> {
				LairInstance used = Lairs.instance(TRENCH, 0);
				check(used != null, "His instance is not open");
				Lairs.close(minecraft, used);
				LairInstance fresh = Lairs.open(minecraft, TRENCH);
				check(fresh != null && fresh.slot == 0, "No fresh instance opened in his slot");
				ServerLevel level = lair(minecraft);
				for (BlockPos cell : FoundryTrench.exitCells()) {
					check(!level.getBlockState(cell.offset(o)).is(JugcraftLairs.LAIR_EXIT), "His Grey Mist outlived his instance at " + cell);
				}
				check(Lairs.enter(player(minecraft), fresh), "The player did not go into the fresh trench");
			});
			context.waitTicks(40);
			server.runOnServer(minecraft -> {
				IronDreadnoughtEntity again = dreadnought(minecraft);
				check(again.phase() == Phase.WAITING && again.position().distanceTo(again.waiting()) < 0.1,
						"He does not stand in the fresh trench's gantry: " + again.phase() + " at " + again.position());
			});
			System.out.println("[dreadnought] client game test: his fight passed end to end");
			server.runOnServer(minecraft -> Lairs.reset());
		}
	}

	@FunctionalInterface
	private interface Posing {
		void pose(IronDreadnoughtEntity dreadnought, ServerLevel level, ServerPlayer player);
	}

	/**
	 * Unfreezes the world, puts him at {@code spot} facing south and the player {@value #FRONT} blocks in front of him (so
	 * his attacks land in view), puts his next charge off, does what {@code posing} asks, waits {@code ticks} for the pose
	 * and freezes the world on it. Every pose waits at least 10 ticks: the client learns where he was put only on the
	 * server's next updates, and a frozen world sends none.
	 */
	private static void pose(ClientGameTestContext context, TestServerContext server, Vec3 spot, Posing posing, int ticks) {
		server.runCommand("tick unfreeze");
		server.runOnServer(minecraft -> {
			ServerLevel level = lair(minecraft);
			IronDreadnoughtEntity dreadnought = dreadnought(minecraft);
			ServerPlayer player = player(minecraft);
			player.teleportTo(spot.x, spot.y, spot.z + FRONT);
			dreadnought.teleportTo(spot.x, spot.y, spot.z);
			dreadnought.setYRot(0.0F);
			dreadnought.setYBodyRot(0.0F);
			dreadnought.setYHeadRot(0.0F);
			dreadnought.dischargeIn(2000);
			posing.pose(dreadnought, level, player);
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

	/** A picture from (x, y, z) in the trench's own coordinates (the camera's feet), turned to look at (ax, ay, az) in them. */
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
		return minecraft.getLevel(TRENCH.dimension);
	}

	/** The first slot's trench, with room round it. */
	private static AABB slot() {
		BlockPos o = TRENCH.origin(0);
		return new AABB(o.getX() - 8, o.getY() - 16, o.getZ() - 8, o.getX() + TRENCH.width + 8, o.getY() + TRENCH.height + 8,
				o.getZ() + TRENCH.length + 8);
	}

	/** He, alive, in the first slot's trench. */
	private static IronDreadnoughtEntity dreadnought(MinecraftServer minecraft) {
		List<IronDreadnoughtEntity> found = lair(minecraft).getEntitiesOfClass(IronDreadnoughtEntity.class, slot(), IronDreadnoughtEntity::isAlive);
		check(!found.isEmpty(), "The Iron Dreadnought is not in his Foundry Trench");
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
