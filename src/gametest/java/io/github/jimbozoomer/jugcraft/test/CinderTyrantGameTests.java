package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.MoltenSlagBlock;
import io.github.jimbozoomer.jugcraft.lair.SluiceGateBlock;
import io.github.jimbozoomer.jugcraft.lair.TroughStoneBlock;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderKiln;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderTyrantEntity;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderTyrantLoot;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderlingEntity;
import io.github.jimbozoomer.jugcraft.lair.tyrant.FallingCinderEntity;
import io.github.jimbozoomer.jugcraft.lair.tyrant.JugcraftTyrant;
import io.github.jimbozoomer.jugcraft.lair.tyrant.MagmaGobEntity;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Cinder Tyrant (docs/features/cinder-tyrant.md), as far as a game-test server can show him: one put down on his own
 * (with no lair instance, over a kiln laid out from the corner of an arena the kiln's size, so his bowl, his crucible,
 * his channel and his sluices' places are all loaded; each test lays the floor, troughs and gates it needs) waits sunk in
 * his crucible until a player's blow wakes him, and crawls out; his health grows with the party, and the Molten Heart
 * shortens his cooldowns and widens his breath; hot he takes half a blow and no fire, quenched more, and his heat comes
 * back; his tail sweeps round his back and flanks and not before him; a Body Slam into a flooded trough quenches him; his
 * breath burns before him and Fire Resistance stops it, the Salamander Charm does not; the Lava Wave passes under whoever
 * is up a block or in the air, once a wave; the channel's surge spills over its banks and ebbs; the choke moves round the
 * sluices, never onto an open one; the Eruption; his gobs, cinders and fire patches; his Cinderlings, gutting out in water
 * and crumbling when he sinks; the Salamander Charm against the slag; and his loot is each participant's own. His fight
 * in the Cinder Kiln itself runs in a real world, in {@link CinderTyrantClientGameTests}.
 *
 * <p>He fights any player within 38 blocks of his bowl's centre; the arena keeps every other test's players farther off
 * than that. Each test sends him away before it ends, with his Cinderlings, and lets the slag ebb.
 */
public class CinderTyrantGameTests {
	/** An arena of air the kiln's width and length (72 x 80), so everything of his fight is loaded and ticking. */
	private static final String KILN = "jugcraft-test:kiln_bowl";
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client, so
	 * a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;
	/** A creature hurt shrugs off a blow no harder than the last for 10 ticks after (vanilla's); a test waits this long between blows. */
	private static final int HURT_COOLDOWN = 12;

	/** The origin of a kiln laid out from the arena's corner, a block under it: its crucible's floor on the arena's. */
	private static BlockPos origin(GameTestHelper helper) {
		return helper.absolutePos(new BlockPos(0, -1, 0));
	}

	/** He waits sunk in the crucible of the kiln laid out from {@code origin}. */
	private static CinderTyrantEntity waiting(GameTestHelper helper, BlockPos origin) {
		return CinderTyrantEntity.summon(helper.getLevel(), origin, null);
	}

	/** The bowl's cracked basalt under the template's cells {@code x0..x1}, {@code z0..z1}, for him and players to stand on. */
	private static void floor(ServerLevel level, BlockPos origin, int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				level.setBlockAndUpdate(origin.offset(x, CinderKiln.BOWL, z), JugcraftLairs.CRACKED_BASALT.defaultBlockState());
			}
		}
	}

	/** Trough stone, flooded or dry, in the bowl's floor under the template's cells {@code x0..x1}, {@code z0..z1}. */
	private static void trough(ServerLevel level, BlockPos origin, int x0, int z0, int x1, int z1, boolean flooded) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				level.setBlockAndUpdate(origin.offset(x, CinderKiln.BOWL, z),
						JugcraftLairs.TROUGH_STONE.defaultBlockState().setValue(TroughStoneBlock.FLOODED, flooded));
			}
		}
	}

	/** A survival player standing {@code dx}, {@code dz} from his bowl's centre, on its floor. */
	private static ServerPlayer player(GameTestHelper helper, CinderTyrantEntity tyrant, double dx, double dz) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 at = tyrant.centre().add(dx, 0.0, dz);
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		return player;
	}

	/** Puts him in the middle of his bowl on its floor, facing south. */
	private static void middle(CinderTyrantEntity tyrant) {
		Vec3 centre = tyrant.centre();
		tyrant.teleportTo(centre.x, tyrant.floorY(), centre.z);
		tyrant.setYRot(0.0F);
	}

	/** Checks {@code body}, then sends him away whether it passed or failed, so a failed test leaves no Tyrant fighting on. */
	private static void ending(ServerLevel level, CinderTyrantEntity tyrant, Runnable body) {
		try {
			body.run();
		} finally {
			for (UUID id : tyrant.cinderlings()) {
				if (level.getEntity(id) instanceof CinderlingEntity ling) {
					ling.discard();
				}
			}
			tyrant.ebb(level);
			tyrant.discard();
		}
	}

	/**
	 * Sunk in his crucible he takes no harm; a player's blow wakes him at full health, and over {@value
	 * CinderTyrantEntity#WAKE_TICKS} ticks he rises out of the slag and crawls out over the crucible's rim onto the floor.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 120)
	public void aBlowWakesHimAndHeCrawlsOut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 31, 30, 40, 42);
		ServerPlayer player = player(helper, tyrant, 0.5, 3.0);
		boolean sunk = tyrant.phase() == CinderTyrantEntity.Phase.WAITING && tyrant.position().distanceTo(tyrant.sunk()) < 0.01
				&& tyrant.action() == CinderTyrantEntity.Action.SUNK;
		boolean harmless = !tyrant.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
		boolean woken = tyrant.phase() == CinderTyrantEntity.Phase.WAKING && tyrant.getHealth() == tyrant.getMaxHealth();
		helper.runAfterDelay(CinderTyrantEntity.WAKE_TICKS + 3, () -> {
			ending(level, tyrant, () -> {
				player.discard();
				Vec3 out = CinderKiln.out().add(origin.getX(), origin.getY(), origin.getZ());
				helper.assertTrue(sunk, "He did not wait sunk in his crucible");
				helper.assertTrue(harmless, "Sunk, he was harmed");
				helper.assertTrue(woken, "A blow did not wake him at full health");
				helper.assertTrue(tyrant.phase() == CinderTyrantEntity.Phase.KILN, "He did not crawl out to fight: " + tyrant.phase());
				helper.assertTrue(Math.abs(tyrant.getY() - tyrant.floorY()) < 0.1, "He is not on the floor but at " + tyrant.getY());
				helper.assertTrue(Math.hypot(tyrant.getX() - out.x, tyrant.getZ() - out.z) < 2.0, "He did not crawl out over the rim but to " + tyrant.position());
			});
			helper.succeed();
		});
	}

	/**
	 * His health grows by half for each player after the first, at most two and a half times; the Molten Heart makes every
	 * cooldown a third shorter and his breath wider.
	 */
	@GameTest(structure = KILN, padding = 8)
	public void hisHealthAndTheMoltenHeart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CinderTyrantEntity tyrant = waiting(helper, origin(helper));
		ending(level, tyrant, () -> {
			helper.assertTrue(CinderTyrantEntity.partyScale(1) == 1.0 && CinderTyrantEntity.partyScale(2) == 1.5
					&& CinderTyrantEntity.partyScale(4) == 2.5 && CinderTyrantEntity.partyScale(9) == 2.5, "His health scales with the party, at most 2.5 times");
			helper.assertTrue(tyrant.cooldown(240) == 240 && tyrant.breathHalfAngle() == CinderTyrantEntity.BREATH_HALF_ANGLE,
					"Before the Molten Heart his cooldowns and his breath are their own");
			tyrant.startHeart(level);
			helper.assertTrue(tyrant.molten() && tyrant.phase() == CinderTyrantEntity.Phase.HEART, "No Molten Heart");
			helper.assertTrue(tyrant.cooldown(240) == 160 && tyrant.cooldown(CinderTyrantEntity.GLOBAL_COOLDOWN) == 13,
					"In the Molten Heart his cooldowns are not a third shorter: " + tyrant.cooldown(240));
			helper.assertTrue(tyrant.breathHalfAngle() == CinderTyrantEntity.HEART_BREATH_HALF_ANGLE, "In the Molten Heart his breath is no wider");
		});
		helper.succeed();
	}

	/**
	 * His rule, the heat is his: hot, a blow does him half its damage, and fire none; quenched, a blow does a quarter more and
	 * he crawls at half his pace; {@value CinderTyrantEntity#QUENCH_TICKS} ticks later his seams flare back over {@value
	 * CinderTyrantEntity#REHEAT_TICKS} and he is hot again.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 300)
	public void theHeatIsHis(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 30, 28, 41, 43);
		tyrant.wake(level);
		helper.runAfterDelay(CinderTyrantEntity.WAKE_TICKS + 2, () -> {
			float before = tyrant.getHealth();
			tyrant.hurtServer(level, level.damageSources().magic(), 10.0F);
			float hot = before - tyrant.getHealth();
			boolean fireproof = !tyrant.hurtServer(level, level.damageSources().hotFloor(), 10.0F);
			tyrant.quench(level);
			boolean quenched = tyrant.heat() == CinderTyrantEntity.Heat.QUENCHED && tyrant.quenchedTicks() == CinderTyrantEntity.QUENCH_TICKS;
			boolean slow = tyrant.speed() == CinderTyrantEntity.SPEED * CinderTyrantEntity.QUENCH_SPEED;
			// The next blow waits out his hurt cooldown: a blow within it does only what it adds to the last.
			helper.runAfterDelay(HURT_COOLDOWN, () -> {
				float warm = tyrant.getHealth();
				tyrant.hurtServer(level, level.damageSources().magic(), 10.0F);
				float cold = warm - tyrant.getHealth();
				helper.runAfterDelay(CinderTyrantEntity.QUENCH_TICKS + 5 - HURT_COOLDOWN, () -> {
					CinderTyrantEntity.Heat flaring = tyrant.heat();
					helper.runAfterDelay(CinderTyrantEntity.REHEAT_TICKS, () -> {
						ending(level, tyrant, () -> {
							helper.assertTrue(Math.abs(hot - 10.0 * CinderTyrantEntity.HOT_TAKEN) < 0.01, "Hot, he took " + hot + " of 10, not half");
							helper.assertTrue(fireproof, "Fire harmed him");
							helper.assertTrue(quenched, "He was not quenched");
							helper.assertTrue(slow, "Quenched, he does not crawl at half his pace");
							helper.assertTrue(Math.abs(cold - 10.0 * CinderTyrantEntity.QUENCH_TAKEN) < 0.01, "Quenched, he took " + cold + " of 10, not a quarter more");
							helper.assertTrue(flaring == CinderTyrantEntity.Heat.REHEATING, "After his quench his seams did not flare back: " + flaring);
							helper.assertTrue(tyrant.heat() == CinderTyrantEntity.Heat.HOT && tyrant.taken() == CinderTyrantEntity.HOT_TAKEN,
									"He is not hot again: " + tyrant.heat());
							helper.assertTrue(tyrant.speed() == CinderTyrantEntity.SPEED, "Hot again, he does not crawl at his pace");
						});
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * His Tail Sweep strikes a player behind him and one beside him, and sets them burning, and spares the one before his
	 * head. They wait out their loading in creative, where he pays them no heed.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 180)
	public void hisTailSweepsRoundHisBackNotBeforeHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 30, 32, 41, 43);
		ServerPlayer front = player(helper, tyrant, 0.0, 3.5);
		ServerPlayer behind = player(helper, tyrant, 0.0, -3.5);
		ServerPlayer beside = player(helper, tyrant, 3.5, 0.0);
		List<ServerPlayer> players = List.of(front, behind, beside);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, tyrant, () -> {
				middle(tyrant);  // facing south, towards the one in front
				int hit = tyrant.sweep(level);
				helper.assertTrue(hit == 2, "The sweep struck " + hit + " players, not 2");
				helper.assertTrue(front.getHealth() == front.getMaxHealth() && !front.isOnFire(), "The one before his head was not spared");
				for (ServerPlayer struck : List.of(behind, beside)) {
					helper.assertTrue(struck.getHealth() < struck.getMaxHealth() && struck.isOnFire(), "A player behind or beside him was not struck and set burning");
				}
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * His Body Slam lands: a player within {@value CinderTyrantEntity#SLAM_RADIUS} blocks is struck, one farther off only
	 * thrown back. Landing in a flooded trough quenches him; he is in it when its stone is under him or within
	 * {@value CinderTyrantEntity#QUENCH_MARGIN} of his feet, and not a hair beyond that, nor over dry trough stone.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 180)
	public void aSlamInAFloodedTroughQuenchesHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 28, 28, 43, 48);
		trough(level, origin, 28, 41, 43, 43, true);   // flooded: three rows from z 41
		trough(level, origin, 28, 31, 43, 33, false);  // dry
		ServerPlayer near = player(helper, tyrant, 2.0, 5.0);
		ServerPlayer far = player(helper, tyrant, 4.0, 5.0);
		List<ServerPlayer> players = List.of(near, far);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, tyrant, () -> {
				double x = origin.getX() + CinderKiln.BOWL_X;
				double z0 = origin.getZ() + 41.0;
				tyrant.teleportTo(x, tyrant.floorY(), z0 - 1.3 - 0.4);
				helper.assertTrue(!tyrant.inFloodedTrough(level), "Short of the trough's edge by more than his margin, he is in it");
				tyrant.teleportTo(x, tyrant.floorY(), z0 - 1.3 - 0.2);
				helper.assertTrue(tyrant.inFloodedTrough(level), "Within his margin of the trough's edge, he is not in it");
				tyrant.teleportTo(x, tyrant.floorY(), origin.getZ() + 32.5);
				helper.assertTrue(!tyrant.inFloodedTrough(level), "Over dry trough stone, he is in a flooded trough");
				tyrant.teleportTo(x, tyrant.floorY(), origin.getZ() + 42.5);
				int hit = tyrant.slamLands(level);
				helper.assertTrue(hit == 1, "The slam struck " + hit + " players, not 1");
				helper.assertTrue(near.getHealth() < near.getMaxHealth(), "The player beside where he landed was not struck");
				helper.assertTrue(far.getHealth() == far.getMaxHealth() && far.getDeltaMovement().horizontalDistance() > 0.1,
						"The player farther off was struck, or not thrown back");
				helper.assertTrue(tyrant.heat() == CinderTyrantEntity.Heat.QUENCHED && tyrant.quenchedTicks() == CinderTyrantEntity.QUENCH_TICKS,
						"Landing in the flooded trough did not quench him");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * His Kiln Breath burns a player before him and sets them burning; Fire Resistance stops it, and a Salamander Charm does
	 * not. A player forty degrees aside it misses; in the Molten Heart, sweeping wider, it does not.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 180)
	public void hisBreathBurnsBeforeHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 28, 34, 43, 46);
		ServerPlayer front = player(helper, tyrant, 0.0, 4.0);
		ServerPlayer resistant = player(helper, tyrant, 1.0, 5.5);
		ServerPlayer charmed = player(helper, tyrant, -1.0, 5.5);
		double aside = Math.toRadians(40.0);
		ServerPlayer wide = player(helper, tyrant, 5.0 * Math.sin(aside), 5.0 * Math.cos(aside));
		List<ServerPlayer> players = List.of(front, resistant, charmed, wide);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		resistant.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600));
		charmed.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftTyrant.SALAMANDER_CHARM));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, tyrant, () -> {
				middle(tyrant);
				int hit = tyrant.breathe(level);
				helper.assertTrue(hit == 2, "His breath burned " + hit + " players, not 2");
				helper.assertTrue(front.getHealth() < front.getMaxHealth() && front.isOnFire(), "The player before him was not burned");
				helper.assertTrue(charmed.getHealth() < charmed.getMaxHealth(), "The Salamander Charm turned his breath aside");
				helper.assertTrue(resistant.getHealth() == resistant.getMaxHealth(), "Fire Resistance did not stop his breath");
				helper.assertTrue(wide.getHealth() == wide.getMaxHealth(), "His breath reached forty degrees aside");
				tyrant.startHeart(level);
				tyrant.breathe(level);
				helper.assertTrue(wide.getHealth() < wide.getMaxHealth(), "In the Molten Heart his breath did not reach forty degrees aside");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * The Lava Wave burns a player it passes on the floor, once a wave; one standing a block up and one in the air it
	 * passes under.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 180)
	public void theLavaWavePassesUnderThoseUpOrInTheAir(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 26, 28, 45, 47);
		// A step of basalt a block up, as a shelf or the crucible's rim, six blocks east of the centre.
		level.setBlockAndUpdate(origin.offset(41, CinderKiln.BOWL + 1, 37), Blocks.BASALT.defaultBlockState());
		ServerPlayer grounded = player(helper, tyrant, 0.0, 6.0);
		ServerPlayer up = player(helper, tyrant, 6.0, 0.0);
		ServerPlayer leaping = player(helper, tyrant, -6.0, 0.0);
		List<ServerPlayer> players = List.of(grounded, up, leaping);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, tyrant, () -> {
				middle(tyrant);
				up.snapTo(up.getX(), tyrant.floorY() + 1.0, up.getZ(), 0.0F, 0.0F);
				leaping.snapTo(leaping.getX(), tyrant.floorY() + 0.8, leaping.getZ(), 0.0F, 0.0F);
				grounded.setOnGround(true);
				up.setOnGround(true);
				leaping.setOnGround(false);
				tyrant.waveFrom(new Vec3(tyrant.getX(), tyrant.floorY(), tyrant.getZ()));
				int first = tyrant.rollWave(level, 0.0, CinderTyrantEntity.WAVE_REACH);
				int again = tyrant.rollWave(level, 0.0, CinderTyrantEntity.WAVE_REACH);
				helper.assertTrue(first == 1 && again == 0, "The wave burned " + first + " players, then " + again + ", not 1 and none");
				helper.assertTrue(grounded.getHealth() < grounded.getMaxHealth() && grounded.isOnFire(), "The wave did not burn the player on the floor");
				helper.assertTrue(up.getHealth() == up.getMaxHealth(), "The wave reached the player a block up");
				helper.assertTrue(leaping.getHealth() == leaping.getMaxHealth(), "The wave reached the player in the air");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * The heat channel's surge, announced, spills {@value CinderTyrantEntity#SURGE_WARN} ticks later: every cell of floor
	 * within {@value CinderTyrantEntity#SURGE_SPILL} blocks of the channel with open air over it is slag, but not the
	 * crucible, nor a cell with a block on it, nor floor farther off; {@value CinderTyrantEntity#SURGE_TICKS} ticks later it
	 * ebbs, and every cell is what it was, cracked basalt in the bowl and rough basalt beyond it.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 200)
	public void theChannelSurgesOverItsBanksAndEbbs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		Map<BlockPos, BlockState> laid = new LinkedHashMap<>();
		for (int x = CinderKiln.RUN_WEST - 4; x <= CinderKiln.RUN_EAST + 4; x++) {
			for (int z = CinderKiln.CHANNEL_NORTH; z <= CinderKiln.CHANNEL_SOUTH; z++) {
				BlockState state = CinderKiln.inChannel(x, z) ? JugcraftLairs.MOLTEN_SLAG.defaultBlockState()
						: CinderKiln.inBowl(x, z) ? JugcraftLairs.CRACKED_BASALT.defaultBlockState() : Blocks.BASALT.defaultBlockState();
				BlockPos cell = origin.offset(x, CinderKiln.BOWL, z);
				level.setBlockAndUpdate(cell, state);
				laid.put(cell, state);
			}
		}
		BlockPos covered = origin.offset(CinderKiln.RUN_WEST - 2, CinderKiln.BOWL, 18);
		level.setBlockAndUpdate(covered.above(), Blocks.BASALT.defaultBlockState());
		tyrant.startSurge(level);
		boolean held = tyrant.surged().isEmpty();
		helper.runAfterDelay(CinderTyrantEntity.SURGE_WARN + 3, () -> {
			Set<BlockPos> spilled = new HashSet<>(tyrant.surged());
			for (Map.Entry<BlockPos, BlockState> entry : laid.entrySet()) {
				BlockPos cell = entry.getKey();
				int x = cell.getX() - origin.getX();
				int z = cell.getZ() - origin.getZ();
				boolean bank = x >= CinderKiln.RUN_WEST - CinderTyrantEntity.SURGE_SPILL && x <= CinderKiln.RUN_EAST + CinderTyrantEntity.SURGE_SPILL
						&& !CinderKiln.inChannel(x, z) && !CinderKiln.inCrucible(x, z) && !cell.equals(covered);
				helper.assertTrue(spilled.contains(cell) == bank, "The surge " + (bank ? "did not spill over" : "spilled over") + " " + cell);
				helper.assertTrue(!bank || level.getBlockState(cell).is(JugcraftLairs.MOLTEN_SLAG), "The surge's cell " + cell + " is not slag");
			}
			helper.assertTrue(level.getBlockState(covered).is(JugcraftLairs.CRACKED_BASALT), "The slag spilled under a block standing on the floor");
		});
		helper.runAfterDelay(CinderTyrantEntity.SURGE_WARN + CinderTyrantEntity.SURGE_TICKS + 6, () -> {
			ending(level, tyrant, () -> {
				helper.assertTrue(held, "The surge spilled before its warning");
				helper.assertTrue(tyrant.surged().isEmpty(), "The surge has not ebbed");
				for (Map.Entry<BlockPos, BlockState> entry : laid.entrySet()) {
					helper.assertTrue(level.getBlockState(entry.getKey()) == entry.getValue(),
							"After the ebb, " + entry.getKey() + " is " + level.getBlockState(entry.getKey()) + ", not " + entry.getValue());
				}
			});
			helper.succeed();
		});
	}

	/**
	 * In the Eruption the choke moves round the sluices, west, east and south, never onto an open one, and with the others
	 * open it stays where it is; a choked gate will not turn, and a gate choked while open closes and its trough drains.
	 * When the fight resets, every gate is ready and every trough dry. Each gate here is its wheel over a frame block on
	 * trough stone, at the kiln's wheels.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 40)
	public void theChokeMovesRoundTheSluices(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		List<BlockPos> wheels = tyrant.wheels();
		BlockState gate = JugcraftLairs.SLUICE_GATE.defaultBlockState().setValue(SluiceGateBlock.FACING, Direction.NORTH);
		for (BlockPos wheel : wheels) {
			level.setBlockAndUpdate(wheel, gate.setValue(SluiceGateBlock.PART, SluiceGateBlock.Part.WHEEL));
			level.setBlockAndUpdate(wheel.below(), gate.setValue(SluiceGateBlock.PART, SluiceGateBlock.Part.FRAME));
			for (Direction way : List.of(Direction.NORTH, Direction.SOUTH)) {
				level.setBlockAndUpdate(wheel.below(2).relative(way), JugcraftLairs.TROUGH_STONE.defaultBlockState());
			}
			level.setBlockAndUpdate(wheel.below(2), JugcraftLairs.TROUGH_STONE.defaultBlockState());
		}
		ending(level, tyrant, () -> {
			helper.assertTrue(SluiceGateBlock.turn(level, wheels.get(1), null) == SluiceGateBlock.Turn.OPENED, "The east sluice did not open");
			int first = tyrant.moveChoke(level);
			helper.assertTrue(first == 0 && flow(level, wheels.get(0)) == SluiceGateBlock.Flow.CHOKED, "The choke did not fall on the west sluice first");
			int second = tyrant.moveChoke(level);
			helper.assertTrue(second == 2 && flow(level, wheels.get(2)) == SluiceGateBlock.Flow.CHOKED,
					"The choke did not pass over the open east sluice to the south one: " + second);
			helper.assertTrue(flow(level, wheels.get(0)) == SluiceGateBlock.Flow.READY, "The west sluice was not cleared as the choke moved on");
			helper.assertTrue(flow(level, wheels.get(1)) == SluiceGateBlock.Flow.OPEN, "The open east sluice was choked");
			helper.assertTrue(SluiceGateBlock.turn(level, wheels.get(2), null) == SluiceGateBlock.Turn.CHOKED, "A choked sluice turned");
			helper.assertTrue(SluiceGateBlock.turn(level, wheels.get(0), null) == SluiceGateBlock.Turn.OPENED, "The cleared west sluice did not turn");
			helper.assertTrue(tyrant.moveChoke(level) == 2 && flow(level, wheels.get(2)) == SluiceGateBlock.Flow.CHOKED,
					"With the other two open, the choke did not stay where it was");
			helper.assertTrue(SluiceGateBlock.choke(level, wheels.get(1), true) && flow(level, wheels.get(1)) == SluiceGateBlock.Flow.CHOKED
					&& !flooded(level, wheels.get(1)), "A sluice choked while open did not close and drain");
			tyrant.reset(level);
			for (BlockPos wheel : wheels) {
				helper.assertTrue(flow(level, wheel) == SluiceGateBlock.Flow.READY && !flooded(level, wheel), "After the reset, " + wheel + " is not ready and dry");
			}
			helper.assertTrue(tyrant.choked() == -1, "After the reset a sluice is still counted choked");
		});
		helper.succeed();
	}

	private static SluiceGateBlock.Flow flow(ServerLevel level, BlockPos wheel) {
		return level.getBlockState(wheel).getValue(SluiceGateBlock.FLOW);
	}

	/** Whether the trough under the test's gate at {@code wheel} is flooded. */
	private static boolean flooded(ServerLevel level, BlockPos wheel) {
		return level.getBlockState(wheel.below(2)).getValue(TroughStoneBlock.FLOODED);
	}

	/**
	 * At half his health, the Eruption: he leaps up onto the forge's lip, unhurt while it lasts, roars as the channel surges
	 * and two Cinderlings crawl out on its banks, and {@value CinderTyrantEntity#ERUPT_TICKS} ticks later comes back down
	 * into the bowl to fight on.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 220)
	public void atHalfHealthTheEruption(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 30, 14, 41, 42);
		tyrant.wake(level);
		helper.runAfterDelay(CinderTyrantEntity.WAKE_TICKS + 2, () -> {
			tyrant.setHealth(tyrant.getMaxHealth() * 0.5F);
			tyrant.startEruption(level);
			boolean unhurt = !tyrant.hurtServer(level, level.damageSources().magic(), 10.0F);
			helper.runAfterDelay(CinderTyrantEntity.ERUPT_LEAP + 3, () -> {
				Vec3 lip = CinderKiln.lip().add(origin.getX(), origin.getY(), origin.getZ());
				helper.assertTrue(unhurt, "Erupting, he was harmed");
				helper.assertTrue(tyrant.phase() == CinderTyrantEntity.Phase.ERUPTING && tyrant.position().distanceTo(lip) < 0.5
						&& tyrant.action() == CinderTyrantEntity.Action.ROAR, "He is not roaring on the forge's lip but at " + tyrant.position());
				List<UUID> lings = tyrant.cinderlings();
				helper.assertTrue(lings.size() == CinderTyrantEntity.ERUPT_CINDERLINGS, lings.size() + " Cinderlings crawled out, not "
						+ CinderTyrantEntity.ERUPT_CINDERLINGS);
				for (boolean west : List.of(true, false)) {
					Vec3 bank = CinderKiln.bank(west).add(origin.getX(), origin.getY(), origin.getZ());
					helper.assertTrue(lings.stream().anyMatch(id -> level.getEntity(id) instanceof CinderlingEntity ling
							&& Math.hypot(ling.getX() - bank.x, ling.getZ() - bank.z) < 2.0), "No Cinderling crawled out on the " + (west ? "west" : "east") + " bank");
				}
			});
			helper.runAfterDelay(CinderTyrantEntity.ERUPT_LEAP + CinderTyrantEntity.SURGE_WARN + 3,
					() -> helper.assertTrue(!tyrant.surged().isEmpty(), "The channel did not surge as he roared"));
			helper.runAfterDelay(2 * CinderTyrantEntity.ERUPT_LEAP + CinderTyrantEntity.ERUPT_TICKS + 3, () -> {
				ending(level, tyrant, () -> {
					Vec3 out = CinderKiln.out().add(origin.getX(), origin.getY(), origin.getZ());
					helper.assertTrue(tyrant.phase() == CinderTyrantEntity.Phase.ERUPTION, "He did not come down to fight on: " + tyrant.phase());
					helper.assertTrue(Math.abs(tyrant.getY() - tyrant.floorY()) < 0.1 && Math.hypot(tyrant.getX() - out.x, tyrant.getZ() - out.z) < 2.0,
							"He did not come down into the bowl but to " + tyrant.position());
					float before = tyrant.getHealth();
					tyrant.hurtServer(level, level.damageSources().magic(), 10.0F);
					helper.assertTrue(tyrant.getHealth() < before, "Down from the lip, he cannot be harmed");
				});
				helper.succeed();
			});
		});
	}

	/**
	 * A gob of magma he spits arcs onto its mark and bursts there, striking the player on it and leaving a patch of fire;
	 * a cinder falls onto its mark and bursts the same way; a player standing in a fire patch burns.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 200)
	public void hisGobsAndCindersBurstAndLeaveFire(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 26, 30, 45, 48);
		ServerPlayer spat = player(helper, tyrant, 0.0, 7.0);
		ServerPlayer under = player(helper, tyrant, -5.0, 3.0);
		ServerPlayer beside = player(helper, tyrant, 5.0, 3.0);
		List<ServerPlayer> players = List.of(spat, under, beside);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			middle(tyrant);
			MagmaGobEntity gob = MagmaGobEntity.spit(level, tyrant, new Vec3(spat.getX(), tyrant.floorY(), spat.getZ()));
			FallingCinderEntity cinder = FallingCinderEntity.drop(level, tyrant, new Vec3(under.getX(), tyrant.floorY(), under.getZ()));
			helper.runAfterDelay(30, () -> {
				ending(level, tyrant, () -> {
					helper.assertTrue(gob.isRemoved() && cinder.isRemoved(), "The gob or the cinder did not burst");
					helper.assertTrue(spat.getHealth() < spat.getMaxHealth(), "The gob did not strike the player on its mark");
					helper.assertTrue(under.getHealth() < under.getMaxHealth(), "The cinder did not strike the player under it");
					helper.assertTrue(tyrant.patches().size() >= 2, "No patches of fire where they burst: " + tyrant.patches().size());
					float before = beside.getHealth();
					tyrant.firePatch(level, new Vec3(beside.getX() - 0.5, tyrant.floorY(), beside.getZ()));
					tyrant.scorch(level);
					helper.assertTrue(beside.getHealth() < before && beside.isOnFire(), "Standing in a patch of fire did not burn the player");
					players.forEach(ServerPlayer::discard);
				});
				helper.succeed();
			});
		});
	}

	/**
	 * His Mantle Shed brings out Cinderlings, never more than {@value CinderTyrantEntity#MAX_CINDERLINGS} at once; one on a
	 * flooded trough gutters out; when he sinks back into his crucible the rest crumble to ash.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 120)
	public void hisCinderlingsGutterInWaterAndCrumbleWhenHeSinks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 28, 28, 43, 46);
		trough(level, origin, 41, 44, 43, 46, true);
		tyrant.wake(level);
		helper.runAfterDelay(CinderTyrantEntity.WAKE_TICKS + 2, () -> {
			middle(tyrant);
			int shed = tyrant.shed(level, 6);
			int more = tyrant.shed(level, 2);
			List<UUID> lings = tyrant.cinderlings();
			CinderlingEntity wet = (CinderlingEntity) level.getEntity(lings.getFirst());
			wet.teleportTo(origin.getX() + 42.5, tyrant.floorY(), origin.getZ() + 45.5);
			helper.runAfterDelay(10, () -> {
				boolean guttered = wet.isRemoved();
				tyrant.reset(level);
				ending(level, tyrant, () -> {
					helper.assertTrue(shed == CinderTyrantEntity.MAX_CINDERLINGS && more == 0, "More than " + CinderTyrantEntity.MAX_CINDERLINGS
							+ " Cinderlings came at once: " + shed + " and " + more);
					helper.assertTrue(guttered, "A Cinderling on a flooded trough did not gutter out");
					for (UUID id : lings) {
						helper.assertTrue(!(level.getEntity(id) instanceof CinderlingEntity ling) || ling.isRemoved(), "A Cinderling outlived his sinking back");
					}
					helper.assertTrue(tyrant.phase() == CinderTyrantEntity.Phase.WAITING && tyrant.getHealth() == tyrant.getMaxHealth()
							&& tyrant.position().distanceTo(tyrant.sunk()) < 0.1, "Left alone he did not sink back into his crucible, healed");
				});
				helper.succeed();
			});
		});
	}

	/**
	 * A Salamander Charm held in the offhand: the kiln's slag does not harm its holder nor set them burning, and no hot
	 * ground hurts them; without it, both do.
	 */
	@GameTest(maxTicks = 120)
	public void theSalamanderCharmWardsOffHotGround(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer bare = helper.makeMockServerPlayerInLevel();
		ServerPlayer warded = helper.makeMockServerPlayerInLevel();
		bare.setGameMode(GameType.CREATIVE);
		warded.setGameMode(GameType.CREATIVE);
		warded.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftTyrant.SALAMANDER_CHARM));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			bare.setGameMode(GameType.SURVIVAL);
			warded.setGameMode(GameType.SURVIVAL);
			helper.assertTrue(MoltenSlagBlock.harms(bare) && !MoltenSlagBlock.harms(warded), "The slag does not spare the charm's holder alone");
			boolean bareHurt = bare.hurtServer(level, level.damageSources().hotFloor(), 1.0F);
			boolean wardedHurt = warded.hurtServer(level, level.damageSources().hotFloor(), 1.0F);
			helper.assertTrue(bareHurt && bare.getHealth() < bare.getMaxHealth(), "Hot ground did not hurt a player without the charm");
			helper.assertTrue(!wardedHurt && warded.getHealth() == warded.getMaxHealth(), "Hot ground hurt the charm's holder");
			bare.discard();
			warded.discard();
			helper.succeed();
		});
	}

	/**
	 * His loot is each participant's own: everyone who struck him rolls for themselves (Tyrant Scales, and on a first kill
	 * always one of his trophies) into their own inventory, and a bystander who never struck him gets nothing. Afterwards
	 * Tempered is theirs, and a later roll no longer counts as a first kill. As he falls the crucible's slag cools to
	 * obsidian.
	 */
	@GameTest(structure = KILN, padding = 8)
	public void hisLootIsEachParticipantsOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 31, 36, 40, 44);
		BlockPos slag = BlockPos.containing(CinderKiln.CRUCIBLE_X, CinderKiln.BOWL, CinderKiln.CRUCIBLE_Z).offset(origin);
		level.setBlockAndUpdate(slag, JugcraftLairs.MOLTEN_SLAG.defaultBlockState());
		ServerPlayer first = player(helper, tyrant, 0.5, 3.0);
		ServerPlayer second = player(helper, tyrant, 2.5, 3.0);
		ServerPlayer bystander = player(helper, tyrant, -1.5, 3.0);
		tyrant.took(first, 10.0F);
		tyrant.took(second, 4.0F);
		ending(level, tyrant, () -> {
			List<ServerPlayer> participants = tyrant.participants(level);
			helper.assertTrue(participants.contains(first) && participants.contains(second), "Both who struck him take part");
			helper.assertFalse(participants.contains(bystander), "A bystander who never struck him does not");
			helper.assertTrue(CinderTyrantLoot.firstKill(first), "A newcomer has not yet won Tempered");
			List<ItemStack> loot = CinderTyrantLoot.roll(level, tyrant, first);
			int scales = loot.stream().filter(stack -> stack.is(JugcraftTyrant.TYRANT_SCALE)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(scales >= 3 && scales <= 6, scales + " Tyrant Scales, not 3 to 6");
			helper.assertTrue(loot.stream().filter(CinderTyrantLoot::trophy).count() == 1, "A first kill always brings one of his trophies");
			CinderTyrantLoot.reward(level, tyrant, participants);
			for (ServerPlayer player : List.of(first, second)) {
				helper.assertTrue(player.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) >= 3, "Each participant's loot went into their own inventory");
				int trophies = player.getInventory().countItem(JugcraftTyrant.cinderbrand()) + player.getInventory().countItem(JugcraftTyrant.magmaw());
				helper.assertTrue(trophies == 1, "Each first kill brought its own trophy, not " + trophies);
				helper.assertFalse(CinderTyrantLoot.firstKill(player), "Tempered is theirs now");
			}
			helper.assertTrue(bystander.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) == 0, "The bystander got nothing");
			helper.assertTrue(CinderTyrantLoot.firstKill(bystander), "The bystander has not won Tempered");
			helper.assertTrue(tyrant.coolCrucible(level) >= 1 && level.getBlockState(slag).is(Blocks.OBSIDIAN), "The crucible's slag did not cool to obsidian");
			for (ServerPlayer player : List.of(first, second, bystander)) {
				player.discard();
			}
		});
		helper.succeed();
	}
}
