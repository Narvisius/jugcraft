package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.ArchiveLecternBlock;
import io.github.jimbozoomer.jugcraft.lair.CryptArchive;
import io.github.jimbozoomer.jugcraft.lair.GraveSealBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Glow;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Rune;
import io.github.jimbozoomer.jugcraft.lair.PhylacteryBlock;
import io.github.jimbozoomer.jugcraft.lair.lich.CryptLichEntity;
import io.github.jimbozoomer.jugcraft.lair.lich.CryptLichLoot;
import io.github.jimbozoomer.jugcraft.lair.lich.JugcraftLich;
import io.github.jimbozoomer.jugcraft.lair.lich.LichBonesEntity;
import io.github.jimbozoomer.jugcraft.lair.lich.RestlessDeadEntity;
import io.github.jimbozoomer.jugcraft.lair.lich.SoulBoltEntity;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Crypt Lich (docs/features/crypt-lich.md), in a real Archive of Last Names: unlike a game-test
 * server's world, it has the lair dimension. His fight from end to end, with the one player in survival (shielded by
 * Resistance, so it can watch every attack): he waits on his throne; standing on the hall's floor wakes him and he comes
 * down the apse's steps, his chains burning and a name lit; the Warded King's attacks, the dead rising from the floor
 * graves and from a dark name's sarcophagus; the three seals broken in their order and the phylactery cracked; the
 * Erasure scraping a name; the Unwritten's attacks; the scraped name read back at the lectern; the Last Name, his crown
 * flaring; and when he falls the player has their loot and Nameless, his dead are gone and Grey Mist stands on the dais
 * where the phylactery stood, and a fresh instance in his slot has none of it and him on his throne again. Along the way,
 * pictures of each part of the fight, the world frozen for each so the pose holds. CI job {@code client}.
 */
public class CryptLichClientGameTests implements FabricClientGameTest {
	private static final Lair ARCHIVE = Lair.CRYPT_ARCHIVE;
	/** A standing player's eyes over their feet, where the camera looks from. */
	private static final double EYE = 1.62;
	/** Where he is posed for his attacks (the template's x, z): on the north lane, facing south down it to the dais. */
	private static final double POSE_X = 32.5;
	private static final double POSE_Z = 22.5;

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

			// An instance opens: the archive is placed and he waits on his throne; the player arrives on the landing.
			server.runOnServer(minecraft -> {
				LairInstance instance = Lairs.open(minecraft, ARCHIVE);
				check(instance != null, "No Archive of Last Names opened");
				check(Lairs.enter(player(minecraft), instance), "The player did not go in");
			});
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos o = ARCHIVE.origin(0);
			String dim = ARCHIVE.dimension.identifier().toString();
			Vec3 centre = CryptLichEntity.centre(o);
			Vec3 at = new Vec3(o.getX() + POSE_X, o.getY() + CryptArchive.STAND, o.getZ() + POSE_Z);
			server.runOnServer(minecraft -> {
				CryptLichEntity lich = lich(minecraft);
				check(lich.phase() == CryptLichEntity.Phase.WAITING, "He is not waiting but " + lich.phase());
				check(lich.position().distanceTo(lich.waiting()) < 0.1, "He is not on his throne but at " + lich.position());
			});
			shot(context, singleplayer, dim, o, 32.5, CryptArchive.STAND + 1.5, 17.5, CryptArchive.WAIT_X, CryptArchive.WAIT_Y + 1.4,
					CryptArchive.WAIT_Z, false, "jugcraft_crypt_lich_waiting");

