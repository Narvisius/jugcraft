package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairInstance;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.SluiceGateBlock;
import io.github.jimbozoomer.jugcraft.lair.TroughStoneBlock;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderKiln;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderTyrantEntity;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderTyrantLoot;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderlingEntity;
import io.github.jimbozoomer.jugcraft.lair.tyrant.FallingCinderEntity;
import io.github.jimbozoomer.jugcraft.lair.tyrant.JugcraftTyrant;
import io.github.jimbozoomer.jugcraft.lair.tyrant.MagmaGobEntity;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client game test for the Cinder Tyrant (docs/features/cinder-tyrant.md), in a real Cinder Kiln: unlike a game-test
 * server's world, it has the lair dimension. His fight from end to end, with the one player in survival (shielded by
 * Resistance, so it can watch every attack): he waits sunk in his crucible, only his crest over the slag; stepping down
 * onto the bowl's floor wakes him and he rises out of the slag and crawls out; the Kiln's attacks; a Body Slam baited
 * into a flooded trough quenches him; the Eruption from the forge's lip, the channel's surge and a sluice choked with
 * slag; the Cinder Rain and the Lava Wave; the Molten Heart, his cracks blazing white; and when he falls the player has
 * their loot and Tempered, the crucible has cooled to obsidian, the sluices are clear, his Cinderlings are gone and Grey
 * Mist stands in the bowl's middle, and a fresh instance in his slot has none of it. Along the way, pictures of each part
 * of the fight, the world frozen for each so the pose holds. CI job {@code client}.
 */
public class CinderTyrantClientGameTests implements FabricClientGameTest {
	private static final Lair KILN = Lair.CINDER_KILN;
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

			// An instance opens: the kiln is placed and he waits sunk in his crucible, only his crest over the slag.
			server.runOnServer(minecraft -> {
				LairInstance instance = Lairs.open(minecraft, KILN);
				check(instance != null, "No Cinder Kiln opened");
				check(Lairs.enter(player(minecraft), instance), "The player did not go in");
			});
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			BlockPos o = KILN.origin(0);
			String kiln = KILN.dimension.identifier().toString();
			Vec3 centre = CinderTyrantEntity.centre(o);
			// His crucible, from the bowl's centre (in its own coordinates): north, the slag's surface a block over the floor.
			double crucibleZ = CinderKiln.CRUCIBLE_Z - CinderKiln.BOWL_Z;
			double sunkZ = CinderKiln.SUNK_Z - CinderKiln.BOWL_Z;
			server.runOnServer(minecraft -> {
				CinderTyrantEntity tyrant = tyrant(minecraft);
				check(tyrant.phase() == CinderTyrantEntity.Phase.WAITING, "He is not waiting but " + tyrant.phase());
				check(tyrant.position().distanceTo(tyrant.sunk()) < 0.1, "He is not sunk in his crucible but at " + tyrant.position());
			});
			shot(context, singleplayer, kiln, centre, 2.0, 0.5, sunkZ + 6.0, 0.0, 0.3, sunkZ, false, "jugcraft_cinder_tyrant_sunk");

