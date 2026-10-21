package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.CydonianSanctum;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.sentinel.CydonianSentinelEntity;
import io.github.jimbozoomer.jugcraft.lair.sentinel.CydonianSentinelEntity.Attack;
import io.github.jimbozoomer.jugcraft.lair.sentinel.CydonianSentinelEntity.Wind;
import io.github.jimbozoomer.jugcraft.lair.sentinel.JugcraftSentinel;
import io.github.jimbozoomer.jugcraft.lair.sentinel.QuakeSpikeEntity;
import io.github.jimbozoomer.jugcraft.lair.sentinel.RustShardEntity;
import io.github.jimbozoomer.jugcraft.lair.sentinel.SentinelLoot;
import io.github.jimbozoomer.jugcraft.lair.sentinel.SentinelMeteorEntity;
import io.github.jimbozoomer.jugcraft.lair.sentinel.ShardSentryEntity;
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
 * Client game test for the Cydonian Sentinel (docs/features/cydonian-sentinel.md), in a real Cydonian Sanctum: unlike a
 * game-test server's world, it has the lair dimension, the crater, its standing stones and its rim. Its fight from end to
 * end, with the one player in survival (shielded by Resistance, so it can watch every attack): it waits on its plinth;
 * standing on the crater's floor wakes it and it steps down, its core flaring; the Sentinel's Hammerfall, Shard Volley,
 * Quake Line and Core Lance (its plates open); a Red Storm from the west; the Shell Cracks, its sentries rising; the
 * Unbound Core's Sweeping Lance, Shard Burst and Starfall (it sinks, dimmed); the Red Giant; and when it falls the player
 * has their loot and Red Planet, its sentries are gone and the Red Mist stands in the crater's middle, and a fresh
 * instance in its slot has none of it and it on its plinth again. Along the way, pictures of each part of the fight, the
 * world frozen for each so the pose holds. CI job {@code client}.
 */
public class CydonianSentinelClientGameTests implements FabricClientGameTest {
	private static final Lair SANCTUM = Lair.CYDONIAN_SANCTUM;
	/** A standing player's eyes over their feet, where the camera looks from. */
	private static final double EYE = 1.62;
	/** Where it is posed for its attacks (the template's x, z): south of the crater's middle, facing south across it. */
	private static final double POSE_X = 36.5;
	private static final double POSE_Z = 42.5;

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
			server.runCommand("difficulty normal");  // it is a monster: in peaceful it would be gone

			// An instance opens: the Sanctum is placed and it waits on its plinth; the player arrives in the gate chamber.
			server.runOnServer(minecraft -> {
				LairInstance instance = Lairs.open(minecraft, SANCTUM);
				check(instance != null, "No Cydonian Sanctum opened");
				check(Lairs.enter(player(minecraft), instance), "The player did not go in");
			});
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos o = SANCTUM.origin(0);
			String dim = SANCTUM.dimension.identifier().toString();
			Vec3 centre = CydonianSentinelEntity.centre(o);
			Vec3 at = new Vec3(o.getX() + POSE_X, o.getY() + CydonianSanctum.STAND, o.getZ() + POSE_Z);
			server.runOnServer(minecraft -> {
				CydonianSentinelEntity sentinel = sentinel(minecraft);
				check(sentinel.phase() == CydonianSentinelEntity.Phase.WAITING, "It is not waiting but " + sentinel.phase());
				check(sentinel.position().distanceTo(sentinel.waiting()) < 0.1, "It is not on its plinth but at " + sentinel.position());
				check(sentinel.glow() == CydonianSentinelEntity.Glow.DARK, "Waiting, its core is not dark");
			});
			shot(context, singleplayer, dim, o, 40.5, CydonianSanctum.STAND + 1.0, 30.5, CydonianSanctum.WAIT_X, CydonianSanctum.WAIT_Y + 2.2,
					CydonianSanctum.WAIT_Z, false, "jugcraft_cydonian_sentinel_waiting");

