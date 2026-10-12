package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.yeti.FallingIcicleEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.GlacierHall;
import io.github.jimbozoomer.jugcraft.lair.yeti.JugcraftYeti;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiKingEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiKingLoot;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiWhelpEntity;
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
 * Client game test for the Yeti King (docs/features/yeti-king.md), in a real Glacier Hall: unlike a game-test server's
 * world, it has the lair dimension. His fight from end to end, with the one player in survival (shielded by Resistance,
 * so it can watch every attack): he waits slumped on his throne, stepping onto the lake wakes him and he leaps down
 * onto it; the Hunt's attacks, his Ground Slam baring the snow where he lands; the King's Roar from his dais, a blizzard
 * and his kin out of the dens; the Blizzard's icicles and spikes; the Fury of the Peaks, his crown ablaze; and when he
 * falls the player has their loot and Abominable, the snow has drifted back over his lake and Grey Mist stands in its
 * middle, and a fresh instance in his slot no longer has it. Along the way, pictures of each part of the fight, the world
 * frozen for each so the pose holds. CI job {@code client}.
 */
public class YetiKingClientGameTests implements FabricClientGameTest {
	private static final Lair HALL = Lair.GLACIER_HALL;
	/** A standing player's eyes over their feet, where the camera looks from. */
	private static final double EYE = 1.62;

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
			server.runCommand("time set midnight");

			// An instance opens: the hall is placed and he waits, slumped on his throne.
			server.runOnServer(minecraft -> {
				LairInstance instance = Lairs.open(minecraft, HALL);
				check(instance != null, "No Glacier Hall opened");
				check(Lairs.enter(player(minecraft), instance), "The player did not go in");
			});
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos o = HALL.origin(0);
			String hall = HALL.dimension.identifier().toString();
			Vec3 centre = YetiKingEntity.centre(o);
			// His throne, from the centre of his lake (in its own coordinates): north, up on the dais.
			double throneZ = GlacierHall.THRONE_Z - GlacierHall.LAKE_Z;
			double throneUp = GlacierHall.SEAT_TOP - (GlacierHall.LAKE + 1);
			server.runOnServer(minecraft -> {
				YetiKingEntity king = king(minecraft);
				check(king.phase() == YetiKingEntity.Phase.WAITING, "He is not waiting but " + king.phase());
				check(king.position().distanceTo(king.throne()) < 0.1, "He is not on his throne but at " + king.position());
			});
			shot(context, singleplayer, hall, centre, 1.0, 1.0, throneZ + 13.5, 0.0, throneUp + 1.5, throneZ, false, "jugcraft_yeti_king_throne");