			// Stepping down onto the bowl's floor wakes him: the player, in survival in the bowl's middle, and he rises out of
			// the slag; caught as he rises.
			server.runCommand("effect give @a minecraft:resistance infinite 4 true");
			server.runCommand("effect give @a minecraft:saturation infinite 0 true");
			server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.1f %.2f %.1f 180 0", kiln, centre.x + 0.5, centre.y, centre.z));
			server.runCommand("gamemode survival @a");
			context.waitTicks(16);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> check(tyrant(minecraft).phase() == CinderTyrantEntity.Phase.WAKING,
					"Stepping onto the floor did not wake him: " + tyrant(minecraft).phase()));
			shot(context, singleplayer, kiln, centre, 2.0, 0.5, sunkZ + 6.0, 0.0, 1.2, sunkZ, true, "jugcraft_cinder_tyrant_wakes");
			server.runCommand("tick unfreeze");
			context.waitTicks(CinderTyrantEntity.WAKE_TICKS);
			server.runOnServer(minecraft -> {
				CinderTyrantEntity tyrant = tyrant(minecraft);
				check(tyrant.phase() == CinderTyrantEntity.Phase.KILN, "He did not crawl out to fight: " + tyrant.phase());
				check(Math.abs(tyrant.getY() - tyrant.floorY()) < 0.1, "He is not on the floor but at " + tyrant.getY());
			});

			// The Kiln, in the bowl: each attack caught as it lands, he in the middle of the bowl facing south, the player
			// five blocks in front of him; the camera then stands where it shows the attack best, turned towards the point
			// named.
			pose(context, server, centre, (tyrant, level, player) -> tyrant.begin(level, CinderTyrantEntity.Attack.TAIL_SWEEP, player),
					CinderTyrantEntity.Attack.TAIL_SWEEP.windup + 2);
			shot(context, singleplayer, kiln, centre, 7.0, 1.0, 1.0, 0.0, 1.0, 0.0, true, "jugcraft_cinder_tyrant_tail_sweep");
			pose(context, server, centre, (tyrant, level, player) -> tyrant.begin(level, CinderTyrantEntity.Attack.EMBER_SPIT, player),
					CinderTyrantEntity.Attack.EMBER_SPIT.windup + 3);
			server.runOnServer(minecraft -> {
				int gobs = lair(minecraft).getEntitiesOfClass(MagmaGobEntity.class, slot()).size();
				check(gobs >= 1, "No gobs of magma are flying");
			});
			shot(context, singleplayer, kiln, centre, 7.0, 1.0, 3.0, 0.0, 1.5, 3.0, true, "jugcraft_cinder_tyrant_ember_spit");
			pose(context, server, centre, (tyrant, level, player) -> tyrant.begin(level, CinderTyrantEntity.Attack.BODY_SLAM, player),
					CinderTyrantEntity.Attack.BODY_SLAM.windup + 8);
			shot(context, singleplayer, kiln, centre, 8.0, 1.0, 2.5, 0.0, 2.5, 2.5, true, "jugcraft_cinder_tyrant_body_slam");
			// He comes down before the next pose.
			server.runCommand("tick unfreeze");
			context.waitTicks(CinderTyrantEntity.Attack.BODY_SLAM.active);
			pose(context, server, centre, (tyrant, level, player) -> tyrant.begin(level, CinderTyrantEntity.Attack.KILN_BREATH, player),
					CinderTyrantEntity.Attack.KILN_BREATH.windup + 8);
			shot(context, singleplayer, kiln, centre, 6.5, 1.0, 3.0, 0.0, 1.0, 3.0, true, "jugcraft_cinder_tyrant_kiln_breath");
			pose(context, server, centre, (tyrant, level, player) -> tyrant.begin(level, CinderTyrantEntity.Attack.MANTLE_SHED, player),
					CinderTyrantEntity.Attack.MANTLE_SHED.windup + 6);
			server.runOnServer(minecraft -> check(!tyrant(minecraft).cinderlings().isEmpty(), "No Cinderlings crawled off his mantle"));
			shot(context, singleplayer, kiln, centre, 5.0, 1.0, 6.0, 0.0, 0.5, 0.0, true, "jugcraft_cinder_tyrant_cinderlings");

			// His rule: the south sluice turned, the player stands in its flooded trough, and his Body Slam comes down in it.
			// The water bursts into steam and he is quenched, his seams dark.
			double trough = 11.0;
			pose(context, server, centre, (tyrant, level, player) -> {
				check(SluiceGateBlock.turn(level, tyrant.wheels().get(2), null) == SluiceGateBlock.Turn.OPENED, "The south sluice did not open");
				player.teleportTo(centre.x, centre.y, centre.z + trough);
				tyrant.begin(level, CinderTyrantEntity.Attack.BODY_SLAM, player);
			}, CinderTyrantEntity.Attack.BODY_SLAM.windup + CinderTyrantEntity.Attack.BODY_SLAM.active + 3);
			server.runOnServer(minecraft -> {
				CinderTyrantEntity tyrant = tyrant(minecraft);
				check(tyrant.heat() == CinderTyrantEntity.Heat.QUENCHED, "His slam into the flooded trough did not quench him: " + tyrant.heat());
				check(tyrant.taken() == CinderTyrantEntity.QUENCH_TAKEN, "Quenched, blows do not do more");
			});
			shot(context, singleplayer, kiln, centre, 6.0, 1.5, trough - 4.0, 0.0, 1.5, trough, true, "jugcraft_cinder_tyrant_quenched");

			// The Eruption, at half his health: he leaps up onto the forge's lip and roars, the heat channel surges over its
			// banks and Cinderlings crawl out on them; then he comes down, and a sluice is choked with slag.
			double lipZ = CinderKiln.LIP_Z - CinderKiln.BOWL_Z;
			double lipUp = CinderKiln.LIP + 1 - (CinderKiln.BOWL + 1);
			pose(context, server, centre, (tyrant, level, player) -> {
				tyrant.setHealth(tyrant.getMaxHealth() * 0.5F);
				tyrant.startEruption(level);
			}, CinderTyrantEntity.ERUPT_LEAP + 10);
			server.runOnServer(minecraft -> {
				CinderTyrantEntity tyrant = tyrant(minecraft);
				check(tyrant.phase() == CinderTyrantEntity.Phase.ERUPTING, "He is not erupting but " + tyrant.phase());
				Vec3 lip = CinderKiln.lip().add(o.getX(), o.getY(), o.getZ());
				check(tyrant.position().distanceTo(lip) < 0.5, "He is not on the forge's lip but at " + tyrant.position());
			});
			shot(context, singleplayer, kiln, centre, 4.0, 1.5, crucibleZ + 2.0, 0.0, lipUp + 1.5, lipZ, true, "jugcraft_cinder_tyrant_eruption");
			server.runCommand("tick unfreeze");
			context.waitTicks(CinderTyrantEntity.SURGE_WARN + 4);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> check(!tyrant(minecraft).surged().isEmpty(), "The heat channel did not surge"));
			double channelZ = (CinderKiln.CHANNEL_NORTH + CinderKiln.CHANNEL_SOUTH) / 2.0 + 0.5 - CinderKiln.BOWL_Z;
			shot(context, singleplayer, kiln, centre, 6.0, 3.0, channelZ + 6.0, 0.0, 0.0, channelZ, true, "jugcraft_cinder_tyrant_surge");
			// He comes down a few ticks after the Eruption's end (it is ERUPT_LEAP + ERUPT_TICKS + ERUPT_LEAP ticks long, and
			// its pose and the surge's wait took ERUPT_LEAP + 10 and SURGE_WARN + 4 of them), and the choke falls on the
			// first sluice, the west one.
			server.runCommand("tick unfreeze");
			context.waitTicks(CinderTyrantEntity.ERUPT_TICKS + CinderTyrantEntity.ERUPT_LEAP - 10 - CinderTyrantEntity.SURGE_WARN - 4 + 3);
			server.runCommand("tick freeze");
			server.runOnServer(minecraft -> {
				CinderTyrantEntity tyrant = tyrant(minecraft);
				check(tyrant.phase() == CinderTyrantEntity.Phase.ERUPTION, "He did not come down from the lip: " + tyrant.phase());
				check(Math.abs(tyrant.getY() - tyrant.floorY()) < 0.1, "He is not on the floor but at " + tyrant.getY());
				check(tyrant.choked() == 0, "The west sluice is not the one choked: " + tyrant.choked());
				BlockPos wheel = tyrant.wheels().getFirst();
				check(lair(minecraft).getBlockState(wheel).getValue(SluiceGateBlock.FLOW) == SluiceGateBlock.Flow.CHOKED, "The west sluice is not choked");
				check(SluiceGateBlock.turn(lair(minecraft), wheel, null) == SluiceGateBlock.Turn.CHOKED, "The choked sluice turned");
			});
			// The choked sluice, from the bowl before it (where the Cinder Kiln's own picture of it stands).
			shoot(context, singleplayer, kiln, o.getX() + 17.5, o.getY() + CinderKiln.BOWL + 2, o.getZ() + 41.5, 112, 10, true,
					"jugcraft_cinder_tyrant_choked_sluice");

			// The Eruption's attacks: cinders shaken from the vent onto the player, and the Lava Wave rolling out from him.
			pose(context, server, centre, (tyrant, level, player) -> tyrant.begin(level, CinderTyrantEntity.Attack.CINDER_RAIN, player),
					CinderTyrantEntity.Attack.CINDER_RAIN.windup + 10);
			server.runOnServer(minecraft -> {
				int falling = lair(minecraft).getEntitiesOfClass(FallingCinderEntity.class, slot()).size();
				check(falling >= 2, "Only " + falling + " cinders are falling");
			});
			shot(context, singleplayer, kiln, centre, 8.0, 1.0, 7.0, 0.5, 4.0, 5.0, true, "jugcraft_cinder_tyrant_cinder_rain");
			pose(context, server, centre, (tyrant, level, player) -> tyrant.begin(level, CinderTyrantEntity.Attack.LAVA_WAVE, player),
					CinderTyrantEntity.Attack.LAVA_WAVE.windup + 16);
			shot(context, singleplayer, kiln, centre, 0.0, 6.0, 15.0, 0.0, 0.0, 1.0, true, "jugcraft_cinder_tyrant_lava_wave");

			// The Molten Heart, below a fifth of his health: white-hot cores swelling from his seams, seen from above his back.
			pose(context, server, centre, (tyrant, level, player) -> {
				tyrant.setHealth(tyrant.getMaxHealth() * 0.15F);
				tyrant.startHeart(level);
			}, 12);
			AtomicReference<Vec3> molten = new AtomicReference<>();
			AtomicReference<Vec3> facing = new AtomicReference<>();
			server.runOnServer(minecraft -> {
				CinderTyrantEntity tyrant = tyrant(minecraft);
				check(tyrant.molten() && tyrant.phase() == CinderTyrantEntity.Phase.HEART, "He is not in the Molten Heart");
				molten.set(tyrant.position());
				facing.set(tyrant.facing());
			});
			Vec3 side = new Vec3(-facing.get().z, 0.0, facing.get().x);
			Vec3 back = molten.get().add(0.0, 2.0, 0.0);
			Vec3 above = molten.get().add(facing.get().scale(4.0)).add(side.scale(3.0)).add(0.0, 3.0, 0.0);
			shoot(context, singleplayer, kiln, above.x, above.y, above.z, aimYaw(above, back), aimPitch(above, back), true,
					"jugcraft_cinder_tyrant_molten_heart");
			server.runCommand("tick unfreeze");

			// He falls: the player's loot and advancement, the crucible cooled to obsidian, the sluices clear and the troughs
			// dry, the surge ebbed, his Cinderlings gone with him, and Grey Mist in the bowl's middle.
			BlockPos crucibleTop = BlockPos.containing(CinderKiln.CRUCIBLE_X, CinderKiln.BOWL, CinderKiln.CRUCIBLE_Z).offset(o);
			server.runOnServer(minecraft -> {
				ServerLevel level = lair(minecraft);
				CinderTyrantEntity tyrant = tyrant(minecraft);
				check(CinderTyrantLoot.firstKill(player(minecraft)), "The player had already won Tempered");
				check(level.getBlockState(crucibleTop).is(JugcraftLairs.MOLTEN_SLAG), "No slag in his crucible as he falls");
				check(!tyrant.spill(level).isEmpty(), "The channel did not spill as he falls");
				tyrant.took(player(minecraft), 10.0F);
				tyrant.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			});
			context.waitTicks(10);
			server.runOnServer(minecraft -> {
				ServerPlayer player = player(minecraft);
				ServerLevel level = lair(minecraft);
				check(player.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) >= 3, "No Tyrant Scales");
				int trophies = player.getInventory().countItem(JugcraftTyrant.cinderbrand()) + player.getInventory().countItem(JugcraftTyrant.magmaw());
				check(trophies == 1, "A first kill must bring one of his trophies, not " + trophies);
				check(!CinderTyrantLoot.firstKill(player), "Tempered was not awarded");
				check(level.getBlockState(crucibleTop).is(Blocks.OBSIDIAN), "His crucible did not cool to obsidian");
				for (BlockPos at : CinderTyrantEntity.exitCells(centre)) {
					check(level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT), "No Grey Mist in the bowl's middle at " + at);
				}
				for (BlockPos at : CinderKiln.WHEELS) {
					BlockPos gateWheel = at.offset(o);
					check(level.getBlockState(gateWheel).getValue(SluiceGateBlock.FLOW) == SluiceGateBlock.Flow.READY, "A sluice is not clear at " + gateWheel);
				}
				BlockPos southTrough = BlockPos.containing(CinderKiln.BOWL_X, CinderKiln.BOWL, CinderKiln.BOWL_Z + trough).offset(o);
				check(!level.getBlockState(southTrough).getValue(TroughStoneBlock.FLOODED), "The south trough is still flooded");
				check(level.getEntitiesOfClass(CinderTyrantEntity.class, slot()).stream().noneMatch(CinderTyrantEntity::isAlive), "He is still there");
				check(level.getEntitiesOfClass(CinderlingEntity.class, slot()).isEmpty(), "His Cinderlings outlived him");
				BlockPos bank = BlockPos.containing(CinderKiln.bank(true)).below().offset(o);
				check(!level.getBlockState(bank).is(JugcraftLairs.MOLTEN_SLAG), "The surge did not ebb as he fell");
			});
			shot(context, singleplayer, kiln, centre, 0.0, 1.5, 8.0, 0.0, 0.0, crucibleZ, false, "jugcraft_cinder_tyrant_defeated");
			// A fresh instance in his slot is placed anew: the Grey Mist he left is gone with the old one, and the crucible is
			// molten again.
			server.runOnServer(minecraft -> {
				LairInstance used = Lairs.instance(KILN, 0);
				check(used != null, "His instance is not open");
				Lairs.close(minecraft, used);
				LairInstance fresh = Lairs.open(minecraft, KILN);
				check(fresh != null && fresh.slot == 0, "No fresh instance opened in his slot");
				ServerLevel level = lair(minecraft);
				for (BlockPos at : CinderTyrantEntity.exitCells(centre)) {
					check(!level.getBlockState(at).is(JugcraftLairs.LAIR_EXIT), "His Grey Mist outlived his instance at " + at);
				}
				check(level.getBlockState(crucibleTop).is(JugcraftLairs.MOLTEN_SLAG), "The fresh kiln's crucible is not molten");
			});
			System.out.println("[tyrant] client game test: his fight passed end to end");
			server.runOnServer(minecraft -> Lairs.reset());
		}
	}

	@FunctionalInterface
	private interface Posing {
		void pose(CinderTyrantEntity tyrant, ServerLevel level, ServerPlayer player);
	}

	/**
	 * Unfreezes the world, puts him in the middle of the bowl facing south and the player five blocks in front of him (so
	 * his attacks land in view), does what {@code posing} asks, waits {@code ticks} for the pose and freezes the world on
	 * it. Every pose waits at least 10 ticks: the client learns where he was put only on the server's next updates, and a
	 * frozen world sends none.
	 */
	private static void pose(ClientGameTestContext context, TestServerContext server, Vec3 centre, Posing posing, int ticks) {
		server.runCommand("tick unfreeze");
		server.runOnServer(minecraft -> {
			ServerLevel level = lair(minecraft);
			CinderTyrantEntity tyrant = tyrant(minecraft);
			ServerPlayer player = player(minecraft);
			player.teleportTo(centre.x + 0.5, centre.y, centre.z + 5.0);
			tyrant.teleportTo(centre.x, tyrant.floorY(), centre.z);
			tyrant.setYRot(0.0F);
			tyrant.setYBodyRot(0.0F);
			tyrant.setYHeadRot(0.0F);
			posing.pose(tyrant, level, player);
		});
		context.waitTicks(Math.max(10, ticks));
		server.runCommand("tick freeze");
	}

	/**
	 * A picture from ({@code dx}, {@code up}, {@code dz}) off the bowl's centre (the camera's feet), turned to look at
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
		return minecraft.getLevel(KILN.dimension);
	}

	/** The first slot's kiln, with room round it. */
	private static AABB slot() {
		BlockPos o = KILN.origin(0);
		return new AABB(o.getX() - 8, o.getY() - 16, o.getZ() - 8, o.getX() + KILN.width + 8, o.getY() + KILN.height + 8, o.getZ() + KILN.length + 8);
	}

	/** He, alive, in the first slot's kiln. */
	private static CinderTyrantEntity tyrant(MinecraftServer minecraft) {
		List<CinderTyrantEntity> found = lair(minecraft).getEntitiesOfClass(CinderTyrantEntity.class, slot(), CinderTyrantEntity::isAlive);
		check(!found.isEmpty(), "The Cinder Tyrant is not in his Cinder Kiln");
		return found.getFirst();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	/**
	 * Stands the camera at (x, y, z) in {@code dimension}, looking along yaw and pitch, its burning put out, waits for the
	 * world to draw, and shoots.
	 */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, String dimension, double x, double y,
			double z, int yaw, int pitch, boolean frozen, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runOnServer(minecraft -> {
			// Flying, so the camera stays where it is put (set on the server, which tells the client), and its burning put
			// out, so the flames do not cover the picture.
			ServerPlayer player = player(minecraft);
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
			player.clearFire();
		});
		server.runCommand(String.format(Locale.ROOT, "execute in %s run tp @a %.2f %.2f %.2f %d %d", dimension, x, y, z, yaw, pitch));
		context.waitTicks(frozen ? 20 : 30);
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(10);
		context.takeScreenshot(name);
	}
}