			// Standing on the crater's floor wakes it: the player, in survival on the floor, and its core flares as it steps
			// down from its plinth; caught as it stirs.
			server.runCommand("effect give @a minecraft:resistance infinite 4 true");
			server.runCommand("effect give @a minecraft:saturation infinite 0 true");
			server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.1f %.3f %.1f 180 0", dim, centre.x + 3.0, centre.y, centre.z - 8.0));
			server.runCommand("gamemode survival @a");
			context.waitTicks(22);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> check(sentinel(minecraft).phase() == CydonianSentinelEntity.Phase.WAKING,
					"Standing on the crater's floor did not wake it: " + sentinel(minecraft).phase()));
			shot(context, singleplayer, dim, o, 41.5, CydonianSanctum.STAND + 1.0, 31.5, CydonianSanctum.WAIT_X, CydonianSanctum.WAIT_Y + 2.2,
					CydonianSanctum.WAIT_Z, true, "jugcraft_cydonian_sentinel_wakes");
			// The player stands out of the fight (in creative) while it steps down, so it has no foe to begin an attack on
			// (a Core Lance would open its plates) when it is looked at; then back in for its attacks.
			server.runCommand("gamemode creative @a");
			server.runCommand("tick unfreeze");
			context.waitTicks(CydonianSentinelEntity.WAKE_TICKS + 10);
			server.runOnServer(minecraft -> {
				CydonianSentinelEntity sentinel = sentinel(minecraft);
				check(sentinel.phase() == CydonianSentinelEntity.Phase.SENTINEL, "It did not step down to fight: " + sentinel.phase());
				check(sentinel.glow() == CydonianSentinelEntity.Glow.LIT && sentinel.form() == CydonianSentinelEntity.Form.SHELL,
						"Stepped down, its core is not lit in its shell: " + sentinel.glow() + ", " + sentinel.attack());
			});
			server.runCommand("gamemode survival @a");

			// The Sentinel: each attack caught as it lands, it south of the crater's middle facing south, the player five
			// blocks in front of it; the camera then stands where it shows the attack best, turned towards the point named.
			pose(context, server, at, (sentinel, level, player) -> sentinel.begin(level, Attack.HAMMERFALL, player), Attack.HAMMERFALL.windup + 2);
			shotFrom(context, singleplayer, dim, at, 7.0, 1.5, 4.0, 0.0, 1.8, 1.5, "jugcraft_cydonian_sentinel_hammerfall");
			pose(context, server, at, (sentinel, level, player) -> sentinel.begin(level, Attack.SHARD_VOLLEY, player), Attack.SHARD_VOLLEY.windup + 3);
			server.runOnServer(minecraft -> check(lair(minecraft).getEntitiesOfClass(RustShardEntity.class, slot()).size()
					>= CydonianSentinelEntity.VOLLEY_SHARDS, "No volley of shards flies at the player"));
			shotFrom(context, singleplayer, dim, at, 6.0, 1.5, 6.0, 0.0, 1.8, 2.5, "jugcraft_cydonian_sentinel_volley");
			pose(context, server, at, (sentinel, level, player) -> sentinel.begin(level, Attack.QUAKE_LINE, player), Attack.QUAKE_LINE.windup + 4);
			server.runOnServer(minecraft -> {
				int spikes = lair(minecraft).getEntitiesOfClass(QuakeSpikeEntity.class, slot()).size();
				check(spikes >= 5, "Only " + spikes + " spikes burst up along its Quake Line");
			});
			shotFrom(context, singleplayer, dim, at, -7.0, 5.0, -2.0, 0.0, 0.5, 6.0, "jugcraft_cydonian_sentinel_quake");
			pose(context, server, at, (sentinel, level, player) -> sentinel.begin(level, Attack.CORE_LANCE, player), Attack.CORE_LANCE.windup - 4);
			server.runOnServer(minecraft -> {
				CydonianSentinelEntity sentinel = sentinel(minecraft);
				check(sentinel.platesOpen() && sentinel.glow() == CydonianSentinelEntity.Glow.OPEN, "Charging its Core Lance, its plates are not open");
			});
			shotFrom(context, singleplayer, dim, at, 1.5, 1.5, 5.5, 0.0, 2.6, 0.0, "jugcraft_cydonian_sentinel_lance_open");
			server.runCommand("tick unfreeze");
			context.waitTicks(10);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> check(sentinel(minecraft).action() == CydonianSentinelEntity.Action.LANCE,
					"Its Core Lance did not fire: " + sentinel(minecraft).action()));
			shotFrom(context, singleplayer, dim, at, 6.0, 1.0, 3.0, 0.0, 2.0, 3.0, "jugcraft_cydonian_sentinel_lance");

			// Its rule, the Red Storm, called from the west: its arms planted, the dust streaming in, then scouring the crater.
			pose(context, server, at, (sentinel, level, player) -> sentinel.callStorm(level, Wind.WEST), CydonianSentinelEntity.STORM_TELL + 15);
			server.runOnServer(minecraft -> check(sentinel(minecraft).storm() == CydonianSentinelEntity.Storm.BLOWING,
					"The storm from the west did not blow: " + sentinel(minecraft).storm()));
			shotFrom(context, singleplayer, dim, centre, 12.0, 2.0, -1.0, 0.0, 2.5, -2.0, "jugcraft_cydonian_sentinel_storm");
			server.runCommand("tick unfreeze");
			context.waitTicks(CydonianSentinelEntity.STORM_TICKS);
			server.runOnServer(minecraft -> check(sentinel(minecraft).storm() == CydonianSentinelEntity.Storm.CALM, "The storm did not pass"));

			// The Shell Cracks, at six tenths of its health: it roars, and its sentries rise from the small craters.
			pose(context, server, at, (sentinel, level, player) -> sentinel.setHealth(sentinel.getMaxHealth() * CydonianSentinelEntity.CRACK_AT - 1.0F), 14);
			server.runOnServer(minecraft -> {
				CydonianSentinelEntity sentinel = sentinel(minecraft);
				check(sentinel.phase() == CydonianSentinelEntity.Phase.CRACKING && sentinel.action() == CydonianSentinelEntity.Action.ROAR,
						"The Shell did not crack: " + sentinel.phase());
				check(sentinel.sentries().size() == CydonianSentinelEntity.SENTRIES_RISE, sentinel.sentries().size() + " sentries rose, not "
						+ CydonianSentinelEntity.SENTRIES_RISE);
			});
			shotFrom(context, singleplayer, dim, at, 0.0, 7.0, 14.0, 0.0, 1.0, 2.0, "jugcraft_cydonian_sentinel_cracks");
			server.runCommand("tick unfreeze");
			context.waitTicks(CydonianSentinelEntity.CRACK_TICKS + 5);
			server.runOnServer(minecraft -> check(sentinel(minecraft).phase() == CydonianSentinelEntity.Phase.RED_STORM,
					"The Red Storm did not begin: " + sentinel(minecraft).phase()));

			// The Unbound Core, at a quarter of its health: its shell bursts apart and the core floats free, its shards orbiting.
			pose(context, server, at, (sentinel, level, player) -> sentinel.setHealth(sentinel.getMaxHealth() * CydonianSentinelEntity.UNBOUND_AT - 1.0F), 4);
			server.runOnServer(minecraft -> check(sentinel(minecraft).phase() == CydonianSentinelEntity.Phase.BURSTING,
					"Its shell did not burst: " + sentinel(minecraft).phase()));
			server.runCommand("tick unfreeze");
			context.waitTicks(CydonianSentinelEntity.BURST_TICKS + 15);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				CydonianSentinelEntity sentinel = sentinel(minecraft);
				check(sentinel.phase() == CydonianSentinelEntity.Phase.UNBOUND && sentinel.form() == CydonianSentinelEntity.Form.CORE,
						"Its core did not float free: " + sentinel.phase());
			});
			shotFrom(context, singleplayer, dim, at, 4.5, 1.0, 4.0, 0.0, 2.0, 0.0, "jugcraft_cydonian_sentinel_unbound");
			pose(context, server, at, (sentinel, level, player) -> sentinel.begin(level, Attack.SWEEPING_LANCE, player), Attack.SWEEPING_LANCE.windup + 12);
			shotFrom(context, singleplayer, dim, at, -4.0, 4.0, -4.0, 0.0, 1.0, 3.0, "jugcraft_cydonian_sentinel_sweep");
			pose(context, server, at, (sentinel, level, player) -> sentinel.begin(level, Attack.SHARD_BURST, player), Attack.SHARD_BURST.windup + 3);
			server.runOnServer(minecraft -> check(lair(minecraft).getEntitiesOfClass(RustShardEntity.class, slot()).size()
					>= CydonianSentinelEntity.BURST_SHARDS, "No shards burst out all round it"));
			shotFrom(context, singleplayer, dim, at, 5.0, 3.0, 5.0, 0.0, 1.5, 0.0, "jugcraft_cydonian_sentinel_shard_burst");
			pose(context, server, at, (sentinel, level, player) -> sentinel.begin(level, Attack.STARFALL, player),
					Attack.STARFALL.windup - CydonianSentinelEntity.METEOR_FALL + 10);
			server.runOnServer(minecraft -> check(!lair(minecraft).getEntitiesOfClass(SentinelMeteorEntity.class, slot()).isEmpty(),
					"No meteor falls onto the Starfall's marks"));
			shotFrom(context, singleplayer, dim, at, 6.0, 1.0, 9.0, 0.0, 5.0, 5.0, "jugcraft_cydonian_sentinel_starfall");
			server.runCommand("tick unfreeze");
			context.waitTicks(CydonianSentinelEntity.METEOR_FALL);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				CydonianSentinelEntity sentinel = sentinel(minecraft);
				check(sentinel.sunk() && sentinel.glow() == CydonianSentinelEntity.Glow.DIM, "After its Starfall it did not sink, dimmed");
			});
			shotFrom(context, singleplayer, dim, at, 3.5, 0.5, 3.5, 0.0, 0.5, 0.0, "jugcraft_cydonian_sentinel_sunk");
			server.runCommand("tick unfreeze");
			context.waitTicks(Attack.STARFALL.recovery);

			// The Red Giant, below a tenth: the core burns white-hot.
			pose(context, server, at, (sentinel, level, player) -> sentinel.setHealth(sentinel.getMaxHealth() * CydonianSentinelEntity.GIANT_AT - 1.0F), 12);
			server.runOnServer(minecraft -> {
				CydonianSentinelEntity sentinel = sentinel(minecraft);
				check(sentinel.phase() == CydonianSentinelEntity.Phase.RED_GIANT && sentinel.glow() == CydonianSentinelEntity.Glow.GIANT,
						"The Red Giant did not come: " + sentinel.phase());
			});
			shotFrom(context, singleplayer, dim, at, 4.5, 1.5, 4.5, 0.0, 2.8, 0.0, "jugcraft_cydonian_sentinel_red_giant");

			// It falls: the player's loot and advancement, its sentries and things gone with it, and the Red Mist in the
			// crater's middle. Beside the mist, a stand wears the Sentinel's Visor and holds the Corelance.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				CydonianSentinelEntity sentinel = sentinel(minecraft);
				check(SentinelLoot.firstKill(player(minecraft)), "The player had already won Red Planet");
				sentinel.took(player(minecraft), 10.0F);
				sentinel.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			});
			server.runCommand(String.format(Locale.ROOT, "execute in %s run summon minecraft:armor_stand %.1f %.1f %.1f {ShowArms:1b,NoBasePlate:1b,"
					+ "Rotation:[10f,0f],equipment:{head:{id:\"jugcraft:sentinels_visor\",count:1},mainhand:{id:\"jugcraft:corelance\",count:1}}}",
					dim, centre.x + 2.5, centre.y, centre.z + 1.5));
			context.waitTicks(6);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				ServerPlayer player = player(minecraft);
				ServerLevel level = lair(minecraft);
				check(player.getInventory().countItem(JugcraftSentinel.CYDONIAN_SHARD) >= 4, "No Cydonian Shards");
				int trophies = player.getInventory().countItem(JugcraftSentinel.corelance()) + player.getInventory().countItem(JugcraftSentinel.redstarCleaver());
				check(trophies == 1, "A first kill must bring one of its trophies, not " + trophies);
				check(!SentinelLoot.firstKill(player), "Red Planet was not awarded");
				for (BlockPos cell : CydonianSanctum.craterMistCells(o)) {
					check(level.getBlockState(cell).is(JugcraftLairs.RED_MIST), "No Red Mist in the crater's middle at " + cell.subtract(o));
				}
				check(level.getEntitiesOfClass(ShardSentryEntity.class, slot()).isEmpty(), "Its sentries outlived it");
				check(level.getEntitiesOfClass(RustShardEntity.class, slot()).isEmpty(), "Its shards outlived it");
			});
			shotFrom(context, singleplayer, dim, centre, 1.5, 1.0, 7.5, 1.0, 1.5, 0.0, "jugcraft_cydonian_sentinel_defeated");
			server.runCommand("tick unfreeze");
			server.runCommand(String.format(Locale.ROOT, "execute in %s run kill @e[type=minecraft:armor_stand]", dim));
			context.waitTicks(30);
			server.runOnServer(minecraft -> check(lair(minecraft).getEntitiesOfClass(CydonianSentinelEntity.class, slot()).stream()
					.noneMatch(CydonianSentinelEntity::isAlive), "It is still there"));

			// A fresh instance in its slot is placed anew: the Red Mist it left is gone, and, once the player has gone in again
			// (into the gate chamber, far from it), it waits on its plinth again, its core dark.
			server.runOnServer(minecraft -> {
				LairInstance used = Lairs.instance(SANCTUM, 0);
				check(used != null, "Its instance is not open");
				Lairs.close(minecraft, used);
				LairInstance fresh = Lairs.open(minecraft, SANCTUM);
				check(fresh != null && fresh.slot == 0, "No fresh instance opened in its slot");
				ServerLevel level = lair(minecraft);
				for (BlockPos cell : CydonianSanctum.craterMistCells(o)) {
					check(!level.getBlockState(cell).is(JugcraftLairs.RED_MIST), "Its Red Mist outlived its instance at " + cell.subtract(o));
				}
				check(Lairs.enter(player(minecraft), fresh), "The player did not go into the fresh Sanctum");
			});
			context.waitTicks(40);
			server.runOnServer(minecraft -> {
				CydonianSentinelEntity again = sentinel(minecraft);
				check(again.phase() == CydonianSentinelEntity.Phase.WAITING && again.position().distanceTo(again.waiting()) < 0.1
						&& again.glow() == CydonianSentinelEntity.Glow.DARK, "It does not wait on the fresh Sanctum's plinth: " + again.phase()
						+ " at " + again.position());
			});
			System.out.println("[sentinel] client game test: its fight passed end to end");
			server.runOnServer(minecraft -> Lairs.reset());
		}
	}

	@FunctionalInterface
	private interface Posing {
		void pose(CydonianSentinelEntity sentinel, ServerLevel level, ServerPlayer player);
	}

	/**
	 * Unfreezes the world, puts it at {@code spot} facing south and the player five blocks in front of it (so its attacks
	 * land in view), does what {@code posing} asks, waits {@code ticks} for the pose and freezes the world on it. Every
	 * pose waits at least 10 ticks: the client learns where it was put only on the server's next updates, and a frozen
	 * world sends none.
	 */
	private static void pose(ClientGameTestContext context, TestServerContext server, Vec3 spot, Posing posing, int ticks) {
		server.runCommand("tick unfreeze");
		server.runOnServer(minecraft -> {
			ServerLevel level = lair(minecraft);
			CydonianSentinelEntity sentinel = sentinel(minecraft);
			ServerPlayer player = player(minecraft);
			player.teleportTo(spot.x, spot.y, spot.z + 5.0);
			double up = sentinel.form() == CydonianSentinelEntity.Form.CORE ? CydonianSentinelEntity.CORE_HOVER : 0.0;
			sentinel.teleportTo(spot.x, spot.y + up, spot.z);
			sentinel.setYRot(0.0F);
			sentinel.setYBodyRot(0.0F);
			sentinel.setYHeadRot(0.0F);
			posing.pose(sentinel, level, player);
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

	/** A picture from (x, y, z) in the Sanctum's own coordinates (the camera's feet), turned to look at (ax, ay, az) in them. */
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
		return minecraft.getLevel(SANCTUM.dimension);
	}

	/** The first slot's Sanctum, with room round it (and over it, for the meteors). */
	private static AABB slot() {
		BlockPos o = SANCTUM.origin(0);
		return new AABB(o.getX() - 8, o.getY() - 16, o.getZ() - 8, o.getX() + SANCTUM.width + 8, o.getY() + SANCTUM.height + 24,
				o.getZ() + SANCTUM.length + 8);
	}

	/** It, alive, in the first slot's Sanctum. */
	private static CydonianSentinelEntity sentinel(MinecraftServer minecraft) {
		List<CydonianSentinelEntity> found = lair(minecraft).getEntitiesOfClass(CydonianSentinelEntity.class, slot(), CydonianSentinelEntity::isAlive);
		check(!found.isEmpty(), "The Cydonian Sentinel is not in its Sanctum");
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