			// Stepping onto the lake wakes him: the player, in survival on the lake before his dais, and he rises roaring;
			// caught as he roars.
			server.runCommand("effect give @a minecraft:resistance infinite 4 true");
			server.runCommand("effect give @a minecraft:saturation infinite 0 true");
			server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.1f %.2f %.1f 180 0", hall, centre.x + 0.5, centre.y, centre.z - 8.0));
			server.runCommand("gamemode survival @a");
			context.waitTicks(16);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> check(king(minecraft).phase() == YetiKingEntity.Phase.WAKING,
					"Stepping onto the lake did not wake him: " + king(minecraft).phase()));
			shot(context, singleplayer, hall, centre, 1.0, 1.0, throneZ + 13.5, 0.0, throneUp + 2.5, throneZ, true, "jugcraft_yeti_king_wakes");
			server.runCommand("tick unfreeze");
			context.waitTicks(YetiKingEntity.WAKE_TICKS);
			server.runOnServer(minecraft -> {
				YetiKingEntity king = king(minecraft);
				check(king.phase() == YetiKingEntity.Phase.HUNT, "He did not come down to hunt: " + king.phase());
				check(Math.abs(king.getY() - king.floorY()) < 0.1, "He is not on his lake but at " + king.getY());
			});

			// The Hunt, on the lake: each attack caught as it lands, he in the middle of the lake facing south, the player
			// five blocks in front of him; the camera then stands where it shows the attack best, turned towards the point
			// named.
			pose(context, server, centre, (king, level, player) -> king.begin(level, YetiKingEntity.Attack.MAUL_SWIPE, player),
					YetiKingEntity.Attack.MAUL_SWIPE.windup + 1);
			shot(context, singleplayer, hall, centre, -6.0, 1.0, 5.5, 0.0, 2.0, 1.5, true, "jugcraft_yeti_king_maul_swipe");
			pose(context, server, centre, (king, level, player) -> king.begin(level, YetiKingEntity.Attack.BOULDER_THROW, player),
					YetiKingEntity.Attack.BOULDER_THROW.windup - 1);
			shot(context, singleplayer, hall, centre, 7.0, 1.0, 6.5, 0.0, 4.0, 0.5, true, "jugcraft_yeti_king_boulder_throw");
			pose(context, server, centre, (king, level, player) -> king.begin(level, YetiKingEntity.Attack.GROUND_SLAM, player),
					YetiKingEntity.Attack.GROUND_SLAM.windup + 6);
			shot(context, singleplayer, hall, centre, -9.0, 1.0, 9.0, 0.0, 3.5, 3.0, true, "jugcraft_yeti_king_ground_slam");
			// Where he came down, the drift snow blasted bare to glare ice.
			server.runCommand("tick unfreeze");
			context.waitTicks(YetiKingEntity.Attack.GROUND_SLAM.active);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> check(king(minecraft).baredPatches() >= 1, "His slam bared no snow"));
			shot(context, singleplayer, hall, centre, 0.0, 9.0, 17.0, 0.5, 0.0, 5.0, true, "jugcraft_yeti_king_slam_ice");
			pose(context, server, centre, (king, level, player) -> king.begin(level, YetiKingEntity.Attack.FROST_BREATH, player),
					YetiKingEntity.Attack.FROST_BREATH.windup + 8);
			shot(context, singleplayer, hall, centre, 6.5, 1.0, 3.0, 0.0, 2.5, 3.0, true, "jugcraft_yeti_king_frost_breath");
			pose(context, server, centre, (king, level, player) -> king.begin(level, YetiKingEntity.Attack.AVALANCHE_CHARGE, player),
					YetiKingEntity.Attack.AVALANCHE_CHARGE.windup + 3);
			shot(context, singleplayer, hall, centre, -7.0, 1.0, 5.0, 0.0, 1.5, 3.0, true, "jugcraft_yeti_king_charge");

			// The King's Roar, at half his health: he bounds back onto his dais and roars, a blizzard howls and his kin come
			// out of the dens.
			pose(context, server, centre, (king, level, player) -> {
				king.setHealth(king.getMaxHealth() * 0.5F);
				king.startRoar(level);
			}, 16 + 12);
			AtomicReference<Vec3> whelp = new AtomicReference<>();
			server.runOnServer(minecraft -> {
				YetiKingEntity king = king(minecraft);
				check(king.phase() == YetiKingEntity.Phase.ROARING, "He is not roaring but " + king.phase());
				List<UUID> kin = king.whelps();
				check(kin.size() == YetiKingEntity.KIN, kin.size() + " whelps answered his roar, not " + YetiKingEntity.KIN);
				Entity first = lair(minecraft).getEntity(kin.getFirst());
				check(first != null, "His first whelp is not in the hall");
				whelp.set(first.position());
			});
			double roarZ = GlacierHall.ROAR_Z - GlacierHall.LAKE_Z;
			double roarUp = GlacierHall.STEP_TOP - (GlacierHall.LAKE + 1);
			shot(context, singleplayer, hall, centre, 2.0, 1.0, roarZ + 12.0, 0.0, roarUp + 2.5, roarZ, true, "jugcraft_yeti_king_roar");
			// His kin, caught as they come: from the lake's side of the newest, towards it and its den.
			Vec3 near = whelp.get();
			Vec3 lakeward = new Vec3(centre.x - near.x, 0.0, centre.z - near.z).normalize();
			Vec3 camera = new Vec3(near.x, centre.y, near.z).add(lakeward.scale(5.0)).add(0.0, 0.5, 0.0);
			Vec3 at = near.add(0.0, 0.6, 0.0);
			shoot(context, singleplayer, hall, camera.x, camera.y, camera.z, aimYaw(camera, at), aimPitch(camera, at), true, "jugcraft_yeti_king_kin");
			server.runCommand("tick unfreeze");
			context.waitTicks(YetiKingEntity.ROAR_TICKS + 16);
			server.runOnServer(minecraft -> {
				YetiKingEntity king = king(minecraft);
				check(king.phase() == YetiKingEntity.Phase.BLIZZARD, "After his roar he did not come down for the Blizzard: " + king.phase());
				check(Math.abs(king.getY() - king.floorY()) < 0.1, "After his roar he is not on his lake but at " + king.getY());
			});

			// The Blizzard: icicles shaken from the vault onto the player, glacial spikes bursting up along the ice to them.
			// The icicles are caught falling one above another (the first just short of the ice), from seven blocks off,
			// low and looking up into the fall: from further, they were lost among the vault's own icicles behind them.
			pose(context, server, centre, (king, level, player) -> king.begin(level, YetiKingEntity.Attack.ICICLE_FALL, player),
					YetiKingEntity.Attack.ICICLE_FALL.windup + 12);
			server.runOnServer(minecraft -> {
				int falling = lair(minecraft).getEntitiesOfClass(FallingIcicleEntity.class, slot()).size();
				check(falling >= 3, "Only " + falling + " icicles are falling");
			});
			shot(context, singleplayer, hall, centre, 7.5, 1.0, 6.0, 0.5, 4.5, 5.0, true, "jugcraft_yeti_king_icicle_fall");
			pose(context, server, centre, (king, level, player) -> king.begin(level, YetiKingEntity.Attack.GLACIAL_SPIKES, player),
					YetiKingEntity.Attack.GLACIAL_SPIKES.windup + 8);
			shot(context, singleplayer, hall, centre, 7.0, 2.0, 9.0, 0.0, 1.0, 5.0, true, "jugcraft_yeti_king_glacial_spikes");

			// The Fury of the Peaks, below a fifth of his health: his crown ablaze and his eyes burning blue, close to.
			pose(context, server, centre, (king, level, player) -> {
				king.setHealth(king.getMaxHealth() * 0.15F);
				king.startFury(level);
			}, 12);
			// The camera stands where he is now (in those ticks he may have gone for the player), five and a half blocks
			// before him at his height, looking into his eyes. Put three and a half blocks from the lake's centre, it was so
			// near his hunched head that the head filled half the picture.
			AtomicReference<Vec3> furious = new AtomicReference<>();
			AtomicReference<Vec3> facing = new AtomicReference<>();
			server.runOnServer(minecraft -> {
				YetiKingEntity king = king(minecraft);
				check(king.furious() && king.phase() == YetiKingEntity.Phase.FURY, "He is not in the Fury of the Peaks");
				furious.set(king.position());
				facing.set(king.facing());
			});
			Vec3 eyes = furious.get().add(facing.get()).add(0.0, 3.3, 0.0);
			Vec3 before = furious.get().add(facing.get().scale(5.5)).add(0.0, 1.4, 0.0);
			shoot(context, singleplayer, hall, before.x, before.y, before.z, aimYaw(before, eyes), aimPitch(before, eyes), true,
					"jugcraft_yeti_king_fury");
			server.runCommand("tick unfreeze");

			// He falls, with a patch of his lake bare: the player's loot and advancement, the snow drifted back, his kin gone
			// with him, and Grey Mist in the lake's middle.
			AtomicReference<List<BlockPos>> bare = new AtomicReference<>();
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				YetiKingEntity king = king(minecraft);
				check(YetiKingLoot.firstKill(player(minecraft)), "The player had already won Abominable");
				king.callKin(level, YetiKingEntity.KIN);
				bare.set(king.bare(level, centre.add(0.0, 0.0, 8.0), YetiKingEntity.RING));
				check(!bare.get().isEmpty(), "No snow to bare as he falls");
				king.took(player(minecraft), 10.0F);
				king.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			});
			context.waitTicks(10);
			server.runOnServer(minecraft -> {
				ServerPlayer player = player(minecraft);
				ServerLevel level = lair(minecraft);
				check(player.getInventory().countItem(JugcraftYeti.YETI_FUR) >= 4, "No Yeti Fur");
				int trophies = player.getInventory().countItem(JugcraftYeti.glacierMaul()) + player.getInventory().countItem(JugcraftYeti.rimeclaw());
				check(trophies == 1, "A first kill must bring one of his trophies, not " + trophies);
				check(!YetiKingLoot.firstKill(player), "Abominable was not awarded");
				for (BlockPos cell : bare.get()) {
					check(level.getBlockState(cell).is(JugcraftLairs.DRIFT_SNOW), "The snow did not drift back as he fell at " + cell);
				}
				for (BlockPos at2 : YetiKingEntity.exitCells(centre)) {
					check(level.getBlockState(at2).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist in the lake's middle at " + at2);
				}
				check(level.getEntitiesOfClass(YetiKingEntity.class, slot()).stream().noneMatch(YetiKingEntity::isAlive), "He is still there");
				check(level.getEntitiesOfClass(YetiWhelpEntity.class, slot()).isEmpty(), "His kin outlived him");
			});
			shot(context, singleplayer, hall, centre, 0.0, 0.5, 6.0, -0.5, 1.5, 0.0, false, "jugcraft_yeti_king_defeated");
			// A fresh instance in his slot is placed anew: the Grey Mist he left is gone with the old one.
			server.runOnServer(minecraft -> {
				LairInstance used = Lairs.instance(HALL, 0);
				check(used != null, "His instance is not open");
				Lairs.close(minecraft, used);
				LairInstance fresh = Lairs.open(minecraft, HALL);
				check(fresh != null && fresh.slot == 0, "No fresh instance opened in his slot");
				ServerLevel level = lair(minecraft);
				for (BlockPos at2 : YetiKingEntity.exitCells(centre)) {
					check(!level.getBlockState(at2).is(JugcraftLairs.LAIR_EXIT), "His Grey Mist outlived his instance at " + at2);
				}
			});
			System.out.println("[yeti] client game test: his fight passed end to end");
			server.runOnServer(minecraft -> Lairs.reset());
		}
	}

	@FunctionalInterface
	private interface Posing {
		void pose(YetiKingEntity king, ServerLevel level, ServerPlayer player);
	}

	/**
	 * Unfreezes the world, puts him in the middle of the lake facing south and the player five blocks in front of him (so
	 * his attacks land in view), does what {@code posing} asks, waits {@code ticks} for the pose and freezes the world on
	 * it. Every pose waits at least 10 ticks: the client learns where he was put only on the server's next updates, and a
	 * frozen world sends none.
	 */
	private static void pose(ClientGameTestContext context, TestServerContext server, Vec3 centre, Posing posing, int ticks) {
		server.runCommand("tick unfreeze");
		server.runOnServer(minecraft -> {
			ServerLevel level = lair(minecraft);
			YetiKingEntity king = king(minecraft);
			ServerPlayer player = player(minecraft);
			player.teleportTo(centre.x + 0.5, centre.y, centre.z + 5.0);
			king.teleportTo(centre.x, king.floorY(), centre.z);
			king.setYRot(0.0F);
			king.setYBodyRot(0.0F);
			king.setYHeadRot(0.0F);
			posing.pose(king, level, player);
		});
		context.waitTicks(Math.max(10, ticks));
		server.runCommand("tick freeze");
	}

	/**
	 * A picture from ({@code dx}, {@code up}, {@code dz}) off the lake's centre (the camera's feet), turned to look at
	 * ({@code ax}, {@code ay}, {@code az}) off it.
	 */
	private static void shot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, Vec3 centre, double dx,
			double up, double dz, double ax, double ay, double az, boolean frozen, String name) {
		Vec3 camera = centre.add(dx, up, dz);
		Vec3 at = centre.add(ax, ay, az);
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
		return minecraft.getLevel(HALL.dimension);
	}

	/** The first slot's hall, with room round it. */
	private static AABB slot() {
		BlockPos o = HALL.origin(0);
		return new AABB(o.getX() - 8, o.getY() - 16, o.getZ() - 8, o.getX() + HALL.width + 8, o.getY() + HALL.height + 8, o.getZ() + HALL.length + 8);
	}

	/** He, alive, in the first slot's hall. */
	private static YetiKingEntity king(MinecraftServer minecraft) {
		List<YetiKingEntity> found = lair(minecraft).getEntitiesOfClass(YetiKingEntity.class, slot(), YetiKingEntity::isAlive);
		check(!found.isEmpty(), "The Yeti King is not in his Glacier Hall");
		return found.getFirst();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	/**
	 * Stands the camera at (x, y, z) in {@code dimension}, looking along yaw and pitch, its frost thawed, waits for the
	 * world to draw, and shoots.
	 */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, double x, double y,
			double z, int yaw, int pitch, boolean frozen, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runOnServer(minecraft -> {
			// Flying, so the camera stays where it is put (set on the server, which tells the client), and thawed, so his
			// frost does not cloud the picture.
			ServerPlayer player = player(minecraft);
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.setTicksFrozen(0);
		});
		server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.2f %.2f %.2f %d %d", dimension, x, y, z, yaw, pitch));
		context.waitTicks(frozen ? 20 : 30);
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(10);
		context.takeScreenshot(name);
	}
}