			// Standing on the hall's floor wakes him: the player, in survival on the floor, and his crown lights as he rises
			// from his throne; caught as he rises.
			server.runCommand("effect give @a minecraft:resistance infinite 4 true");
			server.runCommand("effect give @a minecraft:saturation infinite 0 true");
			server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.1f %.3f %.1f 180 0", dim, centre.x + 3.0, centre.y, centre.z + 6.0));
			server.runCommand("gamemode survival @a");
			context.waitTicks(22);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> check(lich(minecraft).phase() == CryptLichEntity.Phase.WAKING,
					"Standing on the hall's floor did not wake him: " + lich(minecraft).phase()));
			shot(context, singleplayer, dim, o, 36.5, CryptArchive.STAND + 1.5, 19.5, 32.5, CryptArchive.APSE_STAND + 1.5, 11.0, true,
					"jugcraft_crypt_lich_wakes");
			server.runCommand("tick unfreeze");
			context.waitTicks(CryptLichEntity.WAKE_TICKS + 10);
			server.runOnServer(minecraft -> {
				CryptLichEntity lich = lich(minecraft);
				ServerLevel level = lair(minecraft);
				check(lich.phase() == CryptLichEntity.Phase.WARDED_KING, "He did not come down to fight: " + lich.phase());
				check(lich.warded() && lich.lit() != null, "His chains do not burn with a name lit");
				check(level.getBlockState(lich.stone(lich.lit())).getValue(NameStoneBlock.GLOW) == Glow.LIT, "The lit name does not burn on its stone");
			});
			// His chains: soul-fire from each seal to the phylactery on the dais, and from it to him; from behind the dais. The
			// player is in creative for it, so he strikes at no one and stands at rest.
			server.runCommand("gamemode creative @a");
			pose(context, server, at, (lich, level, player) -> {
			}, 12);
			shotFrom(context, singleplayer, dim, centre, 4.0, 3.5, 6.0, 0.0, 1.0, -8.0, "jugcraft_crypt_lich_chains");
			server.runCommand("gamemode survival @a");

			// The Warded King: each attack caught as it lands, he on the north lane facing south, the player five blocks in
			// front of him; the camera then stands where it shows the attack best, turned towards the point named.
			pose(context, server, at, (lich, level, player) -> lich.begin(level, CryptLichEntity.Attack.GRAVE_SWEEP, player),
					CryptLichEntity.Attack.GRAVE_SWEEP.windup + 2);
			shotFrom(context, singleplayer, dim, at, 4.5, 1.5, 3.0, 0.0, 1.6, 1.0, "jugcraft_crypt_lich_sweep");
			pose(context, server, at, (lich, level, player) -> lich.begin(level, CryptLichEntity.Attack.BONE_LANES, player),
					CryptLichEntity.Attack.BONE_LANES.windup + 4);
			server.runOnServer(minecraft -> {
				CryptLichEntity lich = lich(minecraft);
				int clumps = lair(minecraft).getEntitiesOfClass(LichBonesEntity.class, slot()).size();
				check(lich.lanes().equals(List.of(Rune.CROWN)), "He did not call the north lane, the player's: " + lich.lanes());
				check(clumps >= 8, "Only " + clumps + " clumps of bones burst up the north lane");
			});
			shotFrom(context, singleplayer, dim, at, -6.5, 3.5, 3.0, 0.0, 0.3, 1.0, "jugcraft_crypt_lich_lanes");
			pose(context, server, at, (lich, level, player) -> lich.begin(level, CryptLichEntity.Attack.SOUL_BOLT, player),
					CryptLichEntity.Attack.SOUL_BOLT.windup - 4);
			// His bolt is in the air for under a second, and the server can run a few ticks ahead of the client's count: the
			// world runs on until a bolt has drifted a few ticks, and the server freezes it in the step that sees it.
			server.runCommand("tick unfreeze");
			boolean drifting = false;
			for (int wait = 0; wait < 40 && !drifting; wait++) {
				context.waitTick();
				drifting = server.computeOnServer(minecraft -> {
					boolean loosed = lair(minecraft).getEntitiesOfClass(SoulBoltEntity.class, slot()).stream().anyMatch(bolt -> bolt.tickCount >= 4);
					if (loosed) {
						minecraft.tickRateManager().setFrozen(true);
					}
					return loosed;
				});
			}
			check(drifting, "No Soul Bolt drifts from his hand");
			shotFrom(context, singleplayer, dim, at, 4.0, 1.2, 2.5, 0.0, 1.8, 2.0, "jugcraft_crypt_lich_bolt");
			pose(context, server, at, (lich, level, player) -> lich.begin(level, CryptLichEntity.Attack.RAISE_DEAD, player),
					CryptLichEntity.Attack.RAISE_DEAD.windup + 8);
			server.runOnServer(minecraft -> check(lich(minecraft).dead().size() == CryptLichEntity.RAISED,
					lich(minecraft).dead().size() + " dead rose from the graves, not " + CryptLichEntity.RAISED));
			shotFrom(context, singleplayer, dim, at, 0.0, 3.5, 11.0, 0.0, 0.5, 2.5, "jugcraft_crypt_lich_dead");
			// A dark name's seal holds and raises one from that alcove's sarcophagus; then his dead are cleared away, so they
			// do not crowd the pictures after.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> {
				CryptLichEntity lich = lich(minecraft);
				ServerLevel level = lair(minecraft);
				Rune dark = lich.order().get(2);
				int before = lich.dead().size();
				boolean answered = lich.sealUsed(level, lich.seal(dark), level.getBlockState(lich.seal(dark)), player(minecraft));
				check(answered && lich.dead().size() == before + 1 && !level.getBlockState(lich.seal(dark)).getValue(GraveSealBlock.BROKEN),
						"A dark name's seal did not hold and raise the dead");
				Vec3 foot = CryptArchive.sarcophagusDead(dark).add(o.getX(), o.getY(), o.getZ());
				check(level.getEntitiesOfClass(RestlessDeadEntity.class, new AABB(foot, foot).inflate(0.5)).size() == 1,
						"No Restless Dead climbs out of the dark name's sarcophagus");
				level.getEntitiesOfClass(RestlessDeadEntity.class, slot()).forEach(Entity::discard);
			});

			// The three seals broken in their order: the phylactery cracks and he is exposed, staggered.
			Vec3 near = centre.add(0.0, 0.0, -7.0);
			pose(context, server, near, (lich, level, player) -> {
				for (int i = 0; i < 3; i++) {
					lich.breakSeal(level, lich.lit());
				}
			}, 12);
			server.runOnServer(minecraft -> {
				CryptLichEntity lich = lich(minecraft);
				check(lich.ward() == CryptLichEntity.Ward.EXPOSED && !lich.warded(), "With the three seals broken he is not exposed");
				check(lair(minecraft).getBlockState(lich.phylactery()).getValue(PhylacteryBlock.CRACKED), "The phylactery did not crack");
			});
			shotFrom(context, singleplayer, dim, centre, 4.5, 1.5, 3.0, 0.0, 1.0, -4.0, "jugcraft_crypt_lich_exposed");

			// The Erasure, at half his health: he shrieks, his chains reforge at once, and the name that comes last is scraped.
			pose(context, server, at, (lich, level, player) -> lich.setHealth(lich.getMaxHealth() * CryptLichEntity.ERASE_AT), 14);
			server.runOnServer(minecraft -> {
				CryptLichEntity lich = lich(minecraft);
				check(lich.phase() == CryptLichEntity.Phase.ERASURE && lich.action() == CryptLichEntity.Action.SHRIEK, "The Erasure did not come");
				check(lich.scraped() != null && lair(minecraft).getBlockState(lich.stone(lich.scraped())).getValue(NameStoneBlock.GLOW) == Glow.ERASED,
						"No name was scraped from its stone");
			});
			shotFrom(context, singleplayer, dim, at, 3.5, 1.0, 4.5, 0.0, 2.0, 0.0, "jugcraft_crypt_lich_erasure");
			server.runCommand("tick unfreeze");
			context.waitTicks(CryptLichEntity.ERASE_TICKS);
			server.runOnServer(minecraft -> check(lich(minecraft).phase() == CryptLichEntity.Phase.UNWRITTEN,
					"The Unwritten did not begin: " + lich(minecraft).phase()));

			// The Unwritten: his Soul Drain and his Grave Pull.
			pose(context, server, at, (lich, level, player) -> lich.begin(level, CryptLichEntity.Attack.SOUL_DRAIN, player),
					CryptLichEntity.Attack.SOUL_DRAIN.windup + 12);
			server.runOnServer(minecraft -> check(lich(minecraft).attack() == CryptLichEntity.Attack.SOUL_DRAIN, "His Soul Drain broke"));
			shotFrom(context, singleplayer, dim, at, 4.5, 1.0, 2.0, 0.0, 1.6, 2.5, "jugcraft_crypt_lich_drain");
			pose(context, server, at, (lich, level, player) -> lich.begin(level, CryptLichEntity.Attack.GRAVE_PULL, player),
					CryptLichEntity.Attack.GRAVE_PULL.windup + CryptLichEntity.PULL_BURST + 3);
			server.runOnServer(minecraft -> check(lair(minecraft).getEntitiesOfClass(LichBonesEntity.class, slot()).size() >= 6,
					"No bones burst round his feet"));
			shotFrom(context, singleplayer, dim, at, 5.0, 3.0, 4.0, 0.0, 0.3, 0.0, "jugcraft_crypt_lich_pull");

			// The scraped name's turn: its seal holds until the player reads the names back at the lectern; then it breaks.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> {
				CryptLichEntity lich = lich(minecraft);
				ServerLevel level = lair(minecraft);
				ServerPlayer player = player(minecraft);
				Rune scraped = lich.scraped();
				check(scraped != null, "No name is scraped in the Unwritten");
				List<Rune> order = lich.order();
				lich.breakSeal(level, order.get(0));
				lich.breakSeal(level, order.get(1));
				check(lich.lit() == scraped, "The scraped name's turn did not come");
				lich.sealUsed(level, lich.seal(scraped), level.getBlockState(lich.seal(scraped)), player);
				check(!level.getBlockState(lich.seal(scraped)).getValue(GraveSealBlock.BROKEN), "The scraped name's seal broke before it was read back");
				List<Rune> restored = ArchiveLecternBlock.read(level, lich.lectern(), player);
				check(restored.equals(List.of(scraped)) && lich.scraped() == null
						&& level.getBlockState(lich.stone(scraped)).getValue(NameStoneBlock.GLOW) == Glow.LIT, "Reading the names back did not restore it");
				lich.sealUsed(level, lich.seal(scraped), level.getBlockState(lich.seal(scraped)), player);
				check(lich.ward() == CryptLichEntity.Ward.EXPOSED, "Read back, the scraped name's seal did not break and expose him");
			});

			// The Last Name, at a fifth of his health: the phylactery shatters, his chains are gone and his crown flares.
			pose(context, server, at, (lich, level, player) -> lich.setHealth(lich.getMaxHealth() * 0.15F), 14);
			server.runOnServer(minecraft -> {
				CryptLichEntity lich = lich(minecraft);
				check(lich.phase() == CryptLichEntity.Phase.LAST_NAME && lich.ward() == CryptLichEntity.Ward.GONE
						&& lich.crown() == CryptLichEntity.Crown.FLARE, "The Last Name did not come");
			});
			shotFrom(context, singleplayer, dim, at, 2.5, 0.8, 4.0, 0.0, 2.2, 0.0, "jugcraft_crypt_lich_last_name");

			// He falls: the player's loot and advancement, his dead and things gone with him, and Grey Mist on the dais where
			// the phylactery stood. Caught as he falls.
			server.runCommand("tick unfreeze");
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				CryptLichEntity lich = lich(minecraft);
				check(CryptLichLoot.firstKill(player(minecraft)), "The player had already won Nameless");
				lich.took(player(minecraft), 10.0F);
				lich.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			});
			context.waitTicks(6);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				ServerPlayer player = player(minecraft);
				ServerLevel level = lair(minecraft);
				check(player.getInventory().countItem(JugcraftLich.GRAVE_DUST) >= 3, "No Grave Dust");
				int trophies = player.getInventory().countItem(JugcraftLich.soulreaver()) + player.getInventory().countItem(JugcraftLich.gravewarden());
				check(trophies == 1, "A first kill must bring one of his trophies, not " + trophies);
				check(!CryptLichLoot.firstKill(player), "Nameless was not awarded");
				for (BlockPos cell : CryptArchive.exitCells()) {
					check(level.getBlockState(cell.offset(o)).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist on the dais at " + cell);
				}
				check(level.getEntitiesOfClass(RestlessDeadEntity.class, slot()).isEmpty(), "His dead outlived him");
				check(level.getEntitiesOfClass(SoulBoltEntity.class, slot()).isEmpty(), "His Soul Bolts outlived him");
			});
			shotFrom(context, singleplayer, dim, centre, 3.0, 1.0, 7.5, 0.0, 1.5, 0.0, "jugcraft_crypt_lich_defeated");
			server.runCommand("tick unfreeze");
			context.waitTicks(30);
			server.runOnServer(minecraft -> check(lair(minecraft).getEntitiesOfClass(CryptLichEntity.class, slot()).stream()
					.noneMatch(CryptLichEntity::isAlive), "He is still there"));

			// A fresh instance in his slot is placed anew: the Grey Mist he left is gone with the old one, the phylactery is
			// whole on its plinth, and, once the player has gone in again (onto the landing, far from him), he waits on his
			// throne again.
			server.runOnServer(minecraft -> {
				LairInstance used = Lairs.instance(ARCHIVE, 0);
				check(used != null, "His instance is not open");
				Lairs.close(minecraft, used);
				LairInstance fresh = Lairs.open(minecraft, ARCHIVE);
				check(fresh != null && fresh.slot == 0, "No fresh instance opened in his slot");
				ServerLevel level = lair(minecraft);
				for (BlockPos cell : CryptArchive.exitCells()) {
					check(!level.getBlockState(cell.offset(o)).is(JugcraftLairs.LAIR_EXIT), "His Grey Mist outlived his instance at " + cell);
				}
				BlockPos urn = o.offset(CryptArchive.PHYLACTERY);
				check(level.getBlockState(urn).is(JugcraftLairs.PHYLACTERY) && !level.getBlockState(urn).getValue(PhylacteryBlock.CRACKED),
						"The fresh archive's phylactery is not whole");
				for (Rune rune : Rune.values()) {
					check(!level.getBlockState(CryptArchive.seal(o, rune)).getValue(GraveSealBlock.BROKEN), "A seal of the fresh archive is broken");
				}
				check(Lairs.enter(player(minecraft), fresh), "The player did not go into the fresh archive");
			});
			context.waitTicks(40);
			server.runOnServer(minecraft -> {
				CryptLichEntity again = lich(minecraft);
				check(again.phase() == CryptLichEntity.Phase.WAITING && again.position().distanceTo(again.waiting()) < 0.1,
						"He does not wait on the fresh archive's throne: " + again.phase() + " at " + again.position());
			});
			System.out.println("[lich] client game test: his fight passed end to end");
			server.runOnServer(minecraft -> Lairs.reset());
		}
	}

	@FunctionalInterface
	private interface Posing {
		void pose(CryptLichEntity lich, ServerLevel level, ServerPlayer player);
	}

	/**
	 * Unfreezes the world, puts him at {@code spot} facing south and the player five blocks in front of him (so his attacks
	 * land in view), does what {@code posing} asks, waits {@code ticks} for the pose and freezes the world on it.
	 * Every pose waits at least 10 ticks: the client learns where he was put only on the server's next updates, and a
	 * frozen world sends none.
	 */
	private static void pose(ClientGameTestContext context, TestServerContext server, Vec3 spot, Posing posing, int ticks) {
		server.runCommand("tick unfreeze");
		server.runOnServer(minecraft -> {
			ServerLevel level = lair(minecraft);
			CryptLichEntity lich = lich(minecraft);
			ServerPlayer player = player(minecraft);
			player.teleportTo(spot.x, spot.y, spot.z + 5.0);
			lich.teleportTo(spot.x, spot.y, spot.z);
			lich.setYRot(0.0F);
			lich.setYBodyRot(0.0F);
			lich.setYHeadRot(0.0F);
			posing.pose(lich, level, player);
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

	/** A picture from (x, y, z) in the archive's own coordinates (the camera's feet), turned to look at (ax, ay, az) in them. */
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
		return minecraft.getLevel(ARCHIVE.dimension);
	}

	/** The first slot's archive, with room round it. */
	private static AABB slot() {
		BlockPos o = ARCHIVE.origin(0);
		return new AABB(o.getX() - 8, o.getY() - 16, o.getZ() - 8, o.getX() + ARCHIVE.width + 8, o.getY() + ARCHIVE.height + 8,
				o.getZ() + ARCHIVE.length + 8);
	}

	/** He, alive, in the first slot's archive. */
	private static CryptLichEntity lich(MinecraftServer minecraft) {
		List<CryptLichEntity> found = lair(minecraft).getEntitiesOfClass(CryptLichEntity.class, slot(), CryptLichEntity::isAlive);
		check(!found.isEmpty(), "The Crypt Lich is not in his Archive of Last Names");
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
